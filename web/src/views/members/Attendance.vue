<template>
  <div class="attendance-page">
    <el-card class="mb16" header="查询条件">
      <div class="toolbar">
        <el-select
          v-model="filters.clubId"
          placeholder="社团名称"
          class="w200"
          filterable
          clearable
          :loading="clubLoading"
          @change="handleClubChange"
        >
          <el-option
            v-for="club in clubOptions"
            :key="club.id"
            :label="club.clubName"
            :value="club.id"
          />
        </el-select>
        <el-input
          v-model="filters.memberKeyword"
          placeholder="成员用户名"
          class="w180"
          clearable
          @keyup.enter="resetAndLoad"
          @clear="resetAndLoad"
        />
        <el-input
          v-model="filters.activityId"
          placeholder="活动 ID"
          class="w160"
          clearable
          @keyup.enter="resetAndLoad"
          @clear="resetAndLoad"
        />
        <el-date-picker
          v-model="filters.dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          class="w320"
        />
        <el-button type="primary" :disabled="!filters.clubId" @click="resetAndLoad">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="success" :loading="exportLoading" :disabled="!filters.clubId" @click="handleExport">导出 CSV</el-button>
        <el-button type="warning" :disabled="!filters.clubId" @click="openManualDialog">补录考勤</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="mb16" v-if="stats">
      <el-col :span="6" v-for="card in summaryCards" :key="card.label">
        <el-card>
          <div class="summary-card">
            <div class="summary-label">{{ card.label }}</div>
            <div class="summary-value">{{ card.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mb16" header="考勤统计" v-if="filters.clubId">
      <el-tabs v-model="statsTab">
        <el-tab-pane label="按成员" name="member">
          <el-table :data="stats?.memberStats || []" size="small" height="300px">
            <el-table-column type="index" width="50" label="#" align="center" />
            <el-table-column prop="refName" label="成员" />
            <el-table-column prop="totalCount" label="总次数" width="100" align="center" />
            <el-table-column prop="normalCount" label="正常" width="100" align="center" />
            <el-table-column prop="lateCount" label="迟到" width="100" align="center" />
            <el-table-column prop="leaveEarlyCount" label="早退" width="100" align="center" />
            <el-table-column prop="absentCount" label="缺勤" width="100" align="center" />
            <el-table-column prop="exceptionCount" label="异常" width="100" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="按活动" name="activity">
          <el-table :data="stats?.activityStats || []" size="small" height="300px">
            <el-table-column type="index" width="50" label="#" align="center" />
            <el-table-column prop="refName" label="活动/场景" />
            <el-table-column prop="totalCount" label="总次数" width="100" align="center" />
            <el-table-column prop="normalCount" label="正常" width="100" align="center" />
            <el-table-column prop="lateCount" label="迟到" width="100" align="center" />
            <el-table-column prop="leaveEarlyCount" label="早退" width="100" align="center" />
            <el-table-column prop="absentCount" label="缺勤" width="100" align="center" />
            <el-table-column prop="exceptionCount" label="异常" width="100" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="按日期" name="date">
          <el-table :data="stats?.dateStats || []" size="small" height="300px">
            <el-table-column prop="refName" label="日期" />
            <el-table-column prop="totalCount" label="总次数" width="100" align="center" />
            <el-table-column prop="normalCount" label="正常" width="100" align="center" />
            <el-table-column prop="lateCount" label="迟到" width="100" align="center" />
            <el-table-column prop="leaveEarlyCount" label="早退" width="100" align="center" />
            <el-table-column prop="absentCount" label="缺勤" width="100" align="center" />
            <el-table-column prop="exceptionCount" label="异常" width="100" align="center" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card v-if="filters.clubId" header="考勤记录">
      <CommonTable :data="records" :loading="loading" class="attendance-table">
        <el-table-column type="index" label="#" width="60" align="center" header-align="center" />
        <el-table-column prop="memberUsername" label="成员" width="160" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column prop="clubName" label="社团名称" width="180" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column prop="activityName" label="活动名称" min-width="140" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column prop="attendanceType" label="考勤类型" width="120" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType[row.attendanceType] || 'info'">
              {{ typeText[row.attendanceType] || row.attendanceType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="attendanceTime" label="打卡时间" width="200" align="center" header-align="center" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column label="异常状态" width="150" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag v-if="row.exceptionFlag" :type="row.exceptionStatus === 1 ? 'success' : 'danger'">
              {{ row.exceptionStatus === 1 ? '已处理' : '待处理' }}
            </el-tag>
            <el-tag v-else type="info">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常说明" prop="exceptionReason" width="200" show-overflow-tooltip align="center" header-align="center" />
        <el-table-column label="操作" width="200" align="center" header-align="center">
          <template #default="{ row }">
            <el-button text type="warning" size="small" @click="handleMarkException(row)">标记异常</el-button>
            <el-button
              text
              type="success"
              size="small"
              :disabled="row.exceptionStatus === 1"
              @click="handleResolveException(row)"
            >
              标记已处理
            </el-button>
          </template>
        </el-table-column>
      </CommonTable>
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

    <el-card v-if="filters.clubId" class="mt16" header="考勤规则配置">
      <el-form :model="ruleForm" :rules="ruleRules" ref="ruleFormRef" inline label-width="140px">
        <el-form-item label="迟到阈值(分钟)" prop="lateThreshold">
          <el-input-number v-model="ruleForm.lateThreshold" :min="1" />
        </el-form-item>
        <div
          v-if="manualDialog.form.activityId && !pendingLoading && pendingMembers.length === 0"
          class="helper-text"
        >
          该活动下暂无未签到成员
        </div>
        <el-form-item label="早退阈值(分钟)" prop="leaveEarlyThreshold">
          <el-input-number v-model="ruleForm.leaveEarlyThreshold" :min="1" />
        </el-form-item>
        <el-form-item label="缺勤阈值(分钟)" prop="absenceThreshold">
          <el-input-number v-model="ruleForm.absenceThreshold" :min="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="ruleSaving" @click="handleRuleSave">保存规则</el-button>
          <el-button @click="loadRule">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="manualDialog.visible" title="补录考勤" width="480px">
      <el-form :model="manualDialog.form" :rules="manualRules" ref="manualFormRef" label-width="110px">
        <el-form-item label="活动" prop="activityId">
          <el-select
            v-model="manualDialog.form.activityId"
            placeholder="请选择近30天已结束的活动"
            filterable
            clearable
            :loading="activityLoading"
          >
            <el-option
              v-for="activity in activityOptions"
              :key="activity.id"
              :label="activity.activityName"
              :value="activity.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="成员" prop="memberId">
          <el-select
            v-model="manualDialog.form.memberId"
            filterable
            placeholder="请选择未签到成员"
            :loading="pendingLoading"
            :disabled="!manualDialog.form.activityId || pendingMembers.length === 0"
          >
            <el-option
              v-for="member in pendingMembers"
              :key="member.id"
              :label="member.username || String(member.id)"
              :value="member.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="考勤类型" prop="attendanceType">
          <el-select v-model="manualDialog.form.attendanceType">
            <el-option v-for="(label, key) in typeText" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="考勤时间" prop="attendanceTime">
          <el-date-picker
            v-model="manualDialog.form.attendanceTime"
            type="datetime"
            placeholder="选择时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="manualDialog.form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="manualDialog.loading" @click="handleManualSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import CommonTable from '@/components/common/CommonTable.vue';
import {
  listAttendance,
  createAttendance,
  getAttendanceStats,
  exportAttendance,
  markAttendanceException,
  resolveAttendanceException,
  getAttendanceRule,
  saveAttendanceRule,
  type Attendance,
  type AttendanceRule,
  type AttendanceStatsResponse,
  listMembers,
  type Member
} from '@/api/members';
import { getManagedClubs, listClubs, type Club } from '@/api/clubs';
import { useAuthStore } from '@/store/modules/auth';
import {
  listActivities,
  listRegistrations,
  listSignIns,
  type Activity
} from '@/api/activities';

const filters = reactive({
  clubId: null as number | null,
  memberKeyword: '',
  activityId: '',
  dateRange: [] as string[]
});

const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);
const exportLoading = ref(false);
const clubLoading = ref(false);
const ruleSaving = ref(false);
const memberLoading = ref(false);
const statsTab = ref('member');

const authStore = useAuthStore();

// 考勤权限社团列表 / 系统管理员可见的社团列表
const clubOptions = ref<Club[]>([]);
const records = ref<Attendance[]>([]);
const stats = ref<AttendanceStatsResponse | null>(null);
const ruleForm = reactive<AttendanceRule>({
  clubId: 0,
  lateThreshold: 10,
  leaveEarlyThreshold: 10,
  absenceThreshold: 30
});
const memberOptions = ref<Member[]>([]);
const activityOptions = ref<Activity[]>([]);
const pendingMembers = ref<Member[]>([]);
const activityLoading = ref(false);
const pendingLoading = ref(false);
const lastActivityFetchClubId = ref<number | null>(null);

const ruleFormRef = ref<FormInstance>();
const manualFormRef = ref<FormInstance>();

const ruleRules: FormRules = {
  lateThreshold: [{ required: true, message: '请输入迟到阈值', trigger: 'blur' }],
  leaveEarlyThreshold: [{ required: true, message: '请输入早退阈值', trigger: 'blur' }],
  absenceThreshold: [{ required: true, message: '请输入缺勤阈值', trigger: 'blur' }]
};

const manualDialog = reactive({
  visible: false,
  loading: false,
  form: {
    memberId: null as number | null,
    attendanceType: 1,
    attendanceTime: '',
    activityId: null as number | null,
    remark: ''
  }
});

const manualRules: FormRules = {
  memberId: [{ required: true, message: '请选择成员', trigger: 'change' }],
  activityId: [{ required: true, message: '请选择活动', trigger: 'change' }],
  attendanceType: [{ required: true, message: '请选择考勤类型', trigger: 'change' }],
  attendanceTime: [{ required: true, message: '请选择时间', trigger: 'change' }]
};

const typeText: Record<number, string> = {
  1: '正常',
  2: '迟到',
  3: '早退',
  4: '缺勤'
};

const typeTagType: Record<number, 'success' | 'warning' | 'danger' | 'info'> = {
  1: 'success',
  2: 'warning',
  3: 'warning',
  4: 'danger'
};

const summaryCards = computed(() => {
  if (!stats.value) return [];
  const s = stats.value.summary;
  return [
    { label: '总考勤数', value: s.totalCount },
    { label: '正常', value: s.normalCount },
    { label: '迟到/早退', value: s.lateCount + s.leaveEarlyCount },
    { label: '缺勤', value: s.absentCount },
    { label: '异常', value: s.exceptionCount }
  ];
});

// 加载当前用户拥有考勤权限的社团；系统管理员加载全部社团
async function loadClubs() {
  clubLoading.value = true;
  try {
    if (authStore.isSystemAdmin) {
      const res = await listClubs({ status: 1, page: 1, size: 100 });
      clubOptions.value = res.records || [];
    } else {
      const managedClubs = await getManagedClubs();
      clubOptions.value = managedClubs || [];
    }
    if (!clubOptions.value.some((club) => club.id === filters.clubId)) {
      filters.clubId = null;
    }
    if (!filters.clubId && clubOptions.value.length === 1) {
      filters.clubId = clubOptions.value[0].id;
      await handleClubChange();
    }
  } catch (error) {
    console.error('加载考勤社团失败:', error);
    ElMessage.error('加载社团列表失败');
  } finally {
    clubLoading.value = false;
  }
}

function buildDateRange() {
  if (!filters.dateRange || filters.dateRange.length !== 2) return { start: undefined, end: undefined };
  return {
    start: filters.dateRange[0],
    end: filters.dateRange[1]
  };
}

async function load() {
  if (!filters.clubId) {
    records.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const { start, end } = buildDateRange();
    const res = await listAttendance({
      clubId: filters.clubId,
      activityId: filters.activityId ? Number(filters.activityId) : undefined,
      startTime: start,
      endTime: end,
      page: page.value,
      size: size.value
    });
    let filtered = res.records || [];
    if (filters.memberKeyword.trim()) {
      const keyword = filters.memberKeyword.trim().toLowerCase();
      filtered = filtered.filter(rec => rec.memberUsername?.toLowerCase().includes(keyword));
    }
    records.value = filtered;
    total.value = res.total;
    await loadStats();
  } finally {
    loading.value = false;
  }
}

async function loadStats() {
  if (!filters.clubId) {
    stats.value = null;
    return;
  }
  const { start, end } = buildDateRange();
  stats.value = await getAttendanceStats({
    clubId: filters.clubId,
    startTime: start,
    endTime: end
  });
}

async function loadRule() {
  if (!filters.clubId) return;
  // 系统管理员只查看考勤统计，不参与规则配置，直接跳过
  if (authStore.isSystemAdmin) return;
  const rule = await getAttendanceRule(filters.clubId);
  ruleForm.clubId = filters.clubId;
  ruleForm.lateThreshold = rule.lateThreshold;
  ruleForm.leaveEarlyThreshold = rule.leaveEarlyThreshold;
  ruleForm.absenceThreshold = rule.absenceThreshold;
}

async function handleRuleSave() {
  if (!filters.clubId) return;
  await ruleFormRef.value?.validate();
  ruleSaving.value = true;
  try {
    await saveAttendanceRule({
      clubId: filters.clubId,
      lateThreshold: ruleForm.lateThreshold,
      leaveEarlyThreshold: ruleForm.leaveEarlyThreshold,
      absenceThreshold: ruleForm.absenceThreshold
    });
    ElMessage.success('规则已保存');
  } finally {
    ruleSaving.value = false;
  }
}

function resetAndLoad() {
  page.value = 1;
  load();
}

function handlePageChange(val: number) {
  page.value = val;
  load();
}

function handleSizeChange(val: number) {
  size.value = val;
  page.value = 1;
  load();
}

function handleReset() {
  filters.memberKeyword = '';
  filters.activityId = '';
  filters.dateRange = [];
  page.value = 1;
  size.value = 10;
  load();
}

async function handleExport() {
  if (!filters.clubId) return;
  exportLoading.value = true;
  try {
    const { start, end } = buildDateRange();
    const blob = await exportAttendance({
      clubId: filters.clubId,
      activityId: filters.activityId ? Number(filters.activityId) : undefined,
      startTime: start,
      endTime: end
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'attendance.csv';
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } finally {
    exportLoading.value = false;
  }
}

async function handleMarkException(row: Attendance) {
  if (!row.id) return;
  try {
    const { value } = await ElMessageBox.prompt('请输入异常原因', '标记异常', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '异常原因'
    });
    await markAttendanceException(row.id, value || '人工标记异常');
    ElMessage.success('已标记为异常');
    load();
  } catch {
    // ignore
  }
}

async function handleResolveException(row: Attendance) {
  if (!row.id) return;
  try {
    const { value } = await ElMessageBox.prompt('请输入处理说明', '处理异常', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '处理说明'
    });
    await resolveAttendanceException(row.id, value || '已处理');
    ElMessage.success('异常已处理');
    load();
  } catch {
    // ignore
  }
}

async function handleClubChange() {
  if (!filters.clubId) {
    records.value = [];
    stats.value = null;
    return;
  }
  ruleForm.clubId = filters.clubId;
  await Promise.all([loadRule(), loadMembersOptions()]);
  activityOptions.value = [];
  pendingMembers.value = [];
  lastActivityFetchClubId.value = null;
  resetAndLoad();
}

async function loadMembersOptions() {
  if (!filters.clubId) return;
  memberLoading.value = true;
  try {
    const res = await listMembers({ clubId: filters.clubId, page: 1, size: 100 });
    memberOptions.value = res.records || [];
  } finally {
    memberLoading.value = false;
  }
}

async function loadRecentActivities(force = false) {
  if (!filters.clubId) return;
  if (!force && lastActivityFetchClubId.value === filters.clubId && activityOptions.value.length) {
    return;
  }
  activityLoading.value = true;
  try {
    const res = await listActivities({
      clubId: filters.clubId,
      status: 2,
      page: 1,
      size: 100
    });
    const now = Date.now();
    const cutoff = now - 30 * 24 * 60 * 60 * 1000;
    activityOptions.value = (res.records || []).filter(activity => {
      const timeStr = activity.endTime || activity.startTime;
      if (!timeStr) return false;
      const time = new Date(timeStr).getTime();
      return !Number.isNaN(time) && time >= cutoff;
    });
    lastActivityFetchClubId.value = filters.clubId;
  } finally {
    activityLoading.value = false;
  }
}

async function loadPendingMembers(activityId: number) {
  if (!activityId) {
    pendingMembers.value = [];
    return;
  }
  pendingLoading.value = true;
  try {
    // 使用 size: 100 避免超过后端限制
    const [registrationRes, signRes] = await Promise.all([
      listRegistrations({ activityId, status: 1, page: 1, size: 100 }),
      listSignIns({ activityId, page: 1, size: 100 })
    ]);
    
    // 如果报名人数超过 100，提示用户
    if (registrationRes.total && registrationRes.total > 100) {
      console.warn(`活动报名人数超过100，仅显示前100条`);
    }
    
    const signedUserIds = new Set((signRes.records || []).map(record => record.userId));
    const memberMap = new Map(memberOptions.value.map(member => [member.userId, member]));
    pendingMembers.value = (registrationRes.records || [])
      .filter(reg => !signedUserIds.has(reg.userId))
      .map(reg => memberMap.get(reg.userId))
      .filter((member): member is Member => Boolean(member));
    if (pendingMembers.value.length === 0) {
      ElMessage.warning('该活动暂无未签到成员');
    }
  } catch (error) {
    console.error('加载未签到成员失败', error);
    ElMessage.error('加载未签到成员失败');
    pendingMembers.value = [];
  } finally {
    pendingLoading.value = false;
  }
}

async function openManualDialog() {
  if (!filters.clubId) return;
  manualDialog.form.memberId = null;
  manualDialog.form.attendanceType = 1;
  manualDialog.form.attendanceTime = '';
  manualDialog.form.activityId = null;
  manualDialog.form.remark = '';
  pendingMembers.value = [];
  // 确保成员列表已加载，因为 loadPendingMembers 依赖它
  if (memberOptions.value.length === 0) {
    await loadMembersOptions();
  }
  await loadRecentActivities(true);
  manualDialog.visible = true;
}

async function handleManualSubmit() {
  if (!filters.clubId) return;
  await manualFormRef.value?.validate();
  manualDialog.loading = true;
  try {
    await createAttendance({
      clubId: filters.clubId,
      memberId: manualDialog.form.memberId!,
      activityId: manualDialog.form.activityId ?? undefined,
      attendanceType: manualDialog.form.attendanceType,
      attendanceTime: manualDialog.form.attendanceTime,
      remark: manualDialog.form.remark
    });
    ElMessage.success('补录成功');
    manualDialog.visible = false;
    load();
  } finally {
    manualDialog.loading = false;
  }
}

onMounted(async () => {
  await loadClubs();
});

watch(
  () => manualDialog.form.activityId,
  value => {
    manualDialog.form.memberId = null;
    if (value) {
      loadPendingMembers(Number(value));
    } else {
      pendingMembers.value = [];
    }
  }
);
</script>

<style scoped>
.attendance-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.w200 {
  width: 200px;
}
.w180 {
  width: 180px;
}
.w160 {
  width: 160px;
}
.w320 {
  width: 320px;
}
.summary-card {
  display: flex;
  flex-direction: column;
  text-align: center;
  padding: 8px 0;
}
.summary-label {
  font-size: 14px;
  color: #909399;
}
.summary-value {
  font-size: 24px;
  font-weight: 600;
  margin-top: 4px;
}

.helper-text {
  margin: -8px 0 12px 0;
  color: #f59e0b;
  font-size: 13px;
}
</style>
