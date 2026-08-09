<template>
  <!-- 小数位统一 -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-row>
      <a-col :span="12" class="hasMsg">
        <a-form-item
          label="小数位数"
          name="stringValue"
          :rules="
            !falg
              ? [
                  {
                    required: true,
                    message: '请输入小数位数',
                    trigger: 'change',
                  },
                ]
              : []
          "
        >
          <a-input
            v-if="!falg"
            v-model:value="form.stringValue"
            placeholder="请输入小数位数"
            type="number"
            min="0"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.stringValue ?? "-" }}</div>
          <span class="msg"><InfoCircleOutlined />如“2”表示保留两位小数</span>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row> </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { InfoCircleOutlined } from "@ant-design/icons-vue";
import { getColumnByAssetId } from "@/api/col/task/index.js";

const props = defineProps({
  form: Object,
  inputFields: Array,
  falg: Boolean,
});

const emit = defineEmits(["update:form"]);

const formRef = ref(null);

const form = reactive({ ...props.form });

const exposedFields = ["stringValue"];

function validate() {
  return new Promise((resolve) => {
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

