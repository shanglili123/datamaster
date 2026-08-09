<template>
  <!-- 字符串类型校验 -->
  <a-form
    ref="formRef"
    :model="form"
    :rules="falg ? {} : rules"
    :label-col="{ style: { width: '130px' } }"
    :disabled="false"
  >
    <a-row>
      <a-col :span="12">
        <!-- 评测对象下拉 -->
        <a-form-item label="使用正则" name="useRegexFlag">
          <a-checkbox
            v-if="!falg"
            v-model:checked="form.useRegexFlag"
            :checked-value="1"
            :un-checked-value="0"
            >使用正则</a-checkbox
          >
          <div v-else class="form-readonly">
            {{ form.useRegexFlag == 1 ? "使用正则" : "不使用正则" }}
          </div>
        </a-form-item>
      </a-col>
      <a-col :span="12" v-if="!form.useRegexFlag" class="hasMsg">
        <a-form-item label="允许字符类型" name="allowedChars">
          <template v-if="!falg">
            <a-checkbox-group v-model:value="form.allowedChars" name="chars">
              <a-checkbox :value="'1'">数字</a-checkbox>
              <a-checkbox :value="'2'">字母</a-checkbox>
              <a-checkbox :value="'3'">空格</a-checkbox>
              <a-checkbox :value="'4'">特殊符号</a-checkbox>
            </a-checkbox-group>
          </template>
          <div v-else class="form-readonly">{{ allowedCharsText }}</div>
          <span class="msg"
            ><InfoFilled /> 若选中数字和字母，系统自动识别为“仅允许字母与数字的组合</span
          >
        </a-form-item>
      </a-col>
      <a-col :span="12" v-if="!form.useRegexFlag">
        <a-form-item label="忽略空值" name="ignoreNullValue">
          <a-radio-group v-if="!falg" v-model:value="form.ignoreNullValue">
            <a-radio :value="'1'">是</a-radio>
            <a-radio :value="'0'">否</a-radio>
          </a-radio-group>
          <div v-else class="form-readonly">
            {{
              form.ignoreNullValue === "1"
                ? "是"
                : form.ignoreNullValue === "0"
                ? "否"
                : "-"
            }}
          </div>
        </a-form-item>
      </a-col>
      <a-col :span="12" v-if="form.useRegexFlag">
        <a-form-item
          label="正则表达式"
          name="regex"
          :rules="[
            {
              required: form.useRegexFlag,
              message: '请输入正则表达式',
              trigger: 'blur',
              validator: (rule, value, callback) => {
                if (form.useRegexFlag && !value) {
                  callback(new Error('请输入正则表达式'));
                } else {
                  callback();
                }
              },
            },
          ]"
        >
          <a-input
            v-if="!falg"
            v-model:value="form.regex"
            placeholder="请输入正则表达式"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.regex || "-" }}</div>
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { getColumnByAssetId } from "@/api/col/task/index.js";

import { InfoCircleFilled as InfoFilled } from "@ant-design/icons-vue";

const props = defineProps({
  form: Object,
  dppQualityTaskObjSaveReqVO: Array,
  falg: Boolean,
});
const emit = defineEmits(["update:form"]);

const formRef = ref(null);

const form = reactive({ ...props.form });
const columnList = ref([]);
const allowedCharsText = computed(() => {
  const map = { 1: "数字", 2: "字母", 3: "空格", 4: "特殊符号" };
  return Array.isArray(form.allowedChars) && form.allowedChars.length
    ? form.allowedChars.map((v) => map[v] || v).join(", ")
    : "-";
});
watch(
  () => form.useRegexFlag,
  (val) => {
    if (val) {
      form.allowedChars = ["1"];
      form.ignoreNullValue = "1";
    }
  }
);
// 表单校验规则
const rules = {
  regex: [
    {
      validator: (rule, value, callback) => {
        if (form.useRegexFlag && !value) {
          callback(new Error("请输入正则表达式"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
  allowedChars: [
    {
      type: "array",
      required: true,
      min: 1,
      message: "请选择允许的字符类型",
      trigger: "change",
    },
  ],
  ignoreNullValue: [
    { required: true, message: "请选择忽略空值", trigger: "change" },
  ],
};

const exposedFields = [
  "useRegexFlag",
  "allowedChars",
  "ignoreNullValue",
  "regex",
];

function validate() {
  return new Promise((resolve, reject) => {
    formRef.value.validate().then(() => {
      const result = Object.fromEntries(
        exposedFields.map((key) => [key, form[key]])
      );
      resolve({ valid: true, data: result });
    }).catch(() => {
      resolve({ valid: false });
    });
  });
}

defineExpose({ validate });
</script>
<style scoped>
.rule-half {
  width: 100%;
}
.form-readonly {
  min-height: 32px;
  line-height: 32px;
}
</style>

