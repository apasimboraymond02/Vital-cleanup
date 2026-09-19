import React, { useState, useEffect } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useSelector } from 'react-redux';
import { Calendar, AlertCircle, Target, Thermometer, Droplets, Activity } from 'lucide-react-native';
import { VictoryChart, VictoryScatter, VictoryAxis, VictoryTheme, VictoryLine } from 'victory-native';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useDatabase } from '../../database';
import { useTheme } from '../../theme/ThemeContext';
import { cycleUtils } from '../cycle/cycleUtils';

export const FertilityScreen = () => {
  const theme = useTheme();
  const { db } = useDatabase();
  const { cycleSettings } = useSelector((state: any) => state.settings);
  const [fertilityData, setFertilityData] = useState<any[]>([]);
  const [currentDay, setCurrentDay] = useState(1);
  const [riskLevel, setRiskLevel] = useState<'low' | 'medium' | 'high'>('low');
  const [basalTemp, setBasalTemp] = useState<number | null>(null);
  [cervicalMucus, setCervicalMucus] = useState<string>('dry');

  useEffect(() => {
    loadFertilityData();
    calculateCurrentDay();
  }, []);

  const loadFertilityData = async () => {
    if (!db) return;

    try {
      // Load temperature data
      db.transaction(tx => {
        tx.executeSql(
          `SELECT * FROM health_metrics 
           WHERE type = 'temperature' 
           ORDER BY logged_date DESC 
           LIMIT 30`,
          [],
          (_, { rows }) => {
            const temps = rows._array.map(row => ({
              x: new Date(row.logged_date).getDate(),
              y: row.value,
              date: new Date(row.logged_date),
            }));
            setFertilityData(temps);
            
            if (temps.length > 0) {
              setBasalTemp(temps[temps.length - 1].y);
            }
          }
        );
      });
    } catch (error) {
      console.error('Error loading fertility data:', error);
    }
  };

  const calculateCurrentDay = () => {
    // In production, this would calculate from last period
    const day = 14; // Example: ovulation day
    setCurrentDay(day);
    const risk = cycleUtils.calculateFertilityRisk(day, cycleSettings.averageLength);
    setRiskLevel(risk);
  };

  const renderRiskIndicator = () => {
    const riskColors = {
      low: { bg: '#E8F5E9', text: '#2E7D32', icon: '✓' },
      medium: { bg: '#FFF3E0', text: '#EF6C00', icon: '⚠️' },
      high: { bg: '#FFEBEE', text: '#C62828', icon: '🔥' },
    };

    const riskInfo = riskColors[riskLevel];

    return (
      <Card style={[styles.riskCard, { backgroundColor: riskInfo.bg }]}>
        <View style={styles.riskHeader}>
          <Target size={24} color={riskInfo.text} />
          <Text style={[styles.riskTitle, { color: riskInfo.text }]}>
            Today's Fertility Risk: {riskLevel.toUpperCase()}
          </Text>
        </View>
        <Text style={[styles.riskText, { color: riskInfo.text }]}>
          {riskLevel === 'low' 
            ? 'Low probability of conception today'
            : riskLevel === 'medium'
            ? 'Moderate probability of conception'
            : 'High probability of conception - fertile window'}
        </Text>
        <View style={styles.riskNote}>
          <AlertCircle size={16} color={riskInfo.text} />
          <Text style={[styles.riskNoteText, { color: riskInfo.text }]}>
            This is an estimate based on cycle tracking. Use contraception if avoiding pregnancy.
          </Text>
        </View>
      </Card>
    );
  };

  const renderCycleChart = () => {
    const cycleDays = Array.from({ length: cycleSettings.averageLength }, (_, i) => i + 1);
    const ovulationDay = cycleSettings.ovulationDay;
    const fertileStart = ovulationDay - 5;
    const fertileEnd = ovulationDay + 2;

    const data = cycleDays.map(day => ({
      day,
      fertility: cycleUtils.calculateFertilityRisk(day, cycleSettings.averageLength),
    }));

    return (
      <Card style={styles.chartCard}>
        <Text style={styles.cardTitle}>Monthly Fertility Window</Text>
        <View style={styles.cycleVisualization}>
          {cycleDays.map(day => {
            let color = '#E8F5E9'; // Low
            let label = '';
            
            if (day === ovulationDay) {
              color = '#FF5252';
              label = 'O';
            } else if (day >= fertileStart && day <= fertileEnd) {
              color = day < ovulationDay ? '#FFCC80' : '#FFAB91';
            }
            
            return (
              <View key={day} style={styles.dayContainer}>
                <View 
                  style={[
                    styles.dayBox, 
                    { 
                      backgroundColor: color,
                      borderWidth: day === currentDay ? 2 : 0,
                      borderColor: theme.colors.primary,
                    }
                  ]}
                >
                  <Text style={styles.dayText}>{day}</Text>
                  {label ? <Text style={styles.ovulationLabel}>{label}</Text> : null}
                </View>
                {day === currentDay && <Text style={styles.todayLabel}>Today</Text>}
              </View>
            );
          })}
        </View>
        <View style={styles.legend}>
          <View style={styles.legendItem}>
            <View style={[styles.legendColor, { backgroundColor: '#E8F5E9' }]} />
            <Text style={styles.legendText}>Low</Text>
          </View>
          <View style={styles.legendItem}>
            <View style={[styles.legendColor, { backgroundColor: '#FFCC80' }]} />
            <Text style={styles.legendText}>Medium</Text>
          </View>
          <View style={styles.legendItem}>
            <View style={[styles.legendColor, { backgroundColor: '#FFAB91' }]} />
            <Text style={styles.legendText}>High</Text>
          </View>
          <View style={styles.legendItem}>
            <View style={[styles.legendColor, { backgroundColor: '#FF5252' }]} />
            <Text style={styles.legendText}>Ovulation</Text>
          </View>
        </View>
      </Card>
    );
  };

  const renderTemperatureChart = () => {
    if (fertilityData.length === 0) return null;

    return (
      <Card style={styles.tempCard}>
        <View style={styles.cardHeader}>
          <Thermometer size={20} color={theme.colors.primary} />
          <Text style={styles.cardTitle}>Basal Body Temperature</Text>
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
            tickFormat={(t) => `${t}°C`}
            style={{
              axis: { stroke: theme.colors.text },
              tickLabels: { fill: theme.colors.text },
            }}
          />
          <VictoryLine
            data={fertilityData}
            style={{
              data: { stroke: theme.colors.primary, strokeWidth: 2 },
            }}
            interpolation="natural"
          />
          <VictoryScatter
            data={fertilityData}
            size={4}
            style={{
              data: { fill: theme.colors.primary },
            }}
          />
        </VictoryChart>

        {basalTemp && (
          <View style={styles.currentTemp}>
            <Text style={styles.tempLabel}>Current BBT:</Text>
            <Text style={styles.tempValue}>{basalTemp}°C</Text>
          </View>
        )}
      </Card>
    );
  };

  const renderSymptoms = () => {
    const symptoms = [
      { icon: Droplets, label: 'Cervical Mucus', value: cervicalMucus, color: '#BA68C8' },
      { icon: Activity, label: 'Cervical Position', value: 'Medium', color: '#4CAF50' },
      { icon: Target, label: 'LH Surge', value: 'Negative', color: '#2196F3' },
    ];

    return (
      <Card style={styles.symptomsCard}>
        <Text style={styles.cardTitle}>Fertility Signs</Text>
        <View style={styles.symptomsGrid}>
          {symptoms.map((symptom, index) => (
            <TouchableOpacity
              key={index}
              style={styles.symptomItem}
              onPress={() => logSymptom(symptom.label)}
            >
              <View style={[styles.symptomIcon, { backgroundColor: symptom.color + '20' }]}>
                <symptom.icon size={24} color={symptom.color} />
              </View>
              <Text style={styles.symptomLabel}>{symptom.label}</Text>
              <Text style={styles.symptomValue}>{symptom.value}</Text>
              <Button
                title="Log"
                onPress={() => logSymptom(symptom.label)}
                size="small"
                variant="outline"
                style={styles.logButton}
              />
            </TouchableOpacity>
          ))}
        </View>
      </Card>
    );
  };

  const renderEducation = () => {
    return (
      <Card style={styles.educationCard}>
        <Text style={styles.cardTitle}>Understanding Your Cycle</Text>
        <ScrollView style={styles.educationContent}>
          <View style={styles.educationSection}>
            <Text style={styles.sectionTitle}>Fertile Window</Text>
            <Text style={styles.sectionText}>
              The fertile window is typically 6 days long, ending on the day of ovulation. 
              This includes the 5 days before ovulation and the day of ovulation itself.
            </Text>
          </View>
          
          <View style={styles.educationSection}>
            <Text style={styles.sectionTitle}>Basal Body Temperature (BBT)</Text>
            <Text style={styles.sectionText}>
              Your BBT rises slightly (0.3-0.5°C) after ovulation due to increased progesterone. 
              Tracking BBT can help confirm ovulation has occurred.
            </Text>
          </View>
          
          <View style={styles.educationSection}>
            <Text style={styles.sectionTitle}>Cervical Mucus</Text>
            <Text style={styles.sectionText}>
              Fertile cervical mucus resembles raw egg whites - clear, stretchy, and slippery. 
              This creates an optimal environment for sperm survival and transport.
            </Text>
          </View>
          
          <View style={styles.educationSection}>
            <Text style={styles.sectionTitle}>Effectiveness</Text>
            <Text style={styles.sectionText}>
              When used perfectly, fertility awareness methods can be 95-99% effective at 
              preventing pregnancy. Typical use effectiveness is around 76-88%.
            </Text>
          </View>
        </ScrollView>
      </Card>
    );
  };

  const logSymptom = (symptomType: string) => {
    Alert.prompt(
      `Log ${symptomType}`,
      'Enter value:',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Save',
          onPress: (value) => {
            if (value && db) {
              const today = new Date().toISOString().split('T')[0];
              db.transaction(tx => {
                tx.executeSql(
                  `INSERT INTO health_metrics (type, value, unit, logged_date) 
                   VALUES (?, ?, ?, ?)`,
                  [symptomType.toLowerCase().replace(' ', '_'), parseFloat(value), 'custom', today],
                  () => {
                    Alert.alert('Success', `${symptomType} logged successfully`);
                    loadFertilityData();
                  }
                );
              });
            }
          },
        },
      ],
      'plain-text'
    );
  };

  return (
    <ScrollView 
      style={[styles.container, { backgroundColor: theme.colors.background }]}
      contentContainerStyle={styles.content}
    >
      <View style={styles.header}>
        <Text style={styles.title}>Fertility Tracker</Text>
        <TouchableOpacity onPress={() => {/* Open calendar */}}>
          <Calendar size={24} color={theme.colors.primary} />
        </TouchableOpacity>
      </View>

      {renderRiskIndicator()}
      {renderCycleChart()}
      {renderTemperatureChart()}
      {renderSymptoms()}
      {renderEducation()}

      <Card style={styles.disclaimerCard}>
        <AlertCircle size={20} color={theme.colors.warning} />
        <Text style={styles.disclaimerText}>
          Important: This tracker is for informational purposes only. It is not a reliable 
          method of birth control. Always consult with a healthcare provider for family 
          planning and contraception advice.
        </Text>
      </Card>
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
  riskCard: {
    marginBottom: 16,
    padding: 16,
    borderRadius: 12,
  },
  riskHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  riskTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 12,
  },
  riskText: {
    fontSize: 14,
    marginBottom: 12,
    lineHeight: 20,
  },
  riskNote: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  riskNoteText: {
    flex: 1,
    fontSize: 12,
    marginLeft: 8,
    lineHeight: 16,
  },
  chartCard: {
    marginBottom: 16,
    padding: 16,
  },
  cycleVisualization: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  dayContainer: {
    alignItems: 'center',
    width: '14%',
    marginBottom: 8,
  },
  dayBox: {
    width: 32,
    height: 32,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  dayText: {
    fontSize: 12,
    fontWeight: '600',
  },
  ovulationLabel: {
    position: 'absolute',
    top: -8,
    fontSize: 10,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  todayLabel: {
    fontSize: 10,
    opacity: 0.7,
  },
  legend: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginTop: 8,
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  legendColor: {
    width: 12,
    height: 12,
    borderRadius: 2,
    marginRight: 4,
  },
  legendText: {
    fontSize: 10,
    opacity: 0.8,
  },
  tempCard: {
    marginBottom: 16,
    padding: 16,
  },
  currentTemp: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 16,
  },
  tempLabel: {
    fontSize: 14,
    opacity: 0.7,
    marginRight: 8,
  },
  tempValue: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  symptomsCard: {
    marginBottom: 16,
    padding: 16,
  },
  symptomsGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  symptomItem: {
    alignItems: 'center',
    flex: 1,
    marginHorizontal: 4,
  },
  symptomIcon: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  symptomLabel: {
    fontSize: 12,
    textAlign: 'center',
    marginBottom: 4,
  },
  symptomValue: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 8,
  },
  logButton: {
    width: '100%',
  },
  educationCard: {
    marginBottom: 16,
    padding: 16,
    maxHeight: 300,
  },
  educationContent: {
    maxHeight: 250,
  },
  educationSection: {
    marginBottom: 16,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },
  sectionText: {
    fontSize: 14,
    lineHeight: 20,
    opacity: 0.8,
  },
  disclaimerCard: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    padding: 16,
    marginBottom: 32,
    backgroundColor: 'rgba(255,152,0,0.1)',
    borderRadius: 12,
  },
  disclaimerText: {
    flex: 1,
    marginLeft: 12,
    fontSize: 12,
    lineHeight: 16,
    fontStyle: 'italic',
  },
});