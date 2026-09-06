<template>
  <div class="app-container">
    <dm-wrap :columns="tableStore.columns" :tableRef="tableRef">
      <template #search>
        <dm-search-bar v-bind="searchStore" :params="tableStore.params" :tableRef="tableRef" :visible-count="3" />
      </template>
      <template #actions-data>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['ont:webhook:add']">新增Webhook</a-button>
      </template>
      <dm-table v-bind="tableStore" ref="tableRef">
        <template #ontology="{ row }">
          {{ ontologyName(row.ontologyId) }}
        </template>
        <template #actionName="{ row }">
          {{ actionName(row.actionId) }}
        </template>
        <template #method="{ row }">
          <a-tag :color="row.method === 'PUT' ? 'orange' : 'blue'">{{ row.method || 'POST' }}</a-tag>
        </template>
        <template #enabled="{ row }">
          <a-tag :color="row.enabled ? 'green' : 'default'">{{ row.enabled ? '启用' : '停用' }}</a-tag>
        </template>
        <template #action="{ row }">
          <a-button type="link" size="small" @click="handleLogs(row)" v-hasPermi="['ont:webhook:list']">日志</a-button>
          <a-button type="link" size="small" @click="handleUpdate(row)" v-hasPermi="['ont:webhook:edit']">修改</a-button>
          <a-button type="link" danger size="small" @click="handleDelete(row)" v-hasPermi="['ont:webhook:remove']">删除</a-button>
        </template>
      </dm-table>
    </dm-wrap>

    <a-divider>回调日志<a-button v-if="selectedWebhookId" type="link" danger size="small" style="margin-left:8px" @click="handleClearLogs" v-hasPermi="['ont:webhook:remove']">清除日志</a-button></a-divider>
    <a-table :columns="logColumns" :data-source="logData" :loading="logLoading" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="logStatusColor(record.status)">{{ logStatusText(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'executionType'">
          <a-tag color="cyan">{{ record.executionType }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" size="small" @click="handleViewLog(record)">查看详情</a-button>
        </template>
      </template>
    </a-table>

    <a-modal :title="title" v-model:open="open" width="760px" destroy-on-close ok-text="确定" cancel-text="取消" @ok="submitForm" @cancel="cancel">
      <a-form :label-col="{ style: { width: '110px' } }">
        <a-form-item label="Webhook 名称"><a-input v-model:value="form.name" placeholder="如 客户源系统回调" /></a-form-item>
        <a-form-item label="所属本体">
          <a-select v-model:value="form.ontologyId" placeholder="请选择本体" @change="onChangeOntology" style="width:100%">
            <a-select-option v-for="o in ontologyOptions" :key="o.value" :value="o.value" :label="o.label">{{ o.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="绑定动作">
          <a-select v-model:value="form.actionId" placeholder="可选：绑定单个动作执行；为空绑定本体级" allow-clear style="width:100%">
            <a-select-option v-for="a in actionOptions" :key="a.value" :value="a.value" :label="a.label">{{ a.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="回调地址"><a-input v-model:value="form.url" placeholder="http://source-system/api/callback" /></a-form-item>
        <a-form-item label="请求方法">
          <a-select v-model:value="form.method" placeholder="默认 POST" style="width:100%">
            <a-select-option value="POST">POST</a-select-option>
            <a-select-option value="PUT">PUT</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="请求头"><a-textarea v-model:value="form.headers" :rows="2" placeholder='JSON，如 {"Authorization":"Bearer xxx"}' /></a-form-item>
        <a-form-item label="请求体模板"><a-textarea v-model:value="form.payloadTemplate" :rows="3" placeholder="JSON 模板，空时按执行记录生成默认结构" /></a-form-item>
        <a-form-item label="回调密钥"><a-input v-model:value="form.secret" placeholder="可选，回调签名密钥" /></a-form-item>
        <a-form-item label="是否启用">
          <a-switch v-model:checked="form.enabled" />
        </a-form-item>
        <a-form-item label="最大重试">
          <a-input-number v-model:value="form.maxRetry" :min="0" style="width:100%" /><span style="color:#999;font-size:12px;"> 0 表示不重试</span>
        </a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal title="回调详情" v-model:open="logOpen" width="760px" :footer="null">
      <div style="margin-bottom:8px;"><a-tag :color="logStatusColor(currentLog.status)">{{ logStatusText(currentLog.status) }}</a-tag>
        <a-tag v-if="currentLog.httpStatus" style="margin-left:8px">{{ currentLog.httpStatus }}</a-tag>
        <a-tag v-if="currentLog.retryCount != null" style="margin-left:8px" color="default">重试 {{ currentLog.retryCount }} 次</a-tag>
      </div>
      <div v-if="currentLog.payload" style="margin-top:8px;"><b>请求体</b>
        <pre style="background:#f5f5f5;padding:8px;border-radius:4px;font-size:12px;max-height:220px;overflow:auto;white-space:pre-wrap;word-break:break-all;">{{ currentLog.payload }}</pre>
      </div>
      <div v-if="currentLog.responseBody" style="margin-top:8px;"><b>响应体</b>
        <pre style="background:#e6f4ff;padding:8px;border-radius:4px;font-size:12px;max-height:220px;overflow:auto;white-space:pre-wrap;word-break:break-all;">{{ currentLog.responseBody }}</pre>
      </div>
      <div v-if="currentLog.errorMessage" style="margin-top:8px;"><b>错误信息</b>
        <pre style="background:#fff1f0;padding:8px;border-radius:4px;font-size:12px;max-height:140px;overflow:auto;white-space:pre-wrap;word-break:break-all;">{{ currentLog.errorMessage }}</pre>
      </div>
      <div v-if="!currentLog.payload && !currentLog.responseBody && !currentLog.errorMessage" style="padding:24px;color:#999;">暂无详情数据</div>
    </a-modal>
  </div>
</template>
<script setup name="OntWebhook">
import { listWebhook, getWebhook, addWebhook, updateWebhook, delWebhook, listWebhookLog, clearWebhookLogs } from '@/api/ont/webhook'
import { listOntology } from '@/api/ont/ontology'
import { getActionsByOntology } from '@/api/ont/action'

const { proxy } = getCurrentInstance()
const tableRef = ref(null)
const open = ref(false)
const logOpen = ref(false)
const title = ref('')
const logLoading = ref(false)
const selectedWebhookId = ref(null)
const currentLog = ref({})

const ontologyMap = new Map()
const actionMap = new Map()
const ontologyOptions = ref([])
const actionOptions = ref([])

const tableStore = reactive({
  config: { stripe: true },
  columns: [
    { label: '名称', prop: 'name', align: 'left', width: 160 },
    { label: '所属本体', prop: 'ontologyId', align: 'left', width: 130, slot: 'ontology' },
    { label: '绑定动作', prop: 'actionId', align: 'left', width: 130, slot: 'actionName' },
    { label: '回调地址', prop: 'url', align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '方法', prop: 'method', align: 'center', width: 80, slot: 'method' },
    { label: '状态', prop: 'enabled', align: 'center', width: 80, slot: 'enabled' },
    { label: '最大重试', prop: 'maxRetry', align: 'center', width: 90 },
    { label: '创建时间', prop: 'createTime', align: 'center', width: 170, date: true },
    { label: '操作', width: 200, slot: 'action', align: 'center' }
  ],
  func: listWebhook,
  params: {},
  events: {}
})

const searchStore = reactive({
  items: [
    { label: '名称', prop: 'name', component: { is: 'input' } },
    { label: '是否启用', prop: 'enabled', component: { is: 'select', options: [{ value: 'true', label: '启用' }, { value: 'false', label: '停用' }] } }
  ],
  config: { permi: ['ont:webhook:query'] }
})

function ontologyName(id) { return (id && ontologyMap.get(id)) || ('本体#' + id) }
function actionName(id) { return (id && actionMap.get(id)) || (id ? '动作#' + id : '-') }

function loadOntologies() {
  listOntology({ pageNo: 1, pageSize: 1000 }).then(res => {
    const rows = res.data.rows || []
    ontologyOptions.value = rows.map(o => ({ value: o.id, label: o.name }))
    rows.forEach(o => ontologyMap.set(o.id, o.name))
    ontologyOptions.value.forEach(o => getActionsByOntology(o.value).then(r => {
      const acts = r.data || []
      acts.forEach(a => actionMap.set(a.id, a.name))
    }).catch(() => {}))
  })
}

function onChangeOntology(opts) {
  actionOptions.value = []
  if (!opts || !opts.keep) { form.value.actionId = null }
  const oid = form.value.ontologyId
  if (!oid) return
  getActionsByOntology(oid).then(res => {
    const acts = res.data || []
    actionOptions.value = acts.map(a => ({ value: a.id, label: a.name }))
    acts.forEach(a => actionMap.set(a.id, a.name))
  })
}

const data = reactive({ form: {} })
const { form } = toRefs(data)

const logData = ref([])
const logColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center' },
  { title: '执行类型', dataIndex: 'executionType', key: 'executionType', width: 100, align: 'center' },
  { title: '描述', dataIndex: 'webhookName', key: 'webhookName', width: 180, ellipsis: true },
  { title: 'HTTP状态', dataIndex: 'httpStatus', key: 'httpStatus', width: 90, align: 'center' },
  { title: '已重试', dataIndex: 'retryCount', key: 'retryCount', width: 80, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, align: 'center' }
]

function logStatusText(s) { return { SUCCESS: '成功', FAILED: '失败', PENDING: '待发送' }[s] || s }
function logStatusColor(s) { return { SUCCESS: 'green', FAILED: 'red', PENDING: 'orange' }[s] || 'default' }

function loadLogs() {
  logLoading.value = true
  const query = { pageNo: 1, pageSize: 20 }
  if (selectedWebhookId.value) { query.webhookId = selectedWebhookId.value }
  listWebhookLog(query).then(res => {
    logData.value = res.data.rows || []
  }).finally(() => { logLoading.value = false })
}

function reset() { form.value = {} }
function handleAdd() { reset(); form.value = { name: null, ontologyId: null, actionId: null, url: null, method: 'POST', headers: null, payloadTemplate: null, secret: null, enabled: true, maxRetry: 0, description: null }; actionOptions.value = []; open.value = true; title.value = '新增Webhook' }
function handleUpdate(row) { reset(); getWebhook(row.id).then(res => { const keep = res.data.actionId; form.value = res.data; onChangeOntology({ keep }); open.value = true; title.value = '修改Webhook' }) }
function submitForm() {
  if (!form.value.name) { proxy.$modal.msgError('Webhook 名称不能为空'); return }
  if (!form.value.url) { proxy.$modal.msgError('回调地址不能为空'); return }
  const fn = form.value.id ? updateWebhook : addWebhook
  fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; tableRef.value.getList() })
}
function handleDelete(row) {
  proxy.$modal.confirm('确认删除Webhook"' + row.name + '"？').then(() => delWebhook(row.id)).then(() => { tableRef.value.getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
function handleLogs(row) { selectedWebhookId.value = row.id; loadLogs() }
function handleClearLogs() {
  proxy.$modal.confirm('确认清除当前回调日志？').then(() => clearWebhookLogs(selectedWebhookId.value)).then(() => { proxy.$modal.msgSuccess('清除成功'); loadLogs() }).catch(() => {})
}
function handleViewLog(record) { currentLog.value = record; logOpen.value = true }
function cancel() { open.value = false; reset() }
loadOntologies()
loadLogs()
</script>