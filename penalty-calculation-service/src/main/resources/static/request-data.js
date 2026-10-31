function getRequestData() {
  return {
    formatVersion: 1,
    stageNumber: stageNumber.value ? Number(stageNumber.value) : null,
    contractNumber: `Г-00${contractNumber.value}`,
    stageAmount: stageAmount.value.replace(/\s/g, '').replace(',', '.'),
    reportPlannedSubmissionDate: reportPlannedSubmissionDate.value,
    reportActualSubmissionDate: reportActualSubmissionDate.value,
    grantReport: getReportRequestData(grantReport),
    extraBudgetReport: getReportRequestData(extraBudgetReport),
    technicalReport: {
      ...getReportRequestData(technicalReport),
      plannedCompletionDate: plannedCompletionDate.value,
      actualCompletionDate: actualCompletionDate.value,
    },
    calculationDate: calculationDate.value,
  };
}

function getReportRequestData(panel) {
  return {
    cases: [...panel.querySelector('[data-form-field="cases"]').rows].map(row => ({
      returnedForRevisionDate: row.querySelector('[data-form-field="returnedForRevisionDate"]').value,
      actualSubmissionDate: row.querySelector('[data-form-field="actualSubmissionDate"]').value,
      remarksCorrected: row.querySelector('[data-form-field="remarksCorrected"]').checked,
    })),
    reportApprovalDate: panel.querySelector('[name="reportApprovalDate"]').value,
    daysToExclude: {
      days: [...panel.querySelectorAll('.days-to-exclude-list [name="days"]')].map(input => input.value),
      dayRanges: [...panel.querySelector('.day-ranges-to-exclude-list').children].map(row => ({
        startDate: row.querySelector('[name="startDate"]').value,
        endDate: row.querySelector('[name="endDate"]').value,
      })),
    },
  };
}

function checkRequestData(data) {
  const reports = ['grantReport', 'extraBudgetReport', 'technicalReport'];
  checkObjectFields(data, 'Запрос', ['formatVersion', 'stageNumber', 'contractNumber', 'stageAmount', 'reportPlannedSubmissionDate', 'reportActualSubmissionDate', ...reports, 'calculationDate']);
  if (data.formatVersion !== 1) throw Error('formatVersion: поддерживается только версия 1.');
  if ('stageNumber' in data && data.stageNumber !== null && (!Number.isInteger(data.stageNumber) || data.stageNumber < 1 || data.stageNumber > 10)) throw Error('stageNumber: укажите номер этапа целым числом от 1 до 10.');
  if ('contractNumber' in data && (typeof data.contractNumber !== 'string' || !/^Г-00\d{0,2}$/.test(data.contractNumber))) throw Error('contractNumber: ожидается номер с префиксом Г-00 и двумя цифрами.');
  if ('stageAmount' in data && !['string', 'number'].includes(typeof data.stageAmount)) throw Error('stageAmount: ожидается сумма строкой или числом.');
  if (typeof data.stageAmount === 'number' && Math.abs(data.stageAmount) > Number.MAX_SAFE_INTEGER) throw Error('stageAmount: передайте большую сумму строкой для сохранения точности.');
  if ('stageAmount' in data && !/^\d*([.,]\d{0,2})?$/.test(String(data.stageAmount))) throw Error('stageAmount: укажите только цифры и до двух знаков после запятой.');
  checkDateFields(data, 'Запрос', ['reportPlannedSubmissionDate', 'reportActualSubmissionDate']);
  for (const reportName of reports) {
    const report = data[reportName];
    const dateFields = ['reportApprovalDate', ...(reportName === 'technicalReport' ? ['plannedCompletionDate', 'actualCompletionDate'] : [])];
    checkObjectFields(report, reportName, ['cases', 'daysToExclude', ...dateFields]);
    if (!Array.isArray(report.cases)) throw Error(`${reportName}.cases: ожидается массив кейсов.`);
    report.cases.forEach((item, index) => {
      const path = `${reportName}.cases[${index}]`;
      checkObjectFields(item, path, ['returnedForRevisionDate', 'actualSubmissionDate', 'remarksCorrected']);
      checkDateFields(item, path, ['returnedForRevisionDate', 'actualSubmissionDate']);
      if ('remarksCorrected' in item && typeof item.remarksCorrected !== 'boolean') throw Error(`${path}.remarksCorrected: ожидается true или false.`);
    });
    checkDateFields(report, reportName, dateFields);
    const daysToExclude = report.daysToExclude;
    const daysToExcludePath = `${reportName}.daysToExclude`;
    checkObjectFields(daysToExclude, daysToExcludePath, ['days', 'dayRanges']);
    if (!Array.isArray(daysToExclude.days) || daysToExclude.days.some(day => typeof day !== 'string')) throw Error(`${daysToExcludePath}.days: ожидается массив строк с датами.`);
    if (!Array.isArray(daysToExclude.dayRanges)) throw Error(`${daysToExcludePath}.dayRanges: ожидается массив интервалов дат.`);
    daysToExclude.dayRanges.forEach((range, index) => {
      const path = `${daysToExcludePath}.dayRanges[${index}]`;
      checkObjectFields(range, path, ['startDate', 'endDate']);
      if (typeof range.startDate !== 'string' || typeof range.endDate !== 'string') throw Error(`${path}: укажите startDate и endDate строками дд.мм.гггг.`);
    });
  }
  checkDateFields(data, 'Запрос', ['calculationDate']);
}

function checkObjectFields(value, path, allowedFields) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw Error(`${path}: ожидается объект.`);
  const extra = Object.keys(value).filter(key => !allowedFields.includes(key));
  if (extra.length) throw Error(`${path}: неизвестные поля ${extra.join(', ')}. Они не могут быть перенесены в форму.`);
}

function checkDateFields(value, path, fields) {
  for (const field of fields) {
    if (field in value && value[field] !== null && typeof value[field] !== 'string') {
      throw Error(`${path}.${field}: укажите дату строкой дд.мм.гггг.`);
    }
  }
}

function fillRequestData(data) {
  checkRequestData(data);
  reportForm.reset();
  stageNumber.value = data.stageNumber ?? '';
  contractNumber.value = data.contractNumber?.slice(4) ?? '';
  stageAmount.value = data.stageAmount ?? '';
  formatAmount(stageAmount);
  reportPlannedSubmissionDate.value = data.reportPlannedSubmissionDate ?? '';
  reportActualSubmissionDate.value = data.reportActualSubmissionDate ?? '';
  for (const panel of reportPanels) {
    const report = data[panel.dataset.report];
    const rows = panel.querySelector('[data-form-field="cases"]');
    while (rows.rows.length < report.cases.length) addCaseRow(panel);
    report.cases.forEach((item, index) => {
      const row = rows.rows[index];
      row.querySelector('[data-form-field="returnedForRevisionDate"]').value = item.returnedForRevisionDate ?? '';
      row.querySelector('[data-form-field="actualSubmissionDate"]').value = item.actualSubmissionDate ?? '';
      row.querySelector('[data-form-field="remarksCorrected"]').checked = item.remarksCorrected ?? true;
    });
    panel.querySelector('[name="reportApprovalDate"]').value = report.reportApprovalDate ?? '';
    for (const day of report.daysToExclude.days) addDayToExclude(panel, day);
    for (const range of report.daysToExclude.dayRanges) addDayRangeToExclude(panel, range.startDate, range.endDate);
    updateCaseNumbers(panel);
    updatePanelPlannedDates(panel);
  }
  plannedCompletionDate.value = data.technicalReport.plannedCompletionDate ?? '';
  actualCompletionDate.value = data.technicalReport.actualCompletionDate ?? '';
  calculationDate.value = data.calculationDate ?? '';
  validateForm();
}
