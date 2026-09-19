package com.teraxes.vital.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.domain.CycleStats
import com.teraxes.vital.domain.CycleUtils
import com.teraxes.vital.domain.DayPhaseType
import com.teraxes.vital.ui.theme.*
import kotlin.math.*

/**
 * High-craft, highly interactive cycle wheel with touch/drag scrubbing,
 * animated orbital particles, glowing phase arcs, and real-time biological telemetry.
 */
@Composable
fun InteractiveCycleWheel(
    cycleStats: CycleStats,
    onOpenPhaseEducation: (DayPhaseType) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDays = cycleStats.averageLength.coerceIn(21, 40)
    var selectedDay by remember(cycleStats.currentDay, cycleStats.averageLength) {
        mutableIntStateOf(cycleStats.currentDay.coerceIn(1, totalDays))
    }

    var isDragging by remember { mutableStateOf(false) }

    // Pulsing glow animation for the active day indicator
    val infiniteTransition = rememberInfiniteTransition(label = "wheel_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Animated day angle for smooth spring response
    val targetAngle = remember(selectedDay, totalDays) {
        (selectedDay - 1).toFloat() / totalDays * 360f - 90f
    }
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "wheel_needle_angle"
    )

    // Evaluate phase and risk for the selected day
    val selectedPhase = remember(selectedDay, totalDays) {
        CycleUtils.getCyclePhase(selectedDay, totalDays)
    }
    val selectedRisk = remember(selectedDay, totalDays) {
        CycleUtils.calculateFertilityRisk(selectedDay, totalDays)
    }
    val phaseType = when (selectedPhase.lowercase()) {
        "menstrual" -> DayPhaseType.PERIOD_LOGGED
        "follicular" -> DayPhaseType.FOLLICULAR
        "ovulation" -> DayPhaseType.OVULATION
        else -> DayPhaseType.LUTEAL
    }

    val ovulationDay = (totalDays - 14).coerceAtLeast(10)
    val fertileStartDay = (ovulationDay - 5).coerceAtLeast(1)
    val fertileEndDay = (ovulationDay + 2).coerceAtMost(totalDays)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_cycle_wheel_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Interactive Hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = RosePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "INTERACTIVE CYCLE DIAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RosePrimary,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = getPhaseBadgeColor(phaseType).copy(alpha = 0.15f),
                    modifier = Modifier.clickable { onOpenPhaseEducation(phaseType) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedPhase.replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = getPhaseTextColor(phaseType)
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Phase info",
                            tint = getPhaseTextColor(phaseType),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Interactive Circular Canvas
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .pointerInput(totalDays) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false }
                        ) { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVec = change.position - center
                            var angleDeg = Math.toDegrees(atan2(touchVec.y.toDouble(), touchVec.x.toDouble())).toFloat()
                            angleDeg = (angleDeg + 90f + 360f) % 360f
                            val day = (angleDeg / 360f * totalDays).toInt() + 1
                            selectedDay = day.coerceIn(1, totalDays)
                        }
                    }
                    .pointerInput(totalDays) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVec = offset - center
                            var angleDeg = Math.toDegrees(atan2(touchVec.y.toDouble(), touchVec.x.toDouble())).toFloat()
                            angleDeg = (angleDeg + 90f + 360f) % 360f
                            val day = (angleDeg / 360f * totalDays).toInt() + 1
                            selectedDay = day.coerceIn(1, totalDays)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasSize = size.minDimension
                    val strokeWidth = 24.dp.toPx()
                    val radius = (canvasSize - strokeWidth) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // 1. Draw subtle background track
                    drawCircle(
                        color = Color(0xFFF0EBEF),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    // 2. Draw Phase Arcs
                    // Menstrual: Days 1..5
                    val menstrualSweep = (5f / totalDays) * 360f
                    drawArc(
                        color = Color(0xFFFF5277),
                        startAngle = -90f,
                        sweepAngle = menstrualSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Follicular: Days 6..(ovulationDay - 1)
                    val follicularStartAngle = -90f + menstrualSweep
                    val follicularDays = (ovulationDay - 1) - 5
                    val follicularSweep = (follicularDays.toFloat() / totalDays) * 360f
                    drawArc(
                        color = Color(0xFFFFB300),
                        startAngle = follicularStartAngle,
                        sweepAngle = follicularSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth)
                    )

                    // Ovulation Window: Days (ovulationDay - 2)..(ovulationDay + 1)
                    val ovulationStartAngle = -90f + ((ovulationDay - 3f) / totalDays * 360f)
                    val ovulationSweep = (4f / totalDays) * 360f
                    drawArc(
                        color = Color(0xFF00BFA5),
                        startAngle = ovulationStartAngle,
                        sweepAngle = ovulationSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth + 4.dp.toPx())
                    )

                    // Luteal Phase: Days (ovulationDay + 2)..totalDays
                    val lutealStartAngle = -90f + ((ovulationDay + 1f) / totalDays * 360f)
                    val lutealDays = totalDays - (ovulationDay + 1)
                    val lutealSweep = (lutealDays.toFloat() / totalDays) * 360f
                    drawArc(
                        color = Color(0xFF9C27B0),
                        startAngle = lutealStartAngle,
                        sweepAngle = lutealSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth)
                    )

                    // 3. Draw tick marks along the wheel for each day
                    for (i in 1..totalDays) {
                        val tickAngleRad = Math.toRadians(((i - 1f) / totalDays * 360f - 90f).toDouble())
                        val innerR = radius - strokeWidth / 2f + 2.dp.toPx()
                        val outerR = radius + strokeWidth / 2f - 2.dp.toPx()
                        val startPoint = Offset(
                            (center.x + innerR * cos(tickAngleRad)).toFloat(),
                            (center.y + innerR * sin(tickAngleRad)).toFloat()
                        )
                        val endPoint = Offset(
                            (center.x + outerR * cos(tickAngleRad)).toFloat(),
                            (center.y + outerR * sin(tickAngleRad)).toFloat()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = startPoint,
                            end = endPoint,
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // 4. Draw Animated Pointer Indicator for the Selected Day
                    val currentRad = Math.toRadians(animatedAngle.toDouble())
                    val indicatorCenter = Offset(
                        (center.x + radius * cos(currentRad)).toFloat(),
                        (center.y + radius * sin(currentRad)).toFloat()
                    )

                    // Glowing outer aura
                    drawCircle(
                        color = RosePrimary.copy(alpha = glowAlpha * 0.45f),
                        radius = (16.dp * pulseScale).toPx(),
                        center = indicatorCenter
                    )
                    // Indicator Core
                    drawCircle(
                        color = Color.White,
                        radius = 9.dp.toPx(),
                        center = indicatorCenter
                    )
                    drawCircle(
                        color = RosePrimary,
                        radius = 6.dp.toPx(),
                        center = indicatorCenter
                    )
                }

                // Center Display Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = if (selectedDay == cycleStats.currentDay) "TODAY" else "INSPECTING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedDay == cycleStats.currentDay) RosePrimary else Color.Gray,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Day $selectedDay",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "of $totalDays days",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val riskColor = when (selectedRisk) {
                        "high" -> HighRiskText
                        "medium" -> MedRiskText
                        else -> LowRiskText
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = riskColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$selectedRisk chance".uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = riskColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "💡 Drag your finger around the dial or tap any day to scrub through your cycle",
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Shortcut Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickJumpChip(
                    label = "Today (D${cycleStats.currentDay})",
                    isSelected = selectedDay == cycleStats.currentDay,
                    onClick = { selectedDay = cycleStats.currentDay },
                    modifier = Modifier.weight(1f)
                )
                QuickJumpChip(
                    label = "Period (D1)",
                    isSelected = selectedDay == 1,
                    onClick = { selectedDay = 1 },
                    modifier = Modifier.weight(1f)
                )
                QuickJumpChip(
                    label = "Ovulation (D$ovulationDay)",
                    isSelected = selectedDay == ovulationDay,
                    onClick = { selectedDay = ovulationDay },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Biological Telemetry for Selected Day
            DynamicDayTelemetryCard(
                selectedDay = selectedDay,
                totalDays = totalDays,
                ovulationDay = ovulationDay,
                fertileStart = fertileStartDay,
                fertileEnd = fertileEndDay,
                phaseType = phaseType
            )
        }
    }
}

@Composable
private fun QuickJumpChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) RosePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DynamicDayTelemetryCard(
    selectedDay: Int,
    totalDays: Int,
    ovulationDay: Int,
    fertileStart: Int,
    fertileEnd: Int,
    phaseType: DayPhaseType
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Estimated Hormone Dynamics
            Text(
                text = "ESTIMATED HORMONE ACTIVITY (DAY $selectedDay)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            // Estrogen Bar
            val estrogenLevel = when {
                selectedDay <= 5 -> 0.20f
                selectedDay in 6..(ovulationDay - 2) -> 0.45f + (selectedDay - 5) * 0.08f
                selectedDay in (ovulationDay - 1)..ovulationDay -> 0.95f
                selectedDay in (ovulationDay + 1)..(ovulationDay + 7) -> 0.65f
                else -> 0.25f
            }.coerceIn(0.1f, 1f)

            // Progesterone Bar
            val progesteroneLevel = when {
                selectedDay <= ovulationDay -> 0.15f
                selectedDay in (ovulationDay + 1)..(ovulationDay + 7) -> 0.50f + (selectedDay - ovulationDay) * 0.06f
                selectedDay in (ovulationDay + 8)..totalDays -> 0.85f - (selectedDay - (ovulationDay + 7)) * 0.12f
                else -> 0.15f
            }.coerceIn(0.1f, 1f)

            HormoneProgressBar(
                name = "Estrogen (E2)",
                level = estrogenLevel,
                barColor = Color(0xFFFF4081),
                status = if (estrogenLevel > 0.8f) "Peak Surge" else if (estrogenLevel > 0.5f) "High" else "Baseline"
            )

            HormoneProgressBar(
                name = "Progesterone (P4)",
                level = progesteroneLevel,
                barColor = Color(0xFF7C4DFF),
                status = if (progesteroneLevel > 0.7f) "Dominant" else if (progesteroneLevel > 0.4f) "Rising" else "Low"
            )

            // Fertility Guidance for this specific day
            val guidanceText = when {
                selectedDay == ovulationDay -> "🥚 Ovulation Day — Egg release underway. Maximum statistical probability of conception."
                selectedDay in (ovulationDay - 3) until ovulationDay -> "⚡ Prime Fertile Window — High quality cervical mucus keeps sperm viable for multiple days."
                selectedDay in fertileStart until (ovulationDay - 3) -> "🌿 Opening Fertile Window — Intercourse now can lead to fertilization in 3-5 days."
                selectedDay > (ovulationDay + 1) -> "🛡️ Luteal Phase — Post-ovulatory infertile phase. Cervical mucus thickens to prevent sperm transit."
                selectedDay <= 5 -> "🩸 Menstruation — Endometrial shedding. Conception is unlikely."
                else -> "🌱 Pre-Fertile Follicular — Dominant follicle maturing in the ovary."
            }

            Text(
                text = guidanceText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun HormoneProgressBar(
    name: String,
    level: Float,
    barColor: Color,
    status: String
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "hormone_bar_anim"
    )

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = barColor)
        }

        LinearProgressIndicator(
            progress = { animatedLevel },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f)
        )
    }
}

private fun getPhaseBadgeColor(type: DayPhaseType): Color = when (type) {
    DayPhaseType.PERIOD_LOGGED, DayPhaseType.PERIOD_PREDICTED -> PhaseMenstrual
    DayPhaseType.FOLLICULAR -> PhaseFollicularBorder
    DayPhaseType.OVULATION -> PhaseOvulationBorder
    DayPhaseType.LUTEAL -> PhaseLutealBorder
    else -> Color.Gray
}

private fun getPhaseTextColor(type: DayPhaseType): Color = when (type) {
    DayPhaseType.PERIOD_LOGGED, DayPhaseType.PERIOD_PREDICTED -> PhaseMenstrual
    DayPhaseType.FOLLICULAR -> PhaseFollicularText
    DayPhaseType.OVULATION -> PhaseOvulationText
    DayPhaseType.LUTEAL -> PhaseLutealText
    else -> Color.Gray
}
