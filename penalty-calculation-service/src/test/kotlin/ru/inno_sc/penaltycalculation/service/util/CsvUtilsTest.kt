package ru.inno_sc.penaltycalculation.service.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.inno_sc.penaltycalculation.service.AbstractTest

class CsvUtilsTest : AbstractTest() {
  @Test
  fun `reads named columns with comments quotes and empty cells after closing resource`() {
    val records = CsvUtils.readResource("csv/sample.csv")

    assertEquals(2, records.size)
    assertTrue(records[0].parser.isClosed)
    assertEquals("first", records[0]["name"])
    assertEquals("semi; \"quote\"", records[0]["description"])
    assertEquals("", records[0]["optional"])
    assertEquals("second", records[1]["name"])
    assertEquals("plain", records[1]["description"])
    assertEquals("value", records[1]["optional"])
  }
}
