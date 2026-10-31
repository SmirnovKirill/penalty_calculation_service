package ru.inno_sc.penaltycalculation.api.penalty.request

import java.time.LocalDate

data class GrantReportDto(
  val cases: List<ReportCaseDto>,
  val reportApprovalDate: LocalDate,
)
