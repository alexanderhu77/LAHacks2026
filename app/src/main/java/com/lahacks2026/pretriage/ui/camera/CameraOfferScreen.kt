package com.lahacks2026.pretriage.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.ui.components.NoraBtnKind
import com.lahacks2026.pretriage.ui.components.NoraButton
import com.lahacks2026.pretriage.ui.theme.NoraTheme

@Composable
fun CameraOfferScreen(
    onTakePhoto: () -> Unit,
    onSkip: () -> Unit,
) {
    val c = NoraTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "OPTIONAL",
            color = c.inkMuted,
            style = NoraTheme.typography.label.copy(fontSize = 13.sp, letterSpacing = 0.4.sp),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text("Want to show me?", style = NoraTheme.typography.display, color = c.ink)
        Spacer(Modifier.height(10.dp))
        Text(
            "A photo helps me look at rashes, wounds, eyes, or moles. Skip if it doesn't apply.",
            style = NoraTheme.typography.body, color = c.inkSoft,
        )

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(18.dp))
                .padding(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text("The image stays on your device.", color = c.inkSoft, style = NoraTheme.typography.label)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "HELPFUL FOR THINGS LIKE",
                color = c.inkMuted,
                style = NoraTheme.typography.caption.copy(letterSpacing = 0.5.sp),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "skin or rashes · eyes · wounds · moles",
                color = c.inkSoft,
                style = NoraTheme.typography.body,
            )
        }

        Spacer(Modifier.weight(1f))

        NoraButton(
            onClick = onTakePhoto,
            big = true,
            leadingIcon = {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(20.dp))
            },
        ) { Text("Take a photo") }
        Spacer(Modifier.height(10.dp))
        NoraButton(
            onClick = onSkip,
            kind = NoraBtnKind.Secondary,
        ) { Text("Skip — no photo needed", textAlign = TextAlign.Center) }
        Spacer(Modifier.height(16.dp))
    }
}
