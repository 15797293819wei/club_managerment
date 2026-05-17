<template>
  <div class="layout">
    <aside class="sidebar">
      <SidebarMenu />
    </aside>
    <section class="main">
      <AppHeader />
      <div class="content">
        <RouterView />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/layout/AppHeader.vue';
import SidebarMenu from '@/components/layout/SidebarMenu.vue';
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 240px;
  background-color: #111827;
  color: #e5e7eb;
  display: flex;
  flex-direction: column;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f3f4f6;
}

.content {
  flex: 1;
  padding: 16px 24px;
  overflow: auto;
  display: block;
}

.content > * {
  width: 100%;
}
</style>

