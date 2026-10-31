package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.convention.TestBean
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.service.Application
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode

@SpringBootTest(classes = [Application::class])
class PenaltyServiceRealCasesTest {
  @TestBean(methodName = "fixedClock", enforceOverride = true)
  private lateinit var clock: Clock

  @Autowired
  private lateinit var service: PenaltyCalculationService

  @Autowired
  private lateinit var mapper: JsonMapper

  @Test
  fun `ROGA I KORYTA`() {
    val request = readRequest("Г-0029_№3_29.10.2026.json")
    val result = service.calculate(request)

    assertEquals(3, result.stageNumber)
    assertEquals("Г-0029", result.contractNumber)
    assertEquals(BigDecimal("50000000"), result.stageAmount)
    assertEquals(date("29.10.2026"), result.calculationDate)
    assertEquals(BigDecimal("14.00"), result.keyRatePercent)

    // 1. Неустойка по задержке отчётности
    // 1.1 Первая сдача
    assertEquals(listOf(date("24.12.2025"), date("25.12.2025")), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
    assertEquals(2, result.reportingDelayPenalty.initialSubmissionDaysForCalculation.size)

    // Доработка отчётов
    // 1.2 Грант
    assertEquals(listOf(date("12.03.2026"), date("18.03.2026")), result.reportingDelayPenalty.grantReport.overdueDays)
    assertEquals(2, result.reportingDelayPenalty.grantReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.grantReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.grantReport.overdueDays,
      result.reportingDelayPenalty.grantReport.daysForCalculation
    )
    assertEquals(2, result.reportingDelayPenalty.grantReport.daysForCalculation.size)

    // 1.3 Внебюджет
    assertEquals(listOf(date("12.03.2026"), date("18.03.2026"), date("19.03.2026")), result.reportingDelayPenalty.extraBudgetReport.overdueDays)
    assertEquals(3, result.reportingDelayPenalty.extraBudgetReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.extraBudgetReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.extraBudgetReport.overdueDays,
      result.reportingDelayPenalty.extraBudgetReport.daysForCalculation
    )
    assertEquals(3, result.reportingDelayPenalty.extraBudgetReport.daysForCalculation.size)

    // 1.4 Технический отчёт
    assertEquals(
      dateRange("30.01.2026", "16.02.2026") + listOf(date("12.03.2026")),
      result.reportingDelayPenalty.technicalReport.overdueDays
    )
    assertEquals(19, result.reportingDelayPenalty.technicalReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.technicalReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.technicalReport.overdueDays,
      result.reportingDelayPenalty.technicalReport.daysForCalculation
    )
    assertEquals(19, result.reportingDelayPenalty.technicalReport.daysForCalculation.size)

    // 1.5 Итого
    assertEquals(
      dateRange("24.12.2025", "25.12.2025") +
        dateRange("30.01.2026", "16.02.2026") +
        listOf(date("12.03.2026")) +
        dateRange("18.03.2026", "19.03.2026"),
      result.reportingDelayPenalty.totalOverdueDays
    )
    assertEquals(23, result.reportingDelayPenalty.totalOverdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.totalDaysToExclude)
    assertEquals(
      result.reportingDelayPenalty.totalOverdueDays,
      result.reportingDelayPenalty.totalDaysForCalculation
    )
    assertEquals(23, result.reportingDelayPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("536666.67"), result.reportingDelayPenalty.penalty)

    // 2. Неустойка по просрочке выполнения работ этапа
    assertEquals(
      dateRange("23.12.2025", "10.01.2026"),
      result.stageCompletionPenalty.overdueDays
    )
    assertEquals(BigDecimal("443333.34"), result.stageCompletionPenalty.penalty)
    assertEquals(19, result.stageCompletionPenalty.overdueDays.size)

    // Итог
    assertEquals(42, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("980000.01"), result.totalPenalty)
  }

  @Test
  fun `INTRA`() {
    val request = readRequest("Г-0014_№3_02.12.2025.json")
    val result = service.calculate(request)

    assertEquals(3, result.stageNumber)
    assertEquals("Г-0014", result.contractNumber)
    assertEquals(BigDecimal("37793453.05"), result.stageAmount)
    assertEquals(date("02.12.2025"), result.calculationDate)
    assertEquals(BigDecimal("16.50"), result.keyRatePercent)

    // 1. Неустойка по задержке отчётности
    // 1.1 Первая сдача
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
    assertEquals(0, result.reportingDelayPenalty.initialSubmissionDaysForCalculation.size)

    // Доработка отчётов
    // 1.2 Грант
    assertEquals(
      dateRange("28.10.2025", "30.10.2025") + dateRange("06.11.2025", "18.11.2025"),
      result.reportingDelayPenalty.grantReport.overdueDays
    )
    assertEquals(16, result.reportingDelayPenalty.grantReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.grantReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.grantReport.overdueDays,
      result.reportingDelayPenalty.grantReport.daysForCalculation
    )
    assertEquals(16, result.reportingDelayPenalty.grantReport.daysForCalculation.size)

    // 1.3 Внебюджет
    assertEquals(
      dateRange("28.10.2025", "30.10.2025") + dateRange("06.11.2025", "18.11.2025"),
      result.reportingDelayPenalty.extraBudgetReport.overdueDays
    )
    assertEquals(16, result.reportingDelayPenalty.extraBudgetReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.extraBudgetReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.extraBudgetReport.overdueDays,
      result.reportingDelayPenalty.extraBudgetReport.daysForCalculation
    )
    assertEquals(16, result.reportingDelayPenalty.extraBudgetReport.daysForCalculation.size)

    // 1.4 Технический отчёт
    assertEquals(
      dateRange("21.10.2025", "18.11.2025"),
      result.reportingDelayPenalty.technicalReport.overdueDays
    )
    assertEquals(29, result.reportingDelayPenalty.technicalReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.technicalReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.technicalReport.overdueDays,
      result.reportingDelayPenalty.technicalReport.daysForCalculation
    )
    assertEquals(29, result.reportingDelayPenalty.technicalReport.daysForCalculation.size)

    // 1.5 Итого
    assertEquals(
      dateRange("21.10.2025", "18.11.2025"),
      result.reportingDelayPenalty.totalOverdueDays
    )
    assertEquals(29, result.reportingDelayPenalty.totalOverdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.totalDaysToExclude)
    assertEquals(
      result.reportingDelayPenalty.totalOverdueDays,
      result.reportingDelayPenalty.totalDaysForCalculation
    )
    assertEquals(29, result.reportingDelayPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("602805.58"), result.reportingDelayPenalty.penalty)

    // 2. Неустойка по просрочке выполнения работ этапа
    assertEquals(
      dateRange("26.09.2025", "13.10.2025"),
      result.stageCompletionPenalty.overdueDays
    )
    assertEquals(BigDecimal("374155.19"), result.stageCompletionPenalty.penalty)
    assertEquals(18, result.stageCompletionPenalty.overdueDays.size)

    // Итог
    assertEquals(47, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("976960.77"), result.totalPenalty)
  }

  @Test
  fun `RUMO`() {
    val request = readRequest("Г-0042_№1_10.03.2025.json")
    val result = service.calculate(request)

    assertEquals(1, result.stageNumber)
    assertEquals("Г-0042", result.contractNumber)
    assertEquals(BigDecimal("145000000"), result.stageAmount)
    assertEquals(date("10.03.2025"), result.calculationDate)
    assertEquals(BigDecimal("21.00"), result.keyRatePercent)

    // 1. Неустойка по задержке отчётности
    // 1.1 Первая сдача
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
    assertEquals(0, result.reportingDelayPenalty.initialSubmissionDaysForCalculation.size)

    // Доработка отчётов
    // 1.2 Грант
    assertEquals(
      dateRange("09.10.2024", "10.10.2024") + listOf(date("16.01.2025")) + dateRange("12.02.2025", "21.02.2025"),
      result.reportingDelayPenalty.grantReport.overdueDays
    )
    assertEquals(13, result.reportingDelayPenalty.grantReport.overdueDays.size)
    assertEquals(
      dateRange("12.02.2025", "21.02.2025"),
      result.reportingDelayPenalty.grantReport.daysToExclude
    )
    assertEquals(10, result.reportingDelayPenalty.grantReport.daysToExclude.size)
    assertEquals(
      listOf(date("09.10.2024"), date("10.10.2024"), date("16.01.2025")),
      result.reportingDelayPenalty.grantReport.daysForCalculation
    )
    assertEquals(3, result.reportingDelayPenalty.grantReport.daysForCalculation.size)

    // 1.3 Внебюджет
    assertEquals(
      listOf(date("09.10.2024")) + dateRange("12.02.2025", "21.02.2025"),
      result.reportingDelayPenalty.extraBudgetReport.overdueDays
    )
    assertEquals(11, result.reportingDelayPenalty.extraBudgetReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.extraBudgetReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.extraBudgetReport.overdueDays,
      result.reportingDelayPenalty.extraBudgetReport.daysForCalculation
    )
    assertEquals(11, result.reportingDelayPenalty.extraBudgetReport.daysForCalculation.size)

    // 1.4 Технический отчёт
    assertEquals(listOf(date("10.10.2024")), result.reportingDelayPenalty.technicalReport.overdueDays)
    assertEquals(1, result.reportingDelayPenalty.technicalReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.technicalReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.technicalReport.overdueDays,
      result.reportingDelayPenalty.technicalReport.daysForCalculation
    )
    assertEquals(1, result.reportingDelayPenalty.technicalReport.daysForCalculation.size)

    // 1.5 Итого
    assertEquals(
      dateRange("09.10.2024", "10.10.2024") + listOf(date("16.01.2025")) + dateRange("12.02.2025", "21.02.2025"),
      result.reportingDelayPenalty.totalOverdueDays
    )
    assertEquals(13, result.reportingDelayPenalty.totalOverdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.totalDaysToExclude)
    assertEquals(
      result.reportingDelayPenalty.totalOverdueDays,
      result.reportingDelayPenalty.totalDaysForCalculation
    )
    assertEquals(13, result.reportingDelayPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("1319500.00"), result.reportingDelayPenalty.penalty)

    // 2. Неустойка по просрочке выполнения работ этапа
    assertEquals(emptyList<LocalDate>(), result.stageCompletionPenalty.overdueDays)
    assertEquals(BigDecimal("0.00"), result.stageCompletionPenalty.penalty)
    assertEquals(0, result.stageCompletionPenalty.overdueDays.size)

    // Итог
    assertEquals(13, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("1319500.00"), result.totalPenalty)
  }

  @Test
  fun `AVRORA`() {
    val request = readRequest("Г-0070_№1_28.08.2026.json")
    val result = service.calculate(request)

    assertEquals(1, result.stageNumber)
    assertEquals("Г-0070", result.contractNumber)
    assertEquals(BigDecimal("187500000"), result.stageAmount)
    assertEquals(date("28.08.2026"), result.calculationDate)
    assertEquals(BigDecimal("14.00"), result.keyRatePercent)

    // 1. Неустойка по задержке отчётности
    // 1.1 Первая сдача
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
    assertEquals(0, result.reportingDelayPenalty.initialSubmissionDaysForCalculation.size)

    // Доработка отчётов
    // 1.2 Грант
    assertEquals(
      dateRange("27.05.2026", "29.05.2026"),
      result.reportingDelayPenalty.grantReport.overdueDays
    )
    assertEquals(3, result.reportingDelayPenalty.grantReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.grantReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.grantReport.overdueDays,
      result.reportingDelayPenalty.grantReport.daysForCalculation
    )
    assertEquals(3, result.reportingDelayPenalty.grantReport.daysForCalculation.size)

    // 1.3 Внебюджет
    assertEquals(listOf(date("29.05.2026")), result.reportingDelayPenalty.extraBudgetReport.overdueDays)
    assertEquals(1, result.reportingDelayPenalty.extraBudgetReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.extraBudgetReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.extraBudgetReport.overdueDays,
      result.reportingDelayPenalty.extraBudgetReport.daysForCalculation
    )
    assertEquals(1, result.reportingDelayPenalty.extraBudgetReport.daysForCalculation.size)

    // 1.4 Технический отчёт
    assertEquals(
      dateRange("04.07.2026", "06.07.2026"),
      result.reportingDelayPenalty.technicalReport.overdueDays
    )
    assertEquals(3, result.reportingDelayPenalty.technicalReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.technicalReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.technicalReport.overdueDays,
      result.reportingDelayPenalty.technicalReport.daysForCalculation
    )
    assertEquals(3, result.reportingDelayPenalty.technicalReport.daysForCalculation.size)

    // 1.5 Итого
    assertEquals(
      dateRange("27.05.2026", "29.05.2026") + dateRange("04.07.2026", "06.07.2026"),
      result.reportingDelayPenalty.totalOverdueDays
    )
    assertEquals(6, result.reportingDelayPenalty.totalOverdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.totalDaysToExclude)
    assertEquals(
      result.reportingDelayPenalty.totalOverdueDays,
      result.reportingDelayPenalty.totalDaysForCalculation
    )
    assertEquals(6, result.reportingDelayPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("525000.00"), result.reportingDelayPenalty.penalty)

    // 2. Неустойка по просрочке выполнения работ этапа
    assertEquals(emptyList<LocalDate>(), result.stageCompletionPenalty.overdueDays)
    assertEquals(BigDecimal("0.00"), result.stageCompletionPenalty.penalty)
    assertEquals(0, result.stageCompletionPenalty.overdueDays.size)

    // Итог
    assertEquals(6, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("525000.00"), result.totalPenalty)
  }

  @Test
  fun `ZTI`() {
    val request = readRequest("Г-0062_№2_25.06.2026.json")
    val result = service.calculate(request)

    assertEquals(2, result.stageNumber)
    assertEquals("Г-0062", result.contractNumber)
    assertEquals(BigDecimal("44255000"), result.stageAmount)
    assertEquals(date("25.06.2026"), result.calculationDate)
    assertEquals(BigDecimal("14.25"), result.keyRatePercent)

    // 1. Неустойка по задержке отчётности
    // 1.1 Первая сдача
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
    assertEquals(0, result.reportingDelayPenalty.initialSubmissionDaysForCalculation.size)

    // Доработка отчётов
    // 1.2 Грант
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.grantReport.overdueDays)
    assertEquals(0, result.reportingDelayPenalty.grantReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.grantReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.grantReport.overdueDays,
      result.reportingDelayPenalty.grantReport.daysForCalculation
    )
    assertEquals(0, result.reportingDelayPenalty.grantReport.daysForCalculation.size)

    // 1.3 Внебюджет
    assertEquals(
      dateRange("20.06.2026", "22.06.2026"),
      result.reportingDelayPenalty.extraBudgetReport.overdueDays
    )
    assertEquals(3, result.reportingDelayPenalty.extraBudgetReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.extraBudgetReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.extraBudgetReport.overdueDays,
      result.reportingDelayPenalty.extraBudgetReport.daysForCalculation
    )
    assertEquals(3, result.reportingDelayPenalty.extraBudgetReport.daysForCalculation.size)

    // 1.4 Технический отчёт
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.technicalReport.overdueDays)
    assertEquals(0, result.reportingDelayPenalty.technicalReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.technicalReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.technicalReport.overdueDays,
      result.reportingDelayPenalty.technicalReport.daysForCalculation
    )
    assertEquals(0, result.reportingDelayPenalty.technicalReport.daysForCalculation.size)

    // 1.5 Итого
    assertEquals(
      dateRange("20.06.2026", "22.06.2026"),
      result.reportingDelayPenalty.totalOverdueDays
    )
    assertEquals(3, result.reportingDelayPenalty.totalOverdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.totalDaysToExclude)
    assertEquals(
      result.reportingDelayPenalty.totalOverdueDays,
      result.reportingDelayPenalty.totalDaysForCalculation
    )
    assertEquals(3, result.reportingDelayPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("63063.38"), result.reportingDelayPenalty.penalty)

    // 2. Неустойка по просрочке выполнения работ этапа
    assertEquals(
      dateRange("16.03.2026", "25.03.2026"),
      result.stageCompletionPenalty.overdueDays
    )
    assertEquals(BigDecimal("210211.25"), result.stageCompletionPenalty.penalty)
    assertEquals(10, result.stageCompletionPenalty.overdueDays.size)

    // Итог
    assertEquals(13, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("273274.63"), result.totalPenalty)
  }

  @Test
  fun `ZSL SOYUZ`() {
    val request = readRequest("Г-0050_№2_17.07.2026.json")
    val result = service.calculate(request)

    assertEquals(2, result.stageNumber)
    assertEquals("Г-0050", result.contractNumber)
    assertEquals(BigDecimal("92754966.11"), result.stageAmount)
    assertEquals(date("17.07.2026"), result.calculationDate)
    assertEquals(BigDecimal("14.25"), result.keyRatePercent)

    // 1. Неустойка по задержке отчётности
    // 1.1 Первая сдача
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
    assertEquals(0, result.reportingDelayPenalty.initialSubmissionDaysForCalculation.size)

    // Доработка отчётов
    // 1.2 Грант
    assertEquals(listOf(date("23.06.2026")), result.reportingDelayPenalty.grantReport.overdueDays)
    assertEquals(1, result.reportingDelayPenalty.grantReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.grantReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.grantReport.overdueDays,
      result.reportingDelayPenalty.grantReport.daysForCalculation
    )
    assertEquals(1, result.reportingDelayPenalty.grantReport.daysForCalculation.size)

    // 1.3 Внебюджет
    assertEquals(
      dateRange("23.12.2025", "26.12.2025"),
      result.reportingDelayPenalty.extraBudgetReport.overdueDays
    )
    assertEquals(4, result.reportingDelayPenalty.extraBudgetReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.extraBudgetReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.extraBudgetReport.overdueDays,
      result.reportingDelayPenalty.extraBudgetReport.daysForCalculation
    )
    assertEquals(4, result.reportingDelayPenalty.extraBudgetReport.daysForCalculation.size)

    // 1.4 Технический отчёт
    assertEquals(
      dateRange("13.12.2025", "25.05.2026") + dateRange("02.06.2026", "16.06.2026"),
      result.reportingDelayPenalty.technicalReport.overdueDays
    )
    assertEquals(179, result.reportingDelayPenalty.technicalReport.overdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.technicalReport.daysToExclude)
    assertEquals(
      result.reportingDelayPenalty.technicalReport.overdueDays,
      result.reportingDelayPenalty.technicalReport.daysForCalculation
    )
    assertEquals(179, result.reportingDelayPenalty.technicalReport.daysForCalculation.size)

    // 1.5 Итого
    assertEquals(
      dateRange("13.12.2025", "25.05.2026") + dateRange("02.06.2026", "16.06.2026") + listOf(date("23.06.2026")),
      result.reportingDelayPenalty.totalOverdueDays
    )
    assertEquals(180, result.reportingDelayPenalty.totalOverdueDays.size)
    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.totalDaysToExclude)
    assertEquals(
      result.reportingDelayPenalty.totalOverdueDays,
      result.reportingDelayPenalty.totalDaysForCalculation
    )
    assertEquals(180, result.reportingDelayPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("7930549.61"), result.reportingDelayPenalty.penalty)

    // 2. Неустойка по просрочке выполнения работ этапа
    assertEquals(
      dateRange("26.11.2025", "16.06.2026"),
      result.stageCompletionPenalty.overdueDays
    )
    assertEquals(BigDecimal("8943897.61"), result.stageCompletionPenalty.penalty)
    assertEquals(203, result.stageCompletionPenalty.overdueDays.size)

    // Итог
    assertEquals(383, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("16874447.22"), result.totalPenalty)
  }

  private fun readRequest(fileName: String): CalculatePenaltyRequestDto {
    val json = requireNotNull(javaClass.getResourceAsStream("/test_cases/$fileName")).use {
      mapper.readTree(it)
    } as ObjectNode
    return mapper.treeToValue(json, CalculatePenaltyRequestDto::class.java)
  }

  private fun dateRange(start: String, end: String): List<LocalDate> = date(start).datesUntil(date(end).plusDays(1)).toList()

  private fun date(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)

  companion object {
    @JvmStatic
    fun fixedClock(): Clock = Clock.fixed(LocalDate.parse("29.10.2026", DATE_FORMATTER).atTime(9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
  }
}
