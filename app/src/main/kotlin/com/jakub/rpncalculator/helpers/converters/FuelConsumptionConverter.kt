package com.jakub.rpncalculator.helpers.converters

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.MATH_CONTEXT
import java.math.BigDecimal

/**
 * Base unit: liters per 100 kilometers.
 *
 * Volume-per-distance units (L/100km) and distance-per-volume units (km/L, mpg) are
 * reciprocally related, not linearly related, so those units override toBase/fromBase
 * instead of relying on a plain multiplicative factor.
 *
 * Main references:
 * - https://en.wikipedia.org/wiki/Fuel_economy_in_automobiles
 */
object FuelConsumptionConverter : Converter {
    override val nameResId: Int = R.string.unit_fuel_consumption
    override val imageResId: Int = R.drawable.ic_fuel_consumption_vector
    override val key: String = "FuelConsumptionConverter"

    private val HUNDRED = BigDecimal("100")

    // 100 * liters-per-US-gallon / km-per-mile
    private val US_MPG_CONSTANT = BigDecimal("235.214583")

    // 100 * liters-per-imperial-gallon / km-per-mile
    private val IMPERIAL_MPG_CONSTANT = BigDecimal("282.480936")

    sealed class Unit(nameResId: Int, symbolResId: Int, factor: BigDecimal, key: String) :
        Converter.Unit(nameResId, symbolResId, factor, key) {
        data object LitersPer100Km : Unit(
            nameResId = R.string.unit_fuel_consumption_liters_per_100km,
            symbolResId = R.string.unit_fuel_consumption_liters_per_100km_symbol,
            factor = BigDecimal.ONE,
            key = "LitersPer100Km"
        )

        data object KilometersPerLiter : Unit(
            nameResId = R.string.unit_fuel_consumption_kilometers_per_liter,
            symbolResId = R.string.unit_fuel_consumption_kilometers_per_liter_symbol,
            factor = BigDecimal.ONE,
            key = "KilometersPerLiter"
        ) {
            override fun toBase(value: BigDecimal): BigDecimal = HUNDRED.divide(value, MATH_CONTEXT)
            override fun fromBase(value: BigDecimal): BigDecimal = HUNDRED.divide(value, MATH_CONTEXT)
        }

        data object MilesPerGallonUs : Unit(
            nameResId = R.string.unit_fuel_consumption_mpg_us,
            symbolResId = R.string.unit_fuel_consumption_mpg_us_symbol,
            factor = BigDecimal.ONE,
            key = "MilesPerGallonUs"
        ) {
            override fun toBase(value: BigDecimal): BigDecimal = US_MPG_CONSTANT.divide(value, MATH_CONTEXT)
            override fun fromBase(value: BigDecimal): BigDecimal = US_MPG_CONSTANT.divide(value, MATH_CONTEXT)
        }

        data object MilesPerGallonImperial : Unit(
            nameResId = R.string.unit_fuel_consumption_mpg_imperial,
            symbolResId = R.string.unit_fuel_consumption_mpg_imperial_symbol,
            factor = BigDecimal.ONE,
            key = "MilesPerGallonImperial"
        ) {
            override fun toBase(value: BigDecimal): BigDecimal = IMPERIAL_MPG_CONSTANT.divide(value, MATH_CONTEXT)
            override fun fromBase(value: BigDecimal): BigDecimal = IMPERIAL_MPG_CONSTANT.divide(value, MATH_CONTEXT)
        }
    }

    override val units: List<Unit> = listOf(
        Unit.LitersPer100Km,
        Unit.KilometersPerLiter,
        Unit.MilesPerGallonUs,
        Unit.MilesPerGallonImperial
    )
    override val defaultTopUnit: Unit = Unit.LitersPer100Km
    override val defaultBottomUnit: Unit = Unit.MilesPerGallonUs
}
