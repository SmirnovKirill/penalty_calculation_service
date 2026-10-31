package ru.inno_sc.penaltycalculation.service.service

import java.io.ByteArrayOutputStream
import java.time.LocalDate
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.api.penalty.response.PenaltyCalculationResultDto
import ru.inno_sc.penaltycalculation.api.penalty.response.SingleReportRevisionPenaltyDto
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER

class PenaltyCalculationExcelService {
  private companion object {
    const val COLUMN_WIDTH_UNIT = 256
    const val SUMMARY_LABEL_WIDTH = 48
    const val MINIMUM_TEXT_WIDTH = 8
    const val TEXT_LINE_HEIGHT = 15f
    val REPORT_COLUMN_WIDTHS = listOf(9, 18, 18, 18, 42, 85)
  }

  fun generate(request: CalculatePenaltyRequestDto, result: PenaltyCalculationResultDto): ByteArray = XSSFWorkbook().use { workbook ->
    val textStyle = workbook.createCellStyle().apply {
      wrapText = true
      verticalAlignment = VerticalAlignment.TOP
    }
    val headingStyle = workbook.createCellStyle().apply {
      cloneStyleFrom(textStyle)
      setFont(workbook.createFont().apply { bold = true })
    }
    val summary = workbook.createSheet("Итог").apply {
      setColumnWidth(0, SUMMARY_LABEL_WIDTH * COLUMN_WIDTH_UNIT)
      setColumnWidth(1, 100 * COLUMN_WIDTH_UNIT)
    }
    row(summary, headingStyle, "Расчёт неустойки")
    row(summary, textStyle, "Договор", result.contractNumber)
    row(summary, textStyle, "Номер этапа", result.stageNumber.toString())
    row(summary, textStyle, "Дата расчёта", result.calculationDate)
    row(summary, textStyle, "Сумма этапа", result.stageAmount)
    row(summary, textStyle, "Ключевая ставка", result.keyRatePercent)
    row(summary, textStyle, "Плановая подача отчётности", request.reportPlannedSubmissionDate.format(DATE_FORMATTER))
    row(summary, textStyle, "Фактическая подача отчётности", request.reportActualSubmissionDate.format(DATE_FORMATTER))
    row(summary, textStyle, "Плановое завершение работ", request.technicalReport.plannedCompletionDate.format(DATE_FORMATTER))
    row(summary, textStyle, "Фактическое завершение работ", request.technicalReport.actualCompletionDate.format(DATE_FORMATTER))
    result.initialSubmissionPenalty.let {
      stage(
        summary,
        textStyle,
        headingStyle,
        "1. Неустойка по несвоевременной первой сдаче отчёта",
        it.daysForCalculation,
        it.overdueDays,
        it.daysToExcludeManually
      )
    }
    result.revisionsPenalty.let {
      stage(
        summary,
        textStyle,
        headingStyle,
        "2. Неустойка по несвоевременной доработке отчёта",
        it.daysForCalculation,
        it.overdueDays,
        it.daysToExcludeManually
      )
      row(
        summary,
        textStyle,
        "Правило доработки",
        "Неустойка по доработке рассчитывается по уникальным дням просрочки всех трёх отчётов. " +
          "Если одна дата встречается в нескольких отчётах, она учитывается один раз."
      )
    }
    result.stageCompletionPenalty.let {
      stage(
        summary,
        textStyle,
        headingStyle,
        "3. Неустойка по просрочке выполнения работ этапа",
        it.overdueDays
      )
    }
    row(summary, textStyle)
    row(summary, headingStyle, "Всего дней просрочки", result.totalDaysForCalculation.size.toString())
    row(
      summary,
      textStyle,
      "Формула",
      "Формула: сумма этапа × ключевая ставка ЦБ / 100 × сумма дней просрочки по трём типам / 300. Итог округляется вверх до копейки."
    )
    val keyRate = result.keyRatePercent.removeSuffix("%")
    val initialDaysCount = result.initialSubmissionPenalty.daysForCalculation.size
    val revisionDaysCount = result.revisionsPenalty.daysForCalculation.size
    val completionDaysCount = result.stageCompletionPenalty.overdueDays.size
    row(
      summary,
      textStyle,
      "Итоговый расчёт",
      "${result.stageAmount} × $keyRate / 100 × ($initialDaysCount + $revisionDaysCount + $completionDaysCount) / 300 = ${result.totalPenalty}"
    )
    row(summary, headingStyle, "Итого", result.totalPenalty)

    report(workbook, textStyle, headingStyle, "Грант", result.revisionsPenalty.grantReport, request.grantReport.reportApprovalDate)
    report(
      workbook,
      textStyle,
      headingStyle,
      "Внебюджет",
      result.revisionsPenalty.extraBudgetReport,
      request.extraBudgetReport.reportApprovalDate
    )
    report(workbook, textStyle, headingStyle, "Тех", result.revisionsPenalty.technicalReport, request.technicalReport.reportApprovalDate)
    ByteArrayOutputStream().use { output ->
      workbook.write(output)
      output.toByteArray()
    }
  }

  private fun row(sheet: Sheet, style: CellStyle, vararg values: String): Row {
    val row = sheet.createRow(sheet.physicalNumberOfRows)
    values.forEachIndexed { index, value ->
      row.createCell(index).apply {
        setCellValue(value)
        cellStyle = style
      }
    }
    val lines = values.mapIndexed { index, value ->
      val width = (sheet.getColumnWidth(index) / COLUMN_WIDTH_UNIT - 2).coerceAtLeast(MINIMUM_TEXT_WIDTH)
      value.lines().sumOf { (it.length + width - 1).div(width).coerceAtLeast(1) }
    }.maxOrNull() ?: 1
    row.heightInPoints = (lines * TEXT_LINE_HEIGHT + 5).coerceAtMost(400f)
    return row
  }

  private fun days(values: List<LocalDate>): String = values.joinToString(", ") { it.format(DATE_FORMATTER) }

  private fun daySteps(
    overdueDays: List<LocalDate>,
    daysToExcludeManually: List<LocalDate>,
    daysForCalculation: List<LocalDate>,
  ): List<Pair<String, String>> {
    val excludedDaySteps = if (daysToExcludeManually.isEmpty()) {
      emptyList()
    } else {
      listOf(
        "Исходные дни просрочки (${overdueDays.size})" to days(overdueDays),
        "Исключены вручную (${daysToExcludeManually.size})" to days(daysToExcludeManually),
      )
    }
    return excludedDaySteps +
      ("Дни просрочки (${daysForCalculation.size})" to days(daysForCalculation).ifEmpty { "нет" })
  }

  private fun dayRows(
    sheet: Sheet,
    textStyle: CellStyle,
    headingStyle: CellStyle,
    daysForCalculation: List<LocalDate>,
    overdueDays: List<LocalDate>,
    daysToExcludeManually: List<LocalDate>,
  ) {
    daySteps(overdueDays, daysToExcludeManually, daysForCalculation).forEach { (label, dates) ->
      row(sheet, textStyle, label, dates).getCell(0).cellStyle = headingStyle
    }
  }

  private fun stage(
    sheet: Sheet,
    textStyle: CellStyle,
    headingStyle: CellStyle,
    title: String,
    daysForCalculation: List<LocalDate>,
    overdueDays: List<LocalDate> = emptyList(),
    daysToExcludeManually: List<LocalDate> = emptyList(),
  ) {
    row(sheet, headingStyle, title)
    dayRows(sheet, textStyle, headingStyle, daysForCalculation, overdueDays, daysToExcludeManually)
  }

  private fun report(
    workbook: XSSFWorkbook,
    textStyle: CellStyle,
    headingStyle: CellStyle,
    name: String,
    report: SingleReportRevisionPenaltyDto,
    approvalDate: LocalDate,
  ) {
    val sheet = workbook.createSheet(name)
    REPORT_COLUMN_WIDTHS.forEachIndexed { index, width -> sheet.setColumnWidth(index, width * COLUMN_WIDTH_UNIT) }
    row(sheet, headingStyle, if (name == "Тех") "Технический отчёт" else name)
    row(sheet, textStyle, "Дата согласования", approvalDate.format(DATE_FORMATTER))
    dayRows(sheet, textStyle, headingStyle, report.daysForCalculation, report.overdueDays, report.daysToExcludeManually)
    row(sheet, headingStyle, "Кейс", "Возврат на доработку", "Плановая подача", "Фактическая подача", "Замечания", "Дни просрочки")
    report.cases.forEach { case ->
      row(
        sheet,
        textStyle,
        case.caseNumber.toString(),
        case.returnedForRevisionDate,
        case.plannedSubmissionDate,
        case.actualSubmissionDate,
        if (case.remarksCorrected) "Исправлены" else "Возобновляется фиксация нарушения срока.",
        daySteps(case.overdueDays, case.daysToExcludeManually, case.daysForCalculation)
          .joinToString("\n") { (label, dates) -> "$label: $dates" }
      )
    }
  }
}
