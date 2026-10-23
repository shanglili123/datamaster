<template>
  <div class="app-container asset-operate-apply-page">
    <div class="pagecont-top" v-show="showSearch">
      <a-form ref="queryRef" :model="queryParams" layout="inline" @submit.prevent>
        <a-form-item label="目标表" name="tableName">
          <a-input v-model:value="queryParams.tableName" allow-clear placeholder="请输入表名" @pressEnter="handleQuery" />
        </a-form-item>
        <a-form-item label="操作类型" name="operateType">
          <a-select v-model:value="queryParams.operateType" allow-clear style="width: 150px" placeholder="请选择">
            <a-select-option v-for="item in operateTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态" name="approvalStatus">
          <a-select v-model:value="queryParams.approvalStatus" allow-clear style="width: 180px" placeholder="请选择">
            <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleQuery">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
      <div class="top-right-btn">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>
    </div>

    <div class="pagecont-main">
      <div class="operate-toolbar">
        <a-space>
          <a-button type="primary" v-hasPermi="['ast:assetOperateApply:add']" @click="handleAdd">新建申请</a-button>
          <a-button @click="getList">刷新</a-button>
        </a-space>
        <span class="toolbar-tip">资产操作必须经过审批后才能执行</span>
      </div>

      <a-table
        :loading="loading"
        :data-source="rows"
        :columns="tableColumns"
        :pagination="false"
        :scroll="{ x: 1250 }"
        row-key="id"
        :locale="{ emptyText: '暂无申请' }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'operateType'">
            {{ operateTypeText(record.operateType) }}
          </template>
          <template v-else-if="column.dataIndex === 'approvalStatus'">
            <a-tag :color="statusColor(record)">{{ statusText(record) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'operateTime' || column.dataIndex === 'createTime'">
            {{ formatTime(record[column.dataIndex]) }}
          </template>
          <template v-else-if="column.dataIndex === 'operateJson'">
            <a-typography-paragraph :ellipsis="{ rows: 2 }" :content="record.operateJson || '-'" />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space size="small" wrap>
              <a-button type="link" size="small" @click="handleDetail(record)">详情</a-button>
              <a-button
                v-if="canEdit(record)"
                type="link"
                size="small"
                v-hasPermi="['ast:assetOperateApply:edit']"
                @click="handleEdit(record)"
              >编辑</a-button>
              <a-button
                v-if="canSubmit(record)"
                type="link"
                size="small"
                v-hasPermi="['ast:assetOperateApply:edit']"
                @click="handleSubmit(record)"
              >提交</a-button>
              <a-button
                v-if="record.approvalStatus === 'PENDING_APPROVAL'"
                type="link"
                size="small"
                v-hasPermi="['ast:assetOperateApply:edit']"
                @click="handleReview(record)"
              >审批</a-button>
              <a-button
                v-if="record.approvalStatus === 'APPROVED'"
                type="link"
                size="small"
                v-hasPermi="['ast:assetOperateApply:execute']"
                @click="handleExecute(record)"
              >执行</a-button>
              <a-button
                v-if="canDelete(record)"
                type="link"
                danger
                size="small"
                v-hasPermi="['ast:assetOperateApply:remove']"
                @click="handleDelete(record)"
              >删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <pagination
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <a-modal v-model:open="formOpen" :title="formTitle" width="760px" :confirm-loading="saving" @ok="submitForm">
      <a-form ref="formRef" :model="form" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="资产 ID" name="assetId">
              <a-input-number v-model:value="form.assetId" :min="1" style="width: 100%" placeholder="请输入资产 ID" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据源 ID" name="datasourceId">
              <a-input-number v-model:value="form.datasourceId" :min="1" style="width: 100%" placeholder="请输入数据源 ID" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="目标表" name="tableName">
              <a-input v-model:value="form.tableName" placeholder="请输入目标表名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="操作类型" name="operateType">
              <a-select v-model:value="form.operateType" placeholder="请选择操作类型">
                <a-select-option v-for="item in operateTypeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="表说明" name="tableComment">
          <a-input v-model:value="form.tableComment" allow-clear placeholder="可选" />
        </a-form-item>
        <a-form-item label="操作 JSON" name="operateJson" extra="示例：{&quot;updateBefore&quot;:{},&quot;updateAfter&quot;:{},&quot;fieldNames&quot;:[&quot;id&quot;]}">
          <a-textarea v-model:value="form.operateJson" :rows="10" show-count :maxlength="65535" placeholder="请输入合法 JSON" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="reviewOpen" title="审批资产操作申请" width="620px" :confirm-loading="reviewing" @ok="submitReview">
      <a-descriptions bordered :column="2" size="small">
        <a-descriptions-item label="目标表">{{ reviewForm.tableName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="操作类型">{{ operateTypeText(reviewForm.operateType) }}</a-descriptions-item>
        <a-descriptions-item label="申请人">{{ reviewForm.createBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="申请时间">{{ formatTime(reviewForm.createTime) }}</a-descriptions-item>
      </a-descriptions>
      <a-form ref="reviewRef" :model="reviewForm" :rules="reviewRules" layout="vertical" class="review-form">
        <a-form-item label="审批意见" name="comment">
          <a-textarea v-model:value="reviewForm.comment" :rows="5" :placeholder="reviewAction === 'reject' ? '驳回时必须填写原因' : '可填写审批说明'" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="reviewOpen = false">取消</a-button>
          <a-button danger :loading="reviewing" @click="submitReview('reject')">驳回</a-button>
          <a-button type="primary" :loading="reviewing" @click="submitReview('approve')">通过</a-button>
        </a-space>
      </template>
    </a-modal>

    <a-modal v-model:open="detailOpen" title="资产操作申请详情" width="780px" :footer="null">
      <a-descriptions bordered :column="2" size="small">
        <a-descriptions-item label="申请 ID">{{ detailForm.id }}</a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="statusColor(detailForm)">{{ statusText(detailForm) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="资产 ID">{{ detailForm.assetId }}</a-descriptions-item>
        <a-descriptions-item label="数据源 ID">{{ detailForm.datasourceId }}</a-descriptions-item>
        <a-descriptions-item label="目标表">{{ detailForm.tableName }}</a-descriptions-item>
        <a-descriptions-item label="操作类型">{{ operateTypeText(detailForm.operateType) }}</a-descriptions-item>
        <a-descriptions-item label="申请人">{{ detailForm.createBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审批人">{{ detailForm.approvedBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="申请时间">{{ formatTime(detailForm.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="审批时间">{{ formatTime(detailForm.approvedTime) }}</a-descriptions-item>
        <a-descriptions-item label="审批意见" :span="2">{{ detailForm.approvalComment || '-' }}</a-descriptions-item>
        <a-descriptions-item label="执行结果" :span="2">{{ detailForm.executionMessage || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div class="detail-json-title">操作 JSON</div>
      <pre class="detail-json">{{ prettyJson(detailForm.operateJson) }}</pre>
    </a-modal>
  </div>
</template>

<script setup name="AssetOperateApply">
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { normalizePage, pageRows } from '@/utils/page.js'
import {
  addDaAssetOperateApply,
  approveDaAssetOperateApply,
  delDaAssetOperateApply,
  executeDaAssetOperateApply,
  getDaAssetOperateApply,
  listDaAssetOperateApply,
  rejectDaAssetOperateApply,
  submitDaAssetOperateApply,
  updateDaAssetOperateApply,
} from '@/api/ast/assetchild/operate/daAssetOperateApply'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const saving = ref(false)
const reviewing = ref(false)
const showSearch = ref(true)
const formOpen = ref(false)
const reviewOpen = ref(false)
const detailOpen = ref(false)
const formTitle = ref('新建资产操作申请')
const reviewAction = ref('approve')
const total = ref(0)
const rows = ref([])
const formRef = ref()
const reviewRef = ref()
const queryRef = ref()

const operateTypeOptions = [
  { value: '1', label: '新增' },
  { value: '2', label: '修改' },
  { value: '3', label: '删除' },
  { value: '4', label: '导入（需任务执行）' },
]
const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING_APPROVAL', label: '待审批' },
  { value: 'APPROVED', label: '已批准' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'EXECUTING', label: '执行中' },
  { value: 'EXECUTED', label: '已执行' },
  { value: 'FAILED', label: '执行失败' },
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tableName: undefined,
  operateType: undefined,
  approvalStatus: undefined,
  orderByColumn: 'create_time',
  isAsc: 'desc',
})
const form = reactive({
  id: null,
  assetId: null,
  datasourceId: null,
  tableName: '',
  tableComment: '',
  operateType: undefined,
  operateJson: '',
})
const detailForm = reactive({})
const reviewForm = reactive({ id: null, tableName: '', operateType: '', createBy: '', createTime: null, comment: '' })

const tableColumns = computed(() => [
  { title: '申请 ID', dataIndex: 'id', width: 90 },
  { title: '目标表', dataIndex: 'tableName', width: 180, ellipsis: true },
  { title: '操作类型', dataIndex: 'operateType', width: 120 },
  { title: '操作内容', dataIndex: 'operateJson', width: 260, ellipsis: true },
  { title: '状态', dataIndex: 'approvalStatus', width: 130, align: 'center' },
  { title: '申请人', dataIndex: 'createBy', width: 110, ellipsis: true },
  { title: '申请时间', dataIndex: 'createTime', width: 170 },
  { title: '执行结果', dataIndex: 'executionMessage', width: 240, ellipsis: true },
  { title: '操作', key: 'actions', fixed: 'right', width: 300 },
])

const formRules = {
  assetId: [{ required: true, message: '请输入资产 ID', trigger: 'blur' }],
  datasourceId: [{ required: true, message: '请输入数据源 ID', trigger: 'blur' }],
  tableName: [{ required: true, message: '请输入目标表名', trigger: 'blur' }],
  operateType: [{ required: true, message: '请选择操作类型', trigger: 'change' }],
  operateJson: [
    { required: true, message: '请输入操作 JSON', trigger: 'blur' },
    { validator: validateJson, trigger: 'blur' },
  ],
}
const reviewRules = computed(() => reviewAction.value === 'reject'
  ? { comment: [{ required: true, message: '驳回时必须填写审批意见', trigger: 'blur' }] }
  : {})

function validateJson(_rule, value) {
  if (!value) return Promise.resolve()
  try {
    JSON.parse(value)
    return Promise.resolve()
  } catch (_error) {
    return Promise.reject(new Error('操作 JSON 格式不正确'))
  }
}

function normalizeStatus(row = {}) {
  if (row.approvalStatus) return row.approvalStatus
  return row.executeFlag === '1' && row.executeTime ? 'EXECUTED' : 'DRAFT'
}
function statusText(row) {
  return statusOptions.find(item => item.value === normalizeStatus(row))?.label || normalizeStatus(row) || '-'
}
function statusColor(row) {
  return ({ DRAFT: 'default', PENDING_APPROVAL: 'processing', APPROVED: 'blue', REJECTED: 'red', EXECUTING: 'cyan', EXECUTED: 'green', FAILED: 'orange' })[normalizeStatus(row)] || 'default'
}
function operateTypeText(value) {
  return operateTypeOptions.find(item => item.value === String(value))?.label || value || '-'
}
function formatTime(value) {
  if (!value) return '-'
  if (typeof proxy?.parseTime === 'function') return proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}')
  return value
}
function prettyJson(value) {
  if (!value) return '-'
  try { return JSON.stringify(JSON.parse(value), null, 2) } catch (_error) { return value }
}
function canEdit(row) { return ['DRAFT', 'REJECTED', 'FAILED'].includes(normalizeStatus(row)) }
function canSubmit(row) { return ['DRAFT', 'REJECTED', 'FAILED'].includes(normalizeStatus(row)) }
function canDelete(row) { return ['DRAFT', 'REJECTED', 'FAILED'].includes(normalizeStatus(row)) }

async function getList() {
  loading.value = true
  try {
    const response = await listDaAssetOperateApply({ ...queryParams })
    const page = normalizePage(response)
    total.value = page.total
    rows.value = pageRows(page.rows, page.total, queryParams)
  } finally {
    loading.value = false
  }
}
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  queryParams.tableName = undefined
  queryParams.operateType = undefined
  queryParams.approvalStatus = undefined
  queryParams.pageNum = 1
  queryRef.value?.resetFields?.()
  getList()
}
function handleTableChange(_pagination, _filters, sorter) {
  const orderMap = { ascend: 'asc', descend: 'desc' }
  queryParams.orderByColumn = sorter?.column?.key || sorter?.field || 'create_time'
  queryParams.isAsc = orderMap[sorter?.order] || 'desc'
  getList()
}
function resetForm() {
  Object.assign(form, { id: null, assetId: null, datasourceId: null, tableName: '', tableComment: '', operateType: undefined, operateJson: '' })
  formRef.value?.resetFields?.()
}
async function handleAdd() {
  resetForm()
  formTitle.value = '新建资产操作申请'
  formOpen.value = true
}
async function handleEdit(row) {
  try {
    const response = await getDaAssetOperateApply(row.id)
    Object.assign(form, response.data || {})
    formTitle.value = '编辑资产操作申请'
    formOpen.value = true
  } catch (_error) { /* request interceptor handles message */ }
}
async function submitForm() {
  try {
    await formRef.value.validate()
    saving.value = true
    if (form.id) await updateDaAssetOperateApply({ ...form })
    else await addDaAssetOperateApply({ ...form })
    message.success(form.id ? '保存成功' : '申请已保存为草稿')
    formOpen.value = false
    await getList()
  } finally {
    saving.value = false
  }
}
function handleSubmit(row) {
  Modal.confirm({
    title: '确认提交申请？',
    content: '提交后将进入审批流程，提交期间不能直接修改申请内容。',
    onOk: async () => {
      await submitDaAssetOperateApply(row.id)
      message.success('申请已提交审批')
      await getList()
    },
  })
}
async function handleReview(row) {
  const response = await getDaAssetOperateApply(row.id)
  Object.assign(reviewForm, { ...response.data, comment: '' })
  reviewAction.value = 'approve'
  reviewOpen.value = true
}
async function submitReview(action) {
  reviewAction.value = action
  await reviewRef.value?.validate?.()
  reviewing.value = true
  try {
    const payload = { id: reviewForm.id, comment: reviewForm.comment }
    if (action === 'reject') await rejectDaAssetOperateApply(reviewForm.id, payload)
    else await approveDaAssetOperateApply(reviewForm.id, payload)
    message.success(action === 'reject' ? '申请已驳回' : '申请已批准')
    reviewOpen.value = false
    await getList()
  } finally {
    reviewing.value = false
  }
}
function handleExecute(row) {
  Modal.confirm({
    title: '确认执行申请？',
    content: '执行会对目标数据源产生实际写入，请确认审批内容和目标表无误。',
    onOk: async () => {
      await executeDaAssetOperateApply(row.id)
      message.success('执行请求已提交')
      await getList()
    },
  })
}
function handleDetail(row) {
  getDaAssetOperateApply(row.id).then(response => {
    Object.keys(detailForm).forEach(key => delete detailForm[key])
    Object.assign(detailForm, response.data || {})
    detailOpen.value = true
  })
}
function handleDelete(row) {
  Modal.confirm({
    title: '确认删除草稿？',
    content: `申请 ${row.id} 删除后不可恢复。`,
    onOk: async () => {
      await delDaAssetOperateApply(row.id)
      message.success('删除成功')
      await getList()
    },
  })
}

onMounted(getList)
</script>

<style scoped>
.asset-operate-apply-page { min-height: 100%; }
.operate-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.toolbar-tip { color: #8c8c8c; font-size: 13px; }
.review-form { margin-top: 18px; }
.detail-json-title { margin-top: 18px; margin-bottom: 8px; font-weight: 600; }
.detail-json { max-height: 360px; overflow: auto; padding: 12px; border-radius: 4px; background: #f6f8fa; white-space: pre-wrap; word-break: break-word; }
</style>
