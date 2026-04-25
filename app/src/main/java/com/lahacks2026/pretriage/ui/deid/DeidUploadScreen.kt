package com.lahacks2026.pretriage.ui.deid

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.DeidPhase
import com.lahacks2026.pretriage.ui.components.AppButton
import com.lahacks2026.pretriage.ui.components.DisplayText
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette

@Composable
fun DeidUploadScreen(
    phase: DeidPhase,
    onBack: () -> Unit,
    onPhaseChange: (DeidPhase) -> Unit,
    onDone: () -> Unit,
) {
    val palette = LocalAppPalette.current

    LaunchedEffect(phase) {
        val next = when (phase) {
            DeidPhase.Extracting -> DeidPhase.Scrubbing
            DeidPhase.Scrubbing -> DeidPhase.Sending
            DeidPhase.Sending -> DeidPhase.Done
            else -> null
        } ?: return@LaunchedEffect
        kotlinx.coroutines.delay(if (phase == DeidPhase.Sending) 1100 else 900)
        onPhaseChange(next)
    }

    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(scroll)
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
            }
            Text("Send to provider", fontWeight = FontWeight.W600, fontSize = 16.sp, color = palette.ink, fontFamily = palette.fontBody)
        }

        Spacer(Modifier.height(12.dp))
        DisplayText("Lab report · April 22", size = 24.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Anything personal gets replaced with placeholders before it leaves your phone.",
            color = palette.inkSoft,
            fontSize = 14.sp,
            fontFamily = palette.fontBody,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(18.dp))
        DocumentPreview(scrubbed = phase != DeidPhase.Preview)

        Spacer(Modifier.height(18.dp))
        PipelineList(phase = phase)

        Spacer(Modifier.height(20.dp))
        when (phase) {
            DeidPhase.Preview -> {
                AppButton(
                    text = "Send de-identified",
                    big = true,
                    icon = Icons.Filled.Shield,
                    onClick = { onPhaseChange(DeidPhase.Extracting) },
                )
            }
            DeidPhase.Done -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val shape = RoundedCornerShape(12.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.statusGreen.copy(alpha = 0.13f), shape)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = palette.statusGreen, modifier = Modifier.size(18.dp))
                        Text(
                            "Sent to PediaCare. They'll reply within an hour.",
                            color = palette.statusGreen,
                            fontWeight = FontWeight.W500,
                            fontSize = 14.sp,
                            fontFamily = palette.fontBody,
                        )
                    }
                    AppButton(text = "Done", onClick = onDone)
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun DocumentPreview(scrubbed: Boolean) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, shape)
            .border(1.dp, palette.border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        DocLine(label = "Patient", raw = "Maria Hernandez", placeholder = "[PATIENT_NAME_1]", scrubbed = scrubbed)
        DocLine(label = "DOB", raw = "1979-03-14", placeholder = "[DOB_1]", scrubbed = scrubbed)
        DocLine(label = "MRN", raw = "A-2849-1077", placeholder = "[MRN_1]", scrubbed = scrubbed)
        Spacer(Modifier.height(8.dp))
        LabRow(label = "Hemoglobin A1c", value = "6.7%")
        LabRow(label = "Fasting glucose", value = "132 mg/dL")
        LabRow(label = "LDL cholesterol", value = "148 mg/dL")
        Spacer(Modifier.height(8.dp))
        DocLine(label = "Provider", raw = "Dr. James Chen", placeholder = "[PROVIDER_1]", scrubbed = scrubbed)
    }
}

@Composable
private fun DocLine(label: String, raw: String, placeholder: String, scrubbed: Boolean) {
    val palette = LocalAppPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = palette.inkSoft,
            fontSize = 12.sp,
            fontFamily = palette.fontMono,
            modifier = Modifier.width(70.dp),
        )
        Text(
            if (scrubbed) placeholder else raw,
            color = if (scrubbed) palette.accent else palette.ink,
            fontSize = 12.sp,
            fontFamily = palette.fontMono,
            modifier = Modifier
                .background(if (scrubbed) palette.accentSoft else Color.Transparent)
                .padding(horizontal = if (scrubbed) 4.dp else 0.dp),
        )
    }
}

@Composable
private fun LabRow(label: String, value: String) {
    val palette = LocalAppPalette.current
    Row {
        Text("$label · ", color = palette.inkSoft, fontSize = 12.sp, fontFamily = palette.fontMono)
        Text(value, color = palette.ink, fontSize = 12.sp, fontFamily = palette.fontMono)
    }
}

private data class PipeRow(val phase: DeidPhase, val label: String, val mono: String)

private val pipeRows = listOf(
    PipeRow(DeidPhase.Extracting, "Reading the document", "medgemma → JSON"),
    PipeRow(DeidPhase.Scrubbing, "Scrubbing personal info", "tanaos → [PATIENT_NAME_1]"),
    PipeRow(DeidPhase.Sending, "Sending to your provider", "POST · de-identified only"),
)

@Composable
private fun PipelineList(phase: DeidPhase) {
    val palette = LocalAppPalette.current
    val current = phase.ordinal // Preview=0, Extracting=1, Scrubbing=2, Sending=3, Done=4

    val infinite = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infinite.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000)),
        label = "blink-a",
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pipeRows.forEachIndexed { idx, row ->
            val rowOrdinal = row.phase.ordinal // 1, 2, 3
            val state = when {
                phase == DeidPhase.Done || current > rowOrdinal -> "done"
                current == rowOrdinal -> "active"
                else -> "pending"
            }
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (state == "active") palette.accentSoft else palette.surface, shape)
                    .border(1.dp, palette.border, shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(if (state == "done") palette.accent else Color.Transparent, CircleShape)
                        .border(
                            if (state == "done") 0.dp else 2.dp,
                            if (state == "done") Color.Transparent else palette.inkMuted,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when (state) {
                        "done" -> Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                        "active" -> Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(palette.accent.copy(alpha = blinkAlpha), CircleShape)
                        )
                        else -> Unit
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        row.label,
                        color = palette.ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = palette.fontBody,
                    )
                    Text(
                        row.mono,
                        color = palette.inkMuted,
                        fontSize = 11.sp,
                        fontFamily = palette.fontMono,
                    )
                }
            }
        }
    }
}
