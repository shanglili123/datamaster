<template>
  <a-modal
    title="详请"
    v-model:open="visible"
    width="800px"
    :draggable="true"
    :footer="null"
  >
    <a-table
      striped
      :loading="loading"
      :data-source="dpDataElemCodeList"
      :row-key="(record) => record.id"
      :pagination="false"
      :scroll="{ y: '65vh' }"
    >
      <a-table-column title="编号" align="left" data-index="id" :width="80" />
      <a-table-column
        title="代码值"
        align="left"
        data-index="codeValue"
        :width="160"
      >
        <template #default="{ record }">
          {{ record.codeValue || "-" }}
        </template>
      </a-table-column>
      <a-table-column
        title="代码名称"
        align="left"
        data-index="codeName"
        :width="350"
      >
        <template #default="{ record }">
          {{ record.codeName || "-" }}
        </template>
      </a-table-column>
      <a-table-column
        title="创建人"
        align="left"
        data-index="createBy"
        :width="160"
      >
        <template #default="{ record }">
          {{ record.createBy || "-" }}
        </template>
      </a-table-column>
      <a-table-column
        title="创建时间"
        align="left"
        data-index="createTime"
        :width="220"
      >
        <template #default="{ record }">
          <span>{{
            parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}")
          }}</span>
        </template>
      </a-table-column>
      <a-table-column
        title="备注"
        align="left"
        data-index="remark"
        :width="360"
        ellipsis
      >
        <template #default="{ record }">
          {{ record.remark || "-" }}
        </template>
      </a-table-column>
      <template #emptyText>
        <div class="emptyBg">
          <img
            src="../../../../../../../../assets/system/images/no_data/noData.png"
            alt=""
          />
          <p>无数据</p>
        </div>
      </template>
    </a-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
    <template #footer>
      <div class="dialog-footer">
        <a-button size="small" @click="handleClose">关 闭</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup name="ComponentOne">
import {
  listDpDataElemCode,
  validateCodeValue,
} from "@/api/std/dataElem/dataElem.js";
const route = useRoute();
const { proxy } = getCurrentInstance();

const dpDataElemCodeList = ref([]);

const open = ref(false);
const openDetail = ref(false);
const loading = ref(true);
const total = ref(0);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    dataElemId: null,
    codeValue: null,
    codeName: null,
    createTime: null,
  },
});

let id = route.query.id;

const { queryParams, form } = toRefs(data);

/** 查询数据元代码列表 */
function getList(id) {
  if (id == -1) {
    return;
  }
  loading.value = true;
  queryParams.value.dataElemId = id;
  listDpDataElemCode(queryParams.value).then((response) => {
    dpDataElemCodeList.value = response.data.rows;
    total.value = response.data.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  openDetail.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    dataElemId: null,
    codeValue: null,
    codeName: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
  };
  proxy.resetForm("dpDataElemCodeRef");
}

const visible = ref(false);

function openDialog(row) {
  visible.value = true;
  getList(row.id);
}
function handleClose() {
  visible.value = false;
  reset();
}
defineExpose({ openDialog });
</script>

