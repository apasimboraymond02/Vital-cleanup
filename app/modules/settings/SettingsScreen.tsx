import React from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  Switch,
  Linking,
  Alert,
} from 'react-native';
import { useSelector, useDispatch } from 'react-redux';
import {
  Settings as SettingsIcon,
  Bell,
  Moon,
  Globe,
  Lock,
  Shield,
  User,
  HelpCircle,
  Star,
  Share2,
  Mail,
  ChevronRight,
} from 'lucide-react-native';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useTheme } from '../../theme/ThemeContext';
import {
  setTheme,
  setLanguage,
  updateNotifications,
  updateHealthProfile,
} from '../../store/settingsSlice';

export const SettingsScreen = () => {
  const theme = useTheme();
  const dispatch = useDispatch();
  const settings = useSelector((state: any) => state.settings);

  const handleNotificationToggle = (key: string, value: boolean) => {
    dispatch(updateNotifications({ [key]: value }));
  };

  const handleThemeChange = (themeMode: 'light' | 'dark' | 'system') => {
    dispatch(setTheme(themeMode));
  };

  const handleLanguageChange = (language: string) => {
    dispatch(setLanguage(language));
  };

  const openPrivacyPolicy = () => {
    Linking.openURL('https://example.com/privacy-policy');
  };

  const openTermsOfService = () => {
    Linking.openURL('https://example.com/terms');
  };

  const openSupport = () => {
    Linking.openURL('mailto:support@example.com');
  };

  const rateApp = () => {
    // Platform-specific app store links
    const storeUrl = Platform.OS === 'ios' 
      ? 'https://apps.apple.com/app/idYOUR_APP_ID'
      : 'https://play.google.com/store/apps/details?id=com.yourapp';

    Linking.openURL(storeUrl).catch(() => {
      Alert.alert('Error', 'Could not open app store');
    });
  };

  const shareApp = () => {
    // Implement sharing logic
    Alert.alert('Share', 'Share this app with friends and family!');
  };

  const renderSettingItem = (
    icon: React.ReactNode,
    title: string,
    rightElement: React.ReactNode,
    onPress?: () => void
  ) => {
    return (
      <TouchableOpacity
        style={styles.settingItem}
        onPress={onPress}
        disabled={!onPress}
      >
        <View style={styles.settingLeft}>
          <View style={styles.settingIcon}>
            {icon}
          </View>
          <Text style={styles.settingLabel}>{title}</Text>
        </View>
        {rightElement}
      </TouchableOpacity>
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
        <SettingsIcon size={32} color={theme.colors.primary} />
        <Text style={styles.title}>Settings</Text>
      </View>

      {renderSection('Appearance', (
        <>
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

      {renderSection('Notifications', (
        <>
          <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Bell size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Cycle Reminders</Text>
            </View>
            <Switch
              value={settings.notifications.cycleReminders}
              onValueChange={(value) => handleNotificationToggle('cycleReminders', value)}
              trackColor={{ false: '#767577', true: theme.colors.primary }}
            />
          </View>

          <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Bell size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Symptom Reminders</Text>
            </View>
            <Switch
              value={settings.notifications.symptomReminders}
              onValueChange={(value) => handleNotificationToggle('symptomReminders', value)}
              trackColor={{ false: '#767577', true: theme.colors.primary }}
            />
          </View>

          <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Bell size={20} color={theme.colors.primary} />
              <Text style={styles.settingLabel}>Appointment Reminders</Text>
            </View>
            <Switch
              value={settings.notifications.appointmentReminders}
              onValueChange={(value) => handleNotificationToggle('appointmentReminders', value)}
              trackColor={{ false: '#767577', true: theme.colors.primary }}
            />
          </View>
        </>
      ))}

      {renderSection('Privacy & Security', (
        <>
          {renderSettingItem(
            <Lock size={20} color={theme.colors.primary} />,
            'Privacy Settings',
            <ChevronRight size={20} color={theme.colors.textSecondary} />,
            () => navigation.navigate('PrivacySettings')
          )}

          {renderSettingItem(
            <Shield size={20} color={theme.colors.primary} />,
            'Privacy Policy',
            <ChevronRight size={20} color={theme.colors.textSecondary} />,
            openPrivacyPolicy
          )}

          {renderSettingItem(
            <Shield size={20} color={theme.colors.primary} />,
            'Terms of Service',
            <ChevronRight size={20} color={theme.colors.textSecondary} />,
            openTermsOfService
          )}
        </>
      ))}

      {renderSection('Support', (
        <>
          {renderSettingItem(
            <HelpCircle size={20} color={theme.colors.primary} />,
            'Help & Support',
            <ChevronRight size={20} color={theme.colors.textSecondary} />,
            openSupport
          )}

          {renderSettingItem(
            <Mail size={20} color={theme.colors.primary} />,
            'Contact Us',
            <ChevronRight size={20} color={theme.colors.textSecondary} />,
            openSupport
          )}

          <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
              <Text style={styles.settingLabel}>App Version</Text>
            </View>
            <Text style={styles.settingValue}>1.0.0</Text>
          </View>
        </>
      ))}

      {renderSection('About', (
        <>
          {renderSettingItem(
            <Star size={20} color={theme.colors.primary} />,
            'Rate the App',
            <ChevronRight size={20} color={theme.colors.textSecondary} />,
            rateApp
          )}

          {renderSettingItem(
            <Share2 size={20} color={theme.colors.primary} />,
            'Share with Friends',
            <ChevronRight size={20} color={theme.colors.textSecondary} />,
            shareApp
          )}

          <View style={styles.aboutText}>
            <Text style={styles.aboutTitle}>Women's Health & Wellness</Text>
            <Text style={styles.aboutDescription}>
              A privacy-first health tracking app designed to empower women 
              with accurate, personalized insights while keeping your data secure.
            </Text>
            <Text style={styles.copyright}>
              © 2024 Women's Health Inc. All rights reserved.
            </Text>
          </View>
        </>
      ))}
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
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  settingIcon: {
    marginRight: 12,
  },
  settingLabel: {
    fontSize: 16,
  },
  settingValue: {
    fontSize: 14,
    opacity: 0.7,
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
  aboutText: {
    padding: 16,
  },
  aboutTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },
  aboutDescription: {
    fontSize: 14,
    lineHeight: 20,
    opacity: 0.8,
    marginBottom: 12,
  },
  copyright: {
    fontSize: 12,
    opacity: 0.6,
    fontStyle: 'italic',
  },
});