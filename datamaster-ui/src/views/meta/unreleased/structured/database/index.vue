<template>
  <div class="app-container">

    <qt-wrap :columns="tableStroe.columns" :tableRef="tableRef">
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="tableStroe.params"
          :tableRef="tableRef"
          :config="{ permi: ['md:unreleased:structured:db:query'] }"
        />
      </template>
      <template #actions-data>
        <a-button
          type="primary"
          :icon="h(PlusOutlined)"
          @click="handleAddClick"
          v-hasPermi="['md:unreleased:structured:db:add']"
        >
          新增
        </a-button>
        <a-button
          type="primary"
          danger
          :icon="h(DeleteOutlined)"
          :disabled="!store.rows.length"
          @click="handleDeleteColumnClick"
          v-hasPermi="['md:unreleased:structured:db:remove']"
        >
          删除
        </a-button>
      </template>
      <qt-table v-bind="tableStroe" ref="tableRef">
        <template #domain-name="scope">
          {{ getDomainPath(scope.row.domainId) }}
        </template>

        <template #status="scope">
          <a-switch
            v-if="scope.row.status != undefined"
            :checked="scope.row.status === '1'"
            checked-value="1"
            un-checked-value="0"
            @change="handleStatusChange(scope.row, $event)"
          />
        </template>

        <template #handle="{ row }">
          <a-button
            type="link"
            :icon="h(EyeOutlined)"
            @click="handleDetailClick(row)"
            v-hasPermi="['md:unreleased:structured:db:detail']"
          >
            详情
          </a-button>
          <a-button
            type="link"
            :icon="h(EditOutlined)"
            :disabled="row.status == 1"
            @click="handleEditClick(row)"
            v-hasPermi="['md:unreleased:structured:db:edit']"
          >
            修改
          </a-button>
          <a-popover placement="bottom" trigger="click" :overlay-style="{ width: '107px' }">
            <template #content>
              <a-button
                type="link"
                danger
                :icon="h(DeleteOutlined)"
                :disabled="row.status == 1"
                @click="handleDeleteClick(row)"
                v-hasPermi="['md:unreleased:structured:db:remove']"
              >
                删除
              </a-button>
            </template>
            <a-button
              type="link"
              :icon="h(DownOutlined)"
              v-hasPermi="[
                'md:unreleased:structured:db:remove',
                'md:unreleased:structured:db:edit',
              ]"
            >
              更多
            </a-button>
          </a-popover>
        </template>
      </qt-table>
    </qt-wrap>

    <!-- 新增/修改弹窗 -->
    <a-modal
      v-model:open="dialog.open"
      :title="dialog.title"
      width="1200"
      destroy-on-close
    >
      <a-form
        :model="dialog.form"
        :rules="rules"
        ref="formRef"
        class="column-form"
        :label-col="{ style: { width: '110px' } }"
      >
        <a-form-item label="业务域" name="domainId">
          <a-tree-select
            show-search
            v-model:value="dialog.form.domainId"
            :tree-data="store.treeDomains"
            :field-names="{ value: 'id', label: 'name', children: 'children' }"
            placeholder="请选择业务域"
            :tree-default-expand-all="true"
            @change="handleDomainChange"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="数据连接名称" name="datasourceId">
          <a-select
            allow-clear
            v-model:value="dialog.form.datasourceId"
            placeholder="请选择数据连接名称"
            @change="handleDatasourceChange"
          >
            <a-select-option
              v-for="item in store.datasources"
              :key="item.id"
              :value="item.id"
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

        <a-form-item label="库名" name="dbName">
          <a-select
            allow-clear
            v-model:value="dialog.form.dbName"
            placeholder="请选择库名"
          >
            <a-select-option
              v-for="(item, index) in store.databases"
              :key="item.dbName + '_' + index"
              :value="item.dbName"
            >
              {{ item.dbName }}
            </a-select-option>
          </a-select>
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

        <!-- <a-form-item label="安全等级">
          <a-select
            allow-clear
            v-model:value="dialog.form.safetyLevelId"
            placeholder="请选择安全等级"
          >
            <a-select-option
              v-for="item in store.sensitiveLevels"
              :key="item.id"
              :label="item.sensitiveLevel"
              :value="item.id"
            />
          </a-select>
        </a-form-item> -->

        <a-form-item label="所属分层">
          <a-select
            allow-clear
            v-model:value="dialog.form.belongingLayer"
            placeholder="请选择所属分层"
          >
            <a-select-option
              v-for="dict in toValue(dicts.meta_dw_layers)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="所属系统">
          <a-input
            allow-clear
            v-model:value="dialog.form.belongingSystem"
            placeholder="请输入所属系统"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="状态" class="row-full">
          <a-radio-group v-model:value="dialog.form.status">
            <a-radio
              v-for="dict in toValue(dicts.meta_task_status)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="描述" class="row-full">
          <a-textarea
            v-model:value="dialog.form.description"
            placeholder="请输入描述"
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item label="备注" class="row-full">
          <a-textarea
            v-model:value="dialog.form.remark"
            placeholder="请输入备注"
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item
          label="变更说明"
          class="row-full"
          name="updateMsg"
          v-if="dialog.form.id"
        >
          <a-textarea
            v-model:value="dialog.form.updateMsg"
            placeholder="请输入变更说明"
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
            show-count
          />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="handleCancelClick">取消</a-button>
          <a-button type="primary" @click="handleConfirmClick">
            确定
          </a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="UnreleasedStructuredDatabase">
import { message, Modal } from 'ant-design-vue'
import { reactive, ref, getCurrentInstance, toValue, h } from "vue";
import { PlusOutlined, DeleteOutlined, EyeOutlined, EditOutlined, DownOutlined } from '@ant-design/icons-vue';

import { listDomain } from "@/api/tax/domain/domain.js";

import {
  listDb,
  addDb,
  updateDb,
  getDb,
  updateDbStatus,
  delDb,
  batchDeleteCheck,
} from "@/api/cat/unreleased/db.js";

import { getParentLabelPath } from "@/utils/anivia.js";

import { listDaDatasource } from "@/api/cat/dataSource/dataSource";

import { useRoute, useRouter } from "vue-router";

import { listDgSensitiveLevel } from "@/api/cat/compliance/sensitiveLevel";

import { getRealtimeMcTaskScopeList } from "@/api/cat/task/task.js";

// 表单验证规则
const rules = {
  domainId: [{ required: true, message: "请选择业务域", trigger: "change" }],
  datasourceId: [
    { required: true, message: "请选择数据连接名称", trigger: "change" },
  ],
  dbName: [
    { required: true, message: "请选择库名", trigger: ["blur", "change"] },
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
  updateMsg: [
    { required: true, message: "请输入变更说明", trigger: ["change", "blur"] },
  ],
};

const DEFAULT_FORM = {
  status: "0",
};

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "datasource_type",
  "meta_task_status",
  "meta_dw_layers"
);

const router = useRouter();
const route = useRoute();

const formRef = ref();
const store = reactive({
  rows: [],
  treeDomains: [],
});

// 列表
const tableRef = ref(null);
const tableStroe = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: { prop: "createTime", order: "descending" },
      onSelectionChange: function (rows) {
        store.rows = rows;
      },
      onRowDblclick: handleDetailClick,
    },
  },
  columns: [
    {
      type: "selection",
      width: 55,
      // selectable: function (row) {
      //     return row.status === '0' ? true : false;
      // }
    },
    {
      label: "编号",
      prop: "id",
      sortable: true,
      width: 90,
    },
    {
      label: "库名",
      prop: "dbName",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 240,
      align: "left",
      link: {
        external: handleDetailClick,
      },
    },
    {
      label: "描述",
      prop: "description",
      align: "left",
      width: 240,
      showOverflowTooltip: {
        effect: "light",
      },
    },
    {
      label: "业务域",
      prop: "domainId",
      slot: "domain-name",
      width: 240,
      showOverflowTooltip: {
        effect: "light",
      },
    },
    {
      label: "数据库类型",
      prop: "dbType",
      dict: "datasource_type",
      width: 90,
    },
    {
      label: "质量探查",
      prop: "dataQuality",
      width: 90,
      sortable: true,
    },
    {
      label: "表数量",
      prop: "tableCount",
      sortable: true,
      width: 90,
    },
    {
      label: "版本号",
      prop: "version",
      width: 90,
    },
    {
      label: "状态",
      prop: "status",
      width: 90,
      slot: "status",
    },

    {
      label: "更新人",
      prop: "updateBy",
      width: 120,
    },
    {
      label: "更新时间",
      prop: "updateTime",
      sortable: true,
      width: 160,
      date: true,
    },
    {
      label: "创建人",
      prop: "createBy",
      width: 120,
    },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      width: 160,
      date: true,
    },
    {
      label: "操作",
      width: 220,
      fixed: "right",
      slot: "handle",
    },
  ],
  func: listDb,
  params: {
    dataType: 1,
  },
  events: {
    formatData: function (data) {
      data.forEach((item) => {
        item.version = proxy.formatVersion(item.version);
      });
      return data;
    },
  },
});

// 搜索项
const searchStore = reactive({
  items: [
    {
      label: "库名",
      prop: "dbName",
      component: {
        is: "input",
      },
    },
    {
      label: "数据库类型",
      prop: "dbType",
      component: {
        is: "select",
        options: dicts.datasource_type,
      },
    },
    {
      label: "业务域",
      prop: "domainCode",
      component: {
        is: "tree-select",
        filterable: true,
        data: store.treeDomains,
        props: { value: "code", label: "name", children: "children" },
        valueKey: "id",
        checkStrictly: true,
        defaultExpandAll: true,
      },
    },
  ],
});

// 新增/修改弹窗
const dialog = reactive({
  open: false,
  title: "",
  form: { ...DEFAULT_FORM },
});

// 获取业务域路径
const getDomainPath = computed(() => {
  return function (id) {
    let domainName = getParentLabelPath(store.treeDomains, id, {
      idKey: "id",
      labelKey: "name",
      childrenKey: "children",
    });
    const idx = domainName.indexOf("/");
    return idx == -1 ? domainName : domainName.slice(idx + 1);
  };
});

// 获取业务域列表
function getDomains() {
  listDomain().then((res) => {
    store.domains = [...res.data];
    store.treeDomains.splice(0, store.treeDomains.length);
    const domains = {
      id: 0,
      code: 0,
      name: "顶级节点",
      children: proxy.handleTree(res.data, "id", "parentId"),
    };
    store.treeDomains.push(domains);
  });
}

// 获取数据源列表
function getDatasources() {
  listDaDatasource().then((res) => {
    res.data.rows.forEach((item) => {
      item.datasourceConfig = item.datasourceConfig
        ? JSON.parse(item.datasourceConfig)
        : {};
    });
    store.datasources = res.data.rows;
  });
}

// 获取安全等级
function getSensitiveLevel() {
  listDgSensitiveLevel({ pageSize: 1000 }).then((res) => {
    store.sensitiveLevels = res.data.rows;
  });
}

// 切换数据源
function handleDatasourceChange(id) {
  const data = store.datasources.find((item) => item.id === id);
  dialog.form.ip = data.ip;
  dialog.form.port = data.port;
  dialog.form.username = data.datasourceConfig?.username;
  dialog.form.dbType = data.datasourceType;
  getRealtimeMcTaskScopeList(id).then((res) => {
    store.databases = res.data;
    dialog.form.dbName = "";
  });
}

// 切换业务域
function handleDomainChange(id) {
  const data = store.domains.find((item) => item.id === id);
  dialog.form.domainCode = data.code;
}

// 取消新增/修改
function handleCancelClick() {
  formRef.value.resetFields();
  dialog.form = {
    ...DEFAULT_FORM,
  };
  dialog.loading = false;
  dialog.open = false;
}

// 确认新增/修改
function handleConfirmClick() {
  formRef.value
    .validate()
    .then(() => {
      if (dialog.form.safetyLevelId == undefined) {
        dialog.form.safetyLevelId = null;
        dialog.form.safetyLevelName = null;
      }
      return dialog.func(dialog.form);
    })
    .then(() => {
      proxy.$modal.msgSuccess(`${dialog.form.id ? "修改" : "新增"}库元数据成功！`);
      handleCancelClick();
      tableRef.value.getList();
    })
    .catch(() => {});
}

// 点击新增
function handleAddClick() {
  dialog.title = "新增库元数据";
  dialog.open = true;
  dialog.func = addDb;
}

// 打开修改弹窗
function handleEditClick(row) {
  dialog.open = true;
  dialog.func = updateDb;
  dialog.title = "修改任务";
  getDb(row.id).then((res) => {
    const {
      createBy,
      createTime,
      delFlag,
      updateBy,
      updateTime,
      updaterId,
      validFlag,
      auditTime,
      ...form
    } = res.data;
    dialog.form = form;
    handleDatasourceChange(res.data.datasourceId);
  });
}

// 切换状态
function handleStatusChange(row, status) {
  Modal.confirm({
    title: "系统提示",
    content: `是否确认${status == 1 ? "发布" : "取消发布"}数据编号为${
      row.id
    }的库元数据吗？`,
    okText: "确定",
    cancelText: "取消",
    onOk: async () => {
      try {
        await updateDbStatus({
          id: row.id,
          status,
        });
        message.success(
          `编号为${row.id}的库元数据${status == 1 ? "发布" : "取消发布"}成功!`
        );
        row.status = status;
      } catch (error) {
        row.status = status == "1" ? "0" : "1";
      }
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
        await delDb(canDeleteIds.toString());
        message.success("删除成功");
        tableRef.value.getList();
      },
    });
  });
}

// 点击详情
function handleDetailClick(row) {
  router.push({
    path: route.path + "/detail",
    query: {
      id: row.id,
      table_status: 1,
    },
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
      await delDb(row.id);
      message.success("删除成功");
      tableRef.value.getList();
    },
  });
}

// getDomains();
// getSensitiveLevel();
getDatasources();
</script>

