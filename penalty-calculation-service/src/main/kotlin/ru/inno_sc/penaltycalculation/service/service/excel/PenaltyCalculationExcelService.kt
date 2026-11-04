package ru.inno_sc.penaltycalculation.service.service.excel

import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.DataValidation
import org.apache.poi.ss.usermodel.DataValidationConstraint
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.PrintSetup
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.api.penalty.response.PenaltyCalculationResultDto
import ru.inno_sc.penaltycalculation.api.penalty.response.ReportRevisionDto
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.MAX_SUPPORTED_YEAR
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.MIN_SUPPORTED_YEAR
import ru.inno_sc.penaltycalculation.service.util.MoneyUtils

@Suppress("MagicNumber")
class PenaltyCalculationExcelService {
  private companion object {
    const val COLUMN_WIDTH_UNIT = 256
    const val MAX_EXCEL_PRECISION = 15
    val REPORT_COLUMN_WIDTHS = listOf(9, 18, 18, 18, 42, 85)
    val ACCOUNTING_COLUMN_WIDTHS = listOf(14, 14, 16, 18, 16, 16, 20, 18)
  }

  fun generate(request: CalculatePenaltyRequestDto, result: PenaltyCalculationResultDto): ByteArray = XSSFWorkbook().use { workbook ->
    val styles = ReportStyles(workbook)
    writeSummarySheet(workbook, styles, request, result)
    writeReportSheet(workbook, styles, "Грант", result.reportingDelayPenalty.grantReport, request.grantReport.reportApprovalDate)
    writeReportSheet(workbook, styles, "Внебюджет", result.reportingDelayPenalty.extraBudgetReport, request.extraBudgetReport.reportApprovalDate)
    writeReportSheet(workbook, styles, "Тех", result.reportingDelayPenalty.technicalReport, request.technicalReport.reportApprovalDate)
    writeAccountingSheet(workbook, request, result)

    workbook.setActiveSheet(0)
    ByteArrayOutputStream().use { output ->
      workbook.write(output)
      output.toByteArray()
    }
  }

  private fun writeSummarySheet(
    workbook: XSSFWorkbook,
    styles: ReportStyles,
    request: CalculatePenaltyRequestDto,
    result: PenaltyCalculationResultDto
  ) {
    val summary = workbook.createSheet("Итог").apply {
      listOf(48, 100).forEachIndexed { index, width -> setColumnWidth(index, width * COLUMN_WIDTH_UNIT) }
    }
    writeSummaryHeader(summary, styles, request, result)
    writeSummaryReporting(summary, styles, result)
    writeSummaryCompletion(summary, styles, result)
    writeSummaryTotal(summary, styles, result)
  }

  private fun writeSummaryHeader(summary: Sheet, styles: ReportStyles, request: CalculatePenaltyRequestDto, result: PenaltyCalculationResultDto) {
    row(summary, styles.heading, "Расчёт неустойки")
    row(summary, styles.text, "Договор", result.contractNumber)
    row(summary, styles.text, "Номер этапа", result.stageNumber.toString())
    row(summary, styles.text, "Дата расчёта", result.calculationDate)
    row(summary, styles.text, "Сумма этапа", result.stageAmount)
    row(summary, styles.text, "Ключевая ставка", result.keyRatePercent)
    row(summary, styles.text, "Плановая подача отчётности", request.reportPlannedSubmissionDate.format(DATE_FORMATTER))
    row(summary, styles.text, "Фактическая подача отчётности", request.reportActualSubmissionDate.format(DATE_FORMATTER))
    row(summary, styles.text, "Плановое завершение работ", request.technicalReport.plannedCompletionDate.format(DATE_FORMATTER))
    row(summary, styles.text, "Фактическое завершение работ", request.technicalReport.actualCompletionDate.format(DATE_FORMATTER))
  }

  private fun row(sheet: Sheet, style: CellStyle, vararg values: Any): Row = styledRow(
    sheet,
    *values.map { style to it }.toTypedArray(),
  )

  private fun styledRow(sheet: Sheet, vararg values: Pair<CellStyle, Any>): Row = sheet.createRow(sheet.physicalNumberOfRows).also { row ->
    values.forEachIndexed { index, (style, value) -> cell(row.createCell(index), style, value) }
  }

  private fun cell(cell: Cell, style: CellStyle, value: Any) {
    cell.cellStyle = style
    when (value) {
      is XSSFRichTextString -> cell.setCellValue(value)

      is LocalDate -> cell.setCellValue(value.atStartOfDay())

      // Большие суммы сохраняем текстом: Excel ограничен 15 значащими цифрами.
      is BigDecimal -> if (value.stripTrailingZeros().precision() <= MAX_EXCEL_PRECISION) {
        cell.setCellValue(value.toDouble())
      } else {
        cell.setCellValue(value.toPlainString())
      }

      is Number -> cell.setCellValue(value.toDouble())

      else -> cell.setCellValue(value.toString())
    }
    if (style.dataFormatString == "dd.mm.yyyy") {
      val helper = cell.sheet.dataValidationHelper
      val constraint = helper.createDateConstraint(
        DataValidationConstraint.OperatorType.BETWEEN,
        DateUtil.getExcelDate(LocalDate.of(MIN_SUPPORTED_YEAR, 1, 1).atStartOfDay()).toString(),
        DateUtil.getExcelDate(LocalDate.of(MAX_SUPPORTED_YEAR, 12, 31).atStartOfDay()).toString(),
        "dd.MM.yyyy",
      )
      val validation = helper.createValidation(constraint, CellRangeAddressList(cell.rowIndex, cell.rowIndex, cell.columnIndex, cell.columnIndex))
      validation.errorStyle = DataValidation.ErrorStyle.STOP
      validation.createPromptBox("Дата", "Введите дату в формате дд.мм.гггг.")
      validation.createErrorBox("Некорректная дата", "Укажите дату от 01.01.$MIN_SUPPORTED_YEAR до 31.12.$MAX_SUPPORTED_YEAR.")
      validation.showPromptBox = true
      validation.showErrorBox = true
      cell.sheet.addValidationData(validation)
    }
    if (style.dataFormatString == "0.00%") {
      val helper = cell.sheet.dataValidationHelper
      val constraint = helper.createDecimalConstraint(DataValidationConstraint.OperatorType.BETWEEN, "0", "10")
      val validation = helper.createValidation(constraint, CellRangeAddressList(cell.rowIndex, cell.rowIndex, cell.columnIndex, cell.columnIndex))
      validation.errorStyle = DataValidation.ErrorStyle.STOP
      validation.createPromptBox("Ключевая ставка", "Введите ставку, например 14,25%.")
      validation.createErrorBox("Некорректная ставка", "Укажите ставку от 0 до 1000%.")
      validation.showPromptBox = true
      validation.showErrorBox = true
      cell.sheet.addValidationData(validation)
    }
  }

  private fun writeSummaryReporting(summary: Sheet, styles: ReportStyles, result: PenaltyCalculationResultDto) {
    val reportingDelayPenalty = result.reportingDelayPenalty
    val reportRevisions =
      listOf(reportingDelayPenalty.grantReport, reportingDelayPenalty.extraBudgetReport, reportingDelayPenalty.technicalReport)
    val revisionOverdueDays = reportRevisions.flatMap { it.overdueDays }.distinct().sorted()
    val revisionDaysForCalculation = reportRevisions.flatMap { it.daysForCalculation }.distinct().sorted()
    row(summary, styles.heading, "1. Несвоевременное предоставление отчетности")
    stage(
      summary,
      styles.text,
      styles.heading,
      "Первичная сдача отчётности",
      reportingDelayPenalty.initialSubmissionDaysForCalculation,
    )
    stage(
      summary,
      styles.text,
      styles.heading,
      "Доработки отчётов",
      revisionDaysForCalculation,
      revisionOverdueDays,
      revisionOverdueDays.filterNot { it in revisionDaysForCalculation },
    )
    row(
      summary,
      styles.text,
      "Правило доработки",
      "Дни доработок объединяются после применения исключений отдельно для каждого отчёта. " +
        "Если одна дата участвует в расчёте нескольких отчётов, она учитывается один раз."
    )
    stage(
      summary,
      styles.text,
      styles.heading,
      "Общие дни просрочки по отчётности",
      reportingDelayPenalty.totalDaysForCalculation,
      reportingDelayPenalty.totalOverdueDays,
      reportingDelayPenalty.totalDaysToExclude,
    )
    row(
      summary,
      styles.text,
      "Правило исключений",
      "Первичная сдача рассчитывается без исключений. Дата полностью исключается из общих дней отчётности, " +
        "только если она не участвует в расчёте ни первичной сдачи, ни одного из отчётов."
    )
    writePenaltyCalculation(
      summary,
      styles,
      result,
      "по отчётности",
      reportingDelayPenalty.totalDaysForCalculation.size,
      reportingDelayPenalty.penalty
    )
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

  private fun dayRows(
    sheet: Sheet,
    textStyle: CellStyle,
    headingStyle: CellStyle,
    daysForCalculation: List<LocalDate>,
    overdueDays: List<LocalDate>,
    daysToExclude: List<LocalDate>,
  ) {
    daySteps(overdueDays, daysToExclude, daysForCalculation).forEach { (label, dates) ->
      styledRow(sheet, headingStyle to label, textStyle to dates)
    }
  }

  private fun daySteps(
    overdueDays: List<LocalDate>,
    daysToExclude: List<LocalDate>,
    daysForCalculation: List<LocalDate>,
  ): List<Pair<String, String>> {
    val excludedDaySteps = if (daysToExclude.isEmpty()) {
      emptyList()
    } else {
      listOf(
        "Исходные дни просрочки (${overdueDays.size})" to formatDays(overdueDays),
        "Исключены из расчёта (${daysToExclude.size})" to formatDays(daysToExclude),
      )
    }
    return excludedDaySteps +
      ("Дни просрочки (${daysForCalculation.size})" to formatDays(daysForCalculation).ifEmpty { "нет" })
  }

  private fun formatDays(values: List<LocalDate>): String = values.joinToString(", ") { it.format(DATE_FORMATTER) }

  private fun writePenaltyCalculation(
    sheet: Sheet,
    styles: ReportStyles,
    result: PenaltyCalculationResultDto,
    title: String,
    days: Int,
    penalty: String
  ) {
    val keyRatePercent = result.keyRatePercent.removeSuffix("%")
    row(
      sheet,
      styles.text,
      "Формула",
      "Сумма этапа × ключевая ставка ЦБ / 100 × дни просрочки / 300. Сумма округляется математически до копейки.",
    )
    row(sheet, styles.text, "Расчёт неустойки $title", "${result.stageAmount} × $keyRatePercent / 100 × $days / 300 = $penalty")
    row(sheet, styles.heading, "Неустойка $title", penalty)
  }

  private fun writeSummaryCompletion(summary: Sheet, styles: ReportStyles, result: PenaltyCalculationResultDto) {
    val stageCompletionPenalty = result.stageCompletionPenalty
    stage(
      summary,
      styles.text,
      styles.heading,
      "2. Недостижение результатов этапа реализации проекта",
      stageCompletionPenalty.overdueDays,
    )
    writePenaltyCalculation(
      summary,
      styles,
      result,
      "за недостижение результатов",
      stageCompletionPenalty.overdueDays.size,
      stageCompletionPenalty.penalty
    )
  }

  private fun writeSummaryTotal(summary: Sheet, styles: ReportStyles, result: PenaltyCalculationResultDto) {
    val reportingDelayPenalty = result.reportingDelayPenalty
    val stageCompletionPenalty = result.stageCompletionPenalty
    row(summary, styles.text)
    row(summary, styles.heading, "3. Итог")
    row(summary, styles.heading, "Всего дней просрочки", result.totalDaysForCalculation.size.toString())
    row(summary, styles.text, "Формула", "Сумма двух неустоек, каждая округлена математически до копейки.")
    row(
      summary,
      styles.text,
      "Итоговый расчёт",
      "${reportingDelayPenalty.penalty} + ${stageCompletionPenalty.penalty} = ${result.totalPenalty}",
    )
    row(summary, styles.heading, "Итого", result.totalPenalty)
  }

  private fun writeReportSheet(
    workbook: XSSFWorkbook,
    styles: ReportStyles,
    reportName: String,
    reportRevision: ReportRevisionDto,
    reportApprovalDate: LocalDate,
  ) {
    val sheet = workbook.createSheet(reportName)
    REPORT_COLUMN_WIDTHS.forEachIndexed { index, width -> sheet.setColumnWidth(index, width * COLUMN_WIDTH_UNIT) }
    row(sheet, styles.heading, if (reportName == "Тех") "Технический отчёт" else reportName)
    row(sheet, styles.text, "Дата согласования", reportApprovalDate.format(DATE_FORMATTER))
    dayRows(
      sheet,
      styles.text,
      styles.heading,
      reportRevision.daysForCalculation,
      reportRevision.overdueDays,
      reportRevision.daysToExclude
    )
    row(sheet, styles.heading, "Кейс", "Возврат на доработку", "Плановая подача", "Фактическая подача", "Замечания", "Дни просрочки")
    reportRevision.cases.forEach { case ->
      row(
        sheet,
        styles.text,
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

  private fun writeAccountingSheet(workbook: XSSFWorkbook, request: CalculatePenaltyRequestDto, result: PenaltyCalculationResultDto) {
    val styles = AccountingStyles(workbook)
    val sheet = workbook.createSheet("Бухгалтерская справка")
    ACCOUNTING_COLUMN_WIDTHS.forEachIndexed { index, width -> sheet.setColumnWidth(index, width * COLUMN_WIDTH_UNIT) }
    writeAccountingHeader(sheet, styles, request, result)
    writeAccountingReports(sheet, styles, request, result)
    writeAccountingCalculation(sheet, styles, request, result)
    mergeAccountingSheet(sheet, result)
    configureAccountingPrint(workbook, sheet)
  }

  private fun writeAccountingHeader(
    sheet: Sheet,
    styles: AccountingStyles,
    request: CalculatePenaltyRequestDto,
    result: PenaltyCalculationResultDto
  ) {
    row(sheet, styles.title, "Бухгалтерская справка по расчету неустойки")
    row(sheet, styles.documentDate, request.calculationDate)
    row(sheet, styles.metadata, "Договора гранта: № ${result.contractNumber}")
    row(sheet, styles.metadata, "Дата договора гранта:")
    row(sheet, styles.metadata, "Получатель гранта:")
    row(sheet, styles.metadata, "Этап финансирования: ${result.stageNumber}")
    row(sheet, styles.text)
    row(sheet, styles.heading, "Расчет количества дней просрочки по предоставляемой отчетности в календарных днях:")
  }

  private fun writeAccountingReports(
    sheet: Sheet,
    styles: AccountingStyles,
    request: CalculatePenaltyRequestDto,
    result: PenaltyCalculationResultDto
  ) {
    val reports = listOf(
      result.reportingDelayPenalty.grantReport,
      result.reportingDelayPenalty.extraBudgetReport,
      result.reportingDelayPenalty.technicalReport
    )
    val names = listOf(
      "Отчет о расходах получателя гранта, источником финансового обеспечения которых являются средства гранта",
      "Отчет о расходах получателя гранта, источником финансового обеспечения которых являются внебюджетные средства",
      "Технический отчет о выполнении работ",
    )
    val approvals = listOf(
      request.grantReport.reportApprovalDate,
      request.extraBudgetReport.reportApprovalDate,
      request.technicalReport.reportApprovalDate
    )
    val daysByReport = accountingDays(result)
    reports.forEachIndexed { index, report ->
      row(sheet, styles.reportName, "${index + 1}. ${names[index]}")
      reportTable(sheet, styles, request, report, approvals[index], result.reportingDelayPenalty.initialSubmissionDaysForCalculation.size)
      reportSummary(sheet, styles, report, result.reportingDelayPenalty.initialSubmissionDaysForCalculation, index, daysByReport)
    }
  }

  // Костыль который делает расчет для бухгалтерии чтобы даты учитывались только 1 раз
  private fun accountingDays(result: PenaltyCalculationResultDto): List<List<LocalDate>> {
    val reporting = result.reportingDelayPenalty
    val used = mutableSetOf<LocalDate>()
    return listOf(reporting.grantReport, reporting.extraBudgetReport, reporting.technicalReport).map { report ->
      (reporting.initialSubmissionDaysForCalculation + report.daysForCalculation).distinct().sorted().filter { used.add(it) }
    }
  }

  private fun reportTable(
    sheet: Sheet,
    styles: AccountingStyles,
    request: CalculatePenaltyRequestDto,
    report: ReportRevisionDto,
    approvalDate: LocalDate,
    initialSubmissionDaysForCalculation: Int,
  ) {
    row(
      sheet, styles.tableHeading,
      "Дата завершения этапа/подачи отчетности", "", "Отработка замечаний", "", "", "",
      "Количество дней просрочки предоставления отчета", "Фактическая дата согласования отчета"
    )
    row(sheet, styles.tableHeading, *Array<Any>(8) { "" })
    row(sheet, styles.tableHeading, "план", "факт", "номер кейса", "дата возврата на доработку", "дата подачи доработанного отчета", "", "", "")
    row(sheet, styles.tableHeading, "", "", "", "", "план", "факт", "", "")
    styledRow(
      sheet,
      styles.date to request.reportPlannedSubmissionDate,
      styles.date to request.reportActualSubmissionDate,
      styles.table to "", styles.table to "", styles.table to "", styles.table to "",
      styles.table to initialSubmissionDaysForCalculation,
      styles.date to approvalDate
    )
    report.cases.forEach { case ->
      styledRow(
        sheet,
        styles.table to "",
        styles.table to "",
        styles.tableHeading to "кейс ${case.caseNumber}",
        styles.date to LocalDate.parse(case.returnedForRevisionDate, DATE_FORMATTER),
        styles.date to LocalDate.parse(case.plannedSubmissionDate, DATE_FORMATTER),
        styles.date to LocalDate.parse(case.actualSubmissionDate, DATE_FORMATTER),
        styles.table to case.daysForCalculation.size,
        styles.table to ""
      )
    }
  }

  private fun reportSummary(
    sheet: Sheet,
    styles: AccountingStyles,
    report: ReportRevisionDto,
    initialDays: List<LocalDate>,
    index: Int,
    daysByReport: List<List<LocalDate>>,
  ) {
    val overdue = (initialDays + report.daysForCalculation).distinct()
    val included = daysByReport[index]
    val repeated = daysByReport.take(index).map { previous ->
      (initialDays + report.daysForCalculation).distinct().sorted().filter { it in previous }
    }
    row(sheet, styles.text)
    row(
      sheet,
      styles.heading,
      "Итого дней просрочки в предоставлении отчета:",
      "",
      "",
      "",
      overdue.size,
      plural(overdue.size, "день", "дня", "дней")
    )
    row(sheet, styles.text, reportDaysText(styles, included, index, repeated))
    row(sheet, styles.text)
  }

  private fun plural(number: Int, one: String, few: String, many: String): String = when {
    number % 100 in 11..14 -> many
    number % 10 == 1 -> one
    number % 10 in 2..4 -> few
    else -> many
  }

  private fun reportDaysText(styles: AccountingStyles, included: List<LocalDate>, index: Int, repeated: List<List<LocalDate>>): XSSFRichTextString {
    val dates = if (included.isEmpty()) "" else days(included)
    val spaces = if (index == 2) "  " else " "
    return XSSFRichTextString().apply {
      append(
        "Количество дней просрочки, учитываемые в расчете неустойки:$spaces${included.size} " +
          plural(included.size, "день", "дня", "дней"),
        styles.underlinedFont
      )
      if (index == 1) {
        append(if (dates.isEmpty()) " " else " с ", styles.boldFont)
        append(dates.removePrefix("с ") + " ", styles.textFont)
      } else {
        append(" $dates. ", if (index == 2) styles.boldFont else styles.textFont)
      }
      append(previousDaysText(repeated), styles.textFont)
    }
  }

  private fun previousDaysText(repeated: List<List<LocalDate>>): String = repeated.mapIndexed { index, dates ->
    if (dates.isEmpty()) {
      ""
    } else {
      "\n${days(dates, withCount = true).replaceFirstChar { it.uppercase() }} - " +
        "эти дни учтены в пункте ${index + 1} настоящей бухгалтерской справки."
    }
  }.joinToString("")

  private fun days(values: List<LocalDate>, withCount: Boolean = false): String {
    if (values.isEmpty()) return "нет"
    val result = mutableListOf<String>()
    var start = values.first()
    var end = start
    fun append() {
      val period = "с ${start.minusDays(1).format(DATE_FORMATTER)} по ${end.format(DATE_FORMATTER)}"
      val count = ChronoUnit.DAYS.between(start, end).toInt() + 1
      result += if (withCount) "$period - $count ${plural(count, "день", "дня", "дней")}" else period
    }
    for (value in values.drop(1)) {
      if (value == end.plusDays(1)) {
        end = value
      } else {
        append()
        start = value
        end = value
      }
    }
    append()
    return result.joinToString(", ")
  }

  private fun writeAccountingCalculation(
    sheet: Sheet,
    styles: AccountingStyles,
    request: CalculatePenaltyRequestDto,
    result: PenaltyCalculationResultDto
  ) {
    row(sheet, styles.heading, "Расчет размера неустойки")
    row(sheet, styles.text)
    writeAccountingCalculationHeader(sheet, styles, request)
    val rate = result.keyRatePercent.removeSuffix("%").replace(',', '.').toBigDecimal().movePointLeft(2)
    val daysByReport = accountingDays(result)
    daysByReport.forEachIndexed { index, days ->
      calculationRow(sheet, styles, index + 1, days.size, request.stageAmount, rate)
    }
    styledRow(
      sheet,
      styles.totalText to "ИТОГО:  ",
      styles.calculationHeading to result.reportingDelayPenalty.totalDaysForCalculation.size,
      styles.totalMoney to request.stageAmount,
      styles.totalPercent to rate,
      styles.totalMoney to money(result.reportingDelayPenalty.penalty),
      styles.totalMoney to ""
    )
    writeAccountingTotal(sheet, styles, result)
  }

  private fun writeAccountingCalculationHeader(sheet: Sheet, styles: AccountingStyles, request: CalculatePenaltyRequestDto) {
    row(
      sheet,
      styles.calculationHeading,
      "Пункт",
      "Количество дней просрочки",
      "Сумма этапа",
      "Ключевая ставка Центрального банка РФ на ${request.calculationDate.format(DATE_FORMATTER)}",
      "Сумма неустойки, руб.",
      ""
    )
  }

  private fun calculationRow(sheet: Sheet, styles: AccountingStyles, number: Int, days: Int, amount: BigDecimal, rate: BigDecimal) {
    styledRow(
      sheet,
      styles.calculation to "$number.",
      styles.calculation to days,
      styles.money to amount,
      styles.percent to rate,
      styles.money to referencePenalty(amount, rate, days),
      styles.money to ""
    )
    row(sheet, styles.calculation, *Array<Any>(6) { "" })
  }

  // Справочный расчёт: округлённые суммы строк могут не сложиться в итог, как и у бухгалтерии.
  // Корректный итог берём из основного расчёта, а не складываем эти строки.
  private fun referencePenalty(amount: BigDecimal, rate: BigDecimal, days: Int): BigDecimal =
    amount.multiply(rate).multiply(BigDecimal.valueOf(days.toLong())).divide(BigDecimal("300"), 2, HALF_UP)

  private fun money(value: String): BigDecimal = value.replace(Regex("[^0-9,.-]"), "").replace(',', '.').toBigDecimal()

  private fun writeAccountingTotal(sheet: Sheet, styles: AccountingStyles, result: PenaltyCalculationResultDto) {
    row(sheet, styles.text)
    styledRow(
      sheet,
      styles.text to "Итого сумма неустойки по данной бухгалтерской справке составляет",
      styles.text to "",
      styles.text to "",
      styles.text to "",
      styles.text to "",
      styles.text to "",
      styles.outsideMoney to money(result.reportingDelayPenalty.penalty)
    )
    row(sheet, styles.text, MoneyUtils.moneyInWords(money(result.reportingDelayPenalty.penalty)))
    row(sheet, styles.text)
    row(sheet, styles.text, "Бухгалтер", "", "", "", "", "")
  }

  // todo разобраться
  private fun mergeAccountingSheet(sheet: Sheet, result: PenaltyCalculationResultDto) {
    // Шапка документа. Диапазоны считаются от первой строки указанного блока.
    merge(sheet, 0, "A1:H1", "A2:H2", "A5:H5")

    // Три отчёта: название, четыре строки шапки, первичная подача, кейсы и итоги.
    var startRow = 8
    val reports = listOf(
      result.reportingDelayPenalty.grantReport,
      result.reportingDelayPenalty.extraBudgetReport,
      result.reportingDelayPenalty.technicalReport
    )
    reports.forEach { report ->
      val last = 6 + report.cases.size
      merge(
        sheet, startRow,
        "A1:H1",
        "A2:B3", "C2:F3", "G2:G5", "H2:H5",
        "A4:A5", "B4:B5", "C4:C5", "D4:D5", "E4:F4",
        "A6:A$last", "B6:B$last", "H6:H$last",
        "A${last + 2}:D${last + 2}", "A${last + 3}:H${last + 3}"
      )
      startRow += 10 + report.cases.size
    }

    // Денежная таблица, итог и подпись бухгалтера.
    merge(
      sheet, startRow,
      "E3:F3",
      "A4:A5", "B4:B5", "C4:C5", "D4:D5", "E4:F5",
      "A6:A7", "B6:B7", "C6:C7", "D6:D7", "E6:F7",
      "A8:A9", "B8:B9", "C8:C9", "D8:D9", "E8:F9",
      "E10:F10", "A12:F12", "A13:H13", "A15:C15", "F15:H15"
    )
  }

  private fun merge(sheet: Sheet, startRow: Int, vararg ranges: String) {
    ranges.forEach { range ->
      val region = CellRangeAddress.valueOf(range).apply {
        firstRow += startRow
        lastRow += startRow
      }
      val style = sheet.getRow(region.firstRow).getCell(region.firstColumn).cellStyle
      for (index in region.firstRow..region.lastRow) {
        val row = sheet.getRow(index)
        for (column in region.firstColumn..region.lastColumn) {
          (row.getCell(column) ?: row.createCell(column)).cellStyle = style
        }
      }
      if (region.numberOfCells > 1) sheet.addMergedRegion(region)
    }
  }

  private fun configureAccountingPrint(workbook: XSSFWorkbook, sheet: Sheet) {
    sheet.fitToPage = true
    sheet.printSetup.apply {
      paperSize = PrintSetup.LETTER_PAPERSIZE
      fitWidth = 1
      fitHeight = 0
    }
    val first = sheet.first()
    workbook.setPrintArea(workbook.getSheetIndex(sheet), first.firstCellNum.toInt(), first.lastCellNum - 1, first.rowNum, sheet.lastRowNum)
  }
}
