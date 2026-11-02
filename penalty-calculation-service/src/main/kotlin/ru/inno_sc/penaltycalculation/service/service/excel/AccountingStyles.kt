package ru.inno_sc.penaltycalculation.service.service.excel

import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook

@Suppress("MagicNumber")
class AccountingStyles(private val workbook: XSSFWorkbook) {
  private companion object {
    const val MONEY_FORMAT = "#,##0.00;-#,##0.00;-"
  }

  val textFont = font("Calibri", 11)
  val boldFont = font("Calibri", 11, bold = true)
  val underlinedFont = font("Calibri", 11, bold = true).apply { underline = Font.U_SINGLE }
  val text = style(textFont)
  val heading = style(boldFont).apply { wrapText = false }
  val metadata = style(font("Calibri", 11, italic = true)).apply { wrapText = false }
  val title = style(font("Calibri", 16, bold = true)).apply { alignment = HorizontalAlignment.CENTER }
  val documentDate = copy(heading, "dd.mm.yyyy").apply { alignment = HorizontalAlignment.RIGHT }
  val reportName = copy(text).apply { borderBottom = BorderStyle.MEDIUM }
  val table = style(font("Times New Roman", 10), BorderStyle.MEDIUM)
  val tableHeading = style(font("Times New Roman", 10, bold = true), BorderStyle.MEDIUM)
  val date = copy(table, "dd.mm.yyyy")
  val calculation = style(font("Times New Roman", 10), BorderStyle.THIN)
  val calculationHeading = style(font("Times New Roman", 10, bold = true), BorderStyle.THIN)
  val money = money(calculation)
  val percent = copy(calculation, "0.00%")
  val totalText = copy(calculationHeading).apply { alignment = HorizontalAlignment.LEFT }
  val totalMoney = money(calculationHeading)
  val totalPercent = copy(calculationHeading, "0.00%")
  val outsideMoney = money(heading)

  private fun font(name: String, size: Short, bold: Boolean = false, italic: Boolean = false): XSSFFont = workbook.createFont().apply {
    fontName = name
    fontHeightInPoints = size
    this.bold = bold
    this.italic = italic
  }

  private fun style(font: XSSFFont, border: BorderStyle = BorderStyle.NONE): CellStyle = workbook.createCellStyle().apply {
    setFont(font)
    verticalAlignment = VerticalAlignment.CENTER
    alignment = if (border == BorderStyle.NONE) HorizontalAlignment.LEFT else HorizontalAlignment.CENTER
    wrapText = true
    borderLeft = border
    borderRight = border
    borderTop = border
    borderBottom = border
  }

  private fun copy(base: CellStyle, format: String = base.dataFormatString): CellStyle = workbook.createCellStyle().apply {
    cloneStyleFrom(base)
    dataFormat = workbook.createDataFormat().getFormat(format)
  }

  private fun money(base: CellStyle): CellStyle = copy(base, MONEY_FORMAT).apply {
    alignment = HorizontalAlignment.RIGHT
    wrapText = false
    shrinkToFit = true
  }
}
