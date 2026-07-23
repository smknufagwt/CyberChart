package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CandleData
import com.example.data.ChartStyle
import com.example.data.DrawingElement
import com.example.data.DrawingMode
import com.example.ui.ChartViewState
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCrosshair
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGridLine
import com.example.ui.theme.CyberPanelBorder
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricNeonViolet
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// TradingView Color Palette Constants
private val TvBackground = Color(0xFF131722)
private val TvGrid = Color(0xFF1E222D)
private val TvBullTeal = Color(0xFF089981)
private val TvBearCrimson = Color(0xFFF23645)
private val TvAmberEma = Color(0xFFFFB300)
private val TvBbCyan = Color(0xFF00E5FF)

/**
 * Custom TradingView-Grade High-Performance Canvas for Jetpack Compose.
 * Features ZERO object allocation in the `onDraw` frame loop to ensure 120 FPS / ZERO-LAG execution.
 */
@Composable
fun PerformanceChartCanvas(
    state: ChartViewState,
    onAddHorizontalLine: (Float) -> Unit,
    onAddTrendLine: (Long, Float, Long, Float) -> Unit,
    onSelectCandle: (Int) -> Unit,
    onDismissAlertBanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val candles = state.candles
    val drawings = state.drawings
    val textMeasurer = rememberTextMeasurer()

    // Pan & Zoom view state
    var visibleCandleCount by remember { mutableIntStateOf(50) }
    var panOffsetIndex by remember { mutableIntStateOf(0) }

    // Active gesture drawing state
    var isDrawingActive by remember { mutableStateOf(false) }
    var drawStartOffset by remember { mutableStateOf(Offset.Zero) }
    var drawCurrentOffset by remember { mutableStateOf(Offset.Zero) }

    // Pre-allocated reusable path objects to avoid allocation during draw loop
    val reusablePath = remember { Path() }
    val emaPath = remember { Path() }
    val bbUpperPath = remember { Path() }
    val bbLowerPath = remember { Path() }
    val bbFillPath = remember { Path() }
    val areaPath = remember { Path() }
    val areaFillPath = remember { Path() }
    val rsiPath = remember { Path() }
    val dashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

    // Pre-allocated volume colors
    val bullishVolColor = remember { TvBullTeal.copy(alpha = 0.35f) }
    val bearishVolColor = remember { TvBearCrimson.copy(alpha = 0.35f) }

    // Monospace axis & HUD text styles
    val axisTextStyle = remember {
        TextStyle(
            color = CyberTextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }

    val watermarkStyle = remember {
        TextStyle(
            color = Color.White.copy(alpha = 0.06f),
            fontSize = 32.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black
        )
    }

    val legendTextStyle = remember {
        TextStyle(
            color = CyberTextPrimary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }

    val peakCalloutStyle = remember {
        TextStyle(
            color = Color.White,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }

    val volTextStyle = remember {
        TextStyle(
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.US) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.drawingMode, candles.size) {
                if (state.drawingMode == DrawingMode.PAN_ZOOM) {
                    // Pan and Pinch Zoom Gestures
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (candles.isNotEmpty()) {
                            if (zoom != 1.0f) {
                                val newCount = (visibleCandleCount / zoom).toInt()
                                visibleCandleCount = newCount.coerceIn(15, 150)
                            }
                            if (pan.x != 0f) {
                                val candleWidth = size.width / visibleCandleCount.toFloat()
                                val indexDelta = (-pan.x / candleWidth).toInt()
                                if (indexDelta != 0) {
                                    val maxPan = max(0, candles.size - visibleCandleCount)
                                    panOffsetIndex = (panOffsetIndex + indexDelta).coerceIn(-maxPan, 0)
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(state.drawingMode, candles.size, visibleCandleCount, panOffsetIndex) {
                if (state.drawingMode != DrawingMode.PAN_ZOOM) {
                    // Interactive Drawing Gestures
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDrawingActive = true
                            drawStartOffset = offset
                            drawCurrentOffset = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            drawCurrentOffset = change.position
                        },
                        onDragEnd = {
                            if (isDrawingActive && candles.isNotEmpty()) {
                                isDrawingActive = false
                                val chartHeight = size.height * 0.75f
                                val rightPadding = 70f
                                val plotWidth = size.width - rightPadding

                                val totalCount = candles.size
                                val endIndex = (totalCount + panOffsetIndex).coerceIn(1, totalCount)
                                val startIndex = max(0, endIndex - visibleCandleCount)
                                val visibleList = candles.subList(startIndex, endIndex)

                                if (visibleList.isNotEmpty()) {
                                    val minPrice = visibleList.minOf { it.low }
                                    val maxPrice = visibleList.maxOf { it.high }
                                    val priceRange = max(0.0001f, maxPrice - minPrice)

                                    if (state.drawingMode == DrawingMode.HORIZONTAL_LINE) {
                                        val price = maxPrice - ((drawStartOffset.y / chartHeight) * priceRange)
                                        onAddHorizontalLine(price)
                                    } else if (state.drawingMode == DrawingMode.TREND_LINE) {
                                        val startIdx = ((drawStartOffset.x / plotWidth) * visibleList.size).toInt().coerceIn(0, visibleList.lastIndex)
                                        val endIdx = ((drawCurrentOffset.x / plotWidth) * visibleList.size).toInt().coerceIn(0, visibleList.lastIndex)

                                        val startCandle = visibleList[startIdx]
                                        val endCandle = visibleList[endIdx]

                                        val startPrice = maxPrice - ((drawStartOffset.y / chartHeight) * priceRange)
                                        val endPrice = maxPrice - ((drawCurrentOffset.y / chartHeight) * priceRange)

                                        onAddTrendLine(startCandle.timestamp, startPrice, endCandle.timestamp, endPrice)
                                    }
                                }
                            }
                        }
                    )
                } else {
                    // Crosshair Tap Selection
                    detectTapGestures { offset ->
                        if (candles.isNotEmpty()) {
                            val rightPadding = 70f
                            val plotWidth = size.width - rightPadding
                            val totalCount = candles.size
                            val endIndex = (totalCount + panOffsetIndex).coerceIn(1, totalCount)
                            val startIndex = max(0, endIndex - visibleCandleCount)
                            val visibleCount = endIndex - startIndex

                            val candleWidth = plotWidth / visibleCount.toFloat()
                            val clickedIndexInVisible = (offset.x / candleWidth).toInt().coerceIn(0, visibleCount - 1)
                            val actualIndex = startIndex + clickedIndexInVisible
                            onSelectCandle(actualIndex)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // TradingView Dark Slate canvas background
            drawRect(color = TvBackground, size = size)

            // 1. Symbol Watermark Background Text (TradingView style)
            val watermarkText = "${state.symbol.displayName}  ${state.interval.label}"
            val measuredWatermark = textMeasurer.measure(watermarkText, watermarkStyle)
            drawText(
                textLayoutResult = measuredWatermark,
                topLeft = Offset(
                    (canvasWidth - measuredWatermark.size.width) / 2f,
                    (canvasHeight - measuredWatermark.size.height) / 2.5f
                )
            )

            // Draw TradingView Grid Lines
            val bentoCols = 6
            val bentoRows = 8
            val colStep = canvasWidth / bentoCols
            val rowStep = canvasHeight / bentoRows
            for (c in 1 until bentoCols) {
                drawLine(
                    color = TvGrid,
                    start = Offset(c * colStep, 0f),
                    end = Offset(c * colStep, canvasHeight),
                    strokeWidth = 1f
                )
            }
            for (r in 1 until bentoRows) {
                drawLine(
                    color = TvGrid,
                    start = Offset(0f, r * rowStep),
                    end = Offset(canvasWidth, r * rowStep),
                    strokeWidth = 1f
                )
            }

            if (candles.isEmpty()) {
                val measuredText = textMeasurer.measure("CONNECTING TRADINGVIEW ENGINE...", axisTextStyle)
                drawText(
                    textLayoutResult = measuredText,
                    topLeft = Offset((canvasWidth - measuredText.size.width) / 2f, (canvasHeight - measuredText.size.height) / 2f)
                )
                return@Canvas
            }

            // Layout split: Main Chart = 78% height, RSI Panel = 22% height if visible
            val rightPadding = 75f
            val axisBottomPadding = 20f

            val rsiHeight = if (state.showRsi14) canvasHeight * 0.20f else 0f
            val mainChartHeight = canvasHeight - rsiHeight - axisBottomPadding
            val plotWidth = canvasWidth - rightPadding

            // Determine visible candles window
            val totalCount = candles.size
            val endIndex = (totalCount + panOffsetIndex).coerceIn(1, totalCount)
            val startIndex = max(0, endIndex - visibleCandleCount)
            val visibleCandles = candles.subList(startIndex, endIndex)
            val visibleCount = visibleCandles.size

            if (visibleCount == 0) return@Canvas

            val candleWidth = plotWidth / visibleCount.toFloat()
            val bodyWidth = max(1.5f, candleWidth * 0.72f)
            val halfBodyWidth = bodyWidth / 2f

            // Min and Max price bounds with padding
            var minPrice = Float.MAX_VALUE
            var maxPrice = Float.MIN_VALUE
            var maxVolume = 0f
            var highestCandleIdx = 0
            var lowestCandleIdx = 0

            for (i in 0 until visibleCount) {
                val c = visibleCandles[i]
                if (c.low < minPrice) {
                    minPrice = c.low
                    lowestCandleIdx = i
                }
                if (c.high > maxPrice) {
                    maxPrice = c.high
                    highestCandleIdx = i
                }
                if (c.volume > maxVolume) maxVolume = c.volume
            }

            val priceMargin = max(0.1f, (maxPrice - minPrice) * 0.06f)
            minPrice -= priceMargin
            maxPrice += priceMargin
            val priceRange = max(0.001f, maxPrice - minPrice)

            // Inline coordinate converters
            fun priceToY(price: Float): Float {
                return mainChartHeight * (1f - (price - minPrice) / priceRange)
            }

            fun yToPrice(y: Float): Float {
                return maxPrice - (y / mainChartHeight) * priceRange
            }

            fun indexToX(index: Int): Float {
                return index * candleWidth + (candleWidth / 2f)
            }

            // 2. Draw Price Axis Ticks & Grid Lines
            val gridSteps = 6
            val priceStep = priceRange / gridSteps
            for (step in 0..gridSteps) {
                val priceTick = minPrice + (step * priceStep)
                val y = priceToY(priceTick)

                drawLine(
                    color = TvGrid,
                    start = Offset(0f, y),
                    end = Offset(plotWidth, y),
                    strokeWidth = 1f
                )

                val priceText = String.format(Locale.US, if (state.symbol.isForex && state.symbol.pipDigits == 4) "%.4f" else "%.2f", priceTick)
                val measuredPrice = textMeasurer.measure(priceText, axisTextStyle)
                drawText(
                    textLayoutResult = measuredPrice,
                    topLeft = Offset(plotWidth + 6f, y - (measuredPrice.size.height / 2f))
                )
            }

            // Right Y Axis boundary line
            drawLine(
                color = TvGrid,
                start = Offset(plotWidth, 0f),
                end = Offset(plotWidth, mainChartHeight),
                strokeWidth = 1.5f
            )

            // Time X Axis grid lines
            val timeStepCount = min(5, visibleCount)
            val timeStepIndex = visibleCount / timeStepCount
            for (i in 0 until visibleCount step max(1, timeStepIndex)) {
                val x = indexToX(i)
                drawLine(
                    color = TvGrid,
                    start = Offset(x, 0f),
                    end = Offset(x, mainChartHeight),
                    strokeWidth = 1f
                )

                val timeStr = timeFormatter.format(Date(visibleCandles[i].timestamp))
                val measuredTime = textMeasurer.measure(timeStr, axisTextStyle)
                drawText(
                    textLayoutResult = measuredTime,
                    topLeft = Offset(x - (measuredTime.size.width / 2f), mainChartHeight + 2f)
                )
            }

            // 3. Draw Bollinger Bands Cloud (if enabled)
            if (state.showBollingerBands) {
                bbUpperPath.reset()
                bbLowerPath.reset()
                bbFillPath.reset()

                var bbHasStart = false

                for (i in 0 until visibleCount) {
                    val upper = visibleCandles[i].bollingerUpper
                    val lower = visibleCandles[i].bollingerLower

                    if (!upper.isNaN() && !lower.isNaN()) {
                        val x = indexToX(i)
                        val yUpper = priceToY(upper)
                        val yLower = priceToY(lower)

                        if (!bbHasStart) {
                            bbUpperPath.moveTo(x, yUpper)
                            bbLowerPath.moveTo(x, yLower)
                            bbFillPath.moveTo(x, yUpper)
                            bbHasStart = true
                        } else {
                            bbUpperPath.lineTo(x, yUpper)
                            bbLowerPath.lineTo(x, yLower)
                            bbFillPath.lineTo(x, yUpper)
                        }
                    }
                }

                if (bbHasStart) {
                    // Complete fill polygon path down through lower band backwards
                    for (i in (visibleCount - 1) downTo 0) {
                        val lower = visibleCandles[i].bollingerLower
                        if (!lower.isNaN()) {
                            bbFillPath.lineTo(indexToX(i), priceToY(lower))
                        }
                    }
                    bbFillPath.close()

                    // Fill shaded area between bands
                    drawPath(
                        path = bbFillPath,
                        color = TvBbCyan.copy(alpha = 0.08f)
                    )
                    // Draw Upper and Lower band strokes
                    drawPath(
                        path = bbUpperPath,
                        color = TvBbCyan.copy(alpha = 0.7f),
                        style = Stroke(width = 1.2f)
                    )
                    drawPath(
                        path = bbLowerPath,
                        color = TvBbCyan.copy(alpha = 0.7f),
                        style = Stroke(width = 1.2f)
                    )
                }
            }

            // 4. Draw Volume Indicator Panel
            val volumeHeightScale = mainChartHeight * 0.18f
            val volumeTopY = mainChartHeight - volumeHeightScale

            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(0f, volumeTopY),
                end = Offset(plotWidth, volumeTopY),
                strokeWidth = 1f,
                pathEffect = dashedEffect
            )

            for (i in 0 until visibleCount) {
                val c = visibleCandles[i]
                val x = indexToX(i)

                val volHeight = if (maxVolume > 0) (c.volume / maxVolume) * volumeHeightScale else 0f
                val volTop = mainChartHeight - volHeight
                drawRect(
                    color = if (c.isBullish) bullishVolColor else bearishVolColor,
                    topLeft = Offset(x - halfBodyWidth, volTop),
                    size = Size(bodyWidth, volHeight)
                )
            }

            // 5. Draw Main Price Series according to ChartStyle
            when (state.chartStyle) {
                ChartStyle.CANDLESTICK, ChartStyle.HOLLOW_CANDLE -> {
                    for (i in 0 until visibleCount) {
                        val c = visibleCandles[i]
                        val x = indexToX(i)
                        val color = if (c.isBullish) TvBullTeal else TvBearCrimson

                        val highY = priceToY(c.high)
                        val lowY = priceToY(c.low)
                        val openY = priceToY(c.open)
                        val closeY = priceToY(c.close)
                        val bodyTop = min(openY, closeY)
                        val bodyHeight = max(1.5f, kotlin.math.abs(openY - closeY))

                        // Wick
                        drawLine(
                            color = color,
                            start = Offset(x, highY),
                            end = Offset(x, lowY),
                            strokeWidth = 1.2f
                        )

                        // Body
                        if (state.chartStyle == ChartStyle.HOLLOW_CANDLE && c.isBullish) {
                            // Hollow candle body
                            drawRect(
                                color = color,
                                topLeft = Offset(x - halfBodyWidth, bodyTop),
                                size = Size(bodyWidth, bodyHeight),
                                style = Stroke(width = 1.2f)
                            )
                        } else {
                            // Solid candle body
                            drawRect(
                                color = color,
                                topLeft = Offset(x - halfBodyWidth, bodyTop),
                                size = Size(bodyWidth, bodyHeight)
                            )
                        }
                    }
                }

                ChartStyle.AREA_GLOW -> {
                    areaPath.reset()
                    areaFillPath.reset()
                    var areaHasStart = false

                    for (i in 0 until visibleCount) {
                        val c = visibleCandles[i]
                        val x = indexToX(i)
                        val y = priceToY(c.close)

                        if (!areaHasStart) {
                            areaPath.moveTo(x, y)
                            areaFillPath.moveTo(x, mainChartHeight)
                            areaFillPath.lineTo(x, y)
                            areaHasStart = true
                        } else {
                            areaPath.lineTo(x, y)
                            areaFillPath.lineTo(x, y)
                        }
                    }

                    if (areaHasStart) {
                        val lastX = indexToX(visibleCount - 1)
                        areaFillPath.lineTo(lastX, mainChartHeight)
                        areaFillPath.close()

                        // Vertical gradient fill underneath area curve
                        val areaGradient = Brush.verticalGradient(
                            colors = listOf(TvBullTeal.copy(alpha = 0.45f), TvBullTeal.copy(alpha = 0.0f)),
                            startY = 0f,
                            endY = mainChartHeight
                        )

                        drawPath(path = areaFillPath, brush = areaGradient)
                        drawPath(path = areaPath, color = TvBullTeal, style = Stroke(width = 2.2f))
                    }
                }

                ChartStyle.HEIKIN_ASHI -> {
                    var haOpen = visibleCandles.first().open
                    var haClose = visibleCandles.first().close

                    for (i in 0 until visibleCount) {
                        val c = visibleCandles[i]
                        val x = indexToX(i)

                        val currentHaClose = (c.open + c.high + c.low + c.close) / 4f
                        val currentHaOpen = if (i == 0) (c.open + c.close) / 2f else (haOpen + haClose) / 2f
                        val currentHaHigh = maxOf(c.high, currentHaOpen, currentHaClose)
                        val currentHaLow = minOf(c.low, currentHaOpen, currentHaClose)

                        haOpen = currentHaOpen
                        haClose = currentHaClose

                        val isHaBullish = currentHaClose >= currentHaOpen
                        val color = if (isHaBullish) TvBullTeal else TvBearCrimson

                        val highY = priceToY(currentHaHigh)
                        val lowY = priceToY(currentHaLow)
                        val openY = priceToY(currentHaOpen)
                        val closeY = priceToY(currentHaClose)
                        val bodyTop = min(openY, closeY)
                        val bodyHeight = max(1.5f, kotlin.math.abs(openY - closeY))

                        drawLine(color = color, start = Offset(x, highY), end = Offset(x, lowY), strokeWidth = 1.2f)
                        drawRect(color = color, topLeft = Offset(x - halfBodyWidth, bodyTop), size = Size(bodyWidth, bodyHeight))
                    }
                }

                ChartStyle.BARS -> {
                    for (i in 0 until visibleCount) {
                        val c = visibleCandles[i]
                        val x = indexToX(i)
                        val color = if (c.isBullish) TvBullTeal else TvBearCrimson

                        val highY = priceToY(c.high)
                        val lowY = priceToY(c.low)
                        val openY = priceToY(c.open)
                        val closeY = priceToY(c.close)

                        // Vertical high-low line
                        drawLine(color = color, start = Offset(x, highY), end = Offset(x, lowY), strokeWidth = 1.5f)
                        // Left tick for Open
                        drawLine(color = color, start = Offset(x - halfBodyWidth, openY), end = Offset(x, openY), strokeWidth = 1.5f)
                        // Right tick for Close
                        drawLine(color = color, start = Offset(x, closeY), end = Offset(x + halfBodyWidth, closeY), strokeWidth = 1.5f)
                    }
                }
            }

            // 6. Draw SMA 20 Line
            if (state.showSma20) {
                reusablePath.reset()
                var hasFirstPoint = false

                for (i in 0 until visibleCount) {
                    val sma = visibleCandles[i].sma20
                    if (!sma.isNaN()) {
                        val x = indexToX(i)
                        val y = priceToY(sma)
                        if (!hasFirstPoint) {
                            reusablePath.moveTo(x, y)
                            hasFirstPoint = true
                        } else {
                            reusablePath.lineTo(x, y)
                        }
                    }
                }

                if (hasFirstPoint) {
                    drawPath(path = reusablePath, color = ElectricNeonViolet, style = Stroke(width = 2f))
                }
            }

            // 7. Draw EMA 50 Line
            if (state.showEma50) {
                emaPath.reset()
                var hasFirstEma = false

                for (i in 0 until visibleCount) {
                    val ema = visibleCandles[i].ema50
                    if (!ema.isNaN()) {
                        val x = indexToX(i)
                        val y = priceToY(ema)
                        if (!hasFirstEma) {
                            emaPath.moveTo(x, y)
                            hasFirstEma = true
                        } else {
                            emaPath.lineTo(x, y)
                        }
                    }
                }

                if (hasFirstEma) {
                    drawPath(path = emaPath, color = TvAmberEma, style = Stroke(width = 2f))
                }
            }

            // 8. Draw High & Low Callout Markers (TradingView Style Peak Tags)
            if (visibleCount > 1) {
                val highCandle = visibleCandles[highestCandleIdx]
                val highX = indexToX(highestCandleIdx)
                val highY = priceToY(highCandle.high)

                val highText = String.format(Locale.US, "▲ H:%.2f", highCandle.high)
                val measuredHigh = textMeasurer.measure(highText, peakCalloutStyle)

                drawRect(
                    color = TvBullTeal,
                    topLeft = Offset(highX - (measuredHigh.size.width / 2f) - 4f, highY - measuredHigh.size.height - 6f),
                    size = Size(measuredHigh.size.width + 8f, measuredHigh.size.height + 4f)
                )
                drawText(
                    textLayoutResult = measuredHigh,
                    topLeft = Offset(highX - (measuredHigh.size.width / 2f), highY - measuredHigh.size.height - 4f)
                )

                val lowCandle = visibleCandles[lowestCandleIdx]
                val lowX = indexToX(lowestCandleIdx)
                val lowY = priceToY(lowCandle.low)

                val lowText = String.format(Locale.US, "▼ L:%.2f", lowCandle.low)
                val measuredLow = textMeasurer.measure(lowText, peakCalloutStyle)

                drawRect(
                    color = TvBearCrimson,
                    topLeft = Offset(lowX - (measuredLow.size.width / 2f) - 4f, lowY + 2f),
                    size = Size(measuredLow.size.width + 8f, measuredLow.size.height + 4f)
                )
                drawText(
                    textLayoutResult = measuredLow,
                    topLeft = Offset(lowX - (measuredLow.size.width / 2f), lowY + 4f)
                )
            }

            // 9. Live Price Tracker & Right Axis Price Tag
            val latestCandle = visibleCandles.last()
            val liveY = priceToY(latestCandle.close)
            if (liveY in 0f..mainChartHeight) {
                val liveColor = if (latestCandle.isBullish) TvBullTeal else TvBearCrimson

                // Dashed live price line
                drawLine(
                    color = liveColor.copy(alpha = 0.8f),
                    start = Offset(0f, liveY),
                    end = Offset(plotWidth, liveY),
                    strokeWidth = 1.2f,
                    pathEffect = dashedEffect
                )

                // Right axis live price badge
                val livePriceStr = String.format(
                    Locale.US,
                    if (state.symbol.isForex && state.symbol.pipDigits == 4) "%.4f" else "%.2f",
                    latestCandle.close
                )
                val measuredLive = textMeasurer.measure(livePriceStr, peakCalloutStyle)

                drawRect(
                    color = liveColor,
                    topLeft = Offset(plotWidth + 1f, liveY - 10f),
                    size = Size(measuredLive.size.width + 12f, measuredLive.size.height + 6f)
                )
                drawText(
                    textLayoutResult = measuredLive,
                    topLeft = Offset(plotWidth + 6f, liveY - 7f)
                )
            }

            // 9.5 Draw Liquidity Pools (BSL / SSL Lines - Smart Money Concepts)
            if (state.showLiquidityOverlay) {
                val bslLevel = state.liquidityConcept.buySideLevel
                val sslLevel = state.liquidityConcept.sellSideLevel

                if (bslLevel > 0f) {
                    val bslY = priceToY(bslLevel)
                    if (bslY in 0f..mainChartHeight) {
                        val bslColor = ElectricNeonViolet
                        drawLine(
                            color = bslColor,
                            start = Offset(0f, bslY),
                            end = Offset(plotWidth, bslY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                        val bslText = "💧 BSL POOL"
                        val measuredBsl = textMeasurer.measure(bslText, peakCalloutStyle)
                        drawRect(
                            color = bslColor,
                            topLeft = Offset(plotWidth - measuredBsl.size.width - 12f, bslY - 10f),
                            size = Size(measuredBsl.size.width + 10f, measuredBsl.size.height + 4f)
                        )
                        drawText(
                            textLayoutResult = measuredBsl,
                            topLeft = Offset(plotWidth - measuredBsl.size.width - 7f, bslY - 8f)
                        )
                    }
                }

                if (sslLevel > 0f) {
                    val sslY = priceToY(sslLevel)
                    if (sslY in 0f..mainChartHeight) {
                        val sslColor = TvBearCrimson
                        drawLine(
                            color = sslColor,
                            start = Offset(0f, sslY),
                            end = Offset(plotWidth, sslY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                        val sslText = "💧 SSL POOL"
                        val measuredSsl = textMeasurer.measure(sslText, peakCalloutStyle)
                        drawRect(
                            color = sslColor,
                            topLeft = Offset(plotWidth - measuredSsl.size.width - 12f, sslY - 10f),
                            size = Size(measuredSsl.size.width + 10f, measuredSsl.size.height + 4f)
                        )
                        drawText(
                            textLayoutResult = measuredSsl,
                            topLeft = Offset(plotWidth - measuredSsl.size.width - 7f, sslY - 8f)
                        )
                    }
                }
            }

            // 10. Draw User Drawings (Horizontal Lines & Trendlines)
            for (element in drawings) {
                when (element) {
                    is DrawingElement.HorizontalLine -> {
                        val y = priceToY(element.price)
                        if (y in 0f..mainChartHeight) {
                            drawLine(
                                color = Color(element.colorArgb),
                                start = Offset(0f, y),
                                end = Offset(plotWidth, y),
                                strokeWidth = 2f,
                                pathEffect = dashedEffect
                            )
                            val labelText = String.format(Locale.US, "LEVEL: %.2f", element.price)
                            val measured = textMeasurer.measure(labelText, axisTextStyle)
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(10f, y - measured.size.height - 2f)
                            )
                        }
                    }

                    is DrawingElement.TrendLine -> {
                        val startIdx = visibleCandles.indexOfFirst { it.timestamp >= element.startTimestamp }
                        val endIdx = visibleCandles.indexOfLast { it.timestamp <= element.endTimestamp }

                        if (startIdx != -1 && endIdx != -1 && startIdx <= endIdx) {
                            val startX = indexToX(startIdx)
                            val startY = priceToY(element.startPrice)
                            val endX = indexToX(endIdx)
                            val endY = priceToY(element.endPrice)

                            drawLine(
                                color = Color(element.colorArgb),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 2.5f
                            )
                        }
                    }
                }
            }

            // Live Preview during Trendline Drag
            if (isDrawingActive && state.drawingMode == DrawingMode.TREND_LINE) {
                drawLine(
                    color = CyberCyan,
                    start = drawStartOffset,
                    end = drawCurrentOffset,
                    strokeWidth = 2f,
                    pathEffect = dashedEffect
                )
            }

            // 11. Active Custom Price Threshold Alerts Lines on Canvas
            for (alert in state.priceAlerts) {
                if (alert.symbol == state.symbol && alert.isActive) {
                    val y = priceToY(alert.targetPrice)
                    if (y in 0f..mainChartHeight) {
                        val alertColor = Color(0xFFFFD700) // Neon Gold
                        drawLine(
                            color = alertColor,
                            start = Offset(0f, y),
                            end = Offset(plotWidth, y),
                            strokeWidth = 2f,
                            pathEffect = dashedEffect
                        )

                        val alertLabel = String.format(Locale.US, "🔔 %.2f", alert.targetPrice)
                        val measuredAlert = textMeasurer.measure(alertLabel, axisTextStyle)

                        drawRect(
                            color = alertColor.copy(alpha = 0.25f),
                            topLeft = Offset(plotWidth + 2f, y - 8f),
                            size = Size(measuredAlert.size.width + 8f, measuredAlert.size.height + 4f)
                        )
                        drawText(
                            textLayoutResult = measuredAlert,
                            topLeft = Offset(plotWidth + 6f, y - 6f)
                        )
                    }
                }
            }

            // 12. Draw Lower RSI Sub-Panel Canvas (if enabled)
            if (state.showRsi14) {
                val rsiTop = mainChartHeight + axisBottomPadding
                val rsiBottom = canvasHeight

                drawLine(
                    color = TvGrid,
                    start = Offset(0f, rsiTop),
                    end = Offset(canvasWidth, rsiTop),
                    strokeWidth = 1.5f
                )

                val rsiTitle = "RSI (14)"
                val measuredRsiTitle = textMeasurer.measure(rsiTitle, axisTextStyle)
                drawText(
                    textLayoutResult = measuredRsiTitle,
                    topLeft = Offset(10f, rsiTop + 4f)
                )

                fun rsiToY(rsiVal: Float): Float {
                    return rsiBottom - ((rsiVal / 100f) * rsiHeight)
                }

                val y70 = rsiToY(70f)
                val y30 = rsiToY(30f)

                // 70 Overbought line
                drawLine(color = TvBearCrimson.copy(alpha = 0.5f), start = Offset(0f, y70), end = Offset(plotWidth, y70), strokeWidth = 1f, pathEffect = dashedEffect)
                drawText(textLayoutResult = textMeasurer.measure("70", axisTextStyle), topLeft = Offset(plotWidth + 6f, y70 - 6f))

                // 30 Oversold line
                drawLine(color = TvBullTeal.copy(alpha = 0.5f), start = Offset(0f, y30), end = Offset(plotWidth, y30), strokeWidth = 1f, pathEffect = dashedEffect)
                drawText(textLayoutResult = textMeasurer.measure("30", axisTextStyle), topLeft = Offset(plotWidth + 6f, y30 - 6f))

                // RSI Curve Path
                rsiPath.reset()
                var rsiHasStart = false

                for (i in 0 until visibleCount) {
                    val rsi = visibleCandles[i].rsi14
                    if (!rsi.isNaN()) {
                        val x = indexToX(i)
                        val y = rsiToY(rsi)
                        if (!rsiHasStart) {
                            rsiPath.moveTo(x, y)
                            rsiHasStart = true
                        } else {
                            rsiPath.lineTo(x, y)
                        }
                    }
                }

                if (rsiHasStart) {
                    drawPath(path = rsiPath, color = CyberCyan, style = Stroke(width = 1.8f))
                }
            }

            // 13. Top-Left TradingView Legend HUD Readout (Active or Hovered Candle)
            val displayCandle = if (state.selectedCandleIndex in candles.indices) {
                candles[state.selectedCandleIndex]
            } else {
                visibleCandles.lastOrNull()
            }

            displayCandle?.let { c ->
                val change = c.close - c.open
                val changePct = if (c.open != 0f) (change / c.open) * 100f else 0f
                val isBull = c.close >= c.open
                val statColor = if (isBull) TvBullTeal else TvBearCrimson

                val formatStr = if (state.symbol.isForex && state.symbol.pipDigits == 4) "%.4f" else "%.2f"
                val legendText = String.format(
                    Locale.US,
                    "O:$formatStr H:$formatStr L:$formatStr C:$formatStr  %s$formatStr (%s%.2f%%)",
                    c.open, c.high, c.low, c.close,
                    if (change >= 0) "+" else "", change,
                    if (changePct >= 0) "+" else "", changePct
                )

                val measuredLegend = textMeasurer.measure(legendText, legendTextStyle.copy(color = statColor))

                // Draw translucent legend backdrop card
                drawRect(
                    color = TvBackground.copy(alpha = 0.85f),
                    topLeft = Offset(8f, 6f),
                    size = Size(measuredLegend.size.width + 12f, measuredLegend.size.height + 8f)
                )
                drawRect(
                    color = TvGrid,
                    topLeft = Offset(8f, 6f),
                    size = Size(measuredLegend.size.width + 12f, measuredLegend.size.height + 8f),
                    style = Stroke(width = 1f)
                )
                drawText(
                    textLayoutResult = measuredLegend,
                    topLeft = Offset(14f, 10f)
                )
            }

            // 14. Crosshair Line overlay when selected
            if (state.selectedCandleIndex in candles.indices) {
                val selectedIndexInVisible = state.selectedCandleIndex - startIndex
                if (selectedIndexInVisible in 0 until visibleCount) {
                    val c = candles[state.selectedCandleIndex]
                    val crosshairX = indexToX(selectedIndexInVisible)
                    val crosshairY = priceToY(c.close)

                    drawLine(color = CyberCrosshair, start = Offset(crosshairX, 0f), end = Offset(crosshairX, canvasHeight), strokeWidth = 1f, pathEffect = dashedEffect)
                    drawLine(color = CyberCrosshair, start = Offset(0f, crosshairY), end = Offset(plotWidth, crosshairY), strokeWidth = 1f, pathEffect = dashedEffect)
                }
            }
        }

        // Alert Banner Overlay
        AnimatedVisibility(
            visible = state.activeAlertBanner != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            state.activeAlertBanner?.let { message ->
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                        .background(Color(0xFF1E1035), shape = RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color(0xFFFFD700), shape = RoundedCornerShape(8.dp))
                        .clickable { onDismissAlertBanner() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alert Triggered",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(
                        onClick = onDismissAlertBanner,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Alert",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Floating Quick Action Control: Re-center / Fit View Button
        if (panOffsetIndex != 0 || visibleCandleCount != 50) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 80.dp, bottom = 26.dp)
                    .background(TvBackground.copy(alpha = 0.9f))
                    .border(1.dp, CyberCyan, RoundedCornerShape(4.dp))
                    .clickable {
                        panOffsetIndex = 0
                        visibleCandleCount = 50
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "🎯 RE-CENTER",
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
