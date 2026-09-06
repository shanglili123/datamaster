<template>
  <div class="concept-panel">
    <!-- 工具条：搜索 + 新增 -->
    <div class="panel-toolbar">
      <a-input
        v-model:value="queryParams.name"
        placeholder="请输入概念名称"
        allow-clear
        style="width: 200px"
        @keyup.enter="handleQuery"
      />
      <a-button type="primary" @click="handleQuery">查询</a-button>
      <a-button @click="resetQuery">重置</a-button>
      <a-button type="primary" class="toolbar-right" @click="handleAdd" v-hasPermi="['ont:concept:add']">
        <template #icon><PlusOutlined /></template>
        新增概念
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="conceptList"
      :loading="loading"
      row-key="id"
      size="middle"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'color'">
          <span class="color-cell">
            <span class="color-dot" :style="{ background: record.color || '#d9d9d9' }"></span>
            {{ record.color || '-' }}
          </span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'blue'">{{ record.status === 1 ? '已发布' : '草稿' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openBindTable(record)" v-hasPermi="['ont:concept:edit']">绑定表</a-button>
          <a-button type="link" size="small" @click="openPrimaryProperty(record)" v-hasPermi="['ont:property:edit']">设置主属性</a-button>
          <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['ont:concept:edit']">修改</a-button>
          <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['ont:concept:remove']">删除</a-button>
        </template>
      </template>
      <template #empty>
        <div class="emptyBg">
          <img src="@/assets/system/images/no_data/noData.png" alt="" />
          <p>暂无概念，点击右上角「新增概念」创建</p>
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

    <!-- 概念对话框 -->
    <a-modal :title="title" v-model:open="open" width="600px" destroy-on-close ok-text="确定" cancel-text="取消" @ok="submitForm" @cancel="cancel">
      <a-form ref="conceptRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
        <a-form-item label="概念名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入概念名称" />
        </a-form-item>
        <a-form-item label="图标" name="icon">
          <a-input v-model:value="form.icon" placeholder="图标名称" />
        </a-form-item>
        <a-form-item label="颜色" name="color">
          <div class="color-field">
            <input type="color" class="color-picker" :value="form.color || '#1677ff'" @input="form.color = $event.target.value" />
            <a-input v-model:value="form.color" placeholder="请输入颜色值，如 #1677ff" allow-clear />
          </div>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="排序" name="sortOrder">
              <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" placeholder="排序号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-select v-model:value="form.status" placeholder="请选择状态">
                <a-select-option :value="0">草稿</a-select-option>
                <a-select-option :value="1">已发布</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="form.description" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="请输入描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 概念绑表对话框 -->
    <ConceptBindModal v-model="bindOpen" :concept-id="bindConcept.id" :concept-name="bindConcept.name" @success="getList" />

    <a-modal
      v-model:open="primaryOpen"
      :title="'设置主属性 - ' + (primaryConcept.name || '')"
      width="560px"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="primarySaving"
      @ok="savePrimaryProperty"
    >
      <a-alert
        type="info"
        show-icon
        message="物理表有主键时会在字段映射保存后自动采用；没有物理主键时，可在这里选择稳定且唯一的属性。联合主键可选择多个。"
        style="margin-bottom:12px"
      />
      <a-form-item label="主属性" required>
        <a-select
          v-model:value="primaryPropertyIds"
          mode="multiple"
          :options="primaryPropertyOptions"
          placeholder="请选择已绑定物理字段的属性"
          show-search
          option-filter-prop="label"
        />
      </a-form-item>
      <a-empty v-if="!primaryLoading && !primaryPropertyOptions.length" description="暂无已映射属性，请先绑定表并完成属性字段映射" />
    </a-modal>
  </div>
</template>

<script setup name="ConceptPanel">
import { listConcept, getConcept, addConcept, updateConcept, delConcept } from '@/api/ont/concept'
import { PlusOutlined } from '@ant-design/icons-vue'
import { genCode } from '@/utils/codeGen'
import ConceptBindModal from './bindings/ConceptBindModal.vue'
import { listProperty, setPrimaryProperties } from '@/api/ont/property'
import { listConceptTable } from '@/api/ont/conceptTable'
import { listPropertyColumn } from '@/api/ont/propertyColumn'

const props = defineProps({
  ontologyId: {
    type: [Number, String],
    required: true
  }
})

const { proxy } = getCurrentInstance()
const loading = ref(false)
const conceptList = ref([])
const total = ref(0)
const open = ref(false)
const title = ref('')

// 概念绑表状态（逻辑内聚在 ConceptBindModal 中）
const bindOpen = ref(false)
const bindConcept = ref({})

const columns = [
  { title: '概念名称', dataIndex: 'name', align: 'left', width: 160 },
  { title: '图标', dataIndex: 'icon', align: 'center', width: 100 },
  { title: '颜色', dataIndex: 'color', key: 'color', align: 'center', width: 130 },
  { title: '排序', dataIndex: 'sortOrder', align: 'center', width: 70 },
  { title: '状态', key: 'status', align: 'center', width: 90 },
  { title: '描述', dataIndex: 'description', align: 'left', ellipsis: true },
  { title: '操作', key: 'action', align: 'center', width: 230, fixed: 'right' }
]

const primaryOpen = ref(false)
const primarySaving = ref(false)
const primaryLoading = ref(false)
const primaryConcept = ref({})
const primaryPropertyIds = ref([])
const primaryPropertyOptions = ref([])

function rowsOf(res) {
  return Array.isArray(res.data) ? res.data : (res.data?.rows || [])
}

async function openPrimaryProperty(concept) {
  primaryConcept.value = concept
  primaryPropertyIds.value = []
  primaryPropertyOptions.value = []
  primaryOpen.value = true
  primaryLoading.value = true
  try {
    const [propertyRes, tableRes] = await Promise.all([
      listProperty({ conceptId: concept.id, pageNum: 1, pageSize: 1000 }),
      listConceptTable({ conceptId: concept.id })
    ])
    const properties = rowsOf(propertyRes)
    const tables = rowsOf(tableRes)
    const mappedIds = new Set()
    for (const table of tables) {
      const mappingRes = await listPropertyColumn(table.id)
      rowsOf(mappingRes).forEach(mapping => mappedIds.add(String(mapping.propertyId)))
    }
    primaryPropertyOptions.value = properties
      .filter(property => mappedIds.has(String(property.id)))
      .map(property => ({
        value: property.id,
        label: `${property.name}（${property.code}）${property.isPrimary ? ' · 当前主属性' : ''}`
      }))
    primaryPropertyIds.value = properties
      .filter(property => property.isPrimary && mappedIds.has(String(property.id)))
      .map(property => property.id)
  } finally {
    primaryLoading.value = false
  }
}

async function savePrimaryProperty() {
  if (!primaryPropertyIds.value.length) {
    proxy.$modal.msgError('请至少选择一个主属性')
    return
  }
  primarySaving.value = true
  try {
    await setPrimaryProperties({
      conceptId: primaryConcept.value.id,
      propertyIds: primaryPropertyIds.value
    })
    proxy.$modal.msgSuccess('主属性已更新')
    primaryOpen.value = false
  } finally {
    primarySaving.value = false
  }
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ontologyId: props.ontologyId,
  name: undefined
})

const data = reactive({
  form: {},
  rules: {
    name: [{ required: true, message: '概念名称不能为空', trigger: 'blur' }]
  }
})
const { form, rules } = toRefs(data)

function getList() {
  loading.value = true
  queryParams.ontologyId = props.ontologyId
  listConcept(queryParams).then(res => {
    conceptList.value = res.data?.rows || []
    total.value = res.data?.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.name = undefined
  handleQuery()
}

function reset() {
  form.value = { ontologyId: props.ontologyId, name: undefined, code: undefined, icon: undefined, color: undefined, sortOrder: 0, status: 0, description: undefined }
  proxy.resetForm('conceptRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增概念'
}

function handleUpdate(row) {
  reset()
  getConcept(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改概念'
  })
}

function submitForm() {
  proxy.$refs['conceptRef'].validate().then(() => {
    form.value.ontologyId = props.ontologyId
    if (!form.value.code) form.value.code = genCode('cpt')
    const fn = form.value.id ? updateConcept : addConcept
    fn(form.value).then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  }).catch(() => {})
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除概念"' + row.name + '"？').then(() => {
    return delConcept(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function cancel() {
  open.value = false
  reset()
}

/* ================= 概念绑定物理表 ================= */

function openBindTable(row) {
  bindConcept.value = { id: row.id, name: row.name }
  bindOpen.value = true
}

defineExpose({ reload: getList })

getList()
</script>

<style lang="scss" scoped>
.concept-panel {
  .panel-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    .toolbar-right {
      margin-left: auto;
    }
  }

  .color-cell {
    display: inline-flex;
    align-items: center;
    gap: 6px;

    .color-dot {
      display: inline-block;
      width: 14px;
      height: 14px;
      border-radius: 3px;
      border: 1px solid #e5eaf2;
    }
  }

  .color-field {
    display: flex;
    align-items: center;
    gap: 8px;

    .color-picker {
      width: 32px;
      height: 32px;
      padding: 0;
      border: 1px solid #d9d9d9;
      border-radius: 6px;
      background: #ffffff;
      cursor: pointer;

      &::-webkit-color-swatch-wrapper {
        padding: 2px;
      }

      &::-webkit-color-swatch {
        border: none;
        border-radius: 4px;
      }
    }
  }
}
</style>
