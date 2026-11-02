package ru.inno_sc.penaltycalculation.service.service

import java.time.LocalDate
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogType
import ru.inno_sc.penaltycalculation.service.domain.catalog.WorkdayCalendarModel
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogException
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException
import ru.inno_sc.penaltycalculation.service.service.catalog.CatalogService

class WorkdayService(private val catalogs: CatalogService) {
  fun isWorkday(date: LocalDate): Boolean = isWorkday(date, catalogs.model(CatalogType.WORKDAY_CALENDAR))

  fun addWorkdays(from: LocalDate, days: Int): LocalDate {
    if (days < 1) throw CatalogValidationException("Количество рабочих дней должно быть больше нуля.")
    val calendar = catalogs.model<WorkdayCalendarModel>(CatalogType.WORKDAY_CALENDAR)
    if (calendar.years.none { it.year == from.year }) {
      throw CatalogException("Производственный календарь за ${from.year} год не загружен.")
    }
    var date = from
    var remainingDays = days
    while (remainingDays > 0) {
      date = date.plusDays(1)
      if (isWorkday(date, calendar)) remainingDays--
    }
    return date
  }

  private fun isWorkday(date: LocalDate, calendar: WorkdayCalendarModel): Boolean {
    val year = calendar.years.find { it.year == date.year }
      ?: throw CatalogException("Производственный календарь за ${date.year} год не загружен.")
    return date in year.workingDays || (date.dayOfWeek.value <= 5 && date !in year.daysOff)
  }
}
