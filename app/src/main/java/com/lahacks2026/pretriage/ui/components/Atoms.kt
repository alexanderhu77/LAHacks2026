package com.lahacks2026.pretriage.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.ui.theme.AppPalette
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette

@Composable
fun BrandMark(
    size: Int = 44,
    palette: AppPalette = LocalAppPalette.current,
    bg: Color = palette.accentSoft,
    fg: Color = palette.accent,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(bg, RoundedCornerShape((size / 4).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size((size * 0.42f).dp)
                .background(fg, CircleShape),
        )
        Box(
            modifier = Modifier
                .size((size * 0.18f).dp)
                .background(bg, CircleShape)
                .padding(0.dp),
        )
    }
}

@Composable
fun PrivacyBadge(
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    palette: AppPalette = LocalAppPalette.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(palette.accentSoft, RoundedCornerShape(999.dp))
            .border(1.dp, palette.accent.copy(alpha = 0.13f), RoundedCornerShape(999.dp))
            .padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 6.dp else 8.dp,
            ),
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = palette.accentInk,
            modifier = Modifier.size(14.dp),
        )
        Text(
            "On-device · nothing leaves your phone",
            color = palette.accentInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            fontFamily = palette.fontBody,
        )
    }
}

@Composable
fun DisplayText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 28.sp,
    weight: FontWeight = FontWeight.W500,
    align: TextAlign = TextAlign.Start,
    palette: AppPalette = LocalAppPalette.current,
) {
    Text(
        text = text,
        modifier = modifier,
        color = palette.ink,
        fontFamily = palette.fontDisplay,
        fontWeight = weight,
        fontSize = size,
        lineHeight = size * 1.15f,
        textAlign = align,
        style = TextStyle(letterSpacing = (-0.01).sp),
    )
}

enum class BtnKind { Primary, Secondary, Ghost, Danger }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Primary,
    big: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    palette: AppPalette = LocalAppPalette.current,
) {
    val (bg, fg, borderColor) = when (kind) {
        BtnKind.Primary -> Triple(
            palette.accent,
            if (palette.chrome == com.lahacks2026.pretriage.ui.theme.Chrome.Dark) palette.accentInk else Color.White,
            Color.Transparent,
        )
        BtnKind.Secondary -> Triple(Color.Transparent, palette.ink, palette.border)
        BtnKind.Ghost -> Triple(Color.Transparent, palette.inkSoft, Color.Transparent)
        BtnKind.Danger -> Triple(palette.statusRed, Color.White, Color.Transparent)
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (big) 22.dp else 18.dp,
                vertical = if (big) 18.dp else 14.dp,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(if (big) 22.dp else 18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            color = fg,
            fontFamily = palette.fontBody,
            fontWeight = FontWeight.W600,
            fontSize = if (big) 18.sp else 16.sp,
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    tag: String,
    color: Color,
    modifier: Modifier = Modifier,
    palette: AppPalette = LocalAppPalette.current,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .background(color.copy(alpha = 0.09f), shape)
            .border(1.dp, color.copy(alpha = 0.20f), shape)
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(
            "${label.uppercase()} · $tag",
            color = color,
            fontWeight = FontWeight.W600,
            fontSize = 14.sp,
            fontFamily = palette.fontBody,
        )
    }
}
