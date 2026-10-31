package ru.inno_sc.penaltycalculation.service.service

import java.math.BigDecimal
import java.time.LocalDate
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.util.CsvUtils

class KeyRateService {
  private val rates = buildMap {
    for (row in CsvUtils.readResource("catalog/key-rates.csv")) {
      val rate = row["percent"].toBigDecimal()
      LocalDate.parse(row["from"], DATE_FORMATTER).datesUntil(LocalDate.parse(row["through"], DATE_FORMATTER).plusDays(1))
        .forEach { put(it, rate) }
    }
  }

  fun rateOn(date: LocalDate): BigDecimal = requireNotNull(rates[date]) {
    "Ключевая ставка на ${date.format(DATE_FORMATTER)} не загружена в справочник."
  }
}
