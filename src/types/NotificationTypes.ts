export interface NotificationPayload {
  imageUrl?: string;
  notificationId: number;
  groupID?: number;
  type?: number;
  importance?: number;
  notificationBitmap?: string; // base64 encoded bitmap
  notificationBlurBitmap?: string; // base64 encoded bitmap
  title: string;
  body: string;
  categoryId: string;
  categoryName: string;
  uri: string;
  action: string;
  tag: string;
  channel?: string;
  notifType: string;
  isPersonalized?: boolean;
  isGroupingNeeded?: boolean;
}

export interface NotificationData {
  notificationId: number;
  title: string;
  body: string;
  postImage?: string;
  groupId: string;
  action: string;
  categoryType: string;
  tag: string;
  uri: string;
  extra: string; // JSON string
  notificationType: number;
  userName?: string;
  postId?: string;
  reporterID?: number;
  userId?: number;
  timestamp?: number; // Unix timestamp when notification was created
}

export interface MoEngageNotificationData {
  moeChannelId: string;
  moeAppId: string;
  gcmCampaignId: string;
  moeCidAttr?: any;
  action?: string;
  categoryName?: string;
  categoryId?: string;
  gcmWebUrl?: string;
  gcmImageUrl?: string;
  gcmTitle: string;
  gcmAlert?: string;
  gcmSubtext?: string;
  buttonText?: string;
  gcmNotificationType?: string;
  googleDeliveredPriority?: string;
}

export interface ActiveNotification {
  id: number;
  packageName: string;
  notification: {
    extras: {
      [key: string]: any;
    };
    when: number;
  };
}

export interface NotificationBuilderConfig {
  contentTitle?: string;
  contentText?: string;
  smallIcon: string;
  largeIcon?: string;
  color: string;
  priority: number;
  autoCancel: boolean;
  onlyAlertOnce: boolean;
  sound?: string;
  vibrate?: number[];
  lights?: {
    color: number;
    onMs: number;
    offMs: number;
  };
  category?: string;
  visibility?: number;
  group?: string;
  groupSummary?: boolean;
  customContentView?: any;
  customBigContentView?: any;
  customHeadsUpContentView?: any;
  style?: any;
  actions?: NotificationAction[];
}

export interface NotificationAction {
  icon: string;
  title: string;
  intent: any;
}

export interface CricketMatch {
  matchState: string;
  team1: {
    sName: string;
  };
  team2: {
    sName: string;
  };
  batTeam: {
    id: string;
    innings: Array<{
      score: string;
      wkts: string;
      overs: string;
    }>;
  };
  bowTeam: {
    id: string;
    innings: Array<{
      score: string;
      wkts: string;
      overs: string;
    }>;
  };
  batTeamName: string;
  bowTeamName: string;
  imageUrl: string;
  header: {
    status: string;
  };
  venue: {
    name: string;
  };
}

export enum NotificationType {
  GENERAL = 0,
  REPORTER = 1,
  COMMENT = 2,
  REPLY = 3,
  MOENGAGE = 4,
}

export enum MatchState {
  PREVIEW = "PREVIEW",
  INPROGRESS = "INPROGRESS",
  COMPLETE = "COMPLETE",
  DEFAULT = "DEFAULT",
}

export interface NotificationVersion {
  version: number;
  layouts: {
    small: string;
    large: string;
    headsUp: string;
  };
  style?: string;
}
