<template>
  <div class="app-container metric-entry">
    <div class="metric-entry__toolbar">
      <strong>本体指标</strong>
      <a-select v-model:value="ontologyId" :options="options" :loading="loading" placeholder="选择本体管理指标" show-search option-filter-prop="label" style="width: 360px" />
      <span>业务定义、ADS 字段映射与发布版本</span>
    </div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <MetricPanel v-if="ontologyId" :key="`${spaceKey}:${ontologyId}`" :ontology-id="ontologyId" />
    <a-empty v-else-if="!loading" description="请先选择本体；如无本体，请在本体管理中创建。" />
  </div>
</template>

<script setup name="OntologyMetricEntry">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import useUserStore from '@/store/system/user'
import { loadPagedOptions } from '@/api/ont/metricOptions'
import { listOntology } from '@/api/ont/ontology'
import MetricPanel from '../workspace/components/MetricPanel.vue'

const userStore = useUserStore()
const spaceKey = computed(() => `${userStore.spaceId || ''}:${userStore.spaceCode || ''}`)
const options = ref([])
const ontologyId = ref()
const loading = ref(false)
const error = ref('')
let sequence = 0
watch(spaceKey, async () => {
  const current = ++sequence
  options.value = []; ontologyId.value = undefined; error.value = ''
  if (!userStore.spaceId && !userStore.spaceCode) return
  loading.value = true
  try {
    const items = await loadPagedOptions(listOntology, { spaceId: userStore.spaceId, spaceCode: userStore.spaceCode })
    if (current !== sequence) return
    options.value = items.map(item => ({ value: item.id, label: item.name }))
    ontologyId.value = options.value[0]?.value
  } catch (failure) {
    if (current === sequence) error.value = '本体列表加载失败，请检查当前空间及本体查询权限。'
  } finally {
    if (current === sequence) loading.value = false
  }
}, { immediate: true })
onBeforeUnmount(() => { ++sequence })
</script>

<style lang="scss" scoped>
.metric-entry { display: flex; flex-direction: column; gap: 16px; }
.metric-entry__toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 16px; }
.metric-entry__toolbar span { color: #606b7b; font-size: 13px; }
</style>
