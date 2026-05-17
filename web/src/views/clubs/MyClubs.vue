<template>
  <div class="my-clubs">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的社团</span>
          <el-button type="primary" :icon="Plus" @click="goCreateClub">创建社团</el-button>
        </div>
      </template>
      <el-empty v-if="!loading && !records.length" description="暂未加入任何社团" />
      <el-skeleton :loading="loading" animated>
        <template #default>
          <el-row :gutter="16">
            <el-col v-for="club in records" :key="club.id" :xs="24" :sm="12" :md="8">
              <el-card shadow="hover" class="club-card">
                <h3 class="club-title">{{ club.clubName }}</h3>
                <p class="club-role">我的身份：{{ roleText[club.role] || club.role }}</p>
                <p class="club-desc">{{ club.description || '暂无简介' }}</p>
                <div class="club-actions">
                  <el-button
                    v-if="club.role === 'PRESIDENT'"
                    type="danger"
                    text
                    @click="handleDissolve(club)"
                  >
                    申请解散社团
                  </el-button>
                  <el-button
                    v-else
                    type="danger"
                    text
                    @click="handleQuit(club)"
                  >
                    退出社团
                  </el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </template>
      </el-skeleton>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { getJoinedClubs, dissolveClub, type JoinedClub } from '@/api/clubs';
import { getMemberSelfState, quitClub } from '@/api/members';
import { useAuthStore } from '@/store/modules/auth';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const records = ref<JoinedClub[]>([]);

const roleText: Record<string, string> = {
  MEMBER: '成员',
  STAFF: '干事',
  VICE_MINISTER: '副部长',
  MINISTER: '部长',
  PRESIDENT: '社长'
};

async function load() {
  loading.value = true;
  try {
    records.value = await getJoinedClubs();
  } catch (error) {
    console.error(error);
    ElMessage.error('加载我的社团失败');
  } finally {
    loading.value = false;
  }
}

function goCreateClub() {
  router.push({ name: 'ClubCreateApply' });
}

async function handleDissolve(club: JoinedClub) {
  try {
    const { value } = await ElMessageBox.prompt(
      `解散「${club.clubName}」将导致所有成员退出社团，并需要系统管理员审核后才会生效。\n\n请输入解散原因：`,
      '申请解散社团',
      {
        inputType: 'textarea',
        inputPlaceholder: '请填写解散原因（必填）',
        inputValidator: (val: string) => !!val && val.trim().length > 0,
        inputErrorMessage: '解散原因不能为空',
        confirmButtonText: '提交申请',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    await dissolveClub(club.id, { reason: value.trim() });
    ElMessage.success('已提交解散申请，请等待系统管理员审核');
    await Promise.all([load(), authStore.refreshClubRoles()]);
  } catch {
    // 用户取消或出错时不额外提示
  }
}

async function handleQuit(club: JoinedClub) {
  try {
    const confirm = await ElMessageBox.confirm(
      `确认退出「${club.clubName}」吗？退出后将失去该社团的所有成员权限。`,
      '退出社团',
      {
        type: 'warning',
        confirmButtonText: '退出',
        cancelButtonText: '取消'
      }
    );
    if (!confirm) return;

    // 先获取自己在该社团的 memberId，然后调用退出接口
    const selfState = await getMemberSelfState(club.id);
    if (!selfState.memberId) {
      ElMessage.error('未找到你的成员信息，无法退出');
      return;
    }
    await quitClub(selfState.memberId);
    ElMessage.success('已退出社团');
    await Promise.all([load(), authStore.refreshClubRoles()]);
  } catch {
    // 用户取消或接口报错时，不额外提示
  }
}

onMounted(load);
</script>

<style scoped>
.my-clubs {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.club-card {
  min-height: 140px;
}

.club-title {
  margin: 0 0 4px;
  font-size: 18px;
}

.club-role {
  margin: 0 0 8px;
  color: #4b5563;
}

.club-desc {
  margin: 0;
  color: #6b7280;
}

.club-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
</style>

