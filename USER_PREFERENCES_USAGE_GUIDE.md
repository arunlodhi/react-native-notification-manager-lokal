# UserPreferences Usage Guide

## Overview

The React Native notification manager now supports passing user preferences directly instead of relying on SharedPreferences. This allows for more flexible, predictable, and testable notification behavior with full language support.

## How to Pass UserPreferences from React Native

### 1. Import the Required Types and Functions

```typescript
import {
  createNotificationWithCustomLayout,
  UserPreferences,
} from "react-native-notification-manager-lokal";
```

### 2. Create UserPreferences Object

```typescript
// Define user preferences
const userPreferences: UserPreferences = {
  selectedLanguage: "ta", // Tamil language
  preferredLocale: "ta", // Tamil locale
  isNotificationGroupingActive: true,
  keepNotificationAtTop: false,
  isSilentPush: false,
};
```

### 3. Create Notification with UserPreferences

```typescript
// Basic usage with Tamil language support
await createNotificationWithCustomLayout(
  123, // id
  "செய்தி தலைப்பு", // title in Tamil
  "செய்தி விவரம்", // body in Tamil
  "1", // categoryId
  "செய்திகள்", // categoryName in Tamil
  "https://app.com/article/123", // uri
  "ACTION_PUSH", // action (will show share button)
  userPreferences, // User preferences object
  {
    // Optional parameters
    imageUrl: "https://example.com/image.jpg",
    channel: "Recommendation",
    importance: 4,
    notificationVersion: 6, // Uses advanced language handling
    isGroupingNeeded: true,
    groupID: 100,
    notifType: "article",
    isPersonalized: true,
  }
);
```

## Language-Specific Examples

### Tamil Language Notification

```typescript
const tamilPreferences: UserPreferences = {
  selectedLanguage: "ta",
  preferredLocale: "ta",
};

await createNotificationWithCustomLayout(
  1,
  "புதிய கட்டுரை", // New Article
  "இது ஒரு சுவாரஸ்யமான கட்டுரை", // This is an interesting article
  "1",
  "செய்திகள்", // News
  "https://app.com/article/1",
  "ACTION_PUSH",
  tamilPreferences,
  { notificationVersion: 6 } // Version 6 has special Tamil handling
);
```

### Bengali Language Notification

```typescript
const bengaliPreferences: UserPreferences = {
  selectedLanguage: "bn",
  preferredLocale: "bn",
};

await createNotificationWithCustomLayout(
  2,
  "নতুন নিবন্ধ", // New Article
  "এটি একটি আকর্ষণীয় নিবন্ধ", // This is an interesting article
  "1",
  "সংবাদ", // News
  "https://app.com/article/2",
  "ACTION_PUSH",
  bengaliPreferences,
  { notificationVersion: 5 } // Will use "|" for title concatenation
);
```

### Hindi Language Notification

```typescript
const hindiPreferences: UserPreferences = {
  selectedLanguage: "hi",
  preferredLocale: "hi",
};

await createNotificationWithCustomLayout(
  3,
  "नया लेख", // New Article
  "यह एक दिलचस्प लेख है", // This is an interesting article
  "1",
  "समाचार", // News
  "https://app.com/article/3",
  "ACTION_PUSH",
  hindiPreferences,
  { notificationVersion: 4 } // Will use "|" for title concatenation
);
```

### English Language Notification

```typescript
const englishPreferences: UserPreferences = {
  selectedLanguage: "en",
  preferredLocale: "en",
};

await createNotificationWithCustomLayout(
  4,
  "Breaking News",
  "Important update available",
  "1",
  "News",
  "https://app.com/article/4",
  "ACTION_PUSH",
  englishPreferences,
  { notificationVersion: 6 } // Will use "." for title concatenation
);
```

## Advanced Usage Examples

### Silent Push Notification

```typescript
const silentPreferences: UserPreferences = {
  selectedLanguage: "en",
  preferredLocale: "en",
  isSilentPush: true, // No sound or vibration
};

await createNotificationWithCustomLayout(
  5,
  "Background Update",
  "Data synchronized",
  "2",
  "System",
  "https://app.com/sync",
  "ACTION_SYNC",
  silentPreferences
);
```

### High Priority Notification (Keep at Top)

```typescript
const highPriorityPreferences: UserPreferences = {
  selectedLanguage: "en",
  preferredLocale: "en",
  keepNotificationAtTop: true, // Will use MessagingStyle for high priority
};

await createNotificationWithCustomLayout(
  6,
  "Urgent Alert",
  "Immediate attention required",
  "3",
  "Alerts",
  "https://app.com/alert/6",
  "ACTION_PUSH",
  highPriorityPreferences,
  { importance: 4 } // High importance
);
```

### Grouped Notifications

```typescript
const groupPreferences: UserPreferences = {
  selectedLanguage: "en",
  preferredLocale: "en",
  isNotificationGroupingActive: true,
};

// Create multiple notifications in the same group
for (let i = 7; i <= 10; i++) {
  await createNotificationWithCustomLayout(
    i,
    `Article ${i}`,
    `Content for article ${i}`,
    "1",
    "News",
    `https://app.com/article/${i}`,
    "ACTION_PUSH",
    groupPreferences,
    {
      isGroupingNeeded: true,
      groupID: 100, // Same group ID
    }
  );
}
```

### Per-User Language Preferences

```typescript
// Function to get user's language preferences
function getUserPreferences(userId: string): UserPreferences {
  // This could come from your app's user settings, AsyncStorage, etc.
  const userSettings = getUserSettings(userId);

  return {
    selectedLanguage: userSettings.language || "en",
    preferredLocale: userSettings.locale || "en",
    isNotificationGroupingActive: userSettings.groupNotifications ?? true,
    keepNotificationAtTop: userSettings.priorityNotifications ?? false,
    isSilentPush: false,
  };
}

// Usage
const userPrefs = getUserPreferences("user123");
await createNotificationWithCustomLayout(
  11,
  "Personalized Content",
  "Content tailored for you",
  "4",
  "Recommendations",
  "https://app.com/personalized/11",
  "ACTION_PUSH",
  userPrefs,
  { isPersonalized: true }
);
```

## Language Support

The system supports all the same languages as the native implementation:

| Language  | Code | Special Features                    |
| --------- | ---- | ----------------------------------- |
| English   | `en` | Default, uses "." for concatenation |
| Tamil     | `ta` | Extra lines for complex script      |
| Malayalam | `ml` | Extra lines for complex script      |
| Bengali   | `bn` | Uses "\|" for title concatenation   |
| Hindi     | `hi` | Uses "\|" for title concatenation   |
| Telugu    | `te` | Standard handling                   |
| Kannada   | `kn` | Standard handling                   |
| Marathi   | `mr` | Standard handling                   |
| Gujarati  | `gu` | Standard handling                   |
| Punjabi   | `pa` | Standard handling                   |

## UserPreferences Interface

```typescript
interface UserPreferences {
  selectedLanguage?: string; // Default: "en"
  preferredLocale?: string; // Default: "none"
  isNotificationGroupingActive?: boolean; // Default: true
  keepNotificationAtTop?: boolean; // Default: false
  isSilentPush?: boolean; // Default: false
}
```

## Benefits

### 1. **No SharedPreferences Dependency**

- All preferences passed directly in notification calls
- More predictable and testable
- No need to manage SharedPreferences state

### 2. **Per-Notification Customization**

- Different language settings per notification
- User-specific preferences
- Context-aware notifications

### 3. **Complete Native Feature Parity**

- Title concatenation with language-specific punctuation
- Tamil/Malayalam special text layout handling
- Device-specific optimizations (Xiaomi)
- Advanced grouping and priority logic

### 4. **Type Safety**

- Full TypeScript support
- Compile-time validation
- Clear API with IntelliSense support

## Migration from Old API

### Before (using SharedPreferences)

```typescript
// Old way - had to manage SharedPreferences separately
await AsyncStorage.setItem('selected_language', 'ta');
await createNotificationWithImage(id, imageUrl, title, body, ...);
```

### After (using UserPreferences)

```typescript
// New way - pass preferences directly
const userPrefs: UserPreferences = { selectedLanguage: "ta" };
await createNotificationWithCustomLayout(
  id,
  title,
  body,
  categoryId,
  categoryName,
  uri,
  action,
  userPrefs,
  { imageUrl }
);
```

This approach provides a cleaner, more flexible API while maintaining complete compatibility with the native Android notification functionality.
