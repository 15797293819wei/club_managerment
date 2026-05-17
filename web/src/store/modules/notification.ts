import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  getUnreadNotificationCount,
  listNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  type Notification
} from '@/api/notifications';

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0);
  const latest = ref<Notification[]>([]);

  async function fetchUnreadCount() {
    unreadCount.value = await getUnreadNotificationCount();
  }

  async function fetchLatest(limit = 5) {
    const res = await listNotifications({ page: 1, size: limit });
    latest.value = res.records || [];
  }

  async function markRead(id: number) {
    await markNotificationRead(id);
    await fetchUnreadCount();
  }

  async function markAllRead() {
    await markAllNotificationsRead();
    unreadCount.value = 0;
  }

  return {
    unreadCount,
    latest,
    fetchUnreadCount,
    fetchLatest,
    markRead,
    markAllRead
  };
});

