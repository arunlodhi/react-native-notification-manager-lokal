package io.lokal.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;

public class NotificationClickReceiver extends BroadcastReceiver {
    
    // Constants matching Android implementation
    private static final String NOTIFICATION_ID_EXTRA = "notification_id_extra";
    private static final String URI_EXTRA = "uri_extra";
    private static final String ACTION_EXTRA = "action_extra";
    private static final String CATEGORY_ID_EXTRA = "category_id_extra";
    private static final String CATEGORY_NAME_EXTRA = "notification_category_name_extra";
    private static final String IS_PERSONALIZED_EXTRA = "is_personalized_extra";
    private static final String TAG_EXTRA = "tag_extra";
    private static final String CHANNEL_EXTRA = "channel_extra";
    private static final String IMPORTANCE_EXTRA = "importance_extra";
    private static final String GROUP_NOTIF_ID_EXTRA = "group_notif_id_extra";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Extract notification data from intent (exact Android logic)
            int notificationId = intent.getIntExtra(NOTIFICATION_ID_EXTRA, 0);
            String uri = intent.getStringExtra(URI_EXTRA);
            String action = intent.getStringExtra(ACTION_EXTRA);
            String categoryId = intent.getStringExtra(CATEGORY_ID_EXTRA);
            String categoryName = intent.getStringExtra(CATEGORY_NAME_EXTRA);
            boolean isPersonalized = intent.getBooleanExtra(IS_PERSONALIZED_EXTRA, false);
            String tag = intent.getStringExtra(TAG_EXTRA);
            String channel = intent.getStringExtra(CHANNEL_EXTRA);
            int importance = intent.getIntExtra(IMPORTANCE_EXTRA, 0);
            int groupId = intent.getIntExtra(GROUP_NOTIF_ID_EXTRA, 0);

            // Create data object for React Native (matching interface)
            WritableMap data = Arguments.createMap();
            data.putInt("notificationId", notificationId);
            data.putString("action", action != null ? action : "");
            data.putString("uri", uri != null ? uri : "");
            data.putString("categoryId", categoryId != null ? categoryId : "");
            data.putString("categoryName", categoryName != null ? categoryName : "");
            data.putString("tag", tag != null ? tag : "");
            data.putString("channel", channel != null ? channel : "");
            data.putInt("importance", importance);
            data.putBoolean("isPersonalized", isPersonalized);
            data.putInt("groupId", groupId);
            
            // Add extras map for additional data
            WritableMap extras = Arguments.createMap();
            extras.putString("source", "notification_click");
            extras.putLong("timestamp", System.currentTimeMillis());
            data.putMap("extras", extras);

            // Send event to React Native
            sendEventToReactNative(context, "onNotificationClick", data);

            // Launch main activity (exact Android logic)
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                
                // Add notification data to launch intent for deep linking
                launchIntent.putExtra(NOTIFICATION_ID_EXTRA, notificationId);
                launchIntent.putExtra(URI_EXTRA, uri);
                launchIntent.putExtra(ACTION_EXTRA, action);
                launchIntent.putExtra(CATEGORY_ID_EXTRA, categoryId);
                launchIntent.putExtra(CATEGORY_NAME_EXTRA, categoryName);
                launchIntent.putExtra("is_from_notification", true);
                
                context.startActivity(launchIntent);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Log error but don't crash
            android.util.Log.e("NotificationClick", "Error handling notification click", e);
        }
    }

    private void sendEventToReactNative(Context context, String eventName, WritableMap params) {
        try {
            // Get React Native context and send event
            ReactApplication reactApplication = (ReactApplication) context.getApplicationContext();
            ReactInstanceManager reactInstanceManager = reactApplication.getReactNativeHost().getReactInstanceManager();
            
            if (reactInstanceManager != null) {
                ReactContext reactContext = reactInstanceManager.getCurrentReactContext();
                if (reactContext != null && reactContext.hasActiveCatalystInstance()) {
                    reactContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit(eventName, params);
                } else {
                    // React Native context not ready, store event for later
                    android.util.Log.w("NotificationClick", "React Native context not ready, event will be lost");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("NotificationClick", "Failed to send event to React Native", e);
        }
    }
}
