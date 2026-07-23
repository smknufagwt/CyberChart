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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.example.data.EconomicEvent
import com.example.data.ImpactLevel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EconomicCalendarDialog(
    events: List<EconomicEvent>,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredEvents = remember(selectedFilter, events) {
        when (selectedFilter) {
            "HIGH" -> events.filter { it.impact == ImpactLevel.HIGH }
            "USD" -> events.filter { it.currency == "USD" }
            "EUR" -> events.filter { it.currency == "EUR" }
            else -> events
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.US) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BentoWhiteBorder, RoundedCornerShape(8.dp)),
            color = CyberBlack,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = CyberCyan,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "MACRO ECONOMIC CALENDAR",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CyberTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "HIGH", "USD", "EUR").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) ElectricNeonViolet else CyberDarkSurface)
                                .border(1.dp, if (isSelected) Color.White else BentoWhiteSubtle)
                                .clickable { selectedFilter = filter }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.White else CyberTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Event List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEvents, key = { it.id }) { event ->
                        val impactColor = when (event.impact) {
                            ImpactLevel.HIGH -> NeonRed
                            ImpactLevel.MEDIUM -> Color(0xFFFF9800)
                            ImpactLevel.LOW -> Color(0xFFFFD700)
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Currency Badge
                                        Box(
                                            modifier = Modifier
                                                .background(ElectricNeonViolet.copy(alpha = 0.2f))
                                                .border(1.dp, ElectricNeonViolet)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = event.currency,
                                                color = ElectricNeonViolet,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Time
                                        Text(
                                            text = timeFormatter.format(Date(event.timestamp)),
                                            color = CyberTextSecondary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Impact Badge
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = "Impact",
                                            tint = impactColor,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text(
                                            text = event.impact.name,
                                            color = impactColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Event Title
                                Text(
                                    text = event.title,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Actual vs Forecast vs Previous Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("ACTUAL", color = CyberTextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        Text(
                                            text = event.actual,
                                            color = if (event.isReleased) NeonGreen else CyberTextPrimary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text("FORECAST", color = CyberTextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        Text(
                                            text = event.forecast,
                                            color = CyberTextPrimary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text("PREVIOUS", color = CyberTextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        Text(
                                            text = event.previous,
                                            color = CyberTextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
