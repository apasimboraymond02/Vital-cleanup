package com.teraxes.vital.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.domain.CycleUtils
import com.teraxes.vital.domain.DayPhaseType
import com.teraxes.vital.ui.components.ClinicalSummaryReportDialog
import com.teraxes.vital.ui.components.CorrectPredictionDialog
import com.teraxes.vital.ui.components.CyclePhaseEducationDialog
import com.teraxes.vital.ui.components.CycleSyncingGuidanceCard
import com.teraxes.vital.ui.components.DailyReportCheckInCard
import com.teraxes.vital.ui.components.HomeScreenGlanceCard
import com.teraxes.vital.ui.components.InteractiveCycleWheel
import com.teraxes.vital.ui.components.InteractiveHormoneWaveVisualizer
import com.teraxes.vital.ui.theme.*
import com.teraxes.vital.ui.viewmodel.VitalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: VitalViewModel,
    onNavigateToCycle: () -> Unit,
    onNavigateToPregnancy: () -> Unit,
    onOpenAccountSetup: () -> Unit = {}
) {
    val stats by viewModel.cycleStats.collectAsState()
    val predictions by viewModel.cyclePredictions.collectAsState()
    val insights by viewModel.correlationInsights.collectAsState()
    val activePregnancy by viewModel.activePregnancy.collectAsState()
    val symptoms by viewModel.symptoms.collectAsState()
    val cycles by viewModel.cycles.collectAsState()
    val healthMetrics by viewModel.healthMetrics.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val isAccountCreated by viewModel.isAccountCreated.collectAsState()
    val avgCycleLength by viewModel.avgCycleLength.collectAsState()
    val isBlindPredictionEnabled by viewModel.isBlindPredictionEnabled.collectAsState()
    val isDiscreetMode by viewModel.isDiscreetMode.collectAsState()
    val userPin by viewModel.userPin.collectAsState()
    val lastCheckInDate by viewModel.lastCheckInDate.collectAsState()

    val todaySdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val todayDateStr = remember { todaySdf.format(java.util.Date()) }
    val isCheckedInToday = lastCheckInDate == todayDateStr

    var showQuickLogDialog by remember { mutableStateOf(false) }
    var showCorrectPredictionDialog by remember { mutableStateOf(false) }
    var showPhaseEducationDialog by remember { mutableStateOf(false) }
    var showDoctorReportDialog by remember { mutableStateOf(false) }
    var educationInitialPhase by remember { mutableStateOf<DayPhaseType?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userName.isBlank()) "Hello, Visitor 👋" else "Hello, $userName 👋",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isDiscreetMode) "Discreet Privacy Mode is active" else if (!isAccountCreated) "Viewing sample baseline preview" else "Here is your health summary for today",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (userPin.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.lockApp() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock Vault",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Vital Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Home Screen & Interactive Glance Widget View
        item {
            HomeScreenGlanceCard(
                cycleStats = stats,
                isDiscreetMode = isDiscreetMode,
                onQuickCheckIn = { showQuickLogDialog = true }
            )
        }

        // Sample Baseline Preview Notice Banner
        if (!isAccountCreated) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sample_preview_notice_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Preview Mode — Sample Baseline Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = "Vital has initialized a starting reference baseline (sample cycle entry with a 28-day cycle average and 5-day flow duration) so you can immediately explore estimated cycle insights, predictions, and fertility windows. Create your account when you are ready to track your own data!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = onOpenAccountSetup,
                            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preview_create_account_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Account & Customize Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Pregnancy Banner (If applicable)
        if (activePregnancy != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_pregnancy_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChildCare,
                                contentDescription = "Pregnancy",
                                tint = RosePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Pregnancy Mode Active",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = RoseTertiary
                                )
                                Text(
                                    text = "Week ${activePregnancy?.currentWeek ?: 1} • Tap to view dashboard",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Button(
                            onClick = onNavigateToPregnancy,
                            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("View", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Daily Check-Up Prediction Booster Card
        item {
            DailyCheckInPredictionBoosterCard(
                isCheckedInToday = isCheckedInToday,
                onQuickCheckIn = { showQuickLogDialog = true }
            )
        }

        // Daily Report & Check-In (What to expect, pregnancy risk from sex today, daily mood question)
        item {
            DailyReportCheckInCard(
                viewModel = viewModel,
                cycleStats = stats
            )
        }

        // Interactive Animated Cycle Wheel
        item {
            InteractiveCycleWheel(
                cycleStats = stats,
                onOpenPhaseEducation = { phase ->
                    educationInitialPhase = phase
                    showPhaseEducationDialog = true
                }
            )
        }

        // Dynamic Hormone Wave Matrix Visualizer
        item {
            InteractiveHormoneWaveVisualizer(
                cycleStats = stats
            )
        }

        // Phase-Based Lifestyle, Nutrition & Workout Cycle Syncing Guidance
        item {
            CycleSyncingGuidanceCard(
                cycleStats = stats
            )
        }

        // Cycle Status Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cycle_status_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val phase = stats.currentPhase.lowercase()
                    val phaseVisual = when (phase) {
                        "menstrual" -> DashboardPhaseVisual(PhaseMenstrual, PhaseMenstrualBg, PhaseMenstrualBorder, Icons.Default.WaterDrop, DayPhaseType.PERIOD_LOGGED)
                        "follicular" -> DashboardPhaseVisual(PhaseFollicularText, PhaseFollicularBg, PhaseFollicularBorder, Icons.Default.Spa, DayPhaseType.FOLLICULAR)
                        "ovulation" -> DashboardPhaseVisual(PhaseOvulationText, PhaseOvulationBg, PhaseOvulationBorder, Icons.Default.Star, DayPhaseType.OVULATION)
                        else -> DashboardPhaseVisual(PhaseLutealText, PhaseLutealBg, PhaseLutealBorder, Icons.Default.Bedtime, DayPhaseType.LUTEAL)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = phaseVisual.bg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, phaseVisual.border),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    educationInitialPhase = phaseVisual.type
                                    showPhaseEducationDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = phaseVisual.icon,
                                    contentDescription = null,
                                    tint = phaseVisual.color,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = phase.replaceFirstChar { it.uppercase() } + " Phase",
                                    color = phaseVisual.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Learn",
                                    tint = phaseVisual.color.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        if (isBlindPredictionEnabled) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Blind AI Engine",
                                        color = Color(0xFF1B5E20),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Day ${stats.currentDay}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "of your ${stats.averageLength}-day cycle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Next Period", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = predictions?.nextPeriodDate?.let { CycleUtils.formatRelativeDate(it) } ?: "N/A",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp),
                            color = Color.LightGray
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Fertility Chance", fontSize = 12.sp, color = Color.Gray)
                            val risk = CycleUtils.calculateFertilityRisk(stats.currentDay, stats.averageLength)
                            val riskColor = when (risk) {
                                "high" -> HighRiskText
                                "medium" -> MedRiskText
                                else -> LowRiskText
                            }
                            Text(
                                text = risk.replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.Bold,
                                color = riskColor,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showQuickLogDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_log_period_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Period")
                        }

                        OutlinedButton(
                            onClick = { showCorrectPredictionDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("correct_prediction_button")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = RosePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Correct Date", color = RosePrimary)
                        }
                    }
                }
            }
        }

        // Health Banner / Compose Gradient Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("dashboard_hero_banner"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    RosePrimary,
                                    RoseTertiary
                                )
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Track Your Rhythm",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Daily logs enhance fertility & symptom predictions",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Log Symptoms",
                    icon = Icons.Default.Sick,
                    color = Color(0xFFFFF3E0),
                    onClick = onNavigateToCycle,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Fertility Map",
                    icon = Icons.Default.CalendarMonth,
                    color = Color(0xFFE8F5E9),
                    onClick = onNavigateToCycle,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Clinical Dossier",
                    icon = Icons.Default.MedicalServices,
                    color = Color(0xFFE1F5FE),
                    onClick = { showDoctorReportDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // AI Correlation Insights
        item {
            Text(
                text = "Health Insights & Patterns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (insights.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "Log symptoms over consecutive days to unlock pattern correlations.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(insights) { insight ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insight_item_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Insight",
                            tint = RosePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = insight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Recent Symptom Logs Preview
        item {
            Text(
                text = "Recent Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (symptoms.isEmpty()) {
            item {
                Text(
                    text = "No recent symptoms logged today.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        } else {
            items(symptoms.take(3)) { symptom ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(symptom.symptomType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Severity: ${symptom.severity}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(symptom.date, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    // Quick Log Dialog
    if (showQuickLogDialog) {
        QuickLogPeriodDialog(
            onDismiss = { showQuickLogDialog = false },
            onConfirm = { startDate, endDate, flow ->
                viewModel.logCycle(
                    startDate = startDate,
                    endDate = endDate,
                    flowIntensity = flow,
                    mood = "Happy",
                    symptoms = emptyList(),
                    notes = "Logged from Dashboard"
                )
                showQuickLogDialog = false
            }
        )
    }

    // Correct Prediction Dialog
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

    if (showDoctorReportDialog) {
        ClinicalSummaryReportDialog(
            userName = userName,
            cycles = cycles,
            symptoms = symptoms,
            healthMetrics = healthMetrics,
            onDismiss = { showDoctorReportDialog = false }
        )
    }
}

private data class DashboardPhaseVisual(
    val color: Color,
    val bg: Color,
    val border: Color,
    val icon: ImageVector,
    val type: DayPhaseType
)

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "quick_action_scale"
    )

    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier
            .height(84.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = RoseTertiary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RoseTertiary)
        }
    }
}

@Composable
fun QuickLogPeriodDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, Long?, String) -> Unit
) {
    var selectedFlow by remember { mutableStateOf("Medium") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Log Period") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Flow Intensity:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Light", "Medium", "Heavy").forEach { flow ->
                        FilterChip(
                            selected = selectedFlow == flow,
                            onClick = { selectedFlow = flow },
                            label = { Text(flow) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    onConfirm(now, null, selectedFlow)
                }
            ) {
                Text("Start Period Today")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DailyCheckInPredictionBoosterCard(
    isCheckedInToday: Boolean,
    onQuickCheckIn: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_checkin_prediction_booster_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCheckedInToday) Color(0xFFF1F8E9) else RosePrimaryContainer.copy(alpha = 0.55f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCheckedInToday) Color(0xFF81C784) else RosePrimary.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCheckedInToday) Color(0xFF2E7D32).copy(alpha = 0.15f) else RosePrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCheckedInToday) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isCheckedInToday) Color(0xFF2E7D32) else RosePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Help Us Predict Better",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCheckedInToday) Color(0xFF2E7D32) else RoseTertiary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCheckedInToday) Color(0xFF2E7D32).copy(alpha = 0.15f) else RosePrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isCheckedInToday) "Engine Calibrated ✓" else "Daily Check-In Tip",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCheckedInToday) Color(0xFF2E7D32) else RosePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Please usually fill your daily check-up so Vital can predict your cycle phases, ovulation, and symptoms much better. Each daily log of your flow, mood, and sensations trains our predictive model to understand your unique biology.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!isCheckedInToday) {
                Button(
                    onClick = onQuickCheckIn,
                    colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fill Today's Check-Up Now", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

