package ru.inno_sc.penaltycalculation.service.service

import java.time.LocalDate
import ru.inno_sc.penaltycalculation.service.util.CsvUtils

class WorkdayService {
  private val calendars = CsvUtils.readResource("catalog/calendar.csv").associate { row ->
    val year = row["year"].toInt()
    year to CalendarOverrides(dates(year, row["daysOff"]), dates(year, row["workingDays"]))
  }

  fun isWorkday(date: LocalDate): Boolean {
    val calendar = requireNotNull(calendars[date.year]) {
      "Производственный календарь за ${date.year} год не загружен."
    }
    return date in calendar.workingDays || (date.dayOfWeek.value <= 5 && date !in calendar.daysOff)
  }

  fun addWorkdays(from: LocalDate, days: Int): LocalDate {
    require(days >= 0) { "Количество рабочих дней не может быть отрицательным." }
    require(from.year in calendars) { "Производственный календарь за ${from.year} год не загружен." }
    var date = from
    var remainingDays = days
    while (remainingDays > 0) {
      date = date.plusDays(1)
      if (isWorkday(date)) remainingDays--
    }
    return date
  }

  private fun dates(year: Int, value: String): Set<LocalDate> = value.split(' ')
    .filter { it.isNotBlank() }.map {
      val (day, month) = it.split('.')
      LocalDate.of(year, month.toInt(), day.toInt())
    }.toSet()

  private data class CalendarOverrides(val daysOff: Set<LocalDate>, val workingDays: Set<LocalDate>)
}
