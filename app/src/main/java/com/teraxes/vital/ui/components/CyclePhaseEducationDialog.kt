package com.teraxes.vital.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.domain.DayPhaseType
import com.teraxes.vital.ui.theme.*

data class PhaseEducationItem(
    val phaseType: DayPhaseType,
    val title: String,
    val subtitle: String,
    val typicalTiming: String,
    val primaryColor: Color,
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val icon: ImageVector,
    val summary: String,
    val whatIsHappening: String,
    val hormones: String,
    val whatYouFeel: List<String>,
    val nutritionAndSelfCare: List<String>,
    val fertilityStatus: String,
    val whatToTrack: String
)

val CyclePhasesEducationList = listOf(
    PhaseEducationItem(
        phaseType = DayPhaseType.PERIOD_LOGGED,
        title = "Menstrual Phase (Period)",
        subtitle = "Endometrium Shedding & Cycle Reset",
        typicalTiming = "Days 1 – 5 to 7 of cycle",
        primaryColor = PhaseMenstrual,
        bgColor = PhaseMenstrualBg,
        borderColor = PhaseMenstrualBorder,
        textColor = PhaseMenstrual,
        icon = Icons.Default.WaterDrop,
        summary = "Day 1 of bleeding marks the official first day of your new cycle. The uterine lining breaks down and sheds naturally.",
        whatIsHappening = "Because no fertilized egg was implanted during the previous cycle, progesterone and estrogen plummet, prompting the uterus to shed its nutrient-rich endometrium.",
        hormones = "Estrogen and Progesterone are at their lowest baseline levels.",
        whatYouFeel = listOf(
            "Uterine cramps (from prostaglandins)",
            "Lower back ache and pelvic heaviness",
            "Fatigue, low stamina, and desire for rest",
            "Relief from premenstrual tension"
        ),
        nutritionAndSelfCare = listOf(
            "Iron-rich foods: Spinach, beans, lentils, dark greens, lean meat",
            "Warm herbal teas (ginger, peppermint, chamomile)",
            "Hydration and electrolytes to replace fluids",
            "Gentle walking, light stretching, heating pad on lower belly"
        ),
        fertilityStatus = "Very Low chance of conception (near baseline zero).",
        whatToTrack = "Flow volume (Light, Medium, Heavy), pad/tampon count, cramp severity, headache, back pain."
    ),
    PhaseEducationItem(
        phaseType = DayPhaseType.FOLLICULAR,
        title = "Follicular Phase",
        subtitle = "Follicle Maturation & Rising Estrogen",
        typicalTiming = "Days 6 – 12 (overlaps with period from Day 1)",
        primaryColor = PhaseFollicular,
        bgColor = PhaseFollicularBg,
        borderColor = PhaseFollicularBorder,
        textColor = PhaseFollicularText,
        icon = Icons.Default.Spa,
        summary = "Your brain signals the ovaries to mature egg follicles. Rising estrogen rebuilds the uterine wall and revitalizes your body.",
        whatIsHappening = "The pituitary gland releases FSH (Follicle-Stimulating Hormone), encouraging multiple follicles to grow in the ovary. One dominant follicle will prepare to release an egg. Estrogen thickens the uterine lining again.",
        hormones = "FSH increases; Estrogen climbs steadily and peaks right before ovulation.",
        whatYouFeel = listOf(
            "Surge in natural energy, stamina, and mental sharpness",
            "Elevated mood, optimism, and social desire",
            "Clearer, glowing skin",
            "Increased muscle recovery and workout capacity"
        ),
        nutritionAndSelfCare = listOf(
            "Lean proteins, colorful vegetables, and fermented foods (kimchi, kefir)",
            "Complex carbohydrates (quinoa, oats, sweet potatoes)",
            "Higher-intensity workouts, HIIT, cardio, and weight training",
            "Ideal time for creative projects, scheduling meetings, and new habits"
        ),
        fertilityStatus = "Low to Medium. Increases as you get closer to the fertile window.",
        whatToTrack = "Energy levels, transition from dry to creamy cervical fluid, mood, workout performance."
    ),
    PhaseEducationItem(
        phaseType = DayPhaseType.FERTILE,
        title = "Fertile Window",
        subtitle = "Sperm Viability & Approaching Ovulation",
        typicalTiming = "Approx. 5 days prior to ovulation through ovulation day (~Days 10 – 15)",
        primaryColor = PhaseFertile,
        bgColor = PhaseFertileBg,
        borderColor = PhaseFertileBorder,
        textColor = PhaseFertileText,
        icon = Icons.Default.Eco,
        summary = "The ~6 days in your cycle when intercourse can result in pregnancy. Sperm can survive up to 5 days in fertile cervical fluid.",
        whatIsHappening = "As estrogen climbs toward its peak, the cervix softens and produces fertile cervical fluid (nourishing and guiding sperm into the fallopian tubes).",
        hormones = "High Estrogen; LH begins rising rapidly to trigger egg release.",
        whatYouFeel = listOf(
            "Noticeably increased libido and sex drive",
            "Cervical fluid becomes slippery, wet, or creamy/watery",
            "Heightened senses, confidence, and social charisma",
            "Mild bloating or mild pelvic awareness"
        ),
        nutritionAndSelfCare = listOf(
            "Antioxidant-dense berries, zinc-rich pumpkin seeds, citrus",
            "Stay well-hydrated to support healthy cervical fluid consistency",
            "If trying to conceive (TTC), schedule intercourse every 1–2 days",
            "If avoiding pregnancy, strictly use barrier protection or abstain"
        ),
        fertilityStatus = "HIGH FERTILITY — Significant probability of conception.",
        whatToTrack = "Cervical mucus consistency (creamy, watery, slippery), libido changes, LH test strip readings."
    ),
    PhaseEducationItem(
        phaseType = DayPhaseType.OVULATION,
        title = "Ovulation Day",
        subtitle = "Peak Fertility & Mature Egg Release",
        typicalTiming = "Mid-cycle (~Day 14 in a 28-day cycle, 12-24 hours lifespan)",
        primaryColor = PhaseOvulation,
        bgColor = PhaseOvulationBg,
        borderColor = PhaseOvulationBorder,
        textColor = PhaseOvulationText,
        icon = Icons.Default.Star,
        summary = "The ovary releases a mature egg into the fallopian tube. This is the single day of peak fertility in the entire monthly cycle.",
        whatIsHappening = "A surge of LH (Luteinizing Hormone) triggers the dominant ovarian follicle to rupture and release an egg. The egg survives for approximately 12 to 24 hours unless fertilized.",
        hormones = "LH and Estrogen peak; Progesterone begins its rise immediately after.",
        whatYouFeel = listOf(
            "Peak sex drive and magnetism",
            "Stretchy, transparent, raw egg-white cervical fluid",
            "Mittelschmerz (mild localized pinch or ache on one side of lower abdomen)",
            "Slight breast sensitivity or increased sense of smell"
        ),
        nutritionAndSelfCare = listOf(
            "Anti-inflammatory foods: leafy greens, turmeric, walnuts, salmon",
            "Hydration with coconut water or electrolyte-rich fluids",
            "Peak window for conception intercourse if attempting pregnancy",
            "High energy for public speaking, social gatherings, or intense workouts"
        ),
        fertilityStatus = "PEAK FERTILITY — Maximum chance of egg fertilization.",
        whatToTrack = "Egg-white cervical mucus, LH surge on test kit, one-sided ovulation twinges, basal temperature dip before surge."
    ),
    PhaseEducationItem(
        phaseType = DayPhaseType.LUTEAL,
        title = "Luteal Phase",
        subtitle = "Progesterone Dominance & PMS / Wind-Down",
        typicalTiming = "Days 15 – 28 (from ovulation until the next period)",
        primaryColor = PhaseLuteal,
        bgColor = PhaseLutealBg,
        borderColor = PhaseLutealBorder,
        textColor = PhaseLutealText,
        icon = Icons.Default.Bedtime,
        summary = "Progesterone dominates to sustain the uterine lining. Your body temperature rises and prepares for either pregnancy or the next period.",
        whatIsHappening = "The ruptured follicle transforms into the corpus luteum, pumping out progesterone. If no embryo implants, the corpus luteum shrinks, progesterone drops, signaling the next period.",
        hormones = "Progesterone peaks mid-luteal and drops sharply right before menstruation.",
        whatYouFeel = listOf(
            "Calm, nesting focus in early luteal phase",
            "Basal Body Temperature rises by ~0.3°C – 0.5°C",
            "Late luteal PMS: bloating, tender breasts, irritability, cravings",
            "Gradual decline in physical stamina and increased sleep needs"
        ),
        nutritionAndSelfCare = listOf(
            "Magnesium-rich foods: dark chocolate (70%+), pumpkin seeds, almonds, bananas",
            "Healthy fats: Avocado, olive oil, nuts to support hormone synthesis",
            "Warm herbal teas (chamomile, spearmint for bloating, dandelion)",
            "Switch to low-impact workouts: Pilates, yoga, swimming, steady walks",
            "Prioritize 8+ hours of restful sleep and reduce late caffeine"
        ),
        fertilityStatus = "Low (Egg has dissolved; virtually zero after 24–48 hours post-ovulation).",
        whatToTrack = "PMS symptoms, mood swings, breast tenderness, bloating, food cravings, BBT temperature spike."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyclePhaseEducationDialog(
    initialPhase: DayPhaseType? = null,
    onDismiss: () -> Unit
) {
    var selectedPhaseType by remember {
        mutableStateOf(
            if (initialPhase == DayPhaseType.PERIOD_PREDICTED) DayPhaseType.PERIOD_LOGGED
            else initialPhase ?: DayPhaseType.PERIOD_LOGGED
        )
    }

    val selectedItem = CyclePhasesEducationList.firstOrNull { it.phaseType == selectedPhaseType }
        ?: CyclePhasesEducationList.first()

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("cycle_phase_education_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Header
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Cycle Phase Guide",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Biology, Hormones & Daily Body Signs",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Phase Selection Chips Row
                ScrollableTabRow(
                    selectedTabIndex = CyclePhasesEducationList.indexOf(selectedItem).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {},
                    containerColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CyclePhasesEducationList.forEach { phase ->
                        val isSelected = phase.phaseType == selectedPhaseType
                        Tab(
                            selected = isSelected,
                            onClick = { selectedPhaseType = phase.phaseType },
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) phase.primaryColor else phase.bgColor,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) phase.primaryColor else phase.borderColor
                                ),
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = phase.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else phase.textColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = phase.title.split(" ").first(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else phase.textColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detailed Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Main Phase Card Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = selectedItem.bgColor,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, selectedItem.borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                        color = selectedItem.primaryColor,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = selectedItem.icon,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = selectedItem.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = selectedItem.textColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = selectedItem.primaryColor
                                ) {
                                    Text(
                                        text = selectedItem.typicalTiming,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedItem.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = selectedItem.textColor,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Biological Mechanism
                    EducationSectionCard(
                        title = "What Happens in Your Body",
                        icon = Icons.Default.Biotech,
                        tint = selectedItem.primaryColor
                    ) {
                        Text(
                            text = selectedItem.whatIsHappening,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = selectedItem.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Hormones: ${selectedItem.hormones}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = selectedItem.textColor
                            )
                        }
                    }

                    // What You Feel (Physical & Emotional)
                    EducationSectionCard(
                        title = "Physical & Emotional Signs",
                        icon = Icons.Default.Psychology,
                        tint = selectedItem.primaryColor
                    ) {
                        selectedItem.whatYouFeel.forEach { item ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 5.dp)
                                        .size(5.dp)
                                        .background(selectedItem.primaryColor, CircleShape)
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Self-Care & Nutrition Tips
                    EducationSectionCard(
                        title = "Nutrition & Lifestyle Tips",
                        icon = Icons.Default.Favorite,
                        tint = selectedItem.primaryColor
                    ) {
                        selectedItem.nutritionAndSelfCare.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 5.dp)
                                        .size(5.dp)
                                        .background(selectedItem.primaryColor, CircleShape)
                                )
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Fertility Status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChildCare,
                                contentDescription = null,
                                tint = selectedItem.primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Conception Probability",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedItem.fertilityStatus,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = selectedItem.primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EducationSectionCard(
    title: String,
    icon: ImageVector,
    tint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
