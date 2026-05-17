<template>
  <div class="club-explore">
    <el-card class="mb16" header="所有社团">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索社团名称或代码"
          clearable
          class="keyword"
          @keyup.enter="resetAndLoad"
          @clear="resetAndLoad"
        />
        <el-input-number
          v-model="minMembers"
          :min="0"
          :max="10000"
          placeholder="最少成员"
          class="number-input"
          @change="resetAndLoad"
        />
        <el-input-number
          v-model="maxMembers"
          :min="0"
          :max="10000"
          placeholder="最多成员"
          class="number-input"
          @change="resetAndLoad"
        />
        <el-select v-model="sortBy" class="sort-select" @change="resetAndLoad">
          <el-option value="memberCount" label="按成员数" />
          <el-option value="createdTime" label="按创建时间" />
        </el-select>
        <el-select v-model="sortOrder" class="sort-select" @change="resetAndLoad">
          <el-option value="desc" label="倒序" />
          <el-option value="asc" label="正序" />
        </el-select>
        <el-button type="primary" @click="resetAndLoad">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
      <el-empty v-if="!loading && !records.length" description="暂无可加入的社团" />
      <el-skeleton :loading="loading" animated>
        <template #template>
          <div class="card-skeleton" v-for="n in pageSize" :key="n">
            <el-skeleton-item variant="rect" style="height: 140px" />
          </div>
        </template>
        <template #default>
          <el-row :gutter="16">
            <el-col v-for="club in records" :key="club.id" :xs="24" :sm="12" :md="8">
              <el-card shadow="hover" class="club-card">
                <div class="club-card__header">
                  <h3 class="club-card__title">{{ club.clubName }}</h3>
                  <el-tag type="success" size="small">可加入</el-tag>
                </div>
                <p class="club-card__description">{{ club.description || '暂未填写简介' }}</p>
                <div class="club-card__meta">
                  <span>编号：{{ club.clubCode || '-' }}</span>
                  <span>管理员：{{ club.founderUsername || '-' }}</span>
                </div>
                <div class="club-card__actions">
                  <el-button text type="primary" @click="goDetail(club.id)">查看详情</el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </template>
      </el-skeleton>
      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="成员数排行榜" v-loading="leaderboardLoading">
          <template v-if="leaderboard.member.length">
            <ul class="leaderboard">
              <li v-for="(item, index) in leaderboard.member" :key="item.clubId">
                <span class="leaderboard__index">{{ index + 1 }}</span>
                <div class="leaderboard__info">
                  <div class="leaderboard__title">{{ item.clubName }}</div>
                  <div class="leaderboard__meta">{{ item.metricLabel }}：{{ item.metricValue }}</div>
                </div>
              </li>
            </ul>
          </template>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="活动数排行榜" v-loading="leaderboardLoading">
          <template v-if="leaderboard.activity.length">
            <ul class="leaderboard">
              <li v-for="(item, index) in leaderboard.activity" :key="item.clubId">
                <span class="leaderboard__index">{{ index + 1 }}</span>
                <div class="leaderboard__info">
                  <div class="leaderboard__title">{{ item.clubName }}</div>
                  <div class="leaderboard__meta">{{ item.metricLabel }}：{{ item.metricValue }}</div>
                </div>
              </li>
            </ul>
          </template>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { listPublicClubs, fetchClubLeaderboard, type Club, type ClubLeaderboardItem } from '@/api/clubs';

const router = useRouter();

const keyword = ref('');
const minMembers = ref<number | null>(null);
const maxMembers = ref<number | null>(null);
const sortBy = ref<'memberCount' | 'createdTime'>('memberCount');
const sortOrder = ref<'asc' | 'desc'>('desc');
const records = ref<Club[]>([]);
const loading = ref(false);
const page = ref(1);
const total = ref(0);
const pageSize = 9;
const leaderboardLoading = ref(false);
const leaderboard = reactive<{ member: ClubLeaderboardItem[]; activity: ClubLeaderboardItem[] }>({
  member: [],
  activity: []
});

async function load() {
  loading.value = true;
  try {
    const res = await listPublicClubs({
      keyword: keyword.value,
      minMembers: minMembers.value ?? undefined,
      maxMembers: maxMembers.value ?? undefined,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value,
      page: page.value,
      size: pageSize
    });
    records.value = res.records || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
}

function resetAndLoad() {
  page.value = 1;
  load();
}

function resetFilters() {
  keyword.value = '';
  minMembers.value = null;
  maxMembers.value = null;
  sortBy.value = 'memberCount';
  sortOrder.value = 'desc';
  resetAndLoad();
}

function handlePageChange(val: number) {
  page.value = val;
  load();
}

function goDetail(id: number) {
  router.push({ name: 'ClubDetail', params: { id } });
}

async function loadLeaderboard() {
  leaderboardLoading.value = true;
  try {
    const [member, activity] = await Promise.all([
      fetchClubLeaderboard({ type: 'member_count', limit: 5 }),
      fetchClubLeaderboard({ type: 'activity_count', limit: 5 })
    ]);
    leaderboard.member = member;
    leaderboard.activity = activity;
  } finally {
    leaderboardLoading.value = false;
  }
}

onMounted(async () => {
  await Promise.all([load(), loadLeaderboard()]);
});
</script>

<style scoped>
.club-explore {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.keyword {
  flex: 1;
  min-width: 220px;
}

.number-input,
.sort-select {
  width: 140px;
}

.club-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.club-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.club-card__title {
  font-size: 18px;
  margin: 0;
}

.club-card__description {
  flex: 1;
  color: #636c7b;
  margin: 0;
  min-height: 48px;
}

.club-card__meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 13px;
}

.club-card__actions {
  display: flex;
  justify-content: flex-end;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.card-skeleton {
  margin-bottom: 16px;
}

.leaderboard {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.leaderboard li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.leaderboard__index {
  font-weight: 600;
  font-size: 18px;
  color: #409eff;
  width: 24px;
  text-align: center;
}

.leaderboard__info {
  flex: 1;
}

.leaderboard__title {
  font-weight: 500;
  margin-bottom: 4px;
}

.leaderboard__meta {
  font-size: 12px;
  color: #909399;
}
</style>


