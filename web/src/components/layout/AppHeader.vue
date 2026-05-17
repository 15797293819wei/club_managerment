<template>
  <header class="header">
    <div class="right">
      <el-badge :value="unreadCount" :hidden="!unreadCount" class="mr12">
        <el-button text @click="goMessages">消息</el-button>
      </el-badge>
      <span class="username">{{ auth.user?.realName || auth.user?.username || '未登录' }}</span>
      <el-button text @click="logout" v-if="auth.isAuthenticated">退出</el-button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { useNotificationStore } from '@/store/modules/notification';

const auth = useAuthStore();
const notificationStore = useNotificationStore();
const router = useRouter();

const unreadCount = computed(() => notificationStore.unreadCount);

function logout() {
  auth.logout();
}

function goMessages() {
  router.push({ name: 'MessageCenter' });
}

onMounted(() => {
  notificationStore.fetchUnreadCount().catch(() => {});
});
</script>

<style scoped>
.header {
  height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #111827;
  color: #e5e7eb;
}

.right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.username {
  font-size: 14px;
}

.mr12 {
  margin-right: 12px;
}
</style>

