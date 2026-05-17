<template>
  <div class="roles-page">
    <el-card class="mb16" header="角色管理">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索角色代码/名称"
          class="w220"
          clearable
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select v-model="filters.status" placeholder="状态" class="w140" clearable @change="reload">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="禁用" />
        </el-select>
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button type="primary" @click="openCreate">新增角色</el-button>
      </div>

      <el-table :data="records" :loading="loading" size="small">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="roleCode" label="角色代码" width="160" />
        <el-table-column prop="roleName" label="角色名称" width="180" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button text size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              text
              size="small"
              type="warning"
              @click="toggleStatus(row)"
            >
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

    <el-dialog v-model="dialog.visible" :title="dialog.editing ? '编辑角色' : '新增角色'" width="520px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="角色代码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="dialog.editing" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
  listRoles,
  createRole,
  updateRole,
  changeRoleStatus,
  deleteRole,
  type RoleInfo,
  type RoleSavePayload,
  type RoleUpdatePayload
} from '@/api/roles';

const filters = reactive({
  keyword: '',
  status: undefined as number | undefined
});

const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);
const records = ref<RoleInfo[]>([]);

const dialog = reactive({
  visible: false,
  editing: false
});
const formRef = ref<FormInstance>();
const form = reactive<RoleSavePayload & { id?: number }>({
  id: undefined,
  roleCode: '',
  roleName: '',
  description: '',
  status: 1
});
const saving = ref(false);

const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色代码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
};

async function load() {
  loading.value = true;
  try {
    const res = await listRoles({
      page: page.value,
      size: size.value,
      keyword: filters.keyword?.trim() || undefined,
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
  form.id = undefined;
  form.roleCode = '';
  form.roleName = '';
  form.description = '';
  form.status = 1;
  dialog.visible = true;
}

function openEdit(row: RoleInfo) {
  dialog.editing = true;
  form.id = row.id;
  form.roleCode = row.roleCode;
  form.roleName = row.roleName;
  form.description = row.description || '';
  form.status = row.status;
  dialog.visible = true;
}

async function save() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload: RoleSavePayload | RoleUpdatePayload = {
      roleCode: form.roleCode,
      roleName: form.roleName,
      description: form.description,
      status: form.status
    };
    if (dialog.editing && form.id) {
      await updateRole(form.id, payload as RoleUpdatePayload);
      ElMessage.success('角色已更新');
    } else {
      await createRole(payload as RoleSavePayload);
      ElMessage.success('角色已创建');
    }
    dialog.visible = false;
    load();
  } finally {
    saving.value = false;
  }
}

async function toggleStatus(row: RoleInfo) {
  const target = row.status === 1 ? 0 : 1;
  await changeRoleStatus(row.id, { status: target });
  ElMessage.success(target === 1 ? '已启用' : '已禁用');
  load();
}

async function remove(row: RoleInfo) {
  await ElMessageBox.confirm(`确定要删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' });
  await deleteRole(row.id);
  ElMessage.success('删除成功');
  load();
}

onMounted(() => {
  load();
});
</script>

<style scoped>
.roles-page {
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

.w220 {
  width: 220px;
}

.mb16 {
  margin-bottom: 16px;
}
</style>
