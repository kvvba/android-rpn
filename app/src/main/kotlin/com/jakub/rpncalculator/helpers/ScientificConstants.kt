package com.jakub.rpncalculator.helpers

import java.math.BigDecimal

/** A named constant offered by the "const" picker, entered into X when chosen. */
data class NamedConstant(val label: String, val value: BigDecimal)

val SCIENTIFIC_CONSTANTS: List<NamedConstant> by lazy {
    listOf(
        NamedConstant("π (pi)", RpnEngine.PI),
        NamedConstant("e (Euler's number)", RpnEngine.E),
        NamedConstant("φ (golden ratio)", BigDecimal("1.6180339887498948482045868343656381")),
        NamedConstant("c (speed of light, m/s)", BigDecimal("299792458")),
        NamedConstant("G (gravitational constant, N·m²/kg²)", BigDecimal("6.6743E-11")),
        NamedConstant("h (Planck constant, J·s)", BigDecimal("6.62607015E-34")),
        NamedConstant("k (Boltzmann constant, J/K)", BigDecimal("1.380649E-23")),
        NamedConstant("Nₐ (Avogadro's number, 1/mol)", BigDecimal("6.02214076E23")),
        NamedConstant("R (gas constant, J/(mol·K))", BigDecimal("8.31446261815324")),
        NamedConstant("e (elementary charge, C)", BigDecimal("1.602176634E-19")),
        NamedConstant("g (standard gravity, m/s²)", BigDecimal("9.80665")),
        NamedConstant("F (Faraday constant, C/mol)", BigDecimal("96485.33212")),
        NamedConstant("ε₀ (vacuum permittivity, F/m)", BigDecimal("8.8541878128E-12")),
        NamedConstant("μ₀ (vacuum permeability, N/A²)", BigDecimal("1.25663706212E-6")),
        NamedConstant("σ (Stefan-Boltzmann constant, W/(m²·K⁴))", BigDecimal("5.670374419E-8")),
        NamedConstant("mₑ (electron mass, kg)", BigDecimal("9.1093837015E-31")),
        NamedConstant("mₚ (proton mass, kg)", BigDecimal("1.67262192369E-27")),
        NamedConstant("u (atomic mass unit, kg)", BigDecimal("1.66053906660E-27")),
        NamedConstant("AU (astronomical unit, m)", BigDecimal("149597870700"))
    )
}
