<template>
  <div class="app-container dpp-task-list-page" ref="app-container">

    <a-layout>
      <DeptTree
        :api="api"
        :editable="true"
        :leftWidth="leftWidth"
        :placeholder="'请输入数据集成目录名称'"
        ref="DeptTreeRef"
        title="数据集成目录"
        @node-click="handleNodeClick"
        :extraParams="{
          spaceCode: userStore.spaceCode,
          spaceId: userStore.spaceId,
        }"
      />
      <a-layout-content class="main-content">
        <dm-wrap :columns="tableStore.columns" :tableRef="tableRef">
          <template #search>
            <dm-search-bar
              v-bind="searchStore"
              :params="tableStore.params"
              @query="handleQuery"
              @reset="resetQuery"
            />
          </template>
          <template #actions-data>
            <a-button type="primary" @click="openTaskConfigDialog">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
          </template>

          <dm-table v-bind="tableStore" ref="tableRef">
            <template #name="{ row }">
              <div class="name-label task-title">
                <div
                  class="justify task-title-row"
                  @click="
                    routeTo('/col/task/integratioTask/detail', {
                      ...row,
                      info: true,
                    })
                  "
                >
                  <img
                    :src="getDatasourceIcon(row.draftJson)"
                    alt=""
                    class="datasource-icon"
                    v-if="getDatasourceIcon(row.draftJson)"
                  />
                  <a-typography-link
                    :underline="false"
                    class="task-name-text task-name-ellipsis"
                    :title="row.name"
                  >
                    {{ row.name }}
                  </a-typography-link>
                  <a-tag
                    color="blue"
                    class="task-cat-ellipsis"
                    :title="row.catName"
                  >
                    {{ row.catName }}
                  </a-tag>
                </div>
                <div class="text-ellipsis desc-text" :title="row.description">
                  {{ row.description }}
                </div>
              </div>
            </template>
            <template #releaseState="{ row }">
              <div class="flex-column" style="gap: 4px">
                <div class="flex-center">
                  <span class="black-label mr5">发布状态:</span>
                  <a-tag
                    v-if="row.status !== null && row.status !== undefined && row.status !== ''"
                    :color="row.status == '1' ? 'success' : row.status == '-1' ? 'default' : 'warning'"
                  >
                    {{ row.status == "1" ? "已发布" : row.status == "-1" ? "草稿" : "未发布" }}
                  </a-tag>
                </div>
                <div class="flex-center">
                  <span class="black-label mr5">调度状态:</span>
                  <a-tag
                    v-if="row.schedulerState !== null && row.schedulerState !== undefined && row.schedulerState !== ''"
                    :color="row.schedulerState == '1' ? 'success' : 'default'"
                  >
                    {{ row.schedulerState == "1" ? "已上线" : "未上线" }}
                  </a-tag>
                </div>
              </div>
            </template>
            <template #cronExpression="{ row }">
              <div class="flex-column fz14 grey-black-text">
                <div class="cron-row">
                  <ClockCircleOutlined class="cron-icon" />
                  <span
                    class="text-ellipsis cron-text"
                    :title="cronToZh(row.cronExpression)"
                  >
                    {{ cronToZh(row.cronExpression) }}
                  </span>
                </div>
              </div>
            </template>
            <template #lastExecute="{ row }">
              <div class="flex-column fz14 last-execute-col">
                <template v-if="row.lastExecuteTime">
                  <a-tag
                    v-if="
                      row.lastExecuteStatus !== null &&
                      row.lastExecuteStatus !== undefined &&
                      row.lastExecuteStatus !== ''
                    "
                    :color="taskInstanceStatusType(row.lastExecuteStatus)"
                  >
                    {{ taskInstanceStatusLabel(row.lastExecuteStatus) }}
                  </a-tag>
                  <span>
                    {{ parseTime(row.lastExecuteTime, "{y}-{m}-{d} {h}:{i}") }}
                  </span>
                </template>
              </div>
            </template>
            <template #createBy="{ row }">
              <div class="flex-column fz14">
                <span
                  class="text-ellipsis person-charge-ellipsis"
                  :title="row.createBy"
                  >{{ row.createBy }}</span
                >
              </div>
            </template>
            <template #executionType="{ row }">
              <dict-tag
                :options="col_etl_task_execution_type"
                :value="row.executionType"
              />
            </template>
            <template #action="{ row }">
              <div class="task-actions">
                <a-button
                  type="link"
                  :icon="h(EditOutlined)"
                  :disabled="isPublished(row)"
                  @click="routeTo('/col/task/integratioTask/edit', row)">配置任务</a-button
                >
                <a-button
                  type="link"
                  :icon="h(EyeOutlined)"
                  @click="
                    routeTo('/col/task/integratioTask/detail', {
                      ...row,
                      info: true,
                    })
                  ">详情</a-button
                >
                <a-button
                  type="link"
                  :icon="h(UploadOutlined)"
                  :disabled="isPublished(row)"
                  :loading="publishingTaskId === row.id"
                  @click="handlePublish(row)">发布</a-button
                >
                <a-button
                  type="link"
                  :icon="h(DownloadOutlined)"
                  :disabled="!isPublished(row)"
                  :loading="unpublishingTaskId === row.id"
                  @click="handleUnpublish(row)">卸载</a-button
                >
                <a-popover placement="bottom" :width="150" trigger="click">
                  <template #content>
                  <div style="width: 100px" class="butgdlist">
                    <a-button
                      type="link"
                      style="padding-left: 14px"
                      :icon="h(ControlOutlined)"
                      @click="handleJobLog(row)"
                      :disabled="row.schedulerState == '1'">调度周期</a-button
                    >
                    <a-button
                      type="link"
                      :icon="h(FieldTimeOutlined)"
                      @click="handleDataView(row)">运行实例</a-button
                    >
                    <a-button
                      type="link"
                      :icon="h(PlayCircleOutlined)"
                      :disabled="!isPublished(row)"
                      @click="handleExecuteOnce(row)">执行一次</a-button
                    >
                    <a-button
                      type="link"
                      :icon="h(MonitorOutlined)"
                      @click="handleOpsPolicy(row)">运维托管</a-button
                    >
                    <a-button
                      type="link"
                      danger
                      :icon="h(DeleteOutlined)"
                      :disabled="isPublished(row)"
                      @click="handleDelete(row)">删除</a-button
                    >
                    <a-button
                      type="link"
                      :icon="h(CopyOutlined)"
                      :disabled="isPublished(row)"
                      @click="handleClone(row)">克隆</a-button>
                  </div>
                  </template>
                  <a-button type="link" :icon="h(DownOutlined)">更多</a-button>
                </a-popover>
              </div>
            </template>
          </dm-table>
        </dm-wrap>
      </a-layout-content>
    </a-layout>
    <instance
      :visible="DataView"
      :taskType="1"
      @update:visible="DataView = $event"
      @confirm="submitForm"
      :data="form"
      title="运行实例"
    />
    <a-modal
      title="调度周期"
      v-model:open="openCron"
      :footer="null"
      destroy-on-close
      :width="700"
    >
      <crontab
        ref="crontabRef"
        @hide="openCron = false"
        @fill="crontabFill"
        :expression="expression"
      >
      </crontab>
    </a-modal>
    <!-- 新增 -->
    <add
      :visible="taskConfigDialogVisible"
      title="新增任务"
      @update:visible="taskConfigDialogVisible = $event"
      @save="handleSave"
      @confirm="handleConfirm"
      @回echo完成="handle回echo完成"
      :data="nodeData"
      :info="route.query.info"
      :catCode="tableStore.params.catCode"
      :deptOptions="deptOptions"
      :savedDataSourceId="回echo数据.dataSourceId"
      :savedAssetTableId="回echo数据.assetTableId"
      :savedDataSourceName="回echo数据.dataSourceName"
      :savedDataSourceType="回echo数据.dataSourceType"
    />
    <a-modal
      title="运维托管"
      v-model:open="opsDialogVisible"
      width="560px"
      destroy-on-close
    >
      <a-form :model="opsForm" :label-col="{ style: { width: '120px' } }">
        <a-form-item label="任务名称">
          <span>{{ opsTask.name }}</span>
        </a-form-item>
        <a-form-item label="失败即停">
          <a-switch v-model:checked="opsForm.failStopEnabled" />
        </a-form-item>
        <a-form-item label="AI托管">
          <a-switch v-model:checked="opsForm.aiManaged" />
        </a-form-item>
        <a-form-item label="自动恢复">
          <a-switch v-model:checked="opsForm.autoRecoverEnabled" :disabled="!opsForm.aiManaged" />
        </a-form-item>
        <a-form-item label="恢复次数">
          <a-input-number
            v-model:value="opsForm.maxRecoverTimes"
            :min="0"
            :max="10"
            :disabled="!opsForm.autoRecoverEnabled"
          />
        </a-form-item>
        <a-form-item label="恢复策略">
          <a-select v-model:value="opsForm.recoverStrategy">
            <a-select-option label="安全自动恢复" value="SAFE_AUTO" />
            <a-select-option label="只给建议" value="SUGGEST_ONLY" />
          </a-select>
        </a-form-item>
        <a-form-item label="通知用户">
          <a-input v-model:value="opsForm.notifyUsers" placeholder="多个用户用逗号分隔" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="opsDialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="opsSaving" @click="saveOpsPolicy">保存</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="DppIntegratioTask">
import {
  listDppEtlTask,
  delDppEtlTask,
  addDppEtlTask,
  updateDppEtlTask,
  updateReleaseSchedule,
  updateReleaseJobTask,
  publishDppEtlTask,
  unpublishDppEtlTask,
  releaseTaskCrontab,
  startDppEtlTask,
  createEtlTaskFront,
  copyCreateEtl,
} from "@/api/col/task/index.js";
import { getTaskOpsPolicy, saveTaskOpsPolicy } from "@/api/col/taskOps.js";
import { usePageRefresh } from "@/composables/usePageRefresh";
import { cronToZh } from "@/utils/cronUtils";
import Crontab from "@/components/Crontab/index.vue";
import instance from "@/views/col/components/instance.vue";
const userStore = useUserStore();
import { useRoute, useRouter } from "vue-router";
import useUserStore from "@/store/system/user";
import {
  listAttTaskCat,
  getAttTaskCat,
  addAttTaskCat,
  updateAttTaskCat,
  delAttTaskCat,
} from "@/api/tax/cat/taskCat/taskCat";
import DeptTree from "@/components/DeptTree";
import add from "./add/add.vue";
import {
  ref,
  reactive,
  h,
  getCurrentInstance,
  watch,
  onBeforeUnmount,
} from "vue";
import {
  ClockCircleOutlined,
  EditOutlined,
  EyeOutlined,
  UploadOutlined,
  DownloadOutlined,
  DownOutlined,
  ControlOutlined,
  FieldTimeOutlined,
  PlayCircleOutlined,
  MonitorOutlined,
  DeleteOutlined,
  CopyOutlined,
} from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();

const api = {
  list: listAttTaskCat,
  get: getAttTaskCat,
  add: addAttTaskCat,
  update: updateAttTaskCat,
  del: delAttTaskCat,
};
const {
  dpp_etl_task_status,
  col_etl_task_execution_type,
} = proxy.useDict(
  "dpp_etl_task_status",
  "col_etl_task_execution_type"
);

const route = useRoute();
const router = useRouter();

// Table and Search Store
const tableRef = ref(null);
const tableStore = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: { prop: "create_time", order: "descending" },
    },
  },
  columns: [
    { label: "编号", prop: "id", width: 110, sortable: true },
    {
      label: "任务信息",
      prop: "name",
      align: "left",
      slot: "name",
      width: 280,
    },
    {
      label: "运行控制",
      prop: "status",
      width: 200,
      slot: "releaseState",
      align: "left",
    },
    {
      label: "调度周期",
      prop: "cronExpression",
      width: 210,
      slot: "cronExpression",
      align: "left",
    },
    {
      label: "最近执行",
      width: 200,
      slot: "lastExecute",
      align: "left",
    },

    {
      label: "创建人",
      slot: "createBy",
      width: 120,
      align: "left",
      showOverflowTooltip: true,
    },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      sortableKey: "create_time",
      date: true,
      width: 150,
      align: "left",
    },

    {
      label: "操作",
      align: "center",
      fixed: "right",
      slot: "action",
      width: 270,
    },
  ],
  func: listWrapper,
  params: {
    catCode: null,
    spaceId: userStore.spaceId,
    spaceCode: userStore.spaceCode,
  },
});

let deptOptions = ref([]);
function getDeptTree() {
  listAttTaskCat({
    spaceId: userStore.spaceId,
    spaceCode: userStore.spaceCode,
    validFlag: true,
  }).then((response) => {
    deptOptions.value = [];
    var children = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "数据集成目录",
        value: "",
        code: "0",
        id: 0,
        disabled: true,
        children: children,
      },
    ];
  });
}

const searchStore = reactive({
  items: [
    {
      label: "名称",
      prop: "name",
      align: "left",
      component: { is: "input", placeholder: "请输入任务名称" },
    },
    {
      label: "状态",
      prop: "status",
      component: {
        is: "select",
        placeholder: "请选择任务状态",
        options: dpp_etl_task_status,
      },
    },
  ],
});

function listWrapper(params) {
  const p = { ...params };
  p.spaceId = userStore.spaceId;
  p.spaceCode = userStore.spaceCode;
  return listDppEtlTask(p);
}

function handleQuery() {
  tableRef.value.getList();
}

function resetQuery() {
  if (DeptTreeRef.value?.resetTree) {
    DeptTreeRef.value.resetTree();
  }
  tableStore.params.catCode = "";
  handleQuery();
}

// 部门树
const leftWidth = ref(240);
const DeptTreeRef = ref(null);
function handleNodeClick(data) {
  tableStore.params.catCode = data.code;
  handleQuery();
}
// 任务配置
const taskConfigDialogVisible = ref(false);
const publishingTaskId = ref(null);
const unpublishingTaskId = ref(null);
const opsDialogVisible = ref(false);
const opsSaving = ref(false);
const opsTask = ref({});
const opsForm = ref({
  taskId: null,
  failStopEnabled: false,
  aiManaged: false,
  autoRecoverEnabled: false,
  maxRecoverTimes: 1,
  recoverStrategy: "SAFE_AUTO",
  notifyUsers: "",
});
let nodeData = ref({ taskConfig: {}, name: null });
let 回echo数据 = ref({});

const openTaskConfigDialog = () => {
  nodeData.value = { taskConfig: {}, name: null };
  taskConfigDialogVisible.value = true;
  
  // 检查是否有回echo数据
  if (回echo数据.value.dataSourceId || 回echo数据.value.assetTableId) {
    // 从回echo数据中构建任务配置
    nodeData.value.taskConfig = {
      readerDatasource: {
        datasourceId: 回echo数据.value.dataSourceId,
        datasourceName: 回echo数据.value.dataSourceName,
        datasourceType: 回echo数据.value.dataSourceType
      }
    };
    
    if (回echo数据.value.assetTableId) {
      nodeData.value.taskConfig.asset_id_cpoy = 回echo数据.value.assetTableId;
    }
  }
};

// 监听子组件回echo完成
const handle回echo完成 = (回echoData) => {
  回echo数据.value = 回echoData;
  console.log('父组件收到回echo完成:', 回echoData);
};

const handleSave = (form) => {
  console.log("🚀 handleSave called once, form:", form.name);
  const parms = {
    ...form,
    spaceId: userStore.spaceId,
    spaceCode: userStore.spaceCode,
    draftJson: JSON.stringify({ ...form }),
  };
  createEtlTaskFront(parms).then((res) => {
    if (res.code == 200) {
      console.log("🚀 save success");
      proxy.$modal.msgSuccess("操作成功");
      handleQuery();
      taskConfigDialogVisible.value = false;
    }
  }).finally(() => {
    taskConfigDialogVisible.value = false;
  });
};

const handleConfirm = (form) => {
  const parms = {
    ...form,
    spaceId: userStore.spaceId,
    spaceCode: userStore.spaceCode,
    draftJson: JSON.stringify({ ...form }),
  };
  createEtlTaskFront(parms).then((res) => {
    if (res.code == 200) {
      proxy.$modal.msgSuccess("操作成功");
      handleQuery();
      routeTo("/col/task/integratioTask/edit", res.data);
    }
  }).finally(() => {
    taskConfigDialogVisible.value = false;
  });
};

// Actions
function handleStatusChange(id, row) {
  const text = row.status == "1" ? "上线" : "下线";
  proxy.$modal
    .confirm('确认要"' + text + '","' + row.name + '"数据集成任务吗？')
    .then(function () {
      updateReleaseJobTask({
        id,
        releaseState: row.status,
        spaceCode: userStore.spaceCode,
        spaceId: userStore.spaceId,
      })
        .then((response) => {
          proxy.$modal.msgSuccess("操作成功");
          handleQuery();
        })
        .catch((error) => {
          row.status = row.status === "1" ? "0" : "1";
        });
    })
    .catch((error) => {
      row.status = row.status === "1" ? "0" : "1";
    });
}

function handleschedulerState(id, row) {
  const text = row.schedulerState == "1" ? "上线" : "下线";
  proxy.$modal
    .confirm('确认要"' + text + '","' + row.name + '"数据集成调度状态吗？')
    .then(function () {
      updateReleaseSchedule({
        id,
        schedulerState: row.schedulerState,
        spaceCode: userStore.spaceCode,
        spaceId: userStore.spaceId,
      })
        .then((response) => {
          proxy.$modal.msgSuccess("操作成功");
          handleQuery();
        })
        .catch((error) => {
          row.schedulerState = row.schedulerState == "1" ? "0" : "1";
        });
    })
    .catch((error) => {
      row.schedulerState = row.schedulerState == "1" ? "0" : "1";
    });
}

async function handlePublish(row) {
  if (publishingTaskId.value) {
    return;
  }
  proxy.$modal
    .confirm(`确认发布【${row.name}】到 DolphinScheduler 吗？`)
    .then(async () => {
      publishingTaskId.value = row.id;
      await publishDppEtlTask({
        id: row.id,
        type: row.type || "1",
        spaceCode: userStore.spaceCode,
        spaceId: userStore.spaceId,
      });
    })
    .then(() => {
      proxy.$modal.msgSuccess("发布成功");
      handleQuery();
    })
    .catch((error) => {
      if (error === "cancel" || error === "close") {
        return;
      }
      proxy.$modal.msgError(error?.message || error?.msg || "发布失败");
    })
    .finally(() => {
      publishingTaskId.value = null;
    });
}

async function handleUnpublish(row) {
  if (unpublishingTaskId.value) {
    return;
  }
  proxy.$modal
    .confirm(`确认从 DolphinScheduler 卸载【${row.name}】吗？卸载后任务可编辑和删除。`)
    .then(async () => {
      unpublishingTaskId.value = row.id;
      await unpublishDppEtlTask({
        id: row.id,
        type: row.type || "1",
        spaceCode: userStore.spaceCode,
        spaceId: userStore.spaceId,
      });
    })
    .then(() => {
      proxy.$modal.msgSuccess("卸载成功");
      handleQuery();
    })
    .catch((error) => {
      if (error === "cancel" || error === "close") {
        return;
      }
      proxy.$modal.msgError(error?.message || error?.msg || "卸载失败");
    })
    .finally(() => {
      unpublishingTaskId.value = null;
    });
}

function routeTo(link, row) {
  if (link !== "" && link.indexOf("http") !== -1) {
    window.location.href = link;
    return;
  }
  if (link !== "") {
    if (link === router.currentRoute.value.path) {
      window.location.reload();
    } else {
      router.push({
        path: link,
        query: {
          id: row.id,
          info: row.info,
        },
      });
    }
  }
}

// Cron and Instance
const openCron = ref(false);
const expression = ref("");
const row = ref({});

function handleJobLog(data) {
  row.value = data || {};
  expression.value = data.cronExpression || "";
  openCron.value = true;
}

function crontabFill(value) {
  row.value.crontab = value;
  releaseTaskCrontab({
    crontab: row.value.crontab,
    spaceCode: userStore.spaceCode,
    spaceId: userStore.spaceId,
    id: row.value.id,
  }).then((response) => {
    proxy.$modal.msgSuccess("操作成功");
    handleQuery();
  });
}

const DataView = ref(false);
const form = ref({});
function handleDataView(row) {
  form.value = row;
  DataView.value = true;
}

function submitForm() {
  handleQuery();
}

const handleExecuteOnce = async (row) => {
  if (!row?.id) {
    proxy.$modal.msgWarning("无效的任务id，请刷新后重试");
    return;
  }
  try {
    const res = await startDppEtlTask(row.id);
    if (Number(res?.code) === 200) {
      proxy.$modal.msgSuccess("执行成功");
    } else {
      proxy.$modal.msgWarning(res?.msg || "执行失败，请联系管理员");
    }
  } catch (e) {
    //
  }
};

const handleClone = (row) => {
  proxy.$modal
    .confirm(`确定要克隆任务【${row.name}】吗？`)
    .then(() => {
      return copyCreateEtl({
        id: Number(row.id),
        spaceCode: userStore.spaceCode,
        spaceId: userStore.spaceId,
      });
    })
    .then(() => {
      handleQuery();
    })
    .catch(() => {});
};

function handleDelete(row) {
  const ids = row.id;
  proxy.$modal
    .confirm('是否确认删除数据集成任务编号为"' + ids + '"的数据项？')
    .then(function () {
      return delDppEtlTask(ids);
    })
    .then(() => {
      handleQuery();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

// Utils
const getDatasourceIcon = (json) => {
  try {
    let type = json && JSON.parse(json).taskType;
    switch (type) {
      case "FLINK":
        return new URL("@/assets/images/common/flink.svg", import.meta.url).href;
      default:
        return new URL("@/assets/images/common/flink.svg", import.meta.url).href;
    }
  } catch {
    return new URL("@/assets/images/common/flink.svg", import.meta.url).href;
  }
};

const getStatus = (status) => {
  if (status == "-1") {
    return "-1";
  } else {
    return "0";
  }
};

const taskInstanceStatusMap = {
  0: { label: "提交成功", type: "default" },
  1: { label: "运行中", type: "processing" },
  2: { label: "准备暂停", type: "warning" },
  3: { label: "暂停", type: "warning" },
  4: { label: "准备停止", type: "warning" },
  5: { label: "停止", type: "default" },
  6: { label: "失败", type: "error" },
  7: { label: "成功", type: "success" },
  8: { label: "需要容错", type: "warning" },
  9: { label: "已杀死", type: "error" },
  10: { label: "等待线程", type: "default" },
  11: { label: "等待依赖", type: "default" },
};

function taskInstanceStatusLabel(status) {
  return taskInstanceStatusMap[String(status)]?.label || status || "-";
}

function taskInstanceStatusType(status) {
  return taskInstanceStatusMap[String(status)]?.type || "default";
}

async function handleOpsPolicy(row) {
  opsTask.value = row || {};
  opsForm.value = {
    taskId: row.id,
    failStopEnabled: false,
    aiManaged: false,
    autoRecoverEnabled: false,
    maxRecoverTimes: 1,
    recoverStrategy: "SAFE_AUTO",
  };
  opsDialogVisible.value = true;
  try {
    const res = await getTaskOpsPolicy(row.id);
    if (res?.data) {
      opsForm.value = {
        taskId: row.id,
        failStopEnabled: !!res.data.failStopEnabled,
        aiManaged: !!res.data.aiManaged,
        autoRecoverEnabled: !!res.data.autoRecoverEnabled,
        maxRecoverTimes: res.data.maxRecoverTimes ?? 1,
        recoverStrategy: res.data.recoverStrategy || "SAFE_AUTO",
        notifyUsers: res.data.notifyUsers || "",
      };
    }
  } catch (e) {
    proxy.$modal.msgWarning("运维策略加载失败");
  }
}

async function saveOpsPolicy() {
  opsSaving.value = true;
  try {
    await saveTaskOpsPolicy(opsForm.value);
    proxy.$modal.msgSuccess("运维托管策略已保存");
    opsDialogVisible.value = false;
  } finally {
    opsSaving.value = false;
  }
}

const isPublished = (row) => String(row?.status) === "1";

const taskStatusWs = ref(null);
let taskStatusReconnectTimer = null;

function updateTaskStatusRow(payload) {
  return tableRef.value?.updateRowByKey?.("id", payload.taskId, {
    lastExecuteStatus: payload.lastExecuteStatus,
    lastExecuteTime: payload.lastExecuteTime,
  });
}

function handleTaskStatusMessage(event) {
  try {
    const payload = JSON.parse(event.data);
    if (!payload || payload.type !== "etl-task-status") {
      return;
    }
    if (String(payload.taskType) !== "1") {
      return;
    }
    if (
      payload.spaceId !== null &&
      payload.spaceId !== undefined &&
      String(payload.spaceId) !== String(userStore.spaceId)
    ) {
      return;
    }
    updateTaskStatusRow(payload);
  } catch (error) {
    console.warn("解析ETL任务状态WebSocket消息失败", error);
  }
}

function initTaskStatusWebSocket() {
  if (!userStore.id || !import.meta.env.VITE_APP_WEBSOCKET_API) {
    return;
  }
  closeTaskStatusWebSocket();
  const wsUri =
    import.meta.env.VITE_APP_WEBSOCKET_API +
    "/websocket/etlTask/" +
    userStore.id;
  taskStatusWs.value = new WebSocket(wsUri);
  taskStatusWs.value.onmessage = handleTaskStatusMessage;
  taskStatusWs.value.onclose = () => {
    taskStatusWs.value = null;
    if (taskStatusReconnectTimer) {
      clearTimeout(taskStatusReconnectTimer);
    }
    taskStatusReconnectTimer = setTimeout(() => {
      initTaskStatusWebSocket();
    }, 2000);
  };
}

function closeTaskStatusWebSocket() {
  if (taskStatusReconnectTimer) {
    clearTimeout(taskStatusReconnectTimer);
    taskStatusReconnectTimer = null;
  }
  if (taskStatusWs.value) {
    taskStatusWs.value.close();
    taskStatusWs.value = null;
  }
}

// Initialization
watch(
  () => userStore.spaceId,
  () => {
    handleQuery();
    getDeptTree();
  }
);

watch(
  () => userStore.id,
  (value) => {
    if (value) {
      initTaskStatusWebSocket();
    } else {
      closeTaskStatusWebSocket();
    }
  },
  { immediate: true }
);

if (userStore.spaceId) {
  getDeptTree();
}
usePageRefresh("integratioTask", () => handleQuery());

onBeforeUnmount(() => {
  closeTaskStatusWebSocket();
});
</script>

<style lang="scss" src="@/assets/system/styles/table-style-optimized.scss"></style>
<style scoped lang="scss">
:deep(.dm-search-bar) {
  .ant-form {
    flex-wrap: nowrap !important;
  }
  .ant-form-item {
    flex-shrink: 0 !important;
    margin-bottom: 0 !important;
  }
  .search-content {
    width: 150px !important;
  }
}
.cron-row {
  display: flex;
  align-items: center;
  gap: 5px;
}
.cron-icon {
  flex-shrink: 0;
  line-height: 1;
}
.cron-icon svg {
  display: block;
}
:deep(.anticon) {
  line-height: 1;
}
</style>


