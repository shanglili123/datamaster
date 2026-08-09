<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch">
      <a-form
        class="btn-style"
        :model="queryParams"
        ref="queryRef"
        layout="inline"
        :label-col="{ style: { width: '75px' } }"
      >
        <a-form-item label="ID" name="id">
          <a-input
            class="el-form-input-width"
            v-model:value="queryParams.id"
            placeholder="请输入ID"
            allow-clear
            @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="部门名称" name="name">
          <a-input
            class="el-form-input-width"
            v-model:value="queryParams.name"
            placeholder="请输入部门名称"
            allow-clear
            @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input
            class="el-form-input-width"
            v-model:value="queryParams.email"
            placeholder="请输入邮箱"
            allow-clear
            @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="部门状态" name="status">
          <a-select
            v-model:value="queryParams.status"
            placeholder="请选择部门状态"
            allow-clear
            class="el-form-input-width"
          >
            <a-select-option
              v-for="dict in sys_notice_status"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-select-option>
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
          <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
          </a-button>
        </a-form-item>
      </a-form>
    </div>
    <div class="pagecont-bottom">
      <div class="justify-between mb15">
        <a-row :gutter="10" class="btn-style">
          <a-col :span="1.5">
            <a-button
              type="primary"
              :icon="h(PlusOutlined)"
              @click="handleAdd"
              v-hasPermi="['gen:dept:add']"
              >新增</a-button
            >
          </a-col>
          <a-col :span="1.5">
            <a-button :icon="h(SwitcherOutlined)" @click="toggleExpandAll"
              >展开/折叠</a-button
            >
          </a-col>
        </a-row>
        <right-toolbar
          v-model:showSearch="showSearch"
          @queryTable="getList"
        ></right-toolbar>
      </div>

      <a-table
        height="60vh"
        v-if="refreshTable"
        :loading="loading"
        :data-source="deptList"
        :columns="tableColumns"
        :pagination="false"
        row-key="id"
        :default-expand-all-rows="isExpandAll"
        :children-column-name="'children'"
        :locale="{ emptyText: '暂无数据' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <dict-tag :options="sys_notice_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button
              type="link"
              :icon="h(EditOutlined)"
              @click="handleUpdate(record)"
              v-hasPermi="['gen:dept:edit']"
              >修改</a-button
            >
            <a-button
              type="link"
              :icon="h(PlusOutlined)"
              @click="handleAdd(record)"
              v-hasPermi="['gen:dept:add']"
              >新增</a-button
            >
            <a-button
              type="link"
              danger
              :icon="h(DeleteOutlined)"
              @click="handleDelete(record)"
              v-hasPermi="['gen:dept:remove']"
              >删除</a-button
            >
          </template>
          <template v-else>
            <span>{{ record[column.dataIndex] || '-' }}</span>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 添加或修改示例部门对话框 -->
    <a-modal
      :title="title"
      v-model:open="open"
      width="800px"
      draggable
      destroy-on-close
    >
      <a-form ref="deptRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="父部门id" name="parentId">
              <a-tree-select
                v-model:value="form.parentId"
                :tree-data="deptOptions"
                :field-names="{ value: 'id', label: 'name', children: 'children' }"
                placeholder="请选择父部门id"
                check-strictly
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="部门名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入部门名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="form.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="部门状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio
                  v-for="dict in sys_notice_status"
                  :key="dict.value"
                  :value="parseInt(dict.value)"
                  >{{ dict.label }}</a-radio
                >
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否有效" name="validFlag">
              <a-input v-model:value="form.validFlag" placeholder="请输入是否有效" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="删除标志" name="delFlag">
              <a-input v-model:value="form.delFlag" placeholder="请输入删除标志" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-input
                v-model:value="form.remark"
                type="textarea"
                placeholder="请输入内容"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button type="primary" @click="submitForm">确 定</a-button>
          <a-button @click="cancel">取 消</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="Dept">
import {
  listDept,
  getDept,
  delDept,
  addDept,
  updateDept,
} from "@/api/example/gen/dept";
import { h } from 'vue';
import { PlusOutlined, SwitcherOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { sys_notice_status } = proxy.useDict("sys_notice_status");

const tableColumns = [
  { title: '部门名称', dataIndex: 'name', align: 'left', ellipsis: true },
  { title: '邮箱', dataIndex: 'email', align: 'center' },
  { title: '部门状态', dataIndex: 'status', align: 'center' },
  { title: '操作', key: 'actions', align: 'center', width: 220 },
];

const deptList = ref([]);
const deptOptions = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const isExpandAll = ref(true);
const refreshTable = ref(true);
const daterangeCreateTime = ref([]);
const daterangeUpdateTime = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    id: null,
    name: null,
    leader: null,
    phone: null,
    email: null,
    status: null,
  },
  rules: {
    parentId: [
      { required: true, message: "父部门id不能为空", trigger: "blur" },
    ],
    status: [
      { required: true, message: "部门状态不能为空", trigger: "change" },
    ],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" },
    ],
    updateTime: [
      { required: true, message: "更新时间不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询示例部门列表 */
function getList() {
  loading.value = true;
  queryParams.value.params = {};
  if (null != daterangeCreateTime && "" != daterangeCreateTime) {
    queryParams.value.params["beginCreateTime"] = daterangeCreateTime.value[0];
    queryParams.value.params["endCreateTime"] = daterangeCreateTime.value[1];
  }
  if (null != daterangeUpdateTime && "" != daterangeUpdateTime) {
    queryParams.value.params["beginUpdateTime"] = daterangeUpdateTime.value[0];
    queryParams.value.params["endUpdateTime"] = daterangeUpdateTime.value[1];
  }
  listDept(queryParams.value).then((response) => {
    deptList.value = proxy.handleTree(response.data, "id", "parentId");
    loading.value = false;
  });
}

/** 查询示例部门下拉树结构 */
function getTreeselect() {
  listDept().then((response) => {
    deptOptions.value = [];
    const data = { id: 0, name: "顶级节点", children: [] };
    data.children = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value.push(data);
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
    parentId: null,
    name: null,
    leader: null,
    phone: null,
    email: null,
    status: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
  };
  proxy.resetForm("deptRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  daterangeCreateTime.value = [];
  daterangeUpdateTime.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 新增按钮操作 */
function handleAdd(row) {
  reset();
  getTreeselect();
  if (row != null && row.id) {
    form.value.parentId = row.id;
  } else {
    form.value.parentId = 0;
  }
  open.value = true;
  title.value = "新增示例部门";
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}

/** 修改按钮操作 */
// async function handleUpdate(row) {
//   reset();
//   await getTreeselect();
//   if (row != null) {
//     form.value.parentId = row.parentId;
//   }
//   getDept(row.id).then(response => {
//     form.value = response.data;
//     open.value = true;
//     title.value = "修改示例部门";
//   });
// }

async function handleUpdate(row) {
  reset();
  // await getTreeselect();
  const response = await listDept();
  deptOptions.value = [];
  // 过滤节点的计算属性
  const filteredDepts = response.data.filter((d) => {
    // 过滤条件：去掉目标部门ID或者祖先中包含目标部门ID的项
    return (
      d.id !== row.id &&
      !d.parentId.toString().split(",").includes(row.id.toString())
    );
  });
  const data = { id: 0, name: "顶级节点", children: [] };
  data.children = proxy.handleTree(filteredDepts, "id", "parentId");
  deptOptions.value.push(data);
  if (row != null) {
    form.value.parentId = row.parentId;
  }
  getDept(row.id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改示例部门";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["deptRef"].validate().then(() => {
    if (form.value.id != null) {
      updateDept(form.value).then((response) => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addDept(form.value).then((response) => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal
    .confirm('是否确认删除示例部门编号为"' + row.id + '"的数据项？')
    .then(function () {
      return delDept(row.id);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

getList();
</script>

