import React, { useState } from 'react';
import { StyleSheet, ScrollView, Alert, View } from 'react-native';
import { Text, Surface, TouchableRipple, useTheme, Divider } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { FileText, ChevronRight, Shield, Database } from 'lucide-react-native';
import { HealthReportService } from '../services/HealthReportService';
import { DatabaseService } from '../services/DatabaseService';

export const SettingsScreen = () => {
  const theme = useTheme();
  const [exporting, setExporting] = useState(false);

  const handleExportReport = async () => {
    setExporting(true);
    try {
      // TODO: Replace this mock data with real data from your SQLite database or Store
      const mockData = {
        userName: "Jane Doe",
        dateRange: { 
          start: new Date(new Date().setMonth(new Date().getMonth() - 3)), 
          end: new Date() 
        },
        cycles: [
          { startDate: '2023-10-01', endDate: '2023-10-29', length: 28 },
          { startDate: '2023-11-01', endDate: '2023-11-28', length: 27 },
          { startDate: '2023-12-01', endDate: null, length: 0 },
        ],
        symptoms: [
          { date: '2023-11-14', type: 'Cramps', severity: 'Medium' as const },
          { date: '2023-11-15', type: 'Headache', severity: 'Low' as const },
          { date: '2023-12-02', type: 'Fatigue', severity: 'High' as const },
        ]
      // Define range (e.g., last 3 months)
      const endDate = new Date();
      const startDate = new Date();
      startDate.setMonth(startDate.getMonth() - 3);

      // Fetch real data
      const { cycles, symptoms } = await DatabaseService.getReportData(startDate, endDate);

      const reportData = {
        userName: "Jane Doe", // You can fetch this from User Preferences if available
        dateRange: { start: startDate, end: endDate },
        cycles,
        symptoms
      };

      await HealthReportService.exportReport(mockData);
      await HealthReportService.exportReport(reportData);
    } catch (error) {
      console.error(error);
      Alert.alert("Export Failed", "Could not generate the PDF report.");
    } finally {
      setExporting(false);
    }
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text variant="headlineMedium" style={styles.header}>Settings</Text>

        <View style={styles.section}>
          <Text variant="titleSmall" style={styles.sectionTitle}>Health Data</Text>
          <Surface style={styles.surface} elevation={1}>
            <TouchableRipple onPress={handleExportReport} disabled={exporting}>
              <View style={styles.row}>
                <View style={styles.iconContainer}>
                  <FileText size={24} color={theme.colors.primary} />
                </View>
                <View style={styles.rowContent}>
                  <Text variant="bodyLarge">Export Clinician Report</Text>
                  <Text variant="bodySmall" style={styles.subtitle}>
                    {exporting ? 'Generating PDF...' : 'Download PDF for your doctor'}
                  </Text>
                </View>
                <ChevronRight size={20} color="#ccc" />
              </View>
            </TouchableRipple>
            
            <Divider />

            <TouchableRipple onPress={() => {}}>
              <View style={styles.row}>
                <View style={styles.iconContainer}>
                  <Database size={24} color={theme.colors.primary} />
                </View>
                <View style={styles.rowContent}>
                  <Text variant="bodyLarge">Backup & Restore</Text>
                  <Text variant="bodySmall" style={styles.subtitle}>Manage local backups</Text>
                </View>
                <ChevronRight size={20} color="#ccc" />
              </View>
            </TouchableRipple>
          </Surface>
        </View>

        <View style={styles.section}>
          <Text variant="titleSmall" style={styles.sectionTitle}>Privacy</Text>
          <Surface style={styles.surface} elevation={1}>
            <TouchableRipple onPress={() => {}}>
              <View style={styles.row}>
                <View style={styles.iconContainer}>
                  <Shield size={24} color={theme.colors.primary} />
                </View>
                <View style={styles.rowContent}>
                  <Text variant="bodyLarge">Security</Text>
                  <Text variant="bodySmall" style={styles.subtitle}>App lock and permissions</Text>
                </View>
                <ChevronRight size={20} color="#ccc" />
              </View>
            </TouchableRipple>
          </Surface>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  content: { padding: 16 },
  header: { marginBottom: 24, fontWeight: 'bold' },
  section: { marginBottom: 24 },
  sectionTitle: { marginBottom: 8, marginLeft: 4, color: '#666', textTransform: 'uppercase', fontSize: 12 },
  surface: { backgroundColor: 'white', borderRadius: 12, overflow: 'hidden' },
  row: { flexDirection: 'row', alignItems: 'center', padding: 16 },
  iconContainer: { width: 40, alignItems: 'center', justifyContent: 'center', marginRight: 12 },
  rowContent: { flex: 1 },
  subtitle: { color: '#666', marginTop: 2 }
});