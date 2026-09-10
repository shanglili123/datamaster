<template>
  <div class="app-container">
    <a-form
      :model="store.form"
      :rules="rules"
      ref="formRef"
      :label-col="{ style: { width: '110px' } }"
    >
      <div class="module-head">基础信息</div>
      <div class="module-body infotop column-form">
        <a-form-item label="所属库名" name="dbId">
          <a-select
            v-model:value="store.form.dbId"
            :disabled="!!route.query.id"
            placeholder="请选择所属库名"
            @change="handleMetaDBChange"
          >
            <a-select-option
              v-for="item in store.metaDatabases"
              :key="item.id"
              :value="item.id"
            >
              {{ item.dbName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="来源系统" name="sourceSystemName">
          <a-input
            v-model:value="store.form.sourceSystemName"
            :disabled="!!route.query.id"
            placeholder="自动获取来源系统"
          />
        </a-form-item>
        <a-form-item label="表名称" name="tableName">
          <a-input
            allow-clear
            :disabled="!!route.query.id"
            v-model:value="store.form.tableName"
            placeholder="请输入表名称"
          />
        </a-form-item>

        <a-form-item label="表注释" name="tableComment">
          <a-input
            allow-clear
            :disabled="!!route.query.id"
            v-model:value="store.form.tableComment"
            placeholder="请输入表注释"
          />
        </a-form-item>

        <a-form-item label="所属分层">
          <a-select
            allow-clear
            v-model:value="store.form.belongingLayer"
            disabled
            placeholder="请选择所属分层"
          >
            <a-select-option
              v-for="dict in toValue(dicts.meta_dw_layers)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="所属系统" name="belongingSystem">
          <a-input
            allow-clear
            v-model:value="store.form.belongingSystem"
            disabled
            placeholder="请输入所属系统"
          />
        </a-form-item>

        <!-- <a-form-item label="安全等级" name="safetyLevelId">
          <a-select
            allow-clear
            v-model:value="store.form.safetyLevelId"
            placeholder="请选择安全等级"
          >
            <a-select-option
              v-for="item in store.sensitiveLevels"
              :key="item.id"
              :label="item.sensitiveLevel"
              :value="item.id"
            />
          </a-select>
        </a-form-item> -->

        <!-- <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="store.form.status">
            <a-radio
              v-for="dict in toValue(dicts.meta_task_status)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item> -->

        <a-form-item label="备注" class="row-full">
          <a-textarea
            v-model:value="store.form.remark"
            placeholder="请输入备注"
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
            show-count
          />
        </a-form-item>
      </div>

      <div class="module-head">技术信息</div>
      <div class="module-body infotop column-form">
        <a-form-item label="数据连接名称" name="datasourceId">
          <a-select
            allow-clear
            v-model:value="store.form.datasourceId"
            placeholder="请选择数据连接名称"
            @change="handleDatasourceChange"
            disabled
          >
            <a-select-option
              v-for="item in store.datasources"
              :key="item.id"
              :value="item.id"
            >
              {{ item.datasourceName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="数据库类型" name="dbType">
          <a-input
            allow-clear
            v-model:value="store.form.dbType"
            disabled
            placeholder="请输入数据库类型"
          />
        </a-form-item>

        <a-form-item label="IP" name="ip">
          <a-input
            allow-clear
            v-model:value="store.form.ip"
            disabled
            placeholder="请输入ip"
          />
        </a-form-item>

        <a-form-item label="端口号" name="port">
          <a-input
            allow-clear
            v-model:value="store.form.port"
            disabled
            placeholder="请输入端口号"
          />
        </a-form-item>

        <a-form-item label="账号" name="username">
          <a-input
            allow-clear
            v-model:value="store.form.username"
            disabled
            placeholder="请输入账号"
          />
        </a-form-item>

        <a-form-item label="存储类型" name="storageType">
          <a-input
            allow-clear
            v-model:value="store.form.storageType"
            placeholder="请输入存储类型"
          />
        </a-form-item>

        <!-- <a-form-item label="存储大小" name="storageSize">
          <a-input-number
            :min="0"
            v-model:value="store.form.storageSize"
            placeholder="请输入存储大小"
            :controls="false"
            class="number-input"
          />
        </a-form-item> -->

      </div>

      <div class="module-head">业务信息</div>
      <div class="module-body infotop column-form">
        <a-form-item label="是否主表" name="masterFlag">
          <a-radio-group v-model:value="store.form.masterFlag">
            <a-radio
              v-for="dict in toValue(dicts.table_yes_no)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="是否临时表" name="tempFlag">
          <a-radio-group v-model:value="store.form.tempFlag">
            <a-radio
              v-for="dict in toValue(dicts.table_yes_no)"
              :key="dict.value"
              :value="dict.value"
            >
              {{ dict.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="描述" class="row-full">
          <a-textarea
            v-model:value="store.form.description"
            placeholder="请输入描述"
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <!-- <a-form-item
          label="变更说明"
          name="updateMsg"
          class="row-full"
          v-if="store.form.id"
        >
          <a-textarea
            v-model:value="store.form.updateMsg"
            placeholder="请输入变更说明"
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
          />
        </a-form-item> -->
      </div>
    </a-form>

    <div class="button-style">
      <a-button @click="handleDraftClick"> 暂存 </a-button>
      <a-button
        type="primary"
        class="fh_btn"
        @mousedown="(e) => e.preventDefault()"
        @click="router.back"
      >
        <svg-icon iconClass="fhs" />返回列表
      </a-button>
      <a-button type="primary" @click="handleConfirmClick">
        确认并退出
      </a-button>
    </div>
  </div>
</template>

<script setup name="TableHandle">
import { reactive, getCurrentInstance, toValue } from "vue";
import { listDb, getDb } from "@/api/cat/catalog/db";
import { listDaDatasource } from "@/api/cat/dataSource/dataSource";
import { listDgSensitiveLevel } from "@/api/cat/compliance/sensitiveLevel";
import {
  getTable,
  updateTable,
  addTable,
  draftTable,
} from "@/api/cat/catalog/table";
import { useRoute, useRouter } from "vue-router";

const DEFAULT_FORM = {
  status: "0",
  masterFlag: "1",
  tempFlag: "0",
};

const rules = {
  dbId: [
    { required: true, message: "请选择所属库名", trigger: ["blur", "change"] },
  ],
  sourceSystemName: [
    { required: true, message: "来源系统不能为空", trigger: "change" },
  ],
  datasourceId: [
    { required: true, message: "请选择数据连接名称", trigger: "change" },
  ],
  tableName: [
    { required: true, message: "请输入表名称", trigger: ["blur", "change"] },
  ],
  dbType: [
    {
      required: true,
      message: "请输入数据库类型",
      trigger: ["blur", "change"],
    },
  ],
  ip: [
    { required: true, message: "请输入数据库ip", trigger: ["blur", "change"] },
  ],
  port: [
    {
      required: true,
      message: "请输入数据库端口",
      trigger: ["blur", "change"],
    },
  ],
  username: [
    {
      required: true,
      message: "请输入数据库用户名",
      trigger: ["blur", "change"],
    },
  ],
  updateMsg: [
    { required: true, message: "请输入变更说明", trigger: ["change", "blur"] },
  ],
};

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "meta_task_status",
  "meta_dw_layers",
  "sys_yes_no",
  "table_yes_no"
);

const router = useRouter();
const route = useRoute();
const formRef = ref();
const store = reactive({
  form: { ...DEFAULT_FORM },
  metaDatabases: [],
  sensitiveLevels: [],
  loading: false,
});

// 获取库元素列表
function getMetaDatabases() {
  return listDb({ pageSize: 1000 }).then((res) => {
    store.metaDatabases = res.data.rows;
    if (route.query.dbId) {
      store.form.dbId = route.query.dbId - 0;
      handleMetaDBChange(route.query.dbId - 0);
    }
    return res;
  });
}

// 获取安全等级
function getSensitiveLevel() {
  listDgSensitiveLevel({ pageSize: 1000 }).then((res) => {
    store.sensitiveLevels = res.data.rows;
  });
}

// 获取数据源列表
function getDatasources() {
  return listDaDatasource().then((res) => {
    res.data.rows.forEach((item) => {
      item.datasourceConfig = item.datasourceConfig
        ? JSON.parse(item.datasourceConfig)
        : {};
    });
    store.datasources = res.data.rows;
    return res.data.rows;
  });
}

// 切换数据源
function handleDatasourceChange(id) {
  const data = store.datasources?.find((item) => item.id === id);
  store.form.ip = data.ip;
  store.form.port = data.port;
  store.form.username = data.datasourceConfig?.username;
  store.form.dbType = data.datasourceType;
}

// 切换库元数据
function handleMetaDBChange(id) {
  getDb(id).then((res) => {
    store.form.domainId = res.data.domainId;
    store.form.sourceSystemName = res.data.sourceSystemName;
    store.form.dbName = res.data.dbName;
    store.form.datasourceId = res.data.datasourceId;
    store.form.belongingLayer = res.data.belongingLayer;
    store.form.belongingSystem = res.data.belongingSystem;
    handleDatasourceChange(res.data.datasourceId);
  });
}

// 确认新增/修改
async function handleConfirmClick() {
  store.loading = true;
  const valid = await formRef.value.validate();
  store.loading = false;
  if (!valid) return;
  store.loading = true;
  const func = route.query.id ? updateTable : addTable;
  if (store.form.safetyLevelId == undefined) {
    store.form.safetyLevelId = null;
    store.form.safetyLevelName = null;
  }
  await func(store.form);
  store.loading = false;
  proxy.$modal.msgSuccess(`${route.query.id ? "修改" : "新增"}表元数据成功！`);
  router.back();
}

async function handleDraftClick() {
  store.loading = true;
  await draftTable(store.form);
  store.loading = false;
  proxy.$modal.msgSuccess(`暂存表元数据成功！`);
}

// 获取详情
async function getDetail() {
  if (!route.query.id) return;
  getTable(route.query.id).then((res) => {
    const {
      createBy,
      createTime,
      delFlag,
      updateBy,
      updateTime,
      updaterId,
      auditTime,
      auditStatus,
      ...form
    } = res.data;
    store.form = form;
    handleMetaDBChange(res.data.dbId);
  });
}

getMetaDatabases();
// getSensitiveLevel();
getDatasources();
getDetail();
</script>

<style lang="scss" scoped>
.app-container {
  background-color: #fff;
  padding: 15px 30px 30px;

  .module-body {
    margin-bottom: 40px;
  }

  .button-style {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
