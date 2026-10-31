const { productName, version } = require('./package.json');
const executableName = `Расчет неустойки версия ${version}`;

module.exports = {
  appId: 'ru.inno_sc.penaltycalculation',
  productName,
  files: ['desktop/*.cjs', 'desktop/app-icon.png', 'desktop/app-icon.ico'],
  win: {
    icon: 'desktop/app-icon.ico',
    target: 'portable',
    artifactName: executableName + '.${ext}',
    executableName,
    extraResources: [
      { from: 'build/desktop/win32-x64' },
    ],
  },
  portable: {
    unpackDirName: true,
  },
};
