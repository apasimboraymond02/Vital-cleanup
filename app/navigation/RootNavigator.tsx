import React, { useEffect } from 'react';
import { View, ActivityIndicator } from 'react-native';
import { useSelector, useDispatch } from 'react-redux';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { RootState } from '../store';
import { loginSuccess, logout } from '../modules/auth/authSlice';

import AuthNavigator from './AuthNavigator';
import MainTabs from './MainTabs';
import { CycleFormScreen } from '../modules/cycle/CycleFormScreen';
import { SymptomTrackerScreen } from '../modules/cycle/SymptomTrackerScreen';
import { FertilityScreen } from '../modules/fertility/FertilityScreen';
import { PregnancyDashboard } from '../modules/pregnancy/PregnancyDashboard';
import { SettingsScreen } from '../modules/settings/SettingsScreen';

export type RootStackParamList = {
  Auth: undefined;
  Main: undefined;
  CycleForm: { cycleId?: string };
  SymptomTracker: { date: string };
  Fertility: undefined;
  PregnancyDashboard: undefined;
  Settings: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function RootNavigator() {
  const dispatch = useDispatch();
  const { isAuthenticated, isLoading } = useSelector((state: RootState) => state.auth);

  useEffect(() => {
    const bootstrapAsync = async () => {
      try {
        const userSession = await AsyncStorage.getItem('user_session');
        if (userSession) {
          dispatch(loginSuccess(JSON.parse(userSession)));
        } else {
          dispatch(logout());
        }
      } catch (e) {
        dispatch(logout());
      }
    };

    bootstrapAsync();
  }, [dispatch]);

  if (isLoading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }} testID="loading-container">
        <ActivityIndicator size="large" color="#9C27B0" />
      </View>
    );
  }

  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      {!isAuthenticated ? (
        <Stack.Screen name="Auth" component={AuthNavigator} />
      ) : (
        <>
          <Stack.Screen name="Main" component={MainTabs} />
          <Stack.Screen 
            name="CycleForm" 
            component={CycleFormScreen}
            options={{ headerShown: true, title: 'Log Period' }}
          />
          <Stack.Screen 
            name="SymptomTracker" 
            component={SymptomTrackerScreen}
            options={{ headerShown: true, title: 'Symptoms' }}
          />
          <Stack.Screen 
            name="Fertility" 
            component={FertilityScreen}
            options={{ headerShown: true, title: 'Fertility Tracker' }}
          />
          <Stack.Screen 
            name="PregnancyDashboard" 
            component={PregnancyDashboard}
            options={{ headerShown: true, title: 'Pregnancy' }}
          />
          <Stack.Screen 
            name="Settings" 
            component={SettingsScreen}
            options={{ headerShown: true, title: 'Settings' }}
          />
        </>
      )}
    </Stack.Navigator>
  );
}