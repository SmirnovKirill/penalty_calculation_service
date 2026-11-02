package ru.inno_sc.penaltycalculation.service.service.catalog.loader

import java.time.DateTimeException
import java.time.LocalDate
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.MAX_SUPPORTED_YEAR
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.MIN_SUPPORTED_YEAR
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogType
import ru.inno_sc.penaltycalculation.service.domain.catalog.WorkdayCalendarModel
import ru.inno_sc.penaltycalculation.service.domain.catalog.YearInfo
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException

class WorkdayCalendarModelLoader : CatalogModelLoader<WorkdayCalendarModel>() {
  override val type: CatalogType = CatalogType.WORKDAY_CALENDAR

  override fun load(csv: String): WorkdayCalendarModel = parseCsv(csv, listOf("year", "daysOff", "workingDays")) { rows ->
    WorkdayCalendarModel(
      rows.map { row ->
        val year = field(row, "year", ::getValidatedYear)
        YearInfo(
          year,
          field(row, "daysOff") { getValidatedDates(year, it) },
          field(row, "workingDays") { getValidatedDates(year, it) }
        )
      },
    )
  }.also(::validate)

  private fun getValidatedYear(value: String): Int = value.trim().toIntOrNull()
    ?: throw CatalogValidationException("Год календаря должен быть целым числом.")

  private fun getValidatedDates(year: Int, value: String): Set<LocalDate> = value.trim().split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .map {
      try {
        LocalDate.parse("$it.$year", DATE_FORMATTER)
      } catch (error: DateTimeException) {
        throw CatalogValidationException("некорректная дата \"$it\" для $year года. Ожидается формат ДД.ММ.", error)
      }
    }
    .also { dates ->
      if (dates.zipWithNext().any { (previous, next) -> next <= previous }) {
        throw CatalogValidationException("Даты должны идти по возрастанию и не повторяться.")
      }
    }
    .toSet()

  private fun validate(model: WorkdayCalendarModel) {
    model.years.forEachIndexed { index, calendar ->
      if (calendar.year !in MIN_SUPPORTED_YEAR..MAX_SUPPORTED_YEAR) {
        throw CatalogValidationException(
          "Строка ${index + 1}, столбец \"year\", значение \"${calendar.year}\": " +
            "Год календаря должен быть от $MIN_SUPPORTED_YEAR до $MAX_SUPPORTED_YEAR.",
        )
      }
      if (calendar.daysOff.intersect(calendar.workingDays).isNotEmpty()) {
        throw CatalogValidationException("Календарь за ${calendar.year} год: одна дата указана и выходной, и рабочей.")
      }
    }
    if (model.years.map { it.year }.zipWithNext().any { (previous, next) -> next != previous + 1 }) {
      throw CatalogValidationException("Годы календаря должны идти подряд и не повторяться.")
    }
  }
}
