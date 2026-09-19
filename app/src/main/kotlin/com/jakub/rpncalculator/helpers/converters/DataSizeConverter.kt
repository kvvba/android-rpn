package com.jakub.rpncalculator.helpers.converters

import com.jakub.rpncalculator.R
import java.math.BigDecimal

/**
 * Base unit: byte.
 *
 * Main references:
 * - https://en.wikipedia.org/wiki/Byte
 * - https://en.wikipedia.org/wiki/Binary_prefix
 */
object DataSizeConverter : Converter {
    override val nameResId: Int = R.string.unit_data_size
    override val imageResId: Int = R.drawable.ic_data_size_vector
    override val key: String = "DataSizeConverter"

    sealed class Unit(nameResId: Int, symbolResId: Int, factor: BigDecimal, key: String) :
        Converter.Unit(nameResId, symbolResId, factor, key) {
        data object Bit : Unit(
            nameResId = R.string.unit_data_size_bit,
            symbolResId = R.string.unit_data_size_bit_symbol,
            factor = BigDecimal("0.125"),
            key = "Bit"
        )

        data object Byte : Unit(
            nameResId = R.string.unit_data_size_byte,
            symbolResId = R.string.unit_data_size_byte_symbol,
            factor = BigDecimal.ONE,
            key = "Byte"
        )

        data object Kilobyte : Unit(
            nameResId = R.string.unit_data_size_kilobyte,
            symbolResId = R.string.unit_data_size_kilobyte_symbol,
            factor = BigDecimal("1000"),
            key = "Kilobyte"
        )

        data object Kibibyte : Unit(
            nameResId = R.string.unit_data_size_kibibyte,
            symbolResId = R.string.unit_data_size_kibibyte_symbol,
            factor = BigDecimal("1024"),
            key = "Kibibyte"
        )

        data object Megabyte : Unit(
            nameResId = R.string.unit_data_size_megabyte,
            symbolResId = R.string.unit_data_size_megabyte_symbol,
            factor = BigDecimal("1000000"),
            key = "Megabyte"
        )

        data object Mebibyte : Unit(
            nameResId = R.string.unit_data_size_mebibyte,
            symbolResId = R.string.unit_data_size_mebibyte_symbol,
            factor = BigDecimal("1048576"),
            key = "Mebibyte"
        )

        data object Gigabyte : Unit(
            nameResId = R.string.unit_data_size_gigabyte,
            symbolResId = R.string.unit_data_size_gigabyte_symbol,
            factor = BigDecimal("1000000000"),
            key = "Gigabyte"
        )

        data object Gibibyte : Unit(
            nameResId = R.string.unit_data_size_gibibyte,
            symbolResId = R.string.unit_data_size_gibibyte_symbol,
            factor = BigDecimal("1073741824"),
            key = "Gibibyte"
        )

        data object Terabyte : Unit(
            nameResId = R.string.unit_data_size_terabyte,
            symbolResId = R.string.unit_data_size_terabyte_symbol,
            factor = BigDecimal("1000000000000"),
            key = "Terabyte"
        )

        data object Tebibyte : Unit(
            nameResId = R.string.unit_data_size_tebibyte,
            symbolResId = R.string.unit_data_size_tebibyte_symbol,
            factor = BigDecimal("1099511627776"),
            key = "Tebibyte"
        )
    }

    override val units: List<Unit> = listOf(
        Unit.Bit,
        Unit.Byte,
        Unit.Kilobyte,
        Unit.Kibibyte,
        Unit.Megabyte,
        Unit.Mebibyte,
        Unit.Gigabyte,
        Unit.Gibibyte,
        Unit.Terabyte,
        Unit.Tebibyte
    )
    override val defaultTopUnit: Unit = Unit.Gigabyte
    override val defaultBottomUnit: Unit = Unit.Gibibyte
}
