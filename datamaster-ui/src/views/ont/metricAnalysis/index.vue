<template>
  <div class="app-container metric-analysis-entry">
    <div class="entry-toolbar">
      <strong>指标分析</strong>
      <a-select v-model:value="ontologyId" :options="options" :loading="loading" placeholder="选择本体" show-search option-filter-prop="label" style="width: 360px" />
      <span>按已发布指标版本查询，保存个人分析条件</span>
    </div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <MetricAnalysisPanel v-if="ontologyId" :key="`${spaceKey}:${ontologyId}`" :ontology-id="ontologyId" :initial-selection="initialSelection" />
    <a-empty v-else-if="!loading" description="当前空间暂无可选本体，请先创建本体并发布指标。" />
  </div>
</template>

<script setup name="MetricAnalysisEntry">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import useUserStore from '@/store/system/user'
import { listOntology } from '@/api/ont/ontology'
import { loadPagedOptions } from '@/api/ont/metricOptions'
import { useRoute } from 'vue-router'
import { positiveRouteId, routeText } from '@/utils/analysisNavigation'
import MetricAnalysisPanel from '../workspace/components/MetricAnalysisPanel.vue'

const route = useRoute()
const initialSelection = computed(() => String(route.query.ontologyId || '') === String(ontologyId.value || '') ? {
  metricId: positiveRouteId(route.query.metricId), version: positiveRouteId(route.query.version)
} : null)
const userStore = useUserStore()
const spaceKey = computed(() => `${userStore.spaceId || ''}:${userStore.spaceCode || ''}`)
const options = ref([])
const ontologyId = ref()
const loading = ref(false)
const error = ref('')
let sequence = 0
watch(() => [spaceKey.value, route.query.ontologyId, route.query.metricId, route.query.version, route.query.spaceId], async () => {
  const current = ++sequence
  options.value = []; ontologyId.value = undefined; error.value = ''; loading.value = false
  if (!userStore.spaceId && !userStore.spaceCode) return
  loading.value = true
  try {
    const items = await loadPagedOptions(listOntology, { spaceId: userStore.spaceId, spaceCode: userStore.spaceCode }, { isCurrent: () => current === sequence })
    if (current !== sequence) return
    if (routeText(route.query.spaceId) && String(route.query.spaceId) !== String(userStore.spaceId)) { error.value = '此分析链接属于其他空间，请切回原空间后打开'; return }
    options.value = items.map(item => ({ value: String(item.id), label: item.name }))
    const requested = positiveRouteId(route.query.ontologyId)
    if (requested && !options.value.some(item => item.value === requested)) { error.value = '链接中的本体不属于当前可访问空间'; return }
    ontologyId.value = requested || options.value[0]?.value
  } catch (failure) {
    if (current === sequence) error.value = `本体列表加载失败：${failure?.response?.data?.msg || failure?.message || '请检查当前空间及本体查询权限'}`
  } finally { if (current === sequence) loading.value = false }
}, { immediate: true })
onBeforeUnmount(() => { ++sequence })
</script>

<style lang="scss" scoped>
.metric-analysis-entry { display: flex; flex-direction: column; gap: 16px; }
.entry-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 16px; }
.entry-toolbar span { color: #606b7b; font-size: 13px; }
</style>
