package com.example.data

import androidx.compose.runtime.Immutable

/**
 * High-performance immutable data model representing a single candlestick tick/bar.
 * Uses primitive Float/Long fields to minimize memory overhead and CPU cache misses.
 */
@Immutable
data class CandleData(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float,
    val sma20: Float = Float.NaN,
    val ema50: Float = Float.NaN,
    val bollingerUpper: Float = Float.NaN,
    val bollingerLower: Float = Float.NaN,
    val rsi14: Float = Float.NaN
) {
    val isBullish: Boolean inline get() = close >= open
    val isBearish: Boolean inline get() = close < open
}

/**
 * Visual styles for rendering chart series (TradingView style).
 */
enum class ChartStyle(val label: String) {
    CANDLESTICK("Candles"),
    HOLLOW_CANDLE("Hollow"),
    AREA_GLOW("Area"),
    HEIKIN_ASHI("Heikin-Ashi"),
    BARS("OHLC Bars")
}

/**
 * Active drawing modes supported by the chart canvas.
 */
enum class DrawingMode {
    PAN_ZOOM,           // Standard navigation (1-finger pan, 2-finger zoom, tap crosshair)
    HORIZONTAL_LINE,    // Tap to drop/drag horizontal support & resistance level
    TREND_LINE          // Touch-drag to draw a trendline from (TimeA, PriceA) to (TimeB, PriceB)
}

/**
 * Chart timeframe interval for Binance WebSocket and REST streams.
 */
enum class ChartInterval(val code: String, val label: String) {
    ONE_MIN("1m", "1M"),
    FIVE_MIN("5m", "5M"),
    FIFTEEN_MIN("15m", "15M"),
    ONE_HOUR("1h", "1H")
}

/**
 * Available Crypto & Major Forex Trading Pairs.
 */
enum class CryptoSymbol(
    val symbol: String,
    val displayName: String,
    val basePrice: Float,
    val isForex: Boolean = false,
    val pipDigits: Int = 2
) {
    // Crypto Pairs
    BTCUSDT("btcusdt", "BTC / USDT", 88500f, false, 2),
    ETHUSDT("ethusdt", "ETH / USDT", 3250f, false, 2),
    SOLUSDT("solusdt", "SOL / USDT", 195f, false, 2),
    BNBUSDT("bnbusdt", "BNB / USDT", 610f, false, 2),
    LTCUSDT("ltcusdt", "LTC / USDT", 82.50f, false, 2),
    TRXUSDT("trxusdt", "TRX / USDT", 0.2050f, false, 4),
    TONUSDT("tonusdt", "TON / USDT", 5.45f, false, 2),

    // Major Forex Pairs (Synchronized Feed)
    EURUSD("eurusd", "EUR / USD", 1.0850f, true, 4),
    GBPUSD("gbpusd", "GBP / USD", 1.2650f, true, 4),
    USDJPY("usdjpy", "USD / JPY", 154.20f, true, 2),
    AUDUSD("audusd", "AUD / USD", 0.6550f, true, 4),
    USDCAD("usdcad", "USD / CAD", 1.3820f, true, 4),
    USDCHF("usdchf", "USD / CHF", 0.8840f, true, 4)
}
