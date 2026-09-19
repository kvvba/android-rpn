package com.jakub.rpncalculator.helpers.converters

import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.helpers.MATH_CONTEXT
import java.math.BigDecimal

/**
 * Base unit: milliliter.
 *
 * Main references:
 * - https://en.wikipedia.org/wiki/Cooking_weights_and_measures
 * - https://en.wikipedia.org/wiki/Teaspoon
 * - https://en.wikipedia.org/wiki/Tablespoon
 */
object CookingConverter : Converter {
    override val nameResId: Int = R.string.unit_cooking
    override val imageResId: Int = R.drawable.ic_cooking_vector
    override val key: String = "CookingConverter"

    sealed class Unit(nameResId: Int, symbolResId: Int, factor: BigDecimal, key: String) :
        Converter.Unit(nameResId, symbolResId, factor, key) {
        data object Pinch : Unit(
            nameResId = R.string.unit_cooking_pinch,
            symbolResId = R.string.unit_cooking_pinch_symbol,
            factor = BigDecimal("0.30809509961"),
            key = "Pinch"
        )

        data object Dash : Unit(
            nameResId = R.string.unit_cooking_dash,
            symbolResId = R.string.unit_cooking_dash_symbol,
            factor = BigDecimal("0.61619019922"),
            key = "Dash"
        )

        data object TeaspoonUs : Unit(
            nameResId = R.string.unit_cooking_teaspoon_us,
            symbolResId = R.string.unit_cooking_teaspoon_us_symbol,
            factor = BigDecimal("4.92892159375"),
            key = "TeaspoonUs"
        )

        data object TablespoonUs : Unit(
            nameResId = R.string.unit_cooking_tablespoon_us,
            symbolResId = R.string.unit_cooking_tablespoon_us_symbol,
            factor = BigDecimal("14.78676478125"),
            key = "TablespoonUs"
        )

        data object FluidOunceUs : Unit(
            nameResId = R.string.unit_cooking_fluid_ounce_us,
            symbolResId = R.string.unit_cooking_fluid_ounce_us_symbol,
            factor = BigDecimal("29.5735295625"),
            key = "FluidOunceUs"
        )

        data object CupUs : Unit(
            nameResId = R.string.unit_cooking_cup_us,
            symbolResId = R.string.unit_cooking_cup_us_symbol,
            factor = BigDecimal("236.5882365"),
            key = "CupUs"
        )

        data object PintUs : Unit(
            nameResId = R.string.unit_cooking_pint_us,
            symbolResId = R.string.unit_cooking_pint_us_symbol,
            factor = BigDecimal("473.176473"),
            key = "PintUs"
        )

        data object QuartUs : Unit(
            nameResId = R.string.unit_cooking_quart_us,
            symbolResId = R.string.unit_cooking_quart_us_symbol,
            factor = BigDecimal("946.352946"),
            key = "QuartUs"
        )

        data object GallonUs : Unit(
            nameResId = R.string.unit_cooking_gallon_us,
            symbolResId = R.string.unit_cooking_gallon_us_symbol,
            factor = BigDecimal("3785.411784"),
            key = "GallonUs"
        )

        data object Milliliter : Unit(
            nameResId = R.string.unit_cooking_milliliter,
            symbolResId = R.string.unit_cooking_milliliter_symbol,
            factor = BigDecimal.ONE,
            key = "Milliliter"
        )

        data object Liter : Unit(
            nameResId = R.string.unit_cooking_liter,
            symbolResId = R.string.unit_cooking_liter_symbol,
            factor = BigDecimal("1000"),
            key = "Liter"
        )

        data object TeaspoonUk : Unit(
            nameResId = R.string.unit_cooking_teaspoon_uk,
            symbolResId = R.string.unit_cooking_teaspoon_uk_symbol,
            factor = BigDecimal("28.4130625").divide(BigDecimal("6"), MATH_CONTEXT),
            key = "TeaspoonUk"
        )

        data object DessertspoonUk : Unit(
            nameResId = R.string.unit_cooking_dessertspoon_uk,
            symbolResId = R.string.unit_cooking_dessertspoon_uk_symbol,
            factor = BigDecimal("28.4130625").divide(BigDecimal("3"), MATH_CONTEXT),
            key = "DessertspoonUk"
        )

        data object TablespoonUk : Unit(
            nameResId = R.string.unit_cooking_tablespoon_uk,
            symbolResId = R.string.unit_cooking_tablespoon_uk_symbol,
            factor = BigDecimal("28.4130625").divide(BigDecimal("2"), MATH_CONTEXT),
            key = "TablespoonUk"
        )

        data object FluidOunceUk : Unit(
            nameResId = R.string.unit_cooking_fluid_ounce_uk,
            symbolResId = R.string.unit_cooking_fluid_ounce_uk_symbol,
            factor = BigDecimal("28.4130625"),
            key = "FluidOunceUk"
        )

        data object PintUk : Unit(
            nameResId = R.string.unit_cooking_pint_uk,
            symbolResId = R.string.unit_cooking_pint_uk_symbol,
            factor = BigDecimal("568.26125"),
            key = "PintUk"
        )

        data object GallonUk : Unit(
            nameResId = R.string.unit_cooking_gallon_uk,
            symbolResId = R.string.unit_cooking_gallon_uk_symbol,
            factor = BigDecimal("4546.09"),
            key = "GallonUk"
        )

        data object TeaspoonMetric : Unit(
            nameResId = R.string.unit_cooking_teaspoon_metric,
            symbolResId = R.string.unit_cooking_teaspoon_metric_symbol,
            factor = BigDecimal("5"),
            key = "TeaspoonMetric"
        )

        data object TablespoonMetric : Unit(
            nameResId = R.string.unit_cooking_tablespoon_metric,
            symbolResId = R.string.unit_cooking_tablespoon_metric_symbol,
            factor = BigDecimal("15"),
            key = "TablespoonMetric"
        )

        data object CupMetric : Unit(
            nameResId = R.string.unit_cooking_cup_metric,
            symbolResId = R.string.unit_cooking_cup_metric_symbol,
            factor = BigDecimal("250"),
            key = "CupMetric"
        )
    }

    override val units: List<Unit> = listOf(
        Unit.Pinch,
        Unit.Dash,
        Unit.TeaspoonUs,
        Unit.TablespoonUs,
        Unit.FluidOunceUs,
        Unit.CupUs,
        Unit.PintUs,
        Unit.QuartUs,
        Unit.GallonUs,
        Unit.TeaspoonUk,
        Unit.DessertspoonUk,
        Unit.TablespoonUk,
        Unit.FluidOunceUk,
        Unit.PintUk,
        Unit.GallonUk,
        Unit.TeaspoonMetric,
        Unit.TablespoonMetric,
        Unit.CupMetric,
        Unit.Milliliter,
        Unit.Liter
    )
    override val defaultTopUnit: Unit = Unit.TeaspoonUs
    override val defaultBottomUnit: Unit = Unit.TablespoonUs
}
