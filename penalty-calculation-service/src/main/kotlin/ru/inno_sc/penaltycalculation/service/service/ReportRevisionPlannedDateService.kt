package ru.inno_sc.penaltycalculation.service.service

import java.time.Clock
import java.time.LocalDate

class ReportRevisionPlannedDateService(private val workdayService: WorkdayService, private val clock: Clock) {
  fun getReportRevisionPlannedDate(returnedForRevisionDate: LocalDate, caseNumber: Int): LocalDate {
    require(caseNumber > 0) { "Номер кейса должен быть положительным." }
    require(!returnedForRevisionDate.isAfter(LocalDate.now(clock))) {
      "Дата возврата на доработку: дата не может быть в будущем."
    }
    val workdaysCount = when {
      caseNumber <= 2 -> 3
      caseNumber <= 5 -> 2
      else -> 0
    }

    if (workdaysCount != 0) {
      return workdayService.addWorkdays(returnedForRevisionDate, workdaysCount)
    } else {
      var plannedSubmissionDate = returnedForRevisionDate
      while (!workdayService.isWorkday(plannedSubmissionDate)) {
        plannedSubmissionDate = plannedSubmissionDate.plusDays(1)
      }
      return plannedSubmissionDate
    }
  }
}
