import React, { useState, useEffect } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useSelector } from 'react-redux';
import { Baby, Calendar, Heart, Timer, Bell, CheckCircle } from 'lucide-react-native';
import { VictoryChart, VictoryArea, VictoryAxis, VictoryTheme } from 'victory-native';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useDatabase } from '../../database';
import { useTheme } from '../../theme/ThemeContext';
import { Pregnancy, PregnancyMilestone } from '../../types';

export const PregnancyDashboard = () => {
  const theme = useTheme();
  const { db } = useDatabase();
  const [pregnancy, setPregnancy] = useState<Pregnancy | null>(null);
  const [milestones, setMilestones] = useState<PregnancyMilestone[]>([]);
  const [weeks, setWeeks] = useState(0);
  const [dueDate, setDueDate] = useState<Date | null>(null);

  useEffect(() => {
    loadPregnancyData();
  }, []);

  const loadPregnancyData = async () => {
    if (!db) return;

    try {
      db.transaction(tx => {
        tx.executeSql(
          `SELECT * FROM pregnancies ORDER BY start_date DESC LIMIT 1`,
          [],
          (_, { rows }) => {
            if (rows.length > 0) {
              const data = rows._array[0];
              const pregnancyData: Pregnancy = {
                id: data.id.toString(),
                startDate: new Date(data.start_date),
                dueDate: new Date(data.due_date),
                lastPeriodDate: new Date(data.last_period_date),
                confirmedByDoctor: data.confirmed_by_doctor === 1,
                doctorVisitDates: data.doctor_visit_dates ? JSON.parse(data.doctor_visit_dates).map((d: string) => new Date(d)) : [],
                currentWeek: data.current_week,
                notes: data.notes,
                milestones: [],
              };
              setPregnancy(pregnancyData);
              setDueDate(new Date(data.due_date));
              
              // Calculate current week
              const today = new Date();
              const start = new Date(data.start_date);
              const diff = Math.floor((today.getTime() - start.getTime()) / (1000 * 60 * 60 * 24 * 7));
              setWeeks(Math.min(40, Math.max(1, diff)));
              
              loadMilestones(data.id);
            }
          }
        );
      });
    } catch (error) {
      console.error('Error loading pregnancy data:', error);
    }
  };

  const loadMilestones = async (pregnancyId: number) => {
    if (!db) return;

    // This would come from a milestones table in production
    const defaultMilestones: PregnancyMilestone[] = [
      { id: '1', week: 8, title: 'First Ultrasound', description: 'Confirm heartbeat and due date', completed: false },
      { id: '2', week: 12, title: 'NT Scan', description: 'Nuchal translucency screening', completed: false },
      { id: '3', week: 20, title: 'Anatomy Scan', description: 'Detailed baby anatomy check', completed: false },
      { id: '4', week: 24, title: 'Glucose Test', description: 'Gestational diabetes screening', completed: false },
      { id: '5', week: 28, title: 'Rhogam Shot', description: 'If Rh negative', completed: false },
      { id: '6', week: 36, title: 'GBS Test', description: 'Group B strep screening', completed: false },
    ];
    setMilestones(defaultMilestones);
  };

  const handleAddPregnancy = () => {
    Alert.prompt(
      'Start Pregnancy Tracking',
      'Enter the first day of your last menstrual period (LMP):',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Continue',
          onPress: (date) => {
            if (date) {
              calculateDueDate(new Date(date));
            }
          },
        },
      ],
      'plain-text'
    );
  };

  const calculateDueDate = (lmpDate: Date) => {
    const dueDate = new Date(lmpDate);
    dueDate.setDate(dueDate.getDate() + 280); // 40 weeks from LMP
    
    if (!db) return;

    db.transaction(tx => {
      tx.executeSql(
        `INSERT INTO pregnancies (start_date, due_date, last_period_date, current_week) 
         VALUES (?, ?, ?, ?)`,
        [lmpDate.toISOString(), dueDate.toISOString(), lmpDate.toISOString(), 1],
        () => {
          loadPregnancyData();
          Alert.alert(
            'Pregnancy Tracking Started',
            `Your estimated due date is ${dueDate.toLocaleDateString()}. Remember to confirm with your healthcare provider.`,
            [{ text: 'OK' }]
          );
        },
        (_, error) => {
          console.error('Error saving pregnancy:', error);
          return false;
        }
      );
    });
  };

  const renderProgress = () => {
    if (!dueDate) return null;

    const progress = (weeks / 40) * 100;
    const daysLeft = Math.floor((dueDate.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24));

    return (
      <Card style={styles.progressCard}>
        <View style={styles.progressHeader}>
          <Baby size={24} color={theme.colors.primary} />
          <Text style={styles.progressTitle}>Pregnancy Progress</Text>
        </View>
        
        <View style={styles.weekIndicator}>
          <Text style={styles.weekNumber}>{weeks}</Text>
          <Text style={styles.weekLabel}>weeks</Text>
        </View>

        <View style={styles.progressBarContainer}>
          <View style={[styles.progressBar, { width: `${progress}%` }]} />
        </View>

        <View style={styles.progressDetails}>
          <View style={styles.progressDetail}>
            <Text style={styles.detailLabel}>Trimester</Text>
            <Text style={styles.detailValue}>
              {weeks <= 13 ? 'First' : weeks <= 26 ? 'Second' : 'Third'}
            </Text>
          </View>
          <View style={styles.progressDetail}>
            <Text style={styles.detailLabel}>Due Date</Text>
            <Text style={styles.detailValue}>{dueDate.toLocaleDateString()}</Text>
          </View>
          <View style={styles.progressDetail}>
            <Text style={styles.detailLabel}>Days Left</Text>
            <Text style={styles.detailValue}>{daysLeft}</Text>
          </View>
        </View>
      </Card>
    );
  };

  const renderWeekDevelopment = () => {
    const developmentData = [
      { week: 4, size: 'Poppy seed' },
      { week: 8, size: 'Raspberry' },
      { week: 12, size: 'Lime' },
      { week: 16, size: 'Avocado' },
      { week: 20, size: 'Banana' },
      { week: 24, size: 'Corn' },
      { week: 28, size: 'Eggplant' },
      { week: 32, size: 'Squash' },
      { week: 36, size: 'Romaine lettuce' },
      { week: 40, size: 'Pumpkin' },
    ];

    const currentData = developmentData.find(d => d.week >= weeks) || developmentData[developmentData.length - 1];

    return (
      <Card style={styles.developmentCard}>
        <Text style={styles.cardTitle}>Baby Development</Text>
        <View style={styles.developmentContent}>
          <View style={styles.sizeIndicator}>
            <Heart size={32} color={theme.colors.primary} />
            <Text style={styles.sizeText}>Size of a</Text>
            <Text style={styles.sizeValue}>{currentData.size}</Text>
          </View>
          <Text style={styles.developmentText}>
            At {weeks} weeks, your baby is developing rapidly. Organs are maturing, 
            and baby is practicing breathing movements.
          </Text>
        </View>
      </Card>
    );
  };

  const renderMilestones = () => {
    const upcomingMilestones = milestones
      .filter(m => m.week >= weeks)
      .slice(0, 3);

    return (
      <Card style={styles.milestonesCard}>
        <View style={styles.cardHeader}>
          <Calendar size={20} color={theme.colors.primary} />
          <Text style={styles.cardTitle}>Upcoming Milestones</Text>
        </View>
        
        {upcomingMilestones.map((milestone, index) => (
          <TouchableOpacity
            key={milestone.id}
            style={[
              styles.milestoneItem,
              index < upcomingMilestones.length - 1 && styles.milestoneBorder,
            ]}
            onPress={() => toggleMilestone(milestone.id)}
          >
            <View style={styles.milestoneInfo}>
              <Text style={styles.milestoneWeek}>Week {milestone.week}</Text>
              <Text style={styles.milestoneTitle}>{milestone.title}</Text>
              <Text style={styles.milestoneDescription}>{milestone.description}</Text>
            </View>
            <View style={[
              styles.milestoneCheck,
              milestone.completed && styles.milestoneCompleted,
            ]}>
              {milestone.completed && <CheckCircle size={20} color="#FFFFFF" />}
            </View>
          </TouchableOpacity>
        ))}
      </Card>
    );
  };

  const renderTools = () => {
    const tools = [
      { icon: Timer, title: 'Kick Counter', action: () => {/* Navigate */} },
      { icon: Bell, title: 'Contraction Timer', action: () => {/* Navigate */} },
      { icon: Calendar, title: 'Doctor Visits', action: () => {/* Navigate */} },
    ];

    return (
      <Card style={styles.toolsCard}>
        <Text style={styles.cardTitle}>Pregnancy Tools</Text>
        <View style={styles.toolsGrid}>
          {tools.map((tool, index) => (
            <TouchableOpacity
              key={index}
              style={styles.toolItem}
              onPress={tool.action}
            >
              <View style={[styles.toolIcon, { backgroundColor: theme.colors.primary + '20' }]}>
                <tool.icon size={24} color={theme.colors.primary} />
              </View>
              <Text style={styles.toolTitle}>{tool.title}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </Card>
    );
  };

  const toggleMilestone = (milestoneId: string) => {
    setMilestones(prev => prev.map(milestone =>
      milestone.id === milestoneId
        ? { ...milestone, completed: !milestone.completed }
        : milestone
    ));
  };

  if (!pregnancy) {
    return (
      <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
        <ScrollView contentContainerStyle={styles.emptyContent}>
          <Card style={styles.emptyState}>
            <Baby size={64} color={theme.colors.gray} />
            <Text style={styles.emptyStateTitle}>Pregnancy Tracker</Text>
            <Text style={styles.emptyStateText}>
              Track your pregnancy week by week with personalized insights, 
              milestone tracking, and helpful tools.
            </Text>
            <Button
              title="Start Tracking Pregnancy"
              onPress={handleAddPregnancy}
              style={styles.startButton}
            />
            <Text style={styles.disclaimer}>
              Entering pregnancy data will help us provide more accurate 
              cycle predictions in the future.
            </Text>
          </Card>
        </ScrollView>
      </View>
    );
  }

  return (
    <ScrollView 
      style={[styles.container, { backgroundColor: theme.colors.background }]}
      contentContainerStyle={styles.content}
    >
      {renderProgress()}
      {renderWeekDevelopment()}
      {renderMilestones()}
      {renderTools()}
      
      <Card style={styles.healthTips}>
        <Text style={styles.cardTitle}>Health Tips This Week</Text>
        <View style={styles.tipsList}>
          <Text style={styles.tip}>• Stay hydrated with at least 8 glasses of water daily</Text>
          <Text style={styles.tip}>• Continue taking prenatal vitamins</Text>
          <Text style={styles.tip}>• Monitor for signs of preeclampsia (swelling, headaches)</Text>
          <Text style={styles.tip}>• Practice gentle exercises like walking or prenatal yoga</Text>
          <Text style={styles.tip}>• Schedule your next prenatal appointment</Text>
        </View>
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
  emptyContent: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  emptyState: {
    alignItems: 'center',
    padding: 32,
    width: '100%',
  },
  emptyStateTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    marginTop: 16,
    marginBottom: 8,
  },
  emptyStateText: {
    textAlign: 'center',
    marginBottom: 24,
    lineHeight: 20,
    opacity: 0.8,
  },
  startButton: {
    marginTop: 16,
    marginBottom: 24,
  },
  disclaimer: {
    textAlign: 'center',
    fontSize: 12,
    opacity: 0.6,
    fontStyle: 'italic',
  },
  progressCard: {
    marginBottom: 16,
    padding: 16,
  },
  progressHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
  },
  progressTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginLeft: 12,
  },
  weekIndicator: {
    alignItems: 'center',
    marginBottom: 20,
  },
  weekNumber: {
    fontSize: 48,
    fontWeight: 'bold',
  },
  weekLabel: {
    fontSize: 14,
    opacity: 0.7,
  },
  progressBarContainer: {
    height: 8,
    backgroundColor: 'rgba(0,0,0,0.1)',
    borderRadius: 4,
    marginBottom: 20,
    overflow: 'hidden',
  },
  progressBar: {
    height: '100%',
    backgroundColor: '#4CAF50',
    borderRadius: 4,
  },
  progressDetails: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  progressDetail: {
    alignItems: 'center',
  },
  detailLabel: {
    fontSize: 12,
    opacity: 0.7,
    marginBottom: 4,
  },
  detailValue: {
    fontSize: 16,
    fontWeight: '600',
  },
  developmentCard: {
    marginBottom: 16,
    padding: 16,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 16,
  },
  developmentContent: {
    alignItems: 'center',
  },
  sizeIndicator: {
    alignItems: 'center',
    marginBottom: 16,
  },
  sizeText: {
    fontSize: 14,
    opacity: 0.7,
    marginTop: 8,
  },
  sizeValue: {
    fontSize: 24,
    fontWeight: 'bold',
    marginTop: 4,
  },
  developmentText: {
    textAlign: 'center',
    lineHeight: 20,
    opacity: 0.8,
  },
  milestonesCard: {
    marginBottom: 16,
    padding: 16,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  milestoneItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
  },
  milestoneBorder: {
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.1)',
  },
  milestoneInfo: {
    flex: 1,
  },
  milestoneWeek: {
    fontSize: 12,
    opacity: 0.7,
    marginBottom: 2,
  },
  milestoneTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 4,
  },
  milestoneDescription: {
    fontSize: 14,
    opacity: 0.8,
  },
  milestoneCheck: {
    width: 32,
    height: 32,
    borderRadius: 16,
    borderWidth: 2,
    borderColor: '#E0E0E0',
    alignItems: 'center',
    justifyContent: 'center',
  },
  milestoneCompleted: {
    backgroundColor: '#4CAF50',
    borderColor: '#4CAF50',
  },
  toolsCard: {
    marginBottom: 16,
    padding: 16,
  },
  toolsGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  toolItem: {
    alignItems: 'center',
    flex: 1,
  },
  toolIcon: {
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  toolTitle: {
    fontSize: 12,
    textAlign: 'center',
  },
  healthTips: {
    padding: 16,
    marginBottom: 32,
  },
  tipsList: {
    paddingLeft: 8,
  },
  tip: {
    marginBottom: 8,
    lineHeight: 20,
    opacity: 0.8,
  },
});