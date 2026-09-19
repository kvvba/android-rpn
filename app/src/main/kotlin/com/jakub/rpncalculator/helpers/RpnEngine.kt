package com.jakub.rpncalculator.helpers

import com.ezylang.evalex.Expression
import java.math.BigDecimal

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

        fun sqrt(a: BigDecimal): BigDecimal {
            if (a.signum() < 0) {
                throw ArithmeticException("Square root of a negative number")
            }
            return a.sqrt(MATH_CONTEXT)
        }

        fun square(a: BigDecimal): BigDecimal = a.multiply(a, MATH_CONTEXT)

        fun percent(a: BigDecimal): BigDecimal = a.divide(BigDecimal(100), MATH_CONTEXT)

        fun negate(a: BigDecimal): BigDecimal = a.negate()

        // EvalEx's plain SIN/COS/TAN take degrees; the R-suffixed variants take radians.
        fun sin(a: BigDecimal): BigDecimal = evaluateExpression("SIN(${a.toPlainString()})")

        fun cos(a: BigDecimal): BigDecimal = evaluateExpression("COS(${a.toPlainString()})")

        fun tan(a: BigDecimal): BigDecimal = evaluateExpression("TAN(${a.toPlainString()})")

        // EvalEx names these the other way round from calculator convention: its LOG is natural
        // log and its LOG10 is base-10 log.
        fun log10(a: BigDecimal): BigDecimal = evaluateExpression("LOG10(${a.toPlainString()})")

        fun ln(a: BigDecimal): BigDecimal = evaluateExpression("LOG(${a.toPlainString()})")

        private fun evaluateExpression(expression: String): BigDecimal {
            return Expression(expression).evaluate().numberValue
                ?: throw ArithmeticException("Invalid expression: $expression")
        }
    }
}
