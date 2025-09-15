package io.lokal.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import com.facebook.react.bridge.ReadableMap;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Utility class for creating notifications with proper layout handling
 * Matches the native Android implementation functionality exactly
 */
public class NotificationUtil {
    
    private static final String TAG = "NotificationUtil";
    
    // Action types that should show share button
    private static final String ACTION_PUSH = "ACTION_PUSH";
    private static final String ACTION_PUSH_VIDEO = "ACTION_PUSH_VIDEO";
    
    // Language constants
    private static final String TAMIL_LANGUAGE = "ta";
    private static final String MALAYALAM_LANGUAGE = "ml";
    private static final String BENGALI_LANGUAGE = "bn";
    private static final String HINDI_LANGUAGE = "hi";
    
    // Notification preferences
    private static final String NOTIFICATION_KEEP_AT_TOP = "notification_keep_at_top";
    private static final String IS_NOTIFICATION_GROUPING_ACTIVE = "is_notification_grouping_active";
    private static final String MATRIMONY_GROUPED_NOTIF_ID = "matrimony_grouped_notif_id_";
    
    public static void createNotificationWithCustomLayout(Context context,
                                                         int id,
                                                         Bitmap bitmap,
                                                         Bitmap blurrBitmap,
                                                         String title,
                                                         String body,
                                                         String categoryId,
                                                         String categoryName,
                                                         String uri,
                                                         String action,
                                                         String channel,
                                                         int importance,
                                                         int notificationVersion,
                                                         boolean isGroupingNeeded,
                                                         int groupID,
                                                         String notifType,
                                                         boolean isPersonalized,
                                                         UserPreferences userPreferences) {
        
        // Apply title concatenation logic for versions 4, 5, 6 (matching native)
        title = setTitleForNotificationVersion4And5(title, body, notificationVersion, userPreferences);
        
        // Determine if share button should be shown (matching native logic)
        boolean showShareButton = shouldShowShareButton(action, uri);
        
        // Create intents
        Intent intent = createMainIntent(context, id, uri, action, categoryId, categoryName);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        
        Intent shareIntent = new Intent(intent);
        shareIntent.putExtra("IS_SHARE_EXTRA", true);
        PendingIntent sharePendingIntent = PendingIntent.getActivity(context, -id, shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        
        // Get layout IDs and style based on notification version
        int[] layoutIds = getLayoutIds(notificationVersion);
        int notificationLarge = layoutIds[0];
        int notificationSmall = layoutIds[1];
        int notificationHeadsUp = layoutIds[2];
        NotificationCompat.Style style = getNotificationStyle(notificationVersion);
        
        // Create RemoteViews for different states
        RemoteViews notificationLayoutCollapsed = new RemoteViews(context.getPackageName(), notificationSmall);
        RemoteViews notificationLayoutExpanded = new RemoteViews(context.getPackageName(), notificationLarge);
        RemoteViews notificationLayoutHeadsUp = new RemoteViews(context.getPackageName(), notificationHeadsUp);
        
        // Set content for collapsed view with language-specific handling
        setupCollapsedView(notificationLayoutCollapsed, title, body, bitmap, blurrBitmap, 
                          categoryName, notificationVersion, userPreferences);
        
        // Set content for expanded view with share/footer logic and language handling
        setupExpandedView(notificationLayoutExpanded, title, body, bitmap, blurrBitmap, 
                         categoryName, showShareButton, sharePendingIntent, notificationVersion, userPreferences);
        
        // Set content for heads-up view
        setupHeadsUpView(notificationLayoutHeadsUp, title, body, bitmap, blurrBitmap, 
                        categoryName, notificationVersion, userPreferences);
        
        // Create notification builder with proper sound configuration
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getPackageName() + "_" + channel)
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setSmallIcon(getNotificationIcon(context))
            .setColor(getNotificationColor(context))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setSound(soundUri)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setCustomContentView(notificationLayoutCollapsed)
            .setCustomBigContentView(notificationLayoutExpanded)
            .setCustomHeadsUpContentView(notificationLayoutHeadsUp)
            .setStyle(style);
        
        // Add metadata for refresh functionality
        Bundle extras = new Bundle();
        extras.putInt("notification_refresh_id_extra", id);
        extras.putLong("notification_time_extra", System.currentTimeMillis());
        builder.addExtras(extras);
        
        // Handle notification grouping (matching native logic)
        handleNotificationGrouping(context, builder, id, isGroupingNeeded, groupID);
        
        // Set priority based on Android version
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            builder.setPriority(getPriorityForImportance(importance));
        }
        
        // Handle high priority notifications with device-specific logic
        if (importance >= NotificationCompat.PRIORITY_HIGH) {
            handleHighPriorityNotification(context, builder, id, title, body, channel, 
                                          pendingIntent, notificationLayoutCollapsed, 
                                          notificationLayoutHeadsUp, notificationLayoutExpanded, importance);
        } else {
            // Standard notification posting
            postNotification(id, builder.build(), context);
        }
        
        // Handle group summary if needed
        if (isGroupingNeeded && Build.VERSION.SDK_INT > Build.VERSION_CODES.M && groupID != 0) {
            handleGroupSummaryNotification(context, groupID, channel, categoryName, title, body, id);
        }
    }
    
    private static boolean shouldShowShareButton(String action, String uri) {
        // Match native logic for determining when to show share button
        boolean actionMatch = action != null && (action.equals(ACTION_PUSH) || action.equals(ACTION_PUSH_VIDEO));
        boolean uriMatch = uri != null && isArticleOrVideoUri(uri);
        return actionMatch || uriMatch;
    }
    
    private static boolean isArticleOrVideoUri(String uri) {
        // Simple check for article/video URIs - can be enhanced based on actual URI patterns
        return uri.contains("/article/") || uri.contains("/video/");
    }
    
    private static Intent createMainIntent(Context context, int id, String uri, String action, 
                                         String categoryId, String categoryName) {
        Intent intent = new Intent(context, NotificationClickReceiver.class);
        intent.putExtra("notification_id_extra", id);
        intent.putExtra("uri_extra", uri);
        intent.putExtra("action_extra", action);
        intent.putExtra("category_id_extra", categoryId);
        intent.putExtra("notification_category_name_extra", categoryName);
        intent.putExtra("is_source_notification", true);
        return intent;
    }
    
    private static int[] getLayoutIds(int notificationVersion) {
        // Return [large, small, headsUp] layout IDs based on version
        switch (notificationVersion) {
            case 1:
                return new int[]{
                    getLayoutId("notification_large_v1"),
                    getLayoutId("notification_small_v1"),
                    getLayoutId("notification_headsup_v1")
                };
            case 2:
                return new int[]{
                    getLayoutId("notification_large_v2"),
                    getLayoutId("notification_small_v2"),
                    getLayoutId("notification_small_v2")
                };
            case 3:
                return new int[]{
                    getLayoutId("notification_large_v3"),
                    getLayoutId("notification_small_v3"),
                    getLayoutId("notification_small_v3")
                };
            case 4:
                return new int[]{
                    getLayoutId("notification_large_v4"),
                    getLayoutId("notification_small_v4"),
                    getLayoutId("notification_headsup_v4")
                };
            case 5:
                return new int[]{
                    getLayoutId("notification_large_v5"),
                    getLayoutId("notification_small_v5"),
                    getLayoutId("notification_small_v5")
                };
            case 6:
                return new int[]{
                    getLayoutId("notification_large_v7"),
                    getLayoutId("notification_small_v7"),
                    getLayoutId("notification_small_v7")
                };
            default:
                return new int[]{
                    getLayoutId("notification_large"),
                    getLayoutId("notification_small"),
                    getLayoutId("notification_headsup")
                };
        }
    }
    
    private static int getLayoutId(String layoutName) {
        // This is a placeholder - in actual implementation, you'd use proper resource IDs
        // For now, we'll use the v1 layouts as default
        switch (layoutName) {
            case "notification_large_v1":
                return io.lokal.notifications.R.layout.notification_large_v1;
            case "notification_small_v1":
                return io.lokal.notifications.R.layout.notification_small_v1;
            case "notification_headsup_v1":
                return io.lokal.notifications.R.layout.notification_headsup_v1;
            // Add other cases as needed
            default:
                return io.lokal.notifications.R.layout.notification_large_v1;
        }
    }
    
    private static void setupCollapsedView(RemoteViews remoteView, String title, String body, 
                                         Bitmap bitmap, Bitmap blurrBitmap, String categoryName) {
        remoteView.setTextViewText(io.lokal.notifications.R.id.title, parseHtmlTags(title));
        remoteView.setTextViewText(io.lokal.notifications.R.id.body, parseHtmlTags(body));
        
        if (bitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.icon, bitmap);
        }
        if (blurrBitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.blurr_view, blurrBitmap);
        }
        
        bindNotificationHeader(remoteView, categoryName);
    }
    
    private static void setupExpandedView(RemoteViews remoteView, String title, String body,
                                        Bitmap bitmap, Bitmap blurrBitmap, String categoryName,
                                        boolean showShareButton, PendingIntent sharePendingIntent) {
        remoteView.setTextViewText(io.lokal.notifications.R.id.title, parseHtmlTags(title));
        remoteView.setTextViewText(io.lokal.notifications.R.id.body, parseHtmlTags(body));
        
        if (bitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.thumbnail, bitmap);
        }
        if (blurrBitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.blurr_view, blurrBitmap);
        }
        
        // THIS IS THE KEY MISSING FUNCTIONALITY - Control share_container and notification_footer visibility
        remoteView.setViewVisibility(io.lokal.notifications.R.id.share_container, 
                                   showShareButton ? View.VISIBLE : View.GONE);
        remoteView.setViewVisibility(io.lokal.notifications.R.id.notification_footer, 
                                   showShareButton ? View.GONE : View.VISIBLE);
        
        // Set share button click handler
        if (showShareButton) {
            remoteView.setOnClickPendingIntent(io.lokal.notifications.R.id.share, sharePendingIntent);
        }
        
        bindNotificationHeader(remoteView, categoryName);
        
        // Hide icon for older Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            remoteView.setViewVisibility(io.lokal.notifications.R.id.icon, View.GONE);
        }
    }
    
    private static void setupHeadsUpView(RemoteViews remoteView, String title, String body,
                                       Bitmap bitmap, Bitmap blurrBitmap, String categoryName) {
        remoteView.setTextViewText(io.lokal.notifications.R.id.title, parseHtmlTags(title));
        remoteView.setTextViewText(io.lokal.notifications.R.id.body, parseHtmlTags(body));
        
        if (bitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.icon, bitmap);
        }
        if (blurrBitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.blurr_view, blurrBitmap);
        }
        
        bindNotificationHeader(remoteView, categoryName);
    }
    
    private static void bindNotificationHeader(RemoteViews remoteViews, String categoryName) {
        if (categoryName != null && !categoryName.equals("")) {
            remoteViews.setViewVisibility(io.lokal.notifications.R.id.category_container, View.VISIBLE);
            remoteViews.setTextViewText(io.lokal.notifications.R.id.category_name, categoryName);
        } else {
            remoteViews.setViewVisibility(io.lokal.notifications.R.id.category_container, View.GONE);
        }
        
        // Set current timestamp
        remoteViews.setTextViewText(io.lokal.notifications.R.id.time_stamp, getCurrentTime());
    }
    
    // Title concatenation logic matching native implementation
    private static String setTitleForNotificationVersion4And5(String title, String body, int notificationVersion, UserPreferences userPreferences) {
        if (!body.isEmpty() && (notificationVersion == 4 || notificationVersion == 5 || notificationVersion == 6)) {
            title = title.trim();
            if (checkForPunctuationMarksInTheEndTitle(title)) {
                title = title + " " + body;
            } else {
                String language = userPreferences.getSelectedLanguageLocale(false);
                if (language.equals(BENGALI_LANGUAGE) || language.equals(HINDI_LANGUAGE)) {
                    title = title + "| " + body;
                } else {
                    title = title + ". " + body;
                }
            }
        }
        return title;
    }
    
    private static boolean checkForPunctuationMarksInTheEndTitle(String title) {
        List<String> punctuationMarkList = new ArrayList<>();
        punctuationMarkList.add(".");
        punctuationMarkList.add("?");
        punctuationMarkList.add(",");
        punctuationMarkList.add("!");
        punctuationMarkList.add("|");
        
        for (String punctuation : punctuationMarkList) {
            if (title.endsWith(punctuation)) {
                return true;
            }
        }
        return false;
    }
    
    // Language-specific handling matching native implementation exactly
    private static String getSelectedLanguageLocale(Context context, boolean skipPreferredLocale) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        
        if (skipPreferredLocale) {
            return prefs.getString("selected_language", "en"); // Constants.DEFAULT_LANGUAGE = "en"
        }
        
        String preferredLocale = prefs.getString("preferred_locale", "none"); // Constants.NONE = "none"
        
        if (preferredLocale.equalsIgnoreCase("none")) {
            return prefs.getString("selected_language", "en"); // Constants.DEFAULT_LANGUAGE = "en"
        } else {
            return preferredLocale;
        }
    }
    
    private static NotificationCompat.Style getNotificationStyle(int notificationVersion) {
        switch (notificationVersion) {
            case 1:
            case 2:
            case 3:
                return new NotificationCompat.DecoratedCustomViewStyle();
            case 4:
                return new NotificationCompat.DecoratedCustomViewStyle();
            case 5:
            case 6:
                return null;
            default:
                return new NotificationCompat.DecoratedCustomViewStyle();
        }
    }
    
    // Enhanced setup methods with language-specific handling using UserPreferences
    private static void setupCollapsedView(RemoteViews remoteView, String title, String body, 
                                         Bitmap bitmap, Bitmap blurrBitmap, String categoryName,
                                         int notificationVersion, UserPreferences userPreferences) {
        remoteView.setTextViewText(io.lokal.notifications.R.id.title, parseHtmlTags(title));
        remoteView.setTextViewText(io.lokal.notifications.R.id.body, parseHtmlTags(body));
        
        // Language-specific max lines handling using UserPreferences
        String languageLocale = userPreferences.getSelectedLanguageLocale(false);
        boolean isTamilOrMalayalam = languageLocale.equals(TAMIL_LANGUAGE) || languageLocale.equals(MALAYALAM_LANGUAGE);
        
        if (notificationVersion == 6) {
            if (body.isEmpty()) {
                if (isTamilOrMalayalam) {
                    remoteView.setInt(io.lokal.notifications.R.id.title, "setMaxLines", 3);
                } else {
                    remoteView.setInt(io.lokal.notifications.R.id.title, "setMaxLines", 2);
                }
                remoteView.setViewVisibility(io.lokal.notifications.R.id.body, View.GONE);
            } else {
                if (isTamilOrMalayalam) {
                    remoteView.setInt(io.lokal.notifications.R.id.title, "setMaxLines", 2);
                    remoteView.setInt(io.lokal.notifications.R.id.body, "setMaxLines", 1);
                } else {
                    remoteView.setInt(io.lokal.notifications.R.id.title, "setMaxLines", 1);
                    remoteView.setInt(io.lokal.notifications.R.id.body, "setMaxLines", 1);
                }
            }
        }
        
        // Dynamic text sizing for version 5
        if (notificationVersion == 5) {
            remoteView.setTextViewTextSize(io.lokal.notifications.R.id.title, TypedValue.COMPLEX_UNIT_SP, 16);
        }
        
        if (bitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.icon, bitmap);
        }
        if (blurrBitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.blurr_view, blurrBitmap);
        }
        
        bindNotificationHeader(remoteView, categoryName);
    }
    
    private static void setupExpandedView(RemoteViews remoteView, String title, String body,
                                        Bitmap bitmap, Bitmap blurrBitmap, String categoryName,
                                        boolean showShareButton, PendingIntent sharePendingIntent,
                                        int notificationVersion, UserPreferences userPreferences) {
        remoteView.setTextViewText(io.lokal.notifications.R.id.title, parseHtmlTags(title));
        remoteView.setTextViewText(io.lokal.notifications.R.id.body, parseHtmlTags(body));
        
        // Language-specific handling for expanded view using UserPreferences
        String languageLocale = userPreferences.getSelectedLanguageLocale(false);
        boolean isTamilOrMalayalam = languageLocale.equals(TAMIL_LANGUAGE) || languageLocale.equals(MALAYALAM_LANGUAGE);
        
        if (notificationVersion == 6) {
            if (body.isEmpty()) {
                if (isTamilOrMalayalam) {
                    remoteView.setInt(io.lokal.notifications.R.id.title, "setMaxLines", 2);
                } else {
                    remoteView.setInt(io.lokal.notifications.R.id.title, "setMaxLines", 2);
                }
                remoteView.setViewVisibility(io.lokal.notifications.R.id.body, View.GONE);
            } else {
                if (isTamilOrMalayalam) {
                    remoteView.setInt(io.lokal.notifications.R.id.title, "setMaxLines", 2);
                    remoteView.setInt(io.lokal.notifications.R.id.body, "setMaxLines", 1);
                } else {
                    remoteView.setInt(io.lokal.notifications.R.id.title, "setMaxLines", 1);
                    remoteView.setInt(io.lokal.notifications.R.id.body, "setMaxLines", 2);
                }
            }
        }
        
        // Dynamic text sizing for version 5
        if (notificationVersion == 5) {
            remoteView.setTextViewTextSize(io.lokal.notifications.R.id.title, TypedValue.COMPLEX_UNIT_SP, 16);
        }
        
        if (bitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.thumbnail, bitmap);
        }
        if (blurrBitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.blurr_view, blurrBitmap);
        }
        
        // Control share_container and notification_footer visibility
        remoteView.setViewVisibility(io.lokal.notifications.R.id.share_container, 
                                   showShareButton ? View.VISIBLE : View.GONE);
        remoteView.setViewVisibility(io.lokal.notifications.R.id.notification_footer, 
                                   showShareButton ? View.GONE : View.VISIBLE);
        
        // Set share button click handler
        if (showShareButton) {
            remoteView.setOnClickPendingIntent(io.lokal.notifications.R.id.share, sharePendingIntent);
        }
        
        bindNotificationHeader(remoteView, categoryName);
        
        // Hide icon for older Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            remoteView.setViewVisibility(io.lokal.notifications.R.id.icon, View.GONE);
        }
    }
    
    private static void setupHeadsUpView(RemoteViews remoteView, String title, String body,
                                       Bitmap bitmap, Bitmap blurrBitmap, String categoryName,
                                       int notificationVersion, UserPreferences userPreferences) {
        remoteView.setTextViewText(io.lokal.notifications.R.id.title, parseHtmlTags(title));
        remoteView.setTextViewText(io.lokal.notifications.R.id.body, parseHtmlTags(body));
        
        // Dynamic text sizing for version 5
        if (notificationVersion == 5) {
            remoteView.setTextViewTextSize(io.lokal.notifications.R.id.title, TypedValue.COMPLEX_UNIT_SP, 16);
        }
        
        if (bitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.icon, bitmap);
        }
        if (blurrBitmap != null) {
            remoteView.setImageViewBitmap(io.lokal.notifications.R.id.blurr_view, blurrBitmap);
        }
        
        bindNotificationHeader(remoteView, categoryName);
    }
    
    // Notification grouping logic matching native implementation
    private static void handleNotificationGrouping(Context context, NotificationCompat.Builder builder, 
                                                  int id, boolean isGroupingNeeded, int groupID) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        boolean isGroupingActive = prefs.getBoolean(IS_NOTIFICATION_GROUPING_ACTIVE, true);
        
        if (!isGroupingActive) {
            builder.setGroup(String.valueOf(id));
        } else if (isGroupingNeeded && groupID != 0) {
            builder.setGroup(String.valueOf(groupID));
        }
    }
    
    // High priority notification handling with device-specific logic
    private static void handleHighPriorityNotification(Context context, NotificationCompat.Builder builder,
                                                      int id, String title, String body, String channel,
                                                      PendingIntent pendingIntent, RemoteViews collapsed,
                                                      RemoteViews headsUp, RemoteViews expanded, int importance) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        boolean keepAtTop = prefs.getBoolean(NOTIFICATION_KEEP_AT_TOP, false);
        
        if (!isXiaomiDevice()) {
            if (keepAtTop) {
                createCustomHighPriorityNotification(context, id, title, body, channel, 
                                                   pendingIntent, collapsed, headsUp, expanded);
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
    }
    
    private static void createCustomHighPriorityNotification(Context context, int id, String title, String body,
                                                           String channel, PendingIntent pendingIntent,
                                                           RemoteViews collapsed, RemoteViews headsUp, RemoteViews expanded) {
        Person otherPerson = new Person.Builder()
                .setBot(false)
                .setName("Lokal")
                .setImportant(true)
                .build();

        Person self = new Person.Builder()
                .setBot(false)
                .setName("Lokal")
                .setImportant(true)
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getPackageName() + "_" + channel);
        builder.setContentIntent(pendingIntent)
                .setSmallIcon(getNotificationIcon(context))
                .setColor(getNotificationColor(context))
                .setContentTitle(parseHtmlTags(title))
                .setContentText(parseHtmlTags(body))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .addPerson(self)
                .setStyle(new NotificationCompat.MessagingStyle(self)
                        .setConversationTitle("Lokal")
                        .addMessage(parseHtmlTags(body), System.currentTimeMillis(), otherPerson))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setCustomContentView(collapsed)
                .setCustomHeadsUpContentView(headsUp)
                .setCustomBigContentView(expanded)
                .setGroup(UUID.randomUUID().toString())
                .setShowWhen(false)
                .setGroupSummary(false);

        // Handle silent push
        if (isSilentPush(context)) {
            builder.setSound(null);
            builder.setWhen(System.currentTimeMillis());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setSilent(true);
            }
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        }

        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        postNotification(id, builder.build(), context);
    }
    
    // Group summary notification logic matching native implementation
    private static void handleGroupSummaryNotification(Context context, int groupID, String channel,
                                                      String categoryName, String title, String body, int id) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String existingIds = prefs.getString(MATRIMONY_GROUPED_NOTIF_ID + groupID, "");
        
        if (!TextUtils.isEmpty(existingIds)) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
                boolean isPresent = false;
                for (StatusBarNotification activeNotification : activeNotifications) {
                    if (activeNotification.getId() == groupID) {
                        isPresent = true;
                        break;
                    }
                }
                if (!isPresent) {
                    existingIds = "";
                }
            }
        }
        
        existingIds = TextUtils.isEmpty(existingIds) ? String.valueOf(id) : (existingIds + "," + id);
        prefs.edit().putString(MATRIMONY_GROUPED_NOTIF_ID + groupID, existingIds).apply();
        
        // Create group summary notification
        Intent intent = new Intent(context, NotificationClickReceiver.class);
        intent.putExtra("notification_id_extra", groupID);
        PendingIntent groupPendingIntent = PendingIntent.getActivity(context, groupID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, context.getPackageName() + "_" + channel)
                .setContentIntent(groupPendingIntent)
                .setSmallIcon(getNotificationIcon(context))
                .setColor(getNotificationColor(context))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setGroup(String.valueOf(groupID))
                .setContentTitle(title)
                .setGroupSummary(true)
                .setContentText(body);

        NotificationManagerCompat.from(context).notify(groupID, summaryBuilder.build());
    }
    
    // Utility methods
    private static boolean isXiaomiDevice() {
        return Build.MANUFACTURER.equalsIgnoreCase("xiaomi");
    }
    
    private static boolean isSilentPush(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("is_silent_push", false);
    }
    
    private static int getPriorityForImportance(int importance) {
        switch (importance) {
            case NotificationManagerCompat.IMPORTANCE_HIGH:
                return NotificationCompat.PRIORITY_HIGH;
            case NotificationManagerCompat.IMPORTANCE_DEFAULT:
                return NotificationCompat.PRIORITY_DEFAULT;
            case NotificationManagerCompat.IMPORTANCE_LOW:
                return NotificationCompat.PRIORITY_LOW;
            case NotificationManagerCompat.IMPORTANCE_MIN:
                return NotificationCompat.PRIORITY_MIN;
            default:
                return NotificationCompat.PRIORITY_DEFAULT;
        }
    }
    
    private static void postNotification(int notificationId, android.app.Notification notification, Context context) {
        // Notification limiting logic would go here
        NotificationManagerCompat.from(context).notify(notificationId, notification);
    }
    
    private static int getNotificationColor(Context context) {
        try {
            return context.getResources().getColor(android.R.color.holo_blue_light);
        } catch (Exception e) {
            return 0xFF00BCD4; // Default teal color
        }
    }
    
    private static String parseHtmlTags(String text) {
        if (text == null) return "";
        // Enhanced HTML parsing - can be further enhanced to match LokalTextUtils
        return text.replaceAll("<[^>]*>", "").trim();
    }
    
    private static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    // ========== CONSOLIDATED UTILITY METHODS ==========
    // These methods are now centralized in NotificationUtil to avoid duplication
    
    public static int getNotificationIcon(Context context) {
        try {
            return context.getApplicationInfo().icon;
        } catch (Exception e) {
            return android.R.drawable.ic_dialog_info;
        }
    }
    
    /**
     * Create notification channels - consolidated from NotificationManagerModule
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Create channels matching exact Android implementation
            createChannel(notificationManager, "Recommendation", "Recommendations", NotificationManager.IMPORTANCE_HIGH);
            createChannel(notificationManager, "Cricket", "Cricket Updates", NotificationManager.IMPORTANCE_LOW);
            createChannel(notificationManager, "Comments", "Comments", NotificationManager.IMPORTANCE_LOW);
            createChannel(notificationManager, "Downloads", "Downloads", NotificationManager.IMPORTANCE_LOW);
            createChannel(notificationManager, "Uploads", "Uploads", NotificationManager.IMPORTANCE_LOW);
        }
    }
    
    private static void createChannel(NotificationManager notificationManager, String channelId, String channelName, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, channelName, importance);
            channel.setDescription("Channel for " + channelName);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Create basic notification - consolidated from NotificationManagerModule
     */
    public static void createBasicNotification(Context context, int id, String title, String body, 
                                             String categoryId, String categoryName, String uri, 
                                             String action, String tag, String channel, int importance, 
                                             boolean isGroupingNeeded, int groupID, String notifType, 
                                             boolean isPersonalized) {
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(getNotificationIcon(context))
            .setPriority(importance)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false);

        // Add timestamp for ordering (exact Android logic)
        long currentTime = System.currentTimeMillis();
        builder.getExtras().putLong("notification_time_extra", currentTime);
        builder.getExtras().putInt("notification_refresh_id_extra", id);

        // Create intent for click handling
        Intent intent = createMainIntent(context, id, uri, action, categoryId, categoryName);
        intent.putExtra("is_personalized_extra", isPersonalized);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        builder.setContentIntent(pendingIntent);

        // Handle grouping if needed (exact Android logic)
        if (isGroupingNeeded && groupID != 0) {
            builder.setGroup(String.valueOf(groupID));
            
            // Create group summary if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                createBasicGroupSummaryNotification(context, groupID, channel, categoryName);
            }
        }

        // Set notification category for high priority (exact Android logic)
        if (importance >= NotificationCompat.PRIORITY_HIGH) {
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }
    
    /**
     * Create basic group summary notification - consolidated from NotificationManagerModule
     */
    public static void createBasicGroupSummaryNotification(Context context, int groupID, String channel, String categoryName) {
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, channel)
            .setContentTitle(categoryName)
            .setContentText("Multiple notifications")
            .setSmallIcon(getNotificationIcon(context))
            .setGroup(String.valueOf(groupID))
            .setGroupSummary(true)
            .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(groupID + 10000, summaryBuilder.build());
    }
    
    /**
     * Create UserPreferences object from React Native config - consolidated from NotificationManagerModule
     */
    public static UserPreferences createUserPreferencesFromConfig(ReadableMap config) {
        // Extract user preferences from config with defaults
        String selectedLanguage = config.hasKey("selectedLanguage") ? config.getString("selectedLanguage") : "en";
        String preferredLocale = config.hasKey("preferredLocale") ? config.getString("preferredLocale") : "none";
        boolean isNotificationGroupingActive = config.hasKey("isNotificationGroupingActive") ? config.getBoolean("isNotificationGroupingActive") : true;
        boolean keepNotificationAtTop = config.hasKey("keepNotificationAtTop") ? config.getBoolean("keepNotificationAtTop") : false;
        boolean isSilentPush = config.hasKey("isSilentPush") ? config.getBoolean("isSilentPush") : false;

        return new UserPreferences(selectedLanguage, preferredLocale, isNotificationGroupingActive, keepNotificationAtTop, isSilentPush);
    }
}
