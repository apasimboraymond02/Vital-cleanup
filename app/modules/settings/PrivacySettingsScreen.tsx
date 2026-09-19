import React, { useState } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  Switch,
  Alert,
} from 'react-native';
import { useSelector, useDispatch } from 'react-redux';
import {
  Lock,
  Shield,
  Database,
  Eye,
  EyeOff,
  Download,
  Trash2,
  AlertCircle,
  CheckCircle,
} from 'lucide-react-native';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useTheme } from '../../theme/ThemeContext';
import { useDatabase } from '../../database';
import { updatePrivacy } from '../../store/settingsSlice';
import { DataExportService } from '../../services/exportData';

export const PrivacySettingsScreen = () => {
  const theme = useTheme();
  const dispatch = useDispatch();
  const { db } = useDatabase();
  const { privacy } = useSelector((state: any) => state.settings);
  const [exporting, setExporting] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const handlePrivacyToggle = (key: string, value: boolean | number) => {
    dispatch(updatePrivacy({ [key]: value }));
  };

  const handleExportAllData = async () => {
    setExporting(true);
    try {
      const options = {
        format: privacy.exportFormat as 'json' | 'csv',
        includeCycles: true,
        includeSymptoms: true,
        includePregnancies: true,
        includeHealthMetrics: true,
        includeSettings: true,
      };

      const fileUri = await DataExportService.exportData(options);
      
      Alert.alert(
        'Export Complete',
        'Your data has been exported successfully. Would you like to share it?',
        [
          { text: 'Later', style: 'cancel' },
          {
            text: 'Share',
            onPress: () => DataExportService.shareFile(fileUri),
          },
        ]
      );
    } catch (error) {
      Alert.alert('Error', 'Failed to export data. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  const handleDeleteAllData = () => {
    Alert.alert(
      'Delete All Data',
      'This will permanently delete all your health data from this device. This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete Everything',
          style: 'destructive',
          onPress: async () => {
            setDeleting(true);
            try {
              if (!db) throw new Error('Database not available');

              await new Promise((resolve, reject) => {
                db.transaction(tx => {
                  tx.executeSql('DELETE FROM cycles');
                  tx.executeSql('DELETE FROM symptoms');
                  tx.executeSql('DELETE FROM pregnancies');
                  tx.executeSql('DELETE FROM health_metrics');
                  tx.executeSql('DELETE FROM bookmarks');
                  tx.executeSql('DELETE FROM settings');
                }, reject, resolve);
              });

              Alert.alert('Success', 'All data has been deleted.');
            } catch (error) {
              Alert.alert('Error', 'Failed to delete data. Please try again.');
            } finally {
              setDeleting(false);
            }
          },
        },
      ]
    );
  };

  const renderPrivacySetting = (
    icon: React.ReactNode,
    title: string,
    description: string,
    value: boolean,
    onValueChange: (value: boolean) => void,
    dangerous?: boolean
  ) => {
    return (
      <View style={styles.settingItem}>
        <View style={styles.settingIcon}>
          {icon}
        </View>
        <View style={styles.settingInfo}>
          <Text style={styles.settingTitle}>{title}</Text>
          <Text style={styles.settingDescription}>{description}</Text>
        </View>
        <Switch
          value={value}
          onValueChange={onValueChange}
          trackColor={{ false: '#767577', true: dangerous ? '#F44336' : theme.colors.primary }}
        />
      </View>
    );
  };

  const renderDataPoint = (
    icon: React.ReactNode,
    title: string,
    count: number,
    description: string
  ) => {
    return (
      <View style={styles.dataPoint}>
        <View style={[styles.dataIcon, { backgroundColor: theme.colors.primary + '20' }]}>
          {icon}
        </View>
        <View style={styles.dataInfo}>
          <Text style={styles.dataTitle}>{title}</Text>
          <Text style={styles.dataCount}>{count} records</Text>
          <Text style={styles.dataDescription}>{description}</Text>
        </View>
      </View>
    );
  };

  return (
    <ScrollView 
      style={[styles.container, { backgroundColor: theme.colors.background }]}
      contentContainerStyle={styles.content}
    >
      <View style={styles.header}>
        <Shield size={32} color={theme.colors.primary} />
        <Text style={styles.title}>Privacy & Data</Text>
        <Text style={styles.subtitle}>
          Your health data is encrypted and stays on your device
        </Text>
      </View>

      <Card style={styles.privacyCard}>
        <Text style={styles.sectionTitle}>Privacy Settings</Text>
        
        {renderPrivacySetting(
          <Database size={20} color={theme.colors.primary} />,
          'Local-Only Mode',
          'Keep all data on this device, no cloud sync',
          privacy.localOnly,
          (value) => handlePrivacyToggle('localOnly', value)
        )}

        {renderPrivacySetting(
          <Eye size={20} color={theme.colors.primary} />,
          'Share Anonymized Data',
          'Help improve the app (no personal info)',
          privacy.shareAnonymizedData,
          (value) => handlePrivacyToggle('shareAnonymizedData', value)
        )}

        {renderPrivacySetting(
          <Trash2 size={20} color="#F44336" />,
          'Auto-Delete Old Data',
          `Delete data older than ${privacy.autoDeletePeriod} days`,
          privacy.autoDeletePeriod > 0,
          (value) => handlePrivacyToggle('autoDeletePeriod', value ? 90 : 0)
        )}
      </Card>

      <Card style={styles.dataCard}>
        <Text style={styles.sectionTitle}>Your Data</Text>
        
        <View style={styles.dataPoints}>
          {renderDataPoint(
            <Database size={20} color={theme.colors.primary} />,
            'Cycle Records',
            24,
            'Period logs and cycle history'
          )}
          
          {renderDataPoint(
            <AlertCircle size={20} color={theme.colors.primary} />,
            'Symptoms',
            156,
            'Logged symptoms and notes'
          )}
          
          {renderDataPoint(
            <CheckCircle size={20} color={theme.colors.primary} />,
            'Health Metrics',
            89,
            'Temperature, mood, and other metrics'
          )}
        </View>

        <View style={styles.exportOptions}>
          <Text style={styles.exportTitle}>Export Format:</Text>
          <View style={styles.formatOptions}>
            {(['json', 'csv'] as const).map((format) => (
              <Button
                key={format}
                title={format.toUpperCase()}
                onPress={() => handlePrivacyToggle('exportFormat', format)}
                variant={privacy.exportFormat === format ? 'primary' : 'outline'}
                size="small"
                style={styles.formatButton}
              />
            ))}
          </View>
        </View>

        <Button
          title={exporting ? 'Exporting...' : 'Export All Data'}
          onPress={handleExportAllData}
          loading={exporting}
          leftIcon={<Download size={20} />}
          variant="outline"
          style={styles.exportButton}
        />
      </Card>

      <Card style={[styles.dangerCard, { backgroundColor: 'rgba(244,67,54,0.1)' }]}>
        <Text style={[styles.sectionTitle, { color: '#F44336' }]}>Danger Zone</Text>
        
        <View style={styles.dangerContent}>
          <Trash2 size={24} color="#F44336" />
          <View style={styles.dangerInfo}>
            <Text style={[styles.dangerTitle, { color: '#F44336' }]}>
              Delete All Data
            </Text>
            <Text style={styles.dangerDescription}>
              Permanently remove all your health data from this device.
              This cannot be undone.
            </Text>
          </View>
        </View>

        <Button
          title={deleting ? 'Deleting...' : 'Delete Everything'}
          onPress={handleDeleteAllData}
          loading={deleting}
          variant="outline"
          style={[styles.deleteButton, { borderColor: '#F44336' }]}
          textStyle={{ color: '#F44336' }}
        />
      </Card>

      <View style={styles.privacyPolicy}>
        <Text style={styles.policyTitle}>Our Privacy Promise</Text>
        <Text style={styles.policyText}>
          • Your health data is encrypted and stored locally{'\n'}
          • We never sell or share your personal information{'\n'}
          • You control what data is collected and stored{'\n'}
          • All data processing happens on your device{'\n'}
          • Open source components ensure transparency
        </Text>
      </View>
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
    alignItems: 'center',
    marginBottom: 32,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    marginTop: 16,
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    opacity: 0.7,
    lineHeight: 24,
  },
  privacyCard: {
    marginBottom: 16,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 20,
  },
  settingItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.1)',
  },
  settingIcon: {
    marginRight: 12,
  },
  settingInfo: {
    flex: 1,
  },
  settingTitle: {
    fontSize: 16,
    fontWeight: '500',
    marginBottom: 4,
  },
  settingDescription: {
    fontSize: 12,
    opacity: 0.7,
    lineHeight: 16,
  },
  dataCard: {
    marginBottom: 16,
    padding: 16,
  },
  dataPoints: {
    marginBottom: 20,
  },
  dataPoint: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  dataIcon: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 16,
  },
  dataInfo: {
    flex: 1,
  },
  dataTitle: {
    fontSize: 16,
    fontWeight: '500',
    marginBottom: 2,
  },
  dataCount: {
    fontSize: 14,
    opacity: 0.7,
    marginBottom: 4,
  },
  dataDescription: {
    fontSize: 12,
    opacity: 0.6,
  },
  exportOptions: {
    marginBottom: 20,
  },
  exportTitle: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
  },
  formatOptions: {
    flexDirection: 'row',
  },
  formatButton: {
    marginRight: 8,
  },
  exportButton: {
    marginBottom: 8,
  },
  dangerCard: {
    marginBottom: 32,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(244,67,54,0.2)',
  },
  dangerContent: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: 20,
  },
  dangerInfo: {
    flex: 1,
    marginLeft: 12,
  },
  dangerTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 4,
  },
  dangerDescription: {
    fontSize: 14,
    opacity: 0.8,
    lineHeight: 20,
  },
  deleteButton: {
    borderWidth: 2,
  },
  privacyPolicy: {
    padding: 20,
    backgroundColor: 'rgba(0,0,0,0.03)',
    borderRadius: 12,
  },
  policyTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 12,
  },
  policyText: {
    fontSize: 14,
    lineHeight: 24,
    opacity: 0.8,
  },
});