import { useEffect, useState } from 'react';
import * as LocalAuthentication from 'expo-local-authentication';
import { Alert } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { useDispatch } from 'react-redux';
import { setBiometricEnabled } from '../store/authSlice';

export const useBiometricLock = () => {
  const dispatch = useDispatch();
  const [isBiometricAvailable, setIsBiometricAvailable] = useState(false);
  const [isEnrolled, setIsEnrolled] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    checkBiometricAvailability();
  }, []);

  const checkBiometricAvailability = async () => {
    try {
      const hasHardware = await LocalAuthentication.hasHardwareAsync();
      const isEnrolled = await LocalAuthentication.isEnrolledAsync();
      
      setIsBiometricAvailable(hasHardware);
      setIsEnrolled(isEnrolled);
      
      // Check if biometric is enabled in settings
      const biometricEnabled = await SecureStore.getItemAsync('biometric_enabled');
      if (biometricEnabled === 'true') {
        dispatch(setBiometricEnabled(true));
      }
    } catch (error) {
      console.error('Error checking biometric availability:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const authenticate = async (options?: {
    promptMessage?: string;
    fallbackLabel?: string;
  }): Promise<boolean> => {
    if (!isBiometricAvailable || !isEnrolled) {
      return false;
    }

    try {
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: options?.promptMessage || 'Authenticate to continue',
        fallbackLabel: options?.fallbackLabel || 'Use passcode',
        disableDeviceFallback: false,
      });

      return result.success;
    } catch (error) {
      console.error('Biometric authentication error:', error);
      return false;
    }
  };

  const enableBiometric = async (): Promise<boolean> => {
    if (!isBiometricAvailable) {
      Alert.alert('Not Available', 'Biometric authentication is not available on this device.');
      return false;
    }

    if (!isEnrolled) {
      Alert.alert(
        'Not Enrolled',
        'Please set up biometric authentication in your device settings first.'
      );
      return false;
    }

    const authenticated = await authenticate({
      promptMessage: 'Enable biometric authentication for this app',
      fallbackLabel: 'Use passcode instead',
    });

    if (authenticated) {
      await SecureStore.setItemAsync('biometric_enabled', 'true');
      dispatch(setBiometricEnabled(true));
      return true;
    }

    return false;
  };

  const disableBiometric = async (): Promise<void> => {
    await SecureStore.setItemAsync('biometric_enabled', 'false');
    dispatch(setBiometricEnabled(false));
  };

  const toggleBiometric = async (): Promise<boolean> => {
    const biometricEnabled = await SecureStore.getItemAsync('biometric_enabled');
    if (biometricEnabled === 'true') {
      await disableBiometric();
      return false;
    } else {
      return await enableBiometric();
    }
  };

  const getBiometricType = async (): Promise<LocalAuthentication.AuthenticationType[]> => {
    try {
      const types = await LocalAuthentication.supportedAuthenticationTypesAsync();
      return types;
    } catch (error) {
      console.error('Error getting biometric types:', error);
      return [];
    }
  };

  const getBiometricTypeName = (type: LocalAuthentication.AuthenticationType): string => {
    switch (type) {
      case LocalAuthentication.AuthenticationType.FINGERPRINT:
        return 'Fingerprint';
      case LocalAuthentication.AuthenticationType.FACIAL_RECOGNITION:
        return 'Face ID';
      case LocalAuthentication.AuthenticationType.IRIS:
        return 'Iris Scanner';
      default:
        return 'Biometric';
    }
  };

  return {
    isBiometricAvailable,
    isEnrolled,
    isLoading,
    authenticate,
    enableBiometric,
    disableBiometric,
    toggleBiometric,
    getBiometricType,
    getBiometricTypeName,
  };
};