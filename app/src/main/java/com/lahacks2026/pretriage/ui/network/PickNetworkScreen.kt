package com.lahacks2026.pretriage.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lahacks2026.pretriage.data.LaCareNetwork
import com.lahacks2026.pretriage.data.LaCareNetworks
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.NoraBtnKind
import com.lahacks2026.pretriage.ui.components.NoraButton
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.theme.NoraTheme

/**
 * First user-facing localization step: pick the LA Care Medi-Cal network the user
 * is enrolled in. Selection persists for the whole session and is forwarded into
 * the find-a-provider WebView search at result time.
 */
@Composable
fun PickNetworkScreen(
    selected: LaCareNetwork?,
    onPick: (LaCareNetwork) -> Unit,
    onContinue: () -> Unit,
) {
    val c = NoraTheme.colors
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 24.dp)
            .verticalScroll(scroll),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrandMark(size = 26.dp, bg = c.accentSoft, fg = c.accent)
                Text("Nora", color = c.ink, style = NoraTheme.typography.label, fontWeight = FontWeight.SemiBold)
            }
            PrivacyBadge(compact = true)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Pick your network",
            style = NoraTheme.typography.display,
            color = c.ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Choose the LA Care Medi-Cal network you're enrolled in. We'll use it to point you to in-network providers when we recommend a visit.",
            style = NoraTheme.typography.body,
            color = c.inkSoft,
        )

        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (network in LaCareNetworks.all) {
                NetworkCard(
                    network = network,
                    isSelected = selected?.id == network.id,
                    onClick = { onPick(network) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        NoraButton(
            onClick = onContinue,
            kind = NoraBtnKind.Primary,
            enabled = selected != null,
        ) { Text("Continue") }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun NetworkCard(
    network: LaCareNetwork,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val c = NoraTheme.colors
    val borderColor = if (isSelected) c.accent else c.border
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) c.accentSoft else c.surface)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                network.displayName,
                color = c.ink,
                style = NoraTheme.typography.body,
                fontWeight = FontWeight.SemiBold,
            )
            Text(network.tagline, color = c.inkSoft, style = NoraTheme.typography.caption)
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(c.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = c.surface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
