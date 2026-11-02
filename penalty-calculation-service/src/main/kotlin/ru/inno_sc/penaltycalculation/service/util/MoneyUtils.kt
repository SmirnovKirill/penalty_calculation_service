package ru.inno_sc.penaltycalculation.service.util

import com.ibm.icu.text.PluralRules
import com.ibm.icu.text.RuleBasedNumberFormat
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

object MoneyUtils {
  private val locale = Locale.forLanguageTag("ru")
  private val plurals = PluralRules.forLocale(locale)

  fun moneyInWords(amount: BigDecimal): String {
    val rubles = amount.toBigInteger()
    val kopecks = amount.remainder(BigDecimal.ONE).movePointRight(2).intValueExact()
    val words = RuleBasedNumberFormat(locale, RuleBasedNumberFormat.SPELLOUT).format(rubles)
    return "($words) ${plural(rubles.mod(BigInteger.valueOf(100)).toInt(), "рубль", "рубля", "рублей")} " +
      "${kopecks.toString().padStart(2, '0')} ${plural(kopecks, "копейка", "копейки", "копеек")}."
  }

  private fun plural(lastDigits: Int, one: String, few: String, many: String): String = when (plurals.select(lastDigits.toDouble())) {
    "one" -> one
    "few" -> few
    else -> many
  }
}
