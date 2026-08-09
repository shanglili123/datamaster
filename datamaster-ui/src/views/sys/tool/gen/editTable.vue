<template>
  <div class="dataBody">
    <a-card class="app-container">
      <a-tabs v-model:activeKey="activeName">
        <a-tab-pane key="basic" tab="基本信息">
          <basic-info-form ref="basicInfo" :info="info" />
        </a-tab-pane>
        <a-tab-pane key="columnInfo" tab="字段信息">
          <a-table
            ref="dragTable"
            striped
            row-key="columnId"
            :columns="tableColumns"
            :data-source="columns"
            :scroll="{ y: tableHeight }"
            :pagination="false"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.dataIndex === 'index'">{{ index + 1 }}</template>
              <template v-else-if="column.dataIndex === 'columnComment'">
                <a-input v-model:value="record.columnComment"></a-input>
              </template>
              <template v-else-if="column.dataIndex === 'javaType'">
                <a-select v-model:value="record.javaType">
                  <a-select-option value="Long">Long</a-select-option>
                  <a-select-option value="String">String</a-select-option>
                  <a-select-option value="Integer">Integer</a-select-option>
                  <a-select-option value="Double">Double</a-select-option>
                  <a-select-option value="BigDecimal">BigDecimal</a-select-option>
                  <a-select-option value="Date">Date</a-select-option>
                  <a-select-option value="Boolean">Boolean</a-select-option>
                </a-select>
              </template>
              <template v-else-if="column.dataIndex === 'javaField'">
                <a-input v-model:value="record.javaField"></a-input>
              </template>
              <!--          插入列（已禁用）
                          <template v-else-if="column.dataIndex === 'isInsert'">
                            <a-checkbox checked-value="1" un-checked-value="0" v-model:checked="record.isInsert"></a-checkbox>
                          </template>-->
              <template v-else-if="column.dataIndex === 'isEdit'">
                <a-checkbox checked-value="1" un-checked-value="0" v-model:checked="record.isEdit"></a-checkbox>
              </template>
              <template v-else-if="column.dataIndex === 'isList'">
                <a-checkbox checked-value="1" un-checked-value="0" v-model:checked="record.isList"></a-checkbox>
              </template>
              <template v-else-if="column.dataIndex === 'isQuery'">
                <a-checkbox checked-value="1" un-checked-value="0" v-model:checked="record.isQuery"></a-checkbox>
              </template>
              <template v-else-if="column.dataIndex === 'queryType'">
                <a-select v-model:value="record.queryType">
                  <a-select-option value="EQ">=</a-select-option>
                  <a-select-option value="NE">!=</a-select-option>
                  <a-select-option value="GT">&gt;</a-select-option>
                  <a-select-option value="GTE">&gt;=</a-select-option>
                  <a-select-option value="LT">&lt;</a-select-option>
                  <a-select-option value="LTE">&lt;=</a-select-option>
                  <a-select-option value="LIKE">LIKE</a-select-option>
                  <a-select-option value="BETWEEN">BETWEEN</a-select-option>
                </a-select>
              </template>
              <template v-else-if="column.dataIndex === 'isRequired'">
                <a-checkbox checked-value="1" un-checked-value="0" v-model:checked="record.isRequired"></a-checkbox>
              </template>
              <template v-else-if="column.dataIndex === 'htmlType'">
                <a-select v-model:value="record.htmlType">
                  <a-select-option value="input">文本框</a-select-option>
                  <a-select-option value="textarea">文本域</a-select-option>
                  <a-select-option value="select">下拉框</a-select-option>
                  <a-select-option value="radio">单选框</a-select-option>
                  <a-select-option value="checkbox">复选框</a-select-option>
                  <a-select-option value="datetime">日期控件</a-select-option>
                  <a-select-option value="imageUpload">图片上传</a-select-option>
                  <a-select-option value="fileUpload">文件上传</a-select-option>
                  <a-select-option value="editor">富文本控件</a-select-option>
                </a-select>
              </template>
              <template v-else-if="column.dataIndex === 'dictType'">
                <a-select v-model:value="record.dictType" allow-clear show-search option-filter-prop="label" placeholder="请选择">
                  <a-select-option
                          v-for="dict in dictOptions"
                          :key="dict.dictType"
                          :value="dict.dictType"
                          :label="dict.dictName">
                    <span style="float: left">{{ dict.dictName }}</span>
                    <span style="float: right; color: #8492a6; font-size: 13px">{{ dict.dictType }}</span>
                  </a-select-option>
                </a-select>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="genInfo" tab="生成信息">
          <gen-info-form ref="genInfo" :info="info" :tables="tables" />
        </a-tab-pane>
      </a-tabs>
      <div style="text-align: center; margin-top: 10px">
        <a-button @click="close()">返回</a-button>
        <a-button type="primary" @click="submitForm()">提交</a-button>
      </div>
    </a-card>
  </div>

</template>

<script setup name="GenEdit">
import { getGenTable, updateGenTable } from "@/api/system/tool/gen.js";
import { optionselect as getDictOptionselect } from "@/api/system/system/dict/type.js";
import basicInfoForm from "./basicInfoForm.vue";
import genInfoForm from "./genInfoForm.vue";

const route = useRoute();
const { proxy } = getCurrentInstance();

const activeName = ref("columnInfo");
const tableHeight = ref(document.documentElement.scrollHeight - 245 + "px");
const tables = ref([]);
const columns = ref([]);
const dictOptions = ref([]);
const info = ref({});

const tableColumns = [
  { title: "序号", dataIndex: "index", width: 80, align: "center" },
  { title: "字段列名", dataIndex: "columnName", width: 150, ellipsis: true },
  { title: "字段描述", dataIndex: "columnComment", align: "center" },
  { title: "物理类型", dataIndex: "columnType", width: 150, ellipsis: true, align: "center" },
  { title: "Java类型", dataIndex: "javaType", width: 150, align: "center" },
  { title: "java属性", dataIndex: "javaField", width: 150, align: "center" },
  { title: "新增/编辑(saveReqVO)", dataIndex: "isEdit", width: 120, align: "center" },
  { title: "列表(respVO)", dataIndex: "isList", width: 120, align: "center" },
  { title: "查询(pageReqVO)", dataIndex: "isQuery", width: 120, align: "center" },
  { title: "查询方式", dataIndex: "queryType", width: 150, align: "center" },
  { title: "必填", dataIndex: "isRequired", width: 60, align: "center" },
  { title: "显示类型", dataIndex: "htmlType", align: "center" },
  { title: "字典类型", dataIndex: "dictType", width: 150, align: "center" }
];

/** 提交按钮 */
function submitForm() {
  const basicForm = proxy.$refs.basicInfo.$refs.basicInfoForm;
  const genForm = proxy.$refs.genInfo.$refs.genInfoForm;
  Promise.all([basicForm, genForm].map(getFormPromise)).then(res => {
    const validateResult = res.every(item => !!item);
    if (validateResult) {
      const genTable = Object.assign({}, info.value);
      genTable.columns = columns.value;
      genTable.params = {
        treeCode: info.value.treeCode,
        treeName: info.value.treeName,
        treeParentCode: info.value.treeParentCode,
        parentMenuId: info.value.parentMenuId
      };
      updateGenTable(genTable).then(res => {
        proxy.$modal.msgSuccess(res.msg);
        if (res.code === 200) {
          close();
        }
      });
    } else {
      proxy.$modal.msgError("表单校验未通过，请重新检查提交内容");
    }
  });
}

function getFormPromise(form) {
  return form.validate().then(() => true).catch(() => false);
}

function close() {
  const obj = { path: "/tool/gen", query: { t: Date.now(), pageNum: route.query.pageNum } };
  proxy.$tab.closeOpenPage(obj);
}

(() => {
  const tableId = route.params && route.params.tableId;
  if (tableId) {
    // 获取表详细信息
    getGenTable(tableId).then(res => {
      columns.value = res.data.rows;
      info.value = res.data.info;
      tables.value = res.data.tables;
    });
    /** 查询字典下拉列表 */
    getDictOptionselect().then(response => {
      dictOptions.value = response.data;
    });
  }
})();
</script>
<style scoped>
/*.pagecont{*/
/*  background: #ffffff;*/
/*  padding: 15px;*/
/*}*/
.dataBody{
  min-height: calc(100vh - 115px);
}
.app-container {
  background-color: white !important;
  min-height: auto;
}
</style>
