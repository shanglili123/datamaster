<template>
  <div class="sidebar-logo-container" :class="{ collapse: collapse }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/" aria-label="DataMaster">
        <span class="brand-mark"><i></i><b></b><em></em></span>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <span class="brand-copy">
          <strong>DataMaster</strong>
          <small>AI DATA INTELLIGENCE</small>
        </span>
        <span class="brand-mark"><i></i><b></b><em></em></span>
      </router-link>
    </transition>
  </div>
</template>

<script setup>
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
  height: 82px;
  background: transparent;
  overflow: hidden;

  &::after {
    content: "";
    position: absolute;
    right: 18px;
    bottom: 0;
    left: 18px;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(111, 161, 255, 0.42), transparent);
    pointer-events: none;
  }

  & .sidebar-logo-link {
    display: flex !important;
    align-items: center;
    justify-content: flex-start;
    flex-direction: row !important;
    flex-wrap: nowrap;
    height: 100%;
    width: 100%;
    gap: 11px;
    padding: 0 20px;
    box-sizing: border-box;

  }

  .brand-mark {
    position: relative;
    display: inline-flex;
    width: 36px;
    height: 36px;
    flex: 0 0 36px;
    align-items: center;
    justify-content: center;
    border: 1px solid rgba(124, 159, 255, 0.65);
    border-radius: 11px;
    background: linear-gradient(135deg, #5b7cff, #23d0c3);
    box-shadow: 0 0 20px rgba(77, 124, 255, 0.35);

    i, b, em {
      position: absolute;
      display: block;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #fff;
      box-shadow: 0 0 7px rgba(255, 255, 255, 0.9);
    }

    i { transform: translate(-8px, 7px); }
    b { transform: translate(7px, -8px); }
    em { width: 4px; height: 4px; transform: translate(8px, 8px); }

    &::before, &::after {
      position: absolute;
      width: 20px;
      height: 1px;
      content: '';
      background: rgba(255, 255, 255, 0.82);
      transform-origin: center;
    }

    &::before { transform: rotate(-45deg); }
    &::after { width: 16px; transform: rotate(38deg); }
  }

  .brand-copy {
    display: flex;
    min-width: 0;
    flex: 1 1 auto;
    flex-direction: column;
    line-height: 1;

    strong {
      color: #f5f8ff;
      font-size: 18px;
      font-weight: 650;
      letter-spacing: 0.2px;
    }

    small {
      margin-top: 5px;
      color: #7f9fd8;
      font-size: 8px;
      font-weight: 600;
      letter-spacing: 1.1px;
      white-space: nowrap;
    }
  }

  &:not(.collapse) .brand-mark {
    margin-left: auto;
  }

  &.collapse {
    .sidebar-logo-link {
      justify-content: center;
      padding: 0;
    }

    .brand-mark { width: 34px; height: 34px; flex-basis: 34px; }
  }
}
</style>
