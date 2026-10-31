package ru.inno_sc.penaltycalculation.service.converter

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import ru.inno_sc.penaltycalculation.api.penalty.response.AllReportsRevisionPenaltyDto
import ru.inno_sc.penaltycalculation.api.penalty.response.InitialSubmissionPenaltyDto
import ru.inno_sc.penaltycalculation.api.penalty.response.PenaltyCalculationResultDto
import ru.inno_sc.penaltycalculation.api.penalty.response.ReportRevisionCaseDto
import ru.inno_sc.penaltycalculation.api.penalty.response.SingleReportRevisionPenaltyDto
import ru.inno_sc.penaltycalculation.api.penalty.response.StageCompletionPenaltyDto
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.domain.PenaltyCalculationResult
import ru.inno_sc.penaltycalculation.service.domain.SingleReportRevisionPenalty

fun PenaltyCalculationResult.toDto(): PenaltyCalculationResultDto = PenaltyCalculationResultDto(
  stageNumber = stageNumber,
  contractNumber = contractNumber,
  stageAmount = money(stageAmount),
  calculationDate = calculationDate.format(DATE_FORMATTER),
  keyRatePercent = "${keyRatePercent.stripTrailingZeros().toPlainString()}%",
  initialSubmissionPenalty = InitialSubmissionPenaltyDto(
    overdueDays = initialSubmissionPenalty.overdueDays,
    daysToExcludeManually = initialSubmissionPenalty.daysToExcludeManually,
    daysForCalculation = initialSubmissionPenalty.daysForCalculation,
  ),
  revisionsPenalty = AllReportsRevisionPenaltyDto(
    grantReport = revisionsPenalty.grantReport.toDto(),
    extraBudgetReport = revisionsPenalty.extraBudgetReport.toDto(),
    technicalReport = revisionsPenalty.technicalReport.toDto(),
    overdueDays = revisionsPenalty.totalOverdueDays,
    daysToExcludeManually = revisionsPenalty.totalDaysToExcludeManually,
    daysForCalculation = revisionsPenalty.totalDaysForCalculation,
  ),
  stageCompletionPenalty = StageCompletionPenaltyDto(
    overdueDays = stageCompletionPenalty.overdueDays,
  ),
  totalDaysForCalculation = totalDaysForCalculation,
  totalPenalty = money(totalPenalty),
)

private fun SingleReportRevisionPenalty.toDto() = SingleReportRevisionPenaltyDto(
  overdueDays = overdueDays,
  cases = cases.map { case ->
    ReportRevisionCaseDto(
      caseNumber = case.caseNumber,
      returnedForRevisionDate = case.returnedForRevisionDate.format(DATE_FORMATTER),
      plannedSubmissionDate = case.plannedSubmissionDate.format(DATE_FORMATTER),
      actualSubmissionDate = case.actualSubmissionDate.format(DATE_FORMATTER),
      remarksCorrected = case.remarksCorrected,
      overdueDays = case.overdueDays,
      daysToExcludeManually = case.daysToExcludeManually,
      daysForCalculation = case.daysForCalculation,
    )
  },
  daysToExcludeManually = daysToExcludeManually,
  daysForCalculation = daysForCalculation,
)

private fun money(value: BigDecimal): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU")).format(value)
