<template>
  <!-- 清理过期数据 -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-row>
      <a-col :span="12">
        <a-form-item label="时间范围">
          <a-radio-group
            v-model:value="form.dataRange"
            @change="(e) => handleDataRangeChange(e.target.value)"
            :disabled="falg"
          >
            <a-radio :value="'0'">最近时间范围</a-radio>
            <a-radio :value="'1'">具体日期</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item
          v-if="form.dataRange == '0'"
          label="维持时间范围"
          :rules="
            !falg
              ? [
                  {
                    required: true,
                    message: '请输入维持时间范围',
                    trigger: 'change',
                  },
                ]
              : []
          "
          name="dataRangeValue"
        >
          <template v-if="!falg">
            <a-input-number
              v-model:value="form.dataRangeValue"
              :min="1"
              class="rule-half"
            />
            <a-select v-model:value="form.dataRangeType" class="rule-half">
              <a-select-option label="天前" value="1" />
              <a-select-option label="月前" value="2" />
              <a-select-option label="年前" value="3" />
            </a-select>
          </template>
          <div v-else class="form-readonly">{{ rangeValueText }}</div>
        </a-form-item>

        <a-form-item
          v-if="form.dataRange == '1'"
          label="具体日期"
          :rules="
            !falg
              ? [
                  {
                    required: true,
                    message: '请选择具体日期',
                    trigger: 'change',
                  },
                ]
              : []
          "
          name="dataRangeValue"
        >
          <template v-if="!falg">
            <a-date-picker
              v-model:value="form.dataRangeValue"
              format="YYYY/MM/DD"
              class="rule-half"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
            />
            <a-select v-model:value="form.dataRangeType" class="rule-half">
              <a-select-option label="之前" value="1" />
              <a-select-option label="之后" value="2" />
            </a-select>
          </template>
          <div v-else class="form-readonly">{{ dateValueText }}</div>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row>
      <a-col :span="12">
        <a-form-item label="处理方式">
          <a-radio-group v-model:value="form.handleType" :disabled="falg">
            <a-radio :value="'0'">标记为过期</a-radio>
            <a-radio :value="'1'">删除记录</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item
          label="标记字段"
          v-if="form.handleType == '0'"
          :rules="
            !falg
              ? [
                  {
                    required: true,
                    message: '请选择标记字段',
                    trigger: 'change',
                  },
                ]
              : []
          "
        >
          <template v-if="!falg">
            <a-select
              v-model:value="form.handleColumns"
              placeholder="请选择清洗字段"
              allow-clear
              class="rule-half"
            >
              <a-select-option
                v-for="dict in inputFields"
                :key="dict.columnName"
                :label="dict.label"
                :value="dict.columnName"
              />
            </a-select>
          </template>
          <div v-else class="form-readonly">{{ handleColumnsText }}</div>
        </a-form-item>
      </a-col>
    </a-row>
    <a-row>
      <a-col :span="12">
        <a-form-item
          label="标记值"
          v-if="form.handleType == '0'"
          :rules="
            !falg
              ? [{ required: true, message: '请输入标记值', trigger: 'change' }]
              : []
          "
        >
          <a-input
            v-if="!falg"
            v-model:value="form.handleValue"
            placeholder="请输入标记值"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.handleValue || "-" }}</div>
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref } from "vue";
import moment from "moment";

const props = defineProps({
  form: Object,
  inputFields: Array,
  falg: Boolean,
});

const emit = defineEmits(["update:form"]);
const formRef = ref(null);

const dataRange = props.form?.dataRange ?? "0";

const form = reactive({ ...props.form });
const dataRangeText = computed(() =>
  form.dataRange === "0"
    ? "最近时间范围"
    : form.dataRange === "1"
    ? "具体日期"
    : "-"
);
const rangeValueText = computed(() => {
  const typeText =
    form.dataRangeType === "1"
      ? "天前"
      : form.dataRangeType === "2"
      ? "月前"
      : form.dataRangeType === "3"
      ? "年前"
      : "";
  return form.dataRangeValue ? `${form.dataRangeValue} ${typeText}` : "-";
});
const dateValueText = computed(() => {
  const typeText =
    form.dataRangeType === "1"
      ? "之前"
      : form.dataRangeType === "2"
      ? "之后"
      : "";
  return form.dataRangeValue ? `${form.dataRangeValue} ${typeText}` : "-";
});
const handleTypeText = computed(() =>
  form.handleType === "0"
    ? "标记为过期"
    : form.handleType === "1"
    ? "删除记录"
    : "-"
);
const handleColumnsText = computed(() => {
  const f = (inputFields || []).find(
    (d) => d.columnName === form.handleColumns
  );
  return f ? f.label : form.handleColumns || "-";
});

// 用change事件监听dataRange变化，切换时如果dataRangeValue为空则赋默认值
function handleDataRangeChange(newVal) {
  if (newVal === "0") {
    form.dataRangeValue = 30;
  } else if (newVal === "1") {
    form.dataRangeValue = moment().format("YYYY-MM-DD");
  }
}

// 验证并返回清洗后的有效数据
function validate() {
  return new Promise((resolve) => {
    formRef.value.validate((valid) => {
      if (!valid) return resolve({ valid: false });

      // 只保存当前页面实际有的字段
      const cleanedData = {
        dataRange: form.dataRange,
        dataRangeValue: form.dataRangeValue,
        handleType: form.handleType,
        dataRangeType: form.dataRangeType,
      };

      if (form.dataRange === "0") {
        // 最近时间范围，dataRangeValue 是数字
        cleanedData.dataRangeValue = form.dataRangeValue;
      } else if (form.dataRange === "1") {
        // 具体日期，dataRangeValue 是日期字符串
        cleanedData.dataRangeValue = form.dataRangeValue;
      }

      if (form.handleType === "0") {
        cleanedData.handleColumns = form.handleColumns;
        cleanedData.handleValue = form.handleValue;
      }

      resolve({
        valid: true,
        data: cleanedData,
      });
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

