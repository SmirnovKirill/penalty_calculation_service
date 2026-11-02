package ru.inno_sc.penaltycalculation.service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.AbstractTest.Companion.date

class ReportRevisionPlannedDateServiceTest : AbstractTest() {
  @Autowired
  private lateinit var service: ReportRevisionPlannedDateService

  @ParameterizedTest(name = "Кейс {0}: срок {1}")
  @CsvSource(
    "1, 05.03.2026",
    "2, 05.03.2026",
    "3, 04.03.2026",
    "4, 04.03.2026",
    "5, 04.03.2026",
    "6, 02.03.2026",
  )
  fun `calculates revision deadlines by case number`(caseNumber: Int, expected: String) {
    assertEquals(date(expected), service.getReportRevisionPlannedDate(date("02.03.2026"), caseNumber))
  }

  @Test
  fun `case 6 moves a holiday Monday to the next working day`() {
    assertEquals(date("10.03.2026"), service.getReportRevisionPlannedDate(date("09.03.2026"), 6))
  }

  @ParameterizedTest(name = "Недопустимая дата {0}")
  @CsvSource(
    "31.12.2021, Дата возврата на доработку: дата не может быть раньше 01.01.2022.",
    "31.12.2050, Дата возврата на доработку: дата не может быть в будущем.",
    "01.01.2051, Дата возврата на доработку: дата не может быть позже 31.12.2050.",
  )
  fun `rejects invalid return dates`(value: String, expectedMessage: String) {
    val error = assertThrows(IllegalArgumentException::class.java) {
      service.getReportRevisionPlannedDate(returnedForRevisionDate = date(value), caseNumber = 6)
    }

    assertEquals(expectedMessage, error.message)
  }
}
