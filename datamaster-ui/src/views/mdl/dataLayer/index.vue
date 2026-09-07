<template>
  <div class="app-container" ref="app-container">

    <a-layout>
      <!-- 左侧树 -->
      <DeptTree
        :deptOptions="layerTreeOptions"
        :leftWidth="leftWidth"
        :placeholder="'请输入数仓分层名称'"
        ref="layerTreeRef"
        @node-click="handleNodeClick"
        title="数仓分层"
      >
      </DeptTree>

      <!-- 右侧列表 -->
      <a-layout-content class="main-content">
        <!-- 顶部信息卡片 -->
        <layerInfoCard v-if="currentLayer" class="mb15" :layer="currentLayer" />

        <dm-wrap :columns="tableStore.columns" :tableRef="tableRef">
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
              @click="handleAdd"
              v-hasPermi="['mdl:dataLayer:add']"
            >
              新增
            </a-button>
          </template>

          <dm-table v-bind="tableStore" ref="tableRef">
            <template #action="{ row }">
              <a-button
                type="link"
                size="small"
                @click="handleUpdate(row)"
                v-hasPermi="['mdl:dataLayer:edit']"
              >
                修改
              </a-button>
              <a-button
                type="link"
                danger
                size="small"
                @click="handleDelete(row)"
                v-hasPermi="['mdl:dataLayer:remove']"
              >
                删除
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleDetail(row)"
                v-hasPermi="['mdl:dataLayer:edit']"
              >
                详情
              </a-button>
            </template>

            <template #status="{ row }">
              <a-switch
                v-model:checked="row.status"
                checked-value="1"
                un-checked-value="0"
                @change="() => handleStatusChange(row)"
              />
            </template>
          </dm-table>
        </dm-wrap>
      </a-layout-content>
    </a-layout>

    <!-- 添加或修改规范对话框 -->
    <a-modal
      :title="title"
      v-model:open="open"
      draggable
      width="800px"
      destroy-on-close
    >
      <a-form
        ref="specificationRef"
        :model="form"
        :rules="rules"
        :label-col="{ style: { width: '140px' } }"
        @submit.prevent
      >
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="表前缀" name="prefixName">
              <a-input v-model:value="form.prefixName" placeholder="请输入表前缀" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="业务英文缩写" name="businessEngName">
              <a-input
                v-model:value="form.businessEngName"
                placeholder="请输入业务英文缩写"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio
                  v-for="dict in sys_normal_disable"
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
            <a-form-item label="描述" name="description">
              <a-textarea
                v-model:value="form.description"
                placeholder="请输入描述"
                :auto-size="{ minRows: 4, maxRows: 8 }"
                show-count
                :maxlength="500"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="cancel">取 消</a-button>
          <a-button type="primary" @click="submitForm">确 定</a-button>
        </div>
      </template>
    </a-modal>

    <!-- 规范详情对话框 -->
    <a-modal
      title="规范详情"
      v-model:open="openDetail"
      draggable
      width="800px"
      destroy-on-close
    >
      <a-form ref="specificationDetailRef" :model="form" :label-col="{ style: { width: '140px' } }">
        <a-form-item label="编号:" name="id">
          <div class="form-readonly">
            {{ form.id }}
          </div>
        </a-form-item>
        <a-form-item label="表前缀" name="prefixName">
          <div class="form-readonly">{{ form.prefixName ?? "-" }}</div>
        </a-form-item>
        <a-form-item label="业务英文缩写" name="businessEngName">
          <div class="form-readonly">{{ form.businessEngName ?? "-" }}</div>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <dict-tag :options="sys_normal_disable" :value="form.status" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <div class="form-readonly textarea">
            {{ form.description ?? "-" }}
          </div>
        </a-form-item>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="创建人" name="createBy">
              <div class="form-readonly">
                {{ form.createBy }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="创建时间" name="createTime">
              <div class="form-readonly">
                {{ parseTime(form.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="更新人" name="updateBy">
              <div class="form-readonly">
                {{ form.updateBy }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="更新时间" name="updateTime">
              <div class="form-readonly">
                {{ parseTime(form.updateTime, "{y}-{m}-{d} {h}:{i}") || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="cancelDetail">关 闭</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="DataLayer">
import { treeDataLayer } from "@/api/mdl/dataLayer/dataLayer.js";
import {
  listDataLayerSpecification,
  getDataLayerSpecification,
  addDataLayerSpecification,
  updateDataLayerSpecification,
  delDataLayerSpecification,
} from "@/api/mdl/dataLayerSpecification/dataLayerSpecification.js";
import DeptTree from "@/components/DeptTree";
import layerInfoCard from "./components/layerInfoCard.vue";
import {
  computed,
  getCurrentInstance,
  nextTick,
  onMounted,
  reactive,
  ref,
  toRefs,
} from "vue";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");
const leftWidth = ref(240); // 初始左侧宽度
const layerTreeOptions = ref([]);
const currentLayer = ref(null);
const layerTreeRef = ref(null);
const tableRef = ref(null);
const activeDropdownNodeId = ref(null);

const ids = ref([]);
const single = ref(true);
const multiple = ref(true);

const tableStore = reactive({
  config: {
    stripe: true,
    table: {
      rowKey: "id",
      defaultSort: { prop: "createTime", order: "descending" },
      onSelectionChange: function (selection) {
        ids.value = selection.map((item) => item.id);
        single.value = selection.length !== 1;
        multiple.value = !selection.length;
      },
    },
  },
  columns: [
    // { type: "selection", width: 55, align: "left" },
    { label: "编号", prop: "id", width: 60, sortable: true },
    {
      label: "表前缀",
      prop: "prefixName",
      align: "left",
      width: 180,
      showOverflowTooltip: true,
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
      label: "业务英文缩写",
      prop: "businessEngName",
      align: "left",
      width: 100,
    },

    {
      label: "状态",
      prop: "status",
      align: "left",
      width: 100,
      slot: "status",
    },

    {
      label: "创建人",
      prop: "createBy",
      width: 120,
      align: "left",
      showOverflowTooltip: true,
    },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      sortableKey: "create_time",
      date: true,
      width: 150,
      align: "left",
    },
    {
      label: "操作",
      width: 220,
      slot: "action",
      fixed: "right",
    },
  ],
  func: listDataLayerSpecification,
  params: {
    dataLayerId: computed(() => currentLayer.value?.id || null),
  },
});

const searchStore = reactive({
  items: [
    {
      label: "表前缀",
      prop: "prefixName",
      component: { is: "input", placeholder: "请输入表前缀" },
    },
    {
      label: "业务英文缩写",
      prop: "businessEngName",
      component: { is: "input", placeholder: "请输入业务英文缩写" },
    },
  ],
});

const open = ref(false);
const openDetail = ref(false);
const title = ref("");

const data = reactive({
  form: {},
  rules: {
    prefixName: [
      { required: true, message: "表前缀不能为空", trigger: "blur" },
    ],
    businessEngName: [
      { required: true, message: "业务英文缩写不能为空", trigger: "blur" },
      { pattern: /^[a-zA-Z]+$/, message: "只能输入英文字符", trigger: "blur" },
    ],
  },
});

const { form, rules } = toRefs(data);

/** 查询树 */
function getTree() {
  treeDataLayer().then((response) => {
    layerTreeOptions.value = response.data;
    nextTick(() => {
      let targetNode = null;
      const findNode = (nodes) => {
        for (let node of nodes) {
          if (node.name === "操作数据层") {
            targetNode = node;
            return;
          }
          if (node.children && node.children.length > 0) {
            findNode(node.children);
          }
        }
      };
      findNode(layerTreeOptions.value);

      if (targetNode) {
        if (layerTreeRef.value && layerTreeRef.value.setCurrentKey) {
          layerTreeRef.value.setCurrentKey(targetNode.id);
        }
        currentLayer.value = targetNode;
        tableRef.value?.getList();
      } else if (layerTreeOptions.value.length > 0) {
        const firstNode = layerTreeOptions.value[0];
        if (layerTreeRef.value && layerTreeRef.value.setCurrentKey) {
          layerTreeRef.value.setCurrentKey(firstNode.id);
        }
        currentLayer.value = firstNode;
        tableRef.value?.getList();
      }
    });
  });
}

/** 节点单击事件 */
function handleNodeClick(data) {
  if (data.parentId != 0) {
    currentLayer.value = data;
    tableRef.value?.getList();
  }
}

// 处理树节点点击
function handleTreeNodeClick(node) {
  // 如果是一级节点，只展开/折叠，不触发选中
  if (node.level === 1) {
    if (node.expanded) {
      node.collapse();
    } else {
      node.expand();
    }
  } else {
    // 如果不是一级节点，执行正常的节点点击逻辑
    currentLayer.value = node.data;
    tableRef.value?.getList();
  }
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 关闭详情
function cancelDetail() {
  openDetail.value = false;
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    dataLayerId: currentLayer.value ? currentLayer.value.id : null,
    prefixName: null,
    businessEngName: null,
    ownerUserId: null,
    ownerUserName: null,
    ownerUserPhoneNumber: null,
    status: "0",
    description: null,
  };
  proxy.resetForm("specificationRef");
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  if (!currentLayer.value) {
    proxy.$modal.msgError("请先选择左侧数仓分层");
    return;
  }
  open.value = true;
  title.value = "添加规范";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row?.id || ids.value[0];
  getDataLayerSpecification(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改规范";
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row?.id || ids.value[0];
  getDataLayerSpecification(_id).then((response) => {
    form.value = response.data;
    openDetail.value = true;
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["specificationRef"]
    .validate()
    .then(() => {
      if (form.value.id != null) {
        updateDataLayerSpecification(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          tableRef.value?.getList();
        });
      } else {
        addDataLayerSpecification(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          tableRef.value?.getList();
        });
      }
    })
    .catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row?.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除规范编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delDataLayerSpecification(_ids);
    })
    .then(() => {
      tableRef.value?.getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** 状态修改 */
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用";
  proxy.$modal
    .confirm("确认要" + text + '规范"' + row.id + '"吗?')
    .then(function () {
      return updateDataLayerSpecification({ id: row.id, status: row.status });
    })
    .then(() => {
      proxy.$modal.msgSuccess(text + "成功");
    })
    .catch(function () {
      // 恢复switch状态
      row.status = row.status === "0" ? "1" : "0";
    });
}

onMounted(() => {
  getTree();
});
</script>

<style scoped lang="scss">
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.treelabel {
  flex: 1;
  cursor: pointer;

  &:hover {
    color: var(--el-color-primary);
  }
}

.iconimg,
.zjiconimg {
  margin-right: 6px;
  font-size: 16px;
}

.colorxz {
  color: var(--el-color-primary);
}

.colorwxz {
  color: var(--el-text-color-secondary);
}

.operation-trigger {
  padding: 4px;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background-color: var(--el-fill-color-light);
  }

  &.is-active {
    background-color: var(--el-fill-color);
  }
}

.action-icon {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
</style>


