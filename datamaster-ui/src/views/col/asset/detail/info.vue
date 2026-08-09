<template>
  <!-- 资产详情 tab -->
  <div class="basicInfo">
    <a-descriptions title="" :column="2" bordered>
      <a-descriptions-item v-for="(item, index) in fileDesc" :key="index" :label-style="{ width: '200px' }"
        :span="item.span">
        <template #label>
          <div class="cell-item">{{ item.label }}</div>
        </template>
        <span v-if="item.key == 'tags'">
          <template v-if="item.value.length != 0">
            <a-tag v-for="tag in item.value" :key="tag" class="mr10">
              {{ tag }}
            </a-tag>
          </template>
          <template v-else>-</template>
        </span>
        <span v-else-if="item.key == 'createType'">{{ item.value == 1 ? "虚拟资产创建" : "完整资产创建" }}</span>
        <div v-else-if="item.key == 'createTime'">
            {{
            parseTime(
                form1.createTime,
                "{y}-{m}-{d} {h}:{i}"
            ) || "-"
            }}
        </div>
        <div v-else-if="item.key == 'updateTime'">
            {{
            parseTime(
                form1.updateTime,
                "{y}-{m}-{d} {h}:{i}"
            ) || "-"
            }}
        </div>
        <span v-else>{{ getDescValue(item) }}</span>
      </a-descriptions-item>
    </a-descriptions>
  </div>
</template>
<script setup name="BasicInfo">
const props = defineProps({
  form1: {
    type: Object,
    default: () => { },
  },
});
const fileDesc = computed(() => {
  if (props.form1.type == 1) {
    return table.value;
  } else if (props.form1.type == 7) {
    return file.value;
  }
});
const table = ref([
  {
    key: "datasourceName",
    label: "数据连接名称",
    value: "",
  },
  {
    key: "datasourceType",
    label: "数据连接类型",
    value: "",
  },
  {
    key: "dbname",
    label: "数据连接实例",
    value: "",
  },
  {
    key: "datasourceIp",
    label: "数据库连接IP",
    value: "",
  },
  {
      key: "dataCount",
      label: "行数",
      value: "",
  },
  {
      key: "fieldCount",
      label: "列数",
      value: "",
  },
  {
      key: "createBy",
      label: "创建人",
      value: "-",
  },
  {
      key: "createTime",
      label: "创建时间",
      value: "-",
  },
  {
      key: "updateBy",
      label: "更新人",
      value: "-",
  },
  {
      key: "updateTime",
      label: "更新时间",
      value: "-",
  },
  {
      key: "remark",
      label: "备注",
      value: "",
      span: 24,
  },
]);
const file = ref([
  {
    key: "datasourceName",
    label: "数据连接名称",
    value: "",
  },
  {
    key: "datasourceType",
    label: "数据连接类型",
    value: "",
  },
  {
    key: "fileName",
    label: "文件名",
    value: "-",
  },
  {
    key: "fileType",
    label: "文件类型",
    value: "-",
  },
  {
    key: "fileSize",
    label: "文件大小（字节）",
    value: "-",
  },
  {
    key: "filePath",
    label: "文件路径",
    value: "-",
  },
  {
      key: "createBy",
      label: "创建人",
      value: "-",
  },
  {
    key: "fileCreateTime",
    label: "创建时间",
    value: "-",
  },
  {
      key: "updateBy",
      label: "更新人",
      value: "-",
  },
  {
    key: "fileLastModified",
    label: "更新时间",
    value: "-",
  },
  // {
  //   key: "fileTime",
  //   label: "访问时间",
  //   value: "-",
  // },
  {
    key: "remark",
    label: "备注",
    value: "",
    span: 24,
  },
]);
const getDescValue = (row) => {
  let detail = { ...props.form1 };
  if (props.form1) {
    if (props.form1.type == 7) {
      detail = {
        ...detail,
        ...{
          fileName: props.form1.fileInfo?.name,
          filePath: props.form1.fileInfo?.path,
          fileType: props.form1.fileInfo?.type,
          fileSize: props.form1.fileInfo?.size,
          fileCreateTime: props.form1.fileInfo?.createTime,
          fileLastModified: props.form1.fileInfo?.lastModified,
          fileTime: props.form1.fileInfo?.time,
        },
      };
    }
    row.value = detail[row.key];
  }
  return row.value !== null && row.value !== undefined ? row.value : "-";
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

