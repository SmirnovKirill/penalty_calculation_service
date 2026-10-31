const { app, BrowserWindow, dialog, screen } = require('electron');
const { spawn } = require('node:child_process');
const { once } = require('node:events');
const { mkdirSync } = require('node:fs');
const { createInterface } = require('node:readline');
const path = require('node:path');

let backend;
let window;
let quitting = false;
let backendLog = '';

function ready(child) {
  const lines = createInterface({ input: child.stdout });
  const cancel = new AbortController();
  let timeout;
  const marker = new Promise(resolve => {
    lines.on('line', line => {
      const match = line.match(/^PENALTY_CALCULATION_SERVICE_DESKTOP_PORT=(\d+)$/);
      if (match) resolve(`http://127.0.0.1:${match[1]}`);
    });
  });
  const exited = once(child, 'exit', { signal: cancel.signal }).then(([code]) => {
    throw new Error(`Бэкенд завершился при запуске: ${code}.`);
  });
  const expired = new Promise((_, reject) => {
    timeout = setTimeout(reject, 60000, new Error('Бэкенд не запустился за 60 секунд.'));
  });
  return Promise.race([marker, exited, expired]).finally(() => {
    clearTimeout(timeout);
    cancel.abort();
    lines.close();
    child.stdout.resume();
  });
}

async function start() {
  const root = app.isPackaged ? process.resourcesPath
    : path.join(__dirname, '..', 'build', 'desktop', 'win32-x64');
  const java = path.join(root, 'runtime', 'bin', 'java.exe');
  const cwd = app.getPath('userData');
  mkdirSync(cwd, { recursive: true });
  backend = spawn(java, [
    '-Xmx384m', '-jar', path.join(root, 'backend', 'app.jar'),
    '--desktop=true', '--server.address=127.0.0.1', '--server.port=0', '--spring.main.banner-mode=off',
  ], { cwd, windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] });
  const log = chunk => { backendLog = (backendLog + chunk).slice(-6000); };
  backend.stdout.on('data', log);
  backend.stderr.on('data', log);
  backend.stdin.on('error', () => {}); // The child may exit before its pipe is closed.
  const origin = await ready(backend);
  backend.once('exit', () => {
    if (!quitting) fail(new Error('Бэкенд неожиданно завершился.'));
  });

  const size = screen.getPrimaryDisplay().workAreaSize;
  window = new BrowserWindow({
    icon: path.join(__dirname, 'app-icon.png'),
    title: app.getName(), width: Math.min(1440, size.width), height: Math.min(900, size.height),
    minWidth: 800, minHeight: 600, show: false,
  });
  window.setMenu(null);
  window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  const localOnly = (event, url) => { if (new URL(url).origin !== origin) event.preventDefault(); };
  window.webContents.on('will-navigate', localOnly);
  window.webContents.on('will-redirect', localOnly);
  window.webContents.session.setPermissionRequestHandler((_contents, _permission, callback) => callback(false));
  window.webContents.session.setPermissionCheckHandler(() => false);
  window.webContents.session.on('will-download', (_event, item) => {
    if (item.getFilename().endsWith('.xlsx')) {
      item.setSaveDialogOptions({
        title: 'Сохранить расчёт',
        defaultPath: path.join(app.getPath('downloads'), item.getFilename()),
        filters: [{ name: 'Excel', extensions: ['xlsx'] }],
      });
    }
  });
  window.webContents.session.webRequest.onHeadersReceived((details, callback) => callback({
    responseHeaders: { ...details.responseHeaders, 'Content-Security-Policy': [
      "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'",
    ] },
  }));
  window.webContents.on('render-process-gone', () => {
    if (!quitting) fail(new Error('Окно приложения неожиданно завершилось.'));
  });
  window.once('ready-to-show', () => window.show());
  window.on('closed', () => { window = null; });
  await window.loadURL(`${origin}/`);
}

function fail(error) {
  if (quitting) return;
  dialog.showErrorBox(`Ошибка ${app.getName()}`, `${error.message}\n\n${backendLog}`);
  app.quit();
}

async function stop() {
  if (!backend?.pid || backend.exitCode !== null || backend.signalCode !== null) return;
  backend.stdin.end();
  try {
    await once(backend, 'exit', { signal: AbortSignal.timeout(5000) });
  } catch {
    // Portable-оболочка удаляет временную папку после выхода Electron, поэтому ждём завершения Java.
    // Подписываемся на exit до kill, чтобы не пропустить быстрое завершение процесса.
    if (backend.exitCode === null && backend.signalCode === null) {
      const exited = once(backend, 'exit');
      backend.kill('SIGKILL');
      await exited;
    }
  }
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (!window) return;
    if (window.isMinimized()) window.restore();
    window.show();
    window.focus();
  });
  app.on('window-all-closed', () => app.quit());
  app.on('before-quit', event => {
    if (quitting) return;
    event.preventDefault();
    quitting = true;
    stop().finally(() => app.quit());
  });
  app.whenReady().then(start).catch(fail);
}
