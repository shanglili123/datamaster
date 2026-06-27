<template>
  <div class="app-container ai-skill-page">
    <div class="page-header">
      <div>
        <h2>问数 Skill</h2>
        <p>维护表级问数 Skill，并同步给 AI 问数使用。</p>
      </div>
    </div>

    <div class="skill-toolbar">
      <el-form :model="queryParams" inline @submit.prevent>
        <el-form-item label="名称">
          <el-input v-model="queryParams.skillName" placeholder="请输入Skill名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getList">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="skill-actions">
        <el-button type="primary" icon="Plus" @click="handleAdd">新增</el-button>
        <el-button icon="Connection" @click="openTableGenerate">生成表级Skill</el-button>
        <el-button type="success" icon="Upload" @click="handleSyncAllSkills">同步问数Skill</el-button>
        <el-button icon="Link" @click="handleSyncAllDatasources">同步数据源</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="skillList" border>
      <el-table-column label="名称" prop="skillName" min-width="180" show-overflow-tooltip />
      <el-table-column label="编码" prop="skillCode" min-width="220" show-overflow-tooltip />
      <el-table-column label="类型" prop="skillType" width="150">
        <template #default="{ row }">{{ skillTypeText(row.skillType) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" prop="sourceType" width="130" />
      <el-table-column label="版本" prop="version" width="80" />
      <el-table-column label="问数同步" width="150">
        <template #default="{ row }">
          <el-tag :type="syncTag(row.dbgptSyncStatus)" size="small">
            {{ syncText(row.dbgptSyncStatus) }}
          </el-tag>
          <div class="sync-doc" v-if="row.dbgptDocumentName">{{ row.dbgptDocumentName }}</div>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" width="330" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" icon="View" @click="handleVersions(row)">版本</el-button>
          <el-button link type="success" icon="Check" @click="handlePublish(row)">发布</el-button>
          <el-button link type="success" icon="Upload" @click="handleSyncSkill(row)">同步</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)">归档</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="editorOpen" :title="editorTitle" width="900px" append-to-body>
      <el-form ref="skillFormRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="skillName">
              <el-input v-model="form.skillName" placeholder="请输入Skill名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="编码" prop="skillCode">
              <el-input v-model="form.skillCode" placeholder="请输入Skill编码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="类型" prop="skillType">
              <el-input value="表级问数" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已发布" value="PUBLISHED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="对象ID">
              <el-input-number v-model="form.bizObjectId" controls-position="right" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="22"
            placeholder="请输入Markdown Skill内容"
          />
        </el-form-item>
        <el-form-item label="变更说明">
          <el-input v-model="form.changeRemark" placeholder="请输入变更说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tableGenerateOpen" title="生成表级Skill" width="560px" append-to-body>
      <el-form :model="tableGenerateForm" label-width="90px">
        <el-form-item label="数据源">
          <el-select
            v-model="tableGenerateForm.datasourceId"
            placeholder="请选择数据源"
            filterable
            clearable
            style="width: 100%"
            @change="handleGenerateDatasourceChange"
          >
            <el-option
              v-for="item in datasourceOptions"
              :key="item.id"
              :label="item.datasourceName || item.name || item.id"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="表">
          <el-select
            v-model="tableGenerateForm.tableName"
            placeholder="请选择表"
            filterable
            clearable
            :loading="tableLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in tableOptions"
              :key="item.tableName || item.name"
              :label="tableOptionLabel(item)"
              :value="item.tableName || item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="强制刷新">
          <el-switch v-model="tableGenerateForm.forceRefresh" />
        </el-form-item>
        <el-form-item label="发布">
          <el-switch v-model="tableGenerateForm.publish" />
        </el-form-item>
        <el-form-item label="人工备注">
          <el-input v-model="tableGenerateForm.manualNotes" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tableGenerateOpen = false">取消</el-button>
        <el-button type="primary" :loading="generatingTable" @click="handleGenerateTable">生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="versionOpen" title="Skill版本" size="680px">
      <el-table :data="versionList" border>
        <el-table-column label="版本" prop="version" width="80" />
        <el-table-column label="类型" prop="changeType" width="110" />
        <el-table-column label="说明" prop="changeRemark" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleRollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup name="AiSkill">
import { getCurrentInstance, reactive, ref } from 'vue'
import {
  addSkill,
  delSkill,
  generateTableSkill,
  getSkill,
  listSkill,
  listSkillVersions,
  publishSkill,
  rollbackSkill,
  syncAllSkillToDbgpt,
  syncSkillToDbgpt,
  updateSkill
} from '@/api/ai/skill'
import { syncAllDatasourceToDbgpt } from '@/api/ai/dbgpt'
import { listDaDatasource, tableList } from '@/api/ast/dataSource/dataSource'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const skillList = ref([])
const total = ref(0)
const editorOpen = ref(false)
const editorTitle = ref('新增Skill')
const tableGenerateOpen = ref(false)
const versionOpen = ref(false)
const versionList = ref([])
const currentSkill = ref(null)
const skillFormRef = ref()
const datasourceOptions = ref([])
const tableOptions = ref([])
const tableLoading = ref(false)
const generatingTable = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  skillName: '',
  skillType: 'TABLE',
  status: ''
})

const form = reactive({
  id: null,
  skillName: '',
  skillCode: '',
  skillType: 'TABLE',
  status: 'DRAFT',
  sourceType: 'MANUAL',
  bizObjectType: '',
  bizObjectId: null,
  content: '',
  changeRemark: ''
})

const tableGenerateForm = reactive({
  assetId: null,
  datasourceId: null,
  tableName: '',
  forceRefresh: false,
  publish: false,
  manualNotes: ''
})

const rules = {
  skillName: [{ required: true, message: 'Skill名称不能为空', trigger: 'blur' }],
  skillCode: [{ required: true, message: 'Skill编码不能为空', trigger: 'blur' }],
  skillType: [{ required: true, message: 'Skill类型不能为空', trigger: 'change' }],
  content: [{ required: true, message: 'Skill内容不能为空', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listSkill(queryParams).then((res) => {
    const data = res.data || {}
    skillList.value = data.rows || []
    total.value = data.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function resetQuery() {
  queryParams.pageNum = 1
  queryParams.skillName = ''
  queryParams.skillType = 'TABLE'
  queryParams.status = ''
  getList()
}

function resetForm() {
  Object.assign(form, {
    id: null,
    skillName: '',
    skillCode: '',
    skillType: 'TABLE',
    status: 'DRAFT',
    sourceType: 'MANUAL',
    bizObjectType: '',
    bizObjectId: null,
    content: '',
    changeRemark: ''
  })
}

function handleAdd() {
  resetForm()
  editorTitle.value = '新增Skill'
  editorOpen.value = true
}

function handleEdit(row) {
  getSkill(row.id).then((res) => {
    resetForm()
    Object.assign(form, res.data || {})
    editorTitle.value = '编辑Skill'
    editorOpen.value = true
  })
}

function submitForm() {
  skillFormRef.value.validate((valid) => {
    if (!valid) return
    const payload = {
      id: form.id,
      skillName: form.skillName,
      skillCode: form.skillCode,
      skillType: form.skillType,
      status: form.status,
      sourceType: form.sourceType,
      bizObjectType: form.bizObjectType,
      bizObjectId: form.bizObjectId,
      content: form.content,
      changeRemark: form.changeRemark
    }
    const request = form.id ? updateSkill(payload) : addSkill(payload)
    request.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      editorOpen.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('确认归档该Skill吗？').then(() => {
    return delSkill(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('归档成功')
    getList()
  })
}

function handlePublish(row) {
  publishSkill(row.id).then(() => {
    proxy.$modal.msgSuccess('发布成功')
    getList()
  })
}

function handleSyncSkill(row) {
  syncSkillToDbgpt(row.id).then((res) => {
    proxy.$modal.msgSuccess(res.msg || '同步成功')
    getList()
  })
}

function handleSyncAllSkills() {
  syncAllSkillToDbgpt().then((res) => {
    proxy.$modal.msgSuccess(res.msg || '同步完成')
    getList()
  })
}

function handleSyncAllDatasources() {
  syncAllDatasourceToDbgpt().then((res) => {
    proxy.$modal.msgSuccess(res.msg || '数据源同步完成')
  })
}

function openTableGenerate() {
  tableGenerateForm.assetId = null
  tableGenerateForm.datasourceId = null
  tableGenerateForm.tableName = ''
  tableGenerateForm.forceRefresh = false
  tableGenerateForm.publish = false
  tableGenerateForm.manualNotes = ''
  tableOptions.value = []
  loadDatasourceOptions()
  tableGenerateOpen.value = true
}

function loadDatasourceOptions() {
  listDaDatasource({ pageNum: 1, pageSize: 1000 }).then((res) => {
    datasourceOptions.value = toRows(res.data)
  })
}

function handleGenerateDatasourceChange(datasourceId) {
  tableGenerateForm.tableName = ''
  tableOptions.value = []
  if (!datasourceId) return
  tableLoading.value = true
  tableList(datasourceId).then((res) => {
    tableOptions.value = toRows(res.data)
  }).finally(() => {
    tableLoading.value = false
  })
}

function toRows(data) {
  if (Array.isArray(data)) {
    return data
  }
  if (data && Array.isArray(data.rows)) {
    return data.rows
  }
  if (data && Array.isArray(data.list)) {
    return data.list
  }
  return []
}

function tableOptionLabel(item) {
  const name = item.tableName || item.name || ''
  const comment = item.tableComment || item.comment || ''
  return comment ? `${name}（${comment}）` : name
}

function handleGenerateTable() {
  if (!tableGenerateForm.datasourceId || !tableGenerateForm.tableName) {
    proxy.$modal.msgWarning('请选择数据源和表')
    return
  }
  generatingTable.value = true
  generateTableSkill(tableGenerateForm).then(() => {
    proxy.$modal.msgSuccess('表级Skill已生成')
    tableGenerateOpen.value = false
    getList()
  }).finally(() => {
    generatingTable.value = false
  })
}

function handleVersions(row) {
  currentSkill.value = row
  listSkillVersions(row.id).then((res) => {
    versionList.value = res.data || []
    versionOpen.value = true
  })
}

function handleRollback(row) {
  if (!currentSkill.value) return
  proxy.$modal.confirm('确认回滚到该版本吗？').then(() => {
    return rollbackSkill(currentSkill.value.id, row.version)
  }).then(() => {
    proxy.$modal.msgSuccess('回滚成功')
    versionOpen.value = false
    getList()
  })
}

function skillTypeText(type) {
  const map = {
    TABLE: '表级问数'
  }
  return map[type] || type
}

function statusText(status) {
  const map = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

function statusTag(status) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'ARCHIVED') return 'info'
  return 'warning'
}

function syncText(status) {
  const map = {
    SYNCED: '已同步',
    FAILED: '失败',
    REMOVED: '已移除'
  }
  return map[status] || '未同步'
}

function syncTag(status) {
  if (status === 'SYNCED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

getList()
</script>

<style scoped>
.ai-skill-page {
  min-height: calc(100vh - 84px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
}

.page-header p {
  margin: 0;
  color: #909399;
}

.skill-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.skill-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.sync-doc {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.el-textarea__inner) {
  font-family: Consolas, Monaco, monospace;
  line-height: 1.55;
}
</style>
