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
        <el-form-item label="类型">
          <el-select v-model="queryParams.skillType" placeholder="请选择类型" clearable>
            <el-option label="表级问数" value="TABLE" />
            <el-option label="整库问数" value="DATABASE" />
            <el-option label="多表问数" value="MULTI_TABLE" />
            <el-option label="报告模板" value="REPORT_TEMPLATE" />
          </el-select>
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
        <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['ai:skill:add']">新增</el-button>
        <el-button icon="Connection" @click="openSkillGenerate" v-hasPermi="['ai:skill:generate']">生成问数Skill</el-button>
        <el-button type="success" icon="Upload" @click="handleSyncAllSkills" v-hasPermi="['ai:skill:sync']">同步问数Skill</el-button>
        <el-button icon="Link" @click="handleSyncAllDatasources" v-hasPermi="['da:dataSource:edit']">同步数据源</el-button>
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
      <el-table-column label="操作" width="500" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" icon="View" @click="handleVersions(row)">版本</el-button>
          <el-button link type="primary" icon="Upload" @click="openTemplateEditor(row)" v-hasPermi="['ai:skill:edit']">上传模板</el-button>
          <el-button link type="primary" icon="Tickets" @click="openTemplateList(row)">查看模板</el-button>
          <el-button link type="success" icon="Check" @click="handlePublish(row)" v-hasPermi="['ai:skill:publish']">发布</el-button>
          <el-button link type="success" icon="Upload" @click="handleSyncSkill(row)" v-hasPermi="['ai:skill:sync']">同步</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)" v-hasPermi="['ai:skill:remove']">归档</el-button>
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
              <el-select v-model="form.skillType" style="width: 100%">
                <el-option label="表级问数" value="TABLE" />
                <el-option label="整库问数" value="DATABASE" />
                <el-option label="多表问数" value="MULTI_TABLE" />
                <el-option label="报告模板" value="REPORT_TEMPLATE" />
              </el-select>
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

    <el-dialog v-model="tableGenerateOpen" title="生成问数Skill" width="560px" append-to-body>
      <el-form :model="tableGenerateForm" label-width="90px">
        <el-form-item label="生成范围">
          <el-segmented
            v-model="tableGenerateForm.generateScope"
            :options="generateScopeOptions"
            @change="handleGenerateScopeChange"
          />
        </el-form-item>
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
        <el-form-item v-if="tableGenerateForm.generateScope !== 'database'" label="表">
          <el-select
            v-model="tableGenerateTableValue"
            placeholder="请选择表"
            filterable
            clearable
            :multiple="tableGenerateForm.generateScope === 'multi'"
            collapse-tags
            collapse-tags-tooltip
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
        <el-button type="primary" :loading="generatingTable" @click="handleGenerateTable">
          {{ skillGenerateButtonText }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="versionOpen" title="Skill版本" size="680px">
      <el-table :data="pagedVersionList" border>
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
      <pagination
        v-show="versionList.length > 0"
        :total="versionList.length"
        v-model:page="versionPagination.pageNum"
        v-model:limit="versionPagination.pageSize"
      />
    </el-drawer>

    <el-drawer v-model="templateListOpen" :title="templateListTitle" size="780px">
      <div class="template-list-toolbar">
        <el-button type="primary" icon="Plus" @click="openTemplateEditor(currentTemplateSkill)">上传报告模板</el-button>
      </div>
      <el-table v-loading="templateLoading" :data="pagedTemplateList" border>
        <el-table-column label="模板名称" prop="templateName" min-width="180" show-overflow-tooltip />
        <el-table-column label="编码" prop="templateCode" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认" prop="defaultFlag" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.defaultFlag" type="success">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="版本" prop="version" width="70" />
        <el-table-column label="更新时间" prop="updateTime" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTemplateEditor(currentTemplateSkill, row)">编辑</el-button>
            <el-button link type="success" :disabled="row.defaultFlag" @click="handleSetDefaultTemplate(row)">设默认</el-button>
            <el-button link type="danger" @click="handleDeleteTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="templateList.length > 0"
        :total="templateList.length"
        v-model:page="templatePagination.pageNum"
        v-model:limit="templatePagination.pageSize"
      />
    </el-drawer>

    <el-dialog v-model="templateEditorOpen" :title="templateEditorTitle" width="980px" append-to-body>
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="templateForm.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="templateForm.templateCode" placeholder="请输入模板编码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="templateForm.status" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已发布" value="PUBLISHED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认模板">
              <el-switch v-model="templateForm.defaultFlag" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="操作">
              <el-button icon="Document" @click="fillTemplateFormat">填入标准格式</el-button>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模板JSON" prop="templateContent">
          <el-input
            v-model="templateForm.templateContent"
            type="textarea"
            :rows="24"
            placeholder="请粘贴报告模板JSON"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="templateForm.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateEditorOpen = false">取消</el-button>
        <el-button type="primary" :loading="templateSaving" @click="submitTemplateForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AiSkill">
import { computed, getCurrentInstance, reactive, ref } from 'vue'
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
  templateFormRef.value.validate((valid) => {
    if (!valid || !currentTemplateSkill.value?.id) return
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
  })
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

.template-list-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}

:deep(.el-textarea__inner) {
  font-family: Consolas, Monaco, monospace;
  line-height: 1.55;
}
</style>
