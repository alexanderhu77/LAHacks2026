package com.lahacks2026.pretriage.ui.diagnostics

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lahacks2026.pretriage.BuildConfig
import com.zeticai.mlange.core.model.llm.LLMQuantType
import com.zeticai.mlange.core.model.llm.LLMTarget
import com.zeticai.mlange.core.model.llm.ZeticMLangeLLMModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MelangeSmoke"

private const val SMOKE_MODEL_ID = "google/medgemma-1.5-4b-it" // Updated to MedGemma
private const val SMOKE_PROMPT = "What is the capital of France? Answer in one sentence."

private sealed interface SmokeState {
    data object Idle : SmokeState
    data class Downloading(val progress: Float, val status: String) : SmokeState
    data object Generating : SmokeState
    data class Done(val text: String, val tokens: Int, val durationMs: Long) : SmokeState
    data class Error(val message: String) : SmokeState
}

private fun maskToken(t: String): String =
    if (t.length <= 8) "***" else t.take(4) + "…" + t.takeLast(4) + " (len=${t.length})"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmokeTestScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<SmokeState>(SmokeState.Idle) }
    val token = BuildConfig.MELANGE_TOKEN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics — Melange Smoke Test") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Model: $SMOKE_MODEL_ID", style = MaterialTheme.typography.bodyMedium)
            Text("Prompt: \"$SMOKE_PROMPT\"", style = MaterialTheme.typography.bodyMedium)

            if (token.isBlank()) {
                Text(
                    "MELANGE_TOKEN is not set. Add MELANGE_TOKEN=<your_token> to local.properties " +
                        "(see local.properties.example), then rebuild.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (token.isBlank()) {
                        state = SmokeState.Error("MELANGE_TOKEN missing in local.properties")
                        return@Button
                    }
                    Log.i(TAG, "▶ Run tapped. modelId=$SMOKE_MODEL_ID token=${maskToken(token)}")
                    state = SmokeState.Downloading(0f, "starting")
                    scope.launch {
                        var lastProgress = 0f
                        var lastStatus = "starting"
                        var lastProgressAt = System.currentTimeMillis()
                        val started = System.currentTimeMillis()
                        val heartbeat: Job = launch(Dispatchers.IO) {
                            while (isActive) {
                                delay(2000)
                                val elapsed = (System.currentTimeMillis() - started) / 1000
                                val sinceProg = (System.currentTimeMillis() - lastProgressAt) / 1000
                                Log.i(
                                    TAG,
                                    "♥ heartbeat: elapsed=${elapsed}s lastProgress=${(lastProgress * 100).toInt()}% " +
                                        "status=$lastStatus sinceLastProgress=${sinceProg}s"
                                )
                            }
                        }
                        try {
                            val result = withContext(Dispatchers.IO) {
                                Log.i(TAG, "→ constructing ZeticMLangeLLMModel… (this triggers download/auth)")
                                val ctorStart = System.currentTimeMillis()
                                val model = ZeticMLangeLLMModel(
                                    context.applicationContext,
                                    token,
                                    SMOKE_MODEL_ID,
                                    version = null,
                                    target = LLMTarget.LLAMA_CPP,
                                    quantType = LLMQuantType.GGUF_QUANT_Q4_K_M,
                                    onProgress = { progress ->
                                        lastProgress = progress
                                        lastProgressAt = System.currentTimeMillis()
                                        Log.d(TAG, "onProgress: ${(progress * 100).toInt()}%")
                                        state = SmokeState.Downloading(progress, lastStatus)
                                    },
                                    onStatusChanged = { status ->
                                        lastStatus = status.toString()
                                        Log.i(TAG, "onStatusChanged: $lastStatus")
                                        state = SmokeState.Downloading(lastProgress, lastStatus)
                                    }
                                )
                                Log.i(TAG, "✓ constructor returned in ${System.currentTimeMillis() - ctorStart}ms")
                                state = SmokeState.Generating
                                val start = System.currentTimeMillis()
                                Log.i(TAG, "→ model.cleanUp()")
                                model.cleanUp()
                                Log.i(TAG, "→ model.run(prompt)")
                                model.run(SMOKE_PROMPT)
                                val sb = StringBuilder()
                                var count = 0
                                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                                    val r = model.waitForNextToken()
                                    if (r.generatedTokens == 0) break
                                    sb.append(r.token)
                                    count++
                                    if (count > 256) break
                                }
                                val duration = System.currentTimeMillis() - start
                                Log.i(TAG, "✓ generation done: tokens=$count durMs=$duration")
                                model.cleanUp()
                                Triple(sb.toString(), count, duration)
                            }
                            state = SmokeState.Done(result.first, result.second, result.third)
                        } catch (t: Throwable) {
                            val chain = StringBuilder()
                            var cur: Throwable? = t
                            var depth = 0
                            while (cur != null && depth < 8) {
                                Log.e(TAG, "✗ [$depth] ${cur.javaClass.name}: ${cur.message}", cur)
                                if (chain.isNotEmpty()) chain.append("\n  caused by: ")
                                chain.append(cur.javaClass.simpleName).append(": ").append(cur.message ?: "(no msg)")
                                cur = cur.cause
                                depth++
                            }
                            state = SmokeState.Error(chain.toString())
                        } finally {
                            heartbeat.cancel()
                        }
                    }
                },
                enabled = state !is SmokeState.Downloading && state !is SmokeState.Generating
            ) {
                Text("Run Melange Test")
            }

            if (state is SmokeState.Error || state is SmokeState.Done) {
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                context.filesDir.resolve("mlange_cache").deleteRecursively()
                                withContext(Dispatchers.Main) {
                                    state = SmokeState.Idle
                                }
                                Log.i(TAG, "✓ Cache cleared manually.")
                            } catch (e: Exception) {
                                Log.e(TAG, "✗ Failed to clear cache", e)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Local Cache & Reset")
                }
            }

            when (val s = state) {
                SmokeState.Idle -> Text("Ready. Tap the button.")
                is SmokeState.Downloading -> Text("Downloading model… ${(s.progress * 100).toInt()}% — ${s.status}")
                SmokeState.Generating -> Text("Generating…")
                is SmokeState.Done -> {
                    Text("Output (${s.tokens} tokens in ${s.durationMs} ms):")
                    Text(s.text.ifBlank { "(empty response)" })
                }
                is SmokeState.Error -> Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
            }

            Text(
                "Watch full log: adb logcat MelangeSmoke:V '*:S'",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
