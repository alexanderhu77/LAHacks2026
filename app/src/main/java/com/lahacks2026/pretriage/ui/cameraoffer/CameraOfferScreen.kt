package com.lahacks2026.pretriage.ui.cameraoffer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.ui.components.AppButton
import com.lahacks2026.pretriage.ui.components.BtnKind
import com.lahacks2026.pretriage.ui.components.DisplayText
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette

@Composable
fun CameraOfferScreen(
    onSkip: () -> Unit,
    onCapture: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Column(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)) {
            Text(
                "OPTIONAL",
                color = palette.inkMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.4.sp,
                fontFamily = palette.fontBody,
            )
            Spacer(Modifier.height(4.dp))
            DisplayText("Want to show me?", size = 28.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "A photo helps me look at rashes, wounds, eyes, or moles. Skip if it doesn't apply.",
                color = palette.inkSoft,
                fontSize = 16.sp,
                fontFamily = palette.fontBody,
                lineHeight = 22.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
        val shape = RoundedCornerShape(18.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface, shape)
                .border(1.dp, palette.border, shape)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                Text(
                    "The image stays on your device.",
                    color = palette.inkSoft,
                    fontSize = 14.sp,
                    fontFamily = palette.fontBody,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "HELPFUL FOR THINGS LIKE",
                color = palette.inkMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.5.sp,
                fontFamily = palette.fontBody,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "skin or rashes · eyes · wounds · moles",
                color = palette.inkSoft,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                fontFamily = palette.fontBody,
            )
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppButton(
                text = "Take a photo",
                big = true,
                icon = Icons.Filled.CameraAlt,
                onClick = onCapture,
            )
            AppButton(
                text = "Skip — no photo needed",
                kind = BtnKind.Secondary,
                onClick = onSkip,
            )
        }
    }
}
