import React from 'react';
import { View, Text } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Home, BarChart, Calendar, Settings } from 'lucide-react-native';

import { PregnancyDashboard } from '../modules/pregnancy/PregnancyDashboard';
import { FertilityScreen } from '../modules/fertility/FertilityScreen';
import { SettingsScreen } from '../modules/settings/SettingsScreen';

// Placeholder for a feature that is in the plan but not yet built
const CalendarView = () => (
  <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
    <Text>Calendar View Coming Soon</Text>
  </View>
);

const Tab = createBottomTabNavigator();

export default function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        // The RootNavigator will handle the main header title for these screens
        headerShown: false,
        tabBarIcon: ({ color, size }) => {
          if (route.name === 'Dashboard') {
            return <Home color={color} size={size} />;
          } else if (route.name === 'Fertility') {
            return <BarChart color={color} size={size} />;
          } else if (route.name === 'Calendar') {
            return <Calendar color={color} size={size} />;
          } else if (route.name === 'SettingsTab') {
            return <Settings color={color} size={size} />;
          }
          return null;
        },
        tabBarActiveTintColor: '#9C27B0',
        tabBarInactiveTintColor: 'gray',
      })}
    >
      <Tab.Screen name="Dashboard" component={PregnancyDashboard} />
      <Tab.Screen name="Fertility" component={FertilityScreen} />
      <Tab.Screen name="Calendar" component={CalendarView} />
      <Tab.Screen name="SettingsTab" component={SettingsScreen} options={{ title: 'Settings' }} />
    </Tab.Navigator>
  );
}