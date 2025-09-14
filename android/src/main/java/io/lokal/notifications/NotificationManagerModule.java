package io.lokal.notifications;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.notification.StatusBarNotification;
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
            createNotificationChannels();
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

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channels matching exact Android implementation
            createChannel("Recommendation", "Recommendations", NotificationManager.IMPORTANCE_HIGH);
            createChannel("Cricket", "Cricket Updates", NotificationManager.IMPORTANCE_LOW);
            createChannel("Comments", "Comments", NotificationManager.IMPORTANCE_LOW);
            createChannel("Downloads", "Downloads", NotificationManager.IMPORTANCE_LOW);
            createChannel("Uploads", "Uploads", NotificationManager.IMPORTANCE_LOW);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel(String channelId, String channelName, int importance) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.setDescription("Channel for " + channelName);
        notificationManager.createNotificationChannel(channel);
    }

    private void createGroupSummaryNotification(int groupID, String channel, String categoryName) {
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(reactContext, channel)
            .setContentTitle(categoryName)
            .setContentText("Multiple notifications")
            .setSmallIcon(getNotificationIcon())
            .setGroup(String.valueOf(groupID))
            .setGroupSummary(true)
            .setAutoCancel(true);

        notificationManager.notify(groupID + 10000, summaryBuilder.build());
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(reactContext, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(getNotificationIcon())
            .setLargeIcon(bitmap)
            .setPriority(importance)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setStyle(new NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null)); // Hide large icon when expanded

        // Add timestamp for ordering
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

        // Handle grouping if needed
        if (isGroupingNeeded && groupID != 0) {
            builder.setGroup(String.valueOf(groupID));
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                createGroupSummaryNotification(groupID, channel, categoryName);
            }
        }

        // Set notification category for high priority
        if (importance >= NotificationCompat.PRIORITY_HIGH) {
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        }

        notificationManager.notify(id, builder.build());
    }

    private int getNotificationIcon() {
        // Try to get the app's icon, fallback to a default
        try {
            return reactContext.getApplicationInfo().icon;
        } catch (Exception e) {
            return android.R.drawable.ic_dialog_info;
        }
    }

    private void sendEventToJS(String eventName, WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }
}
