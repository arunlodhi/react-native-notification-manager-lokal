import { NativeModules, Platform } from "react-native";
import { Constants } from "../types/Constants";
import {
  ActiveNotification,
  NotificationData,
} from "../types/NotificationTypes";
import { NotificationRefreshCallbacks } from "../interfaces/NotificationCallbacks";
import { NotificationReCreator } from "./NotificationReCreator";
import {
  getRemoteConfigNumber,
  getRemoteConfigBoolean,
} from "../managers/RemoteConfigManager";
import { RemoteConfigConstants } from "../types/RemoteConfigTypes";
import { recordException } from "../managers/AnalyticsManager";
import AsyncStorage from "@react-native-async-storage/async-storage";

const { NotificationRefresherModule } = NativeModules;

/**
 * NotificationRefresher - Exact port of Android NotificationRefresher.kt
 * Handles re-pushing notifications to keep them at the top
 */
export class NotificationRefresher {
  private static instance: NotificationRefresher;
  private callbacks?: NotificationRefreshCallbacks;

  private static readonly KEY_LAST_REFRESH_TIME = "last_refresh_time";
  private static readonly DEFAULT_REFRESH_TIME = Constants.DEFAULT_REFRESH_TIME;
  private static readonly RESET_TIMEOUT = Constants.RESET_TIMEOUT;
  private static readonly NOTIFY_DELAY = Constants.NOTIFY_DELAY;

  private constructor() {}

  public static getInstance(): NotificationRefresher {
    if (!NotificationRefresher.instance) {
      NotificationRefresher.instance = new NotificationRefresher();
    }
    return NotificationRefresher.instance;
  }

  public setCallbacks(callbacks: NotificationRefreshCallbacks): void {
    this.callbacks = callbacks;
  }

  /**
   * Checks if refresh can be triggered based on timeout
   * Matches Android logic exactly
   */
  private async canTriggerRefresh(): Promise<boolean> {
    const currentTime = Date.now();
    const lastRefreshTime = await this.getLastRefreshTime();

    const timeout = await this.getRemoteConfigLong(
      RemoteConfigConstants.NOTIFICATION_UNLOCK_AT_TOP_TIMEOUT_MS,
      NotificationRefresher.DEFAULT_REFRESH_TIME
    );

    if (currentTime - lastRefreshTime >= timeout) {
      await this.saveLastRefreshTime(currentTime);
      return true;
    }
    return false;
  }

  /**
   * Gets the last refresh time from storage
   */
  private async getLastRefreshTime(): Promise<number> {
    return await this.getStoredLong(
      NotificationRefresher.KEY_LAST_REFRESH_TIME,
      0
    );
  }

  /**
   * Saves the last refresh time to storage
   */
  private async saveLastRefreshTime(time: number): Promise<void> {
    await this.storeLong(NotificationRefresher.KEY_LAST_REFRESH_TIME, time);
  }

  /**
   * Main refresh method - matches Android logic exactly
   * Refreshes notifications to keep them at the top
   */
  public async refreshNotifications(): Promise<void> {
    try {
      if (!(await this.canTriggerRefresh())) {
        return;
      }

      if (
        typeof Platform.Version === "number" &&
        Platform.Version >= Constants.VERSION_CODES_M
      ) {
        const activeNotifications = await this.getActiveNotifications();

        const currentAppNotifications: ActiveNotification[] = [];
        for (const notification of activeNotifications) {
          if (notification.packageName === (await this.getPackageName())) {
            currentAppNotifications.push(notification);
          }
        }

        const limit = await this.getRemoteConfigInt(
          RemoteConfigConstants.NOTIFICATION_UNLOCK_AT_TOP_LIMIT,
          3
        );

        if (currentAppNotifications.length < limit) {
          return;
        }

        // Sort notifications by timestamp in descending order (newest first)
        const sortedNotifications = currentAppNotifications.sort((a, b) => {
          const timeA =
            a.notification.extras[Constants.NOTIFICATION_TIME_EXTRA] || 0;
          const timeB =
            b.notification.extras[Constants.NOTIFICATION_TIME_EXTRA] || 0;
          return timeB - timeA; // Descending order
        });

        this.callbacks?.onRefreshStarted();
        await this.refreshNotificationsList(sortedNotifications);
      }
    } catch (exception) {
      const error =
        exception instanceof Error ? exception : new Error(String(exception));
      console.error(
        "[NotificationRefresher] Notification refresh exception:",
        error.message
      );
      this.callbacks?.onRefreshFailed(error);
      await this.recordException(error);
    }
  }

  /**
   * Refreshes the list of notifications
   * Matches Android logic with device-specific handling
   */
  private async refreshNotificationsList(
    sortedNotifications: ActiveNotification[]
  ): Promise<void> {
    try {
      // Reverse to get oldest first (matches Android logic)
      const existingNotifications = [...sortedNotifications].reverse();

      if (await this.isXiaomiDevice()) {
        // Xiaomi device logic - simple timestamp update
        let refreshedCount = 0;
        for (const notification of existingNotifications) {
          await this.delay(NotificationRefresher.NOTIFY_DELAY);

          // Update timestamp and re-post notification
          notification.notification.when = Date.now();
          const id =
            notification.notification.extras[
              Constants.NOTIFICATION_REFRESH_ID_EXTRA
            ];
          if (id) {
            await this.notifyNotification(id, notification);
            refreshedCount++;
          }
        }
        this.callbacks?.onRefreshCompleted(refreshedCount);
      } else {
        // Non-Xiaomi device logic - recreate notifications from database
        const cachedNotifications = await this.getTodayNotifications();
        let refreshedCount = 0;

        for (const notification of existingNotifications) {
          try {
            await this.delay(NotificationRefresher.NOTIFY_DELAY);

            const id =
              notification.notification.extras[
                Constants.NOTIFICATION_REFRESH_ID_EXTRA
              ];
            if (!id) continue;

            const notificationToRefresh = cachedNotifications.find(
              (cached) => cached.notificationId === id
            );

            if (notificationToRefresh) {
              const notificationPayload = this.convertToNotificationPayload(
                notificationToRefresh
              );

              // Cancel existing notification
              await this.cancelNotification(id);

              // Recreate notification
              await NotificationReCreator.getInstance().createNotification(
                notificationPayload
              );
              refreshedCount++;
            }
          } catch (exception) {
            const error =
              exception instanceof Error
                ? exception
                : new Error(String(exception));
            console.error(
              "[NotificationRefresher] Failed to refresh notification:",
              error.message
            );
          }
        }
        this.callbacks?.onRefreshCompleted(refreshedCount);
      }

      // Reset silent push flag after timeout (matches Android logic)
      setTimeout(() => {
        NotificationReCreator.getInstance().setSilentPush(false);
      }, NotificationRefresher.RESET_TIMEOUT);
    } catch (exception) {
      const error =
        exception instanceof Error ? exception : new Error(String(exception));
      console.error(
        "[NotificationRefresher] Failed to refresh notifications:",
        error.message
      );
      this.callbacks?.onRefreshFailed(error);
    }
  }

  /**
   * Converts NotificationData to NotificationPayload
   * Matches Android toNotificationPayload() extension function
   */
  private convertToNotificationPayload(
    notificationData: NotificationData
  ): any {
    let extraData: any = {};
    try {
      extraData = JSON.parse(notificationData.extra);
    } catch (e) {
      console.warn(
        "[NotificationRefresher] Failed to parse notification extra data"
      );
    }

    const notificationBody = extraData[Constants.BODY_EXTRA] || "";
    const notificationCategoryName = extraData[Constants.CATEGORY_NAME] || "";

    return {
      imageUrl: notificationData.postImage,
      notificationId: notificationData.notificationId,
      groupID: parseInt(notificationData.groupId) || 0,
      title: notificationData.title,
      action: notificationData.action,
      body: notificationBody,
      categoryId: notificationData.categoryType,
      categoryName: notificationCategoryName,
      notifType: "",
      tag: notificationData.tag,
      uri: notificationData.uri,
      channel: Constants.DEFAULT_CHANNEL,
      importance: Constants.IMPORTANCE_HIGH,
      isPersonalized: false,
      isGroupingNeeded: false,
    };
  }

  /**
   * Utility methods for native module calls
   */
  private async getActiveNotifications(): Promise<ActiveNotification[]> {
    if (Platform.OS === "android") {
      return await NotificationRefresherModule.getActiveNotifications();
    }
    return [];
  }

  private async getPackageName(): Promise<string> {
    if (Platform.OS === "android") {
      return await NotificationRefresherModule.getPackageName();
    }
    return "";
  }

  private async isXiaomiDevice(): Promise<boolean> {
    if (Platform.OS === "android") {
      return await NotificationRefresherModule.isXiaomiDevice();
    }
    return false;
  }

  private async notifyNotification(
    id: number,
    notification: ActiveNotification
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationRefresherModule.notifyNotification(id, notification);
    }
  }

  private async cancelNotification(id: number): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationRefresherModule.cancelNotification(id);
    }
  }

  private async getTodayNotifications(): Promise<NotificationData[]> {
    if (Platform.OS === "android") {
      return await NotificationRefresherModule.getTodayNotifications();
    }
    return [];
  }

  /**
   * Storage and configuration methods
   */
  private async getStoredLong(
    key: string,
    defaultValue: number
  ): Promise<number> {
    try {
      const value = await AsyncStorage.getItem(key);
      return value ? parseInt(value, 10) : defaultValue;
    } catch (error) {
      console.warn(
        `[NotificationRefresher] Failed to get stored long ${key}:`,
        error
      );
      return defaultValue;
    }
  }

  private async storeLong(key: string, value: number): Promise<void> {
    try {
      await AsyncStorage.setItem(key, value.toString());
    } catch (error) {
      console.warn(
        `[NotificationRefresher] Failed to store long ${key}:`,
        error
      );
    }
  }

  private async getRemoteConfigLong(
    key: string,
    defaultValue: number
  ): Promise<number> {
    return await getRemoteConfigNumber(key as any, defaultValue);
  }

  private async getRemoteConfigInt(
    key: string,
    defaultValue: number
  ): Promise<number> {
    return await getRemoteConfigNumber(key as any, defaultValue);
  }

  private async recordException(exception: Error): Promise<void> {
    recordException(exception);
  }

  private async delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}

// Static method to match Android usage pattern
export const refreshNotifications = async (): Promise<void> => {
  await NotificationRefresher.getInstance().refreshNotifications();
};
