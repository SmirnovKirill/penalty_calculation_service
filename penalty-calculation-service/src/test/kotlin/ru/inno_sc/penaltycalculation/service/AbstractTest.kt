package ru.inno_sc.penaltycalculation.service

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.convention.TestBean
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER

@SpringBootTest(classes = [Application::class])
abstract class AbstractTest {
  @TestBean(methodName = "fixedClock", enforceOverride = true)
  protected lateinit var clock: Clock

  companion object {
    @JvmStatic
    fun fixedClock(): Clock = Clock.fixed(LocalDate.parse("28.10.2026", DATE_FORMATTER).atTime(9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
  }
}
