import React, { createContext, useContext, useEffect, useState } from 'react';
import * as SQLite from 'expo-sqlite';
import * as Crypto from 'expo-crypto';
import * as SecureStore from 'expo-secure-store';

interface DatabaseContextType {
  db: SQLite.SQLiteDatabase | null;
  initializeDatabase: () => Promise<void>;
  encryptData: (data: string) => Promise<string>;
  decryptData: (encryptedData: string) => Promise<string>;
}

const DatabaseContext = createContext<DatabaseContextType | undefined>(undefined);

export const DatabaseProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [db, setDb] = useState<SQLite.SQLiteDatabase | null>(null);
  const [encryptionKey, setEncryptionKey] = useState<string | null>(null);

  useEffect(() => {
    initializeDatabase();
  }, []);

  const generateEncryptionKey = async (): Promise<string> => {
    const key = await Crypto.digestStringAsync(
      Crypto.CryptoDigestAlgorithm.SHA256,
      `${Date.now()}${Math.random()}`
    );
    await SecureStore.setItemAsync('db_encryption_key', key);
    return key;
  };

  const getEncryptionKey = async (): Promise<string> => {
    try {
      let key = await SecureStore.getItemAsync('db_encryption_key');
      if (!key) {
        key = await generateEncryptionKey();
      }
      return key;
    } catch (error) {
      console.error('Error getting encryption key:', error);
      throw error;
    }
  };

  const encryptData = async (data: string): Promise<string> => {
    try {
      const key = await getEncryptionKey();
      // Simple XOR encryption for demonstration (use stronger encryption in production)
      return btoa(data.split('').map((char, i) => 
        char.charCodeAt(0) ^ key.charCodeAt(i % key.length)
      ).join(','));
    } catch (error) {
      console.error('Encryption error:', error);
      return data;
    }
  };

  const decryptData = async (encryptedData: string): Promise<string> => {
    try {
      const key = await getEncryptionKey();
      const bytes = atob(encryptedData).split(',').map(Number);
      return bytes.map((byte, i) => 
        String.fromCharCode(byte ^ key.charCodeAt(i % key.length))
      ).join('');
    } catch (error) {
      console.error('Decryption error:', error);
      return encryptedData;
    }
  };

  const initializeDatabase = async () => {
    try {
      const database = SQLite.openDatabase('womens_health.db');
      
      // Initialize tables
      await new Promise<void>((resolve, reject) => {
        database.transaction(tx => {
          // Cycle tracking table
          tx.executeSql(
            `CREATE TABLE IF NOT EXISTS cycles (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              start_date TEXT NOT NULL,
              end_date TEXT,
              flow_intensity INTEGER CHECK(flow_intensity BETWEEN 1 AND 5),
              mood TEXT,
              notes TEXT,
              symptoms TEXT,
              created_at TEXT DEFAULT CURRENT_TIMESTAMP,
              updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
              is_deleted INTEGER DEFAULT 0
            )`
          );

          // Symptoms table
          tx.executeSql(
            `CREATE TABLE IF NOT EXISTS symptoms (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT NOT NULL,
              category TEXT,
              severity INTEGER CHECK(severity BETWEEN 1 AND 5),
              cycle_id INTEGER,
              logged_date TEXT NOT NULL,
              notes TEXT,
              created_at TEXT DEFAULT CURRENT_TIMESTAMP,
              FOREIGN KEY (cycle_id) REFERENCES cycles(id) ON DELETE CASCADE
            )`
          );

          // Pregnancy tracking table
          tx.executeSql(
            `CREATE TABLE IF NOT EXISTS pregnancies (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              start_date TEXT NOT NULL,
              due_date TEXT NOT NULL,
              last_period_date TEXT,
              confirmed_by_doctor INTEGER DEFAULT 0,
              doctor_visit_dates TEXT,
              current_week INTEGER,
              notes TEXT,
              created_at TEXT DEFAULT CURRENT_TIMESTAMP,
              updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )`
          );

          // User settings table
          tx.executeSql(
            `CREATE TABLE IF NOT EXISTS settings (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              key TEXT UNIQUE NOT NULL,
              value TEXT,
              encrypted INTEGER DEFAULT 0,
              created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )`
          );

          // Health metrics table
          tx.executeSql(
            `CREATE TABLE IF NOT EXISTS health_metrics (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              type TEXT NOT NULL,
              value REAL NOT NULL,
              unit TEXT,
              logged_date TEXT NOT NULL,
              notes TEXT,
              created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )`
          );

          // Content bookmarks table
          tx.executeSql(
            `CREATE TABLE IF NOT EXISTS bookmarks (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              content_id TEXT NOT NULL,
              content_type TEXT NOT NULL,
              title TEXT,
              url TEXT,
              saved_at TEXT DEFAULT CURRENT_TIMESTAMP
            )`
          );
        }, reject, () => {
          setDb(database);
          resolve();
        });
      });
    } catch (error) {
      console.error('Database initialization error:', error);
    }
  };

  const contextValue: DatabaseContextType = {
    db,
    initializeDatabase,
    encryptData,
    decryptData,
  };

  return (
    <DatabaseContext.Provider value={contextValue}>
      {children}
    </DatabaseContext.Provider>
  );
};

export const useDatabase = () => {
  const context = useContext(DatabaseContext);
  if (!context) {
    throw new Error('useDatabase must be used within a DatabaseProvider');
  }
  return context;
};