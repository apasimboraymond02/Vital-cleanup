package com.teraxes.vital.domain

import com.teraxes.vital.data.model.CycleEntity
import com.teraxes.vital.data.model.SymptomLogEntity
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class DayPhaseType {
    PERIOD_LOGGED,
    PERIOD_PREDICTED,
    FOLLICULAR,
    FERTILE,
    OVULATION,
    LUTEAL,
    NONE
}

data class CycleStats(
    val averageLength: Int = 28,
    val averagePeriodLength: Int = 5,
    val minLength: Int = 21,
    val maxLength: Int = 35,
    val variation: Int = 0,
    val currentDay: Int = 1,
    val currentPhase: String = "unknown",
    val regularity: String = "Regular"
)

data class CyclePredictions(
    val nextPeriodDate: Long,
    val ovulationDate: Long,
    val fertileWindowStart: Long,
    val fertileWindowEnd: Long
)

data class RhythmMethodResult(
    val isStandardDaysEligible: Boolean,
    val standardDaysFirstUnsafeDay: Int,
    val standardDaysLastUnsafeDay: Int,
    val oginoFirstUnsafeDay: Int,
    val oginoLastUnsafeDay: Int,
    val isCurrentDayUnsafe: Boolean,
    val guidanceNote: String,
    val methodReliabilityRating: String
)

data class PhaseEvaluation(
    val phaseType: DayPhaseType,
    val cycleDay: Int,
    val phaseName: String
)

data class BlindedCycleData(
    val redactedName: String,
    val redactedEmail: String,
    val redactedNotes: String,
    val cyclesLoggedCount: Int,
    val symptomFrequencyMap: Map<String, Int>,
    val blindedMetricsVector: List<Double>
)

data class BlindCalibrationResult(
    val calibratedAvgCycleLength: Int,
    val accuracyBoostPercentage: Int,
    val blindedMetricsCount: Int
)

data class CalendarDay(
    val date: Date,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val cycleDay: Int? = null,
    val phaseType: DayPhaseType? = null
)

data class DailyReportInfo(
    val dateFormatted: String,
    val cycleDay: Int,
    val phaseName: String,
    val phaseType: DayPhaseType,
    val sexPregnancyRiskLevel: String,
    val sexPregnancyAnswer: String,
    val sexPregnancyExplanation: String,
    val contraceptionTip: String,
    val whatToExpectBody: String,
    val whatToExpectMood: String,
    val whatToExpectHormones: String,
    val recommendedNutrition: String,
    val recommendedMovement: String
)

data class PredictionAccuracy(
    val isHighAccuracy: Boolean,
    val title: String,
    val description: String
)

object CycleUtils {
    private const val DAY_IN_MS = 86400000L

    fun getCycleLength(cycle: CycleEntity): Int {
        val end = cycle.endDate ?: System.currentTimeMillis()
        val diff = end - cycle.startDate
        return (diff / DAY_IN_MS).toInt() + 1
    }

    fun calculateStats(cycles: List<CycleEntity>, fallbackAvgLength: Int = 28, fallbackPeriodLength: Int = 5): CycleStats {
        if (cycles.isEmpty()) {
            return CycleStats(fallbackAvgLength, fallbackPeriodLength, 21, 35, 0, 1)
        }
        val lengths = cycles.filter { it.endDate != null }.map { getCycleLength(it) }
        val avgLength = if (lengths.isNotEmpty()) lengths.average().toInt() else fallbackAvgLength
        val minLength = lengths.minOrNull() ?: (avgLength - 2)
        val maxLength = lengths.maxOrNull() ?: (avgLength + 2)
        val variation = maxLength - minLength
        
        val lastCycle = cycles.firstOrNull() ?: return CycleStats(fallbackAvgLength, fallbackPeriodLength, 21, 35, 0, 1)
        val currentDay = ((System.currentTimeMillis() - lastCycle.startDate) / DAY_IN_MS).toInt() + 1
        
        var regularity = "Regular"
        if (variation > 7) regularity = "Irregular"
        else if (variation > 3) regularity = "Slightly Irregular"

        return CycleStats(
            averageLength = avgLength,
            averagePeriodLength = fallbackPeriodLength,
            minLength = minLength,
            maxLength = maxLength,
            variation = variation,
            currentDay = currentDay,
            currentPhase = getCyclePhase(currentDay, avgLength),
            regularity = regularity
        )
    }

    fun predictNextCycle(cycles: List<CycleEntity>, fallbackAvgLength: Int = 28): CyclePredictions? {
        if (cycles.isEmpty()) return null
        val lastStart = cycles.first().startDate
        val nextPeriod = lastStart + (fallbackAvgLength * DAY_IN_MS)
        val ovulation = nextPeriod - (14 * DAY_IN_MS)
        return CyclePredictions(
            nextPeriodDate = nextPeriod,
            ovulationDate = ovulation,
            fertileWindowStart = ovulation - (5 * DAY_IN_MS),
            fertileWindowEnd = ovulation + (2 * DAY_IN_MS)
        )
    }

    fun calculateFertilityRisk(cycleDay: Int, cycleLength: Int = 28): String {
        val ovulationDay = cycleLength - 14
        return when {
            cycleDay in (ovulationDay - 5)..(ovulationDay + 2) -> if (cycleDay in (ovulationDay - 1)..(ovulationDay + 1)) "high" else "medium"
            else -> "low"
        }
    }

    fun getCyclePhase(cycleDay: Int, cycleLength: Int = 28): String {
        return when {
            cycleDay <= 5 -> "menstrual"
            cycleDay <= (cycleLength - 15) -> "follicular"
            cycleDay <= (cycleLength - 13) -> "ovulation"
            cycleDay <= cycleLength -> "luteal"
            else -> "unknown"
        }
    }

    fun evaluatePhaseForDate(date: Date, cycles: List<CycleEntity>, avgLength: Int = 28): PhaseEvaluation {
        val matchedCycle = cycles.find { it.startDate <= date.time && (it.endDate ?: (it.startDate + 35 * DAY_IN_MS)) >= date.time }
        if (matchedCycle != null) {
            val day = ((date.time - matchedCycle.startDate) / DAY_IN_MS).toInt() + 1
            val phase = getCyclePhase(day, avgLength)
            val type = when (phase) {
                "menstrual" -> DayPhaseType.PERIOD_LOGGED
                "follicular" -> DayPhaseType.FOLLICULAR
                "ovulation" -> DayPhaseType.OVULATION
                else -> DayPhaseType.LUTEAL
            }
            return PhaseEvaluation(type, day, phase.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
        }
        return PhaseEvaluation(DayPhaseType.NONE, 1, "Unknown")
    }

    fun calculateRhythmMethod(stats: CycleStats, shortestOverride: Int? = null, longestOverride: Int? = null): RhythmMethodResult {
        val minLen = shortestOverride ?: stats.minLength
        val maxLen = longestOverride ?: stats.maxLength
        
        val isSdm = minLen >= 26 && maxLen <= 32
        val sdmStart = 8
        val sdmEnd = 19
        val oginoStart = minLen - 18
        val oginoEnd = maxLen - 11
        val isUnsafe = if (isSdm) stats.currentDay in sdmStart..sdmEnd else stats.currentDay in oginoStart..oginoEnd
        
        return RhythmMethodResult(
            isStandardDaysEligible = isSdm,
            standardDaysFirstUnsafeDay = sdmStart,
            standardDaysLastUnsafeDay = sdmEnd,
            oginoFirstUnsafeDay = oginoStart,
            oginoLastUnsafeDay = oginoEnd,
            isCurrentDayUnsafe = isUnsafe,
            guidanceNote = if (isUnsafe) "High risk of pregnancy today." else "Lower risk of pregnancy today.",
            methodReliabilityRating = if (isSdm) "High (Standard Days Method)" else "Moderate (Ogino-Knaus)"
        )
    }

    fun extractBlindedCycleData(cycles: List<CycleEntity>, symptoms: List<SymptomLogEntity>): BlindedCycleData {
        return BlindedCycleData(
            redactedName = "[REDACTED_PII]",
            redactedEmail = "[REDACTED_PII]",
            redactedNotes = "[REDACTED_PII]",
            cyclesLoggedCount = cycles.size,
            symptomFrequencyMap = symptoms.groupBy { it.symptomType }.mapValues { it.value.size },
            blindedMetricsVector = listOf(cycles.size.toDouble(), symptoms.size.toDouble())
        )
    }

    fun recalibrateWithBlindData(cycles: List<CycleEntity>, symptoms: List<SymptomLogEntity>, currentAvgCycleLength: Int): BlindCalibrationResult {
        return BlindCalibrationResult(currentAvgCycleLength, 0, 0)
    }

    fun getCalendarDaysForMonth(year: Int, month: Int, cycles: List<CycleEntity>, symptoms: List<SymptomLogEntity>): List<CalendarDay> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        val days = mutableListOf<CalendarDay>()
        for (i in 1..42) {
            calendar.set(Calendar.DAY_OF_MONTH, i)
            days.add(CalendarDay(calendar.time, calendar.get(Calendar.MONTH) == month, false))
            if (i > 28 && calendar.get(Calendar.MONTH) != month) break
        }
        return days
    }

    fun formatDate(ms: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))
    fun formatDateShort(ms: Long): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))

    fun getPredictionAccuracy(cycles: List<CycleEntity>): PredictionAccuracy {
        val isHigh = cycles.size >= 3
        return PredictionAccuracy(
            isHighAccuracy = isHigh,
            title = if (isHigh) "High Prediction Accuracy" else "Baseline Accuracy",
            description = if (isHigh) "Based on your logged cycles." else "Log more cycles to improve accuracy."
        )
    }

    fun utcMillisToLocalMidnight(ms: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getDailyReport(currentDay: Int, avgLength: Int, currentPhase: String): DailyReportInfo {
        val risk = calculateFertilityRisk(currentDay, avgLength)
        val phaseType = when (currentPhase.lowercase(Locale.getDefault())) {
            "menstrual" -> DayPhaseType.PERIOD_LOGGED
            "follicular" -> DayPhaseType.FOLLICULAR
            "ovulation" -> DayPhaseType.OVULATION
            "luteal" -> DayPhaseType.LUTEAL
            else -> DayPhaseType.NONE
        }

        return DailyReportInfo(
            dateFormatted = formatDate(System.currentTimeMillis()),
            cycleDay = currentDay,
            phaseName = currentPhase.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            phaseType = phaseType,
            sexPregnancyRiskLevel = risk.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            sexPregnancyAnswer = if (risk == "low") "Low Probability" else "High Probability",
            sexPregnancyExplanation = "Based on your cycle data...",
            contraceptionTip = "Use protection if not planning pregnancy.",
            whatToExpectBody = "Body changes expected...",
            whatToExpectMood = "Mood changes expected...",
            whatToExpectHormones = "Hormone changes expected...",
            recommendedNutrition = "Eat well...",
            recommendedMovement = "Stay active..."
        )
    }

    fun formatRelativeDate(date: Long): String {
        val diff = ((date - System.currentTimeMillis()) / DAY_IN_MS).toInt()
        return when {
            diff == 0 -> "Today"
            diff == 1 -> "Tomorrow"
            diff == -1 -> "Yesterday"
            diff > 0 -> "In $diff days"
            else -> "${Math.abs(diff)} days ago"
        }
    }
}
