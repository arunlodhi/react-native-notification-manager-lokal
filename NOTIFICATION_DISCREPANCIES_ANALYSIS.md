# Notification Builder Logic Discrepancies Analysis

## Overview

After conducting a comprehensive comparison between the native Android notification implementation (`app/src/main/java/get/lokal/localnews/notification/NotificationUtil.java`) and the React Native port, I've identified several critical discrepancies that need to be addressed.

## Major Discrepancies Found

### 1. **Missing Advanced Notification Features**

#### A. Title Concatenation Logic (Version 4, 5, 6)

**Native Implementation:**

```java
private static String setTitleForNotificationVersion4And5(String title, String body, int notificationVersion, Context context){
    if(!body.isEmpty() && (notificationVersion == 4 || notificationVersion == 5 || notificationVersion == 6)){
        title = title.trim();
        if(checkForPunctuationMarksInTheEndTitle(title)){
            title = title + " " + body;
        }else{
            String language = LanguageUtils.getSelectedLanguageLocale(context, false);
            if (language.equals(Constants.BENGALI_LANGUAGE) || language.equals(Constants.HINDI_LANGUAGE)){
                title = title + "| " + body;
            }else{
                title = title + ". " + body;
            }
        }
    }
    return title;
}
```

**React Native Implementation:** ❌ **MISSING**

- No title concatenation logic for versions 4, 5, 6
- No language-specific punctuation handling
- No punctuation mark detection

#### B. Language-Specific Text Handling

**Native Implementation:**

```java
String languageLocale = getSelectedLanguageLocale(context, false);
boolean isTamilOrMalayalam = languageLocale.equals(Constants.TAMIL_LANGUAGE) || languageLocale.equals(Constants.MALAYALAM_LANGUAGE);

if (body.isEmpty()) {
    if (isTamilOrMalayalam) {
        notificationLayoutCollapsed.setInt(R.id.title, "setMaxLines", 3);
    } else {
        notificationLayoutCollapsed.setInt(R.id.title, "setMaxLines", 2);
    }
    notificationLayoutCollapsed.setViewVisibility(R.id.body, View.GONE);
}
```

**React Native Implementation:** ❌ **MISSING**

- No language-specific max lines handling
- No Tamil/Malayalam special handling
- No dynamic body visibility based on content

### 2. **Missing Notification Styles and Configurations**

#### A. DecoratedCustomViewStyle Usage

**Native Implementation:**

```java
NotificationCompat.Style style;
switch (notificationVersion) {
    case 1:
    case 2:
    case 3:
        style = new NotificationCompat.DecoratedCustomViewStyle();
        break;
    case 4:
        style = new NotificationCompat.DecoratedCustomViewStyle();
        break;
    case 5:
    case 6:
        style = null;
        break;
}
builder.setStyle(style);
```

**React Native Implementation:** ❌ **MISSING**

- No style configuration based on notification version
- Missing DecoratedCustomViewStyle for appropriate versions

#### B. Sound and Vibration Configuration

**Native Implementation:**

```java
Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
builder.setDefaults(Notification.DEFAULT_SOUND)
       .setSound(soundUri);
```

**React Native Implementation:** ❌ **PARTIALLY MISSING**

- Has `setDefaults(NotificationCompat.DEFAULT_SOUND)` but missing explicit sound URI
- No RingtoneManager integration

### 3. **Missing Advanced Priority and Category Logic**

#### A. High Priority Notification Handling

**Native Implementation:**

```java
if (!CommonNotificationUtils.isXiaomiDevice()) {
    boolean keepAtTop = SharedPrefUtil.getBoolean(context, NOTIFICATION_KEEP_AT_TOP, false);

    if (keepAtTop) {
        createCustomHighPriorityNotification(context, id, title, body, channel, pendingIntent,
                notificationLayoutCollapsed, notificationLayoutHeadsUp, notificationLayoutExpanded);
    } else {
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        postNotification(id, builder.build(), context);
    }
} else {
    // Special flow for Xiaomi devices
    builder.setShowWhen(false);
    builder.setPriority(NotificationCompat.PRIORITY_MAX);
    postNotification(id, builder.build(), context);
}
```

**React Native Implementation:** ❌ **MISSING**

- No device-specific handling (Xiaomi)
- No "keep at top" functionality
- No custom high priority notification creation
- Missing `setShowWhen(false)` for specific cases

#### B. MessagingStyle for High Priority

**Native Implementation:**

```java
NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getPackageName() + "_" + channel);
builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
       .addPerson(self)
       .setStyle(new NotificationCompat.MessagingStyle(self)
               .setConversationTitle(context.getString(R.string.app_name))
               .addMessage(LokalTextUtils.parseHtmlTags(body), System.currentTimeMillis(), otherPerson))
       .setGroup(UUID.randomUUID().toString())
       .setGroupSummary(false);
```

**React Native Implementation:** ❌ **MISSING**

- No MessagingStyle implementation
- No Person objects creation
- No conversation title setting

### 4. **Missing Notification Grouping Logic**

#### A. Grouping Deactivation Check

**Native Implementation:**

```java
if(!SharedPrefUtil.getBoolean(context, Constants.IS_NOTIFICATION_GROUPING_ACTIVE)){
    builder.setGroup(String.valueOf(id));
}
```

**React Native Implementation:** ❌ **MISSING**

- No check for notification grouping preference
- No individual notification grouping when grouping is disabled

#### B. Group Summary Notification Logic

**Native Implementation:**

```java
String str = SharedPrefUtil.getString(context, Constants.MATRIMONY_GROUPED_NOTIF_ID+groupID,"");
if(!TextUtils.isEmpty(str)){
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    StatusBarNotification[] activeNotificationsList = notificationManager.getActiveNotifications();
    boolean isPresent=false;
    for(StatusBarNotification activeNotification: activeNotificationsList){
        if(activeNotification.getId()==groupID){
            isPresent = true;
            break;
        }
    }
    if(!isPresent){
        str = "";
    }
}
str = TextUtils.isEmpty(str) ? String.valueOf(id) : (str + "," + id);
SharedPrefUtil.updateSharedPreferences(context,Constants.MATRIMONY_GROUPED_NOTIF_ID+groupID,str);
```

**React Native Implementation:** ❌ **MISSING**

- No active notification checking before creating group summary
- No persistent storage of grouped notification IDs
- Simplified group summary creation

### 5. **Missing Text Processing and Formatting**

#### A. HTML Tag Processing

**Native Implementation:**

```java
notificationLayoutExpanded.setTextViewText(R.id.title, LokalTextUtils.parseHtmlTags(title));
notificationLayoutExpanded.setTextViewText(R.id.body, LokalTextUtils.parseHtmlTags(body));
```

**React Native Implementation:** ❌ **SIMPLIFIED**

- Basic regex HTML removal vs comprehensive LokalTextUtils.parseHtmlTags()
- Missing advanced HTML processing

#### B. Text Size Handling

**Native Implementation:**

```java
if(notificationVersion == 5) {
    notificationLayoutCollapsed.setTextViewTextSize(R.id.title, TypedValue.COMPLEX_UNIT_PX,
            LokalTextUtils.getDimensionByLocalePx(context, R.dimen.notification_text_size_v5));
}
```

**React Native Implementation:** ❌ **MISSING**

- No dynamic text size setting based on version
- No locale-specific dimension handling

### 6. **Missing Notification Lifecycle Management**

#### A. Silent Push Handling

**Native Implementation:**

```java
if (NotificationReCreator.INSTANCE.isSilentPush()) {
    builder.setSound(null);
    builder.setWhen(System.currentTimeMillis());
    builder.setSilent(true);
} else {
    builder.setDefaults(Notification.DEFAULT_SOUND);
}
```

**React Native Implementation:** ❌ **MISSING**

- No silent push detection
- No conditional sound/vibration handling
- Missing `setSilent()` functionality

#### B. Notification Limiting

**Native Implementation:**

```java
private static void postNotification(int notificationId, Notification notification, Context context) {
    NotificationLimiter.INSTANCE.limitNotifications(context);
    NotificationManagerCompat.from(context).notify(notificationId, notification);
}
```

**React Native Implementation:** ❌ **MISSING**

- No notification limiting mechanism
- Direct notification posting without checks

## Recommendations

### Immediate Priority (Critical)

1. **Implement title concatenation logic** for versions 4, 5, 6
2. **Add language-specific text handling** (Tamil/Malayalam)
3. **Implement proper notification styles** based on version
4. **Add device-specific handling** (Xiaomi devices)

### High Priority

1. **Implement MessagingStyle** for high priority notifications
2. **Add notification grouping preference checks**
3. **Implement advanced HTML text processing**
4. **Add silent push handling**

### Medium Priority

1. **Implement notification limiting**
2. **Add locale-specific text sizing**
3. **Enhance group summary logic**
4. **Add sound URI configuration**

## Impact Assessment

**Current State:** The React Native implementation covers ~60% of the native functionality
**Missing Features:** ~40% of advanced notification features are not implemented
**Risk Level:** HIGH - Missing critical user experience features like language support and device-specific optimizations

## Next Steps

1. Prioritize implementation based on user impact
2. Create comprehensive test cases for each missing feature
3. Implement features
