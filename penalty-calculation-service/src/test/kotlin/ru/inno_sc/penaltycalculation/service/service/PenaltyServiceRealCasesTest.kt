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

    // 1. Неустойка по несвоевременной первой сдаче отчёта
    assertEquals(datesRange("24.12.2025", "25.12.2025"), result.initialSubmissionPenalty.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysToExcludeManually)
    assertEquals(datesRange("24.12.2025", "25.12.2025"), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(2, result.initialSubmissionPenalty.daysForCalculation.size)
    assertEquals(BigDecimal("46666.67"), result.initialSubmissionPenalty.penalty)

    // 2. Неустойка по несвоевременной доработке отчёта
    // 2.1 Грант
    assertEquals(listOf(date("12.03.2026"), date("18.03.2026")), result.revisionsPenalty.grantReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.grantReport.daysToExcludeManually)
    assertEquals(listOf(date("12.03.2026"), date("18.03.2026")), result.revisionsPenalty.grantReport.daysForCalculation)
    assertEquals(2, result.revisionsPenalty.grantReport.daysForCalculation.size)

    // 2.2 Внебюджет
    assertEquals(listOf(date("12.03.2026")) + datesRange("18.03.2026", "19.03.2026"), result.revisionsPenalty.extraBudgetReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.extraBudgetReport.daysToExcludeManually)
    assertEquals(
      listOf(date("12.03.2026")) + datesRange("18.03.2026", "19.03.2026"),
      result.revisionsPenalty.extraBudgetReport.daysForCalculation
    )
    assertEquals(3, result.revisionsPenalty.extraBudgetReport.daysForCalculation.size)

    // 2.3 Технический отчёт
    assertEquals(datesRange("30.01.2026", "16.02.2026") + date("12.03.2026"), result.revisionsPenalty.technicalReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.technicalReport.daysToExcludeManually)
    assertEquals(datesRange("30.01.2026", "16.02.2026") + date("12.03.2026"), result.revisionsPenalty.technicalReport.daysForCalculation)
    assertEquals(19, result.revisionsPenalty.technicalReport.daysForCalculation.size)

    // 2.4 Итого
    assertEquals(
      datesRange("30.01.2026", "16.02.2026") + date("12.03.2026") + datesRange("18.03.2026", "19.03.2026"),
      result.revisionsPenalty.totalOverdueDays
    )
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.totalDaysToExcludeManually)
    assertEquals(
      datesRange("30.01.2026", "16.02.2026") + date("12.03.2026") + datesRange("18.03.2026", "19.03.2026"),
      result.revisionsPenalty.totalDaysForCalculation
    )
    assertEquals(21, result.revisionsPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("490000.00"), result.revisionsPenalty.penalty)

    // 3. Неустойка по просрочке выполнения работ этапа
    assertEquals(datesRange("23.12.2025", "09.01.2026"), result.stageCompletionPenalty.overdueDays)
    assertEquals(BigDecimal("420000.00"), result.stageCompletionPenalty.penalty)
    assertEquals(18, result.stageCompletionPenalty.overdueDays.size)

    // Итог
    assertEquals(41, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("956666.67"), result.totalPenalty)
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

    // 1. Неустойка по несвоевременной первой сдаче отчёта
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(0, result.initialSubmissionPenalty.daysForCalculation.size)
    assertEquals(BigDecimal("0.00"), result.initialSubmissionPenalty.penalty)

    // 2. Неустойка по несвоевременной доработке отчёта
    // 2.1 Грант
    assertEquals(datesRange("28.10.2025", "30.10.2025") + datesRange("06.11.2025", "18.11.2025"), result.revisionsPenalty.grantReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.grantReport.daysToExcludeManually)
    assertEquals(
      datesRange("28.10.2025", "30.10.2025") + datesRange("06.11.2025", "18.11.2025"),
      result.revisionsPenalty.grantReport.daysForCalculation
    )
    assertEquals(16, result.revisionsPenalty.grantReport.daysForCalculation.size)

    // 2.2 Внебюджет
    assertEquals(
      datesRange("28.10.2025", "30.10.2025") + datesRange("06.11.2025", "18.11.2025"),
      result.revisionsPenalty.extraBudgetReport.overdueDays
    )
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.extraBudgetReport.daysToExcludeManually)
    assertEquals(
      datesRange("28.10.2025", "30.10.2025") + datesRange("06.11.2025", "18.11.2025"),
      result.revisionsPenalty.extraBudgetReport.daysForCalculation
    )
    assertEquals(16, result.revisionsPenalty.extraBudgetReport.daysForCalculation.size)

    // 2.3 Технический отчёт
    assertEquals(datesRange("21.10.2025", "18.11.2025"), result.revisionsPenalty.technicalReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.technicalReport.daysToExcludeManually)
    assertEquals(datesRange("21.10.2025", "18.11.2025"), result.revisionsPenalty.technicalReport.daysForCalculation)
    assertEquals(29, result.revisionsPenalty.technicalReport.daysForCalculation.size)

    // 2.4 Итого
    assertEquals(datesRange("21.10.2025", "18.11.2025"), result.revisionsPenalty.totalOverdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.totalDaysToExcludeManually)
    assertEquals(datesRange("21.10.2025", "18.11.2025"), result.revisionsPenalty.totalDaysForCalculation)
    assertEquals(29, result.revisionsPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("602805.58"), result.revisionsPenalty.penalty)

    // 3. Неустойка по просрочке выполнения работ этапа
    assertEquals(datesRange("26.09.2025", "12.10.2025"), result.stageCompletionPenalty.overdueDays)
    assertEquals(BigDecimal("353368.79"), result.stageCompletionPenalty.penalty)
    assertEquals(17, result.stageCompletionPenalty.overdueDays.size) // todo тут расхождение с бухгалтерией!

    // Итог
    assertEquals(46, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("956174.37"), result.totalPenalty)
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

    // 1. Неустойка по несвоевременной первой сдаче отчёта
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(0, result.initialSubmissionPenalty.daysForCalculation.size)
    assertEquals(BigDecimal("0.00"), result.initialSubmissionPenalty.penalty)

    // 2. Неустойка по несвоевременной доработке отчёта
    // 2.1 Грант
    assertEquals(
      datesRange("09.10.2024", "10.10.2024") + date("16.01.2025") + datesRange("12.02.2025", "21.02.2025"),
      result.revisionsPenalty.grantReport.overdueDays
    )
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.grantReport.daysToExcludeManually)
    assertEquals(
      datesRange("09.10.2024", "10.10.2024") + date("16.01.2025") + datesRange("12.02.2025", "21.02.2025"),
      result.revisionsPenalty.grantReport.daysForCalculation
    )
    assertEquals(13, result.revisionsPenalty.grantReport.daysForCalculation.size)

    // 2.2 Внебюджет
    assertEquals(listOf(date("09.10.2024")) + datesRange("12.02.2025", "21.02.2025"), result.revisionsPenalty.extraBudgetReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.extraBudgetReport.daysToExcludeManually)
    assertEquals(
      listOf(date("09.10.2024")) + datesRange("12.02.2025", "21.02.2025"),
      result.revisionsPenalty.extraBudgetReport.daysForCalculation
    )
    assertEquals(11, result.revisionsPenalty.extraBudgetReport.daysForCalculation.size)

    // 2.3 Технический отчёт
    assertEquals(listOf(date("10.10.2024")), result.revisionsPenalty.technicalReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.technicalReport.daysToExcludeManually)
    assertEquals(listOf(date("10.10.2024")), result.revisionsPenalty.technicalReport.daysForCalculation)
    assertEquals(1, result.revisionsPenalty.technicalReport.daysForCalculation.size)

    // 2.4 Итого
    assertEquals(
      datesRange("09.10.2024", "10.10.2024") + date("16.01.2025") + datesRange("12.02.2025", "21.02.2025"),
      result.revisionsPenalty.totalOverdueDays
    )
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.totalDaysToExcludeManually)
    assertEquals(
      datesRange("09.10.2024", "10.10.2024") + date("16.01.2025") + datesRange("12.02.2025", "21.02.2025"),
      result.revisionsPenalty.totalDaysForCalculation
    )
    assertEquals(13, result.revisionsPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("1319500.00"), result.revisionsPenalty.penalty)

    // 3. Неустойка по просрочке выполнения работ этапа
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

    // 1. Неустойка по несвоевременной первой сдаче отчёта
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(0, result.initialSubmissionPenalty.daysForCalculation.size)
    assertEquals(BigDecimal("0.00"), result.initialSubmissionPenalty.penalty)

    // 2. Неустойка по несвоевременной доработке отчёта
    // 2.1 Грант
    assertEquals(datesRange("27.05.2026", "29.05.2026"), result.revisionsPenalty.grantReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.grantReport.daysToExcludeManually)
    assertEquals(datesRange("27.05.2026", "29.05.2026"), result.revisionsPenalty.grantReport.daysForCalculation)
    assertEquals(3, result.revisionsPenalty.grantReport.daysForCalculation.size)

    // 2.2 Внебюджет
    assertEquals(listOf(date("29.05.2026")), result.revisionsPenalty.extraBudgetReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.extraBudgetReport.daysToExcludeManually)
    assertEquals(listOf(date("29.05.2026")), result.revisionsPenalty.extraBudgetReport.daysForCalculation)
    assertEquals(1, result.revisionsPenalty.extraBudgetReport.daysForCalculation.size)

    // 2.3 Технический отчёт
    assertEquals(datesRange("04.07.2026", "06.07.2026"), result.revisionsPenalty.technicalReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.technicalReport.daysToExcludeManually)
    assertEquals(datesRange("04.07.2026", "06.07.2026"), result.revisionsPenalty.technicalReport.daysForCalculation)
    assertEquals(3, result.revisionsPenalty.technicalReport.daysForCalculation.size)

    // 2.4 Итого
    assertEquals(datesRange("27.05.2026", "29.05.2026") + datesRange("04.07.2026", "06.07.2026"), result.revisionsPenalty.totalOverdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.totalDaysToExcludeManually)
    assertEquals(datesRange("27.05.2026", "29.05.2026") + datesRange("04.07.2026", "06.07.2026"), result.revisionsPenalty.totalDaysForCalculation)
    assertEquals(6, result.revisionsPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("525000.00"), result.revisionsPenalty.penalty)

    // 3. Неустойка по просрочке выполнения работ этапа
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

    // 1. Неустойка по несвоевременной первой сдаче отчёта
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(0, result.initialSubmissionPenalty.daysForCalculation.size)
    assertEquals(BigDecimal("0.00"), result.initialSubmissionPenalty.penalty)

    // 2. Неустойка по несвоевременной доработке отчёта
    // 2.1 Грант
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.grantReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.grantReport.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.grantReport.daysForCalculation)
    assertEquals(0, result.revisionsPenalty.grantReport.daysForCalculation.size)

    // 2.2 Внебюджет
    assertEquals(datesRange("20.06.2026", "22.06.2026"), result.revisionsPenalty.extraBudgetReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.extraBudgetReport.daysToExcludeManually)
    assertEquals(datesRange("20.06.2026", "22.06.2026"), result.revisionsPenalty.extraBudgetReport.daysForCalculation)
    assertEquals(3, result.revisionsPenalty.extraBudgetReport.daysForCalculation.size)

    // 2.3 Технический отчёт
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.technicalReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.technicalReport.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.technicalReport.daysForCalculation)
    assertEquals(0, result.revisionsPenalty.technicalReport.daysForCalculation.size)

    // 2.4 Итого
    assertEquals(datesRange("20.06.2026", "22.06.2026"), result.revisionsPenalty.totalOverdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.totalDaysToExcludeManually)
    assertEquals(datesRange("20.06.2026", "22.06.2026"), result.revisionsPenalty.totalDaysForCalculation)
    assertEquals(3, result.revisionsPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("63063.38"), result.revisionsPenalty.penalty)

    // 3. Неустойка по просрочке выполнения работ этапа
    assertEquals(datesRange("16.03.2026", "24.03.2026"), result.stageCompletionPenalty.overdueDays)
    assertEquals(BigDecimal("189190.13"), result.stageCompletionPenalty.penalty)
    assertEquals(10, result.stageCompletionPenalty.overdueDays.size) // todo не сходится тоже

    // Итог
    assertEquals(3, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("63063.38"), result.totalPenalty)
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

    // 1. Неустойка по несвоевременной первой сдаче отчёта
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysToExcludeManually)
    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysForCalculation)
    assertEquals(0, result.initialSubmissionPenalty.daysForCalculation.size)
    assertEquals(BigDecimal("0.00"), result.initialSubmissionPenalty.penalty)

    // 2. Неустойка по несвоевременной доработке отчёта
    // 2.1 Грант
    assertEquals(listOf(date("23.06.2026")), result.revisionsPenalty.grantReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.grantReport.daysToExcludeManually)
    assertEquals(listOf(date("23.06.2026")), result.revisionsPenalty.grantReport.daysForCalculation)
    assertEquals(1, result.revisionsPenalty.grantReport.daysForCalculation.size)

    // 2.2 Внебюджет
    assertEquals(datesRange("23.12.2025", "26.12.2025"), result.revisionsPenalty.extraBudgetReport.overdueDays)
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.extraBudgetReport.daysToExcludeManually)
    assertEquals(datesRange("23.12.2025", "26.12.2025"), result.revisionsPenalty.extraBudgetReport.daysForCalculation)
    assertEquals(4, result.revisionsPenalty.extraBudgetReport.daysForCalculation.size)

    // 2.3 Технический отчёт
    assertEquals(
      datesRange("13.12.2025", "25.05.2026") + datesRange("02.06.2026", "16.06.2026"),
      result.revisionsPenalty.technicalReport.overdueDays
    )
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.technicalReport.daysToExcludeManually)
    assertEquals(
      datesRange("13.12.2025", "25.05.2026") + datesRange("02.06.2026", "16.06.2026"),
      result.revisionsPenalty.technicalReport.daysForCalculation
    )
    assertEquals(179, result.revisionsPenalty.technicalReport.daysForCalculation.size)

    // 2.4 Итого
    assertEquals(
      datesRange("13.12.2025", "25.05.2026") + datesRange("02.06.2026", "16.06.2026") + date("23.06.2026"),
      result.revisionsPenalty.totalOverdueDays
    )
    assertEquals(emptyList<LocalDate>(), result.revisionsPenalty.totalDaysToExcludeManually)
    assertEquals(
      datesRange("13.12.2025", "25.05.2026") + datesRange("02.06.2026", "16.06.2026") + date("23.06.2026"),
      result.revisionsPenalty.totalDaysForCalculation
    )
    assertEquals(180, result.revisionsPenalty.totalDaysForCalculation.size)
    assertEquals(BigDecimal("7930549.61"), result.revisionsPenalty.penalty)

    // 3. Неустойка по просрочке выполнения работ этапа
    assertEquals(datesRange("26.11.2025", "15.06.2026"), result.stageCompletionPenalty.overdueDays)
    assertEquals(BigDecimal("8899839.00"), result.stageCompletionPenalty.penalty)
    assertEquals(202, result.stageCompletionPenalty.overdueDays.size) // todo и тут не сходится

    // Итог
    assertEquals(382, result.totalDaysForCalculation.size)
    assertEquals(BigDecimal("16830388.61"), result.totalPenalty)
  }

  private fun readRequest(fileName: String): CalculatePenaltyRequestDto {
    val json = requireNotNull(javaClass.getResourceAsStream("/test_cases/$fileName")).use {
      mapper.readTree(it)
    } as ObjectNode
    return mapper.treeToValue(json, CalculatePenaltyRequestDto::class.java)
  }

  private fun date(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)

  private fun datesRange(from: String, through: String): List<LocalDate> = date(from).datesUntil(date(through).plusDays(1)).toList()

  companion object {
    @JvmStatic
    fun fixedClock(): Clock = Clock.fixed(LocalDate.parse("29.10.2026", DATE_FORMATTER).atTime(9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
  }
}
