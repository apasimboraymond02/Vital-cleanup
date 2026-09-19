import React, { useState } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  Switch,
  TouchableOpacity,
  Alert,
  Linking,
} from 'react-native';
import { useSelector, useDispatch } from 'react-redux';
import {
  User,
  Settings,
  Lock,
  Bell,
  Moon,
  Globe,
  Download,
  Trash2,
  Shield,
  HelpCircle,
  LogOut,
  ChevronRight,
} from 'lucide-react-native';
import * as Sharing from 'expo-sharing';
import * as FileSystem from 'expo-file-system';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useTheme } from '../../theme/ThemeContext';
import { useDatabase } from '../../database';
import {
  updateNotifications,
  updatePrivacy,
  updateHealthProfile,
  setTheme,
  setLanguage,
  resetSettings,
} from '../../store/settingsSlice';
import { logout } from '../../store/authSlice';

export const ProfileScreen = () => {
  const theme = useTheme();
  const dispatch = useDispatch();
  const { db, encryptData } = useDatabase();
  const settings = useSelector((state: any) => state.settings);
  const auth = useSelector((state: any) => state.auth);

  const [exporting, setExporting] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const handleNotificationToggle = (key: string, value: boolean) => {
    dispatch(updateNotifications({ [key]: value }));
  };

  const handlePrivacyToggle = (key: string, value: boolean | number) => {
    dispatch(updatePrivacy({ [key]: value }));
  };

  const handleThemeChange = (themeMode: 'light' | 'dark' | 'system') => {
    dispatch(setTheme(themeMode));
  };

  const exportData = async () => {
    setExporting(true);
    try {
      if (!db) throw new Error('Database not available');

      const data: any = {};

      // Export cycles
      const cycles = await new Promise((resolve) => {
        db.transaction(tx => {
          tx.executeSql(
            'SELECT * FROM cycles WHERE is_deleted = 0',
            [],
            (_, { rows }) => resolve(rows._array)
          );
        });
      });

      // Export symptoms
      const symptoms = await new Promise((resolve) => {
        db.transaction(tx => {
          tx.executeSql(
            'SELECT * FROM symptoms',
            [],
            (_, { rows }) => resolve(rows._array)
          );
        });
      });

      // Export pregnancies
      const pregnancies = await new Promise((resolve) => {
        db.transaction(tx => {
          tx.executeSql(
            'SELECT * FROM pregnancies',
            [],
            (_, { rows }) => resolve(rows._array)
          );
        });
      });

      data.cycles = cycles;
      data.symptoms = symptoms;
      data.pregnancies = pregnancies;
      data.settings = settings;
      data.exportDate = new Date().toISOString();
      data.appVersion = '1.0.0';

      const jsonData = JSON.stringify(data, null, 2);
      const fileUri = FileSystem.documentDirectory + 'health_data_export.json';
      
      await FileSystem.writeAsStringAsync(fileUri, jsonData);
      
      if (await Sharing.isAvailableAsync()) {
        await Sharing.shareAsync(fileUri, {
          mimeType: 'application/json',
          dialogTitle: 'Export Health Data',
        });
      } else {
        Alert.alert(
          'Export Complete',
          `Data exported to: ${fileUri}`,
          [{ text: 'OK' }]
        );
      }
    } catch (error) {
      console.error('Export error:', error);
      Alert.alert('Error', 'Failed to export data. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  const deleteData = () => {
    Alert.alert(
      'Delete All Data',
      'This will permanently delete all your health data. This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
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
                }, reject, resolve);
              });

              dispatch(resetSettings());
              Alert.alert('Success', 'All data has been deleted.');
            } catch (error) {
              console.error('Delete error:', error);
              Alert.alert('Error', 'Failed to delete data. Please try again.');
            } finally {
              setDeleting(false);
            }
          },
        },
      ]
    );
  };

  const openPrivacyPolicy = () => {
    Linking.openURL('https://example.com/privacy-policy');
  };

  const openTerms = () => {
    Linking.openURL('https://example.com/terms');
  };

  const openSupport = () => {
    Linking.openURL('mailto:support@example.com');
  };

  const handleLogout = () => {
    Alert.alert(
      'Log Out',
      'Are you sure you want to log out?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Log Out',
          style: 'destructive',
          onPress: () => dispatch(logout()),
        },
      ]
    );
  };

  const renderSection = (title: string, children: React.ReactNode) => (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      <Card style={styles.sectionCard}>{children}</Card>
    </View>
  );

  return (
    <ScrollView 
      style={[styles.container, { backgroundColor: theme.colors.background }]}
      contentContainerStyle={styles.content}
    >
      <View style={styles.header}>
        <View style={[styles.avatar, { backgroundColor: theme.colors.primary }]}>
          <User size={32} color="#FFFFFF" />
        </View>
        <View style={styles.userInfo}>
          <Text style={styles.userName}>Women's Health User</Text>
          <Text style={styles.userId}>ID: {auth.user?.id?.substring(0, 8)}...</Text>
        </View>
      </View>

      {renderSection('Account Settings', (
        <>
          <TouchableOpacity style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <User size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Health Profile</Text>
            </View>
            <ChevronRight size={20} color={theme.colors.textSecondary} />
          </TouchableOpacity>

          <TouchableOpacity style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Bell size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Notifications</Text>
            </View>
            <ChevronRight size={20} color={theme.colors.textSecondary} />
          </TouchableOpacity>

          <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Moon size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Theme</Text>
            </View>
            <View style={styles.themeOptions}>
              {(['light', 'dark', 'system'] as const).map((themeMode) => (
                <TouchableOpacity
                  key={themeMode}
                  style={[
                    styles.themeOption,
                    settings.theme === themeMode && styles.themeOptionSelected,
                    settings.theme === themeMode && { backgroundColor: theme.colors.primary },
                  ]}
                  onPress={() => handleThemeChange(themeMode)}
                >
                  <Text style={[
                    styles.themeOptionText,
                    settings.theme === themeMode && { color: '#FFFFFF' },
                  ]}>
                    {themeMode.charAt(0).toUpperCase() + themeMode.slice(1)}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Globe size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Language</Text>
            </View>
            <Text style={styles.settingValue}>{settings.language.toUpperCase()}</Text>
          </View>
        </>
      ))}

      {renderSection('Privacy & Security', (
        <>
          <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Lock size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Local-Only Mode</Text>
            </View>
            <Switch
              value={settings.privacy.localOnly}
              onValueChange={(value) => handlePrivacyToggle('localOnly', value)}
              trackColor={{ false: '#767577', true: theme.colors.primary }}
            />
          </View>

          <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Shield size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Biometric Lock</Text>
            </View>
            <Switch
              value={auth.biometricEnabled}
              onValueChange={() => {/* Toggle biometric */}}
              trackColor={{ false: '#767577', true: theme.colors.primary }}
            />
          </View>

          <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Text style={styles.settingLabel}>Auto-delete Old Data</Text>
              <Text style={styles.settingDescription}>
                Delete data older than {settings.privacy.autoDeletePeriod} days
              </Text>
            </View>
            <Switch
              value={settings.privacy.autoDeletePeriod > 0}
              onValueChange={(value) => handlePrivacyToggle('autoDeletePeriod', value ? 90 : 0)}
              trackColor={{ false: '#767577', true: theme.colors.primary }}
            />
          </View>

          <TouchableOpacity style={styles.settingItem} onPress={openPrivacyPolicy}>
            <Text style={styles.settingLabel}>Privacy Policy</Text>
            <ChevronRight size={20} color={theme.colors.textSecondary} />
          </TouchableOpacity>

          <TouchableOpacity style={styles.settingItem} onPress={openTerms}>
            <Text style={styles.settingLabel}>Terms of Service</Text>
            <ChevronRight size={20} color={theme.colors.textSecondary} />
          </TouchableOpacity>
        </>
      ))}

      {renderSection('Data Management', (
        <>
          <TouchableOpacity 
            style={styles.settingItem}
            onPress={exportData}
            disabled={exporting}
          >
            <View style={styles.settingLeft}>
              <Download size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>
                {exporting ? 'Exporting...' : 'Export All Data'}
              </Text>
            </View>
            <Text style={styles.settingValue}>
              {settings.privacy.exportFormat.toUpperCase()}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.settingItem, styles.dangerItem]}
            onPress={deleteData}
            disabled={deleting}
          >
            <View style={styles.settingLeft}>
              <Trash2 size={20} color="#F44336" />
              <Text style={[styles.settingLabel, styles.dangerText]}>
                {deleting ? 'Deleting...' : 'Delete All Data'}
              </Text>
            </View>
            <ChevronRight size={20} color="#F44336" />
          </TouchableOpacity>
        </>
      ))}

      {renderSection('Support', (
        <>
          <TouchableOpacity style={styles.settingItem} onPress={openSupport}>
            <View style={styles.settingLeft}>
              <HelpCircle size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Help & Support</Text>
            </View>
            <ChevronRight size={20} color={theme.colors.textSecondary} />
          </TouchableOpacity>

          <TouchableOpacity style={styles.settingItem}>
            <Text style={styles.settingLabel}>App Version</Text>
            <Text style={styles.settingValue}>1.0.0</Text>
          </TouchableOpacity>
        </>
      ))}

      <View style={styles.logoutSection}>
        <Button
          title="Log Out"
          onPress={handleLogout}
          variant="outline"
          leftIcon={<LogOut size={20} />}
          style={styles.logoutButton}
        />
      </View>

      <View style={styles.footer}>
        <Text style={styles.footerText}>
          Your data is encrypted and stored locally on your device.
        </Text>
        <Text style={styles.footerText}>
          We never sell your health data to third parties.
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
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 24,
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 16,
  },
  userInfo: {
    flex: 1,
  },
  userName: {
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 4,
  },
  userId: {
    fontSize: 14,
    opacity: 0.7,
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 12,
    marginLeft: 4,
  },
  sectionCard: {
    padding: 0,
    overflow: 'hidden',
  },
  settingItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.1)',
  },
  settingLeft: {
    flex: 1,
  },
  settingLabel: {
    fontSize: 16,
    marginBottom: 2,
  },
  settingDescription: {
    fontSize: 12,
    opacity: 0.6,
  },
  settingValue: {
    fontSize: 14,
    opacity: 0.7,
    marginRight: 8,
  },
  themeOptions: {
    flexDirection: 'row',
  },
  themeOption: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    marginLeft: 8,
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.1)',
  },
  themeOptionSelected: {
    borderColor: 'transparent',
  },
  themeOptionText: {
    fontSize: 12,
    fontWeight: '500',
  },
  dangerItem: {
    borderBottomColor: 'rgba(244,67,54,0.2)',
  },
  dangerText: {
    color: '#F44336',
  },
  logoutSection: {
    marginTop: 8,
    marginBottom: 32,
  },
  logoutButton: {
    borderColor: '#F44336',
  },
  footer: {
    alignItems: 'center',
    padding: 16,
    marginTop: 16,
  },
  footerText: {
    fontSize: 12,
    textAlign: 'center',
    opacity: 0.6,
    marginBottom: 4,
    lineHeight: 16,
  },
});