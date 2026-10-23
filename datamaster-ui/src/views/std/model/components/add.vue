<template>
  <a-modal
    :title="title"
    v-model:open="visibleDialog"
    draggable
    class="large-dialog"
    :destroy-on-close="true"
  >
    <a-form
      ref="dpModelRef"
      :model="form"
      :rules="rules"
      :disabled="submitting"
      :label-col="{ style: { width: '110px' } }"
      @submit.prevent
    >
      <a-form-item v-if="!form.id" label="创建方式" name="createType">
        <a-radio-group v-model:value="form.createType">
          <a-radio v-for="dict in dp_model_create_type" :key="dict.value" :value="dict.value">{{ dict.label
          }}</a-radio>
        </a-radio-group>
      </a-form-item>
      <div class="h2-title">基础信息</div>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="中文名称" name="modelComment">
            <a-input v-model:value="form.modelComment" placeholder="请输入中文名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="governanceRequired ? '业务过程编码' : '英文名称'" name="modelName">
            <a-input v-model:value="form.modelName" placeholder="请输入基础业务过程编码，不含分层/域前缀" :disabled="hasFixedName || (isLegacy && !upgradeNaming)" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="逻辑模型目录" name="catCode">
            <!-- <a-input v-model:value="form.contact" placeholder="请输入联系人" /> -->
            <a-tree-select show-search v-model:value="form.catCode" :tree-data="deptOptions"
              :field-names="{ value: 'code', label: 'name', children: 'children' }" placeholder="请选择逻辑模型目录"
              tree-check-strictly
/>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述" name="description">
            <a-textarea v-model:value="form.description" placeholder="请输入描述" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20" v-if="type != 3">
        <a-col :span="12">
          <a-form-item label="标准类型" name="description">
            <a-select v-model:value="form.documentType" placeholder="请选择类型" allow-clear
              @change="fetchSecondLevelDocs" style="width: 100%;"
>
              <a-select-option v-for="dict in dp_document_type" :key="dict.value" :value="dict.value">{{ dict.label
              }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="标准登记" name="documentId">
            <a-select v-model:value="form.documentId" placeholder="请选择标准进行绑定"
              style="width: 100%;" allow-clear
>
              <a-select-option v-for="doc in secondLevelDocs" :key="doc.value" :value="doc.value">{{ doc.label
              }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="form.status">
              <a-radio v-for="dict in dp_model_status" :key="dict.value" :value="dict.value">{{ dict.label
              }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="form.remark" placeholder="请输入备注" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="h2-title">治理命名</div>
      <a-alert v-if="isLegacy" type="info" show-icon message="历史模型默认仅维护普通字段；只有显式升级才应用治理命名，不会修改来源物理表。" style="margin-bottom:12px" />
      <a-form-item v-if="isLegacy" label="升级命名">
        <a-checkbox v-model:checked="upgradeNaming">升级为规范治理命名（GOVERNED_V1）</a-checkbox>
      </a-form-item>
      <template v-if="governanceRequired">
        <a-alert v-if="hasFixedName" type="info" show-icon message="已有规范物理表名保持不变；治理校验不会因分类或字典变化重新命名。" style="margin-bottom:12px" />
        <GovernedTableFields :model="form" :naming="naming" :disabled="submitting" :identity-locked="hasFixedName" />
        <a-form-item v-if="isLegacy" label="原目标名称">
          <a-input :value="originalTargetName" readonly />
        </a-form-item>
        <a-alert v-if="isLegacy && upgradeNaming" type="info" show-icon message="以下新名称仅为规范命名预览；已有目标物理表名可能由服务端保留，最终以保存后详情为准。升级不会重命名来源物理表。" style="margin-bottom:12px" />
        <a-form-item :label="isLegacy ? '规范命名预览' : '目标物理表名'">
          <a-input :value="targetTableName" readonly placeholder="请先预览并校验命名" />
          <a-button style="margin-top:8px" :loading="naming.previewLoading" @click="naming.preview()">{{ hasFixedName ? '校验治理信息' : '预览最终表名' }}</a-button>
          <div style="margin-top:6px">仅用于模型目标命名，不重命名来源表；新建默认小写，已有大小写规则保持，最终名称以服务端为准。</div>
        </a-form-item>
        <a-alert v-if="naming.previewError" type="error" show-icon :message="naming.previewError" style="margin-bottom:12px" />
      </template>
      <a-form-item v-if="form.id && form.tableName" label="来源物理表">
        <a-input :value="form.tableName" readonly />
      </a-form-item>

      <div v-if="form.createType == 2">
        <a-divider orientation="center">
          <span class="blue-text">数据源</span>
        </a-divider>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="数据库连接" name="datasourceId" :rules="[
              {
                required: true,
                message: '请选择数据库连接',
                trigger: 'change',
              },
            ]"
>
              <a-select v-model:value="form.datasourceId" placeholder="请选择数据连接" @change="handleDatasourceChange"
                show-search
>
                <a-select-option v-for="dict in createTypeList" :key="dict.id" :value="dict.id">{{ dict.datasourceName
                }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据库类型" name="datasourceType">
              <a-input v-model:value="form.datasourceType" placeholder="请输入数据库类型" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="数据库地址" name="ip">
              <a-input v-model:value="form.ip" placeholder="请输入数据库类型" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="选择表" name="tableName" :rules="[
              { required: true, message: '表不能为空', trigger: 'change' },
            ]"
>
              <a-select v-model:value="form.tableName" placeholder="请选择表" @change="handleChange(true)" show-search>
                <a-select-option v-for="item in TablesByDataSource" :key="item.tableName" :value="item.tableName">{{
                  item.tableName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </div>
    </a-form>

    <div class="h2-title">属性字段</div>
    <a-button style="margin-bottom: 5px;margin-top: 10px;" type="primary" @click="handleAdd" size="small"
      @mousedown="(e) => e.preventDefault()"
>
      <i class="iconfont-mini icon-xinzeng mr5"></i>新增
    </a-button>
    <a-spin :spinning="loading">
      <a-table
        :data-source="tableData"
        :columns="tableColumns"
        :pagination="false"
        style="width: 100%"
        :locale="{ emptyText: '暂无数据' }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">
            {{ index + 1 }}
          </template>
          <template v-else-if="column.dataIndex === 'pkFlag'">
            <a-switch v-model:checked="record.pkFlag" :checked-value="'1'" :un-checked-value="'0'" disabled />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="editRow(record, index)">编辑</a-button>
            <a-button type="link" danger size="small" @click="deleteRow(record)">删除</a-button>
          </template>
          <template v-else>
            <span>{{ record[column.dataIndex] || '-' }}</span>
          </template>
        </template>
      </a-table>
    </a-spin>

    <template #footer>
      <div class="dialog-footer">
        <a-button @click="closeDialog">取消</a-button>
        <a-button type="primary" :loading="submitting" @click="confirmDialog">确认</a-button>
      </div>
    </template>
  </a-modal>

  <columnAdd :visible="addDialog" @update:dialogFormVisible="addDialog = $event" @confirm="handleFormSubmit"
    :deptOptions="deptOptions" :userList="userList" :deptList="deptList" :row="selectedRow" :data="form"
/>
</template>

<script setup>
const { proxy } = getCurrentInstance();
import GovernedTableFields from '@/components/GovernedTableFields.vue';
import { useGovernedTableNaming } from '@/composables/useGovernedTableNaming';
import {
  listDpDocument,
} from "@/api/std/document/document";

import {
  getDaDatasourceList,
  tableList,
  columnsList,
} from "@/api/std/model/model";

import columnAdd from "./columnAdd";

import { defineProps, defineEmits, ref, computed, watch } from "vue";

import { getDpModelColumnList } from "@/api/std/model/model";
const { dp_model_status, dp_model_create_type, dp_document_type } = proxy.useDict(
  "dp_model_status",
  "dp_model_create_type",
  "dp_document_type"
);
const props = defineProps({
  visible: { type: Boolean, default: true },
  title: { type: String, default: "表单标题" },
  deptOptions: { type: Array, default: () => [] },
  column_type: { type: Array, default: () => [] },
  userList: { type: Array, default: () => [] },
  deptList: { type: Array, default: () => [] },
  dataList: { type: Object, default: () => { } },
  catCode: { type: Object, default: () => { } },
  type: { type: String, default: "" },
});
let loading = ref(false);
let formRequest = 0;
let columnsRequest = 0;

let secondLevelDocs = ref([]);
const btnloading = ref(false);

const fetchSecondLevelDocs = async (type, preserveSelection = false) => {
  if (!type) {
    secondLevelDocs.value = [];
    if (!preserveSelection) {
      form.value.documentId = '';
    }
    return;
  }

  const request = formRequest;
  const documentType = type;
  try {
    btnloading.value = true;
    const res = await listDpDocument({ type });
    if (request !== formRequest || form.value.documentType !== documentType) return;
    secondLevelDocs.value = (res.data.rows || []).map(d => ({
      label: d.name,
      value: d.id,
    }));

    // 只有在不是保留选择的情况下才清空
    if (!preserveSelection) {
      form.value.documentId = '';
    }
  } catch (error) {
    if (request !== formRequest || form.value.documentType !== documentType) return;
    secondLevelDocs.value = [];
    if (!preserveSelection) {
      form.value.documentId = '';
    }
  } finally {
    if (request === formRequest) btnloading.value = false;
  }
}

let createTypeList = ref([]);
const getDaDatasourceListList = async () => {
  try {
    const response = await getDaDatasourceList();
    createTypeList.value = response.data;
  } catch (error) {
    console.error("请求失败:", error);
  }
};
// 表
let TablesByDataSource = ref([]);
const getTablesByDataSourceIdList = async () => {
  const request = formRequest;
  const datasourceId = form.value.datasourceId;
  try {
    const response = await tableList(datasourceId);
    if (request === formRequest && datasourceId === form.value.datasourceId) TablesByDataSource.value = response.data || [];
  } catch (error) {
    if (request === formRequest) proxy.$message.warning('来源表加载失败，请检查数据源权限');
  }
};
const fetchDpModelColumnList = async () => {
  const request = formRequest;
  const columns = ++columnsRequest;
  loading.value = true;
  try {
    const response = await getDpModelColumnList({ modelId: form.value.id });
    if (request === formRequest && columns === columnsRequest) tableData.value = response.data || [];
  } catch (error) {
    if (request === formRequest) proxy.$message.warning('模型属性字段加载失败，请重试');
  } finally {
    if (request === formRequest && columns === columnsRequest) loading.value = false;
  }
};
const getColumnByAssetIdList = async (isOld) => {
  const request = formRequest;
  const columns = ++columnsRequest;
  const source = { modelId: form.value.id, id: form.value.datasourceId, tableName: form.value.tableName, type: form.value.datasourceType, isOld };
  loading.value = true;
  try {
    const response = await columnsList(source);
    if (request === formRequest && columns === columnsRequest && source.id === form.value.datasourceId && source.tableName === form.value.tableName) tableData.value = response.data || [];
  } catch (error) {
    if (request === formRequest) proxy.$message.warning('来源表字段导入失败，请检查数据源权限');
  } finally {
    if (request === formRequest && columns === columnsRequest) loading.value = false;
  }
};
const handleDatasourceChange = (value) => {
  const selectedDatasource = createTypeList.value.find(
    (item) => item.id === value
  );
  if (selectedDatasource) {
    form.value.tableName = "";
    TablesByDataSource.value = [];
    tableData.value = [];
    ++columnsRequest;
    loading.value = false;
    form.value.ip = selectedDatasource.ip;
    form.value.datasourceConfig = selectedDatasource.datasourceConfig;
    form.value.datasourceType = selectedDatasource.datasourceType;
    form.value.datasourceName = selectedDatasource.datasourceName;
    form.value.port = selectedDatasource.port;
    getTablesByDataSourceIdList();
  }
};
const handleChange = (isOld) => {
  const table = TablesByDataSource.value.find(
    (item) => item.tableName == form.value.tableName
  );
  if (table) {
    if(table.tableComment){
      form.value.modelComment = table.tableComment;
    }
    if (!form.value.modelName) form.value.modelName = table.tableName;
  }
  tableData.value = [];

  getColumnByAssetIdList(isOld);
};
let addDialog = ref(false);
let selectedRow = ref({});
const visibleDialog = computed({
  get() {
    return props.visible;
  },
  set(newValue) {
    emit("update:visible", newValue); // 使用 emit 触发父组件更新
  },
});

const emit = defineEmits(['update:visible', 'update:dialogFormVisible', 'confirm', 'submit']);
const createDefaults = () => ({
  documentId: '', documentType: undefined, modelName: '', modelComment: '',
  catCode: props.catCode, createType: '1', description: '', remark: '', status: '0',
  dataConnection: '', dbType: '', dbAddress: '', dataTable: '',
  datasourceId: '', datasourceName: '', datasourceType: '', datasourceConfig: '',
  ip: '', port: '', tableName: '', tableType: '1', dataLayerId: undefined,
  businessCategoryId: null, dataDomainId: undefined, themeDomainId: undefined,
  tableCase: '2', physicalTableName: '', namingVersion: '',
});
const form = ref(createDefaults());
const upgradeNaming = ref(false);
const submitting = ref(false);
const originalTargetName = ref('');
const isLegacy = computed(() => !!form.value.id && form.value.namingVersion !== 'GOVERNED_V1');
const governanceRequired = computed(() => !isLegacy.value || upgradeNaming.value);
const hasFixedName = computed(() => !!form.value.id && !isLegacy.value && !!form.value.physicalTableName);
const naming = useGovernedTableNaming(() => form.value, () => form.value.modelName, () => form.value.datasourceType);
const targetTableName = computed(() => hasFixedName.value ? form.value.physicalTableName : (naming.previewResult?.tableName || ''));
watch(upgradeNaming, () => naming.resetPreview());

const rules = computed(() => ({
  modelComment: [
    { required: true, message: "表中文名称表不能为空", trigger: "blur" },
  ],
  modelName: [
    { required: true, message: "英文名称不能为空", trigger: "blur" },
    ...(isLegacy.value && !upgradeNaming.value ? [] : [{
      pattern: /^[A-Za-z][A-Za-z0-9_]*$/,
      message: "基础编码只能包含字母、数字和下划线，且必须以字母开头",
      trigger: "blur",
    }]),
  ],
  // status: [{ required: true, message: "发布状态不能为空", trigger: "change" }],
  catCode: [{ required: true, message: "逻辑模型目录不能为空", trigger: "change" }],
  // documentType: [
  //   {
  //     validator: (rule, value, callback) => {
  //       if (value && !form.value.documentId) {
  //         callback(new Error('请选择标准登记'));
  //       } else {
  //         callback();
  //       }
  //     },
  //     trigger: 'change'
  //   }
  // ],
  // documentId: [
  //   {
  //     validator: (rule, value, callback) => {
  //       if (form.value.documentType && !value) {
  //         callback(new Error('请选择标准登记'));
  //       } else {
  //         callback();
  //       }
  //     },
  //     trigger: 'change'
  //   }
  // ]
}));
const tableData = ref([]);

const tableColumns = [
  { title: '编号', key: 'index', align: 'left', width: 60 },
  { title: '关联标准', dataIndex: 'dataElemName', align: 'left', width: 250, ellipsis: true },
  { title: '中文名称', dataIndex: 'cnName', align: 'left', width: 250, ellipsis: true },
  { title: '英文名称', dataIndex: 'engName', align: 'left', width: 250, ellipsis: true },
  { title: '描述', dataIndex: 'description', align: 'left', width: 250, ellipsis: true },
  { title: '数据类型', dataIndex: 'columnType', align: 'center', width: 100, ellipsis: true },
  { title: '属性长度', dataIndex: 'columnLength', align: 'center', width: 80 },
  { title: '是否主键', dataIndex: 'pkFlag', align: 'center', width: 80 },
  { title: '操作', key: 'actions', align: 'center', width: 150 },
];
function getDeptLabel(row) {
  // 递归查找树形结构中匹配的节点
  const findLabel = (tree) => {
    for (let node of tree) {
      if (node.id == row.authorityDept) {
        console.log("node", node);

        return node.label;
      }
      if (node.children) {
        const found = findLabel(node.children);
        if (found) return found;
      }
    }
    return null;
  };
  return findLabel(props.deptList) || "-";
}
//表字段的新增
function handleFormSubmit(formData) {
  console.log("提交的表单数据:", formData);
  if (formData.index !== undefined && formData.index !== null) {
    // 如果存在 index，则直接修改对应索引的数据
    tableData.value[formData.index] = { ...formData };
    console.log("数据已修改:", tableData.value[formData.index]);
  } else {
    // 如果没有 index，则新增数据
    tableData.value.push({ ...formData });
    console.log("新数据已新增:", formData);
  }

  console.log("当前表格数据:", tableData.value);
}

function handleAdd() {
  selectedRow.value = {};
  addDialog.value = true;
  return
  proxy.$refs["dpModelRef"].validate((valid) => {
    if (valid) {
      selectedRow.value = {};
      addDialog.value = true;
    } else {
      proxy.$message.warning("添加失败，基本信息填写完整后才能继续操作");
    }
  });
}

const editRow = (row, i) => {
  selectedRow.value = {};
  selectedRow.value = { ...row, index: i };
  addDialog.value = true;
};

const deleteRow = (row) => {
  const index = tableData.value.indexOf(row);
  if (index !== -1) {
    tableData.value.splice(index, 1);
  }
};

watch(
  () => [props.visible, props.dataList?.id],
  ([visible]) => {
    ++formRequest;
    naming.cancel();
    if (!visible) return;
    getDaDatasourceListList();
    upgradeNaming.value = false;
    submitting.value = false;
    TablesByDataSource.value = [];
    tableData.value = [];
    loading.value = false;
    // Always start from fresh defaults, including when switching between old records.
    form.value = { ...createDefaults(), ...(props.dataList?.id ? props.dataList : {}) };
    form.value.tableType = String(form.value.tableType || '1');
    if (!form.value.id) form.value.tableCase = '2';
    originalTargetName.value = form.value.physicalTableName || form.value.modelName || '';
    if (form.value.id) {
      form.value.documentId = Number(form.value.documentId) || '';
      if (form.value.documentId === -1) form.value.documentId = null;
      fetchSecondLevelDocs(form.value.documentType, true);
      // Never repurpose the original imported tableName as a target name.
      form.value.createType = '1';
      fetchDpModelColumnList();
    } else secondLevelDocs.value = [];
    if (governanceRequired.value) naming.loadOptions();
  },
  { immediate: true }
);
watch(upgradeNaming, value => {
  if (value) naming.loadOptions();
});

const closeDialog = () => {
  ++formRequest;
  naming.cancel();
  form.value = createDefaults();
  tableData.value = [];
  proxy.resetForm("dpModelRef");
  emit("update:visible", false);
};

const confirmDialog = async () => {
  if (submitting.value) return;
  if (!tableData.value?.length) return proxy.$message.warning('操作失败，请添加属性字段');
  const request = formRequest;
  submitting.value = true;
  try {
    await proxy.$refs.dpModelRef.validate();
    if (governanceRequired.value) {
      if (form.value.id && !isLegacy.value && !form.value.physicalTableName) {
        return proxy.$message.warning('规范模型缺少物理表名，请先修复数据，不能按基础编码猜测目标名称');
      }
      const result = await naming.preview();
      if (!result || request !== formRequest || !props.visible) return;
      // Preserve any existing target identity; the server decides legacy upgrade adoption.
      if (!form.value.id || !form.value.physicalTableName) form.value.physicalTableName = result.tableName;
    }
    if (request !== formRequest || !props.visible) return;
    const submitted = { ...form.value, tableCase: form.value.tableCase || '2', documentId: form.value.documentId || -1, upgradeNaming: isLegacy.value && upgradeNaming.value };
    submitted.governanceProvided = governanceRequired.value;
    if (governanceRequired.value) {
      // Explicit null clears a previous category; undefined would disappear during JSON serialization.
      submitted.businessCategoryId = form.value.businessCategoryId === undefined || form.value.businessCategoryId === '' ? null : form.value.businessCategoryId;
    }
    if (isLegacy.value && !upgradeNaming.value) {
      // Ordinary legacy edits must not introduce default governance or change naming identity.
      ['tableType', 'dataLayerId', 'businessCategoryId', 'dataDomainId', 'themeDomainId', 'tableCase', 'physicalTableName', 'namingVersion'].forEach(key => delete submitted[key]);
    }
    const columns = tableData.value.map(item => form.value.id ? { ...item, modelId: form.value.id } : { ...item });
    emit('confirm', { form: submitted, tableData: columns, ...(form.value.id ? { modelId: form.value.id } : {}) });
    closeDialog();
  } catch (error) {
    // Field validation is rendered inline; preview failures are rendered above.
  } finally {
    if (request === formRequest) submitting.value = false;
  }
};
</script>

<style scoped lang="less">
.blue-text {
  color: #2666fb;
}

.dialog {
  min-height: 300px;
  max-height: 900px;
  overflow: auto;
}
</style>

