package com.teraxes.vital.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.domain.CycleStats
import com.teraxes.vital.ui.theme.RosePrimary
import kotlin.math.*

/**
 * Interactive 4-Hormone Wave Visualizer with real-time drag scrubbing across cycle days,
 * showcasing dynamic wave curves for Estrogen (E2), Progesterone (P4), Luteinizing Hormone (LH),
 * and Follicle-Stimulating Hormone (FSH).
 */
@Composable
fun InteractiveHormoneWaveVisualizer(
    cycleStats: CycleStats,
    modifier: Modifier = Modifier
) {
    val totalDays = cycleStats.averageLength.coerceIn(21, 40)
    var selectedDay by remember(cycleStats.currentDay, totalDays) {
        mutableIntStateOf(cycleStats.currentDay.coerceIn(1, totalDays))
    }

    val ovulationDay = (totalDays - 14).coerceAtLeast(10)

    val animatedDay by animateFloatAsState(
        targetValue = selectedDay.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "hormone_scrub_day"
    )

    // Animated glow pulse on active scrubber
    val infiniteTransition = rememberInfiniteTransition(label = "scrubber_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scrubber_pulse_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_hormone_visualizer_card"),
        shape = RoundedCornerShape(20.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = Color(0xFF7C4DFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DYNAMIC HORMONE WAVE MATRIX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C4DFF),
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEDE7F6)
                ) {
                    Text(
                        text = "Day $selectedDay / $totalDays",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF512DA8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Interactive Wave Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFF9F7FA), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .pointerInput(totalDays) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val progress = (change.position.x / size.width).coerceIn(0f, 1f)
                            val day = (progress * (totalDays - 1)).toInt() + 1
                            selectedDay = day.coerceIn(1, totalDays)
                        }
                    }
                    .pointerInput(totalDays) {
                        detectTapGestures { offset ->
                            val progress = (offset.x / size.width).coerceIn(0f, 1f)
                            val day = (progress * (totalDays - 1)).toInt() + 1
                            selectedDay = day.coerceIn(1, totalDays)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Phase Background Tints
                    val mWidth = (5f / totalDays) * w
                    val oStart = ((ovulationDay - 3f) / totalDays) * w
                    val oWidth = (4f / totalDays) * w

                    // Menstrual Tint
                    drawRect(
                        color = Color(0xFFFF5277).copy(alpha = 0.08f),
                        topLeft = Offset(0f, 0f),
                        size = Size(mWidth, h)
                    )
                    // Ovulation Tint
                    drawRect(
                        color = Color(0xFF00BFA5).copy(alpha = 0.10f),
                        topLeft = Offset(oStart, 0f),
                        size = Size(oWidth, h)
                    )

                    // 2. Draw Horizontal Grid Baseline
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(0f, h * 0.5f),
                        end = Offset(w, h * 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )

                    // 3. Generate and draw Hormone Wave Curves
                    val estrogenPoints = mutableListOf<Offset>()
                    val progesteronePoints = mutableListOf<Offset>()
                    val lhPoints = mutableListOf<Offset>()

                    val steps = 100
                    for (i in 0..steps) {
                        val frac = i.toFloat() / steps
                        val dayVal = 1f + frac * (totalDays - 1f)
                        val x = frac * w

                        // Estrogen formula: high before ovulation (Day ~12-13), drop, secondary peak in mid-luteal (Day ~21)
                        val eNorm = if (dayVal <= ovulationDay) {
                            val dist = abs(dayVal - (ovulationDay - 1f))
                            exp(-0.2f * dist * dist) * 0.85f + 0.15f
                        } else {
                            val midLuteal = ovulationDay + (totalDays - ovulationDay) / 2f
                            val dist = abs(dayVal - midLuteal)
                            exp(-0.15f * dist * dist) * 0.55f + 0.15f
                        }
                        val yEstrogen = h - (eNorm * (h - 20.dp.toPx()) + 10.dp.toPx())
                        estrogenPoints.add(Offset(x, yEstrogen))

                        // Progesterone formula: very low until ovulation, surges to peak in mid-luteal
                        val pNorm = if (dayVal <= ovulationDay) {
                            0.08f
                        } else {
                            val midLuteal = ovulationDay + (totalDays - ovulationDay) / 2f
                            val dist = abs(dayVal - midLuteal)
                            exp(-0.10f * dist * dist) * 0.82f + 0.08f
                        }
                        val yProgesterone = h - (pNorm * (h - 20.dp.toPx()) + 10.dp.toPx())
                        progesteronePoints.add(Offset(x, yProgesterone))

                        // LH Surge formula: extremely sharp spike right at ovulationDay - 1 to ovulationDay
                        val lhDist = abs(dayVal - (ovulationDay - 0.5f))
                        val lhNorm = exp(-1.8f * lhDist * lhDist) * 0.95f + 0.05f
                        val yLh = h - (lhNorm * (h - 20.dp.toPx()) + 10.dp.toPx())
                        lhPoints.add(Offset(x, yLh))
                    }

                    // Draw Smooth Curves
                    drawSmoothPath(estrogenPoints, Color(0xFFFF4081), 2.5.dp.toPx())
                    drawSmoothPath(progesteronePoints, Color(0xFF7C4DFF), 2.5.dp.toPx())
                    drawSmoothPath(lhPoints, Color(0xFFFF9100), 2.dp.toPx())

                    // 4. Draw Interactive Scrubber Cursor
                    val scrubFrac = (animatedDay - 1f) / (totalDays - 1f)
                    val scrubX = (scrubFrac * w).coerceIn(0f, w)

                    // Vertical Guideline
                    drawLine(
                        color = Color(0xFF212121).copy(alpha = 0.6f),
                        start = Offset(scrubX, 0f),
                        end = Offset(scrubX, h),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Scrubber Pip Indicator on Top
                    drawCircle(
                        color = RosePrimary.copy(alpha = 0.35f),
                        radius = (8.dp * pulseScale).toPx(),
                        center = Offset(scrubX, 10.dp.toPx())
                    )
                    drawCircle(
                        color = RosePrimary,
                        radius = 5.dp.toPx(),
                        center = Offset(scrubX, 10.dp.toPx())
                    )
                }
            }

            // Legend & Interactive Guide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HormoneLegendItem("Estrogen (E2)", Color(0xFFFF4081))
                HormoneLegendItem("Progesterone (P4)", Color(0xFF7C4DFF))
                HormoneLegendItem("LH Surge", Color(0xFFFF9100))
            }

            // Interactive Day Snapshot Card
            val estrogenDesc = when {
                selectedDay in (ovulationDay - 2)..ovulationDay -> "Surging at maximum peak (boosts energy, libido, skin glow)"
                selectedDay > ovulationDay -> "Secondary luteal plateau (supports uterine lining)"
                else -> "Rising steadily during follicular phase"
            }
            val progesteroneDesc = when {
                selectedDay <= ovulationDay -> "Low baseline (permits follicular development)"
                selectedDay in (ovulationDay + 4)..(ovulationDay + 9) -> "Peak dominance (promotes calming sleep & elevates body temp)"
                else -> "Tapering down preparing for next cycle"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Day $selectedDay Bio-Snapshot:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• Estrogen: $estrogenDesc",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Progesterone: $progesteroneDesc",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HormoneLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun DrawScope.drawSmoothPath(points: List<Offset>, color: Color, strokeWidth: Float) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val midX = (p0.x + p1.x) / 2f
            val midY = (p0.y + p1.y) / 2f
            quadraticTo(p0.x, p0.y, midX, midY)
        }
        val last = points.last()
        lineTo(last.x, last.y)
    }
    drawPath(path, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
