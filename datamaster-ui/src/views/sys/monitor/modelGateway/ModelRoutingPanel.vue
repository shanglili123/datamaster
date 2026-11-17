<template>
  <section>
    <h3>当前空间路由策略</h3>
    <a-alert type="info" show-icon message="页面配置按空间生效，保存不会调用模型" description="无页面配置时沿用部署默认；保存后覆盖当前空间，新建请求生效，在途请求不切换。关闭路由后使用当前空间默认模型，不是关闭模型服务。" />
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-spin :spinning="loading">
      <p v-if="loaded">来源：{{ form.source === 'DATABASE' ? '页面已保存配置' : '部署默认配置' }} · revision：{{ form.revision }}</p>
      <a-form layout="vertical" :disabled="!editable || !loaded || loading || saving">
        <a-form-item label="启用模型路由"><a-switch v-model:checked="form.enabled" /></a-form-item>
        <a-form-item label="经济模型" :required="form.enabled"><a-select v-model:value="form.economyProfileId" :options="options" allow-clear placeholder="选择当前空间已启用的模型" /></a-form-item>
        <a-form-item label="高级模型" :required="form.enabled"><a-select v-model:value="form.advancedProfileId" :options="options" allow-clear placeholder="复杂、工具及高风险请求使用" /></a-form-item>
        <a-form-item label="复杂输入字符阈值（1–16000）"><a-input-number v-model:value="form.complexInputCharacters" :min="1" :max="16000" :precision="0" /></a-form-item>
        <a-form-item label="语义分类"><a-switch v-model:checked="form.semanticDecisionEnabled" /><p>开启后，符合条件的请求额外调用一次分类模型并计量；高风险及工具请求不会因此降级。</p></a-form-item>
        <a-form-item v-if="form.semanticDecisionEnabled" label="分类模型"><a-select v-model:value="form.decisionProfileId" :options="options" allow-clear placeholder="不选则使用经济模型" /></a-form-item>
        <a-form-item label="严格 JSON Schema"><a-switch v-model:checked="form.strictJsonSchema" /><p>仅在经济与高级模型均已验证支持所需 Schema 后启用；保存不会验证供应商兼容性。</p></a-form-item>
      </a-form>
    </a-spin>
    <a-space>
      <a-button :disabled="saving || loading || !spaceId" @click="load">重新加载</a-button>
      <a-button v-if="editable" type="primary" :loading="saving" :disabled="!loaded || loading || !!profilesError" @click="save">保存当前空间策略</a-button>
    </a-space>
    <p v-if="profilesError" role="alert">{{ profilesError }}；无法确认模型选项，暂不能保存。</p>
    <p>不跨空间选择模型，不自动切换失败供应商；模型被路由引用后，需先调整路由才能停用或删除。DB-GPT接口同步模式下，远程工具问数/报告使用高级模型，关闭路由则使用空间默认模型，且须已同步在线；不控制其内部每一步的路由。</p>
  </section>
</template>
<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { getModelRouting, saveModelRouting } from '@/api/ai/modelGateway'
import { modelRoutingPayload } from '@/utils/modelRouting'
const props = defineProps({
  spaceId: { type: [String, Number], default: null },
  spaceCode: { type: String, default: '' },
  profiles: { type: Array, default: () => [] },
  profilesError: { type: String, default: '' },
  editable: { type: Boolean, default: false }
})
const blank = () => ({ enabled: false, economyProfileId: undefined, advancedProfileId: undefined, semanticDecisionEnabled: false, decisionProfileId: undefined, strictJsonSchema: false, complexInputCharacters: 12000, revision: 0, source: 'DEPLOYMENT' })
const form = ref(blank()), loading = ref(false), saving = ref(false), loaded = ref(false), error = ref('')
let sequence = 0
const options = computed(() => props.profiles.filter(profile => profile.enabled && profile.hasApiKey).map(profile => ({ value: String(profile.id), label: `${profile.name} · ${profile.model}` })))
function apply(value) {
  if (!value || String(value.spaceId) !== String(props.spaceId) || value.spaceCode !== props.spaceCode || !Number.isInteger(value.revision)) throw new Error('路由响应空间或版本不一致')
  form.value = { ...value, economyProfileId: value.economyProfileId == null ? undefined : String(value.economyProfileId), advancedProfileId: value.advancedProfileId == null ? undefined : String(value.advancedProfileId), decisionProfileId: value.decisionProfileId == null ? undefined : String(value.decisionProfileId) }
}
async function load() {
  if (saving.value) return
  const token = ++sequence; loaded.value = false; loading.value = false; error.value = ''; form.value = blank()
  if (!props.spaceId) return
  loading.value = true
  try {
    const response = await getModelRouting({ spaceId: props.spaceId, spaceCode: props.spaceCode })
    if (token !== sequence) return
    apply(response.data); loaded.value = true
  } catch (failure) { if (token === sequence) error.value = failure?.message || '路由策略读取失败' }
  finally { if (token === sequence) loading.value = false }
}
function save() {
  if (!props.editable || !loaded.value || loading.value || saving.value || props.profilesError) return
  let payload
  try { payload = modelRoutingPayload(form.value, props.profiles, { spaceId: props.spaceId, spaceCode: props.spaceCode }) }
  catch (failure) { error.value = failure.message; return }
  const token = sequence, original = JSON.stringify(form.value)
  Modal.confirm({ title: '保存当前空间模型路由策略？', content: '后续请求可能使用不同模型。语义分类会增加一次调用；严格Schema兼容性需人工确认。本操作不调用模型。', async onOk() {
    if (token !== sequence || original !== JSON.stringify(form.value) || saving.value) return
    saving.value = true; error.value = ''
    try {
      const response = await saveModelRouting(payload)
      if (token !== sequence) return
      apply(response.data); message.success('当前空间路由策略已保存')
    } catch (failure) { if (token === sequence) error.value = failure?.message || '保存失败，请重新加载核对版本' }
    finally { if (token === sequence) saving.value = false }
  } })
}
watch(() => [props.spaceId, props.spaceCode], () => { ++sequence; saving.value = false; load() }, { immediate: true })
onBeforeUnmount(() => { ++sequence })
</script>
<style scoped>
section { display: flex; flex-direction: column; gap: 12px; }
.ant-select { max-width: 600px; width: 100%; }
p { margin: 8px 0; }
</style>
