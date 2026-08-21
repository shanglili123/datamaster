<template>
  <a-spin :spinning="loading">
  <dm-wrap
    :columns="tableStore.columns"
    :tableRef="tableRef"
  >
    <template #search>
      <dm-search-bar
        v-bind="searchStore"
        :params="tableStore.params"
        :tableRef="tableRef"
      />
    </template>
    <template #actions-data>
      <a-button
        type="primary"
        :icon="h(PlusOutlined)"
        @click="handleAdd()"
        v-hasPermi="[`${permBase}:add`]"
        >新增</a-button
      >
      <a-button
        danger
        :icon="h(DeleteOutlined)"
        :disabled="!selection.rows.length"
        v-hasPermi="[`${permBase}:remove`]"
        @click="handleDeleteSelected"
      >
        删除
      </a-button>
      <a-button
        class="toggle-expand-all"
        type="primary"
        @click="toggleExpandAll"
      >
        <svg-icon v-if="defaultExpandAll" icon-class="toggle" />
        <svg-icon v-else icon-class="expand" />
        <span>{{ defaultExpandAll ? "折叠" : "展开" }}</span>
      </a-button>
    </template>
    <dm-table v-bind="tableStore" :key="tableKey" ref="tableRef">
      <template #validFlag="{ row }">
        <a-switch
          v-model:checked="row.validFlag"
          @change="handleStatusChange(row)"
        />
      </template>
      <template #action="{ row }">
        <a-button
          type="link"
          :icon="h(EditOutlined)"
          @click="handleUpdate(row)"
          v-hasPermi="[`${permBase}:edit`]"
          >修改</a-button
        >
        <a-button
          type="link"
          :icon="h(PlusOutlined)"
          @click="handleAdd(row)"
          v-hasPermi="[`${permBase}:add`]"
          >新增</a-button
        >
        <a-button
          type="link"
          danger
          :icon="h(DeleteOutlined)"
          @click="handleDelete(row)"
          v-hasPermi="[`${permBase}:remove`]"
          :disabled="row.validFlag"
          >删除</a-button
        >
      </template>
    </dm-table>
  </dm-wrap>
  </a-spin>

  <CatEditDialog
    ref="catEditDialogRef"
    @cancel="onDialogCancel"
    @submit="onDialogSubmit"
  />
</template>

<script setup>
const props = defineProps({
  listFunc: { type: Function, required: true },
  getFunc: { type: Function, required: true },
  delFunc: { type: Function, required: true },
  addFunc: { type: Function, required: true },
  updateFunc: { type: Function, required: true },
  batchDelCheckFunc: { type: Function, required: false },
  nameLabel: { type: String, default: "目录名称" },
  titleBase: { type: String, default: "目录" },
  permBase: { type: String, required: true },
});

import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'
import { ref, reactive, toRefs, getCurrentInstance, h } from "vue";

import CatEditDialog from "./catEditDialog.vue";

const { proxy } = getCurrentInstance();
const catEditDialogRef = ref();
const appContainerRef = ref();
const isExpandAll = ref(false);
const tableKey = ref(0);
const tableRef = ref(null);
const selection = reactive({ rows: [] });

const treeOptions = ref([]);

const tableStore = reactive({
  config: {
    stripe: true,
    notPagination: true,
    notPaginationParams: true,
    sort: true,
    table: {
      rowKey: "id",
      defaultExpandAll: false,
      defaultSort: { prop: "sortOrder", order: "ascending" },
      treeProps: { children: "children", hasChildren: "hasChildren" },
      onSelectionChange: function (rows) {
        selection.rows = rows;
      },
    },
  },
  columns: [
    {
      type: "selection",
      width: 55,
      selectable: function (row) {
        return true;
      },
    },
    { label: `${props.nameLabel}`, prop: "name", width: 200, align: "left" },
    {
      label: "描述",
      prop: "description",
      align: "left",
      width: 240,
      showOverflowTooltip: { effect: "light" },
    },
    { label: "状态", prop: "validFlag", slot: "validFlag", align: "center" },
    {
      label: "排序",
      prop: "sortOrder",
      sortable: true,
      sortableKey: "sortOrder",
    },
    {
      label: "备注",
      prop: "remark",
      width: 200,
      showOverflowTooltip: { effect: "light" },
    },
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
  func: props.listFunc,
  params: {},
  events: {
    formatData: (data) => proxy.handleTree(data, "id", "parentId"),
  },
});

const searchStore = reactive({
  items: [
    { label: props.nameLabel, prop: "name", component: { is: "input" } },
    {
      label: "上级目录",
      prop: "code",
      component: {
        is: "tree-select",
        data: treeOptions,
        props: { value: "code", label: "name", children: "children" },
        valueKey: "id",
        checkStrictly: true,
      },
    },
  ],
  config: { permi: [`${props.permBase}:query`] },
});
const defaultExpandAll = computed({
  get() {
    return tableStore.config.table.defaultExpandAll;
  },
  set(val) {
    tableStore.config.table.defaultExpandAll = val;
  },
});
function toggleExpandAll() {
  defaultExpandAll.value = !defaultExpandAll.value;
  tableRef.value.reload();
}
function handleQueryClick() {
  tableRef.value && tableRef.value.getList();
}

function handleStatusChange(row) {
  const text = row.validFlag === true ? "启用" : "禁用";
  proxy.$modal
    .confirm(
      '确认要"' + text + '","' + row.name + '"' + props.titleBase + "吗？"
    )
    .then(function () {
      props
        .updateFunc({
          id: row.id,
          parentId: row.parentId,
          validFlag: row.validFlag,
        })
        .then(() => {
          proxy.$modal.msgSuccess(text + "成功");
          handleQueryClick();
        })
        .catch(() => {
          row.validFlag = !row.validFlag;
        });
    })
    .catch(function () {
      row.validFlag = !row.validFlag;
    });
}

function buildTreeOptions(source) {
  treeOptions.value = [];
  const root = { id: 0, name: "顶级节点", children: [] };
  root.children = proxy.handleTree(source, "id", "parentId");
  treeOptions.value.push(root);
}

function handleAdd(row) {
  props.listFunc().then((response) => {
    buildTreeOptions(response.data);
    const parentId = row && row.id ? row.id : 0;
    catEditDialogRef.value.open({
      title: `添加${props.titleBase}`,
      nameLabel: props.nameLabel,
      treeOptions: treeOptions.value,
      form: { parentId },
      rules: rules.value,
    });
  });
}

async function handleUpdate(row) {
  const response = await props.listFunc();
  const filtered = response.data.filter((d) => {
    return (
      d.id !== row.id &&
      !(
        d.parentId != null &&
        d.parentId.toString().split(",").includes(row.id.toString())
      )
    );
  });
  buildTreeOptions(filtered);
  props.getFunc(row.id).then((res) => {
    catEditDialogRef.value.open({
      title: `修改${props.titleBase}`,
      nameLabel: props.nameLabel,
      treeOptions: treeOptions.value,
      form: res.data,
      rules: rules.value,
    });
  });
}

function onDialogSubmit(payload) {
  if (payload.id != null) {
    props
      .updateFunc(payload)
      .then(() => {
        proxy.$modal.msgSuccess("修改成功");
        handleQueryClick();
      })
      .catch(() => {});
  } else {
    props
      .addFunc(payload)
      .then(() => {
        proxy.$modal.msgSuccess("新增成功");
        handleQueryClick();
      })
      .catch(() => {});
  }
}

function onDialogCancel() {}

function handleDelete(row) {
  const id = row.id;
  proxy.$modal
    .confirm("是否确认删除" + props.titleBase + '编号为"' + id + '"的数据项？')
    .then(function () {
      return props.delFunc(id);
    })
    .then(() => {
      handleQueryClick();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}
let loading = ref(false);
function handleDeleteSelected() {
  if (!selection.rows.length) return;
  const ids = selection.rows.map((item) => item.id);
  if (props.batchDelCheckFunc) {
    loading.value = true;
    props
      .batchDelCheckFunc(ids)
      .then((res) => {
        loading.value = false;

        const {
          cannotDeleteCount = 0,
          canDeleteIds = [],
          canDeleteCount = 0,
        } = res?.data || {};
        return Modal.confirm({
          title: "系统提示",
          content: `可删除${canDeleteCount}个，不可删除${cannotDeleteCount}个，是否删除可删部分`,
          okText: "确定",
          cancelText: "取消",
          onOk: async () => {
            if (canDeleteCount === 0) {
              message.success("执行成功");
              return;
            } else {
              await props.delFunc(canDeleteIds);
              message.success("删除成功");
              tableRef.value.getList();
            }
          },
        });
      })
      .finally(() => {});
  } else {
    Modal.confirm({
      title: "系统提示",
      content: `可删除${selection.rows.length}个，不可删除0个，是否删除可删部分`,
      okText: "确定",
      cancelText: "取消",
      onOk: async () => {
        await props.delFunc(ids);
        message.success("删除成功");
        tableRef.value.getList();
      },
    });
  }
}

const data = reactive({
  form: {},
  rules: {
    name: [
      {
        required: true,
        message: `${props.nameLabel}不能为空`,
        trigger: "blur",
      },
    ],
    parentId: [
      { required: true, message: "上级目录不能为空", trigger: "blur" },
    ],
  },
});
const { form, rules } = toRefs(data);

defineExpose({
  refresh: handleQueryClick,
  tableStore,
});

props.listFunc().then((response) => buildTreeOptions(response.data));
</script>

