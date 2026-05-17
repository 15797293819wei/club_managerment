<template>
  <div class="profile-page">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card header="基本信息">
          <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="80px">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="profileForm.realName" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="修改密码">
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="90px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPassword" @click="handleChangePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="角色与社团">
          <p>系统角色：</p>
          <el-tag v-for="role in roleList" :key="role.id" class="mr8" type="info">
            {{ role.roleName }}
          </el-tag>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { getProfile, updateProfile, changePassword, type ProfileDetail } from '@/api/profile';

const profile = ref<ProfileDetail | null>(null);
const profileFormRef = ref<FormInstance>();
const passwordFormRef = ref<FormInstance>();

const profileForm = reactive({
  username: '',
  realName: '',
  email: '',
  phone: ''
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: ''
});

const savingProfile = ref(false);
const savingPassword = ref(false);

const roleList = computed(() => profile.value?.roles ?? []);

const profileRules: FormRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
};

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
};

async function loadProfile() {
  const data = await getProfile();
  profile.value = data;
  profileForm.username = data.username;
  profileForm.realName = data.realName || '';
  profileForm.email = data.email || '';
  profileForm.phone = data.phone || '';
}

async function handleSaveProfile() {
  await profileFormRef.value?.validate();
  savingProfile.value = true;
  try {
    await updateProfile({
      realName: profileForm.realName,
      email: profileForm.email || undefined,
      phone: profileForm.phone || undefined
    });
    ElMessage.success('个人信息已更新');
    await loadProfile();
  } finally {
    savingProfile.value = false;
  }
}

async function handleChangePassword() {
  await passwordFormRef.value?.validate();
  savingPassword.value = true;
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    ElMessage.success('密码修改成功');
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
  } finally {
    savingPassword.value = false;
  }
}

onMounted(loadProfile);
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mr8 {
  margin-right: 8px;
  margin-bottom: 4px;
}
</style>

