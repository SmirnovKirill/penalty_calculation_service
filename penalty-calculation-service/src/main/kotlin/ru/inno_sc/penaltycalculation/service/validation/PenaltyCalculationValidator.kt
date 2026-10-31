package ru.inno_sc.penaltycalculation.service.validation

import java.time.LocalDate
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.api.penalty.request.DaysToExcludeManuallyDto
import ru.inno_sc.penaltycalculation.api.penalty.request.ReportCaseDto

class PenaltyCalculationValidator {
  fun validate(request: CalculatePenaltyRequestDto, today: LocalDate) {
    validateDateNotFuture(request.calculationDate, "Дата расчёта", today)
    require(request.contractNumber.matches(Regex("Г-00(0[1-9]|[1-7][0-9]|80)"))) {
      "Укажите номер договора от Г-0001 до Г-0080."
    }
    require(request.stageAmount.signum() >= 0) { "Сумма этапа не может быть отрицательной." }
    require(request.stageNumber in 1..MAX_STAGE_NUMBER) { "Номер этапа должен быть от 1 до 10." }
    validateDateNotFuture(request.reportPlannedSubmissionDate, "Плановая дата подачи отчётности", request.calculationDate)
    validateDateNotFuture(request.reportActualSubmissionDate, "Фактическая дата подачи отчётности", request.calculationDate)

    val technical = request.technicalReport
    validateReport(
      "Грант",
      request.grantReport.cases,
      request.grantReport.reportApprovalDate,
      request.reportActualSubmissionDate,
      request.calculationDate,
    )
    validateReport(
      "Внебюджет",
      request.extraBudgetReport.cases,
      request.extraBudgetReport.reportApprovalDate,
      request.reportActualSubmissionDate,
      request.calculationDate,
    )

    validateReport("Тех", technical.cases, technical.reportApprovalDate, request.reportActualSubmissionDate, request.calculationDate)
    validateDateNotFuture(technical.plannedCompletionDate, "Плановая дата завершения работ", request.calculationDate)
    validateDateNotFuture(technical.actualCompletionDate, "Фактическая дата завершения работ", request.calculationDate)
    validateManualExclusions(request.daysToExcludeManually, request.calculationDate)
  }

  private fun validateReport(
    name: String,
    cases: List<ReportCaseDto>,
    reportApprovalDate: LocalDate,
    submitted: LocalDate,
    calculationDate: LocalDate,
  ) {
    require(cases.isNotEmpty()) { "$name: добавьте хотя бы один кейс." }
    var previous = submitted
    cases.forEachIndexed { index, case ->
      val label = "$name, строка ${index + 1}"
      validateDateNotFuture(case.returnedForRevisionDate, "$label: дата возврата на доработку", calculationDate)
      validateDateNotFuture(case.actualSubmissionDate, "$label: дата подачи доработанного отчёта", calculationDate)

      require(!case.returnedForRevisionDate.isBefore(previous)) {
        "$label: возврат на доработку не может предшествовать предыдущей подаче."
      }
      require(!case.actualSubmissionDate.isBefore(case.returnedForRevisionDate)) {
        "$label: подача доработанного отчета не может предшествовать возврату на доработку."
      }
      previous = case.actualSubmissionDate
    }
    validateDateNotFuture(reportApprovalDate, "$name: дата согласования отчёта", calculationDate)
    require(!reportApprovalDate.isBefore(previous)) {
      "$name: согласование не может предшествовать последней подаче отчёта."
    }
    require(cases.last().remarksCorrected) {
      "$name: замечания последнего кейса не исправлены. Добавьте повторную доработку."
    }
  }

  private fun validateManualExclusions(exclusions: DaysToExcludeManuallyDto, calculationDate: LocalDate) {
    exclusions.days.forEachIndexed { index, date ->
      validateDateNotFuture(date, "Исключаемая дата ${index + 1}", calculationDate)
    }
    exclusions.dayRanges.forEachIndexed { index, range ->
      val label = "Исключаемый период ${index + 1}"
      require(!range.startDate.isAfter(range.endDate)) { "$label: начало периода не может быть позже окончания." }
      validateDateNotFuture(range.startDate, "$label: начало", calculationDate)
      validateDateNotFuture(range.endDate, "$label: конец", calculationDate)
    }
  }

  private fun validateDateNotFuture(date: LocalDate, label: String, calculationDate: LocalDate) {
    require(!date.isAfter(calculationDate)) { "$label: дата не может быть в будущем." }
  }

  private companion object {
    const val MAX_STAGE_NUMBER = 10
  }
}
