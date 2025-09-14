export interface NotificationCallbacks {
  onNotificationBuilt: (bundle: { [key: string]: any }) => void;
}

export interface NotificationEventCallbacks {
  onNotificationClick?: (data: NotificationClickData) => void;
  onNotificationDismiss?: (notificationId: number) => void;
  onNotificationReceived?: (data: NotificationReceivedData) => void;
  onAnalyticsEvent?: (
    eventName: string,
    eventType: string,
    properties: { [key: string]: any }
  ) => void;
  onError?: (error: Error) => void;
}

export interface NotificationClickData {
  notificationId: number;
  action: string;
  uri?: string;
  categoryId?: string;
  categoryName?: string;
  postId?: string;
  channel?: string;
  importance?: number;
  isPersonalized?: boolean;
  groupId?: number;
  tag?: string;
  extras: { [key: string]: any };
}

export interface NotificationReceivedData {
  notificationId: number;
  title: string;
  body: string;
  imageUrl?: string;
  action: string;
  uri?: string;
  categoryId?: string;
  categoryName?: string;
  channel?: string;
  importance?: number;
  isPersonalized?: boolean;
  groupId?: number;
  tag?: string;
  notifType?: string;
  extras: { [key: string]: any };
}

export interface ResponseCallbacks<T> {
  onSuccess: (data: T) => void;
  onFailure: (error: Error) => void;
}

export interface ResponseCallbackWithAction<T> {
  onSuccess: (data: T) => void;
  onFailure: (error: Error, value?: string) => void;
}

export interface NotificationPermissionCallbacks {
  onPermissionGranted: () => void;
  onPermissionDenied: () => void;
  onPermissionRequested: () => void;
}

export interface NotificationRefreshCallbacks {
  onRefreshStarted: () => void;
  onRefreshCompleted: (refreshedCount: number) => void;
  onRefreshFailed: (error: Error) => void;
}

export interface NotificationLimitCallbacks {
  onNotificationsLimited: (removedCount: number) => void;
  onLimitCheckCompleted: (activeCount: number, limit: number) => void;
}

export interface CricketNotificationCallbacks extends NotificationCallbacks {
  onCricketDataFetched: (matchData: any) => void;
  onCricketImagesFetched: (batTeamIcon: string, bowlTeamIcon: string) => void;
}

export interface CommentNotificationCallbacks extends NotificationCallbacks {
  onCommentNotificationGrouped: (groupId: string) => void;
  onCommentNotificationSingle: (notificationId: number) => void;
}

export interface MoEngageNotificationCallbacks extends NotificationCallbacks {
  onMoEngageNotificationProcessed: (campaignId: string) => void;
  onMoEngageAttributesSet: (attributes: { [key: string]: any }) => void;
}

export interface LocalNotificationCallbacks extends NotificationCallbacks {
  onLocalNotificationScheduled: (
    notificationId: number,
    scheduledTime: number
  ) => void;
  onLocalNotificationCancelled: (notificationId: number) => void;
}

export interface NotificationImageCallbacks {
  onImageLoaded: (imageBase64: string) => void;
  onImageLoadFailed: (error: Error) => void;
  onBlurImageLoaded: (blurImageBase64: string) => void;
  onBlurImageLoadFailed: (error: Error) => void;
}

export interface NotificationValidationCallbacks {
  onNotificationValid: (notificationId: number) => void;
  onNotificationInvalid: (notificationId: number, reason: string) => void;
  onGroupIdValid: (groupId: string) => void;
  onGroupIdInvalid: (groupId: string, reason: string) => void;
}

export interface NotificationAnalyticsCallbacks {
  onAnalyticsEventTracked: (
    eventName: string,
    properties: { [key: string]: any }
  ) => void;
  onAnalyticsEventFailed: (eventName: string, error: Error) => void;
}

export interface NotificationDatabaseCallbacks {
  onNotificationSaved: (notificationId: number) => void;
  onNotificationUpdated: (notificationId: number) => void;
  onNotificationDeleted: (notificationId: number) => void;
  onNotificationsFetched: (notifications: any[]) => void;
  onDatabaseError: (error: Error) => void;
}

export interface NotificationChannelCallbacks {
  onChannelCreated: (channelId: string) => void;
  onChannelUpdated: (channelId: string) => void;
  onChannelDeleted: (channelId: string) => void;
  onChannelError: (channelId: string, error: Error) => void;
}

export interface NotificationGroupCallbacks {
  onGroupCreated: (groupId: string) => void;
  onGroupUpdated: (groupId: string) => void;
  onGroupDeleted: (groupId: string) => void;
  onGroupSummaryCreated: (groupId: string, summaryId: number) => void;
}

export interface NotificationSchedulerCallbacks {
  onNotificationScheduled: (
    notificationId: number,
    scheduledTime: number
  ) => void;
  onNotificationCancelled: (notificationId: number) => void;
  onSchedulerError: (error: Error) => void;
  onPeriodicTaskStarted: (taskName: string) => void;
  onPeriodicTaskStopped: (taskName: string) => void;
}

export interface NotificationWorkerCallbacks {
  onWorkerStarted: (workerName: string) => void;
  onWorkerCompleted: (workerName: string, result: any) => void;
  onWorkerFailed: (workerName: string, error: Error) => void;
  onWorkerProgress: (workerName: string, progress: number) => void;
}
