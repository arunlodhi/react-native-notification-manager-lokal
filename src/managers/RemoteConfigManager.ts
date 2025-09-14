import AsyncStorage from "@react-native-async-storage/async-storage";
import {
  RemoteConfigProvider,
  RemoteConfigValues,
  RemoteConfigConstants,
  DEFAULT_REMOTE_CONFIG,
} from "../types/RemoteConfigTypes";

/**
 * RemoteConfigManager - Manages remote configuration for notifications
 * Provides a centralized way to access remote config values with fallbacks
 */
export class RemoteConfigManager {
  private static instance: RemoteConfigManager;
  private provider?: RemoteConfigProvider;
  private cachedValues: Partial<RemoteConfigValues> = {};
  private isInitialized = false;

  private constructor() {}

  public static getInstance(): RemoteConfigManager {
    if (!RemoteConfigManager.instance) {
      RemoteConfigManager.instance = new RemoteConfigManager();
    }
    return RemoteConfigManager.instance;
  }

  /**
   * Initialize with a remote config provider
   * @param provider - The remote config provider (Firebase, custom, etc.)
   */
  public async initialize(provider?: RemoteConfigProvider): Promise<void> {
    this.provider = provider;

    if (this.provider) {
      await this.provider.initialize();
      await this.fetchAndCache();
    } else {
      // Load cached values from storage if no provider
      await this.loadCachedValues();
    }

    this.isInitialized = true;
  }

  /**
   * Set remote config values manually (useful for testing or when no provider is available)
   */
  public setValues(values: Partial<RemoteConfigValues>): void {
    this.cachedValues = { ...this.cachedValues, ...values };
    this.saveCachedValues();
  }

  /**
   * Get boolean value from remote config
   */
  public async getBoolean(
    key: keyof RemoteConfigValues,
    defaultValue?: boolean
  ): Promise<boolean> {
    await this.ensureInitialized();

    // Try cached value first
    if (this.cachedValues[key] !== undefined) {
      return this.cachedValues[key] as boolean;
    }

    // Try provider
    if (this.provider) {
      try {
        const value = await this.provider.getBoolean(
          key,
          defaultValue ?? (DEFAULT_REMOTE_CONFIG[key] as boolean) ?? false
        );
        this.cachedValues[key] = value as any;
        return value;
      } catch (error) {
        console.warn(
          `[RemoteConfigManager] Failed to get boolean ${key}:`,
          error
        );
      }
    }

    // Fallback to default
    return defaultValue ?? (DEFAULT_REMOTE_CONFIG[key] as boolean) ?? false;
  }

  /**
   * Get number value from remote config
   */
  public async getNumber(
    key: keyof RemoteConfigValues,
    defaultValue?: number
  ): Promise<number> {
    await this.ensureInitialized();

    // Try cached value first
    if (this.cachedValues[key] !== undefined) {
      return this.cachedValues[key] as number;
    }

    // Try provider
    if (this.provider) {
      try {
        const value = await this.provider.getNumber(
          key,
          defaultValue ?? (DEFAULT_REMOTE_CONFIG[key] as number) ?? 0
        );
        this.cachedValues[key] = value as any;
        return value;
      } catch (error) {
        console.warn(
          `[RemoteConfigManager] Failed to get number ${key}:`,
          error
        );
      }
    }

    // Fallback to default
    return defaultValue ?? (DEFAULT_REMOTE_CONFIG[key] as number) ?? 0;
  }

  /**
   * Get string value from remote config
   */
  public async getString(
    key: keyof RemoteConfigValues,
    defaultValue?: string
  ): Promise<string> {
    await this.ensureInitialized();

    // Try cached value first
    if (this.cachedValues[key] !== undefined) {
      return this.cachedValues[key] as string;
    }

    // Try provider
    if (this.provider) {
      try {
        const value = await this.provider.getString(
          key,
          defaultValue ?? (DEFAULT_REMOTE_CONFIG[key] as string) ?? ""
        );
        this.cachedValues[key] = value as any;
        return value;
      } catch (error) {
        console.warn(
          `[RemoteConfigManager] Failed to get string ${key}:`,
          error
        );
      }
    }

    // Fallback to default
    return defaultValue ?? (DEFAULT_REMOTE_CONFIG[key] as string) ?? "";
  }

  /**
   * Fetch and activate remote config
   */
  public async fetchAndActivate(): Promise<boolean> {
    if (!this.provider) {
      return false;
    }

    try {
      const success = await this.provider.fetchAndActivate();
      if (success) {
        await this.fetchAndCache();
      }
      return success;
    } catch (error) {
      console.error(
        "[RemoteConfigManager] Failed to fetch and activate:",
        error
      );
      return false;
    }
  }

  /**
   * Get all cached values
   */
  public getCachedValues(): Partial<RemoteConfigValues> {
    return { ...this.cachedValues };
  }

  /**
   * Clear cached values
   */
  public async clearCache(): Promise<void> {
    this.cachedValues = {};
    await AsyncStorage.removeItem("@notification_remote_config");
  }

  /**
   * Private methods
   */
  private async ensureInitialized(): Promise<void> {
    if (!this.isInitialized) {
      await this.initialize();
    }
  }

  private async fetchAndCache(): Promise<void> {
    if (!this.provider) return;

    // Fetch all known config values
    const configKeys = Object.values(RemoteConfigConstants) as Array<
      keyof RemoteConfigValues
    >;

    for (const key of configKeys) {
      try {
        const defaultValue = DEFAULT_REMOTE_CONFIG[key];
        let value: any;

        if (typeof defaultValue === "boolean") {
          value = await this.provider.getBoolean(key, defaultValue);
        } else if (typeof defaultValue === "number") {
          value = await this.provider.getNumber(key, defaultValue);
        } else if (typeof defaultValue === "string") {
          value = await this.provider.getString(key, defaultValue);
        }

        if (value !== undefined) {
          this.cachedValues[key] = value;
        }
      } catch (error) {
        console.warn(`[RemoteConfigManager] Failed to fetch ${key}:`, error);
      }
    }

    await this.saveCachedValues();
  }

  private async loadCachedValues(): Promise<void> {
    try {
      const cached = await AsyncStorage.getItem("@notification_remote_config");
      if (cached) {
        this.cachedValues = JSON.parse(cached);
      }
    } catch (error) {
      console.warn(
        "[RemoteConfigManager] Failed to load cached values:",
        error
      );
    }
  }

  private async saveCachedValues(): Promise<void> {
    try {
      await AsyncStorage.setItem(
        "@notification_remote_config",
        JSON.stringify(this.cachedValues)
      );
    } catch (error) {
      console.warn(
        "[RemoteConfigManager] Failed to save cached values:",
        error
      );
    }
  }
}

// Convenience functions for easy access
export const getRemoteConfigBoolean = async (
  key: keyof RemoteConfigValues,
  defaultValue?: boolean
): Promise<boolean> => {
  return await RemoteConfigManager.getInstance().getBoolean(key, defaultValue);
};

export const getRemoteConfigNumber = async (
  key: keyof RemoteConfigValues,
  defaultValue?: number
): Promise<number> => {
  return await RemoteConfigManager.getInstance().getNumber(key, defaultValue);
};

export const getRemoteConfigString = async (
  key: keyof RemoteConfigValues,
  defaultValue?: string
): Promise<string> => {
  return await RemoteConfigManager.getInstance().getString(key, defaultValue);
};

export const initializeRemoteConfig = async (
  provider?: RemoteConfigProvider
): Promise<void> => {
  await RemoteConfigManager.getInstance().initialize(provider);
};

export const setRemoteConfigValues = (
  values: Partial<RemoteConfigValues>
): void => {
  RemoteConfigManager.getInstance().setValues(values);
};
