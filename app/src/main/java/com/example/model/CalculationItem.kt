package com.example.model

data class CalculationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val expression: String,
    val result: String,
    val stepExplanation: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isScanned: Boolean = false,
    val note: String? = null
)
