package ru.inno_sc.penaltycalculation.api.penalty.response

import java.time.LocalDate

data class PenaltyCalculationResultDto(
  val stageNumber: Int,
  val contractNumber: String,
  val stageAmount: String,
  val calculationDate: String,
  val keyRatePercent: String,
  val reportingDelayPenalty: ReportingDelayPenaltyDto,
  val stageCompletionPenalty: StageCompletionPenaltyDto,
  val totalDaysForCalculation: List<LocalDate>,
  val totalPenalty: String,
)

data class ReportingDelayPenaltyDto(
  val initialSubmissionDaysForCalculation: List<LocalDate>,

  val grantReport: ReportRevisionDto,
  val extraBudgetReport: ReportRevisionDto,
  val technicalReport: ReportRevisionDto,
  val totalOverdueDays: List<LocalDate>,
  val totalDaysToExclude: List<LocalDate>,
  val totalDaysForCalculation: List<LocalDate>,
  val penalty: String,
)

data class ReportRevisionDto(
  val cases: List<ReportRevisionCaseDto>,
  val overdueDays: List<LocalDate>,
  val daysToExclude: List<LocalDate>,
  val daysForCalculation: List<LocalDate>,
)

data class ReportRevisionCaseDto(
  val caseNumber: Int,
  val returnedForRevisionDate: String,
  val plannedSubmissionDate: String,
  val actualSubmissionDate: String,
  val remarksCorrected: Boolean,
  val overdueDays: List<LocalDate>,
  val daysToExclude: List<LocalDate>,
  val daysForCalculation: List<LocalDate>,
)

data class StageCompletionPenaltyDto(
  val overdueDays: List<LocalDate>,
  val penalty: String,
)
