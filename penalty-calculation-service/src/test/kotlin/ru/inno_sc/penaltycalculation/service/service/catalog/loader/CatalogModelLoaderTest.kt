package ru.inno_sc.penaltycalculation.service.service.catalog.loader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogModel
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogType
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException

class CatalogModelLoaderTest {
  @Test
  fun `reads LF CSV with quoted cells empty values blank lines and comments`() {
    val csv = """
      # Комментарии старых справочников тоже поддерживаются.
      first;second;third

      "one";"two";"three"
      # Комментарий между записями.
      ;plain;""
      last;trailing;
    """.trimIndent()

    val model = TestLoader().load(csv)

    assertEquals(
      listOf(
        listOf("one", "two", "three"),
        listOf("", "plain", ""),
        listOf("last", "trailing", ""),
      ),
      model.rows,
    )
  }

  @Test
  fun `reads CRLF CSV with single line values`() {
    val csv = """
      # Комментарии старых справочников тоже поддерживаются.
      first;second;third

      "one";"two";"three"
      # Комментарий между записями.
      ;plain;""
      last;trailing;
    """.trimIndent().replace("\n", "\r\n")

    val model = TestLoader().load(csv)

    assertEquals(
      listOf(
        listOf("one", "two", "three"),
        listOf("", "plain", ""),
        listOf("last", "trailing", ""),
      ),
      model.rows,
    )
  }

  @Test
  fun `rejects an empty CSV file`() {
    val csv = ""
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Файл справочника пуст.", error.message)
  }

  @Test
  fun `rejects a CSV file containing only whitespace`() {
    val csv = " \n\t "
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Файл справочника пуст.", error.message)
  }

  @Test
  fun `rejects a header without data records`() {
    val csv = """
      first;second;third
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Справочник не может быть пустым.", error.message)
  }

  @Test
  fun `rejects comments without a header`() {
    val csv = """
      # Заголовок и данные отсутствуют.
      # В файле только комментарии.
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects quoted column names`() {
    val csv = """
      "first";"second";"third"
      one;two;three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects data without a header`() {
    val csv = """
      one;two;three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects an unknown column name`() {
    val csv = """
      first;wrong;third
      one;two;three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects columns in the wrong order`() {
    val csv = """
      second;first;third
      one;two;three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects a duplicate column name`() {
    val csv = """
      first;second;second
      one;two;three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects an empty column name`() {
    val csv = """
      first;;third
      one;two;three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects a missing column in the header`() {
    val csv = """
      first;second
      one;two
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects an extra column in the header`() {
    val csv = """
      first;second;third;fourth
      one;two;three;four
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects a comma instead of the semicolon delimiter`() {
    val csv = """
      first,second,third
      one,two,three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects a record with a missing field`() {
    val csv = """
      first;second;third
      one;two
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 1: количество столбцов CSV не соответствует заголовку.", error.message)
  }

  @Test
  fun `rejects a second record with an extra field`() {
    val csv = """
      first;second;third
      one;two;three
      one;two;three;four
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2: количество столбцов CSV не соответствует заголовку.", error.message)
  }

  @Test
  fun `rejects an unclosed quote in the header`() {
    val csv = """
      "first;second;third
      one;two;three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Ожидается заголовок CSV: first;second;third.", error.message)
  }

  @Test
  fun `rejects a multiline value in the second record`() {
    val csv = """
      first;second;third
      one;two;three
      one;"line one
      line two";three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Строка 2: значения не могут содержать переносы строк.", error.message)
  }

  @Test
  fun `rejects an unclosed quote in a data record`() {
    val csv = """
      first;second;third
      one;"two;three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Не удалось прочитать CSV. Проверьте разделители и кавычки.", error.message)
  }

  @Test
  fun `rejects characters after a closing quote`() {
    val csv = """
      first;second;third
      one;"two"wrong;three
    """.trimIndent()
    val loader = TestLoader()

    val error = assertThrows(CatalogValidationException::class.java) { loader.load(csv) }

    assertEquals("Не удалось прочитать CSV. Проверьте разделители и кавычки.", error.message)
  }

  private data class CsvTestModel(val rows: List<List<String>>) : CatalogModel()

  private class TestLoader : CatalogModelLoader<CsvTestModel>() {
    override val type: CatalogType = CatalogType.KEY_RATES

    override fun load(csv: String): CsvTestModel = parseCsv(csv, listOf("first", "second", "third")) { rows ->
      CsvTestModel(rows.map { it.toList() })
    }
  }
}
