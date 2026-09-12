<template>
  <div class="app-container ontology-page">
    <!-- 顶部搜索 + 新增按钮 -->
    <div class="page-toolbar">
      <dm-search-bar
        v-bind="searchStore"
        :params="queryParams"
        :tableRef="listController"
        :visible-count="3"
      />
      <a-button type="primary" @click="handleAdd" v-hasPermi="['ont:ontology:add']">
        <template #icon><PlusOutlined /></template>
        新增本体
      </a-button>
    </div>

    <!-- 卡片网格 -->
    <a-spin :spinning="loading">
      <div v-if="!loading && cardList.length === 0" class="empty-wrap">
        <a-empty description="暂无本体，点击下方按钮创建第一个本体">
          <a-button type="primary" @click="handleAdd" v-hasPermi="['ont:ontology:add']">
            <template #icon><PlusOutlined /></template>
            新增本体
          </a-button>
        </a-empty>
      </div>
      <template v-else>
        <a-row :gutter="[16, 16]">
          <a-col v-for="row in cardList" :key="row.id" :xs="24" :sm="12" :lg="8" :xxl="6">
            <div class="ontology-card" @click="goWorkspace(row)">
              <div class="card-head">
                <div class="card-title">
                  <span class="card-name" :title="row.name">{{ row.name }}</span>
                  <a-tag :color="statusColor(row.status)" class="card-status">{{ statusText(row.status) }}</a-tag>
                </div>
                <div class="card-actions" @click.stop>
                  <a-tooltip title="修改">
                    <a-button type="text" size="small" @click="handleUpdate(row)" v-hasPermi="['ont:ontology:edit']">
                      <template #icon><EditOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-tooltip title="删除">
                    <a-button type="text" danger size="small" @click="handleDelete(row)" v-hasPermi="['ont:ontology:remove']">
                      <template #icon><DeleteOutlined /></template>
                    </a-button>
                  </a-tooltip>
                </div>
              </div>
              <div class="card-desc" :title="row.description">{{ row.description || '暂无描述' }}</div>
              <div class="card-footer">
                <span class="card-user" :title="row.createBy">{{ row.createBy || '-' }}</span>
                <span class="card-time">{{ parseTime(row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
              </div>
            </div>
          </a-col>
        </a-row>
        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          :page-sizes="[8, 12, 16, 24]"
          @pagination="getList"
        />
      </template>
    </a-spin>

    <!-- 新增/修改对话框 -->
    <a-modal :title="title" v-model:open="open" width="900px" wrap-class-name="ontology-workspace-modal ontology-modal--form" draggable destroy-on-close @cancel="cancel">
      <a-form ref="ontologyRef" class="ontology-form ontology-form-grid" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }">
        <template v-if="!aiEnabled">
          <a-row :gutter="20">
            <a-col :span="12">
              <a-form-item label="本体名称" name="name">
                <a-input v-model:value="form.name" placeholder="请输入本体名称" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="状态" name="status">
                <a-select v-model:value="form.status" placeholder="请选择状态">
                  <a-select-option :value="0">草稿</a-select-option>
                  <a-select-option :value="1">已发布</a-select-option>
                  <a-select-option :value="2">已归档</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="20">
            <a-col :span="24">
              <a-form-item label="描述" name="description" :label-col="{ style: { width: '100px' } }">
                <a-textarea v-model:value="form.description" :maxlength="500" :auto-size="{ minRows: 3, maxRows: 6 }" placeholder="请输入描述" />
              </a-form-item>
            </a-col>
          </a-row>
        </template>

        <a-form-item label="AI 生成" :label-col="{ style: { width: '100px' } }" class="ai-toggle-item ontology-form-grid__full">
          <a-checkbox v-model:checked="aiEnabled" :disabled="isEdit">启用 AI 生成</a-checkbox>
          <span class="toggle-tip">将根据数据源中的业务表自动生成本体、概念和属性</span>
        </a-form-item>

        <!-- 启用 AI 生成后，下方展示数据源和业务表选择 -->
        <template v-if="aiEnabled">
          <section class="ai-config-panel">
            <div class="ai-config-title">AI 生成配置</div>
            <div class="ai-config-desc">选择数据源后，可指定一张业务表；不指定时将按该数据源的全部业务表生成。</div>
            <a-form-item label="数据源" required>
              <a-select
                v-model:value="aiDatasourceId"
                placeholder="请选择数据源"
                :loading="datasourceLoading"
                show-search
                option-filter-prop="label"
                @change="onAiDatasourceChange"
              >
                <a-select-option v-for="ds in datasourceList" :key="ds.id" :value="ds.id" :label="ds.datasourceName">
                  {{ ds.datasourceName }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="业务表" class="ai-table-item">
              <a-select
                v-model:value="aiTableName"
                placeholder="不选则生成全部表"
                :loading="tableLoading"
                allow-clear
                show-search
                option-filter-prop="label"
              >
                <a-select-option v-for="t in tableOptions" :key="t.tableName" :value="t.tableName" :label="t.tableName">
                  {{ t.tableComment ? t.tableName + '（' + t.tableComment + '）' : t.tableName }}
                </a-select-option>
              </a-select>
              <div class="form-tip">不选表则对该数据源全部表生成本体</div>
            </a-form-item>
          </section>
        </template>
      </a-form>

      <template #footer>
        <a-button @click="cancel">取 消</a-button>
        <a-button v-if="!aiEnabled" type="primary" @click="submitForm">确 定</a-button>
        <a-button v-else type="primary" :loading="aiGenerating" @click="handleAiGenerate">确 定</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="OntologyList">
import { computed, inject, reactive, ref } from 'vue'
import { listOntology, getOntology, addOntology, updateOntology, delOntology } from '@/api/ont/ontology'
import { aiGenerate, listAiGenerateTables } from '@/api/ont/aiGenerate'
import { listDaDatasource } from '@/api/ast/dataSource/dataSource'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { genCode } from '@/utils/codeGen'
import useUserStore from '@/store/system/user'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const router = useRouter()
const stationNavigation = inject('spaceWorkstationNavigation', null)
const open = ref(false)
const title = ref('')
const loading = ref(false)
const cardList = ref([])
const total = ref(0)

// 默认手动创建；勾选后启用 AI 生成
const aiEnabled = ref(false)
const isEdit = computed(() => form.value.id != null)
const datasourceList = ref([])
const datasourceLoading = ref(false)
const aiDatasourceId = ref(undefined)
const aiTableName = ref(undefined)
const tableOptions = ref([])
const tableLoading = ref(false)
const aiGenerating = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 12,
  name: undefined,
  status: undefined
})

const searchStore = reactive({
  items: [
    { label: '本体名称', prop: 'name', component: { is: 'input' } },
    { label: '状态', prop: 'status', component: { is: 'select', options: [{ value: 0, label: '草稿' }, { value: 1, label: '已发布' }, { value: 2, label: '已归档' }] } }
  ],
  config: { permi: ['ont:ontology:query'] }
})

// 供 dm-search-bar 查询/重置调用（替代原 dm-table 的 tableRef）
const listController = {
  getList,
  resetQuery
}

const data = reactive({
  form: {},
  rules: {
    name: [{ required: true, message: '本体名称不能为空', trigger: 'blur' }]
  }
})
const { form, rules } = toRefs(data)

function statusText(status) {
  const map = { 0: '草稿', 1: '已发布', 2: '已归档' }
  return map[status] || '未知'
}

function statusColor(status) {
  const map = { 0: 'blue', 1: 'green', 2: 'default' }
  return map[status] || 'default'
}

function getList() {
  loading.value = true
  listOntology(queryParams).then(res => {
    cardList.value = res.data?.rows || []
    total.value = Number(res.data?.total) || 0
  }).finally(() => {
    loading.value = false
  })
}

function resetQuery() {
  queryParams.pageNum = 1
  getList()
}

function goWorkspace(row) {
  if (stationNavigation?.openPage?.({
    path: '/ont/workspace/' + row.id,
    query: { ontologyId: row.id },
    title: '本体工作台',
    routeName: 'OntWorkspace',
    meta: { title: '本体工作台', fullScreen: true },
  })) return
  router.push('/ont/workspace/' + row.id)
}

function reset() {
  form.value = { name: undefined, description: undefined, status: 0 }
  aiEnabled.value = false
  aiDatasourceId.value = undefined
  aiTableName.value = undefined
  tableOptions.value = []
  proxy.resetForm('ontologyRef')
}

function loadDatasources() {
  datasourceLoading.value = true
  listDaDatasource({ pageNum: 1, pageSize: 9999, spaceId: userStore.spaceId, spaceCode: userStore.spaceCode }).then(res => {
    datasourceList.value = res.data?.rows || []
  }).finally(() => {
    datasourceLoading.value = false
  })
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增本体'
  loadDatasources()
}

function handleUpdate(row) {
  reset()
  getOntology(row.id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改本体'
  })
}

function submitForm() {
  if (aiEnabled.value) {
    proxy.$modal.msgWarning('已启用 AI 生成，请点击「确定」')
    return
  }
  proxy.$refs['ontologyRef'].validate().then(() => {
    if (!form.value.code) form.value.code = genCode('ont')
    if (form.value.id != null) {
      updateOntology(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addOntology(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  }).catch(() => {})
}

function onAiDatasourceChange() {
  aiTableName.value = undefined
  tableOptions.value = []
  if (!aiDatasourceId.value) return
  tableLoading.value = true
  listAiGenerateTables({ datasourceId: aiDatasourceId.value }).then(res => {
    tableOptions.value = res.data || []
  }).finally(() => {
    tableLoading.value = false
  })
}

function handleAiGenerate() {
  if (!aiEnabled.value) {
    proxy.$modal.msgWarning('请先启用 AI 生成')
    return
  }
  if (!aiDatasourceId.value) {
    proxy.$modal.msgWarning('请先选择数据源')
    return
  }
  aiGenerating.value = true
  const params = {
    datasourceId: aiDatasourceId.value,
    tableName: aiTableName.value || undefined
  }
  aiGenerate(params).then(res => {
    const resp = res.data || {}
    if (resp.qualityWarning) {
      proxy.$modal.msgError(resp.qualityWarning)
      return
    }
    proxy.$modal.msgSuccess('AI 生成本体成功')
    open.value = false
    reset()
    getList()
    const ontologyId = resp.ontologyId
    if (ontologyId) {
      router.push('/ont/workspace/' + ontologyId)
    }
  }).finally(() => {
    aiGenerating.value = false
  })
}

function handleDelete(row) {
  const _id = row.id
  proxy.$modal.confirm('是否确认删除本体编号为"' + _id + '"的数据项？').then(function () {
    return delOntology(_id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function cancel() {
  open.value = false
  reset()
}

getList()
</script>

<style lang="scss" scoped>
.ontology-page {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  gap: 20px;
  padding: 4px;
  background: linear-gradient(180deg, #f7faff 0, #fff 240px);
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 72px;
  padding: 14px 20px;
  background: #ffffff;
  border: 1px solid #e6edf7;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(31, 45, 61, 0.05);

  :deep(.dm-search-bar) {
    flex: 1;
    min-width: 0;
  }

  > :deep(.ant-btn-primary) {
    flex-shrink: 0;
    height: 36px;
    padding-inline: 16px;
    border-radius: 6px;
    box-shadow: 0 4px 10px rgba(22, 119, 255, 0.18);
  }
}

.empty-wrap {
  padding: 92px 24px;
  background: #ffffff;
  border: 1px dashed #cfdced;
  border-radius: 12px;

  :deep(.ant-empty-description) {
    margin-block: 14px 20px;
    color: #68778a;
  }
}

.ontology-card {
  position: relative;
  height: 100%;
  min-height: 154px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e6edf7;
  border-radius: 12px;
  box-shadow: 0 3px 12px rgba(31, 45, 61, 0.05);
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: #b8d2ff;
    box-shadow: 0 12px 26px rgba(22, 119, 255, 0.12);

    .card-actions {
      opacity: 1;
    }
  }
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;

  .card-name {
    font-size: 16px;
    font-weight: 600;
    color: #1f2d3d;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-status {
    flex-shrink: 0;
    margin-right: 0;
  }
}

.card-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s ease;

  :deep(.ant-btn) {
    color: #4e5969;
  }

  :deep(.ant-btn-dangerous) {
    color: #ff4d4f;
  }
}

.card-desc {
  margin-top: 12px;
  height: 40px;
  font-size: 13px;
  line-height: 20px;
  color: #6b7787;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f7;
  font-size: 12px;
  color: #8a95a6;

  .card-user {
    max-width: 50%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-time {
    flex-shrink: 0;
  }
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #7a8798;
  line-height: 1.4;
}

.ontology-form {
  padding: 8px 8px 0;

  :deep(.ant-form-item) {
    margin-bottom: 20px;
  }

  :deep(.ant-input),
  :deep(.ant-select-selector) {
    border-radius: 6px;
  }
}

.ai-toggle-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px !important;

  :deep(.ant-form-item-control-input-content) {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  :deep(.ant-checkbox-wrapper) {
    font-weight: 500;
    color: #223047;
  }
}

.toggle-tip {
  font-size: 12px;
  color: #8995a5;
}

.ai-config-panel {
  margin: 4px 0 8px;
  padding: 20px 20px 4px;
  background: linear-gradient(135deg, #f4f8ff, #fbfdff);
  border: 1px solid #d8e7ff;
  border-radius: 10px;

  :deep(.ant-form-item-label > label) {
    color: #45566d;
  }
}

.ai-config-title {
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #1e5fbf;
}

.ai-config-desc {
  margin-bottom: 18px;
  font-size: 12px;
  line-height: 1.6;
  color: #6f7e91;
}

@media (max-width: 768px) {
  .ontology-page {
    gap: 14px;
    padding: 0;
  }

  .page-toolbar {
    align-items: stretch;
    flex-direction: column;
    padding: 14px;

    > :deep(.ant-btn-primary) {
      width: 100%;
    }
  }

  .card-actions {
    opacity: 1;
  }

  .ai-toggle-item :deep(.ant-form-item-control-input-content) {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }
}
</style>
