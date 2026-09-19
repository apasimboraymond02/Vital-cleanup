import * as FileSystem from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { useDatabase } from '../database';

export interface ExportOptions {
  format: 'json' | 'csv';
  includeCycles: boolean;
  includeSymptoms: boolean;
  includePregnancies: boolean;
  includeHealthMetrics: boolean;
  includeSettings: boolean;
  startDate?: Date;
  endDate?: Date;
}

export class DataExportService {
  static async exportData(options: ExportOptions): Promise<string> {
    try {
      const data: any = {
        metadata: {
          exportDate: new Date().toISOString(),
          appVersion: '1.0.0',
          format: options.format,
          platform: Platform.OS,
        },
        data: {},
      };

      // Collect data based on options
      if (options.includeCycles) {
        data.data.cycles = await this.exportCycles(options);
      }

      if (options.includeSymptoms) {
        data.data.symptoms = await this.exportSymptoms(options);
      }

      if (options.includePregnancies) {
        data.data.pregnancies = await this.exportPregnancies();
      }

      if (options.includeHealthMetrics) {
        data.data.healthMetrics = await this.exportHealthMetrics(options);
      }

      if (options.includeSettings) {
        data.data.settings = await this.exportSettings();
      }

      // Generate file
      let fileContent: string;
      let fileExtension: string;

      if (options.format === 'json') {
        fileContent = JSON.stringify(data, null, 2);
        fileExtension = 'json';
      } else {
        fileContent = this.convertToCSV(data);
        fileExtension = 'csv';
      }

      // Save file
      const fileName = `health_data_export_${Date.now()}.${fileExtension}`;
      const fileUri = FileSystem.documentDirectory + fileName;

      await FileSystem.writeAsStringAsync(fileUri, fileContent);

      return fileUri;
    } catch (error) {
      console.error('Export error:', error);
      throw new Error('Failed to export data');
    }
  }

  private static async exportCycles(options: ExportOptions): Promise<any[]> {
    const { db } = useDatabase();
    if (!db) return [];

    return new Promise((resolve) => {
      let query = 'SELECT * FROM cycles WHERE is_deleted = 0';
      const params: any[] = [];

      if (options.startDate) {
        query += ' AND start_date >= ?';
        params.push(options.startDate.toISOString());
      }

      if (options.endDate) {
        query += ' AND start_date <= ?';
        params.push(options.endDate.toISOString());
      }

      query += ' ORDER BY start_date DESC';

      db.transaction(tx => {
        tx.executeSql(query, params, (_, { rows }) => {
          resolve(rows._array);
        });
      });
    });
  }

  private static async exportSymptoms(options: ExportOptions): Promise<any[]> {
    const { db } = useDatabase();
    if (!db) return [];

    return new Promise((resolve) => {
      let query = 'SELECT * FROM symptoms';
      const params: any[] = [];

      if (options.startDate) {
        query += ' WHERE logged_date >= ?';
        params.push(options.startDate.toISOString());
      }

      if (options.endDate) {
        query += query.includes('WHERE') ? ' AND' : ' WHERE';
        query += ' logged_date <= ?';
        params.push(options.endDate.toISOString());
      }

      query += ' ORDER BY logged_date DESC';

      db.transaction(tx => {
        tx.executeSql(query, params, (_, { rows }) => {
          resolve(rows._array);
        });
      });
    });
  }

  private static async exportPregnancies(): Promise<any[]> {
    const { db } = useDatabase();
    if (!db) return [];

    return new Promise((resolve) => {
      db.transaction(tx => {
        tx.executeSql(
          'SELECT * FROM pregnancies ORDER BY start_date DESC',
          [],
          (_, { rows }) => {
            resolve(rows._array);
          }
        );
      });
    });
  }

  private static async exportHealthMetrics(options: ExportOptions): Promise<any[]> {
    const { db } = useDatabase();
    if (!db) return [];

    return new Promise((resolve) => {
      let query = 'SELECT * FROM health_metrics';
      const params: any[] = [];

      if (options.startDate) {
        query += ' WHERE logged_date >= ?';
        params.push(options.startDate.toISOString());
      }

      if (options.endDate) {
        query += query.includes('WHERE') ? ' AND' : ' WHERE';
        query += ' logged_date <= ?';
        params.push(options.endDate.toISOString());
      }

      query += ' ORDER BY logged_date DESC';

      db.transaction(tx => {
        tx.executeSql(query, params, (_, { rows }) => {
          resolve(rows._array);
        });
      });
    });
  }

  private static async exportSettings(): Promise<any> {
    try {
      const settings: any = {};

      // Export secure store items (excluding sensitive data)
      const keys = ['user_id', 'analytics_enabled', 'biometric_enabled'];
      
      for (const key of keys) {
        const value = await SecureStore.getItemAsync(key);
        if (value) {
          settings[key] = value;
        }
      }

      return settings;
    } catch (error) {
      console.error('Error exporting settings:', error);
      return {};
    }
  }

  private static convertToCSV(data: any): string {
    const csvLines: string[] = [];

    // Add metadata
    csvLines.push('Section,Key,Value');
    for (const [key, value] of Object.entries(data.metadata)) {
      csvLines.push(`metadata,${key},${JSON.stringify(value)}`);
    }

    // Add data sections
    for (const [section, items] of Object.entries(data.data)) {
      if (Array.isArray(items) && items.length > 0) {
        // Add headers
        const headers = Object.keys(items[0]);
        csvLines.push(`${section},${headers.join(',')}`);
        
        // Add rows
        for (const item of items) {
          const values = headers.map(header => {
            const value = item[header];
            if (typeof value === 'object') {
              return JSON.stringify(value);
            }
            return String(value);
          });
          csvLines.push(`${section},${values.join(',')}`);
        }
      }
    }

    return csvLines.join('\n');
  }

  static async shareFile(fileUri: string): Promise<void> {
    try {
      if (await Sharing.isAvailableAsync()) {
        await Sharing.shareAsync(fileUri, {
          mimeType: 'text/csv',
          dialogTitle: 'Share Health Data Export',
        });
      } else {
        throw new Error('Sharing not available');
      }
    } catch (error) {
      console.error('Share error:', error);
      throw error;
    }
  }

  static async deleteExportFile(fileUri: string): Promise<void> {
    try {
      await FileSystem.deleteAsync(fileUri);
    } catch (error) {
      console.error('Delete file error:', error);
    }
  }
}