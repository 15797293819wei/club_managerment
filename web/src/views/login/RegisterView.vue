<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">注册账号</h1>
      <p class="subtitle">创建您的学生账号</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleSubmit">
            注册
          </el-button>
        </el-form-item>
        <div class="links center">
          <el-link type="primary" @click="goLogin">返回登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { register } from '@/api/auth';

const router = useRouter();
const formRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  studentId: '',
  email: '',
  phone: ''
});

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

async function handleSubmit() {
  await formRef.value?.validate();
  loading.value = true;
  try {
    await register({
      username: form.username.trim(),
      realName: form.realName.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      studentId: form.studentId || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined
    });
    ElMessage.success('注册成功，请登录');
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
  width: 380px;
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

.links.center {
  display: flex;
  justify-content: center;
}
</style>

