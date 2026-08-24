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
    <a-modal :title="title" v-model:open="open" width="800px" draggable destroy-on-close ok-text="OK" cancel-text="Cancel" @ok="submitForm" @cancel="cancel">
      <a-form ref="ontologyRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }">
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
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="OntologyList">
import { listOntology, getOntology, addOntology, updateOntology, delOntology } from '@/api/ont/ontology'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { genCode } from '@/utils/codeGen'

const { proxy } = getCurrentInstance()
const router = useRouter()
const open = ref(false)
const title = ref('')
const loading = ref(false)
const cardList = ref([])
const total = ref(0)

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
  router.push('/ont/workspace/' + row.id)
}

function reset() {
  form.value = { name: undefined, description: undefined, status: 0 }
  proxy.resetForm('ontologyRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增本体'
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
  gap: 16px;
}

.page-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
}

.empty-wrap {
  padding: 80px 0;
  background: #ffffff;
  border: 1px dashed #e5eaf2;
  border-radius: 8px;
}

.ontology-card {
  position: relative;
  height: 100%;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.06);
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-3px);
    border-color: #c9dcff;
    box-shadow: 0 8px 20px rgba(22, 119, 255, 0.12);

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
    font-size: 15px;
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
  margin-top: 10px;
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
  margin-top: 12px;
  padding-top: 10px;
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
</style>
