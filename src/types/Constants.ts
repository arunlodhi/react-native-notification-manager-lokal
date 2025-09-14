// Constants ported from Android codebase
export const Constants = {
  // Notification IDs
  COMMENTS_SUMMARY_NOTIFICATION_ID: 9999,
  COMMENTS_NOTIFICATION_ID: 99999,
  COMMENTS_GROUP_NOTIFICATION_ID: "comments_group_notification",

  // Intent Actions
  ACTION_PUSH: "get.lokal.localnews.ACTION_PUSH",
  ACTION_PUSH_STICKY: "get.lokal.localnews.ACTION_PUSH_STICKY",
  ACTION_PUSH_CRICKET: "get.lokal.localnews.ACTION_PUSH_CRICKET",
  ACTION_PUSH_VIDEO: "get.lokal.localnews.ACTION_PUSH_VIDEO",
  ACTION_PUSH_POLL: "get.lokal.localnews.ACTION_PUSH_POLL",
  ACTION_PUSH_SHARE: "get.lokal.localnews.ACTION_PUSH_SHARE",
  ACTION_PUSH_POLL_STICKY: "get.lokal.localnews.ACTION_PUSH_POLL_STICKY",
  ACTION_PUSH_SHARE_STICKY: "get.lokal.localnews.ACTION_PUSH_SHARE_STICKY",
  ACTION_VIEW: "android.intent.action.VIEW",

  // Intent Extras
  CHANNEL_EXTRA: "channel_extra",
  IMPORTANCE_EXTRA: "importance_extra",
  NOTIFICATION_ID_EXTRA: "notification_id_extra",
  IS_SOURCE_NOTIFICATION: "is_source_notification",
  NOTIFICATION_CATEGORY_NAME_EXTRA: "notification_category_name_extra",
  NOTIFICATION_TYPE: "notification_type",
  CATEGORY_ID_EXTRA: "category_id_extra",
  URI_EXTRA: "uri_extra",
  GROUP_NOTIF_ID_EXTRA: "group_notif_id_extra",
  TAG_EXTRA: "tag_extra",
  ACTION_EXTRA: "action_extra",
  IS_SHARE_EXTRA: "is_share_extra",
  IS_NOTIFICATION_HUB_OPEN_EXTRA: "is_notification_hub_open_extra",
  IS_OPENED_FROM_AD: "is_opened_from_ad",
  IS_INTERNAL_URI: "is_internal_uri",

  // MoEngage Constants
  NOTIFICATION_FROM_MOE: "notification_from_moe",
  NOTIFICATION_APP_ID: "notification_app_id",
  NOTIFICATION_MOE_PUSH_FROM: "notification_moe_push_from",
  NOTIFICATION_MOE_PUSH: "notification_moe_push",
  NOTIFICATION_CAMPAIGN_ID: "notification_campaign_id",
  NOTIFICATION_MOE_ATTR: "notification_moe_attr",

  // SharedPreferences Keys
  PREV_NOTIFS_LIST: "prev_notifs_list",
  PREV_NOTIFS_SAMPLE_GROUPS_LIST: "prev_notifs_sample_groups_list",
  MATRIMONY_GROUPED_NOTIF_ID: "matrimony_grouped_notif_id_",
  COMMENT_NOTIFICATION_IDS: "comment_notification_ids",
  IS_NOTIFICATION_GROUPING_ACTIVE: "is_notification_grouping_active",
  IS_NOTIFICATION_WORKER_RUNNING: "is_notification_worker_running",

  // Remote Config Keys
  NOTIFICATION_KEEP_AT_TOP: "notification_keep_at_top",
  NOTIFICATION_LIMIT: "notification_limit",
  NOTIFICATION_UNLOCK_AT_TOP_LIMIT: "notification_unlock_at_top_limit",
  NOTIFICATION_UNLOCK_AT_TOP_TIMEOUT_MS:
    "notification_unlock_at_top_timeout_ms",
  NOTIFICATION_UI_VERSION: "notification_ui_version",
  IS_BULLETIN_NOTIFICATION_ACTIVE: "is_bulletin_notification_active",
  IS_STICKY_NOTIFICATION_ACTIVE: "is_sticky_notification_active",
  IS_STICKY_ONGOING: "is_sticky_ongoing",
  NOTIFICATION_MAX_CANCEL_COUNT: "notification_max_cancel_count",
  STICKY_NOTIFICATION_TIME_INTERVAL: "sticky_notification_time_interval",
  STICKY_NOTIFICATION_CONFIG: "sticky_notification_config",

  // Notification Refresh
  NOTIFICATION_REFRESH_ID_EXTRA: "notification_refresh_id_extra",
  NOTIFICATION_TIME_EXTRA: "notification_time_extra",
  NOTIFICATION_TIMESTAMP: "notification_timestamp",
  INTERVAL_MINUTES: 15,

  // Categories
  NEWS_CATEGORY: 1,
  JOBS_CATEGORY: 2,
  WISHES_CATEGORY: 3,
  ADVERTISEMENT_CATEGORY: 4,
  MATRIMONY_CATEGORY: 5,
  REAL_ESTATE_CATEGORY: 6,
  COMMUNITY_SUBMISSION_CATEGORY: 7,
  USER_VERIFICATION_DOCS_CATEGORY: 8,

  // Notification Types
  TYPE_VIDEO: "1",
  TYPE_QUIZ: 4,

  // Defaults
  UNSET: "unset",
  INTEGER_ZERO: 0,
  DEFAULT_REFRESH_TIME: 5 * 60 * 1000, // 5 minutes
  RESET_TIMEOUT: 30000, // 30 seconds
  NOTIFY_DELAY: 300, // 300ms

  // Languages
  TAMIL_LANGUAGE: "ta",
  MALAYALAM_LANGUAGE: "ml",
  BENGALI_LANGUAGE: "bn",
  HINDI_LANGUAGE: "hi",

  // Notification Versions
  NOTIFICATION_VERSION_1: 1,
  NOTIFICATION_VERSION_2: 2,
  NOTIFICATION_VERSION_3: 3,
  NOTIFICATION_VERSION_4: 4,
  NOTIFICATION_VERSION_5: 5,
  NOTIFICATION_VERSION_6: 6,

  // Priority Constants
  PRIORITY_MAX: 2,
  PRIORITY_HIGH: 1,
  PRIORITY_DEFAULT: 0,
  PRIORITY_LOW: -1,
  PRIORITY_MIN: -2,

  // Importance Constants
  IMPORTANCE_UNSPECIFIED: -1000,
  IMPORTANCE_NONE: 0,
  IMPORTANCE_MIN: 1,
  IMPORTANCE_LOW: 2,
  IMPORTANCE_DEFAULT: 3,
  IMPORTANCE_HIGH: 4,
  IMPORTANCE_MAX: 5,

  // Notification Categories
  CATEGORY_CALL: "call",
  CATEGORY_MESSAGE: "msg",
  CATEGORY_EMAIL: "email",
  CATEGORY_EVENT: "event",
  CATEGORY_PROMO: "promo",
  CATEGORY_ALARM: "alarm",
  CATEGORY_PROGRESS: "progress",
  CATEGORY_SOCIAL: "social",
  CATEGORY_ERROR: "err",
  CATEGORY_TRANSPORT: "transport",
  CATEGORY_SYSTEM: "sys",
  CATEGORY_SERVICE: "service",
  CATEGORY_RECOMMENDATION: "recommendation",
  CATEGORY_STATUS: "status",

  // Visibility Constants
  VISIBILITY_PUBLIC: 1,
  VISIBILITY_PRIVATE: 0,
  VISIBILITY_SECRET: -1,

  // Default Values
  DEFAULT_NOTIFICATION_LIMIT: 3,
  MAX_NOTIFICATION_LIST_SIZE: 20,
  PROGRESS_MAX: 100,
  MIN_PERMISSION_DIALOG_DISPLAY_DELAY: 1000,

  // Cricket Notification
  CRICKET_NOTIFICATION_ID: -99,
  CRICKET_PREVIEW_NOTIFICATION_ID: 99,

  // Special Notification IDs
  PROFILE_VERIFIED: "profile_verified",
  VERIFICATION_FAIL: "verification_fail",
  REQUEST_RECEIVED: "request_received",
  NEW_MATCH: "new_match",

  // Database Constants
  BODY_EXTRA: "body_extra",
  CATEGORY_NAME: "category_name",

  // Analytics Constants
  NOTIFICATION_BUILT: "notification_built",
  NOTIFICATION_WITHOUT_IMAGE: "notification_without_image",
  NOTIFICATION_WITH_IMAGE: "notification_with_image",
  STICKY_NOTIF: "sticky_notif",
  NOTIFICATION_PERMISSION_SUBMITTED: "notification_permission_submitted",
  NOTIFICATION_INTERVAL_INSTANT: "instant",

  // Channels
  DEFAULT_CHANNEL: "Recommendation",
  CRICKET_CHANNEL: "Cricket",
  COMMENTS_CHANNEL: "Comments",
  DOWNLOADS_CHANNEL: "Downloads",
  UPLOADS_CHANNEL: "Uploads",

  // Notification Layouts (React Native will use different approach)
  LAYOUT_NOTIFICATION_SMALL: "notification_small",
  LAYOUT_NOTIFICATION_LARGE: "notification_large",
  LAYOUT_NOTIFICATION_HEADSUP: "notification_headsup",
  LAYOUT_NOTIFICATION_SMALL_WITHOUT_IMAGE: "notification_small_without_image",
  LAYOUT_NOTIFICATION_LARGE_WITHOUT_IMAGE: "notification_large_without_image",

  // Device Types
  XIAOMI_MANUFACTURER: "Xiaomi",

  // Notification Sound Types
  TYPE_NOTIFICATION: 2,
  TYPE_RINGTONE: 1,
  TYPE_ALARM: 4,

  // Flags
  FLAG_UPDATE_CURRENT: 134217728,
  FLAG_CANCEL_CURRENT: 268435456,
  FLAG_NO_CREATE: 536870912,
  FLAG_ONE_SHOT: 1073741824,
  FLAG_IMMUTABLE: 67108864,

  // Build Version Codes
  VERSION_CODES_M: 23,
  VERSION_CODES_N_MR1: 25,
  VERSION_CODES_O: 26,
  VERSION_CODES_TIRAMISU: 33,
};

export const AnalyticsConstants = {
  NOTIFICATION: "notification",
  NOTIFICATION_ID: "notification_id",
  NOTIFICATION_CHANNEL: "notification_channel",
  NOTIFICATION_IMPORTANCE: "notification_importance",
  IS_PERSONALIZED_EXTRA: "is_personalized_extra",
  CATEGORY_NAME: "category_name",
  POST_ID: "post_id",
  CATEGORY_ID: "category_id",
  SOURCE: "source",
  LINK: "link",
  CLICKED: "clicked",
  BUILT: "built",
  BLOCKED: "blocked",
  ACCEPTED: "accepted",
  REJECTED: "rejected",
  ANDROID_POPUP: "android_popup",
  INHOUSE: "inhouse",
  ANDROID: "android",
  NOTIFICATION_SETTING: "notification_setting",
  NOTIFICATION_HUB: "notification_hub",
  STICKY_NOTIF: "sticky_notif",
  JOB_CARD: "job_card",
  BUY_AND_SELL_ARTICLE_CARD: "buy_and_sell_article_card",
  NOTIFICATION_WITHOUT_IMAGE: "notification_without_image",
  NOTIFICATION_WITH_IMAGE: "notification_with_image",
  NOTIFICATION_SERVICE: "notification_service",
  NOTIFICATION_BUILT: "notification_built",
  NOTIFICATION_TYPE: "notification_type",
  NOTIFICATION_INTERVAL: "notification_interval",
  USER_ID: "user_id",
  REPORTER_ID: "reporter_id",
  POST_COUNT: "post_count",
  COMMENTS_COUNT: "comments_count",
  TYPE: "type",
  CATEGORY: "category",
  TIME_STAMP: "time_stamp",
  EVENT_NAME: "event_name",
  POPUP_TYPE: "popup_type",
  REDIRECTION: "redirection",
  NOTIFICATION_PERMISSION: "notification_permission",
  NOTIFICATION_PERMISSION_NUDGE: "notification_permission_nudge",
  LIVE_ENTRY_POINT_NOTIF_POPUP: "live_entry_point_notif_popup",
  TAP_BACK: "tap_back",
  TAP_NOTIFICATION_PERMISSION_CONTINUE: "tap_notification_permission_continue",
  VIEWED_NOTIFICATION_PERMISSION_POPUP: "viewed_notification_permission_popup",
};

export const DbConstants = {
  BODY_EXTRA: "body_extra",
  CATEGORY_NAME: "category_name",
  COMMENT_NOTIFICATION: 1,
};

export const RemoteConfigConstants = {
  NOTIFICATION_KEEP_AT_TOP: "notification_keep_at_top",
  NOTIFICATION_LIMIT: "notification_limit",
  NOTIFICATION_UNLOCK_AT_TOP_LIMIT: "notification_unlock_at_top_limit",
  NOTIFICATION_UNLOCK_AT_TOP_TIMEOUT_MS:
    "notification_unlock_at_top_timeout_ms",
  IS_NOTIFICATION_INGESTION_ENABLED: "is_notification_ingestion_enabled",
  NOTIFICATION_DIALOG_SHOW_COUNT: "notification_dialog_show_count",
  NOTIFICATION_DIALOG_INTERVAL_IN_SECOUNDS:
    "notification_dialog_interval_in_secounds",
  READ_POST_IMPRESSION_COUNT: "read_post_impression_count",
  IS_NOTIFICATION_WORKER_RUNNING: "is_notification_worker_running",
  NOTIFICATION_VERSION: "notification_version",
  UNIFIED_FEED_ENABLED: "unified_feed_enabled",
  JOBS_CATEGORY_ID: "jobs_category_id",
  MIN_VERSION_CODE: "min_version_code",
};

export const SharedPrefConstants = {
  IS_NOTIFICATION_PERMISSION_DENIED_BY_USER:
    "is_notification_permission_denied_by_user",
  NOTIFICATION_OPT_IN_POPUP: "notification_opt_in_popup",
  ANDROID_PERMISSION_POPUP_COUNT: "android_permission_popup_count",
};

export const UserProperties = {
  NOTIFICATIONS_ENABLED: "notifications_enabled",
};
