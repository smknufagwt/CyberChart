package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DrawingMode
import com.example.ui.theme.BentoWhiteBorder
import com.example.ui.theme.BentoWhiteSubtle
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricNeonViolet
import com.example.ui.theme.NeonRed

/**
 * Bento Grid Footer Toolbar (4-column layout with sharp white borders and active highlights).
 */
@Composable
fun DrawingToolbar(
    activeMode: DrawingMode,
    onModeSelect: (DrawingMode) -> Unit,
    onClearDrawings: () -> Unit,
    drawingsCount: Int,
    onHideToolbar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBlack)
            .border(2.dp, BentoWhiteBorder),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: DRAW MODE [NAV / PAN]
        BentoFooterButton(
            title = "DRAW",
            subTitle = if (activeMode == DrawingMode.PAN_ZOOM) "PAN/ZOOM" else "ACTIVE",
            isSelected = activeMode == DrawingMode.PAN_ZOOM,
            activeBgColor = ElectricNeonViolet,
            activeTextColor = Color.White,
            onClick = { onModeSelect(DrawingMode.PAN_ZOOM) },
            modifier = Modifier
                .weight(1f)
                .border(1.dp, BentoWhiteSubtle)
        )

        // Column 2: H-LINE TOOL
        BentoFooterButton(
            title = "H-LINE",
            subTitle = "SUPPORT",
            isSelected = activeMode == DrawingMode.HORIZONTAL_LINE,
            activeBgColor = ElectricNeonViolet,
            activeTextColor = Color.White,
            onClick = { onModeSelect(DrawingMode.HORIZONTAL_LINE) },
            modifier = Modifier
                .weight(1f)
                .border(1.dp, BentoWhiteSubtle)
        )

        // Column 3: TRENDLINE TOOL
        BentoFooterButton(
            title = "TREND",
            subTitle = "LINE TOOL",
            isSelected = activeMode == DrawingMode.TREND_LINE,
            activeBgColor = CyberCyan,
            activeTextColor = Color.Black,
            onClick = { onModeSelect(DrawingMode.TREND_LINE) },
            modifier = Modifier
                .weight(1f)
                .border(1.dp, BentoWhiteSubtle)
        )

        // Column 4: SYSTEM / CLEAR DRAWINGS
        BentoFooterButton(
            title = "CLEAR",
            subTitle = if (drawingsCount > 0) "($drawingsCount ITEMS)" else "EMPTY",
            isSelected = false,
            activeBgColor = NeonRed,
            activeTextColor = Color.White,
            titleColor = if (drawingsCount > 0) NeonRed else CyberTextPrimary,
            onClick = { if (drawingsCount > 0) onClearDrawings() },
            modifier = Modifier
                .weight(1f)
                .border(1.dp, BentoWhiteSubtle)
        )

        // Column 5 (Optional): HIDE TOOLBAR
        if (onHideToolbar != null) {
            BentoFooterButton(
                title = "TOOLBAR",
                subTitle = "HIDE ▼",
                isSelected = false,
                activeBgColor = BentoWhiteSubtle,
                activeTextColor = Color.White,
                titleColor = CyberCyan,
                onClick = onHideToolbar,
                modifier = Modifier
                    .weight(0.9f)
                    .border(1.dp, BentoWhiteSubtle)
            )
        }
    }
}

@Composable
private fun BentoFooterButton(
    title: String,
    subTitle: String,
    isSelected: Boolean,
    activeBgColor: Color,
    activeTextColor: Color,
    titleColor: Color = CyberTextPrimary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(if (isSelected) activeBgColor else CyberBlack)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (isSelected) activeTextColor else titleColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subTitle,
                color = if (isSelected) activeTextColor.copy(alpha = 0.8f) else CyberTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
