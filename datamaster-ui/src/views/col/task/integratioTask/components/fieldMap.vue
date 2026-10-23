<template>
  <div class="field-mapping" @keydown.esc="cancelConnection">
    <div class="mapping-actions">
      <span class="mapping-help">{{ info ? '查看字段映射关系' : '拖动端点连线，或依次点击两侧端点；点击连线可解除映射。每个字段只能连接一次。' }}</span>
      <a-space v-if="!info" wrap>
        <a-button size="small" @click="matchByName">同名映射</a-button>
        <a-button size="small" @click="matchByOrder">按序映射</a-button>
        <a-button size="small" :disabled="!connections.length" @click="clearConnections">清空连线</a-button>
      </a-space>
    </div>
    <div v-if="pendingPort" class="mapping-prompt" role="status">已选择 {{ portLabel(pendingPort) }}，请选择另一侧字段，按 Esc 取消。</div>
    <div ref="viewportRef" class="mapping-viewport" @scroll="scheduleMeasure">
      <div ref="boardRef" class="mapping-board">
        <section class="mapping-side">
          <h4 class="mapping-heading">上游输入字段 <small>{{ readerForm.tableFields.length }}</small></h4>
          <draggable :list="readerForm.tableFields" item-key="_mappingKey" handle=".mapping-drag-handle" :disabled="info" :animation="0" class="mapping-list" @end="scheduleMeasure">
            <template #item="{ element, index }">
              <div class="mapping-row mapping-row--source" :class="{ 'is-connected': isConnected('source', element._mappingKey) }">
                <span v-if="!info" class="mapping-drag-handle" title="拖动调整字段顺序"><HolderOutlined /></span>
                <a-input v-if="!info" v-model:value="element.columnName" size="small" :aria-label="`上游字段 ${index + 1}`" />
                <span v-else class="mapping-name" :title="element.columnName">{{ element.columnName }}</span>
                <a-button v-if="!info" type="text" danger size="small" :aria-label="`删除上游字段 ${element.columnName || index + 1}`" @click="deleteField('source', element._mappingKey)"><DeleteOutlined /></a-button>
                <button :ref="(el) => setPortRef('source', element._mappingKey, el)" type="button" class="mapping-port mapping-port--source" :class="portClass('source', element._mappingKey)" :data-mapping-owner="ownerId" data-mapping-side="source" :data-mapping-key="element._mappingKey" :disabled="info" :aria-label="`连接上游字段 ${element.columnName || index + 1}`" :title="connectionLabel('source', element._mappingKey)" @pointerdown="startConnection($event, 'source', element._mappingKey)" @click="keyboardSelect($event, 'source', element._mappingKey)"><span></span></button>
              </div>
            </template>
          </draggable>
          <a-empty v-if="!readerForm.tableFields.length" :image-style="{ height: '40px' }" description="暂无上游字段" />
        </section>
        <div class="mapping-gap" aria-hidden="true"></div>
        <section class="mapping-side">
          <h4 class="mapping-heading">目标表字段 <small>{{ readerForm.toColumnsList.length }}</small></h4>
          <draggable :list="readerForm.toColumnsList" item-key="_mappingKey" handle=".mapping-drag-handle" :disabled="info" :animation="0" class="mapping-list" @end="scheduleMeasure">
            <template #item="{ element, index }">
              <div class="mapping-row mapping-row--target" :class="{ 'is-connected': isConnected('target', element._mappingKey) }">
                <button :ref="(el) => setPortRef('target', element._mappingKey, el)" type="button" class="mapping-port mapping-port--target" :class="portClass('target', element._mappingKey)" :data-mapping-owner="ownerId" data-mapping-side="target" :data-mapping-key="element._mappingKey" :disabled="info || isSystemField(element)" :aria-label="`连接目标字段 ${element.columnName || index + 1}`" :title="connectionLabel('target', element._mappingKey)" @pointerdown="startConnection($event, 'target', element._mappingKey)" @click="keyboardSelect($event, 'target', element._mappingKey)"><span></span></button>
                <a-input v-if="!info && !isSystemField(element)" v-model:value="element.columnName" size="small" :aria-label="`目标字段 ${index + 1}`" />
                <span v-else class="mapping-name" :title="element.columnName">{{ element.columnName }}</span>
                <small v-if="isSystemField(element)" class="mapping-system">系统生成</small>
                <a-button v-if="!info && !isSystemField(element)" type="text" danger size="small" :aria-label="`删除目标字段 ${element.columnName || index + 1}`" @click="deleteField('target', element._mappingKey)"><DeleteOutlined /></a-button>
                <span v-if="!info" class="mapping-drag-handle" title="拖动调整字段顺序"><HolderOutlined /></span>
              </div>
            </template>
          </draggable>
          <a-empty v-if="!readerForm.toColumnsList.length" :image-style="{ height: '40px' }" description="暂无目标字段" />
        </section>
        <svg class="mapping-lines" :width="boardSize.width" :height="boardSize.height" :viewBox="`0 0 ${boardSize.width} ${boardSize.height}`" aria-label="字段映射连线">
          <defs><marker :id="`${ownerId}-arrow`" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse"><path d="M 1 1 L 9 5 L 1 9" class="mapping-arrow" /></marker></defs>
          <g v-for="line in renderedConnections" :key="line.id" class="mapping-edge">
            <path v-if="!info" :d="line.path" class="mapping-edge-hit" role="button" tabindex="0" :aria-label="`解除映射 ${line.label}`" @click="removeConnection(line.id)" @keydown.enter.prevent="removeConnection(line.id)" @keydown.space.prevent="removeConnection(line.id)" @keydown.delete.prevent="removeConnection(line.id)"><title>{{ line.label }}（点击解除）</title></path>
            <path :d="line.path" class="mapping-edge-line" :marker-end="`url(#${ownerId}-arrow)`"><title>{{ line.label }}</title></path>
          </g>
          <path v-if="previewPath" :d="previewPath" class="mapping-edge-line mapping-edge-preview" />
        </svg>
      </div>
    </div>
    <div class="mapping-footer">
      <span>已映射 {{ connections.length }} 对，上游未映射 {{ readerForm.tableFields.length - connections.length }} 个，目标未映射 {{ readerForm.toColumnsList.length - connections.length }} 个</span>
      <a-button v-if="!info" size="small" @click="addRow">添加字段行</a-button>
    </div>
    <details v-if="connections.length" class="mapping-details">
      <summary>查看映射明细</summary>
      <ul>
        <li v-for="pair in connectionDetails" :key="pair.id"><span>{{ pair.label }}</span><a-button v-if="!info" type="link" size="small" @click="removeConnection(pair.id)">解除</a-button></li>
      </ul>
    </details>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import draggable from 'vuedraggable';
import { DeleteOutlined, HolderOutlined } from '@ant-design/icons-vue';

const props = defineProps({
  tableFields: { type: Array, default: () => [] },
  toColumnsList: { type: Array, default: () => [] },
  type: { type: String, default: '' },
  systemColumns: { type: Array, default: () => [] },
  info: { type: Boolean, default: false },
});
const ownerId = `field-mapping-${getCurrentInstance().uid}`;
const readerForm = ref({ tableFields: [], toColumnsList: [] });
const connections = ref([]);
const pendingPort = ref(null);
const dragging = ref(null);
const viewportRef = ref(null);
const boardRef = ref(null);
const boardSize = ref({ width: 1, height: 1 });
const portPositions = ref({});
const portElements = new Map();
let keySequence = 0;
let measureFrame = 0;
let resizeObserver;
let pointerStart;
let disposed = false;

function fieldsFor(side) {
  return side === 'source' ? readerForm.value.tableFields : readerForm.value.toColumnsList;
}
function portId(side, key) {
  return `${side}:${key}`;
}
function copyFields(fields, side) {
  return (Array.isArray(fields) ? fields : []).map((field) => ({ ...field, _mappingKey: `${side}-${++keySequence}`, isChecked: Boolean(field.isChecked) }));
}
function restoreMapping() {
  cancelConnection();
  readerForm.value = {
    tableFields: copyFields(props.tableFields, 'source'),
    toColumnsList: copyFields(props.toColumnsList, 'target'),
  };
  const sources = readerForm.value.tableFields;
  const targets = readerForm.value.toColumnsList;
  // 历史草稿按完整数组同下标且双勾选保存，不能分别过滤后重新配对。
  connections.value = sources.flatMap((source, index) => {
    const target = targets[index];
    return source.isChecked && target && !isSystemField(target) && (props.type.toLowerCase() === 'hdfs' || target.isChecked)
      ? [{ source: source._mappingKey, target: target._mappingKey }] : [];
  });
  syncChecked();
  scheduleMeasure();
}
watch(() => [props.tableFields, props.toColumnsList, props.type, props.systemColumns], restoreMapping, { deep: true, immediate: true });
watch(() => props.info, (value) => { if (value) cancelConnection(); });

function syncChecked() {
  readerForm.value.tableFields.forEach((field) => { field.isChecked = isConnected('source', field._mappingKey); });
  readerForm.value.toColumnsList.forEach((field) => { field.isChecked = isConnected('target', field._mappingKey); });
}
function isConnected(side, key) {
  return connections.value.some((pair) => pair[side] === key);
}
function portClass(side, key) {
  return { 'is-connected': isConnected(side, key), 'is-pending': pendingPort.value?.side === side && pendingPort.value?.key === key };
}
function fieldName(side, key) {
  return fieldsFor(side).find((field) => field._mappingKey === key)?.columnName || '未命名字段';
}
function portLabel(port) {
  return `${port.side === 'source' ? '上游' : '目标'}字段 ${fieldName(port.side, port.key)}`;
}
function connectionLabel(side, key) {
  const pair = connections.value.find((entry) => entry[side] === key);
  if (side === 'target' && isSystemField(fieldsFor(side).find((field) => field._mappingKey === key))) return '系统自动生成，无需手动连接';
  return pair ? `${fieldName('source', pair.source)} → ${fieldName('target', pair.target)}` : '未映射，拖动或点击连接';
}
function isSystemField(field) {
  return props.systemColumns.some((name) => String(name).toLowerCase() === String(field?.columnName || '').toLowerCase());
}
function connect(first, second) {
  if (props.info || first.side === second.side) return;
  if (!fieldsFor(first.side).some((field) => field._mappingKey === first.key)
    || !fieldsFor(second.side).some((field) => field._mappingKey === second.key)) return;
  const source = first.side === 'source' ? first.key : second.key;
  const target = first.side === 'target' ? first.key : second.key;
  if (isSystemField(fieldsFor('target').find((field) => field._mappingKey === target))) return;
  connections.value = connections.value.filter((pair) => pair.source !== source && pair.target !== target);
  connections.value.push({ source, target });
  pendingPort.value = null;
  syncChecked();
  scheduleMeasure();
}
function selectPort(port) {
  if (props.info) return;
  const previous = pendingPort.value;
  if (previous && previous.side !== port.side) connect(previous, port);
  else pendingPort.value = previous?.key === port.key && previous.side === port.side ? null : port;
}
function keyboardSelect(event, side, key) {
  if (event.detail === 0) selectPort({ side, key });
}
function startConnection(event, side, key) {
  if (props.info || event.button !== 0) return;
  event.preventDefault();
  pointerStart = { side, key, x: event.clientX, y: event.clientY, pointerId: event.pointerId };
  window.addEventListener('pointermove', moveConnection);
  window.addEventListener('pointerup', finishConnection);
  window.addEventListener('pointercancel', cancelConnection);
}
function moveConnection(event) {
  if (!pointerStart || event.pointerId !== pointerStart.pointerId) return;
  if (!dragging.value && Math.hypot(event.clientX - pointerStart.x, event.clientY - pointerStart.y) < 4) return;
  const bounds = boardRef.value?.getBoundingClientRect();
  if (!bounds) return;
  dragging.value = {
    side: pointerStart.side, key: pointerStart.key,
    x: (event.clientX - bounds.left) * boardRef.value.clientWidth / (bounds.width || 1),
    y: (event.clientY - bounds.top) * boardRef.value.clientHeight / (bounds.height || 1),
  };
}
function finishConnection(event) {
  if (!pointerStart || event.pointerId !== pointerStart.pointerId) return;
  const start = { side: pointerStart.side, key: pointerStart.key };
  if (dragging.value) {
    const target = document.elementFromPoint(event.clientX, event.clientY)?.closest('[data-mapping-owner]');
    if (target?.dataset.mappingOwner === ownerId && target.dataset.mappingSide !== start.side) {
      connect(start, { side: target.dataset.mappingSide, key: target.dataset.mappingKey });
    }
  } else selectPort(start);
  stopPointer();
}
function stopPointer() {
  pointerStart = null;
  dragging.value = null;
  window.removeEventListener('pointermove', moveConnection);
  window.removeEventListener('pointerup', finishConnection);
  window.removeEventListener('pointercancel', cancelConnection);
}
function cancelConnection() {
  stopPointer();
  pendingPort.value = null;
}
function removeConnection(id) {
  if (props.info) return;
  connections.value = connections.value.filter((pair) => `${pair.source}:${pair.target}` !== id);
  syncChecked();
}
function clearConnections() {
  cancelConnection();
  connections.value = [];
  syncChecked();
}
function matchByOrder() {
  if (props.info) return;
  clearConnections();
  const targets = readerForm.value.toColumnsList.filter((field) => !isSystemField(field));
  connections.value = readerForm.value.tableFields.slice(0, targets.length).map((source, index) => ({ source: source._mappingKey, target: targets[index]._mappingKey }));
  syncChecked();
}
function matchByName() {
  if (props.info) return;
  clearConnections();
  const used = new Set();
  readerForm.value.tableFields.forEach((source) => {
    const name = String(source.columnName || '').trim().toLowerCase();
    const target = readerForm.value.toColumnsList.find((field) => !used.has(field._mappingKey) && !isSystemField(field) && name && String(field.columnName || '').trim().toLowerCase() === name);
    if (target) {
      used.add(target._mappingKey);
      connections.value.push({ source: source._mappingKey, target: target._mappingKey });
    }
  });
  syncChecked();
}
function addRow() {
  if (props.info) return;
  readerForm.value.tableFields.push({ columnName: '', isChecked: false, _mappingKey: `source-${++keySequence}` });
  readerForm.value.toColumnsList.push({ columnName: '', isChecked: false, _mappingKey: `target-${++keySequence}` });
  scheduleMeasure();
}
function deleteField(side, key) {
  if (props.info) return;
  cancelConnection();
  const fields = fieldsFor(side);
  fields.splice(fields.findIndex((field) => field._mappingKey === key), 1);
  connections.value = connections.value.filter((pair) => pair[side] !== key);
  syncChecked();
  scheduleMeasure();
}

// 端点就是字段行的一部分，使用真实坐标而非标题高度或行号偏移。
function setPortRef(side, key, element) {
  const id = portId(side, key);
  if (element) portElements.set(id, element);
  else portElements.delete(id);
}
function measurePorts() {
  const board = boardRef.value;
  if (!board) return;
  const bounds = board.getBoundingClientRect();
  boardSize.value = { width: Math.max(1, board.clientWidth), height: Math.max(1, board.clientHeight) };
  const positions = {};
  const scaleX = board.clientWidth / (bounds.width || 1);
  const scaleY = board.clientHeight / (bounds.height || 1);
  portElements.forEach((element, id) => {
    const rect = element.getBoundingClientRect();
    positions[id] = { x: (rect.left + rect.width / 2 - bounds.left) * scaleX, y: (rect.top + rect.height / 2 - bounds.top) * scaleY };
  });
  portPositions.value = positions;
}
function scheduleMeasure() {
  nextTick(() => {
    if (disposed) return;
    cancelAnimationFrame(measureFrame);
    measureFrame = requestAnimationFrame(measurePorts);
  });
}
function curve(source, target) {
  const bend = Math.max(30, Math.abs(target.x - source.x) / 2);
  return `M ${source.x} ${source.y} C ${source.x + bend} ${source.y}, ${target.x - bend} ${target.y}, ${target.x} ${target.y}`;
}
const connectionDetails = computed(() => connections.value.map((pair) => ({ ...pair, id: `${pair.source}:${pair.target}`, label: `${fieldName('source', pair.source)} → ${fieldName('target', pair.target)}` })));
const renderedConnections = computed(() => connectionDetails.value.flatMap((pair) => {
  const source = portPositions.value[portId('source', pair.source)];
  const target = portPositions.value[portId('target', pair.target)];
  return source && target ? [{ ...pair, path: curve(source, target) }] : [];
}));
const previewPath = computed(() => {
  const drag = dragging.value;
  if (!drag) return '';
  const anchor = portPositions.value[portId(drag.side, drag.key)];
  if (!anchor) return '';
  return drag.side === 'source' ? curve(anchor, drag) : curve(drag, anchor);
});
onMounted(() => {
  resizeObserver = new ResizeObserver(scheduleMeasure);
  if (boardRef.value) resizeObserver.observe(boardRef.value);
  window.addEventListener('resize', scheduleMeasure);
  scheduleMeasure();
});
onBeforeUnmount(() => {
  disposed = true;
  cancelConnection();
  resizeObserver?.disconnect();
  cancelAnimationFrame(measureFrame);
  window.removeEventListener('resize', scheduleMeasure);
});

// 下游执行器按完整数组的同下标且双勾选配对，已连接字段必须成对放在前缀。
// 未连接字段完整保留在尾部并取消勾选，兼容旧草稿和图校验。
function getColumns() {
  const pairs = readerForm.value.tableFields.flatMap((source) => {
    const pair = connections.value.find((entry) => entry.source === source._mappingKey);
    const target = pair && readerForm.value.toColumnsList.find((field) => field._mappingKey === pair.target);
    return target ? [{ source, target }] : [];
  });
  const clean = (field, isChecked) => {
    const value = { ...field, isChecked };
    delete value._mappingKey;
    return value;
  };
  return {
    fromColumns: [...pairs.map((pair) => clean(pair.source, true)), ...readerForm.value.tableFields.filter((field) => !isConnected('source', field._mappingKey)).map((field) => clean(field, false))],
    toColumns: [...pairs.map((pair) => clean(pair.target, true)), ...readerForm.value.toColumnsList.filter((field) => !isConnected('target', field._mappingKey)).map((field) => clean(field, false))],
  };
}
defineExpose({ getColumns });
</script>

<style lang="scss" scoped>
.field-mapping { min-width: 0; }
.mapping-actions { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; }
.mapping-help, .mapping-footer { color: var(--dm-text-secondary, #6b7280); font-size: 12px; line-height: 22px; }
.mapping-prompt { margin-bottom: 8px; color: var(--dm-text-regular, #374151); font-size: 12px; }
.mapping-viewport { max-height: 440px; overflow: auto; border: 1px solid var(--dm-border-color, #e2e9f3); border-radius: 8px; overscroll-behavior: contain; }
.mapping-board { position: relative; display: grid; grid-template-columns: minmax(210px, 1fr) 160px minmax(210px, 1fr); min-width: 620px; padding: 12px 18px 16px; background: #fff; }
.mapping-side { min-width: 0; }
.mapping-heading { display: flex; align-items: center; gap: 8px; height: 32px; margin: 0 0 8px; color: var(--dm-text-regular, #374151); font-size: 14px; line-height: 24px; }
.mapping-heading small { color: var(--dm-text-secondary, #6b7280); font-size: 12px; font-weight: 400; }
.mapping-list { display: flex; flex-direction: column; gap: 6px; }
.mapping-row { position: relative; display: flex; align-items: center; gap: 4px; height: 36px; min-width: 0; padding: 4px 6px; border: 1px solid var(--dm-border-color, #e2e9f3); border-radius: 6px; background: #fafbff; }
.mapping-row :deep(.ant-input) { flex: 1; min-width: 0; }
.mapping-row :deep(.ant-btn) { flex: 0 0 auto; }
.mapping-name { flex: 1; min-width: 0; overflow: hidden; color: var(--dm-text-regular, #374151); text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.mapping-system { flex-shrink: 0; color: var(--dm-text-secondary, #6b7280); font-size: 11px; }
.mapping-drag-handle { flex-shrink: 0; color: var(--dm-text-secondary, #6b7280); cursor: grab; }
.mapping-port { position: absolute; top: 50%; z-index: 3; display: grid; width: 24px; height: 24px; padding: 0; place-items: center; transform: translateY(-50%); border: 0; background: transparent; cursor: crosshair; touch-action: none; }
.mapping-port--source { right: -24px; }
.mapping-port--target { left: -24px; }
.mapping-port span { width: 10px; height: 10px; border: 2px solid var(--dm-color-primary, #4f7cff); border-radius: 50%; background: #fff; }
.mapping-port.is-connected span { background: var(--dm-color-primary, #4f7cff); }
.mapping-port.is-pending span, .mapping-port:focus-visible span { outline: 2px solid var(--dm-text-regular, #374151); outline-offset: 3px; }
.mapping-port:disabled { cursor: default; }
.mapping-lines { position: absolute; top: 0; left: 0; z-index: 2; overflow: hidden; pointer-events: none; }
.mapping-edge-line { fill: none; stroke: var(--dm-color-primary, #4f7cff); stroke-width: 2; stroke-linecap: round; pointer-events: none; }
.mapping-arrow { fill: none; stroke: var(--dm-color-primary, #4f7cff); stroke-width: 2; stroke-linejoin: round; }
.mapping-edge-hit { fill: none; stroke: transparent; stroke-width: 16; cursor: pointer; pointer-events: stroke; }
.mapping-edge:hover .mapping-edge-line, .mapping-edge:focus-within .mapping-edge-line { stroke-width: 3; }
.mapping-edge-hit:focus { outline: none; }
.mapping-edge-preview { stroke-dasharray: 5 4; }
.mapping-footer { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.mapping-details { margin-top: 8px; color: var(--dm-text-regular, #374151); font-size: 12px; }
.mapping-details summary { cursor: pointer; }
.mapping-details ul { margin: 8px 0 0; padding-left: 20px; }
.mapping-details li { min-height: 28px; }
@media (forced-colors: active) {
  .mapping-edge-line, .mapping-arrow { stroke: CanvasText; }
  .mapping-port span { border-color: CanvasText; }
  .mapping-port.is-connected span { background: CanvasText; }
}
</style>
