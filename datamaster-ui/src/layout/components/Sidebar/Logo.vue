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
  height: 60px;
  line-height: 50px;
  background: #2b2f3a;
  text-align: center;
  overflow: hidden;

  &::after {
    content: "";
    position: absolute;
    right: 18px;
    bottom: 0;
    left: 18px;
    height: 1px;
    background: rgba(255, 255, 255, 0.22);
    pointer-events: none;
  }

  & .sidebar-logo-link {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    height: 100%;
    width: 100%;
    padding: 0 0 0 18px;
    box-sizing: border-box;

    & .sidebar-logo-image {
      display: block;
      vertical-align: middle;
      object-fit: contain;
    }

    & .sidebar-logo-full {
      width: 150px;
      height: 36px;
      flex: 0 0 150px;
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
    }

  }
}
</style>
