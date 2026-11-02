package ru.inno_sc.penaltycalculation.service.domain.catalog

import ru.inno_sc.penaltycalculation.api.catalog.CatalogSourceDto

enum class CatalogSource(val dto: CatalogSourceDto) {
  BUILTIN(CatalogSourceDto.BUILTIN),
  CUSTOM(CatalogSourceDto.CUSTOM),
}
