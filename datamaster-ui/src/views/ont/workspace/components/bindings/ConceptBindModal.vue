<template>
  <a-modal title="绑定数据表" v-model:open="visible" width="640px" destroy-on-close :footer="null">
    <p class="modal-hint">概念「{{ conceptName }}」当前绑定的物理表：</p>
    <div class="bind-list">
      <div v-for="b in bindRows" :key="b.id" class="bind-item">
        <span class="bind-name">{{ b.tableName }}</span>
        <span class="bind-ds">{{ dsName(b.datasourceId) }}</span>
        <a-button type="link" danger size="small" @click="deleteBind(b)">解除绑定</a-button>
      </div>
      <div v-if="!bindLoading && !bindRows.length" class="bind-empty">暂无绑定，请在下方选择数据源与表进行绑定</div>
    </div>
    <a-divider style="margin: 12px 0">新增绑定</a-divider>
    <div class="bind-form">
      <a-select
        v-model:value="bindForm.datasourceId"
        :options="dsOptions"
        placeholder="请选择数据源"
        style="width: 250px"
        show-search
        option-filter-prop="label"
        @change="handleBindDsChange"
      />
      <a-select
        v-model:value="bindForm.tableName"
        :options="bindTableOptions"
        placeholder="请选择表"
        style="width: 230px"
        show-search
        option-filter-prop="label"
      />
      <a-button type="primary" :loading="bindSaving" @click="submitBind">绑定</a-button>
    </div>
  </a-modal>
</template>

<script setup name="ConceptBindModal">
import { listConceptTable, addConceptTable, delConceptTable } from '@/api/ont/conceptTable'
import { getDaDatasourceList } from '@/api/ast/dataSource/dataSource'
import { getCatalogTableListAsset } from '@/api/cat/unreleased/table'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  conceptId: {
    type: String,
    default: ''
  },
  conceptName: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const { proxy } = getCurrentInstance()
const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

const bindLoading = ref(false)
const bindSaving = ref(false)
const bindRows = ref([])
let dsCache = []
const dsOptions = ref([])
const bindForm = ref({})
const bindTableOptions = ref([])

// 统一解析响应数据：list 接口可能返回数组或分页对象
function rowsOf(res) {
  return Array.isArray(res.data) ? res.data : (res.data?.rows || [])
}

function dsName(datasourceId) {
  const hit = dsCache.find(d => d.id === datasourceId)
  return hit ? hit.datasourceName : ''
}

watch(visible, val => {
  if (!val || !props.conceptId) return
  bindForm.value = {}
  bindTableOptions.value = []
  // 已有绑定列表
  bindLoading.value = true
  listConceptTable({ conceptId: props.conceptId }).then(res => {
    bindRows.value = rowsOf(res)
  }).finally(() => {
    bindLoading.value = false
  })
  // 数据源下拉只加载一次
  if (!dsCache.length) {
    getDaDatasourceList().then(res => {
      dsCache = rowsOf(res)
      dsOptions.value = dsCache.map(d => ({ value: d.id, label: d.datasourceName }))
    })
  }
})

function handleBindDsChange(dsId) {
  bindForm.value = { datasourceId: dsId }
  bindTableOptions.value = []
  if (!dsId) return
  // 表候选改为元数据模块已发布的登记表
  getCatalogTableListAsset({ datasourceId: dsId, status: '1' }).then(res => {
    bindTableOptions.value = rowsOf(res).map(t => ({
      value: t.tableName,
      label: t.tableComment ? `${t.tableName} (${t.tableComment})` : t.tableName
    })).filter(t => !!t.value)
  }).catch(() => {
    proxy.$modal.msgError('获取表列表失败')
  })
}

function submitBind() {
  if (!bindForm.value.datasourceId || !bindForm.value.tableName) {
    proxy.$modal.msgError('请先选择数据源和数据表')
    return
  }
  bindSaving.value = true
  addConceptTable({
    conceptId: props.conceptId,
    datasourceId: bindForm.value.datasourceId,
    tableName: bindForm.value.tableName
  }).then(() => {
    proxy.$modal.msgSuccess('绑定成功')
    emit('success')
    return refreshBindRows()
  }).finally(() => {
    bindSaving.value = false
  })
}

function deleteBind(row) {
  delConceptTable(row.id).then(() => {
    proxy.$modal.msgSuccess('已解除绑定')
    emit('success')
    return refreshBindRows()
  })
}

function refreshBindRows() {
  return listConceptTable({ conceptId: props.conceptId }).then(res => {
    bindRows.value = rowsOf(res)
  })
}
</script>

<style lang="scss" scoped>
.modal-hint {
  color: #999;
  font-size: 12px;
  margin: 0 0 8px;
}

.bind-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 6px;

  .bind-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 10px;

    & + .bind-item {
      border-top: 1px solid #f5f5f5;
    }

    .bind-name {
      font-weight: 500;
    }

    .bind-ds {
      color: #999;
      font-size: 12px;
      flex: 1;
    }
  }

  .bind-empty {
    padding: 16px;
    text-align: center;
    color: #bbb;
  }
}

.bind-form {
  display: flex;
  gap: 8px;
}
</style>
