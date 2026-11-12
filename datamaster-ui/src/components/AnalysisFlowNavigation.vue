<template>
  <nav class="analysis-flow-nav" aria-label="指标分析决策流程">
    <span>业务流程：</span>
    <a-button :type="stage === 'metric' ? 'primary' : 'default'" size="small" :disabled="disabled" v-hasPermi="['ont:metric:list']" @click="go('/ont/metric')">1 指标管理</a-button>
    <span aria-hidden="true">→</span>
    <a-button :type="stage === 'analysis' ? 'primary' : 'default'" size="small" :disabled="disabled" v-hasPermi="['ont:metric:list']" @click="go('/ont/metric-analysis')">2 指标分析</a-button>
    <span aria-hidden="true">→</span>
    <a-button :type="stage === 'decision' ? 'primary' : 'default'" size="small" :disabled="disabled" v-hasPermi="['ai:ask-data:query']" @click="go('/ai/ask')">3 方案与决策任务</a-button>
    <small>跳转不自动查询、审批或执行</small>
  </nav>
</template>
<script setup>
import useUserStore from '@/store/system/user'
import { positiveRouteId, useAnalysisNavigation } from '@/utils/analysisNavigation'
const props = defineProps({ stage: String, ontologyId: [String, Number], disabled: Boolean })
const user = useUserStore()
const { open } = useAnalysisNavigation()
function go(path) {
  return open(path, { spaceId: String(user.spaceId || ''), ...(positiveRouteId(props.ontologyId) ? { ontologyId: String(props.ontologyId) } : {}) })
}
</script>
<style scoped>
.analysis-flow-nav { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; padding: 10px 12px; border: 1px solid #e8edf5; border-radius: 6px; margin-bottom: 12px; }
small { color: #667085; }
</style>
