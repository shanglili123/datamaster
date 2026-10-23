<template>
  <section class="lineage-graph" aria-label="数据血缘分析">
    <div class="lineage-toolbar">
      <label>方向
        <a-select v-model:value="filters.direction" :options="directions" aria-label="血缘方向" />
      </label>
      <label>深度
        <a-select v-model:value="filters.depth" :options="depths" aria-label="血缘深度" />
      </label>
      <label>粒度
        <a-select v-model:value="filters.granularity" :options="granularities" aria-label="血缘粒度" />
      </label>
      <a-checkbox v-model:checked="filters.includeInactive">包含非活动关系</a-checkbox>
      <a-button :disabled="!canRequest" :loading="loading" @click="loadGraph">刷新</a-button>
      <a-radio-group v-model:value="view" aria-label="血缘展示方式">
        <a-radio-button value="graph">图视图</a-radio-button>
        <a-radio-button value="table">关系表</a-radio-button>
      </a-radio-group>
      <a-button v-if="view === 'graph'" :disabled="!nodes.length || loading" @click="fitGraph">适应画布</a-button>
    </div>

    <p class="lineage-caption">
      中心：<strong>{{ centerLabel }}</strong>
      <span v-if="payload?.centerId"> · {{ payload.centerId }}</span>
      <span> · 配置声明血缘，箭头表示数据流向；表 → 任务 → 表计为 1 跳。非运行成功证明。</span>
    </p>
    <div class="lineage-legend" aria-label="图例">
      <span>类型标签：表 TABLE / 字段 COLUMN / 任务 TASK / 文件 FILE / 流 STREAM / 外部 EXTERNAL</span>
      <span><i class="center-key" />粗描边：中心表</span>
      <span><i class="line-key" />实线：表 / 读取 / 写入</span>
      <span><i class="line-key dashed" />虚线：字段 / 陈旧 / 非活动（以文字为准）</span>
    </div>

    <div class="lineage-notices" role="status" aria-live="polite">
      <p v-if="loading">正在加载当前条件的真实血缘…</p>
      <p v-else-if="error" role="alert">{{ error }}</p>
      <p v-else-if="!canRequest">请选择中心表及当前空间后查看血缘。</p>
      <p v-else-if="payload">{{ graphStatusText }}</p>
      <p v-if="isTruncated">结果已截断，仅显示后端允许范围；可缩小深度或调整方向后重试。</p>
      <p v-if="hasStale">存在陈旧关系：保留最近一次有效解析结果，不代表当前配置已解析成功。</p>
      <p v-if="hasPartial">存在部分解析、失败或不支持的关系；不能据此认定完整血缘。</p>
      <ul v-if="diagnostics.length" aria-label="血缘诊断">
        <li v-for="(item, index) in diagnostics" :key="index">{{ item }}</li>
      </ul>
      <p v-if="renderError" role="alert">画布布局失败，请切换关系表查看已返回的数据。</p>
    </div>

    <a-spin :spinning="loading">
      <div v-show="view === 'graph'" class="lineage-canvas-wrap">
        <div
          ref="containerRef"
          class="lineage-canvas"
          aria-label="血缘关系图，完整内容也可通过关系表查看"
          @focusin="focusNode"
          @focusout="tooltip = null"
        />
        <div v-if="!loading && !nodes.length" class="lineage-empty">
          <a-empty :description="error ? '加载失败，可点击刷新重试' : emptyText" />
        </div>
        <div v-if="tooltip && !loading" class="lineage-tooltip" role="tooltip">
          <strong>{{ tooltip.title }}</strong>
          <div v-for="(line, index) in tooltip.lines" :key="index">{{ line }}</div>
        </div>
      </div>
      <div v-if="view === 'table'" class="lineage-tables">
        <h3>节点（{{ nodes.length }}）</h3>
        <a-table :columns="nodeColumns" :data-source="nodeRows" row-key="id" size="small" :pagination="{ pageSize: 10 }" :scroll="{ x: 1000 }" />
        <h3>关系（{{ edges.length }}）</h3>
        <a-table :columns="edgeColumns" :data-source="edgeRows" row-key="id" size="small" :pagination="{ pageSize: 10 }" :scroll="{ x: 1100 }" />
      </div>
    </a-spin>
  </section>
</template>

<script setup name="LineageGraph">
import { computed, nextTick, onActivated, onDeactivated, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { Graph } from '@antv/x6';
import { DagreLayout } from '@antv/layout';
import useUserStore from '@/store/system/user';
import { getAssetLineageGraph, getTableLineageGraph } from '@/api/lineage';

const props = defineProps({
  source: { type: String, default: 'metadata', validator: (value) => ['asset', 'metadata'].includes(value) },
  centerId: { type: [String, Number], default: undefined },
  centerName: { type: String, default: '' },
});
const userStore = useUserStore();
const containerRef = ref(null);
const payload = ref(null);
const loading = ref(false);
const error = ref('');
const renderError = ref(false);
const tooltip = ref(null);
const view = ref('graph');
const filters = reactive({ direction: 'BOTH', depth: 1, granularity: 'TABLE', includeInactive: false });
const directions = [{ value: 'BOTH', label: '上下游' }, { value: 'UPSTREAM', label: '上游' }, { value: 'DOWNSTREAM', label: '下游' }];
const depths = [1, 2, 3, 4, 5].map((value) => ({ value, label: `${value} 跳` }));
const granularities = [{ value: 'TABLE', label: '表级' }, { value: 'FIELD', label: '字段级' }];
const kindNames = { TABLE: '表', COLUMN: '字段', TASK: '任务', FILE: '文件', STREAM: '流', EXTERNAL: '外部' };
const relationNames = { TABLE: '表级依赖', FIELD: '字段依赖', READ: '读取', WRITE: '写入' };
const statusNames = { COMPLETE: '解析完整', PARTIAL: '部分解析', FAILED: '解析失败', INACTIVE: '非活动', UNSUPPORTED: '不支持', UNKNOWN: '未知' };
const diagnosticNames = {
  UNSUPPORTED: '包含不支持的解析类型', PARSE_FAILED: '存在解析失败', PARTIAL: '存在部分解析结果',
};
const hasValue = (value) => value !== undefined && value !== null && value !== '';
const canRequest = computed(() => hasValue(props.centerId) && hasValue(userStore.spaceId) && Boolean(userStore.token));
const nodes = computed(() => payload.value?.nodes || []);
const edges = computed(() => payload.value?.edges || []);
const nodeMap = computed(() => new Map(nodes.value.map((node) => [node.id, node])));
const edgeMap = computed(() => new Map(edges.value.map((edge) => [edge.id, edge])));
const centerLabel = computed(() => nodeMap.value.get(payload.value?.centerId)?.name || (payload.value?.status === 'EMPTY' ? props.centerName : '') || (hasValue(props.centerId) ? String(props.centerId) : '未选择'));
const isTruncated = computed(() => payload.value?.truncated === true || payload.value?.status === 'TRUNCATED');
const hasStale = computed(() => [...nodes.value, ...edges.value].some((item) => item.stale === true));
const hasPartial = computed(() => [...nodes.value, ...edges.value].some((item) => ['PARTIAL', 'FAILED', 'UNSUPPORTED'].includes(item.status)) || diagnostics.value.some((item) => /UNSUPPORTED|PARSE_FAILED|PARTIAL|不支持|解析失败|部分解析/.test(item)));
const diagnostics = computed(() => (payload.value?.diagnostics || []).map((code) => diagnosticNames[code] ? `${diagnosticNames[code]}（${code}）` : code));
const graphStatusText = computed(() => ({
  OK: '配置血缘已加载；运行状态未知。',
  EMPTY: '当前范围内暂无血缘关系（不代表未选择范围内不存在血缘）。',
  DISABLED: '血缘能力未启用，请联系管理员。',
  UNRESOLVED: '中心对象尚未解析或无法映射为授权血缘节点。',
  TRUNCATED: '已加载部分配置血缘；结果超过后端范围限制。',
}[payload.value?.status] || `血缘状态：${payload.value?.status || '未知'}，请结合诊断信息判断。`));
const emptyText = computed(() => !canRequest.value ? '请选择中心表' : (payload.value ? graphStatusText.value : '暂无血缘数据'));
const statusText = (item) => `${statusNames[item.status] || item.status || '解析状态未知'}${item.stale ? ' · 陈旧' : ''}`;
const kindText = (node) => `${kindNames[node.type] || '未知类型'} ${node.type || 'UNKNOWN'}`;
const relationText = (edge) => `${relationNames[edge.kind] || '未知关系'} ${edge.kind || 'UNKNOWN'}`;
const nodeName = (id) => nodeMap.value.get(id)?.name || id;
const isDashed = (edge) => edge.kind === 'FIELD' || edge.stale === true || edge.status === 'INACTIVE';
const nodeRows = computed(() => nodes.value.map((node) => ({
  id: node.id, name: node.name, type: kindText(node), center: node.id === payload.value?.centerId ? '中心' : '—',
  parent: node.parentId ? nodeName(node.parentId) : '—', column: node.columnName || '—', task: hasValue(node.taskId) ? String(node.taskId) : '—',
  status: statusText(node), run: '未知（配置声明）',
})));
const edgeRows = computed(() => edges.value.map((edge) => ({
  id: edge.id, source: nodeName(edge.source), target: nodeName(edge.target), kind: relationText(edge),
  status: statusText(edge), style: isDashed(edge) ? '虚线' : '实线', confidence: edge.confidence || '未知',
  task: hasValue(edge.taskId) ? String(edge.taskId) : '—',
})));
const nodeColumns = [
  { title: '节点', dataIndex: 'name' }, { title: '类型', dataIndex: 'type' }, { title: '中心', dataIndex: 'center' },
  { title: '所属表', dataIndex: 'parent' }, { title: '字段', dataIndex: 'column' }, { title: '生产任务 ID', dataIndex: 'task' },
  { title: '解析 / 有效性', dataIndex: 'status' }, { title: '运行状态', dataIndex: 'run' }, { title: '稳定 ID', dataIndex: 'id' },
];
const edgeColumns = [
  { title: '来源', dataIndex: 'source' }, { title: '目标', dataIndex: 'target' }, { title: '关系', dataIndex: 'kind' },
  { title: '线型', dataIndex: 'style' }, { title: '解析 / 有效性', dataIndex: 'status' },
  { title: '置信度', dataIndex: 'confidence' }, { title: '生产任务 ID', dataIndex: 'task' }, { title: '稳定 ID', dataIndex: 'id' },
];

let graph = null;
let observer = null;
let requestSequence = 0;
let active = true;
let disposed = false;

function clearGraph() {
  tooltip.value = null;
  payload.value = null;
  error.value = '';
  renderError.value = false;
  graph?.clearCells();
}

// Never accept the pre-v2 tasks/tables/rels format or synthesize nodes/IDs.
function readGraph(data) {
  if (!data || typeof data.status !== 'string' || !Array.isArray(data.nodes) || !Array.isArray(data.edges)
    || (data.centerId !== null && data.centerId !== undefined && typeof data.centerId !== 'string')
    || (data.diagnostics !== undefined && (!Array.isArray(data.diagnostics) || data.diagnostics.some((item) => typeof item !== 'string')))) {
    throw new Error('Invalid graph contract');
  }
  const ids = new Set();
  data.nodes.forEach((node) => {
    if (typeof node.id !== 'string' || !node.id || ids.has(node.id) || typeof node.type !== 'string' || typeof node.name !== 'string') throw new Error('Invalid node');
    ids.add(node.id);
  });
  const edgeIds = new Set();
  data.edges.forEach((edge) => {
    if (typeof edge.id !== 'string' || !edge.id || ids.has(edge.id) || edgeIds.has(edge.id)
      || typeof edge.kind !== 'string' || !ids.has(edge.source) || !ids.has(edge.target)) throw new Error('Invalid edge');
    edgeIds.add(edge.id);
  });
  if (data.nodes.length && (!data.centerId || !ids.has(data.centerId))) throw new Error('Missing center');
  return data;
}

async function loadGraph() {
  const sequence = ++requestSequence;
  clearGraph();
  loading.value = false;
  if (!active || disposed || !canRequest.value) return;
  loading.value = true;
  const params = {
    spaceId: userStore.spaceId, spaceCode: userStore.spaceCode,
    direction: filters.direction, depth: filters.depth, granularity: filters.granularity,
    includeInactive: filters.includeInactive,
  };
  try {
    const request = props.source === 'asset' ? getAssetLineageGraph : getTableLineageGraph;
    const response = await request(props.centerId, params);
    if (sequence !== requestSequence || disposed || !active) return;
    payload.value = readGraph(response?.data);
    await nextTick();
    if (sequence === requestSequence && !disposed && active) renderGraph();
  } catch {
    if (sequence !== requestSequence || disposed || !active) return;
    payload.value = null;
    graph?.clearCells();
    error.value = '血缘加载失败或响应不符合 v2 契约，请确认权限、服务状态后刷新重试。';
  } finally {
    if (sequence === requestSequence) loading.value = false;
  }
}

function nodeTooltip(node) {
  return {
    title: node.name,
    lines: [kindText(node), statusText(node), '运行状态：未知（配置声明）', ...(node.columnName ? [`字段：${node.columnName}`] : [])],
  };
}
function focusNode(event) {
  const id = event.target.closest?.('[data-lineage-id]')?.getAttribute('data-lineage-id');
  const node = nodeMap.value.get(id);
  tooltip.value = node ? nodeTooltip(node) : null;
}
function fitGraph() {
  if (!graph || !nodes.value.length || !containerRef.value?.clientWidth) return;
  graph.zoomToFit({ padding: 36, maxScale: 1 });
  graph.centerContent();
}
function resizeGraph() {
  const container = containerRef.value;
  if (!container || !graph || !container.clientWidth || !container.clientHeight) return;
  graph.resize(container.clientWidth, container.clientHeight);
  fitGraph();
}
function shorten(value, max = 22) {
  const letters = Array.from(value);
  return letters.length > max ? `${letters.slice(0, max - 1).join('')}…` : value;
}
function renderGraph() {
  if (!graph || disposed || !active) return;
  graph.clearCells();
  renderError.value = false;
  if (!nodes.value.length) return;
  try {
    const width = 248;
    const height = 100;
    const layout = new DagreLayout({ type: 'dagre', rankdir: 'LR', nodeSize: [width, height], nodesep: 36, ranksep: 100 });
    const positioned = layout.layout({
      nodes: nodes.value.map((node) => ({ id: node.id, width, height })),
      edges: edges.value.map((edge) => ({ source: edge.source, target: edge.target })),
    });
    const positions = new Map(positioned.nodes.map((node) => [node.id, node]));
    graph.fromJSON({
      nodes: nodes.value.map((node) => {
        const center = node.id === payload.value.centerId;
        const position = positions.get(node.id);
        return {
          id: node.id, shape: 'rect', x: position.x - width / 2, y: position.y - height / 2, width, height,
          label: `${center ? '中心 · ' : ''}${kindText(node)}\n${shorten(node.name)}\n${shorten(statusText(node), 26)}`,
          attrs: {
            body: { rx: 6, ry: 6, fill: 'var(--lineage-surface)', stroke: 'var(--lineage-ink)', strokeWidth: center ? 3 : 1, strokeDasharray: node.stale || node.status === 'INACTIVE' ? '6 4' : '' },
            label: { fill: 'var(--lineage-ink)', fontSize: 13, lineHeight: 22, fontFamily: 'inherit' },
          },
        };
      }),
      edges: edges.value.map((edge) => ({
        id: edge.id, shape: 'edge', source: edge.source, target: edge.target, zIndex: -1,
        connector: { name: 'rounded' }, router: { name: 'manhattan' },
        attrs: {
          line: { stroke: 'var(--lineage-ink)', strokeWidth: 2, strokeDasharray: isDashed(edge) ? '6 4' : '', targetMarker: { name: 'classic', size: 7 } },
          wrap: { strokeWidth: 24 },
        },
        labels: [{ attrs: {
          label: { text: `${relationNames[edge.kind] || edge.kind}${edge.stale ? ' · 陈旧' : ''}${edge.status === 'INACTIVE' ? ' · 非活动' : ''}`, fill: 'var(--lineage-ink)', fontSize: 12 },
          body: { fill: 'var(--lineage-surface)', stroke: 'none' },
        } }],
      })),
    });
    // Keyboard focus exposes exactly the same safe details as mouse hover.
    nodes.value.forEach((node) => {
      const element = graph.findViewByCell(node.id)?.el;
      element?.setAttribute('tabindex', '0');
      element?.setAttribute('data-lineage-id', node.id);
      element?.setAttribute('role', 'img');
      element?.setAttribute('aria-label', [node.name, ...nodeTooltip(node).lines].join('，'));
    });
    resizeGraph();
  } catch {
    graph.clearCells();
    renderError.value = true;
  }
}

watch(() => [props.source, props.centerId, userStore.spaceId, userStore.spaceCode, userStore.id, userStore.token, userStore.permissions.join('|')], loadGraph, { immediate: true, flush: 'sync' });
watch(() => [filters.direction, filters.depth, filters.granularity, filters.includeInactive], loadGraph);
watch(view, async () => { tooltip.value = null; await nextTick(); resizeGraph(); });
onMounted(() => {
  graph = new Graph({
    container: containerRef.value, width: containerRef.value.clientWidth || 1, height: 520,
    interacting: false, panning: true, mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'], minScale: 0.1, maxScale: 2 },
    async: false, grid: false, background: { color: 'transparent' },
  });
  graph.on('node:mouseenter', ({ node }) => { const data = nodeMap.value.get(node.id); tooltip.value = data ? nodeTooltip(data) : null; });
  graph.on('node:mouseleave', () => { tooltip.value = null; });
  graph.on('edge:mouseenter', ({ edge }) => {
    const data = edgeMap.value.get(edge.id);
    tooltip.value = data ? { title: relationText(data), lines: [`${nodeName(data.source)} → ${nodeName(data.target)}`, statusText(data), `置信度：${data.confidence || '未知'}`] } : null;
  });
  graph.on('edge:mouseleave', () => { tooltip.value = null; });
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(resizeGraph);
    observer.observe(containerRef.value);
  } else window.addEventListener('resize', resizeGraph);
  renderGraph();
});
onDeactivated(() => { active = false; ++requestSequence; loading.value = false; clearGraph(); });
onActivated(() => { if (!active) { active = true; loadGraph(); } });
onUnmounted(() => {
  disposed = true;
  ++requestSequence;
  observer?.disconnect();
  window.removeEventListener('resize', resizeGraph);
  graph?.off();
  graph?.dispose();
  graph = null;
});
</script>

<style scoped lang="scss">
.lineage-graph {
  --lineage-surface: var(--dm-bg-card, #fff);
  --lineage-ink: var(--dm-text-main, #111827);
  color: var(--lineage-ink);
  width: 100%;
  min-width: 0;
}
.lineage-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 12px; }
.lineage-toolbar label { display: inline-flex; align-items: center; gap: 8px; }
.lineage-toolbar :deep(.ant-select) { min-width: 90px; }
.lineage-caption { overflow-wrap: anywhere; margin: 8px 0; }
.lineage-legend { display: flex; flex-wrap: wrap; gap: 10px 20px; color: var(--dm-text-secondary, #6b7280); font-size: 12px; }
.lineage-legend span { display: inline-flex; align-items: center; gap: 6px; }
.center-key { width: 18px; height: 12px; border: 3px solid var(--lineage-ink); border-radius: 2px; }
.line-key { width: 24px; border-top: 2px solid var(--lineage-ink); }
.line-key.dashed { border-top-style: dashed; }
.lineage-notices { margin: 12px 0; font-size: 13px; }
.lineage-notices p { margin: 4px 0; }
.lineage-notices ul { padding-left: 20px; max-height: 160px; overflow: auto; }
.lineage-canvas-wrap { position: relative; border: 1px solid var(--dm-border-color, #e2e9f3); background: var(--lineage-surface); border-radius: 6px; }
.lineage-canvas { width: 100%; height: 520px; min-height: 360px; }
.lineage-empty { position: absolute; inset: 0; display: grid; place-items: center; pointer-events: none; }
.lineage-tooltip { position: absolute; top: 12px; left: 12px; max-width: min(360px, calc(100% - 24px)); padding: 10px 14px; background: var(--lineage-surface); color: var(--lineage-ink); border: 1px solid var(--lineage-ink); border-radius: 6px; overflow-wrap: anywhere; pointer-events: none; }
.lineage-tables h3 { margin: 16px 0 8px; font-size: 14px; color: var(--lineage-ink); }
:deep(.x6-node:focus) { outline: 2px solid var(--lineage-ink); outline-offset: 4px; }
:global(html.dark .lineage-graph), :global([data-theme='dark'] .lineage-graph) { --lineage-surface: #1f1f1f; --lineage-ink: #f3f4f6; }
@media (forced-colors: active) { .lineage-graph { --lineage-surface: Canvas; --lineage-ink: CanvasText; } }
</style>
