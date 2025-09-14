import { NativeModules, Platform } from "react-native";
import { Constants, RemoteConfigConstants } from "../types/Constants";
import { ActiveNotification } from "../types/NotificationTypes";
import { NotificationLimitCallbacks } from "../interfaces/NotificationCallbacks";

const { NotificationLimiterModule } = NativeModules;

/**
 * NotificationLimiter - Exact port of Android NotificationLimiter.kt
 * Maintains notification order and limits based on timestamp
 */
export class NotificationLimiter {
  private static instance: NotificationLimiter;
  private callbacks?: NotificationLimitCallbacks;

  private constructor() {}

  public static getInstance(): NotificationLimiter {
    if (!NotificationLimiter.instance) {
      NotificationLimiter.instance = new NotificationLimiter();
    }
    return NotificationLimiter.instance;
  }

  public setCallbacks(callbacks: NotificationLimitCallbacks): void {
    this.callbacks = callbacks;
  }

  /**
   * Limits notifications based on the exact same logic as Android
   * Sorts by timestamp and removes oldest notifications when limit is exceeded
   */
  public async limitNotifications(): Promise<void> {
    try {
      const keepAtTop = await this.getRemoteConfigBoolean(
        RemoteConfigConstants.NOTIFICATION_KEEP_AT_TOP,
        false
      );

      if (
        keepAtTop &&
        typeof Platform.Version === "number" &&
        Platform.Version >= Constants.VERSION_CODES_M
      ) {
        const activeNotifications = await this.getActiveNotifications();

        let notificationLimit = await this.getRemoteConfigInt(
          RemoteConfigConstants.NOTIFICATION_LIMIT,
          0
        );

        if (notificationLimit === 0) {
          console.log(
            "[NotificationLimiter] Not removing older notifications since limit is 0"
          );
          this.callbacks?.onLimitCheckCompleted(
            activeNotifications.length,
            notificationLimit
          );
          return;
        }

        if (notificationLimit > 1) {
          notificationLimit -= 1;
        }

        const currentAppNotifications: ActiveNotification[] = [];
        for (const notification of activeNotifications) {
          if (notification.packageName === (await this.getPackageName())) {
            currentAppNotifications.push(notification);
          }
        }

        // Sort notifications by timestamp in descending order (newest first)
        // This matches the exact logic from Android: sortedByDescending
        const sortedNotifications = currentAppNotifications.sort((a, b) => {
          const timeA =
            a.notification.extras[Constants.NOTIFICATION_TIME_EXTRA] || 0;
          const timeB =
            b.notification.extras[Constants.NOTIFICATION_TIME_EXTRA] || 0;
          return timeB - timeA; // Descending order
        });

        if (sortedNotifications.length <= notificationLimit) {
          console.log(
            "[NotificationLimiter] Not removing older notifications since active notifications are less than limit"
          );
          this.callbacks?.onLimitCheckCompleted(
            sortedNotifications.length,
            notificationLimit
          );
          return;
        }

        // Remove oldest notifications (keep newest ones)
        // This matches Android logic: sortedNotifications.reversed().dropLast(notificationLimit)
        const notificationsToRemove = sortedNotifications
          .reverse() // Oldest first
          .slice(0, sortedNotifications.length - notificationLimit); // Drop last N (keep newest N)

        let removedCount = 0;
        for (const notification of notificationsToRemove) {
          const id =
            notification.notification.extras[
              Constants.NOTIFICATION_REFRESH_ID_EXTRA
            ];
          if (id) {
            await this.cancelNotification(id);
            removedCount++;
          }
        }

        this.callbacks?.onNotificationsLimited(removedCount);
        this.callbacks?.onLimitCheckCompleted(
          sortedNotifications.length - removedCount,
          notificationLimit
        );
      }
    } catch (exception) {
      const error =
        exception instanceof Error ? exception : new Error(String(exception));
      console.error(
        "[NotificationLimiter] Exception cancelling notifications:",
        error.message
      );
      await this.recordException(error);
    }
  }

  /**
   * Gets active notifications from the system
   * Uses native module to access Android NotificationManager.getActiveNotifications()
   */
  private async getActiveNotifications(): Promise<ActiveNotification[]> {
    if (Platform.OS === "android") {
      return await NotificationLimiterModule.getActiveNotifications();
    }
    return [];
  }

  /**
   * Cancels a notification by ID
   * Uses native module to call Android NotificationManager.cancel()
   */
  private async cancelNotification(notificationId: number): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationLimiterModule.cancelNotification(notificationId);
    }
  }

  /**
   * Gets the current app's package name
   */
  private async getPackageName(): Promise<string> {
    if (Platform.OS === "android") {
      return await NotificationLimiterModule.getPackageName();
    }
    return "";
  }

  /**
   * Gets remote config boolean value
   * This should be implemented to match your remote config system
   */
  private async getRemoteConfigBoolean(
    key: string,
    defaultValue: boolean
  ): Promise<boolean> {
    // TODO: Implement your remote config logic here
    // For now, return default value
    return defaultValue;
  }

  /**
   * Gets remote config integer value
   * This should be implemented to match your remote config system
   */
  private async getRemoteConfigInt(
    key: string,
    defaultValue: number
  ): Promise<number> {
    // TODO: Implement your remote config logic here
    // For now, return default value
    return defaultValue;
  }

  /**
   * Records exception to crash reporting system
   * This should be implemented to match your crash reporting system
   */
  private async recordException(exception: Error): Promise<void> {
    // TODO: Implement your crash reporting logic here (Firebase Crashlytics, etc.)
    console.error("[NotificationLimiter] Exception recorded:", exception);
  }
}

// Static method to match Android usage pattern
export const limitNotifications = async (): Promise<void> => {
  await NotificationLimiter.getInstance().limitNotifications();
};
