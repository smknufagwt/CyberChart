package com.example.network

import android.util.Log
import com.example.data.CandleData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-latency Binance WebSocket client with auto-reconnect backoff logic.
 * Emits real-time candlestick updates (`CandleData`) via a shared flow.
 */
class BinanceWebSocketManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null

    private val _candleFlow = MutableSharedFlow<CandleData>(extraBufferCapacity = 64)
    val candleFlow: SharedFlow<CandleData> = _candleFlow.asSharedFlow()

    private val _connectionState = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()

    private var currentSymbol = "btcusdt"
    private var currentInterval = "1m"
    private val isConnecting = AtomicBoolean(false)
    private var reconnectAttempt = 0
    private var shouldReconnect = true

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    fun connect(symbol: String, interval: String = "1m") {
        currentSymbol = symbol.lowercase()
        currentInterval = interval.lowercase()
        shouldReconnect = true
        reconnectAttempt = 0

        disconnectInternal()
        startConnection()
    }

    private fun startConnection() {
        if (!shouldReconnect || isConnecting.getAndSet(true)) return

        val url = "wss://stream.binance.com:9443/ws/${currentSymbol}@kline_${currentInterval}"
        Log.d("BinanceWS", "Connecting to Binance WS: $url")

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("BinanceWS", "WebSocket Opened for $currentSymbol")
                isConnecting.set(false)
                reconnectAttempt = 0
                _connectionState.tryEmit(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseKlineMessage(text)?.let { candle ->
                    _candleFlow.tryEmit(candle)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("BinanceWS", "WebSocket Failure: ${t.message}")
                isConnecting.set(false)
                _connectionState.tryEmit(false)
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("BinanceWS", "WebSocket Closed: $reason")
                isConnecting.set(false)
                _connectionState.tryEmit(false)
                scheduleReconnect()
            }
        })
    }

    private val mapAdapter = moshi.adapter(Map::class.java)

    /**
     * Fast string extraction with Moshi structured fallback to handle any Binance WebSocket payload variation.
     */
    private fun parseKlineMessage(json: String): CandleData? {
        try {
            if (!json.contains("\"e\":\"kline\"")) return null

            val kIndex = json.indexOf("\"k\":{")
            if (kIndex != -1) {
                val kObj = json.substring(kIndex + 4)
                val t = extractJsonLong(kObj, "\"t\":")
                val o = extractJsonFloat(kObj, "\"o\":\"")
                val h = extractJsonFloat(kObj, "\"h\":\"")
                val l = extractJsonFloat(kObj, "\"l\":\"")
                val c = extractJsonFloat(kObj, "\"c\":\"")
                val v = extractJsonFloat(kObj, "\"v\":\"") ?: 0f

                if (t != null && o != null && h != null && l != null && c != null) {
                    return CandleData(
                        timestamp = t,
                        open = o,
                        high = h,
                        low = l,
                        close = c,
                        volume = v
                    )
                }
            }

            // Robust fallback via Moshi adapter
            val map = mapAdapter.fromJson(json) as? Map<*, *>
            val kMap = map?.get("k") as? Map<*, *> ?: return null
            val t = (kMap["t"] as? Number)?.toLong() ?: return null
            val o = (kMap["o"] as? String)?.toFloatOrNull() ?: return null
            val h = (kMap["h"] as? String)?.toFloatOrNull() ?: return null
            val l = (kMap["l"] as? String)?.toFloatOrNull() ?: return null
            val c = (kMap["c"] as? String)?.toFloatOrNull() ?: return null
            val v = (kMap["v"] as? String)?.toFloatOrNull() ?: 0f

            return CandleData(
                timestamp = t,
                open = o,
                high = h,
                low = l,
                close = c,
                volume = v
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun extractJsonFloat(src: String, key: String): Float? {
        val idx = src.indexOf(key)
        if (idx == -1) return null
        val start = idx + key.length
        val end = src.indexOf('"', start)
        if (end == -1) return null
        return src.substring(start, end).toFloatOrNull()
    }

    private fun extractJsonLong(src: String, key: String): Long? {
        val idx = src.indexOf(key)
        if (idx == -1) return null
        val start = idx + key.length
        var end = src.indexOf(',', start)
        if (end == -1) end = src.indexOf('}', start)
        if (end == -1) return null
        return src.substring(start, end).trim().toLongOrNull()
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        reconnectAttempt++
        val backoffDelay = (1000L * (1 shl minOf(reconnectAttempt, 5))).coerceAtMost(30_000L)
        Log.d("BinanceWS", "Scheduling WS reconnect in ${backoffDelay}ms (attempt $reconnectAttempt)")
        scope.launch {
            delay(backoffDelay)
            if (shouldReconnect) {
                startConnection()
            }
        }
    }

    fun disconnect() {
        shouldReconnect = false
        disconnectInternal()
    }

    private fun disconnectInternal() {
        webSocket?.close(1000, "Normal Close")
        webSocket = null
        isConnecting.set(false)
        _connectionState.tryEmit(false)
    }
}
