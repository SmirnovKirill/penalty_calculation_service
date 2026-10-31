package ru.inno_sc.penaltycalculation.api.penalty.response

import java.time.LocalDate

data class PenaltyCalculationResultDto(
  val stageNumber: Int,
  val contractNumber: String,
  val stageAmount: String,
  val calculationDate: String,
  val keyRatePercent: String,
  val initialSubmissionPenalty: InitialSubmissionPenaltyDto,
  val revisionsPenalty: AllReportsRevisionPenaltyDto,
  val stageCompletionPenalty: StageCompletionPenaltyDto,
  val totalDaysForCalculation: List<LocalDate>,
  val totalPenalty: String,
)

data class InitialSubmissionPenaltyDto(
  val overdueDays: List<LocalDate>,
  val daysToExcludeManually: List<LocalDate>,
  val daysForCalculation: List<LocalDate>
)

data class AllReportsRevisionPenaltyDto(
  val grantReport: SingleReportRevisionPenaltyDto,
  val extraBudgetReport: SingleReportRevisionPenaltyDto,
  val technicalReport: SingleReportRevisionPenaltyDto,
  val overdueDays: List<LocalDate>,
  val daysToExcludeManually: List<LocalDate>,
  val daysForCalculation: List<LocalDate>
)

data class SingleReportRevisionPenaltyDto(
  val cases: List<ReportRevisionCaseDto>,
  val overdueDays: List<LocalDate>,
  val daysToExcludeManually: List<LocalDate>,
  val daysForCalculation: List<LocalDate>
)

data class ReportRevisionCaseDto(
  val caseNumber: Int,
  val returnedForRevisionDate: String,
  val plannedSubmissionDate: String,
  val actualSubmissionDate: String,
  val remarksCorrected: Boolean,
  val overdueDays: List<LocalDate>,
  val daysToExcludeManually: List<LocalDate>,
  val daysForCalculation: List<LocalDate>,
)

data class StageCompletionPenaltyDto(
  val overdueDays: List<LocalDate>
)
