package io.lokal.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.facebook.react.ReactApplication
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule

class NotificationClickReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val uri = intent.getStringExtra("uri")
        val action = intent.getStringExtra("action")
        val categoryId = intent.getStringExtra("category_id")
        
        // Create click data
        val clickData = Arguments.createMap().apply {
            putInt("notificationId", notificationId)
            putString("uri", uri)
            putString("action", action)
            putString("categoryId", categoryId)
            putDouble("timestamp", System.currentTimeMillis().toDouble())
        }
        
        // Send event to React Native
        sendEventToReactNative(context, "onNotificationClick", clickData)
        
        // Launch the app if not running
        launchApp(context)
    }
    
    private fun sendEventToReactNative(context: Context, eventName: String, params: Arguments.WritableMap) {
        try {
            val reactApplication = context.applicationContext as? ReactApplication
            val reactContext = reactApplication?.reactNativeHost?.reactInstanceManager?.currentReactContext
            
            reactContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                ?.emit(eventName, params)
        } catch (e: Exception) {
            // React Native context not available, store for later
            // In a production app, you might want to store this in SharedPreferences
            // and send it when the app becomes active
        }
    }
    
    private fun launchApp(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(it)
        }
    }
}
