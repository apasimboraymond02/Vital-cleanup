import React, { useEffect } from 'react';
import { Provider } from 'react-redux';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { StatusBar } from 'expo-status-bar';
import { PaperProvider } from 'react-native-paper';
import { GestureHandlerRootView } from 'react-native-gesture-handler';

import { store } from './store';
import RootNavigator from './navigation/RootNavigator';
import { DatabaseProvider } from './database';
import { ThemeProvider, useTheme } from './theme/ThemeContext';
import { BiometricLockProvider } from './components/privacy/BiometricLockProvider';
import { PrivacyDisclaimer } from './components/privacy/PrivacyDisclaimer';

export default function App() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <Provider store={store}>
        <DatabaseProvider>
          <ThemeProvider>
            <BiometricLockProvider>
              <SafeAreaProvider>
                <PaperWrapper>
                  <NavigationContainer>
                    <RootNavigator />
                    <PrivacyDisclaimer />
                    <StatusBar style="auto" />
                  </NavigationContainer>
                </PaperWrapper>
              </SafeAreaProvider>
            </BiometricLockProvider>
          </ThemeProvider>
        </DatabaseProvider>
      </Provider>
    </GestureHandlerRootView>
  );
}

function PaperWrapper({ children }: { children: React.ReactNode }) {
  const theme = useTheme();
  return <PaperProvider theme={theme.paperTheme}>{children}</PaperProvider>;
}