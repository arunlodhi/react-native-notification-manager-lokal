import { NativeModules, Platform } from "react-native";
import { Constants } from "../types/Constants";
import { LocalNotificationCallbacks } from "../interfaces/NotificationCallbacks";

const { LocalNotificationManagerModule } = NativeModules;

/**
 * LocalNotificationManager - Handles local notification scheduling
 * Provides React Native interface for local notifications
 */
export class LocalNotificationManager {
  private static instance: LocalNotificationManager;
  private callbacks?: LocalNotificationCallbacks;

  private constructor() {}

  public static getInstance(): LocalNotificationManager {
    if (!LocalNotificationManager.instance) {
      LocalNotificationManager.instance = new LocalNotificationManager();
    }
    return LocalNotificationManager.instance;
  }

  public setCallbacks(callbacks: LocalNotificationCallbacks): void {
    this.callbacks = callbacks;
  }

  /**
   * Schedules a local notification
   */
  public async scheduleNotification(
    id: number,
    title: string,
    body: string,
    scheduledTime: number,
    data?: { [key: string]: any }
  ): Promise<void> {
    try {
      if (Platform.OS === "android") {
        await LocalNotificationManagerModule.scheduleNotification({
          id,
          title,
          body,
          scheduledTime,
          data: data || {},
        });
      }

      this.callbacks?.onLocalNotificationScheduled(id, scheduledTime);
      console.log(
        `[LocalNotificationManager] Scheduled notification ${id} for ${new Date(
          scheduledTime
        )}`
      );
    } catch (error) {
      console.error(
        `[LocalNotificationManager] Failed to schedule notification ${id}:`,
        error
      );
      throw error;
    }
  }

  /**
   * Cancels a scheduled local notification
   */
  public async cancelNotification(id: number): Promise<void> {
    try {
      if (Platform.OS === "android") {
        await LocalNotificationManagerModule.cancelNotification(id);
      }

      this.callbacks?.onLocalNotificationCancelled(id);
      console.log(`[LocalNotificationManager] Cancelled notification ${id}`);
    } catch (error) {
      console.error(
        `[LocalNotificationManager] Failed to cancel notification ${id}:`,
        error
      );
      throw error;
    }
  }

  /**
   * Cancels all scheduled local notifications
   */
  public async cancelAllNotifications(): Promise<void> {
    try {
      if (Platform.OS === "android") {
        await LocalNotificationManagerModule.cancelAllNotifications();
      }

      console.log("[LocalNotificationManager] Cancelled all notifications");
    } catch (error) {
      console.error(
        "[LocalNotificationManager] Failed to cancel all notifications:",
        error
      );
      throw error;
    }
  }

  /**
   * Gets all scheduled local notifications
   */
  public async getScheduledNotifications(): Promise<any[]> {
    try {
      if (Platform.OS === "android") {
        return await LocalNotificationManagerModule.getScheduledNotifications();
      }
      return [];
    } catch (error) {
      console.error(
        "[LocalNotificationManager] Failed to get scheduled notifications:",
        error
      );
      return [];
    }
  }
}

// Static methods for convenience
export const scheduleLocalNotification = async (
  id: number,
  title: string,
  body: string,
  scheduledTime: number,
  data?: { [key: string]: any }
): Promise<void> => {
  await LocalNotificationManager.getInstance().scheduleNotification(
    id,
    title,
    body,
    scheduledTime,
    data
  );
};

export const cancelLocalNotification = async (id: number): Promise<void> => {
  await LocalNotificationManager.getInstance().cancelNotification(id);
};

export const cancelAllLocalNotifications = async (): Promise<void> => {
  await LocalNotificationManager.getInstance().cancelAllNotifications();
};

export const getScheduledLocalNotifications = async (): Promise<any[]> => {
  return await LocalNotificationManager.getInstance().getScheduledNotifications();
};
