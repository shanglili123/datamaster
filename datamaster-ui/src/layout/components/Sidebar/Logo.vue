<template>
  <div
    class="sidebar-logo-container"
    :class="{ collapse: collapse }"
    :style="{
      backgroundColor:
        sideTheme === 'theme-dark'
          ? variables.menuBackground
          : variables.menuLightBackground,
    }"
  >
    <transition name="sidebarLogoFade">
      <router-link
        v-if="collapse"
        key="collapse"
        class="sidebar-logo-link"
        to="/"
      >
        <img
          src="/datamaster-favicon.svg"
          class="sidebar-logo-image sidebar-logo-icon"
          alt="DataMaster"
        />
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <img
          src="/datamaster-logo.svg"
          class="sidebar-logo-image sidebar-logo-full"
          alt="DataMaster"
        />
      </router-link>
    </transition>
  </div>
</template>

<script setup>
import variables from "@/assets/system/styles/variables.module.scss";

import useSettingsStore from "@/store/system/settings";

import { computed } from "vue";

defineProps({
  collapse: {
    type: Boolean,
    required: true,
  },
  currentRoute: {
    type: String,
    default: "/",
  },
});

const settingsStore = useSettingsStore();
const sideTheme = computed(() => settingsStore.sideTheme);
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 76px;
  line-height: 76px;
  background: #ffffff;
  text-align: center;
  overflow: hidden;

  &::after {
    content: "";
    position: absolute;
    right: 16px;
    bottom: 0;
    left: 16px;
    height: 1px;
    background: #e5e7eb;
    pointer-events: none;
  }

  & .sidebar-logo-link {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    height: 100%;
    width: 100%;
    padding: 0 14px;
    box-sizing: border-box;

    & .sidebar-logo-image {
      display: block;
      vertical-align: middle;
      object-fit: contain;
      transform: translateY(4px);
    }

    & .sidebar-logo-full {
      width: 100%;
      max-width: 220px;
      height: 50px;
      flex: 0 1 220px;
    }
  }

  &.collapse {
    .sidebar-logo-link {
      justify-content: center;
      padding: 0;
    }

    .sidebar-logo-image {
      width: 46px;
      height: 46px;
      max-width: 46px;
      transform: translateY(4px);
    }

  }
}
</style>
