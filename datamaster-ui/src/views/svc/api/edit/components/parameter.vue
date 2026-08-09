<template>
  <div>
    <a-spin :spinning="loading">
    <a-form ref="form2" :model="form2" :rules="rules" :label-col="{ style: { width: '120px' } }">
      <div class="header-container" style="margin-top: -10px;">
        <div class="header-left">
          <div class="blue-bar"></div>
          数据源配置
        </div>
      </div>
      <a-row :gutter="20">
        <a-col :span="8">
          <a-form-item label="配置方式" name="apiServiceType">
            <a-select v-model:value="form2.apiServiceType" placeholder="请选择配置方式" @change="configTypeSelectChanged"
              class="select-width">
              <a-select-option v-for="dict in ds_api_bas_info_api_service_type" :key="dict.id" :label="dict.label"
                :value="dict.value" />
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="数据源" name="sourceId">
            <a-select v-model:value="form2.sourceId" placeholder="请选择数据源" @change="sourceSelectChanged" class="select-width">
              <a-select-option v-for="source in sourceOptions" :key="source.id" :label="source.datasourceName"
                :value="source.id" :disabled="source.status === '0'" />
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8" v-if="form2.apiServiceType === '1'" :gutter="20">
          <a-form-item label="数据库表" name="table">
            <a-select v-model:value="form2.table" placeholder="请选择数据库表" @change="tableSelectChanged"
              show-search :filter-option="filterTable" class="select-width">
              <a-select-option v-for="item in form2.filteredTableOptions" :key="item.tableName"
                :label="item.tableComment ? item.tableComment : item.tableName" :value="item">
                <a-tooltip :disabled="isShowTooltip"
                  :title="item.tableName + (item.tableComment ? `(${item.tableComment})` : '')">
                  <div class="option-item" @mouseover="spanMouseenter($event)">
                    {{
                      item.tableName + (item.tableComment ? `(${item.tableComment})` : '')
                    }}
                  </div>
                </a-tooltip>
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <!-- SQL 编辑器 -->
      <a-row v-if="form2.apiServiceType === '2'" class="sql-editor-container">
        <a-col :span="24">
          <sql-editor ref="sqleditor" :value="form2.sqlText" class="sql-editor"
            @changeTextarea="changeTextarea($event)" />
        </a-col>
        <a-form-item v-if="form2.apiServiceType === '2'" class="sql-parse-btn-container">
          <a-button size="small" type="primary" @click="sqlParseFunction" class="sql-parse-btn">SQL解析</a-button>
        </a-form-item>
      </a-row>
      <!--      <div class="clearfix header-text">-->
      <!--          <div class="header-left">-->
      <!--              <div class="blue-bar"></div>-->
      <!--              参数配置-->
      <!--          </div>-->
      <!--      </div>-->
      <div class="header-container">
        <div class="header-left">
          <div class="blue-bar"></div>
          参数配置
        </div>
      </div>
      <a-form ref="form2" :model="form2" :label-col="{ style: { width: '100px' } }" label="字段列表：">
        <div class="header-text">
          请求参数
          <a-button v-if="form2.apiServiceType !== '2'" type="link" class="add-link"
            @click="openDialog('first')">
            新增参数
          </a-button>
        </div>
        <a-table :data-source="form2.reqParams" :columns="reqParamColumns" :pagination="false" striped :scroll="{ y: 250 }" row-key="paramName" size="small" class="tableStyle">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              <span>{{ index + 1 }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'nullable'">
              <a-checkbox :checked="record.nullable === '1'" @change="(e) => record.nullable = e.target.checked ? '1' : '0'" />
            </template>
            <template v-else-if="column.dataIndex === 'paramComment'">
              <a-input v-model:value="record.paramComment" placeholder="请输入描述" />
            </template>
            <template v-else-if="column.dataIndex === 'paramType'">
              <a-select v-model:value="record.paramType" placeholder="请选择参数类型">
                <a-select-option v-for="dict in ds_api_param_type" :key="dict.id" :value="dict.value">{{ dict.label }}</a-select-option>
              </a-select>
            </template>
            <template v-else-if="column.dataIndex === 'whereType'">
              <a-select v-model:value="record.whereType" placeholder="请选择操作符">
                <a-select-option v-for="dict in da_api_param_operator" :key="dict.id" :value="dict.value">{{ dict.label }}</a-select-option>
              </a-select>
            </template>
            <template v-else-if="column.dataIndex === 'exampleValue'">
              <a-input v-model:value="record.exampleValue" placeholder="请输入示例值" />
            </template>
            <template v-else-if="column.dataIndex === 'defaultValue'">
              <a-input v-model:value="record.defaultValue" placeholder="请输入默认值" />
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="handleDelete(index)">删除</a-button>
            </template>
          </template>
        </a-table>
        <div class="header-text">
          返回字段
          <a-button type="link" v-if="form2.apiServiceType !== '2'" class="add-link"
            @click="openDialog('second')">
            新增参数
          </a-button>
        </div>
        <a-table class="tableStyle" :data-source="form2.resParams" :columns="resParamColumns" :pagination="false" striped :scroll="{ y: 250 }" row-key="fieldName" size="small">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              <span>{{ index + 1 }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'fieldComment'">
              <a-input v-model:value="record.fieldComment" placeholder="请输入描述" />
            </template>
            <template v-else-if="column.dataIndex === 'dataType'">
              <a-select v-model:value="record.dataType" allow-clear placeholder="请选择数据类型">
                <a-select-option v-for="dict in ds_api_param_type" :key="dict.id" :value="dict.value">{{ dict.label }}</a-select-option>
              </a-select>
            </template>
            <template v-else-if="column.dataIndex === 'dateFormat'">
              <a-input v-if="record.dataType == '4'" v-model:value="record.dateFormat" placeholder="请输入时间格式" />
              <span v-else>-</span>
            </template>
            <template v-else-if="column.dataIndex === 'exampleValue'">
              <a-input v-model:value="record.exampleValue" placeholder="请输入示例值" />
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="handleDelete(index, true)">删除</a-button>
            </template>
          </template>
        </a-table>

        <a-table v-if="form2.apiServiceType === '1' && false" row-key="id" :scroll="{ y: 250 }"
          class="tableStyle" :data-source="form2.sortParams" :columns="sortParamColumns" :pagination="false" striped>
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              <span>{{ index + 1 }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="handlesortDelete(index, true)">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </a-form>
    </a-spin>
    <tableDialog v-if="firstDialogVisible" :visible="firstDialogVisible" dialog-title="请求参数"
      :tableData="form2.fieldParams" @confirm="handleFirstConfirm" @close="firstDialogVisible = false"
      :list="form2.reqParams" :apiServiceType="form2.apiServiceType" :inputList="inputList" />
    <tableDialog v-if="secondDialogVisible" :visible="secondDialogVisible" dialog-title="返回字段"
      :tableData="form2.fieldParams" @confirm="handleSecondConfirm" @close="secondDialogVisible = false"
      :list="form2.resParams" />
    <tableDialog :visible="sortDialogVisible" dialog-title="返回字段" :tableData="form2.fieldParams"
      @confirm="handlesortConfirm" @close="sortDialogVisible = false" :list="form2.sortParams" />
  </div>
</template>

<script setup name="parameter">
import { message } from 'ant-design-vue'
import Sortable from "sortablejs";

import SqlEditor from "@/components/SqlEditor";

import tableDialog from "./tableDialog.vue";

import {
  tableList,
  columnsList
} from "@/api/ast/dataSource/dataSource.js";

import { sqlParse } from "@/api/svc/api/api.js";

const { proxy } = getCurrentInstance();
const {
  ds_api_bas_info_api_service_type,
  ds_api_param_type,
  da_api_param_operator
} = proxy.useDict(
  "ds_api_bas_info_api_service_type",
  "ds_api_param_type",
  "da_api_param_operator"
);

const props = defineProps({
  form2: {
    type: Array,
    default: () => [],
    required: true,
  },
  rules: {
    type: Object,
    required: true,
  },
  configTypeOptions: {
    type: Array,
    required: true,
  },
  sourceOptions: {
    type: Array,
    required: true,
  },

  paramTypeOptions: {
    type: Array,
    required: true,
  },
  whereTypeOptions: {
    type: Array,
    required: true,
  },
  splReult: {
    type: Boolean,
    default: false,
  },
  activeReult: {
    type: Number,
    default: 0,
  },
});
let loading = ref(false);
const data = reactive({
  lastSqlText: "", // 存储上次的 SQL 文本，用于检测是否发生变化
  firstDialogVisible: false,
  secondDialogVisible: false,
  sortDialogVisible: false,
  isShowTooltip: false,
});

const {
  lastSqlText,
  firstDialogVisible,
  secondDialogVisible,
  sortDialogVisible,
  isShowTooltip

} = toRefs(data);

const reqParamColumns = computed(() => {
  const cols = [
    { title: '序号', key: 'index', align: 'center', width: 80 },
    { title: '参数名称', dataIndex: 'paramName', align: 'center', ellipsis: true },
    { title: '是否允许为空', dataIndex: 'nullable', align: 'center', ellipsis: true },
    { title: '描述', dataIndex: 'paramComment', align: 'center' },
    { title: '参数类型', dataIndex: 'paramType', align: 'center' },
  ];
  if (props.splReult !== true) {
    cols.push({ title: '操作符', dataIndex: 'whereType', align: 'center' });
  }
  cols.push(
    { title: '示例值', dataIndex: 'exampleValue', align: 'center', ellipsis: true },
    { title: '默认值', dataIndex: 'defaultValue', align: 'center', ellipsis: true },
  );
  if (props.form2.apiServiceType !== '2') {
    cols.push({ title: '操作', key: 'actions', align: 'center', width: 150 });
  }
  return cols;
});

const resParamColumns = computed(() => {
  const cols = [
    { title: '序号', key: 'index', align: 'center', width: 80 },
    { title: '中文名称', dataIndex: 'fieldName', align: 'center', ellipsis: true },
    { title: '描述', dataIndex: 'fieldComment', align: 'center', ellipsis: true },
    { title: '数据类型', dataIndex: 'dataType', align: 'center', ellipsis: true },
    { title: '时间格式', dataIndex: 'dateFormat', align: 'center', ellipsis: true },
    { title: '示例值', dataIndex: 'exampleValue', align: 'center', ellipsis: true },
  ];
  if (props.form2.apiServiceType !== '2') {
    cols.push({ title: '操作', key: 'actions', align: 'center', width: 150 });
  }
  return cols;
});

const sortParamColumns = computed(() => {
  const cols = [
    { title: '序号', key: 'index', align: 'center', width: 80 },
    { title: '中文名称', dataIndex: 'fieldName', align: 'center', ellipsis: true },
  ];
  if (props.form2.apiServiceType !== '2') {
    cols.push({ title: '操作', key: 'actions', align: 'center' });
  }
  cols.push({ title: '拖动', key: 'drag', align: 'center', width: 75 });
  return cols;
});
function getTableInfo(sourceId) {
  tableList(sourceId).then((response) => {
    props.form2.filteredTableOptions = response.data;
  });


}
const rules = {
  name: [{ required: true, message: "请输入参数名称", trigger: "blur" }],
  columnType: [
    { required: true, message: "请选择参数类型", trigger: "change" },
  ],
};
//监听form2.sourceId发生变化查询表格信息
watch(
  () => props.form2?.sourceId,
  (newValue) => {
    if (newValue) {
      console.log("🚀 ~ newValue:", newValue)
      getTableInfo(newValue);
    }
  }
);

watch(
  () => props.form2?.table,
  (newValue, oldValue) => {
    if (
      newValue?.id &&
      oldValue?.id &&
      newValue.id !== oldValue.id
    ) {
      props.form2.reqParams = [];
      props.form2.resParams = [];
    }
  },
  { flush: 'post', deep: false }
);


function sqlParseFunction() {
  if (!props.form2.sourceId) {
    props.$message.warning("数据源不能为空");
    return;
  }
  if (!props.form2.sqlText) {
    props.$message.warning("解析SQL不能为空");
    return;
  }
  const data = {};
  data.sourceId = props.form2.sourceId;
  data.sqlText = props.form2.sqlText;
  sqlParse(data).then((response) => {
    if (response.code === 200) {
      const { data } = response;
      props.form2.reqParams = data.reqParams;
      props.form2.resParams = data.resParams;
      props.form2.lastSqlText = props.form2.sqlText;
      props.splReult = true;
      proxy.$modal.msgSuccess("解析成功，请进行下一步");
    } else {
      proxy.$modal.msgWarning(response.msg || "解析失败，请重试");
    }
  });
}

function setSort() {
  const el = this.$refs.dragTable.$el.querySelectorAll(
    ".el-table__body-wrapper > table > tbody"
  )[0];
  const sortable = Sortable.create(el, {
    handle: ".allowDrag",
    onEnd: (evt) => {
      const targetRow = this.form2.sortParams.splice(evt.oldIndex, 1)[0];
      this.form2.sortParams.splice(evt.newIndex, 0, targetRow);
      for (let index in this.form2.sortParams) {
        this.form2.sortParams[index].sort = parseInt(index) + 1;
      }
    },
  });
}

function validateFormParameter(formName, callback) {
  proxy.$refs[formName].validate((valid) => {
    if (valid) {
      if (props.form2.resParams.length <= 0 && props.form2.apiServiceType != 3) {
        proxy.$message.warning("验证失败，返回字段不能为空");
      } else if (
        props.form2.apiServiceType !== 1 &&
        props.form2.lastSqlText &&
        props.form2.lastSqlText !== props.form2.sqlText
      ) {
        proxy.$message.info("SOL变化请重新解析");
      } else {
        callback(props.form2);
      }
    } else {
      proxy.$message.warning("验证失败，请检查后重试");
      return false;
    }
  });
}
defineExpose({
  validateFormParameter,
  getTableInfo,
});
function closeDialog() {
  firstDialogVisible.value = false;
  secondDialogVisible.value = false;
  sortDialogVisible.value = false;
}
function handleFirstConfirm(val) {
  if (props.form2.apiServiceType != '3') {
    let params = [];
    val.forEach((item) => {
      const exists = props.form2.reqParams.some(param => param.columnName === item.engName);
      if (!exists) {
        params.push({
          paramName: item.engName,
          paramComment: item.cnName || undefined,
          nullable: "0",
          ...item,
        });
      }
    });

    if (params.length > 0) {
      props.form2.reqParams = [...props.form2.reqParams, ...params];
    }
  } else {
    val.forEach((item) => {
      const exists = props.form2.reqParams.some(param => param.columnName === item.columnName);

      if (!exists) {
        props.form2.reqParams.push(item);
      }
    });
  }

  closeDialog();
}
function handleSecondConfirm(val) {
  props.form2.resParams = [];
  props.form2.resParams = val.map((row) => {
    return {
      fieldName: row.engName,
      fieldComment: row.cnName || undefined,
      dataType: row.dataType || undefined,
      ...row,
    };
  });
  closeDialog();
}

function handlesortConfirm(val) {
  props.form2.sortParams = [];
  props.form2.sortParams = val.map((row) => {
    return {
      fieldName: row.columnName,
      fieldComment: row.columnComment || undefined,
      dataType: row.dataType || undefined,
      ...row,
    };
  });
  closeDialog();
}

function configTypeSelectChanged() {
  if (props.form2.apiServiceType != "1") {
    props.form2.reqParams = [];
    props.form2.resParams = [];
    props.form2.headerJson = [];
    props.form2.table = {};
  }
}

function sourceSelectChanged(e) {
  props.form2.reqParams = [];
  props.form2.resParams = [];
  props.form2.table = {};

  let source = props.sourceOptions.filter((i) => i.id == e)[0];
  let config = JSON.parse(source.datasourceConfig)
  props.form2.dbType = source.datasourceType;
  props.form2.dbName = config.dbname;
  props.form2.sid = config.sid;
}
const inputList = computed(() =>
  props.form2.fieldParams.filter((item) => Number(item.type) == 1)
);

function tableSelectChanged(item) {
  loading.value = true;
  let type = props.sourceOptions.filter(i => i.id == props.form2.sourceId)[0]?.datasourceType
  const data = {
    id: props.form2.sourceId,
    tableName: item.tableName,
    type
  };
  props.form2.tableId = item.id;
  props.form2.tableName = item.tableName;
  columnsList(data)
    .then((response) => {
      props.form2.fieldParams = response.data;
    })
    .finally(() => {
      loading.value = false;
    });
  props.form2.reqParams = [];
  props.form2.resParams = [];

  // listDataColumn(data).then(response => {
  //     if (response.success) {
  //         this.form2.fieldParams = response.data;
  //         this.form2.reqParams = [];
  //         this.form2.resParams = [];
  //         this.form2.sortParams = []
  //         this.form2.sortBy = []
  //         // this.setSort()
  //     } else {
  //         this.$notify({
  //             title: "提示",
  //             dangerouslyUseHTMLString: true, // 启用 HTML 字符串解析
  //             message: response.msg,
  //             type: "error",
  //             duration: 2000
  //         });
  //     }
  // });
}

function openDialog(type) {
  if (type === "first") firstDialogVisible.value = true;

  if (type === "second") secondDialogVisible.value = true;
  if (type === "3") {
    sortDialogVisible.value = true;
  }

}

function handleDelete(index, falg) {
  if (falg) {
    props.form2.resParams.splice(index, 1);
  } else {
    props.form2.reqParams.splice(index, 1);
  }
  proxy.$message({
    type: "success",
    message: "删除成功!",
  });
}

function handlesortDelete(index) {
  this.form2.sortParams.splice(index, 1);
  this.$message({
    type: "success",
    message: "删除成功!",
  });
}

function spanMouseenter(e) {
  let target = e.target;
  if (target.clientWidth < target.scrollWidth) {
    this.isShowTooltip = false;
  } else {
    this.isShowTooltip = true;
  }
}

function filterTable(query) { }

function changeTextarea(val) {
  props.form2.sqlText = val;
}
function deleteRow(index, row) {
  const rowIndex = props.form2.reqParams.findIndex(item => item.id == row.id);
  if (rowIndex !== -1) {
    props.form2.reqParams.splice(rowIndex, 1);
  }
}
function deleteRows(index, row) {
  const rowIndex = props.form2.resParams.findIndex(item => item.id == row.id);
  if (rowIndex !== -1) {
    props.form2.resParams.splice(rowIndex, 1);
  }
}

</script>


<style scoped lang="less">
.tableStyle {
  font-size: 14px;
  margin: 0px !important;
}

.home {
  display: flex;
  flex-direction: column;
  height: 88vh;

}

.header-container {
  height: 36px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
  margin: 10px 0;
  border-radius: 4px;
}

.blue-bar {
  background-color: #2666fb;
  width: 5px;
  height: 20px;
  margin-right: 10px;
  border-radius: 2px;
}

.header-text {
  margin: 12px 0
}

.header-left {
  display: flex;
  align-items: center;
  font-size: 16px;
  line-height: 24px;
  font-style: normal;
}

.option-item {
  cursor: pointer;
}

::v-deep.ant-select-item-option {
  max-width: 569px !important;
}

.ant-input,
.ant-select {
  width: 100%;
}

.select-width {
  width: 98%;
}

.header-text {
  font-size: 14px;
  margin-bottom: 3px;
  margin: 10px 0;
}

.add-link {
  margin-left: 10px;
  margin: 10px 0;
}

.sort-section {
  font-size: 14px;
  height: 40px;
  margin: 5px 0;
  display: flex;
  justify-content: space-between;
}

.sql-editor {
  height: 300px;
  margin: 10px 10px;
}

.allowDrag {
  cursor: pointer;
  /* 鼠标悬停时显示小手光标 */
}

.sql-editor-container {
  position: relative;
}

.sql-parse-btn-container {
  position: absolute;
  top: -30px;
  right: 10px;
  z-index: 10;
}
</style>

