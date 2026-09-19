import React, { createContext, useContext, useEffect, useState } from 'react';
import { AppState, AppStateStatus } from 'react-native';
import * as LocalAuthentication from 'expo-local-authentication';
import { useSelector } from 'react-redux';
import { RootState } from '../../store';

interface BiometricLockContextType {
  isLocked: boolean;
  authenticate: () => Promise<boolean>;
}

const BiometricLockContext = createContext<BiometricLockContextType | undefined>(undefined);

export const BiometricLockProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { biometricEnabled } = useSelector((state: RootState) => state.auth);
  const [isLocked, setIsLocked] = useState(false);
  const [appState, setAppState] = useState(AppState.currentState);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', handleAppStateChange);
    return () => subscription.remove();
  }, []);

  useEffect(() => {
    if (biometricEnabled && appState === 'background') {
      setIsLocked(true);
    }
  }, [appState, biometricEnabled]);

  const handleAppStateChange = (nextAppState: AppStateStatus) => {
    if (appState === 'active' && nextAppState.match(/inactive|background/)) {
      // App going to background
      if (biometricEnabled) {
        setIsLocked(true);
      }
    }
    setAppState(nextAppState);
  };

  const authenticate = async (): Promise<boolean> => {
    if (!biometricEnabled) {
      setIsLocked(false);
      return true;
    }

    try {
      const hasHardware = await LocalAuthentication.hasHardwareAsync();
      if (!hasHardware) {
        setIsLocked(false);
        return true;
      }

      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: 'Authenticate to access your health data',
        fallbackLabel: 'Enter PIN',
      });

      if (result.success) {
        setIsLocked(false);
        return true;
      }

      return false;
    } catch (error) {
      console.error('Biometric authentication error:', error);
      setIsLocked(false);
      return true; // Allow access if biometric fails
    }
  };

  const value = {
    isLocked,
    authenticate,
  };

  return (
    <BiometricLockContext.Provider value={value}>
      {children}
    </BiometricLockContext.Provider>
  );
};

export const useBiometricLock = () => {
  const context = useContext(BiometricLockContext);
  if (!context) {
    throw new Error('useBiometricLock must be used within a BiometricLockProvider');
  }
  return context;
};