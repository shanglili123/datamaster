<template>
  <div
    class="app-container quality-task-page"
    ref="app-container"
    style="background-color: #f0f2f5"
  >
    <a-spin class="quality-page-spin" :spinning="loadingInstance">
    <div class="custom-card">
      <div class="steps-inner">
        <ul class="zl-step">
          <li
            v-for="(item, index) in stepsList"
            :key="index"
            :class="{
              statusEnd: activeReult === index,
              prevStep: index < activeReult,
              cur: index > activeReult,
            }"
          >
            <div
              class="step-circle"
              :class="{
                active: activeReult === index,
                prev: index < activeReult,
              }"
            >
              <span>{{ index + 1 }}</span>
            </div>

            <!-- 步骤名称 -->
            <span class="step-name">{{ item.name }}</span>
          </li>
        </ul>
      </div>
    </div>

    <div
      class="pagecont-top quality-task-editor"
      v-show="showSearch"
      style="padding-bottom: 15px"
    >
      <a-spin class="quality-content-spin" :spinning="loading">
      <div class="infotop">
        <div class="main">
          <a-form
            ref="formRef"
            class="quality-base-form"
            :model="form"
            :label-col="{ style: { width: '120px' } }"
            v-show="activeReult == 0"
            :disabled="route.query.info"
          >
            <div class="h2-titles">基础信息</div>
            <a-row :gutter="20">
              <a-col :xs="24" :md="12">
                <a-form-item
                  label="任务名称"
                  name="taskName"
                  :rules="[
                    {
                      required: true,
                      message: '请输入任务名称',
                      trigger: 'blur',
                    },
                  ]"
                >
                  <a-input
                    v-model:value="form.taskName"
                    placeholder="请输入任务名称"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item
                  label="任务分类"
                  name="catCode"
                  :rules="[
                    {
                      required: true,
                      message: '请选择任务分类',
                      trigger: 'change',
                    },
                  ]"
                >
                  <a-tree-select
                    show-search
                    v-model:value="form.catCode"
                    :tree-data="deptOptions"
                    :field-names="{
                      value: 'code',
                      label: 'name',
                      children: 'children',
                    }"
                    placeholder="请选择任务分类"
                    tree-check-strictly
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="20">
              <a-col :xs="24" :md="12">
                <a-form-item
                  label="执行策略"
                  name="strategy"
                  :rules="[
                    {
                      required: true,
                      message: '请选择执行策略',
                      trigger: 'blur',
                    },
                  ]"
                >
                  <a-select
                    class="el-form-input-width"
                    v-model:value="form.strategy"
                    placeholder="请选择执行策略"
                    style="width: 100%"
                  >
                    <a-select-option
                      v-for="dict in col_etl_task_execution_type"
                      :key="dict.value"
                      :value="dict.value"
                    >{{ dict.label }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item
                  label="调度周期"
                  name="cycle"
                >
                  <a-input v-model:value="form.cycle" placeholder="请选择调度周期">
                    <template #addonAfter>
                      <a-button
                        type="primary"
                        @click="handleShowCron"
                        style="background-color: #2666fb; color: #fff"
                      >
                        配置
                        <ClockCircleOutlined style="margin-left: 4px" />
                      </a-button>
                    </template>
                  </a-input>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="20">
              <a-col :xs="24" :md="12">
                <a-form-item label="任务状态" name="status">
                  <a-radio-group
                    v-model:value="form.status"
                    class="el-form-input-width"
                  >
                    <a-radio
                      v-for="dict in ast_discovery_task_status"
                      :key="dict.value"
                      :value="dict.value"
                    >
                      {{ dict.label }}
                    </a-radio>
                  </a-radio-group>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="20">
              <a-col :span="24">
                <a-form-item label="任务描述" name="description">
                  <a-textarea
                    v-model:value="form.description"
                    placeholder="请输入任务描述"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <!--                        <a-divider>-->
            <!--                            <span class="blue-text">属性信息</span>-->
            <!--                        </a-divider>-->
            <!-- <div class="clearfix header-text">
                            <div class="header-left">
                                <div class="blue-bar"></div>

                            </div>
                        </div> -->
            <div class="h2-titles">属性信息</div>
            <a-row :gutter="20">
              <a-col :xs="24" :md="12">
                <a-form-item
                  label="任务优先级"
                  name="priority"
                  :rules="[
                    {
                      required: true,
                      message: '请选择任务优先级',
                      trigger: 'change',
                    },
                  ]"
                >
                  <a-select
                    v-model:value="form.priority"
                    placeholder="请选择任务优先级"
                  >
                    <a-select-option
                      v-for="dict in priorityOptions"
                      :key="dict.value"
                      :value="dict.value"
                    >{{ dict.label }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item
                  label="Worker分组"
                  name="workerGroup"
                  :rules="[
                    {
                      required: true,
                      message: '请输入Worker分组',
                      trigger: 'blur',
                    },
                  ]"
                >
                  <a-input
                    v-model:value="form.workerGroup"
                    placeholder="请输入Worker分组"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="失败重试次数" name="retryTimes">
                  <a-input
                    type="number"
                    v-model:value="form.retryTimes"
                    placeholder="请输入失败重试次数"
                  >
                    <template #addonAfter>次</template>
                  </a-input>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="延迟执行时间" name="delayTime">
                  <a-input
                    type="number"
                    v-model:value="form.delayTime"
                    placeholder="请输入延迟执行时间"
                  >
                    <template #addonAfter>分</template>
                  </a-input>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="备注" name="remark">
              <a-textarea
                v-model:value="form.remark"
                placeholder="请输入备注"
              />
            </a-form-item>
          </a-form>

          <div v-show="activeReult == 1">
            <a-spin :spinning="loadingList" style="display: block">
            <div class="h2-titles">稽查对象信息</div>
            <div class="justify-between mb15">
              <a-row :gutter="15" class="btn-style">
                <a-col :span="1.5">
                  <a-button
                    type="primary"
                    :icon="h(PlusOutlined)"
                    @click="openDialog(undefined)"
                    v-if="!route.query.info"
                    >新增</a-button>
                </a-col>
              </a-row>
            </div>
            <a-table striped :data-source="pagedQualityTaskObjects" :pagination="false" :scroll="{ y: 'max(220px, calc(100vh - 390px))' }" :columns="objTableColumns">
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.dataIndex === 'name'">
                  {{ record.name }}
                </template>
                <template v-if="column.dataIndex === 'datasourceType'">
                  <img
                    :src="getDatasourceIcon(record.datasourceType)"
                    class="iconimg"
                  />
                  {{ record.datasourceType }}
                </template>
                <template v-if="column.dataIndex === 'dbname'">
                  <template v-if="record.datasourceConfig">
                    <template
                      v-if="JSON.parse(record.datasourceConfig).dbname"
                    >
                      {{ JSON.parse(record.datasourceConfig).dbname }}
                    </template>
                  </template>
                </template>
                <template v-if="column.dataIndex === 'tableName'">
                  {{ record.tableName }}
                </template>
                <template v-if="column.key === 'actions'">
                  <a-button
                    type="link"
                    size="small"
                    @click="openDialog(record, getObjectGlobalIndex(index) + 1)"
                    >修改</a-button>
                  <a-button
                    type="link"
                    danger
                    size="small"
                    @click="handleDelete(record)"
                    >删除</a-button>
                </template>
              </template>
            </a-table>
            <pagination
              v-show="dppQualityTaskObjSaveReqVO.length > 0"
              :total="dppQualityTaskObjSaveReqVO.length"
              v-model:page="objectQueryParams.pageNum"
              v-model:limit="objectQueryParams.pageSize"
            />
            </a-spin>
          </div>
          <div v-show="activeReult == 2">
            <a-spin :spinning="loadingList" style="display: block">
            <div class="clearfix header-text" style="margin-top: 10px">
              <div class="header-left">
                <div class="blue-bar"></div>
                稽查规则信息
              </div>
            </div>
            <a-form
              class="btn-style"
              :model="queryParams"
              ref="queryRef"
              layout="inline"
              :label-col="{ style: { width: '75px' } }"
              @submit.prevent
            >
              <a-form-item label="规则名称" name="name">
                <a-input
                  class="el-form-input-width"
                  v-model:value="queryParams.name"
                  placeholder="请输入规则名称"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
              <a-form-item label="质量维度" name="dimensionType">
                <a-select
                  v-model:value="queryParams.dimensionType"
                  placeholder="请选择质量维度"
                  style="width: 210px"
                >
                  <a-select-option
                    v-for="dict in att_rule_audit_q_dimension"
                    :key="dict.value"
                    :value="dict.value"
                  >{{ dict.label }}</a-select-option>
                </a-select>
              </a-form-item>

              <a-form-item label="状态" name="publishStatus">
                <a-select
                  v-model:value="queryParams.publishStatus"
                  placeholder="请选择状态"
                  allow-clear
                  class="el-form-input-width"
                >
                  <a-select-option label="上线" value="online" />
                  <a-select-option label="下线" value="offline" />
                </a-select>
              </a-form-item>
              <a-form-item>
                <a-button
                  type="primary"
                  @click="handleQuery"
                  @mousedown="(e) => e.preventDefault()"
                >
                  <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
                </a-button>
                <a-button
                  @click="resetQuery"
                  @mousedown="(e) => e.preventDefault()"
                >
                  <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
                </a-button>
              </a-form-item>
            </a-form>
            <div class="justify-between mb15">
              <a-row :gutter="15" class="btn-style">
                <a-col :span="1.5">
                  <a-button
                    type="primary"
                    :icon="h(PlusOutlined)"
                    @click="openRuleSelector(undefined)"
                    v-if="!route.query.info"
                    >新增</a-button>
                </a-col>
                <a-col :span="1.5">
                  <a-tooltip
                    title="会自动获取资产关联的数据元中的稽查规则"
                    placement="top"
                  >
                    <a-button
                      type="warning"
                      @click="selectInspectionRule(undefined)"
                      v-if="!route.query.info"
                    >
                      <template #icon>
                        <ReloadOutlined />
                      </template>
                      获取稽查规则
                    </a-button>
                  </a-tooltip>
                </a-col>
              </a-row>
            </div>
            <a-table
              striped
              :data-source="pagedQualityTaskEvaluates"
              :pagination="false"
              :scroll="{ y: 'max(220px, calc(100vh - 430px))' }"
              :columns="ruleTableColumns"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'name'">
                  {{ record.name || "-" }}
                </template>
                <template v-if="column.dataIndex === 'evaColumn'">
                  {{ record.evaColumn || "-" }}
                </template>
                <template v-if="column.dataIndex === 'ruleName'">
                  {{ record.ruleName || "-" }}
                </template>
                <template v-if="column.dataIndex === 'ruleDescription'">
                  {{ record.ruleDescription || "-" }}
                </template>
                <template v-if="column.dataIndex === 'dimensionType'">
                  <dict-tag
                    :options="att_rule_audit_q_dimension"
                    :value="record.dimensionType"
                  />
                </template>
                <template v-if="column.dataIndex === 'status'">
                  {{ record.status == "1" ? "上线" : "下线" }}
                </template>
                <template v-if="column.key === 'actions'">
                  <a-button
                    type="link"
                    size="small"
                    @click="openRuleDialog(record, getRuleOriginIndex(record) + 1)"
                    >修改</a-button>
                  <a-button
                    type="link"
                    danger
                    size="small"
                    @click="handleRuleDelete(record)"
                    >删除</a-button>
                </template>
              </template>
            </a-table>
            <pagination
              v-show="dppQualityTaskEvaluateSaveReqVO.length > 0"
              :total="dppQualityTaskEvaluateSaveReqVO.length"
              v-model:page="queryParams.pageNum"
              v-model:limit="queryParams.pageSize"
            />
            </a-spin>
          </div>
        </div>
        <div class="button-style">
          <a-button type="primary" @click="handleSuccess">返回列表</a-button>
          <a-button v-if="activeReult != 0" @click="handleLastStep">上一步</a-button>
          <a-button
            type="primary"
            v-if="activeReult === 2 && !route.query.info"
            @click="submitForm"
            :loading="loadingOptions.loading"
          >
            确定并退出
          </a-button>
          <a-button v-if="activeReult !== 2" @click="handleNextStep">下一步</a-button>
        </div>
      </div>
      </a-spin>
    </div>
    <a-modal title="Cron表达式生成器" v-model:open="openCron" destroy-on-close :footer="null">
      <crontab
        ref="crontabRef"
        @hide="openCron = false"
        @fill="crontabFill"
        :expression="expression"
      >
      </crontab>
    </a-modal>
    <InspectionTargetDialog
      ref="inspectionTargetDialog"
      @confirm="Inspectionconfirm"
    />
    <RuleSelectorDialog
      ref="ruleSelectorDialog"
      @confirm="RuleSelectorconfirm"
      :dppQualityTaskObjSaveReqVO="dppQualityTaskObjSaveReqVO"
    />
    </a-spin>
  </div>
</template>

<script setup name="qualityTask">
import { message } from 'ant-design-vue'
import { ref, reactive, toRefs, onMounted, computed, h } from "vue";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";

import { useRoute, useRouter } from "vue-router";

import InspectionTargetDialog from "../components/inspectionTarget.vue";

import RuleSelectorDialog from "../components/ruleBase.vue";

import { listAttQualityCat } from "@/api/tax/cat/qualityCat/qualityCat.js";

import {
  addDppQualityTask,
  updateDppQualityTask,
  getDppQualityTask,
} from "@/api/ast/quality/qualityTask";

import Crontab from "@/components/Crontab/index.vue";

import { getColumnByAssetId } from "@/api/col/task/index.js";

import useUserStore from "@/store/system/user";

import { treeData } from "../data.js";
const { proxy } = getCurrentInstance();
const route = useRoute();
const userStore = useUserStore();
const loading = ref(false);
const showSearch = ref(true);
let id = route.query.id || "";
const router = useRouter();
const {
  att_rule_audit_q_dimension,
  ast_discovery_task_status,
  col_etl_task_execution_type,
} = proxy.useDict(
  "att_rule_audit_q_dimension",
  "ast_discovery_task_status",
  "col_etl_task_execution_type"
);
let dppQualityTaskObjSaveReqVO = ref([]);
const objectQueryParams = ref({
  pageNum: 1,
  pageSize: 6,
});
// 图标
const getDatasourceIcon = (type) => {
  switch (type) {
    case "DM8":
      return new URL("@/assets/system/images/dpp/DM.png", import.meta.url).href;
    case "Oracle11":
      return new URL("@/assets/system/images/dpp/oracle.png", import.meta.url)
        .href;
    case "MySql":
      return new URL("@/assets/system/images/dpp/mysql.png", import.meta.url)
        .href;
    case "Hive":
      return new URL("@/assets/system/images/dpp/Hive.png", import.meta.url)
        .href;
    case "Sqlerver":
      return new URL(
        "@/assets/system/images/dpp/sqlServer.png",
        import.meta.url
      ).href;
    case "Kafka":
      return new URL("@/assets/system/images/dpp/kafka.png", import.meta.url)
        .href;
    case "HDFS":
      return new URL("@/assets/system/images/dpp/hdfs.png", import.meta.url)
        .href;
    case "SHELL":
      return new URL("@/assets/system/images/dpp/SHELL.png", import.meta.url)
        .href;
    case "Kingbase8":
      return new URL("@/assets/system/images/dpp/kingBase.png", import.meta.url)
        .href;
    default:
      return null;
  }
};

let loadingInstance = ref(null); // 全局 loading 实例
let originList = ref([]);

const dppQualityTaskEvaluateSaveReqVO = ref([...originList.value]);
const pagedQualityTaskObjects = computed(() => {
  const start =
    (objectQueryParams.value.pageNum - 1) * objectQueryParams.value.pageSize;
  return dppQualityTaskObjSaveReqVO.value.slice(
    start,
    start + objectQueryParams.value.pageSize
  );
});
const pagedQualityTaskEvaluates = computed(() => {
  const start = (queryParams.value.pageNum - 1) * queryParams.value.pageSize;
  return dppQualityTaskEvaluateSaveReqVO.value.slice(
    start,
    start + queryParams.value.pageSize
  );
});
const objTableColumns = computed(() => {
  const cols = [
    { title: '序号', key: 'index', align: 'center', width: 80, customRender: ({ index }) => index + 1 },
    { title: '稽查对象名称', dataIndex: 'name', align: 'center', ellipsis: true },
    { title: '数据连接名称', dataIndex: 'datasourceType', align: 'center', ellipsis: true },
    { title: '模式名称', dataIndex: 'dbname', align: 'center', ellipsis: true },
    { title: '表名称', dataIndex: 'tableName', align: 'center', ellipsis: true },
  ];
  if (!route.query.info) {
    cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 200 });
  }
  return cols;
});
const ruleTableColumns = computed(() => {
  const cols = [
    { title: '序号', key: 'index', align: 'center', width: 80, customRender: ({ index }) => index + 1 },
    { title: '评测名称', dataIndex: 'name', align: 'center', ellipsis: true },
    { title: '评测字段', dataIndex: 'evaColumn', align: 'center', ellipsis: true },
    { title: '稽查规则', dataIndex: 'ruleName', align: 'center', ellipsis: true },
    { title: '规则描述', dataIndex: 'ruleDescription', align: 'center', ellipsis: true },
    { title: '质量维度', dataIndex: 'dimensionType', align: 'center', width: 120, ellipsis: true },
    { title: '状态', dataIndex: 'status', align: 'center', width: 100 },
  ];
  if (!route.query.info) {
    cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 200 });
  }
  return cols;
});
function getObjectGlobalIndex(pageIndex) {
  return (
    (objectQueryParams.value.pageNum - 1) * objectQueryParams.value.pageSize +
    pageIndex
  );
}
function getRuleOriginIndex(row) {
  return originList.value.indexOf(row);
}

let loadingList = ref(false);
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  applyRuleFilter();
};

function applyRuleFilter() {
  dppQualityTaskEvaluateSaveReqVO.value = originList.value.filter((item) => {
    if (queryParams.value.name && !item.name.includes(queryParams.value.name))
      return false;
    if (
      queryParams.value.dimensionType &&
      item.dimensionType !== queryParams.value.dimensionType
    )
      return false;
    if (queryParams.value.publishStatus) {
      const statusVal =
        queryParams.value.publishStatus === "online" ? "1" : "0";
      if (item.status !== statusVal) return false;
    }
    return true;
  });
}
function renameRuleToRuleConfig(data, obj) {
  return data
    .filter(
      (col) => Array.isArray(col.cleanRuleList) && col.cleanRuleList.length > 0
    )
    .flatMap((col) =>
      col.cleanRuleList.map((item) => {
        let parsedRule = {};
        try {
          parsedRule = JSON.parse(item.rule || "{}");
        } catch (e) {
          console.warn(`rule JSON 解析失败: ${item.rule}`, e);
        }

        const evaColumnStr = col.columnName;

        return {
          ...item,
          id: undefined,
          warningLevel: "2",
          datasourceId: obj?.datasourceId || "",
          tableName: obj?.tableName || col.tableName,
          evaColumn: evaColumnStr,
          rule: JSON.stringify({
            ...parsedRule,
            evaColumn: evaColumnStr,
          }),
        };
      })
    );
}

async function selectInspectionRule() {
  loading.value = true;

  try {
    for (const item of dppQualityTaskObjSaveReqVO.value || []) {
      try {
        const res = await getColumnByAssetId({
          withRule: 1,
          id: item.datasourceId,
          tableName: item.tableName,
          spaceId: userStore.spaceId,
          spaceCode: userStore.spaceCode,
        });

        if (res?.data?.length) {
          const rowsWithSource = res.data.map((row) => ({
            ...row,
            datasourceId: item.datasourceId,
            tableName: item.tableName,
          }));

          const obj = renameRuleToRuleConfig(rowsWithSource, item) || [];

          let addedCount = 0;
          obj.forEach((newRule) => {
            // 规则唯一标识
            const key = `${newRule.tableName}_${newRule.evaColumn}_${newRule.ruleName}`;
            // 查找是否已存在相同规则
            const existIndex = originList.value.findIndex(
              (r) => `${r.tableName}_${r.evaColumn}_${r.ruleName}` === key
            );
            if (existIndex > -1) {
              // 覆盖
              originList.value.splice(existIndex, 1, newRule);
            } else {
              // 追加
              originList.value.push(newRule);
              addedCount++;
            }
          });
          applyRuleFilter();

          if (addedCount > 0) {
            message.success(
              `已追加 ${addedCount} 条规则，来自表 ${item.tableName}`
            );
          } else {
            // message.info(`表 ${item.tableName} 没有新规则追加`);
          }
        }
      } catch (err) {
        console.warn(
          `获取规则失败: datasourceId=${item.datasourceId}, tableName=${item.tableName}`,
          err
        );
      }
    }
  } finally {
    loading.value = false;
  }
}

const resetQuery = () => {
  queryParams.value = {
    pageNum: 1,
    pageSize: 6,
    name: "",
    dimensionType: "",
    publishStatus: "",
  };
  applyRuleFilter();
};
let deptOptions = ref([]);

let openCron = ref(false);
const expression = ref("");
/** 调度周期按钮操作 */
function handleShowCron() {
  expression.value = form.value.cycle;
  openCron.value = true;
}
/** 确定后回传值 */
async function crontabFill(value) {
  form.value.cycle = value;
  await nextTick();
  formRef.value?.validateField("cycle");
}
function getDeptTree() {
  listAttQualityCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "质量探查目录",
        value: "",
        id: 0,
      children: deptOptions.value,
      },
    ];
  });
}
const data = reactive({
  form: {
    taskName: "",
    catCode: "",
    status: "1",
    contactId: "",
    priority: "",
    workerGroup: "default",
    retryCount: 0,
    retryInterval: 0,
    delayMinutes: 0,
    description: "",
    retryTimes: "",
    delayTime: "",
    cycle: "",
    strategy: "PARALLEL",
  },
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    name: "",
    dimensionType: "",
    publishStatus: "",
  },
  stepsList: [
    { name: "基础信息", id: 0 },
    { name: "稽查对象信息", id: 1 },
    { name: "稽查规则", id: 2 },
  ],
  activeReult: 0,
  active: 0,
  loadingOptions: { loading: false },
});

const { form, stepsList, activeReult, loadingOptions, queryParams, active } =
  toRefs(data);
const formRef = ref();

const priorityOptions = ref([
  { label: "高", value: "high" },
  { label: "中", value: "medium" },
  { label: "低", value: "low" },
]);

function handleLastStep() {
  activeReult.value--;
}
const inspectionTargetDialog = ref();
const openDialog = (row, index) => {
  inspectionTargetDialog.value.openDialog(row, index);
};
function handleDelete(row) {
  const idxTable = dppQualityTaskObjSaveReqVO.value.findIndex(
    (item) => item.ruleName == row.ruleName
  );
  if (idxTable !== -1) {
    dppQualityTaskObjSaveReqVO.value.splice(idxTable, 1);
  } else {
    proxy.$message.warning("删除失败，字段未找到");
  }
}
function handleRuleDelete(row) {
  const realIndex = originList.value.indexOf(row);
  if (realIndex !== -1) {
    originList.value.splice(realIndex, 1);
  }
  applyRuleFilter();
}
async function handleNextStep() {
  try {
    await formRef.value?.validate();
  } catch (err) {
    console.warn("表单校验未通过：", err);
    message.warning("校验未通过，请检查必填项");
    loadingInstance.value = false;
    return;
  }
  activeReult.value++;
}

function Inspectionconfirm(obj, mode) {
  const index = Number(mode) - 1;
  const list = dppQualityTaskObjSaveReqVO.value;
  const isDuplicate = list.some((item, i) => {
    if (index >= 0) {
      return i != index && item.name == obj.name;
    } else {
      return item.name == obj.name;
    }
  });

  if (isDuplicate) {
    proxy.$message.warning("校验未通过，稽查对象名称不能重复");
    return;
  }

  if (!isNaN(index) && index >= 0 && index < list.length) {
    list.splice(index, 1, obj);
  } else {
    list.push(obj);
  }

  inspectionTargetDialog.value.closeDialog();
}

let ruleSelectorDialog = ref();
const openRuleSelector = (row) => {
  ruleSelectorDialog.value.openDialog(row);
};
const openRuleDialog = (row, index, falg) => {
  ruleSelectorDialog.value.openDialog(row, index, falg);
};
function RuleSelectorconfirm(obj, mode) {
  const index = Number(mode) - 1;
  const list = originList.value;
  const isDuplicate = list.some((item, i) => {
    if (index >= 0) {
      return i !== index && item.name == obj.name;
    } else {
      return item.name === obj.name;
    }
  });

  if (isDuplicate) {
    proxy.$message.warning("校验未通过，评测名称不能重复");
    return;
  }

  if (!isNaN(index) && index >= 0 && index < list.length) {
    list.splice(index, 1, obj);
  } else {
    list.push(obj);
  }

  applyRuleFilter();
  ruleSelectorDialog.value.closeDialog();
}

// 页面跳转
const handleSuccess = () => {
  router.push("/cat/quality/qualityTask");
};
async function submitForm() {
  loadingInstance.value = true;
  try {
    await formRef.value?.validate();
  } catch (err) {
    console.warn("表单校验未通过：", err);
    message.warning("校验未通过，请检查必填项");
    loadingInstance.value = false;
    return;
  }
  try {
    const payload = {
      ...form.value,
      creatorId: userStore.id,
      createBy: userStore.nickName || userStore.name,
      contactId: userStore.id,
      dppQualityTaskObjSaveReqVO: dppQualityTaskObjSaveReqVO.value,
      dppQualityTaskEvaluateSaveReqVO:
        dppQualityTaskEvaluateSaveReqVO.value,
    };
    const res = form.value.id
      ? await updateDppQualityTask({
          ...payload,
        })
      : await addDppQualityTask({
          ...payload,
        });

    // 响应处理
    if (res.code == "200") {
      proxy.$modal.msgSuccess(res.msg);
      handleSuccess();
    } else {
      message.warning(res.msg || "提交失败！");
    }
  } catch (err) {
  } finally {
    loadingInstance.value = false;
  }
}

function code(obj) {
  dppQualityTaskObjSaveReqVO.value = Array.isArray(obj) ? [...obj] : [];
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function fetchQualityTaskDetail(taskId) {
  let response = await getDppQualityTask(taskId);
  if (!response) {
    await sleep(150);
    response = await getDppQualityTask(taskId);
  }
  return response;
}

async function getDppQualityTaskinfo() {
  loadingInstance.value = true;
  const _id = id;
  try {
    const response = await fetchQualityTaskDetail(_id);
    if (!response) {
      throw new Error("详情请求被取消，请重新打开配置页");
    }
    const detail = response.data || {};
    const taskObjList =
      detail.dppQualityTaskObjSaveReqVO ||
      detail.collectorQualityTaskObjSaveReqVO ||
      detail.qualityTaskObjSaveReqVO ||
      detail.CollectorQualityTaskObjSaveReqVO ||
      detail.QualityTaskObjSaveReqVO ||
      [];
    const taskEvaluateList =
      detail.dppQualityTaskEvaluateRespVOS ||
      detail.collectorQualityTaskEvaluateRespVOS ||
      detail.qualityTaskEvaluateRespVOS ||
      detail.CollectorQualityTaskEvaluateRespVOS ||
      detail.QualityTaskEvaluateRespVOS ||
      detail.dppQualityTaskEvaluateSaveReqVO ||
      detail.collectorQualityTaskEvaluateSaveReqVO ||
      detail.qualityTaskEvaluateSaveReqVO ||
      [];
    const {
      dppQualityTaskObjSaveReqVO: _dppQualityTaskObjSaveReqVO,
      collectorQualityTaskObjSaveReqVO: _collectorQualityTaskObjSaveReqVO,
      qualityTaskObjSaveReqVO: _qualityTaskObjSaveReqVO,
      CollectorQualityTaskObjSaveReqVO: _CollectorQualityTaskObjSaveReqVO,
      QualityTaskObjSaveReqVO: _QualityTaskObjSaveReqVO,
      dppQualityTaskEvaluateRespVOS: _dppQualityTaskEvaluateRespVOS,
      collectorQualityTaskEvaluateRespVOS: _collectorQualityTaskEvaluateRespVOS,
      qualityTaskEvaluateRespVOS: _qualityTaskEvaluateRespVOS,
      CollectorQualityTaskEvaluateRespVOS: _CollectorQualityTaskEvaluateRespVOS,
      QualityTaskEvaluateRespVOS: _QualityTaskEvaluateRespVOS,
      dppQualityTaskEvaluateSaveReqVO: _dppQualityTaskEvaluateSaveReqVO,
      collectorQualityTaskEvaluateSaveReqVO: _collectorQualityTaskEvaluateSaveReqVO,
      qualityTaskEvaluateSaveReqVO: _qualityTaskEvaluateSaveReqVO,
      ...obj
    } = detail;
    originList.value = Array.isArray(taskEvaluateList)
      ? [...taskEvaluateList]
      : [];
    applyRuleFilter();
    code(taskObjList);
    Object.assign(form.value, obj);
    if (form.value.contactId != null && form.value.contactId !== "") {
      form.value.contactId = Number(form.value.contactId);
    }
  } catch (error) {
    console.error("获取质量任务失败:", error);
    const errMsg = error?.message || error || "未知错误";
    message.warning(`获取质量任务信息失败：${errMsg}`);
  } finally {
    loadingInstance.value = false;
  }
}

// 监听 id 变化
watch(
  () => route.query.id,
  (newId) => {
    id = newId; // 如果 id 为空，使用默认值 1
    if (id) {
      getDppQualityTaskinfo();
    }
  },
  { immediate: true } //
);

getDeptTree();
</script>
<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
  overflow-y: auto;
}

.pagecont-top {
  min-height: calc(100vh - 165px);
  height: auto;
  position: relative;
  padding-bottom: 20px;
}

// 质量探查新增页内容高度不固定：表单、对象列表和规则列表都可能随着
// 屏幕尺寸及数据量变化。让页面使用主内容区的滚动，不再用固定 vh 和
// 绝对定位把底部按钮压出可视区域。
.quality-task-editor {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 165px);

  .infotop {
    display: flex;
    flex: 1 1 auto;
    min-height: 0;
    flex-direction: column;
  }

  .main {
    min-height: 0;
  }
}

.steps-wrap {
  //width: 87.5vw;
  height: 80px;
  padding: 20px 20px;
  step-height: 40px;
  //box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  border: 0px solid #ebeef5;
  background-color: #fff;
  margin: 15px 15px -34px 15px;
}

.custom-card {
  width: 100%;
  height: 100px;
  padding: 34px 177px 26px 189px;
  background: #fff;
  box-sizing: border-box;
  margin-bottom: 15px;

  .steps-inner {
    padding: 0 10px;
    padding-left: 20px;
    display: flex;
    width: auto;
    color: #303133;
    transition: 0.3s;
    transform: translateZ(0);

    &::-webkit-scrollbar {
      height: 5px;
    }

    .zl-step {
      list-style: none;
      width: 100%;
      height: 20px;
      padding: 0;
      margin: 20px auto;
      cursor: pointer;
      display: flex;
      align-items: flex-end;

      li {
        position: relative;
        flex: 1;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #d7d8da;
        color: #666;
        font-weight: 500;
        transition: background 0.3s;

        &:first-child {
          z-index: 2;
          clip-path: polygon(
            0 0,
            calc(100% - 20px) 0,
            100% 50%,
            calc(100% - 20px) 100%,
            0 100%
          );
        }

        &:not(:first-child):not(:last-child) {
          margin-left: -10px;
          clip-path: polygon(
            0 0,
            calc(100% - 20px) 0,
            100% 50%,
            calc(100% - 20px) 100%,
            0 100%
          );
          z-index: 1;

          &::before {
            content: "";
            position: absolute;
            left: 0;
            top: 0;
            width: 20px;
            height: 100%;
            background: #fff;
            clip-path: polygon(0 0, 100% 50%, 0 100%);
            z-index: 2;
          }
        }

        &:last-child {
          margin-left: -10px;
          z-index: 0;
          clip-path: polygon(0 0, 100% 0, 100% 100%, 0 100%);

          &::before {
            content: "";
            position: absolute;
            left: 0;
            top: 0;
            width: 20px;
            height: 100%;
            background: #fff;
            clip-path: polygon(0 0, 100% 50%, 0 100%);
            z-index: 2;
          }
        }

        &.statusEnd {
          background: linear-gradient(270deg, #e9effe 0%, #5589fa 100%);
          color: #2666fb !important;
        }

        &.prevStep {
          background: #e9effe !important;
          font-weight: normal;
          font-size: 16px !important;
          color: #2666fb !important;
        }

        &.cur {
          background: #f1f1f5;
          color: #404040;
          font-weight: 500;
        }
      }
    }

    .step-circle {
      width: 26px;
      height: 26px;
      border-radius: 50%;
      background: #f1f1f5;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      font-weight: bold;
      margin-right: 11px;
      border: 1px solid #b2b2b2;
      flex-shrink: 0;
      transition: all 0.3s;

      &.active {
        background: #2666fb;
        color: #fff;
        border: 1px solid #fff;
      }

      &.prev {
        background: #f1f1f5 !important;
        border: 1px solid #2666fb !important;
        color: #2666fb !important;
      }
    }

    .step-name {
      font-family: PingFang SC, PingFang SC;
      font-weight: 500;
      font-size: 16px;
    }
  }
}

.button-style {
  position: sticky;
  bottom: 0;
  flex: none;
  margin-top: 20px;
  padding: 14px 35px 12px 0;
  background: #fff;
  border-top: 1px solid #edf1f6;
  text-align: right;
  z-index: 10;
}

.main {
  flex: 1;
  // margin: 15px;
  background-color: white;
  padding: 0px 25px 0;
}

.home {
  display: flex;
  flex-direction: column;
  min-height: 0;

  .clearfix {
    width: 100%;
    height: 36px;
    background-color: #f8f8f9;
    display: flex;
    align-items: center;
    padding-left: 10px;
  }

  .clearfix span {
    display: flex;
    align-items: center;
  }

  .blue-bar {
    background-color: #2666fb; // 蓝条颜色
    width: 5px; // 宽度5px
    height: 20px; // 高度20px
    margin-right: 10px; // 图片与文字之间的间距
  }
}

.option-item {
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.blue-text {
  color: var(--el-color-primary);
}

.blue-bar {
  background-color: #2666fb;
  width: 5px;
  height: 20px;
  margin-right: 10px;
  border-radius: 2px;
}

.header-text {
  margin: 20px 0;
}

.header-left {
  display: flex;
  align-items: center;
  font-size: 16px;
  line-height: 24px;
  font-style: normal;
}

.iconimg {
  width: 15px;
  height: 15px;
  font-size: 15px;
  vertical-align: middle;
}

/* 质量探查编辑页：固定外层工作区，内容区独立滚动，避免页面整体上下跳动。 */
.quality-task-page {
  height: 100%;
  min-height: 0;
  margin: 0;
  overflow: hidden;
}

:deep(.quality-page-spin) {
  height: 100%;
  min-height: 0;
}

:deep(.quality-page-spin > .ant-spin-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.custom-card {
  flex: none;
  height: 76px;
  margin-bottom: 12px;
  padding: 18px clamp(40px, 10vw, 180px);
  border: 1px solid #e8edf5;
  border-radius: 10px;
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.05);

  .steps-inner {
    height: 40px;
    padding: 0;

    .zl-step {
      height: 40px;
      margin: 0;
      align-items: stretch;
      gap: 10px;

      li,
      li:first-child,
      li:not(:first-child):not(:last-child),
      li:last-child {
        height: 40px;
        margin-left: 0;
        clip-path: none;
        border: 1px solid #e1e7ef;
        border-radius: 8px;
        background: #f6f8fb;
        color: #68758a;

        &::before {
          display: none;
        }

        &.statusEnd {
          border-color: #7da5ff;
          background: #edf3ff;
          color: #2666fb !important;
          box-shadow: 0 4px 12px rgba(38, 102, 251, 0.12);
        }

        &.prevStep {
          border-color: #c6d8ff;
          background: #f4f7ff !important;
          font-size: 14px !important;
        }
      }
    }

    .step-circle {
      width: 24px;
      height: 24px;
      margin-right: 8px;
      font-size: 14px;
    }

    .step-name {
      font-size: 14px;
    }
  }
}

.quality-task-editor {
  flex: 1 1 auto;
  min-height: 0;
  margin-bottom: 0;
  padding: 0 !important;
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.06);
}

:deep(.quality-content-spin),
:deep(.quality-content-spin > .ant-spin-container) {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.quality-task-editor .infotop {
  height: 100%;
}

.quality-task-editor .main {
  flex: 1 1 auto;
  min-height: 0;
  padding: 20px 28px;
  overflow-x: hidden;
  overflow-y: auto;
}

.quality-base-form {
  width: min(1120px, 100%);
  margin: 0 auto;

  .h2-titles {
    margin: 4px 0 18px;
    padding: 10px 14px;
    color: #1f2d3d;
    font-size: 15px;
    font-weight: 600;
    background: linear-gradient(90deg, #f3f7ff 0%, #fafcff 65%, #fff 100%);
    border-left: 4px solid #2666fb;
    border-radius: 6px;
  }

  .ant-form-item {
    margin-bottom: 18px;
  }

  .ant-input,
  .ant-input-affix-wrapper,
  .ant-input-group-wrapper,
  .ant-select,
  .ant-tree-select {
    width: 100%;
  }

  textarea.ant-input {
    min-height: 76px;
    resize: vertical;
  }
}

.button-style {
  position: relative;
  bottom: auto;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 0;
  padding: 12px 28px;
  box-shadow: 0 -5px 14px rgba(31, 45, 61, 0.04);
}

@media (max-width: 900px) {
  .custom-card {
    padding-right: 20px;
    padding-left: 20px;
  }

  .quality-task-editor .main {
    padding: 16px;
  }

  .quality-base-form {
    :deep(.ant-form-item-label) {
      width: 96px !important;
    }
  }
}
</style>

