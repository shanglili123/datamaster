<template>
  <!-- 正则表达式替换   -->
  <a-form
    ref="formRef"
    :model="form"
    :rules="rules"
    :label-col="{ style: { width: '130px' } }"
    :disabled="false"
  >
    <a-row>
      <a-col :span="12">
        <a-form-item
          label="正则表达式"
          name="regex"
          :rules="
            !falg
              ? [
                  {
                    required: true,
                    message: '请输入正则表达式',
                    trigger: 'blur',
                  },
                ]
              : []
          "
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
      <a-col :span="12">
        <a-form-item
          label="替换为"
          name="replacement"
          :rules="
            !falg
              ? [{ required: true, message: '请输入替换内容', trigger: 'blur' }]
              : []
          "
        >
          <a-input
            v-if="!falg"
            v-model:value="form.replacement"
            placeholder="请输入替换内容"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.replacement || "-" }}</div>
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { getColumnByAssetId } from "@/api/col/task/index.js";

const props = defineProps({
  form: Object,
  inputFields: Array,
  falg: Boolean,
});
const emit = defineEmits(["update:form"]);

const formRef = ref(null);
const form = reactive({ ...props.form });
// 表单校验规则
const rules = {
  regex: [
    {
      validator: (rule, value, callback) => {
        if (form.pattern && !value) {
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
const exposedFields = ["regex", "replacement"];
function validate() {
  return new Promise((resolve, reject) => {
    formRef.value.validate((valid) => {
      if (valid) {
        const result = Object.fromEntries(
          exposedFields.map((key) => [key, form[key]])
        );
        resolve({ valid: true, data: result });
      } else {
        resolve({ valid: false });
      }
    });
  });
}

defineExpose({ validate });
</script>
<style scoped>
.form-readonly {
  min-height: 32px;
  line-height: 32px;
}
.rule-half {
  width: 100%;
}
</style>

