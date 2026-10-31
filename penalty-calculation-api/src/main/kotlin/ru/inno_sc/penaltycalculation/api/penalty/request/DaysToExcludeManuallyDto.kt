package ru.inno_sc.penaltycalculation.api.penalty.request

import java.time.LocalDate

data class DaysToExcludeManuallyDto(
  val days: List<LocalDate> = emptyList(),
  val dayRanges: List<DayRangeDto> = emptyList(),
)
