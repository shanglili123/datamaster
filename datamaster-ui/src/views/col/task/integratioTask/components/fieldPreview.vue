<template>
    <!-- 申请服务弹框 -->
    <a-modal title="步骤里的字段和其来源" v-model:open="open" width="800px" :draggable="true"
        :destroy-on-close="true" :footer="null">
        <div class="info-line">
            <span class="label">步骤名称：</span>
            <span class="value">{{ form.name || '-' }}</span>
        </div>
        <a-divider orientation="center">
            <span class="blue-text">{{ title }}</span>
        </a-divider>
        <a-table
          striped
          :scroll="{ y: '420px' }"
          :data-source="tableFields"
          :row-key="(record, index) => index"
          :pagination="false"
          size="middle"
        >
            <a-table-column title="序号" align="left" :width="80">
                <template #default="{ index }">
                    <span>{{ index + 1 }}</span>
                </template>
            </a-table-column>
            <a-table-column title="字段名称" align="left" data-index="columnName" ellipsis />
            <a-table-column title="字段类型" align="left" data-index="columnType" ellipsis>
                <template #default="{ record }">
                    {{ record.columnType || "-" }}
                </template>
            </a-table-column>
            <a-table-column title="字段长度" align="left" data-index="length" :width="70">
                <template #default="{ record }">
                    {{ record.length || "-" }}
                </template>
            </a-table-column>
            <a-table-column title="字段精度" align="left" data-index="precision" :width="70">
                <template #default="{ record }">
                    {{ record.precision || "-" }}
                </template>
            </a-table-column>
            <a-table-column title="步骤来源" align="left" data-index="source" ellipsis>
                <template #default="{ record }">
                    {{ record.source || "-" }}
                </template>
            </a-table-column>
        </a-table>
        <template #footer>
            <div class="dialog-footer">
                <a-button @click="cancel">关 闭</a-button>
                <!-- <a-button type="primary" @click="submitForm">确 定</a-button> -->
            </div>
        </template>
    </a-modal>
</template>

<script setup name="RpApplyDialog">

const emit = defineEmits(['setLoading']);
const open = ref(false);
const cancel = () => {
    open.value = false;
    reset();
};
const tableFields = ref({});

let title = ref()
let form = ref({});
const show = async (data, node, tit) => {
    console.log("🚀 ~ show ~ node:", node.data.name)
    form.value.name = node.data.name
    console.log("🚀 ~ show ~     form.value.name :", form.value.name)
    tableFields.value = data;
    title.value = tit
    open.value = true;

};
defineExpose({ show });
// #endregion
</script>
<style lang="scss" scoped>
.blue-text {
    color: #2666fb;
}

.info-line {
    // padding: 8px 12px;
    font-size: 14px;
}

.label {
    color: #909399;
    font-weight: bold;
    margin-right: 8px;
}

.value {
    color: #303133;
}
</style>

