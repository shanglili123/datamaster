<template>
  <!-- 表输出 -->
  <a-modal v-model:open="visibleDialog" :draggable="true" :title="currentNode?.data?.name"
    :closable="false" :destroy-on-close="true" class="medium-dialog" :mask-closable="false" :width="1200">
    <a-spin :spinning="loading">
    <a-form :model="form" :label-col="{ style: { width: '110px' } }"
      :disabled="info">
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
              @change="handleDatasourceChange" show-search>
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
        <a-col :span="12">
          <a-form-item label="选择表">
            <a-select v-if="!info" v-model:value="form.taskParams.target_asset_id" placeholder="请选择表" @change="handleChange"
              show-search :loading="loadingTables" style="width:100%">
              <a-select-option v-for="item in TablesByDataSource" :key="item.tableName" :label="item.tableName"
                :value="item.tableName" />
            </a-select>
            <div v-else class="form-readonly">{{ form.taskParams.target_asset_id || '-' }}</div>
            <div v-if="!info" style="display:flex;gap:8px;margin-top:8px">
              <a-input v-model:value="newTableName" placeholder="新表名（默认源表名）" style="flex:1" />
              <a-button type="primary" @click="handleCreateTable" :loading="creatingTable">创建新表</a-button>
            </div>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="where条件">
            <a-textarea v-if="!info" v-model:value="form.taskParams.where" placeholder="请输入where条件" />
            <div v-else class="form-readonly">{{ form.taskParams.where || '-' }}</div>
          </a-form-item>
        </a-col>
      </a-row>

      <div class="h2-title">字段映射</div>

      <div style="margin-top: -20px">
        <a-spin :spinning="loadingList">
          <YourChildComponent ref="childComponent" :tableFields="tableFields" :toColumnsList="ColumnByAssettab"
            :info="info" />
        </a-spin>
      </div>
      <div class="h2-title">输出配置</div>

      <a-row :gutter="20">
        <a-col :span="24" class=" hasMsg">
          <a-form-item label="前置SQL">
            <a-textarea v-if="!info" v-model:value="form.preSql" placeholder="请输入前置SQL" />
            <div v-else class="form-readonly">{{ form.preSql || '-' }}</div>
            <span class="msg"><InfoCircleOutlined />数据写入之前执行的SQL</span>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
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
      <a-row :gutter="20"
        v-if="form.taskParams.writeModeType == 3 && form.taskParams.writerDatasource.datasourceType !== 'Doris'">
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
      <a-row :gutter="20">
        <a-col :span="24" class=" hasMsg">
          <a-form-item label="后置SQL">
            <a-textarea v-if="!info" v-model:value="form.taskParams.postSql" placeholder="请输入后置SQL" />
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
        <a-button type="primary" @click="saveData" v-if="!info">保存</a-button>
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
let form = ref({});
let loadingTables = ref(false)
let newTableName = ref('')
let creatingTable = ref(false)
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

const childComponent = ref(null); // 表字段
const tableFields = ref([]); // 来源表格
const createTypeList = ref([]); // 数据源列表

// 获取数据源列表
const getDatasourceList = async () => {
  try {
    loading.value = true;
    const response = await listDaDatasource({
      spaceCode: userStore.spaceCode,
      spaceId: userStore.spaceId,
      datasourceType: "DM8,Oracle11,MySql,Oracle,Kingbase8,Doris,ClickHouse,Hive,MongoDB,Elasticsearch,SQL_Server,SQL_Server2008,PostgreSQL",
      pageSize: 9999,
    });
    createTypeList.value = response.data.rows;
  } finally {
    loading.value = false;
  }
};

// 获取表列表
const fetchTablesByDatasourceId = async (id) => {
  try {
    loadingTables.value = true;
    const response = await getTablesByDataSourceId({ datasourceId: id });
    TablesByDataSource.value = response.data || [];
  } finally {
    loadingTables.value = false;
  }
};

// 获取列数据
const getColumnByAssetIdList = async () => {
  try {
    loadingList.value = true;
    const response = await getColumnByAssetId({
      id: form.value.taskParams.writerDatasource.datasourceId,
      tableName: form.value.taskParams.target_asset_id,
    });
    ColumnByAssettab.value = response.data || [];
  } finally {
    loadingList.value = false;
  }
};

// 获取列数据
const getColumns = () => {
  return childComponent.value?.getColumns();
};

// 处理数据源变化
const resetAndFetchTables = async (selectedDatasource) => {
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

  await fetchTablesByDatasourceId(id);
};

// 处理数据源变化
const handleDatasourceChange = (value) => {
  const selectedDatasource = createTypeList.value.find(
    (item) => item.id == value
  );
  if (selectedDatasource) {
    resetAndFetchTables(selectedDatasource);
  }
};

// 处理表变化
const setTableName = (selectedDatasource) => {
  form.value.taskParams.target_table_name = selectedDatasource.tableName;
};

const handleChange = (value) => {
  const selectedDatasource = TablesByDataSource.value.find(
    (item) => item.tableName == value
  );
  if (selectedDatasource) {
    setTableName(selectedDatasource);
    ColumnByAssettab.value = [];
    getColumnByAssetIdList();
  }
};

const off = () => {
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
    if (!form.value?.taskParams?.target_asset_id) {
      return proxy.$message.warning('请选择表');
    }
    if (!form.value?.taskParams?.writeModeType && form.value?.taskParams?.writeModeType !== 0) {
      return proxy.$message.warning('请选择写入模式');
    }
    if (form.value?.taskParams?.writeModeType === 3 &&
        form.value?.taskParams?.writerDatasource?.datasourceType !== 'Doris' &&
        (!form.value?.taskParams?.selectedColumns || !form.value.taskParams.selectedColumns.length)) {
      return proxy.$message.warning('请选择更新主键字段');
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
    const { fromColumns = [], toColumns = [] } = getColumns() || {};

    taskParams.tableFields = fromColumns.length ? fromColumns : taskParams.tableFields;
    taskParams.toColumnsList = toColumns.length ? toColumns : ColumnByAssettab.value;
    const { target_columns, columns } = handleType2TaskParams(taskParams.tableFields, taskParams.toColumnsList);
    taskParams.target_columns = target_columns;
    taskParams.columns = columns;

    taskParams.outputFields = ColumnByAssettab.value;
    if (!taskParams.target_table_name && taskParams.target_asset_id) {
      taskParams.target_table_name = taskParams.target_asset_id;
    }
    form.value.taskParams = { ...form.value.taskParams, ...taskParams }
    emit("confirm", form.value);

  } catch (error) {
    console.error("保存数据失败:", error);
    loading.value = false;
  }
};


const handleCreateTable = async () => {
  if (!newTableName.value) {
    proxy.$modal.msgWarning('请输入新表名');
    return;
  }
  const writer = form.value.taskParams?.writerDatasource;
  if (!writer?.datasourceId) {
    proxy.$modal.msgWarning('请先选择目标数据连接');
    return;
  }
  if (!tableFields.value?.length) {
    proxy.$modal.msgWarning('没有源表字段信息');
    return;
  }
  try {
    creatingTable.value = true;
    const { data } = await createTaskTempTable({
      datasourceType: writer.datasourceType,
      datasourceConfig: writer.datasourceConfig,
      ip: writer.ip,
      port: writer.port,
      dbname: writer.dbname,
      tableName: newTableName.value,
      columns: tableFields.value.map(col => ({
        columnName: col.columnName,
        columnType: col.columnType,
        columnLength: col.columnLength,
        columnScale: col.columnScale,
        pkFlag: col.pkFlag,
        nullableFlag: col.nullableFlag,
        columnComment: col.columnComment,
      })),
    });
    proxy.$modal.msgSuccess('创建成功');
    await fetchTablesByDatasourceId(writer.datasourceId);
    form.value.taskParams.target_asset_id = newTableName.value;
    form.value.taskParams.target_table_name = newTableName.value;
    // 自动获取新建表的字段并回显
    ColumnByAssettab.value = [];
    getColumnByAssetIdList();
  } catch (error) {
    console.error('创建表失败:', error);
  } finally {
    creatingTable.value = false;
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
  if (datasource?.datasourceId) {
    fetchTablesByDatasourceId(datasource.datasourceId);
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
    getDatasourceList();

    const nodeData = props.currentNode?.getProp?.("data") || {};
    const copy = deepCopy(nodeData);
    // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
    Object.keys(form.value).forEach(k => { delete form.value[k]; });
    Object.assign(form.value, copy);

    // 确保嵌套对象存在
    form.value.taskParams = form.value.taskParams || {};
    form.value.taskParams.writerDatasource = form.value.taskParams.writerDatasource || {};
    form.value.taskParams.selectedColumns = form.value.taskParams.selectedColumns || [];

    const taskParams = form.value.taskParams;
    const savedTableFields = taskParams.tableFields?.length
      ? deepCopy(taskParams.tableFields)
      : deepCopy(taskParams.inputFields);
    tableFields.value = Array.isArray(savedTableFields) ? savedTableFields : [];
    // 尝试从上游输入节点获取源表名
    const parentNode = getParentNode(props.currentNode, props.graph);
    const sourceTableName = parentNode?.getProp?.("data")?.taskParams?.asset_id || '';
    newTableName.value = sourceTableName;
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
