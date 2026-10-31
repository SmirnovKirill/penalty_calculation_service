package ru.inno_sc.penaltycalculation.api.penalty.request

import java.time.LocalDate

data class ExtraBudgetReportDto(
  val cases: List<ReportCaseDto>,
  val reportApprovalDate: LocalDate,
)
