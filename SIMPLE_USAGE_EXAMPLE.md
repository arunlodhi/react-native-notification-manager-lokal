# Simple Remote Config Usage

This guide shows how to provide remote configuration using a well-defined typed object.

## 🎯 Simple Setup (Recommended)

### Step 1: Define Your Configuration

```typescript
import {
  UserRemoteConfig,
  initializeSimpleRemoteConfig,
  createNotification,
} from "react-native-notification-manager-lokal";

// Define your notification configuration with full TypeScript support
const myNotificationConfig: UserRemoteConfig = {
  // Notification behavior
  notificationKeepAtTop: true, // Keep notifications at top
  notificationLimit: 15, // Show max 15 notifications
  notificationVersion: 6, // Use latest layout version
  isNotificationGroupingActive: true, // Group similar notifications

  // Feature toggles
  isCricketNotificationActive: true, // Enable cricket scores
  isCommentNotificationActive: true, // Enable comment notifications
  isStickyNotificationActive: false, // Disable sticky notifications

  // Timing configurations
  notificationUnlockAtTopTimeoutMs: 600000, // 10 minutes refresh timeout
  cricketNotificationInterval: 45000, // 45 seconds cricket updates
  stickyNotificationTimeInterval: 1800000, // 30 minutes sticky interval

  // Advanced settings
  isNotificationIngestionEnabled: true, // Enable analytics
  isUnifiedFeedActive: false, // Disable unified feed experiment
};
```

### Step 2: Initialize the Notification Manager

```typescript
async function setupNotifications() {
  try {
    // Initialize with your typed config object
    await initializeSimpleRemoteConfig(myNotificationConfig);

    console.log("✅ Notification manager initialized with your config!");

    // Now create notifications - they'll use your configuration
    await createNotification(
      1,
      "Welcome!",
      "Notifications are now configured with your settings",
      "welcome",
      "Welcome",
      "https://example.com",
      "ACTION_PUSH",
      "welcome_tag"
    );
  } catch (error) {
    console.error("❌ Setup failed:", error);
  }
}

setupNotifications();
```

## 🔧 Dynamic Configuration Updates

### Update Specific Settings

```typescript
import { updateSimpleRemoteConfig } from "react-native-notification-manager-lokal";

// Update only specific settings
await updateSimpleRemoteConfig({
  notificationVersion: 7, // Switch to newest layout
  notificationLimit: 20, // Increase limit
  isCricketNotificationActive: false, // Disable cricket
});
```

### Get Current Configuration

```typescript
import { getSimpleRemoteConfig } from "react-native-notification-manager-lokal";

const currentConfig = getSimpleRemoteConfig();
console.log("Current notification version:", currentConfig.notificationVersion);
console.log("Current notification limit:", currentConfig.notificationLimit);
```

### Check Feature Status

```typescript
import { isFeatureEnabled } from "react-native-notification-manager-lokal";

if (isFeatureEnabled("isCricketNotificationActive")) {
  console.log("Cricket notifications are enabled");
  // Create cricket notification
}

if (isFeatureEnabled("isCommentNotificationActive")) {
  console.log("Comment notifications are enabled");
  // Handle comment notifications
}
```

## 📋 Complete Configuration Reference

```typescript
interface UserRemoteConfig {
  // 🔔 Notification Behavior
  notificationKeepAtTop?: boolean; // Keep notifications at top of list
  notificationLimit?: number; // Maximum number of notifications (0-100)
  notificationUnlockAtTopTimeoutMs?: number; // Refresh timeout in milliseconds
  notificationUnlockAtTopLimit?: number; // Minimum notifications before refresh
  isNotificationGroupingActive?: boolean; // Enable notification grouping
  notificationVersion?: number; // UI version (1-7)
  isNotificationIngestionEnabled?: boolean; // Enable analytics ingestion
  isNotificationWorkerRunning?: boolean; // Background worker status

  // 🏏 Cricket Notifications
  isCricketNotificationActive?: boolean; // Enable cricket score notifications
  cricketNotificationInterval?: number; // Update interval in milliseconds (min: 1000ms)

  // 💬 Comment Notifications
  isCommentNotificationActive?: boolean; // Enable comment notifications
  commentNotificationGrouping?: boolean; // Group comment notifications

  // 📌 Sticky Notifications
  isStickyNotificationActive?: boolean; // Enable persistent notifications
  stickyNotificationTimeInterval?: number; // Sticky notification interval (min: 60000ms)
  notificationMaxCancelCount?: number; // Max cancellations before stopping

  // 🎨 UI Configuration
  notificationUIVersion?: string; // UI version as string
  isUnifiedFeedActive?: boolean; // Enable unified feed experiment
}
```

## 🎨 Layout Version Examples

### Basic Layout (Version 1)

```typescript
const basicConfig: UserRemoteConfig = {
  notificationVersion: 1, // Simple, clean layout
  notificationKeepAtTop: false,
  notificationLimit: 5,
};
```

### Advanced Layout (Version 6)

```typescript
const advancedConfig: UserRemoteConfig = {
  notificationVersion: 6, // Latest layout with blur effects, share buttons
  notificationKeepAtTop: true,
  notificationLimit: 15,
  isNotificationGroupingActive: true,
};
```

### Production Configuration

```typescript
const productionConfig: UserRemoteConfig = {
  // Use latest features
  notificationVersion: 6,
  notificationKeepAtTop: true,
  notificationLimit: 20,

  // Enable all notification types
  isCricketNotificationActive: true,
  isCommentNotificationActive: true,
  isStickyNotificationActive: true,

  // Optimize for performance
  notificationUnlockAtTopTimeoutMs: 300000, // 5 minutes
  cricketNotificationInterval: 30000, // 30 seconds

  // Enable analytics
  isNotificationIngestionEnabled: true,
};
```

## 🧪 A/B Testing Configuration

```typescript
// Define different configs for A/B testing
const controlGroupConfig: UserRemoteConfig = {
  notificationVersion: 1,
  notificationKeepAtTop: false,
  notificationLimit: 10,
};

const testGroupConfig: UserRemoteConfig = {
  notificationVersion: 6,
  notificationKeepAtTop: true,
  notificationLimit: 20,
  isNotificationGroupingActive: true,
};

// Apply based on user group
const userGroup = getUserABTestGroup(); // Your A/B testing logic
const config = userGroup === "control" ? controlGroupConfig : testGroupConfig;

await initializeSimpleRemoteConfig(config);
```

## ✅ Validation and Error Handling

```typescript
import {
  validateUserRemoteConfig,
  UserRemoteConfig,
} from "react-native-notification-manager-lokal";

const userConfig: UserRemoteConfig = {
  notificationVersion: 8, // Invalid! Must be 1-7
  notificationLimit: -5, // Invalid! Must be positive
};

// Validate before using
const validation = validateUserRemoteConfig(userConfig);
if (!validation.isValid) {
  console.error("Config validation failed:", validation.errors);
  // Handle validation errors
} else {
  await initializeSimpleRemoteConfig(userConfig);
}
```

## 🔄 Runtime Configuration Changes

```typescript
import {
  updateSimpleRemoteConfig,
  getNotificationVersion,
  shouldKeepNotificationsAtTop,
} from "react-native-notification-manager-lokal";

// Check current settings
console.log("Current version:", getNotificationVersion());
console.log("Keep at top:", shouldKeepNotificationsAtTop());

// Update settings at runtime
await updateSimpleRemoteConfig({
  notificationVersion: 7,
  notificationKeepAtTop: false,
});

// Settings are immediately applied to new notifications
```

## 🚀 Complete Working Example

```typescript
import {
  UserRemoteConfig,
  initializeSimpleRemoteConfig,
  createNotification,
  createCricketNotification,
  updateSimpleRemoteConfig,
  getNotificationVersion,
} from "react-native-notification-manager-lokal";

async function completeExample() {
  // 1. Define your configuration
  const config: UserRemoteConfig = {
    notificationVersion: 6,
    notificationKeepAtTop: true,
    notificationLimit: 15,
    isCricketNotificationActive: true,
    isCommentNotificationActive: true,
    cricketNotificationInterval: 30000,
  };

  // 2. Initialize
  await initializeSimpleRemoteConfig(config);

  // 3. Create notifications (they'll use your config automatically)
  await createNotification(
    1,
    "Breaking News",
    "Important update!",
    "news",
    "News",
    "https://example.com/article",
    "ACTION_PUSH",
    "news"
  );

  // 4. Create cricket notification (if enabled in config)
  if (config.isCricketNotificationActive) {
    await createCricketNotification(
      "INPROGRESS",
      "India",
      "Australia",
      "IND",
      "AUS",
      undefined,
      undefined,
      "2nd Innings",
      "MCG",
      "250",
      "4",
      "45.2",
      "180",
      "8",
      "35.4"
    );
  }

  // 5. Update config at runtime
  await updateSimpleRemoteConfig({
    notificationVersion: 7, // Switch to newest layout
  });

  console.log(
    "✅ All done! Notifications are using version:",
    getNotificationVersion()
  );
}

completeExample();
```

## 💡 Key Benefits

- ✅ **Type Safety** - Full TypeScript support with IntelliSense
- ✅ **Validation** - Automatic validation of config values
- ✅ **Defaults** - Sensible defaults for all settings
- ✅ **Runtime Updates** - Change settings without restart
- ✅ **Persistence** - Configuration is automatically saved
- ✅ **Simple API** - Just provide an object, no complex setup

This approach gives you complete control over notification behavior with a simple, well-typed configuration object!
