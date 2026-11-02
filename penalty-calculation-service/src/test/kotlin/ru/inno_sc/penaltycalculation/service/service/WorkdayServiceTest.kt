package ru.inno_sc.penaltycalculation.service.service

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.AbstractTest.Companion.date
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogException
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException

class WorkdayServiceTest : AbstractTest() {
  @Autowired
  private lateinit var service: WorkdayService

  @Test
  fun `recognizes holidays and exceptional working weekends`() {
    for ((value, working) in mapOf(
      "09.03.2026" to false,
      "10.03.2026" to true,
      "08.03.2026" to false,
      "01.11.2025" to true,
      "03.11.2025" to false,
    )) {
      assertEquals(working, service.isWorkday(date(value)), value)
    }
  }

  @Test
  fun `counts from the next day using holidays and working weekends`() {
    for ((from, expected) in mapOf(
      "06.03.2026" to "10.03.2026",
      "31.10.2025" to "01.11.2025",
      "30.12.2025" to "12.01.2026",
    )) {
      assertEquals(date(expected), service.addWorkdays(date(from), 1), from)
    }
    val monday = date("02.03.2026")
    assertEquals(date("05.03.2026"), service.addWorkdays(monday, 3))
  }

  @ParameterizedTest(name = "{0} рабочих дней: разрешено = {1}")
  @CsvSource("-1, false", "0, false", "1, true")
  fun `requires a positive number of workdays`(days: Int, allowed: Boolean) {
    val from = date("02.03.2026")

    if (allowed) {
      assertEquals(date("03.03.2026"), service.addWorkdays(from, days))
    } else {
      val error = assertThrows(CatalogValidationException::class.java) { service.addWorkdays(from, days) }
      assertEquals("Количество рабочих дней должно быть больше нуля.", error.message)
    }
  }

  @ParameterizedTest(name = "{0}: календарь загружен = {1}")
  @CsvSource("31.12.2021, false", "01.01.2022, true", "31.12.2027, true", "01.01.2051, false")
  fun `checks dates within the loaded calendar years`(value: String, available: Boolean) {
    val date = date(value)

    if (available) {
      assertDoesNotThrow { service.isWorkday(date) }
    } else {
      assertThrows(CatalogException::class.java) { service.isWorkday(date) }
    }
  }
}
