<template>
  <div class="join-club-apply-page">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
    </div>

    <el-card v-loading="pageLoading" header="申请加入社团">
      <el-alert
        v-if="selfState?.joined"
        type="success"
        show-icon
        :closable="false"
        title="你已是该社团成员，无需重复申请。"
        class="mb12"
      />
      <el-alert
        v-else-if="selfState?.pendingApplication"
        type="info"
        show-icon
        :closable="false"
        title="你已提交入社申请，请等待审核。"
        class="mb12"
      />

      <el-descriptions v-if="club" :column="2" border class="mb12">
        <el-descriptions-item label="社团名称">{{ club.clubName }}</el-descriptions-item>
        <el-descriptions-item label="社团代码">{{ club.clubCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">
          {{ club.description || '暂无简介' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="申请理由" prop="applicationReason">
          <el-input
            v-model="form.applicationReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="简要说明你想加入该社团的原因（可选）"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="saving"
            :disabled="!!selfState?.joined || !!selfState?.pendingApplication"
            @click="handleSubmit"
          >
            提交申请
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { getClub, type Club } from '@/api/clubs';
import { getMemberSelfState, submitMemberApplication, type MemberSelfState } from '@/api/members';

const route = useRoute();
const router = useRouter();
const clubId = Number(route.params.id);

const pageLoading = ref(false);
const saving = ref(false);

const club = ref<Club | null>(null);
const selfState = ref<MemberSelfState | null>(null);

const formRef = ref<FormInstance>();
const form = reactive({
  applicationReason: ''
});

// 后端允许不填理由，这里不强制必填
const rules: FormRules = {
  applicationReason: [{ min: 0, max: 200, message: '最多 200 字', trigger: 'blur' }]
};

function goBack() {
  router.back();
}

async function load() {
  if (!Number.isFinite(clubId)) {
    ElMessage.error('无效的社团 ID');
    return;
  }
  pageLoading.value = true;
  try {
    const [c, s] = await Promise.all([getClub(clubId), getMemberSelfState(clubId)]);
    club.value = c;
    selfState.value = s;
  } catch (e) {
    console.error(e);
    ElMessage.error('加载社团信息失败');
  } finally {
    pageLoading.value = false;
  }
}

async function handleSubmit() {
  if (!Number.isFinite(clubId)) return;
  if (selfState.value?.joined) {
    ElMessage.success('你已加入该社团');
    return;
  }
  if (selfState.value?.pendingApplication) {
    ElMessage.info('你已提交申请，请等待审核');
    return;
  }

  await formRef.value?.validate();
  saving.value = true;
  try {
    await submitMemberApplication({
      clubId,
      applicationReason: form.applicationReason?.trim() || undefined
    });
    ElMessage.success('申请已提交，请等待审核');
    await load(); // 刷新状态，按钮会变为不可点
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.join-club-apply-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  margin-bottom: 8px;
}

.mb12 {
  margin-bottom: 12px;
}
</style>

