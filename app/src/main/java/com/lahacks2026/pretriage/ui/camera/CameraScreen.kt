package com.lahacks2026.pretriage.ui.camera

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lahacks2026.pretriage.ui.components.AppButton
import com.lahacks2026.pretriage.ui.components.BtnKind
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private enum class CapturePhase { Framing, Precheck, Warned }

internal data class QualityReport(val ok: Boolean, val reason: String?)

@Composable
private fun Spinner(color: Color, size: androidx.compose.ui.unit.Dp, strokeWidth: androidx.compose.ui.unit.Dp = 3.dp) {
    val infinite = rememberInfiniteTransition(label = "spinner")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing)),
        label = "spin",
    )
    Canvas(
        modifier = Modifier.size(size).rotate(rotation),
    ) {
        val sw = strokeWidth.toPx()
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(sw / 2f, sw / 2f),
            size = Size(this.size.width - sw, this.size.height - sw),
            style = Stroke(width = sw),
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onPhotoCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    if (cameraPermissionState.status.isGranted) {
        CameraContent(onPhotoCaptured = onPhotoCaptured, onCancel = onCancel)
    } else {
        PermissionRequestScreen(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                "Camera access is required to capture photos of symptoms.",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = palette.fontBody,
            )
            Spacer(Modifier.height(24.dp))
            AppButton(text = "Grant permission", onClick = onRequestPermission)
            Spacer(Modifier.height(8.dp))
            AppButton(text = "Cancel", kind = BtnKind.Ghost, onClick = onCancel)
        }
    }
}

@Composable
private fun CameraContent(
    onPhotoCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    var phase by remember { mutableStateOf(CapturePhase.Framing) }
    var captured by remember { mutableStateOf<Bitmap?>(null) }
    var quality by remember { mutableStateOf<QualityReport?>(null) }
    var showFlash by remember { mutableStateOf(false) }

    LaunchedEffect(captured) {
        val bmp = captured ?: return@LaunchedEffect
        phase = CapturePhase.Precheck
        // Precheck on a background thread.
        val report = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            assessQuality(bmp)
        }
        quality = report
        if (report.ok) {
            onPhotoCaptured(bmp)
        } else {
            phase = CapturePhase.Warned
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture,
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Text("On-device only", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.W500, fontFamily = palette.fontBody)
            }
        }

        // Reticle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 50.dp, end = 50.dp, top = 200.dp, bottom = 200.dp)
                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        )

        if (showFlash) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))
        }

        if (phase == CapturePhase.Precheck) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spinner(color = palette.accent, size = 44.dp)
                    Spacer(Modifier.height(14.dp))
                    Text("Checking quality…", color = Color.White, fontSize = 14.sp, fontFamily = palette.fontBody)
                }
            }
        }

        if (phase == CapturePhase.Warned) {
            QualitySheet(
                reason = quality?.reason ?: "Try again with steadier hands",
                onRetake = { phase = CapturePhase.Framing; captured = null; quality = null },
                onUseAnyway = { captured?.let(onPhotoCaptured) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 110.dp),
            )
        }

        // Shutter row
        if (phase == CapturePhase.Framing) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                ) {
                    IconButton(
                        onClick = {
                            showFlash = true
                            takePicture(imageCapture, cameraExecutor) { bmp ->
                                showFlash = false
                                captured = bmp
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {}
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }
}

@Composable
private fun QualitySheet(
    reason: String,
    onRetake: () -> Unit,
    onUseAnyway: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .background(palette.surface, shape)
            .border(1.dp, palette.border, shape)
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(palette.statusAmber.copy(alpha = 0.13f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = palette.statusAmber, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("A bit blurry — retake?", fontWeight = FontWeight.W600, fontSize = 15.sp, color = palette.ink, fontFamily = palette.fontBody)
                Spacer(Modifier.height(2.dp))
                Text(reason, color = palette.inkSoft, fontSize = 13.sp, fontFamily = palette.fontBody)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(text = "Retake", kind = BtnKind.Secondary, onClick = onRetake, modifier = Modifier.weight(1f))
            AppButton(text = "Use anyway", onClick = onUseAnyway, modifier = Modifier.weight(1f))
        }
    }
}

private fun takePicture(
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onResult: (Bitmap?) -> Unit,
) {
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            try {
                val bmp = imageProxyToBitmap(image)
                onResult(bmp?.let { resizeMaxEdge(it, 768) })
            } finally {
                image.close()
            }
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("CameraScreen", "capture failed", exception)
            onResult(null)
        }
    })
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val rot = image.imageInfo.rotationDegrees
    if (rot == 0) return bmp
    val m = Matrix().apply { postRotate(rot.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}

internal fun resizeMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val longest = maxOf(w, h)
    if (longest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longest.toFloat()
    return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
}

internal fun assessQuality(bitmap: Bitmap): QualityReport {
    // Lightweight blur + luminance proxy: sample a center crop, compute luminance mean
    // and a Sobel-like gradient mean. Not pixel-perfect but fast and demo-safe.
    val side = minOf(bitmap.width, bitmap.height)
    val sample = 96
    val srcX = (bitmap.width - side) / 2
    val srcY = (bitmap.height - side) / 2
    val crop = Bitmap.createBitmap(bitmap, srcX, srcY, side, side)
    val small = Bitmap.createScaledBitmap(crop, sample, sample, true)
    val pixels = IntArray(sample * sample)
    small.getPixels(pixels, 0, sample, 0, 0, sample, sample)

    var sumLum = 0.0
    var gradSum = 0.0
    for (y in 1 until sample - 1) {
        for (x in 1 until sample - 1) {
            val p = pixels[y * sample + x]
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            val lum = 0.299 * r + 0.587 * g + 0.114 * b
            sumLum += lum
            val pr = pixels[y * sample + (x + 1)]
            val rRight = 0.299 * ((pr shr 16) and 0xff) + 0.587 * ((pr shr 8) and 0xff) + 0.114 * (pr and 0xff)
            val pd = pixels[(y + 1) * sample + x]
            val rDown = 0.299 * ((pd shr 16) and 0xff) + 0.587 * ((pd shr 8) and 0xff) + 0.114 * (pd and 0xff)
            gradSum += kotlin.math.abs(lum - rRight) + kotlin.math.abs(lum - rDown)
        }
    }
    val totalPx = (sample - 2) * (sample - 2)
    val meanLum = sumLum / (sample * sample)
    val meanGrad = gradSum / totalPx

    val reason = when {
        meanLum < 35.0 -> "Too dark — better light helps."
        meanLum > 230.0 -> "Too bright — try a softer light."
        meanGrad < 4.0 -> "Hold steady about 6 inches away. Better light helps."
        else -> null
    }
    return QualityReport(ok = reason == null, reason = reason)
}
