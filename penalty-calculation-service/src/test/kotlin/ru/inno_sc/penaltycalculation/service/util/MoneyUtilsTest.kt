package ru.inno_sc.penaltycalculation.service.util

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MoneyUtilsTest {
  @Test
  fun `spells out the accounting statement amount`() {
    assertEquals("(двести пятьдесят тысяч) рублей 00 копеек.", MoneyUtils.moneyInWords(BigDecimal("250000.00")))
  }

  @Test
  fun `uses feminine thousands and singular currency forms`() {
    assertEquals("(две тысячи один) рубль 01 копейка.", MoneyUtils.moneyInWords(BigDecimal("2001.01")))
  }

  @Test
  fun `uses plural currency forms for two`() {
    assertEquals("(два) рубля 02 копейки.", MoneyUtils.moneyInWords(BigDecimal("2.02")))
  }

  @Test
  fun `spells out zero`() {
    assertEquals("(ноль) рублей 00 копеек.", MoneyUtils.moneyInWords(BigDecimal("0.00")))
  }
}
