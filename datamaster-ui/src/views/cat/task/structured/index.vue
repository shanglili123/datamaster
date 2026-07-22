<template>
  <div class="app-container dpp-task-list-page" ref="app-container">

    <el-container>
      <SourceSystemTree
        ref="sourceSystemTreeRef"
        @node-click="handleNodeClick"
        @data-loaded="handleTreeDataLoaded"
      />
      <el-main class="main-content">
        <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
          <template #search>
            <qt-search-bar
              v-bind="searchStore"
              :params="tableStore.params"
              @query="handleQueryClick"
              @reset="handleResetQueryClick"
            />
          </template>
          <template #actions-data>
            <el-button type="primary" plain @click="handleAddClick">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </el-button>
            <el-button
              type="danger"
              plain
              icon="Delete"
              :disabled="!store.rows.length"
              @click="handleDeleteColumnClick"
            >
              删除
            </el-button>
          </template>
          <qt-table v-bind="tableStore" ref="tableRef">
            <template #name="{ row }">
              <div class="name-label task-title">
                <div class="justify task-title-row" @click="handleDetailClick(row)">
                  <img
                    :src="getDatasourceIcon(row.dbType)"
                    alt=""
                    class="datasource-icon"
                    v-if="getDatasourceIcon(row.dbType)"
                  />
                  <el-link
                    type="primary"
                    :underline="false"
                    class="task-name-text task-name-ellipsis"
                    :title="row.name"
                  >
                    {{ row.name || "-" }}
                  </el-link>
                  <el-tag
                    type="primary"
                    :underline="false"
                    class="task-cat-ellipsis"
                    :title="row.sourceSystemName"
                  >
                    {{ row.sourceSystemName || "-" }}
                  </el-tag>
                </div>
                <div class="text-ellipsis desc-text" :title="row.description">
                  {{ row.description || "-" }}
                </div>
              </div>
            </template>
            <template #releaseState="{ row }">
              <div class="task-status-stack fz12">
                <div class="flex-center">
                  <span class="black-label mr5">发布状态:</span>
                  <el-tag :type="row.status == '1' ? 'success' : 'warning'">
                    {{ row.status == "1" ? "已发布" : "未发布" }}
                  </el-tag>
                </div>
                <div class="flex-center">
                  <span class="black-label mr5">调度状态:</span>
                  <el-tag :type="row.schedulerStatus == '1' ? 'success' : 'info'">
                    {{ row.schedulerStatus == "1" ? "已上线" : "未上线" }}
                  </el-tag>
                </div>
              </div>
            </template>
            <template #cronExpression="{ row }">
              <div class="flex-column fz14 grey-black-text">
                <div class="flex-center">
                  <el-icon class="mr5"><Clock /></el-icon>
                  <span
                    class="text-ellipsis cron-text"
                    :title="cronToZh(row.cronExpression)"
                  >
                    {{ cronToZh(row.cronExpression) || "-" }}
                  </span>
                </div>
              </div>
            </template>
            <template #lastExecute="{ row }">
              <div class="flex-column fz14 last-execute-col">
                <template v-if="row.lastExecuteTime">
                  <span>
                    {{ parseTime(row.lastExecuteTime, "{y}-{m}-{d} {h}:{i}") }}
                  </span>
                </template>
                <template v-else>
                  <div class="mb5">
                    <el-tag type="info" class="not-executed-tag">未执行</el-tag>
                  </div>
                  <span>-</span>
                </template>
              </div>
            </template>
            <template #createBy="{ row }">
              <div class="flex-column fz14">
                <span
                  class="text-ellipsis person-charge-ellipsis"
                  :title="row.createBy"
                >{{ row.createBy || "-" }}</span>
                <span>{{ row.createPhoneNumber || "-" }}</span>
              </div>
            </template>
            <template #action="{ row }">
              <div class="task-actions-col">
                <div class="action-row">
                  <el-button
                    link
                    type="primary"
                    icon="Edit"
                    :disabled="row.status == '1'"
                    @click="handleEditClick(row)">修改</el-button>
                  <el-button
                    link
                    type="primary"
                    icon="view"
                    @click="handleDetailClick(row)">详情</el-button>
                  <el-button
                    link
                    type="danger"
                    icon="Delete"
                    :disabled="row.status == '1'"
                    @click="handleDeleteClick(row)">删除</el-button>
                </div>
                <div class="action-row">
                  <el-button
                    link
                    type="success"
                    icon="Upload"
                    :disabled="row.status == '1'"
                    :loading="publishingId === row.id"
                    @click="handlePublishClick(row)">发布</el-button>
                  <el-button
                    link
                    type="warning"
                    icon="Download"
                    :disabled="row.status != '1'"
                    :loading="unpublishingId === row.id"
                    @click="handleUnpublishClick(row)">卸载</el-button>
                  <el-button
                    link
                    type="primary"
                    icon="VideoPlay"
                    :disabled="row.status != '1'"
                    @click="handleRunClick(row)">执行一次</el-button>
                </div>
              </div>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <!-- 调度周期弹窗 -->
    <el-dialog
      title="Cron表达式生成器"
      v-model="cronDialog.open"
      :append-to="$refs['app-container']"
      destroy-on-close
    >
      <Crontab
        @hide="handleCloseCronClick"
        @fill="handleConfirmCronClick"
        :expression="cronDialog.data"
      />
    </el-dialog>

    <!-- 新增/修改弹窗 -->
    <el-dialog
      v-model="dialog.open"
      :title="dialog.title"
      width="1200"
      :loading="dialog.loading"
      @close="handleCancelClick"
    >
      <el-form
        :model="dialog.form"
        class="column-form"
        :rules="rules"
        ref="formRef"
        label-width="110px"
      >
        <el-form-item label="来源系统" prop="sourceSystemId">
          <el-tree-select
            filterable
            v-model="dialog.form.sourceSystemId"
            :data="store.sourceSystems"
            :props="{ value: 'id', label: 'name', children: 'children' }"
            value-key="id"
            placeholder="请选择来源系统"
            check-strictly
            @change="handleDomainChange"
            default-expand-all
          />
        </el-form-item>

        <el-form-item label="任务名称" prop="name">
          <el-input v-model="dialog.form.name" placeholder="请输入任务名称" />
        </el-form-item>

        <el-form-item label="数据连接名称" prop="datasourceId">
          <el-select
            v-model="dialog.form.datasourceId"
            placeholder="请选择数据连接名称"
            @change="handleDatasourceChange"
          >
            <el-option
              v-for="item in store.datasources"
              :key="item.id"
              :label="item.datasourceName"
              :value="item.id"
              :disabled="
                !COLLECT_DATASOURCE_TYPES.includes(item.datasourceType)
              "
            >
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="数据库类型" prop="dbType">
          <el-input
            v-model="dialog.form.dbType"
            disabled
            placeholder="请输入数据库类型"
          />
        </el-form-item>

        <el-form-item label="IP" prop="ip">
          <el-input v-model="dialog.form.ip" disabled placeholder="请输入ip" />
        </el-form-item>

        <el-form-item label="端口号" prop="port">
          <el-input
            v-model="dialog.form.port"
            disabled
            placeholder="请输入端口号"
          />
        </el-form-item>

        <el-form-item label="账号" prop="username">
          <el-input
            v-model="dialog.form.username"
            disabled
            placeholder="请输入账号"
          />
        </el-form-item>

        <qt-form-item
          label="调度周期"
          prop="cronExpression"
          :tip="{
            content: '支持Cron表达式，如 0 0 * * * 表示每天0点执行，不填则不启用定时调度',
          }"
        >
          <el-input
            v-model="dialog.form.cronExpression"
            placeholder="请配置调度周期"
          >
            <template #append>
              <el-button
                type="primary"
                @click="handleOpenCronClick"
                style="background-color: #2666fb; color: #fff"
              >
                配置
                <i class="el-icon-time el-icon--right"></i>
              </el-button>
            </template>
          </el-input>
        </qt-form-item>

        <el-form-item
          label="采集模式"
          class="row-full"
          prop="collectionMode"
          v-if="false"
        >
          <el-radio-group v-model="dialog.form.collectionMode">
            <el-radio
              v-for="dict in toValue(dicts.mc_collect_mode)"
              :key="dict.value"
              :label="dict.value"
            >
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="采集范围" class="row-full" prop="collectionScope">
          <div class="collection-wrap">
            <el-radio-group v-model="dialog.form.collectionScope">
              <el-radio
                v-for="dict in toValue(dicts.mc_collect_scope)"
                :key="dict.value"
                :label="dict.value"
              >
                {{ dict.label }}
              </el-radio>
            </el-radio-group>

            <el-form-item
              prop="tables"
              v-if="dialog.form.collectionScope == 1"
              label-width="0"
              style="margin-bottom: 0"
            >
              <el-transfer
                v-model="dialog.form.tables"
                :data="dialog.tableList"
                :props="{ label: 'label', key: 'dbName' }"
                filterable
                :filter-method="onFilterTransfer"
                filter-placeholder="请输入物理库名称"
                :titles="['来源库', '已选来源库']"
                style="--el-transfer-panel-width: 320px"
              />
            </el-form-item>
          </div>
        </el-form-item>

        <el-form-item label="描述" class="row-full" prop="description">
          <el-input
            v-model="dialog.form.description"
            type="textarea"
            placeholder="请输入描述"
            :min-height="192"
            show-word-limit
            maxlength="500个字符"
          />
        </el-form-item>

        <el-form-item label="备注" class="row-full" prop="remark">
          <el-input
            v-model="dialog.form.remark"
            type="textarea"
            placeholder="请输入备注"
            :min-height="192"
            show-word-limit
            maxlength="500个字符"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelClick">取消</el-button>
          <el-button type="primary" @click="handleConfirmClick">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="McTaskStructured">
import { useRouter } from "vue-router";
import {
  reactive,
  ref,
  getCurrentInstance,
  toValue,
  computed,
  watch,
} from "vue";
import Crontab from "@/components/Crontab/index.vue";
import SourceSystemTree from "./components/SourceSystemTree.vue";
import { getParentLabelPath } from "@/utils/anivia.js";
import { cronToZh } from "@/utils/cronUtils";
import { getDatasourceIcon } from "@/utils/datasource";
import {
  listTask,
  addTask,
  getTask,
  updateTask,
  delTask,
  getRealtimeMcTaskScopeList,
  updateReleaseJobTask,
  updateReleaseSchedule,
  runJobOnce,
  sourceSystemTree,
  batchDeleteCheck,
} from "@/api/cat/task/task";
import { listDaDatasource } from "@/api/cat/dataSource/dataSource";
import useUserStore from "@/store/system/user";
import { listValidSourceSystem } from "@/api/tax/sourceSystem/sourceSystem";

// 表单验证规则
const rules = {
  sourceSystemId: [
    { required: true, message: "请选择来源系统", trigger: "change" },
  ],
  name: [
    { required: true, message: "请输入任务名称", trigger: "blur" },
    { min: 2, max: 20, message: "长度在 2 到 20 个字符", trigger: "blur" },
  ],
  datasourceId: [
    { required: true, message: "请选择数据连接名称", trigger: "change" },
  ],
  dbType: [
    {
      required: true,
      message: "请输入数据库类型",
      trigger: ["blur", "change"],
    },
  ],
  ip: [
    { required: true, message: "请输入数据库ip", trigger: ["blur", "change"] },
  ],
  port: [
    {
      required: true,
      message: "请输入数据库端口",
      trigger: ["blur", "change"],
    },
  ],
  username: [
    {
      required: true,
      message: "请输入数据库用户名",
      trigger: ["blur", "change"],
    },
  ],
  cronExpression: [],
  collectionMode: [
    { required: true, message: "请选择采集模式", trigger: "change" },
  ],
  collectionScope: [
    { required: true, message: "请选择采集范围", trigger: "change" },
  ],
  tables: [
    {
      required: true,
      validator: (rule, value, callback) => {
        if (
          dialog.form.collectionScope == "1" &&
          (!value || value.length === 0)
        ) {
          callback(new Error("请选择已选来源库"));
        } else {
          callback();
        }
      },
      trigger: "change",
    },
  ],
};

const DETAIL_PATH = "/cat/meta/task/detail";

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "datasource_type",
  "mc_collect_scope",
  "mc_collect_mode"
);

const router = useRouter();
const userStore = useUserStore();

const formRef = ref();
const sourceSystemTreeRef = ref();
const store = reactive({
  loading: false,
  rows: [],
  domains: [],
  treeDomains: [],
  sourceSystems: [],
  flatSourceSystems: [],
  datasources: [],
});

function getAllSourceSystems() {
  return listValidSourceSystem().then((res) => {
    store.sourceSystems = res.data || [];
    // 扁平化数据用于查找
    const flatten = (list) => {
      if (!Array.isArray(list)) return [];
      let result = [];
      list.forEach((item) => {
        result.push(item);
        if (item.children && item.children.length > 0) {
          result = result.concat(flatten(item.children));
        }
      });
      return result;
    };
    store.flatSourceSystems = flatten(store.sourceSystems);
  });
}

function handleTreeDataLoaded({ treeData, flatData }) {
  store.treeDomains = treeData;
  store.domains = flatData;
}

// 节点单击事件
function handleNodeClick(data) {
  // 清除之前的筛选
  tableStore.params.sourceSystemId = undefined;
  tableStore.params.datasourceId = undefined;
  tableStore.params.id = undefined;
  tableStore.params.type = undefined;

  if (data.type === "SOURCE") {
    tableStore.params.sourceSystemId = data.id;
  } else if (data.type === "DATASOURCE") {
    tableStore.params.datasourceId = data.id;
    tableStore.params.type = data.type;
  } else if (data.type === "DATABASE") {
    tableStore.params.id = data.taskId;
  }
  tableRef.value.getList();
}

// 列表
const tableRef = ref(null);
const tableStore = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: { prop: "create_time", order: "descending" },
      onSelectionChange: function (rows) {
        store.rows = rows;
      },
      onRowDblclick: handleDetailClick,
    },
  },
  columns: [
    { type: "selection", width: 55 },
    { label: "编号", prop: "id", sortable: true, width: 90 },
    { label: "任务信息", prop: "name", align: "left", slot: "name", width: 280 },
    { label: "运行控制", prop: "status", width: 145, slot: "releaseState", align: "left" },
    { label: "调度周期", prop: "cronExpression", width: 160, slot: "cronExpression", align: "left" },
    { label: "最近执行", width: 160, slot: "lastExecute", align: "left" },
    { label: "创建人", slot: "createBy", width: 120, align: "left" },
    { label: "创建时间", prop: "createTime", sortable: true, sortableKey: "create_time", date: true, width: 150, align: "left" },
    { label: "操作", align: "center", fixed: "right", slot: "action", width: 260 },
  ],
  func: listTask,
  params: {},
  events: {
    formatParams(params) {
      if (!params.time || !params.time.length) return params;
      const { time, ...other } = { ...params };
      other.createTimeStart = time[0];
      other.createTimeEnd = time[1];
      return other;
    },
  },
});

// 搜索项
const searchStore = reactive({
  items: [
    {
      label: "任务名称",
      prop: "name",
      component: { is: "input", placeholder: "请输入任务名称" },
    },
    {
      label: "任务状态",
      prop: "status",
      component: {
        is: "select",
        placeholder: "请选择任务状态",
        options: [
          { value: "0", label: "未发布" },
          { value: "1", label: "已发布" },
        ],
      },
    },
    {
      label: "创建时间",
      prop: "time",
      style: { width: "320px" },
      component: {
        is: "date-picker",
        type: "daterange",
        startPlaceholder: "开始日期",
        endPlaceholder: "结束日期",
      },
    },
  ],
});

// 新增/修改弹窗
const DEFAULT_FORM = {
  collectionMode: "1",
  collectionScope: "2",
  tables: [],
};
const dialog = reactive({
  open: false,
  title: "",
  loading: false,
  tableList: [],
  form: {
    ...DEFAULT_FORM,
  },
});

const COLLECT_DATASOURCE_TYPES = [
  "DM",
  "DM8",
  "MySql",
  "MYSQL",
  "Oracle11",
  "Oracle",
  "ORACLE11",
  "ORACLE",
  "PostgreSQL",
  "POSTGRESQL",
  "Hive",
  "HIVE",
  "Kingbase8",
  "KINGBASE8",
];

// 调度周期弹窗
const cronDialog = reactive({
  open: false,
  data: "",
});

// 获取来源系统路径
const getDomainPath = computed(() => {
  return function (id) {
    let domainName = getParentLabelPath(store.sourceSystems, id, {
      idKey: "id",
      labelKey: "name",
      childrenKey: "children",
    });
    const idx = domainName.indexOf("/");
    return idx == -1 ? domainName : domainName.slice(idx + 1);
  };
});

// 获取数据源列表
function getDatasources() {
  return listDaDatasource().then((res) => {
    const rows = res.data?.rows || [];
    rows.forEach((item) => {
      item.datasourceConfig = parseDatasourceConfig(item.datasourceConfig);
    });
    store.datasources = rows;
  });
}

function parseDatasourceConfig(config) {
  if (!config) return {};
  if (typeof config === "object") return config;
  try {
    return JSON.parse(config);
  } catch (e) {
    try {
      return JSON.parse(config.replace(/\\"/g, '"'));
    } catch (err) {
      console.error("数据源配置解析失败", err);
      return {};
    }
  }
}

// 搜索按钮操作
function handleQueryClick() {
  tableRef.value?.getList();
}

// 重置按钮操作
function handleResetQueryClick() {
  if (sourceSystemTreeRef.value?.resetTree) {
    sourceSystemTreeRef.value.resetTree();
  }
  tableStore.params.sourceSystemId = null;
  tableStore.params.datasourceId = null;
  tableStore.params.id = null;
  tableStore.params.status = null;
  tableStore.params.createBy = null;
  tableRef.value?.resetQuery();
}

// 切换数据源
function handleDatasourceChange(id, falg = true) {
  const data = store.datasources.find((item) => item.id === id);
  if (!data) return;
  dialog.form.ip = data.ip;
  dialog.form.port = data.port;
  dialog.form.username = data.datasourceConfig?.username;
  dialog.form.dbType = data.datasourceType;
  if (falg) {
    dialog.form.tables = [];
  }
  getRealtimeMcTaskScopeList(id).then((res) => {
    dialog.tableList = res.data.map((item) => ({
      ...item,
      label: item.schemaName
        ? `${item.dbName}.${item.schemaName}`
        : item.dbName,
    }));
  });
}

// 切换来源系统
function handleDomainChange(id) {
  const data = store.flatSourceSystems.find((item) => item.id === id);
  if (data) {
    dialog.form.sourceSystemId = data.id;
    dialog.form.sourceSystemName = data.name;
  }
}
function handleRunClick(val) {
  runJobOnce({ id: val.id }).then((res) => {
    if (res.code == 200) {
      ElMessage.success("执行成功");
    } else {
    }
  });
}
// 打开调度周期弹窗
function handleOpenCronClick() {
  cronDialog.data = dialog.form.cronExpression;
  cronDialog.open = true;
}

// 关闭调度周期弹窗
function handleCloseCronClick() {
  cronDialog.open = false;
  cronDialog.data = "";
}

// 确认调度周期弹窗
function handleConfirmCronClick(data) {
  dialog.form.cronExpression = data;
  cronDialog.open = false;
}

// 点击详情
function handleDetailClick(row) {
  router.push({
    path: DETAIL_PATH,
    query: {
      id: row.id,
    },
  });
}

// 点击新增
function handleAddClick() {
  dialog.title = "新增采集任务";
  dialog.open = true;
  dialog.func = addTask;
  getAllSourceSystems();
  getDatasources();
}

function applyCurrentUserAsCreator(target) {
  target.creatorId = userStore.id;
  target.createBy = userStore.nickName || userStore.name;
  target.leader = userStore.id;
  target.leaderPhone = userStore.phonenumber || "";
}

// 取消新增/修改
function handleCancelClick() {
  formRef.value.resetFields();
  dialog.form = {
    ...DEFAULT_FORM,
  };
  dialog.tableList = [];
  dialog.loading = false;
  dialog.open = false;
}

// 确认新增/修改
async function handleConfirmClick() {
  const valid = await formRef.value.validate();
  if (!valid) return;
  dialog.loading = true;
  const { tables, ...params } = dialog.form;
  applyCurrentUserAsCreator(params);
  if (params.collectionScope == "1") {
    params.scopeSaveReqVOS = dialog.tableList.filter((item) =>
      tables.includes(item.dbName)
    );
  }
  try {
    await dialog.func(params);
    sourceSystemTreeRef.value?.getTreeData?.();
    handleCancelClick();
    tableRef.value.getList();
  } catch (err) {
    console.error(err);
  } finally {
    dialog.loading = false;
  }
}

// 打开修改弹窗
function handleEditClick(row) {
  dialog.open = true;
  dialog.func = updateTask;
  dialog.title = "修改任务";
  getTask(row.id).then((res) => {
    if (res.data.scopeSaveReqVOS) {
      res.data.tables = res.data.scopeSaveReqVOS.map((item) => item.dbName);
    }
    // 确保回显时包含来源系统名称
    if (res.data.sourceSystemId && !res.data.sourceSystemName) {
      const system = store.flatSourceSystems.find(
        (item) => item.id === res.data.sourceSystemId
      );
      if (system) {
        res.data.sourceSystemName = system.name;
      }
    }
    dialog.form = res.data;
    handleDatasourceChange(res.data.datasourceId, false);
  });
}

// 删除
function handleDeleteClick(row) {
  ElMessageBox.confirm(`是否确认删除编号为${row.id}的数据项？`, "系统提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      return delTask(row.id);
    })
    .then(() => {
      ElMessage.success("删除成功");
      tableRef.value.getList();
    });
}

// 删除选中行
function handleDeleteColumnClick() {
  if (!store.rows.length) return;
  const ids = store.rows.map((item) => item.id);
  store.loading = true;
  batchDeleteCheck(ids).then((res) => {
    const { canDeleteCount, cannotDeleteCount, canDeleteIds } = res.data;
    store.loading = false;
    ElMessageBox.confirm(
      `可删除${canDeleteCount}个，不可删除${cannotDeleteCount}个，是否删除可删部分`,
      "系统提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    )
      .then(() => {
        if (!canDeleteIds.length) {
          ElMessage.success("删除成功");
          return;
        }
        return delTask(canDeleteIds);
      })
      .then((res) => {
        if (!res) return;
        ElMessage.success("删除成功");
        tableRef.value.getList();
      });
  });
}

// 筛选表
function onFilterTransfer(value, item) {
  if (!value) return item;
  const txt = (item.label || item.dbName || "").toLowerCase();
  return txt.includes(value.toLowerCase());
}

const publishingId = ref(null);
const unpublishingId = ref(null);

function handlePublishClick(row) {
  proxy.$modal.confirm(`确认发布【${row.name}】到 DolphinScheduler 吗？`).then(async () => {
    publishingId.value = row.id;
    await updateReleaseJobTask({ id: row.id, status: "1" });
  }).then(() => {
    proxy.$modal.msgSuccess("发布成功");
    tableRef.value.getList();
  }).catch((error) => {
    if (error === "cancel" || error === "close") return;
    proxy.$modal.msgError(error?.message || error?.msg || "发布失败");
  }).finally(() => {
    publishingId.value = null;
  });
}

function handleUnpublishClick(row) {
  proxy.$modal.confirm(`确认从 DolphinScheduler 卸载【${row.name}】吗？卸载后任务可编辑和删除。`).then(async () => {
    unpublishingId.value = row.id;
    await updateReleaseJobTask({ id: row.id, status: "0" });
  }).then(() => {
    proxy.$modal.msgSuccess("卸载成功");
    tableRef.value.getList();
  }).catch((error) => {
    if (error === "cancel" || error === "close") return;
    proxy.$modal.msgError(error?.message || error?.msg || "卸载失败");
  }).finally(() => {
    unpublishingId.value = null;
  });
}

getDatasources();
getAllSourceSystems();
</script>

<style lang="scss" src="@/assets/system/styles/table-style-optimized.scss"></style>
<style lang="scss" scoped>
.task-actions-col {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
  .action-row {
    display: flex;
    justify-content: flex-start;
    gap: 0;
    .el-button {
      font-size: 12px;
      padding: 0 2px;
    }
  }
}
</style>

