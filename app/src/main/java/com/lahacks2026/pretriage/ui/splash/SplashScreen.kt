package com.lahacks2026.pretriage.ui.splash

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.ui.WarmupState
import com.lahacks2026.pretriage.ui.WarmupStep
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.theme.NoraTheme

private data class StepRow(val step: WarmupStep, val name: String, val detail: String)

private val SPLASH_STEPS = listOf(
    StepRow(WarmupStep.Triage, "Loading nurse model", "MedGemma 1.5 · 4B"),
    StepRow(WarmupStep.Voice, "Loading voice model", "Whisper · tiny"),
    StepRow(WarmupStep.Privacy, "Loading privacy filter", "tanaos anonymizer"),
)

@Composable
fun SplashScreen(
    warmup: WarmupState,
    onStart: () -> Unit,
    onDone: () -> Unit,
) {
    val c = NoraTheme.colors
    LaunchedEffect(Unit) { onStart() }
    LaunchedEffect(warmup.allDone) {
        if (warmup.allDone) {
            kotlinx.coroutines.delay(450)
            onDone()
        }
    }

    val progress = (warmup.completed.size + warmup.failed.size).toFloat() / SPLASH_STEPS.size
    val pct by animateFloatAsState(targetValue = progress, animationSpec = tween(500), label = "splash-progress")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PulsingMark()
            Spacer(Modifier.height(28.dp))
            Text("Nora", style = NoraTheme.typography.display, color = c.ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your pre-triage co-pilot",
                style = NoraTheme.typography.body,
                color = c.inkSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(c.surfaceAlt),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct)
                        .height(4.dp)
                        .background(c.accent),
                )
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(0.78f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SPLASH_STEPS.forEach { row ->
                    SplashRow(row, warmup)
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
        ) {
            PrivacyBadge(compact = true)
        }
    }
}

@Composable
private fun PulsingMark() {
    val c = NoraTheme.colors
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2400)),
        label = "pulse-scale",
    )
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .clip(RoundedCornerShape(28.dp))
                .background(c.accentSoft),
        )
        BrandMark(size = 72.dp, bg = c.surface, fg = c.accent)
    }
}

@Composable
private fun SplashRow(row: StepRow, warmup: WarmupState) {
    val c = NoraTheme.colors
    val done = row.step in warmup.completed
    val active = warmup.active == row.step && !done
    val pending = !done && !active

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (done) c.accent else c.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
        Text(
            text = row.name,
            modifier = Modifier.weight(1f),
            color = if (pending) c.inkMuted else c.ink,
            style = NoraTheme.typography.label,
        )
        Text(
            text = row.detail,
            color = c.inkMuted,
            style = NoraTheme.typography.mono,
        )
    }
}

