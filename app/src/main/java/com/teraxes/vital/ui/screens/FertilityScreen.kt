package com.teraxes.vital.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.domain.CycleUtils
import com.teraxes.vital.domain.DayPhaseType
import com.teraxes.vital.ui.components.CyclePhaseEducationDialog
import com.teraxes.vital.ui.theme.*
import com.teraxes.vital.ui.viewmodel.VitalViewModel

@Composable
fun FertilityScreen(
    viewModel: VitalViewModel
) {
    val stats by viewModel.cycleStats.collectAsState()
    val predictions by viewModel.cyclePredictions.collectAsState()

    var customCycleLength by remember { mutableStateOf(stats.averageLength.toString()) }
    var calcResult by remember { mutableStateOf<String?>(null) }
    var showPhaseEducationDialog by remember { mutableStateOf(false) }
    var educationInitialPhase by remember { mutableStateOf<DayPhaseType?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Conception Chance Header Card
        item {
            val risk = CycleUtils.calculateFertilityRisk(stats.currentDay, stats.averageLength)
            val (bgColor, textColor, statusText) = when (risk) {
                "high" -> Triple(HighRiskBg, HighRiskText, "High Fertility Day")
                "medium" -> Triple(MedRiskBg, MedRiskText, "Moderate Fertility Day")
                else -> Triple(LowRiskBg, LowRiskText, "Low Fertility Day")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fertility_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = "Fertility",
                        tint = textColor,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Cycle Day ${stats.currentDay} • ${stats.currentPhase.replaceFirstChar { it.uppercase() }} Phase",
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            educationInitialPhase = if (risk == "high") DayPhaseType.OVULATION else DayPhaseType.FERTILE
                            showPhaseEducationDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = textColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Learn about your fertile window", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                }
            }
        }

        // Fertile Window & Ovulation Prediction
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("predictions_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fertility Schedule & Predictions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (predictions == null) {
                        Text("Log your period cycles to calculate fertile window and ovulation dates.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        PredictionRow(
                            icon = Icons.Default.Star,
                            label = "Estimated Ovulation",
                            value = CycleUtils.formatDate(predictions!!.ovulationDate),
                            color = PhaseOvulation
                        )
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        PredictionRow(
                            icon = Icons.Default.DateRange,
                            label = "Fertile Window",
                            value = "${CycleUtils.formatDateShort(predictions!!.fertileWindowStart)} - ${CycleUtils.formatDateShort(predictions!!.fertileWindowEnd)}",
                            color = PhaseFertile
                        )
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        PredictionRow(
                            icon = Icons.Default.WaterDrop,
                            label = "Next Period Expected",
                            value = CycleUtils.formatDate(predictions!!.nextPeriodDate),
                            color = PhaseMenstrual
                        )
                    }
                }
            }
        }

        // Custom Ovulation Calculator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fertility_calculator_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ovulation Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Calculate ovulation date for any custom cycle length.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customCycleLength,
                        onValueChange = { customCycleLength = it },
                        label = { Text("Average Cycle Length (Days)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val len = customCycleLength.toIntOrNull() ?: 28
                            val ovDay = (len - 14).coerceAtLeast(10)
                            calcResult = "For a $len-day cycle, ovulation typically occurs on Day $ovDay. Your fertile window spans Days ${ovDay - 5} through ${ovDay + 2}."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                    ) {
                        Text("Calculate Fertile Window")
                    }

                    if (calcResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RosePrimaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = calcResult!!,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                color = RoseTertiary
                            )
                        }
                    }
                }
            }
        }

        // Dedicated Rhythm Pregnancy Prevention Method Engine (Ogino-Knaus & Standard Days Method)
        item {
            var inputShortest by remember(stats) { mutableStateOf(stats.minLength.toString()) }
            var inputLongest by remember(stats) { mutableStateOf(stats.maxLength.toString()) }

            val customRhythmResult = remember(inputShortest, inputLongest, stats) {
                val s = inputShortest.toIntOrNull() ?: stats.minLength
                val l = inputLongest.toIntOrNull() ?: stats.maxLength
                CycleUtils.calculateRhythmMethod(stats, s, l)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rhythm_method_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = RosePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Rhythm Pregnancy Prevention Method",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ogino-Knaus & Standard Days Algorithm",
                                fontSize = 11.sp,
                                color = RoseTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Current Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (customRhythmResult.isCurrentDayUnsafe) HighRiskBg else LowRiskBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (customRhythmResult.isCurrentDayUnsafe) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (customRhythmResult.isCurrentDayUnsafe) HighRiskText else LowRiskText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (customRhythmResult.isCurrentDayUnsafe) "FERTILE / UNSAFE WINDOW" else "INFERTILE / SAFE PERIOD",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (customRhythmResult.isCurrentDayUnsafe) HighRiskText else LowRiskText
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = customRhythmResult.guidanceNote,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = if (customRhythmResult.isCurrentDayUnsafe) HighRiskText else LowRiskText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Formula Breakdown
                    Text(
                        text = "Ogino-Knaus Calculation Formula:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "First Unsafe Day (Shortest - 18):", fontSize = 12.sp)
                                Text(
                                    text = "Day ${customRhythmResult.oginoFirstUnsafeDay}",
                                    fontWeight = FontWeight.Bold,
                                    color = RosePrimary,
                                    fontSize = 12.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Last Unsafe Day (Longest - 11):", fontSize = 12.sp)
                                Text(
                                    text = "Day ${customRhythmResult.oginoLastUnsafeDay}",
                                    fontWeight = FontWeight.Bold,
                                    color = RosePrimary,
                                    fontSize = 12.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Unsafe Abstinence Span:", fontSize = 12.sp)
                                Text(
                                    text = "Cycle Days ${customRhythmResult.oginoFirstUnsafeDay} to ${customRhythmResult.oginoLastUnsafeDay}",
                                    fontWeight = FontWeight.Bold,
                                    color = HighRiskText,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Custom Cycle Length Modifiers
                    Text(
                        text = "Customize Cycle Length Limits for Rhythm Calculations:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputShortest,
                            onValueChange = { inputShortest = it },
                            label = { Text("Shortest Cycle (Days)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputLongest,
                            onValueChange = { inputLongest = it },
                            label = { Text("Longest Cycle (Days)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Method Suitability: ${customRhythmResult.methodReliabilityRating}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }
            }
        }

        // Key Facts & Reliability Advisory Card (Rhythm Method & Pregnancy Prevention)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reliability_facts_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Key Facts & Method Reliability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Important health facts regarding calendar/rhythm method predictions and fertility tracking:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                    FactItem(
                        icon = Icons.Default.Warning,
                        title = "Method Failure Rate (12% – 24%)",
                        description = "The calendar/rhythm method alone has an estimated typical-use failure rate of 12% to 24% per year when relied upon solely for pregnancy prevention."
                    )

                    FactItem(
                        icon = Icons.Default.SyncProblem,
                        title = "Natural Cycle Fluctuations",
                        description = "Ovulation timing can shift unexpectedly due to stress, illness, travel, weight changes, hormonal shifts, or sleep disruptions."
                    )

                    FactItem(
                        icon = Icons.Default.Timer,
                        title = "Sperm & Egg Lifespan",
                        description = "Sperm can survive up to 5 days inside the female reproductive tract, while an unfertilized egg survives 12 to 24 hours after ovulation."
                    )

                    FactItem(
                        icon = Icons.Default.FactCheck,
                        title = "Optional Symptothermal Methods",
                        description = "For users who wish to enhance calendar predictions, symptom logging (cervical mucus, cramps) or optional temperature (BBT) tracking can be added, though temperature is never required."
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalInformation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Medical Disclaimer: Vital provides cycle estimations for educational awareness only. Consult a healthcare provider for personalized contraceptive or family planning advice.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPhaseEducationDialog) {
        CyclePhaseEducationDialog(
            initialPhase = educationInitialPhase,
            onDismiss = { showPhaseEducationDialog = false }
        )
    }
}

@Composable
fun FactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun PredictionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
    }
}
