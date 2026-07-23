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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

@Composable
fun MacroLiquidityPanel(
    state: ChartViewState,
    onDismiss: () -> Unit,
    onToggleLiquidityOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(CyberBlack, RoundedCornerShape(8.dp))
                .border(2.dp, BentoWhiteBorder, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column {
                // Header Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🌐 MACRO & LIQUIDITY MATRIX",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ElectricNeonViolet)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "REAL-TIME",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(CyberDarkSurface)
                            .border(1.dp, BentoWhiteSubtle)
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✕ CLOSE",
                            color = CyberTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 1: Crypto Fear & Greed Index
                val fngValue = state.macroData.fearAndGreedValue
                val fngClass = state.macroData.fearAndGreedClassification
                val fngColor = when {
                    fngValue <= 25 -> NeonRed
                    fngValue <= 45 -> Color(0xFFFF9800)
                    fngValue <= 55 -> CyberCyan
                    fngValue <= 75 -> NeonGreen
                    else -> ElectricNeonViolet
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberDarkSurface)
                        .border(1.dp, BentoWhiteSubtle)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧠 FEAR & GREED INDEX",
                                color = CyberTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$fngValue / 100 • $fngClass",
                                color = fngColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress Bar representing 0-100 meter
                        LinearProgressIndicator(
                            progress = { fngValue / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = fngColor,
                            trackColor = CyberBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: Macro Assets Grid (Gold, DXY, US02Y, US10Y)
                Text(
                    text = "📊 GLOBAL MACRO CORRELATIONS",
                    color = CyberTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Gold XAUUSD
                    MacroCard(
                        title = "🥇 GOLD",
                        value = String.format(Locale.US, "$%.1f", state.macroData.goldPrice),
                        delta = String.format(Locale.US, "%+.2f%%", state.macroData.goldChangePct),
                        isPositive = state.macroData.goldChangePct >= 0,
                        modifier = Modifier.weight(1f)
                    )

                    // DXY Dollar Index
                    MacroCard(
                        title = "💵 DXY",
                        value = String.format(Locale.US, "%.2f", state.macroData.dxyIndex),
                        delta = String.format(Locale.US, "%+.2f%%", state.macroData.dxyChangePct),
                        isPositive = state.macroData.dxyChangePct >= 0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // US 2Y Bond Yield
                    MacroCard(
                        title = "🏛️ US02Y",
                        value = String.format(Locale.US, "%.2f%%", state.macroData.us2yYield),
                        delta = String.format(Locale.US, "%+.2f", state.macroData.us2yChange),
                        isPositive = state.macroData.us2yChange >= 0,
                        modifier = Modifier.weight(1f)
                    )

                    // US 10Y Bond Yield
                    MacroCard(
                        title = "📈 US10Y",
                        value = String.format(Locale.US, "%.2f%%", state.macroData.us10yYield),
                        delta = String.format(Locale.US, "%+.2f", state.macroData.us10yChange),
                        isPositive = state.macroData.us10yChange >= 0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 3: Smart Money Liquidity Pool Concept
                val liq = state.liquidityConcept
                val biasColor = when (liq.bias) {
                    "BUY-SIDE SWEEP" -> ElectricNeonViolet
                    "SELL-SIDE SWEEP" -> NeonRed
                    else -> CyberCyan
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberDarkSurface)
                        .border(1.dp, biasColor)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💧 LIQUIDITY POOL ANALYSIS",
                                color = CyberTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .background(biasColor.copy(alpha = 0.2f))
                                    .border(1.dp, biasColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = liq.bias,
                                    color = biasColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val precision = if (state.symbol.isForex && state.symbol.pipDigits == 4) "%.4f" else "%.2f"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "BUY-SIDE (BSL):",
                                    color = CyberTextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = String.format(Locale.US, precision, liq.buySideLevel),
                                    color = ElectricNeonViolet,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "SELL-SIDE (SSL):",
                                    color = CyberTextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = String.format(Locale.US, precision, liq.sellSideLevel),
                                    color = NeonRed,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "POOL SATURATION SCORE:",
                                color = CyberTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${liq.score}% DENSITY",
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle Liquidity Overlay Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (state.showLiquidityOverlay) ElectricNeonViolet else CyberDarkSurface)
                        .border(1.dp, BentoWhiteSubtle)
                        .clickable { onToggleLiquidityOverlay() }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.showLiquidityOverlay) "💧 CHART OVERLAY: BSL / SSL ON" else "💧 CHART OVERLAY: BSL / SSL OFF",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroCard(
    title: String,
    value: String,
    delta: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CyberDarkSurface)
            .border(1.dp, BentoWhiteSubtle)
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = CyberTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = delta,
                    color = if (isPositive) NeonGreen else NeonRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
