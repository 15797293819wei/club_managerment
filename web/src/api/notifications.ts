import http from './http';
import type { PageResult } from './types';

export interface Notification {
  id: number;
  userId: number;
  type: string;
  title: string;
  content: string;
  relatedId?: number;
  readFlag: number; // 0 未读，1 已读
  createdTime: string;
  readTime?: string;
}

export interface NotificationPageQuery {
  onlyUnread?: boolean;
  page?: number;
  size?: number;
}

export function listNotifications(params: NotificationPageQuery) {
  return http.get<PageResult<Notification>>('/notifications', {
    params
  }) as unknown as Promise<PageResult<Notification>>;
}

export function getUnreadNotificationCount() {
  return http.get<number>('/notifications/unread-count') as unknown as Promise<number>;
}

export function markNotificationRead(id: number) {
  return http.post<void>(`/notifications/${id}/read`) as unknown as Promise<void>;
}

export function markAllNotificationsRead() {
  return http.post<void>('/notifications/read-all') as unknown as Promise<void>;
}

