// Main entry point for React Native Notification Port
export { NotificationManager } from "./managers/NotificationManager";

// Types
export * from "./types/NotificationTypes";
export * from "./types/Constants";

// Interfaces
export * from "./interfaces/NotificationCallbacks";

// Convenience functions
export {
  initializeNotifications,
  createNotification,
  createNotificationWithImage,
  cancelNotification,
  refreshNotifications,
  limitNotifications,
} from "./managers/NotificationManager";

// Local notifications
export {
  scheduleLocalNotification,
  cancelLocalNotification,
  getScheduledLocalNotifications,
} from "./managers/LocalNotificationManager";

// Periodic refresh
export {
  scheduleNotificationRefresh,
  cancelNotificationRefresh,
} from "./managers/NotificationRefreshAlarmManager";

// Cricket notifications
export { createCricketNotification } from "./managers/NotificationManager";

// Quiz notifications
export { createQuizNotification } from "./managers/NotificationManager";

// Comment notifications
export { createCommentNotification } from "./managers/NotificationManager";

// Matrimony notifications
export { getMatrimonyNotificationType } from "./managers/NotificationManager";

// Advanced notification features
export {
  setSilentPush,
  setNotificationVersion,
} from "./managers/NotificationManager";
