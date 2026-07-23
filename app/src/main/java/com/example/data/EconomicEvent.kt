package com.example.data

import androidx.compose.runtime.Immutable

enum class ImpactLevel {
    HIGH,   // Red Flag / High Volatility
    MEDIUM, // Orange Flag / Moderate Volatility
    LOW     // Yellow Flag / Low Volatility
}

@Immutable
data class EconomicEvent(
    val id: String,
    val timestamp: Long,
    val currency: String,
    val title: String,
    val impact: ImpactLevel,
    val actual: String,
    val forecast: String,
    val previous: String,
    val isReleased: Boolean = true
)
