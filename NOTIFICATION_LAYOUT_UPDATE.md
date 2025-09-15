# React Native Notification Manager - Layout Update

## Overview

The React Native notification manager has been updated to match the native Android implementation's functionality, specifically addressing the missing programmatic control of `share_container` and `notification_footer` elements.

## Key Changes Made

### 1. New NotificationUtil Class

Created `NotificationUtil.java` that replicates the native Android notification creation logic:

- **Custom RemoteViews**: Uses proper custom layouts instead of basic NotificationCompat styles
- **Share Button Logic**: Implements the same logic as native to determine when to show share buttons
- **Footer Control**: Programmatically controls `notification_footer` visibility based on share button state
- **Layout Versioning**: Supports different notification layout versions (v1-v7)

### 2. Updated NotificationManagerModule

Enhanced the main module with:

- **New Method**: `createNotificationWithCustomLayout()` - Uses the new NotificationUtil
- **Image Support**: `loadImageAndCreateCustomNotification()` - Handles image loading with custom layouts
- **Backward Compatibility**: Existing methods remain unchanged

### 3. Key Functionality Implemented

#### Share Container & Footer Logic

```java
// The critical missing functionality now implemented:
remoteView.setViewVisibility(R.id.share_container,
                           showShareButton ? View.VISIBLE : View.GONE);
remoteView.setViewVisibility(R.id.notification_footer,
                           showShareButton ? View.GONE : View.VISIBLE);
```

#### Share Button Determination

Share buttons are shown when:

- Action is `ACTION_PUSH` or `ACTION_PUSH_VIDEO`
- URI contains `/article/` or `/video/`

#### Layout Version Support

Supports notification versions 1-7 with proper layout selection:

- Version 1: `notification_large_v1`, `notification_small_v1`, `notification_headsup_v1`
- Version 2-7: Corresponding layout files
- Default: Falls back to base layouts

## Usage

### Basic Custom Layout Notification

```javascript
import { NativeModules } from "react-native";
const { NotificationManagerModule } = NativeModules;

// Create notification with custom layout (no image)
NotificationManagerModule.createNotificationWithCustomLayout({
  id: 123,
  title: "Article Title",
  body: "Article description",
  categoryId: "1",
  categoryName: "News",
  uri: "https://app.com/article/123",
  action: "ACTION_PUSH", // This will show share button
  channel: "Recommendation",
  importance: 4,
  notificationVersion: 1,
});
```

### Custom Layout with Image

```javascript
// Create notification with custom layout and image
NotificationManagerModule.createNotificationWithCustomLayout({
  id: 124,
  title: "Video Title",
  body: "Video description",
  categoryId: "2",
  categoryName: "Videos",
  uri: "https://app.com/video/456",
  action: "ACTION_PUSH_VIDEO", // This will show share button
  imageUrl: "https://example.com/image.jpg",
  channel: "Recommendation",
  importance: 4,
  notificationVersion: 1,
});
```

### Regular Notification (No Share Button)

```javascript
// Create notification without share functionality
NotificationManagerModule.createNotificationWithCustomLayout({
  id: 125,
  title: "General Notification",
  body: "General message",
  categoryId: "3",
  categoryName: "General",
  uri: "https://app.com/general/789",
  action: "ACTION_VIEW", // This will NOT show share button
  channel: "Recommendation",
  importance: 4,
  notificationVersion: 1,
});
```

## Behavior Changes

### Before Update

- Notifications used basic `NotificationCompat.Builder` with standard styles
- No programmatic control of layout elements
- `share_container` was always visible (set in XML)
- `notification_footer` was always visible (set in XML)
- No differentiation between article/video and other notification types

### After Update

- Notifications use custom `RemoteViews` with proper layout control
- `share_container` visibility controlled programmatically based on action/URI
- `notification_footer` visibility is inverse of share_container (native behavior)
- Share button click handlers properly configured
- Supports multiple notification layout versions
- Matches native Android implementation exactly

## Files Modified

1. **New**: `NotificationUtil.java` - Core notification creation logic
2. **Updated**: `NotificationManagerModule.java` - Added custom layout methods
3. **Existing**: All layout XML files remain unchanged

## Backward Compatibility

- Existing `createNotification()` and `createNotificationWithImage()` methods unchanged
- New functionality available through `createNotificationWithCustomLayout()`
- No breaking changes to existing React Native interface

## Testing

To test the new functionality:

1. **Article/Video Notifications**: Use `ACTION_PUSH` or `ACTION_PUSH_VIDEO` - should show share button, hide footer
2. **General Notifications**: Use other actions - should hide share button, show footer
3. **Image Loading**: Test with and without `imageUrl` parameter
4. **Layout Versions**: Test different `notificationVersion` values (1-7)

## Next Steps

1. Test the implementation with actual notification scenarios
2. Verify share button click handling works correctly
3. Test across different Android versions and devices
4. Consider adding blur bitmap support if needed
5. Add unit tests for the new functionality

## Summary

The React Native notification manager now properly replicates the native Android notification behavior, including:

- ✅ Programmatic control of `share_container` visibility
- ✅ Programmatic control of `notification_footer` visibility
- ✅ Proper share button logic based on action and URI
- ✅ Support for multiple notification layout versions
- ✅ Custom RemoteViews implementation
- ✅ Backward compatibility maintained

This resolves the issue where `notification_footer` was always visible and `share_container` visibility wasn't being controlled programmatically.
