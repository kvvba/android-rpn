package com.jakub.rpncalculator.helpers

import java.math.BigDecimal

enum class ConstantSection(val label: String) {
    MATHEMATICAL("Mathematical"),
    UNIVERSAL("Universal"),
    ELECTROMAGNETIC("Electromagnetic"),
    ATOMIC_AND_NUCLEAR("Atomic & nuclear"),
    PHYSICAL_CHEMISTRY("Physical chemistry"),
    THERMODYNAMIC("Thermodynamic"),
    ASTROPHYSICS("Astrophysics")
}

/** A named constant offered by the "const" picker, entered into X when chosen. */
data class NamedConstant(val label: String, val value: BigDecimal, val section: ConstantSection)

/**
 * CODATA physical constants, the same values Casio scientific calculators (e.g. fx-991EX) ship
 * as their built-in constant table, grouped the same way. A few of Casio's more obscure nuclear
 * magnetic-moment/gyromagnetic-ratio entries are intentionally omitted rather than risk a wrong
 * low-order digit.
 */
val SCIENTIFIC_CONSTANTS: List<NamedConstant> by lazy {
    listOf(
        // Mathematical
        NamedConstant("π (pi)", RpnEngine.PI, ConstantSection.MATHEMATICAL),
        NamedConstant("e (Euler's number)", RpnEngine.E, ConstantSection.MATHEMATICAL),
        NamedConstant("φ (golden ratio)", BigDecimal("1.6180339887498948482045868343656381"), ConstantSection.MATHEMATICAL),

        // Universal
        NamedConstant("c (speed of light, m/s)", BigDecimal("299792458"), ConstantSection.UNIVERSAL),
        NamedConstant("G (gravitational constant, N·m²/kg²)", BigDecimal("6.6743E-11"), ConstantSection.UNIVERSAL),
        NamedConstant("h (Planck constant, J·s)", BigDecimal("6.62607015E-34"), ConstantSection.UNIVERSAL),
        NamedConstant("ħ (reduced Planck constant, J·s)", BigDecimal("1.054571817E-34"), ConstantSection.UNIVERSAL),
        NamedConstant("g (standard gravity, m/s²)", BigDecimal("9.80665"), ConstantSection.UNIVERSAL),
        NamedConstant("atm (standard atmosphere, Pa)", BigDecimal("101325"), ConstantSection.UNIVERSAL),

        // Electromagnetic
        NamedConstant("e (elementary charge, C)", BigDecimal("1.602176634E-19"), ConstantSection.ELECTROMAGNETIC),
        NamedConstant("ε₀ (vacuum permittivity, F/m)", BigDecimal("8.8541878128E-12"), ConstantSection.ELECTROMAGNETIC),
        NamedConstant("μ₀ (vacuum permeability, N/A²)", BigDecimal("1.25663706212E-6"), ConstantSection.ELECTROMAGNETIC),
        NamedConstant("F (Faraday constant, C/mol)", BigDecimal("96485.33212"), ConstantSection.ELECTROMAGNETIC),
        NamedConstant("α (fine-structure constant)", BigDecimal("7.2973525693E-3"), ConstantSection.ELECTROMAGNETIC),
        NamedConstant("μB (Bohr magneton, J/T)", BigDecimal("9.2740100783E-24"), ConstantSection.ELECTROMAGNETIC),
        NamedConstant("μN (nuclear magneton, J/T)", BigDecimal("5.0507837461E-27"), ConstantSection.ELECTROMAGNETIC),

        // Atomic & nuclear
        NamedConstant("mₑ (electron mass, kg)", BigDecimal("9.1093837015E-31"), ConstantSection.ATOMIC_AND_NUCLEAR),
        NamedConstant("mₚ (proton mass, kg)", BigDecimal("1.67262192369E-27"), ConstantSection.ATOMIC_AND_NUCLEAR),
        NamedConstant("mₙ (neutron mass, kg)", BigDecimal("1.67492749804E-27"), ConstantSection.ATOMIC_AND_NUCLEAR),
        NamedConstant("mμ (muon mass, kg)", BigDecimal("1.883531627E-28"), ConstantSection.ATOMIC_AND_NUCLEAR),
        NamedConstant("u (atomic mass unit, kg)", BigDecimal("1.66053906660E-27"), ConstantSection.ATOMIC_AND_NUCLEAR),
        NamedConstant("a₀ (Bohr radius, m)", BigDecimal("5.29177210903E-11"), ConstantSection.ATOMIC_AND_NUCLEAR),
        NamedConstant("rₑ (classical electron radius, m)", BigDecimal("2.8179403262E-15"), ConstantSection.ATOMIC_AND_NUCLEAR),
        NamedConstant("λC (electron Compton wavelength, m)", BigDecimal("2.42631023867E-12"), ConstantSection.ATOMIC_AND_NUCLEAR),

        // Physical chemistry
        NamedConstant("Nₐ (Avogadro constant, 1/mol)", BigDecimal("6.02214076E23"), ConstantSection.PHYSICAL_CHEMISTRY),
        NamedConstant("R (molar gas constant, J/(mol·K))", BigDecimal("8.31446261815324"), ConstantSection.PHYSICAL_CHEMISTRY),
        NamedConstant("k (Boltzmann constant, J/K)", BigDecimal("1.380649E-23"), ConstantSection.PHYSICAL_CHEMISTRY),
        NamedConstant("Vm (molar volume of ideal gas, m³/mol)", BigDecimal("2.271095464E-2"), ConstantSection.PHYSICAL_CHEMISTRY),

        // Thermodynamic
        NamedConstant("σ (Stefan-Boltzmann constant, W/(m²·K⁴))", BigDecimal("5.670374419E-8"), ConstantSection.THERMODYNAMIC),
        NamedConstant("b (Wien displacement constant, m·K)", BigDecimal("2.897771955E-3"), ConstantSection.THERMODYNAMIC),
        NamedConstant("T₀ (ice point, K)", BigDecimal("273.15"), ConstantSection.THERMODYNAMIC),

        // Astrophysics (not on Casio, kept for convenience)
        NamedConstant("AU (astronomical unit, m)", BigDecimal("149597870700"), ConstantSection.ASTROPHYSICS),
        NamedConstant("ly (light-year, m)", BigDecimal("9460730472580800"), ConstantSection.ASTROPHYSICS),
        NamedConstant("pc (parsec, m)", BigDecimal("3.0856775814913673E16"), ConstantSection.ASTROPHYSICS),
        NamedConstant("M☉ (solar mass, kg)", BigDecimal("1.98892E30"), ConstantSection.ASTROPHYSICS),
        NamedConstant("R☉ (solar radius, m)", BigDecimal("6.957E8"), ConstantSection.ASTROPHYSICS),
        NamedConstant("M⊕ (Earth mass, kg)", BigDecimal("5.9722E24"), ConstantSection.ASTROPHYSICS),
        NamedConstant("R⊕ (Earth equatorial radius, m)", BigDecimal("6.3781E6"), ConstantSection.ASTROPHYSICS)
    )
}
