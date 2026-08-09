<template>
  <div class="container">
    <qt-wrap
      :columns="tableStroe.columns"
      :tableRef="tableRef"
      :config="{ fullContent: false, actions: { table: { search: false } } }"
    >
      <qt-table v-bind="tableStroe" ref="tableRef">
        <template #handle="{ row }">
          <a-button
            type="link"
            :icon="h(EyeOutlined)"
            :disabled="!row.dataElemId"
            @click="handleDataElemClick(row)"
          >
            查看标准数据元
          </a-button>

          <a-button
            type="link"
            :icon="h(EyeOutlined)"
            @click="handleSensitiveLevelClick(row)"
            :disabled="!row.safetyLevelId"
          >
            查看安全等级
          </a-button>
        </template>
      </qt-table>
    </qt-wrap>

    <a-modal
      title="安全等级详情"
      v-model:open="dialog.open"
      width="800"
      destroy-on-close
    >
      <a-form :label-col="{ style: { width: '100px' } }" class="column-form">
        <a-form-item label="编号">
          <div class="form-readonly">
            {{ getFormatValue(dialog.form.id) }}
          </div>
        </a-form-item>
        <a-form-item label="级别名称">
          <div class="form-readonly">
            {{ getFormatValue(dialog.form.sensitiveLevel) }}
          </div>
        </a-form-item>
        <a-form-item label="替换规则">
          <dict-tag
            :options="toValue(dicts.da_sensitive_level_rule)"
            :value="dialog.form.sensitiveRule"
          />
        </a-form-item>
        <a-form-item label="状态">
          <dict-tag
            :options="toValue(dicts.da_sensitive_status)"
            :value="dialog.form.onlineFlag"
          />
        </a-form-item>
        <a-form-item label="起始字符位置">
          <div class="form-readonly">
            {{ getFormatValue(dialog.form.startCharLoc) }}
          </div>
        </a-form-item>
        <a-form-item label="截止字符位置">
          <div class="form-readonly">
            {{ getFormatValue(dialog.form.endCharLoc) }}
          </div>
        </a-form-item>
        <a-form-item label="替换内容">
          <div class="form-readonly">
            {{ getFormatValue(dialog.form.maskCharacter) }}
          </div>
        </a-form-item>
        <a-form-item label="描述" class="row-full">
          <div class="form-readonly textarea">
            {{ getFormatValue(dialog.form.description) }}
          </div>
        </a-form-item>
        <a-form-item label="备注" class="row-full">
          <div class="form-readonly textarea">
            {{ getFormatValue(dialog.form.remark) }}
          </div>
        </a-form-item>
        <a-form-item label="创建人">
          <div class="form-readonly">
            {{ getFormatValue(dialog.form.createBy) }}
          </div>
        </a-form-item>
        <a-form-item label="创建时间">
          <div class="form-readonly">
            {{
              getFormatValue(
                parseTime(dialog.form.createTime, "{y}-{m}-{d} {h}:{i}")
              )
            }}
          </div>
        </a-form-item>
        <a-form-item label="更新人">
          <div class="form-readonly">
            {{ getFormatValue(dialog.form.updateBy) }}
          </div>
        </a-form-item>
        <a-form-item label="更新时间">
          <div class="form-readonly">
            {{
              getFormatValue(
                parseTime(dialog.form.updateTime, "{y}-{m}-{d} {h}:{i}")
              )
            }}
          </div>
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="dialog.open = false">关 闭</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="DataGovern">
import { reactive, ref, toValue, getCurrentInstance, h } from "vue";
import { EyeOutlined } from '@ant-design/icons-vue';
import { listColumn } from "@/api/cat/unreleased/column.js";
import { useRouter } from "vue-router";
import { listDgSensitiveLevel, getDgSensitiveLevel } from "@/api/cat/compliance/sensitiveLevel";

const BASE_URL = "/meta/unreleased/structured/column";

const props = defineProps({
  detail: {
    type: Object,
    required: true,
  },
});

const router = useRouter();

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict("da_sensitive_level_rule", "da_sensitive_status");

const store = reactive({});

const dialog = reactive({
  open: false,
  form: {},
});

const tableRef = ref();
const tableStroe = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: { prop: "createTime", order: "descending" },
      onRowDblclick: handleDetailClick,
    },
  },
  columns: [
    {
      label: "编号",
      prop: "id",
      sortable: true,
      width: 105,
    },
    {
      label: "字段名称",
      prop: "columnName",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 140,
      link: {
        external: handleDetailClick,
      },
    },
    {
      label: "字段注释",
      prop: "columnComment",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 140,
    },

    {
      label: "标准数据元",
      prop: "dataElemName",
      width: 110,
    },
    {
      label: "字段类型",
      prop: "columnType",
      width: 110,
      dict: "column_type",
    },
    {
      label: "字段长度",
      prop: "columnLength",
      width: 100,
      sortable: true,
    },
    {
      label: "是否必填",
      prop: "nullableFlag",
      width: 90,
      dict: "table_yes_no",
    },
    {
      label: "更新人",
      prop: "updateBy",
      width: 120,
    },
    {
      label: "更新时间",
      prop: "updateTime",
      sortable: true,
      width: 160,
      date: true,
    },
    {
      label: "创建人",
      prop: "createBy",
      width: 120,
    },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      width: 160,
      date: true,
    },
    {
      label: "操作",
      width: 280,
      fixed: "right",
      slot: "handle",
    },
  ],
  func: listColumn,
  params: {
    tableId: props.detail.id,
    dataType: 1,
  },
});

// 获取安全等级
function getSensitiveLevel() {
  listDgSensitiveLevel({ pageSize: 1000 }).then((res) => {
    store.sensitiveLevels = res.data.rows;
  });
}

// // 获取标准数据元
// function getDataElem() {
//   getDgDataElemList().then((res) => {
//     store.dataElemList = res.data;
//   });
// }

// 详情
function handleDetailClick(row) {
  router.push({
    path: BASE_URL + "/detail",
    query: {
      id: row.id,
    },
  });
}

// 查看标准数据元
function handleDataElemClick(row) {
  router.push({
    path: "/std/dataElem/column/detail",
    query: {
      id: row.dataElemId,
    },
  });
}

// 查看安全等级
function handleSensitiveLevelClick(row) {
  dialog.open = true;
  getDgSensitiveLevel(row.safetyLevelId).then((res) => {
    dialog.form = res.data;
  });
}

getSensitiveLevel();
</script>
