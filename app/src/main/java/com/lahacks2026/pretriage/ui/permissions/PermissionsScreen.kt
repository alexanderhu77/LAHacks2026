package com.lahacks2026.pretriage.ui.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.ui.components.AppButton
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.BtnKind
import com.lahacks2026.pretriage.ui.components.DisplayText
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette

@Composable
fun PermissionsScreen(
    onContinue: () -> Unit,
) {
    val palette = LocalAppPalette.current

    val multiplePerms = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ -> onContinue() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Column(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp, start = 4.dp, end = 4.dp)) {
            BrandMark(size = 44)
            Spacer(Modifier.height(18.dp))
            DisplayText("One quick step before we start", size = 28.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Voice and camera make Nora more accessible. You can use text instead — your choice, every time.",
                color = palette.inkSoft,
                fontSize = 16.sp,
                fontFamily = palette.fontBody,
                lineHeight = 22.sp,
            )
        }

        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionCard(Icons.Filled.Mic, "Microphone", "So you can speak symptoms instead of typing.")
            PermissionCard(Icons.Filled.CameraAlt, "Camera", "Optional — for rashes, wounds, or moles.")
        }

        Spacer(Modifier.weight(1f))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PrivacyBadge()
            }
            AppButton(
                text = "Allow & continue",
                big = true,
                onClick = {
                    multiplePerms.launch(
                        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                    )
                },
            )
            AppButton(text = "Use text only", kind = BtnKind.Ghost, onClick = onContinue)
        }
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, body: String) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, shape)
            .border(1.dp, palette.border, shape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(palette.accentSoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.W600, fontSize = 16.sp, color = palette.ink, fontFamily = palette.fontBody)
            Spacer(Modifier.height(2.dp))
            Text(body, color = palette.inkSoft, fontSize = 14.sp, fontFamily = palette.fontBody)
        }
    }
}
