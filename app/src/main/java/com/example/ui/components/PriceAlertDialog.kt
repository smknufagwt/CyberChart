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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AlertCondition
import com.example.data.PriceAlert
import com.example.ui.theme.BentoWhiteBorder
import com.example.ui.theme.BentoWhiteSubtle
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricNeonViolet
import com.example.ui.theme.NeonRed
import java.util.Locale

@Composable
fun PriceAlertDialog(
    currentPrice: Float,
    symbolName: String,
    alerts: List<PriceAlert>,
    onAddAlert: (Float, AlertCondition) -> Unit,
    onRemoveAlert: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var priceInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentPrice)) }
    var selectedCondition by remember { mutableStateOf(AlertCondition.ABOVE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alerts",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "PRICE ALERTS ($symbolName)",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
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

                Spacer(modifier = Modifier.height(12.dp))

                // Current Market Reference Price
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberDarkSurface)
                        .border(1.dp, BentoWhiteSubtle)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE MARKET PRICE",
                            color = CyberTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", currentPrice),
                            color = ElectricNeonViolet,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Condition Selector Tabs (CROSSES ABOVE / CROSSES BELOW)
                Text(
                    text = "CONDITION",
                    color = CyberTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAboveSelected = selectedCondition == AlertCondition.ABOVE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isAboveSelected) ElectricNeonViolet else CyberDarkSurface)
                            .border(1.dp, if (isAboveSelected) Color.White else BentoWhiteSubtle)
                            .clickable { selectedCondition = AlertCondition.ABOVE }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▲ CROSSES ABOVE",
                            color = if (isAboveSelected) Color.White else CyberTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val isBelowSelected = selectedCondition == AlertCondition.BELOW
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isBelowSelected) CyberCyan else CyberDarkSurface)
                            .border(1.dp, if (isBelowSelected) Color.White else BentoWhiteSubtle)
                            .clickable { selectedCondition = AlertCondition.BELOW }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▼ CROSSES BELOW",
                            color = if (isBelowSelected) Color.Black else CyberTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price Input Field
                Text(
                    text = "TARGET PRICE THRESHOLD",
                    color = CyberTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = {
                        priceInput = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = BentoWhiteSubtle,
                        focusedContainerColor = CyberDarkSurface,
                        unfocusedContainerColor = CyberDarkSurface
                    ),
                    singleLine = true
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = NeonRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add Alert Button
                Button(
                    onClick = {
                        val price = priceInput.toFloatOrNull()
                        if (price != null && price > 0) {
                            onAddAlert(price, selectedCondition)
                            errorMessage = null
                        } else {
                            errorMessage = "ENTER A VALID NUMERIC PRICE"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAlert,
                        contentDescription = "Set Alert",
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "SET PRICE ALERT THRESHOLD",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of Active Alerts
                Text(
                    text = "ACTIVE ALERTS (${alerts.size})",
                    color = CyberTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (alerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberDarkSurface)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO ACTIVE PRICE ALERTS SET",
                            color = CyberTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(alerts, key = { it.id }) { alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyberDarkSurface)
                                    .border(1.dp, BentoWhiteSubtle)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (alert.condition == AlertCondition.ABOVE) "▲ ABOVE" else "▼ BELOW",
                                        color = if (alert.condition == AlertCondition.ABOVE) ElectricNeonViolet else CyberCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.2f", alert.targetPrice),
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveAlert(alert.id) },
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Alert",
                                        tint = NeonRed
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
