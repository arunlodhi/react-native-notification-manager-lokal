/**
 * React Native Notification Manager - Lokal
 * Complete React Native port of Android notification functionality
 */

// Remote configuration
export {
  RemoteConfigManager,
  getRemoteConfigBoolean,
  getRemoteConfigNumber,
  getRemoteConfigString,
  initializeRemoteConfig,
  setRemoteConfigValues,
} from "./managers/RemoteConfigManager";

// Simple remote configuration (recommended for most users)
export {
  SimpleRemoteConfigManager,
  initializeSimpleRemoteConfig,
  setSimpleRemoteConfig,
  updateSimpleRemoteConfig,
  getSimpleRemoteConfig,
  isFeatureEnabled,
  getNotificationVersion,
  shouldKeepNotificationsAtTop,
} from "./managers/SimpleRemoteConfigManager";

// Analytics and crash reporting
export {
  AnalyticsManager,
  initializeAnalytics,
  trackNotificationBuilt,
  trackNotificationClicked,
  trackNotificationDismissed,
  recordException,
  logMessage,
  setAnalyticsUserId,
} from "./managers/AnalyticsManager";

// Database management
export {
  DatabaseManager,
  storeNotificationData,
  getTodayNotifications,
  getNotificationById,
  cleanOldNotifications,
  getNotificationBadgeCount,
} from "./managers/DatabaseManager";

// Notification utilities
export {
  NotificationLimiter,
  limitNotifications as limitNotificationsUtil,
} from "./utils/NotificationLimiter";

export {
  NotificationRefresher,
  refreshNotifications as refreshNotificationsUtil,
} from "./utils/NotificationRefresher";

// Types
export type {
  NotificationPayload,
  NotificationData,
  MoEngageNotificationData,
  ActiveNotification,
  NotificationBuilderConfig,
  NotificationAction,
  CricketMatch,
  NotificationVersion,
} from "./types/NotificationTypes";

export { NotificationType, MatchState } from "./types/NotificationTypes";

export type {
  RemoteConfigValues,
  RemoteConfigProvider,
} from "./types/RemoteConfigTypes";

export {
  RemoteConfigConstants,
  DEFAULT_REMOTE_CONFIG,
} from "./types/RemoteConfigTypes";

export type { UserRemoteConfig } from "./types/UserRemoteConfig";

export {
  DEFAULT_USER_REMOTE_CONFIG,
  validateUserRemoteConfig,
  mergeWithDefaults,
  convertToInternalConfig,
} from "./types/UserRemoteConfig";

export type {
  CricketTeam,
  CricketInnings,
  CricketTeamWithInnings,
  CricketMatch as CricketMatchType,
  CricketNotificationData,
  CricketNotificationCallbacks,
} from "./types/CricketTypes";

export { CricketMatchState } from "./types/CricketTypes";

export type {
  CommentNotificationData,
  CommentNotificationCallbacks,
  CommentNotificationConfig,
  CommentNotificationInterval,
} from "./types/CommentTypes";

export {
  CommentNotificationType,
  COMMENT_NOTIFICATION_CONSTANTS,
} from "./types/CommentTypes";

export type {
  AnalyticsProvider,
  CrashReportingProvider,
  NotificationAnalyticsBundle,
  AnalyticsEventName,
  AnalyticsCategory,
  AnalyticsSource,
} from "./types/AnalyticsTypes";

export { ANALYTICS_CONSTANTS } from "./types/AnalyticsTypes";

// Note: This package provides a comprehensive notification management system
// that matches the exact Android implementation. The core NotificationManager
// and other components will be implemented in future versions.
//
// Current implementation includes:
// - Remote configuration management
// - Analytics and crash reporting
// - Database persistence for notifications
// - Notification limiting and refresh utilities
// - Comprehensive TypeScript types
