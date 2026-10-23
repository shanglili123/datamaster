<template>
  <div class="basicInfo">
    <a-descriptions title="" :column="2" bordered>
      <a-descriptions-item v-for="(item, index) in fileDesc" :key="index" :label-style="{ width: '200px' }"
        :span="item.span">
        <template #label>
          <div class="cell-item">{{ item.label }}</div>
        </template>
        <div v-if="item.key == 'createTime'">
          {{
            parseTime(
              daDiscoveryTaskDetail.createTime,
              "{y}-{m}-{d} {h}:{i}"
            ) || "-"
          }}
        </div>
        <div v-else-if="item.key == 'createType'">
          <dict-tag :options="dp_model_create_type" :value="daDiscoveryTaskDetail.createType" />
        </div>
        <div v-else-if="item.key == 'updateTime'">
          {{
            parseTime(
              daDiscoveryTaskDetail.updateTime,
              "{y}-{m}-{d} {h}:{i}"
            ) || "-"
          }}
        </div>
        <div v-else>{{ getDescValue(item) }}</div>
      </a-descriptions-item>
    </a-descriptions>
  </div>
</template>
<script setup name="BasicInfo">
import moment from "moment";
import { cronToZh } from "@/utils/cronUtils";
const { proxy } = getCurrentInstance();
const { dp_model_status, dp_model_create_type } = proxy.useDict(
  "dp_model_status",
  "dp_model_create_type"
);
const props = defineProps({
  daDiscoveryTaskDetail: {
    type: Object,
    default: () => { },
  },
});
const fileDesc = computed(() => {
  return table.value;
});
const table = ref([
  {
    key: "catName",
    label: "逻辑模型目录",
    value: "",
  },
  { key: "physicalTableName", label: "物理目标表名", value: "" },
  { key: "tableType", label: "表类型", value: "" },
  { key: "dataLayerName", label: "数仓分层", value: "" },
  { key: "businessCategoryName", label: "业务分类（资产标签）", value: "" },
  { key: "dataDomainName", label: "数据域", value: "" },
  { key: "themeDomainName", label: "主题域", value: "" },
  { key: "namingVersion", label: "命名状态", value: "" },
  // {
  //   key: "createType",
  //   label: "创建方式",
  //   value: "",
  // },
  {
    key: "createBy",
    label: "创建人",
    value: "",
  },
  {
    key: "createTime",
    label: "创建时间",
    value: "",
  },
  {
    key: "updateBy",
    label: "更新人",
    value: "",
  },
  {
    key: "updateTime",
    label: "更新时间",
    value: "",
    type: "time",
  },
  // {
  //   key: "remark",
  //   label: "备注",
  //   value: "",
  //   span: 24,
  // },
]);
const getDescValue = (row) => {
  let detail = { ...props.daDiscoveryTaskDetail };
  if (props.daDiscoveryTaskDetail) {
    if (row.type == "time") {
      row.value = moment(detail[row.key]).format("YYYY-MM-DD");
    } else if (row.key == "countSubmitted") {
      row.countSubmitted = detail[row.countSubmitted];
      row.countPending = detail[row.countPending];
      row.countIgnoreFlag = detail[row.countIgnoreFlag];
    } else if (row.key === "tableType") {
      row.value = { '1': '明细表', '2': '汇总表', '3': '维度表', '4': '应用表' }[detail.tableType];
    } else if (row.key === "namingVersion") {
      row.value = detail.namingVersion === 'GOVERNED_V1' ? '规范命名' : '历史兼容（未升级）';
    } else {
      row.value = detail[row.key];
    }
  }
  return row.value !== null && row.value !== undefined && row.value !== "" ? row.value : "-";
};
</script>
<style lang="scss" scoped>
:deep(.base-label) {
  width: 200px;

  .cell-item {
    font-weight: 500;
  }
}
</style>

