import { Cycle } from '../../types';

export const cycleUtils = {
  getCycleLength(cycle: Cycle): number {
    const end = cycle.endDate || new Date();
    const diff = end.getTime() - cycle.startDate.getTime();
    return Math.floor(diff / (1000 * 60 * 60 * 24)) + 1;
  },

  calculateCycleStats(cycles: Cycle[]) {
    if (cycles.length === 0) {
      return {
        averageLength: 28,
        averagePeriodLength: 5,
        minLength: 21,
        maxLength: 35,
        regularity: 'Insufficient Data',
        variation: 0,
      };
    }

    const lengths = cycles.map(cycle => this.getCycleLength(cycle));
    const periodLengths = cycles.map(cycle => {
      if (cycle.endDate) {
        const diff = cycle.endDate.getTime() - cycle.startDate.getTime();
        return Math.floor(diff / (1000 * 60 * 60 * 24)) + 1;
      }
      return 5; // Default assumption
    });

    const averageLength = Math.round(
      lengths.reduce((a, b) => a + b, 0) / lengths.length
    );
    
    const averagePeriodLength = Math.round(
      periodLengths.reduce((a, b) => a + b, 0) / periodLengths.length
    );

    const minLength = Math.min(...lengths);
    const maxLength = Math.max(...lengths);
    const variation = maxLength - minLength;

    let regularity = 'Regular';
    if (variation > 7) regularity = 'Irregular';
    else if (variation > 3) regularity = 'Slightly Irregular';

    return {
      averageLength,
      averagePeriodLength,
      minLength,
      maxLength,
      regularity,
      variation,
    };
  },

  predictNextCycle(cycles: Cycle[]) {
    if (cycles.length < 3) {
      return null;
    }

    // Use last 6 cycles for prediction
    const recentCycles = cycles.slice(0, 6);
    const lengths = recentCycles.map(cycle => this.getCycleLength(cycle));
    const avgLength = lengths.reduce((a, b) => a + b, 0) / lengths.length;

    const lastCycle = recentCycles[0];
    const lastStart = lastCycle.startDate;
    
    // Predict next period start date
    const nextPeriod = new Date(lastStart);
    nextPeriod.setDate(nextPeriod.getDate() + avgLength);

    // Predict ovulation (14 days before next period)
    const ovulation = new Date(nextPeriod);
    ovulation.setDate(ovulation.getDate() - 14);

    // Fertile window (ovulation ± 3 days)
    const fertileWindowStart = new Date(ovulation);
    fertileWindowStart.setDate(fertileWindowStart.getDate() - 3);
    
    const fertileWindowEnd = new Date(ovulation);
    fertileWindowEnd.setDate(fertileWindowEnd.getDate() + 3);

    return {
      nextPeriod: nextPeriod.toISOString(),
      ovulation: ovulation.toISOString(),
      fertileWindow: {
        start: fertileWindowStart.toISOString(),
        end: fertileWindowEnd.toISOString(),
      },
    };
  },

  calculateFertilityRisk(cycleDay: number, cycleLength: number = 28): 'low' | 'medium' | 'high' {
    const ovulationDay = 14; // Simplified calculation
    
    if (cycleDay < ovulationDay - 5 || cycleDay > ovulationDay + 2) {
      return 'low';
    } else if (cycleDay >= ovulationDay - 2 && cycleDay <= ovulationDay + 1) {
      return 'high';
    } else {
      return 'medium';
    }
  },

  getCyclePhase(cycleDay: number, cycleLength: number = 28) {
    if (cycleDay <= 5) return 'menstrual';
    if (cycleDay <= 13) return 'follicular';
    if (cycleDay === 14) return 'ovulation';
    if (cycleDay <= 28) return 'luteal';
    return 'unknown';
  },

  formatDate(date: Date): string {
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  },

  formatRelativeDate(date: Date): string {
    const now = new Date();
    const diff = Math.floor((date.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    
    if (diff === 0) return 'Today';
    if (diff === 1) return 'Tomorrow';
    if (diff === -1) return 'Yesterday';
    if (diff > 0) return `In ${diff} days`;
    if (diff < 0) return `${Math.abs(diff)} days ago`;
    return 'Today';
  },
};