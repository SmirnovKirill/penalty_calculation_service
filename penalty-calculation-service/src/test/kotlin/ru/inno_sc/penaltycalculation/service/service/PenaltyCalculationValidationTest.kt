package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.api.penalty.request.DayRangeDto
import ru.inno_sc.penaltycalculation.api.penalty.request.DaysToExcludeDto
import ru.inno_sc.penaltycalculation.api.penalty.request.ExtraBudgetReportDto
import ru.inno_sc.penaltycalculation.api.penalty.request.GrantReportDto
import ru.inno_sc.penaltycalculation.api.penalty.request.ReportCaseDto
import ru.inno_sc.penaltycalculation.api.penalty.request.TechnicalReportDto
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER

class PenaltyCalculationValidationTest : AbstractTest() {
  @Autowired
  private lateinit var service: PenaltyCalculationService

  @Test
  fun `allows report submission before its planned date`() {
    val request = request().copy(
      reportPlannedSubmissionDate = date("11.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
    )

    val result = service.calculate(request)

    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
  }

  @Test
  fun `allows work completion before its planned date`() {
    val request = request()
    val technical = request.technicalReport.copy(
      plannedCompletionDate = date("11.11.2025"),
      actualCompletionDate = date("10.11.2025"),
    )

    val result = service.calculate(
      request.copy(
        technicalReport = technical
      )
    )

    assertEquals(emptyList<LocalDate>(), result.stageCompletionPenalty.overdueDays)
  }

  @Test
  fun `allows report submission revision and approval on the same day`() {
    val request = request().copy(
      reportPlannedSubmissionDate = date("10.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(
          ReportCaseDto(
            returnedForRevisionDate = date("10.11.2025"),
            actualSubmissionDate = date("10.11.2025"),
          )
        ),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    assertDoesNotThrow { service.calculate(request) }
  }

  @Test
  fun `next revision cannot start before the previous submission`() {
    val request = request()
    val technical = request.technicalReport.copy(
      cases = listOf(
        ReportCaseDto(
          returnedForRevisionDate = date("10.11.2025"),
          actualSubmissionDate = date("12.11.2025"),
          remarksCorrected = false,
        ),
        ReportCaseDto(
          returnedForRevisionDate = date("11.11.2025"),
          actualSubmissionDate = date("13.11.2025"),
        ),
      ),
      reportApprovalDate = date("13.11.2025"),
    )

    assertThrows(IllegalArgumentException::class.java) {
      service.calculate(
        request.copy(
          technicalReport = technical
        )
      )
    }
  }

  @Test
  fun `report approval cannot precede the last revision submission`() {
    val request = request()
    val technical = request.technicalReport.copy(
      cases = listOf(
        ReportCaseDto(
          returnedForRevisionDate = date("10.11.2025"),
          actualSubmissionDate = date("11.11.2025"),
        ),
        ReportCaseDto(
          returnedForRevisionDate = date("12.11.2025"),
          actualSubmissionDate = date("14.11.2025"),
        ),
      ),
      reportApprovalDate = date("13.11.2025"),
    )

    assertThrows(IllegalArgumentException::class.java) {
      service.calculate(
        request.copy(
          technicalReport = technical
        )
      )
    }
  }

  @Test
  fun `last revision must correct the remarks`() {
    val request = request()
    val technical = request.technicalReport.copy(
      cases = listOf(
        ReportCaseDto(
          returnedForRevisionDate = date("10.11.2025"),
          actualSubmissionDate = date("11.11.2025"),
          remarksCorrected = false,
        )
      ),
      reportApprovalDate = date("12.11.2025"),
    )

    assertThrows(IllegalArgumentException::class.java) {
      service.calculate(
        request.copy(
          technicalReport = technical
        )
      )
    }
  }

  @Test
  fun `allows an unresolved revision followed by a correction on the same day`() {
    val request = request()
    val technical = request.technicalReport.copy(
      cases = listOf(
        ReportCaseDto(
          returnedForRevisionDate = date("10.11.2025"),
          actualSubmissionDate = date("11.11.2025"),
          remarksCorrected = false,
        ),
        ReportCaseDto(
          returnedForRevisionDate = date("11.11.2025"),
          actualSubmissionDate = date("11.11.2025"),
          remarksCorrected = true,
        ),
      ),
      reportApprovalDate = date("12.11.2025"),
    )

    assertDoesNotThrow {
      service.calculate(
        request.copy(
          technicalReport = technical
        )
      )
    }
  }

  @Test
  fun `requested calculation date determines the key rate and result date`() {
    val historical = service.calculate(request().copy(calculationDate = date("10.11.2025")))
    val current = service.calculate(request().copy(calculationDate = date("28.10.2026")))

    assertEquals(date("10.11.2025"), historical.calculationDate)
    assertEquals(BigDecimal("16.50"), historical.keyRatePercent)
    assertEquals(date("28.10.2026"), current.calculationDate)
    assertEquals(BigDecimal("14.00"), current.keyRatePercent)
  }

  @Test
  fun `events after a historical calculation date are rejected even when already in the past`() {
    val request = request().copy(
      calculationDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(
          ReportCaseDto(
            returnedForRevisionDate = date("10.11.2025"),
            actualSubmissionDate = date("10.11.2025"),
          )
        ),
        reportApprovalDate = date("11.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    assertThrows(IllegalArgumentException::class.java) {
      service.calculate(request)
    }
  }

  @Test
  fun `work completion is independent of approval before or after it`() {
    val request = request()
    val approvedBeforeCompletion = service.calculate(
      request.copy(
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("10.11.2025"),
          actualCompletionDate = date("12.11.2025"),
          reportApprovalDate = date("11.11.2025"),
        )
      )
    )
    val approvedAfterCompletion = service.calculate(
      request.copy(
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("10.11.2025"),
          actualCompletionDate = date("12.11.2025"),
          reportApprovalDate = date("13.11.2025"),
        )
      )
    )

    assertEquals(approvedBeforeCompletion.stageCompletionPenalty, approvedAfterCompletion.stageCompletionPenalty)
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `rejects an excluded day before the planned reporting date`(report: String) {
    val request = request().copy(reportPlannedSubmissionDate = date("11.11.2025"))
    val invalid = withDaysToExclude(report, DaysToExcludeDto(days = listOf(date("10.11.2025"))), request)

    val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(invalid) }

    assertTrue(error.message.orEmpty().contains("плановой дате подачи отчётности"))
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `rejects ranges partly or wholly before the planned reporting date`(report: String) {
    val request = request().copy(reportPlannedSubmissionDate = date("12.11.2025"))
    val partlyBefore = withDaysToExclude(
      report,
      DaysToExcludeDto(dayRanges = listOf(DayRangeDto(startDate = date("11.11.2025"), endDate = date("13.11.2025")))),
      request,
    )
    val whollyBefore = withDaysToExclude(
      report,
      DaysToExcludeDto(dayRanges = listOf(DayRangeDto(startDate = date("10.11.2025"), endDate = date("11.11.2025")))),
      request,
    )

    val partlyBeforeError = assertThrows(IllegalArgumentException::class.java) { service.calculate(partlyBefore) }
    val whollyBeforeError = assertThrows(IllegalArgumentException::class.java) { service.calculate(whollyBefore) }

    assertTrue(partlyBeforeError.message.orEmpty().contains("плановой дате подачи отчётности"))
    assertTrue(whollyBeforeError.message.orEmpty().contains("плановой дате подачи отчётности"))
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `allows an excluded day and a single-day range equal to the planned reporting date`(report: String) {
    val request = request().copy(reportPlannedSubmissionDate = date("12.11.2025"))
    val valid = withDaysToExclude(
      report,
      DaysToExcludeDto(
        days = listOf(date("12.11.2025")),
        dayRanges = listOf(DayRangeDto(startDate = date("12.11.2025"), endDate = date("12.11.2025"))),
      ),
      request,
    )

    assertDoesNotThrow { service.calculate(valid) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `allows excluded days and ranges after the planned reporting date`(report: String) {
    val valid = withDaysToExclude(
      report,
      DaysToExcludeDto(
        days = listOf(date("11.11.2025")),
        dayRanges = listOf(DayRangeDto(startDate = date("12.11.2025"), endDate = date("14.11.2025"))),
      ),
    )

    assertDoesNotThrow { service.calculate(valid) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `rejects point dates and range starts before the minimum supported date`(report: String) {
    val request = request().copy(reportPlannedSubmissionDate = date("01.01.2022"))
    val point = withDaysToExclude(report, DaysToExcludeDto(days = listOf(date("31.12.2021"))), request)
    val range = withDaysToExclude(
      report,
      DaysToExcludeDto(dayRanges = listOf(DayRangeDto(startDate = date("31.12.2021"), endDate = date("01.01.2022")))),
      request,
    )

    val pointError = assertThrows(IllegalArgumentException::class.java) { service.calculate(point) }
    val rangeError = assertThrows(IllegalArgumentException::class.java) { service.calculate(range) }

    assertTrue(pointError.message.orEmpty().contains("01.01.2022"))
    assertTrue(rangeError.message.orEmpty().contains("01.01.2022"))
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `allows the minimum supported date as an exclusion boundary`(report: String) {
    val request = request().copy(reportPlannedSubmissionDate = date("01.01.2022"))
    val valid = withDaysToExclude(
      report,
      DaysToExcludeDto(
        days = listOf(date("01.01.2022")),
        dayRanges = listOf(DayRangeDto(startDate = date("01.01.2022"), endDate = date("01.01.2022"))),
      ),
      request,
    )

    assertDoesNotThrow { service.calculate(valid) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `rejects point dates and either interval boundary after a historical calculation date`(report: String) {
    val historical = request().copy(calculationDate = date("10.11.2025"))
    val invalidExclusions = listOf(
      DaysToExcludeDto(days = listOf(date("11.11.2025"))),
      DaysToExcludeDto(dayRanges = listOf(DayRangeDto(startDate = date("11.11.2025"), endDate = date("12.11.2025")))),
      DaysToExcludeDto(dayRanges = listOf(DayRangeDto(startDate = date("10.11.2025"), endDate = date("11.11.2025")))),
    )
    for (daysToExclude in invalidExclusions) {
      val invalid = withDaysToExclude(report, daysToExclude, historical)

      val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(invalid) }

      assertTrue(error.message.orEmpty().contains("будущ"))
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `rejects reversed intervals in each report`(report: String) {
    val invalid = withDaysToExclude(
      report,
      DaysToExcludeDto(dayRanges = listOf(DayRangeDto(startDate = date("14.11.2025"), endDate = date("12.11.2025")))),
    )

    val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(invalid) }

    assertTrue(error.message.orEmpty().contains("начало периода не может быть позже окончания"))
  }

  private fun request(): CalculatePenaltyRequestDto {
    val case = ReportCaseDto(
      returnedForRevisionDate = date("10.11.2025"),
      actualSubmissionDate = date("10.11.2025"),
    )
    return CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("10.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )
  }

  private fun withDaysToExclude(
    report: String,
    daysToExclude: DaysToExcludeDto,
    base: CalculatePenaltyRequestDto = request(),
  ): CalculatePenaltyRequestDto = when (report) {
    "grantReport" -> base.copy(grantReport = base.grantReport.copy(daysToExclude = daysToExclude))
    "extraBudgetReport" -> base.copy(extraBudgetReport = base.extraBudgetReport.copy(daysToExclude = daysToExclude))
    "technicalReport" -> base.copy(technicalReport = base.technicalReport.copy(daysToExclude = daysToExclude))
    else -> error("Unknown report: $report")
  }

  private fun date(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)
}
