<template>
  <!--  超长字段截断  -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-row>
      <a-col :span="12">
        <a-form-item
          label="字符数量"
          name="maxLength"
          :rules="
            !falg
              ? [{ required: true, message: '请输入字符数量', trigger: 'blur' }]
              : []
          "
        >
          <a-input-number v-if="!falg" v-model:value="form.maxLength" :min="0" />
          <div v-else class="form-readonly">{{ form.maxLength ?? "-" }}</div>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row>
      <a-col :span="12" class="hasMsg">
        <a-form-item
          label="处理方式"
          name="direction"
          :rules="
            !falg
              ? [{ required: true, message: '请选择处理方式', trigger: 'blur' }]
              : []
          "
        >
          <a-radio-group v-model:value="form.direction" :disabled="falg">
            <a-radio :value="'1'">正向</a-radio>
            <a-radio :value="'2'">反向</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
    </a-row>
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
console.log("🚀 ~ form:", form);
const exposedFields = ["maxLength", "direction"];
const directionText = computed(() =>
  form.direction === "1" ? "正向" : form.direction === "2" ? "反向" : "-"
);
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

