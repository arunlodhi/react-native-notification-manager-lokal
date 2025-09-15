import { NativeModules, Platform, NativeEventEmitter } from "react-native";
import { Constants, RemoteConfigConstants } from "../types/Constants";
import {
  NotificationPayload,
  NotificationData,
  UserPreferences,
} from "../types/NotificationTypes";
import {
  NotificationEventCallbacks,
  NotificationCallbacks,
} from "../interfaces/NotificationCallbacks";
import { NotificationLimiter } from "../utils/NotificationLimiter";
import { NotificationRefresher } from "../utils/NotificationRefresher";
import { NotificationReCreator } from "../utils/NotificationReCreator";
import { NotificationUtil } from "../utils/NotificationUtil";

const { NotificationManagerModule } = NativeModules;

/**
 * NotificationManager - Main entry point for React Native notification functionality
 * Maintains exact same logic as Android while providing React Native integration
 */
export class NotificationManager {
  private static instance: NotificationManager;
  private eventEmitter: NativeEventEmitter;
  private eventCallbacks?: NotificationEventCallbacks;
  private isInitialized = false;

  private constructor() {
    this.eventEmitter = new NativeEventEmitter(NotificationManagerModule);
    this.setupEventListeners();
  }

  public static getInstance(): NotificationManager {
    if (!NotificationManager.instance) {
      NotificationManager.instance = new NotificationManager();
    }
    return NotificationManager.instance;
  }

  /**
   * Initialize the notification manager
   * Sets up all the components with exact Android logic
   */
  public async initialize(): Promise<void> {
    if (this.isInitialized) {
      return;
    }

    try {
      // Initialize native module
      await NotificationManagerModule.initialize();

      // Set up notification limiter
      NotificationLimiter.getInstance().setCallbacks({
        onNotificationsLimited: (removedCount: number) => {
          console.log(
            `[NotificationManager] Limited ${removedCount} notifications`
          );
        },
        onLimitCheckCompleted: (activeCount: number, limit: number) => {
          console.log(
            `[NotificationManager] Limit check: ${activeCount}/${limit} notifications`
          );
        },
      });

      // Set up notification refresher
      NotificationRefresher.getInstance().setCallbacks({
        onRefreshStarted: () => {
          console.log("[NotificationManager] Notification refresh started");
        },
        onRefreshCompleted: (refreshedCount: number) => {
          console.log(
            `[NotificationManager] Refreshed ${refreshedCount} notifications`
          );
        },
        onRefreshFailed: (error: Error) => {
          console.error(
            "[NotificationManager] Notification refresh failed:",
            error
          );
        },
      });

      this.isInitialized = true;
      console.log("[NotificationManager] Initialized successfully");
    } catch (error) {
      console.error("[NotificationManager] Initialization failed:", error);
      throw error;
    }
  }

  /**
   * Set event callbacks for notification interactions
   * These will be called when notifications are clicked/dismissed
   */
  public setEventCallbacks(callbacks: NotificationEventCallbacks): void {
    this.eventCallbacks = callbacks;
  }

  /**
   * Creates a notification without image - matches Android logic exactly
   */
  public async createNotification(
    id: number,
    title: string,
    body: string,
    categoryId: string,
    categoryName: string,
    uri: string,
    action: string,
    tag: string,
    channel: string = Constants.DEFAULT_CHANNEL,
    importance: number = Constants.IMPORTANCE_HIGH,
    isGroupingNeeded: boolean = false,
    groupID: number = 0,
    notifType: string = "",
    isPersonalized: boolean = false,
    callbacks?: NotificationCallbacks
  ): Promise<void> {
    await this.ensureInitialized();

    // Validate notification using exact Android logic
    if (!(await this.isNotificationValid(String(id), String(groupID)))) {
      console.log(
        `[NotificationManager] Notification ${id} is not valid, skipping`
      );
      return;
    }

    try {
      // Apply notification limiting before creation
      await NotificationLimiter.getInstance().limitNotifications();

      // Create notification via native module
      await NotificationManagerModule.createNotification({
        id,
        title,
        body,
        categoryId,
        categoryName,
        uri,
        action,
        tag,
        channel,
        importance,
        isGroupingNeeded,
        groupID,
        notifType,
        isPersonalized,
      });

      // Call analytics callback
      if (callbacks) {
        const bundle = await this.getNotificationBuiltBundle(
          uri,
          String(id),
          this.getPostIdFromUri(uri),
          categoryId,
          channel,
          importance,
          notifType
        );
        callbacks.onNotificationBuilt(bundle);
      }

      console.log(`[NotificationManager] Created notification ${id}`);
    } catch (error) {
      console.error(
        `[NotificationManager] Failed to create notification ${id}:`,
        error
      );
      throw error;
    }
  }

  /**
   * Creates a notification with image - matches Android logic exactly
   */
  public async createNotificationWithImage(
    id: number,
    imageUrl: string,
    title: string,
    body: string,
    categoryId: string,
    categoryName: string,
    uri: string,
    action: string,
    tag: string,
    channel: string = Constants.DEFAULT_CHANNEL,
    importance: number = Constants.IMPORTANCE_HIGH,
    isGroupingNeeded: boolean = false,
    groupID: number = 0,
    notifType: string = "",
    isPersonalized: boolean = false,
    callbacks?: NotificationCallbacks
  ): Promise<void> {
    await this.ensureInitialized();

    // Validate notification using exact Android logic
    if (!(await this.isNotificationValid(String(id), String(groupID)))) {
      console.log(
        `[NotificationManager] Notification ${id} is not valid, skipping`
      );
      return;
    }

    try {
      // Apply notification limiting before creation
      await NotificationLimiter.getInstance().limitNotifications();

      // Create notification with image via native module
      await NotificationManagerModule.createNotificationWithImage({
        id,
        imageUrl,
        title,
        body,
        categoryId,
        categoryName,
        uri,
        action,
        tag,
        channel,
        importance,
        isGroupingNeeded,
        groupID,
        notifType,
        isPersonalized,
      });

      // Call analytics callback
      if (callbacks) {
        const bundle = await this.getNotificationBuiltBundle(
          uri,
          String(id),
          this.getPostIdFromUri(uri),
          categoryId,
          channel,
          importance,
          notifType
        );
        callbacks.onNotificationBuilt(bundle);
      }

      console.log(
        `[NotificationManager] Created notification with image ${id}`
      );
    } catch (error) {
      console.error(
        `[NotificationManager] Failed to create notification with image ${id}:`,
        error
      );
      throw error;
    }
  }

  /**
   * Creates a notification with custom layout and user preferences - NEW METHOD
   * This method allows users to pass language and notification preferences directly
   */
  public async createNotificationWithCustomLayout(
    id: number,
    title: string,
    body: string,
    categoryId: string,
    categoryName: string,
    uri: string,
    action: string,
    userPreferences: UserPreferences,
    options?: {
      imageUrl?: string;
      channel?: string;
      importance?: number;
      notificationVersion?: number;
      isGroupingNeeded?: boolean;
      groupID?: number;
      notifType?: string;
      isPersonalized?: boolean;
    },
    callbacks?: NotificationCallbacks
  ): Promise<void> {
    await this.ensureInitialized();

    const {
      imageUrl,
      channel = Constants.DEFAULT_CHANNEL,
      importance = Constants.IMPORTANCE_HIGH,
      notificationVersion = 1,
      isGroupingNeeded = false,
      groupID = 0,
      notifType = "",
      isPersonalized = false,
    } = options || {};

    // Validate notification using exact Android logic
    if (!(await this.isNotificationValid(String(id), String(groupID)))) {
      console.log(
        `[NotificationManager] Notification ${id} is not valid, skipping`
      );
      return;
    }

    try {
      // Apply notification limiting before creation
      await NotificationLimiter.getInstance().limitNotifications();

      // Create notification with custom layout via native module
      await NotificationManagerModule.createNotificationWithCustomLayout({
        id,
        title,
        body,
        categoryId,
        categoryName,
        uri,
        action,
        channel,
        importance,
        notificationVersion,
        imageUrl,
        isGroupingNeeded,
        groupID,
        notifType,
        isPersonalized,
        // Pass user preferences directly
        selectedLanguage: userPreferences.selectedLanguage,
        preferredLocale: userPreferences.preferredLocale,
        isNotificationGroupingActive:
          userPreferences.isNotificationGroupingActive,
        keepNotificationAtTop: userPreferences.keepNotificationAtTop,
        isSilentPush: userPreferences.isSilentPush,
      });

      // Call analytics callback
      if (callbacks) {
        const bundle = await this.getNotificationBuiltBundle(
          uri,
          String(id),
          this.getPostIdFromUri(uri),
          categoryId,
          channel,
          importance,
          notifType
        );
        callbacks.onNotificationBuilt(bundle);
      }

      console.log(
        `[NotificationManager] Created custom layout notification ${id} with language: ${
          userPreferences.selectedLanguage || "en"
        }`
      );
    } catch (error) {
      console.error(
        `[NotificationManager] Failed to create custom layout notification ${id}:`,
        error
      );
      throw error;
    }
  }

  /**
   * Recreates a notification from stored data - used by refresh logic
   */
  public async recreateNotification(
    notificationData: NotificationData
  ): Promise<void> {
    await this.ensureInitialized();

    const notificationPayload =
      NotificationReCreator.getInstance().convertToNotificationPayload(
        notificationData
      );
    await NotificationReCreator.getInstance().createNotification(
      notificationPayload
    );
  }

  /**
   * Cancels a notification by ID
   */
  public async cancelNotification(notificationId: number): Promise<void> {
    await this.ensureInitialized();

    try {
      await NotificationManagerModule.cancelNotification(notificationId);
      console.log(
        `[NotificationManager] Cancelled notification ${notificationId}`
      );
    } catch (error) {
      console.error(
        `[NotificationManager] Failed to cancel notification ${notificationId}:`,
        error
      );
      throw error;
    }
  }

  /**
   * Refreshes notifications to keep them at top - matches Android logic exactly
   */
  public async refreshNotifications(): Promise<void> {
    await this.ensureInitialized();
    await NotificationRefresher.getInstance().refreshNotifications();
  }

  /**
   * Limits notifications based on configuration - matches Android logic exactly
   */
  public async limitNotifications(): Promise<void> {
    await this.ensureInitialized();
    await NotificationLimiter.getInstance().limitNotifications();
  }

  /**
   * Gets active notifications from the system
   */
  public async getActiveNotifications(): Promise<any[]> {
    await this.ensureInitialized();
    return await NotificationManagerModule.getActiveNotifications();
  }

  /**
   * Validates notification using exact Android logic - now uses consolidated NotificationUtil methods
   */
  private async isNotificationValid(
    notificationId: string,
    sampleGroupId: string
  ): Promise<boolean> {
    // Use consolidated method from NotificationUtil
    return await NotificationUtil.isNotificationValid(
      notificationId,
      sampleGroupId
    );
  }

  /**
   * Sets up event listeners for notification interactions
   */
  private setupEventListeners(): void {
    this.eventEmitter.addListener("onNotificationClick", (data: any) => {
      console.log("[NotificationManager] Notification clicked:", data);
      this.eventCallbacks?.onNotificationClick?.(data);
    });

    this.eventEmitter.addListener(
      "onNotificationDismiss",
      (notificationId: number) => {
        console.log(
          "[NotificationManager] Notification dismissed:",
          notificationId
        );
        this.eventCallbacks?.onNotificationDismiss?.(notificationId);
      }
    );

    this.eventEmitter.addListener("onNotificationReceived", (data: any) => {
      console.log("[NotificationManager] Notification received:", data);
      this.eventCallbacks?.onNotificationReceived?.(data);
    });
  }

  /**
   * Utility methods
   */
  private async ensureInitialized(): Promise<void> {
    if (!this.isInitialized) {
      await this.initialize();
    }
  }

  // Removed duplicate utility methods - now using consolidated methods from NotificationUtil
  private getPostIdFromUri(uri: string): string {
    return NotificationUtil.getPostIdFromUri(uri);
  }

  private async getNotificationBuiltBundle(
    uri: string | null,
    notificationId: string,
    postId: string,
    categoryId: string,
    channel: string,
    importance: number,
    notifType: string
  ): Promise<{ [key: string]: any }> {
    return await NotificationUtil.getNotificationBuiltBundle(
      uri,
      notificationId,
      postId,
      categoryId,
      channel,
      importance,
      notifType
    );
  }

  private async getStoredIntArray(key: string): Promise<number[]> {
    return await NotificationUtil.getStoredIntArray(key);
  }

  private async storeIntArray(key: string, array: number[]): Promise<void> {
    await NotificationUtil.storeIntArray(key, array);
  }
}

// Static methods for convenience
export const initializeNotifications = async (): Promise<void> => {
  await NotificationManager.getInstance().initialize();
};

export const createNotification = async (
  id: number,
  title: string,
  body: string,
  categoryId: string,
  categoryName: string,
  uri: string,
  action: string,
  tag: string,
  callbacks?: NotificationCallbacks
): Promise<void> => {
  await NotificationManager.getInstance().createNotification(
    id,
    title,
    body,
    categoryId,
    categoryName,
    uri,
    action,
    tag,
    Constants.DEFAULT_CHANNEL,
    Constants.IMPORTANCE_HIGH,
    false,
    0,
    "",
    false,
    callbacks
  );
};

export const createNotificationWithImage = async (
  id: number,
  imageUrl: string,
  title: string,
  body: string,
  categoryId: string,
  categoryName: string,
  uri: string,
  action: string,
  tag: string,
  callbacks?: NotificationCallbacks
): Promise<void> => {
  await NotificationManager.getInstance().createNotificationWithImage(
    id,
    imageUrl,
    title,
    body,
    categoryId,
    categoryName,
    uri,
    action,
    tag,
    Constants.DEFAULT_CHANNEL,
    Constants.IMPORTANCE_HIGH,
    false,
    0,
    "",
    false,
    callbacks
  );
};

export const cancelNotification = async (
  notificationId: number
): Promise<void> => {
  await NotificationManager.getInstance().cancelNotification(notificationId);
};

export const refreshNotifications = async (): Promise<void> => {
  await NotificationManager.getInstance().refreshNotifications();
};

export const limitNotifications = async (): Promise<void> => {
  await NotificationManager.getInstance().limitNotifications();
};

export const createCricketNotification = async (
  matchState: string,
  team1Name: string,
  team2Name: string,
  team1ShortName: string,
  team2ShortName: string,
  team1IconUrl?: string,
  team2IconUrl?: string,
  matchStatus?: string,
  venue?: string,
  team1Score?: string,
  team1Wickets?: string,
  team1Overs?: string,
  team2Score?: string,
  team2Wickets?: string,
  team2Overs?: string
): Promise<void> => {
  const manager = NotificationManager.getInstance();
  await manager.initialize();

  await NotificationManagerModule.createCricketNotification({
    matchState,
    team1Name,
    team2Name,
    team1ShortName,
    team2ShortName,
    team1IconUrl,
    team2IconUrl,
    matchStatus,
    venue,
    team1Score,
    team1Wickets,
    team1Overs,
    team2Score,
    team2Wickets,
    team2Overs,
  });
};

export const createQuizNotification = async (
  id: number,
  imageUrl: string,
  title: string,
  body: string,
  categoryId: string,
  categoryName: string,
  uri: string,
  action: string,
  tag: string,
  channel: string = Constants.DEFAULT_CHANNEL,
  importance: number = Constants.IMPORTANCE_HIGH
): Promise<void> => {
  const manager = NotificationManager.getInstance();
  await manager.initialize();

  await NotificationManagerModule.createQuizNotification({
    id,
    imageUrl,
    title,
    body,
    categoryId,
    categoryName,
    uri,
    action,
    tag,
    channel,
    importance,
  });
};

export const createCommentNotification = async (
  title: string,
  body: string,
  notificationType: number,
  notificationInterval: string,
  postId: string,
  reporterId: number,
  userId: string,
  postCount: number,
  commentsCount: number,
  isGrouped: boolean = false
): Promise<void> => {
  const manager = NotificationManager.getInstance();
  await manager.initialize();

  await NotificationManagerModule.createCommentNotification({
    title,
    body,
    notificationType,
    notificationInterval,
    postId,
    reporterId,
    userId,
    postCount,
    commentsCount,
    isGrouped,
  });
};

export const getMatrimonyNotificationType = async (
  notificationId: number
): Promise<string | null> => {
  return await NotificationManagerModule.getMatrimonyNotificationType(
    notificationId
  );
};

export const setSilentPush = async (isSilent: boolean): Promise<void> => {
  await NotificationManagerModule.setSilentPush(isSilent);
};

export const setNotificationVersion = async (
  version: number
): Promise<void> => {
  await NotificationManagerModule.setNotificationVersion(version);
};

/**
 * NEW: Create notification with custom layout and user preferences - Convenience function
 * This is the main function users should use for advanced notifications with language support
 */
export const createNotificationWithCustomLayout = async (
  id: number,
  title: string,
  body: string,
  categoryId: string,
  categoryName: string,
  uri: string,
  action: string,
  userPreferences: UserPreferences,
  options?: {
    imageUrl?: string;
    channel?: string;
    importance?: number;
    notificationVersion?: number;
    isGroupingNeeded?: boolean;
    groupID?: number;
    notifType?: string;
    isPersonalized?: boolean;
  },
  callbacks?: NotificationCallbacks
): Promise<void> => {
  await NotificationManager.getInstance().createNotificationWithCustomLayout(
    id,
    title,
    body,
    categoryId,
    categoryName,
    uri,
    action,
    userPreferences,
    options,
    callbacks
  );
};

/**
 * Locale Management Functions
 * Set the app locale for notifications and UI
 */

/**
 * Set the app locale for notifications and UI
 * @param languageCode - Language code (e.g., 'en', 'hi', 'ta', 'te', etc.)
 */
export const setAppLocale = async (languageCode: string): Promise<void> => {
  await NotificationManagerModule.setAppLocale(languageCode);
};
