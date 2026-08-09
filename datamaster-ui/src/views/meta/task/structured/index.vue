<template>
  <div class="app-container dpp-task-list-page" ref="app-container">

    <a-layout>
      <SourceSystemTree
        ref="sourceSystemTreeRef"
        @node-click="handleNodeClick"
        @data-loaded="handleTreeDataLoaded"
      />
      <a-layout-content class="main-content">
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
            <a-button type="primary" @click="handleAddClick">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
            <a-button
              type="danger"
              :icon="h(DeleteOutlined)"
              :disabled="!store.rows.length"
              @click="handleDeleteColumnClick"
            >
              删除
            </a-button>
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
                  <a-typography-link
                    class="task-name-text task-name-ellipsis"
                    :title="row.name"
                  >
                    {{ row.name || "-" }}
                  </a-typography-link>
                  <a-tag
                    class="task-cat-ellipsis"
                    :title="row.sourceSystemName"
                  >
                    {{ row.sourceSystemName || "-" }}
                  </a-tag>
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
                  <a-tag :color="row.status == '1' ? 'success' : 'warning'">
                    {{ row.status == "1" ? "已发布" : "未发布" }}
                  </a-tag>
                </div>
                <div class="flex-center">
                  <span class="black-label mr5">调度状态:</span>
                  <a-tag :color="row.schedulerStatus == '1' ? 'success' : 'default'">
                    {{ row.schedulerStatus == "1" ? "已上线" : "未上线" }}
                  </a-tag>
                </div>
              </div>
            </template>
            <template #cronExpression="{ row }">
              <div class="flex-column fz14 grey-black-text">
                <div class="flex-center">
                  <ClockCircleOutlined class="mr5" />
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
                    <a-tag class="not-executed-tag">未执行</a-tag>
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
              </div>
            </template>
            <template #action="{ row }">
              <div class="task-actions-col">
                <div class="action-row">
                  <a-button
                    type="link"
                    :icon="h(EditOutlined)"
                    :disabled="row.status == '1'"
                    @click="handleEditClick(row)"
                  >修改</a-button>
                  <a-button
                    type="link"
                    :icon="h(EyeOutlined)"
                    @click="handleDetailClick(row)"
                  >详情</a-button>
                  <a-button
                    type="link"
                    danger
                    :icon="h(DeleteOutlined)"
                    :disabled="row.status == '1'"
                    @click="handleDeleteClick(row)"
                  >删除</a-button>
                </div>
                <div class="action-row">
                  <a-button
                    type="link"
                    style="color: #52c41a"
                    :icon="h(UploadOutlined)"
                    :disabled="row.status == '1'"
                    :loading="publishingId === row.id"
                    @click="handlePublishClick(row)"
                  >发布</a-button>
                  <a-button
                    type="link"
                    style="color: #faad14"
                    :icon="h(DownloadOutlined)"
                    :disabled="row.status != '1'"
                    :loading="unpublishingId === row.id"
                    @click="handleUnpublishClick(row)"
                  >卸载</a-button>
                  <a-button
                    type="link"
                    :icon="h(PlayCircleOutlined)"
                    :disabled="row.status != '1'"
                    @click="handleRunClick(row)"
                  >执行一次</a-button>
                </div>
              </div>
            </template>
          </qt-table>
        </qt-wrap>
      </a-layout-content>
    </a-layout>

    <!-- 调度周期弹窗 -->
    <a-modal
      title="Cron表达式生成器"
      v-model:open="cronDialog.open"
      destroy-on-close
      :footer="null"
    >
      <Crontab
        @hide="handleCloseCronClick"
        @fill="handleConfirmCronClick"
        :expression="cronDialog.data"
      />
    </a-modal>

    <!-- 新增/修改弹窗 -->
    <a-modal
      v-model:open="dialog.open"
      :title="dialog.title"
      width="1200"
      @cancel="handleCancelClick"
    >
      <a-tabs v-model:activeKey="dialog.activeTab" class="task-form-tabs">
        <a-tab-pane :tab="'基础配置'" key="base">
          <a-form
            :model="dialog.form"
            class="column-form"
            :rules="rules"
            ref="formRef"
            :label-col="{ style: { width: '110px' } }"
          >
        <a-form-item label="来源系统" name="sourceSystemId">
          <a-tree-select
            show-search
            v-model:value="dialog.form.sourceSystemId"
            :tree-data="store.sourceSystems"
            :field-names="{ value: 'id', label: 'name', children: 'children' }"
            placeholder="请选择来源系统"
            @change="handleDomainChange"
            :tree-default-expand-all="true"
          />
        </a-form-item>

        <a-form-item label="任务名称" name="name">
          <a-input v-model:value="dialog.form.name" placeholder="请输入任务名称" />
        </a-form-item>

        <a-form-item label="数据连接名称" name="datasourceId">
          <a-select
            v-model:value="dialog.form.datasourceId"
            placeholder="请选择数据连接名称"
            @change="handleDatasourceChange"
          >
            <a-select-option
              v-for="item in store.datasources"
              :key="item.id"
              :value="item.id"
              :disabled="
                !COLLECT_DATASOURCE_TYPES.includes(item.datasourceType)
              "
            >
              {{ item.datasourceName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="数据库类型" name="dbType">
          <a-input
            v-model:value="dialog.form.dbType"
            disabled
            placeholder="请输入数据库类型"
          />
        </a-form-item>

        <a-form-item label="IP" name="ip">
          <a-input v-model:value="dialog.form.ip" disabled placeholder="请输入ip" />
        </a-form-item>

        <a-form-item label="端口号" name="port">
          <a-input
            v-model:value="dialog.form.port"
            disabled
            placeholder="请输入端口号"
          />
        </a-form-item>

        <a-form-item label="账号" name="username">
          <a-input
            v-model:value="dialog.form.username"
            disabled
            placeholder="请输入账号"
          />
        </a-form-item>

        <qt-form-item
          label="调度周期"
          name="cronExpression"
          :tip="{
            content: '支持Cron表达式，如 0 0 * * * 表示每天0点执行，不填则不启用定时调度',
          }"
        >
          <a-input
            v-model:value="dialog.form.cronExpression"
            placeholder="请配置调度周期"
          >
            <template #append>
              <a-button
                type="primary"
                @click="handleOpenCronClick"
                style="background-color: #2666fb; color: #fff"
              >
                配置
                <ClockCircleOutlined />
              </a-button>
            </template>
          </a-input>
        </qt-form-item>

        <a-form-item
          label="采集模式"
          class="row-full"
          name="collectionMode"
          v-if="false"
        >
          <a-radio-group v-model:value="dialog.form.collectionMode">
            <a-radio
              v-for="dict in toValue(dicts.mc_collect_mode)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="采集范围" class="row-full" name="collectionScope">
          <div class="collection-wrap">
            <a-radio-group v-model:value="dialog.form.collectionScope">
              <a-radio
                v-for="dict in toValue(dicts.mc_collect_scope)"
                :key="dict.value"
                :value="dict.value"
              >
                {{ dict.label }}
              </a-radio>
            </a-radio-group>

            <a-form-item
              name="tables"
              v-if="dialog.form.collectionScope == 1"
              label=""
              style="margin-bottom: 0"
            >
              <a-transfer
                v-model:target-keys="dialog.form.tables"
                :data-source="dialog.tableList"
                :row-key="(record) => record.dbName"
                :render="(item) => item.label"
                show-search
                :filter-option="onFilterTransfer"
                :titles="['来源库', '已选来源库']"
                :list-style="{ width: '320px', height: 'auto' }"
              />
            </a-form-item>
          </div>
        </a-form-item>

        <a-form-item label="描述" class="row-full" name="description">
          <a-textarea
            v-model:value="dialog.form.description"
            placeholder="请输入描述"
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item label="备注" class="row-full" name="remark">
          <a-textarea
            v-model:value="dialog.form.remark"
            placeholder="请输入备注"
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
            show-count
          />
        </a-form-item>
          </a-form>
        </a-tab-pane>
        <a-tab-pane :tab="'质量规则'" key="quality" :disabled="!dialog.form.id">
          <div v-if="dialog.form.id" class="quality-tab">
            <!-- 稽查对象 -->
            <div class="quality-section">
              <div class="quality-section-header">
                <span class="quality-section-title">稽查对象</span>
                <a-button type="primary" size="small" :icon="h(PlusOutlined)" @click="openObjDialog()">添加稽查对象</a-button>
              </div>
              <a-table :data-source="dialog.form.qualityObjs" striped bordered size="small" :pagination="false" :locale="{ emptyText: '暂无稽查对象' }" :columns="[
                { title: '对象名称', dataIndex: 'name', ellipsis: true },
                { title: '数据源', dataIndex: 'datasourceId', width: 120, align: 'center' },
                { title: '表名', dataIndex: 'tableName', ellipsis: true },
                { title: '操作', key: 'actions', width: 120, align: 'center' },
              ]">
                <template #bodyCell="{ column, record, index }">
                  <template v-if="column.dataIndex === 'datasourceId'">
                    {{ getDatasourceName(record.datasourceId) }}
                  </template>
                  <template v-if="column.key === 'actions'">
                    <a-button type="link" size="small" @click="openObjDialog(record, index)">编辑</a-button>
                    <a-button type="link" danger size="small" @click="removeObj(index)">删除</a-button>
                  </template>
                </template>
              </a-table>
            </div>

            <!-- 评测规则 -->
            <div class="quality-section" style="margin-top: 16px;">
              <div class="quality-section-header">
                <span class="quality-section-title">评测规则</span>
                <a-button type="primary" size="small" :icon="h(PlusOutlined)" @click="openEvalDialog()">添加评测规则</a-button>
              </div>
              <a-table :data-source="dialog.form.qualityEvaluates" striped bordered size="small" :pagination="false" :locale="{ emptyText: '暂无评测规则' }" :columns="[
                { title: '规则名称', dataIndex: 'name', ellipsis: true },
                { title: '规则编号', dataIndex: 'ruleCode', width: 120, ellipsis: true },
                { title: '表名', dataIndex: 'tableName', ellipsis: true },
                { title: '检查字段', dataIndex: 'evaColumn', width: 120, ellipsis: true },
                { title: '告警等级', dataIndex: 'warningLevel', width: 100, align: 'center' },
                { title: '操作', key: 'actions', width: 120, align: 'center' },
              ]">
                <template #bodyCell="{ column, record, index }">
                  <template v-if="column.key === 'actions'">
                    <a-button type="link" size="small" @click="openEvalDialog(record, index)">编辑</a-button>
                    <a-button type="link" danger size="small" @click="removeEval(index)">删除</a-button>
                  </template>
                </template>
              </a-table>
            </div>
          </div>
          <a-empty v-else description="请先保存基础配置，再配置质量规则" />
        </a-tab-pane>
      </a-tabs>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="handleCancelClick">取消</a-button>
          <a-button type="primary" @click="handleConfirmClick">
            确定
          </a-button>
        </div>
      </template>
    </a-modal>

    <!-- 稽查对象编辑弹窗 -->
    <a-modal v-model:open="objDialog.open" :title="objDialog.title" width="600" destroy-on-close>
      <a-form ref="objFormRef" :model="objDialog.form" :rules="objRules" :label-col="{ style: { width: '120px' } }">
        <a-form-item label="对象名称" name="name">
          <a-input v-model:value="objDialog.form.name" placeholder="请输入稽查对象名称" />
        </a-form-item>
        <a-form-item label="数据源" name="datasourceId">
          <a-select v-model:value="objDialog.form.datasourceId" placeholder="请选择数据源" show-search @change="onObjDatasourceChange">
            <a-select-option v-for="ds in store.datasources" :key="ds.id" :value="ds.id">{{ ds.datasourceName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="表名" name="tableName">
          <a-select v-model:value="objDialog.form.tableName" placeholder="请选择表" show-search :loading="objDialog.tableLoading">
            <a-select-option v-for="t in objDialog.tableOptions" :key="t.tableName" :value="t.tableName">{{ t.tableName }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="objDialog.open = false">取消</a-button>
        <a-button type="primary" @click="confirmObjDialog">确定</a-button>
      </template>
    </a-modal>

    <!-- 评测规则编辑弹窗 -->
    <a-modal v-model:open="evalDialog.open" :title="evalDialog.title" width="700" destroy-on-close>
      <a-form ref="evalFormRef" :model="evalDialog.form" :rules="evalRules" :label-col="{ style: { width: '120px' } }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="规则名称" name="name">
              <a-input v-model:value="evalDialog.form.name" placeholder="请输入规则名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="规则编号" name="ruleCode">
              <a-input v-model:value="evalDialog.form.ruleCode" placeholder="请输入规则编号" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="关联稽查对象" name="objId">
              <a-select v-model:value="evalDialog.form.objId" placeholder="请选择稽查对象" @change="onEvalObjChange">
                <a-select-option v-for="obj in dialog.form.qualityObjs" :key="obj.id || obj._tempId" :value="obj.id || obj._tempId">{{ obj.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="告警等级" name="warningLevel">
              <a-select v-model:value="evalDialog.form.warningLevel" placeholder="请选择告警等级">
                <a-select-option value="1">低</a-select-option>
                <a-select-option value="2">中</a-select-option>
                <a-select-option value="3">高</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="检查表名" name="tableName">
              <a-input v-model:value="evalDialog.form.tableName" placeholder="自动带出" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检查字段" name="evaColumn">
              <a-input v-model:value="evalDialog.form.evaColumn" placeholder="多个字段逗号隔开" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="规则类型" name="ruleType">
          <a-input v-model:value="evalDialog.form.ruleType" placeholder="如：not_null, unique, length等" />
        </a-form-item>
        <a-form-item label="规则配置" name="rule">
          <a-textarea v-model:value="evalDialog.form.rule" :rows="3" placeholder='JSON格式，如 {"min":1,"max":100}' />
        </a-form-item>
        <a-form-item label="where条件" name="whereClause">
          <a-textarea v-model:value="evalDialog.form.whereClause" :rows="2" placeholder="可选，SQL where条件" />
        </a-form-item>
        <a-form-item label="规则描述" name="ruleDescription">
          <a-textarea v-model:value="evalDialog.form.ruleDescription" :rows="2" placeholder="规则描述" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="evalDialog.open = false">取消</a-button>
        <a-button type="primary" @click="confirmEvalDialog">确定</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="McTaskStructured">
import { message, Modal } from 'ant-design-vue'
import {
  ClockCircleOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  UploadOutlined,
} from "@ant-design/icons-vue";
import { useRouter } from "vue-router";

import {
  reactive,
  ref,
  getCurrentInstance,
  toValue,
  h,
} from "vue";

import Crontab from "@/components/Crontab/index.vue";

import SourceSystemTree from "./components/SourceSystemTree.vue";

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
  runJobOnce,
  batchDeleteCheck,
} from "@/api/cat/task/task";

import { listDaDatasource } from "@/api/cat/dataSource/dataSource";

import { getTablesByDataSourceId } from "@/api/col/task/index.js";

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

const DETAIL_PATH = "/meta/task/detail";

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
  qualityObjs: [],
  qualityEvaluates: [],
};
const dialog = reactive({
  open: false,
  title: "",
  loading: false,
  activeTab: "base",
  tableList: [],
  form: {
    ...DEFAULT_FORM,
  },
});

// ==================== 质量规则内嵌编辑 ====================

// 稽查对象编辑弹窗
const objDialog = reactive({
  open: false,
  title: "",
  editIndex: -1,
  tableLoading: false,
  tableOptions: [],
  form: { name: "", datasourceId: null, tableName: "" },
});
const objRules = {
  name: [{ required: true, message: "请输入对象名称", trigger: "blur" }],
  datasourceId: [{ required: true, message: "请选择数据源", trigger: "change" }],
  tableName: [{ required: true, message: "请选择表", trigger: "change" }],
};
const objFormRef = ref();

function getDatasourceName(id) {
  const ds = store.datasources.find((d) => d.id === id);
  return ds ? ds.datasourceName : id;
}

function openObjDialog(row, index) {
  objDialog.editIndex = index !== undefined ? index : -1;
  objDialog.title = index !== undefined ? "编辑稽查对象" : "新增稽查对象";
  objDialog.form = row ? JSON.parse(JSON.stringify(row)) : { name: "", datasourceId: null, tableName: "" };
  objDialog.tableOptions = [];
  if (objDialog.form.datasourceId) {
    loadObjTables(objDialog.form.datasourceId);
  }
  objDialog.open = true;
}

async function loadObjTables(datasourceId) {
  objDialog.tableLoading = true;
  try {
    const res = await getTablesByDataSourceId({ datasourceId });
    objDialog.tableOptions = res.code == "200" ? res.data : [];
  } finally {
    objDialog.tableLoading = false;
  }
}

function onObjDatasourceChange(id) {
  objDialog.form.tableName = "";
  objDialog.tableOptions = [];
  if (id) loadObjTables(id);
}

function confirmObjDialog() {
  objFormRef.value
    .validate()
    .then(() => {
      const data = JSON.parse(JSON.stringify(objDialog.form));
      if (objDialog.editIndex >= 0) {
        dialog.form.qualityObjs[objDialog.editIndex] = data;
      } else {
        data._tempId = Date.now() + Math.random();
        dialog.form.qualityObjs.push(data);
      }
      objDialog.open = false;
    })
    .catch(() => {});
}

function removeObj(index) {
  dialog.form.qualityObjs.splice(index, 1);
}

// 评测规则编辑弹窗
const evalDialog = reactive({
  open: false,
  title: "",
  editIndex: -1,
  form: {},
});
const evalRules = {
  name: [{ required: true, message: "请输入规则名称", trigger: "blur" }],
  objId: [{ required: true, message: "请选择稽查对象", trigger: "change" }],
};
const evalFormRef = ref();

function openEvalDialog(row, index) {
  evalDialog.editIndex = index !== undefined ? index : -1;
  evalDialog.title = index !== undefined ? "编辑评测规则" : "新增评测规则";
  evalDialog.form = row ? JSON.parse(JSON.stringify(row)) : {
    name: "", ruleCode: "", objId: null, warningLevel: "", tableName: "",
    evaColumn: "", ruleType: "", rule: "", whereClause: "", ruleDescription: "",
  };
  evalDialog.open = true;
}

function onEvalObjChange(objId) {
  const obj = dialog.form.qualityObjs.find((o) => (o.id || o._tempId) === objId);
  if (obj) {
    evalDialog.form.tableName = obj.tableName;
    evalDialog.form.datasourceId = obj.datasourceId;
    evalDialog.form.objName = obj.name;
  }
}

function confirmEvalDialog() {
  evalFormRef.value
    .validate()
    .then(() => {
      const data = JSON.parse(JSON.stringify(evalDialog.form));
      if (evalDialog.editIndex >= 0) {
        dialog.form.qualityEvaluates[evalDialog.editIndex] = data;
      } else {
        dialog.form.qualityEvaluates.push(data);
      }
      evalDialog.open = false;
    })
    .catch(() => {});
}

function removeEval(index) {
  dialog.form.qualityEvaluates.splice(index, 1);
}

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
  } catch {
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
      message.success("执行成功");
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
  dialog.activeTab = "base";
  getAllSourceSystems();
  getDatasources();
}

function applyCurrentUserAsCreator(target) {
  target.creatorId = userStore.id;
  target.createBy = userStore.nickName || userStore.name;
}

// 取消新增/修改
function handleCancelClick() {
  formRef.value.resetFields();
  dialog.form = {
    ...DEFAULT_FORM,
  };
  dialog.tableList = [];
  dialog.loading = false;
  dialog.activeTab = "base";
  dialog.open = false;
}

// 确认新增/修改
function handleConfirmClick() {
  formRef.value
    .validate()
    .then(async () => {
      dialog.loading = true;
      const { tables, ...params } = dialog.form;
      applyCurrentUserAsCreator(params);
      if (params.collectionScope == "1") {
        params.scopeSaveReqVOS = dialog.tableList.filter((item) =>
          tables.includes(item.dbName)
        );
      }
      // 确保质量规则数据随任务一起提交
      params.qualityObjs = dialog.form.qualityObjs || [];
      params.qualityEvaluates = dialog.form.qualityEvaluates || [];
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
    })
    .catch(() => {});
}

// 打开修改弹窗
function handleEditClick(row) {
  dialog.open = true;
  dialog.func = updateTask;
  dialog.title = "修改任务";
  dialog.activeTab = "base";
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
    // 确保质量规则数据初始化
    if (!res.data.qualityObjs) res.data.qualityObjs = [];
    if (!res.data.qualityEvaluates) res.data.qualityEvaluates = [];
    dialog.form = res.data;
    handleDatasourceChange(res.data.datasourceId, false);
  });
}

// 删除
function handleDeleteClick(row) {
  Modal.confirm({
    title: "系统提示",
    content: `是否确认删除编号为${row.id}的数据项？`,
    okText: "确定",
    cancelText: "取消",
    onOk: async () => {
      await delTask(row.id);
      message.success("删除成功");
      tableRef.value.getList();
    },
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
    Modal.confirm({
      title: "系统提示",
      content: `可删除${canDeleteCount}个，不可删除${cannotDeleteCount}个，是否删除可删部分`,
      okText: "确定",
      cancelText: "取消",
      onOk: async () => {
        if (!canDeleteIds.length) {
          message.success("删除成功");
          return;
        }
        await delTask(canDeleteIds);
        message.success("删除成功");
        tableRef.value.getList();
      },
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
    .ant-btn {
      font-size: 12px;
      padding: 0 2px;
    }
  }
}

.quality-tab {
  .quality-section {
    margin-bottom: 8px;
  }
  .quality-section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }
  .quality-section-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }
}
</style>

