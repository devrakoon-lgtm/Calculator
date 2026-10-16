package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CalculationItem
import com.example.scanner.ImageMathScanner
import com.example.scanner.ScanResult
import com.example.util.MathScannerParser
import com.example.util.ParsedProblem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CalculatorUiState(
    val currentExpression: String = "",
    val displayValue: String = "0",
    val pendingOperator: Char? = null,
    val liveResultPreview: String? = null,
    val isEvaluated: Boolean = false,
    val history: List<CalculationItem> = emptyList(),
    // Scanner State
    val isScanning: Boolean = false,
    val scanResult: ScanResult? = null,
    val previewBitmap: Bitmap? = null,
    val selectedProblem: ParsedProblem? = null,
    val activeTab: AppTab = AppTab.CALCULATOR
)

enum class AppTab {
    CALCULATOR,
    SCANNER,
    HISTORY
}

class CalculatorViewModel(
    private val scanner: ImageMathScanner = ImageMathScanner()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        // Pre-populate with a couple of nice example entries so the history isn't barren
        _uiState.update { current ->
            current.copy(
                history = listOf(
                    CalculationItem(
                        expression = "150 - 65",
                        result = "85",
                        stepExplanation = listOf(
                            "Subtraction: 150 - 65",
                            "Units column: 0 is less than 5, borrow 1 ten (10 + 0 = 10) - 5 = 5",
                            "Tens column: (15 - 1 borrowed) - 6 = 8",
                            "Final Difference = 85"
                        ),
                        isScanned = true,
                        note = "Sample Worksheet"
                    ),
                    CalculationItem(
                        expression = "28 + 47",
                        result = "75",
                        stepExplanation = listOf(
                            "Addition: 28 + 47",
                            "Units column: 8 + 7 = 15 (carry 1 to tens column)",
                            "Tens column: 2 + 4 + 1 (carry) = 7",
                            "Final Sum = 75"
                        ),
                        isScanned = false
                    )
                )
            )
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // --- Calculator Input Handlers ---

    fun onDigit(digit: String) {
        _uiState.update { state ->
            val newDisplay = if (state.isEvaluated || state.displayValue == "0") {
                digit
            } else {
                state.displayValue + digit
            }

            val newExpr = if (state.isEvaluated) {
                digit
            } else {
                state.currentExpression + digit
            }

            val preview = evaluateLive(newExpr)

            state.copy(
                displayValue = newDisplay,
                currentExpression = newExpr,
                liveResultPreview = preview,
                isEvaluated = false
            )
        }
    }

    fun onDecimal() {
        _uiState.update { state ->
            if (state.isEvaluated) {
                state.copy(
                    displayValue = "0.",
                    currentExpression = "0.",
                    isEvaluated = false
                )
            } else if (!state.displayValue.contains(".")) {
                val newDisp = state.displayValue + "."
                val newExpr = if (state.currentExpression.isEmpty()) "0." else state.currentExpression + "."
                state.copy(
                    displayValue = newDisp,
                    currentExpression = newExpr,
                    liveResultPreview = evaluateLive(newExpr)
                )
            } else {
                state
            }
        }
    }

    fun onOperator(op: Char) {
        _uiState.update { state ->
            val expr = state.currentExpression.trim()
            if (expr.isEmpty()) {
                val base = state.displayValue.ifEmpty { "0" }
                state.copy(
                    currentExpression = "$base $op ",
                    pendingOperator = op,
                    isEvaluated = false
                )
            } else if (expr.endsWith("+") || expr.endsWith("-")) {
                // Replace previous operator
                val replaced = expr.dropLast(1).trim() + " $op "
                state.copy(
                    currentExpression = replaced,
                    pendingOperator = op,
                    isEvaluated = false
                )
            } else {
                val newExpr = "$expr $op "
                state.copy(
                    currentExpression = newExpr,
                    pendingOperator = op,
                    displayValue = "0",
                    isEvaluated = false,
                    liveResultPreview = evaluateLive(newExpr.trim())
                )
            }
        }
    }

    fun onEquals() {
        _uiState.update { state ->
            val expr = state.currentExpression.trim()
            if (expr.isEmpty()) return@update state

            val parsed = MathScannerParser.parseSingleExpression(expr)
            if (parsed != null) {
                val historyItem = CalculationItem(
                    expression = parsed.cleanedExpression,
                    result = parsed.formattedResult,
                    stepExplanation = parsed.steps,
                    isScanned = false
                )

                state.copy(
                    displayValue = parsed.formattedResult,
                    currentExpression = parsed.cleanedExpression,
                    liveResultPreview = null,
                    isEvaluated = true,
                    history = listOf(historyItem) + state.history
                )
            } else {
                state
            }
        }
    }

    fun onClear() {
        _uiState.update { state ->
            state.copy(
                currentExpression = "",
                displayValue = "0",
                pendingOperator = null,
                liveResultPreview = null,
                isEvaluated = false
            )
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            if (state.isEvaluated) {
                return@update state.copy(currentExpression = "", displayValue = "0", isEvaluated = false)
            }
            if (state.displayValue.length > 1) {
                val newDisp = state.displayValue.dropLast(1)
                val newExpr = if (state.currentExpression.isNotEmpty()) state.currentExpression.dropLast(1) else ""
                state.copy(
                    displayValue = newDisp,
                    currentExpression = newExpr,
                    liveResultPreview = evaluateLive(newExpr.trim())
                )
            } else {
                state.copy(
                    displayValue = "0",
                    currentExpression = if (state.currentExpression.isNotEmpty()) state.currentExpression.dropLast(1) else "",
                    liveResultPreview = null
                )
            }
        }
    }

    fun onToggleSign() {
        _uiState.update { state ->
            try {
                val value = BigDecimal(state.displayValue)
                val negated = value.negate()
                val formatted = MathScannerParser.formatNumber(negated)
                state.copy(
                    displayValue = formatted,
                    currentExpression = formatted
                )
            } catch (_: Exception) {
                state
            }
        }
    }

    private fun evaluateLive(expression: String): String? {
        val trimmed = expression.trim()
        val parsed = MathScannerParser.parseSingleExpression(trimmed) ?: return null
        return parsed.formattedResult
    }

    // --- Scanner Actions ---

    fun scanImageBitmap(bitmap: Bitmap, sourceName: String = "Scanned Image") {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, previewBitmap = bitmap) }
            val result = scanner.scanBitmap(bitmap)
            handleScanSuccess(result, sourceName)
        }
    }

    fun scanImageUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val result = scanner.scanUri(context, uri)
            handleScanSuccess(result, "Photo Scan")
        }
    }

    fun scanSampleProblem(expression: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val sampleBitmap = scanner.createSampleProblemBitmap(expression, "Addition & Subtraction")
            _uiState.update { it.copy(previewBitmap = sampleBitmap) }
            val result = scanner.scanBitmap(sampleBitmap)
            handleScanSuccess(result, "Sample: $expression")
        }
    }

    private fun handleScanSuccess(result: ScanResult, sourceName: String) {
        val primary = result.primaryProblem
        val updatedHistory = if (primary != null) {
            val historyItem = CalculationItem(
                expression = primary.cleanedExpression,
                result = primary.formattedResult,
                stepExplanation = primary.steps,
                isScanned = true,
                note = sourceName
            )
            listOf(historyItem) + _uiState.value.history
        } else {
            _uiState.value.history
        }

        _uiState.update { state ->
            state.copy(
                isScanning = false,
                scanResult = result,
                selectedProblem = primary,
                history = updatedHistory
            )
        }
    }

    fun applyScannedProblemToCalculator(problem: ParsedProblem) {
        _uiState.update { state ->
            state.copy(
                activeTab = AppTab.CALCULATOR,
                currentExpression = problem.cleanedExpression,
                displayValue = problem.formattedResult,
                isEvaluated = true
            )
        }
    }

    fun applyHistoryItemToCalculator(item: CalculationItem) {
        _uiState.update { state ->
            state.copy(
                activeTab = AppTab.CALCULATOR,
                currentExpression = item.expression,
                displayValue = item.result,
                isEvaluated = true
            )
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    fun deleteHistoryItem(id: String) {
        _uiState.update { state ->
            state.copy(history = state.history.filterNot { it.id == id })
        }
    }

    fun clearScanResult() {
        _uiState.update { it.copy(scanResult = null, previewBitmap = null, selectedProblem = null) }
    }
}
