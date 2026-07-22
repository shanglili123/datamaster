<!-- eslint-disable vue/require-toggle-inside-transition -->
<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <div class="app-main-inner">
          <keep-alive :include="tagsViewStore.cachedViews">
            <component v-if="!route.meta.link" :is="Component" :key="route.path"/>
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

const tagsViewStore = useTagsViewStore()
</script>

<style lang="scss" scoped>
.app-main {
  /* 50= navbar  50  */
  //min-height: calc(100vh - 50px);
  //width: 100%;
  //position: relative;
  //overflow: hidden;
  background-color: var(--dm-bg-layout, #eef3f8);
  min-height: calc(100vh) !important;
  width: 100%;
  position: relative;
  overflow-y: auto;
  overflow-x: hidden;
  .app-main-inner{
    width: 100%;
    height: 100%;
    padding: 16px;
  }
}

.fixed-header + .app-main {
  padding-top: 60px;
}

.hasTagsView {
  .app-main {
    /* 84 = navbar + tags-view = 60 + 34 */
    min-height: calc(100vh - 100px);
  }

  .fixed-header + .app-main {
    padding-top: 100px;
  }
}
</style>

<style lang="scss">
// fix css style bug in open el-dialog
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 6px;
  }
}

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


