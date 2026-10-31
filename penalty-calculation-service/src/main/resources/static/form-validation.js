function parseDate(value) {
  if (!/^\d{2}\.\d{2}\.\d{4}$/.test(value)) return null;
  const [day, month, year] = value.split('.').map(Number);
  const date = new Date(year, month - 1, day);
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day ? date : null;
}

function dateError(value, calculationDate = null) {
  if (!value) return 'Укажите дату.';
  const date = parseDate(value);
  if (!date) return 'Введите корректную дату в формате дд.мм.гггг.';
  if (date < new Date(2022, 0, 1)) return 'Дата не может быть раньше 01.01.2022.';
  if (date > new Date()) return 'Дата не может быть в будущем.';
  if (calculationDate && date > calculationDate) return 'Дата не может быть позже даты расчёта.';
  return '';
}

function stageNumberError(value) {
  return /^\d+$/.test(value) && Number(value) >= 1 && Number(value) <= 10
    ? '' : 'Укажите номер этапа целым числом от 1 до 10.';
}

function contractNumberError(value) {
  const number = Number(value);
  const hasTwoDigits = /^\d{2}$/.test(value);
  if (hasTwoDigits && number >= 1 && number <= 80) return '';
  return 'Введите номер от 01 до 80.';
}

function stageAmountError(value) {
  return /^\d+([.,]\d{1,2})?$/.test(value.replace(/\s/g, ''))
    ? '' : 'Укажите сумму с точностью до двух знаков после запятой.';
}

function formatDate(input) {
  const beforeCaret = input.value.slice(0, input.selectionStart).replace(/\D/g, '').length;
  input.value = input.value.replace(/\D/g, '').slice(0, 8)
    .replace(/^(\d{2})(\d)/, '$1.$2').replace(/^(\d{2}\.\d{2})(\d)/, '$1.$2');
  const caret = beforeCaret + (beforeCaret > 2) + (beforeCaret > 4);
  input.setSelectionRange(caret, caret);
}

function formatAmount(input) {
  const value = input.value.replace(/[^\d.,]/g, '');
  const [integer, decimals = ''] = value.split(/[.,]/);
  const separator = value.match(/[.,]/)?.[0] ?? '';
  let remainingCharacters = input.value.slice(0, input.selectionStart).replace(/[^\d.,]/g, '').length;

  input.value = integer.replace(/\B(?=(\d{3})+(?!\d))/g, ' ') + separator + decimals.slice(0, 2);

  // Восстанавливаем курсор, не учитывая добавленные пробелы.
  let caretPosition = 0;
  while (remainingCharacters > 0 && caretPosition < input.value.length) {
    if (input.value[caretPosition] !== ' ') remainingCharacters--;
    caretPosition++;
  }
  input.setSelectionRange(caretPosition, caretPosition);
}

function validateForm() {
  stageNumber.setCustomValidity(stageNumberError(stageNumber.value));
  contractNumber.setCustomValidity(contractNumberError(contractNumber.value));
  stageAmount.setCustomValidity(stageAmountError(stageAmount.value));

  reportPlannedSubmissionDate.setCustomValidity(dateError(reportPlannedSubmissionDate.value, parseDate(calculationDate.value)));
  reportActualSubmissionDate.setCustomValidity(dateError(reportActualSubmissionDate.value, parseDate(calculationDate.value)));

  for (const report of reportPanels) {
    let previousActualSubmissionDate = reportActualSubmissionDate;
    const caseRows = report.querySelector('[data-form-field="cases"]');
    for (const caseRow of caseRows.rows) {
      const returnedForRevisionDate = caseRow.querySelector('[data-form-field="returnedForRevisionDate"]');
      returnedForRevisionDate.setCustomValidity(dateError(returnedForRevisionDate.value, parseDate(calculationDate.value)));
      validateDateOrder(returnedForRevisionDate, previousActualSubmissionDate, 'Возврат на доработку не может предшествовать предыдущей подаче отчёта.');

      const actualSubmissionDate = caseRow.querySelector('[data-form-field="actualSubmissionDate"]');
      actualSubmissionDate.setCustomValidity(dateError(actualSubmissionDate.value, parseDate(calculationDate.value)));
      validateDateOrder(actualSubmissionDate, returnedForRevisionDate, 'Подача доработанного отчёта не может предшествовать возврату на доработку.');
      previousActualSubmissionDate = actualSubmissionDate;

      const remarksCorrected = caseRow.querySelector('[data-form-field="remarksCorrected"]');
      remarksCorrected.setCustomValidity(caseRow === caseRows.lastElementChild && !remarksCorrected.checked
        ? 'Замечания не устранены. Отчет не может быть принят.' : '');
    }
    const reportApprovalDate = report.querySelector('[name="reportApprovalDate"]');
    reportApprovalDate.setCustomValidity(dateError(reportApprovalDate.value, parseDate(calculationDate.value)));
    validateDateOrder(reportApprovalDate, previousActualSubmissionDate, 'Согласование не может предшествовать последней подаче отчёта.');

    report.querySelectorAll('.days-to-exclude-list [name="days"]').forEach(dayToExclude => {
      dayToExclude.setCustomValidity(dateError(dayToExclude.value, parseDate(calculationDate.value)));
      validateDateOrder(dayToExclude, reportPlannedSubmissionDate, 'Исключаемая дата не может предшествовать плановой дате подачи отчётности.');
    });
    for (const range of report.querySelector('.day-ranges-to-exclude-list').children) {
      const startDate = range.querySelector('[name="startDate"]');
      const endDate = range.querySelector('[name="endDate"]');
      startDate.setCustomValidity(dateError(startDate.value, parseDate(calculationDate.value)));
      endDate.setCustomValidity(dateError(endDate.value, parseDate(calculationDate.value)));
      validateDateOrder(startDate, reportPlannedSubmissionDate, 'Начало интервала не может предшествовать плановой дате подачи отчётности.');
      validateDateOrder(endDate, reportPlannedSubmissionDate, 'Конец интервала не может предшествовать плановой дате подачи отчётности.');
      validateDateOrder(endDate, startDate, 'Конец интервала не может предшествовать его началу.');
    }
  }

  plannedCompletionDate.setCustomValidity(dateError(plannedCompletionDate.value, parseDate(calculationDate.value)));
  actualCompletionDate.setCustomValidity(dateError(actualCompletionDate.value, parseDate(calculationDate.value)));

  calculationDate.setCustomValidity(dateError(calculationDate.value));

  updateLongCaseWarnings();
}

function validateDateOrder(input, previous, message) {
  if (input.validity.valid && previous.validity.valid && parseDate(input.value) < parseDate(previous.value)) {
    input.setCustomValidity(message);
  }
}
