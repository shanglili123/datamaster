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
  background-color: var(--dm-bg-layout, #eef3f8);
  width: 100%;
  position: relative;
  /* 由 main-container 撑满剩余高度：header(100px) 与 submenu-tabs 占用的空间已由 flex 布局扣除，
     不能再写死 min-height: calc(100vh)（会导致内容溢出产生页面级滚动条） */
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  .app-main-inner{
    width: 100%;
    height: 100%;
    padding: 16px;
    box-sizing: border-box;
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


