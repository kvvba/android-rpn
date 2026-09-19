package com.jakub.rpncalculator.helpers

import android.content.Context
import org.fossify.commons.extensions.toast
import com.jakub.rpncalculator.R
import com.jakub.rpncalculator.models.History
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal

private const val MAX_UNDO_HISTORY = 100

private val NAMED_UNARY_FUNCTIONS = setOf(
    SIN, COS, TAN, ASIN, ACOS, ATAN, SINH, COSH, TANH, ASINH, ACOSH, ATANH, LN, LOG10
)

private data class CalculatorSnapshot(
    val stack: List<BigDecimal>,
    val entry: String,
    val entryActive: Boolean,
    val memory: BigDecimal
)

/**
 * Android-facing wrapper around [RpnEngine]: owns the text entry buffer, number formatting,
 * history recording and instance-state persistence. All arithmetic is delegated to [RpnEngine].
 */
class CalculatorImpl(
    calculator: Calculator,
    private val context: Context,
    calculatorState: String = ""
) {
    private var callback: Calculator? = calculator
    private val engine = RpnEngine()
    private var entry = "0"
    private var entryActive = false
    private var angleUnit = AngleUnit.DEG
    private var displayMode = DisplayMode.NORMAL
    private var engineeringShift = 0
    private var lastX: BigDecimal = BigDecimal.ZERO
    private val formatter = NumberFormatHelper()
    private val undoHistory = ArrayDeque<CalculatorSnapshot>()

    private val decimalSeparator: String get() = formatter.decimalSeparator
    private val groupingSeparator: String get() = formatter.groupingSeparator

    init {
        if (calculatorState != "") {
            setFromSaveInstanceState(calculatorState)
        }
        refreshDisplay()
    }

    fun numpadClicked(id: Int) {
        when (id) {
            R.id.btn_decimal -> decimalClicked()
            R.id.btn_0 -> addDigit("0")
            R.id.btn_1 -> addDigit("1")
            R.id.btn_2 -> addDigit("2")
            R.id.btn_3 -> addDigit("3")
            R.id.btn_4 -> addDigit("4")
            R.id.btn_5 -> addDigit("5")
            R.id.btn_6 -> addDigit("6")
            R.id.btn_7 -> addDigit("7")
            R.id.btn_8 -> addDigit("8")
            R.id.btn_9 -> addDigit("9")
        }
    }

    private fun pushHistory() {
        undoHistory.addLast(CalculatorSnapshot(engine.snapshot(), entry, entryActive, engine.memoryValue()))
        if (undoHistory.size > MAX_UNDO_HISTORY) {
            undoHistory.removeFirst()
        }
    }

    fun handleUndo() {
        val previous = undoHistory.removeLastOrNull() ?: return
        engine.clear()
        previous.stack.forEach { engine.push(it) }
        entry = previous.entry
        entryActive = previous.entryActive
        engine.setMemoryValue(previous.memory)
        refreshDisplay()
    }

    private fun addDigit(digit: String) {
        pushHistory()
        if (!entryActive) {
            entry = ""
            entryActive = true
        }

        if (entry == "0") {
            entry = ""
        }

        entry += digit
        // Once an exponent ("e") has been typed, its digits are entered raw: grouping
        // separators only make sense for the mantissa.
        if (!entry.contains("e")) {
            entry = formatter.formatForDisplay(entry)
        }
        refreshDisplay()
    }

    private fun decimalClicked() {
        if (entryActive && entry.contains("e")) {
            return
        }

        pushHistory()
        if (!entryActive) {
            entry = "0"
            entryActive = true
        }

        if (!entry.contains(decimalSeparator)) {
            entry += decimalSeparator
        }
        refreshDisplay()
    }

    /** Appends the scientific-notation exponent marker, e.g. typing 1 2 3 then this key. */
    fun handleExponent() {
        if (entryActive && entry.contains("e")) {
            return
        }

        pushHistory()
        if (!entryActive) {
            entry = "1"
            entryActive = true
        }
        entry += "e"
        refreshDisplay()
    }

    fun handleEnter() {
        pushHistory()
        if (entryActive) {
            pushEntry()
        } else {
            engine.peek()?.let { engine.push(it) }
        }
        refreshDisplay()
    }

    private fun pushEntry() {
        engine.push(parseEntry() ?: BigDecimal.ZERO)
        entryActive = false
    }

    private fun parseEntry(): BigDecimal? = try {
        entry.removeGroupSeparator().toBigDecimal()
    } catch (_: NumberFormatException) {
        null
    }

    /** Replaces the entire stack with the sum of all its values. */
    fun handleSum() {
        val pendingCount = if (entryActive) 1 else 0
        if (engine.size + pendingCount < 1) {
            return
        }

        pushHistory()
        ensureEntryPushed()
        val values = engine.snapshot()
        val sum = values.fold(BigDecimal.ZERO) { acc, value -> RpnEngine.add(acc, value) }
        engine.clear()
        engine.push(sum)
        recordHistory("Σ(${values.joinToString(", ") { it.format() }})", sum.format())
        refreshDisplay()
    }

    /** Replaces the entire stack with the product of all its values. */
    fun handleProduct() {
        val pendingCount = if (entryActive) 1 else 0
        if (engine.size + pendingCount < 1) {
            return
        }

        pushHistory()
        ensureEntryPushed()
        val values = engine.snapshot()
        val product = values.fold(BigDecimal.ONE) { acc, value -> RpnEngine.multiply(acc, value) }
        engine.clear()
        engine.push(product)
        recordHistory("Π(${values.joinToString(", ") { it.format() }})", product.format())
        refreshDisplay()
    }

    fun handleSwap() {
        val pendingCount = if (entryActive) 1 else 0
        if (engine.size + pendingCount < 2) {
            return
        }

        pushHistory()
        ensureEntryPushed()
        engine.swapTop()
        refreshDisplay()
    }

    fun handleRollUp() {
        val pendingCount = if (entryActive) 1 else 0
        if (engine.size + pendingCount < 2) {
            return
        }

        pushHistory()
        ensureEntryPushed()
        engine.rollUp()
        refreshDisplay()
    }

    fun handleRollDown() {
        val pendingCount = if (entryActive) 1 else 0
        if (engine.size + pendingCount < 2) {
            return
        }

        pushHistory()
        ensureEntryPushed()
        engine.rollDown()
        refreshDisplay()
    }

    /** Loads a constant (e.g. π) or the memory value as the new pending entry, like typing it. */
    private fun handleLoadValue(value: BigDecimal) {
        pushHistory()
        ensureEntryPushed()
        engine.push(value)
        entryActive = false
        refreshDisplay()
    }

    fun handleConstant(value: BigDecimal) = handleLoadValue(value)

    /** Re-enters the X value from just before the last successful operation, like an ANS key. */
    fun handleLastX() = handleLoadValue(lastX)

    fun handleMemoryRecall() = handleLoadValue(engine.memoryValue())

    fun handleMemoryClear() {
        pushHistory()
        engine.memoryClear()
        refreshDisplay()
    }

    fun handleMemoryAdd() {
        val value = currentXValue() ?: return
        pushHistory()
        engine.memoryAdd(value)
        refreshDisplay()
    }

    fun handleMemorySubtract() {
        val value = currentXValue() ?: return
        pushHistory()
        engine.memorySubtract(value)
        refreshDisplay()
    }

    private fun currentXValue(): BigDecimal? = if (entryActive) parseEntry() else engine.peek()

    /** Number of populated registers: X (including a pending entry), Y, 1, 2, ... */
    fun stackCount(): Int = engine.size + if (entryActive) 1 else 0

    /** Registers from X down, each labeled X, Y, 1, 2, ... */
    fun stackSnapshot(): List<Pair<String, String>> {
        val labeled = mutableListOf<Pair<String, String>>()
        var position = 0
        if (entryActive) {
            labeled.add(registerLabel(position) to displayEntry())
            position++
        }
        engine.snapshot().asReversed().forEach { value ->
            labeled.add(registerLabel(position) to value.format())
            position++
        }
        return labeled
    }

    private fun registerLabel(position: Int) = when (position) {
        0 -> "X"
        1 -> "Y"
        else -> (position - 1).toString()
    }

    fun currentAngleUnit(): AngleUnit = angleUnit

    fun handleToggleAngleUnit() {
        angleUnit = angleUnit.next()
    }

    /** Converts X from the current angle unit to the next one, then switches to it. */
    fun handleConvertAngleUnit() {
        pushHistory()
        ensureEntryPushed()
        val nextUnit = angleUnit.next()
        engine.dropTop()?.let { engine.push(RpnEngine.convertAngle(it, angleUnit, nextUnit)) }
        angleUnit = nextUnit
        refreshDisplay()
    }

    /** Directly overwrites the register [position] slots down from X (0 = X, 1 = Y, ...). */
    fun handleEditRegister(position: Int, value: BigDecimal) {
        pushHistory()
        ensureEntryPushed()
        engine.replaceAt(position, value)
        refreshDisplay()
    }

    fun currentDisplayMode(): DisplayMode = displayMode

    fun handleToggleDisplayMode() {
        displayMode = displayMode.next()
        engineeringShift = 0
        refreshDisplay()
    }

    /** Moves the engineering decimal point left (raises the exponent by 3); unbounded. */
    fun handleShiftDecimalLeft() {
        if (displayMode != DisplayMode.ENGINEERING) {
            displayMode = DisplayMode.ENGINEERING
            engineeringShift = 0
        } else {
            engineeringShift += 3
        }
        refreshDisplay()
    }

    /** Moves the engineering decimal point right (lowers the exponent by 3); unbounded. */
    fun handleShiftDecimalRight() {
        if (displayMode != DisplayMode.ENGINEERING) {
            displayMode = DisplayMode.ENGINEERING
            engineeringShift = 0
        } else {
            engineeringShift -= 3
        }
        refreshDisplay()
    }

    fun handleDrop() {
        pushHistory()
        if (entryActive) {
            entry = "0"
            entryActive = false
        } else {
            engine.dropTop()
        }
        refreshDisplay()
    }

    fun handleChs() {
        pushHistory()
        if (entryActive) {
            // Once an exponent has been typed, +/- negates the exponent rather than the
            // significand, matching where the user is actively typing.
            val exponentIndex = entry.indexOf("e")
            entry = if (exponentIndex == -1) {
                negateLeadingSign(entry)
            } else {
                entry.substring(0, exponentIndex + 1) + negateLeadingSign(entry.substring(exponentIndex + 1))
            }
        } else {
            engine.dropTop()?.let { engine.push(RpnEngine.negate(it)) }
        }
        refreshDisplay()
    }

    private fun negateLeadingSign(text: String): String =
        if (text.startsWith("-")) text.substring(1) else "-$text"

    fun handleBackspace() {
        if (!entryActive) {
            return
        }

        pushHistory()
        val dropped = entry.dropLast(1).trimEnd(groupingSeparator.single())
        // A dangling exponent sign with no digits left after it (e.g. "1e-" from deleting the
        // last exponent digit of "1e-2") disappears along with that digit, same as the
        // significand's sign does.
        val newEntry = if (dropped.endsWith("e-")) dropped.dropLast(1) else dropped
        if (newEntry.isEmpty() || newEntry == "-") {
            entry = "0"
            entryActive = false
        } else if (newEntry.contains("e")) {
            entry = newEntry
        } else {
            entry = formatter.formatForDisplay(newEntry)
        }
        refreshDisplay()
    }

    fun handleReset() {
        pushHistory()
        engine.clear()
        entry = "0"
        entryActive = false
        refreshDisplay()
    }

    fun handleOperation(operation: String) {
        val pendingCount = if (entryActive) 1 else 0
        val requiredCount = if (isUnary(operation)) 1 else 2
        if (engine.size + pendingCount < requiredCount) {
            return
        }

        pushHistory()
        ensureEntryPushed()

        if (isUnary(operation)) {
            val operand = engine.peek()
            val outcome = engine.applyUnary(unaryFunction(operation))
            handleOutcome(operation, outcome) { result ->
                if (operand != null) {
                    lastX = operand
                    val formula = when {
                        operation == EXP -> "e^${operand.format()}"
                        operation == POWER10 -> "10^${operand.format()}"
                        isNamedFunction(operation) -> "${symbolFor(operation)}(${operand.format()})"
                        else -> "${operand.format()}${symbolFor(operation)}"
                    }
                    recordHistory(formula, result.format())
                }
            }
        } else {
            val snapshot = engine.snapshot()
            val a = snapshot.getOrNull(snapshot.size - 2)
            val b = snapshot.getOrNull(snapshot.size - 1)
            val outcome = engine.applyBinary(binaryFunction(operation))
            handleOutcome(operation, outcome) { result ->
                if (a != null && b != null) {
                    lastX = b
                    val formula = when (operation) {
                        LOG -> "log_${b.format()}(${a.format()})"
                        XTH_ROOT -> "${b.format()}√(${a.format()})"
                        else -> "${a.format()} ${symbolFor(operation)} ${b.format()}"
                    }
                    recordHistory(formula, result.format())
                }
            }
        }
    }

    private fun ensureEntryPushed() {
        if (entryActive) {
            pushEntry()
        }
    }

    private fun isUnary(operation: String) = operation == ROOT || operation == PERCENT ||
        operation == SQUARE || operation == INVERSE || operation == EXP || operation == POWER10 ||
        operation == FACTORIAL || isNamedFunction(operation)

    private fun isNamedFunction(operation: String) = operation in NAMED_UNARY_FUNCTIONS

    private fun unaryFunction(operation: String): (BigDecimal) -> BigDecimal = when (operation) {
        ROOT -> RpnEngine.Companion::sqrt
        SQUARE -> RpnEngine.Companion::square
        SIN -> { a -> RpnEngine.sin(a, angleUnit) }
        COS -> { a -> RpnEngine.cos(a, angleUnit) }
        TAN -> { a -> RpnEngine.tan(a, angleUnit) }
        ASIN -> { a -> RpnEngine.asin(a, angleUnit) }
        ACOS -> { a -> RpnEngine.acos(a, angleUnit) }
        ATAN -> { a -> RpnEngine.atan(a, angleUnit) }
        SINH -> RpnEngine.Companion::sinh
        COSH -> RpnEngine.Companion::cosh
        TANH -> RpnEngine.Companion::tanh
        ASINH -> RpnEngine.Companion::asinh
        ACOSH -> RpnEngine.Companion::acosh
        ATANH -> RpnEngine.Companion::atanh
        LN -> RpnEngine.Companion::ln
        INVERSE -> RpnEngine.Companion::inverse
        EXP -> RpnEngine.Companion::exp
        LOG10 -> RpnEngine.Companion::log10
        POWER10 -> { a -> RpnEngine.power(BigDecimal.TEN, a) }
        FACTORIAL -> RpnEngine.Companion::factorial
        else -> RpnEngine.Companion::percent
    }

    private fun binaryFunction(operation: String): (BigDecimal, BigDecimal) -> BigDecimal =
        when (operation) {
            PLUS -> RpnEngine.Companion::add
            MINUS -> RpnEngine.Companion::subtract
            MULTIPLY -> RpnEngine.Companion::multiply
            DIVIDE -> RpnEngine.Companion::divide
            MODULUS -> RpnEngine.Companion::modulus
            QUOTIENT -> RpnEngine.Companion::quotient
            LOG -> RpnEngine.Companion::logBase
            NPR -> RpnEngine.Companion::nPr
            NCR -> RpnEngine.Companion::nCr
            XTH_ROOT -> RpnEngine.Companion::xthRoot
            PERCENT_CHANGE -> RpnEngine.Companion::percentChange
            else -> RpnEngine.Companion::power
        }

    private fun symbolFor(operation: String) = when (operation) {
        PLUS -> "+"
        MINUS -> "-"
        MULTIPLY -> "×"
        DIVIDE -> "÷"
        MODULUS -> "mod"
        QUOTIENT -> "quot"
        POWER -> "^"
        ROOT -> "√"
        SQUARE -> "²"
        SIN -> "sin"
        COS -> "cos"
        TAN -> "tan"
        ASIN -> "sin⁻¹"
        ACOS -> "cos⁻¹"
        ATAN -> "tan⁻¹"
        SINH -> "sinh"
        COSH -> "cosh"
        TANH -> "tanh"
        ASINH -> "sinh⁻¹"
        ACOSH -> "cosh⁻¹"
        ATANH -> "tanh⁻¹"
        LN -> "ln"
        NPR -> "nPr"
        NCR -> "nCr"
        XTH_ROOT -> "√"
        INVERSE -> "⁻¹"
        LOG10 -> "log10"
        FACTORIAL -> "!"
        PERCENT_CHANGE -> "Δ%"
        else -> "%"
    }

    private fun handleOutcome(
        operation: String,
        outcome: OpOutcome,
        onSuccess: (BigDecimal) -> Unit
    ) {
        when (outcome) {
            is OpOutcome.Success -> {
                entryActive = false
                onSuccess(outcome.value)
            }

            is OpOutcome.Error -> when (outcome.error) {
                RpnError.INSUFFICIENT_STACK -> {}
                RpnError.INVALID_OPERATION -> {
                    val messageRes = if (operation == DIVIDE) {
                        R.string.formula_divide_by_zero_error
                    } else {
                        R.string.error_invalid_operation
                    }
                    context.toast(messageRes)
                }
            }
        }
        refreshDisplay()
    }

    private fun recordHistory(formula: String, result: String) {
        HistoryHelper(context).insertOrUpdateHistoryEntry(
            History(id = null, formula = formula, result = result, timestamp = System.currentTimeMillis())
        )
    }

    fun addNumberToFormula(number: String) {
        handleReset()
        entry = number
        entryActive = true
        refreshDisplay()
    }

    private fun refreshDisplay() {
        val stack = engine.snapshot()
        val stackBelowX = if (entryActive) stack else stack.dropLast(1)
        val stackText = stackBelowX.joinToString("\n") { it.format() }
        callback!!.showNewFormula(stackText, context)

        val xText = if (entryActive) displayEntry() else (engine.peek()?.format() ?: "0")
        callback!!.showNewResult(xText, context)
    }

    /**
     * [entry] as typed, but with an explicit "+" inserted before an as-yet-unsigned exponent -
     * matching how a value is shown once it's actually pushed (e.g. "1.23e+5"), so scientific
     * notation looks the same while typing as it does afterward.
     */
    private fun displayEntry(): String {
        val eIndex = entry.indexOf("e")
        if (eIndex == -1) {
            return entry
        }
        val exponent = entry.substring(eIndex + 1)
        val signedExponent = if (exponent.startsWith("-")) exponent else "+$exponent"
        return entry.substring(0, eIndex) + "e" + signedExponent
    }

    private fun BigDecimal.format() = formatter.bigDecimalToString(this, displayMode, engineeringShift)

    private fun String.removeGroupSeparator() = formatter.removeGroupingSeparator(this)

    fun getCalculatorStateJson(): JSONObject {
        val jsonObj = JSONObject()
        val stackArray = JSONArray()
        engine.snapshot().forEach { stackArray.put(it.toString()) }
        jsonObj.put(STACK, stackArray)
        jsonObj.put(ENTRY, entry)
        jsonObj.put(ENTRY_ACTIVE, entryActive)
        jsonObj.put(MEMORY, engine.memoryValue().toString())
        jsonObj.put(ANGLE_UNIT, angleUnit.name)
        jsonObj.put(DISPLAY_MODE, displayMode.name)
        jsonObj.put(ENGINEERING_SHIFT, engineeringShift)
        jsonObj.put(LAST_X, lastX.toString())
        return jsonObj
    }

    private fun setFromSaveInstanceState(json: String) {
        val jsonObject = JSONTokener(json).nextValue() as JSONObject
        engine.clear()
        val stackArray = jsonObject.optJSONArray(STACK)
        if (stackArray != null) {
            for (i in 0 until stackArray.length()) {
                try {
                    engine.push(BigDecimal(stackArray.getString(i)))
                } catch (_: Exception) {
                    // skip malformed entries
                }
            }
        }
        entry = jsonObject.optString(ENTRY, "0")
        entryActive = jsonObject.optBoolean(ENTRY_ACTIVE, false)
        try {
            engine.setMemoryValue(BigDecimal(jsonObject.optString(MEMORY, "0")))
        } catch (_: NumberFormatException) {
            // keep the default (zero) memory value
        }
        try {
            angleUnit = AngleUnit.valueOf(jsonObject.optString(ANGLE_UNIT, AngleUnit.DEG.name))
        } catch (_: IllegalArgumentException) {
            angleUnit = AngleUnit.DEG
        }
        try {
            displayMode = DisplayMode.valueOf(jsonObject.optString(DISPLAY_MODE, DisplayMode.NORMAL.name))
        } catch (_: IllegalArgumentException) {
            displayMode = DisplayMode.NORMAL
        }
        engineeringShift = jsonObject.optInt(ENGINEERING_SHIFT, 0)
        try {
            lastX = BigDecimal(jsonObject.optString(LAST_X, "0"))
        } catch (_: NumberFormatException) {
            lastX = BigDecimal.ZERO
        }
    }
}
