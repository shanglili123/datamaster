<template>
  <div :style="'height:' + height">
    <a-spin :spinning="loading" class="iframe-loading">
      <iframe ref="frameRef" :src="url" frameborder="0" scrolling="auto" height="100%" width="100%" allowfullscreen="true" webkitallowfullscreen="true" mozallowfullscreen="true"></iframe>
    </a-spin>
  </div>
</template>

<script setup>
const props = defineProps({
  src: {
    type: String,
    required: true,
  },
});

const height = ref(document.documentElement.clientHeight - 94.5 + "px;");
const loading = ref(true);
const url = computed(() => props.src);

onMounted(() => {
  setTimeout(() => {
    loading.value = false;
  }, 300);
  window.onresize = function temp() {
    height.value = document.documentElement.clientHeight - 94.5 + "px;";
  };
});
</script>

<style scoped>
.iframe-loading,
.iframe-loading .ant-spin-container {
  height: 100%;
}
</style>

