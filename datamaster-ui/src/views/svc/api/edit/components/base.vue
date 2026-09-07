<template>
    <a-form class="service-api-base-form" ref="form1" :model="form1" :rules="rules1"
        :label-col="{ style: { width: '120px' } }">
        <!--        <a-row :gutter="20">-->
        <!--            <a-col :span="12">-->
        <!--                <a-form-item label="数据所属目录" name="typeId" v-if="isChange">-->
        <!--                    <a-tree-select show-search v-model:value="form1.catCode"  :tree-data="deptOptions"-->
        <!--                                    :field-names="{ value: 'code', label: 'name', children: 'children' }"-->  
        <!--                                    placeholder="请选择所属目录" tree-check-strictly/>-->
        <!--                </a-form-item>-->
        <!--            </a-col>-->
        <!--        </a-row>-->
        <a-row :gutter="[32, 4]">
            <a-col :span="12">
                <a-form-item label="API名称" name="name">
                    <a-input v-model:value="form1.name" placeholder="请输入API名称" />
                </a-form-item>
            </a-col>
            <a-col :span="12">
                <a-form-item label="API版本" name="apiVersion">
                    <a-input v-model:value="form1.apiVersion" placeholder="请输入API版本，如v1.0.0" />
                </a-form-item>
            </a-col>
        </a-row>
        <a-row :gutter="[32, 4]">
            <a-col :span="12">
                <a-form-item label="API地址" name="apiUrl">
                    <a-input v-model:value="form1.apiUrl" placeholder="请输入API地址，只允许字母、数字、下划线、中划线和斜杠"
                        @input="handleApiUrlInput"
/>
                </a-form-item>
            </a-col>
            <a-col :span="12">
                <a-form-item label="API目录" name="catCode">
                    <a-tree-select show-search v-model:value="form1.catCode" :tree-data="deptOptions"
                        :field-names="{ value: 'code', label: 'name', children: 'children' }"
                        placeholder="请选择所属API目录" @change="handleCatSelect"
/>
                </a-form-item>
            </a-col>
        </a-row>
        <a-row :gutter="[32, 4]">
            <a-col :span="12">
                <a-form-item label="请求方式" name="reqMethod">
                    <a-select v-model:value="form1.reqMethod" placeholder="请选择请求方式">
                        <a-select-option v-for="dict in ds_api_bas_info_api_method_type" :key="dict.value" :label="dict.label"
                            :value="dict.value"
/>
                    </a-select>
                </a-form-item>
            </a-col>
            <a-col :span="12">
                <a-form-item label="返回格式" name="resDataType">
                    <a-select v-model:value="form1.resDataType" placeholder="请选择返回格式">
                        <a-select-option v-for="dict in ds_api_bas_info_res_data_type" :key="dict.value" :label="dict.label"
                            :value="dict.value"
/>
                    </a-select>
                </a-form-item>
            </a-col>
        </a-row>
        <a-row :gutter="[32, 4]">
            <a-col :span="24">
                <a-form-item label="描述" name="description">
                    <a-textarea v-model:value="form1.description" placeholder="请输入描述" />
                </a-form-item>
            </a-col>
        </a-row>
        <!--        <a-form-item label="是否开启缓存：" name="cacheSwitch">-->
        <!--            <a-radio-group v-model:value="form1.cacheSwitch">-->
        <!--                <a-radio v-for="dict in whetherOptions" :key="dict.id" :value="dict.itemText">{{ dict.itemValue-->
        <!--                    }}</a-radio>-->
        <!--            </a-radio-group>-->
        <!--        </a-form-item>-->
        <!--        <a-form-item label="是否显示JSON样例" name="sortColumn">-->
        <!--            <a-radio-group v-model:value="form1.sortColumn">-->
        <!--                <a-radio v-for="dict in cacheOptions" :key="dict.id" :value="dict.itemValue">-->
        <!--                    {{ dict.itemText }}-->
        <!--                </a-radio>-->
        <!--            </a-radio-group>-->
        <!--        </a-form-item>-->
        <a-row :gutter="[32, 4]">
            <a-col :span="24">
                <a-form-item label="IP黑名单" name="deny">
                    <a-textarea v-model:value="form1.deny" placeholder="请输入IP黑名单多个用英文,隔开" />
                </a-form-item>
            </a-col>
        </a-row>

        <a-row :gutter="[32, 4]">
            <a-col :span="12">
                <a-form-item label="是否限流" name="rateLimit">
                    <a-radio-group v-model:value="form1.rateLimit.enable">
                        <a-radio v-for="dict in ds_api_limit_status" :key="dict.value" :value="dict.value">{{
                            dict.label
                            }}</a-radio>
                    </a-radio-group>
                </a-form-item>
            </a-col>
            <a-col :span="12">
                <a-form-item label="状态" name="status">
                    <a-radio-group v-model:value="form1.status">
                        <a-radio v-for="dict in ds_api_status" :key="dict.value" :value="dict.value">{{
                            dict.label
                        }}</a-radio>
                    </a-radio-group>
                </a-form-item>
            </a-col>
        </a-row>

        <a-row :gutter="[32, 4]">
            <a-col :span="24" style="color: #333333;">
                <!-- class="input-number" -->
                <a-form-item v-if="form1.rateLimit.enable === '1'" label="限流配置">
                    每&nbsp;
                    <a-input-number v-model:value="form1.rateLimit.seconds" :min="1" />
                    &nbsp; 秒内限制请求 &nbsp;
                    <a-input-number v-model:value="form1.rateLimit.times" :min="1" />
                    &nbsp; 次
                </a-form-item>
            </a-col>

        </a-row>
        <a-row :gutter="[32, 4]">
            <a-col :span="24">
                <a-form-item label="备注" name="remark">
                    <a-textarea v-model:value="form1.remark" placeholder="请输入内容" />
                </a-form-item>
            </a-col>
        </a-row>
    </a-form>
</template>

<script setup name="base">
import { listAttApiCat } from '@/api/svc/apiCat/apiCat';
const { proxy } = getCurrentInstance();
const {
    ds_api_bas_info_res_data_type,
    da_sensitive_status,
    ds_api_bas_info_api_method_type,
    ds_api_status,
    ds_api_limit_status
} = proxy.useDict(
    'ds_api_bas_info_res_data_type',
    'da_sensitive_status',
    'ds_api_bas_info_api_method_type',
    'ds_api_status',
    'ds_api_limit_status'
);

const props = defineProps({
    form1: {
        type: Object,
        default: () => {
            return {
                status: 1
            };
        }
    },
    rules1: {
        type: Object,
        required: true
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
        default: false
    },
    treeOptions: {
        type: Array,
        default: () => []
    },
    idArray: {
        type: Array,
        default: () => []
    },
    typeNames: {
        type: String,
        default: () => ''
    }
});

const data = reactive({
    // 定义一个 data 用来存储 props 中的值
    localForm: { ...props.form1 }, // 用于管理表单数据
    typeName: props.typeNames, // 用于存储目录名称
    defaultProps: {
        children: 'children',
        label: 'name',
        isLeaf: 'isLeaf' // 指定是否是叶子节点的字段名
    },
    cacheOptions: [
        { id: 1, itemText: '是', itemValue: 0 },
        { id: 2, itemText: '否', itemValue: 1 }
    ],
    deptOptions: []
});

const { localForm, typeName, defaultProps, cacheOptions, deptOptions } = toRefs(data);

watch(
    () => props.form1,
    (newValue, oldValue) => {
        // 当 props 中的 form1 发生变化时，更新 localForm
        localForm.value = { ...newValue };
    }
);
props.form1.status = props.form1.status || '0';

function getApiCatList() {
    listAttApiCat().then((response) => {
        deptOptions.value = proxy.handleTree(response.data, 'id', 'parentId');
        deptOptions.value = [
            {
                name: 'API服务目录',
                value: '',
                id: 0,
                children: deptOptions.value
            }
        ];
    });
}
// 树形选项数据的规范化
function normalizeOptions(node) {
    if (node.children && !node.children.length) {
        delete node.children; // 去除没有子节点的空children属性
    }
    return {
        id: node.id,
        label: node.name,
        children: node.children
    };
}
function validateFormBase(formName, callback) {
    proxy.$refs[formName].validate().then(() => {
        callback(props.form1);
    }).catch(() => {});
}
getApiCatList();
defineExpose({
    validateFormBase
});

// 处理API地址输入，过滤非法字符
const handleApiUrlInput = (value) => {
    console.log('value', value);

    if (value) {
        // 只保留字母、数字、下划线、中划线和斜杠
        const filteredValue = value.replace(/[^\w\-\/]/g, '');
        // 直接更新表单值
        props.form1.apiUrl = filteredValue;
    }
};

// 处理目录选择
const handleCatSelect = (value) => {
    // 在所有选项中查找匹配的目录
    const findCategory = (options, code) => {
        for (const option of options) {
            if (option.code === code) {
                return option;
            }
            if (option.children) {
                const found = findCategory(option.children, code);
                if (found) return found;
            }
        }
        return null;
    };

    const selectedCat = findCategory(deptOptions.value, value);
    console.log('selectedCat', selectedCat);

    if (selectedCat) {
        // 同时设置 catCode 和 catId
        props.form1.catCode = selectedCat.code;
        props.form1.catId = selectedCat.id;
    }
};
</script>

<style scoped>
.service-api-base-form {
    width: 100%;
}

.service-api-base-form :deep(.ant-input),
.service-api-base-form :deep(.ant-input-affix-wrapper),
.service-api-base-form :deep(.ant-input-number),
.service-api-base-form :deep(.ant-select),
.service-api-base-form :deep(.ant-tree-select) {
    width: 100%;
    max-width: 100%;
}

@container service-api-editor (max-width: 1080px) {
    .service-api-base-form :deep(.ant-col-12) {
        max-width: 100%;
        flex: 0 0 100%;
    }
}

.input-number {
    width: auto;
    max-width: 150px;
}
</style>

