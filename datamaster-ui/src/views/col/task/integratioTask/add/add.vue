<template>
  <el-dialog
    v-model="visibleDialog"
    draggable
    class="dialog"
    :title="title"
    destroy-on-close
    :append-to="$refs['app-container']"
  >
    <el-form
      ref="daDiscoveryTaskRef"
      :model="form"
      :rules="title == '任务详情' ? {} : rules"
      label-width="146px"
      @submit.prevent
      :disabled="title == '任务详情'"
    >
      <div class="h2-title">基本信息</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="任务名称" prop="name">
            <el-input
              v-if="title != '任务详情'"
              v-model="form.name"
              placeholder="请输入任务名称"
            />
            <div class="form-readonly" v-else>{{ form.name }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="任务类目" prop="catCode">
            <el-tree-select
              :default-expanded-keys="defaultExpandedCats"
              filterable
              v-model="form.catCode"
              :data="deptOptions"
              :props="{ value: 'code', label: 'name', children: 'children' }"
              value-key="id"
              placeholder="请选择任务类目"
              check-strictly
              @node-click="handleNodeClick"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="执行策略" prop="executionType">
            <el-select
              v-if="title != '任务详情'"
              class="el-form-input-width"
              v-model="form.executionType"
              placeholder="请选择执行策略"
              style="width: 100%"
            >
              <el-option
                v-for="dict in col_etl_task_execution_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              ></el-option>
            </el-select>
            <div class="form-readonly" v-else>
              {{
                col_etl_task_execution_type.find(
                  (item) => item.value == form.executionType
                )?.label || "-"
              }}
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="调度周期" prop="crontab">
            <el-input
              v-if="title != '任务详情'"
              v-model="form.crontab"
              placeholder="请选择调度周期"
            >
              <template #append>
                <el-button
                  type="primary"
                  @click="handleShowCron"
                  style="background-color: #2666fb; color: #fff"
                >
                  配置
                  <i class="el-icon-time el-icon--right"></i>
                </el-button>
              </template>
            </el-input>
            <div class="form-readonly" v-else>{{ form.crontab }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="责任人" prop="personCharge">
            <el-tree-select
              filterable
              v-model="form.personCharge"
              :data="userList"
              :props="{
                value: 'userId',
                label: 'nickName',
                children: 'children',
              }"
              value-key="ID"
              placeholder="请选择责任人"
              check-strictly
              @change="handleContactChange"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话" prop="contactNumber">
            <el-input
              v-if="title != '任务详情'"
              v-model="form.contactNumber"
              placeholder="请输入联系电话"
              disabled
            >
            </el-input>
            <div class="form-readonly" v-else>{{ form.contactNumber }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="描述" prop="description">
            <el-input
              v-if="title != '任务详情'"
              v-model="form.description"
              type="textarea"
              placeholder="请输入描述"
            />
            <div class="form-readonly" v-else>
              {{ form.description || "-" }}
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="任务状态" prop="releaseState">
            <el-radio-group
              v-if="title != '任务详情'"
              v-model="form.releaseState"
              class="el-form-input-width"
            >
              <el-radio
                v-for="dict in dpp_etl_task_status"
                :key="dict.value"
                :label="dict.value"
                :disabled="dict.value == 1"
              >
                {{ dict.label }}
              </el-radio>
            </el-radio-group>
            <div class="form-readonly" v-else>
              {{
                dpp_etl_task_status.find(
                  (item) => item.value == form.releaseState
                )?.label || "-"
              }}
            </div>
          </el-form-item>
        </el-col>
      </el-row>
      <div class="h2-title">属性信息</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="任务优先级" prop="taskPriority">
            <el-select
              v-if="title != '任务详情'"
              clearable
              v-model="form.taskPriority"
              placeholder="请选择任务优先级"
            >
              <el-option
                v-for="(item, index) in col_etl_task_priority"
                :key="index"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <div class="form-readonly" v-else>
              {{
                col_etl_task_priority.find(
                  (item) => item.value == form.taskPriority
                )?.label || "-"
              }}
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Worker分组" prop="workerGroup">
            <el-input
              v-if="title != '任务详情'"
              v-model="form.workerGroup"
              placeholder="请输入Worker分组"
              disabled
            />
            <div class="form-readonly" v-else>
              {{ form.workerGroup ?? "-" }}
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="失败重试次数" prop="failRetryTimes">
            <el-input
              v-if="title != '任务详情'"
              type="number"
              v-model="form.failRetryTimes"
              placeholder="请输入失败重试次数"
            >
              <template #append>次</template>
            </el-input>
            <div class="form-readonly" v-else>
              {{ form.failRetryTimes || "-" }}
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="失败重试间隔" prop="failRetryInterval">
            <el-input
              v-if="title != '任务详情'"
              type="number"
              v-model="form.failRetryInterval"
              placeholder="请输入失败重试间隔"
            >
              <template #append>分</template>
            </el-input>
            <div class="form-readonly" v-else>
              {{ form.failRetryInterval || "-" }}
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="延迟执行时间" prop="delayTime">
            <el-input
              v-if="title != '任务详情'"
              type="number"
              v-model="form.delayTime"
              placeholder="请输入延迟执行时间"
            >
              <template #append>分</template>
            </el-input>
            <div class="form-readonly" v-else>{{ form.delayTime || "-" }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="执行引擎" prop="taskType">
            <el-radio-group
              v-if="title != '任务详情'"
              v-model="form.taskType"
              class="el-form-input-width"
              :disabled="props.data.id"
            >
              <el-radio label="SPARK"> SPARK </el-radio>
              <el-radio label="FLINK"> FLINK (FlinkX) </el-radio>
            </el-radio-group>
            <div class="form-readonly" v-else>{{ form.taskType || "-" }}</div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <template v-if="form.taskType == 'SPARK'">
          <el-col :span="12">
            <el-form-item label="Driver核心数" prop="driverCores">
              <el-input-number
                v-if="title != '任务详情'"
                placeholder="请输入Driver核心数"
                v-model="form.driverCores"
                controls-position="right"
                :min="0"
                style="width: 100%"
              />
              <div class="form-readonly" v-else>
                {{ form.driverCores || "-" }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Driver内存数" prop="driverMemory">
              <el-input
                v-if="title != '任务详情'"
                v-model="form.driverMemory"
                placeholder="请输入Driver内存数"
                style="width: 100%"
              >
              </el-input>
              <div class="form-readonly" v-else>
                {{ form.driverMemory || "-" }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Executor数量" prop="numExecutors">
              <el-input-number
                v-if="title != '任务详情'"
                placeholder="请输入Executor数量"
                v-model="form.numExecutors"
                controls-position="right"
                style="width: 100%"
                :min="0"
              />
              <div class="form-readonly" v-else>
                {{ form.numExecutors || "-" }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Executor内存数" prop="executorMemory">
              <el-input
                v-if="title != '任务详情'"
                v-model="form.executorMemory"
                placeholder="请输入Executor内存数"
                style="width: 100%"
              >
              </el-input>
              <div class="form-readonly" v-else>
                {{ form.executorMemory || "-" }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Executor核心数" prop="executorCores">
              <el-input-number
                v-if="title != '任务详情'"
                placeholder="请输入Executor核心数"
                v-model="form.executorCores"
                controls-position="right"
                style="width: 100%"
                :min="0"
              />
              <div class="form-readonly" v-else>
                {{ form.executorCores || "-" }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Yarn队列" prop="yarnQueue">
              <el-input
                v-if="title != '任务详情'"
                v-model="form.yarnQueue"
                placeholder="请输入Yarn队列(选填)"
              >
              </el-input>
              <div class="form-readonly" v-else>
                {{ form.yarnQueue || "-" }}
              </div>
            </el-form-item>
          </el-col>
        </template>
        <template v-if="form.taskType == 'FLINK'">
          <el-col :span="12">
            <el-form-item label="JobManager内存" prop="jobManagerMemory">
              <el-input v-if="title != '任务详情'" v-model="form.jobManagerMemory" placeholder="请输入JobManager内存，如 1G" />
              <div class="form-readonly" v-else>{{ form.jobManagerMemory || "-" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="TaskManager内存" prop="taskManagerMemory">
              <el-input v-if="title != '任务详情'" v-model="form.taskManagerMemory" placeholder="请输入TaskManager内存，如 2G" />
              <div class="form-readonly" v-else>{{ form.taskManagerMemory || "-" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Slot数量" prop="slot">
              <el-input-number v-if="title != '任务详情'" v-model="form.slot" controls-position="right" :min="1" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.slot || "-" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="TaskManager数量" prop="taskManager">
              <el-input-number v-if="title != '任务详情'" v-model="form.taskManager" controls-position="right" :min="1" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.taskManager || "-" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="并行度" prop="parallelism">
              <el-input-number v-if="title != '任务详情'" v-model="form.parallelism" controls-position="right" :min="1" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.parallelism || "-" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Yarn队列" prop="yarnQueue">
              <el-input v-if="title != '任务详情'" v-model="form.yarnQueue" placeholder="请输入Yarn队列(选填)" />
              <div class="form-readonly" v-else>{{ form.yarnQueue || "-" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="错误记录容忍" prop="errorLimitRecord">
              <el-input-number v-if="title != '任务详情'" v-model="form.errorLimitRecord" controls-position="right" :min="0" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.errorLimitRecord ?? "-" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="错误比例容忍" prop="errorLimitPercentage">
              <el-input-number v-if="title != '任务详情'" v-model="form.errorLimitPercentage" controls-position="right" :min="0" :max="1" :step="0.01" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.errorLimitPercentage ?? "-" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="断点续传" prop="isRestore">
              <el-switch v-if="title != '任务详情'" v-model="form.isRestore" />
              <div class="form-readonly" v-else>{{ form.isRestore ? "开启" : "关闭" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Checkpoint行数" prop="maxRowNumForCheckpoint">
              <el-input-number v-if="title != '任务详情'" v-model="form.maxRowNumForCheckpoint" controls-position="right" :min="0" style="width: 100%" />
              <div class="form-readonly" v-else>{{ form.maxRowNumForCheckpoint ?? "-" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务日志" prop="isLogger">
              <el-switch v-if="title != '任务详情'" v-model="form.isLogger" />
              <div class="form-readonly" v-else>{{ form.isLogger ? "开启" : "关闭" }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日志级别" prop="logLevel">
              <el-select v-if="title != '任务详情'" v-model="form.logLevel" style="width: 100%">
                <el-option label="INFO" value="info" />
                <el-option label="DEBUG" value="debug" />
                <el-option label="WARN" value="warn" />
                <el-option label="ERROR" value="error" />
              </el-select>
              <div class="form-readonly" v-else>{{ form.logLevel || "-" }}</div>
            </el-form-item>
          </el-col>
        </template>
      </el-row>
      <!-- <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
          </el-form-item>
        </el-col>
      </el-row> -->
    </el-form>
    <template #footer>
      <div style="text-align: right">
        <template v-if="info">
          <el-button @click="closeDialog">关闭</el-button>
          <el-button type="primary" v-if="!route.query.info" @click="saveClose"
            >保存</el-button
          >
        </template>
        <template v-else>
          <el-button @click="saveClose" :disabled="saveLoading">仅保存</el-button>
          <el-button type="primary" @click="saveData" :disabled="saveLoading">保存并配置流程</el-button>
        </template>
      </div>
    </template>
  </el-dialog>

  <el-dialog
    title="Cron表达式生成器"
    v-model="openCron"
    class="dialog"
    :append-to="$refs['app-container']"
    destroy-on-close
  >
    <!--    <crontab ref="crontabRef" @hide="openCron = false" @fill="crontabFill" :expression="expression" :Crontab="false">-->
    <crontab
      ref="crontabRef"
      @hide="openCron = false"
      @fill="crontabFill"
      :expression="expression"
    >
    </crontab>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, getCurrentInstance } from "vue";
import Crontab from "@/components/Crontab/index.vue";
const { proxy } = getCurrentInstance();
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
  userList: { type: Object, default: () => ({}) },
  info: { type: Boolean, default: false },
  catCode: { type: String, default: "" },
  savedDataSourceId: { type: [String, Number], default: "" },
  savedAssetTableId: { type: [String, Number], default: "" },
  savedDataSourceName: { type: String, default: "" },
  savedDataSourceType: { type: String, default: "" },
});

const emit = defineEmits(["update:visible", "confirm", "save", "回echo完成"]);

const saveLoading = ref(false);

// 定义表单验证规则
const rules = {
  name: [{ required: true, message: "任务名称不能为空", trigger: "change" }],
  catCode: [{ required: true, message: "任务类目不能为空", trigger: "change" }],
  executionType: [
    { required: true, message: "执行策略不能为空", trigger: "change" },
  ],
  // releaseState: [{ required: true, message: "任务状态不能为空", trigger: "change" }],
  engine: [{ required: true, message: "执行引擎不能为空", trigger: "change" }],
  personCharge: [
    { required: true, message: "责任人不能为空", trigger: "change" },
  ],
};
const form = ref({
  catId: "",
  name: "",
  catCode: "", // 可以初始化为空，也可以设为默认值
  executionType: "PARALLEL", // 初始化为空或默认值
  crontab: "",
  releaseState: "0",
  description: "",
  contactNumber: "",
  personCharge: "",
  // 新添加
  taskPriority: "",
  workerGroup: "default",
  failRetryTimes: "",
  failRetryInterval: "",
  delayTime: "",
  taskType: "SPARK",
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
  // Spark配置
  driverCores: 1,
  driverMemory: "512m",
  numExecutors: 1,
  executorMemory: "512m",
  executorCores: 1,
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
    contactNumber: "",
    personCharge: "",
    // 新添加
    taskPriority: "",
    workerGroup: "default",
    failRetryTimes: "",
    failRetryInterval: "",
    delayTime: "",
    taskType: "SPARK",
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
    // Spark配置
    driverCores: 1,
    driverMemory: "512m",
    numExecutors: 1,
    executorMemory: "512m",
    executorCores: 1,
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
        applyFlinkSettingDefaults();
        form.value.personCharge = Number(form.value.personCharge) || "";
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
  console.log("任务类目改变了，当前值：", val);
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
const saveClose = () => {
  if (saveLoading.value) return;
  console.log("🚀 saveClose called, saveLoading:", saveLoading.value);
  daDiscoveryTaskRef.value.validate((valid) => {
    console.log("🚀 validate callback, valid:", valid, "saveLoading:", saveLoading.value);
    if (valid) {
      saveLoading.value = true;
      console.log("🚀 emitting save event");
      normalizeFlinkSetting();
      emit("save", form.value);
    }
  });
};
// 保存数据的方法
const saveData = () => {
  if (saveLoading.value) return;
  daDiscoveryTaskRef.value.validate((valid) => {
    if (valid) {
      saveLoading.value = true;
      normalizeFlinkSetting();
      emit("confirm", form.value);
      // 发送回echo完成事件
      emit("回echo完成", {
        dataSourceId: props.savedDataSourceId,
        dataSourceName: props.savedDataSourceName,
        dataSourceType: props.savedDataSourceType,
        assetTableId: props.savedAssetTableId
      });
    } else {
      console.log("表单校验未通过");
    }
  });
};

const applyFlinkSettingDefaults = () => {
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
  if (form.value.taskType !== "FLINK") {
    return;
  }
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
const handleContactChange = (selectedValue) => {
  const selectedUser = props.userList.find(
    (user) => user.userId == selectedValue
  );
  console.log("🚀 ~ handleContactChange ~ selectedUser:", selectedUser);
  form.value.contactNumber = selectedUser?.phonenumber || "";
};
const defaultExpandedCats = computed(() => {
  return props.deptOptions.map((item) => item.id);
});
</script>
<style lang="scss" scoped>
.blue-text {
  color: var(--el-color-primary);
}

:deep(.el-select) {
  .el-select__wrapper.is-disabled {
    cursor: default;
    background-color: #fcfcfc;
    --el-select-disabled-color: #333;

    .el-select__suffix {
      display: none;
    }
  }
}
</style>

