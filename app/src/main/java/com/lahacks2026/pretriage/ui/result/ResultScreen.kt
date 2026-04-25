package com.lahacks2026.pretriage.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.ui.components.AppButton
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.BtnKind
import com.lahacks2026.pretriage.ui.components.DisplayText
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.components.StatusPill
import com.lahacks2026.pretriage.ui.theme.AppPalette
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette

private data class SeverityViz(
    val label: String,
    val tag: String,
    val color: (AppPalette) -> Color,
    val headlineFallback: String,
    val actionLabel: String,
    val actionIcon: ImageVector,
)

private fun vizFor(severity: SeverityLevel): SeverityViz = when (severity) {
    SeverityLevel.EMERGENCY -> SeverityViz(
        "Emergency", "Call 911 now", { it.statusRed },
        "Get to an emergency room now.",
        "Call 911", Icons.Filled.Phone,
    )
    SeverityLevel.URGENT_CARE -> SeverityViz(
        "Urgent care", "Go in person today", { it.statusAmber },
        "Worth a same-day in-person visit.",
        "Find urgent care", Icons.Filled.LocalHospital,
    )
    SeverityLevel.TELEHEALTH -> SeverityViz(
        "Telehealth", "Talk to a clinician", { it.statusBlue },
        "A video visit should sort this out.",
        "Start video visit", Icons.Filled.Phone,
    )
    SeverityLevel.SELF_CARE -> SeverityViz(
        "Self-care", "Manage at home", { it.statusGreen },
        "Rest, fluids, and watch how you feel.",
        "See self-care tips", Icons.Filled.Favorite,
    )
}

@Composable
fun ResultScreen(
    decision: TriageDecision,
    emergencyShortCircuit: Boolean,
    onRestart: () -> Unit,
    onAction: (IntentHint) -> Unit,
    onUploadDocs: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val viz = vizFor(decision.severity)
    val color = viz.color(palette)
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(scroll)
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "← Start over",
                modifier = Modifier.clickable(onClick = onRestart),
                color = palette.inkSoft,
                fontSize = 14.sp,
                fontFamily = palette.fontBody,
            )
            PrivacyBadge(compact = true)
        }

        Spacer(Modifier.height(16.dp))
        StatusPill(label = viz.label, tag = viz.tag, color = color)
        Spacer(Modifier.height(14.dp))
        DisplayText(viz.headlineFallback, size = 28.sp)

        Spacer(Modifier.height(22.dp))
        ReasoningCard(decision = decision, emergencyShortCircuit = emergencyShortCircuit)

        Spacer(Modifier.height(18.dp))
        PrimaryActionButton(
            label = viz.actionLabel,
            icon = viz.actionIcon,
            color = color,
            onClick = { onAction(decision.recommendedAction.intentHint) },
        )

        if (decision.severity == SeverityLevel.URGENT_CARE || decision.severity == SeverityLevel.TELEHEALTH) {
            Spacer(Modifier.height(12.dp))
            AppButton(
                text = "Send my recent labs (de-identified)",
                kind = BtnKind.Secondary,
                icon = Icons.Filled.Description,
                onClick = onUploadDocs,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Nora is a guide, not a diagnosis. Trust your gut — if something feels worse than this says, seek care.",
            color = palette.inkMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFamily = palette.fontBody,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReasoningCard(decision: TriageDecision, emergencyShortCircuit: Boolean) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, shape)
            .border(1.dp, palette.border, shape)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrandMark(size = 26)
            Text("What I'm seeing", fontSize = 13.sp, color = palette.inkSoft, fontWeight = FontWeight.W600, fontFamily = palette.fontBody)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            decision.reasoning,
            color = palette.ink,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontFamily = palette.fontBody,
        )
        if (decision.redFlags.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            FlowChips(decision.redFlags.map { it.label })
        }
        Spacer(Modifier.height(12.dp))
        if (emergencyShortCircuit) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = palette.statusRed, modifier = Modifier.size(12.dp))
                Text(
                    "Safety rule fired — bypassed model",
                    color = palette.statusRed,
                    fontSize = 11.sp,
                    fontFamily = palette.fontMono,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("MedGemma · on-device", color = palette.inkMuted, fontSize = 11.sp, fontFamily = palette.fontMono)
                Text(
                    "confidence ${"%.2f".format(decision.confidence)}",
                    color = palette.inkMuted, fontSize = 11.sp, fontFamily = palette.fontMono,
                )
            }
        }
    }
}

@Composable
private fun FlowChips(items: List<String>) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(99.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { label ->
            Box(
                modifier = Modifier
                    .background(palette.surfaceAlt, shape)
                    .border(1.dp, palette.border, shape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(label, color = palette.inkSoft, fontSize = 12.sp, fontFamily = palette.fontBody)
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.W600, fontFamily = palette.fontBody)
                Text("One tap", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontFamily = palette.fontBody)
            }
        }
        Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
