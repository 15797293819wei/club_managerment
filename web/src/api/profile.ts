import http from './http';

export interface RoleSimple {
  id: number;
  roleCode: string;
  roleName: string;
}

export interface ProfileDetail {
  id: number;
  username: string;
  realName?: string;
  email?: string;
  phone?: string;
  gender?: number;
  avatar?: string;
  roles?: RoleSimple[];
}

export interface JoinedClubSummary {
  id: number;
  clubName: string;
  clubCode?: string;
  description?: string;
  logo?: string;
  role?: string;
  memberCount?: number;
  status?: number;
  createdTime?: string;
}

export interface ActivityRecord {
  id: number;
  activityId: number;
  clubId?: number;
  clubName?: string;
  activityName?: string;
  startTime?: string;
  endTime?: string;
  registrationTime?: string;
  activityStatus?: number;
  registrationStatus?: number;
}

export interface AttendanceRecord {
  id: number;
  clubId?: number;
  clubName?: string;
  activityId?: number;
  activityName?: string;
  attendanceType: number;
  attendanceTime?: string;
  remark?: string;
}

export interface EvaluationRecord {
  id: number;
  activityId: number;
  activityName?: string;
  clubName?: string;
  rating: number;
  comment?: string;
  createdTime?: string;
}

export interface PersonalStats {
  joinedClubCount: number;
  activityCount: number;
  attendanceCount: number;
  signInCount: number;
  evaluationCount: number;
  signInRate: number;
  averageRating?: number;
}

export interface PersonalOverview {
  joinedClubs: JoinedClubSummary[];
  activities: ActivityRecord[];
  attendance: AttendanceRecord[];
  evaluations: EvaluationRecord[];
  stats: PersonalStats;
}

export interface UpdateProfilePayload {
  realName: string;
  email?: string;
  phone?: string;
  gender?: number;
  avatar?: string;
}

export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
}

export function getProfile() {
  return http.get<ProfileDetail>('/profile') as unknown as Promise<ProfileDetail>;
}

export function updateProfile(data: UpdateProfilePayload) {
  return http.put<void>('/profile', data) as unknown as Promise<void>;
}

export function changePassword(data: ChangePasswordPayload) {
  return http.put<void>('/profile/password', data) as unknown as Promise<void>;
}

export function getProfileOverview() {
  return http.get<PersonalOverview>('/profile/overview') as unknown as Promise<PersonalOverview>;
}


