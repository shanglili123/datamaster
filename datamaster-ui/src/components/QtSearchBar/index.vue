<template>
  <div class="qt-search-bar">
    <el-form
      ref="formRef"
      :model="props.params"
      :inline="true"
      @submit.prevent
      v-bind="props.config?.form"
    >
      <el-form-item
        :label="item.label"
        :prop="item.prop"
        v-for="(item, index) in props.items"
        :key="item.prop"
        v-bind="getFormItemProps(item)"
        v-show="index < props.visibleCount ? true : store.expand"
      >
        <!-- 输入框 -->
        <el-input
          class="search-content"
          v-if="item.component.is == 'input'"
          v-model="props.params[item.prop]"
          clearable
          :placeholder="`请输入${item.label}`"
          v-bind="item.component"
          style="width: 150px"
          @keyup.enter="handleQueryClick"
        />

        <!-- 下拉框 -->
        <el-select
          class="search-content"
          v-if="item.component.is == 'select'"
          v-model="props.params[item.prop]"
          clearable
          :placeholder="`请选择${item.label}`"
          v-bind="item.component"
          style="width: 150px"
        >
          <el-option
            v-for="(option, index) in item.component.options"
            :key="index"
            v-bind="option"
          />
        </el-select>

        <!-- 时间选择器 -->
        <el-date-picker
          class="search-content"
          v-if="item.component.is == 'date-picker'"
          v-model="props.params[item.prop]"
          :type="item.component.type || 'date'"
          clearable
          :placeholder="`请选择${item.label}`"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          v-bind="item.component"
          style="width: 200px"
        />

        <!-- 树形选择框 -->
        <el-tree-select
          class="search-content"
          v-if="item.component.is == 'tree-select'"
          v-model="props.params[item.prop]"
          clearable
          :placeholder="`请选择${item.label}`"
          v-bind="item.component"
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item class="search-btns" v-if="props.showButtons">
        <el-button plain type="primary" @click="handleQueryClick">
          <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
        </el-button>
        <el-button @click="handleResetClick">
          <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
        </el-button>
        <el-button
          plain
          type="primary"
          v-if="store.length > props.visibleCount"
          @click="store.expand = !store.expand"
          class="extend-btn"
        >
          <svg-icon v-if="store.expand" icon-class="toggle" />
          <svg-icon v-else icon-class="expand" />
          <span>{{ store.expand ? "折叠" : "展开" }}</span>
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup name="QtSearchBar">
import { reactive, ref, inject, onMounted, getCurrentInstance } from "vue";

const props = defineProps({
  params: {
    type: Object,
    required: true,
  },
  config: {
    type: Object,
    default: () => {
      return {};
    },
  },
  items: {
    type: Array,
    required: true,
  },
  tableRef: {
    type: Object,
    default: () => {
      return {};
    },
  },
  visibleCount: {
    type: Number,
    default: 3,
  },
  showButtons: {
    type: Boolean,
    default: true,
  },
});

const emits = defineEmits(["query"]);

const formRef = ref(null);

const store = reactive({
  expand: false,
  length: props.items.length,
});

// 过滤form参数
function getFormItemProps(item) {
  const { component, ...data } = item;
  return data;
}

// 查询
function handleQueryClick() {
  emits("query");
  props.tableRef?.getList();
}

// 重置
function handleResetClick() {
  formRef.value.resetFields();
  emits("reset");
  props.tableRef?.resetQuery();
}

defineExpose({ handleQueryClick, handleResetClick, formRef });

// 注册到 QtWrap 以便重置功能
const registerSearchBar = inject('qtWrapRegisterSearchBar', null);
const instance = getCurrentInstance();
onMounted(() => {
  registerSearchBar?.(instance?.proxy);
});
</script>

<style lang="scss" scoped>
.qt-search-bar {
  ::v-deep(.el-form) {
    display: flex !important;
    flex-wrap: nowrap !important;
    align-items: flex-start !important;
    column-gap: 12px;
  }

  ::v-deep(.el-form-item) {
    flex-shrink: 0 !important;
    margin-right: 0 !important;
    margin-bottom: 0 !important;
  }

  ::v-deep(.el-form-item__label) {
    height: 32px;
    line-height: 32px;
    color: #4e5969;
    font-weight: 500;
  }
}

.search-content {
  width: 150px;

  ::v-deep(.el-input__wrapper),
  ::v-deep(.el-select__wrapper) {
    min-height: 32px;
    border-radius: 6px;
    box-shadow: 0 0 0 1px #e2e8f0 inset;
    background: #fbfcfe;

    &:hover {
      box-shadow: 0 0 0 1px #c8d4e4 inset;
    }

    &.is-focus,
    &.is-focused {
      background: #ffffff;
      box-shadow: 0 0 0 1px var(--el-color-primary) inset;
    }
  }
}

.search-btns {
  margin-left: auto;

  .el-button {
    height: 32px;
    padding: 8px 12px;
    font-size: 12px;
    border-radius: 6px;
  }
}
.extend-btn {
  .svg-icon {
    font-size: 12px;
    margin-right: 6px;
  }
}
</style>
