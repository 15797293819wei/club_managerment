<template>
  <div class="club-detail-page">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
    </div>
    <el-card v-if="club" class="mb16">
      <template #header>
        <div class="card-header">
          <span>{{ club.clubName }}</span>
          <el-button type="primary" @click="handleApply">申请加入</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="社团代码">{{ club.clubCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="club.status === 1 ? 'success' : 'info'">
            {{ club.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建人">{{ club.founderUsername || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ club.createdTime ? club.createdTime.replace('T', ' ') : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">
          {{ club.description || '暂无简介' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-empty v-else description="未找到社团信息" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { getClub, type Club } from '@/api/clubs';

const route = useRoute();
const router = useRouter();
const clubId = Number(route.params.id);

function goBack() {
  router.back();
}

function handleApply() {
  router.push({ name: 'ClubJoinApply', params: { id: clubId } });
}

const club = ref<Club | null>(null);

async function load() {
  if (!Number.isFinite(clubId)) {
    ElMessage.error('无效的社团 ID');
    return;
  }
  try {
    club.value = await getClub(clubId);
  } catch (error) {
    console.error(error);
    ElMessage.error('加载社团详情失败');
  }
}

onMounted(load);
</script>

<style scoped>
.club-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  margin-bottom: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 18px;
  font-weight: 600;
}

.mb16 {
  margin-bottom: 16px;
}
</style>

