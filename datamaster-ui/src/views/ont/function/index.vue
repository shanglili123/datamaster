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
        <template #params="{ row }">
          {{ parseParams(row.params).join(', ') || '-' }}
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
        <a-form-item label="函数名称"><a-input v-model:value="form.name" placeholder="函数名称" /></a-form-item>
        <a-form-item label="语言">
          <a-select v-model:value="form.lang" placeholder="请选择语言">
            <a-select-option value="TYPESCRIPT">TypeScript</a-select-option>
            <a-select-option value="PYTHON">Python</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="函数代码">
          <Codemirror v-model:value="form.body" :options="codeEditorOptions" height="260px" />
        </a-form-item>
        <a-form-item label="参数">
          <div style="width:100%">
            <div v-for="(p, idx) in paramNames" :key="idx" style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
              <a-input v-model:value="paramNames[idx]" placeholder="参数名，如 name" style="width:260px" size="small" />
              <a-button type="link" danger size="small" @click="paramNames.splice(idx, 1)">删除</a-button>
            </div>
            <a-button type="dashed" size="small" @click="paramNames.push('')">+ 添加参数</a-button>
            <div style="font-size:12px;color:#999;margin-top:4px;">声明函数输入参数，代码内用 $${'{name}'} 引用或通过 input 对象获取（如 input.name）</div>
          </div>
        </a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal title="编辑代码" v-model:open="codeOpen" width="900px" destroy-on-close ok-text="Save" cancel-text="Cancel" @ok="saveCode" @cancel="codeOpen = false">
      <div style="margin-bottom:8px;"><a-tag :color="editFunc.lang === 'PYTHON' ? 'orange' : 'blue'">{{ editFunc.lang }}</a-tag> {{ editFunc.name }}</div>
      <div style="margin-bottom:8px;padding:8px 12px;background:#fff7e6;border:1px solid #ffd591;border-radius:6px;font-size:12px;color:#d46b08;">可用 ${参数名} 引用入参，也可用 input 对象获取全部入参，如 $${'{name}'}；动作绑定数据来源概念后 input.source.rows 为主概念数据、input.relations 为关联关系数据，脚本输出 JSON 数组（键=输出属性 code）可写回输出目标概念。<b>脚本必须输出结果</b>（TS 用 console.log、Python 用 print 输出 JSON 数组或对象，输出将被记录/落库）。</div>
      <div style="margin-bottom:8px;display:flex;gap:8px;align-items:center;">
        <span style="font-size:13px;color:#666;">示例代码：</span>
        <a-button size="small" @click="insertExample('TYPESCRIPT')">插入 TypeScript 示例</a-button>
        <a-button size="small" @click="insertExample('PYTHON')">插入 Python 示例</a-button>
      </div>
      <Codemirror v-model:value="editFunc.body" :options="editCodeEditorOptions" height="400px" />
    </a-modal>

    <a-modal title="执行结果" v-model:open="resultOpen" width="700px" :footer="null">
      <pre style="background:#f6ffed;padding:12px;border-radius:6px;font-family:monospace;font-size:12px;max-height:400px;overflow:auto;white-space:pre-wrap;word-break:break-all;">{{ currentResult.outputResult || currentResult.errorMessage || 'No output' }}</pre>
    </a-modal>
  </div>
</template>
<script setup name="OntFunction">
import { listFunction, getFunction, addFunction, updateFunction, delFunction, submitFuncExecution, approveFuncExecution, rejectFuncExecution, runFuncExecution, listFuncExecution } from '@/api/ont/function'
import { genCode } from '@/utils/codeGen'
import Codemirror from 'codemirror-editor-vue3'
import 'codemirror/lib/codemirror.css'
import 'codemirror/theme/idea.css'
import 'codemirror/mode/javascript/javascript.js'
import 'codemirror/mode/python/python.js'
import 'codemirror/addon/display/placeholder.js'

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
    { label: '参数', prop: 'params', align: 'left', width: 160, slot: 'params' },
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

const paramNames = ref([])
const editParamNames = ref([])

const codeEditorOptions = computed(() => ({
  mode: form.value.lang === 'PYTHON' ? 'text/x-python' : 'text/typescript',
  theme: 'default',
  lineNumbers: true,
  lineWrapping: true,
  tabSize: 4,
  readOnly: false,
  placeholder: '// 在此编写函数代码，可用 ${参数名} 引用入参'
}))
const editCodeEditorOptions = computed(() => ({
  mode: editFunc.value.lang === 'PYTHON' ? 'text/x-python' : 'text/typescript',
  theme: 'default',
  lineNumbers: true,
  lineWrapping: true,
  tabSize: 4,
  readOnly: false,
  placeholder: '// 在此编写函数代码，可用 ${参数名} 引用入参'
}))

function parseParams(s) {
  if (!s) return []
  try {
    const arr = JSON.parse(s)
    return Array.isArray(arr) ? arr.filter(n => typeof n === 'string') : []
  } catch {
    return []
  }
}

const TS_EXAMPLE = `// 示例：声明参数 name、amount 后，可用 \${name}、\${amount} 直接引用，
// 也可用 input.name / input.amount 访问 input 对象。
// 绑定数据来源概念后，input.source.rows 为来源数据、input.source.relations 为关联数据。
// 【必须输出】用 console.log 输出结果（JSON 数组或对象），输出会被记录并可写回落库。
function greet(user) {
  return { name: user.name, amount: Number(user.amount) * 2 }
}
const rows = (input.source && input.source.rows) || [{ name: \${name}, amount: \${amount} }]
const result = rows.map(r => greet(r))
console.log(JSON.stringify(result))
`

const PY_EXAMPLE = `# 示例：声明参数 name、amount 后，可用 \${name}、\${amount} 直接引用，
# 也可用 input['name'] / input['amount'] 访问 input 对象。
# 绑定数据来源概念后，input['source']['rows'] 为来源数据、input['source']['relations'] 为关联数据。
# 【必须输出】用 print 输出 JSON（数组或对象），输出会被记录并可写回落库。
import json

def greet(user):
    return {'name': user['name'], 'amount': user['amount'] * 2}

rows = input.get('source', {}).get('rows') or [{'name': \${name}, 'amount': \${amount}}]
result = [greet(r) for r in rows]
print(json.dumps(result, ensure_ascii=False))
`

function exampleBody(lang) {
  return lang === 'PYTHON' ? PY_EXAMPLE : TS_EXAMPLE
}

function insertExample(lang) {
  editFunc.value.body = exampleBody(lang)
}

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
function handleAdd() { reset(); paramNames.value = []; form.value = { name: undefined, code: undefined, lang: 'TYPESCRIPT', body: exampleBody('TYPESCRIPT'), description: undefined, params: '[]' }; open.value = true; title.value = '新增函数' }
function handleUpdate(row) { reset(); getFunction(row.id).then(res => { form.value = res.data; paramNames.value = parseParams(res.data.params); open.value = true; title.value = '修改函数' }) }
function submitForm() {
  if (!form.value.name) { proxy.$modal.msgError('函数名称不能为空'); return }
  if (!form.value.code) { form.value.code = genCode('fn') }
  if (!form.value.lang) { proxy.$modal.msgError('语言不能为空'); return }
  if (!form.value.body || !form.value.body.trim()) { proxy.$modal.msgError('函数代码不能为空'); return }
  const rawParams = paramNames.value.map(n => (n || '').trim()).filter(Boolean)
  if (new Set(rawParams).size !== rawParams.length) { proxy.$modal.msgError('参数名不能重复'); return }
  form.value.params = JSON.stringify(rawParams)
  const fn = form.value.id ? updateFunction : addFunction
  fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; tableRef.value.getList() })
}
function handleDelete(row) {
  proxy.$modal.confirm('确认删除函数"' + row.name + '"？').then(() => delFunction(row.id)).then(() => { tableRef.value.getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
function handleEditCode(row) { editFunc.value = { ...row }; editParamNames.value = parseParams(row.params); codeOpen.value = true }
function saveCode() {
  if (!editFunc.value.body || !editFunc.value.body.trim()) { proxy.$modal.msgError('函数代码不能为空'); return }
  const cleanParams = editParamNames.value.map(n => (n || '').trim()).filter(Boolean)
  updateFunction({
    id: editFunc.value.id,
    name: editFunc.value.name,
    code: editFunc.value.code,
    lang: editFunc.value.lang,
    body: editFunc.value.body,
    description: editFunc.value.description,
    params: JSON.stringify(cleanParams)
  }).then(() => {
    proxy.$modal.msgSuccess('代码已保存')
    codeOpen.value = false
    tableRef.value.getList()
  })
}
function doExecFunc(row, inputParams) {
  if (row.needsApproval) {
    submitFuncExecution({ functionId: row.id, inputParams }).then(() => {
      proxy.$modal.msgSuccess('已提交审批'); loadExecutions()
    })
  } else {
    proxy.$modal.confirm('确认直接执行"' + row.name + '"？').then(() => {
      submitFuncExecution({ functionId: row.id, inputParams }).then(res => {
        return runFuncExecution(res.data.id)
      }).then(() => { proxy.$modal.msgSuccess('执行完成'); loadExecutions() })
    }).catch(() => {})
  }
}
function handleSubmitExec(row) {
  // 共享函数执行：按声明的参数收集入参；数据来源由「动作绑定函数」时配置
  const params = parseParams(row.params)
  if (params.length) {
    const execParamValues = ref({})
    proxy.$modal.confirm({
      title: '请输入参数',
      content: h('div', { style: 'padding:8px 0;' }, params.map(name =>
        h('div', { style: 'display:flex;align-items:center;gap:8px;margin-bottom:8px;' }, [
          h('span', { style: 'min-width:80px;font-size:13px;' }, name),
          h('input', {
            style: 'flex:1;padding:4px 8px;border:1px solid #d9d9d9;border-radius:4px;',
            value: execParamValues.value[name] || '',
            onInput: (e) => { execParamValues.value[name] = e.target.value },
            placeholder: '请输入 ' + name
          })
        ])
      )),
      onOk() {
        const inputParams = JSON.stringify(execParamValues.value)
        doExecFunc(row, inputParams)
      }
    })
  } else {
    doExecFunc(row, '{}')
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
