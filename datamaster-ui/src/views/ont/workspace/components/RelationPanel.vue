<template>
  <div class="relation-panel">
    <!-- 工具条：搜索 + 新增 -->
    <div class="panel-toolbar">
      <a-input
        v-model:value="queryParams.name"
        placeholder="请输入关系名称"
        allow-clear
        style="width: 200px"
        @keyup.enter="handleQuery"
      />
      <a-button type="primary" @click="handleQuery">查询</a-button>
      <a-button @click="resetQuery">重置</a-button>
      <a-button type="primary" class="toolbar-right" @click="handleAdd" v-hasPermi="['ont:relation:add']">
        <template #icon><PlusOutlined /></template>
        新增关系
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="relationList"
      :loading="loading"
      row-key="id"
      size="middle"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sourceConceptId'">
          {{ conceptName(record.sourceConceptId) }}
        </template>
        <template v-else-if="column.key === 'targetConceptId'">
          {{ conceptName(record.targetConceptId) }}
        </template>
        <template v-else-if="column.key === 'relationType'">
          <a-tag :color="relationTypeColor(record.relationType)">{{ relationTypeText(record.relationType) }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openRelColumn(record)" v-hasPermi="['ont:relation:edit']">关联表/字段</a-button>
          <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['ont:relation:edit']">修改</a-button>
          <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['ont:relation:remove']">删除</a-button>
        </template>
      </template>
      <template #empty>
        <div class="emptyBg">
          <img src="@/assets/system/images/no_data/noData.png" alt="" />
          <p>暂无关系，点击右上角「新增关系」创建</p>
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

    <!-- 关系对话框 -->
    <a-modal :title="title" v-model:open="open" width="600px" destroy-on-close ok-text="确定" cancel-text="取消" @ok="submitForm" @cancel="cancel">
      <a-form ref="relationRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
        <a-form-item label="关系名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入关系名称" />
        </a-form-item>
        <a-form-item label="关系类型" name="relationType">
          <a-select v-model:value="form.relationType" placeholder="请选择关系类型">
            <a-select-option value="one_to_one">一对一</a-select-option>
            <a-select-option value="one_to_many">一对多</a-select-option>
            <a-select-option value="many_to_one">多对一</a-select-option>
            <a-select-option value="many_to_many">多对多</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="源概念" name="sourceConceptId">
          <a-select
            v-model:value="form.sourceConceptId"
            :options="conceptOptions"
            placeholder="请选择源概念"
            show-search
            :filter-option="filterOption"
          />
        </a-form-item>
        <a-form-item label="目标概念" name="targetConceptId">
          <a-select
            v-model:value="form.targetConceptId"
            :options="conceptOptions"
            placeholder="请选择目标概念"
            show-search
            :filter-option="filterOption"
          />
        </a-form-item>
        <a-form-item label="排序" name="sortOrder">
          <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" placeholder="排序号" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="form.description" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="请输入描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 关联字段映射对话框 -->
    <RelColumnModal
      v-model="rcOpen"
      :relation-id="rcRelation.id"
      :source-concept-id="rcRelation.sourceConceptId"
      :target-concept-id="rcRelation.targetConceptId"
      :relation-name="rcRelation.name"
      @success="getList"
    />
  </div>
</template>

<script setup name="RelationPanel">
import { listConcept } from '@/api/ont/concept'
import { listRelation, getRelation, addRelation, updateRelation, delRelation } from '@/api/ont/relation'
import { PlusOutlined } from '@ant-design/icons-vue'
import { genCode } from '@/utils/codeGen'
import RelColumnModal from './bindings/RelColumnModal.vue'

const props = defineProps({
  ontologyId: {
    type: [Number, String],
    required: true
  }
})

const { proxy } = getCurrentInstance()
const loading = ref(false)
const relationList = ref([])
const total = ref(0)
const open = ref(false)
const title = ref('')
const conceptOptions = ref([])

// 关联字段映射状态（逻辑内聚在 RelColumnModal 中）
const rcOpen = ref(false)
const rcRelation = ref({})

const columns = [
  { title: '关系名称', dataIndex: 'name', align: 'left', width: 150 },
  { title: '源概念', key: 'sourceConceptId', align: 'center', width: 140 },
  { title: '目标概念', key: 'targetConceptId', align: 'center', width: 140 },
  { title: '关系类型', key: 'relationType', align: 'center', width: 110 },
  { title: '描述', dataIndex: 'description', align: 'left', ellipsis: true },
  { title: '操作', key: 'action', align: 'center', width: 210, fixed: 'right' }
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ontologyId: props.ontologyId,
  name: undefined
})

const data = reactive({
  form: {},
  rules: {
    name: [{ required: true, message: '关系名称不能为空', trigger: 'blur' }],
    relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }],
    sourceConceptId: [{ required: true, message: '请选择源概念', trigger: 'change' }],
    targetConceptId: [{ required: true, message: '请选择目标概念', trigger: 'change' }]
  }
})
const { form, rules } = toRefs(data)

function filterOption(input, option) {
  if (!option || !option.label) return false
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

function conceptName(conceptId) {
  const matched = conceptOptions.value.find(c => c.value === conceptId)
  return matched ? matched.label : (conceptId ?? '-')
}

function relationTypeText(type) {
  return { one_to_one: '一对一', one_to_many: '一对多', many_to_one: '多对一', many_to_many: '多对多' }[type] || type
}

function relationTypeColor(type) {
  return { one_to_one: 'blue', one_to_many: 'green', many_to_one: 'orange', many_to_many: 'purple' }[type] || 'default'
}

// 加载当前本体下的概念（用于表格显示概念名和表单下拉）
function loadConcepts() {
  listConcept({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 }).then(res => {
    const rows = res.data?.rows || []
    conceptOptions.value = rows.map(c => ({ value: c.id, label: c.name }))
  })
}

function getList() {
  loading.value = true
  queryParams.ontologyId = props.ontologyId
  listRelation(queryParams).then(res => {
    relationList.value = res.data?.rows || []
    total.value = Number(res.data?.total) || 0
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
  form.value = {
    ontologyId: props.ontologyId,
    name: undefined,
    relationType: 'one_to_many',
    sourceConceptId: undefined,
    targetConceptId: undefined,
    sortOrder: 0,
    description: undefined
  }
  proxy.resetForm('relationRef')
}

function handleAdd() {
  // 打开弹窗前刷新概念下拉，避免在概念面板新增概念后选项过期
  listConcept({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 }).then(res => {
    const rows = res.data?.rows || []
    conceptOptions.value = rows.map(c => ({ value: c.id, label: c.name }))
    if (!conceptOptions.value.length) {
      proxy.$modal.msgError('请先在「概念管理」中创建概念')
      return
    }
    reset()
    open.value = true
    title.value = '新增关系'
  })
}

function handleUpdate(row) {
  reset()
  getRelation(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改关系'
  })
}

function submitForm() {
  proxy.$refs['relationRef'].validate().then(() => {
    form.value.ontologyId = props.ontologyId
    if (!form.value.code) form.value.code = genCode('rel')
    const fn = form.value.id ? updateRelation : addRelation
    fn(form.value).then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  }).catch(() => {})
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除关系"' + row.name + '"？').then(() => {
    return delRelation(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function cancel() {
  open.value = false
  reset()
}

/* ================= 关系关联字段映射 ================= */

function openRelColumn(record) {
  rcRelation.value = {
    id: record.id,
    name: record.name,
    sourceConceptId: record.sourceConceptId,
    targetConceptId: record.targetConceptId
  }
  rcOpen.value = true
}

defineExpose({ reload: () => { loadConcepts(); getList() } })

loadConcepts()
getList()
</script>

<style lang="scss" scoped>
.relation-panel {
  .panel-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    .toolbar-right {
      margin-left: auto;
    }
  }
}
</style>
