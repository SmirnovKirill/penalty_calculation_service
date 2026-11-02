package ru.inno_sc.penaltycalculation.service.domain.catalog

import java.math.BigDecimal
import java.time.LocalDate

data class KeyRateCatalogModel(val periods: List<KeyRatePeriod>) : CatalogModel()

data class KeyRatePeriod(val from: LocalDate, val through: LocalDate, val percent: BigDecimal)
