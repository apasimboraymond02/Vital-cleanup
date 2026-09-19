import _ from 'lodash';
import moment from 'moment';

interface Cycle {
  startDate: string;
  length: number;
}

interface Symptom {
  date: string;
  type: string;
  severity: 'Low' | 'Medium' | 'High';
}

export const CorrelationService = {
  /**
   * Analyzes symptoms to find correlations with cycle phases
   */
  analyzeSymptomPatterns: (cycles: Cycle[], symptoms: Symptom[]) => {
    const insights: string[] = [];

    // Group symptoms by type
    const symptomsByType = _.groupBy(symptoms, 'type');

    Object.keys(symptomsByType).forEach(type => {
      const occurrences = symptomsByType[type];
      
      // We need a minimum amount of data to make a correlation
      if (occurrences.length < 3) return;

      // Track which phase the symptom appears in
      const phaseCounts = {
        menstrual: 0, // Days 1-5
        follicular: 0, // Days 6-14
        luteal: 0, // Days 15-28+
      };

      occurrences.forEach(symptom => {
        // Find which cycle this symptom falls into
        const cycle = cycles.find(c => {
          const start = moment(c.startDate);
          const end = moment(c.startDate).add(c.length, 'days');
          return moment(symptom.date).isBetween(start, end, 'day', '[]');
        });

        if (cycle) {
          const dayOfCycle = moment(symptom.date).diff(moment(cycle.startDate), 'days') + 1;
          
          if (dayOfCycle <= 5) phaseCounts.menstrual++;
          else if (dayOfCycle <= 14) phaseCounts.follicular++;
          else phaseCounts.luteal++;
        }
      });

      // Simple heuristic: if > 60% of occurrences happen in one phase
      const total = occurrences.length;
      if (phaseCounts.menstrual / total > 0.6) {
        insights.push(`Your ${type} is strongly linked to your menstrual phase.`);
      } else if (phaseCounts.luteal / total > 0.6) {
        insights.push(`You tend to experience ${type} during your luteal phase (PMS).`);
      }
    });

    return insights;
  }
};