package com.teraxes.vital.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.teraxes.vital.domain.CycleStats
import com.teraxes.vital.ui.theme.*

data class PhaseGuidanceData(
    val phaseTitle: String,
    val energyLevel: String,
    val hormoneContext: String,
    val workoutPlan: List<String>,
    val nutritionPlan: List<String>,
    val mindAndSelfCare: List<String>,
    val badgeColor: Color,
    val textColor: Color
)

object CycleSyncingData {
    fun getGuidanceForPhase(phase: String): PhaseGuidanceData = when (phase.lowercase()) {
        "menstrual" -> PhaseGuidanceData(
            phaseTitle = "Menstrual Phase (Rest & Rejuvenation)",
            energyLevel = "Low & Inward Focusing",
            hormoneContext = "Estrogen & Progesterone are at baseline low. Your body is shedding the endometrial lining and requires restorative rest.",
            workoutPlan = listOf(
                "Gentle walking & slow outdoor strolls",
                "Restorative / Yin Yoga & deep hip opening",
                "Pelvic floor relaxation & mindful stretching",
                "Take full rest days without guilt"
            ),
            nutritionPlan = listOf(
                "Iron-rich foods: Lentils, spinach, red meat, organ meats",
                "Anti-inflammatory spices: Turmeric, ginger tea, cinnamon",
                "Magnesium-dense snacks: Dark chocolate (85%+), pumpkin seeds",
                "Warm bone broths and slow-cooked stews"
            ),
            mindAndSelfCare = listOf(
                "Schedule low-demand social periods and prioritize 8-9 hours of sleep",
                "Journal on monthly reflections and intention setting",
                "Use heating pads and warm Epsom salt baths for cramp relief"
            ),
            badgeColor = PhaseMenstrual,
            textColor = PhaseMenstrual
        )
        "follicular" -> PhaseGuidanceData(
            phaseTitle = "Follicular Phase (Renewal & Rising Stamina)",
            energyLevel = "Rising High & Creative",
            hormoneContext = "FSH stimulates follicle growth while Estrogen climbs steadily, boosting brain neuroplasticity, mood, and insulin sensitivity.",
            workoutPlan = listOf(
                "High-Intensity Interval Training (HIIT) & sprints",
                "Progressive strength training and lifting heavier weights",
                "Cardio dance, boxing, and fast-paced cycling",
                "Trying new sports or complex movement skills"
            ),
            nutritionPlan = listOf(
                "Fermented foods for estrogen metabolism: Kimchi, sauerkraut, kefir",
                "Lean proteins: Salmon, pasture-raised eggs, edamame",
                "Cruciferous vegetables: Broccoli sprouts, cauliflower, cabbage",
                "Complex slow-burning carbs: Quinoa, oats, sweet potatoes"
            ),
            mindAndSelfCare = listOf(
                "Plan brainstorms, launch new projects, and tackle ambitious goals",
                "Take advantage of heightened social energy and collaborative work",
                "Explore creative hobbies and learning opportunities"
            ),
            badgeColor = PhaseFollicularBorder,
            textColor = PhaseFollicularText
        )
        "ovulation" -> PhaseGuidanceData(
            phaseTitle = "Ovulation Phase (Peak Vitality & Magnetic Drive)",
            energyLevel = "Peak Maximum Stamina",
            hormoneContext = "Estrogen and Luteinizing Hormone (LH) surge to peak levels, accompanied by a slight testosterone spike for max power and libido.",
            workoutPlan = listOf(
                "Attempting personal records (PRs) in weightlifting",
                "High-impact bootcamps and sprint intervals",
                "Group fitness classes and dynamic team sports",
                "Keep dynamic warm-ups thorough to prevent ligament strains"
            ),
            nutritionPlan = listOf(
                "Antioxidant-rich berries, pomegranate, and citrus fruits",
                "Fiber-dense foods to assist liver clearance of estrogen",
                "Zinc & Selenium rich foods: Brazil nuts, oysters, sunflower seeds",
                "Stay generously hydrated with electrolytes"
            ),
            mindAndSelfCare = listOf(
                "High-impact meetings, negotiations, and public speaking",
                "Social outings, date nights, and intimate connection",
                "Channel heightened verbal fluency and charisma"
            ),
            badgeColor = PhaseOvulationBorder,
            textColor = PhaseOvulationText
        )
        else -> PhaseGuidanceData(
            phaseTitle = "Luteal Phase (Power Down & PMS Defense)",
            energyLevel = "Gradually Tapering & Focused",
            hormoneContext = "Progesterone surges to dominate the phase, raising resting basal temperature and metabolic rate while increasing GABAergic calming pathways.",
            workoutPlan = listOf(
                "Strength training with moderate weights and longer rest intervals",
                "Reformer Pilates & barre workouts",
                "Steady-state zone 2 cardio (hiking, brisk walking)",
                "Shift away from exhaustive HIIT in the final 5 days before your period"
            ),
            nutritionPlan = listOf(
                "Complex carbs to boost serotonin: Butternut squash, brown rice",
                "Vitamin B6 for progesterone synthesis: Avocados, bananas, chickpeas",
                "Magnesium glycinate & calcium-rich sesame seeds/tahini",
                "Limit excessive caffeine and refined sugar to reduce PMS irritability"
            ),
            mindAndSelfCare = listOf(
                "Wrap up open tasks, deep clean, and organize your space",
                "Protect evening downtime and decrease screen exposure before bed",
                "Set healthy boundaries and honor personal solitude"
            ),
            badgeColor = PhaseLutealBorder,
            textColor = PhaseLutealText
        )
    }
}

/**
 * Phase-Based Lifestyle, Nutrition & Workout Guidance Card
 */
@Composable
fun CycleSyncingGuidanceCard(
    cycleStats: CycleStats,
    modifier: Modifier = Modifier
) {
    var showDetailDialog by remember { mutableStateOf(false) }
    val phaseData = remember(cycleStats.currentPhase) {
        CycleSyncingData.getGuidanceForPhase(cycleStats.currentPhase)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cycle_syncing_guidance_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(phaseData.badgeColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = phaseData.textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "CYCLE SYNCING GUIDANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = phaseData.textColor,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Workout, Nutrition & Energy Sync",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = phaseData.badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.clickable { showDetailDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Full Guide",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = phaseData.textColor
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = phaseData.textColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Quick Sync Highlight Pillars (Workout, Nutrition, Energy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SyncPillarCard(
                    icon = Icons.Default.DirectionsRun,
                    title = "Optimal Movement",
                    snippet = phaseData.workoutPlan.firstOrNull() ?: "Walking & Stretching",
                    modifier = Modifier.weight(1f)
                )
                SyncPillarCard(
                    icon = Icons.Default.Restaurant,
                    title = "Target Fuel",
                    snippet = phaseData.nutritionPlan.firstOrNull()?.substringBefore(":") ?: "Nutrient rich foods",
                    modifier = Modifier.weight(1f)
                )
            }

            // Energy Bar Indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = phaseData.textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Current Energy State: ${phaseData.energyLevel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = phaseData.hormoneContext,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showDetailDialog) {
        CycleSyncingDetailDialog(
            guidanceData = phaseData,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
private fun SyncPillarCard(
    icon: ImageVector,
    title: String,
    snippet: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = RosePrimary)
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
            }
            Text(
                text = snippet,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun CycleSyncingDetailDialog(
    guidanceData: PhaseGuidanceData,
    onDismiss: () -> Unit
) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = guidanceData.phaseTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = guidanceData.textColor
                        )
                        Text(
                            text = "Biologically Synchronized Lifestyle Protocol",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Workouts Section
                    GuidanceSection(
                        icon = Icons.Default.FitnessCenter,
                        title = "RECOMMENDED WORKOUTS & MOVEMENT",
                        items = guidanceData.workoutPlan,
                        accentColor = guidanceData.textColor
                    )

                    // Nutrition Section
                    GuidanceSection(
                        icon = Icons.Default.Restaurant,
                        title = "TARGET NUTRITION & FOOD CHOICES",
                        items = guidanceData.nutritionPlan,
                        accentColor = guidanceData.textColor
                    )

                    // Mind & Recovery Section
                    GuidanceSection(
                        icon = Icons.Default.SelfImprovement,
                        title = "MINDSET, SLEEP & SOCIAL ENERGY",
                        items = guidanceData.mindAndSelfCare,
                        accentColor = guidanceData.textColor
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = guidanceData.badgeColor)
                ) {
                    Text("Got It, Close Protocol")
                }
            }
        }
    }
}

@Composable
private fun GuidanceSection(
    icon: ImageVector,
    title: String,
    items: List<String>,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 0.8.sp
            )
        }

        items.forEach { item ->
            Row(
                modifier = Modifier.padding(start = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("•", fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.Bold)
                Text(
                    text = item,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
