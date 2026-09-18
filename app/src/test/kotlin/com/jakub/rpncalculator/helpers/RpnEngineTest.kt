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
}
