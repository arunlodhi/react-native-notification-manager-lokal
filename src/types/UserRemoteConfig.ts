/**
 * User Remote Configuration Types
 * Simple, well-defined object that users can provide directly
 */

export interface UserRemoteConfig {
  // Notification behavior configs
  notificationKeepAtTop?: boolean;
  notificationLimit?: number;
  notificationUnlockAtTopTimeoutMs?: number;
  notificationUnlockAtTopLimit?: number;
  isNotificationGroupingActive?: boolean;
  notificationVersion?: number;
  isNotificationIngestionEnabled?: boolean;
  isNotificationWorkerRunning?: boolean;

  // Cricket notification configs
  isCricketNotificationActive?: boolean;
  cricketNotificationInterval?: number;

  // Comment notification configs
  isCommentNotificationActive?: boolean;
  commentNotificationGrouping?: boolean;

  // Sticky notification configs
  isStickyNotificationActive?: boolean;
  stickyNotificationTimeInterval?: number;
  notificationMaxCancelCount?: number;

  // UI version configs
  notificationUIVersion?: string;
  isUnifiedFeedActive?: boolean;
}

/**
 * Default configuration values
 */
export const DEFAULT_USER_REMOTE_CONFIG: Required<UserRemoteConfig> = {
  // Notification behavior
  notificationKeepAtTop: false,
  notificationLimit: 10,
  notificationUnlockAtTopTimeoutMs: 300000, // 5 minutes
  notificationUnlockAtTopLimit: 3,
  isNotificationGroupingActive: true,
  notificationVersion: 1,
  isNotificationIngestionEnabled: false,
  isNotificationWorkerRunning: false,

  // Cricket notifications
  isCricketNotificationActive: false,
  cricketNotificationInterval: 30000, // 30 seconds

  // Comment notifications
  isCommentNotificationActive: true,
  commentNotificationGrouping: true,

  // Sticky notifications
  isStickyNotificationActive: false,
  stickyNotificationTimeInterval: 3600000, // 1 hour
  notificationMaxCancelCount: 5,

  // UI versions
  notificationUIVersion: "1",
  isUnifiedFeedActive: false,
};

/**
 * Validation function to ensure config object is valid
 */
export function validateUserRemoteConfig(config: UserRemoteConfig): {
  isValid: boolean;
  errors: string[];
} {
  const errors: string[] = [];

  // Validate notification version
  if (config.notificationVersion !== undefined) {
    if (config.notificationVersion < 1 || config.notificationVersion > 7) {
      errors.push("notificationVersion must be between 1 and 7");
    }
  }

  // Validate notification limit
  if (config.notificationLimit !== undefined) {
    if (config.notificationLimit < 0 || config.notificationLimit > 100) {
      errors.push("notificationLimit must be between 0 and 100");
    }
  }

  // Validate timeout
  if (config.notificationUnlockAtTopTimeoutMs !== undefined) {
    if (config.notificationUnlockAtTopTimeoutMs < 0) {
      errors.push("notificationUnlockAtTopTimeoutMs must be positive");
    }
  }

  // Validate cricket interval
  if (config.cricketNotificationInterval !== undefined) {
    if (config.cricketNotificationInterval < 1000) {
      errors.push("cricketNotificationInterval must be at least 1000ms");
    }
  }

  // Validate sticky interval
  if (config.stickyNotificationTimeInterval !== undefined) {
    if (config.stickyNotificationTimeInterval < 60000) {
      errors.push(
        "stickyNotificationTimeInterval must be at least 60000ms (1 minute)"
      );
    }
  }

  return {
    isValid: errors.length === 0,
    errors,
  };
}

/**
 * Helper function to merge user config with defaults
 */
export function mergeWithDefaults(
  userConfig: UserRemoteConfig
): Required<UserRemoteConfig> {
  return {
    ...DEFAULT_USER_REMOTE_CONFIG,
    ...userConfig,
  };
}

/**
 * Convert user config to internal remote config format
 */
export function convertToInternalConfig(userConfig: UserRemoteConfig): {
  [key: string]: any;
} {
  const merged = mergeWithDefaults(userConfig);

  return {
    // Convert camelCase to snake_case for internal use
    notification_keep_at_top: merged.notificationKeepAtTop,
    notification_limit: merged.notificationLimit,
    notification_unlock_at_top_timeout_ms:
      merged.notificationUnlockAtTopTimeoutMs,
    notification_unlock_at_top_limit: merged.notificationUnlockAtTopLimit,
    is_notification_grouping_active: merged.isNotificationGroupingActive,
    notification_version: merged.notificationVersion,
    is_notification_ingestion_enabled: merged.isNotificationIngestionEnabled,
    is_notification_worker_running: merged.isNotificationWorkerRunning,
    is_cricket_notification_active: merged.isCricketNotificationActive,
    cricket_notification_interval: merged.cricketNotificationInterval,
    is_comment_notification_active: merged.isCommentNotificationActive,
    comment_notification_grouping: merged.commentNotificationGrouping,
    is_sticky_notification_active: merged.isStickyNotificationActive,
    sticky_notification_time_interval: merged.stickyNotificationTimeInterval,
    notification_max_cancel_count: merged.notificationMaxCancelCount,
    notification_ui_version: merged.notificationUIVersion,
    is_unified_feed_active: merged.isUnifiedFeedActive,
  };
}
