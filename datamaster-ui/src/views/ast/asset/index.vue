<template>
  <div class="app-container" ref="app-container">

    <a-layout>
      <DeptTree :deptOptions="deptOptions" :leftWidth="leftWidth" :placeholder="'请输入资产目录名称'" ref="DeptTreeRef"
        @node-click="handleNodeClick"
/>

      <a-layout-content>
        <div class="pagecont-top" v-show="showSearch">
          <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '45px' } }"
            v-show="showSearch" @submit.prevent
>
            <a-form-item label="名称" name="name">
              <a-input style="width: 150px" v-model:value="queryParams.name" placeholder="请输入资产名称" allow-clear
                @pressEnter="handleQuery"
/>
            </a-form-item>
            <a-form-item label="状态" name="status">
              <a-select style="width: 150px" v-model:value="queryParams.status" placeholder="请选择发布状态" allow-clear>
                <a-select-option v-for="dict in da_assets_status" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="主题" name="themeIdList">
              <a-select style="width: 150px" v-model:value="queryParams.themeIdList" :max-tag-count="1" mode="multiple"
                placeholder="请选择主题名称"
>
                <a-select-option v-for="dict in themeList" :key="dict.id" :value="dict.id">{{ dict.name }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="类型" name="status">
              <a-select style="width: 150px" v-model:value="queryParams.type" placeholder="请选择资产类型" allow-clear>
                <a-select-option v-for="dict in da_asset_type" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
                <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
              </a-button>
              <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
                <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
              </a-button>
            </a-form-item>
          </a-form>
          <div class="data-action-btns">
            <a-button type="primary" @click="handleAdd" v-hasPermi="['ast:asset:add']"
              @mousedown="(e) => e.preventDefault()"
>
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
          </div>
        </div>
        <div class="pagecont-bottom pagecont-bottoms">
          <a-table
            striped
            :loading="loading"
            :data-source="daAssetList"
            :columns="tableColumns"
            :pagination="false"
            :scroll="{ x: 1500 }"
            :locale="{ emptyText: '暂无搜索内容～' }"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'name'">
                <a class="asset-name-link" @click="routeTo('/col/asset/detail', record)">{{ record.name }}</a>
              </template>
              <template v-else-if="column.dataIndex === 'type'">
                <a-tag v-if="getTypeName(record)" style="margin-right: 8px;">{{ getTypeName(record) }}</a-tag>
                <a-tag v-if="!unregistered(record)">未注册</a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <a-tag :color="record.status == 2 ? 'success' : 'warning'">{{ record.status == 2 ? "已发布" : "未发布" }}</a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'dataCount'">
                <template v-if="record.type == 1 && unregistered(record)">
                  {{ record.dataCount }}行 / {{ record.fieldCount }}列
                </template>
                <template v-else>-</template>
              </template>
              <template v-else-if="column.dataIndex === 'datasourceName'">
                <template v-if="record.type == 1 && unregistered(record) && record.datasourceType">
                  <overflow-tooltip :text="record.datasourceName" max-width="150px" />
                </template>
                <template v-else>-</template>
              </template>
              <template v-else-if="column.dataIndex === 'tableName'">
                {{ record.tableName && record.tableName != -1 ? record.tableName : "-" }}
              </template>
              <template v-else-if="column.dataIndex === 'catName'">
                {{ record.catName || "-" }}
              </template>
              <template v-else-if="column.dataIndex === 'themeName'">
                {{ record.assetsAssetThemeRelList?.length ? record.assetsAssetThemeRelList.map((ele) => ele.themeName).join(", ") : "-" }}
              </template>
              <template v-else-if="column.dataIndex === 'createTime'">
                {{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}
              </template>
              <template v-else-if="column.dataIndex === 'description'">
                <overflow-tooltip :text="record.description || '-'" max-width="260px" />
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-button v-if="!unregistered(record)" type="link" size="small" @click="handleUpdate(record, 'register')">注册</a-button>
                <a-button v-if="unregistered(record)" type="link" size="small" @click="handleView(record)">详情</a-button>
                <a-button
                  v-if="unregistered(record)"
                  type="link"
                  size="small"
                  :class="record.status == 2 ? 'danger-text' : 'success-text'"
                  @click="handleStatusChange(record)"
                >{{ record.status == 2 ? "撤销发布" : "发布" }}</a-button>
                <a-dropdown>
                  <a-button type="link" size="small">更多<DownOutlined style="font-size: 10px; margin-left: 2px;" /></a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item v-if="unregistered(record)" key="edit" @click="handleUpdate(record)">
                        <EditOutlined />修改
                      </a-menu-item>
                      <a-menu-item v-if="unregistered(record) && record.type == 1" key="refresh" @click="handleRefresh(record)">
                        <ReloadOutlined />更新数据
                      </a-menu-item>
                      <a-menu-item v-if="record.type == 1" key="sync" @click="handleSync(record)">
                        <ReloadOutlined />元数据同步
                      </a-menu-item>
                      <a-menu-item v-if="unregistered(record) && false" key="apply" @click="handleApply(record)">
                        <EditOutlined />申请
                      </a-menu-item>
                      <a-menu-item v-if="record.sourceType == 1" key="delete" @click="handleDelete(record)">
                        <DeleteOutlined />删除
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </template>
            </template>
          </a-table>
          <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList"
          />
        </div>
      </a-layout-content>
    </a-layout>
    <!-- 数据资产详情对话框 -->
    <a-modal :title="title" v-model:open="openDetail" width="800px">
      <template #title>
        <span role="heading" aria-level="2">
          {{ title }}
        </span>
      </template>
      <a-form ref="daAssetRef" :model="form" :label-col="{ style: { width: '80px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="资产名称" name="name">
              <div>
                {{ form.name }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="目录编码" name="catCode">
              <div>
                {{ form.catCode }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="主题id" name="themeId">
              <div>
                {{ form.themeId }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据连接id" name="datasourceId">
              <div>
                {{ form.datasourceId }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="表名称" name="tableName">
              <div>
                {{ form.tableName }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="表描述" name="tableComment">
              <div>
                {{ form.tableComment }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="数据量(条)" name="dataCount">
              <div>
                {{ form.dataCount }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="字段量" name="fieldCount">
              <div>
                {{ form.fieldCount }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <div>
                {{ form.status }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="描述" name="description">
              <div>
                {{ form.description }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="备注" name="remark">
              <div>
                {{ form.remark }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button size="small" @click="cancel">关 闭</a-button>
        </div>
      </template>
    </a-modal>
    <CreateEditModal :deptOptions="deptOptions" :visible="open" :title="title" @update:visible="open = $event"
      @confirm="getList" :data="form" :isRegister="isRegister" type="0"
/>
    <!-- 用户导入对话框 -->

    <a-modal :title="upload.title" v-model:open="upload.open" width="800px"
      destroy-on-close
>
      <a-upload ref="uploadRef" :max-count="1" accept=".xlsx, .xls" :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false"
>
        <a-upload-dragger>
          <p class="ant-upload-drag-icon"><UploadOutlined /></p>
          <p class="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
        </a-upload-dragger>
        <template #tip>
          <div class="ant-upload-hint text-center">
            <div class="ant-upload-hint">
              <a-checkbox v-model:checked="upload.updateSupport" />是否更新已经存在的数据资产数据
            </div>
            <span>仅允许导入xls、xlsx格式文件。</span>
            <a-typography-link :underline="false" style="font-size: 12px; vertical-align: baseline"
              @click="importTemplate"
>下载模板</a-typography-link>
          </div>
        </template>
      </a-upload>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="upload.open = false">取 消</a-button>
          <a-button type="primary" @click="submitFileForm">确 定</a-button>
        </div>
      </template>
    </a-modal>
    <!-- 申请数据资产对话框 -->
    <a-modal :title="titleApply" v-model:open="openApply" width="800px">
      <a-form ref="assetApplyRef" :model="formApply" :rules="rulesApply" :label-col="{ style: { width: '100px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="资产名称">
              <a-input v-model:value="formApply.name" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="英文名称">
              <a-input v-model:value="formApply.tableName" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="主题名称">
              <a-input v-model:value="formApply.themeName" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据连接">
              <a-input v-model:value="formApply.datasourceName" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="数据库地址">
              <a-input v-model:value="formApply.datasourceIp" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据库类型">
              <a-input v-model:value="formApply.datasourceType" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="描述">
              <a-textarea v-model:value="formApply.description" :rows="3" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="申请空间" name="spaceCode">
              <a-select v-model:value="formApply.spaceCode" @change="handleSelectSpace" placeholder="请选择申请空间">
                <a-select-option v-for="item in spaceOptions" :key="item.code" :value="item.code">{{ item.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="phone">
              <a-input v-model:value="formApply.phone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="申请事由" name="applyReason">
              <a-textarea v-model:value="formApply.applyReason" :rows="3" placeholder="请输入申请事由" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="openApply = false">取 消</a-button>
          <a-button type="primary" @click="submitApplyForm">确 定</a-button>
        </div>
      </template>
    </a-modal>
  </div>

</template>

<script setup name="Asset">
import { h } from 'vue';
import { EditOutlined, ReloadOutlined, DeleteOutlined, UploadOutlined, DownOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue'
import {
  getDaAsset,
  delDaAsset,
  updateDaAsset,
  listDppAsset,
  startDaDiscoveryTask,
  syncAsset,
} from "@/api/ast/asset/asset";

import OverflowTooltip from "@/components/OverflowTooltip";

import CreateEditModal from "@/views/col/asset/add";

import { currentUser } from "@/api/tax/space/space.js";

import DeptTree from "@/components/DeptTree";

import { listAttAssetCat } from "@/api/tax/cat/assetCat/assetCat.js";

import { getToken } from "@/utils/auth.js";

import { addDaAssetApply } from "@/api/ast/assetApply/assetApply";

import useUserStore from "@/store/system/user";

import { getThemeList } from "@/api/tax/theme/theme.js";

import { normalizePage, pageRows } from "@/utils/page.js";
const { proxy } = getCurrentInstance();
const { da_assets_status, da_asset_type } = proxy.useDict(
  "da_assets_status",
  "da_asset_source",
  "da_asset_type"
);

const unregistered = (item) => {
  return item.createType == undefined || item.createType == 2;
}

const daAssetList = ref([]);
const isRegister = ref(false);
const titleBtns = [
  {
    id: 1,
    name: "库表",
    icon: "da-database",
  },
  {
    id: 7,
    name: "文件",
    icon: "da-document",
  },
];
const getTypeName = (item) => {
  const btn = titleBtns.find((b) => b.id == item.type);
  return btn ? btn.name : "";
};
const tableColumns = [
  { title: "资产名称", dataIndex: "name", key: "name", width: 180, ellipsis: true },
  { title: "类型", dataIndex: "type", key: "type", width: 110 },
  { title: "状态", dataIndex: "status", key: "status", width: 110 },
  { title: "数据量", dataIndex: "dataCount", key: "dataCount", width: 130 },
  { title: "数据源", dataIndex: "datasourceName", key: "datasourceName", width: 160, ellipsis: true },
  { title: "表名称", dataIndex: "tableName", key: "tableName", width: 160, ellipsis: true },
  { title: "所属目录", dataIndex: "catName", key: "catName", width: 150, ellipsis: true },
  { title: "所属主题", dataIndex: "themeName", key: "themeName", width: 170, ellipsis: true },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 160 },
  { title: "资产描述", dataIndex: "description", key: "description", ellipsis: true },
  { title: "操作", key: "actions", width: 210, fixed: "right" },
];
let themeList = ref([]);
async function getAssetThemeList() {
  const response = await getThemeList();
  themeList.value = response.data;
}

const deptOptions = ref(undefined);
const leftWidth = ref(240); // 初始左侧宽度
const open = ref(false);
const openDetail = ref(false);
const openApply = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const total = ref(0);
const title = ref("");
const titleApply = ref("");
const spaceOptions = ref([]);
const defaultSort = ref({ prop: "create_time", order: "desc" });
const router = useRouter();
const userStore = useUserStore();
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
  url: import.meta.env.VITE_APP_BASE_API + "/ast/daAsset/importData",
});
const data = reactive({
  form: {},
  formApply: {
    spaceCode: null,
    phone: null,
    applyReason: null,
  },
  queryParams: {
    themeIdList: [],
    type: null,
    pageNum: 1,
    pageSize: 6,
    name: null,
    catCode: null,
    themeId: null,
    datasourceId: null,
    tableName: null,
    tableComment: null,
    dataCount: null,
    fieldCount: null,
    status: null,
    description: null,
    createTime: null,
    params: {
      sourceType: [0, 1],
    },
  },
  rules: {
    name: [{ required: true, message: "资产名称不能为空", trigger: "blur" }],
    catCode: [{ required: true, message: "目录编码不能为空", trigger: "blur" }],
    themeId: [{ required: true, message: "主题id不能为空", trigger: "blur" }],
    datasourceId: [
      { required: true, message: "数据连接id不能为空", trigger: "blur" },
    ],
    tableName: [{ required: true, message: "表名称不能为空", trigger: "blur" }],
  },
  rulesApply: {
    spaceCode: [
      { required: true, message: "申请空间不能为空", trigger: "change" },
    ],
    phone: [{ required: true, message: "联系电话不能为空", trigger: "blur" }],
  },
});

const { queryParams, form, formApply, rulesApply } = toRefs(data);

watch(
  () => userStore.spaceCode,
  () => {
    getList();
  },
  { immediate: true } // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
);

function submitApplyForm() {
  proxy.$refs["assetApplyRef"].validate().then(() => {
    formApply.value.id = null;
    formApply.value.updateBy = null;
    formApply.value.updaterId = null;
    formApply.value.updateTime = null;
    formApply.value.validFlag = null;
    formApply.value.delFlag = null;
    formApply.value.status = null;
    addDaAssetApply(formApply.value).then(() => {
      proxy.$modal.msgSuccess("申请成功");
      openApply.value = false;
      getList();
    });
  }).catch(() => {});
}

function handleRefresh(row) {
  const _id = row.id;
  loading.value = true;
  startDaDiscoveryTask({ id: _id })
    .then((res) => {
      if (res.code == 200) {
        proxy.$modal.msgSuccess("更新成功");
        getList();
      } else {
        proxy.$modal.msgWarning("更新失败，请联系管理员");
      }
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleSync(row) {
  const _id = row.id;
  loading.value = true;
  syncAsset({ assetId: _id })
    .then((res) => {
      if (res.code == 200) {
        proxy.$modal.msgSuccess(res.msg || "同步成功");
        getList();
      } else {
        proxy.$modal.msgWarning(res.msg || "同步失败，请联系管理员");
      }
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleApply(row) {
  const _id = row.id || ids.value;
  getDaAsset(_id).then((response) => {
    formApply.value = response.data;
    openApply.value = true;
    titleApply.value = "申请数据资产";
    formApply.value.phone = userStore.phonenumber;
    formApply.value.assetId = _id;
    formApply.value.assetName = response.data.name;
  });
  currentUser().then((response) => {
    spaceOptions.value = response.data;
  });
}

function handleSelectSpace(value) {
  formApply.value.spaceCode = value;
  const space = spaceOptions.value.find((item) => item.code === value);
  formApply.value.spaceId = space.id;
  formApply.value.spaceName = space.name;
}

function handleView(row) {
  if (!unregistered(row)) {
    return proxy.$modal.msgWarning("该资产暂未注册，请注册后重试");
  }
  routeTo(
    '/col/asset/detail',
    row
  )
}

/** 查询数据资产列表 */
function getList() {
  if (!queryParams.value?.orderByColumn) {
    queryParams.value.orderByColumn = defaultSort.value.prop;
    queryParams.value.isAsc = defaultSort.value.order;
  }
  loading.value = true;
  queryParams.value.spaceCode = userStore.spaceCode;
  queryParams.value.spaceId = userStore.spaceId;
  listDppAsset(queryParams.value).then((response) => {
    const page = normalizePage(response);
    total.value = page.total;
    daAssetList.value = pageRows(page.rows, page.total, queryParams.value);
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
    catCode: null,
    themeId: null,
    datasourceId: null,
    tableName: null,
    tableComment: null,
    dataCount: null,
    fieldCount: null,
    status: null,
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
  proxy.resetForm("daAssetRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}
const DeptTreeRef = ref(null);
/** 重置按钮操作 */
function resetQuery() {
  if (DeptTreeRef.value?.resetTree) {
    DeptTreeRef.value.resetTree();
  }
  queryParams.value.params.sourceType = [0, 1];
  queryParams.value.catCode = "";
  queryParams.value.pageNum = 1;
  queryParams.value.type = null;
  reset();
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 查询部门下拉树结构 */
/** 排序触发事件 */
function getAssetCat() {
  listAttAssetCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "资产目录",
        value: "",
        id: 0,
        children: deptOptions.value,
      },
    ];
  });
}
/** 新增按钮操作 */
function handleAdd() {
  isRegister.value = false;
  reset();
  open.value = true;
  title.value = "新增数据资产";
}

/** 修改按钮操作 */
function handleUpdate(row, register) {
  if (register == 'register') {
    isRegister.value = true;
  } else {
    isRegister.value = false;
  }
  reset();
  const _id = row.id || ids.value;
  getDaAsset(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改数据资产";
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  // proxy.$message.error("功能开发中....");
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除数据资产编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delDaAsset(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => { });
}

/** ---------------- 导入相关操作 -----------------**/
/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    "system/user/importTemplate",
    {},
    `daAsset_template_${new Date().getTime()}.xlsx`
  );
}

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
}

function handleNodeClick(data) {
  queryParams.value.catCode = data.code;
  handleQuery();
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
  getList();
};
/** ---------------------------------**/

function routeTo(link, row) {
  if (link !== "" && link.indexOf("http") !== -1) {
    window.location.href = link;
    return;
  }
  if (link !== "") {
    if (link === router.currentRoute.value.path) {
      window.location.reload();
    } else {
      router.push({
        path: link,
        query: {
          id: row.id,
        },
      });
    }
  }
}

/** 启用禁用开关 */
function handleStatusChange(row) {
  const text = row.status === "2" ? "撤销发布" : "发布";
  const status = row.status === "2" ? "1" : "2";
  proxy.$modal
    .confirm("确认要" + text + '"' + row.name + '"资产吗？')
    .then(function () {
      updateDaAsset({ id: row.id, status: status }).then((res) => {
        if (res.code == 200) {
          proxy.$modal.msgSuccess(text + "成功");
          getList();
        }
      });
    });
}
queryParams.value.orderByColumn = defaultSort.value.prop;
queryParams.value.isAsc = defaultSort.value.order;
// getList();
getAssetCat();
getAssetThemeList();
</script>
<style scoped lang="scss">
.pagecont-top {
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  gap: 8px;

  .ant-form {
    display: flex !important;
    flex-wrap: nowrap !important;
    flex: 0 1 auto !important;

    .ant-form-item {
      display: inline-flex !important;
      flex-shrink: 0 !important;
      margin-bottom: 0 !important;
    }
  }

  .data-action-btns {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  .top-right-btn {
    flex-shrink: 0;
  }
}

.butgdlist {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.button-inner {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  /* 图标和文字间距 */
}

.fix-icon {
  //width: 16px; /* 固定图标占位宽度，使图标文字对齐一致 */
  //text-align: center;
  margin-left: -10px;
}

::v-deep {
  .selectlist .ant-tag {
    background: #f3f8ff !important;
    border: 0px solid #6ba7ff !important;
    color: #2666fb !important;
  }
}

.app-container {
  margin: 13px 15px;
}

.ant-layout-content {
  padding: 2px 0px;
  // box-shadow: 1px 1px 3px rgba(0, 0, 0, .2);
}

//上传附件样式调整
::v-deep {

  // .el-upload-list{
  //    display: flex;
  // }
  .ant-upload-list-item {
    width: 100%;
    height: 25px;
  }
}

.pagecont-bottom {
  padding: 0;
  background-color: transparent;
  box-shadow: none;

  .pagination-container {
    height: 60px;
    background: #ffffff;
    border-radius: 2px;
    margin: 15px 0 0;
    padding: 14px 20px !important;

    :deep(.ant-pagination) {
      right: 20px;
    }
  }
}

.asset-name-link {
  color: #2666fb;
  cursor: pointer;

  &:hover {
    color: #4d85ff;
  }
}

.danger-text {
  color: #ff7a00 !important;
}

.success-text {
  color: #039792 !important;
}

.pagecont-bottom .ant-table-wrapper {
  background: #ffffff;
  border-radius: 2px;
  padding: 12px;
}

:deep(.tag-view) {
  &:not(.is-fullscreen) {
    margin-top: 25vh !important;
  }

  .ant-modal-body {
    height: 195px;
  }
}
</style>
