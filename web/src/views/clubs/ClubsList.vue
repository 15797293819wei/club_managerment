<template>
  <div class="clubs-page">
    <el-card class="mb16" header="社团管理">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索社团名称或代码"
          clearable
          class="w220"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select v-model="filters.status" placeholder="状态" class="w160" clearable @change="reload">
          <el-option :value="1" label="正常" />
          <el-option :value="0" label="停用" />
          <el-option :value="3" label="已解散" />
        </el-select>
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button type="danger" v-permission="'SYSTEM_ADMIN'" @click="handleClearDissolved">清除已解散社团</el-button>
      </div>

      <el-table :data="records" :loading="loading" size="small">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="clubName" label="社团名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="clubCode" label="社团代码" width="140" />
        <el-table-column prop="founderUsername" label="创建人" width="140" />
        <el-table-column prop="createdTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdTime?.replace('T', ' ') }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" v-permission="'SYSTEM_ADMIN'">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 3"
              text
              type="danger"
              size="small"
              @click="handleDelete(row.id)"
            >
              删除
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
import { listClubs, deleteClub, type Club } from '@/api/clubs';

const filters = reactive({
  keyword: '',
  status: undefined as number | undefined
});

const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);
const records = ref<Club[]>([]);

async function load() {
  loading.value = true;
  try {
    const res = await listClubs({
      keyword: filters.keyword?.trim() || undefined,
      status: filters.status,
      page: page.value,
      size: size.value
    });
    // 默认不显示已解散的社团（status === 3），除非用户明确选择查看
    const filtered = filters.status === undefined
      ? (res.records || []).filter((club) => club.status !== 3)
      : res.records || [];
    records.value = filtered;
    total.value = filters.status === undefined
      ? filtered.length
      : res.total || 0;
  } catch (error) {
    console.error(error);
    ElMessage.error('加载社团列表失败');
  } finally {
    loading.value = false;
  }
}

function getStatusText(status: number | undefined): string {
  if (status === 1) return '正常';
  if (status === 3) return '已解散';
  if (status === 0) return '停用';
  return '未知';
}

function getStatusTagType(status: number | undefined): 'success' | 'info' | 'danger' {
  if (status === 1) return 'success';
  if (status === 3) return 'danger';
  return 'info';
}

async function handleClearDissolved() {
  try {
    await ElMessageBox.confirm(
      '确认清除所有已解散的社团信息吗？此操作将永久删除这些社团的所有数据，包括成员关系、活动记录等，且无法恢复！',
      '清除已解散社团',
      {
        type: 'warning',
        confirmButtonText: '确定清除',
        cancelButtonText: '取消',
        dangerouslyUseHTMLString: false
      }
    );

    // 先获取所有已解散的社团（后端 size 限制为 100，这里取上限）
    const res = await listClubs({ status: 3, page: 1, size: 100 });
    const dissolvedClubs = res.records || [];

    if (dissolvedClubs.length === 0) {
      ElMessage.info('没有已解散的社团');
      return;
    }

    // 批量删除
    let successCount = 0;
    let failCount = 0;
    for (const club of dissolvedClubs) {
      try {
        await deleteClub(club.id);
        successCount++;
      } catch {
        failCount++;
      }
    }

    if (successCount > 0) {
      ElMessage.success(`已清除 ${successCount} 个已解散的社团${failCount > 0 ? `，${failCount} 个清除失败` : ''}`);
      await load();
    } else {
      ElMessage.error('清除失败，请稍后重试');
    }
  } catch {
    // 用户取消
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该已解散的社团吗？此操作将永久删除该社团的所有数据，且无法恢复！', '删除社团', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    });
    await deleteClub(id);
    ElMessage.success('已删除');
    if (records.value.length === 1 && page.value > 1) {
      page.value -= 1;
    }
    await load();
  } catch {
    // 用户取消或删除失败
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

onMounted(() => {
  load();
});
</script>

<style scoped>
.clubs-page {
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

.mb16 {
  margin-bottom: 16px;
}
</style>

