package ru.inno_sc.penaltycalculation.service.service.excel

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ReportStyles(workbook: XSSFWorkbook) {
  val text: CellStyle = workbook.createCellStyle().apply {
    wrapText = true
    verticalAlignment = VerticalAlignment.TOP
  }
  val heading: CellStyle = workbook.createCellStyle().apply {
    cloneStyleFrom(text)
    setFont(workbook.createFont().apply { bold = true })
  }
}
