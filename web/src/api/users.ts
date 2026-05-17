import http from './http';
import type { PageResult } from './types';

export interface UserSummary {
  id: number;
  username: string;
  realName?: string;
  studentId?: string;
  email?: string;
  phone?: string;
  status: number;
  createdTime?: string;
}

export interface UserDetail extends UserSummary {
  gender?: number;
  avatar?: string;
  roles: Array<{
    id: number;
    roleCode: string;
    roleName: string;
  }>;
}

export interface UserPageQuery {
  page?: number;
  size?: number;
  keyword?: string;
  username?: string;
  studentId?: string;
  roleCode?: string;
  status?: number;
}

export interface UserSavePayload {
  username: string;
  password: string;
  realName: string;
  studentId?: string;
  email?: string;
  phone?: string;
  gender?: number;
  avatar?: string;
  status?: number;
  roleIds?: number[];
}

export interface UserUpdatePayload {
  username?: string;
  password?: string;
  realName?: string;
  studentId?: string;
  email?: string;
  phone?: string;
  gender?: number;
  avatar?: string;
  status?: number;
  roleIds?: number[] | null;
}

export interface UserStatusPayload {
  status: number;
}

export interface UserBatchStatusPayload {
  userIds: number[];
  status: number;
}

export interface ResetUserPasswordPayload {
  newPassword: string;
}

export interface AssignRolesPayload {
  roleIds: number[];
}

export function listUsers(params: UserPageQuery) {
  return http.get<PageResult<UserSummary>>('/users', {
    params
  }) as unknown as Promise<PageResult<UserSummary>>;
}

export function getUser(id: number) {
  return http.get<UserDetail>(`/users/${id}`) as unknown as Promise<UserDetail>;
}

export function createUser(data: UserSavePayload) {
  return http.post<{ id: number }>('/users', data) as unknown as Promise<{ id: number }>;
}

export function updateUser(id: number, data: UserUpdatePayload) {
  return http.put<void>(`/users/${id}`, data) as unknown as Promise<void>;
}

export function changeUserStatus(id: number, data: UserStatusPayload) {
  return http.patch<void>(`/users/${id}/status`, data) as unknown as Promise<void>;
}

export function batchChangeUserStatus(data: UserBatchStatusPayload) {
  return http.post<void>('/users/status/batch', data) as unknown as Promise<void>;
}

export function resetUserPassword(id: number, data: ResetUserPasswordPayload) {
  return http.post<void>(`/users/${id}/reset-password`, data) as unknown as Promise<void>;
}

export function assignUserRoles(id: number, data: AssignRolesPayload) {
  return http.put<void>(`/users/${id}/roles`, data) as unknown as Promise<void>;
}

export function deleteUser(id: number) {
  return http.delete<void>(`/users/${id}`) as unknown as Promise<void>;
}

