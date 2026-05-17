<template>
  <div class="users-page">
    <el-card class="mb16" header="用户管理">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索姓名/用户名/学号"
          class="w220"
          clearable
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-input
          v-model="filters.username"
          placeholder="用户名"
          class="w180"
          clearable
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-input
          v-model="filters.studentId"
          placeholder="学号"
          class="w160"
          clearable
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select
          v-model="filters.status"
          placeholder="状态"
          class="w140"
          clearable
          @change="reload"
        >
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="禁用" />
        </el-select>
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>

      <el-table :data="records" :loading="loading" size="small">
        <el-table-column type="selection" width="50" />
        <el-table-column type="index" width="60" label="#" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="140" />
        <el-table-column prop="studentId" label="学号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button text size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text size="small" type="warning" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button text size="small" type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialog.visible" :title="dialog.editing ? '编辑用户' : '新增用户'" width="640px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="dialog.editing" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!dialog.editing">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="form.studentId" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  listUsers,
  createUser,
  updateUser,
  changeUserStatus,
  deleteUser,
  type UserSummary,
  type UserSavePayload,
  type UserUpdatePayload
} from '@/api/users';

const filters = reactive({
  keyword: '',
  username: '',
  studentId: '',
  status: undefined as number | undefined
});

const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);
const records = ref<UserSummary[]>([]);

const dialog = reactive({
  visible: false,
  editing: false
});
const formRef = ref<FormInstance>();
const form = reactive<UserSavePayload & { id?: number }>({
  id: undefined,
  username: '',
  password: '',
  realName: '',
  studentId: '',
  email: '',
  phone: '',
  gender: undefined,
  avatar: '',
  status: 1,
  roleIds: []
});
const saving = ref(false);

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
};

async function load() {
  loading.value = true;
  try {
    const res = await listUsers({
      page: page.value,
      size: size.value,
      keyword: filters.keyword?.trim() || undefined,
      username: filters.username?.trim() || undefined,
      studentId: filters.studentId?.trim() || undefined,
      status: filters.status
    });
    records.value = res.records || [];
    total.value = res.total || 0;
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

function openCreate() {
  dialog.editing = false;
  Object.assign(form, {
    id: undefined,
    username: '',
    password: '',
    realName: '',
    studentId: '',
    email: '',
    phone: '',
    gender: undefined,
    avatar: '',
    status: 1,
    roleIds: []
  });
  dialog.visible = true;
}

function openEdit(row: UserSummary) {
  dialog.editing = true;
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName || '',
    studentId: row.studentId || '',
    email: row.email || '',
    phone: row.phone || '',
    status: row.status
  });
  dialog.visible = true;
}

async function save() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (dialog.editing && form.id) {
      const payload: UserUpdatePayload = {
        username: form.username,
        password: form.password || undefined,
        realName: form.realName,
        studentId: form.studentId || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        status: form.status
      };
      await updateUser(form.id, payload);
      ElMessage.success('用户已更新');
    } else {
      const payload: UserSavePayload = {
        username: form.username,
        password: form.password,
        realName: form.realName,
        studentId: form.studentId || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        status: form.status,
        roleIds: form.roleIds
      };
      await createUser(payload);
      ElMessage.success('用户已创建');
    }
    dialog.visible = false;
    load();
  } finally {
    saving.value = false;
  }
}

async function toggleStatus(row: UserSummary) {
  const target = row.status === 1 ? 0 : 1;
  await changeUserStatus(row.id, { status: target });
  ElMessage.success(target === 1 ? '已启用' : '已禁用');
  load();
}

async function remove(row: UserSummary) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' });
  await deleteUser(row.id);
  ElMessage.success('删除成功');
  load();
}

onMounted(() => {
  load();
});
</script>

<style scoped>
.users-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.w140 {
  width: 140px;
}

.w160 {
  width: 160px;
}

.w180 {
  width: 180px;
}

.w220 {
  width: 220px;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
