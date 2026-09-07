<template>
  <div class="object-panel">
    <!-- 工具条：搜索对象集（始终可见） -->
    <div class="panel-toolbar">
      <a-input-search
        v-model:value="searchValue"
        placeholder="搜索对象集（概念 / 表名）"
        allow-clear
        style="width: 240px"
      />
      <span class="toolbar-tip">对象集 = 概念 + 绑定物理表；点击左侧对象集查看其实例数据与血缘</span>
    </div>

    <a-layout class="object-layout">
      <!-- 对象集列表侧栏 -->
      <a-layout-sider :width="240" theme="light" :bordered="true">
        <a-spin :spinning="objectSetLoading">
          <div v-if="filteredObjectSets.length > 0" class="object-set-list">
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
          <a-empty v-else-if="!objectSetLoading" description="暂无对象集（概念未绑定数据表）" />
        </a-spin>
      </a-layout-sider>

      <a-layout-content class="object-content">
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
              <template v-else-if="column.key === '__relation__'">
                <template v-if="currentRelations.length">
                  <OntRelationJump
                    v-for="rel in currentRelations"
                    :key="'rel-' + rel.id"
                    :ontology-id="props.ontologyId"
                    :concept-id="selectedObjectSet.conceptId"
                    :table-binding-id="selectedObjectSet.tableBindingId"
                    :row="record"
                    :relation="rel"
                  />
                </template>
                <span v-else style="color:#bbb">无</span>
              </template>
              <template v-else-if="column.key === '__action__'">
                <a-space>
                  <template v-if="rowOperateEnabled">
                    <a-button type="link" size="small" @click="openObjectAction(record)">执行动作</a-button>
                    <a-tooltip :title="rowModifiable ? '该对象集未配置主键属性，无法定位记录' : ''">
                      <a-button type="link" size="small" :disabled="rowModifiable" @click="openRowEdit(record)">修改</a-button>
                    </a-tooltip>
                    <a-popconfirm
                      title="删除该对象实例？"
                      description="删除操作会先生成执行记录并进入确认流程"
                      ok-text="删除"
                      cancel-text="取消"
                      :ok-button-props="{ danger: true }"
                      :disabled="rowModifiable"
                      @confirm="openRowDelete(record)"
                    >
                      <a-tooltip :title="rowModifiable ? '该对象集未配置主键属性，无法定位记录' : ''">
                        <a-button type="link" size="small" danger :disabled="rowModifiable">删除</a-button>
                      </a-tooltip>
                    </a-popconfirm>
                  </template>
                  <a-button type="link" size="small" @click="openRowChanges(record)">数据变化</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
        <a-card v-else><a-empty description="请在选择一个对象集" /></a-card>
      </a-layout-content>
    </a-layout>

    <!-- 对象血缘（四维度聚合）弹窗 -->
    <ObjectLineageDialog
      v-model:visible="lineageVisible"
      :concept-id="lineageConcept"
      :concept-name="lineageTableName"
      :table-name="lineageTable"
    />

    <!-- 行级「数据变化」弹窗 -->
    <ObjectRowChangesDialog
      v-model:visible="rowChangesVisible"
      :concept-id="rowChangesConceptId"
      :concept-name="rowChangesConceptName"
      :table-name="rowChangesTable"
      :primary-key="rowChangesPk"
      :row-payload="rowChangesPayload"
    />

    <!-- 行操作弹框：新增/修改/删除 → 提交预览 → 确认执行（审批移至审批中心，不在弹框内嵌） -->
    <a-modal
      v-model:open="rowModal.visible"
      :title="rowModalTitle"
      :width="780"
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
        <a-form v-else class="ontology-form-grid object-row-form" :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }" style="margin-top: 8px">
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
          <a-alert :message="rowModal.preview.message" :type="previewAlertType" show-icon />
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
              {{ rowModal.mode === 'DELETE' ? '提交删除预览' : rowModal.mode === 'UPDATE' ? '提交修改预览' : '提交新增预览' }}
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

    <!-- 对象级人工动作：对象已由当前行确定，只需选择动作并填写真正的业务入参。 -->
    <a-modal
      v-model:open="objectAction.visible"
      title="执行对象动作"
      width="660px"
      wrap-class-name="ontology-workspace-modal ontology-modal--form"
      ok-text="确定执行"
      cancel-text="取消"
      :confirm-loading="objectAction.saving"
      :ok-button-props="{ disabled: objectAction.detailLoading || Boolean(objectActionInvalidReason) }"
      @ok="submitObjectAction"
    >
      <a-alert
        type="info"
        show-icon
        :message="'触发对象：' + (selectedObjectSet ? selectedObjectSet.conceptName : '') + ' / ' + (objectAction.objectKey || '-')"
        style="margin-bottom:12px"
      />
      <a-form class="ontology-form-grid object-action-form" :label-col="{ style: { width: '110px' } }">
        <a-form-item label="选择动作" required>
          <a-select
            v-model:value="objectAction.actionId"
            :options="objectActionOptions"
            placeholder="选择绑定到该对象类型的人工动作"
            :loading="objectAction.detailLoading"
            @change="handleObjectActionChange"
          />
        </a-form-item>
        <a-alert
          v-if="objectActionInvalidReason"
          class="ontology-form-grid__full"
          type="error"
          show-icon
          :message="objectActionInvalidReason"
          description="请先到动作管理中编辑该动作并添加执行步骤，保存后再回来执行。"
          style="margin-bottom:12px"
        />
        <div v-else-if="objectActionExecutionSteps.length" class="object-action-step-list ontology-form-grid__full">
          <div class="toolbar-tip" style="margin-bottom:6px">将按以下顺序原子执行，任一步失败都会整体回滚：</div>
          <div v-for="step in objectActionExecutionSteps" :key="step.stepNo" class="object-action-step-item">
            <a-tag color="blue">步骤 {{ step.stepNo }}</a-tag>
            <span>{{ step.name }}</span>
            <span class="toolbar-tip">{{ actionTypeLabel(step.actionType) }}</span>
          </div>
        </div>
        <a-form-item
          v-for="param in objectActionInputParams"
          :key="param.name"
          :label="param.label"
          :required="param.required"
        >
          <a-input v-model:value="objectAction.params[param.name]" :placeholder="'请输入 ' + param.name" />
        </a-form-item>
        <div v-if="objectAction.actionId && !objectActionInvalidReason && !objectActionInputParams.length" class="toolbar-tip ontology-form-grid__full">
          该动作无需人工填写参数；订单编号、产品名称、数量等占位符会从当前对象自动读取。
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="OntObjectPanel">
import { listObjectSets, queryObjects, rowPreview, rowConfirm } from '@/api/ont/objectInstance'
import { listRelation } from '@/api/ont/relation'
import { getAction, listAction, submitExecution } from '@/api/ont/action'
import ObjectLineageDialog from '@/views/ont/object/components/ObjectLineageDialog.vue'
import ObjectRowChangesDialog from '@/views/ont/object/components/ObjectRowChangesDialog.vue'
import OntFilterBuilder from '@/components/OntFilterBuilder/index.vue'
import OntRelationJump from '@/components/OntRelationJump/index.vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import useUserStore from '@/store/system/user'

const props = defineProps({
  ontologyId: {
    type: [String, Number],
    required: true
  }
})
// 待审批时通知父级（本体工作台）切到「动作管理」tab（审批中心/执行记录区）
const emit = defineEmits(['switchTab'])

const userStore = useUserStore()

// 行操作权限：与对象实例查询同权限（能进对象实例面板即可用行操作；真实写权限由后端
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

// 行级「数据变化」弹窗状态
const rowChangesVisible = ref(false)
const rowChangesConceptId = ref(null)
const rowChangesConceptName = ref('')
const rowChangesTable = ref('')
const rowChangesPk = ref(null)
const rowChangesPayload = ref(null)

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

const objectActionList = ref([])
const objectAction = reactive({
  visible: false,
  saving: false,
  detailLoading: false,
  record: null,
  objectKey: '',
  actionId: undefined,
  params: {}
})

const objectActionOptions = computed(() => objectActionList.value.map(action => ({
  value: action.id,
  label: `${action.name}（${{ CREATE: '新建', UPDATE: '更新', DELETE: '删除', COMPOSITE: '多目标动作', FUNCTION: '函数' }[action.actionType] || action.actionType}）`
})))

function parseActionParamConfig(action) {
  if (action?.actionType === 'COMPOSITE') {
    const steps = parseJsonArray(action.executionSteps ?? action.execution_steps)
    return steps.flatMap(step => parseJsonArray(step.paramConfig ?? step.param_config))
  }
  return parseJsonArray(action?.paramConfig ?? action?.param_config)
}

function parseJsonArray(value) {
  if (Array.isArray(value)) return value
  if (!value) return []
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    if (Array.isArray(parsed)) return parsed
    if (typeof parsed === 'string' && parsed !== value) return parseJsonArray(parsed)
    return []
  } catch {
    return []
  }
}

function placeholderName(template) {
  return String(template || '').trim().replace(/^\$\{/, '').replace(/\}$/, '').trim()
}

function objectRecordValue(record, property) {
  if (!record || !property) return undefined
  const candidates = [property.physicalColumnName, property.propertyCode].filter(Boolean)
  for (const candidate of candidates) {
    if (Object.prototype.hasOwnProperty.call(record, candidate)) return record[candidate]
    const actualKey = Object.keys(record).find(key => String(key).toLowerCase() === String(candidate).toLowerCase())
    if (actualKey !== undefined) return record[actualKey]
  }
  return undefined
}

const currentObjectAction = computed(() =>
  objectActionList.value.find(action => String(action.id) === String(objectAction.actionId))
)

const objectActionExecutionSteps = computed(() => {
  const action = currentObjectAction.value
  if (!action || action.actionType !== 'COMPOSITE') return []
  return parseJsonArray(action.executionSteps ?? action.execution_steps).map((step, index) => ({
    ...step,
    stepNo: step.stepNo || index + 1,
    name: step.name || `步骤 ${index + 1}`
  }))
})

const objectActionInvalidReason = computed(() => {
  const action = currentObjectAction.value
  if (!action || action.actionType !== 'COMPOSITE') return ''
  return objectActionExecutionSteps.value.length ? '' : '该多目标动作未配置执行步骤，当前不能执行'
})

function actionTypeLabel(type) {
  return { CREATE: '新建', UPDATE: '更新', DELETE: '删除' }[type] || type || '-'
}

const objectActionInputParams = computed(() => {
  const seen = new Set()
  const properties = selectedObjectSet.value?.properties || []
  return parseActionParamConfig(currentObjectAction.value)
    .filter(cfg => !cfg.condition && (cfg.valueMode === 'placeholder'
      || (cfg.valueMode === 'relative' && /^\$\{[^}]+\}$/.test(String(cfg.valueTemplate || '').trim()))))
    .filter(cfg => {
      const name = placeholderName(cfg.valueTemplate)
      return !properties.some(property => property.propertyCode === name)
    })
    .map(cfg => ({
      name: placeholderName(cfg.valueTemplate),
      label: ((selectedObjectSet.value?.properties || []).find(p => p.propertyCode === cfg.propertyCode)?.propertyName
        || cfg.propertyCode) + (cfg.valueMode === 'relative' ? '（运算量）' : ''),
      required: !!cfg.required || cfg.valueMode === 'relative'
    }))
    .filter(param => param.name && !seen.has(param.name) && seen.add(param.name))
})

function selectedRowObjectKey(record) {
  const pks = pkFields.value
  if (!pks.length) return ''
  if (pks.length === 1) {
    const value = objectRecordValue(record, pks[0])
    return value === undefined || value === null ? '' : String(value)
  }
  const key = {}
  pks.forEach(p => { key[p.propertyCode || p.physicalColumnName] = objectRecordValue(record, p) })
  return JSON.stringify(key)
}

async function openObjectAction(record) {
  const os = selectedObjectSet.value
  if (!os) return
  const objectKey = selectedRowObjectKey(record)
  if (!objectKey) {
    message.warning('该对象集没有可用的主键，无法绑定动作执行记录')
    return
  }
  objectAction.record = record
  objectAction.objectKey = objectKey
  objectAction.actionId = undefined
  objectAction.params = {}
  objectAction.visible = true
  const res = await listAction({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 })
  const rows = res.data?.rows || []
  objectActionList.value = rows.filter(action =>
    String(action.conceptId) === String(os.conceptId)
      && action.actionType !== 'CREATE'
  )
}

function resetObjectActionParams() {
  objectAction.params = {}
}

async function handleObjectActionChange(actionId) {
  resetObjectActionParams()
  if (!actionId) return
  objectAction.detailLoading = true
  try {
    const detail = (await getAction(actionId)).data || {}
    const index = objectActionList.value.findIndex(action => String(action.id) === String(actionId))
    if (index >= 0) {
      objectActionList.value.splice(index, 1, { ...objectActionList.value[index], ...detail })
    }
  } finally {
    objectAction.detailLoading = false
  }
}

function buildObjectActionParams() {
  const params = { ...objectAction.params }
  const properties = selectedObjectSet.value?.properties || []
  parseActionParamConfig(currentObjectAction.value).forEach(cfg => {
    const name = placeholderName(cfg.valueTemplate)
    const isPlaceholder = cfg.valueMode === 'placeholder'
      || (cfg.valueMode === 'relative' && /^\$\{[^}]+\}$/.test(String(cfg.valueTemplate || '').trim()))
    if (!isPlaceholder) return
    const property = properties.find(p => p.propertyCode === name)
    if (name && property) {
      params[name] = objectRecordValue(objectAction.record, property)
    }
  })
  return params
}

async function submitObjectAction() {
  const action = currentObjectAction.value
  if (!action) {
    message.warning('请选择动作')
    return
  }
  if (objectActionInvalidReason.value) {
    message.warning(objectActionInvalidReason.value)
    return
  }
  const missing = objectActionInputParams.value.filter(p => p.required
    && (objectAction.params[p.name] === undefined || objectAction.params[p.name] === null || objectAction.params[p.name] === ''))
  if (missing.length) {
    message.warning('缺少必填参数：' + missing.map(p => p.label).join('、'))
    return
  }
  objectAction.saving = true
  try {
    const res = await submitExecution({
      actionId: action.id,
      objectKey: objectAction.objectKey,
      inputParams: JSON.stringify(buildObjectActionParams()),
      triggerType: 'MANUAL'
    })
    const record = res.data || {}
    message.success(record.status === 'PENDING_APPROVAL' ? '已提交，等待人工确认' : '已提交执行')
    objectAction.visible = false
    emit('switchTab', 'action')
  } finally {
    objectAction.saving = false
  }
}

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

// 主键字段（DELETE 定位 / UPDATE 只读）
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
    const res = await listRelation({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 200 })
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

// 打开行级「数据变化」弹窗：按主键值匹配该对象的版本快照
function openRowChanges(record) {
  const os = selectedObjectSet.value
  if (!os || !record) return
  // 主键 = 属性绑定中标为主键的物理列
  const pkCols = (os.properties || []).filter(p => p.isPrimary && p.physicalColumnName).map(p => p.physicalColumnName)
  if (pkCols.length === 0) {
    rowChangesPk.value = null
  } else {
    const pk = {}
    pkCols.forEach(c => {
      if (record[c] !== undefined && record[c] !== null) pk[c] = record[c]
    })
    rowChangesPk.value = Object.keys(pk).length ? pk : null
  }
  rowChangesConceptId.value = os.conceptId
  rowChangesConceptName.value = os.conceptName
  rowChangesTable.value = os.tableName
  rowChangesPayload.value = record
  rowChangesVisible.value = true
}

function buildColumns(os) {
  const cols = [
    { title: '#', dataIndex: 'rowIndex', key: 'rowIndex', width: 60, fixed: 'left' }
  ]
  // 语义属性优先展示，未映射物理列兜底；dataIndex 始终用物理列名，title 用语义属性名
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
  // 后端返回的表头（物理列）作为兜底
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
  // 行级「数据变化」操作列
  cols.push({ title: '操作', dataIndex: '__action__', key: '__action__', width: 240, fixed: 'right' })
  columns.value = cols
}

async function loadData() {
  if (!selectedObjectSet.value) return
  loading.value = true
  try {
    const res = await queryObjects({
      ontologyId: props.ontologyId,
      conceptId: selectedObjectSet.value.conceptId,
      tableBindingId: selectedObjectSet.value.tableBindingId,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      filters: JSON.stringify(filterSpec.value)
    })
    const data = res.data || {}
    const rows = data.rows || []
    rowData.value = rows.map((row, idx) => ({ ...row, __rid__: `${idx}-${pagination.current}`, __index__: idx + 1 }))
    pagination.total = Number(data.total) || 0
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
    ontologyId: props.ontologyId,
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

// 待审批时跳转审批中心：通知父级（本体工作台）切到「动作管理」tab 的执行记录区
function goApprovalCenter() {
  emit('switchTab', 'action')
}

async function loadObjectSets() {
  objectSetLoading.value = true
  try {
    const res = await listObjectSets(props.ontologyId)
    const list = res.data || []
    objectSets.value = list
    if (list.length > 0) {
      selectObjectSet(list[0])
    }
  } finally {
    objectSetLoading.value = false
  }
}

// 面板懒挂载时初始化一次
onMounted(() => {
  loadObjectSets()
})

// 兼容工作台 dirty 机制：needReload 为 true 时重新加载（图谱变更后对象集可能变化）
defineExpose({
  reload: loadObjectSets
})
</script>

<style lang="scss" scoped>
.object-panel {
  .panel-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    .toolbar-tip {
      font-size: 12px;
      color: #86909c;
    }
  }
}

.object-layout {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;

  .object-set-list {
    padding: 8px;
    max-height: 560px;
    overflow: auto;
  }

  .object-set-item {
    padding: 10px 12px;
    border-radius: 6px;
    cursor: pointer;
    margin-bottom: 6px;
    border: 1px solid transparent;
    transition: all 0.2s;

    &:hover {
      background: #f5f5f5;
    }

    &.active {
      background: #e6f4ff;
      border-color: #91caff;
    }
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
}

.object-content {
  padding: 0 12px;
  min-width: 0;
}

.filter-toolbar {
  margin-bottom: 16px;

  .ont-filter-builder {
    width: 100%;
  }
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
  display: inline-block;
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
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  align-items: center;
}
.object-action-step-list {
  margin: 0 0 12px 110px;
  padding: 8px 10px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #fafafa;
}
.object-action-step-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  font-size: 13px;
}
.row-footer-left {
  margin-right: auto;
}

@media (max-width: 768px) {
  .object-action-step-list {
    margin-left: 0;
  }

  .row-webhook-hint {
    display: block;
    margin-top: 6px;
    margin-left: 0;
  }

  .row-footer-left {
    width: 100%;
    margin-right: 0;
  }
}
</style>
