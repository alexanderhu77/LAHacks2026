package com.lahacks2026.pretriage.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lahacks2026.pretriage.data.ChatMessage
import com.lahacks2026.pretriage.ml.whisper.AudioSampler
import com.lahacks2026.pretriage.ml.whisper.WhisperFeature
import com.lahacks2026.pretriage.triage.EmergencyShortCircuit
import com.lahacks2026.pretriage.ui.ChatUiState
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.NoraBtnKind
import com.lahacks2026.pretriage.ui.components.NoraButton
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.theme.NoraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ChatScreen"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    image: Bitmap?,
    onComposerChange: (String) -> Unit,
    onChatRecordingChange: (Boolean) -> Unit,
    onSend: () -> String?,
    onAdvanceNoraTurn: () -> Unit,
    onAppendPhotoBubbleIfNeeded: () -> Unit,
    onAppendSkippedPhoto: () -> Unit,
    onStartChatIfNeeded: () -> Unit,
    onRequestPhoto: () -> Unit,
    onReadyToTriage: () -> Unit,
    onEmergencyShortCircuit: () -> Unit,
    onRestart: () -> Unit,
) {
    val c = NoraTheme.colors
    val listState = rememberLazyListState()

    // ── Effects ──

    LaunchedEffect(Unit) {
        onStartChatIfNeeded()
    }

    LaunchedEffect(image) {
        if (image != null) {
            onAppendPhotoBubbleIfNeeded()
        }
    }

    LaunchedEffect(state.pendingNoraTurn) {
        if (state.pendingNoraTurn) {
            delay(1100)
            onAdvanceNoraTurn()
        }
    }

    LaunchedEffect(state.readyToTriage) {
        if (state.readyToTriage) {
            delay(400)
            onReadyToTriage()
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(state.messages.size, state.pendingNoraTurn) {
        if (state.messages.isNotEmpty() || state.pendingNoraTurn) {
            val lastIdx = if (state.pendingNoraTurn) state.messages.size else state.messages.size - 1
            if (lastIdx >= 0) {
                listState.animateScrollToItem(lastIdx)
            }
        }
    }

    // When the IME opens, the LazyColumn viewport shrinks and the previously-anchored
    // bottom bubble can clip out of view. Re-pin to the latest item on IME show/hide.
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible && (state.messages.isNotEmpty() || state.pendingNoraTurn)) {
            val lastIdx = if (state.pendingNoraTurn) state.messages.size else state.messages.size - 1
            if (lastIdx >= 0) {
                listState.animateScrollToItem(lastIdx)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            // Consume the IME inset at the column's bottom: TopBar stays pinned to the top,
            // the weighted LazyColumn absorbs the height loss, and the composer rises with
            // the keyboard instead of the whole screen panning up.
            .imePadding()
    ) {
        // ── Top Bar ──
        TopBar(onRestart = onRestart)

        // ── Chat Transcript ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.messages) { msg ->
                ChatBubble(
                    msg = msg,
                    image = image,
                    onRequestPhoto = onRequestPhoto,
                    onSkipPhoto = onAppendSkippedPhoto
                )
            }

            if (state.pendingNoraTurn) {
                item {
                    NoraTypingBubble()
                }
            }
        }

        // ── Composer ──
        ChatComposer(
            composer = state.composer,
            recording = state.recording,
            onComposerChange = onComposerChange,
            onRecordingChange = onChatRecordingChange,
            onSend = {
                val sentText = onSend()
                if (sentText != null && EmergencyShortCircuit.match(sentText) != null) {
                    onEmergencyShortCircuit()
                }
            }
        )
    }
}

@Composable
private fun TopBar(onRestart: () -> Unit) {
    val c = NoraTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BrandMark(size = 32.dp, bg = c.accentSoft, fg = c.accent)
                Column {
                    Text(
                        "Nora",
                        style = NoraTheme.typography.label,
                        color = c.ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Pre-triage co-pilot",
                        style = NoraTheme.typography.caption,
                        color = c.inkMuted
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrivacyBadge(compact = true)
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Restart",
                    tint = c.inkSoft,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onRestart() }
                )
            }
        }
        Divider(thickness = 1.dp, color = c.border.copy(alpha = 0.5f))
    }
}

@Composable
private fun ChatBubble(
    msg: ChatMessage,
    image: Bitmap?,
    onRequestPhoto: () -> Unit,
    onSkipPhoto: () -> Unit
) {
    when (msg) {
        is ChatMessage.User -> UserBubble(text = msg.text)
        is ChatMessage.Nora -> NoraBubble(text = msg.text)
        is ChatMessage.Photo -> PhotoBubble(image = image)
        is ChatMessage.PhotoRequest -> PhotoRequestBubble(
            reason = msg.reason,
            onTakeClick = onRequestPhoto,
            onSkipClick = onSkipPhoto
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    val c = NoraTheme.colors
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp))
                .background(c.accent.copy(alpha = 0.14f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text, style = NoraTheme.typography.body, color = c.ink)
        }
    }
}

@Composable
private fun NoraBubble(text: String) {
    val c = NoraTheme.colors
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text, style = NoraTheme.typography.body, color = c.ink)
        }
    }
}

@Composable
private fun PhotoBubble(image: Bitmap?) {
    val c = NoraTheme.colors
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 180.dp)
                .clip(RoundedCornerShape(18.dp))
                .then(
                    if (image == null) Modifier.background(Brush.linearGradient(listOf(c.surfaceAlt, c.border)))
                    else Modifier
                )
        ) {
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Uploaded photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun PhotoRequestBubble(
    reason: String,
    onTakeClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val c = NoraTheme.colors
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(reason, style = NoraTheme.typography.body, color = c.ink)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoraButton(
                    onClick = onTakeClick,
                    modifier = Modifier.weight(1.3f),
                    big = false,
                    shape = CircleShape,
                    leadingIcon = {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                ) {
                    Text("Take a photo")
                }
                
                NoraButton(
                    onClick = onSkipClick,
                    modifier = Modifier.weight(0.7f),
                    kind = NoraBtnKind.Secondary,
                    big = false,
                    shape = CircleShape
                ) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun NoraTypingBubble() {
    val c = NoraTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    @Composable
    fun pulsingCircle(delayMillis: Int) {
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0.3f at delayMillis
                    1f at delayMillis + 300
                    0.3f at delayMillis + 600
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(c.inkMuted.copy(alpha = alpha))
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp))
                .background(c.surface)
                .border(1.dp, c.border, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            pulsingCircle(0)
            pulsingCircle(150)
            pulsingCircle(300)
        }
    }
}

@Composable
private fun ChatComposer(
    composer: String,
    recording: Boolean,
    onComposerChange: (String) -> Unit,
    onRecordingChange: (Boolean) -> Unit,
    onSend: () -> Unit,
) {
    val c = NoraTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Voice Implementation (copied/adapted from IntakeScreen) ──
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
    val whisperUsed = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    
    val sampler = remember {
        AudioSampler { pcm ->
            Log.i(TAG, "audio captured: ${pcm.size} samples")
            scope.launch {
                runCatching {
                    whisperUsed.set(true)
                    val text = withContext(Dispatchers.IO) { whisper.run(pcm) }
                    val merged = if (composer.isBlank()) text.trim()
                        else "${composer.trim()} ${text.trim()}".trim()
                    onComposerChange(merged)
                    onRecordingChange(false)
                }.onFailure {
                    Log.e(TAG, "transcription failed", it)
                    onRecordingChange(false)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { sampler.release() }
            if (whisperUsed.get()) {
                Thread { runCatching { whisper.close() } }.apply { isDaemon = true }.start()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
    ) {
        Divider(thickness = 1.dp, color = c.border)
        
        Row(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Text Input Pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = composer,
                    onValueChange = onComposerChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    textStyle = NoraTheme.typography.body.copy(color = c.ink),
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    maxLines = 4,
                    decorationBox = { inner ->
                        if (composer.isEmpty() && !recording) {
                            Text("Type a message...", color = c.inkMuted, style = NoraTheme.typography.body)
                        } else if (recording) {
                            Text("Listening...", color = c.accent, style = NoraTheme.typography.body, fontWeight = FontWeight.Medium)
                        }
                        inner()
                    }
                )

                Icon(
                    imageVector = if (recording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Voice",
                    tint = if (recording) c.statusRed else c.inkSoft,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            if (!hasMic) {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (recording) {
                                    sampler.stopRecording()
                                } else {
                                    onRecordingChange(true)
                                    sampler.startRecording()
                                }
                            }
                        }
                )
            }

            // Send Button
            val canSend = composer.isNotBlank() && !recording
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (canSend) c.accent else c.surfaceAlt)
                    .clickable(enabled = canSend) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Send",
                    tint = if (canSend) c.onAccent else c.inkMuted,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
