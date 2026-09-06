<template>
  <a-modal
    v-model:open="visibleDialog"
    class="dialog"
    :title="title"
    destroy-on-close
    :width="900"
    :style="{ top: '40px' }"
  >
    <a-form
      ref="daDiscoveryTaskRef"
      :model="form"
      :rules="title == '任务详情' ? {} : rules"
      :label-col="{ style: { width: '130px' } }"
      @submit.prevent
      :disabled="title == '任务详情'"
    >
      <div class="h2-title">基本信息</div>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="任务名称" name="name">
            <a-input
              v-if="title != '任务详情'"
              v-model:value="form.name"
              placeholder="请输入任务名称"
            />
            <div class="form-readonly" v-else>{{ form.name }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="任务目录" name="catCode">
            <a-tree-select
              :tree-default-expanded-keys="defaultExpandedCats"
              show-search
              v-model:value="form.catCode"
              :tree-data="deptOptions"
              :field-names="{ value: 'code', label: 'name', children: 'children' }"
              placeholder="请选择任务目录"
              @select="(value, node) => handleNodeClick(node)"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="执行策略" name="executionType">
            <a-select
              v-if="title != '任务详情'"
              class="el-form-input-width"
              v-model:value="form.executionType"
              placeholder="请选择执行策略"
              style="width: 100%"
            >
              <a-select-option
                v-for="dict in col_etl_task_execution_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              >{{ dict.label }}</a-select-option>
            </a-select>
            <div class="form-readonly" v-else>
              {{
                col_etl_task_execution_type.find(
                  (item) => item.value == form.executionType
                )?.label || "-"
              }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="调度周期" name="crontab">
            <a-input
              v-if="title != '任务详情'"
              class="cron-input"
              v-model:value="form.crontab"
              placeholder="请选择调度周期"
            >
              <template #addonAfter>
                <a-button
                  type="primary"
                  @click="handleShowCron"
                  style="background-color: #2666fb; color: #fff; border: none; box-shadow: none; height: 100%;"
                >
                  配置
                </a-button>
              </template>
            </a-input>
            <div class="form-readonly" v-else>{{ form.crontab }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="描述" name="description">
            <a-textarea
              v-if="title != '任务详情'"
              v-model:value="form.description"
              placeholder="请输入描述"
            />
            <div class="form-readonly" v-else>
              {{ form.description || "-" }}
            </div>
          </a-form-item>
        </a-col>
      </a-row>
      <div class="h2-title prop-title" :class="{ collapsed: propCollapsed }" @click="propCollapsed = !propCollapsed">
        <span class="prop-title-inner">
          <span class="prop-title-text">属性信息</span>
          <span class="prop-caret">{{ propCollapsed ? '▸' : '▾' }}</span>
        </span>
      </div>
      <div v-show="!propCollapsed">
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="任务优先级" name="taskPriority">
            <a-select
              v-if="title != '任务详情'"
              allow-clear
              v-model:value="form.taskPriority"
              placeholder="请选择任务优先级"
            >
              <a-select-option
                v-for="(item, index) in col_etl_task_priority"
                :key="index"
                :label="item.label"
                :value="item.value"
              />
            </a-select>
            <div class="form-readonly" v-else>
              {{
                col_etl_task_priority.find(
                  (item) => item.value == form.taskPriority
                )?.label || "-"
              }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="Worker分组" name="workerGroup">
            <a-input
              v-if="title != '任务详情'"
              v-model:value="form.workerGroup"
              placeholder="请输入Worker分组"
              disabled
            />
            <div class="form-readonly" v-else>
              {{ form.workerGroup ?? "-" }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="失败重试次数" name="failRetryTimes">
            <a-input
              v-if="title != '任务详情'"
              type="number"
              v-model:value="form.failRetryTimes"
              placeholder="请输入失败重试次数"
            >
              <template #addonAfter>次</template>
            </a-input>
            <div class="form-readonly" v-else>
              {{ form.failRetryTimes || "-" }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="失败重试间隔" name="failRetryInterval">
            <a-input
              v-if="title != '任务详情'"
              type="number"
              v-model:value="form.failRetryInterval"
              placeholder="请输入失败重试间隔"
            >
              <template #addonAfter>分</template>
            </a-input>
            <div class="form-readonly" v-else>
              {{ form.failRetryInterval || "-" }}
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="延迟执行时间" name="delayTime">
            <a-input
              v-if="title != '任务详情'"
              type="number"
              v-model:value="form.delayTime"
              placeholder="请输入延迟执行时间"
            >
              <template #addonAfter>分</template>
            </a-input>
            <div class="form-readonly" v-else>{{ form.delayTime || "-" }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="执行引擎" name="taskType">
            <div class="form-readonly">FLINK (FlinkX)</div>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="20">
        <template v-if="form.taskType == 'FLINK'">
          <a-col :span="12">
            <a-form-item label="JobManager内存" name="jobManagerMemory">
              <a-input v-if="title != '任务详情'" v-model:value="form.jobManagerMemory" placeholder="请输入JobManager内存，如 1G" />
              <div class="form-readonly" v-else>{{ form.jobManagerMemory || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="TaskManager内存" name="taskManagerMemory">
              <a-input v-if="title != '任务详情'" v-model:value="form.taskManagerMemory" placeholder="请输入TaskManager内存，如 2G" />
              <div class="form-readonly" v-else>{{ form.taskManagerMemory || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Slot数量" name="slot">
              <a-input-number v-if="title != '任务详情'" v-model:value="form.slot" :min="1" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.slot || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="TaskManager数量" name="taskManager">
              <a-input-number v-if="title != '任务详情'" v-model:value="form.taskManager" :min="1" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.taskManager || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="并行度" name="parallelism">
              <a-input-number v-if="title != '任务详情'" v-model:value="form.parallelism" :min="1" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.parallelism || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Yarn队列" name="yarnQueue">
              <a-input v-if="title != '任务详情'" v-model:value="form.yarnQueue" placeholder="请输入Yarn队列(选填)" />
              <div class="form-readonly" v-else>{{ form.yarnQueue || "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="错误记录容忍" name="errorLimitRecord">
              <a-input-number v-if="title != '任务详情'" v-model:value="form.errorLimitRecord" :min="0" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.errorLimitRecord ?? "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="错误比例容忍" name="errorLimitPercentage">
              <a-input-number v-if="title != '任务详情'" v-model:value="form.errorLimitPercentage" :min="0" :max="1" :step="0.01" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.errorLimitPercentage ?? "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="断点续传" name="isRestore">
              <a-switch v-if="title != '任务详情'" v-model:checked="form.isRestore" />
              <div class="form-readonly" v-else>{{ form.isRestore ? "开启" : "关闭" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Checkpoint行数" name="maxRowNumForCheckpoint">
              <a-input-number v-if="title != '任务详情'" v-model:value="form.maxRowNumForCheckpoint" :min="0" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.maxRowNumForCheckpoint ?? "-" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="任务日志" name="isLogger">
              <a-switch v-if="title != '任务详情'" v-model:checked="form.isLogger" />
              <div class="form-readonly" v-else>{{ form.isLogger ? "开启" : "关闭" }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="日志级别" name="logLevel">
              <a-select v-if="title != '任务详情'" v-model:value="form.logLevel" style="width: 100%">
                <a-select-option label="INFO" value="info" />
                <a-select-option label="DEBUG" value="debug" />
                <a-select-option label="WARN" value="warn" />
                <a-select-option label="ERROR" value="error" />
              </a-select>
              <div class="form-readonly" v-else>{{ form.logLevel || "-" }}</div>
            </a-form-item>
          </a-col>
        </template>
      </a-row>
      </div>
    </a-form>
    <template #footer>
      <div style="text-align: right">
        <template v-if="info">
          <a-button @click="closeDialog">关闭</a-button>
          <a-button type="primary" v-if="!route.query.info" @click="saveClose"
            >保存</a-button
          >
        </template>
        <template v-else>
          <a-button @click="saveClose" :disabled="saveLoading">仅保存</a-button>
          <a-button type="primary" @click="saveData" :disabled="saveLoading">保存并配置流程</a-button>
        </template>
      </div>
    </template>
  </a-modal>

  <a-modal
    title="Cron表达式生成器"
    v-model:open="openCron"
    class="dialog"
    :footer="null"
    destroy-on-close
    :width="700"
  >
    <!--    <crontab ref="crontabRef" @hide="openCron = false" @fill="crontabFill" :expression="expression" :Crontab="false">-->
    <crontab
      ref="crontabRef"
      @hide="openCron = false"
      @fill="crontabFill"
      :expression="expression"
    >
    </crontab>
  </a-modal>
</template>

<script setup>
import { ref, computed, watch, getCurrentInstance } from "vue";
import Crontab from "@/components/Crontab/index.vue";
import useUserStore from "@/store/system/user";
const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const {
  col_etl_task_execution_type,
  dpp_etl_task_status,
  col_etl_task_priority,
} = proxy.useDict(
  "col_etl_task_execution_type",
  "dpp_etl_task_status",
  "col_etl_task_priority"
);
import { useRoute, useRouter } from "vue-router";
const route = useRoute();
const router = useRouter();
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  data: { type: Object, default: () => ({}) },
  deptOptions: { type: Object, default: () => ({}) },
  info: { type: Boolean, default: false },
  catCode: { type: String, default: "" },
  savedDataSourceId: { type: [String, Number], default: "" },
  savedAssetTableId: { type: [String, Number], default: "" },
  savedDataSourceName: { type: String, default: "" },
  savedDataSourceType: { type: String, default: "" },
});

const emit = defineEmits(["update:visible", "confirm", "save", "回echo完成"]);

const saveLoading = ref(false);

// 属性信息区默认折叠
const propCollapsed = ref(true);

// 定义表单验证规则
const rules = {
  name: [{ required: true, message: "任务名称不能为空", trigger: "change" }],
  catCode: [{ required: true, message: "任务目录不能为空", trigger: "change" }],
  executionType: [
    { required: true, message: "执行策略不能为空", trigger: "change" },
  ],
  // releaseState: [{ required: true, message: "任务状态不能为空", trigger: "change" }],
  taskType: [{ required: true, message: "执行引擎不能为空", trigger: "change" }],
};
const form = ref({
  catId: "",
  name: "",
  catCode: "", // 可以初始化为空，也可以设为默认值
  executionType: "PARALLEL", // 初始化为空或默认值
  crontab: "",
  releaseState: "0",
  description: "",
  // 新添加
  taskPriority: "",
  workerGroup: "default",
  failRetryTimes: "",
  failRetryInterval: "",
  delayTime: "",
  taskType: "FLINK",
  // Fink配置
  jobManagerMemory: "1G",
  taskManagerMemory: "2G",
  slot: 1,
  taskManager: 2,
  parallelism: 1,
  errorLimitRecord: 100,
  errorLimitPercentage: 0.1,
  isRestore: true,
  maxRowNumForCheckpoint: 10000,
  isLogger: true,
  logLevel: "info",
  yarnQueue: "",
});

const reset = () => {
  proxy.resetForm("daDiscoveryTaskRef");
  form.value = {
    name: "",
    catId: "",
    catCode: "", // 可以初始化为空，也可以设为默认值
    executionType: "PARALLEL", // 初始化为空或默认值
    crontab: "",
    releaseState: "0",
    description: "",
    // 新添加
    taskPriority: "",
    workerGroup: "default",
    failRetryTimes: "",
    failRetryInterval: "",
    delayTime: "",
    taskType: "FLINK",
    // Fink配置
    jobManagerMemory: "1G",
    taskManagerMemory: "2G",
    slot: 1,
    taskManager: 2,
    parallelism: 1,
    errorLimitRecord: 100,
    errorLimitPercentage: 0.1,
    isRestore: true,
    maxRowNumForCheckpoint: 10000,
    isLogger: true,
    logLevel: "info",
    yarnQueue: "",
  };
};
watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      if (props.data.id) {
        let data = JSON.parse(JSON.stringify(props.data.taskConfig));
        console.log("🚀 ~ props.data.taskConfig:", props.data.taskConfig);
        let draftJson = JSON.parse(data.draftJson);
        form.value = { ...data, ...draftJson };
        form.value.taskType = "FLINK";
        applyFlinkSettingDefaults();
        form.value.crontab = props?.data.taskConfig?.crontab;
      } else {
        form.value.catCode = props?.catCode || "";
        applyFlinkSettingDefaults();
      }
    } else {
      reset();
      saveLoading.value = false;
    }
  }
);
const handleNodeClick = (val) => {
  console.log("任务目录改变了，当前值：", val);
  console.log("任务目录 code 字段：", val && val.code, "| form.catCode：", form.value.catCode);
  // 虚拟根节点（数据集成目录, id=0）不可作为实际任务目录，选中时忽略
  if (val && (val.code === "0" || val.id === 0 || val.id === "0")) {
    form.value.catCode = "";
    form.value.catId = "";
    return;
  }
  // 目录节点可能没有 code 字段（仅 id），此时退回用 id 作为 catCode，保证必填校验能通过
  if (val && (val.code === undefined || val.code === null || val.code === "")) {
    form.value.catCode = String(val.id);
  }
  form.value.catId = val.id;
};
// 计算属性处理 v-model
const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit("update:visible", newValue);
  },
});
let daDiscoveryTaskRef = ref();
const closeDialog = () => {
  emit("update:visible", false);
};
const applyCurrentUserAsCreator = () => {
  form.value.creatorId = userStore.id;
  form.value.createBy = userStore.nickName || userStore.name;
};
const saveClose = () => {
  if (saveLoading.value) return;
  console.log("🚀 saveClose called, saveLoading:", saveLoading.value);
  daDiscoveryTaskRef.value.validate().then(() => {
    console.log("🚀 validate callback, valid:", true, "saveLoading:", saveLoading.value);
    saveLoading.value = true;
    console.log("🚀 emitting save event");
    normalizeFlinkSetting();
    applyCurrentUserAsCreator();
    emit("save", form.value);
  }).catch(() => {
    // 校验失败不应静默吞掉，提示用户缺失字段
    proxy.$modal.msgError("请完善必填项后保存");
  });
};
// 保存数据的方法
const saveData = () => {
  if (saveLoading.value) return;
  console.log("🚀 [saveData] 开始 validate...");
  daDiscoveryTaskRef.value.validate().then(() => {
    console.log("✅ [saveData] validate 通过，进入 then");
    saveLoading.value = true;
    try {
      normalizeFlinkSetting();
      applyCurrentUserAsCreator();
      emit("confirm", form.value);
      // 发送回echo完成事件
      emit("回echo完成", {
        dataSourceId: props.savedDataSourceId,
        dataSourceName: props.savedDataSourceName,
        dataSourceType: props.savedDataSourceType,
        assetTableId: props.savedAssetTableId
      });
    } catch (e) {
      console.error("❌ [saveData] then 内部异常：", e);
      proxy.$modal.msgError("保存异常：" + (e && e.message ? e.message : e));
    }
  }).catch((err) => {
    const flat = (err && err.errorFields || []).map((f) => f.name);
    console.warn("❌ [saveData] validate 未通过 errors：", flat, "| err=", err, "| form.catCode=", form.value.catCode, "| form.name=", form.value.name, "| executionType=", form.value.executionType, "| taskType=", form.value.taskType);
    if (flat.length) {
      proxy.$modal.msgError("请完善必填项：" + flat.join("、"));
    } else if (err && err.errorFields) {
      // validate() 返回了空 errorFields —— 一般是某个 form-item 字段访问异常造成
      console.warn("⚠️ validate 返回空 errorFields，err 详情：", JSON.stringify(err));
      proxy.$modal.msgError("表单校验异常，请检查控制台后重试");
    } else {
      proxy.$modal.msgError("保存失败：" + (err && err.message ? err.message : String(err)));
    }
  });
};

const applyFlinkSettingDefaults = () => {
  form.value.taskType = "FLINK";
  const setting = form.value.setting || {};
  const errorLimit = setting.errorLimit || {};
  const restore = setting.restore || {};
  const log = setting.log || {};
  form.value.jobManagerMemory = form.value.jobManagerMemory || "1G";
  form.value.taskManagerMemory = form.value.taskManagerMemory || "2G";
  form.value.slot = Number(form.value.slot || 1);
  form.value.taskManager = Number(form.value.taskManager || 2);
  form.value.parallelism = Number(form.value.parallelism || 1);
  form.value.errorLimitRecord = Number(form.value.errorLimitRecord ?? errorLimit.record ?? 100);
  form.value.errorLimitPercentage = Number(form.value.errorLimitPercentage ?? errorLimit.percentage ?? 0.1);
  form.value.isRestore = form.value.isRestore ?? restore.isRestore ?? true;
  form.value.maxRowNumForCheckpoint = Number(form.value.maxRowNumForCheckpoint ?? restore.maxRowNumForCheckpoint ?? 10000);
  form.value.isLogger = form.value.isLogger ?? log.isLogger ?? true;
  form.value.logLevel = form.value.logLevel || log.level || "info";
};

const normalizeFlinkSetting = () => {
  form.value.taskType = "FLINK";
  form.value.setting = {
    ...(form.value.setting || {}),
    errorLimit: {
      record: Number(form.value.errorLimitRecord ?? 100),
      percentage: Number(form.value.errorLimitPercentage ?? 0.1),
    },
    restore: {
      maxRowNumForCheckpoint: Number(form.value.maxRowNumForCheckpoint ?? 10000),
      isRestore: Boolean(form.value.isRestore),
      restoreColumnName: form.value.restoreColumnName || "",
      restoreColumnIndex: Number(form.value.restoreColumnIndex || 0),
    },
    log: {
      isLogger: Boolean(form.value.isLogger),
      level: form.value.logLevel || "info",
      path: form.value.logPath || "",
      pattern: form.value.logPattern || "",
    },
  };
};

let openCron = ref(false);
const expression = ref("");
/** 调度周期按钮操作 */
function handleShowCron() {
  expression.value = form.value.crontab;
  openCron.value = true;
}
/** 确定后回传值 */
function crontabFill(value) {
  form.value.crontab = value;
}
const defaultExpandedCats = computed(() => {
  return props.deptOptions.map((item) => item.code);
});
</script>
<style lang="scss" scoped>
.blue-text {
  color: #1677ff;
}

/* 属性信息区可折叠标题 */
.prop-title {
  cursor: pointer;
  user-select: none;
  display: flex;
  align-items: center;

  &.collapsed {
    margin-bottom: 0;
  }

  .prop-title-inner {
    display: inline-flex;
    align-items: center;
  }

  .prop-title-text {
    flex: 1;
  }

  .prop-caret {
    color: #909399;
    font-size: 24px;
    line-height: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    margin-left: 6px;
    transition: transform 0.2s;
  }

  &:hover .prop-caret {
    color: #1677ff;
  }
}

/* 任务配置弹窗：限制高度为视口自适应，内容超高时内部滚动，
   避免整个弹窗被内容撑高超出屏幕（"页面太高"） */
:deep(.dialog) {
  max-width: 100vw;
}

:deep(.dialog .ant-modal-body) {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  padding-right: 4px;
}

:deep(.ant-select) {
  .ant-select-selector {
    cursor: default;
    background-color: #fcfcfc;
    color: #333;
  }

  .ant-select-suffix {
    display: none;
  }
}

/* 调度周期 addonAfter 内的"配置"按钮：去掉 addon 容器的边框/背景，
   让按钮与输入框右侧无缝融合（避免"按钮外面还套个框"） */
:deep(.cron-input .ant-input-group-addon) {
  padding: 0;
  border: none;
  background-color: transparent;
}
</style>

