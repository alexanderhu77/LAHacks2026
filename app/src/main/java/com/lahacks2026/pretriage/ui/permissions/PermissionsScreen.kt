package com.lahacks2026.pretriage.ui.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.NoraBtnKind
import com.lahacks2026.pretriage.ui.components.NoraButton
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.theme.NoraTheme

@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val c = NoraTheme.colors
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> onContinue() }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> cameraLauncher.launch(Manifest.permission.CAMERA) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BrandMark(size = 44.dp, bg = c.accentSoft, fg = c.accent)
        Spacer(Modifier.height(18.dp))
        Text(
            "One quick step before we start",
            style = NoraTheme.typography.display,
            color = c.ink,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Voice and camera make Nora more accessible. You can use text instead — your choice, every time.",
            style = NoraTheme.typography.body,
            color = c.inkSoft,
        )

        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionCard(Icons.Default.Mic, "Microphone", "So you can speak symptoms instead of typing.")
            PermissionCard(Icons.Default.CameraAlt, "Camera", "Optional — for rashes, wounds, or moles.")
        }

        Spacer(Modifier.weight(1f))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            PrivacyBadge()
        }
        Spacer(Modifier.height(10.dp))
        NoraButton(
            onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            big = true,
        ) { Text("Allow & continue") }
        Spacer(Modifier.height(8.dp))
        NoraButton(
            onClick = onContinue,
            kind = NoraBtnKind.Ghost,
        ) { Text("Use text only") }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, body: String) {
    val c = NoraTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = NoraTheme.typography.title, color = c.ink, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(body, style = NoraTheme.typography.label, color = c.inkSoft)
        }
    }
}
