import { useState, useCallback } from 'react';
import * as SecureStore from 'expo-secure-store';
import * as Crypto from 'expo-crypto';

export const useEncryptedStorage = () => {
  const [isLoading, setIsLoading] = useState(false);

  const generateEncryptionKey = async (): Promise<string> => {
    const randomBytes = await Crypto.getRandomBytesAsync(32);
    return Array.from(randomBytes)
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
  };

  const getOrCreateEncryptionKey = async (keyName: string): Promise<string> => {
    let key = await SecureStore.getItemAsync(keyName);
    
    if (!key) {
      key = await generateEncryptionKey();
      await SecureStore.setItemAsync(keyName, key);
    }
    
    return key;
  };

  const encryptData = async (data: string, encryptionKey: string): Promise<string> => {
    // Simple XOR encryption for demonstration
    // In production, use a proper encryption library
    const encrypted = data.split('').map((char, i) => 
      char.charCodeAt(0) ^ encryptionKey.charCodeAt(i % encryptionKey.length)
    ).join(',');
    
    return Buffer.from(encrypted).toString('base64');
  };

  const decryptData = async (encryptedData: string, encryptionKey: string): Promise<string> => {
    try {
      const decoded = Buffer.from(encryptedData, 'base64').toString();
      const bytes = decoded.split(',').map(Number);
      
      return bytes.map((byte, i) => 
        String.fromCharCode(byte ^ encryptionKey.charCodeAt(i % encryptionKey.length))
      ).join('');
    } catch (error) {
      console.error('Decryption error:', error);
      throw new Error('Failed to decrypt data');
    }
  };

  const storeEncryptedItem = async (
    key: string,
    value: string,
    encryptionKeyName: string = 'default_encryption_key'
  ): Promise<void> => {
    setIsLoading(true);
    try {
      const encryptionKey = await getOrCreateEncryptionKey(encryptionKeyName);
      const encryptedValue = await encryptData(value, encryptionKey);
      
      await SecureStore.setItemAsync(key, encryptedValue);
    } catch (error) {
      console.error('Error storing encrypted item:', error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const getEncryptedItem = async (
    key: string,
    encryptionKeyName: string = 'default_encryption_key'
  ): Promise<string | null> => {
    setIsLoading(true);
    try {
      const encryptedValue = await SecureStore.getItemAsync(key);
      if (!encryptedValue) return null;

      const encryptionKey = await getOrCreateEncryptionKey(encryptionKeyName);
      const decryptedValue = await decryptData(encryptedValue, encryptionKey);
      
      return decryptedValue;
    } catch (error) {
      console.error('Error getting encrypted item:', error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const deleteEncryptedItem = async (key: string): Promise<void> => {
    try {
      await SecureStore.deleteItemAsync(key);
    } catch (error) {
      console.error('Error deleting encrypted item:', error);
      throw error;
    }
  };

  const storeObject = async <T>(
    key: string,
    value: T,
    encryptionKeyName?: string
  ): Promise<void> => {
    const jsonString = JSON.stringify(value);
    await storeEncryptedItem(key, jsonString, encryptionKeyName);
  };

  const getObject = async <T>(
    key: string,
    encryptionKeyName?: string
  ): Promise<T | null> => {
    const jsonString = await getEncryptedItem(key, encryptionKeyName);
    if (!jsonString) return null;
    
    try {
      return JSON.parse(jsonString) as T;
    } catch (error) {
      console.error('Error parsing stored object:', error);
      throw error;
    }
  };

  const hashData = async (data: string): Promise<string> => {
    try {
      return await Crypto.digestStringAsync(
        Crypto.CryptoDigestAlgorithm.SHA256,
        data
      );
    } catch (error) {
      console.error('Error hashing data:', error);
      throw error;
    }
  };

  const verifyDataIntegrity = async (
    storedHashKey: string,
    data: string
  ): Promise<boolean> => {
    try {
      const storedHash = await SecureStore.getItemAsync(storedHashKey);
      if (!storedHash) return false;

      const currentHash = await hashData(data);
      return storedHash === currentHash;
    } catch (error) {
      console.error('Error verifying data integrity:', error);
      return false;
    }
  };

  const storeWithIntegrityCheck = async (
    key: string,
    value: string,
    encryptionKeyName?: string
  ): Promise<void> => {
    // Store encrypted data
    await storeEncryptedItem(key, value, encryptionKeyName);
    
    // Store hash for integrity verification
    const hash = await hashData(value);
    await SecureStore.setItemAsync(`${key}_hash`, hash);
  };

  return {
    isLoading,
    storeEncryptedItem,
    getEncryptedItem,
    deleteEncryptedItem,
    storeObject,
    getObject,
    hashData,
    verifyDataIntegrity,
    storeWithIntegrityCheck,
  };
};