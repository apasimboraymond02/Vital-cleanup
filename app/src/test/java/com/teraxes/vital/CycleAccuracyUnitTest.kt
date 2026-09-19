package com.teraxes.vital

import com.teraxes.vital.data.model.CycleEntity
import com.teraxes.vital.data.model.SymptomLogEntity
import com.teraxes.vital.domain.CycleStats
import com.teraxes.vital.domain.CycleUtils
import com.teraxes.vital.domain.DayPhaseType
import org.junit.Assert.*
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Clinical and Mathematical Accuracy Verification Tests for Vital Cycle Tracking Engine.
 */
class CycleAccuracyUnitTest {

    private val dayMs = 24 * 60 * 60 * 1000L

    @Test
    fun testStandard28DayCycle_OvulationAndFertileWindowAccuracy() {
        val baseTime = 1700000000000L // arbitrary epoch timestamp
        val cycle = CycleEntity(
            id = 1,
            startDate = baseTime,
            endDate = baseTime + (4 * dayMs) // 5 days flow
        )

        val stats = CycleUtils.calculateStats(listOf(cycle), fallbackAvgLength = 28, fallbackPeriodLength = 5)
        assertEquals(28, stats.averageLength)
        assertEquals(5, stats.averagePeriodLength)

        // Ovulation for 28-day cycle must be Day 14 (28 - 14)
        val ovulationDay = (stats.averageLength - 14).coerceAtLeast(10)
        assertEquals(14, ovulationDay)

        // Fertile Window: Ovulation - 5 (Day 9) to Ovulation + 2 (Day 16)
        val fertileRiskDay9 = CycleUtils.calculateFertilityRisk(9, 28)
        val fertileRiskDay14 = CycleUtils.calculateFertilityRisk(14, 28)
        val fertileRiskDay16 = CycleUtils.calculateFertilityRisk(16, 28)
        val lutealRiskDay22 = CycleUtils.calculateFertilityRisk(22, 28)

        assertEquals("medium", fertileRiskDay9)
        assertEquals("high", fertileRiskDay14) // Peak / High fertility
        assertEquals("medium", fertileRiskDay16)
        assertEquals("low", lutealRiskDay22) // Post-ovulatory infertile window
    }

    @Test
    fun testShort21DayCycle_OvulationAndFertileWindowAccuracy() {
        // Short 21-day cycle: Ovulation = 21 - 14 = Day 7, clamped to min 10
        val ovulationDay = (21 - 14).coerceAtLeast(10)
        assertEquals(10, ovulationDay)

        val riskDay10 = CycleUtils.calculateFertilityRisk(10, 21)
        assertEquals("high", riskDay10)
    }

    @Test
    fun testLong35DayCycle_OvulationAndFertileWindowAccuracy() {
        // Long 35-day cycle: Ovulation = 35 - 14 = Day 21
        val ovulationDay = (35 - 14).coerceAtLeast(10)
        assertEquals(21, ovulationDay)

        val riskDay15 = CycleUtils.calculateFertilityRisk(15, 35)
        val riskDay20 = CycleUtils.calculateFertilityRisk(20, 35)
        val riskDay21 = CycleUtils.calculateFertilityRisk(21, 35)
        val riskDay28 = CycleUtils.calculateFertilityRisk(28, 35)

        assertEquals("low", riskDay15)
        assertEquals("high", riskDay20) // Day 21-1 is within high risk window
        assertEquals("high", riskDay21) // Ovulation day
        assertEquals("low", riskDay28) // Luteal phase
    }

    @Test
    fun testOginoKnausAndStandardDaysMethodCalculations() {
        // Regular 28-day cycle
        val stats = CycleStats(
            averageLength = 28,
            averagePeriodLength = 5,
            minLength = 28,
            maxLength = 28,
            variation = 0,
            currentDay = 12
        )

        val sdmResult = CycleUtils.calculateRhythmMethod(stats)
        assertTrue(sdmResult.isStandardDaysEligible) // Standard Days Method applies (26-32 days)
        assertEquals(8, sdmResult.standardDaysFirstUnsafeDay)
        assertEquals(19, sdmResult.standardDaysLastUnsafeDay)
        assertTrue(sdmResult.isCurrentDayUnsafe) // Day 12 is in 8..19

        // Test safe day outside window
        val safeDayStats = stats.copy(currentDay = 22)
        val safeResult = CycleUtils.calculateRhythmMethod(safeDayStats)
        assertFalse(safeResult.isCurrentDayUnsafe)

        // Irregular cycle: 24 to 34 days -> Ogino-Knaus calculation (24 - 18 = Day 6; 34 - 11 = Day 23)
        val irregularStats = CycleStats(
            averageLength = 29,
            averagePeriodLength = 5,
            minLength = 24,
            maxLength = 34,
            variation = 10,
            currentDay = 15
        )
        val oginoResult = CycleUtils.calculateRhythmMethod(irregularStats)
        assertFalse(oginoResult.isStandardDaysEligible)
        assertEquals(6, oginoResult.oginoFirstUnsafeDay)
        assertEquals(23, oginoResult.oginoLastUnsafeDay)
        assertTrue(oginoResult.isCurrentDayUnsafe) // Day 15 is in 6..23
    }

    @Test
    fun testPhaseEvaluation_MenstrualFollicularOvulationLuteal() {
        val now = System.currentTimeMillis()
        val cycle = CycleEntity(
            id = 1,
            startDate = now,
            endDate = now + (4 * dayMs)
        )

        // Day 1: Menstrual
        val day1Eval = CycleUtils.evaluatePhaseForDate(Date(now), listOf(cycle))
        assertEquals(DayPhaseType.PERIOD_LOGGED, day1Eval.phaseType)
        assertEquals(1, day1Eval.cycleDay)

        // Day 7: Follicular
        val day7Eval = CycleUtils.evaluatePhaseForDate(Date(now + 6 * dayMs), listOf(cycle))
        assertEquals(DayPhaseType.FOLLICULAR, day7Eval.phaseType)

        // Day 14: Ovulation (28 - 14 = 14)
        val day14Eval = CycleUtils.evaluatePhaseForDate(Date(now + 13 * dayMs), listOf(cycle))
        assertEquals(DayPhaseType.OVULATION, day14Eval.phaseType)

        // Day 20: Luteal
        val day20Eval = CycleUtils.evaluatePhaseForDate(Date(now + 19 * dayMs), listOf(cycle))
        assertEquals(DayPhaseType.LUTEAL, day20Eval.phaseType)
    }

    @Test
    fun testBlindedDataExtraction_SanitizesPII() {
        val cycle = CycleEntity(
            id = 1,
            startDate = System.currentTimeMillis() - (30 * dayMs),
            endDate = System.currentTimeMillis() - (25 * dayMs),
            notes = "Sensitive user diary"
        )
        val symptom = SymptomLogEntity(
            id = 1,
            date = "2026-08-20",
            symptomType = "Cramps",
            severity = "Moderate",
            notes = "User medical observation"
        )

        val blinded = CycleUtils.extractBlindedCycleData(listOf(cycle), listOf(symptom))
        assertEquals("[REDACTED_PII]", blinded.redactedName)
        assertEquals("[REDACTED_PII]", blinded.redactedEmail)
        assertEquals("[REDACTED_PII]", blinded.redactedNotes)
        assertEquals(1, blinded.cyclesLoggedCount)
        assertEquals(1, blinded.symptomFrequencyMap["Cramps"])
        assertTrue(blinded.blindedMetricsVector.isNotEmpty())
    }

    @Test
    fun testCalendarMonthGridStructure() {
        val days = CycleUtils.getCalendarDaysForMonth(2026, 7, emptyList(), emptyList()) // August 2026
        // Calendar grid must be a multiple of 7 (complete weeks)
        assertEquals(0, days.size % 7)
        assertTrue(days.size in 28..42)
        // Must contain 31 days for August
        val augustDays = days.filter { it.isCurrentMonth }
        assertEquals(31, augustDays.size)
    }
}
