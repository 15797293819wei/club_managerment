<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">找回密码</h1>
      <p class="subtitle">填写账户信息以重置密码</p>
      <el-steps :active="step" finish-status="success" align-center class="mb16">
        <el-step title="验证身份" />
        <el-step title="设置新密码" />
      </el-steps>

      <el-form v-if="step === 0" ref="verifyFormRef" :model="verifyForm" :rules="verifyRules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="verifyForm.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="verifyForm.email" placeholder="手机号/邮箱" size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleVerify">
            发送重置请求
          </el-button>
        </el-form-item>
      </el-form>

      <el-form v-else ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="0">
        <el-form-item prop="token">
          <el-input v-model="resetForm.token" placeholder="重置令牌" size="large" />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input
            v-model="resetForm.newPassword"
            placeholder="新密码"
            type="password"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleReset">
            重置密码
          </el-button>
        </el-form-item>
      </el-form>

      <div class="links center">
        <el-link type="primary" @click="goLogin">返回登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { forgotPassword, resetPassword } from '@/api/auth';

const router = useRouter();
const step = ref(0);
const loading = ref(false);

const verifyFormRef = ref<FormInstance>();
const resetFormRef = ref<FormInstance>();

const verifyForm = reactive({
  username: '',
  email: ''
});

const resetForm = reactive({
  token: '',
  newPassword: ''
});

const verifyRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
};

const resetRules: FormRules = {
  token: [{ required: true, message: '请输入重置令牌', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
};

async function handleVerify() {
  await verifyFormRef.value?.validate();
  loading.value = true;
  try {
    const res = await forgotPassword({
      username: verifyForm.username.trim(),
      email: verifyForm.email || undefined
    });
    ElMessage.success(res.message || '重置请求已发送，请检查邮箱或联系管理员');
    if (res.token) {
      resetForm.token = res.token;
    }
    step.value = 1;
  } catch (e) {
    // 具体错误信息已由全局 http 拦截器提示
    console.warn('忘记密码请求失败', e);
  } finally {
    loading.value = false;
  }
}

async function handleReset() {
  await resetFormRef.value?.validate();
  loading.value = true;
  try {
    await resetPassword({
      token: resetForm.token.trim(),
      newPassword: resetForm.newPassword
    });
    ElMessage.success('密码已重置，请使用新密码登录');
    router.replace({ name: 'Login' });
  } finally {
    loading.value = false;
  }
}

function goLogin() {
  router.push({ name: 'Login' });
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: url('@/assets/login_bg.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.login-card {
  width: 420px;
  padding: 32px 28px;
  border-radius: 16px;
  background-color: #ffffff;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.35);
}

.title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  text-align: center;
}

.subtitle {
  margin: 0 0 24px;
  font-size: 13px;
  text-align: center;
  color: #6b7280;
}

.mb16 {
  margin-bottom: 16px;
}

.links.center {
  display: flex;
  justify-content: center;
}
</style>
