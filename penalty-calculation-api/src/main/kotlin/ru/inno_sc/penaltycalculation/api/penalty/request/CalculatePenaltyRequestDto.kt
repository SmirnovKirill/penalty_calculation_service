package ru.inno_sc.penaltycalculation.api.penalty.request

import java.math.BigDecimal
import java.time.LocalDate

data class CalculatePenaltyRequestDto(
  val formatVersion: Int,
  val stageNumber: Int,
  val contractNumber: String,
  val stageAmount: BigDecimal,
  val calculationDate: LocalDate,
  val reportPlannedSubmissionDate: LocalDate,
  val reportActualSubmissionDate: LocalDate,
  val grantReport: GrantReportDto,
  val extraBudgetReport: ExtraBudgetReportDto,
  val technicalReport: TechnicalReportDto
)

data class GrantReportDto(
  val cases: List<ReportCaseDto>,
  val reportApprovalDate: LocalDate,
  val daysToExclude: DaysToExcludeDto
)

data class ReportCaseDto(
  val returnedForRevisionDate: LocalDate,
  val actualSubmissionDate: LocalDate,
  val remarksCorrected: Boolean = true,
)

data class ExtraBudgetReportDto(
  val cases: List<ReportCaseDto>,
  val reportApprovalDate: LocalDate,
  val daysToExclude: DaysToExcludeDto
)

data class TechnicalReportDto(
  val cases: List<ReportCaseDto>,
  val reportApprovalDate: LocalDate,
  val plannedCompletionDate: LocalDate,
  val actualCompletionDate: LocalDate,
  val daysToExclude: DaysToExcludeDto
)

data class DaysToExcludeDto(
  val days: List<LocalDate> = emptyList(),
  val dayRanges: List<DayRangeDto> = emptyList(),
)

data class DayRangeDto(
  val startDate: LocalDate,
  val endDate: LocalDate,
)
