package com.jakub.rpncalculator.helpers

enum class AngleUnit {
    DEG, RAD, GRAD;

    fun next(): AngleUnit = entries[(ordinal + 1) % entries.size]
}
