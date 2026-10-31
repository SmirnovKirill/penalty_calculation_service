package ru.inno_sc.penaltycalculation.api.penalty.request

import java.time.LocalDate

data class DayRangeDto(
  val startDate: LocalDate,
  val endDate: LocalDate,
)
