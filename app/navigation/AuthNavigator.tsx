import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Alert,
} from 'react-native';
import { useDispatch } from 'react-redux';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import {
  Lock,
  Shield,
  Eye,
  EyeOff,
  Mail,
  User,
  Calendar,
} from 'lucide-react-native';
import * as SecureStore from 'expo-secure-store';
import * as LocalAuthentication from 'expo-local-authentication';

import { Text } from '../components/common/Text';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { TextInput } from '../components/common/TextInput';
import { useTheme } from '../theme/ThemeContext';
import { setUser, setBiometricEnabled } from '../store/authSlice';
import { RootStackParamList } from './RootNavigator';

type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

export default function AuthNavigator() {
  const theme = useTheme();
  const dispatch = useDispatch();
  const [mode, setMode] = useState<'login' | 'register' | 'welcome'>('welcome');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    age: '',
  });

  const handleLogin = async () => {
    if (!formData.email || !formData.password) {
      Alert.alert('Error', 'Please enter email and password');
      return;
    }

    setLoading(true);
    try {
      // In production, this would validate with backend
      const userId = `user_${Date.now()}`;
      await SecureStore.setItemAsync('user_id', userId);
      
      dispatch(setUser({
        id: userId,
        preferences: {
          privacyLevel: 'balanced',
          syncEnabled: false,
          analyticsEnabled: true,
        },
      }));

      // Check biometric availability
      const hasHardware = await LocalAuthentication.hasHardwareAsync();
      if (hasHardware) {
        const enrollLevel = await LocalAuthentication.getEnrolledLevelAsync();
        if (enrollLevel > 0) {
          askBiometricSetup();
        }
      }
    } catch (error) {
      Alert.alert('Error', 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async () => {
    if (!formData.email || !formData.password || !formData.confirmPassword) {
      Alert.alert('Error', 'Please fill all fields');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      Alert.alert('Error', 'Passwords do not match');
      return;
    }

    if (formData.password.length < 8) {
      Alert.alert('Error', 'Password must be at least 8 characters');
      return;
    }

    setLoading(true);
    try {
      const userId = `user_${Date.now()}`;
      await SecureStore.setItemAsync('user_id', userId);
      
      // Store age if provided
      if (formData.age) {
        await SecureStore.setItemAsync('user_age', formData.age);
      }

      dispatch(setUser({
        id: userId,
        email: formData.email,
        ageGroup: formData.age ? (parseInt(formData.age) < 18 ? 'teen' : 'adult') : 'adult',
        preferences: {
          privacyLevel: 'max',
          syncEnabled: false,
          analyticsEnabled: true,
        },
      }));
    } catch (error) {
      Alert.alert('Error', 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const askBiometricSetup = () => {
    Alert.alert(
      'Enhanced Security',
      'Would you like to enable biometric authentication for faster and more secure access?',
      [
        {
          text: 'Not Now',
          style: 'cancel',
        },
        {
          text: 'Enable',
          onPress: async () => {
            await SecureStore.setItemAsync('biometric_enabled', 'true');
            dispatch(setBiometricEnabled(true));
          },
        },
      ]
    );
  };

  const renderWelcomeScreen = () => {
    return (
      <View style={styles.welcomeContainer}>
        <View style={styles.welcomeHeader}>
          <View style={[styles.iconContainer, { backgroundColor: theme.colors.primary + '20' }]}>
            <Shield size={48} color={theme.colors.primary} />
          </View>
          <Text style={styles.welcomeTitle}>Women's Health & Wellness</Text>
          <Text style={styles.welcomeSubtitle}>
            Privacy-first health tracking for every stage of life
          </Text>
        </View>

        <View style={styles.features}>
          <View style={styles.feature}>
            <Lock size={24} color={theme.colors.success} />
            <Text style={styles.featureText}>End-to-end encrypted</Text>
          </View>
          <View style={styles.feature}>
            <Shield size={24} color={theme.colors.success} />
            <Text style={styles.featureText}>Your data stays on your device</Text>
          </View>
          <View style={styles.feature}>
            <Calendar size={24} color={theme.colors.success} />
            <Text style={styles.featureText}>Comprehensive cycle tracking</Text>
          </View>
        </View>

        <View style={styles.welcomeButtons}>
          <Button
            title="Create Account"
            onPress={() => setMode('register')}
            style={styles.welcomeButton}
          />
          <Button
            title="Sign In"
            onPress={() => setMode('login')}
            variant="outline"
            style={styles.welcomeButton}
          />
          <TouchableOpacity style={styles.guestButton}>
            <Text style={styles.guestText}>Continue as Guest</Text>
          </TouchableOpacity>
        </View>

        <Text style={styles.privacyNote}>
          By continuing, you agree to our Privacy Policy and Terms of Service.
        </Text>
      </View>
    );
  };

  const renderLoginScreen = () => {
    return (
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.authContainer}
      >
        <ScrollView contentContainerStyle={styles.authContent}>
          <View style={styles.authHeader}>
            <TouchableOpacity onPress={() => setMode('welcome')} style={styles.backButton}>
              <Text style={styles.backText}>← Back</Text>
            </TouchableOpacity>
            <Text style={styles.authTitle}>Sign In</Text>
          </View>

          <Card style={styles.authCard}>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Email</Text>
              <TextInput
                placeholder="Enter your email"
                value={formData.email}
                onChangeText={(text) => setFormData({...formData, email: text})}
                leftIcon={<Mail size={20} color={theme.colors.textSecondary} />}
                keyboardType="email-address"
                autoCapitalize="none"
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Password</Text>
              <TextInput
                placeholder="Enter your password"
                value={formData.password}
                onChangeText={(text) => setFormData({...formData, password: text})}
                leftIcon={<Lock size={20} color={theme.colors.textSecondary} />}
                rightIcon={
                  <TouchableOpacity onPress={() => setShowPassword(!showPassword)}>
                    {showPassword ? (
                      <EyeOff size={20} color={theme.colors.textSecondary} />
                    ) : (
                      <Eye size={20} color={theme.colors.textSecondary} />
                    )}
                  </TouchableOpacity>
                }
                secureTextEntry={!showPassword}
              />
            </View>

            <TouchableOpacity style={styles.forgotPassword}>
              <Text style={styles.forgotText}>Forgot password?</Text>
            </TouchableOpacity>

            <Button
              title="Sign In"
              onPress={handleLogin}
              loading={loading}
              style={styles.authButton}
            />

            <TouchableOpacity onPress={() => setMode('register')} style={styles.switchMode}>
              <Text style={styles.switchText}>
                Don't have an account? <Text style={styles.switchHighlight}>Sign up</Text>
              </Text>
            </TouchableOpacity>
          </Card>
        </ScrollView>
      </KeyboardAvoidingView>
    );
  };

  const renderRegisterScreen = () => {
    return (
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.authContainer}
      >
        <ScrollView contentContainerStyle={styles.authContent}>
          <View style={styles.authHeader}>
            <TouchableOpacity onPress={() => setMode('welcome')} style={styles.backButton}>
              <Text style={styles.backText}>← Back</Text>
            </TouchableOpacity>
            <Text style={styles.authTitle}>Create Account</Text>
          </View>

          <Card style={styles.authCard}>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Email</Text>
              <TextInput
                placeholder="Enter your email"
                value={formData.email}
                onChangeText={(text) => setFormData({...formData, email: text})}
                leftIcon={<Mail size={20} color={theme.colors.textSecondary} />}
                keyboardType="email-address"
                autoCapitalize="none"
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Age (Optional)</Text>
              <TextInput
                placeholder="Your age"
                value={formData.age}
                onChangeText={(text) => setFormData({...formData, age: text})}
                leftIcon={<User size={20} color={theme.colors.textSecondary} />}
                keyboardType="numeric"
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Password</Text>
              <TextInput
                placeholder="Create a password"
                value={formData.password}
                onChangeText={(text) => setFormData({...formData, password: text})}
                leftIcon={<Lock size={20} color={theme.colors.textSecondary} />}
                rightIcon={
                  <TouchableOpacity onPress={() => setShowPassword(!showPassword)}>
                    {showPassword ? (
                      <EyeOff size={20} color={theme.colors.textSecondary} />
                    ) : (
                      <Eye size={20} color={theme.colors.textSecondary} />
                    )}
                  </TouchableOpacity>
                }
                secureTextEntry={!showPassword}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>Confirm Password</Text>
              <TextInput
                placeholder="Confirm your password"
                value={formData.confirmPassword}
                onChangeText={(text) => setFormData({...formData, confirmPassword: text})}
                leftIcon={<Lock size={20} color={theme.colors.textSecondary} />}
                secureTextEntry={!showPassword}
              />
            </View>

            <View style={styles.requirements}>
              <Text style={styles.requirementsTitle}>Password Requirements:</Text>
              <Text style={styles.requirement}>• At least 8 characters</Text>
              <Text style={styles.requirement}>• Use a mix of letters and numbers</Text>
              <Text style={styles.requirement}>• Avoid common passwords</Text>
            </View>

            <Button
              title="Create Account"
              onPress={handleRegister}
              loading={loading}
              style={styles.authButton}
            />

            <TouchableOpacity onPress={() => setMode('login')} style={styles.switchMode}>
              <Text style={styles.switchText}>
                Already have an account? <Text style={styles.switchHighlight}>Sign in</Text>
              </Text>
            </TouchableOpacity>
          </Card>

          <Text style={styles.privacyInfo}>
            Your data is encrypted and stored locally. We never sell your health information.
          </Text>
        </ScrollView>
      </KeyboardAvoidingView>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {mode === 'welcome' && renderWelcomeScreen()}
      {mode === 'login' && renderLoginScreen()}
      {mode === 'register' && renderRegisterScreen()}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  welcomeContainer: {
    flex: 1,
    padding: 32,
    justifyContent: 'center',
    alignItems: 'center',
  },
  welcomeHeader: {
    alignItems: 'center',
    marginBottom: 48,
  },
  iconContainer: {
    width: 96,
    height: 96,
    borderRadius: 48,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
  },
  welcomeTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 12,
  },
  welcomeSubtitle: {
    fontSize: 16,
    textAlign: 'center',
    opacity: 0.7,
    lineHeight: 24,
  },
  features: {
    width: '100%',
    marginBottom: 48,
  },
  feature: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  featureText: {
    fontSize: 16,
    marginLeft: 12,
  },
  welcomeButtons: {
    width: '100%',
    marginBottom: 32,
  },
  welcomeButton: {
    marginBottom: 12,
  },
  guestButton: {
    padding: 16,
  },
  guestText: {
    fontSize: 16,
    fontWeight: '500',
    textAlign: 'center',
    color: '#2196F3',
  },
  privacyNote: {
    fontSize: 12,
    textAlign: 'center',
    opacity: 0.6,
    lineHeight: 16,
  },
  authContainer: {
    flex: 1,
  },
  authContent: {
    padding: 24,
    flexGrow: 1,
    justifyContent: 'center',
  },
  authHeader: {
    alignItems: 'center',
    marginBottom: 32,
  },
  backButton: {
    alignSelf: 'flex-start',
    marginBottom: 16,
  },
  backText: {
    fontSize: 16,
    color: '#2196F3',
  },
  authTitle: {
    fontSize: 32,
    fontWeight: 'bold',
  },
  authCard: {
    padding: 24,
  },
  inputGroup: {
    marginBottom: 20,
  },
  inputLabel: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
  },
  forgotPassword: {
    alignSelf: 'flex-end',
    marginBottom: 24,
  },
  forgotText: {
    fontSize: 14,
    color: '#2196F3',
  },
  authButton: {
    marginBottom: 16,
  },
  switchMode: {
    padding: 12,
  },
  switchText: {
    fontSize: 14,
    textAlign: 'center',
    opacity: 0.7,
  },
  switchHighlight: {
    color: '#2196F3',
    fontWeight: '600',
  },
  requirements: {
    backgroundColor: 'rgba(0,0,0,0.03)',
    padding: 16,
    borderRadius: 8,
    marginBottom: 24,
  },
  requirementsTitle: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 8,
  },
  requirement: {
    fontSize: 12,
    opacity: 0.7,
    marginBottom: 4,
  },
  privacyInfo: {
    fontSize: 12,
    textAlign: 'center',
    opacity: 0.6,
    marginTop: 24,
    lineHeight: 16,
  },
});