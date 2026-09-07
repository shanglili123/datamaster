<template>
  <a-modal
    :open="open"
    title="AI 生成动作"
    width="680px"
    wrap-class-name="ontology-workspace-modal ontology-modal--form"
    destroy-on-close
    :confirm-loading="generating"
    ok-text="关闭"
    cancel-text="取消"
    @cancel="$emit('update:open', false)"
    @ok="$emit('update:open', false)"
  >
    <a-form :label-col="{ style: { width: '90px' } }" class="ontology-form-grid">
      <a-form-item label="触发概念">
        <a-select
          v-model:value="selectedConceptId"
          placeholder="留空 = 生成整个本体的动作"
          allow-clear
          show-search
          option-filter-prop="label"
          :options="conceptOptions"
          style="width: 320px"
        />
      </a-form-item>
      <a-form-item label="动作描述" required class="ontology-form-grid__full">
        <a-textarea
          v-model:value="userPrompt"
          placeholder="请描述你想要生成的动作功能，例如：帮我创建一个扣减库存的多步骤动作，先检查库存是否充足，再扣减库存数量，最后同步更新订单状态"
          :auto-size="{ minRows: 3, maxRows: 6 }"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item class="ontology-form-grid__full">
        <a-button type="primary" :loading="generating" :disabled="!userPrompt.trim()" @click="handleGenerate">
          <template #icon><ThunderboltOutlined /></template>
          生成动作
        </a-button>
      </a-form-item>
    </a-form>

    <a-divider v-if="previewActions.length" style="margin: 12px 0">生成结果（点击「使用」回填到表单）</a-divider>

    <div v-if="previewActions.length" class="ai-preview-list">
      <div v-for="(action, idx) in previewActions" :key="idx" class="ai-preview-card">
        <div class="ai-preview-card-head">
          <div>
            <span class="ai-preview-name">{{ action.actionName || '未命名动作' }}</span>
            <a-tag :color="action.actionType === 'COMPOSITE' ? 'blue' : 'green'" style="margin-left: 8px;">
              {{ action.actionType === 'COMPOSITE' ? '多步骤' : '函数' }}
            </a-tag>
          </div>
          <a-button type="primary" size="small" @click="handleUse(action)">
            <template #icon><CheckOutlined /></template>
            使用
          </a-button>
        </div>
        <div class="ai-preview-desc">{{ action.actionDescription || '-' }}</div>
        <div v-if="action.actionType === 'COMPOSITE' && action.executionSteps" class="ai-preview-detail">
          <div class="ai-preview-detail-title">执行步骤：</div>
          <div v-for="(step, si) in parseSteps(action.executionSteps)" :key="si" class="ai-preview-step">
            <a-tag color="cyan">{{ si + 1 }}</a-tag>
            <a-tag :color="stepActionColor(step.actionType)">{{ step.actionType }}</a-tag>
            <span>{{ step.relationCode || step.conceptCode || '-' }}</span>
            <span v-if="step.paramConfig && step.paramConfig.length" style="color:#999;margin-left:8px;">
              设置 {{ step.paramConfig.length }} 个字段
            </span>
          </div>
        </div>
        <div v-if="action.actionType === 'FUNCTION'" class="ai-preview-detail">
          <div class="ai-preview-detail-title">函数代码（{{ action.functionLang || '-' }}）：</div>
          <pre class="ai-preview-code">{{ action.functionBody || '-' }}</pre>
        </div>
      </div>
    </div>

    <a-empty v-if="!generating && !previewActions.length && searchDone" description="AI 未生成动作，请尝试调整描述后重新生成" />
  </a-modal>
</template>

<script setup name="AiGenerateDialog">
import { ref } from 'vue'
import { aiGenerateActions } from '@/api/ont/action'
import { ThunderboltOutlined, CheckOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  conceptOptions: { type: Array, default: () => [] },
  ontologyId: { type: [Number, String], required: true }
})

const emit = defineEmits(['update:open', 'use-action'])

const { proxy } = getCurrentInstance()

const userPrompt = ref('')
const selectedConceptId = ref(undefined)
const generating = ref(false)
const previewActions = ref([])
const searchDone = ref(false)

function parseSteps(stepsJson) {
  try {
    const steps = typeof stepsJson === 'string' ? JSON.parse(stepsJson) : stepsJson
    return Array.isArray(steps) ? steps : []
  } catch {
    return []
  }
}

function stepActionColor(type) {
  if (type === 'CREATE') return 'green'
  if (type === 'UPDATE') return 'orange'
  if (type === 'DELETE') return 'red'
  return 'default'
}

async function handleGenerate() {
  if (!userPrompt.value.trim()) return
  generating.value = true
  searchDone.value = false
  previewActions.value = []
  try {
    const res = await aiGenerateActions({
      ontologyId: props.ontologyId,
      conceptId: selectedConceptId.value || undefined,
      prompt: userPrompt.value.trim()
    })
    if (res.data?.qualityWarning) {
      proxy.$modal.msgWarning(res.data.qualityWarning)
    }
    previewActions.value = res.data?.actions || []
    searchDone.value = true
  } catch (e) {
    proxy.$modal.msgError('AI 生成失败：' + (e.message || '未知错误'))
    searchDone.value = true
  } finally {
    generating.value = false
  }
}

function handleUse(action) {
  emit('use-action', { ...action, triggerConceptId: selectedConceptId.value })
}
</script>

<style scoped>
.ai-preview-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}
.ai-preview-card {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px 16px;
  background: #fafafa;
}
.ai-preview-card:hover {
  border-color: #1677ff;
}
.ai-preview-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ai-preview-name {
  font-weight: 600;
  font-size: 14px;
}
.ai-preview-desc {
  color: #666;
  font-size: 13px;
  margin-top: 6px;
  line-height: 1.5;
}
.ai-preview-detail {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e8e8e8;
}
.ai-preview-detail-title {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}
.ai-preview-step {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 0;
  font-size: 13px;
}
.ai-preview-code {
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 12px;
  max-height: 160px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
