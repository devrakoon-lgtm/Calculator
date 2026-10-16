package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.regex.Pattern

data class ParsedProblem(
    val rawText: String,
    val cleanedExpression: String,
    val terms: List<BigDecimal>,
    val operators: List<Char>, // '+' or '-'
    val result: BigDecimal,
    val formattedResult: String,
    val steps: List<String>
)

object MathScannerParser {

    /**
     * Cleans OCR raw string and attempts to extract addition and subtraction problems.
     */
    fun parseMathText(rawInput: String): List<ParsedProblem> {
        val problems = mutableListOf<ParsedProblem>()
        if (rawInput.isBlank()) return problems

        val lines = rawInput.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }

        // First attempt: line by line horizontal expressions
        for (line in lines) {
            val parsed = parseSingleExpression(line)
            if (parsed != null) {
                problems.add(parsed)
            }
        }

        // If no line-by-line problem found, try entire text normalized
        if (problems.isEmpty()) {
            val wholeTextParsed = parseSingleExpression(rawInput.replace("\n", " "))
            if (wholeTextParsed != null) {
                problems.add(wholeTextParsed)
            }
        }

        // If still empty, check for vertical / column math pattern (e.g. Line 1: "48", Line 2: "+ 75", Line 3: "---")
        if (problems.isEmpty() && lines.size >= 2) {
            val verticalParsed = parseVerticalExpression(lines)
            if (verticalParsed != null) {
                problems.add(verticalParsed)
            }
        }

        return problems
    }

    private fun cleanOcrCharacters(input: String): String {
        var text = input
            .replace("—", "-")
            .replace("–", "-")
            .replace("_", "-")
            .replace("−", "-")
            .replace("plus", "+", ignoreCase = true)
            .replace("minus", "-", ignoreCase = true)
            .replace("add", "+", ignoreCase = true)
            .replace("sub", "-", ignoreCase = true)
            .replace("=", " ")
            .replace("?", " ")
            .replace(":", " ")
            .replace(";", " ")

        // Replace common OCR errors like letter 'O' or 'o' between digits or next to operators
        text = text.replace(Regex("(?<=\\d)[Oo](?=\\d)"), "0")
        text = text.replace(Regex("(?<=[+\\-])\\s*[Oo]\\s*(?=\\d)"), "0")
        text = text.replace(Regex("(?<=\\d)\\s*[Oo]\\s*(?=[+\\-])"), "0")

        // Replace lowercase 'l' or uppercase 'I' or '|' when used as 1
        text = text.replace(Regex("(?<=\\d)[lI|](?=\\d)"), "1")
        text = text.replace(Regex("(?<=[+\\-])\\s*[lI|]\\s*(?=\\d)"), "1")
        text = text.replace(Regex("(?<=\\d)\\s*[lI|]\\s*(?=[+\\-])"), "1")

        return text
    }

    fun parseSingleExpression(line: String): ParsedProblem? {
        val cleaned = cleanOcrCharacters(line)

        // Find sequences of (Number) (+ or -) (Number) ...
        // Matches e.g. "45 + 12", "150 - 38", "10 + 20 - 5 + 3"
        val regex = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*([+\\-])\\s*([0-9]+(?:\\.[0-9]+)?)(?:\\s*([+\\-])\\s*([0-9]+(?:\\.[0-9]+)?))*")
        val match = regex.find(cleaned) ?: return null

        val matchedString = match.value

        // Tokenize matched expression
        val numbers = mutableListOf<BigDecimal>()
        val operators = mutableListOf<Char>()

        val tokenMatcher = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)|([+\\-])").matcher(matchedString)
        while (tokenMatcher.find()) {
            val token = tokenMatcher.group()
            if (token == "+" || token == "-") {
                operators.add(token[0])
            } else {
                try {
                    numbers.add(BigDecimal(token))
                } catch (_: Exception) { }
            }
        }

        if (numbers.size < 2 || operators.isEmpty() || operators.size != numbers.size - 1) {
            return null
        }

        // Calculate step by step
        var current = numbers[0]
        val steps = mutableListOf<String>()
        val expressionBuilder = StringBuilder()
        expressionBuilder.append(formatNumber(numbers[0]))

        for (i in operators.indices) {
            val op = operators[i]
            val nextNum = numbers[i + 1]
            expressionBuilder.append(" $op ").append(formatNumber(nextNum))

            val prev = current
            current = if (op == '+') {
                current.add(nextNum)
            } else {
                current.subtract(nextNum)
            }

            // Generate detailed step
            if (numbers.size == 2) {
                // Two term detailed step
                val detailed = generateTwoTermStep(prev, op, nextNum, current)
                steps.addAll(detailed)
            } else {
                steps.add("Step ${i + 1}: ${formatNumber(prev)} $op ${formatNumber(nextNum)} = ${formatNumber(current)}")
            }
        }

        val formattedResult = formatNumber(current)

        return ParsedProblem(
            rawText = line,
            cleanedExpression = expressionBuilder.toString(),
            terms = numbers,
            operators = operators,
            result = current,
            formattedResult = formattedResult,
            steps = steps
        )
    }

    private fun parseVerticalExpression(lines: List<String>): ParsedProblem? {
        // Example: Line 1 = "145", Line 2 = "- 89" or "+ 72"
        for (i in 0 until lines.size - 1) {
            val l1 = cleanOcrCharacters(lines[i]).trim()
            val l2 = cleanOcrCharacters(lines[i + 1]).trim()

            val num1Match = Regex("^[0-9]+(?:\\.[0-9]+)?$").find(l1)
            val num2Match = Regex("^([+\\-])\\s*([0-9]+(?:\\.[0-9]+)?)$").find(l2)

            if (num1Match != null && num2Match != null) {
                val n1Str = num1Match.value
                val op = num2Match.groupValues[1][0]
                val n2Str = num2Match.groupValues[2]

                return parseSingleExpression("$n1Str $op $n2Str")
            }
        }
        return null
    }

    private fun generateTwoTermStep(a: BigDecimal, op: Char, b: BigDecimal, result: BigDecimal): List<String> {
        val steps = mutableListOf<String>()

        // Check if both are integers for column addition / subtraction
        val aIsInt = a.scale() <= 0 || a.stripTrailingZeros().scale() <= 0
        val bIsInt = b.scale() <= 0 || b.stripTrailingZeros().scale() <= 0

        if (aIsInt && bIsInt && a >= BigDecimal.ZERO && b >= BigDecimal.ZERO) {
            val aInt = a.toLong()
            val bInt = b.toLong()
            if (op == '+') {
                steps.add("Addition: ${formatNumber(a)} + ${formatNumber(b)}")
                steps.add("Units column: ${aInt % 10} + ${bInt % 10} = ${(aInt % 10) + (bInt % 10)}" +
                        if (((aInt % 10) + (bInt % 10)) >= 10) " (carry 1 to tens column)" else "")
                if (aInt >= 10 || bInt >= 10) {
                    val tensA = (aInt / 10) % 10
                    val tensB = (bInt / 10) % 10
                    val carry = if (((aInt % 10) + (bInt % 10)) >= 10) 1 else 0
                    steps.add("Tens column: $tensA + $tensB" + (if (carry > 0) " + $carry (carry)" else "") + " = ${tensA + tensB + carry}")
                }
                steps.add("Final Sum = ${formatNumber(result)}")
            } else {
                steps.add("Subtraction: ${formatNumber(a)} - ${formatNumber(b)}")
                val uA = aInt % 10
                val uB = bInt % 10
                if (uA < uB) {
                    steps.add("Units column: $uA is less than $uB, borrow 1 ten (10 + $uA = ${10 + uA}) - $uB = ${(10 + uA) - uB}")
                } else {
                    steps.add("Units column: $uA - $uB = ${uA - uB}")
                }
                if (aInt >= 10 || bInt >= 10) {
                    val tensA = (aInt / 10)
                    val tensB = (bInt / 10)
                    val borrowed = if (uA < uB) 1 else 0
                    steps.add("Tens column: ($tensA - $borrowed borrowed) - $tensB = ${tensA - borrowed - tensB}")
                }
                steps.add("Final Difference = ${formatNumber(result)}")
            }
        } else {
            steps.add("Equation: ${formatNumber(a)} $op ${formatNumber(b)}")
            steps.add("Calculate: ${formatNumber(a)} ${if (op == '+') "plus" else "minus"} ${formatNumber(b)} equals ${formatNumber(result)}")
        }

        return steps
    }

    fun formatNumber(number: BigDecimal): String {
        val stripped = number.stripTrailingZeros()
        return if (stripped.scale() <= 0) {
            stripped.toPlainString()
        } else {
            stripped.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        }
    }
}
