import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { Text, TextInput, Button } from 'react-native-paper';
import { useDispatch } from 'react-redux';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { loginStart, loginSuccess, loginFailure } from '../../store/authSlice';

export const LoginScreen = ({ navigation }: any) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const dispatch = useDispatch();

  const handleLogin = async () => {
    if (!email || !password) {
      Alert.alert('Error', 'Please enter both email and password');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      Alert.alert('Error', 'Please enter a valid email address');
      return;
    }
    
    dispatch(loginStart());
    
    // Simulate API call
    setTimeout(async () => {
      // In a real app, you would validate credentials here
      const mockUser = { id: '1', email, name: 'User' };
      await AsyncStorage.setItem('user_session', JSON.stringify(mockUser));
      dispatch(loginSuccess(mockUser));
    }, 1500);
  };

  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <Text variant="displaySmall" style={styles.title}>Welcome Back</Text>
        <Text variant="bodyLarge" style={styles.subtitle}>Sign in to continue tracking your health</Text>

        <TextInput
          label="Email"
          value={email}
          onChangeText={setEmail}
          mode="outlined"
          style={styles.input}
          autoCapitalize="none"
          keyboardType="email-address"
          left={<TextInput.Icon icon="email" />}
        />

        <TextInput
          label="Password"
          value={password}
          onChangeText={setPassword}
          mode="outlined"
          style={styles.input}
          secureTextEntry
          left={<TextInput.Icon icon="lock" />}
        />

        <Button 
          mode="contained" 
          onPress={handleLogin} 
          loading={loading}
          style={styles.button}
          contentStyle={{ height: 48 }}
        >
          Login
        </Button>

        <View style={styles.footer}>
          <Text variant="bodyMedium">Don't have an account? </Text>
          <TouchableOpacity onPress={() => navigation.navigate('Signup')}>
            <Text variant="bodyMedium" style={styles.link}>Sign Up</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 20, justifyContent: 'center' },
  content: { width: '100%', maxWidth: 400, alignSelf: 'center' },
  title: { color: '#9C27B0', fontWeight: 'bold', marginBottom: 8 },
  subtitle: { color: '#666', marginBottom: 32 },
  input: { marginBottom: 16, backgroundColor: '#fff' },
  button: { marginTop: 8, marginBottom: 24, backgroundColor: '#9C27B0' },
  footer: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center' },
  link: { color: '#9C27B0', fontWeight: 'bold' },
});