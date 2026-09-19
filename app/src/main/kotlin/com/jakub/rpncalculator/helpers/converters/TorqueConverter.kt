package com.jakub.rpncalculator.helpers.converters

import com.jakub.rpncalculator.R
import java.math.BigDecimal

/**
 * Base unit: newton-meter.
 *
 * Main references:
 * - https://en.wikipedia.org/wiki/Torque
 */
object TorqueConverter : Converter {
    override val nameResId: Int = R.string.unit_torque
    override val imageResId: Int = R.drawable.ic_torque_vector
    override val key: String = "TorqueConverter"

    sealed class Unit(nameResId: Int, symbolResId: Int, factor: BigDecimal, key: String) :
        Converter.Unit(nameResId, symbolResId, factor, key) {
        data object NewtonMeter : Unit(
            nameResId = R.string.unit_torque_newton_meter,
            symbolResId = R.string.unit_torque_newton_meter_symbol,
            factor = BigDecimal.ONE,
            key = "NewtonMeter"
        )

        data object NewtonCentimeter : Unit(
            nameResId = R.string.unit_torque_newton_centimeter,
            symbolResId = R.string.unit_torque_newton_centimeter_symbol,
            factor = BigDecimal("0.01"),
            key = "NewtonCentimeter"
        )

        data object KilogramForceMeter : Unit(
            nameResId = R.string.unit_torque_kilogram_force_meter,
            symbolResId = R.string.unit_torque_kilogram_force_meter_symbol,
            factor = BigDecimal("9.80665"),
            key = "KilogramForceMeter"
        )

        data object PoundForceFoot : Unit(
            nameResId = R.string.unit_torque_pound_force_foot,
            symbolResId = R.string.unit_torque_pound_force_foot_symbol,
            factor = BigDecimal("1.3558179483314"),
            key = "PoundForceFoot"
        )

        data object PoundForceInch : Unit(
            nameResId = R.string.unit_torque_pound_force_inch,
            symbolResId = R.string.unit_torque_pound_force_inch_symbol,
            factor = BigDecimal("0.1129848290276"),
            key = "PoundForceInch"
        )

        data object OunceForceInch : Unit(
            nameResId = R.string.unit_torque_ounce_force_inch,
            symbolResId = R.string.unit_torque_ounce_force_inch_symbol,
            factor = BigDecimal("0.007061551814"),
            key = "OunceForceInch"
        )
    }

    override val units: List<Unit> = listOf(
        Unit.NewtonMeter,
        Unit.NewtonCentimeter,
        Unit.KilogramForceMeter,
        Unit.PoundForceFoot,
        Unit.PoundForceInch,
        Unit.OunceForceInch
    )
    override val defaultTopUnit: Unit = Unit.NewtonMeter
    override val defaultBottomUnit: Unit = Unit.PoundForceFoot
}
