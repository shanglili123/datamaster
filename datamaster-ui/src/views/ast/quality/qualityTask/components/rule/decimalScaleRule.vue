<template>
  <!-- 数值精度校验 -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-row>
      <a-col :span="12">
        <a-form-item label="小数位数" name="scale">
          <a-input
            v-if="!falg"
            v-model:value="form.scale"
            placeholder="请输入小数位数"
            type="number"
            :min="0"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.scale ?? "-" }}</div>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="忽略整数值" name="skipInteger">
          <a-radio-group v-if="!falg" v-model:value="form.skipInteger">
            <a-radio :value="'1'">是</a-radio>
            <a-radio :value="'0'">否</a-radio>
          </a-radio-group>
          <div v-else class="form-readonly">
            {{
              form.skipInteger === "1"
                ? "是"
                : form.skipInteger === "0"
                ? "否"
                : "-"
            }}
          </div>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row>
      <a-col :span="12">
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
    </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { getColumnByAssetId } from "@/api/col/task/index.js";

const props = defineProps({
  form: Object,
  dppQualityTaskObjSaveReqVO: Array,
  falg: Boolean,
});

const emit = defineEmits(["update:form"]);

const formRef = ref(null);

const form = reactive({ ...props.form });

const exposedFields = ["scale", "skipInteger", "ignoreNullValue"];

function validate() {
  return new Promise((resolve) => {
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

