const CatalogSource = Object.freeze({ BUILTIN: 'BUILTIN', CUSTOM: 'CUSTOM' });
const columns = {
  from: 'С',
  through: 'По',
  rate: 'Ставка, %',
  year: 'Год',
  daysOff: 'Дополнительные выходные',
  workingDays: 'Дополнительные рабочие дни',
};
const response = await fetch('/settings/settings.html');
if (!response.ok) throw Error('Не удалось загрузить настройки.');
document.body.insertAdjacentHTML('beforeend', await response.text());
const dialog = document.getElementById('settings-dialog');
document.getElementById('open-settings').onclick = () => dialog.showModal();
document.getElementById('close-settings').onclick = () => dialog.close();
const { version, releaseDate } = await fetch('/settings/package.json').then(response => response.json());
document.getElementById('settings-version').textContent = `${version} от ${releaseDate}`;

for (const panel of dialog.querySelectorAll('[data-catalog]')) {
  const name = panel.dataset.catalog;
  const header = panel.dataset.header;
  const table = panel.querySelector('tbody');
  const file = panel.querySelector('[type=file]');
  const save = panel.querySelector('[type=submit]');
  const cancel = panel.querySelector('[data-cancel]');
  const output = panel.querySelector('output');
  const note = (message, error = false) => {
    output.textContent = message;
    output.classList.toggle('error', error);
    output.setAttribute('role', error ? 'alert' : 'status');
  };
  function serialize() {
    const rows = [...table.rows].map(row => [...row.querySelectorAll('input, textarea')].map(input => input.value.trim().replace(/\r?\n/g, ' ')).join(';'));
    return `${header}\n${rows.join('\n')}\n`;
  }
  let savedCsv = serialize();
  function updateSaveState() { save.disabled = cancel.disabled = serialize() === savedCsv; return !save.disabled; }
  function changed() { note(updateSaveState() ? 'Изменения не сохранены.' : ''); }
  function validateDates() {
    table.querySelectorAll('[data-date]').forEach(input => input.setCustomValidity(dateError(input.value, null, true)));
    table.querySelectorAll('[data-year]').forEach(input => {
      const year = Number(input.value);
      input.setCustomValidity(/^\d{4}$/.test(input.value) && year >= MIN_SUPPORTED_YEAR && year <= MAX_SUPPORTED_YEAR
        ? '' : `Укажите год от ${MIN_SUPPORTED_YEAR} до ${MAX_SUPPORTED_YEAR}.`);
    });
  }
  async function request(method = 'GET', body) {
    const response = await fetch(`/api/settings/catalogs/${name}`, { method, body, headers: { 'Content-Type': 'text/plain;charset=UTF-8' } });
    const data = await response.json();
    if (!response.ok) throw Error(data.detail || 'Не удалось сохранить справочник.');
    return data;
  }
  async function run(action) {
    panel.disabled = true;
    try { await action(); } catch (error) {
      note(error.message.replace(/столбец "([^"]+)"/, (match, field) =>
        Object.hasOwn(columns, field) ? `столбец "${columns[field]}" (${field})` : match), true);
    }
    finally { panel.disabled = false; updateSaveState(); }
  }
  function row(values) {
    const fragment = panel.querySelector('template').content.cloneNode(true);
    fragment.querySelectorAll('input, textarea').forEach((input, i) => { input.value = (values[i] || '').trim(); });
    return fragment;
  }
  function render(csv) {
    const lines = csv.replace(/^\uFEFF/, '').split(/\r?\n/).filter(line => line.trim() && !line.trimStart().startsWith('#'));
    if (lines.shift() !== header) throw Error(`Ожидается заголовок: ${header}`);
    const rows = lines.map(line => line.split(';').map(value => value.replace(/^"(.*)"$/, '$1')));
    if (!rows.length || rows.some(values => values.length !== 3)) throw Error('Нужны непустые строки с тремя столбцами.');
    table.replaceChildren(...rows.map(row));
    validateDates();
  }
  function show(data) {
    render(data.csv);
    savedCsv = serialize();
    updateSaveState();
    panel.querySelector('[data-source]').textContent = data.source === CatalogSource.CUSTOM ? 'Используется пользовательский справочник' : 'Используется встроенный справочник (нет изменений от пользователя)';
  }
  function applied(data) {
    show(data);
    note('Сохранено. Новые данные используются в расчётах.');
    document.dispatchEvent(new CustomEvent('catalog-changed', { detail: name }));
  }
  panel.querySelector('form').onsubmit = event => {
    event.preventDefault();
    validateDates();
    const invalid = table.querySelector('[data-date]:invalid, [data-year]:invalid');
    if (invalid) { invalid.reportValidity(); return; }
    run(async () => {
      applied(await request('PUT', serialize()));
    });
  };
  cancel.onclick = () => { render(savedCsv); changed(); };
  table.oninput = event => {
    const input = event.target;
    if (input.matches('[data-date]')) formatDate(input);
    if (input.matches('[data-rate]')) formatAmount(input, 5, false);
    if (input.matches('[data-year], textarea')) {
      const year = input.matches('[data-year]');
      const clean = value => value.replace(/\s/g, ' ').replace(year ? /\D/g : /[^\d. ]/g, '').slice(0, year ? 4 : Infinity);
      const caret = clean(input.value.slice(0, input.selectionStart)).length;
      input.value = clean(input.value);
      input.setSelectionRange(caret, caret);
    }
    validateDates();
    changed();
  };
  table.onclick = event => {
    if (event.target.matches('[data-remove]')) { event.target.closest('tr').remove(); changed(); }
  };
  panel.querySelector('[data-add]').onclick = () => {
    const year = Number(table.rows[table.rows.length - 1]?.querySelector('input').value || new Date().getFullYear() - 1) + 1;
    table.append(row(name === 'WORKDAY_CALENDAR' ? [String(year), '', ''] : ['', '', '']));
    changed();
  };
  panel.querySelector('[data-import]').onclick = () => file.click();
  file.onchange = () => run(async () => {
    if (!file.files[0]) return;
    render(await file.files[0].text());
    panel.querySelector('details').open = true;
    note(updateSaveState() ? 'Импортировано в редактор. Проверьте данные и нажмите «Сохранить изменения».' : 'Файл совпадает с действующим справочником.');
  }).finally(() => { file.value = ''; });
  panel.querySelector('[data-export]').onclick = () => run(async () => {
    const { csv } = await request();
    const link = document.createElement('a');
    const content = csv.split(/\r?\n/).filter(line => !line.trimStart().startsWith('#')).join('\r\n');
    const url = URL.createObjectURL(new Blob(['\uFEFF', content], { type: 'text/csv;charset=UTF-8' }));
    link.href = url;
    link.download = `${name}.csv`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 0);
    note('Экспортирован действующий справочник.');
  });
  panel.querySelector('[data-reset]').onclick = () => {
    if (confirm('Вернуть встроенный справочник? Все изменения этого справочника будут потеряны. Встроенные данные сразу начнут использоваться в расчётах.')) {
      run(async () => applied(await request('DELETE')));
    }
  };
  await run(async () => show(await request()));
}
