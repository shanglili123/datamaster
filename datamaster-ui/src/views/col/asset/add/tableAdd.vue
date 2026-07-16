<template>
    <!-- // 数据库表 -->
    <el-row :gutter="20">
        <el-col :span="12">
            <el-form-item label="数据连接名称" prop="datasourceId"
                :rules="[{ required: true, message: '请选择数据连接名称', trigger: 'change' }]">
                <el-select v-model="localForm.datasourceId" placeholder="请选择数据连接名称" @change="handleDatasourceChange"
                    filterable :loading="loading"
                    :disabled="!props.isRegister && localForm.id && localForm.createType == '2'">
                    <el-option v-for="dict in createTypeList" :key="dict.id" :label="dict.datasourceName"
                        :value="dict.id" />
                </el-select>
            </el-form-item>
        </el-col>

        <el-col :span="12">
            <el-form-item label="数据连接类型" prop="datasourceType">
                <el-input v-model="localForm.datasourceType" disabled />
            </el-form-item>
        </el-col>
    </el-row>

    <el-row :gutter="20">
        <el-col :span="12">
            <el-form-item label="数据连接实例" prop="dbname">
                <el-input v-model="localForm.dbname" disabled />
            </el-form-item>
        </el-col>

        <el-col :span="12">
            <el-form-item label="选择表" prop="tableName"
                :rules="[{ required: true, message: '请选择表', trigger: 'change' }]">
                <el-select v-model="localForm.tableName" filterable @change="handleTableChange" :loading="loadingList"
                    :disabled="!props.isRegister && localForm.id && localForm.createType == '2'">
                    <el-option v-for="item in tablesByDataSource" :key="item.id || item.tableName"
                        :label="item.tableName" :value="item.tableName">
                        <span>{{ item.tableName }}</span>
                        <el-tag v-if="item.assetCreatedFlag" size="small" type="warning" style="margin-left: 8px">
                            已创建资产
                        </el-tag>
                    </el-option>
                </el-select>
            </el-form-item>
        </el-col>
    </el-row>
    <el-row :gutter="20" v-if="columnsByAssetTable.length">
        <el-col :span="24">
            <el-form-item label="字段配置">
                <el-table :data="columnsByAssetTable" border height="260">
                    <el-table-column label="字段名称" prop="columnName" min-width="140" show-overflow-tooltip />
                    <el-table-column label="字段注释" prop="columnComment" min-width="160" show-overflow-tooltip />
                    <el-table-column label="字段类型" prop="columnType" width="120" show-overflow-tooltip />
                    <el-table-column label="长度" prop="columnLength" width="80" />
                    <el-table-column label="主键" prop="pkFlag" width="70">
                        <template #default="{ row }">{{ row.pkFlag == '1' ? '是' : '否' }}</template>
                    </el-table-column>
                    <el-table-column label="可空" prop="nullableFlag" width="70">
                        <template #default="{ row }">{{ row.nullableFlag == '1' ? '是' : '否' }}</template>
                    </el-table-column>
                    <el-table-column label="敏感等级" min-width="160">
                        <template #default="{ row }">
                            <el-select v-model="row.sensitiveLevelId" clearable placeholder="请选择">
                                <el-option v-for="item in sensitiveLevelList" :key="item.id"
                                    :label="item.sensitiveLevel" :value="String(item.id)" />
                            </el-select>
                        </template>
                    </el-table-column>
                </el-table>
            </el-form-item>
        </el-col>
    </el-row>
</template>

<script setup>
import { ref, watch, watchEffect } from 'vue';
import useUserStore from '@/store/system/user.js';
import { getColumnByAssetId, getTablesByDataSourceId as getRealtimeTablesByDataSourceId } from '@/api/col/task/index.js';
import {
    listDaDatasourceByProjectCode
} from '@/api/ast/dataSource/dataSource.js';
import { getDaDiscoveryTableList, getDaDiscoveryColumnList } from '@/api/ast/discovery/discoveryTable.js';
import { listDaSensitiveLevel } from '@/api/ast/security/sensitiveLevel/sensitiveLevel.js';
import { listTable as listCatalogTable } from '@/api/cat/unreleased/table.js';
import { listColumn as listCatalogColumn } from '@/api/cat/unreleased/column.js';
const props = defineProps({
    form: Object,
    isRegister: Boolean,
    type: String
});
const emit = defineEmits(['update:form']);

const userStore = useUserStore();
const createTypeList = ref([]); // 数据源列表
const sensitiveLevelList = ref([]);
let loading = ref(false);
const getDatasourceList = async () => {
    try {
        loading.value = true;
        const response = await listDaDatasourceByProjectCode({ pageSize: 9999, projectCode: userStore.projectCode, projectId: userStore.projectId });
        createTypeList.value = response.data?.rows || [];
    } finally {
        loading.value = false;
    }
};
const getSensitiveLevelList = async () => {
    const response = await listDaSensitiveLevel({ pageSize: 9999, onlineFlag: '1' });
    sensitiveLevelList.value = response.data?.rows || [];
};
const getPublicSensitiveLevelId = () => {
    const publicLevel = sensitiveLevelList.value.find((item) => item.sensitiveLevel?.includes('公开'));
    return String(publicLevel?.id || '5');
};
const loadingList = ref(false);
const columnsByAssetTable = ref([]);
const tablesByDataSource = ref([]);

const localForm = ref({ ...props.form });

// 同步 props.form 到 localForm

getDatasourceList();
getSensitiveLevelList();

const getRows = (response) => {
    if (Array.isArray(response?.data?.rows)) {
        return response.data.rows;
    }
    if (Array.isArray(response?.data)) {
        return response.data;
    }
    if (Array.isArray(response?.rows)) {
        return response.rows;
    }
    return [];
};

const getColumnsByTable = async (table) => {
    if (table?.metadataSource === 'catalog' && table?.id) {
        const response = await listCatalogColumn({
            tableId: table.id,
            datasourceId: table.datasourceId || localForm.value.datasourceId,
            pageSize: 9999
        });
        return getRows(response).map((item) => ({
            ...item,
            columnLength: item.columnLength ?? item.columnPrecision
        }));
    }
    if (table?.id) {
        const response = await getDaDiscoveryColumnList({
            tableId: table.id,
            datasourceId: localForm.value.datasourceId
        });
        return getRows(response);
    }
    const response = await getColumnByAssetId({
        id: localForm.value.datasourceId,
        tableName: table?.tableName,
        projectId: userStore.projectId,
        projectCode: userStore.projectCode
    });
    return getRows(response);
};

const buildAssetRemark = (table, columns) => {
    const sourceLabel = table?.metadataSource === 'realtime' ? '数据源实时表' : table?.metadataSource === 'catalog' ? '元数据中心' : '元数据采集';
    const lines = [
        `${sourceLabel}表备注：${table?.tableComment || '-'}`,
        `${sourceLabel}数据量：${table?.dataCount ?? '-'}`,
        `${sourceLabel}字段数：${table?.fieldCount ?? (columns || []).length}`,
        '字段注释：'
    ];
    (columns || []).forEach((column) => {
        const typeInfo = [column.columnType, column.columnLength ? `长度${column.columnLength}` : '', column.columnScale ? `精度${column.columnScale}` : '']
            .filter(Boolean)
            .join('/');
        lines.push(`${column.columnName || '-'}：${column.columnComment || '-'}${typeInfo ? `（${typeInfo}）` : ''}`);
    });
    return lines.join('\n');
};

const isSameCatalogDatabase = (item) => {
    const dbName = localForm.value.dbname;
    const schemaName = localForm.value.schemaName;
    if (dbName && item.dbName && item.dbName !== dbName) {
        return false;
    }
    if (schemaName && item.schemaName && item.schemaName !== schemaName) {
        return false;
    }
    return true;
};

const dedupeCatalogTables = (rows) => {
    const tableMap = new Map();
    rows
        .filter(isSameCatalogDatabase)
        .forEach((item) => {
            const key = [item.dbName || '', item.schemaName || '', item.tableName || ''].join('.');
            if (!tableMap.has(key)) {
                tableMap.set(key, item);
            }
        });
    return Array.from(tableMap.values());
};

// 获取表列表
const getTablesByDatasourceId = async (id) => {
    loadingList.value = true;
    try {
        const catalogQuery = { pageSize: 9999, orderByColumn: 'id', isAsc: 'desc' };
        let catalogResponse = await listCatalogTable({
            ...catalogQuery,
            datasourceId: id,
            dbName: localForm.value.dbname,
            schemaName: localForm.value.schemaName
        });
        let catalogRows = dedupeCatalogTables(getRows(catalogResponse));
        if (!catalogRows.length && localForm.value.dbname) {
            catalogResponse = await listCatalogTable({
                ...catalogQuery,
                dbName: localForm.value.dbname,
                schemaName: localForm.value.schemaName
            });
            catalogRows = dedupeCatalogTables(getRows(catalogResponse));
        }
        const catalogTables = catalogRows.map((item) => ({
            ...item,
            dataCount: item.rowCount,
            fieldCount: item.columnCount,
            metadataSource: 'catalog'
        }));
        if (catalogTables.length) {
            tablesByDataSource.value = catalogTables;
            localForm.value.discoveryTaskId = tablesByDataSource.value[0]?.taskId;
            return;
        }

        const tableResponse = await getDaDiscoveryTableList({ datasourceId: id });
        const discoveryTables = getRows(tableResponse).map((item) => ({
            ...item,
            metadataSource: 'discovery'
        }));
        if (discoveryTables.length) {
            tablesByDataSource.value = discoveryTables;
            localForm.value.discoveryTaskId = tablesByDataSource.value[0]?.taskId;
            return;
        }

        const realtimeResponse = await getRealtimeTablesByDataSourceId({ datasourceId: id });
        tablesByDataSource.value = getRows(realtimeResponse).map((item) => ({
            ...item,
            tableComment: item.tableComment || item.remarks || item.comment,
            metadataSource: 'realtime'
        }));
        localForm.value.discoveryTaskId = tablesByDataSource.value[0]?.taskId;
    } finally {
        loadingList.value = false;
    }
};

// 数据源变化时
const handleDatasourceChange = async (id) => {
    const selected = createTypeList.value.find((item) => item.id == id);
    if (!selected) return;
    const { datasourceType, datasourceName, datasourceConfig, ip } = selected;
    const config = JSON.parse(datasourceConfig);

    Object.assign(localForm.value, {
        datasourceType,
        datasourceIp: ip,
        datasourceName,
        dbname: config.dbname,
        schemaName: config.sid,
        datasourceId: id
    });
    Object.assign(localForm.value, {
        tableName: '',
        tableComment: '',
        dataCount: undefined,
        fieldCount: undefined,
        discoveryTableId: undefined,
        tableId: undefined,
        discoveryTaskId: undefined,
        assetCreatedFlag: false,
        existingAssetId: undefined,
        assetColumnList: []
    });
    tablesByDataSource.value = [];
    columnsByAssetTable.value = [];
    emit('update:form', localForm.value);

    await getTablesByDatasourceId(id);
};

// 表变化时
const handleTableChange = async (tableName) => {
    const selected = tablesByDataSource.value.find((item) => item.tableName == tableName);
    if (!selected) return;
    localForm.value.tableName = tableName;
    localForm.value.tableComment = selected.tableComment;
    localForm.value.dataCount = selected.dataCount;
    localForm.value.fieldCount = selected.fieldCount;
    localForm.value.discoveryTableId = selected.metadataSource === 'discovery' ? selected.id : undefined;
    localForm.value.tableId = selected.metadataSource === 'catalog' ? selected.id : undefined;
    localForm.value.assetCreatedFlag = !!selected.assetCreatedFlag;
    localForm.value.existingAssetId = selected.assetId;
    const columns = await getColumnsByTable(selected);
    localForm.value.fieldCount = selected.fieldCount ?? columns.length;
    columnsByAssetTable.value = columns.map((item) => ({
        columnName: item.columnName,
        columnComment: item.columnComment,
        columnType: item.columnType,
        columnLength: item.columnLength,
        columnScale: item.columnScale,
        nullableFlag: item.nullableFlag,
        pkFlag: item.pkFlag,
        defaultValue: item.defaultValue,
        sensitiveLevelId: item.sensitiveLevelId || getPublicSensitiveLevelId()
    }));
    localForm.value.assetColumnList = columnsByAssetTable.value;
    localForm.value.remark = buildAssetRemark(selected, columnsByAssetTable.value);
    emit('update:form', localForm.value);
};
watch(
    columnsByAssetTable,
    (list) => {
        localForm.value.assetColumnList = list;
        emit('update:form', localForm.value);
    },
    { deep: true }
);
watchEffect(() => {
    localForm.value = { ...props.form };
});

// if (localForm.value?.datasourceId) {
//   getTablesByDatasourceId(localForm.value.datasourceId);
// }
</script>

