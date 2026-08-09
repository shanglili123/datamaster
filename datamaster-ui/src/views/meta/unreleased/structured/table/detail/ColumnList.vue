<template>
  <div class="container">
    <qt-wrap
      :columns="tableStroe.columns"
      :tableRef="tableRef"
      :config="{ fullContent: false, actions: { table: { search: false } } }"
    >
      <qt-table v-bind="tableStroe" ref="tableRef">
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
          >
            详情
          </a-button>
          <template v-if="detail.status == '1' && route.query.table_status">
            <a-button
              type="link"
              :disabled="row.status == 1"
              :icon="h(EditOutlined)"
              @click="handleEditClick(row)"
            >
              修改
            </a-button>
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
        </template>
      </qt-table>
    </qt-wrap>

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
        :disabled="dialog.type == 'Detail'"
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
        <a-form-item label="标准数据元" name="dataElemId">
          <a-select
            allow-clear
            v-model:value="dialog.form.dataElemId"
            placeholder="请选择标准数据元"
          >
            <a-select-option
              v-for="item in store.dataElemList"
              :key="item.id"
              :value="item.id"
            >
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
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

        <a-form-item
          label="变更说明"
          class="row-full"
          name="updateMsg"
          v-if="dialog.type != 'Detail'"
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
          <a-button @click="handleCancelClick" v-if="dialog.type != 'Detail'">
            取消
          </a-button>
          <a-button @click="handleCancelClick" v-if="dialog.type == 'Detail'">
            关闭
          </a-button>
          <a-button
            type="primary"
            v-if="dialog.type != 'Detail'"
            @click="handleConfirmClick"
          >
            确定
          </a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="ColumnList">
import { message, Modal } from 'ant-design-vue'
import { getCurrentInstance, h, nextTick, reactive, ref, toValue, watch } from "vue";
import { EyeOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue';

import {
  listColumn,
  delColumn,
  getColumn,
  updateColumn,
  updateColumnStatus,
} from "@/api/cat/unreleased/column.js";

import { useRoute, useRouter } from "vue-router";

import { listDgSensitiveLevel } from "@/api/cat/compliance/sensitiveLevel";

const BASE_URL = "/meta/unreleased/structured/column";

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
  columnScale: [
    {
      required: true,
      message: "请输入字段小数位",
      trigger: ["change", "blur"],
    },
  ],
  updateMsg: [
    { required: true, message: "请输入变更说明", trigger: ["change", "blur"] },
  ],
  businessDefinition: [
    { required: true, message: "请输入业务定义", trigger: "blur" },
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

const props = defineProps({
  detail: {
    type: Object,
    required: true,
  },
});

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "meta_task_status",
  "meta_dw_layers",
  "table_yes_no",
  "column_type"
);

const router = useRouter();
const route = useRoute();

const store = reactive({});

const formRef = ref();
const tableRef = ref();
const tableStroe = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: { prop: "createTime", order: "descending" },
      onRowDblclick: handleDetailClick,
    },
  },
  columns: [
    {
      label: "编号",
      prop: "id",
      sortable: true,
      width: 105,
    },
    {
      label: "字段名称",
      prop: "columnName",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 140,
      link: {
        external: handleDetailClick,
      },
    },
    {
      label: "字段注释",
      prop: "columnComment",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 140,
    },

    {
      label: "标准数据元",
      prop: "dataElemName",
      width: 110,
    },
    {
      label: "字段类型",
      prop: "columnType",
      width: 110,
      dict: "column_type",
    },

    {
      label: "字段长度",
      prop: "columnLength",
      width: 100,
      sortable: true,
    },
    {
      label: "字段精度",
      prop: "columnPrecision",
      width: 100,
      sortable: true,
    },
    {
      label: "字段小数",
      prop: "columnScale",
      width: 100,
      sortable: true,
    },

    {
      label: "是否必填",
      prop: "nullableFlag",
      width: 90,
      dict: "table_yes_no",
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
      label: "状态",
      prop: "status",
      width: 90,
      slot: "status",
      invisible: route.query.released,
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
    tableId: props.detail.id,
    status: route.query.table_status ? "" : props.detail.status,
    dataType: 1,
  },
});

const dialog = reactive({
  open: false,
  form: {},
});

// 获取安全等级
function getSensitiveLevel() {
  listDgSensitiveLevel({ pageSize: 1000 }).then((res) => {
    store.sensitiveLevels = res.data.rows;
  });
}

function refreshColumnList() {
  Object.assign(tableStroe.params, {
    tableId: props.detail.id,
    status: route.query.table_status ? "" : props.detail.status,
    dataType: 1,
  });
  nextTick(() => {
    if (tableRef.value && tableRef.value.getList) {
      tableRef.value.getList();
    }
  });
}

watch(
  () => [props.detail.id, props.detail.status, route.query.table_status],
  () => {
    refreshColumnList();
  }
);

// // 获取标准数据元
// function getDataElem() {
//   getDgDataElemList().then((res) => {
//     store.dataElemList = res.data;
//   });
// }

// 打开修改弹窗
function handleEditClick(row) {
  dialog.type = "Edit";
  dialog.title = "修改字段元数据";
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
async function handleConfirmClick() {
  dialog.loading = true;
  const valid = await formRef.value.validate();
  dialog.loading = false;
  if (!valid) return;
  dialog.loading = true;
  if (dialog.form.safetyLevelId == undefined) {
    dialog.form.safetyLevelId = null;
    dialog.form.safetyLevelName = null;
  }
  await updateColumn(dialog.form);
  dialog.loading = false;
  proxy.$modal.msgSuccess(
    `${dialog.form.id ? "修改" : "新增"}字段元数据成功！`
  );
  handleCancelClick();
  tableRef.value.getList();
}

// 详情页面
function handleDetailPageClick(row) {
  router.push({
    path: BASE_URL + "/detail",
    query: {
      id: row.id,
    },
  });
}

// 详情
function handleDetailClick(row) {
  handleEditClick(row);
  dialog.type = "Detail";
  dialog.title = "字段元数据详情";
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
// getSensitiveLevel();
</script>
