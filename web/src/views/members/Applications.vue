<template>
  <div class="member-applications">
    <el-card header="入社申请审核">
      <div class="toolbar">
        <el-select
          v-model="filters.clubId"
          placeholder="选择社团"
          class="w220"
          clearable
          :loading="clubsLoading"
          @change="reload"
        >
          <el-option v-for="c in managedClubs" :key="c.id" :label="c.clubName" :value="c.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" class="w160" clearable @change="reload">
          <el-option :value="0" label="待审核" />
          <el-option :value="1" label="已通过" />
          <el-option :value="2" label="已驳回" />
        </el-select>
        <el-button type="primary" @click="reload">查询</el-button>
      </div>

      <el-table :data="records" :loading="loading" size="small">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="clubName" label="社团" min-width="180" show-overflow-tooltip />
        <el-table-column prop="applicantUsername" label="申请人" width="140" />
        <el-table-column prop="applicationReason" label="申请理由" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createdTime" label="申请时间" width="180">
          <template #default="{ row }">
            {{ row.createdTime?.replace('T', ' ') }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status] || 'info'">
              {{ statusText[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button
              text
              type="success"
              size="small"
              :disabled="row.status !== 0"
              @click="handleReview(row, 1)"
            >
              通过
            </el-button>
            <el-button
              text
              type="danger"
              size="small"
              :disabled="row.status !== 0"
              @click="handleReview(row, 2)"
            >
              驳回
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
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getManagedClubs, type Club } from '@/api/clubs';
import {
  listMemberApplications,
  reviewMemberApplication,
  type MemberApplication
} from '@/api/members';

const filters = reactive({
  clubId: undefined as number | undefined,
  status: 0 as number | undefined
});

const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);
const records = ref<MemberApplication[]>([]);
const clubsLoading = ref(false);
const managedClubs = ref<Club[]>([]);

const statusText: Record<number, string> = {
  0: '待审核',
  1: '已通过',
  2: '已驳回'
};

const statusTagType: Record<number, 'info' | 'success' | 'danger'> = {
  0: 'info',
  1: 'success',
  2: 'danger'
};

async function load() {
  loading.value = true;
  try {
    const res = await listMemberApplications({
      clubId: filters.clubId,
      status: filters.status,
      page: page.value,
      size: size.value
    });
    records.value = res.records || [];
    total.value = res.total || 0;
  } catch (error) {
    console.error(error);
    ElMessage.error('加载入社申请失败');
  } finally {
    loading.value = false;
  }
}

async function loadManagedClubs() {
  clubsLoading.value = true;
  try {
    managedClubs.value = await getManagedClubs();
    // 默认选中第一个管理的社团，确保“入社审批”确实是审批学生加入“你的社团”
    if (!filters.clubId && managedClubs.value.length) {
      filters.clubId = managedClubs.value[0].id;
    }
  } catch (e) {
    console.error(e);
    ElMessage.error('加载可管理社团失败');
  } finally {
    clubsLoading.value = false;
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

async function handleReview(row: MemberApplication, status: 1 | 2) {
  const action = status === 1 ? '通过' : '驳回';
  try {
    const { value } = await ElMessageBox.prompt(`请输入${action}意见（可选）`, action, {
      inputPlaceholder: '审核意见',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    });
    await reviewMemberApplication(row.id, status, value || undefined);
    ElMessage.success(`${action}成功`);
    load();
  } catch {
    // 用户取消
  }
}

onMounted(async () => {
  await loadManagedClubs();
  await load();
});
</script>

<style scoped>
.member-applications {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.w160 {
  width: 160px;
}

.w220 {
  width: 220px;
}
</style>

