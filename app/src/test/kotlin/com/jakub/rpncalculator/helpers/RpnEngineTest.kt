package com.jakub.rpncalculator.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class RpnEngineTest {
    private lateinit var engine: RpnEngine

    @Before
    fun setUp() {
        engine = RpnEngine()
    }

    @Test
    fun `push and peek return the top of the stack`() {
        engine.push(BigDecimal(3))
        engine.push(BigDecimal(4))
        assertEquals(BigDecimal(4), engine.peek())
        assertEquals(2, engine.size)
    }

    @Test
    fun `peek on empty stack returns null`() {
        assertNull(engine.peek())
    }

    @Test
    fun `add pops two values and pushes their sum`() {
        engine.push(BigDecimal(3))
        engine.push(BigDecimal(4))

        val outcome = engine.applyBinary(RpnEngine::add)

        assertEquals(OpOutcome.Success(BigDecimal(7)), outcome)
        assertEquals(1, engine.size)
        assertEquals(BigDecimal(7), engine.peek())
    }

    @Test
    fun `subtract preserves operand order (a minus b)`() {
        engine.push(BigDecimal(10))
        engine.push(BigDecimal(4))

        val outcome = engine.applyBinary(RpnEngine::subtract)

        assertEquals(OpOutcome.Success(BigDecimal(6)), outcome)
    }

    @Test
    fun `binary op with fewer than two values fails without mutating the stack`() {
        engine.push(BigDecimal(5))

        val outcome = engine.applyBinary(RpnEngine::add)

        assertEquals(OpOutcome.Error(RpnError.INSUFFICIENT_STACK), outcome)
        assertEquals(1, engine.size)
        assertEquals(BigDecimal(5), engine.peek())
    }

    @Test
    fun `divide by zero fails and restores the operands`() {
        engine.push(BigDecimal(5))
        engine.push(BigDecimal.ZERO)

        val outcome = engine.applyBinary(RpnEngine::divide)

        assertEquals(OpOutcome.Error(RpnError.INVALID_OPERATION), outcome)
        assertEquals(2, engine.size)
        assertEquals(BigDecimal.ZERO, engine.peek())
    }

    @Test
    fun `sqrt of a negative number fails and restores the operand`() {
        engine.push(BigDecimal(-9))

        val outcome = engine.applyUnary(RpnEngine::sqrt)

        assertEquals(OpOutcome.Error(RpnError.INVALID_OPERATION), outcome)
        assertEquals(1, engine.size)
        assertEquals(BigDecimal(-9), engine.peek())
    }

    @Test
    fun `sqrt of a positive number succeeds`() {
        engine.push(BigDecimal(9))

        val outcome = engine.applyUnary(RpnEngine::sqrt)

        assertTrue(outcome is OpOutcome.Success)
        assertEquals(0, BigDecimal(3).compareTo((outcome as OpOutcome.Success).value))
    }

    @Test
    fun `power supports fractional exponents`() {
        engine.push(BigDecimal(4))
        engine.push(BigDecimal("0.5"))

        val outcome = engine.applyBinary(RpnEngine::power)

        assertTrue(outcome is OpOutcome.Success)
        assertEquals(0, BigDecimal(2).compareTo((outcome as OpOutcome.Success).value))
    }

    @Test
    fun `percent divides by one hundred`() {
        engine.push(BigDecimal(50))

        val outcome = engine.applyUnary(RpnEngine::percent)

        assertTrue(outcome is OpOutcome.Success)
        assertEquals(0, BigDecimal("0.5").compareTo((outcome as OpOutcome.Success).value))
    }

    @Test
    fun `swapTop exchanges the two topmost values`() {
        engine.push(BigDecimal(1))
        engine.push(BigDecimal(2))

        assertTrue(engine.swapTop())

        assertEquals(BigDecimal(1), engine.peek())
        assertEquals(listOf(BigDecimal(2), BigDecimal(1)), engine.snapshot())
    }

    @Test
    fun `swapTop with fewer than two values fails`() {
        engine.push(BigDecimal(1))
        assertFalse(engine.swapTop())
    }

    @Test
    fun `rollDown moves X to the bottom and shifts the rest up`() {
        engine.push(BigDecimal(1))
        engine.push(BigDecimal(2))
        engine.push(BigDecimal(3))

        assertTrue(engine.rollDown())

        assertEquals(BigDecimal(2), engine.peek())
        assertEquals(listOf(BigDecimal(3), BigDecimal(1), BigDecimal(2)), engine.snapshot())
    }

    @Test
    fun `rollUp moves the bottom value to X and shifts the rest down`() {
        engine.push(BigDecimal(1))
        engine.push(BigDecimal(2))
        engine.push(BigDecimal(3))

        assertTrue(engine.rollUp())

        assertEquals(BigDecimal(1), engine.peek())
        assertEquals(listOf(BigDecimal(2), BigDecimal(3), BigDecimal(1)), engine.snapshot())
    }

    @Test
    fun `rollUp and rollDown undo each other`() {
        engine.push(BigDecimal(1))
        engine.push(BigDecimal(2))
        engine.push(BigDecimal(3))
        val original = engine.snapshot()

        engine.rollDown()
        engine.rollUp()

        assertEquals(original, engine.snapshot())
    }

    @Test
    fun `rollUp with fewer than two values fails`() {
        engine.push(BigDecimal(1))
        assertFalse(engine.rollUp())
    }

    @Test
    fun `rollDown with fewer than two values fails`() {
        engine.push(BigDecimal(1))
        assertFalse(engine.rollDown())
    }

    @Test
    fun `dropTop removes and returns the top value`() {
        engine.push(BigDecimal(1))
        engine.push(BigDecimal(2))

        assertEquals(BigDecimal(2), engine.dropTop())
        assertEquals(1, engine.size)
    }

    @Test
    fun `dropTop on empty stack returns null`() {
        assertNull(engine.dropTop())
    }

    @Test
    fun `clear empties the stack`() {
        engine.push(BigDecimal(1))
        engine.push(BigDecimal(2))
        engine.clear()
        assertEquals(0, engine.size)
    }

    @Test
    fun `negate flips the sign`() {
        assertEquals(BigDecimal(-5), RpnEngine.negate(BigDecimal(5)))
        assertEquals(BigDecimal(5), RpnEngine.negate(BigDecimal(-5)))
    }

    @Test
    fun `square multiplies a value by itself`() {
        assertEquals(0, BigDecimal(49).compareTo(RpnEngine.square(BigDecimal(7))))
    }

    @Test
    fun `sin cos and tan support degrees, radians and gradians`() {
        val tolerance = BigDecimal("1e-10")

        assertEquals(0, BigDecimal.ONE.compareTo(RpnEngine.sin(BigDecimal(90), AngleUnit.DEG)))
        assertEquals(0, BigDecimal.ONE.compareTo(RpnEngine.cos(BigDecimal.ZERO, AngleUnit.DEG)))
        assertEquals(0, BigDecimal.ZERO.compareTo(RpnEngine.tan(BigDecimal.ZERO, AngleUnit.DEG)))

        assertTrue(RpnEngine.sin(RpnEngine.PI.divide(BigDecimal(2)), AngleUnit.RAD).subtract(BigDecimal.ONE).abs() < tolerance)
        assertTrue(RpnEngine.sin(BigDecimal(100), AngleUnit.GRAD).subtract(BigDecimal.ONE).abs() < tolerance)
    }

    @Test
    fun `asin acos and atan are the inverse of sin cos and tan`() {
        val tolerance = BigDecimal("1e-10")

        assertTrue(RpnEngine.asin(BigDecimal.ONE, AngleUnit.DEG).subtract(BigDecimal(90)).abs() < tolerance)
        assertTrue(RpnEngine.acos(BigDecimal.ONE, AngleUnit.DEG).abs() < tolerance)
        assertTrue(RpnEngine.atan(BigDecimal.ONE, AngleUnit.DEG).subtract(BigDecimal(45)).abs() < tolerance)
        assertTrue(RpnEngine.asin(BigDecimal.ONE, AngleUnit.GRAD).subtract(BigDecimal(100)).abs() < tolerance)
    }

    @Test
    fun `hyperbolic and inverse hyperbolic functions round-trip`() {
        val tolerance = BigDecimal("1e-10")

        assertTrue(RpnEngine.sinh(BigDecimal.ZERO).abs() < tolerance)
        assertTrue(RpnEngine.cosh(BigDecimal.ZERO).subtract(BigDecimal.ONE).abs() < tolerance)
        assertTrue(RpnEngine.tanh(BigDecimal.ZERO).abs() < tolerance)

        val x = BigDecimal("1.5")
        assertTrue(RpnEngine.asinh(RpnEngine.sinh(x)).subtract(x).abs() < tolerance)
        assertTrue(RpnEngine.acosh(RpnEngine.cosh(x)).subtract(x).abs() < tolerance)
        assertTrue(RpnEngine.atanh(RpnEngine.tanh(x)).subtract(x).abs() < tolerance)
    }

    @Test
    fun `inverse computes 1 over x`() {
        assertEquals(0, BigDecimal("0.25").compareTo(RpnEngine.inverse(BigDecimal(4))))
    }

    @Test
    fun `exp is the inverse of ln`() {
        val tolerance = BigDecimal("1e-10")
        assertTrue(RpnEngine.exp(BigDecimal.ONE).subtract(RpnEngine.E).abs() < tolerance)
        assertTrue(RpnEngine.ln(RpnEngine.exp(BigDecimal(2))).subtract(BigDecimal(2)).abs() < tolerance)
    }

    @Test
    fun `log10 is base-10 logarithm`() {
        val tolerance = BigDecimal("1e-10")
        assertTrue(RpnEngine.log10(BigDecimal(100)).subtract(BigDecimal(2)).abs() < tolerance)
        assertTrue(RpnEngine.log10(BigDecimal(1000)).subtract(BigDecimal(3)).abs() < tolerance)
    }

    @Test
    fun `factorial multiplies down to 1`() {
        assertEquals(0, BigDecimal(120).compareTo(RpnEngine.factorial(BigDecimal(5))))
        assertEquals(0, BigDecimal.ONE.compareTo(RpnEngine.factorial(BigDecimal.ZERO)))
    }

    @Test
    fun `factorial rejects negative numbers`() {
        try {
            RpnEngine.factorial(BigDecimal(-1))
            assertTrue("expected ArithmeticException", false)
        } catch (_: ArithmeticException) {
            // expected
        }
    }

    @Test
    fun `percentChange computes 100 times (x minus y) over y`() {
        assertEquals(0, BigDecimal(-10).compareTo(RpnEngine.percentChange(BigDecimal(100), BigDecimal(90))))
        assertEquals(0, BigDecimal(10).compareTo(RpnEngine.percentChange(BigDecimal(100), BigDecimal(110))))
    }

    @Test
    fun `modulus computes the remainder of a divided by b`() {
        assertEquals(0, BigDecimal(1).compareTo(RpnEngine.modulus(BigDecimal(7), BigDecimal(3))))
        assertEquals(0, BigDecimal(-1).compareTo(RpnEngine.modulus(BigDecimal(-7), BigDecimal(3))))
    }

    @Test
    fun `modulus by zero fails`() {
        try {
            RpnEngine.modulus(BigDecimal(7), BigDecimal.ZERO)
            assertTrue("expected ArithmeticException", false)
        } catch (_: ArithmeticException) {
            // expected
        }
    }

    @Test
    fun `quotient computes the truncated integer division of a by b`() {
        assertEquals(0, BigDecimal(2).compareTo(RpnEngine.quotient(BigDecimal(7), BigDecimal(3))))
        assertEquals(0, BigDecimal(-2).compareTo(RpnEngine.quotient(BigDecimal(-7), BigDecimal(3))))
    }

    @Test
    fun `quotient by zero fails`() {
        try {
            RpnEngine.quotient(BigDecimal(7), BigDecimal.ZERO)
            assertTrue("expected ArithmeticException", false)
        } catch (_: ArithmeticException) {
            // expected
        }
    }

    @Test
    fun `nPr counts ordered arrangements`() {
        assertEquals(0, BigDecimal(20).compareTo(RpnEngine.nPr(BigDecimal(5), BigDecimal(2))))
        assertEquals(0, BigDecimal.ONE.compareTo(RpnEngine.nPr(BigDecimal(5), BigDecimal.ZERO)))
    }

    @Test
    fun `nCr counts unordered selections`() {
        assertEquals(0, BigDecimal(10).compareTo(RpnEngine.nCr(BigDecimal(5), BigDecimal(2))))
        assertEquals(0, BigDecimal.ONE.compareTo(RpnEngine.nCr(BigDecimal(5), BigDecimal(5))))
    }

    @Test
    fun `xthRoot computes the x-th root of y`() {
        val tolerance = BigDecimal("1e-10")
        assertTrue(RpnEngine.xthRoot(BigDecimal(8), BigDecimal(3)).subtract(BigDecimal(2)).abs() < tolerance)
        assertTrue(RpnEngine.xthRoot(BigDecimal(16), BigDecimal(4)).subtract(BigDecimal(2)).abs() < tolerance)
    }

    @Test
    fun `nPr and nCr reject r greater than n`() {
        try {
            RpnEngine.nPr(BigDecimal(2), BigDecimal(5))
            assertTrue("expected ArithmeticException", false)
        } catch (_: ArithmeticException) {
            // expected
        }
    }

    @Test
    fun `ln is natural log`() {
        assertEquals(0, BigDecimal.ONE.compareTo(RpnEngine.ln(RpnEngine.E)))
    }

    @Test
    fun `logBase computes log of the argument in the given base`() {
        // Computed via the change-of-base identity (ln(argument) / ln(base)). EvalEx's LOG is
        // double-precision internally regardless of the requested MathContext (confirmed: it
        // returns ln(8) as exactly 17 significant digits), so even a clean answer like this can
        // land ~1e-16 off — a tight tolerance, not exact equality, is the correct check here.
        val tolerance = BigDecimal("1e-10")
        assertTrue(
            RpnEngine.logBase(BigDecimal(100), BigDecimal(10)).subtract(BigDecimal(2)).abs() < tolerance
        )
        assertTrue(
            RpnEngine.logBase(BigDecimal(8), BigDecimal(2)).subtract(BigDecimal(3)).abs() < tolerance
        )
    }

    @Test
    fun `pi and e are rounded to the shared math context`() {
        assertEquals(MATH_CONTEXT.precision, RpnEngine.PI.precision())
        assertEquals(MATH_CONTEXT.precision, RpnEngine.E.precision())
        assertTrue(RpnEngine.PI > BigDecimal("3.14") && RpnEngine.PI < BigDecimal("3.15"))
        assertTrue(RpnEngine.E > BigDecimal("2.71") && RpnEngine.E < BigDecimal("2.72"))
    }

    @Test
    fun `memory starts at zero and supports add, subtract, recall and clear`() {
        assertEquals(0, BigDecimal.ZERO.compareTo(engine.memoryValue()))

        engine.memoryAdd(BigDecimal(5))
        assertEquals(0, BigDecimal(5).compareTo(engine.memoryValue()))

        engine.memorySubtract(BigDecimal(2))
        assertEquals(0, BigDecimal(3).compareTo(engine.memoryValue()))

        engine.memoryClear()
        assertEquals(0, BigDecimal.ZERO.compareTo(engine.memoryValue()))
    }

    @Test
    fun `setMemoryValue overwrites memory directly`() {
        engine.setMemoryValue(BigDecimal(42))
        assertEquals(0, BigDecimal(42).compareTo(engine.memoryValue()))
    }

    @Test
    fun `sqrt of a negative number does not corrupt memory`() {
        engine.memoryAdd(BigDecimal(9))
        engine.push(BigDecimal(-9))
        engine.applyUnary(RpnEngine::sqrt)
        assertEquals(0, BigDecimal(9).compareTo(engine.memoryValue()))
    }
}
