<template>
  <button class="sidebar-toggle" type="button" @click="toggleClick" :aria-label="isActive ? '收起侧边栏' : '展开侧边栏'">
    <svg
      :class="{ 'is-active': isActive }"
      class="hamburger"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      fill="none"
    >
      <rect x="3.5" y="4" width="17" height="16" rx="3" stroke="currentColor" stroke-width="1.8" />
      <path d="M9 4.5V19.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
      <path d="M14.5 9L17.5 12L14.5 15" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
  </button>
</template>

<script setup>
defineProps({
  isActive: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits()
const toggleClick = () => {
  emit('toggleClick');
}
</script>

<style scoped>
.sidebar-toggle {
  width: 42px;
  height: 50px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.sidebar-toggle:hover {
  color: #1677ff;
  background: #f1f5f9;
}

.hamburger {
  display: inline-block;
  vertical-align: middle;
  width: 22px;
  height: 22px;
  transition: transform 0.18s ease;
}

.hamburger.is-active {
  transform: rotate(180deg);
}
</style>
