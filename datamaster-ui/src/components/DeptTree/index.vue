<template>
  <a-layout-sider
    :width="leftWidth || 1"
    :collapsed="leftWidth === 0"
    :collapsed-width="0"
    :trigger="null"
    :style="{
      marginLeft: leftWidth == 0 ? '-15px' : '0px',
      '--qt-wrap-height': qtWrapheight,
    }"
    class="left-pane"
  >
    <div class="left-tree">
      <!-- 头部：第一行标题 + 刷新/收缩，第二行搜索框独占一行 -->
      <div class="tree-header">
        <span
          class="header-title"
          :class="{ 'is-active': selectedAll }"
          @click="handleSelectAll"
        >
          <FolderOpenOutlined class="header-icon" />
          <span class="header-text">{{ title }}</span>
        </span>
        <span class="header-actions">
          <a-tooltip title="刷新目录">
            <a-button type="text" size="small" class="header-btn" @click="handleRefresh">
              <ReloadOutlined />
            </a-button>
          </a-tooltip>
          <a-tooltip title="收起目录">
            <a-button type="text" size="small" class="header-btn" @click="toggleCollapse">
              <MenuFoldOutlined />
            </a-button>
          </a-tooltip>
        </span>
      </div>
      <div class="tree-search">
        <a-input
          class="filter-tree"
          size="small"
          v-model:value="deptName"
          :placeholder="placeholder"
          allow-clear
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
      </div>
      <div class="head-container">
        <a-tree
          class="dept-tree"
          :tree-data="displayData"
          :field-names="{ title: 'name', children: 'children', key: 'id' }"
          ref="deptTreeRef"
          :selected-keys="selectedKeys"
          :expanded-keys="expandedKeys"
          @select="handleTreeSelect"
          @expand="handleTreeExpand"
        >
          <template #title="{ data, selected }">
            <span class="custom-tree-node">
              <!-- 有子节点：文件夹图标 -->
              <FolderOpenOutlined
                class="iconimg colorxz"
                v-if="isExpanded(data) && data.children && data.children.length"
              />
              <FolderOutlined
                class="iconimg colorxz"
                v-else-if="data.children && data.children.length"
              />

              <!-- 无子节点的节点 -->
              <FileTextOutlined
                class="zjiconimg colorwxz"
                v-show="!selected && (!data.children || data.children.length === 0)"
              />
              <FileTextOutlined
                class="zjiconimg colorxz"
                v-show="selected && (!data.children || data.children.length === 0)"
              />

              <a-tooltip
                class="box-item"
                :title="data.name"
                placement="top-start"
                :disabled="!data.name || data.name.length < 10"
              >
                <span class="treelabel">{{ data.name }}</span>
              </a-tooltip>

              <!-- 操作入口 -->
              <a-dropdown
                v-if="editable"
                trigger="click"
                @openChange="(v) => handleDropdownVisibleChange(v, data.id)"
              >
                <span
                  class="operation-trigger"
                  :class="{ 'is-active': activeDropdownNodeId === data.id }"
                  @click.stop
                >
                  <MoreOutlined class="action-icon" />
                </span>
                <template #overlay>
                  <a-menu class="dept-tree-dropdown" @click="({ key }) => handleCommand(key, data)">
                    <a-menu-item :icon="h(PlusOutlined)" key="add">新增子级</a-menu-item>
                    <template v-if="data.id != '0'">
                      <a-menu-item :icon="h(CopyOutlined)" key="addSibling">新增同级</a-menu-item>
                      <a-menu-item :icon="h(EditOutlined)" key="edit">编辑</a-menu-item>
                      <a-menu-item :icon="h(DeleteOutlined)" key="delete" class="delete-item">删除</a-menu-item>
                    </template>
                  </a-menu>
                </template>
              </a-dropdown>
            </span>
          </template>
        </a-tree>
      </div>
    </div>
  </a-layout-sider>

  <!-- 拖拽栏 -->
  <div class="resize-bar" @mousedown="startResize">
    <div class="resize-handle-sx">
      <span class="zjsx"></span>
      <RightOutlined
        v-if="leftWidth == 0"
        @click.stop="toggleCollapse"
        class="collapse-icon"
      />
      <LeftOutlined v-else class="collapse-icon" @click.stop="toggleCollapse" />
    </div>
  </div>
  <CatEditDialog ref="catEditDialogRef" @submit="handleCatSubmit" />
</template>

<script setup>
import {
  ref,
  defineProps,
  defineEmits,
  watch,
  computed,
  getCurrentInstance,
  onMounted,
  onUnmounted,
  h,
} from "vue";
import {
  SearchOutlined,
  PlusOutlined,
  CopyOutlined,
  EditOutlined,
  DeleteOutlined,
  MoreOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  FileTextOutlined,
  RightOutlined,
  LeftOutlined,
  ReloadOutlined,
  MenuFoldOutlined,
} from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const props = defineProps({
  deptOptions: Array,
  leftWidth: {
    type: Number,
    default: 300,
  },
  placeholder: {
    type: String,
    default: "请输入部门名称",
  },
  defaultExpand: {
    type: Boolean,
    default: false,
  },
  editable: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: "目录",
  },
  api: {
    type: Object,
    default: () => ({}),
  },
  extraParams: {
    type: Object,
    default: () => ({}),
  },
});
const emit = defineEmits([
  "node-click",
  "update:deptName",
  "update:leftWidth",
  "node-add",
  "node-edit",
  "node-delete",
]);

// 1. 初始化高度
const qtWrapheight = ref("86vh");
let resizeObserver = null;
import CatEditDialog from "@/components/Cat/catEditDialog";
const catEditDialogRef = ref(null);
const processedData = ref([]);

const dialogTreeOptions = computed(() => {
  return [
    {
      id: "0",
      name: props.title,
      children: processedData.value,
    },
  ];
});

// ===================== 树数据与展示 =====================
// 合成根节点（id=0，标题在头部展示，树主体只显示其 children）
const rootData = computed(() => {
  const data = processedData.value || [];
  if (
    data.length === 1 &&
    data[0] &&
    String(data[0].id) === "0" &&
    Array.isArray(data[0].children)
  ) {
    return data[0];
  }
  return null;
});

const headerNode = computed(() => {
  if (rootData.value) return rootData.value;
  return { id: "0", name: props.title, children: processedData.value || [] };
});

// 树主体展示数据：有合成根则展示其 children，否则原样展示
const displaySource = computed(() => {
  return rootData.value ? rootData.value.children || [] : processedData.value || [];
});

// 搜索过滤
const deptName = ref("");
const preFilterExpandedKeys = ref(null);

function nodeMatches(node, keyword) {
  return (node.name || "").indexOf(keyword) !== -1;
}

function filterTree(nodes, keyword) {
  const result = [];
  for (const node of nodes || []) {
    const children = filterTree(node.children, keyword);
    if (nodeMatches(node, keyword) || children.length) {
      result.push({ ...node, children });
    }
  }
  return result;
}

function collectKeysWithChildren(nodes, acc = []) {
  for (const node of nodes || []) {
    if (node.children && node.children.length) {
      acc.push(node.id);
      collectKeysWithChildren(node.children, acc);
    }
  }
  return acc;
}

const displayData = computed(() => {
  const keyword = (deptName.value || "").trim();
  if (!keyword) return displaySource.value;
  return filterTree(displaySource.value, keyword);
});

watch(deptName, (val) => {
  const keyword = (val || "").trim();
  if (keyword) {
    if (preFilterExpandedKeys.value === null) {
      preFilterExpandedKeys.value = [...expandedKeys.value];
    }
    expandedKeys.value = collectKeysWithChildren(displayData.value);
  } else if (preFilterExpandedKeys.value !== null) {
    expandedKeys.value = preFilterExpandedKeys.value;
    preFilterExpandedKeys.value = null;
  }
  emit("update:deptName", val);
});

// ===================== 选中与展开 =====================
const deptTreeRef = ref(null);
const leftWidth = ref(props.leftWidth);
const expandedKeys = ref([]);
const selectedKeys = ref([]);
const currentNodeKey = ref(null);
const activeDropdownNodeId = ref(null);

const selectedAll = computed(() => String(currentNodeKey.value) === "0");

function isExpanded(data) {
  return expandedKeys.value.includes(data.id);
}

function handleTreeExpand(keys) {
  expandedKeys.value = keys;
}

function handleTreeSelect(keys, info) {
  if (!info || !info.selected || !keys.length) return;
  const node = findNodeByKey(keys[0]);
  if (!node) return;
  selectedKeys.value = [node.id];
  currentNodeKey.value = node.id;
  emit("node-click", node);
}

function handleSelectAll() {
  selectedKeys.value = [];
  currentNodeKey.value = headerNode.value.id;
  emit("node-click", headerNode.value);
}

function findNodeByKey(key, nodes = displaySource.value) {
  for (const node of nodes || []) {
    if (String(node.id) === String(key)) return node;
    if (node.children && node.children.length) {
      const found = findNodeByKey(key, node.children);
      if (found) return found;
    }
  }
  return null;
}

// ===================== 目录增删改（editable） =====================
function handleNodeAdd(data) {
  if (props.api.add) {
    catEditDialogRef.value.open({
      title: "新增" + props.title,
      nameLabel: props.title + "名称",
      treeOptions: dialogTreeOptions.value,
      form: {
        parentId: data.id,
        validFlag: true,
        ...props.extraParams,
      },
    });
  } else {
    emit("node-add", data);
  }
}

function handleNodeAddSibling(data) {
  const parentId = data.parentId || "0";
  if (props.api.add) {
    catEditDialogRef.value.open({
      title: "新增" + props.title + "同级",
      nameLabel: props.title + "名称",
      treeOptions: dialogTreeOptions.value,
      form: {
        parentId: parentId,
        validFlag: true,
        ...props.extraParams,
      },
    });
  } else {
    emit("node-add", { ...data, parentId });
  }
}

function handleCommand(command, data) {
  switch (command) {
    case "add":
      handleNodeAdd(data);
      break;
    case "addSibling":
      handleNodeAddSibling(data);
      break;
    case "edit":
      handleNodeEdit(data);
      break;
    case "delete":
      handleNodeDelete(data);
      break;
  }
}

function handleNodeEdit(data) {
  if (props.api.get) {
    props.api.get(data.id).then((response) => {
      catEditDialogRef.value.open({
        title: "编辑" + props.title,
        nameLabel: props.title + "名称",
        treeOptions: dialogTreeOptions.value,
        form: response.data,
      });
    });
  } else {
    emit("node-edit", data);
  }
}

function handleNodeDelete(data) {
  if (props.api.del) {
    proxy.$modal
      .confirm('是否确认删除"' + data.name + '"？')
      .then(function () {
        if (props.api.del) {
          return props.api.del(data.id);
        }
      })
      .then(() => {
        proxy.$modal.msgSuccess("删除成功");
        // 如果删除的是当前选中的节点，清空选中状态
        if (String(currentNodeKey.value) === String(data.id)) {
          currentNodeKey.value = null;
          selectedKeys.value = [];
          emit("node-click", {});
        }
        getDeptTree();
      });
  } else {
    emit("node-delete", data);
  }
}

function handleCatSubmit(formData) {
  if (formData.id) {
    if (props.api.update) {
      props.api.update(formData).then((response) => {
        proxy.$modal.msgSuccess("修改成功");
        getDeptTree();
      });
    }
  } else {
    if (props.api.add) {
      props.api.add(formData).then((response) => {
        proxy.$modal.msgSuccess("新增成功");
        // 如果是新增，自动展开父节点
        if (
          formData.parentId &&
          !expandedKeys.value.includes(formData.parentId)
        ) {
          expandedKeys.value.push(formData.parentId);
        }
        getDeptTree();
      });
    }
  }
}

function handleDropdownVisibleChange(visible, nodeId) {
  activeDropdownNodeId.value = visible ? nodeId : null;
}

// ===================== 数据加载 =====================
function getDeptTree() {
  if (props.api.list) {
    props.api.list(props.extraParams).then((response) => {
      const tree = proxy.handleTree(response.data, "id", "parentId");
      processedData.value = [
        {
          name: props.title,
          value: "",
          id: 0,
          children: tree,
        },
      ];
      const defaultExpanded = [0];
      if (expandedKeys.value.length === 0) {
        expandedKeys.value = defaultExpanded;
      } else if (!expandedKeys.value.some((k) => String(k) === "0")) {
        expandedKeys.value.unshift(0);
      }
    });
  } else if (props.deptOptions && props.deptOptions.length > 0) {
    processedData.value = props.deptOptions;
  }
}

function handleRefresh() {
  if (props.api.list) {
    getDeptTree();
  } else {
    // 无 api 时重新展开首层，给出刷新反馈
    expandedKeys.value = getIdsByLevel(displaySource.value, 1);
  }
}

watch(
  () => props.extraParams,
  (val) => {
    if (props.api.list) {
      getDeptTree();
    }
  },
  { deep: true }
);

onMounted(() => {
  if (props.api.list) {
    getDeptTree();
  }
});

function getIdsByLevel(nodes, level = 2, currentLevel = 1) {
  let ids = [];
  if (!nodes || currentLevel > level) return ids;

  for (const node of nodes) {
    ids.push(node.id);
    if (node.children && node.children.length > 0) {
      ids = ids.concat(getIdsByLevel(node.children, level, currentLevel + 1));
    }
  }
  return ids;
}

watch(
  () => props.deptOptions,
  (val) => {
    if (!props.api.list && Array.isArray(val)) {
      processedData.value = val;
    }
    if (Array.isArray(val) && val.length > 0) {
      if (
        val.length === 1 &&
        val[0] &&
        String(val[0].id) === "0" &&
        Array.isArray(val[0].children)
      ) {
        expandedKeys.value = props.defaultExpand
          ? getIdsByLevel(val[0].children, 1)
          : [val[0].id];
      } else {
        expandedKeys.value = getIdsByLevel(val, props.defaultExpand ? 2 : 1);
      }
      // 恢复选中节点在树中的高亮
      if (currentNodeKey.value !== null && findNodeByKey(currentNodeKey.value)) {
        selectedKeys.value = [currentNodeKey.value];
      }
    }
  },
  { immediate: true }
);

watch(
  () => props.leftWidth,
  (val) => {
    leftWidth.value = val;
  }
);

// ===================== 高度监听 =====================
const getQtWrapHeight = () => {
  const element = document.querySelector(".qt-wrap");
  if (element) {
    qtWrapheight.value = element.offsetHeight + "px";
  } else {
    qtWrapheight.value = "86vh";
  }
};

onMounted(() => {
  getQtWrapHeight();

  const targetElement = document.querySelector(".qt-wrap");
  if (targetElement) {
    resizeObserver = new ResizeObserver(() => {
      getQtWrapHeight();
    });
    resizeObserver.observe(targetElement);
  }

  window.addEventListener("resize", getQtWrapHeight);
});

onUnmounted(() => {
  if (resizeObserver) {
    const targetElement = document.querySelector(".qt-wrap");
    if (targetElement) {
      resizeObserver.unobserve(targetElement);
    }
    resizeObserver.disconnect();
  }
  window.removeEventListener("resize", getQtWrapHeight);
});

// ===================== 拖拽与折叠 =====================
const isResizing = ref(false);
let startX = 0;
const startResize = (event) => {
  isResizing.value = true;
  startX = event.clientX;
  document.addEventListener("mousemove", updateResize);
  document.addEventListener("mouseup", stopResize);
};
const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener("mousemove", updateResize);
  document.removeEventListener("mouseup", stopResize);
};
const updateResize = (event) => {
  if (isResizing.value) {
    const delta = event.clientX - startX;
    leftWidth.value += delta;
    startX = event.clientX;
    requestAnimationFrame(() => {});
  }
};

const toggleCollapse = () => {
  leftWidth.value = leftWidth.value === 0 ? 300 : 0;
  emit("update:leftWidth", leftWidth.value);
};

// ===================== 对外接口 =====================
const resetTree = () => {
  currentNodeKey.value = null;
  selectedKeys.value = [];
};

const setCurrentKey = (key) => {
  currentNodeKey.value = key;
  selectedKeys.value = key === null || key === undefined ? [] : [key];
};

defineExpose({ resetTree, getDeptTree, setCurrentKey, deptTreeRef });
</script>

<style scoped lang="scss">
.left-pane {
  background: transparent;
  overflow: hidden;
  flex-shrink: 0;

  :deep(.ant-layout-sider-children) {
    width: 100%;
  }
}

.left-tree {
  height: v-bind(qtWrapheight);
  padding: 12px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
  scrollbar-width: none;
  -ms-overflow-style: none;
  box-sizing: border-box;
  overflow: auto;
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;

  .header-title {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
    cursor: pointer;
    font-size: 14px;
    font-weight: 600;
    color: #1f2d3d;
    white-space: nowrap;
    overflow: hidden;

    .header-icon {
      font-size: 16px;
      color: var(--ant-primary-color, #1677ff);
    }

    .header-text {
      overflow: hidden;
      text-overflow: ellipsis;
    }

    &.is-active {
      color: var(--ant-primary-color, #1677ff);
    }
  }

  .header-actions {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    flex-shrink: 0;

    .header-btn {
      flex-shrink: 0;
      color: #7f8da3;

      &:hover {
        color: var(--ant-primary-color, #1677ff);
      }
    }
  }
}

.tree-search {
  margin-bottom: 10px;

  .filter-tree {
    width: 100%;
  }
}

.head-container {
  overflow: auto;
}

.custom-tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  padding-right: 6px;
  overflow: hidden;
  min-width: 0;

  .treelabel {
    margin-left: 8px;
    flex: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-family: PingFang SC;
    font-weight: 400;
    font-size: 13px;
    color: #3f4a5a;
    min-width: 0;
  }

  .operation-trigger {
    display: none;
    margin-left: 2px;
    cursor: pointer;
    align-items: center;
    transform: rotate(90deg); /* 旋转 90 度实现竖向三个点 */
    flex-shrink: 0;

    &.is-active {
      display: flex;
    }
  }

  &:hover {
    .operation-trigger {
      display: flex;
    }
  }

  .action-icon {
    font-size: 16px;
    color: #999;
  }
}

.zjiconimg {
  font-size: 12px;
}

.colorxz {
  color: var(--ant-primary-color, #1677ff);
}

.colorwxz {
  color: #8aaadc;
}

.iconimg {
  font-size: 15px;
}

.resize-bar {
  height: v-bind(qtWrapheight);
  cursor: ew-resize;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 12px;
  flex-shrink: 0;
}

.resize-handle-sx {
  width: 12px;
  text-align: center;
  position: relative;
}

.zjsx {
  display: none;
  width: 5px;
  height: 50px;
  border-left: 1px solid #ccc;
  border-right: 1px solid #ccc;
}

.collapse-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 18px;
  color: #7f8da3;
  cursor: pointer;
  z-index: 10;
  padding: 5px 2px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #e5eaf2;
  box-shadow: 0 4px 12px rgba(31, 45, 61, 0.08);

  &:hover {
    color: var(--ant-primary-color, #1677ff);
    border-color: #c9dcff;
  }
}

:deep(.filter-tree) {
  .ant-input-affix-wrapper {
    border-radius: 6px;
    background: #f8fafc;
  }
}

:deep(.dept-tree) {
  background: transparent;
  color: #3f4a5a;

  .ant-tree-treenode {
    padding: 2px 0;
    width: 100%;
  }

  .ant-tree-node-content-wrapper {
    flex: 1;
    min-width: 0;
    border-radius: 6px;
    line-height: 30px;
    transition: background-color 0.16s ease, color 0.16s ease;

    &:hover {
      background: #f6faff;
    }

    &.ant-tree-node-selected {
      background: #eef5ff;

      .custom-tree-node .treelabel {
        color: var(--ant-primary-color, #1677ff);
        font-weight: 500;
      }
    }
  }

  .ant-tree-switcher {
    color: #9aa8ba;
    line-height: 30px;
  }
}
</style>

<style lang="scss">
.dept-tree-dropdown {
  background-color: #ffffff !important;
  border: 1px solid #ebeef5 !important;
  padding: 4px 0 !important;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1) !important;

  .ant-dropdown-menu-item {
    color: #606266 !important;
    font-size: 14px !important;
    padding: 8px 16px !important;

    &:hover {
      background-color: #f5f7fa !important;
      color: #409eff !important;
    }

    &.delete-item {
      color: #f56c6c !important;
      border-top: 1px solid #f0f2f5;
      margin-top: 4px;
      padding-top: 12px !important;

      &:hover {
        background-color: #fef0f0 !important;
        color: #f56c6c !important;
      }
    }

    .anticon {
      margin-right: 8px;
    }
  }
}
</style>
