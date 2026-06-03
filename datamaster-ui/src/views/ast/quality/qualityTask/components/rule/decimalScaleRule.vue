<template>
  <!-- 数值精度校验 -->
  <el-form ref="formRef" :model="form" label-width="130px" :disabled="false">
    <el-row>
      <el-col :span="12">
        <el-form-item label="小数位数" prop="scale">
          <el-input
            v-if="!falg"
            v-model="form.scale"
            placeholder="请输入小数位数"
            type="number"
            min="0"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.scale ?? "-" }}</div>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="忽略整数值" prop="skipInteger">
          <el-radio-group v-if="!falg" v-model="form.skipInteger">
            <el-radio :value="'1'">是</el-radio>
            <el-radio :value="'0'">否</el-radio>
          </el-radio-group>
          <div v-else class="form-readonly">
            {{
              form.skipInteger === "1"
                ? "是"
                : form.skipInteger === "0"
                ? "否"
                : "-"
            }}
          </div>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="12">
        <el-form-item label="忽略空值" prop="ignoreNullValue">
          <el-radio-group v-if="!falg" v-model="form.ignoreNullValue">
            <el-radio :value="'1'">是</el-radio>
            <el-radio :value="'0'">否</el-radio>
          </el-radio-group>
          <div v-else class="form-readonly">
            {{
              form.ignoreNullValue === "1"
                ? "是"
                : form.ignoreNullValue === "0"
                ? "否"
                : "-"
            }}
          </div>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
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
.rule-half {
  width: 100%;
}
.form-readonly {
  min-height: 32px;
  line-height: 32px;
}
</style>

