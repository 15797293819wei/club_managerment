<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">社团管理系统</h1>
      <p class="subtitle">请使用账户登录</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <div class="links">
          <el-link type="primary" @click="goRegister">注册账号</el-link>
          <el-link type="info" @click="goForgot">忘记密码？</el-link>
        </div>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width: 100%"
            :loading="auth.loading"
            @click="handleSubmit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useAuthStore } from '@/store/modules/auth';

const router = useRouter();
const auth = useAuthStore();

const formRef = ref<FormInstance>();
const form = reactive({
  username: '',
  password: ''
});

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

async function handleSubmit() {
  await formRef.value?.validate();
  try {
    await auth.loginWithPassword(form.username.trim(), form.password);
    ElMessage.success('登录成功');
    router.replace({ name: 'Dashboard' });
  } catch {
    // 错误提示由全局 http 拦截器处理
  }
}

function goRegister() {
  router.push({ name: 'Register' });
}

function goForgot() {
  router.push({ name: 'ForgotPassword' });
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
  width: 360px;
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

.links {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
</style>

