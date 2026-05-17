<template>
  <div class="club-apply-page">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
    </div>
    <el-card header="创建社团申请">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="社团名称" prop="clubName">
          <el-input v-model="form.clubName" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="社团代码" prop="clubCode">
          <el-input v-model="form.clubCode" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="宗旨" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="章程" prop="constitution">
          <el-input v-model="form.constitution" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSubmit">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { submitClubApplication } from '@/api/clubs';

const router = useRouter();
const formRef = ref<FormInstance>();
const saving = ref(false);

function goBack() {
  router.back();
}

const form = reactive({
  clubName: '',
  clubCode: '',
  description: '',
  purpose: '',
  constitution: ''
});

const rules: FormRules = {
  clubName: [{ required: true, message: '请输入社团名称', trigger: 'blur' }]
};

async function handleSubmit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await submitClubApplication({
      clubName: form.clubName.trim(),
      clubCode: form.clubCode?.trim() || undefined,
      description: form.description?.trim() || undefined,
      purpose: form.purpose?.trim() || undefined,
      constitution: form.constitution?.trim() || undefined
    });
    ElMessage.success('申请已提交，请等待审核');
    router.replace({ name: 'MyClubs' });
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.club-apply-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  margin-bottom: 8px;
}
</style>

