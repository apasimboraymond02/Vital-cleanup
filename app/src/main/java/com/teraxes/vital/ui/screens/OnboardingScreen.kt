package com.teraxes.vital.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teraxes.vital.domain.CycleUtils
import com.teraxes.vital.ui.theme.*
import com.teraxes.vital.ui.viewmodel.VitalViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class OnboardingStage {
    SLIDES,
    QUESTIONNAIRE,
    SIGN_UP
}

data class IntroSlide(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tag: String
)

data class GoalOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: VitalViewModel,
    onComplete: () -> Unit
) {
    var currentStage by remember { mutableStateOf(OnboardingStage.SLIDES) }
    val coroutineScope = rememberCoroutineScope()

    // Questionnaire Answers (empty defaults ensure new users actively choose their options)
    val selectedGoals = remember { mutableStateListOf<String>() }
    var selectedRegularity by remember { mutableStateOf("") }
    var selectedDiscomfort by remember { mutableStateOf("") }
    var selectedPrimaryMode by remember { mutableStateOf("") }
    var questionnaireErrorMessage by remember { mutableStateOf("") }

    // Sign-Up Data
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    val dayMs = 86400000L
    val now = System.currentTimeMillis()

    var selectedLmpDateMs by remember { mutableLongStateOf(now - (14 * dayMs)) }
    var hasSelectedLmpDate by remember { mutableStateOf(false) }
    var showLmpDatePicker by remember { mutableStateOf(false) }

    var avgCycleDays by remember { mutableIntStateOf(28) }
    var avgPeriodDays by remember { mutableIntStateOf(5) }
    var flowIntensity by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var includePastHistory by remember { mutableStateOf(false) }
    val pastCycleDates = remember {
        mutableStateListOf(
            selectedLmpDateMs - (28 * dayMs),
            selectedLmpDateMs - (56 * dayMs)
        )
    }
    var editingPastCycleIndex by remember { mutableIntStateOf(-1) }
    var showPastDatePicker by remember { mutableStateOf(false) }
    var signUpErrorMessage by remember { mutableStateOf("") }

    // DatePicker for LMP
    if (showLmpDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedLmpDateMs
        )
        DatePickerDialog(
            onDismissRequest = { showLmpDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val utcMillis = datePickerState.selectedDateMillis
                        if (utcMillis != null) {
                            val localMidnight = CycleUtils.utcMillisToLocalMidnight(utcMillis)
                            selectedLmpDateMs = localMidnight
                            hasSelectedLmpDate = true
                            signUpErrorMessage = ""
                            pastCycleDates.clear()
                            pastCycleDates.add(localMidnight - (avgCycleDays * dayMs))
                            pastCycleDates.add(localMidnight - (2 * avgCycleDays * dayMs))
                        }
                        showLmpDatePicker = false
                    }
                ) {
                    Text("OK", color = RosePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLmpDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // DatePicker for a past cycle
    if (showPastDatePicker && editingPastCycleIndex >= 0 && editingPastCycleIndex < pastCycleDates.size) {
        val initialMs = pastCycleDates[editingPastCycleIndex]
        val pastDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMs
        )
        DatePickerDialog(
            onDismissRequest = { showPastDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val utcMillis = pastDatePickerState.selectedDateMillis
                        if (utcMillis != null) {
                            val localMidnight = CycleUtils.utcMillisToLocalMidnight(utcMillis)
                            pastCycleDates[editingPastCycleIndex] = localMidnight
                        }
                        showPastDatePicker = false
                    }
                ) {
                    Text("OK", color = RosePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPastDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pastDatePickerState)
        }
    }

    AnimatedContent(
        targetState = currentStage,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) togetherWith
                        slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(350))
            } else {
                slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) togetherWith
                        slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350))
            }
        },
        label = "onboarding_stage_transition"
    ) { stage ->
        when (stage) {
            OnboardingStage.SLIDES -> {
                IntroSlidesSection(
                    onSkip = { currentStage = OnboardingStage.QUESTIONNAIRE },
                    onFinishSlides = { currentStage = OnboardingStage.QUESTIONNAIRE }
                )
            }
            OnboardingStage.QUESTIONNAIRE -> {
                QuestionnaireSection(
                    selectedGoals = selectedGoals,
                    onToggleGoal = { goalId ->
                        questionnaireErrorMessage = ""
                        if (selectedGoals.contains(goalId)) {
                            selectedGoals.remove(goalId)
                        } else {
                            selectedGoals.add(goalId)
                        }
                    },
                    selectedRegularity = selectedRegularity,
                    onSelectRegularity = { selectedRegularity = it; questionnaireErrorMessage = "" },
                    selectedDiscomfort = selectedDiscomfort,
                    onSelectDiscomfort = { selectedDiscomfort = it; questionnaireErrorMessage = "" },
                    selectedPrimaryMode = selectedPrimaryMode,
                    onSelectPrimaryMode = { selectedPrimaryMode = it; questionnaireErrorMessage = "" },
                    errorMessage = questionnaireErrorMessage,
                    onBack = { currentStage = OnboardingStage.SLIDES },
                    onNext = {
                        if (selectedGoals.isEmpty()) {
                            questionnaireErrorMessage = "Please choose at least one health goal to personalize Vital."
                        } else if (selectedRegularity.isBlank()) {
                            questionnaireErrorMessage = "Please choose how regular your menstrual cycles are."
                        } else if (selectedDiscomfort.isBlank()) {
                            questionnaireErrorMessage = "Please choose your PMS or discomfort level."
                        } else if (selectedPrimaryMode.isBlank()) {
                            questionnaireErrorMessage = "Please choose your primary tracking mode."
                        } else {
                            questionnaireErrorMessage = ""
                            currentStage = OnboardingStage.SIGN_UP
                        }
                    }
                )
            }
            OnboardingStage.SIGN_UP -> {
                SignUpSection(
                    name = name,
                    onNameChange = { name = it; signUpErrorMessage = "" },
                    email = email,
                    onEmailChange = { email = it; signUpErrorMessage = "" },
                    pin = pin,
                    onPinChange = { pin = it },
                    selectedLmpDateMs = selectedLmpDateMs,
                    hasSelectedLmpDate = hasSelectedLmpDate,
                    onOpenLmpDatePicker = { showLmpDatePicker = true },
                    onSelectLmpPreset = { targetMs ->
                        selectedLmpDateMs = targetMs
                        hasSelectedLmpDate = true
                        signUpErrorMessage = ""
                        pastCycleDates.clear()
                        pastCycleDates.add(targetMs - (avgCycleDays * dayMs))
                        pastCycleDates.add(targetMs - (2 * avgCycleDays * dayMs))
                    },
                    avgCycleDays = avgCycleDays,
                    onCycleDaysChange = { avgCycleDays = it },
                    avgPeriodDays = avgPeriodDays,
                    onPeriodDaysChange = { avgPeriodDays = it },
                    flowIntensity = flowIntensity,
                    onFlowChange = { flowIntensity = it; signUpErrorMessage = "" },
                    mood = mood,
                    onMoodChange = { mood = it; signUpErrorMessage = "" },
                    notes = notes,
                    onNotesChange = { notes = it },
                    includePastHistory = includePastHistory,
                    onTogglePastHistory = { includePastHistory = it },
                    pastCycleDates = pastCycleDates,
                    onEditPastCycle = { index ->
                        editingPastCycleIndex = index
                        showPastDatePicker = true
                    },
                    onRemovePastCycle = { index ->
                        if (pastCycleDates.size > 1) pastCycleDates.removeAt(index)
                    },
                    onAddPastCycle = {
                        val lastDate = pastCycleDates.lastOrNull() ?: selectedLmpDateMs
                        pastCycleDates.add(lastDate - (avgCycleDays * dayMs))
                    },
                    errorMessage = signUpErrorMessage,
                    onBack = { currentStage = OnboardingStage.QUESTIONNAIRE },
                    onSubmit = {
                        if (name.isBlank() || name.trim().length < 2) {
                            signUpErrorMessage = "Please enter your full name (at least 2 characters)."
                        } else if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
                            signUpErrorMessage = "Please enter a valid email address."
                        } else if (!hasSelectedLmpDate) {
                            signUpErrorMessage = "Please choose the start date of your last period (LMP) or select a preset."
                        } else if (flowIntensity.isBlank()) {
                            signUpErrorMessage = "Please choose your typical flow intensity (Light, Medium, or Heavy)."
                        } else if (mood.isBlank()) {
                            signUpErrorMessage = "Please choose your baseline mood."
                        } else {
                            val realPastDates = if (includePastHistory) {
                                pastCycleDates.filter { it < selectedLmpDateMs }.sorted()
                            } else emptyList()

                            val effectiveAvgCycleDays = if (realPastDates.isNotEmpty()) {
                                val allDates = (realPastDates + selectedLmpDateMs).sortedDescending()
                                val intervals = mutableListOf<Int>()
                                for (i in 0 until allDates.size - 1) {
                                    val diff = TimeUnit.MILLISECONDS.toDays(allDates[i] - allDates[i + 1]).toInt()
                                    if (diff in 15..60) intervals.add(diff)
                                }
                                if (intervals.isNotEmpty()) intervals.average().toInt().coerceIn(21, 45) else avgCycleDays
                            } else {
                                avgCycleDays
                            }

                            viewModel.createAccount(
                                name = name.trim(),
                                email = email.trim(),
                                pin = pin.trim(),
                                avgCycleDays = effectiveAvgCycleDays,
                                avgPeriodDays = avgPeriodDays,
                                lmpDateMs = selectedLmpDateMs,
                                flowIntensity = flowIntensity,
                                mood = mood,
                                notes = notes,
                                pastCycleDatesMs = realPastDates,
                                goals = selectedGoals.toSet(),
                                regularity = selectedRegularity,
                                discomfort = selectedDiscomfort,
                                primaryMode = selectedPrimaryMode
                            )
                            onComplete()
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 1. INTRO SLIDES
// -------------------------------------------------------------------------
@Composable
private fun IntroSlidesSection(
    onSkip: () -> Unit,
    onFinishSlides: () -> Unit
) {
    val slides = remember {
        listOf(
            IntroSlide(
                title = "Cycle & Hormone Harmony",
                subtitle = "Decode Your Natural Rhythm",
                description = "Track your menstrual phases with algorithmic accuracy. Gain personalized insights for energy, mood, and nutrition tailored to every phase of your cycle.",
                icon = Icons.Default.Spa,
                primaryColor = Color(0xFFE91E63),
                secondaryColor = Color(0xFFFCE4EC),
                tag = "Phase Harmony"
            ),
            IntroSlide(
                title = "Fertility & Ovulation Intelligence",
                subtitle = "Know Your Fertile Windows",
                description = "Whether planning to conceive or tracking natural rhythms, pinpoint your peak fertile days and ovulation windows with clinically aligned predictions.",
                icon = Icons.Default.Eco,
                primaryColor = Color(0xFF2E7D32),
                secondaryColor = Color(0xFFE8F5E9),
                tag = "Conception Intelligence"
            ),
            IntroSlide(
                title = "Health & Body Vitals Hub",
                subtitle = "All Your Biometrics in One Place",
                description = "Track basal body temperature (BBT), daily hydration, sleep, weight, and blood pressure. Generate comprehensive clinical health reports on demand.",
                icon = Icons.Default.MonitorHeart,
                primaryColor = Color(0xFF673AB7),
                secondaryColor = Color(0xFFEDE7F6),
                tag = "Clinical Vitals"
            ),
            IntroSlide(
                title = "100% Private & On-Device",
                subtitle = "Your Data Never Leaves Your Device",
                description = "Your intimate health data is strictly encrypted and stored locally on your device. Set an optional 4-digit PIN for maximum privacy.",
                icon = Icons.Default.Security,
                primaryColor = Color(0xFF00838F),
                secondaryColor = Color(0xFFE0F7FA),
                tag = "Privacy First"
            ),
            IntroSlide(
                title = "Daily Check-Ins Power Accuracy",
                subtitle = "Help Us Predict Better",
                description = "Usually filling in your quick daily check-up is the best way to help Vital predict your cycle with high precision. Daily logging trains our predictive models to your body's personal rhythm.",
                icon = Icons.Default.AutoAwesome,
                primaryColor = Color(0xFFD81B60),
                secondaryColor = Color(0xFFFCE4EC),
                tag = "Predictive Precision"
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Bar (Skip button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RosePrimary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Vital Logo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VITAL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = RoseTertiary,
                    letterSpacing = 2.sp
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.testTag("onboarding_skip_button")
            ) {
                Text(
                    text = "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Hero Icon Graphic Box with subtle gradient ring
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = slide.secondaryColor,
                    border = androidx.compose.foundation.BorderStroke(2.dp, slide.primaryColor.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .size(160.dp)
                        .padding(bottom = 20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = slide.title,
                            tint = slide.primaryColor,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = slide.primaryColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = slide.tag.uppercase(),
                        color = slide.primaryColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = slide.subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = slide.primaryColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Slide Indicators (Dots & Pill)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            repeat(slides.size) { index ->
                val isCurrent = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (isCurrent) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) RosePrimary else Color.LightGray.copy(alpha = 0.6f))
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedIconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Slide", tint = RosePrimary)
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < slides.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinishSlides()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("intro_slides_next_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
            ) {
                Text(
                    text = if (pagerState.currentPage < slides.size - 1) "Continue" else "Get Started",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (pagerState.currentPage < slides.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 2. QUESTIONNAIRE SECTION
// -------------------------------------------------------------------------
@Composable
private fun QuestionnaireSection(
    selectedGoals: List<String>,
    onToggleGoal: (String) -> Unit,
    selectedRegularity: String,
    onSelectRegularity: (String) -> Unit,
    selectedDiscomfort: String,
    onSelectDiscomfort: (String) -> Unit,
    selectedPrimaryMode: String,
    onSelectPrimaryMode: (String) -> Unit,
    errorMessage: String = "",
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val scrollState = rememberScrollState()

    val goalOptions = remember {
        listOf(
            GoalOption("track_cycle", "Track Period & Flow", "Log period days, flow heaviness, and symptoms", Icons.Default.WaterDrop),
            GoalOption("predict_fertility", "Predict Fertility & Ovulation", "Find fertile windows for conception or natural awareness", Icons.Default.Eco),
            GoalOption("track_vitals", "Sync Vitals & BBT", "Monitor waking temperature, hydration, sleep & BP", Icons.Default.MonitorHeart),
            GoalOption("pregnancy_mode", "Monitor Pregnancy", "Track weekly milestones, baby growth & clinical scans", Icons.Default.ChildCare),
            GoalOption("understand_hormones", "Hormonal Phase Balance", "Sync lifestyle, diet, and workouts with 4 cycle phases", Icons.Default.Spa),
            GoalOption("symptom_relief", "Alleviate PMS & Pain", "Identify patterns and access natural relief tips", Icons.Default.SelfImprovement)
        )
    }

    val regularityOptions = listOf(
        "Very regular (variation of 1-2 days)",
        "Somewhat regular (variation of 3-5 days)",
        "Irregular or unpredictable",
        "First time tracking / not sure"
    )

    val discomfortOptions = listOf(
        "Minimal / None (Smooth cycles)",
        "Moderate cramps & fatigue",
        "Severe PMS & cramps (Need symptom coaching)"
    )

    val primaryModeOptions = listOf(
        "Menstrual Cycle & Vitality",
        "Fertility & Trying to Conceive (TTC)",
        "Pregnancy Journey"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Step Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RosePrimary)
                    }

                    Text(
                        text = "Step 2 of 3 • Personalization",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseTertiary
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                LinearProgressIndicator(
                    progress = { 0.66f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = RosePrimary,
                    trackColor = RosePrimaryContainer.copy(alpha = 0.5f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "Personalize Your Experience",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please choose your options below. Vital tailors your cycle predictions, fertility windows, and daily coaching based on your personal answers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Predictive Calibration Note
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RosePrimaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = RosePrimary, modifier = Modifier.size(22.dp))
                    Column {
                        Text(
                            text = "Calibrate Predictive Precision",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = RoseTertiary
                        )
                        Text(
                            text = "Choose your baseline options below to calibrate period forecasts and symptom insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Question 1: What do you want to do most?
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RosePrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("1", fontWeight = FontWeight.Bold, color = RosePrimary, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "What do you want to do most?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select all that apply to you",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedGoals.isNotEmpty()) RosePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = if (selectedGoals.isNotEmpty()) "✓ ${selectedGoals.size} chosen" else "Required *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedGoals.isNotEmpty()) RosePrimary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    goalOptions.forEach { goal ->
                        val isSelected = selectedGoals.contains(goal.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) RosePrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) RosePrimary else Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleGoal(goal.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = goal.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) RosePrimary else Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = goal.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) RoseTertiary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = goal.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleGoal(goal.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = RosePrimary)
                                )
                            }
                        }
                    }
                }
            }

            // Question 2: How regular are your periods?
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RosePrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("2", fontWeight = FontWeight.Bold, color = RosePrimary, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "How regular are your menstrual cycles?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedRegularity.isNotEmpty()) RosePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = if (selectedRegularity.isNotEmpty()) "✓ Chosen" else "Required *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRegularity.isNotEmpty()) RosePrimary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    regularityOptions.forEach { option ->
                        val isSelected = selectedRegularity == option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) RosePrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) RosePrimary else Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectRegularity(option) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RoseTertiary else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectRegularity(option) },
                                    colors = RadioButtonDefaults.colors(selectedColor = RosePrimary)
                                )
                            }
                        }
                    }
                }
            }

            // Question 3: PMS & Menstrual Discomfort
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RosePrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("3", fontWeight = FontWeight.Bold, color = RosePrimary, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Do you experience PMS or pain?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDiscomfort.isNotEmpty()) RosePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = if (selectedDiscomfort.isNotEmpty()) "✓ Chosen" else "Required *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedDiscomfort.isNotEmpty()) RosePrimary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    discomfortOptions.forEach { option ->
                        val isSelected = selectedDiscomfort == option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) RosePrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) RosePrimary else Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectDiscomfort(option) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RoseTertiary else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectDiscomfort(option) },
                                    colors = RadioButtonDefaults.colors(selectedColor = RosePrimary)
                                )
                            }
                        }
                    }
                }
            }

            // Question 4: Primary App Focus Mode
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RosePrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("4", fontWeight = FontWeight.Bold, color = RosePrimary, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Primary tracking mode:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPrimaryMode.isNotEmpty()) RosePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = if (selectedPrimaryMode.isNotEmpty()) "✓ Chosen" else "Required *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPrimaryMode.isNotEmpty()) RosePrimary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    primaryModeOptions.forEach { option ->
                        val isSelected = selectedPrimaryMode == option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) RosePrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) RosePrimary else Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectPrimaryMode(option) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RoseTertiary else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectPrimaryMode(option) },
                                    colors = RadioButtonDefaults.colors(selectedColor = RosePrimary)
                                )
                            }
                        }
                    }
                }
            }

            // Validation Error Banner
            if (errorMessage.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Continue Button
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("questionnaire_continue_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
            ) {
                Text("Continue to Sign-Up & Baseline", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------------------
// 3. SIGN-UP & BASELINE SECTION
// -------------------------------------------------------------------------
@Composable
private fun SignUpSection(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    selectedLmpDateMs: Long,
    hasSelectedLmpDate: Boolean,
    onOpenLmpDatePicker: () -> Unit,
    onSelectLmpPreset: (Long) -> Unit,
    avgCycleDays: Int,
    onCycleDaysChange: (Int) -> Unit,
    avgPeriodDays: Int,
    onPeriodDaysChange: (Int) -> Unit,
    flowIntensity: String,
    onFlowChange: (String) -> Unit,
    mood: String,
    onMoodChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    includePastHistory: Boolean,
    onTogglePastHistory: (Boolean) -> Unit,
    pastCycleDates: List<Long>,
    onEditPastCycle: (Int) -> Unit,
    onRemovePastCycle: (Int) -> Unit,
    onAddPastCycle: () -> Unit,
    errorMessage: String,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()
    val dayMs = 86400000L
    val now = System.currentTimeMillis()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Step Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RosePrimary)
                    }

                    Text(
                        text = "Step 3 of 3 • Account & Baseline",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseTertiary
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                LinearProgressIndicator(
                    progress = { 1.0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = RosePrimary,
                    trackColor = RosePrimaryContainer.copy(alpha = 0.5f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "Create Your Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure your baseline flow dates and credentials to complete setup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Account Credentials Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Your Profile Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RoseTertiary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Your Full Name") },
                        placeholder = { Text("e.g., Sarah Johnson") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RosePrimary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g., sarah@example.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = RosePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_email_input")
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) onPinChange(it) },
                        label = { Text("Optional 4-Digit PIN (App Lock)") },
                        placeholder = { Text("Protects app launch") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RosePrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_pin_input")
                    )
                }
            }

            // Flow Baseline Customization Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Flow & Cycle Baseline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RoseTertiary
                    )

                    // Last Period Date Picker Trigger
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "When did your last period / flow start?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RoseTertiary
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLmpDatePicker() }
                                .testTag("onboarding_lmp_picker_trigger"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RosePrimary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RosePrimary)
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Last Flow Start Date", fontSize = 11.sp, color = Color.Gray)
                                            if (!hasSelectedLmpDate) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                                ) {
                                                    Text("Required *", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            } else {
                                                Text("✓ Selected", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                                            }
                                        }
                                        Text(
                                            text = if (hasSelectedLmpDate) CycleUtils.formatDate(selectedLmpDateMs) else "Tap to choose last flow date",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasSelectedLmpDate) RoseTertiary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = onOpenLmpDatePicker,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick Date", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                0 to "Today",
                                1 to "Yesterday",
                                3 to "3d ago",
                                7 to "1w ago",
                                14 to "2w ago"
                            )
                            presets.forEach { (daysAgo, label) ->
                                val targetMs = now - (daysAgo * dayMs)
                                val isSelected = hasSelectedLmpDate && (now - selectedLmpDateMs) in (daysAgo * dayMs - 43200000L)..(daysAgo * dayMs + 43200000L)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectLmpPreset(targetMs) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Average Cycle Length (+ and -)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Average Cycle Length", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Days between periods (21-45 days)", fontSize = 11.sp, color = Color.Gray)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { if (avgCycleDays > 21) onCycleDaysChange(avgCycleDays - 1) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = RosePrimary)
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.defaultMinSize(minWidth = 64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)) {
                                    Text("$avgCycleDays d", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = RoseTertiary)
                                }
                            }

                            FilledTonalIconButton(
                                onClick = { if (avgCycleDays < 45) onCycleDaysChange(avgCycleDays + 1) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = RosePrimary)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Average Period Duration (+ and -)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Average Period Duration", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Bleeding days (2-10 days)", fontSize = 11.sp, color = Color.Gray)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { if (avgPeriodDays > 2) onPeriodDaysChange(avgPeriodDays - 1) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = RosePrimary)
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.defaultMinSize(minWidth = 64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)) {
                                    Text("$avgPeriodDays d", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = RoseTertiary)
                                }
                            }

                            FilledTonalIconButton(
                                onClick = { if (avgPeriodDays < 10) onPeriodDaysChange(avgPeriodDays + 1) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = RosePrimary)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Flow Intensity
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Typical Flow Intensity:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (flowIntensity.isBlank()) {
                                Text("Required *", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            } else {
                                Text("✓ $flowIntensity", fontSize = 10.sp, color = RosePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Light", "Medium", "Heavy").forEach { intensity ->
                                FilterChip(
                                    selected = flowIntensity == intensity,
                                    onClick = { onFlowChange(intensity) },
                                    label = { Text(intensity) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Baseline Mood
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Baseline Mood:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (mood.isBlank()) {
                                Text("Required *", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            } else {
                                Text("✓ $mood", fontSize = 10.sp, color = RosePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Happy", "Neutral", "Crampy", "Tired").forEach { m ->
                                FilterChip(
                                    selected = mood == m,
                                    onClick = { onMoodChange(m) },
                                    label = { Text(m, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Past Cycle History Card (Actual Dates)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = RosePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Past Cycle History", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoseTertiary)
                                Text("High-accuracy real dates", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Switch(
                            checked = includePastHistory,
                            onCheckedChange = onTogglePastHistory,
                            modifier = Modifier.testTag("onboarding_past_history_switch")
                        )
                    }

                    if (includePastHistory) {
                        Text(
                            text = "Previous Period Start Dates:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseTertiary
                        )

                        pastCycleDates.forEachIndexed { index, dateMs ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditPastCycle(index) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("-${index + 1}", fontWeight = FontWeight.Bold, color = RosePrimary, fontSize = 12.sp)
                                        Column {
                                            Text("Period #${index + 1}", fontSize = 11.sp, color = Color.Gray)
                                            Text(CycleUtils.formatDate(dateMs), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { onEditPastCycle(index) }) {
                                            Text("Change Date", fontSize = 11.sp)
                                        }
                                        if (pastCycleDates.size > 1) {
                                            IconButton(onClick = { onRemovePastCycle(index) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (pastCycleDates.size < 5) {
                            OutlinedButton(
                                onClick = onAddPastCycle,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add Another Past Period Start Date", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Submit Button
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_complete_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Account & Enter Vital", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
