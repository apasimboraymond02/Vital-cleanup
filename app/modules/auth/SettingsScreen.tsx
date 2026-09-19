import React from 'react';
import { View, StyleSheet, Alert } from 'react-native';
import { List, Button, Divider, Text } from 'react-native-paper';
import { useDispatch } from 'react-redux';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { logout } from '../auth/authSlice';

export const SettingsScreen = () => {
  const dispatch = useDispatch();

  const handleLogout = () => {
    Alert.alert(
      'Log Out',
      'Are you sure you want to log out?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Log Out',
          style: 'destructive',
          onPress: async () => {
            await AsyncStorage.removeItem('user_session');
            dispatch(logout());
          },
        },
      ]
    );
  };

  return (
    <View style={styles.container}>
      <List.Section>
        <List.Subheader>Account</List.Subheader>
        <List.Item
          title="Profile"
          left={() => <List.Icon icon="account" />}
          onPress={() => {}}
        />
        <List.Item
          title="Notifications"
          left={() => <List.Icon icon="bell" />}
          onPress={() => {}}
        />
        <List.Item
          title="Privacy & Security"
          left={() => <List.Icon icon="shield-check" />}
          onPress={() => {}}
        />
      </List.Section>

      <Divider />

      <View style={styles.logoutContainer}>
        <Button
          mode="outlined"
          textColor="#D32F2F"
          style={styles.logoutButton}
          onPress={handleLogout}
        >
          Log Out
        </Button>
        <Text variant="bodySmall" style={styles.versionText}>
          Version 1.0.0
        </Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  logoutContainer: {
    padding: 16,
    marginTop: 'auto',
    marginBottom: 16,
  },
  logoutButton: {
    borderColor: '#D32F2F',
  },
  versionText: {
    textAlign: 'center',
    marginTop: 16,
    color: '#666',
  },
});