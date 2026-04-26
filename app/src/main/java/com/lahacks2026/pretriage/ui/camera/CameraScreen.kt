package com.lahacks2026.pretriage.ui.camera

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lahacks2026.pretriage.ui.components.NoraBtnKind
import com.lahacks2026.pretriage.ui.components.NoraButton
import com.lahacks2026.pretriage.ui.theme.NoraTheme
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.Executors

private const val TAG = "CameraScreen"
private const val MAX_EDGE_PX = 768

private sealed interface CapturePhase {
    data object Framing : CapturePhase
    data object Prechecking : CapturePhase
    data class Warned(val bitmap: Bitmap) : CapturePhase
    data class Ready(val bitmap: Bitmap) : CapturePhase
}

private data class QualityCheck(val ok: Boolean, val luminance: Float, val variance: Float)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onPhotoCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val permission = rememberPermissionState(Manifest.permission.CAMERA)
    if (!permission.status.isGranted) {
        PermissionRequest(
            onRequest = { permission.launchPermissionRequest() },
            onCancel = onCancel,
        )
        return
    }
    CameraContent(onPhotoCaptured = onPhotoCaptured, onCancel = onCancel)
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit, onCancel: () -> Unit) {
    val c = NoraTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Camera access lets you photograph a rash, wound, or document.",
            color = c.ink, style = NoraTheme.typography.body,
        )
        Spacer(Modifier.height(20.dp))
        NoraButton(onClick = onRequest, big = true) { Text("Enable camera") }
        Spacer(Modifier.height(10.dp))
        NoraButton(onClick = onCancel, kind = NoraBtnKind.Ghost) { Text("Skip") }
    }
}

@Composable
private fun CameraContent(
    onPhotoCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val c = NoraTheme.colors
    val lifecycle = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    // rememberCoroutineScope is bound to Dispatchers.Main.immediate; we use it
    // to bounce off the camera executor before touching Compose state or nav.
    val mainScope = rememberCoroutineScope()

    var phase by remember { mutableStateOf<CapturePhase>(CapturePhase.Framing) }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    runCatching {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    }.onFailure { Log.e(TAG, "bind failed", it) }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        // Top bar
        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 18.dp, end = 18.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Text("On-device only", color = Color.White, style = NoraTheme.typography.caption)
            }
        }

        // Reticle
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 120.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            )
        }

        // Precheck overlay
        if (phase is CapturePhase.Prechecking) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpinnerArc(color = c.accent, size = 44.dp)
                    Spacer(Modifier.height(14.dp))
                    Text("Checking quality…", color = Color.White, style = NoraTheme.typography.label)
                }
            }
        }

        // Warning sheet (blurry)
        (phase as? CapturePhase.Warned)?.let { warned ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surface)
                    .border(1.dp, c.border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.statusAmber.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = c.statusAmber, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("A bit blurry — retake?", style = NoraTheme.typography.title, color = c.ink, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Hold steady about 6 inches away. Better light helps.",
                            color = c.inkSoft, style = NoraTheme.typography.label,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoraButton(
                        onClick = { phase = CapturePhase.Framing },
                        modifier = Modifier.weight(1f),
                        kind = NoraBtnKind.Secondary,
                    ) { Text("Retake") }
                    NoraButton(
                        onClick = { onPhotoCaptured(warned.bitmap) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Use anyway") }
                }
            }
        }

        // Shutter row
        if (phase is CapturePhase.Framing) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            phase = CapturePhase.Prechecking
                            imageCapture.takePicture(
                                executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        // Decode + scale + quality check on the
                                        // camera executor (background). Hand the
                                        // result back to the main scope so state
                                        // changes and navigation never run from
                                        // a non-main thread.
                                        val bm = image.captureToBitmap().scaleToMaxEdge(MAX_EDGE_PX)
                                        image.close()
                                        val q = bm.qualityCheck()
                                        Log.i(TAG, "quality lum=${q.luminance} var=${q.variance} ok=${q.ok}")
                                        mainScope.launch {
                                            phase = if (q.ok) CapturePhase.Ready(bm)
                                                    else CapturePhase.Warned(bm)
                                            if (q.ok) onPhotoCaptured(bm)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e(TAG, "capture failed", exception)
                                        mainScope.launch { phase = CapturePhase.Framing }
                                    }
                                },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun SpinnerArc(color: Color, size: androidx.compose.ui.unit.Dp) {
    val infinite = rememberInfiniteTransition(label = "spinner")
    val rot by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spinner-rot",
    )
    Canvas(modifier = Modifier.size(size).rotate(rot)) {
        val sw = 3.dp.toPx()
        drawArc(
            color = color,
            startAngle = 0f, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(sw, sw),
            size = Size(this.size.width - 2 * sw, this.size.height - 2 * sw),
            style = Stroke(width = sw),
        )
    }
}

private fun ImageProxy.captureToBitmap(): Bitmap {
    val buffer: ByteBuffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun Bitmap.scaleToMaxEdge(maxEdge: Int): Bitmap {
    val w = width
    val h = height
    val maxSide = maxOf(w, h)
    if (maxSide <= maxEdge) return this
    val scale = maxEdge.toFloat() / maxSide
    return Bitmap.createScaledBitmap(this, (w * scale).toInt(), (h * scale).toInt(), true)
}

/**
 * Cheap precheck: average luminance + a pseudo-Laplacian variance estimate.
 * Failing thresholds match the design spec for the "blurry retake?" sheet.
 */
private fun Bitmap.qualityCheck(): QualityCheck {
    val w = width.coerceAtMost(64)
    val h = height.coerceAtMost(64)
    val px = IntArray(w * h)
    val scaled = Bitmap.createScaledBitmap(this, w, h, true)
    scaled.getPixels(px, 0, w, 0, 0, w, h)

    var lumSum = 0.0
    val gray = FloatArray(px.size)
    for (i in px.indices) {
        val p = px[i]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        val y = 0.299f * r + 0.587f * g + 0.114f * b
        gray[i] = y
        lumSum += y
    }
    val mean = lumSum / px.size

    // Pseudo-Laplacian: |center - avg(neighbors)|.
    var lapSum = 0.0
    var lapSqSum = 0.0
    var n = 0
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            val idx = y * w + x
            val avg = (gray[idx - 1] + gray[idx + 1] + gray[idx - w] + gray[idx + w]) / 4f
            val v = (gray[idx] - avg).toDouble()
            lapSum += v
            lapSqSum += v * v
            n++
        }
    }
    val variance = if (n > 0) (lapSqSum / n - (lapSum / n) * (lapSum / n)).toFloat() else 0f
    val ok = mean > 28 && variance > 8f
    return QualityCheck(ok = ok, luminance = mean.toFloat(), variance = variance)
}
