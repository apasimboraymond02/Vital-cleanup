import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Appearance } from 'react-native';

interface SettingsState {
  theme: 'light' | 'dark' | 'system';
  language: string;
  notifications: {
    cycleReminders: boolean;
    symptomReminders: boolean;
    medicationReminders: boolean;
    appointmentReminders: boolean;
  };
  privacy: {
    localOnly: boolean;
    shareAnonymizedData: boolean;
    autoDeletePeriod: number; // days
    exportFormat: 'json' | 'csv';
  };
  healthProfile: {
    age: number;
    hasPCOS: boolean;
    hasEndometriosis: boolean;
    hasMigraines: boolean;
    contraceptionMethod?: string;
    lastPapSmear?: string;
    bloodType?: string;
  };
  cycleSettings: {
    averageLength: number;
    typicalPeriodLength: number;
    ovulationDay: number;
    trackSymptoms: boolean;
    trackMood: boolean;
    trackFlow: boolean;
  };
}

const initialState: SettingsState = {
  theme: 'system',
  language: 'en',
  notifications: {
    cycleReminders: true,
    symptomReminders: true,
    medicationReminders: false,
    appointmentReminders: true,
  },
  privacy: {
    localOnly: true,
    shareAnonymizedData: false,
    autoDeletePeriod: 90,
    exportFormat: 'json',
  },
  healthProfile: {
    age: 25,
    hasPCOS: false,
    hasEndometriosis: false,
    hasMigraines: false,
  },
  cycleSettings: {
    averageLength: 28,
    typicalPeriodLength: 5,
    ovulationDay: 14,
    trackSymptoms: true,
    trackMood: true,
    trackFlow: true,
  },
};

const settingsSlice = createSlice({
  name: 'settings',
  initialState,
  reducers: {
    setTheme: (state, action: PayloadAction<'light' | 'dark' | 'system'>) => {
      state.theme = action.payload;
    },
    setLanguage: (state, action: PayloadAction<string>) => {
      state.language = action.payload;
    },
    updateNotifications: (state, action: PayloadAction<Partial<SettingsState['notifications']>>) => {
      state.notifications = { ...state.notifications, ...action.payload };
    },
    updatePrivacy: (state, action: PayloadAction<Partial<SettingsState['privacy']>>) => {
      state.privacy = { ...state.privacy, ...action.payload };
    },
    updateHealthProfile: (state, action: PayloadAction<Partial<SettingsState['healthProfile']>>) => {
      state.healthProfile = { ...state.healthProfile, ...action.payload };
    },
    updateCycleSettings: (state, action: PayloadAction<Partial<SettingsState['cycleSettings']>>) => {
      state.cycleSettings = { ...state.cycleSettings, ...action.payload };
    },
    resetSettings: () => initialState,
  },
});

export const {
  setTheme,
  setLanguage,
  updateNotifications,
  updatePrivacy,
  updateHealthProfile,
  updateCycleSettings,
  resetSettings,
} = settingsSlice.actions;

export default settingsSlice.reducer;