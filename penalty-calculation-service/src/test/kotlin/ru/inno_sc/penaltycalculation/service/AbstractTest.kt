package ru.inno_sc.penaltycalculation.service

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.convention.TestBean
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.service.catalog.CatalogService

@SpringBootTest(classes = [Application::class])
abstract class AbstractTest {
  @TestBean(methodName = "fixedClock", enforceOverride = true)
  protected lateinit var clock: Clock

  @TestBean(methodName = "testCatalogDirectory", enforceOverride = true)
  protected lateinit var catalogDirectory: Path

  @Autowired
  protected lateinit var catalogService: CatalogService

  companion object {
    fun date(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)

    @JvmStatic
    fun fixedClock(): Clock = Clock.fixed(date("28.10.2026").atTime(9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @JvmStatic
    fun testCatalogDirectory(): Path = Files.createTempDirectory("penalty-catalog-test-")
  }
}
