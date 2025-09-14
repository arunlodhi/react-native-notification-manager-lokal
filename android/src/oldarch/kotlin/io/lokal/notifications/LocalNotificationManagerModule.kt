package io.lokal.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.facebook.react.bridge.*
import org.json.JSONArray
import org.json.JSONObject

class LocalNotificationManagerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    private val alarmManager: AlarmManager = reactContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val sharedPrefs: SharedPreferences = reactContext.getSharedPreferences("LocalNotifications", Context.MODE_PRIVATE)

    companion object {
        const val MODULE_NAME = "LocalNotificationManagerModule"
    }

    override fun getName(): String = MODULE_NAME

    @ReactMethod
    fun scheduleNotification(config: ReadableMap, promise: Promise) {
        try {
            val id = config.getInt("id")
            val title = config.getString("title") ?: ""
            val body = config.getString("body") ?: ""
            val scheduledTime = config.getDouble("scheduledTime").toLong()
            val data = config.getMap("data")

            // Store notification data
            storeScheduledNotification(id, title, body, scheduledTime, data)

            // Create intent for the scheduled notification
            val intent = Intent(reactApplicationContext, LocalNotificationReceiver::class.java).apply {
                putExtra("notification_id", id)
                putExtra("title", title)
                putExtra("body", body)
                putExtra("scheduled_time", scheduledTime)
                
                data?.let { dataMap ->
                    val bundle = Arguments.toBundle(dataMap)
                    putExtra("data", bundle)
                }
            }

            val pendingIntent = PendingIntent.getBroadcast(
                reactApplicationContext,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            // Schedule the notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
            }

            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SCHEDULE_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun cancelNotification(id: Int, promise: Promise) {
        try {
            // Cancel the scheduled alarm
            val intent = Intent(reactApplicationContext, LocalNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                reactApplicationContext,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            
            alarmManager.cancel(pendingIntent)
            
            // Remove from stored notifications
            removeScheduledNotification(id)
            
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CANCEL_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun cancelAllNotifications(promise: Promise) {
        try {
            // Get all scheduled notifications and cancel them
            val scheduledIds = getScheduledNotificationIds()
            
            for (id in scheduledIds) {
                val intent = Intent(reactApplicationContext, LocalNotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    reactApplicationContext,
                    id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )
                
                alarmManager.cancel(pendingIntent)
            }
            
            // Clear all stored notifications
            clearAllScheduledNotifications()
            
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CANCEL_ALL_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getScheduledNotifications(promise: Promise) {
        try {
            val notificationsJson = sharedPrefs.getString("scheduled_notifications", "[]") ?: "[]"
            val jsonArray = JSONArray(notificationsJson)
            val result = Arguments.createArray()
            
            for (i in 0 until jsonArray.length()) {
                val notifObj = jsonArray.getJSONObject(i)
                val notifMap = Arguments.createMap().apply {
                    putInt("id", notifObj.optInt("id", 0))
                    putString("title", notifObj.optString("title", ""))
                    putString("body", notifObj.optString("body", ""))
                    putDouble("scheduledTime", notifObj.optLong("scheduledTime", 0).toDouble())
                    
                    // Add data if present
                    if (notifObj.has("data")) {
                        val dataObj = notifObj.getJSONObject("data")
                        val dataMap = Arguments.createMap()
                        
                        val keys = dataObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = dataObj.get(key)
                            
                            when (value) {
                                is String -> dataMap.putString(key, value)
                                is Int -> dataMap.putInt(key, value)
                                is Double -> dataMap.putDouble(key, value)
                                is Boolean -> dataMap.putBoolean(key, value)
                            }
                        }
                        
                        putMap("data", dataMap)
                    }
                }
                
                result.pushMap(notifMap)
            }
            
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("GET_SCHEDULED_ERROR", e.message, e)
        }
    }

    // Helper methods
    private fun storeScheduledNotification(id: Int, title: String, body: String, scheduledTime: Long, data: ReadableMap?) {
        try {
            val existingJson = sharedPrefs.getString("scheduled_notifications", "[]") ?: "[]"
            val jsonArray = JSONArray(existingJson)
            val newNotif = JSONObject().apply {
                put("id", id)
                put("title", title)
                put("body", body)
                put("scheduledTime", scheduledTime)
                
                data?.let { dataMap ->
                    val dataObj = JSONObject()
                    val iterator = dataMap.keySetIterator()
                    while (iterator.hasNextKey()) {
                        val key = iterator.nextKey()
                        when (val type = dataMap.getType(key)) {
                            ReadableType.String -> dataObj.put(key, dataMap.getString(key))
                            ReadableType.Number -> dataObj.put(key, dataMap.getDouble(key))
                            ReadableType.Boolean -> dataObj.put(key, dataMap.getBoolean(key))
                            else -> {} // Skip unsupported types
                        }
                    }
                    put("data", dataObj)
                }
            }
            
            jsonArray.put(newNotif)
            sharedPrefs.edit().putString("scheduled_notifications", jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Log error but don't throw
            android.util.Log.e("LocalNotificationManager", "Failed to store notification", e)
        }
    }

    private fun removeScheduledNotification(id: Int) {
        try {
            val existingJson = sharedPrefs.getString("scheduled_notifications", "[]") ?: "[]"
            val jsonArray = JSONArray(existingJson)
            val newArray = JSONArray()
            
            for (i in 0 until jsonArray.length()) {
                val notifObj = jsonArray.getJSONObject(i)
                if (notifObj.optInt("id", 0) != id) {
                    newArray.put(notifObj)
                }
            }
            
            sharedPrefs.edit().putString("scheduled_notifications", newArray.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationManager", "Failed to remove notification", e)
        }
    }

    private fun getScheduledNotificationIds(): List<Int> {
        val ids = mutableListOf<Int>()
        try {
            val notificationsJson = sharedPrefs.getString("scheduled_notifications", "[]") ?: "[]"
            val jsonArray = JSONArray(notificationsJson)
            
            for (i in 0 until jsonArray.length()) {
                val notifObj = jsonArray.getJSONObject(i)
                ids.add(notifObj.optInt("id", 0))
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationManager", "Failed to get notification IDs", e)
        }
        return ids
    }

    private fun clearAllScheduledNotifications() {
        try {
            sharedPrefs.edit().putString("scheduled_notifications", "[]").apply()
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationManager", "Failed to clear notifications", e)
        }
    }
}
