package com.jakub.rpncalculator.helpers.converters

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.MATH_CONTEXT
import java.math.BigDecimal

/**
 * Base unit: liter per second.
 *
 * Main references:
 * - https://en.wikipedia.org/wiki/Volumetric_flow_rate
 */
object FlowRateConverter : Converter {
    override val nameResId: Int = R.string.unit_flow_rate
    override val imageResId: Int = R.drawable.ic_flow_rate_vector
    override val key: String = "FlowRateConverter"

    sealed class Unit(nameResId: Int, symbolResId: Int, factor: BigDecimal, key: String) :
        Converter.Unit(nameResId, symbolResId, factor, key) {
        data object LiterPerSecond : Unit(
            nameResId = R.string.unit_flow_rate_liter_per_second,
            symbolResId = R.string.unit_flow_rate_liter_per_second_symbol,
            factor = BigDecimal.ONE,
            key = "LiterPerSecond"
        )

        data object LiterPerMinute : Unit(
            nameResId = R.string.unit_flow_rate_liter_per_minute,
            symbolResId = R.string.unit_flow_rate_liter_per_minute_symbol,
            factor = BigDecimal.ONE.divide(BigDecimal("60"), MATH_CONTEXT),
            key = "LiterPerMinute"
        )

        data object LiterPerHour : Unit(
            nameResId = R.string.unit_flow_rate_liter_per_hour,
            symbolResId = R.string.unit_flow_rate_liter_per_hour_symbol,
            factor = BigDecimal.ONE.divide(BigDecimal("3600"), MATH_CONTEXT),
            key = "LiterPerHour"
        )

        data object CubicMeterPerHour : Unit(
            nameResId = R.string.unit_flow_rate_cubic_meter_per_hour,
            symbolResId = R.string.unit_flow_rate_cubic_meter_per_hour_symbol,
            factor = BigDecimal("1000").divide(BigDecimal("3600"), MATH_CONTEXT),
            key = "CubicMeterPerHour"
        )

        data object CubicMeterPerSecond : Unit(
            nameResId = R.string.unit_flow_rate_cubic_meter_per_second,
            symbolResId = R.string.unit_flow_rate_cubic_meter_per_second_symbol,
            factor = BigDecimal("1000"),
            key = "CubicMeterPerSecond"
        )

        data object GallonUsPerMinute : Unit(
            nameResId = R.string.unit_flow_rate_gallon_us_per_minute,
            symbolResId = R.string.unit_flow_rate_gallon_us_per_minute_symbol,
            factor = BigDecimal("3.785411784").divide(BigDecimal("60"), MATH_CONTEXT),
            key = "GallonUsPerMinute"
        )

        data object CubicFootPerMinute : Unit(
            nameResId = R.string.unit_flow_rate_cubic_foot_per_minute,
            symbolResId = R.string.unit_flow_rate_cubic_foot_per_minute_symbol,
            factor = BigDecimal("28.316846592").divide(BigDecimal("60"), MATH_CONTEXT),
            key = "CubicFootPerMinute"
        )
    }

    override val units: List<Unit> = listOf(
        Unit.LiterPerSecond,
        Unit.LiterPerMinute,
        Unit.LiterPerHour,
        Unit.CubicMeterPerHour,
        Unit.CubicMeterPerSecond,
        Unit.GallonUsPerMinute,
        Unit.CubicFootPerMinute
    )
    override val defaultTopUnit: Unit = Unit.LiterPerMinute
    override val defaultBottomUnit: Unit = Unit.GallonUsPerMinute
}
