package io.lokal.notifications;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.service.notification.StatusBarNotification;
import java.util.Locale;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import java.util.*;

public class NotificationManagerModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "NotificationManagerModule";
    private ReactApplicationContext reactContext;
    private NotificationManager notificationManager;

    // Constants matching Android implementation
    private static final String NOTIFICATION_TIME_EXTRA = "notification_time_extra";
    private static final String NOTIFICATION_REFRESH_ID_EXTRA = "notification_refresh_id_extra";
    private static final String NOTIFICATION_ID_EXTRA = "notification_id_extra";
    private static final String URI_EXTRA = "uri_extra";
    private static final String ACTION_EXTRA = "action_extra";
    private static final String CATEGORY_ID_EXTRA = "category_id_extra";
    private static final String CATEGORY_NAME_EXTRA = "notification_category_name_extra";
    private static final String IS_PERSONALIZED_EXTRA = "is_personalized_extra";

    public NotificationManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.notificationManager = (NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void initialize(Promise promise) {
        try {
            // Use consolidated method from NotificationUtil
            NotificationUtil.createNotificationChannels(reactContext);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("INIT_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void createNotification(ReadableMap config, Promise promise) {
        try {
            int id = config.getInt("id");
            String title = config.getString("title");
            String body = config.getString("body");
            String categoryId = config.getString("categoryId");
            String categoryName = config.getString("categoryName");
            String uri = config.getString("uri");
            String action = config.getString("action");
            String tag = config.getString("tag");
            String channel = config.hasKey("channel") ? config.getString("channel") : "Recommendation";
            int importance = config.hasKey("importance") ? config.getInt("importance") : NotificationCompat.PRIORITY_HIGH;
            boolean isGroupingNeeded = config.hasKey("isGroupingNeeded") ? config.getBoolean("isGroupingNeeded") : false;
            int groupID = config.hasKey("groupID") ? config.getInt("groupID") : 0;
            String notifType = config.hasKey("notifType") ? config.getString("notifType") : "";
            boolean isPersonalized = config.hasKey("isPersonalized") ? config.getBoolean("isPersonalized") : false;

            createNotificationInternal(id, title, body, categoryId, categoryName, uri, action, tag, 
                                     channel, importance, isGroupingNeeded, groupID, notifType, isPersonalized);
            
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CREATE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void createNotificationWithImage(ReadableMap config, Promise promise) {
        try {
            String imageUrl = config.getString("imageUrl");
            loadImageAndCreateNotification(config, imageUrl, promise);
        } catch (Exception e) {
            promise.reject("CREATE_IMAGE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void createNotificationWithCustomLayout(ReadableMap config, Promise promise) {
        try {
            int id = config.getInt("id");
            String title = config.getString("title");
            String body = config.getString("body");
            String categoryId = config.getString("categoryId");
            String categoryName = config.getString("categoryName");
            String uri = config.getString("uri");
            String action = config.getString("action");
            String channel = config.hasKey("channel") ? config.getString("channel") : "Recommendation";
            int importance = config.hasKey("importance") ? config.getInt("importance") : NotificationCompat.PRIORITY_HIGH;
            int notificationVersion = config.hasKey("notificationVersion") ? config.getInt("notificationVersion") : 1;
            String imageUrl = config.hasKey("imageUrl") ? config.getString("imageUrl") : null;

            boolean isGroupingNeeded = config.hasKey("isGroupingNeeded") ? config.getBoolean("isGroupingNeeded") : false;
            int groupID = config.hasKey("groupID") ? config.getInt("groupID") : 0;
            String notifType = config.hasKey("notifType") ? config.getString("notifType") : "";
            boolean isPersonalized = config.hasKey("isPersonalized") ? config.getBoolean("isPersonalized") : false;

            // Create UserPreferences from config using consolidated method
            UserPreferences userPreferences = NotificationUtil.createUserPreferencesFromConfig(config);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Load image and create notification with custom layout
                loadImageAndCreateCustomNotification(config, imageUrl, promise);
            } else {
                // Create notification without image using custom layout with all native functionality
                NotificationUtil.createNotificationWithCustomLayout(
                    reactContext,
                    id,
                    null, // no bitmap
                    null, // no blur bitmap
                    title,
                    body,
                    categoryId,
                    categoryName,
                    uri,
                    action,
                    channel,
                    importance,
                    notificationVersion,
                    isGroupingNeeded,
                    groupID,
                    notifType,
                    isPersonalized,
                    userPreferences
                );
                promise.resolve(true);
            }
        } catch (Exception e) {
            promise.reject("CREATE_CUSTOM_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void cancelNotification(int notificationId, Promise promise) {
        try {
            notificationManager.cancel(notificationId);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CANCEL_ERROR", e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    public void getActiveNotifications(Promise promise) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                promise.resolve(Arguments.createArray());
                return;
            }

            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
            WritableArray result = Arguments.createArray();
            
            for (StatusBarNotification notification : notifications) {
                WritableMap notifMap = Arguments.createMap();
                notifMap.putInt("id", notification.getId());
                notifMap.putString("packageName", notification.getPackageName());
                
                WritableMap notificationData = Arguments.createMap();
                WritableMap extras = Arguments.createMap();
                
                Bundle notificationExtras = notification.getNotification().extras;
                if (notificationExtras != null) {
                    for (String key : notificationExtras.keySet()) {
                        Object value = notificationExtras.get(key);
                        if (value instanceof String) {
                            extras.putString(key, (String) value);
                        } else if (value instanceof Integer) {
                            extras.putInt(key, (Integer) value);
                        } else if (value instanceof Long) {
                            extras.putDouble(key, ((Long) value).doubleValue());
                        }
                    }
                }
                
                notificationData.putMap("extras", extras);
                notificationData.putDouble("when", notification.getNotification().when);
                notifMap.putMap("notification", notificationData);
                
                result.pushMap(notifMap);
            }
            
            promise.resolve(result);
        } catch (Exception e) {
            promise.reject("GET_ACTIVE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void areNotificationsEnabled(Promise promise) {
        try {
            boolean enabled = notificationManager.areNotificationsEnabled();
            promise.resolve(enabled);
        } catch (Exception e) {
            promise.reject("CHECK_ENABLED_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getStoredIntArray(String key, Promise promise) {
        try {
            SharedPreferences prefs = reactContext.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
            String jsonString = prefs.getString(key, "[]");
            
            JSONArray jsonArray = new JSONArray(jsonString);
            WritableArray result = Arguments.createArray();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                result.pushInt(jsonArray.getInt(i));
            }
            
            promise.resolve(result);
        } catch (Exception e) {
            promise.reject("GET_ARRAY_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void storeIntArray(String key, ReadableArray array, Promise promise) {
        try {
            SharedPreferences prefs = reactContext.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
            JSONArray jsonArray = new JSONArray();
            
            for (int i = 0; i < array.size(); i++) {
                jsonArray.put(array.getInt(i));
            }
            
            prefs.edit().putString(key, jsonArray.toString()).apply();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("STORE_ARRAY_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void setAppLocale(String languageCode, Promise promise) {
        try {
            setLocale(languageCode);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("SET_LOCALE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void createQuizNotification(ReadableMap config, Promise promise) {
        try {
            int id = config.getInt("id");
            String imageUrl = config.hasKey("imageUrl") ? config.getString("imageUrl") : "";
            String title = config.getString("title");
            String body = config.getString("body");
            String categoryId = config.hasKey("categoryId") ? config.getString("categoryId") : "";
            String categoryName = config.hasKey("categoryName") ? config.getString("categoryName") : "";
            String uri = config.hasKey("uri") ? config.getString("uri") : "";
            String action = config.hasKey("action") ? config.getString("action") : "";
            String tag = config.hasKey("tag") ? config.getString("tag") : "";
            String channel = config.hasKey("channel") ? config.getString("channel") : "default";
            int importance = config.hasKey("importance") ? config.getInt("importance") : NotificationCompat.PRIORITY_HIGH;

            if (imageUrl != null && !imageUrl.isEmpty()) {
                loadImageAndCreateQuizNotification(config, imageUrl, promise);
            } else {
                createQuizNotificationInternal(id, null, null, title, body, categoryId, categoryName,
                    uri, action, tag, channel, importance);
                promise.resolve(true);
            }
        } catch (Exception e) {
            promise.reject("CREATE_QUIZ_ERROR", e.getMessage());
        }
    }

    // Private helper methods
    private void createNotificationInternal(int id, String title, String body, String categoryId, 
                                          String categoryName, String uri, String action, String tag,
                                          String channel, int importance, boolean isGroupingNeeded, 
                                          int groupID, String notifType, boolean isPersonalized) {
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(reactContext, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(getNotificationIcon())
            .setPriority(importance)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false);

        // Add timestamp for ordering (exact Android logic)
        long currentTime = System.currentTimeMillis();
        builder.getExtras().putLong(NOTIFICATION_TIME_EXTRA, currentTime);
        builder.getExtras().putInt(NOTIFICATION_REFRESH_ID_EXTRA, id);

        // Create intent for click handling
        Intent intent = new Intent(reactContext, NotificationClickReceiver.class);
        intent.putExtra(NOTIFICATION_ID_EXTRA, id);
        intent.putExtra(URI_EXTRA, uri);
        intent.putExtra(ACTION_EXTRA, action);
        intent.putExtra(CATEGORY_ID_EXTRA, categoryId);
        intent.putExtra(CATEGORY_NAME_EXTRA, categoryName);
        intent.putExtra(IS_PERSONALIZED_EXTRA, isPersonalized);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            reactContext, id, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        builder.setContentIntent(pendingIntent);

        // Handle grouping if needed (exact Android logic)
        if (isGroupingNeeded && groupID != 0) {
            builder.setGroup(String.valueOf(groupID));
            
            // Create group summary if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                createGroupSummaryNotification(groupID, channel, categoryName);
            }
        }

        // Set notification category for high priority (exact Android logic)
        if (importance >= NotificationCompat.PRIORITY_HIGH) {
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        }

        notificationManager.notify(id, builder.build());
    }

    // Removed duplicate methods - now using consolidated methods from NotificationUtil
    private void createGroupSummaryNotification(int groupID, String channel, String categoryName) {
        // Use consolidated method from NotificationUtil
        NotificationUtil.createBasicGroupSummaryNotification(reactContext, groupID, channel, categoryName);
    }

    private void loadImageAndCreateNotification(ReadableMap config, String imageUrl, Promise promise) {
        Glide.with(reactContext)
            .asBitmap()
            .load(imageUrl)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    try {
                        createNotificationWithBitmap(config, resource);
                        promise.resolve(true);
                    } catch (Exception e) {
                        promise.reject("CREATE_WITH_BITMAP_ERROR", e.getMessage());
                    }
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    // Fallback to notification without image
                    try {
                        createNotification(config, promise);
                    } catch (Exception e) {
                        promise.reject("FALLBACK_ERROR", e.getMessage());
                    }
                }
            });
    }

    private void loadImageAndCreateCustomNotification(ReadableMap config, String imageUrl, Promise promise) {
        Glide.with(reactContext)
            .asBitmap()
            .load(imageUrl)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    try {
                        createCustomNotificationWithBitmap(config, resource);
                        promise.resolve(true);
                    } catch (Exception e) {
                        promise.reject("CREATE_CUSTOM_WITH_BITMAP_ERROR", e.getMessage());
                    }
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    // Fallback to custom notification without image
                    try {
                        createNotificationWithCustomLayout(config, promise);
                    } catch (Exception e) {
                        promise.reject("CUSTOM_FALLBACK_ERROR", e.getMessage());
                    }
                }
            });
    }

    private void createCustomNotificationWithBitmap(ReadableMap config, Bitmap bitmap) {
        int id = config.getInt("id");
        String title = config.getString("title");
        String body = config.getString("body");
        String categoryId = config.getString("categoryId");
        String categoryName = config.getString("categoryName");
        String uri = config.getString("uri");
        String action = config.getString("action");
        String channel = config.hasKey("channel") ? config.getString("channel") : "Recommendation";
        int importance = config.hasKey("importance") ? config.getInt("importance") : NotificationCompat.PRIORITY_HIGH;
        int notificationVersion = config.hasKey("notificationVersion") ? config.getInt("notificationVersion") : 1;
        boolean isGroupingNeeded = config.hasKey("isGroupingNeeded") ? config.getBoolean("isGroupingNeeded") : false;
        int groupID = config.hasKey("groupID") ? config.getInt("groupID") : 0;
        String notifType = config.hasKey("notifType") ? config.getString("notifType") : "";
        boolean isPersonalized = config.hasKey("isPersonalized") ? config.getBoolean("isPersonalized") : false;

        // Create UserPreferences from config using consolidated method
        UserPreferences userPreferences = NotificationUtil.createUserPreferencesFromConfig(config);

        // Use enhanced NotificationUtil with all native functionality
        NotificationUtil.createNotificationWithCustomLayout(
            reactContext,
            id,
            bitmap,
            null, // blurrBitmap - can be enhanced later
            title,
            body,
            categoryId,
            categoryName,
            uri,
            action,
            channel,
            importance,
            notificationVersion,
            isGroupingNeeded,
            groupID,
            notifType,
            isPersonalized,
            userPreferences
        );
    }

    private void createNotificationWithBitmap(ReadableMap config, Bitmap bitmap) {
        int id = config.getInt("id");
        String title = config.getString("title");
        String body = config.getString("body");
        String categoryId = config.getString("categoryId");
        String categoryName = config.getString("categoryName");
        String uri = config.getString("uri");
        String action = config.getString("action");
        String tag = config.getString("tag");
        String channel = config.hasKey("channel") ? config.getString("channel") : "Recommendation";
        int importance = config.hasKey("importance") ? config.getInt("importance") : NotificationCompat.PRIORITY_HIGH;
        boolean isGroupingNeeded = config.hasKey("isGroupingNeeded") ? config.getBoolean("isGroupingNeeded") : false;
        int groupID = config.hasKey("groupID") ? config.getInt("groupID") : 0;
        String notifType = config.hasKey("notifType") ? config.getString("notifType") : "";
        boolean isPersonalized = config.hasKey("isPersonalized") ? config.getBoolean("isPersonalized") : false;
        int notificationVersion = config.hasKey("notificationVersion") ? config.getInt("notificationVersion") : 1;

        // Create UserPreferences from config using consolidated method
        UserPreferences userPreferences = NotificationUtil.createUserPreferencesFromConfig(config);

        // Use the enhanced NotificationUtil with all native functionality
        NotificationUtil.createNotificationWithCustomLayout(
            reactContext,
            id,
            bitmap,
            null, // blurrBitmap - can be added later if needed
            title,
            body,
            categoryId,
            categoryName,
            uri,
            action,
            channel,
            importance,
            notificationVersion,
            isGroupingNeeded,
            groupID,
            notifType,
            isPersonalized,
            userPreferences
        );
    }

    // Removed duplicate methods - now using consolidated methods from NotificationUtil
    private int getNotificationIcon() {
        // Use consolidated method from NotificationUtil
        return NotificationUtil.getNotificationIcon(reactContext);
    }

    private void loadImageAndCreateQuizNotification(ReadableMap config, String imageUrl, Promise promise) {
        Glide.with(reactContext)
            .asBitmap()
            .load(imageUrl)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    try {
                        int id = config.getInt("id");
                        String title = config.getString("title");
                        String body = config.getString("body");
                        String categoryId = config.hasKey("categoryId") ? config.getString("categoryId") : "";
                        String categoryName = config.hasKey("categoryName") ? config.getString("categoryName") : "";
                        String uri = config.hasKey("uri") ? config.getString("uri") : "";
                        String action = config.hasKey("action") ? config.getString("action") : "";
                        String tag = config.hasKey("tag") ? config.getString("tag") : "";
                        String channel = config.hasKey("channel") ? config.getString("channel") : "default";
                        int importance = config.hasKey("importance") ? config.getInt("importance") : NotificationCompat.PRIORITY_HIGH;

                        // Create blurred bitmap (simple implementation)
                        Bitmap blurBitmap = createBlurredBitmap(resource);
                        
                        createQuizNotificationInternal(id, resource, blurBitmap, title, body, categoryId, categoryName,
                            uri, action, tag, channel, importance);
                        promise.resolve(true);
                    } catch (Exception e) {
                        promise.reject("CREATE_QUIZ_WITH_BITMAP_ERROR", e.getMessage());
                    }
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    // Fallback to quiz notification without image
                    try {
                        createQuizNotification(config, promise);
                    } catch (Exception e) {
                        promise.reject("QUIZ_FALLBACK_ERROR", e.getMessage());
                    }
                }
            });
    }

    private void createQuizNotificationInternal(int id, Bitmap bitmap, Bitmap blurBitmap,
                                              String title, String body, String categoryId, String categoryName,
                                              String uri, String action, String tag, String channel, int importance) {
        
        // Create notification channels
        NotificationUtil.createNotificationChannels(reactContext);

        // Create intent
        Intent intent = new Intent(reactContext, NotificationClickReceiver.class);
        intent.putExtra("channel", channel);
        intent.putExtra("importance", importance);
        intent.putExtra(NOTIFICATION_ID_EXTRA, id);
        intent.putExtra("is_source_notification", true);
        intent.putExtra(CATEGORY_ID_EXTRA, categoryId);
        intent.putExtra(CATEGORY_NAME_EXTRA, categoryName);
        intent.putExtra(URI_EXTRA, uri);
        intent.putExtra(ACTION_EXTRA, action);
        intent.putExtra("tag", tag);
        intent.setAction(action.isEmpty() ? "ACTION_PUSH" : action);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            reactContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Get layout resource IDs for quiz layouts
        int smallLayoutId = getLayoutId("notification_small_quiz");
        int largeLayoutId = getLayoutId("notification_large_quiz");

        // Create collapsed view (small)
        android.widget.RemoteViews collapsedView = new android.widget.RemoteViews(reactContext.getPackageName(), smallLayoutId);
        collapsedView.setTextViewText(getViewId("title"), parseHtmlTags(title));
        collapsedView.setTextViewText(getViewId("body"), parseHtmlTags(body));
        if (bitmap != null) {
            collapsedView.setImageViewBitmap(getViewId("icon"), bitmap);
        }
        if (blurBitmap != null) {
            collapsedView.setImageViewBitmap(getViewId("blurr_view"), blurBitmap);
        }
        bindNotificationHeader(collapsedView, categoryName);

        // Create expanded view (large)
        android.widget.RemoteViews expandedView = new android.widget.RemoteViews(reactContext.getPackageName(), largeLayoutId);
        expandedView.setTextViewText(getViewId("title"), parseHtmlTags(title));
        expandedView.setTextViewText(getViewId("body"), parseHtmlTags(body));
        if (bitmap != null) {
            expandedView.setImageViewBitmap(getViewId("thumbnail"), bitmap);
        }
        if (blurBitmap != null) {
            expandedView.setImageViewBitmap(getViewId("blurr_view"), blurBitmap);
        }
        bindNotificationHeader(expandedView, categoryName);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(reactContext, reactContext.getPackageName() + "_" + channel)
            .setContentIntent(pendingIntent)
            .setSmallIcon(getNotificationIcon())
            .setColor(reactContext.getResources().getColor(android.R.color.holo_blue_bright))
            .setDefaults(android.app.Notification.DEFAULT_SOUND)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setCustomHeadsUpContentView(collapsedView);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            builder.setShowWhen(false);
        }

        // Add timestamp for ordering
        android.app.Notification notification = builder.build();
        notification.extras.putLong(NOTIFICATION_TIME_EXTRA, System.currentTimeMillis());
        notification.extras.putInt(NOTIFICATION_REFRESH_ID_EXTRA, id);

        notificationManager.notify(id, notification);
    }

    // Helper methods - using reflection to access private methods from NotificationUtil
    private int getLayoutId(String layoutName) {
        int resourceId = reactContext.getResources().getIdentifier(
            layoutName, "layout", reactContext.getPackageName()
        );
        return resourceId != 0 ? resourceId : android.R.layout.simple_list_item_1;
    }

    private int getViewId(String viewName) {
        int resourceId = reactContext.getResources().getIdentifier(
            viewName, "id", reactContext.getPackageName()
        );
        return resourceId != 0 ? resourceId : android.R.id.text1;
    }

    private void bindNotificationHeader(android.widget.RemoteViews remoteViews, String categoryName) {
        if (categoryName != null && !categoryName.isEmpty()) {
            remoteViews.setViewVisibility(getViewId("category_container"), android.view.View.VISIBLE);
            remoteViews.setTextViewText(getViewId("category_name"), categoryName);
        } else {
            remoteViews.setViewVisibility(getViewId("category_container"), android.view.View.GONE);
        }
        
        remoteViews.setTextViewText(getViewId("time_stamp"), getCurrentTime());
    }

    private String parseHtmlTags(String text) {
        if (text == null) return "";
        // Simple HTML tag removal - NotificationUtil has more sophisticated parsing
        return text.replaceAll("<[^>]*>", "").trim();
    }

    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private Bitmap createBlurredBitmap(Bitmap bitmap) {
        try {
            // Simple blur implementation - in production you'd use RenderScript or similar
            Bitmap blurred = bitmap.copy(bitmap.getConfig(), true);
            // Apply blur effect here if needed
            return blurred;
        } catch (Exception e) {
            return null;
        }
    }

    private void sendEventToJS(String eventName, WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    // Locale utility methods
    private void setLocale(String languageCode) {
        try {
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);
            
            Resources resources = reactContext.getResources();
            Configuration config = resources.getConfiguration();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList localeList = new LocaleList(locale);
                LocaleList.setDefault(localeList);
                config.setLocales(localeList);
            } else {
                config.locale = locale;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLayoutDirection(locale);
            }
            
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            
            // Store the selected locale in SharedPreferences for persistence
            SharedPreferences prefs = reactContext.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("selected_language", languageCode).apply();
            
            // Also update preferred locale if it was set
            String preferredLocale = prefs.getString("preferred_locale", "none");
            if (!preferredLocale.equals("none")) {
                prefs.edit().putString("preferred_locale", languageCode).apply();
            }
            
            // Send event to JS about locale change
            WritableMap params = Arguments.createMap();
            params.putString("locale", languageCode);
            sendEventToJS("onLocaleChanged", params);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to set locale: " + e.getMessage());
        }
    }
    
    
}
