<template>
  <el-form
    ref="innerFormRef"
    v-bind="$attrs"
    :model="model"
    :rules="rules"
  >
    <slot />
  </el-form>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';

interface Props {
  model: Record<string, any>;
  rules?: FormRules;
}

defineProps<Props>();

const innerFormRef = ref<FormInstance>();

function validate() {
  return innerFormRef.value?.validate();
}

function resetFields() {
  return innerFormRef.value?.resetFields();
}

defineExpose({
  validate,
  resetFields
});
</script>

