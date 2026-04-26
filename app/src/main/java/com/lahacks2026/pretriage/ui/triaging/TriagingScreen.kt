package com.lahacks2026.pretriage.ui.triaging

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.theme.NoraTheme
import kotlinx.coroutines.delay

@Composable
fun TriagingScreen(
    withImage: Boolean,
    decisionAvailable: Boolean,
    onDone: () -> Unit,
) {
    val c = NoraTheme.colors
    val steps = remember(withImage) {
        listOfNotNull(
            "Reading what you said",
            if (withImage) "Looking at the photo" else null,
            "Checking insurance options",
            "Drafting recommendation",
        )
    }
    var i by remember { mutableIntStateOf(0) }

    // Advance the checklist one tick at a time. If the orchestrator hasn't
    // returned by the second-to-last step, we hold there until it does — caller
    // enforces the 8 s hard cap and falls back to RuleBased on timeout.
    LaunchedEffect(steps) {
        for (idx in steps.indices) {
            i = idx
            delay(620)
        }
        i = steps.size
        delay(280)
    }
    LaunchedEffect(decisionAvailable, i) {
        if (decisionAvailable && i >= steps.size) {
            onDone()
        }
    }

    val infinite = rememberInfiniteTransition(label = "ring")
    val rot by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "ring-rot",
    )

    Box(
        modifier = Modifier.fillMaxSize().background(c.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier.size(92.dp).rotate(rot),
                ) {
                    val sw = 2.dp.toPx()
                    drawArc(
                        color = c.accent,
                        startAngle = 0f, sweepAngle = 270f, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(sw, sw),
                        size = Size(size.width - 2 * sw, size.height - 2 * sw),
                        style = Stroke(width = sw),
                    )
                }
                BrandMark(size = 72.dp, bg = c.accentSoft, fg = c.accent)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("One moment…", style = NoraTheme.typography.displaySmall, color = c.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Running on your phone, not the cloud.",
                    style = NoraTheme.typography.label,
                    color = c.inkSoft,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(0.78f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                steps.forEachIndexed { idx, label ->
                    StepRow(label, done = idx < i)
                }
            }
        }
    }
}

@Composable
private fun StepRow(label: String, done: Boolean) {
    val c = NoraTheme.colors
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
            label,
            color = if (done) c.ink else c.inkMuted,
            style = NoraTheme.typography.label,
        )
    }
}
