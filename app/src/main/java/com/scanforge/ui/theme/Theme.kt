package com.scanforge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Colors ───────────────────────────────────────────────────────────────────
object ScanColors {
    val Bg         = Color(0xFF060812)
    val Bg2        = Color(0xFF0D1120)
    val Bg3        = Color(0xFF111827)
    val Surface    = Color(0xFF161C2D)
    val SurfaceHigh= Color(0xFF1E2540)

    val Accent     = Color(0xFF6366F1)
    val AccentLight= Color(0xFF818CF8)
    val AccentPale = Color(0xFFA5B4FC)

    val Green      = Color(0xFF22D3A5)
    val Orange     = Color(0xFFF97316)
    val Pink       = Color(0xFFEC4899)
    val Red        = Color(0xFFEF4444)

    val Text1      = Color(0xFFF1F5F9)
    val Text2      = Color(0xFF94A3B8)
    val Text3      = Color(0xFF475569)

    val Border     = Color(0xFF1E2A3D)
    val BorderHigh = Color(0xFF2D3E5A)

    // Gradient pairs
    val GradientPrimary = listOf(Accent, Color(0xFF818CF8))
    val GradientSuccess = listOf(Green, Accent)
    val GradientWarm    = listOf(Orange, Pink)
}

// ─── Typography ───────────────────────────────────────────────────────────────
// Uses system default; in production add custom fonts via res/font/
object ScanTypography {
    val HeadingXL = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.8).sp,
        lineHeight = 38.sp
    )
    val HeadingL = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp
    )
    val HeadingM = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = (-0.3).sp
    )
    val BodyL = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    val BodyM = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val BodyS = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    )
    val Label = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp
    )
    val Mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
    )
}

// ─── Material Theme ────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary        = ScanColors.Accent,
    onPrimary      = Color.White,
    secondary      = ScanColors.AccentLight,
    onSecondary    = Color.White,
    background     = ScanColors.Bg,
    onBackground   = ScanColors.Text1,
    surface        = ScanColors.Surface,
    onSurface      = ScanColors.Text1,
    surfaceVariant = ScanColors.Bg2,
    outline        = ScanColors.Border,
    error          = ScanColors.Red,
)

@Composable
fun ScanForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
