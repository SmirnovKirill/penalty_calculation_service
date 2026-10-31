package ru.inno_sc.penaltycalculation.service.controller

import java.nio.charset.StandardCharsets
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.inno_sc.penaltycalculation.api.Paths
import ru.inno_sc.penaltycalculation.api.penalty.request.CalculatePenaltyRequestDto
import ru.inno_sc.penaltycalculation.api.penalty.response.PenaltyCalculationResultDto
import ru.inno_sc.penaltycalculation.api.reportrevision.ReportRevisionPlannedDateResponseDto
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_PATTERN
import ru.inno_sc.penaltycalculation.service.converter.toDto
import ru.inno_sc.penaltycalculation.service.service.PenaltyCalculationExcelService
import ru.inno_sc.penaltycalculation.service.service.PenaltyCalculationService
import ru.inno_sc.penaltycalculation.service.service.ReportRevisionPlannedDateService

@RestController
class ReportController(
  private val reportRevisionPlannedDateService: ReportRevisionPlannedDateService,
  private val penaltyCalculationService: PenaltyCalculationService,
  private val penaltyCalculationExcelService: PenaltyCalculationExcelService,
) {
  @PostMapping(Paths.CALCULATE_PENALTY)
  fun calculatePenalty(@RequestBody request: CalculatePenaltyRequestDto): PenaltyCalculationResultDto = penaltyCalculationService.calculate(
    request
  ).toDto()

  @PostMapping(Paths.EXPORT_PENALTY)
  fun exportPenalty(@RequestBody request: CalculatePenaltyRequestDto): ResponseEntity<ByteArray> {
    val calculation = penaltyCalculationService.calculate(request).toDto()
    val xlsxBytes = penaltyCalculationExcelService.generate(request, calculation)
    val fileName = "${request.contractNumber}_№${request.stageNumber}_${request.calculationDate.format(DATE_FORMATTER)}.xlsx"
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
      .body(xlsxBytes)
  }

  @GetMapping(Paths.REPORT_REVISION_PLANNED_DATE)
  fun reportRevisionPlannedDate(
    @RequestParam("caseNumber") caseNumber: Int,
    @RequestParam("returnedForRevisionDate") @DateTimeFormat(pattern = DATE_PATTERN) returnedForRevisionDate: LocalDate,
  ): ReportRevisionPlannedDateResponseDto = ReportRevisionPlannedDateResponseDto(
    reportRevisionPlannedDateService.getReportRevisionPlannedDate(returnedForRevisionDate, caseNumber)
  )
}
