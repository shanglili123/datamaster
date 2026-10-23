<template>
  <a-modal wrap-class-name="integration-component-modal" v-model:open="visibleDialog" :title="form.name || '逻辑集成节点'" :width="900" :destroy-on-close="true">
    <a-form :model="form" :label-col="{ style: { width: '120px' } }" :disabled="info">
      <h3 class="etl-section-title">基本信息</h3>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="节点名称" required>
            <a-input v-if="!info" v-model:value="form.name" placeholder="请输入节点名称" />
            <div v-else class="form-readonly">{{ form.name || '-' }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="节点模式">
            <a-radio-group v-if="!info" v-model:value="form.taskParams.pipelineNode.mode">
              <a-radio value="INTEGRATION">普通集成</a-radio>
              <a-radio value="MERGE">数据合并</a-radio>
            </a-radio-group>
            <div v-else class="form-readonly">{{ modeLabel }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <template v-if="form.taskParams.pipelineNode.mode === 'INTEGRATION'">
        <h3 class="etl-section-title">输入来源</h3>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="源数据连接" required>
              <a-select v-if="!info" :value="form.taskParams.pipelineNode.readerDatasource.datasourceId" show-search :options="datasourceOptions.map(item => ({ label: item.datasourceName, value: String(item.id) }))" @change="value => selectDatasource('reader', value)" placeholder="请选择源数据连接" />
              <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.readerDatasource.datasourceId || '-' }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="源表" required>
              <a-input v-if="!info" v-model:value="form.taskParams.pipelineNode.inputs[0].table" placeholder="请输入源表名" />
              <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.inputs[0].table || '-' }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="上游来源节点">
              <a-select v-if="!info" v-model:value="form.taskParams.pipelineNode.inputs[0].refNodeId" allow-clear :options="upstreamOptions" placeholder="可选：引用上游节点输出" />
              <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.inputs[0].refNodeId || '-' }}</div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="源字段">
          <a-space>
            <a-button v-if="!info" :loading="fieldsLoading" @click="loadColumns">读取源表字段</a-button>
            <span>{{ fieldNames.length ? `已读取 ${fieldNames.length} 个字段` : '尚未读取字段' }}</span>
          </a-space>
        </a-form-item>
      </template>

      <template v-else>
        <h3 class="etl-section-title">合并配置</h3>
        <a-form-item label="源数据连接" required>
          <a-select v-if="!info" :value="form.taskParams.pipelineNode.readerDatasource.datasourceId" show-search :options="datasourceOptions.map(item => ({ label: item.datasourceName, value: String(item.id) }))" @change="value => selectDatasource('reader', value)" placeholder="请选择源数据连接" />
          <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.readerDatasource.datasourceId || '-' }}</div>
        </a-form-item>
        <a-form-item label="合并方式">
          <a-select v-if="!info" v-model:value="form.taskParams.pipelineNode.mergeConfig.type">
            <a-select-option value="UNION_ALL">UNION ALL（结构相同表）</a-select-option>
            <a-select-option value="JOIN" disabled>JOIN（后续支持）</a-select-option>
          </a-select>
          <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.mergeConfig.type || 'UNION_ALL' }}</div>
        </a-form-item>
        <a-form-item label="输入节点">
          <a-select v-if="!info" v-model:value="mergeInputNodeIds" mode="multiple" :options="upstreamOptions" placeholder="选择上游集成节点" />
          <div v-else class="form-readonly">{{ mergeInputLabels }}</div>
        </a-form-item>
        <a-form-item label="合并字段">
          <a-space>
            <a-button v-if="!info" :loading="fieldsLoading" @click="loadColumns">读取上游字段</a-button>
            <span>{{ fieldNames.length ? `已读取 ${fieldNames.length} 个字段` : '尚未读取字段' }}</span>
          </a-space>
        </a-form-item>
      </template>

      <h3 class="etl-section-title">目标与写入</h3>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="目标数据连接" required>
            <a-select v-if="!info" :value="form.taskParams.pipelineNode.output.writerDatasource.datasourceId" show-search :options="datasourceOptions.map(item => ({ label: item.datasourceName, value: String(item.id) }))" @change="value => selectDatasource('writer', value)" placeholder="请选择目标数据连接" />
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.output.writerDatasource.datasourceId || '-' }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="写入模式">
            <a-select v-if="!info" v-model:value="form.taskParams.pipelineNode.output.writeModeType">
              <a-select-option value="1">全量覆盖</a-select-option>
              <a-select-option value="2">追加写入</a-select-option>
              <a-select-option value="3">增量更新（Upsert）</a-select-option>
            </a-select>
            <div v-else class="form-readonly">{{ writeModeLabel }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="流程层级">
            <a-select v-if="!info" v-model:value="form.taskParams.pipelineNode.layer">
              <a-select-option value="ODS">ODS 原始层</a-select-option>
              <a-select-option value="DWD">DWD 明细层</a-select-option>
              <a-select-option value="DWS">DWS 汇总层</a-select-option>
            </a-select>
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.layer || '-' }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="目标表" required>
            <a-input v-if="!info" v-model:value="form.taskParams.pipelineNode.output.table" placeholder="请输入目标表名" />
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.output.table || '-' }}</div>
            <div class="form-help">目标表需预先包含来源表 source_table、写入时间 write_time 及启用的可选追溯列；保存任务不会自动补列</div>
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item v-if="String(form.taskParams.pipelineNode.output.writeModeType) === '3'" label="更新主键字段">
        <a-input v-if="!info" v-model:value="form.taskParams.pipelineNode.output.selectedColumnsText" placeholder="多个字段用逗号分隔，例如 ID,ORG_ID" />
        <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.output.selectedColumns?.join(', ') || '-' }}</div>
      </a-form-item>

      <a-alert v-if="String(form.taskParams.pipelineNode.output.writeModeType) === '1'" type="warning" show-icon
        message="全量模式将在任务运行前清空目标表，请确认目标表和写入方式。" style="margin-bottom: 16px" />
      <h3 class="etl-section-title">来源追溯</h3>
      <a-form-item label="必写字段">
        <span>来源表 source_table、写入时间 write_time（转换处理时间）</span>
      </a-form-item>
      <a-form-item label="可选追溯字段">
        <a-space wrap>
          <a-checkbox v-model:checked="form.taskParams.pipelineNode.sourceMetadata.includeNaturalKey">来源自然键</a-checkbox>
          <a-checkbox v-model:checked="form.taskParams.pipelineNode.sourceMetadata.includeRowHash">来源行 Hash</a-checkbox>
          <a-checkbox v-model:checked="form.taskParams.pipelineNode.sourceMetadata.includeSourceDatasource">来源数据源</a-checkbox>
          <a-checkbox v-model:checked="form.taskParams.pipelineNode.sourceMetadata.includeSourceDatabase">来源数据库</a-checkbox>
          <a-checkbox v-model:checked="form.taskParams.pipelineNode.sourceMetadata.includeSourceTask">来源任务</a-checkbox>
          <a-checkbox v-model:checked="form.taskParams.pipelineNode.sourceMetadata.includeSourceBatch" :disabled="!form.taskParams.pipelineNode.sourceMetadata.includeSourceBatch">来源批次（暂不支持，请取消旧配置）</a-checkbox>
          <a-checkbox v-model:checked="form.taskParams.pipelineNode.sourceMetadata.includeSourceUpdateTime">源端更新时间</a-checkbox>
          <a-checkbox v-model:checked="form.taskParams.pipelineNode.sourceMetadata.includeRecordOp">操作类型</a-checkbox>
        </a-space>
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12" v-if="form.taskParams.pipelineNode.sourceMetadata.includeNaturalKey">
          <a-form-item label="自然键字段">
            <a-input v-if="!info" v-model:value="form.taskParams.pipelineNode.sourceMetadata.naturalKeyColumnsText" placeholder="源主键字段，多个用逗号分隔" />
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.sourceMetadata.naturalKeyColumnsText || '-' }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12" v-if="form.taskParams.pipelineNode.sourceMetadata.includeSourceUpdateTime">
          <a-form-item label="源更新时间字段">
            <a-input v-if="!info" v-model:value="form.taskParams.pipelineNode.sourceMetadata.sourceUpdateTimeSourceColumn" placeholder="源表真实字段，非自动采集 CDC 时间" />
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.sourceMetadata.sourceUpdateTimeSourceColumn || '-' }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12" v-if="form.taskParams.pipelineNode.sourceMetadata.includeRecordOp">
          <a-form-item label="操作类型字段">
            <a-input v-if="!info" v-model:value="form.taskParams.pipelineNode.sourceMetadata.recordOpSourceColumn" placeholder="源表真实字段，非自动解析 CDC 事件" />
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.sourceMetadata.recordOpSourceColumn || '-' }}</div>
          </a-form-item>
        </a-col>
      </a-row>

      <h3 class="etl-section-title">运行治理</h3>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="Schema 监控">
            <a-checkbox v-if="!info" v-model:checked="form.taskParams.pipelineNode.schemaMonitoring.enabled">执行前检查源结构</a-checkbox>
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.schemaMonitoring.enabled ? '已启用' : '未启用' }}</div>
          </a-form-item>
          <a-form-item v-if="form.taskParams.pipelineNode.schemaMonitoring.enabled" label="元数据探查任务">
            <a-select v-if="!info" v-model:value="form.taskParams.pipelineNode.schemaMonitoring.taskId" show-search allow-clear :options="schemaTaskOptions" placeholder="选择已有元数据探查任务" />
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.schemaMonitoring.taskId || '-' }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="质量检查">
            <a-checkbox v-if="!info" v-model:checked="form.taskParams.pipelineNode.output.qualityConfig.enabled">写入后执行质量检查</a-checkbox>
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.output.qualityConfig.enabled ? '已启用' : '未启用' }}</div>
          </a-form-item>
          <a-form-item v-if="form.taskParams.pipelineNode.output.qualityConfig.enabled" label="质量检查任务">
            <a-select v-if="!info" v-model:value="form.taskParams.pipelineNode.output.qualityConfig.taskId" show-search allow-clear :options="qualityTaskOptions" placeholder="选择已有质量检查任务" />
            <div v-else class="form-readonly">{{ form.taskParams.pipelineNode.output.qualityConfig.taskId || '-' }}</div>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <template #footer>
      <a-button @click="closeDialog">取消</a-button>
      <a-button v-if="!info" type="primary" @click="saveData">确定</a-button>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { listDaDatasource, getDaDatasource } from '@/api/ast/dataSource/dataSource.js';
import { getColumnByAssetId } from '@/api/col/task/index.js';
import { listTask } from '@/api/cat/task/task.js';
import { listDppQualityTask } from '@/api/ast/quality/qualityTask.js';
import useUserStore from '@/store/system/user.js';

const props = defineProps({
  visible: { type: Boolean, default: false },
  currentNode: { type: Object, default: () => ({}) },
  graph: { type: Object, default: null },
  info: { type: [Boolean, String], default: false },
});
const emit = defineEmits(['update', 'confirm']);
const userStore = useUserStore();
const visibleDialog = computed({
  get: () => props.visible,
  set: value => emit('update', value),
});

const createDefault = () => ({
  name: '',
  taskParams: {
    type: 3,
    typeName: '逻辑集成节点',
    pipelineNode: {
      mode: 'INTEGRATION',
      layer: 'ODS',
      pipelineNodeId: '',
      inputs: [{ kind: 'TABLE', table: '', refNodeId: '', alias: '' }],
      transforms: [],
      mergeConfig: { type: 'UNION_ALL' },
      output: {
        table: '',
        writerDatasource: { datasourceId: '', datasourceType: '', dbname: '' },
        writeModeType: '2',
        selectedColumns: [],
        selectedColumnsText: '',
        qualityConfig: { enabled: false, onFailure: 'FAIL', rules: [], taskId: '' },
      },
      readerDatasource: { datasourceId: '', datasourceType: '', dbname: '' },
      schemaMonitoring: { enabled: false, onDrift: 'FAIL', taskId: '' },
      sourceMetadata: { enabled: true, includeNaturalKey: false, includeRowHash: false, includeSourceDatasource: false, includeSourceDatabase: false, includeSourceTask: false, includeSourceBatch: false, includeSourceUpdateTime: false, includeRecordOp: false, naturalKeyColumns: [], naturalKeyColumnsText: '', sourceUpdateTimeSourceColumn: '', recordOpSourceColumn: '' },
    },
  },
});
const form = ref(createDefault());
const datasourceOptions = ref([]);
const schemaTaskOptions = ref([]);
const qualityTaskOptions = ref([]);
const fieldsLoading = ref(false);
const fieldNames = computed(() => (form.value.taskParams.pipelineNode.columns || []).map(item => {
  if (typeof item === 'string') return item;
  return item?.columnName || item?.name || '';
}).filter(Boolean));

const upstreamOptions = computed(() => {
  const currentId = props.currentNode?.id;
  return (props.graph?.getNodes?.() || [])
    .filter(node => node.id !== currentId && String(node.getData?.()?.componentType) === '100')
    .map(node => ({ label: node.getData()?.name || node.id, value: node.id }));
});
const modeLabel = computed(() => form.value.taskParams.pipelineNode.mode === 'MERGE' ? '数据合并' : '普通集成');
const writeModeLabel = computed(() => ({ '1': '全量覆盖', '2': '追加写入', '3': '增量更新（Upsert）' })[
  String(form.value.taskParams.pipelineNode.output.writeModeType)] || '追加写入');
const mergeInputLabels = computed(() => (form.value.taskParams.pipelineNode.inputs || [])
  .map(item => {
    const ref = item?.refNodeId || item?.refNodeCode || item;
    return upstreamOptions.value.find(option => option.value === ref)?.label || ref;
  })
  .join(', ') || '-');

// 合并节点保存完整输入对象，但多选框只使用节点 ID；这里做双向适配，
// 保证手工连线、编辑回显和表单选择最终写入同一份 inputs 配置。
const mergeInputNodeIds = computed({
  get: () => (form.value.taskParams.pipelineNode.inputs || [])
    .map(item => typeof item === 'string' ? item : (item?.refNodeId || item?.refNodeCode || ''))
    .filter(Boolean),
  set: (values) => {
    const node = form.value.taskParams.pipelineNode;
    const previous = Array.isArray(node.inputs) ? node.inputs : [];
    const selectedValues = Array.isArray(values) ? values : [];
    node.inputs = selectedValues.map(value => {
      const id = String(value);
      const old = previous.find(item => String(item?.refNodeId || item?.refNodeCode || item) === id);
      const upstream = props.graph?.getCellById?.(id);
      return {
        ...(typeof old === 'object' ? old : {}),
        kind: 'NODE_OUTPUT',
        refNodeId: id,
        refNodeCode: old?.refNodeCode || upstream?.getData?.()?.code || id,
        alias: old?.alias || upstream?.getData?.()?.name || id,
      };
    });
    const upstreamDatasources = node.inputs
      .map(item => props.graph?.getCellById?.(item.refNodeId)?.getData?.()?.taskParams?.pipelineNode
        ?.output?.writerDatasource)
      .filter(item => item?.datasourceId != null);
    const datasourceIds = [...new Set(upstreamDatasources.map(item => String(item.datasourceId)))];
    if (datasourceIds.length === 1 && upstreamDatasources.length === node.inputs.length) {
      const selectedDatasource = upstreamDatasources[0];
      node.readerDatasource = { ...selectedDatasource };
    }
  },
});

async function loadDatasources() {
  try {
    const response = await listDaDatasource({
      pageSize: 9999,
      spaceCode: userStore.spaceCode,
      spaceId: userStore.spaceId,
      datasourceType: 'DM8,Oracle11,MySql,Oracle,Kingbase8,Doris,ClickHouse,Hive,PostgreSQL,SQL_Server,SQL_Server2008',
    });
    datasourceOptions.value = response?.data?.rows || [];
  } catch (e) {
    datasourceOptions.value = [];
  }
}

function responseRows(response) {
  const data = response?.data ?? response ?? {};
  if (Array.isArray(data)) return data;
  return Array.isArray(data.rows) ? data.rows
    : Array.isArray(data.list) ? data.list
      : Array.isArray(data.records) ? data.records : [];
}

function taskOptions(rows, nameKeys) {
  return rows
    .filter(item => item && item.id != null)
    .map(item => {
      const name = nameKeys.map(key => item[key]).find(value => value != null && String(value).trim())
        || `任务 ${item.id}`;
      return { label: `${name}（${item.id}）`, value: String(item.id) };
    });
}

function keepSelectedTaskOption(options, value, label) {
  const selected = String(value || '').trim();
  if (selected && !options.some(option => String(option.value) === selected)) {
    options.unshift({ label: `${label}（${selected}）`, value: selected });
  }
  return options;
}

async function loadGovernanceTasks() {
  const commonQuery = {
    pageNum: 1,
    pageSize: 9999,
    spaceCode: userStore.spaceCode,
    spaceId: userStore.spaceId,
  };
  const [schemaResult, qualityResult] = await Promise.allSettled([
    listTask({ ...commonQuery, collectType: '1' }),
    listDppQualityTask(commonQuery),
  ]);
  const node = form.value.taskParams.pipelineNode;
  const schemaRows = schemaResult.status === 'fulfilled' ? responseRows(schemaResult.value) : [];
  const qualityRows = qualityResult.status === 'fulfilled' ? responseRows(qualityResult.value) : [];
  schemaTaskOptions.value = keepSelectedTaskOption(
    taskOptions(schemaRows, ['name', 'taskName']), node.schemaMonitoring?.taskId, '元数据探查任务');
  qualityTaskOptions.value = keepSelectedTaskOption(
    taskOptions(qualityRows, ['taskName', 'name']), node.output?.qualityConfig?.taskId, '质量检查任务');
}
async function selectDatasource(kind, value) {
  if (!value) return;
  try {
    const response = await getDaDatasource(value);
    const item = response?.data || {};
    let dbname = item.dbname || '';
    try {
      const config = typeof item.datasourceConfig === 'string' ? JSON.parse(item.datasourceConfig) : (item.datasourceConfig || {});
      dbname = dbname || config.dbname || config.database || '';
    } catch (e) {
      // 保留空实例名，后端仍可从数据源配置解析。
    }
    const node = form.value.taskParams.pipelineNode;
    if (kind === 'reader') {
      node.readerDatasource = { ...node.readerDatasource, ...item, datasourceId: String(item.id || value), dbname };
    } else {
      node.output.writerDatasource = { ...node.output.writerDatasource, ...item, datasourceId: String(item.id || value), dbname };
    }
  } catch (e) {
    message.error('获取数据源信息失败');
  }
}

function resolveFieldSource() {
  const node = form.value.taskParams.pipelineNode;
  const first = node.inputs?.[0] || {};
  let table = first.table || '';
  let datasourceId = node.readerDatasource?.datasourceId || '';
  if (first.refNodeId && props.graph?.getCellById) {
    const upstream = props.graph.getCellById(first.refNodeId);
    const upstreamPipeline = upstream?.getData?.()?.taskParams?.pipelineNode || {};
    // NODE_OUTPUT 的表和连接必须来自上游输出，不能被当前节点残留的源连接覆盖。
    table = upstreamPipeline.output?.table || upstreamPipeline.output?.target_table_name || table || '';
    datasourceId = upstreamPipeline.output?.writerDatasource?.datasourceId || datasourceId || '';
  }
  return { table: String(table || '').trim(), datasourceId: String(datasourceId || '').trim() };
}

async function loadColumns() {
  const { table, datasourceId } = resolveFieldSource();
  if (!table || !datasourceId) {
    return message.warning('请先选择数据源并配置源表或上游节点');
  }
  const node = form.value.taskParams.pipelineNode;
  const first = node.inputs?.[0] || {};
  const upstream = first.refNodeId && props.graph?.getCellById
    ? props.graph.getCellById(first.refNodeId)?.getData?.()?.taskParams?.pipelineNode : null;
  if (upstream?.output?.writerDatasource) {
    node.readerDatasource = { ...upstream.output.writerDatasource };
  }
  fieldsLoading.value = true;
  try {
    const response = await getColumnByAssetId({ withRule: 2, id: datasourceId, tableName: table });
    const rawFields = response?.data?.rows || response?.data || [];
    const fields = (Array.isArray(rawFields) ? rawFields : [])
      .map(item => typeof item === 'string' ? { columnName: item, columnType: 'STRING' } : item)
      .filter(item => item && String(item.columnName || item.name || '').trim())
      .map(item => ({
        ...item,
        columnName: String(item.columnName || item.name).trim(),
        columnType: item.columnType || item.type || 'STRING',
      }));
    if (!fields.length) return message.warning('未读取到源表字段，请确认数据源、库表名称和权限');
    node.tableFields = fields;
    node.columns = fields.map(item => item.columnName);
    if (!Array.isArray(node.targetColumns) || !node.targetColumns.length) {
      node.targetColumns = [...node.columns];
    }
    message.success(`已读取 ${fields.length} 个字段`);
  } catch (e) {
    message.error('读取源表字段失败');
  } finally {
    fieldsLoading.value = false;
  }
}

function normalize(data) {
  const base = createDefault();
  const source = data || {};
  const params = source.taskParams || {};
  const pipeline = params.pipelineNode || {};
  const merged = {
    ...base,
    ...source,
    taskParams: {
      ...base.taskParams,
      ...params,
      pipelineNode: {
        ...base.taskParams.pipelineNode,
        ...pipeline,
        output: {
          ...base.taskParams.pipelineNode.output,
          ...(pipeline.output || {}),
          writerDatasource: {
            ...base.taskParams.pipelineNode.output.writerDatasource,
            ...((pipeline.output || {}).writerDatasource || {}),
          },
        },
        readerDatasource: { ...base.taskParams.pipelineNode.readerDatasource, ...(pipeline.readerDatasource || {}) },
        mergeConfig: { ...base.taskParams.pipelineNode.mergeConfig, ...(pipeline.mergeConfig || {}) },
        schemaMonitoring: { ...base.taskParams.pipelineNode.schemaMonitoring, ...(pipeline.schemaMonitoring || {}) },
        sourceMetadata: { ...base.taskParams.pipelineNode.sourceMetadata, ...(pipeline.sourceMetadata || {}) },
      },
    },
  };
  const node = merged.taskParams.pipelineNode;
  // 逻辑集成节点的治理契约要求 source_table、write_time 必须落库。
  node.sourceMetadata.enabled = true;
  node.columns = Array.isArray(node.columns) ? node.columns : [];
  node.targetColumns = Array.isArray(node.targetColumns) ? node.targetColumns : [];
  node.tableFields = Array.isArray(node.tableFields) ? node.tableFields : [];
  if (!node.columns.length && Array.isArray(node.input?.columns)) node.columns = [...node.input.columns];
  if (!node.tableFields.length && Array.isArray(node.input?.tableFields)) node.tableFields = [...node.input.tableFields];
  if (!node.targetColumns.length && Array.isArray(node.output?.target_columns)) node.targetColumns = [...node.output.target_columns];
  node.output.writeModeType = String(node.output.writeModeType || '2');
  node.output.selectedColumns = Array.isArray(node.output.selectedColumns) ? node.output.selectedColumns : [];
  node.output.selectedColumnsText = node.output.selectedColumnsText
    || node.output.selectedColumns.join(',');
  if (node.schemaMonitoring.taskId != null && node.schemaMonitoring.taskId !== '') {
    node.schemaMonitoring.taskId = String(node.schemaMonitoring.taskId);
  }
  if (node.output.qualityConfig.taskId != null && node.output.qualityConfig.taskId !== '') {
    node.output.qualityConfig.taskId = String(node.output.qualityConfig.taskId);
  }
  if (!node.sourceMetadata.naturalKeyColumnsText && Array.isArray(node.sourceMetadata.naturalKeyColumns)) {
    node.sourceMetadata.naturalKeyColumnsText = node.sourceMetadata.naturalKeyColumns.join(',');
  }
  if (node.mode === 'MERGE') {
    node.inputs = (Array.isArray(node.inputs) ? node.inputs : []).map(item => typeof item === 'string'
      ? { kind: 'NODE_OUTPUT', refNodeId: item }
      : { ...item, refNodeId: item?.refNodeId || item?.refNodeCode || '' });
  } else {
    const input = typeof node.inputs?.[0] === 'string'
      ? { kind: 'TABLE', table: node.inputs[0] }
      : (node.inputs?.[0] || { kind: 'TABLE', table: '', refNodeId: '', alias: '' });
    node.inputs = [{ ...input, refNodeId: input.refNodeId || input.refNodeCode || '' }];
  }
  return merged;
}

watch(() => [props.visible, props.currentNode?.id], ([visible]) => {
  if (visible) {
    form.value = normalize(props.currentNode?.getProp?.('data') || props.currentNode?.data || {});
    loadDatasources();
    loadGovernanceTasks();
  }
});

function saveData() {
  const node = form.value.taskParams.pipelineNode;
  node.sourceMetadata.enabled = true;
  if (node.sourceMetadata.includeSourceBatch && !node.sourceMetadata.sourceBatchValue) {
    return message.warning('自动来源批次尚未支持，请取消旧的来源批次选项');
  }
  if (node.sourceMetadata?.naturalKeyColumnsText !== undefined) {
    node.sourceMetadata.naturalKeyColumns = String(node.sourceMetadata.naturalKeyColumnsText || '')
      .split(',').map(item => item.trim()).filter(Boolean);
  }
  node.output.writeModeType = String(node.output.writeModeType || '2');
  node.output.selectedColumns = String(node.output.selectedColumnsText || '')
    .split(',').map(item => item.trim()).filter(Boolean);
  if (node.mode === 'MERGE') {
    node.inputs = (node.inputs || []).map(item => typeof item === 'string'
      ? { kind: 'NODE_OUTPUT', refNodeId: item }
      : { ...item, refNodeId: item?.refNodeId || item?.refNodeCode || '' });
  }
  if (!String(form.value.name || '').trim()) return message.warning('请输入节点名称');
  if (!String(node.output.table || '').trim()) return message.warning('请输入目标表');
  if (!fieldNames.value.length) return message.warning('请先读取源表字段');
  if (!String(node.output.writerDatasource?.datasourceId || '').trim()) {
    return message.warning('请选择目标数据连接');
  }
  if (node.mode === 'MERGE' && !String(node.readerDatasource?.datasourceId || '').trim()) {
    return message.warning('合并节点请选择读取 ODS 的数据连接');
  }
  if (node.mode === 'INTEGRATION'
    && !node.inputs?.[0]?.refNodeId
    && !String(node.readerDatasource?.datasourceId || '').trim()) {
    return message.warning('请选择源数据连接');
  }
  if (node.output.writeModeType === '3'
    && !node.output.selectedColumns.length
    && String(node.output.writerDatasource?.datasourceType || '').toLowerCase() !== 'doris') {
    return message.warning('增量更新请配置更新主键字段');
  }
  if (node.mode === 'INTEGRATION' && !String(node.inputs?.[0]?.table || '').trim() && !node.inputs?.[0]?.refNodeId) {
    return message.warning('请输入源表或选择上游节点');
  }
  if (node.mode === 'MERGE' && (!Array.isArray(node.inputs) || node.inputs.length < 2)) {
    return message.warning('合并节点至少选择两个上游节点');
  }
  if (node.sourceMetadata.includeNaturalKey && !node.sourceMetadata.naturalKeyColumns.length) {
    return message.warning('启用来源自然键后请选择自然键字段');
  }
  if (node.schemaMonitoring?.enabled && !String(node.schemaMonitoring?.taskId || '').trim()) {
    return message.warning('已启用元数据探查，请选择元数据探查任务');
  }
  if (node.output.qualityConfig?.enabled && !String(node.output.qualityConfig?.taskId || '').trim()) {
    return message.warning('已启用质量检查，请选择质量检查任务');
  }
  if (node.sourceMetadata?.includeSourceUpdateTime
    && !String(node.sourceMetadata?.sourceUpdateTimeSourceColumn || '').trim()) {
    return message.warning('已启用源端更新时间，请选择源更新时间字段');
  }
  if (node.sourceMetadata?.includeRecordOp
    && !String(node.sourceMetadata?.recordOpSourceColumn || '').trim()) {
    return message.warning('已启用操作类型，请选择源端操作字段');
  }
  const firstInput = node.inputs?.[0] || {};
  node.input = {
    readerDatasource: node.readerDatasource,
    table_name: firstInput.table || '',
    columns: node.columns || [],
    tableFields: node.tableFields || [],
    sourceMetadata: node.sourceMetadata,
    schemaMonitoring: node.schemaMonitoring,
  };
  node.output = {
    ...node.output,
    target_table_name: node.output.table,
    writerDatasource: node.output.writerDatasource,
    columns: node.columns || [],
    target_columns: node.targetColumns || [],
    qualityConfig: node.output.qualityConfig,
  };
  emit('confirm', form.value);
}
function closeDialog() { emit('update', false); }
</script>
