<template>
  <div class="activity-detail">
    <div class="header-actions mb16">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>
    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item variant="h1" style="width: 60%" />
        <el-skeleton-item variant="text" style="width: 80%" />
        <el-skeleton-item variant="text" style="width: 40%" />
      </template>
      <template #default>
        <el-card v-if="activity" class="mb16" :header="activity.activityName">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="所属社团">{{ activity.clubName || activity.clubId }}</el-descriptions-item>
            <el-descriptions-item label="活动状态">
              <el-tag :type="statusTagType[activity.status] || 'info'">
                {{ statusText[activity.status] || '未知' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatDate(activity.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">
              {{ activity.endTime ? formatDate(activity.endTime) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="报名截止">
              {{ activity.registrationDeadline ? formatDate(activity.registrationDeadline) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="地点">{{ activity.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="人数限制" :span="2">
              <span>
                {{ activity.currentParticipants ?? 0 }}
                <span v-if="activity.maxParticipants"> / {{ activity.maxParticipants }}</span>
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="活动类型" :span="2">
              {{ activity.activityType || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="活动简介" :span="2">
              {{ activity.description || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="actions">
            <el-tag v-if="countdownText" type="warning">{{ countdownText }}</el-tag>
            <el-tag v-if="signedIn" type="success">
              已签到 {{ mySignRecord?.signInTime ? formatDate(mySignRecord?.signInTime) : '' }}
            </el-tag>
            <el-button type="primary" @click="handleRegister" :loading="actionLoading.register" v-if="canRegister">
              报名参加
            </el-button>
            <el-button
              type="warning"
              @click="handleCancelRegistration"
              :loading="actionLoading.cancel"
              v-if="canCancelRegistration"
            >
              取消报名
            </el-button>
            <el-button type="primary" @click="openSignDialog" :loading="actionLoading.signIn" v-if="canSignIn">
              签到打卡
            </el-button>
            <el-button type="success" @click="openEvaluationDialog" :loading="actionLoading.evaluate" v-if="canEvaluate">
              发布评价
            </el-button>
            <el-tag v-if="registrationInfoText" type="info">{{ registrationInfoText }}</el-tag>
          </div>
        </el-card>
        <el-empty v-else description="未找到活动信息" />
      </template>
    </el-skeleton>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="评分统计" class="mb16">
          <div v-if="rating">
            <div class="rating">
              <div class="rating__value">{{ rating.averageRating.toFixed(1) }}</div>
              <div class="rating__meta">共 {{ rating.count }} 条评价</div>
            </div>
          </div>
          <el-empty v-else description="暂无评分数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="我的状态" class="mb16">
          <ul class="status-list">
            <li>
              报名状态：
              <strong>{{ registrationStatusText }}</strong>
            </li>
            <li>
              签到状态：
              <strong>{{ signedIn ? '已签到' : '未签到' }}</strong>
              <span v-if="mySignRecord" class="status-extra">（{{ formatDate(mySignRecord.signInTime) }}）</span>
            </li>
            <li>
              我的评价：
              <strong>{{ userEvaluation ? `${userEvaluation.rating} 分` : '未评价' }}</strong>
            </li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <el-card header="活动评价">
      <el-table :data="evaluationRecords" stripe class="evaluation-table">
        <el-table-column type="index" label="#" width="60" align="center" header-align="center" />
        <el-table-column prop="username" label="用户名" width="160" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column label="评分" width="150" align="center" header-align="center">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="评价内容" align="center" header-align="center">
          <template #default="{ row }">
            <div class="comment-main">{{ row.comment || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="200" align="center" header-align="center">
          <template #default="{ row }">{{ formatDate(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column v-if="isManager" label="操作" width="120" align="center" header-align="center">
          <template #default="{ row }">
            <el-button text type="danger" size="small" @click="handleDeleteEvaluation(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="evaluationPage.page"
          :page-size="evaluationPage.size"
          :total="evaluationPage.total"
          @current-change="handleEvaluationPageChange"
        />
      </div>
    </el-card>

    <el-card header="报名与签到统计" class="mb16" v-if="activity && isManager">
      <div class="stat-line">
        <span>已报名：{{ registrationList.length }} 人</span>
        <span>已签到：{{ signedCount }} 人</span>
        <span>未签到：{{ unsignedCount }} 人</span>
      </div>
      <el-row :gutter="12">
        <el-col :span="12">
          <div class="sub-title">已签到名单</div>
          <el-table :data="signedRegistrations" size="small" class="registration-table">
            <el-table-column type="index" label="#" width="48" align="center" header-align="center" />
            <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip align="center" header-align="center" />
            <el-table-column label="签到时间" width="160" align="center" header-align="center">
              <template #default="{ row }">{{ formatDate(row.signInTime) }}</template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :span="12">
          <div class="sub-title">未签到名单</div>
          <el-table :data="unsignedRegistrations" size="small" class="registration-table">
            <el-table-column type="index" label="#" width="48" align="center" header-align="center" />
            <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip align="center" header-align="center" />
            <el-table-column label="报名时间" width="160" align="center" header-align="center">
              <template #default="{ row }">{{ formatDate(row.registrationTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" header-align="center">
              <template #default="{ row }">
                <el-button
                  text
                  type="primary"
                  size="small"
                  :disabled="!canManagerSignIn"
                  @click="handleManagerSignIn(row)"
                >
                  代签
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="signDialog.visible" title="活动签到" width="400px">
      <el-form :model="signDialog.form" :rules="signRules" ref="signFormRef" label-width="110px">
        <el-form-item label="签到方式" prop="signInType">
          <el-radio-group v-model="signDialog.form.signInType">
            <el-radio :value="1">手动签到</el-radio>
            <el-radio :value="2">二维码签到</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="签到地点" prop="location">
          <el-input v-model="signDialog.form.location" placeholder="二维码签到可填写地点" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="signDialog.form.remark" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="signDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading.signIn" @click="handleSignSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="evaluationDialog.visible" title="发布评价" width="400px">
      <el-form :model="evaluationDialog.form" :rules="evaluationRules" ref="evaluationFormRef" label-width="90px">
        <el-form-item label="评分" prop="rating">
          <el-rate v-model="evaluationDialog.form.rating" :max="5" />
        </el-form-item>
        <el-form-item label="评价内容" prop="comment">
          <el-input v-model="evaluationDialog.form.comment" type="textarea" :rows="4" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evaluationDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading.evaluate" @click="handleEvaluationSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { useAuthStore } from '@/store/modules/auth';
import {
  getActivity,
  getActivityRating,
  listRegistrations,
  listSignIns,
  listEvaluations,
  registerActivity,
  cancelRegistration,
  signInActivity,
  managerSignInActivity,
  evaluateActivity,
  deleteEvaluation,
  type Activity,
  type RatingStatistics,
  type EvaluationRecord,
  type Registration,
  type SignInRecord
} from '@/api/activities';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const activityId = Number(route.params.id);

function goBack() {
  router.back();
}

const loading = ref(false);
const activity = ref<Activity | null>(null);
const rating = ref<RatingStatistics | null>(null);
const evaluationRecords = ref<EvaluationRecord[]>([]);
const evaluationPage = reactive({ page: 1, size: 10, total: 0 });
const registrationList = ref<Registration[]>([]);
const signedUserIds = ref<Set<number>>(new Set());

const registrationStatus = ref<'NONE' | 'REGISTERED' | 'CANCELLED'>('NONE');
const signedIn = ref(false);
const mySignRecord = ref<SignInRecord | null>(null);
const userEvaluation = ref<EvaluationRecord | null>(null);

const actionLoading = reactive({
  register: false,
  cancel: false,
  signIn: false,
  evaluate: false
});

const signDialog = reactive({
  visible: false,
  form: {
    signInType: 1,
    location: '',
    remark: ''
  }
});

const evaluationDialog = reactive({
  visible: false,
  form: {
    rating: 5,
    comment: ''
  }
});

const signFormRef = ref();
const evaluationFormRef = ref();

const signRules = {
  signInType: [{ required: true, message: '请选择签到方式', trigger: 'change' }]
};

const evaluationRules = {
  rating: [{ required: true, message: '请给出评分', trigger: 'change' }],
  comment: [{ max: 300, message: '评价内容不能超过 300 字', trigger: 'blur' }]
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

const countdownText = computed(() => {
  if (!activity.value?.startTime) return '';
  const start = new Date(activity.value.startTime).getTime();
  const now = Date.now();
  if (start <= now) return '';
  const diffMinutes = Math.round((start - now) / 60000);
  if (diffMinutes <= 0) return '';
  if (diffMinutes < 60) return `距离开始还有 ${diffMinutes} 分钟`;
  const hours = Math.floor(diffMinutes / 60);
  const minutes = diffMinutes % 60;
  return `距离开始还有 ${hours} 小时 ${minutes} 分钟`;
});

const isManager = computed(() => authStore.hasPermission('SYSTEM_ADMIN') || authStore.hasPermission('CLUB_ADMIN'));

const registrationStatusText = computed(() => {
  if (registrationStatus.value === 'REGISTERED') return '已报名';
  if (registrationStatus.value === 'CANCELLED') return '已取消';
  return '未报名';
});

const registrationInfoText = computed(() => {
  if (registrationStatus.value === 'REGISTERED') {
    if (signedIn.value) return '已签到';
    return '报名成功，记得准时参加';
  }
  if (registrationStatus.value === 'CANCELLED') {
    return '您已取消报名';
  }
  return '';
});

const canRegister = computed(() => {
  if (!activity.value) return false;
  return activity.value.status === 0 && registrationStatus.value !== 'REGISTERED';
});

const canCancelRegistration = computed(() => {
  if (!activity.value) return false;
  return activity.value.status === 0 && registrationStatus.value === 'REGISTERED';
});

const canSignIn = computed(() => {
  if (!activity.value) return false;
  return activity.value.status === 1 && registrationStatus.value === 'REGISTERED' && !signedIn.value;
});

const canEvaluate = computed(() => {
  if (!activity.value) return false;
  return activity.value.status === 2 && registrationStatus.value === 'REGISTERED' && !userEvaluation.value;
});

const signedRegistrations = computed(() =>
  registrationList.value.filter(item => item.status === 1 && signedUserIds.value.has(item.userId))
);
const unsignedRegistrations = computed(() =>
  registrationList.value.filter(item => item.status === 1 && !signedUserIds.value.has(item.userId))
);
const signedCount = computed(() => signedRegistrations.value.length);
const unsignedCount = computed(() => unsignedRegistrations.value.length);
const canManagerSignIn = computed(() => !!activity.value && activity.value.status === 1);

async function loadActivity() {
  loading.value = true;
  try {
    activity.value = await getActivity(activityId);
  } finally {
    loading.value = false;
  }
}

async function loadRating() {
  try {
    rating.value = await getActivityRating(activityId);
  } catch (err) {
    rating.value = null;
  }
}

async function loadRegistrationStatus() {
  const userId = authStore.user?.id;
  if (!userId) return;
  const res = await listRegistrations({ activityId, userId, page: 1, size: 1 });
  const record = res.records?.[0];
  if (!record) {
    registrationStatus.value = 'NONE';
  } else if (record.status === 1) {
    registrationStatus.value = 'REGISTERED';
  } else {
    registrationStatus.value = 'CANCELLED';
  }
}

async function loadSignStatus() {
  const userId = authStore.user?.id;
  if (!userId) return;
  const res = await listSignIns({ activityId, userId, page: 1, size: 1 });
  const record = res.records?.[0];
  signedIn.value = !!record;
  mySignRecord.value = record || null;
}

async function loadUserEvaluation() {
  const userId = authStore.user?.id;
  if (!userId) {
    userEvaluation.value = null;
    return;
  }
  const res = await listEvaluations({ activityId, userId, page: 1, size: 1 });
  userEvaluation.value = res.records?.[0] || null;
}

async function loadEvaluations() {
  const res = await listEvaluations({
    activityId,
    page: evaluationPage.page,
    size: evaluationPage.size
  });
  evaluationRecords.value = res.records || [];
  evaluationPage.total = res.total || 0;
}

async function initialize() {
  if (!Number.isFinite(activityId)) {
    ElMessage.error('无效的活动 ID');
    return;
  }
  await Promise.all([
    loadActivity(),
    loadRating(),
    loadRegistrationStatus(),
    loadSignStatus(),
    loadUserEvaluation(),
    loadEvaluations(),
    loadRegistrationsForManager()
  ]);
}

async function loadRegistrationsForManager() {
  if (!isManager.value) {
    registrationList.value = [];
    signedUserIds.value = new Set();
    return;
  }
  const res = await listRegistrations({ activityId, page: 1, size: 100 });
  const registrations = res.records || [];
  registrationList.value = registrations;
  const signRes = await listSignIns({ activityId, page: 1, size: 100 });
  const set = new Set<number>();
  (signRes.records || []).forEach(item => set.add(item.userId));
  signedUserIds.value = set;
}

async function handleRegister() {
  actionLoading.register = true;
  try {
    await registerActivity(activityId);
    ElMessage.success('报名成功');
    await Promise.all([loadActivity(), loadRegistrationStatus()]);
  } finally {
    actionLoading.register = false;
  }
}

async function handleCancelRegistration() {
  actionLoading.cancel = true;
  try {
    await cancelRegistration(activityId);
    ElMessage.success('已取消报名');
    await Promise.all([loadActivity(), loadRegistrationStatus()]);
  } finally {
    actionLoading.cancel = false;
  }
}

function openSignDialog() {
  signDialog.visible = true;
  signDialog.form.signInType = 1;
  signDialog.form.location = '';
  signDialog.form.remark = '';
}

async function handleSignSubmit() {
  await signFormRef.value?.validate();
  actionLoading.signIn = true;
  try {
    await signInActivity(activityId, {
      signInType: signDialog.form.signInType,
      location: signDialog.form.location || undefined,
      remark: signDialog.form.remark || undefined
    });
    ElMessage.success('签到成功');
    signDialog.visible = false;
    await Promise.all([loadSignStatus(), loadRegistrationsForManager()]);
  } finally {
    actionLoading.signIn = false;
  }
}

async function handleManagerSignIn(row: Registration) {
  try {
    await ElMessageBox.confirm(`确认为「${row.username || row.userId}」进行代签吗？`, '提示', {
      type: 'warning'
    });
    actionLoading.signIn = true;
    await managerSignInActivity(activityId, {
      userId: row.userId,
      signInType: 1,
      remark: '管理员代签'
    });
    ElMessage.success('已代签成功');
    await Promise.all([loadSignStatus(), loadRegistrationsForManager()]);
  } finally {
    actionLoading.signIn = false;
  }
}

function openEvaluationDialog() {
  evaluationDialog.visible = true;
  evaluationDialog.form.rating = 5;
  evaluationDialog.form.comment = '';
}

async function handleEvaluationSubmit() {
  await evaluationFormRef.value?.validate();
  actionLoading.evaluate = true;
  try {
    await evaluateActivity(activityId, {
      rating: evaluationDialog.form.rating,
      comment: evaluationDialog.form.comment?.trim() || undefined
    });
    ElMessage.success('评价已提交');
    evaluationDialog.visible = false;
    await Promise.all([loadRating(), loadUserEvaluation(), loadEvaluations()]);
  } finally {
    actionLoading.evaluate = false;
  }
}

async function handleDeleteEvaluation(row: EvaluationRecord) {
  try {
    await ElMessageBox.confirm('确认删除该评价吗？', '提示', { type: 'warning' });
    await deleteEvaluation(row.id);
    ElMessage.success('评价已删除');
    await Promise.all([loadRating(), loadEvaluations()]);
  } catch {
    // 用户取消或删除失败时无需额外提示
  }
}

function handleEvaluationPageChange(page: number) {
  evaluationPage.page = page;
  loadEvaluations();
}

function formatDate(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ');
}

onMounted(initialize);
</script>

<style scoped>
.activity-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb16 {
  margin-bottom: 16px;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.rating {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
}

.rating__value {
  font-size: 36px;
  font-weight: bold;
  color: #f97316;
}

.rating__meta {
  color: #6b7280;
}

.status-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-extra {
  margin-left: 4px;
  color: #6b7280;
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.evaluation-table :deep(.el-table) {
  font-size: 14px;
}

.evaluation-table :deep(.el-table th) {
  padding: 8px 0;
}

.evaluation-table :deep(.el-table td) {
  padding: 8px 0;
}
</style>


