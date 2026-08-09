<template>
    <!-- // 视频  -->
    <a-row :gutter="20" v-if="form.type == '5'">
        <a-col :span="12">
            <a-form-item label="平台" name="assetsAssetVideo.platform" :rules="[
                {
                    required: true,
                    message: '请选择平台',
                    trigger: 'blur',
                },
            ]">
                <a-select v-model:value="form.assetsAssetVideo.platform" placeholder="请选择平台">
                    <a-select-option v-for="dict in da_asset_video_platform" :key="dict.value" :label="dict.label"
                        :value="dict.value" />
                </a-select>
            </a-form-item>
        </a-col>
        <a-col :span="12">
            <a-form-item label="平台ip" name="assetsAssetVideo.ip" :rules="[
                {
                    required: true,
                    message: '请输入平台IP',
                    trigger: 'blur',
                },
            ]">
                <a-input v-model:value="form.assetsAssetVideo.ip" placeholder="请输入平台ip" />
            </a-form-item>
        </a-col>

    </a-row>
    <a-row :gutter="20" v-if="form.type == '5'">
        <a-col :span="12">
            <a-form-item label="平台端口" name="assetsAssetVideo.port" :rules="[
                {
                    required: true,
                    message: '请输入平台端口',
                    trigger: 'blur',
                },
            ]">
                <a-input v-model:value="localForm.assetsAssetVideo.port" placeholder="请输入平台端口" />
            </a-form-item>
        </a-col>
        <a-col :span="12">
            <a-form-item label="摄像头编码" name="assetsAssetVideo.config.cameraCode" :rules="[
                {
                    required: true,
                    message: '请输入摄像头编码',
                    trigger: 'blur',
                },
            ]">
                <a-input v-model:value="localForm.assetsAssetVideo.config.cameraCode" placeholder="请输入摄像头编码" />
            </a-form-item>
        </a-col>
    </a-row>
    <a-row :gutter="20" v-if="form.type == '5'">
        <a-col :span="12">
            <a-form-item label="摄像头名称" name="assetsAssetVideo.config.cameraName" :rules="[
                {
                    required: true,
                    message: '请输入摄像头名称',
                    trigger: 'blur',
                },
            ]">
                <a-input v-model:value="localForm.assetsAssetVideo.config.cameraName" placeholder="请输入摄像头名称" />
            </a-form-item>
        </a-col>
        <a-col :span="12">
            <a-form-item label="服务上下文" name="assetsAssetVideo.config.artemisPath" :rules="[
                {
                    required: true,
                    message: '请输入服务上下文',
                    trigger: 'blur',
                },
            ]">
                <a-input v-model:value="localForm.assetsAssetVideo.config.artemisPath" placeholder="/artemis" />
            </a-form-item>
        </a-col>

    </a-row>
    <a-row :gutter="20" v-if="form.type == '5'">
        <a-col :span="12">
            <a-form-item label="公钥" name="assetsAssetVideo.config.appkey" :rules="[
                {
                    required: true,
                    message: '请输入公钥',
                    trigger: 'blur',
                },
            ]">
                <a-input v-model:value="localForm.assetsAssetVideo.config.appkey" placeholder="请输入公钥" />
            </a-form-item>
        </a-col>
        <a-col :span="12">
            <a-form-item label="私钥" name="assetsAssetVideo.config.appSecret" :rules="[
                {
                    required: true,
                    message: '请输入私钥',
                    trigger: 'blur',
                },
            ]">
                <a-input v-model:value="localForm.assetsAssetVideo.config.appSecret" placeholder="请输入私钥" />
            </a-form-item>
        </a-col>

    </a-row>
    <!-- tableComment -->
</template>

<script setup>
import { ref, watch, getCurrentInstance } from "vue";
import useUserStore from "@/store/system/user.js";
import {
    getTablesByDataSourceId,
    getColumnByAssetId,
} from "@/api/col/task/index.js";
import { dppNoPageList } from "@/api/ast/asset/asset.js";
import { getDaDatasource, listDaDatasourceNoKafkaBySpaceCode } from "@/api/ast/dataSource/dataSource.js";
const props = defineProps({
    form: Object,
});
const emit = defineEmits(["update:form"]);

const { proxy } = getCurrentInstance();


let localForm = ref({});

watchEffect(() => {
    localForm.value = { ...props.form };

});

</script>

