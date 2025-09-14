package io.lokal.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Base64
import java.io.ByteArrayOutputStream
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class NotificationManagerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    private val notificationManager: NotificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager: AlarmManager = reactContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val sharedPrefs: SharedPreferences = reactContext.getSharedPreferences("NotificationManager", Context.MODE_PRIVATE)
    private val moduleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val MODULE_NAME = "NotificationManagerModule"
        const val NOTIFICATION_TIME_EXTRA = "notification_time_extra"
        const val NOTIFICATION_REFRESH_ID_EXTRA = "notification_refresh_id_extra"
        const val PREV_NOTIFS_LIST = "prev_notifs_list"
        const val PREV_NOTIFS_SAMPLE_GROUPS_LIST = "prev_notifs_sample_groups_list"
        const val NOTIFICATION_LIMIT = "notification_limit"
        const val NOTIFICATION_KEEP_AT_TOP = "notification_keep_at_top"
        const val DEFAULT_NOTIFICATION_LIMIT = 3
        const val MAX_NOTIFICATION_LIST_SIZE = 20
        const val REFRESH_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
        const val XIAOMI_MANUFACTURER = "Xiaomi"
    }

    override fun getName(): String = MODULE_NAME

    @ReactMethod
    fun initialize(promise: Promise) {
        try {
            createNotificationChannels()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun createNotification(config: ReadableMap, promise: Promise) {
        moduleScope.launch {
            try {
                val id = config.getInt("id")
                val title = config.getString("title") ?: ""
                val body = config.getString("body") ?: ""
                val channel = config.getString("channel") ?: "default"
                val importance = config.getInt("importance")
                val uri = config.getString("uri")
                val action = config.getString("action")
                val categoryId = config.getString("categoryId")
                val isGroupingNeeded = config.getBoolean("isGroupingNeeded")
                val groupID = config.getInt("groupID")

                // Validate notification
                if (!isValidNotification(id, groupID)) {
                    promise.resolve(false)
                    return@launch
                }

                // Apply notification limiting
                limitNotifications()

                // Create notification
                val notification = createNotificationBuilder(title, body, channel, importance)
                    .setContentIntent(createContentIntent(id, uri, action, categoryId))
                    .build()

                // Add timestamp for ordering
                notification.extras.putLong(NOTIFICATION_TIME_EXTRA, System.currentTimeMillis())

                notificationManager.notify(id, notification)
                
                // Send analytics event
                sendEvent("onNotificationBuilt", createAnalyticsBundle(id, categoryId, uri))
                
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("CREATE_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun createNotificationWithImage(config: ReadableMap, promise: Promise) {
        moduleScope.launch {
            try {
                val id = config.getInt("id")
                val imageUrl = config.getString("imageUrl") ?: ""
                val title = config.getString("title") ?: ""
                val body = config.getString("body") ?: ""
                val channel = config.getString("channel") ?: "default"
                val importance = config.getInt("importance")
                val uri = config.getString("uri")
                val action = config.getString("action")
                val categoryId = config.getString("categoryId")

                // Validate notification
                if (!isValidNotification(id, 0)) {
                    promise.resolve(false)
                    return@launch
                }

                // Apply notification limiting
                limitNotifications()

                // Load image
                val bitmap = loadImageBitmap(imageUrl)
                
                // Create notification with image
                val notification = createNotificationBuilder(title, body, channel, importance)
                    .setLargeIcon(bitmap)
                    .setStyle(NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null))
                    .setContentIntent(createContentIntent(id, uri, action, categoryId))
                    .build()

                // Add timestamp for ordering
                notification.extras.putLong(NOTIFICATION_TIME_EXTRA, System.currentTimeMillis())

                notificationManager.notify(id, notification)
                
                // Send analytics event
                sendEvent("onNotificationBuilt", createAnalyticsBundle(id, categoryId, uri))
                
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("CREATE_WITH_IMAGE_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun refreshNotifications(promise: Promise) {
        moduleScope.launch {
            try {
                val activeNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager.activeNotifications.toList()
                } else {
                    emptyList()
                }

                val manufacturer = Build.MANUFACTURER
                var refreshedCount = 0

                for (notification in activeNotifications) {
                    if (manufacturer.equals(XIAOMI_MANUFACTURER, ignoreCase = true)) {
                        // Xiaomi: Simple timestamp update and re-post
                        notification.notification.extras.putLong(NOTIFICATION_TIME_EXTRA, System.currentTimeMillis())
                        notificationManager.notify(notification.id, notification.notification)
                    } else {
                        // Other devices: Cancel and recreate with delay
                        notificationManager.cancel(notification.id)
                        delay(300) // 300ms delay as in Android
                        
                        notification.notification.extras.putLong(NOTIFICATION_TIME_EXTRA, System.currentTimeMillis())
                        notificationManager.notify(notification.id, notification.notification)
                    }
                    refreshedCount++
                }

                sendEvent("onRefreshCompleted", Arguments.createMap().apply {
                    putInt("refreshedCount", refreshedCount)
                })

                promise.resolve(refreshedCount)
            } catch (e: Exception) {
                sendEvent("onRefreshFailed", Arguments.createMap().apply {
                    putString("error", e.message)
                })
                promise.reject("REFRESH_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun limitNotifications(promise: Promise? = null) {
        moduleScope.launch {
            try {
                val keepAtTop = getRemoteConfigBoolean(NOTIFICATION_KEEP_AT_TOP, true)
                if (!keepAtTop) {
                    promise?.resolve(0)
                    return@launch
                }

                val limit = getRemoteConfigInt(NOTIFICATION_LIMIT, DEFAULT_NOTIFICATION_LIMIT)
                val activeNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager.activeNotifications.toList()
                } else {
                    emptyList()
                }

                if (activeNotifications.size <= limit) {
                    promise?.resolve(0)
                    return@launch
                }

                // Sort by timestamp (newest first)
                val sortedNotifications = activeNotifications.sortedByDescending { 
                    it.notification.extras.getLong(NOTIFICATION_TIME_EXTRA, 0)
                }

                // Remove oldest notifications
                val toRemove = sortedNotifications.drop(limit)
                for (notification in toRemove) {
                    notificationManager.cancel(notification.id)
                }

                sendEvent("onNotificationsLimited", Arguments.createMap().apply {
                    putInt("removedCount", toRemove.size)
                })

                promise?.resolve(toRemove.size)
            } catch (e: Exception) {
                promise?.reject("LIMIT_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun cancelNotification(id: Int, promise: Promise) {
        try {
            notificationManager.cancel(id)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CANCEL_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getActiveNotifications(promise: Promise) {
        try {
            val activeNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.activeNotifications.map { statusBarNotification ->
                    Arguments.createMap().apply {
                        putInt("id", statusBarNotification.id)
                        putString("tag", statusBarNotification.tag)
                        putString("packageName", statusBarNotification.packageName)
                        putDouble("postTime", statusBarNotification.postTime.toDouble())
                    }
                }
            } else {
                emptyList()
            }
            
            val result = Arguments.createArray()
            activeNotifications.forEach { result.pushMap(it) }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("GET_ACTIVE_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun scheduleNotificationRefresh(promise: Promise) {
        try {
            val intent = Intent(reactApplicationContext, NotificationRefreshReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                reactApplicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            val triggerTime = System.currentTimeMillis() + REFRESH_INTERVAL_MS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }

            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SCHEDULE_REFRESH_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun cancelNotificationRefresh(promise: Promise) {
        try {
            val intent = Intent(reactApplicationContext, NotificationRefreshReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                reactApplicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            alarmManager.cancel(pendingIntent)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CANCEL_REFRESH_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun createQuizNotification(config: ReadableMap, promise: Promise) {
        moduleScope.launch {
            try {
                val id = config.getInt("id")
                val imageUrl = config.getString("imageUrl") ?: ""
                val title = config.getString("title") ?: ""
                val body = config.getString("body") ?: ""
                val categoryId = config.getString("categoryId") ?: ""
                val categoryName = config.getString("categoryName") ?: ""
                val uri = config.getString("uri") ?: ""
                val action = config.getString("action") ?: ""
                val tag = config.getString("tag") ?: ""
                val channel = config.getString("channel") ?: "default"
                val importance = config.getInt("importance")

                // Validate notification
                if (!isValidNotification(id, 0)) {
                    promise.resolve(false)
                    return@launch
                }

                // Apply notification limiting
                limitNotifications()

                // Load image
                val bitmap = if (imageUrl.isNotEmpty()) loadImageBitmap(imageUrl) else null
                val blurBitmap = bitmap?.let { createBlurredBitmap(it) }

                createQuizNotificationInternal(
                    id, bitmap, blurBitmap, title, body, categoryId, categoryName,
                    uri, action, tag, channel, importance
                )

                // Send analytics event
                sendEvent("onNotificationBuilt", createAnalyticsBundle(id, categoryId, uri))
                
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("CREATE_QUIZ_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun createCommentNotification(config: ReadableMap, promise: Promise) {
        moduleScope.launch {
            try {
                val title = config.getString("title") ?: ""
                val body = config.getString("body") ?: ""
                val notificationType = config.getInt("notificationType")
                val notificationInterval = config.getString("notificationInterval") ?: "instant"
                val postId = config.getString("postId") ?: ""
                val reporterId = config.getInt("reporterId")
                val userId = config.getString("userId") ?: ""
                val postCount = config.getInt("postCount")
                val commentsCount = config.getInt("commentsCount")
                val isGrouped = config.getBoolean("isGrouped")

                createCommentNotificationInternal(
                    title, body, notificationType, notificationInterval,
                    postId, reporterId, userId, postCount, commentsCount, isGrouped
                )

                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("CREATE_COMMENT_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun getMatrimonyNotificationType(notificationId: Int, promise: Promise) {
        val type = when (notificationId) {
            -10 -> "PROFILE_VERIFIED"
            -11 -> "VERIFICATION_FAIL"
            -12 -> "REQUEST_RECEIVED"
            -13 -> "NEW_MATCH"
            else -> null
        }
        promise.resolve(type)
    }

    @ReactMethod
    fun createCricketNotification(config: ReadableMap, promise: Promise) {
        moduleScope.launch {
            try {
                val matchState = config.getString("matchState") ?: "PREVIEW"
                val team1Name = config.getString("team1Name") ?: ""
                val team2Name = config.getString("team2Name") ?: ""
                val team1ShortName = config.getString("team1ShortName") ?: ""
                val team2ShortName = config.getString("team2ShortName") ?: ""
                val team1IconUrl = config.getString("team1IconUrl")
                val team2IconUrl = config.getString("team2IconUrl")
                val matchStatus = config.getString("matchStatus") ?: ""
                val venue = config.getString("venue") ?: ""
                
                // Team scores (for in-progress/complete matches)
                val team1Score = config.getString("team1Score") ?: ""
                val team1Wickets = config.getString("team1Wickets") ?: ""
                val team1Overs = config.getString("team1Overs") ?: ""
                val team2Score = config.getString("team2Score") ?: ""
                val team2Wickets = config.getString("team2Wickets") ?: ""
                val team2Overs = config.getString("team2Overs") ?: ""

                when (matchState.uppercase()) {
                    "PREVIEW" -> {
                        createCricketPreviewNotification(
                            team1ShortName, team2ShortName, matchStatus, venue
                        )
                    }
                    "INPROGRESS", "COMPLETE" -> {
                        val team1Icon = team1IconUrl?.let { loadImageBitmap(it) }
                        val team2Icon = team2IconUrl?.let { loadImageBitmap(it) }
                        
                        createCricketScoreNotification(
                            team1Name, team2Name, team1Icon, team2Icon,
                            team1Score, team1Wickets, team1Overs,
                            team2Score, team2Wickets, team2Overs
                        )
                    }
                }

                // Send analytics event
                sendEvent("onNotificationBuilt", createAnalyticsBundle(-99, "cricket", null))
                
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("CREATE_CRICKET_ERROR", e.message, e)
            }
        }
    }

    private fun createCricketPreviewNotification(
        team1: String, team2: String, status: String, venue: String
    ) {
        val channel = "Cricket"
        createNotificationChannels()

        val intent = Intent().apply {
            action = "ACTION_PUSH_CRICKET"
        }
        val pendingIntent = PendingIntent.getActivity(
            reactApplicationContext, 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val layoutId = getLayoutId("item_view_notification_match_status")
        val customView = android.widget.RemoteViews(reactApplicationContext.packageName, layoutId)
        
        val message = "$team1 vs $team2, $status, Venue: $venue"
        customView.setTextViewText(getViewId("match_status"), message)

        val builder = NotificationCompat.Builder(reactApplicationContext, "${reactApplicationContext.packageName}_$channel")
            .setContentIntent(pendingIntent)
            .setSmallIcon(getNotificationIcon())
            .setColor(reactApplicationContext.resources.getColor(android.R.color.holo_blue_bright))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customView)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            builder.setShowWhen(false)
        }

        notificationManager.notify(99, builder.build())
    }

    private fun createCricketScoreNotification(
        team1Name: String, team2Name: String,
        team1Icon: Bitmap?, team2Icon: Bitmap?,
        team1Score: String, team1Wickets: String, team1Overs: String,
        team2Score: String, team2Wickets: String, team2Overs: String
    ) {
        val channel = "Cricket"
        createNotificationChannels()

        val intent = Intent().apply {
            action = "ACTION_PUSH_CRICKET"
        }
        val pendingIntent = PendingIntent.getActivity(
            reactApplicationContext, 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Collapsed view (mini scorecard)
        val collapsedLayoutId = getLayoutId("item_view_notification_score_card_mini")
        val collapsedView = android.widget.RemoteViews(reactApplicationContext.packageName, collapsedLayoutId)
        
        collapsedView.setTextViewText(getViewId("team_1_title"), team1Name)
        collapsedView.setTextViewText(getViewId("team_2_title"), team2Name)
        
        if (team1Score.isNotEmpty()) {
            val team1ScoreText = if (team1Overs.isNotEmpty()) "$team1Score-$team1Wickets ($team1Overs)" else "$team1Score-$team1Wickets"
            collapsedView.setTextViewText(getViewId("team_1_subtitle1"), team1ScoreText)
        } else {
            collapsedView.setViewVisibility(getViewId("team_1_subtitle1"), android.view.View.GONE)
        }
        
        if (team2Score.isNotEmpty()) {
            val team2ScoreText = if (team2Overs.isNotEmpty()) "$team2Score-$team2Wickets ($team2Overs)" else "$team2Score-$team2Wickets"
            collapsedView.setTextViewText(getViewId("team_2_subtitle1"), team2ScoreText)
        } else {
            collapsedView.setViewVisibility(getViewId("team_2_subtitle1"), android.view.View.GONE)
        }

        team1Icon?.let { collapsedView.setImageViewBitmap(getViewId("ic_team_1"), it) }
        team2Icon?.let { collapsedView.setImageViewBitmap(getViewId("ic_team_2"), it) }

        // Expanded view (full scorecard)
        val expandedLayoutId = getLayoutId("item_view_notification_score_card")
        val expandedView = android.widget.RemoteViews(reactApplicationContext.packageName, expandedLayoutId)
        
        expandedView.setTextViewText(getViewId("team_1_title"), team1Name)
        expandedView.setTextViewText(getViewId("team_2_title"), team2Name)
        
        if (team1Score.isNotEmpty()) {
            expandedView.setTextViewText(getViewId("team_1_subtitle1"), "$team1Score-$team1Wickets")
            expandedView.setTextViewText(getViewId("team_1_subtitle2"), team1Overs)
        } else {
            expandedView.setViewVisibility(getViewId("team_1_subtitle1"), android.view.View.GONE)
            expandedView.setViewVisibility(getViewId("team_1_subtitle2"), android.view.View.GONE)
        }
        
        if (team2Score.isNotEmpty()) {
            expandedView.setTextViewText(getViewId("team_2_subtitle1"), "$team2Score-$team2Wickets")
            expandedView.setTextViewText(getViewId("team_2_subtitle2"), team2Overs)
        } else {
            expandedView.setViewVisibility(getViewId("team_2_subtitle1"), android.view.View.GONE)
            expandedView.setViewVisibility(getViewId("team_2_subtitle2"), android.view.View.GONE)
        }

        team1Icon?.let { expandedView.setImageViewBitmap(getViewId("ic_team_1"), it) }
        team2Icon?.let { expandedView.setImageViewBitmap(getViewId("ic_team_2"), it) }

        val builder = NotificationCompat.Builder(reactApplicationContext, "${reactApplicationContext.packageName}_$channel")
            .setContentIntent(pendingIntent)
            .setSmallIcon(getNotificationIcon())
            .setColor(reactApplicationContext.resources.getColor(android.R.color.holo_blue_bright))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            builder.setShowWhen(false)
        }

        notificationManager.notify(-99, builder.build())
    }

    // Helper methods
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                android.app.NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT),
                android.app.NotificationChannel("high", "High Priority", NotificationManager.IMPORTANCE_HIGH),
                android.app.NotificationChannel("low", "Low Priority", NotificationManager.IMPORTANCE_LOW)
            )
            
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    private fun createNotificationBuilder(title: String, body: String, channel: String, importance: Int): NotificationCompat.Builder {
        return NotificationCompat.Builder(reactApplicationContext, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(getNotificationIcon())
            .setPriority(importance)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
    }

    private fun createCustomNotificationWithoutImage(
        title: String, 
        body: String, 
        channel: String, 
        importance: Int, 
        categoryName: String?,
        pendingIntent: PendingIntent
    ): NotificationCompat.Builder {
        
        val notificationVersion = getNotificationVersion()
        val layoutId = when (notificationVersion) {
            1 -> getLayoutId("notification_small_without_image_v1")
            2 -> getLayoutId("notification_small_without_image_v2") 
            3 -> getLayoutId("notification_small_without_image_v3")
            4 -> getLayoutId("notification_small_without_image_v4")
            5 -> getLayoutId("notification_small_without_image_v5")
            6 -> getLayoutId("notification_small_without_image_v6")
            7 -> getLayoutId("notification_small_without_image_v7")
            else -> getLayoutId("notification_small_without_image_v1")
        }

        val customView = android.widget.RemoteViews(reactApplicationContext.packageName, layoutId)
        customView.setTextViewText(getViewId("title"), parseHtmlTags(title))
        customView.setTextViewText(getViewId("body"), parseHtmlTags(body))
        
        bindNotificationHeader(customView, categoryName)

        // Language-specific handling for Tamil/Malayalam (exact Android logic)
        val languageLocale = getCurrentLanguage()
        val isTamilOrMalayalam = languageLocale == "ta" || languageLocale == "ml"
        
        if (notificationVersion == 6 || notificationVersion == 7) {
            if (body.isEmpty()) {
                val maxLines = if (isTamilOrMalayalam) 3 else 2
                customView.setInt(getViewId("title"), "setMaxLines", maxLines)
                customView.setViewVisibility(getViewId("body"), android.view.View.GONE)
            } else {
                if (isTamilOrMalayalam) {
                    customView.setInt(getViewId("title"), "setMaxLines", 2)
                    customView.setInt(getViewId("body"), "setMaxLines", 1)
                } else {
                    customView.setInt(getViewId("title"), "setMaxLines", 1)
                    customView.setInt(getViewId("body"), "setMaxLines", 1)
                }
            }
        }

        val builder = NotificationCompat.Builder(reactApplicationContext, channel)
            .setContentIntent(pendingIntent)
            .setSmallIcon(getNotificationIcon())
            .setColor(reactApplicationContext.resources.getColor(android.R.color.holo_blue_bright))
            .setDefaults(android.app.Notification.DEFAULT_SOUND)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setCustomContentView(customView)
            .setCustomHeadsUpContentView(customView)
            .setCustomBigContentView(customView)

        // Style handling based on version (exact Android logic)
        val style = when (notificationVersion) {
            1, 2, 3, 4 -> NotificationCompat.DecoratedCustomViewStyle()
            5, 6, 7 -> null
            else -> NotificationCompat.DecoratedCustomViewStyle()
        }
        
        if (style != null) {
            builder.setStyle(style)
        }

        return builder
    }

    private fun createCustomNotificationWithImage(
        title: String,
        body: String, 
        channel: String,
        importance: Int,
        categoryName: String?,
        pendingIntent: PendingIntent,
        bitmap: Bitmap?,
        blurBitmap: Bitmap?
    ): NotificationCompat.Builder {
        
        val notificationVersion = getNotificationVersion()
        
        val smallLayoutId = when (notificationVersion) {
            1 -> getLayoutId("notification_small_v1")
            2 -> getLayoutId("notification_small_v2")
            3 -> getLayoutId("notification_small_v3") 
            4 -> getLayoutId("notification_small_v4")
            5 -> getLayoutId("notification_small_v5")
            6 -> getLayoutId("notification_small_v7")
            else -> getLayoutId("notification_small_v1")
        }
        
        val largeLayoutId = when (notificationVersion) {
            1 -> getLayoutId("notification_large_v1")
            2 -> getLayoutId("notification_large_v2")
            3 -> getLayoutId("notification_large_v3")
            4 -> getLayoutId("notification_large_v4") 
            5 -> getLayoutId("notification_large_v5")
            6 -> getLayoutId("notification_large_v7")
            else -> getLayoutId("notification_large_v1")
        }

        // Create collapsed view
        val collapsedView = android.widget.RemoteViews(reactApplicationContext.packageName, smallLayoutId)
        collapsedView.setTextViewText(getViewId("title"), parseHtmlTags(title))
        collapsedView.setTextViewText(getViewId("body"), parseHtmlTags(body))
        bitmap?.let { collapsedView.setImageViewBitmap(getViewId("icon"), it) }
        blurBitmap?.let { collapsedView.setImageViewBitmap(getViewId("blurr_view"), it) }
        bindNotificationHeader(collapsedView, categoryName)

        // Create expanded view  
        val expandedView = android.widget.RemoteViews(reactApplicationContext.packageName, largeLayoutId)
        expandedView.setTextViewText(getViewId("title"), parseHtmlTags(title))
        expandedView.setTextViewText(getViewId("body"), parseHtmlTags(body))
        bitmap?.let { expandedView.setImageViewBitmap(getViewId("thumbnail"), it) }
        blurBitmap?.let { expandedView.setImageViewBitmap(getViewId("blurr_view"), it) }
        bindNotificationHeader(expandedView, categoryName)

        val builder = NotificationCompat.Builder(reactApplicationContext, channel)
            .setContentIntent(pendingIntent)
            .setSmallIcon(getNotificationIcon())
            .setColor(reactApplicationContext.resources.getColor(android.R.color.holo_blue_bright))
            .setDefaults(android.app.Notification.DEFAULT_SOUND)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setCustomContentView(collapsedView)
            .setCustomHeadsUpContentView(collapsedView)
            .setCustomBigContentView(expandedView)

        if (notificationVersion in 1..4) {
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }

        return builder
    }

    private fun bindNotificationHeader(remoteViews: android.widget.RemoteViews, categoryName: String?) {
        if (!categoryName.isNullOrEmpty()) {
            remoteViews.setViewVisibility(getViewId("category_container"), android.view.View.VISIBLE)
            remoteViews.setTextViewText(getViewId("category_name"), categoryName)
        } else {
            remoteViews.setViewVisibility(getViewId("category_container"), android.view.View.GONE)
        }
        
        remoteViews.setTextViewText(getViewId("time_stamp"), getCurrentTime())
    }

    private fun parseHtmlTags(text: String): CharSequence {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            android.text.Html.fromHtml(text)
        }
    }

    private fun getCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun getNotificationVersion(): Int {
        // Default to version 1, can be made configurable via remote config
        return sharedPrefs.getInt("notification_version", 1)
    }

    private fun getLayoutId(layoutName: String): Int {
        return reactApplicationContext.resources.getIdentifier(
            layoutName, "layout", reactApplicationContext.packageName
        ).takeIf { it != 0 } ?: android.R.layout.simple_list_item_1
    }

    private fun getViewId(viewName: String): Int {
        return reactApplicationContext.resources.getIdentifier(
            viewName, "id", reactApplicationContext.packageName
        ).takeIf { it != 0 } ?: android.R.id.text1
    }

    private fun createContentIntent(id: Int, uri: String?, action: String?, categoryId: String?): PendingIntent {
        val intent = Intent(reactApplicationContext, NotificationClickReceiver::class.java).apply {
            putExtra("notification_id", id)
            putExtra("uri", uri)
            putExtra("action", action)
            putExtra("category_id", categoryId)
        }

        return PendingIntent.getBroadcast(
            reactApplicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
    }

    private suspend fun loadImageBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val future = Glide.with(reactApplicationContext)
                .asBitmap()
                .load(imageUrl)
                .submit()
            
            future.get()
        } catch (e: Exception) {
            null
        }
    }

    private fun isValidNotification(id: Int, groupId: Int): Boolean {
        return isValidNotificationId(id) && isValidGroupId(groupId)
    }

    private fun isValidNotificationId(id: Int): Boolean {
        val prevNotifsList = getStoredIntArray(PREV_NOTIFS_LIST).toMutableList()
        
        if (prevNotifsList.contains(id)) {
            return false
        }
        
        if (id > 0) {
            if (prevNotifsList.size >= MAX_NOTIFICATION_LIST_SIZE) {
                prevNotifsList.removeAt(0)
            }
            prevNotifsList.add(id)
            storeIntArray(PREV_NOTIFS_LIST, prevNotifsList)
        }
        
        return true
    }

    private fun isValidGroupId(groupId: Int): Boolean {
        if (groupId == 0) return true
        
        val prevGroupsList = getStoredIntArray(PREV_NOTIFS_SAMPLE_GROUPS_LIST).toMutableList()
        
        if (prevGroupsList.contains(groupId)) {
            return false
        }
        
        if (prevGroupsList.size >= MAX_NOTIFICATION_LIST_SIZE) {
            prevGroupsList.removeAt(0)
        }
        prevGroupsList.add(groupId)
        storeIntArray(PREV_NOTIFS_SAMPLE_GROUPS_LIST, prevGroupsList)
        
        return true
    }

    private fun getStoredIntArray(key: String): List<Int> {
        val jsonString = sharedPrefs.getString(key, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        return (0 until jsonArray.length()).map { jsonArray.getInt(it) }
    }

    private fun storeIntArray(key: String, array: List<Int>) {
        val jsonArray = JSONArray(array)
        sharedPrefs.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun getRemoteConfigBoolean(key: String, defaultValue: Boolean): Boolean {
        // In a real implementation, this would fetch from Firebase Remote Config
        return sharedPrefs.getBoolean(key, defaultValue)
    }

    private fun getRemoteConfigInt(key: String, defaultValue: Int): Int {
        // In a real implementation, this would fetch from Firebase Remote Config
        return sharedPrefs.getInt(key, defaultValue)
    }

    private fun createAnalyticsBundle(id: Int, categoryId: String?, uri: String?): WritableMap {
        return Arguments.createMap().apply {
            putInt("notification_id", id)
            putString("category_id", categoryId)
            putString("uri", uri)
            putDouble("timestamp", System.currentTimeMillis().toDouble())
        }
    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun createQuizNotificationInternal(
        id: Int, bitmap: Bitmap?, blurBitmap: Bitmap?,
        title: String, body: String, categoryId: String, categoryName: String,
        uri: String, action: String, tag: String, channel: String, importance: Int
    ) {
        createNotificationChannels()

        val intent = Intent().apply {
            putExtra("channel", channel)
            putExtra("importance", importance)
            putExtra("notification_id", id)
            putExtra("is_source_notification", true)
            putExtra("category_id", categoryId)
            putExtra("category_name", categoryName)
            putExtra("uri", uri)
            putExtra("action", action)
            putExtra("tag", tag)
            this.action = action.ifEmpty { "ACTION_PUSH" }
        }

        val pendingIntent = PendingIntent.getActivity(
            reactApplicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Quiz layouts
        val smallLayoutId = getLayoutId("notification_small_quiz")
        val largeLayoutId = getLayoutId("notification_large_quiz")

        val collapsedView = android.widget.RemoteViews(reactApplicationContext.packageName, smallLayoutId)
        collapsedView.setTextViewText(getViewId("title"), parseHtmlTags(title))
        collapsedView.setTextViewText(getViewId("body"), parseHtmlTags(body))
        bitmap?.let { collapsedView.setImageViewBitmap(getViewId("icon"), it) }
        blurBitmap?.let { collapsedView.setImageViewBitmap(getViewId("blurr_view"), it) }
        bindNotificationHeader(collapsedView, categoryName)

        val expandedView = android.widget.RemoteViews(reactApplicationContext.packageName, largeLayoutId)
        expandedView.setTextViewText(getViewId("title"), parseHtmlTags(title))
        expandedView.setTextViewText(getViewId("body"), parseHtmlTags(body))
        bitmap?.let { expandedView.setImageViewBitmap(getViewId("thumbnail"), it) }
        blurBitmap?.let { expandedView.setImageViewBitmap(getViewId("blurr_view"), it) }
        bindNotificationHeader(expandedView, categoryName)

        val builder = NotificationCompat.Builder(reactApplicationContext, "${reactApplicationContext.packageName}_$channel")
            .setContentIntent(pendingIntent)
            .setSmallIcon(getNotificationIcon())
            .setColor(reactApplicationContext.resources.getColor(android.R.color.holo_blue_bright))
            .setDefaults(android.app.Notification.DEFAULT_SOUND)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setCustomHeadsUpContentView(collapsedView)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            builder.setShowWhen(false)
        }

        notificationManager.notify(id, builder.build())
    }

    private fun createCommentNotificationInternal(
        title: String, body: String, notificationType: Int, notificationInterval: String,
        postId: String, reporterId: Int, userId: String, postCount: Int, commentsCount: Int, isGrouped: Boolean
    ) {
        val channel = "Comments"
        createNotificationChannels()

        val intent = Intent().apply {
            putExtra("notification_type", notificationType)
            putExtra("notification_interval", notificationInterval)
            putExtra("post_id", postId)
            putExtra("user_id", userId)
            putExtra("reporter_id", reporterId)
            putExtra("post_count", postCount)
            putExtra("comments_count", commentsCount)
            action = "ACTION_PUSH_NOTIFICATION"
            putExtra("is_source_notification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            reactApplicationContext, 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val builder = NotificationCompat.Builder(reactApplicationContext, "${reactApplicationContext.packageName}_$channel")
            .setContentIntent(pendingIntent)
            .setSmallIcon(getCommentNotificationIcon())
            .setColor(reactApplicationContext.resources.getColor(android.R.color.holo_blue_bright))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(title)
            .setContentText(body)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)

        if (isGrouped && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            builder.setGroup("comments_group_notification")

            val summaryBuilder = NotificationCompat.Builder(reactApplicationContext, "${reactApplicationContext.packageName}_$channel")
                .setContentIntent(pendingIntent)
                .setSmallIcon(getCommentNotificationIcon())
                .setColor(reactApplicationContext.resources.getColor(android.R.color.holo_blue_bright))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setGroup("comments_group_notification")
                .setContentTitle("Comments")
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentText("You have some unread comments")

            val commentId = createNotificationId()
            notificationManager.notify(commentId, builder.build())
            notificationManager.notify(9999, summaryBuilder.build())
        } else {
            val commentId = createNotificationId()
            notificationManager.notify(commentId, builder.build())
        }
    }

    private fun setTitleForNotificationVersion(title: String, body: String, notificationVersion: Int): String {
        if (body.isNotEmpty() && (notificationVersion == 4 || notificationVersion == 5 || notificationVersion == 6)) {
            val trimmedTitle = title.trim()
            return if (checkForPunctuationMarksInTitle(trimmedTitle)) {
                "$trimmedTitle $body"
            } else {
                val language = getCurrentLanguage()
                if (language == "bn" || language == "hi") {
                    "$trimmedTitle| $body"
                } else {
                    "$trimmedTitle. $body"
                }
            }
        }
        return title
    }

    private fun checkForPunctuationMarksInTitle(title: String): Boolean {
        val punctuationMarks = listOf(".", "?", ",", "!", "|")
        return punctuationMarks.any { title.endsWith(it) }
    }

    private fun getCurrentLanguage(): String {
        return java.util.Locale.getDefault().language
    }

    private fun createBlurredBitmap(bitmap: Bitmap): Bitmap? {
        return try {
            // Simple blur implementation - in production you'd use RenderScript or similar
            val blurred = bitmap.copy(bitmap.config, true)
            // Apply blur effect here if needed
            blurred
        } catch (e: Exception) {
            null
        }
    }

    private fun createNotificationId(): Int {
        val sdf = java.text.SimpleDateFormat("ddHHmmss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date()).toInt()
    }

    private fun getCommentNotificationIcon(): Int {
        return try {
            // Try to get comment-specific icon, fallback to default
            reactApplicationContext.resources.getIdentifier(
                "ic_comments_v2", "drawable", reactApplicationContext.packageName
            ).takeIf { it != 0 } ?: getNotificationIcon()
        } catch (e: Exception) {
            getNotificationIcon()
        }
    }

    private fun createHighPriorityMessagingNotification(
        id: Int, title: String, body: String, channel: String,
        pendingIntent: PendingIntent, customContentView: android.widget.RemoteViews,
        customHeadsUpView: android.widget.RemoteViews, customBigView: android.widget.RemoteViews
    ) {
        val appName = try {
            reactApplicationContext.applicationInfo.loadLabel(reactApplicationContext.packageManager).toString()
        } catch (e: Exception) {
            "App"
        }

        val otherPerson = androidx.core.app.Person.Builder()
            .setBot(false)
            .setName(appName)
            .setImportant(true)
            .build()

        val self = androidx.core.app.Person.Builder()
            .setBot(false)
            .setName(appName)
            .setImportant(true)
            .build()

        val builder = NotificationCompat.Builder(reactApplicationContext, "${reactApplicationContext.packageName}_$channel")
            .setContentIntent(pendingIntent)
            .setSmallIcon(getNotificationIcon())
            .setColor(reactApplicationContext.resources.getColor(android.R.color.holo_blue_bright))
            .setContentTitle(parseHtmlTags(title))
            .setContentText(parseHtmlTags(body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .addPerson(self)
            .setStyle(NotificationCompat.MessagingStyle(self)
                .setConversationTitle(appName)
                .addMessage(parseHtmlTags(body).toString(), System.currentTimeMillis(), otherPerson))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setCustomContentView(customContentView)
            .setCustomHeadsUpContentView(customHeadsUpView)
            .setCustomBigContentView(customBigView)
            .setGroup(UUID.randomUUID().toString())
            .setShowWhen(false)
            .setGroupSummary(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        // Check for silent push
        if (isSilentPush()) {
            builder.setSound(null)
                .setWhen(System.currentTimeMillis())
                .setSilent(true)
        } else {
            builder.setDefaults(android.app.Notification.DEFAULT_SOUND)
        }

        notificationManager.notify(id, builder.build())
    }

    private fun isSilentPush(): Boolean {
        // Check if this is a silent push notification
        return sharedPrefs.getBoolean("is_silent_push", false)
    }

    @ReactMethod
    fun setSilentPush(isSilent: Boolean, promise: Promise) {
        try {
            sharedPrefs.edit().putBoolean("is_silent_push", isSilent).apply()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SET_SILENT_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun setNotificationVersion(version: Int, promise: Promise) {
        try {
            sharedPrefs.edit().putInt("notification_version", version).apply()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SET_VERSION_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getRemoteConfigBoolean(key: String, defaultValue: Boolean, promise: Promise) {
        try {
            val value = getRemoteConfigBoolean(key, defaultValue)
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("GET_REMOTE_CONFIG_BOOLEAN_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getRemoteConfigInt(key: String, defaultValue: Int, promise: Promise) {
        try {
            val value = getRemoteConfigInt(key, defaultValue)
            promise.resolve(value)
        } catch (e: Exception) {
            promise.reject("GET_REMOTE_CONFIG_INT_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun createNotificationWithCustomLayout(config: ReadableMap, promise: Promise) {
        moduleScope.launch {
            try {
                val id = config.getInt("id")
                val title = config.getString("title") ?: ""
                val body = config.getString("body") ?: ""
                val channel = config.getString("channel") ?: "default"
                val importance = config.getInt("importance")
                val uri = config.getString("uri")
                val action = config.getString("action")
                val categoryId = config.getString("categoryId")
                val categoryName = config.getString("categoryName")
                val imageUrl = config.getString("imageUrl")
                val notificationVersion = config.getInt("notificationVersion")

                // Validate notification
                if (!isValidNotification(id, 0)) {
                    promise.resolve(false)
                    return@launch
                }

                // Apply notification limiting
                limitNotifications()

                val pendingIntent = createContentIntent(id, uri, action, categoryId)

                val notification = if (imageUrl.isNullOrEmpty()) {
                    // Create without image using custom layout
                    createCustomNotificationWithoutImage(
                        title, body, channel, importance, categoryName, pendingIntent
                    ).build()
                } else {
                    // Create with image using custom layout
                    val bitmap = loadImageBitmap(imageUrl)
                    val blurBitmap = bitmap?.let { createBlurredBitmap(it) }
                    createCustomNotificationWithImage(
                        title, body, channel, importance, categoryName, 
                        pendingIntent, bitmap, blurBitmap
                    ).build()
                }

                // Add timestamp for ordering
                notification.extras.putLong(NOTIFICATION_TIME_EXTRA, System.currentTimeMillis())
                notification.extras.putInt(NOTIFICATION_REFRESH_ID_EXTRA, id)

                notificationManager.notify(id, notification)
                
                // Send analytics event
                sendEvent("onNotificationBuilt", createAnalyticsBundle(id, categoryId, uri))
                
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("CREATE_CUSTOM_LAYOUT_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun createStickyNotification(config: ReadableMap, promise: Promise) {
        moduleScope.launch {
            try {
                val id = config.getInt("id")
                val title = config.getString("title") ?: ""
                val body = config.getString("body") ?: ""
                val channel = config.getString("channel") ?: "Sticky"
                val uri = config.getString("uri")
                val action = config.getString("action")

                createNotificationChannels()

                val intent = Intent().apply {
                    putExtra("channel", channel)
                    putExtra("importance", NotificationManager.IMPORTANCE_LOW)
                    putExtra("notification_id", id)
                    putExtra("is_source_notification", true)
                    putExtra("uri", uri)
                    this.action = action ?: "ACTION_PUSH_STICKY"
                }

                val pendingIntent = PendingIntent.getActivity(
                    reactApplicationContext, id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )

                // Use sticky notification layout
                val layoutId = getLayoutId("notification_sticky_small")
                val customView = android.widget.RemoteViews(reactApplicationContext.packageName, layoutId)
                customView.setTextViewText(getViewId("title"), parseHtmlTags(title))
                customView.setTextViewText(getViewId("body"), parseHtmlTags(body))

                val builder = NotificationCompat.Builder(reactApplicationContext, "${reactApplicationContext.packageName}_$channel")
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(getNotificationIcon())
                    .setColor(reactApplicationContext.resources.getColor(android.R.color.holo_blue_bright))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true) // Sticky notification
                    .setAutoCancel(false)
                    .setCustomContentView(customView)

                // Add timestamp for ordering
                val notification = builder.build()
                notification.extras.putLong(NOTIFICATION_TIME_EXTRA, System.currentTimeMillis())

                notificationManager.notify(id, notification)
                
                // Send analytics event
                sendEvent("onNotificationBuilt", createAnalyticsBundle(id, "sticky", uri))
                
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("CREATE_STICKY_ERROR", e.message, e)
            }
        }
    }

    private fun getNotificationIcon(): Int {
        return try {
            reactApplicationContext.applicationInfo.icon
        } catch (e: Exception) {
            android.R.drawable.ic_dialog_info
        }
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        moduleScope.cancel()
    }
}
