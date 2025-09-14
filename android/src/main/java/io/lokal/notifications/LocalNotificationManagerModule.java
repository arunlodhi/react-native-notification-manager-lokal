package io.lokal.notifications;

import com.facebook.react.bridge.*;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class LocalNotificationManagerModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "LocalNotificationManagerModule";
    private NotificationManager notificationManager;
    private AlarmManager alarmManager;
    private ReactApplicationContext reactContext;

    public LocalNotificationManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.notificationManager = (NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) reactContext.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void scheduleNotification(ReadableMap config, Promise promise) {
        try {
            int id = config.getInt("id");
            String title = config.getString("title");
            String body = config.getString("body");
            double scheduledTime = config.getDouble("scheduledTime");
            ReadableMap data = config.hasKey("data") ? config.getMap("data") : null;

            // Store notification data for later use
            storeScheduledNotification(id, title, body, (long) scheduledTime, data);

            // Create intent for the scheduled notification
            Intent intent = new Intent(reactContext, LocalNotificationReceiver.class);
            intent.putExtra("notification_id", id);
            intent.putExtra("title", title);
            intent.putExtra("body", body);
            intent.putExtra("scheduled_time", (long) scheduledTime);
            
            if (data != null) {
                // Convert ReadableMap to Bundle for intent extras
                Bundle dataBundle = new Bundle();
                ReadableMapKeySetIterator iterator = data.keySetIterator();
                while (iterator.hasNextKey()) {
                    String key = iterator.nextKey();
                    ReadableType type = data.getType(key);
                    
                    switch (type) {
                        case String:
                            dataBundle.putString(key, data.getString(key));
                            break;
                        case Number:
                            dataBundle.putDouble(key, data.getDouble(key));
                            break;
                        case Boolean:
                            dataBundle.putBoolean(key, data.getBoolean(key));
                            break;
                    }
                }
                intent.putExtra("data", dataBundle);
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                reactContext, 
                id, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            // Schedule the notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (long) scheduledTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, (long) scheduledTime, pendingIntent);
            }

            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("SCHEDULE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void cancelNotification(int id, Promise promise) {
        try {
            // Cancel the scheduled alarm
            Intent intent = new Intent(reactContext, LocalNotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                reactContext, 
                id, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            
            alarmManager.cancel(pendingIntent);
            
            // Cancel any existing notification
            notificationManager.cancel(id);
            
            // Remove from stored notifications
            removeScheduledNotification(id);
            
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CANCEL_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void cancelAllNotifications(Promise promise) {
        try {
            // Get all scheduled notifications and cancel them
            List<Integer> scheduledIds = getScheduledNotificationIds();
            
            for (int id : scheduledIds) {
                Intent intent = new Intent(reactContext, LocalNotificationReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    reactContext, 
                    id, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
                );
                
                alarmManager.cancel(pendingIntent);
                notificationManager.cancel(id);
            }
            
            // Clear all stored notifications
            clearAllScheduledNotifications();
            
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CANCEL_ALL_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getScheduledNotifications(Promise promise) {
        try {
            SharedPreferences prefs = reactContext.getSharedPreferences("LocalNotifications", Context.MODE_PRIVATE);
            String notificationsJson = prefs.getString("scheduled_notifications", "[]");
            
            JSONArray jsonArray = new JSONArray(notificationsJson);
            WritableArray result = Arguments.createArray();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject notifObj = jsonArray.getJSONObject(i);
                WritableMap notifMap = Arguments.createMap();
                
                notifMap.putInt("id", notifObj.optInt("id", 0));
                notifMap.putString("title", notifObj.optString("title", ""));
                notifMap.putString("body", notifObj.optString("body", ""));
                notifMap.putDouble("scheduledTime", notifObj.optLong("scheduledTime", 0));
                
                // Add data if present
                if (notifObj.has("data")) {
                    JSONObject dataObj = notifObj.getJSONObject("data");
                    WritableMap dataMap = Arguments.createMap();
                    
                    Iterator<String> keys = dataObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object value = dataObj.get(key);
                        
                        if (value instanceof String) {
                            dataMap.putString(key, (String) value);
                        } else if (value instanceof Integer) {
                            dataMap.putInt(key, (Integer) value);
                        } else if (value instanceof Double) {
                            dataMap.putDouble(key, (Double) value);
                        } else if (value instanceof Boolean) {
                            dataMap.putBoolean(key, (Boolean) value);
                        }
                    }
                    
                    notifMap.putMap("data", dataMap);
                }
                
                result.pushMap(notifMap);
            }
            
            promise.resolve(result);
        } catch (Exception e) {
            promise.reject("GET_SCHEDULED_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void showNotificationNow(ReadableMap config, Promise promise) {
        try {
            int id = config.getInt("id");
            String title = config.getString("title");
            String body = config.getString("body");
            
            // Create and show notification immediately
            NotificationCompat.Builder builder = new NotificationCompat.Builder(reactContext, "LocalNotifications")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(getNotificationIcon())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

            notificationManager.notify(id, builder.build());
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("SHOW_NOW_ERROR", e.getMessage());
        }
    }

    // Private helper methods
    private void storeScheduledNotification(int id, String title, String body, long scheduledTime, ReadableMap data) {
        try {
            SharedPreferences prefs = reactContext.getSharedPreferences("LocalNotifications", Context.MODE_PRIVATE);
            String existingJson = prefs.getString("scheduled_notifications", "[]");
            
            JSONArray jsonArray = new JSONArray(existingJson);
            JSONObject newNotif = new JSONObject();
            
            newNotif.put("id", id);
            newNotif.put("title", title);
            newNotif.put("body", body);
            newNotif.put("scheduledTime", scheduledTime);
            
            if (data != null) {
                JSONObject dataObj = new JSONObject();
                ReadableMapKeySetIterator iterator = data.keySetIterator();
                while (iterator.hasNextKey()) {
                    String key = iterator.nextKey();
                    ReadableType type = data.getType(key);
                    
                    switch (type) {
                        case String:
                            dataObj.put(key, data.getString(key));
                            break;
                        case Number:
                            dataObj.put(key, data.getDouble(key));
                            break;
                        case Boolean:
                            dataObj.put(key, data.getBoolean(key));
                            break;
                    }
                }
                newNotif.put("data", dataObj);
            }
            
            jsonArray.put(newNotif);
            prefs.edit().putString("scheduled_notifications", jsonArray.toString()).apply();
        } catch (Exception e) {
            // Log error but don't throw
            android.util.Log.e("LocalNotificationManager", "Failed to store notification", e);
        }
    }

    private void removeScheduledNotification(int id) {
        try {
            SharedPreferences prefs = reactContext.getSharedPreferences("LocalNotifications", Context.MODE_PRIVATE);
            String existingJson = prefs.getString("scheduled_notifications", "[]");
            
            JSONArray jsonArray = new JSONArray(existingJson);
            JSONArray newArray = new JSONArray();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject notifObj = jsonArray.getJSONObject(i);
                if (notifObj.optInt("id", 0) != id) {
                    newArray.put(notifObj);
                }
            }
            
            prefs.edit().putString("scheduled_notifications", newArray.toString()).apply();
        } catch (Exception e) {
            android.util.Log.e("LocalNotificationManager", "Failed to remove notification", e);
        }
    }

    private List<Integer> getScheduledNotificationIds() {
        List<Integer> ids = new ArrayList<>();
        try {
            SharedPreferences prefs = reactContext.getSharedPreferences("LocalNotifications", Context.MODE_PRIVATE);
            String notificationsJson = prefs.getString("scheduled_notifications", "[]");
            
            JSONArray jsonArray = new JSONArray(notificationsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject notifObj = jsonArray.getJSONObject(i);
                ids.add(notifObj.optInt("id", 0));
            }
        } catch (Exception e) {
            android.util.Log.e("LocalNotificationManager", "Failed to get notification IDs", e);
        }
        return ids;
    }

    private void clearAllScheduledNotifications() {
        try {
            SharedPreferences prefs = reactContext.getSharedPreferences("LocalNotifications", Context.MODE_PRIVATE);
            prefs.edit().putString("scheduled_notifications", "[]").apply();
        } catch (Exception e) {
            android.util.Log.e("LocalNotificationManager", "Failed to clear notifications", e);
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
