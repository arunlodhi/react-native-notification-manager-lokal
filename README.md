# React Native Notification Port

A complete React Native port of the Android notification functionality from the Lokal app, maintaining **exactly the same logic** for notification ordering, re-pushing to top, limiting, and device-specific handling.

## Features

✅ **Exact Android Logic Port**: All notification management logic ported 1:1 from Android  
✅ **Notification Ordering**: Timestamp-based sorting (newest first)  
✅ **Re-pushing to Top**: Periodic refresh every 15 minutes to keep notifications visible  
✅ **Notification Limiting**: Configurable limits with oldest notification removal  
✅ **Device-Specific Handling**: Special logic for Xiaomi devices  
✅ **Notification Recreation**: Complete recreation with fresh timestamps  
✅ **Group Management**: Support for grouped notifications  
✅ **Analytics Integration**: Built-in analytics event tracking  
✅ **Deep Linking**: Click/impression events passed to React Native

## Installation

```bash
npm install react-native-notification-manager-lokal
```

### Android Setup

**✅ Autolinking Support**: This module supports React Native autolinking (0.60+) and new architecture. No manual linking required!

#### Step 1: Install Dependencies (Optional)

The module automatically includes required dependencies, but you can add these to your `android/app/build.gradle` if you need specific versions:

```gradle
// android/app/build.gradle (optional - already included in the module)
dependencies {
  // Image loading (already included)
  // implementation 'com.github.bumptech.glide:glide:4.14.2'
  // implementation 'jp.wasabeef:glide-transformations:4.3.0'
}
```

#### Step 2: Permissions (Automatic)

The module automatically adds required permissions via its AndroidManifest.xml:

- `android.permission.VIBRATE`
- `android.permission.WAKE_LOCK`
- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.SCHEDULE_EXACT_ALARM`
- `android.permission.POST_NOTIFICATIONS`

#### Step 3: Receivers (Automatic)

The module automatically registers required broadcast receivers:

- `NotificationClickReceiver` - Handles notification clicks
- `LocalNotificationReceiver` - Handles local notifications

**That's it! No manual setup required.** 🎉

## Quick Start

```typescript
import {
  NotificationManager,
  initializeNotifications,
  createNotification,
  createNotificationWithImage,
} from "react-native-notification-manager-lokal";

// Initialize the notification system
await initializeNotifications();

// Set up event callbacks for analytics and deep linking
NotificationManager.getInstance().setEventCallbacks({
  onNotificationClick: (data) => {
    // Handle deep linking
    console.log("Notification clicked:", data);
    // Navigate to appropriate screen based on data.uri
  },
  onNotificationDismiss: (notificationId) => {
    // Track dismissal analytics
    console.log("Notification dismissed:", notificationId);
  },
  onNotificationReceived: (data) => {
    // Track impression analytics
    console.log("Notification received:", data);
  },
});

// Create a simple notification
await createNotification(
  12345, // id
  "Breaking News", // title
  "Important update...", // body
  "1", // categoryId
  "News", // categoryName
  "/article/12345", // uri for deep linking
  "ACTION_PUSH", // action
  "news_tag" // tag
);

// Create notification with image
await createNotificationWithImage(
  12346, // id
  "https://example.com/image.jpg", // imageUrl
  "Photo News", // title
  "Check out this image", // body
  "1", // categoryId
  "News", // categoryName
  "/article/12346", // uri
  "ACTION_PUSH", // action
  "photo_tag" // tag
);
```

## Architecture

The module uses a **simplified architecture** to avoid duplication:

### **React Native Layer (TypeScript):**

- **NotificationManager**: Main interface for all notification operations
- **LocalNotificationManager**: Handles local/scheduled notifications
- **NotificationRefreshAlarmManager**: Manages periodic refresh scheduling
- **Types & Interfaces**: TypeScript definitions and callbacks

### **Native Layer (Android Java):**

- **NotificationManagerModule**: Single native module handling all core functionality
- **LocalNotificationManagerModule**: Handles local notification scheduling
- **NotificationRefreshAlarmManagerModule**: Manages alarm-based refresh
- **NotificationClickReceiver**: Handles notification click events
- **NotificationManagerPackage**: React Native package registration

### **Key Design Principles:**

- ✅ **Single Responsibility**: Each module has one clear purpose
- ✅ **No Duplication**: Logic exists in one place (native layer)
- ✅ **Clean Interface**: TypeScript layer provides clean API
- ✅ **Exact Android Logic**: All Android logic preserved in native modules

## Core Components

### NotificationManager

Main entry point for all notification functionality.

```typescript
import {
  NotificationManager,
  initializeNotifications,
  createNotification,
  createNotificationWithImage,
  refreshNotifications,
  limitNotifications,
} from "react-native-notification-manager-lokal";

// Initialize (convenience function)
await initializeNotifications();

// Or use the manager directly
const manager = NotificationManager.getInstance();
await manager.initialize();

// Create notifications (convenience functions)
await createNotification(
  id,
  title,
  body,
  categoryId,
  categoryName,
  uri,
  action,
  tag
);
await createNotificationWithImage(
  id,
  imageUrl,
  title,
  body,
  categoryId,
  categoryName,
  uri,
  action,
  tag
);

// Or use manager methods
await manager.createNotification(/* parameters */);
await manager.createNotificationWithImage(/* parameters */);

// Management operations
await refreshNotifications(); // Re-push to top
await limitNotifications(); // Apply limits
await manager.cancelNotification(id); // Cancel specific notification
```

### Local Notifications

```typescript
import {
  scheduleLocalNotification,
  cancelLocalNotification,
  getScheduledLocalNotifications,
} from "react-native-notification-manager-lokal";

// Schedule a local notification
await scheduleLocalNotification(
  123, // id
  "Reminder", // title
  "Don't forget!", // body
  Date.now() + 60000, // scheduledTime (1 minute from now)
  { customData: "value" } // optional data
);

// Cancel a scheduled notification
await cancelLocalNotification(123);

// Get all scheduled notifications
const scheduled = await getScheduledLocalNotifications();
```

### Periodic Refresh

```typescript
import {
  scheduleNotificationRefresh,
  cancelNotificationRefresh,
} from "react-native-notification-manager-lokal";

// Start periodic refresh (every 15 minutes)
await scheduleNotificationRefresh();

// Stop periodic refresh
await cancelNotificationRefresh();
```

## Advanced Usage

### Custom Notification Configuration

```typescript
await manager.createNotification(
  id,
  title,
  body,
  categoryId,
  categoryName,
  uri,
  action,
  tag,
  "CustomChannel", // channel
  Constants.IMPORTANCE_MAX, // importance
  true, // isGroupingNeeded
  123, // groupID
  "video", // notifType
  true, // isPersonalized
  {
    onNotificationBuilt: (bundle) => {
      // Analytics callback
      console.log("Notification built:", bundle);
    },
  }
);
```

### Notification Recreation

```typescript
import { NotificationReCreator } from "react-native-notification-manager-lokal";

const recreator = NotificationReCreator.getInstance();

// Convert stored data to payload
const payload = recreator.convertToNotificationPayload(notificationData);

// Recreate notification
await recreator.createNotification(payload);
```

### Local Notifications

```typescript
import {
  LocalNotificationManager,
  scheduleLocalNotification,
} from "react-native-notification-manager-lokal";

// Schedule a local notification
await scheduleLocalNotification(
  123, // id
  "Reminder", // title
  "Don't forget!", // body
  Date.now() + 60000, // scheduledTime (1 minute from now)
  { customData: "value" } // optional data
);
```

## Configuration

### Remote Config Integration

The system uses several remote config flags that match the Android implementation:

```typescript
// These should be implemented in your remote config system
const remoteConfigKeys = {
  NOTIFICATION_KEEP_AT_TOP: "notification_keep_at_top",
  NOTIFICATION_LIMIT: "notification_limit",
  NOTIFICATION_UNLOCK_AT_TOP_LIMIT: "notification_unlock_at_top_limit",
  NOTIFICATION_UNLOCK_AT_TOP_TIMEOUT_MS:
    "notification_unlock_at_top_timeout_ms",
};
```

### Constants

All constants from the Android implementation are available:

```typescript
import { Constants } from "react-native-notification-manager-lokal";

// Notification priorities
Constants.IMPORTANCE_MAX;
Constants.IMPORTANCE_HIGH;
Constants.IMPORTANCE_DEFAULT;

// Channels
Constants.DEFAULT_CHANNEL;
Constants.CRICKET_CHANNEL;
Constants.COMMENTS_CHANNEL;

// Actions
Constants.ACTION_PUSH;
Constants.ACTION_PUSH_VIDEO;
Constants.ACTION_PUSH_CRICKET;

// And many more...
```

## Event Handling

### Analytics Integration

```typescript
NotificationManager.getInstance().setEventCallbacks({
  onNotificationClick: (data) => {
    // Track click analytics
    analytics.track("notification_clicked", {
      notification_id: data.notificationId,
      category_id: data.categoryId,
      post_id: data.postId,
      uri: data.uri,
    });

    // Handle deep linking
    navigation.navigate("Article", { id: data.postId });
  },

  onNotificationReceived: (data) => {
    // Track impression analytics
    analytics.track("notification_received", {
      notification_id: data.notificationId,
      category_id: data.categoryId,
    });
  },
});
```

### Deep Linking

```typescript
const handleNotificationClick = (data) => {
  const { uri, action, categoryId } = data;

  if (action === Constants.ACTION_PUSH) {
    // Handle article navigation
    if (uri.includes("/article/")) {
      const articleId = uri.split("/").pop();
      navigation.navigate("Article", { id: articleId });
    }
  } else if (action === Constants.ACTION_PUSH_VIDEO) {
    // Handle video navigation
    navigation.navigate("Video", { uri });
  }
  // Add more action handlers as needed
};
```

## Complete Integration Example

Here's a complete example of how to integrate the notification system into your React Native app:

### App.tsx

```typescript
import React, { useEffect } from "react";
import { NavigationContainer } from "@react-navigation/native";
import {
  NotificationManager,
  initializeNotifications,
  scheduleNotificationRefresh,
} from "react-native-notification-manager-lokal";
import analytics from "@react-native-firebase/analytics";
import crashlytics from "@react-native-firebase/crashlytics";

export default function App() {
  useEffect(() => {
    setupNotifications();
  }, []);

  const setupNotifications = async () => {
    try {
      // Initialize the notification system
      await initializeNotifications();

      // Set up event callbacks
      NotificationManager.getInstance().setEventCallbacks({
        onNotificationClick: (data) => {
          // Track analytics
          analytics().logEvent("notification_clicked", {
            notification_id: data.notificationId,
            category_id: data.categoryId,
            action: data.action,
            uri: data.uri,
          });

          // Handle deep linking
          handleDeepLink(data.uri, data.action);
        },

        onNotificationReceived: (data) => {
          // Track impression
          analytics().logEvent("notification_received", {
            notification_id: data.notificationId,
            category_id: data.categoryId,
          });
        },

        onNotificationDismiss: (notificationId) => {
          analytics().logEvent("notification_dismissed", {
            notification_id: notificationId,
          });
        },

        onAnalyticsEvent: (eventName, eventType, properties) => {
          analytics().logEvent(eventName, properties);
        },

        onError: (error) => {
          crashlytics().recordError(error);
          console.error("Notification error:", error);
        },
      });

      // Start periodic refresh (every 15 minutes)
      await scheduleNotificationRefresh();

      console.log("Notification system initialized successfully");
    } catch (error) {
      console.error("Failed to initialize notifications:", error);
      crashlytics().recordError(error);
    }
  };

  const handleDeepLink = (uri: string, action: string) => {
    if (!uri) return;

    // Handle different types of deep links
    if (uri.includes("/article/")) {
      const articleId = uri.split("/").pop();
      // Navigate to article screen
      // navigation.navigate('Article', { id: articleId });
    } else if (uri.includes("/video/")) {
      const videoId = uri.split("/").pop();
      // Navigate to video screen
      // navigation.navigate('Video', { id: videoId });
    } else if (uri.includes("/job/")) {
      const jobId = uri.split("/").pop();
      // Navigate to job screen
      // navigation.navigate('Job', { id: jobId });
    }
  };

  return <NavigationContainer>{/* Your app navigation */}</NavigationContainer>;
}
```

### Creating Notifications in Your Service

```typescript
// NotificationService.ts
import {
  createNotification,
  createNotificationWithImage,
  Constants,
} from "react-native-notification-manager-lokal";

export class NotificationService {
  static async createNewsNotification(newsData: any) {
    try {
      if (newsData.imageUrl) {
        await createNotificationWithImage(
          newsData.id,
          newsData.imageUrl,
          newsData.title,
          newsData.summary,
          newsData.categoryId,
          newsData.categoryName,
          `/article/${newsData.id}`,
          Constants.ACTION_PUSH,
          `news_${newsData.id}`,
          {
            onNotificationBuilt: (bundle) => {
              console.log("News notification created:", bundle);
            },
          }
        );
      } else {
        await createNotification(
          newsData.id,
          newsData.title,
          newsData.summary,
          newsData.categoryId,
          newsData.categoryName,
          `/article/${newsData.id}`,
          Constants.ACTION_PUSH,
          `news_${newsData.id}`,
          {
            onNotificationBuilt: (bundle) => {
              console.log("News notification created:", bundle);
            },
          }
        );
      }
    } catch (error) {
      console.error("Failed to create news notification:", error);
    }
  }

  static async createJobNotification(jobData: any) {
    await createNotification(
      jobData.id,
      `New Job: ${jobData.title}`,
      `${jobData.company} - ${jobData.location}`,
      "2", // Jobs category
      "Jobs",
      `/job/${jobData.id}`,
      Constants.ACTION_PUSH,
      `job_${jobData.id}`
    );
  }

  static async createVideoNotification(videoData: any) {
    await createNotificationWithImage(
      videoData.id,
      videoData.thumbnail,
      videoData.title,
      videoData.description,
      videoData.categoryId,
      "Videos",
      `/video/${videoData.id}`,
      Constants.ACTION_PUSH_VIDEO,
      `video_${videoData.id}`
    );
  }
}
```

## Troubleshooting

### Common Issues

#### 1. Notifications Not Appearing

**Problem**: Notifications are created but don't appear on the device.

**Solutions**:

- Check if notification permissions are granted
- Verify notification channels are created properly
- Ensure the app is not in battery optimization mode (especially on Xiaomi devices)

```typescript
// Check notification permissions
const manager = NotificationManager.getInstance();
const notifications = await manager.getActiveNotifications();
console.log("Active notifications:", notifications.length);
```

#### 2. Images Not Loading

**Problem**: Notifications with images show without the image.

**Solutions**:

- Verify Glide dependencies are added to build.gradle
- Check if image URLs are accessible
- Ensure proper internet permissions

```gradle
// Make sure these are in your build.gradle
implementation 'com.github.bumptech.glide:glide:4.14.2'
implementation 'jp.wasabeef:glide-transformations:4.3.0'
```

#### 3. Click Events Not Working

**Problem**: Notification clicks don't trigger the callback.

**Solutions**:

- Verify NotificationClickReceiver is registered in AndroidManifest.xml
- Check if event callbacks are set before notifications are created
- Ensure the app is properly handling the React Native bridge

```xml
<!-- Verify this is in your AndroidManifest.xml -->
<receiver
  android:name="io.lokal.notifications.NotificationClickReceiver"
  android:exported="false" />
```

#### 4. Periodic Refresh Not Working

**Problem**: Notifications don't refresh every 15 minutes.

**Solutions**:

- Check if SCHEDULE_EXACT_ALARM permission is granted
- Verify the device allows background processing
- Check battery optimization settings

```typescript
// Start periodic refresh manually
import { scheduleNotificationRefresh } from "react-native-notification-manager-lokal";
await scheduleNotificationRefresh();
```

#### 5. TypeScript Errors

**Problem**: TypeScript compilation errors.

**Solutions**:

- Ensure all peer dependencies are installed
- Check if @types/react-native is compatible
- Verify the module is properly imported

```bash
npm install --save-dev @types/react @types/react-native
```

### Device-Specific Issues

#### Xiaomi Devices

Xiaomi devices have aggressive battery optimization. Users need to:

1. Go to Settings > Apps > [Your App] > Battery saver > No restrictions
2. Go to Settings > Apps > [Your App] > Autostart > Enable
3. Go to Settings > Apps > [Your App] > Other permissions > Display pop-up windows while running in background

#### OnePlus Devices

OnePlus devices may kill background processes:

1. Go to Settings > Apps > [Your App] > Battery > Battery optimization > Don't optimize
2. Go to Settings > Apps > [Your App] > App permissions > Allow all permissions

### Debug Mode

Enable debug logging to troubleshoot issues:

```typescript
// Add this to see detailed logs
import { NotificationManager } from "react-native-notification-manager-lokal";

const manager = NotificationManager.getInstance();

// This will log all notification operations
manager.setEventCallbacks({
  onNotificationClick: (data) => {
    console.log("[DEBUG] Notification clicked:", JSON.stringify(data, null, 2));
  },
  onNotificationReceived: (data) => {
    console.log(
      "[DEBUG] Notification received:",
      JSON.stringify(data, null, 2)
    );
  },
  onError: (error) => {
    console.error("[DEBUG] Notification error:", error);
  },
});
```

## Performance Considerations

### Memory Management

The notification system automatically manages memory by:

- Limiting active notifications (configurable via remote config)
- Cleaning up old notification data
- Using efficient image caching with Glide

### Battery Optimization

To minimize battery usage:

- Notifications are batched when possible
- Periodic refresh uses exact alarms only when necessary
- Image loading is optimized with caching

### Network Usage

- Images are cached after first load
- Blur transformations are cached
- Network requests are minimized through intelligent caching

## Migration from Android

If you're migrating from the existing Android notification system:

1. **Keep existing logic**: All Android logic is preserved exactly
2. **Update imports**: Change from Android imports to React Native imports
3. **Add event callbacks**: Set up analytics and deep linking callbacks
4. **Test thoroughly**: Verify all notification types work as expected

### Migration Checklist

- [ ] Install the React Native package
- [ ] Copy native Android files
- [ ] Update MainApplication.java
- [ ] Add permissions to AndroidManifest.xml
- [ ] Update build.gradle dependencies
- [ ] Set up event callbacks
- [ ] Test notification creation
- [ ] Test notification clicks
- [ ] Test periodic refresh
- [ ] Test on different devices
- [ ] Verify analytics integration
- [ ] Test deep linking

## Support

For issues and questions:

1. Check the troubleshooting section above
2. Review the Android implementation for reference
3. Check device-specific settings
4. Enable debug logging to identify issues

## License

MIT License - see LICENSE file for details.
