package ru.inno_sc.penaltycalculation.service.service.catalog.loader

import java.math.BigDecimal
import java.time.LocalDate
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.DATE_FORMATTER
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.MAX_SUPPORTED_YEAR
import ru.inno_sc.penaltycalculation.service.config.CommonConfig.Companion.MIN_SUPPORTED_YEAR
import ru.inno_sc.penaltycalculation.service.domain.catalog.CatalogType
import ru.inno_sc.penaltycalculation.service.domain.catalog.KeyRateCatalogModel
import ru.inno_sc.penaltycalculation.service.domain.catalog.KeyRatePeriod
import ru.inno_sc.penaltycalculation.service.domain.exception.CatalogValidationException

private val MAX_RATE = BigDecimal("1000")
private const val MAX_RATE_SCALE = 2

class KeyRateCatalogModelLoader : CatalogModelLoader<KeyRateCatalogModel>() {
  override val type: CatalogType = CatalogType.KEY_RATES

  override fun load(csv: String): KeyRateCatalogModel = parseCsv(csv, listOf("from", "through", "rate")) { rows ->
    KeyRateCatalogModel(
      rows.map { row ->
        KeyRatePeriod(
          from = field(row, "from", ::getValidatedDate),
          through = field(row, "through", ::getValidatedDate),
          percent = field(row, "rate", ::getValidatedRate),
        )
      },
    )
  }.also(::validate)

  private fun getValidatedDate(value: String): LocalDate = LocalDate.parse(value.trim(), DATE_FORMATTER)

  private fun getValidatedRate(value: String): BigDecimal {
    val text = value.trim().replace(',', '.')
    if (text.contains('e', ignoreCase = true)) {
      throw CatalogValidationException("Ставка должна быть десятичным числом без экспоненты.")
    }
    val rate = text.toBigDecimalOrNull()
      ?: throw CatalogValidationException("Ставка должна быть числом без знака процента.")
    if (rate.signum() < 0 || rate > MAX_RATE) {
      throw CatalogValidationException("Ставка должна быть от 0 до 1000%.")
    }
    if (rate.scale() > MAX_RATE_SCALE) {
      throw CatalogValidationException("Ставка должна содержать не более двух знаков после запятой.")
    }
    return rate
  }

  private fun validate(model: KeyRateCatalogModel) {
    model.periods.forEachIndexed { index, period ->
      listOf("from" to period.from, "through" to period.through).forEach { (field, date) ->
        if (date.year !in MIN_SUPPORTED_YEAR..MAX_SUPPORTED_YEAR) {
          throw CatalogValidationException(
            "Строка ${index + 1}, столбец \"$field\", значение \"${date.format(DATE_FORMATTER)}\": " +
              "Год ставки должен быть от $MIN_SUPPORTED_YEAR до $MAX_SUPPORTED_YEAR.",
          )
        }
      }
      if (period.from > period.through) {
        throw CatalogValidationException("Период ${index + 1}: конец предшествует началу.")
      }
    }
    val continuous = model.periods.zipWithNext().all { (previous, next) ->
      next.from == previous.through.plusDays(1)
    }
    if (!continuous) {
      throw CatalogValidationException("Периоды ставок должны идти непрерывно, без пропусков и пересечений.")
    }
  }
}
