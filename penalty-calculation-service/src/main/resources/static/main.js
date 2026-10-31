document.querySelectorAll('[data-form-template]').forEach(placeholder => {
  const template = document.getElementById(placeholder.dataset.formTemplate);
  placeholder.replaceWith(template.content.cloneNode(true));
});

const reportForm = document.querySelector('#reports-form');
const stageNumber = reportForm.querySelector('[name="stageNumber"]');
const contractNumber = reportForm.querySelector('[name="contractNumber"]');
const stageAmount = reportForm.querySelector('[name="stageAmount"]');
const reportPlannedSubmissionDate = reportForm.querySelector('[name="reportPlannedSubmissionDate"]');
const reportActualSubmissionDate = reportForm.querySelector('[name="reportActualSubmissionDate"]');
const tabs = [...document.querySelectorAll('.tabs button')];
const grantReport = document.querySelector('#grant-report');
const extraBudgetReport = document.querySelector('#extra-budget-report');
const technicalReport = document.querySelector('#technical-report');
const plannedCompletionDate = technicalReport.querySelector('[name="plannedCompletionDate"]');
const actualCompletionDate = technicalReport.querySelector('[name="actualCompletionDate"]');
const reportPanels = [grantReport, extraBudgetReport, technicalReport];
const plannedDateHint = grantReport.querySelector('[data-form-field="plannedSubmissionDate"]').textContent;
const calculationDate = reportForm.querySelector('[name="calculationDate"]');
const submit = document.querySelector('#submit-report');
const result = document.querySelector('#result');
const calculationResult = document.querySelector('#calculation-result');

calculationDate.value = today();
new ResizeObserver(() => document.documentElement.style.setProperty('--actions-height', `${document.querySelector('.actions').offsetHeight}px`))
  .observe(document.querySelector('.actions'));

function today() {
  const date = new Date();
  return `${String(date.getDate()).padStart(2, '0')}.${String(date.getMonth() + 1).padStart(2, '0')}.${date.getFullYear()}`;
}

tabs.forEach((tab, index) => tab.addEventListener('click', () => {
  tabs.forEach((button, i) => {
    button.classList.toggle('active', i === index);
    reportPanels[i].hidden = i !== index;
  });
}));

reportForm.addEventListener('input', ({ target }) => {
  if (!target.matches('input')) return;
  if (target === stageNumber) target.value = target.value.replace(/\D/g, '');
  if (target === contractNumber) target.value = target.value.replace(/\D/g, '').slice(0, 2);
  if (target === stageAmount) formatAmount(target);
  if (target.matches('[data-date]')) {
    formatDate(target);
    if (target.matches('[data-form-field="returnedForRevisionDate"]')) {
      updatePanelPlannedDates(target.closest('[data-report]'));
    }
  }
  validateForm();
});

reportForm.addEventListener('change', ({ target }) => {
  if (!target.matches('[data-form-field="remarksCorrected"]')) return;
  const panel = target.closest('[data-report]');
  updateCaseNumbers(panel);
  updatePanelPlannedDates(panel);
  validateForm();
});

function clearCaseRow(row) {
  row.querySelectorAll('.case-cell input').forEach(input => {
    if (input.type === 'checkbox') input.checked = true;
    else input.value = '';
    input.setCustomValidity('');
  });
  const planned = row.querySelector('[data-form-field="plannedSubmissionDate"]');
  planned.textContent = plannedDateHint;
}

function addCaseRow(panel) {
  const rows = panel.querySelector('[data-form-field="cases"]');
  rows.append(panel.querySelector('[data-form-case-template]').content.cloneNode(true));
  return rows.lastElementChild;
}

reportPanels.forEach(panel => {
  const rows = panel.querySelector('[data-form-field="cases"]');

  panel.querySelector('.add-case').addEventListener('click', () => {
    const row = addCaseRow(panel);
    updateCaseNumbers(panel);
    updatePanelPlannedDates(panel);
    validateForm();
    row.querySelector('[data-date]').focus();
  });

  rows.addEventListener('click', ({ target }) => {
    if (!target.matches('.remove-case')) return;
    const row = target.closest('tr');
    if (rows.rows.length === 1) clearCaseRow(row);
    else {
      const shared = row.querySelector('.shared');
      if (shared) rows.rows[1].append(shared);
      row.remove();
    }
    updateCaseNumbers(panel);
    updatePanelPlannedDates(panel);
    validateForm();
  });
});

function addDayToExclude(panel, dayToExclude = '') {
  const rows = panel.querySelector('.days-to-exclude-list');
  rows.append(document.querySelector('#day-to-exclude-template').content.cloneNode(true));
  const row = rows.lastElementChild;
  row.querySelector('[name="days"]').value = dayToExclude;
  return row;
}

function addDayRangeToExclude(panel, startDate = '', endDate = '') {
  const rows = panel.querySelector('.day-ranges-to-exclude-list');
  rows.append(document.querySelector('#day-range-to-exclude-template').content.cloneNode(true));
  const row = rows.lastElementChild;
  row.querySelector('[name="startDate"]').value = startDate;
  row.querySelector('[name="endDate"]').value = endDate;
  return row;
}

reportPanels.forEach(panel => {
  panel.querySelector('.add-day-to-exclude').addEventListener('click', () => {
    addDayToExclude(panel).querySelector('[name="days"]').focus();
    validateForm();
  });
  panel.querySelector('.add-day-range-to-exclude').addEventListener('click', () => {
    addDayRangeToExclude(panel).querySelector('[name="startDate"]').focus();
    validateForm();
  });
  panel.querySelector('.days-to-exclude-list').addEventListener('click', ({ target }) => {
    if (!target.matches('.remove-day-to-exclude')) return;
    target.closest('.day-to-exclude-row').remove();
    validateForm();
  });
  panel.querySelector('.day-ranges-to-exclude-list').addEventListener('click', ({ target }) => {
    if (!target.matches('.remove-day-range-to-exclude')) return;
    target.closest('.day-range-to-exclude-row').remove();
    validateForm();
  });
});

reportForm.addEventListener('reset', event => {
  event.preventDefault();
  reportPanels.forEach(panel => {
    const rows = panel.querySelector('[data-form-field="cases"]');
    [...rows.rows].slice(1).forEach(row => row.remove());
    clearCaseRow(rows.rows[0]);
    updateCaseNumbers(panel);
    panel.querySelector('.days-to-exclude-list').replaceChildren();
    panel.querySelector('.day-ranges-to-exclude-list').replaceChildren();
  });
  reportForm.querySelectorAll('input').forEach(input => {
    if (input.type !== 'checkbox' && !input.readOnly) input.value = '';
    input.setCustomValidity('');
  });
  calculationDate.value = today();
  result.textContent = '';
  calculationResult.hidden = true;
  tabs[0].click();
  updateLongCaseWarnings();
});

function updateLongCaseWarnings() {
  const calendarDay = date => Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
  reportForm.querySelectorAll('[data-form-field="actualSubmissionDate"]').forEach(input => {
    const planned = parseDate(input.closest('tr').querySelector('[data-form-field="plannedSubmissionDate"]').textContent);
    const actual = parseDate(input.value);
    const days = planned && actual && input.validity.valid
      ? (calendarDay(actual) - calendarDay(planned)) / 86400000
      : 0;
    const cell = input.closest('td');
    const note = cell.querySelector('.long-case-note');
    const message = days > 30 ? `Долгая работа над кейсом: ${days} дн. (более 30).` : '';
    cell.classList.toggle('long-case-warning', days > 30);
    note.textContent = message;
    note.hidden = !message;
  });
}

function updateCaseNumbers(panel) {
  let number = 1;
  for (const row of panel.querySelector('[data-form-field="cases"]').rows) {
    row.querySelector('[data-form-field="caseNumber"]').value = number;
    if (row.querySelector('[data-form-field="remarksCorrected"]').checked) number++;
  }
}

function updatePanelPlannedDates(panel) {
  panel.querySelectorAll('[data-form-field="returnedForRevisionDate"]').forEach(updatePlannedDate);
}

async function updatePlannedDate(input) {
  const row = input.closest('tr');
  const cell = row.querySelector('[data-form-field="plannedSubmissionDate"]');
  const caseNumber = Number(row.querySelector('[data-form-field="caseNumber"]').value);
  const firstRowForCase = [...row.parentElement.rows]
    .find(candidate => Number(candidate.querySelector('[data-form-field="caseNumber"]').value) === caseNumber);
  const value = firstRowForCase?.querySelector('[data-form-field="returnedForRevisionDate"]').value;
  cell.textContent = plannedDateHint;
  updateLongCaseWarnings();
  if (!Number.isInteger(caseNumber) || caseNumber < 1 || !value || dateError(value)) return;
  cell.textContent = '…';
  result.textContent = '';
  try {
    const params = new URLSearchParams({ caseNumber, returnedForRevisionDate: value });
    const response = await fetch(`/api/reports/revision-planned-date?${params}`);
    const data = await response.json();
    if (!response.ok) throw new Error(data.detail || 'Не удалось рассчитать дату. Проверьте наличие календаря для выбранного года.');
    cell.textContent = data.plannedSubmissionDate;
    updateLongCaseWarnings();
  } catch (error) {
    cell.textContent = 'Ошибка';
    result.textContent = error.message;
  }
}

async function postRequest(path, data) {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const error = await response.json();
    throw Error(error.detail || 'Не удалось выполнить запрос. Проверьте данные и повторите попытку.');
  }
  return response;
}

reportForm.addEventListener('submit', async event => {
  event.preventDefault();
  const data = validatedRequest();
  if (!data) return;
  submit.disabled = true;
  result.textContent = '';
  calculationResult.hidden = true;
  const loadingTime = showCalculationCat();
  try {
    const response = await postRequest('/api/reports/penalty-calculation', data);
    const calculation = await response.json();
    renderCalculation(calculation);
    result.textContent = `Неустойка: ${calculation.totalPenalty}.`;
  } catch (error) {
    result.textContent = error.message;
  } finally {
    await loadingTime;
    hideCalculationCat();
    submit.disabled = false;
  }
});

function validatedRequest() {
  validateForm();
  const invalid = reportForm.querySelector('input:invalid');
  if (invalid) {
    const report = invalid.closest('[data-report]');
    if (report) tabs[reportPanels.indexOf(report)].click();
    invalid.reportValidity();
    return null;
  }
  return getRequestData();
}

document.querySelector('#export-report').addEventListener('click', async event => {
  const data = validatedRequest();
  if (!data) return;
  const button = event.currentTarget;
  button.disabled = true;
  result.textContent = 'Готовим Excel…';
  try {
    const response = await postRequest('/api/reports/penalty-calculation/export', data);
    const url = URL.createObjectURL(await response.blob());
    const link = document.querySelector('#calculation-download');
    link.href = url;
    const disposition = response.headers.get('Content-Disposition') || '';
    const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    link.download = encodedName ? decodeURIComponent(encodedName) : disposition.match(/filename="?([^";]+)"?/i)?.[1] || 'penalty_calculation.xlsx';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 0);
    result.textContent = 'Файл XLSX подготовлен.';
  } catch (error) {
    result.textContent = error.message;
  } finally {
    button.disabled = false;
  }
});
