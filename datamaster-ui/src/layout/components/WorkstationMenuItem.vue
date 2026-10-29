<template>
  <div class="workstation-menu-node">
    <button type="button" class="workstation-menu-entry" :class="{ 'is-active': !item.children.length && activePath === item.path, 'is-parent-active': item.children.length && containsActive }" :aria-expanded="item.children.length ? expanded : undefined" @click="select">
      <svg-icon :icon-class="item.iconName" />
      <span>{{ item.title }}</span>
      <DownOutlined v-if="item.children.length" class="workstation-menu-arrow" :class="{ 'is-collapsed': !expanded }" />
    </button>
    <div v-if="item.children.length && expanded" class="workstation-menu-children">
      <workstation-menu-item v-for="child in item.children" :key="child.path" :item="child" :active-path="activePath" @navigate="emit('navigate', $event)" />
    </div>
  </div>
</template>

<script setup name="WorkstationMenuItem">
import { computed, ref, watch } from 'vue';
import { DownOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  item: { type: Object, required: true },
  activePath: { type: String, default: '' },
});
const emit = defineEmits(['navigate']);
const expanded = ref(false);
function hasPath(item, path) {
  return item.path === path || item.children.some((child) => hasPath(child, path));
}
const containsActive = computed(() => hasPath(props.item, props.activePath));
watch(containsActive, (active) => { if (active) expanded.value = true; }, { immediate: true });
function select() {
  if (props.item.children.length) expanded.value = !expanded.value;
  else emit('navigate', props.item);
}
</script>

<style lang="scss" scoped>
.workstation-menu-entry { display: flex; align-items: center; gap: 9px; width: 100%; min-width: 0; padding: 9px 10px; color: #68758d; font-size: 12px; text-align: left; background: transparent; border: 1px solid transparent; border-radius: 9px; cursor: pointer; }
.workstation-menu-entry > span { min-width: 0; overflow-wrap: anywhere; }
.workstation-menu-entry:hover { color: #425ad2; background: #f5f7ff; }
.workstation-menu-entry.is-active { color: #4058d4; font-weight: 600; background: #edf0ff; border-color: #e0e5ff; box-shadow: inset 3px 0 #6076e9; }
.workstation-menu-entry.is-parent-active { color: #4058d4; font-weight: 600; }
.workstation-menu-entry :deep(.svg-icon) { flex-shrink: 0; font-size: 14px; }
.workstation-menu-arrow { flex-shrink: 0; margin-left: auto; color: #9aa7ba; font-size: 10px; transition: transform .2s ease; }
.workstation-menu-arrow.is-collapsed { transform: rotate(-90deg); }
.workstation-menu-children { margin-left: 16px; padding-left: 6px; border-left: 1px solid #e1e7f0; }
</style>
