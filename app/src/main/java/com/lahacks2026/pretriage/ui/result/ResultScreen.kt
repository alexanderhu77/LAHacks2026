package com.lahacks2026.pretriage.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onNavigateBack: () -> Unit
) {
    // Mocking a result for UI design
    val severity = "Urgent Care Today"
    val severityColor = Color(0xFFE67E22) // Orange-ish
    val reasoning = "Based on your description of a sharp pain in your chest that radiates to your arm, we recommend visiting Urgent Care for an immediate evaluation. This ensures your symptoms are monitored by a professional."

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Triage Result") },
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
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Severity Badge
            Surface(
                color = severityColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.large,
                border = BoxShadow(severityColor) // Not a real thing, using border
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        severity,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
            }

            Text(
                "Reasoning",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                reasoning,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Action Card (Insurance Routed)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Recommended Action",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Visit 'City Health Urgent Care' (In-network). Your copay is $20.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* TODO: Open Maps */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Directions")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Privacy Footnote
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF27AE60),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Result generated locally & privately.",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// Helper to make it look nicer since BoxShadow isn't standard in M3 easily
@Composable
private fun BoxShadow(color: Color) = androidx.compose.foundation.BorderStroke(2.dp, color)
