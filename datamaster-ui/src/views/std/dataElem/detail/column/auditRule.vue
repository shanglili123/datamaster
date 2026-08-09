<template>
    <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
            <a-col :span="1.5">
                <a-button type="primary" @click="openRuleSelector(undefined)"
                    @mousedown="(e) => e.preventDefault()">
                    <i class="iconfont-mini icon-xinzeng mr5"></i>关联
                </a-button>
            </a-col>
        </a-row>
        <div class="justify-end top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </div>
    </div>
    <a-table striped :loading="loading" :data-source="dataList" :pagination="false" :scroll="{ y: 400 }" :columns="tableColumns">
        <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
                <span>{{ (queryParams.pageNum - 1) * queryParams.pageSize + index + 1 }}</span>
            </template>
            <template v-if="column.dataIndex === 'name'">
                {{ record.name || '-' }}
            </template>
            <template v-if="column.dataIndex === 'ruleName'">
                {{ record.ruleName || '-' }}
            </template>
            <template v-if="column.dataIndex === 'ruleDescription'">
                {{ record.ruleDescription || '-' }}
            </template>
            <template v-if="column.dataIndex === 'dimensionType'">
                <dict-tag :options="att_rule_audit_q_dimension" :value="record.dimensionType" />
            </template>
            <template v-if="column.dataIndex === 'createBy'">
                {{ record.createBy || "-" }}
            </template>
            <template v-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
            </template>
            <template v-if="column.dataIndex === 'updateTime'">
                <span>{{ parseTime(record.updateTime, '{y}-{m}-{d} {h}:{i}') || '-' }}</span>
            </template>
            <template v-if="column.dataIndex === 'status'">
                {{ record.status == '1' ? '上线' : '下线' }}
            </template>
            <template v-if="column.key === 'actions'">
                <a-button type="link" size="small"
                    @click="openRuleDialog(record, index + 1)">修改</a-button>
                <a-button type="link" danger size="small" @click="handleRuleDelete(record)">删除</a-button>
            </template>
        </template>
    </a-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    <RuleSelectorDialog ref="ruleSelectorDialog" @confirm="RuleSelectorconfirm" :dppQualityTaskObjSaveReqVO="[]"
        :type="3" />
</template>

<script setup name="dataElemAudit">
import { message } from 'ant-design-vue'
import { ref, watch } from 'vue';

const { proxy } = getCurrentInstance();

const props = defineProps({
    dataElemId: {
        required: true,
        type: String
    },
    ruleType: {
        required: true,
        type: String,
        default: 1
    }
});


import { listDpDataElemRuleRel, dpDataElemRuleRel, putDpDataElemRuleRel, DlEPutDpDataElemRuleRel } from '@/api/std/dataElem/dataElem';

import RuleSelectorDialog from '@/views/ast/quality/qualityTask/components/ruleBase.vue';
const { att_rule_audit_q_dimension, att_rule_audit_type, att_rule_level, } = proxy.useDict(
    'att_rule_audit_q_dimension',
    'att_rule_audit_type',
    'att_rule_level'
);

const loading = ref(true);
const showSearch = ref(true);
const defaultSort = ref({ prop: 'createTime', order: 'desc' });
const total = ref(0);
const dataList = ref([]);
const typeName = ref(null);
const tableColumns = [
    { title: '编号', key: 'index', align: 'left', width: 60 },
    { title: '稽查名称', dataIndex: 'name', align: 'left', width: 200 },
    { title: '稽查规则', dataIndex: 'ruleName', align: 'left', width: 200 },
    { title: '描述', dataIndex: 'ruleDescription', align: 'left', ellipsis: true },
    { title: '质量维度', dataIndex: 'dimensionType', align: 'left', width: 100 },
    { title: '创建人', dataIndex: 'createBy', align: 'left', width: 120, ellipsis: true },
    { title: '创建时间', dataIndex: 'createTime', align: 'left', width: 150 },
    { title: '更新时间', dataIndex: 'updateTime', align: 'left', width: 300 },
    { title: '状态', dataIndex: 'status', align: 'left' },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 180 },
];

const data = reactive({
    form: {
        name: null,
        level: null,
        type: null,
        qualityDim: null,
    },
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        dataElemId: null,
        type: 1
    },
    defaultProps: {
        children: 'children',
        label: 'name',
        isLeaf: 'isLeaf'
    }
});
const { queryParams, form, defaultProps } = toRefs(data);
const open = ref(false);
const title = ref('');
const sourceName = ref('');
queryParams.value.dataElemId = props.dataElemId;

watch(sourceName, (val) => {
    proxy.$refs.tree.filter(val);
});

let ruleSelectorDialog = ref()
const openRuleSelector = (row) => {
    ruleSelectorDialog.value.openDialog(undefined, undefined, undefined);
}
const openRuleDialog = (row, index, falg) => {
    console.log("🚀 ~ openRuleDialog ~ row33:", row)
    ruleSelectorDialog.value.openDialog({ ...row, ruleConfig: row.rule, }, index, falg);
};
function RuleSelectorconfirm(obj, mode) {
    loading.value = true;
    let api = obj?.id ? putDpDataElemRuleRel : dpDataElemRuleRel;
    api({ ...obj, dataElemId: props.dataElemId, type: 1, ruleId: obj.ruleCode, }).then((res) => {
        console.log("🚀 ~ RuleSelectorconfirm ~ obj:", obj)
        if (res.code == 200) {
            proxy.$message.success(res.msg);
            open.value = false;
            getList();
        }
    });
    loading.value = false;
    ruleSelectorDialog.value.closeDialog();
}







//查询数据元列表
function getList() {
    loading.value = true;
    listDpDataElemRuleRel(queryParams.value).then((response) => {
        dataList.value = response.data.rows;
        total.value = response.data.total;
        loading.value = false;
    });
}

/** 删除按钮操作 */
function handleRuleDelete(row) {
    const _ids = row.id;
    proxy.$modal
        .confirm('是否确认删除关联稽查规则数据？')
        .then(function () {
            return DlEPutDpDataElemRuleRel(_ids);
        })
        .then(() => {
            getList();
            proxy.$modal.msgSuccess("删除成功");
        })
        .catch(() => { });
}



getList();
</script>
<style lang="scss" scoped>
.base-info {
    margin-top: 5px;

    .type-name {
        color: #000;
        font-size: 18px;
        font-weight: bold;
    }

    .base-content {
        margin-top: 20px;
        padding-left: 25px;

        :deep(.el-form-item__label) {
            padding: 0 0 0 0 !important;
        }

        :deep(.el-form-item) {
            margin-bottom: 5px;
        }
    }
}

.hint-div {
    margin: 10px 0px 20px 20px;
    border-top: 1px solid rgba(204, 204, 204, 0.5);
    border-right: 1px solid rgba(204, 204, 204, 0.5);
    border-bottom: 1px solid #e5f1f8;
    border-left: 1px solid #e5f1f8;
    border-radius: 2px;
    padding: 10px;
    box-shadow: -1px 1px 2px #e5f1f8;
    display: flex;
    align-items: center;

    span {
        margin-left: 5px;
    }
}

// 设置只有叶子节点有多选框
:deep(.el-tree-node) {
    .is-leaf+.el-checkbox .el-checkbox__inner {
        display: inline-block !important;
    }

    .el-checkbox__input>.el-checkbox__inner {
        display: none;
    }
}
</style>

