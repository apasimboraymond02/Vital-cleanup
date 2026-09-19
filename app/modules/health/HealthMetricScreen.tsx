import React, { useState, useEffect } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Alert,
} from 'react-native';
import {
  Thermometer,
  Weight,
  Activity,
  Heart,
  Brain,
  Plus,
  Trash2,
  Save,
  Calendar,
  TrendingUp,
} from 'lucide-react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import { VictoryChart, VictoryLine, VictoryAxis, VictoryTheme } from 'victory-native';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useDatabase } from '../../database';
import { useTheme } from '../../theme/ThemeContext';

type MetricType = 'temperature' | 'weight' | 'blood_pressure' | 'blood_sugar' | 'mood' | 'other';

interface HealthMetric {
  id?: string;
  type: MetricType;
  value: number;
  unit: string;
  loggedDate: Date;
  notes?: string;
}

export const HealthMetricsScreen = () => {
  const theme = useTheme();
  const { db } = useDatabase();
  
  const [metrics, setMetrics] = useState<HealthMetric[]>([]);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [selectedType, setSelectedType] = useState<MetricType>('temperature');
  const [value, setValue] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);

  const metricTypes = [
    { type: 'temperature', label: 'Temperature', icon: Thermometer, unit: '°C' },
    { type: 'weight', label: 'Weight', icon: Weight, unit: 'kg' },
    { type: 'blood_pressure', label: 'Blood Pressure', icon: Activity, unit: 'mmHg' },
    { type: 'blood_sugar', label: 'Blood Sugar', icon: Heart, unit: 'mg/dL' },
    { type: 'mood', label: 'Mood', icon: Brain, unit: '1-10' },
  ];

  useEffect(() => {
    loadMetrics();
  }, []);

  const loadMetrics = async () => {
    if (!db) return;

    try {
      db.transaction(tx => {
        tx.executeSql(
          `SELECT * FROM health_metrics 
           ORDER BY logged_date DESC 
           LIMIT 50`,
          [],
          (_, { rows }) => {
            const loadedMetrics = rows._array.map(row => ({
              id: row.id.toString(),
              type: row.type as MetricType,
              value: row.value,
              unit: row.unit,
              loggedDate: new Date(row.logged_date),
              notes: row.notes,
            }));
            setMetrics(loadedMetrics);
          }
        );
      });
    } catch (error) {
      console.error('Error loading metrics:', error);
    }
  };

  const handleAddMetric = async () => {
    if (!value.trim() || !db) {
      Alert.alert('Error', 'Please enter a value');
      return;
    }

    const numericValue = parseFloat(value);
    if (isNaN(numericValue)) {
      Alert.alert('Error', 'Please enter a valid number');
      return;
    }

    setLoading(true);
    try {
      const metric: Omit<HealthMetric, 'id'> = {
        type: selectedType,
        value: numericValue,
        unit: metricTypes.find(m => m.type === selectedType)?.unit || '',
        loggedDate: selectedDate,
        notes: notes.trim(),
      };

      await new Promise<void>((resolve, reject) => {
        db.transaction(tx => {
          tx.executeSql(
            `INSERT INTO health_metrics (type, value, unit, logged_date, notes)
             VALUES (?, ?, ?, ?, ?)`,
            [
              metric.type,
              metric.value,
              metric.unit,
              metric.loggedDate.toISOString().split('T')[0],
              metric.notes || '',
            ],
            () => resolve(),
            (_, error) => {
              reject(error);
              return false;
            }
          );
        });
      });

      // Reset form
      setValue('');
      setNotes('');
      
      // Reload metrics
      await loadMetrics();
      
      Alert.alert('Success', 'Metric logged successfully');
    } catch (error) {
      console.error('Error adding metric:', error);
      Alert.alert('Error', 'Failed to log metric');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteMetric = async (metricId: string) => {
    if (!db) return;

    Alert.alert(
      'Delete Metric',
      'Are you sure you want to delete this metric?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await new Promise<void>((resolve, reject) => {
                db.transaction(tx => {
                  tx.executeSql(
                    'DELETE FROM health_metrics WHERE id = ?',
                    [metricId],
                    () => resolve(),
                    (_, error) => {
                      reject(error);
                      return false;
                    }
                  );
                });
              });

              // Remove from local state
              setMetrics(metrics.filter(m => m.id !== metricId));
              Alert.alert('Success', 'Metric deleted');
            } catch (error) {
              console.error('Error deleting metric:', error);
              Alert.alert('Error', 'Failed to delete metric');
            }
          },
        },
      ]
    );
  };

  const renderMetricForm = () => {
    const selectedMetric = metricTypes.find(m => m.type === selectedType);
    const Icon = selectedMetric?.icon || Thermometer;

    return (
      <Card style={styles.formCard}>
        <Text style={styles.sectionTitle}>Log New Metric</Text>
        
        <View style={styles.metricTypeSelector}>
          {metricTypes.map((metricType) => {
            const Icon = metricType.icon;
            const isSelected = selectedType === metricType.type;
            
            return (
              <TouchableOpacity
                key={metricType.type}
                style={[
                  styles.metricTypeButton,
                  isSelected && styles.metricTypeSelected,
                  isSelected && { backgroundColor: theme.colors.primary + '20' },
                ]}
                onPress={() => setSelectedType(metricType.type)}
              >
                <Icon 
                  size={20} 
                  color={isSelected ? theme.colors.primary : theme.colors.textSecondary} 
                />
                <Text style={[
                  styles.metricTypeLabel,
                  isSelected && { color: theme.colors.primary },
                ]}>
                  {metricType.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        <View style={styles.formRow}>
          <View style={styles.dateInput}>
            <TouchableOpacity
              style={styles.dateButton}
              onPress={() => setShowDatePicker(true)}
            >
              <Calendar size={20} color={theme.colors.primary} />
              <Text style={styles.dateText}>
                {selectedDate.toLocaleDateString()}
              </Text>
            </TouchableOpacity>
          </View>

          <View style={styles.valueInput}>
            <TextInput
              style={[styles.input, { flex: 1 }]}
              placeholder="Value"
              placeholderTextColor={theme.colors.textSecondary}
              value={value}
              onChangeText={setValue}
              keyboardType="decimal-pad"
            />
            <Text style={styles.unitText}>{selectedMetric?.unit}</Text>
          </View>
        </View>

        {showDatePicker && (
          <DateTimePicker
            value={selectedDate}
            mode="date"
            display="default"
            onChange={(event, date) => {
              setShowDatePicker(false);
              if (date) {
                setSelectedDate(date);
              }
            }}
          />
        )}

        <TextInput
          style={[styles.notesInput, { borderColor: theme.colors.border }]}
          placeholder="Notes (optional)"
          placeholderTextColor={theme.colors.textSecondary}
          value={notes}
          onChangeText={setNotes}
          multiline
          numberOfLines={3}
        />

        <Button
          title="Add Metric"
          onPress={handleAddMetric}
          loading={loading}
          leftIcon={<Plus size={20} />}
          style={styles.addButton}
        />
      </Card>
    );
  };

  const renderMetricsChart = () => {
    const temperatureMetrics = metrics
      .filter(m => m.type === 'temperature')
      .slice(0, 10)
      .reverse();

    if (temperatureMetrics.length < 2) return null;

    const chartData = temperatureMetrics.map((metric, index) => ({
      x: index,
      y: metric.value,
      date: metric.loggedDate,
    }));

    return (
      <Card style={styles.chartCard}>
        <View style={styles.chartHeader}>
          <Thermometer size={20} color={theme.colors.primary} />
          <Text style={styles.chartTitle}>Temperature Trend</Text>
        </View>
        
        <VictoryChart
          theme={VictoryTheme.material}
          height={200}
          padding={{ top: 20, bottom: 40, left: 40, right: 20 }}
        >
          <VictoryAxis
            style={{
              axis: { stroke: theme.colors.text },
              tickLabels: { fill: theme.colors.text },
            }}
          />
          <VictoryAxis
            dependentAxis
            tickFormat={(y) => `${y}°C`}
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

  const renderRecentMetrics = () => {
    const recentMetrics = metrics.slice(0, 10);

    if (recentMetrics.length === 0) {
      return (
        <Card style={styles.emptyState}>
          <TrendingUp size={48} color={theme.colors.gray} />
          <Text style={styles.emptyTitle}>No metrics logged yet</Text>
          <Text style={styles.emptyText}>
            Start tracking your health metrics to see trends and insights.
          </Text>
        </Card>
      );
    }

    return (
      <Card style={styles.metricsListCard}>
        <Text style={styles.sectionTitle}>Recent Metrics</Text>
        
        {recentMetrics.map((metric) => {
          const metricType = metricTypes.find(m => m.type === metric.type);
          const Icon = metricType?.icon || Thermometer;
          
          return (
            <View key={metric.id} style={styles.metricItem}>
              <View style={styles.metricInfo}>
                <View style={[styles.metricIcon, { backgroundColor: theme.colors.primary + '20' }]}>
                  <Icon size={20} color={theme.colors.primary} />
                </View>
                <View style={styles.metricDetails}>
                  <Text style={styles.metricName}>{metricType?.label}</Text>
                  <Text style={styles.metricDate}>
                    {metric.loggedDate.toLocaleDateString()}
                  </Text>
                  {metric.notes ? (
                    <Text style={styles.metricNotes} numberOfLines={1}>
                      {metric.notes}
                    </Text>
                  ) : null}
                </View>
              </View>
              
              <View style={styles.metricValueContainer}>
                <Text style={styles.metricValue}>
                  {metric.value} {metric.unit}
                </Text>
                <TouchableOpacity
                  onPress={() => metric.id && handleDeleteMetric(metric.id)}
                  style={styles.deleteButton}
                >
                  <Trash2 size={18} color={theme.colors.error} />
                </TouchableOpacity>
              </View>
            </View>
          );
        })}
      </Card>
    );
  };

  return (
    <ScrollView 
      style={[styles.container, { backgroundColor: theme.colors.background }]}
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
    >
      {renderMetricForm()}
      {renderMetricsChart()}
      {renderRecentMetrics()}
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
  formCard: {
    marginBottom: 16,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 16,
  },
  metricTypeSelector: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 16,
    justifyContent: 'space-between',
  },
  metricTypeButton: {
    alignItems: 'center',
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'transparent',
    width: '18%',
    marginBottom: 8,
  },
  metricTypeSelected: {
    borderWidth: 2,
  },
  metricTypeLabel: {
    fontSize: 10,
    marginTop: 4,
    textAlign: 'center',
  },
  formRow: {
    flexDirection: 'row',
    marginBottom: 16,
  },
  dateInput: {
    flex: 1,
    marginRight: 12,
  },
  dateButton: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderWidth: 1,
    borderColor: '#E0E0E0',
    borderRadius: 8,
  },
  dateText: {
    marginLeft: 8,
    fontSize: 16,
  },
  valueInput: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#E0E0E0',
    borderRadius: 8,
    paddingHorizontal: 12,
  },
  input: {
    fontSize: 16,
    paddingVertical: 12,
  },
  unitText: {
    fontSize: 14,
    opacity: 0.7,
    marginLeft: 8,
  },
  notesInput: {
    borderWidth: 1,
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    minHeight: 80,
    textAlignVertical: 'top',
    marginBottom: 16,
  },
  addButton: {
    marginBottom: 8,
  },
  chartCard: {
    marginBottom: 16,
    padding: 16,
  },
  chartHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  chartTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 12,
  },
  metricsListCard: {
    marginBottom: 32,
    padding: 16,
  },
  metricItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.1)',
  },
  metricInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  metricIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  metricDetails: {
    flex: 1,
  },
  metricName: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 2,
  },
  metricDate: {
    fontSize: 12,
    opacity: 0.7,
    marginBottom: 2,
  },
  metricNotes: {
    fontSize: 12,
    opacity: 0.6,
    fontStyle: 'italic',
  },
  metricValueContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  metricValue: {
    fontSize: 16,
    fontWeight: '600',
    marginRight: 12,
  },
  deleteButton: {
    padding: 4,
  },
  emptyState: {
    alignItems: 'center',
    padding: 32,
    marginBottom: 16,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 8,
  },
  emptyText: {
    textAlign: 'center',
    opacity: 0.7,
    lineHeight: 20,
  },
});