<template>
  <div class="sidebar-menu">
    <div class="logo">社团管理系统</div>
    <el-menu
      :default-active="activeMenu"
      class="menu"
      background-color="#111827"
      text-color="#e5e7eb"
      active-text-color="#60a5fa"
      router
    >
      <el-menu-item
        v-for="item in menuRoutes"
        :key="menuIndex(item)"
        :index="menuIndex(item)"
        v-permission="item.meta?.permission"
      >
        <span>{{ item.meta?.title }}</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import routes from '@/router/routes';
import type { RouteRecordRaw } from 'vue-router';

const route = useRoute();

const root = routes.find((r) => r.path === '/');
const menuRoutes = computed(() =>
  (root?.children || []).filter((r) => !r.meta?.hidden && r.meta?.menu)
);

function menuIndex(item: RouteRecordRaw): string {
  const base = root?.path === '/' ? '' : root?.path || '';
  const childPath = (item.path as string) || '';
  const full = childPath ? `${base}/${childPath}` : base || '/';
  // 规范化斜杠，避免出现多个连续斜杠
  return full.replace(/\/+/g, '/');
}

const activeMenu = computed(() => route.path || '/');
</script>

<style scoped>
.sidebar-menu {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.logo {
  height: 56px;
  min-width: 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  background-color: #0f172a;
  border-bottom: 1px solid #1f2937;
}

.menu {
  flex: 1;
  border-right: none;
}

.menu :deep(.el-menu-item) {
  font-size: 15px;
  justify-content: center;
}
</style>

