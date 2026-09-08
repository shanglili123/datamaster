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
                    :key="'rel-' + rel.id + '-' + relationViewVersion"
                    :ontology-id="props.ontologyId"
                    :concept-id="selectedObjectSet.conceptId"
                    :table-binding-id="selectedObjectSet.tableBindingId"
                    :row="record"
                    :relation="rel"
                    :editable="rowOperateEnabled"
                    @add="openRelationCreate(record, rel)"
                    @remove="targetRecord => openRelationDelete(record, rel, targetRecord)"
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
      <a-spin :spinning="rowModal.loading || rowModal.confirming || relationCreateLoading">
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
          <template v-for="f in displayedFormFields" :key="'f-' + f.propertyCode">
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

        <template v-if="rowModal.mode === 'CREATE' && relationCreateFields.length">
          <a-divider style="margin: 12px 0">关联对象</a-divider>
          <a-form class="ontology-form-grid" :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }">
            <a-form-item v-for="field in relationCreateFields" :key="field.relationId" :label="field.label">
              <a-select
                v-model:value="rowModal.relationValues[field.relationId]"
                :options="field.options"
                :loading="field.loading"
                :placeholder="'选择' + field.targetConceptName"
                show-search
                option-filter-prop="label"
                allow-clear
              />
              <div class="row-relation-hint">选择后写入当前对象的关联字段 {{ field.sourceColumn }}；不会重复新增关联对象。</div>
            </a-form-item>
          </a-form>
        </template>

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

    <!-- 关系管理：只新增或删除关系，不新增/删除两端对象。 -->
    <a-modal
      v-model:open="relationManage.visible"
      :title="relationManage.operation === 'DELETE' ? '删除关联' : '新增关联'"
      width="680px"
      wrap-class-name="ontology-workspace-modal ontology-modal--form"
      :footer="null"
      :mask-closable="false"
      destroy-on-close
      @cancel="closeRelationManage"
    >
      <a-spin :spinning="relationManage.loading || relationManage.confirming">
        <a-alert
          type="info"
          show-icon
          :message="relationManage.operation === 'DELETE' ? '只删除这条关系' : '只新增一条关系'"
          description="不会新增或删除关系两端的对象数据。"
          style="margin-bottom: 12px"
        />
        <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
          <a-form-item label="当前对象">
            <a-input :value="relationManage.currentLabel" disabled />
          </a-form-item>
          <a-form-item label="关系">
            <a-input :value="relationManage.relationName" disabled />
          </a-form-item>
          <a-form-item :label="relationManage.otherConceptName || '关联对象'" required>
            <a-select
              v-if="relationManage.operation === 'CREATE'"
              v-model:value="relationManage.selectedValue"
              :options="relationManage.options"
              :loading="relationManage.optionLoading"
              :placeholder="'选择已有' + (relationManage.otherConceptName || '关联对象')"
              show-search
              option-filter-prop="label"
              allow-clear
              @change="resetRelationPreview"
            />
            <a-input v-else :value="relationManage.selectedLabel" disabled />
            <div v-if="relationManage.operation === 'CREATE'" class="row-relation-hint">优先显示名称类属性；实际执行提交关系字段映射对应的值。</div>
          </a-form-item>
        </a-form>

        <a-alert
          v-if="relationManage.error"
          type="error"
          show-icon
          :message="relationManage.error"
          style="margin-bottom: 12px"
        />
        <template v-if="relationManage.preview">
          <a-divider style="margin: 12px 0">预览与执行</a-divider>
          <a-alert :message="relationManage.preview.message" :type="relationPreviewAlertType" show-icon />
          <template v-if="relationManage.preview.generatedSql">
            <div class="row-sql-title">执行内容预览</div>
            <pre class="row-sql-block">{{ relationManage.preview.generatedSql }}</pre>
          </template>
          <p v-if="relationManage.preview.previewResult" class="row-preview-result">
            预览结果：{{ relationManage.preview.previewResult }}
          </p>
        </template>

        <div class="row-modal-footer">
          <div class="row-footer-left">
            <a-button
              v-if="relationManage.preview?.status === 'PENDING_APPROVAL'"
              type="link"
              @click="goApprovalCenter"
            >已提交待审批，前往审批中心</a-button>
          </div>
          <a-button @click="closeRelationManage">关闭</a-button>
          <a-button
            v-if="relationManage.preview"
            :loading="relationManage.loading"
            @click="submitRelationPreview"
          >重新预览</a-button>
          <a-button
            v-if="!relationManage.preview"
            type="primary"
            :loading="relationManage.loading"
            :disabled="Boolean(relationManage.error)"
            @click="submitRelationPreview"
          >提交预览</a-button>
          <a-button
            v-if="relationPreviewCanExecute"
            type="primary"
            :loading="relationManage.confirming"
            @click="confirmRelationExecute"
          >确认执行</a-button>
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
          <a-select
            v-if="isObjectActionRelationParam(param)"
            :value="param.referencePropertyCode ? objectActionRelationState(param).selectedValue : objectAction.params[param.name]"
            :options="objectActionRelationState(param).options"
            :loading="objectActionRelationState(param).loading"
            :placeholder="'搜索并选择' + param.label"
            show-search
            allow-clear
            :filter-option="false"
            @search="keyword => searchObjectActionRelationObjects(param, keyword)"
            @dropdown-visible-change="open => open && searchObjectActionRelationObjects(param, '')"
            @change="value => handleObjectActionRelationChange(param, value)"
          />
          <a-input v-else v-model:value="objectAction.params[param.name]" :placeholder="'请输入 ' + param.name" />
          <div v-if="objectActionRelationState(param).error" class="modal-hint-line" style="color:#ff4d4f;">
            {{ objectActionRelationState(param).error }}
          </div>
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
import { previewConceptTable } from '@/api/ont/conceptTable'
import { listRelation, getRelation } from '@/api/ont/relation'
import { listRelationColumn } from '@/api/ont/relationColumn'
import { listRelationTable } from '@/api/ont/relationTable'
import { getAction, listAction, submitExecution, runExecution } from '@/api/ont/action'
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
const relationViewVersion = ref(0)

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
  relationValues: {}, // CREATE 直接外键关系：relationId -> 目标对象关联值
  triggerWebhook: true,
  preview: null // RowOperateRespVO
})

const relationCreateFields = ref([])
const relationCreateLoading = ref(false)

const relationManage = reactive({
  visible: false,
  loading: false,
  confirming: false,
  optionLoading: false,
  record: null,
  relationId: undefined,
  relationName: '',
  operation: 'CREATE',
  currentEndpoint: 'SOURCE',
  currentLabel: '',
  currentRelationValue: undefined,
  otherConceptName: '',
  selectedValue: undefined,
  selectedLabel: '',
  options: [],
  error: '',
  preview: null
})

const relationPreviewCanExecute = computed(() => relationManage.preview?.status === 'APPROVED')
const relationPreviewAlertType = computed(() => {
  const status = relationManage.preview?.status
  if (status === 'APPROVED' || status === 'EXECUTED') return 'success'
  if (status === 'PENDING_APPROVAL') return 'warning'
  return status ? 'error' : 'info'
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

// 对象动作中的关系端点参数（例如就职动作的公司）使用目标对象下拉，
// 不把关系表物理列名 company_id 暴露成手填输入框。
const objectActionRelationStates = reactive({})
const objectActionRelationMetaByParam = reactive({})
let objectActionAllObjectSets = []
const objectActionRelationSearchTimers = new Map()
const emptyObjectActionRelationState = { options: [], loading: false, error: '' }

const objectActionOptions = computed(() => objectActionList.value.map(action => ({
  value: action.id,
  label: `${action.name}（${{ CREATE: '新建', UPDATE: '更新', DELETE: '删除', COMPOSITE: '多目标动作', FUNCTION: '函数' }[action.actionType] || action.actionType}）`
})))

function parseActionParamConfig(action) {
  const normalize = config => {
    const match = /^\$\{object\.([^.}]+)\.([^}]+)\}$/.exec(String(config?.valueTemplate || '').trim())
    if (match && config.valueMode === 'relative' && !config.targetRelationId) {
      const relation = currentRelations.value.find(item => item.code === match[1])
      if (relation) {
        return {
          ...config,
          targetRelationId: relation.id,
          relationEndpoint: 'TARGET',
          referenceRelationCode: match[1],
          referencePropertyCode: match[2]
        }
      }
    }
    return config
  }
  if (action?.actionType === 'COMPOSITE') {
    const steps = parseJsonArray(action.executionSteps ?? action.execution_steps)
    return steps.flatMap(step => parseJsonArray(step.paramConfig ?? step.param_config).map(config => normalize({
      ...config,
      targetRelationId: config.targetRelationId || config.target_relation_id || step.relationId || step.relation_id
    })))
  }
  return parseJsonArray(action?.paramConfig ?? action?.param_config).map(normalize)
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

function triggerObjectPropertyName(template) {
  const match = /^\$\{object\.([^.}]+)\}$/.exec(String(template || '').trim())
  return match ? match[1] : ''
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

const objectActionAllInputParams = computed(() => {
  const seen = new Set()
  const properties = selectedObjectSet.value?.properties || []
  return parseActionParamConfig(currentObjectAction.value)
    .filter(cfg => (!cfg.condition || String(cfg.conditionValueSource || '').toUpperCase() === 'PARAM') && (cfg.valueMode === 'placeholder'
      || (cfg.valueMode === 'relative' && /^\$\{[^}]+\}$/.test(String(cfg.valueTemplate || '').trim()))))
    .filter(cfg => {
      const name = placeholderName(cfg.valueTemplate)
      const objectProperty = triggerObjectPropertyName(cfg.valueTemplate)
      return !objectProperty && !properties.some(property => property.propertyCode === name)
    })
    .map(cfg => {
      const name = placeholderName(cfg.valueTemplate)
      const relation = objectActionRelationMetaByParam[name] || resolveObjectActionRelationMeta(cfg)
      return {
        ...cfg,
        name,
        label: relation?.conceptName
          || ((selectedObjectSet.value?.properties || []).find(p => p.propertyCode === cfg.propertyCode)?.propertyName
            || cfg.propertyCode) + (cfg.valueMode === 'relative' ? '（运算量）' : ''),
        required: !!cfg.required || cfg.valueMode === 'relative',
        relationMeta: relation
      }
    })
    .filter(param => param.name && !seen.has(param.name) && seen.add(param.name))
})

function isCurrentObjectRelationParam(param) {
  const conceptId = param?.relationMeta?.conceptId
  return !!conceptId && String(conceptId) === String(selectedObjectSet.value?.conceptId)
}

// 关系主体已经由当前对象行确定，不再重复让用户选择；只展示另一端对象和关系属性参数。
const objectActionInputParams = computed(() =>
  objectActionAllInputParams.value.filter(param => !isCurrentObjectRelationParam(param))
)

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
  Object.keys(objectActionRelationStates).forEach(key => delete objectActionRelationStates[key])
  Object.keys(objectActionRelationMetaByParam).forEach(key => delete objectActionRelationMetaByParam[key])
  objectActionAllObjectSets = []
  const res = await listAction({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 })
  const rows = res.data?.rows || []
  objectActionList.value = rows.filter(action =>
    String(action.conceptId) === String(os.conceptId)
      && action.actionType !== 'CREATE'
      && !String(action.name || '').startsWith('内置-')
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
    await prepareObjectActionRelationSelectors()
  } finally {
    objectAction.detailLoading = false
  }
}

function normalizeObjectSetsResponse(res) {
  if (Array.isArray(res?.data)) return res.data
  return res?.data?.rows || []
}

function relationConfigEndpoint(cfg, mapping, relationTableConfig) {
  const explicit = String(cfg?.relationEndpoint || '').toUpperCase()
  if (explicit === 'SOURCE' || explicit === 'TARGET') return explicit
  const code = String(cfg?.propertyCode || '').toLowerCase()
  if (relationTableConfig && code && String(relationTableConfig.sourceColumn || '').toLowerCase() === code) return 'SOURCE'
  if (relationTableConfig && code && String(relationTableConfig.targetColumn || '').toLowerCase() === code) return 'TARGET'
  return ''
}

function parseRelationTableConfig(value) {
  if (!value) return { sourceColumn: '', targetColumn: '' }
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    if (Array.isArray(parsed)) return { sourceColumn: parsed[0] || '', targetColumn: parsed[1] || '' }
    return { sourceColumn: parsed?.sourceColumn || '', targetColumn: parsed?.targetColumn || '' }
  } catch {
    return { sourceColumn: '', targetColumn: '' }
  }
}

function resolveObjectActionRelationMeta(cfg) {
  if (!cfg?.targetRelationId) return null
  const relation = currentRelations.value.find(item => String(item.id) === String(cfg.targetRelationId))
  const endpoint = String(cfg.relationEndpoint || '').toUpperCase()
  const conceptId = endpoint === 'SOURCE' ? relation?.sourceConceptId
    : endpoint === 'TARGET' ? relation?.targetConceptId : undefined
  const set = objectActionAllObjectSets.find(item => String(item.conceptId) === String(conceptId))
  return {
    relationId: cfg.targetRelationId,
    endpoint,
    conceptId,
    conceptName: set?.conceptName || (endpoint === 'TARGET' ? '目标对象' : endpoint === 'SOURCE' ? '主体对象' : ''),
    tableBindingId: set?.tableBindingId
  }
}

function objectActionRelationState(param) {
  return objectActionRelationStates[param.name] || emptyObjectActionRelationState
}

function isObjectActionRelationParam(param) {
  return !!param?.relationMeta?.relationId
}

function handleObjectActionRelationChange(param, value) {
  const state = objectActionRelationState(param)
  if (!param.referencePropertyCode) {
    objectAction.params[param.name] = value
    return
  }
  state.selectedValue = value
  const option = (state.options || []).find(item => String(item.value) === String(value))
  if (!option?.record) return
  const objectSet = objectActionAllObjectSets.find(item =>
    String(item.conceptId) === String(param.relationMeta.conceptId)
      && String(item.tableBindingId) === String(param.relationMeta.tableBindingId))
  const property = (objectSet?.properties || []).find(item =>
    item.propertyCode === param.referencePropertyCode || item.physicalColumnName === param.referencePropertyCode)
  const resolved = property
    ? objectRecordValue(option.record, property)
    : objectRecordValue(option.record, { physicalColumnName: param.referencePropertyCode, propertyCode: param.referencePropertyCode })
  if (resolved !== undefined && resolved !== null) objectAction.params[param.name] = resolved
}

function objectActionRelationOptionLabel(objectSet, record, objectKey) {
  const properties = objectSet?.properties || []
  const displayProperty = properties.find(property => !property.isPrimary
    && /(name|title|label|名称|全称|简称)/i.test(`${property.propertyCode || ''} ${property.propertyName || ''}`))
    || properties.find(property => !property.isPrimary
      && /(code|编码)/i.test(`${property.propertyCode || ''} ${property.propertyName || ''}`))
    || properties.find(property => !property.isPrimary)
  const displayValue = displayProperty ? objectRecordValue(record, displayProperty) : undefined
  return displayValue !== undefined && displayValue !== null && String(displayValue) !== String(objectKey)
    ? `${displayValue}（${objectKey}）` : String(objectKey)
}

async function loadObjectActionRelationObjects(param, keyword = '') {
  if (!isObjectActionRelationParam(param)) return
  const state = objectActionRelationStates[param.name]
    || (objectActionRelationStates[param.name] = { options: [], loading: false, error: '', requestNo: 0 })
  const requestNo = ++state.requestNo
  state.loading = true
  state.error = ''
  try {
    if (!objectActionAllObjectSets.length) {
      objectActionAllObjectSets = normalizeObjectSetsResponse(await listObjectSets(props.ontologyId))
    }
    let relation = currentRelations.value.find(item => String(item.id) === String(param.relationMeta.relationId))
    if (!relation) relation = (await getRelation(param.relationMeta.relationId).catch(() => ({ data: null }))).data
    if (!relation) {
      // 关系详情接口比动作详情更准确；失败时保留已有关系列表结果。
      const relationRows = currentRelations.value || []
      relation = relationRows.find(item => String(item.id) === String(param.relationMeta.relationId))
    }
    if (!relation) {
      state.error = '未找到动作绑定的关系定义'
      return
    }
    const [columnRes, tableRes] = await Promise.all([
      listRelationColumn(param.relationMeta.relationId),
      listRelationTable(param.relationMeta.relationId)
    ])
    const mappings = Array.isArray(columnRes?.data) ? columnRes.data : (columnRes?.data?.rows || [])
    const mapping = mappings.find(item => String(item.sourceConceptTableId) === String(selectedObjectSet.value?.tableBindingId)) || mappings[0]
    const tables = Array.isArray(tableRes?.data) ? tableRes.data : (tableRes?.data?.rows || [])
    const relationTableConfig = parseRelationTableConfig(tables[0]?.columnNames)
    const endpoint = relationConfigEndpoint(param, mapping, relationTableConfig)
    if (!endpoint || !mapping) {
      state.error = '关系字段映射未配置主体/客体端点'
      return
    }
    const conceptId = endpoint === 'SOURCE' ? relation.sourceConceptId : relation.targetConceptId
    const tableBindingId = endpoint === 'SOURCE' ? mapping.sourceConceptTableId : mapping.targetConceptTableId
    const referenceColumn = endpoint === 'SOURCE' ? mapping.sourceColumn : mapping.targetColumn
    const objectSet = objectActionAllObjectSets.find(item => String(item.tableBindingId) === String(tableBindingId))
      || objectActionAllObjectSets.find(item => String(item.conceptId) === String(conceptId))
    if (!objectSet || !referenceColumn) {
      state.error = '未找到关系端点对应的对象表绑定'
      return
    }
    objectActionRelationMetaByParam[param.name] = {
      relationId: param.relationMeta.relationId,
      endpoint,
      conceptId,
      conceptName: objectSet.conceptName || (endpoint === 'TARGET' ? '目标对象' : '主体对象'),
      tableBindingId
    }
    // 当前触发对象就是关系的这一端：直接读取当前行中关系映射指定的物理字段（例如人物.id），
    // 不展示下拉，也不能使用概念展示主属性（例如 person_code=P1003）替代 bigint 外键。
    if (String(conceptId) === String(selectedObjectSet.value?.conceptId)) {
      const currentValue = objectRecordValue(objectAction.record, {
        physicalColumnName: referenceColumn,
        propertyCode: referenceColumn
      })
      if (currentValue === undefined || currentValue === null || currentValue === '') {
        state.error = `当前对象缺少关系关联字段 ${referenceColumn}`
        return
      }
      objectAction.params[param.name] = currentValue
      state.options = []
      return
    }
    const filterSpec = { groups: [], orderBy: [], columns: [], keyword: keyword || '' }
    let rows = []
    try {
      const res = await queryObjects({
        ontologyId: props.ontologyId,
        conceptId: objectSet.conceptId,
        tableBindingId: objectSet.tableBindingId,
        pageNum: 1,
        pageSize: 30,
        filters: JSON.stringify(filterSpec)
      })
      rows = res.data?.rows || []
    } catch (queryError) {
      const previewRes = await previewConceptTable(objectSet.tableBindingId, 30, filterSpec)
      rows = previewRes.data?.rows || []
    }
    if (!rows.length) {
      try {
        const previewRes = await previewConceptTable(objectSet.tableBindingId, 30, filterSpec)
        rows = previewRes.data?.rows || []
      } catch (previewError) {
        // 主查询已返回空结果时，保留空列表并在下方给出可读提示。
      }
    }
    if (requestNo !== state.requestNo) return
    state.options = rows.map(record => {
      const value = objectRecordValue(record, { physicalColumnName: referenceColumn, propertyCode: referenceColumn })
      const key = value === undefined || value === null ? '' : String(value)
      return { value: key, label: objectActionRelationOptionLabel(objectSet, record, key), record, disabled: !key }
    }).filter(option => option.value)
    if (!state.options.length) state.error = `未从 ${objectSet.tableName || '目标表'} 查询到可引用实体`
  } catch (e) {
    if (requestNo === state.requestNo) state.error = e?.response?.data?.msg || e?.message || '关联对象加载失败'
  } finally {
    if (requestNo === state.requestNo) state.loading = false
  }
}

function searchObjectActionRelationObjects(param, keyword) {
  const oldTimer = objectActionRelationSearchTimers.get(param.name)
  if (oldTimer) clearTimeout(oldTimer)
  const timer = setTimeout(() => loadObjectActionRelationObjects(param, keyword).catch(() => {}), 250)
  objectActionRelationSearchTimers.set(param.name, timer)
}

async function prepareObjectActionRelationSelectors() {
  Object.keys(objectActionRelationStates).forEach(key => delete objectActionRelationStates[key])
  Object.keys(objectActionRelationMetaByParam).forEach(key => delete objectActionRelationMetaByParam[key])
  objectActionAllObjectSets = normalizeObjectSetsResponse(await listObjectSets(props.ontologyId).catch(() => ({ data: [] })))
  const configs = objectActionAllInputParams.value.filter(isObjectActionRelationParam)
  for (const param of configs) {
    objectActionRelationStates[param.name] = { options: [], loading: false, error: '', requestNo: 0, selectedValue: undefined }
    loadObjectActionRelationObjects(param).catch(() => {})
  }
}

function buildObjectActionParams() {
  const params = { ...objectAction.params }
  const properties = selectedObjectSet.value?.properties || []
  parseActionParamConfig(currentObjectAction.value).forEach(cfg => {
    const name = placeholderName(cfg.valueTemplate)
    const objectProperty = triggerObjectPropertyName(cfg.valueTemplate)
    const isPlaceholder = cfg.valueMode === 'placeholder'
      || (cfg.valueMode === 'relative' && /^\$\{[^}]+\}$/.test(String(cfg.valueTemplate || '').trim()))
    if (!isPlaceholder) return
    const property = properties.find(p => p.propertyCode === (objectProperty || name))
    if (name && property) {
      params[objectProperty ? `object.${objectProperty}` : name] = objectRecordValue(objectAction.record, property)
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
    let record = res.data || {}
    // MANUAL 且无需审批时，提交接口只会把记录置为 APPROVED；对象动作入口必须继续调用 run，
    // 否则用户还要去动作管理页再点一次“执行”。需要审批的动作仍停在 PENDING_APPROVAL。
    if (record.status === 'APPROVED' && record.id
      && (!record.triggerType || record.triggerType === 'MANUAL')) {
      const runRes = await runExecution(record.id)
      record = runRes.data || record
    }
    if (record.status === 'PENDING_APPROVAL') {
      message.success('已提交，等待人工确认')
    } else if (record.status === 'EXECUTED') {
      message.success('执行完成')
    } else if (record.status === 'RUNNING' || record.autoExecute) {
      message.success('已提交，正在执行')
    } else {
      message.success('已提交')
    }
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

const displayedFormFields = computed(() => {
  if (rowModal.mode !== 'CREATE' || !relationCreateFields.value.length) return formFields.value
  const relationPropertyCodes = new Set(relationCreateFields.value.map(field => field.sourcePropertyCode))
  return formFields.value.filter(field => !relationPropertyCodes.has(field.propertyCode))
})

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
    const rows = Array.isArray(res.data) ? res.data : ((res.data && res.data.rows) || [])
    currentRelations.value = rows.filter(r =>
      String(r.sourceConceptId) === String(os.conceptId)
    )
  } catch {
    currentRelations.value = []
  }
}

function resetRelationPreview() {
  relationManage.preview = null
}

function relationPrimaryProperty(objectSet) {
  return (objectSet?.properties || []).find(property => property.isPrimary && property.physicalColumnName)
}

function sameRelationPhysicalTable(relationTable, objectSet) {
  if (!relationTable || !objectSet
    || String(relationTable.datasourceId) !== String(objectSet.datasourceId)
    || String(relationTable.tableName || '').toLowerCase() !== String(objectSet.tableName || '').toLowerCase()) return false
  if (relationTable.databaseName && objectSet.databaseName
    && String(relationTable.databaseName).toLowerCase() !== String(objectSet.databaseName).toLowerCase()) return false
  return !relationTable.schemaName || !objectSet.schemaName
    || String(relationTable.schemaName).toLowerCase() === String(objectSet.schemaName).toLowerCase()
}

function relationOwnerEndpoint(relation, mapping, sourceSet, targetSet) {
  if (String(relation.relationType).toLowerCase() === 'one_to_many') return 'TARGET'
  if (String(relation.relationType).toLowerCase() === 'many_to_one') return 'SOURCE'
  const sourcePrimary = relationPrimaryProperty(sourceSet)
  const targetPrimary = relationPrimaryProperty(targetSet)
  const sourceIsPrimary = sourcePrimary
    && String(sourcePrimary.physicalColumnName).toLowerCase() === String(mapping.sourceColumn || '').toLowerCase()
  const targetIsPrimary = targetPrimary
    && String(targetPrimary.physicalColumnName).toLowerCase() === String(mapping.targetColumn || '').toLowerCase()
  if (sourceIsPrimary && !targetIsPrimary) return 'TARGET'
  if (targetIsPrimary && !sourceIsPrimary) return 'SOURCE'
  return ''
}

async function prepareRelationManage(record, relation, operation) {
  const currentPrimary = relationPrimaryProperty(selectedObjectSet.value)
  const currentValue = objectRecordValue(record, currentPrimary)
  if (!currentPrimary || currentValue === undefined || currentValue === null || currentValue === '') {
    message.warning('当前对象没有可用的主属性，无法管理关系')
    return null
  }
  const [columnRes, tableRes] = await Promise.all([
    listRelationColumn(relation.id),
    listRelationTable(relation.id)
  ])
  const mappings = Array.isArray(columnRes.data) ? columnRes.data : (columnRes.data?.rows || [])
  const mapping = mappings.find(item =>
    String(item.sourceConceptTableId) === String(selectedObjectSet.value.tableBindingId)) || mappings[0]
  if (!mapping?.sourceColumn || !mapping?.targetColumn) {
    message.warning('关系尚未配置完整的主体、客体字段映射')
    return null
  }
  const sourceSet = objectSets.value.find(item =>
    String(item.tableBindingId) === String(mapping.sourceConceptTableId)) || selectedObjectSet.value
  const otherSet = objectSets.value.find(item =>
    String(item.tableBindingId) === String(mapping.targetConceptTableId))
    || objectSets.value.find(item => String(item.conceptId) === String(relation.targetConceptId))
  const otherPrimary = relationPrimaryProperty(otherSet)
  if (!otherSet || !otherPrimary) {
    message.warning('关系目标对象尚未绑定物理表或设置主属性')
    return null
  }
  const relationTables = Array.isArray(tableRes.data) ? tableRes.data : (tableRes.data?.rows || [])
  const relationTable = relationTables[0]
  const tableIsSource = sameRelationPhysicalTable(relationTable, sourceSet)
  const tableIsTarget = sameRelationPhysicalTable(relationTable, otherSet)
  const junction = Boolean(relationTable) && !tableIsSource && !tableIsTarget
  let ownerEndpoint = ''
  if (!junction) {
    if (tableIsSource !== tableIsTarget) {
      ownerEndpoint = tableIsSource ? 'SOURCE' : 'TARGET'
    } else {
      ownerEndpoint = relationOwnerEndpoint(relation, mapping, sourceSet, otherSet)
    }
    if (!ownerEndpoint) {
      message.warning('无法判断关系关联字段位于主体表还是客体表')
      return null
    }
  }
  const currentRelationValue = junction || ownerEndpoint === 'TARGET'
    ? objectRecordValue(record, { physicalColumnName: mapping.sourceColumn, propertyCode: mapping.sourceColumn })
    : currentValue
  if (currentRelationValue === undefined || currentRelationValue === null || currentRelationValue === '') {
    message.warning(`当前对象缺少关系字段值 ${mapping.sourceColumn}`)
    return null
  }
  const targetValueProperty = junction || ownerEndpoint === 'SOURCE'
    ? { physicalColumnName: mapping.targetColumn, propertyCode: mapping.targetColumn }
    : otherPrimary
  relationManage.record = record
  relationManage.currentLabel = `${selectedObjectSet.value?.conceptName || '当前对象'}：${currentValue}`
  relationManage.relationId = relation.id
  relationManage.relationName = relation.name || '关联'
  relationManage.operation = operation
  relationManage.currentEndpoint = 'SOURCE'
  relationManage.currentRelationValue = currentRelationValue
  relationManage.otherConceptName = otherSet.conceptName || '关联对象'
  relationManage.selectedValue = undefined
  relationManage.selectedLabel = ''
  relationManage.options = []
  relationManage.optionLoading = false
  relationManage.error = ''
  relationManage.preview = null
  return { otherSet, otherPrimary, targetValueProperty }
}

async function openRelationCreate(record, relation) {
  let context
  try {
    context = await prepareRelationManage(record, relation, 'CREATE')
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '关系字段配置读取失败')
    return
  }
  if (!context) return
  relationManage.visible = true
  relationManage.optionLoading = true
  try {
    const rowsRes = await queryObjects({
      ontologyId: props.ontologyId,
      conceptId: context.otherSet.conceptId,
      tableBindingId: context.otherSet.tableBindingId,
      pageNum: 1,
      pageSize: 200,
      filters: JSON.stringify({ groups: [], orderBy: [], columns: [], keyword: '' })
    })
    const options = []
    ;(rowsRes.data?.rows || []).forEach(targetRecord => {
      const value = objectRecordValue(targetRecord, context.targetValueProperty)
      if (value === undefined || value === null || value === '') return
      if (options.some(option => String(option.value) === String(value))) return
      options.push({
        value,
        label: objectActionRelationOptionLabel(context.otherSet, targetRecord, value),
        record: targetRecord
      })
    })
    relationManage.options = options
    if (!options.length) relationManage.error = `${relationManage.otherConceptName}暂无可选择的数据`
  } catch (e) {
    relationManage.error = e?.response?.data?.msg || e?.message || '关联对象加载失败'
  } finally {
    relationManage.optionLoading = false
  }
}

async function openRelationDelete(record, relation, targetRecord) {
  let context
  try {
    context = await prepareRelationManage(record, relation, 'DELETE')
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '关系字段配置读取失败')
    return
  }
  if (!context) return
  const value = objectRecordValue(targetRecord, context.targetValueProperty)
  if (value === undefined || value === null || value === '') {
    message.warning('关联对象缺少主属性值，无法删除关系')
    return
  }
  relationManage.selectedValue = value
  relationManage.selectedLabel = objectActionRelationOptionLabel(context.otherSet, targetRecord, value)
  relationManage.visible = true
}

function closeRelationManage() {
  relationManage.visible = false
  relationManage.preview = null
}

function buildRelationPayload() {
  const os = selectedObjectSet.value
  const relation = currentRelations.value.find(item => String(item.id) === String(relationManage.relationId))
  const currentPrimary = relationPrimaryProperty(os)
  const currentValue = objectRecordValue(relationManage.record, currentPrimary)
  const currentRelationValue = relationManage.currentRelationValue
  const otherValue = relationManage.selectedValue
  const currentIsSource = relationManage.currentEndpoint === 'SOURCE'
  const data = {
    [currentPrimary.propertyCode]: currentValue,
    __relation_source__: currentIsSource ? currentRelationValue : otherValue,
    __relation_target__: currentIsSource ? otherValue : currentRelationValue
  }
  return {
    ontologyId: props.ontologyId,
    conceptId: os.conceptId,
    tableBindingId: os.tableBindingId,
    relationId: relation?.id,
    actionType: relationManage.operation,
    data,
    triggerWebhook: true,
    spaceId: userStore.spaceId ? Number(userStore.spaceId) : null
  }
}

async function submitRelationPreview() {
  if (!relationManage.relationId || relationManage.selectedValue === undefined
    || relationManage.selectedValue === null || relationManage.selectedValue === '') {
    message.warning('请选择要关联或解除的对象')
    return
  }
  relationManage.loading = true
  relationManage.preview = null
  try {
    const res = await rowPreview(buildRelationPayload())
    relationManage.preview = res.data || {}
    if (relationManage.preview.status === 'PENDING_APPROVAL') {
      message.warning(relationManage.preview.message || '已提交待审批')
    } else {
      message.success(relationManage.preview.message || '已生成关系操作预览')
    }
  } finally {
    relationManage.loading = false
  }
}

async function confirmRelationExecute() {
  if (!relationManage.preview?.executionId) return
  relationManage.confirming = true
  try {
    const res = await rowConfirm({
      ...buildRelationPayload(),
      executionId: relationManage.preview.executionId
    })
    const data = res.data || {}
    if (data.status === 'EXECUTED') {
      message.success(data.message || '关系操作执行成功')
      closeRelationManage()
      relationViewVersion.value++
      loadData()
    } else {
      relationManage.preview = data
      message.error(data.message || '关系操作执行失败')
    }
  } finally {
    relationManage.confirming = false
  }
}

function relationOptionLabel(objectSet, record, key) {
  const properties = objectSet?.properties || []
  const displayProperty = properties.find(p => !p.isPrimary && /(name|title|label|名称|全称|简称)/i.test(`${p.propertyCode || ''} ${p.propertyName || ''}`))
    || properties.find(p => !p.isPrimary && /(code|编码)/i.test(`${p.propertyCode || ''} ${p.propertyName || ''}`))
    || properties.find(p => !p.isPrimary)
  const value = displayProperty ? objectRecordValue(record, displayProperty) : undefined
  return value === undefined || value === null || String(value) === String(key) ? String(key) : `${value}（${key}）`
}

// 新增对象时，加载出向关系的目标对象列表。直接外键关系写入当前对象行；
// 有关系表的多对多关系不在这里伪装成当前对象字段，避免误写学生表。
async function loadCreateRelationFields() {
  const os = selectedObjectSet.value
  relationCreateFields.value = []
  if (!os || !os.conceptId) return
  relationCreateLoading.value = true
  try {
    await loadRelations(os)
    if (!currentRelations.value.length) return
    const setsRes = await listObjectSets(props.ontologyId)
    const objectSetsAll = normalizeObjectSetsResponse(setsRes)
    const fields = []
    for (const relation of currentRelations.value) {
      try {
        const [columnRes, tableRes] = await Promise.all([listRelationColumn(relation.id), listRelationTable(relation.id)])
        const mappings = Array.isArray(columnRes.data) ? columnRes.data : (columnRes.data?.rows || [])
        const mapping = mappings.find(item => String(item.sourceConceptTableId) === String(os.tableBindingId)) || mappings[0]
        if (!mapping?.sourceColumn || !mapping?.targetConceptTableId || !mapping?.targetColumn) continue
        const hasRelationTable = Array.isArray(tableRes.data) ? tableRes.data.length > 0 : (tableRes.data?.rows || []).length > 0
        if (hasRelationTable) continue
        const sourceProperty = (os.properties || []).find(p => String(p.physicalColumnName).toLowerCase() === String(mapping.sourceColumn).toLowerCase())
        const targetSet = objectSetsAll.find(item => String(item.tableBindingId) === String(mapping.targetConceptTableId))
        if (!sourceProperty || !targetSet) continue
        const rowsRes = await queryObjects({
          ontologyId: props.ontologyId,
          conceptId: targetSet.conceptId,
          tableBindingId: targetSet.tableBindingId,
          pageNum: 1,
          pageSize: 200,
          filters: JSON.stringify({ groups: [], orderBy: [], columns: [], keyword: '' })
        })
        const rows = rowsRes.data?.rows || []
        const options = []
        rows.forEach(record => {
          const actualKey = Object.keys(record).find(key => String(key).toLowerCase() === String(mapping.targetColumn).toLowerCase())
          const value = actualKey === undefined ? undefined : record[actualKey]
          if (value !== undefined && value !== null && !options.some(option => String(option.value) === String(value))) {
            options.push({ value, label: relationOptionLabel(targetSet, record, value) })
          }
        })
        fields.push({
          relationId: relation.id,
          label: `${relation.name || '关联'}（${targetSet.conceptName || '目标对象'}）`,
          targetConceptName: targetSet.conceptName || '目标对象',
          sourceColumn: mapping.sourceColumn,
          sourcePropertyCode: sourceProperty.propertyCode,
          options
        })
      } catch (e) {
        // 关系元数据不完整时不阻断普通对象新增。
      }
    }
    relationCreateFields.value = fields
  } finally {
    relationCreateLoading.value = false
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
  relationCreateFields.value.forEach(field => {
    const value = rowModal.relationValues[field.relationId]
    if (value !== undefined && value !== null && value !== '' && field.sourcePropertyCode) {
      data[field.sourcePropertyCode] = value
    }
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
  rowModal.relationValues = {}
  rowModal.triggerWebhook = true
  rowModal.preview = null
  rowModal.loading = false
  rowModal.confirming = false
  rowModal.visible = true
}

function openRowCreate() {
  openRowModal('CREATE', null)
  loadCreateRelationFields().catch(() => { relationCreateFields.value = [] })
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
.row-relation-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #86909c;
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
