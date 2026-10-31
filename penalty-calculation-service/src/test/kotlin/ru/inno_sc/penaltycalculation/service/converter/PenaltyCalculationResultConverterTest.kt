package ru.inno_sc.penaltycalculation.service.converter

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.inno_sc.penaltycalculation.api.penalty.response.PenaltyCalculationResultDto
import ru.inno_sc.penaltycalculation.api.penalty.response.ReportRevisionCaseDto
import ru.inno_sc.penaltycalculation.api.penalty.response.ReportRevisionDto
import ru.inno_sc.penaltycalculation.api.penalty.response.ReportingDelayPenaltyDto
import ru.inno_sc.penaltycalculation.api.penalty.response.StageCompletionPenaltyDto
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.domain.PenaltyCalculationResult
import ru.inno_sc.penaltycalculation.service.domain.ReportRevision
import ru.inno_sc.penaltycalculation.service.domain.ReportRevisionCase
import ru.inno_sc.penaltycalculation.service.domain.ReportingDelayPenalty
import ru.inno_sc.penaltycalculation.service.domain.StageCompletionPenalty

class PenaltyCalculationResultConverterTest : AbstractTest() {
  @Test
  fun `converts calculation result to dto`() {
    val initialSubmissionDaysForCalculation = listOf(date("11.11.2025"), date("12.11.2025"))
    val grantReportOverdueDays = listOf(date("18.11.2025"), date("19.11.2025"))
    val grantReportDaysToExclude = listOf(date("19.11.2025"))
    val grantReportDaysForCalculation = listOf(date("18.11.2025"))
    val extraBudgetReportDays = listOf(date("18.11.2025"), date("19.11.2025"))
    val technicalReportDays = listOf(date("20.11.2025"))
    val totalOverdueDays = initialSubmissionDaysForCalculation + grantReportOverdueDays + technicalReportDays
    val totalDaysToExclude = listOf(date("19.11.2025"))
    val totalDaysForCalculation =
      initialSubmissionDaysForCalculation + grantReportDaysForCalculation + technicalReportDays

    val result = PenaltyCalculationResult(
      stageNumber = 4,
      contractNumber = "Г-0029",
      stageAmount = BigDecimal("1000000.25"),
      calculationDate = date("28.10.2026"),
      keyRatePercent = BigDecimal("14.50"),
      reportingDelayPenalty = ReportingDelayPenalty(
        initialSubmissionDaysForCalculation = initialSubmissionDaysForCalculation,
        grantReport = ReportRevision(
          cases = listOf(
            ReportRevisionCase(
              caseNumber = 1,
              returnedForRevisionDate = date("12.11.2025"),
              plannedSubmissionDate = date("17.11.2025"),
              actualSubmissionDate = date("19.11.2025"),
              remarksCorrected = true,
              overdueDays = grantReportOverdueDays,
              daysToExclude = grantReportDaysToExclude,
              daysForCalculation = grantReportDaysForCalculation,
            )
          ),
          overdueDays = grantReportOverdueDays,
          daysToExclude = grantReportDaysToExclude,
          daysForCalculation = grantReportDaysForCalculation,
        ),
        extraBudgetReport = ReportRevision(
          cases = emptyList(),
          overdueDays = extraBudgetReportDays,
          daysToExclude = extraBudgetReportDays,
          daysForCalculation = emptyList(),
        ),
        technicalReport = ReportRevision(
          cases = emptyList(),
          overdueDays = technicalReportDays,
          daysToExclude = emptyList(),
          daysForCalculation = technicalReportDays,
        ),
        totalOverdueDays = totalOverdueDays,
        totalDaysToExclude = totalDaysToExclude,
        totalDaysForCalculation = totalDaysForCalculation,
        penalty = BigDecimal("1933.34"),
      ),
      stageCompletionPenalty = StageCompletionPenalty(
        overdueDays = technicalReportDays,
        penalty = BigDecimal("483.34"),
      ),
      totalDaysForCalculation = totalDaysForCalculation + technicalReportDays,
      totalPenalty = BigDecimal("2416.68"),
    )
    val expected = PenaltyCalculationResultDto(
      stageNumber = 4,
      contractNumber = "Г-0029",
      stageAmount = "1\u00a0000\u00a0000,25\u00a0₽",
      calculationDate = "28.10.2026",
      keyRatePercent = "14.5%",
      reportingDelayPenalty = ReportingDelayPenaltyDto(
        initialSubmissionDaysForCalculation = initialSubmissionDaysForCalculation,
        grantReport = ReportRevisionDto(
          cases = listOf(
            ReportRevisionCaseDto(
              caseNumber = 1,
              returnedForRevisionDate = "12.11.2025",
              plannedSubmissionDate = "17.11.2025",
              actualSubmissionDate = "19.11.2025",
              remarksCorrected = true,
              overdueDays = grantReportOverdueDays,
              daysToExclude = grantReportDaysToExclude,
              daysForCalculation = grantReportDaysForCalculation,
            )
          ),
          overdueDays = grantReportOverdueDays,
          daysToExclude = grantReportDaysToExclude,
          daysForCalculation = grantReportDaysForCalculation,
        ),
        extraBudgetReport = ReportRevisionDto(
          cases = emptyList(),
          overdueDays = extraBudgetReportDays,
          daysToExclude = extraBudgetReportDays,
          daysForCalculation = emptyList(),
        ),
        technicalReport = ReportRevisionDto(
          cases = emptyList(),
          overdueDays = technicalReportDays,
          daysToExclude = emptyList(),
          daysForCalculation = technicalReportDays,
        ),
        totalOverdueDays = totalOverdueDays,
        totalDaysToExclude = totalDaysToExclude,
        totalDaysForCalculation = totalDaysForCalculation,
        penalty = "1\u00a0933,34\u00a0₽",
      ),
      stageCompletionPenalty = StageCompletionPenaltyDto(
        overdueDays = technicalReportDays,
        penalty = "483,34\u00a0₽",
      ),
      totalDaysForCalculation = totalDaysForCalculation + technicalReportDays,
      totalPenalty = "2\u00a0416,68\u00a0₽",
    )

    assertEquals(expected, result.toDto())
  }

  private fun date(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)
}
