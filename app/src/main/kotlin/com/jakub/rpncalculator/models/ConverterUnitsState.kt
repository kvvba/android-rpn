package com.jakub.rpncalculator.models

import com.jakub.rpncalculator.helpers.converters.Converter

data class ConverterUnitsState(
    val topUnit: Converter.Unit,
    val bottomUnit: Converter.Unit,
)
