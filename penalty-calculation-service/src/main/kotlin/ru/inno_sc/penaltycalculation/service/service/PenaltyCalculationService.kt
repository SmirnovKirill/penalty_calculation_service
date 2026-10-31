package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.math.RoundingMode.UP
import java.time.Clock
import java.time.LocalDate
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.api.penalty.request.DaysToExcludeManuallyDto
import ru.inno_sc.penaltycalculation.api.penalty.request.ReportCaseDto
import ru.inno_sc.penaltycalculation.service.domain.AllReportsRevisionPenalty
import ru.inno_sc.penaltycalculation.service.domain.InitialSubmissionPenalty
import ru.inno_sc.penaltycalculation.service.domain.PenaltyCalculationResult
import ru.inno_sc.penaltycalculation.service.domain.ReportRevisionCase
import ru.inno_sc.penaltycalculation.service.domain.SingleReportRevisionPenalty
import ru.inno_sc.penaltycalculation.service.domain.StageCompletionPenalty
import ru.inno_sc.penaltycalculation.service.validation.PenaltyCalculationValidator

class PenaltyCalculationService(
  private val reportRevisionPlannedDateService: ReportRevisionPlannedDateService,
  private val keyRates: KeyRateService,
  private val validator: PenaltyCalculationValidator,
  private val clock: Clock,
) {
  fun calculate(reports: CalculatePenaltyRequestDto): PenaltyCalculationResult {
    validator.validate(reports, LocalDate.now(clock))
    val keyRatePercent = keyRates.rateOn(reports.calculationDate)

    val initialSubmissionPenalty = calculateInitialSubmissionPenalty(reports)
    val revisionsPenalty = calculateAllReportsRevisionPenalty(reports)
    val stageCompletionPenalty = calculateStageCompletionPenalty(reports)
    val totalDaysForCalculation = initialSubmissionPenalty.daysForCalculation +
      revisionsPenalty.totalDaysForCalculation + stageCompletionPenalty.overdueDays

    return PenaltyCalculationResult(
      contractNumber = reports.contractNumber,
      calculationDate = reports.calculationDate,
      stageAmount = reports.stageAmount,
      keyRatePercent = keyRatePercent,
      initialSubmissionPenalty = initialSubmissionPenalty,
      revisionsPenalty = revisionsPenalty,
      stageCompletionPenalty = stageCompletionPenalty,
      stageNumber = reports.stageNumber,
      totalDaysForCalculation = totalDaysForCalculation,
      totalPenalty = calculatePenalty(reports.stageAmount, keyRatePercent, totalDaysForCalculation.size),
    )
  }

  fun calculateInitialSubmissionPenalty(reports: CalculatePenaltyRequestDto): InitialSubmissionPenalty {
    val overdueDays = getCalendarOverdueDays(reports.reportPlannedSubmissionDate, reports.reportActualSubmissionDate)
    val daysToExcludeManually = getDaysToExcludeManually(reports.daysToExcludeManually).toSet()
    val daysForCalculation = overdueDays.filterNot { it in daysToExcludeManually }
    return InitialSubmissionPenalty(
      overdueDays = overdueDays,
      daysToExcludeManually = overdueDays.filter { it in daysToExcludeManually },
      daysForCalculation = daysForCalculation,
      penalty = calculatePenalty(reports.stageAmount, keyRates.rateOn(reports.calculationDate), daysForCalculation.size),
    )
  }

  private fun getCalendarOverdueDays(planned: LocalDate, actual: LocalDate): List<LocalDate> = if (actual.isAfter(planned)) {
    planned.plusDays(1).datesUntil(actual.plusDays(1)).toList()
  } else {
    emptyList()
  }

  fun calculateAllReportsRevisionPenalty(reports: CalculatePenaltyRequestDto): AllReportsRevisionPenalty {
    val daysToExcludeManually = getDaysToExcludeManually(reports.daysToExcludeManually).toSet()
    val grantReport = calculateSingleReportRevisionPenalty(reports.grantReport.cases, daysToExcludeManually)
    val extraBudgetReport = calculateSingleReportRevisionPenalty(reports.extraBudgetReport.cases, daysToExcludeManually)
    val technicalReport = calculateSingleReportRevisionPenalty(reports.technicalReport.cases, daysToExcludeManually)
    val reportPenalties = listOf(grantReport, extraBudgetReport, technicalReport)
    val totalOverdueDays = reportPenalties.flatMap { it.overdueDays }.distinct().sorted()
    val totalDaysToExcludeManually = reportPenalties.flatMap { it.daysToExcludeManually }.distinct().sorted()
    val totalDaysForCalculation = reportPenalties.flatMap { it.daysForCalculation }.distinct().sorted()
    return AllReportsRevisionPenalty(
      grantReport = grantReport,
      extraBudgetReport = extraBudgetReport,
      technicalReport = technicalReport,
      totalOverdueDays = totalOverdueDays,
      totalDaysToExcludeManually = totalDaysToExcludeManually,
      totalDaysForCalculation = totalDaysForCalculation,
      penalty = calculatePenalty(reports.stageAmount, keyRates.rateOn(reports.calculationDate), totalDaysForCalculation.size),
    )
  }

  private fun calculateSingleReportRevisionPenalty(cases: List<ReportCaseDto>, daysToExcludeManually: Set<LocalDate>,): SingleReportRevisionPenalty {
    val reportRevisionCases = calculateReportRevisionCase(cases, daysToExcludeManually)
    return SingleReportRevisionPenalty(
      cases = reportRevisionCases,
      overdueDays = reportRevisionCases.flatMap { it.overdueDays }.distinct().sorted(),
      daysToExcludeManually = reportRevisionCases.flatMap { it.daysToExcludeManually }.distinct().sorted(),
      daysForCalculation = reportRevisionCases.flatMap { it.daysForCalculation }.distinct().sorted(),
    )
  }

  private fun calculateReportRevisionCase(cases: List<ReportCaseDto>, daysToExcludeManually: Set<LocalDate>): List<ReportRevisionCase> {
    val result = mutableListOf<ReportRevisionCase>()
    for ((returnedForRevisionDate, actualSubmissionDate, remarksCorrected) in cases) {
      val previous = result.lastOrNull()
      val caseNumber = when {
        previous == null -> 1
        previous.remarksCorrected -> previous.caseNumber + 1
        else -> previous.caseNumber
      }
      val plannedSubmissionDate = if (previous != null && !previous.remarksCorrected) {
        // Неисправленная попытка не завершает кейс и не даёт нового срока.
        previous.plannedSubmissionDate
      } else {
        reportRevisionPlannedDateService.getReportRevisionPlannedDate(returnedForRevisionDate, caseNumber)
      }
      val overdueDays = getCalendarOverdueDays(plannedSubmissionDate, actualSubmissionDate)
      val daysForCalculation = overdueDays.filterNot { it in daysToExcludeManually }
      result += ReportRevisionCase(
        caseNumber = caseNumber,
        returnedForRevisionDate = returnedForRevisionDate,
        plannedSubmissionDate = plannedSubmissionDate,
        actualSubmissionDate = actualSubmissionDate,
        remarksCorrected = remarksCorrected,
        overdueDays = overdueDays,
        daysToExcludeManually = overdueDays.filter { it in daysToExcludeManually },
        daysForCalculation = daysForCalculation,
      )
    }
    return result
  }

  fun calculateStageCompletionPenalty(reports: CalculatePenaltyRequestDto): StageCompletionPenalty {
    val technicalReport = reports.technicalReport
    val overdueDays = if (technicalReport.actualCompletionDate.isAfter(technicalReport.plannedCompletionDate)) {
      technicalReport.plannedCompletionDate.plusDays(1).datesUntil(technicalReport.actualCompletionDate).toList()
    } else {
      emptyList()
    } // Тут свои правила расчета, это корректная логика.
    return StageCompletionPenalty(
      overdueDays = overdueDays,
      penalty = calculatePenalty(reports.stageAmount, keyRates.rateOn(reports.calculationDate), overdueDays.size),
    )
  }

  private fun getDaysToExcludeManually(exclusions: DaysToExcludeManuallyDto): List<LocalDate> {
    val daysFromRanges = exclusions.dayRanges.flatMap { it.startDate.datesUntil(it.endDate.plusDays(1)).toList() }
    return (exclusions.days + daysFromRanges).distinct()
  }

  private fun calculatePenalty(stageAmount: BigDecimal, keyRatePercent: BigDecimal, daysCount: Int): BigDecimal =
    stageAmount.multiply(keyRatePercent).multiply(BigDecimal.valueOf(daysCount.toLong())).divide(PENALTY_DIVISOR, MONEY_SCALE, UP)

  private companion object {
    val PENALTY_DIVISOR = BigDecimal("30000")
    const val MONEY_SCALE = 2
  }
}
