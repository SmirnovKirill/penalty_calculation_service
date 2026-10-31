package ru.inno_sc.penaltycalculation.api.penalty.request

import java.time.LocalDate

data class ReportCaseDto(
  val returnedForRevisionDate: LocalDate,
  val actualSubmissionDate: LocalDate,
  val remarksCorrected: Boolean = true,
)
