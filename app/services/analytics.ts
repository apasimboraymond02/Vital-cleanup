import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

interface AnalyticsEvent {
  name: string;
  properties?: Record<string, any>;
  timestamp?: number;
}

class AnalyticsService {
  private static instance: AnalyticsService;
  private enabled = true;
  private events: AnalyticsEvent[] = [];
  private readonly MAX_EVENTS = 1000;
  private readonly FLUSH_INTERVAL = 60000; // 1 minute
  private flushTimer: NodeJS.Timeout | null = null;

  private constructor() {
    this.loadSettings();
    this.startFlushTimer();
  }

  static getInstance(): AnalyticsService {
    if (!AnalyticsService.instance) {
      AnalyticsService.instance = new AnalyticsService();
    }
    return AnalyticsService.instance;
  }

  private async loadSettings() {
    try {
      const analyticsEnabled = await SecureStore.getItemAsync('analytics_enabled');
      this.enabled = analyticsEnabled !== 'false';
    } catch (error) {
      console.error('Error loading analytics settings:', error);
    }
  }

  async setEnabled(enabled: boolean) {
    this.enabled = enabled;
    try {
      await SecureStore.setItemAsync('analytics_enabled', enabled.toString());
    } catch (error) {
      console.error('Error saving analytics settings:', error);
    }
  }

  track(eventName: string, properties?: Record<string, any>) {
    if (!this.enabled) return;

    const event: AnalyticsEvent = {
      name: eventName,
      properties: {
        ...properties,
        platform: Platform.OS,
        timestamp: Date.now(),
      },
    };

    this.events.push(event);

    // Limit events in memory
    if (this.events.length > this.MAX_EVENTS) {
      this.events = this.events.slice(-this.MAX_EVENTS);
    }

    // Flush immediately for important events
    if (this.isImportantEvent(eventName)) {
      this.flush();
    }
  }

  private isImportantEvent(eventName: string): boolean {
    const importantEvents = [
      'app_launch',
      'app_crash',
      'cycle_logged',
      'pregnancy_started',
      'privacy_settings_changed',
    ];
    return importantEvents.includes(eventName);
  }

  private startFlushTimer() {
    this.flushTimer = setInterval(() => {
      this.flush();
    }, this.FLUSH_INTERVAL);
  }

  private async flush() {
    if (this.events.length === 0) return;

    const eventsToFlush = [...this.events];
    this.events = [];

    try {
      // In production, this would send to your analytics service
      console.log('Flushing analytics events:', eventsToFlush);
      
      // Example: Send to Mixpanel, Amplitude, or your backend
      // await fetch('https://your-analytics-endpoint.com/events', {
      //   method: 'POST',
      //   headers: {
      //     'Content-Type': 'application/json',
      //   },
      //   body: JSON.stringify(eventsToFlush),
      // });
    } catch (error) {
      console.error('Error flushing analytics:', error);
      // Re-add events to retry later
      this.events = [...eventsToFlush, ...this.events];
    }
  }

  async exportEvents(): Promise<AnalyticsEvent[]> {
    return [...this.events];
  }

  async clearEvents() {
    this.events = [];
  }

  dispose() {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
    }
    this.flush();
  }
}

export const analytics = AnalyticsService.getInstance();

// Common events
export const AnalyticsEvents = {
  APP_LAUNCH: 'app_launch',
  APP_BACKGROUND: 'app_background',
  CYCLE_LOGGED: 'cycle_logged',
  CYCLE_UPDATED: 'cycle_updated',
  SYMPTOM_LOGGED: 'symptom_logged',
  PREGNANCY_STARTED: 'pregnancy_started',
  PREGNANCY_UPDATED: 'pregnancy_updated',
  FERTILITY_VIEWED: 'fertility_viewed',
  CONTENT_VIEWED: 'content_viewed',
  SETTINGS_CHANGED: 'settings_changed',
  PRIVACY_CHANGED: 'privacy_changed',
  DATA_EXPORTED: 'data_exported',
  DATA_DELETED: 'data_deleted',
  BIOMETRIC_ENABLED: 'biometric_enabled',
  ERROR_OCCURRED: 'error_occurred',
};