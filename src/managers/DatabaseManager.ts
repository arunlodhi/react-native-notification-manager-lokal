import AsyncStorage from "@react-native-async-storage/async-storage";
import { NotificationData } from "../types/NotificationTypes";
import { recordException } from "./AnalyticsManager";

/**
 * DatabaseManager - Manages notification persistence
 * Provides database-like functionality using AsyncStorage for notification data
 */
export class DatabaseManager {
  private static instance: DatabaseManager;
  private static readonly NOTIFICATIONS_KEY = "@notification_data";
  private static readonly NOTIFICATION_IDS_KEY = "@notification_ids";
  private static readonly GROUPED_NOTIFICATION_IDS_KEY =
    "@grouped_notification_ids";

  private constructor() {}

  public static getInstance(): DatabaseManager {
    if (!DatabaseManager.instance) {
      DatabaseManager.instance = new DatabaseManager();
    }
    return DatabaseManager.instance;
  }

  /**
   * Store notification data
   */
  public async storeNotificationData(
    notificationData: NotificationData
  ): Promise<void> {
    try {
      const existingData = await this.getAllNotificationData();
      const updatedData = existingData.filter(
        (data) => data.notificationId !== notificationData.notificationId
      );
      updatedData.push(notificationData);

      await AsyncStorage.setItem(
        DatabaseManager.NOTIFICATIONS_KEY,
        JSON.stringify(updatedData)
      );
    } catch (error) {
      console.error(
        "[DatabaseManager] Failed to store notification data:",
        error
      );
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Get all notification data
   */
  public async getAllNotificationData(): Promise<NotificationData[]> {
    try {
      const data = await AsyncStorage.getItem(
        DatabaseManager.NOTIFICATIONS_KEY
      );
      return data ? JSON.parse(data) : [];
    } catch (error) {
      console.error(
        "[DatabaseManager] Failed to get notification data:",
        error
      );
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
      return [];
    }
  }

  /**
   * Get today's notifications (last 24 hours)
   */
  public async getTodayNotifications(): Promise<NotificationData[]> {
    try {
      const allData = await this.getAllNotificationData();
      const oneDayAgo = Date.now() - 24 * 60 * 60 * 1000;

      return allData.filter((data) => {
        const timestamp = data.timestamp || 0;
        return timestamp >= oneDayAgo;
      });
    } catch (error) {
      console.error(
        "[DatabaseManager] Failed to get today's notifications:",
        error
      );
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
      return [];
    }
  }

  /**
   * Get notification data by ID
   */
  public async getNotificationById(
    notificationId: number
  ): Promise<NotificationData | null> {
    try {
      const allData = await this.getAllNotificationData();
      return (
        allData.find((data) => data.notificationId === notificationId) || null
      );
    } catch (error) {
      console.error(
        "[DatabaseManager] Failed to get notification by ID:",
        error
      );
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
      return null;
    }
  }

  /**
   * Delete notification data by ID
   */
  public async deleteNotificationById(notificationId: number): Promise<void> {
    try {
      const existingData = await this.getAllNotificationData();
      const filteredData = existingData.filter(
        (data) => data.notificationId !== notificationId
      );

      await AsyncStorage.setItem(
        DatabaseManager.NOTIFICATIONS_KEY,
        JSON.stringify(filteredData)
      );
    } catch (error) {
      console.error("[DatabaseManager] Failed to delete notification:", error);
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Clean old notifications (older than 7 days)
   */
  public async cleanOldNotifications(): Promise<void> {
    try {
      const allData = await this.getAllNotificationData();
      const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;

      const recentData = allData.filter((data) => {
        const timestamp = data.timestamp || 0;
        return timestamp >= sevenDaysAgo;
      });

      await AsyncStorage.setItem(
        DatabaseManager.NOTIFICATIONS_KEY,
        JSON.stringify(recentData)
      );

      console.log(
        `[DatabaseManager] Cleaned ${
          allData.length - recentData.length
        } old notifications`
      );
    } catch (error) {
      console.error(
        "[DatabaseManager] Failed to clean old notifications:",
        error
      );
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Store notification ID for tracking (matches Android TinyDB functionality)
   */
  public async storeNotificationId(notificationId: number): Promise<void> {
    try {
      const existingIds = await this.getStoredNotificationIds();
      if (!existingIds.includes(notificationId)) {
        // Keep only last 20 IDs (matches Android logic)
        if (existingIds.length >= 20) {
          existingIds.shift();
        }
        existingIds.push(notificationId);
        await AsyncStorage.setItem(
          DatabaseManager.NOTIFICATION_IDS_KEY,
          JSON.stringify(existingIds)
        );
      }
    } catch (error) {
      console.error(
        "[DatabaseManager] Failed to store notification ID:",
        error
      );
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Get stored notification IDs
   */
  public async getStoredNotificationIds(): Promise<number[]> {
    try {
      const data = await AsyncStorage.getItem(
        DatabaseManager.NOTIFICATION_IDS_KEY
      );
      return data ? JSON.parse(data) : [];
    } catch (error) {
      console.error("[DatabaseManager] Failed to get notification IDs:", error);
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
      return [];
    }
  }

  /**
   * Check if notification ID exists
   */
  public async hasNotificationId(notificationId: number): Promise<boolean> {
    try {
      const existingIds = await this.getStoredNotificationIds();
      return existingIds.includes(notificationId);
    } catch (error) {
      console.error(
        "[DatabaseManager] Failed to check notification ID:",
        error
      );
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
      return false;
    }
  }

  /**
   * Store grouped notification IDs (matches Android SharedPreferences logic)
   */
  public async storeGroupedNotificationIds(
    groupId: number,
    notificationIds: string
  ): Promise<void> {
    try {
      const key = `${DatabaseManager.GROUPED_NOTIFICATION_IDS_KEY}_${groupId}`;
      await AsyncStorage.setItem(key, notificationIds);
    } catch (error) {
      console.error(
        "[DatabaseManager] Failed to store grouped notification IDs:",
        error
      );
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Get grouped notification IDs
   */
  public async getGroupedNotificationIds(groupId: number): Promise<string> {
    try {
      const key = `${DatabaseManager.GROUPED_NOTIFICATION_IDS_KEY}_${groupId}`;
      const data = await AsyncStorage.getItem(key);
      return data || "";
    } catch (error) {
      console.error(
        "[DatabaseManager] Failed to get grouped notification IDs:",
        error
      );
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
      return "";
    }
  }

  /**
   * Update notification read status (for notification hub functionality)
   */
  public async updateNotificationReadStatus(
    groupId: string,
    isRead: boolean
  ): Promise<void> {
    try {
      const key = `@notification_read_status_${groupId}`;
      await AsyncStorage.setItem(
        key,
        JSON.stringify({ isRead, timestamp: Date.now() })
      );
    } catch (error) {
      console.error("[DatabaseManager] Failed to update read status:", error);
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Get notification read status
   */
  public async getNotificationReadStatus(groupId: string): Promise<boolean> {
    try {
      const key = `@notification_read_status_${groupId}`;
      const data = await AsyncStorage.getItem(key);
      if (data) {
        const parsed = JSON.parse(data);
        return parsed.isRead || false;
      }
      return false;
    } catch (error) {
      console.error("[DatabaseManager] Failed to get read status:", error);
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
      return false;
    }
  }

  /**
   * Get notification badge count (unread notifications)
   */
  public async getNotificationBadgeCount(): Promise<number> {
    try {
      const allData = await this.getAllNotificationData();
      let unreadCount = 0;

      for (const notification of allData) {
        const isRead = await this.getNotificationReadStatus(
          notification.groupId || String(notification.notificationId)
        );
        if (!isRead) {
          unreadCount++;
        }
      }

      return unreadCount;
    } catch (error) {
      console.error("[DatabaseManager] Failed to get badge count:", error);
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
      return 0;
    }
  }

  /**
   * Clear all notification data
   */
  public async clearAllData(): Promise<void> {
    try {
      await AsyncStorage.multiRemove([
        DatabaseManager.NOTIFICATIONS_KEY,
        DatabaseManager.NOTIFICATION_IDS_KEY,
      ]);

      // Clear grouped notification IDs
      const keys = await AsyncStorage.getAllKeys();
      const groupedKeys = keys.filter((key: string) =>
        key.startsWith(DatabaseManager.GROUPED_NOTIFICATION_IDS_KEY)
      );
      if (groupedKeys.length > 0) {
        await AsyncStorage.multiRemove(groupedKeys);
      }

      console.log("[DatabaseManager] Cleared all notification data");
    } catch (error) {
      console.error("[DatabaseManager] Failed to clear all data:", error);
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Get database statistics
   */
  public async getDatabaseStats(): Promise<{
    totalNotifications: number;
    todayNotifications: number;
    unreadCount: number;
    oldestNotification?: Date;
    newestNotification?: Date;
  }> {
    try {
      const allData = await this.getAllNotificationData();
      const todayData = await this.getTodayNotifications();
      const unreadCount = await this.getNotificationBadgeCount();

      let oldestTimestamp = Number.MAX_SAFE_INTEGER;
      let newestTimestamp = 0;

      for (const notification of allData) {
        const timestamp = notification.timestamp || 0;
        if (timestamp > 0) {
          oldestTimestamp = Math.min(oldestTimestamp, timestamp);
          newestTimestamp = Math.max(newestTimestamp, timestamp);
        }
      }

      return {
        totalNotifications: allData.length,
        todayNotifications: todayData.length,
        unreadCount,
        oldestNotification:
          oldestTimestamp !== Number.MAX_SAFE_INTEGER
            ? new Date(oldestTimestamp)
            : undefined,
        newestNotification:
          newestTimestamp > 0 ? new Date(newestTimestamp) : undefined,
      };
    } catch (error) {
      console.error("[DatabaseManager] Failed to get database stats:", error);
      recordException(
        error instanceof Error ? error : new Error(String(error))
      );
      return {
        totalNotifications: 0,
        todayNotifications: 0,
        unreadCount: 0,
      };
    }
  }
}

// Convenience functions for easy access
export const storeNotificationData = async (
  notificationData: NotificationData
): Promise<void> => {
  await DatabaseManager.getInstance().storeNotificationData(notificationData);
};

export const getTodayNotifications = async (): Promise<NotificationData[]> => {
  return await DatabaseManager.getInstance().getTodayNotifications();
};

export const getNotificationById = async (
  notificationId: number
): Promise<NotificationData | null> => {
  return await DatabaseManager.getInstance().getNotificationById(
    notificationId
  );
};

export const cleanOldNotifications = async (): Promise<void> => {
  await DatabaseManager.getInstance().cleanOldNotifications();
};

export const getNotificationBadgeCount = async (): Promise<number> => {
  return await DatabaseManager.getInstance().getNotificationBadgeCount();
};
