<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">错误明细存储配置</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="数据源" prop="datasourceId">
          <el-select v-model="form.datasourceId" filterable placeholder="请选择数据源" style="width: 400px">
            <el-option v-for="item in datasourceList" :key="item.id" :label="item.datasourceName" :value="item.id">
              <span>{{ item.datasourceName }}</span>
              <span class="datasource-type-tag">[{{ item.datasourceType }}]</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" placeholder="默认 quality_error_data" style="width: 400px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving" @mousedown="e => e.preventDefault()">
            保存
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="ErrorStorageConfig">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getErrorStorageConfig, setErrorStorageConfig } from '@/api/ast/quality/errorStorageConfig'
import { listDaDatasource } from '@/api/ast/dataSource/dataSource'

const JDBC_TYPES = ['MySql', 'MySQL', 'PostgreSQL', 'DM8', 'Oracle', 'Oracle11', 'Kingbase8', 'SQL_Server', 'SQL_Server2008', 'DB2', 'ClickHouse', 'Doris', 'Hive', 'MariaDB', 'OSCAR']

const formRef = ref(null)
const datasourceList = ref([])
const saving = ref(false)

const form = reactive({
  datasourceId: null,
  tableName: 'quality_error_data'
})

const rules = {
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }]
}

function fetchDatasources() {
  listDaDatasource({ pageNum: 1, pageSize: 9999 }).then(res => {
    const all = res.data?.rows || res.data || []
    datasourceList.value = all.filter(d => JDBC_TYPES.includes(d.datasourceType))
  })
}

function fetchConfig() {
  getErrorStorageConfig().then(res => {
    const data = res.data
    if (data) {
      form.datasourceId = data.datasourceId || null
      form.tableName = data.tableName || 'quality_error_data'
    }
  })
}

function handleSave() {
  formRef.value.validate(valid => {
    if (!valid) return
    saving.value = true
    setErrorStorageConfig(form.datasourceId, form.tableName).then(() => {
      ElMessage.success('配置成功')
    }).catch(err => {
      ElMessage.error(err.msg || '操作失败')
    }).finally(() => {
      saving.value = false
    })
  })
}

onMounted(() => {
  fetchDatasources()
  fetchConfig()
})
</script>

<style scoped>
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.datasource-type-tag {
  color: #909399;
  font-size: 12px;
  margin-left: 6px;
}
</style>
