<template>
    <el-dialog v-model="visibleDialog" :draggable="true" class="medium-dialog" :title="currentNode?.data?.name"
        showCancelButton :show-close="false" destroy-on-close :close-on-click-modal="false">
        <el-form ref="dpModelRefs" :model="form" label-width="110px" @submit.prevent v-loading="loading"
            :disabled="info">

            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="节点名称" prop="name"
                        :rules="[{ required: true, message: '请输入节点名称', trigger: 'change' }]">
                        <el-input v-if="!info" v-model="form.name" placeholder="请输入节点名称" />
                        <div v-else class="form-readonly">{{ form.name }}</div>
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="类型" prop="typeName">
                        <el-input v-if="!info" v-model="form.taskParams.typeName" placeholder="请输入类型" disabled />
                        <div v-else class="form-readonly">{{ form.taskParams.typeName }}</div>
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="24">
                    <el-form-item label="描述" prop="description">
                        <el-input v-if="!info" v-model="form.description" type="textarea" placeholder="请输入描述" />
                        <div v-else class="form-readonly">{{ form.description || '-' }}</div>
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="连接方式" prop="clmt">
                        <el-radio-group v-if="!info" @change="handleReleaseStateChange" v-model="form.taskParams.clmt">
                            <template v-for="dict in col_connection" :key="dict.value">
                                <el-radio :value="dict.value">
                                    {{ dict.label }}
                                </el-radio>
                            </template>
                        </el-radio-group>
                        <div class="form-readonly" v-else>{{col_connection.find((item) => item.value ==
                            form.taskParams.clmt)?.label || '-'}}</div>
                    </el-form-item>
                </el-col>
                <template v-if="form.taskParams.clmt == '1'">
                    <el-col :span="12">
                        <el-form-item label="资产表" prop="taskParams.asset_id_cpoy" :rules="[
                            { required: true, message: '请选择资产表', trigger: 'blur' }
                        ]">
                            <el-select v-if="!info" v-model="form.taskParams.asset_id_cpoy" filterable
                                @change="handleAssetTableChange" :loading="dppLoading">
                                <el-option v-for="item in dppNoPageListList" :key="item.id" :label="item.name"
                                    :value="item.id" />
                            </el-select>
                            <div class="form-readonly" v-else>{{dppNoPageListList.find((item) => item.id ==
                                form.taskParams.asset_id_cpoy)?.name || '-'}}</div>
                        </el-form-item>
                    </el-col>
                </template>
            </el-row>
            <!-- -->
            <template v-if="form.taskParams.clmt == '0' || form.taskParams.clmt == '2'">
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="源数据库连接" prop="taskParams.readerDatasource.datasourceId" :rules="[
                            {
                                required: true,
                                message: '请选择源数据库连接',
                                trigger: 'change'
                            }
                        ]">
                            <el-select v-if="!info" v-model="form.taskParams.readerDatasource.datasourceId"
                                placeholder="请选择源数据库连接" @change="handleDatasourceChange" filterable>
                                <el-option v-for="dict in createTypeList" :key="dict.id" :label="dict.datasourceName"
                                    :value="dict.id"></el-option>
                            </el-select>
                            <div class="form-readonly" v-else>{{createTypeList.find((item) => item.id ==
                                form.taskParams.readerDatasource.datasourceId)?.datasourceName || '-'}}</div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="数据连接类型" prop="taskParams.readerDatasource.datasourceType">
                            <el-input v-if="!info" v-model="form.taskParams.readerDatasource.datasourceType"
                                placeholder="请输入数据连接类型" disabled />
                            <div class="form-readonly" v-else>{{ form.taskParams.readerDatasource.datasourceType || '-'
                                }}</div>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="数据连接实例" prop="taskParams.readerDatasource.dbname">
                            <el-input v-if="!info" v-model="form.taskParams.readerDatasource.dbname"
                                placeholder="请输入数据连接实例" disabled />
                            <div class="form-readonly" v-else>{{ form.taskParams.readerDatasource.dbname || '-' }}</div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12" v-if="form.taskParams.clmt == '0' && !isKafkaReader">
                        <el-form-item label="选择表" prop="taskParams.asset_id"
                            :rules="[{ required: true, message: '请选择表', trigger: 'change' }]">
                            <el-select v-if="!info" v-model="form.taskParams.asset_id" filterable @change="handleChange"
                                :loading="loadingTables">
                                <el-option v-for="item in TablesByDataSource" :key="item.tableName"
                                    :label="item.tableName" :value="item.tableName" />
                            </el-select>
                            <div class="form-readonly" v-else>{{ form.taskParams.asset_id }}</div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12" v-if="form.taskParams.clmt == '2'">
                        <el-form-item>
                            <div style="text-align: right; width: 100%">
                                <el-button size="small" type="primary" @click="sqlParseFunction"
                                    class="sql-parse-btn">SQL解析</el-button>
                            </div>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20" v-if="form.taskParams.clmt == '2'">
                    <el-col :span="24">
                        <el-form-item label="SQL语句" prop="taskParams.querySql"
                            :rules="[{ required: true, message: '请输入SQL语句', trigger: 'blur' }]">
                            <sql-editor ref="editorRef" :value="form.taskParams.querySql" class="sql-editor"
                                :height="'140px'" @changeTextarea="changeTextarea($event)" />
                        </el-form-item>
                    </el-col>
                </el-row>
            </template>
            <el-row :gutter="20" v-if="form.taskParams.clmt != '2'">
                <el-col :span="12">
                    <el-form-item label="读取模式" prop="taskParams.readModeType" :rules="[
                        {
                            required: true,
                            message: '请选择读取模式',
                            trigger: 'change'
                        }
                    ]">
                        <el-radio-group v-if="!info" v-model="form.taskParams.readModeType"
                            @change="handlereadModeTypeChange">
                            <el-radio value="1">全量</el-radio>
                            <el-radio value="2">id增量</el-radio>
                            <el-radio value="3">时间增量</el-radio>
                            <el-radio value="4" :disabled="!isStreamReader">CDC/流式</el-radio>
                        </el-radio-group>
                        <div class="form-readonly" v-else>{{ form.taskParams.readModeType == 1 ? '全量' :
                            form.taskParams.readModeType == 2 ?
                                'id增量' : form.taskParams.readModeType == 3 ? '时间增量' : 'CDC/流式' }}</div>
                    </el-form-item>
                </el-col>
            </el-row>
            <template v-if="form.taskParams.clmt != '2' && form.taskParams.readModeType == 4">
                <el-row :gutter="20" v-if="isKafkaReader">
                    <el-col :span="12">
                        <el-form-item label="Topic" prop="taskParams.topic">
                            <el-input v-if="!info" v-model="form.taskParams.topic" placeholder="请输入 Topic" />
                            <div class="form-readonly" v-else>{{ form.taskParams.topic || '-' }}</div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="消费组" prop="taskParams.kafkaConfig.groupId">
                            <el-input v-if="!info" v-model="form.taskParams.kafkaConfig.groupId"
                                placeholder="默认 datamaster-chunjun" />
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaConfig.groupId || '-' }}</div>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20" v-if="isKafkaReader">
                    <el-col :span="12">
                        <el-form-item label="启动位置" prop="taskParams.kafkaConfig.mode">
                            <el-select v-if="!info" v-model="form.taskParams.kafkaConfig.mode">
                                <el-option label="最新" value="LATEST" />
                                <el-option label="最早" value="EARLIEST" />
                                <el-option label="消费组位点" value="GROUP_OFFSETS" />
                                <el-option label="指定时间" value="TIMESTAMP" />
                                <el-option label="指定位点" value="SPECIFIC_OFFSETS" />
                            </el-select>
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaConfig.mode || '-' }}</div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="消息格式" prop="taskParams.kafkaConfig.codec">
                            <el-input v-if="!info" v-model="form.taskParams.kafkaConfig.codec" placeholder="默认 json" />
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaConfig.codec || '-' }}</div>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20" v-if="isKafkaReader">
                    <el-col :span="24">
                        <el-form-item label="字段配置" prop="taskParams.kafkaFieldsJson"
                            :rules="[{ required: true, message: '请输入字段配置', trigger: 'blur' }]">
                            <el-input v-if="!info" v-model="form.taskParams.kafkaFieldsJson" type="textarea"
                                placeholder='例如 [{"columnName":"id","columnType":"STRING","key":"id"}]' />
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaFieldsJson || '-' }}</div>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20" v-if="isKafkaReader">
                    <el-col :span="24">
                        <el-form-item label="Kafka扩展配置" prop="taskParams.kafkaConfigJson">
                            <el-input v-if="!info" v-model="form.taskParams.kafkaConfigJson" type="textarea"
                                placeholder='可选 JSON，例如 {"consumerSettings":{"security.protocol":"SASL_PLAINTEXT"}}' />
                            <div class="form-readonly" v-else>{{ form.taskParams.kafkaConfigJson || '-' }}</div>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20" v-if="!isKafkaReader && !isStreamingMqReader">
                    <el-col :span="12">
                        <el-form-item label="Server ID" prop="taskParams.cdcConfig.serverId">
                            <el-input v-if="!info" v-model="form.taskParams.cdcConfig.serverId"
                                placeholder="MySQL CDC 可空，默认按数据源生成" />
                            <div class="form-readonly" v-else>{{ form.taskParams.cdcConfig.serverId || '-' }}</div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="时间格式" prop="taskParams.cdcConfig.timestampFormat">
                            <el-input v-if="!info" v-model="form.taskParams.cdcConfig.timestampFormat"
                                placeholder="yyyy-MM-dd HH:mm:ss" />
                            <div class="form-readonly" v-else>{{ form.taskParams.cdcConfig.timestampFormat || '-' }}</div>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20" v-if="!isKafkaReader && !isStreamingMqReader">
                    <el-col :span="24">
                        <el-form-item label="CDC扩展配置" prop="taskParams.cdcConfigJson">
                            <el-input v-if="!info" v-model="form.taskParams.cdcConfigJson" type="textarea"
                                placeholder='可选 JSON，例如 {"readPosition":"current","startScn":"123"}' />
                            <div class="form-readonly" v-else>{{ form.taskParams.cdcConfigJson || '-' }}</div>
                        </el-form-item>
                    </el-col>
                </el-row>
            </template>
            <template v-if="form.taskParams.readModeType == 2">
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="id字段" prop="taskParams.idIncrementConfig.incrementColumn" :rules="[
                            {
                                required: true,
                                message: '请选择id字段',
                                trigger: 'blur'
                            }
                        ]">
                            <el-select v-if="!info" v-model="form.taskParams.idIncrementConfig.incrementColumn"
                                collapse-tags collapse-tags-tooltip filterable placeholder="请选择id字段">
                                <el-option v-for="item in ColumnByAssettab" :key="item.columnName"
                                    :label="item.columnName" :value="item.columnName" />
                            </el-select>
                            <div class="form-readonly" v-else>{{ form.taskParams.idIncrementConfig.incrementColumn ||
                                '-' }}</div>
                        </el-form-item>
                    </el-col>

                    <el-col :span="12">
                        <el-form-item label="开始值" prop="taskParams.idIncrementConfig.incrementStart" :rules="[
                            { required: true, message: '请输入开始值', trigger: 'change' },
                            { validator: checkInteger, trigger: 'change' }
                        ]">
                            <el-input v-if="!info" v-model="form.taskParams.idIncrementConfig.incrementStart"
                                placeholder="请输入开始值" type="number">
                            </el-input>
                            <div class="form-readonly" v-else>{{ form.taskParams.idIncrementConfig.incrementStart || '-'
                                }}</div>
                        </el-form-item>
                    </el-col>
                </el-row>
            </template>
            <template v-if="form.taskParams.readModeType == 3">
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="时间格式" prop="taskParams.dateIncrementConfig.dateFormat">
                            <el-select v-if="!info" v-model="form.taskParams.dateIncrementConfig.dateFormat"
                                placeholder="请选择时间格式">
                                <el-option v-for="item in dateFormatOptions" :key="item.value" :label="item.label"
                                    :value="item.value" />
                            </el-select>
                            <div class="form-readonly" v-else>{{dateFormatOptions.find(item => item.value ==
                                form.taskParams.dateIncrementConfig.dateFormat)?.label || '-'}}</div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="初始游标">
                            <el-date-picker v-if="!info" clearable v-model="dateIncrementCursorTime"
                                :type="dateIncrementPickerType" :format="dateIncrementPickerFormat"
                                :value-format="dateIncrementPickerFormat" placeholder="请选择首次同步的起始时间" />
                            <div class="form-readonly" v-else>{{ dateIncrementCursorTime || '-' }}</div>
                        </el-form-item>
                    </el-col>
                </el-row>
            </template>
            <el-row :gutter="20" v-if="form.taskParams.clmt != '2' && form.taskParams.readModeType != 4">
                <el-col :span="24">
                    <el-form-item label="where条件" prop="where">
                        <el-input v-if="!info" v-model="form.taskParams.where" type="textarea"
                            :placeholder="'例如 id > 10 and id < 1000，请不要以分号;结尾'" />
                        <div class="form-readonly" v-else>{{ form.taskParams.where || '-' }}</div>
                    </el-form-item>
                </el-col>
            </el-row>
            <template v-if="form.taskParams.clmt != '2'">
                <div class="h2-title">属性字段</div>
                <el-table stripe height="310px" v-loading="loadingList" :data="ColumnByAssettab">
                    <el-table-column label="序号" type="index" width="80" align="left">
                        <template #default="scope">
                            <span>{{ scope.$index + 1 }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="英文名称" align="left" prop="columnName"
                        :show-overflow-tooltip="{ effect: 'light' }">
                        <template #default="scope">
                            {{ scope.row.columnName || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="中文名称" align="left" prop="columnComment"
                        :show-overflow-tooltip="{ effect: 'light' }">
                        <template #default="scope">
                            {{ scope.row.columnComment || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="字段类型" align="left" prop="columnType">
                        <template #default="scope">
                            {{ scope.row.columnType || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="是否主键" align="left" prop="pkFlag" v-if="form?.taskParams.type == '1'">
                        <template #default="scope">
                            <el-switch v-model="scope.row.pkFlag" :active-value="'1'" :inactive-value="'0'" disabled />
                        </template>
                    </el-table-column>
                    <el-table-column label="字段长度" align="left" prop="columnLength" v-if="form?.taskParams.type == '1'">
                        <template #default="scope">
                            {{ scope.row.columnLength || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="小数经度" align="left" prop="columnScale" v-if="form?.taskParams.type == '1'">
                        <template #default="scope">
                            {{ scope.row.columnScale || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="增量配置" align="center" fixed="right" width="150"
                        v-if="form.taskParams.readModeType != 1 && form.taskParams.readModeType != 4">
                        <template #default="scope">
                            <template v-if="info">
                                <el-tag v-if="form.taskParams.readModeType == 2
                                    && form.taskParams.idIncrementConfig.incrementColumn == scope.row.columnName"
                                    type="success">增量字段</el-tag>
                                <el-tag v-else-if="form.taskParams.readModeType == 3
                                    && hasDateIncrementConfig(scope.row.columnName)" type="success">增量字段</el-tag>
                                <span v-else>-</span>
                            </template>
                            <template v-else-if="form.taskParams.readModeType == 2">
                                <el-tag v-if="form.taskParams.idIncrementConfig.incrementColumn == scope.row.columnName"
                                    type="success">已设置</el-tag>
                                <el-button v-else link type="primary"
                                    @click="setIdIncrementColumn(scope.row)">设为增量字段</el-button>
                            </template>
                            <template v-else-if="form.taskParams.readModeType == 3">
                                <el-checkbox :model-value="hasDateIncrementConfig(scope.row.columnName)"
                                    :disabled="!isTimeColumn(scope.row)" @change="setDateIncrementColumn(scope.row, $event)">
                                    {{ isTimeColumn(scope.row) ? '选择' : '非时间字段' }}
                                </el-checkbox>
                            </template>
                        </template>
                    </el-table-column>
                </el-table>
            </template>

        </el-form>
        <template #footer>
            <div style="text-align: right">
                <el-button @click="closeDialog">关闭</el-button>
                <el-button type="primary" @click="saveData" v-if="!info">保存</el-button>
            </div>
        </template>
    </el-dialog>
</template>
<script setup>
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
});

const emit = defineEmits(['update', 'confirm']);
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
const isKafkaReader = computed(() => form.value?.taskParams?.readerDatasource?.datasourceType === 'Kafka');
const cdcDatasourceTypes = ['MySql', 'Oracle', 'Oracle11', 'SQL_Server', 'SQL_Server2008'];
const streamingMqDatasourceTypes = ['RabbitMQ', 'Redis', 'RocketMQ'];
const isCdcReader = computed(() => {
    const datasourceType = form.value?.taskParams?.readerDatasource?.datasourceType;
    return cdcDatasourceTypes.includes(datasourceType);
});
const isStreamingMqReader = computed(() => {
    const datasourceType = form.value?.taskParams?.readerDatasource?.datasourceType;
    return streamingMqDatasourceTypes.includes(datasourceType);
});
const isStreamReader = computed(() => isKafkaReader.value || isStreamingMqReader.value || isCdcReader.value);

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
            projectCode: userStore.projectCode,
            projectId: userStore.projectId,
            datasourceType: "DM8,Oracle11,MySql,Oracle,Kingbase8,Doris,ClickHouse,Hive,MongoDB,Elasticsearch,SQL_Server,SQL_Server2008,PostgreSQL,Kafka",
        });
        createTypeList.value = response.data.rows;
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
    ColumnByAssettab.value = await fetchData(
        getColumnByAssetId,
        {
            withRule: 2,
            id: form.value.taskParams.readerDatasource.datasourceId,
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
    };
    form.value.taskParams.cdcConfig = {
        serverId: '',
        timestampFormat: 'yyyy-MM-dd HH:mm:ss',
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
        datasource_id: id,
        datasourceId: id
    };
    resetReaderSelection();
    ensureStreamConfigs();
    form.value.taskParams.dateIncrementConfig.column = [];
    dateIncrementCursorDraft.value = '';
    form.value.taskParams.idIncrementConfig.incrementColumn = null;

    if (datasourceType === 'Kafka' || streamingMqDatasourceTypes.includes(datasourceType)) {
        if (form.value.taskParams.clmt == '2') {
            form.value.taskParams.clmt = '0';
        }
        form.value.taskParams.readModeType = '4';
        TablesByDataSource.value = [];
    } else {
        if (form.value.taskParams.readModeType == '4' && !cdcDatasourceTypes.includes(datasourceType)) {
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
        ...plainObject(taskParams.cdcConfig),
    };
    taskParams.kafkaConfig = {
        groupId: 'datamaster-chunjun',
        mode: 'LATEST',
        codec: 'json',
        ...plainObject(taskParams.kafkaConfig),
    };
    taskParams.kafkaFieldsJson = taskParams.kafkaFieldsJson || '';
    if (!taskParams.kafkaFieldsJson && taskParams.kafkaConfigJson) {
        const legacyFields = parseKafkaFieldsFromJson(taskParams.kafkaConfigJson);
        if (legacyFields.length) {
            taskParams.kafkaFieldsJson = JSON.stringify(legacyFields);
            taskParams.kafkaConfigJson = stripKafkaSchemaFromJson(taskParams.kafkaConfigJson);
        }
    }
    taskParams.kafkaConfigJson = taskParams.kafkaConfigJson || '';
    if (!taskParams.kafkaFieldsJson) {
        const fields = taskParams.kafkaConfig.tableFields || taskParams.tableFields;
        if (Array.isArray(fields) && fields.length) {
            taskParams.kafkaFieldsJson = JSON.stringify(fields);
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
const normalizeKafkaFields = (fields) => {
    if (!Array.isArray(fields)) return [];
    return fields
        .map((field, index) => {
            if (typeof field === 'string') {
                return {
                    id: index + 1,
                    columnName: field,
                    columnType: 'STRING',
                    key: field,
                    columnComment: '',
                };
            }
            if (!field || typeof field !== 'object') return null;
            const columnName = field.columnName || field.name || field.key;
            if (!columnName) return null;
            const normalizedField = {
                id: field.id || index + 1,
                columnName,
                columnType: field.columnType || field.type || 'STRING',
                key: field.key || columnName,
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
        return normalizeKafkaFields(Array.isArray(parsed) ? parsed : getKafkaFieldSource(parsed));
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
const mergeKafkaConfigJson = () => {
    const raw = form.value.taskParams.kafkaFieldsJson;
    if (!raw) {
        proxy.$message.warning('请输入 Kafka 字段配置');
        return false;
    }
    try {
        const parsed = JSON.parse(raw);
        const fields = normalizeKafkaFields(Array.isArray(parsed) ? parsed : getKafkaFieldSource(parsed));
        if (!fields.length) {
            proxy.$message.warning('Kafka 字段配置至少包含一个字段');
            return false;
        }
        if (!mergeKafkaExtraConfigJson()) {
            return false;
        }
        ColumnByAssettab.value = fields;
        form.value.taskParams.kafkaConfig = {
            ...plainObject(form.value.taskParams.kafkaConfig),
            tableFields: fields,
            column: fields.map((field) => {
                const column = {
                    name: field.columnName,
                    type: field.columnType,
                };
                ['index', 'value', 'format', 'parseFormat', 'splitter', 'isPart', 'notNull'].forEach((key) => {
                    if (field[key] !== undefined && field[key] !== null && field[key] !== '') {
                        column[key] = field[key];
                    }
                });
                return column;
            }),
        };
        return true;
    } catch (error) {
        proxy.$message.warning('Kafka 字段配置不是合法 JSON');
        return false;
    }
};
const handleChange = (value) => {
    const selectedDatasource = TablesByDataSource.value.find((item) => item.tableName == value);
    if (selectedDatasource) {
        setTableName(selectedDatasource);
        ColumnByAssettab.value = [];
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
        projectCode: userStore.projectCode,
        projectId: userStore.projectId,
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
};
const handleAssetTableChange = (value) => {
    // 找到对应的选中项
    const selectedItem = dppNoPageListList.value.find((item) => item.id == value);

    form.value.taskParams.asset_id = selectedItem.tableName;
    form.value.taskParams.table_name = selectedItem.tableName;

    // 调用 API 获取数据源信息
    getDaDatasource(selectedItem.datasourceId).then((response) => {
        let { datasourceType, datasourceConfig, ip, port, id } = response.data;
        let code = JSON.parse(datasourceConfig);
        // 更新 readerDatasource
        form.value.taskParams.readerDatasource = {
            datasourceType,
            datasourceConfig,
            ip,
            port,
            dbname: code.dbname,
            datasource_id: id,
            datasourceId: id
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
            && !(form.value?.taskParams.readModeType == '4' && isKafkaReader.value)
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
                projectCode: userStore.projectCode || '133545087166112',
                projectId: userStore.projectId
            });
            loading.value = false;
            form.value.code = response.data;
        }
        const taskParams = form.value?.taskParams;
        taskParams.tableFields = ColumnByAssettab.value;
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
watchEffect(() => {
    if (props.visible) {
        // 数据源
        if (props.currentNode.data.taskParams.clmt == 1) {
            getdppNoPageListList();
        } else {
            getDatasourceList();
        }
        form.value = deepCopy(props.currentNode.data);
        ensureStreamConfigs();
        const savedTableFields = deepCopy(props.currentNode.data.taskParams.tableFields);
        ColumnByAssettab.value = Array.isArray(savedTableFields) ? savedTableFields : [];

    } else {
        off();
    }
});
const initialReaderDatasource = props.currentNode?.data?.taskParams?.readerDatasource;
if (initialReaderDatasource?.datasourceId
    && initialReaderDatasource.datasourceType !== 'Kafka'
    && !streamingMqDatasourceTypes.includes(initialReaderDatasource.datasourceType)) {
    getTablesByDatasourceId(initialReaderDatasource.datasourceId);
}
</script>
<style scoped lang="less">
.blue-text {
    color: #2666fb;
}
</style>

