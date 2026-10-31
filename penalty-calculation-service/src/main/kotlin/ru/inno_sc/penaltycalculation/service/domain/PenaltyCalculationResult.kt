package ru.inno_sc.penaltycalculation.service.domain

import java.math.BigDecimal
import java.time.LocalDate

data class PenaltyCalculationResult(
  val stageNumber: Int,
  val contractNumber: String,
  val stageAmount: BigDecimal,
  val calculationDate: LocalDate,
  val keyRatePercent: BigDecimal,
  val initialSubmissionPenalty: InitialSubmissionPenalty,
  val revisionsPenalty: AllReportsRevisionPenalty,
  val stageCompletionPenalty: StageCompletionPenalty,
  val totalDaysForCalculation: List<LocalDate>,
  val totalPenalty: BigDecimal,
)

data class InitialSubmissionPenalty(
  val overdueDays: List<LocalDate>,
  val daysToExcludeManually: List<LocalDate> = emptyList(),
  val daysForCalculation: List<LocalDate>,
  val penalty: BigDecimal, // Только для тестирования и нигде не используется, чтобы избежать многократного округления
)

data class AllReportsRevisionPenalty(
  val grantReport: SingleReportRevisionPenalty,
  val extraBudgetReport: SingleReportRevisionPenalty,
  val technicalReport: SingleReportRevisionPenalty,
  val totalOverdueDays: List<LocalDate>,
  val totalDaysToExcludeManually: List<LocalDate>,
  val totalDaysForCalculation: List<LocalDate>,
  val penalty: BigDecimal, // Только для тестирования и нигде не используется, чтобы избежать многократного округления
)

data class SingleReportRevisionPenalty(
  val cases: List<ReportRevisionCase>,
  val overdueDays: List<LocalDate>,
  val daysToExcludeManually: List<LocalDate> = emptyList(),
  val daysForCalculation: List<LocalDate>
)

data class ReportRevisionCase(
  val caseNumber: Int,
  val returnedForRevisionDate: LocalDate,
  val plannedSubmissionDate: LocalDate,
  val actualSubmissionDate: LocalDate,
  val remarksCorrected: Boolean,
  val overdueDays: List<LocalDate>,
  val daysToExcludeManually: List<LocalDate> = emptyList(),
  val daysForCalculation: List<LocalDate>
)

data class StageCompletionPenalty(
  val overdueDays: List<LocalDate>,
  val penalty: BigDecimal, // Только для тестирования и нигде не используется, чтобы избежать многократного округления
)
