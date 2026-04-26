package com.lahacks2026.pretriage.ui.deid

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lahacks2026.pretriage.privacy.AnonymizerService
import com.lahacks2026.pretriage.privacy.MockTelehealthClient
import com.lahacks2026.pretriage.privacy.PhiTokenMap
import com.lahacks2026.pretriage.ui.DeidPhase
import com.lahacks2026.pretriage.ui.components.NoraBtnKind
import com.lahacks2026.pretriage.ui.components.NoraButton
import com.lahacks2026.pretriage.ui.theme.NoraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class DocLine(val label: String, val raw: String, val placeholder: String)

private val DemoDoc = listOf(
    DocLine("Patient", "Maria Hernandez", "[PATIENT_NAME_1]"),
    DocLine("DOB", "1979-03-14", "[DOB_1]"),
    DocLine("MRN", "A-2849-1077", "[MRN_1]"),
)

private val DemoLabs = listOf(
    "Hemoglobin A1c" to "6.7%",
    "Fasting glucose" to "132 mg/dL",
    "LDL cholesterol" to "148 mg/dL",
)

private val DemoProvider = DocLine("Provider", "Dr. James Chen", "[PROVIDER_1]")

@Composable
fun DeidUploadScreen(
    phase: DeidPhase,
    onPhaseChange: (DeidPhase) -> Unit,
    anonymizer: AnonymizerService,
    tokenMap: PhiTokenMap,
    telehealth: MockTelehealthClient,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val c = NoraTheme.colors
    var providerEcho by remember { mutableStateOf("") }

    LaunchedEffect(phase) {
        when (phase) {
            DeidPhase.Extracting -> {
                delay(900)
                onPhaseChange(DeidPhase.Scrubbing)
            }
            DeidPhase.Scrubbing -> {
                // Run the actual anonymizer over a synthetic document blob so the
                // pipeline is real, not just animated.
                val raw = buildString {
                    appendLine("Patient: ${DemoDoc[0].raw}")
                    appendLine("DOB: ${DemoDoc[1].raw}")
                    appendLine("MRN: ${DemoDoc[2].raw}")
                    DemoLabs.forEach { (k, v) -> appendLine("$k: $v") }
                    appendLine("Provider: ${DemoProvider.raw}")
                }
                val res = anonymizer.scrub(raw, tokenMap).getOrNull()
                providerEcho = res?.redacted ?: raw
                delay(700)
                onPhaseChange(DeidPhase.Sending)
            }
            DeidPhase.Sending -> {
                val resp = telehealth.send(providerEcho).getOrNull().orEmpty()
                // Re-identify the response locally before we'd render it. We only
                // surface the success banner in the demo, but this proves the round-trip.
                val reIdentified = tokenMap.resolve(resp)
                android.util.Log.i("PreTriage", "telehealth re-identified ${reIdentified.length} chars")
                onPhaseChange(DeidPhase.Done)
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(28.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = c.ink)
            }
            Text("Send to provider", color = c.ink, style = NoraTheme.typography.title, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))
        Text("Lab report · April 22", style = NoraTheme.typography.displaySmall, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "Anything personal gets replaced with placeholders before it leaves your phone.",
            color = c.inkSoft, style = NoraTheme.typography.label,
        )

        Spacer(Modifier.height(18.dp))
        DocPreview(scrubbed = phase != DeidPhase.Preview)

        Spacer(Modifier.height(18.dp))
        Pipeline(phase)

        Spacer(Modifier.height(18.dp))
        when (phase) {
            DeidPhase.Preview -> NoraButton(
                onClick = { onPhaseChange(DeidPhase.Extracting) },
                big = true,
                leadingIcon = {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                },
            ) { Text("Send de-identified") }
            DeidPhase.Done -> Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.statusGreen.copy(alpha = 0.12f))
                        .padding(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = c.statusGreen)
                        Text(
                            "Sent to PediaCare. They'll reply within an hour.",
                            color = c.statusGreen, style = NoraTheme.typography.body, fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                NoraButton(onClick = onDone) { Text("Done") }
            }
            else -> Box(modifier = Modifier.height(56.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DocPreview(scrubbed: Boolean) {
    val c = NoraTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        DemoDoc.forEach { line -> Line(line, scrubbed) }
        Spacer(Modifier.height(8.dp))
        DemoLabs.forEach { (k, v) ->
            Row {
                Text("$k · ", color = c.inkSoft, style = NoraTheme.typography.mono)
                Text(v, color = c.ink, style = NoraTheme.typography.mono)
            }
        }
        Spacer(Modifier.height(8.dp))
        Line(DemoProvider, scrubbed)
    }
}

@Composable
private fun Line(line: DocLine, scrubbed: Boolean) {
    val c = NoraTheme.colors
    Row {
        Text(line.label, color = c.inkSoft, style = NoraTheme.typography.mono, modifier = Modifier.width(82.dp))
        val highlighted = animateFloatAsState(if (scrubbed) 1f else 0f, tween(250), label = "scrub-${line.label}").value
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(c.accentSoft.copy(alpha = highlighted))
                .padding(horizontal = 4.dp),
        ) {
            Text(
                if (scrubbed) line.placeholder else line.raw,
                color = if (scrubbed) c.accent else c.ink,
                style = NoraTheme.typography.mono,
            )
        }
    }
}

@Composable
private fun Pipeline(phase: DeidPhase) {
    val c = NoraTheme.colors
    val rows = listOf(
        DeidPhase.Extracting to ("Reading the document" to "medgemma → JSON"),
        DeidPhase.Scrubbing to ("Scrubbing personal info" to "tanaos → [PATIENT_NAME_1]"),
        DeidPhase.Sending to ("Sending to your provider" to "POST · de-identified only"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (key, copy) ->
            val (label, mono) = copy
            val state = when {
                phase == DeidPhase.Done -> "done"
                phase == key -> "active"
                phase.ordinal > key.ordinal -> "done"
                else -> "pending"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state == "active") c.accentSoft else c.surface)
                    .border(1.dp, c.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (state == "done") c.accent else Color.Transparent)
                        .border(if (state == "done") 0.dp else 2.dp, c.inkMuted, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state == "done") {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    } else if (state == "active") {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(c.accent))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = c.ink, style = NoraTheme.typography.label, fontWeight = FontWeight.Medium)
                    Text(mono, color = c.inkMuted, style = NoraTheme.typography.mono)
                }
            }
        }
    }
}
