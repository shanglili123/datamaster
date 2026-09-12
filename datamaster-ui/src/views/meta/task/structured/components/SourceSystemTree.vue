<template>
  <a-layout-sider
    :width="leftWidth || 1"
    :collapsed="leftWidth === 0"
    :collapsed-width="0"
    :trigger="null"
    :style="{
      marginLeft: leftWidth == 0 ? '-15px' : '0px',
    }"
    class="left-pane"
  >
    <div class="left-tree">
      <!-- 头部：第一行标题 + 刷新/收缩，第二行搜索框独占一行 -->
      <div class="tree-header">
        <span class="header-title">
          <FolderOpenOutlined class="header-icon" />
          <span class="header-text">目录</span>
        </span>
        <span class="header-actions">
          <a-tooltip title="刷新目录">
            <a-button type="text" size="small" class="header-btn" @click="getTreeData">
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
          v-model:value="filterText"
          :placeholder="treeType === 'dbTable' ? '库名/表名' : '来源系统/数据源/库名'"
          allow-clear
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
      </div>
      <div class="tree-wrapper">
        <a-spin :spinning="loading">
          <a-tree
            v-if="treeData.length > 0"
            ref="treeRef"
            class="dept-tree"
            :tree-data="visibleTreeData"
            :field-names="{ title: 'name', key: 'nodeKey', children: 'children' }"
            :selected-keys="selectedKeys"
            :expanded-keys="expandedKeys"
            @select="handleSelect"
            @expand="handleExpand"
          >
            <template #title="{ data }">
              <span class="custom-tree-node">
                <!-- 来源系统节点 - 使用自定义 zoom 图标 -->
                <svg-icon
                  v-if="data.type === 'SOURCE'"
                  icon-class="zoom"
                  class="node-icon colorwxz"
                />
                <!-- 数据源/数据库节点(带类型时) - 使用 type 对应的图标 -->
                <img
                  v-else-if="
                    (data.type === 'DATASOURCE' || data.type === 'DATABASE') &&
                    data.datasourceType
                  "
                  :src="getDatasourceIcon(data.datasourceType)"
                  class="node-icon"
                />

                <!-- 表节点/无类型的数据库节点 - 使用叶子节点图标 -->
                <svg-icon v-else icon-class="zbzc" class="node-icon colorwxz" />

                <a-tooltip :title="data.name" placement="top-start" :disabled="!data.name">
                  <span class="treelabel"> {{ data.name }} </span>
                </a-tooltip>
              </span>
            </template>
          </a-tree>
          <a-empty
            v-else-if="!loading"
            :description="treeType === 'dbTable' ? '暂无库表数据' : '暂无来源系统数据'"
            :image="Empty.PRESENTED_IMAGE_SIMPLE"
          />
        </a-spin>
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
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import useUserStore from "@/store/system/user";
import { Empty } from "ant-design-vue";
import {
  LeftOutlined,
  RightOutlined,
  SearchOutlined,
  FolderOpenOutlined,
  ReloadOutlined,
  MenuFoldOutlined,
} from "@ant-design/icons-vue";
import { sourceSystemTree, dbTableTree } from "@/api/cat/task/task";
import { getDatasourceIcon } from "@/utils/datasource";

const props = defineProps({
  initialLeftWidth: {
    type: Number,
    default: 240,
  },
  // 树形模式: sourceSystem-来源系统三级树(默认), dbTable-库表两级树
  treeType: {
    type: String,
    default: "sourceSystem",
  },
});

const userStore = useUserStore();

const emit = defineEmits(["node-click", "data-loaded", "update:leftWidth"]);

const treeRef = ref(null);
const filterText = ref("");
const treeData = ref([]);
const flatData = ref([]);
const expandedKeys = ref([]);
const selectedKeys = ref([]);
const loading = ref(false);
const leftWidth = ref(props.initialLeftWidth);

// 折叠/展开状态持久化：按树类型分别存储，刷新或重新进入页面时保持用户的手动折叠状态
const STORAGE_PREFIX = "dm-source-system-tree";
const storageKey = `${STORAGE_PREFIX}-${props.treeType}-expanded`;
const storageWidthKey = `${STORAGE_PREFIX}-${props.treeType}-width`;

function restoreState() {
  try {
    const saved = localStorage.getItem(storageKey);
    if (saved) {
      const keys = JSON.parse(saved);
      if (Array.isArray(keys)) expandedKeys.value = keys;
    }
    const savedWidth = localStorage.getItem(storageWidthKey);
    if (savedWidth !== null) {
      const restoredWidth = Number(savedWidth);
      // 兼容旧版本将默认展开宽度持久化为 300px 的数据
      leftWidth.value = restoredWidth === 300
        ? (props.initialLeftWidth || 240)
        : (restoredWidth || props.initialLeftWidth);
    }
  } catch (e) {
    // 忽略异常，使用默认折叠状态
  }
}
restoreState();

// 过滤树数据（保留匹配节点及其祖先）
function filterTreeData(list, keyword) {
  if (!keyword) return list;
  return list.reduce((acc, item) => {
    const children = item.children
      ? filterTreeData(item.children, keyword)
      : [];
    if (item.name.includes(keyword) || children.length > 0) {
      acc.push({ ...item, children });
    }
    return acc;
  }, []);
}

const visibleTreeData = computed(() =>
  filterTreeData(treeData.value, filterText.value)
);

// 拖拽逻辑
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
  }
};

// 折叠展开
const toggleCollapse = () => {
  leftWidth.value = leftWidth.value === 0 ? (props.initialLeftWidth || 240) : 0;
  try {
    localStorage.setItem(storageWidthKey, String(leftWidth.value));
  } catch (e) {
    // 忽略存储异常
  }
  emit("update:leftWidth", leftWidth.value);
};

const handleSelect = (keys, e) => {
  selectedKeys.value = keys;
  // ant-design-vue 4.x 的 select 事件节点对象上原始数据在 dataRef，没有 .data 属性
  const nodeData = e.node?.dataRef ?? e.node ?? null;
  emit("node-click", nodeData);
};

// 用户手动展开/折叠节点时记录状态，避免数据刷新后被强制重置
const handleExpand = (keys) => {
  expandedKeys.value = keys;
  try {
    localStorage.setItem(storageKey, JSON.stringify(keys));
  } catch (e) {
    // 忽略存储异常
  }
};

const getTreeData = () => {
  loading.value = true;
  const fetchApi = props.treeType === "dbTable" ? dbTableTree : sourceSystemTree;
  fetchApi({ spaceId: userStore.spaceId, spaceCode: userStore.spaceCode })
    .then((res) => {
      loading.value = false;
      if (!res || !res.data) {
        treeData.value = [];
        flatData.value = [];
        return;
      }
      const formatData = (list) => {
        if (!Array.isArray(list)) return [];
        return list.map((item) => {
          const node = {
            ...item,
            nodeKey: `${item.type}_${item.id}`,
          };
          if (item.children && item.children.length > 0) {
            node.children = formatData(item.children);
          }
          return node;
        });
      };
      treeData.value = formatData(res.data);

      // 扁平化数据用于查找
      const flatten = (list) => {
        if (!Array.isArray(list)) return [];
        let result = [];
        list.forEach((item) => {
          result.push(item);
          if (item.children && item.children.length > 0) {
            result = result.concat(flatten(item.children));
          }
        });
        return result;
      };
      const flat = flatten(treeData.value);
      flatData.value = flat;

      // 保留用户手动展开的节点，剔除已不存在的节点 key，避免误展开
      const existKeys = new Set(flat.map((item) => item.nodeKey));
      expandedKeys.value = expandedKeys.value.filter((key) => existKeys.has(key));

      emit("data-loaded", {
        treeData: treeData.value,
        flatData: flatData.value,
      });
    })
    .catch((err) => {
      loading.value = false;
      console.error("Failed to fetch source system tree:", err);
    });
};

onMounted(() => {
  getTreeData();
});

const resetTree = () => {
  selectedKeys.value = [];
};

defineExpose({
  getTreeData,
  resetTree,
  flatData,
  treeData,
});
</script>

<style lang="scss" scoped>
.left-pane {
  background: transparent;
  overflow: hidden;
  flex-shrink: 0;

  :deep(.ant-layout-sider-children) {
    width: 100%;
    height: 100%;
  }
}

.left-tree {
  height: 100%;
  padding: 12px;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
  overflow: hidden;
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
  flex-shrink: 0;

  .filter-tree {
    width: 100%;
  }
}

.tree-wrapper {
  flex: 1;
  overflow: auto;
  min-height: 200px;
  padding-right: 2px;
}


.custom-tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  padding: 0 10px;
  overflow: hidden;
  min-width: 0;

  .node-icon {
    width: 16px;
    height: 16px;
    margin-right: 8px;
    flex-shrink: 0;
    font-size: 15px;
  }

  .treelabel {
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
}

.colorxz {
  color: var(--el-color-primary);
}

.colorwxz {
  color: #8aaadc;
}

.resize-bar {
  /* 高度由 flex 行布局拉伸（ant-layout 高度由内容决定，height:100% 会解析为 0） */
  align-self: stretch;
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
    color: var(--el-color-primary);
    border-color: #c9dcff;
  }
}

:deep(.dept-tree) {
  background: transparent;
  color: #3f4a5a;

  .ant-tree-node-content-wrapper {
    height: 34px;
    line-height: 34px;
    border-radius: 6px;
    margin: 2px 0;
    transition: background-color 0.16s ease, color 0.16s ease;
  }

  .ant-tree-treenode {
    &.ant-tree-treenode-selected > .ant-tree-node-content-wrapper {
      background: #eef5ff;
      color: var(--el-color-primary);
    }
  }

  .ant-tree-switcher {
    color: #9aa8ba;
  }
}
</style>

