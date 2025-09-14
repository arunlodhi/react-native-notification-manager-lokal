/**
 * Comment Notification Types
 * Matches the exact Android comment notification implementation
 */

export interface CommentNotificationData {
  title: string;
  body: string;
  notificationType: CommentNotificationType;
  notificationInterval: string;
  postId: string;
  reporterId: number;
  userId: string;
  postCount: number;
  commentsCount: number;
  isGrouped: boolean;
  userName?: string;
  postTitle?: string;
}

export enum CommentNotificationType {
  REPORTER = 1,
  COMMENT = 2,
  REPLY = 3,
}

export interface CommentNotificationCallbacks {
  onNotificationBuilt: (bundle: { [key: string]: any }) => void;
}

export interface CommentNotificationConfig {
  title: string;
  body: string;
  notificationType: CommentNotificationType;
  notificationInterval: string;
  postId: string;
  reporterId: number;
  userId: string;
  postCount: number;
  commentsCount: number;
  isGrouped?: boolean;
}

// Constants for comment notifications
export const COMMENT_NOTIFICATION_CONSTANTS = {
  COMMENTS_SUMMARY_NOTIFICATION_ID: 9999,
  COMMENTS_NOTIFICATION_ID: 99999,
  COMMENTS_GROUP_NOTIFICATION_ID: "comments_group_notification",
  NOTIFICATION_INTERVAL_INSTANT: "instant",
  NOTIFICATION_INTERVAL_HOURLY: "hourly",
  NOTIFICATION_INTERVAL_DAILY: "daily",
} as const;

export type CommentNotificationInterval =
  | typeof COMMENT_NOTIFICATION_CONSTANTS.NOTIFICATION_INTERVAL_INSTANT
  | typeof COMMENT_NOTIFICATION_CONSTANTS.NOTIFICATION_INTERVAL_HOURLY
  | typeof COMMENT_NOTIFICATION_CONSTANTS.NOTIFICATION_INTERVAL_DAILY;
