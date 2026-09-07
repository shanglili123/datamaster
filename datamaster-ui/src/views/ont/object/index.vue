<template>
  <div class="app-container">
    <a-layout style="min-height: calc(100vh - 120px)">
      <a-layout-sider :width="320" theme="light" :bordered="true">
        <div style="padding: 12px">
          <div style="font-size: 14px; font-weight: 600; margin-bottom: 12px">对象集（概念 + 物理表）</div>
          <a-input-search v-model:value="searchValue" placeholder="搜索对象集" style="margin-bottom: 12px" />
        </div>
        <a-spin :spinning="objectSetLoading">
          <div v-if="objectSets.length > 0" style="padding: 0 8px">
            <div
              v-for="os in filteredObjectSets"
              :key="os.tableBindingId"
              class="object-set-item"
              :class="{ active: selectedObjectSet && os.tableBindingId === selectedObjectSet.tableBindingId }"
              @click="selectObjectSet(os)"
            >
              <div class="object-set-title">
                <span>{{ os.conceptName }}</span>
              </div>
              <div class="object-set-sub">{{ os.tableName }}<template v-if="os.databaseName">（{{ os.databaseName }}）</template></div>
            </div>
          </div>
          <a-empty v-else description="暂无对象集" />
        </a-spin>
      </a-layout-sider>

      <a-layout-content style="padding: 0 16px">
        <a-card v-if="selectedObjectSet" :title="selectedObjectSet.conceptName + ' - 对象实例'">
          <template #extra>
            <a-space>
              <a-button v-if="rowOperateEnabled" type="primary" size="small" @click="openRowCreate">
                <template #icon><PlusOutlined /></template>
                新增数据
              </a-button>
              <a-button type="primary" ghost size="small" @click="openLineage">
                对象血缘（四维度）
              </a-button>
              <span style="color:#999">物理表：{{ selectedObjectSet.tableName }}</span>
              <a-button size="small" :loading="loading" @click="handleSearch">刷新</a-button>
            </a-space>
          </template>
          <!-- 筛选工具栏：完整宽度，不再挤压在卡片头部 -->
          <div class="filter-toolbar">
            <OntFilterBuilder
              :fields="filterFields"
              v-model="filterSpec"
              show-query
              @query="handleSearch"
            />
          </div>
          <a-table
            :columns="columns"
            :data-source="rowData"
            :loading="loading"
            :pagination="pagination"
            row-key="__rid__"
            size="middle"
            :scroll="{ x: 'max-content' }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'rowIndex'">
                {{ (pagination.current - 1) * pagination.pageSize + record.__index__ }}
              </template>
              <template v-else-if="column.key === '__operate__'">
                <a-space v-if="rowOperateEnabled">
                  <a-button type="link" size="small" :disabled="rowModifiable" @click="openRowEdit(record)">修改</a-button>
                  <a-popconfirm
                    title="删除该对象实例？"
                    description="删除操作会先生成执行记录并进入确认流程"
                    ok-text="删除"
                    cancel-text="取消"
                    :ok-button-props="{ danger: true }"
                    @confirm="openRowDelete(record)"
                  >
                    <a-button type="link" size="small" danger :disabled="rowModifiable">删除</a-button>
                  </a-popconfirm>
                </a-space>
                <span v-else style="color:#bbb">—</span>
              </template>
              <template v-else-if="column.key === '__relation__'">
                <template v-if="currentRelations.length">
                  <OntRelationJump
                    v-for="rel in currentRelations"
                    :key="'rel-' + rel.id"
                    :ontology-id="route.params.ontologyId"
                    :concept-id="selectedObjectSet.conceptId"
                    :table-binding-id="selectedObjectSet.tableBindingId"
                    :row="record"
                    :relation="rel"
                  />
                </template>
                <span v-else style="color:#bbb">无</span>
              </template>
            </template>
          </a-table>
        </a-card>
        <a-card v-else><a-empty description="请在左侧选择一个对象集" /></a-card>
      </a-layout-content>
    </a-layout>

    <ObjectLineageDialog
      v-model:visible="lineageVisible"
      :concept-id="lineageConcept"
      :concept-name="lineageTableName"
      :table-name="lineageTable"
    />

    <!-- 行操作弹框：新增/修改/删除 → 提交预览 → 确认执行（审批移至审批中心，不在弹框内嵌） -->
    <a-modal
    v-model:open="rowModal.visible"
    :title="rowModalTitle"
    :width="760"
    wrap-class-name="ontology-workspace-modal ontology-modal--data"
      :footer="null"
      :mask-closable="false"
      destroy-on-close
      @cancel="closeRowModal"
    >
      <a-spin :spinning="rowModal.loading || rowModal.confirming">
        <template v-if="rowModal.mode === 'DELETE'">
          <a-alert
            type="warning"
            show-icon
            :message="'确定删除该对象实例？'"
            :description="'将按主属性准确定位对象并进入确认流程，可在下方核对执行内容。'"
          />
          <div class="row-delete-pk">
            <span class="row-delete-label">主键定位：</span>
            <template v-for="f in pkFields" :key="'pk-' + f.propertyCode">
              <a-tag v-if="rowModal.form[f.propertyCode] !== '' && rowModal.form[f.propertyCode] != null" color="red">{{ f.propertyName }} = {{ rowModal.form[f.propertyCode] }}</a-tag>
            </template>
          </div>
        </template>
        <a-form v-else class="ontology-form-grid" :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }" style="margin-top: 8px">
          <template v-for="f in formFields" :key="'f-' + f.propertyCode">
            <a-form-item :class="{ 'ontology-form-grid__full': f.dataType === 'text' }" :label="f.propertyName + (f.isPrimary ? '（主键）' : '')">
              <a-input
                v-if="f.dataType === 'string'"
                v-model:value="rowModal.form[f.propertyCode]"
                :disabled="rowModal.mode === 'UPDATE' && f.isPrimary"
                allow-clear
              />
              <a-date-picker
                v-else-if="f.dataType === 'date'"
                v-model:value="rowModal.form[f.propertyCode]"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                :disabled="rowModal.mode === 'UPDATE' && f.isPrimary"
              />
              <a-input-number
                v-else-if="f.dataType === 'integer' || f.dataType === 'decimal'"
                v-model:value="rowModal.form[f.propertyCode]"
                :disabled="rowModal.mode === 'UPDATE' && f.isPrimary"
                style="width: 100%"
              />
              <a-switch
                v-else-if="f.dataType === 'boolean'"
                v-model:checked="rowModal.form[f.propertyCode]"
                :disabled="rowModal.mode === 'UPDATE' && f.isPrimary"
              />
              <a-textarea
                v-else
                v-model:value="rowModal.form[f.propertyCode]"
                :disabled="rowModal.mode === 'UPDATE' && f.isPrimary"
                :rows="2"
                allow-clear
              />
            </a-form-item>
          </template>
        </a-form>

        <a-divider style="margin: 12px 0">回调选项</a-divider>
        <a-form class="ontology-form-grid" :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }">
          <a-form-item class="ontology-form-grid__full" label="执行后回调">
            <a-switch v-model:checked="rowModal.triggerWebhook" />
            <span class="row-webhook-hint">开启后按动作/本体绑定的 Webhook 配置，在执行成功后发起回调</span>
          </a-form-item>
        </a-form>

        <template v-if="rowModal.preview">
          <a-divider style="margin: 12px 0">预览与执行</a-divider>
          <a-alert
            :message="rowModal.preview.message"
            :type="previewAlertType"
            show-icon
          />
          <template v-if="rowModal.preview.generatedSql">
            <div class="row-sql-title">执行内容预览</div>
            <pre class="row-sql-block">{{ rowModal.preview.generatedSql }}</pre>
          </template>
          <p v-if="rowModal.preview.previewResult" class="row-preview-result">
            预览结果：{{ rowModal.preview.previewResult }}
          </p>
        </template>

        <div class="row-modal-footer">
          <template v-if="!rowModal.preview">
            <a-button @click="closeRowModal">取消</a-button>
            <a-button type="primary" :loading="rowModal.loading" @click="submitRowPreview">
              {{
                rowModal.mode === 'DELETE'
                  ? '提交删除预览'
                  : rowModal.mode === 'UPDATE'
                    ? '提交修改预览'
                    : '提交新增预览'
              }}
            </a-button>
          </template>
          <template v-else>
            <div class="row-footer-left">
              <template v-if="rowModal.preview.status === 'PENDING_APPROVAL' && !rowModal.preview.canApprove">
                <a-button type="link" @click="goApprovalCenter">已提交待审批，前往审批中心处理本单</a-button>
              </template>
            </div>
            <a-button @click="closeRowModal">关闭</a-button>
            <a-button @click="submitRowPreview" :loading="rowModal.loading">重新预览</a-button>
            <a-button
              v-if="previewCanExecute"
              type="primary"
              :loading="rowModal.confirming"
              @click="confirmRowExecute"
            >
              确认执行
            </a-button>
          </template>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>
<script setup name="OntObjectInstance">
import { listObjectSets, queryObjects, rowPreview, rowConfirm } from '@/api/ont/objectInstance'
import { listRelation } from '@/api/ont/relation'
import ObjectLineageDialog from './components/ObjectLineageDialog.vue'
import OntFilterBuilder from '@/components/OntFilterBuilder/index.vue'
import OntRelationJump from '@/components/OntRelationJump/index.vue'
import { useRoute, useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import useUserStore from '@/store/system/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 行操作权限：与对象实例查询同权限（能进对象浏览器即可用行操作；真实写权限由后端
// checkTableAccess ONTOLOGY_CREATE/UPDATE/DELETE 字段级严格模式兜底）。admin（*:*:*）等同放行。
const rowOperateEnabled = computed(() => {
  const perms = userStore.permissions
  if (!Array.isArray(perms) || perms.length === 0) return false
  return perms.includes('*:*:*') || perms.includes('ont:object-instance:query')
})

const objectSetLoading = ref(false)
const objectSets = ref([])
const selectedObjectSet = ref(null)
const searchValue = ref('')

// 类型化查询 spec + 可选字段（OntFilterBuilder）
const filterFields = ref([])
const filterSpec = ref({ groups: [{ connector: 'AND', filters: [] }], orderBy: [], columns: [], keyword: '' })
// 当前对象集（源概念）的出向关系，用于行内“关联”跳转
const currentRelations = ref([])

// 对象血缘（四维度）弹窗状态
const lineageVisible = ref(false)
const lineageConcept = ref(null)
const lineageTableName = ref('')
const lineageTable = ref('')

// ---------- 行操作（新增/修改/删除：提交预览 → 审批（审批中心） → 确认执行） ----------
const rowModal = reactive({
  visible: false,
  mode: 'CREATE', // CREATE | UPDATE | DELETE
  loading: false, // 提交预览中
  confirming: false, // 确认执行中
  form: {}, // key = 属性编码 propertyCode
  triggerWebhook: true,
  preview: null // RowOperateRespVO
})

// 可编辑表单字段（仅含物理列映射属性），主键字段编辑态只读
const formFields = computed(() => {
  const os = selectedObjectSet.value
  if (!os || !Array.isArray(os.properties)) return []
  return os.properties
    .filter(p => p.physicalColumnName)
    .map(p => ({
      propertyId: p.propertyId,
      propertyName: p.propertyName || p.physicalColumnName,
      propertyCode: p.propertyCode,
      physicalColumnName: p.physicalColumnName,
      dataType: p.dataType || 'string',
      isPrimary: !!p.isPrimary
    }))
})

// 主键字段（DELETE 定位 / UPDATE 只读 / 新增加粗校验）
const pkFields = computed(() => formFields.value.filter(f => f.isPrimary))

// 无主键映射时禁用行内修改/删除（后端亦无 WHERE 定位依据）
const rowModifiable = computed(() => pkFields.value.length === 0)

const rowModalTitle = computed(() => {
  const os = selectedObjectSet.value
  const name = os ? os.conceptName : ''
  if (rowModal.mode === 'CREATE') return `新增对象实例 - ${name}`
  if (rowModal.mode === 'UPDATE') return `修改对象实例 - ${name}`
  return `删除对象实例 - ${name}`
})

// 弹框内预览状态 → 按钮可用性/文案。
// 审批已移至审批中心（工作台「动作管理」），行操作弹框不再内嵌审批：
// PENDING_APPROVAL 一律 canApprove=false，仅 APPROVED 才允许确认执行。
const previewCanExecute = computed(() => {
  const p = rowModal.preview
  return !!p && p.status === 'APPROVED'
})
const previewAlertType = computed(() => {
  const p = rowModal.preview
  if (!p) return 'info'
  if (p.status === 'EXECUTED' || p.status === 'APPROVED') return 'success'
  if (p.status === 'RUNNING') return 'processing'
  if (p.status === 'RECONCILIATION_REQUIRED') return 'error'
  if (p.status === 'PENDING_APPROVAL') return 'warning'
  return 'error'
})

const rowData = ref([])
const columns = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条`
})

const filteredObjectSets = computed(() => {
  const keyword = searchValue.value.trim().toLowerCase()
  if (!keyword) return objectSets.value
  return objectSets.value.filter(os =>
    (os.conceptName || '').toLowerCase().includes(keyword) ||
    (os.conceptCode || '').toLowerCase().includes(keyword) ||
    (os.tableName || '').toLowerCase().includes(keyword)
  )
})

function selectObjectSet(os) {
  selectedObjectSet.value = os
  pagination.current = 1
  resetFilterSpec()
  buildFilterFields(os)
  buildColumns(os)
  loadRelations(os)
  loadData()
}

// 重置类型化查询 spec
function resetFilterSpec() {
  filterSpec.value = { groups: [{ connector: 'AND', filters: [] }], orderBy: [], columns: [], keyword: '' }
}

// 构建 OntFilterBuilder 可选字段（语义属性名 → 物理列）
function buildFilterFields(os) {
  const fields = []
  ;(os.properties || []).forEach(p => {
    if (p.physicalColumnName) {
      fields.push({ label: p.propertyName || p.physicalColumnName, value: p.physicalColumnName })
    }
  })
  filterFields.value = fields
}

// 加载当前对象集（源概念）的出向关系，供行内“关联”跳转
async function loadRelations(os) {
  currentRelations.value = []
  if (!os || !os.conceptId) return
  try {
    const res = await listRelation({ ontologyId: route.params.ontologyId, pageNum: 1, pageSize: 200 })
    const rows = (res.data && res.data.rows) || []
    currentRelations.value = rows.filter(r =>
      String(r.sourceConceptId) === String(os.conceptId)
    )
  } catch {
    currentRelations.value = []
  }
}

// 打开对象血缘（四维度聚合）弹窗
function openLineage() {
  const os = selectedObjectSet.value
  if (!os) return
  lineageConcept.value = os.conceptId
  lineageTableName.value = os.conceptName
  lineageTable.value = os.tableName
  lineageVisible.value = true
}

function buildColumns(os) {
  const cols = [
    { title: '#', dataIndex: 'rowIndex', key: 'rowIndex', width: 60, fixed: 'left' }
  ]
  // 语义属性优先展示，未映射物理列兜底；
  // dataIndex 始终用物理列名（后端返回原始物理列键），title 用语义属性名
  if (os.properties && os.properties.length > 0) {
    os.properties.forEach(p => {
      if (!p.physicalColumnName) return
      cols.push({
        title: p.propertyName || p.physicalColumnName,
        dataIndex: p.physicalColumnName,
        key: p.physicalColumnName,
        ellipsis: true
      })
    })
  }
  // 后端返回的表头（物理列）作为兜底，避免遗漏未映射列
  if (os.columnsCache && os.columnsCache.length > 0) {
    os.columnsCache.forEach(colName => {
      const already = cols.some(c => c.dataIndex === colName)
      if (!already) {
        cols.push({ title: colName, dataIndex: colName, key: colName, ellipsis: true })
      }
    })
  }
  // 行内“关联”操作列（关系跳转，d8 同页内嵌展开）
  cols.push({ title: '关联', dataIndex: '__relation__', key: '__relation__', width: 160, fixed: 'right' })
  // 行内“修改 / 删除”操作列（行级写入，走审批流）
  cols.push({ title: '操作', dataIndex: '__operate__', key: '__operate__', width: 130, fixed: 'right' })
  columns.value = cols
}

async function loadData() {
  if (!selectedObjectSet.value) return
  loading.value = true
  try {
    const res = await queryObjects({
      ontologyId: route.params.ontologyId,
      conceptId: selectedObjectSet.value.conceptId,
      tableBindingId: selectedObjectSet.value.tableBindingId,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      filters: JSON.stringify(filterSpec.value)
    })
    const data = res.data || {}
    const rows = data.rows || []
    // 记录行号与物理→语义映射
    rowData.value = rows.map((row, idx) => ({ ...row, __rid__: `${idx}-${pagination.current}`, __index__: idx + 1 }))
    pagination.total = data.total || 0
    if (data.columns && !selectedObjectSet.value.columnsCache) {
      selectedObjectSet.value.columnsCache = data.columns
      buildColumns(selectedObjectSet.value)
    }
  } finally {
    loading.value = false
  }
}

// 过滤/排序/投影/关键字变化时自动刷新
watch(filterSpec, () => {
  if (selectedObjectSet.value) {
    pagination.current = 1
    loadData()
  }
}, { deep: true })

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// ---------- 行操作 handlers ----------

function initRowForm(record) {
  const form = {}
  formFields.value.forEach(f => {
    let v = record ? record[f.physicalColumnName] : ''
    if (f.dataType === 'boolean') {
      v = [true, 1, '1', 'true', 'TRUE', 'Y', 'y'].includes(v)
    }
    form[f.propertyCode] = v
  })
  return form
}

// 行为级写入 → 后端 inputParams（key=属性编码）；空值剔除（UPDATE 空列不入 SET，避免误清空）
function buildRowData() {
  const data = {}
  if (rowModal.mode === 'DELETE') {
    pkFields.value.forEach(f => { data[f.propertyCode] = rowModal.form[f.propertyCode] })
    return data
  }
  formFields.value.forEach(f => {
    const v = rowModal.form[f.propertyCode]
    if (v === '' || v === undefined || v === null) return
    if (rowModal.mode === 'UPDATE' && f.isPrimary) { data[f.propertyCode] = v; return }
    data[f.propertyCode] = v
  })
  return data
}

function buildRowPayload() {
  const os = selectedObjectSet.value
  return {
    ontologyId: route.params.ontologyId,
    conceptId: os.conceptId,
    tableBindingId: os.tableBindingId,
    actionType: rowModal.mode,
    data: buildRowData(),
    triggerWebhook: rowModal.triggerWebhook,
    spaceId: userStore.spaceId ? Number(userStore.spaceId) : null
  }
}

function openRowModal(mode, record) {
  rowModal.mode = mode
  rowModal.form = initRowForm(record)
  rowModal.triggerWebhook = true
  rowModal.preview = null
  rowModal.loading = false
  rowModal.confirming = false
  rowModal.visible = true
}

function openRowCreate() {
  openRowModal('CREATE', null)
}

function openRowEdit(record) {
  openRowModal('UPDATE', record)
}

function openRowDelete(record) {
  openRowModal('DELETE', record)
}

function closeRowModal() {
  rowModal.visible = false
  rowModal.preview = null
}

// 校验主键：CREATE/UPDATE/DELETE 均需主键定位
function validatePk() {
  if (rowModal.mode === 'CREATE') return true
  const missings = pkFields.value.filter(f => {
    const v = rowModal.form[f.propertyCode]
    return v === '' || v === undefined || v === null
  })
  if (missings.length) {
    message.warning('缺少主键值（' + missings.map(f => f.propertyName).join('、') + '），无法定位记录')
    return false
  }
  return true
}

async function submitRowPreview() {
  if (!validatePk()) return
  rowModal.loading = true
  rowModal.preview = null
  try {
    const res = await rowPreview(buildRowPayload())
    const data = res.data || {}
    rowModal.preview = data
    if (data.status === 'PENDING_APPROVAL' && !data.canApprove) {
      message.warning(data.message || '已提交待审批，请前往审批中心处理本单')
    } else if (data.status === 'REJECTED') {
      message.error(data.message || '提交未通过')
    } else {
      message.success(data.message || '已提交预览，请核对后确认执行')
    }
  } catch {
    // 拦截器已统一提示
  } finally {
    rowModal.loading = false
  }
}

async function confirmRowExecute() {
  const preview = rowModal.preview
  if (!preview || !preview.executionId) return
  rowModal.confirming = true
  try {
    const res = await rowConfirm({ ...buildRowPayload(), executionId: preview.executionId })
    const data = res.data || {}
    if (data.status === 'EXECUTED') {
      message.success(data.message || '执行成功')
      rowModal.visible = false
      rowModal.preview = null
      loadData()
    } else {
      message.error(data.message || '执行失败')
      rowModal.preview = data
    }
  } catch {
    // 拦截器已统一提示
  } finally {
    rowModal.confirming = false
  }
}

// 待审批时跳转审批中心：进入本体工作台「动作管理」tab 的执行记录区
function goApprovalCenter() {
  router.push({ path: `/ont/workspace/${route.params.ontologyId}`, query: { tab: 'action' } })
}

async function loadObjectSets() {
  objectSetLoading.value = true
  try {
    const res = await listObjectSets(route.params.ontologyId)
    const list = res.data || []
    objectSets.value = list
    if (list.length > 0) {
      selectObjectSet(list[0])
    }
  } finally {
    objectSetLoading.value = false
  }
}

onMounted(() => {
  loadObjectSets()
})
</script>

<style scoped>
.ont-page-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-bottom: 12px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);

  .back-btn {
    padding-left: 0;
    color: #4e5969;

    &:hover {
      color: #1677ff;
    }
  }

  .page-title {
    font-size: 16px;
    font-weight: 600;
    color: #1f2d3d;
  }
}
.object-set-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 6px;
  border: 1px solid transparent;
  transition: all 0.2s;
}
.object-set-item:hover {
  background: #f5f5f5;
}
.object-set-item.active {
  background: #e6f4ff;
  border-color: #91caff;
}
.object-set-title {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
}
.object-set-sub {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.filter-toolbar {
  margin-bottom: 16px;
}
.filter-toolbar .ont-filter-builder {
  width: 100%;
}
.row-delete-pk {
  margin: 12px 0 4px;
  font-size: 13px;
  color: #4e5969;
}
.row-delete-label {
  font-weight: 600;
}
.row-webhook-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #999;
}
.row-sql-title {
  margin: 10px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
}
.row-sql-block {
  margin: 0 0 8px;
  padding: 10px 12px;
  max-height: 200px;
  overflow: auto;
  background: #1f2d3d;
  border-radius: 6px;
  color: #c9d1d9;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
.row-preview-result {
  margin: 8px 0 0;
  font-size: 13px;
  color: #4e5969;
  word-break: break-all;
}
.row-modal-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  align-items: center;
}
.row-footer-left {
  margin-right: auto;
}
</style>
