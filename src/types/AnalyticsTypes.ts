/**
 * Analytics and Crash Reporting Types
 * Provides interfaces for analytics tracking and crash reporting
 */

export interface AnalyticsProvider {
  /**
   * Track an event with properties
   */
  trackEvent(eventName: string, properties?: { [key: string]: any }): void;

  /**
   * Track a category event (matches Android AnalyticsEventsTracker)
   */
  trackCategoryEvent(
    eventName: string,
    category: string,
    properties?: { [key: string]: any }
  ): void;

  /**
   * Track a conversion event
   */
  trackConversionEvent(
    source: string,
    action: string,
    properties?: { [key: string]: any }
  ): void;

  /**
   * Set user properties
   */
  setUserProperties(properties: { [key: string]: any }): void;

  /**
   * Set user ID
   */
  setUserId(userId: string): void;
}

export interface CrashReportingProvider {
  /**
   * Record an exception
   */
  recordException(error: Error): void;

  /**
   * Log a message
   */
  log(message: string): void;

  /**
   * Set custom key-value pairs
   */
  setCustomKey(key: string, value: string | number | boolean): void;

  /**
   * Set user identifier
   */
  setUserId(userId: string): void;
}

export interface NotificationAnalyticsBundle {
  notification_id?: string;
  post_id?: string;
  category_id?: string;
  status?: string;
  channel?: string;
  importance?: number;
  notification_type?: string;
  uri?: string;
  source?: string;
  is_personalized?: boolean;
  classified_id?: string;
  type_card?: string;
  notification_channel?: string;
  notification_importance?: number;
}

// Analytics constants matching Android implementation
export const ANALYTICS_CONSTANTS = {
  // Event names
  NOTIFICATION_BUILT: "notification_built",
  NOTIFICATION_CLICKED: "notification_clicked",
  NOTIFICATION_DISMISSED: "notification_dismissed",
  APP_OPEN: "app_open",

  // Categories
  NOTIFICATION_WITH_IMAGE: "notification_with_image",
  NOTIFICATION_WITHOUT_IMAGE: "notification_without_image",
  CRICKET_NOTIFICATION: "cricket_notification",
  COMMENT_NOTIFICATION: "comment_notification",
  QUIZ_NOTIFICATION: "quiz_notification",

  // Sources
  NOTIFICATION: "notification",
  LINK: "link",
  CLICKED: "clicked",

  // Status
  BUILT: "built",
  BLOCKED: "blocked",

  // Notification types
  STICKY_NOTIF: "sticky_notif",
  REGULAR_NOTIF: "regular_notif",

  // Properties
  NOTIFICATION_ID: "notification_id",
  POST_ID: "post_id",
  CATEGORY_ID: "category_id",
  CATEGORY_NAME: "category_name",
  USER_ID: "user_id",
  REPORTER_ID: "reporter_id",
  POST_COUNT: "post_count",
  COMMENTS_COUNT: "comments_count",
  NOTIFICATION_TYPE: "notification_type",
  NOTIFICATION_INTERVAL: "notification_interval",
  IS_PERSONALIZED_EXTRA: "is_personalized",
  CHANNEL: "channel",
  IMPORTANCE: "importance",
  TIME_STAMP: "time_stamp",
  TYPE: "type",
  EVENT_NAME: "event_name",
} as const;

export type AnalyticsEventName =
  | typeof ANALYTICS_CONSTANTS.NOTIFICATION_BUILT
  | typeof ANALYTICS_CONSTANTS.NOTIFICATION_CLICKED
  | typeof ANALYTICS_CONSTANTS.NOTIFICATION_DISMISSED
  | typeof ANALYTICS_CONSTANTS.APP_OPEN;

export type AnalyticsCategory =
  | typeof ANALYTICS_CONSTANTS.NOTIFICATION_WITH_IMAGE
  | typeof ANALYTICS_CONSTANTS.NOTIFICATION_WITHOUT_IMAGE
  | typeof ANALYTICS_CONSTANTS.CRICKET_NOTIFICATION
  | typeof ANALYTICS_CONSTANTS.COMMENT_NOTIFICATION
  | typeof ANALYTICS_CONSTANTS.QUIZ_NOTIFICATION;

export type AnalyticsSource =
  | typeof ANALYTICS_CONSTANTS.NOTIFICATION
  | typeof ANALYTICS_CONSTANTS.LINK
  | typeof ANALYTICS_CONSTANTS.CLICKED;
