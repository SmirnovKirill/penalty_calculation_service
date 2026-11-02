package ru.inno_sc.penaltycalculation.service.domain.catalog

import java.time.LocalDate

data class WorkdayCalendarModel(val years: List<YearInfo>) : CatalogModel()

data class YearInfo(val year: Int, val daysOff: Set<LocalDate>, val workingDays: Set<LocalDate>)
