package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val CyanPrimary = Color(0xFF00E5FF)
val CyanPrimaryVariant = Color(0xFF00B0FF)
val CobaltSecondary = Color(0xFF2979FF)
val EmeraldSuccess = Color(0xFF00E676)
val AmberWarning = Color(0xFFFFAB00)
val CoralError = Color(0xFFFF5252)

val DarkBackground = Color(0xFF070B14)
val DarkSurface = Color(0xFF0F1726)
val DarkSurfaceElevated = Color(0xFF162033)
val DarkSurfaceCard = Color(0xFF1B273D)
val DarkBorder = Color(0xFF233552)

val TextPrimary = Color(0xFFF0F4F8)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Card Gradient Palettes
object CardGradients {
    val MidnightCyan = Brush.linearGradient(
        listOf(Color(0xFF004D5A), Color(0xFF021B2B), Color(0xFF0A101D))
    )
    val ObsidianCrimson = Brush.linearGradient(
        listOf(Color(0xFF6A0D1F), Color(0xFF2D070E), Color(0xFF0D0204))
    )
    val EmeraldSecurity = Brush.linearGradient(
        listOf(Color(0xFF005339), Color(0xFF022B1E), Color(0xFF051711))
    )
    val NeonCobalt = Brush.linearGradient(
        listOf(Color(0xFF153372), Color(0xFF0C193D), Color(0xFF080D1D))
    )
    val CyberAmber = Brush.linearGradient(
        listOf(Color(0xFF5E3A00), Color(0xFF2E1A00), Color(0xFF170D00))
    )
    val CarbonSlate = Brush.linearGradient(
        listOf(Color(0xFF2D3748), Color(0xFF1A202C), Color(0xFF11141C))
    )

    fun getGradient(index: Int): Brush {
        return when (index % 6) {
            0 -> MidnightCyan
            1 -> ObsidianCrimson
            2 -> EmeraldSecurity
            3 -> NeonCobalt
            4 -> CyberAmber
            else -> CarbonSlate
        }
    }

    fun getAccentColor(index: Int): Color {
        return when (index % 6) {
            0 -> Color(0xFF00E5FF)
            1 -> Color(0xFFFF5252)
            2 -> Color(0xFF00E676)
            3 -> Color(0xFF448AFF)
            4 -> Color(0xFFFFD740)
            else -> Color(0xFFCFD8DC)
        }
    }
}

