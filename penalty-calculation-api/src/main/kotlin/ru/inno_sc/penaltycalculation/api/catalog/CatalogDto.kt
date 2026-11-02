package ru.inno_sc.penaltycalculation.api.catalog

data class CatalogDto(val type: CatalogTypeDto, val source: CatalogSourceDto, val csv: String)
