package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalUriHandler
import com.example.ui.components.CyberHeader
import com.example.ui.components.DrawingToolbar
import com.example.ui.components.EconomicCalendarDialog
import com.example.ui.components.MacroLiquidityPanel
import com.example.ui.components.PerformanceChartCanvas
import com.example.ui.components.PriceAlertDialog
import com.example.ui.components.TermsOfUseDialog
import com.example.ui.theme.BentoWhiteBorder
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricNeonViolet

/**
 * Main application screen combining top CyberHeader, central zero-lag PerformanceChartCanvas,
 * and bottom DrawingToolbar.
 */
@Composable
fun CyberTerminalApp(
    viewModel: ChartViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val uriHandler = LocalUriHandler.current
    var showPriceAlertDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    var showDrawingToolbar by remember { mutableStateOf(true) }

    val lastPrice = uiState.candles.lastOrNull()?.close ?: uiState.symbol.basePrice

    if (showPriceAlertDialog) {
        PriceAlertDialog(
            currentPrice = lastPrice,
            symbolName = uiState.symbol.displayName,
            alerts = uiState.priceAlerts.filter { it.symbol == uiState.symbol },
            onAddAlert = { targetPrice, condition ->
                viewModel.addPriceAlert(targetPrice, condition)
            },
            onRemoveAlert = { alertId ->
                viewModel.removePriceAlert(alertId)
            },
            onDismiss = { showPriceAlertDialog = false }
        )
    }

    if (showTermsDialog) {
        TermsOfUseDialog(
            onDismiss = { showTermsDialog = false }
        )
    }

    if (uiState.showCalendarPanel) {
        EconomicCalendarDialog(
            events = uiState.economicEvents,
            onDismiss = { viewModel.toggleCalendarPanel() }
        )
    }

    if (uiState.showMacroPanel) {
        MacroLiquidityPanel(
            state = uiState,
            onDismiss = { viewModel.toggleMacroPanel() },
            onToggleLiquidityOverlay = { viewModel.toggleLiquidityOverlay() }
        )
    }

    Scaffold(
        containerColor = CyberBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    top = safeInsets.calculateTopPadding(),
                    bottom = safeInsets.calculateBottomPadding()
                )
                .background(CyberBlack)
        ) {
            // Top Terminal Header
            CyberHeader(
                state = uiState,
                onSelectSymbol = { viewModel.selectSymbol(it) },
                onSelectInterval = { viewModel.selectInterval(it) },
                onSelectChartStyle = { viewModel.setChartStyle(it) },
                onToggleSma = { viewModel.toggleSma20() },
                onToggleEma = { viewModel.toggleEma50() },
                onToggleBollinger = { viewModel.toggleBollingerBands() },
                onToggleRsi = { viewModel.toggleRsi14() },
                onOpenAlerts = { showPriceAlertDialog = true },
                onOpenCalendar = { viewModel.toggleCalendarPanel() },
                onOpenTerms = { showTermsDialog = true },
                onOpenMacro = { viewModel.toggleMacroPanel() }
            )

            // Central Custom Performance Chart Canvas (Fills available height)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
                    .border(2.dp, BentoWhiteBorder)
            ) {
                PerformanceChartCanvas(
                    state = uiState,
                    onAddHorizontalLine = { viewModel.addHorizontalLine(it) },
                    onAddTrendLine = { startT, startP, endT, endP ->
                        viewModel.addTrendLine(startT, startP, endT, endP)
                    },
                    onSelectCandle = { viewModel.selectCandleIndex(it) },
                    onDismissAlertBanner = { viewModel.dismissAlertBanner() }
                )

                // Floating Reveal Drawing Toolbar Pill Button (When hidden)
                if (!showDrawingToolbar) {
                    Box(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.BottomStart)
                            .padding(10.dp)
                            .background(CyberBlack.copy(alpha = 0.9f))
                            .border(1.dp, BentoWhiteBorder)
                            .clickable { showDrawingToolbar = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "✏️ DRAW TOOLS ▲",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Bottom Drawing Tools Toolbar
            if (showDrawingToolbar) {
                DrawingToolbar(
                    activeMode = uiState.drawingMode,
                    onModeSelect = { viewModel.setDrawingMode(it) },
                    onClearDrawings = { viewModel.clearDrawings() },
                    drawingsCount = uiState.drawings.size,
                    onHideToolbar = { showDrawingToolbar = false }
                )
            }

            // Bottom Scrollable Copyright Arlingkin & Anonymous Footer Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0F14))
                    .border(1.dp, BentoWhiteBorder)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Anonymous Logo & Copyright Arlingkin
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showTermsDialog = true }
                ) {
                    Text(
                        text = "🎭",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "© 2026 ARLINGKIN™ TERMINAL • ALL RIGHTS RESERVED • REGISTERED BUILD alpha-v1.2®",
                        color = CyberTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // t.me/arlingkin link button that opens website
                    Box(
                        modifier = Modifier
                            .background(CyberCyan.copy(alpha = 0.2f))
                            .border(1.dp, CyberCyan, RoundedCornerShape(2.dp))
                            .clickable {
                                try {
                                    uriHandler.openUri("https://t.me/arlingkin")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✈️ t.me/arlingkin",
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Terms & Disclaimer button
                    Box(
                        modifier = Modifier
                            .background(ElectricNeonViolet.copy(alpha = 0.25f))
                            .border(1.dp, ElectricNeonViolet, RoundedCornerShape(2.dp))
                            .clickable { showTermsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📜 TERMS & DISCLAIMER",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
