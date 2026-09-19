package com.teraxes.vital.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.domain.CycleStats
import com.teraxes.vital.domain.CycleUtils
import com.teraxes.vital.domain.DayPhaseType
import com.teraxes.vital.ui.theme.*
import com.teraxes.vital.ui.viewmodel.VitalViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

// MARK: - Main Daily Report & Check-In Container
@Composable
fun DailyReportCheckInCard(
    viewModel: VitalViewModel,
    cycleStats: CycleStats,
    modifier: Modifier = Modifier
) {
    val todaySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDateStr = remember { todaySdf.format(Date()) }

    val lastCheckInDate by viewModel.lastCheckInDate.collectAsState()
    val isCheckedInToday = lastCheckInDate == todayDateStr

    val reportInfo = remember(cycleStats.currentDay, cycleStats.averageLength, cycleStats.currentPhase) {
        CycleUtils.getDailyReport(
            currentDay = cycleStats.currentDay,
            avgLength = cycleStats.averageLength,
            currentPhase = cycleStats.currentPhase
        )
    }

    var isCheckInExpanded by remember { mutableStateOf(!isCheckedInToday) }
    var selectedFlow by remember { mutableStateOf(if (cycleStats.currentPhase.equals("menstrual", ignoreCase = true)) "Medium" else "None") }
    var selectedSpotting by remember { mutableStateOf("None") }
    var selectedPain by remember { mutableStateOf("Optimal (No Pain)") }
    var selectedMood by remember { mutableStateOf("Happy & Radiant 😊") }
    var selectedEnergy by remember { mutableStateOf("Peak Energy (100% ⚡)") }
    val selectedSymptoms = remember { mutableStateListOf<String>() }
    var userNotes by remember { mutableStateOf("") }
    var showReportDetails by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_report_check_in_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. TODAY'S DAILY REPORT & PREDICTIONS
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header: Today's Date & Phase Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TODAY'S DAILY REPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RosePrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = reportInfo.dateFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = getPhaseBadgeColor(reportInfo.phaseType)
                    ) {
                        Text(
                            text = "Day ${reportInfo.cycleDay} • ${reportInfo.phaseName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pregnancy Risk & Sex Prediction Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pregnancy_risk_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (reportInfo.sexPregnancyRiskLevel.contains("Peak") || reportInfo.sexPregnancyRiskLevel.contains("High")) {
                            RosePrimaryContainer.copy(alpha = 0.85f)
                        } else if (reportInfo.sexPregnancyRiskLevel.contains("Low") || reportInfo.sexPregnancyRiskLevel.contains("Extremely")) {
                            Color(0xFFE8F5E9)
                        } else {
                            Color(0xFFFFF8E1)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (reportInfo.sexPregnancyRiskLevel.contains("Peak") || reportInfo.sexPregnancyRiskLevel.contains("High")) {
                                    Icons.Default.WarningAmber
                                } else if (reportInfo.sexPregnancyRiskLevel.contains("Low") || reportInfo.sexPregnancyRiskLevel.contains("Extremely")) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = if (reportInfo.sexPregnancyRiskLevel.contains("Peak") || reportInfo.sexPregnancyRiskLevel.contains("High")) {
                                    RosePrimary
                                } else if (reportInfo.sexPregnancyRiskLevel.contains("Low") || reportInfo.sexPregnancyRiskLevel.contains("Extremely")) {
                                    Color(0xFF2E7D32)
                                } else {
                                    Color(0xFFF57F17)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Can sex today lead to pregnancy?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = reportInfo.sexPregnancyAnswer,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (reportInfo.sexPregnancyRiskLevel.contains("Peak") || reportInfo.sexPregnancyRiskLevel.contains("High")) {
                                RosePrimary
                            } else if (reportInfo.sexPregnancyRiskLevel.contains("Low") || reportInfo.sexPregnancyRiskLevel.contains("Extremely")) {
                                Color(0xFF1B5E20)
                            } else {
                                Color(0xFFE65100)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = reportInfo.sexPregnancyExplanation,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "💡 ${reportInfo.contraceptionTip}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // What to Expect Section
                Text(
                    text = "What to Expect Today",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Hormones & Body summary pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Spa, null, tint = RosePrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Body & Energy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reportInfo.whatToExpectBody,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (showReportDetails) Int.MAX_VALUE else 3
                            )
                        }
                    }

                    // Mood & Mind summary pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, null, tint = RoseTertiary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mood & Drive", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reportInfo.whatToExpectMood,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (showReportDetails) Int.MAX_VALUE else 3
                            )
                        }
                    }
                }

                // Expandable Full Daily Guidance
                AnimatedVisibility(visible = showReportDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🔬 Hormonal Balance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(reportInfo.whatToExpectHormones, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🥗 Nourishment Recommendation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(reportInfo.recommendedNutrition, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🏃 Movement & Workout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(reportInfo.recommendedMovement, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Expand/Collapse Details Button
                TextButton(
                    onClick = { showReportDetails = !showReportDetails },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = if (showReportDetails) "Show Less" else "View Full Day Insights",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RosePrimary
                    )
                    Icon(
                        imageVector = if (showReportDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = RosePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // =========================================================================
        // 2. DAILY CHECK-IN WITH ANIMATED QUESTION & ANSWER DIAGRAMS
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_check_in_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header with animation indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(RosePrimaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = RosePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Daily Health Check-In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Log flow, symptoms, feeling & energy",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isCheckedInToday) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.clickable { isCheckInExpanded = !isCheckInExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isCheckInExpanded) "Editing" else "Logged Today",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                // Predictive Accuracy Tip Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RosePrimaryContainer.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RosePrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RosePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = RosePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Help Vital Predict Better",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseTertiary
                            )
                            Text(
                                text = "Usually filling your daily check-up is the best way to help us predict your cycles accurately. Logging your daily symptoms, mood, and flow trains our algorithms to recognize your body's personal rhythm.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!isCheckedInToday || isCheckInExpanded) {
                    Spacer(modifier = Modifier.height(18.dp))

                    // -------------------------------------------------------------
                    // QUESTION 1: PERIOD / MENSTRUAL FLOW (with Animated Diagram)
                    // -------------------------------------------------------------
                    CheckInQuestionHeader(
                        title = "1. Menstrual Flow",
                        subtitle = "Select today's bleeding volume",
                        diagramContent = { FlowQuestionAnimatedDiagram() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val flowOptions = listOf(
                        FlowOption("None", "No Flow", 0, "No bleeding today"),
                        FlowOption("Light", "Light Flow", 1, "Pantyliner / Spotting"),
                        FlowOption("Medium", "Medium Flow", 2, "Regular pad/tampon (3-4h)"),
                        FlowOption("Heavy", "Heavy Flow", 3, "Soaking pad (1-2h)"),
                        FlowOption("Very Heavy", "Very Heavy", 4, "Clotting / Super plus")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        flowOptions.forEach { opt ->
                            val isSelected = selectedFlow == opt.id
                            Surface(
                                onClick = { selectedFlow = opt.id },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) RosePrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RosePrimary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Animated Droplet Meter Diagram
                                        FlowAnswerDropletDiagram(dropsCount = opt.dropsCount, isSelected = isSelected)
                                        Column {
                                            Text(
                                                text = opt.label,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) RosePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = opt.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedFlow = opt.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = RosePrimary)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // -------------------------------------------------------------
                    // QUESTION 2: SPOTTING & DISCHARGE COLOR (with Animated Diagram)
                    // -------------------------------------------------------------
                    CheckInQuestionHeader(
                        title = "2. Spotting & Fluid Color",
                        subtitle = "Color shade and fluid texture",
                        diagramContent = { SpottingQuestionAnimatedDiagram() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val spottingOptions = listOf(
                        SpottingOption("None", "None", "Clean / No spotting", Color(0xFFE0E0E0)),
                        SpottingOption("Light Pink", "Light Pink", "Early/late spotting or implantation", Color(0xFFF48FB1)),
                        SpottingOption("Bright Red", "Bright Red", "Fresh active blood flow", Color(0xFFE53935)),
                        SpottingOption("Dark Brown", "Dark Brown", "Oxidized old blood", Color(0xFF5D4037)),
                        SpottingOption("Egg White", "Egg White / Stretchy", "High fertility cervical mucus", Color(0xFF80DEEA)),
                        SpottingOption("Creamy White", "Creamy White", "Luteal phase thick mucus", Color(0xFFFFF59D))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        spottingOptions.chunked(2).forEach { rowList ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowList.forEach { opt ->
                                    val isSelected = selectedSpotting == opt.id
                                    Surface(
                                        onClick = { selectedSpotting = opt.id },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) RosePrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RosePrimary) else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SpottingAnswerSwatchDiagram(swatchColor = opt.color, isSelected = isSelected)
                                            Column {
                                                Text(
                                                    text = opt.label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) RosePrimary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = opt.subtext,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // -------------------------------------------------------------
                    // QUESTION 3: PAIN & CRAMP INTENSITY (with Animated Wave Diagram)
                    // -------------------------------------------------------------
                    CheckInQuestionHeader(
                        title = "3. Pain & Cramps Level",
                        subtitle = "Physiological discomfort scale (0 to 10)",
                        diagramContent = { PainQuestionAnimatedDiagram() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val painOptions = listOf(
                        PainOption("Optimal (No Pain)", "0 - Optimal (No Pain)", 0, "Comfortable and pain-free", Color(0xFF4CAF50)),
                        PainOption("Mild / Tolerable", "1-3 - Mild / Tolerable", 1, "Noticeable dull ache", Color(0xFFFFB300)),
                        PainOption("Moderate Cramps", "4-6 - Moderate Cramps", 2, "Pelvic spasms, heating pad helps", Color(0xFFFB8C00)),
                        PainOption("Severe / Distracting", "7-8 - Severe / Distracting", 3, "Interferes with focus and tasks", Color(0xFFE53935)),
                        PainOption("Debilitating", "9-10 - Debilitating", 4, "Requires bedrest / medication", Color(0xFFB71C1C))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        painOptions.forEach { opt ->
                            val isSelected = selectedPain == opt.id
                            Surface(
                                onClick = { selectedPain = opt.id },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) RosePrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RosePrimary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PainAnswerWaveDiagram(level = opt.level, waveColor = opt.indicatorColor, isSelected = isSelected)
                                        Column {
                                            Text(
                                                text = opt.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) RosePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = opt.description,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPain = opt.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = RosePrimary)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // -------------------------------------------------------------
                    // QUESTION 4: FEELING & MOOD (with Animated Aura Diagram)
                    // -------------------------------------------------------------
                    CheckInQuestionHeader(
                        title = "4. Mood & Emotional State",
                        subtitle = "How do you feel mentally today?",
                        diagramContent = { MoodQuestionAnimatedDiagram() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val moodOptions = listOf(
                        MoodOption("Happy & Radiant 😊", "Happy & Radiant 😊", "Positive & vibrant", Color(0xFFFFD54F)),
                        MoodOption("Calm & Balanced 😌", "Calm & Balanced 😌", "Peaceful & centered", Color(0xFF81C784)),
                        MoodOption("Sensitive / Vulnerable 🥺", "Sensitive 🥺", "Tender & emotional", Color(0xFF64B5F6)),
                        MoodOption("Irritable / Agitated 😤", "Irritable 😤", "Easily triggered or tense", Color(0xFFFF8A65)),
                        MoodOption("Anxious / Overwhelmed 😰", "Anxious 😰", "Restless mind or worry", Color(0xFFBA68C8)),
                        MoodOption("Depleted / Exhausted 😴", "Depleted 😴", "Low emotional bandwidth", Color(0xFF90A4AE))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        moodOptions.chunked(2).forEach { rowList ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowList.forEach { opt ->
                                    val isSelected = selectedMood == opt.id
                                    Surface(
                                        onClick = { selectedMood = opt.id },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) RosePrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RosePrimary) else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            MoodAnswerAuraDiagram(moodColor = opt.accentColor, isSelected = isSelected)
                                            Column {
                                                Text(
                                                    text = opt.label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) RosePrimary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = opt.desc,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // -------------------------------------------------------------
                    // QUESTION 5: ENERGY LEVEL (with Animated Battery Diagram)
                    // -------------------------------------------------------------
                    CheckInQuestionHeader(
                        title = "5. Energy Level & Physical Drive",
                        subtitle = "Overall stamina and vitality",
                        diagramContent = { EnergyQuestionAnimatedDiagram() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val energyOptions = listOf(
                        EnergyOption("Peak Energy (100% ⚡)", "Peak Energy (100% ⚡)", 100, "Ready for workouts & focus", Color(0xFF43A047)),
                        EnergyOption("Steady Energy (75% 🔋)", "Steady Energy (75% 🔋)", 75, "Good standard baseline", Color(0xFF00ACC1)),
                        EnergyOption("Moderate Dip (45% 🪫)", "Moderate Dip (45% 🪫)", 45, "Sluggish, craving rest", Color(0xFFFB8C00)),
                        EnergyOption("Drained / Fatigued (15% 💤)", "Drained (15% 💤)", 15, "Low battery, need sleep", Color(0xFFE53935))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        energyOptions.forEach { opt ->
                            val isSelected = selectedEnergy == opt.id
                            Surface(
                                onClick = { selectedEnergy = opt.id },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) RosePrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RosePrimary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        EnergyAnswerBatteryDiagram(percentage = opt.percentage, isSelected = isSelected)
                                        Column {
                                            Text(
                                                text = opt.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) RosePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = opt.subtext,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedEnergy = opt.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = RosePrimary)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // -------------------------------------------------------------
                    // QUESTION 6: SECONDARY BODY SYMPTOMS (Multi-Select Chips)
                    // -------------------------------------------------------------
                    Text(
                        text = "6. Physical Sensations & Secondary Symptoms:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap all that you notice today:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val quickSymptoms = listOf(
                        "Cramps", "Headache", "Tender Breasts", "Bloating",
                        "Clear Fluid", "Backache", "Acne", "Nausea", "Insomnia",
                        "Food Cravings", "Hot Flashes", "Brain Fog"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickSymptoms.chunked(3).forEach { rowList ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowList.forEach { sym ->
                                    val isSelected = selectedSymptoms.contains(sym)
                                    Surface(
                                        onClick = {
                                            if (isSelected) selectedSymptoms.remove(sym)
                                            else selectedSymptoms.add(sym)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) RosePrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, RosePrimary) else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = sym,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) RosePrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // -------------------------------------------------------------
                    // NOTES INPUT
                    // -------------------------------------------------------------
                    OutlinedTextField(
                        value = userNotes,
                        onValueChange = { userNotes = it },
                        placeholder = { Text("Personal note (e.g. took ibuprofen, felt energetic after lunch)", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkin_notes_input"),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // -------------------------------------------------------------
                    // LIVE CHECK-IN RADAR/SUMMARY PREVIEW
                    // -------------------------------------------------------------
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "CHECK-IN SUMMARY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RosePrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Flow: $selectedFlow • Pain: $selectedPain",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Mood: $selectedMood • Energy: $selectedEnergy",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SUBMIT BUTTON
                    Button(
                        onClick = {
                            viewModel.logDailyCheckIn(
                                date = todayDateStr,
                                mood = selectedMood,
                                energyLevel = selectedEnergy,
                                symptoms = selectedSymptoms.toList(),
                                notes = userNotes,
                                periodFlow = selectedFlow,
                                spotting = selectedSpotting,
                                painLevel = selectedPain
                            )
                            isCheckInExpanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_daily_checkin_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCheckedInToday) "Update Today's Check-In" else "Save Daily Check-In",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // COLLAPSED STATE AFTER LOGGING
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Today's Logged Vitals:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RosePrimary
                                    )
                                    Text(
                                        text = "🩸 Flow: $selectedFlow • 💧 Spotting: $selectedSpotting",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "⚡ Pain: $selectedPain • 🔋 Energy: $selectedEnergy",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "😊 Mood: $selectedMood",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (selectedSymptoms.isNotEmpty()) {
                                        Text(
                                            text = "🏷️ Sensations: ${selectedSymptoms.joinToString(", ")}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TextButton(onClick = { isCheckInExpanded = true }) {
                                    Text("Edit", fontSize = 12.sp, color = RosePrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Prediction Calibrated: Daily check-in logged! Thank you for helping us predict better.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// QUESTION HEADER WRAPPER (With Animated Diagram)
// =========================================================================
@Composable
private fun CheckInQuestionHeader(
    title: String,
    subtitle: String,
    diagramContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp, 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            diagramContent()
        }
    }
}

// =========================================================================
// QUESTION ANIMATED DIAGRAMS
// =========================================================================

/** 1. Menstrual Flow Question Animated Diagram */
@Composable
fun FlowQuestionAnimatedDiagram() {
    val infiniteTransition = rememberInfiniteTransition(label = "flow_question")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow_wave"
    )

    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        val w = size.width
        val h = size.height
        val wavePath = Path()
        wavePath.moveTo(0f, h * 0.5f)

        val steps = 20
        for (i in 0..steps) {
            val x = (w / steps) * i
            val y = (h * 0.5f) + sin((i.toFloat() / steps) * 2f * Math.PI.toFloat() + waveOffset) * (h * 0.25f)
            wavePath.lineTo(x, y)
        }
        wavePath.lineTo(w, h)
        wavePath.lineTo(0f, h)
        wavePath.close()

        drawPath(wavePath, brush = Brush.verticalGradient(listOf(RosePrimary, Color(0xFFB71C1C))))

        // Floating dynamic droplet
        val dropY = (h * 0.35f) + sin(waveOffset) * 2f
        drawCircle(
            color = RosePrimary,
            radius = 3.dp.toPx(),
            center = Offset(w * 0.5f, dropY)
        )
    }
}

/** 2. Spotting Question Animated Diagram */
@Composable
fun SpottingQuestionAnimatedDiagram() {
    val infiniteTransition = rememberInfiniteTransition(label = "spotting_question")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotting_pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        val w = size.width
        val h = size.height
        // Gradient color swatch transition
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF48FB1).copy(alpha = pulseAlpha), Color(0xFFE53935), Color(0xFF5D4037)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.45f
            ),
            radius = w * 0.4f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}

/** 3. Pain Scale Question Animated Diagram */
@Composable
fun PainQuestionAnimatedDiagram() {
    val infiniteTransition = rememberInfiniteTransition(label = "pain_question")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pain_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
        val w = size.width
        val h = size.height
        val path = Path()
        path.moveTo(0f, h * 0.5f)

        val steps = 24
        for (i in 0..steps) {
            val x = (w / steps) * i
            // Spasm ECG wave pulse
            val amp = if (i in 8..16) (h * 0.45f) else (h * 0.15f)
            val y = (h * 0.5f) + sin((i.toFloat() / steps) * 4f * Math.PI.toFloat() + phase) * amp
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFFE53935),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/** 4. Mood Question Animated Diagram */
@Composable
fun MoodQuestionAnimatedDiagram() {
    val infiniteTransition = rememberInfiniteTransition(label = "mood_question")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mood_aura"
    )

    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.5f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(RosePrimary.copy(alpha = 0.7f), RoseTertiary.copy(alpha = 0.2f), Color.Transparent),
                center = center,
                radius = (w * 0.45f) * scale
            ),
            radius = (w * 0.45f) * scale,
            center = center
        )

        drawCircle(
            color = RosePrimary,
            radius = 3.dp.toPx(),
            center = center
        )
    }
}

/** 5. Energy Question Animated Diagram */
@Composable
fun EnergyQuestionAnimatedDiagram() {
    val infiniteTransition = rememberInfiniteTransition(label = "energy_question")
    val batteryFill by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "battery_fill"
    )
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        val w = size.width
        val h = size.height
        // Battery outline
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(0f, h * 0.2f),
            size = Size(w * 0.85f, h * 0.6f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Battery cap
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(w * 0.88f, h * 0.35f),
            size = Size(w * 0.1f, h * 0.3f),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
        // Fill level
        val fillWidth = (w * 0.8f) * batteryFill
        drawRoundRect(
            color = if (batteryFill > 0.6f) Color(0xFF43A047) else if (batteryFill > 0.3f) Color(0xFFFB8C00) else Color(0xFFE53935),
            topLeft = Offset(w * 0.05f, h * 0.28f),
            size = Size(fillWidth, h * 0.44f),
            cornerRadius = CornerRadius(1.5.dp.toPx())
        )
    }
}

// =========================================================================
// ANSWER OPTION ANIMATED MICRO-DIAGRAMS
// =========================================================================

/** Menstrual Flow Droplets Diagram (0 to 4 animated droplets) */
@Composable
fun FlowAnswerDropletDiagram(dropsCount: Int, isSelected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "flow_droplet")
    val bounce by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drop_bounce"
    )

    Box(
        modifier = Modifier
            .size(36.dp, 26.dp)
            .background(
                if (isSelected) RosePrimary.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val w = size.width
            val h = size.height

            if (dropsCount == 0) {
                // Hollow outline droplet
                drawCircle(
                    color = Color.Gray,
                    radius = 4.dp.toPx(),
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else {
                val dropSpacing = w / (dropsCount + 1)
                for (i in 1..dropsCount) {
                    val cx = dropSpacing * i
                    val cy = (h * 0.5f) + if (isSelected) (bounce * (if (i % 2 == 0) 1f else -1f)) else 0f
                    val dropColor = when (dropsCount) {
                        1 -> Color(0xFFF48FB1)
                        2 -> Color(0xFFE57373)
                        3 -> RosePrimary
                        else -> Color(0xFFB71C1C)
                    }

                    drawCircle(
                        color = dropColor,
                        radius = (3.5f + (dropsCount * 0.3f)).dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}

/** Spotting / Fluid Color Swatch Diagram */
@Composable
fun SpottingAnswerSwatchDiagram(swatchColor: Color, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) RosePrimary else Color.LightGray.copy(alpha = 0.6f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = if (swatchColor == Color(0xFFFFF59D) || swatchColor == Color(0xFFE0E0E0)) Color.Black else Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** Pain Scale Wave Diagram */
@Composable
fun PainAnswerWaveDiagram(level: Int, waveColor: Color, isSelected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pain_wave_anim")
    val shift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pain_wave_shift"
    )

    Canvas(modifier = Modifier.size(34.dp, 20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path()
        path.moveTo(0f, h * 0.5f)

        val steps = 16
        val amp = when (level) {
            0 -> 1f
            1 -> 3.dp.toPx()
            2 -> 6.dp.toPx()
            3 -> 8.dp.toPx()
            else -> 10.dp.toPx()
        }

        for (i in 0..steps) {
            val x = (w / steps) * i
            val y = (h * 0.5f) + sin((i.toFloat() / steps) * (level + 1) * Math.PI.toFloat() + if (isSelected) shift else 0f) * amp
            path.lineTo(x, y.coerceIn(2f, h - 2f))
        }

        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(width = (if (isSelected) 2.5.dp else 1.8.dp).toPx(), cap = StrokeCap.Round)
        )
    }
}

/** Mood Aura Diagram */
@Composable
fun MoodAnswerAuraDiagram(moodColor: Color, isSelected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mood_ring_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mood_pulse"
    )

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = (size.width * 0.45f) * if (isSelected) pulse else 1f
            drawCircle(
                color = moodColor.copy(alpha = if (isSelected) 0.85f else 0.45f),
                radius = r,
                center = Offset(size.width * 0.5f, size.height * 0.5f)
            )
            drawCircle(
                color = moodColor,
                radius = 3.dp.toPx(),
                center = Offset(size.width * 0.5f, size.height * 0.5f)
            )
        }
    }
}

/** Energy Battery Cell Diagram */
@Composable
fun EnergyAnswerBatteryDiagram(percentage: Int, isSelected: Boolean) {
    val casingColor = if (isSelected) RosePrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = Modifier.size(32.dp, 18.dp)) {
        val w = size.width
        val h = size.height

        // Outer battery casing
        drawRoundRect(
            color = casingColor,
            topLeft = Offset(0f, 0f),
            size = Size(w * 0.85f, h),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Battery cap
        drawRoundRect(
            color = casingColor,
            topLeft = Offset(w * 0.88f, h * 0.25f),
            size = Size(w * 0.1f, h * 0.5f),
            cornerRadius = CornerRadius(1.dp.toPx())
        )

        // Fill blocks based on percentage
        val fillRatio = percentage / 100f
        val fillWidth = (w * 0.75f) * fillRatio
        val fillColor = when {
            percentage >= 75 -> Color(0xFF43A047)
            percentage >= 45 -> Color(0xFFFB8C00)
            else -> Color(0xFFE53935)
        }

        drawRoundRect(
            color = fillColor,
            topLeft = Offset(w * 0.05f, h * 0.15f),
            size = Size(fillWidth, h * 0.7f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

// Data models for the option pills
data class FlowOption(val id: String, val label: String, val dropsCount: Int, val description: String)
data class SpottingOption(val id: String, val label: String, val subtext: String, val color: Color)
data class PainOption(val id: String, val label: String, val level: Int, val description: String, val indicatorColor: Color)
data class MoodOption(val id: String, val label: String, val desc: String, val accentColor: Color)
data class EnergyOption(val id: String, val label: String, val percentage: Int, val subtext: String, val barColor: Color = Color.Unspecified)

private fun getPhaseBadgeColor(phaseType: DayPhaseType): Color {
    return when (phaseType) {
        DayPhaseType.PERIOD_LOGGED -> PhaseMenstrual
        DayPhaseType.PERIOD_PREDICTED -> PhasePredicted
        DayPhaseType.OVULATION -> PhaseOvulation
        DayPhaseType.FERTILE -> PhaseFertile
        DayPhaseType.FOLLICULAR -> PhaseFollicularText
        DayPhaseType.LUTEAL -> PhaseLutealText
        DayPhaseType.NONE -> Color.Gray
    }
}
