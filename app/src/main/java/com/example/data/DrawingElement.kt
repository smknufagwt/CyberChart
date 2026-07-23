package com.example.data

import androidx.compose.runtime.Immutable

/**
 * Represent user-drawn elements (Horizontal Support/Resistance, Trendline)
 * stored using primitive price and time values so they stay pinned to the
 * relative chart grid during panning and zooming.
 */
@Immutable
sealed class DrawingElement {
    abstract val id: String
    abstract val colorArgb: Long

    @Immutable
    data class HorizontalLine(
        override val id: String,
        val price: Float,
        override val colorArgb: Long = 0xFF8A2BE2 // Electric Neon Violet
    ) : DrawingElement()

    @Immutable
    data class TrendLine(
        override val id: String,
        val startTimestamp: Long,
        val startPrice: Float,
        val endTimestamp: Long,
        val endPrice: Float,
        override val colorArgb: Long = 0xFF00FFFF // Cyber Cyan
    ) : DrawingElement()
}
