<template>
  <!-- 数值字段范围校验 -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="false">
    <a-row>
      <a-col :span="12">
        <a-form-item label="最小值" name="minValue">
          <a-input
            v-if="!falg"
            v-model:value="form.minValue"
            placeholder="不填写表示不限制最小值"
            type="number"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.minValue ?? "-" }}</div>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="最大值" name="maxValue">
          <a-input
            v-if="!falg"
            v-model:value="form.maxValue"
            placeholder="不填写表示不限制最大值"
            type="number"
            class="rule-half"
          />
          <div v-else class="form-readonly">{{ form.maxValue ?? "-" }}</div>
        </a-form-item>
      </a-col>
    </a-row>

    <a-row>
      <a-col :span="12">
        <a-form-item label="是否包含边界值" name="includeBoundary">
          <a-radio-group v-if="!falg" v-model:value="form.includeBoundary">
            <a-radio :value="'1'">包含</a-radio>
            <a-radio :value="'0'">不包含</a-radio>
          </a-radio-group>
          <div v-else class="form-readonly">{{ includeBoundaryText }}</div>
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { getColumnByAssetId } from "@/api/col/task/index.js";
import useUserStore from "@/store/system/user";

const props = defineProps({
  form: Object,
  dppQualityTaskObjSaveReqVO: Array,
  falg: Boolean,
});

const emit = defineEmits(["update:form"]);

const formRef = ref(null);
const userStore = useUserStore();

const form = reactive({ ...props.form });
const includeBoundaryText = computed(() =>
  form.includeBoundary === "1"
    ? "包含"
    : form.includeBoundary === "0"
    ? "不包含"
    : "-"
);
const columnList = ref([]);
// 评测对象改变时，更新相关字段和字段列表
async function handleTargetObjectChange(selectedName, falg) {
  const selected = props.auditTargets.find(
    (item) => item.datasourceName == selectedName
  );
  if (selected) {
    form.datasourceId = selected.datasourceId;
    form.assetid = selected.assetid || "";
    form.checkField = "";
    await fetchColumns();
  } else {
    form.datasourceId = null;
    form.assetid = "";
    form.checkField = "";
    columnList.value = [];
  }
}

const loading = ref(false);

async function fetchColumns() {
  if (!form.datasourceId || !form.assetid) {
    columnList.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await getColumnByAssetId({
      id: form.datasourceId,
      tableName: form.assetid,
      spaceId: userStore.spaceId,
      spaceCode: userStore.spaceCode,
    });
    if (res.code == "200") {
      columnList.value = res.data;
    } else {
      columnList.value = [];
    }
  } catch (error) {
    columnList.value = [];
  } finally {
    loading.value = false;
  }
}

const exposedFields = [
  "targetObject",
  "datasourceId",
  "assetid",
  "checkField",
  "minValue",
  "maxValue",
  "includeBoundary",
];

function validate() {
  return new Promise((resolve) => {
    formRef.value.validate().then(() => {
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

onMounted(() => {
  if (form.targetObject) {
    fetchColumns();
  }
  console.log("子组件 mounted hook");
});

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

