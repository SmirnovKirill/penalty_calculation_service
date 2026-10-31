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
import ru.inno_sc.penaltycalculation.api.penalty.request.DaysToExcludeManuallyDto
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
  fun `submission and revisions include actual day but completion excludes it`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        reportPlannedSubmissionDate = date("24.02.2026"),
        grantReport = GrantReportDto(listOf(case("02.03.2026", "08.03.2026")), date("10.03.2026")),
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("05.03.2026"),
          actualCompletionDate = date("08.03.2026"),
        ),
      )
    )

    assertEquals(days("25.02.2026", "26.02.2026", "27.02.2026"), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026"), result.revisionsPenalty.totalDaysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026"), result.stageCompletionPenalty.overdueDays)
  }

  @Test
  fun `completion the day after its deadline has no chargeable days`() {
    val request = request()
    val result = service.calculateStageCompletionPenalty(
      request.copy(
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("05.03.2026"),
          actualCompletionDate = date("06.03.2026"),
        )
      )
    )

    assertEquals(emptyList<LocalDate>(), result.overdueDays)
    assertEquals(BigDecimal("0.00"), result.penalty)
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

    assertEquals(days("01.01.2026", "02.01.2026", "03.01.2026", "04.01.2026"), result.overdueDays)
  }

  @Test
  fun `overlapping delays of different reports count once including weekends`() {
    val request = request().copy(
      grantReport = GrantReportDto(listOf(case("02.03.2026", "09.03.2026")), date("10.03.2026")),
      extraBudgetReport = ExtraBudgetReportDto(listOf(case("02.03.2026", "06.03.2026")), date("10.03.2026")),
    )

    val result = service.calculateAllReportsRevisionPenalty(request)

    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026"), result.grantReport.daysForCalculation)
    assertEquals(days("06.03.2026"), result.extraBudgetReport.daysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026"), result.totalDaysForCalculation)
  }

  @Test
  fun `disjoint report delays are combined in date order without filling the gaps`() {
    val request = request()
    val result = service.calculateAllReportsRevisionPenalty(
      request.copy(
        grantReport = GrantReportDto(listOf(case("04.03.2026", "11.03.2026")), date("11.03.2026")),
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
        listOf(
          case("02.03.2026", "04.03.2026", remarksCorrected = false),
          case("05.03.2026", "06.03.2026", remarksCorrected = false),
          case("09.03.2026", "10.03.2026"),
        ),
        date("10.03.2026")
      )
    )

    val result = service.calculateAllReportsRevisionPenalty(request)
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
        listOf(
          case("02.03.2026", "04.03.2026", remarksCorrected = false),
          case("05.03.2026", "06.03.2026"),
          case("10.03.2026", "13.03.2026"),
          case("16.03.2026", "18.03.2026"),
        ),
        date("18.03.2026")
      )
    )

    val result = service.calculateAllReportsRevisionPenalty(request)

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
      grantReport = GrantReportDto(listOf(case("02.03.2026", "06.03.2026")), date("20.03.2026")),
      extraBudgetReport = ExtraBudgetReportDto(listOf(case("06.03.2026", "10.03.2026")), date("20.03.2026")),
    )

    val result = service.calculateAllReportsRevisionPenalty(request)

    assertEquals(days("06.03.2026"), result.grantReport.daysForCalculation)
    assertEquals(emptyList<LocalDate>(), result.extraBudgetReport.daysForCalculation)
    assertEquals(days("06.03.2026"), result.totalDaysForCalculation)
  }

  @Test
  fun `manual dates and overlapping ranges include both ends and exclude each overdue day once`() {
    val request = request().copy(
      reportPlannedSubmissionDate = date("20.02.2026"),
      reportActualSubmissionDate = date("28.02.2026"),
      daysToExcludeManually = DaysToExcludeManuallyDto(
        days = days("21.02.2026", "24.02.2026", "24.02.2026", "19.02.2026", "01.03.2026"),
        dayRanges = listOf(
          DayRangeDto(date("22.02.2026"), date("24.02.2026")),
          DayRangeDto(date("24.02.2026"), date("26.02.2026")),
          DayRangeDto(date("27.02.2026"), date("27.02.2026")),
        ),
      ),
    )

    val result = service.calculateInitialSubmissionPenalty(request)

    assertEquals(
      days("21.02.2026", "22.02.2026", "23.02.2026", "24.02.2026", "25.02.2026", "26.02.2026", "27.02.2026", "28.02.2026"),
      result.overdueDays
    )
    assertEquals(
      days("21.02.2026", "22.02.2026", "23.02.2026", "24.02.2026", "25.02.2026", "26.02.2026", "27.02.2026"),
      result.daysToExcludeManually
    )
    assertEquals(days("28.02.2026"), result.daysForCalculation)
  }

  @Test
  fun `manual exclusions apply to every report without moving revision deadlines`() {
    val request = request()
    val result = service.calculateAllReportsRevisionPenalty(
      request.copy(
        grantReport = GrantReportDto(listOf(case("02.03.2026", "10.03.2026")), date("10.03.2026")),
        extraBudgetReport = ExtraBudgetReportDto(listOf(case("02.03.2026", "06.03.2026")), date("10.03.2026")),
        technicalReport = request.technicalReport.copy(cases = listOf(case("02.03.2026", "07.03.2026"))),
        daysToExcludeManually = DaysToExcludeManuallyDto(
          days = days("06.03.2026", "06.03.2026"),
          dayRanges = listOf(DayRangeDto(date("03.03.2026"), date("08.03.2026"))),
        ),
      )
    )

    val grantCase = result.grantReport.cases.single()
    assertEquals(date("05.03.2026"), grantCase.plannedSubmissionDate)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026"), grantCase.daysToExcludeManually)
    assertEquals(days("09.03.2026", "10.03.2026"), grantCase.daysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026"), result.grantReport.daysToExcludeManually)
    assertEquals(days("09.03.2026", "10.03.2026"), result.grantReport.daysForCalculation)
    assertEquals(days("06.03.2026"), result.extraBudgetReport.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.extraBudgetReport.daysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026"), result.technicalReport.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.technicalReport.daysForCalculation)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026", "09.03.2026", "10.03.2026"), result.totalOverdueDays)
    assertEquals(days("06.03.2026", "07.03.2026", "08.03.2026"), result.totalDaysToExcludeManually)
    assertEquals(days("09.03.2026", "10.03.2026"), result.totalDaysForCalculation)
  }

  @Test
  fun `excluding every overdue day preserves the original periods but produces zero penalty`() {
    val request = request().copy(
      reportPlannedSubmissionDate = date("26.02.2026"),
      grantReport = GrantReportDto(listOf(case("02.03.2026", "06.03.2026")), date("10.03.2026")),
      daysToExcludeManually = DaysToExcludeManuallyDto(
        dayRanges = listOf(DayRangeDto(date("27.02.2026"), date("06.03.2026"))),
      ),
    )

    val result = service.calculate(request)

    assertEquals(days("27.02.2026"), result.initialSubmissionPenalty.overdueDays)
    assertEquals(days("27.02.2026"), result.initialSubmissionPenalty.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(days("06.03.2026"), result.revisionsPenalty.totalOverdueDays)
    assertEquals(days("06.03.2026"), result.revisionsPenalty.totalDaysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.totalDaysForCalculation)
    assertEquals(emptyList<LocalDate>(), result.totalDaysForCalculation)
    assertEquals(BigDecimal("0.00"), result.totalPenalty)
  }

  @Test
  fun `manual exclusions do not remove the same days from completion penalty`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        reportPlannedSubmissionDate = date("26.02.2026"),
        grantReport = GrantReportDto(listOf(case("02.03.2026", "06.03.2026")), date("10.03.2026")),
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("26.02.2026"),
          actualCompletionDate = date("07.03.2026"),
        ),
        daysToExcludeManually = DaysToExcludeManuallyDto(
          days = days("27.02.2026"),
          dayRanges = listOf(DayRangeDto(date("28.02.2026"), date("06.03.2026"))),
        ),
      )
    )

    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.totalDaysForCalculation)
    assertEquals(
      days("27.02.2026", "28.02.2026", "01.03.2026", "02.03.2026", "03.03.2026", "04.03.2026", "05.03.2026", "06.03.2026"),
      result.stageCompletionPenalty.overdueDays
    )
    assertEquals(result.stageCompletionPenalty.overdueDays, result.totalDaysForCalculation)
    assertEquals(BigDecimal("373.34"), result.totalPenalty)
  }

  @Test
  fun `the same dates in different penalty kinds count separately`() {
    val request = request()
    val result = service.calculate(
      request.copy(
        reportPlannedSubmissionDate = date("26.02.2026"),
        grantReport = GrantReportDto(listOf(case("02.03.2026", "06.03.2026")), date("10.03.2026")),
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("26.02.2026"),
          actualCompletionDate = date("07.03.2026"),
        ),
      )
    )

    assertEquals(days("27.02.2026"), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(days("06.03.2026"), result.revisionsPenalty.totalDaysForCalculation)
    assertEquals(
      days(
        "27.02.2026", "06.03.2026",
        "27.02.2026", "28.02.2026", "01.03.2026", "02.03.2026", "03.03.2026", "04.03.2026", "05.03.2026", "06.03.2026",
      ),
      result.totalDaysForCalculation
    )
    assertEquals(BigDecimal("466.67"), result.totalPenalty)
  }

  @ParameterizedTest
  @CsvSource("1,0.01", "10.71,0.02")
  fun `total penalty is rounded up once instead of adding rounded components`(amount: String, expected: String) {
    val case = case("06.03.2026", "13.03.2026")
    val request = request().copy(
      stageAmount = BigDecimal(amount),
      reportPlannedSubmissionDate = date("04.03.2026"),
      reportActualSubmissionDate = date("05.03.2026"),
      grantReport = GrantReportDto(listOf(case), date("13.03.2026")),
      extraBudgetReport = ExtraBudgetReportDto(listOf(case), date("13.03.2026")),
      technicalReport = TechnicalReportDto(listOf(case), date("13.03.2026"), date("02.03.2026"), date("04.03.2026")),
    )

    val result = service.calculate(request)

    assertEquals(days("05.03.2026"), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(days("13.03.2026"), result.revisionsPenalty.totalDaysForCalculation)
    assertEquals(days("03.03.2026"), result.stageCompletionPenalty.overdueDays)
    assertEquals(days("05.03.2026", "13.03.2026", "03.03.2026"), result.totalDaysForCalculation)
    assertEquals(BigDecimal("0.01"), result.initialSubmissionPenalty.penalty)
    assertEquals(BigDecimal("0.01"), result.revisionsPenalty.penalty)
    assertEquals(BigDecimal("0.01"), result.stageCompletionPenalty.penalty)
    assertEquals(BigDecimal(expected), result.totalPenalty)
  }

  private fun request(): CalculatePenaltyRequestDto {
    val case = case("02.03.2026", "05.03.2026")
    return CalculatePenaltyRequestDto(
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("27.02.2026"),
      reportActualSubmissionDate = date("27.02.2026"),
      grantReport = GrantReportDto(listOf(case), date("10.03.2026")),
      extraBudgetReport = ExtraBudgetReportDto(listOf(case), date("10.03.2026")),
      technicalReport = TechnicalReportDto(listOf(case), date("10.03.2026"), date("26.02.2026"), date("26.02.2026")),
      daysToExcludeManually = DaysToExcludeManuallyDto(),
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
