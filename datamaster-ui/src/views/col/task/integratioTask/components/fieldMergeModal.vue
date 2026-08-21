<template>
  <a-modal
    v-model:open="dialogVisible"
    :draggable="true"
    class="medium-dialog"
    :title="title"
    :destroy-on-close="true"
    :footer="null"
    :width="1200"
  >
    <a-form
      ref="formRef"
      :model="form.ruleConfig.fieldMerge"
      :rules="formRules"
      :label-col="{ style: { width: '150px' } }"
    >
      <a-row :gutter="20">
        <!-- 选择字段（单选） -->
        <a-col :span="12">
          <a-form-item
            label="选择字段"
            name="sourceField"
          >
            <a-select
              v-model:value="form.ruleConfig.fieldMerge.sourceField"
              placeholder="请选择字段名称"
              show-search
              :disabled="row.columnName"
            >
              <a-select-option
                v-for="dict in tableFields"
                :key="dict.columnName"
                :label="dict.columnName"
                :value="dict.columnName"
                :disabled="usedFields.includes(dict.columnName)"
              />
            </a-select>
          </a-form-item>
        </a-col>

        <!-- 合并字段（多选） -->
        <a-col :span="12">
          <a-form-item
            label="合并字段"
            name="sourceFields"
          >
            <a-select
              v-model:value="form.ruleConfig.fieldMerge.sourceFields"
              placeholder="请选择字段名称"
              show-search
              mode="multiple"
            >
              <a-select-option
                v-for="dict in tableFields"
                :key="dict.columnName"
                :label="dict.columnName"
                :value="dict.columnName"
              />
            </a-select>
          </a-form-item>
        </a-col>

        <!-- 分隔符 -->
        <a-col :span="12">
          <a-form-item
            label="分隔符"
            name="separator"
          >
            <a-input
              v-model:value="form.ruleConfig.fieldMerge.separator"
              placeholder="请输入分隔符（不能包含中文）"
            />
          </a-form-item>
        </a-col>

        <!-- 空值处理 -->
        <a-col :span="12">
          <a-form-item
            label="空值处理"
            name="handleNull"
          >
            <a-select
              v-model:value="form.ruleConfig.fieldMerge.handleNull"
              placeholder="请选择空值处理方式"
              show-search
            >
              <a-select-option label="保留为空" value="keep" />
              <a-select-option label="替换为默认值" value="default" />
              <a-select-option label="删除该条记录" value="remove" />
            </a-select>
          </a-form-item>
        </a-col>

        <!-- 默认值（仅在选择 default 时显示） -->
        <a-col :span="12" v-if="form.ruleConfig.fieldMerge.handleNull === 'default'">
          <a-form-item
            label="默认值"
            name="defaultValue"
          >
            <a-input
              v-model:value="form.ruleConfig.fieldMerge.defaultValue"
              placeholder="请输入默认值"
            />
          </a-form-item>
        </a-col>

        <!-- 是否去除空格 -->
        <a-col :span="12">
          <a-form-item
            label="是否去除首尾空格"
            name="trimSpace"
          >
            <a-radio-group v-model:value="form.ruleConfig.fieldMerge.trimSpace">
              <a-radio :value="true">是</a-radio>
              <a-radio :value="false">否</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <template #footer>
      <div style="text-align: right">
        <a-button @click="closeDialog">关闭</a-button>
        <a-button type="primary" @click="submitForm">保存</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
import { ref, watch, computed } from "vue";

// props
const props = defineProps({
  visibleDialogs: { type: Boolean, default: true },
  title: { type: String, default: "字段合并规则配置" },
  row: { type: Object, default: () => ({}) },
  tableFields: { type: Array, default: () => [] },
  fieldFields: { type: Array, default: () => [] },
  id: { type: String, default: "" },
});
const usedFields = computed(() => {
  return props.fieldFields
    ?.map(f => f?.columnName)
    .filter(Boolean);
});



// emits
const emit = defineEmits(["update:visibleDialogs", "confirm"]);

// dialog 显示状态响应式绑定
const dialogVisible = computed({
  get: () => props.visibleDialogs,
  set: (val) => emit("update:visibleDialogs", val),
});

// 表单数据
const form = ref({
  id: props.id,
  ruleConfig: {
    fieldMerge: {
      sourceField: "",
      sourceFields: [],
      separator: "-",
      handleNull: "skip",
      defaultValue: "",
      trimSpace: true,
    },
  },
});

// 表单引用
const formRef = ref(null);

// 表单校验规则
const formRules = {
  sourceField: [{ required: true, message: "请选择字段", trigger: "change" }],
  sourceFields: [{ required: true, message: "请选择字段", trigger: "change" }],
  separator: [
    { required: true, message: "请输入分隔符", trigger: "blur" },
    {
      pattern: /^[^\u4e00-\u9fa5]+$/,
      message: "分隔符不能包含中文",
      trigger: "blur",
    },
  ],
  handleNull: [{ required: true, message: "请选择空值处理方式", trigger: "change" }],
  defaultValue: [
    {
      validator: (rule, value, callback) => {
        if (form.value.ruleConfig.fieldMerge.handleNull === "default" && !value) {
          callback(new Error("请输入默认值"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
  trimSpace: [{ required: true, message: "请选择是否去除空格", trigger: "change" }],
};

// 监听弹窗显示，弹出时初始化数据
watch(
  () => props.visibleDialogs,
  (val) => {
    if (val && props.row) {
      const list = props.row.cleanRuleList;
      if (Array.isArray(list) && list.length > 0) {
        const lastRule = list[list.length - 1];
        if (lastRule?.ruleConfig) {
          try {
            const parsed = JSON.parse(lastRule.ruleConfig);
            form.value = {
              id: props.row.elementId?.[0] || "",
              ruleConfig: parsed,
            };
          } catch (err) {
            console.error("ruleConfig JSON 解析失败:", err);
          }
        }
      } else {
        form.value = {
          id: props.row.elementId?.[0] || "",
          ruleConfig: {
            fieldMerge: {
              sourceField: "",
              sourceFields: [],
              separator: "-",
              handleNull: "skip",
              defaultValue: "",
              trimSpace: true,
            },
          },
        };
      }
    }
  }
);

// 重置表单并清除校验状态
function reset() {
  form.value = {
    id: props.id,
    ruleConfig: {
      fieldMerge: {
        sourceField: "",
        sourceFields: [],
        separator: "-",
        handleNull: "skip",
        defaultValue: "",
        trimSpace: true,
      },
    },
  };
  if (formRef.value) formRef.value.clearValidate();
}

// 关闭弹窗事件，重置表单
function closeDialog() {
  dialogVisible.value = false; // 触发 update:visibleDialogs 通知父组件关闭弹窗
  reset();
}

// 提交表单
function submitForm() {
  formRef.value.validate((valid) => {
    if (!valid) return;
    if (form.value.ruleConfig.fieldMerge.handleNull !== "default") {
      form.value.ruleConfig.fieldMerge.defaultValue = "";
    }
    const saveData = [
      {
        id: form.value.id || null,
        dataElemId: props.id,
        ruleType: 2,
        ruleId: form.value.id,
        ruleConfig: JSON.stringify(form.value.ruleConfig),
      },
    ];
    reset();
    emit("confirm", saveData);
    console.log("🚀 ~ formRef.value.validate ~ saveData:", saveData);
  });
}
</script>

<style scoped lang="scss">
.medium-dialog {
  .el-form-item {
    margin-bottom: 16px;
  }
}
</style>

