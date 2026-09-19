package com.jakub.rpncalculator.helpers

import com.ezylang.evalex.Expression
import java.math.BigDecimal
import java.math.MathContext

enum class RpnError {
    INSUFFICIENT_STACK,
    INVALID_OPERATION
}

sealed class OpOutcome {
    data class Success(val value: BigDecimal) : OpOutcome()
    data class Error(val error: RpnError) : OpOutcome()
}

/**
 * Pure stack-based RPN arithmetic engine. Holds no Android dependencies so it can be
 * unit tested on the plain JVM. The bottom of [stack] is index 0, the top (the "X" register) is
 * the last element.
 */
class RpnEngine {
    private val stack = ArrayDeque<BigDecimal>()
    private var memory: BigDecimal = BigDecimal.ZERO

    val size: Int get() = stack.size

    fun snapshot(): List<BigDecimal> = stack.toList()

    fun peek(): BigDecimal? = stack.lastOrNull()

    fun push(value: BigDecimal) {
        stack.addLast(value)
    }

    fun dropTop(): BigDecimal? = if (stack.isEmpty()) null else stack.removeLast()

    fun swapTop(): Boolean {
        if (stack.size < 2) {
            return false
        }
        val x = stack.removeLast()
        val y = stack.removeLast()
        stack.addLast(x)
        stack.addLast(y)
        return true
    }

    /** Rotates the whole stack so the bottom-most value becomes the new X. */
    fun rollUp(): Boolean {
        if (stack.size < 2) {
            return false
        }
        stack.addLast(stack.removeFirst())
        return true
    }

    /** Rotates the whole stack so X becomes the new bottom-most value. */
    fun rollDown(): Boolean {
        if (stack.size < 2) {
            return false
        }
        stack.addFirst(stack.removeLast())
        return true
    }

    fun clear() {
        stack.clear()
    }

    fun memoryValue(): BigDecimal = memory

    fun setMemoryValue(value: BigDecimal) {
        memory = value
    }

    fun memoryClear() {
        memory = BigDecimal.ZERO
    }

    fun memoryAdd(value: BigDecimal) {
        memory = memory.add(value, MATH_CONTEXT)
    }

    fun memorySubtract(value: BigDecimal) {
        memory = memory.subtract(value, MATH_CONTEXT)
    }

    fun applyBinary(compute: (BigDecimal, BigDecimal) -> BigDecimal): OpOutcome {
        if (stack.size < 2) {
            return OpOutcome.Error(RpnError.INSUFFICIENT_STACK)
        }

        val b = stack.removeLast()
        val a = stack.removeLast()
        return try {
            val result = compute(a, b)
            stack.addLast(result)
            OpOutcome.Success(result)
        } catch (_: Exception) {
            stack.addLast(a)
            stack.addLast(b)
            OpOutcome.Error(RpnError.INVALID_OPERATION)
        }
    }

    fun applyUnary(compute: (BigDecimal) -> BigDecimal): OpOutcome {
        if (stack.isEmpty()) {
            return OpOutcome.Error(RpnError.INSUFFICIENT_STACK)
        }

        val a = stack.removeLast()
        return try {
            val result = compute(a)
            stack.addLast(result)
            OpOutcome.Success(result)
        } catch (_: Exception) {
            stack.addLast(a)
            OpOutcome.Error(RpnError.INVALID_OPERATION)
        }
    }

    companion object {
        /**
         * EvalEx's trig/log functions are backed by double-precision math internally regardless
         * of the MathContext they're asked to evaluate with (confirmed by inspection: LOG(8)
         * comes back as exactly 17 significant digits, a double's ceiling). Rounding their
         * results to this precision is honest about what's actually reliable, rather than
         * carrying ~19 extra digits of noise up to [MATH_CONTEXT] that can make an otherwise
         * clean answer (e.g. a base-2 log of a power of two) miss exact equality by roughly
         * 1e-16 and needlessly trip the display's scientific-notation fallback.
         */
        private val TRANSCENDENTAL_CONTEXT = MathContext(15)

        /**
         * The full-precision constants below come from EvalEx's own literals, which carry more
         * digits than [MATH_CONTEXT]; rounding here keeps them consistent with every other value
         * this engine produces.
         */
        val PI: BigDecimal by lazy { evaluateExpression("PI").round(MATH_CONTEXT) }
        val E: BigDecimal by lazy { evaluateExpression("E").round(MATH_CONTEXT) }

        fun add(a: BigDecimal, b: BigDecimal): BigDecimal = a.add(b, MATH_CONTEXT)

        fun subtract(a: BigDecimal, b: BigDecimal): BigDecimal = a.subtract(b, MATH_CONTEXT)

        fun multiply(a: BigDecimal, b: BigDecimal): BigDecimal = a.multiply(b, MATH_CONTEXT)

        fun divide(a: BigDecimal, b: BigDecimal): BigDecimal {
            if (b.signum() == 0) {
                throw ArithmeticException("Division by zero")
            }
            return a.divide(b, MATH_CONTEXT)
        }

        fun power(a: BigDecimal, b: BigDecimal): BigDecimal =
            evaluateExpression("${a.toPlainString()}^${b.toPlainString()}")

        /** The [x]th root of [y]: y^(1/x). */
        fun xthRoot(y: BigDecimal, x: BigDecimal): BigDecimal =
            evaluateExpression("${y.toPlainString()}^(1/${x.toPlainString()})")

        fun sqrt(a: BigDecimal): BigDecimal {
            if (a.signum() < 0) {
                throw ArithmeticException("Square root of a negative number")
            }
            return a.sqrt(MATH_CONTEXT)
        }

        fun square(a: BigDecimal): BigDecimal = a.multiply(a, MATH_CONTEXT)

        fun percent(a: BigDecimal): BigDecimal = a.divide(BigDecimal(100), MATH_CONTEXT)

        fun negate(a: BigDecimal): BigDecimal = a.negate()

        // EvalEx's R-suffixed trig functions take radians; converting through radians ourselves
        // (rather than relying on EvalEx's degree-only functions) lets one code path support
        // DEG/RAD/GRAD uniformly, since EvalEx has no gradian support at all.
        fun sin(a: BigDecimal, unit: AngleUnit): BigDecimal =
            evaluateExpression("SINR(${toRadians(a, unit).toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        fun cos(a: BigDecimal, unit: AngleUnit): BigDecimal =
            evaluateExpression("COSR(${toRadians(a, unit).toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        fun tan(a: BigDecimal, unit: AngleUnit): BigDecimal =
            evaluateExpression("TANR(${toRadians(a, unit).toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        fun asin(a: BigDecimal, unit: AngleUnit): BigDecimal =
            fromRadians(evaluateExpression("ASINR(${a.toPlainString()})"), unit).round(TRANSCENDENTAL_CONTEXT)

        fun acos(a: BigDecimal, unit: AngleUnit): BigDecimal =
            fromRadians(evaluateExpression("ACOSR(${a.toPlainString()})"), unit).round(TRANSCENDENTAL_CONTEXT)

        fun atan(a: BigDecimal, unit: AngleUnit): BigDecimal =
            fromRadians(evaluateExpression("ATANR(${a.toPlainString()})"), unit).round(TRANSCENDENTAL_CONTEXT)

        // Hyperbolic functions take no angle argument, so they're unaffected by the angle unit.
        fun sinh(a: BigDecimal): BigDecimal =
            evaluateExpression("SINH(${a.toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        fun cosh(a: BigDecimal): BigDecimal =
            evaluateExpression("COSH(${a.toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        fun tanh(a: BigDecimal): BigDecimal =
            evaluateExpression("TANH(${a.toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        fun asinh(a: BigDecimal): BigDecimal =
            evaluateExpression("ASINH(${a.toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        fun acosh(a: BigDecimal): BigDecimal =
            evaluateExpression("ACOSH(${a.toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        fun atanh(a: BigDecimal): BigDecimal =
            evaluateExpression("ATANH(${a.toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        // EvalEx names this the other way round from calculator convention: its LOG is natural
        // log, not base-10.
        fun ln(a: BigDecimal): BigDecimal =
            evaluateExpression("LOG(${a.toPlainString()})").round(TRANSCENDENTAL_CONTEXT)

        /** log base [base] of [argument], via the change-of-base identity. */
        fun logBase(argument: BigDecimal, base: BigDecimal): BigDecimal =
            ln(argument).divide(ln(base), TRANSCENDENTAL_CONTEXT)

        /** Number of permutations of [r] items taken from [n]: n! / (n - r)!. */
        fun nPr(n: BigDecimal, r: BigDecimal): BigDecimal {
            val (nInt, rInt) = validatedNAndR(n, r)
            var result = BigDecimal.ONE
            for (i in 0 until rInt) {
                result = result.multiply(BigDecimal(nInt - i))
            }
            return result
        }

        /** Number of combinations of [r] items taken from [n]: n! / (r! (n - r)!). */
        fun nCr(n: BigDecimal, r: BigDecimal): BigDecimal {
            val (nInt, rInt) = validatedNAndR(n, r)
            val smallerR = minOf(rInt, nInt - rInt)
            var numerator = BigDecimal.ONE
            var denominator = BigDecimal.ONE
            for (i in 0 until smallerR) {
                numerator = numerator.multiply(BigDecimal(nInt - i))
                denominator = denominator.multiply(BigDecimal(i + 1))
            }
            return numerator.divide(denominator, MATH_CONTEXT)
        }

        private fun validatedNAndR(n: BigDecimal, r: BigDecimal): Pair<Int, Int> {
            val nInt = n.intValueExact()
            val rInt = r.intValueExact()
            if (nInt < 0 || rInt < 0 || rInt > nInt) {
                throw ArithmeticException("nPr/nCr require 0 <= r <= n")
            }
            return nInt to rInt
        }

        private fun toRadians(value: BigDecimal, unit: AngleUnit): BigDecimal = when (unit) {
            AngleUnit.RAD -> value
            AngleUnit.DEG -> value.multiply(PI, MATH_CONTEXT).divide(BigDecimal(180), MATH_CONTEXT)
            AngleUnit.GRAD -> value.multiply(PI, MATH_CONTEXT).divide(BigDecimal(200), MATH_CONTEXT)
        }

        private fun fromRadians(value: BigDecimal, unit: AngleUnit): BigDecimal = when (unit) {
            AngleUnit.RAD -> value
            AngleUnit.DEG -> value.multiply(BigDecimal(180), MATH_CONTEXT).divide(PI, MATH_CONTEXT)
            AngleUnit.GRAD -> value.multiply(BigDecimal(200), MATH_CONTEXT).divide(PI, MATH_CONTEXT)
        }

        private fun evaluateExpression(expression: String): BigDecimal {
            return Expression(expression).evaluate().numberValue
                ?: throw ArithmeticException("Invalid expression: $expression")
        }
    }
}
