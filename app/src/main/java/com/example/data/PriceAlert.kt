package com.example.data

import java.util.UUID

enum class AlertCondition {
    ABOVE,
    BELOW
}

data class PriceAlert(
    val id: String = UUID.randomUUID().toString(),
    val symbol: CryptoSymbol,
    val targetPrice: Float,
    val condition: AlertCondition,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val note: String = ""
)
