<template>
  <!-- 新增或修改数据资产地图任务对话框 -->
  <a-modal :title="title" v-model:open="visibleDialog" class="medium-dialog" destroy-on-close>
    <a-form ref="daDiscoveryTaskRef" :model="form" :label-col="{ style: { width: '110px' } }" @submit.prevent>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="资产名称" name="name" :rules="[{ required: true, message: '请输入资产名称', trigger: 'blur' },{ min: 1, max: 30, message: '长度必须介于1到20个字符之间', trigger: 'blur' }]">
            <a-input v-model:value="form.name" placeholder="请输入资产名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="所属目录" name="catCode"
            :rules="[{ required: true, message: '请输入目录编码', trigger: 'change' }]"
>
            <a-tree-select show-search tree-default-expand-all v-model:value="form.catCode" :tree-data="deptOptions"
              :field-names="{ value: 'code', label: 'name', children: 'children' }" placeholder="请选择所属目录"
/>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="所属主题" name="themeIdList"
            :rules="[{ required: true, message: '请选择主题', trigger: 'change' }]"
>
            <a-select v-model:value="form.themeIdList" mode="multiple" max-tag-count="2" placeholder="请选择主题名称">
              <a-select-option v-for="dict in themeList" :key="dict.id" :label="dict.name" :value="dict.id">{{ dict.name }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="资产状态" name="status" :rules="[
            {
              required: true,
              message: '请选择资产状态',
              trigger: 'change',
            },
          ]"
>
            <a-radio-group v-model:value="form.status">
              <a-radio value="1">未发布</a-radio>
              <a-radio value="2">已发布</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="资产类型" name="type" :rules="[
            {
              required: true,
              message: '请选择资产类型',
              trigger: 'change',
            },
          ]"
>
            <a-select v-model:value="form.type" placeholder="请输入类型" show-search :disabled="form.id"
              @change="handleTypeChange"
>
              <a-select-option v-for="dict in da_asset_type" :key="dict.value" :label="dict.label"
                :value="dict.value"
>{{ dict.label }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12" v-if="!props.isRegister">
          <a-form-item label="创建类型" name="status" :rules="[
            {
              required: true,
              message: '请选择创建类型',
              trigger: 'change',
            },
          ]"
>
            <a-radio-group v-model:value="form.createType" :disabled="form.id" @change="handleCreateChange">
              <a-radio value="1">暂不注册资产</a-radio>
              <a-radio value="2">注册资产</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述" name="description">
            <a-textarea v-model:value="form.description" :auto-size="{ minRows: 8 }" placeholder="请输入描述" />
          </a-form-item>
        </a-col>
      </a-row>
      <!-- <a-divider v-if="form.id != undefined || form.createType == '2'">
        <span class="blue-text">参数配置</span>
      </a-divider> -->
      <component :is="currentFormComponent" v-model:form="form" ref="ApiConfigRef" v-if="form.createType == '2'"
        :isRegister="props.isRegister" :type="props.type"
/>
      <a-row :gutter="20" v-if="form.type == '111' && (form.id != undefined || form.createType == '2')">
        <a-col :span="12">
          <a-form-item label="文件类型" name="assetsAssetGeo.fileType"
            :rules="[{ required: true, message: '请输入文件类型', trigger: 'blur' }]"
>
            <a-select v-model:value="form.assetsAssetGeo.fileType" placeholder="请选择参数类型">
              <a-select-option v-for="dict in da_asset_geo_file_type" :key="dict.value" :label="dict.label"
                :value="dict.value"
/>
            </a-select>
            <!-- <a-input v-model:value="form.assetsAssetGeo.fileType" placeholder="请输入文件类型" /> -->
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="上传文件" name="assetsAssetGeo.fileUrl"
            :rules="[{ required: true, message: '请上传文件', trigger: 'fileUrl' }]"
>
            <FileUploadbtn :limit="1" v-model="form.assetsAssetGeo.fileUrl" :dragFlag="false" :fileType="['geojson']"
              :fileSize="50" :isShowTip="false" v-model:fileSize="form.fileSize" v-model:fileExt="form.fileType"
/>
          </a-form-item>
        </a-col>
      </a-row>
      <excelAdd ref="excelAddRef" />
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="form.remark" :auto-size="{ minRows: 8 }" placeholder="请输入备注" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <template #footer>
      <div class="dialog-footer">
        <!-- 关闭按钮 -->
        <a-button @click="closeDialog">取消</a-button>
        <!-- 保存按钮 -->
        <a-button type="primary" @click="saveData" :loading="loading">确定</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
import { message } from 'ant-design-vue'
import { defineProps, defineEmits, ref, computed, watch } from "vue";
// import { message } from 'ant-design-vue'
import Crontab from "@/components/Crontab/index.vue";
// import { message } from 'ant-design-vue'
import { getDaDiscoveryTask } from "@/api/ast/discovery/discoveryTask";
// 数据库表

import tableConfigForm from "./tableAdd.vue";
// 视频

import daAssetVideo from "./videoAdd.vue";
// 矢量数据 上传

import excelAdd from "./excelAdd.vue";
// 非结构化数据

import Unstructured from "./unstructuredAdd.vue";

import { getThemeList } from "@/api/tax/theme/theme.js";

import useUserStore from "@/store/system/user";
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
const { da_asset_type, da_asset_geo_file_type, da_asset_video_platform } = proxy.useDict(
  "da_asset_type",
  "da_asset_geo_file_type",
  "da_asset_video_platform"
);

import { addDaAsset, updateDaAsset, bindResources } from "@/api/ast/asset/asset";
// import { message } from 'ant-design-vue'
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  data: { type: Object, default: () => ({}) },
  deptOptions: { type: Object, default: () => ({}) },
  type: { type: Boolean, default: false },
  isEdit: { type: Boolean, default: false },
  isRegister: { type: Boolean, default: false },
});
const excelAddRef = ref();
let loading = ref(false); // 加载状态（全局）
const emit = defineEmits(["update:visible", "confirm"]);
const currentFormComponent = computed(() => {
  switch (form.value.type) {
    case "1":
      return tableConfigForm;
    // case "5":
    //   return daAssetVideo;
    // case "6":
    //   return excelAdd;
    case "7":
      return Unstructured;
    default:
      return null;
  }
});
let themeList = ref([]);
async function getAssetThemeList() {
  const response = await getThemeList();
  themeList.value = response.data;
  excelAddRef.value.show(form.value);
}
// eslint-disable-next-line no-unused-vars
const createTypeList = ref([]); // 数据源列表
// const getDatasourceList = async () => {
//   try {
//     loading.value = true;
//     const response = await listDaDatasourceNoKafkaBySpaceCode({
//       spaceCode: userStore.spaceCode,
//       spaceId: userStore.spaceId,
//     });
//     createTypeList.value = response.data || [];
//   } finally {
//     loading.value = false;
//   }
// };
let openCron = ref(false);
const expression = ref("");
/** 调度周期按钮操作 */
// eslint-disable-next-line no-unused-vars
function handleShowCron() {
  expression.value = form.value.cronExpression;
  openCron.value = true;
}
/** 确定后回传值 */
// eslint-disable-next-line no-unused-vars
function crontabFill(value) {
  form.value.cronExpression = value;
}
// 创建一个本地响应式数据，用来修改表单内容
const form = ref({
  type: "1",
  createType: "2",
  catCode: "",
  sourceType: "0",
  name: "",
  themeIdList: [],
    status: "1",
    description: "",
    remark: "",
    source: "3",
  // 1
  tableName: "",
  tableId: null,
  datasourceId: null,
  tableComment: "",
  datasourceType: "",
  dbname: "",
  dataCount: null,
  fieldCount: null,
  assetColumnList: [],
  // 4
  assetsAssetGeo: {
    fileUrl: "",
    fileType: "",
    elementType: "",
    coordinateSystem: "",
    example: "",
    fileName: "",
  },
  assetsAssetVideo: {
    ip: "",
    port: "",
    protocol: "",
    platform: "",

    config: {
      cameraName: "",
      cameraCode: "",
      appkey: "",
      appSecret: "",
      artemisPath: "",
    },
  },
  assetsAssetFiles: {
    url: null,
    startData: "",
    tableFields: [],
    startColumn: "",
  },
  //   7
  fileInfo: {},
  filePath: "",
});
watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      getAssetThemeList();
      if (props.data.id) {
        // props.data.id
      } else {
        form.value.catCode = props.data.catCode || "";
        console.log("🚀 ~ props.data:", props.data.catCode);
      }
    } else {
      clearForm();
    }
  }
);
const handleTypeChange = () => {
  // 清空表格字段
  form.value.tableName = "";
  form.value.datasourceId = null;
  form.value.tableComment = "";
  form.value.datasourceType = "";
  form.value.dbname = "";
  form.value.assetsAssetGeo = {
    fileUrl: "",
    fileType: "",
    elementType: "",
    coordinateSystem: "",
    example: "",
    fileName: "",
  };
  form.value.assetsAssetVideo = {
    ip: "",
    port: "",
    protocol: "",
    platform: "",
    cameraName: "",
    cameraCode: "",
    artemisPath: "",
    config: {
      cameraName: "",
      cameraCode: "",
      appkey: "",
      appSecret: "",
      artemisPath: "",
    },
  };
  form.value.assetsAssetFiles = {
    url: null,
    startData: "",
    tableFields: [],
    startColumn: "",
  };
  excelAddRef.value && excelAddRef.value.show(form.value);
};
const handleCreateChange = () => {
  excelAddRef.value && excelAddRef.value.show(form.value);
};
const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit("update:visible", newValue);
  },
});

// 关闭对话框的方法
const closeDialog = async () => {
  emit("update:visible", false);
};

watch(
  () => props.data,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      if (props.data.id) {
        form.value = JSON.parse(JSON.stringify(newVal));
        form.value.datasourceId = form.value.datasourceId === "" || form.value.datasourceId == null
          ? null
          : Number(form.value.datasourceId);
        form.value.misfirePolicy = Number(form.value.misfirePolicy) || "";
        form.value.concurrent = Number(form.value.concurrent) || "";
        // form.value.status = Number(form.value.status) || "";
        form.value.themeIdList = form.value?.assetsAssetThemeRelList?.map((item) => Number(item.themeId)) || [];
        form.value.createType = props.isRegister ? "2" : form.value.createType;
        // 视频配置处理
        if (props.data.type == "5") {
          if (form.value.assetsAssetVideo == null) {
            form.value.assetsAssetVideo = {
              ip: "",
              port: "",
              protocol: "",
              platform: "",
              cameraName: "",
              cameraCode: "",
              artemisPath: "", //服务上下文
              config: JSON.stringify({
                cameraName: "",
                cameraCode: "",
                artemisPath: "",
              }),
            };
          } else {
            form.value.assetsAssetVideo.config = JSON.parse(props.data?.assetsAssetVideo?.config);
          }
        }
        if (form.value.assetsAssetGeo == null) {
          form.value.assetsAssetGeo = {
            fileUrl: "",
            fileType: "",
            elementType: "",
            coordinateSystem: "",
            example: "",
            fileName: "",
          };
        }
      }
    }
  },
  {
    immediate: true,
    deep: true,
  }
);

function getFormDataByType(type) {
  const commonFields = {
    type: form.value.type,
    catCode: form.value.catCode,
    sourceType: form.value.sourceType,
    name: form.value.name,
    status: form.value.status,
    createType: form.value.createType,
    description: form.value.description,
    remark: form.value.remark,
    source: form.value.source,
    id: form.value.id,
    themeIdList: form.value.themeIdList,
  };

  switch (type) {
    case "1":
      return {
        ...commonFields,
        tableName: form.value.tableName,
        tableId: form.value.tableId,
        datasourceId: form.value.datasourceId,
        tableComment: form.value.tableComment,
        datasourceType: form.value.datasourceType,
        dbname: form.value.dbname,
        dataCount: form.value.dataCount,
        fieldCount: form.value.fieldCount,
        assetColumnList: form.value.assetColumnList || [],
      };
    case "4":
      return {
        ...commonFields,
        assetsAssetGeo: { ...form.value.assetsAssetGeo },
      };
    case "5":
      return {
        ...commonFields,
        assetsAssetVideo: {
          ip: form.value.assetsAssetVideo.ip,
          port: form.value.assetsAssetVideo.port,
          protocol: form.value.assetsAssetVideo.protocol,
          platform: form.value.assetsAssetVideo.platform,
          cameraName: form.value.assetsAssetVideo.config.cameraName,
          cameraCode: form.value.assetsAssetVideo.config.cameraCode,
          artemisPath: form.value.assetsAssetVideo.config.artemisPath,
          config: JSON.stringify({
            cameraName: form.value.assetsAssetVideo.config.cameraName,
            cameraCode: form.value.assetsAssetVideo.config.cameraCode,
            artemisPath: form.value.assetsAssetVideo.config.artemisPath,
          }),
        },
      };
    case "6":
      return {
        ...commonFields,

        assetsAssetFiles: {
          url: proxy.$refs.excelAddRef.form.assetsAssetFiles.url,
          startData: proxy.$refs.excelAddRef.form.assetsAssetFiles.startData,
          startColumn: proxy.$refs.excelAddRef.form.assetsAssetFiles.startColumn,
          type: proxy.$refs.excelAddRef.form.assetsAssetFiles.type,
          name: proxy.$refs.excelAddRef.form.assetsAssetFiles.name,
        },
      };
    case "7": {
      const fileInfo = ApiConfigRef.value ? Object.fromEntries(ApiConfigRef.value.fileDesc.map((item) => [item.key, item.value])) : {};
      return {
        ...commonFields,
        datasourceId: form.value.datasourceId,
        fileInfo: fileInfo,
        filePath: fileInfo.path,
      };
    }
    default:
      return commonFields;
  }
}

let daDiscoveryTaskRef = ref(); // 保存数据的方法
let ApiConfigRef = ref();
const saveData = async () => {
  loading.value = true; // 开始加载
  try {
    const valid = await proxy.$refs["daDiscoveryTaskRef"].validate();
    if (valid) {
      if (props.data.type == "5") {
        if (form.value.assetsAssetVideo == null) {
          form.value.assetsAssetVideo = {
            ip: "",
            port: "",
            protocol: "",
            platform: "",
            cameraName: "",
            cameraCode: "",
            artemisPath: "", //服务上下文
            config: JSON.stringify({
              cameraName: "",
              cameraCode: "",
              artemisPath: "",
            }),
          };
        } else {
          form.value.assetsAssetVideo.config = JSON.stringify(form.value?.assetsAssetVideo?.config);
        }
      }
      form.value = getFormDataByType(form.value.type);
      if (form.value.id != null) {
        if (form.value.createType == "1") {
          await bindResources(form.value);
          proxy.$modal.msgSuccess("配置成功");
        } else {
          await updateDaAsset(form.value);
          proxy.$modal.msgSuccess("修改成功");
        }
      } else {
        let payload = {
          ...form.value,
        };
        payload.spaceCode = userStore.spaceCode;
        payload.spaceId = userStore.spaceId;
        await addDaAsset({
          ...payload,
        });
        proxy.$modal.msgSuccess("新增成功，已提交审核");
      }
      emit("update:visible", false);
      emit("confirm", form.value);
    } else {
      proxy.$message.warning("验证失败，请检查表单信息");
    }
  } finally {
    loading.value = false; // 结束加载
  }
};

// 清空表单数据
const clearForm = () => {
  form.value = {
    type: "1",
    catCode: "",
    sourceType: "0",
    name: "",
    themeIdList: [],
    status: '1',
    createType: "2",
    description: "",
    remark: "",
    source: "3",
    // 1
    tableName: "",
    tableId: null,
    datasourceId: null,
    tableComment: "",
    datasourceType: "",
    dbname: "",
    dataCount: null,
    fieldCount: null,
    assetColumnList: [],
    // 4
    assetsAssetGeo: {
      fileUrl: "",
      fileType: "",
      elementType: "",
      coordinateSystem: "",
      example: "",
      fileName: "",
    },
    // 5
    assetsAssetVideo: {
      ip: "",
      port: "",
      protocol: "",
      platform: "",
      cameraName: "",
      cameraCode: "",
      artemisPath: "", //服务上下文
      config: JSON.stringify({
        cameraName: "",
        cameraCode: "",
        artemisPath: "",
      }),
    },
    assetsAssetFiles: {
      url: null,
      startData: "",
      startColumn: "",
      tableFields: [],
    },
    //   7
    filePath: "",
  };
};
</script>

<style scoped lang="less">
.blue-text {
  color: #2666fb;
}
</style>

