<template>
  <section class="source-config">
    <a-alert type="info" show-icon message="指标查询来源配置" description="按当前本体的真实表绑定批准或撤销查询来源，不维护全局账号名单。账号由数据源管理提供；批准仅表示管理员确认，不能代替数据库实际只读授权。每次执行仍须通过空间、发布版本和字段治理检查。" />
    <a-alert v-if="settings && !settings.enabled" type="warning" show-icon message="运维已紧急关闭全部指标查询，页面批准不能覆盖此开关。" />
    <p v-if="settings">运行保护：超时 {{ settings.timeoutSeconds }} 秒 · 每服务进程并发上限 {{ settings.maxConcurrent }}。修改运行保护仍由运维管理。</p>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <div class="toolbar"><a-button :disabled="loading || saving" @click="reload">刷新来源</a-button><span>仅空间管理员或平台管理员可管理，且须具有对应功能权限。</span></div>
    <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="bindingId" :pagination="{ current: pageNum, pageSize, total, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] }" :scroll="{ x: 1500 }" @change="changePage">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'source'">{{ record.databaseName || '—' }} / {{ record.schemaName || '—' }} / {{ record.tableName }}</template>
        <template v-else-if="column.key === 'status'"><a-tag :color="colors[record.status]">{{ labels[record.status] || record.status }}</a-tag><div class="notice">{{ record.notice }}</div></template>
        <template v-else-if="column.key === 'approved'">{{ record.approvedBy || '—' }}<div>{{ record.approvedAt || '—' }}</div></template>
        <template v-else-if="column.key === 'action'">
          <a-button v-if="canApprove" type="link" :disabled="saving || !record.username || !record.sourceToken || record.status === 'UNAVAILABLE'" @click="beginChange(record, 'APPROVE')">{{ record.status === 'APPROVED' ? '重新确认' : '批准' }}</a-button>
          <a-button v-if="canRevoke" type="link" danger :disabled="saving || !record.revision || !record.sourceToken || record.status === 'REVOKED'" @click="beginChange(record, 'REVOKE')">撤销</a-button>
          <a-button type="link" @click="showHistory(record)">历史</a-button>
        </template>
      </template>
    </a-table>
    <a-modal v-model:open="editing" :title="operation === 'APPROVE' ? '确认并批准当前来源' : '撤销来源批准'" :confirm-loading="saving" :closable="!saving" :mask-closable="false" :keyboard="!saving" :cancel-button-props="{ disabled: saving }" ok-text="确认提交" cancel-text="取消" @ok="submit">
      <template v-if="target">
        <p>{{ target.conceptName }} · {{ target.datasourceName || target.datasourceId }}</p>
        <p>{{ target.databaseName }} / {{ target.schemaName || '—' }} / {{ target.tableName }} · 当前账号：{{ target.username || '来源不可用' }}</p>
        <a-alert v-if="operation === 'APPROVE'" type="warning" show-icon message="本操作不验证数据库权限，也不执行 SQL" description="请先由数据库管理员对该数据源的当前账号落实所需的最小只读权限和数据隔离。来源或连接配置（含密码）变化后需重新批准。" />
        <a-checkbox v-if="operation === 'APPROVE'" v-model:checked="readonlyConfirmed" :disabled="saving">我确认该账号已由数据库管理员配置为所需的只读权限</a-checkbox>
        <a-form layout="vertical"><a-form-item label="操作说明（必填，最多500字）"><a-textarea v-model:value="reason" :maxlength="500" :rows="3" :disabled="saving" /></a-form-item></a-form>
        <a-alert v-if="editError" type="error" show-icon :message="editError" />
      </template>
    </a-modal>
    <a-modal v-model:open="historyOpen" title="来源批准 / 撤销历史" width="1000px" :footer="null" destroy-on-close>
      <a-alert v-if="historyError" type="error" show-icon :message="historyError" />
      <a-table :columns="historyColumns" :data-source="historyRows" :loading="historyLoading" row-key="id" :pagination="{ current: historyPage, pageSize: 10, total: historyTotal }" :scroll="{ x: 1000 }" @change="changeHistoryPage">
        <template #bodyCell="{ column, record }"><template v-if="column.key === 'action'">{{ record.action === 'APPROVE' ? '批准' : '撤销' }}</template></template>
      </a-table>
    </a-modal>
  </section>
</template>
<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import useUserStore from '@/store/system/user'
import { checkPermi } from '@/utils/permission'
import { listMetricSourceApprovals, getMetricSourceSettings, listMetricSourceHistory, approveMetricSource, revokeMetricSource } from '@/api/ont/metricSourceApproval'
const props = defineProps({ ontologyId: { type: [String, Number], required: true }, active: { type: Boolean, default: true } })
const user = useUserStore()
const canApprove = computed(() => checkPermi(['ont:metric-source:approve']))
const canRevoke = computed(() => checkPermi(['ont:metric-source:revoke']))
const rows = ref([]), total = ref(0), pageNum = ref(1), pageSize = ref(10), loading = ref(false), saving = ref(false), error = ref(''), settings = ref(null)
const editing = ref(false), target = ref(null), operation = ref('APPROVE'), reason = ref(''), readonlyConfirmed = ref(false), editError = ref('')
const historyOpen = ref(false), historyRows = ref([]), historyPage = ref(1), historyTotal = ref(0), historyLoading = ref(false), historyError = ref(''), historyTarget = ref(null)
let generation = 0, listSequence = 0, historySequence = 0
const labels = { UNAPPROVED: '未批准', APPROVED: '已批准', REVOKED: '已撤销', STALE: '来源已变化', UNAVAILABLE: '来源不可用' }
const colors = { APPROVED: 'green', STALE: 'orange', REVOKED: 'red', UNAVAILABLE: 'red' }
const columns = [{ title: '概念', dataIndex: 'conceptName', width: 140 }, { title: '数据源', dataIndex: 'datasourceName', width: 150 }, { title: '类型', dataIndex: 'datasourceType', width: 100 }, { title: '库 / Schema / 表', key: 'source', width: 260 }, { title: '当前账号（只读）', dataIndex: 'username', width: 160 }, { title: '状态', key: 'status', width: 300 }, { title: '最近批准人 / 时间', key: 'approved', width: 190 }, { title: '操作', key: 'action', width: 220 }]
const historyColumns = [{ title: '操作', key: 'action', width: 80 }, { title: '版本', dataIndex: 'revision', width: 70 }, { title: '操作者', dataIndex: 'actorId', width: 100 }, { title: '时间', dataIndex: 'occurredAt', width: 190 }, { title: '账号', dataIndex: 'username', width: 130 }, { title: '表', dataIndex: 'tableName', width: 150 }, { title: '说明', dataIndex: 'reason', width: 280 }]
const scope = () => ({ ontologyId: props.ontologyId, spaceId: user.spaceId, spaceCode: user.spaceCode })
async function load() {
  if (!props.active || !props.ontologyId) return
  const seq = ++listSequence, context = generation
  loading.value = true; error.value = ''
  try {
    const [page, protection] = await Promise.all([listMetricSourceApprovals({ ...scope(), pageNum: pageNum.value, pageSize: pageSize.value }), getMetricSourceSettings(scope())])
    if (seq !== listSequence || context !== generation) return
    rows.value = page.data?.rows || []; total.value = page.data?.total || 0; settings.value = protection.data
  } catch (failure) { if (seq === listSequence && context === generation) { rows.value = []; total.value = 0; settings.value = null; error.value = failure?.message || '来源配置读取失败' } }
  finally { if (seq === listSequence && context === generation) loading.value = false }
}
function reload() { if (!saving.value) load() }
function changePage(pagination) { pageNum.value = pagination.current; pageSize.value = pagination.pageSize; load() }
function beginChange(record, action) {
  if (saving.value || !props.active) return
  target.value = { ...record }; operation.value = action; reason.value = ''; readonlyConfirmed.value = false; editError.value = ''; editing.value = true
}
async function submit() {
  if (!target.value || saving.value || !props.active) return
  if (!reason.value.trim()) { editError.value = '请填写操作说明'; return }
  if (operation.value === 'APPROVE' && !readonlyConfirmed.value) { editError.value = '请先确认数据库实际只读授权'; return }
  const context = generation, action = operation.value
  const data = { ...scope(), bindingId: target.value.bindingId, revision: target.value.revision, sourceToken: target.value.sourceToken, reason: reason.value.trim(), readonlyConfirmed: readonlyConfirmed.value }
  saving.value = true; editError.value = ''
  try {
    await (action === 'APPROVE' ? approveMetricSource(data) : revokeMetricSource(data))
    if (context !== generation) return
    editing.value = false; message.success(action === 'APPROVE' ? '当前来源已批准' : '来源批准已撤销'); await load()
  } catch (failure) { if (context === generation) editError.value = failure?.message || '操作失败，请刷新来源和版本后重试' }
  finally { if (context === generation) saving.value = false }
}
function showHistory(record) { historyTarget.value = record.bindingId; historyPage.value = 1; historyOpen.value = true; loadHistory() }
async function loadHistory() {
  const seq = ++historySequence, context = generation
  historyLoading.value = true; historyError.value = ''
  try {
    const response = await listMetricSourceHistory({ ...scope(), bindingId: historyTarget.value, pageNum: historyPage.value, pageSize: 10 })
    if (seq !== historySequence || context !== generation || !historyOpen.value) return
    historyRows.value = response.data?.rows || []; historyTotal.value = response.data?.total || 0
  } catch (failure) { if (seq === historySequence && context === generation) { historyRows.value = []; historyTotal.value = 0; historyError.value = failure?.message || '历史读取失败' } }
  finally { if (seq === historySequence && context === generation) historyLoading.value = false }
}
function changeHistoryPage(pagination) { historyPage.value = pagination.current; loadHistory() }
function reset() {
  ++generation; ++listSequence; ++historySequence
  rows.value = []; total.value = 0; settings.value = null; error.value = ''; loading.value = false; saving.value = false
  editing.value = false; target.value = null; reason.value = ''; readonlyConfirmed.value = false; editError.value = ''
  historyOpen.value = false; historyRows.value = []; historyTarget.value = null; historyLoading.value = false; historyError.value = ''; historyTotal.value = 0
}
watch(() => [props.ontologyId, props.active, user.spaceId, user.spaceCode], () => { reset(); pageNum.value = 1; load() }, { immediate: true })
watch(historyOpen, value => { if (!value) { ++historySequence; historyRows.value = []; historyLoading.value = false } })
onBeforeUnmount(reset)
defineExpose({ reload })
</script>
<style scoped>
.source-config { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.notice { margin-top: 6px; font-size: 12px; }
.ant-checkbox-wrapper { margin: 16px 0; }
</style>
