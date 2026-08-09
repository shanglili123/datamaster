<template>
  <div>
    <a-dropdown trigger="click" @click="(e) => handleSetSize(e.key)">
      <div class="size-icon--style">
        <svg-icon class-name="size-icon" icon-class="size" />
      </div>
      <template #overlay>
        <a-menu>
          <a-menu-item v-for="item of sizeOptions" :key="item.value" :disabled="size === item.value">
            {{ item.label }}
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<script setup>

import useAppStore from "@/store/system/app";

const appStore = useAppStore();
const size = computed(() => appStore.size);
const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const sizeOptions = ref([
  { label: "较大", value: "large" },
  { label: "默认", value: "default" },
  { label: "稍小", value: "small" },
]);

function handleSetSize(size) {
  proxy.$modal.loading("正在设置布局大小，请稍候...");
  appStore.setSize(size);
  setTimeout("window.location.reload()", 1000);
}
</script>

<style lang='scss' scoped>
.size-icon--style {
  font-size: 18px;
  line-height: 50px;
  padding-right: 7px;
}
</style>

