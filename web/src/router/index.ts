import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import routes from './routes';
import { useAuthStore } from '@/store/modules/auth';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL || '/'),
  routes: routes as RouteRecordRaw[]
});

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore();
  const isPublic = to.meta?.public === true;

  // 如果本地有 token 但还没有加载用户信息，先尝试恢复登录状态
  if (!auth.user && auth.token) {
    try {
      await auth.fetchProfile();
    } catch {
      // token 失效等情况，清理并回到登录页
      auth.logout();
      next({ name: 'Login', query: { redirect: to.fullPath } });
      return;
    }
  }

  // 受保护路由必须登录（且已加载用户信息）
  if (!isPublic && !auth.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
    return;
  }

  // 已登录访问登录页时，直接跳到首页
  if (to.name === 'Login' && auth.isAuthenticated) {
    next({ name: 'Dashboard' });
    return;
  }

  next();
});

export default router;

