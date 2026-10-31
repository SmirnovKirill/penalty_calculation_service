package ru.inno_sc.penaltycalculation.service.controller

import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ReportExceptionHandler {
  @ExceptionHandler(IllegalArgumentException::class)
  fun illegalArgument(error: IllegalArgumentException): ProblemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, error.message)
}
