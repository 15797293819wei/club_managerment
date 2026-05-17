<template>
  <div class="demo-page">
    <el-card header="示例表格与表单">
      <CommonForm :model="form" :rules="rules" ref="formRef" label-width="90px" class="mb16">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option label="类型A" value="A" />
            <el-option label="类型B" value="B" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleAdd">添加</el-button>
        </el-form-item>
      </CommonForm>

      <CommonTable :data="records">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="120" />
      </CommonTable>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import CommonForm from '@/components/common/CommonForm.vue';
import CommonTable from '@/components/common/CommonTable.vue';

interface DemoRow {
  name: string;
  type: string;
}

const formRef = ref<FormInstance>();
const form = reactive<DemoRow>({
  name: '',
  type: ''
});
const records = ref<DemoRow[]>([]);

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
};

async function handleAdd() {
  await formRef.value?.validate();
  records.value.push({ name: form.name, type: form.type });
  form.name = '';
  form.type = '';
  ElMessage.success('已添加');
}
</script>

<style scoped>
.demo-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb16 {
  margin-bottom: 16px;
}
</style>

