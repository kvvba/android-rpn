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

    fun clear() {
        stack.clear()
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
        fun add(a: BigDecimal, b: BigDecimal): BigDecimal = a.add(b, MATH_CONTEXT)

        fun subtract(a: BigDecimal, b: BigDecimal): BigDecimal = a.subtract(b, MATH_CONTEXT)

        fun multiply(a: BigDecimal, b: BigDecimal): BigDecimal = a.multiply(b, MATH_CONTEXT)

        fun divide(a: BigDecimal, b: BigDecimal): BigDecimal {
            if (b.signum() == 0) {
                throw ArithmeticException("Division by zero")
            }
            return a.divide(b, MATH_CONTEXT)
        }

        fun power(a: BigDecimal, b: BigDecimal): BigDecimal {
            val expression = Expression("${a.toPlainString()}^${b.toPlainString()}")
            val result = expression.evaluate().numberValue
                ?: throw ArithmeticException("Invalid power expression")
            return result
        }

        fun sqrt(a: BigDecimal): BigDecimal {
            if (a.signum() < 0) {
                throw ArithmeticException("Square root of a negative number")
            }
            return a.sqrt(MATH_CONTEXT)
        }

        fun percent(a: BigDecimal): BigDecimal = a.divide(BigDecimal(100), MATH_CONTEXT)

        fun negate(a: BigDecimal): BigDecimal = a.negate()
    }
}
