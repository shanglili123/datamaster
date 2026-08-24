<template>
  <a-modal v-model:open="visibleDialog" class="dialog" :title="title" destroy-on-close width="60%">
    <a-form ref="daDiscoveryTaskRef" :model="form" :label-col="{ style: { width: '120px' } }" @submit.prevent
      :disabled="title == '任务详情'">
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="任务名称" name="name"
            :rules="[{ required: title != '任务详情', message: '请输入任务名称', trigger: 'blur' }]">
            <a-input v-if="title != '任务详情'" v-model:value="form.name" placeholder="请输入任务名称" />
            <div class="form-readonly" v-else>{{ form.name }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="数据开发目录" name="catCode" :rules="[
            {
              required: title != '任务详情',
              message: '请选择数据开发目录',
              trigger: 'change',
            },
          ]">
            <a-tree-select show-search v-model:value="form.catCode" :tree-data="deptOptions"
              :field-names="{ value: 'code', label: 'name', children: 'children' }" placeholder="请选择数据开发目录" />
          </a-form-item>
        </a-col>
      </a-row>


      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="调度周期" name="crontab">
            <a-input v-if="title != '任务详情'" v-model:value="form.crontab" placeholder="请输入调度周期" readonly>
              <template #addonAfter>
                <a-button type="primary" @click="handleShowCron" style="background-color: #2666fb; color: #fff">
                  配置
                  <template #icon><ClockCircleOutlined /></template>
                </a-button>
              </template>
            </a-input>
            <div class="form-readonly" v-else>{{ form.crontab }}</div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="执行引擎" name="typaCode"
            :rules="[{ required: title != '任务详情', message: '请选择执行引擎', trigger: 'change' }]">
            <a-tree-select show-search :disabled="info" v-model:value="form.typaCode" :tree-data="treeData"
              :field-names="{ value: 'value', label: 'label', children: 'children' }"
              @change="getDaDatasource(true)" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="20">
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述" name="description">
            <a-textarea v-if="title != '任务详情'" v-model:value="form.description" placeholder="请输入描述" />
            <div class="form-readonly" v-else>{{ form.description || '-' }}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="任务状态" name="releaseState"><a-radio-group style=" width: 100%"
              v-model:value="form.releaseState" class="el-form-input-width" v-if="title != '任务详情'">
              <a-radio v-for="dict in dpp_etl_task_status" :key="dict.value" :value="dict.value"
                :disabled="dict.value == 1">
                {{ dict.label }}
              </a-radio>
            </a-radio-group>
            <div class="form-readonly" v-else>{{dpp_etl_task_status.find(item => item.value ==
              form.releaseState)?.label ||
              '-'}}</div>
          </a-form-item>
        </a-col>
      </a-row>
      <div class="h2" @click="templateShow = !templateShow">> 使用模板</div>
      <template v-if="templateAct.id || templateShow">
        <a-spin :spinning="tempLoading">
          <div class="h2-template">
            <div class="h2-item" :class="{ act: templateAct.id == item.id }" v-for="item in templateList" :key="item.id"
              @click="handleTemplate(item)">
              <div class="h2-item-title">{{ item.name }}</div>
              <div class="h2-item-editor">
                <CodeShow v-model="item.content" :config="{
                  renderSideBySide: false,
                  fontSize: 9,
                  scrollbar: {
                    vertical: 'hidden',
                    horizontal: 'hidden',
                  },
                }" />
              </div>
            </div>
            <a-empty style="width: 100%" v-if="total == 0" description="暂无数据" />
          </div>
        </a-spin>
        <pagination layout="prev, pager, next" v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize" @pagination="getList" />
      </template>
    </a-form>

    <template #footer>
      <div style="text-align: right">
        <template v-if="info">
          <a-button @click="closeDialog">关闭</a-button>
          <a-button type="primary" @click="saveClose" v-if="!route.query.info">保存</a-button>
        </template>
        <template v-else>
          <a-button @click="saveClose">仅保存</a-button>
          <a-button type="primary" @click="saveData">保存并配置流程</a-button>
        </template>
      </div>
    </template>
  </a-modal>
  <a-modal title="Cron表达式生成器" v-model:open="openCron" class="dialog" destroy-on-close :width="700">
    <crontab ref="crontabRef" @hide="openCron = false" @fill="crontabFill" :expression="expression"> </crontab>
  </a-modal>
</template>

<script setup>
import { defineProps, defineEmits, ref, computed, watch } from "vue";
import CodeShow from "@/components/SqlEditor/editorShow/index.vue";
import Crontab from "@/components/Crontab/index.vue";
import { useRoute, useRouter } from "vue-router";
import { ClockCircleOutlined } from "@ant-design/icons-vue";
const route = useRoute();
const { proxy } = getCurrentInstance();
import { dppEtlSqlTemp, getNodeUniqueKey } from "@/api/col/task/index.js";
import { listDaDatasourceNoKafkaBySpaceCode } from "@/api/ast/dataSource/dataSource";
const { dpp_etl_task_status } = proxy.useDict("dpp_etl_task_status");
import useUserStore from "@/store/system/user";
const userStore = useUserStore();
import { treeData } from "@/views/col/task/developTask/data";
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  data: { type: Object, default: () => ({}) },
  deptOptions: { type: Object, default: () => ({}) },
  info: { type: Boolean, default: false },
});

const emit = defineEmits(["update:visible", "confirm"]);

const form = ref({
  // 表单数据
  name: "",
  catCode: "",
  crontab: "",
  releaseState: "0",
  description: "",
  // json值
  typaCode: "SQL",
  // 固定值
  executionType: "PARALLEL", // 初始化为空或默认值
  status: "0",
  datasources: { datasourceId: "" },
});
const total = ref(0);
const queryParams = ref({
  pageNum: 1,
  pageSize: 6,
});
const tempLoading = ref(false);
const TEMPLATE_TYPE_MAP = {
  SQL: 2,
  PROCEDURE: 3,
  FLINK: 4,
  SHELL: 5,
};
const getList = async () => {
  tempLoading.value = true;
  try {
    let type = TEMPLATE_TYPE_MAP[form.value.typaCode] || 2;
    let params = {
      ...queryParams.value,
      type: type,
    };
    dppEtlSqlTemp(params).then((response) => {
      templateList.value = response.data.rows;
      total.value = response.data.total;
    });
  } finally {
    tempLoading.value = false;
  }
};

const templateShow = ref(true);
const templateAct = ref({
  id: "",
  sqlData: { content: "" },
  queryParams: queryParams.value,
  typaCode: "DM",
});
const templateList = ref([]);
const handleTemplate = (item) => {
  templateAct.value = {
    id: item.id,
    sqlData: item,
    queryParams: queryParams.value,
    typaCode: form.value.typaCode,
  };
};

let loading = ref(false);
let createTypeList = ref([]);

/** 查询数据源列表（按引擎类型决定是否展示） */
function getDaDatasource(flag) {
  templateAct.value.typaCode = form.value.typaCode;
  // 刷新模板列表
  getList();
  var needDatasource = (form.value.typaCode == "SQL" || form.value.typaCode == "PROCEDURE");
  if (!needDatasource) {
    createTypeList.value = [];
    return;
  }
  loading.value = true;
  listDaDatasourceNoKafkaBySpaceCode({
    spaceCode: userStore.spaceCode,
    spaceId: userStore.spaceId,
  }).then((response) => {
    createTypeList.value = response.data;
    if (flag) {
      form.value.datasources.datasourceId = "";
    }
    loading.value = false;
  });
}
watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      form.value = { ...form.value, ...props.data };
      // 模版
      templateAct.value = form.value.draftJson ? JSON.parse(form.value.draftJson) : { ...templateAct.value };
      // 获取模版列表
      queryParams.value = templateAct.value.queryParams || queryParams.value;
      // 执行引擎
      form.value.typaCode = templateAct.value.typaCode;
      getDaDatasource();
      getList();
      // 任务状态
      if (form.value.status != null && form.value.status != undefined) {
        form.value.releaseState = form.value.status == "-1" ? "0" : form.value.status;
      }
    } else {
      proxy.resetForm("daDiscoveryTaskRef");
    }
  }
);

// 计算属性处理 v-model
const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit("update:visible", newValue);
  },
});

let daDiscoveryTaskRef = ref();
// 关闭对话框的方法
const closeDialog = () => {
  emit("update:visible", false);
};
const applyCurrentUserAsCreator = (target) => {
  target.creatorId = userStore.id;
  target.createBy = userStore.nickName || userStore.name;
};
const saveClose = async () => {
  try {
    const valid = await daDiscoveryTaskRef.value.validate();
    if (valid) {
      if (!form.value.code) {
        const response = await getNodeUniqueKey({
          spaceCode: userStore.spaceCode || "133545087166112",
          spaceId: userStore.spaceId,
        });
        if (response && response.data) {
          form.value.code = response.data; // Set unique code
        }
      }
      const formData = JSON.parse(JSON.stringify(form.value));
      applyCurrentUserAsCreator(formData);
      formData.draftJson = JSON.stringify(templateAct.value);
      console.log("🚀 ~ saveData ~ formData:", formData);
      emit("save", formData);
      emit("update:visible", false);
    } else {
      console.log("表单校验未通过");
    }
  } catch (error) {
    console.error("保存数据时出错:", error);
  }
};
// 保存数据的方法
const saveData = async () => {
  try {
    const valid = await daDiscoveryTaskRef.value.validate();
    if (valid) {
      if (!form.value.code) {
        const response = await getNodeUniqueKey({
          spaceCode: userStore.spaceCode || "133545087166112",
          spaceId: userStore.spaceId,
        });
        if (response && response.data) {
          form.value.code = response.data; // Set unique code
        }
      }
      const formData = JSON.parse(JSON.stringify(form.value));
      applyCurrentUserAsCreator(formData);
      formData.draftJson = JSON.stringify(templateAct.value);
      console.log("🚀 ~ saveData ~ formData:", formData);
      emit("confirm", formData);
      emit("update:visible", false);
    } else {
      console.log("表单校验未通过");
    }
  } catch (error) {
    console.error("保存数据时出错:", error);
  }
};

let openCron = ref(false);
const expression = ref("");
/** 调度周期按钮操作 */
function handleShowCron() {
  expression.value = form.value.crontab;
  openCron.value = true;
}
/** 确定后回传值 */
function crontabFill(value) {
  form.value.crontab = value;
}
// 定义表单验证规则额
</script>
<style scoped lang="less">
.blue-text {
  color: #2666fb;
}

:deep(.ant-select) {
  &.ant-select-disabled {
    cursor: default;
    background-color: #fcfcfc;
    color: #333;

    .ant-select-suffix {
      display: none;
    }
  }
}

.h2 {
  user-select: none;
  cursor: pointer;
  font-size: 14px;
  color: #2666fb;

  &:hover {
    color: #4a86fc;
  }
}

.h2-template {
  display: flex;
  flex-flow: row wrap;
  margin-top: 10px;
  background: #f8f9fa;
  padding: 10px;
  gap: 10px;
  border-radius: 6px;

  .h2-item {
    position: relative;
    width: 32.6%;
    border: 1px solid rgba(5, 5, 5, 0.06);
    border-radius: 6px;
    transition: box-shadow 0.3s, border-color 0.3s;

    &:hover {
      border-color: transparent;
      box-shadow: 0 1px 2px -2px #00000029, 0 3px 6px #0000001f, 0 5px 12px 4px #00000017;
    }

    &.act {

      .h2-item-title,
      .h2-item-editor {
        background: #e6f7ff;
      }

      &::after {
        visibility: visible;
        position: absolute;
        inset-block-start: 2px;
        inset-inline-end: 2px;
        opacity: 1;
        width: 0;
        height: 0;
        border: 6px solid #1890ff;
        border-block-end: 6px solid transparent;
        border-inline-start: 6px solid transparent;
        border-start-end-radius: 2px;
        content: "";
      }
    }

    .h2-item-title {
      background: #fff;
      padding: 8px 12px 0;
      font-size: 14px;
      color: #000000e0;
    }

    .h2-item-editor {
      background: #fff;
      padding: 8px;
      height: 150px;
    }
  }
}
</style>

