import { NativeModules, Platform } from "react-native";
import { Constants } from "../types/Constants";
import { NotificationRefresher } from "../utils/NotificationRefresher";

const { NotificationRefreshAlarmManagerModule } = NativeModules;

/**
 * NotificationRefreshAlarmManager - Exact port of Android NotificationRefreshAlarmManager.kt
 * Handles periodic notification refresh scheduling
 */
export class NotificationRefreshAlarmManager {
  private static instance: NotificationRefreshAlarmManager;
  private refreshInterval: NodeJS.Timeout | null = null;

  // Constants matching Android
  public static readonly NOTIFICATION_REFRESH_ID_EXTRA =
    Constants.NOTIFICATION_REFRESH_ID_EXTRA;
  public static readonly NOTIFICATION_TIME_EXTRA =
    Constants.NOTIFICATION_TIME_EXTRA;
  public static readonly INTERVAL_MINUTES = Constants.INTERVAL_MINUTES;

  private constructor() {}

  public static getInstance(): NotificationRefreshAlarmManager {
    if (!NotificationRefreshAlarmManager.instance) {
      NotificationRefreshAlarmManager.instance =
        new NotificationRefreshAlarmManager();
    }
    return NotificationRefreshAlarmManager.instance;
  }

  /**
   * Schedules periodic notification refresh - matches Android schedule() method
   */
  public async schedule(): Promise<void> {
    console.log(
      "[NotificationRefreshAlarmManager] Scheduling notification refresh"
    );

    // Clear existing interval if any
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }

    // Schedule periodic refresh every 15 minutes (matching Android logic)
    const intervalMs =
      NotificationRefreshAlarmManager.INTERVAL_MINUTES * 60 * 1000;

    this.refreshInterval = setInterval(async () => {
      await this.onAlarm();
    }, intervalMs);

    // Also run immediately
    await this.onAlarm();
  }

  /**
   * Cancels the scheduled refresh
   */
  public cancel(): void {
    console.log(
      "[NotificationRefreshAlarmManager] Cancelling notification refresh"
    );

    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  /**
   * Alarm callback - matches Android onAlarm() method exactly
   */
  private async onAlarm(): Promise<void> {
    try {
      const keepAtTop = await this.getRemoteConfigBoolean(
        "notification_keep_at_top",
        false
      );

      if (
        keepAtTop &&
        typeof Platform.Version === "number" &&
        Platform.Version >= Constants.VERSION_CODES_M
      ) {
        const activeNotifications = await this.getActiveNotifications();

        const currentAppNotifications: any[] = [];
        const packageName = await this.getPackageName();

        for (const notification of activeNotifications) {
          if (notification.packageName === packageName) {
            currentAppNotifications.push(notification);
          }
        }

        // Sort notifications by timestamp in descending order (newest first)
        // This matches the exact logic from Android: sortedByDescending
        const sortedNotifications = currentAppNotifications.sort((a, b) => {
          const timeA =
            a.notification.extras[
              NotificationRefreshAlarmManager.NOTIFICATION_TIME_EXTRA
            ] || 0;
          const timeB =
            b.notification.extras[
              NotificationRefreshAlarmManager.NOTIFICATION_TIME_EXTRA
            ] || 0;
          return timeB - timeA; // Descending order
        });

        // Refresh notifications using the NotificationRefresher
        await NotificationRefresher.getInstance().refreshNotifications();
      }
    } catch (exception) {
      const error =
        exception instanceof Error ? exception : new Error(String(exception));
      console.error(
        "[NotificationRefreshAlarmManager] Notification refresh worker exception:",
        error.message
      );
      await this.recordException(error);
    }
  }

  /**
   * Gets active notifications from the system
   */
  private async getActiveNotifications(): Promise<any[]> {
    if (Platform.OS === "android") {
      return await NotificationRefreshAlarmManagerModule.getActiveNotifications();
    }
    return [];
  }

  /**
   * Gets the current app's package name
   */
  private async getPackageName(): Promise<string> {
    if (Platform.OS === "android") {
      return await NotificationRefreshAlarmManagerModule.getPackageName();
    }
    return "";
  }

  /**
   * Gets remote config boolean value
   */
  private async getRemoteConfigBoolean(
    key: string,
    defaultValue: boolean
  ): Promise<boolean> {
    // TODO: Implement your remote config logic here
    return defaultValue;
  }

  /**
   * Records exception to crash reporting system
   */
  private async recordException(exception: Error): Promise<void> {
    // TODO: Implement your crash reporting logic here
    console.error(
      "[NotificationRefreshAlarmManager] Exception recorded:",
      exception
    );
  }
}

// Static method to match Android usage pattern
export const scheduleNotificationRefresh = async (): Promise<void> => {
  await NotificationRefreshAlarmManager.getInstance().schedule();
};

export const cancelNotificationRefresh = (): void => {
  NotificationRefreshAlarmManager.getInstance().cancel();
};
