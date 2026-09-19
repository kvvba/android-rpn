package com.jakub.rpncalculator.helpers.converters

import com.jakub.rpncalculator.R
import java.math.BigDecimal

/**
 * Base unit: watt.
 *
 * Main references:
 * - https://en.wikipedia.org/wiki/Watt
 * - https://en.wikipedia.org/wiki/Horsepower
 * - https://en.wikipedia.org/wiki/British_thermal_unit
 */
object PowerConverter : Converter {
    override val nameResId: Int = R.string.unit_power
    override val imageResId: Int = R.drawable.ic_power_vector
    override val key: String = "PowerConverter"

    sealed class Unit(nameResId: Int, symbolResId: Int, factor: BigDecimal, key: String) :
        Converter.Unit(nameResId, symbolResId, factor, key) {
        data object Milliwatt : Unit(
            nameResId = R.string.unit_power_milliwatt,
            symbolResId = R.string.unit_power_milliwatt_symbol,
            factor = BigDecimal("0.001"),
            key = "Milliwatt"
        )

        data object Watt : Unit(
            nameResId = R.string.unit_power_watt,
            symbolResId = R.string.unit_power_watt_symbol,
            factor = BigDecimal.ONE,
            key = "Watt"
        )

        data object Kilowatt : Unit(
            nameResId = R.string.unit_power_kilowatt,
            symbolResId = R.string.unit_power_kilowatt_symbol,
            factor = BigDecimal("1000"),
            key = "Kilowatt"
        )

        data object Megawatt : Unit(
            nameResId = R.string.unit_power_megawatt,
            symbolResId = R.string.unit_power_megawatt_symbol,
            factor = BigDecimal("1000000"),
            key = "Megawatt"
        )

        data object Gigawatt : Unit(
            nameResId = R.string.unit_power_gigawatt,
            symbolResId = R.string.unit_power_gigawatt_symbol,
            factor = BigDecimal("1000000000"),
            key = "Gigawatt"
        )

        data object HorsepowerMechanical : Unit(
            nameResId = R.string.unit_power_horsepower_mechanical,
            symbolResId = R.string.unit_power_horsepower_mechanical_symbol,
            factor = BigDecimal("745.699872"),
            key = "HorsepowerMechanical"
        )

        data object HorsepowerMetric : Unit(
            nameResId = R.string.unit_power_horsepower_metric,
            symbolResId = R.string.unit_power_horsepower_metric_symbol,
            factor = BigDecimal("735.49875"),
            key = "HorsepowerMetric"
        )

        data object BtuPerHour : Unit(
            nameResId = R.string.unit_power_btu_per_hour,
            symbolResId = R.string.unit_power_btu_per_hour_symbol,
            factor = BigDecimal("0.29307107"),
            key = "BtuPerHour"
        )
    }

    override val units: List<Unit> = listOf(
        Unit.Milliwatt,
        Unit.Watt,
        Unit.Kilowatt,
        Unit.Megawatt,
        Unit.Gigawatt,
        Unit.HorsepowerMechanical,
        Unit.HorsepowerMetric,
        Unit.BtuPerHour
    )
    override val defaultTopUnit: Unit = Unit.Kilowatt
    override val defaultBottomUnit: Unit = Unit.HorsepowerMechanical
}
