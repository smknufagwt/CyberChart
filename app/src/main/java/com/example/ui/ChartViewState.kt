package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.data.CandleData
import com.example.data.ChartInterval
import com.example.data.CryptoSymbol
import com.example.data.DrawingElement
import com.example.data.DrawingMode

import com.example.data.ChartStyle
import com.example.data.EconomicEvent
import com.example.data.PriceAlert

@Immutable
data class MacroData(
    val fearAndGreedValue: Int = 74,
    val fearAndGreedClassification: String = "Greed",
    val goldPrice: Float = 2685.50f,
    val goldChangePct: Float = 0.42f,
    val dxyIndex: Float = 104.18f,
    val dxyChangePct: Float = -0.15f,
    val us2yYield: Float = 4.18f,
    val us2yChange: Float = -0.03f,
    val us10yYield: Float = 4.38f,
    val us10yChange: Float = -0.02f
)

@Immutable
data class LiquidityConcept(
    val bias: String = "BUY-SIDE SWEEP", // BUY-SIDE SWEEP, SELL-SIDE SWEEP, BALANCED POOL
    val score: Int = 84, // 0-100 Liquidity Density / Saturation
    val buySideLevel: Float = 0f, // Upper Liquidity Pool Target (BSL)
    val sellSideLevel: Float = 0f, // Lower Liquidity Pool Target (SSL)
    val sweepDistancePct: Float = 0.12f
)

/**
 * Immutable UI state for the financial charting application.
 * Using an immutable wrapper ensures Jetpack Compose recomposition is tightly controlled.
 */
@Immutable
data class ChartViewState(
    val candles: List<CandleData> = emptyList(),
    val symbol: CryptoSymbol = CryptoSymbol.BTCUSDT,
    val interval: ChartInterval = ChartInterval.ONE_MIN,
    val chartStyle: ChartStyle = ChartStyle.CANDLESTICK,
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val drawingMode: DrawingMode = DrawingMode.PAN_ZOOM,
    val drawings: List<DrawingElement> = emptyList(),
    val priceAlerts: List<PriceAlert> = emptyList(),
    val activeAlertBanner: String? = null,
    val economicEvents: List<EconomicEvent> = emptyList(),
    val showCalendarPanel: Boolean = false,
    val showMacroPanel: Boolean = false,
    val showLiquidityOverlay: Boolean = true,
    val macroData: MacroData = MacroData(),
    val liquidityConcept: LiquidityConcept = LiquidityConcept(),
    val showSma20: Boolean = true,
    val showEma50: Boolean = false,
    val showBollingerBands: Boolean = false,
    val showRsi14: Boolean = true,
    val selectedCandleIndex: Int = -1, // Crosshair selection (-1 for none)
    val gasGwei: Int = 18,
    val gasStatus: String = "LOW",
    val statusMessage: String = "INITIALIZING ENGINE..."
)
