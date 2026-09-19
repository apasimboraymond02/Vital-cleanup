package com.teraxes.vital.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.domain.CycleUtils
import com.teraxes.vital.ui.theme.RosePrimary
import com.teraxes.vital.ui.theme.RosePrimaryContainer
import com.teraxes.vital.ui.theme.RoseTertiary
import com.teraxes.vital.ui.viewmodel.VitalViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class PregnancyCalculationMethod {
    CONCEPTION_DATE,
    LMP_DATE,
    DUE_DATE
}

@Composable
fun PregnancyScreen(
    viewModel: VitalViewModel
) {
    val activePregnancy by viewModel.activePregnancy.collectAsState()
    var showStartPregnancyDialog by remember { mutableStateOf(false) }
    var showExitConfirmationDialog by remember { mutableStateOf(false) }

    if (activePregnancy == null) {
        // Not active - Display informative onboarding and positive test notice
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_pregnancy_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RosePrimaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChildCare,
                                    contentDescription = "Pregnancy",
                                    tint = RosePrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Pregnancy Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Track your pregnancy week-by-week, monitor baby size comparisons, and stay on top of critical prenatal milestones.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Positive Pregnancy Test Notice Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("positive_test_notice_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Positive Test Required",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Positive Test Confirmation Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please enable Pregnancy Mode ONLY if you have received a positive pregnancy test result.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enabling this mode pauses your standard cycle predictions and customizes your daily insights specifically for your gestational timeline.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Key Setup Information Info Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Information Needed for Accurate Tracking:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = RoseTertiary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        InfoBulletItem(
                            icon = Icons.Default.Favorite,
                            title = "Likely Conception Date or Last Period (LMP)",
                            desc = "Knowing the day you likely conceived or your LMP helps us pinpoint your gestational age and due date."
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoBulletItem(
                            icon = Icons.Default.MedicalServices,
                            title = "Doctor or Ultrasound Due Date",
                            desc = "If you've already visited a healthcare provider, you can directly enter your official estimated due date."
                        )
                    }
                }
            }

            // Setup Trigger Button
            item {
                Button(
                    onClick = { showStartPregnancyDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("enable_pregnancy_mode_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ChildCare, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Up Pregnancy Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    } else {
        // Active Pregnancy View
        val preg = activePregnancy!!
        val milestones by viewModel.getPregnancyMilestones(preg.id).collectAsState(initial = emptyList())

        val dayMs = 86400000L
        val diffMs = (System.currentTimeMillis() - preg.lastPeriodDate).coerceAtLeast(0)
        val totalDays = (diffMs / dayMs).toInt()
        val currentWeek = ((totalDays / 7) + 1).coerceIn(1, 42)
        val dayInWeek = totalDays % 7

        val trimester = when (currentWeek) {
            in 1..12 -> "1st Trimester"
            in 13..27 -> "2nd Trimester"
            else -> "3rd Trimester"
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pregnancy Hero Progress Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pregnancy_progress_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RosePrimaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RosePrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = trimester.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "WEEK $currentWeek, DAY $dayInWeek",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = RoseTertiary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Estimated Due Date: ${CycleUtils.formatDate(preg.dueDate)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RoseTertiary.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Baby size comparison card
                        val babySize = getBabySizeForWeek(currentWeek)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RosePrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Eco,
                                            contentDescription = "Baby Size",
                                            tint = RosePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Baby Size Comparison", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = "Your baby is the size of a $babySize",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Key Dates & Tracking Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tracking Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RoseTertiary
                            )
                            if (preg.isConfirmedByDoctor) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Doctor Confirmed",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val conceptionEstMs = preg.lastPeriodDate + (14 * dayMs)
                        TrackingDetailRow(label = "Likely Conception Date", value = CycleUtils.formatDate(conceptionEstMs))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.LightGray.copy(alpha = 0.4f))
                        TrackingDetailRow(label = "First Day of Last Period (LMP)", value = CycleUtils.formatDate(preg.lastPeriodDate))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.LightGray.copy(alpha = 0.4f))
                        TrackingDetailRow(label = "Estimated Due Date", value = CycleUtils.formatDate(preg.dueDate))

                        if (preg.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Notes:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(preg.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Milestones Checklist Header
            item {
                Text(
                    text = "Medical Milestones & Scans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(milestones) { milestone ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("milestone_item_card"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = milestone.isCompleted,
                            onCheckedChange = { completed ->
                                viewModel.toggleMilestone(milestone.id, completed)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Week ${milestone.week}: ${milestone.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = milestone.description,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Disable / End Pregnancy Mode Button
            item {
                OutlinedButton(
                    onClick = { showExitConfirmationDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exit Pregnancy Mode")
                }
            }
        }
    }

    // Setup Dialog with Positive Test Check & Important Info Questions
    if (showStartPregnancyDialog) {
        StartPregnancySetupDialog(
            onDismiss = { showStartPregnancyDialog = false },
            onConfirm = { lmpDate, dueDate, isConfirmed, notes ->
                viewModel.startPregnancy(lmpDate, dueDate, isConfirmed, notes)
                showStartPregnancyDialog = false
            }
        )
    }

    // Exit Confirmation Dialog
    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            title = { Text("Exit Pregnancy Mode?") },
            text = { Text("Are you sure you want to turn off Pregnancy Mode? This will resume normal menstrual cycle tracking.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.endPregnancy()
                        showExitConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Exit Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoBulletItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = RosePrimary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun TrackingDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StartPregnancySetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (lmpDate: Long, dueDate: Long, isConfirmed: Boolean, notes: String) -> Unit
) {
    var hasPositiveTest by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(PregnancyCalculationMethod.CONCEPTION_DATE) }

    // Offset in weeks relative to today for selected date
    var selectedWeeksAgo by remember { mutableIntStateOf(4) } // Default 4 weeks ago
    var isConfirmedByDoctor by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val dayMs = 86400000L
    val now = System.currentTimeMillis()

    // Calculate dates based on chosen method
    val (calculatedLmp, calculatedConception, calculatedDueDate) = remember(selectedMethod, selectedWeeksAgo) {
        val chosenDateMs = now - (selectedWeeksAgo * 7 * dayMs)
        when (selectedMethod) {
            PregnancyCalculationMethod.CONCEPTION_DATE -> {
                val conception = chosenDateMs
                val lmp = conception - (14 * dayMs)
                val due = conception + (266 * dayMs)
                Triple(lmp, conception, due)
            }
            PregnancyCalculationMethod.LMP_DATE -> {
                val lmp = chosenDateMs
                val conception = lmp + (14 * dayMs)
                val due = lmp + (280 * dayMs)
                Triple(lmp, conception, due)
            }
            PregnancyCalculationMethod.DUE_DATE -> {
                // If user selected due date weeks in future
                val due = now + (selectedWeeksAgo * 7 * dayMs)
                val lmp = due - (280 * dayMs)
                val conception = lmp + (14 * dayMs)
                Triple(lmp, conception, due)
            }
        }
    }

    val totalDays = ((now - calculatedLmp) / dayMs).toInt().coerceAtLeast(0)
    val calculatedWeek = ((totalDays / 7) + 1).coerceIn(1, 42)
    val calculatedDayInWeek = totalDays % 7

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChildCare, contentDescription = null, tint = RosePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pregnancy Setup Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mandatory Positive Test Checkbox
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasPositiveTest) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hasPositiveTest = !hasPositiveTest }
                        ) {
                            Checkbox(
                                checked = hasPositiveTest,
                                onCheckedChange = { hasPositiveTest = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "I confirm I have tested positive for pregnancy.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasPositiveTest) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!hasPositiveTest) {
                            Text(
                                text = "⚠️ Enable this mode only after confirming a positive test result.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                            )
                        }
                    }
                }

                // Question 1: Calculation Method
                Text(
                    text = "1. How would you like to calculate your timeline?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RoseTertiary
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MethodOptionRow(
                        title = "Likely Conception Date",
                        subtitle = "Day you likely conceived (ovulation/intercourse)",
                        selected = selectedMethod == PregnancyCalculationMethod.CONCEPTION_DATE,
                        onClick = { selectedMethod = PregnancyCalculationMethod.CONCEPTION_DATE }
                    )

                    MethodOptionRow(
                        title = "First Day of Last Period (LMP)",
                        subtitle = "Standard medical baseline date",
                        selected = selectedMethod == PregnancyCalculationMethod.LMP_DATE,
                        onClick = { selectedMethod = PregnancyCalculationMethod.LMP_DATE }
                    )

                    MethodOptionRow(
                        title = "Estimated Due Date (EDD)",
                        subtitle = "Provided by doctor or ultrasound scan",
                        selected = selectedMethod == PregnancyCalculationMethod.DUE_DATE,
                        onClick = { selectedMethod = PregnancyCalculationMethod.DUE_DATE }
                    )
                }

                // Question 2: Select Date Offset
                Text(
                    text = when (selectedMethod) {
                        PregnancyCalculationMethod.CONCEPTION_DATE -> "2. When was your likely conception date?"
                        PregnancyCalculationMethod.LMP_DATE -> "2. When was the first day of your last period?"
                        PregnancyCalculationMethod.DUE_DATE -> "2. When is your estimated due date?"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RoseTertiary
                )

                // Easy preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = if (selectedMethod == PregnancyCalculationMethod.DUE_DATE) {
                        listOf(30 to "30 Wks", 32 to "32 Wks", 34 to "34 Wks", 36 to "36 Wks")
                    } else {
                        listOf(2 to "2 Wks Ago", 4 to "4 Wks Ago", 6 to "6 Wks Ago", 8 to "8 Wks Ago")
                    }

                    presets.forEach { (weeks, label) ->
                        FilterChip(
                            selected = selectedWeeksAgo == weeks,
                            onClick = { selectedWeeksAgo = weeks },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Fine-tune offset controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Adjust timeline:", fontSize = 12.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (selectedWeeksAgo > 1) selectedWeeksAgo -= 1 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                        }
                        Text(
                            text = "$selectedWeeksAgo Wks",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { if (selectedWeeksAgo < 40) selectedWeeksAgo += 1 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                        }
                    }
                }

                // Live Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RosePrimaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Calculated Timeline Preview:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoseTertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Gestational Age: Week $calculatedWeek, Day $calculatedDayInWeek",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseTertiary
                        )
                        Text(
                            text = "• Likely Conception: ${CycleUtils.formatDate(calculatedConception)}",
                            fontSize = 11.sp,
                            color = RoseTertiary.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "• Estimated Due Date: ${CycleUtils.formatDate(calculatedDueDate)}",
                            fontSize = 11.sp,
                            color = RoseTertiary.copy(alpha = 0.9f)
                        )
                    }
                }

                // Question 3: Doctor Confirmation & Notes
                Text(
                    text = "3. Medical Confirmation (Optional):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RoseTertiary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isConfirmedByDoctor = !isConfirmedByDoctor }
                ) {
                    Checkbox(
                        checked = isConfirmedByDoctor,
                        onCheckedChange = { isConfirmedByDoctor = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirmed by healthcare provider / ultrasound", fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes or Clinic Info") },
                    placeholder = { Text("e.g., Dr. Smith's clinic, first scan notes") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasPositiveTest) {
                        onConfirm(calculatedLmp, calculatedDueDate, isConfirmedByDoctor, notes)
                    }
                },
                enabled = hasPositiveTest,
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
            ) {
                Text("Enable Mode")
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
private fun MethodOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) RosePrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, RosePrimary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

fun getBabySizeForWeek(week: Int): String {
    return when (week) {
        in 1..4 -> "Poppy Seed"
        in 5..7 -> "Blueberry"
        in 8..11 -> "Raspberry"
        in 12..15 -> "Lime"
        in 16..19 -> "Avocado"
        in 20..23 -> "Banana"
        in 24..27 -> "Eggplant"
        in 28..31 -> "Coconut"
        in 32..35 -> "Pineapple"
        else -> "Watermelon"
    }
}
