# Remote Config Setup Guide

This guide explains how to provide remote configuration when using the React Native Notification Manager.

## 🚀 Quick Setup

### Option 1: Firebase Remote Config (Recommended)

```typescript
import {
  initializeRemoteConfig,
  RemoteConfigProvider,
} from "react-native-notification-manager-lokal";
import remoteConfig from "@react-native-firebase/remote-config";

// Create Firebase Remote Config provider
const firebaseRemoteConfigProvider: RemoteConfigProvider = {
  async initialize() {
    await remoteConfig().setDefaults({
      notification_keep_at_top: false,
      notification_limit: 10,
      notification_unlock_at_top_timeout_ms: 300000,
      notification_unlock_at_top_limit: 3,
      is_notification_grouping_active: true,
      notification_version: 1,
      is_notification_ingestion_enabled: false,
      is_cricket_notification_active: false,
      cricket_notification_interval: 30000,
      is_comment_notification_active: true,
      comment_notification_grouping: true,
      is_sticky_notification_active: false,
      sticky_notification_time_interval: 3600000,
      notification_max_cancel_count: 5,
      notification_ui_version: "1",
      is_unified_feed_active: false,
    });

    // Set minimum fetch interval
    await remoteConfig().setConfigSettings({
      minimumFetchIntervalMillis: 300000, // 5 minutes
    });
  },

  async getBoolean(key, defaultValue) {
    try {
      return remoteConfig().getValue(key).asBoolean();
    } catch (error) {
      console.warn(`Failed to get remote config boolean ${key}:`, error);
      return defaultValue;
    }
  },

  async getNumber(key, defaultValue) {
    try {
      return remoteConfig().getValue(key).asNumber();
    } catch (error) {
      console.warn(`Failed to get remote config number ${key}:`, error);
      return defaultValue;
    }
  },

  async getString(key, defaultValue) {
    try {
      return remoteConfig().getValue(key).asString();
    } catch (error) {
      console.warn(`Failed to get remote config string ${key}:`, error);
      return defaultValue;
    }
  },

  async fetchAndActivate() {
    try {
      return await remoteConfig().fetchAndActivate();
    } catch (error) {
      console.error("Failed to fetch and activate remote config:", error);
      return false;
    }
  },
};

// Initialize the notification manager with Firebase Remote Config
await initializeRemoteConfig(firebaseRemoteConfigProvider);
```

### Option 2: Custom API Remote Config

```typescript
import {
  initializeRemoteConfig,
  RemoteConfigProvider,
} from "react-native-notification-manager-lokal";

// Create custom API remote config provider
const customApiRemoteConfigProvider: RemoteConfigProvider = {
  async initialize() {
    // Initialize your custom remote config service
    console.log("Initializing custom remote config...");
  },

  async getBoolean(key, defaultValue) {
    try {
      const response = await fetch(`https://your-api.com/config/${key}`);
      const data = await response.json();
      return data.value !== undefined ? Boolean(data.value) : defaultValue;
    } catch (error) {
      console.warn(`Failed to get remote config boolean ${key}:`, error);
      return defaultValue;
    }
  },

  async getNumber(key, defaultValue) {
    try {
      const response = await fetch(`https://your-api.com/config/${key}`);
      const data = await response.json();
      return data.value !== undefined ? Number(data.value) : defaultValue;
    } catch (error) {
      console.warn(`Failed to get remote config number ${key}:`, error);
      return defaultValue;
    }
  },

  async getString(key, defaultValue) {
    try {
      const response = await fetch(`https://your-api.com/config/${key}`);
      const data = await response.json();
      return data.value !== undefined ? String(data.value) : defaultValue;
    } catch (error) {
      console.warn(`Failed to get remote config string ${key}:`, error);
      return defaultValue;
    }
  },

  async fetchAndActivate() {
    try {
      // Fetch latest config from your API
      const response = await fetch("https://your-api.com/config/refresh");
      return response.ok;
    } catch (error) {
      console.error("Failed to fetch and activate remote config:", error);
      return false;
    }
  },
};

// Initialize with custom API provider
await initializeRemoteConfig(customApiRemoteConfigProvider);
```

### Option 3: Manual Configuration (Testing/Development)

```typescript
import { setRemoteConfigValues } from "react-native-notification-manager-lokal";

// Set values manually for testing or when no remote config service is available
setRemoteConfigValues({
  // Notification behavior
  notification_keep_at_top: true,
  notification_limit: 15,
  notification_unlock_at_top_timeout_ms: 600000, // 10 minutes
  notification_unlock_at_top_limit: 5,
  is_notification_grouping_active: true,
  notification_version: 6, // Use latest layout version
  is_notification_ingestion_enabled: true,

  // Cricket notifications
  is_cricket_notification_active: true,
  cricket_notification_interval: 45000, // 45 seconds

  // Comment notifications
  is_comment_notification_active: true,
  comment_notification_grouping: true,

  // Sticky notifications
  is_sticky_notification_active: true,
  sticky_notification_time_interval: 1800000, // 30 minutes
  notification_max_cancel_count: 3,

  // UI configuration
  notification_ui_version: "6",
  is_unified_feed_active: false,
});
```

## 📋 Available Remote Config Keys

### Notification Behavior

```typescript
notification_keep_at_top: boolean; // Keep notifications at top of list
notification_limit: number; // Maximum number of notifications
notification_unlock_at_top_timeout_ms: number; // Refresh timeout in milliseconds
notification_unlock_at_top_limit: number; // Minimum notifications before refresh
is_notification_grouping_active: boolean; // Enable notification grouping
notification_version: number; // UI version (1-7)
is_notification_ingestion_enabled: boolean; // Enable analytics ingestion
```

### Cricket Notifications

```typescript
is_cricket_notification_active: boolean; // Enable cricket notifications
cricket_notification_interval: number; // Update interval in milliseconds
```

### Comment Notifications

```typescript
is_comment_notification_active: boolean; // Enable comment notifications
comment_notification_grouping: boolean; // Group comment notifications
```

### Sticky Notifications

```typescript
is_sticky_notification_active: boolean; // Enable sticky notifications
sticky_notification_time_interval: number; // Sticky notification interval
notification_max_cancel_count: number; // Max cancellations before stopping
```

### UI Configuration

```typescript
notification_ui_version: string; // UI version as string
is_unified_feed_active: boolean; // Enable unified feed experiment
```

## 🔄 Dynamic Configuration Updates

### Fetch Latest Config

```typescript
import { RemoteConfigManager } from "react-native-notification-manager-lokal";

// Fetch and activate latest remote config
const success = await RemoteConfigManager.getInstance().fetchAndActivate();
if (success) {
  console.log("Remote config updated successfully");
}
```

### Get Current Config Values

```typescript
import {
  getRemoteConfigBoolean,
  getRemoteConfigNumber,
  RemoteConfigConstants,
} from "react-native-notification-manager-lokal";

// Get specific config values
const keepAtTop = await getRemoteConfigBoolean(
  RemoteConfigConstants.NOTIFICATION_KEEP_AT_TOP,
  false
);

const notificationLimit = await getRemoteConfigNumber(
  RemoteConfigConstants.NOTIFICATION_LIMIT,
  10
);

console.log("Keep at top:", keepAtTop);
console.log("Notification limit:", notificationLimit);
```

### Check All Cached Values

```typescript
import { RemoteConfigManager } from "react-native-notification-manager-lokal";

const allValues = RemoteConfigManager.getInstance().getCachedValues();
console.log("All remote config values:", allValues);
```

## 🎛️ Complete Setup Example

```typescript
import {
  initializeRemoteConfig,
  initializeAnalytics,
  createNotification,
  setNotificationVersion,
  RemoteConfigProvider,
  AnalyticsProvider,
  CrashReportingProvider,
} from "react-native-notification-manager-lokal";

// 1. Set up remote config
const remoteConfigProvider: RemoteConfigProvider = {
  // Your implementation here (Firebase, custom API, etc.)
};

// 2. Set up analytics (optional)
const analyticsProvider: AnalyticsProvider = {
  trackEvent: (eventName, properties) => {
    // Your analytics implementation
  },
  trackCategoryEvent: (eventName, category, properties) => {
    // Your category event tracking
  },
  // ... other methods
};

const crashReportingProvider: CrashReportingProvider = {
  recordException: (error) => {
    // Your crash reporting implementation
  },
  // ... other methods
};

// 3. Initialize everything
async function setupNotificationManager() {
  try {
    // Initialize remote config
    await initializeRemoteConfig(remoteConfigProvider);

    // Initialize analytics (optional)
    initializeAnalytics(analyticsProvider, crashReportingProvider);

    // Set notification version (uses remote config if available)
    await setNotificationVersion(6);

    console.log("Notification manager setup complete!");

    // Now you can create notifications with full feature parity
    await createNotification(
      1,
      "Test Notification",
      "This uses the latest layouts!",
      "test",
      "Test",
      "https://example.com",
      "ACTION_PUSH",
      "test_tag"
    );
  } catch (error) {
    console.error("Failed to setup notification manager:", error);
  }
}

setupNotificationManager();
```

## 🔧 Advanced Configuration

### Environment-Specific Config

```typescript
// Development
setRemoteConfigValues({
  notification_version: 1, // Use basic layouts for development
  is_notification_ingestion_enabled: false,
  notification_limit: 5,
});

// Production
await initializeRemoteConfig(firebaseRemoteConfigProvider);
```

### A/B Testing Setup

```typescript
// Set different notification versions for A/B testing
const userGroup = getUserABTestGroup(); // Your A/B testing logic

const notificationVersion = userGroup === "control" ? 1 : 6;
await setNotificationVersion(notificationVersion);

setRemoteConfigValues({
  notification_version: notificationVersion,
  // Other A/B test specific configs
});
```

The remote config system provides complete flexibility for controlling notification behavior dynamically without app updates!
