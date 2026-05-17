import http from './http';

export interface ClubActivitySummary {
  clubId: number;
  clubName: string;
  activityCount: number;
  registrationCount: number;
  signInCount: number;
  totalParticipants: number;
}

export interface ClubActivityTrendPoint {
  period: string;
  activityCount: number;
  registrationCount: number;
  signInCount: number;
}

export interface MemberParticipation {
  userId: number;
  username: string;
  realName?: string;
  registrationCount: number;
  signInCount: number;
  participationRate: number;
}

export interface ClubActivityParams {
  clubId?: number;
  startTime?: string;
  endTime?: string;
}

export interface ClubActivityTrendParams extends ClubActivityParams {
  granularity?: 'DAY' | 'WEEK' | 'MONTH';
}

export interface MemberParticipationParams {
  clubId?: number;
  startTime?: string;
  endTime?: string;
  limit?: number;
}

export interface MemberDetailParams {
  clubId?: number;
  startTime?: string;
  endTime?: string;
}

export interface DashboardStats {
  pendingTasks: number;
  upcomingActivities: number;
  recentMembers: number;
}

export function getClubActivitySummary(params: ClubActivityParams) {
  return http.get<ClubActivitySummary[]>('/stats/club-activity/summary', {
    params
  }) as unknown as Promise<ClubActivitySummary[]>;
}

export function getClubActivityTrend(params: ClubActivityTrendParams) {
  return http.get<ClubActivityTrendPoint[]>('/stats/club-activity/trend', {
    params
  }) as unknown as Promise<ClubActivityTrendPoint[]>;
}

export function getMemberParticipationRanking(params: MemberParticipationParams) {
  return http.get<MemberParticipation[]>('/stats/member-participation/ranking', {
    params
  }) as unknown as Promise<MemberParticipation[]>;
}

export function getMemberParticipationDetail(userId: number, params: MemberDetailParams) {
  return http.get<MemberParticipation>(`/stats/member-participation/${userId}`, {
    params
  }) as unknown as Promise<MemberParticipation>;
}

export interface DashboardStatsParams extends ClubActivityParams {}

export function getDashboardStats(params?: DashboardStatsParams) {
  return http.get<DashboardStats>('/stats/dashboard', { params }) as unknown as Promise<DashboardStats>;
}

