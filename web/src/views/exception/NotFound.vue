<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在或已被移除。</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

function goHome() {
  router.push({ name: 'Dashboard' });
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #4b5563;
}

h1 {
  font-size: 64px;
  margin: 0;
}
</style>

