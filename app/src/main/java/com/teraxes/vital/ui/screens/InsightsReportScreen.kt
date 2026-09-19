package com.teraxes.vital.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.data.model.CycleEntity
import com.teraxes.vital.data.model.HealthMetricEntity
import com.teraxes.vital.data.model.SymptomLogEntity
import com.teraxes.vital.domain.HealthReportService
import com.teraxes.vital.ui.components.ClinicalSummaryReportDialog
import com.teraxes.vital.ui.theme.RosePrimaryContainer
import com.teraxes.vital.ui.theme.RoseTertiary

@Composable
fun InsightsReportScreen(
    userName: String,
    insights: List<String>,
    cycles: List<CycleEntity>,
    symptoms: List<SymptomLogEntity>,
    healthMetrics: List<HealthMetricEntity> = emptyList()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showClinicalSummaryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Health Insights & Clinical Dossier",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        // 1. Clinical Summary & Gynecologist Report Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clinical Summary & Doctor Export",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Generate a comprehensive clinical summary with ACOG red-flag triaging, cycle length variability, pain severity, and BBT thermal shifts formatted for your gynecologist.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray, lineHeight = 20.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showClinicalSummaryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View & Export Clinical Dossier")
                }
            }
        }

        // 2. Pattern Correlation Insights
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RosePrimaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = RoseTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Symptom Pattern Insights",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = RoseTertiary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (insights.isEmpty()) {
                    Text(
                        text = "Log symptoms consistently across cycle days to unlock pattern correlations.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = RoseTertiary)
                    )
                } else {
                    insights.forEach { insight ->
                        Text(
                            text = "• $insight",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = RoseTertiary,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 3. Quick Share HTML Report Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Share HTML Document",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Directly share a formatted document via email, WhatsApp, or cloud drive.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray, lineHeight = 20.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        val reportHtml = HealthReportService.generateHtmlReport(userName, cycles, symptoms, healthMetrics)
                        HealthReportService.shareReport(context, reportHtml)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instant Share HTML Report")
                }
            }
        }
    }

    if (showClinicalSummaryDialog) {
        ClinicalSummaryReportDialog(
            userName = userName,
            cycles = cycles,
            symptoms = symptoms,
            healthMetrics = healthMetrics,
            onDismiss = { showClinicalSummaryDialog = false }
        )
    }
}
