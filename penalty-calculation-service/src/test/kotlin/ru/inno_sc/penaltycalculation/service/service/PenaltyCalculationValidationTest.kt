package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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
import ru.inno_sc.penaltycalculation.service.AbstractTest.Companion.date
import ru.inno_sc.penaltycalculation.service.validation.PenaltyCalculationValidator

class PenaltyCalculationValidationTest : AbstractTest() {
  @Autowired
  private lateinit var service: PenaltyCalculationService

  // Границы дат

  @ParameterizedTest(name = "{0}: разрешена = {1}")
  @CsvSource("31.12.2021, false", "01.01.2022, true", "31.12.2050, true", "01.01.2051, false")
  fun `checks supported calculation date boundaries`(value: String, allowed: Boolean) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы даты расчёта; остальные даты равны 01.01.2022.
    val case = ReportCaseDto(returnedForRevisionDate = date("01.01.2022"), actualSubmissionDate = date("01.01.2022"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = candidate,
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = date("01.01.2022"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        plannedCompletionDate = date("01.01.2022"),
        actualCompletionDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("Дата расчёта: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{0}: разрешена = {1}")
  @CsvSource("31.12.2021, false", "01.01.2022, true", "31.12.2050, true", "01.01.2051, false")
  fun `checks supported planned submission date boundaries`(value: String, allowed: Boolean) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value)
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = candidate, // Проверяем обе границы: 2022–2050.
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

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("Плановая дата подачи отчётности: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{0}: разрешена = {1}")
  @CsvSource("31.12.2021, false", "01.01.2022, true", "31.12.2050, true", "01.01.2051, false")
  fun `checks supported actual submission date boundaries`(value: String, allowed: Boolean) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы фактической подачи; доработки и согласование — 31.12.2050.
    val case = ReportCaseDto(returnedForRevisionDate = date("31.12.2050"), actualSubmissionDate = date("31.12.2050"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = candidate,
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("31.12.2050"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("31.12.2050"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("31.12.2050"),
        plannedCompletionDate = date("01.01.2022"),
        actualCompletionDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("Фактическая дата подачи отчётности: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{2}, {0}: разрешена = {1}")
  @CsvSource(
    "31.12.2021, false, grantReport, Грант",
    "01.01.2022, true, grantReport, Грант",
    "31.12.2050, true, grantReport, Грант",
    "01.01.2051, false, grantReport, Грант",
    "31.12.2021, false, extraBudgetReport, Внебюджет",
    "01.01.2022, true, extraBudgetReport, Внебюджет",
    "31.12.2050, true, extraBudgetReport, Внебюджет",
    "01.01.2051, false, extraBudgetReport, Внебюджет",
    "31.12.2021, false, technicalReport, Тех",
    "01.01.2022, true, technicalReport, Тех",
    "31.12.2050, true, technicalReport, Тех",
    "01.01.2051, false, technicalReport, Тех",
  )
  fun `checks supported revision return date boundaries`(value: String, allowed: Boolean, report: String, reportName: String) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы возврата на доработку; подача и согласование — 31.12.2050.
    val case = ReportCaseDto(returnedForRevisionDate = date("01.01.2022"), actualSubmissionDate = date("31.12.2050"))
    val checkedCase = ReportCaseDto(
      returnedForRevisionDate = candidate, // Меняется дата возврата выбранного раздела.
      actualSubmissionDate = date("31.12.2050"),
    )
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = date("01.01.2022"),
      grantReport = GrantReportDto(
        cases = listOf(if (report == "grantReport") checkedCase else case),
        reportApprovalDate = date("31.12.2050"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(if (report == "extraBudgetReport") checkedCase else case),
        reportApprovalDate = date("31.12.2050"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(if (report == "technicalReport") checkedCase else case),
        reportApprovalDate = date("31.12.2050"),
        plannedCompletionDate = date("01.01.2022"),
        actualCompletionDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("$reportName, строка 1: дата возврата на доработку: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{2}, {0}: разрешена = {1}")
  @CsvSource(
    "31.12.2021, false, grantReport, Грант",
    "01.01.2022, true, grantReport, Грант",
    "31.12.2050, true, grantReport, Грант",
    "01.01.2051, false, grantReport, Грант",
    "31.12.2021, false, extraBudgetReport, Внебюджет",
    "01.01.2022, true, extraBudgetReport, Внебюджет",
    "31.12.2050, true, extraBudgetReport, Внебюджет",
    "01.01.2051, false, extraBudgetReport, Внебюджет",
    "31.12.2021, false, technicalReport, Тех",
    "01.01.2022, true, technicalReport, Тех",
    "31.12.2050, true, technicalReport, Тех",
    "01.01.2051, false, technicalReport, Тех",
  )
  fun `checks supported revision submission date boundaries`(value: String, allowed: Boolean, report: String, reportName: String) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы подачи доработки; возврат — 01.01.2022, согласование — 31.12.2050.
    val case = ReportCaseDto(returnedForRevisionDate = date("01.01.2022"), actualSubmissionDate = date("01.01.2022"))
    val checkedCase = ReportCaseDto(
      returnedForRevisionDate = date("01.01.2022"),
      actualSubmissionDate = candidate, // Меняется дата подачи выбранного раздела.
    )
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = date("01.01.2022"),
      grantReport = GrantReportDto(
        cases = listOf(if (report == "grantReport") checkedCase else case),
        reportApprovalDate = date("31.12.2050"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(if (report == "extraBudgetReport") checkedCase else case),
        reportApprovalDate = date("31.12.2050"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(if (report == "technicalReport") checkedCase else case),
        reportApprovalDate = date("31.12.2050"),
        plannedCompletionDate = date("01.01.2022"),
        actualCompletionDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("$reportName, строка 1: дата подачи доработанного отчёта: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{2}, {0}: разрешена = {1}")
  @CsvSource(
    "31.12.2021, false, grantReport, Грант",
    "01.01.2022, true, grantReport, Грант",
    "31.12.2050, true, grantReport, Грант",
    "01.01.2051, false, grantReport, Грант",
    "31.12.2021, false, extraBudgetReport, Внебюджет",
    "01.01.2022, true, extraBudgetReport, Внебюджет",
    "31.12.2050, true, extraBudgetReport, Внебюджет",
    "01.01.2051, false, extraBudgetReport, Внебюджет",
    "31.12.2021, false, technicalReport, Тех",
    "01.01.2022, true, technicalReport, Тех",
    "31.12.2050, true, technicalReport, Тех",
    "01.01.2051, false, technicalReport, Тех",
  )
  fun `checks supported approval date boundaries`(value: String, allowed: Boolean, report: String, reportName: String) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы согласования выбранного раздела; доработка завершена 01.01.2022.
    val case = ReportCaseDto(returnedForRevisionDate = date("01.01.2022"), actualSubmissionDate = date("01.01.2022"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = date("01.01.2022"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = if (report == "grantReport") candidate else date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = if (report == "extraBudgetReport") candidate else date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = if (report == "technicalReport") candidate else date("01.01.2022"),
        plannedCompletionDate = date("01.01.2022"),
        actualCompletionDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("$reportName: дата согласования отчёта: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{0}: разрешена = {1}")
  @CsvSource("31.12.2021, false", "01.01.2022, true", "31.12.2050, true", "01.01.2051, false")
  fun `checks supported planned completion date boundaries`(value: String, allowed: Boolean) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы планового завершения; фактическое завершение может быть раньше.
    val case = ReportCaseDto(returnedForRevisionDate = date("01.01.2022"), actualSubmissionDate = date("01.01.2022"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = date("01.01.2022"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        plannedCompletionDate = candidate,
        actualCompletionDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("Плановая дата завершения работ: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{0}: разрешена = {1}")
  @CsvSource("31.12.2021, false", "01.01.2022, true", "31.12.2050, true", "01.01.2051, false")
  fun `checks supported actual completion date boundaries`(value: String, allowed: Boolean) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы фактического завершения работ.
    val case = ReportCaseDto(returnedForRevisionDate = date("01.01.2022"), actualSubmissionDate = date("01.01.2022"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = date("01.01.2022"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        plannedCompletionDate = date("01.01.2022"),
        actualCompletionDate = candidate,
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("Фактическая дата завершения работ: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{2}, {0}: разрешена = {1}")
  @CsvSource(
    "31.12.2021, false, grantReport, Грант",
    "01.01.2022, true, grantReport, Грант",
    "31.12.2050, true, grantReport, Грант",
    "01.01.2051, false, grantReport, Грант",
    "31.12.2021, false, extraBudgetReport, Внебюджет",
    "01.01.2022, true, extraBudgetReport, Внебюджет",
    "31.12.2050, true, extraBudgetReport, Внебюджет",
    "01.01.2051, false, extraBudgetReport, Внебюджет",
    "31.12.2021, false, technicalReport, Тех",
    "01.01.2022, true, technicalReport, Тех",
    "31.12.2050, true, technicalReport, Тех",
    "01.01.2051, false, technicalReport, Тех",
  )
  fun `checks supported excluded day boundaries`(value: String, allowed: Boolean, report: String, reportName: String) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы исключаемой даты выбранного раздела.
    val case = ReportCaseDto(returnedForRevisionDate = date("01.01.2022"), actualSubmissionDate = date("01.01.2022"))
    val daysToExclude = DaysToExcludeDto(days = listOf(candidate))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = date("01.01.2022"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = if (report == "grantReport") daysToExclude else DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = if (report == "extraBudgetReport") daysToExclude else DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        plannedCompletionDate = date("01.01.2022"),
        actualCompletionDate = date("01.01.2022"),
        daysToExclude = if (report == "technicalReport") daysToExclude else DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("$reportName: исключаемая дата 1: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{2}, {0}: разрешена = {1}")
  @CsvSource(
    "31.12.2021, false, grantReport, Грант",
    "01.01.2022, true, grantReport, Грант",
    "31.12.2050, true, grantReport, Грант",
    "01.01.2051, false, grantReport, Грант",
    "31.12.2021, false, extraBudgetReport, Внебюджет",
    "01.01.2022, true, extraBudgetReport, Внебюджет",
    "31.12.2050, true, extraBudgetReport, Внебюджет",
    "01.01.2051, false, extraBudgetReport, Внебюджет",
    "31.12.2021, false, technicalReport, Тех",
    "01.01.2022, true, technicalReport, Тех",
    "31.12.2050, true, technicalReport, Тех",
    "01.01.2051, false, technicalReport, Тех",
  )
  fun `checks supported exclusion range start boundaries`(value: String, allowed: Boolean, report: String, reportName: String) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы начала исключаемого периода; конец — 31.12.2050.
    val case = ReportCaseDto(returnedForRevisionDate = date("01.01.2022"), actualSubmissionDate = date("01.01.2022"))
    val daysToExclude = DaysToExcludeDto(
      dayRanges = listOf(DayRangeDto(startDate = candidate, endDate = date("31.12.2050"))),
    )
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = date("01.01.2022"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = if (report == "grantReport") daysToExclude else DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = if (report == "extraBudgetReport") daysToExclude else DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        plannedCompletionDate = date("01.01.2022"),
        actualCompletionDate = date("01.01.2022"),
        daysToExclude = if (report == "technicalReport") daysToExclude else DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("$reportName: исключаемый период 1: начало: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{2}, {0}: разрешена = {1}")
  @CsvSource(
    "31.12.2021, false, grantReport, Грант",
    "01.01.2022, true, grantReport, Грант",
    "31.12.2050, true, grantReport, Грант",
    "01.01.2051, false, grantReport, Грант",
    "31.12.2021, false, extraBudgetReport, Внебюджет",
    "01.01.2022, true, extraBudgetReport, Внебюджет",
    "31.12.2050, true, extraBudgetReport, Внебюджет",
    "01.01.2051, false, extraBudgetReport, Внебюджет",
    "31.12.2021, false, technicalReport, Тех",
    "01.01.2022, true, technicalReport, Тех",
    "31.12.2050, true, technicalReport, Тех",
    "01.01.2051, false, technicalReport, Тех",
  )
  fun `checks supported exclusion range end boundaries`(value: String, allowed: Boolean, report: String, reportName: String) {
    val validator = PenaltyCalculationValidator()
    val today = date("02.01.2051")
    val candidate = date(value) // Границы конца периода; начало — 01.01.2022.
    val case = ReportCaseDto(returnedForRevisionDate = date("01.01.2022"), actualSubmissionDate = date("01.01.2022"))
    val daysToExclude = DaysToExcludeDto(
      dayRanges = listOf(DayRangeDto(startDate = date("01.01.2022"), endDate = candidate)),
    )
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("31.12.2050"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("01.01.2022"),
      reportActualSubmissionDate = date("01.01.2022"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = if (report == "grantReport") daysToExclude else DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        daysToExclude = if (report == "extraBudgetReport") daysToExclude else DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("01.01.2022"),
        plannedCompletionDate = date("01.01.2022"),
        actualCompletionDate = date("01.01.2022"),
        daysToExclude = if (report == "technicalReport") daysToExclude else DaysToExcludeDto(),
      ),
    )

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      val boundary = if (candidate.year < 2022) "раньше 01.01.2022" else "позже 31.12.2050"
      assertEquals("$reportName: исключаемый период 1: конец: дата не может быть $boundary.", error.message)
    }
  }

  @ParameterizedTest(name = "{0}: разрешена = {1}")
  @CsvSource("27.10.2026, true", "28.10.2026, true", "29.10.2026, false")
  fun `allows calculation dates before and on today but not after it`(value: String, allowed: Boolean) {
    val validator = PenaltyCalculationValidator()
    val today = date("28.10.2026")
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date(value), // Вчера и сегодня допустимы, завтра — нет.
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

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      assertEquals("Дата расчёта: дата не может быть в будущем.", error.message)
    }
  }

  @ParameterizedTest(name = "{0}: разрешена = {1}")
  @CsvSource("09.11.2025, true", "10.11.2025, true", "11.11.2025, false")
  fun `allows report submission before and on the calculation date but not after it`(value: String, allowed: Boolean) {
    val validator = PenaltyCalculationValidator()
    val today = date("28.10.2026")
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("10.11.2025"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("10.11.2025"),
      reportActualSubmissionDate = date(value), // Не позже даты расчёта 10.11.2025.
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

    if (allowed) {
      assertDoesNotThrow { validator.validate(request, today) }
    } else {
      val error = assertThrows(IllegalArgumentException::class.java) { validator.validate(request, today) }
      assertEquals("Фактическая дата подачи отчётности: дата не может быть в будущем.", error.message)
    }
  }

  @Test
  fun `events after a historical calculation date are rejected even when already in the past`() {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("10.11.2025"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("10.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("11.11.2025"), // После даты расчёта 10.11.2025.
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

    val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(request) }

    assertEquals("Грант: дата согласования отчёта: дата не может быть в будущем.", error.message)
  }

  // Подача и доработка отчётности

  @Test
  fun `allows report submission before its planned date`() {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("11.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"), // Раньше плановой подачи 11.11.2025 — допустимо.
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

    val result = service.calculate(request)

    assertEquals(emptyList<LocalDate>(), result.reportingDelayPenalty.initialSubmissionDaysForCalculation)
  }

  @Test
  fun `allows report submission revision and approval on the same day`() {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("10.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"), // Подача, доработка и согласование в один день.
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

    assertDoesNotThrow { service.calculate(request) }
  }

  @Test
  fun `next revision cannot start before the previous submission`() {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
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
        cases = listOf(
          ReportCaseDto(
            returnedForRevisionDate = date("10.11.2025"),
            actualSubmissionDate = date("12.11.2025"),
            remarksCorrected = false,
          ),
          ReportCaseDto(
            returnedForRevisionDate = date("11.11.2025"), // Раньше предыдущей подачи 12.11.2025.
            actualSubmissionDate = date("13.11.2025"),
          ),
        ),
        reportApprovalDate = date("13.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(request) }

    assertEquals("Тех, строка 2: возврат на доработку не может предшествовать предыдущей подаче.", error.message)
  }

  @Test
  fun `report approval cannot precede the last revision submission`() {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
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
        reportApprovalDate = date("13.11.2025"), // Раньше последней подачи 14.11.2025.
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(request) }

    assertEquals("Тех: согласование не может предшествовать последней подаче отчёта.", error.message)
  }

  @Test
  fun `last revision must correct the remarks`() {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
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
        cases = listOf(
          ReportCaseDto(
            returnedForRevisionDate = date("10.11.2025"),
            actualSubmissionDate = date("11.11.2025"),
            remarksCorrected = false, // Последний кейс: замечания не исправлены.
          ),
        ),
        reportApprovalDate = date("12.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(request) }

    assertEquals("Тех: замечания последнего кейса не исправлены. Добавьте повторную доработку.", error.message)
  }

  @Test
  fun `allows an unresolved revision followed by a correction on the same day`() {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
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
        cases = listOf(
          ReportCaseDto(
            returnedForRevisionDate = date("10.11.2025"),
            actualSubmissionDate = date("11.11.2025"),
            remarksCorrected = false,
          ),
          ReportCaseDto(
            returnedForRevisionDate = date("11.11.2025"),
            actualSubmissionDate = date("11.11.2025"),
            remarksCorrected = true, // Исправленная повторная подача в тот же день.
          ),
        ),
        reportApprovalDate = date("12.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    assertDoesNotThrow { service.calculate(request) }
  }

  // Завершение работ

  @Test
  fun `allows work completion before its planned date`() {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
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
        plannedCompletionDate = date("11.11.2025"),
        actualCompletionDate = date("10.11.2025"), // Раньше планового завершения 11.11.2025 — допустимо.
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    val result = service.calculate(request)

    assertEquals(emptyList<LocalDate>(), result.stageCompletionPenalty.overdueDays)
  }

  @Test
  fun `work completion is independent of approval before or after it`() {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val request = CalculatePenaltyRequestDto(
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
        reportApprovalDate = date("11.11.2025"), // До завершения работ 12.11.2025.
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("12.11.2025"),
        daysToExclude = DaysToExcludeDto(),
      ),
    )

    val approvedBeforeCompletion = service.calculate(request)
    val approvedAfterCompletion = service.calculate(
      // После завершения работ 12.11.2025.
      request.copy(technicalReport = request.technicalReport.copy(reportApprovalDate = date("13.11.2025"))),
    )

    assertEquals(approvedBeforeCompletion.stageCompletionPenalty, approvedAfterCompletion.stageCompletionPenalty)
  }

  // Исключение дней

  @ParameterizedTest
  @CsvSource("grantReport, Грант", "extraBudgetReport, Внебюджет", "technicalReport, Тех")
  fun `rejects an excluded day before the planned reporting date`(report: String, reportName: String) {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val daysToExclude = DaysToExcludeDto(days = listOf(date("10.11.2025"))) // Раньше плановой подачи 11.11.2025.
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("11.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "grantReport") daysToExclude else DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "extraBudgetReport") daysToExclude else DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = if (report == "technicalReport") daysToExclude else DaysToExcludeDto(),
      ),
    )

    val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(request) }

    assertEquals("$reportName: исключаемая дата 1: дата не может предшествовать плановой дате подачи отчётности.", error.message)
  }

  @ParameterizedTest
  @CsvSource("grantReport, Грант", "extraBudgetReport, Внебюджет", "technicalReport, Тех")
  fun `rejects ranges partly or wholly before the planned reporting date`(report: String, reportName: String) {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val partlyExcludedDays = DaysToExcludeDto(
      // Часть периода до плановой подачи 12.11.2025.
      dayRanges = listOf(DayRangeDto(startDate = date("11.11.2025"), endDate = date("13.11.2025"))),
    )
    val whollyExcludedDays = DaysToExcludeDto(
      // Весь период до плановой подачи 12.11.2025.
      dayRanges = listOf(DayRangeDto(startDate = date("10.11.2025"), endDate = date("11.11.2025"))),
    )
    val partlyBefore = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("12.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "grantReport") partlyExcludedDays else DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "extraBudgetReport") partlyExcludedDays else DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = if (report == "technicalReport") partlyExcludedDays else DaysToExcludeDto(),
      ),
    )
    val whollyBefore = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("12.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "grantReport") whollyExcludedDays else DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "extraBudgetReport") whollyExcludedDays else DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = if (report == "technicalReport") whollyExcludedDays else DaysToExcludeDto(),
      ),
    )

    val partlyBeforeError = assertThrows(IllegalArgumentException::class.java) { service.calculate(partlyBefore) }
    val whollyBeforeError = assertThrows(IllegalArgumentException::class.java) { service.calculate(whollyBefore) }

    assertEquals(
      "$reportName: исключаемый период 1: начало периода не может предшествовать плановой дате подачи отчётности.",
      partlyBeforeError.message,
    )
    assertEquals(
      "$reportName: исключаемый период 1: начало периода не может предшествовать плановой дате подачи отчётности.",
      whollyBeforeError.message,
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `allows an excluded day and a single-day range equal to the planned reporting date`(report: String) {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val daysToExclude = DaysToExcludeDto(
      days = listOf(date("12.11.2025")), // Равно плановой подаче — допустимо.
      dayRanges = listOf(DayRangeDto(startDate = date("12.11.2025"), endDate = date("12.11.2025"))),
    )
    val request = CalculatePenaltyRequestDto(
      formatVersion = 1,
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("12.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "grantReport") daysToExclude else DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "extraBudgetReport") daysToExclude else DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = if (report == "technicalReport") daysToExclude else DaysToExcludeDto(),
      ),
    )

    assertDoesNotThrow { service.calculate(request) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["grantReport", "extraBudgetReport", "technicalReport"])
  fun `allows excluded days and ranges after the planned reporting date`(report: String) {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val daysToExclude = DaysToExcludeDto(
      days = listOf(date("11.11.2025")), // После плановой подачи 10.11.2025.
      dayRanges = listOf(DayRangeDto(startDate = date("12.11.2025"), endDate = date("14.11.2025"))),
    )
    val request = CalculatePenaltyRequestDto(
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
        daysToExclude = if (report == "grantReport") daysToExclude else DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "extraBudgetReport") daysToExclude else DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = if (report == "technicalReport") daysToExclude else DaysToExcludeDto(),
      ),
    )

    assertDoesNotThrow { service.calculate(request) }
  }

  @ParameterizedTest
  @CsvSource("grantReport, Грант", "extraBudgetReport, Внебюджет", "technicalReport, Тех")
  fun `rejects point dates and either interval boundary after a historical calculation date`(report: String, reportName: String) {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val exclusions = listOf(
      DaysToExcludeDto(days = listOf(date("11.11.2025"))) to // После даты расчёта 10.11.2025.
        "$reportName: исключаемая дата 1: дата не может быть в будущем.",
      DaysToExcludeDto(
        // Начало после даты расчёта 10.11.2025.
        dayRanges = listOf(DayRangeDto(startDate = date("11.11.2025"), endDate = date("12.11.2025"))),
      ) to "$reportName: исключаемый период 1: начало: дата не может быть в будущем.",
      DaysToExcludeDto(
        // Конец после даты расчёта 10.11.2025.
        dayRanges = listOf(DayRangeDto(startDate = date("10.11.2025"), endDate = date("11.11.2025"))),
      ) to "$reportName: исключаемый период 1: конец: дата не может быть в будущем.",
    )

    for ((daysToExclude, expectedMessage) in exclusions) {
      val request = CalculatePenaltyRequestDto(
        formatVersion = 1,
        stageNumber = 1,
        calculationDate = date("10.11.2025"),
        contractNumber = "Г-0001",
        stageAmount = BigDecimal("100000"),
        reportPlannedSubmissionDate = date("10.11.2025"),
        reportActualSubmissionDate = date("10.11.2025"),
        grantReport = GrantReportDto(
          cases = listOf(case),
          reportApprovalDate = date("10.11.2025"),
          daysToExclude = if (report == "grantReport") daysToExclude else DaysToExcludeDto(),
        ),
        extraBudgetReport = ExtraBudgetReportDto(
          cases = listOf(case),
          reportApprovalDate = date("10.11.2025"),
          daysToExclude = if (report == "extraBudgetReport") daysToExclude else DaysToExcludeDto(),
        ),
        technicalReport = TechnicalReportDto(
          cases = listOf(case),
          reportApprovalDate = date("10.11.2025"),
          plannedCompletionDate = date("10.11.2025"),
          actualCompletionDate = date("10.11.2025"),
          daysToExclude = if (report == "technicalReport") daysToExclude else DaysToExcludeDto(),
        ),
      )

      val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(request) }

      assertEquals(expectedMessage, error.message)
    }
  }

  @ParameterizedTest
  @CsvSource("grantReport, Грант", "extraBudgetReport, Внебюджет", "technicalReport, Тех")
  fun `rejects reversed intervals in each report`(report: String, reportName: String) {
    val case = ReportCaseDto(returnedForRevisionDate = date("10.11.2025"), actualSubmissionDate = date("10.11.2025"))
    val daysToExclude = DaysToExcludeDto(
      dayRanges = listOf(DayRangeDto(startDate = date("14.11.2025"), endDate = date("12.11.2025"))), // Начало позже конца.
    )
    val request = CalculatePenaltyRequestDto(
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
        daysToExclude = if (report == "grantReport") daysToExclude else DaysToExcludeDto(),
      ),
      extraBudgetReport = ExtraBudgetReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        daysToExclude = if (report == "extraBudgetReport") daysToExclude else DaysToExcludeDto(),
      ),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
        daysToExclude = if (report == "technicalReport") daysToExclude else DaysToExcludeDto(),
      ),
    )

    val error = assertThrows(IllegalArgumentException::class.java) { service.calculate(request) }

    assertEquals("$reportName: исключаемый период 1: начало периода не может быть позже окончания.", error.message)
  }
}
