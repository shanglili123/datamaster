<template>
  <div class="action-panel">
    <!-- 工具条：搜索 + 新增 -->
    <div class="panel-toolbar">
      <a-input
        v-model:value="queryParams.name"
        placeholder="请输入动作名称"
        allow-clear
        style="width: 200px"
        @keyup.enter="handleQuery"
      />
      <a-select
        v-model:value="queryParams.actionType"
        placeholder="动作类型"
        allow-clear
        style="width: 140px"
        :options="actionTypeOptions"
      />
      <a-button type="primary" @click="handleQuery">查询</a-button>
      <a-button @click="resetQuery">重置</a-button>
      <a-button type="primary" class="toolbar-right" @click="handleAdd" v-hasPermi="['ont:action:add']">
        <template #icon><PlusOutlined /></template>
        新增动作
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="actionList"
      :loading="loading"
      row-key="id"
      size="middle"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actionType'">
          <a-tag :color="actionTypeColor(record.actionType)">{{ actionTypeText(record.actionType) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'conceptName'">
          {{ conceptMap[record.conceptId] || record.conceptId || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openSubmitExec(record)" v-hasPermi="['ont:action:edit']">执行</a-button>
          <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['ont:action:edit']">修改</a-button>
          <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['ont:action:remove']">删除</a-button>
        </template>
      </template>
      <template #empty>
        <div class="emptyBg">
          <img src="@/assets/system/images/no_data/noData.png" alt="" />
          <p>暂无动作，点击右上角「新增动作」创建</p>
        </div>
      </template>
    </a-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <a-divider class="exec-divider">执行记录</a-divider>

    <a-table
      :columns="execColumns"
      :data-source="execList"
      :loading="execLoading"
      row-key="id"
      size="middle"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actionName'">
          {{ actionNameMap[record.actionId] || record.actionId || '-' }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="execStatusColor(record.status)">{{ execStatusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'generatedSql'">
          <span class="sql-cell">{{ record.generatedSql || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'execAction'">
          <a-button v-if="record.status === 'PENDING_APPROVAL'" type="link" size="small" style="color:#52c41a" @click="handleApprove(record)" v-hasPermi="['ont:action:edit']">通过</a-button>
          <a-button v-if="record.status === 'PENDING_APPROVAL'" type="link" danger size="small" @click="handleReject(record)" v-hasPermi="['ont:action:edit']">拒绝</a-button>
          <a-button v-if="record.status === 'APPROVED'" type="link" size="small" style="color:#1677ff" @click="handleRun(record)" v-hasPermi="['ont:action:edit']">执行</a-button>
          <a-button type="link" size="small" @click="handleViewResult(record)" v-hasPermi="['ont:action:query']">查看结果</a-button>
        </template>
      </template>
      <template #empty>
        <div class="emptyBg">
          <img src="@/assets/system/images/no_data/noData.png" alt="" />
          <p>暂无执行记录，点击上方「执行」发起</p>
        </div>
      </template>
    </a-table>

    <!-- 动作对话框 -->
    <a-modal :title="title" v-model:open="open" width="600px" destroy-on-close ok-text="OK" cancel-text="Cancel" @ok="submitForm" @cancel="cancel">
      <a-form ref="actionRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
        <a-form-item label="动作名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入动作名称，如 创建客户" />
        </a-form-item>
        <a-form-item label="动作类型" name="actionType">
          <a-select v-model:value="form.actionType" placeholder="请选择动作类型" :options="actionTypeOptions" />
        </a-form-item>
        <a-form-item label="绑定概念" name="conceptId">
          <a-select
            v-model:value="form.conceptId"
            placeholder="请选择绑定的概念"
            show-search
            option-filter-prop="label"
            :options="conceptOptions"
          />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="form.description" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="请输入描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 提交执行对话框 -->
    <a-modal title="提交执行" v-model:open="execOpen" width="600px" destroy-on-close ok-text="OK" cancel-text="Cancel" :confirm-loading="execSaving" @ok="submitExec" @cancel="execOpen = false">
      <a-alert
        type="info"
        show-icon
        :message="'动作：' + (execAction.name || '') + '（' + actionTypeText(execAction.actionType) + ' · 绑定概念：' + (conceptMap[execAction.conceptId] || '-') + '）'"
        style="margin-bottom: 12px;"
      />
      <p class="modal-hint-line">提交后系统生成 SQL 并预检，进入审批流；可在下方「执行记录」中审批与执行。</p>
      <a-form :label-col="{ style: { width: '90px' } }">
        <a-form-item label="输入参数">
          <a-textarea
            v-model:value="inputParams"
            :auto-size="{ minRows: 4, maxRows: 10 }"
            placeholder='JSON 格式，如 {"name":"张三"}'
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 执行结果对话框 -->
    <a-modal title="执行结果" v-model:open="resultOpen" width="720px" :footer="null">
      <div class="result-section">
        <div class="result-title">生成 SQL</div>
        <pre class="result-pre sql-pre">{{ currentResult.generatedSql || '无' }}</pre>
      </div>
      <div class="result-section">
        <div class="result-title">预览结果</div>
        <pre class="result-pre">{{ currentResult.previewResult || '无' }}</pre>
      </div>
      <div v-if="currentResult.beforeData" class="result-section">
        <div class="result-title">执行前数据</div>
        <pre class="result-pre">{{ currentResult.beforeData }}</pre>
      </div>
      <div v-if="currentResult.errorMessage" class="result-section">
        <div class="result-title error">错误信息</div>
        <pre class="result-pre error-pre">{{ currentResult.errorMessage }}</pre>
      </div>
    </a-modal>
  </div>
</template>

<script setup name="ActionPanel">
import { listAction, getAction, addAction, updateAction, delAction, submitExecution, approveExecution, rejectExecution, runExecution, listExecution } from '@/api/ont/action'
import { listConcept } from '@/api/ont/concept'
import { PlusOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  ontologyId: {
    type: [Number, String],
    required: true
  }
})

const { proxy } = getCurrentInstance()
const loading = ref(false)
const actionList = ref([])
const total = ref(0)
const open = ref(false)
const title = ref('')

// 执行记录状态
const execLoading = ref(false)
const execList = ref([])

// 概念下拉与名称映射（动作必须绑定到本体内的概念）
const conceptOptions = ref([])
const conceptMap = computed(() => {
  const map = {}
  conceptOptions.value.forEach(c => { map[c.value] = c.label })
  return map
})
// 动作名称映射（执行记录展示用）
const actionNameMap = computed(() => {
  const map = {}
  actionList.value.forEach(a => { map[a.id] = a.name })
  return map
})

const actionTypeOptions = [
  { value: 'CREATE', label: '新建' },
  { value: 'UPDATE', label: '更新' },
  { value: 'DELETE', label: '删除' },
  { value: 'QUERY', label: '查询' }
]

function actionTypeText(type) {
  return { CREATE: '新建', UPDATE: '更新', DELETE: '删除', QUERY: '查询' }[type] || type
}

function actionTypeColor(type) {
  return { CREATE: 'green', UPDATE: 'blue', DELETE: 'red', QUERY: 'purple' }[type] || 'default'
}

function execStatusText(s) {
  return { DRAFT: '草稿', PENDING_APPROVAL: '待审批', APPROVED: '已批准', REJECTED: '已拒绝', EXECUTED: '已执行', FAILED: '失败' }[s] || s
}

function execStatusColor(s) {
  return { DRAFT: 'default', PENDING_APPROVAL: 'orange', APPROVED: 'green', REJECTED: 'red', EXECUTED: 'blue', FAILED: 'red' }[s] || 'default'
}

const columns = [
  { title: '动作名称', dataIndex: 'name', align: 'left', width: 160 },
  { title: '动作类型', key: 'actionType', align: 'center', width: 100 },
  { title: '绑定概念', key: 'conceptName', align: 'center', width: 140 },
  { title: '描述', dataIndex: 'description', align: 'left', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 170 },
  { title: '操作', key: 'action', align: 'center', width: 170, fixed: 'right' }
]

const execColumns = [
  { title: '动作', key: 'actionName', align: 'left', width: 140 },
  { title: '状态', key: 'status', align: 'center', width: 100 },
  { title: '生成SQL', dataIndex: 'generatedSql', align: 'left', ellipsis: true },
  { title: '审批意见', dataIndex: 'approvalReason', align: 'center', width: 130, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 170 },
  { title: '操作', key: 'execAction', align: 'center', width: 210, fixed: 'right' }
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ontologyId: props.ontologyId,
  name: undefined,
  actionType: undefined
})

const data = reactive({
  form: {},
  rules: {
    name: [{ required: true, message: '动作名称不能为空', trigger: 'blur' }],
    actionType: [{ required: true, message: '动作类型不能为空', trigger: 'change' }],
    conceptId: [{ required: true, message: '绑定概念不能为空', trigger: 'change' }]
  }
})
const { form, rules } = toRefs(data)

function getList() {
  loading.value = true
  queryParams.ontologyId = props.ontologyId
  listAction(queryParams).then(res => {
    actionList.value = res.data?.rows || []
    total.value = res.data?.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function loadExecutions() {
  execLoading.value = true
  listExecution({ pageNum: 1, pageSize: 20, ontologyId: props.ontologyId }).then(res => {
    execList.value = res.data?.rows || []
  }).finally(() => {
    execLoading.value = false
  })
}

function loadConcepts() {
  listConcept({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 100 }).then(res => {
    const rows = (res.data && res.data.rows) || []
    conceptOptions.value = rows.map(c => ({ value: c.id, label: c.name }))
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.name = undefined
  queryParams.actionType = undefined
  handleQuery()
}

function reset() {
  form.value = { ontologyId: props.ontologyId, name: undefined, actionType: undefined, conceptId: undefined, description: undefined }
  proxy.resetForm('actionRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增动作'
}

function handleUpdate(row) {
  reset()
  getAction(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改动作'
  })
}

function submitForm() {
  proxy.$refs['actionRef'].validate().then(() => {
    form.value.ontologyId = props.ontologyId
    const fn = form.value.id ? updateAction : addAction
    fn(form.value).then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  }).catch(() => {})
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除动作"' + row.name + '"？').then(() => {
    return delAction(row.id)
  }).then(() => {
    getList()
    loadExecutions()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function cancel() {
  open.value = false
  reset()
}

/* ================= 提交执行 ================= */

const execOpen = ref(false)
const execSaving = ref(false)
const execAction = ref({})
const inputParams = ref('{}')

function openSubmitExec(row) {
  execAction.value = row
  inputParams.value = '{}'
  execOpen.value = true
}

function submitExec() {
  let params = {}
  const raw = (inputParams.value || '').trim()
  if (raw) {
    try {
      params = JSON.parse(raw)
    } catch (e) {
      proxy.$modal.msgError('输入参数必须是合法的 JSON')
      return
    }
  }
  execSaving.value = true
  submitExecution({ actionId: execAction.value.id, inputParams: JSON.stringify(params) }).then(res => {
    proxy.$modal.msgSuccess(res.data && res.data.status === 'PENDING_APPROVAL' ? '已提交审批' : '已提交')
    execOpen.value = false
    loadExecutions()
  }).finally(() => {
    execSaving.value = false
  })
}

/* ================= 审批与执行 ================= */

function handleApprove(record) {
  proxy.$modal.confirm('确认审批通过该执行记录？').then(() => {
    return approveExecution({ executionId: record.id, approvalReason: '同意' })
  }).then(() => {
    proxy.$modal.msgSuccess('审批通过')
    loadExecutions()
  }).catch(() => {})
}

function handleReject(record) {
  proxy.$modal.confirm('确认拒绝该执行记录？').then(() => {
    return rejectExecution({ executionId: record.id, approvalReason: '拒绝' })
  }).then(() => {
    proxy.$modal.msgSuccess('已拒绝')
    loadExecutions()
  }).catch(() => {})
}

function handleRun(record) {
  proxy.$modal.confirm('确认执行该记录？执行后数据将变更。').then(() => {
    return runExecution(record.id)
  }).then(() => {
    proxy.$modal.msgSuccess('执行完成')
    loadExecutions()
  }).catch(() => {})
}

const resultOpen = ref(false)
const currentResult = ref({})

function handleViewResult(record) {
  currentResult.value = record
  resultOpen.value = true
}

defineExpose({
  reload() {
    getList()
    loadExecutions()
  }
})

loadConcepts()
getList()
loadExecutions()
</script>

<style lang="scss" scoped>
.action-panel {
  .panel-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    .toolbar-right {
      margin-left: auto;
    }
  }

  .sql-cell {
    font-family: monospace;
    font-size: 12px;
  }

  .exec-divider {
    margin: 16px 0 12px;
  }

  .modal-hint-line {
    margin: 0 0 8px;
    color: #8a95a6;
    font-size: 12px;
  }

  .result-section {
    margin-bottom: 12px;

    .result-title {
      font-size: 12px;
      font-weight: 600;
      color: #555;
      margin-bottom: 4px;

      &.error {
        color: #cf1322;
      }
    }

    .result-pre {
      margin: 0;
      padding: 10px 12px;
      background: #f6f8fa;
      border: 1px solid #e5eaf2;
      border-radius: 6px;
      font-family: monospace;
      font-size: 12px;
      max-height: 220px;
      overflow: auto;
      white-space: pre-wrap;
      word-break: break-all;

      &.error-pre {
        background: #fff1f0;
        border-color: #ffa39e;
      }
    }
  }
}
</style>
