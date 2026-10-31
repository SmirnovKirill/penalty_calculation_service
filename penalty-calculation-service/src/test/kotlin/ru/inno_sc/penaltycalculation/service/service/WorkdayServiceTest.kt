package ru.inno_sc.penaltycalculation.service.service

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER

class WorkdayServiceTest : AbstractTest() {
  @Autowired
  private lateinit var service: WorkdayService

  @Test
  fun `recognizes holidays and exceptional working weekends`() {
    for ((date, working) in mapOf(
      "09.03.2026" to false,
      "10.03.2026" to true,
      "08.03.2026" to false,
      "01.11.2025" to true,
      "03.11.2025" to false,
    )) {
      assertEquals(working, service.isWorkday(LocalDate.parse(date, DATE_FORMATTER)), date)
    }
  }

  @Test
  fun `counts from the next day using holidays and working weekends`() {
    for ((from, expected) in mapOf(
      "06.03.2026" to "10.03.2026",
      "31.10.2025" to "01.11.2025",
      "30.12.2025" to "12.01.2026",
    )) {
      assertEquals(LocalDate.parse(expected, DATE_FORMATTER), service.addWorkdays(LocalDate.parse(from, DATE_FORMATTER), 1), from)
    }
    val monday = LocalDate.of(2026, 3, 2)
    assertEquals(LocalDate.parse("05.03.2026", DATE_FORMATTER), service.addWorkdays(monday, 3))
  }

  @Test
  fun `keeps the date unchanged for zero workdays on holidays and working days`() {
    for (value in listOf("08.03.2026", "10.03.2026")) {
      val date = LocalDate.parse(value, DATE_FORMATTER)
      assertEquals(date, service.addWorkdays(date, 0), value)
    }
  }

  @Test
  fun `rejects negative counts and dates outside the loaded calendars`() {
    assertThrows(IllegalArgumentException::class.java) { service.addWorkdays(LocalDate.of(2026, 3, 2), -1) }
    val unsupported = LocalDate.of(2021, 12, 31)
    assertThrows(IllegalArgumentException::class.java) { service.isWorkday(unsupported) }
    assertThrows(IllegalArgumentException::class.java) { service.addWorkdays(unsupported, 0) }
    assertThrows(IllegalArgumentException::class.java) { service.addWorkdays(LocalDate.of(2027, 12, 30), 1) }
  }
}
