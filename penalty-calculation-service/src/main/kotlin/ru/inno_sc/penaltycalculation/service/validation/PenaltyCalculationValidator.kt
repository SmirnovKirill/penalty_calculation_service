package ru.inno_sc.penaltycalculation.service.validation

import java.time.LocalDate
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.api.penalty.request.DaysToExcludeDto
import ru.inno_sc.penaltycalculation.api.penalty.request.ReportCaseDto
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.MIN_SUPPORTED_DATE

class PenaltyCalculationValidator {
  fun validate(request: CalculatePenaltyRequestDto, today: LocalDate) {
    validateDate(request.calculationDate, "Дата расчёта", today)
    require(request.contractNumber.matches(Regex("Г-00(0[1-9]|[1-7][0-9]|80)"))) {
      "Укажите номер договора от Г-0001 до Г-0080."
    }
    require(request.stageAmount.signum() >= 0) { "Сумма этапа не может быть отрицательной." }
    require(request.stageNumber in 1..MAX_STAGE_NUMBER) { "Номер этапа должен быть от 1 до 10." }
    validateDate(request.reportPlannedSubmissionDate, "Плановая дата подачи отчётности", request.calculationDate)
    validateDate(request.reportActualSubmissionDate, "Фактическая дата подачи отчётности", request.calculationDate)

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
    validateDate(technical.plannedCompletionDate, "Плановая дата завершения работ", request.calculationDate)
    validateDate(technical.actualCompletionDate, "Фактическая дата завершения работ", request.calculationDate)
    validateDaysToExclude(
      "Грант",
      request.grantReport.daysToExclude,
      request.reportPlannedSubmissionDate,
      request.calculationDate,
    )
    validateDaysToExclude(
      "Внебюджет",
      request.extraBudgetReport.daysToExclude,
      request.reportPlannedSubmissionDate,
      request.calculationDate,
    )
    validateDaysToExclude(
      "Тех",
      request.technicalReport.daysToExclude,
      request.reportPlannedSubmissionDate,
      request.calculationDate,
    )
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
      validateDate(case.returnedForRevisionDate, "$label: дата возврата на доработку", calculationDate)
      validateDate(case.actualSubmissionDate, "$label: дата подачи доработанного отчёта", calculationDate)

      require(!case.returnedForRevisionDate.isBefore(previous)) {
        "$label: возврат на доработку не может предшествовать предыдущей подаче."
      }
      require(!case.actualSubmissionDate.isBefore(case.returnedForRevisionDate)) {
        "$label: подача доработанного отчета не может предшествовать возврату на доработку."
      }
      previous = case.actualSubmissionDate
    }
    validateDate(reportApprovalDate, "$name: дата согласования отчёта", calculationDate)
    require(!reportApprovalDate.isBefore(previous)) {
      "$name: согласование не может предшествовать последней подаче отчёта."
    }
    require(cases.last().remarksCorrected) {
      "$name: замечания последнего кейса не исправлены. Добавьте повторную доработку."
    }
  }

  private fun validateDaysToExclude(
    reportName: String,
    exclusions: DaysToExcludeDto,
    reportPlannedSubmissionDate: LocalDate,
    calculationDate: LocalDate,
  ) {
    exclusions.days.forEachIndexed { index, date ->
      val label = "$reportName: исключаемая дата ${index + 1}"
      validateDate(date, label, calculationDate)
      require(!date.isBefore(reportPlannedSubmissionDate)) {
        "$label: дата не может предшествовать плановой дате подачи отчётности."
      }
    }
    exclusions.dayRanges.forEachIndexed { index, range ->
      val label = "$reportName: исключаемый период ${index + 1}"
      validateDate(range.startDate, "$label: начало", calculationDate)
      validateDate(range.endDate, "$label: конец", calculationDate)
      require(!range.startDate.isAfter(range.endDate)) { "$label: начало периода не может быть позже окончания." }
      require(!range.startDate.isBefore(reportPlannedSubmissionDate)) {
        "$label: начало периода не может предшествовать плановой дате подачи отчётности."
      }
    }
  }

  private fun validateDate(date: LocalDate, label: String, calculationDate: LocalDate) {
    require(!date.isBefore(MIN_SUPPORTED_DATE)) { "$label: дата не может быть раньше 01.01.2022." }
    require(!date.isAfter(calculationDate)) { "$label: дата не может быть в будущем." }
  }

  private companion object {
    const val MAX_STAGE_NUMBER = 10
  }
}
