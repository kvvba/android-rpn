package com.jakub.rpncalculator.helpers.converters

import com.jakub.rpncalculator.R
import java.math.BigDecimal

/**
 * Base unit: lux (illuminance).
 *
 * Main references:
 * - https://en.wikipedia.org/wiki/Lux
 * - https://en.wikipedia.org/wiki/Foot-candle
 * - https://en.wikipedia.org/wiki/Phot
 */
object LightingConverter : Converter {
    override val nameResId: Int = R.string.unit_lighting
    override val imageResId: Int = R.drawable.ic_lighting_vector
    override val key: String = "LightingConverter"

    sealed class Unit(nameResId: Int, symbolResId: Int, factor: BigDecimal, key: String) :
        Converter.Unit(nameResId, symbolResId, factor, key) {
        data object Lux : Unit(
            nameResId = R.string.unit_lighting_lux,
            symbolResId = R.string.unit_lighting_lux_symbol,
            factor = BigDecimal.ONE,
            key = "Lux"
        )

        data object Kilolux : Unit(
            nameResId = R.string.unit_lighting_kilolux,
            symbolResId = R.string.unit_lighting_kilolux_symbol,
            factor = BigDecimal("1000"),
            key = "Kilolux"
        )

        data object FootCandle : Unit(
            nameResId = R.string.unit_lighting_foot_candle,
            symbolResId = R.string.unit_lighting_foot_candle_symbol,
            factor = BigDecimal("10.76391"),
            key = "FootCandle"
        )

        data object Phot : Unit(
            nameResId = R.string.unit_lighting_phot,
            symbolResId = R.string.unit_lighting_phot_symbol,
            factor = BigDecimal("10000"),
            key = "Phot"
        )
    }

    override val units: List<Unit> = listOf(
        Unit.Lux,
        Unit.Kilolux,
        Unit.FootCandle,
        Unit.Phot
    )
    override val defaultTopUnit: Unit = Unit.Lux
    override val defaultBottomUnit: Unit = Unit.FootCandle
}
