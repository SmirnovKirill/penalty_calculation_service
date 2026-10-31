package ru.inno_sc.penaltycalculation.service.config

import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle.STRICT
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer
import tools.jackson.databind.module.SimpleModule

@Configuration
@Import(
  ControllerConfig::class,
  ServiceConfig::class
)
class CommonConfig {
  @Bean
  fun clock(): Clock = Clock.systemDefaultZone()

  @Bean
  fun localDateModule(): SimpleModule = SimpleModule()
    .addSerializer(LocalDate::class.java, LocalDateSerializer(DATE_FORMATTER))
    .addDeserializer(LocalDate::class.java, LocalDateDeserializer(DATE_FORMATTER))

  companion object {
    const val DATE_PATTERN = "dd.MM.uuuu"
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN).withResolverStyle(STRICT)
    val MIN_SUPPORTED_DATE: LocalDate = LocalDate.of(2022, 1, 1)
  }
}
