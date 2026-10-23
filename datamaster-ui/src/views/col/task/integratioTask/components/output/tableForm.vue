<template>
  <!-- 表输出 -->
  <a-modal wrap-class-name="integration-component-modal" v-model:open="visibleDialog" :draggable="true" :title="currentNode?.data?.name"
    :closable="false" :destroy-on-close="true" class="medium-dialog" :mask-closable="false" :width="1200">
    <a-spin :spinning="loading">
    <a-form :model="form" :label-col="{ style: { width: '110px' } }"
      :disabled="info">
      <h3 class="etl-section-title">基本信息</h3>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="节点名称">
            <a-input v-if="!info" v-model:value="form.name" placeholder="请输入节点名称" />
            <div v-else class="form-readonly">{{ form.name }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="类型">
            <a-select v-if="!info" v-model:value="form.taskParams.typeName" placeholder="请输入类型" show-search disabled>
              <a-select-option v-for="dict in typeList" :key="dict.value" :label="dict.label" :value="dict.value">{{ dict.label }}</a-select-option>
            </a-select>
            <div v-else class="form-readonly">{{ form.taskParams.typeName }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述">
            <a-textarea v-if="!info" v-model:value="form.description" placeholder="请输入描述" />
            <div v-else class="form-readonly">{{ form.description || '-' }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="目标数据连接">
            <a-select v-if="!info" v-model:value="form.taskParams.writerDatasource.datasourceId" placeholder="请选择目标数据连接"
              @change="handleDatasourceChange" show-search allow-clear :disabled="creatingTable">
              <a-select-option v-for="dict in createTypeList" :key="dict.id" :label="dict.datasourceName"
                :value="String(dict.id)">{{ dict.datasourceName }}</a-select-option>
            </a-select>
            <div v-else class="form-readonly">{{createTypeList.find((item) => item.id ==
              form.taskParams.writerDatasource.datasourceId)?.datasourceName || '-'}}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="数据连接类型">
            <a-input v-if="!info" v-model:value="form.taskParams.writerDatasource.datasourceType" placeholder="请输入数据连接类型"
              disabled />
            <div v-else class="form-readonly">{{ form.taskParams.writerDatasource.datasourceType || '-' }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="数据连接实例">
            <a-input v-if="!info" v-model:value="form.taskParams.writerDatasource.dbname" placeholder="请输入数据连接实例" disabled />
            <div v-else class="form-readonly">{{ form.taskParams.writerDatasource.dbname || '-' }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12" v-if="!isKafkaWriter">
          <a-form-item label="目标表">
            <a-select v-if="!info" v-model:value="form.taskParams.target_asset_id" placeholder="请选择目标表" @change="handleChange"
              show-search :loading="loadingTables" :disabled="creatingTable" style="width:100%">
              <a-select-option v-for="item in TablesByDataSource" :key="item.tableName" :label="item.tableName"
                :value="item.tableName" />
            </a-select>
            <div v-else class="form-readonly">{{ form.taskParams.target_asset_id || '-' }}</div>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- Kafka 输出配置：Topic + 分区键（主键/键分区） -->
      <a-row :gutter="20" v-if="isKafkaWriter">
        <a-col :span="12">
          <a-form-item label="Topic" required>
            <a-input v-if="!info" v-model:value="form.taskParams.kafkaWriterConfig.topic"
              placeholder="请输入输出 Topic" />
            <div v-else class="form-readonly">{{ form.taskParams.kafkaWriterConfig?.topic || '-' }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="分区键字段">
            <a-select v-if="!info" v-model:value="form.taskParams.kafkaWriterConfig.partitionAssignColumns"
              mode="multiple" placeholder="选择作为消息 key 的字段（同 key 同分区）" style="width:100%">
              <a-select-option v-for="col in sourceColumnNames" :key="col" :value="col">{{ col }}</a-select-option>
            </a-select>
            <div v-else class="form-readonly">{{ (form.taskParams.kafkaWriterConfig?.partitionAssignColumns || []).join(', ') || '-' }}</div>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="20" v-if="isKafkaWriter && !info">
        <a-col :span="24">
          <a-form-item label="分区方式">
            <a-radio-group v-model:value="form.taskParams.kafkaWriterConfig.partitionStrategy">
              <a-radio value="hash-by-key">按分区键哈希（同 key 同分区）</a-radio>
              <a-radio value="all-to-zero">全部写入 partition 0</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>


      <template v-if="!isKafkaWriter">
        <div class="h2-title etl-section-title" role="heading" aria-level="3">新建目标表治理（仅创建新表时必填）</div>
        <a-form-item v-if="!info" label="基础名称" required>
          <a-input v-model:value="newTableName" placeholder="请输入基础业务过程编码，不含分层/域前缀" :disabled="creatingTable" />
        </a-form-item>
        <GovernedTableFields :model="form.taskParams.governance" :naming="naming" :readonly="info" :disabled="creatingTable" />
        <a-form-item v-if="!info" label="最终表名">
          <a-input :value="naming.previewResult?.tableName || ''" readonly placeholder="点击预览，由服务端生成最终名称" />
          <div style="display:flex;gap:8px;margin-top:8px">
            <a-button :loading="naming.previewLoading" :disabled="creatingTable" @click="naming.preview()">预览最终表名</a-button>
            <a-button v-hasPermi="['ast:dataSource:query']" type="primary" @click="handleCreateTable" :loading="creatingTable">创建新表</a-button>
            <a-button v-if="queryTarget" v-hasPermi="['ast:dataQuery:list']" @click="openDataQuery(queryTarget)">SQL查询</a-button>
          </div>
          <div style="margin-top:6px">分层、表类型和数据域/主题域仅用于新建表；保存已有任务无需补填。业务分类仅作为资产标签，不参与表名。</div>
        </a-form-item>
        <a-alert v-if="naming.previewError" type="error" show-icon :message="naming.previewError" style="margin-bottom:12px" />
        <a-alert v-if="createNotice" :type="createNotice.type" show-icon :message="createNotice.message" style="margin-bottom:12px" />
      </template>

      <a-alert v-if="!isKafkaWriter" type="info" show-icon style="margin: 12px 0"
        :message="`目标表必须具备 ${requiredMetadataColumns.join('、')} 及启用的可选追溯列；保存任务不会自动修改已有表结构。`" />
      <div class="h2-title etl-section-title" role="heading" aria-level="3" v-if="!isKafkaWriter">字段映射</div>

      <div v-if="!isKafkaWriter">
        <a-spin :spinning="loadingList">
          <YourChildComponent ref="childComponent" :tableFields="tableFields" :toColumnsList="ColumnByAssettab"
            :system-columns="requiredMetadataColumns" :info="info" />
        </a-spin>
      </div>
      <div class="h2-title etl-section-title" role="heading" aria-level="3" v-if="!isKafkaWriter">输出配置</div>

      <a-row :gutter="20" v-if="!isKafkaWriter">
        <a-col :span="24" class=" hasMsg">
          <a-form-item label="前置SQL">
            <a-textarea v-if="!info" v-model:value="form.taskParams.preSql" placeholder="请输入前置SQL，多条语句用分号分隔" />
            <div v-else class="form-readonly">{{ form.taskParams.preSql || '-' }}</div>
            <span class="msg"><InfoCircleOutlined />在目标数据库执行；多条普通 SQL 用分号分隔，不支持存储过程脚本分隔指令</span>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20" v-if="!isKafkaWriter">
        <a-col :span="24">
          <a-form-item label="质量检查">
            <a-checkbox v-if="!info" v-model:checked="form.taskParams.qualityConfig.enabled">写入后执行质量检查</a-checkbox>
            <div v-else class="form-readonly">{{ form.taskParams.qualityConfig.enabled ? '已启用' : '未启用' }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20" v-if="!isKafkaWriter && form.taskParams.qualityConfig.enabled">
        <a-col :span="12">
          <a-form-item label="失败策略">
            <a-select v-if="!info" v-model:value="form.taskParams.qualityConfig.onFailure">
              <a-select-option value="FAIL">失败即终止</a-select-option>
              <a-select-option value="WARN">失败仅告警</a-select-option>
            </a-select>
            <div v-else class="form-readonly">{{ form.taskParams.qualityConfig.onFailure || 'FAIL' }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20" v-if="!isKafkaWriter">
        <a-col :span="12">
          <a-form-item label="写入模式">
            <a-radio-group v-if="!info" v-model:value="form.taskParams.writeModeType">
              <a-radio :value="2">追加</a-radio>
              <a-radio :value="1">全量</a-radio>
              <a-radio :value="3">增量更新</a-radio>
            </a-radio-group>
            <div v-else class="form-readonly">{{ form.taskParams.writeModeType == 1 ? '全量' :
              form.taskParams.writeModeType == 2 ? '追加' : '增量更新' }}</div>

          </a-form-item>
        </a-col>
        <a-col :span="12" class=" hasMsg">
          <a-form-item label="单次写入数据">
            <a-input v-if="!info" v-model:value="form.taskParams.description" placeholder="请输入单次写入数据条数"
              type="number" addon-after="条" />
            <div v-else class="form-readonly">{{ form.taskParams.description ? form.taskParams.description + '条' : '-'
            }}</div>
            <span class="msg"><InfoCircleOutlined />不输入默认值1000条</span>
          </a-form-item>
        </a-col>
      </a-row>
      <a-alert v-if="!isKafkaWriter && String(form.taskParams.writeModeType) === '1'"
        type="warning" show-icon style="margin-bottom: 12px"
        message="全量模式：未配置有效前置 SQL 时，任务运行前将执行 TRUNCATE TABLE 清空目标表；配置自定义前置 SQL 后不再自动清表。" />
      <a-row :gutter="20"
        v-if="!isKafkaWriter && form.taskParams.writeModeType == 3 && form.taskParams.writerDatasource.datasourceType !== 'Doris'">
        <a-col :span="24">
          <a-form-item label="更新主键字段">
            <a-checkbox-group v-if="!info" v-model:value="form.taskParams.selectedColumns">
              <a-checkbox v-for="item in ColumnByAssettab" :key="item.id" :value="item.columnName">
                {{ item.columnName }}
              </a-checkbox>
            </a-checkbox-group>
            <div v-else class="form-readonly">{{ form.taskParams.selectedColumns || '-' }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20" v-if="!isKafkaWriter">
        <a-col :span="24" class=" hasMsg">
          <a-form-item label="后置SQL">
            <a-textarea v-if="!info" v-model:value="form.taskParams.postSql" placeholder="请输入后置SQL，多条语句用分号分隔" />
            <div v-else class="form-readonly">{{ form.taskParams.postSql || '-' }}</div>
            <span class="msg"><InfoCircleOutlined />数据同步完成后执行的SQL</span>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    </a-spin>
    <template #footer>
      <div style="text-align: right">
        <a-button @click="closeDialog">关闭</a-button>
        <a-button type="primary" @click="saveData" :disabled="creatingTable" v-if="!info">保存</a-button>
      </div>
    </template>
  </a-modal>
</template>
<script setup name="OutputForm">
import { InfoCircleOutlined } from "@ant-design/icons-vue";

import {
  listDaDatasource,
} from "@/api/ast/dataSource/dataSource.js";

import {
    handleType2TaskParams,
    getParentNode,
} from "@/views/col/utils/opBase.js";
import { typeList } from "@/utils/graph.js";
import {
    getTablesByDataSourceId,
    getColumnByAssetId,
    getLocalNodeUniqueKey as getNodeUniqueKey,
    createTaskTempTable,
} from "@/api/col/task/index.js";
import GovernedTableFields from '@/components/GovernedTableFields.vue';
import { useGovernedTableNaming } from '@/composables/useGovernedTableNaming';
import { useDataQueryNavigation } from '@/utils/dataQueryNavigation';
const { openDataQuery } = useDataQueryNavigation();
const { proxy } = getCurrentInstance();
import useUserStore from "@/store/system/user.js";
import YourChildComponent from "../fieldMap.vue";
const userStore = useUserStore();
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  currentNode: { type: Object, default: () => ({}) },
  info: { type: Boolean, default: false },
  graph: { type: Object, default: null },
});
const emit = defineEmits(["update", "confirm"]);
const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit("update", newValue);
  },
});

// 变量定义
let loading = ref(false);
let loadingList = ref(false);
let opens = ref(false);
let row = ref();
let TablesByDataSource = ref([]);
let ColumnByAssettab = ref([]);
let form = ref({ taskParams: { governance: { tableType: '1' }, qualityConfig: { enabled: false, onFailure: 'FAIL', rules: [] } } });
let loadingTables = ref(false)
let newTableName = ref('')
let creatingTable = ref(false)
const queryTarget = ref(null);
const createNotice = ref(null);
let dialogRequest = 0;
let tableRequest = 0;
let columnRequest = 0;
const naming = useGovernedTableNaming(
  () => form.value.taskParams?.governance,
  () => newTableName.value,
  () => form.value.taskParams?.writerDatasource?.datasourceType,
);
function handleRule(data) {
  row.value = {};
  row.value = data;
  opens.value = true;
}
const submitForm = (value) => {
  tableFields.value.forEach((column) => {
    if (row.value.id == column.id) {
      if (value.length > 0) {
        column.cleanRuleList = value;
        column.elementId = value.map((item) => item.ruleId);
      }
    }
    opens.value = false;
  });
};

function getSourceParams() {
  let node = props.currentNode;
  const visited = new Set();
  while (node && !visited.has(node.id)) {
    visited.add(node.id);
    node = getParentNode(node, props.graph);
    const params = node?.getProp?.('data')?.taskParams;
    if (params && String(params.type) === '1') return params;
  }
  return {};
}
const requiredMetadataColumns = computed(() => {
  const source = getSourceParams();
  const metadata = source.sourceMetadata || {};
  const enabled = value => value === true || value === 'true' || value === '1' || value === 1;
  const columns = [metadata.sourceTableColumn || 'source_table', metadata.writeTimeColumn || 'write_time'];
  const optionalColumns = [
    ['includeSourceDatasource', 'sourceDatasourceColumn', 'source_datasource'],
    ['includeSourceDatabase', 'sourceDatabaseColumn', 'source_database'],
    ['includeSourceTask', 'sourceTaskColumn', 'source_task'],
    ['includeSourceBatch', 'sourceBatchColumn', 'source_batch'],
    ['includeSourceUpdateTime', 'sourceUpdateTimeColumn', 'source_update_time'],
    ['includeRecordOp', 'recordOpColumn', 'record_op'],
    ['includeRowHash', 'rowHashColumn', 'source_row_hash'],
  ];
  optionalColumns.forEach(([flag, property, fallback]) => {
    if (enabled(metadata[flag])) columns.push(metadata[property] || fallback);
  });
  const naturalKeys = metadata.naturalKeyColumns?.length ? metadata.naturalKeyColumns
    : (source.sourceKeyColumns?.length ? source.sourceKeyColumns : (source.tableFields || []).filter(field => enabled(field.pkFlag)));
  if (naturalKeys.length && (metadata.includeNaturalKey == null || enabled(metadata.includeNaturalKey))) {
    columns.push(metadata.naturalKeyColumn || 'source_natural_key');
  }
  if (Array.isArray(metadata.preservedMetadataColumns)) columns.push(...metadata.preservedMetadataColumns);
  const names = new Map();
  columns.forEach(name => {
    const value = String(name || '').trim();
    if (value) names.set(value.toLowerCase(), value);
  });
  return [...names.values()];
});

const childComponent = ref(null); // 表字段
const tableFields = ref([]); // 来源表格
const createTypeList = ref([]); // 数据源列表

// 归一化数据源类型（与 Kafka 输入组件一致的判断口径）
const normalizeDatasourceType = (type) => String(type || '').replace(/[\s_-]/g, '').toLowerCase();
// 目标数据源是否为 Kafka
const isKafkaWriter = computed(() => {
  const ds = form.value?.taskParams?.writerDatasource;
  return normalizeDatasourceType(ds?.datasourceType) === 'kafka';
});
// Kafka 分区键可选字段（来自源表/上游输入字段）
const sourceColumnNames = computed(() => {
  const candidates = [
    form.value?.taskParams?.tableFields,
    tableFields.value,
    form.value?.taskParams?.inputFields,
  ];
  for (const fields of candidates) {
    if (!Array.isArray(fields) || !fields.length) continue;
    const names = fields
      .map(c => c?.columnName ?? c?.name ?? c?.key ?? '')
      .filter(Boolean);
    if (names.length) return names;
  }
  return [];
});

// 获取数据源列表（含 Kafka，支持输出到 Kafka）
const getDatasourceList = async () => {
  const dialog = dialogRequest;
  try {
    loading.value = true;
    const response = await listDaDatasource({
      spaceCode: userStore.spaceCode,
      spaceId: userStore.spaceId,
      datasourceType: "DM8,Oracle11,MySql,Oracle,Kingbase8,Doris,ClickHouse,Hive,MongoDB,Elasticsearch,SQL_Server,SQL_Server2008,PostgreSQL,Kafka",
      pageSize: 9999,
    });
    if (dialog === dialogRequest) createTypeList.value = response.data.rows;
  } catch (error) {
    if (dialog === dialogRequest) proxy.$message.warning(error?.message || '数据连接加载失败，请检查权限');
  } finally {
    if (dialog === dialogRequest) loading.value = false;
  }
};

// 获取表列表
const fetchTablesByDatasourceId = async (id) => {
  const request = ++tableRequest;
  const dialog = dialogRequest;
  try {
    loadingTables.value = true;
    const response = await getTablesByDataSourceId({ datasourceId: id });
    if (request === tableRequest && dialog === dialogRequest && String(id) === String(form.value.taskParams?.writerDatasource?.datasourceId)) TablesByDataSource.value = response.data || [];
  } finally {
    if (request === tableRequest) loadingTables.value = false;
  }
};

// 获取列数据
const getColumnByAssetIdList = async () => {
  const request = ++columnRequest;
  const dialog = dialogRequest;
  const datasourceId = form.value.taskParams.writerDatasource.datasourceId;
  const tableName = form.value.taskParams.target_asset_id;
  try {
    loadingList.value = true;
    const response = await getColumnByAssetId({ id: datasourceId, tableName });
    if (request === columnRequest && dialog === dialogRequest && datasourceId === form.value.taskParams.writerDatasource.datasourceId && tableName === form.value.taskParams.target_asset_id) ColumnByAssettab.value = response.data || [];
  } finally {
    if (request === columnRequest) loadingList.value = false;
  }
};

// 获取列数据
const getColumns = () => {
  return childComponent.value?.getColumns();
};

// 处理数据源变化
const resetAndFetchTables = async (selectedDatasource) => {
  ++tableRequest;
  ++columnRequest;
  loadingTables.value = false;
  loadingList.value = false;
  queryTarget.value = null;
  createNotice.value = null;
  naming.resetPreview();
  TablesByDataSource.value = [];
  ColumnByAssettab.value = [];
  let { datasourceType, datasourceConfig, ip, port, id } = selectedDatasource;
  let code = {};
  try { code = JSON.parse(datasourceConfig); } catch (e) { code = {}; }
  form.value.taskParams.target_asset_id = "";
  form.value.taskParams.target_table_name = "";
  form.value.taskParams.writerDatasource = {
    datasourceType,
    datasourceConfig,
    ip,
    port,
    dbname: code.dbname,
    target_asset_id: String(id),
    datasourceId: String(id),
    datasourceName: selectedDatasource.datasourceName,
  };
  form.value.taskParams.kafkaWriterConfig = { topic: "", partitionAssignColumns: [] };

  if (isKafkaWriter.value) {
    // Kafka 无表/列概念，无需拉取表列表
    return;
  }
  await fetchTablesByDatasourceId(id);
};

// 处理数据源变化
const handleDatasourceChange = (value) => {
  const selectedDatasource = createTypeList.value.find(
    (item) => item.id == value
  );
  if (selectedDatasource) {
    resetAndFetchTables(selectedDatasource).catch(error => proxy.$message.warning(error?.message || '目标表列表加载失败，请检查权限'));
  } else {
    ++tableRequest;
    ++columnRequest;
    naming.resetPreview();
    form.value.taskParams.writerDatasource = {};
    form.value.taskParams.target_asset_id = '';
    form.value.taskParams.target_table_name = '';
    TablesByDataSource.value = [];
    ColumnByAssettab.value = [];
    queryTarget.value = null;
    createNotice.value = null;
    loadingTables.value = false;
    loadingList.value = false;
  }
};

// 处理表变化
const setTableName = (selectedDatasource) => {
  form.value.taskParams.target_table_name = selectedDatasource.tableName;
};

const handleChange = (value) => {
  queryTarget.value = null;
  createNotice.value = null;
  const selectedDatasource = TablesByDataSource.value.find(
    (item) => item.tableName == value
  );
  if (selectedDatasource) {
    setTableName(selectedDatasource);
    ColumnByAssettab.value = [];
    getColumnByAssetIdList().catch(error => proxy.$message.warning(error?.message || '目标字段加载失败，请检查权限'));
  }
};

const off = () => {
  ++dialogRequest;
  ++tableRequest;
  ++columnRequest;
  naming.cancel();
  queryTarget.value = null;
  createNotice.value = null;
  creatingTable.value = false;
  loading.value = false;
  loadingTables.value = false;
  loadingList.value = false;
  // 清空表格字段数据
  ColumnByAssettab.value = [];
  TablesByDataSource.value = [];
  tableFields.value = [];
};
// 保存数据
const saveData = async () => {
  try {
    await nextTick();
    // 手动校验必填字段（避免 a-form dot-notation path 校验失效）
    if (!form.value?.name) {
      return proxy.$message.warning('请输入节点名称');
    }
    if (!form.value?.taskParams?.writerDatasource?.datasourceId) {
      return proxy.$message.warning('请选择目标数据连接');
    }
    if (isKafkaWriter.value) {
      // Kafka 输出：校验 Topic
      const writerCfg = form.value.taskParams.kafkaWriterConfig || {};
      if (!writerCfg.topic) {
        return proxy.$message.warning('请输入 Topic');
      }
    } else {
      if (!form.value?.taskParams?.target_asset_id) {
        return proxy.$message.warning('请选择目标表');
      }
      const targetNames = new Set(ColumnByAssettab.value.map(field =>
        String(field.columnName || field.name || '').trim().toLowerCase()));
      const missing = requiredMetadataColumns.value.filter(name => !targetNames.has(name.toLowerCase()));
      if (missing.length) {
        return proxy.$message.warning(`目标表缺少必写追溯列：${missing.join('、')}，请先补齐表结构并重新选择目标表`);
      }
      if (!form.value?.taskParams?.writeModeType && form.value?.taskParams?.writeModeType !== 0) {
        return proxy.$message.warning('请选择写入模式');
      }
      if (form.value?.taskParams?.writeModeType === 3 &&
          form.value?.taskParams?.writerDatasource?.datasourceType !== 'Doris' &&
          (!form.value?.taskParams?.selectedColumns || !form.value.taskParams.selectedColumns.length)) {
        return proxy.$message.warning('请选择更新主键字段');
      }
    }

    // 没有 code 时生成唯一 code
    if (!form.value.code) {
      loading.value = true;
      try {
        const { data } = await getNodeUniqueKey({
          spaceCode: userStore.spaceCode || "133545087166112",
          spaceId: userStore.spaceId,
        });
        form.value.code = data;
      } finally {
        loading.value = false;
      }
    }

    const taskParams = form.value.taskParams || {};
    delete taskParams.where;
    delete form.value.preSql;
    const mappedColumns = getColumns();

    taskParams.tableFields = mappedColumns ? mappedColumns.fromColumns : (taskParams.tableFields || tableFields.value);
    if (isKafkaWriter.value) {
      // Kafka：分区键作为消息 key，实现"同 key 同分区"的主键/键分区
      taskParams.kafkaWriterConfig = taskParams.kafkaWriterConfig || {};
      taskParams.target_columns = taskParams.tableFields ? taskParams.tableFields.map(c => c.columnName ?? c.name) : [];
      taskParams.columns = taskParams.tableFields;
    } else {
      taskParams.toColumnsList = mappedColumns ? mappedColumns.toColumns : ColumnByAssettab.value;
      const { target_columns, columns } = handleType2TaskParams(taskParams.tableFields, taskParams.toColumnsList);
      if (!columns.length) {
        return proxy.$message.warning('请至少连接一对业务字段');
      }
      if ([...columns, ...target_columns].some(name => !String(name || '').trim())) {
        return proxy.$message.warning('映射字段名称不能为空');
      }
      if (new Set(columns.map(name => String(name).toLowerCase())).size !== columns.length ||
          new Set(target_columns.map(name => String(name).toLowerCase())).size !== target_columns.length) {
        return proxy.$message.warning('映射字段名称重复，请检查上游和目标字段');
      }
      taskParams.target_columns = target_columns;
      taskParams.columns = columns;

      taskParams.outputFields = target_columns.map(name =>
        ColumnByAssettab.value.find(field => field.columnName === name) || { columnName: name });
      if (!taskParams.target_table_name && taskParams.target_asset_id) {
        taskParams.target_table_name = taskParams.target_asset_id;
      }
    }
    form.value.taskParams = { ...form.value.taskParams, ...taskParams }
    emit("confirm", form.value);

  } catch (error) {
    console.error("保存数据失败:", error);
    loading.value = false;
  }
};


const handleCreateTable = async () => {
  if (creatingTable.value) return;
  if (!newTableName.value?.trim()) {
    proxy.$modal.msgWarning('请输入基础名称');
    return;
  }
  const writer = { ...form.value.taskParams?.writerDatasource };
  if (!writer?.datasourceId) {
    proxy.$modal.msgWarning('请先选择目标数据连接');
    return;
  }
  if (!tableFields.value?.length) {
    proxy.$modal.msgWarning('没有源表字段信息');
    return;
  }
  const dialog = dialogRequest;
  const currentTarget = () => dialog === dialogRequest && props.visible && String(writer.datasourceId) === String(form.value.taskParams?.writerDatasource?.datasourceId);
  try {
    creatingTable.value = true;
    createNotice.value = null;
    const preview = await naming.preview();
    if (!preview || !currentTarget()) return;
    const namingPayload = { ...naming.payload };
    const parentParams = getSourceParams();
    const sourceMetadata = { ...(parentParams.sourceMetadata || {}), enabled: true };
    const naturalKeyColumns = (sourceMetadata.naturalKeyColumns || []).length
      ? sourceMetadata.naturalKeyColumns
      : (parentParams.tableFields || [])
        .filter(col => String(col.pkFlag) === '1' || col.pkFlag === true)
        .map(col => col.columnName || col.name)
        .filter(Boolean);
    const metadataColumns = sourceMetadata.enabled ? [
      ...(sourceMetadata.includeSourceDatasource ? [{ columnName: sourceMetadata.sourceDatasourceColumn || 'source_datasource', columnType: 'VARCHAR', columnLength: '64', pkFlag: '0', nullableFlag: '1' }] : []),
      { columnName: sourceMetadata.sourceTableColumn || 'source_table', columnType: 'VARCHAR', columnLength: '256', pkFlag: '0', nullableFlag: '1' },
      ...(sourceMetadata.includeSourceTask ? [{ columnName: sourceMetadata.sourceTaskColumn || 'source_task', columnType: 'VARCHAR', columnLength: '128', pkFlag: '0', nullableFlag: '1' }] : []),
      ...(sourceMetadata.includeSourceBatch ? [{ columnName: sourceMetadata.sourceBatchColumn || 'source_batch', columnType: 'VARCHAR', columnLength: '128', pkFlag: '0', nullableFlag: '1' }] : []),
      { columnName: sourceMetadata.writeTimeColumn || 'write_time', columnType: 'TIMESTAMP', pkFlag: '0', nullableFlag: '1' },
      ...(sourceMetadata.includeSourceDatabase ? [{ columnName: sourceMetadata.sourceDatabaseColumn || 'source_database', columnType: 'VARCHAR', columnLength: '128', pkFlag: '0', nullableFlag: '1' }] : []),
      ...(sourceMetadata.includeRowHash ? [{ columnName: sourceMetadata.rowHashColumn || 'source_row_hash', columnType: 'VARCHAR', columnLength: '64', pkFlag: '0', nullableFlag: '1' }] : []),
      ...(sourceMetadata.includeSourceUpdateTime && sourceMetadata.sourceUpdateTimeSourceColumn ? [{ columnName: sourceMetadata.sourceUpdateTimeColumn || 'source_update_time', columnType: 'TIMESTAMP', pkFlag: '0', nullableFlag: '1' }] : []),
      ...(sourceMetadata.includeRecordOp && sourceMetadata.recordOpSourceColumn ? [{ columnName: sourceMetadata.recordOpColumn || 'record_op', columnType: 'VARCHAR', columnLength: '16', pkFlag: '0', nullableFlag: '1' }] : []),
      ...(sourceMetadata.includeNaturalKey !== false && naturalKeyColumns.length
        ? [{ columnName: sourceMetadata.naturalKeyColumn || 'source_natural_key', columnType: 'VARCHAR', columnLength: '64', pkFlag: '0', nullableFlag: '1' }]
        : []),
    ] : [];
    const existingNames = new Set(tableFields.value.map(col => String(col.columnName || col.name || '').trim().toLowerCase()));
    const metadataNames = new Set();
    const conflicts = metadataColumns.filter(col => {
      const name = col.columnName.trim().toLowerCase();
      const duplicate = existingNames.has(name) || metadataNames.has(name);
      metadataNames.add(name);
      return duplicate;
    }).map(col => col.columnName);
    if (conflicts.length) {
      const message = `来源治理列与业务字段或其它治理列重名：${[...new Set(conflicts)].join('、')}。请修改来源治理列配置后重试，未创建表。`;
      createNotice.value = { type: 'warning', message };
      proxy.$modal.msgWarning(message);
      return;
    }
    const createColumns = [...tableFields.value, ...metadataColumns];
    const { data } = await createTaskTempTable({
      datasourceType: writer.datasourceType,
      datasourceConfig: writer.datasourceConfig,
      ip: writer.ip,
      port: writer.port,
      dbname: writer.dbname,
      datasourceId: writer.datasourceId,
      ...namingPayload,
      tableName: preview.tableName,
      baseTableName: namingPayload.baseTableName,
      newTableName: namingPayload.baseTableName,
      tableCase: '2',
      sourceMetadata,
      columns: createColumns.map(col => ({
        columnName: col.columnName,
        columnType: col.columnType,
        columnLength: col.columnLength,
        columnScale: col.columnScale,
        pkFlag: col.pkFlag,
        nullableFlag: col.nullableFlag,
        columnComment: col.columnComment,
      })),
    });
    if (!currentTarget()) return;
    if ((!data?.tableCreated && !data?.assetRegistered) || !data?.tableName) {
      createNotice.value = { type: 'warning', message: data?.message || '服务端未确认目标表可用，请检查目标库状态后再操作' };
      proxy.$modal.msgWarning(createNotice.value.message);
      return;
    }
    const actualName = data.tableName;
    // target_asset_id intentionally retains the historical table-name semantics.
    form.value.taskParams.target_asset_id = actualName;
    form.value.taskParams.target_table_name = actualName;
    form.value.taskParams.governance.baseTableName = namingPayload.baseTableName;
    form.value.taskParams.governance.tableCase = '2';
    queryTarget.value = { assetId: data.assetId, datasourceId: data.datasourceId || writer.datasourceId, tableName: actualName };
    if (data.assetRegistered !== true) {
      createNotice.value = { type: 'warning', message: `物理表 ${actualName} 已创建，但资产登记未成功：${data.message || '请检查登记权限或联系管理员补登记'}。请勿重复建表。` };
      proxy.$modal.msgWarning(createNotice.value.message);
    } else {
      createNotice.value = { type: 'success', message: data.message || `物理表 ${actualName} 已创建并登记资产` };
      proxy.$modal.msgSuccess(createNotice.value.message);
    }
    ColumnByAssettab.value = [];
    const refreshed = await Promise.allSettled([fetchTablesByDatasourceId(writer.datasourceId), getColumnByAssetIdList()]);
    if (currentTarget()) {
      if (!TablesByDataSource.value.some(item => item.tableName === actualName)) TablesByDataSource.value.push({ tableName: actualName });
      if (refreshed.some(result => result.status === 'rejected')) {
        proxy.$modal.msgWarning('物理表已创建，但表列表或字段刷新失败，请检查权限后重新选择目标表；请勿重复建表');
      }
    }
  } catch (error) {
    if (currentTarget()) {
      createNotice.value = { type: 'error', message: error?.response?.data?.msg || error?.message || '创建请求失败，请检查权限及目标库状态后再操作' };
      proxy.$modal.msgWarning(createNotice.value.message);
    }
  } finally {
    if (dialog === dialogRequest) creatingTable.value = false;
  }
};

const closeDialog = () => {
  off();
  // 关闭对话框
  emit("update", false);
};

// 监听属性变化
function deepCopy(data) {
  if (data === undefined || data === null) {
    return {};
  }
  try {
    return JSON.parse(JSON.stringify(data));
  } catch (e) {
    return {};
  }
}

// 处理数据源和列操作的共用函数
const handleDatasource = (datasource, assetId) => {
  if (!datasource) return;
  // Kafka 无表列表，跳过拉取
  if (normalizeDatasourceType(datasource.datasourceType) === 'kafka') return;
  if (datasource?.datasourceId) {
    fetchTablesByDatasourceId(datasource.datasourceId).catch(error => proxy.$message.warning(error?.message || '目标表列表加载失败，请检查权限'));
  } else {
    console.warn("无效的数据源信息", datasource);
  }
};
// 监听属性变化 — 使用 watch 限定依赖，避免表单字段变化触发回调覆盖用户操作
watch(
  () => [props.visible, props.currentNode?.id],
  ([visible]) => {
    if (!visible) {
      off();
      return;
    }
    off();
    getDatasourceList();

    const nodeData = props.currentNode?.getProp?.("data") || {};
    const copy = deepCopy(nodeData);
    // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
    Object.keys(form.value).forEach(k => { delete form.value[k]; });
    Object.assign(form.value, copy);

    // 确保嵌套对象存在
    form.value.taskParams = form.value.taskParams || {};
    if (form.value.taskParams.preSql == null) {
      form.value.taskParams.preSql = copy.preSql || '';
    }
    form.value.taskParams.writerDatasource = form.value.taskParams.writerDatasource || {};
    form.value.taskParams.governance = form.value.taskParams.governance || {};
    form.value.taskParams.governance.tableType = String(form.value.taskParams.governance.tableType || '1');
    form.value.taskParams.governance.tableCase = '2';
    naming.loadOptions();
    form.value.taskParams.selectedColumns = form.value.taskParams.selectedColumns || [];
    form.value.taskParams.qualityConfig = form.value.taskParams.qualityConfig || {};
    form.value.taskParams.qualityConfig.enabled = !!form.value.taskParams.qualityConfig.enabled;
    form.value.taskParams.qualityConfig.onFailure = form.value.taskParams.qualityConfig.onFailure || 'FAIL';
    form.value.taskParams.qualityConfig.rules = Array.isArray(form.value.taskParams.qualityConfig.rules)
      ? form.value.taskParams.qualityConfig.rules : [];
    form.value.taskParams.kafkaWriterConfig = form.value.taskParams.kafkaWriterConfig || {
      topic: "",
      partitionAssignColumns: [],
      partitionStrategy: "hash-by-key",
    };

    const taskParams = form.value.taskParams;
    const savedTableFields = Array.isArray(taskParams.tableFields)
      ? deepCopy(taskParams.tableFields)
      : deepCopy(taskParams.inputFields);
    tableFields.value = Array.isArray(savedTableFields) ? savedTableFields : [];
    // 尝试从上游输入节点获取源表名
    const sourceTableName = getSourceParams().asset_id || '';
    newTableName.value = taskParams.governance.baseTableName || sourceTableName;
    ColumnByAssettab.value = Array.isArray(taskParams.toColumnsList) ? taskParams.toColumnsList : [];

    // 加载当前数据源对应的表列表
    handleDatasource(form.value.taskParams.writerDatasource);
  },
  { immediate: true }
);
</script>


<style scoped lang="less">
.blue-text {
  color: #2666fb;
}
</style>
