<template>
  <div class="app-container">

    <dm-wrap :columns="tableStroe.columns" :tableRef="tableRef">
      <template #search>
        <dm-search-bar
          v-bind="searchStore"
          :params="tableStroe.params"
          :tableRef="tableRef"
          :config="{ permi: ['cat:column:query'] }"
        />
      </template>
      <template #actions-data>
        <a-button
          type="primary"
          :icon="h(PlusOutlined)"
          @click="handleAddClick"
          v-hasPermi="['cat:column:add']"
        >
          新增
        </a-button>

        <a-button
          type="primary"
          danger
          :icon="h(DeleteOutlined)"
          :disabled="!store.rows.length"
          @click="handleDeleteColumnClick"
        >
          删除
        </a-button>
      </template>
      <dm-table v-bind="tableStroe" ref="tableRef">
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
            :icon="h(EditOutlined)"
            :disabled="row.status == 1"
            @click="handleEditClick(row)"
          >
            修改
          </a-button>
          <a-popover placement="bottom" trigger="click" :overlay-style="{ width: '120px' }">
            <template #content>
              <a-button
                type="link"
                danger
                :icon="h(DeleteOutlined)"
                :disabled="row.status == 1"
                @click="handleDeleteClick(row)"
              >
                删除
              </a-button>
            </template>
            <a-button
              type="link"
              :icon="h(DownOutlined)"
              v-hasPermi="[
                'cat:column:edit',
                'cat:column:remove',
                'cat:column:detail',
              ]"
            >
              更多
            </a-button>
          </a-popover>
        </template>
      </dm-table>
    </dm-wrap>

    <a-modal
      v-model:open="dialog.open"
      title="修改字段元数据"
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
        <a-form-item label="字段名称" name="columnName">
          <a-input
            allow-clear
            v-model:value="dialog.form.columnName"
            placeholder="请输入字段注释"
          />
        </a-form-item>
        <a-form-item label="字段注释" name="columnComment">
          <a-input
            allow-clear
            v-model:value="dialog.form.columnComment"
            placeholder="请输入字段注释"
          />
        </a-form-item>
        <!-- <a-form-item label="安全等级" name="safetyLevelId">
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
        <!-- <a-form-item label="标准数据元" name="dataElemId">
          <a-select
            allow-clear
            v-model:value="dialog.form.dataElemId"
            placeholder="请选择标准数据元"
          >
            <a-select-option
              v-for="item in store.dataElemList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </a-select>
        </a-form-item> -->
        <a-form-item label="字段类型" name="columnType">
          <a-select
            allow-clear
            v-model:value="dialog.form.columnType"
            placeholder="请选择字段类型"
          >
            <a-select-option
              v-for="dict in toValue(dicts.column_type)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="字段长度" name="columnLength">
          <a-input-number
            :min="0"
            v-model:value="dialog.form.columnLength"
            placeholder="请输入字段长度"
            class="number-input"
          />
        </a-form-item>

        <a-form-item label="字段精度" name="columnPrecision">
          <a-input-number
            :min="0"
            v-model:value="dialog.form.columnPrecision"
            placeholder="请输入字段精度"
            class="number-input"
          />
        </a-form-item>

        <a-form-item label="字段小数位" name="columnScale">
          <a-input-number
            :min="0"
            v-model:value="dialog.form.columnScale"
            placeholder="请输入字段小数位"
            class="number-input"
          />
        </a-form-item>

        <a-form-item label="业务定义" name="businessDefinition">
          <a-input
            allow-clear
            v-model:value="dialog.form.businessDefinition"
            placeholder="请输入业务定义"
          />
        </a-form-item>

        <a-form-item label="度量单位" name="measuringUnit">
          <a-input
            allow-clear
            v-model:value="dialog.form.measuringUnit"
            placeholder="请输入度量单位"
          />
        </a-form-item>

        <a-form-item label="是否必填" name="nullableFlag">
          <a-radio-group v-model:value="dialog.form.nullableFlag">
            <a-radio
              v-for="dict in toValue(dicts.table_yes_no)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="默认值" name="defaultValue">
          <a-input
            allow-clear
            v-model:value="dialog.form.defaultValue"
            placeholder="请输入默认值"
          />
        </a-form-item>

        <a-form-item label="是否主键" name="pkFlag">
          <a-radio-group v-model:value="dialog.form.pkFlag">
            <a-radio
              v-for="dict in toValue(dicts.table_yes_no)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="是否外键" name="fkFlag">
          <a-radio-group v-model:value="dialog.form.fkFlag">
            <a-radio
              v-for="dict in toValue(dicts.table_yes_no)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="状态" name="status">
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

        <a-form-item label="备注" class="row-full">
          <a-textarea
            v-model:value="dialog.form.remark"
            placeholder="请输入备注"
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
            show-count
          />
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

        <a-form-item label="变更说明" class="row-full" name="updateMsg">
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

<script setup name="CatalogColumn">
import { message, Modal } from 'ant-design-vue'
import { getCurrentInstance, h, reactive, ref, toValue } from "vue";
import { PlusOutlined, DeleteOutlined, EditOutlined, DownOutlined } from '@ant-design/icons-vue';

import { listDomain } from "@/api/tax/domain/domain.js";

import { getParentLabelPath } from "@/utils/anivia.js";

import {
  listColumn,
  delColumn,
  getColumn,
  updateColumn,
  updateColumnStatus,
  batchDeleteCheck,
} from "@/api/cat/catalog/column.js";

import { listDb } from "@/api/cat/catalog/db";

import { listTable } from "@/api/cat/catalog/table";

import { useRoute, useRouter } from "vue-router";

import { listDgSensitiveLevel } from "@/api/cat/compliance/sensitiveLevel";

const rules = {
  columnName: [
    { required: true, message: "请输入字段名称", trigger: "change" },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
      message: "字段名称必须以字母开头，可包含字母、数字和下划线",
      trigger: "blur",
    },
  ],
  columnType: [
    { required: true, message: "请选择字段类型", trigger: "change" },
  ],
  columnLength: [
    { required: true, message: "请输入字段长度", trigger: ["change", "blur"] },
  ],
  updateMsg: [
    { required: true, message: "请输入变更说明", trigger: ["change", "blur"] },
  ],
  businessDefinition: [
    { required: true, message: "请输入业务定义", trigger: "blur" },
  ],
  columnScale: [
    {
      required: true,
      message: "请输入字段小数位",
      trigger: ["change", "blur"],
    },
  ],
  columnPrecision: [
    { required: true, message: "请输入字段精度", trigger: ["change", "blur"] },
  ],
  pkFlag: [{ required: true, message: "请选择是否主键", trigger: "change" }],
  fkFlag: [{ required: true, message: "请选择是否外键", trigger: "change" }],
  nullableFlag: [
    { required: true, message: "请选择是否可空", trigger: "change" },
  ],
};

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "meta_task_status",
  "meta_dw_layers",
  "table_yes_no",
  "column_type"
);

const router = useRouter();
const route = useRoute();

const store = reactive({
  rows: [],
  domains: [],
  treeDomains: [],
  metaDatabases: [],
  metaTables: [],
});

const formRef = ref();
const tableRef = ref();
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
      width: 105,
    },
    {
      label: "所属库名",
      prop: "dbName",
      align: "left",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 140,
    },
    {
      label: "所属表名",
      prop: "tableName",
      align: "left",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 180,
    },
    {
      label: "字段名称",
      align: "left",
      prop: "columnName",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 240,
    },
    {
      label: "字段注释",
      prop: "columnComment",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 230,
      align: "left",
    },
    {
      label: "描述",
      prop: "description",
      width: 240,
      align: "left",
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
      label: "数据标准",
      prop: "dataElemName",
      width: 110,
    },
    {
      label: "质量探查",
      prop: "dataQuality",
      width: 90,
      sortable: true,
    },
    {
      label: "字段长度",
      prop: "columnLength",
      width: 90,
      sortable: true,
    },
    {
      label: "字段精度",
      prop: "columnPrecision",
      width: 90,
      sortable: true,
    },
    {
      label: "字段小数",
      prop: "columnScale",
      width: 90,
      sortable: true,
    },
    {
      label: "默认值",
      prop: "defaultValue",
      width: 110,
    },
    {
      label: "是否主键",
      prop: "pkFlag",
      width: 90,
      dict: "table_yes_no",
    },
    {
      label: "是否外键",
      prop: "fkFlag",
      width: 90,
      dict: "table_yes_no",
    },
    {
      label: "是否可空",
      prop: "nullableFlag",
      width: 90,
      dict: "table_yes_no",
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
  func: listColumn,
  params: {
    dataType: 1,
  },
});

const searchStore = reactive({
  items: [
    {
      label: "字段名称",
      prop: "columnName",
      component: {
        is: "input",
      },
    },
    {
      label: "字段注释",
      prop: "columnComment",
      component: {
        is: "input",
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
    {
      label: "所属库名",
      prop: "dbId",
      component: {
        is: "select",
        options: store.metaDatabases,
      },
    },

    {
      label: "所属表名",
      prop: "tableId",
      component: {
        is: "select",
        options: store.metaTables,
      },
    },
  ],
});

const dialog = reactive({
  open: false,
  form: {},
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

// 获取库元素列表
function getMetaDatabases() {
  store.metaDatabases.splice(0, store.metaDatabases.length);
  return listDb({ pageSize: 1000 }).then((res) => {
    res.data.rows.forEach((item) => {
      store.metaDatabases.push({
        value: item.id,
        label: item.dbName,
      });
    });
    return res;
  });
}

// 获取表元素列表
function getMetaTables() {
  store.metaTables.splice(0, store.metaTables.length);
  return listTable({ pageSize: 1000 }).then((res) => {
    res.data.rows.forEach((item) => {
      store.metaTables.push({
        value: item.id,
        label: item.tableName,
      });
    });
    return res;
  });
}

// 获取安全等级
function getSensitiveLevel() {
  listDgSensitiveLevel({ pageSize: 1000 }).then((res) => {
    store.sensitiveLevels = res.data.rows;
  });
}

// 获取标准数据元
// function getDataElem() {
//   getDgDataElemList().then((res) => {
//     store.dataElemList = res.data;
//   });
// }

// 新增
function handleAddClick() {
  router.push({
    path: route.path + "/add",
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
        await delColumn(canDeleteIds.toString());
        message.success("删除成功");
        tableRef.value.getList();
      },
    });
  });
}

// 打开修改弹窗
function handleEditClick(row) {
  dialog.type = "Edit";
  dialog.open = true;
  getColumn(row.id).then((res) => {
    const {
      createBy,
      createTime,
      delFlag,
      updateBy,
      updateTime,
      updaterId,
      auditTime,
      ...form
    } = res.data;
    dialog.form = form;
  });
}

// 关闭修改弹窗
function handleCancelClick() {
  formRef.value.resetFields();
  dialog.form = {};
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
      return updateColumn(dialog.form);
    })
    .then(() => {
      proxy.$modal.msgSuccess(
        `${dialog.form.id ? "修改" : "新增"}字段元数据成功！`
      );
      handleCancelClick();
      tableRef.value.getList();
    })
    .catch(() => {});
}

// 删除
function handleDeleteClick(row) {
  Modal.confirm({
    title: "系统提示",
    content: `是否确认删除编号为${row.id}的数据项？`,
    okText: "确定",
    cancelText: "取消",
    onOk: async () => {
      await delColumn(row.id);
      message.success("删除成功");
      tableRef.value.getList();
    },
  });
}

// 切换状态
function handleStatusChange(row, status) {
  Modal.confirm({
    title: "系统提示",
    content: `是否确认${status == 1 ? "发布" : "取消发布"}数据编号为${
      row.id
    }的字段元数据吗？`,
    okText: "确定",
    cancelText: "取消",
    onOk: async () => {
      try {
        await updateColumnStatus({
          id: row.id,
          status,
        });
        message.success(
          `编号为${row.id}的字段元数据${status == 1 ? "发布" : "取消发布"}成功!`
        );
        row.status = status;
      } catch (error) {
        row.status = status == "1" ? "0" : "1";
      }
    },
  });
}

// getDomains();
getMetaDatabases();
getMetaTables();
// getSensitiveLevel();
</script>

