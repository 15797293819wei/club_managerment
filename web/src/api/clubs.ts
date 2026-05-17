import http from './http';
import type { PageResult } from './types';

export interface Club {
  id: number;
  clubName: string;
  clubCode?: string;
  description?: string;
  status?: number;
  founderId?: number;
  founderUsername?: string;
  logo?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface ClubPageQuery {
  keyword?: string;
  status?: number;
  minMembers?: number;
  maxMembers?: number;
  sortBy?: 'memberCount' | 'establishedTime' | 'createdTime';
  sortOrder?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export interface ClubSavePayload {
  clubName: string;
  description?: string;
  clubCode?: string;
}

export function listClubs(params: ClubPageQuery) {
  return http.get<PageResult<Club>>('/clubs', { params }) as unknown as Promise<PageResult<Club>>;
}

export interface PublicClubQuery {
  keyword?: string;
  minMembers?: number;
  maxMembers?: number;
  sortBy?: 'memberCount' | 'establishedTime' | 'createdTime';
  sortOrder?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export function listPublicClubs(params: PublicClubQuery) {
  return http.get<PageResult<Club>>('/clubs/public', { params }) as unknown as Promise<PageResult<Club>>;
}

export function getClub(id: number) {
  return http.get<Club>(`/clubs/${id}`) as unknown as Promise<Club>;
}

export function createClub(data: ClubSavePayload) {
  return http.post('/clubs', data) as unknown as Promise<void>;
}

export function updateClub(id: number, data: Partial<ClubSavePayload>) {
  return http.put(`/clubs/${id}`, data) as unknown as Promise<void>;
}

export function deleteClub(id: number) {
  return http.delete(`/clubs/${id}`) as unknown as Promise<void>;
}

export function changeClubStatus(id: number, status: number) {
  return http.patch<void>(`/clubs/${id}/status`, { status });
}

export interface DissolveClubPayload {
  reason: string;
}

export function dissolveClub(id: number, data: DissolveClubPayload) {
  return http.post<void>(`/clubs/${id}/dissolve`, data) as unknown as Promise<void>;
}

export interface ClubDissolutionApplication {
  id: number;
  clubId: number;
  clubName?: string;
  applicantId: number;
  applicantUsername?: string;
  reason: string;
  status: number; // 0-待审核，1-已通过，2-已驳回
  reviewerId?: number;
  reviewTime?: string;
  reviewComment?: string;
  createdTime?: string;
}

export interface ReviewDissolutionPayload {
  status: 1 | 2; // 1-通过，2-驳回
  reviewComment?: string;
}

export function reviewClubDissolution(id: number, data: ReviewDissolutionPayload) {
  return http.put<void>(`/club-dissolutions/${id}/review`, data) as unknown as Promise<void>;
}

export interface ClubApplication {
  id: number;
  applicantId: number;
  applicantUsername?: string;
  clubName: string;
  clubCode?: string;
  description?: string;
  purpose?: string;
  constitution?: string;
  logo?: string;
  attachment?: string;
  status: number;
  reviewerId?: number;
  reviewTime?: string;
  reviewComment?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface ClubApplicationQuery {
  keyword?: string;
  status?: number;
  page?: number;
  size?: number;
}

export function listClubApplications(params: ClubApplicationQuery) {
  return http.get<PageResult<ClubApplication>>('/club-applications', { params }) as unknown as Promise<PageResult<ClubApplication>>;
}

export interface ClubApplicationSubmitPayload {
  clubName: string;
  clubCode?: string;
  description?: string;
  purpose?: string;
  constitution?: string;
  logo?: string;
  attachment?: string;
}

export function submitClubApplication(data: ClubApplicationSubmitPayload) {
  return http.post<{ id: number }>('/club-applications', data);
}

export function canSubmitClubApplication() {
  return http.get<boolean>('/club-applications/can-submit') as unknown as Promise<boolean>;
}

export function reviewClubApplication(id: number, status: 1 | 2, reviewComment?: string) {
  return http.put(`/club-applications/${id}/review`, { status, reviewComment }) as unknown as Promise<void>;
}

export interface BatchReviewClubApplicationsPayload {
  applicationIds: number[];
  status: 1 | 2;
  reviewComment?: string;
}

export function batchReviewClubApplications(data: BatchReviewClubApplicationsPayload) {
  return http.post<void>('/club-applications/batch-review', data);
}

export interface ClubApplicationHistoryEntry {
  id: number;
  applicationId: number;
  reviewerId: number;
  reviewerName?: string;
  status: number;
  reviewComment?: string;
  createdTime: string;
}

export function fetchClubApplicationHistory(id: number) {
  return http.get<ClubApplicationHistoryEntry[]>(`/club-applications/${id}/history`);
}

export interface TransferClubAdminPayload {
  targetUserId: number;
}

export function transferClubAdmin(clubId: number, data: TransferClubAdminPayload) {
  return http.put<void>(`/clubs/${clubId}/transfer-admin`, data);
}

export function getManagedClubs() {
  return http.get<Club[]>('/clubs/managed') as unknown as Promise<Club[]>;
}

export interface JoinedClub {
  id: number;
  clubName: string;
  clubCode?: string;
  description?: string;
  logo?: string;
  founderId?: number;
  founderUsername?: string;
  memberCount?: number;
  status?: number;
  role: string; // MEMBER, STAFF, VICE_MINISTER, MINISTER
  createdTime?: string;
  updatedTime?: string;
}

export function getJoinedClubs() {
  return http.get<JoinedClub[]>('/clubs/joined') as unknown as Promise<JoinedClub[]>;
}

export interface ClubLeaderboardItem {
  clubId: number;
  clubName: string;
  metricValue: number;
  metricLabel: string;
}

export interface ClubLeaderboardParams {
  type?: 'member_count' | 'activity_count';
  limit?: number;
}

export function fetchClubLeaderboard(params?: ClubLeaderboardParams) {
  return http.get<ClubLeaderboardItem[]>('/clubs/leaderboard', { params }) as unknown as Promise<ClubLeaderboardItem[]>;
}

