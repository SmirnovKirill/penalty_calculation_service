package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.AbstractTest.Companion.date
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogException

class KeyRateServiceTest : AbstractTest() {
  @Autowired
  private lateinit var service: KeyRateService

  @Test
  fun `reads inclusive rate periods and applies rates on weekends`() {
    for ((value, percent) in mapOf(
      "13.02.2022" to "8.50",
      "14.02.2022" to "9.50",
      "26.07.2026" to "14.25",
      "27.07.2026" to "14.00",
    )) {
      assertEquals(BigDecimal(percent), service.rateOn(date(value)), value)
    }
  }

  @ParameterizedTest(name = "{0}: ключевая ставка = {1}")
  @CsvSource(
    value = [
      "31.12.2021, нет ставки",
      "01.01.2022, 8.50",
      "30.11.2026, 14.00",
      "01.01.2051, нет ставки",
    ],
    nullValues = ["нет ставки"],
  )
  fun `reads rates within the loaded catalog dates`(value: String, expected: String?) {
    val date = date(value)

    if (expected != null) {
      assertEquals(BigDecimal(expected), service.rateOn(date))
    } else {
      assertThrows(CatalogException::class.java) { service.rateOn(date) }
    }
  }
}
