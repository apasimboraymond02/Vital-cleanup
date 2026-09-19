import React, { useState, useEffect } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Alert,
} from 'react-native';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import {
  Plus,
  Trash2,
  Save,
  Calendar,
  Thermometer,
  Heart,
  Brain,
  Droplets,
  Activity,
  X,
} from 'lucide-react-native';
import DateTimePicker from '@react-native-community/datetimepicker';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useDatabase } from '../../database';
import { useTheme } from '../../theme/ThemeContext';
import { RootStackParamList } from '../../navigation/RootNavigator';

type SymptomTrackerRouteProp = RouteProp<RootStackParamList, 'SymptomTracker'>;
type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

interface SymptomEntry {
  id: string;
  name: string;
  category: 'physical' | 'emotional' | 'digestive' | 'skin' | 'other';
  severity: 1 | 2 | 3 | 4 | 5;
  notes?: string;
  custom?: boolean;
}

export const SymptomTrackerScreen = () => {
  const theme = useTheme();
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<SymptomTrackerRouteProp>();
  const { db } = useDatabase();
  
  const { date: routeDate } = route.params;
  const [selectedDate, setSelectedDate] = useState(new Date(routeDate));
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [symptoms, setSymptoms] = useState<SymptomEntry[]>([]);
  const [customSymptom, setCustomSymptom] = useState('');
  const [loading, setLoading] = useState(false);

  const predefinedSymptoms: SymptomEntry[] = [
    { id: '1', name: 'Cramps', category: 'physical', severity: 3 },
    { id: '2', name: 'Headache', category: 'physical', severity: 3 },
    { id: '3', name: 'Fatigue', category: 'physical', severity: 3 },
    { id: '4', name: 'Bloating', category: 'digestive', severity: 3 },
    { id: '5', name: 'Nausea', category: 'digestive', severity: 3 },
    { id: '6', name: 'Breast Tenderness', category: 'physical', severity: 3 },
    { id: '7', name: 'Back Pain', category: 'physical', severity: 3 },
    { id: '8', name: 'Acne', category: 'skin', severity: 3 },
    { id: '9', name: 'Anxiety', category: 'emotional', severity: 3 },
    { id: '10', name: 'Mood Swings', category: 'emotional', severity: 3 },
    { id: '11', name: 'Food Cravings', category: 'other', severity: 3 },
    { id: '12', name: 'Insomnia', category: 'other', severity: 3 },
  ];

  const categoryIcons = {
    physical: Heart,
    emotional: Brain,
    digestive: Activity,
    skin: Droplets,
    other: Thermometer,
  };

  const categoryColors = {
    physical: '#FF5252',
    emotional: '#9C27B0',
    digestive: '#4CAF50',
    skin: '#2196F3',
    other: '#FF9800',
  };

  useEffect(() => {
    loadSymptomsForDate();
  }, [selectedDate]);

  const loadSymptomsForDate = async () => {
    if (!db) return;

    try {
      const dateString = selectedDate.toISOString().split('T')[0];
      
      db.transaction(tx => {
        tx.executeSql(
          `SELECT * FROM symptoms WHERE logged_date = ?`,
          [dateString],
          (_, { rows }) => {
            const loadedSymptoms = rows._array.map(row => ({
              id: row.id.toString(),
              name: row.name,
              category: row.category as any,
              severity: row.severity,
              notes: row.notes,
            }));
            setSymptoms(loadedSymptoms);
          }
        );
      });
    } catch (error) {
      console.error('Error loading symptoms:', error);
    }
  };

  const handleAddSymptom = (symptom: SymptomEntry) => {
    if (!symptoms.some(s => s.name === symptom.name)) {
      setSymptoms([...symptoms, { ...symptom, id: Date.now().toString() }]);
    }
  };

  const handleAddCustomSymptom = () => {
    if (!customSymptom.trim()) return;

    const newSymptom: SymptomEntry = {
      id: Date.now().toString(),
      name: customSymptom.trim(),
      category: 'other',
      severity: 3,
      custom: true,
    };

    setSymptoms([...symptoms, newSymptom]);
    setCustomSymptom('');
  };

  const handleRemoveSymptom = (symptomId: string) => {
    setSymptoms(symptoms.filter(s => s.id !== symptomId));
  };

  const handleUpdateSeverity = (symptomId: string, severity: 1 | 2 | 3 | 4 | 5) => {
    setSymptoms(symptoms.map(s => 
      s.id === symptomId ? { ...s, severity } : s
    ));
  };

  const handleSaveSymptoms = async () => {
    if (!db) return;

    setLoading(true);
    try {
      const dateString = selectedDate.toISOString().split('T')[0];

      // Delete existing symptoms for this date
      await new Promise<void>((resolve, reject) => {
        db.transaction(tx => {
          tx.executeSql(
            'DELETE FROM symptoms WHERE logged_date = ?',
            [dateString],
            () => resolve(),
            (_, error) => {
              reject(error);
              return false;
            }
          );
        });
      });

      // Save new symptoms
      for (const symptom of symptoms) {
        await new Promise<void>((resolve, reject) => {
          db.transaction(tx => {
            tx.executeSql(
              `INSERT INTO symptoms (name, category, severity, notes, logged_date)
               VALUES (?, ?, ?, ?, ?)`,
              [
                symptom.name,
                symptom.category,
                symptom.severity,
                symptom.notes || '',
                dateString,
              ],
              () => resolve(),
              (_, error) => {
                reject(error);
                return false;
              }
            );
          });
        });
      }

      Alert.alert('Success', 'Symptoms saved successfully', [
        { text: 'OK', onPress: () => navigation.goBack() },
      ]);
    } catch (error) {
      console.error('Error saving symptoms:', error);
      Alert.alert('Error', 'Failed to save symptoms');
    } finally {
      setLoading(false);
    }
  };

  const renderSymptomItem = (symptom: SymptomEntry) => {
    const Icon = categoryIcons[symptom.category];
    const color = categoryColors[symptom.category];

    return (
      <Card key={symptom.id} style={styles.symptomItem}>
        <View style={styles.symptomHeader}>
          <View style={styles.symptomInfo}>
            <View style={[styles.categoryIcon, { backgroundColor: color + '20' }]}>
              <Icon size={20} color={color} />
            </View>
            <View>
              <Text style={styles.symptomName}>{symptom.name}</Text>
              <Text style={[styles.symptomCategory, { color }]}>
                {symptom.category.charAt(0).toUpperCase() + symptom.category.slice(1)}
              </Text>
            </View>
          </View>
          <TouchableOpacity
            onPress={() => handleRemoveSymptom(symptom.id)}
            style={styles.removeButton}
          >
            <Trash2 size={20} color={theme.colors.error} />
          </TouchableOpacity>
        </View>

        <View style={styles.severityContainer}>
          <Text style={styles.severityLabel}>Severity:</Text>
          <View style={styles.severityButtons}>
            {[1, 2, 3, 4, 5].map(level => (
              <TouchableOpacity
                key={level}
                style={[
                  styles.severityButton,
                  symptom.severity === level && styles.severityButtonSelected,
                  symptom.severity === level && { backgroundColor: color },
                ]}
                onPress={() => handleUpdateSeverity(symptom.id, level as any)}
              >
                <Text style={[
                  styles.severityText,
                  symptom.severity === level && { color: '#FFFFFF' },
                ]}>
                  {level}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <TextInput
          style={[styles.notesInput, { borderColor: theme.colors.border }]}
          placeholder="Add notes (optional)"
          placeholderTextColor={theme.colors.textSecondary}
          value={symptom.notes || ''}
          onChangeText={(text) => {
            setSymptoms(symptoms.map(s =>
              s.id === symptom.id ? { ...s, notes: text } : s
            ));
          }}
          multiline
        />
      </Card>
    );
  };

  const renderPredefinedSymptoms = () => {
    const categories = ['physical', 'emotional', 'digestive', 'skin', 'other'];

    return (
      <Card style={styles.predefinedCard}>
        <Text style={styles.sectionTitle}>Common Symptoms</Text>
        
        {categories.map(category => {
          const Icon = categoryIcons[category as keyof typeof categoryIcons];
          const color = categoryColors[category as keyof typeof categoryColors];
          const categorySymptoms = predefinedSymptoms.filter(s => s.category === category);

          if (categorySymptoms.length === 0) return null;

          return (
            <View key={category} style={styles.categorySection}>
              <View style={styles.categoryHeader}>
                <Icon size={16} color={color} />
                <Text style={[styles.categoryTitle, { color }]}>
                  {category.charAt(0).toUpperCase() + category.slice(1)}
                </Text>
              </View>
              
              <View style={styles.symptomGrid}>
                {categorySymptoms.map(symptom => {
                  const isAdded = symptoms.some(s => s.name === symptom.name);
                  return (
                    <TouchableOpacity
                      key={symptom.id}
                      style={[
                        styles.symptomChip,
                        isAdded && styles.symptomChipAdded,
                        isAdded && { backgroundColor: color + '20' },
                      ]}
                      onPress={() => isAdded ? null : handleAddSymptom(symptom)}
                      disabled={isAdded}
                    >
                      <Text style={[
                        styles.symptomChipText,
                        isAdded && { color },
                      ]}>
                        {symptom.name}
                      </Text>
                      {isAdded && (
                        <View style={[styles.addedBadge, { backgroundColor: color }]}>
                          <Text style={styles.addedText}>✓</Text>
                        </View>
                      )}
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>
          );
        })}
      </Card>
    );
  };

  const renderCustomSymptomInput = () => {
    return (
      <Card style={styles.customCard}>
        <Text style={styles.sectionTitle}>Custom Symptom</Text>
        <View style={styles.customInputContainer}>
          <TextInput
            style={[styles.customInput, { borderColor: theme.colors.border }]}
            placeholder="Enter custom symptom name"
            placeholderTextColor={theme.colors.textSecondary}
            value={customSymptom}
            onChangeText={setCustomSymptom}
            onSubmitEditing={handleAddCustomSymptom}
          />
          <Button
            title="Add"
            onPress={handleAddCustomSymptom}
            disabled={!customSymptom.trim()}
            size="small"
            style={styles.addButton}
          />
        </View>
      </Card>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <ScrollView 
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <Card style={styles.headerCard}>
          <View style={styles.header}>
            <TouchableOpacity
              style={styles.dateSelector}
              onPress={() => setShowDatePicker(true)}
            >
              <Calendar size={20} color={theme.colors.primary} />
              <Text style={styles.dateText}>
                {selectedDate.toLocaleDateString('en-US', {
                  weekday: 'long',
                  month: 'long',
                  day: 'numeric',
                  year: 'numeric',
                })}
              </Text>
            </TouchableOpacity>
            
            <Text style={styles.symptomsCount}>
              {symptoms.length} symptom{symptoms.length !== 1 ? 's' : ''} logged
            </Text>
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
        </Card>

        {symptoms.length > 0 ? (
          <View style={styles.symptomsList}>
            {symptoms.map(renderSymptomItem)}
          </View>
        ) : (
          <Card style={styles.emptyState}>
            <Text style={styles.emptyTitle}>No symptoms logged</Text>
            <Text style={styles.emptyText}>
              Tap on symptoms below to start tracking how you're feeling today.
            </Text>
          </Card>
        )}

        {renderPredefinedSymptoms()}
        {renderCustomSymptomInput()}

        <View style={styles.footer}>
          <Button
            title="Save Symptoms"
            onPress={handleSaveSymptoms}
            loading={loading}
            leftIcon={<Save size={20} />}
            style={styles.saveButton}
          />
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
  headerCard: {
    marginBottom: 16,
    padding: 16,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  dateSelector: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  dateText: {
    fontSize: 16,
    fontWeight: '500',
    marginLeft: 8,
  },
  symptomsCount: {
    fontSize: 14,
    opacity: 0.7,
  },
  symptomsList: {
    marginBottom: 16,
  },
  symptomItem: {
    marginBottom: 12,
    padding: 16,
  },
  symptomHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  symptomInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  categoryIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  symptomName: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 2,
  },
  symptomCategory: {
    fontSize: 12,
    opacity: 0.8,
  },
  removeButton: {
    padding: 4,
  },
  severityContainer: {
    marginBottom: 12,
  },
  severityLabel: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
  },
  severityButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  severityButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  severityButtonSelected: {
    borderColor: 'transparent',
  },
  severityText: {
    fontSize: 16,
    fontWeight: '600',
  },
  notesInput: {
    borderWidth: 1,
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    minHeight: 40,
    textAlignVertical: 'top',
  },
  emptyState: {
    padding: 32,
    alignItems: 'center',
    marginBottom: 16,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 8,
  },
  emptyText: {
    textAlign: 'center',
    opacity: 0.7,
    lineHeight: 20,
  },
  predefinedCard: {
    marginBottom: 16,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 16,
  },
  categorySection: {
    marginBottom: 20,
  },
  categoryHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  categoryTitle: {
    fontSize: 14,
    fontWeight: '600',
    marginLeft: 8,
  },
  symptomGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  symptomChip: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#E0E0E0',
    marginRight: 8,
    marginBottom: 8,
    flexDirection: 'row',
    alignItems: 'center',
  },
  symptomChipAdded: {
    borderColor: 'transparent',
  },
  symptomChipText: {
    fontSize: 12,
  },
  addedBadge: {
    width: 16,
    height: 16,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 6,
  },
  addedText: {
    fontSize: 10,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  customCard: {
    marginBottom: 16,
    padding: 16,
  },
  customInputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  customInput: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    marginRight: 12,
  },
  addButton: {
    minWidth: 80,
  },
  footer: {
    marginTop: 8,
    marginBottom: 32,
  },
  saveButton: {
    marginBottom: 8,
  },
});