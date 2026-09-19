package com.jakub.rpncalculator.helpers.converters

import com.jakub.rpncalculator.R
import java.math.BigDecimal

/**
 * Base unit: kilogram per cubic meter.
 *
 * Main references:
 * - https://en.wikipedia.org/wiki/Density
 */
object DensityConverter : Converter {
    override val nameResId: Int = R.string.unit_density
    override val imageResId: Int = R.drawable.ic_density_vector
    override val key: String = "DensityConverter"

    sealed class Unit(nameResId: Int, symbolResId: Int, factor: BigDecimal, key: String) :
        Converter.Unit(nameResId, symbolResId, factor, key) {
        data object KilogramPerCubicMeter : Unit(
            nameResId = R.string.unit_density_kg_per_cubic_meter,
            symbolResId = R.string.unit_density_kg_per_cubic_meter_symbol,
            factor = BigDecimal.ONE,
            key = "KilogramPerCubicMeter"
        )

        data object GramPerLiter : Unit(
            nameResId = R.string.unit_density_gram_per_liter,
            symbolResId = R.string.unit_density_gram_per_liter_symbol,
            factor = BigDecimal.ONE,
            key = "GramPerLiter"
        )

        data object GramPerCubicCentimeter : Unit(
            nameResId = R.string.unit_density_gram_per_cubic_centimeter,
            symbolResId = R.string.unit_density_gram_per_cubic_centimeter_symbol,
            factor = BigDecimal("1000"),
            key = "GramPerCubicCentimeter"
        )

        data object KilogramPerLiter : Unit(
            nameResId = R.string.unit_density_kg_per_liter,
            symbolResId = R.string.unit_density_kg_per_liter_symbol,
            factor = BigDecimal("1000"),
            key = "KilogramPerLiter"
        )

        data object PoundPerCubicFoot : Unit(
            nameResId = R.string.unit_density_pound_per_cubic_foot,
            symbolResId = R.string.unit_density_pound_per_cubic_foot_symbol,
            factor = BigDecimal("16.01846337"),
            key = "PoundPerCubicFoot"
        )

        data object PoundPerCubicInch : Unit(
            nameResId = R.string.unit_density_pound_per_cubic_inch,
            symbolResId = R.string.unit_density_pound_per_cubic_inch_symbol,
            factor = BigDecimal("27679.90471"),
            key = "PoundPerCubicInch"
        )

        data object PoundPerGallonUs : Unit(
            nameResId = R.string.unit_density_pound_per_gallon_us,
            symbolResId = R.string.unit_density_pound_per_gallon_us_symbol,
            factor = BigDecimal("119.8264273"),
            key = "PoundPerGallonUs"
        )
    }

    override val units: List<Unit> = listOf(
        Unit.KilogramPerCubicMeter,
        Unit.GramPerLiter,
        Unit.GramPerCubicCentimeter,
        Unit.KilogramPerLiter,
        Unit.PoundPerCubicFoot,
        Unit.PoundPerCubicInch,
        Unit.PoundPerGallonUs
    )
    override val defaultTopUnit: Unit = Unit.KilogramPerCubicMeter
    override val defaultBottomUnit: Unit = Unit.GramPerCubicCentimeter
}
