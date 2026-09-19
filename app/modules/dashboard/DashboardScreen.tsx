import React, { useState, useEffect } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
} from 'react-native';
import { useSelector } from 'react-redux';
import {
  Calendar,
  Heart,
  Baby,
  Bell,
  TrendingUp,
  AlertCircle,
  BookOpen,
  ChevronRight,
} from 'lucide-react-native';
import { VictoryPie } from 'victory-native';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useDatabase } from '../../database';
import { useTheme } from '../../theme/ThemeContext';
import { cycleUtils } from '../cycle/cycleUtils';

export const DashboardScreen = () => {
  const theme = useTheme();
  const { db } = useDatabase();
  const settings = useSelector((state: any) => state.settings);
  const [cycleStats, setCycleStats] = useState<any>(null);
  const [pregnancyStats, setPregnancyStats] = useState<any>(null);
  const [upcomingReminders, setUpcomingReminders] = useState<any[]>([]);
  const [healthTips, setHealthTips] = useState<any[]>([]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    if (!db) return;

    try {
      // Load cycle data
      db.transaction(tx => {
        tx.executeSql(
          `SELECT * FROM cycles WHERE is_deleted = 0 ORDER BY start_date DESC LIMIT 6`,
          [],
          (_, { rows }) => {
            const cycles = rows._array.map(row => ({
              startDate: new Date(row.start_date),
              endDate: row.end_date ? new Date(row.end_date) : undefined,
              flowIntensity: row.flow_intensity,
              mood: row.mood,
              symptoms: row.symptoms ? JSON.parse(row.symptoms) : [],
            }));

            const stats = cycleUtils.calculateCycleStats(cycles);
            setCycleStats(stats);
          }
        );

        // Load pregnancy data
        tx.executeSql(
          `SELECT * FROM pregnancies ORDER BY start_date DESC LIMIT 1`,
          [],
          (_, { rows }) => {
            if (rows.length > 0) {
              const pregnancy = rows._array[0];
              const today = new Date();
              const start = new Date(pregnancy.start_date);
              const weeks = Math.floor((today.getTime() - start.getTime()) / (1000 * 60 * 60 * 24 * 7));
              
              setPregnancyStats({
                weeks,
                dueDate: new Date(pregnancy.due_date),
                trimester: weeks <= 13 ? 'First' : weeks <= 26 ? 'Second' : 'Third',
              });
            }
          }
        );

        // Load reminders
        tx.executeSql(
          `SELECT * FROM health_metrics WHERE logged_date >= date('now', '-7 days')`,
          [],
          (_, { rows }) => {
            const metrics = rows._array;
            // Create reminders based on data
            const reminders = [
              { id: '1', type: 'cycle', title: 'Period due in 3 days', time: '2 days ago', icon: Calendar },
              { id: '2', type: 'health', title: 'Log today\'s symptoms', time: 'Today', icon: Bell },
              { id: '3', type: 'appointment', title: 'Annual check-up next week', time: 'Next week', icon: Calendar },
            ];
            setUpcomingReminders(reminders);
          }
        );
      });

      // Load health tips
      const tips = [
        { id: '1', title: 'Stay Hydrated', description: 'Drink at least 8 glasses of water daily', category: 'nutrition' },
        { id: '2', title: 'Practice Mindfulness', description: '5 minutes of meditation daily', category: 'mental_health' },
        { id: '3', title: 'Regular Exercise', description: '30 minutes of moderate activity', category: 'exercise' },
      ];
      setHealthTips(tips);
    } catch (error) {
      console.error('Dashboard load error:', error);
    }
  };

  const renderCycleOverview = () => {
    if (!cycleStats) return null;

    const phase = cycleUtils.getCyclePhase(
      cycleStats.currentDay || 14,
      cycleStats.averageLength
    );

    const phaseColors = {
      menstrual: '#FF5252',
      follicular: '#4CAF50',
      ovulation: '#FF9800',
      luteal: '#2196F3',
    };

    return (
      <Card style={styles.overviewCard}>
        <View style={styles.overviewHeader}>
          <View style={styles.overviewTitle}>
            <Calendar size={24} color={theme.colors.primary} />
            <Text style={styles.cardTitle}>Cycle Overview</Text>
          </View>
          <TouchableOpacity>
            <ChevronRight size={20} color={theme.colors.textSecondary} />
          </TouchableOpacity>
        </View>

        <View style={styles.cycleStats}>
          <View style={styles.statItem}>
            <Text style={styles.statLabel}>Current Phase</Text>
            <View style={styles.phaseBadge}>
              <View 
                style={[
                  styles.phaseDot, 
                  { backgroundColor: phaseColors[phase as keyof typeof phaseColors] || theme.colors.primary }
                ]} 
              />
              <Text style={styles.statValue}>
                {phase.charAt(0).toUpperCase() + phase.slice(1)}
              </Text>
            </View>
          </View>

          <View style={styles.statItem}>
            <Text style={styles.statLabel}>Avg. Length</Text>
            <Text style={styles.statValue}>{cycleStats.averageLength} days</Text>
          </View>

          <View style={styles.statItem}>
            <Text style={styles.statLabel}>Regularity</Text>
            <Text style={[
              styles.statValue,
              { color: cycleStats.regularity === 'Regular' ? theme.colors.success : theme.colors.warning }
            ]}>
              {cycleStats.regularity}
            </Text>
          </View>
        </View>

        {cycleStats.lastPeriod && (
          <View style={styles.lastPeriod}>
            <Text style={styles.lastPeriodLabel}>Last Period</Text>
            <Text style={styles.lastPeriodDate}>
              {cycleUtils.formatRelativeDate(cycleStats.lastPeriod)}
            </Text>
          </View>
        )}
      </Card>
    );
  };

  const renderPregnancyOverview = () => {
    if (!pregnancyStats) return null;

    return (
      <Card style={styles.pregnancyCard}>
        <View style={styles.overviewHeader}>
          <View style={styles.overviewTitle}>
            <Baby size={24} color={theme.colors.primary} />
            <Text style={styles.cardTitle}>Pregnancy</Text>
          </View>
          <TouchableOpacity>
            <ChevronRight size={20} color={theme.colors.textSecondary} />
          </TouchableOpacity>
        </View>

        <View style={styles.pregnancyProgress}>
          <Text style={styles.weeksText}>{pregnancyStats.weeks}</Text>
          <Text style={styles.weeksLabel}>weeks</Text>
        </View>

        <View style={styles.pregnancyDetails}>
          <View style={styles.pregnancyDetail}>
            <Text style={styles.detailLabel}>Trimester</Text>
            <Text style={styles.detailValue}>{pregnancyStats.trimester}</Text>
          </View>
          <View style={styles.pregnancyDetail}>
            <Text style={styles.detailLabel}>Due Date</Text>
            <Text style={styles.detailValue}>
              {pregnancyStats.dueDate.toLocaleDateString()}
            </Text>
          </View>
        </View>
      </Card>
    );
  };

  const renderFertilityChart = () => {
    const fertilityData = [
      { x: 'Low', y: 15 },
      { x: 'Medium', y: 6 },
      { x: 'High', y: 5 },
      { x: 'Ovulation', y: 1 },
    ];

    const colorScale = ['#E8F5E9', '#FFF3E0', '#FFEBEE', '#FF5252'];

    return (
      <Card style={styles.fertilityCard}>
        <View style={styles.overviewHeader}>
          <View style={styles.overviewTitle}>
            <Heart size={24} color={theme.colors.primary} />
            <Text style={styles.cardTitle}>Monthly Fertility</Text>
          </View>
        </View>

        <View style={styles.chartContainer}>
          <VictoryPie
            data={fertilityData}
            colorScale={colorScale}
            height={200}
            padding={40}
            innerRadius={50}
            labels={() => null}
          />
          <View style={styles.chartCenter}>
            <Text style={styles.chartCenterText}>27</Text>
            <Text style={styles.chartCenterLabel}>Day Cycle</Text>
          </View>
        </View>

        <View style={styles.legend}>
          {fertilityData.map((item, index) => (
            <View key={index} style={styles.legendItem}>
              <View style={[styles.legendColor, { backgroundColor: colorScale[index] }]} />
              <Text style={styles.legendText}>{item.x}: {item.y} days</Text>
            </View>
          ))}
        </View>
      </Card>
    );
  };

  const renderReminders = () => {
    return (
      <Card style={styles.remindersCard}>
        <View style={styles.overviewHeader}>
          <Text style={styles.cardTitle}>Reminders</Text>
          <TouchableOpacity>
            <Text style={styles.seeAllText}>See All</Text>
          </TouchableOpacity>
        </View>

        {upcomingReminders.map((reminder, index) => {
          const Icon = reminder.icon;
          return (
            <TouchableOpacity
              key={reminder.id}
              style={[
                styles.reminderItem,
                index < upcomingReminders.length - 1 && styles.reminderBorder,
              ]}
            >
              <View style={styles.reminderIcon}>
                <Icon size={20} color={theme.colors.primary} />
              </View>
              <View style={styles.reminderInfo}>
                <Text style={styles.reminderTitle}>{reminder.title}</Text>
                <Text style={styles.reminderTime}>{reminder.time}</Text>
              </View>
              <ChevronRight size={20} color={theme.colors.textSecondary} />
            </TouchableOpacity>
          );
        })}
      </Card>
    );
  };

  const renderHealthTips = () => {
    return (
      <Card style={styles.tipsCard}>
        <View style={styles.overviewHeader}>
          <Text style={styles.cardTitle}>Today's Health Tips</Text>
          <TouchableOpacity>
            <BookOpen size={20} color={theme.colors.primary} />
          </TouchableOpacity>
        </View>

        {healthTips.map((tip, index) => (
          <TouchableOpacity
            key={tip.id}
            style={[
              styles.tipItem,
              index < healthTips.length - 1 && styles.tipBorder,
            ]}
          >
            <View style={styles.tipContent}>
              <Text style={styles.tipTitle}>{tip.title}</Text>
              <Text style={styles.tipDescription}>{tip.description}</Text>
            </View>
            <View style={[styles.tipCategory, { backgroundColor: theme.colors.primary + '20' }]}>
              <Text style={[styles.tipCategoryText, { color: theme.colors.primary }]}>
                {tip.category.replace('_', ' ')}
              </Text>
            </View>
          </TouchableOpacity>
        ))}
      </Card>
    );
  };

  const renderQuickActions = () => {
    const actions = [
      { icon: Calendar, label: 'Log Period', color: '#9C27B0' },
      { icon: Heart, label: 'Log Symptoms', color: '#FF4081' },
      { icon: Bell, label: 'Set Reminder', color: '#2196F3' },
      { icon: BookOpen, label: 'Read Article', color: '#4CAF50' },
    ];

    return (
      <Card style={styles.actionsCard}>
        <Text style={styles.cardTitle}>Quick Actions</Text>
        <View style={styles.actionsGrid}>
          {actions.map((action, index) => (
            <TouchableOpacity
              key={index}
              style={styles.actionItem}
            >
              <View style={[styles.actionIcon, { backgroundColor: action.color + '20' }]}>
                <action.icon size={24} color={action.color} />
              </View>
              <Text style={styles.actionLabel}>{action.label}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </Card>
    );
  };

  return (
    <ScrollView 
      style={[styles.container, { backgroundColor: theme.colors.background }]}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
    >
      <View style={styles.header}>
        <View>
          <Text style={styles.greeting}>Good Morning</Text>
          <Text style={styles.date}>
            {new Date().toLocaleDateString('en-US', { 
              weekday: 'long', 
              year: 'numeric', 
              month: 'long', 
              day: 'numeric' 
            })}
          </Text>
        </View>
        <TouchableOpacity style={styles.notificationButton}>
          <Bell size={24} color={theme.colors.text} />
          <View style={styles.notificationBadge} />
        </TouchableOpacity>
      </View>

      {renderCycleOverview()}
      {renderPregnancyOverview()}
      {renderFertilityChart()}
      {renderReminders()}
      {renderHealthTips()}
      {renderQuickActions()}

      <Card style={styles.disclaimerCard}>
        <AlertCircle size={20} color={theme.colors.warning} />
        <Text style={styles.disclaimerText}>
          This information is for educational purposes only. Consult a healthcare 
          professional for medical advice.
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
    paddingBottom: 32,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 24,
  },
  greeting: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  date: {
    fontSize: 14,
    opacity: 0.7,
  },
  notificationButton: {
    position: 'relative',
    padding: 8,
  },
  notificationBadge: {
    position: 'absolute',
    top: 6,
    right: 6,
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#FF4081',
  },
  overviewCard: {
    marginBottom: 16,
    padding: 16,
  },
  overviewHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  overviewTitle: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginLeft: 12,
  },
  cycleStats: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  statItem: {
    alignItems: 'center',
    flex: 1,
  },
  statLabel: {
    fontSize: 12,
    opacity: 0.7,
    marginBottom: 4,
  },
  statValue: {
    fontSize: 16,
    fontWeight: '600',
  },
  phaseBadge: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  phaseDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 6,
  },
  lastPeriod: {
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: 'rgba(0,0,0,0.1)',
  },
  lastPeriodLabel: {
    fontSize: 12,
    opacity: 0.7,
    marginBottom: 4,
  },
  lastPeriodDate: {
    fontSize: 14,
    fontWeight: '600',
  },
  pregnancyCard: {
    marginBottom: 16,
    padding: 16,
  },
  pregnancyProgress: {
    alignItems: 'center',
    marginBottom: 16,
  },
  weeksText: {
    fontSize: 48,
    fontWeight: 'bold',
  },
  weeksLabel: {
    fontSize: 14,
    opacity: 0.7,
  },
  pregnancyDetails: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  pregnancyDetail: {
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
  fertilityCard: {
    marginBottom: 16,
    padding: 16,
  },
  chartContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
    position: 'relative',
  },
  chartCenter: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
  chartCenterText: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  chartCenterLabel: {
    fontSize: 12,
    opacity: 0.7,
  },
  legend: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: 16,
    marginBottom: 8,
  },
  legendColor: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 6,
  },
  legendText: {
    fontSize: 12,
    opacity: 0.8,
  },
  remindersCard: {
    marginBottom: 16,
    padding: 16,
  },
  seeAllText: {
    fontSize: 14,
    color: '#2196F3',
    fontWeight: '500',
  },
  reminderItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
  },
  reminderBorder: {
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.1)',
  },
  reminderIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(156, 39, 176, 0.1)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  reminderInfo: {
    flex: 1,
  },
  reminderTitle: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 2,
  },
  reminderTime: {
    fontSize: 12,
    opacity: 0.6,
  },
  tipsCard: {
    marginBottom: 16,
    padding: 16,
  },
  tipItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 12,
  },
  tipBorder: {
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.1)',
  },
  tipContent: {
    flex: 1,
    marginRight: 12,
  },
  tipTitle: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 4,
  },
  tipDescription: {
    fontSize: 12,
    opacity: 0.7,
    lineHeight: 16,
  },
  tipCategory: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
  },
  tipCategoryText: {
    fontSize: 10,
    fontWeight: '500',
  },
  actionsCard: {
    marginBottom: 16,
    padding: 16,
  },
  actionsGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 12,
  },
  actionItem: {
    alignItems: 'center',
    flex: 1,
  },
  actionIcon: {
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  actionLabel: {
    fontSize: 12,
    textAlign: 'center',
  },
  disclaimerCard: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    padding: 16,
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