package ru.inno_sc.penaltycalculation.service.converter

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import ru.inno_sc.penaltycalculation.api.penalty.response.PenaltyCalculationResultDto
import ru.inno_sc.penaltycalculation.api.penalty.response.ReportRevisionCaseDto
import ru.inno_sc.penaltycalculation.api.penalty.response.ReportRevisionDto
import ru.inno_sc.penaltycalculation.api.penalty.response.ReportingDelayPenaltyDto
import ru.inno_sc.penaltycalculation.api.penalty.response.StageCompletionPenaltyDto
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.domain.PenaltyCalculationResult
import ru.inno_sc.penaltycalculation.service.domain.ReportRevision

fun PenaltyCalculationResult.toDto(): PenaltyCalculationResultDto = PenaltyCalculationResultDto(
  stageNumber = stageNumber,
  contractNumber = contractNumber,
  stageAmount = money(stageAmount),
  calculationDate = calculationDate.format(DATE_FORMATTER),
  keyRatePercent = "${keyRatePercent.stripTrailingZeros().toPlainString()}%",
  reportingDelayPenalty = ReportingDelayPenaltyDto(
    initialSubmissionDaysForCalculation = reportingDelayPenalty.initialSubmissionDaysForCalculation,
    grantReport = reportingDelayPenalty.grantReport.toDto(),
    extraBudgetReport = reportingDelayPenalty.extraBudgetReport.toDto(),
    technicalReport = reportingDelayPenalty.technicalReport.toDto(),
    totalOverdueDays = reportingDelayPenalty.totalOverdueDays,
    totalDaysToExclude = reportingDelayPenalty.totalDaysToExclude,
    totalDaysForCalculation = reportingDelayPenalty.totalDaysForCalculation,
    penalty = money(reportingDelayPenalty.penalty),
  ),
  stageCompletionPenalty = StageCompletionPenaltyDto(
    overdueDays = stageCompletionPenalty.overdueDays,
    penalty = money(stageCompletionPenalty.penalty),
  ),
  totalDaysForCalculation = totalDaysForCalculation,
  totalPenalty = money(totalPenalty),
)

private fun ReportRevision.toDto() = ReportRevisionDto(
  cases = cases.map { case ->
    ReportRevisionCaseDto(
      caseNumber = case.caseNumber,
      returnedForRevisionDate = case.returnedForRevisionDate.format(DATE_FORMATTER),
      plannedSubmissionDate = case.plannedSubmissionDate.format(DATE_FORMATTER),
      actualSubmissionDate = case.actualSubmissionDate.format(DATE_FORMATTER),
      remarksCorrected = case.remarksCorrected,
      overdueDays = case.overdueDays,
      daysToExclude = case.daysToExclude,
      daysForCalculation = case.daysForCalculation,
    )
  },
  overdueDays = overdueDays,
  daysToExclude = daysToExclude,
  daysForCalculation = daysForCalculation,
)

private fun money(value: BigDecimal): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU")).format(value)
