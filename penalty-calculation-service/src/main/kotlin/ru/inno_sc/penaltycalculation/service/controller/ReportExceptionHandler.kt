package ru.inno_sc.penaltycalculation.service.controller

import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogException
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException

@RestControllerAdvice
class ReportExceptionHandler {
  @ExceptionHandler(IllegalArgumentException::class)
  fun illegalArgument(error: IllegalArgumentException): ProblemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, error.message)

  @ExceptionHandler(CatalogException::class)
  fun catalogError(error: CatalogException): ProblemDetail = ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, error.message)

  @ExceptionHandler(CatalogValidationException::class)
  fun catalogValidationError(error: CatalogValidationException): ProblemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, error.message)
}
