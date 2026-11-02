package ru.inno_sc.penaltycalculation.service.service.catalog

import java.io.IOException
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.inno_sc.penaltycalculation.service.AbstractTest.Companion.date
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogSource
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogType
import ru.inno_sc.penaltycalculation.service.domain.catalog.KeyRateCatalogModel
import ru.inno_sc.penaltycalculation.service.domain.catalog.WorkdayCalendarModel
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogException
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException
import ru.inno_sc.penaltycalculation.service.service.KeyRateService
import ru.inno_sc.penaltycalculation.service.service.catalog.loader.CatalogModelLoader
import ru.inno_sc.penaltycalculation.service.service.catalog.loader.KeyRateCatalogModelLoader
import ru.inno_sc.penaltycalculation.service.service.catalog.loader.WorkdayCalendarModelLoader

class CatalogServiceTest {
  @TempDir
  lateinit var directory: Path

  @Test
  fun `checks the requested model type before returning it`() {
    val catalogs = catalogs()

    assertEquals(catalogs.get(CatalogType.KEY_RATES).model, catalogs.model<KeyRateCatalogModel>(CatalogType.KEY_RATES))
    // Запрашиваем модель календаря из справочника ставок.
    assertThrows(ClassCastException::class.java) {
      catalogs.model<WorkdayCalendarModel>(CatalogType.KEY_RATES)
    }
  }

  @Test
  fun `business calls reuse the parsed model until the catalog changes`() {
    var loads = 0
    val loader = KeyRateCatalogModelLoader()
    val countingLoader = object : CatalogModelLoader<KeyRateCatalogModel>() {
      override val type: CatalogType = CatalogType.KEY_RATES

      override fun load(csv: String): KeyRateCatalogModel {
        loads++
        return loader.load(csv)
      }
    }
    val catalogs = catalogs(rateLoader = countingLoader)
    val rates = KeyRateService(catalogs)
    val date = date("28.10.2026")

    assertEquals(BigDecimal("14.00"), rates.rateOn(date))
    assertEquals(1, loads)

    catalogs.save(
      CatalogType.KEY_RATES,
      """
        from;through;rate
        01.01.2026;31.12.2026;13.25
      """.trimIndent(),
    )

    assertEquals(BigDecimal("13.25"), rates.rateOn(date))
    assertEquals(2, loads)
  }

  @Test
  fun `saved rates immediately replace bundled rates and are loaded by a new service instance`() {
    val catalogs = catalogs()
    val rates = KeyRateService(catalogs)
    assertEquals(CatalogSource.BUILTIN, catalogs.get(CatalogType.KEY_RATES).source)
    assertEquals(BigDecimal("14.00"), rates.rateOn(date("28.10.2026")))

    val csv = "\uFEFF" + """
      # Свои ставки
      from;through;rate
      01.01.2026;31.12.2026;13,25
    """.trimIndent().replace("\n", "\r\n")
    val saved = catalogs.save(CatalogType.KEY_RATES, csv)

    assertEquals(CatalogSource.CUSTOM, saved.source)
    assertEquals(BigDecimal("13.25"), rates.rateOn(date("28.10.2026")))
    assertEquals(saved, catalogs.get(CatalogType.KEY_RATES))
    assertEquals(saved, catalogs().get(CatalogType.KEY_RATES))
    assertEquals(CatalogSource.BUILTIN, catalogs.get(CatalogType.WORKDAY_CALENDAR).source)
  }

  @Test
  fun `reset restores only the selected bundled catalog for current and new service instances`() {
    val catalogs = catalogs()
    val builtinRates = catalogs.get(CatalogType.KEY_RATES)
    catalogs.save(
      CatalogType.KEY_RATES,
      """
        from;through;rate
        01.01.2026;31.12.2026;13.25
      """.trimIndent(),
    )
    val customCalendar = catalogs.save(
      CatalogType.WORKDAY_CALENDAR,
      """
        year;daysOff;workingDays
        2026;03.03;07.03
      """.trimIndent(),
    )

    assertEquals(builtinRates, catalogs.reset(CatalogType.KEY_RATES))

    assertEquals(builtinRates, catalogs.get(CatalogType.KEY_RATES))
    assertEquals(customCalendar, catalogs.get(CatalogType.WORKDAY_CALENDAR))
    val newCatalogs = catalogs()
    assertEquals(builtinRates, newCatalogs.get(CatalogType.KEY_RATES))
    assertEquals(customCalendar, newCatalogs.get(CatalogType.WORKDAY_CALENDAR))
  }

  @Test
  fun `invalid rates leave the saved file and parsed model unchanged`() {
    val catalogs = catalogs()
    val saved = catalogs.save(
      CatalogType.KEY_RATES,
      """
        from;through;rate
        01.01.2026;31.12.2026;13.25
      """.trimIndent(),
    )
    // Между периодами пропущено 11.01.2026.
    val csv = """
      from;through;rate
      01.01.2026;10.01.2026;13.25
      12.01.2026;31.12.2026;14.00
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { catalogs.save(CatalogType.KEY_RATES, csv) }

    assertEquals("Периоды ставок должны идти непрерывно, без пропусков и пересечений.", error.message)
    assertEquals(saved, catalogs.get(CatalogType.KEY_RATES))
    assertEquals(saved.csv, Files.readString(directory.resolve("key-rates.csv")))
  }

  @Test
  fun `corrupted saved file falls back to builtin without changing other catalogs`() {
    val catalogs = catalogs()
    val builtinRates = catalogs.get(CatalogType.KEY_RATES)
    val customCalendar = catalogs.save(
      CatalogType.WORKDAY_CALENDAR,
      """
        year;daysOff;workingDays
        2026;03.03;07.03
      """.trimIndent(),
    )
    val file = directory.resolve("key-rates.csv")
    // 31 февраля не существует.
    val csv = """
      from;through;rate
      31.02.2026;31.12.2026;13.25
    """.trimIndent()
    Files.writeString(file, csv)

    val loaded = catalogs()

    assertEquals(builtinRates, loaded.get(CatalogType.KEY_RATES))
    assertEquals(customCalendar, loaded.get(CatalogType.WORKDAY_CALENDAR))
    assertEquals(csv, Files.readString(file))
  }

  @Test
  fun `unreadable saved file falls back to builtin`() {
    val builtinRates = catalogs().get(CatalogType.KEY_RATES)
    // Вместо CSV создана папка: прочитать её как файл нельзя.
    Files.createDirectory(directory.resolve("key-rates.csv"))

    val loaded = catalogs()

    assertEquals(builtinRates, loaded.get(CatalogType.KEY_RATES))
  }

  @Test
  fun `failed save wraps IO error and leaves active catalog unchanged`() {
    val dataDirectory = directory.resolve("catalog")
    val catalogs = catalogs(dataDirectory)
    val original = catalogs.get(CatalogType.KEY_RATES)
    // Вместо каталога хранения — обычный файл.
    Files.writeString(dataDirectory, "occupied")

    val error = assertThrows(CatalogException::class.java) {
      catalogs.save(
        CatalogType.KEY_RATES,
        """
          from;through;rate
          01.01.2026;31.12.2026;13.25
        """.trimIndent(),
      )
    }

    assertEquals("Не удалось сохранить справочник key-rates.csv.", error.message)
    assertTrue(error.cause is IOException)
    assertEquals(original, catalogs.get(CatalogType.KEY_RATES))
    assertEquals("occupied", Files.readString(dataDirectory))
  }

  @Test
  fun `failed reset wraps IO error and leaves active catalog unchanged`() {
    val catalogs = catalogs()
    val saved = catalogs.save(
      CatalogType.KEY_RATES,
      """
        from;through;rate
        01.01.2026;31.12.2026;13.25
      """.trimIndent(),
    )
    val file = directory.resolve("key-rates.csv")
    // Подменяем CSV непустой папкой: обычное удаление завершится ошибкой.
    Files.delete(file)
    Files.createDirectory(file)
    Files.writeString(file.resolve("occupied"), "occupied")

    val error = assertThrows(CatalogException::class.java) { catalogs.reset(CatalogType.KEY_RATES) }

    assertEquals("Не удалось вернуть встроенный справочник key-rates.csv.", error.message)
    assertTrue(error.cause is IOException)
    assertEquals(saved, catalogs.get(CatalogType.KEY_RATES))
  }

  private fun catalogs(
    directory: Path = this.directory,
    rateLoader: CatalogModelLoader<KeyRateCatalogModel> = KeyRateCatalogModelLoader(),
  ): CatalogService = CatalogService(
    directory,
    listOf(rateLoader, WorkdayCalendarModelLoader()).associateBy { it.type },
  )
}
