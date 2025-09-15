# Notification String Resources Analysis - CORRECTED

## Overview

This analysis examines all XML layouts used by the notification builders and identifies string resources that should be replaced by the notification builder logic. **CORRECTED** after discovering that quiz layouts ARE being used in `NotificationManagerModule.java`.

## Layouts Used by Notification Builders

### NotificationUtil.java (Main notification builder)

Based on `NotificationUtil.java`, the following layouts are used by the main notification builder depending on the notification version:

### Version 1:

- **Large**: `notification_large_v1.xml`
- **Small**: `notification_small_v1.xml`
- **HeadsUp**: `notification_headsup_v1.xml`

### Version 2:

- **Large**: `notification_large_v2.xml`
- **Small**: `notification_small_v2.xml`
- **HeadsUp**: `notification_small_v2.xml` (reuses small)

### Version 3:

- **Large**: `notification_large_v3.xml`
- **Small**: `notification_small_v3.xml`
- **HeadsUp**: `notification_small_v3.xml` (reuses small)

### Version 4:

- **Large**: `notification_large_v4.xml`
- **Small**: `notification_small_v4.xml`
- **HeadsUp**: `notification_headsup_v4.xml`

### Version 5:

- **Large**: `notification_large_v5.xml`
- **Small**: `notification_small_v5.xml`
- **HeadsUp**: `notification_small_v5.xml` (reuses small)

### Version 6:

- **Large**: `notification_large_v7.xml`
- **Small**: `notification_small_v7.xml`
- **HeadsUp**: `notification_small_v7.xml` (reuses small)

### Default:

- **Large**: `notification_large.xml`
- **Small**: `notification_small.xml`
- **HeadsUp**: `notification_headsup.xml`

### NotificationManagerModule.java (Quiz notification builder)

Based on `NotificationManagerModule.java`, the following layouts are used by the quiz notification builder:

- **Large**: `notification_large_quiz.xml`
- **Small**: `notification_small_quiz.xml`
- **HeadsUp**: `notification_small_quiz.xml` (reuses small)

## String Resources Analysis

### Files and String Resources Found:

| File Name                       | String Resource                | Variable Name | Is Being Replaced by Notification Builder                                   |
| ------------------------------- | ------------------------------ | ------------- | --------------------------------------------------------------------------- |
| `notification_large_v1.xml`     | `@string/title`                | `title`       | ✅ **YES** - `remoteView.setTextViewText(R.id.title, parseHtmlTags(title))` |
| `notification_large_v1.xml`     | `@string/share`                | `share`       | ❌ **NO** - Static string, not replaced                                     |
| `notification_large_v1.xml`     | `@string/open_in_app`          | `open_in_app` | ❌ **NO** - Static string, not replaced                                     |
| `notification_small_v1.xml`     | `@string/title`                | `title`       | ✅ **YES** - `remoteView.setTextViewText(R.id.title, parseHtmlTags(title))` |
| `notification_headsup_v1.xml`   | `@string/title`                | `title`       | ✅ **YES** - `remoteView.setTextViewText(R.id.title, parseHtmlTags(title))` |
| `notification_large_v4.xml`     | `@string/title`                | `title`       | ✅ **YES** - `remoteView.setTextViewText(R.id.title, parseHtmlTags(title))` |
| `notification_large_v4.xml`     | `@string/description`          | `body`        | ✅ **YES** - `remoteView.setTextViewText(R.id.body, parseHtmlTags(body))`   |
| `notification_large_v7.xml`     | No string resources            | -             | -                                                                           |
| `notification_sticky_small.xml` | `@string/title`                | `title`       | ❌ **NO** - This layout is NOT used by notification builder                 |
| `notification_sticky_small.xml` | `@string/notification_refresh` | `label`       | ❌ **NO** - This layout is NOT used by notification builder                 |
| `notification_large_v2.xml`     | `@string/title`                | `title`       | ✅ **YES** - `remoteView.setTextViewText(R.id.title, parseHtmlTags(title))` |
| `notification_large_v2.xml`     | `@string/description` (tools)  | `body`        | ✅ **YES** - `remoteView.setTextViewText(R.id.body, parseHtmlTags(body))`   |
| `notification_large_quiz.xml`   | `@string/title`                | `title`       | ⚠️ **PROBLEM** - Used by `createQuizNotification` but strings NOT replaced  |
| `notification_large_quiz.xml`   | `@string/description`          | `body`        | ⚠️ **PROBLEM** - Used by `createQuizNotification` but strings NOT replaced  |
| `notification_small_quiz.xml`   | `@string/title`                | `title`       | ⚠️ **PROBLEM** - Used by `createQuizNotification` but strings NOT replaced  |
| `notification_small_quiz.xml`   | `@string/description`          | `body`        | ⚠️ **PROBLEM** - Used by `createQuizNotification` but strings NOT replaced  |

### Header Layout (included in all notifications):

| File Name                 | String Resource     | Variable Name   | Is Being Replaced by Notification Builder                                    |
| ------------------------- | ------------------- | --------------- | ---------------------------------------------------------------------------- |
| `notification_header.xml` | No string resources | `category_name` | ✅ **YES** - `remoteView.setTextViewText(R.id.category_name, categoryName)`  |
| `notification_header.xml` | No string resources | `time_stamp`    | ✅ **YES** - `remoteView.setTextViewText(R.id.time_stamp, getCurrentTime())` |

## Summary of Notification Builder Logic

The notification builder in `NotificationUtil.java` handles the following replacements:

### ✅ **REPLACED Variables:**

1. **`title`** - Replaced in all layouts via `remoteView.setTextViewText(R.id.title, parseHtmlTags(title))`
2. **`body`** - Replaced in layouts that have body text via `remoteView.setTextViewText(R.id.body, parseHtmlTags(body))`
3. **`category_name`** - Replaced in header via `remoteView.setTextViewText(R.id.category_name, categoryName)`
4. **`time_stamp`** - Replaced in header via `remoteView.setTextViewText(R.id.time_stamp, getCurrentTime())`

### ❌ **NOT REPLACED Variables:**

1. **`@string/share`** - Static string in `notification_large_v1.xml` and similar layouts
2. **`@string/open_in_app`** - Static string in `notification_large_v1.xml` and similar layouts
3. **`@string/notification_refresh`** - In `notification_sticky_small.xml` (layout not used anywhere)

### ⚠️ **PROBLEMATIC Variables (Used but NOT properly replaced):**

4. **`@string/title`** in quiz layouts - Used by `createQuizNotification` but XML still shows placeholder
5. **`@string/description`** in quiz layouts - Used by `createQuizNotification` but XML still shows placeholder

## Key Findings

### Files and Variables NOT Getting Replaced by Notification Builder:

#### **Used by Notification Builder:**

- **File**: `notification_large_v1.xml`
  - **Variable**: `@string/share` - Static string, not replaced
  - **Variable**: `@string/open_in_app` - Static string, not replaced

#### **NOT Used by Any Notification Builder:**

- **File**: `notification_sticky_small.xml`
  - **Variable**: `@string/title` - Not replaced (layout not used anywhere)
  - **Variable**: `@string/notification_refresh` - Not replaced (layout not used anywhere)

#### **⚠️ CRITICAL ISSUE - Used but Strings NOT Replaced:**

- **File**: `notification_large_quiz.xml` (Used by `NotificationManagerModule.createQuizNotification`)

  - **Variable**: `@string/title` - **PROBLEM**: Layout used but placeholder string not replaced
  - **Variable**: `@string/description` - **PROBLEM**: Layout used but placeholder string not replaced

- **File**: `notification_small_quiz.xml` (Used by `NotificationManagerModule.createQuizNotification`)
  - **Variable**: `@string/title` - **PROBLEM**: Layout used but placeholder string not replaced
  - **Variable**: `@string/description` - **PROBLEM**: Layout used but placeholder string not replaced

## Critical Issues Found

### **🚨 MAJOR PROBLEM: Quiz Layouts Show Placeholder Text**

The `createQuizNotification` method in `NotificationManagerModule.java` uses quiz layouts but **does NOT properly replace** the string resources. This means users will see placeholder text like "Title" and "Description" instead of actual notification content.

**Root Cause**: The quiz layouts contain `android:text="@string/title"` and `android:text="@string/description"` attributes, but the `createQuizNotificationInternal` method calls `setTextViewText()` which may not override these preset values properly.

## Recommendations

### **🔥 URGENT - Fix Quiz Notifications:**

1. **Remove** `android:text` attributes from quiz layout XML files, OR
2. **Fix** the `createQuizNotificationInternal` method to ensure dynamic text properly replaces placeholder text
3. **Test** quiz notifications to verify dynamic content is displayed

### **📋 Other Issues:**

1. **For main layouts**: The `@string/share` and `@string/open_in_app` strings should be made dynamic if they need localization or customization.

2. **For unused layout**: Consider removing `notification_sticky_small.xml` if it's truly obsolete.

3. **Missing body text**: Some layouts like `notification_large_v7.xml` don't have body text fields, which might be intentional for the design but should be verified.

## Summary

- **✅ Working properly**: Main notification layouts (v1-v7) via `NotificationUtil.java`
- **⚠️ Critical issue**: Quiz notification layouts show placeholder text instead of dynamic content
- **❌ Unused**: `notification_sticky_small.xml` layout is not used anywhere
- **❌ Static strings**: Share and open-in-app buttons use static text
