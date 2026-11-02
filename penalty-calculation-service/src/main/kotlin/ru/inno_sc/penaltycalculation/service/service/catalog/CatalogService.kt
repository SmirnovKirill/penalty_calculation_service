package ru.inno_sc.penaltycalculation.service.service.catalog

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.CATALOG_DIRECTORY
import ru.inno_sc.penaltycalculation.service.domain.catalog.Catalog
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogModel
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogSource
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogType
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogException
import ru.inno_sc.penaltycalculation.service.service.catalog.loader.CatalogModelLoader

class CatalogService(
  private val directory: Path,
  private val loaders: Map<CatalogType, CatalogModelLoader<*>>,
) {
  private val logger = LoggerFactory.getLogger(CatalogService::class.java)

  private val catalogs = CatalogType.entries.associateWith {
    loadCustomCatalog(it) ?: loadBuiltinCatalog(it)
  }.toMutableMap()

  private fun loadCustomCatalog(type: CatalogType): Catalog<*>? = try {
    val path = getCustomCatalogPath(type)
    if (Files.exists(path)) parseCatalog(type, CatalogSource.CUSTOM, Files.readString(path)) else null
  } catch (error: Exception) {
    logger.warn("Не удалось загрузить сохранённый справочник ${type.fileName}. Используется встроенный справочник.", error)
    null
  }

  private fun getCustomCatalogPath(type: CatalogType): Path = directory.resolve(type.fileName)

  private fun parseCatalog(type: CatalogType, source: CatalogSource, csv: String): Catalog<*> {
    val content = csv.removePrefix("\uFEFF")
    val model = loaders.getValue(type).load(content)
    return Catalog(type, source, content, model)
  }

  private fun loadBuiltinCatalog(type: CatalogType): Catalog<*> = try {
    ClassPathResource("$CATALOG_DIRECTORY/${type.fileName}").inputStream.bufferedReader()
      .use { parseCatalog(type, CatalogSource.BUILTIN, it.readText()) }
  } catch (error: IOException) {
    throw CatalogException("Не удалось загрузить встроенный справочник ${type.fileName}.", error)
  }

  fun get(type: CatalogType): Catalog<*> = catalogs.getValue(type)

  inline fun <reified T : CatalogModel> model(type: CatalogType): T = get(type).model as T

  fun save(type: CatalogType, csv: String): Catalog<*> = try {
    val newCatalog = parseCatalog(type, CatalogSource.CUSTOM, csv)
    val file = getCustomCatalogPath(type)
    Files.createDirectories(directory)
    val temporary = Files.createTempFile(directory, ".${file.fileName}-", ".tmp")
    try {
      Files.writeString(temporary, newCatalog.csv)
      Files.move(temporary, file, ATOMIC_MOVE, REPLACE_EXISTING)
    } finally {
      Files.deleteIfExists(temporary)
    }
    catalogs[type] = newCatalog
    newCatalog
  } catch (error: IOException) {
    throw CatalogException("Не удалось сохранить справочник ${type.fileName}.", error)
  }

  fun reset(type: CatalogType): Catalog<*> = try {
    val catalog = loadBuiltinCatalog(type)
    Files.deleteIfExists(getCustomCatalogPath(type))
    catalogs[type] = catalog
    catalog
  } catch (error: IOException) {
    throw CatalogException("Не удалось вернуть встроенный справочник ${type.fileName}.", error)
  }
}
