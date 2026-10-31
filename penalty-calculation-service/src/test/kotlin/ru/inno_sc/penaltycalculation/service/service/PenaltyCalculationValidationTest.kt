package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.api.penalty.request.DaysToExcludeManuallyDto
import ru.inno_sc.penaltycalculation.api.penalty.request.ExtraBudgetReportDto
import ru.inno_sc.penaltycalculation.api.penalty.request.GrantReportDto
import ru.inno_sc.penaltycalculation.api.penalty.request.ReportCaseDto
import ru.inno_sc.penaltycalculation.api.penalty.request.TechnicalReportDto
import ru.inno_sc.penaltycalculation.service.AbstractTest
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER

class PenaltyCalculationValidationTest : AbstractTest() {
  @Autowired
  private lateinit var service: PenaltyCalculationService

  @Test
  fun `allows report submission before its planned date`() {
    val request = request().copy(
      reportPlannedSubmissionDate = date("11.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
    )

    val result = service.calculate(request)

    assertEquals(emptyList<LocalDate>(), result.initialSubmissionPenalty.daysForCalculation)
  }

  @Test
  fun `allows work completion before its planned date`() {
    val request = request()
    val technical = request.technicalReport.copy(
      plannedCompletionDate = date("11.11.2025"),
      actualCompletionDate = date("10.11.2025"),
    )

    val result = service.calculate(request.copy(technicalReport = technical))

    assertEquals(emptyList<LocalDate>(), result.stageCompletionPenalty.overdueDays)
  }

  @Test
  fun `allows report submission revision and approval on the same day`() {
    val request = request().copy(
      reportPlannedSubmissionDate = date("10.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(
          ReportCaseDto(
            returnedForRevisionDate = date("10.11.2025"),
            actualSubmissionDate = date("10.11.2025"),
          )
        ),
        reportApprovalDate = date("10.11.2025"),
      ),
    )

    assertDoesNotThrow { service.calculate(request) }
  }

  @Test
  fun `next revision cannot start before the previous submission`() {
    val request = request()
    val technical = request.technicalReport.copy(
      cases = listOf(
        ReportCaseDto(
          returnedForRevisionDate = date("10.11.2025"),
          actualSubmissionDate = date("12.11.2025"),
          remarksCorrected = false,
        ),
        ReportCaseDto(
          returnedForRevisionDate = date("11.11.2025"),
          actualSubmissionDate = date("13.11.2025"),
        ),
      ),
      reportApprovalDate = date("13.11.2025"),
    )

    assertThrows(IllegalArgumentException::class.java) {
      service.calculate(request.copy(technicalReport = technical))
    }
  }

  @Test
  fun `report approval cannot precede the last revision submission`() {
    val request = request()
    val technical = request.technicalReport.copy(
      cases = listOf(
        ReportCaseDto(
          returnedForRevisionDate = date("10.11.2025"),
          actualSubmissionDate = date("11.11.2025"),
        ),
        ReportCaseDto(
          returnedForRevisionDate = date("12.11.2025"),
          actualSubmissionDate = date("14.11.2025"),
        ),
      ),
      reportApprovalDate = date("13.11.2025"),
    )

    assertThrows(IllegalArgumentException::class.java) {
      service.calculate(request.copy(technicalReport = technical))
    }
  }

  @Test
  fun `last revision must correct the remarks`() {
    val request = request()
    val technical = request.technicalReport.copy(
      cases = listOf(
        ReportCaseDto(
          returnedForRevisionDate = date("10.11.2025"),
          actualSubmissionDate = date("11.11.2025"),
          remarksCorrected = false,
        )
      ),
      reportApprovalDate = date("12.11.2025"),
    )

    assertThrows(IllegalArgumentException::class.java) {
      service.calculate(request.copy(technicalReport = technical))
    }
  }

  @Test
  fun `allows an unresolved revision followed by a correction on the same day`() {
    val request = request()
    val technical = request.technicalReport.copy(
      cases = listOf(
        ReportCaseDto(
          returnedForRevisionDate = date("10.11.2025"),
          actualSubmissionDate = date("11.11.2025"),
          remarksCorrected = false,
        ),
        ReportCaseDto(
          returnedForRevisionDate = date("11.11.2025"),
          actualSubmissionDate = date("11.11.2025"),
          remarksCorrected = true,
        ),
      ),
      reportApprovalDate = date("12.11.2025"),
    )

    assertDoesNotThrow { service.calculate(request.copy(technicalReport = technical)) }
  }

  @Test
  fun `requested calculation date determines the key rate and result date`() {
    val historical = service.calculate(request().copy(calculationDate = date("10.11.2025")))
    val current = service.calculate(request().copy(calculationDate = date("28.10.2026")))

    assertEquals(date("10.11.2025"), historical.calculationDate)
    assertEquals(BigDecimal("16.50"), historical.keyRatePercent)
    assertEquals(date("28.10.2026"), current.calculationDate)
    assertEquals(BigDecimal("14.00"), current.keyRatePercent)
  }

  @Test
  fun `events after a historical calculation date are rejected even when already in the past`() {
    val request = request().copy(
      calculationDate = date("10.11.2025"),
      grantReport = GrantReportDto(
        cases = listOf(
          ReportCaseDto(
            returnedForRevisionDate = date("10.11.2025"),
            actualSubmissionDate = date("10.11.2025"),
          )
        ),
        reportApprovalDate = date("11.11.2025"),
      ),
    )

    assertThrows(IllegalArgumentException::class.java) {
      service.calculate(request)
    }
  }

  @Test
  fun `work completion is independent of approval before or after it`() {
    val request = request()
    val approvedBeforeCompletion = service.calculate(
      request.copy(
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("10.11.2025"),
          actualCompletionDate = date("12.11.2025"),
          reportApprovalDate = date("11.11.2025"),
        )
      )
    )
    val approvedAfterCompletion = service.calculate(
      request.copy(
        technicalReport = request.technicalReport.copy(
          plannedCompletionDate = date("10.11.2025"),
          actualCompletionDate = date("12.11.2025"),
          reportApprovalDate = date("13.11.2025"),
        )
      )
    )

    assertEquals(approvedBeforeCompletion.stageCompletionPenalty, approvedAfterCompletion.stageCompletionPenalty)
  }

  private fun request(): CalculatePenaltyRequestDto {
    val case = ReportCaseDto(
      returnedForRevisionDate = date("10.11.2025"),
      actualSubmissionDate = date("10.11.2025"),
    )
    return CalculatePenaltyRequestDto(
      stageNumber = 1,
      calculationDate = date("28.10.2026"),
      contractNumber = "Г-0001",
      stageAmount = BigDecimal("100000"),
      reportPlannedSubmissionDate = date("10.11.2025"),
      reportActualSubmissionDate = date("10.11.2025"),
      grantReport = GrantReportDto(cases = listOf(case), reportApprovalDate = date("10.11.2025")),
      extraBudgetReport = ExtraBudgetReportDto(cases = listOf(case), reportApprovalDate = date("10.11.2025")),
      technicalReport = TechnicalReportDto(
        cases = listOf(case),
        reportApprovalDate = date("10.11.2025"),
        plannedCompletionDate = date("10.11.2025"),
        actualCompletionDate = date("10.11.2025"),
      ),
      daysToExcludeManually = DaysToExcludeManuallyDto(),
    )
  }

  private fun date(value: String): LocalDate = LocalDate.parse(value, DATE_FORMATTER)
}
