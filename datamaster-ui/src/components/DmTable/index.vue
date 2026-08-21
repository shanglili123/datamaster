<!--
    DmTable 组件
    说明：基于 Ant Design Vue 的表格封装，集成了分页、排序、字典、图标、链接等功能
    作者：datamaster
-->
<template>
  <div class="dm-table">
    <a-spin :spinning="store.loading">
      <div :class="['dm-table--main', config.table?.class]">
        <a-table
          v-if="store.showTable"
          :data-source="tableData"
          :columns="antColumns"
          v-bind="antTableProps"
          :row-key="rowKey"
          :row-selection="antRowSelection"
          :default-expand-all-rows="antDefaultExpandAll"
          :tree="antTreeProps"
          :pagination="false"
          :locale="{ emptyText: emptyContent }"
          @change="handleAntSortChange"
        >
          <template #headerCell="{ column }">
            <div class="tip-wrap" v-if="column.tip">
              <span class="tip-label">{{ column.label }}</span>
              <a-tooltip v-bind="column.tip">
                <template #title v-if="column.tip.custom">
                  <div class="tip-content" v-html="column.tip.content"></div>
                </template>
                <span class="tip-icon-wrapper">
                  <InfoFilled />
                </span>
              </a-tooltip>
            </div>
            <span v-else>{{ column.title }}</span>
          </template>

          <template #bodyCell="{ column, record, index }">
            <!-- 空数据处理 -->
            <span v-if="column.prop && [undefined, null].includes(record[column.prop])">
              {{ getFormatValue(record[column.prop]) }}
            </span>

            <!-- 字典 -->
            <dict-tag
              v-else-if="column.dict"
              :options="getDictOptions(column.dict)"
              :value="record[column.prop]"
            />

            <!-- 链接 -->
            <a
              v-else-if="column.link"
              @click="handleLinkClick(column, record)"
              style="cursor: pointer"
            >
              {{ record[column.prop] }}
            </a>

            <!-- 图标 -->
            <svg-icon
              v-else-if="column.svg"
              v-bind="column.svg"
              :icon-class="record[column.prop]"
            />

            <!-- 时间格式化 -->
            <span v-else-if="column.date">
              {{ parseTime(record[column.prop], column.date === true ? '{y}-{m}-{d} {h}:{i}' : column.date) }}
            </span>

            <!-- 自定义 slot -->
            <slot
              v-else-if="column.slot"
              :name="column.slot"
              v-bind="{ row: record, $index: index, column_data: column }"
            />

            <!-- 序号列（el-table type="index" 兼容） -->
            <span v-else-if="column.type === 'index'">{{ index + 1 }}</span>

            <span v-else>{{ record[column.prop] ?? '' }}</span>
          </template>
        </a-table>
      </div>
    </a-spin>

    <div
      :class="['dm-table--pagination', config.pagination?.class]"
      v-if="!config.notPagination"
    >
      <a-pagination
        :total="store.total"
        v-model:current="store.params.pageNum"
        v-model:page-size="store.params.pageSize"
        :page-size-options="config.pagination?.pageSizes || DEFAULT_PAGE_SIZES"
        show-size-changer
        show-quick-jumper
        :show-total="(total) => `共 ${total} 条`"
        @change="handleAntPageChange"
        @showSizeChange="handleAntSizeChange"
      />
    </div>
  </div>
</template>

<script setup name="DmTable">
import { reactive, computed, nextTick, h, toValue } from 'vue';
import { useRouter } from 'vue-router';
import { InfoCircleFilled as InfoFilled } from '@ant-design/icons-vue';
import SvgIcon from '@/components/SvgIcon/index.vue';
import { scrollTo } from '@/utils/scroll-to';

defineOptions({
  inheritAttrs: false,
});

/**
 * props
 * @param {columns} 表格项
 * @param {Function} 获取表格数据的方法 参数:params 需返回一个Promise
 * @param {Object} config.table 表格配置（兼容 el-table 旧配置，内部自动适配 a-table）
 * @param {Object} config.pagination 分页配置
 * @param {Boolean} config.initResquest 是否初始化请求
 * @param {Boolean} config.notPagination 不使用分页
 * @param {Boolean} config.notPaginationParams 不使用默认的分页参数
 * @param {Boolean} config.autoPagination 前端分页
 * @param {Object|Boolean} config.sort 后端排序所需的key
 * @param {Array} column.dict 字典数据
 * @param {Object} column.svg svg图标数据
 * @param {Object} column.link 跳转参数
 * @param {Object} column.slot 自定义插槽
 * @param {Object} column.date 时间参数
 * @param {events} 事件回调
 */
const props = defineProps({
  config: {
    type: Object,
    default: () => ({}),
  },
  columns: {
    type: Array,
    required: true,
  },
  func: {
    type: Function,
    required: true,
  },
  params: {
    type: Object,
    default: () => ({}),
  },
  events: {
    type: Object,
    default: () => ({}),
  },
});

const { proxy } = getCurrentInstance();
const router = useRouter();

const DEFAULT_PAGE_PARAMS = {
  pageNum: 1,
  pageSize: 6,
};
const DEFAULT_PAGE_SIZES = [6, 8, 10, 20, 30, 50];

const store = reactive({
  loading: false,
  params: {},
  total: 0,
  data: [],
  rows: [],
  defaultSort: {},
  sort: {
    prop: 'orderByColumn',
    order: 'isAsc',
  },
  dict: {},
  showTable: true,
  pagerCount: document.body.clientWidth < 1300 ? 4 : 7,
});

const config = computed(() => props.config || {});

// ─────────────────────────────────────────────
// 适配层：el-table → a-table
// ─────────────────────────────────────────────

/**
 * rowKey 提取
 */
const rowKey = computed(() => {
  const table = config.value.table || {};
  return table.rowKey || 'id';
});

/**
 * 将 el-table columns 转换为 a-table columns
 * {label, prop} → {title, dataIndex}
 */
const antColumns = computed(() => {
  return props.columns
    // el-table 的选择列由 a-table 的 row-selection prop 承担，映射成列会变成幽灵空列
    // noHide 列强制展示（隐藏列下拉里不可取消勾选，如"字段名称"）
    .filter((c) => (!c.hide || c.noHide) && c.type !== 'selection')
    .map((c) => {
      const col = {
        title: c.label,
        dataIndex: c.prop,
        key: c.prop || c.slot || c.label,
        align: c.align || 'center',
        width: c.width,
        fixed: c.fixed,
        ellipsis: c.showOverflowTooltip ? true : undefined,
        sorter: c.sortable ? true : undefined,
        tip: c.tip,
        type: c.type,
        prop: c.prop,
        dict: c.dict,
        link: c.link,
        svg: c.svg,
        date: c.date,
        slot: c.slot,
      };
      Object.keys(col).forEach((k) => {
        if (col[k] === undefined) delete col[k];
      });
      return col;
    });
});

/**
 * config.table 属性映射
 * el-table props → a-table props
 */
const antTableProps = computed(() => {
  const table = config.value.table || {};
  const result = {};

  // stripe → striped
  if (table.stripe !== undefined) result.striped = table.stripe;

  // border → bordered
  if (table.border !== undefined) result.bordered = table.border;

  // size 直接透传
  if (table.size) result.size = table.size === 'default' ? 'middle' : table.size;

  // 滚动配置（含自动计算的横向滚动）
  const scroll = tableScroll.value;
  if (scroll) result.scroll = scroll;

  // onRowDblclick → customRow.dblclick
  if (table.onRowDblclick) {
    result.customRow = (record, index) => ({
      dblclick: () => {
        table.onRowDblclick(record, index);
      },
    });
  }

  return result;
});

/**
 * 表格滚动配置
 * - config.table.height → scroll.y
 * - 列总宽超出时自动启用横向滚动 scroll.x，避免列被压缩、fixed 列失效
 */
const tableScroll = computed(() => {
  const table = config.value.table || {};
  const scroll = {};
  if (table.height) scroll.y = table.height;
  if (table.scroll && typeof table.scroll === 'object') {
    Object.assign(scroll, table.scroll);
  }
  if (scroll.x === undefined) {
    const cols = props.columns.filter((c) => (!c.hide || c.noHide) && c.type !== 'selection');
    const totalWidth = cols.reduce(
      (sum, c) => sum + (typeof c.width === 'number' ? c.width : 0),
      0
    );
    const hasFixed = cols.some((c) => c.fixed);
    const allHaveWidth = cols.length > 0 && cols.every((c) => typeof c.width === 'number');
    if (totalWidth > 0 && (hasFixed || allHaveWidth)) {
      // antd 将 scroll.x 作为表格最小宽度：容器更宽时列自动拉伸，更窄时出现横向滚动条
      scroll.x = totalWidth;
    }
  }
  return Object.keys(scroll).length ? scroll : undefined;
});

/**
 * 树形表格配置
 */
const antTreeProps = computed(() => {
  const table = config.value.table || {};
  if (table.treeProps) {
    return {
      childrenColumnName: table.treeProps.children || 'children',
    };
  }
  return undefined;
});

const antDefaultExpandAll = computed(() => {
  const table = config.value.table || {};
  return table.defaultExpandAll || false;
});

/**
 * 行选择配置适配
 * el-table: onSelectionChange(selection) → a-table: rowSelection.onChange(selectedRowKeys, selectedRows)
 */
const antRowSelection = computed(() => {
  const table = config.value.table || {};
  const hasSelection = props.columns.some((c) => c.type === 'selection');

  if (!hasSelection && !table.onSelectionChange) return undefined;

  return {
    type: 'checkbox',
    onChange: (selectedRowKeys, selectedRows) => {
      if (table.onSelectionChange) {
        table.onSelectionChange(selectedRows);
      }
    },
  };
});

/**
 * 默认排序适配
 */
const defaultTableSort = computed(() => {
  const { table } = config.value;
  return table?.defaultSort || store.defaultSort;
});

/**
 * 空状态内容
 */
const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '暂无记录'),
]);

// ─────────────────────────────────────────────
// 业务逻辑（保持不变）
// ─────────────────────────────────────────────

const tableData = computed(() => {
  const { notPagination, autoPagination } = config.value;
  if (notPagination) return store.data;
  const { pageNum, pageSize } = store.params;
  if (autoPagination) {
    return store.data.slice((pageNum - 1) * pageSize, pageNum * pageSize);
  }
  return store.data;
});

function getList() {
  store.loading = true;
  const { formatParams, formatData } = props.events;
  const { notPagination, autoPagination } = config.value;
  let params = Object.assign({}, store.params, props.params);
  params = formatParams ? formatParams(params) : params;
  props
    .func(params)
    .then((res) => {
      const pageData = normalizePageData(res);
      let data = pageData.rows;
      if (!notPagination) {
        store.total = pageData.total;
      }
      if (autoPagination) {
        store.total = data.length;
      }
      store.total = store.total || 0;
      data = formatData ? formatData(data, params) : data;
      store.data = data;
      store.loading = false;
    })
    .catch(() => {
      store.loading = false;
    });
}

function normalizePageData(res) {
  const data = res?.data ?? res ?? {};
  if (Array.isArray(data)) {
    return { rows: data, total: data.length };
  }
  const rows = Array.isArray(data.rows)
    ? data.rows
    : Array.isArray(data.list)
      ? data.list
      : Array.isArray(data.records)
        ? data.records
        : Array.isArray(res?.rows)
          ? res.rows
          : [];
  const total = Number(data.total ?? data.totalCount ?? res?.total ?? rows.length);
  return { rows, total: Number.isNaN(total) ? rows.length : total };
}

function resetQuery() {
  setupDefaultPageParams();
  getList();
}

function handleAntSizeChange(current, pageSize) {
  store.params.pageSize = pageSize;
  const { pageNum } = store.params;
  if (pageNum * pageSize > store.total) {
    store.params.pageNum = 1;
  }
  const { onPageSizeChange } = props.events;
  const { pagination } = config.value;
  onPageSizeChange && onPageSizeChange({ ...store.params });
  getList();
  if (pagination?.notAutoScroll) return;
  scrollTo(0, 800);
}

function handleAntPageChange(pageNum, pageSize) {
  store.params.pageNum = pageNum;
  store.params.pageSize = pageSize;
  const { onPageCurrentChange } = props.events;
  const { pagination } = config.value;
  onPageCurrentChange && onPageCurrentChange({ ...store.params });
  getList();
  if (pagination?.notAutoScroll) return;
  scrollTo(0, 800);
}

function handleAntSortChange(pag, filters, sorter) {
  const { onSortChange } = props.events;
  const sort = store.sort;
  const order = sorter.order === 'ascend' ? 'ascending' : sorter.order === 'descend' ? 'descending' : null;
  const prop = sorter.field || sorter.column?.dataIndex;

  if (order && prop) {
    const colData = props.columns.find((c) => c.prop === prop);
    store.params[sort.prop] = colData?.sortableKey || prop;
    store.params[sort.order] = order;
  } else {
    store.params[sort.prop] = undefined;
    store.params[sort.order] = undefined;
  }

  onSortChange && onSortChange({ ...store.params, ...props.params }, { ...sort });
  getList();
}

function handleLinkClick(column, row) {
  const { onLinkClick } = props.events;
  const { type, path, name, external, ...other } = column.link;
  onLinkClick && onLinkClick(column, row);
  if (external) return external(row);
  const params = other.params ? other.params(row) : undefined;
  const query = other.query ? other.query(row) : undefined;
  router.push({ name, path, params, query });
}

function getDictOptions(key) {
  if (store.dict[key]) return store.dict[key];
  const value = toValue(proxy.useDict(key)[key]);
  store.dict[key] = value;
  return value;
}

function getFormatValue(val) {
  return val === undefined || val === null ? '-' : val;
}

function reload() {
  store.showTable = false;
  nextTick(() => {
    store.showTable = true;
  });
}

function updateRowByKey(keyField, keyValue, patch) {
  if (!keyField || keyValue === undefined || keyValue === null || !patch) {
    return false;
  }
  const rowIndex = store.data.findIndex((item) => {
    return String(item?.[keyField]) === String(keyValue);
  });
  if (rowIndex < 0) {
    return false;
  }
  store.data[rowIndex] = { ...store.data[rowIndex], ...patch };
  return true;
}

function setupDefaultPageParams() {
  const { notPagination, pagination } = config.value;
  if (pagination?.params) {
    const { notPaginationParams } = config.value;
    const defaultParams = notPaginationParams ? {} : { ...DEFAULT_PAGE_PARAMS };
    const params = Object.assign({}, defaultParams, config.value.pagination.params);
    for (let key in params) {
      store.params[key] = params[key];
    }
    return;
  }
  if (notPagination) return;
  for (let key in DEFAULT_PAGE_PARAMS) {
    store.params[key] = DEFAULT_PAGE_PARAMS[key];
  }
}

// 初始化
setupDefaultPageParams();

if (config.value.sort) {
  let sort = config.value.sort;
  if (typeof sort == 'boolean') {
    sort = { ...store.sort };
  }
  store.sort = { ...sort };
  let defaultSort = defaultTableSort.value;
  if (!Object.keys(defaultSort).length) {
    defaultSort = { prop: 'createTime', order: 'descending' };
    store.defaultSort = { ...defaultSort };
  }
  store.params[sort.prop] = defaultSort.prop;
  store.params[sort.order] = defaultSort.order;
}

if (config.value.initResquest || config.value.initResquest === undefined) {
  getList();
}

defineExpose({
  getList,
  resetQuery,
  reload,
  updateRowByKey,
});
</script>

<style lang="scss" scoped>
.dm-table {
  width: 100%;
}

.dm-table--main {
  overflow: hidden;
  border: 1px solid #edf1f7;
  border-radius: 8px;
}

:deep(.ant-table) {
  .ant-table-thead > tr > th {
    background: #f7f9fc;
    font-weight: 600;
    color: #2f3a4a;
    font-size: 13px;
    height: 42px;
  }

  .ant-table-tbody > tr > td {
    color: #3f4a5a;
    font-size: 13px;
    padding: 10px 16px;
  }

  .ant-table-tbody > tr:hover > td {
    background: #f6faff !important;
  }
}

.tip-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tip-icon-wrapper {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: #909399;
}

.dm-table--pagination {
  padding: 14px 2px 2px;
  display: flex;
  justify-content: flex-end;
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
