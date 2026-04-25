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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.lahacks2026.pretriage.data.InsurancePlan
import com.lahacks2026.pretriage.data.DemoScenario
import androidx.compose.material.icons.filled.Phone

import androidx.compose.ui.graphics.vector.ImageVector
import com.lahacks2026.pretriage.data.*
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    plan: InsurancePlan,
    scenario: DemoScenario?,
    onNavigateBack: () -> Unit
) {
    // Determine content from scenario or use defaults
    val severityLevel = scenario?.severity ?: SeverityLevel.URGENT_CARE
    val severity = when (severityLevel) {
        SeverityLevel.SELF_CARE -> "Self Care"
        SeverityLevel.TELEHEALTH -> "Telehealth"
        SeverityLevel.URGENT_CARE -> "Urgent Care Today"
        SeverityLevel.EMERGENCY -> "EMERGENCY IMMEDIATELY"
    }
    val reasoning = scenario?.reasoning ?: "Based on your description, we recommend visiting Urgent Care for an immediate evaluation."
    
    val isEmergency = severityLevel == SeverityLevel.EMERGENCY
    val severityColor = if (isEmergency) MaterialTheme.colorScheme.error else Color(0xFFE67E22)
    val severityIcon = if (isEmergency) Icons.Default.Warning else Icons.Default.Warning

    val copay = plan.copayFor(severityLevel) ?: 0
    val redFlags = scenario?.redFlags ?: emptyList()
    val confidence = scenario?.confidence ?: 0.0

    var showDeidFlow by remember { mutableStateOf(false) }

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
                        severityIcon,
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

            if (redFlags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Detected Red Flags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
                redFlags.forEach { flag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            flag,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Action Card (Insurance Routed or Emergency)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEmergency) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (isEmergency) "IMMEDIATE ACTION REQUIRED" else "Recommended Action (${plan.name})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isEmergency) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (isEmergency) "Call 911 or visit the nearest Emergency Room immediately." 
                        else "Use '${plan.urgentCareNetworkQuery}' for in-network care. Your copay is $${copay}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEmergency) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val buttonText = when {
                        isEmergency -> "CALL 911 NOW"
                        severityLevel == SeverityLevel.TELEHEALTH -> "Start Video Visit"
                        else -> "Open Directions"
                    }
                    
                    Button(
                        onClick = { /* TODO: Dialer or Maps/URL */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (isEmergency) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                    ) {
                        if (isEmergency) {
                            Icon(Icons.Default.Phone, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(buttonText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // De-identification Escalation
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Secure Escalation", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        "Need to share records with a doctor? Our privacy loop scrubs your identity on-device before sending.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    if (!showDeidFlow) {
                        Button(
                            onClick = { showDeidFlow = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share De-identified Records")
                        }
                    } else {
                        // Simulated De-id Progress
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Anonymizing [PATIENT_NAME]...", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                            Text("Sent securely to provider.", color = Color(0xFF27AE60), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // AI Confidence Meter
            if (confidence > 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Confidence", style = MaterialTheme.typography.labelSmall)
                        Text("${(confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = confidence.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = if (confidence > 0.9) Color(0xFF27AE60) else if (confidence > 0.7) Color(0xFFF1C40F) else MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

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
