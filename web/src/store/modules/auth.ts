import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import router from '@/router';
import { login, fetchCurrentUser } from '@/api/auth';
import { getJoinedClubs } from '@/api/clubs';
import { getToken, setToken, clearToken } from '@/utils/auth';

export interface UserProfile {
  id: number;
  username: string;
  realName?: string;
  roles: string[];
}

const CLUB_ROLE_PRIORITY: Record<string, number> = {
  MEMBER: 1,
  STAFF: 2,
  VICE_MINISTER: 3,
  MINISTER: 4,
  PRESIDENT: 5
};

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken());
  const user = ref<UserProfile | null>(null);
  const loading = ref(false);
  const clubRoles = ref<string[]>([]);

  // 是否已登录：仅在 token 和用户信息同时存在时视为登录
  const isAuthenticated = computed(() => Boolean(token.value && user.value));
  const upperRoles = computed(() => (user.value?.roles || []).map((r) => r.toUpperCase()));
  const isSystemAdmin = computed(() => upperRoles.value.includes('SYSTEM_ADMIN'));
  const highestClubRole = computed(() => {
    if (!clubRoles.value.length) {
      return null;
    }
    return clubRoles.value.reduce<string | null>((prev, role) => {
      const code = role.toUpperCase();
      if (!CLUB_ROLE_PRIORITY[code]) {
        return prev;
      }
      if (!prev) {
        return code;
      }
      return CLUB_ROLE_PRIORITY[code] > CLUB_ROLE_PRIORITY[prev] ? code : prev;
    }, null);
  });
  const clubRoleLevel = computed(() =>
    highestClubRole.value ? CLUB_ROLE_PRIORITY[highestClubRole.value] : 0
  );
  const hasClubRole = computed(() => clubRoleLevel.value > 0);
  const isClubPresident = computed(() => highestClubRole.value === 'PRESIDENT');

  function hasPermission(required?: string | string[]) {
    if (!required) return true;
    const need = Array.isArray(required) ? required : [required];
    return need.some((r) => matchPermission(r));
  }

  function matchPermission(code: string) {
    const normalized = code.toUpperCase();
    switch (normalized) {
      case 'CLUB_MEMBER':
        return clubRoleLevel.value >= CLUB_ROLE_PRIORITY.MEMBER;
      case 'CLUB_STAFF':
        return clubRoleLevel.value >= CLUB_ROLE_PRIORITY.STAFF;
      case 'CLUB_MANAGER':
        return clubRoleLevel.value >= CLUB_ROLE_PRIORITY.VICE_MINISTER;
      case 'CLUB_PRESIDENT':
        return clubRoleLevel.value >= CLUB_ROLE_PRIORITY.PRESIDENT;
      case 'CLUB_ADMIN':
        return clubRoleLevel.value >= CLUB_ROLE_PRIORITY.VICE_MINISTER;
      case 'NON_SYSTEM':
      case 'NON_SYSTEM_ADMIN':
        return !isSystemAdmin.value;
      default:
        return upperRoles.value.includes(normalized);
    }
  }

  async function loginWithPassword(username: string, password: string) {
    loading.value = true;
    try {
      const response = await login({ username, password });
      setToken(response.token);
      token.value = response.token;
      await fetchProfile();
    } finally {
      loading.value = false;
    }
  }

  async function fetchProfile() {
    if (!token.value) {
      throw new Error('missing token');
    }
    const profile = await fetchCurrentUser();
    user.value = profile;
    try {
      await refreshClubRoles();
    } catch (error) {
      console.warn('加载社团角色失败', error);
    }
    return profile;
  }

  async function refreshClubRoles() {
    if (!token.value) {
      clubRoles.value = [];
      return;
    }
    try {
      const clubs = await getJoinedClubs();
      clubRoles.value = clubs
        .map((club) => club.role?.toUpperCase() || '')
        .filter((role) => Boolean(role) && Boolean(CLUB_ROLE_PRIORITY[role]));
    } catch (error) {
      clubRoles.value = [];
      throw error;
    }
  }

  function logout() {
    clearToken();
    token.value = null;
    user.value = null;
    clubRoles.value = [];
    router.push({ name: 'Login' });
  }

  return {
    token,
    user,
    loading,
    isAuthenticated,
    hasPermission,
    isSystemAdmin,
    highestClubRole,
    hasClubRole,
    isClubPresident,
    refreshClubRoles,
    loginWithPassword,
    fetchProfile,
    logout
  };
});
