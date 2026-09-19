import * as Crypto from 'expo-crypto';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

export class EncryptionService {
  private static instance: EncryptionService;
  private encryptionKey: string | null = null;

  private constructor() {}

  static getInstance(): EncryptionService {
    if (!EncryptionService.instance) {
      EncryptionService.instance = new EncryptionService();
    }
    return EncryptionService.instance;
  }

  async initialize(): Promise<void> {
    try {
      let key = await SecureStore.getItemAsync('encryption_key');
      
      if (!key) {
        key = await this.generateEncryptionKey();
        await SecureStore.setItemAsync('encryption_key', key);
      }

      this.encryptionKey = key;
    } catch (error) {
      console.error('Encryption initialization error:', error);
      throw error;
    }
  }

  private async generateEncryptionKey(): Promise<string> {
    // Generate a secure random key
    const randomBytes = await Crypto.getRandomBytesAsync(32);
    const key = Array.from(randomBytes)
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
    
    return key;
  }

  async encrypt(data: string): Promise<string> {
    if (!this.encryptionKey) {
      await this.initialize();
    }

    try {
      // For production, use stronger encryption like AES-GCM
      // This is a simplified example
      const encrypted = await Crypto.digestStringAsync(
        Crypto.CryptoDigestAlgorithm.SHA256,
        data + this.encryptionKey!
      );
      
      return encrypted;
    } catch (error) {
      console.error('Encryption error:', error);
      throw error;
    }
  }

  async decrypt(encryptedData: string): Promise<string> {
    // Note: SHA256 is not reversible
    // In production, use proper symmetric encryption
    console.warn('SHA256 encryption is not reversible. Use proper encryption for sensitive data.');
    return encryptedData;
  }

  async hashData(data: string): Promise<string> {
    try {
      const hash = await Crypto.digestStringAsync(
        Crypto.CryptoDigestAlgorithm.SHA256,
        data
      );
      return hash;
    } catch (error) {
      console.error('Hashing error:', error);
      throw error;
    }
  }

  async generateDataHash(data: any): Promise<string> {
    const dataString = JSON.stringify(data);
    return this.hashData(dataString);
  }

  async verifyDataIntegrity(data: any, expectedHash: string): Promise<boolean> {
    const currentHash = await this.generateDataHash(data);
    return currentHash === expectedHash;
  }

  async secureWipe(data: any): Promise<void> {
    // Overwrite data in memory (conceptually)
    // In JavaScript, we can't guarantee memory wiping
    // This is more relevant for native code
    if (data && typeof data === 'object') {
      Object.keys(data).forEach(key => {
        if (typeof data[key] === 'string') {
          data[key] = this.generateRandomString(data[key].length);
        }
        data[key] = null;
      });
    }
  }

  private generateRandomString(length: number): string {
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  }

  static async isDeviceSecure(): Promise<boolean> {
    try {
      if (Platform.OS === 'ios') {
        // iOS security check
        return true; // Simplified
      } else if (Platform.OS === 'android') {
        // Android security check
        return true; // Simplified
      }
      return true;
    } catch (error) {
      console.error('Device security check error:', error);
      return false;
    }
  }
}

export const encryption = EncryptionService.getInstance();