<template>
  <div class="app-container" ref="app-container">
    <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="tableStore.params"
          :tableRef="tableRef"
          :visible-count="4"
        />
      </template>
      <template #actions-data>
        <a-button
          type="primary"
          @click="handleAdd"
          v-hasPermi="['mdl:themeDomain:add']"
        >
          新增
        </a-button>
        <a-button
          class="extend-btn"
          type="primary"
          @click="toggleExpandAll"
        >
          <svg-icon v-if="defaultExpandAll" icon-class="toggle" />
          <svg-icon v-else icon-class="expand" />
          <span>{{ defaultExpandAll ? "折叠" : "展开" }}</span>
        </a-button>
      </template>

      <qt-table v-bind="tableStore" ref="tableRef">
        <template #action="{ row }">
          <a-button
            type="link"
            size="small"
            @click="handleUpdate(row)"
            v-hasPermi="['mdl:themeDomain:edit']"
          >
            修改
          </a-button>
          <a-button
            type="link"
            size="small"
            @click="handleAdd(row)"
            v-hasPermi="['mdl:themeDomain:add']"
          >
            新增
          </a-button>
          <a-button
            type="link"
            danger
            size="small"
            @click="handleDelete(row)"
            v-hasPermi="['mdl:themeDomain:remove']"
          >
            删除
          </a-button>
        </template>

        <template #validFlag="{ row }">
          <a-switch
            v-model:checked="row.validFlag"
            @change="() => handleStatusChange(row)"
          >
          </a-switch>
        </template>
      </qt-table>
    </qt-wrap>
    <a-modal
      :title="title"
      v-model:open="open"
      width="800px"
      draggable
      destroy-on-close
    >
      <a-form
        ref="themeDomainRef"
        :model="form"
        :rules="rules"
        :label-col="{ style: { width: '130px' } }"
        @submit.prevent
      >
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="主题域名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入主题域名称" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="上级主题域" name="parentId">
              <a-tree-select
                show-search
                allow-clear
                v-model:value="form.parentId"
                :tree-data="attDataElemCatOptions"
                :field-names="{ value: 'id', label: 'name', children: 'children' }"
                placeholder="请选择上级主题域"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="数仓分层" name="dataLayerId">
              <a-tree-select
                show-search
                allow-clear
                :treeDefaultExpandAll="true"
                v-model:value="form.dataLayerId"
                :tree-data="dataLayerOptions"
                :field-names="{ value: 'id', label: 'name', children: 'children' }"
                placeholder="请选择数仓分层"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="英文缩写" name="engName">
              <a-input v-model:value="form.engName" placeholder="请输入英文缩写" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="描述" name="description">
              <a-textarea
                v-model:value="form.description"
                :maxlength="256"
                :auto-size="{ minRows: 4, maxRows: 8 }"
                show-count
                placeholder="请输入描述"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea
                v-model:value="form.remark"
                :maxlength="500"
                show-count
                :auto-size="{ minRows: 2, maxRows: 4 }"
                placeholder="请输入备注"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="cancel">取 消</a-button>
          <a-button type="primary" @click="submitForm"
            >确 定</a-button
          >
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="DataElemCat">
import {
  getThemeDomain,
  addThemeDomain,
  updateThemeDomain,
  listThemeDomain,
  delThemeDomain,
} from "@/api/mdl/themeDomain/themeDomain.js";
import { treeDataLayer } from "@/api/mdl/dataLayer/dataLayer.js";
import QtWrap from "@/components/QtWrap";
import QtTable from "@/components/QtTable";
import QtSearchBar from "@/components/QtSearchBar";

const { proxy } = getCurrentInstance();
const attDataElemCatOptions = ref([]);
const dataLayerOptions = ref([]);
const open = ref(false);
const title = ref("");
const tableRef = ref(null);

// 添加计算属性用于控制展开/折叠
const defaultExpandAll = computed({
  get() {
    return tableStore.config.table.defaultExpandAll;
  },
  set(val) {
    tableStore.config.table.defaultExpandAll = val;
  },
});

const data = reactive({
  form: {},
  queryParams: {
    name: null,
    code: null,
    dataLayerId: null,
  },
  rules: {
    name: [{ required: true, message: "主题域名称不能为空", trigger: "blur" }],
    parentId: [
      { required: true, message: "上级主题域不能为空", trigger: "change" },
    ],
    engName: [
      { required: true, message: "英文缩写不能为空", trigger: "blur" },
      { pattern: /^[a-zA-Z]+$/, message: "只能输入英文字符", trigger: "blur" },
    ],
    dataLayerId: [
      { required: true, message: "数仓分层不能为空", trigger: "change" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

const tableStore = reactive({
  config: {
    stripe: true,
    notPagination: true,
    notPaginationParams: true,
    table: {
      rowKey: "id",
      defaultExpandAll: false,
      lazy: false,
      load: null,
      treeProps: { children: "children", hasChildren: "hasChildren" },
      defaultSort: { prop: "createTime", order: "descending" },
    },
  },
  columns: [
    {
      label: "主题域名称",
      prop: "name",
      width: 200,
      align: "left",
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "描述",
      prop: "description",
      width: 250,
      align: "left",
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "英文缩写",
      prop: "engName",
      width: 200,
      align: "left",
      showOverflowTooltip: { effect: "light" },
    },
    { label: "数仓分层", prop: "dataLayerName", align: "left", width: 140 },
    { label: "状态", prop: "validFlag", slot: "validFlag", width: 100 },
    { label: "备注", prop: "remark", align: "left" },

    { label: "创建人", prop: "createBy", align: "left" },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      date: true,
      width: 180,
    },
    { label: "操作", width: 220, slot: "action", fixed: "right" },
  ],
  func: listThemeDomain,
  params: queryParams,
  events: {
    formatData: (data) => proxy.handleTree(data, "id", "parentId"),
  },
});

const searchStore = reactive({
  items: [
    {
      label: "主题域名称",
      prop: "name",
      component: { is: "input", placeholder: "请输入主题域名称" },
    },
    {
      label: "上级主题域",
      prop: "code",
      component: {
        is: "tree-select",
        data: attDataElemCatOptions,
        props: { value: "code", label: "name", children: "children" },
        valueKey: "id",
        placeholder: "请选择上级主题域",
        checkStrictly: true,
      },
    },
    {
      label: "数仓分层",
      prop: "dataLayerId",
      component: {
        is: "tree-select",
        data: dataLayerOptions,
        props: { value: "id", label: "name", children: "children" },
        valueKey: "id",
        placeholder: "请选择数仓分层",
        checkStrictly: true,
      },
    },
  ],
});

/** 查询主题域管理列表 */
function getList() {
  tableRef.value?.getList();
}

function getDataTree() {
  listThemeDomain().then((response) => {
    attDataElemCatOptions.value = [];
    const data = { id: 0, name: "顶级节点", children: [] };
    data.children = proxy.handleTree(response.data, "id", "parentId");
    attDataElemCatOptions.value.push(data);
  });
}

function getDataLayerTree() {
  treeDataLayer().then((response) => {
    const disableRoot = (list) => {
      return list.map((item) => {
        const newItem = { ...item };
        if (!item.parentId || item.parentId === 0 || item.parentId === "0") {
          newItem.disabled = true;
        }
        if (item.children && item.children.length) {
          newItem.children = disableRoot(item.children);
        }
        return newItem;
      });
    };
    dataLayerOptions.value = disableRoot(response.data);
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    name: null,
    parentId: null,
    description: null,
    code: null,
    engName: null,
    ownerUserId: null,
    ownerUserPhoneNumber: null,
    dataLayerId: null,
    // validFlag: true,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
  };
  proxy.resetForm("themeDomainRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  getList();
}
/** 改变启用状态值 */
function handleStatusChange(row) {
  const text = row.validFlag === true ? "启用" : "禁用";
  proxy.$modal
    .confirm(`确认要"${text}","${row.name}"主题域吗？`)
    .then(() => {
      updateThemeDomain({
        id: row.id,
        parentId: row.parentId,
        validFlag: row.validFlag,
      })
        .then((response) => {
          proxy.$modal.msgSuccess(text + "成功");
          getList();
        })
        .catch((err) => {
          row.validFlag = !row.validFlag;
        });
    })
    .catch(() => {
      row.validFlag = !row.validFlag;
    });
}

/** 重置按钮操作 */
function resetQuery() {
  Object.keys(queryParams.value).forEach((key) => {
    queryParams.value[key] = null;
  });
  handleQuery();
}

/** 新增按钮操作 */
function handleAdd(row) {
  reset();
  // getTreeselect();
  listThemeDomain().then((response) => {
    attDataElemCatOptions.value = [];
    const data = { id: 0, name: "顶级节点", children: [] };
    data.children = proxy.handleTree(response.data, "id", "parentId");
    attDataElemCatOptions.value.push(data);
  });
  if (row != null && row.id) {
    form.value.parentId = row.id;
  } else {
    form.value.parentId = 0;
  }
  open.value = true;
  title.value = "新增主题域";
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  defaultExpandAll.value = !defaultExpandAll.value;
  tableRef.value.reload();
}

/** 修改按钮操作 */
async function handleUpdate(row) {
  reset();
  // await getTreeselect();
  const response = await listThemeDomain();
  attDataElemCatOptions.value = [];
  // 过滤节点的计算属性
  const filteredDepts = response.data.filter((d) => {
    // 过滤条件：去掉目标部门ID或者祖先中包含目标部门ID的项
    return (
      d.ID !== row.id &&
      !d.parentId.toString().split(",").includes(row.id.toString())
    );
  });
  const data = { id: 0, name: "顶级节点", children: [] };
  data.children = proxy.handleTree(filteredDepts, "id", "parentId");
  attDataElemCatOptions.value.push(data);
  if (row != null) {
    form.value.parentId = row.parentId;
  }
  getThemeDomain(row.id).then((response) => {
    //把createTime过滤掉
    delete response.data.createTime;
    delete response.data.updateTime;
    form.value = response.data;

    open.value = true;
    title.value = "修改主题域";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["themeDomainRef"]
    .validate()
    .then(() => {
      if (form.value.id != null) {
        updateThemeDomain(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addThemeDomain(form.value).then((response) => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    })
    .catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal
    .confirm('是否确认删除主题域管理编号为"' + row.name + '"的数据项？')
    .then(function () {
      return delThemeDomain(row.id);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

// 初始化数据
onMounted(() => {
  getDataTree();
  getDataLayerTree();
});
</script>
<style scoped lang="scss">
.extend-btn {
  .svg-icon {
    font-size: 12px;
    margin-right: 6px;
  }
}
</style>

