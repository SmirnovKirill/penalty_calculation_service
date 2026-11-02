package ru.inno_sc.penaltycalculation.service.service.catalog.loader

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ru.inno_sc.penaltycalculation.service.AbstractTest.Companion.date
import ru.inno_sc.penaltycalculation.service.domain.catalog.KeyRatePeriod
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException

class KeyRateCatalogModelLoaderTest {
  private val loader = KeyRateCatalogModelLoader()

  // Значения ставки

  @Test
  fun `accepts zero rate`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;0
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(BigDecimal("0"), model.periods.single().percent)
  }

  @Test
  fun `accepts rate with a decimal dot`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;13.25
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(BigDecimal("13.25"), model.periods.single().percent)
  }

  @Test
  fun `accepts rate with a decimal comma`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;13,25
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(BigDecimal("13.25"), model.periods.single().percent)
  }

  @Test
  fun `rejects more than two decimal places`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;13.251
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\", значение \"13.251\": Ставка должна содержать не более двух знаков после запятой.", error.message)
  }

  @Test
  fun `accepts the maximum rate of 1000 percent`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;1000
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(BigDecimal("1000"), model.periods.single().percent)
  }

  @Test
  fun `rejects an empty rate`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\": поле не заполнено.", error.message)
  }

  @Test
  fun `identifies whitespace rate in the second record`() {
    val csv = """
      from;through;rate
      01.01.2026;10.01.2026;13.25
      11.01.2026;31.12.2026;"   "
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2, столбец \"rate\": поле не заполнено.", error.message)
  }

  @Test
  fun `rejects a negative rate`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;-1
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\", значение \"-1\": Ставка должна быть от 0 до 1000%.", error.message)
  }

  @Test
  fun `rejects a rate above 1000 percent`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;1000.01
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\", значение \"1000.01\": Ставка должна быть от 0 до 1000%.", error.message)
  }

  @Test
  fun `rejects text instead of a rate`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;ставка
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\", значение \"ставка\": Ставка должна быть числом без знака процента.", error.message)
  }

  @Test
  fun `rejects a rate containing the percent sign`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;13.25%
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\", значение \"13.25%\": Ставка должна быть числом без знака процента.", error.message)
  }

  @Test
  fun `rejects multiple decimal separators`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;13,25.5
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\", значение \"13,25.5\": Ставка должна быть числом без знака процента.", error.message)
  }

  @Test
  fun `rejects NaN as a rate`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;NaN
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\", значение \"NaN\": Ставка должна быть числом без знака процента.", error.message)
  }

  @Test
  fun `rejects infinity as a rate`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;Infinity
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\", значение \"Infinity\": Ставка должна быть числом без знака процента.", error.message)
  }

  @Test
  fun `rejects lowercase exponent notation`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;1e1
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"rate\", значение \"1e1\": Ставка должна быть десятичным числом без экспоненты.", error.message)
  }

  // Дата начала периода

  @Test
  fun `rejects 31 December 2021 as from`() {
    val csv = """
      from;through;rate
      31.12.2021;31.12.2050;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"from\", значение \"31.12.2021\": Год ставки должен быть от 2022 до 2050.", error.message)
  }

  @Test
  fun `accepts 1 January 2022 as from`() {
    val csv = """
      from;through;rate
      01.01.2022;31.12.2050;13.25
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(date("01.01.2022"), model.periods.single().from)
  }

  @Test
  fun `accepts 31 December 2050 as from`() {
    val csv = """
      from;through;rate
      31.12.2050;31.12.2050;13.25
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(date("31.12.2050"), model.periods.single().from)
  }

  @Test
  fun `rejects 1 January 2051 as from`() {
    val csv = """
      from;through;rate
      01.01.2051;31.12.2050;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"from\", значение \"01.01.2051\": Год ставки должен быть от 2022 до 2050.", error.message)
  }

  @Test
  fun `rejects an empty start date`() {
    val csv = """
      from;through;rate
      ;31.12.2026;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"from\": поле не заполнено.", error.message)
  }

  @Test
  fun `identifies whitespace from in the second record`() {
    val csv = """
      from;through;rate
      01.01.2026;10.01.2026;13.25
      "   ";31.12.2026;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2, столбец \"from\": поле не заполнено.", error.message)
  }

  @Test
  fun `rejects an impossible start date`() {
    val csv = """
      from;through;rate
      31.02.2026;31.12.2026;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"from\", значение \"31.02.2026\": некорректная дата. Ожидается формат ДД.ММ.ГГГГ.", error.message)
  }

  @Test
  fun `rejects a date in ISO format`() {
    val csv = """
      from;through;rate
      2026-01-01;31.12.2026;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"from\", значение \"2026-01-01\": некорректная дата. Ожидается формат ДД.ММ.ГГГГ.", error.message)
  }

  // Дата окончания периода

  @Test
  fun `rejects 31 December 2021 as through`() {
    val csv = """
      from;through;rate
      01.01.2022;31.12.2021;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"through\", значение \"31.12.2021\": Год ставки должен быть от 2022 до 2050.", error.message)
  }

  @Test
  fun `accepts 1 January 2022 as through`() {
    val csv = """
      from;through;rate
      01.01.2022;01.01.2022;13.25
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(date("01.01.2022"), model.periods.single().through)
  }

  @Test
  fun `accepts 31 December 2050 as through`() {
    val csv = """
      from;through;rate
      01.01.2022;31.12.2050;13.25
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(date("31.12.2050"), model.periods.single().through)
  }

  @Test
  fun `rejects 1 January 2051 as through`() {
    val csv = """
      from;through;rate
      01.01.2022;01.01.2051;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"through\", значение \"01.01.2051\": Год ставки должен быть от 2022 до 2050.", error.message)
  }

  @Test
  fun `rejects an empty end date`() {
    val csv = """
      from;through;rate
      01.01.2026;;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"through\": поле не заполнено.", error.message)
  }

  @Test
  fun `identifies whitespace through in the second record`() {
    val csv = """
      from;through;rate
      01.01.2026;10.01.2026;13.25
      11.01.2026;"   ";13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2, столбец \"through\": поле не заполнено.", error.message)
  }

  @Test
  fun `rejects an impossible end month`() {
    val csv = """
      from;through;rate
      01.01.2026;31.13.2026;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"through\", значение \"31.13.2026\": некорректная дата. Ожидается формат ДД.ММ.ГГГГ.", error.message)
  }

  // Порядок и непрерывность периодов

  @Test
  fun `builds typed periods in chronological order`() {
    val csv = """
      from;through;rate
      01.01.2026;01.01.2026;0.00
      02.01.2026;31.12.2026; 13,25
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(
      listOf(
        KeyRatePeriod(date("01.01.2026"), date("01.01.2026"), BigDecimal("0.00")),
        KeyRatePeriod(date("02.01.2026"), date("31.12.2026"), BigDecimal("13.25")),
      ),
      model.periods,
    )
  }

  @Test
  fun `rejects a period ending before it starts`() {
    val csv = """
      from;through;rate
      31.12.2026;01.01.2026;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Период 1: конец предшествует началу.", error.message)
  }

  @Test
  fun `rejects periods in reverse order`() {
    val csv = """
      from;through;rate
      02.01.2026;31.12.2026; 13,25
      01.01.2026;01.01.2026;0.00
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Периоды ставок должны идти непрерывно, без пропусков и пересечений.", error.message)
  }

  @Test
  fun `rejects a gap between periods`() {
    val csv = """
      from;through;rate
      01.01.2026;10.01.2026;13.25
      12.01.2026;31.12.2026;14.00
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Периоды ставок должны идти непрерывно, без пропусков и пересечений.", error.message)
  }

  @Test
  fun `rejects periods sharing an endpoint`() {
    val csv = """
      from;through;rate
      01.01.2026;10.01.2026;13.25
      10.01.2026;31.12.2026;14.00
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Периоды ставок должны идти непрерывно, без пропусков и пересечений.", error.message)
  }

  @Test
  fun `rejects partially overlapping periods`() {
    val csv = """
      from;through;rate
      01.01.2026;20.01.2026;13.25
      10.01.2026;31.12.2026;14.00
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Периоды ставок должны идти непрерывно, без пропусков и пересечений.", error.message)
  }

  @Test
  fun `rejects a period contained in another period`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;13.25
      10.01.2026;20.01.2026;14.00
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Периоды ставок должны идти непрерывно, без пропусков и пересечений.", error.message)
  }

  @Test
  fun `rejects duplicate periods`() {
    val csv = """
      from;through;rate
      01.01.2026;31.12.2026;13.25
      01.01.2026;31.12.2026;13.25
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Периоды ставок должны идти непрерывно, без пропусков и пересечений.", error.message)
  }
}
