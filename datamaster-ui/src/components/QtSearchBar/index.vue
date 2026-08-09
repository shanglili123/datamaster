<template>
  <div class="qt-search-bar">
    <a-form
      ref="formRef"
      :model="props.params"
      layout="inline"
      @submit.prevent
      v-bind="props.config?.form"
    >
      <a-form-item
        :label="item.label"
        :name="item.prop"
        v-for="(item, index) in props.items"
        :key="item.prop"
        v-bind="getFormItemProps(item)"
        v-show="index < props.visibleCount ? true : store.expand"
      >
        <!-- 输入框 -->
        <a-input
          class="search-content"
          v-if="item.component.is == 'input'"
          v-model:value="props.params[item.prop]"
          allow-clear
          :placeholder="`请输入${item.label}`"
          v-bind="item.component"
          style="width: 150px"
          @keyup.enter="handleQueryClick"
        />

        <!-- 下拉框 -->
        <a-select
          class="search-content"
          v-if="item.component.is == 'select'"
          v-model:value="props.params[item.prop]"
          allow-clear
          :placeholder="`请选择${item.label}`"
          v-bind="item.component"
          style="width: 150px"
        >
          <a-select-option
            v-for="(option, index) in item.component.options"
            :key="index"
            v-bind="option"
          />
        </a-select>

        <!-- 时间选择器 -->
        <a-date-picker
          class="search-content"
          v-if="item.component.is == 'date-picker'"
          v-model:value="props.params[item.prop]"
          :type="item.component.type || 'date'"
          :allow-clear="true"
          :placeholder="`请选择${item.label}`"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          v-bind="item.component"
          style="width: 200px"
        />

        <!-- 树形选择框 -->
        <a-tree-select
          class="search-content"
          v-if="item.component.is == 'tree-select'"
          v-model:value="props.params[item.prop]"
          allow-clear
          :placeholder="`请选择${item.label}`"
          v-bind="item.component"
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item class="search-btns" v-if="props.showButtons">
        <a-button type="primary" @click="handleQueryClick">
          <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
        </a-button>
        <a-button @click="handleResetClick">
          <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
        </a-button>
        <a-button
          type="primary"
          v-if="store.length > props.visibleCount"
          @click="store.expand = !store.expand"
          class="extend-btn"
        >
          <svg-icon v-if="store.expand" icon-class="toggle" />
          <svg-icon v-else icon-class="expand" />
          <span>{{ store.expand ? "折叠" : "展开" }}</span>
        </a-button>
      </a-form-item>
    </a-form>
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
  :deep(.ant-form-inline) {
    display: flex !important;
    flex-wrap: nowrap !important;
    align-items: flex-start !important;
    column-gap: 12px;
  }

  :deep(.ant-form-item) {
    flex-shrink: 0 !important;
    margin-right: 0 !important;
    margin-bottom: 0 !important;
  }

  :deep(.ant-form-item-label) {
    height: 32px;
    line-height: 32px;
    color: #4e5969;
    font-weight: 500;
  }
}

.search-content {
  width: 150px;

  :deep(.ant-input-affix-wrapper),
  :deep(.ant-select-selector),
  :deep(.ant-picker) {
    min-height: 32px;
    border-radius: 6px;
    background: #fbfcfe;

    &:hover {
      border-color: #c8d4e4;
    }
  }

  :deep(.ant-input-affix-wrapper-focused),
  :deep(.ant-select-focused .ant-select-selector),
  :deep(.ant-picker-focused) {
    background: #ffffff;
    border-color: #1677ff;
  }
}

.search-btns {
  margin-left: auto;

  .ant-btn {
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
