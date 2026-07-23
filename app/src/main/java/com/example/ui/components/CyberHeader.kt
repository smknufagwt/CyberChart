package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChartInterval
import com.example.data.CryptoSymbol
import com.example.ui.ChartViewState
import com.example.ui.theme.BentoWhiteBorder
import com.example.ui.theme.BentoWhiteSubtle
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricNeonViolet
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import java.util.Locale

import com.example.data.ChartStyle

/**
 * Bento Grid Styling Top Header Bar.
 * Features hard white borders, high-contrast monospace typography, badge tags, and real-time ticker data.
 */
@Composable
fun CyberHeader(
    state: ChartViewState,
    onSelectSymbol: (CryptoSymbol) -> Unit,
    onSelectInterval: (ChartInterval) -> Unit,
    onSelectChartStyle: (ChartStyle) -> Unit = {},
    onToggleSma: () -> Unit,
    onToggleEma: () -> Unit = {},
    onToggleBollinger: () -> Unit = {},
    onToggleRsi: () -> Unit,
    onOpenAlerts: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenMacro: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lastCandle = state.candles.lastOrNull()
    val currentPrice = lastCandle?.close ?: state.symbol.basePrice
    val openPrice = state.candles.firstOrNull()?.open ?: currentPrice
    val priceChange = currentPrice - openPrice
    val changePct = if (openPrice != 0f) (priceChange / openPrice) * 100f else 0f
    val isPositive = priceChange >= 0

    var isAssetStripExpanded by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("ALL") } // ALL, CRYPTO, FOREX

    val filteredSymbols = remember(selectedCategory) {
        when (selectedCategory) {
            "CRYPTO" -> CryptoSymbol.entries.filter { !it.isForex }
            "FOREX" -> CryptoSymbol.entries.filter { it.isForex }
            else -> CryptoSymbol.entries
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .border(2.dp, BentoWhiteBorder)
            .padding(8.dp)
    ) {
        // Row 1: Symbol Title + Ticker Price + Gas + Quick Expand Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.symbol.displayName,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                
                // Live price readout
                Text(
                    text = String.format(Locale.US, "%.${state.symbol.pipDigits}f", currentPrice),
                    color = if (isPositive) ElectricNeonViolet else CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                
                // Percentage badge
                Box(
                    modifier = Modifier
                        .background(if (isPositive) ElectricNeonViolet.copy(alpha = 0.25f) else CyberCyan.copy(alpha = 0.25f))
                        .border(1.dp, if (isPositive) ElectricNeonViolet else CyberCyan)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%+.2f%%", changePct),
                        color = if (isPositive) Color.White else CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right Cluster: F&G + Gas + Asset Drawer Toggle Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Real-time Fear & Greed Pill
                val fngVal = state.macroData.fearAndGreedValue
                val fngColor = when {
                    fngVal <= 25 -> NeonRed
                    fngVal <= 45 -> Color(0xFFFF9800)
                    fngVal <= 55 -> CyberCyan
                    fngVal <= 75 -> NeonGreen
                    else -> ElectricNeonViolet
                }
                Box(
                    modifier = Modifier
                        .background(fngColor.copy(alpha = 0.15f))
                        .border(1.dp, fngColor)
                        .clickable { onOpenMacro() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🧠 F&G $fngVal",
                        color = fngColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Real-time Gas GWEI pill
                val gasColor = when (state.gasStatus) {
                    "LOW" -> NeonGreen
                    "FAST" -> Color(0xFFFFB300)
                    else -> NeonRed
                }
                Box(
                    modifier = Modifier
                        .background(gasColor.copy(alpha = 0.15f))
                        .border(1.dp, gasColor)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⛽ ${state.gasGwei}GWEI",
                        color = gasColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Asset Selector Toggle Button (Allows hiding asset bar for full screen space)
                Box(
                    modifier = Modifier
                        .background(if (isAssetStripExpanded) CyberDarkSurface else ElectricNeonViolet)
                        .border(1.dp, BentoWhiteSubtle)
                        .clickable { isAssetStripExpanded = !isAssetStripExpanded }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isAssetStripExpanded) "▲ COMPACT" else "▼ ASSETS",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Expandable Symbol Selector & Asset Category Filter (Space efficient)
        if (isAssetStripExpanded) {
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Filter Pills (ALL, CRYPTO, FOREX)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    listOf("ALL", "CRYPTO", "FOREX").forEach { cat ->
                        val isCatSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .background(if (isCatSelected) Color.White else CyberBlack)
                                .border(1.dp, if (isCatSelected) Color.White else BentoWhiteSubtle)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isCatSelected) Color.Black else CyberTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Filtered Symbols LazyRow
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = filteredSymbols) { sym ->
                        val isSelected = sym == state.symbol
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) ElectricNeonViolet else CyberDarkSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.White else BentoWhiteSubtle
                                )
                                .clickable { onSelectSymbol(sym) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (sym.isForex) "💱 ${sym.displayName.replace(" / ", "/")}" else "⚡ ${sym.displayName.replace(" / ", "/")}",
                                color = if (isSelected) Color.White else CyberTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 3: Timeframe Intervals, Chart Styles & Indicators Horizontal Bar
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframe intervals
            items(ChartInterval.entries) { inter ->
                val isSelected = inter == state.interval
                Box(
                    modifier = Modifier
                        .background(if (isSelected) Color.White else CyberBlack)
                        .border(1.dp, if (isSelected) Color.White else BentoWhiteSubtle)
                        .clickable { onSelectInterval(inter) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = inter.label,
                        color = if (isSelected) Color.Black else CyberTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Divider spacer
            item {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(BentoWhiteSubtle)
                )
            }

            // Chart Styles selector
            items(ChartStyle.entries) { style ->
                val isSelected = style == state.chartStyle
                Box(
                    modifier = Modifier
                        .background(if (isSelected) CyberCyan else CyberBlack)
                        .border(1.dp, if (isSelected) Color.White else BentoWhiteSubtle)
                        .clickable { onSelectChartStyle(style) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = style.label,
                        color = if (isSelected) Color.Black else CyberTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Divider spacer
            item {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(BentoWhiteSubtle)
                )
            }

            // SMA 20 toggle
            item {
                Box(
                    modifier = Modifier
                        .background(if (state.showSma20) ElectricNeonViolet else CyberBlack)
                        .border(1.dp, if (state.showSma20) Color.White else BentoWhiteSubtle)
                        .clickable { onToggleSma() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SMA20",
                        color = if (state.showSma20) Color.White else CyberTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // EMA 50 toggle
            item {
                Box(
                    modifier = Modifier
                        .background(if (state.showEma50) Color(0xFFFFB300) else CyberBlack)
                        .border(1.dp, if (state.showEma50) Color.White else BentoWhiteSubtle)
                        .clickable { onToggleEma() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "EMA50",
                        color = if (state.showEma50) Color.Black else CyberTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // BB (Bollinger Bands) toggle
            item {
                Box(
                    modifier = Modifier
                        .background(if (state.showBollingerBands) Color(0xFF00E5FF) else CyberBlack)
                        .border(1.dp, if (state.showBollingerBands) Color.White else BentoWhiteSubtle)
                        .clickable { onToggleBollinger() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BB(20,2)",
                        color = if (state.showBollingerBands) Color.Black else CyberTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // RSI 14 toggle
            item {
                Box(
                    modifier = Modifier
                        .background(if (state.showRsi14) CyberCyan else CyberBlack)
                        .border(1.dp, if (state.showRsi14) Color.White else BentoWhiteSubtle)
                        .clickable { onToggleRsi() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "RSI14",
                        color = if (state.showRsi14) Color.Black else CyberTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // CALENDAR Button
            item {
                Box(
                    modifier = Modifier
                        .background(CyberBlack)
                        .border(1.dp, BentoWhiteSubtle)
                        .clickable { onOpenCalendar() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📅CAL",
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // MACRO & LIQUIDITY MATRIX Button
            item {
                Box(
                    modifier = Modifier
                        .background(ElectricNeonViolet)
                        .border(1.dp, Color.White)
                        .clickable { onOpenMacro() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🌐MACRO",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ALERT Thresholds Button
            item {
                val activeAlertCount = state.priceAlerts.count { it.isActive }
                Box(
                    modifier = Modifier
                        .background(if (activeAlertCount > 0) Color(0xFFFFD700) else CyberBlack)
                        .border(1.dp, if (activeAlertCount > 0) Color.White else BentoWhiteSubtle)
                        .clickable { onOpenAlerts() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (activeAlertCount > 0) "🔔($activeAlertCount)" else "🔔ALERT",
                        color = if (activeAlertCount > 0) Color.Black else CyberTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // TERMS & COPYRIGHT Button
            item {
                Box(
                    modifier = Modifier
                        .background(ElectricNeonViolet.copy(alpha = 0.2f))
                        .border(1.dp, ElectricNeonViolet)
                        .clickable { onOpenTerms() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📜TERMS",
                        color = ElectricNeonViolet,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
