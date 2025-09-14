package io.lokal.notifications;

import com.facebook.react.bridge.*;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class NotificationRefresherModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "NotificationRefresherModule";
    private NotificationManager notificationManager;
    private ReactApplicationContext reactContext;

    // Constants matching Android implementation
    private static final String NOTIFICATION_TIME_EXTRA = "notification_time_extra";
    private static final String NOTIFICATION_REFRESH_ID_EXTRA = "notification_refresh_id_extra";

    public NotificationRefresherModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.notificationManager = (NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
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
                    // Extract all extras for refresh logic
                    for (String key : notificationExtras.keySet()) {
                        Object value = notificationExtras.get(key);
                        if (value instanceof String) {
                            extras.putString(key, (String) value);
                        } else if (value instanceof Integer) {
                            extras.putInt(key, (Integer) value);
                        } else if (value instanceof Long) {
                            extras.putDouble(key, ((Long) value).doubleValue());
                        } else if (value instanceof Boolean) {
                            extras.putBoolean(key, (Boolean) value);
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
    public void getPackageName(Promise promise) {
        try {
            String packageName = reactContext.getPackageName();
            promise.resolve(packageName);
        } catch (Exception e) {
            promise.reject("PACKAGE_NAME_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void isXiaomiDevice(Promise promise) {
        try {
            String manufacturer = Build.MANUFACTURER;
            boolean isXiaomi = "Xiaomi".equalsIgnoreCase(manufacturer);
            promise.resolve(isXiaomi);
        } catch (Exception e) {
            promise.reject("DEVICE_CHECK_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void notifyNotification(int id, ReadableMap notification, Promise promise) {
        try {
            // For Xiaomi devices - simple timestamp update and re-post
            // This matches the exact Android logic for Xiaomi refresh
            
            // Create a basic notification with updated timestamp
            NotificationCompat.Builder builder = new NotificationCompat.Builder(reactContext, "Recommendation")
                .setSmallIcon(getNotificationIcon())
                .setAutoCancel(true)
                .setOnlyAlertOnce(true); // Silent refresh

            // Update timestamp for ordering
            long currentTime = System.currentTimeMillis();
            builder.getExtras().putLong(NOTIFICATION_TIME_EXTRA, currentTime);
            builder.getExtras().putInt(NOTIFICATION_REFRESH_ID_EXTRA, id);

            // Extract notification data from ReadableMap
            ReadableMap notifData = notification.hasKey("notification") ? notification.getMap("notification") : null;
            if (notifData != null && notifData.hasKey("extras")) {
                ReadableMap extras = notifData.getMap("extras");
                if (extras != null) {
                    // Copy existing extras
                    ReadableMapKeySetIterator iterator = extras.keySetIterator();
                    while (iterator.hasNextKey()) {
                        String key = iterator.nextKey();
                        ReadableType type = extras.getType(key);
                        
                        switch (type) {
                            case String:
                                builder.getExtras().putString(key, extras.getString(key));
                                break;
                            case Number:
                                double value = extras.getDouble(key);
                                if (key.equals(NOTIFICATION_TIME_EXTRA)) {
                                    builder.getExtras().putLong(key, (long) value);
                                } else {
                                    builder.getExtras().putInt(key, (int) value);
                                }
                                break;
                            case Boolean:
                                builder.getExtras().putBoolean(key, extras.getBoolean(key));
                                break;
                        }
                    }
                }
            }

            // Re-post notification with updated timestamp
            notificationManager.notify(id, builder.build());
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("NOTIFY_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void cancelNotification(int id, Promise promise) {
        try {
            notificationManager.cancel(id);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CANCEL_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getTodayNotifications(Promise promise) {
        try {
            // Get notifications from SharedPreferences (simulating database)
            // In a real implementation, this would query your notification database
            SharedPreferences prefs = reactContext.getSharedPreferences("NotificationDatabase", Context.MODE_PRIVATE);
            String notificationsJson = prefs.getString("today_notifications", "[]");
            
            JSONArray jsonArray = new JSONArray(notificationsJson);
            WritableArray result = Arguments.createArray();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject notifObj = jsonArray.getJSONObject(i);
                WritableMap notifMap = Arguments.createMap();
                
                // Convert JSON to WritableMap (matching NotificationData structure)
                notifMap.putInt("notificationId", notifObj.optInt("notificationId", 0));
                notifMap.putString("title", notifObj.optString("title", ""));
                notifMap.putString("body", notifObj.optString("body", ""));
                notifMap.putString("postImage", notifObj.optString("postImage", ""));
                notifMap.putString("groupId", notifObj.optString("groupId", "0"));
                notifMap.putString("action", notifObj.optString("action", ""));
                notifMap.putString("categoryType", notifObj.optString("categoryType", ""));
                notifMap.putString("tag", notifObj.optString("tag", ""));
                notifMap.putString("uri", notifObj.optString("uri", ""));
                notifMap.putString("extra", notifObj.optString("extra", "{}"));
                notifMap.putInt("notificationType", notifObj.optInt("notificationType", 0));
                notifMap.putString("userName", notifObj.optString("userName", ""));
                notifMap.putString("postId", notifObj.optString("postId", ""));
                notifMap.putInt("reporterID", notifObj.optInt("reporterID", 0));
                notifMap.putInt("userId", notifObj.optInt("userId", 0));
                
                result.pushMap(notifMap);
            }
            
            promise.resolve(result);
        } catch (Exception e) {
            promise.reject("GET_TODAY_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void saveNotificationToDatabase(ReadableMap notificationData, Promise promise) {
        try {
            // Save notification to SharedPreferences (simulating database)
            SharedPreferences prefs = reactContext.getSharedPreferences("NotificationDatabase", Context.MODE_PRIVATE);
            String existingJson = prefs.getString("today_notifications", "[]");
            
            JSONArray jsonArray = new JSONArray(existingJson);
            JSONObject newNotif = new JSONObject();
            
            // Convert ReadableMap to JSON
            newNotif.put("notificationId", notificationData.getInt("notificationId"));
            newNotif.put("title", notificationData.getString("title"));
            newNotif.put("body", notificationData.getString("body"));
            newNotif.put("postImage", notificationData.hasKey("postImage") ? notificationData.getString("postImage") : "");
            newNotif.put("groupId", notificationData.hasKey("groupId") ? notificationData.getString("groupId") : "0");
            newNotif.put("action", notificationData.getString("action"));
            newNotif.put("categoryType", notificationData.getString("categoryType"));
            newNotif.put("tag", notificationData.getString("tag"));
            newNotif.put("uri", notificationData.getString("uri"));
            newNotif.put("extra", notificationData.hasKey("extra") ? notificationData.getString("extra") : "{}");
            newNotif.put("timestamp", System.currentTimeMillis());
            
            jsonArray.put(newNotif);
            
            // Keep only last 50 notifications
            if (jsonArray.length() > 50) {
                JSONArray trimmedArray = new JSONArray();
                for (int i = jsonArray.length() - 50; i < jsonArray.length(); i++) {
                    trimmedArray.put(jsonArray.get(i));
                }
                jsonArray = trimmedArray;
            }
            
            prefs.edit().putString("today_notifications", jsonArray.toString()).apply();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("SAVE_NOTIFICATION_ERROR", e.getMessage());
        }
    }

    private int getNotificationIcon() {
        try {
            return reactContext.getApplicationInfo().icon;
        } catch (Exception e) {
            return android.R.drawable.ic_dialog_info;
        }
    }
}
