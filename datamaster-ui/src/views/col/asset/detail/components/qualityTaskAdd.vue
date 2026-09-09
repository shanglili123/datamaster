<template>
    <!-- 资产质量的弹窗 -->
    <a-modal v-model:open="visible" :title="title" class="medium-dialog" @close="handleClose" destroy-on-close :width="1200">
        <a-spin :spinning="loadingInstance">
        <div ref="app-container">
            <!--            <div class="pagecont-top" v-show="showSearch" style="padding-bottom: 15px">-->
            <!--                <div class="infotop">-->
            <!--                    <div class="main">-->
            <a-spin :spinning="loadingList">
                <!-- <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline"
                                :label-col="{ style: { width: '75px' } }" @submit.prevent>
                                <a-form-item label="规则名称" name="name">
                                    <a-input class="el-form-input-width" v-model:value="queryParams.name"
                                        placeholder="请输入规则名称" allow-clear @pressEnter="handleQuery" />
                                </a-form-item>
                                <a-form-item label="质量维度" name="dimensionType">
                                    <a-select v-model:value="queryParams.dimensionType" placeholder="请选择质量维度"
                                        style="width: 210px;">
                                        <a-select-option v-for="dict in att_rule_audit_q_dimension" :key="dict.value"
                                            :label="dict.label" :value="dict.value">{{ dict.label }}</a-select-option>
                                    </a-select>
                                </a-form-item>

                                <a-form-item label="状态" name="publishStatus">
                                    <a-select v-model:value="queryParams.publishStatus" placeholder="请选择状态" allow-clear
                                        class="el-form-input-width">
                                        <a-select-option label="上线" value="online" />
                                        <a-select-option label="下线" value="offline" />
                                    </a-select>
                                </a-form-item>
                                <a-form-item>
                                    <a-button type="primary" @click="handleQuery"
                                        @mousedown="(e) => e.preventDefault()">
                                        <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
                                    </a-button>
                                    <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
                                        <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
                                    </a-button>
                                </a-form-item>
                            </a-form> -->
                <div class="justify-between mb15">
                    <a-row :gutter="15" class="btn-style">
                        <a-col :span="1.5">
                            <a-button type="primary" :icon="h(PlusOutlined)" @click="openRuleSelector(undefined)"
                                v-if="!route.query.info"
>新增</a-button>
                        </a-col>
                        <a-col :span="1.5">
                            <a-tooltip title="会自动获取资产关联的数据元中的稽查规则" placement="top">
                                <a-button @click="selectInspectionRule(undefined)"
                                    v-if="!route.query.info"
>
                                    <ReloadOutlined style="margin-right: 4px;" />
                                    获取稽查规则
                                </a-button>
                            </a-tooltip>
                        </a-col>
                    </a-row>
                </div>

                <a-table striped :data-source="dppQualityTaskEvaluateSaveReqVO" :pagination="false" :scroll="{ y: 550 }" :columns="tableColumns">
                    <template #bodyCell="{ column, record, index }">
                        <template v-if="column.dataIndex === 'id'">
                            {{ record.id || '-' }}
                        </template>
                        <template v-if="column.dataIndex === 'name'">
                            {{ record.name || '-' }}
                        </template>
                        <template v-if="column.dataIndex === 'evaColumn'">
                            {{ record.evaColumn || '-' }}
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
                        <template v-if="column.dataIndex === 'status'">
                            {{ record.status == '1' ? '上线' : '下线' }}
                        </template>
                        <template v-if="column.key === 'actions'">
                            <a-button type="link" size="small"
                                @click="openRuleDialog(record, index + 1, true)"
>查看</a-button>
                            <a-button type="link" size="small"
                                @click="openRuleDialog(record, index + 1)"
>修改</a-button>
                            <a-button type="link" danger size="small"
                                @click="handleRuleDelete(index + 1)"
>删除</a-button>
                        </template>
                    </template>
                </a-table>
                <!--                        </div>-->
                <!--                    </div>-->
                <!--                </div>-->
            </a-spin>
        </div>
        </a-spin>
        <RuleSelectorDialog ref="ruleSelectorDialog" @confirm="RuleSelectorconfirm" v-if="visible"
            :dppQualityTaskObjSaveReqVO="dppQualityTaskObjSaveReqVO" :type="2" :tableName="formData?.tableName"
/>
        <template #footer>
            <a-button @click="handleClose">取消</a-button>
            <a-button type="primary" @click="submitForm" :loading="loadingOptions.loading">
                确定
            </a-button>
        </template>
    </a-modal>
</template>

<script setup name="qualityTask">
import { message } from 'ant-design-vue'
import { ref, reactive, toRefs, onMounted, computed } from 'vue';
import { h } from 'vue';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue';

import { useRoute, useRouter } from 'vue-router';

import RuleSelectorDialog from '@/views/ast/quality/qualityTask/components/ruleBase.vue';

import { listAttQualityCat } from "@/api/tax/cat/qualityCat/qualityCat.js";

import {
    addDppQualityTask,
    updateDppQualityTask
} from "@/api/ast/quality/qualityTask";
const { proxy } = getCurrentInstance();
const route = useRoute();
const loading = ref(false);
const showSearch = ref(true);

import moment from 'moment';

import {
    getColumnByAssetId,
} from '@/api/col/task/index.js';

import useUserStore from '@/store/system/user';
let id = route.query.id || '';
const router = useRouter();
const userStore = useUserStore();
const { att_rule_audit_q_dimension, col_etl_task_execution_type } = proxy.useDict(

    'att_rule_audit_q_dimension', 'col_etl_task_execution_type'
);
let dppQualityTaskObjSaveReqVO = ref([

])
function convertAssetToTask(asset) {
    return [{
        datasourceId: asset.datasourceId,
        name: asset.tableComment,
        datasourceType: asset.datasourceType,
        tableName: asset.tableName
    }];
}

// 图标
const getDatasourceIcon = (type) => {
    switch (type) {
        case "DM8": return new URL("@/assets/system/images/dpp/DM.png", import.meta.url).href;
        case "Oracle11": return new URL("@/assets/system/images/dpp/oracle.png", import.meta.url).href;
        case "MySql": return new URL("@/assets/system/images/dpp/mysql.png", import.meta.url).href;
        case "Hive": return new URL("@/assets/system/images/dpp/Hive.png", import.meta.url).href;
        case "Sqlerver": return new URL("@/assets/system/images/dpp/sqlServer.png", import.meta.url).href;
        case "Kafka": return new URL("@/assets/system/images/dpp/kafka.png", import.meta.url).href;
        case "HDFS": return new URL("@/assets/system/images/dpp/hdfs.png", import.meta.url).href;
        case "SHELL": return new URL("@/assets/system/images/dpp/SHELL.png", import.meta.url).href;
        case "Kingbase8": return new URL("@/assets/system/images/dpp/kingBase.png", import.meta.url).href;
        default: return null;
    }
};

let loadingInstance = ref(null)  // 全局 loading 实例
let originList = ref([

])
function getIconByValue(value) {
    const node = treeData.find(item => item.value?.toLowerCase() === value?.toLowerCase())
    return node ? node.icon : ''
}
const dppQualityTaskEvaluateSaveReqVO = ref([...originList.value]);
const tableColumns = computed(() => {
    const cols = [
        { title: '编号', dataIndex: 'id', align: 'left' },
        { title: '评测名称', dataIndex: 'name', align: 'left', ellipsis: true },
        { title: '评测字段', dataIndex: 'evaColumn', align: 'left', ellipsis: true },
        { title: '稽查规则', dataIndex: 'ruleName', align: 'left', width: 200, ellipsis: true },
        { title: '规则描述', dataIndex: 'ruleDescription', align: 'left', ellipsis: true },
        { title: '质量维度', dataIndex: 'dimensionType', align: 'left', width: 100, ellipsis: true },
        { title: '状态', dataIndex: 'status', align: 'left', width: 80 },
    ];
    if (!route.query.info) {
        cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 180 });
    }
    return cols;
});

let loadingList = ref(false)
const handleQuery = () => {
    dppQualityTaskEvaluateSaveReqVO.value = originList.value.filter(item => {
        if (queryParams.value.name && !item.name.includes(queryParams.value.name)) return false;
        if (queryParams.value.dimensionType && item.dimensionType !== queryParams.value.dimensionType) return false;
        if (queryParams.value.publishStatus) {
            const statusVal = queryParams.value.publishStatus === 'online' ? '1' : '0';
            if (item.status !== statusVal) return false;
        }

        return true;
    });
};
const resetQuery = () => {
    queryParams.value = {
        name: '',
        qualityDim: '',
        publishStatus: '',
    };
    dppQualityTaskEvaluateSaveReqVO.value = [...originList.value];
};
let deptOptions = ref([])

let openCron = ref(false);
const expression = ref("");
/** 调度周期按钮操作 */
function handleShowCron() {
    expression.value = form.value.cycle;
    openCron.value = true;
}
/** 确定后回传值 */
async function crontabFill(value) {
    form.value.cycle = value;
    await nextTick();
    formRef.value?.validateField('cycle');
}
function getDeptTree() {
    listAttQualityCat().then((response) => {
        deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
        deptOptions.value = [
            {
                name: "质量探查目录",
                value: "",
                id: 0,
                children: deptOptions.value,
            },
        ];
    });
}
const data = reactive({
    form: {
        assetFlag: '1',
        assetId: '',
        taskName: '',
        catCode: "-1",
        status: '1',
        contactId: '',
        priority: 'medium',
        workerGroup: 'default',
        retryCount: 0,
        retryInterval: 0,
        delayMinutes: 0,
        description: '',
        retryTimes: "",
        delayTime: "",
        cycle: "0 0 0 * * ?",
        strategy: "PARALLEL"

    },
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        name: '',
        qualityDim: '',
        publishStatus: ''
    },
    activeReult: 0,
    active: 0,
    loadingOptions: { loading: false }
});

const { form, stepsList, activeReult, loadingOptions, queryParams, active } = toRefs(data);
const formRef = ref();
let formData = ref({})
let visible = ref(false)
let title = ref("")
function open(data, row) {
    handleClose()
    form.value.assetId = data.id
    form.value.taskName = `${data.name}_${data.tableName}_${moment().format('YYYYMMDDHHmmss')}`;
    getDeptTree()
    title.value = row?.id ? "修改资产质量任务" : "新增资产质量任务"
    console.log("🚀 ~ open ~ row?.nam:", row)
    formData.value = data
    visible.value = true
    if (row?.id) {
        getDppQualityTaskinfo(row)
    } else {
        dppQualityTaskObjSaveReqVO.value = JSON.parse(JSON.stringify(convertAssetToTask(data)));
    }
}
function renameRuleToRuleConfig(data, obj) {
    return data
        .filter(col => Array.isArray(col.cleanRuleList) && col.cleanRuleList.length > 0)
        .flatMap(col =>
            col.cleanRuleList.map(item => {
                let parsedRule = {};
                try {
                    parsedRule = JSON.parse(item.rule || '{}');
                } catch (e) {
                    console.warn(`rule JSON 解析失败: ${item.rule}`, e);
                }

                const evaColumnStr = col.columnName;

                return {
                    ...item,
                    id: undefined,
                    warningLevel: '2',
                    datasourceId: obj?.datasourceId || '',
                    tableName: obj?.tableName || col.tableName,
                    evaColumn: evaColumnStr,
                    rule: JSON.stringify({
                        ...parsedRule,
                        evaColumn: evaColumnStr,
                    })
                };
            })
        );
}
async function selectInspectionRule() {
    loading.value = true;

    try {
        for (const item of dppQualityTaskObjSaveReqVO.value || []) {
            try {
                const res = await getColumnByAssetId({
                    withRule: 1,
                    id: item.datasourceId,
                    tableName: item.tableName,
                    spaceId: userStore.spaceId,
                    spaceCode: userStore.spaceCode
                });

                if (res?.data?.length) {
                    const rowsWithSource = res.data.map(row => ({
                        ...row,
                        datasourceId: item.datasourceId,
                        tableName: item.tableName
                    }));

                    const obj = renameRuleToRuleConfig(rowsWithSource, item) || [];

                    let addedCount = 0;
                    obj.forEach(newRule => {
                        // 规则唯一标识
                        const key = `${newRule.tableName}_${newRule.evaColumn}_${newRule.ruleName}`;

                        // 查找是否已存在相同规则
                        const existIndex = originList.value.findIndex(
                            r => `${r.tableName}_${r.evaColumn}_${r.ruleName}` === key
                        );

                        if (existIndex > -1) {
                            // 覆盖
                            originList.value.splice(existIndex, 1, newRule);
                        } else {
                            // 追加
                            originList.value.push(newRule);
                            addedCount++;
                        }
                    });

                    dppQualityTaskEvaluateSaveReqVO.value = [...originList.value];

                    if (addedCount > 0) {
                        message.success(`已追加 ${addedCount} 条规则，来自表 ${item.tableName}`);
                    } else {
                        message.info(`表 ${item.tableName} 没有新规则追加`);
                    }
                }
            } catch (err) {
                console.warn(`获取规则失败: datasourceId=${item.datasourceId}, tableName=${item.tableName}`, err);
            }
        }
    } finally {
        loading.value = false;
    }
}

const inspectionTargetDialog = ref();
function handleRuleDelete(index) {
    const realIndex = Number(index) - 1;
    originList.value.splice(realIndex, 1);
    dppQualityTaskEvaluateSaveReqVO.value = originList.value
}

let ruleSelectorDialog = ref()
const openRuleSelector = (row) => {
    ruleSelectorDialog.value.openDialog(undefined, undefined, undefined);
};
const openRuleDialog = (row, index, falg) => {

    ruleSelectorDialog.value.openDialog(row, index, falg);
};
function RuleSelectorconfirm(obj, mode) {
    const index = Number(mode) - 1;
    const list = originList.value;
    const isDuplicate = list.some((item, i) => {
        if (index >= 0) {
            return i !== index && item.name == obj.name;
        } else {
            return item.name === obj.name;
        }
    });

    if (isDuplicate) {
        proxy.$message.warning("评测名称不能重复！");
        return;
    }

    if (!isNaN(index) && index >= 0 && index < list.length) {
        list.splice(index, 1, obj);
    } else {
        list.push(obj);
    }

    dppQualityTaskEvaluateSaveReqVO.value = list;
    ruleSelectorDialog.value.closeDialog();
}

const emit = defineEmits(['submit-success'])
async function submitForm() {
    loadingInstance.value = true
    try {
        await formRef.value?.validate();
    } catch (err) {
        message.warning("表单校验未通过，请检查必填项！");
        loadingInstance.value = false
        return;
    }
    try {
        dppQualityTaskObjSaveReqVO.value = dppQualityTaskObjSaveReqVO.value.map(item => ({
            ...item,
            name: item.name && item.name.trim() ? item.name : `资产质量${moment().format('YYYYMMDDHHmmss')}`
        }));

        const payload = {
            ...form.value,
            creatorId: userStore.id,
            createBy: userStore.nickName || userStore.name,
            contactId: userStore.id,
            dppQualityTaskObjSaveReqVO: dppQualityTaskObjSaveReqVO.value,
            dppQualityTaskEvaluateSaveReqVO: dppQualityTaskEvaluateSaveReqVO.value
        };

        const res = form.value.id
            ? await updateDppQualityTask(payload)
            : await addDppQualityTask(payload);

        // 响应处理
        if (res.code == '200') {
            handleClose()
            emit('submit-success')
        } else {
            message.error(res.msg || "提交失败！");
        }
    } catch (err) {

    } finally {
        loadingInstance.value = false
    }
}

function code(obj) {
    dppQualityTaskObjSaveReqVO.value = Array.isArray(obj) ? [...obj] : [];
    console.log("🚀 ~ code ~ dppQualityTaskObjSaveReqVO.value:", dppQualityTaskObjSaveReqVO.value)
}

function getDppQualityTaskinfo(data) {
    loadingInstance.value = true;

    try {
        const detail = data || {};
        const taskObjList =
            detail.dppQualityTaskObjSaveReqVO ||
            detail.collectorQualityTaskObjSaveReqVO ||
            detail.qualityTaskObjSaveReqVO ||
            detail.CollectorQualityTaskObjSaveReqVO ||
            detail.QualityTaskObjSaveReqVO ||
            [];
        const taskEvaluateList =
            detail.dppQualityTaskEvaluateRespVOS ||
            detail.collectorQualityTaskEvaluateRespVOS ||
            detail.qualityTaskEvaluateRespVOS ||
            detail.CollectorQualityTaskEvaluateRespVOS ||
            detail.QualityTaskEvaluateRespVOS ||
            detail.dppQualityTaskEvaluateSaveReqVO ||
            detail.collectorQualityTaskEvaluateSaveReqVO ||
            detail.qualityTaskEvaluateSaveReqVO ||
            [];
        const {
            dppQualityTaskObjSaveReqVO: _dppQualityTaskObjSaveReqVO,
            collectorQualityTaskObjSaveReqVO: _collectorQualityTaskObjSaveReqVO,
            qualityTaskObjSaveReqVO: _qualityTaskObjSaveReqVO,
            CollectorQualityTaskObjSaveReqVO: _CollectorQualityTaskObjSaveReqVO,
            QualityTaskObjSaveReqVO: _QualityTaskObjSaveReqVO,
            dppQualityTaskEvaluateRespVOS: _dppQualityTaskEvaluateRespVOS,
            collectorQualityTaskEvaluateRespVOS: _collectorQualityTaskEvaluateRespVOS,
            qualityTaskEvaluateRespVOS: _qualityTaskEvaluateRespVOS,
            CollectorQualityTaskEvaluateRespVOS: _CollectorQualityTaskEvaluateRespVOS,
            QualityTaskEvaluateRespVOS: _QualityTaskEvaluateRespVOS,
            dppQualityTaskEvaluateSaveReqVO: _dppQualityTaskEvaluateSaveReqVO,
            collectorQualityTaskEvaluateSaveReqVO: _collectorQualityTaskEvaluateSaveReqVO,
            qualityTaskEvaluateSaveReqVO: _qualityTaskEvaluateSaveReqVO,
            ...obj
        } = detail;
        originList.value = Array.isArray(taskEvaluateList) ? [...taskEvaluateList] : []
        dppQualityTaskEvaluateSaveReqVO.value = Array.isArray(taskEvaluateList) ? [...taskEvaluateList] : [];
        code(taskObjList)
        Object.assign(form.value, obj);
        if (form.value.contactId != null && form.value.contactId !== "") {
            form.value.contactId = Number(form.value.contactId)
        }
    } finally {
        loadingInstance.value = false;
    }
}
const handleClose = () => {
    formData.value == {}
    dppQualityTaskEvaluateSaveReqVO.value = []
    dppQualityTaskObjSaveReqVO.value = []
    originList.value = []
    visible.value = false
}

defineExpose({
    open
});
</script>
<style scoped lang="scss"></style>

