import { NativeModules, Platform } from "react-native";
import { Constants, DbConstants } from "../types/Constants";
import {
  NotificationPayload,
  NotificationData,
  NotificationType,
} from "../types/NotificationTypes";
import { NotificationUtil } from "./NotificationUtil";

const { NotificationReCreatorModule } = NativeModules;

/**
 * NotificationReCreator - Exact port of Android NotificationReCreator.kt
 * Handles recreation of notifications with fresh timestamps
 */
export class NotificationReCreator {
  private static instance: NotificationReCreator;
  private isSilentPush = false;

  private constructor() {}

  public static getInstance(): NotificationReCreator {
    if (!NotificationReCreator.instance) {
      NotificationReCreator.instance = new NotificationReCreator();
    }
    return NotificationReCreator.instance;
  }

  public setSilentPush(silent: boolean): void {
    this.isSilentPush = silent;
  }

  public getSilentPush(): boolean {
    return this.isSilentPush;
  }

  /**
   * Converts NotificationData to NotificationPayload
   * Matches Android toNotificationPayload() extension function exactly
   */
  public convertToNotificationPayload(
    notificationData: NotificationData
  ): NotificationPayload {
    let extraData: any = {};
    try {
      extraData = JSON.parse(notificationData.extra);
    } catch (e) {
      console.warn(
        "[NotificationReCreator] Failed to parse notification extra data"
      );
    }

    const notificationBody = extraData[DbConstants.BODY_EXTRA] || "";
    const notificationCategoryName = extraData[DbConstants.CATEGORY_NAME] || "";

    return {
      imageUrl: notificationData.postImage,
      notificationId: notificationData.notificationId,
      groupID: parseInt(notificationData.groupId) || 0,
      title: notificationData.title,
      action: notificationData.action,
      body: notificationBody,
      categoryId: notificationData.categoryType,
      categoryName: notificationCategoryName,
      notifType: "",
      tag: notificationData.tag,
      uri: notificationData.uri,
      channel: Constants.DEFAULT_CHANNEL,
      importance: Constants.IMPORTANCE_HIGH,
      isPersonalized: false,
      isGroupingNeeded: false,
    };
  }

  /**
   * Central point to re-push notifications
   * Matches Android createNotification() method exactly
   */
  public async createNotification(
    notificationPayload: NotificationPayload
  ): Promise<void> {
    this.isSilentPush = true;

    if (notificationPayload.imageUrl) {
      try {
        // Load main image
        const imageBase64 = await this.loadImageAsBase64(
          notificationPayload.imageUrl
        );

        try {
          // Load blurred image
          const blurImageBase64 = await this.loadBlurredImageAsBase64(
            notificationPayload.imageUrl
          );
          await this.createBlurredNotification(
            imageBase64,
            blurImageBase64,
            notificationPayload
          );
        } catch (blurError) {
          // Fallback to notification without blur
          await this.createNotificationWithoutBlurImage(
            imageBase64,
            notificationPayload
          );
        }
      } catch (imageError) {
        // Fallback to notification without image
        await this.createNotificationWithoutImage(notificationPayload);
      }
    } else {
      await this.createNotificationWithoutImage(notificationPayload);
    }
  }

  /**
   * Creates notification with both main and blurred images
   * Matches Android createBlurredNotification() logic
   */
  private async createBlurredNotification(
    imageBase64: string,
    blurImageBase64: string,
    notificationPayload: NotificationPayload
  ): Promise<void> {
    if (notificationPayload.type === Constants.TYPE_QUIZ) {
      await this.createNotificationForQuiz(
        imageBase64,
        blurImageBase64,
        notificationPayload
      );
    } else {
      await this.createNotificationWithBlurImage(
        imageBase64,
        blurImageBase64,
        notificationPayload
      );
    }
  }

  /**
   * Creates notification without image
   * Matches Android createNotificationWithoutImage() extension function
   */
  private async createNotificationWithoutImage(
    notificationPayload: NotificationPayload
  ): Promise<void> {
    await NotificationUtil.getInstance().createNotification(
      notificationPayload.notificationId,
      notificationPayload.title,
      notificationPayload.body,
      notificationPayload.categoryId,
      notificationPayload.categoryName,
      notificationPayload.uri,
      notificationPayload.action,
      notificationPayload.tag,
      notificationPayload.channel || Constants.DEFAULT_CHANNEL,
      notificationPayload.importance || Constants.IMPORTANCE_HIGH,
      notificationPayload.isGroupingNeeded || false,
      notificationPayload.groupID || 0,
      notificationPayload.notifType,
      notificationPayload.isPersonalized || false,
      {
        onNotificationBuilt: () => {
          // No need to send analytics here since it is a retry mechanism
        },
      }
    );
  }

  /**
   * Creates notification with blur image
   * Matches Android createNotificationWithBlurImage() extension function
   */
  private async createNotificationWithBlurImage(
    imageBase64: string,
    blurImageBase64: string,
    notificationPayload: NotificationPayload
  ): Promise<void> {
    await NotificationUtil.getInstance().createNotificationWithImage(
      notificationPayload.notificationId,
      imageBase64,
      blurImageBase64,
      notificationPayload.title,
      notificationPayload.body,
      notificationPayload.categoryId,
      notificationPayload.categoryName,
      notificationPayload.uri,
      notificationPayload.action,
      notificationPayload.tag,
      notificationPayload.channel || Constants.DEFAULT_CHANNEL,
      notificationPayload.importance || Constants.IMPORTANCE_HIGH,
      notificationPayload.isGroupingNeeded || false,
      notificationPayload.groupID || 0,
      notificationPayload.notifType,
      notificationPayload.isPersonalized || false,
      {
        onNotificationBuilt: () => {
          // No need to send analytics here since it is a retry mechanism
        },
      }
    );
  }

  /**
   * Creates notification without blur image
   * Matches Android createNotificationWithoutBlurImage() extension function
   */
  private async createNotificationWithoutBlurImage(
    imageBase64: string,
    notificationPayload: NotificationPayload
  ): Promise<void> {
    await NotificationUtil.getInstance().createNotificationWithImage(
      notificationPayload.notificationId,
      imageBase64,
      null, // No blur image
      notificationPayload.title,
      notificationPayload.body,
      notificationPayload.categoryId,
      notificationPayload.categoryName,
      notificationPayload.uri,
      notificationPayload.action,
      notificationPayload.tag,
      notificationPayload.channel || Constants.DEFAULT_CHANNEL,
      notificationPayload.importance || Constants.IMPORTANCE_HIGH,
      notificationPayload.isGroupingNeeded || false,
      notificationPayload.groupID || 0,
      notificationPayload.notifType,
      notificationPayload.isPersonalized || false,
      {
        onNotificationBuilt: () => {
          // No need to send analytics here since it is a retry mechanism
        },
      }
    );
  }

  /**
   * Creates quiz notification with blur image
   * Matches Android createNotificationForQuiz() extension function
   */
  private async createNotificationForQuiz(
    imageBase64: string,
    blurImageBase64: string,
    notificationPayload: NotificationPayload
  ): Promise<void> {
    await NotificationUtil.getInstance().createNotificationForQuiz(
      notificationPayload.notificationId,
      imageBase64,
      blurImageBase64,
      notificationPayload.title,
      notificationPayload.body,
      notificationPayload.categoryId,
      notificationPayload.categoryName,
      notificationPayload.uri,
      notificationPayload.action,
      notificationPayload.tag,
      notificationPayload.channel || Constants.DEFAULT_CHANNEL,
      notificationPayload.importance || Constants.IMPORTANCE_HIGH,
      notificationPayload.isGroupingNeeded || false,
      notificationPayload.groupID || 0,
      notificationPayload.notifType,
      notificationPayload.isPersonalized || false,
      {
        onNotificationBuilt: () => {
          // No need to send analytics here since it is a retry mechanism
        },
      }
    );
  }

  /**
   * Creates quiz notification without blur image
   * Matches Android createNotificationForQuizWithoutBlur() extension function
   */
  private async createNotificationForQuizWithoutBlur(
    imageBase64: string,
    notificationPayload: NotificationPayload
  ): Promise<void> {
    await NotificationUtil.getInstance().createNotificationForQuiz(
      notificationPayload.notificationId,
      imageBase64,
      null, // No blur image
      notificationPayload.title,
      notificationPayload.body,
      notificationPayload.categoryId,
      notificationPayload.categoryName,
      notificationPayload.uri,
      notificationPayload.action,
      notificationPayload.tag,
      notificationPayload.channel || Constants.DEFAULT_CHANNEL,
      notificationPayload.importance || Constants.IMPORTANCE_HIGH,
      notificationPayload.isGroupingNeeded || false,
      notificationPayload.groupID || 0,
      notificationPayload.notifType,
      notificationPayload.isPersonalized || false,
      {
        onNotificationBuilt: () => {
          // No need to send analytics here since it is a retry mechanism
        },
      }
    );
  }

  /**
   * Loads image as base64 string
   * Uses native module to handle image loading with Glide equivalent
   */
  private async loadImageAsBase64(imageUrl: string): Promise<string> {
    if (Platform.OS === "android") {
      return await NotificationReCreatorModule.loadImageAsBase64(imageUrl);
    }
    throw new Error("Image loading not supported on this platform");
  }

  /**
   * Loads blurred image as base64 string
   * Uses native module to handle blur transformation (equivalent to BlurTransformation(20, 3))
   */
  private async loadBlurredImageAsBase64(imageUrl: string): Promise<string> {
    if (Platform.OS === "android") {
      return await NotificationReCreatorModule.loadBlurredImageAsBase64(
        imageUrl,
        20,
        3
      );
    }
    throw new Error("Blurred image loading not supported on this platform");
  }
}

// Static methods to match Android usage pattern
export const createNotification = async (
  notificationPayload: NotificationPayload
): Promise<void> => {
  await NotificationReCreator.getInstance().createNotification(
    notificationPayload
  );
};

export const convertToNotificationPayload = (
  notificationData: NotificationData
): NotificationPayload => {
  return NotificationReCreator.getInstance().convertToNotificationPayload(
    notificationData
  );
};

export const setSilentPush = (silent: boolean): void => {
  NotificationReCreator.getInstance().setSilentPush(silent);
};

export const isSilentPush = (): boolean => {
  return NotificationReCreator.getInstance().getSilentPush();
};
