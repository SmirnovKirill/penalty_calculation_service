package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.time.LocalDate
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogType
import ru.inno_sc.penaltycalculation.service.domain.catalog.KeyRateCatalogModel
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogException
import ru.inno_sc.penaltycalculation.service.service.catalog.CatalogService

class KeyRateService(private val catalogs: CatalogService) {
  fun rateOn(date: LocalDate): BigDecimal {
    val period = catalogs.model<KeyRateCatalogModel>(CatalogType.KEY_RATES).periods
      .find { date >= it.from && date <= it.through }
      ?: throw CatalogException("Ключевая ставка на ${date.format(DATE_FORMATTER)} не загружена в справочник.")
    return period.percent
  }
}
