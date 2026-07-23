<template>
  <div class="qt-wrap">
    <div
      :class="[config.search ? '' : 'qt-wrap--search']"
      v-if="$slots.search || hasDataActions || config.actions.table.show"
      ref="searchSectionRef"
    >
      <div class="qt-wrap--search-inner">
        <slot name="search"></slot>
        <div class="search-query-btns" v-if="$slots.search">
          <el-button plain type="primary" @click="handleQuery">
            <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
          </el-button>
          <el-button @click="handleReset">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
          </el-button>
        </div>
        <div class="data-actions" v-if="hasDataActions">
          <slot name="actions-data"></slot>
        </div>
        <div class="table-actions" v-if="config.actions.table.show">
          <el-tooltip effect="dark" content="刷新" placement="top">
            <el-button
              circle
              v-show="config.actions.table.refresh"
              @click="handleRefreshClick"
            >
              <i class="iconfont icon-a-shuaxinxianxing"></i>
            </el-button>
          </el-tooltip>

          <el-tooltip effect="dark" content="隐藏列" placement="top">
            <el-dropdown
              trigger="click"
              :hide-on-click="false"
              v-show="config.actions.table.columns"
              popper-class="columns-popper"
            >
              <el-button circle icon="Menu" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="item in props.columns"
                    :key="item.prop"
                  >
                    <el-checkbox
                      v-show="item?.type != 'selection'"
                      :checked="!item.hide"
                      :label="item.label"
                      @change="handleCheckboxChange($event, item)"
                    />
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-tooltip>
        </div>
      </div>
    </div>
    <div :class="['qt-wrap--content', config.fullContent ? 'full' : '']">
      <div class="qt-wrap--main" v-if="$slots.default">
        <slot name="default" />
      </div>
    </div>
  </div>
</template>

<script setup name="QtWrap">
import { computed, useSlots, ref, provide } from "vue";
import { merge } from "lodash-es";

const DEFAULT_CONFIG = {
  fullContent: true,
  actions: {
    show: true,
    table: {
      show: true,
      search: true,
      refresh: true,
      columns: true,
    },
  },
};

const props = defineProps({
  config: {
    type: Object,
    default: () => {
      return {};
    },
  },
  columns: {
    type: Array,
    default: () => {
      return [];
    },
  },
  tableRef: {
    type: Object,
    default: () => {
      return {};
    },
  },
});

const slots = useSlots();
const searchSectionRef = ref(null);
const searchBarRef = ref(null);

const config = computed(() => {
  return merge({}, DEFAULT_CONFIG, props.config);
});

const hasDataActions = computed(() => {
  return Boolean(slots["actions-data"]);
});

// 提供注册方法给 QtSearchBar
provide('qtWrapRegisterSearchBar', (instance) => {
  searchBarRef.value = instance;
});

// 查询
function handleQuery() {
  props.tableRef?.getList?.();
}

// 重置
function handleReset() {
  searchBarRef.value?.handleResetClick?.();
  props.tableRef?.resetQuery?.();
}

// 刷新
function handleRefreshClick() {
  props.tableRef.getList();
}

// 隐藏列
function handleCheckboxChange(checked, item) {
  item.hide = !checked;
}
</script>

<style lang="scss" scoped>
.qt-wrap {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.qt-wrap--search {
  padding: 14px 16px 2px;
  background: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
}

.qt-wrap--search-inner {
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-start;
  gap: 8px;

  :deep(.qt-search-bar) {
    flex: 0 1 auto;
  }

  :deep(.qt-search-bar .el-form) {
    flex-wrap: nowrap !important;
    row-gap: 0;
  }

  :deep(.qt-search-bar .el-form-item) {
    flex-shrink: 0;
    margin-bottom: 0;
  }

  :deep(.qt-search-bar .search-btns) {
    display: none !important;
  }
}

.search-query-btns {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;

  .el-button {
    height: 32px;
    padding: 8px 12px;
    font-size: 12px;
    border-radius: 6px;
  }
}

.qt-wrap--actions-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.qt-wrap--content {
  background-color: #ffffff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
  overflow: hidden;
}

.qt-wrap--content.full {
  flex: 1;
  padding: 14px 16px;
  min-height: calc(100vh - 250px);
}

.qt-wrap--actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.data-actions {
  margin-left: auto;

  ::v-deep(.el-button) {
    height: 32px;
    padding: 8px 12px;
    font-size: 12px;
    border-radius: 6px;
  }
}

.data-actions,
.table-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  ::v-deep(.el-button + .el-button) {
    margin-left: 0;
  }
}

.table-actions {
  gap: 8px;

  ::v-deep(.el-button.is-circle) {
    width: 32px;
    height: 32px;
    border-radius: 6px;
    background: #f7f9fc;
    border-color: #e5eaf2;
    color: #4e5969;

    &:hover {
      background: #eef5ff;
      border-color: #c9dcff;
      color: var(--el-color-primary);
    }
  }
}
</style>

<style lang="scss">
.columns-popper {
  .el-dropdown-menu__item {
    line-height: 30px;
    padding: 0px 17px;
  }
}
</style>

