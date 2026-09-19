import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

export interface NotificationSchedule {
  id: string;
  title: string;
  body: string;
  date: Date;
  repeat?: 'daily' | 'weekly' | 'monthly';
  data?: Record<string, any>;
}

export class NotificationService {
  private static instance: NotificationService;

  static getInstance(): NotificationService {
    if (!NotificationService.instance) {
      NotificationService.instance = new NotificationService();
    }
    return NotificationService.instance;
  }

  async registerForPushNotifications(): Promise<string | null> {
    if (!Device.isDevice) {
      console.log('Must use physical device for Push Notifications');
      return null;
    }

    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;

    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }

    if (finalStatus !== 'granted') {
      console.log('Failed to get push token for push notification!');
      return null;
    }

    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('default', {
        name: 'default',
        importance: Notifications.AndroidImportance.MAX,
        vibrationPattern: [0, 250, 250, 250],
        lightColor: '#FF231F7C',
      });
    }

    const token = (await Notifications.getExpoPushTokenAsync()).data;
    return token;
  }

  async schedulePeriodReminder(date: Date): Promise<string> {
    const trigger = this.getTriggerForDate(date);
    
    const notificationId = await Notifications.scheduleNotificationAsync({
      content: {
        title: 'Period Reminder',
        body: 'Time to log your period start',
        data: { type: 'period_reminder' },
      },
      trigger,
    });

    await this.saveNotification('period_reminder', notificationId);
    return notificationId;
  }

  async scheduleOvulationReminder(date: Date): Promise<string> {
    const trigger = this.getTriggerForDate(date);
    
    const notificationId = await Notifications.scheduleNotificationAsync({
      content: {
        title: 'Ovulation Window',
        body: 'Your fertile window is starting soon',
        data: { type: 'ovulation_reminder' },
      },
      trigger,
    });

    await this.saveNotification('ovulation_reminder', notificationId);
    return notificationId;
  }

  async scheduleSymptomReminder(hour: number, minute: number): Promise<string> {
    const trigger: Notifications.DailyTriggerInput = {
      hour,
      minute,
      repeats: true,
    };

    const notificationId = await Notifications.scheduleNotificationAsync({
      content: {
        title: 'Daily Symptom Check-in',
        body: 'How are you feeling today? Log your symptoms',
        data: { type: 'symptom_reminder' },
      },
      trigger,
    });

    await this.saveNotification('symptom_reminder', notificationId);
    return notificationId;
  }

  async schedulePregnancyReminder(week: number, day: number, message: string): Promise<string> {
    const date = new Date();
    date.setDate(date.getDate() + (week * 7) + day);

    const trigger = this.getTriggerForDate(date);
    
    const notificationId = await Notifications.scheduleNotificationAsync({
      content: {
        title: `Week ${week} Update`,
        body: message,
        data: { type: 'pregnancy_update', week },
      },
      trigger,
    });

    await this.saveNotification('pregnancy_update', notificationId);
    return notificationId;
  }

  async cancelNotification(notificationId: string): Promise<void> {
    await Notifications.cancelScheduledNotificationAsync(notificationId);
    await this.removeNotification(notificationId);
  }

  async cancelAllNotifications(): Promise<void> {
    await Notifications.cancelAllScheduledNotificationsAsync();
    await AsyncStorage.removeItem('scheduled_notifications');
  }

  async getScheduledNotifications(): Promise<Notifications.NotificationRequest[]> {
    return await Notifications.getScheduledNotificationsAsync();
  }

  private getTriggerForDate(date: Date): Notifications.DateTriggerInput {
    return {
      date,
      repeats: false,
    };
  }

  private async saveNotification(type: string, notificationId: string): Promise<void> {
    try {
      const existing = await AsyncStorage.getItem('scheduled_notifications');
      const notifications = existing ? JSON.parse(existing) : {};
      notifications[type] = notificationId;
      await AsyncStorage.setItem('scheduled_notifications', JSON.stringify(notifications));
    } catch (error) {
      console.error('Error saving notification:', error);
    }
  }

  private async removeNotification(notificationId: string): Promise<void> {
    try {
      const existing = await AsyncStorage.getItem('scheduled_notifications');
      if (existing) {
        const notifications = JSON.parse(existing);
        for (const [type, id] of Object.entries(notifications)) {
          if (id === notificationId) {
            delete notifications[type];
            break;
          }
        }
        await AsyncStorage.setItem('scheduled_notifications', JSON.stringify(notifications));
      }
    } catch (error) {
      console.error('Error removing notification:', error);
    }
  }

  async checkAndScheduleNotifications(userSettings: any): Promise<void> {
    // Clear existing notifications
    await this.cancelAllNotifications();

    // Schedule based on user preferences
    if (userSettings.notifications.cycleReminders) {
      // Schedule period reminders based on cycle prediction
      const nextPeriod = this.calculateNextPeriod(userSettings.cycleSettings);
      if (nextPeriod) {
        const reminderDate = new Date(nextPeriod);
        reminderDate.setDate(reminderDate.getDate() - 1); // Remind 1 day before
        await this.schedulePeriodReminder(reminderDate);
      }
    }

    if (userSettings.notifications.symptomReminders) {
      // Schedule daily symptom check-in at 8 PM
      await this.scheduleSymptomReminder(20, 0);
    }

    if (userSettings.notifications.medicationReminders) {
      // Schedule medication reminders
      // This would be customized based on user's medication schedule
    }

    if (userSettings.notifications.appointmentReminders) {
      // Schedule appointment reminders
      // This would sync with calendar
    }
  }

  private calculateNextPeriod(cycleSettings: any): Date | null {
    // Simplified calculation - in production, use actual cycle data
    const avgLength = cycleSettings.averageLength || 28;
    const nextPeriod = new Date();
    nextPeriod.setDate(nextPeriod.getDate() + avgLength);
    return nextPeriod;
  }
}

export const notificationService = NotificationService.getInstance();