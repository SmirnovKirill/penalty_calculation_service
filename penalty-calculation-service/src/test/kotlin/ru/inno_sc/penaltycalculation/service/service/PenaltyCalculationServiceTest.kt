package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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

class PenaltyCalculationServiceTest : AbstractTest() {
  @Autowired
  private lateinit var service: PenaltyCalculationService

  @Test
  fun `all penalty kinds exclude planned day and include actual day`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        reportPlannedSubmissionDate = date("24.02.2026"),
        grantReport = GrantReportDto(
          cases = listOf(case("02.03.2026", "08.03.2026")),
          reportApprovalDate = date("10.03.2026"),
          daysToExclude = DaysToExcludeDto(),
        ),
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("05.03.2026"),
          actualCompletionDate = date("08.03.2026"),
        ),
      )
    )

    assertEquals(days("25.02.2026", "26.02.2026", "27.02.2026"), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026"), result.reportingDelayPenalty.grantReport.daysForCalculation)
    assertEquals(
      days("25.02.2026", "26.02.2026", "27.02.2026", "06.03.2026", "07.03.2026", "08.03.2026"),
      result.reportingDelayPenalty.totalDaysForCalculation
    )
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026"), result.stageCompletionPenalty.overdueDays)
    assertEquals(BigDecimal("280.00"), result.reportingDelayPenalty.penalty)
    assertEquals(BigDecimal("140.00"), result.stageCompletionPenalty.penalty)
    assertEquals(BigDecimal("420.00"), result.totalPenalty)
  }

  @Test
  fun `completion counts holidays across the year boundary`() {
    val request = request()
    val result = service.calculateStageCompletionPenalty(
      request.copy(
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("31.12.2025"),
          actualCompletionDate = date("05.01.2026"),
        )
      )
    )

    assertEquals(days("01.01.2026", "02.01.2026", "03.01.2026", "04.01.2026", "05.01.2026"), result.overdueDays)
  }

  @Test
  fun `reporting combines initial delay with unique days of all reports including weekends`() {
    val request = request().copy(
      reportPlannedSubmissionDate = date("25.02.2026"),
      grantReport = GrantReportDto(
        cases = listOf(case("02.03.2026", "09.03.2026")),
        reportApprovalDate = date("10.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case("02.03.2026", "06.03.2026")),
        reportApprovalDate = date("10.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    val result = service.calculateReportingDelayPenalty(request)

    assertEquals(days("26.02.2026", "27.02.2026"), result.initialSubmissionDaysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026"), result.grantReport.daysForCalculation)
    assertEquals(days("06.03.2026"), result.extraBudgetReport.daysForCalculation)
    assertEquals(emptyList<LocalDate>(), result.technicalReport.daysForCalculation)
    assertEquals(
      days("26.02.2026", "27.02.2026", "06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026"),
      result.totalOverdueDays
    )
    assertEquals(emptyList<LocalDate>(), result.totalDaysToExclude)
    assertEquals(
      days("26.02.2026", "27.02.2026", "06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026"),
      result.totalDaysForCalculation
    )
    assertEquals(BigDecimal("280.00"), result.penalty)
  }

  @Test
  fun `disjoint report delays are combined in date order without filling the gaps`() {
    val request = request()
    val result = service.calculateReportingDelayPenalty(
      request.copy(
        grantReport = GrantReportDto(
          cases = listOf(case("04.03.2026", "11.03.2026")),
          reportApprovalDate = date("11.03.2026"),
          daysToExclude = DaysToExcludeDto(),
        ),
        extraBudgetReport = request.extraBudgetReport.copy(cases = listOf(case("02.03.2026", "06.03.2026"))),
        technicalReport = request.technicalReport.copy(cases = listOf(case("03.03.2026", "07.03.2026"))),
      )
    )

    assertEquals(days("11.03.2026"), result.grantReport.daysForCalculation)
    assertEquals(days("06.03.2026"), result.extraBudgetReport.daysForCalculation)
    assertEquals(days("07.03.2026"), result.technicalReport.daysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026", "11.03.2026"), result.totalDaysForCalculation)
  }

  @Test
  fun `uncorrected attempts keep the original deadline and do not duplicate overdue days`() {
    val request = request().copy(
      grantReport = GrantReportDto(
        cases = listOf(
          case("02.03.2026", "04.03.2026", remarksCorrected = false),
          case("05.03.2026", "06.03.2026", remarksCorrected = false),
          case("09.03.2026", "10.03.2026"),
        ),
        reportApprovalDate = date("10.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      )
    )

    val result = service.calculateReportingDelayPenalty(request)
    val grant = result.grantReport

    assertEquals(listOf(1, 1, 1), grant.cases.map { it.caseNumber })
    assertEquals(days("05.03.2026", "05.03.2026", "05.03.2026"), grant.cases.map { it.plannedSubmissionDate })
    assertEquals(emptyList<LocalDate>(), grant.cases[0].daysForCalculation)
    assertEquals(days("06.03.2026"), grant.cases[1].daysForCalculation)
    assertEquals(
      days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026", "10.03.2026"),
      grant.cases[2].daysForCalculation
    )
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026", "10.03.2026"), result.totalDaysForCalculation)
  }

  @Test
  fun `only corrected attempts advance the case number and get a new deadline`() {
    val request = request().copy(
      grantReport = GrantReportDto(
        cases = listOf(
          case("02.03.2026", "04.03.2026", remarksCorrected = false),
          case("05.03.2026", "06.03.2026"),
          case("10.03.2026", "13.03.2026"),
          case("16.03.2026", "18.03.2026"),
        ),
        reportApprovalDate = date("18.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      )
    )

    val result = service.calculateReportingDelayPenalty(request)

    assertEquals(listOf(1, 1, 2, 3), result.grantReport.cases.map { it.caseNumber })
    assertEquals(
      days("05.03.2026", "05.03.2026", "13.03.2026", "18.03.2026"),
      result.grantReport.cases.map { it.plannedSubmissionDate }
    )
    assertEquals(days("06.03.2026"), result.grantReport.daysForCalculation)
    assertEquals(1, result.extraBudgetReport.cases.single().caseNumber)
    assertEquals(1, result.technicalReport.cases.single().caseNumber)
  }

  @Test
  fun `later on-time submission and approval do not extend another reports delay`() {
    val request = request().copy(
      grantReport = GrantReportDto(
        cases = listOf(case("02.03.2026", "06.03.2026")),
        reportApprovalDate = date("20.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case("06.03.2026", "10.03.2026")),
        reportApprovalDate = date("20.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    val result = service.calculateReportingDelayPenalty(request)

    assertEquals(days("06.03.2026"), result.grantReport.daysForCalculation)
    assertEquals(emptyList<LocalDate>(), result.extraBudgetReport.daysForCalculation)
    assertEquals(days("06.03.2026"), result.totalDaysForCalculation)
  }

  @Test
  fun `dates and overlapping ranges exclude each day once only in their report`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        grantReport = request.grantReport.copy(
          cases = listOf(case("02.03.2026", "10.03.2026")),
          daysToExclude = DaysToExcludeDto(
            days = days("06.03.2026", "08.03.2026", "08.03.2026", "01.03.2026"),
            dayRanges = listOf(
              DayRangeDto(startDate = date("07.03.2026"), endDate = date("08.03.2026")),
              DayRangeDto(startDate = date("08.03.2026"), endDate = date("09.03.2026")),
              DayRangeDto(startDate = date("09.03.2026"), endDate = date("09.03.2026")),
            ),
          ),
        ),
      )
    )

    val reporting = result.reportingDelayPenalty
    val grant = reporting.grantReport
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026", "10.03.2026"), grant.overdueDays)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026"), grant.daysToExclude)
    assertEquals(days("10.03.2026"), grant.daysForCalculation)
    assertEquals(date("05.03.2026"), grant.cases.single().plannedSubmissionDate)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026"), grant.cases.single().daysToExclude)
    assertEquals(emptyList<LocalDate>(), reporting.extraBudgetReport.daysToExclude)
    assertEquals(emptyList<LocalDate>(), reporting.technicalReport.daysToExclude)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026"), reporting.totalDaysToExclude)
    assertEquals(days("10.03.2026"), reporting.totalDaysForCalculation)
    assertEquals(BigDecimal("46.67"), reporting.penalty)
    assertEquals(BigDecimal("46.67"), result.totalPenalty)
  }

  @Test
  fun `one report exclusion keeps a day in totals while another report counts it`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        grantReport = request.grantReport.copy(
          cases = listOf(case("02.03.2026", "07.03.2026")),
          daysToExclude = DaysToExcludeDto(
            dayRanges = listOf(DayRangeDto(startDate = date("06.03.2026"), endDate = date("07.03.2026"))),
          ),
        ),
        extraBudgetReport = request.extraBudgetReport.copy(cases = listOf(case("02.03.2026", "07.03.2026"))),
      )
    )

    val reporting = result.reportingDelayPenalty
    assertEquals(days("06.03.2026", "07.03.2026"), reporting.grantReport.daysToExclude)
    assertEquals(emptyList<LocalDate>(), reporting.grantReport.daysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026"), reporting.extraBudgetReport.daysForCalculation)
    assertEquals(emptyList<LocalDate>(), reporting.totalDaysToExclude)
    assertEquals(days("06.03.2026", "07.03.2026"), reporting.totalDaysForCalculation)
    assertEquals(BigDecimal("93.34"), reporting.penalty)
  }

  @Test
  fun `a day is excluded when every report with that overdue day excludes it`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        grantReport = request.grantReport.copy(
          cases = listOf(case("02.03.2026", "07.03.2026")),
          daysToExclude = DaysToExcludeDto(days = days("07.03.2026")),
        ),
        extraBudgetReport = request.extraBudgetReport.copy(
          cases = listOf(case("02.03.2026", "07.03.2026")),
          daysToExclude = DaysToExcludeDto(
            dayRanges = listOf(DayRangeDto(startDate = date("07.03.2026"), endDate = date("07.03.2026"))),
          ),
        ),
      )
    )

    val reporting = result.reportingDelayPenalty
    assertEquals(days("07.03.2026"), reporting.grantReport.daysToExclude)
    assertEquals(days("07.03.2026"), reporting.extraBudgetReport.daysToExclude)
    assertEquals(emptyList<LocalDate>(), reporting.technicalReport.daysToExclude)
    assertEquals(emptyList<LocalDate>(), reporting.technicalReport.overdueDays)
    assertEquals(days("06.03.2026", "07.03.2026"), reporting.totalOverdueDays)
    assertEquals(days("07.03.2026"), reporting.totalDaysToExclude)
    assertEquals(days("06.03.2026"), reporting.totalDaysForCalculation)
    assertEquals(BigDecimal("46.67"), reporting.penalty)
  }

  @Test
  fun `three report intervals stay independent and totals retain any counted day`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        grantReport = request.grantReport.copy(
          cases = listOf(case("02.03.2026", "10.03.2026")),
          daysToExclude = DaysToExcludeDto(
            dayRanges = listOf(DayRangeDto(startDate = date("06.03.2026"), endDate = date("07.03.2026"))),
          ),
        ),
        extraBudgetReport = request.extraBudgetReport.copy(
          cases = listOf(case("02.03.2026", "08.03.2026")),
          daysToExclude = DaysToExcludeDto(
            dayRanges = listOf(DayRangeDto(startDate = date("07.03.2026"), endDate = date("08.03.2026"))),
          ),
        ),
        technicalReport = request.technicalReport.copy(
          cases = listOf(case("02.03.2026", "09.03.2026")),
          daysToExclude = DaysToExcludeDto(
            dayRanges = listOf(DayRangeDto(startDate = date("08.03.2026"), endDate = date("09.03.2026"))),
          ),
        ),
      )
    )

    val reporting = result.reportingDelayPenalty
    assertEquals(days("06.03.2026", "07.03.2026"), reporting.grantReport.daysToExclude)
    assertEquals(days("08.03.2026", "09.03.2026", "10.03.2026"), reporting.grantReport.daysForCalculation)
    assertEquals(days("07.03.2026", "08.03.2026"), reporting.extraBudgetReport.daysToExclude)
    assertEquals(days("06.03.2026"), reporting.extraBudgetReport.daysForCalculation)
    assertEquals(days("08.03.2026", "09.03.2026"), reporting.technicalReport.daysToExclude)
    assertEquals(days("06.03.2026", "07.03.2026"), reporting.technicalReport.daysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026", "10.03.2026"), reporting.totalOverdueDays)
    assertEquals(emptyList<LocalDate>(), reporting.totalDaysToExclude)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026", "10.03.2026"), reporting.totalDaysForCalculation)
    assertEquals(BigDecimal("233.34"), reporting.penalty)
  }

  @Test
  fun `excluding every revision day preserves original dates and produces zero reporting penalty`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        grantReport = request.grantReport.copy(
          cases = listOf(case("02.03.2026", "06.03.2026")),
          daysToExclude = DaysToExcludeDto(days = days("06.03.2026")),
        ),
        extraBudgetReport = request.extraBudgetReport.copy(
          cases = listOf(case("02.03.2026", "07.03.2026")),
          daysToExclude = DaysToExcludeDto(
            dayRanges = listOf(DayRangeDto(startDate = date("06.03.2026"), endDate = date("07.03.2026"))),
          ),
        ),
        technicalReport = request.technicalReport.copy(
          cases = listOf(case("02.03.2026", "08.03.2026")),
          daysToExclude = DaysToExcludeDto(
            dayRanges = listOf(DayRangeDto(startDate = date("06.03.2026"), endDate = date("08.03.2026"))),
          ),
        ),
      )
    )

    val reporting = result.reportingDelayPenalty
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026"), reporting.totalOverdueDays)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026"), reporting.totalDaysToExclude)
    assertEquals(emptyList<LocalDate>(), reporting.totalDaysForCalculation)
    assertEquals(BigDecimal("0.00"), reporting.penalty)
    assertEquals(emptyList<LocalDate>(), result.totalDaysForCalculation)
    assertEquals(BigDecimal("0.00"), result.totalPenalty)
  }

  @Test
  fun `report exclusions never remove initial submission or completion days`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        reportPlannedSubmissionDate = date("26.02.2026"),
        grantReport = request.grantReport.copy(
          cases = listOf(case("02.03.2026", "06.03.2026")),
          daysToExclude = DaysToExcludeDto(
            dayRanges = listOf(DayRangeDto(startDate = date("27.02.2026"), endDate = date("06.03.2026"))),
          ),
        ),
        extraBudgetReport = request.extraBudgetReport.copy(daysToExclude = DaysToExcludeDto(days = days("27.02.2026"))),
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("26.02.2026"),
          actualCompletionDate = date("07.03.2026"),
          daysToExclude = DaysToExcludeDto(
            dayRanges = listOf(DayRangeDto(startDate = date("27.02.2026"), endDate = date("07.03.2026"))),
          ),
        ),
      )
    )

    val reporting = result.reportingDelayPenalty
    assertEquals(days("27.02.2026"), reporting.initialSubmissionDaysForCalculation)
    assertEquals(days("06.03.2026"), reporting.grantReport.daysToExclude)
    assertEquals(emptyList<LocalDate>(), reporting.extraBudgetReport.daysToExclude)
    assertEquals(emptyList<LocalDate>(), reporting.technicalReport.daysToExclude)
    assertEquals(days("27.02.2026", "06.03.2026"), reporting.totalOverdueDays)
    assertEquals(days("06.03.2026"), reporting.totalDaysToExclude)
    assertEquals(days("27.02.2026"), reporting.totalDaysForCalculation)
    assertEquals(BigDecimal("46.67"), reporting.penalty)
    assertEquals(
      days("27.02.2026", "28.02.2026", "01.03.2026", "02.03.2026", "03.03.2026", "04.03.2026", "05.03.2026", "06.03.2026", "07.03.2026"),
      result.stageCompletionPenalty.overdueDays
    )
    assertEquals(
      days(
        "27.02.2026", "27.02.2026", "28.02.2026", "01.03.2026", "02.03.2026", "03.03.2026",
        "04.03.2026", "05.03.2026", "06.03.2026", "07.03.2026",
      ),
      result.totalDaysForCalculation
    )
    assertEquals(BigDecimal("420.00"), result.stageCompletionPenalty.penalty)
    assertEquals(BigDecimal("466.67"), result.totalPenalty)
  }

  @Test
  fun `the same dates in reporting and completion count in both sections`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        reportPlannedSubmissionDate = date("26.02.2026"),
        grantReport = GrantReportDto(
          cases = listOf(case("02.03.2026", "06.03.2026")),
          reportApprovalDate = date("10.03.2026"),
          daysToExclude = DaysToExcludeDto(),
        ),
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("26.02.2026"),
          actualCompletionDate = date("07.03.2026"),
        ),
      )
    )

    assertEquals(days("27.02.2026"), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
    assertEquals(days("27.02.2026", "06.03.2026"), result.reportingDelayPenalty.totalDaysForCalculation)
    assertEquals(
      days(
        "27.02.2026", "06.03.2026",
        "27.02.2026", "28.02.2026", "01.03.2026", "02.03.2026", "03.03.2026", "04.03.2026", "05.03.2026", "06.03.2026", "07.03.2026",
      ),
      result.totalDaysForCalculation
    )
    assertEquals(BigDecimal("93.34"), result.reportingDelayPenalty.penalty)
    assertEquals(BigDecimal("420.00"), result.stageCompletionPenalty.penalty)
    assertEquals(BigDecimal("513.34"), result.totalPenalty)
  }

  // Для 1 рубля прежнее округление всех 3 дней давало 0.01, два раздела дают 0.01 + 0.01 = 0.02.
  @ParameterizedTest
  @CsvSource("1,0.01,0.01,0.02", "10.71,0.01,0.01,0.02", "100000,93.34,46.67,140.01")
  fun `reporting and completion are rounded separately before summing`(
    amount: String,
    reportingPenalty: String,
    completionPenalty: String,
    totalPenalty: String,
  ) {
    val case = case("06.03.2026", "13.03.2026")
    val request = request().copy(
      stageAmount = BigDecimal(amount),
      reportPlannedSubmissionDate = date("04.03.2026"),
      reportActualSubmissionDate = date("05.03.2026"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("13.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("13.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("13.03.2026"),
        plannedCompletionDate = date("02.03.2026"),
        actualCompletionDate = date("03.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    val result = service.calculate(request)

    assertEquals(days("05.03.2026"), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
    assertEquals(days("13.03.2026"), result.reportingDelayPenalty.grantReport.daysForCalculation)
    assertEquals(days("13.03.2026"), result.reportingDelayPenalty.extraBudgetReport.daysForCalculation)
    assertEquals(days("13.03.2026"), result.reportingDelayPenalty.technicalReport.daysForCalculation)
    assertEquals(days("05.03.2026", "13.03.2026"), result.reportingDelayPenalty.totalDaysForCalculation)
    assertEquals(days("03.03.2026"), result.stageCompletionPenalty.overdueDays)
    assertEquals(days("05.03.2026", "13.03.2026", "03.03.2026"), result.totalDaysForCalculation)
    assertEquals(BigDecimal(reportingPenalty), result.reportingDelayPenalty.penalty)
    assertEquals(BigDecimal(completionPenalty), result.stageCompletionPenalty.penalty)
    assertEquals(BigDecimal(totalPenalty), result.totalPenalty)
  }

  private fun request(): CalculatePenaltyRequestDto {
    val case = case("02.03.2026", "05.03.2026")
    return CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("27.02.2026"),
      reportActualSubmissionDate = date("27.02.2026"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.03.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.03.2026"),
        plannedCompletionDate = date("26.02.2026"),
        actualCompletionDate = date("26.02.2026"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )
  }

  private fun case(returned: String, submitted: String, remarksCorrected: Boolean = true) = ReportCaseDto(
    returnedForRevisionDate = date(returned),
    actualSubmissionDate = date(submitted),
    remarksCorrected = remarksCorrected,
  )

  private fun days(vararg values: String): List<LocalDate> = values.map(::date)

  private fun date(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)
}
