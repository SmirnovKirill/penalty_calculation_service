package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.math.RoundingMode.UP
import java.time.Clock
import java.time.LocalDate
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.api.penalty.request.DaysToExcludeDto
import ru.inno_sc.penaltycalculation.api.penalty.request.ReportCaseDto
import ru.inno_sc.penaltycalculation.service.domain.PenaltyCalculationResult
import ru.inno_sc.penaltycalculation.service.domain.ReportRevision
import ru.inno_sc.penaltycalculation.service.domain.ReportRevisionCase
import ru.inno_sc.penaltycalculation.service.domain.ReportingDelayPenalty
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

    val reportingDelayPenalty = calculateReportingDelayPenalty(reports)
    val stageCompletionPenalty = calculateStageCompletionPenalty(reports)
    val totalDaysForCalculation = reportingDelayPenalty.totalDaysForCalculation + stageCompletionPenalty.overdueDays

    return PenaltyCalculationResult(
      contractNumber = reports.contractNumber,
      calculationDate = reports.calculationDate,
      stageAmount = reports.stageAmount,
      keyRatePercent = keyRatePercent,
      reportingDelayPenalty = reportingDelayPenalty,
      stageCompletionPenalty = stageCompletionPenalty,
      stageNumber = reports.stageNumber,
      totalDaysForCalculation = totalDaysForCalculation,
      totalPenalty = reportingDelayPenalty.penalty + stageCompletionPenalty.penalty,
    )
  }

  fun calculateReportingDelayPenalty(reports: CalculatePenaltyRequestDto): ReportingDelayPenalty {
    val initialSubmissionDaysForCalculation =
      getCalendarOverdueDays(reports.reportPlannedSubmissionDate, reports.reportActualSubmissionDate)
    val grantReport =
      getReportRevision(reports.grantReport.cases, getDaysToExclude(reports.grantReport.daysToExclude).toSet())
    val extraBudgetReport =
      getReportRevision(reports.extraBudgetReport.cases, getDaysToExclude(reports.extraBudgetReport.daysToExclude).toSet())
    val technicalReport =
      getReportRevision(reports.technicalReport.cases, getDaysToExclude(reports.technicalReport.daysToExclude).toSet())
    val reportRevisions = listOf(grantReport, extraBudgetReport, technicalReport)
    val totalOverdueDays = (
      initialSubmissionDaysForCalculation + reportRevisions.flatMap {
        it.overdueDays
      }
      ).distinct().sorted()
    val totalDaysForCalculation = (
      initialSubmissionDaysForCalculation +
        reportRevisions.flatMap { it.daysForCalculation }
      ).distinct().sorted()
    val totalDaysToExclude = totalOverdueDays.filterNot { it in totalDaysForCalculation }
    return ReportingDelayPenalty(
      initialSubmissionDaysForCalculation = initialSubmissionDaysForCalculation,
      grantReport = grantReport,
      extraBudgetReport = extraBudgetReport,
      technicalReport = technicalReport,
      totalOverdueDays = totalOverdueDays,
      totalDaysToExclude = totalDaysToExclude,
      totalDaysForCalculation = totalDaysForCalculation,
      penalty = calculatePenalty(reports.stageAmount, keyRates.rateOn(reports.calculationDate), totalDaysForCalculation.size),
    )
  }

  private fun getDaysToExclude(exclusions: DaysToExcludeDto): List<LocalDate> {
    val daysFromRanges = exclusions.dayRanges.flatMap { it.startDate.datesUntil(it.endDate.plusDays(1)).toList() }
    return (exclusions.days + daysFromRanges).distinct()
  }

  private fun getCalendarOverdueDays(planned: LocalDate, actual: LocalDate): List<LocalDate> = if (actual.isAfter(planned)) {
    planned.plusDays(1).datesUntil(actual.plusDays(1)).toList()
  } else {
    emptyList()
  }

  private fun getReportRevision(cases: List<ReportCaseDto>, daysToExclude: Set<LocalDate>): ReportRevision {
    val reportRevisionCases = calculateReportRevisionCase(cases, daysToExclude)
    return ReportRevision(
      cases = reportRevisionCases,
      overdueDays = reportRevisionCases.flatMap { it.overdueDays }.distinct().sorted(),
      daysToExclude = reportRevisionCases.flatMap { it.daysToExclude }.distinct().sorted(),
      daysForCalculation = reportRevisionCases.flatMap { it.daysForCalculation }.distinct().sorted(),
    )
  }

  private fun calculateReportRevisionCase(cases: List<ReportCaseDto>, daysToExclude: Set<LocalDate>): List<ReportRevisionCase> {
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
      val daysForCalculation = overdueDays.filterNot { it in daysToExclude }
      result += ReportRevisionCase(
        caseNumber = caseNumber,
        returnedForRevisionDate = returnedForRevisionDate,
        plannedSubmissionDate = plannedSubmissionDate,
        actualSubmissionDate = actualSubmissionDate,
        remarksCorrected = remarksCorrected,
        overdueDays = overdueDays,
        daysToExclude = overdueDays.filter { it in daysToExclude },
        daysForCalculation = daysForCalculation,
      )
    }
    return result
  }

  private fun calculatePenalty(stageAmount: BigDecimal, keyRatePercent: BigDecimal, daysCount: Int): BigDecimal =
    stageAmount.multiply(keyRatePercent).multiply(BigDecimal.valueOf(daysCount.toLong())).divide(PENALTY_DIVISOR, MONEY_SCALE, UP)

  fun calculateStageCompletionPenalty(reports: CalculatePenaltyRequestDto): StageCompletionPenalty {
    val technicalReport = reports.technicalReport
    val overdueDays =
      getCalendarOverdueDays(technicalReport.plannedCompletionDate, technicalReport.actualCompletionDate)
    return StageCompletionPenalty(
      overdueDays = overdueDays,
      penalty = calculatePenalty(reports.stageAmount, keyRates.rateOn(reports.calculationDate), overdueDays.size),
    )
  }

  private companion object {
    val PENALTY_DIVISOR = BigDecimal("30000")
    const val MONEY_SCALE = 2
  }
}
