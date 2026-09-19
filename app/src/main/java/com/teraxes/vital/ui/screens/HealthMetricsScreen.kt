package com.teraxes.vital.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.data.model.CycleEntity
import com.teraxes.vital.data.model.HealthMetricEntity
import com.teraxes.vital.data.model.SymptomLogEntity
import com.teraxes.vital.domain.CycleStats
import com.teraxes.vital.domain.HealthReportService
import com.teraxes.vital.ui.components.BiphasicBbtChart
import com.teraxes.vital.ui.components.ClinicalSummaryReportDialog
import com.teraxes.vital.ui.components.CycleSyncingGuidanceCard
import com.teraxes.vital.ui.theme.*
import com.teraxes.vital.ui.viewmodel.VitalViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun HealthMetricsScreen(viewModel: VitalViewModel) {
    val metrics by viewModel.healthMetrics.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val symptoms by viewModel.symptoms.collectAsState()
    val stats by viewModel.cycleStats.collectAsState()
    val userName by viewModel.userName.collectAsState()

    HealthMetricsScreenContent(
        metrics = metrics,
        cycles = cycles,
        symptoms = symptoms,
        stats = stats,
        userName = userName,
        onLogMetric = { type, valDouble, unit, date, notes ->
            viewModel.logMetric(type, valDouble, unit, date, notes)
        },
        onDeleteMetric = { id -> viewModel.deleteMetric(id) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthMetricsScreenContent(
    metrics: List<HealthMetricEntity>,
    cycles: List<CycleEntity>,
    symptoms: List<SymptomLogEntity>,
    stats: CycleStats,
    userName: String,
    onLogMetric: (type: String, value: Double, unit: String, date: String, notes: String) -> Unit,
    onDeleteMetric: (Long) -> Unit
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Dialog & UI State
    var activeModalType by remember { mutableStateOf<String?>(null) }
    var showDoctorReportDialog by remember { mutableStateOf(false) }
    var selectedChartMetric by remember { mutableStateOf("temperature") }
    var historyFilterType by remember { mutableStateOf("all") }
    var showEducationalGuide by remember { mutableStateOf(false) }
    var selectedSeasonTab by remember { mutableStateOf(stats.currentPhase.lowercase()) }

    // Derived today's / latest vitals
    val todayWaterIntake = remember(metrics, todayStr) {
        metrics.filter { it.type == "water" && it.loggedDate == todayStr }
            .sumOf { it.value }
    }
    val latestBbt = remember(metrics) {
        metrics.filter { it.type == "temperature" }.maxByOrNull { it.loggedDate }
    }
    val latestWeight = remember(metrics) {
        metrics.filter { it.type == "weight" }.maxByOrNull { it.loggedDate }
    }
    val latestSleep = remember(metrics) {
        metrics.filter { it.type == "sleep" }.maxByOrNull { it.loggedDate }
    }
    val latestBp = remember(metrics) {
        metrics.filter { it.type == "blood_pressure" }.maxByOrNull { it.loggedDate }
    }

    // Filtered logs for history
    val filteredLogs = remember(metrics, historyFilterType) {
        if (historyFilterType == "all") metrics.sortedByDescending { it.id }
        else metrics.filter { it.type == historyFilterType }.sortedByDescending { it.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Screen Header & Quick Report Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Health & Body Vitals",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Track your biometrics in sync with your cycle",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                FilledTonalButton(
                    onClick = { showDoctorReportDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Doctor Dossier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Cycle Phase & Vitals Sync Banner
        item {
            CycleVitalsSyncBanner(
                currentPhase = stats.currentPhase,
                cycleDay = stats.currentDay,
                onExplainClick = { showEducationalGuide = true }
            )
        }

        // 3. Quick-Add Floating Action Row
        item {
            Text(
                text = "Quick Log Biometrics",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    QuickActionChip(
                        icon = Icons.Default.WaterDrop,
                        label = "+250ml Water",
                        color = Color(0xFF0288D1),
                        onClick = {
                            onLogMetric("water", 250.0, "ml", todayStr, "Quick cup of water")
                            Toast.makeText(context, "Added 250ml water", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item {
                    QuickActionChip(
                        icon = Icons.Default.WaterDrop,
                        label = "+500ml Bottle",
                        color = Color(0xFF0288D1),
                        onClick = {
                            onLogMetric("water", 500.0, "ml", todayStr, "Water bottle hydration")
                            Toast.makeText(context, "Added 500ml water", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item {
                    QuickActionChip(
                        icon = Icons.Default.Thermostat,
                        label = "Log BBT",
                        color = PhaseOvulation,
                        onClick = { activeModalType = "temperature" }
                    )
                }
                item {
                    QuickActionChip(
                        icon = Icons.Default.MonitorWeight,
                        label = "Log Weight",
                        color = Color(0xFF673AB7),
                        onClick = { activeModalType = "weight" }
                    )
                }
                item {
                    QuickActionChip(
                        icon = Icons.Default.Bedtime,
                        label = "Log Sleep",
                        color = Color(0xFF3F51B5),
                        onClick = { activeModalType = "sleep" }
                    )
                }
                item {
                    QuickActionChip(
                        icon = Icons.Default.Favorite,
                        label = "Log Blood Pressure",
                        color = PhaseMenstrual,
                        onClick = { activeModalType = "blood_pressure" }
                    )
                }
            }
        }

        // 4. Vitals Summary Cards (Hydration, BBT, Weight, Sleep, BP)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Hydration Main Card
                HydrationTrackerCard(
                    todayIntakeMl = todayWaterIntake,
                    goalMl = 2000.0,
                    onQuickAdd250 = {
                        onLogMetric("water", 250.0, "ml", todayStr, "Cup of water")
                        Toast.makeText(context, "Added 250ml water", Toast.LENGTH_SHORT).show()
                    },
                    onQuickAdd500 = {
                        onLogMetric("water", 500.0, "ml", todayStr, "Bottle of water")
                        Toast.makeText(context, "Added 500ml water", Toast.LENGTH_SHORT).show()
                    },
                    onCustomLog = { activeModalType = "water" }
                )

                // 2x2 Grid of Vital Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // BBT Card
                    VitalGlanceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Thermostat,
                        iconTint = PhaseOvulationText,
                        iconBg = PhaseOvulationBg,
                        title = "Basal Temp (BBT)",
                        value = if (latestBbt != null) "${latestBbt.value} ${latestBbt.unit}" else "Not logged",
                        subtitle = if (latestBbt != null) {
                            if (latestBbt.value >= 36.6 || (latestBbt.unit == "°F" && latestBbt.value >= 97.9)) "Luteal shift detected" else "Follicular baseline"
                        } else "Log waking temp",
                        onClick = { activeModalType = "temperature" }
                    )

                    // Weight & Fluid Retention Card
                    VitalGlanceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MonitorWeight,
                        iconTint = Color(0xFF673AB7),
                        iconBg = Color(0xFFEDE7F6),
                        title = "Body Weight",
                        value = if (latestWeight != null) "${latestWeight.value} ${latestWeight.unit}" else "Not logged",
                        subtitle = if (stats.currentPhase.lowercase() == "luteal") "Luteal fluid balance" else "Normal baseline",
                        onClick = { activeModalType = "weight" }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sleep Card
                    VitalGlanceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bedtime,
                        iconTint = Color(0xFF3F51B5),
                        iconBg = Color(0xFFE8EAF6),
                        title = "Sleep & Rest",
                        value = if (latestSleep != null) "${latestSleep.value} hrs" else "Not logged",
                        subtitle = latestSleep?.notes?.ifEmpty { "Target: 7-9 hrs" } ?: "Target: 7-9 hrs",
                        onClick = { activeModalType = "sleep" }
                    )

                    // Blood Pressure Card
                    VitalGlanceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Favorite,
                        iconTint = PhaseMenstrual,
                        iconBg = PhaseMenstrualBg,
                        title = "Blood Pressure",
                        value = if (latestBp != null) "${latestBp.value.toInt()}${if (latestBp.notes.contains("/")) "/${latestBp.notes.substringAfter("/").substringBefore(" ")}" else ""} ${latestBp.unit}" else "Not logged",
                        subtitle = if (latestBp != null) "Resting Vitals" else "Track cardiovascular",
                        onClick = { activeModalType = "blood_pressure" }
                    )
                }
            }
        }

        // 5. Interactive Visual Trend Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Biometric Trends & Curve",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Last 14 Days",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metric Selectors
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("temperature", "🌡️ BBT Curve", PhaseOvulation),
                            Triple("water", "💧 Water Intake", Color(0xFF0288D1)),
                            Triple("weight", "⚖️ Weight", Color(0xFF673AB7)),
                            Triple("sleep", "😴 Sleep", Color(0xFF3F51B5)),
                            Triple("blood_pressure", "❤️ BP Systolic", PhaseMenstrual)
                        ).forEach { (type, label, _) ->
                            FilterChip(
                                selected = selectedChartMetric == type,
                                onClick = { selectedChartMetric = type },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Canvas Visualizer
                    val chartData = remember(metrics, selectedChartMetric) {
                        metrics.filter { it.type == selectedChartMetric }
                            .sortedBy { it.loggedDate }
                            .takeLast(14)
                    }

                    BiometricCanvasChart(
                        metricType = selectedChartMetric,
                        data = chartData
                    )
                }
            }
        }

        // 6. Advanced Biphasic BBT & Cervical Mucus Thermal Shift Chart
        item {
            BiphasicBbtChart(
                metrics = metrics,
                cycleStats = stats,
                onLogBbtClick = { activeModalType = "temperature" }
            )
        }

        // 7. Phase-Based Lifestyle, Nutrition & Workout Cycle Syncing
        item {
            CycleSyncingGuidanceCard(
                cycleStats = stats
            )
        }

        // 8. Cycle-Synced Body Coaching (Nutrition, Movement & Energy)
        item {
            CycleSyncedCoachingCard(
                activePhase = stats.currentPhase,
                selectedTab = selectedSeasonTab,
                onTabSelect = { selectedSeasonTab = it }
            )
        }

        // 7. Educational Guide Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEducationalGuide = !showEducationalGuide }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How Your Cycle Drives Your Vitals",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            imageVector = if (showEducationalGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = showEducationalGuide) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EducationalGuideItem(
                                title = "🌡️ Biphasic BBT & Ovulation Confirmation",
                                description = "After ovulation, progesterone produced by the corpus luteum raises your baseline waking temperature by 0.3°C - 0.5°C (0.5°F - 1.0°F) throughout the luteal phase. A sustained rise confirms ovulation occurred."
                            )
                            EducationalGuideItem(
                                title = "⚖️ Luteal Fluid Balance vs Real Weight",
                                description = "High progesterone and aldosterone in the late luteal phase cause transient sodium and water retention. A 0.5 - 2 kg weight fluctuation before your period is normal fluid and subsides once menstruation starts."
                            )
                            EducationalGuideItem(
                                title = "💧 Hydration & Menstrual Cramp Relief",
                                description = "Drinking ample water reduces vasopressin and prostaglandin concentration in uterine tissue, which directly eases menstrual muscle spasms and diminishes premenstrual headaches."
                            )
                            EducationalGuideItem(
                                title = "😴 Sleep Architecture & Progesterone",
                                description = "Body temperature remains slightly elevated during the luteal phase, which can disrupt REM sleep and cause vivid dreams. Keeping your bedroom cool (18-19°C) promotes deeper restorative sleep."
                            )
                        }
                    }
                }
            }
        }

        // 8. Recorded Logs History & Deletion
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logged Biometrics History",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // History Filter
                var expandedFilter by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expandedFilter = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (historyFilterType == "all") "Filter: All" else historyFilterType.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp
                        )
                    }
                    DropdownMenu(
                        expanded = expandedFilter,
                        onDismissRequest = { expandedFilter = false }
                    ) {
                        DropdownMenuItem(text = { Text("All Metrics") }, onClick = { historyFilterType = "all"; expandedFilter = false })
                        DropdownMenuItem(text = { Text("Temperature (BBT)") }, onClick = { historyFilterType = "temperature"; expandedFilter = false })
                        DropdownMenuItem(text = { Text("Water Intake") }, onClick = { historyFilterType = "water"; expandedFilter = false })
                        DropdownMenuItem(text = { Text("Weight") }, onClick = { historyFilterType = "weight"; expandedFilter = false })
                        DropdownMenuItem(text = { Text("Sleep") }, onClick = { historyFilterType = "sleep"; expandedFilter = false })
                        DropdownMenuItem(text = { Text("Blood Pressure") }, onClick = { historyFilterType = "blood_pressure"; expandedFilter = false })
                    }
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No metrics recorded in this category yet. Use the quick log buttons above to record your first entry.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { metric ->
                MetricHistoryCard(
                    metric = metric,
                    onDelete = { onDeleteMetric(metric.id) }
                )
            }
        }
    }

    // Modal Sheet / Dialogs for Biometric Entry
    if (activeModalType != null) {
        BiometricEntryDialog(
            type = activeModalType!!,
            todayDateStr = todayStr,
            onDismiss = { activeModalType = null },
            onSave = { valDouble, unit, date, notes ->
                onLogMetric(activeModalType!!, valDouble, unit, date, notes)
                activeModalType = null
            }
        )
    }

    // Clinical Summary & Gynecologist Report Dialog
    if (showDoctorReportDialog) {
        ClinicalSummaryReportDialog(
            userName = userName,
            cycles = cycles,
            symptoms = symptoms,
            healthMetrics = metrics,
            onDismiss = { showDoctorReportDialog = false }
        )
    }
}

// -------------------------------------------------------------
// Sub-components: Vitals Sync Banner, Glance Cards, Charts
// -------------------------------------------------------------

@Composable
fun CycleVitalsSyncBanner(
    currentPhase: String,
    cycleDay: Int,
    onExplainClick: () -> Unit
) {
    val phaseLower = currentPhase.lowercase()
    val (phaseColor, phaseBg, phaseBorder, phaseTitle, phaseBody) = when (phaseLower) {
        "menstrual" -> Tuple5(
            PhaseMenstrual,
            PhaseMenstrualBg,
            PhaseMenstrualBorder,
            "Menstrual Season • Day $cycleDay",
            "Estrogen & progesterone are at their lowest baseline. Prioritize iron-rich hydration, gentle recovery, and magnesium."
        )
        "follicular" -> Tuple5(
            PhaseFollicularText,
            PhaseFollicularBg,
            PhaseFollicularBorder,
            "Follicular Season • Day $cycleDay",
            "Estrogen is steadily rising. Energy, metabolism, and stamina are peaking. Ideal window for progressive strength training."
        )
        "ovulation" -> Tuple5(
            PhaseOvulationText,
            PhaseOvulationBg,
            PhaseOvulationBorder,
            "Ovulatory Window • Day $cycleDay",
            "LH surge triggers ovulation. Look for a 0.3-0.5°C thermal BBT shift in the coming 24-48 hours."
        )
        else -> Tuple5(
            PhaseLutealText,
            PhaseLutealBg,
            PhaseLutealBorder,
            "Luteal Season • Day $cycleDay",
            "Progesterone dominates, elevating basal body temperature (+0.4°C) and promoting mild temporary fluid retention."
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = phaseBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, phaseBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = phaseColor.copy(alpha = 0.2f), modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = phaseColor, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = phaseTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = phaseColor)
                    )
                }

                IconButton(onClick = onExplainClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Learn more", tint = phaseColor, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = phaseBody,
                style = MaterialTheme.typography.bodySmall.copy(color = phaseColor.copy(alpha = 0.85f), lineHeight = 18.sp)
            )
        }
    }
}

@Composable
fun HydrationTrackerCard(
    todayIntakeMl: Double,
    goalMl: Double = 2000.0,
    onQuickAdd250: () -> Unit,
    onQuickAdd500: () -> Unit,
    onCustomLog: () -> Unit
) {
    val progress = (todayIntakeMl / goalMl).toFloat().coerceIn(0f, 1f)
    val percentage = (progress * 100).roundToInt()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB3E5FC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFF0288D1).copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Hydration Goal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF01579B))
                        )
                        Text(
                            text = "${todayIntakeMl.toInt()} / ${goalMl.toInt()} ml ($percentage% reached)",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0277BD))
                        )
                    }
                }

                TextButton(onClick = onCustomLog) {
                    Text("Custom", color = Color(0xFF0288D1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF0288D1),
                trackColor = Color(0xFFE1F5FE)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickAdd250,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+250 ml (Glass)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onQuickAdd500,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE1F5FE), contentColor = Color(0xFF01579B))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+500 ml (Bottle)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VitalGlanceCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconBg,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                }

                Icon(
                    Icons.Default.AddCircleOutline,
                    contentDescription = "Log",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = iconTint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
fun QuickActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// -------------------------------------------------------------
// Canvas Biometric Trend Chart
// -------------------------------------------------------------

@Composable
fun BiometricCanvasChart(
    metricType: String,
    data: List<HealthMetricEntity>
) {
    if (data.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recorded readings for ${metricType.replace('_', ' ')} yet.\nLog multiple days to display the trend line.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val primaryColor = when (metricType) {
        "temperature" -> PhaseOvulation
        "water" -> Color(0xFF0288D1)
        "weight" -> Color(0xFF673AB7)
        "sleep" -> Color(0xFF3F51B5)
        else -> PhaseMenstrual
    }

    val values = data.map { it.value }
    val minVal = (values.minOrNull() ?: 0.0)
    val maxVal = (values.maxOrNull() ?: 10.0)
    val range = (if (maxVal == minVal) 1.0 else maxVal - minVal)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val padY = 20f
                val padX = 24f
                val graphWidth = width - (padX * 2)
                val graphHeight = height - (padY * 2)

                // Draw background horizontal gridlines
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = padY + (graphHeight * (i.toFloat() / gridLines))
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(padX, y),
                        end = Offset(width - padX, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // If BBT chart, draw luteal shift reference line (~36.6°C)
                if (metricType == "temperature" && minVal <= 36.6 && maxVal >= 36.6) {
                    val shiftRatio = ((36.6 - minVal) / range).toFloat()
                    val shiftY = (padY + graphHeight) - (shiftRatio * graphHeight)
                    drawLine(
                        color = PhaseOvulation.copy(alpha = 0.5f),
                        start = Offset(padX, shiftY),
                        end = Offset(width - padX, shiftY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                if (data.size == 1) {
                    // Single point
                    val cx = width / 2f
                    val cy = padY + (graphHeight / 2f)
                    drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(cx, cy))
                } else {
                    val stepX = graphWidth / (data.size - 1)
                    val points = data.mapIndexed { idx, item ->
                        val x = padX + (idx * stepX)
                        val norm = ((item.value - minVal) / range).toFloat()
                        val y = (padY + graphHeight) - (norm * graphHeight)
                        Offset(x, y)
                    }

                    // Line Path
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    // Fill Gradient below line
                    val fillPath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                        lineTo(points.last().x, padY + graphHeight)
                        lineTo(points.first().x, padY + graphHeight)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = padY,
                            endY = padY + graphHeight
                        )
                    )

                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Data Points
                    points.forEach { pt ->
                        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                        drawCircle(color = primaryColor, radius = 3.5.dp.toPx(), center = pt)
                    }
                }
            }
        }

        // X-Axis Date Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val firstDate = data.firstOrNull()?.loggedDate?.takeLast(5) ?: ""
            val lastDate = data.lastOrNull()?.loggedDate?.takeLast(5) ?: ""
            Text(text = "Date: $firstDate", fontSize = 10.sp, color = Color.Gray)
            Text(text = "Range: ${String.format("%.1f", minVal)} - ${String.format("%.1f", maxVal)} ${data.firstOrNull()?.unit ?: ""}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Text(text = "Latest: $lastDate", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

// -------------------------------------------------------------
// Cycle Synced Nutrition & Movement Planner
// -------------------------------------------------------------

@Composable
fun CycleSyncedCoachingCard(
    activePhase: String,
    selectedTab: String,
    onTabSelect: (String) -> Unit
) {
    val phases = listOf(
        Triple("menstrual", "Menstrual", PhaseMenstrual),
        Triple("follicular", "Follicular", PhaseFollicularText),
        Triple("ovulation", "Ovulatory", PhaseOvulationText),
        Triple("luteal", "Luteal", PhaseLutealText)
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cycle-Synced Body Coaching",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Active: ${activePhase.replaceFirstChar { it.uppercase() }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                phases.forEach { (key, label, tabColor) ->
                    val isSelected = selectedTab == key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) tabColor.copy(alpha = 0.15f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) tabColor else Color.LightGray.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onTabSelect(key) }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) tabColor else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Coaching Details by selected phase
            when (selectedTab) {
                "menstrual" -> CoachingPhaseDetail(
                    nutrition = "Iron & magnesium focus: Spinach, red lentils, pumpkin seeds, dark chocolate (70%+), and bone broth to replenish blood loss.",
                    movement = "Restorative movement: Gentle pelvic Yin yoga, 20-30 min scenic walks, and foam rolling. Avoid high-stress exhaustion.",
                    vitalTip = "Resting heart rate drops to its lowest baseline. Drink warm herbal teas (ginger, raspberry leaf) for cramp relief."
                )
                "follicular" -> CoachingPhaseDetail(
                    nutrition = "Fresh, light & energizing: Lean proteins, sprouted grains, citrus fruits, and fermented foods to support estrogen surge.",
                    movement = "High stamina & strength building: Progressive weight training, cardio, and high-energy workout routines.",
                    vitalTip = "Insulin sensitivity is peak. Your basal body temp sits at baseline (~36.2°C - 36.5°C). Great time to hit personal records."
                )
                "ovulation" -> CoachingPhaseDetail(
                    nutrition = "Antioxidant & liver support: Cruciferous vegetables (broccoli, Brussels sprouts), wild berries, avocado, and high fiber.",
                    movement = "Peak anaerobic power: HIIT sessions, heavy resistance sets, sprints, and intense cardiovascular exercise.",
                    vitalTip = "LH surge occurs. Cervical mucus shifts to clear/stretchy. BBT will jump 0.3°C+ within 24 hours of ovulation."
                )
                else -> CoachingPhaseDetail(
                    nutrition = "Complex slow-burn carbs & Vitamin B6: Sweet potatoes, quinoa, oats, walnuts, and magnesium to balance serotonin and curb PMS cravings.",
                    movement = "Lower-intensity strength & Pilates: Moderate resistance sets, barre, swimming, and functional mobility. Avoid overtraining.",
                    vitalTip = "Metabolic burn increases by 100-300 kcal/day. Progesterone increases BBT (~36.6°C - 37.1°C) and induces temporary fluid retention."
                )
            }
        }
    }
}

@Composable
fun CoachingPhaseDetail(
    nutrition: String,
    movement: String,
    vitalTip: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CoachingItem(icon = Icons.Default.Restaurant, title = "Nutrition Focus", text = nutrition, color = Color(0xFF2E7D32))
        CoachingItem(icon = Icons.Default.DirectionsRun, title = "Movement & Workouts", text = movement, color = Color(0xFFD81B60))
        CoachingItem(icon = Icons.Default.FavoriteBorder, title = "Hormones & Vitals", text = vitalTip, color = Color(0xFF1565C0))
    }
}

@Composable
fun CoachingItem(icon: ImageVector, title: String, text: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            }
        }
        Column {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
fun EducationalGuideItem(title: String, description: String) {
    Column {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
    }
}

// -------------------------------------------------------------
// History Log Item
// -------------------------------------------------------------

@Composable
fun MetricHistoryCard(
    metric: HealthMetricEntity,
    onDelete: () -> Unit
) {
    val (icon, color, label) = when (metric.type) {
        "temperature" -> Triple(Icons.Default.Thermostat, PhaseOvulationText, "Basal Body Temp (BBT)")
        "water" -> Triple(Icons.Default.WaterDrop, Color(0xFF0288D1), "Hydration Intake")
        "weight" -> Triple(Icons.Default.MonitorWeight, Color(0xFF673AB7), "Body Weight")
        "sleep" -> Triple(Icons.Default.Bedtime, Color(0xFF3F51B5), "Sleep Duration")
        "blood_pressure" -> Triple(Icons.Default.Favorite, PhaseMenstrual, "Blood Pressure")
        else -> Triple(Icons.Default.ShowChart, MaterialTheme.colorScheme.primary, metric.type.replace('_', ' ').replaceFirstChar { it.uppercase() })
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }

                Column {
                    Text(
                        text = "$label: ${if (metric.value % 1.0 == 0.0) metric.value.toInt().toString() else metric.value.toString()} ${metric.unit}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${metric.loggedDate}${if (metric.notes.isNotEmpty()) " • " + metric.notes else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = Color.Gray.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// Biometric Entry Dialog Modal
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BiometricEntryDialog(
    type: String,
    todayDateStr: String,
    onDismiss: () -> Unit,
    onSave: (value: Double, unit: String, date: String, notes: String) -> Unit
) {
    var valueInput by remember { mutableStateOf("") }
    var secondaryInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(todayDateStr) }
    var selectedUnit by remember(type) {
        mutableStateOf(
            when (type) {
                "temperature" -> "°C"
                "weight" -> "kg"
                "water" -> "ml"
                "sleep" -> "hrs"
                "blood_pressure" -> "mmHg"
                else -> ""
            }
        )
    }

    val title = when (type) {
        "temperature" -> "Log Basal Body Temp (BBT)"
        "weight" -> "Log Body Weight"
        "water" -> "Log Hydration Intake"
        "sleep" -> "Log Sleep & Recovery"
        "blood_pressure" -> "Log Blood Pressure"
        else -> "Log Health Metric"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Unit Toggles for BBT and Weight
                if (type == "temperature") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedUnit == "°C",
                            onClick = { selectedUnit = "°C" },
                            label = { Text("Celsius (°C)") }
                        )
                        FilterChip(
                            selected = selectedUnit == "°F",
                            onClick = { selectedUnit = "°F" },
                            label = { Text("Fahrenheit (°F)") }
                        )
                    }
                    Text(
                        text = "Measure immediately upon waking before getting out of bed.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                if (type == "weight") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedUnit == "kg",
                            onClick = { selectedUnit = "kg" },
                            label = { Text("Kilograms (kg)") }
                        )
                        FilterChip(
                            selected = selectedUnit == "lbs",
                            onClick = { selectedUnit = "lbs" },
                            label = { Text("Pounds (lbs)") }
                        )
                    }
                }

                if (type == "blood_pressure") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = valueInput,
                            onValueChange = { valueInput = it },
                            label = { Text("Systolic (e.g. 120)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = secondaryInput,
                            onValueChange = { secondaryInput = it },
                            label = { Text("Diastolic (e.g. 80)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = valueInput,
                        onValueChange = { valueInput = it },
                        label = { Text(if (type == "water") "Amount ($selectedUnit)" else "Value ($selectedUnit)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Quick presets for sleep or water
                if (type == "water") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("250", "350", "500", "750", "1000").forEach { preset ->
                            SuggestionChip(
                                onClick = { valueInput = preset },
                                label = { Text("$preset ml") }
                            )
                        }
                    }
                } else if (type == "sleep") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("6.0", "7.0", "7.5", "8.0", "9.0").forEach { preset ->
                            SuggestionChip(
                                onClick = { valueInput = preset },
                                label = { Text("$preset hrs") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes / Context (optional)") },
                    placeholder = {
                        Text(
                            when (type) {
                                "temperature" -> "e.g. measured at 7:00 AM, restful sleep"
                                "weight" -> "e.g. pre-breakfast, feeling slight luteal water retention"
                                "sleep" -> "e.g. deep & restful, vivid dreams"
                                "blood_pressure" -> "e.g. resting seated pulse 68 bpm"
                                else -> "e.g. daily entry"
                            },
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val primaryVal = valueInput.toDoubleOrNull()
                    if (primaryVal != null) {
                        val finalNotes = if (type == "blood_pressure" && secondaryInput.isNotBlank()) {
                            "${primaryVal.toInt()}/${secondaryInput.trim()} mmHg${if (notesInput.isNotBlank()) " • $notesInput" else ""}"
                        } else {
                            notesInput
                        }
                        onSave(primaryVal, selectedUnit, dateInput, finalNotes)
                    }
                }
            ) {
                Text("Save Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
