import React, { useEffect, useState } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useSelector, useDispatch } from 'react-redux';
import { VictoryChart, VictoryLine, VictoryAxis, VictoryTheme } from 'victory-native';
import { Calendar, Plus, TrendingUp, AlertCircle } from 'lucide-react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useDatabase } from '../../database';
import { useTheme } from '../../theme/ThemeContext';
import { RootStackParamList } from '../../navigation/RootNavigator';
import { Cycle, Symptom } from '../../types';
import { cycleUtils } from './cycleUtils';

type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

export const CycleScreen = () => {
  const theme = useTheme();
  const navigation = useNavigation<NavigationProp>();
  const dispatch = useDispatch();
  const { db } = useDatabase();
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [currentCycle, setCurrentCycle] = useState<Cycle | null>(null);
  const [predictions, setPredictions] = useState<{
    nextPeriod: string;
    fertileWindow: { start: string; end: string };
    ovulation: string;
  } | null>(null);

  useEffect(() => {
    loadCycles();
  }, []);

  const loadCycles = async () => {
    if (!db) return;

    try {
      db.transaction(tx => {
        tx.executeSql(
          `SELECT * FROM cycles 
           WHERE is_deleted = 0 
           ORDER BY start_date DESC 
           LIMIT 12`,
          [],
          (_, { rows }) => {
            const loadedCycles = rows._array.map(row => ({
              id: row.id,
              startDate: new Date(row.start_date),
              endDate: row.end_date ? new Date(row.end_date) : undefined,
              flowIntensity: row.flow_intensity,
              mood: row.mood,
              notes: row.notes,
              symptoms: row.symptoms ? JSON.parse(row.symptoms) : [],
            }));
            setCycles(loadedCycles);
            
            // Find current cycle
            const current = loadedCycles.find(cycle => 
              !cycle.endDate || cycle.endDate > new Date()
            );
            setCurrentCycle(current || null);

            // Generate predictions
            if (loadedCycles.length >= 3) {
              const preds = cycleUtils.predictNextCycle(loadedCycles);
              setPredictions(preds);
            }
          }
        );
      });
    } catch (error) {
      console.error('Error loading cycles:', error);
    }
  };

  const handleAddPeriod = () => {
    navigation.navigate('CycleForm');
  };

  const handleLogSymptoms = (date: string) => {
    navigation.navigate('SymptomTracker', { date });
  };

  const renderCycleStats = () => {
    if (cycles.length === 0) {
      return (
        <Card style={styles.emptyState}>
          <Calendar size={48} color={theme.colors.gray} />
          <Text style={styles.emptyStateTitle}>Start Tracking Your Cycle</Text>
          <Text style={styles.emptyStateText}>
            Log your first period to begin getting personalized insights and predictions.
          </Text>
          <Button
            title="Log First Period"
            onPress={handleAddPeriod}
            style={styles.addButton}
          />
        </Card>
      );
    }

    const stats = cycleUtils.calculateCycleStats(cycles);

    return (
      <View style={styles.statsContainer}>
        <Card style={styles.statCard}>
          <Text style={styles.statLabel}>Average Cycle Length</Text>
          <Text style={styles.statValue}>{stats.averageLength} days</Text>
          <Text style={styles.statSubtext}>
            Range: {stats.minLength} - {stats.maxLength} days
          </Text>
        </Card>

        <Card style={styles.statCard}>
          <Text style={styles.statLabel}>Average Period Length</Text>
          <Text style={styles.statValue}>{stats.averagePeriodLength} days</Text>
        </Card>

        <Card style={styles.statCard}>
          <Text style={styles.statLabel}>Regularity</Text>
          <Text style={[
            styles.statValue,
            { color: stats.regularity === 'Regular' ? theme.colors.success : theme.colors.warning }
          ]}>
            {stats.regularity}
          </Text>
          <Text style={styles.statSubtext}>
            Variation: {stats.variation} days
          </Text>
        </Card>
      </View>
    );
  };

  const renderPredictions = () => {
    if (!predictions) return null;

    return (
      <Card style={styles.predictionsCard}>
        <View style={styles.cardHeader}>
          <TrendingUp size={20} color={theme.colors.primary} />
          <Text style={styles.cardTitle}>Predictions</Text>
        </View>
        
        <View style={styles.predictionItem}>
          <Text style={styles.predictionLabel}>Next Period</Text>
          <Text style={styles.predictionValue}>
            {new Date(predictions.nextPeriod).toLocaleDateString()}
          </Text>
        </View>

        <View style={styles.predictionItem}>
          <Text style={styles.predictionLabel}>Fertile Window</Text>
          <Text style={styles.predictionValue}>
            {new Date(predictions.fertileWindow.start).toLocaleDateString()} - 
            {new Date(predictions.fertileWindow.end).toLocaleDateString()}
          </Text>
        </View>

        <View style={styles.predictionItem}>
          <Text style={styles.predictionLabel}>Ovulation</Text>
          <Text style={styles.predictionValue}>
            {new Date(predictions.ovulation).toLocaleDateString()}
          </Text>
        </View>

        <View style={styles.disclaimer}>
          <AlertCircle size={14} color={theme.colors.warning} />
          <Text style={styles.disclaimerText}>
            Predictions are estimates based on your logged data. Not a substitute for medical advice.
          </Text>
        </View>
      </Card>
    );
  };

  const renderCycleChart = () => {
    if (cycles.length < 2) return null;

    const chartData = cycles.slice(0, 6).map((cycle, index) => ({
      x: index + 1,
      y: cycleUtils.getCycleLength(cycle),
    })).reverse();

    return (
      <Card style={styles.chartCard}>
        <Text style={styles.cardTitle}>Cycle Length History</Text>
        <VictoryChart
          theme={VictoryTheme.material}
          height={200}
          padding={{ top: 20, bottom: 40, left: 40, right: 20 }}
        >
          <VictoryAxis
            tickFormat={(x) => `Cycle ${x}`}
            style={{
              axis: { stroke: theme.colors.text },
              tickLabels: { fill: theme.colors.text },
            }}
          />
          <VictoryAxis
            dependentAxis
            tickFormat={(y) => `${y}d`}
            style={{
              axis: { stroke: theme.colors.text },
              tickLabels: { fill: theme.colors.text },
            }}
          />
          <VictoryLine
            data={chartData}
            style={{
              data: { stroke: theme.colors.primary, strokeWidth: 2 },
            }}
            interpolation="natural"
          />
        </VictoryChart>
      </Card>
    );
  };

  return (
    <ScrollView 
      style={[styles.container, { backgroundColor: theme.colors.background }]}
      contentContainerStyle={styles.content}
    >
      <View style={styles.header}>
        <Text style={styles.title}>Cycle Tracker</Text>
        <TouchableOpacity onPress={handleAddPeriod} style={styles.addButton}>
          <Plus size={24} color={theme.colors.primary} />
        </TouchableOpacity>
      </View>

      {renderCycleStats()}
      {renderPredictions()}
      {renderCycleChart()}

      <Card style={styles.quickActions}>
        <Text style={styles.cardTitle}>Quick Actions</Text>
        <View style={styles.actionButtons}>
          <Button
            title="Log Period"
            onPress={handleAddPeriod}
            variant="outline"
            style={styles.actionButton}
          />
          <Button
            title="Log Symptoms"
            onPress={() => handleLogSymptoms(new Date().toISOString().split('T')[0])}
            variant="outline"
            style={styles.actionButton}
          />
          <Button
            title="View History"
            onPress={() => {/* Navigate to history */}}
            variant="outline"
            style={styles.actionButton}
          />
        </View>
      </Card>

      {currentCycle && (
        <Card style={styles.currentCycle}>
          <Text style={styles.cardTitle}>Current Cycle</Text>
          <View style={styles.cycleInfo}>
            <Text style={styles.cycleDate}>
              Started: {currentCycle.startDate.toLocaleDateString()}
            </Text>
            {currentCycle.endDate ? (
              <Text style={styles.cycleDate}>
                Ended: {currentCycle.endDate.toLocaleDateString()}
              </Text>
            ) : (
              <Text style={styles.cycleDuration}>
                Day {Math.floor((Date.now() - currentCycle.startDate.getTime()) / (1000 * 60 * 60 * 24)) + 1}
              </Text>
            )}
          </View>
        </Card>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: 16,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
  },
  addButton: {
    padding: 8,
  },
  emptyState: {
    alignItems: 'center',
    padding: 32,
    marginBottom: 24,
  },
  emptyStateTitle: {
    fontSize: 20,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 8,
  },
  emptyStateText: {
    textAlign: 'center',
    marginBottom: 24,
    lineHeight: 20,
  },
  statsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  statCard: {
    flex: 1,
    marginHorizontal: 4,
    padding: 12,
    alignItems: 'center',
  },
  statLabel: {
    fontSize: 12,
    opacity: 0.7,
    marginBottom: 4,
  },
  statValue: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  statSubtext: {
    fontSize: 10,
    opacity: 0.5,
    marginTop: 2,
  },
  predictionsCard: {
    marginBottom: 16,
    padding: 16,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginLeft: 8,
  },
  predictionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.1)',
  },
  predictionLabel: {
    fontSize: 14,
    opacity: 0.7,
  },
  predictionValue: {
    fontSize: 14,
    fontWeight: '600',
  },
  disclaimer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 16,
    padding: 12,
    borderRadius: 8,
    backgroundColor: 'rgba(255,193,7,0.1)',
  },
  disclaimerText: {
    fontSize: 12,
    marginLeft: 8,
    flex: 1,
  },
  chartCard: {
    marginBottom: 16,
    padding: 16,
  },
  quickActions: {
    marginBottom: 16,
    padding: 16,
  },
  actionButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 12,
  },
  actionButton: {
    flex: 1,
    marginHorizontal: 4,
  },
  currentCycle: {
    padding: 16,
  },
  cycleInfo: {
    marginTop: 12,
  },
  cycleDate: {
    fontSize: 14,
    marginBottom: 4,
  },
  cycleDuration: {
    fontSize: 16,
    fontWeight: '600',
    color: '#4CAF50',
  },
});