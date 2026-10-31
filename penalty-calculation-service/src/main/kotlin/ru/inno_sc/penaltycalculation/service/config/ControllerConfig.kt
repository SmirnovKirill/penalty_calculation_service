package ru.inno_sc.penaltycalculation.service.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.inno_sc.penaltycalculation.service.controller.ReportController
import ru.inno_sc.penaltycalculation.service.controller.ReportExceptionHandler

@Configuration
@Import(ReportExceptionHandler::class, ReportController::class)
class ControllerConfig
