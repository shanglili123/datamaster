<!-- eslint-disable vue/require-toggle-inside-transition -->
<template>
  <section class="app-main" :class="{ 'app-main--standalone': standalone }">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <div class="app-main-inner">
          <keep-alive :include="tagsViewStore.cachedViews">
            <component v-if="!route.meta.link" :is="Component" :key="route.query.id == null ? route.path : `${route.path}:${route.query.id}`"/>
          </keep-alive>
        </div>
      </transition>
    </router-view>
    <iframe-toggle />
  </section>
</template>

<script setup>
import iframeToggle from "./IframeToggle/index"
import useTagsViewStore from '@/store/system/tagsView'

defineProps({ standalone: { type: Boolean, default: false } })
const tagsViewStore = useTagsViewStore()
</script>

<style lang="scss" scoped>
.app-main {
  width: 100%;
  min-width: 0;
  position: relative;

  .app-main-inner {
    width: 100%;
    min-height: calc(100vh - 222px);
    padding: 16px;
    box-sizing: border-box;
  }
}

.app-main--standalone {
  min-height: 100vh;
  background: var(--dm-bg-layout, #eef3f8);

  .app-main-inner {
    min-height: 100vh;
  }
}
</style>

<style lang="scss">
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background-color: transparent;
}

::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background-color: #94a3b8;
}
</style>


