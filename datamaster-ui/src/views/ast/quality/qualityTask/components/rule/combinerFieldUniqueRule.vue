<template>
  <!-- 字段组完整性校验 -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-row>
      <a-col :span="24">
        <a-form-item label="字段完整性" name="fillStrategy">
          <a-radio-group
            v-if="!falg"
            v-model:value="form.fillStrategy"
            class="rule-half"
          >
            <a-radio :value="'1'">必须全部填写（部分为空为异常）</a-radio>
            <a-radio :value="'2'"
              >要么全部为空，要么全部填写（部分填写为异常）</a-radio
            >
          </a-radio-group>
          <div v-else class="form-readonly">{{ fillStrategyText }}</div>
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref, computed } from "vue";

const props = defineProps({
  form: Object,
  dppQualityTaskObjSaveReqVO: Array,
  falg: Boolean,
});

const emit = defineEmits(["update:form"]);

const formRef = ref(null);

const form = reactive({ ...props.form });
const fillStrategyText = computed(() =>
  form.fillStrategy === "1"
    ? "必须全部填写（部分为空为异常）"
    : form.fillStrategy === "2"
    ? "要么全部为空，要么全部填写（部分填写为异常）"
    : "-"
);
function validate() {
  return new Promise((resolve) => {
    formRef.value.validate().then(() => {
      const exposedFields = ["fillStrategy"];
      const data = Object.fromEntries(
        exposedFields.map((key) => [key, form[key]])
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

