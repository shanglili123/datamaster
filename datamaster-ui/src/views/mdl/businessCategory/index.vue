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
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['dm:businesscategory:add']"
        >
          新增
        </el-button>
        <el-button
          class="extend-btn"
          type="primary"
          plain
          @click="toggleExpandAll"
        >
          <svg-icon v-if="defaultExpandAll" icon-class="toggle" />
          <svg-icon v-else icon-class="expand" />
          <span>{{ defaultExpandAll ? "折叠" : "展开" }}</span>
        </el-button>
      </template>

      <qt-table v-bind="tableStore" ref="tableRef">
        <template #action="{ row }">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(row)"
            v-hasPermi="['dm:businesscategory:edit']"
          >
            修改
          </el-button>
          <el-button
            link
            type="primary"
            icon="Plus"
            @click="handleAdd(row)"
            v-hasPermi="['dm:businesscategory:add']"
          >
            新增
          </el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(row)"
            v-hasPermi="['dm:businesscategory:remove']"
          >
            删除
          </el-button>
        </template>

        <template #validFlag="{ row }">
          <el-switch
            v-model="row.validFlag"
            active-color="#13ce66"
            inactive-color="#ff4949"
            @change="handleStatusChange(row)"
          />
        </template>
      </qt-table>
    </qt-wrap>

    <el-dialog
      :title="title"
      v-model="open"
      width="800px"
      :append-to="$refs['app-container']"
      draggable
    >
      <template #header="{ close, titleId, titleClass }">
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>
      <el-form
        ref="businessCategoryRef"
        :model="form"
        :rules="rules"
        label-width="130px"
        @submit.prevent
      >
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="业务分类名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入业务分类名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="上级分类" prop="parentId">
              <el-tree-select
                filterable
                v-model="form.parentId"
                :data="parentOptions"
                :props="{ value: 'id', label: 'name', children: 'children' }"
                value-key="id"
                placeholder="请选择上级分类"
                check-strictly
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="英文缩写" prop="engName">
              <el-input v-model="form.engName" placeholder="请输入英文缩写" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="负责人" prop="ownerId">
              <el-select
                v-model="form.ownerId"
                filterable
                placeholder="请选择负责人"
                @change="handleOwnerChange"
              >
                <el-option
                  v-for="item in managerOptions"
                  :key="item.userId"
                  :label="item.nickName"
                  :value="item.userId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="负责人电话" prop="ownerPhone">
              <el-input
                v-model="form.ownerPhone"
                placeholder="请输入负责人电话"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                maxlength="256"
                :min-height="128"
                show-word-limit
                placeholder="请输入描述"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="form.remark"
                type="textarea"
                maxlength="500"
                show-word-limit
                placeholder="请输入备注"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="BusinessCategory">
import {
  listBusinessCategory,
  getBusinessCategory,
  addBusinessCategory,
  updateBusinessCategory,
  delBusinessCategory,
} from "@/api/mdl/businessCategory/businessCategory.js";
import { deptUserTree } from "@/api/system/system/user.js";
import QtWrap from "@/components/QtWrap";
import QtTable from "@/components/QtTable";
import QtSearchBar from "@/components/QtSearchBar";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");
const parentOptions = ref([]);
const managerOptions = ref([]);
const open = ref(false);
const title = ref("");
const tableRef = ref(null);

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
    ownerId: null,
  },
  rules: {
    name: [{ required: true, message: "业务分类名称不能为空", trigger: "blur" }],
    engName: [
      { required: true, message: "英文缩写不能为空", trigger: "blur" },
      { pattern: /^[a-zA-Z]+$/, message: "只能输入英文字符", trigger: "blur" },
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
      label: "业务分类名称",
      prop: "name",
      width: 200,
      align: "left",
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "层级编码",
      prop: "code",
      width: 120,
      align: "left",
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
      width: 120,
      align: "left",
    },
    { label: "负责人", prop: "ownerName", align: "left", width: 120 },
    {
      label: "负责人电话",
      prop: "ownerPhone",
      width: 140,
      align: "left",
    },
    { label: "排序", prop: "sortOrder", width: 80, align: "left" },
    { label: "状态", prop: "validFlag", slot: "validFlag", width: 100 },
    { label: "创建人", prop: "createBy", align: "left", width: 120 },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      date: true,
      width: 180,
    },
    { label: "操作", width: 220, slot: "action", fixed: "right" },
  ],
  func: listBusinessCategory,
  params: queryParams,
  events: {
    formatData: (data) => proxy.handleTree(data, "id", "parentId"),
  },
});

const searchStore = reactive({
  items: [
    {
      label: "业务分类名称",
      prop: "name",
      component: { is: "input", placeholder: "请输入业务分类名称" },
    },
    {
      label: "层级编码",
      prop: "code",
      component: { is: "input", placeholder: "请输入层级编码" },
    },
    {
      label: "负责人",
      prop: "ownerId",
      component: {
        is: "select",
        options: computed(() =>
          managerOptions.value.map((item) => ({
            label: item.nickName,
            value: item.userId,
          }))
        ),
        placeholder: "请选择负责人",
      },
    },
  ],
});

function getList() {
  tableRef.value?.getList();
}

function getParentTree() {
  listBusinessCategory().then((response) => {
    parentOptions.value = [];
    const data = { id: 0, name: "顶级节点", children: [] };
    data.children = proxy.handleTree(response.data, "id", "parentId");
    parentOptions.value.push(data);
  });
}

function getManagerOptions() {
  deptUserTree().then((response) => {
    managerOptions.value = response.data;
  });
}

const handleOwnerChange = (selectedValue) => {
  const selectedUser = managerOptions.value.find(
    (user) => user.userId == selectedValue
  );
  form.value.ownerPhone = selectedUser?.phonenumber || "";
};

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    id: null,
    name: null,
    code: null,
    parentId: 0,
    engName: null,
    sortOrder: 0,
    ownerId: null,
    ownerPhone: null,
    description: null,
    validFlag: true,
    remark: null,
  };
  proxy.resetForm("businessCategoryRef");
}

function handleQuery() {
  getList();
}

function handleStatusChange(row) {
  const text = row.validFlag === true ? "启用" : "禁用";
  proxy.$modal
    .confirm(`确认要"${text}","${row.name}"业务分类吗？`)
    .then(() => {
      updateBusinessCategory({
        id: row.id,
        parentId: row.parentId,
        validFlag: row.validFlag,
      })
        .then(() => {
          proxy.$modal.msgSuccess(text + "成功");
          getList();
        })
        .catch(() => {
          row.validFlag = !row.validFlag;
        });
    })
    .catch(() => {
      row.validFlag = !row.validFlag;
    });
}

function resetQuery() {
  Object.keys(queryParams.value).forEach((key) => {
    queryParams.value[key] = null;
  });
  handleQuery();
}

function handleAdd(row) {
  reset();
  getParentTree();
  if (row != null && row.id) {
    form.value.parentId = row.id;
  } else {
    form.value.parentId = 0;
  }
  open.value = true;
  title.value = "新增业务分类";
}

function toggleExpandAll() {
  defaultExpandAll.value = !defaultExpandAll.value;
  tableRef.value.reload();
}

async function handleUpdate(row) {
  reset();
  const response = await listBusinessCategory();
  parentOptions.value = [];
  const filteredDepts = response.data.filter((d) => {
    return (
      d.id !== row.id &&
      !String(d.parentId).split(",").includes(String(row.id))
    );
  });
  const data = { id: 0, name: "顶级节点", children: [] };
  data.children = proxy.handleTree(filteredDepts, "id", "parentId");
  parentOptions.value.push(data);
  if (row != null) {
    form.value.parentId = row.parentId;
  }
  getBusinessCategory(row.id).then((response) => {
    delete response.data.createTime;
    delete response.data.updateTime;
    form.value = response.data;
    open.value = true;
    title.value = "修改业务分类";
  });
}

function submitForm() {
  proxy.$refs["businessCategoryRef"].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateBusinessCategory(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addBusinessCategory(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleDelete(row) {
  proxy.$modal
    .confirm('是否确认删除业务分类"' + row.name + '"的数据项？')
    .then(function () {
      return delBusinessCategory(row.id);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

onMounted(() => {
  getParentTree();
  getManagerOptions();
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
