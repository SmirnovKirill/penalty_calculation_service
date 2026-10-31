package ru.inno_sc.penaltycalculation.service.converter

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.domain.AllReportsRevisionPenalty
import ru.inno_sc.penaltycalculation.service.domain.InitialSubmissionPenalty
import ru.inno_sc.penaltycalculation.service.domain.PenaltyCalculationResult
import ru.inno_sc.penaltycalculation.service.domain.ReportRevisionCase
import ru.inno_sc.penaltycalculation.service.domain.SingleReportRevisionPenalty
import ru.inno_sc.penaltycalculation.service.domain.StageCompletionPenalty
import tools.jackson.databind.json.JsonMapper

class PenaltyCalculationResultConverterTest : AbstractTest() {
  @Autowired
  private lateinit var mapper: JsonMapper

  @Test
  fun `renders revision penalty calculated from unique overdue days across reports`() {
    val days = listOf(date("05.03.2026"))
    val report = SingleReportRevisionPenalty(cases = emptyList(), overdueDays = days, daysForCalculation = days)

    val dto = result(totalPenalty = BigDecimal("23333.34"), grantReport = report, extraBudgetReport = report).copy(stageNumber = 4).toDto()

    assertEquals(4, dto.stageNumber)
    assertEquals("28.10.2026", dto.calculationDate)
    assertEquals("14%", dto.keyRatePercent)
    assertEquals(listOf(date("05.03.2026")), dto.revisionsPenalty.daysForCalculation)
    assertEquals(days, dto.totalDaysForCalculation)
    assertEquals("23\u00a0333,34\u00a0₽", dto.totalPenalty)
    assertEquals(days, dto.revisionsPenalty.grantReport.daysForCalculation)
    assertEquals(days, dto.revisionsPenalty.extraBudgetReport.daysForCalculation)
    assertEquals(emptyList<LocalDate>(), dto.initialSubmissionPenalty.daysForCalculation)
    assertEquals(emptyList<LocalDate>(), dto.stageCompletionPenalty.overdueDays)
  }

  @Test
  fun `returns revision history as data with empty date lists`() {
    val days = listOf(date("06.03.2026"), date("07.03.2026"), date("08.03.2026"), date("09.03.2026"))
    val report = SingleReportRevisionPenalty(
      overdueDays = days,
      cases = listOf(
        ReportRevisionCase(
          1,
          date("02.03.2026"),
          date("05.03.2026"),
          date("06.03.2026"),
          false,
          listOf(date("06.03.2026")),
          daysForCalculation = listOf(date("06.03.2026"))
        ),
        ReportRevisionCase(1, date("09.03.2026"), date("05.03.2026"), date("09.03.2026"), true, days, daysForCalculation = days),
      ),
      daysForCalculation = days
    )

    val dto = result(totalPenalty = BigDecimal("93333.34"), grantReport = report).toDto()
    val cases = dto.revisionsPenalty.grantReport.cases

    assertEquals(listOf(1, 1), cases.map { it.caseNumber })
    assertEquals(listOf("05.03.2026", "05.03.2026"), cases.map { it.plannedSubmissionDate })
    assertEquals(listOf(false, true), cases.map { it.remarksCorrected })
    assertEquals("02.03.2026", cases.first().returnedForRevisionDate)
    assertEquals("09.03.2026", cases.last().actualSubmissionDate)
    assertEquals(days, cases.last().daysForCalculation)
    assertEquals(days, dto.revisionsPenalty.grantReport.daysForCalculation)
    assertEquals(emptyList<LocalDate>(), dto.revisionsPenalty.extraBudgetReport.daysForCalculation)
    assertEquals(emptyList<LocalDate>(), dto.initialSubmissionPenalty.daysForCalculation)
  }

  @Test
  fun `keeps exact kopecks when amounts exceed JavaScript integer precision`() {
    val dto = result(
      stageAmount = BigDecimal("9007199254740993.01"),
      totalPenalty = BigDecimal("4203359652212.47"),
      initialSubmissionOverdueDays = listOf(date("05.03.2026")),
    ).toDto()

    assertEquals("9\u00a0007\u00a0199\u00a0254\u00a0740\u00a0993,01\u00a0₽", dto.stageAmount)
    assertEquals("4\u00a0203\u00a0359\u00a0652\u00a0212,47\u00a0₽", dto.totalPenalty)
  }

  @Test
  fun `serializes only one penalty amount after adding days of all three types`() {
    val overdueDays = listOf(date("05.03.2026"))
    val dto = result(
      stageAmount = BigDecimal("1"),
      totalPenalty = BigDecimal("0.01"),
      initialSubmissionOverdueDays = overdueDays,
      grantReport = SingleReportRevisionPenalty(cases = emptyList(), overdueDays = overdueDays, daysForCalculation = overdueDays),
      stageCompletionOverdueDays = overdueDays,
    ).toDto()
    val json = mapper.readTree(mapper.writeValueAsString(dto))

    assertEquals(overdueDays + overdueDays + overdueDays, dto.totalDaysForCalculation)
    assertEquals(mapper.readTree("""["05.03.2026", "05.03.2026", "05.03.2026"]"""), json.path("totalDaysForCalculation"))
    assertFalse(json.has("totalOverdueDaysCount"))
    assertEquals("0,01\u00a0₽", dto.totalPenalty)
    for (name in listOf("initialSubmissionPenalty", "revisionsPenalty", "stageCompletionPenalty")) {
      val section = json.path(name)
      assertFalse(section.has("penalty"), name)
      assertFalse(section.has("totalPenalty"), name)
      assertFalse(section.has("formula"), name)
      assertFalse(section.has("summary"), name)
      assertFalse(section.has("overdueDaysCount"), name)
      assertFalse(section.has("daysForCalculationСount"), name)
    }
    assertFalse(json.path("revisionsPenalty").path("grantReport").has("overdueDaysCount"))
  }

  private fun result(
    totalPenalty: BigDecimal,
    stageAmount: BigDecimal = BigDecimal("50000000"),
    initialSubmissionOverdueDays: List<LocalDate> = emptyList(),
    grantReport: SingleReportRevisionPenalty = emptyReport(),
    extraBudgetReport: SingleReportRevisionPenalty = emptyReport(),
    stageCompletionOverdueDays: List<LocalDate> = emptyList(),
  ): PenaltyCalculationResult {
    val totalRevisionOverdueDays = (grantReport.overdueDays + extraBudgetReport.overdueDays).distinct().sorted()
    val totalRevisionExcludedDays = (grantReport.daysToExcludeManually + extraBudgetReport.daysToExcludeManually).distinct().sorted()
    val totalRevisionDaysForCalculation = (grantReport.daysForCalculation + extraBudgetReport.daysForCalculation).distinct().sorted()
    return PenaltyCalculationResult(
      stageNumber = 1,
      contractNumber = "Г-0029",
      stageAmount = stageAmount,
      calculationDate = date("28.10.2026"),
      keyRatePercent = BigDecimal("14.00"),
      totalPenalty = totalPenalty,
      initialSubmissionPenalty = InitialSubmissionPenalty(
        overdueDays = initialSubmissionOverdueDays,
        daysForCalculation = initialSubmissionOverdueDays,
        penalty = BigDecimal("999.99"),
      ),
      revisionsPenalty = AllReportsRevisionPenalty(
        grantReport = grantReport,
        extraBudgetReport = extraBudgetReport,
        technicalReport = emptyReport(),
        totalOverdueDays = totalRevisionOverdueDays,
        totalDaysToExcludeManually = totalRevisionExcludedDays,
        totalDaysForCalculation = totalRevisionDaysForCalculation,
        penalty = BigDecimal("999.99"),
      ),
      stageCompletionPenalty = StageCompletionPenalty(overdueDays = stageCompletionOverdueDays, penalty = BigDecimal("999.99")),
      totalDaysForCalculation =
      initialSubmissionOverdueDays + totalRevisionDaysForCalculation + stageCompletionOverdueDays,
    )
  }

  private fun emptyReport() = SingleReportRevisionPenalty(cases = emptyList(), overdueDays = emptyList(), daysForCalculation = emptyList())

  private fun date(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)
}
