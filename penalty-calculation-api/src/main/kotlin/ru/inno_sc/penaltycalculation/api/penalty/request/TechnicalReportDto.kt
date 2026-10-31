package ru.inno_sc.penaltycalculation.api.penalty.request

import java.time.LocalDate

data class TechnicalReportDto(
  val cases: List<ReportCaseDto>,
  val reportApprovalDate: LocalDate,
  val plannedCompletionDate: LocalDate,
  val actualCompletionDate: LocalDate,
)
