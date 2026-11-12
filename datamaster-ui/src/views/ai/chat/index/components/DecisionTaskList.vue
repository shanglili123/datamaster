<template>
  <details class="decision-tasks" @toggle="onToggle">
    <summary>我的决策任务（当前空间，最近50条）</summary>
    <a-button :loading="loading" :disabled="disabled" @click="load">刷新任务</a-button>
    <p v-if="error" role="alert">{{ error }}</p>
    <ul><li v-for="task in tasks" :key="task.id">
      <span>{{ task.goal }} · {{ task.status }} · V{{ task.revision }}</span>
      <a-button type="link" :disabled="disabled" @click="selected = task.id">恢复查看</a-button>
    </li></ul>
    <DecisionProposal v-if="selected" :key="`${contextKey}:${selected}`" :decision-id="selected" :context-key="`${contextKey}:tasks`" :ontology-id="tasks.find(t => t.id === selected)?.ontologyId" :disabled="disabled" @updated="update" @reanalyze="emit('reanalyze', $event)" />
  </details>
</template>
<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { listDecisionTasks } from '@/api/ai/decision'
import DecisionProposal from './DecisionProposal.vue'
const props = defineProps({ spaceId: [String, Number], spaceCode: String, disabled: Boolean, contextKey: String })
const emit = defineEmits(['reanalyze'])
const tasks = ref([]), selected = ref(''), loading = ref(false), error = ref('')
let generation = 0
watch(() => [props.spaceId, props.spaceCode], () => { ++generation; tasks.value = []; selected.value = ''; loading.value = false; error.value = '' }, { flush: 'sync' })
async function load() {
  if (props.disabled || !props.spaceId) return
  const token = ++generation; loading.value = true; error.value = ''
  try { const res = await listDecisionTasks({ spaceId: props.spaceId, spaceCode: props.spaceCode }); if (token === generation) tasks.value = res.data || [] }
  catch (failure) { if (token === generation) error.value = failure?.message || '任务加载失败' }
  finally { if (token === generation) loading.value = false }
}
function onToggle(event) { if (event.target.open && !tasks.value.length) load() }
function update(proposal) { const index = tasks.value.findIndex(t => t.id === proposal.id); if (index >= 0) tasks.value[index] = { ...tasks.value[index], id: proposal.id, goal: proposal.goal, status: proposal.status, revision: proposal.revision, ontologyId: proposal.ontologyId } }
onBeforeUnmount(() => { ++generation })
</script>
<style scoped>
.decision-tasks { margin: 12px 0; padding: 12px; border: 1px solid #d9dfe8; border-radius: 6px; }
li { margin: 8px 0; overflow-wrap: anywhere; }
</style>
