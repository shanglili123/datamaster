<template>
  <div class="app-container" ref="app-container">
    <a-layout style="min-height: calc(100vh - 125px)">
      <a-layout-sider :width="300" theme="light" :bordered="true">
        <div style="padding: 12px">
          <a-input-search v-model:value="searchValue" placeholder="搜索概念" style="margin-bottom: 12px" />
          <a-button type="primary" block @click="handleAddConcept" v-hasPermi="['ont:concept:add']">
            <template #icon><PlusOutlined /></template>
            新增概念
          </a-button>
        </div>
        <a-spin :spinning="treeLoading">
          <a-tree
            v-if="conceptTree.length > 0"
            :tree-data="conceptTree"
            :field-names="{ title: 'name', key: 'id' }"
            :default-expand-all="true"
            :selected-keys="selectedKeys"
            @select="onSelectConcept"
          />
          <a-empty v-else description="暂无概念" />
        </a-spin>
      </a-layout-sider>
      <a-layout-content style="padding: 0 16px">
        <a-card v-if="selectedConcept" :title="selectedConcept.name + ' - 详情'">
          <a-tabs v-model:activeKey="activeTab">
            <!-- 属性列表 -->
            <a-tab-pane key="properties" tab="属性列表">
              <div style="margin-bottom: 12px">
                <a-button type="primary" @click="handleAddProperty" v-hasPermi="['ont:property:add']">
                  <template #icon><PlusOutlined /></template>
                  新增属性
                </a-button>
              </div>
              <a-table :columns="propertyColumns" :data-source="propertyList" :loading="propertyLoading" row-key="id" size="middle">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'dataType'"><a-tag>{{ record.dataType }}</a-tag></template>
                  <template v-if="column.key === 'isPrimary'"><a-badge :status="record.isPrimary ? 'success' : 'default'" :text="record.isPrimary ? '是' : '否'" /></template>
                  <template v-if="column.key === 'isRequired'"><a-badge :status="record.isRequired ? 'success' : 'default'" :text="record.isRequired ? '是' : '否'" /></template>
                  <template v-if="column.key === 'action'">
                    <a-button type="link" size="small" @click="handleUpdateProperty(record)" v-hasPermi="['ont:property:edit']">修改</a-button>
                    <a-button type="link" danger size="small" @click="handleDeleteProperty(record)" v-hasPermi="['ont:property:remove']">删除</a-button>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <!-- 关系列表 -->
            <a-tab-pane key="relations" tab="关系列表">
              <div style="margin-bottom: 12px">
                <a-button type="primary" @click="handleAddRelation" v-hasPermi="['ont:relation:add']">
                  <template #icon><PlusOutlined /></template>
                  新增关系
                </a-button>
              </div>
              <a-table :columns="relationColumns" :data-source="relationList" :loading="relationLoading" row-key="id" size="middle">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'relationType'">
                    <a-tag :color="relationTypeColor(record.relationType)">{{ relationTypeText(record.relationType) }}</a-tag>
                  </template>
                  <template v-if="column.key === 'action'">
                    <a-button type="link" size="small" @click="handleUpdateRelation(record)" v-hasPermi="['ont:relation:edit']">修改</a-button>
                    <a-button type="link" danger size="small" @click="handleDeleteRelation(record)" v-hasPermi="['ont:relation:remove']">删除</a-button>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <!-- 表绑定 -->
            <a-tab-pane key="bindings" tab="表绑定">
              <div style="margin-bottom: 12px">
                <a-button type="primary" @click="handleAddBinding" v-hasPermi="['ont:concept-table:add']">
                  <template #icon><PlusOutlined /></template>
                  新增绑定
                </a-button>
              </div>
              <a-spin :spinning="bindingLoading">
                <div v-if="bindingList.length === 0" style="text-align: center; padding: 40px 0">
                  <a-empty description="暂无表绑定，请点击上方按钮新增" />
                </div>
                <a-card v-for="binding in bindingList" :key="binding.id" size="small" style="margin-bottom: 12px">
                  <template #title>
                    <span>{{ binding.tableName }}</span>
                    <a-tag color="blue" style="margin-left: 8px">{{ binding.databaseName || '默认库' }}</a-tag>
                    <a-tag v-if="binding.schemaName">{{ binding.schemaName }}</a-tag>
                  </template>
                  <template #extra>
                    <a-button type="link" size="small" @click="handleColumnMapping(binding)" v-hasPermi="['ont:property-column:edit']">列映射</a-button>
                    <a-button type="link" danger size="small" @click="handleDeleteBinding(binding)" v-hasPermi="['ont:concept-table:remove']">删除</a-button>
                  </template>
                  <a-descriptions :column="3" size="small" bordered>
                    <a-descriptions-item label="数据源ID">{{ binding.datasourceId }}</a-descriptions-item>
                    <a-descriptions-item label="表名">{{ binding.tableName }}</a-descriptions-item>
                    <a-descriptions-item label="创建时间">{{ binding.createTime }}</a-descriptions-item>
                  </a-descriptions>
                </a-card>
              </a-spin>
            </a-tab-pane>
          </a-tabs>
        </a-card>
        <a-card v-else><a-empty description="请在左侧选择一个概念" /></a-card>
      </a-layout-content>
    </a-layout>
    <!-- 概念对话框 -->
    <a-modal :title="conceptTitle" v-model:open="conceptOpen" width="600px" ok-text="确定" cancel-text="取消" @ok="submitConceptForm" @cancel="conceptOpen = false">
      <a-form ref="conceptRef" :model="conceptForm" :rules="conceptRules" :label-col="{ style: { width: '80px' } }">
        <a-form-item label="概念名称" name="name"><a-input v-model:value="conceptForm.name" placeholder="请输入概念名称" /></a-form-item>
        <a-form-item label="图标" name="icon"><a-input v-model:value="conceptForm.icon" placeholder="图标名称" /></a-form-item>
        <a-form-item label="颜色" name="color"><a-input v-model:value="conceptForm.color" placeholder="颜色值" /></a-form-item>
        <a-form-item label="描述" name="description"><a-textarea v-model:value="conceptForm.description" :auto-size="{ minRows: 2, maxRows: 4 }" /></a-form-item>
      </a-form>
    </a-modal>
    <!-- 属性对话框 -->
    <a-modal :title="propertyTitle" v-model:open="propertyOpen" width="600px" ok-text="确定" cancel-text="取消" @ok="submitPropertyForm" @cancel="propertyOpen = false">
      <a-form ref="propertyRef" :model="propertyForm" :rules="propertyRules" :label-col="{ style: { width: '80px' } }">
        <a-form-item label="属性名称" name="name"><a-input v-model:value="propertyForm.name" placeholder="请输入属性名称" /></a-form-item>
        <a-form-item label="数据类型" name="dataType">
          <a-select v-model:value="propertyForm.dataType" placeholder="请选择数据类型">
            <a-select-option value="string">string</a-select-option>
            <a-select-option value="integer">integer</a-select-option>
            <a-select-option value="decimal">decimal</a-select-option>
            <a-select-option value="date">date</a-select-option>
            <a-select-option value="boolean">boolean</a-select-option>
            <a-select-option value="text">text</a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="主键"><a-switch v-model:checked="propertyForm.isPrimary" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="必填"><a-switch v-model:checked="propertyForm.isRequired" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="默认值"><a-input v-model:value="propertyForm.defaultValue" placeholder="默认值" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="propertyForm.description" :auto-size="{ minRows: 2, maxRows: 4 }" /></a-form-item>
      </a-form>
    </a-modal>
    <!-- 关系对话框 -->
    <a-modal :title="relationTitle" v-model:open="relationOpen" width="600px" ok-text="确定" cancel-text="取消" @ok="submitRelationForm" @cancel="relationOpen = false">
      <a-form ref="relationRef" :model="relationForm" :rules="relationRules" :label-col="{ style: { width: '100px' } }">
        <a-form-item label="关系名称" name="name"><a-input v-model:value="relationForm.name" placeholder="请输入关系名称" /></a-form-item>
        <a-form-item label="关系类型" name="relationType">
          <a-select v-model:value="relationForm.relationType" placeholder="请选择">
            <a-select-option value="one_to_one">一对一</a-select-option>
            <a-select-option value="one_to_many">一对多</a-select-option>
            <a-select-option value="many_to_one">多对一</a-select-option>
            <a-select-option value="many_to_many">多对多</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="源概念" name="sourceConceptId">
          <a-select v-model:value="relationForm.sourceConceptId" :options="conceptOptions" show-search :filter-option="filterOption" />
        </a-form-item>
        <a-form-item label="目标概念" name="targetConceptId">
          <a-select v-model:value="relationForm.targetConceptId" :options="conceptOptions" show-search :filter-option="filterOption" />
        </a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="relationForm.description" :auto-size="{ minRows: 2, maxRows: 4 }" /></a-form-item>
      </a-form>
    </a-modal>
    <!-- 表绑定对话框 -->
    <a-modal :title="bindingTitle" v-model:open="bindingOpen" width="600px" ok-text="确定" cancel-text="取消" @ok="submitBindingForm" @cancel="bindingOpen = false">
      <a-form ref="bindingRef" :model="bindingForm" :rules="bindingRules" :label-col="{ style: { width: '80px' } }">
        <a-form-item label="数据源" name="datasourceId">
          <a-select v-model:value="bindingForm.datasourceId" placeholder="请选择数据源" show-search :filter-option="filterOption" @change="onDatasourceChange">
            <a-select-option v-for="ds in datasourceList" :key="ds.id" :value="ds.id" :label="ds.name">{{ ds.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据库" name="databaseName">
          <a-select v-model:value="bindingForm.databaseName" placeholder="请选择数据库" :loading="schemaLoading" :disabled="!bindingForm.datasourceId">
            <a-select-option v-for="db in databaseList" :key="db" :value="db">{{ db }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Schema" name="schemaName">
          <a-select v-model:value="bindingForm.schemaName" placeholder="请选择Schema" :loading="schemaLoading" :disabled="!bindingForm.databaseName" allowClear>
            <a-select-option v-for="s in schemaList" :key="s" :value="s">{{ s }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="表名" name="tableName">
          <a-select v-model:value="bindingForm.tableName" placeholder="请选择表" show-search :filter-option="filterOption" :loading="tableLoading" :disabled="!bindingForm.databaseName">
            <a-select-option v-for="t in tableList" :key="t" :value="t">{{ t }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 列映射对话框 -->
    <a-modal title="属性列映射" v-model:open="columnMappingOpen" width="800px" ok-text="保存" cancel-text="取消" @ok="submitColumnMapping" @cancel="columnMappingOpen = false">
      <p style="margin-bottom: 12px; color: #666">为当前绑定表 <strong>{{ currentBinding?.tableName }}</strong> 的属性配置物理列映射</p>
      <a-table :columns="columnMappingColumns" :data-source="columnMappingData" row-key="propertyId" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'propertyName'">{{ record.propertyName }}</template>
          <template v-if="column.key === 'columnName'">
            <a-select v-model:value="record.columnName" placeholder="选择物理列" style="width: 200px" show-search :filter-option="filterOption" :loading="tableColumnsLoading">
              <a-select-option v-for="col in availableColumns" :key="col" :value="col">{{ col }}</a-select-option>
            </a-select>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>
<script setup name="ConceptManage">
import { listConcept, getConcept, addConcept, updateConcept, delConcept } from '@/api/ont/concept'
import { listProperty, addProperty, updateProperty, delProperty } from '@/api/ont/property'
import { listRelation, addRelation, updateRelation, delRelation } from '@/api/ont/relation'
import { listConceptTable, addConceptTable, delConceptTable } from '@/api/ont/conceptTable'
import { listPropertyColumn, batchSavePropertyColumns } from '@/api/ont/propertyColumn'
import { listDppDatasource } from '@/api/col/datasource/datasource'
import { getTables, getTableSchema, getColumns } from '@/api/ast/autoFill/metadata-query'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { genCode } from '@/utils/codeGen'

const { proxy } = getCurrentInstance()
const route = useRoute()
const ontologyId = computed(() => route.params.ontologyId)

// === Concept state ===
const treeLoading = ref(false)
const conceptTree = ref([])
const searchValue = ref('')
const selectedKeys = ref([])
const selectedConcept = ref(null)
const conceptOpen = ref(false)
const conceptTitle = ref('新增概念')
const conceptOptions = ref([])
const activeTab = ref('properties')

// === Property state ===
const propertyList = ref([])
const propertyLoading = ref(false)
const propertyOpen = ref(false)
const propertyTitle = ref('新增属性')

// === Relation state ===
const relationList = ref([])
const relationLoading = ref(false)
const relationOpen = ref(false)
const relationTitle = ref('新增关系')

// === Binding state ===
const bindingList = ref([])
const bindingLoading = ref(false)
const bindingOpen = ref(false)
const bindingTitle = ref('新增绑定')
const datasourceList = ref([])
const databaseList = ref([])
const schemaList = ref([])
const tableList = ref([])
const schemaLoading = ref(false)
const tableLoading = ref(false)

// === Column mapping state ===
const columnMappingOpen = ref(false)
const columnMappingData = ref([])
const currentBinding = ref(null)
const availableColumns = ref([])
const tableColumnsLoading = ref(false)

const propertyColumns = [
  { title: '属性名称', dataIndex: 'name', align: 'center', width: 150 },
  { title: '数据类型', dataIndex: 'dataType', align: 'center', width: 100, key: 'dataType' },
  { title: '主键', align: 'center', width: 80, key: 'isPrimary' },
  { title: '必填', align: 'center', width: 80, key: 'isRequired' },
  { title: '默认值', dataIndex: 'defaultValue', align: 'center', width: 120 },
  { title: '排序', dataIndex: 'sortOrder', align: 'center', width: 80 },
  { title: '操作', key: 'action', align: 'center', width: 160, fixed: 'right' }
]

const relationColumns = [
  { title: '关系名称', dataIndex: 'name', align: 'center', width: 150 },
  { title: '关系类型', dataIndex: 'relationType', align: 'center', width: 120, key: 'relationType' },
  { title: '排序', dataIndex: 'sortOrder', align: 'center', width: 80 },
  { title: '描述', dataIndex: 'description', align: 'left', ellipsis: true },
  { title: '操作', key: 'action', align: 'center', width: 160, fixed: 'right' }
]

const columnMappingColumns = [
  { title: '属性名称', key: 'propertyName', align: 'center', width: 150 },
  { title: '物理列名', key: 'columnName', align: 'center', width: 220 }
]

const data = reactive({
  conceptForm: {},
  conceptRules: {
    name: [{ required: true, message: '概念名称不能为空', trigger: 'blur' }]
  },
  propertyForm: {},
  propertyRules: {
    name: [{ required: true, message: '属性名称不能为空', trigger: 'blur' }],
    dataType: [{ required: true, message: '请选择数据类型', trigger: 'change' }]
  },
  relationForm: {},
  relationRules: {
    name: [{ required: true, message: '关系名称不能为空', trigger: 'blur' }],
    relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }],
    sourceConceptId: [{ required: true, message: '请选择源概念', trigger: 'change' }],
    targetConceptId: [{ required: true, message: '请选择目标概念', trigger: 'change' }]
  },
  bindingForm: {},
  bindingRules: {
    datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
    tableName: [{ required: true, message: '请选择表', trigger: 'change' }]
  }
})
const { conceptForm, conceptRules, propertyForm, propertyRules, relationForm, relationRules, bindingForm, bindingRules } = toRefs(data)

function filterOption(input, option) {
  if (!option || !option.label) return false
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

function relationTypeText(type) {
  return { one_to_one: '一对一', one_to_many: '一对多', many_to_one: '多对一', many_to_many: '多对多' }[type] || type
}

function relationTypeColor(type) {
  return { one_to_one: 'blue', one_to_many: 'green', many_to_one: 'orange', many_to_many: 'purple' }[type] || 'default'
}

// === Concept CRUD ===
function loadConcepts() {
  treeLoading.value = true
  listConcept({ ontologyId: ontologyId.value, pageNum: 1, pageSize: 1000 }).then(res => {
    const rows = res.data?.rows || res.data || []
    conceptTree.value = rows
    conceptOptions.value = rows.map(c => ({ value: c.id, label: c.name }))
    treeLoading.value = false
  })
}

function onSelectConcept(keys) {
  if (!keys.length) return
  selectedKeys.value = keys
  const id = keys[0]
  getConcept(id).then(res => {
    selectedConcept.value = res.data
    loadProperties(id)
    loadRelations(id)
    loadBindings(id)
  })
}

function handleAddConcept() {
  data.conceptForm = { ontologyId: ontologyId.value, name: undefined, code: undefined, status: 0, sortOrder: 0 }
  conceptTitle.value = '新增概念'
  conceptOpen.value = true
}

function submitConceptForm() {
  proxy.$refs['conceptRef'].validate().then(() => {
    if (!conceptForm.value.code) conceptForm.value.code = genCode('cpt')
    const fn = conceptForm.value.id ? updateConcept : addConcept
    fn(conceptForm.value).then(() => {
      message.success(conceptForm.value.id ? '修改成功' : '新增成功')
      conceptOpen.value = false
      loadConcepts()
    })
  }).catch(() => {})
}

// === Property CRUD ===
function loadProperties(conceptId) {
  propertyLoading.value = true
  listProperty({ conceptId, pageNum: 1, pageSize: 1000 }).then(res => {
    propertyList.value = res.data?.rows || res.data || []
    propertyLoading.value = false
  })
}

function handleAddProperty() {
  data.propertyForm = { conceptId: selectedConcept.value?.id, name: undefined, code: undefined, dataType: 'string', isPrimary: false, isRequired: false, sortOrder: 0 }
  propertyTitle.value = '新增属性'
  propertyOpen.value = true
}

function handleUpdateProperty(row) {
  data.propertyForm = { ...row }
  propertyTitle.value = '修改属性'
  propertyOpen.value = true
}

function submitPropertyForm() {
  proxy.$refs['propertyRef'].validate().then(() => {
    if (!propertyForm.value.code) propertyForm.value.code = genCode('prop')
    const fn = propertyForm.value.id ? updateProperty : addProperty
    fn(propertyForm.value).then(() => {
      message.success(propertyForm.value.id ? '修改成功' : '新增成功')
      propertyOpen.value = false
      loadProperties(selectedConcept.value.id)
    })
  }).catch(() => {})
}

function handleDeleteProperty(row) {
  proxy.$modal.confirm('是否确认删除属性"' + row.name + '"？').then(() => delProperty(row.id)).then(() => {
    message.success('删除成功')
    loadProperties(selectedConcept.value.id)
  }).catch(() => {})
}

// === Relation CRUD ===
function loadRelations(conceptId) {
  relationLoading.value = true
  listRelation({ ontologyId: ontologyId.value, pageNum: 1, pageSize: 1000 }).then(res => {
    const rows = res.data?.rows || res.data || []
    relationList.value = rows.filter(r => r.sourceConceptId === conceptId || r.targetConceptId === conceptId)
    relationLoading.value = false
  })
}

function handleAddRelation() {
  data.relationForm = { ontologyId: ontologyId.value, name: undefined, code: undefined, relationType: 'one_to_many', sourceConceptId: selectedConcept.value?.id, sortOrder: 0 }
  relationTitle.value = '新增关系'
  relationOpen.value = true
}

function handleUpdateRelation(row) {
  data.relationForm = { ...row }
  relationTitle.value = '修改关系'
  relationOpen.value = true
}

function submitRelationForm() {
  proxy.$refs['relationRef'].validate().then(() => {
    if (!relationForm.value.code) relationForm.value.code = genCode('rel')
    const fn = relationForm.value.id ? updateRelation : addRelation
    fn(relationForm.value).then(() => {
      message.success(relationForm.value.id ? '修改成功' : '新增成功')
      relationOpen.value = false
      loadRelations(selectedConcept.value.id)
    })
  }).catch(() => {})
}

function handleDeleteRelation(row) {
  proxy.$modal.confirm('是否确认删除关系"' + row.name + '"？').then(() => delRelation(row.id)).then(() => {
    message.success('删除成功')
    loadRelations(selectedConcept.value.id)
  }).catch(() => {})
}

// === Binding CRUD ===
function loadBindings(conceptId) {
  bindingLoading.value = true
  listConceptTable({ conceptId }).then(res => {
    bindingList.value = res.data || []
    bindingLoading.value = false
  }).catch(() => { bindingLoading.value = false })
}

function loadDatasources() {
  listDppDatasource({}).then(res => {
    datasourceList.value = res.data || []
  })
}

function handleAddBinding() {
  data.bindingForm = { conceptId: selectedConcept.value?.id, datasourceId: undefined, databaseName: undefined, schemaName: undefined, tableName: undefined }
  databaseList.value = []
  schemaList.value = []
  tableList.value = []
  bindingTitle.value = '新增绑定'
  bindingOpen.value = true
  loadDatasources()
}

function onDatasourceChange(dsId) {
  data.bindingForm.databaseName = undefined
  data.bindingForm.schemaName = undefined
  data.bindingForm.tableName = undefined
  databaseList.value = []
  schemaList.value = []
  tableList.value = []
  if (!dsId) return
  schemaLoading.value = true
  getTableSchema({ datasourceId: dsId }).then(res => {
    databaseList.value = res.data || []
    schemaLoading.value = false
  }).catch(() => { schemaLoading.value = false })
}

function submitBindingForm() {
  proxy.$refs['bindingRef'].validate().then(() => {
    addConceptTable(bindingForm.value).then(() => {
      message.success('绑定成功')
      bindingOpen.value = false
      loadBindings(selectedConcept.value.id)
    })
  }).catch(() => {})
}

function handleDeleteBinding(row) {
  proxy.$modal.confirm('是否确认删除表绑定"' + row.tableName + '"？').then(() => delConceptTable(row.id)).then(() => {
    message.success('删除成功')
    loadBindings(selectedConcept.value.id)
  }).catch(() => {})
}

// === Column Mapping ===
function handleColumnMapping(binding) {
  currentBinding.value = binding
  columnMappingData.value = []
  availableColumns.value = []
  columnMappingOpen.value = true
  // Load existing mappings
  listPropertyColumn(binding.id).then(res => {
    const existing = res.data || []
    // Build mapping data from properties
    const propertyMap = {}
    propertyList.value.forEach(p => { propertyMap[p.id] = p })
    columnMappingData.value = propertyList.value.map(p => {
      const matched = existing.find(e => e.propertyId === p.id)
      return {
        propertyId: p.id,
        propertyName: p.name,
        propertyCode: p.code,
        columnName: matched ? matched.columnName : undefined
      }
    })
  })
  // Load available columns
  if (binding.datasourceId && binding.tableName) {
    tableColumnsLoading.value = true
    getColumns({ datasourceId: binding.datasourceId, databaseName: binding.databaseName, tableName: binding.tableName }).then(res => {
      availableColumns.value = (res.data || []).map(c => c.columnName || c.name)
      tableColumnsLoading.value = false
    }).catch(() => { tableColumnsLoading.value = false })
  }
}

function submitColumnMapping() {
  const mappings = columnMappingData.value.filter(m => m.columnName).map(m => ({
    propertyId: m.propertyId,
    conceptTableId: currentBinding.value.id,
    columnName: m.columnName
  }))
  batchSavePropertyColumns(currentBinding.value.id, mappings).then(() => {
    message.success('列映射保存成功')
    columnMappingOpen.value = false
  })
}

loadConcepts()
</script>
