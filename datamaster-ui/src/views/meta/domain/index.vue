<template>
  <a-spin :spinning="store.loading">
    <div class="app-container">
      <dm-wrap :columns="tableStroe.columns" :tableRef="tableRef">
        <template #search>
          <dm-search-bar
            v-bind="searchStore"
            :params="tableStroe.params"
            :tableRef="tableRef"
          />
        </template>
        <template #actions-data>
          <a-button
            type="primary"
            :icon="h(PlusOutlined)"
            @click="handleAddClick"
            v-hasPermi="['md:domain:add']"
          >
            新增
          </a-button>
          <a-button
            type="danger"
            :icon="h(DeleteOutlined)"
            :disabled="!store.rows.length"
            @click="handleDeleteColumnClick"
            v-hasPermi="['md:domain:remove']"
          >
            删除
          </a-button>
          <a-button
            class="toggle-expand-all"
            type="primary"
            @click="handleToggleExpandClick"
          >
            <svg-icon v-if="defaultExpandAll" icon-class="toggle" />
            <svg-icon v-else icon-class="expand" />
            <span>{{ defaultExpandAll ? "折叠" : "展开" }}</span>
          </a-button>
        </template>
        <dm-table v-bind="tableStroe" ref="tableRef">
          <template #valid-flag="{ row }">
            <a-switch
              v-model:checked="row.validFlag"
              @change="handleStatusChange(row, $event)"
            />
          </template>
          <template #action="{ row }">
            <a-button
              type="link"
              :icon="h(EditOutlined)"
              @click="handleEditClick(row)"
              v-hasPermi="['md:domain:edit']"
            >
              修改
            </a-button>
            <a-button
              type="link"
              :icon="h(PlusOutlined)"
              @click="handleAddClick(row)"
              v-hasPermi="['md:domain:add']"
            >
              新增
            </a-button>
            <a-button
              type="link"
              danger
              :icon="h(DeleteOutlined)"
              :disabled="row.validFlag"
              @click="handleDeleteClick(row)"
              v-hasPermi="['md:domain:remove']"
            >
              删除
            </a-button>
          </template>
        </dm-table>
      </dm-wrap>
    </div>

    <!-- 新增/修改弹窗 -->
    <a-modal
      v-model:open="dialog.open"
      :title="dialog.title"
      width="800"
    >
      <a-spin :spinning="dialog.loading">
        <a-form
          :model="dialog.form"
          :rules="rules"
          ref="formRef"
          :label-col="{ style: { width: '110px' } }"
        >
          <a-form-item label="上级目录" name="parentId">
            <a-tree-select
              show-search
              v-model:value="dialog.form.parentId"
              :tree-data="store.treeDomains"
              :field-names="{ value: 'id', label: 'name', children: 'children' }"
              placeholder="请选择上级目录"
              :tree-default-expand-all="true"
            />
          </a-form-item>
          <a-form-item label="业务域名称" name="name">
            <a-input v-model:value="dialog.form.name" placeholder="请输入业务域名称" />
          </a-form-item>
          <a-form-item label="状态" name="validFlag">
            <a-radio-group v-model:value="dialog.form.validFlag">
              <a-radio :value="true">启用</a-radio>
              <a-radio :value="false">禁用</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="排序" name="sortOrder">
            <a-input-number
              v-model:value="dialog.form.sortOrder"
              :min="0"
              style="width: 100%"
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
          <a-form-item label="备注" class="row-full">
            <a-textarea
              v-model:value="dialog.form.remark"
              placeholder="请输入备注"
              :auto-size="{ minRows: 8 }"
              :maxlength="500"
              show-count
            />
          </a-form-item>
        </a-form>
      </a-spin>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="handleCancelClick">取消</a-button>
          <a-button type="primary" @click="handleConfirmClick">
            确定
          </a-button>
        </div>
      </template>
    </a-modal>
  </a-spin>
</template>

<script setup name="MetaDomain">
import {
  listDomain,
  addDomain,
  delDomain,
  updateDomain,
  updateDomainStatus,
  getDomain,
  batchDeleteCheck,
} from "@/api/tax/domain/domain.js";
import { message, Modal } from "ant-design-vue";
import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons-vue";
import { computed, getCurrentInstance, h, reactive, ref } from "vue";

const DEFAULT_FORM = {
  validFlag: true,
  sortOrder: 0,
};

// 表单验证规则
const rules = {
  parentId: [{ required: true, message: "请选择上级目录", trigger: "change" }],
  name: [{ required: true, message: "请输入业务域名称", trigger: "blur" }],
};

const formRef = ref(null);
const tableRef = ref(null);
const store = reactive({
  rows: [],
  treeDomains: [],
  loading: false,
});

const { proxy } = getCurrentInstance();
const tableStroe = reactive({
  config: {
    stripe: true,
    notPagination: true,
    sort: true,
    table: {
      rowKey: "id",
      defaultSort: { prop: "sortOrder", order: "ascending" },
      treeProps: { children: "children", hasChildren: "hasChildren" },
      defaultExpandAll: true,
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
      //     return row.validFlag == true ? false : true;
      // }
    },
    { label: "业务域名称", prop: "name", width: 240, align: "left" },
    {
      label: "描述",
      prop: "description",
      align: "left",
      width: 240,
      showOverflowTooltip: {
        effect: "light",
      },
    },
    { label: "状态", prop: "validFlag", slot: "valid-flag", align: "center" },
    {
      label: "排序",
      prop: "sortOrder",
      sortable: true,
    },
    { label: "备注", prop: "remark" },
    { label: "创建人", prop: "createBy" },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      sortableKey: "create_time",
      date: true,
    },
    { label: "操作", width: 240, slot: "action", align: "center" },
  ],
  func: getTreeData,
  params: {},
});

const searchStore = reactive({
  items: [
    {
      label: "业务域名称",
      prop: "name",
      component: { is: "input" },
    },
    {
      label: "上级目录",
      prop: "code",
      component: {
        is: "tree-select",
        data: store.treeDomains,
        props: { value: "code", label: "name", children: "children" },
        valueKey: "id",
        checkStrictly: true,
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

const defaultExpandAll = computed({
  get() {
    return tableStroe.config.table.defaultExpandAll;
  },
  set(val) {
    tableStroe.config.table.defaultExpandAll = val;
  },
});

// 获取列表数据
function getTreeData(param) {
  return listDomain(param).then((res) => {
    const data = proxy.handleTree(res.data, "id", "parentId");
    if (Object.keys(tableStroe.params).length) return { data };
    // 查询全部的时候更新数据
    store.treeDomains.splice(0, store.treeDomains.length);
    const domains = {
      id: 0,
      code: 0,
      name: "顶级节点",
      children: JSON.parse(JSON.stringify(data)),
    };
    store.treeDomains.push(domains);
    return { data };
  });
}

// 点击新增
function handleAddClick(row) {
  if (row) {
    dialog.form.parentId = row.id;
  }
  dialog.title = "新增业务域";
  dialog.open = true;
  dialog.func = addDomain;
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
        await delDomain(canDeleteIds.toString());
        message.success("删除成功");
        tableRef.value.getList();
      },
    });
  });
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
  dialog.loading = true;
  formRef.value
    .validate()
    .then(async () => {
      dialog.loading = false;
      await dialog.func(dialog.form);
      proxy.$modal.msgSuccess(
        `${dialog.form.id ? "修改" : "新增"}业务域任务成功！`
      );
      handleCancelClick();
      tableRef.value.getList();
    })
    .catch(() => {
      dialog.loading = false;
    });
}

// 切换状态
function handleStatusChange(row, status) {
  Modal.confirm({
    title: "系统提示",
    content: `是否确认${status ? "上线" : "下线"}数据编号为${row.id}的业务域吗？`,
    okText: "确定",
    cancelText: "取消",
    onOk: async () => {
      try {
        await updateDomainStatus(row.id, status);
        message.success(
          `编号为${row.id}的业务域${status ? "上线" : "下线"}成功!`
        );
        row.validFlag = status;
      } catch (error) {
        row.validFlag = status ? false : true;
      }
    },
  });
}

// 打开修改弹窗
function handleEditClick(row) {
  dialog.open = true;
  dialog.func = updateDomain;
  dialog.title = "修改任务";
  getDomain(row.id).then((res) => {
    dialog.form = res.data;
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
      await delDomain(row.id);
      message.success("删除成功");
      tableRef.value.getList();
    },
  });
}

// 展开
function handleToggleExpandClick() {
  defaultExpandAll.value = !defaultExpandAll.value;
  tableRef.value.reload();
}
</script>
