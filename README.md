# React Native Notification Manager - Lokal

A complete React Native port of Android notification functionality with exact same logic for ordering, re-pushing to top, and device-specific handling. This package provides production-ready notification management with comprehensive features matching the native Android implementation.

## Features

- ✅ **Exact Android Logic Port**: Maintains identical notification behavior as native Android
- ✅ **Remote Configuration**: Flexible configuration management with caching
- ✅ **Analytics Integration**: Comprehensive tracking and crash reporting
- ✅ **Database Persistence**: Local storage for notification data and state
- ✅ **Notification Limiting**: Smart notification management with timestamp-based ordering
- ✅ **Notification Refresh**: Keep notifications at top with device-specific optimizations
- ✅ **Multiple Notification Types**: Support for regular, cricket, quiz, and comment notifications
- ✅ **Grouping Support**: Advanced notification grouping with summary notifications
- ✅ **Device Optimization**: Special handling for Xiaomi and other manufacturers
- ✅ **TypeScript Support**: Full type safety with comprehensive interfaces
- ✅ **Production Ready**: Error handling, logging, and crash reporting built-in

## Installation

```bash
npm install react-native-notification-manager-lokal
# or
yarn add react-native-notification-manager-lokal
```

### Dependencies

This package requires the following peer dependencies:

```bash
npm install @react-native-async-storage/async-storage
```

## Quick Start

### 1. Initialize with Simple Remote Config (Recommended)

```typescript
import {
  UserRemoteConfig,
  initializeSimpleRemoteConfig,
  createNotification,
} from "react-native-notification-manager-lokal";

// Define your notification configuration with full TypeScript support
const myConfig: UserRemoteConfig = {
  notificationVersion: 6, // Use latest layouts
  notificationKeepAtTop: true, // Keep notifications at top
  notificationLimit: 15, // Show max 15 notifications
  isCricketNotificationActive: true, // Enable cricket scores
  isCommentNotificationActive: true, // Enable comments
};

// Initialize with your typed config object
await initializeSimpleRemoteConfig(myConfig);
```

### 1b. Advanced Setup (Optional)

```typescript
import {
  initializeRemoteConfig,
  initializeAnalytics,
} from "react-native-notification-manager-lokal";

// Optional: Set up with custom remote config provider
await initializeRemoteConfig(yourRemoteConfigProvider);

// Optional: Set up analytics and crash reporting
initializeAnalytics(yourAnalyticsProvider, yourCrashReportingProvider);
```

### 2. Create Basic Notifications

```typescript
import { createNotification } from "react-native-notification-manager-lokal";

// Create a simple notification
await createNotification(
  1, // id
  "Breaking News", // title
  "Important update available", // body
  "news", // categoryId
  "News", // categoryName
  "https://example.com/article/123", // uri
  "ACTION_PUSH", // action
  "news_tag" // tag
);
```

### 3. Create Notifications with Images

```typescript
import { createNotificationWithImage } from "react-native-notification-manager-lokal";

await createNotificationWithImage(
  2, // id
  "https://example.com/image.jpg", // imageUrl
  "Sports Update", // title
  "Match results are in!", // body
  "sports", // categoryId
  "Sports", // categoryName
  "https://example.com/match/456", // uri
  "ACTION_PUSH", // action
  "sports_tag" // tag
);
```

## Advanced Usage

### Remote Configuration

Set up remote configuration to control notification behavior dynamically:

```typescript
import {
  initializeRemoteConfig,
  setRemoteConfigValues,
  RemoteConfigProvider,
} from "react-native-notification-manager-lokal";

// Option 1: Use your own remote config provider (Firebase, etc.)
const myRemoteConfigProvider: RemoteConfigProvider = {
  async initialize() {
    // Initialize your remote config
  },
  async getBoolean(key, defaultValue) {
    // Return boolean value from your remote config
    return yourRemoteConfig.getBoolean(key) ?? defaultValue;
  },
  async getNumber(key, defaultValue) {
    // Return number value from your remote config
    return yourRemoteConfig.getNumber(key) ?? defaultValue;
  },
  async getString(key, defaultValue) {
    // Return string value from your remote config
    return yourRemoteConfig.getString(key) ?? defaultValue;
  },
  async fetchAndActivate() {
    // Fetch latest config values
    return await yourRemoteConfig.fetchAndActivate();
  },
};

await initializeRemoteConfig(myRemoteConfigProvider);

// Option 2: Set values manually (useful for testing)
setRemoteConfigValues({
  notification_keep_at_top: true,
  notification_limit: 10,
  notification_unlock_at_top_timeout_ms: 300000,
  is_notification_grouping_active: true,
});
```

### Analytics Integration

Track notification events with your analytics provider:

```typescript
import {
  initializeAnalytics,
  AnalyticsProvider,
  CrashReportingProvider,
} from "react-native-notification-manager-lokal";

const analyticsProvider: AnalyticsProvider = {
  trackEvent(eventName, properties) {
    // Track event with your analytics service
    yourAnalytics.track(eventName, properties);
  },
  trackCategoryEvent(eventName, category, properties) {
    // Track category-specific events
    yourAnalytics.track(eventName, { category, ...properties });
  },
  trackConversionEvent(source, action, properties) {
    // Track conversion events
    yourAnalytics.track("conversion", { source, action, ...properties });
  },
  setUserProperties(properties) {
    yourAnalytics.setUserProperties(properties);
  },
  setUserId(userId) {
    yourAnalytics.setUserId(userId);
  },
};

const crashReportingProvider: CrashReportingProvider = {
  recordException(error) {
    yourCrashlytics.recordError(error);
  },
  log(message) {
    yourCrashlytics.log(message);
  },
  setCustomKey(key, value) {
    yourCrashlytics.setCustomKey(key, value);
  },
  setUserId(userId) {
    yourCrashlytics.setUserId(userId);
  },
};

initializeAnalytics(analyticsProvider, crashReportingProvider);
```

### Cricket Notifications

Create live cricket score notifications:

```typescript
import { createCricketNotification } from "react-native-notification-manager-lokal";

await createCricketNotification(
  "INPROGRESS", // matchState
  "India", // team1Name
  "Australia", // team2Name
  "IND", // team1ShortName
  "AUS", // team2ShortName
  "https://example.com/team1.png", // team1IconUrl
  "https://example.com/team2.png", // team2IconUrl
  "2nd Innings", // matchStatus
  "Melbourne Cricket Ground", // venue
  "250", // team1Score
  "4", // team1Wickets
  "45.2", // team1Overs
  "180", // team2Score
  "8", // team2Wickets
  "35.4" // team2Overs
);
```

### Comment Notifications

Create grouped comment notifications:

```typescript
import {
  createCommentNotification,
  CommentNotificationType,
} from "react-native-notification-manager-lokal";

await createCommentNotification(
  "New Comments", // title
  "John commented on your post", // body
  CommentNotificationType.COMMENT, // notificationType
  "instant", // notificationInterval
  "post_123", // postId
  456, // reporterId
  "user_789", // userId
  1, // postCount
  3, // commentsCount
  true // isGrouped
);
```

### Quiz Notifications

Create interactive quiz notifications:

```typescript
import { createQuizNotification } from "react-native-notification-manager-lokal";

await createQuizNotification(
  3, // id
  "https://example.com/quiz-image.jpg", // imageUrl
  "Daily Quiz", // title
  "Test your knowledge!", // body
  "quiz", // categoryId
  "Quiz", // categoryName
  "https://example.com/quiz/daily", // uri
  "ACTION_PUSH_QUIZ", // action
  "quiz_tag", // tag
  "Quiz", // channel
  5 // importance
);
```

### Database Operations

Access notification data and statistics:

```typescript
import {
  DatabaseManager,
  getTodayNotifications,
  getNotificationBadgeCount,
  cleanOldNotifications,
} from "react-native-notification-manager-lokal";

// Get today's notifications
const todayNotifications = await getTodayNotifications();

// Get unread notification count
const badgeCount = await getNotificationBadgeCount();

// Clean old notifications (older than 7 days)
await cleanOldNotifications();

// Get database statistics
const stats = await DatabaseManager.getInstance().getDatabaseStats();
console.log("Database stats:", stats);
```

### Event Handling

Listen to notification events:

```typescript
import { NotificationManager } from "react-native-notification-manager-lokal";

const manager = NotificationManager.getInstance();

manager.setEventCallbacks({
  onNotificationClick: (data) => {
    console.log("Notification clicked:", data);
    // Handle notification click
  },
  onNotificationDismiss: (notificationId) => {
    console.log("Notification dismissed:", notificationId);
    // Handle notification dismissal
  },
  onNotificationReceived: (data) => {
    console.log("Notification received:", data);
    // Handle notification received
  },
});
```

## Configuration Options

### Remote Config Keys

The package supports the following remote configuration keys:

```typescript
// Notification behavior
notification_keep_at_top: boolean; // Keep notifications at top
notification_limit: number; // Maximum number of notifications
notification_unlock_at_top_timeout_ms: number; // Refresh timeout
notification_unlock_at_top_limit: number; // Refresh limit
is_notification_grouping_active: boolean; // Enable grouping
notification_version: number; // UI version
is_notification_ingestion_enabled: boolean; // Analytics ingestion

// Cricket notifications
is_cricket_notification_active: boolean;
cricket_notification_interval: number;

// Comment notifications
is_comment_notification_active: boolean;
comment_notification_grouping: boolean;

// Sticky notifications
is_sticky_notification_active: boolean;
sticky_notification_time_interval: number;
notification_max_cancel_count: number;
```

### Notification Channels

Default channels are automatically created:

- `Recommendation` - High importance, general notifications
- `Cricket` - Low importance, cricket updates
- `Comments` - Low importance, comment notifications
- `Downloads` - Low importance, download progress
- `Uploads` - Low importance, upload progress

## API Reference

### Core Functions

#### `initializeNotifications(): Promise<void>`

Initialize the notification manager.

#### `createNotification(id, title, body, categoryId, categoryName, uri, action, tag, callbacks?): Promise<void>`

Create a basic notification without image.

#### `createNotificationWithImage(id, imageUrl, title, body, categoryId, categoryName, uri, action, tag, callbacks?): Promise<void>`

Create a notification with image.

#### `cancelNotification(notificationId): Promise<void>`

Cancel a notification by ID.

#### `refreshNotifications(): Promise<void>`

Refresh notifications to keep them at top.

#### `limitNotifications(): Promise<void>`

Apply notification limiting based on configuration.

### Specialized Notifications

#### `createCricketNotification(...args): Promise<void>`

Create cricket score notification.

#### `createQuizNotification(...args): Promise<void>`

Create quiz notification.

#### `createCommentNotification(...args): Promise<void>`

Create comment notification.

### Configuration

#### `initializeRemoteConfig(provider?): Promise<void>`

Initialize remote configuration.

#### `setRemoteConfigValues(values): void`

Set remote config values manually.

### Analytics

#### `initializeAnalytics(analyticsProvider?, crashReportingProvider?): void`

Initialize analytics and crash reporting.

#### `trackNotificationBuilt(category, properties): void`

Track notification built event.

#### `trackNotificationClicked(source, properties): void`

Track notification clicked event.

### Database

#### `getTodayNotifications(): Promise<NotificationData[]>`

Get notifications from last 24 hours.

#### `getNotificationBadgeCount(): Promise<number>`

Get unread notification count.

#### `cleanOldNotifications(): Promise<void>`

Clean notifications older than 7 days.

## TypeScript Support

The package includes comprehensive TypeScript definitions:

```typescript
import {
  NotificationPayload,
  NotificationData,
  RemoteConfigProvider,
  AnalyticsProvider,
  CrashReportingProvider,
  CricketNotificationData,
  CommentNotificationData,
  NotificationCallbacks,
} from "react-native-notification-manager-lokal";
```

## Error Handling

The package includes comprehensive error handling:

- All methods include try-catch blocks
- Errors are logged to console and crash reporting
- Graceful fallbacks for missing dependencies
- Network failure resilience

## Performance Considerations

- Notifications are limited based on configuration to prevent memory issues
- Old notifications are automatically cleaned up
- Database operations are optimized with caching
- Device-specific optimizations for better performance

## Platform Support

- ✅ Android (Full support with native module)
- ⚠️ iOS (Limited support, basic functionality only)

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For issues and questions:

- Create an issue on GitHub
- Check the documentation
- Review the example implementation

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history and updates.
