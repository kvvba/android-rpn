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

private data class CalculatorSnapshot(
    val stack: List<BigDecimal>,
    val entry: String,
    val entryActive: Boolean
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
        undoHistory.addLast(CalculatorSnapshot(engine.snapshot(), entry, entryActive))
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
        entry = formatter.formatForDisplay(entry)
        refreshDisplay()
    }

    private fun decimalClicked() {
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
            entry = if (entry.startsWith("-")) entry.substring(1) else "-$entry"
        } else {
            engine.dropTop()?.let { engine.push(RpnEngine.negate(it)) }
        }
        refreshDisplay()
    }

    fun handleBackspace() {
        if (!entryActive) {
            return
        }

        pushHistory()
        val newEntry = entry.dropLast(1).trimEnd(groupingSeparator.single())
        if (newEntry.isEmpty() || newEntry == "-") {
            entry = "0"
            entryActive = false
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
                    recordHistory("${operand.format()}${symbolFor(operation)}", result.format())
                }
            }
        } else {
            val snapshot = engine.snapshot()
            val a = snapshot.getOrNull(snapshot.size - 2)
            val b = snapshot.getOrNull(snapshot.size - 1)
            val outcome = engine.applyBinary(binaryFunction(operation))
            handleOutcome(operation, outcome) { result ->
                if (a != null && b != null) {
                    recordHistory(
                        "${a.format()} ${symbolFor(operation)} ${b.format()}",
                        result.format()
                    )
                }
            }
        }
    }

    private fun ensureEntryPushed() {
        if (entryActive) {
            pushEntry()
        }
    }

    private fun isUnary(operation: String) = operation == ROOT || operation == PERCENT

    private fun unaryFunction(operation: String): (BigDecimal) -> BigDecimal = when (operation) {
        ROOT -> RpnEngine.Companion::sqrt
        else -> RpnEngine.Companion::percent
    }

    private fun binaryFunction(operation: String): (BigDecimal, BigDecimal) -> BigDecimal =
        when (operation) {
            PLUS -> RpnEngine.Companion::add
            MINUS -> RpnEngine.Companion::subtract
            MULTIPLY -> RpnEngine.Companion::multiply
            DIVIDE -> RpnEngine.Companion::divide
            else -> RpnEngine.Companion::power
        }

    private fun symbolFor(operation: String) = when (operation) {
        PLUS -> "+"
        MINUS -> "-"
        MULTIPLY -> "×"
        DIVIDE -> "÷"
        POWER -> "^"
        ROOT -> "√"
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

        val xText = if (entryActive) entry else (engine.peek()?.format() ?: "0")
        callback!!.showNewResult(xText, context)
    }

    private fun BigDecimal.format() = formatter.bigDecimalToString(this)

    private fun String.removeGroupSeparator() = formatter.removeGroupingSeparator(this)

    fun getCalculatorStateJson(): JSONObject {
        val jsonObj = JSONObject()
        val stackArray = JSONArray()
        engine.snapshot().forEach { stackArray.put(it.toString()) }
        jsonObj.put(STACK, stackArray)
        jsonObj.put(ENTRY, entry)
        jsonObj.put(ENTRY_ACTIVE, entryActive)
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
    }
}
