package ru.inno_sc.penaltycalculation.service.service.catalog.loader

import java.io.IOException
import java.io.UncheckedIOException
import java.time.DateTimeException
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogModel
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogType
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException

abstract class CatalogModelLoader<T : CatalogModel> {
  abstract val type: CatalogType

  abstract fun load(csv: String): T

  private val format: CSVFormat = CSVFormat.DEFAULT.builder()
    .setDelimiter(';')
    .setCommentMarker('#')
    .setHeader()
    .setSkipHeaderRecord(true)
    .get()

  protected fun <T> parseCsv(csv: String, headers: List<String>, parseRows: (List<CSVRecord>) -> T): T = try {
    if (csv.isBlank()) throw CatalogValidationException("Файл справочника пуст.")
    val header = csv.lineSequence().firstOrNull { it.isNotEmpty() && !it.startsWith('#') }
    if (header != headers.joinToString(";")) {
      throw CatalogValidationException("Ожидается заголовок CSV: ${headers.joinToString(";")}.")
    }
    val rows = format.parse(csv.reader()).use { it.records }
    if (rows.isEmpty()) throw CatalogValidationException("Справочник не может быть пустым.")
    rows.firstOrNull { it.size() != headers.size }?.let { row ->
      throw CatalogValidationException("Строка ${row.recordNumber}: количество столбцов CSV не соответствует заголовку.")
    }
    rows.firstOrNull { row -> row.any { '\n' in it || '\r' in it } }?.let { row ->
      throw CatalogValidationException("Строка ${row.recordNumber}: значения не могут содержать переносы строк.")
    }
    parseRows(rows)
  } catch (error: IOException) {
    throw CatalogValidationException("Не удалось прочитать CSV. Проверьте разделители и кавычки.", error)
  } catch (error: UncheckedIOException) {
    throw CatalogValidationException("Не удалось прочитать CSV. Проверьте разделители и кавычки.", error)
  } catch (error: IllegalArgumentException) {
    throw CatalogValidationException("Некорректное значение в справочнике. Проверьте заголовок и данные CSV.", error)
  }

  protected fun <V> field(row: CSVRecord, name: String, parse: (String) -> V): V {
    val value = row[name]
    return try {
      parse(value)
    } catch (error: RuntimeException) {
      val reason = when (error) {
        is DateTimeException -> "некорректная дата. Ожидается формат ДД.ММ.ГГГГ."
        is NumberFormatException -> "некорректное число."
        is CatalogValidationException -> error.message
        else -> throw error
      }
      val details = if (value.isBlank()) ": поле не заполнено." else ", значение \"$value\": $reason"
      throw CatalogValidationException("Строка ${row.recordNumber}, столбец \"$name\"$details", error)
    }
  }
}
