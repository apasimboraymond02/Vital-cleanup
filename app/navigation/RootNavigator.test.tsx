import React from 'react';
import { View, Text } from 'react-native';
import { render, screen } from '@testing-library/react-native';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import { NavigationContainer } from '@react-navigation/native';
import RootNavigator from './RootNavigator';

// 1. Mock the child navigators to simplify the test
jest.mock('./AuthNavigator', () => () => <View><Text>AuthNavigator</Text></View>);
jest.mock('./MainTabs', () => () => <View><Text>MainTabs</Text></View>);

// 2. Mock the screens imported in RootNavigator to avoid dependency issues
jest.mock('../modules/cycle/CycleFormScreen', () => ({ CycleFormScreen: () => <View /> }));
jest.mock('../modules/cycle/SymptomTrackerScreen', () => ({ SymptomTrackerScreen: () => <View /> }));
jest.mock('../modules/fertility/FertilityScreen', () => ({ FertilityScreen: () => <View /> }));
jest.mock('../modules/pregnancy/PregnancyDashboard', () => ({ PregnancyDashboard: () => <View /> }));
jest.mock('../modules/settings/SettingsScreen', () => ({ SettingsScreen: () => <View /> }));

const mockStore = configureStore([]);

describe('RootNavigator', () => {
  it('shows loading indicator when isLoading is true', () => {
    const store = mockStore({
      auth: { isAuthenticated: false, isLoading: true },
    });

    render(
      <Provider store={store}>
        {/* NavigationContainer is not strictly needed here as RootNavigator returns a View, 
            but good to have for consistency if logic changes */}
        <RootNavigator />
      </Provider>
    );

    expect(screen.getByTestId('loading-container')).toBeTruthy();
  });

  it('shows AuthNavigator when not authenticated', () => {
    const store = mockStore({
      auth: { isAuthenticated: false, isLoading: false },
    });

    render(
      <Provider store={store}>
        <NavigationContainer>
          <RootNavigator />
        </NavigationContainer>
      </Provider>
    );

    // Check if the mocked AuthNavigator text is present
    expect(screen.getByText('AuthNavigator')).toBeTruthy();
    // Ensure MainTabs is NOT present
    expect(screen.queryByText('MainTabs')).toBeNull();
  });

  it('shows MainTabs when authenticated', () => {
    const store = mockStore({
      auth: { isAuthenticated: true, isLoading: false },
    });

    render(
      <Provider store={store}>
        <NavigationContainer>
          <RootNavigator />
        </NavigationContainer>
      </Provider>
    );

    // Check if the mocked MainTabs text is present
    expect(screen.getByText('MainTabs')).toBeTruthy();
    // Ensure AuthNavigator is NOT present
    expect(screen.queryByText('AuthNavigator')).toBeNull();
  });
});