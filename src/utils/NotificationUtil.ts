import { NativeModules, Platform } from "react-native";
import {
  Constants,
  AnalyticsConstants,
  RemoteConfigConstants,
} from "../types/Constants";
import {
  NotificationPayload,
  MoEngageNotificationData,
  CricketMatch,
} from "../types/NotificationTypes";
import {
  NotificationCallbacks,
  NotificationEventCallbacks,
} from "../interfaces/NotificationCallbacks";
import { NotificationLimiter } from "./NotificationLimiter";

const { NotificationUtilModule } = NativeModules;

/**
 * NotificationUtil - Exact port of Android NotificationUtil.java
 * Core notification creation and management functionality
 */
export class NotificationUtil {
  private static instance: NotificationUtil;
  private eventCallbacks?: NotificationEventCallbacks;

  // Constants matching Android
  private static readonly COMMENTS_SUMMARY_NOTIFICATION_ID =
    Constants.COMMENTS_SUMMARY_NOTIFICATION_ID;
  private static readonly COMMENTS_NOTIFICATION_ID =
    Constants.COMMENTS_NOTIFICATION_ID;
  private static readonly COMMENTS_GROUP_NOTIFICATION_ID =
    Constants.COMMENTS_GROUP_NOTIFICATION_ID;

  private constructor() {}

  public static getInstance(): NotificationUtil {
    if (!NotificationUtil.instance) {
      NotificationUtil.instance = new NotificationUtil();
    }
    return NotificationUtil.instance;
  }

  public setEventCallbacks(callbacks: NotificationEventCallbacks): void {
    this.eventCallbacks = callbacks;
  }

  /**
   * Validates notification ID - exact port of Android isValid() method
   */
  public async isValid(id: number): Promise<boolean> {
    console.log(`[NotificationUtil] isValid() called with id = ${id}`);

    const prevNotifsList = await this.getStoredIntArray(
      Constants.PREV_NOTIFS_LIST
    );

    if (prevNotifsList.includes(id)) {
      return false;
    } else {
      if (id > 0) {
        if (prevNotifsList.length === 20) {
          prevNotifsList.shift(); // Remove first element
        }
        prevNotifsList.push(id);
        await this.storeIntArray(Constants.PREV_NOTIFS_LIST, prevNotifsList);
      }
      return true;
    }
  }

  /**
   * Validates group ID - exact port of Android isValidGroupId() method
   */
  public async isValidGroupId(channelId: string): Promise<boolean> {
    if (!channelId) return true;

    const id = parseInt(channelId);
    console.log(`[NotificationUtil] isValidGroupId() called with id = ${id}`);

    const prevNotifsGroupsList = await this.getStoredIntArray(
      Constants.PREV_NOTIFS_SAMPLE_GROUPS_LIST
    );

    if (prevNotifsGroupsList.includes(id)) {
      return false;
    } else {
      if (id > 0) {
        if (prevNotifsGroupsList.length === 20) {
          prevNotifsGroupsList.shift(); // Remove first element
        }
        prevNotifsGroupsList.push(id);
        await this.storeIntArray(
          Constants.PREV_NOTIFS_SAMPLE_GROUPS_LIST,
          prevNotifsGroupsList
        );
      }
      return true;
    }
  }

  /**
   * Validates notification - exact port of Android isNotificationValid() method
   */
  public async isNotificationValid(
    notificationId: string,
    sampleGroupId: string
  ): Promise<boolean> {
    const notifId = parseInt(notificationId);
    const isNotificationIdValid = await this.isValid(notifId);

    if (isNotificationIdValid) {
      return await this.isValidGroupId(sampleGroupId);
    } else {
      return false;
    }
  }

  /**
   * Creates notification without image - exact port of Android createNotification() method
   */
  public async createNotification(
    id: number,
    title: string,
    body: string,
    categoryId: string,
    categoryName: string,
    uri: string,
    action: string,
    tag: string,
    channel: string,
    importance: number,
    isGroupingNeeded: boolean,
    groupID: number,
    notifType: string,
    isPersonalized: boolean,
    callbacks: NotificationCallbacks
  ): Promise<void> {
    console.log("[NotificationUtil] createNotification: without image");

    await this.createNotificationChannel(channel, importance);

    console.log("[NotificationUtil] creating notification");

    const intent = this.createNotificationIntent(
      id,
      channel,
      importance,
      isPersonalized,
      categoryName,
      notifType,
      categoryId,
      uri,
      groupID,
      isGroupingNeeded,
      action,
      tag
    );

    if (!(await this.isIntentValid(intent))) {
      return;
    }

    await this.storeTimeStamp();

    // Get post ID from URI
    let postId = Constants.UNSET;
    if (uri) {
      postId = this.getPostIdFromUri(uri);
    }

    // Add extra properties to Analytics
    callbacks.onNotificationBuilt(
      await this.getNotificationBuiltBundle(
        uri,
        String(id),
        postId,
        categoryId,
        channel,
        importance,
        notifType
      )
    );

    const notificationVersion = await this.getNotificationVersion();

    // Set title for notification versions 4, 5, 6
    const finalTitle = this.setTitleForNotificationVersion4And5(
      title,
      body,
      notificationVersion
    );

    const notificationConfig =
      this.getNotificationLayoutConfig(notificationVersion);

    if (
      !(await this.getRemoteConfigBoolean(
        Constants.IS_NOTIFICATION_GROUPING_ACTIVE,
        true
      ))
    ) {
      // Set unique group for each notification
    }

    try {
      if (
        isGroupingNeeded &&
        typeof Platform.Version === "number" &&
        typeof Platform.Version === "number" &&
        Platform.Version > Constants.VERSION_CODES_M &&
        groupID !== 0
      ) {
        await this.createGroupedNotification(
          id,
          finalTitle,
          body,
          channel,
          importance,
          groupID,
          intent,
          notificationConfig,
          callbacks
        );
      } else {
        await this.createSingleNotification(
          id,
          finalTitle,
          body,
          channel,
          importance,
          intent,
          notificationConfig
        );
      }
    } catch (exception) {
      const error =
        exception instanceof Error ? exception : new Error(String(exception));
      console.error(
        "[NotificationUtil] Exception creating notification:",
        error
      );
      await this.recordException(error);
    }
  }

  /**
   * Creates notification with image - exact port of Android createNotification() with bitmap
   */
  public async createNotificationWithImage(
    id: number,
    imageBase64: string,
    blurImageBase64: string | null,
    title: string,
    body: string,
    categoryId: string,
    categoryName: string,
    uri: string,
    action: string,
    tag: string,
    channel: string,
    importance: number,
    isGroupingNeeded: boolean,
    groupID: number,
    notifType: string,
    isPersonalized: boolean,
    callbacks: NotificationCallbacks
  ): Promise<void> {
    console.log("[NotificationUtil] createNotification: with image");

    // Show share buttons only in case of Article/Video notifications
    let showShareButton = false;
    await this.createNotificationChannel(channel, importance);

    const intent = this.createNotificationIntent(
      id,
      channel,
      importance,
      isPersonalized,
      categoryName,
      notifType,
      categoryId,
      uri,
      groupID,
      isGroupingNeeded,
      action,
      tag
    );

    if (action) {
      if (
        action === Constants.ACTION_PUSH ||
        action === Constants.ACTION_PUSH_VIDEO
      ) {
        showShareButton = true;
      }
    }

    // Check for unified feed experiment
    if (
      action &&
      (await this.isInvalidActionForUnifiedFeed(action)) &&
      categoryId &&
      categoryId !== String(await this.getJobsId())
    ) {
      await this.recordException(
        new Error(
          `Notification creation failed due to unified feed. Notification ID: ${id}`
        )
      );
      return;
    }

    let postId = Constants.UNSET;
    if (uri) {
      postId = this.getPostIdFromUri(uri);
      if (await this.isArticleOrVideoUri(uri)) {
        showShareButton = true;
      }
    }

    if (!(await this.isIntentValid(intent))) {
      await this.recordException(
        new Error(
          `Notification creation failed due invalid intent. Notification ID: ${id}`
        )
      );
      return;
    }

    await this.storeTimeStamp();

    callbacks.onNotificationBuilt(
      await this.getNotificationBuiltBundle(
        uri,
        String(id),
        postId,
        categoryId,
        channel,
        importance,
        notifType
      )
    );

    const notificationVersion = await this.getNotificationVersion();
    const finalTitle = this.setTitleForNotificationVersion4And5(
      title,
      body,
      notificationVersion
    );
    const notificationConfig =
      this.getNotificationLayoutConfig(notificationVersion);

    try {
      if (
        isGroupingNeeded &&
        typeof Platform.Version === "number" &&
        Platform.Version > Constants.VERSION_CODES_M &&
        groupID !== 0
      ) {
        await this.createGroupedNotificationWithImage(
          id,
          imageBase64,
          blurImageBase64,
          finalTitle,
          body,
          channel,
          importance,
          groupID,
          intent,
          notificationConfig,
          showShareButton,
          callbacks
        );
      } else {
        await this.createSingleNotificationWithImage(
          id,
          imageBase64,
          blurImageBase64,
          finalTitle,
          body,
          channel,
          importance,
          intent,
          notificationConfig,
          showShareButton
        );
      }
    } catch (exception) {
      const error =
        exception instanceof Error ? exception : new Error(String(exception));
      await this.recordException(
        new Error(
          `Notification creation failed: ${error.message}. Notification ID: ${id}`
        )
      );
      console.error(
        "[NotificationUtil] Exception creating notification:",
        error
      );
    }

    console.log(`[NotificationUtil] createNotification: with image id: ${id}`);
  }

  /**
   * Creates quiz notification - exact port of Android createNotificationForQuiz()
   */
  public async createNotificationForQuiz(
    id: number,
    imageBase64: string,
    blurImageBase64: string | null,
    title: string,
    body: string,
    categoryId: string,
    categoryName: string,
    uri: string,
    action: string,
    tag: string,
    channel: string,
    importance: number,
    isGroupingNeeded: boolean,
    groupID: number,
    notifType: string,
    isPersonalized: boolean,
    callbacks: NotificationCallbacks
  ): Promise<void> {
    await this.createNotificationChannel(channel, importance);

    const intent = this.createNotificationIntent(
      id,
      channel,
      importance,
      isPersonalized,
      categoryName,
      notifType,
      categoryId,
      uri,
      groupID,
      isGroupingNeeded,
      action,
      tag
    );

    if (!(await this.isIntentValid(intent))) {
      return;
    }

    let postId = Constants.UNSET;
    if (uri) {
      postId = this.getPostIdFromUri(uri);
    }

    await this.storeTimeStamp();
    callbacks.onNotificationBuilt(
      await this.getNotificationBuiltBundle(
        uri,
        String(id),
        postId,
        categoryId,
        channel,
        importance,
        notifType
      )
    );

    try {
      await this.createQuizNotification(
        id,
        imageBase64,
        blurImageBase64,
        title,
        body,
        categoryName,
        channel,
        intent
      );
    } catch (exception) {
      const error =
        exception instanceof Error ? exception : new Error(String(exception));
      await this.recordException(error);
      console.error(
        "[NotificationUtil] Exception creating quiz notification:",
        error
      );
    }

    console.log(
      `[NotificationUtil] createNotificationForQuiz: with image id: ${id}`
    );
  }

  /**
   * Creates cricket notification - exact port of Android createNotificationForCricket()
   */
  public async createNotificationForCricket(
    cricketData: CricketMatch,
    batTeamIconBase64: string,
    bowlTeamIconBase64: string,
    callbacks: NotificationCallbacks
  ): Promise<void> {
    const channel = Constants.CRICKET_CHANNEL;
    await this.createNotificationChannel(channel, Constants.IMPORTANCE_LOW);

    const intent = {
      action: Constants.ACTION_PUSH_CRICKET,
    };

    try {
      await this.createCricketNotificationWithData(
        cricketData,
        batTeamIconBase64,
        bowlTeamIconBase64,
        channel,
        intent
      );
    } catch (exception) {
      const error =
        exception instanceof Error ? exception : new Error(String(exception));
      console.error(
        "[NotificationUtil] Exception creating cricket notification:",
        error
      );
    }

    await this.storeTimeStamp();

    // Adding extra properties to Analytics
    const extraPropertyBundle = await this.getNotificationBuiltBundle(
      null,
      "-5",
      null,
      Constants.UNSET,
      channel,
      Constants.IMPORTANCE_LOW,
      ""
    );
    callbacks.onNotificationBuilt(extraPropertyBundle);
  }

  /**
   * Creates comment notification - exact port of Android createNotificationForComments()
   */
  public async createNotificationForComments(
    title: string,
    body: string,
    notificationType: number,
    notificationInterval: string,
    postId: string,
    reporterID: number,
    userId: string,
    postCount: number,
    commentsCount: number,
    isGrouped: boolean
  ): Promise<void> {
    console.log(
      "[NotificationUtil] createNotificationForComments: without image"
    );

    if (
      typeof Platform.Version === "number" &&
      Platform.Version < (await this.getMinimumVersionCode())
    ) {
      return;
    }

    const channel = Constants.COMMENTS_CHANNEL;
    await this.createNotificationChannel(channel, Constants.IMPORTANCE_LOW);

    const bundle = await this.getNotificationHubBundle(
      notificationType,
      postId,
      reporterID,
      null,
      String(postCount),
      String(commentsCount),
      null,
      notificationInterval,
      userId
    );

    await this.trackAnalyticsEvent(
      AnalyticsConstants.NOTIFICATION_BUILT,
      AnalyticsConstants.NOTIFICATION_WITHOUT_IMAGE,
      bundle
    );

    try {
      if (isGrouped) {
        await this.createGroupedCommentNotification(
          title,
          body,
          channel,
          postId,
          reporterID,
          userId,
          postCount,
          commentsCount
        );
      } else {
        await this.createSingleCommentNotification(
          title,
          body,
          channel,
          postId,
          reporterID,
          userId
        );
      }
    } catch (exception) {
      const error =
        exception instanceof Error ? exception : new Error(String(exception));
      console.error(
        "[NotificationUtil] Exception creating comment notification:",
        error
      );
      await this.recordException(error);
    }

    await this.storeTimeStamp();
  }

  // Private helper methods

  private async createNotificationChannel(
    channel: string,
    importance: number
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.createNotificationChannel(
        channel,
        importance
      );
    }
  }

  private createNotificationIntent(
    id: number,
    channel: string,
    importance: number,
    isPersonalized: boolean,
    categoryName: string,
    notifType: string,
    categoryId: string,
    uri: string,
    groupID: number,
    isGroupingNeeded: boolean,
    action: string,
    tag: string
  ): any {
    return {
      id,
      channel,
      importance,
      isPersonalized,
      categoryName,
      notifType,
      categoryId,
      uri,
      groupID,
      isGroupingNeeded,
      action,
      tag,
    };
  }

  private async isIntentValid(intent: any): Promise<boolean> {
    // Basic validation - can be extended based on requirements
    return intent && intent.id && intent.id > 0;
  }

  private async storeTimeStamp(): Promise<void> {
    const timestamp = Date.now();
    await this.storeValue(Constants.NOTIFICATION_TIMESTAMP, String(timestamp));
  }

  private getPostIdFromUri(uri: string): string {
    try {
      // Extract post ID from URI - matches Android logic
      const segments = uri.split("/");
      for (let i = 0; i < segments.length; i++) {
        if (segments[i] === "post" && i + 1 < segments.length) {
          return segments[i + 1];
        }
      }
      return Constants.UNSET;
    } catch (error) {
      return Constants.UNSET;
    }
  }

  private async getNotificationBuiltBundle(
    uri: string | null,
    notificationId: string,
    postId: string | null,
    categoryId: string,
    channel: string,
    importance: number,
    notifType: string
  ): Promise<{ [key: string]: any }> {
    return {
      uri: uri || "",
      notification_id: notificationId,
      post_id: postId || Constants.UNSET,
      category_id: categoryId,
      channel,
      importance,
      notification_type: notifType,
      timestamp: Date.now(),
    };
  }

  private async getNotificationVersion(): Promise<number> {
    return await this.getRemoteConfigInt(
      RemoteConfigConstants.NOTIFICATION_VERSION,
      1
    );
  }

  private setTitleForNotificationVersion4And5(
    title: string,
    body: string,
    version: number
  ): string {
    if (version === 4 || version === 5 || version === 6) {
      return body; // Use body as title for these versions
    }
    return title;
  }

  private getNotificationLayoutConfig(version: number): any {
    return {
      version,
      useCustomLayout: version >= 4,
    };
  }

  private async isInvalidActionForUnifiedFeed(
    action: string
  ): Promise<boolean> {
    const unifiedFeedEnabled = await this.getRemoteConfigBoolean(
      RemoteConfigConstants.UNIFIED_FEED_ENABLED,
      false
    );

    if (!unifiedFeedEnabled) return false;

    const invalidActions = [Constants.ACTION_PUSH, Constants.ACTION_PUSH_VIDEO];
    return invalidActions.includes(action);
  }

  private async getJobsId(): Promise<number> {
    return await this.getRemoteConfigInt(
      RemoteConfigConstants.JOBS_CATEGORY_ID,
      2
    );
  }

  private async isArticleOrVideoUri(uri: string): Promise<boolean> {
    return uri.includes("/article/") || uri.includes("/video/");
  }

  private async getMinimumVersionCode(): Promise<number> {
    return await this.getRemoteConfigInt(
      RemoteConfigConstants.MIN_VERSION_CODE,
      23
    );
  }

  private async createSingleNotification(
    id: number,
    title: string,
    body: string,
    channel: string,
    importance: number,
    intent: any,
    config: any
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.createSingleNotification({
        id,
        title,
        body,
        channel,
        importance,
        intent,
        config,
      });
    }
  }

  private async createGroupedNotification(
    id: number,
    title: string,
    body: string,
    channel: string,
    importance: number,
    groupID: number,
    intent: any,
    config: any,
    callbacks: NotificationCallbacks
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.createGroupedNotification({
        id,
        title,
        body,
        channel,
        importance,
        groupID,
        intent,
        config,
      });
    }
  }

  private async createSingleNotificationWithImage(
    id: number,
    imageBase64: string,
    blurImageBase64: string | null,
    title: string,
    body: string,
    channel: string,
    importance: number,
    intent: any,
    config: any,
    showShareButton: boolean
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.createSingleNotificationWithImage({
        id,
        imageBase64,
        blurImageBase64,
        title,
        body,
        channel,
        importance,
        intent,
        config,
        showShareButton,
      });
    }
  }

  private async createGroupedNotificationWithImage(
    id: number,
    imageBase64: string,
    blurImageBase64: string | null,
    title: string,
    body: string,
    channel: string,
    importance: number,
    groupID: number,
    intent: any,
    config: any,
    showShareButton: boolean,
    callbacks: NotificationCallbacks
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.createGroupedNotificationWithImage({
        id,
        imageBase64,
        blurImageBase64,
        title,
        body,
        channel,
        importance,
        groupID,
        intent,
        config,
        showShareButton,
      });
    }
  }

  private async createQuizNotification(
    id: number,
    imageBase64: string,
    blurImageBase64: string | null,
    title: string,
    body: string,
    categoryName: string,
    channel: string,
    intent: any
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.createQuizNotification({
        id,
        imageBase64,
        blurImageBase64,
        title,
        body,
        categoryName,
        channel,
        intent,
      });
    }
  }

  private async createCricketNotificationWithData(
    cricketData: CricketMatch,
    batTeamIconBase64: string,
    bowlTeamIconBase64: string,
    channel: string,
    intent: any
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.createCricketNotification({
        cricketData,
        batTeamIconBase64,
        bowlTeamIconBase64,
        channel,
        intent,
      });
    }
  }

  private async createSingleCommentNotification(
    title: string,
    body: string,
    channel: string,
    postId: string,
    reporterID: number,
    userId: string
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.createCommentNotification({
        id: NotificationUtil.COMMENTS_NOTIFICATION_ID,
        title,
        body,
        channel,
        postId,
        reporterID,
        userId,
        isGrouped: false,
      });
    }
  }

  private async createGroupedCommentNotification(
    title: string,
    body: string,
    channel: string,
    postId: string,
    reporterID: number,
    userId: string,
    postCount: number,
    commentsCount: number
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.createCommentNotification({
        id: NotificationUtil.COMMENTS_GROUP_NOTIFICATION_ID,
        title,
        body,
        channel,
        postId,
        reporterID,
        userId,
        postCount,
        commentsCount,
        isGrouped: true,
      });
    }
  }

  private async getNotificationHubBundle(
    notificationType: number,
    postId: string,
    reporterID: number,
    userName: string | null,
    postCount: string,
    commentsCount: string,
    extra: string | null,
    notificationInterval: string,
    userId: string
  ): Promise<{ [key: string]: any }> {
    return {
      notification_type: notificationType,
      post_id: postId,
      reporter_id: reporterID,
      user_name: userName || "",
      post_count: postCount,
      comments_count: commentsCount,
      extra: extra || "{}",
      notification_interval: notificationInterval,
      user_id: userId,
    };
  }

  // Utility methods for storage and remote config
  private async getStoredIntArray(key: string): Promise<number[]> {
    if (Platform.OS === "android") {
      return await NotificationUtilModule.getStoredIntArray(key);
    }
    return [];
  }

  private async storeIntArray(key: string, array: number[]): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.storeIntArray(key, array);
    }
  }

  private async storeValue(key: string, value: string): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.storeValue(key, value);
    }
  }

  private async getRemoteConfigBoolean(
    key: string,
    defaultValue: boolean
  ): Promise<boolean> {
    if (Platform.OS === "android") {
      return await NotificationUtilModule.getRemoteConfigBoolean(
        key,
        defaultValue
      );
    }
    return defaultValue;
  }

  private async getRemoteConfigInt(
    key: string,
    defaultValue: number
  ): Promise<number> {
    if (Platform.OS === "android") {
      return await NotificationUtilModule.getRemoteConfigInt(key, defaultValue);
    }
    return defaultValue;
  }

  private async trackAnalyticsEvent(
    eventName: string,
    eventType: string,
    properties: { [key: string]: any }
  ): Promise<void> {
    if (this.eventCallbacks?.onAnalyticsEvent) {
      this.eventCallbacks.onAnalyticsEvent(eventName, eventType, properties);
    }
  }

  private async recordException(error: Error): Promise<void> {
    if (this.eventCallbacks?.onError) {
      this.eventCallbacks.onError(error);
    }
    console.error("[NotificationUtil] Exception:", error);
  }

  // ========== CONSOLIDATED UTILITY METHODS ==========
  // These methods are now centralized in NotificationUtil to avoid duplication

  /**
   * Extract post ID from URI - consolidated utility method
   */
  public static getPostIdFromUri(uri: string): string {
    if (!uri) return Constants.UNSET;
    try {
      const parts = uri.split("/");
      return parts[parts.length - 1] || Constants.UNSET;
    } catch (error) {
      return Constants.UNSET;
    }
  }

  /**
   * Get stored integer array - consolidated utility method
   */
  public static async getStoredIntArray(key: string): Promise<number[]> {
    if (Platform.OS === "android") {
      return await NotificationUtilModule.getStoredIntArray(key);
    }
    return [];
  }

  /**
   * Store integer array - consolidated utility method
   */
  public static async storeIntArray(
    key: string,
    array: number[]
  ): Promise<void> {
    if (Platform.OS === "android") {
      await NotificationUtilModule.storeIntArray(key, array);
    }
  }

  /**
   * Validate notification ID - consolidated utility method
   */
  public static async isValidNotificationId(id: number): Promise<boolean> {
    console.log(
      `[NotificationUtil] isValidNotificationId() called with id = ${id}`
    );

    const prevNotifsList = await NotificationUtil.getStoredIntArray(
      Constants.PREV_NOTIFS_LIST
    );

    if (prevNotifsList.includes(id)) {
      return false;
    } else {
      if (id > 0) {
        if (prevNotifsList.length === 20) {
          prevNotifsList.shift();
        }
        prevNotifsList.push(id);
        await NotificationUtil.storeIntArray(
          Constants.PREV_NOTIFS_LIST,
          prevNotifsList
        );
      }
      return true;
    }
  }

  /**
   * Validate group ID - consolidated utility method
   */
  public static async isValidGroupId(channelId: string): Promise<boolean> {
    if (!channelId) return true;

    const id = parseInt(channelId);
    console.log(`[NotificationUtil] isValidGroupId() called with id = ${id}`);

    const prevNotifsGroupsList = await NotificationUtil.getStoredIntArray(
      Constants.PREV_NOTIFS_SAMPLE_GROUPS_LIST
    );

    if (prevNotifsGroupsList.includes(id)) {
      return false;
    } else {
      if (id > 0) {
        if (prevNotifsGroupsList.length === 20) {
          prevNotifsGroupsList.shift();
        }
        prevNotifsGroupsList.push(id);
        await NotificationUtil.storeIntArray(
          Constants.PREV_NOTIFS_SAMPLE_GROUPS_LIST,
          prevNotifsGroupsList
        );
      }
      return true;
    }
  }

  /**
   * Validate notification - consolidated utility method
   */
  public static async isNotificationValid(
    notificationId: string,
    sampleGroupId: string
  ): Promise<boolean> {
    const notifId = parseInt(notificationId);
    const isNotificationIdValid = await NotificationUtil.isValidNotificationId(
      notifId
    );

    if (isNotificationIdValid) {
      return await NotificationUtil.isValidGroupId(sampleGroupId);
    } else {
      return false;
    }
  }

  /**
   * Get notification built bundle - consolidated utility method
   */
  public static async getNotificationBuiltBundle(
    uri: string | null,
    notificationId: string,
    postId: string | null,
    categoryId: string,
    channel: string,
    importance: number,
    notifType: string
  ): Promise<{ [key: string]: any }> {
    // Check if notifications are enabled
    const isNotificationEnabled =
      Platform.OS === "android"
        ? await NotificationUtilModule.areNotificationsEnabled()
        : true;
    const status = isNotificationEnabled ? "built" : "blocked";

    return {
      notification_id: notificationId,
      category_id: categoryId,
      status: status,
      post_id: postId || Constants.UNSET,
      channel: channel,
      importance: importance,
      notification_type: notifType,
      uri: uri || "",
      timestamp: Date.now(),
    };
  }
}

// Static methods for convenience - consolidated from NotificationManager
export const createNotification = async (
  id: number,
  title: string,
  body: string,
  categoryId: string,
  categoryName: string,
  uri: string,
  action: string,
  tag: string,
  channel?: string,
  importance?: number,
  isGroupingNeeded?: boolean,
  groupID?: number,
  notifType?: string,
  isPersonalized?: boolean,
  callbacks?: NotificationCallbacks
): Promise<void> => {
  await NotificationUtil.getInstance().createNotification(
    id,
    title,
    body,
    categoryId,
    categoryName,
    uri,
    action,
    tag,
    channel || Constants.DEFAULT_CHANNEL,
    importance || Constants.IMPORTANCE_HIGH,
    isGroupingNeeded || false,
    groupID || 0,
    notifType || "",
    isPersonalized || false,
    callbacks || {
      onNotificationBuilt: () => {},
    }
  );
};

export const createNotificationWithImage = async (
  id: number,
  imageUrl: string,
  title: string,
  body: string,
  categoryId: string,
  categoryName: string,
  uri: string,
  action: string,
  tag: string,
  channel?: string,
  importance?: number,
  isGroupingNeeded?: boolean,
  groupID?: number,
  notifType?: string,
  isPersonalized?: boolean,
  callbacks?: NotificationCallbacks
): Promise<void> => {
  // Load image as base64 first
  const imageBase64 = await NotificationUtilModule.loadImageAsBase64(imageUrl);
  const blurImageBase64 = await NotificationUtilModule.loadBlurredImageAsBase64(
    imageUrl,
    25,
    1
  );

  await NotificationUtil.getInstance().createNotificationWithImage(
    id,
    imageBase64,
    blurImageBase64,
    title,
    body,
    categoryId,
    categoryName,
    uri,
    action,
    tag,
    channel || Constants.DEFAULT_CHANNEL,
    importance || Constants.IMPORTANCE_HIGH,
    isGroupingNeeded || false,
    groupID || 0,
    notifType || "",
    isPersonalized || false,
    callbacks || {
      onNotificationBuilt: () => {},
    }
  );
};
