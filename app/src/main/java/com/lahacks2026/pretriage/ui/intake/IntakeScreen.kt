package com.lahacks2026.pretriage.ui.intake

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lahacks2026.pretriage.ml.whisper.AudioSampler
import com.lahacks2026.pretriage.ml.whisper.WhisperFeature
import com.lahacks2026.pretriage.triage.EmergencyShortCircuit
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.NoraBtnKind
import com.lahacks2026.pretriage.ui.components.NoraButton
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.theme.NoraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "IntakeScreen"

private sealed interface MicState {
    data object Idle : MicState
    data object Recording : MicState
    data class Transcribing(val samples: Int) : MicState
    data class Error(val message: String) : MicState
}

@Composable
fun IntakeScreen(
    transcript: String,
    hasImage: Boolean,
    onTranscriptChange: (String) -> Unit,
    onContinue: () -> Unit,
    onCamera: () -> Unit,
    onEmergencyShortCircuit: () -> Unit,
) {
    val c = NoraTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var micState by remember { mutableStateOf<MicState>(MicState.Idle) }
    var typing by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }

    val recording = micState is MicState.Recording

    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMic = granted }

    val whisper = remember { WhisperFeature(context.applicationContext) }
    // True once Whisper has actually been invoked. Without this, calling
    // whisper.close() on dispose would trigger lazy init (model load + native
    // setup) on the main thread → ANR, even when the user never tapped the mic.
    val whisperUsed = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val sampler = remember {
        AudioSampler { pcm ->
            val started = System.currentTimeMillis()
            Log.i(TAG, "audio captured: ${pcm.size} samples")
            micState = MicState.Transcribing(pcm.size)
            scope.launch {
                runCatching {
                    whisperUsed.set(true)
                    val text = withContext(Dispatchers.IO) { whisper.run(pcm) }
                    val elapsed = System.currentTimeMillis() - started
                    Log.i(TAG, "transcribed ${pcm.size}/${elapsed}ms: $text")
                    val merged = if (transcript.isBlank()) text.trim()
                        else "${transcript.trim()} ${text.trim()}".trim()
                    onTranscriptChange(merged)
                    micState = MicState.Idle
                }.onFailure {
                    Log.e(TAG, "transcription failed", it)
                    micState = MicState.Error(it.message ?: "transcription failed")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // AudioSampler is cheap to release on the main thread.
            runCatching { sampler.release() }
            // Whisper close must NOT touch the main thread: the encoder/decoder
            // properties are `by lazy`, so close() triggers full init if the
            // user never spoke. Punt to a worker thread either way.
            if (whisperUsed.get()) {
                Thread { runCatching { whisper.close() } }.apply { isDaemon = true }.start()
            }
        }
    }

    LaunchedEffect(recording) {
        while (recording) {
            delay(100)
            tick += 1
        }
    }

    // Emergency short-circuit on every transcript change.
    LaunchedEffect(transcript) {
        if (transcript.isNotBlank() && EmergencyShortCircuit.match(transcript) != null) {
            onEmergencyShortCircuit()
        }
    }

    if (typing) {
        TypeOverlay(
            initial = transcript,
            onClose = { typing = false },
            onSave = {
                onTranscriptChange(it)
                typing = false
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 24.dp),
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrandMark(size = 32.dp, bg = c.accentSoft, fg = c.accent)
                Column {
                    Text("Nora", style = NoraTheme.typography.label, color = c.ink, fontWeight = FontWeight.SemiBold)
                    Text("Pre-triage co-pilot", style = NoraTheme.typography.caption, color = c.inkMuted)
                }
            }
            PrivacyBadge(compact = true)
        }

        Spacer(Modifier.height(22.dp))
        Text("What's going on today?", style = NoraTheme.typography.display, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap and tell me in your own words — when it started, where it hurts, anything else.",
            style = NoraTheme.typography.body,
            color = c.inkSoft,
        )

        Spacer(Modifier.height(24.dp))
        TranscriptCard(transcript = transcript, recording = recording)

        Spacer(Modifier.height(24.dp))
        Waveform(recording = recording, tick = tick)

        Spacer(Modifier.weight(1f))

        // Mic button cluster
        if (hasMic) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MicButton(
                    recording = recording,
                    enabled = micState !is MicState.Transcribing,
                    onClick = {
                        when (micState) {
                            is MicState.Idle, is MicState.Error -> {
                                tick = 0
                                micState = MicState.Recording
                                sampler.startRecording()
                            }
                            is MicState.Recording -> sampler.stopRecording()
                            is MicState.Transcribing -> {}
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when (val s = micState) {
                        is MicState.Idle -> if (transcript.isNotBlank()) "Tap to add more" else "Tap to speak"
                        is MicState.Recording -> "Tap to stop"
                        is MicState.Transcribing -> "Transcribing ${s.samples / 16000}s of audio…"
                        is MicState.Error -> "Mic error: ${s.message}"
                    },
                    color = if (micState is MicState.Error) c.statusRed else c.inkSoft,
                    style = NoraTheme.typography.label,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            NoraButton(
                onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                kind = NoraBtnKind.Secondary,
            ) { Text("Enable microphone") }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NoraButton(
                onClick = { typing = true },
                modifier = Modifier.weight(1f),
                kind = NoraBtnKind.Secondary,
                leadingIcon = {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
                },
            ) { Text("Type") }
            NoraButton(
                onClick = onCamera,
                modifier = Modifier.weight(1f),
                kind = NoraBtnKind.Secondary,
                leadingIcon = {
                    val icon = if (hasImage) Icons.Default.Check else Icons.Default.AddAPhoto
                    val tint = if (hasImage) c.statusGreen else c.ink
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                },
            ) { Text(if (hasImage) "Photo added" else "Add photo") }
            NoraButton(
                onClick = onContinue,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                },
            ) { Text("Go") }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TranscriptCard(transcript: String, recording: Boolean) {
    val c = NoraTheme.colors
    val empty = transcript.isBlank()
    val border = c.border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightAtLeast(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (empty) Color.Transparent else c.surface)
            .then(
                if (empty) Modifier.dashedBorder(1.dp, border, 16.dp)
                else Modifier.border(1.dp, border, RoundedCornerShape(16.dp))
            )
            .padding(16.dp),
    ) {
        val display = when {
            transcript.isNotBlank() -> transcript
            recording -> "Listening…"
            else -> "Your words will appear here."
        }
        Text(
            text = display,
            color = if (empty) c.inkMuted else c.ink,
            style = NoraTheme.typography.body.copy(
                fontStyle = if (empty) FontStyle.Italic else FontStyle.Normal,
            ),
        )
    }
}

@Composable
private fun Waveform(recording: Boolean, tick: Int) {
    val c = NoraTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // Surrounding spacers to center 28 bars within the row
        Spacer(Modifier.weight(1f))
        repeat(28) { i ->
            val amp = if (recording) {
                0.3f + 0.7f * abs(sin((tick + i * 3) * 0.4) * cos(i * 0.7)).toFloat()
            } else 0.18f
            val animated by animateFloatAsState(targetValue = amp, animationSpec = tween(100), label = "wf$i")
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((54.dp.value * animated).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (recording) c.accent else c.border),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun MicButton(recording: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = NoraTheme.colors
    val bg = if (recording) c.statusRed else c.accent
    Box(
        modifier = Modifier
            .size(116.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, onClickLabel = "Toggle recording") { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (recording) {
            // Stop glyph
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White),
            )
        } else {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Record",
                tint = Color.White,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

@Composable
private fun TypeOverlay(initial: String, onClose: () -> Unit, onSave: (String) -> Unit) {
    val c = NoraTheme.colors
    var text by remember { mutableStateOf(initial) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.size(28.dp).clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Close, contentDescription = "Close", tint = c.ink) }
            Text("Type your symptoms", style = NoraTheme.typography.title, color = c.ink, fontWeight = FontWeight.SemiBold)
            Box(modifier = Modifier.clickable { onSave(text) }) {
                Text("Done", color = c.accent, style = NoraTheme.typography.body, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxSize(),
                cursorBrush = SolidColor(c.accent),
                textStyle = TextStyle(color = c.ink, fontSize = 17.sp),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            "What are you experiencing?",
                            color = c.inkMuted,
                            style = NoraTheme.typography.body,
                        )
                    }
                    inner()
                },
            )
        }
    }
}

private fun Modifier.heightAtLeast(min: androidx.compose.ui.unit.Dp): Modifier =
    this.heightIn(min = min)

// Lightweight dashed border via Canvas; not exact but visually matches the design.
private fun Modifier.dashedBorder(
    stroke: androidx.compose.ui.unit.Dp,
    color: Color,
    radius: androidx.compose.ui.unit.Dp,
): Modifier = this.drawBehind {
    val r = radius.toPx()
    val sw = stroke.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(r, r),
        style = Stroke(
            width = sw,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
        ),
    )
}

