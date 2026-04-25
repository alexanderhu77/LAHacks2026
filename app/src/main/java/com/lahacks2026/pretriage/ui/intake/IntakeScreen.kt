package com.lahacks2026.pretriage.ui.intake

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import com.lahacks2026.pretriage.data.DemoScenario
import com.lahacks2026.pretriage.data.DemoScenarios
import com.lahacks2026.pretriage.ml.whisper.AudioSampler
import com.lahacks2026.pretriage.ml.whisper.WhisperFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "WhisperMic"

private sealed interface MicState {
    data object Idle : MicState
    data object Recording : MicState
    data class Transcribing(val samples: Int) : MicState
    data class Error(val message: String) : MicState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(
    onNavigateToCamera: (DemoScenario?) -> Unit,
    onNavigateToResult: (DemoScenario?) -> Unit,
    onNavigateToDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var symptomText by remember { mutableStateOf("") }
    var currentScenario by remember { mutableStateOf<DemoScenario?>(null) }
    var showDemoPicker by remember { mutableStateOf(false) }
    var micState by remember { mutableStateOf<MicState>(MicState.Idle) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (!granted) micState = MicState.Error("Mic permission denied")
    }

    val whisperFeature = remember { WhisperFeature(context.applicationContext) }
    val audioSampler = remember {
        AudioSampler { pcm ->
            val started = System.currentTimeMillis()
            Log.i(TAG, "→ audio captured: ${pcm.size} samples (~${pcm.size / 16000f}s)")
            micState = MicState.Transcribing(pcm.size)
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) { whisperFeature.run(pcm) }
                    val dur = System.currentTimeMillis() - started
                    Log.i(TAG, "✓ transcribed in ${dur}ms: $text")
                    symptomText = if (symptomText.isBlank()) text.trim()
                    else "${symptomText.trim()} ${text.trim()}".trim()
                    micState = MicState.Idle
                } catch (t: Throwable) {
                    Log.e(TAG, "✗ transcription failed", t)
                    micState = MicState.Error("${t.javaClass.simpleName}: ${t.message ?: "(no msg)"}")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { audioSampler.release() } catch (_: Throwable) {}
            try { whisperFeature.close() } catch (_: Throwable) {}
        }
    }

    if (showDemoPicker) {
        AlertDialog(
            onDismissRequest = { showDemoPicker = false },
            title = { Text("Select Scenario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DemoScenarios.All.forEach { scenario ->
                        OutlinedButton(
                            onClick = {
                                symptomText = scenario.initialSymptom
                                currentScenario = scenario
                                showDemoPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(scenario.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDemoPicker = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pre-Triage Co-Pilot", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showDemoPicker = true }) {
                        Icon(Icons.Default.Security, contentDescription = "Demo Mode", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Privacy Shield Badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        "On-device processing. Your data never leaves this phone.",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "How are you feeling?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                "Describe your symptoms in detail. You can use your voice or type below.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Voice Button Area
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val recording = micState is MicState.Recording
                val transcribing = micState is MicState.Transcribing
                FilledIconButton(
                    onClick = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@FilledIconButton
                        }
                        when (micState) {
                            MicState.Idle, is MicState.Error -> {
                                micState = MicState.Recording
                                audioSampler.startRecording()
                            }
                            MicState.Recording -> {
                                audioSampler.stopRecording()
                                // Transcribing state set in onAudioReady callback
                            }
                            is MicState.Transcribing -> { /* ignore taps while transcribing */ }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    enabled = !transcribing,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (recording) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (recording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                when (val s = micState) {
                    MicState.Idle -> "Tap to speak"
                    MicState.Recording -> "Listening… tap to stop"
                    is MicState.Transcribing -> "Transcribing ${s.samples / 16000}s of audio…"
                    is MicState.Error -> "Mic error: ${s.message}"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (micState is MicState.Error) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Text Input
            OutlinedTextField(
                value = symptomText,
                onValueChange = { symptomText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., I have a sharp pain in my chest...") },
                label = { Text("Type symptoms") },
                minLines = 3,
                trailingIcon = {
                    if (symptomText.isNotBlank()) {
                        IconButton(onClick = {
                            if (currentScenario?.hasVisual == true) {
                                onNavigateToCamera(currentScenario)
                            } else {
                                onNavigateToResult(currentScenario)
                            }
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Medical Disclaimer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "DISCLAIMER: This is a navigation tool, not a medical diagnosis. If you are experiencing a life-threatening emergency, call 911 immediately.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            TextButton(onClick = onNavigateToDiagnostics) {
                Text(
                    "Diagnostics: Run Melange Test",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
