package com.teraxes.vital.ui.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teraxes.vital.data.database.VitalDatabase
import com.teraxes.vital.data.model.*
import com.teraxes.vital.data.repository.VitalRepository
import com.teraxes.vital.domain.CorrelationService
import com.teraxes.vital.domain.CyclePredictions
import com.teraxes.vital.domain.CycleStats
import com.teraxes.vital.domain.CycleUtils
import com.teraxes.vital.notification.NotificationHelper
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import java.security.MessageDigest
import java.security.SecureRandom

class VitalViewModel(application: Application) : AndroidViewModel(application) {

    // App Preferences / Privacy / Account Profile State
    private val prefs = application.getSharedPreferences("vital_user_prefs", android.content.Context.MODE_PRIVATE)

    private fun getOrCreateSalt(): String {
        var salt = prefs.getString("pin_salt", null)
        if (salt == null) {
            val randomBytes = ByteArray(16)
            SecureRandom().nextBytes(randomBytes)
            salt = randomBytes.joinToString("") { "%02x".format(it) }
            prefs.edit().putString("pin_salt", salt).apply()
        }
        return salt
    }

    private fun hashPin(pin: String): String {
        if (pin.isEmpty()) return ""
        val salt = getOrCreateSalt()
        val saltedInput = "$salt:$pin"
        val bytes = MessageDigest.getInstance("SHA-256").digest(saltedInput.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private val firebaseAnalytics = try {
        FirebaseAnalytics.getInstance(application)
    } catch (e: Exception) {
        null
    }

    private val repository: VitalRepository

    val cycles: StateFlow<List<CycleEntity>>
    val symptoms: StateFlow<List<SymptomLogEntity>>
    val activePregnancy: StateFlow<PregnancyEntity?>
    val healthMetrics: StateFlow<List<HealthMetricEntity>>
    val reminders: StateFlow<List<ReminderEntity>>

    val cycleStats: StateFlow<CycleStats>
    val cyclePredictions: StateFlow<CyclePredictions?>
    val correlationInsights: StateFlow<List<String>>

    private val _isAccountCreated = MutableStateFlow<Boolean>(prefs.getBoolean("is_account_created", false))
    val isAccountCreated: StateFlow<Boolean> = _isAccountCreated.asStateFlow()

    private val _userName = MutableStateFlow<String>(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow<String>(prefs.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userGoals = MutableStateFlow<Set<String>>(prefs.getStringSet("user_goals", emptySet()) ?: emptySet())
    val userGoals: StateFlow<Set<String>> = _userGoals.asStateFlow()

    private val _userRegularity = MutableStateFlow<String>(prefs.getString("user_regularity", "Regular") ?: "Regular")
    val userRegularity: StateFlow<String> = _userRegularity.asStateFlow()

    private val _userDiscomfort = MutableStateFlow<String>(prefs.getString("user_discomfort", "Mild") ?: "Mild")
    val userDiscomfort: StateFlow<String> = _userDiscomfort.asStateFlow()

    private val _userPrimaryMode = MutableStateFlow<String>(prefs.getString("user_primary_mode", "Cycle & Vitals") ?: "Cycle & Vitals")
    val userPrimaryMode: StateFlow<String> = _userPrimaryMode.asStateFlow()

    private val _avgCycleLength = MutableStateFlow<Int>(prefs.getInt("avg_cycle_length", 28))
    val avgCycleLength: StateFlow<Int> = _avgCycleLength.asStateFlow()

    private val _avgPeriodLength = MutableStateFlow<Int>(prefs.getInt("avg_period_length", 5))
    val avgPeriodLength: StateFlow<Int> = _avgPeriodLength.asStateFlow()

    private val _userPin = MutableStateFlow<String>(prefs.getString("user_pin", "") ?: "")
    val userPin: StateFlow<String> = _userPin.asStateFlow()

    private val _isLocked = MutableStateFlow<Boolean>(_userPin.value.isNotEmpty())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Brute Force Protection
    private val _failedPinAttempts = MutableStateFlow<Int>(0)
    val failedPinAttempts: StateFlow<Int> = _failedPinAttempts.asStateFlow()

    private val _lockoutUntilMs = MutableStateFlow<Long>(0L)
    val lockoutUntilMs: StateFlow<Long> = _lockoutUntilMs.asStateFlow()

    private val _isCycleReminderEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("cycle_reminder_enabled", true))
    val isCycleReminderEnabled: StateFlow<Boolean> = _isCycleReminderEnabled.asStateFlow()

    private val _reminderDaysBefore = MutableStateFlow<Int>(prefs.getInt("reminder_days_before", 2))
    val reminderDaysBefore: StateFlow<Int> = _reminderDaysBefore.asStateFlow()

    private val _isFertileReminderEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("fertile_reminder_enabled", true))
    val isFertileReminderEnabled: StateFlow<Boolean> = _isFertileReminderEnabled.asStateFlow()

    private val _isDiscreetMode = MutableStateFlow<Boolean>(prefs.getBoolean("discreet_mode_enabled", false))
    val isDiscreetMode: StateFlow<Boolean> = _isDiscreetMode.asStateFlow()

    private val _isDailyCheckInReminderEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("daily_checkin_enabled", true))
    val isDailyCheckInReminderEnabled: StateFlow<Boolean> = _isDailyCheckInReminderEnabled.asStateFlow()

    private val _isMedicationReminderEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("medication_reminder_enabled", false))
    val isMedicationReminderEnabled: StateFlow<Boolean> = _isMedicationReminderEnabled.asStateFlow()

    private val _isBlindPredictionEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("blind_prediction_enabled", true))
    val isBlindPredictionEnabled: StateFlow<Boolean> = _isBlindPredictionEnabled.asStateFlow()

    private val _lastCheckInDate = MutableStateFlow<String>(prefs.getString("last_check_in_date", "") ?: "")
    val lastCheckInDate: StateFlow<String> = _lastCheckInDate.asStateFlow()

    init {
        val db = VitalDatabase.getDatabase(application)
        repository = VitalRepository(db)

        cycles = repository.cycles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        symptoms = repository.symptoms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activePregnancy = repository.activePregnancy.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        healthMetrics = repository.healthMetrics.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        reminders = repository.reminders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        cycleStats = combine(cycles, _avgCycleLength, _avgPeriodLength) { list, avgC, avgP ->
            CycleUtils.calculateStats(list, fallbackAvgLength = avgC, fallbackPeriodLength = avgP)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CycleStats()
        )

        cyclePredictions = combine(cycles, _avgCycleLength) { list, avgC ->
            CycleUtils.predictNextCycle(list, fallbackAvgLength = avgC)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        correlationInsights = combine(cycles, symptoms) { cList, sList ->
            CorrelationService.analyzeSymptomPatterns(cList, sList)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed starting reference baseline if database is empty
        viewModelScope.launch {
            seedSampleBaselineIfNeeded()
        }

        // Schedule cycle and fertile reminder notifications when predictions or notification preferences change
        viewModelScope.launch {
            combine(
                cyclePredictions,
                _isCycleReminderEnabled,
                _reminderDaysBefore,
                _isFertileReminderEnabled,
                _isDiscreetMode
            ) { preds, cycleEnabled, days, fertileEnabled, discreet ->
                PredictionReminderConfig(preds, cycleEnabled, days, fertileEnabled, discreet)
            }.collect { config ->
                val preds = config.preds
                if (preds != null) {
                    prefs.edit()
                        .putLong("last_predicted_next_start_ms", preds.nextPeriodDate)
                        .putLong("last_predicted_fertile_start_ms", preds.fertileWindowStart)
                        .apply()

                    if (config.cycleEnabled) {
                        NotificationHelper.scheduleCycleReminder(
                            context = getApplication(),
                            predictedStartMs = preds.nextPeriodDate,
                            fertileWindowStartMs = if (config.fertileEnabled) preds.fertileWindowStart else 0L,
                            daysBefore = config.days,
                            isDiscreetMode = config.discreet
                        )
                    } else {
                        NotificationHelper.cancelCycleReminders(getApplication())
                    }
                } else if (!config.cycleEnabled) {
                    NotificationHelper.cancelCycleReminders(getApplication())
                }
            }
        }

        // Schedule recurring daily check-in and medication reminders on startup if enabled
        if (_isDailyCheckInReminderEnabled.value) {
            NotificationHelper.scheduleDailyCheckInReminder(
                context = application,
                hour = 20,
                minute = 0,
                isDiscreetMode = _isDiscreetMode.value
            )
        }
        if (_isMedicationReminderEnabled.value) {
            NotificationHelper.scheduleMedicationReminder(
                context = application,
                hour = 9,
                minute = 0,
                isDiscreetMode = _isDiscreetMode.value
            )
        }
    }

    private suspend fun seedSampleBaselineIfNeeded() {
        val currentCycles = repository.cycles.first()
        if (currentCycles.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            // Starting reference baseline: 28-day average cycle, 5-day flow duration starting 14 days ago
            val sampleStart = now - (14 * dayMs)
            val sampleEnd = sampleStart + (5 * dayMs)
            repository.insertCycle(
                CycleEntity(
                    startDate = sampleStart,
                    endDate = sampleEnd,
                    flowIntensity = "Medium",
                    mood = "Happy",
                    symptomsJson = "[\"Mild Cramps\", \"Normal Flow\"]",
                    notes = "Sample reference baseline (28-day cycle average, 5-day flow duration)"
                )
            )

            // Seed sample health metrics
            repository.insertMetric(
                HealthMetricEntity(
                    type = "temperature",
                    value = 36.6,
                    unit = "°C",
                    loggedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                )
            )
            repository.insertMetric(
                HealthMetricEntity(
                    type = "weight",
                    value = 62.5,
                    unit = "kg",
                    loggedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                )
            )

            // Seed sample reminder
            repository.insertReminder(
                ReminderEntity(type = "cycle", title = "Period due in 14 days (Sample)", time = "09:00 AM")
            )
        }
    }

    fun createAccount(
        name: String,
        email: String,
        pin: String,
        avgCycleDays: Int,
        avgPeriodDays: Int,
        lmpDateMs: Long,
        flowIntensity: String,
        mood: String,
        notes: String,
        pastCycleDatesMs: List<Long> = emptyList(),
        goals: Set<String> = emptySet(),
        regularity: String = "Regular",
        discomfort: String = "Mild",
        primaryMode: String = "Cycle & Vitals"
    ) {
        viewModelScope.launch {
            // Clear sample reference baseline before inserting user's custom cycle
            repository.clearAllCycles()

            // Save profile details to preferences
            val hashedPin = if (pin.isNotBlank()) hashPin(pin) else ""
            prefs.edit()
                .putBoolean("is_account_created", true)
                .putString("user_name", name.ifBlank { "User" })
                .putString("user_email", email)
                .putString("user_pin", hashedPin)
                .putInt("avg_cycle_length", avgCycleDays)
                .putInt("avg_period_length", avgPeriodDays)
                .putStringSet("user_goals", goals)
                .putString("user_regularity", regularity)
                .putString("user_discomfort", discomfort)
                .putString("user_primary_mode", primaryMode)
                .apply()

            _isAccountCreated.value = true
            _userName.value = name.ifBlank { "User" }
            _userEmail.value = email
            _userPin.value = hashedPin
            _isLocked.value = false
            _avgCycleLength.value = avgCycleDays
            _avgPeriodLength.value = avgPeriodDays
            _userGoals.value = goals
            _userRegularity.value = regularity
            _userDiscomfort.value = discomfort
            _userPrimaryMode.value = primaryMode

            val dayMs = 86400000L

            // Insert optional historical cycles first (sorted chronologically)
            pastCycleDatesMs.sorted().forEachIndexed { index, pastStartMs ->
                val pastEndMs = pastStartMs + (avgPeriodDays * dayMs)
                repository.insertCycle(
                    CycleEntity(
                        startDate = pastStartMs,
                        endDate = pastEndMs,
                        flowIntensity = flowIntensity,
                        mood = mood,
                        symptomsJson = "[\"Past Cycle History\"]",
                        notes = "Historical period #${index + 1} provided during account setup"
                    )
                )
            }

            // Calculate initial cycle end date based on custom period duration
            val initialEndDate = lmpDateMs + (avgPeriodDays * dayMs)

            // Insert initial user-defined cycle log (most recent LMP)
            repository.insertCycle(
                CycleEntity(
                    startDate = lmpDateMs,
                    endDate = initialEndDate,
                    flowIntensity = flowIntensity,
                    mood = mood,
                    symptomsJson = "[\"Baseline Flow Entry\"]",
                    notes = notes.ifBlank { "Initial baseline entry from account setup" }
                )
            )

            // Log Firebase Analytics Event
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SIGN_UP, Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, "custom_profile")
            })
            firebaseAnalytics?.logEvent("account_created", Bundle().apply {
                putInt("avg_cycle_days", avgCycleDays)
                putInt("avg_period_days", avgPeriodDays)
            })
        }
    }

    // Actions
    fun logCycle(
        startDate: Long,
        endDate: Long?,
        flowIntensity: String,
        mood: String,
        symptoms: List<String>,
        notes: String
    ) {
        viewModelScope.launch {
            val symptomsJson = "[" + symptoms.joinToString(",") { "\"$it\"" } + "]"
            repository.insertCycle(
                CycleEntity(
                    startDate = startDate,
                    endDate = endDate,
                    flowIntensity = flowIntensity,
                    mood = mood,
                    symptomsJson = symptomsJson,
                    notes = notes
                )
            )

            firebaseAnalytics?.logEvent("log_cycle", Bundle().apply {
                putString("flow_intensity", flowIntensity)
                putString("mood", mood)
            })
        }
    }

    fun deleteCycle(id: Long) {
        viewModelScope.launch {
            repository.deleteCycle(id)
        }
    }

    fun logSymptom(date: String, symptomType: String, severity: String, notes: String) {
        viewModelScope.launch {
            repository.insertSymptom(
                SymptomLogEntity(
                    date = date,
                    symptomType = symptomType,
                    severity = severity,
                    notes = notes
                )
            )

            firebaseAnalytics?.logEvent("log_symptom", Bundle().apply {
                putString("symptom_type", symptomType)
                putString("severity", severity)
            })
        }
    }

    fun logDailyCheckIn(
        date: String,
        mood: String,
        energyLevel: String,
        symptoms: List<String>,
        notes: String,
        periodFlow: String = "None",
        spotting: String = "None",
        painLevel: String = "Optimal"
    ) {
        viewModelScope.launch {
            prefs.edit().putString("last_check_in_date", date).apply()
            _lastCheckInDate.value = date

            val summaryDetails = buildString {
                append("Mood: $mood • Energy: $energyLevel • Pain: $painLevel")
                if (periodFlow != "None") append(" • Period: $periodFlow")
                if (spotting != "None") append(" • Spotting: $spotting")
                if (notes.isNotBlank()) append(" • Notes: $notes")
            }

            // Insert primary check-in entry with full daily snapshot
            repository.insertSymptom(
                SymptomLogEntity(
                    date = date,
                    symptomType = "Daily Check-In: $mood",
                    severity = energyLevel,
                    notes = summaryDetails
                )
            )

            // If period flow or spotting is reported, log specific symptom entry
            if (periodFlow != "None") {
                repository.insertSymptom(
                    SymptomLogEntity(
                        date = date,
                        symptomType = "Menstrual Flow ($periodFlow)",
                        severity = when (periodFlow) {
                            "Heavy", "Very Heavy" -> "Severe"
                            "Medium" -> "Moderate"
                            else -> "Mild"
                        },
                        notes = "Logged via Daily Check-In"
                    )
                )
            }

            if (spotting != "None") {
                repository.insertSymptom(
                    SymptomLogEntity(
                        date = date,
                        symptomType = "Spotting/Discharge ($spotting)",
                        severity = "Mild",
                        notes = "Logged via Daily Check-In"
                    )
                )
            }

            if (painLevel != "Optimal" && painLevel != "No Pain") {
                repository.insertSymptom(
                    SymptomLogEntity(
                        date = date,
                        symptomType = "Pain / Cramps ($painLevel)",
                        severity = when {
                            painLevel.contains("Debilitating") || painLevel.contains("Severe") -> "Severe"
                            painLevel.contains("Moderate") -> "Moderate"
                            else -> "Mild"
                        },
                        notes = "Logged via Daily Check-In"
                    )
                )
            }

            // Insert any specific physical symptoms selected
            symptoms.forEach { sym ->
                repository.insertSymptom(
                    SymptomLogEntity(
                        date = date,
                        symptomType = sym,
                        severity = "Moderate",
                        notes = "Logged via Daily Check-In"
                    )
                )
            }

            firebaseAnalytics?.logEvent("daily_check_in", Bundle().apply {
                putString("mood", mood)
                putString("energy", energyLevel)
                putString("period_flow", periodFlow)
                putString("spotting", spotting)
                putString("pain", painLevel)
                putInt("symptoms_count", symptoms.size)
            })
        }
    }

    fun deleteSymptom(id: Long) {
        viewModelScope.launch {
            repository.deleteSymptom(id)
        }
    }

    fun startPregnancy(
        lmpDate: Long,
        dueDate: Long? = null,
        isConfirmedByDoctor: Boolean = false,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val dayMs = 86400000L
            val calculatedDueDate = dueDate ?: (lmpDate + (280 * dayMs))
            val now = System.currentTimeMillis()
            val totalDays = ((now - lmpDate) / dayMs).toInt().coerceAtLeast(0)
            val calculatedWeek = ((totalDays / 7) + 1).coerceIn(1, 42)

            val pregId = repository.insertPregnancy(
                PregnancyEntity(
                    startDate = lmpDate,
                    dueDate = calculatedDueDate,
                    lastPeriodDate = lmpDate,
                    currentWeek = calculatedWeek,
                    isConfirmedByDoctor = isConfirmedByDoctor,
                    notes = notes
                )
            )

            // Insert default milestones
            val milestones = listOf(
                PregnancyMilestoneEntity(pregnancyId = pregId, week = 8, title = "First Ultrasound", description = "Confirm heartbeat & due date"),
                PregnancyMilestoneEntity(pregnancyId = pregId, week = 12, title = "NT Screening Scan", description = "Nuchal translucency check"),
                PregnancyMilestoneEntity(pregnancyId = pregId, week = 20, title = "Anatomy Scan", description = "Detailed fetal organ check"),
                PregnancyMilestoneEntity(pregnancyId = pregId, week = 24, title = "Glucose Screening", description = "Gestational diabetes test"),
                PregnancyMilestoneEntity(pregnancyId = pregId, week = 36, title = "Group B Strep Test", description = "Pre-labor screening")
            )
            milestones.forEach { repository.insertMilestone(it) }
        }
    }

    fun endPregnancy() {
        viewModelScope.launch {
            repository.deletePregnancy()
        }
    }

    fun getPregnancyMilestones(pregnancyId: Long): Flow<List<PregnancyMilestoneEntity>> {
        return repository.getPregnancyMilestones(pregnancyId)
    }

    fun toggleMilestone(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setMilestoneCompleted(id, completed)
        }
    }

    fun logMetric(type: String, value: Double, unit: String, date: String, notes: String) {
        viewModelScope.launch {
            repository.insertMetric(
                HealthMetricEntity(
                    type = type,
                    value = value,
                    unit = unit,
                    loggedDate = date,
                    notes = notes
                )
            )
        }
    }

    fun deleteMetric(id: Long) {
        viewModelScope.launch {
            repository.deleteMetric(id)
        }
    }

    // Security PIN & Account Reset
    fun setPin(pin: String) {
        val hashed = hashPin(pin)
        prefs.edit().putString("user_pin", hashed).apply()
        _userPin.value = hashed
        _isLocked.value = pin.isNotEmpty()
        _failedPinAttempts.value = 0
        _lockoutUntilMs.value = 0L
    }

    fun resetAccount() {
        viewModelScope.launch {
            repository.clearAllData()
            prefs.edit().clear().apply()
            _isAccountCreated.value = false
            _userName.value = ""
            _userEmail.value = ""
            _userPin.value = ""
            _isLocked.value = false
            _failedPinAttempts.value = 0
            _lockoutUntilMs.value = 0L
            _avgCycleLength.value = 28
            _avgPeriodLength.value = 5
            _userGoals.value = emptySet()
            _userRegularity.value = "Regular"
            _userDiscomfort.value = "Mild"
            _userPrimaryMode.value = "Cycle & Vitals"

            // Re-seed sample baseline for preview mode
            seedSampleBaselineIfNeeded()
        }
    }

    fun isCurrentlyLockedOut(): Boolean {
        return System.currentTimeMillis() < _lockoutUntilMs.value
    }

    fun getRemainingLockoutSeconds(): Int {
        val remainingMs = _lockoutUntilMs.value - System.currentTimeMillis()
        return if (remainingMs > 0) ((remainingMs + 999) / 1000).toInt() else 0
    }

    fun unlockApp(pin: String): Boolean {
        if (isCurrentlyLockedOut()) {
            return false
        }

        if (_userPin.value.isEmpty() || _userPin.value == hashPin(pin)) {
            _isLocked.value = false
            _failedPinAttempts.value = 0
            _lockoutUntilMs.value = 0L
            return true
        } else {
            val newFailed = _failedPinAttempts.value + 1
            _failedPinAttempts.value = newFailed
            if (newFailed >= 5) {
                // Lock out for 30 seconds to prevent brute force
                _lockoutUntilMs.value = System.currentTimeMillis() + 30_000L
                _failedPinAttempts.value = 0
            }
            return false
        }
    }

    fun lockApp() {
        if (_userPin.value.isNotEmpty()) {
            _isLocked.value = true
        }
    }

    // Local Notification & Privacy Settings
    fun setCycleReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cycle_reminder_enabled", enabled).apply()
        _isCycleReminderEnabled.value = enabled
        if (enabled && cyclePredictions.value != null) {
            val preds = cyclePredictions.value!!
            NotificationHelper.scheduleCycleReminder(
                context = getApplication(),
                predictedStartMs = preds.nextPeriodDate,
                fertileWindowStartMs = if (_isFertileReminderEnabled.value) preds.fertileWindowStart else 0L,
                daysBefore = _reminderDaysBefore.value,
                isDiscreetMode = _isDiscreetMode.value
            )
        } else if (!enabled) {
            NotificationHelper.cancelCycleReminders(getApplication())
        }
    }

    fun setReminderDaysBefore(days: Int) {
        prefs.edit().putInt("reminder_days_before", days).apply()
        _reminderDaysBefore.value = days
        if (_isCycleReminderEnabled.value && cyclePredictions.value != null) {
            val preds = cyclePredictions.value!!
            NotificationHelper.scheduleCycleReminder(
                context = getApplication(),
                predictedStartMs = preds.nextPeriodDate,
                fertileWindowStartMs = if (_isFertileReminderEnabled.value) preds.fertileWindowStart else 0L,
                daysBefore = days,
                isDiscreetMode = _isDiscreetMode.value
            )
        }
    }

    fun setFertileReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("fertile_reminder_enabled", enabled).apply()
        _isFertileReminderEnabled.value = enabled
        if (_isCycleReminderEnabled.value && cyclePredictions.value != null) {
            val preds = cyclePredictions.value!!
            NotificationHelper.scheduleCycleReminder(
                context = getApplication(),
                predictedStartMs = preds.nextPeriodDate,
                fertileWindowStartMs = if (enabled) preds.fertileWindowStart else 0L,
                daysBefore = _reminderDaysBefore.value,
                isDiscreetMode = _isDiscreetMode.value
            )
        }
    }

    fun setDiscreetMode(enabled: Boolean) {
        prefs.edit().putBoolean("discreet_mode_enabled", enabled).apply()
        _isDiscreetMode.value = enabled
        firebaseAnalytics?.logEvent("toggle_discreet_mode", Bundle().apply {
            putBoolean("enabled", enabled)
        })
        if (_isCycleReminderEnabled.value && cyclePredictions.value != null) {
            val preds = cyclePredictions.value!!
            NotificationHelper.scheduleCycleReminder(
                context = getApplication(),
                predictedStartMs = preds.nextPeriodDate,
                fertileWindowStartMs = if (_isFertileReminderEnabled.value) preds.fertileWindowStart else 0L,
                daysBefore = _reminderDaysBefore.value,
                isDiscreetMode = enabled
            )
        }
        if (_isDailyCheckInReminderEnabled.value) {
            NotificationHelper.scheduleDailyCheckInReminder(
                context = getApplication(),
                isDiscreetMode = enabled
            )
        }
        if (_isMedicationReminderEnabled.value) {
            NotificationHelper.scheduleMedicationReminder(
                context = getApplication(),
                isDiscreetMode = enabled
            )
        }
    }

    fun setDailyCheckInReminder(enabled: Boolean) {
        prefs.edit().putBoolean("daily_checkin_enabled", enabled).apply()
        _isDailyCheckInReminderEnabled.value = enabled
        if (enabled) {
            NotificationHelper.scheduleDailyCheckInReminder(
                context = getApplication(),
                hour = 20,
                minute = 0,
                isDiscreetMode = _isDiscreetMode.value
            )
        } else {
            NotificationHelper.cancelDailyCheckInReminder(getApplication())
        }
    }

    fun setMedicationReminder(enabled: Boolean) {
        prefs.edit().putBoolean("medication_reminder_enabled", enabled).apply()
        _isMedicationReminderEnabled.value = enabled
        if (enabled) {
            NotificationHelper.scheduleMedicationReminder(
                context = getApplication(),
                hour = 9,
                minute = 0,
                isDiscreetMode = _isDiscreetMode.value
            )
        } else {
            NotificationHelper.cancelMedicationReminder(getApplication())
        }
    }

    fun onNotificationPermissionGranted() {
        if (_isCycleReminderEnabled.value && cyclePredictions.value != null) {
            val preds = cyclePredictions.value!!
            NotificationHelper.scheduleCycleReminder(
                context = getApplication(),
                predictedStartMs = preds.nextPeriodDate,
                fertileWindowStartMs = if (_isFertileReminderEnabled.value) preds.fertileWindowStart else 0L,
                daysBefore = _reminderDaysBefore.value,
                isDiscreetMode = _isDiscreetMode.value
            )
        }
        if (_isDailyCheckInReminderEnabled.value) {
            NotificationHelper.scheduleDailyCheckInReminder(
                context = getApplication(),
                isDiscreetMode = _isDiscreetMode.value
            )
        }
        if (_isMedicationReminderEnabled.value) {
            NotificationHelper.scheduleMedicationReminder(
                context = getApplication(),
                isDiscreetMode = _isDiscreetMode.value
            )
        }
    }

    fun sendTestNotification(type: String = "cycle"): Boolean {
        val preds = cyclePredictions.value
        val isDiscreet = _isDiscreetMode.value

        val tuple = when (type) {
            "fertile" -> {
                if (isDiscreet) {
                    NotificationTuple(
                        "✨ Calendar Schedule Update",
                        "An estimated wellness phase is approaching on your calendar.",
                        NotificationHelper.NOTIFICATION_ID_FERTILITY,
                        NotificationHelper.CHANNEL_FERTILITY_ID,
                        "FERTILITY"
                    )
                } else {
                    NotificationTuple(
                        "✨ Fertile Window Reminder",
                        "Your predicted fertile window is approaching. Open Vital to view ovulation and fertility estimates.",
                        NotificationHelper.NOTIFICATION_ID_FERTILITY,
                        NotificationHelper.CHANNEL_FERTILITY_ID,
                        "FERTILITY"
                    )
                }
            }
            "daily" -> {
                if (isDiscreet) {
                    NotificationTuple(
                        "📝 Daily Note • Better Predictions",
                        "Usually filling your daily entry helps predict your schedule much better. Tap to log today.",
                        NotificationHelper.NOTIFICATION_ID_DAILY_LOG,
                        NotificationHelper.CHANNEL_DAILY_ID,
                        "DASHBOARD"
                    )
                } else {
                    NotificationTuple(
                        "🌸 Daily Check-In • Improve Predictions",
                        "Usually filling your daily check-up helps Vital predict your cycle & fertile window much better. Tap to log today's vitals & symptoms!",
                        NotificationHelper.NOTIFICATION_ID_DAILY_LOG,
                        NotificationHelper.CHANNEL_DAILY_ID,
                        "DASHBOARD"
                    )
                }
            }
            "medication" -> {
                if (isDiscreet) {
                    NotificationTuple(
                        "💊 Routine Reminder",
                        "Scheduled routine reminder for today.",
                        NotificationHelper.NOTIFICATION_ID_MEDICATION,
                        NotificationHelper.CHANNEL_MEDICATION_ID,
                        "HEALTH"
                    )
                } else {
                    NotificationTuple(
                        "💊 Daily Medication / Pill Alert",
                        "Time to take your scheduled contraceptive pill or daily vitamins.",
                        NotificationHelper.NOTIFICATION_ID_MEDICATION,
                        NotificationHelper.CHANNEL_MEDICATION_ID,
                        "HEALTH"
                    )
                }
            }
            else -> {
                if (isDiscreet) {
                    NotificationTuple(
                        "📅 Calendar Schedule Update",
                        "You have an upcoming event in your personal wellness tracker.",
                        NotificationHelper.NOTIFICATION_ID_APPROACHING,
                        NotificationHelper.CHANNEL_ID,
                        "CYCLE"
                    )
                } else {
                    val dateFormatted = if (preds != null) CycleUtils.formatDateShort(preds.nextPeriodDate) else "soon"
                    NotificationTuple(
                        "🌸 Vital Cycle Reminder",
                        "Your predicted cycle start date is approaching on $dateFormatted.",
                        NotificationHelper.NOTIFICATION_ID_APPROACHING,
                        NotificationHelper.CHANNEL_ID,
                        "CYCLE"
                    )
                }
            }
        }

        return NotificationHelper.showNotification(
            context = getApplication(),
            title = tuple.title,
            message = tuple.message,
            notificationId = tuple.id,
            channelId = tuple.channelId,
            navTarget = tuple.navTarget
        )
    }

    fun updateAverageCycleLength(newLength: Int) {
        prefs.edit().putInt("avg_cycle_length", newLength).apply()
        _avgCycleLength.value = newLength
    }

    fun correctPredictionWithActualStart(
        actualStartMs: Long,
        flowIntensity: String = "Medium",
        newAvgCycleDays: Int? = null
    ) {
        viewModelScope.launch {
            if (newAvgCycleDays != null && newAvgCycleDays > 0) {
                prefs.edit().putInt("avg_cycle_length", newAvgCycleDays).apply()
                _avgCycleLength.value = newAvgCycleDays
            }

            val dayMs = 86400000L
            val periodLength = _avgPeriodLength.value
            val endDate = actualStartMs + (periodLength * dayMs)

            repository.insertCycle(
                CycleEntity(
                    startDate = actualStartMs,
                    endDate = endDate,
                    flowIntensity = flowIntensity,
                    mood = "Normal",
                    symptomsJson = "[\"Prediction Correction\"]",
                    notes = "Prediction Correction: Period actually started on ${CycleUtils.formatDate(actualStartMs)}"
                )
            )

            firebaseAnalytics?.logEvent("correct_prediction", Bundle().apply {
                putLong("actual_start_ms", actualStartMs)
                if (newAvgCycleDays != null) putInt("new_avg_cycle_days", newAvgCycleDays)
            })
        }
    }

    fun setBlindPredictionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("blind_prediction_enabled", enabled).apply()
        _isBlindPredictionEnabled.value = enabled
        firebaseAnalytics?.logEvent("toggle_blind_prediction", Bundle().apply {
            putBoolean("enabled", enabled)
        })
    }

    fun getBlindedDataPayload(): com.teraxes.vital.domain.BlindedCycleData {
        return CycleUtils.extractBlindedCycleData(cycles.value, symptoms.value)
    }

    fun recalibrateBlindEngine(): com.teraxes.vital.domain.BlindCalibrationResult {
        val result = CycleUtils.recalibrateWithBlindData(
            cycles = cycles.value,
            symptoms = symptoms.value,
            currentAvgCycleLength = _avgCycleLength.value
        )
        if (result.calibratedAvgCycleLength != _avgCycleLength.value && result.calibratedAvgCycleLength > 0) {
            updateAverageCycleLength(result.calibratedAvgCycleLength)
        }
        firebaseAnalytics?.logEvent("recalibrate_blind_engine", Bundle().apply {
            putInt("accuracy_boost", result.accuracyBoostPercentage)
            putInt("blinded_metrics_count", result.blindedMetricsCount)
        })
        return result
    }
}

private data class PredictionReminderConfig(
    val preds: com.teraxes.vital.domain.CyclePredictions?,
    val cycleEnabled: Boolean,
    val days: Int,
    val fertileEnabled: Boolean,
    val discreet: Boolean
)

private data class NotificationTuple(
    val title: String,
    val message: String,
    val id: Int,
    val channelId: String,
    val navTarget: String
)
