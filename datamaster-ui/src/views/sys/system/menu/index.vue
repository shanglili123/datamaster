<template>
   <div class="app-container" ref="app-container">
      <div class="pagecont-top" v-show="showSearch">
         <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '68px' } }">
            <a-form-item label="菜单名称" name="menuName">
               <a-input
                  v-model:value="queryParams.menuName"
                  placeholder="请输入菜单名称"
                  allow-clear
                  class="el-form-input-width"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="状态" name="status">
               <a-select v-model:value="queryParams.status" placeholder="菜单状态" allow-clear class="el-form-input-width">
                  <a-select-option
                     v-for="dict in sys_normal_disable"
                     :key="dict.value"
                     :value="dict.value">{{ dict.label }}</a-select-option>
               </a-select>
            </a-form-item>
            <a-form-item>
               <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
                  <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
               </a-button>
               <a-button @click="resetQuery" @mousedown="e => e.preventDefault()">
                  <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
               </a-button>
            </a-form-item>
         </a-form>
         <div class="data-action-btns">
            <a-button type="primary" :icon="h(PlusOutlined)" @click="handleAdd" v-hasPermi="['system:menu:add']">新增</a-button>
            <a-button :icon="h(SwitcherOutlined)" @click="toggleExpandAll">展开/折叠</a-button>
         </div>
         <div class="top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
         </div>
      </div>
      <div  class="pagecont-bottom">

         <a-spin :spinning="loading">
            <a-table
               v-if="refreshTable"
               :data-source="menuList"
               :columns="tableColumns"
               :pagination="false"
               :scroll="{ y: '60vh' }"
               row-key="menuId"
               :default-expand-all-rows="isExpandAll"
               :children-column-name="'children'"
               :locale="{ emptyText: emptyContent }"
            >
               <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'icon'">
                     <svg-icon :icon-class="record.icon" />
                  </template>
                  <template v-else-if="column.dataIndex === 'perms'">
                     <span>{{ record.perms || '-' }}</span>
                  </template>
                  <template v-else-if="column.dataIndex === 'component'">
                     <span>{{ record.component || '-' }}</span>
                  </template>
                  <template v-else-if="column.dataIndex === 'status'">
                     <dict-tag :options="sys_normal_disable" :value="record.status" />
                  </template>
                  <template v-else-if="column.dataIndex === 'createTime'">
                     <span>{{ parseTime(record.createTime) }}</span>
                  </template>
                  <template v-else-if="column.key === 'actions'">
                     <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['system:menu:edit']">修改</a-button>
                     <a-button type="link" size="small" @click="handleAdd(record)" v-hasPermi="['system:menu:add']">新增</a-button>
                     <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['system:menu:remove']">删除</a-button>
                  </template>
                  <template v-else>
                     <span>{{ record[column.dataIndex] || '-' }}</span>
                  </template>
               </template>
            </a-table>
         </a-spin>
         <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>

      <!-- 添加或修改菜单对话框 -->
      <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
         <a-form ref="menuRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }">
            <a-row :gutter="20">
               <a-col :span="24">
                  <a-form-item label="上级菜单">
                     <a-tree-select
                        v-model:value="form.parentId"
                        :tree-data="menuOptions"
                        :field-names="{ value: 'menuId', label: 'menuName', children: 'children' }"
                        placeholder="选择上级菜单"
                     />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="菜单类型" name="menuType">
                     <a-radio-group v-model:value="form.menuType">
                        <a-radio :value="'M'">目录</a-radio>
                        <a-radio :value="'C'">菜单</a-radio>
                        <a-radio :value="'F'">按钮</a-radio>
                     </a-radio-group>
                  </a-form-item>
               </a-col>
               <a-col :span="12" v-if="form.menuType != 'F'">
                  <a-form-item label="菜单图标" name="icon">
                     <a-popover
                        placement="bottom-start"
                        :overlay-style="{ width: '540px' }"
                        trigger="click"
                     >
                        <template #content>
                           <icon-select ref="iconSelectRef" @selected="selected" :active-icon="form.icon" />
                        </template>
                        <a-input v-model:value="form.icon" placeholder="点击选择图标" @blur="showSelectIcon" readonly>
                           <template #prefix>
                              <svg-icon
                                 v-if="form.icon"
                                 :icon-class="form.icon"
                                 class="el-input__icon"
                                 style="height: 32px;width: 16px;"
                              />
                              <SearchOutlined v-else style="height: 32px;width: 16px;" />
                           </template>
                        </a-input>
                     </a-popover>
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="显示排序" name="orderNum">
                     <a-input-number style="width:100%" v-model:value="form.orderNum" :min="0" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="菜单名称" name="menuName">
                     <a-input v-model:value="form.menuName" placeholder="请输入菜单名称" />
                  </a-form-item>
               </a-col>
               <a-col :span="12" v-if="form.menuType == 'C'">
                  <a-form-item name="routeName">
                     <template #label>
                        <span>
                           <a-tooltip title="默认不填则和路由地址相同：如地址为：`user`，则名称为`User`（注意：因为router会删除名称相同路由，为避免名字的冲突，特殊情况下请自定义，保证唯一性）" placement="top">
                              <InfoCircleOutlined style="color: #909399;" />
                           </a-tooltip>
                           路由名称
                        </span>
                     </template>
                     <a-input v-model:value="form.routeName" placeholder="请输入路由名称" />
                  </a-form-item>
               </a-col>
               <a-col :span="12" v-if="form.menuType != 'F'">
                  <a-form-item name="path">
                     <template #label>
                        <span>
                           <a-tooltip title="访问的路由地址，如：`user`，如外网地址需内链访问则以`http(s)://`开头" placement="top">
                              <InfoCircleOutlined style="color: #909399;" />
                           </a-tooltip>
                           路由地址
                        </span>
                     </template>
                     <a-input v-model:value="form.path" placeholder="请输入路由地址" />
                  </a-form-item>
               </a-col>
               <a-col :span="12" v-if="form.menuType != 'F'">
                  <a-form-item>
                     <template #label>
                        <span>
                           <a-tooltip title="选择是外链则路由地址需要以`http(s)://`开头" placement="top">
                              <InfoCircleOutlined style="color: #909399;" />
                           </a-tooltip>
                           是否外链
                        </span>
                     </template>
                     <a-radio-group v-model:value="form.isFrame">
                        <a-radio :value="'0'">是</a-radio>
                        <a-radio :value="'1'">否</a-radio>
                     </a-radio-group>
                  </a-form-item>
               </a-col>

               <a-col :span="12" v-if="form.menuType == 'C'">
                  <a-form-item name="component">
                     <template #label>
                        <span>
                           <a-tooltip title="访问的组件路径，如：`system/user/index`，默认在`views`目录下" placement="top">
                              <InfoCircleOutlined style="color: #909399;" />
                           </a-tooltip>
                           组件路径
                        </span>
                     </template>
                     <a-input v-model:value="form.component" placeholder="请输入组件路径" />
                  </a-form-item>
               </a-col>
               <a-col :span="12" v-if="form.menuType != 'M'">
                  <a-form-item>
                     <a-input v-model:value="form.perms" placeholder="请输入权限标识" :maxlength="100" />
                     <template #label>
                        <span>
                           <a-tooltip title="控制器中定义的权限字符，如：@PreAuthorize(`@ss.hasPermi('system:user:list')`)" placement="top">
                              <InfoCircleOutlined style="color: #909399;" />
                           </a-tooltip>
                           权限字符
                        </span>
                     </template>
                  </a-form-item>
               </a-col>
               <a-col :span="12" v-if="form.menuType == 'C'">
                  <a-form-item>
                     <a-input v-model:value="form.query" placeholder="请输入路由参数" :maxlength="255" />
                     <template #label>
                        <span>
                           <a-tooltip title='访问路由的默认传递参数，如：`{"id": 1, "name": "ry"}`' placement="top">
                              <InfoCircleOutlined style="color: #909399;" />
                           </a-tooltip>
                           路由参数
                        </span>
                     </template>
                  </a-form-item>
               </a-col>
               <a-col :span="12" v-if="form.menuType == 'C'">
                  <a-form-item>
                     <template #label>
                        <span>
                           <a-tooltip title="选择是则会被`keep-alive`缓存，需要匹配组件的`name`和地址保持一致" placement="top">
                              <InfoCircleOutlined style="color: #909399;" />
                           </a-tooltip>
                           是否缓存
                        </span>
                     </template>
                     <a-radio-group v-model:value="form.isCache">
                        <a-radio :value="'0'">缓存</a-radio>
                        <a-radio :value="'1'">不缓存</a-radio>
                     </a-radio-group>
                  </a-form-item>
               </a-col>
               <a-col :span="12" v-if="form.menuType != 'F'">
                  <a-form-item>
                     <template #label>
                        <span>
                           <a-tooltip title="选择隐藏则路由将不会出现在侧边栏，但仍然可以访问" placement="top">
                              <InfoCircleOutlined style="color: #909399;" />
                           </a-tooltip>
                           显示状态
                        </span>
                     </template>
                     <a-radio-group v-model:value="form.visible">
                        <a-radio
                           v-for="dict in sys_show_hide"
                           :key="dict.value"
                           :value="dict.value"
                        >{{ dict.label }}</a-radio>
                     </a-radio-group>
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item>
                     <template #label>
                        <span>
                           <a-tooltip title="选择停用则路由将不会出现在侧边栏，也不能被访问" placement="top">
                              <InfoCircleOutlined style="color: #909399;" />
                           </a-tooltip>
                           菜单状态
                        </span>
                     </template>
                     <a-radio-group v-model:value="form.status">
                        <a-radio
                           v-for="dict in sys_normal_disable"
                           :key="dict.value"
                           :value="dict.value"
                        >{{ dict.label }}</a-radio>
                     </a-radio-group>
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

<script setup name="Menu">

import { addMenu, delMenu, getMenu, listMenu, updateMenu } from "@/api/system/system/menu.js";

import SvgIcon from "@/components/SvgIcon/index.vue";

import IconSelect from "@/components/IconSelect/index.vue";

import { h } from 'vue';
import { InfoCircleOutlined, PlusOutlined, SearchOutlined, SwitcherOutlined } from "@ant-design/icons-vue";

const tableColumns = [
  { title: '菜单名称', dataIndex: 'menuName', align: 'left', width: 160, ellipsis: true },
  { title: '图标', dataIndex: 'icon', align: 'center', width: 100 },
  { title: '排序', dataIndex: 'orderNum', align: 'center', width: 60 },
  { title: '权限标识', dataIndex: 'perms', align: 'center', ellipsis: true },
  { title: '组件路径', dataIndex: 'component', align: 'center', ellipsis: true },
  { title: '状态', dataIndex: 'status', align: 'center', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 160 },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const { proxy } = getCurrentInstance();
const { sys_show_hide, sys_normal_disable } = proxy.useDict("sys_show_hide", "sys_normal_disable");

const menuList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const menuOptions = ref([]);
const isExpandAll = ref(false);
const refreshTable = ref(true);
const iconSelectRef = ref(null);
const total = ref(0);

const data = reactive({
  form: {},
  queryParams: {
    menuName: undefined,
    visible: undefined,
    pageNum: 1,
    pageSize: 6
  },
  rules: {
    menuName: [{ required: true, message: "菜单名称不能为空", trigger: "blur" }],
    orderNum: [{ required: true, message: "菜单顺序不能为空", trigger: "blur" }],
    path: [{ required: true, message: "路由地址不能为空", trigger: "blur" }]
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询菜单列表 */
function getList() {
  loading.value = true;
  listMenu(queryParams.value).then(response => {
    const treeData = proxy.handleTree(response.data || [], "menuId");
    total.value = treeData.length;
    menuList.value = paginateTreeRoots(treeData);
    loading.value = false;
  });
}

function paginateTreeRoots(treeData) {
  const pageNum = queryParams.value.pageNum || 1;
  const pageSize = queryParams.value.pageSize || 6;
  const start = (pageNum - 1) * pageSize;
  return treeData.slice(start, start + pageSize);
}

/** 查询菜单下拉树结构 */
function getTreeselect() {
  menuOptions.value = [];
  listMenu().then(response => {
    const menu = { menuId: 0, menuName: "主目录", children: [] };
    menu.children = proxy.handleTree(response.data, "menuId");
    menuOptions.value.push(menu);
  });
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 表单重置 */
function reset() {
  form.value = {
    menuId: undefined,
    parentId: 0,
    menuName: undefined,
    icon: undefined,
    menuType: "M",
    orderNum: 0,
    isFrame: "1",
    isCache: "0",
    visible: "0",
    status: "0"
  };
  proxy.resetForm("menuRef");
}

/** 展示下拉图标 */
function showSelectIcon() {
  iconSelectRef.value.reset();
}

/** 选择图标 */
function selected(name) {
  form.value.icon = name;
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

/** 新增按钮操作 */
function handleAdd(row) {
  reset();
  getTreeselect();
  if (row != null && row.menuId) {
    form.value.parentId = row.menuId;
  } else {
    form.value.parentId = 0;
  }
  open.value = true;
  title.value = "新增菜单";
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}

/** 修改按钮操作 */
async function handleUpdate(row) {
  reset();
  await getTreeselect();
  getMenu(row.menuId).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改菜单";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["menuRef"].validate().then(() => {
    if (form.value.menuId != undefined) {
      updateMenu(form.value).then(response => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addMenu(form.value).then(response => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除名称为"' + row.menuName + '"的数据项?').then(function() {
    return delMenu(row.menuId);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>
