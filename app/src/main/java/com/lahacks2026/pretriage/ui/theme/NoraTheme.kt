package com.lahacks2026.pretriage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ThemeKey { Warm, Calm, Bold, Accessible }

data class NoraColors(
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
    val statusGreen: Color,
    val statusAmber: Color,
    val statusRed: Color,
    val statusBlue: Color,
    val isDark: Boolean,
    val onAccent: Color,
)

data class NoraTypography(
    val display: TextStyle,
    val displaySmall: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodySoft: TextStyle,
    val label: TextStyle,
    val mono: TextStyle,
    val caption: TextStyle,
)

private val Warm = NoraColors(
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
    statusGreen = Color(0xFF5B7A63),
    statusAmber = Color(0xFFC98A3A),
    statusRed = Color(0xFFB3493A),
    statusBlue = Color(0xFF4A6F8A),
    isDark = false,
    onAccent = Color.White,
)

private val Calm = NoraColors(
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
    statusGreen = Color(0xFF2E7A55),
    statusAmber = Color(0xFFB27A2A),
    statusRed = Color(0xFFA83B32),
    statusBlue = Color(0xFF2F6FB0),
    isDark = false,
    onAccent = Color.White,
)

private val Bold = NoraColors(
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
    statusGreen = Color(0xFF7FC794),
    statusAmber = Color(0xFFE0A85A),
    statusRed = Color(0xFFE07565),
    statusBlue = Color(0xFF7FB1D9),
    isDark = true,
    onAccent = Color(0xFF0E1311),
)

private val Accessible = NoraColors(
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
    statusGreen = Color(0xFF0A6B35),
    statusAmber = Color(0xFF8A4B00),
    statusRed = Color(0xFF9B1A1A),
    statusBlue = Color(0xFF003E6E),
    isDark = false,
    onAccent = Color.White,
)

private fun typographyFor(key: ThemeKey, largeType: Boolean): NoraTypography {
    val bodySize = if (largeType || key == ThemeKey.Accessible) 20.sp else 17.sp
    val displayFamily = when (key) {
        ThemeKey.Warm, ThemeKey.Bold -> FontFamily.Serif
        ThemeKey.Calm, ThemeKey.Accessible -> FontFamily.SansSerif
    }
    return NoraTypography(
        display = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp),
        displaySmall = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
        title = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        body = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = bodySize, lineHeight = (bodySize.value * 1.45f).sp),
        bodySoft = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = bodySize, lineHeight = (bodySize.value * 1.5f).sp),
        label = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
        mono = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp),
        caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    )
}

private fun colorsFor(key: ThemeKey): NoraColors = when (key) {
    ThemeKey.Warm -> Warm
    ThemeKey.Calm -> Calm
    ThemeKey.Bold -> Bold
    ThemeKey.Accessible -> Accessible
}

val LocalNoraColors = staticCompositionLocalOf { Warm }
val LocalNoraTypography = staticCompositionLocalOf { typographyFor(ThemeKey.Warm, false) }

object NoraTheme {
    val colors: NoraColors
        @Composable @ReadOnlyComposable
        get() = LocalNoraColors.current

    val typography: NoraTypography
        @Composable @ReadOnlyComposable
        get() = LocalNoraTypography.current
}

@Composable
fun NoraAppTheme(
    themeKey: ThemeKey = ThemeKey.Warm,
    largeType: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = colorsFor(themeKey)
    val typography = typographyFor(themeKey, largeType)
    val m3 = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            error = colors.statusRed,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            error = colors.statusRed,
        )
    }
    CompositionLocalProvider(
        LocalNoraColors provides colors,
        LocalNoraTypography provides typography,
    ) {
        MaterialTheme(colorScheme = m3, content = content)
    }
}
