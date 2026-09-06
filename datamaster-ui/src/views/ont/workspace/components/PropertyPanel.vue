<template>
  <div class="property-panel">
    <!-- 左侧：概念列表作为数据目录 -->
    <div class="catalog-side">
      <div class="catalog-header">概念目录</div>
      <div class="catalog-search">
        <a-input
          v-model:value="catalogKeyword"
          placeholder="搜索概念"
          allow-clear
          size="small"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
      </div>
      <div class="catalog-list">
        <div
          v-for="c in filteredConcepts"
          :key="c.value"
          class="catalog-item"
          :class="{ active: c.value === queryParams.conceptId }"
          @click="selectConcept(c.value)"
        >
          {{ c.label }}
        </div>
        <a-empty
          v-if="!filteredConcepts.length"
          :image="Empty.PRESENTED_IMAGE_SIMPLE"
          :description="conceptOptions.length ? '无匹配概念' : '暂无概念，请先在「概念管理」中创建'"
        />
      </div>
    </div>

    <!-- 右侧：属性列表主区 -->
    <div class="property-main">
      <!-- 工具条：属性名搜索 + 新增 -->
      <div class="panel-toolbar">
        <span class="current-concept">{{ currentConceptName }}</span>
        <a-input
          v-model:value="queryParams.name"
          placeholder="请输入属性名称"
          allow-clear
          style="width: 180px"
          @keyup.enter="handleQuery"
          @pressEnter="handleQuery"
        />
        <a-button type="primary" @click="handleQuery">查询</a-button>
        <a-button @click="resetQuery">重置</a-button>
        <a-button type="primary" class="toolbar-right" @click="handleAdd" v-hasPermi="['ont:property:add']">
          <template #icon><PlusOutlined /></template>
          新增属性
        </a-button>
      </div>

    <a-table
      :columns="columns"
      :data-source="propertyList"
      :loading="loading"
      row-key="id"
      size="middle"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'dataType'">
          <a-tag color="blue">{{ record.dataType }}</a-tag>
        </template>
        <template v-else-if="column.key === 'isPrimary'">
          <a-badge :status="record.isPrimary ? 'success' : 'default'" :text="record.isPrimary ? '是' : '否'" />
        </template>
        <template v-else-if="column.key === 'isRequired'">
          <a-badge :status="record.isRequired ? 'success' : 'default'" :text="record.isRequired ? '是' : '否'" />
        </template>
        <template v-else-if="column.dataIndex === 'defaultValue'">
          {{ record.defaultValue || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openFieldBind(record)" v-hasPermi="['ont:property:edit']">绑定字段</a-button>
          <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['ont:property:edit']">修改</a-button>
          <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['ont:property:remove']">删除</a-button>
        </template>
      </template>
      <template #empty>
        <div class="emptyBg">
          <img src="@/assets/system/images/no_data/noData.png" alt="" />
          <p>{{ queryParams.conceptId ? '暂无属性，点击右上角「新增属性」创建' : '请先在左侧选择概念查看属性' }}</p>
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

    <!-- 属性对话框 -->
    <a-modal :title="title" v-model:open="open" width="600px" destroy-on-close ok-text="确定" cancel-text="取消" @ok="submitForm" @cancel="cancel">
      <a-form ref="propertyRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
        <a-form-item label="所属概念" name="conceptId">
          <a-select
            v-model:value="form.conceptId"
            :options="conceptOptions"
            placeholder="请选择所属概念"
            show-search
            :filter-option="filterOption"
          />
        </a-form-item>
        <a-form-item label="属性名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入属性名称" />
        </a-form-item>
        <a-form-item label="数据类型" name="dataType">
          <a-select v-model:value="form.dataType" placeholder="请选择数据类型">
            <a-select-option value="string">string</a-select-option>
            <a-select-option value="integer">integer</a-select-option>
            <a-select-option value="decimal">decimal</a-select-option>
            <a-select-option value="date">date</a-select-option>
            <a-select-option value="boolean">boolean</a-select-option>
            <a-select-option value="text">text</a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="必填" name="isRequired">
              <a-switch v-model:checked="form.isRequired" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="默认值" name="defaultValue">
              <a-input v-model:value="form.defaultValue" placeholder="默认值" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序" name="sortOrder">
              <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" placeholder="排序号" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="form.description" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="请输入描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 属性绑定字段对话框 -->
    <FieldBindModal v-model="fbOpen" :concept-id="fbConcept.id" :concept-name="fbConcept.name" @success="getList" />
    </div>
  </div>
</template>

<script setup name="PropertyPanel">
import { listConcept } from '@/api/ont/concept'
import { listProperty, getProperty, addProperty, updateProperty, delProperty } from '@/api/ont/property'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { Empty } from 'ant-design-vue'
import { genCode } from '@/utils/codeGen'
import FieldBindModal from './bindings/FieldBindModal.vue'

const props = defineProps({
  ontologyId: {
    type: [Number, String],
    required: true
  }
})

const { proxy } = getCurrentInstance()
const loading = ref(false)
const propertyList = ref([])
const total = ref(0)
const open = ref(false)
const title = ref('')
const conceptOptions = ref([])
const catalogKeyword = ref('')

// 属性绑定字段状态（逻辑内聚在 FieldBindModal 中）
const fbOpen = ref(false)
const fbConcept = ref({})

const columns = [
  { title: '属性名称', dataIndex: 'name', align: 'left', width: 150 },
  { title: '数据类型', key: 'dataType', align: 'center', width: 100 },
  { title: '主属性', key: 'isPrimary', align: 'center', width: 90 },
  { title: '必填', key: 'isRequired', align: 'center', width: 80 },
  { title: '默认值', dataIndex: 'defaultValue', align: 'center', width: 120 },
  { title: '排序', dataIndex: 'sortOrder', align: 'center', width: 70 },
  { title: '描述', dataIndex: 'description', align: 'left', ellipsis: true },
  { title: '操作', key: 'action', align: 'center', width: 210, fixed: 'right' }
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  conceptId: undefined,
  name: undefined
})

const data = reactive({
  form: {},
  rules: {
    conceptId: [{ required: true, message: '请选择所属概念', trigger: 'change' }],
    name: [{ required: true, message: '属性名称不能为空', trigger: 'blur' }],
    dataType: [{ required: true, message: '请选择数据类型', trigger: 'change' }]
  }
})
const { form, rules } = toRefs(data)

function filterOption(input, option) {
  if (!option || !option.label) return false
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

// 左侧概念目录：按关键字过滤
const filteredConcepts = computed(() => {
  const kw = (catalogKeyword.value || '').trim().toLowerCase()
  if (!kw) return conceptOptions.value
  return conceptOptions.value.filter(c => c.label.toLowerCase().indexOf(kw) >= 0)
})

// 当前选中概念名（右侧标题展示）
const currentConceptName = computed(() => {
  const hit = conceptOptions.value.find(c => c.value === queryParams.conceptId)
  return hit ? hit.label : '未选择概念'
})

// 点击左侧目录切换概念范围
function selectConcept(id) {
  if (queryParams.conceptId === id) return
  queryParams.conceptId = id
  queryParams.pageNum = 1
  getList()
}

// 加载当前本体下的概念（用于过滤下拉和表单选择）
function loadConcepts() {
  listConcept({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 }).then(res => {
    const rows = res.data?.rows || []
    conceptOptions.value = rows.map(c => ({ value: c.id, label: c.name }))
    // 默认选中第一个概念，保证列表按概念范围查询
    if (conceptOptions.value.length && !queryParams.conceptId) {
      queryParams.conceptId = conceptOptions.value[0].value
    }
    getList()
  })
}

function getList() {
  // 属性挂在概念下，未选择概念时不发起全量查询
  if (!queryParams.conceptId) {
    propertyList.value = []
    total.value = 0
    return
  }
  loading.value = true
  listProperty(queryParams).then(res => {
    propertyList.value = res.data?.rows || []
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
  if (conceptOptions.value.length) {
    queryParams.conceptId = conceptOptions.value[0].value
  } else {
    queryParams.conceptId = undefined
  }
  handleQuery()
}

function reset() {
  form.value = {
    conceptId: queryParams.conceptId,
    name: undefined,
    dataType: 'string',
    isPrimary: false,
    isRequired: false,
    defaultValue: undefined,
    sortOrder: 0,
    description: undefined
  }
  proxy.resetForm('propertyRef')
}

function handleAdd() {
  // 打开弹窗前刷新概念下拉和列表范围，避免在其他面板新增概念后选项过期
  listConcept({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 }).then(res => {
    const rows = res.data?.rows || []
    conceptOptions.value = rows.map(c => ({ value: c.id, label: c.name }))
    if (!conceptOptions.value.length) {
      proxy.$modal.msgError('请先在「概念管理」中创建概念')
      return
    }
    if (!queryParams.conceptId || !conceptOptions.value.some(c => c.value === queryParams.conceptId)) {
      // 当前过滤器指向的概念已不存在时，回落到第一个概念
      queryParams.conceptId = conceptOptions.value[0].value
      getList()
    }
    reset()
    open.value = true
    title.value = '新增属性'
  })
}

function handleUpdate(row) {
  reset()
  getProperty(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改属性'
  })
}

function submitForm() {
  proxy.$refs['propertyRef'].validate().then(() => {
    if (!form.value.code) form.value.code = genCode('prop')
    const isAdd = !form.value.id
    const fn = isAdd ? addProperty : updateProperty
    fn(form.value).then(() => {
      proxy.$modal.msgSuccess(isAdd ? '新增成功' : '修改成功')
      open.value = false
      if (isAdd) {
        // 新增后把过滤器切到新属性所属概念，保证立即可见
        queryParams.conceptId = form.value.conceptId
        queryParams.pageNum = 1
      }
      getList()
    })
  }).catch(() => {})
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除属性"' + row.name + '"？').then(() => {
    return delProperty(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function cancel() {
  open.value = false
  reset()
}

/* ================= 属性绑定物理字段 ================= */

function conceptNameById(conceptId) {
  const hit = conceptOptions.value.find(c => c.value === conceptId)
  return hit ? hit.label : ''
}

function openFieldBind(record) {
  fbConcept.value = { id: record.conceptId, name: conceptNameById(record.conceptId) }
  fbOpen.value = true
}

defineExpose({ reload: loadConcepts })

loadConcepts()
</script>

<style lang="scss" scoped>
.property-panel {
  display: flex;
  gap: 12px;
  align-items: flex-start;

  // 左侧概念目录
  .catalog-side {
    width: 220px;
    flex-shrink: 0;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 8px;
    overflow: hidden;

    .catalog-header {
      padding: 10px 12px;
      font-weight: 600;
      color: #333;
      border-bottom: 1px solid #f0f0f0;
      background: #fafafa;
    }

    .catalog-search {
      padding: 8px;
      border-bottom: 1px solid #f5f5f5;
    }

    .catalog-list {
      max-height: 560px;
      overflow-y: auto;

      .catalog-item {
        padding: 8px 12px;
        cursor: pointer;
        font-size: 13px;
        color: #333;
        border-left: 3px solid transparent;
        transition: background-color 0.2s;

        &:hover {
          background: #f5f8ff;
        }

        &.active {
          color: #1677ff;
          background: #e6f7ff;
          border-left-color: #1677ff;
          font-weight: 600;
        }
      }
    }
  }

  // 右侧属性主区
  .property-main {
    flex: 1;
    min-width: 0;

    .panel-toolbar {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;

      .current-concept {
        font-weight: 600;
        color: #1677ff;
        margin-right: 4px;
      }

      .toolbar-right {
        margin-left: auto;
      }
    }
  }
}
</style>
