<template>
  <a-modal v-model:open="visibleDialog" wrap-class-name="integration-component-modal" :title="form.name || '逻辑节点'"
    :width="1100" :destroy-on-close="true">
    <a-form :model="form" :disabled="Boolean(info)" :label-col="{ style: { width: '120px' } }">
      <h3 class="etl-section-title">基本信息</h3>
      <a-form-item label="节点名称" required>
        <a-input v-model:value="form.name" placeholder="请输入节点名称" />
      </a-form-item>
      <a-form-item label="逻辑模式" required>
        <a-select v-model:value="logic.mode" :options="modeOptions" />
      </a-form-item>
      <a-form-item v-if="logic.mode === 'CONDITION'" label="前置状态关系">
        <a-radio-group v-model:value="logic.conditionRelation">
          <a-radio value="AND">全部前置成功</a-radio>
          <a-radio value="OR">至少一个前置成功（等待前置结束后判断）</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-alert v-if="logic.mode === 'CONDITION'" type="info" show-icon
        message="控制节点不传递数据或字段。成功、失败两路都需连接独立表输入；下游自行配置数据来源。需要执行失败分支时，DS 工作流失败策略须允许继续判断，不能在前置失败后立即终止整个流程。" />

      <h3 class="etl-section-title">上游表输出（只读）</h3>
      <a-alert :type="upstreamError ? 'warning' : 'info'" show-icon
        :message="upstreamError || '来源由画布实际入边决定；请在画布修改连线，不在此配置数据源或目标表。'" />
      <a-table class="logic-table" :data-source="upstreams" :columns="upstreamColumns" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'fields'">{{ record.fields.map(field => field.columnName).join(', ') || '未提供输出字段' }}</template>
        </template>
      </a-table>

      <template v-if="logic.mode === 'MERGE'">
        <h3 class="etl-section-title">合并配置</h3>
        <a-form-item label="合并方式">
          <a-select v-model:value="logic.mergeType" :options="[{ label: 'UNION ALL（同结构追加）', value: 'UNION_ALL' }, { label: 'JOIN（字段关联）', value: 'JOIN' }]" />
        </a-form-item>
        <div v-if="logic.mergeType === 'UNION_ALL'" class="logic-help">所有上游的业务列名称、顺序和已知类型须一致；来源表和写入时间由执行阶段统一追加／继承。</div>
        <template v-else>
          <a-alert type="info" show-icon message="JOIN 支持两个及以上来源，按配置顺序逐表关联，不设固定的两个来源上限。在同一数据库连接中执行，不是 Flink 跨源流 JOIN；复合来源不继承单表自然键或行 Hash。" />
          <JoinConfig ref="joinConfigRef" v-model="logic.joinConfig" :upstreams="upstreams" :info="Boolean(info)" />
        </template>
      </template>

      <template v-if="logic.mode === 'SPLIT'">
        <h3 class="etl-section-title">分流分支</h3>
        <a-form-item label="匹配策略">
          <a-radio-group v-model:value="logic.matchStrategy">
            <a-radio value="ALL">ALL（发送到全部命中分支）</a-radio>
            <a-radio value="FIRST">FIRST（按数组顺序首次命中）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="未匹配策略">
          <a-radio-group v-model:value="logic.unmatchedPolicy">
            <a-radio value="DROP">DROP（丢弃）</a-radio>
            <a-radio value="DEFAULT">DEFAULT（默认分支）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-alert type="info" show-icon message="条件填写 Flink SQL 布尔表达式，不含 WHERE；这里只保存配置，不执行或校验 SQL。默认分支仅接收未命中记录，最多一个。" />
        <a-button v-if="!info" class="logic-add" @click="addBranch">新增分支</a-button>
        <a-table class="logic-table" :data-source="logic.branches" :columns="branchColumns" row-key="id" :pagination="false" :scroll="{ x: 930 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'order'">{{ index + 1 }}</template>
            <template v-else-if="column.dataIndex === 'id'">
              <a-tooltip :title="isBranchReferenced(record.id) ? '已有出边引用，禁止删除或修改 ID' : '自动生成的稳定 ID，不随名称或顺序变化'">
                <span class="logic-id">{{ record.id }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'name'"><a-input v-model:value="record.name" placeholder="分支名称" /></template>
            <template v-else-if="column.dataIndex === 'condition'">
              <a-textarea v-model:value="record.condition" :disabled="Boolean(info) || record.isDefault" :rows="2" placeholder="例如：`amount` > 100" />
            </template>
            <template v-else-if="column.dataIndex === 'isDefault'">
              <a-checkbox :checked="record.isDefault" :disabled="Boolean(info)" @change="event => setDefault(record, event.target.checked)" />
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-space>
                <a-button type="link" size="small" :disabled="Boolean(info) || index === 0" @click="moveBranch(index, -1)">上移</a-button>
                <a-button type="link" size="small" :disabled="Boolean(info) || index === logic.branches.length - 1" @click="moveBranch(index, 1)">下移</a-button>
                <a-button type="link" danger size="small" :disabled="Boolean(info) || isBranchReferenced(record.id)" @click="removeBranch(record)">删除</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
        <div class="logic-help">ID 自动生成且只读。已有引用边时须先删除边才能删除分支。上移/下移直接调整数组顺序，FIRST 按此顺序匹配。</div>
      </template>
      <a-alert v-else-if="logic.mode === 'BROADCAST'" type="info" show-icon message="广播将同一份业务列传给全部下游，不配置条件或目标表。" />
      <a-alert v-else-if="logic.mode === 'REFERENCE'" type="info" show-icon message="引用上游表输出，业务列原样透传，不配置输入输出组件。" />

      <h3 class="etl-section-title">业务字段（只读）</h3>
      <a-table :data-source="businessFields" :columns="fieldColumns" row-key="columnName" :pagination="false" :scroll="{ y: 260 }" />
    </a-form>
    <template #footer>
      <a-button @click="closeDialog">关闭</a-button>
      <a-button v-if="!info" type="primary" @click="saveData">保存</a-button>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import JoinConfig from './JoinConfig.vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  currentNode: { type: Object, default: () => ({}) },
  graph: { type: Object, default: null },
  info: { type: [Boolean, String], default: false },
});
const emit = defineEmits(['update', 'confirm']);
const visibleDialog = computed({ get: () => props.visible, set: value => emit('update', value) });
const clone = value => JSON.parse(JSON.stringify(value));
const defaultLogic = () => ({ mode: 'REFERENCE', mergeType: 'UNION_ALL', matchStrategy: 'ALL', unmatchedPolicy: 'DROP', branches: [], conditionRelation: 'AND', joinConfig: { sources: [], joins: [], fields: [] } });
const form = ref({ name: '', taskParams: { logicNode: defaultLogic() } });
const logic = computed(() => form.value.taskParams.logicNode);
const joinConfigRef = ref(null);
const modeOptions = [
  { label: '引用（REFERENCE）', value: 'REFERENCE' },
  { label: '合并（MERGE）', value: 'MERGE' },
  { label: '分流（SPLIT）', value: 'SPLIT' },
  { label: '广播（BROADCAST）', value: 'BROADCAST' },
  { label: '任务条件控制（CONDITION）', value: 'CONDITION' },
];
const upstreamColumns = [
  { title: '上游节点', dataIndex: 'name', width: 200 },
  { title: '输出表', dataIndex: 'table', width: 220 },
  { title: '业务列', dataIndex: 'fields' },
];
const fieldColumns = [
  { title: '字段名', dataIndex: 'columnName' },
  { title: '类型', dataIndex: 'columnType' },
  { title: '说明', dataIndex: 'columnComment' },
];
const branchColumns = [
  { title: '顺序', key: 'order', width: 60 },
  { title: '稳定 ID', dataIndex: 'id', width: 180 },
  { title: '名称', dataIndex: 'name', width: 140 },
  { title: 'Flink SQL 条件', dataIndex: 'condition', width: 270 },
  { title: '默认', dataIndex: 'isDefault', width: 65 },
  { title: '操作', key: 'actions', width: 215 },
];

// X6 的图模型不属于 Vue 响应式状态，监听连线/上游数据变化使只读来源同步刷新。
const graphRevision = ref(0);
const refreshGraph = () => { graphRevision.value += 1; };
const graphEvents = ['edge:added', 'edge:removed', 'edge:change:source', 'edge:change:target', 'edge:change:data', 'node:change:data', 'node:removed'];
let boundGraph;
function unbindGraph() {
  graphEvents.forEach(event => boundGraph?.off?.(event, refreshGraph));
  boundGraph = null;
}
watch(() => props.graph, graph => {
  unbindGraph();
  boundGraph = graph;
  graphEvents.forEach(event => graph?.on?.(event, refreshGraph));
  refreshGraph();
}, { immediate: true });
onBeforeUnmount(unbindGraph);

function dataOf(node) {
  return node?.getData?.() || node?.getProp?.('data') || node?.data || {};
}
function currentCell() {
  return props.graph?.getCellById?.(props.currentNode?.id) || props.currentNode;
}
function edgesFor(direction) {
  graphRevision.value;
  const graph = props.graph;
  const cell = currentCell();
  if (!graph || !cell?.id) return [];
  const getter = direction === 'in' ? 'getIncomingEdges' : 'getOutgoingEdges';
  return graph[getter]?.(cell) || [];
}
function sourceFields(params) {
  const raw = Array.isArray(params.outputFields) && params.outputFields.length
    ? params.outputFields : params.target_columns;
  return (Array.isArray(raw) ? raw : []).map(field => {
    const item = typeof field === 'string' ? { columnName: field } : (field || {});
    return {
      ...item,
      columnName: String(item.columnName || item.name || '').trim(),
      columnType: item.columnType || item.dataType || item.type || '',
      columnComment: item.columnComment || item.comment || '',
    };
  }).filter(field => field.columnName && !['source_table', 'write_time', 'source_natural_key', 'source_row_hash', 'source_datasource', 'source_database', 'source_task', 'source_batch', 'source_update_time', 'record_op'].includes(field.columnName.toLowerCase()));
}
const upstreams = computed(() => {
  const nodes = new Map();
  edgesFor('in').forEach(edge => {
    const node = edge.getSourceCell?.() || props.graph?.getCellById?.(edge.getSourceCellId?.());
    if (!node || nodes.has(node.id)) return;
    const data = dataOf(node);
    const params = data.taskParams || {};
    nodes.set(node.id, {
      id: node.id,
      name: data.name || node.id,
      table: params.target_table_name || params.tableName || params.table_name || params.pipelineNode?.output?.table || '上游逻辑表输出',
      fields: sourceFields(params),
      datasourceId: params.writerDatasource?.datasourceId || params.writerDatasource?.id || '',
      datasourceType: params.writerDatasource?.datasourceType || '',
    });
  });
  return [...nodes.values()];
});
const businessFields = computed(() => {
  if (logic.value.mode === 'CONDITION') return [];
  if (logic.value.mode === 'MERGE' && logic.value.mergeType === 'JOIN') {
    return (logic.value.joinConfig?.fields || []).map(projection => {
      const source = upstreams.value.find(upstream => upstream.id === projection.nodeId);
      const field = source?.fields.find(item => item.columnName === projection.column) || {};
      return { ...field, columnName: projection.outputName, pkFlag: '0', nullableFlag: '1' };
    });
  }
  return upstreams.value[0]?.fields || [];
});
const upstreamError = computed(() => {
  const count = upstreams.value.length;
  if (logic.value.mode === 'CONDITION') return count ? '' : '条件控制至少需要一个前置表输出';
  if (logic.value.mode === 'MERGE' ? count < 2 : count !== 1) {
    return logic.value.mode === 'MERGE' ? '合并模式至少需要两个实际上游节点。' : '引用、分流和广播模式必须恰有一个实际上游节点。';
  }
  if (upstreams.value.some(node => !node.fields.length)) return '上游尚未提供 taskParams.outputFields / target_columns，请先保存上游表输出。';
  return '';
});

// 同时识别显式分支引用及 X6 source.port；不通过边标签猜测分支。
function referencedBranchIds() {
  const ids = new Set();
  edgesFor('out').forEach(edge => {
    const data = dataOf(edge);
    [data.branchId, data.sourceBranchId, data.logicNode?.branchId, edge.getSourcePortId?.(), edge.getSource?.()?.port]
      .filter(value => value != null && value !== '').forEach(value => ids.add(String(value)));
  });
  return ids;
}
function isBranchReferenced(id) {
  return referencedBranchIds().has(String(id));
}
let branchSequence = 0;
function newBranchId() {
  let id;
  const used = new Set([...logic.value.branches.map(branch => String(branch.id)), ...referencedBranchIds()]);
  do {
    branchSequence += 1;
    id = `branch_${Date.now().toString(36)}_${branchSequence.toString(36)}`;
  } while (used.has(id));
  return id;
}
function addBranch() {
  if (props.info) return;
  logic.value.branches.push({ id: newBranchId(), name: `分支 ${logic.value.branches.length + 1}`, condition: '', isDefault: false });
}
function removeBranch(branch) {
  if (props.info) return;
  if (isBranchReferenced(branch.id)) return message.warning('该分支仍被出边引用，请先删除对应连线');
  const index = logic.value.branches.indexOf(branch);
  if (index >= 0) logic.value.branches.splice(index, 1);
}
function setDefault(branch, checked) {
  if (props.info) return;
  if (checked) logic.value.branches.forEach(item => { item.isDefault = item === branch; });
  else branch.isDefault = false;
}
function moveBranch(index, offset) {
  if (props.info) return;
  const next = index + offset;
  if (next < 0 || next >= logic.value.branches.length) return;
  const [branch] = logic.value.branches.splice(index, 1);
  logic.value.branches.splice(next, 0, branch);
}

let originalBranches = [];
let originalMode = 'REFERENCE';
watch(() => [props.visible, props.currentNode?.id], ([visible]) => {
  if (!visible) return;
  const data = clone(dataOf(props.currentNode));
  const params = data.taskParams || {};
  const saved = params.logicNode || {};
  form.value = { ...data, name: data.name || '', taskParams: { ...params, logicNode: { ...defaultLogic(), ...saved, branches: [] } } };
  logic.value.branches = (Array.isArray(saved.branches) ? saved.branches : []).map(branch => ({
    id: branch.id == null || branch.id === '' ? newBranchId() : String(branch.id),
    name: branch.name || '', condition: branch.condition || '', isDefault: Boolean(branch.isDefault),
  }));
  originalBranches = logic.value.branches.map(branch => branch.id);
  originalMode = logic.value.mode;
  refreshGraph();
}, { immediate: true });

function closeDialog() { emit('update', false); }
function saveData() {
  if (props.info) return;
  if (!form.value.name.trim()) return message.warning('请输入节点名称');
  if (!modeOptions.some(option => option.value === logic.value.mode)) return message.warning('请选择有效的逻辑模式');
  // 先保存模式才能连接第二个合并输入；完整拓扑在保存任务/发布时校验。
  if (upstreamError.value) message.info('已保存逻辑配置，请按提示补齐上游连线和字段后再保存任务');
  const config = logic.value;
  if (!['UNION_ALL', 'JOIN'].includes(config.mergeType)) return message.warning('请选择有效的合并方式');
  const referenced = referencedBranchIds();
  if (originalBranches.some(id => referenced.has(id) && !config.branches.some(branch => branch.id === id))) {
    return message.warning('仍有出边引用已移除的分支，请先删除连线');
  }
  if (originalMode === 'CONDITION' && config.mode !== 'CONDITION' && edgesFor('out').length) return message.warning('请先移除控制分支连线再切换模式');
  if (originalMode === 'SPLIT' && config.mode !== 'SPLIT' && originalBranches.some(id => referenced.has(id))) {
    return message.warning('请先移除分支引用边，再切换逻辑模式');
  }
  if (config.mode === 'CONDITION' && !['AND', 'OR'].includes(config.conditionRelation)) return message.warning('请选择有效的前置状态关系');
  if (config.mode === 'MERGE' && config.mergeType === 'JOIN') {
    const connections = new Set(upstreams.value.map(source => String(source.datasourceId || '')));
    if (connections.size > 1 || connections.has('')) return message.warning('JOIN 的全部上游输出必须位于同一个已登记数据源连接');
    const error = joinConfigRef.value?.validate?.();
    if (error) return message.warning(error);
  }
  if (config.mode === 'MERGE' && config.mergeType === 'UNION_ALL') {
    const first = businessFields.value;
    const mismatch = upstreams.value.some(node => node.fields.length !== first.length || node.fields.some((field, index) => {
      const expected = first[index];
      return field.columnName !== expected.columnName || (field.columnType && expected.columnType
        && String(field.columnType).toUpperCase() !== String(expected.columnType).toUpperCase());
    }));
    if (mismatch) return message.warning('UNION ALL 要求所有上游业务列名称、顺序和类型一致');
  }
  if (config.mode === 'SPLIT') {
    if (!['ALL', 'FIRST'].includes(config.matchStrategy) || !['DROP', 'DEFAULT'].includes(config.unmatchedPolicy)) {
      return message.warning('请选择有效的匹配策略和未匹配策略');
    }
    if (!config.branches.length) return message.warning('请至少新增一个分支');
    const ids = new Set();
    for (const branch of config.branches) {
      if (!branch.id || ids.has(branch.id)) return message.warning('分支 ID 不可为空或重复');
      ids.add(branch.id);
      if (!branch.name.trim()) return message.warning('请填写分支名称');
      if (!branch.isDefault && !branch.condition.trim()) return message.warning('非默认分支须填写 Flink SQL 条件');
    }
    const defaults = config.branches.filter(branch => branch.isDefault).length;
    if (defaults > 1 || (config.unmatchedPolicy === 'DEFAULT' && defaults !== 1)) {
      return message.warning('默认分支最多一个，DEFAULT 策略必须配置一个默认分支');
    }
  }
  const result = clone(form.value);
  result.name = result.name.trim();
  // 纯逻辑节点不携带旧集成节点的数据源、物理目标或输入输出包装配置。
  result.taskParams = {
    type: 3,
    typeName: '逻辑节点',
    logicNode: {
      mode: config.mode, mergeType: config.mergeType, matchStrategy: config.matchStrategy,
      joinConfig: clone(config.joinConfig || { sources: [], joins: [], fields: [] }),
      unmatchedPolicy: config.unmatchedPolicy, conditionRelation: config.conditionRelation || 'AND',
      branches: clone(config.branches).map(branch => ({
        id: branch.id, name: branch.name.trim(), condition: branch.isDefault ? '' : branch.condition.trim(), isDefault: branch.isDefault,
      })),
    },
    tableFields: clone(businessFields.value),
    inputFields: clone(businessFields.value),
    outputFields: clone(businessFields.value),
  };
  emit('confirm', result);
  closeDialog();
}
</script>

<style scoped>
.logic-table { margin: 16px 0; }
.logic-help { color: var(--ant-color-text-secondary, #666); font-size: 12px; margin: 8px 0 16px; }
.logic-add { margin-top: 16px; }
.logic-id { overflow-wrap: anywhere; font-family: monospace; }
</style>
