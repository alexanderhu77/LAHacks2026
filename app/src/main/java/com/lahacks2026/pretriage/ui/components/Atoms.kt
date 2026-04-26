package com.lahacks2026.pretriage.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.ui.theme.NoraTheme

@Composable
fun PrivacyBadge(compact: Boolean = false) {
    val c = NoraTheme.colors
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(c.accentSoft)
            .border(1.dp, c.accent.copy(alpha = 0.13f), CircleShape)
            .padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 6.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = c.accentInk,
            modifier = Modifier.size(if (compact) 12.dp else 14.dp),
        )
        Text(
            text = "On-device · nothing leaves your phone",
            color = c.accentInk,
            style = NoraTheme.typography.label,
        )
    }
}

@Composable
fun BrandMark(
    size: Dp = 44.dp,
    bg: Color = NoraTheme.colors.accentSoft,
    fg: Color = NoraTheme.colors.accent,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "N",
            color = fg,
            fontWeight = FontWeight.SemiBold,
            style = NoraTheme.typography.display.copy(
                fontSize = TextUnit((size.value * 0.55f).coerceAtLeast(11f), TextUnitType.Sp)
            ),
        )
    }
}

enum class NoraBtnKind { Primary, Secondary, Ghost, Danger }

@Composable
fun NoraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    kind: NoraBtnKind = NoraBtnKind.Primary,
    big: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    leadingIcon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = NoraTheme.colors
    val (bg, fg, border) = when (kind) {
        NoraBtnKind.Primary -> Triple(c.accent, c.onAccent, Color.Transparent)
        NoraBtnKind.Secondary -> Triple(Color.Transparent, c.ink, c.border)
        NoraBtnKind.Ghost -> Triple(Color.Transparent, c.inkSoft, Color.Transparent)
        NoraBtnKind.Danger -> Triple(c.statusRed, Color.White, Color.Transparent)
    }
    val pad = if (big) PaddingValues(horizontal = 22.dp, vertical = 18.dp)
              else PaddingValues(horizontal = 18.dp, vertical = 14.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, SolidColor(border)), shape)
            .clickable(enabled = enabled) { onClick() }
            .padding(pad),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) leadingIcon()
            ProvideTextStyle(
                value = NoraTheme.typography.body.copy(
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (big) 18.sp else 16.sp,
                ),
                content = content,
            )
        }
    }
}
