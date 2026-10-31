const importRequestData = document.querySelector('#import-request-data');
const exportRequestData = document.querySelector('#export-request-data');
const requestDataFile = document.querySelector('#request-data-file');
const requestDataDownload = document.querySelector('#request-data-download');
const resetRequestForm = document.querySelector('#reset-form');
const exportCalculation = document.querySelector('#export-report');

importRequestData.addEventListener('click', () => requestDataFile.click());

requestDataFile.addEventListener('change', async () => {
  const file = requestDataFile.files[0];
  if (!file) return;
  if (!requestFileAvailable()) {
    requestDataFile.value = '';
    return;
  }
  setRequestFileBusy(true);
  result.textContent = 'Загружаем исходные данные…';
  try {
    const data = JSON.parse((await file.text()).replace(/^\uFEFF/, ''));
    fillRequestData(data);
    result.textContent = 'Исходные данные загружены из файла.';
  } catch (error) {
    result.textContent = error instanceof SyntaxError ? `Ошибка JSON: ${error.message}` : error.message;
  } finally {
    requestDataFile.value = '';
    setRequestFileBusy(false);
  }
});

exportRequestData.addEventListener('click', () => {
  if (!requestFileAvailable()) return;
  setRequestFileBusy(true);
  result.textContent = 'Готовим файл с исходными данными…';
  try {
    const data = getRequestData();
    checkRequestData(data);
    requestDataDownload.href = 'data:application/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(data, null, 2) + '\n');
    requestDataDownload.download = `${data.contractNumber}_№${data.stageNumber ?? ''}_${data.calculationDate}.json`;
    requestDataDownload.click();
    result.textContent = 'Файл с исходными данными подготовлен.';
  } catch (error) {
    result.textContent = error.message;
  } finally {
    setRequestFileBusy(false);
  }
});

function requestFileAvailable() {
  if (importRequestData.disabled || exportRequestData.disabled || submit.disabled || exportCalculation.disabled) {
    result.textContent = 'Дождитесь завершения текущей операции.';
    return false;
  }
  return true;
}

function setRequestFileBusy(busy) {
  importRequestData.disabled = busy;
  exportRequestData.disabled = busy;
  requestDataFile.disabled = busy;
  submit.disabled = busy;
  resetRequestForm.disabled = busy;
  exportCalculation.disabled = busy;
}
