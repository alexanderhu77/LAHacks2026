package com.lahacks2026.pretriage.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onPhotoCaptured: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Mock Camera Preview Area
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Align symptom in the frame",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Top Controls
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
        }

        // Bottom Capture Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(80.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Color.White,
            onClick = onPhotoCaptured
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    .padding(2.dp)
                    .background(Color.Black.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
