<template>
  <div class="page-header-wrapper" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">

    <!-- 左侧：搜索区域 -->
    <div class="search-section" style="flex-shrink: 0;">
      <a-form
        :model="queryParams"
        ref="queryRef"
        :layout="'inline'"
        v-show="showSearch"
        :label-col="{ style: { width: '125px' } }"
      >
        <!-- 核心：使用默认插槽，让父页面自定义具体的搜索项 -->
        <slot name="searchForm"></slot>

        <!-- 固定的查询/重置按钮 -->
        <a-form-item>
          <a-button type="primary" @click="handleQuery">
            <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
          </a-button>
          <a-button @click="handleReset">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <!-- 右侧：操作按钮组 -->
    <div class="actions-section" style="display: flex; gap: 10px; align-items: center;">

      <!-- 新增按钮：通过 prop 控制权限和显示 -->
      <a-button
        v-if="showAddBtn"
        type="primary"
        :icon="h(PlusOutlined)"
        @click="handleAdd"
        v-hasPermi="addPermission"
      >
        新增
      </a-button>

      <!-- 展开/折叠按钮：通过 prop 控制显示 -->
      <a-button
        v-if="showToggleBtn"
        class="toggle-expand-all"
        type="primary"
        @click="handleToggle"
      >
        <svg-icon v-if="isExpandAll" icon-class="toggle" />
        <svg-icon v-else icon-class="expand" />
        <span>{{ isExpandAll ? "折叠" : "展开" }}</span>
      </a-button>

      <!-- 工具栏：通常每个页面都需要，直接放置 -->
      <right-toolbar :showSearch="showSearch" @update:showSearch="$emit('update:showSearch', $event)" @queryTable="emitQuery"></right-toolbar>
    </div>

  </div>
</template>

<script setup>
import { defineProps, defineEmits, h } from 'vue';
import { PlusOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  // 查询参数对象，用于重置时清空
  queryParams: {
    type: Object,
    required: true
  },
  // 是否显示搜索栏
  showSearch: {
    type: Boolean,
    default: true
  },
  // 是否显示新增按钮
  showAddBtn: {
    type: Boolean,
    default: true
  },
  // 新增按钮的权限标识
  addPermission: {
    type: Array,
    default: () => []
  },
  // 是否显示展开/折叠按钮
  showToggleBtn: {
    type: Boolean,
    default: false
  },
  // 当前展开状态
  isExpandAll: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['query', 'reset', 'add', 'toggle', 'queryTable', 'update:showSearch']);

// 触发查询
const handleQuery = () => {
  emit('query');
};

// 触发重置
const handleReset = () => {
  emit('reset');
};

// 触发新增
const handleAdd = () => {
  emit('add');
};

// 触发展开/折叠
const handleToggle = () => {
  emit('toggle');
};

// 触发工具栏查询
const emitQuery = () => {
  emit('queryTable');
};
</script>

<style scoped>
/* 如果需要额外的样式调整可以在这里添加 */
.page-header-wrapper {
  min-height: 32px; /* 防止高度塌陷 */
}

/* flex + gap 已控制按钮间距，覆盖全局 .ant-btn + .ant-btn 的 margin-left */
.actions-section :deep(.ant-btn + .ant-btn) {
  margin-left: 0;
}
</style>