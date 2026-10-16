package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import com.example.util.MathScannerParser
import com.example.util.ParsedProblem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ScanResult(
    val rawDetectedText: String,
    val problems: List<ParsedProblem>,
    val primaryProblem: ParsedProblem?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class ImageMathScanner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun scanBitmap(bitmap: Bitmap): ScanResult {
        return suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val raw = visionText.text
                    val parsed = MathScannerParser.parseMathText(raw)
                    val result = ScanResult(
                        rawDetectedText = raw,
                        problems = parsed,
                        primaryProblem = parsed.firstOrNull(),
                        isSuccess = parsed.isNotEmpty(),
                        errorMessage = if (parsed.isEmpty()) {
                            if (raw.isBlank()) "No text detected in the image. Please point clearly at an addition or subtraction problem."
                            else "Detected: \"$raw\", but couldn't find a valid addition/subtraction problem."
                        } else null
                    )
                    continuation.resume(result)
                }
                .addOnFailureListener { exception ->
                    continuation.resume(
                        ScanResult(
                            rawDetectedText = "",
                            problems = emptyList(),
                            primaryProblem = null,
                            isSuccess = false,
                            errorMessage = exception.localizedMessage ?: "Failed to process image"
                        )
                    )
                }
        }
    }

    suspend fun scanUri(context: Context, uri: Uri): ScanResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val raw = visionText.text
                        val parsed = MathScannerParser.parseMathText(raw)
                        val result = ScanResult(
                            rawDetectedText = raw,
                            problems = parsed,
                            primaryProblem = parsed.firstOrNull(),
                            isSuccess = parsed.isNotEmpty(),
                            errorMessage = if (parsed.isEmpty()) {
                                if (raw.isBlank()) "No text detected. Ensure good lighting and legible numbers."
                                else "Detected: \"$raw\". Could not extract a valid addition or subtraction problem."
                            } else null
                        )
                        continuation.resume(result)
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(
                            ScanResult(
                                rawDetectedText = "",
                                problems = emptyList(),
                                primaryProblem = null,
                                isSuccess = false,
                                errorMessage = exception.localizedMessage ?: "Failed to scan image"
                            )
                        )
                    }
            } catch (e: Exception) {
                continuation.resume(
                    ScanResult(
                        rawDetectedText = "",
                        problems = emptyList(),
                        primaryProblem = null,
                        isSuccess = false,
                        errorMessage = "Error reading image file: ${e.message}"
                    )
                )
            }
        }
    }

    /**
     * Generates a realistic simulated paper sheet bitmap with printed or handwritten math problems.
     * Perfect for testing instant scanning right inside the streaming browser emulator!
     */
    fun createSampleProblemBitmap(problemText: String, subtitle: String = "Math Worksheet"): Bitmap {
        val width = 600
        val height = 350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw paper background with faint grid/lines
        val paperPaint = Paint().apply {
            color = Color.parseColor("#FFFDF5") // warm paper
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paperPaint)

        // Draw light blue notebook ruled lines
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E7FF")
            strokeWidth = 2f
        }
        var y = 60f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
            y += 45f
        }

        // Draw red margin line
        val marginPaint = Paint().apply {
            color = Color.parseColor("#FECDD3")
            strokeWidth = 3f
        }
        canvas.drawLine(70f, 0f, 70f, height.toFloat(), marginPaint)

        // Draw header
        val headerPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Exercise: $subtitle", 90f, 45f, headerPaint)

        // Draw math problem
        val mathPaint = Paint().apply {
            color = Color.parseColor("#0F172A") // deep ink black
            textSize = 64f
            isAntiAlias = true
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(problemText, 110f, 180f, mathPaint)

        // Draw answer prompt
        val promptPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText("= ?", 110f + mathPaint.measureText(problemText) + 20f, 180f, promptPaint)

        return bitmap
    }
}
