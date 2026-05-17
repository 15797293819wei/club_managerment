<template>
  <div class="activities">
    <el-card class="mb12" header="活动管理">
      <div class="toolbar mb12">
        <el-input
          v-model="filters.keyword"
          class="w200"
          placeholder="搜索活动名称"
          clearable
          @keyup.enter="resetAndLoad"
          @clear="resetAndLoad"
        />
        <el-input
          v-model="filters.clubName"
          class="w200"
          placeholder="请输入社团名"
          clearable
          @keyup.enter="resetAndLoad"
          @clear="resetAndLoad"
        />
        <el-select v-model="filters.status" class="w180" placeholder="状态筛选" clearable @change="resetAndLoad">
          <el-option :value="0" label="待开始" />
          <el-option :value="1" label="进行中" />
          <el-option :value="2" label="已结束" />
          <el-option :value="3" label="已取消" />
        </el-select>
        <el-button type="primary" @click="resetAndLoad">查询</el-button>
        <el-button type="primary" v-permission="managePermission" @click="openCreate">发布活动</el-button>
      </div>

      <CommonTable :data="records" :loading="loading" class="activities-table">
        <el-table-column type="index" label="#" width="60" align="center" header-align="center" />
        <el-table-column prop="activityName" label="活动名称" min-width="180" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column prop="clubName" label="社团名称" width="180" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column label="时间" min-width="220" align="center" header-align="center">
          <template #default="{ row }">
            <div>{{ formatDate(row.startTime) }}</div>
            <div v-if="row.endTime" class="text-sub">至 {{ formatDate(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="160" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column label="人数" width="160" align="center" header-align="center">
          <template #default="{ row }">
            {{ row.currentParticipants ?? 0 }}<span v-if="row.maxParticipants"> / {{ row.maxParticipants }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'">{{ statusText[row.status] || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="380" align="center" header-align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="toDetail(row.id)">详情</el-button>
            <el-button
              text
              type="primary"
              size="small"
              v-permission="managePermission"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              text
              type="primary"
              size="small"
              v-permission="managePermission"
              @click="openCopy(row)"
            >
              复制
            </el-button>
            <el-button
              text
              type="warning"
              size="small"
              v-permission="managePermission"
              :disabled="row.status === 2 || row.status === 3"
              @click="doCancel(row)"
            >
              取消
            </el-button>
            <el-button
              text
              type="danger"
              size="small"
              v-permission="managePermission"
              @click="doDelete(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </CommonTable>

      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="pagination.page"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.editing ? '编辑活动' : '发布活动'" width="640px">
      <CommonForm :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="社团名称" prop="clubId">
          <el-select 
            v-model="form.clubId" 
            placeholder="请选择社团" 
            filterable
            style="width: 100%"
            :loading="clubLoading"
          >
            <el-option
              v-for="club in clubOptions"
              :key="club.id"
              :label="club.clubName"
              :value="club.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="form.activityName" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="活动类型" prop="activityType">
          <el-input v-model="form.activityType" placeholder="如：讲座、竞赛" />
        </el-form-item>
        <el-form-item label="活动时间" required>
          <el-space direction="vertical" style="width: 100%">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              placeholder="开始时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
            />
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              placeholder="结束时间（可选）"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              clearable
            />
          </el-space>
        </el-form-item>
        <el-form-item label="报名截止时间" prop="registrationDeadline">
          <el-date-picker
            v-model="form.registrationDeadline"
            type="datetime"
            placeholder="报名截止时间（可选）"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            clearable
          />
        </el-form-item>
        <el-form-item label="活动地点" prop="location">
          <el-input v-model="form.location" placeholder="如：学生活动中心 A 区" />
        </el-form-item>
        <el-form-item label="人数上限" prop="maxParticipants">
          <el-input-number
            v-model="form.maxParticipants"
            :min="1"
            :step="1"
            controls-position="right"
            style="width: 100%"
            placeholder="留空表示不限制"
          />
        </el-form-item>
        <el-form-item label="活动简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="活动简介或安排"
          />
        </el-form-item>
      </CommonForm>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormRules } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonTable from '@/components/common/CommonTable.vue';
import CommonForm from '@/components/common/CommonForm.vue';
import {
  listActivities,
  createActivity,
  updateActivity,
  cancelActivity,
  deleteActivity,
  type Activity,
  type ActivityPayload,
  type ActivityQuery
} from '@/api/activities';
import { getManagedClubs, type Club } from '@/api/clubs';

const router = useRouter();

const managePermission = ['CLUB_ADMIN', 'SYSTEM_ADMIN'];

const filters = reactive({
  keyword: '',
  clubName: '',
  status: undefined as number | undefined
});

const pagination = reactive({ page: 1, size: 10 });
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const records = ref<Activity[]>([]);
const clubLoading = ref(false);
const clubOptions = ref<Club[]>([]);

const dialog = reactive({ visible: false, editing: false });
const formRef = ref<InstanceType<typeof CommonForm> | null>(null);
const form = reactive<{
  id?: number;
  clubId: number | null;
  activityName: string;
  activityType: string;
  description: string;
  startTime: string;
  endTime: string;
  location: string;
  maxParticipants: number | null;
  registrationDeadline: string;
}>({
  id: undefined,
  clubId: null,
  activityName: '',
  activityType: '',
  description: '',
  startTime: '',
  endTime: '',
  location: '',
  maxParticipants: null,
  registrationDeadline: ''
});

const rules: FormRules = {
  clubId: [{ required: true, message: '请选择社团', trigger: 'change', type: 'number' as const }],
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  description: [{ max: 500, message: '简介不能超过 500 个字符', trigger: 'blur' }]
};

const statusText: Record<number, string> = {
  0: '待开始',
  1: '进行中',
  2: '已结束',
  3: '已取消'
};
const statusTagType: Record<number, 'info' | 'success' | 'warning' | 'danger'> = {
  0: 'info',
  1: 'success',
  2: 'warning',
  3: 'danger'
};

async function load() {
  loading.value = true;
  try {
    const res = await listActivities({
      keyword: filters.keyword?.trim() || undefined,
      clubName: filters.clubName?.trim() || undefined,
      status: filters.status,
      page: pagination.page,
      size: pagination.size
    });
    records.value = res.records || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
}

function resetAndLoad() {
  pagination.page = 1;
  load();
}

function handlePageChange(page: number) {
  pagination.page = page;
  load();
}

function handleSizeChange(size: number) {
  pagination.size = size;
  pagination.page = 1;
  load();
}

function openCreate() {
  formRef.value?.resetFields();
  dialog.editing = false;
  form.id = undefined;
  form.clubId = null;
  form.activityName = '';
  form.activityType = '';
  form.description = '';
  form.startTime = '';
  form.endTime = '';
  form.location = '';
  form.maxParticipants = null;
  form.registrationDeadline = '';
  dialog.visible = true;
}

function openCopy(row: Activity) {
  formRef.value?.resetFields();
  dialog.editing = false;
  form.id = undefined;
  form.clubId = row.clubId;
  form.activityName = `${row.activityName}（复制）`;
  form.activityType = row.activityType || '';
  form.description = row.description || '';
  form.startTime = '';
  form.endTime = '';
  form.location = row.location || '';
  form.maxParticipants = row.maxParticipants ?? null;
  form.registrationDeadline = '';
  dialog.visible = true;
}

function openEdit(row: Activity) {
  formRef.value?.resetFields();
  dialog.editing = true;
  form.id = row.id;
  form.clubId = row.clubId;
  form.activityName = row.activityName;
  form.activityType = row.activityType || '';
  form.description = row.description || '';
  form.startTime = row.startTime;
  form.endTime = row.endTime || '';
  form.location = row.location || '';
  form.maxParticipants = row.maxParticipants ?? null;
  form.registrationDeadline = row.registrationDeadline || '';
  dialog.visible = true;
}

async function save() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (form.clubId == null) {
      ElMessage.error('请填写社团 ID');
      return;
    }
    const payload: ActivityPayload = {
      clubId: Number(form.clubId),
      activityName: form.activityName.trim(),
      activityType: form.activityType?.trim() || undefined,
      description: form.description?.trim() || undefined,
      startTime: form.startTime,
      endTime: form.endTime || undefined,
      location: form.location?.trim() || undefined,
      maxParticipants: form.maxParticipants ? Number(form.maxParticipants) : undefined,
      registrationDeadline: form.registrationDeadline || undefined
    };
    if (dialog.editing && form.id) {
      await updateActivity(form.id, payload);
      ElMessage.success('活动已更新');
    } else {
      await createActivity(payload);
      ElMessage.success('活动已发布');
    }
    dialog.visible = false;
    await load();
  } finally {
    saving.value = false;
  }
}

function toDetail(id: number) {
  router.push({ name: 'ActivityDetail', params: { id } });
}

function doDelete(id: number) {
  ElMessageBox.confirm('确认删除该活动？该操作不可撤销。', '警告', { type: 'warning' })
    .then(async () => {
      await deleteActivity(id);
      ElMessage.success('活动已删除');
      if (records.value.length === 1 && pagination.page > 1) {
        pagination.page -= 1;
      }
      await load();
    })
    .catch(() => {});
}

function doCancel(row: Activity) {
  ElMessageBox.confirm('确认取消该活动？取消后成员将无法继续报名。', '提示', { type: 'warning' })
    .then(async () => {
      await cancelActivity(row.id);
      ElMessage.success('活动已取消');
      await load();
    })
    .catch(() => {});
}

async function loadClubs() {
  clubLoading.value = true;
  try {
    // 使用 getManagedClubs API 获取当前用户管理的社团
    const managedClubs = await getManagedClubs();
    clubOptions.value = managedClubs;
    
    // 如果只有一个社团，自动选中
    if (managedClubs.length === 1) {
      form.clubId = managedClubs[0].id;
    }
  } catch (err) {
    console.error('加载社团列表失败:', err);
  } finally {
    clubLoading.value = false;
  }
}

function formatDate(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ');
}

onMounted(async () => {
  await loadClubs();
  await load();
});
</script>

<style scoped>
.mb12 {
  margin-bottom: 8px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.w160 {
  width: 160px;
}

.w180 {
  width: 180px;
}

.w200 {
  width: 200px;
}

.text-sub {
  color: #9ca3af;
  font-size: 12px;
}

.activities-table :deep(.el-table) {
  font-size: 14px;
}

.activities-table :deep(.el-table th) {
  padding: 8px 0;
}

.activities-table :deep(.el-table td) {
  padding: 8px 0;
}

.activities-table :deep(.el-button) {
  margin: 0 4px;
}
</style>


