<template>
    <div class="app-container">
        <div slot="header" class="header-container">
            <div class="header-left">
                <div class="blue-bar"></div>
                API调用
            </div>
            <a-button type="primary" style="border-radius: 30px !important" @click="handleCall">
                接口调用
            </a-button>
        </div>
        <div class="body-wrapper">
            <a-form v-if="isChange" ref="form" :model="form" :label-col="{ style: { width: '100px' } }" :disabled="true">
                <a-row>
                    <a-col :span="12">
                        <a-form-item label="API名称">
                            <a-input v-model:value="form.name" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="API版本">
                            <a-input v-model:value="form.apiVersion" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row>
                    <a-col :span="12">
                        <a-form-item label="请求类型">
                            <!--                            <a-input v-model:value="form.reqMethod"/>-->
                            <dict-tag :options="ds_api_bas_info_api_method_type" :value="form.reqMethod" />
                        </a-form-item>
                        <!--                        原请求方式列（table-column 形式），迁移后由 tableColumns + bodyCell 模式实现 -->
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="返回格式" name="resDataType">
                            <dict-tag :options="ds_api_bas_info_res_data_type" :value="form.resDataType" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row>
                    <a-col :span="24">
                        <a-form-item label="调用地址">
                            <a-input v-model:value="form.apiUrl" />
                        </a-form-item>
                    </a-col>
                </a-row>
            </a-form>
            <div class="header-container">
                <div class="header-left">
                    <div class="blue-bar"></div>
                    请求数据
                </div>
            </div>
            <a-row>
                <a-col :span="24">
                    <a-table class="tableStyle" :data-source="form.reqParams" :columns="reqParamColumns" :pagination="false" striped :scroll="{ y: 250 }" style="width: 100%; margin: 15px 0" size="small">
                        <template #bodyCell="{ column, record, index }">
                            <template v-if="column.key === 'index'">
                                <span>{{ index + 1 }}</span>
                            </template>
                            <template v-else-if="column.dataIndex === 'nullable'">
                                <a-checkbox :checked="record.nullable === '1'" disabled />
                            </template>
                            <template v-else-if="column.dataIndex === 'paramType'">
                                <dict-tag :options="ds_api_param_type" :value="record.paramType" />
                            </template>
                            <template v-else-if="column.dataIndex === 'whereType'">
                                <a-select v-model:value="record.whereType" placeholder="请选择操作符" disabled>
                                    <a-select-option v-for="dict in da_api_param_operator" :key="dict.id" :value="dict.value">{{ dict.label }}</a-select-option>
                                </a-select>
                            </template>
                            <template v-else-if="column.dataIndex === 'paramValue'">
                                <a-input v-if="record.paramType != '2'" v-model:value="record.paramValue" placeholder="请输入参数值" />
                                <a-input v-else-if="record.paramType === '2'" v-model:value="record.paramValue" placeholder="请输入参数值" type="number" />
                            </template>
                        </template>
                    </a-table>
                </a-col>
            </a-row>
            <div class="header-container">
                <div class="header-left">
                    <div class="blue-bar"></div>
                    返回数据
                </div>
            </div>
            <a-row>
                <a-col :span="24">
                    <div v-if="apiExecuting">
                        <a-table :data-source="callData.dataList" :columns="callDataColumns" :pagination="false" striped :scroll="{ y: 250 }" style="width: 100%; margin: 15px 0" size="small">
                            <template #bodyCell="{ column, index }">
                                <template v-if="column.key === 'index'">
                                    <span>{{ index + 1 }}</span>
                                </template>
                            </template>
                        </a-table>
                        <div style="display: flex; justify-content: flex-end; margin-top: 20px;"
                            v-if="callData.dataTotal"
>
                            <a-pagination v-if="form.resDataType == '1' || form.resDataType == '3'"
                                :page-size-options="['6', '8', '10', '20', '50', '100']"
                                v-model:current="callData.pageNum" v-model:pageSize="callData.pageSize"
                                :total="callData.dataTotal" show-size-changer show-quick-jumper
                                :locale="paginationLocale"
                                @change="handleCurrentChange"
                                @showSizeChange="(current, size) => handleSizeChange(size)"
/>
                        </div>

                    </div>
                    <div v-else class="no-data">暂无数据</div>
                </a-col>
            </a-row>
        </div>
    </div>
</template>

<script setup>
import { message } from 'ant-design-vue'
import { serviceTesting } from '@/api/svc/api/api.js';

import useUserStore from '@/store/system/user';
import paginationLocale from '@/utils/paginationLocale';
const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const { ds_api_bas_info_api_method_type, ds_api_param_type, ds_api_bas_info_res_data_type,  da_api_param_operator } =
    proxy.useDict(
        'ds_api_bas_info_api_method_type',
        'ds_api_param_type',
        'ds_api_bas_info_res_data_type',
        "da_api_param_operator"
    );

const props = defineProps({
    data: {
        type: Object,
        default: function () {
            return {};
        }
    },
    form: {
        type: Object,
        default: {}
    },
    reqMethodOptions: {
        type: Array,
        required: true
    },

    resTypeOptions: {
        type: Array,
        required: true
    },
    whetherOptions: {
        type: Array,
        required: true
    },
    statusOptions: {
        type: Array,
        required: true
    },
    isChange: {
        type: Boolean,
        default: true
    }
});
const data = reactive({
    title: '数据API调用',
    // 展示切换
    showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false,
        showExample: false
    },
    activeTabName: 'table0',
    apiHeader: {},
    apiHeaderList: [],
    // 操作符数据字典
    whereTypeOptions: [],
    // 参数类型数据字典
    paramTypeOptions: [],
    apiExecuting: false,
    callData: {
        dataList: [],
        columnList: [],
        pageNum: 1,
        pageSize: 6,
        dataTotal: 0
    },
    bashUrl: null
});

const {
    apiHeader,
    apiHeaderList,
    whereTypeOptions,
    activeTabName,
    showOptions,
    title,
    paramTypeOptions,
    apiExecuting,
    callData,
    bashUrl
} = toRefs(data);

const reqParamColumns = [
    { title: '序号', key: 'index', align: 'center', width: 80 },
    { title: '参数名称', dataIndex: 'paramName', align: 'center', ellipsis: true },
    { title: '是否允许为空', dataIndex: 'nullable', align: 'center', ellipsis: true },
    { title: '描述', dataIndex: 'paramComment', align: 'center', ellipsis: true },
    { title: '参数类型', dataIndex: 'paramType', align: 'center', ellipsis: true },
    { title: '操作符', dataIndex: 'whereType', align: 'center', ellipsis: true },
    { title: '参数值', dataIndex: 'paramValue', align: 'center', ellipsis: true },
];

const callDataColumns = computed(() => {
    const cols = [{ title: '序号', key: 'index', align: 'center', width: 80 }];
    (callData.value.columnList || []).forEach((col) => {
        cols.push({ title: col, dataIndex: col, align: 'center', width: 180, ellipsis: true });
    });
    return cols;
});

function showCard() {
    this.$emit('showCard', this.showOptions);
}

function handleSizeChange(val) {
    callData.pageNum = 1;
    callData.pageSize = val;
    handleCall();
}

function handleCurrentChange(val) {
    callData.pageNum = val;
    handleCall();
}

function handleCall() {
    let isNull = false;
    props.form.reqParams.forEach((param) => {
        if (
            param.nullable == '0' &&
            (param.paramValue == null ||
                param.paramValue == undefined ||
                param.paramValue == '')
        ) {
            proxy.$message.warning('输入参数‘' + param.paramName + '’不能为空');
            isNull = true;
            return;
        }
    });
    if (isNull) {
        return;
    }
    const data = {};
    data.pageNum = callData.pageNum;
    data.pageSize = callData.pageSize;
    props.form.reqParams.forEach((param) => {
        param = JSON.parse(JSON.stringify(param));
        if (param.paramType == 2) {
            if (
                param.paramValue != null &&
                param.paramValue != '' &&
                param.paramValue != undefined
            ) {
                param.paramValue = parseInt(param.paramValue);
            }
        }
      if (param.paramType == 5) {
        if (
            param.paramValue != null &&
            param.paramValue != '' &&
            param.paramValue != undefined
        ) {
          try {
            param.paramValue = JSON.parse(param.paramValue);
          } catch (error) {
            proxy.$message.warning('输入参数‘' + param.paramName + '’格式有误，例如为[1,2]或["1","2"]');
            return;
          }
        }
      }
        data[param.paramName] = param.paramValue;
    });
    props.form.params = data;
    let params = {};
    //将props.form的值全部给params
    Object.assign(params, props.form);
    params.reqParamsList = params.reqParams;
    params.resParamsList = params.resParams;
    //删除reqParams和resParams
    delete params.reqParams;
    delete params.resParams;
    delete params.createTime;
    delete params.updateTime;
    params.spaceId = params.spaceId || userStore.spaceId || null;
    params.spaceCode = params.spaceCode || userStore.spaceCode || '';
    // 根据请求方法 (GET / POST) 进行处理
    if (props.form.reqMethod === '1') {
        // 使用 serviceTesting 来模拟 GET 请求
        serviceTesting(params).then((response) => {
            if (response.code === 200) {
                proxy.$message.success('接口调用成功');
                const { data } = response;
                const dataList = [];

                // 根据 resDataType 处理返回的数据
                if (props.form.resDataType == 3) {
                    dataList.push(...data.data);
                } else if (props.form.resDataType == '2') {
                    dataList.push(...data);
                } else {
                    dataList.push(data);
                }

                // 获取列名
                let columnList = [];
                if (dataList.length > 0) {
                    columnList = Object.keys(dataList[0]);
                }

                // 更新数据和列名
                callData.value.dataList = dataList;
                callData.value.columnList = columnList;

                // 如果 resDataType 为 '1'，更新 total
                if (props.form.resDataType == '1' || props.form.resDataType == '3') {
                    callData.value.dataTotal = data.total;
                }
                // 更新 API 执行状态
                apiExecuting.value = true;
            } else {
                // 请求失败的处理
                proxy.$message.warning("操作失败，请联系管理员");
            }
        });
    } else if (props.form.reqMethod === '2') {
        serviceTesting(params).then((response) => {
            if (response.code === 200) {
                proxy.$message.success('接口调用成功');

                const { data } = response;
                const dataList = [];

                // 根据 resDataType 处理返回的数据
                if (props.form.resDataType == 3) {
                    dataList.push(...data.data);
                } else if (props.form.resDataType === '2') {
                    dataList.push(...data);
                } else {
                    dataList.push(data);
                }

                // 获取列名
                let columnList = [];
                if (dataList.length > 0) {
                    columnList = Object.keys(dataList[0]);
                }

                // 更新数据和列名
                callData.value.dataList = dataList;
                callData.value.columnList = columnList;

                // 如果 resDataType 为 '1'，更新 total
                if (props.form.resDataType === '1' || props.form.resDataType == '3') {
                    callData.value.dataTotal = data.total;
                }
                // 更新 API 执行状态
                apiExecuting.value = true;
            } else {
                proxy.$message.warning("操作失败，请联系管理员");
            }
        });
    }
}
</script>

<style lang="scss" scoped>
.app-container {
    margin-top: -5px;
    min-height: 65vh;
    margin-left: 0px;
    // padding: 20px;
    background-color: #ffffff;
    border-radius: 8px;

    // box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

    .header-text {
        display: flex;
        align-items: center;
        margin-bottom: 3px;
        margin: 10px 0;
    }

    .section-title {
        width: 100%;
        height: 36px;
        background-color: #f8f8f9;
        display: flex;
        align-items: center;
        padding-left: 10px;
        margin-bottom: 10px;
        font-size: 16px;
        font-weight: bold;
        color: #333;
    }

    .section-title span {
        display: flex;
        align-items: center;
    }

    .blue-bar {
        background-color: #2666fb;
        width: 5px;
        height: 20px;
        margin-right: 10px;
        border-radius: 2px;
    }

    .header-container {
        height: 36px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 5px 0;
        margin: 10px 0;
        border-radius: 4px;
    }

    .header-left {
        display: flex;
        align-items: center;
        font-size: 16px;
        line-height: 24px;
        font-style: normal;
    }

    .ant-form {
        margin-top: 20px;
    }

    .ant-form-item {
        margin-bottom: 15px;
    }

    .ant-input,
    .ant-select {
        width: 100%;
    }

    .ant-btn {
        transition: background-color 0.3s;
    }

    .tableStyle {
        font-size: 14px;
        margin: 0px !important;
    }

    .no-data {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 250px;
        text-align: center;
        font-size: 14px;
        color: #909399;
        background-color: #f8f8f9;
        border: 1px solid #ebeef5;
        border-radius: 4px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }
}
</style>

