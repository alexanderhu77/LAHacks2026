package com.lahacks2026.pretriage.ui.intake

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.lahacks2026.pretriage.data.DemoScenario
import com.lahacks2026.pretriage.data.DemoScenarios

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(
    onNavigateToCamera: (DemoScenario?) -> Unit,
    onNavigateToResult: (DemoScenario?) -> Unit
) {
    var symptomText by remember { mutableStateOf("") }
    var currentScenario by remember { mutableStateOf<DemoScenario?>(null) }
    var showDemoPicker by remember { mutableStateOf(false) }

    if (showDemoPicker) {
        AlertDialog(
            onDismissRequest = { showDemoPicker = false },
            title = { Text("Select Demo Scenario") },
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
                FilledIconButton(
                    onClick = { /* TODO: Start Whisper Voice Recording */ },
                    modifier = Modifier.fillMaxSize(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                "Tap to speak",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            // HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
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
        }
    }
}
