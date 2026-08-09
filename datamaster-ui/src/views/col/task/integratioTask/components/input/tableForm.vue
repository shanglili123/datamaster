<template>
    <a-modal v-model:open="visibleDialog" class="medium-dialog" :title="currentNode?.data?.name"
        :closable="false" :destroy-on-close="true" :mask-closable="false">
        <a-spin :spinning="loading">
        <a-form ref="dpModelRefs" :model="form" :label-col="{ style: { width: '110px' } }" @submit.prevent
            :disabled="info">

            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="节点名称" name="name"
                        :rules="[{ required: true, message: '请输入节点名称', trigger: 'change' }]">
                        <a-input v-if="!info" v-model:value="form.name" placeholder="请输入节点名称" />
                        <div v-else class="form-readonly">{{ form.name }}</div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="类型" name="typeName">
                        <a-input v-if="!info" v-model:value="form.taskParams.typeName" placeholder="请输入类型" disabled />
                        <div v-else class="form-readonly">{{ form.taskParams.typeName }}</div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="描述" name="description">
                        <a-input v-if="!info" v-model:value="form.description" type="textarea" placeholder="请输入描述" />
                        <div v-else class="form-readonly">{{ form.description || '-' }}</div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="连接方式" name="clmt">
                        <a-radio-group v-if="!info" @change="(e) => handleReleaseStateChange(e.target.value)"
                            v-model:value="form.taskParams.clmt">
                            <template v-for="dict in col_connection" :key="dict.value">
                                <a-radio :value="dict.value">
                                    {{ dict.label }}
                                </a-radio>
                            </template>
                        </a-radio-group>
                        <div class="form-readonly" v-else>{{col_connection.find((item) => item.value ==
                            form.taskParams.clmt)?.label || '-'}}</div>
                    </a-form-item>
                </a-col>
                <template v-if="form.taskParams.clmt == '1'">
                    <a-col :span="12">
                        <a-form-item label="资产表" name="taskParams.asset_id_cpoy" :rules="[
                            { required: true, message: '请选择资产表', trigger: 'blur' }
                        ]">
                            <a-select v-if="!info" v-model:value="form.taskParams.asset_id_cpoy" show-search
                                @change="handleAssetTableChange" :loading="dppLoading">
                                <a-select-option v-for="item in dppNoPageListList" :key="item.id" :label="item.name"
                                    :value="item.id" />
                            </a-select>
                            <div class="form-readonly" v-else>{{dppNoPageListList.find((item) => item.id ==
                                form.taskParams.asset_id_cpoy)?.name || '-'}}</div>
                        </a-form-item>
                    </a-col>
                </template>
            </a-row>
            <!-- -->
            <template v-if="form.taskParams.clmt == '0' || form.taskParams.clmt == '2'">
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="源数据库连接" name="taskParams.readerDatasource.datasourceId" :rules="[
                            {
                                required: true,
                                message: '请选择源数据库连接',
                                trigger: 'change'
                            }
                        ]">
<a-select v-if="!info" :key="'ds-' + createTypeList.length"
    v-model:value="form.taskParams.readerDatasource.datasourceId" placeholder="请选择源数据库连接"
    @change="handleDatasourceChange" show-search>
    <a-select-option v-for="dict in createTypeList" :key="dict.id" :label="dict.datasourceName"
        :value="String(dict.id)">{{ dict.datasourceName }}</a-select-option>
</a-select>
                            <div class="form-readonly" v-else>{{createTypeList.find((item) => item.id ==
                                form.taskParams.readerDatasource.datasourceId)?.datasourceName || '-'}}</div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="数据连接类型" name="taskParams.readerDatasource.datasourceType">
                            <a-input v-if="!info" v-model:value="form.taskParams.readerDatasource.datasourceType"
                                placeholder="请输入数据连接类型" disabled />
                            <div class="form-readonly" v-else>{{ form.taskParams.readerDatasource.datasourceType || '-'
                                }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="数据连接实例" name="taskParams.readerDatasource.dbname">
                            <a-input v-if="!info" v-model:value="form.taskParams.readerDatasource.dbname"
                                placeholder="请输入数据连接实例" disabled />
                            <div class="form-readonly" v-else>{{ form.taskParams.readerDatasource.dbname || '-' }}</div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12" v-if="form.taskParams.clmt == '0' && !isKafkaReader">
                        <a-form-item label="选择表" name="taskParams.asset_id"
                            :rules="[{ required: true, message: '请选择表', trigger: 'change' }]">
                            <a-select v-if="!info" v-model:value="form.taskParams.asset_id" show-search @change="handleChange"
                                :loading="loadingTables">
                                <a-select-option v-for="item in TablesByDataSource" :key="item.tableName"
                                    :label="item.tableName" :value="item.tableName" />
                            </a-select>
                            <div class="form-readonly" v-else>{{ form.taskParams.asset_id }}</div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12" v-if="form.taskParams.clmt == '2'">
                        <a-form-item>
                            <div style="text-align: right; width: 100%">
                                <a-button size="small" type="primary" @click="sqlParseFunction"
                                    class="sql-parse-btn">SQL解析</a-button>
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20" v-if="form.taskParams.clmt == '2'">
                    <a-col :span="24">
                        <a-form-item label="SQL语句" name="taskParams.querySql"
                            :rules="[{ required: true, message: '请输入SQL语句', trigger: 'blur' }]">
                            <sql-editor ref="editorRef" :value="form.taskParams.querySql" class="sql-editor"
                                :height="'140px'" @changeTextarea="changeTextarea($event)" />
                        </a-form-item>
                    </a-col>
                </a-row>
            </template>
            <a-row :gutter="20" v-if="form.taskParams.clmt != '2'">
                <a-col :span="12">
                    <a-form-item label="读取模式" name="taskParams.readModeType" :rules="[
                        {
                            required: true,
                            message: '请选择读取模式',
                            trigger: 'change'
                        }
                    ]">
                        <a-radio-group v-if="!info" v-model:value="form.taskParams.readModeType"
                            @change="(e) => handlereadModeTypeChange(e.target.value)">
                            <a-radio value="1">全量</a-radio>
                            <a-radio value="2">id增量</a-radio>
                            <a-radio value="3">时间增量</a-radio>
                            <a-radio value="4" :disabled="!isStreamReader">CDC/流式</a-radio>
                        </a-radio-group>
                        <div class="form-readonly" v-else>{{ form.taskParams.readModeType == 1 ? '全量' :
                            form.taskParams.readModeType == 2 ?
                                'id增量' : form.taskParams.readModeType == 3 ? '时间增量' : 'CDC/流式' }}</div>
                    </a-form-item>
                </a-col>
            </a-row>
            <template v-if="form.taskParams.clmt != '2' && form.taskParams.readModeType == 4">
                <a-row :gutter="20" v-if="isKafkaReader">
                    <a-col :span="12">
                        <a-form-item label="Topic" name="taskParams.topic">
                            <a-input v-if="!info" v-model:value="form.taskParams.topic" placeholder="请输入 Topic" />
                            <div class="form-readonly" v-else>{{ form.taskParams.topic || '-' }}</div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="消费组" name="taskParams.kafkaConfig.groupId">
                            <a-input v-if="!info" v-model:value="form.taskParams.kafkaConfig.groupId"
                                placeholder="默认 datamaster-chunjun" />
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaConfig.groupId || '-' }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20" v-if="isKafkaReader">
                    <a-col :span="12">
                        <a-form-item label="启动位置" name="taskParams.kafkaConfig.mode">
                            <a-select v-if="!info" v-model:value="form.taskParams.kafkaConfig.mode">
                                <a-select-option label="最新" value="LATEST" />
                                <a-select-option label="最早" value="EARLIEST" />
                                <a-select-option label="消费组位点" value="GROUP_OFFSETS" />
                                <a-select-option label="指定时间" value="TIMESTAMP" />
                                <a-select-option label="指定位点" value="SPECIFIC_OFFSETS" />
                            </a-select>
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaConfig.mode || '-' }}</div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="消息格式" name="taskParams.kafkaConfig.codec">
                            <a-select v-if="!info" v-model:value="form.taskParams.kafkaConfig.codec" placeholder="请选择消息格式">
                                <a-select-option label="json - JSON 格式解析" value="json" />
                                <a-select-option label="text - 纯文本格式" value="text" />
                                <a-select-option label="csv - CSV 格式" value="csv" />
                                <a-select-option label="protobuf - Protobuf 二进制格式" value="protobuf" />
                            </a-select>
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaConfig.codec || '-' }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20" v-if="isKafkaReader">
                    <a-col :span="12">
                        <a-form-item label="是否平铺" name="taskParams.kafkaConfig.pavingData">
                            <a-switch v-if="!info" v-model:checked="form.taskParams.kafkaConfig.pavingData" />
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaConfig.pavingData === false ? '否' : '是' }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20" v-if="isKafkaReader">
                    <a-col :span="24">
                        <a-form-item label="Kafka扩展配置" name="taskParams.kafkaConfigJson">
                            <a-input v-if="!info" v-model:value="form.taskParams.kafkaConfigJson" type="textarea"
                                placeholder='可选 JSON，例如 {"consumerSettings":{"security.protocol":"SASL_PLAINTEXT"}}' />
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaConfigJson || '-' }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20" v-if="!isKafkaReader && !isStreamingMqReader">
                    <a-col :span="12">
                        <a-form-item label="Server ID" name="taskParams.cdcConfig.serverId">
                            <a-input v-if="!info" v-model:value="form.taskParams.cdcConfig.serverId"
                                placeholder="MySQL CDC 可空，默认按数据源生成" />
                            <div class="form-readonly" v-else>{{ form.taskParams.cdcConfig.serverId || '-' }}</div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="时间格式" name="taskParams.cdcConfig.timestampFormat">
                            <a-input v-if="!info" v-model:value="form.taskParams.cdcConfig.timestampFormat"
                                placeholder="yyyy-MM-dd HH:mm:ss" />
                            <div class="form-readonly" v-else>{{ form.taskParams.cdcConfig.timestampFormat || '-' }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20" v-if="!isKafkaReader && !isStreamingMqReader">
                    <a-col :span="12">
                        <a-form-item label="是否平铺" name="taskParams.cdcConfig.pavingData">
                            <a-switch v-if="!info" v-model:checked="form.taskParams.cdcConfig.pavingData" />
                            <div class="form-readonly" v-else>{{ form.taskParams.cdcConfig.pavingData === false ? '否' : '是' }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20" v-if="!isKafkaReader && !isStreamingMqReader">
                    <a-col :span="24">
                        <a-form-item label="CDC扩展配置" name="taskParams.cdcConfigJson">
                            <a-input v-if="!info" v-model:value="form.taskParams.cdcConfigJson" type="textarea"
                                placeholder='可选 JSON，例如 {"readPosition":"current","startScn":"123"}' />
                            <div class="form-readonly" v-else>{{ form.taskParams.cdcConfigJson || '-' }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
            </template>
            <template v-if="form.taskParams.readModeType == 2">
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="id字段" name="taskParams.idIncrementConfig.incrementColumn" :rules="[
                            {
                                required: true,
                                message: '请选择id字段',
                                trigger: 'blur'
                            }
                        ]">
                            <a-select v-if="!info" v-model:value="form.taskParams.idIncrementConfig.incrementColumn"
                                show-search placeholder="请选择id字段">
                                <a-select-option v-for="item in ColumnByAssettab" :key="item.columnName"
                                    :label="item.columnName" :value="item.columnName" />
                            </a-select>
                            <div class="form-readonly" v-else>{{ form.taskParams.idIncrementConfig.incrementColumn ||
                                '-' }}</div>
                        </a-form-item>
                    </a-col>

                    <a-col :span="12">
                        <a-form-item label="开始值" name="taskParams.idIncrementConfig.incrementStart" :rules="[
                            { required: true, message: '请输入开始值', trigger: 'change' },
                            { validator: checkInteger, trigger: 'change' }
                        ]">
                            <a-input v-if="!info" v-model:value="form.taskParams.idIncrementConfig.incrementStart"
                                placeholder="请输入开始值" type="number">
                            </a-input>
                            <div class="form-readonly" v-else>{{ form.taskParams.idIncrementConfig.incrementStart || '-'
                                }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
            </template>
            <template v-if="form.taskParams.readModeType == 3">
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="时间格式" name="taskParams.dateIncrementConfig.dateFormat">
                            <a-select v-if="!info" v-model:value="form.taskParams.dateIncrementConfig.dateFormat"
                                placeholder="请选择时间格式">
                                <a-select-option v-for="item in dateFormatOptions" :key="item.value" :label="item.label"
                                    :value="item.value" />
                            </a-select>
                            <div class="form-readonly" v-else>{{dateFormatOptions.find(item => item.value ==
                                form.taskParams.dateIncrementConfig.dateFormat)?.label || '-'}}</div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="初始游标">
                            <a-date-picker v-if="!info" allow-clear v-model:value="dateIncrementCursorTime"
                                :picker="dateIncrementPickerType" :format="dateIncrementPickerFormat"
                                :value-format="dateIncrementPickerFormat" placeholder="请选择首次同步的起始时间" />
                            <div class="form-readonly" v-else>{{ dateIncrementCursorTime || '-' }}</div>
                        </a-form-item>
                    </a-col>
                </a-row>
            </template>
            <a-row :gutter="20" v-if="form.taskParams.clmt != '2' && form.taskParams.readModeType != 4">
                <a-col :span="24">
                    <a-form-item label="where条件" name="where">
                        <a-input v-if="!info" v-model:value="form.taskParams.where" type="textarea"
                            :placeholder="'例如 id > 10 and id < 1000，请不要以分号;结尾'" />
                        <div class="form-readonly" v-else>{{ form.taskParams.where || '-' }}</div>
                    </a-form-item>
                </a-col>
            </a-row>
            <template v-if="form.taskParams.clmt != '2'">
                <div class="h2-title stream-field-title">
                    <span>属性字段</span>
                    <a-button v-if="isStreamingFieldEditable && !info" size="small" type="primary"
                        @click="addStreamField">添加一行</a-button>
                </div>
                <a-table :loading="loadingList" :data-source="ColumnByAssettab" :columns="tableColumns"
                    :pagination="false" :scroll="{ y: 310 }">
                    <template #bodyCell="{ column, record, index }">
                        <template v-if="column.dataIndex === 'index'">
                            <span>{{ index + 1 }}</span>
                        </template>
                        <template v-else-if="column.dataIndex === 'columnName'">
                            <a-input v-if="isStreamingFieldEditable && !info" v-model:value="record.columnName"
                                placeholder="字段名，如 id" />
                            <span v-else>{{ record.columnName || '-' }}</span>
                        </template>
                        <template v-else-if="column.dataIndex === 'columnComment'">
                            {{ record.columnComment || '-' }}
                        </template>
                        <template v-else-if="column.dataIndex === 'columnType'">
                            <a-select v-if="isStreamingFieldEditable && !info" v-model:value="record.columnType"
                                show-search placeholder="选择类型">
                                <a-select-option v-for="item in streamFieldTypeOptions" :key="item" :label="item"
                                    :value="item" />
                            </a-select>
                            <span v-else>{{ record.columnType || '-' }}</span>
                        </template>
                        <template v-else-if="column.dataIndex === 'isKey'">
                            <a-switch v-if="!info" v-model:checked="record.isKey" />
                            <span v-else>{{ record.isKey ? '是' : '否' }}</span>
                        </template>
                        <template v-else-if="column.dataIndex === 'pkFlag'">
                            <a-switch v-model:checked="record.pkFlag" :checked-value="'1'" :unchecked-value="'0'" disabled />
                        </template>
                        <template v-else-if="column.dataIndex === 'columnLength'">
                            {{ record.columnLength || '-' }}
                        </template>
                        <template v-else-if="column.dataIndex === 'columnScale'">
                            {{ record.columnScale || '-' }}
                        </template>
                        <template v-else-if="column.key === 'increment'">
                            <template v-if="info">
                                <a-tag v-if="form.taskParams.readModeType == 2
                                    && form.taskParams.idIncrementConfig.incrementColumn == record.columnName"
                                    color="success">增量字段</a-tag>
                                <a-tag v-else-if="form.taskParams.readModeType == 3
                                    && hasDateIncrementConfig(record.columnName)" color="success">增量字段</a-tag>
                                <span v-else>-</span>
                            </template>
                            <template v-else-if="form.taskParams.readModeType == 2">
                                <a-tag v-if="form.taskParams.idIncrementConfig.incrementColumn == record.columnName"
                                    color="success">已设置</a-tag>
                                <a-button v-else type="link" size="small"
                                    @click="setIdIncrementColumn(record)">设为增量字段</a-button>
                            </template>
                            <template v-else-if="form.taskParams.readModeType == 3">
                                <a-checkbox :checked="hasDateIncrementConfig(record.columnName)"
                                    :disabled="!isTimeColumn(record)" @change="(e) => setDateIncrementColumn(record, e.target.checked)">
                                    {{ isTimeColumn(record) ? '选择' : '非时间字段' }}
                                </a-checkbox>
                            </template>
                        </template>
                        <template v-else-if="column.key === 'actions'">
                            <a-button type="link" danger size="small" @click="removeStreamField(index)">删除</a-button>
                        </template>
                    </template>
                </a-table>
            </template>

        </a-form>
        </a-spin>
        <template #footer>
            <div style="text-align: right">
                <a-button @click="closeDialog">关闭</a-button>
                <a-button type="primary" @click="saveData" v-if="!info">保存</a-button>
            </div>
        </template>
    </a-modal>
</template>
<script setup>
import { message } from 'ant-design-vue'
import SqlEditor from '@/components/SqlEditor/index1.vue';

import {
    getTablesByDataSourceId,
    getColumnByAssetId,
    getLocalNodeUniqueKey as getNodeUniqueKey
} from '@/api/col/task/index.js';

import {
    listDaDatasource,
    getDaDatasource,
    sqlParse
} from '@/api/ast/dataSource/dataSource.js';

import { listDppAsset } from '@/api/ast/asset/asset.js';
const { proxy } = getCurrentInstance();

import useUserStore from '@/store/system/user.js';
const userStore = useUserStore();
const { col_connection } = proxy.useDict('col_connection');
const props = defineProps({
    visible: { type: Boolean, default: true },
    title: { type: String, default: '表单标题' },
    currentNode: { type: Object, default: () => ({}) },
    info: { type: Boolean, default: false },
    // 新增回echo支持
    savedDataSourceId: {
        type: [String, Number],
        default: ''
    },
    savedAssetTableId: {
        type: [String, Number],
        default: ''
    }
});

const emit = defineEmits(['update', 'confirm', '回echo完成']);
const visibleDialog = computed({
    get() {
        return props.visible;
    },
    set(newValue) {
        emit('update', newValue);
    }
});

// 变量定义
let loading = ref(false);
let loadingList = ref(false);
let TablesByDataSource = ref([]);
let ColumnByAssettab = ref([]);
let dpModelRefs = ref();
let form = ref({});
const tableFields = ref([]); // 来源表格
const createTypeList = ref([]); // 数据源列表

const dateFormatOptions = [
    { label: 'yyyy-MM-dd', value: 'yyyy-MM-dd' },
    { label: 'yyyy-MM-dd HH:mm:ss', value: 'yyyy-MM-dd HH:mm:ss' },
    { label: 'yyyy-MM-dd HH:mm:ss.SSS', value: 'yyyy-MM-dd HH:mm:ss.SSS' },
    { label: 'yyyy-MM-dd HH:mm:ss.SSSSSS', value: 'yyyy-MM-dd HH:mm:ss.SSSSSS' },
]
const streamFieldTypeOptions = [
    'STRING',
    'INT',
    'BIGINT',
    'DOUBLE',
    'DECIMAL',
    'BOOLEAN',
    'DATE',
    'TIMESTAMP'
];
const dateIncrementCursorDraft = ref('');
const dateIncrementConfig = computed(() => form.value?.taskParams?.dateIncrementConfig);
const dateIncrementColumnConfig = computed(() => dateIncrementConfig.value?.column?.[0]);
const dateIncrementPickerFormat = computed(() => {
    return (dateIncrementConfig.value?.dateFormat || 'yyyy-MM-dd')
        .replace(/SSSSSS/g, 'SSS')
        .replace(/yyyy/g, 'YYYY')
        .replace(/dd/g, 'DD');
});
const dateIncrementPickerType = computed(() => {
    return dateIncrementConfig.value?.dateFormat?.includes('HH') ? 'datetime' : 'date';
});
const dateIncrementCursorTime = computed({
    get() {
        return dateIncrementColumnConfig.value?.cursorTime || dateIncrementCursorDraft.value;
    },
    set(value) {
        if (dateIncrementColumnConfig.value) {
            dateIncrementColumnConfig.value.cursorTime = value;
        } else {
            dateIncrementCursorDraft.value = value;
        }
    }
});
const getDatasourceId = (ds) => ds?.datasourceId ?? ds?.datasource_id ?? ds?.id ?? ds?.sourceId;
const normalizeDatasourceType = (type) => String(type || '').replace(/[\s_-]/g, '').toLowerCase();
const getCurrentReaderDatasource = () => {
    const readerDatasource = form.value?.taskParams?.readerDatasource || {};
    const datasourceId = getDatasourceId(readerDatasource);
    const selectedDatasource = createTypeList.value.find(item => String(item.id) === String(datasourceId));
    return {
        ...selectedDatasource,
        ...readerDatasource,
        datasourceType: readerDatasource.datasourceType || selectedDatasource?.datasourceType || ''
    };
};
const isKafkaReader = computed(() => normalizeDatasourceType(getCurrentReaderDatasource().datasourceType) === 'kafka');
const cdcDatasourceTypes = ['mysql', 'oracle', 'oracle11', 'sqlserver', 'sqlserver2008', 'postgresql', 'postgres'];
const streamingQueueDatasourceTypes = ['rabbitmq', 'redis', 'rocketmq'];
const isCdcReader = computed(() => {
    const datasourceType = normalizeDatasourceType(getCurrentReaderDatasource().datasourceType);
    return cdcDatasourceTypes.includes(datasourceType);
});
const isStreamingMqReader = computed(() => {
    const datasourceType = normalizeDatasourceType(getCurrentReaderDatasource().datasourceType);
    return streamingQueueDatasourceTypes.includes(datasourceType);
});
const isStreamReader = computed(() => isKafkaReader.value || isStreamingMqReader.value || isCdcReader.value);
const isStreamingFieldEditable = computed(() => form.value?.taskParams?.readModeType == '4' && isStreamReader.value);

const tableColumns = computed(() => {
    const cols = [
        { title: '序号', dataIndex: 'index', width: 80, align: 'left' },
        { title: '英文名称', dataIndex: 'columnName', align: 'left', ellipsis: true },
    ];
    if (!isStreamingFieldEditable.value) {
        cols.push({ title: '中文名称', dataIndex: 'columnComment', align: 'left', ellipsis: true });
    }
    cols.push({ title: '字段类型', dataIndex: 'columnType', align: 'left' });
    if (isStreamingFieldEditable.value) {
        cols.push({ title: '是否Key', dataIndex: 'isKey', align: 'left' });
    }
    if (form.value?.taskParams?.type == '1' && !isStreamingFieldEditable.value) {
        cols.push({ title: '是否主键', dataIndex: 'pkFlag', align: 'left' });
        cols.push({ title: '字段长度', dataIndex: 'columnLength', align: 'left' });
        cols.push({ title: '小数经度', dataIndex: 'columnScale', align: 'left' });
    }
    if (form.value.taskParams.readModeType != 1 && form.value.taskParams.readModeType != 4) {
        cols.push({ title: '增量配置', key: 'increment', align: 'center', fixed: 'right', width: 150 });
    }
    if (isStreamingFieldEditable.value && !props.info) {
        cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 90 });
    }
    return cols;
});

const getNodeData = () => props.currentNode?.getProp?.("data") || props.currentNode?.data || {};

const normalizeReaderDatasource = (taskParams) => {
    if (!taskParams) {
        return;
    }
    if (!taskParams.readerDatasource || typeof taskParams.readerDatasource !== 'object') {
        taskParams.readerDatasource = { datasourceId: '', datasourceType: '', dbname: '' };
        return;
    }

    const datasourceId = getDatasourceId(taskParams.readerDatasource);
    if (datasourceId !== undefined && datasourceId !== null && datasourceId !== '') {
        taskParams.readerDatasource.datasourceId = String(datasourceId);
        taskParams.readerDatasource.datasource_id = String(datasourceId);
    }
};

const ensureSelectedDatasourceOption = () => {
    const nodeData = getNodeData();
    const ds = form.value?.taskParams?.readerDatasource || nodeData?.taskParams?.readerDatasource;
    const savedId = getDatasourceId(ds);
    if (savedId === undefined || savedId === null || savedId === '') {
        return;
    }

    const datasourceId = String(savedId);
    const exists = createTypeList.value.some(item => String(item.id) === datasourceId);
    if (exists) {
        return;
    }

    const originalDs = nodeData?.taskParams?.readerDatasource || {};
    createTypeList.value.unshift({
        id: datasourceId,
        datasourceName: ds.datasourceName || originalDs.datasourceName || `数据源 ${datasourceId}`,
        datasourceType: ds.datasourceType || originalDs.datasourceType || '',
        ip: ds.ip || originalDs.ip || '',
        port: ds.port || originalDs.port || '',
        datasourceConfig: ds.datasourceConfig || originalDs.datasourceConfig || '{}'
    });
};

// 回echo处理函数
const handle回echo = () => {
    // 处理数据源回echo
    if (props.savedDataSourceId && props.savedDataSourceId !== '') {
        const savedId = String(props.savedDataSourceId);
        const existingDatasource = createTypeList.value.find(item => String(item.id) === savedId);
        
        if (!existingDatasource) {
            // 构建回echo的数据源信息
            const 回echo数据源信息 = {
                id: savedId,
                datasourceName: props.currentNode?.data?.taskParams?.readerDatasource?.datasourceName || '回echo数据源',
                datasourceType: props.currentNode?.data?.taskParams?.readerDatasource?.datasourceType || '',
                ip: props.currentNode?.data?.taskParams?.readerDatasource?.ip || '',
                port: props.currentNode?.data?.taskParams?.readerDatasource?.port || '',
                datasourceConfig: props.currentNode?.data?.taskParams?.readerDatasource?.datasourceConfig || '{}'
            };
            
            // 添加到列表
            createTypeList.value.unshift(回echo数据源信息);
            console.log('回echo: tableForm回echo数据源，ID:', savedId, '名称:', 回echo数据源信息.datasourceName);
        }
    }

    // 处理资产表回echo
    if (props.savedAssetTableId && props.savedAssetTableId !== '') {
        const savedAssetId = String(props.savedAssetTableId);
        const existingAsset = dppNoPageListList.value.find(item => item.id === savedAssetId);
        
        if (!existingAsset) {
            // 构建回echo的资产表信息
            const 回echo资产表信息 = {
                id: savedAssetId,
                name: props.currentNode?.data?.taskParams?.asset_id_cpoy?.name || '回echo资产表',
                datasourceId: props.currentNode?.data?.taskParams?.datasource_id_cpoy || '',
                tableName: props.currentNode?.data?.taskParams?.asset_id_cpoy || savedAssetId
            };
            
            // 添加到列表
            dppNoPageListList.value.unshift(回echo资产表信息);
            console.log('回echo: tableForm回echo资产表，ID:', savedAssetId, '名称:', 回echo资产表信息.name);
        }
    }

    // 通知父组件回echo完成
    emit('回echo完成', {
        dataSourceId: props.savedDataSourceId,
        assetTableId: props.savedAssetTableId
    });
};

const handlereadModeTypeChange = (val) => {
    if (val == '4' && !isStreamReader.value) {
        form.value.taskParams.readModeType = '1';
        proxy.$message.warning('当前数据源暂不支持 CDC/流式读取');
        return;
    }
    form.value.taskParams.idIncrementConfig = {
        incrementColumn: "", // 增量字段
        incrementStart: "", // 开始值
    };
    form.value.taskParams.dateIncrementConfig = {
        logic: "and",
        dateFormat: "yyyy-MM-dd",
        column: [],
    }
    ensureStreamConfigs();
    dateIncrementCursorDraft.value = '';
}

const checkInteger = (rule, value, callback) => {
    if (value === '' || value === null || value === undefined) {
        callback(new Error('请输入开始值'))
        return
    }
    const numValue = Number(value)
    if (isNaN(numValue)) {
        callback(new Error('请输入有效的数字'))
        return
    }
    if (!Number.isInteger(numValue)) {
        callback(new Error('开始值必须为整数'))
        return
    }

    callback()
}
// 获取数据源列表
const getDatasourceList = async () => {
    try {
        loading.value = true;
        const response = await listDaDatasource({
            pageSize: 9999,
            spaceCode: userStore.spaceCode,
            spaceId: userStore.spaceId,
            datasourceType: "DM8,Oracle11,MySql,Oracle,Kingbase8,Doris,ClickHouse,Hive,MongoDB,Elasticsearch,SQL_Server,SQL_Server2008,PostgreSQL,Kafka",
        });
        createTypeList.value = response.data.rows || [];
        ensureSelectedDatasourceOption();
    } finally {
        loading.value = false;
    }
};
let loadingTables = ref(false);
// 获取表列表
const getTablesByDatasourceId = async (id) => {
    TablesByDataSource.value = await fetchData(
        getTablesByDataSourceId,
        { datasourceId: id },
        loadingTables
    );
};
// 获取列数据
const getColumnByAssetIdList = async (id, data) => {
    const dsId = form.value?.taskParams?.readerDatasource?.datasourceId;
    if (!dsId) return;
    ColumnByAssettab.value = await fetchData(
        getColumnByAssetId,
        {
            withRule: 2,
            id: dsId,
            tableName: form.value.taskParams.asset_id
        },
        loadingList
    );
    form.value.taskParams.dateIncrementConfig.column = [];
    dateIncrementCursorDraft.value = '';
    form.value.taskParams.idIncrementConfig.incrementColumn = null;
    form.value.taskParams.inputFields = ColumnByAssettab.value;
};
// 通用的获取数据的函数
const fetchData = async (requestFn, params, loadingState) => {
    try {
        loadingState.value = true;
        const response = await requestFn(params);
        return response.data;
    } finally {
        loadingState.value = false;
    }
};

const resetReaderSelection = () => {
    form.value.taskParams.asset_id = '';
    form.value.taskParams.table_name = '';
    form.value.taskParams.querySql = '';
    form.value.taskParams.inputFields = [];
    form.value.taskParams.tableFields = [];
    form.value.taskParams.columns = [];
    form.value.taskParams.columnsList = [];
    form.value.taskParams.topic = '';
    form.value.taskParams.kafkaFieldsJson = '';
    form.value.taskParams.kafkaConfigJson = '';
    form.value.taskParams.cdcConfigJson = '';
    form.value.taskParams.kafkaConfig = {
        groupId: 'datamaster-chunjun',
        mode: 'LATEST',
        codec: 'json',
        pavingData: true,
    };
    form.value.taskParams.cdcConfig = {
        serverId: '',
        timestampFormat: 'yyyy-MM-dd HH:mm:ss',
        pavingData: true,
    };
};

// 处理数据源变化
const resetAndFetchTables = async (selectedDatasource) => {
    TablesByDataSource.value = [];
    ColumnByAssettab.value = [];
    let { datasourceType, datasourceConfig, ip, port, id } = selectedDatasource;
    let code = JSON.parse(datasourceConfig);
    form.value.taskParams.readerDatasource = {
        datasourceType,
        datasourceConfig,
        ip,
        port,
        dbname: code.dbname,
        datasource_id: String(id),
            datasourceId: String(id),
            datasourceName: selectedDatasource.datasourceName
    };
    resetReaderSelection();
    ensureStreamConfigs();
    form.value.taskParams.dateIncrementConfig.column = [];
    dateIncrementCursorDraft.value = '';
    form.value.taskParams.idIncrementConfig.incrementColumn = null;

    const normalizedDatasourceType = normalizeDatasourceType(datasourceType);
    if (normalizedDatasourceType === 'kafka' || streamingQueueDatasourceTypes.includes(normalizedDatasourceType)) {
        if (form.value.taskParams.clmt == '2') {
            form.value.taskParams.clmt = '0';
        }
        form.value.taskParams.readModeType = '4';
        TablesByDataSource.value = [];
    } else {
        if (form.value.taskParams.readModeType == '4' && !cdcDatasourceTypes.includes(normalizedDatasourceType)) {
            form.value.taskParams.readModeType = '1';
        }
        await getTablesByDatasourceId(id);
    }
};

// 处理数据源变化
const handleDatasourceChange = (value) => {
    const selectedDatasource = createTypeList.value.find((item) => item.id == value);
    if (selectedDatasource) {
        resetAndFetchTables(selectedDatasource);
    }
};

// 处理表变化
const setTableName = (selectedDatasource) => {
    form.value.taskParams.table_name = selectedDatasource.tableName;
};
const setIdIncrementColumn = (field) => {
    form.value.taskParams.idIncrementConfig.incrementColumn = field.columnName;
};
const hasDateIncrementConfig = (columnName) => {
    return dateIncrementColumnConfig.value?.incrementColumn === columnName;
};
const isTimeColumn = (field) => {
    const columnType = String(field?.columnType || '').toUpperCase();
    return columnType.includes('DATE')
        || columnType.startsWith('TIMESTAMP')
        || columnType === 'TIME'
        || columnType === 'YEAR';
};
const inferDateFormat = (field) => {
    const columnType = String(field?.columnType || '').toUpperCase();
    return columnType === 'DATE' || columnType === 'YEAR' ? 'yyyy-MM-dd' : 'yyyy-MM-dd HH:mm:ss.SSSSSS';
};
const setDateIncrementColumn = (field, checked) => {
    if (!checked) {
        form.value.taskParams.dateIncrementConfig.column = [];
        dateIncrementCursorDraft.value = '';
        return;
    }
    const cursorTime = dateIncrementCursorTime.value;
    form.value.taskParams.dateIncrementConfig.dateFormat = inferDateFormat(field);
    form.value.taskParams.dateIncrementConfig.column = [{
        type: '2',
        incrementColumn: field.columnName,
        operator: '>',
        cursorTime
    }];
    dateIncrementCursorDraft.value = '';
};
const plainObject = (value) => {
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {};
};
const parsePlainJson = (raw, label) => {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        throw new Error(`${label}必须是 JSON 对象`);
    }
    return parsed;
};
const ensureStreamConfigs = () => {
    form.value.taskParams = form.value.taskParams || {};
    const taskParams = form.value.taskParams;
    taskParams.cdcConfig = {
        serverId: '',
        timestampFormat: 'yyyy-MM-dd HH:mm:ss',
        pavingData: true,
        ...plainObject(taskParams.cdcConfig),
    };
    taskParams.kafkaConfig = {
        groupId: 'datamaster-chunjun',
        mode: 'LATEST',
        codec: 'json',
        pavingData: true,
        split: false,
        ...plainObject(taskParams.kafkaConfig),
    };
    taskParams.kafkaFieldsJson = taskParams.kafkaFieldsJson || '';
    taskParams.streamFields = normalizeStreamFields(taskParams.streamFields);
    if (!taskParams.streamFields.length && taskParams.kafkaFieldsJson) {
        taskParams.streamFields = parseKafkaFieldsFromJson(taskParams.kafkaFieldsJson);
    }
    if (!taskParams.streamFields.length && taskParams.kafkaConfigJson) {
        const legacyFields = parseKafkaFieldsFromJson(taskParams.kafkaConfigJson);
        if (legacyFields.length) {
            taskParams.streamFields = legacyFields;
            taskParams.kafkaConfigJson = stripKafkaSchemaFromJson(taskParams.kafkaConfigJson);
        }
    }
    taskParams.kafkaConfigJson = taskParams.kafkaConfigJson || '';
    if (!taskParams.streamFields.length) {
        const fields = taskParams.kafkaConfig.tableFields || taskParams.tableFields;
        if (Array.isArray(fields) && fields.length) {
            taskParams.streamFields = normalizeStreamFields(fields);
        }
    }
};

const mergeCdcConfigJson = () => {
    const raw = form.value.taskParams.cdcConfigJson;
    if (!raw) return true;
    try {
        const parsed = parsePlainJson(raw, 'CDC扩展配置');
        form.value.taskParams.cdcConfig = {
            ...plainObject(form.value.taskParams.cdcConfig),
            ...parsed,
        };
        return true;
    } catch (error) {
        proxy.$message.warning('CDC扩展配置不是合法 JSON 对象');
        return false;
    }
};
const normalizeStreamFields = (fields) => {
    if (!Array.isArray(fields)) return [];
    return fields
        .map((field, index) => {
            if (typeof field === 'string') {
                return {
                    id: index + 1,
                    columnName: field,
                    columnType: 'STRING',
                    isKey: false,
                    key: '',
                    columnComment: '',
                };
            }
            if (!field || typeof field !== 'object') return null;
            const columnName = field.columnName || field.name || field.key;
            if (!columnName) return null;
            const isKey = field.isKey === true
                || field.keyFlag === true
                || field.primaryKey === true
                || field.pkFlag === '1'
                || field.pkFlag === 1;
            const normalizedField = {
                id: field.id || index + 1,
                columnName,
                columnType: String(field.columnType || field.type || 'STRING').toUpperCase(),
                isKey,
                key: isKey ? columnName : '',
                pkFlag: isKey ? '1' : '0',
                columnComment: field.columnComment || '',
            };
            ['index', 'value', 'format', 'parseFormat', 'splitter', 'isPart', 'notNull'].forEach((key) => {
                if (field[key] !== undefined && field[key] !== null && field[key] !== '') {
                    normalizedField[key] = field[key];
                }
            });
            return normalizedField;
        })
        .filter(Boolean);
};
const parseKafkaFieldsFromJson = (raw) => {
    if (!raw) return [];
    try {
        const parsed = JSON.parse(raw);
        return normalizeStreamFields(Array.isArray(parsed) ? parsed : getKafkaFieldSource(parsed));
    } catch (error) {
        return [];
    }
};
const getKafkaFieldSource = (parsed) => {
    if (!parsed || typeof parsed !== 'object') return [];
    if (parsed.tableFields || parsed.column || parsed.fields) {
        return parsed.tableFields || parsed.column || parsed.fields;
    }
    if (parsed.tableSchema && typeof parsed.tableSchema === 'object') {
        const firstTopic = Object.keys(parsed.tableSchema)[0];
        return firstTopic ? parsed.tableSchema[firstTopic] : [];
    }
    return [];
};
const stripKafkaSchemaFromJson = (raw) => {
    try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) return '';
        if (!parsed || typeof parsed !== 'object') return '';
        const extra = sanitizeKafkaExtraConfig(parsed);
        return Object.keys(extra).length ? JSON.stringify(extra) : '';
    } catch (error) {
        return raw;
    }
};
const mergeKafkaExtraConfigJson = () => {
    const raw = form.value.taskParams.kafkaConfigJson;
    if (!raw) return true;
    try {
        const parsed = parsePlainJson(raw, 'Kafka扩展配置');
        form.value.taskParams.kafkaConfig = {
            ...plainObject(form.value.taskParams.kafkaConfig),
            ...sanitizeKafkaExtraConfig(parsed),
        };
        return true;
    } catch (error) {
        proxy.$message.warning('Kafka扩展配置不是合法 JSON 对象');
        return false;
    }
};
const sanitizeKafkaExtraConfig = (config) => {
    if (!config || typeof config !== 'object' || Array.isArray(config)) return {};
    const extra = { ...config };
    ['tableFields', 'tableSchema', 'column', 'columns', 'fields'].forEach((key) => {
        delete extra[key];
    });
    return extra;
};
const hasKafkaTopicConfig = () => {
    const taskParams = form.value.taskParams || {};
    const kafkaConfig = plainObject(taskParams.kafkaConfig);
    const directTopics = [taskParams.topic, kafkaConfig.topic];
    if (directTopics.some((value) => value !== null && value !== undefined && String(value).trim())) {
        return true;
    }
    const topics = kafkaConfig.topics;
    if (Array.isArray(topics)) {
        return topics.some((value) => value !== null && value !== undefined && String(value).trim());
    }
    return topics !== null && topics !== undefined && String(topics).split(',').some((value) => value.trim());
};
const collectStreamFields = () => {
    return normalizeStreamFields(ColumnByAssettab.value)
        .map((field, index) => ({
            ...field,
            id: index + 1,
            columnName: String(field.columnName || '').trim(),
            columnType: String(field.columnType || 'STRING').trim().toUpperCase(),
        }))
        .filter((field) => field.columnName);
};
const validateStreamFields = () => {
    const fields = collectStreamFields();
    if (!fields.length) {
        proxy.$message.warning('请添加至少一个流式读取字段');
        return false;
    }
    const names = fields.map((field) => field.columnName);
    if (new Set(names).size !== names.length) {
        proxy.$message.warning('流式读取字段名不能重复');
        return false;
    }
    ColumnByAssettab.value = fields;
    return true;
};
const streamFieldsToColumns = (fields) => {
    return fields.map((field) => {
        const column = {
            name: field.columnName,
            type: field.columnType,
        };
        ['index', 'value', 'format', 'parseFormat', 'splitter', 'isPart', 'notNull'].forEach((key) => {
            if (field[key] !== undefined && field[key] !== null && field[key] !== '') {
                column[key] = field[key];
            }
        });
        if (field.isKey) {
            column.isKey = true;
        }
        return column;
    });
};
const mergeKafkaConfigJson = () => {
    if (!validateStreamFields()) return false;
    if (!mergeKafkaExtraConfigJson()) return false;
    const fields = ColumnByAssettab.value;
    const columns = streamFieldsToColumns(fields);
    const keyFields = fields.filter((field) => field.isKey).map((field) => field.columnName);
    const kafkaConfig = {
        ...plainObject(form.value.taskParams.kafkaConfig),
        split: false,
        tableFields: fields,
        column: columns,
    };
    if (keyFields.length) {
        kafkaConfig.keyFields = keyFields;
    } else {
        delete kafkaConfig.keyFields;
    }
    form.value.taskParams.kafkaConfig = kafkaConfig;
    return true;
};
const addStreamField = () => {
    ColumnByAssettab.value.push({
        id: ColumnByAssettab.value.length + 1,
        columnName: '',
        columnType: 'STRING',
        isKey: false,
        key: '',
        pkFlag: '0',
        columnComment: '',
    });
};
const removeStreamField = (index) => {
    ColumnByAssettab.value.splice(index, 1);
};
const handleChange = (value) => {
    const selectedDatasource = TablesByDataSource.value.find((item) => item.tableName == value);
    if (selectedDatasource) {
        setTableName(selectedDatasource);
        ColumnByAssettab.value = [];
        const dsId = form.value?.taskParams?.readerDatasource?.datasourceId;
        if (!dsId) {
            proxy.$message.warning('请先选择源数据库连接');
            return;
        }
        getColumnByAssetIdList(selectedDatasource.id, selectedDatasource);
    }
};
let dppNoPageListList = ref([]);
const dppLoading = ref(false);
const getdppNoPageListList = async (id) => {
    dppLoading.value = true;
    listDppAsset({
        pageNum: 1,
        pageSize: 9999,
        spaceCode: userStore.spaceCode,
        spaceId: userStore.spaceId,
        params: {
            sourceType: [0, 1],
        },
        orderByColumn: "create_time",
        isAsc: "desc",
    }).then((response) => {
        dppNoPageListList.value = response.data.rows;
        loading.value = false;
    }).finally(() => {
        dppLoading.value = false
    });
};

// 连接方式切换
const handleReleaseStateChange = (value) => {
    // 保存当前 datasourceId，当 clmt 未实际变化（异步字典加载的伪事件）时恢复
    const nodeData = getNodeData();
    const originalClmt = nodeData?.taskParams?.clmt;
    const prevDsId = getDatasourceId(form.value?.taskParams?.readerDatasource) ||
        getDatasourceId(nodeData?.taskParams?.readerDatasource);

    if (value == 1) {
        getdppNoPageListList();
        form.value.taskParams.asset_id_cpoy = '';
    } else {
        getDatasourceList();
    }
    resetReaderSelection();
    ColumnByAssettab.value = [];
    TablesByDataSource.value = [];
    form.value.taskParams.readerDatasource = {};
    form.value.taskParams.dateIncrementConfig.column = [];
    dateIncrementCursorDraft.value = '';
    form.value.taskParams.idIncrementConfig.incrementColumn = null;
    form.value.taskParams.readModeType = '1';
    ensureStreamConfigs();

    // clmt 未变化说明是异步字典加载触发的伪事件，恢复 datasourceId
    if (String(value) === String(originalClmt) && prevDsId && String(prevDsId) !== '') {
        form.value.taskParams.readerDatasource.datasourceId = String(prevDsId);
        form.value.taskParams.readerDatasource.datasource_id = String(prevDsId);
        ensureSelectedDatasourceOption();
    }
};
const handleAssetTableChange = (value) => {
    // 找到对应的选中项
    const selectedItem = dppNoPageListList.value.find((item) => item.id == value);

    form.value.taskParams.asset_id = selectedItem.tableName;
    form.value.taskParams.table_name = selectedItem.tableName;

    // 调用 API 获取数据源信息
    getDaDatasource(selectedItem.datasourceId).then((response) => {
        let { datasourceType, datasourceConfig, ip, port, id, datasourceName } = response.data;
        let code = JSON.parse(datasourceConfig);
        // 更新 readerDatasource
        form.value.taskParams.readerDatasource = {
            datasourceType,
            datasourceConfig,
            ip,
            port,
            dbname: code.dbname,
            datasource_id: id,
            datasourceId: String(id),
            datasourceName
        };
        ensureStreamConfigs();
        // setTableName(response.data);
        // 获取列数据
        ColumnByAssettab.value = [];
        getColumnByAssetIdList(id, value);
    });
};

const off = () => {
    proxy.resetForm('dpModelRefs');
    dateIncrementCursorDraft.value = '';
    // 清空表格字段数据
    ColumnByAssettab.value = [];
    TablesByDataSource.value = [];
    tableFields.value = [];
};
// 保存数据
const saveData = async () => {
    try {
        // 异步验证表单
        const valid = await dpModelRefs.value.validate();
        if (!valid) return;
        ensureStreamConfigs();
        if (typeof form.value.taskParams.topic === 'string') {
            form.value.taskParams.topic = form.value.taskParams.topic.trim();
        }
        if (
            form.value?.taskParams.type == '1' &&
            (!ColumnByAssettab.value || ColumnByAssettab.value.length == 0)
            && !(form.value?.taskParams.readModeType == '4' && isStreamReader.value)
        ) {
            return proxy.$message.warning('校验未通过，请选择属性字段');
        }
        if (isKafkaReader.value && form.value?.taskParams.clmt == '2') {
            return proxy.$message.warning('Kafka 数据源不支持 SQL 语句读取，请使用数据连接方式并配置 Topic');
        }
        if (form.value?.taskParams.clmt != '2' && form.value?.taskParams.readModeType == '4' && !isStreamReader.value) {
            return proxy.$message.warning('当前数据源暂不支持 CDC/流式读取');
        }
        if (form.value?.taskParams.clmt != '2' && form.value?.taskParams.readModeType == '4' && isKafkaReader.value && !mergeKafkaConfigJson()) {
            return;
        }
        if (form.value?.taskParams.clmt != '2' && form.value?.taskParams.readModeType == '4' && isKafkaReader.value && !hasKafkaTopicConfig()) {
            return proxy.$message.warning('请输入 Kafka Topic 或在扩展配置中配置 topics');
        }
        if (form.value?.taskParams.clmt != '2' && form.value?.taskParams.readModeType == '4' && !isKafkaReader.value && !validateStreamFields()) {
            return;
        }
        if (form.value?.taskParams.clmt != '2' && form.value?.taskParams.readModeType == '4' && !isKafkaReader.value && !mergeCdcConfigJson()) {
            return;
        }
        if (form.value?.taskParams.readModeType == '3') {
            const dateIncrementColumns = form.value.taskParams.dateIncrementConfig.column;
            if (dateIncrementColumns.length !== 1 || dateIncrementColumns[0].type !== '2') {
                return proxy.$message.warning('请在属性字段中勾选一个时间类型的增量字段');
            }
            if (!dateIncrementColumns[0].cursorTime) {
                return proxy.$message.warning('请选择时间增量的初始游标');
            }
        }
        // 如果没有 code，就调用接口获取唯一的 code
        if (!form.value.code) {
            loading.value = true;
            const response = await getNodeUniqueKey({
                spaceCode: userStore.spaceCode || '133545087166112',
                spaceId: userStore.spaceId
            });
            loading.value = false;
            form.value.code = response.data;
        }
        const taskParams = form.value?.taskParams;
        taskParams.tableFields = ColumnByAssettab.value;
        if (taskParams.readModeType == '4' && isStreamReader.value) {
            taskParams.streamFields = ColumnByAssettab.value;
            taskParams.columns = ColumnByAssettab.value.map(({ columnName }) => columnName);
            taskParams.column = streamFieldsToColumns(ColumnByAssettab.value);
            taskParams.keyFields = ColumnByAssettab.value.filter((field) => field.isKey).map((field) => field.columnName);
        }
        taskParams.columnsList = ColumnByAssettab.value.map(({ columnName, columnType }) => ({
            colName: columnName,
            dataType: columnType,
        }));
        taskParams.columns = taskParams.tableFields.map(({ columnName }) => columnName);
        taskParams.inputFields = taskParams.tableFields;

        emit("confirm", form.value);

    } catch (error) {
        console.error('保存数据失败:', error);
        loading.value = false;
    }
};
const closeDialog = () => {
    off();
    // 关闭对话框
    emit('update', false);
};

// 监听属性变化
function deepCopy(data) {
    if (data === undefined || data === null) {
        return {}; // 或者返回一个默认值
    }
    try {
        return JSON.parse(JSON.stringify(data));
    } catch (e) {
        return {}; // 或者返回一个默认值
    }
}
function sqlParseFunction() {
    ColumnByAssettab.value = [];
    loadingList.value = true;
    sqlParse({
        sourceId: form.value.taskParams.readerDatasource.datasourceId,
        sql: form.value.taskParams.querySql
    }).then((res) => {
        ColumnByAssettab.value = res.data;
        form.value.taskParams.inputFields = res.data;
        loadingList.value = false;
    });
}
function changeTextarea(val) {
    form.value.taskParams.querySql = val;
}
// 监听属性变化
watch(
    () => [props.visible, props.currentNode?.id],
    ([visible]) => {
    if (visible) {
        // 数据源
        if (props.currentNode?.data?.taskParams?.clmt == 1) {
            getdppNoPageListList();
        } else {
            getDatasourceList();
        }
        const nodeData = props.currentNode?.getProp?.("data") || props.currentNode?.data || {};
        form.value = deepCopy(nodeData);
        form.value.taskParams = form.value.taskParams || {};
        normalizeReaderDatasource(form.value.taskParams);
        ensureStreamConfigs();
        const savedTableFields = deepCopy(nodeData.taskParams?.tableFields);
        const savedStreamFields = deepCopy(form.value.taskParams?.streamFields);
        const restoredFields = Array.isArray(savedStreamFields) && savedStreamFields.length
            ? savedStreamFields
            : savedTableFields;
        ColumnByAssettab.value = Array.isArray(restoredFields)
            ? (form.value.taskParams?.readModeType == '4' ? normalizeStreamFields(restoredFields) : restoredFields)
            : [];

        const ds = form.value?.taskParams?.readerDatasource;
        const dsId = getDatasourceId(ds);
        const normalizedDatasourceType = normalizeDatasourceType(ds.datasourceType);
        if (dsId && normalizedDatasourceType !== 'kafka'
            && !streamingQueueDatasourceTypes.includes(normalizedDatasourceType)) {
            getTablesByDatasourceId(dsId);
        }

        // 修复：确保数据源列表包含已保存的选择
        if (dsId && dsId !== '') {
            const savedId = String(dsId);
            ensureSelectedDatasourceOption();
            const existingDatasource = createTypeList.value.find(item => String(item.id) === savedId);
            if (!existingDatasource) {
                // 从多个来源收集数据源信息
                let datasourceName = '';
                let datasourceType = '';
                let datasourceIp = '';
                let datasourcePort = '';
                let datasourceConfig = '{}';
                
                // 优先从表单数据中获取
                if (ds.datasourceName) {
                    datasourceName = ds.datasourceName;
                }
                if (ds.datasourceType) {
                    datasourceType = ds.datasourceType;
                }
                if (ds.ip) {
                    datasourceIp = ds.ip;
                }
                if (ds.port) {
                    datasourcePort = ds.port;
                }
                if (ds.datasourceConfig) {
                    datasourceConfig = ds.datasourceConfig;
                }
                
                // 其次从原始节点数据中获取
                if (!datasourceName || !datasourceType) {
                    const originalNodeData = getNodeData();
                    const originalDs = originalNodeData?.taskParams?.readerDatasource;
                    if (String(originalDs?.datasourceId ?? originalDs?.datasource_id ?? '') === savedId) {
                        if (!datasourceName && originalDs.datasourceName) {
                            datasourceName = originalDs.datasourceName;
                        }
                        if (!datasourceType && originalDs.datasourceType) {
                            datasourceType = originalDs.datasourceType;
                        }
                        if (!datasourceIp && originalDs.ip) {
                            datasourceIp = originalDs.ip;
                        }
                        if (!datasourcePort && originalDs.port) {
                            datasourcePort = originalDs.port;
                        }
                        if (originalDs.datasourceConfig) {
                            datasourceConfig = originalDs.datasourceConfig;
                        }
                    }
                }
                
                // 使用兜底信息
                if (!datasourceName) {
                    datasourceName = '数据源 ' + savedId;
                }
                if (!datasourceType) {
                    datasourceType = ds.datasourceType || '';
                }
                
                // 创建数据源对象
                const datasourceToAdd = {
                    id: savedId,
                    datasourceName: datasourceName,
                    datasourceType: datasourceType,
                    ip: datasourceIp,
                    port: datasourcePort,
                    datasourceConfig: datasourceConfig
                };
                
                // 检查是否已经存在（再次确认）
                const exists = createTypeList.value.some(item => String(item.id) === savedId);
                if (!exists) {
                    createTypeList.value.unshift(datasourceToAdd);
                    console.log('回echo: 添加缺失的数据源到列表，ID:', savedId, '名称:', datasourceName);
                }
            }
        }

        // 修复：确保资产表列表包含已保存的选择 (当 clmt == 1 时)
        if (props.currentNode?.data?.taskParams?.clmt == '1' &&
            props.currentNode?.data?.taskParams?.asset_id_cpoy &&
            props.currentNode?.data?.taskParams?.asset_id_cpoy !== '') {
            
            const savedAssetId = String(props.currentNode.data.taskParams.asset_id_cpoy);
            const existingAsset = dppNoPageListList.value.find(item => item.id === savedAssetId);
            if (!existingAsset) {
                // 从多个来源收集资产表信息
                let assetName = '';
                let assetDatasourceId = '';
                let assetTableName = '';
                
                // 优先从表单数据中获取
                if (form.value?.taskParams?.asset_id_cpoy?.name) {
                    assetName = form.value.taskParams.asset_id_cpoy.name;
                }
                if (form.value?.taskParams?.asset_id_cpoy?.datasourceId) {
                    assetDatasourceId = String(form.value.taskParams.asset_id_cpoy.datasourceId);
                }
                if (form.value?.taskParams?.asset_id_cpoy?.tableName) {
                    assetTableName = form.value.taskParams.asset_id_cpoy.tableName;
                }
                
                // 其次从原始节点数据中获取
                if (!assetName || !assetTableName) {
                    const originalNodeData = props.currentNode?.getProp?.("data") || props.currentNode?.data || {};
                    const originalAsset = originalNodeData?.taskParams?.asset_id_cpoy;
                    if (originalAsset?.id === savedAssetId) {
                        if (!assetName && originalAsset.name) {
                            assetName = originalAsset.name;
                        }
                        if (!assetDatasourceId && originalAsset.datasourceId) {
                            assetDatasourceId = String(originalAsset.datasourceId);
                        }
                        if (!assetTableName && originalAsset.tableName) {
                            assetTableName = originalAsset.tableName;
                        }
                    }
                }
                
                // 使用兜底信息
                if (!assetName) {
                    assetName = '资产 ' + savedAssetId;
                }
                if (!assetTableName) {
                    assetTableName = savedAssetId;
                }
                
                // 创建资产表对象
                const assetToAdd = {
                    id: savedAssetId,
                    name: assetName,
                    datasourceId: assetDatasourceId,
                    tableName: assetTableName
                };
                
                // 检查是否已经存在（再次确认）
                const exists = dppNoPageListList.value.some(item => item.id === savedAssetId);
                if (!exists) {
                    dppNoPageListList.value.unshift(assetToAdd);
                    console.log('回echo: 添加缺失的资产表到列表，ID:', savedAssetId, '名称:', assetName);
                }
            }
        }

        // 处理回echo - 确保数据源和资产表回echo
        handle回echo();
    } else {
        off();
    }
}, { immediate: true });
</script>
<style scoped lang="less">
.blue-text {
    color: #2666fb;
}

.stream-field-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
}
</style>

