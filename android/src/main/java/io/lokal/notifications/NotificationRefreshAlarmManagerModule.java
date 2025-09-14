package io.lokal.notifications;

import com.facebook.react.bridge.*;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import androidx.annotation.RequiresApi;

public class NotificationRefreshAlarmManagerModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "NotificationRefreshAlarmManagerModule";
    private NotificationManager notificationManager;
    private ReactApplicationContext reactContext;

    // Constants matching Android implementation
    private static final String NOTIFICATION_TIME_EXTRA = "notification_time_extra";

    public NotificationRefreshAlarmManagerModule(ReactApplicationContext reactContext) {
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
                    // Extract timestamp for sorting (exact Android logic)
                    if (notificationExtras.containsKey(NOTIFICATION_TIME_EXTRA)) {
                        long timestamp = notificationExtras.getLong(NOTIFICATION_TIME_EXTRA, 0);
                        extras.putDouble(NOTIFICATION_TIME_EXTRA, (double) timestamp);
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
    public void getDeviceManufacturer(Promise promise) {
        try {
            promise.resolve(Build.MANUFACTURER);
        } catch (Exception e) {
            promise.reject("GET_MANUFACTURER_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getSystemTime(Promise promise) {
        try {
            promise.resolve((double) System.currentTimeMillis());
        } catch (Exception e) {
            promise.reject("GET_TIME_ERROR", e.getMessage());
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
}
