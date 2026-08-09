<template>
  <a-form ref="genInfoForm" :model="info" :rules="rules" :label-col="{ style: { width: '150px' } }">
    <a-row>
      <a-col :span="12">
        <a-form-item name="tplCategory" label="生成模板">
          <a-select v-model:value="info.tplCategory" @change="tplSelectChange">
            <a-select-option value="crud">单表（增删改查）</a-select-option>
            <a-select-option value="tree">树表（增删改查）</a-select-option>
            <a-select-option value="sub">主子表（增删改查）</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item name="tplWebType" label="前端类型">
          <a-select v-model:value="info.tplWebType">
            <a-select-option value="element-ui">Vue2 Element UI 模版</a-select-option>
            <a-select-option value="element-plus">Vue3 Element Plus 模版</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item name="packageName">
          <template #label>
            生成包路径
            <a-tooltip title="生成在哪个java包下，例如 tech.qiantong.system" placement="top">
              <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
            </a-tooltip>
          </template>
          <a-input v-model:value="info.packageName" />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item name="moduleName">
          <template #label>
            二级模块名称
            <a-tooltip title="二级模块名称，例如 dept (system 大模块下的 部门子模块)" placement="top">
              <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
            </a-tooltip>
          </template>
          <a-input v-model:value="info.moduleName" />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item name="businessName">
          <template #label>
            生成业务名
            <a-tooltip title="可理解为功能英文名，例如 user" placement="top">
              <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
            </a-tooltip>
          </template>
          <a-input v-model:value="info.businessName" />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item name="functionName">
          <template #label>
            生成功能名
            <a-tooltip title="用作类描述，例如 用户" placement="top">
              <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
            </a-tooltip>
          </template>
          <a-input v-model:value="info.functionName" />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item name="genType">
          <template #label>
            生成代码方式
            <a-tooltip title="默认为zip压缩包下载，也可以自定义生成路径" placement="top">
              <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
            </a-tooltip>
          </template>
          <a-radio-group v-model:value="info.genType">
            <a-radio value="0">zip压缩包</a-radio>
            <a-radio value="1">自定义路径</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item>
          <template #label>
            上级菜单
            <a-tooltip title="分配到指定菜单下，例如 系统管理" placement="top">
              <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
            </a-tooltip>
          </template>
          <tree-select
           style="width:100%"
            v-model:value="info.parentMenuId"
            :options="menuOptions"
            :objMap="{ value: 'menuId', label: 'menuName', children: 'children' }"
            placeholder="请选择系统菜单"
          />
        </a-form-item>
      </a-col>

      <a-col :span="24" v-if="info.genType == '1'">
        <a-form-item name="genPath">
          <template #label>
            自定义路径
            <a-tooltip title="填写磁盘绝对路径，若不填写，则生成到当前Web空间下" placement="top">
              <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
            </a-tooltip>
          </template>
          <a-input v-model:value="info.genPath">
            <template #addonAfter>
              <a-dropdown>
                <a-button type="primary" size="small">
                  最近路径快速选择
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="info.genPath = '/'">恢复默认的生成基础路径</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
          </a-input>
        </a-form-item>
      </a-col>
    </a-row>

    <template v-if="info.tplCategory == 'tree'">
      <h4 class="form-header">其他信息</h4>
      <a-row v-show="info.tplCategory == 'tree'">
        <a-col :span="12">
          <a-form-item>
            <template #label>
              树编码字段
              <a-tooltip title="树显示的编码字段名， 如：dept_id" placement="top">
                <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
              </a-tooltip>
            </template>
            <a-select v-model:value="info.treeCode" placeholder="请选择">
              <a-select-option
                v-for="(column, index) in info.columns"
                :key="index"
                :value="column.columnName"
              >{{ column.columnName + '：' + column.columnComment }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item>
            <template #label>
              树父编码字段
              <a-tooltip title="树显示的父编码字段名， 如：parent_Id" placement="top">
                <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
              </a-tooltip>
            </template>
            <a-select v-model:value="info.treeParentCode" placeholder="请选择">
              <a-select-option
                v-for="(column, index) in info.columns"
                :key="index"
                :value="column.columnName"
              >{{ column.columnName + '：' + column.columnComment }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item>
            <template #label>
              树名称字段
              <a-tooltip title="树节点的显示名称字段名， 如：dept_name" placement="top">
                <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
              </a-tooltip>
            </template>
            <a-select v-model:value="info.treeName" placeholder="请选择">
              <a-select-option
                v-for="(column, index) in info.columns"
                :key="index"
                :value="column.columnName"
              >{{ column.columnName + '：' + column.columnComment }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
    </template>

    <template v-if="info.tplCategory == 'sub'">
      <h4 class="form-header">关联信息</h4>
      <a-row>
        <a-col :span="12">
          <a-form-item>
            <template #label>
              关联子表的表名
              <a-tooltip title="关联子表的表名， 如：sys_user" placement="top">
                <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
              </a-tooltip>
            </template>
            <a-select v-model:value="info.subTableName" placeholder="请选择" @change="subSelectChange">
              <a-select-option
                v-for="(table, index) in tables"
                :key="index"
                :value="table.tableName"
              >{{ table.tableName + '：' + table.tableComment }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item>
            <template #label>
              子表关联的外键名
              <a-tooltip title="子表关联的外键名， 如：user_id" placement="top">
                <InfoCircleOutlined style="color: #909399; margin-left: 4px" />
              </a-tooltip>
            </template>
            <a-select v-model:value="info.subTableFkName" placeholder="请选择">
              <a-select-option
                v-for="(column, index) in subColumns"
                :key="index"
                :value="column.columnName"
              >{{ column.columnName + '：' + column.columnComment }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
    </template>

  </a-form>
</template>

<script setup>
import { listMenu } from "@/api/system/system/menu.js";
import { InfoCircleOutlined, DownOutlined } from "@ant-design/icons-vue";

const subColumns = ref([]);
const menuOptions = ref([]);
const { proxy } = getCurrentInstance();

const props = defineProps({
  info: {
    type: Object,
    default: null
  },
  tables: {
    type: Array,
    default: null
  }
});

// 表单校验
const rules = ref({
  tplCategory: [{ required: true, message: "请选择生成模板", trigger: "blur" }],
  packageName: [{ required: true, message: "请输入生成包路径", trigger: "blur" }],
  moduleName: [{ required: true, message: "请输入生成模块名", trigger: "blur" }],
  businessName: [{ required: true, message: "请输入生成业务名", trigger: "blur" }],
  functionName: [{ required: true, message: "请输入生成功能名", trigger: "blur" }]
});

function subSelectChange(value) {
  props.info.subTableFkName = "";
}

function tplSelectChange(value) {
  if (value !== "sub") {
    props.info.subTableName = "";
    props.info.subTableFkName = "";
  }
}

function setSubTableColumns(value) {
  for (var item in props.tables) {
    const name = props.tables[item].tableName;
    if (value === name) {
      subColumns.value = props.tables[item].columns;
      break;
    }
  }
}

/** 查询菜单下拉树结构 */
function getMenuTreeselect() {
  listMenu().then(response => {
    menuOptions.value = proxy.handleTree(response.data, "menuId");
  });
}

watch(() => props.info.subTableName, val => {
  setSubTableColumns(val);
});

watch(() => props.info.tplWebType, val => {
  if (val === '') {
    props.info.tplWebType = "element-plus";
  }
});

getMenuTreeselect();
</script>
