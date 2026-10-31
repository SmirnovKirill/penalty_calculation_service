function renderCalculation(data) {
  const report = calculationResult;
  report.querySelector('[data-result-field="stageNumber"]').textContent = data.stageNumber;
  report.querySelector('[data-result-field="contractNumber"]').textContent = data.contractNumber;
  report.querySelector('[data-result-field="stageAmount"]').textContent = data.stageAmount;
  report.querySelector('[data-result-field="calculationDate"]').textContent = data.calculationDate;
  report.querySelector('[data-result-field="keyRatePercent"]').textContent = data.keyRatePercent;

  fillCalculationDays(report.querySelector('[data-stage="initialSubmissionPenalty"] > [data-calculation-days]'), data.initialSubmissionPenalty);

  const caseRows = report.querySelector('[data-case-rows]');
  caseRows.replaceChildren();
  fillReportRevisions(report.querySelector('[data-result-report="grantReport"]'), data.revisionsPenalty.grantReport, caseRows);
  fillReportRevisions(report.querySelector('[data-result-report="extraBudgetReport"]'), data.revisionsPenalty.extraBudgetReport, caseRows);
  fillReportRevisions(report.querySelector('[data-result-report="technicalReport"]'), data.revisionsPenalty.technicalReport, caseRows);
  fillCalculationDays(report.querySelector('[data-stage="revisionsPenalty"] > [data-calculation-days]'), data.revisionsPenalty);

  const completion = report.querySelector('[data-stage="stageCompletionPenalty"]');
  completion.querySelector('[data-result-field="overdueDays.length"]').textContent = data.stageCompletionPenalty.overdueDays.length;
  completion.querySelector('[data-result-field="overdueDays"]').textContent = data.stageCompletionPenalty.overdueDays.join(', ') || 'нет';

  report.querySelector('[data-result-field="totalDaysForCalculation.length"]').textContent = data.totalDaysForCalculation.length;
  const keyRate = data.keyRatePercent.replace(/%$/, '');
  const initialDaysCount = data.initialSubmissionPenalty.daysForCalculation.length;
  const revisionDaysCount = data.revisionsPenalty.daysForCalculation.length;
  const completionDaysCount = data.stageCompletionPenalty.overdueDays.length;
  report.querySelector('[data-result-field="totalFormula"]').textContent =
    `${data.stageAmount} × ${keyRate} / 100 × (${initialDaysCount} + ${revisionDaysCount} + ${completionDaysCount}) / 300 = ${data.totalPenalty}`;

  report.querySelector('details').open = false;
  calculationResult.hidden = false;
}

function fillCalculationDays(root, item) {
  const daysToExcludeManually = item.daysToExcludeManually ?? [];
  root.querySelectorAll('[data-days-to-exclude-manually]').forEach(line => line.hidden = daysToExcludeManually.length === 0);
  root.querySelector('[data-result-field="overdueDays.length"]').textContent = item.overdueDays.length;
  root.querySelector('[data-result-field="overdueDays"]').textContent = item.overdueDays.join(', ') || 'нет';
  root.querySelector('[data-result-field="daysToExcludeManually.length"]').textContent = daysToExcludeManually.length;
  root.querySelector('[data-result-field="daysToExcludeManually"]').textContent = daysToExcludeManually.join(', ') || 'нет';
  root.querySelector('[data-result-field="daysForCalculation.length"]').textContent = item.daysForCalculation.length;
  root.querySelector('[data-result-field="daysForCalculation"]').textContent = item.daysForCalculation.join(', ') || 'нет';
}

function fillReportRevisions(row, data, caseRows) {
  const name = row.querySelector('[data-result-report-name]').textContent;
  data.cases.forEach(caseData => caseRows.append(renderCaseRow(caseData, name)));
  fillCalculationDays(row.querySelector('[data-calculation-days]'), data);
}

function renderCaseRow(data, name) {
  const row = document.querySelector('#result-case-template').content.cloneNode(true);
  row.querySelector('[data-result-field="name"]').textContent = name;
  row.querySelector('[data-result-field="caseNumber"]').textContent = data.caseNumber;
  row.querySelector('[data-result-field="returnedForRevisionDate"]').textContent = data.returnedForRevisionDate;
  row.querySelector('[data-result-field="plannedSubmissionDate"]').textContent = data.plannedSubmissionDate;
  row.querySelector('[data-result-field="actualSubmissionDate"]').textContent = data.actualSubmissionDate;
  row.querySelector('[data-result-remarks-corrected="true"]').hidden = !data.remarksCorrected;
  row.querySelector('[data-result-remarks-corrected="false"]').hidden = data.remarksCorrected;
  fillCalculationDays(row.querySelector('[data-calculation-days]'), data);
  return row;
}
