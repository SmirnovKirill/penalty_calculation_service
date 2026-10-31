package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER

class KeyRateServiceTest : AbstractTest() {
  @Autowired
  private lateinit var service: KeyRateService

  @Test
  fun `reads inclusive rate periods and applies rates on weekends`() {
    for ((date, percent) in mapOf(
      "01.01.2022" to "8.50",
      "13.02.2022" to "8.50",
      "14.02.2022" to "9.50",
      "26.07.2026" to "14.25",
      "27.07.2026" to "14.00",
      "30.11.2026" to "14.00",
    )) {
      assertEquals(BigDecimal(percent), service.rateOn(LocalDate.parse(date, DATE_FORMATTER)), date)
    }
  }

  @Test
  fun `does not extrapolate rates outside the catalog`() {
    for (date in listOf("31.12.2021", "01.12.2026")) {
      assertThrows(IllegalArgumentException::class.java) { service.rateOn(LocalDate.parse(date, DATE_FORMATTER)) }
    }
  }
}
