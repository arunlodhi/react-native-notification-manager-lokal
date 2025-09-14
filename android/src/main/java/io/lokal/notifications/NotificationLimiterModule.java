package io.lokal.notifications;

import com.facebook.react.bridge.*;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import androidx.annotation.RequiresApi;
import java.util.*;

public class NotificationLimiterModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "NotificationLimiterModule";
    private NotificationManager notificationManager;
    private ReactApplicationContext reactContext;

    // Constants matching Android implementation
    private static final String NOTIFICATION_TIME_EXTRA = "notification_time_extra";
    private static final String NOTIFICATION_REFRESH_ID_EXTRA = "notification_refresh_id_extra";

    public NotificationLimiterModule(ReactApplicationContext reactContext) {
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
                    // Extract timestamp for ordering (exact Android logic)
                    if (notificationExtras.containsKey(NOTIFICATION_TIME_EXTRA)) {
                        long timestamp = notificationExtras.getLong(NOTIFICATION_TIME_EXTRA, 0);
                        extras.putDouble(NOTIFICATION_TIME_EXTRA, (double) timestamp);
                    }
                    
                    // Extract refresh ID for cancellation
                    if (notificationExtras.containsKey(NOTIFICATION_REFRESH_ID_EXTRA)) {
                        int refreshId = notificationExtras.getInt(NOTIFICATION_REFRESH_ID_EXTRA, 0);
                        extras.putInt(NOTIFICATION_REFRESH_ID_EXTRA, refreshId);
                    }
                    
                    // Extract other relevant data
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
    public void cancelNotification(int notificationId, Promise promise) {
        try {
            notificationManager.cancel(notificationId);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CANCEL_ERROR", e.getMessage());
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
    public void getDeviceInfo(Promise promise) {
        try {
            WritableMap deviceInfo = Arguments.createMap();
            deviceInfo.putString("manufacturer", Build.MANUFACTURER);
            deviceInfo.putString("model", Build.MODEL);
            deviceInfo.putString("brand", Build.BRAND);
            deviceInfo.putInt("sdkInt", Build.VERSION.SDK_INT);
            deviceInfo.putString("release", Build.VERSION.RELEASE);
            
            // Device-specific flags for notification handling
            boolean isXiaomi = "Xiaomi".equalsIgnoreCase(Build.MANUFACTURER);
            boolean isOnePlus = "OnePlus".equalsIgnoreCase(Build.MANUFACTURER);
            boolean isOppo = "OPPO".equalsIgnoreCase(Build.MANUFACTURER);
            boolean isVivo = "vivo".equalsIgnoreCase(Build.MANUFACTURER);
            boolean isHuawei = "HUAWEI".equalsIgnoreCase(Build.MANUFACTURER);
            
            deviceInfo.putBoolean("isXiaomi", isXiaomi);
            deviceInfo.putBoolean("isOnePlus", isOnePlus);
            deviceInfo.putBoolean("isOppo", isOppo);
            deviceInfo.putBoolean("isVivo", isVivo);
            deviceInfo.putBoolean("isHuawei", isHuawei);
            deviceInfo.putBoolean("requiresSpecialHandling", isXiaomi || isOnePlus || isOppo || isVivo || isHuawei);
            
            promise.resolve(deviceInfo);
        } catch (Exception e) {
            promise.reject("DEVICE_INFO_ERROR", e.getMessage());
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
    public void getNotificationImportance(String channelId, Promise promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
                if (channel != null) {
                    promise.resolve(channel.getImportance());
                } else {
                    promise.resolve(NotificationManager.IMPORTANCE_NONE);
                }
            } else {
                // For older versions, return default importance
                promise.resolve(NotificationManager.IMPORTANCE_DEFAULT);
            }
        } catch (Exception e) {
            promise.reject("GET_IMPORTANCE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void cancelAllNotifications(Promise promise) {
        try {
            notificationManager.cancelAll();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CANCEL_ALL_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getNotificationChannels(Promise promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                List<android.app.NotificationChannel> channels = notificationManager.getNotificationChannels();
                WritableArray result = Arguments.createArray();
                
                for (android.app.NotificationChannel channel : channels) {
                    WritableMap channelMap = Arguments.createMap();
                    channelMap.putString("id", channel.getId());
                    channelMap.putString("name", channel.getName().toString());
                    channelMap.putInt("importance", channel.getImportance());
                    channelMap.putString("description", channel.getDescription());
                    result.pushMap(channelMap);
                }
                
                promise.resolve(result);
            } else {
                promise.resolve(Arguments.createArray());
            }
        } catch (Exception e) {
            promise.reject("GET_CHANNELS_ERROR", e.getMessage());
        }
    }
}
