package ru.inno_sc.penaltycalculation.service.domain.catalog

import ru.inno_sc.penaltycalculation.api.catalog.CatalogTypeDto

enum class CatalogType(val dto: CatalogTypeDto, val fileName: String) {
  KEY_RATES(CatalogTypeDto.KEY_RATES, "key-rates.csv"),
  WORKDAY_CALENDAR(CatalogTypeDto.WORKDAY_CALENDAR, "calendar.csv");

  companion object {
    fun fromDto(dto: CatalogTypeDto): CatalogType = entries.first { it.dto == dto }
  }
}
