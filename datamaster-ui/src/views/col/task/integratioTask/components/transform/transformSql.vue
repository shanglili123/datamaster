<template>
    <el-dialog v-model="visibleDialog" :draggable="true" class="medium-dialog" :title="currentNode?.data?.name"
        showCancelButton :show-close="false" destroy-on-close
>
        <template #header>
            <div class="justify">
                <span class="el-dialog__title">{{ currentNode?.data?.name }}</span>
                <el-tooltip effect="light" content="编写自定义SQL作为FlinkX转换逻辑(transformSql)" placement="top">
                    <el-icon class="tip-icon">
                        <InfoFilled />
                    </el-icon>
                </el-tooltip>
            </div>
        </template>
        <el-form ref="dpModelRefs" :model="form" label-width="110px" @submit.prevent v-loading="loading"
            :disabled="info"
>
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="节点名称" prop="name"
                        :rules="[{ required: true, message: '请输入节点名称', trigger: 'change' }]"
>
                        <el-input v-model="form.name" placeholder="请输入节点名称" />
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="类型" prop="typeName">
                        <el-select v-model="form.taskParams.typeName" placeholder="请输入类型" filterable disabled>
                            <el-option v-for="dict in typeList" :key="dict.value" :label="dict.label"
                                :value="dict.value"
/>
                        </el-select>
                    </el-form-item>
                </el-col>
            </el-row>

            <el-row :gutter="20">
                <el-col :span="24">
                    <el-form-item label="描述" prop="description">
                        <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
                    </el-form-item>
                </el-col>
            </el-row>

            <el-divider content-position="center">
                <span class="blue-text">源表字段</span>
            </el-divider>
            <el-row :gutter="20">
                <el-col :span="24">
                    <div v-if="sourceTableName" class="mb10">
                        <span class="info-label">源表：</span>
                        <el-tag type="info">{{ sourceTableName }}</el-tag>
                        <el-button link type="primary" @click="generateSql">重新生成</el-button>
                    </div>
                    <el-table stripe height="160px" :data="inputFields" v-loading="loadingList" max-height="200">
                        <el-table-column label="字段名称" align="left" prop="columnName" />
                        <el-table-column label="类型" align="left" prop="columnType" width="120" />
                    </el-table>
                </el-col>
            </el-row>

            <el-divider content-position="center">
                <span class="blue-text">自定义 SQL</span>
            </el-divider>
            <el-row :gutter="20">
                <el-col :span="24">
                    <el-form-item label="SQL" prop="taskParams.sql">
                        <el-input v-model="form.taskParams.sql" type="textarea" :rows="10"
                            placeholder="SELECT `col1`, `col2`, ... FROM `source_table` WHERE ..."
/>
                    </el-form-item>
                </el-col>
            </el-row>
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
import { defineProps, defineEmits, ref, computed, watch, getCurrentInstance } from "vue";
import { typeList } from "@/utils/graph.js";
import { getLocalNodeUniqueKey as getNodeUniqueKey } from "@/api/col/task/index.js";
import useUserStore from "@/store/system/user.js";
import { getParentNode } from "@/views/col/utils/opBase.js";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();

const props = defineProps({
    visible: { type: Boolean, default: true },
    title: { type: String, default: "表单标题" },
    currentNode: { type: Object, default: () => ({}) },
    info: { type: Boolean, default: false },
    graph: { type: Object, default: () => ({}) },
});

const emit = defineEmits(["update", "confirm"]);

const visibleDialog = computed({
    get() { return props.visible; },
    set(newValue) { emit("update", newValue); },
});

let inputFields = ref([]);
let sourceTableName = ref("");
let loading = ref(false);
let loadingList = ref(false);
let dpModelRefs = ref();
let form = ref({});

function generateSql() {
    const tableName = sourceTableName.value;
    const fields = inputFields.value;
    if (!fields || fields.length === 0) {
        form.value.taskParams.sql = "SELECT *" + (tableName ? ` FROM \`${tableName}\`` : "");
        return;
    }
    const colList = fields.map(f => `\`${f.columnName}\``).join(",\n    ");
    form.value.taskParams.sql = "SELECT\n    " + colList + (tableName ? `\nFROM \`${tableName}\`` : "");
}

function extractSourceTableName() {
    if (!props.graph || !props.currentNode) return "";
    try {
        const parentNode = getParentNode(props.currentNode, props.graph);
        if (!parentNode) return "";
        const taskParams = parentNode.data?.taskParams || {};
        return taskParams.table_name || taskParams.asset_id || "";
    } catch {
        return "";
    }
}

const off = () => {
    proxy.resetForm("dpModelRefs");
    inputFields.value = [];
    sourceTableName.value = "";
};

const saveData = async () => {
    try {
        const valid = await dpModelRefs.value.validate();
        if (!valid) return;
        if (!form.value.code) {
            loading.value = true;
            const response = await getNodeUniqueKey({
                spaceCode: userStore.spaceCode || "133545087166112",
                spaceId: userStore.spaceId,
            });
            loading.value = false;
            form.value.code = response.data;
        }
        const taskParams = form.value?.taskParams || {};
        // 保持原有的 outputFields 不变，避免触发 outputsChanged 重置下游节点
        const originalData = deepCopy(props.currentNode?.data || {});
        taskParams.outputFields = originalData.taskParams?.outputFields || [];
        emit("confirm", form.value);
    } catch (error) {
        console.error("保存数据失败:", error);
        loading.value = false;
    }
};

const closeDialog = () => {
    off();
    emit("update", false);
};

function deepCopy(data) {
    if (data === undefined || data === null) return {};
    try { return JSON.parse(JSON.stringify(data)); } catch { return {}; }
}

watch(
    () => props.visible ? props.currentNode : null,
    (newNode) => {
        if (!props.visible || !newNode) {
            off();
            return;
        }
        form.value = deepCopy(newNode?.data || {});
        if (!form.value.taskParams) {
            form.value.taskParams = {};
        }
        inputFields.value = newNode?.data?.taskParams?.inputFields
            || newNode?.data?.inputFields
            || [];
        sourceTableName.value = extractSourceTableName();
        const sql = form.value.taskParams.sql;
        if (!sql || /^SELECT\s+\*/i.test(sql)) {
            generateSql();
        }
    },
    { immediate: true }
);
</script>

<style scoped lang="less">
.blue-text {
    color: #2666fb;
}
.info-label {
    font-weight: 500;
    margin-right: 8px;
}
.mb10 {
    margin-bottom: 10px;
}
</style>
