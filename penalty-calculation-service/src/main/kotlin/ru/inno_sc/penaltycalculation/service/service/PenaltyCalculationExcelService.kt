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
import ru.inno_sc.penaltycalculation.api.penalty.response.ReportRevisionDto
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
    val reportingDelayPenalty = result.reportingDelayPenalty
    val stageCompletionPenalty = result.stageCompletionPenalty
    val reportRevisions =
      listOf(reportingDelayPenalty.grantReport, reportingDelayPenalty.extraBudgetReport, reportingDelayPenalty.technicalReport)
    val revisionOverdueDays = reportRevisions.flatMap { it.overdueDays }.distinct().sorted()
    val revisionDaysForCalculation = reportRevisions.flatMap { it.daysForCalculation }.distinct().sorted()
    val keyRatePercent = result.keyRatePercent.removeSuffix("%")
    val formulaPrefix = "${result.stageAmount} × $keyRatePercent / 100 × "
    val formula = "Сумма этапа × ключевая ставка ЦБ / 100 × дни просрочки / 300. Сумма округляется вверх до копейки."

    row(summary, headingStyle, "1. Несвоевременное предоставление отчетности")
    stage(
      summary,
      textStyle,
      headingStyle,
      "Первичная сдача отчётности",
      reportingDelayPenalty.initialSubmissionDaysForCalculation,
    )
    stage(
      summary,
      textStyle,
      headingStyle,
      "Доработки отчётов",
      revisionDaysForCalculation,
      revisionOverdueDays,
      revisionOverdueDays.filterNot { it in revisionDaysForCalculation },
    )
    row(
      summary,
      textStyle,
      "Правило доработки",
      "Дни доработок объединяются после применения исключений отдельно для каждого отчёта. " +
        "Если одна дата участвует в расчёте нескольких отчётов, она учитывается один раз."
    )
    stage(
      summary,
      textStyle,
      headingStyle,
      "Общие дни просрочки по отчётности",
      reportingDelayPenalty.totalDaysForCalculation,
      reportingDelayPenalty.totalOverdueDays,
      reportingDelayPenalty.totalDaysToExclude,
    )
    row(
      summary,
      textStyle,
      "Правило исключений",
      "Первичная сдача рассчитывается без исключений. Дата полностью исключается из общих дней отчётности, " +
        "только если она не участвует в расчёте ни первичной сдачи, ни одного из отчётов."
    )
    row(summary, textStyle, "Формула", formula)
    row(
      summary,
      textStyle,
      "Расчёт неустойки по отчётности",
      "$formulaPrefix${reportingDelayPenalty.totalDaysForCalculation.size} / 300 = ${reportingDelayPenalty.penalty}",
    )
    row(summary, headingStyle, "Неустойка по отчётности", reportingDelayPenalty.penalty)

    stage(
      summary,
      textStyle,
      headingStyle,
      "2. Недостижение результатов этапа реализации проекта",
      stageCompletionPenalty.overdueDays,
    )
    row(summary, textStyle, "Формула", formula)
    row(
      summary,
      textStyle,
      "Расчёт неустойки за недостижение результатов",
      "$formulaPrefix${stageCompletionPenalty.overdueDays.size} / 300 = ${stageCompletionPenalty.penalty}",
    )
    row(summary, headingStyle, "Неустойка за недостижение результатов", stageCompletionPenalty.penalty)

    row(summary, textStyle)
    row(summary, headingStyle, "3. Итог")
    row(summary, headingStyle, "Всего дней просрочки", result.totalDaysForCalculation.size.toString())
    row(summary, textStyle, "Формула", "Сумма двух неустоек, каждая округлена вверх до копейки.")
    row(
      summary,
      textStyle,
      "Итоговый расчёт",
      "${reportingDelayPenalty.penalty} + ${stageCompletionPenalty.penalty} = ${result.totalPenalty}",
    )
    row(summary, headingStyle, "Итого", result.totalPenalty)

    report(workbook, textStyle, headingStyle, "Грант", reportingDelayPenalty.grantReport, request.grantReport.reportApprovalDate)
    report(
      workbook,
      textStyle,
      headingStyle,
      "Внебюджет",
      reportingDelayPenalty.extraBudgetReport,
      request.extraBudgetReport.reportApprovalDate
    )
    report(workbook, textStyle, headingStyle, "Тех", reportingDelayPenalty.technicalReport, request.technicalReport.reportApprovalDate)
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
    daysToExclude: List<LocalDate>,
    daysForCalculation: List<LocalDate>,
  ): List<Pair<String, String>> {
    val excludedDaySteps = if (daysToExclude.isEmpty()) {
      emptyList()
    } else {
      listOf(
        "Исходные дни просрочки (${overdueDays.size})" to days(overdueDays),
        "Исключены из расчёта (${daysToExclude.size})" to days(daysToExclude),
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
    daysToExclude: List<LocalDate>,
  ) {
    daySteps(overdueDays, daysToExclude, daysForCalculation).forEach { (label, dates) ->
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
    daysToExclude: List<LocalDate> = emptyList(),
  ) {
    row(sheet, headingStyle, title)
    dayRows(sheet, textStyle, headingStyle, daysForCalculation, overdueDays, daysToExclude)
  }

  private fun report(
    workbook: XSSFWorkbook,
    textStyle: CellStyle,
    headingStyle: CellStyle,
    reportName: String,
    reportRevision: ReportRevisionDto,
    reportApprovalDate: LocalDate,
  ) {
    val sheet = workbook.createSheet(reportName)
    REPORT_COLUMN_WIDTHS.forEachIndexed { index, width -> sheet.setColumnWidth(index, width * COLUMN_WIDTH_UNIT) }
    row(sheet, headingStyle, if (reportName == "Тех") "Технический отчёт" else reportName)
    row(sheet, textStyle, "Дата согласования", reportApprovalDate.format(DATE_FORMATTER))
    dayRows(sheet, textStyle, headingStyle, reportRevision.daysForCalculation, reportRevision.overdueDays, reportRevision.daysToExclude)
    row(sheet, headingStyle, "Кейс", "Возврат на доработку", "Плановая подача", "Фактическая подача", "Замечания", "Дни просрочки")
    reportRevision.cases.forEach { case ->
      row(
        sheet,
        textStyle,
        case.caseNumber.toString(),
        case.returnedForRevisionDate,
        case.plannedSubmissionDate,
        case.actualSubmissionDate,
        if (case.remarksCorrected) "Исправлены" else "Возобновляется фиксация нарушения срока.",
        daySteps(case.overdueDays, case.daysToExclude, case.daysForCalculation)
          .joinToString("\n") { (label, dates) -> "$label: $dates" }
      )
    }
  }
}
