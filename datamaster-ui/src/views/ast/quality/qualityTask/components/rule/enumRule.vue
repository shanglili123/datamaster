<template>
  <!-- 枚举值校验 -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="falg">
    <a-row>
      <a-col :span="8">
        <a-form-item label="关联代码表" name="useCodeTable">
          <a-radio-group
            v-model:value="form.useCodeTable"
            @change="handleUseCodeTableChange"
          >
            <a-radio :value="'1'">是</a-radio>
            <a-radio :value="'0'">否</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>

      <a-col :span="8">
        <a-form-item label="忽略空值" name="ignoreNullValue">
          <a-radio-group v-model:value="form.ignoreNullValue">
            <a-radio :value="'1'">是</a-radio>
            <a-radio :value="'0'">否</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>

      <a-col :span="8">
        <a-form-item label="是否区分大小写" name="ignoreCase">
          <a-radio-group v-model:value="form.ignoreCase">
            <a-radio :value="'1'">是</a-radio>
            <a-radio :value="'0'">否</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
    </a-row>

    <!-- 👇 当选择了“关联代码表”为“是”时展示 -->
    <a-row v-if="form.useCodeTable == '1'">
      <a-col :span="8">
        <a-form-item
          label="选择代码表"
          name="codeTableId"
          :rules="
            !falg
              ? [{ required: true, message: '选择代码表', trigger: 'change' }]
              : []
          "
        >
          <template v-if="!falg">
            <a-select
              v-model:value="form.codeTableId"
              placeholder="请选择代码表"
              show-search
              allow-clear
              @change="handleCodeTableChange"
              class="rule-half"
            >
              <a-select-option
                v-for="item in dpDataElemList"
                :key="item.id"
                :value="item.id"
                >{{ item.name }}</a-select-option
              >
            </a-select>
          </template>
          <div v-else class="form-readonly">
            {{
              dpDataElemList.find((i) => i.id === form.codeTableId)?.name || "-"
            }}
          </div>
        </a-form-item>
      </a-col>
    </a-row>

    <a-spin :spinning="loadingList">
      <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
          <template v-if="!falg && form.useCodeTable == 0">
            <a-col :span="1.5">
              <a-button
                type="primary"
                :icon="h(PlusOutlined)"
                @click="opencodeDialog(undefined)"
                >新增</a-button
              >
            </a-col>
          </template>
        </a-row>
      </div>
      <a-table
        striped
        :scroll="{ y: 200 }"
        :data-source="pagedCodeList"
        :columns="tableColumns"
        :pagination="false"
        row-key="codeValue"
        size="small"
        :locale="{ emptyText: emptyContent }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'codeValue'">
            <template v-if="!falg && form.useCodeTable == 0">
              <a-input
                v-model:value="record.codeValue"
                style="width: 100%"
                placeholder="请输入代码值"
              />
            </template>
            <template v-else>
              {{ record.codeValue || "-" }}
            </template>
          </template>
          <template v-else-if="column.dataIndex === 'codeName'">
            <template v-if="!falg && form.useCodeTable == 0">
              <a-input
                v-model:value="record.codeName"
                style="width: 100%"
                placeholder="请输入代码名称"
              />
            </template>
            <template v-else>
              {{ record.codeName || "-" }}
            </template>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button
              type="link"
              danger
              size="small"
              @click="handleDelete(record)"
              >删除</a-button
            >
          </template>
        </template>
      </a-table>
      <pagination
        v-show="form.codeList.length > 0"
        :total="form.codeList.length"
        v-model:page="codeQueryParams.pageNum"
        v-model:limit="codeQueryParams.pageSize"
      />
    </a-spin>
    <a-row> </a-row>
  </a-form>
</template>

<script setup>
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref, watch, h } from "vue";

import { PlusOutlined } from "@ant-design/icons-vue";

const tableColumns = computed(() => {
  const cols = [
    { title: '代码值', dataIndex: 'codeValue', align: 'center' },
    { title: '代码名称', dataIndex: 'codeName', align: 'center' },
  ];
  if (!props.falg && form.useCodeTable == 0) {
    cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 150 });
  }
  return cols;
});

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

import { listDpDataElem } from "@/api/std/dataElem/dataElem";

import { listDpDataElemCode } from "@/api/std/dataElem/dataElem";
const props = defineProps({
  form: Object,
  dppQualityTaskObjSaveReqVO: Array,
  falg: Boolean,
});
let loadingList = ref(false);
const emit = defineEmits(["update:form"]);
let loading = ref(false);
const formRef = ref(null);

const form = reactive({ ...props.form });
let dpDataElemCodeList = ref([]);
let dpDataElemList = ref([]);
const codeQueryParams = ref({
  pageNum: 1,
  pageSize: 6,
});
const pagedCodeList = computed(() => {
  const list = form.codeList || [];
  const start =
    (codeQueryParams.value.pageNum - 1) * codeQueryParams.value.pageSize;
  return list.slice(start, start + codeQueryParams.value.pageSize);
});
function handleCodeTableChange(id) {
  if (!id || id == -1) return;
  loading.value = true;
  form.dataElemId = id;
  loadCodeItemsByTableId(id);
}
function loadCodeItemsByTableId(id) {
  if (!id || id == -1) return;
  loading.value = true;
  listDpDataElemCode({
    pageNum: 1,
    pageSize: 999,
    dataElemId: id,
    ruleType: 2,
  }).then((res) => {
    dpDataElemCodeList.value = res.data.rows;
    form.codeList = res.data.rows;
    codeQueryParams.value.pageNum = 1;
    loading.value = false;
  });
}
function handleDelete(row) {
  const index = form.codeList.indexOf(row);
  if (index !== -1) {
    form.codeList.splice(index, 1);
  }
  const maxPage = Math.ceil(form.codeList.length / codeQueryParams.value.pageSize) || 1;
  if (codeQueryParams.value.pageNum > maxPage) {
    codeQueryParams.value.pageNum = maxPage;
  }
}
function opencodeDialog() {
  // 新增一行空数据
  form.codeList.push({
    codeValue: "",
    codeName: "",
  });
  codeQueryParams.value.pageNum =
    Math.ceil(form.codeList.length / codeQueryParams.value.pageSize) || 1;
}
function loadCodeTableList() {
  listDpDataElem({
    pageNum: 1,
    pageSize: 999,
    type: "2",
  })
    .then((res) => {
      dpDataElemList.value = res.data.rows;
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
}
function handleUseCodeTableChange(val) {
  if (val == "1") {
    loadCodeTableList();
  } else {
    form.codeTableId = "";
    form.codeList = [];
    codeQueryParams.value.pageNum = 1;
    dpDataElemList.value = [];
  }
}

onMounted(() => {
  if (form.useCodeTable === "1" && form.codeTableId) {
    handleUseCodeTableChange("1", true);
  }
});
function validate() {
  return new Promise((resolve) => {
    formRef.value.validate().then(() => {
      if (form.useCodeTable === "0") {
        if (!form.codeList || form.codeList.length === 0) {
          message.warning("校验未通过，请至少添加一条代码项");
          resolve({ valid: false });
          return;
        }

        const values = form.codeList
          .map((item) => item.codeValue?.trim())
          .filter((v) => v !== "");
        const hasEmpty = values.length !== form.codeList.length;
        if (hasEmpty) {
          message.warning("校验未通过，代码值不能为空");
          resolve({ valid: false });
          return;
        }

        const duplicates = values.filter(
          (val, idx) => values.indexOf(val) !== idx
        );
        if (duplicates.length > 0) {
          message.warning("校验未通过，代码值不能重复");
          resolve({ valid: false });
          return;
        }
      }

      const result = {
        useCodeTable: form.useCodeTable,
        ignoreNullValue: form.ignoreNullValue,
        ignoreCase: form.ignoreCase,
        codeTableId: form.useCodeTable === "1" ? form.codeTableId : "",
        codeList: form.codeList,
        validValues: form.codeList
          .map((item) => item.codeValue)
          .filter((v) => v !== undefined && v !== null && v !== ""),
      };

      resolve({ valid: true, data: result });
    }).catch(() => {});
  });
}

defineExpose({ validate });
</script>
<style scoped></style>

