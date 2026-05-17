import http from './http';
import type { PageResult } from './types';

export type AnnouncementScopeType = 'GLOBAL' | 'CLUB';

export interface AnnouncementView {
  id: number;
  title: string;
  content: string;
  scopeType: AnnouncementScopeType;
  clubId?: number;
  clubName?: string;
  publisherName: string;
  publisherRole: string;
  createdTime: string;
}

export interface AnnouncementPageQuery {
  page?: number;
  size?: number;
}

export interface AnnouncementManageQuery extends AnnouncementPageQuery {
  scopeType?: AnnouncementScopeType;
  clubId?: number;
}

export interface PublishAnnouncementPayload {
  title: string;
  content: string;
  clubId?: number;
}

// 当前用户可见公告列表（系统公告 + 所在社团公告）
export function listVisibleAnnouncements(params: AnnouncementPageQuery) {
  return http.get<PageResult<AnnouncementView>>('/announcements', {
    params
  }) as unknown as Promise<PageResult<AnnouncementView>>;
}

// 管理端分页公告列表（系统管理员 / 社团管理员）
export function listManageAnnouncements(params: AnnouncementManageQuery) {
  return http.get<PageResult<AnnouncementView>>('/announcements/manage', {
    params
  }) as unknown as Promise<PageResult<AnnouncementView>>;
}

// 发布公告
export function publishAnnouncement(data: PublishAnnouncementPayload) {
  return http.post<void>('/announcements', data) as unknown as Promise<void>;
}

