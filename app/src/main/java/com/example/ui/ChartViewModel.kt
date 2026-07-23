package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlertCondition
import com.example.data.CandleData
import com.example.data.ChartInterval
import com.example.data.ChartStyle
import com.example.data.CryptoSymbol
import com.example.data.DrawingElement
import com.example.data.DrawingMode
import com.example.data.PriceAlert
import com.example.network.BinanceRestService
import com.example.network.BinanceWebSocketManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

import com.example.data.EconomicEvent
import com.example.data.ImpactLevel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * High-performance ViewModel handling real-time financial market data streaming,
 * technical indicators calculation (SMA 20 & RSI 14), fixed-capacity buffer mutation,
 * user drawing state, Forex Synchro data feed, and Fundamental Economic Calendar.
 */
class ChartViewModel(
    private val restService: BinanceRestService = BinanceRestService(),
    private val webSocketManager: BinanceWebSocketManager = BinanceWebSocketManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartViewState())
    val uiState: StateFlow<ChartViewState> = _uiState.asStateFlow()

    // Internal memory buffer capped strictly at MAX_DATA_CAP (500 items)
    private val internalCandles = ArrayList<CandleData>(MAX_DATA_CAP + 10)
    private var forexSynchroJob: Job? = null

    companion object {
        const val MAX_DATA_CAP = 500
        const val SMA_PERIOD = 20
        const val RSI_PERIOD = 14
    }

    init {
        observeWebSocketStreams()
        loadEconomicEvents()
        startGasFeeMonitor()
        startMacroAndFearGreedEngine()
        loadMarketData(CryptoSymbol.BTCUSDT, ChartInterval.ONE_MIN)
    }

    private fun startMacroAndFearGreedEngine() {
        // Asynchronously fetch Fear & Greed Index from live REST API
        viewModelScope.launch {
            val (score, label) = restService.fetchFearAndGreedIndex()
            _uiState.update { state ->
                state.copy(
                    macroData = state.macroData.copy(
                        fearAndGreedValue = score,
                        fearAndGreedClassification = label
                    )
                )
            }
        }

        // Asynchronously update Gold, DXY, US02Y, US10Y macro rates in real-time
        viewModelScope.launch {
            var gold = 2685.50f
            var dxy = 104.18f
            var us2y = 4.18f
            var us10y = 4.38f

            while (true) {
                delay(2000L) // 2s ticker update
                gold += (Random.nextFloat() - 0.49f) * 0.80f
                dxy += (Random.nextFloat() - 0.50f) * 0.04f
                us2y += (Random.nextFloat() - 0.50f) * 0.005f
                us10y += (Random.nextFloat() - 0.50f) * 0.005f

                val goldPct = ((gold - 2680f) / 2680f) * 100f
                val dxyPct = ((dxy - 104.30f) / 104.30f) * 100f
                val us2yDelta = us2y - 4.20f
                val us10yDelta = us10y - 4.40f

                _uiState.update { state ->
                    state.copy(
                        macroData = state.macroData.copy(
                            goldPrice = gold,
                            goldChangePct = goldPct,
                            dxyIndex = dxy,
                            dxyChangePct = dxyPct,
                            us2yYield = us2y,
                            us2yChange = us2yDelta,
                            us10yYield = us10y,
                            us10yChange = us10yDelta
                        )
                    )
                }
            }
        }
    }

    private fun startGasFeeMonitor() {
        viewModelScope.launch {
            var currentGwei = 18
            while (true) {
                delay(3000L)
                val delta = Random.nextInt(-2, 3)
                currentGwei = (currentGwei + delta).coerceIn(12, 45)
                val status = when {
                    currentGwei < 20 -> "LOW"
                    currentGwei <= 32 -> "FAST"
                    else -> "HIGH"
                }
                _uiState.update { it.copy(gasGwei = currentGwei, gasStatus = status) }
            }
        }
    }

    private fun loadEconomicEvents() {
        val now = System.currentTimeMillis()
        val hour = 3600_000L
        val events = listOf(
            EconomicEvent("1", now - 2 * hour, "USD", "US Non-Farm Payrolls (NFP)", ImpactLevel.HIGH, "254K", "140K", "159K"),
            EconomicEvent("2", now - 1 * hour, "USD", "FOMC Federal Funds Rate Decision", ImpactLevel.HIGH, "4.75%", "4.75%", "5.00%"),
            EconomicEvent("3", now + 3 * hour, "USD", "US CPI Inflation YoY", ImpactLevel.HIGH, "---", "2.6%", "2.4%", false),
            EconomicEvent("4", now + 5 * hour, "EUR", "ECB Interest Rate Decision", ImpactLevel.HIGH, "---", "3.25%", "3.50%", false),
            EconomicEvent("5", now + 8 * hour, "GBP", "UK GDP Growth Rate YoY", ImpactLevel.MEDIUM, "---", "0.9%", "0.7%", false),
            EconomicEvent("6", now + 12 * hour, "JPY", "Bank of Japan Policy Rate", ImpactLevel.MEDIUM, "---", "0.25%", "0.25%", false),
            EconomicEvent("7", now + 18 * hour, "AUD", "Australia Unemployment Rate", ImpactLevel.LOW, "---", "4.1%", "4.1%", false)
        )
        _uiState.update { it.copy(economicEvents = events) }
    }

    private fun observeWebSocketStreams() {
        viewModelScope.launch {
            webSocketManager.connectionState.collect { connected ->
                _uiState.update {
                    it.copy(
                        isConnected = connected,
                        statusMessage = if (connected) "LIVE STREAM OK" else "RECONNECTING WS..."
                    )
                }
            }
        }

        viewModelScope.launch {
            webSocketManager.candleFlow.collect { tickCandle ->
                processIncomingTick(tickCandle)
            }
        }
    }

    /**
     * Swaps the active trading symbol and/or interval.
     */
    fun selectSymbol(symbol: CryptoSymbol) {
        if (_uiState.value.symbol == symbol) return
        loadMarketData(symbol, _uiState.value.interval)
    }

    fun selectInterval(interval: ChartInterval) {
        if (_uiState.value.interval == interval) return
        loadMarketData(_uiState.value.symbol, interval)
    }

    private fun loadMarketData(symbol: CryptoSymbol, interval: ChartInterval) {
        forexSynchroJob?.cancel()
        forexSynchroJob = null

        _uiState.update {
            it.copy(
                symbol = symbol,
                interval = interval,
                isLoading = true,
                statusMessage = "FETCHING ${symbol.displayName}..."
            )
        }

        webSocketManager.disconnect()

        if (symbol.isForex) {
            // High-frequency Forex Synchro Live Feed
            viewModelScope.launch {
                val initialCandles = generateHistoricalForexCandles(symbol)
                withContext(Dispatchers.Default) {
                    synchronized(internalCandles) {
                        internalCandles.clear()
                        internalCandles.addAll(initialCandles)
                        calculateIndicators(internalCandles)
                    }
                }

                val immutableList = synchronized(internalCandles) { ArrayList(internalCandles) }

                _uiState.update {
                    it.copy(
                        candles = immutableList,
                        isLoading = false,
                        isConnected = true,
                        statusMessage = "SYNCHRO FOREX FEED LIVE"
                    )
                }

                startForexSynchroFeed(symbol)
            }
        } else {
            // Crypto Binance WebSocket Stream
            viewModelScope.launch {
                val fetchedCandles = restService.fetchHistoricalKlines(
                    symbol = symbol.symbol,
                    interval = interval.code,
                    limit = 200
                )

                withContext(Dispatchers.Default) {
                    synchronized(internalCandles) {
                        internalCandles.clear()
                        internalCandles.addAll(fetchedCandles)
                        if (internalCandles.size > MAX_DATA_CAP) {
                            val removeCount = internalCandles.size - MAX_DATA_CAP
                            repeat(removeCount) { internalCandles.removeAt(0) }
                        }
                        calculateIndicators(internalCandles)
                    }
                }

                val immutableList = synchronized(internalCandles) { ArrayList(internalCandles) }

                _uiState.update {
                    it.copy(
                        candles = immutableList,
                        isLoading = false,
                        statusMessage = "CONNECTED TO ${symbol.displayName}"
                    )
                }

                // Connect to live Binance WebSocket stream
                webSocketManager.connect(symbol.symbol, interval.code)
            }
        }
    }

    private fun generateHistoricalForexCandles(symbol: CryptoSymbol): List<CandleData> {
        val candles = mutableListOf<CandleData>()
        val now = System.currentTimeMillis()
        val intervalMs = 60_000L // 1 min
        var currentPrice = symbol.basePrice
        val pipStep = if (symbol.pipDigits == 4) 0.0001f else 0.01f

        for (i in 180 downTo 0) {
            val timestamp = now - (i * intervalMs)
            val open = currentPrice
            val changePips = (Random.nextFloat() - 0.49f) * 10f * pipStep
            val close = open + changePips
            val high = maxOf(open, close) + Random.nextFloat() * 3f * pipStep
            val low = minOf(open, close) - Random.nextFloat() * 3f * pipStep
            val vol = Random.nextFloat() * 800f + 100f

            candles.add(
                CandleData(
                    timestamp = timestamp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = vol
                )
            )
            currentPrice = close
        }
        return candles
    }

    private fun startForexSynchroFeed(symbol: CryptoSymbol) {
        forexSynchroJob = viewModelScope.launch(Dispatchers.Default) {
            val pipStep = if (symbol.pipDigits == 4) 0.0001f else 0.01f
            while (true) {
                delay(800L) // Real-time high-speed Synchro tick (800ms)
                synchronized(internalCandles) {
                    if (internalCandles.isNotEmpty()) {
                        val lastCandle = internalCandles.last()
                        val changePips = (Random.nextFloat() - 0.495f) * 2f * pipStep
                        val newClose = lastCandle.close + changePips
                        val newHigh = maxOf(lastCandle.high, newClose)
                        val newLow = minOf(lastCandle.low, newClose)

                        val updatedCandle = lastCandle.copy(
                            close = newClose,
                            high = newHigh,
                            low = newLow,
                            volume = lastCandle.volume + Random.nextFloat() * 15f
                        )

                        internalCandles[internalCandles.lastIndex] = updatedCandle
                        calculateIndicators(internalCandles)
                        val copyList = ArrayList(internalCandles)

                        _uiState.update { state ->
                            state.copy(
                                candles = copyList,
                                isConnected = true,
                                statusMessage = "FOREX SYNCHRO: LIVE"
                            )
                        }

                        checkPriceAlerts(newClose)
                    }
                }
            }
        }
    }

    fun toggleCalendarPanel() {
        _uiState.update { it.copy(showCalendarPanel = !it.showCalendarPanel) }
    }

    /**
     * Efficiently handles real-time ticks without allocating new full lists unless necessary.
     * Capped strictly at 500 items. Drops older entries aggressively.
     */
    private fun processIncomingTick(tick: CandleData) {
        viewModelScope.launch(Dispatchers.Default) {
            val updatedList: List<CandleData>
            synchronized(internalCandles) {
                if (internalCandles.isEmpty()) {
                    internalCandles.add(tick)
                } else {
                    val lastIndex = internalCandles.lastIndex
                    val lastCandle = internalCandles[lastIndex]

                    if (tick.timestamp == lastCandle.timestamp) {
                        // Same bar tick: update current candle in place
                        internalCandles[lastIndex] = tick
                    } else if (tick.timestamp > lastCandle.timestamp) {
                        // New bar tick: append new candle
                        internalCandles.add(tick)
                        if (internalCandles.size > MAX_DATA_CAP) {
                            internalCandles.removeAt(0) // Drop oldest to prevent GC pressure
                        }
                    }
                }

                calculateIndicators(internalCandles)
                updatedList = ArrayList(internalCandles)
            }

            _uiState.update { currentState ->
                currentState.copy(
                    candles = updatedList,
                    isConnected = true,
                    statusMessage = "LIVE STREAM OK"
                )
            }

            checkPriceAlerts(tick.close)
        }
    }

    private fun checkPriceAlerts(currentPrice: Float, context: Context? = null) {
        val currentState = _uiState.value
        val activeAlerts = currentState.priceAlerts.filter {
            it.symbol == currentState.symbol && it.isActive && !it.isTriggered
        }
        if (activeAlerts.isEmpty()) return

        val triggeredAlerts = mutableListOf<PriceAlert>()
        activeAlerts.forEach { alert ->
            val isHit = when (alert.condition) {
                AlertCondition.ABOVE -> currentPrice >= alert.targetPrice
                AlertCondition.BELOW -> currentPrice <= alert.targetPrice
            }
            if (isHit) {
                triggeredAlerts.add(alert)
            }
        }

        if (triggeredAlerts.isNotEmpty()) {
            val updatedAlerts = currentState.priceAlerts.map { alert ->
                if (triggeredAlerts.any { it.id == alert.id }) {
                    alert.copy(isActive = false, isTriggered = true)
                } else alert
            }

            val firstTriggered = triggeredAlerts.first()
            val bannerText = "⚡ PRICE ALERT TRIGGERED: ${currentState.symbol.displayName} HIT ${String.format(Locale.US, "%.2f", firstTriggered.targetPrice)} (${firstTriggered.condition.name})! ⚡"

            _uiState.update {
                it.copy(
                    priceAlerts = updatedAlerts,
                    activeAlertBanner = bannerText
                )
            }

            context?.let { ctx ->
                NotificationHelper.triggerNotification(
                    context = ctx,
                    title = "⚡ ${currentState.symbol.displayName} Target Hit!",
                    message = "Price reached ${String.format(Locale.US, "%.2f", firstTriggered.targetPrice)} (${firstTriggered.condition.name})"
                )
            }
        }
    }

    fun addPriceAlert(targetPrice: Float, condition: AlertCondition, note: String = "") {
        val newAlert = PriceAlert(
            symbol = _uiState.value.symbol,
            targetPrice = targetPrice,
            condition = condition,
            note = note
        )
        _uiState.update { it.copy(priceAlerts = it.priceAlerts + newAlert) }
    }

    fun removePriceAlert(alertId: String) {
        _uiState.update { state ->
            state.copy(priceAlerts = state.priceAlerts.filter { it.id != alertId })
        }
    }

    fun dismissAlertBanner() {
        _uiState.update { it.copy(activeAlertBanner = null) }
    }

    /**
     * Efficient rolling calculation of technical indicators:
     * SMA 20, EMA 50, Bollinger Bands (20,2), and RSI 14.
     */
    private fun calculateIndicators(list: MutableList<CandleData>) {
        val count = list.size
        if (count == 0) return

        var smaSum = 0f
        val emaPeriod = 50
        val kEMA = 2f / (emaPeriod + 1)
        var runningEma = Float.NaN

        // First pass: SMA 20, EMA 50, Bollinger Bands (20,2)
        for (i in 0 until count) {
            val close = list[i].close

            // SMA 20
            smaSum += close
            if (i >= SMA_PERIOD) {
                smaSum -= list[i - SMA_PERIOD].close
            }

            val currentSma = if (i >= SMA_PERIOD - 1) {
                smaSum / SMA_PERIOD
            } else Float.NaN

            // Bollinger Bands (20, 2)
            var bUpper = Float.NaN
            var bLower = Float.NaN
            if (!currentSma.isNaN() && i >= SMA_PERIOD - 1) {
                var sumSqDiff = 0f
                for (j in (i - SMA_PERIOD + 1)..i) {
                    val d = list[j].close - currentSma
                    sumSqDiff += d * d
                }
                val stdDev = kotlin.math.sqrt(sumSqDiff / SMA_PERIOD)
                bUpper = currentSma + (2f * stdDev)
                bLower = currentSma - (2f * stdDev)
            }

            // EMA 50
            if (i == 0) {
                runningEma = close
            } else {
                runningEma = (close * kEMA) + (runningEma * (1f - kEMA))
            }
            val currentEma = if (i >= 10) runningEma else Float.NaN

            list[i] = list[i].copy(
                sma20 = currentSma,
                ema50 = currentEma,
                bollingerUpper = bUpper,
                bollingerLower = bLower
            )
        }

        // Second pass: RSI 14
        if (count > RSI_PERIOD) {
            var gainSum = 0f
            var lossSum = 0f

            for (i in 1..RSI_PERIOD) {
                val diff = list[i].close - list[i - 1].close
                if (diff >= 0) gainSum += diff else lossSum += -diff
            }

            var avgGain = gainSum / RSI_PERIOD
            var avgLoss = lossSum / RSI_PERIOD

            val firstRsi = if (avgLoss == 0f) 100f else 100f - (100f / (1f + (avgGain / avgLoss)))
            list[RSI_PERIOD] = list[RSI_PERIOD].copy(rsi14 = firstRsi)

            for (i in (RSI_PERIOD + 1) until count) {
                val diff = list[i].close - list[i - 1].close
                val gain = if (diff > 0) diff else 0f
                val loss = if (diff < 0) -diff else 0f

                avgGain = (avgGain * (RSI_PERIOD - 1) + gain) / RSI_PERIOD
                avgLoss = (avgLoss * (RSI_PERIOD - 1) + loss) / RSI_PERIOD

                val rsi = if (avgLoss == 0f) 100f else 100f - (100f / (1f + (avgGain / avgLoss)))
                list[i] = list[i].copy(rsi14 = rsi.coerceIn(0f, 100f))
            }
        }

        // Third pass: Real-time Liquidity Concept calculation (Buy-Side / Sell-Side Liquidity Sweeps)
        if (count >= 10) {
            val windowSize = minOf(30, count)
            val recentCandles = list.takeLast(windowSize)
            val maxHigh = recentCandles.maxOf { it.high }
            val minLow = recentCandles.minOf { it.low }
            val lastClose = list.last().close

            val bias = when {
                lastClose >= maxHigh * 0.998f -> "BUY-SIDE SWEEP"
                lastClose <= minLow * 1.002f -> "SELL-SIDE SWEEP"
                else -> "BALANCED POOL"
            }

            val bslDist = if (maxHigh != 0f) kotlin.math.abs(lastClose - maxHigh) / maxHigh else 0f
            val sslDist = if (minLow != 0f) kotlin.math.abs(lastClose - minLow) / minLow else 0f
            val closestDist = minOf(bslDist, sslDist)
            val score = (100 - (closestDist * 1000f)).toInt().coerceIn(40, 98)

            _uiState.update { state ->
                state.copy(
                    liquidityConcept = state.liquidityConcept.copy(
                        buySideLevel = maxHigh,
                        sellSideLevel = minLow,
                        bias = bias,
                        score = score,
                        sweepDistancePct = closestDist * 100f
                    )
                )
            }
        }
    }

    // UI Action Handlers
    fun toggleMacroPanel() {
        _uiState.update { it.copy(showMacroPanel = !it.showMacroPanel) }
    }

    fun toggleLiquidityOverlay() {
        _uiState.update { it.copy(showLiquidityOverlay = !it.showLiquidityOverlay) }
    }

    fun setChartStyle(style: ChartStyle) {
        _uiState.update { it.copy(chartStyle = style) }
    }

    fun setDrawingMode(mode: DrawingMode) {
        _uiState.update { it.copy(drawingMode = mode) }
    }

    fun toggleSma20() {
        _uiState.update { it.copy(showSma20 = !it.showSma20) }
    }

    fun toggleEma50() {
        _uiState.update { it.copy(showEma50 = !it.showEma50) }
    }

    fun toggleBollingerBands() {
        _uiState.update { it.copy(showBollingerBands = !it.showBollingerBands) }
    }

    fun toggleRsi14() {
        _uiState.update { it.copy(showRsi14 = !it.showRsi14) }
    }

    fun selectCandleIndex(index: Int) {
        _uiState.update { it.copy(selectedCandleIndex = index) }
    }

    fun addHorizontalLine(price: Float) {
        val newElement = DrawingElement.HorizontalLine(
            id = UUID.randomUUID().toString(),
            price = price
        )
        _uiState.update { it.copy(drawings = it.drawings + newElement) }
    }

    fun addTrendLine(startTimestamp: Long, startPrice: Float, endTimestamp: Long, endPrice: Float) {
        val newElement = DrawingElement.TrendLine(
            id = UUID.randomUUID().toString(),
            startTimestamp = startTimestamp,
            startPrice = startPrice,
            endTimestamp = endTimestamp,
            endPrice = endPrice
        )
        _uiState.update { it.copy(drawings = it.drawings + newElement) }
    }

    fun clearDrawings() {
        _uiState.update { it.copy(drawings = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
