package com.example

import com.example.util.MathScannerParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MathScannerParserTest {

    @Test
    fun testSimpleAddition() {
        val parsed = MathScannerParser.parseSingleExpression("48 + 75")
        assertNotNull(parsed)
        assertEquals("48 + 75", parsed!!.cleanedExpression)
        assertEquals("123", parsed.formattedResult)
        assertTrue(parsed.steps.isNotEmpty())
    }

    @Test
    fun testSimpleSubtraction() {
        val parsed = MathScannerParser.parseSingleExpression("95 - 38")
        assertNotNull(parsed)
        assertEquals("95 - 38", parsed!!.cleanedExpression)
        assertEquals("57", parsed.formattedResult)
        assertTrue(parsed.steps.any { it.contains("borrow", ignoreCase = true) })
    }

    @Test
    fun testCompoundAdditionAndSubtraction() {
        val parsed = MathScannerParser.parseSingleExpression("120 + 45 - 25")
        assertNotNull(parsed)
        assertEquals("140", parsed!!.formattedResult)
    }

    @Test
    fun testOcrArtifactCleaning() {
        // Common OCR noise like 'plus', '=', '?', em-dashes
        val parsed = MathScannerParser.parseSingleExpression("150 — 65 = ?")
        assertNotNull(parsed)
        assertEquals("85", parsed!!.formattedResult)
    }

    @Test
    fun testDecimalAdditionAndSubtraction() {
        val parsed = MathScannerParser.parseSingleExpression("12.5 + 7.5 - 5.0")
        assertNotNull(parsed)
        assertEquals("15", parsed!!.formattedResult)
    }

    @Test
    fun testParseMathTextMultiLine() {
        val rawOcr = "Math Worksheet\nProblem 1:\n48 + 75\nHave a good day!"
        val problems = MathScannerParser.parseMathText(rawOcr)
        assertEquals(1, problems.size)
        assertEquals("123", problems[0].formattedResult)
    }
}
