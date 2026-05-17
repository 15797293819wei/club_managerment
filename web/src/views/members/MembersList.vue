<template>
  <div class="members-page">
    <el-card header="成员管理">
      <div class="toolbar">
        <el-select
          v-model="filters.clubId"
          placeholder="请选择社团"
          class="w220"
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
          v-model="filters.keyword"
          placeholder="搜索用户名"
          clearable
          class="w220"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-button type="primary" @click="reload">查询</el-button>
      </div>

      <el-table :data="records" :loading="loading" size="small">
        <el-table-column type="index" width="60" label="#" />
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="clubName" label="社团" min-width="180" show-overflow-tooltip />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            {{ roleText[row.role] || row.role }}
          </template>
        </el-table-column>
        <el-table-column prop="joinTime" label="加入时间" width="180">
          <template #default="{ row }">
            {{ row.joinTime?.replace('T', ' ') }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '正常' : '已退出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-space>
              <el-dropdown
                v-if="canSetRole(row)"
                trigger="click"
                @command="(cmd: string) => handleChangeRole(row, cmd)"
              >
                <el-button type="primary" text size="small">设置职务</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="PRESIDENT">社长</el-dropdown-item>
                    <el-dropdown-item command="MINISTER">部长</el-dropdown-item>
                    <el-dropdown-item command="VICE_MINISTER">副部长</el-dropdown-item>
                    <el-dropdown-item command="STAFF">干事</el-dropdown-item>
                    <el-dropdown-item command="MEMBER">成员</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button
                v-if="canRemove(row)"
                type="danger"
                text
                size="small"
                @click="handleRemove(row)"
              >
                移出社团
              </el-button>
            </el-space>
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
import { reactive, ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listMembers, assignMemberRole, deleteMember, type Member } from '@/api/members';
import { getManagedClubs, getJoinedClubs, type Club } from '@/api/clubs';
import { useAuthStore } from '@/store/modules/auth';

const filters = reactive({
  clubId: null as number | null,
  keyword: ''
});

const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);
const records = ref<Member[]>([]);
const clubLoading = ref(false);
const clubOptions = ref<Club[]>([]);

const authStore = useAuthStore();
const currentUserId = computed(() => authStore.user?.id);
const highestClubRole = computed(() => authStore.highestClubRole || '');

const CLUB_ROLE_PRIORITY: Record<string, number> = {
  MEMBER: 1,
  STAFF: 2,
  VICE_MINISTER: 3,
  MINISTER: 4,
  PRESIDENT: 5
};

const currentLevel = computed(
  () => CLUB_ROLE_PRIORITY[highestClubRole.value.toUpperCase()] || 0
);

const roleText: Record<string, string> = {
  MEMBER: '成员',
  STAFF: '干事',
  VICE_MINISTER: '副部长',
  MINISTER: '部长',
  PRESIDENT: '社长'
};

function getTargetLevel(role: string | undefined) {
  if (!role) return 0;
  return CLUB_ROLE_PRIORITY[role.toUpperCase()] || 0;
}

function canSetRole(row: Member) {
  // 只有社长可以设置职务，且不能改自己
  if (highestClubRole.value !== 'PRESIDENT') return false;
  if (row.userId === currentUserId.value) return false;
  return true;
}

function canRemove(row: Member) {
  if (row.status !== 1) return false;
  if (row.userId === currentUserId.value) return false;

  const level = currentLevel.value;
  const targetLevel = getTargetLevel(row.role);

  // 社长：可以移除所有其他人
  if (level >= CLUB_ROLE_PRIORITY.PRESIDENT) {
    return targetLevel < level;
  }

  // 部长 / 副部长：可以移除干事和成员
  if (level >= CLUB_ROLE_PRIORITY.VICE_MINISTER) {
    return targetLevel <= CLUB_ROLE_PRIORITY.STAFF;
  }

  // 干事：可以移除成员
  if (level >= CLUB_ROLE_PRIORITY.STAFF) {
    return targetLevel <= CLUB_ROLE_PRIORITY.MEMBER;
  }

  // 成员：无移除权限
  return false;
}

async function load() {
  if (!filters.clubId) {
    records.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await listMembers({
      clubId: filters.clubId,
      page: page.value,
      size: size.value
    });
    records.value = res.records || [];
    total.value = res.total || 0;
  } catch (error) {
    console.error(error);
    ElMessage.error('加载成员列表失败');
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

async function loadClubs() {
  clubLoading.value = true;
  try {
    // 管理者：getManagedClubs；普通成员：getJoinedClubs；去重后统一作为可选社团
    const [managed, joined] = await Promise.all([getManagedClubs(), getJoinedClubs()]);
    const map = new Map<number, Club>();
    (managed || []).forEach((c) => map.set(c.id, c));
    (joined || []).forEach((c) => {
      if (!map.has(c.id)) {
        map.set(c.id, {
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
        } as Club);
      }
    });
    clubOptions.value = Array.from(map.values());

    if (!filters.clubId && clubOptions.value.length === 1) {
      filters.clubId = clubOptions.value[0].id;
      await load();
    }
  } catch (error) {
    console.error('加载社团失败', error);
    ElMessage.error('加载社团列表失败');
  } finally {
    clubLoading.value = false;
  }
}

async function handleClubChange() {
  page.value = 1;
  await load();
}

async function handleChangeRole(row: Member, role: string) {
  try {
    await assignMemberRole(row.id, role);
    ElMessage.success('职务已更新');
    await load();
    await authStore.refreshClubRoles();
  } catch (error) {
    console.error(error);
    ElMessage.error('更新职务失败');
  }
}

async function handleRemove(row: Member) {
  try {
    await ElMessageBox.confirm(
      `确认将「${row.username || row.userId}」移出当前社团吗？`,
      '移出社团',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    );
    await deleteMember(row.id);
    ElMessage.success('已移出社团');
    await load();
    await authStore.refreshClubRoles();
  } catch {
    // 用户取消或接口失败时不再额外提示
  }
}

onMounted(async () => {
  await loadClubs();
  await load();
});
</script>

<style scoped>
.members-page {
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

