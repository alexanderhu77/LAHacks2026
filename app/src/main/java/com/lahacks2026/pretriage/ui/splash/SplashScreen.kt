package com.lahacks2026.pretriage.ui.splash

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.WarmupState
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.DisplayText
import com.lahacks2026.pretriage.ui.components.PrivacyBadge
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette

private data class WarmupRow(val name: String, val detail: String)

private val rows = listOf(
    WarmupRow("Loading nurse model", "MedGemma 1.5 · 4B"),
    WarmupRow("Loading voice model", "Whisper · tiny"),
    WarmupRow("Loading privacy filter", "tanaos anonymizer"),
)

@Composable
fun SplashScreen(
    warmup: WarmupState,
    onWarmupComplete: () -> Unit,
) {
    val palette = LocalAppPalette.current

    LaunchedEffect(warmup.complete) {
        if (warmup.complete) {
            kotlinx.coroutines.delay(550)
            onWarmupComplete()
        }
    }

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(animation = tween(1200)),
        label = "pulseScale",
    )

    val progress = (warmup.step.toFloat() / warmup.total.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(96.dp).scale(pulse),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.accentSoft, RoundedCornerShape(28.dp)),
                )
                BrandMark(size = 72)
            }
            Spacer(Modifier.height(28.dp))
            DisplayText("Nora", size = 30.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your pre-triage co-pilot",
                color = palette.inkSoft,
                fontSize = 15.sp,
                fontFamily = palette.fontBody,
            )
            Spacer(Modifier.height(28.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(4.dp)
                    .background(palette.surfaceAlt, RoundedCornerShape(99.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(palette.accent, RoundedCornerShape(99.dp)),
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(0.7f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rows.forEachIndexed { idx, row ->
                    val done = idx < warmup.step
                    val active = idx == warmup.step && !warmup.complete
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (done) palette.accent else palette.surfaceAlt,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = if (palette.chrome == com.lahacks2026.pretriage.ui.theme.Chrome.Dark) palette.accentInk else androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                        Text(
                            row.name,
                            modifier = Modifier.weight(1f),
                            color = if (done || active) palette.ink else palette.inkMuted,
                            fontSize = 14.sp,
                            fontFamily = palette.fontBody,
                            fontWeight = if (active) FontWeight.W500 else FontWeight.Normal,
                        )
                        Text(
                            row.detail,
                            color = palette.inkMuted,
                            fontSize = 11.sp,
                            fontFamily = palette.fontMono,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            PrivacyBadge(compact = true)
        }
    }
}
