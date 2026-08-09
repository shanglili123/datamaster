<template>
    <a-layout-sider :style="{ width: `${leftWidth}px`, marginLeft: leftWidth == 0 ? '-15px' : '0px', }" class="left-pane">
        <div class="left-tree">
            <div class="head-container">
                <a-tree class="dept-tree" :tree-data="deptOptions" :field-names="{ title: 'name', children: 'children', value: 'id' }"
                    :filter-node-method="filterNode" ref="deptTreeRef" highlight-current
                    :expanded-keys="expandedKeys" @select="(keys, e) => handleNodeClick(e.node.data)"
                    :default-expand-all="defaultExpand">
                    <template #title="{ data, selected }">
                        <span class="custom-tree-node">
                            <img class="node-icon" src="@/assets/da/asset/icon (3).png" alt=""
                                v-if="data.children && data.children.length" />
                            <FileTextOutlined class="zjiconimg colorwxz"
                                v-show="!selected && (!data.children || data.children.length == 0)" />
                            <FileTextOutlined class="zjiconimg colorxz"
                                v-show="selected && (!data.children || data.children.length == 0)" />
                            <span class="treelable" @click="getNode(data)">
                                {{ data.name }}
                            </span>
                        </span>
                    </template>
                </a-tree>
            </div>
        </div>
    </a-layout-sider>
</template>

<script setup>
import { ref, defineProps, defineEmits, watch, getCurrentInstance } from "vue";
import { FileTextOutlined } from "@ant-design/icons-vue";

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
});

const emit = defineEmits(["node-click", "update:deptName", "update:leftWidth"]);

const deptName = ref("");
const deptTreeRef = ref(null);
const leftWidth = ref(props.leftWidth);
const expandedKeys = ref([]);

// 等 deptOptions 加载后设置一级节点展开
watch(
    () => props.deptOptions,
    (val) => {
        if (Array.isArray(val) && val.length > 0) {
            expandedKeys.value = val.map((item) => item.id); // 展开第一层
        }
    },
    { immediate: true }
);

// 过滤节点
const filterNode = (value, data) => {
    if (!value) return true;
    return data.name.indexOf(value) !== -1;
};

watch(deptName, (val) => {
    if (deptTreeRef.value) {
        deptTreeRef.value.filter(val);
    }
});

watch(
    () => props.leftWidth,
    (val) => {
        leftWidth.value = val;
    }
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
        requestAnimationFrame(() => { });
    }
};

// 折叠展开
const toggleCollapse = () => {
    if (leftWidth.value === 0) {
        leftWidth.value = 300;
    } else {
        leftWidth.value = 0;
    }
    emit("update:leftWidth", leftWidth.value);
};

function handleNodeClick(data) {
    emit("node-click", data);
}

const getNode = (node) => {
    console.log(node);
};

const resetTree = () => {
    if (deptTreeRef.value) {
        proxy.$refs.deptTreeRef.setCurrentKey(null);
    }
};

defineExpose({ resetTree });
</script>

<style scoped lang="scss">
.left-pane {
    background: transparent;
    overflow: hidden;
    height: 660px;
    display: flex;
    flex-direction: column;
}

.left-tree {
    padding: 14px;
    flex: 1;
    overflow-y: auto;
    background: #ffffff;
    border: 1px solid #e8edf5;
    border-radius: 8px;
    box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
    scrollbar-width: thin;
}

.left-tree::-webkit-scrollbar {
    width: 6px;
}

.left-tree::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 3px;
}

.left-tree::-webkit-scrollbar-track {
    background: transparent;
}

.custom-tree-node {
    width: 100%;
    display: flex;
    align-items: center;
    min-width: 0;
    padding: 0 10px;

    .node-icon {
        width: 16px;
        height: 16px;
    }

    .treelable {
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
}

.zjiconimg {
    font-size: 12px;
}

.colorxz {
    color: var(--el-color-primary);
}

.colorwxz {
    color: #8aaadc;
}

.resize-bar {
    height: 660px;
    cursor: ew-resize;
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
    --el-tree-node-hover-bg-color: #f6faff;
    background: transparent;

    &.el-tree--highlight-current .el-tree-node.is-current>.el-tree-node__content {
        background: #eef5ff !important;
        border: none;
        border-radius: 6px;

        .custom-tree-node .treelable {
            color: var(--el-color-primary);
        }
    }

    .el-tree-node__content {
        position: relative;
        height: 34px;
        border-radius: 6px;
        margin: 2px 0;

        .el-tree-node__expand-icon {
            position: absolute;
            right: 10px;
            color: #9aa8ba;

            &>svg {
                background: none;
            }
        }
    }
}
</style>

