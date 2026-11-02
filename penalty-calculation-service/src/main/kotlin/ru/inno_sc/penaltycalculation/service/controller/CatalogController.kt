package ru.inno_sc.penaltycalculation.service.controller

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.inno_sc.penaltycalculation.api.Paths
import ru.inno_sc.penaltycalculation.api.catalog.CatalogDto
import ru.inno_sc.penaltycalculation.api.catalog.CatalogTypeDto
import ru.inno_sc.penaltycalculation.service.domain.catalog.Catalog
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogType
import ru.inno_sc.penaltycalculation.service.service.catalog.CatalogService

@RestController
class CatalogController(private val catalogService: CatalogService) {
  @GetMapping(Paths.CATALOG)
  fun getCatalog(@PathVariable("catalogType") catalogType: CatalogTypeDto): CatalogDto = catalogService.get(CatalogType.fromDto(catalogType)).toDto()

  @PutMapping(Paths.CATALOG, consumes = [MediaType.TEXT_PLAIN_VALUE])
  fun saveCatalog(@PathVariable("catalogType") catalogType: CatalogTypeDto, @RequestBody csv: String): CatalogDto =
    catalogService.save(CatalogType.fromDto(catalogType), csv).toDto()

  @DeleteMapping(Paths.CATALOG)
  fun resetCatalog(@PathVariable("catalogType") catalogType: CatalogTypeDto): CatalogDto =
    catalogService.reset(CatalogType.fromDto(catalogType)).toDto()

  private fun Catalog<*>.toDto(): CatalogDto = CatalogDto(type.dto, source.dto, csv)
}
