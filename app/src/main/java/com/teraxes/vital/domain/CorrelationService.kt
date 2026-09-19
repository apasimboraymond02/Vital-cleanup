package com.teraxes.vital.domain

import com.teraxes.vital.data.model.CycleEntity
import com.teraxes.vital.data.model.SymptomLogEntity
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object CorrelationService {

    /**
     * Analyzes logged symptoms against recorded cycles to find phase correlations
     */
    fun analyzeSymptomPatterns(
        cycles: List<CycleEntity>,
        symptoms: List<SymptomLogEntity>
    ): List<String> {
        val insights = mutableListOf<String>()
        if (symptoms.isEmpty()) {
            insights.add("Log more symptoms daily to receive personalized pattern insights.")
            return insights
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val symptomsByType = symptoms.groupBy { it.symptomType }

        symptomsByType.forEach { (type, occurrences) ->
            if (occurrences.size < 2) return@forEach

            var menstrualCount = 0
            var follicularCount = 0
            var lutealCount = 0
            var matched = 0

            occurrences.forEach { symptom ->
                val symptomDateMs = try {
                    dateFormat.parse(symptom.date)?.time
                } catch (e: Exception) {
                    null
                } ?: return@forEach

                // Find matching cycle
                val matchedCycle = cycles.find { cycle ->
                    val cycleEnd = cycle.endDate ?: (cycle.startDate + TimeUnit.DAYS.toMillis(35))
                    symptomDateMs in cycle.startDate..cycleEnd
                }

                if (matchedCycle != null) {
                    val dayOfCycle = (TimeUnit.MILLISECONDS.toDays(symptomDateMs - matchedCycle.startDate) + 1).toInt()
                    val phase = CycleUtils.getCyclePhase(dayOfCycle, CycleUtils.getCycleLength(matchedCycle))
                    when (phase) {
                        "menstrual" -> menstrualCount++
                        "follicular" -> follicularCount++
                        "ovulation", "luteal" -> lutealCount++
                    }
                    matched++
                }
            }

            if (matched > 0) {
                val total = matched.toDouble()
                if (menstrualCount / total >= 0.5) {
                    insights.add("Your $type is strongly linked to your menstrual phase.")
                } else if (lutealCount / total >= 0.5) {
                    insights.add("You tend to experience $type during your luteal phase (PMS).")
                } else if (follicularCount / total >= 0.5) {
                    insights.add("Your $type frequently occurs during your follicular phase.")
                }
            }
        }

        if (insights.isEmpty()) {
            insights.add("Keep logging symptoms over multiple cycles to uncover patterns!")
        }

        return insights
    }
}
