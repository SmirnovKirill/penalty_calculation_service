package ru.inno_sc.penaltycalculation.service.service.catalog.loader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ru.inno_sc.penaltycalculation.service.AbstractTest.Companion.date
import ru.inno_sc.penaltycalculation.service.domain.catalog.YearInfo
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException

class WorkdayCalendarModelLoaderTest {
  private val loader = WorkdayCalendarModelLoader()

  // Годы и их допустимые значения.

  @Test
  fun `rejects year 2021 below the supported range`() {
    val csv = """
      year;daysOff;workingDays
      2021;01.01;31.12
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\", значение \"2021\": Год календаря должен быть от 2022 до 2050.", error.message)
  }

  @Test
  fun `accepts year 2022 including January 1 and December 31`() {
    val csv = """
      year;daysOff;workingDays
      2022;01.01;31.12
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(
      YearInfo(2022, setOf(date("01.01.2022")), setOf(date("31.12.2022"))),
      model.years.single(),
    )
  }

  @Test
  fun `accepts year 2050 including January 1 and December 31`() {
    val csv = """
      year;daysOff;workingDays
      2050;01.01;31.12
    """.trimIndent()

    val model = loader.load(csv)

    assertEquals(
      YearInfo(2050, setOf(date("01.01.2050")), setOf(date("31.12.2050"))),
      model.years.single(),
    )
  }

  @Test
  fun `rejects year 2051 above the supported range`() {
    val csv = """
      year;daysOff;workingDays
      2051;01.01;31.12
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\", значение \"2051\": Год календаря должен быть от 2022 до 2050.", error.message)
  }

  @Test
  fun `rejects an empty year`() {
    val csv = """
      year;daysOff;workingDays
      ;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\": поле не заполнено.", error.message)
  }

  @Test
  fun `identifies a whitespace required year in the second record`() {
    val csv = """
      year;daysOff;workingDays
      2025;;
      "   ";;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2, столбец \"year\": поле не заполнено.", error.message)
  }

  @Test
  fun `rejects text instead of a year`() {
    val csv = """
      year;daysOff;workingDays
      год;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\", значение \"год\": Год календаря должен быть целым числом.", error.message)
  }

  @Test
  fun `rejects a fractional year`() {
    val csv = """
      year;daysOff;workingDays
      2026.5;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\", значение \"2026.5\": Год календаря должен быть целым числом.", error.message)
  }

  @Test
  fun `rejects a year outside the integer range`() {
    val csv = """
      year;daysOff;workingDays
      2147483648;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\", значение \"2147483648\": Год календаря должен быть целым числом.", error.message)
  }

  @Test
  fun `rejects a two digit year`() {
    val csv = """
      year;daysOff;workingDays
      25;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\", значение \"25\": Год календаря должен быть от 2022 до 2050.", error.message)
  }

  @Test
  fun `rejects year zero`() {
    val csv = """
      year;daysOff;workingDays
      0;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\", значение \"0\": Год календаря должен быть от 2022 до 2050.", error.message)
  }

  @Test
  fun `rejects a negative year`() {
    val csv = """
      year;daysOff;workingDays
      -2026;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\", значение \"-2026\": Год календаря должен быть от 2022 до 2050.", error.message)
  }

  @Test
  fun `rejects a five digit year`() {
    val csv = """
      year;daysOff;workingDays
      10000;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"year\", значение \"10000\": Год календаря должен быть от 2022 до 2050.", error.message)
  }

  // Последовательность годов.

  @Test
  fun `builds typed years with consecutive years and increasing dates`() {
    val model = loader.load(
      """
        year;daysOff;workingDays
        2023;;
        2024;"01.01  29.02 01.03";27.04 02.11
      """.trimIndent(),
    )

    assertEquals(
      listOf(
        YearInfo(2023, emptySet(), emptySet()),
        YearInfo(
          2024,
          setOf(date("01.01.2024"), date("29.02.2024"), date("01.03.2024")),
          setOf(date("27.04.2024"), date("02.11.2024")),
        ),
      ),
      model.years,
    )
  }

  @Test
  fun `rejects duplicate years`() {
    val csv = """
      year;daysOff;workingDays
      2026;;
      2026;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Годы календаря должны идти подряд и не повторяться.", error.message)
  }

  @Test
  fun `rejects a missing year between calendar years`() {
    val csv = """
      year;daysOff;workingDays
      2024;;
      2026;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Годы календаря должны идти подряд и не повторяться.", error.message)
  }

  @Test
  fun `rejects years in reverse order`() {
    val csv = """
      year;daysOff;workingDays
      2026;;
      2025;;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Годы календаря должны идти подряд и не повторяться.", error.message)
  }

  // Даты и формат списков.

  @Test
  fun `rejects February 29 in a non leap year`() {
    val csv = """
      year;daysOff;workingDays
      2025;29.02;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals(
      "Строка 1, столбец \"daysOff\", значение \"29.02\": некорректная дата \"29.02\" для 2025 года. Ожидается формат ДД.ММ.",
      error.message,
    )
  }

  @Test
  fun `rejects a nonexistent day in working days`() {
    val csv = """
      year;daysOff;workingDays
      2026;;31.04
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals(
      "Строка 1, столбец \"workingDays\", значение \"31.04\": некорректная дата \"31.04\" для 2026 года. Ожидается формат ДД.ММ.",
      error.message,
    )
  }

  @Test
  fun `rejects a nonexistent month in days off`() {
    val csv = """
      year;daysOff;workingDays
      2026;01.13;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals(
      "Строка 1, столбец \"daysOff\", значение \"01.13\": некорректная дата \"01.13\" для 2026 года. Ожидается формат ДД.ММ.",
      error.message,
    )
  }

  @Test
  fun `rejects day zero`() {
    val csv = """
      year;daysOff;workingDays
      2026;00.01;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals(
      "Строка 1, столбец \"daysOff\", значение \"00.01\": некорректная дата \"00.01\" для 2026 года. Ожидается формат ДД.ММ.",
      error.message,
    )
  }

  @Test
  fun `rejects dates without two digit day and month`() {
    val csv = """
      year;daysOff;workingDays
      2026;1.1;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1, столбец \"daysOff\", значение \"1.1\": некорректная дата \"1.1\" для 2026 года. Ожидается формат ДД.ММ.", error.message)
  }

  @Test
  fun `rejects a full date instead of day and month`() {
    val csv = """
      year;daysOff;workingDays
      2026;01.01.2026;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals(
      "Строка 1, столбец \"daysOff\", значение \"01.01.2026\": некорректная дата \"01.01.2026\" для 2026 года. Ожидается формат ДД.ММ.",
      error.message,
    )
  }

  @Test
  fun `rejects text instead of a date`() {
    val csv = """
      year;daysOff;workingDays
      2026;праздник;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals(
      "Строка 1, столбец \"daysOff\", значение \"праздник\": некорректная дата \"праздник\" для 2026 года. Ожидается формат ДД.ММ.",
      error.message,
    )
  }

  @Test
  fun `rejects comma separated dates`() {
    val csv = """
      year;daysOff;workingDays
      2026;01.01,02.01;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals(
      "Строка 1, столбец \"daysOff\", значение \"01.01,02.01\": некорректная дата \"01.01,02.01\" для 2026 года. Ожидается формат ДД.ММ.",
      error.message,
    )
  }

  @Test
  fun `identifies an invalid day off in the second record`() {
    val csv = """
      year;daysOff;workingDays
      2025;;
      2026;01.01 31.02;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals(
      "Строка 2, столбец \"daysOff\", значение \"01.01 31.02\": некорректная дата \"31.02\" для 2026 года. Ожидается формат ДД.ММ.",
      error.message,
    )
  }

  // Порядок и пересечения дат.

  @Test
  fun `identifies descending days off in the second record`() {
    val csv = """
      year;daysOff;workingDays
      2025;;
      2026;01.03 28.02;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2, столбец \"daysOff\", значение \"01.03 28.02\": Даты должны идти по возрастанию и не повторяться.", error.message)
  }

  @Test
  fun `identifies duplicate days off in the second record`() {
    val csv = """
      year;daysOff;workingDays
      2025;;
      2026;28.02 28.02;
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2, столбец \"daysOff\", значение \"28.02 28.02\": Даты должны идти по возрастанию и не повторяться.", error.message)
  }

  @Test
  fun `identifies descending working days in the second record`() {
    val csv = """
      year;daysOff;workingDays
      2025;;
      2026;;01.03 28.02
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2, столбец \"workingDays\", значение \"01.03 28.02\": Даты должны идти по возрастанию и не повторяться.", error.message)
  }

  @Test
  fun `identifies duplicate working days in the second record`() {
    val csv = """
      year;daysOff;workingDays
      2025;;
      2026;;28.02 28.02
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2, столбец \"workingDays\", значение \"28.02 28.02\": Даты должны идти по возрастанию и не повторяться.", error.message)
  }

  @Test
  fun `rejects a date listed as both working and day off`() {
    val csv = """
      year;daysOff;workingDays
      2026;03.03;03.03
    """.trimIndent()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Календарь за 2026 год: одна дата указана и выходной, и рабочей.", error.message)
  }
}
