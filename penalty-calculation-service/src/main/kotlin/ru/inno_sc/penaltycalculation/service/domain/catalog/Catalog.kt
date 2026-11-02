package ru.inno_sc.penaltycalculation.service.domain.catalog

data class Catalog<T : CatalogModel>(val type: CatalogType, val source: CatalogSource, val csv: String, val model: T)
