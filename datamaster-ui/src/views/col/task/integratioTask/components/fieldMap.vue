<template>
<!-- 输出组件的字段映射   -->
  <div class="container">
    <a-form label-position="left" :label-col="{ style: { width: '80px' } }" :model="readerForm">
      <a-row>
        <!-- 左侧拖拽列表 -->
        <a-col :span="8" :offset="3">
          <p>来源表字段：</p>
          <!-- 全选复选框 -->
          <a-checkbox style="margin-top: -20px" v-model:checked="leftSelectAll" :disabled="info"
            v-if="readerForm.tableFields.length > 0">全选</a-checkbox>
          <draggable tag="div" class="draggable-list" :list="readerForm.tableFields" animation="300" item-key="id" :disabled="info">
            <template v-slot:item="{ element, index }">
              <div class="draggable-item fixed-height">
                <div class="custom-draggable-item">
                  <a-checkbox v-model:checked="element.isChecked" @change="handleCheckedChange(index)"
                    :disabled="info" class="checkbox-left" />
                  <a-input v-if="!info" v-model:value="element.columnName" size="small" class="name-input" />
                  <span v-else class="column-name">{{ element.columnName }}</span>
                  <a-button v-if="!info" type="primary" danger size="small" @click.stop="deleteRow(index)">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                  <img src="../../../../../assets/system/images/dpp/mop.png" class="icon" />
                </div>
              </div>
            </template>
          </draggable>
        </a-col>

        <!-- 中间箭头列 -->
        <a-col :span="4">
          <div class="arrow-container">
            <div v-for="(arrow, index) in arrowRows" :key="index" class="arrow-row fixed-height">
              <div class="circle"></div>
              <div class="arrow-line" :class="{ 'show-arrow': arrow.showArrow }"></div>
              <div class="circle"></div>
            </div>
          </div>
        </a-col>

        <!-- 右侧拖拽列表 -->
        <a-col :span="8">
          <p>目标字段：</p>
          <!-- 全选复选框，仅当不是 hdfs 且有字段时显示 -->
          <a-checkbox v-if="readerForm.toColumnsList.length > 0" :disabled="type == 'hdfs' || info"
            v-model:checked="rightSelectAll" style="margin-top: -20px">
            全选
          </a-checkbox>
          <!-- 拖拽区域 -->
          <draggable tag="div" class="draggable-list" :list="readerForm.toColumnsList" animation="300" item-key="id" :disabled="info">
            <template v-slot:item="{ element, index }">
              <div class="draggable-item fixed-height">
                <div class="custom-draggable-item">
                  <!-- 使用 tooltip 提示禁用原因 -->
                  <a-tooltip v-if="type === 'hdfs'" title="HDFS 类型不可勾选" placement="top">
                    <a-checkbox v-model:checked="element.isChecked" :disabled="true" class="checkbox-left" />
                  </a-tooltip>
                  <!-- 正常复选框 -->
                  <a-checkbox v-else v-model:checked="element.isChecked" @change="handleCheckedChange(index)"
                    :disabled="info" class="checkbox-left" />
                  <a-input v-if="!info" v-model:value="element.columnName" size="small" class="name-input" />
                  <span v-else class="column-name">{{ element.columnName }}</span>
                  <a-button v-if="!info" type="primary" danger size="small" @click.stop="deleteRow(index)">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>

                  <!-- 图标 -->
                  <img src="../../../../../assets/system/images/dpp/mop.png" class="icon" />
                </div>
              </div>
            </template>
          </draggable>
        </a-col>

        <a-col :span="24">
          <div style="text-align:center;margin-top:12px">
            <a-button v-if="!info" type="primary" size="small" html-type="button" @click="addRow">+ 添加一行</a-button>
          </div>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script setup>
import { ref, watch, computed, defineExpose } from "vue";
import draggable from "vuedraggable";
import { DeleteOutlined } from "@ant-design/icons-vue";

// 定义 props
const props = defineProps({
  tableFields: {
    type: Array,
    default: () => [],
  },
  toColumnsList: {
    type: Array,
    default: () => [],
  },
  type: {
    type: String,
    default: '',
  },
  info: {
    type: Boolean,
    default: false,
  },
});

// 内部状态
const readerForm = ref({
  tableFields: [],
  toColumnsList: [],
});

// 初始化表单数据
const updateReaderForm = () => {
  readerForm.value.tableFields = Array.isArray(props.tableFields)
    ? props.tableFields.map((item) => ({
      ...item,
      isChecked: item.isChecked ?? false,
    }))
    : [];
  readerForm.value.toColumnsList = Array.isArray(props.toColumnsList)
    ? props.toColumnsList.map((item) => ({
      ...item,
      isChecked: props.type == 'hdfs' ? true : (item.isChecked ?? false),
    }))
    : [];
};

updateReaderForm();

// 监听 props 变化
watch(
  () => props.tableFields,
  (newVal) => {
    readerForm.value.tableFields = (newVal ?? []).map((item) => ({
      ...item,
      isChecked: item.isChecked ?? false,
    }));
  },
  { deep: true }
);

watch(
  () => props.toColumnsList,
  (newVal) => {
    readerForm.value.toColumnsList = (newVal ?? []).map((item) => ({
      ...item,
      isChecked: item.isChecked ?? false,
    }));
  },
  { deep: true }
);

// 判断对应行左右项是否都选中
const shouldShowArrow = (index) => {
  const fromChecked = readerForm.value.tableFields[index]?.isChecked || false;
  const toChecked = readerForm.value.toColumnsList[index]?.isChecked || false;
  return fromChecked && toChecked;
};

// 计算属性：每一行是否显示箭头
const arrowRows = computed(() => {
  // 获取较短的列表长度
  const length = Math.min(
    readerForm.value.toColumnsList.length,
    readerForm.value.tableFields.length
  );

  // 根据较短的列表来遍历
  return Array.from({ length }).map((_, index) => ({
    showArrow: shouldShowArrow(index),
  }));
});

// 全选计算属性：左侧
const leftSelectAll = computed({
  get() {
    return readerForm.value.tableFields.every((item) => item.isChecked);
  },
  set(value) {
    readerForm.value.tableFields.forEach((item) => {
      item.isChecked = value;
    });
  },
});

// 全选计算属性：右侧
const rightSelectAll = computed({
  get() {
    return readerForm.value.toColumnsList.every((item) => item.isChecked);
  },
  set(value) {
    readerForm.value.toColumnsList.forEach((item) => {
      item.isChecked = value;
    });
  },
});

const handleCheckedChange = (index) => {
  // 当单个项选中状态变化时，leftSelectAll 和 rightSelectAll 会自动通过计算属性更新
};

function addRow() {
  const now = Date.now();
  readerForm.value.tableFields = [...readerForm.value.tableFields, { columnName: '', isChecked: true, id: now }];
  readerForm.value.toColumnsList = [...readerForm.value.toColumnsList, { columnName: '', isChecked: true, id: now + 1 }];
}

function deleteRow(index) {
  readerForm.value.tableFields.splice(index, 1);
  readerForm.value.toColumnsList.splice(index, 1);
}

defineExpose({
  getColumns: () => ({
    fromColumns: readerForm.value.tableFields,
    toColumns: readerForm.value.toColumnsList,
  }),
});
</script>

<style lang="scss" scoped>
.container {
  margin-top: -20px;
}

/* 左右列表容器 */
.draggable-list {
  display: flex;
  flex-direction: column;
}

/* 固定高度 */
.fixed-height {
  height: 40px; // 根据需要调整固定高度
}

/* 拖拽项样式 */
.draggable-item {
  box-sizing: border-box;
  padding: 6px;
  background-color: #fdfdfd;
  border: 1px solid #eee;
  border-radius: 4px;
  margin-bottom: 0;
  cursor: move;
}

.custom-draggable-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.checkbox-left {
  flex-shrink: 0;
  margin-right: 4px;
}

.name-input {
  flex: 1;
  min-width: 0;
}
.name-input :deep(.el-input__wrapper) {
  padding: 0 4px;
  box-shadow: none !important;
  border: 1px solid transparent;
}
.name-input :deep(.el-input__wrapper:hover) {
  border-color: #dcdfe6;
}
.name-input :deep(.el-input__inner) {
  padding: 0;
  font-size: 12px;
  height: 28px;
}

.icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

/* 中间箭头列 */
.arrow-container {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  height: 100%;
  margin-top: 75px;
}

/* 每一行 */
.arrow-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0;
}

/* 小圆点 */
.circle {
  width: 10px;
  height: 10px;
  background-color: #0095ff;
  border-radius: 50%;
}

/* 箭头线：默认隐藏 */
.arrow-line {
  display: none;
}

/* 当条件满足时，显示整条横线和箭头 */
.arrow-line.show-arrow {
  display: block;
  width: 160px;
  height: 2px;
  background-color: #0095ff;
  position: relative;
}

/* 箭头尖 */
.arrow-line.show-arrow::after {
  content: "";
  position: absolute;
  right: 0;
  top: -3px;
  border-top: 4px solid transparent;
  border-bottom: 6px solid transparent;
  border-left: 8px solid #0095ff;
  transition: opacity 0.2s;
}
</style>
