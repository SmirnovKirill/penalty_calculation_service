package ru.inno_sc.penaltycalculation.service.service

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER

class ReportRevisionPlannedDateServiceTest : AbstractTest() {
  @Autowired
  private lateinit var service: ReportRevisionPlannedDateService

  @Test
  fun `case 1 gets three workdays`() {
    assertEquals(date("05.03.2026"), service.getReportRevisionPlannedDate(date("02.03.2026"), 1))
  }

  @Test
  fun `case 2 gets three workdays`() {
    assertEquals(date("05.03.2026"), service.getReportRevisionPlannedDate(date("02.03.2026"), 2))
  }

  @Test
  fun `case 3 gets two workdays`() {
    assertEquals(date("04.03.2026"), service.getReportRevisionPlannedDate(date("02.03.2026"), 3))
  }

  @Test
  fun `case 4 gets two workdays`() {
    assertEquals(date("04.03.2026"), service.getReportRevisionPlannedDate(date("02.03.2026"), 4))
  }

  @Test
  fun `case 5 gets two workdays`() {
    assertEquals(date("04.03.2026"), service.getReportRevisionPlannedDate(date("02.03.2026"), 5))
  }

  @Test
  fun `case 6 stays on the working return day`() {
    assertEquals(date("02.03.2026"), service.getReportRevisionPlannedDate(date("02.03.2026"), 6))
  }

  @Test
  fun `case 6 moves a holiday Monday to the next working day`() {
    assertEquals(date("10.03.2026"), service.getReportRevisionPlannedDate(date("09.03.2026"), 6))
  }

  @Test
  fun `case 6 skips a Sunday followed by a holiday`() {
    assertEquals(date("10.03.2026"), service.getReportRevisionPlannedDate(date("08.03.2026"), 6))
  }

  @Test
  fun `case 7 skips consecutive November holidays`() {
    assertEquals(date("05.11.2025"), service.getReportRevisionPlannedDate(date("03.11.2025"), 7))
  }

  @Test
  fun `case 26 skips the New Year holidays`() {
    assertEquals(date("12.01.2026"), service.getReportRevisionPlannedDate(date("31.12.2025"), 26))
  }

  @Test
  fun `case 6 keeps an exceptional working Saturday`() {
    assertEquals(date("01.11.2025"), service.getReportRevisionPlannedDate(date("01.11.2025"), 6))
  }

  @Test
  fun `rejects return dates before 2022`() {
    val error = assertThrows(IllegalArgumentException::class.java) {
      service.getReportRevisionPlannedDate(returnedForRevisionDate = date("31.12.2021"), caseNumber = 6)
    }

    assertTrue(error.message.orEmpty().contains("01.01.2022"))
  }

  @Test
  fun `allows return on the first supported date even when it is a holiday`() {
    assertDoesNotThrow {
      service.getReportRevisionPlannedDate(returnedForRevisionDate = date("01.01.2022"), caseNumber = 6)
    }
  }

  private fun date(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)
}
