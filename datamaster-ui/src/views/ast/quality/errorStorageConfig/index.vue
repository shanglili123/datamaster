<template>
  <div class="app-container">
    <a-card :bordered="false">
      <template #title>
        <span class="card-title">错误明细存储配置</span>
      </template>
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ style: { width: '120px' } }">
        <a-form-item label="数据源" name="datasourceId">
          <a-select v-model:value="form.datasourceId" show-search placeholder="请选择数据源" style="width: 400px">
            <a-select-option v-for="item in datasourceList" :key="item.id" :value="item.id">
              <span>{{ item.datasourceName }}</span>
              <span class="datasource-type-tag">[{{ item.datasourceType }}]</span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="表名" name="tableName">
          <a-input v-model:value="form.tableName" placeholder="默认 quality_error_data" style="width: 400px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSave" :loading="saving" @mousedown="e => e.preventDefault()">
            保存
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup name="ErrorStorageConfig">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getErrorStorageConfig, setErrorStorageConfig } from '@/api/ast/quality/errorStorageConfig'
import { listDaDatasource } from '@/api/ast/dataSource/dataSource'
import useUserStore from '@/store/system/user'

const JDBC_TYPES = ['MySql', 'MySQL', 'PostgreSQL', 'DM8', 'Oracle', 'Oracle11', 'Kingbase8', 'SQL_Server', 'SQL_Server2008', 'DB2', 'ClickHouse', 'Doris', 'Hive', 'MariaDB', 'OSCAR']
const userStore = useUserStore()

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
  listDaDatasource({ pageNum: 1, pageSize: 9999, spaceId: userStore.spaceId, spaceCode: userStore.spaceCode }).then(res => {
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
  formRef.value.validate().then(() => {
    saving.value = true
    setErrorStorageConfig(form.datasourceId, form.tableName).then(() => {
      message.success('配置成功')
    }).catch(err => {
      message.error(err.msg || '操作失败')
    }).finally(() => {
      saving.value = false
    })
  }).catch(() => { })
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
