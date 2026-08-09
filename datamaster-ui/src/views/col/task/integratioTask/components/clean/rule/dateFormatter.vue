<!-- 日期格式统一 -->
<template>
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-row>
      <a-col :span="12">
        <a-form-item
          label="日期格式"
          name="selectedOption"
          :rules="
            !falg
              ? [
                  {
                    required: true,
                    message: '请选择日期格式',
                    trigger: 'change',
                  },
                ]
              : []
          "
        >
          <template v-if="!falg">
            <a-select
              v-model:value="form.selectedOption"
              placeholder="请选择日期格式"
              class="rule-half"
            >
              <a-select-option label="yyyy" value="yyyy" />
              <a-select-option label="yyyy-MM" value="yyyy-MM" />
              <a-select-option label="yyyy-MM-dd" value="yyyy-MM-dd" />
              <a-select-option label="yy-MM-dd" value="yy-MM-dd" />
              <a-select-option
                label="yyyy-MM-dd HH:mm:ss"
                value="yyyy-MM-dd HH:mm:ss"
              />
            </a-select>
          </template>
          <div v-else class="form-readonly">{{ selectedOptionText }}</div>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row>
      <a-col :span="12" v-if="form.selectedOption == '1'">
        <a-form-item
          label="日期格式"
          name="targetFormat"
          :rules="
            !falg
              ? [
                  {
                    required: true,
                    message: '请输入日期格式，例如：YY-MM-DD',
                    trigger: 'blur',
                  },
                ]
              : []
          "
        >
          <a-input
            v-if="!falg"
            v-model:value="form.targetFormat"
            placeholder="请输入日期格式，例如：YY-MM-DD"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.targetFormat || "-" }}</div>
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

// 初始化 selectedOption
// const getInitialSelectedOption = (targetFormat) => {
//   if (!targetFormat) return "yyyy-MM-dd";
//   // 判断是否为预设值
//   if (targetFormat === "yyyy-MM-dd" || targetFormat === "YYYY-MM-DD HH:mm:ss") {
//     return targetFormat;
//   }
//   // 否则为自定义值，选择配置模板
//   return "1";
// };

const form = reactive({
  ...props.form,
});
const selectedOptionText = computed(() => {
  if (form.selectedOption === "1") return "自定义";
  return form.selectedOption || "-";
});

watch(
  () => form.selectedOption,
  (newVal, oldVal) => {
    if (newVal === "1" && oldVal !== "1") {
      // 从预设值切换到配置模板时，清空 targetFormat，让用户输入自定义格式
      form.targetFormat = "";
    } else if (newVal !== "1") {
      // 选择预设值时，targetFormat 设置为选中的值
      form.targetFormat = newVal;
    }
  }
);
//
function validate() {
  console.log("🚀 ~ validate ~ props.form?:", props.form?.inputFormats);
  return new Promise((resolve) => {
    formRef.value.validate((valid) => {
      if (valid) {
        resolve({
          valid: true,
          data: {
            targetFormat: form.targetFormat,
            selectedOption: form.selectedOption,
            inputFormats: props.form?.inputFormats,
          },
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

