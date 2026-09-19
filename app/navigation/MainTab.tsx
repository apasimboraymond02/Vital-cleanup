import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useTheme } from '../theme/ThemeContext';
import { Calendar, Home, Heart, BookOpen, User } from 'lucide-react-native';

import { DashboardScreen } from '../modules/dashboard/DashboardScreen';
import { CycleScreen } from '../modules/cycle/CycleScreen';
import { FertilityScreen } from '../modules/fertility/FertilityScreen';
import { ContentScreen } from '../modules/content/ContentScreen';
import { ProfileScreen } from '../modules/profile/ProfileScreen';

const Tab = createBottomTabNavigator();

export default function MainTabs() {
  const theme = useTheme();

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ color, size }) => {
          const icons: Record<string, React.ReactNode> = {
            Dashboard: <Home size={size} color={color} />,
            Cycle: <Calendar size={size} color={color} />,
            Fertility: <Heart size={size} color={color} />,
            Content: <BookOpen size={size} color={color} />,
            Profile: <User size={size} color={color} />,
          };
          return icons[route.name];
        },
        tabBarActiveTintColor: theme.colors.primary,
        tabBarInactiveTintColor: theme.colors.gray,
        tabBarStyle: {
          backgroundColor: theme.colors.background,
          borderTopColor: theme.colors.border,
        },
        headerShown: false,
      })}
    >
      <Tab.Screen name="Dashboard" component={DashboardScreen} />
      <Tab.Screen name="Cycle" component={CycleScreen} />
      <Tab.Screen name="Fertility" component={FertilityScreen} />
      <Tab.Screen name="Content" component={ContentScreen} />
      <Tab.Screen name="Profile" component={ProfileScreen} />
    </Tab.Navigator>
  );
}