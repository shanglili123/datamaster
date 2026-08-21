<template>
    <a-modal v-model:open="visibleDialog" class="medium-dialog" title="合表"
        :closable="false" :destroy-on-close="true" :mask-closable="false" :width="1200">
        <a-spin :spinning="loading">
        <a-form ref="dpModelRefs" :model="form" :label-col="{ style: { width: '110px' } }" @submit.prevent>

            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="节点名称" name="name"
                        :rules="[{ required: true, message: '请输入节点名称', trigger: 'change' }]">
                        <a-input v-model:value="form.name" placeholder="请输入节点名称" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="类型" name="typeName">
                        <a-input :value="form.taskParams?.typeName" placeholder="请输入类型" disabled />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="描述" name="description">
                        <a-input v-model:value="form.description" type="textarea" placeholder="请输入描述" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="数据源" name="taskParams.readerDatasource.datasourceId" :rules="[
                        {
                            required: true,
                            message: '请选择数据源',
                            trigger: 'change'
                        }
                    ]">
                        <a-select v-if="createTypeList.length || form.taskParams?.readerDatasource?.datasourceId"
                            :key="'ds-' + createTypeList.length" v-model:value="form.taskParams.readerDatasource.datasourceId"
                            placeholder="请选择数据源" @change="handleDatasourceChange" show-search>
                            <a-select-option v-for="dict in createTypeList" :key="dict.id" :label="dict.datasourceName"
                                :value="String(dict.id)">{{ dict.datasourceName }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="批次大小" name="taskParams.batchSize">
                        <a-input-number v-model:value="form.taskParams.batchSize" :min="1" :step="1"
                            style="width: 100%" placeholder="请输入批次大小" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-divider orientation="center">
                <span class="blue-text">源表</span>
            </a-divider>
            <div class="stream-field-title">
                <span>合并的源表(多个结构相同的表)</span>
                <a-button size="small" type="primary" @click="addSourceTable">添加源表</a-button>
            </div>
            <a-table :data-source="form.taskParams?.sourceTables" :columns="sourceTableColumns"
                :pagination="false" :scroll="{ y: 200 }">
                <template #bodyCell="{ column, record, index }">
                    <template v-if="column.dataIndex === 'index'">
                        <span>{{ index + 1 }}</span>
                    </template>
                    <template v-else-if="column.dataIndex === 'table_name'">
                        <a-input v-model:value="record.table_name" placeholder="请输入表名" />
                    </template>
                    <template v-else-if="column.dataIndex === 'where'">
                        <a-input v-model:value="record.where" placeholder="请输入过滤条件，不需要 WHERE 关键字" />
                    </template>
                    <template v-else-if="column.key === 'actions'">
                        <a-button type="link" danger size="small" @click="removeSourceTable(index)">删除</a-button>
                    </template>
                </template>
            </a-table>
            <a-divider orientation="center">
                <span class="blue-text">输出字段</span>
            </a-divider>
            <div class="stream-field-title">
                <span>输出字段列表</span>
                <a-button size="small" type="primary" @click="addField">添加字段</a-button>
            </div>
            <a-table :data-source="form.taskParams?.tableFields" :columns="fieldColumns"
                :pagination="false" :scroll="{ y: 200 }">
                <template #bodyCell="{ column, record, index }">
                    <template v-if="column.dataIndex === 'index'">
                        <span>{{ index + 1 }}</span>
                    </template>
                    <template v-else-if="column.dataIndex === 'columnName'">
                        <a-input v-model:value="record.columnName" placeholder="字段名，如 id" />
                    </template>
                    <template v-else-if="column.key === 'actions'">
                        <a-button type="link" danger size="small" @click="removeField(index)">删除</a-button>
                    </template>
                </template>
            </a-table>

        </a-form>
        </a-spin>
        <template #footer>
            <div style="text-align: right">
                <a-button @click="closeDialog">取消</a-button>
                <a-button type="primary" :loading="loading" @click="saveData">确定</a-button>
            </div>
        </template>
    </a-modal>
</template>
<script setup>
import { message } from 'ant-design-vue'

import {
    getLocalNodeUniqueKey as getNodeUniqueKey
} from '@/api/col/task/index.js';

import {
    listDaDatasource,
    getDaDatasource
} from '@/api/ast/dataSource/dataSource.js';

const { proxy } = getCurrentInstance();

import useUserStore from '@/store/system/user.js';
const userStore = useUserStore();
const props = defineProps({
    visible: { type: Boolean, default: true },
    title: { type: String, default: '表单标题' },
    currentNode: { type: Object, default: () => ({}) }
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
let dpModelRefs = ref();
let form = ref({});
const createTypeList = ref([]); // 数据源列表

const sourceTableColumns = [
    { title: '序号', dataIndex: 'index', width: 60, align: 'left' },
    { title: '表名', dataIndex: 'table_name', align: 'left', ellipsis: true },
    { title: '过滤条件', dataIndex: 'where', align: 'left', ellipsis: true },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 80 },
];

const fieldColumns = [
    { title: '序号', dataIndex: 'index', width: 60, align: 'left' },
    { title: '输出字段', dataIndex: 'columnName', align: 'left', ellipsis: true },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 80 },
];

const getNodeData = () => props.currentNode?.getProp?.("data") || props.currentNode?.data || {};

const getDatasourceId = (ds) => ds?.datasourceId ?? ds?.datasource_id ?? ds?.id ?? ds?.sourceId;
const normalizeDatasourceType = (type) => String(type || '').replace(/[\s_-]/g, '').toLowerCase();

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

// 获取数据源列表
const getDatasourceList = async () => {
    try {
        loading.value = true;
        const response = await listDaDatasource({
            pageSize: 9999,
            spaceCode: userStore.spaceCode,
            spaceId: userStore.spaceId,
            datasourceType: "DM8,Oracle11,MySql,Oracle,Kingbase8,Doris,ClickHouse,Hive,MongoDB,Elasticsearch,SQL_Server,SQL_Server2008,PostgreSQL",
        });
        createTypeList.value = (response.data.rows || []).filter(
            (item) => normalizeDatasourceType(item.datasourceType) !== 'kafka'
        );
        ensureSelectedDatasourceOption();
    } finally {
        loading.value = false;
    }
};

// 处理数据源变化
const handleDatasourceChange = (value) => {
    if (!value) {
        return;
    }
    getDaDatasource(value).then((response) => {
        let { datasourceType, datasourceConfig, ip, port, id, datasourceName } = response.data || {};
        let dbname = '';
        try {
            dbname = JSON.parse(datasourceConfig).dbname;
        } catch (error) {
            dbname = '';
        }
        form.value.taskParams.readerDatasource = {
            datasourceType,
            datasourceConfig,
            ip,
            port,
            dbname,
            datasource_id: String(id),
            datasourceId: String(id),
            datasourceName
        };
    }).catch(() => {
        message.error('获取数据源信息失败');
    });
};

const addSourceTable = () => {
    form.value.taskParams.sourceTables.push({ table_name: '', where: '' });
};
const removeSourceTable = (index) => {
    form.value.taskParams.sourceTables.splice(index, 1);
};
const addField = () => {
    form.value.taskParams.tableFields.push({ columnName: '' });
};
const removeField = (index) => {
    form.value.taskParams.tableFields.splice(index, 1);
};

const off = () => {
    proxy.resetForm('dpModelRefs');
};

// 保存数据
const saveData = async () => {
    try {
      // 手动检查关键必填字段
      if (!form.value?.taskParams?.readerDatasource?.datasourceId) {
        return proxy.$message.warning('请选择源数据库连接');
      }
      if (!form.value?.taskParams?.asset_id) {
        return proxy.$message.warning('请选择表');
      }
      // 过滤掉表名为空的源表
      form.value.taskParams.sourceTables = (form.value.taskParams.sourceTables || []).filter(
        (item) => item && String(item.table_name || '').trim() !== ''
      );
      if (!form.value.taskParams.sourceTables.length) {
        return proxy.$message.warning('请至少添加一个源表');
        }
        // 过滤掉输出字段为空的字段
        form.value.taskParams.tableFields = (form.value.taskParams.tableFields || []).filter(
            (item) => item && String(item.columnName || '').trim() !== ''
        );
        if (!form.value.taskParams.tableFields.length) {
            return proxy.$message.warning('校验未通过，请选择输出字段');
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
        taskParams.columns = taskParams.tableFields.map(({ columnName }) => columnName);

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
        return {};
    }
    try {
        return JSON.parse(JSON.stringify(data));
    } catch (e) {
        return {};
    }
}
// 监听属性变化
watch(
    () => [props.visible, props.currentNode?.id],
    ([visible]) => {
        if (visible) {
            // 数据源
            getDatasourceList();
            const nodeData = props.currentNode?.getProp?.("data") || props.currentNode?.data || {};
            const copy = deepCopy(nodeData);
            // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
            Object.keys(form.value).forEach(k => { delete form.value[k]; });
            Object.assign(form.value, copy);
            form.value.taskParams = form.value.taskParams || {};
            normalizeReaderDatasource(form.value.taskParams);
            form.value.taskParams.batchSize = form.value.taskParams.batchSize || 1000;
            form.value.taskParams.sourceTables = Array.isArray(form.value.taskParams.sourceTables)
                ? form.value.taskParams.sourceTables
                : [{ table_name: '', where: '' }];
            form.value.taskParams.tableFields = Array.isArray(form.value.taskParams.tableFields)
                ? form.value.taskParams.tableFields
                : [];
            ensureSelectedDatasourceOption();
            // 等待 DOM 更新后清除旧的校验状态，避免残留红字
            nextTick(() => {
                dpModelRefs.value?.clearValidate();
                
            });
        } else {
            off();
        }
    },
    { immediate: true }
);
</script>
<style scoped lang="less">
.blue-text {
    color: #2666fb;
}

.stream-field-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
}
</style>
