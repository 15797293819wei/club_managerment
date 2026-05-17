import http from './http';
import type { PageResult } from './types';

export interface RoleInfo {
  id: number;
  roleCode: string;
  roleName: string;
  description?: string;
  status: number;
}

export interface RoleSimple {
  id: number;
  roleCode: string;
  roleName: string;
}

export interface RolePageQuery {
  page?: number;
  size?: number;
  keyword?: string;
  status?: number;
}

export interface RoleSavePayload {
  roleCode: string;
  roleName: string;
  description?: string;
  status?: number;
}

export interface RoleUpdatePayload {
  roleCode?: string;
  roleName?: string;
  description?: string;
  status?: number;
}

export interface RoleStatusPayload {
  status: number;
}

export function listRoles(params: RolePageQuery) {
  return http.get<PageResult<RoleInfo>>('/roles', {
    params
  }) as unknown as Promise<PageResult<RoleInfo>>;
}

export function getRole(id: number) {
  return http.get<RoleInfo>(`/roles/${id}`) as unknown as Promise<RoleInfo>;
}

export function listActiveRoles() {
  return http.get<RoleSimple[]>('/roles/active') as unknown as Promise<RoleSimple[]>;
}

export function createRole(data: RoleSavePayload) {
  return http.post<{ id: number }>('/roles', data) as unknown as Promise<{ id: number }>;
}

export function updateRole(id: number, data: RoleUpdatePayload) {
  return http.put<void>(`/roles/${id}`, data) as unknown as Promise<void>;
}

export function changeRoleStatus(id: number, data: RoleStatusPayload) {
  return http.patch<void>(`/roles/${id}/status`, data) as unknown as Promise<void>;
}

export function deleteRole(id: number) {
  return http.delete<void>(`/roles/${id}`) as unknown as Promise<void>;
}

