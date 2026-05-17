<template>
  <div class="message-center">
    <el-card header="消息中心">
      <div class="toolbar">
        <el-radio-group v-model="onlyUnread" size="small" @change="reload">
          <el-radio-button :value="false">全部</el-radio-button>
          <el-radio-button :value="true">未读</el-radio-button>
        </el-radio-group>
        <div class="toolbar-right">
          <span class="unread">未读：{{ unreadCount }}</span>
          <el-button type="primary" text @click="markAll">全部标记为已读</el-button>
        </div>
      </div>

      <el-table :data="records" :loading="loading" size="small" @row-click="view">
        <el-table-column type="index" width="60" label="#" align="center" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="180">
          <template #default="{ row }">
            {{ typeText[row.type] || row.type }}
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="时间" width="180">
          <template #default="{ row }">
            {{ row.createdTime?.replace('T', ' ') }}
          </template>
        </el-table-column>
        <el-table-column prop="readFlag" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.readFlag === 1 ? 'info' : 'success'">
              {{ row.readFlag === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button text size="small" @click="view(row)">查看</el-button>
            <el-button
              text
              size="small"
              type="primary"
              :disabled="row.readFlag === 1"
              @click="mark(row)"
            >
              标记已读
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialog.visible" title="消息详情" width="520px">
      <div v-if="dialog.record">
        <h3 class="dialog-title">{{ dialog.record.title }}</h3>
        <p class="dialog-meta">
          {{ dialog.record.createdTime?.replace('T', ' ') }} ·
          {{ typeText[dialog.record.type] || dialog.record.type }}
        </p>
        <p class="dialog-content">
          {{ dialog.record.content }}
        </p>
      </div>
      <template #footer>
        <el-button @click="dialog.visible = false">关闭</el-button>
        <template v-if="canReviewDissolution">
          <el-button type="success" @click="handleApproveDissolution">同意解散</el-button>
          <el-button type="danger" @click="handleRejectDissolution">驳回</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead,
  type Notification
} from '@/api/notifications';
import { reviewClubDissolution } from '@/api/clubs';
import { useAuthStore } from '@/store/modules/auth';

const onlyUnread = ref<boolean | null>(false);
const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);
const unreadCount = ref(0);
const records = ref<Notification[]>([]);

const authStore = useAuthStore();
const isSystemAdmin = computed(() => authStore.isSystemAdmin);

const dialog = reactive<{
  visible: boolean;
  record: Notification | null;
}>({
  visible: false,
  record: null
});

const isDissolutionApplication = computed(() => {
  return dialog.record?.type === 'CLUB_DISSOLUTION_APPLICATION';
});

const canReviewDissolution = computed(() => {
  return isSystemAdmin.value && isDissolutionApplication.value;
});

const typeText: Record<string, string> = {
  MEMBER_APPLICATION_RESULT: '入社申请结果',
  CLUB_APPLICATION_RESULT: '社团申请结果',
  CLUB_DISSOLUTION_APPLICATION: '社团解散申请',
  ACTIVITY_REGISTRATION: '活动报名通知',
  ACTIVITY_REMINDER: '活动提醒',
  EVALUATION_NOTICE: '评价提醒',
  ROLE_CHANGED: '社团角色变更通知',
  ANNOUNCEMENT: '公告通知'
};

async function load() {
  loading.value = true;
  try {
    const res = await listNotifications({
      onlyUnread: onlyUnread.value || undefined,
      page: page.value,
      size: size.value
    });
    records.value = res.records || [];
    total.value = res.total || 0;
    unreadCount.value = await getUnreadNotificationCount();
  } finally {
    loading.value = false;
  }
}

function reload() {
  page.value = 1;
  load();
}

function handlePageChange(p: number) {
  page.value = p;
  load();
}

function handleSizeChange(s: number) {
  size.value = s;
  page.value = 1;
  load();
}

function view(row: Notification) {
  dialog.record = row;
  dialog.visible = true;
  if (row.readFlag === 0) {
    mark(row);
  }
}

async function mark(row: Notification) {
  if (row.readFlag === 1) return;
  await markNotificationRead(row.id);
  row.readFlag = 1;
  unreadCount.value = await getUnreadNotificationCount();
}

async function markAll() {
  await markAllNotificationsRead();
  ElMessage.success('已全部标记为已读');
  reload();
}

async function handleApproveDissolution() {
  if (!dialog.record?.relatedId) {
    ElMessage.error('无法获取申请ID');
    return;
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入审核意见（可选）', '同意解散社团', {
      inputPlaceholder: '审核意见',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    });
    await reviewClubDissolution(dialog.record.relatedId, {
      status: 1,
      reviewComment: value?.trim() || undefined
    });
    ElMessage.success('已同意解散申请');
    dialog.visible = false;
    reload();
  } catch {
    // 用户取消或出错时不额外提示
  }
}

async function handleRejectDissolution() {
  if (!dialog.record?.relatedId) {
    ElMessage.error('无法获取申请ID');
    return;
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因（必填）', '驳回解散申请', {
      inputPlaceholder: '驳回原因',
      inputValidator: (val: string) => !!val && val.trim().length > 0,
      inputErrorMessage: '驳回原因不能为空',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    });
    await reviewClubDissolution(dialog.record.relatedId, {
      status: 2,
      reviewComment: value.trim()
    });
    ElMessage.success('已驳回解散申请');
    dialog.visible = false;
    reload();
  } catch {
    // 用户取消或出错时不额外提示
  }
}

onMounted(() => {
  load();
});
</script>

<style scoped>
.message-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.unread {
  font-size: 13px;
  color: #ef4444;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.dialog-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}

.dialog-meta {
  margin: 0 0 12px;
  color: #6b7280;
  font-size: 14px;
}

.dialog-content {
  white-space: pre-wrap;
  font-size: 15px;
  line-height: 1.6;
}
</style>
