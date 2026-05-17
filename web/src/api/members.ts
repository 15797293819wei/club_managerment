import http from './http';
import type { PageResult } from './types';

export interface Member {
  id: number;
  clubId: number;
  clubName?: string;
  userId: number;
  username?: string;
  role: string;
  status: number;
  joinTime?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface MemberDetail {
  basic: Member;
  profile: {
    userId: number;
    username: string;
    realName?: string;
    studentId?: string;
    email?: string;
    phone?: string;
    avatar?: string;
    gender?: number;
  };
  joinedClubs: Array<{
    id: number;
    clubName: string;
    clubCode?: string;
    description?: string;
    logo?: string;
    founderId?: number;
    founderUsername?: string;
    memberCount?: number;
    status?: number;
    role: string;
    createdTime?: string;
    updatedTime?: string;
  }>;
  participation?: {
    registrationCount: number;
    signInCount: number;
    participationRate: number;
  };
}

export interface MemberStats {
  totalMembers: number;
  inactiveMembers: number;
  newMembers: number;
  activeMembers: number;
  roleDistribution: Record<string, number>;
  topParticipants: Array<{
    userId: number;
    username?: string;
    realName?: string;
    registrationCount: number;
    signInCount: number;
    participationRate: number;
  }>;
}

export interface MemberQuery {
  clubId?: number;
  userId?: number;
  role?: string;
  status?: number;
  page?: number;
  size?: number;
}

export function listMembers(params: MemberQuery) {
  return http.get<PageResult<Member>>('/members', { params }) as unknown as Promise<PageResult<Member>>;
}

export interface MemberSelfState {
  joined: boolean;
  memberId?: number;
  role?: string;
  pendingApplication: boolean;
}

export function getMemberSelfState(clubId: number) {
  return http.get<MemberSelfState>('/members/self/state', { params: { clubId } }) as unknown as Promise<MemberSelfState>;
}

export function assignMemberRole(memberId: number, role: string) {
  return http.put(`/members/${memberId}/role`, { role }) as unknown as Promise<void>;
}

export function changeMemberStatus(memberId: number, status: number) {
  return http.patch(`/members/${memberId}/status`, { status }) as unknown as Promise<void>;
}

export function deleteMember(memberId: number) {
  return http.delete(`/members/${memberId}`) as unknown as Promise<void>;
}

export function quitClub(memberId: number) {
  return http.post(`/members/${memberId}/quit`) as unknown as Promise<void>;
}

export function getMemberDetail(memberId: number) {
  return http.get<MemberDetail>(`/members/${memberId}/overview`) as unknown as Promise<MemberDetail>;
}

export function getMemberStats(clubId: number, recentDays = 30) {
  return http.get<MemberStats>(`/members/club/${clubId}/stats`, {
    params: { recentDays }
  }) as unknown as Promise<MemberStats>;
}

export interface BatchAssignMemberRolePayload {
  clubId: number;
  memberIds: number[];
  role: string;
}

export function batchAssignMemberRole(data: BatchAssignMemberRolePayload) {
  return http.post<void>('/members/batch/role', data) as unknown as Promise<void>;
}

export interface BatchRemoveMembersPayload {
  clubId: number;
  memberIds: number[];
}

export function batchRemoveMembers(data: BatchRemoveMembersPayload) {
  return http.post<void>('/members/batch/remove', data) as unknown as Promise<void>;
}

export interface ExportMembersParams {
  clubId: number;
  role?: string;
  status?: number;
}

export function exportMembers(params: ExportMembersParams) {
  return http.get<Blob>('/members/export', {
    params,
    responseType: 'blob' as any
  }) as unknown as Promise<Blob>;
}

export interface MemberApplication {
  id: number;
  clubId: number;
  clubName?: string;
  applicantId: number;
  applicantUsername?: string;
  applicationReason?: string;
  status: number;
  reviewComment?: string;
  createdTime?: string;
}

export interface MemberApplicationQuery {
  clubId?: number;
  status?: number;
  page?: number;
  size?: number;
}

export function listMemberApplications(params: MemberApplicationQuery) {
  return http.get<PageResult<MemberApplication>>('/member-applications', { params }) as unknown as Promise<PageResult<MemberApplication>>;
}

export interface MemberApplicationSubmitPayload {
  clubId: number;
  applicationReason?: string;
}

export function submitMemberApplication(data: MemberApplicationSubmitPayload) {
  return http.post<{ id: number }>('/member-applications', data);
}

export function reviewMemberApplication(id: number, status: 1 | 2, reviewComment?: string) {
  return http.put(`/member-applications/${id}/review`, { status, reviewComment }) as unknown as Promise<void>;
}

export interface BatchReviewMemberApplicationsPayload {
  clubId: number;
  applicationIds: number[];
  status: 1 | 2;
  reviewComment?: string;
}

export function batchReviewMemberApplications(data: BatchReviewMemberApplicationsPayload) {
  return http.post<void>('/member-applications/batch-review', data);
}

export interface MemberApplicationInspection {
  application: MemberApplication & {
    reviewerId?: number;
    reviewTime?: string;
    updatedTime?: string;
  };
  applicant: {
    userId: number;
    username: string;
    realName?: string;
    studentId?: string;
    email?: string;
    phone?: string;
    avatar?: string;
  };
  otherClubs: MemberDetail['joinedClubs'];
  participation?: {
    userId: number;
    username?: string;
    realName?: string;
    registrationCount: number;
    signInCount: number;
    participationRate: number;
  };
  history: Array<{
    id: number;
    clubId: number;
    status: number;
    reviewComment?: string;
    createdTime?: string;
    updatedTime?: string;
  }>;
  alreadyMember: boolean;
}

export function getMemberApplicationInspection(id: number) {
  return http.get<MemberApplicationInspection>(`/member-applications/${id}/insight`);
}

export interface Attendance {
  id: number;
  clubId: number;
  clubName?: string;
  activityId?: number;
  activityName?: string;
  memberId: number;
  memberUsername?: string;
  attendanceType: number;
  attendanceTime?: string;
  remark?: string;
  exceptionFlag?: boolean;
  exceptionStatus?: number;
  exceptionReason?: string;
  handledBy?: number;
  handledTime?: string;
  createdTime?: string;
}

export interface AttendanceQuery {
  clubId?: number;
  memberId?: number;
  activityId?: number;
  startTime?: string;
  endTime?: string;
  page?: number;
  size?: number;
}

export function listAttendance(params: AttendanceQuery) {
  return http.get<PageResult<Attendance>>('/attendances', { params }) as unknown as Promise<PageResult<Attendance>>;
}

export interface AttendanceCreatePayload {
  clubId: number;
  memberId: number;
  activityId?: number;
  attendanceType: number;
  attendanceTime?: string;
  remark?: string;
}

export function createAttendance(data: AttendanceCreatePayload) {
  return http.post<{ id: number }>('/attendances', data) as unknown as Promise<{ id: number }>;
}

export interface AttendanceStatsResponse {
  summary: {
    totalCount: number;
    normalCount: number;
    lateCount: number;
    leaveEarlyCount: number;
    absentCount: number;
    exceptionCount: number;
  };
  memberStats: AttendanceDimensionStat[];
  activityStats: AttendanceDimensionStat[];
  dateStats: AttendanceDimensionStat[];
}

export interface AttendanceDimensionStat {
  refId?: number;
  refName: string;
  totalCount: number;
  normalCount: number;
  lateCount: number;
  leaveEarlyCount: number;
  absentCount: number;
  exceptionCount: number;
}

export interface AttendanceStatsParams {
  clubId: number;
  startTime?: string;
  endTime?: string;
}

export function getAttendanceStats(params: AttendanceStatsParams) {
  return http.get<AttendanceStatsResponse>('/attendances/stats', { params }) as unknown as Promise<AttendanceStatsResponse>;
}

export interface AttendanceExportParams {
  clubId: number;
  activityId?: number;
  memberId?: number;
  startTime?: string;
  endTime?: string;
}

export function exportAttendance(params: AttendanceExportParams) {
  return http.get<Blob>('/attendances/export', {
    params,
    responseType: 'blob' as any
  }) as unknown as Promise<Blob>;
}

export function markAttendanceException(id: number, reason: string) {
  return http.post<void>(`/attendances/${id}/exception`, { reason }) as unknown as Promise<void>;
}

export function resolveAttendanceException(id: number, reason: string) {
  return http.post<void>(`/attendances/${id}/exception/resolve`, { reason }) as unknown as Promise<void>;
}

export interface AttendanceRule {
  clubId: number;
  lateThreshold: number;
  leaveEarlyThreshold: number;
  absenceThreshold: number;
}

export function getAttendanceRule(clubId: number) {
  return http.get<AttendanceRule>(`/attendances/rules/${clubId}`) as unknown as Promise<AttendanceRule>;
}

export function saveAttendanceRule(data: AttendanceRule) {
  return http.post<void>('/attendances/rules', data) as unknown as Promise<void>;
}

