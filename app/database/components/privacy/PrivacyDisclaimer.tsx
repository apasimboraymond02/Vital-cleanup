import React, { useState, useEffect } from 'react';
import {
  Modal,
  View,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
} from 'react-native';
import { useSelector } from 'react-redux';
import { Shield, AlertCircle, X, Lock } from 'lucide-react-native';

import { Text } from '../common/Text';
import { Button } from '../common/Button';
import { useTheme } from '../../theme/ThemeContext';
import { RootState } from '../../store';

export const PrivacyDisclaimer = () => {
  const theme = useTheme();
  const [visible, setVisible] = useState(false);
  const [accepted, setAccepted] = useState(false);
  const { privacy } = useSelector((state: RootState) => state.settings);

  useEffect(() => {
    // Check if user has accepted privacy policy
    const checkAcceptance = async () => {
      // In production, check AsyncStorage or SecureStore
      const hasAccepted = false; // Replace with actual check
      if (!hasAccepted) {
        setVisible(true);
      }
    };
    checkAcceptance();
  }, []);

  const handleAccept = () => {
    setAccepted(true);
    setVisible(false);
    // Store acceptance in AsyncStorage/SecureStore
  };

  const handleDecline = () => {
    // Handle app exit or limited functionality
    setVisible(false);
  };

  if (!visible) return null;

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      statusBarTranslucent
    >
      <View style={[styles.overlay, { backgroundColor: 'rgba(0,0,0,0.8)' }]}>
        <View style={[styles.modal, { backgroundColor: theme.colors.background }]}>
          <View style={styles.header}>
            <View style={styles.iconContainer}>
              <Shield size={32} color={theme.colors.primary} />
            </View>
            <TouchableOpacity onPress={handleDecline} style={styles.closeButton}>
              <X size={24} color={theme.colors.text} />
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.content}>
            <Text style={styles.title}>Your Privacy Matters</Text>
            
            <View style={[styles.feature, { backgroundColor: theme.colors.surface }]}>
              <Lock size={20} color={theme.colors.success} />
              <Text style={styles.featureText}>
                <Text style={styles.featureTitle}>Local-First Storage: </Text>
                Your health data stays on your device by default
              </Text>
            </View>

            <View style={[styles.feature, { backgroundColor: theme.colors.surface }]}>
              <Shield size={20} color={theme.colors.success} />
              <Text style={styles.featureText}>
                <Text style={styles.featureTitle}>End-to-End Encryption: </Text>
                Sensitive data is encrypted at rest
              </Text>
            </View>

            <View style={[styles.feature, { backgroundColor: theme.colors.surface }]}>
              <AlertCircle size={20} color={theme.colors.success} />
              <Text style={styles.featureText}>
                <Text style={styles.featureTitle}>No Data Selling: </Text>
                We never sell your health data to third parties
              </Text>
            </View>

            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Data Collection</Text>
              <Text style={styles.sectionText}>
                We collect only the data necessary to provide our services. 
                You can choose what information to share and delete your data at any time.
              </Text>
            </View>

            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Medical Disclaimer</Text>
              <Text style={styles.sectionText}>
                This app provides information for educational purposes only. 
                It is not a substitute for professional medical advice, diagnosis, or treatment.
                Always seek the advice of your physician with any questions you may have.
              </Text>
            </View>

            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Your Rights</Text>
              <Text style={styles.sectionText}>
                • Access your data at any time{'\n'}
                • Export your data in standard formats{'\n'}
                • Delete your data permanently{'\n'}
                • Opt-out of data sharing{'\n'}
                • Control notification preferences
              </Text>
            </View>

            <View style={styles.warning}>
              <AlertCircle size={20} color={theme.colors.warning} />
              <Text style={styles.warningText}>
                By continuing, you acknowledge that predictions are estimates based on 
                statistical analysis and may not be accurate for your individual circumstances.
              </Text>
            </View>
          </ScrollView>

          <View style={styles.footer}>
            <Button
              title="Read Full Policy"
              onPress={() => {/* Open full policy */}}
              variant="outline"
              style={styles.footerButton}
            />
            <Button
              title="Accept & Continue"
              onPress={handleAccept}
              style={styles.footerButton}
            />
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  modal: {
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    maxHeight: '90%',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,0,0,0.1)',
  },
  iconContainer: {
    padding: 8,
    borderRadius: 12,
  },
  closeButton: {
    padding: 4,
  },
  content: {
    padding: 20,
    maxHeight: 500,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 24,
  },
  feature: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
  },
  featureTitle: {
    fontWeight: '600',
  },
  featureText: {
    flex: 1,
    marginLeft: 12,
    lineHeight: 20,
  },
  section: {
    marginTop: 20,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },
  sectionText: {
    lineHeight: 20,
    opacity: 0.8,
  },
  warning: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginTop: 24,
    padding: 16,
    borderRadius: 12,
    backgroundColor: 'rgba(255,152,0,0.1)',
  },
  warningText: {
    flex: 1,
    marginLeft: 12,
    lineHeight: 18,
    fontSize: 14,
  },
  footer: {
    flexDirection: 'row',
    padding: 20,
    borderTopWidth: 1,
    borderTopColor: 'rgba(0,0,0,0.1)',
  },
  footerButton: {
    flex: 1,
    marginHorizontal: 8,
  },
});