<template>
    <a-modal :title="title" v-model:open="visible" class="warn-dialog" draggable>
        <a-form ref="formRef" :model="form" :label-col="{ style: { width: '100px' } }" @submit.prevent>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="标准号" name="code" :rules="[
                        { required: true, message: '标准号不能为空', trigger: 'blur' }
                    ]">
                        <a-input v-model:value="form.code" placeholder="请输入标准号" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="标准名称" name="name" :rules="[
                        { required: true, message: '标准名称不能为空', trigger: 'blur' }
                    ]">
                        <a-input v-model:value="form.name" placeholder="请输入标准名称" />
                    </a-form-item>
                </a-col>
            </a-row>

            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="标准级别" name="stdLevel" :rules="[
                        { required: true, message: '标准级别不能为空', trigger: 'change' }
                    ]">
                        <a-select style="width: 100%;" v-model:value="form.stdLevel"
                            placeholder="请选择标准级别">
                            <a-select-option label="国家标准" value="国家标准" />
                            <a-select-option label="行业标准" value="行业标准" />
                            <a-select-option label="地方标准" value="地方标准" />
                            <a-select-option label="团体标准" value="团体标准" />
                        </a-select>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="标准状态" name="status" :rules="[
                        { required: true, message: '标准状态不能为空', trigger: 'blur' }
                    ]">
                        <a-select style="width: 100%;" class="el-form-input-width" v-model:value="form.status"
                            placeholder="请选择标准状态">
                            <a-select-option v-for="dict in dp_document_status" :key="dict.value" :label="dict.label"
                                :value="dict.value">{{ dict.label }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="标准目录" name="catCode" :rules="[
                        { required: true, message: '标准目录不能为空', trigger: 'blur' }
                    ]">
                        <a-tree-select show-search v-model:value="form.catCode" :tree-data="deptOptions"
                            :field-names="{ value: 'code', label: 'name', children: 'children' }"
                            placeholder="请选择标准目录" tree-check-strictly />
                    </a-form-item>
                </a-col>
            </a-row>

            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="文件" name="fileUrl" :rules="[
                        { required: true, message: '文件不能为空', trigger: 'change' }
                    ]">
                        <FileUploadbtn :limit="1" v-model:filename="form.fileName" v-model="form.fileUrl"
                            :dragFlag="false" :fileSize="100" @handleRemove="handleRemove" :isShowTip="false" />
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
                    <a-form-item label="发布机构名称" name="issuingAgency">
                        <a-input v-model:value="form.issuingAgency" placeholder="请输入发布机构名称" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="版本号" name="version">
                        <a-input v-model:value="form.version" placeholder="请输入版本号" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="发布日期" name="releaseDate">
                        <a-date-picker allow-clear style="width: 100%" v-model:value="form.releaseDate"
                            value-format="YYYY-MM-DD" placeholder="请选择发布日期">
                        </a-date-picker>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="实施日期" name="implementationDate">
                        <a-date-picker allow-clear style="width: 100%" v-model:value="form.implementationDate"
                            value-format="YYYY-MM-DD" placeholder="请选择实施日期">
                        </a-date-picker>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="废止日期" name="abolitionDate">
                        <a-date-picker allow-clear style="width: 100%" v-model:value="form.abolitionDate"
                            value-format="YYYY-MM-DD" placeholder="请选择废止日期">
                        </a-date-picker>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="备注" name="remark">
                        <a-input v-model:value="form.remark" type="textarea" placeholder="请输入备注" />
                    </a-form-item>
                </a-col>
            </a-row>
        </a-form>

        <template #footer>
            <div class="dialog-footer">
                <a-button size="small" @click="close">取消</a-button>
                <a-button type="primary" size="small" @click="submitForm" :loading="loading">确定</a-button>
            </div>
        </template>
    </a-modal>
</template>

<script setup>
import { ref, reactive, nextTick, getCurrentInstance } from "vue";
const { proxy } = getCurrentInstance();
import FileUploadbtn from "@/components/FileUploadbtn/index1.vue";
import { addDpDocument, updateDpDocument } from "@/api/std/document/document";
const { column_type, sys_disable, dp_document_status } = proxy.useDict(
    "column_type",
    "sys_disable",
    "dp_document_status"
);

let deptOptions = ref([]);
const visible = ref(false);
const formRef = ref(null);
const loading = ref(false);   // 提交按钮 loading

const form = reactive({
    id: null,
    code: "",
    catCode: "",
    name: "",
    status: "1",
    stdLevel: "",
    standardUrl: "",
    issuingAgency: "",
    version: "",
    releaseDate: "",
    implementationDate: "",
    abolitionDate: "",
    fileName: "",
    fileUrl: "",
    remark: ""
});

const type = ref('1');

const title = ref("标准弹窗");
const emit = defineEmits(["update-success"]);

/** 打开弹窗 */
function openModal(formData = {}, options = [], types) {
    deptOptions.value = options;
    type.value = types

    if (formData && formData.id) {
        Object.assign(form, formData);
        form.catCode = form.catCode != null ? String(form.catCode) : "";
        form.status = form.status != null ? String(form.status) : "";
        title.value = "修改" + (form.stdLevel || "标准");
    } else {
        clearForm();
        title.value = "新增" + (form.stdLevel || "标准");
    }

    visible.value = true;
    nextTick(() => formRef.value?.clearValidate());
}

/** 关闭弹窗 */
function close() {
    visible.value = false;
    clearForm();
}

/** 清空表单 */
function clearForm() {
    form.id = null;
    form.code = "";
    form.name = "";
    form.status = "1";
    form.stdLevel = "";
    form.standardUrl = "";
    form.issuingAgency = "";
    form.catCode = "";
    form.version = "";
    form.releaseDate = "";
    form.implementationDate = "";
    form.abolitionDate = "";
    form.fileName = "";
    form.fileUrl = "";
    form.remark = "";
    form.description = "";

    nextTick(() => formRef.value?.clearValidate());
}
/** 提交表单 */
function submitForm() {
    formRef.value.validate().then(() => {
        loading.value = true;

        const apiCall = form.id ? updateDpDocument : addDpDocument;
        apiCall({ ...form, type: type.value })
            .then(() => {
                proxy.$modal.msgSuccess(form.id ? "修改成功" : "新增成功");
                visible.value = false;
                clearForm();
                emit("update-success");
            })
            .finally(() => {
                loading.value = false;
            });
    }).catch(() => {});
}

/** 文件移除 */
function handleRemove(file) {
    form.standardUrl = null;
    form.fileUrl = "";
}

defineExpose({ openModal, close });
</script>

