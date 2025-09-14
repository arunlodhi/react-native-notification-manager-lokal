import {
  AnalyticsProvider,
  CrashReportingProvider,
  NotificationAnalyticsBundle,
  ANALYTICS_CONSTANTS,
  AnalyticsEventName,
  AnalyticsCategory,
  AnalyticsSource,
} from "../types/AnalyticsTypes";

/**
 * AnalyticsManager - Manages analytics tracking and crash reporting
 * Provides a centralized way to track notification events and crashes
 */
export class AnalyticsManager {
  private static instance: AnalyticsManager;
  private analyticsProvider?: AnalyticsProvider;
  private crashReportingProvider?: CrashReportingProvider;
  private isInitialized = false;

  private constructor() {}

  public static getInstance(): AnalyticsManager {
    if (!AnalyticsManager.instance) {
      AnalyticsManager.instance = new AnalyticsManager();
    }
    return AnalyticsManager.instance;
  }

  /**
   * Initialize with analytics and crash reporting providers
   */
  public initialize(
    analyticsProvider?: AnalyticsProvider,
    crashReportingProvider?: CrashReportingProvider
  ): void {
    this.analyticsProvider = analyticsProvider;
    this.crashReportingProvider = crashReportingProvider;
    this.isInitialized = true;
  }

  /**
   * Track notification built event
   */
  public trackNotificationBuilt(
    category: AnalyticsCategory,
    properties: NotificationAnalyticsBundle
  ): void {
    this.trackCategoryEvent(
      ANALYTICS_CONSTANTS.NOTIFICATION_BUILT,
      category,
      properties
    );
  }

  /**
   * Track notification clicked event
   */
  public trackNotificationClicked(
    source: AnalyticsSource,
    properties: NotificationAnalyticsBundle
  ): void {
    this.trackConversionEvent(
      ANALYTICS_CONSTANTS.NOTIFICATION_CLICKED,
      source,
      properties
    );
  }

  /**
   * Track notification dismissed event
   */
  public trackNotificationDismissed(
    notificationId: string,
    properties?: { [key: string]: any }
  ): void {
    this.trackEvent(ANALYTICS_CONSTANTS.NOTIFICATION_DISMISSED, {
      notification_id: notificationId,
      ...properties,
    });
  }

  /**
   * Track app open event
   */
  public trackAppOpen(
    source: AnalyticsSource,
    properties?: NotificationAnalyticsBundle
  ): void {
    this.trackEvent(ANALYTICS_CONSTANTS.APP_OPEN, {
      source,
      ...properties,
    });
  }

  /**
   * Track category event (matches Android AnalyticsEventsTracker)
   */
  public trackCategoryEvent(
    eventName: AnalyticsEventName,
    category: AnalyticsCategory,
    properties?: { [key: string]: any }
  ): void {
    if (!this.analyticsProvider) {
      console.warn("[AnalyticsManager] No analytics provider configured");
      return;
    }

    try {
      this.analyticsProvider.trackCategoryEvent(eventName, category, {
        category,
        timestamp: Date.now(),
        ...properties,
      });
    } catch (error) {
      console.error(
        "[AnalyticsManager] Failed to track category event:",
        error
      );
      this.recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Track conversion event
   */
  public trackConversionEvent(
    eventName: string,
    source: AnalyticsSource,
    properties?: { [key: string]: any }
  ): void {
    if (!this.analyticsProvider) {
      console.warn("[AnalyticsManager] No analytics provider configured");
      return;
    }

    try {
      this.analyticsProvider.trackConversionEvent(eventName, source, {
        source,
        timestamp: Date.now(),
        ...properties,
      });
    } catch (error) {
      console.error(
        "[AnalyticsManager] Failed to track conversion event:",
        error
      );
      this.recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Track generic event
   */
  public trackEvent(
    eventName: string,
    properties?: { [key: string]: any }
  ): void {
    if (!this.analyticsProvider) {
      console.warn("[AnalyticsManager] No analytics provider configured");
      return;
    }

    try {
      this.analyticsProvider.trackEvent(eventName, {
        timestamp: Date.now(),
        ...properties,
      });
    } catch (error) {
      console.error("[AnalyticsManager] Failed to track event:", error);
      this.recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Set user properties
   */
  public setUserProperties(properties: { [key: string]: any }): void {
    if (!this.analyticsProvider) {
      console.warn("[AnalyticsManager] No analytics provider configured");
      return;
    }

    try {
      this.analyticsProvider.setUserProperties(properties);
    } catch (error) {
      console.error("[AnalyticsManager] Failed to set user properties:", error);
      this.recordException(
        error instanceof Error ? error : new Error(String(error))
      );
    }
  }

  /**
   * Set user ID
   */
  public setUserId(userId: string): void {
    if (this.analyticsProvider) {
      try {
        this.analyticsProvider.setUserId(userId);
      } catch (error) {
        console.error(
          "[AnalyticsManager] Failed to set analytics user ID:",
          error
        );
      }
    }

    if (this.crashReportingProvider) {
      try {
        this.crashReportingProvider.setUserId(userId);
      } catch (error) {
        console.error(
          "[AnalyticsManager] Failed to set crash reporting user ID:",
          error
        );
      }
    }
  }

  /**
   * Record an exception
   */
  public recordException(error: Error): void {
    if (!this.crashReportingProvider) {
      console.warn("[AnalyticsManager] No crash reporting provider configured");
      console.error("[AnalyticsManager] Exception:", error);
      return;
    }

    try {
      this.crashReportingProvider.recordException(error);
    } catch (reportingError) {
      console.error(
        "[AnalyticsManager] Failed to record exception:",
        reportingError
      );
      console.error("[AnalyticsManager] Original exception:", error);
    }
  }

  /**
   * Log a message
   */
  public log(message: string): void {
    if (!this.crashReportingProvider) {
      console.log("[AnalyticsManager]", message);
      return;
    }

    try {
      this.crashReportingProvider.log(message);
    } catch (error) {
      console.error("[AnalyticsManager] Failed to log message:", error);
      console.log("[AnalyticsManager]", message);
    }
  }

  /**
   * Set custom key-value pair for crash reporting
   */
  public setCustomKey(key: string, value: string | number | boolean): void {
    if (!this.crashReportingProvider) {
      console.warn("[AnalyticsManager] No crash reporting provider configured");
      return;
    }

    try {
      this.crashReportingProvider.setCustomKey(key, value);
    } catch (error) {
      console.error("[AnalyticsManager] Failed to set custom key:", error);
    }
  }

  /**
   * Create notification built bundle (matches Android implementation)
   */
  public createNotificationBuiltBundle(
    uri: string | null,
    notificationId: string,
    postId: string,
    categoryId: string,
    channel: string,
    importance: number,
    notifType: string,
    isNotificationEnabled: boolean
  ): NotificationAnalyticsBundle {
    const status = isNotificationEnabled
      ? ANALYTICS_CONSTANTS.BUILT
      : ANALYTICS_CONSTANTS.BLOCKED;

    return {
      notification_id: notificationId,
      category_id: categoryId,
      status: status,
      post_id: postId,
      channel: channel,
      importance: importance,
      notification_type: notifType,
      uri: uri || undefined,
    };
  }

  /**
   * Check if analytics is initialized
   */
  public isAnalyticsInitialized(): boolean {
    return this.isInitialized && !!this.analyticsProvider;
  }

  /**
   * Check if crash reporting is initialized
   */
  public isCrashReportingInitialized(): boolean {
    return this.isInitialized && !!this.crashReportingProvider;
  }
}

// Convenience functions for easy access
export const initializeAnalytics = (
  analyticsProvider?: AnalyticsProvider,
  crashReportingProvider?: CrashReportingProvider
): void => {
  AnalyticsManager.getInstance().initialize(
    analyticsProvider,
    crashReportingProvider
  );
};

export const trackNotificationBuilt = (
  category: AnalyticsCategory,
  properties: NotificationAnalyticsBundle
): void => {
  AnalyticsManager.getInstance().trackNotificationBuilt(category, properties);
};

export const trackNotificationClicked = (
  source: AnalyticsSource,
  properties: NotificationAnalyticsBundle
): void => {
  AnalyticsManager.getInstance().trackNotificationClicked(source, properties);
};

export const trackNotificationDismissed = (
  notificationId: string,
  properties?: { [key: string]: any }
): void => {
  AnalyticsManager.getInstance().trackNotificationDismissed(
    notificationId,
    properties
  );
};

export const recordException = (error: Error): void => {
  AnalyticsManager.getInstance().recordException(error);
};

export const logMessage = (message: string): void => {
  AnalyticsManager.getInstance().log(message);
};

export const setAnalyticsUserId = (userId: string): void => {
  AnalyticsManager.getInstance().setUserId(userId);
};
