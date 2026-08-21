<template>
    <!-- 拆表输出 -->
    <a-modal v-model:open="visibleDialog" :draggable="true" title="拆表" :closable="false" :destroy-on-close="true"
        :mask-closable="false" class="medium-dialog" :width="800" :ok-text="'确定'" :cancel-text="'取消'"
        :confirm-loading="loading" @ok="saveData" @cancel="closeDialog">
        <a-spin :spinning="loading">
            <a-form ref="dpModelRefs" :model="form" :label-col="{ style: { width: '110px' } }" @submit.prevent>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="节点名称" name="name" :rules="[
                            { required: true, message: '请输入节点名称', trigger: 'change' },
                        ]">
                            <a-input v-model:value="form.name" placeholder="请输入节点名称" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="节点类型" name="typeName">
                            <a-input v-model:value="form.taskParams.typeName" disabled />
                        </a-form-item>
                    </a-col>
                </a-row>

                <div class="h2-title">目标表配置</div>
                <div style="color: #999; font-size: 12px; margin-bottom: 8px">单个输入流将按以下目标表拆分，每个目标表生成一个独立的数据库输出</div>

                <a-collapse v-model:activeKey="activeKeys" :bordered="false">
                    <a-collapse-panel v-for="(row, index) in form.taskParams.targetTables" :key="index"
                        :header="'目标表 ' + (index + 1) + (row.tableName ? '：' + row.tableName : '')">
                        <template #extra>
                            <a-button type="link" danger size="small" @click.stop="removeTargetTable(index)">删除</a-button>
                        </template>
                        <a-row :gutter="20">
                            <a-col :span="12">
                                <a-form-item label="目标数据源" required>
                                    <a-select v-model:value="row.writerDatasource.datasourceId" placeholder="请选择目标数据源"
                                        show-search @change="handleRowDatasourceChange(index, $event)">
                                        <a-select-option v-for="dict in createTypeList" :key="dict.id"
                                            :label="dict.datasourceName" :value="String(dict.id)">{{ dict.datasourceName }}</a-select-option>
                                    </a-select>
                                </a-form-item>
                            </a-col>
                            <a-col :span="12">
                                <a-form-item label="目标表名" required>
                                    <a-input v-model:value="row.tableName" placeholder="请输入目标表名" />
                                </a-form-item>
                            </a-col>
                        </a-row>
                        <a-row :gutter="20">
                            <a-col :span="12">
                                <a-form-item label="批次大小">
                                    <a-input-number v-model:value="row.batchSize" :min="1" placeholder="1000" style="width: 100%" />
                                </a-form-item>
                            </a-col>
                        </a-row>
                    </a-collapse-panel>
                </a-collapse>

                <a-button type="dashed" block style="margin-top: 8px" @click="addTargetTable">添加目标表</a-button>
            </a-form>
        </a-spin>
    </a-modal>
</template>
<script setup name="TableSplitOutputForm">
import { message } from 'ant-design-vue';

import {
    listDaDatasource,
    getDaDatasource,
} from "@/api/ast/dataSource/dataSource.js";

import {
    getLocalNodeUniqueKey as getNodeUniqueKey,
} from "@/api/col/task/index.js";
const { proxy } = getCurrentInstance();
import useUserStore from "@/store/system/user.js";
const userStore = useUserStore();

const props = defineProps({
    visible: { type: Boolean, default: true },
    title: { type: String, default: "表单标题" },
    currentNode: { type: Object, default: () => ({}) },
    info: { type: Boolean, default: false },
    graph: { type: Object, default: null },
});
const emit = defineEmits(["update", "confirm"]);
const visibleDialog = computed({
    get() {
        return props.visible;
    },
    set(newValue) {
        emit("update", newValue);
    },
});

// 变量定义
let loading = ref(false);
let dpModelRefs = ref();
let createTypeList = ref([]); // 数据源列表
let activeKeys = ref([]); // 展开的目标表配置项
let form = ref({
    name: "",
    taskParams: {
        typeName: "拆表",
        targetTables: [],
    },
});

// 兼容不同字段命名的数据源 id 获取
const getDatasourceId = (ds) => ds?.datasourceId ?? ds?.datasource_id ?? ds?.id ?? ds?.sourceId;

// 创建一个空的目标表行(与 DB_WRITER 的 taskParams 同构)
const createEmptyRow = () => ({
    writerDatasource: {
        datasourceType: "",
        datasourceConfig: "",
        ip: "",
        port: "",
        dbname: "",
        target_asset_id: "",
        datasourceId: "",
        datasourceName: "",
    },
    target_datasource_id: "",
    target_table_name: "",
    target_asset_id: "",
    tableName: "",
    batchSize: 1000,
    writeModeType: 2,
    selectedColumns: [],
    preSql: "",
    postSql: "",
});

// 获取数据源列表
const getDatasourceList = async () => {
    try {
        loading.value = true;
        const response = await listDaDatasource({
            spaceCode: userStore.spaceCode,
            spaceId: userStore.spaceId,
            datasourceType: "DM8,Oracle11,MySql,Oracle,Kingbase8,Doris,ClickHouse,Hive,MongoDB,Elasticsearch,SQL_Server,SQL_Server2008,PostgreSQL",
            pageSize: 9999,
        });
        createTypeList.value = response.data.rows || [];
    } finally {
        loading.value = false;
    }
};

// 添加目标表
const addTargetTable = () => {
    const targetTables = form.value.taskParams.targetTables;
    const lastRow = targetTables[targetTables.length - 1];
    const row = createEmptyRow();
    // 复用前一行已选择的数据源
    if (getDatasourceId(lastRow?.writerDatasource)) {
        row.writerDatasource = deepCopy(lastRow.writerDatasource);
        row.target_datasource_id = lastRow.target_datasource_id || getDatasourceId(lastRow.writerDatasource);
    }
    targetTables.push(row);
    activeKeys.value.push(targetTables.length - 1);
};

// 删除目标表
const removeTargetTable = (index) => {
    form.value.taskParams.targetTables.splice(index, 1);
};

// 处理目标表数据源变化,获取数据源详细信息
const handleRowDatasourceChange = (index, value) => {
    const row = form.value.taskParams.targetTables[index];
    if (!row) {
        return;
    }
    const selectedDatasource = createTypeList.value.find((item) => item.id == value);
    if (!selectedDatasource) {
        return;
    }
    getDaDatasource(selectedDatasource.id).then((response) => {
        // 行已被删除时不再回填
        if (form.value.taskParams.targetTables[index] !== row) {
            return;
        }
        let { datasourceType, datasourceConfig, ip, port, id, datasourceName } = response.data;
        let code = JSON.parse(datasourceConfig);
        row.writerDatasource = {
            datasourceType,
            datasourceConfig,
            ip,
            port,
            dbname: code.dbname,
            target_asset_id: String(id),
            datasourceId: String(id),
            datasourceName,
        };
        row.target_datasource_id = String(id);
    });
};

const off = () => {
    proxy.resetForm("dpModelRefs");
    createTypeList.value = [];
    activeKeys.value = [];
    form.value = {
        name: "",
        taskParams: {
            typeName: "拆表",
            targetTables: [],
        },
    };
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

// 归一化已保存的目标表配置,补齐 DB_WRITER 所需字段
const normalizeTargetTables = () => {
    const targetTables = form.value.taskParams.targetTables;
    if (!Array.isArray(targetTables) || !targetTables.length) {
        form.value.taskParams.targetTables = [createEmptyRow()];
        return;
    }
    for (const row of targetTables) {
        if (!row.writerDatasource || typeof row.writerDatasource !== "object") {
            row.writerDatasource = {};
        }
        const savedId = getDatasourceId(row.writerDatasource);
        if (savedId !== undefined && savedId !== null && savedId !== "") {
            row.writerDatasource.datasourceId = String(savedId);
            row.writerDatasource.datasource_id = String(savedId);
        }
        if (!row.tableName && row.target_table_name) {
            row.tableName = row.target_table_name;
        }
        if (!row.tableName && row.target_asset_id) {
            row.tableName = row.target_asset_id;
        }
        if (row.batchSize === undefined || row.batchSize === null || row.batchSize === "") {
            row.batchSize = 1000;
        }
        if (!row.writeModeType) {
            row.writeModeType = 2;
        }
        if (!Array.isArray(row.selectedColumns)) {
            row.selectedColumns = [];
        }
    }
};

// 保存数据
const saveData = async () => {
    try {
        await nextTick();
        // 手动校验（避免 a-form dot-notation path 校验失效）
        const targetTables = form.value.taskParams.targetTables || [];
        if (!targetTables.length) {
            message.warning("请至少添加一个目标表");
            return;
        }
        const hasIncomplete = targetTables.some(
            (row) => !getDatasourceId(row.writerDatasource) || !String(row.tableName || "").trim()
        );
        if (hasIncomplete) {
            message.warning("请完善目标表配置");
            return;
        }

        // 没有 code 时生成唯一 code
        if (!form.value.code) {
            loading.value = true;
            try {
                const { data } = await getNodeUniqueKey({
                    spaceCode: userStore.spaceCode || "133545087166112",
                    spaceId: userStore.spaceId,
                });
                form.value.code = data;
            } finally {
                loading.value = false;
            }
        }

        // 归一化为完整的 DB_WRITER 配置
        for (const row of targetTables) {
            const tableName = String(row.tableName || "").trim();
            row.target_table_name = tableName;
            row.target_asset_id = tableName;
            if (row.batchSize === undefined || row.batchSize === null || row.batchSize === "") {
                row.batchSize = 1000;
            }
            if (!row.writeModeType) {
                row.writeModeType = 2;
            }
        }

        emit("confirm", form.value);
    } catch (error) {
        console.error("保存数据失败:", error);
        loading.value = false;
    }
};

const closeDialog = () => {
    off();
    // 关闭对话框
    emit("update", false);
};

// 监听属性变化
watch(
    () => [props.visible, props.currentNode?.id],
    ([visible]) => {
        if (!visible) {
            off();
            return;
        }
        getDatasourceList();

        const nodeData = props.currentNode?.getProp?.("data") || props.currentNode?.data || {};
        const copy = deepCopy(nodeData);
        // 原地更新而非替换 ref，保持 a-form 内部字段注册的响应式代理不被断开
        Object.keys(form.value).forEach(k => { delete form.value[k]; });
        Object.assign(form.value, copy);
        form.value.taskParams = form.value.taskParams || {};
        form.value.taskParams.typeName = form.value.taskParams.typeName || "拆表";
        normalizeTargetTables();
        activeKeys.value = form.value.taskParams.targetTables.length ? [0] : [];
        // 等待 DOM 更新后清除旧的校验状态，避免残留红字
        nextTick(() => {
            dpModelRefs.value?.clearValidate();

        });
    }
);
</script>
