import AsyncStorage from "@react-native-async-storage/async-storage";
import {
  UserRemoteConfig,
  DEFAULT_USER_REMOTE_CONFIG,
  validateUserRemoteConfig,
  mergeWithDefaults,
  convertToInternalConfig,
} from "../types/UserRemoteConfig";

/**
 * SimpleRemoteConfigManager - Easy-to-use remote config with typed objects
 * Users simply provide a well-defined config object
 */
export class SimpleRemoteConfigManager {
  private static instance: SimpleRemoteConfigManager;
  private currentConfig: Required<UserRemoteConfig> =
    DEFAULT_USER_REMOTE_CONFIG;
  private isInitialized = false;

  private constructor() {}

  public static getInstance(): SimpleRemoteConfigManager {
    if (!SimpleRemoteConfigManager.instance) {
      SimpleRemoteConfigManager.instance = new SimpleRemoteConfigManager();
    }
    return SimpleRemoteConfigManager.instance;
  }

  /**
   * Initialize with user's config object
   * @param userConfig - Well-defined typed configuration object
   */
  public async initialize(userConfig?: UserRemoteConfig): Promise<void> {
    if (userConfig) {
      await this.setConfig(userConfig);
    } else {
      // Load from storage if no config provided
      await this.loadFromStorage();
    }
    this.isInitialized = true;
  }

  /**
   * Set configuration with validation
   * @param userConfig - User's configuration object
   */
  public async setConfig(userConfig: UserRemoteConfig): Promise<void> {
    // Validate the config
    const validation = validateUserRemoteConfig(userConfig);
    if (!validation.isValid) {
      throw new Error(`Invalid remote config: ${validation.errors.join(", ")}`);
    }

    // Merge with defaults and store
    this.currentConfig = mergeWithDefaults(userConfig);
    await this.saveToStorage();

    console.log("[SimpleRemoteConfigManager] Configuration updated:", {
      notificationVersion: this.currentConfig.notificationVersion,
      notificationLimit: this.currentConfig.notificationLimit,
      notificationKeepAtTop: this.currentConfig.notificationKeepAtTop,
    });
  }

  /**
   * Get boolean value from config
   */
  public getBoolean(key: keyof UserRemoteConfig): boolean {
    this.ensureInitialized();
    const value = this.currentConfig[key];
    return typeof value === "boolean" ? value : false;
  }

  /**
   * Get number value from config
   */
  public getNumber(key: keyof UserRemoteConfig): number {
    this.ensureInitialized();
    const value = this.currentConfig[key];
    return typeof value === "number" ? value : 0;
  }

  /**
   * Get string value from config
   */
  public getString(key: keyof UserRemoteConfig): string {
    this.ensureInitialized();
    const value = this.currentConfig[key];
    return typeof value === "string" ? value : "";
  }

  /**
   * Get the complete current configuration
   */
  public getCurrentConfig(): Required<UserRemoteConfig> {
    this.ensureInitialized();
    return { ...this.currentConfig };
  }

  /**
   * Update specific config values
   */
  public async updateConfig(updates: Partial<UserRemoteConfig>): Promise<void> {
    const newConfig = { ...this.currentConfig, ...updates };
    await this.setConfig(newConfig);
  }

  /**
   * Reset to default configuration
   */
  public async resetToDefaults(): Promise<void> {
    this.currentConfig = { ...DEFAULT_USER_REMOTE_CONFIG };
    await this.saveToStorage();
  }

  /**
   * Get config in internal format (for compatibility with existing code)
   */
  public getInternalConfig(): { [key: string]: any } {
    return convertToInternalConfig(this.currentConfig);
  }

  /**
   * Check if a specific feature is enabled
   */
  public isFeatureEnabled(feature: keyof UserRemoteConfig): boolean {
    return this.getBoolean(feature);
  }

  /**
   * Get notification version
   */
  public getNotificationVersion(): number {
    return this.getNumber("notificationVersion");
  }

  /**
   * Get notification limit
   */
  public getNotificationLimit(): number {
    return this.getNumber("notificationLimit");
  }

  /**
   * Check if notifications should be kept at top
   */
  public shouldKeepNotificationsAtTop(): boolean {
    return this.getBoolean("notificationKeepAtTop");
  }

  /**
   * Get cricket notification interval
   */
  public getCricketNotificationInterval(): number {
    return this.getNumber("cricketNotificationInterval");
  }

  /**
   * Private methods
   */
  private ensureInitialized(): void {
    if (!this.isInitialized) {
      console.warn(
        "[SimpleRemoteConfigManager] Not initialized, using defaults"
      );
      this.currentConfig = { ...DEFAULT_USER_REMOTE_CONFIG };
    }
  }

  private async saveToStorage(): Promise<void> {
    try {
      await AsyncStorage.setItem(
        "@simple_remote_config",
        JSON.stringify(this.currentConfig)
      );
    } catch (error) {
      console.error(
        "[SimpleRemoteConfigManager] Failed to save config:",
        error
      );
    }
  }

  private async loadFromStorage(): Promise<void> {
    try {
      const stored = await AsyncStorage.getItem("@simple_remote_config");
      if (stored) {
        const parsed = JSON.parse(stored);
        this.currentConfig = mergeWithDefaults(parsed);
      } else {
        this.currentConfig = { ...DEFAULT_USER_REMOTE_CONFIG };
      }
    } catch (error) {
      console.error(
        "[SimpleRemoteConfigManager] Failed to load config:",
        error
      );
      this.currentConfig = { ...DEFAULT_USER_REMOTE_CONFIG };
    }
  }
}

// Convenience functions for easy access
export const initializeSimpleRemoteConfig = async (
  userConfig?: UserRemoteConfig
): Promise<void> => {
  await SimpleRemoteConfigManager.getInstance().initialize(userConfig);
};

export const setSimpleRemoteConfig = async (
  userConfig: UserRemoteConfig
): Promise<void> => {
  await SimpleRemoteConfigManager.getInstance().setConfig(userConfig);
};

export const updateSimpleRemoteConfig = async (
  updates: Partial<UserRemoteConfig>
): Promise<void> => {
  await SimpleRemoteConfigManager.getInstance().updateConfig(updates);
};

export const getSimpleRemoteConfig = (): Required<UserRemoteConfig> => {
  return SimpleRemoteConfigManager.getInstance().getCurrentConfig();
};

export const isFeatureEnabled = (feature: keyof UserRemoteConfig): boolean => {
  return SimpleRemoteConfigManager.getInstance().isFeatureEnabled(feature);
};

export const getNotificationVersion = (): number => {
  return SimpleRemoteConfigManager.getInstance().getNotificationVersion();
};

export const shouldKeepNotificationsAtTop = (): boolean => {
  return SimpleRemoteConfigManager.getInstance().shouldKeepNotificationsAtTop();
};
