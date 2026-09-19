package com.teraxes.vital.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.data.model.CycleEntity
import com.teraxes.vital.domain.CycleUtils
import com.teraxes.vital.domain.DayPhaseType
import com.teraxes.vital.ui.components.CorrectPredictionDialog
import com.teraxes.vital.ui.components.CycleCalendarView
import com.teraxes.vital.ui.components.CyclePhaseEducationDialog
import com.teraxes.vital.ui.components.CyclePhasesEducationList
import com.teraxes.vital.ui.components.InteractiveCycleWheel
import com.teraxes.vital.ui.theme.*
import com.teraxes.vital.ui.viewmodel.VitalViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(
    viewModel: VitalViewModel
) {
    val cycles by viewModel.cycles.collectAsState()
    val symptoms by viewModel.symptoms.collectAsState()
    val stats by viewModel.cycleStats.collectAsState()
    val avgCycleLength by viewModel.avgCycleLength.collectAsState()

    var showAddCycleDialog by remember { mutableStateOf(false) }
    var showAddSymptomDialog by remember { mutableStateOf(false) }
    var showCorrectPredictionDialog by remember { mutableStateOf(false) }
    var showPhaseEducationDialog by remember { mutableStateOf(false) }
    var educationInitialPhase by remember { mutableStateOf<DayPhaseType?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Calendar, 1: Cycles History, 2: Symptoms Log

    var selectedCycleStartDateMs by remember { mutableStateOf<Long?>(null) }
    var selectedSymptomDateStr by remember { mutableStateOf<String?>(null) }

    val sdfParser = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab selector
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Calendar View") },
                modifier = Modifier.testTag("tab_calendar_view")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Cycles History") },
                modifier = Modifier.testTag("tab_cycles_history")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Symptoms Log") },
                modifier = Modifier.testTag("tab_symptoms_log")
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            val accuracy = CycleUtils.getPredictionAccuracy(cycles)

            when (selectedTab) {
                0 -> {
                    // Calendar View Tab
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (accuracy.isHighAccuracy) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (accuracy.isHighAccuracy) Icons.Default.Verified else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (accuracy.isHighAccuracy) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                    Column {
                                        Text(
                                            text = accuracy.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (accuracy.isHighAccuracy) Color(0xFF1B5E20) else Color(0xFFE65100)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = accuracy.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Prediction Inaccurate?",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = RoseTertiary
                                        )
                                        Text(
                                            text = "Period came earlier or later? Correct start date to recalibrate.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { showCorrectPredictionDialog = true },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = RosePrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Correct", fontSize = 11.sp, color = RosePrimary)
                                    }
                                }
                            }
                        }

                        item {
                            CycleCalendarView(
                                cycles = cycles,
                                symptoms = symptoms,
                                onLogCycleForDate = { dateMs ->
                                    selectedCycleStartDateMs = dateMs
                                    showAddCycleDialog = true
                                },
                                onLogSymptomForDate = { dateStr ->
                                    selectedSymptomDateStr = dateStr
                                    showAddSymptomDialog = true
                                }
                            )
                        }

                        // Cycle Phase Biology Masterclass Section
                        item {
                            CyclePhaseBiologySummaryCard(
                                onOpenFullGuide = { phaseType ->
                                    educationInitialPhase = phaseType
                                    showPhaseEducationDialog = true
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // Cycles History Tab
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Summary Stats Card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cycle_stats_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Cycle Insights",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RoseTertiary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        StatItem("Avg Length", "${stats.averageLength} days")
                                        StatItem("Period", "${stats.averagePeriodLength} days")
                                        StatItem("Regularity", stats.regularity)
                                    }
                                }
                            }
                        }

                        // Interactive Cycle Dial
                        item {
                            InteractiveCycleWheel(
                                cycleStats = stats,
                                onOpenPhaseEducation = { phase ->
                                    educationInitialPhase = phase
                                    showPhaseEducationDialog = true
                                }
                            )
                        }

                        // Cycles List
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recorded Cycles (${cycles.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = {
                                        selectedCycleStartDateMs = null
                                        showAddCycleDialog = true
                                    },
                                    modifier = Modifier.testTag("log_cycle_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Log Cycle")
                                }
                            }
                        }

                        if (cycles.isEmpty()) {
                            item {
                                Text("No cycle history logged yet.", color = Color.Gray)
                            }
                        } else {
                            items(cycles, key = { it.id }) { cycle ->
                                CycleHistoryCard(
                                    cycle = cycle,
                                    onDelete = { viewModel.deleteCycle(cycle.id) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Symptoms Log Tab
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            // Educational Banner about Symptoms across phases
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Symptom Patterns by Cycle Phase",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Cramps occur in the Menstrual phase from prostaglandins. Libido surges during Ovulation. Bloating & PMS occur during Luteal phase from progesterone.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Logged Symptoms (${symptoms.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = {
                                        selectedSymptomDateStr = null
                                        showAddSymptomDialog = true
                                    },
                                    modifier = Modifier.testTag("log_symptom_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Symptom")
                                }
                            }
                        }

                        if (symptoms.isEmpty()) {
                            item {
                                Text("No symptoms recorded yet.", color = Color.Gray)
                            }
                        } else {
                            items(symptoms, key = { it.id }) { symptom ->
                                val dateObj = runCatching { sdfParser.parse(symptom.date) }.getOrNull()
                                val phaseEval = dateObj?.let { CycleUtils.evaluatePhaseForDate(it, cycles) }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = symptom.symptomType,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )

                                                // Severity Pill
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = when (symptom.severity.lowercase()) {
                                                        "high" -> HighRiskBg
                                                        "medium" -> MedRiskBg
                                                        else -> LowRiskBg
                                                    }
                                                ) {
                                                    Text(
                                                        text = symptom.severity,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (symptom.severity.lowercase()) {
                                                            "high" -> HighRiskText
                                                            "medium" -> MedRiskText
                                                            else -> LowRiskText
                                                        },
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                // Phase Badge
                                                if (phaseEval != null && phaseEval.phaseType != DayPhaseType.NONE) {
                                                    val (badgeBg, badgeText) = when (phaseEval.phaseType) {
                                                        DayPhaseType.PERIOD_LOGGED, DayPhaseType.PERIOD_PREDICTED -> PhaseMenstrualBg to PhaseMenstrual
                                                        DayPhaseType.FOLLICULAR -> PhaseFollicularBg to PhaseFollicularText
                                                        DayPhaseType.FERTILE -> PhaseFertileBg to PhaseFertileText
                                                        DayPhaseType.OVULATION -> PhaseOvulationBg to PhaseOvulationText
                                                        DayPhaseType.LUTEAL -> PhaseLutealBg to PhaseLutealText
                                                        DayPhaseType.NONE -> Color(0xFFEEEEEE) to Color.Gray
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = badgeBg,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .clickable {
                                                                educationInitialPhase = phaseEval.phaseType
                                                                showPhaseEducationDialog = true
                                                            }
                                                    ) {
                                                        Text(
                                                            text = phaseEval.phaseName.replace("Predicted ", ""),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = badgeText,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Date: ${symptom.date}" + (phaseEval?.cycleDay?.let { " • Cycle Day $it" } ?: ""),
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )

                                            if (symptom.notes.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Note: ${symptom.notes}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        IconButton(onClick = { viewModel.deleteSymptom(symptom.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCycleDialog) {
        AddCycleDialog(
            initialStartDateMs = selectedCycleStartDateMs,
            onDismiss = { showAddCycleDialog = false },
            onSave = { startDate, endDate, flow, mood, notes ->
                viewModel.logCycle(startDate, endDate, flow, mood, emptyList(), notes)
                showAddCycleDialog = false
            }
        )
    }

    if (showAddSymptomDialog) {
        AddSymptomDialog(
            initialDateStr = selectedSymptomDateStr,
            onDismiss = { showAddSymptomDialog = false },
            onSave = { date, type, severity, notes ->
                viewModel.logSymptom(date, type, severity, notes)
                showAddSymptomDialog = false
            }
        )
    }

    if (showCorrectPredictionDialog) {
        CorrectPredictionDialog(
            currentAvgCycleDays = avgCycleLength,
            onDismiss = { showCorrectPredictionDialog = false },
            onConfirmCorrection = { actualStartMs, flowIntensity, newAvgCycleLength ->
                viewModel.correctPredictionWithActualStart(
                    actualStartMs = actualStartMs,
                    flowIntensity = flowIntensity,
                    newAvgCycleDays = newAvgCycleLength
                )
                showCorrectPredictionDialog = false
            }
        )
    }

    if (showPhaseEducationDialog) {
        CyclePhaseEducationDialog(
            initialPhase = educationInitialPhase,
            onDismiss = { showPhaseEducationDialog = false }
        )
    }
}

@Composable
private fun CyclePhaseBiologySummaryCard(
    onOpenFullGuide: (DayPhaseType) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = RosePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Cycle Phase Biology & Meaning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = { onOpenFullGuide(DayPhaseType.PERIOD_LOGGED) }) {
                    Text("Deep Dive", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Each menstrual cycle is driven by 4 distinct hormonal seasons. Tap any phase to explore what is happening inside your body:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            CyclePhasesEducationList.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = item.bgColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, item.borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenFullGuide(item.phaseType) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = item.primaryColor,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = item.textColor
                                )
                                Text(
                                    text = item.typicalTiming,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = item.textColor.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.summary,
                                fontSize = 11.sp,
                                color = item.textColor.copy(alpha = 0.9f),
                                lineHeight = 15.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RosePrimary)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun CycleHistoryCard(cycle: CycleEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cycle_history_card"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = PhaseMenstrual,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${CycleUtils.formatDate(cycle.startDate)} - ${cycle.endDate?.let { CycleUtils.formatDate(it) } ?: "Ongoing"}",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Length: ${CycleUtils.getCycleLength(cycle)} days • Flow: ${cycle.flowIntensity}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Cycle", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun AddCycleDialog(
    initialStartDateMs: Long? = null,
    onDismiss: () -> Unit,
    onSave: (Long, Long?, String, String, String) -> Unit
) {
    var flow by remember { mutableStateOf("Medium") }
    var mood by remember { mutableStateOf("Neutral") }
    var notes by remember { mutableStateOf("") }
    val startDateMs = remember(initialStartDateMs) { initialStartDateMs ?: System.currentTimeMillis() }
    val dateDisplayStr = remember(startDateMs) { CycleUtils.formatDate(startDateMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log New Period / Cycle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Start Date: $dateDisplayStr",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = RoseTertiary
                )

                Text("Flow Intensity:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Light", "Medium", "Heavy").forEach {
                        FilterChip(
                            selected = flow == it,
                            onClick = { flow = it },
                            label = { Text(it) }
                        )
                    }
                }

                Text("Mood:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Happy", "Neutral", "Anxious", "Sad").forEach {
                        FilterChip(
                            selected = mood == it,
                            onClick = { mood = it },
                            label = { Text(it) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(startDateMs, null, flow, mood, notes)
                }
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddSymptomDialog(
    initialDateStr: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var symptomType by remember { mutableStateOf("Cramps") }
    var severity by remember { mutableStateOf("Medium") }
    var notes by remember { mutableStateOf("") }
    val targetDateStr = remember(initialDateStr) {
        initialDateStr ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val commonSymptoms = listOf("Cramps", "Headache", "Bloating", "Fatigue", "Mood Swings", "Acne", "Backache")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Symptom") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Date: $targetDateStr",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = RoseTertiary
                )

                Text("Symptom:")
                Column {
                    commonSymptoms.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { s ->
                                FilterChip(
                                    selected = symptomType == s,
                                    onClick = { symptomType = s },
                                    label = { Text(s, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Text("Severity:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { sev ->
                        FilterChip(
                            selected = severity == sev,
                            onClick = { severity = sev },
                            label = { Text(sev) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(targetDateStr, symptomType, severity, notes) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
