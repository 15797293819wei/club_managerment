import http from './http';
import type { UserProfile } from '@/store/modules/auth';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export function login(data: LoginPayload) {
  return http.post<LoginResponse>('/auth/login', data) as unknown as Promise<LoginResponse>;
}

export function fetchCurrentUser() {
  return http.get<UserProfile>('/auth/me') as unknown as Promise<UserProfile>;
}

export interface RegisterPayload {
  username: string;
  password: string;
  confirmPassword: string;
  realName: string;
  studentId?: string;
  email?: string;
  phone?: string;
}

export interface RegisterResponse {
  id: number;
}

export function register(data: RegisterPayload) {
  return http.post<RegisterResponse>('/auth/register', data) as unknown as Promise<RegisterResponse>;
}

export interface ForgotPasswordPayload {
  username: string;
  email?: string;
  phone?: string;
}

export interface ForgotPasswordResponse {
  message: string;
  token?: string; // 仅用于测试，实际应用中不应该返回
}

export function forgotPassword(data: ForgotPasswordPayload) {
  return http.post<ForgotPasswordResponse>('/auth/forgot-password', data) as unknown as Promise<ForgotPasswordResponse>;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

export function resetPassword(data: ResetPasswordPayload) {
  return http.post<void>('/auth/reset-password', data) as unknown as Promise<void>;
}

export interface ValidatePasswordPayload {
  password: string;
}

export interface ValidatePasswordResponse {
  valid: boolean;
  message: string;
  strength: number;
}

export function validatePassword(data: ValidatePasswordPayload) {
  return http.post<ValidatePasswordResponse>('/auth/validate-password', data) as unknown as Promise<ValidatePasswordResponse>;
}