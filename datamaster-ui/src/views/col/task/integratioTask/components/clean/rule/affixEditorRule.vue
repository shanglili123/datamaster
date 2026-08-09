<template>
  <!--  字段前缀/后缀统一  -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-row>
      <a-col :span="12">
        <a-form-item
          label="标记值"
          name="stringValue"
          :rules="
            !falg
              ? [{ required: true, message: '请输入标记值', trigger: 'blur' }]
              : []
          "
        >
          <a-input
            v-if="!falg"
            v-model:value="form.stringValue"
            placeholder="请输入添加值"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.stringValue || "-" }}</div>
        </a-form-item>
      </a-col>
      <a-col :span="12" class="hasMsg">
        <a-form-item
          label="处理方式"
          name="handleType"
          :rules="
            !falg
              ? [{ required: true, message: '请选择处理方式', trigger: 'blur' }]
              : []
          "
        >
          <a-radio-group v-model:value="form.handleType" :disabled="falg">
            <a-radio :value="'1'">加前綴</a-radio>
            <a-radio :value="'2'">加后綴</a-radio>
            <a-radio :value="'3'">去除前缀</a-radio>
            <a-radio :value="'4'">去除后缀</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row> </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
const props = defineProps({
  form: Object,
  inputFields: Array,
  falg: Boolean,
});

const emit = defineEmits(["update:form"]);

const formRef = ref(null);

const form = reactive({ ...props.form });
const boundaryExamples = computed(() => {
  switch (form.handleType) {
    case "3":
      return ["示例: 如果年龄 > 150，则设置为 150。"];
    case "2":
      return ["示例: 如果收入 < 1000，则设置为 1000。"];
    case "1":
      return [
        "示例1: 如果年龄 > 150，则设置为 150。如果收入 < 1000，则设置为 1000。",
      ];
    default:
      return [];
  }
});
const handleTypeText = computed(() => {
  if (form.handleType === "1") return "加前綴";
  if (form.handleType === "2") return "加后綴";
  if (form.handleType === "3") return "去除前缀";
  if (form.handleType === "4") return "去除后缀";
  return "-";
});
const loading = ref(false);
const exposedFields = ["stringValue", "handleType"];
function validate() {
  return new Promise((resolve) => {
    formRef.value.validate((valid) => {
      if (valid) {
        const data = Object.fromEntries(
          exposedFields.map((key) => [key, form[key]])
        );
        resolve({
          valid: true,
          data,
        });
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

