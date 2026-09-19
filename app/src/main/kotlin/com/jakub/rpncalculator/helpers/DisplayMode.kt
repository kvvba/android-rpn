package com.jakub.rpncalculator.helpers

enum class DisplayMode {
    NORMAL, SCIENTIFIC, ENGINEERING;

    fun next(): DisplayMode = entries[(ordinal + 1) % entries.size]
}
