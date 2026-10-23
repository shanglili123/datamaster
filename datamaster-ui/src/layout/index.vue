<template>
  <div class="app-wrapper">
    <app-main v-if="isStandalonePage" :standalone="true" />
    <space-workstation v-else><app-main /></space-workstation>
    <data-agent-assistant />
  </div>
</template>

<script setup name="Layout">
import { computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import AppMain from './components/AppMain.vue';
import SpaceWorkstation from './components/SpaceWorkstation.vue';
import DataAgentAssistant from './components/DataAgentAssistant.vue';
import usePermissionStore from '@/store/system/permission';

const route = useRoute();
const permissionStore = usePermissionStore();
// 首页和数据探索拥有自己的入口与流程导航，其余页面统一使用空间工作站布局。
const isStandalonePage = computed(() => ['/', '/index', '/explore'].includes(route.path));
watch(() => route.path, (path) => {
  if (path === '/' || path === '/index') permissionStore.resetHomeMenus();
}, { immediate: true });
</script>

<style lang="scss" scoped>
.app-wrapper { min-height: 100vh; }
</style>
