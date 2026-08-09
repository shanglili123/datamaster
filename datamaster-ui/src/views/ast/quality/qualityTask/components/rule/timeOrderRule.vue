<template>
  <!-- 时间字段先后顺序校验 -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="falg">
    <a-form-item label="">
      <div
        class="field-line"
        style="display: flex; align-items: center; flex-wrap: wrap; gap: 8px"
      >
        <a-select
          v-if="form.conditions.length > 0"
          v-model:value="form.conditions[0].leftField"
          placeholder="字段"
          style="width: 120px"
          @change="onLeftFieldChange($event, 0)"
        >
          <a-select-option
            v-for="col in timeColumns"
            :key="col.columnName"
            :value="col.columnName"
            >{{ col.columnName }}</a-select-option
          >
        </a-select>
        <div v-else style="width: 120px"></div>
        <template v-for="(cond, index) in form.conditions" :key="index">
          <a-select
            v-model:value="cond.operator"
            placeholder="符号"
            style="width: 50px"
          >
            <a-select-option value="<">&lt;</a-select-option>
            <a-select-option value="<=">≤</a-select-option>
          </a-select>

          <a-select
            v-model:value="cond.rightField"
            placeholder="字段"
            style="width: 120px"
          >
            <a-select-option
              v-for="col in timeColumns"
              :key="col.columnName"
              :value="col.columnName"
              >{{ col.columnName }}</a-select-option
            >
          </a-select>
          <a-button
            v-if="!falg && index === form.conditions.length - 1"
            :icon="h(DeleteOutlined)"
            type="danger"
            shape="circle"
            @click="removeLastGroup"
            :disabled="form.conditions.length === 0"
          />
        </template>

        <!-- 添加按钮 -->
        <a-button
          v-if="!falg"
          :icon="h(PlusOutlined)"
          type="primary"
          shape="circle"
          @click="addGroup"
        />
      </div>
    </a-form-item>
  </a-form>
</template>

<script setup>
import { message } from 'ant-design-vue'
import { reactive, ref, computed, watch, onMounted, h } from "vue";

import { DeleteOutlined, PlusOutlined } from "@ant-design/icons-vue";

const props = defineProps({
  form: Object,
  falg: Boolean,
  columnList: Array,
});
const emit = defineEmits(["update:form"]);

const formRef = ref(null);

const form = reactive({
  conditions: props.form.conditions?.length
    ? JSON.parse(JSON.stringify(props.form.conditions))
    : [{ leftField: "", operator: "<", rightField: "" }],
});

const columnList = ref([]);

const timeColumns = computed(() =>
  columnList.value.filter((col) => {
    console.log("🚀 ~  columnList.value:", columnList.value);
    if (!col.columnType) return false;
    const type = col.columnType.toUpperCase();
    return (
      type == "DATE" ||
      type.startsWith("TIMESTAMP") ||
      type == "TIME" ||
      type == "YEAR" ||
      type == "DATETIME"
    );
  })
);

// 拼接选择的时间字段，参考evaColumn的实现
const timeOrderFields = computed(() => {
  if (!form.conditions || form.conditions.length === 0) return "";
  // 获取所有唯一的字段名
  const fieldNames = new Set();
  form.conditions.forEach((cond) => {
    if (cond.leftField) fieldNames.add(cond.leftField);
    if (cond.rightField) fieldNames.add(cond.rightField);
  });
  // 转换为数组并排序
  const fieldsArray = Array.from(fieldNames);
  // 使用columnList中的label显示，如果没有则显示columnName
  const map = new Map(
    (columnList.value || []).map((c) => [c.columnName, c.label || c.columnName])
  );
  return fieldsArray.map((field) => map.get(field) || field).join(", ");
});

watch(
  () => props.columnList,
  (newVal) => {
    columnList.value = newVal || [];
  },
  { immediate: true }
);

function addGroup() {
  form.conditions.push({ leftField: "", operator: "<", rightField: "" });
}

function removeLastGroup() {
  if (form.conditions.length > 1) {
    form.conditions.pop();
  }
}

function onLeftFieldChange(value, index) {
  form.conditions[0].leftField = value;
  for (let i = 1; i < form.conditions.length; i++) {
    form.conditions[i].leftField = form.conditions[i - 1].rightField;
  }
}

watch(
  () => form.conditions.map((c) => c.rightField),
  () => {
    for (let i = 1; i < form.conditions.length; i++) {
      form.conditions[i].leftField = form.conditions[i - 1].rightField;
    }
  }
);

function validateCalculationGroups() {
  if (form.conditions.length === 0) {
    message.warning("校验未通过，请至少添加一组计算条件");
    return false;
  }
  for (let i = 0; i < form.conditions.length; i++) {
    const group = form.conditions[i];
    if (!group.leftField) {
      message.warning(`校验未通过，请填写第 ${i + 1} 个计算组的左字段`);
      return false;
    }
    if (!group.operator || !["<", "<="].includes(group.operator)) {
      message.warning(`校验未通过，第 ${i + 1} 个计算组的符号无效`);
      return false;
    }
    if (!group.rightField) {
      message.warning(`校验未通过，请填写第 ${i + 1} 个计算组的右字段`);
      return false;
    }
  }
  return true;
}

function validate() {
  return new Promise((resolve) => {
    formRef.value.validate().then(() => {
      if (!validateCalculationGroups()) {
        resolve({ valid: false });
        return;
      }

      // 获取所有唯一的字段名
      const fieldNames = new Set();
      form.conditions.forEach((cond) => {
        if (cond.leftField) fieldNames.add(cond.leftField);
        if (cond.rightField) fieldNames.add(cond.rightField);
      });

      // 转换为数组
      const fieldsArray = Array.from(fieldNames);

      resolve({
        valid: true,
        data: {
          conditions: JSON.parse(JSON.stringify(form.conditions)),
          evaColumn: fieldsArray, // 直接返回字段数组，用于赋值给父组件的evaColumn
        },
      });
    }).catch(() => {
      message.warning("校验未通过，请完善表单必填项");
      resolve({ valid: false });
    });
  });
}

defineExpose({ validate });
</script>

<style scoped>
.field-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
</style>

