<template>
  <!-- 字段长度范围校验 -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-row>
      <a-col :span="12">
        <a-form-item label="最小长度" name="minLength">
          <a-input
            v-if="!falg"
            v-model:value="form.minLength"
            placeholder="不填写表示不限制最小长度"
            type="number"
            :min="0"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.minLength ?? "-" }}</div>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="最大长度" name="maxLength">
          <a-input
            v-if="!falg"
            v-model:value="form.maxLength"
            placeholder="不填写表示不限制最大长度"
            type="number"
            :min="0"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.maxLength ?? "-" }}</div>
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
const props = defineProps({
  form: Object,
  dppQualityTaskObjSaveReqVO: Array,
  falg: Boolean,
});

const emit = defineEmits(["update:form"]);
const formRef = ref(null);
const form = reactive({ ...props.form });
function validate() {
  return new Promise((resolve) => {
    formRef.value.validate().then(() => {
      const data = Object.fromEntries(
        ["minLength", "maxLength", "ignoreNullValue"].map((key) => [
          key,
          form[key],
        ])
      );
      resolve({
        valid: true,
        data,
      });
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

