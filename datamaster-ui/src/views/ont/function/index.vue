<template>
  <div class="app-container">
    <dm-wrap :columns="tableStore.columns" :tableRef="tableRef">
      <template #search>
        <dm-search-bar v-bind="searchStore" :params="tableStore.params" :tableRef="tableRef" :visible-count="3" />
      </template>
      <template #actions-data>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['ont:function:add']">新增函数</a-button>
      </template>
      <dm-table v-bind="tableStore" ref="tableRef">
        <template #lang="{ row }">
          <a-tag :color="row.lang === 'PYTHON' ? 'orange' : 'blue'">{{ row.lang }}</a-tag>
        </template>
        <template #needsApproval="{ row }">
          <a-tag :color="row.needsApproval ? 'red' : 'green'">{{ row.needsApproval ? '是' : '否' }}</a-tag>
        </template>
        <template #action="{ row }">
          <a-button type="link" size="small" @click="handleEditCode(row)" v-hasPermi="['ont:function:edit']">编辑代码</a-button>
          <a-button type="link" size="small" @click="handleSubmitExec(row)" v-hasPermi="['ont:function:edit']">执行</a-button>
          <a-button type="link" size="small" @click="handleUpdate(row)" v-hasPermi="['ont:function:edit']">修改</a-button>
          <a-button type="link" danger size="small" @click="handleDelete(row)" v-hasPermi="['ont:function:remove']">删除</a-button>
        </template>
      </dm-table>
    </dm-wrap>

    <a-divider>执行记录</a-divider>
    <a-table :columns="execColumns" :data-source="execData" :loading="execLoading" row-key="id" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="execStatusColor(record.status)">{{ execStatusText(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'duration'">
          <span v-if="record.durationMs">{{ record.durationMs }}ms</span>
        </template>
        <template v-if="column.key === 'action'">
          <a-button v-if="record.status === 'PENDING_APPROVAL'" type="link" size="small" style="color:#52c41a" @click="handleApprove(record)">通过</a-button>
          <a-button v-if="record.status === 'PENDING_APPROVAL'" type="link" danger size="small" @click="handleReject(record)">拒绝</a-button>
          <a-button v-if="record.status === 'APPROVED'" type="link" size="small" style="color:#1677ff" @click="handleRun(record)">执行</a-button>
          <a-button type="link" size="small" @click="handleViewResult(record)">查看结果</a-button>
        </template>
      </template>
    </a-table>

    <a-modal :title="title" v-model:open="open" width="700px" destroy-on-close ok-text="OK" cancel-text="Cancel" @ok="submitForm" @cancel="cancel">
      <a-form :label-col="{ style: { width: '100px' } }">
        <a-form-item label="本体ID"><a-input-number v-model:value="form.ontologyId" :min="1" style="width:100%" /></a-form-item>
        <a-form-item label="函数名称"><a-input v-model:value="form.name" placeholder="函数名称" /></a-form-item>
        <a-form-item label="语言">
          <a-select v-model:value="form.lang" placeholder="请选择语言">
            <a-select-option value="TYPESCRIPT">TypeScript</a-select-option>
            <a-select-option value="PYTHON">Python</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="需要审批"><a-switch v-model:checked="form.needsApproval" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal title="编辑代码" v-model:open="codeOpen" width="900px" destroy-on-close ok-text="Save" cancel-text="Cancel" @ok="saveCode" @cancel="codeOpen = false">
      <div style="margin-bottom:8px;"><a-tag :color="editFunc.lang === 'PYTHON' ? 'orange' : 'blue'">{{ editFunc.lang }}</a-tag> {{ editFunc.name }}</div>
      <textarea v-model="editFunc.body" style="width:100%;height:400px;font-family:monospace;font-size:14px;padding:12px;border:1px solid #d9d9d9;border-radius:6px;resize:vertical;background:#fafafa;" spellcheck="false" placeholder="// write your code here"></textarea>
    </a-modal>

    <a-modal title="执行结果" v-model:open="resultOpen" width="700px" :footer="null">
      <pre style="background:#f6ffed;padding:12px;border-radius:6px;font-family:monospace;font-size:12px;max-height:400px;overflow:auto;white-space:pre-wrap;word-break:break-all;">{{ currentResult.outputResult || currentResult.errorMessage || 'No output' }}</pre>
    </a-modal>
  </div>
</template>
<script setup name="OntFunction">
import { listFunction, getFunction, addFunction, updateFunction, delFunction, submitFuncExecution, approveFuncExecution, rejectFuncExecution, runFuncExecution, listFuncExecution } from '@/api/ont/function'
import { genCode } from '@/utils/codeGen'

const { proxy } = getCurrentInstance()
const tableRef = ref(null)
const open = ref(false)
const codeOpen = ref(false)
const resultOpen = ref(false)
const title = ref('')
const execLoading = ref(false)
const currentResult = ref({})
const editFunc = ref({ body: '' })

const tableStore = reactive({
  config: { stripe: true },
  columns: [
    { label: '函数名称', prop: 'name', align: 'left', width: 160 },
    { label: '语言', prop: 'lang', align: 'center', width: 100, slot: 'lang' },
    { label: '需要审批', prop: 'needsApproval', align: 'center', width: 100, slot: 'needsApproval' },
    { label: '描述', prop: 'description', align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '创建时间', prop: 'createTime', align: 'center', width: 170, date: true },
    { label: '操作', width: 300, slot: 'action', align: 'center' }
  ],
  func: listFunction,
  params: {},
  events: {}
})

const searchStore = reactive({
  items: [
    { label: '函数名称', prop: 'name', component: { is: 'input' } },
    { label: '语言', prop: 'lang', component: { is: 'select', options: [{ value: 'TYPESCRIPT', label: 'TypeScript' }, { value: 'PYTHON', label: 'Python' }] } }
  ],
  config: { permi: ['ont:function:query'] }
})

const data = reactive({ form: {} })
const { form } = toRefs(data)

const execData = ref([])
const execColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '输出结果', dataIndex: 'outputResult', key: 'outputResult', width: 300, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, align: 'center' },
  { title: '耗时', key: 'duration', width: 100 },
  { title: '审批意见', dataIndex: 'approvalReason', key: 'approvalReason', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 200, align: 'center' }
]

function execStatusText(s) { return { DRAFT: '草稿', PENDING_APPROVAL: '待审批', APPROVED: '已批准', REJECTED: '已拒绝', EXECUTED: '已执行', FAILED: '失败' }[s] || s }
function execStatusColor(s) { return { DRAFT: 'default', PENDING_APPROVAL: 'orange', APPROVED: 'green', REJECTED: 'red', EXECUTED: 'blue', FAILED: 'red' }[s] || 'default' }

function loadExecutions() {
  execLoading.value = true
  listFuncExecution({ pageNo: 1, pageSize: 20 }).then(res => {
    execData.value = res.data.rows || []
  }).finally(() => { execLoading.value = false })
}

function reset() { form.value = {} }
function handleAdd() { reset(); form.value = { ontologyId: undefined, name: undefined, code: undefined, lang: 'TYPESCRIPT', needsApproval: true, description: undefined }; open.value = true; title.value = '新增函数' }
function handleUpdate(row) { reset(); getFunction(row.id).then(res => { form.value = res.data; open.value = true; title.value = '修改函数' }) }
function submitForm() {
  if (!form.value.name) { proxy.$modal.msgError('函数名称不能为空'); return }
  if (!form.value.code) { form.value.code = genCode('fn') }
  if (!form.value.lang) { proxy.$modal.msgError('语言不能为空'); return }
  if (!form.value.ontologyId) { proxy.$modal.msgError('本体ID不能为空'); return }
  const fn = form.value.id ? updateFunction : addFunction
  fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; tableRef.value.getList() })
}
function handleDelete(row) {
  proxy.$modal.confirm('确认删除函数"' + row.name + '"？').then(() => delFunction(row.id)).then(() => { tableRef.value.getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
function handleEditCode(row) { editFunc.value = { ...row }; codeOpen.value = true }
function saveCode() {
  updateFunction({ id: editFunc.value.id, ontologyId: editFunc.value.ontologyId, name: editFunc.value.name, code: editFunc.value.code, lang: editFunc.value.lang, body: editFunc.value.body, needsApproval: editFunc.value.needsApproval, description: editFunc.value.description }).then(() => {
    proxy.$modal.msgSuccess('代码已保存'); codeOpen.value = false; tableRef.value.getList()
  })
}
function handleSubmitExec(row) {
  if (row.needsApproval) {
    submitFuncExecution({ functionId: row.id, inputParams: '{}' }).then(() => {
      proxy.$modal.msgSuccess('已提交审批'); loadExecutions()
    })
  } else {
    proxy.$modal.confirm('确认直接执行"' + row.name + '"？').then(() => {
      submitFuncExecution({ functionId: row.id, inputParams: '{}' }).then(res => {
        return runFuncExecution(res.data.id)
      }).then(() => { proxy.$modal.msgSuccess('执行完成'); loadExecutions() })
    }).catch(() => {})
  }
}
function handleApprove(record) {
  proxy.$modal.confirm('确认审批通过？').then(() => approveFuncExecution({ executionId: record.id, approvalReason: '同意' })).then(() => { proxy.$modal.msgSuccess('审批通过'); loadExecutions() }).catch(() => {})
}
function handleReject(record) {
  proxy.$modal.confirm('确认拒绝？').then(() => rejectFuncExecution({ executionId: record.id, approvalReason: '拒绝' })).then(() => { proxy.$modal.msgSuccess('已拒绝'); loadExecutions() }).catch(() => {})
}
function handleRun(record) {
  proxy.$modal.confirm('确认执行？').then(() => runFuncExecution(record.id)).then(() => { proxy.$modal.msgSuccess('执行完成'); loadExecutions() }).catch(() => {})
}
function handleViewResult(record) { currentResult.value = record; resultOpen.value = true }
function cancel() { open.value = false; reset() }
loadExecutions()
</script>
