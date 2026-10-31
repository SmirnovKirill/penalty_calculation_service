function getRequestData() {
  return {
    formatVersion: 1,
    stageNumber: stageNumber.value ? Number(stageNumber.value) : null,
    contractNumber: `Г-00${contractNumber.value}`,
    stageAmount: stageAmount.value.replace(/\s/g, '').replace(',', '.'),
    reportPlannedSubmissionDate: reportPlannedSubmissionDate.value,
    reportActualSubmissionDate: reportActualSubmissionDate.value,
    grantReport: reportData(grantReport),
    extraBudgetReport: reportData(extraBudgetReport),
    technicalReport: {
      ...reportData(technicalReport),
      plannedCompletionDate: plannedCompletionDate.value,
      actualCompletionDate: actualCompletionDate.value,
    },
    daysToExcludeManually: {
      days: [...daysToExcludeManually.querySelectorAll('[name="daysToExcludeManually"]')].map(dayToExcludeManually => dayToExcludeManually.value),
      dayRanges: [...dayRangesToExcludeManually.children].map(row => ({
        startDate: row.querySelector('[name="startDate"]').value,
        endDate: row.querySelector('[name="endDate"]').value,
      })),
    },
    calculationDate: calculationDate.value,
  };
}

function reportData(panel) {
  return {
    cases: [...panel.querySelector('[data-form-field="cases"]').rows].map(row => ({
      returnedForRevisionDate: row.querySelector('[data-form-field="returnedForRevisionDate"]').value,
      actualSubmissionDate: row.querySelector('[data-form-field="actualSubmissionDate"]').value,
      remarksCorrected: row.querySelector('[data-form-field="remarksCorrected"]').checked,
    })),
    reportApprovalDate: panel.querySelector('[name="reportApprovalDate"]').value,
  };
}

function checkRequestData(data) {
  const reports = ['grantReport', 'extraBudgetReport', 'technicalReport'];
  checkObjectFields(data, 'Запрос', ['formatVersion', 'stageNumber', 'contractNumber', 'stageAmount', 'reportPlannedSubmissionDate', 'reportActualSubmissionDate', ...reports, 'daysToExcludeManually', 'calculationDate']);
  if ('stageNumber' in data && data.stageNumber !== null && (!Number.isInteger(data.stageNumber) || data.stageNumber < 1 || data.stageNumber > 10)) throw Error('stageNumber: укажите номер этапа целым числом от 1 до 10.');
  if ('contractNumber' in data && (typeof data.contractNumber !== 'string' || !/^Г-00\d{0,2}$/.test(data.contractNumber))) throw Error('contractNumber: ожидается номер с префиксом Г-00 и двумя цифрами.');
  if ('stageAmount' in data && !['string', 'number'].includes(typeof data.stageAmount)) throw Error('stageAmount: ожидается сумма строкой или числом.');
  if (typeof data.stageAmount === 'number' && Math.abs(data.stageAmount) > Number.MAX_SAFE_INTEGER) throw Error('stageAmount: передайте большую сумму строкой для сохранения точности.');
  if ('stageAmount' in data && !/^\d*([.,]\d{0,2})?$/.test(String(data.stageAmount))) throw Error('stageAmount: укажите только цифры и до двух знаков после запятой.');
  checkDateFields(data, 'Запрос', ['reportPlannedSubmissionDate', 'reportActualSubmissionDate']);
  for (const name of reports) {
    if (!(name in data)) continue;
    const report = data[name];
    const dateFields = ['reportApprovalDate', ...(name === 'technicalReport' ? ['plannedCompletionDate', 'actualCompletionDate'] : [])];
    checkObjectFields(report, name, ['cases', ...dateFields]);
    if ('cases' in report) {
      if (!Array.isArray(report.cases)) throw Error(`${name}.cases: ожидается массив кейсов.`);
      report.cases.forEach((item, index) => {
        const path = `${name}.cases[${index}]`;
        checkObjectFields(item, path, ['returnedForRevisionDate', 'actualSubmissionDate', 'remarksCorrected']);
        checkDateFields(item, path, ['returnedForRevisionDate', 'actualSubmissionDate']);
        if ('remarksCorrected' in item && typeof item.remarksCorrected !== 'boolean') throw Error(`${path}.remarksCorrected: ожидается true или false.`);
      });
    }
    checkDateFields(report, name, dateFields);
  }
  if ('daysToExcludeManually' in data) {
    const excluded = data.daysToExcludeManually;
    checkObjectFields(excluded, 'daysToExcludeManually', ['days', 'dayRanges']);
    if ('days' in excluded && (!Array.isArray(excluded.days) || excluded.days.some(value => typeof value !== 'string'))) throw Error('daysToExcludeManually.days: ожидается массив строк с датами.');
    if ('dayRanges' in excluded) {
      if (!Array.isArray(excluded.dayRanges)) throw Error('daysToExcludeManually.dayRanges: ожидается массив интервалов дат.');
      excluded.dayRanges.forEach((range, index) => {
        const path = `daysToExcludeManually.dayRanges[${index}]`;
        checkObjectFields(range, path, ['startDate', 'endDate']);
        if (typeof range.startDate !== 'string' || typeof range.endDate !== 'string') throw Error(`${path}: укажите startDate и endDate строками дд.мм.гггг.`);
      });
    }
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
    const report = data[panel.dataset.report] ?? {};
    const rows = panel.querySelector('[data-form-field="cases"]');
    const cases = report.cases ?? [];
    while (rows.rows.length < cases.length) addCaseRow(panel);
    cases.forEach((item, index) => {
      const row = rows.rows[index];
      const returnedForRevisionDate = row.querySelector('[data-form-field="returnedForRevisionDate"]');
      returnedForRevisionDate.value = item.returnedForRevisionDate ?? '';
      const actualSubmissionDate = row.querySelector('[data-form-field="actualSubmissionDate"]');
      actualSubmissionDate.value = item.actualSubmissionDate ?? '';
      const remarksCorrected = row.querySelector('[data-form-field="remarksCorrected"]');
      remarksCorrected.checked = item.remarksCorrected ?? true;
    });
    const reportApprovalDate = panel.querySelector('[name="reportApprovalDate"]');
    reportApprovalDate.value = report.reportApprovalDate ?? '';
    updateCaseNumbers(panel);
    updatePanelPlannedDates(panel);
  }
  plannedCompletionDate.value = data.technicalReport?.plannedCompletionDate ?? '';
  actualCompletionDate.value = data.technicalReport?.actualCompletionDate ?? '';
  for (const value of data.daysToExcludeManually.days) addDayToExcludeManually(value);
  for (const range of data.daysToExcludeManually.dayRanges) addDayRangeToExcludeManually(range.startDate, range.endDate);
  calculationDate.value = data.calculationDate ?? '';
  validateForm();
}
