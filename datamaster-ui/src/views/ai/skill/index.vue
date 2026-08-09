<template>
  <div class="app-container ai-skill-page">
    <div class="page-header">
      <div>
        <h2>问数 Skill</h2>
        <p>维护表级问数 Skill，并同步给 AI 问数使用。</p>
      </div>
    </div>

    <div class="skill-toolbar">
      <a-form :model="queryParams" layout="inline" @submit.prevent>
        <a-form-item label="名称">
          <a-input v-model:value="queryParams.skillName" placeholder="请输入Skill名称" allow-clear />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="queryParams.skillType" placeholder="请选择类型" allow-clear style="width: 150px">
            <a-select-option label="表级问数" value="TABLE" />
            <a-select-option label="整库问数" value="DATABASE" />
            <a-select-option label="多表问数" value="MULTI_TABLE" />
            <a-select-option label="报告模板" value="REPORT_TEMPLATE" />
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="queryParams.status" placeholder="请选择状态" allow-clear style="width: 150px">
            <a-select-option label="草稿" value="DRAFT" />
            <a-select-option label="已发布" value="PUBLISHED" />
            <a-select-option label="已归档" value="ARCHIVED" />
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :icon="h(SearchOutlined)" @click="getList">查询</a-button>
          <a-button :icon="h(ReloadOutlined)" @click="resetQuery">重置</a-button>
        </a-form-item>
      </a-form>
      <div class="skill-actions">
        <a-button type="primary" :icon="h(PlusOutlined)" @click="handleAdd" v-hasPermi="['ai:skill:add']">新增</a-button>
        <a-button :icon="h(ApartmentOutlined)" @click="openSkillGenerate" v-hasPermi="['ai:skill:generate']">生成问数Skill</a-button>
        <a-button type="primary" :icon="h(UploadOutlined)" @click="handleSyncAllSkills" v-hasPermi="['ai:skill:sync']">同步问数Skill</a-button>
        <a-button :icon="h(LinkOutlined)" @click="handleSyncAllDatasources" v-hasPermi="['ast:dataSource:edit']">同步数据源</a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <a-table
        :data-source="skillList"
        :columns="skillColumns"
        :pagination="false"
        bordered
        row-key="id"
        :scroll="{ x: 1600 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'skillType'">
            {{ skillTypeText(record.skillType) }}
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="statusTagColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'dbgptSyncStatus'">
            <a-tag :color="syncTagColor(record.dbgptSyncStatus)">
              {{ syncText(record.dbgptSyncStatus) }}
            </a-tag>
            <div class="sync-doc" v-if="record.dbgptDocumentName">{{ record.dbgptDocumentName }}</div>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="handleVersions(record)">版本</a-button>
            <a-button type="link" size="small" @click="openTemplateEditor(record)" v-hasPermi="['ai:skill:edit']">上传模板</a-button>
            <a-button type="link" size="small" @click="openTemplateList(record)">查看模板</a-button>
            <a-button type="link" size="small" @click="handlePublish(record)" v-hasPermi="['ai:skill:publish']">发布</a-button>
            <a-button type="link" size="small" @click="handleSyncSkill(record)" v-hasPermi="['ai:skill:sync']">同步</a-button>
            <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['ai:skill:remove']">归档</a-button>
          </template>
        </template>
      </a-table>
    </a-spin>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <a-modal v-model:open="editorOpen" :title="editorTitle" width="900" destroy-on-close>
      <a-form ref="skillFormRef" :model="form" :rules="rules" :label-col="{ style: { width: '90px' } }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="名称" name="skillName">
              <a-input v-model:value="form.skillName" placeholder="请输入Skill名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="编码" name="skillCode">
              <a-input v-model:value="form.skillCode" placeholder="请输入Skill编码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="类型" name="skillType">
              <a-select v-model:value="form.skillType" style="width: 100%">
                <a-select-option label="表级问数" value="TABLE" />
                <a-select-option label="整库问数" value="DATABASE" />
                <a-select-option label="多表问数" value="MULTI_TABLE" />
                <a-select-option label="报告模板" value="REPORT_TEMPLATE" />
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="状态" name="status">
              <a-select v-model:value="form.status" placeholder="请选择状态">
                <a-select-option label="草稿" value="DRAFT" />
                <a-select-option label="已发布" value="PUBLISHED" />
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="对象ID">
              <a-input-number v-model:value="form.bizObjectId" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="内容" name="content">
          <a-textarea
            v-model:value="form.content"
            :rows="22"
            placeholder="请输入Markdown Skill内容"
          />
        </a-form-item>
        <a-form-item label="变更说明">
          <a-input v-model:value="form.changeRemark" placeholder="请输入变更说明" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="editorOpen = false">取消</a-button>
        <a-button type="primary" @click="submitForm">保存</a-button>
      </template>
    </a-modal>

    <a-modal v-model:open="tableGenerateOpen" title="生成问数Skill" width="560" destroy-on-close>
      <a-form :model="tableGenerateForm" :label-col="{ style: { width: '90px' } }">
        <a-form-item label="生成范围">
          <a-segmented
            v-model:value="tableGenerateForm.generateScope"
            :options="generateScopeOptions"
            @change="handleGenerateScopeChange"
          />
        </a-form-item>
        <a-form-item label="数据源">
          <a-select
            v-model:value="tableGenerateForm.datasourceId"
            placeholder="请选择数据源"
            show-search
            allow-clear
            style="width: 100%"
            @change="handleGenerateDatasourceChange"
          >
            <a-select-option
              v-for="item in datasourceOptions"
              :key="item.id"
              :value="item.id"
            >
              {{ item.datasourceName || item.name || item.id }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="tableGenerateForm.generateScope !== 'database'" label="表">
          <a-select
            v-model:value="tableGenerateTableValue"
            placeholder="请选择表"
            show-search
            allow-clear
            :mode="tableGenerateForm.generateScope === 'multi' ? 'multiple' : undefined"
            :loading="tableLoading"
            style="width: 100%"
          >
            <a-select-option
              v-for="item in tableOptions"
              :key="item.tableName || item.name"
              :value="item.tableName || item.name"
            >
              {{ tableOptionLabel(item) }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="强制刷新">
          <a-switch v-model:checked="tableGenerateForm.forceRefresh" />
        </a-form-item>
        <a-form-item label="发布">
          <a-switch v-model:checked="tableGenerateForm.publish" />
        </a-form-item>
        <a-form-item label="人工备注">
          <a-textarea v-model:value="tableGenerateForm.manualNotes" :rows="5" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="tableGenerateOpen = false">取消</a-button>
        <a-button type="primary" :loading="generatingTable" @click="handleGenerateTable">
          {{ skillGenerateButtonText }}
        </a-button>
      </template>
    </a-modal>

    <a-drawer v-model:open="versionOpen" title="Skill版本" width="680">
      <a-table :data-source="pagedVersionList" :columns="versionColumns" :pagination="false" bordered row-key="version" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="handleRollback(record)">回滚</a-button>
          </template>
        </template>
      </a-table>
      <pagination
        v-show="versionList.length > 0"
        :total="versionList.length"
        v-model:page="versionPagination.pageNum"
        v-model:limit="versionPagination.pageSize"
      />
    </a-drawer>

    <a-drawer v-model:open="templateListOpen" :title="templateListTitle" width="780">
      <div class="template-list-toolbar">
        <a-button type="primary" :icon="h(PlusOutlined)" @click="openTemplateEditor(currentTemplateSkill)">上传报告模板</a-button>
      </div>
      <a-spin :spinning="templateLoading">
        <a-table :data-source="pagedTemplateList" :columns="templateColumns" :pagination="false" bordered row-key="id" size="small">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="record.status === 'PUBLISHED' ? 'success' : 'warning'">
                {{ record.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'defaultFlag'">
              <a-tag v-if="record.defaultFlag" color="success">默认</a-tag>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="openTemplateEditor(currentTemplateSkill, record)">编辑</a-button>
              <a-button type="link" size="small" :disabled="record.defaultFlag" @click="handleSetDefaultTemplate(record)">设默认</a-button>
              <a-button type="link" danger size="small" @click="handleDeleteTemplate(record)">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-spin>
      <pagination
        v-show="templateList.length > 0"
        :total="templateList.length"
        v-model:page="templatePagination.pageNum"
        v-model:limit="templatePagination.pageSize"
      />
    </a-drawer>

    <a-modal v-model:open="templateEditorOpen" :title="templateEditorTitle" width="980" destroy-on-close>
      <a-form ref="templateFormRef" :model="templateForm" :rules="templateRules" :label-col="{ style: { width: '90px' } }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模板名称" name="templateName">
              <a-input v-model:value="templateForm.templateName" placeholder="请输入模板名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="模板编码" name="templateCode">
              <a-input v-model:value="templateForm.templateCode" placeholder="请输入模板编码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="状态">
              <a-select v-model:value="templateForm.status" style="width: 100%">
                <a-select-option label="草稿" value="DRAFT" />
                <a-select-option label="已发布" value="PUBLISHED" />
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="默认模板">
              <a-switch v-model:checked="templateForm.defaultFlag" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="操作">
              <a-button :icon="h(FileTextOutlined)" @click="fillTemplateFormat">填入标准格式</a-button>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="模板JSON" name="templateContent">
          <a-textarea
            v-model:value="templateForm.templateContent"
            :rows="24"
            placeholder="请粘贴报告模板JSON"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="templateForm.remark" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="templateEditorOpen = false">取消</a-button>
        <a-button type="primary" :loading="templateSaving" @click="submitTemplateForm">保存</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="AiSkill">

import { computed, getCurrentInstance, h, reactive, ref } from 'vue'

import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  ApartmentOutlined,
  UploadOutlined,
  LinkOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'

import {
  addSkill,
  addSkillReportTemplate,
  deleteSkillReportTemplate,
  delSkill,
  generateDatabaseSkill,
  generateMultiTableSkill,
  generateTableSkill,
  getSkill,
  listSkillReportTemplates,
  listSkill,
  listSkillVersions,
  publishSkill,
  rollbackSkill,
  setDefaultSkillReportTemplate,
  syncAllSkillToDbgpt,
  syncSkillToDbgpt,
  updateSkillReportTemplate,
  updateSkill
} from '@/api/ai/skill'

import { syncAllDatasourceToDbgpt } from '@/api/ai/dbgpt'

import { listDaDatasource, tableList } from '@/api/ast/dataSource/dataSource'

import { buildReportTemplateFormatText } from '@/views/ai/chat/index/reportTemplateFormat'

const { proxy } = getCurrentInstance()

const skillColumns = [
  { title: '名称', dataIndex: 'skillName', minWidth: 180, ellipsis: true },
  { title: '编码', dataIndex: 'skillCode', minWidth: 220, ellipsis: true },
  { title: '类型', dataIndex: 'skillType', width: 150 },
  { title: '状态', dataIndex: 'status', width: 110 },
  { title: '来源', dataIndex: 'sourceType', width: 130 },
  { title: '版本', dataIndex: 'version', width: 80 },
  { title: '问数同步', dataIndex: 'dbgptSyncStatus', width: 150 },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', key: 'actions', width: 500, fixed: 'right' },
]

const versionColumns = [
  { title: '版本', dataIndex: 'version', width: 80 },
  { title: '类型', dataIndex: 'changeType', width: 110 },
  { title: '说明', dataIndex: 'changeRemark', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'actions', width: 90 },
]

const templateColumns = [
  { title: '模板名称', dataIndex: 'templateName', minWidth: 180, ellipsis: true },
  { title: '编码', dataIndex: 'templateCode', minWidth: 180, ellipsis: true },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '默认', dataIndex: 'defaultFlag', width: 80 },
  { title: '版本', dataIndex: 'version', width: 70 },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', key: 'actions', width: 230, fixed: 'right' },
]

function statusTagColor(status) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'ARCHIVED') return 'default'
  return 'warning'
}

function syncTagColor(status) {
  if (status === 'SYNCED') return 'success'
  if (status === 'FAILED') return 'error'
  return 'default'
}

const loading = ref(false)
const skillList = ref([])
const total = ref(0)
const editorOpen = ref(false)
const editorTitle = ref('新增Skill')
const tableGenerateOpen = ref(false)
const versionOpen = ref(false)
const versionList = ref([])
const versionPagination = reactive({
  pageNum: 1,
  pageSize: 6
})
const currentSkill = ref(null)
const skillFormRef = ref()
const datasourceOptions = ref([])
const tableOptions = ref([])
const tableLoading = ref(false)
const generatingTable = ref(false)
const templateListOpen = ref(false)
const templateListTitle = ref('报告模板')
const templateList = ref([])
const templateLoading = ref(false)
const templatePagination = reactive({
  pageNum: 1,
  pageSize: 6
})
const currentTemplateSkill = ref(null)
const templateEditorOpen = ref(false)
const templateEditorTitle = ref('上传报告模板')
const templateSaving = ref(false)
const templateFormRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 6,
  skillName: '',
  skillType: '',
  status: ''
})

const pagedVersionList = computed(() => {
  return paginateList(versionList.value, versionPagination)
})

const pagedTemplateList = computed(() => {
  return paginateList(templateList.value, templatePagination)
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
  generateScope: 'database',
  tableNames: [],
  forceRefresh: false,
  publish: false,
  manualNotes: ''
})

const generateScopeOptions = [
  { label: '整库', value: 'database' },
  { label: '单表', value: 'table' },
  { label: '多表', value: 'multi' }
]

const skillGenerateType = computed(() => {
  return tableGenerateForm.generateScope
})

const skillGenerateButtonText = computed(() => {
  if (skillGenerateType.value === 'database') return '生成整库Skill'
  if (skillGenerateType.value === 'multi') return '生成多表Skill'
  return '生成表级Skill'
})

const tableGenerateTableValue = computed({
  get() {
    return tableGenerateForm.generateScope === 'multi'
      ? tableGenerateForm.tableNames
      : tableGenerateForm.tableNames[0] || ''
  },
  set(value) {
    tableGenerateForm.tableNames = Array.isArray(value)
      ? value
      : value
        ? [value]
        : []
  }
})

const templateForm = reactive({
  id: null,
  skillId: null,
  templateCode: '',
  templateName: '',
  templateContent: '',
  status: 'DRAFT',
  defaultFlag: false,
  remark: ''
})

const rules = {
  skillName: [{ required: true, message: 'Skill名称不能为空', trigger: 'blur' }],
  skillCode: [{ required: true, message: 'Skill编码不能为空', trigger: 'blur' }],
  skillType: [{ required: true, message: 'Skill类型不能为空', trigger: 'change' }],
  content: [{ required: true, message: 'Skill内容不能为空', trigger: 'blur' }]
}

const templateRules = {
  templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
  templateCode: [{ required: true, message: '模板编码不能为空', trigger: 'blur' }],
  templateContent: [
    { required: true, message: '模板JSON不能为空', trigger: 'blur' },
    { validator: validateTemplateJson, trigger: 'blur' }
  ]
}

function getList() {
  loading.value = true
  listSkill(queryParams).then((res) => {
    const page = normalizePageData(res)
    skillList.value = page.rows
    total.value = page.total
  }).finally(() => {
    loading.value = false
  })
}

function normalizePageData(res) {
  const data = res?.data || res || {}
  const rows = Array.isArray(data.rows)
    ? data.rows
    : Array.isArray(data.list)
      ? data.list
      : Array.isArray(data.records)
        ? data.records
        : Array.isArray(res?.rows)
          ? res.rows
          : []
  const total = Number(data.total ?? data.totalCount ?? res?.total ?? rows.length)
  return {
    rows,
    total: Number.isNaN(total) ? rows.length : total
  }
}

function paginateList(list, pagination) {
  const rows = Array.isArray(list) ? list : []
  const pageNum = pagination.pageNum || 1
  const pageSize = pagination.pageSize || 6
  const start = (pageNum - 1) * pageSize
  return rows.slice(start, start + pageSize)
}

function resetQuery() {
  queryParams.pageNum = 1
  queryParams.skillName = ''
  queryParams.skillType = ''
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
  skillFormRef.value.validate().then(() => {
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
  }).catch(() => {})
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

function openTemplateList(row) {
  currentTemplateSkill.value = row
  templateListTitle.value = `${row.skillName} - 报告模板`
  templatePagination.pageNum = 1
  templateListOpen.value = true
  loadTemplateList()
}

function loadTemplateList() {
  if (!currentTemplateSkill.value?.id) return
  templateLoading.value = true
  listSkillReportTemplates(currentTemplateSkill.value.id).then((res) => {
    templateList.value = res.data || []
    templatePagination.pageNum = 1
  }).finally(() => {
    templateLoading.value = false
  })
}

function resetTemplateForm() {
  Object.assign(templateForm, {
    id: null,
    skillId: null,
    templateCode: '',
    templateName: '',
    templateContent: '',
    status: 'DRAFT',
    defaultFlag: false,
    remark: ''
  })
}

function openTemplateEditor(skill, template) {
  const targetSkill = skill || currentTemplateSkill.value
  if (!targetSkill?.id) {
    proxy.$modal.msgWarning('请先选择Skill')
    return
  }
  currentTemplateSkill.value = targetSkill
  resetTemplateForm()
  if (template) {
    Object.assign(templateForm, {
      id: template.id,
      skillId: template.skillId,
      templateCode: template.templateCode,
      templateName: template.templateName,
      templateContent: template.templateContent,
      status: template.status || 'DRAFT',
      defaultFlag: !!template.defaultFlag,
      remark: template.remark || ''
    })
    templateEditorTitle.value = '编辑报告模板'
  } else {
    templateForm.skillId = targetSkill.id
    templateForm.templateCode = `${targetSkill.skillCode || 'skill'}_report_template`
    templateForm.templateName = `${targetSkill.skillName || 'Skill'}报告模板`
    templateForm.templateContent = buildReportTemplateFormatText(targetSkill)
    templateEditorTitle.value = '上传报告模板'
  }
  templateEditorOpen.value = true
}

function fillTemplateFormat() {
  templateForm.templateContent = buildReportTemplateFormatText(currentTemplateSkill.value)
  if (currentTemplateSkill.value?.skillCode && !templateForm.templateCode) {
    templateForm.templateCode = `${currentTemplateSkill.value.skillCode}_report_template`
  }
  if (currentTemplateSkill.value?.skillName && !templateForm.templateName) {
    templateForm.templateName = `${currentTemplateSkill.value.skillName}报告模板`
  }
}

function submitTemplateForm() {
  templateFormRef.value.validate().then(() => {
    if (!currentTemplateSkill.value?.id) return
    templateSaving.value = true
    const payload = {
      id: templateForm.id,
      skillId: currentTemplateSkill.value.id,
      templateCode: templateForm.templateCode,
      templateName: templateForm.templateName,
      templateContent: templateForm.templateContent,
      status: templateForm.status,
      defaultFlag: templateForm.defaultFlag,
      remark: templateForm.remark
    }
    const request = templateForm.id
      ? updateSkillReportTemplate(currentTemplateSkill.value.id, templateForm.id, payload)
      : addSkillReportTemplate(currentTemplateSkill.value.id, payload)
    request.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      templateEditorOpen.value = false
      if (templateListOpen.value) {
        loadTemplateList()
      }
    }).finally(() => {
      templateSaving.value = false
    })
  }).catch(() => {})
}

function handleSetDefaultTemplate(row) {
  if (!currentTemplateSkill.value?.id) return
  setDefaultSkillReportTemplate(currentTemplateSkill.value.id, row.id).then(() => {
    proxy.$modal.msgSuccess('已设为默认模板')
    loadTemplateList()
  })
}

function handleDeleteTemplate(row) {
  if (!currentTemplateSkill.value?.id) return
  proxy.$modal.confirm('确认删除该报告模板吗？').then(() => {
    return deleteSkillReportTemplate(currentTemplateSkill.value.id, row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    loadTemplateList()
  })
}

function validateTemplateJson(rule, value, callback) {
  if (!value) {
    callback()
    return
  }
  try {
    const parsed = JSON.parse(value)
    if (!parsed.dataSchema) {
      callback(new Error('模板缺少 dataSchema'))
      return
    }
    if (!parsed.layout) {
      callback(new Error('模板缺少 layout'))
      return
    }
    if (!parsed.style) {
      callback(new Error('模板缺少 style'))
      return
    }
    callback()
  } catch (error) {
    callback(new Error('模板JSON格式不正确'))
  }
}

function openSkillGenerate() {
  tableGenerateForm.assetId = null
  tableGenerateForm.datasourceId = null
  tableGenerateForm.generateScope = 'database'
  tableGenerateForm.tableNames = []
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
  tableGenerateForm.tableNames = []
  tableOptions.value = []
  if (!datasourceId || tableGenerateForm.generateScope === 'database') return
  tableLoading.value = true
  tableList(datasourceId).then((res) => {
    tableOptions.value = toRows(res.data)
  }).finally(() => {
    tableLoading.value = false
  })
}

function handleGenerateScopeChange() {
  tableGenerateForm.tableNames = []
  tableOptions.value = []
  if (tableGenerateForm.datasourceId && tableGenerateForm.generateScope !== 'database') {
    handleGenerateDatasourceChange(tableGenerateForm.datasourceId)
  }
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
  if (!tableGenerateForm.datasourceId) {
    proxy.$modal.msgWarning('请选择数据源')
    return
  }
  generatingTable.value = true
  const selectedTableNames = tableGenerateForm.tableNames.filter(Boolean)
  const generateType = skillGenerateType.value
  if (generateType === 'table' && selectedTableNames.length !== 1) {
    proxy.$modal.msgWarning('请选择一张表')
    generatingTable.value = false
    return
  }
  if (generateType === 'multi' && selectedTableNames.length < 2) {
    proxy.$modal.msgWarning('多表Skill至少选择两张表')
    generatingTable.value = false
    return
  }
  const payload = {
    datasourceId: tableGenerateForm.datasourceId,
    tableName: selectedTableNames[0] || '',
    tableNames: selectedTableNames,
    forceRefresh: tableGenerateForm.forceRefresh,
    publish: tableGenerateForm.publish,
    manualNotes: tableGenerateForm.manualNotes
  }
  const request = generateType === 'database'
    ? generateDatabaseSkill(payload)
    : generateType === 'multi'
      ? generateMultiTableSkill(payload)
      : generateTableSkill(payload)
  request.then(() => {
    proxy.$modal.msgSuccess(`${skillGenerateButtonText.value}已生成`)
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
    versionPagination.pageNum = 1
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
    TABLE: '表级问数',
    DATABASE: '整库问数',
    MULTI_TABLE: '多表问数',
    REPORT_TEMPLATE: '报告模板',
    PLATFORM_METADATA: '元数据',
    PLATFORM_QUALITY: '质量'
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
  if (status === 'ARCHIVED') return 'default'
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
  if (status === 'FAILED') return 'error'
  return 'default'
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

.template-list-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}

:deep(.ant-input) {
  font-family: Consolas, Monaco, monospace;
  line-height: 1.55;
}
</style>
