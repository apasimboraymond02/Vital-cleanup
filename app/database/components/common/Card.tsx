import React from 'react';
import {
  View,
  StyleSheet,
  ViewStyle,
  TouchableOpacity,
  TouchableOpacityProps,
} from 'react-native';
import { useTheme } from '../../theme/ThemeContext';

interface CardProps extends TouchableOpacityProps {
  children: React.ReactNode;
  style?: ViewStyle;
  elevation?: number;
  onPress?: () => void;
}

export const Card: React.FC<CardProps> = ({
  children,
  style,
  elevation = 2,
  onPress,
  ...props
}) => {
  const theme = useTheme();
  const Component = onPress ? TouchableOpacity : View;

  const cardStyle = {
    backgroundColor: theme.colors.surface,
    borderRadius: 12,
    ...(elevation > 0 && {
      shadowColor: '#000',
      shadowOffset: {
        width: 0,
        height: elevation,
      },
      shadowOpacity: 0.1,
      shadowRadius: elevation * 2,
      elevation: elevation,
    }),
  };

  return (
    <Component
      style={[styles.card, cardStyle, style]}
      onPress={onPress}
      activeOpacity={0.8}
      {...props}
    >
      {children}
    </Component>
  );
};

const styles = StyleSheet.create({
  card: {
    padding: 16,
  },
});