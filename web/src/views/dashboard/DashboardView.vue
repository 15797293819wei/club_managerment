<template>
  <div class="dashboard">
    <el-row :gutter="16" class="mb16">
      <el-col :span="8">
        <el-card header="今日待办">
          <div class="metric">
            <div class="metric-value">{{ stats?.pendingTasks ?? '-' }}</div>
            <div class="metric-label">待审核申请 / 代办事项</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="活动安排">
          <div class="metric">
            <div class="metric-value">{{ stats?.upcomingActivities ?? '-' }}</div>
            <div class="metric-label">待开始 / 进行中的活动</div>
          </div>
        </el-card>
      </el-col>
      <!-- 成员动态根据角色 / 是否有社团来决定是否展示 -->
      <el-col v-if="showMemberDynamics" :span="8">
        <el-card header="成员动态">
          <template v-if="isMemberOnly && memberClubOptions.length > 1" #header>
            <div class="card-header">
              <span>成员动态</span>
              <el-select
                v-model="memberSelectedClubId"
                filterable
                clearable
                placeholder="选择社团"
                class="club-select small"
                @change="load()"
              >
                <el-option
                  v-for="club in memberClubOptions"
                  :key="club.id"
                  :label="club.clubName"
                  :value="club.id"
                />
              </el-select>
            </div>
          </template>
          <div v-if="isMemberOnly && memberClubOptions.length === 1" class="member-dynamics-club">
            社团：{{ memberClubOptions[0]?.clubName }}
          </div>
          <div class="metric">
            <div class="metric-value">{{ stats?.recentMembers ?? '-' }}</div>
            <div class="metric-label">最近加入成员</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="showCharts" :gutter="16">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>社团活跃度趋势</span>
              <template v-if="isSystemAdmin">
                <el-select
                  v-model="selectedClubId"
                  filterable
                  remote
                  reserve-keyword
                  clearable
                  placeholder="全部社团 / 搜索社团"
                  class="club-select"
                  :remote-method="searchClubs"
                  :loading="clubLoading"
                  @change="reloadCharts"
                >
                  <el-option :key="0" label="全部社团" :value="0" />
                  <el-option
                    v-for="club in clubOptions"
                    :key="club.id"
                    :label="club.clubName"
                    :value="club.id"
                  />
                </el-select>
              </template>
              <template v-else-if="isClubStaffOrAbove">
                <el-select
                  v-model="selectedClubId"
                  filterable
                  clearable
                  placeholder="选择社团 / 搜索"
                  class="club-select"
                  @change="reloadCharts"
                >
                  <el-option
                    v-for="club in clubOptions"
                    :key="club.id"
                    :label="club.clubName"
                    :value="club.id"
                  />
                </el-select>
              </template>
            </div>
          </template>
          <div ref="trendChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="成员参与度排行榜" class="chart-card">
          <div ref="rankingChartRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed } from 'vue';
import * as echarts from 'echarts/core';
import { LineChart, BarChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import {
  getDashboardStats,
  getClubActivityTrend,
  getMemberParticipationRanking,
  type DashboardStats,
  type ClubActivityTrendPoint,
  type MemberParticipation
} from '@/api/statistics';
import { useAuthStore } from '@/store/modules/auth';
import { listClubs, getManagedClubs, getJoinedClubs, type Club } from '@/api/clubs';

echarts.use([LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

const stats = ref<DashboardStats | null>(null);
const trendChartRef = ref<HTMLDivElement>();
const rankingChartRef = ref<HTMLDivElement>();

let trendChart: echarts.ECharts | null = null;
let rankingChart: echarts.ECharts | null = null;

const authStore = useAuthStore();
const isSystemAdmin = computed(() => authStore.isSystemAdmin);
const hasClubRole = computed(() => authStore.hasClubRole);
const highestClubRole = computed(() => authStore.highestClubRole || '');
const isClubPresident = computed(() => highestClubRole.value === 'PRESIDENT');
const isClubStaffOrAbove = computed(() =>
  ['STAFF', 'VICE_MINISTER', 'MINISTER', 'PRESIDENT'].includes(highestClubRole.value)
);
// 是否需要显示“成员动态”：系统管理员或已经加入至少一个社团的用户
const showMemberDynamics = computed(() => isSystemAdmin.value || hasClubRole.value);

// 是否展示活跃度趋势 & 排行榜图表（成员仅看成员动态，不展示图表）
const showCharts = computed(() => isSystemAdmin.value || isClubStaffOrAbove.value);

// 仅普通成员（非干事及以上）：用于成员动态的社团选择
const isMemberOnly = computed(
  () => hasClubRole.value && !isSystemAdmin.value && !isClubStaffOrAbove.value
);
const memberClubOptions = ref<Club[]>([]);
const memberSelectedClubId = ref<number | null>(null);

// 系统管理员 / 干事及以上查看活跃度趋势和参与度排行榜时的社团筛选
// 0 表示"全部社团"，其他数字表示具体的社团ID
const selectedClubId = ref<number>(0);
const clubOptions = ref<Club[]>([]);
const clubLoading = ref(false);

// 社长默认查看的社团（当前管理社团中的第一个）
const presidentClubId = ref<number | null>(null);

async function load() {
  try {
    if (isSystemAdmin.value) {
      const baseParams = { clubId: selectedClubId.value === 0 ? undefined : selectedClubId.value };
      const [dashboard, trend, ranking] = await Promise.all([
        getDashboardStats(baseParams),
        getClubActivityTrend({ ...baseParams, granularity: 'MONTH' }),
        getMemberParticipationRanking({ ...baseParams, limit: 10 })
      ]);
      stats.value = dashboard;
      updateTrendChart(trend);
      updateRankingChart(ranking);
    } else if (isClubPresident.value) {
      // 社长：查看自己管理社团的活跃度趋势和参与度排行榜
      if (!presidentClubId.value) {
        const managed = await getManagedClubs();
        presidentClubId.value = managed[0]?.id ?? null;
      }
      if (presidentClubId.value) {
        const baseParams = { clubId: presidentClubId.value };
        const [dashboard, trend, ranking] = await Promise.all([
          getDashboardStats(baseParams),
          getClubActivityTrend({ ...baseParams, granularity: 'MONTH' }),
          getMemberParticipationRanking({ ...baseParams, limit: 10 })
        ]);
        stats.value = dashboard;
        updateTrendChart(trend);
        updateRankingChart(ranking);
      } else {
        // 极端情况：标记为社长但无管理社团时，仅展示基础统计
        stats.value = await getDashboardStats();
      }
    } else if (isClubStaffOrAbove.value) {
      // 部长 / 副部长 / 干事：查看自己加入社团的活跃度趋势和参与度排行榜，可切换社团（仅限自己加入的）
      if (!clubOptions.value.length) {
        const joined = await getJoinedClubs();
        clubOptions.value = joined.map((c) => ({
          id: c.id,
          clubName: c.clubName,
          clubCode: c.clubCode,
          description: c.description,
          status: c.status,
          founderId: c.founderId,
          founderUsername: c.founderUsername,
          logo: c.logo,
          createdTime: c.createdTime,
          updatedTime: c.updatedTime
        })) as Club[];
        if (selectedClubId.value === 0 && clubOptions.value.length) {
          selectedClubId.value = clubOptions.value[0].id;
        }
      }
      if (selectedClubId.value && selectedClubId.value !== 0) {
        const baseParams = { clubId: selectedClubId.value };
        const [dashboard, trend, ranking] = await Promise.all([
          getDashboardStats(baseParams),
          getClubActivityTrend({ ...baseParams, granularity: 'MONTH' }),
          getMemberParticipationRanking({ ...baseParams, limit: 10 })
        ]);
        stats.value = dashboard;
        updateTrendChart(trend);
        updateRankingChart(ranking);
      } else {
        stats.value = await getDashboardStats();
      }
    } else {
      // 成员：仅成员动态，可根据所选社团查看该社团成员动态
      if (hasClubRole.value && !memberClubOptions.value.length) {
        const joined = await getJoinedClubs();
        memberClubOptions.value = joined.map((c) => ({
          id: c.id,
          clubName: c.clubName,
          clubCode: c.clubCode,
          description: c.description,
          status: c.status,
          founderId: c.founderId,
          founderUsername: c.founderUsername,
          logo: c.logo,
          createdTime: c.createdTime,
          updatedTime: c.updatedTime
        })) as Club[];
        if (memberClubOptions.value.length === 1) {
          memberSelectedClubId.value = memberClubOptions.value[0].id;
        }
      }
      const params =
        memberSelectedClubId.value != null ? { clubId: memberSelectedClubId.value } : undefined;
      stats.value = await getDashboardStats(params);
    }
  } catch (err) {
    console.error('加载仪表盘统计失败', err);
  }
}

async function searchClubs(keyword: string) {
  if (!keyword) {
    clubOptions.value = [];
    return;
  }
  clubLoading.value = true;
  try {
    const res = await listClubs({ keyword: keyword.trim(), page: 1, size: 20 });
    clubOptions.value = res.records || [];
  } finally {
    clubLoading.value = false;
  }
}

function reloadCharts() {
  if (showCharts.value) {
    load();
  }
}

function updateTrendChart(data: ClubActivityTrendPoint[]) {
  if (!trendChart) return;
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['活动次数', '报名数', '签到数'] },
    grid: { left: 40, right: 20, bottom: 40, top: 40 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((item) => item.period)
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '活动次数',
        type: 'line',
        smooth: true,
        data: data.map((item) => item.activityCount)
      },
      {
        name: '报名数',
        type: 'line',
        smooth: true,
        data: data.map((item) => item.registrationCount)
      },
      {
        name: '签到数',
        type: 'line',
        smooth: true,
        data: data.map((item) => item.signInCount)
      }
    ]
  });
}

function updateRankingChart(data: MemberParticipation[]) {
  if (!rankingChart) return;
  const categories = data.map((item) => item.realName || item.username || `用户 ${item.userId}`);
  rankingChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['报名次数', '签到次数'] },
    grid: { left: 100, right: 20, bottom: 40, top: 40 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: categories, inverse: true },
    series: [
      {
        name: '报名次数',
        type: 'bar',
        stack: 'total',
        data: data.map((item) => item.registrationCount)
      },
      {
        name: '签到次数',
        type: 'bar',
        stack: 'total',
        data: data.map((item) => item.signInCount)
      }
    ]
  });
}

function initCharts() {
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value);
  }
  if (rankingChartRef.value) {
    rankingChart = echarts.init(rankingChartRef.value);
  }
  load();
  window.addEventListener('resize', handleResize);
}

function disposeCharts() {
  window.removeEventListener('resize', handleResize);
  trendChart?.dispose();
  rankingChart?.dispose();
  trendChart = null;
  rankingChart = null;
}

function handleResize() {
  trendChart?.resize();
  rankingChart?.resize();
}

onMounted(() => {
  initCharts();
});

onBeforeUnmount(() => {
  disposeCharts();
});
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb16 {
  margin-bottom: 16px;
}

.metric {
  text-align: center;
  padding: 12px 0;
}

.metric-value {
  font-size: 32px;
  font-weight: 600;
}

.metric-label {
  margin-top: 4px;
  color: #6b7280;
}

.chart-card {
  height: 360px;
}

.chart {
  width: 100%;
  height: 300px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.club-select {
  width: 260px;
  max-width: 60%;
}

.club-select.small {
  width: 180px;
}

.member-dynamics-club {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}
</style>
