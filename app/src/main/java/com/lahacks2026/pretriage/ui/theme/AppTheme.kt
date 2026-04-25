package com.lahacks2026.pretriage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun AppTheme(
    palette: AppPalette,
    content: @Composable () -> Unit
) {
    val colorScheme = when (palette.chrome) {
        Chrome.Dark -> darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.accentInk,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.accentInk,
            background = palette.bg,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.inkSoft,
            outline = palette.border,
            error = palette.statusRed,
        )
        Chrome.Light -> lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.accentInk,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.accentInk,
            background = palette.bg,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.inkSoft,
            outline = palette.border,
            error = palette.statusRed,
        )
    }

    val body = TextStyle(fontFamily = palette.fontBody, fontSize = palette.bodySize)
    val typography = Typography(
        bodyLarge = body,
        bodyMedium = body.copy(fontSize = 14.sp),
        bodySmall = body.copy(fontSize = 13.sp),
        titleLarge = body.copy(fontFamily = palette.fontDisplay, fontSize = 22.sp, fontWeight = FontWeight.W600),
        titleMedium = body.copy(fontWeight = FontWeight.W600, fontSize = 16.sp),
        titleSmall = body.copy(fontWeight = FontWeight.W600, fontSize = 14.sp),
        labelLarge = body.copy(fontWeight = FontWeight.W600),
        labelMedium = body.copy(fontWeight = FontWeight.W500, fontSize = 12.sp),
        labelSmall = body.copy(fontSize = 11.sp),
    )

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
