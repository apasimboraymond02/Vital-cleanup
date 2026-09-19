import React, { useState, useEffect } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Alert,
} from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import {
  Calendar,
  Droplet,
  Smile,
  X,
  Save,
  Trash2,
} from 'lucide-react-native';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useDatabase } from '../../database';
import { useTheme } from '../../theme/ThemeContext';
import { RootStackParamList } from '../../navigation/RootNavigator';

type CycleFormRouteProp = RouteProp<RootStackParamList, 'CycleForm'>;
type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

export const CycleFormScreen = () => {
  const theme = useTheme();
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<CycleFormRouteProp>();
  const { db } = useDatabase();
  
  const { cycleId } = route.params || {};
  const [loading, setLoading] = useState(false);
  
  // Form state
  const [startDate, setStartDate] = useState(new Date());
  const [endDate, setEndDate] = useState<Date | null>(null);
  const [flowIntensity, setFlowIntensity] = useState<1 | 2 | 3 | 4 | 5>(3);
  const [mood, setMood] = useState('');
  const [notes, setNotes] = useState('');
  const [symptoms, setSymptoms] = useState<string[]>([]);
  
  // Date picker states
  const [showStartDatePicker, setShowStartDatePicker] = useState(false);
  const [showEndDatePicker, setShowEndDatePicker] = useState(false);

  useEffect(() => {
    if (cycleId) {
      loadCycleData();
    }
  }, [cycleId]);

  const loadCycleData = async () => {
    if (!db || !cycleId) return;

    try {
      db.transaction(tx => {
        tx.executeSql(
          'SELECT * FROM cycles WHERE id = ?',
          [cycleId],
          (_, { rows }) => {
            if (rows.length > 0) {
              const cycle = rows._array[0];
              setStartDate(new Date(cycle.start_date));
              if (cycle.end_date) {
                setEndDate(new Date(cycle.end_date));
              }
              setFlowIntensity(cycle.flow_intensity || 3);
              setMood(cycle.mood || '');
              setNotes(cycle.notes || '');
              setSymptoms(cycle.symptoms ? JSON.parse(cycle.symptoms) : []);
            }
          }
        );
      });
    } catch (error) {
      console.error('Error loading cycle:', error);
    }
  };

  const handleSave = async () => {
    if (!db) return;

    setLoading(true);
    try {
      const cycleData = {
        start_date: startDate.toISOString(),
        end_date: endDate ? endDate.toISOString() : null,
        flow_intensity: flowIntensity,
        mood: mood,
        notes: notes,
        symptoms: JSON.stringify(symptoms),
        updated_at: new Date().toISOString(),
      };

      await new Promise<void>((resolve, reject) => {
        db.transaction(tx => {
          if (cycleId) {
            // Update existing cycle
            tx.executeSql(
              `UPDATE cycles 
               SET start_date = ?, end_date = ?, flow_intensity = ?, mood = ?, notes = ?, symptoms = ?, updated_at = ?
               WHERE id = ?`,
              [
                cycleData.start_date,
                cycleData.end_date,
                cycleData.flow_intensity,
                cycleData.mood,
                cycleData.notes,
                cycleData.symptoms,
                cycleData.updated_at,
                cycleId,
              ],
              () => resolve(),
              (_, error) => {
                reject(error);
                return false;
              }
            );
          } else {
            // Insert new cycle
            tx.executeSql(
              `INSERT INTO cycles (start_date, end_date, flow_intensity, mood, notes, symptoms)
               VALUES (?, ?, ?, ?, ?, ?)`,
              [
                cycleData.start_date,
                cycleData.end_date,
                cycleData.flow_intensity,
                cycleData.mood,
                cycleData.notes,
                cycleData.symptoms,
              ],
              () => resolve(),
              (_, error) => {
                reject(error);
                return false;
              }
            );
          }
        });
      });

      Alert.alert(
        'Success',
        cycleId ? 'Cycle updated successfully' : 'Cycle logged successfully',
        [
          {
            text: 'OK',
            onPress: () => navigation.goBack(),
          },
        ]
      );
    } catch (error) {
      console.error('Error saving cycle:', error);
      Alert.alert('Error', 'Failed to save cycle. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = () => {
    if (!cycleId || !db) return;

    Alert.alert(
      'Delete Cycle',
      'Are you sure you want to delete this cycle record?',
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
                    'UPDATE cycles SET is_deleted = 1 WHERE id = ?',
                    [cycleId],
                    () => resolve(),
                    (_, error) => {
                      reject(error);
                      return false;
                    }
                  );
                });
              });

              Alert.alert(
                'Success',
                'Cycle deleted successfully',
                [
                  {
                    text: 'OK',
                    onPress: () => navigation.goBack(),
                  },
                ]
              );
            } catch (error) {
              console.error('Error deleting cycle:', error);
              Alert.alert('Error', 'Failed to delete cycle.');
            }
          },
        },
      ]
    );
  };

  const renderDatePicker = (
    label: string,
    date: Date | null,
    showPicker: boolean,
    setShowPicker: (show: boolean) => void,
    setDate: (date: Date) => void
  ) => {
    return (
      <View style={styles.formGroup}>
        <Text style={styles.label}>{label}</Text>
        <TouchableOpacity
          style={styles.dateInput}
          onPress={() => setShowPicker(true)}
        >
          <Calendar size={20} color={theme.colors.textSecondary} />
          <Text style={styles.dateText}>
            {date ? date.toLocaleDateString() : 'Select date'}
          </Text>
        </TouchableOpacity>
        
        {showPicker && (
          <DateTimePicker
            value={date || new Date()}
            mode="date"
            display="default"
            onChange={(event, selectedDate) => {
              setShowPicker(false);
              if (selectedDate) {
                setDate(selectedDate);
              }
            }}
          />
        )}
      </View>
    );
  };

  const renderFlowIntensity = () => {
    const intensities = [
      { value: 1, label: 'Very Light', emoji: '💧' },
      { value: 2, label: 'Light', emoji: '💧💧' },
      { value: 3, label: 'Medium', emoji: '💧💧💧' },
      { value: 4, label: 'Heavy', emoji: '💧💧💧💧' },
      { value: 5, label: 'Very Heavy', emoji: '💧💧💧💧💧' },
    ];

    return (
      <View style={styles.formGroup}>
        <Text style={styles.label}>Flow Intensity</Text>
        <View style={styles.flowContainer}>
          {intensities.map((intensity) => (
            <TouchableOpacity
              key={intensity.value}
              style={[
                styles.flowOption,
                flowIntensity === intensity.value && styles.flowOptionSelected,
                flowIntensity === intensity.value && { 
                  borderColor: theme.colors.primary,
                  backgroundColor: theme.colors.primary + '20',
                },
              ]}
              onPress={() => setFlowIntensity(intensity.value as any)}
            >
              <Text style={styles.flowEmoji}>{intensity.emoji}</Text>
              <Text style={[
                styles.flowLabel,
                flowIntensity === intensity.value && { color: theme.colors.primary },
              ]}>
                {intensity.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>
    );
  };

  const renderMoodSelector = () => {
    const moods = [
      { emoji: '😊', label: 'Happy' },
      { emoji: '😢', label: 'Sad' },
      { emoji: '😠', label: 'Angry' },
      { emoji: '😰', label: 'Anxious' },
      { emoji: '😴', label: 'Tired' },
      { emoji: '🤒', label: 'Sick' },
      { emoji: '😌', label: 'Calm' },
      { emoji: '🤩', label: 'Energetic' },
    ];

    return (
      <View style={styles.formGroup}>
        <Text style={styles.label}>Mood</Text>
        <View style={styles.moodContainer}>
          {moods.map((moodItem) => (
            <TouchableOpacity
              key={moodItem.label}
              style={[
                styles.moodOption,
                mood === moodItem.label && styles.moodOptionSelected,
                mood === moodItem.label && { 
                  backgroundColor: theme.colors.primary + '20',
                  borderColor: theme.colors.primary,
                },
              ]}
              onPress={() => setMood(mood === moodItem.label ? '' : moodItem.label)}
            >
              <Text style={styles.moodEmoji}>{moodItem.emoji}</Text>
              <Text style={[
                styles.moodLabel,
                mood === moodItem.label && { color: theme.colors.primary },
              ]}>
                {moodItem.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
        {mood && (
          <TouchableOpacity
            style={styles.clearMood}
            onPress={() => setMood('')}
          >
            <X size={16} color={theme.colors.textSecondary} />
            <Text style={styles.clearMoodText}>Clear mood</Text>
          </TouchableOpacity>
        )}
      </View>
    );
  };

  const renderSymptomInput = () => {
    const commonSymptoms = [
      'Cramps', 'Headache', 'Bloating', 'Fatigue', 'Breast Tenderness',
      'Back Pain', 'Acne', 'Nausea', 'Food Cravings', 'Insomnia',
    ];

    const addSymptom = (symptom: string) => {
      if (!symptoms.includes(symptom)) {
        setSymptoms([...symptoms, symptom]);
      }
    };

    const removeSymptom = (symptom: string) => {
      setSymptoms(symptoms.filter(s => s !== symptom));
    };

    return (
      <View style={styles.formGroup}>
        <Text style={styles.label}>Symptoms</Text>
        
        {symptoms.length > 0 && (
          <View style={styles.selectedSymptoms}>
            {symptoms.map((symptom) => (
              <TouchableOpacity
                key={symptom}
                style={[styles.symptomTag, { backgroundColor: theme.colors.primary + '20' }]}
                onPress={() => removeSymptom(symptom)}
              >
                <Text style={[styles.symptomTagText, { color: theme.colors.primary }]}>
                  {symptom}
                </Text>
                <X size={14} color={theme.colors.primary} />
              </TouchableOpacity>
            ))}
          </View>
        )}

        <Text style={styles.symptomSubtitle}>Common Symptoms:</Text>
        <View style={styles.symptomGrid}>
          {commonSymptoms.map((symptom) => (
            <TouchableOpacity
              key={symptom}
              style={[
                styles.symptomButton,
                symptoms.includes(symptom) && { backgroundColor: theme.colors.primary },
              ]}
              onPress={() => 
                symptoms.includes(symptom) 
                  ? removeSymptom(symptom)
                  : addSymptom(symptom)
              }
            >
              <Text style={[
                styles.symptomButtonText,
                symptoms.includes(symptom) && { color: '#FFFFFF' },
              ]}>
                {symptom}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        <TextInput
          style={[styles.input, { height: 80 }]}
          placeholder="Add custom symptoms or notes..."
          placeholderTextColor={theme.colors.textSecondary}
          value={notes}
          onChangeText={setNotes}
          multiline
          numberOfLines={4}
        />
      </View>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <ScrollView 
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <Card style={styles.formCard}>
          {renderDatePicker('Start Date', startDate, showStartDatePicker, setShowStartDatePicker, setStartDate)}
          {renderDatePicker('End Date', endDate, showEndDatePicker, setShowEndDatePicker, setEndDate)}
          {renderFlowIntensity()}
          {renderMoodSelector()}
          {renderSymptomInput()}
        </Card>

        <View style={styles.actionButtons}>
          <Button
            title="Save"
            onPress={handleSave}
            loading={loading}
            leftIcon={<Save size={20} />}
            style={styles.saveButton}
          />
          
          {cycleId && (
            <Button
              title="Delete"
              onPress={handleDelete}
              variant="outline"
              leftIcon={<Trash2 size={20} />}
              style={styles.deleteButton}
            />
          )}
        </View>

        <View style={styles.tips}>
          <Text style={styles.tipsTitle}>Tips:</Text>
          <Text style={styles.tip}>• Log your period as soon as it starts for accurate predictions</Text>
          <Text style={styles.tip}>• Track symptoms daily for better pattern recognition</Text>
          <Text style={styles.tip}>• Mark the end date when your period completely stops</Text>
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    padding: 16,
  },
  formCard: {
    padding: 16,
    marginBottom: 16,
  },
  formGroup: {
    marginBottom: 24,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },
  dateInput: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderWidth: 1,
    borderColor: '#E0E0E0',
    borderRadius: 8,
  },
  dateText: {
    marginLeft: 12,
    fontSize: 16,
    flex: 1,
  },
  flowContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  flowOption: {
    alignItems: 'center',
    padding: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'transparent',
    flex: 1,
    marginHorizontal: 2,
  },
  flowOptionSelected: {
    borderWidth: 2,
  },
  flowEmoji: {
    fontSize: 20,
    marginBottom: 4,
  },
  flowLabel: {
    fontSize: 10,
    textAlign: 'center',
  },
  moodContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  moodOption: {
    alignItems: 'center',
    padding: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'transparent',
    width: '23%',
    marginBottom: 8,
  },
  moodOptionSelected: {
    borderWidth: 2,
  },
  moodEmoji: {
    fontSize: 24,
    marginBottom: 4,
  },
  moodLabel: {
    fontSize: 10,
    textAlign: 'center',
  },
  clearMood: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-end',
    marginTop: 8,
  },
  clearMoodText: {
    fontSize: 12,
    marginLeft: 4,
    color: '#757575',
  },
  selectedSymptoms: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 16,
  },
  symptomTag: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    marginRight: 8,
    marginBottom: 8,
  },
  symptomTagText: {
    fontSize: 12,
    marginRight: 6,
    fontWeight: '500',
  },
  symptomSubtitle: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
    opacity: 0.7,
  },
  symptomGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  symptomButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#E0E0E0',
    marginBottom: 8,
    width: '48%',
  },
  symptomButtonText: {
    fontSize: 12,
    textAlign: 'center',
  },
  input: {
    borderWidth: 1,
    borderColor: '#E0E0E0',
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    textAlignVertical: 'top',
  },
  actionButtons: {
    flexDirection: 'row',
    marginBottom: 24,
  },
  saveButton: {
    flex: 1,
    marginRight: 8,
  },
  deleteButton: {
    flex: 1,
    marginLeft: 8,
    borderColor: '#F44336',
  },
  tips: {
    padding: 16,
    backgroundColor: 'rgba(0,0,0,0.03)',
    borderRadius: 12,
  },
  tipsTitle: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 8,
  },
  tip: {
    fontSize: 12,
    lineHeight: 18,
    marginBottom: 4,
    opacity: 0.8,
  },
});