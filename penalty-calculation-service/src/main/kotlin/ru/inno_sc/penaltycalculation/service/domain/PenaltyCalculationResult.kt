package ru.inno_sc.penaltycalculation.service.domain

import java.math.BigDecimal
import java.time.LocalDate

data class PenaltyCalculationResult(
  val stageNumber: Int,
  val contractNumber: String,
  val stageAmount: BigDecimal,
  val calculationDate: LocalDate,
  val keyRatePercent: BigDecimal,
  val reportingDelayPenalty: ReportingDelayPenalty,
  val stageCompletionPenalty: StageCompletionPenalty,
  val totalDaysForCalculation: List<LocalDate>,
  val totalPenalty: BigDecimal,
)

data class ReportingDelayPenalty(
  val initialSubmissionDaysForCalculation: List<LocalDate>,

  val grantReport: ReportRevision,
  val extraBudgetReport: ReportRevision,
  val technicalReport: ReportRevision,

  val totalOverdueDays: List<LocalDate>,
  val totalDaysToExclude: List<LocalDate>,
  val totalDaysForCalculation: List<LocalDate>,
  val penalty: BigDecimal,
)

data class ReportRevision(
  val cases: List<ReportRevisionCase>,
  val overdueDays: List<LocalDate>,
  val daysToExclude: List<LocalDate>,
  val daysForCalculation: List<LocalDate>
)

data class ReportRevisionCase(
  val caseNumber: Int,
  val returnedForRevisionDate: LocalDate,
  val plannedSubmissionDate: LocalDate,
  val actualSubmissionDate: LocalDate,
  val remarksCorrected: Boolean,
  val overdueDays: List<LocalDate>,
  val daysToExclude: List<LocalDate>,
  val daysForCalculation: List<LocalDate>
)

data class StageCompletionPenalty(
  val overdueDays: List<LocalDate>,
  val penalty: BigDecimal,
)
