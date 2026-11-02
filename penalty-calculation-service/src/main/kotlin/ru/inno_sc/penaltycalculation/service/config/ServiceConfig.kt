package ru.inno_sc.penaltycalculation.service.config

import java.nio.file.Path
import java.nio.file.Paths
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.CATALOG_DIRECTORY
import ru.inno_sc.penaltycalculation.service.service.KeyRateService
import ru.inno_sc.penaltycalculation.service.service.PenaltyCalculationService
import ru.inno_sc.penaltycalculation.service.service.ReportRevisionPlannedDateService
import ru.inno_sc.penaltycalculation.service.service.WorkdayService
import ru.inno_sc.penaltycalculation.service.service.catalog.CatalogService
import ru.inno_sc.penaltycalculation.service.service.catalog.loader.CatalogModelLoader
import ru.inno_sc.penaltycalculation.service.service.catalog.loader.KeyRateCatalogModelLoader
import ru.inno_sc.penaltycalculation.service.service.catalog.loader.WorkdayCalendarModelLoader
import ru.inno_sc.penaltycalculation.service.service.excel.PenaltyCalculationExcelService
import ru.inno_sc.penaltycalculation.service.validation.PenaltyCalculationValidator

@Configuration
@Import(
  KeyRateCatalogModelLoader::class,
  WorkdayCalendarModelLoader::class,
  KeyRateService::class,
  WorkdayService::class,
  PenaltyCalculationValidator::class,
  ReportRevisionPlannedDateService::class,
  PenaltyCalculationService::class,
  PenaltyCalculationExcelService::class,
)
class ServiceConfig {
  @Bean
  fun catalogDirectory(): Path = Paths.get(CATALOG_DIRECTORY)

  @Bean
  fun catalogService(catalogDirectory: Path, loaders: List<CatalogModelLoader<*>>): CatalogService =
    CatalogService(catalogDirectory, loaders.associateBy { it.type })
}
