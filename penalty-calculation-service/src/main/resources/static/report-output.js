function renderCalculation(penaltyCalculationResult) {
  calculationResult.querySelector('[data-result-field="stageNumber"]').textContent = penaltyCalculationResult.stageNumber;
  calculationResult.querySelector('[data-result-field="contractNumber"]').textContent = penaltyCalculationResult.contractNumber;
  calculationResult.querySelector('[data-result-field="stageAmount"]').textContent = penaltyCalculationResult.stageAmount;
  calculationResult.querySelector('[data-result-field="calculationDate"]').textContent = penaltyCalculationResult.calculationDate;
  calculationResult.querySelector('[data-result-field="keyRatePercent"]').textContent = penaltyCalculationResult.keyRatePercent;

  const reportingDelayPanel = calculationResult.querySelector('[data-stage="reportingDelayPenalty"]');
  const reportingDelayPenalty = penaltyCalculationResult.reportingDelayPenalty;
  const initialSubmissionDays = reportingDelayPanel.querySelector('[data-initial-submission-days]');
  initialSubmissionDays.querySelector('[data-result-field="initialSubmissionDaysForCalculation.length"]').textContent = reportingDelayPenalty.initialSubmissionDaysForCalculation.length;
  initialSubmissionDays.querySelector('[data-result-field="initialSubmissionDaysForCalculation"]').textContent = reportingDelayPenalty.initialSubmissionDaysForCalculation.join(', ') || 'нет';

  const caseRows = calculationResult.querySelector('[data-case-rows]');
  caseRows.replaceChildren();
  fillReportRevision(reportingDelayPanel.querySelector('[data-result-report="grantReport"]'), reportingDelayPenalty.grantReport, caseRows);
  fillReportRevision(reportingDelayPanel.querySelector('[data-result-report="extraBudgetReport"]'), reportingDelayPenalty.extraBudgetReport, caseRows);
  fillReportRevision(reportingDelayPanel.querySelector('[data-result-report="technicalReport"]'), reportingDelayPenalty.technicalReport, caseRows);

  const totalDays = reportingDelayPanel.querySelector('[data-reporting-delay-total-days]');
  totalDays.querySelectorAll('[data-result-exclusion-details]').forEach(details => details.hidden = reportingDelayPenalty.totalDaysToExclude.length === 0);
  totalDays.querySelector('[data-result-field="totalOverdueDays.length"]').textContent = reportingDelayPenalty.totalOverdueDays.length;
  totalDays.querySelector('[data-result-field="totalOverdueDays"]').textContent = reportingDelayPenalty.totalOverdueDays.join(', ') || 'нет';
  totalDays.querySelector('[data-result-field="totalDaysToExclude.length"]').textContent = reportingDelayPenalty.totalDaysToExclude.length;
  totalDays.querySelector('[data-result-field="totalDaysToExclude"]').textContent = reportingDelayPenalty.totalDaysToExclude.join(', ') || 'нет';
  totalDays.querySelector('[data-result-field="totalDaysForCalculation.length"]').textContent = reportingDelayPenalty.totalDaysForCalculation.length;
  totalDays.querySelector('[data-result-field="totalDaysForCalculation"]').textContent = reportingDelayPenalty.totalDaysForCalculation.join(', ') || 'нет';
  const keyRatePercent = penaltyCalculationResult.keyRatePercent.replace(/%$/, '');
  const reportingDelayFormula = reportingDelayPanel.querySelector('[data-reporting-delay-formula]');
  reportingDelayFormula.querySelector('[data-result-field="stageAmount"]').textContent = penaltyCalculationResult.stageAmount;
  reportingDelayFormula.querySelector('[data-result-field="keyRatePercent"]').textContent = keyRatePercent;
  reportingDelayFormula.querySelector('[data-result-field="totalDaysForCalculation.length"]').textContent = reportingDelayPenalty.totalDaysForCalculation.length;
  reportingDelayPanel.querySelector('[data-result-field="penalty"]').textContent = reportingDelayPenalty.penalty;

  const stageCompletionPanel = calculationResult.querySelector('[data-stage="stageCompletionPenalty"]');
  stageCompletionPanel.querySelector('[data-result-field="overdueDays.length"]').textContent = penaltyCalculationResult.stageCompletionPenalty.overdueDays.length;
  stageCompletionPanel.querySelector('[data-result-field="overdueDays"]').textContent = penaltyCalculationResult.stageCompletionPenalty.overdueDays.join(', ') || 'нет';
  const stageCompletionFormula = stageCompletionPanel.querySelector('[data-stage-completion-formula]');
  stageCompletionFormula.querySelector('[data-result-field="stageAmount"]').textContent = penaltyCalculationResult.stageAmount;
  stageCompletionFormula.querySelector('[data-result-field="keyRatePercent"]').textContent = keyRatePercent;
  stageCompletionFormula.querySelector('[data-result-field="overdueDays.length"]').textContent = penaltyCalculationResult.stageCompletionPenalty.overdueDays.length;
  stageCompletionPanel.querySelector('[data-result-field="penalty"]').textContent = penaltyCalculationResult.stageCompletionPenalty.penalty;

  calculationResult.querySelector('.calculation-summary [data-result-field="totalDaysForCalculation.length"]').textContent = penaltyCalculationResult.totalDaysForCalculation.length;
  calculationResult.querySelector('[data-result-field="reportingDelayPenalty.penalty"]').textContent = reportingDelayPenalty.penalty;
  calculationResult.querySelector('[data-result-field="stageCompletionPenalty.penalty"]').textContent = penaltyCalculationResult.stageCompletionPenalty.penalty;
  calculationResult.querySelector('[data-result-field="totalPenalty"]').textContent = penaltyCalculationResult.totalPenalty;

  calculationResult.querySelector('details').open = false;
  calculationResult.hidden = false;
}

function fillCalculationDays(panel, calculationDays) {
  const daysToExclude = calculationDays.daysToExclude;
  panel.querySelectorAll('[data-result-exclusion-details]').forEach(details => details.hidden = daysToExclude.length === 0);
  panel.querySelector('[data-result-field="overdueDays.length"]').textContent = calculationDays.overdueDays.length;
  panel.querySelector('[data-result-field="overdueDays"]').textContent = calculationDays.overdueDays.join(', ') || 'нет';
  panel.querySelector('[data-result-field="daysToExclude.length"]').textContent = daysToExclude.length;
  panel.querySelector('[data-result-field="daysToExclude"]').textContent = daysToExclude.join(', ') || 'нет';
  panel.querySelector('[data-result-field="daysForCalculation.length"]').textContent = calculationDays.daysForCalculation.length;
  panel.querySelector('[data-result-field="daysForCalculation"]').textContent = calculationDays.daysForCalculation.join(', ') || 'нет';
}

function fillReportRevision(row, reportRevision, caseRows) {
  const reportName = row.querySelector('[data-result-report-name]').textContent;
  reportRevision.cases.forEach(reportRevisionCase => caseRows.append(renderReportRevisionCaseRow(reportRevisionCase, reportName)));
  fillCalculationDays(row.querySelector('[data-calculation-days]'), reportRevision);
}

function renderReportRevisionCaseRow(reportRevisionCase, reportName) {
  const row = document.querySelector('#result-case-template').content.cloneNode(true);
  row.querySelector('[data-result-field="name"]').textContent = reportName;
  row.querySelector('[data-result-field="caseNumber"]').textContent = reportRevisionCase.caseNumber;
  row.querySelector('[data-result-field="returnedForRevisionDate"]').textContent = reportRevisionCase.returnedForRevisionDate;
  row.querySelector('[data-result-field="plannedSubmissionDate"]').textContent = reportRevisionCase.plannedSubmissionDate;
  row.querySelector('[data-result-field="actualSubmissionDate"]').textContent = reportRevisionCase.actualSubmissionDate;
  row.querySelector('[data-result-remarks-corrected="true"]').hidden = !reportRevisionCase.remarksCorrected;
  row.querySelector('[data-result-remarks-corrected="false"]').hidden = reportRevisionCase.remarksCorrected;
  fillCalculationDays(row.querySelector('[data-calculation-days]'), reportRevisionCase);
  return row;
}
