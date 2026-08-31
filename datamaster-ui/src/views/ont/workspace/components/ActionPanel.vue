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
          {{ record.actionType === 'FUNCTION' ? (functionMap[record.functionId] || record.functionId || '-') : (conceptMap[record.conceptId] || record.conceptId || '-') }}
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
          <a-tag v-if="record.rollbackTime" color="purple">已回退</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'generatedSql'">
          <span class="sql-cell">{{ record.generatedSql || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'execAction'">
          <a-button v-if="record.status === 'PENDING_APPROVAL'" type="link" size="small" style="color:#52c41a" @click="handleApprove(record)" v-hasPermi="['ont:action:edit']">通过</a-button>
          <a-button v-if="record.status === 'PENDING_APPROVAL'" type="link" danger size="small" @click="handleReject(record)" v-hasPermi="['ont:action:edit']">拒绝</a-button>
          <a-button v-if="record.status === 'APPROVED'" type="link" size="small" style="color:#1677ff" @click="handleRun(record)" v-hasPermi="['ont:action:edit']">执行</a-button>
          <a-button v-if="record.status === 'EXECUTED' && !record.rollbackTime && actionTypeMap[record.actionId] !== 'FUNCTION'" type="link" size="small" style="color:#fa8c16" @click="handleRollback(record)" v-hasPermi="['ont:action:edit']">回退</a-button>
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
        <a-form-item v-if="form.actionType !== 'FUNCTION'" label="绑定概念" name="conceptId">
          <a-select
            v-model:value="form.conceptId"
            placeholder="请选择绑定的概念"
            show-search
            option-filter-prop="label"
            :options="conceptOptions"
            @change="loadConceptProperties"
          />
        </a-form-item>
        <a-form-item v-else label="绑定函数" name="functionId">
          <a-select
            v-model:value="form.functionId"
            placeholder="请选择共享函数"
            show-search
            option-filter-prop="label"
            :options="functionOptions"
            @change="handleFunctionChange"
          />
        </a-form-item>
        <template v-if="form.actionType === 'FUNCTION'">
          <a-form-item label="数据来源概念">
            <a-select
              v-model:value="form.sourceConceptId"
              placeholder="选填：绑定后执行时将该概念物理表数据注入 input.source.rows"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="conceptOptions"
              @change="handleSourceConceptChange"
            />
          </a-form-item>
          <a-form-item label="输出目标概念">
            <a-select
              v-model:value="form.outputConceptId"
              placeholder="选填：脚本 JSON 输出按主键 UPSERT 到该概念物理表"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="conceptOptions"
            />
          </a-form-item>
          <a-form-item v-if="functionParamMappings.length" label="入参字段映射">
            <div style="width:100%">
              <div style="font-size:12px;color:#999;margin-bottom:6px;">
                固定值入参执行时输入框手填；字段映射入参把脚本参数名映射到数据来源概念的属性，执行时注入该属性 code，脚本按参数名动态处理该字段。
              </div>
              <div v-for="row in functionParamMappings" :key="row.paramName" style="display:flex;align-items:center;gap:8px;margin-bottom:6px;border:1px solid #eee;padding:6px 8px;border-radius:4px;">
                <span style="width:90px;font-weight:500;">{{ row.paramName }}</span>
                <a-radio-group v-model:value="row.kind" size="small">
                  <a-radio-button value="value">固定值</a-radio-button>
                  <a-radio-button value="field">字段映射</a-radio-button>
                </a-radio-group>
                <a-select
                  v-if="row.kind === 'field'"
                  v-model:value="row.sourcePropertyId"
                  style="flex:1;min-width:180px;"
                  placeholder="选择数据来源概念属性"
                  show-search
                  option-filter-prop="label"
                  :options="sourcePropertyOptions"
                  @change="syncFieldMapping(row)"
                />
                <span v-else style="flex:1;color:#999;font-size:12px;">执行时输入框手填</span>
              </div>
            </div>
          </a-form-item>
          <a-form-item label="读取限制">
            <a-input-number v-model:value="form.readLimit" :min="1" :max="100000" style="width:200px" />
            <div style="font-size:12px;color:#999;margin-top:2px;">数据来源读取行数上限，默认 5000</div>
          </a-form-item>
          <a-form-item label="需要审批">
            <a-switch v-model:checked="form.needsApproval" />
          </a-form-item>
        </template>
        <a-alert
          v-if="form.actionType !== 'FUNCTION' && isConditionType"
          type="warning"
          show-icon
          :message="conditionHint"
          style="margin-bottom: 8px;"
        />
        <template v-if="form.actionType !== 'FUNCTION'">
        <a-form-item label="目标属性">
          <a-select
            v-model:value="selectedPropertyIds"
            mode="multiple"
            allow-clear
            placeholder="选择动作涉及的目标属性（不选则提交时手工填写JSON参数）"
            :options="propertyOptions"
            option-filter-prop="label"
            @change="syncParamRows"
          />
        </a-form-item>
        <a-form-item v-if="paramRows.length" :wrapper-col="{ span: 24 }">
          <div class="param-config-block">
            <div v-for="row in paramRows" :key="row.propertyCode" class="param-row">
              <div class="param-head">
                <span class="param-name">{{ row.propertyLabel }}</span>
                <div class="param-actions">
                  <a-checkbox v-model:checked="row.required">必填</a-checkbox>
                </div>
              </div>
              <div class="param-body">
                <a-radio-group v-model:value="row.valueMode" size="small">
                  <a-radio-button value="direct">固定值</a-radio-button>
                  <a-radio-button value="placeholder">入参引用</a-radio-button>
                  <a-radio-button value="expression">SQL表达式</a-radio-button>
                </a-radio-group>
                <a-input
                  v-if="row.valueMode === 'direct'"
                  v-model:value="row.valueTemplate"
                  size="small"
                  placeholder="固定目标值，如 已审核"
                />
                <a-input
                  v-else-if="row.valueMode === 'placeholder'"
                  v-model:value="row.valueTemplate"
                  size="small"
                  placeholder="引用入参名，如 ${newStatus}"
                />
                <a-textarea
                  v-else
                  v-model:value="row.valueTemplate"
                  size="small"
                  :auto-size="{ minRows: 1, maxRows: 3 }"
                  placeholder="原生SQL表达式，如 CASE WHEN level='VIP' THEN 2 ELSE 1 END"
                />
              </div>
            </div>
          </div>
        </a-form-item>
        <a-form-item v-if="isConditionType" :wrapper-col="{ span: 24 }">
          <div class="condition-config-block">
            <div class="condition-block-head">
              <span class="condition-block-title">条件字段（WHERE 条件）</span>
              <a-button type="dashed" size="small" @click="addConditionRow">
                <template #icon><PlusOutlined /></template>
                添加条件字段
              </a-button>
            </div>
            <a-empty v-if="!conditionRows.length" description="尚未添加条件字段，请点击上方按钮添加（UPDATE/DELETE 至少 1 个）" :image-style="{ height: '48px' }" />
            <div v-for="(row, idx) in conditionRows" :key="idx" class="condition-row">
              <div class="condition-row-head">
                <span class="condition-idx">条件 {{ idx + 1 }}</span>
                <a-button type="link" danger size="small" @click="removeConditionRow(idx)">删除</a-button>
              </div>
              <div class="param-body">
                <a-select
                  v-model:value="row.propertyId"
                  :options="propertyOptions"
                  placeholder="选择条件属性"
                  option-filter-prop="label"
                  class="condition-prop-select"
                  @change="syncConditionProperty(row)"
                />
                <a-select
                  v-if="idx > 0"
                  v-model:value="row.link"
                  class="condition-link-select"
                  :options="conditionLinkOptions"
                />
                <a-checkbox v-model:checked="row.negate">非</a-checkbox>
                <a-checkbox v-model:checked="row.required">必填</a-checkbox>
              </div>
              <div class="param-body">
                <a-radio-group v-model:value="row.valueMode" size="small">
                  <a-radio-button value="direct">固定值</a-radio-button>
                  <a-radio-button value="placeholder">入参引用</a-radio-button>
                  <a-radio-button value="expression">SQL表达式</a-radio-button>
                </a-radio-group>
                <a-input
                  v-if="row.valueMode === 'direct'"
                  v-model:value="row.valueTemplate"
                  size="small"
                  placeholder="固定条件值，如 已审核"
                />
                <a-input
                  v-else-if="row.valueMode === 'placeholder'"
                  v-model:value="row.valueTemplate"
                  size="small"
                  placeholder="引用入参名，如 ${orderId}"
                />
                <a-textarea
                  v-else
                  v-model:value="row.valueTemplate"
                  size="small"
                  :auto-size="{ minRows: 1, maxRows: 3 }"
                  placeholder="原生SQL表达式，如 NOW()"
                />
              </div>
            </div>
          </div>
        </a-form-item>
        </template>
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
        <template v-if="execAction.actionType === 'FUNCTION'">
          <template v-if="execFunctionParams.length">
            <a-form-item
              v-for="paramName in execInputParamsList"
              :key="paramName"
              :label="paramName"
            >
              <a-input v-model:value="execFormValues[paramName]" :placeholder="'引用名 ${' + paramName + '}'" />
            </a-form-item>
            <div v-if="Object.keys(execFieldParamMap).length" class="fixed-param-lines">
              <div class="modal-hint-line">以下入参已映射到数据来源概念属性，无需手填：</div>
              <div v-for="(code, name) in execFieldParamMap" :key="name" class="fixed-param-line">
                <span class="fixed-param-name">{{ name }}</span>
                <span class="fixed-param-tag">字段映射</span>
                <code>{{ code }}</code>
              </div>
            </div>
          </template>
          <a-form-item v-else>
            <div class="modal-hint-line">该函数未声明参数，将直接执行。</div>
          </a-form-item>
        </template>
        <template v-else-if="hasParamConfig">
          <a-form-item
            v-for="cfg in execPlaceholderConfigs"
            :key="cfg.paramName"
            :label="cfg.propName + (cfg.condition ? '（条件）' : '')"
            :required="!!cfg.required"
          >
            <a-input v-model:value="execFormValues[cfg.paramName]" :placeholder="'请输入 ' + cfg.paramName" />
          </a-form-item>
          <div v-if="execFixedConfigs.length" class="fixed-param-lines">
            <div class="modal-hint-line">以下参数已在动作定义中固定：</div>
            <div v-for="cfg in execFixedConfigs" :key="cfg.propertyCode" class="fixed-param-line">
              <span class="fixed-param-name">{{ cfg.propName }}</span>
              <span v-if="cfg.condition" class="fixed-param-tag">条件</span>
              <span class="fixed-param-mode">{{ cfg.modeText }}</span>
              <code>{{ cfg.display }}</code>
            </div>
          </div>
        </template>
        <a-form-item v-else label="输入参数">
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
        <div class="result-title">{{ resultIsFunction ? '生成代码' : '生成 SQL' }}</div>
        <pre class="result-pre sql-pre">{{ currentResult.generatedSql || '无' }}</pre>
      </div>
      <div class="result-section">
        <div class="result-title">预览结果</div>
        <pre class="result-pre">{{ currentResult.previewResult || '无' }}</pre>
      </div>
      <div v-if="!isRolledBack && compareRows.length" class="result-section">
        <div class="result-title">执行前后对比{{ compareRowHint }}</div>
        <a-table
          :columns="compareColumns"
          :data-source="compareRows"
          :pagination="false"
          row-key="colKey"
          size="small"
          :scroll="{ x: 420 }"
          :row-class-name="compareRowClassName"
        />
      </div>
      <template v-else-if="!isRolledBack && (beforeRows.length || afterRows.length)">
        <div v-if="beforeRows.length" class="result-section">
          <div class="result-title">执行前数据</div>
          <a-table
            v-if="snapshotColumns.length"
            :columns="snapshotColumns"
            :data-source="beforeRows"
            :pagination="false"
            :row-key="(_, idx) => idx"
            size="small"
            :scroll="{ x: 420 }"
          />
          <pre v-else class="result-pre">{{ currentResult.beforeData }}</pre>
        </div>
        <div v-if="afterRows.length" class="result-section">
          <div class="result-title">执行后数据</div>
          <a-table
            v-if="snapshotColumns.length"
            :columns="snapshotColumns"
            :data-source="afterRows"
            :pagination="false"
            :row-key="(_, idx) => idx"
            size="small"
            :scroll="{ x: 420 }"
          />
          <pre v-else class="result-pre">{{ currentResult.afterData }}</pre>
        </div>
      </template>
      <div v-if="isRolledBack && rollbackCompareRows.length" class="result-section">
        <div class="result-title">回退前后对比{{ rollbackCompareRowHint }}</div>
        <a-table
          :columns="compareColumns"
          :data-source="rollbackCompareRows"
          :pagination="false"
          row-key="colKey"
          size="small"
          :scroll="{ x: 420 }"
          :row-class-name="compareRowClassName"
        />
      </div>
      <template v-else-if="isRolledBack && (rollbackBeforeRows.length || rollbackAfterRows.length)">
        <div v-if="rollbackBeforeRows.length" class="result-section">
          <div class="result-title">回退前数据</div>
          <a-table
            v-if="rollbackSnapshotColumns.length"
            :columns="rollbackSnapshotColumns"
            :data-source="rollbackBeforeRows"
            :pagination="false"
            :row-key="(_, idx) => idx"
            size="small"
            :scroll="{ x: 420 }"
          />
          <pre v-else class="result-pre">{{ currentResult.rollbackBeforeData || currentResult.beforeData }}</pre>
        </div>
        <div v-if="rollbackAfterRows.length" class="result-section">
          <div class="result-title">回退后数据</div>
          <a-table
            v-if="rollbackSnapshotColumns.length"
            :columns="rollbackSnapshotColumns"
            :data-source="rollbackAfterRows"
            :pagination="false"
            :row-key="(_, idx) => idx"
            size="small"
            :scroll="{ x: 420 }"
          />
          <pre v-else class="result-pre">{{ currentResult.rollbackAfterData || currentResult.afterData }}</pre>
        </div>
      </template>
      <div v-if="currentResult.errorMessage" class="result-section">
        <div class="result-title error">错误信息</div>
        <pre class="result-pre error-pre">{{ currentResult.errorMessage }}</pre>
      </div>
    </a-modal>
  </div>
</template>

<script setup name="ActionPanel">
import { listAction, getAction, addAction, updateAction, delAction, submitExecution, approveExecution, rejectExecution, runExecution, rollbackExecution, listExecution } from '@/api/ont/action'
import { listConcept } from '@/api/ont/concept'
import { listProperty } from '@/api/ont/property'
import { listConceptTable } from '@/api/ont/conceptTable'
import { listPropertyColumn } from '@/api/ont/propertyColumn'
import { listFunction, getFunction } from '@/api/ont/function'
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
// 动作类型映射（id -> 动作类型，回退按钮与结果展示用）
const actionTypeMap = computed(() => {
  const map = {}
  actionList.value.forEach(a => { map[a.id] = a.actionType })
  return map
})

// 共享函数下拉与名称映射（函数类动作绑定共享函数，不绑定本体）
const functionOptions = ref([])
const functionMap = computed(() => {
  const map = {}
  functionOptions.value.forEach(f => { map[f.value] = f.label })
  return map
})
// 函数 id -> 参数名数组（执行弹窗渲染输入框用）
const functionParamsMap = ref({})
// 当前执行动作的函数参数声明（可读副本）
const execFunctionParams = computed(() => functionParamsMap.value[execAction.value.id] || [])
// 当前执行动作的字段映射入参（param_name -> sourcePropertyCode），执行时值来自概念属性映射而非手填
const execFieldParamMap = computed(() => {
  const map = {}
  parseParamConfig(execAction.value.paramConfig).forEach(c => {
    if (c.kind === 'field' && c.paramName && c.sourcePropertyCode) map[c.paramName] = c.sourcePropertyCode
  })
  return map
})
// 需要手填的入参：函数参数声明中非字段映射的部分
const execInputParamsList = computed(() =>
  (execFunctionParams.value || []).filter(name => !execFieldParamMap.value[name])
)

function loadFunctions() {
  listFunction({ pageNum: 1, pageSize: 200 }).then(res => {
    const rows = res.data?.rows || []
    functionOptions.value = rows.map(f => ({ value: f.id, label: f.name }))
  })
}

function parseFunctionParams(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr.filter(n => typeof n === 'string') : []
  } catch (e) {
    return []
  }
}

/* ============ 函数动作：入参字段映射绑定 ============ */
// 绑定表单当前选中函数的参数名列表（PARAMS）
const formFunctionParams = ref([])
// 入参映射行：{ paramName, kind: 'value'|'field', sourcePropertyCode }
const functionParamMappings = ref([])
// 数据来源概念的属性下拉（字段映射选目标用）
const sourcePropertyOptions = ref([])

// form.functionId 变化时加载其参数声明，并重置映射行（默认全部为固定值）
function handleFunctionChange() {
  formFunctionParams.value = []
  functionParamMappings.value = []
  const functionId = form.value.functionId
  if (!functionId) return
  getFunction(functionId).then(res => {
    const fn = res.data || {}
    const params = parseFunctionParams(fn.params)
    formFunctionParams.value = params
    functionParamMappings.value = params.map(name => ({ paramName: name, kind: 'value', sourcePropertyCode: undefined }))
  }).catch(() => {
    formFunctionParams.value = []
    functionParamMappings.value = []
  })
}

// form.sourceConceptId 变化时加载该概念属性供字段映射选择
function handleSourceConceptChange() {
  loadSourcePropertiesFor(form.value.sourceConceptId)
}

function loadSourcePropertiesFor(conceptId) {
  sourcePropertyOptions.value = []
  if (!conceptId) return Promise.resolve()
  return listConceptTable({ conceptId }).then(res => {
    const tables = (res.data && res.data.rows) || res.data || []
    if (!tables.length) return null
    return listPropertyColumn(tables[0].id)
  }).then(res => {
    if (!res) return
    const cols = (res.data && res.data.rows) || res.data || []
    sourcePropertyOptions.value = cols.map(c => {
      const p = propertyDict.value[c.propertyId] || {}
      const code = p.code || c.columnName
      return { value: c.propertyId, code, columnName: c.columnName, label: `${p.name || code}（${c.columnName}）` }
    })
  }).catch(() => {
    sourcePropertyOptions.value = []
  })
}

// 字段映射下拉选中后回写 sourcePropertyCode 与 propertyId
function syncFieldMapping(row) {
  const opt = sourcePropertyOptions.value.find(o => o.value === row.sourcePropertyId)
  if (opt) row.sourcePropertyCode = opt.code
}

const actionTypeOptions = [
  { value: 'CREATE', label: '新建' },
  { value: 'UPDATE', label: '更新' },
  { value: 'DELETE', label: '删除' },
  { value: 'QUERY', label: '查询' },
  { value: 'FUNCTION', label: '函数' }
]

function actionTypeText(type) {
  return { CREATE: '新建', UPDATE: '更新', DELETE: '删除', QUERY: '查询', FUNCTION: '函数' }[type] || type
}

function actionTypeColor(type) {
  return { CREATE: 'green', UPDATE: 'blue', DELETE: 'red', QUERY: 'purple', FUNCTION: 'orange' }[type] || 'default'
}

function execStatusText(s) {
  return { DRAFT: '草稿', PENDING_APPROVAL: '待审批', APPROVED: '已批准', REJECTED: '已拒绝', EXECUTED: '已执行', FAILED: '失败', ROLLED_BACK: '已回退' }[s] || s
}

function execStatusColor(s) {
  return { DRAFT: 'default', PENDING_APPROVAL: 'orange', APPROVED: 'green', REJECTED: 'red', EXECUTED: 'blue', FAILED: 'red', ROLLED_BACK: 'purple' }[s] || 'default'
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
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 170 },
  { title: '审批意见', dataIndex: 'approvalReason', align: 'center', width: 130, ellipsis: true },
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
    actionType: [{ required: true, message: '动作类型不能为空', trigger: 'change' }]
  }
})
const { form, rules } = toRefs(data)

// UPDATE/DELETE 动作必须配置「条件字段」作为 WHERE 条件，否则后端无法生成安全 SQL
const isConditionType = computed(() => form.value.actionType === 'UPDATE' || form.value.actionType === 'DELETE')
const conditionHint = computed(() => {
  if (form.value.actionType === 'DELETE') {
    return '删除动作必须至少添加 1 个「条件字段」作为 WHERE 条件，否则无法生成删除 SQL'
  }
  if (form.value.actionType === 'UPDATE') {
    return '更新动作必须至少添加 1 个「条件字段」作为 WHERE 条件（建议添加主键）'
  }
  return ''
})
// 条件字段间的逻辑连接符：且(AND) / 或(OR)，配合「非」取反勾选组合出完整 WHERE 条件
const conditionLinkOptions = [
  { value: 'AND', label: '且(AND)' },
  { value: 'OR', label: '或(OR)' }
]

// 动态条件字段行：每行自选属性 + 连接符(首个忽略) + 非取反 + 取值配置
const conditionRows = ref([])

function addConditionRow() {
  conditionRows.value.push({
    propertyId: undefined,
    propertyCode: undefined,
    propertyLabel: undefined,
    link: 'AND',
    negate: false,
    valueMode: 'direct',
    valueTemplate: '',
    required: false
  })
}

function removeConditionRow(idx) {
  conditionRows.value.splice(idx, 1)
}

function syncConditionProperty(row) {
  const opt = propertyOptions.value.find(o => o.value === row.propertyId)
  if (opt) {
    row.propertyCode = opt.code
    row.propertyLabel = opt.label
  }
}

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

/* ================= 目标属性配置（属性选择 + 目标值） ================= */

// 本体内全部属性字典（id -> 属性）
const propertyDict = ref({})
// 当前绑定概念的可用属性（经概念表列映射解析）
const propertyOptions = ref([])
const selectedPropertyIds = ref([])
// 每个选中属性的取值配置行
const paramRows = ref([])

let propLoadToken = 0

async function loadConceptProperties(conceptId) {
  const token = ++propLoadToken
  propertyOptions.value = []
  selectedPropertyIds.value = []
  paramRows.value = []
  if (!conceptId) return
  const tableRes = await listConceptTable({ conceptId })
  if (token !== propLoadToken) return
  const tables = (tableRes.data && tableRes.data.rows) || tableRes.data || []
  if (!tables.length) return
  const colRes = await listPropertyColumn(tables[0].id)
  if (token !== propLoadToken) return
  const cols = (colRes.data && colRes.data.rows) || colRes.data || []
  propertyOptions.value = cols.map(c => {
    const p = propertyDict.value[c.propertyId] || {}
    const code = p.code || c.columnName
    return {
      value: c.propertyId,
      code,
      columnName: c.columnName,
      isPrimary: !!p.isPrimary,
      label: `${p.name || code}（${c.columnName}）${p.isPrimary ? ' · 主键' : ''}`
    }
  })
}

function loadProperties() {
  listProperty({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 200 }).then(res => {
    const rows = (res.data && res.data.rows) || []
    const dict = {}
    rows.forEach(p => { dict[p.id] = p })
    propertyDict.value = dict
  })
}

function stripPlaceholder(template) {
  return String(template || '').trim().replace(/^\$\{/, '').replace(/\}$/, '').trim()
}

function syncParamRows(selectedIds) {
  const kept = paramRows.value.filter(r => selectedIds.includes(r.propertyId))
  const existingIds = kept.map(r => r.propertyId)
  selectedIds.forEach(pid => {
    if (existingIds.includes(pid)) return
    const opt = propertyOptions.value.find(o => o.value === pid)
    if (!opt) return
    kept.push({
      propertyId: pid,
      propertyCode: opt.code,
      propertyLabel: opt.label,
      valueMode: 'direct',
      valueTemplate: '',
      required: false
    })
  })
  paramRows.value = kept
}

function buildParamConfigJson() {
  const cfg = []
  // 目标属性（INSERT 插入列 / UPDATE 的 SET 列）
  paramRows.value.forEach(r => {
    cfg.push({
      propertyCode: r.propertyCode,
      valueMode: r.valueMode,
      valueTemplate: r.valueTemplate,
      required: !!r.required
    })
  })
  // 条件字段（WHERE 条件，带 condition/link/negate 标记）
  conditionRows.value.forEach(r => {
    if (!r.propertyCode) return
    cfg.push({
      propertyCode: r.propertyCode,
      valueMode: r.valueMode,
      valueTemplate: r.valueTemplate,
      required: !!r.required,
      condition: true,
      conditionLink: r.link || 'AND',
      conditionNegate: !!r.negate
    })
  })
  return cfg.length ? JSON.stringify(cfg) : undefined
}

function parseParamConfig(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
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
  form.value = { ontologyId: props.ontologyId, name: undefined, actionType: undefined, conceptId: undefined, functionId: undefined, sourceConceptId: undefined, outputConceptId: undefined, readLimit: 5000, needsApproval: true, description: undefined, paramConfig: undefined }
  selectedPropertyIds.value = []
  paramRows.value = []
  conditionRows.value = []
  formFunctionParams.value = []
  functionParamMappings.value = []
  sourcePropertyOptions.value = []
  proxy.resetForm('actionRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增动作'
}

function handleUpdate(row) {
  reset()
  getAction(row.id).then(async res => {
    form.value = res.data
    if (form.value.actionType !== 'FUNCTION') {
      await loadConceptProperties(form.value.conceptId)
      const cfgList = parseParamConfig(form.value.paramConfig)
      // 目标属性回显（含旧动作无 condition 标记的配置）
      const targetCfgs = cfgList.filter(c => !c.condition)
      const condCfgs = cfgList.filter(c => c.condition)
      if (targetCfgs.length) {
        selectedPropertyIds.value = targetCfgs
          .map(c => (propertyOptions.value.find(o => o.code === c.propertyCode) || {}).value)
          .filter(v => v !== undefined)
        syncParamRows(selectedPropertyIds.value)
        targetCfgs.forEach(c => {
          const r = paramRows.value.find(item => item.propertyCode === c.propertyCode)
          if (r) {
            r.valueMode = ['direct', 'placeholder', 'expression'].includes(c.valueMode) ? c.valueMode : 'direct'
            r.valueTemplate = c.valueTemplate || ''
            r.required = !!c.required
          }
        })
      }
      // 条件字段回显（动态行）
      conditionRows.value = condCfgs.map(c => {
        const opt = propertyOptions.value.find(o => o.code === c.propertyCode)
        return {
          propertyId: opt ? opt.value : undefined,
          propertyCode: c.propertyCode,
          propertyLabel: opt ? opt.label : c.propertyCode,
          link: ['AND', 'OR'].includes(c.conditionLink) ? c.conditionLink : 'AND',
          negate: !!c.conditionNegate,
          valueMode: ['direct', 'placeholder', 'expression'].includes(c.valueMode) ? c.valueMode : 'direct',
          valueTemplate: c.valueTemplate || '',
          required: !!c.required
        }
      })
    } else {
      // FUNCTION：加载函数参数声明，并按 param_config 回显字段映射
      formFunctionParams.value = []
      functionParamMappings.value = []
      sourcePropertyOptions.value = []
      const fieldMapByParam = {}
      parseParamConfig(form.value.paramConfig).forEach(c => {
        if (c.kind === 'field' && c.paramName) fieldMapByParam[c.paramName] = c
      })
      if (form.value.functionId) {
        try {
          const fn = (await getFunction(form.value.functionId)).data || {}
          const params = parseFunctionParams(fn.params)
          formFunctionParams.value = params
          functionParamMappings.value = params.map(name => {
            const saved = fieldMapByParam[name]
            return {
              paramName: name,
              kind: (saved && saved.sourcePropertyCode) ? 'field' : 'value',
              sourcePropertyCode: saved ? saved.sourcePropertyCode : undefined,
              sourcePropertyId: saved ? saved.sourcePropertyId : undefined
            }
          })
        } catch {
          formFunctionParams.value = []
          functionParamMappings.value = []
        }
      }
      if (form.value.sourceConceptId) {
        await loadSourcePropertiesFor(form.value.sourceConceptId)
      }
    }
    open.value = true
    title.value = '修改动作'
  })
}

function submitForm() {
  proxy.$refs['actionRef'].validate().then(() => {
    const type = form.value.actionType
    if (type === 'FUNCTION') {
      if (!form.value.functionId) {
        proxy.$modal.msgError('请选择共享函数')
        return
      }
      // 入参字段映射：把 kind=field 的入参映射序列化到 param_config（固定值入参不写入，执行时手填）
      const fieldMappings = functionParamMappings.value
        .filter(m => m.kind === 'field' && m.sourcePropertyCode)
        .map(m => ({ paramName: m.paramName, kind: 'field', sourcePropertyCode: m.sourcePropertyCode, sourcePropertyId: m.sourcePropertyId }))
      form.value.paramConfig = fieldMappings.length ? JSON.stringify(fieldMappings) : undefined
    } else {
      if (!form.value.conceptId) {
        proxy.$modal.msgError('请选择绑定概念')
        return
      }
      // UPDATE/DELETE 必须添加 ≥1 个「条件字段」作为 WHERE 条件，否则后端构建 SQL 会失败
      if (type === 'UPDATE' || type === 'DELETE') {
        const condCount = conditionRows.value.filter(r => r.propertyCode).length
        if (!condCount) {
          proxy.$modal.msgError(type === 'DELETE'
            ? '删除动作必须至少添加 1 个「条件字段」作为 WHERE 条件'
            : '更新动作必须至少添加 1 个「条件字段」作为 WHERE 条件（建议添加主键）')
          return
        }
      }
      form.value.paramConfig = buildParamConfigJson()
    }
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
// 动作参数配置（PARAM_CONFIG）：配置驱动时按属性渲染表单，否则回退手工JSON
const execParamConfigs = ref([])
const execFormValues = reactive({})

const hasParamConfig = computed(() => execParamConfigs.value.length > 0)

// propertyCode -> 属性友好名称（执行弹窗展示用）
const propertyCodeNameMap = computed(() => {
  const map = {}
  Object.values(propertyDict.value).forEach(p => {
    map[p.code] = p.name || p.code
  })
  return map
})

// 入参引用型：执行时需用户填写
const execPlaceholderConfigs = computed(() =>
  execParamConfigs.value
    .filter(c => c.valueMode === 'placeholder')
    .map(c => ({
      ...c,
      paramName: stripPlaceholder(c.valueTemplate),
      propName: propertyCodeNameMap.value[c.propertyCode] || c.propertyCode
    }))
)

// 固定值 / SQL表达式型：只读展示
const execFixedConfigs = computed(() =>
  execParamConfigs.value
    .filter(c => c.valueMode !== 'placeholder')
    .map(c => ({
      ...c,
      propName: propertyCodeNameMap.value[c.propertyCode] || c.propertyCode,
      modeText: c.valueMode === 'direct' ? '固定值' : 'SQL表达式',
      display: c.valueTemplate
    }))
)

function openSubmitExec(row) {
  execAction.value = row
  inputParams.value = '{}'
  execParamConfigs.value = parseParamConfig(row.paramConfig)
  Object.keys(execFormValues).forEach(k => delete execFormValues[k])
  if (row.actionType === 'FUNCTION') {
    const functionId = row.functionId
    functionParamsMap.value[row.id] = []
    if (functionId) {
      getFunction(functionId).then(res => {
        const fn = res.data || {}
        functionParamsMap.value[row.id] = parseFunctionParams(fn.params)
      }).catch(() => {
        functionParamsMap.value[row.id] = []
      })
    }
  }
  execOpen.value = true
}

function submitExec() {
  if (execAction.value.actionType === 'FUNCTION') {
    const params = {}
    ;(execInputParamsList.value || []).forEach(name => {
      const v = execFormValues[name]
      if (v !== undefined && v !== null && v !== '') params[name] = v
    })
    const input = JSON.stringify(params)
    execSaving.value = true
    submitExecution({ actionId: execAction.value.id, inputParams: input }).then(res => {
      proxy.$modal.msgSuccess(res.data && res.data.status === 'PENDING_APPROVAL' ? '已提交审批' : '已提交')
      execOpen.value = false
      loadExecutions()
    }).finally(() => {
      execSaving.value = false
    })
    return
  }
  let params = {}
  if (hasParamConfig.value) {
    // 配置驱动：仅收集入参引用型字段
    execPlaceholderConfigs.value.forEach(cfg => {
      const v = execFormValues[cfg.paramName]
      if (v !== undefined && v !== null && v !== '') params[cfg.paramName] = v
    })
    const missing = execPlaceholderConfigs.value.filter(cfg => cfg.required && params[cfg.paramName] === undefined)
    if (missing.length) {
      proxy.$modal.msgError('缺少必填参数: ' + missing.map(m => m.propName).join('、'))
      return
    }
  } else {
    // 未配置目标属性的旧动作：保留 JSON 手工输入
    const raw = (inputParams.value || '').trim()
    if (raw) {
      try {
        params = JSON.parse(raw)
      } catch (e) {
        proxy.$modal.msgError('输入参数必须是合法的 JSON')
        return
      }
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

function handleRollback(record) {
  proxy.$modal.confirm('确认回退该记录？将按执行前快照还原数据并留痕。').then(() => {
    return rollbackExecution(record.id)
  }).then(() => {
    proxy.$modal.msgSuccess('回退完成')
    loadExecutions()
  }).catch(() => {})
}

const resultOpen = ref(false)
const currentResult = ref({})
// 当前执行记录对应的「物理列 → 本体属性」映射（对比表左侧按属性展示）
const resultColumnMap = ref({})
// 当前执行结果是否为函数类动作（决定「生成 SQL」/「生成代码」标题）
const resultIsFunction = computed(() => actionTypeMap.value[currentResult.value.actionId] === 'FUNCTION')

function handleViewResult(record) {
  currentResult.value = record
  resultOpen.value = true
  loadResultColumnMap(record.actionId)
}

// 由执行记录 actionId → 动作 → 概念 → 概念表列映射 → 属性名，构建物理列→属性字典
async function loadResultColumnMap(actionId) {
  resultColumnMap.value = {}
  if (!actionId) return
  try {
    const res = await getAction(actionId)
    const action = res.data || {}
    const conceptId = action.conceptId
    if (!conceptId) return
    const tableRes = await listConceptTable({ conceptId })
    const tables = (tableRes.data && tableRes.data.rows) || tableRes.data || []
    if (!tables.length) return
    const colRes = await listPropertyColumn(tables[0].id)
    const cols = (colRes.data && colRes.data.rows) || colRes.data || []
    const map = {}
    cols.forEach(c => {
      const p = propertyDict.value[c.propertyId] || {}
      const code = p.code || c.columnName
      map[c.columnName] = {
        name: p.name || code,
        columnName: c.columnName,
        primary: !!p.isPrimary
      }
    })
    resultColumnMap.value = map
  } catch (e) {
    // 映射失败时对比表回退展示物理列名
  }
}

// 执行前后数据解析 → 属性对比表
function parseResultRows(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

function formatCellVal(v) {
  if (v === null || v === undefined) return '—'
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

const beforeRows = computed(() => parseResultRows(currentResult.value.beforeData))
const afterRows = computed(() => parseResultRows(currentResult.value.afterData))

// 回退前后数据快照：
// 兼容两种来源——旧版 ROLLED_BACK 记录存于 rollbackBeforeData/rollbackAfterData；
// 新版回退记录(beforeData=回退前/afterData=回退后)未显式写 rollback* 字段，回退取 beforeData/afterData
const rollbackBeforeRows = computed(() => {
  const direct = parseResultRows(currentResult.value.rollbackBeforeData)
  return direct.length ? direct : parseResultRows(currentResult.value.beforeData)
})
const rollbackAfterRows = computed(() => {
  const direct = parseResultRows(currentResult.value.rollbackAfterData)
  return direct.length ? direct : parseResultRows(currentResult.value.afterData)
})

const isRolledBack = computed(() => currentResult.value.status === 'ROLLED_BACK')

// 对比行构建（执行前后 / 回退前后共用）：多行快照逐行展平，左侧列仅展示已映射到本体属性的列
function buildCompareRows(beforeArr, afterArr) {
  const maxRows = Math.max(beforeArr.length, afterArr.length)
  const rows = []
  for (let i = 0; i < maxRows; i++) {
    const b = beforeArr[i] || {}
    const a = afterArr[i] || {}
    const keys = []
    const seen = {}
    ;[...Object.keys(b), ...Object.keys(a)].forEach(k => {
      if (!seen[k]) {
        seen[k] = 1
        keys.push(k)
      }
    })
    keys.forEach(k => {
      // 仅展示已映射到本体属性的列：未映射的物理列不进入对比表
      const mapped = resultColumnMap.value[k]
      if (!mapped || !mapped.name) return
      rows.push({
        colKey: k,
        name: maxRows > 1 ? `第${i + 1}行 · ${mapped.name}` : mapped.name,
        before: formatCellVal(b[k]),
        after: formatCellVal(a[k]),
        changed: JSON.stringify(b[k] ?? null) !== JSON.stringify(a[k] ?? null)
      })
    })
  }
  return rows
}

// 执行前后属性对比表
const compareRows = computed(() => buildCompareRows(beforeRows.value, afterRows.value))

// 回退前后属性对比表
const rollbackCompareRows = computed(() => buildCompareRows(rollbackBeforeRows.value, rollbackAfterRows.value))

const compareRowHint = computed(() => {
  const n = Math.max(beforeRows.value.length, afterRows.value.length)
  return n > 1 ? `（共 ${n} 行）` : ''
})

const rollbackCompareRowHint = computed(() => {
  const n = Math.max(rollbackBeforeRows.value.length, rollbackAfterRows.value.length)
  return n > 1 ? `（共 ${n} 行）` : ''
})

// 对比表列定义（左侧属性名 → 修改前 → 修改后）
const compareColumns = [
  { title: '属性', dataIndex: 'name', width: 160 },
  { title: '修改前', dataIndex: 'before' },
  { title: '修改后', dataIndex: 'after' }
]

// 有值变化的行高亮显示
function compareRowClassName(record) {
  return record.changed ? 'changed-row' : ''
}

// 执行前/执行后数据集表格列：仅展映已映射到本体属性的列（表头=属性名，dataIndex=物理列名）
const snapshotColumns = computed(() => {
  const src = beforeRows.value[0] || afterRows.value[0] || {}
  return Object.keys(src)
    .filter(k => resultColumnMap.value[k] && resultColumnMap.value[k].name)
    .map(k => ({ title: resultColumnMap.value[k].name, dataIndex: k, width: 160 }))
})

// 回退前/回退后数据集表格列（回退快照独立取列，避免执行快照为空时无列可用）
const rollbackSnapshotColumns = computed(() => {
  const src = rollbackBeforeRows.value[0] || rollbackAfterRows.value[0] || {}
  return Object.keys(src)
    .filter(k => resultColumnMap.value[k] && resultColumnMap.value[k].name)
    .map(k => ({ title: resultColumnMap.value[k].name, dataIndex: k, width: 160 }))
})

defineExpose({
  reload() {
    getList()
    loadExecutions()
  }
})

loadConcepts()
loadProperties()
loadFunctions()
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

  .param-config-block {
    width: 100%;
    border: 1px solid #e5eaf2;
    border-radius: 6px;
    overflow: hidden;

    .param-row {
      padding: 10px 12px;
      border-bottom: 1px solid #eef1f6;

      &:last-child {
        border-bottom: none;
      }

      .param-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;

        .param-name {
          font-size: 13px;
          font-weight: 600;
          color: #333;
        }

        .param-actions {
          display: flex;
          align-items: center;
          gap: 12px;

          :deep(.ant-checkbox-wrapper) {
            font-size: 12px;
          }
        }
      }

      .param-body {
        display: flex;
        align-items: flex-start;
        gap: 8px;

        :deep(.ant-radio-group) {
          flex-shrink: 0;
        }

        :deep(.ant-input),
        textarea.ant-input {
          flex: 1;
        }
      }
    }
  }

  .condition-config-block {
    width: 100%;
    border: 1px solid #e5eaf2;
    border-radius: 6px;
    padding: 10px 12px;

    .condition-block-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;

      .condition-block-title {
        font-size: 13px;
        font-weight: 600;
        color: #333;
      }
    }

    .condition-row {
      padding: 10px 12px;
      border: 1px solid #eef1f6;
      border-radius: 6px;
      margin-bottom: 10px;
      background: #fafbfc;

      &:last-child {
        margin-bottom: 0;
      }

      .condition-row-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;

        .condition-idx {
          font-size: 12px;
          font-weight: 600;
          color: #555;
        }
      }

      .condition-prop-select {
        width: 170px;
      }

      .condition-link-select {
        width: 90px;
      }
    }
  }

  .fixed-param-lines {
    padding: 4px 0 8px;

    .fixed-param-line {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px 0;
      font-size: 12px;

      .fixed-param-name {
        min-width: 90px;
        color: #333;
        font-weight: 600;
      }

      .fixed-param-mode {
        color: #8a95a6;
      }

      .fixed-param-tag {
        flex-shrink: 0;
        padding: 0 6px;
        background: #fff7e6;
        border: 1px solid #ffd591;
        color: #d46b08;
        border-radius: 3px;
        font-size: 12px;
        line-height: 18px;
      }

      code {
        padding: 2px 6px;
        background: #f6f8fa;
        border: 1px solid #e5eaf2;
        border-radius: 4px;
        word-break: break-all;
      }
    }
  }

  .result-section {
    margin-bottom: 12px;

    // a-table 行级变化高亮（row-class-name 返回 changed-row）
    :deep(.changed-row) {
      td {
        background: #fff7e6;
      }

      td:first-child {
        color: #d46b08;
        font-weight: 600;
      }
    }

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
