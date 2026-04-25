package com.lahacks2026.pretriage.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class ThemeKey { Warm, Calm, Bold, Accessible }

enum class Chrome { Light, Dark }

@Immutable
data class AppPalette(
    val key: ThemeKey,
    val name: String,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkMuted: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val badge: Color,
    val statusGreen: Color,
    val statusAmber: Color,
    val statusRed: Color,
    val statusBlue: Color,
    val chrome: Chrome,
    val fontDisplay: FontFamily,
    val fontBody: FontFamily,
    val fontMono: FontFamily,
    val bodySize: TextUnit,
)

private val Inter = FontFamily.SansSerif
private val Fraunces = FontFamily.Serif
private val Mono = FontFamily.Monospace

val WarmPalette = AppPalette(
    key = ThemeKey.Warm,
    name = "Warm",
    bg = Color(0xFFF5EFE2),
    surface = Color(0xFFFBF6EC),
    surfaceAlt = Color(0xFFEFE7D4),
    border = Color(0xFFE3DAC3),
    ink = Color(0xFF2B2A26),
    inkSoft = Color(0xFF6B675C),
    inkMuted = Color(0xFF8E8A7D),
    accent = Color(0xFF5B7A63),
    accentInk = Color(0xFF23362A),
    accentSoft = Color(0xFFDFE7D9),
    badge = Color(0xFF3D5A45),
    statusGreen = Color(0xFF5B7A63),
    statusAmber = Color(0xFFC98A3A),
    statusRed = Color(0xFFB3493A),
    statusBlue = Color(0xFF4A6F8A),
    chrome = Chrome.Light,
    fontDisplay = Fraunces,
    fontBody = Inter,
    fontMono = Mono,
    bodySize = 17.sp,
)

val CalmPalette = AppPalette(
    key = ThemeKey.Calm,
    name = "Calm clinical",
    bg = Color(0xFFEEF3F6),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFE8EEF3),
    border = Color(0xFFD6DEE5),
    ink = Color(0xFF0F1E2A),
    inkSoft = Color(0xFF4B5D6D),
    inkMuted = Color(0xFF7C8A98),
    accent = Color(0xFF2F6FB0),
    accentInk = Color(0xFF0C2C47),
    accentSoft = Color(0xFFDDE8F3),
    badge = Color(0xFF1F4F80),
    statusGreen = Color(0xFF2E7A55),
    statusAmber = Color(0xFFB27A2A),
    statusRed = Color(0xFFA83B32),
    statusBlue = Color(0xFF2F6FB0),
    chrome = Chrome.Light,
    fontDisplay = Inter,
    fontBody = Inter,
    fontMono = Mono,
    bodySize = 17.sp,
)

val BoldPalette = AppPalette(
    key = ThemeKey.Bold,
    name = "Bold dark",
    bg = Color(0xFF0E1311),
    surface = Color(0xFF171C1A),
    surfaceAlt = Color(0xFF1F2624),
    border = Color(0xFF2C3431),
    ink = Color(0xFFF1EFE7),
    inkSoft = Color(0xFFB6B2A5),
    inkMuted = Color(0xFF8A877C),
    accent = Color(0xFFA3D9B1),
    accentInk = Color(0xFF0E1311),
    accentSoft = Color(0xFF1D2A23),
    badge = Color(0xFFA3D9B1),
    statusGreen = Color(0xFF7FC794),
    statusAmber = Color(0xFFE0A85A),
    statusRed = Color(0xFFE07565),
    statusBlue = Color(0xFF7FB1D9),
    chrome = Chrome.Dark,
    fontDisplay = Fraunces,
    fontBody = Inter,
    fontMono = Mono,
    bodySize = 17.sp,
)

val AccessiblePalette = AppPalette(
    key = ThemeKey.Accessible,
    name = "Accessible",
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF0F0F0),
    border = Color(0xFF000000),
    ink = Color(0xFF000000),
    inkSoft = Color(0xFF1A1A1A),
    inkMuted = Color(0xFF3A3A3A),
    accent = Color(0xFF005A9E),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFE6F0FA),
    badge = Color(0xFF003E6E),
    statusGreen = Color(0xFF0A6B35),
    statusAmber = Color(0xFF8A4B00),
    statusRed = Color(0xFF9B1A1A),
    statusBlue = Color(0xFF003E6E),
    chrome = Chrome.Light,
    fontDisplay = Inter,
    fontBody = Inter,
    fontMono = Mono,
    bodySize = 20.sp,
)

fun paletteFor(key: ThemeKey, largeType: Boolean = false): AppPalette {
    val base = when (key) {
        ThemeKey.Warm -> WarmPalette
        ThemeKey.Calm -> CalmPalette
        ThemeKey.Bold -> BoldPalette
        ThemeKey.Accessible -> AccessiblePalette
    }
    return if (largeType && key != ThemeKey.Accessible) base.copy(bodySize = 20.sp) else base
}

val LocalAppPalette = compositionLocalOf { WarmPalette }
