package com.lahacks2026.pretriage.ui.triaging

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lahacks2026.pretriage.ui.components.BrandMark
import com.lahacks2026.pretriage.ui.components.DisplayText
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette

@Composable
fun TriagingScreen(
    withImage: Boolean,
    inFlight: Boolean,
    onTriageRequested: () -> Unit,
) {
    val palette = LocalAppPalette.current

    val steps = listOfNotNull(
        "Reading what you said",
        if (withImage) "Looking at the photo" else null,
        "Checking insurance options",
        "Drafting recommendation",
    )

    var i by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        // Kick off the actual triage call once.
        onTriageRequested()
    }

    LaunchedEffect(inFlight) {
        // Animate the checklist forward while in-flight, then hold at last step.
        i = 0
        val total = steps.size
        while (i < total) {
            kotlinx.coroutines.delay(700)
            if (i < total - 1) i += 1 else break
            // If the call already finished, pop forward fast.
            if (!inFlight) i = total - 1
        }
    }

    val infinite = rememberInfiniteTransition(label = "triage-spin")
    val rot by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1200)),
        label = "rot",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .rotate(rot)
                    .border(2.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
            )
            BrandMark(size = 72)
        }
        Spacer(Modifier.height(28.dp))
        DisplayText("One moment…", size = 24.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Running on your phone, not the cloud.",
            color = palette.inkSoft,
            fontSize = 14.sp,
            fontFamily = palette.fontBody,
        )
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(0.75f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            steps.forEachIndexed { idx, label ->
                val done = idx < i
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(if (done) palette.accent else palette.surfaceAlt, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                    Text(
                        label,
                        color = if (done) palette.ink else palette.inkMuted,
                        fontSize = 14.sp,
                        fontFamily = palette.fontBody,
                        fontWeight = if (idx == i) FontWeight.W500 else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
