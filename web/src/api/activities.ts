import http from './http';
import type { PageResult } from './types';

export interface Activity {
  id: number;
  clubId: number;
  clubName?: string;
  activityName: string;
  activityType?: string;
  description?: string;
  startTime: string;
  endTime?: string;
  location?: string;
  maxParticipants?: number;
  currentParticipants?: number;
  registrationDeadline?: string;
  status: number;
  creatorId?: number;
  createdTime?: string;
  updatedTime?: string;
}

export interface ActivityQuery {
  clubId?: number;
  clubName?: string;
  keyword?: string;
  status?: number;
  page?: number;
  size?: number;
}

export interface ActivityPayload {
  clubId: number;
  activityName: string;
  activityType?: string;
  description?: string;
  startTime: string;
  endTime?: string;
  location?: string;
  maxParticipants?: number;
  registrationDeadline?: string;
}

export function listActivities(params: ActivityQuery) {
  return http.get<PageResult<Activity>>('/activities', { params }) as unknown as Promise<PageResult<Activity>>;
}

export function getActivity(id: number) {
  return http.get<Activity>(`/activities/${id}`) as unknown as Promise<Activity>;
}

export function createActivity(data: ActivityPayload) {
  return http.post<{ id: number }>('/activities', data) as unknown as Promise<{ id: number }>;
}

export function updateActivity(id: number, data: Partial<ActivityPayload>) {
  return http.put<void>(`/activities/${id}`, data) as unknown as Promise<void>;
}

export function cancelActivity(id: number) {
  return http.patch<void>(`/activities/${id}/cancel`) as unknown as Promise<void>;
}

export function deleteActivity(id: number) {
  return http.delete<void>(`/activities/${id}`) as unknown as Promise<void>;
}

export interface Registration {
  id: number;
  activityId: number;
  userId: number;
  username?: string;
  registrationTime: string;
  status: number;
  createdTime?: string;
  updatedTime?: string;
}

export interface RegistrationQuery {
  activityId?: number;
  userId?: number;
  status?: number;
  page?: number;
  size?: number;
}

export function listRegistrations(params: RegistrationQuery) {
  return http.get<PageResult<Registration>>('/activities/registrations', { params }) as unknown as Promise<PageResult<Registration>>;
}

export function registerActivity(id: number) {
  return http.post<{ id: number }>(`/activities/${id}/register`) as unknown as Promise<{ id: number }>;
}

export function cancelRegistration(id: number) {
  return http.patch<void>(`/activities/${id}/cancel-registration`) as unknown as Promise<void>;
}

export interface SignInRecord {
  id: number;
  activityId: number;
  userId: number;
  username?: string;
  signInTime: string;
  signInType: number;
  location?: string;
  remark?: string;
  createdTime?: string;
}

export interface SignInQuery {
  activityId?: number;
  userId?: number;
  page?: number;
  size?: number;
}

export interface SignInPayload {
  signInType: number;
  location?: string;
  remark?: string;
}

export interface ManagerSignInPayload extends SignInPayload {
  userId: number;
}

export function listSignIns(params: SignInQuery) {
  return http.get<PageResult<SignInRecord>>('/activities/sign-ins', { params }) as unknown as Promise<PageResult<SignInRecord>>;
}

export function signInActivity(id: number, data: SignInPayload) {
  return http.post<{ id: number }>(`/activities/${id}/sign-in`, data) as unknown as Promise<{ id: number }>;
}

export function managerSignInActivity(id: number, data: ManagerSignInPayload) {
  return http.post<{ id: number }>(`/activities/${id}/sign-in/manager`, data) as unknown as Promise<{ id: number }>;
}

export interface EvaluationRecord {
  id: number;
  activityId: number;
  userId: number;
  username?: string;
  rating: number;
  comment?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface EvaluationQuery {
  activityId?: number;
  userId?: number;
  page?: number;
  size?: number;
}

export interface EvaluationPayload {
  rating: number;
  comment?: string;
}

export function listEvaluations(params: EvaluationQuery) {
  return http.get<PageResult<EvaluationRecord>>('/activities/evaluations', { params }) as unknown as Promise<PageResult<EvaluationRecord>>;
}

export function evaluateActivity(id: number, data: EvaluationPayload) {
  return http.post<{ id: number }>(`/activities/${id}/evaluate`, data) as unknown as Promise<{ id: number }>;
}

export function deleteEvaluation(id: number) {
  return http.delete<void>(`/activities/evaluations/${id}`) as unknown as Promise<void>;
}

export interface RatingStatistics {
  activityId: number;
  averageRating: number;
  count: number;
}

export function getActivityRating(id: number) {
  return http.get<RatingStatistics>(`/activities/${id}/rating-statistics`) as unknown as Promise<RatingStatistics>;
}


