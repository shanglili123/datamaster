<template>
  <el-aside
    :style="{
      width: `${leftWidth}px`,
      marginLeft: leftWidth == 0 ? '-15px' : '0px',
      '--qt-wrap-height': qtWrapheight,
    }"
    class="left-pane"
  >
    <div class="left-tree">
      <div class="head-container mb10">
        <el-input
          class="filter-tree"
          size="large"
          v-model="filterText"
          placeholder="请输入来源系统/数据源名称/库名"
          clearable
          prefix-icon="Search"
        />
      </div>
      <div class="tree-wrapper" v-loading="loading">
        <el-tree
          v-if="treeData.length > 0"
          ref="treeRef"
          class="dept-tree"
          :data="treeData"
          :props="defaultProps"
          node-key="nodeKey"
          highlight-current
          :default-expanded-keys="defaultExpandedKeys"
          :filter-node-method="filterNode"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <span class="custom-tree-node">
              <!-- 1级节点 - 使用自定义 zoom 图标 -->
              <svg-icon
                v-if="node.level === 1"
                icon-class="zoom"
                class="node-icon colorwxz"
              />
              <!-- 2级节点 - 使用 type 对应的图标 -->
              <img
                v-else-if="node.level == 2"
                :src="getDatasourceIcon(data.datasourceType)"
                class="node-icon"
              />

              <!-- 3级及以后节点 - 使用叶子节点图标 -->
              <svg-icon v-else icon-class="zbzc" class="node-icon colorwxz" />

              <span class="treelabel"> {{ node.label }} </span>
            </span>
          </template>
        </el-tree>
        <el-empty
          v-else-if="!loading"
          description="暂无来源系统数据"
          :image-size="40"
        />
      </div>
    </div>
  </el-aside>

  <!-- 拖拽栏 -->
  <div class="resize-bar" @mousedown="startResize">
    <div class="resize-handle-sx">
      <span class="zjsx"></span>
      <el-icon
        v-if="leftWidth == 0"
        @click.stop="toggleCollapse"
        class="collapse-icon"
      >
        <ArrowRight />
      </el-icon>
      <el-icon v-else class="collapse-icon" @click.stop="toggleCollapse">
        <ArrowLeft />
      </el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from "vue";
import {
  ArrowRight,
  ArrowLeft,
  Search,
  FolderOpened,
  Folder,
} from "@element-plus/icons-vue";
import { sourceSystemTree } from "@/api/cat/task/task";
import { getDatasourceIcon } from "@/utils/datasource";

const props = defineProps({
  initialLeftWidth: {
    type: Number,
    default: 300,
  },
});

const emit = defineEmits(["node-click", "data-loaded", "update:leftWidth"]);

const treeRef = ref(null);
const filterText = ref("");
const treeData = ref([]);
const flatData = ref([]);
const defaultExpandedKeys = ref([]);
const loading = ref(false);
const leftWidth = ref(props.initialLeftWidth);
const qtWrapheight = ref("86vh");
let resizeObserver = null;

const defaultProps = {
  children: "children",
  label: "name",
};

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
  leftWidth.value = leftWidth.value === 0 ? 300 : 0;
  emit("update:leftWidth", leftWidth.value);
};

// 高度监听逻辑
const getQtWrapHeight = () => {
  const element = document.querySelector(".qt-wrap");
  if (element) {
    qtWrapheight.value = element.offsetHeight + "px";
  } else {
    qtWrapheight.value = "86vh";
  }
};

watch(filterText, (val) => {
  if (treeRef.value) {
    treeRef.value.filter(val);
  }
});

const filterNode = (value, data) => {
  if (!value) return true;
  return data.name.includes(value);
};

const handleNodeClick = (data) => {
  emit("node-click", data);
};

const getTreeData = () => {
  loading.value = true;
  sourceSystemTree()
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

      // 获取第1级节点的key用于默认展开
      const getExpandedKeys = (list, level = 1) => {
        if (!Array.isArray(list)) return [];
        let keys = [];
        list.forEach((item) => {
          if (level === 1) {
            keys.push(item.nodeKey);
          }
          if (item.children && item.children.length > 0) {
            keys = keys.concat(getExpandedKeys(item.children, level + 1));
          }
        });
        return keys;
      };
      defaultExpandedKeys.value = getExpandedKeys(treeData.value);

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
      flatData.value = flatten(treeData.value);
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

const resetTree = () => {
  if (treeRef.value) {
    treeRef.value.setCurrentKey(null);
  }
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
  height: v-bind(qtWrapheight);
}

.left-tree {
  padding: 14px;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
}

.tree-wrapper {
  flex: 1;
  overflow: auto;
  min-height: 200px;
  padding-right: 2px;
}

.el-aside {
  padding: 2px 0;
  margin-bottom: 0px;
  background: transparent;
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
    color: var(--el-color-primary);
    border-color: #c9dcff;
  }
}

.mb10 {
  margin-bottom: 10px;
}

:deep(.filter-tree .el-input__wrapper) {
  min-height: 34px;
  border-radius: 6px;
  background: #f8fafc;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

:deep(.dept-tree) {
  --el-tree-node-hover-bg-color: #f6faff;
  background: transparent;
  color: #3f4a5a;

  .el-tree-node__content {
    height: 34px;
    border-radius: 6px;
    margin: 2px 0;
    transition: background-color 0.16s ease, color 0.16s ease;
  }

  &.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content {
    background: #eef5ff;
    color: var(--el-color-primary);
  }

  .el-tree-node__expand-icon {
    color: #9aa8ba;
  }
}
</style>

