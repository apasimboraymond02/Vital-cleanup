import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import * as SecureStore from 'expo-secure-store';
import * as LocalAuthentication from 'expo-local-authentication';

interface AuthState {
  isAuthenticated: boolean;
  user: {
    id: string;
    email?: string;
    ageGroup?: 'teen' | 'adult' | 'pregnancy' | 'menopause';
    preferences: {
      privacyLevel: 'max' | 'balanced' | 'minimal';
      syncEnabled: boolean;
      analyticsEnabled: boolean;
    };
  } | null;
  biometricEnabled: boolean;
  pinEnabled: boolean;
  isLoading: boolean;
}

const initialState: AuthState = {
  isAuthenticated: false,
  user: null,
  biometricEnabled: false,
  pinEnabled: false,
  isLoading: true,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setUser: (state, action: PayloadAction<AuthState['user']>) => {
      state.user = action.payload;
      state.isAuthenticated = !!action.payload;
    },
    setBiometricEnabled: (state, action: PayloadAction<boolean>) => {
      state.biometricEnabled = action.payload;
    },
    setPinEnabled: (state, action: PayloadAction<boolean>) => {
      state.pinEnabled = action.payload;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
    logout: (state) => {
      state.isAuthenticated = false;
      state.user = null;
    },
    updatePreferences: (state, action: PayloadAction<Partial<AuthState['user']['preferences']>>) => {
      if (state.user) {
        state.user.preferences = { ...state.user.preferences, ...action.payload };
      }
    },
  },
});

export const {
  setUser,
  setBiometricEnabled,
  setPinEnabled,
  setLoading,
  logout,
  updatePreferences,
} = authSlice.actions;

export const initializeAuth = () => async (dispatch: any) => {
  try {
    // Check if user exists in secure storage
    const userId = await SecureStore.getItemAsync('user_id');
    const biometricEnabled = await SecureStore.getItemAsync('biometric_enabled');
    const pinEnabled = await SecureStore.getItemAsync('pin_enabled');

    if (userId) {
      // User exists, check biometric if enabled
      if (biometricEnabled === 'true') {
        const authResult = await LocalAuthentication.authenticateAsync({
          promptMessage: 'Authenticate to access your health data',
          fallbackLabel: 'Enter PIN',
        });

        if (authResult.success) {
          dispatch(setUser({
            id: userId,
            preferences: {
              privacyLevel: 'balanced',
              syncEnabled: false,
              analyticsEnabled: true,
            },
          }));
          dispatch(setBiometricEnabled(true));
        }
      } else {
        dispatch(setUser({
          id: userId,
          preferences: {
            privacyLevel: 'balanced',
            syncEnabled: false,
            analyticsEnabled: true,
          },
        }));
      }
      
      dispatch(setPinEnabled(pinEnabled === 'true'));
    }
  } catch (error) {
    console.error('Auth initialization error:', error);
  } finally {
    dispatch(setLoading(false));
  }
};

export default authSlice.reducer;