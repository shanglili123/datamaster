<!--
    ProTable 组件
    说明：基于 Ant Design Vue 的通用表格封装，支持列插槽、字典/时间格式化、自定义排序与分页
    作者：datamaster
-->
<template>
  <div class="pro-table">
    <a-spin :spinning="conf.loading">
      <a-table
        :data-source="conf.data"
        :columns="antColumns"
        v-bind="antTableProps"
        :row-key="rowKey"
        :row-selection="antRowSelection"
        :pagination="false"
        :locale="{ emptyText: emptyContent }"
        @change="onAntSortChange"
      >
        <template #headerCell="{ column }">
          <div
            v-if="
              column.headerConfig &&
              (column.headerConfig.label || column.headerConfig.tooltip)
            "
            class="justify-center"
          >
            <span style="margin-right: 2px">{{
              column.headerConfig.label || column.title
            }}</span>
            <a-tooltip
              v-if="column.headerConfig.tooltip"
              :title="column.headerConfig.tooltip"
              placement="top"
            >
              <span class="tip-icon">
                <InfoFilled />
              </span>
            </a-tooltip>
          </div>
          <span v-else>{{ column.title }}</span>
        </template>

        <template #bodyCell="{ column, record, index }">
          <!-- 操作列 -->
          <template v-if="column.__isActions">
            <slot name="actions" :row="record" :$index="index" />
          </template>
          <!-- 自定义插槽 -->
          <template v-else-if="column.slot">
            <slot
              :name="column.slot"
              :row="record"
              :$index="index"
              :column="column"
            />
          </template>
          <!-- 图片 -->
          <image-preview
            v-else-if="column.image"
            :src="getImageSrc(record, column)"
            :width="column.imageWidth || 50"
            :height="column.imageHeight || 50"
          />
          <!-- 图标 + 文本 -->
          <div v-else-if="column.iconGetter" class="justify">
            <img
              :src="column.iconGetter(record)"
              alt=""
              :style="
                column.iconGetter(record)
                  ? 'width: ' + (column.iconSize || 20) + 'px;margin-right: 5px;'
                  : ''
              "
            />
            <span @dblclick="onCellDblclick(record, column, index)">{{
              displayCell(record, column.prop)
            }}</span>
          </div>
          <!-- 字典 -->
          <dict-tag
            v-else-if="column.dictOptions"
            :options="column.dictOptions"
            :value="getDictValue(record, column)"
          />
          <!-- 时间 -->
          <span v-else-if="column.time">{{
            parseTime(
              record?.[column.prop],
              column.timeFormat || '{y}-{m}-{d} {h}:{i}'
            ) || '-'
          }}</span>
          <!-- cron -->
          <span v-else-if="column.cron">{{
            cronToZh(record?.[column.prop] || '-')
          }}</span>
          <!-- 默认 -->
          <span v-else>{{ displayCell(record, column.prop) }}</span>
        </template>
      </a-table>
    </a-spin>

    <div class="pro-table--pagination" v-if="showPagination">
      <a-pagination
        :total="paginationTotal"
        v-model:current="pageLocal"
        v-model:page-size="limitLocal"
        show-size-changer
        show-quick-jumper
        :show-total="(total) => `共 ${total} 条`"
        @change="onAntPagination"
        @showSizeChange="onAntPagination"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, h } from 'vue';
import { parseTime } from '@/utils/anivia';
import { cronToZh } from '@/utils/cronUtils';
import { InfoCircleFilled as InfoFilled } from '@ant-design/icons-vue';

const noDataImg = new URL(
  '@/assets/images/common/noDataImg.png',
  import.meta.url
).href;

/**
 * ProTable（通用表格）
 * 作者：datamaster
 *
 * 用法：
 * 1) 引入组件：import ProTable from '@/components/ProTable/index.vue'
 * 2) 定义列：const columns = [{ prop: 'name', label: '名称' }, { prop: 'status', label: '状态', dictOptions: [...] }]
 * 3) 准备数据与分页状态：list、loading、total、query({ pageNum, pageSize })
 * 4) 组装唯一入口 config：{ columns, data: list, loading, table, selection, actions, pagination }
 * 5) 模板中使用：<ProTable :config="tableConfig" @sort-change @pagination @update:selected> ...插槽 ...</ProTable>
 */
const props = defineProps({
  config: { type: Object, default: () => ({}) },
});

const emit = defineEmits([
  'update:selected',
  'selection-change',
  'sort-change',
  'pagination',
  'row-dblclick',
  'cell-dblclick',
]);

const cfgRef = computed(() => props.config || {});
const conf = computed(() => ({
  columns: cfgRef.value.columns || [],
  data: cfgRef.value.data || [],
  loading: cfgRef.value.loading || false,
  table: cfgRef.value.table || {},
  selection: cfgRef.value.selection || false,
  selectionSelectable: cfgRef.value.selectionSelectable,
  actions: cfgRef.value.actions || false,
  actionsWidth: cfgRef.value.actionsWidth ?? 240,
  actionsFixed: cfgRef.value.actionsFixed ?? 'right',
  pagination: cfgRef.value.pagination || null,
}));

const rowKey = computed(() => conf.value.table?.rowKey ?? 'id');

// ── 适配层 ──────────────────────────────

const antColumns = computed(() => {
  const cols = (conf.value.columns || [])
    .filter((c) => c.visible !== false)
    .map((c) => {
      const isSortable =
        c.sort === true || c.time === true || c.sortable === 'custom';
      const col = {
        title: c.label,
        dataIndex: c.prop,
        key: c.columnKey || c.sortKey || c.prop || c.label,
        align: c.align || 'left',
        width: c.width,
        ellipsis: (c.showOverflowTooltip ?? true) ? true : undefined,
        sorter: isSortable ? true : undefined,
        // 透传业务字段
        prop: c.prop,
        slot: c.slot,
        image: c.image,
        imageWidth: c.imageWidth,
        imageHeight: c.imageHeight,
        imageFallback: c.imageFallback,
        iconGetter: c.iconGetter,
        iconSize: c.iconSize,
        dictOptions: c.dictOptions,
        time: c.time,
        timeFormat: c.timeFormat,
        cron: c.cron,
        headerConfig: c.headerConfig,
        onDblclick: c.onDblclick,
      };
      Object.keys(col).forEach((k) => {
        if (col[k] === undefined) delete col[k];
      });
      return col;
    });

  // 操作列
  if (conf.value.actions) {
    cols.push({
      title: '操作',
      key: '__actions',
      align: 'center',
      width: conf.value.actionsWidth,
      fixed: conf.value.actionsFixed,
      __isActions: true,
    });
  }

  return cols;
});

const antTableProps = computed(() => {
  const extra = conf.value.table || {};
  const result = {};

  if (extra.stripe !== undefined) result.striped = extra.stripe;
  if (extra.border !== undefined) result.bordered = extra.border;
  if (extra.size) result.size = extra.size === 'default' ? 'middle' : extra.size;
  if (extra.height) result.scroll = { y: extra.height };

  // defaultSort 映射
  const ds = extra.defaultSort || {};
  if (ds.order) {
    const sortOrder =
      ds.order === 'asc' || ds.order === 'ascending' ? 'ascend'
      : ds.order === 'desc' || ds.order === 'descending' ? 'descend'
      : null;
    if (sortOrder && ds.prop) {
      result.defaultSortOrder = {};
    }
  }

  // row-dblclick → customRow
  result.customRow = (record, index) => ({
    dblclick: () => {
      emit('row-dblclick', record, null, null);
    },
  });

  return result;
});

const antRowSelection = computed(() => {
  if (!conf.value.selection) return undefined;

  const rule = conf.value.selectionSelectable;

  const getCheckboxProps = (record) => {
    if (!rule) return { disabled: false };
    let disabled = false;
    if (typeof rule === 'function') {
      disabled = !rule(record);
    } else if (typeof rule === 'string') {
      disabled = !!record?.[rule];
    } else if (typeof rule === 'object' && rule) {
      const { field, disabledValues } = rule;
      if (field) {
        const v = record?.[field];
        if (Array.isArray(disabledValues)) {
          disabled = disabledValues.map(String).includes(String(v));
        } else {
          disabled = !!v;
        }
      }
    }
    return { disabled };
  };

  return {
    type: 'checkbox',
    onChange: (selectedRowKeys, selectedRows) => {
      emit('update:selected', selectedRows);
      emit('selection-change', selectedRows);
    },
    getCheckboxProps: (record) => getCheckboxProps(record),
  };
});

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '暂无记录'),
]);

// ── 业务逻辑 ──────────────────────────────

function displayCell(row, prop) {
  const val = prop ? row?.[prop] : undefined;
  return val ?? '-';
}

function onAntSortChange(pag, filters, sorter) {
  const order =
    sorter.order === 'ascend'
      ? 'asc'
      : sorter.order === 'descend'
        ? 'desc'
        : sorter.order;
  const prop = sorter.field || sorter.column?.dataIndex;
  const col = (conf.value.columns || []).find((c) => c.prop === prop);
  emit('sort-change', {
    prop,
    order,
    column: col,
    sortKey: col?.sortKey,
  });
}

function onCellDblclick(row, col, index) {
  if (typeof col.onDblclick === 'function') {
    col.onDblclick(row, index);
  }
}

// 分页
const paginationConfig = computed(() => conf.value.pagination || null);
const showPagination = computed(() => {
  const cfg = paginationConfig.value;
  const total = cfg && typeof cfg.total !== 'undefined' ? Number(cfg.total) : 0;
  return !!cfg && total > 0;
});
const pageLocal = ref(1);
const limitLocal = ref(6);
watch(
  paginationConfig,
  (cfg) => {
    pageLocal.value = (cfg && cfg.page) || 1;
    limitLocal.value = (cfg && cfg.limit) || 6;
  },
  { immediate: true }
);
const paginationTotal = computed(() => {
  const cfg = paginationConfig.value;
  return cfg && typeof cfg.total !== 'undefined' ? Number(cfg.total) : 0;
});
function onAntPagination(page, pageSize) {
  emit('pagination', { page, limit: pageSize });
}
function onCellDblclickRaw(row, column, cell, event) {
  emit('cell-dblclick', { row, column, cell, event });
}

function getDictValue(row, col) {
  if (typeof col.valueGetter === 'function') {
    try {
      return col.valueGetter(row);
    } catch (e) {
      return row?.[col.prop];
    }
  }
  return row?.[col.prop];
}

function getImageSrc(row, col) {
  let src = null;
  if (typeof col.valueGetter === 'function') {
    try {
      src = col.valueGetter(row);
    } catch (e) {
      src = row?.[col.prop];
    }
  } else {
    src = row?.[col.prop];
  }
  if (!src) return col.imageFallback || noDataImg;
  return src;
}
</script>

<style lang="scss" scoped>
.pro-table {
  width: 100%;
}

.pro-table--pagination {
  padding: 14px 2px 2px;
  display: flex;
  justify-content: flex-end;
}

:deep(.ant-table) {
  .ant-table-thead > tr > th {
    background: #f7f9fc;
    font-weight: 600;
    color: #2f3a4a;
    font-size: 13px;
  }

  .ant-table-tbody > tr > td {
    font-size: 13px;
  }
}

.just-center {
  display: inline-flex;
  align-items: center;
}

.tip-icon {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: #909399;
}

.emptyBg {
  padding: 28px 0;

  img {
    width: 180px;
    max-width: 36%;
    opacity: 0.9;
  }

  p {
    font-size: 14px;
    margin: 14px 0;
    color: #8a95a6;
  }
}
</style>

