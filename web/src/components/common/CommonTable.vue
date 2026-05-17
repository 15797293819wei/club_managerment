<template>
  <el-table
    v-bind="$attrs"
    :data="data"
    :loading="loading"
    border
  >
    <slot />
  </el-table>
</template>

<script setup lang="ts">
interface Props {
  data: any[];
  loading?: boolean;
}

withDefaults(defineProps<Props>(), {
  data: () => [],
  loading: false
});
</script>

