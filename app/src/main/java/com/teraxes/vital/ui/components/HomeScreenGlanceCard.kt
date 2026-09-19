package com.teraxes.vital.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.domain.CycleStats
import com.teraxes.vital.ui.theme.*

/**
 * High-craft Android Home Screen & Glanceable Summary Card
 * Replicating an interactive Android Glance / AppWidget directly on the dashboard
 * with quick-switch privacy and period countdown.
 */
@Composable
fun HomeScreenGlanceCard(
    cycleStats: CycleStats,
    isDiscreetMode: Boolean,
    onQuickCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val daysUntilNext = (cycleStats.averageLength - cycleStats.currentDay).coerceAtLeast(0)
    val phaseColor = when (cycleStats.currentPhase.lowercase()) {
        "menstrual" -> PhaseMenstrual
        "follicular" -> PhaseFollicularBorder
        "ovulation" -> PhaseOvulationBorder
        else -> PhaseLutealBorder
    }

    val fertilityStatus = when (cycleStats.currentPhase.lowercase()) {
        "ovulation" -> "Peak"
        "follicular" -> "Elevated"
        else -> "Low"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_screen_glance_card")
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDiscreetMode) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Widget Title & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(phaseColor, CircleShape)
                    )
                    Text(
                        text = if (isDiscreetMode) "WELLNESS GLANCE" else "CYCLE & VITAL STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDiscreetMode) Color(0xFFE2E8F0) else RosePrimaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (isDiscreetMode) "Discreet Mode" else "Widget View",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDiscreetMode) Color(0xFF475569) else RoseTertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Central Glance Metric
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (isDiscreetMode) {
                        Text(
                            text = "Next Log in $daysUntilNext Days",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily Wellness Progress: Day ${cycleStats.currentDay}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = if (daysUntilNext <= 0) "Period Expected Today" else "$daysUntilNext Days to Period",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cycle Day ${cycleStats.currentDay} • ${cycleStats.currentPhase} Phase",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = phaseColor
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onQuickCheckIn,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Fertility Status & Energy
            if (!isDiscreetMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlanceMetricPill(
                        label = "Fertility Status",
                        value = fertilityStatus,
                        color = if (fertilityStatus == "Peak" || fertilityStatus == "Elevated") HighRiskText else LowRiskText,
                        modifier = Modifier.weight(1f)
                    )
                    GlanceMetricPill(
                        label = "Avg Cycle",
                        value = "${cycleStats.averageLength} days",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlanceMetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(text = label, fontSize = 10.sp, color = Color.Gray)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
