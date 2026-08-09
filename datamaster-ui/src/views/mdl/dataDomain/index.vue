<template>
  <div class="app-container" ref="app-container">
    <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="tableStore.params"
          :tableRef="tableRef"
        />
      </template>
      <template #actions-data>
        <a-button
          type="primary"
          @click="handleAdd"
          v-hasPermi="['mdl:dataDomain:add']"
        >
          新增
        </a-button>
      </template>

      <qt-table v-bind="tableStore" ref="tableRef">
        <template #action="{ row }">
          <a-button
            type="link"
            size="small"
            @click="handleUpdate(row)"
            v-hasPermi="['mdl:dataDomain:edit']"
          >
            修改
          </a-button>
          <a-button
            type="link"
            danger
            size="small"
            @click="handleDelete(row)"
            v-hasPermi="['mdl:dataDomain:remove']"
          >
            删除
          </a-button>
          <a-button
            type="link"
            size="small"
            @click="handleDetail(row)"
            v-hasPermi="['mdl:dataDomain:edit']"
          >
            详情
          </a-button>
        </template>
      </qt-table>
    </qt-wrap>

    <!-- 添加或修改数据域管理对话框 -->
    <a-modal
      :title="title"
      v-model:open="open"
      draggable
      width="800px"
      destroy-on-close
    >
      <a-form
        ref="dataDomainRef"
        :model="form"
        :rules="rules"
        :label-col="{ style: { width: '110px' } }"
        @submit.prevent
      >
        <a-form-item label="数据域名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入数据域名称" />
        </a-form-item>
        <a-form-item label="英文缩写" name="engName">
          <a-input v-model:value="form.engName" placeholder="请输入英文缩写" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea
            v-model:value="form.description"
            placeholder="请输入描述"
            :auto-size="{ minRows: 4, maxRows: 8 }"
            show-count
            :maxlength="500"
          />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="form.remark"
            placeholder="请输入备注"
            :auto-size="{ minRows: 4, maxRows: 8 }"
            show-count
            :maxlength="500"
          />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="cancel">取 消</a-button>
          <a-button type="primary" @click="submitForm">确 定</a-button>
        </div>
      </template>
    </a-modal>

    <!-- 数据域管理详情对话框 -->
    <a-modal
      :title="title"
      v-model:open="openDetail"
      draggable
      width="800px"
      destroy-on-close
    >
      <a-form ref="dataDomainDetailRef" :model="form" :label-col="{ style: { width: '110px' } }">
        <a-form-item label="编号:" name="id">
          <div class="form-readonly">
            {{ form.id }}
          </div>
        </a-form-item>
        <a-form-item label="数据域" name="name">
          <div class="form-readonly">{{ form.name ?? "-" }}</div>
        </a-form-item>
        <a-form-item label="英文缩写" name="engName">
          <div class="form-readonly">{{ form.engName ?? "-" }}</div>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <div class="form-readonly textarea">
            {{ form.description ?? "-" }}
          </div>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <div class="form-readonly textarea">{{ form.remark ?? "-" }}</div>
        </a-form-item>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="创建人" name="createBy">
              <div class="form-readonly">
                {{ form.createBy }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="创建时间" name="createTime">
              <div class="form-readonly">
                {{ parseTime(form.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="更新人" name="updateBy">
              <div class="form-readonly">
                {{ form.updateBy }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="更新时间" name="updateTime">
              <div class="form-readonly">
                {{ parseTime(form.updateTime, "{y}-{m}-{d} {h}:{i}") || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <template #footer>
        <div class="dialog-footer">
          <a-button @click="cancel">关 闭</a-button>
        </div>
      </template>
    </a-modal>

    <!-- 用户导入对话框 -->
    <a-modal
      :title="upload.title"
      v-model:open="upload.open"
      draggable
      destroy-on-close
    >
      <a-upload-dragger
        ref="uploadRef"
        :max-count="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        @progress="handleFileUploadProgress"
        @success="handleFileSuccess"
      >
        <CloudUploadOutlined class="ant-upload-drag-icon" />
        <p class="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
      </a-upload-dragger>
      <div class="ant-upload-drag-hint">
        <div>
          <a-checkbox v-model:checked="upload.updateSupport" />是否更新已经存在的数据域数据
        </div>
        <span>仅允许导入xls、xlsx格式文件。</span>
        <a-typography-link
          type="primary"
          style="font-size: 12px; vertical-align: baseline"
          @click="importTemplate"
        >
          下载模板
        </a-typography-link>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="upload.open = false">取 消</a-button>
          <a-button type="primary" @click="submitFileForm">确 定</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="DataDomain">
import {
  getDataDomain,
  addDataDomain,
  updateDataDomain,
  listDataDomain,
  delDataDomain,
} from "@/api/mdl/dataDomain/dataDomain.js";
import { getToken } from "@/utils/auth.js";
import {
  computed,
  getCurrentInstance,
  reactive,
  ref,
  toRefs,
} from "vue";
import { CloudUploadOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();

const tableRef = ref(null);

const ids = ref([]);
const single = ref(true);
const multiple = ref(true);

const open = ref(false);
const openDetail = ref(false);
const title = ref("");

const tableStore = reactive({
  config: {
    stripe: true,
    table: {
      rowKey: "id",
      defaultSort: { prop: "createTime", order: "descending" },
      onSelectionChange: function (selection) {
        ids.value = selection.map((item) => item.id);
        single.value = selection.length !== 1;
        multiple.value = !selection.length;
      },
    },
  },
  columns: [
    // { type: "selection", width: 55, align: "left" },
    { label: "编号", prop: "id", width: 60, sortable: true },
    {
      label: "数据域名称",
      prop: "name",
      align: "left",
      showOverflowTooltip: {
        effect: "light",
      },
    },
    {
      label: "描述",
      prop: "description",
      align: "left",
      width: 240,
      showOverflowTooltip: {
        effect: "light",
      },
    },
    { label: "英文缩写", prop: "engName", align: "left" },
    {
      label: "创建人",
      prop: "createBy",
      showOverflowTooltip: true,
    },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      sortableKey: "create_time",
      date: true,
      width: 150,
      align: "left",
    },
    // { label: "备注", prop: "remark", align: "left" },
    {
      label: "操作",
      width: 220,
      slot: "action",
      fixed: "right",
    },
  ],
  func: listDataDomain,
  params: {},
});

const searchStore = reactive({
  items: [
    {
      label: "数据域名称",
      prop: "name",
      component: { is: "input", placeholder: "请输入数据域名称" },
    },
    {
      label: "英文缩写",
      prop: "engName",
      component: { is: "input", placeholder: "请输入英文缩写" },
    },
  ],
});

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
  url: import.meta.env.VITE_APP_BASE_API + "/mdl/dataDomain/importData",
});

const data = reactive({
  form: {},
  rules: {
    name: [{ required: true, message: "请输入数据域名称", trigger: "blur" }],
    engName: [
      { required: true, message: "请输入英文缩写", trigger: "blur" },
      { pattern: /^[a-zA-Z]+$/, message: "只能输入英文字符", trigger: "blur" },
    ],
  },
});

const { form, rules } = toRefs(data);

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
    engName: null,
    ownerUserId: null,
    ownerUserPhoneNumber: null,
    description: null,
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
  proxy.resetForm("dataDomainRef");
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加数据域";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row?.id || ids.value[0];
  getDataDomain(_id).then((response) => {
    form.value = response.data;

    open.value = true;
    title.value = "修改数据域";
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row?.id || ids.value[0];
  getDataDomain(_id).then((response) => {
    form.value = response.data;

    openDetail.value = true;
    title.value = "数据域详情";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["dataDomainRef"]
    .validate()
    .then(() => {
      if (form.value.id != null) {
        updateDataDomain(form.value)
          .then(() => {
            proxy.$modal.msgSuccess("修改成功");
            open.value = false;
            tableRef.value.getList();
          })
          .catch(() => {});
      } else {
        addDataDomain(form.value)
          .then(() => {
            proxy.$modal.msgSuccess("新增成功");
            open.value = false;
            tableRef.value.getList();
          })
          .catch(() => {});
      }
    })
    .catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row?.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除数据域编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delDataDomain(_ids);
    })
    .then(() => {
      tableRef.value.getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** ---------------- 导入相关操作 -----------------**/
/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    "system/user/importTemplate",
    {},
    `dataDomain_template_${new Date().getTime()}.xlsx`
  );
}

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
}

/**文件上传中处理 */
const handleFileUploadProgress = () => {
  upload.isUploading = true;
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file) => {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].handleRemove(file);
  proxy.$alert(
    "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
      response.msg +
      "</div>",
    "导入结果",
    { dangerouslyUseHTMLString: true }
  );
  tableRef.value.getList();
};
/** ---------------------------------**/
</script>

