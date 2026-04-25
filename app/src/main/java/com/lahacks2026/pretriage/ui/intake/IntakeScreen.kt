package com.lahacks2026.pretriage.ui.intake

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.data.DemoScenarios
import com.lahacks2026.pretriage.ui.components.AppButton
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.BtnKind
import com.lahacks2026.pretriage.ui.components.DisplayText
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IntakeScreen(
    transcript: String,
    recording: Boolean,
    onTranscriptChange: (String) -> Unit,
    onRecordingChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val palette = LocalAppPalette.current
    var showText by remember { mutableStateOf(false) }
    var showDemoPicker by remember { mutableStateOf(false) }

    // Tick drives waveform animation while recording
    val infinite = rememberInfiniteTransition(label = "wave")
    val tick by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 100_000)),
        label = "wave-tick",
    )

    // Mic ring pulse
    val ringPulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(animation = tween(1400)),
        label = "ringPulse",
    )

    // Demo: while "recording" with no real Whisper backend, simulate a stream so the UI shows.
    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        val phrases = listOf(
            "I've had a",
            "I've had a sore throat",
            "I've had a sore throat for three days",
            "I've had a sore throat for three days and now my ear",
            "I've had a sore throat for three days and now my ear is hurting too.",
        )
        // Only auto-fill if user started from an empty/short transcript.
        if (transcript.length > 30) return@LaunchedEffect
        for (p in phrases) {
            kotlinx.coroutines.delay(500)
            if (!recording) break
            onTranscriptChange(p)
        }
        onRecordingChange(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandMark(size = 32)
                Column {
                    Text("Nora", fontWeight = FontWeight.W600, fontSize = 15.sp, color = palette.ink, fontFamily = palette.fontBody)
                    Text("Pre-triage co-pilot", fontSize = 12.sp, color = palette.inkMuted, fontFamily = palette.fontBody)
                }
            }
            PrivacyBadge(compact = true)
        }

        Spacer(Modifier.height(22.dp))
        DisplayText("What's going on today?", size = 30.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap and tell me in your own words — when it started, where it hurts, anything else.",
            color = palette.inkSoft,
            fontSize = 16.sp,
            fontFamily = palette.fontBody,
            lineHeight = 22.sp,
        )

        // Transcript card
        Spacer(Modifier.height(24.dp))
        TranscriptCard(transcript = transcript, recording = recording)

        // Waveform
        Spacer(Modifier.height(24.dp))
        Waveform(active = recording, tick = tick.toInt())

        Spacer(Modifier.weight(1f))

        // Mic button
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (recording) {
                        Box(
                            modifier = Modifier
                                .size((116f * ringPulse).dp)
                                .border(2.dp, palette.statusRed.copy(alpha = 0.33f), CircleShape),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .shadow(16.dp, CircleShape, clip = false)
                            .background(if (recording) palette.statusRed else palette.accent, CircleShape)
                            .clickable { onRecordingChange(!recording) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (recording) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(36.dp))
                        } else {
                            Icon(Icons.Filled.Mic, contentDescription = "Speak", tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        recording -> "Tap to stop"
                        transcript.isNotBlank() -> "Tap to add more"
                        else -> "Tap to speak"
                    },
                    color = palette.inkSoft,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = palette.fontBody,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppButton(
                text = "Type instead",
                kind = BtnKind.Secondary,
                icon = Icons.Filled.Keyboard,
                onClick = { showText = true },
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = "Continue",
                icon = Icons.Filled.ArrowForward,
                onClick = onContinue,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { showDemoPicker = true }) {
                Icon(Icons.Filled.List, contentDescription = null, tint = palette.inkMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Demo scenarios", color = palette.inkMuted, fontSize = 12.sp, fontFamily = palette.fontBody)
            }
            TextButton(onClick = onOpenDiagnostics) {
                Text("Diagnostics", color = palette.inkMuted, fontSize = 12.sp, fontFamily = palette.fontBody)
            }
        }
    }

    if (showText) {
        TypeOverlay(
            initial = transcript,
            onClose = { showText = false },
            onSave = { v -> onTranscriptChange(v); showText = false },
        )
    }

    if (showDemoPicker) {
        AlertDialog(
            onDismissRequest = { showDemoPicker = false },
            containerColor = palette.surface,
            titleContentColor = palette.ink,
            textContentColor = palette.ink,
            title = { Text("Demo scenarios") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DemoScenarios.All.forEach { sc ->
                        AppButton(
                            text = sc.title,
                            kind = BtnKind.Secondary,
                            onClick = {
                                onTranscriptChange(sc.initialSymptom)
                                showDemoPicker = false
                            },
                        )
                    }
                    AppButton(
                        text = "Sore throat (self-care)",
                        kind = BtnKind.Secondary,
                        onClick = {
                            onTranscriptChange("I've had a sore throat for three days and now my ear is hurting too.")
                            showDemoPicker = false
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDemoPicker = false }) { Text("Close", color = palette.accent) }
            },
        )
    }
}

@Composable
private fun TranscriptCard(transcript: String, recording: Boolean) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(16.dp)
    val hasText = transcript.isNotEmpty()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .background(if (hasText) palette.surface else Color.Transparent, shape)
            .border(1.dp, palette.border, shape)
            .padding(16.dp),
    ) {
        Text(
            text = if (hasText) transcript else if (recording) "Listening…" else "Your words will appear here.",
            color = if (hasText) palette.ink else palette.inkMuted,
            fontSize = 17.sp,
            lineHeight = 26.sp,
            fontFamily = palette.fontBody,
            fontStyle = if (hasText) FontStyle.Normal else FontStyle.Italic,
        )
    }
}

@Composable
private fun Waveform(active: Boolean, tick: Int) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(28) { i ->
            val ampFactor = if (active) {
                0.3f + 0.7f * abs(sin((tick + i * 3) * 0.4f).toFloat() * cos(i * 0.7f).toFloat())
            } else {
                0.18f
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(ampFactor)
                    .background(if (active) palette.accent else palette.border, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun TypeOverlay(initial: String, onClose: () -> Unit, onSave: (String) -> Unit) {
    val palette = LocalAppPalette.current
    var v by remember { mutableStateOf(initial) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = palette.ink)
                }
                Text("Type your symptoms", fontWeight = FontWeight.W600, color = palette.ink, fontFamily = palette.fontBody)
                TextButton(onClick = { onSave(v) }) {
                    Text("Done", color = palette.accent, fontWeight = FontWeight.W600, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = v,
                onValueChange = { v = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(14.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                textStyle = TextStyle(
                    color = palette.ink,
                    fontSize = 17.sp,
                    fontFamily = palette.fontBody,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions.Default,
                decorationBox = { inner ->
                    if (v.isEmpty()) {
                        Text(
                            "What are you experiencing?",
                            color = palette.inkMuted,
                            fontSize = 17.sp,
                            fontFamily = palette.fontBody,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                    inner()
                },
            )
        }
    }
}
