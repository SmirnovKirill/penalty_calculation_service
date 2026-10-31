package ru.inno_sc.penaltycalculation.service.util

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.springframework.core.io.ClassPathResource

internal object CsvUtils {
  private val format = CSVFormat.DEFAULT.builder()
    .setDelimiter(';')
    .setCommentMarker('#')
    .setHeader()
    .setSkipHeaderRecord(true)
    .get()

  fun readResource(path: String): List<CSVRecord> = ClassPathResource(path).inputStream.bufferedReader().use { reader ->
    format.parse(reader).use { it.records }
  }
}
