package com.lahacks2026.pretriage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zeticai.mlange.core.model.llm.ZeticMLangeLLMModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SMOKE_MODEL_ID = "Qwen/Qwen3-4B"
private const val SMOKE_PROMPT = "What is the capital of France? Answer in one sentence."

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmokeTestScreen()
                }
            }
        }
    }
}

private sealed interface SmokeState {
    data object Idle : SmokeState
    data class Downloading(val progress: Float) : SmokeState
    data object Generating : SmokeState
    data class Done(val text: String, val tokens: Int, val durationMs: Long) : SmokeState
    data class Error(val message: String) : SmokeState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmokeTestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<SmokeState>(SmokeState.Idle) }
    val token = BuildConfig.MELANGE_TOKEN

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pre-Triage Co-Pilot — Melange Smoke Test") }) }
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
                    state = SmokeState.Downloading(0f)
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                val model = ZeticMLangeLLMModel(
                                    context.applicationContext,
                                    token,
                                    SMOKE_MODEL_ID,
                                    onProgress = { progress ->
                                        state = SmokeState.Downloading(progress)
                                    }
                                )
                                state = SmokeState.Generating
                                val start = System.currentTimeMillis()
                                model.cleanUp()
                                model.run(SMOKE_PROMPT)
                                val sb = StringBuilder()
                                var count = 0
                                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                                    val r = model.waitForNextToken()
                                    if (r.generatedTokens == 0) break
                                    sb.append(r.token)
                                    count++
                                    if (count > 256) break // hard cap for smoke test
                                }
                                val duration = System.currentTimeMillis() - start
                                model.cleanUp()
                                Triple(sb.toString(), count, duration)
                            }
                            state = SmokeState.Done(result.first, result.second, result.third)
                        } catch (t: Throwable) {
                            state = SmokeState.Error(t.javaClass.simpleName + ": " + (t.message ?: "(no message)"))
                        }
                    }
                },
                enabled = state !is SmokeState.Downloading && state !is SmokeState.Generating
            ) {
                Text("Run Melange Test")
            }

            when (val s = state) {
                SmokeState.Idle -> Text("Ready. Tap the button.")
                is SmokeState.Downloading -> Text("Downloading model… ${(s.progress * 100).toInt()}%")
                SmokeState.Generating -> Text("Generating…")
                is SmokeState.Done -> {
                    Text("Output (${s.tokens} tokens in ${s.durationMs} ms):")
                    Text(s.text.ifBlank { "(empty response)" })
                }
                is SmokeState.Error -> Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
