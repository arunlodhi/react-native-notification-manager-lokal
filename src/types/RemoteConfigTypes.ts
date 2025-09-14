/**
 * Remote Configuration Types
 * Defines all remote config keys and their expected types
 */

export interface RemoteConfigValues {
  // Notification behavior configs
  [RemoteConfigConstants.NOTIFICATION_KEEP_AT_TOP]: boolean;
  [RemoteConfigConstants.NOTIFICATION_LIMIT]: number;
  [RemoteConfigConstants.NOTIFICATION_UNLOCK_AT_TOP_TIMEOUT_MS]: number;
  [RemoteConfigConstants.NOTIFICATION_UNLOCK_AT_TOP_LIMIT]: number;
  [RemoteConfigConstants.IS_NOTIFICATION_GROUPING_ACTIVE]: boolean;
  [RemoteConfigConstants.NOTIFICATION_VERSION]: number;
  [RemoteConfigConstants.IS_NOTIFICATION_INGESTION_ENABLED]: boolean;
  [RemoteConfigConstants.IS_NOTIFICATION_WORKER_RUNNING]: boolean;

  // Cricket notification configs
  [RemoteConfigConstants.IS_CRICKET_NOTIFICATION_ACTIVE]: boolean;
  [RemoteConfigConstants.CRICKET_NOTIFICATION_INTERVAL]: number;

  // Comment notification configs
  [RemoteConfigConstants.IS_COMMENT_NOTIFICATION_ACTIVE]: boolean;
  [RemoteConfigConstants.COMMENT_NOTIFICATION_GROUPING]: boolean;

  // Sticky notification configs
  [RemoteConfigConstants.IS_STICKY_NOTIFICATION_ACTIVE]: boolean;
  [RemoteConfigConstants.STICKY_NOTIFICATION_TIME_INTERVAL]: number;
  [RemoteConfigConstants.NOTIFICATION_MAX_CANCEL_COUNT]: number;

  // UI version configs
  [RemoteConfigConstants.NOTIFICATION_UI_VERSION]: string;
  [RemoteConfigConstants.IS_UNIFIED_FEED_ACTIVE]: boolean;
}

export interface RemoteConfigProvider {
  /**
   * Get boolean value from remote config
   */
  getBoolean(
    key: keyof RemoteConfigValues,
    defaultValue: boolean
  ): Promise<boolean>;

  /**
   * Get number value from remote config
   */
  getNumber(
    key: keyof RemoteConfigValues,
    defaultValue: number
  ): Promise<number>;

  /**
   * Get string value from remote config
   */
  getString(
    key: keyof RemoteConfigValues,
    defaultValue: string
  ): Promise<string>;

  /**
   * Initialize remote config
   */
  initialize(): Promise<void>;

  /**
   * Fetch and activate remote config
   */
  fetchAndActivate(): Promise<boolean>;
}

export class RemoteConfigConstants {
  // Notification behavior
  static readonly NOTIFICATION_KEEP_AT_TOP = "notification_keep_at_top";
  static readonly NOTIFICATION_LIMIT = "notification_limit";
  static readonly NOTIFICATION_UNLOCK_AT_TOP_TIMEOUT_MS =
    "notification_unlock_at_top_timeout_ms";
  static readonly NOTIFICATION_UNLOCK_AT_TOP_LIMIT =
    "notification_unlock_at_top_limit";
  static readonly IS_NOTIFICATION_GROUPING_ACTIVE =
    "is_notification_grouping_active";
  static readonly NOTIFICATION_VERSION = "notification_version";
  static readonly IS_NOTIFICATION_INGESTION_ENABLED =
    "is_notification_ingestion_enabled";
  static readonly IS_NOTIFICATION_WORKER_RUNNING =
    "is_notification_worker_running";

  // Cricket notifications
  static readonly IS_CRICKET_NOTIFICATION_ACTIVE =
    "is_cricket_notification_active";
  static readonly CRICKET_NOTIFICATION_INTERVAL =
    "cricket_notification_interval";

  // Comment notifications
  static readonly IS_COMMENT_NOTIFICATION_ACTIVE =
    "is_comment_notification_active";
  static readonly COMMENT_NOTIFICATION_GROUPING =
    "comment_notification_grouping";

  // Sticky notifications
  static readonly IS_STICKY_NOTIFICATION_ACTIVE =
    "is_sticky_notification_active";
  static readonly STICKY_NOTIFICATION_TIME_INTERVAL =
    "sticky_notification_time_interval";
  static readonly NOTIFICATION_MAX_CANCEL_COUNT =
    "notification_max_cancel_count";

  // UI versions
  static readonly NOTIFICATION_UI_VERSION = "notification_ui_version";
  static readonly IS_UNIFIED_FEED_ACTIVE = "is_unified_feed_active";
}

/**
 * Default remote config values
 */
export const DEFAULT_REMOTE_CONFIG: Partial<RemoteConfigValues> = {
  [RemoteConfigConstants.NOTIFICATION_KEEP_AT_TOP]: false,
  [RemoteConfigConstants.NOTIFICATION_LIMIT]: 10,
  [RemoteConfigConstants.NOTIFICATION_UNLOCK_AT_TOP_TIMEOUT_MS]: 300000, // 5 minutes
  [RemoteConfigConstants.NOTIFICATION_UNLOCK_AT_TOP_LIMIT]: 3,
  [RemoteConfigConstants.IS_NOTIFICATION_GROUPING_ACTIVE]: true,
  [RemoteConfigConstants.NOTIFICATION_VERSION]: 1,
  [RemoteConfigConstants.IS_NOTIFICATION_INGESTION_ENABLED]: false,
  [RemoteConfigConstants.IS_NOTIFICATION_WORKER_RUNNING]: false,
  [RemoteConfigConstants.IS_CRICKET_NOTIFICATION_ACTIVE]: false,
  [RemoteConfigConstants.CRICKET_NOTIFICATION_INTERVAL]: 30000, // 30 seconds
  [RemoteConfigConstants.IS_COMMENT_NOTIFICATION_ACTIVE]: true,
  [RemoteConfigConstants.COMMENT_NOTIFICATION_GROUPING]: true,
  [RemoteConfigConstants.IS_STICKY_NOTIFICATION_ACTIVE]: false,
  [RemoteConfigConstants.STICKY_NOTIFICATION_TIME_INTERVAL]: 3600000, // 1 hour
  [RemoteConfigConstants.NOTIFICATION_MAX_CANCEL_COUNT]: 5,
  [RemoteConfigConstants.NOTIFICATION_UI_VERSION]: "1",
  [RemoteConfigConstants.IS_UNIFIED_FEED_ACTIVE]: false,
};
