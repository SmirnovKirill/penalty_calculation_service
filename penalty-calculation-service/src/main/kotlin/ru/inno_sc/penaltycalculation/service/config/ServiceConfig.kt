package ru.inno_sc.penaltycalculation.service.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.inno_sc.penaltycalculation.service.service.KeyRateService
import ru.inno_sc.penaltycalculation.service.service.PenaltyCalculationExcelService
import ru.inno_sc.penaltycalculation.service.service.PenaltyCalculationService
import ru.inno_sc.penaltycalculation.service.service.ReportRevisionPlannedDateService
import ru.inno_sc.penaltycalculation.service.service.WorkdayService
import ru.inno_sc.penaltycalculation.service.validation.PenaltyCalculationValidator

@Configuration
@Import(
  KeyRateService::class,
  WorkdayService::class,
  PenaltyCalculationValidator::class,
  ReportRevisionPlannedDateService::class,
  PenaltyCalculationService::class,
  PenaltyCalculationExcelService::class,
)
class ServiceConfig
