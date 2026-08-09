<template>
  <div class="app-container" ref="app-container">
    <!-- 使用公共页头组件 -->
    <PageHeader
      :queryParams="queryParams"
      :showSearch="showSearch"
      :showAddBtn="true"
      :addPermission="['tax:cleanCat:add']"
      :showToggleBtn="true"
      :isExpandAll="isExpandAll"
      @query="handleQuery"
      @reset="resetQuery"
      @add="handleAdd"
      @toggle="toggleExpandAll"
      @queryTable="getList"
    >
      <!-- 核心：在 searchForm 插槽中填入当前页面特有的搜索项 -->
      <template #searchForm>
        <a-form-item label="清洗规则目录名称" name="name" :label-col="{ style: { width: '120px' } }">
          <a-input v-model:value="queryParams.name" placeholder="请输入名称" allow-clear @pressEnter="handleQuery" style="width: 200px;" />
        </a-form-item>
        <a-form-item label="上级目录" name="code">
          <a-tree-select v-model:value="queryParams.code" :tree-data="attAssetCatOptions"
            :field-names="{ value: 'code', label: 'name', children: 'children' }"
            placeholder="请选择" allow-clear style="width: 200px;" />
        </a-form-item>
      </template>
    </PageHeader>


    <div class="pagecont-bottom">


      <a-spin :spinning="loading">
        <a-table
          v-if="refreshTable"
          :data-source="AttCleanCatList"
          :columns="tableColumns"
          :pagination="false"
          :scroll="{ y: '60vh' }"
          row-key="id"
          :default-expand-all-rows="isExpandAll"
          :children-column-name="'children'"
          :locale="{ emptyText: emptyContent }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'validFlag'">
              <a-switch v-model:checked="record.validFlag" @change="handleStatusChange(record)" />
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              <span>{{ parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['tax:cleanCat:edit']">修改</a-button>
              <a-button type="link" size="small" @click="handleAdd(record)" v-hasPermi="['tax:cleanCat:add']">新增</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['tax:cleanCat:remove']">删除</a-button>
            </template>
            <template v-else>
              <span>{{ record[column.dataIndex] || '-' }}</span>
            </template>
          </template>
        </a-table>
      </a-spin>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
        @pagination="getList" />
    </div>

    <!-- 新增或修改清洗规则目录管理对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" draggable
      destroy-on-close>
      <a-form ref="attCleanCatRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="目录名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入清洗规则目录名称" />
            </a-form-item>
          </a-col>
          <!--            <a-form-item label="类别排序" name="sortOrder">-->
          <!--&lt;!&ndash;              <a-input v-model:value="form.sortOrder" placeholder="请输入类别排序" />&ndash;&gt;-->
          <!--              <a-input-number v-model:value="form.sortOrder"  steps="1" :min="0"  placeholder="请输入类别排序" />-->
          <!--            </a-form-item>-->
          <a-col :span="12">
            <a-form-item label="上级目录" name="parentId">
              <a-tree-select allow-clear :disabled="form.id" v-model:value="form.parentId" :tree-data="attAssetCatOptions"
                :field-names="{ value: 'id', label: 'name', children: 'children' }" placeholder="请选择上级" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="描述">
              <a-textarea placeholder="请输入描述" v-model:value="form.description" :auto-size="{ minRows: 4, maxRows: 8 }" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="排序" name="sortOrder">
              <a-input-number style="width: 100%" v-model:value="form.sortOrder" :min="0" />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="状态" name="validFlag">
              <a-radio-group v-model:value="form.validFlag">
                <a-radio :value="true">启用</a-radio>
                <a-radio :value="false">禁用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="备注">
              <a-textarea placeholder="请输入备注" v-model:value="form.remark" :auto-size="{ minRows: 4, maxRows: 8 }" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="cancel">取 消</a-button>
          <a-button type="primary" @click="submitForm">确 定</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="CleanCat">

import PageHeader from '@/components/Cat/PageHeader.vue';

import { listAttCleanCat, getAttCleanCat, delAttCleanCat, addAttCleanCat, updateAttCleanCat } from "@/api/tax/cat/cleanCat/cleanCat.js";

import { getToken } from "@/utils/auth.js";

import { normalizePage, pageRows } from "@/utils/page.js";
import { h } from 'vue';
const { proxy } = getCurrentInstance();

const tableColumns = [
  { title: '清洗规则目录名称', dataIndex: 'name', align: 'left', width: 200, ellipsis: true },
  { title: '描述', dataIndex: 'description', align: 'left', width: 250, ellipsis: true },
  { title: '排序', dataIndex: 'sortOrder', align: 'left', ellipsis: true },
  { title: '创建人', dataIndex: 'createBy', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
  { title: '状态', dataIndex: 'validFlag', align: 'center' },
  { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const AttCleanCatList = ref([]);
const attAssetCatOptions = ref([]);
// 列显隐信息
const columns = ref([
  { key: 1, label: "类别名称", visible: true },
  { key: 2, label: "关联上级ID", visible: true },
  { key: 3, label: "类别排序", visible: true },
  { key: 4, label: "描述", visible: true },
  { key: 5, label: "层级编码", visible: true },
  { key: 8, label: "创建人", visible: true },
  { key: 10, label: "创建时间", visible: true },
  { key: 14, label: "备注", visible: true }
]);

const getColumnVisibility = (key) => {
  const column = columns.value.find(col => col.key === key);
  // 如果没有找到对应列配置，默认显示
  if (!column) return true;
  // 如果找到对应列配置，根据visible属性来控制显示
  return column.visible;
};

const open = ref(false);
const openDetail = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const defaultSort = ref({ prop: "createTime", order: "desc" });
const router = useRouter();
const refreshTable = ref(true);
const isExpandAll = ref(false);

/*** 用户导入参数 */
const upload = reactive({
  // 是否显示弹出层（用户导入）
  open: false,
  // 弹出层标题（用户导入）
  title: "",
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的用户数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: { Authorization: "Bearer " + getToken() },
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + "/tax/category/importData"
});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    name: null,
    parentId: null,
    sortOrder: null,
    description: null,
    code: null,
    createTime: null,
  },
  rules: {
    name: [{ required: true, message: '清洗规则目录名称不能为空', trigger: 'blur' }],
    parentId: [{ required: true, message: '上级目录不能为空', trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询清洗规则目录列表 */
function getList() {
  loading.value = true;
  listAttCleanCat(queryParams.value).then(response => {
    const page = normalizePage(response);
    const treeData = proxy.handleTree(page.rows, 'id');
    total.value = treeData.length;
    AttCleanCatList.value = pageRows(treeData, total.value, queryParams.value);
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
    name: null,
    parentId: null,
    sortOrder: 0,
    description: null,
    code: null,
    validFlag: true,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("attCleanCatRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.ID);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 改变启用状态值 */
function handleStatusChange(row) {
  const text = row.validFlag === true ? '启用' : '禁用';
  proxy.$modal
    .confirm('确认要"' + text + '","' + row.name + '"清洗规则目录吗？')
    .then(function () {
      updateAttCleanCat({ id: row.id, validFlag: row.validFlag }).then((response) => {
        proxy.$modal.msgSuccess(text + '成功');
        getList();
      }).catch((err) => {
        row.validFlag = !row.validFlag;
      });
    })
    .catch(function () {
      row.validFlag = !row.validFlag;
    });
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}

/** 排序触发事件 */
function handleSortChange(column, prop, order) {
  queryParams.value.orderByColumn = column.prop;
  queryParams.value.isAsc = column.order;
  getList();
}

/** 新增按钮操作 */
function handleAdd(row) {
  reset();
  listAttCleanCat().then((response) => {
    attAssetCatOptions.value = [];
    const data = { id: 0, name: '顶级节点', children: [] };
    data.children = proxy.handleTree(response.data, 'id', 'parentId');
    attAssetCatOptions.value.push(data);
  });
  if (row != null && row.id) {
    form.value.parentId = row.id;
  } else {
    form.value.parentId = 0;
  }
  open.value = true;
  title.value = "添加清洗规则目录";
}

function getDataTree() {
  listAttCleanCat().then((response) => {
    attAssetCatOptions.value = [];
    const data = { id: 0, name: '顶级节点', children: [] };
    data.children = proxy.handleTree(response.data, 'id', 'parentId');
    attAssetCatOptions.value.push(data);
  });
}

/** 修改按钮操作 */
async function handleUpdate(row) {
  reset();
  const response = await listAttCleanCat();
  attAssetCatOptions.value = [];
  // 过滤节点的计算属性
  const filteredDepts = response.data.filter((d) => {
    // 过滤条件：去掉目标部门ID或者祖先中包含目标部门ID的项
    return d.ID !== row.id && !d.parentId.toString().split(',').includes(row.id.toString());
  });
  const data = { id: 0, name: '顶级节点', children: [] };
  data.children = proxy.handleTree(filteredDepts, 'id', 'parentId');
  attAssetCatOptions.value.push(data);
  if (row != null) {
    form.value.parentId = row.parentId;
  }
  getAttCleanCat(row.id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改清洗规则目录";
  });
}


/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _ID = row.ID || ids.value
  getAttCleanCat(_ID).then(response => {
    form.value = response.data;
    openDetail.value = true;
    title.value = "清洗规则目录详情";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["attCleanCatRef"].validate().then(() => {
    if (form.value.id != null) {
      updateAttCleanCat(form.value).then(response => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      }).catch(error => {
      });
    } else {
      addAttCleanCat(form.value).then(response => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      }).catch(error => {
      });
    }
  }).catch(() => { });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除清洗规则目录编号为"' + ids + '"的数据项？').then(function () {
    return delAttCleanCat(ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => { });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('tax/category/export/CLEAN', {
    ...queryParams.value
  }, `AttCleanCat_${new Date().getTime()}.xlsx`)
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = "清洗规则目录导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download("system/user/importTemplate", {
  }, `AttCleanCat_template_${new Date().getTime()}.xlsx`)
}

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
};

/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true;
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].handleRemove(file);
  proxy.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
  getList();
};
/** ---------------------------------**/

function routeTo(link, row) {
  if (link !== "" && link.indexOf("http") !== -1) {
    window.location.href = link;
    return
  }
  if (link !== "") {
    if (link === router.currentRoute.value.path) {
      window.location.reload();
    } else {
      router.push({
        path: link,
        query: {
          id: row.id
        }
      });
    }
  }
}

getList();
getDataTree();
</script>

