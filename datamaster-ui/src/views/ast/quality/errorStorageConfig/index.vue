<template>
  <div class="app-container">
    <el-card shadow="never" class="mb15">
      <template #header>
        <span class="card-title">错误明细存储配置</span>
      </template>
      <el-form label-width="120px" label-position="right">
        <el-form-item label="当前存储方式">
          <el-tag :type="currentStorageType === 'JDBC' ? 'success' : 'info'" size="large">
            {{ currentStorageType === 'JDBC' ? '关系型数据库' : '未配置 / MongoDB' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="当前数据源" v-if="currentStorageType === 'JDBC'">
          <span>{{ currentDatasourceName || '-' }}</span>
        </el-form-item>
        <el-form-item label="当前表名" v-if="currentStorageType === 'JDBC'">
          <span>{{ currentTableName || '-' }}</span>
        </el-form-item>
        <el-form-item label="是否可用">
          <el-tag :type="storageAvailable ? 'success' : 'danger'" size="small">
            {{ storageAvailable ? '正常' : '不可用' }}
          </el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <span class="card-title">切换存储数据源</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="选择数据源" prop="datasourceId">
          <el-select v-model="form.datasourceId" filterable placeholder="请选择数据源" style="width: 400px"
            @change="onDatasourceChange">
            <el-option v-for="item in datasourceList" :key="item.id" :label="item.datasourceName" :value="item.id">
              <span>{{ item.datasourceName }}</span>
              <span class="datasource-type-tag">[{{ item.datasourceType }}]</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="数据源类型" v-if="selectedDatasource">
          <span>{{ selectedDatasource.datasourceType }}</span>
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" placeholder="请输入表名，默认 quality_error_data" style="width: 400px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving" @mousedown="e => e.preventDefault()">
            <i class="iconfont-mini icon-xinzeng mr5"></i>保存并初始化
          </el-button>
          <el-button @click="handleRefresh" :loading="refreshing" @mousedown="e => e.preventDefault()">
            <i class="iconfont-mini icon-refresh mr5"></i>刷新状态
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="ErrorStorageConfig">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getErrorStorageConfig, setErrorStorageConfig, initErrorStorageTable } from '@/api/ast/quality/errorStorageConfig'
import { listDaDatasource } from '@/api/ast/dataSource/dataSource'

const JDBC_TYPES = ['MySql', 'MySQL', 'PostgreSQL', 'DM8', 'Oracle', 'Oracle11', 'Kingbase8', 'SQL_Server', 'SQL_Server2008', 'DB2', 'ClickHouse', 'Doris', 'Hive', 'MariaDB', 'OSCAR']

const formRef = ref(null)
const datasourceList = ref([])
const selectedDatasource = ref(null)
const currentDatasourceName = ref('')
const currentTableName = ref('')
const currentStorageType = ref('')
const storageAvailable = ref(false)
const saving = ref(false)
const refreshing = ref(false)

const form = reactive({
  datasourceId: null,
  tableName: 'quality_error_data'
})

const rules = {
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }]
}

function onDatasourceChange(id) {
  selectedDatasource.value = datasourceList.value.find(d => d.id === id) || null
}

function fetchDatasources() {
  listDaDatasource({ pageNum: 1, pageSize: 9999 }).then(res => {
    const all = res.data?.rows || res.data || []
    datasourceList.value = all.filter(d => {
      return JDBC_TYPES.includes(d.datasourceType)
    })
  })
}

function fetchConfig() {
  getErrorStorageConfig().then(res => {
    const data = res.data
    if (data) {
      currentDatasourceName.value = data.datasourceName || ''
      const config = data.config
      if (config) {
        currentTableName.value = config.tableName || ''
        currentStorageType.value = 'JDBC'
        form.datasourceId = config.datasourceId || null
        form.tableName = config.tableName || 'quality_error_data'
        onDatasourceChange(form.datasourceId)
      }
    }
  }).catch(() => {
    currentStorageType.value = ''
  })
}

function fetchStatus() {
  getErrorStorageConfig().then(res => {
    storageAvailable.value = !!(res.data?.config)
  }).catch(() => {
    storageAvailable.value = false
  })
}

function handleSave() {
  formRef.value.validate(valid => {
    if (!valid) return
    saving.value = true
    setErrorStorageConfig(form.datasourceId, form.tableName).then(() => {
      ElMessage.success('配置保存成功')
      return initErrorStorageTable()
    }).then(res => {
      ElMessage.success(res.msg || '表初始化完成')
      fetchConfig()
      fetchStatus()
    }).catch(err => {
      ElMessage.error(err.msg || '操作失败')
    }).finally(() => {
      saving.value = false
    })
  })
}

function handleRefresh() {
  refreshing.value = true
  initErrorStorageTable().then(res => {
    ElMessage.success(res.msg || '刷新成功')
    fetchConfig()
    fetchStatus()
  }).catch(err => {
    ElMessage.error(err.msg || '刷新失败')
  }).finally(() => {
    refreshing.value = false
  })
}

onMounted(() => {
  fetchDatasources()
  fetchConfig()
  fetchStatus()
})
</script>

<style scoped>
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.mb15 {
  margin-bottom: 15px;
}
.mr5 {
  margin-right: 5px;
}
.datasource-type-tag {
  color: #909399;
  font-size: 12px;
  margin-left: 6px;
}
</style>
