package com.lahacks2026.pretriage.ui.result

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lahacks2026.pretriage.data.DiagnosticSummary
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.data.TriageDecision
import com.lahacks2026.pretriage.data.InsurancePlan
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.NoraBtnKind
import com.lahacks2026.pretriage.ui.components.NoraButton
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.theme.NoraColors
import com.lahacks2026.pretriage.ui.theme.NoraTheme

private data class SeverityMeta(
    val label: String,
    val tag: String,
    val actionTitle: String,
    val actionIcon: ImageVector,
    val color: (NoraColors) -> Color,
)

private fun metaFor(severity: SeverityLevel): SeverityMeta = when (severity) {
    SeverityLevel.EMERGENCY -> SeverityMeta(
        label = "Emergency", tag = "Call 911 now",
        actionTitle = "Call 911",
        actionIcon = Icons.Default.Phone,
        color = { it.statusRed },
    )
    SeverityLevel.URGENT_CARE -> SeverityMeta(
        label = "Urgent care", tag = "Go in person today",
        actionTitle = "Find urgent care",
        actionIcon = Icons.Default.LocalHospital,
        color = { it.statusAmber },
    )
    SeverityLevel.TELEHEALTH -> SeverityMeta(
        label = "Telehealth", tag = "Talk to a clinician",
        actionTitle = "Start video visit",
        actionIcon = Icons.Default.Phone,
        color = { it.statusBlue },
    )
    SeverityLevel.SELF_CARE -> SeverityMeta(
        label = "Self-care", tag = "Manage at home",
        actionTitle = "See self-care tips",
        actionIcon = Icons.Default.Home,
        color = { it.statusGreen },
    )
}

private fun headlineFor(severity: SeverityLevel): String = when (severity) {
    SeverityLevel.EMERGENCY -> "Get to an emergency room now."
    SeverityLevel.URGENT_CARE -> "Worth a same-day in-person visit."
    SeverityLevel.TELEHEALTH -> "A video visit should sort this out."
    SeverityLevel.SELF_CARE -> "Rest, fluids, and watch how you feel."
}

@Composable
fun ResultScreen(
    decision: TriageDecision,
    plan: InsurancePlan?,
    isShortCircuit: Boolean,
    diagnosticSummary: DiagnosticSummary? = null,
    diagnosticSummaryLoading: Boolean = false,
    onRestart: () -> Unit,
    onSendLabs: () -> Unit,
) {
    val c = NoraTheme.colors
    val context = LocalContext.current
    val meta = metaFor(decision.severity)
    val sev = meta.color(c)

    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 24.dp)
            .verticalScroll(scroll),
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.clickable { onRestart() }) {
                Text("← Start over", color = c.inkSoft, style = NoraTheme.typography.label)
            }
            PrivacyBadge(compact = true)
        }

        // Severity pill
        Spacer(Modifier.height(16.dp))
        SeverityPill(meta, sev)

        // Headline
        Spacer(Modifier.height(14.dp))
        Text(
            text = headlineFor(decision.severity),
            style = NoraTheme.typography.display,
            color = c.ink,
        )

        // What I'm seeing card
        Spacer(Modifier.height(22.dp))
        ReasoningCard(
            decision = decision,
            isShortCircuit = isShortCircuit,
            diagnosticSummary = diagnosticSummary,
            diagnosticSummaryLoading = diagnosticSummaryLoading,
        )

        // Primary action
        Spacer(Modifier.height(18.dp))
        ActionButton(
            actionTitle = meta.actionTitle,
            actionIcon = meta.actionIcon,
            tint = sev,
            onClick = {
                runCatching {
                    val intent = intentFor(decision, plan)
                    intent?.let { context.startActivity(it) }
                }
            },
        )

        // Optional escalate (urgent or telehealth only)
        if (decision.severity == SeverityLevel.URGENT_CARE || decision.severity == SeverityLevel.TELEHEALTH) {
            Spacer(Modifier.height(12.dp))
            NoraButton(
                onClick = onSendLabs,
                kind = NoraBtnKind.Secondary,
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
                },
            ) { Text("Send my recent labs (de-identified)") }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Nora is a guide, not a diagnosis. Trust your gut — if something feels worse than this says, seek care.",
            color = c.inkMuted,
            style = NoraTheme.typography.caption,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SeverityPill(meta: SeverityMeta, sev: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(sev.copy(alpha = 0.10f))
            .border(1.dp, sev.copy(alpha = 0.20f), CircleShape)
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(sev),
        )
        Text(
            "${meta.label.uppercase()} · ${meta.tag}",
            color = sev,
            style = NoraTheme.typography.label,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReasoningCard(
    decision: TriageDecision,
    isShortCircuit: Boolean,
    diagnosticSummary: DiagnosticSummary? = null,
    diagnosticSummaryLoading: Boolean = false,
) {
    val c = NoraTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BrandMark(size = 26.dp, bg = c.accentSoft, fg = c.accent)
            Text(
                "What I'm seeing",
                style = NoraTheme.typography.label,
                color = c.inkSoft,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(10.dp))

        // Three display modes:
        //  1. Gemini synthesis returned → "Potential diagnosis: …" + reasoning paragraph
        //  2. Synthesis still loading → small placeholder so the card isn't empty
        //  3. No synthesis (Gemini unset / failed) → fall back to the model's reasoning
        when {
            diagnosticSummary != null -> {
                Text(
                    "Potential diagnosis",
                    style = NoraTheme.typography.label,
                    color = c.inkSoft,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    diagnosticSummary.potentialDiagnosis,
                    style = NoraTheme.typography.body,
                    color = c.ink,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    diagnosticSummary.reasoning,
                    style = NoraTheme.typography.body,
                    color = c.ink,
                )
            }
            diagnosticSummaryLoading -> {
                Text(
                    "Reading back what you described…",
                    style = NoraTheme.typography.body,
                    color = c.inkSoft,
                )
            }
            else -> {
                Text(decision.reasoning, style = NoraTheme.typography.body, color = c.ink)
            }
        }

        if (decision.redFlags.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                decision.redFlags.take(3).forEach { rf ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(c.surfaceAlt)
                            .border(1.dp, c.border, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(rf.label.replace("_", " "), color = c.inkSoft, style = NoraTheme.typography.caption)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (isShortCircuit) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = c.statusRed, modifier = Modifier.size(12.dp))
                Text(
                    "Safety rule fired — bypassed model",
                    color = c.statusRed,
                    style = NoraTheme.typography.mono,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("MedGemma · on-device", color = c.inkMuted, style = NoraTheme.typography.mono)
                Text(
                    "confidence ${"%.2f".format(decision.confidence)}",
                    color = c.inkMuted,
                    style = NoraTheme.typography.mono,
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    actionTitle: String,
    actionIcon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tint)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(actionIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(actionTitle, color = Color.White, style = NoraTheme.typography.title, fontWeight = FontWeight.SemiBold)
                Text("One tap", color = Color.White.copy(alpha = 0.85f), style = NoraTheme.typography.caption)
            }
        }
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

private fun intentFor(decision: TriageDecision, plan: InsurancePlan?): Intent? {
    return when (decision.recommendedAction.intentHint) {
        IntentHint.DIAL_911 -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
        IntentHint.MAPS_QUERY_URGENT_CARE -> {
            val q = plan?.urgentCareNetworkQuery ?: "urgent care near me"
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(q)}"))
        }
        IntentHint.OPEN_TELEHEALTH_DEEP_LINK -> {
            val link = plan?.telehealthDeepLink ?: "https://teladoc.com"
            Intent(Intent.ACTION_VIEW, Uri.parse(link))
        }
        IntentHint.SHOW_SELF_CARE_TEXT -> null
    }
}
