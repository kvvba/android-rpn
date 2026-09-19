package com.jakub.rpncalculator.helpers.converters

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.CurrencyRatesStore
import com.jakub.rpncalculator.helpers.RpnEngine
import java.math.BigDecimal

/**
 * Base unit: US dollar.
 *
 * Unlike every other converter, rates aren't fixed at compile time: each [Unit.key] is a
 * currency code looked up in [CurrencyRatesStore] at conversion time, so a manual refresh
 * updates every open converter screen immediately.
 */
object CurrencyConverter : Converter {
    override val nameResId: Int = R.string.unit_currency
    override val imageResId: Int = R.drawable.ic_currency_vector
    override val key: String = "CurrencyConverter"

    class Unit(nameResId: Int, symbolResId: Int, code: String) :
        Converter.Unit(nameResId, symbolResId, BigDecimal.ONE, code) {
        override fun toBase(value: BigDecimal): BigDecimal = RpnEngine.divide(value, CurrencyRatesStore.rateFor(key))
        override fun fromBase(value: BigDecimal): BigDecimal = RpnEngine.multiply(value, CurrencyRatesStore.rateFor(key))
    }

    override val units: List<Unit> = listOf(
        Unit(R.string.currency_usd, R.string.currency_usd_symbol, "USD"),
        Unit(R.string.currency_eur, R.string.currency_eur_symbol, "EUR"),
        Unit(R.string.currency_gbp, R.string.currency_gbp_symbol, "GBP"),
        Unit(R.string.currency_jpy, R.string.currency_jpy_symbol, "JPY"),
        Unit(R.string.currency_chf, R.string.currency_chf_symbol, "CHF"),
        Unit(R.string.currency_cad, R.string.currency_cad_symbol, "CAD"),
        Unit(R.string.currency_aud, R.string.currency_aud_symbol, "AUD"),
        Unit(R.string.currency_cny, R.string.currency_cny_symbol, "CNY"),
        Unit(R.string.currency_pln, R.string.currency_pln_symbol, "PLN"),
        Unit(R.string.currency_huf, R.string.currency_huf_symbol, "HUF"),
        Unit(R.string.currency_sek, R.string.currency_sek_symbol, "SEK"),
        Unit(R.string.currency_nok, R.string.currency_nok_symbol, "NOK"),
        Unit(R.string.currency_dkk, R.string.currency_dkk_symbol, "DKK"),
        Unit(R.string.currency_czk, R.string.currency_czk_symbol, "CZK"),
        Unit(R.string.currency_inr, R.string.currency_inr_symbol, "INR"),
        Unit(R.string.currency_brl, R.string.currency_brl_symbol, "BRL"),
        Unit(R.string.currency_mxn, R.string.currency_mxn_symbol, "MXN"),
        Unit(R.string.currency_zar, R.string.currency_zar_symbol, "ZAR"),
        Unit(R.string.currency_nzd, R.string.currency_nzd_symbol, "NZD"),
        Unit(R.string.currency_sgd, R.string.currency_sgd_symbol, "SGD"),
        Unit(R.string.currency_krw, R.string.currency_krw_symbol, "KRW"),
        Unit(R.string.currency_try, R.string.currency_try_symbol, "TRY"),
        Unit(R.string.currency_hkd, R.string.currency_hkd_symbol, "HKD"),
        Unit(R.string.currency_ils, R.string.currency_ils_symbol, "ILS"),
        Unit(R.string.currency_aed, R.string.currency_aed_symbol, "AED")
    )
    override val defaultTopUnit: Unit = units.first { it.key == "GBP" }
    override val defaultBottomUnit: Unit = units.first { it.key == "EUR" }
}
