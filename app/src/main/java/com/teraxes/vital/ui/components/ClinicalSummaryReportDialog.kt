package com.teraxes.vital.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.teraxes.vital.data.model.CycleEntity
import com.teraxes.vital.data.model.HealthMetricEntity
import com.teraxes.vital.data.model.SymptomLogEntity
import com.teraxes.vital.domain.HealthReportService
import com.teraxes.vital.domain.ClinicalRedFlag
import com.teraxes.vital.ui.theme.*

@Composable
fun ClinicalSummaryReportDialog(
    userName: String,
    cycles: List<CycleEntity>,
    symptoms: List<SymptomLogEntity>,
    healthMetrics: List<HealthMetricEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val redFlags = remember(cycles, symptoms, healthMetrics) {
        HealthReportService.evaluateClinicalRedFlags(cycles, symptoms, healthMetrics)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = RosePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Provider Summary Dossier",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Formatted for Gynecologist & PCP Review",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Clinical Triaging Section
                    Text(
                        text = "CLINICAL TRIAGING & FLAGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RosePrimary,
                        letterSpacing = 0.8.sp
                    )

                    redFlags.forEach { flag ->
                        val flagColor = when (flag.severity) {
                            "High" -> Color(0xFFD32F2F)
                            "Moderate" -> Color(0xFFF57C00)
                            else -> Color(0xFF388E3C)
                        }
                        val flagBg = when (flag.severity) {
                            "High" -> Color(0xFFFFEBEE)
                            "Moderate" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE8F5E9)
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = flagBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (flag.severity == "High") Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = flagColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = flag.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = flagColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = flag.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Key Statistics Summary
                    Text(
                        text = "RECORDED METRICS SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RosePrimary,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportStatTile(
                            label = "Cycles Logged",
                            value = "${cycles.size}",
                            modifier = Modifier.weight(1f)
                        )
                        ReportStatTile(
                            label = "Symptoms",
                            value = "${symptoms.size}",
                            modifier = Modifier.weight(1f)
                        )
                        ReportStatTile(
                            label = "Vitals / BBT",
                            value = "${healthMetrics.size}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Report Info Note
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "All clinical data is compiled locally on-device and formatted as a printable HTML dossier for your private medical consultation.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            val reportHtml = HealthReportService.generateHtmlReport(
                                userName = userName,
                                cycles = cycles,
                                symptoms = symptoms,
                                healthMetrics = healthMetrics
                            )
                            HealthReportService.shareReport(context, reportHtml)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export & Share")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = RosePrimary)
            Text(text = label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
