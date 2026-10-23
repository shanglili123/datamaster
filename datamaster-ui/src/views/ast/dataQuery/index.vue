<template>
  <div class="app-container" ref="app-container">

    <div class="head-container">
      <div class="head-title">
        {{ nodeData.name !== "" ? nodeData.name : "数据查询" }}
      </div>
      <div class="head-btns">
        <div class="head-select">
          <!-- 标题后附加下拉选择框 -->
          <a-select :value="queryParams.id" @change="selectDatasource" placeholder="请选择数据源" class="head-select-el" style="width: 300px;">
            <a-select-option v-for="item in TablesByDataSource" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </div>
        <a-button ghost type="primary" size="small" @click="handleQuery" :loading="loadings"
          :disabled="targetPending || Boolean(targetError)">
          <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
        </a-button>
        <a-button type="primary" size="small" @click="handleClear">清除</a-button>
      </div>
    </div>
    <a-alert v-if="targetPending || targetError" show-icon style="margin-bottom: 12px;"
      :type="targetPending ? 'info' : 'warning'"
      :message="targetPending ? '正在校验查询目标，请稍候' : targetError" />
    <a-layout style="90%">
      <!-- 树：数据源->表->字段 -->
      <DeptTree :deptOptions="TablesByDataSource" :leftWidth="leftWidth" :placeholder="'请输入数据源名称'" ref="DeptTreeRef"
        @node-click="handleTreeNodeClick" @nodeload-click="loadTreeData" :loading="loading" />
      <a-layout-content style="padding: 0;">
        <div class="pagecont-bottom" style="padding: 0;">
          <!-- SQL 编辑器 -->
          <Editor ref="editorRef" :model-value="queryParams.sqlText" @update:model-value="handleChange"
            @ready="(editor) => editor.setValue(queryParams.sqlText)" @query="handleQuery" />
        </div>
      </a-layout-content>
    </a-layout>
    <TableInfoDialog :visible="dialogVisible" title="查询结果" @update:visible="dialogVisible = $event"
      :queryParams="queryParams" :spl="spl" />
  </div>
</template>

<script setup name="DataQuery">
import { ref, watch, onActivated, onDeactivated, onBeforeUnmount } from "vue";
import { useRoute } from "vue-router";
import { Modal, message } from "ant-design-vue";
import { getDaAsset } from "@/api/ast/asset/asset";
import { checkPermi } from "@/utils/permission";
import { normalizeDataQueryTarget, isQueryableAsset } from "@/utils/dataQueryNavigation";
import { buildSelectSql, quoteSqlIdentifier } from "@/utils/sqlIdentifiers";
import Editor from "@/components/SqlEditor/editor/index1.vue";
import TableInfoDialog from ".//components/queryResult.vue";
import DeptTree from "@/components/DeptTree/lazy";
import {
  getColumnByAssetId,
} from '@/api/col/task/index.js';
import {
  listQueryDatasource,
} from '@/api/ast/dataSource/dataSource';
import {
  tableList,
} from "@/api/std/model/model";
import { executeSqlQuery } from "@/api/ast/dataSource/dataSource";
import { encrypt } from "@/utils/aesEncrypt";
const leftWidth = ref(240);
const loading = ref(false);
const dialogVisible = ref(false);
const targetPending = ref(false);
const targetError = ref("");

const queryParams = ref({
  pageNum: 1,
  pageSize: 6,
  sqlText: "",
  id: "", // 选中数据源 id，用于查询
  datasourceType: '',
});
let spl = ref('')


const TablesByDataSource = ref([]); // 树顶级为数据源节点

const editorRef = ref(null);
const nodeData = ref({ name: "", taskConfig: {} });

const route = useRoute();
const mountedQueryPath = route.path;
let targetSequence = 0;
let appliedTargetKey = "";
let appliedSql = "";
let appliedDatasourceId = "";
let active = true;
let targetConfirmation = null;

// Dropdown, tree and route all use the authorized list as the source of id AND type.
function selectDatasource(id, options = {}) {
  const datasource = TablesByDataSource.value.find((item) => String(item.id) === String(id));
  if (!datasource) return null;
  if (!options.fromTarget) {
    invalidateTarget();
    targetPending.value = false;
    targetError.value = "";
    dialogVisible.value = false;
    nodeData.value.name = "";
  }
  queryParams.value.id = datasource.id;
  queryParams.value.datasourceType = datasource.datasourceType;
  return datasource;
}

function invalidateTarget() {
  targetSequence += 1;
  targetConfirmation?.destroy();
  targetConfirmation = null;
}

// 1. 获取数据源列表，构造树根节点（数据源）
const getDatasourcesTree = async () => {
  loading.value = true;
  try {
    const res = await listQueryDatasource({
      pageSize: 9999,
      datasourceType: "DM8,Oracle11,MySql,Oracle,Kingbase8,Hive,Doris,SQL_Server,SQL_Server2008,PostgreSQL",
    });
    TablesByDataSource.value = (res.data?.rows || []).map((ds) => ({
      id: ds.id,
      key: `datasource:${ds.id}`,
      datasourceId: ds.id,
      name: ds.datasourceName,
      isLeaf: false,
      children: [],
      datasourceType: ds.datasourceType,
      level: 1,
    }));
    return true;
  } catch (error) {
    message.warning("无法加载可查询的数据连接，请稍后重试");
    return false;
  } finally {
    loading.value = false;
  }
};
const datasourcesReady = getDatasourcesTree();

function confirmTargetReplacement() {
  return new Promise((resolve) => {
    targetConfirmation = Modal.confirm({
      title: "替换当前 SQL？",
      content: "当前查询已有编辑内容，切换查询目标将替换 SQL 和数据连接。",
      okText: "替换",
      cancelText: "保留编辑",
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
      afterClose: () => resolve(false),
    });
  });
}

function failRouteTarget(reason) {
  targetPending.value = false;
  targetError.value = `${reason}；SQL 已保留，请手动选择数据连接后再查询`;
  dialogVisible.value = false;
  queryParams.value.id = "";
  queryParams.value.datasourceType = "";
  nodeData.value.name = "";
  appliedTargetKey = "";
}

async function applyRouteTarget() {
  const wasPending = targetPending.value;
  invalidateTarget();
  const sequence = targetSequence;
  if (!active || !(route.path === mountedQueryPath || /\/dataQuery(?:\/index)?\/?$/i.test(route.path))) return;
  const hasExplicitTarget = ['assetId', 'datasourceId', 'tableName']
    .some((key) => Object.prototype.hasOwnProperty.call(route.query, key));
  if (!hasExplicitTarget) {
    if (wasPending) failRouteTarget("查询目标已取消");
    return;
  }
  // Close old results and block both the button and editor shortcut before any await.
  targetPending.value = true;
  targetError.value = "";
  dialogVisible.value = false;
  const target = normalizeDataQueryTarget(route.query);
  if (!target.assetId && !(target.datasourceId && target.tableName)) {
    failRouteTarget("查询目标缺少有效的数据连接或表名称");
    return;
  }
  if (!checkPermi(["ast:dataQuery:list"])) {
    failRouteTarget("暂无数据查询权限");
    return;
  }
  const current = () => active && sequence === targetSequence;
  const ready = await datasourcesReady;
  if (!current()) return;
  if (!ready) {
    failRouteTarget("无法校验可查询的数据连接");
    return;
  }
  try {
    // Old assetId-only links use the existing detail API only with its own permission.
    // Complete snapshots need no extra asset-detail permission or request.
    if (!(target.datasourceId && target.tableName)) {
      if (!checkPermi(["ast:asset:query"])) {
        failRouteTarget("无法解析旧资产链接：暂无资产详情权限");
        return;
      }
      const response = await getDaAsset(target.assetId);
      if (!current()) return;
      if (!isQueryableAsset(response.data)) {
        failRouteTarget("该资产没有有效的库表查询目标");
        return;
      }
      target.datasourceId = String(response.data.datasourceId);
      target.tableName = response.data.tableName;
    }
    const datasource = TablesByDataSource.value.find((item) => String(item.id) === target.datasourceId);
    if (!datasource) {
      failRouteTarget("目标数据连接不可查询或当前用户无权访问");
      return;
    }
    const key = JSON.stringify([target.datasourceId, target.tableName]);
    if (key === appliedTargetKey) {
      targetPending.value = false;
      return; // Keep-alive/repeated links never replace edits.
    }
    const sqlBefore = queryParams.value.sqlText;
    const idBefore = queryParams.value.id;
    const hasEdits = appliedTargetKey
      ? sqlBefore !== appliedSql || String(idBefore) !== appliedDatasourceId
      : Boolean(sqlBefore.trim());
    if (hasEdits) {
      const confirmed = await confirmTargetReplacement();
      if (!current()) return;
      if (!confirmed) {
        failRouteTarget("已保留编辑，未切换查询目标");
        return;
      }
    }
    if (!current()) return;
    // An edit made while resolving/confirming must not be silently overwritten.
    if (sqlBefore !== queryParams.value.sqlText || idBefore !== queryParams.value.id) {
      failRouteTarget("编辑内容已变化，未替换查询目标");
      return;
    }
    if (!selectDatasource(target.datasourceId, { fromTarget: true })) {
      failRouteTarget("目标数据连接已不可用");
      return;
    }
    appliedSql = buildSelectSql(target.tableName, datasource.datasourceType);
    appliedDatasourceId = String(datasource.id);
    appliedTargetKey = key;
    queryParams.value.sqlText = appliedSql;
    queryParams.value.pageNum = 1;
    nodeData.value.name = target.tableName;
    dialogVisible.value = false;
    targetPending.value = false;
    targetError.value = "";
    // Prefill only: execution and the existing paged/masked result dialog remain manual.
  } catch (error) {
    if (current()) failRouteTarget("无法解析查询目标，请确认资产和连接的访问权限");
  }
}

watch(() => [route.path, route.query.assetId, route.query.datasourceId, route.query.tableName],
  applyRouteTarget, { immediate: true, flush: "sync" });
onActivated(() => { active = true; applyRouteTarget(); });
onDeactivated(() => { active = false; invalidateTarget(); });
onBeforeUnmount(() => { active = false; invalidateTarget(); });

// 2. 懒加载表或字段
const loadTreeData = async (node, resolveSuccess, resolveFail) => {
  console.log("🚀 ~ loadTreeData ~ node:", node)
  if (!node) {
    return resolveSuccess([]);
  }
  try {
    if (node.level == 1) {
      try {
        selectDatasource(node.data.id)
        const res = await tableList(node.data.id);
        const tables = res.data.map((table) => ({
          id: table.tableName,
          key: `table:${node.data.id}:${table.tableName}`,
          datasourceId: node.data.id,
          name: table.tableName,
          isLeaf: false,
          children: [],
          level: 2,
          datasourceType: node.data.datasourceType
        }));
        resolveSuccess(tables);
      } catch (err) {
        resolveFail();
      }
    } else if (node.level == 2) {
      try {
        const parentDs = selectDatasource(node.data.datasourceId || node.parent?.data?.id);
        if (!parentDs) return resolveFail();
        const res = await getColumnByAssetId({
          id: parentDs.id,
          tableName: node.data.id,
        });
        const columns = res.data.map((col) => ({
          id: col.columnName,
          key: `column:${parentDs.id}:${node.data.id}:${col.columnName}`,
          datasourceId: parentDs.id,
          datasourceType: parentDs.datasourceType,
          name: col.columnName,
          level: 3,
          isLeaf: true,
        }));
        resolveSuccess(columns);
      } catch (err) {
        resolveFail();
      }
    } else {
      console.log('3333');

      resolveSuccess([]);
    }
  } finally {


  }
};


// 3. 点击树节点插入SQL
function handleTreeNodeClick({ type, expandedRootId, payload }, node) {
  const data = type === 'node' ? payload : (node?.dataRef || node?.data || node);
  const datasource = selectDatasource(data?.datasourceId || expandedRootId);
  if (!datasource || data?.level === 1) return;
  const text = type === 'sql' ? payload
    : data?.name ? quoteSqlIdentifier(data.name, datasource.datasourceType) : '';
  if (!text) return;
  const currentSql = queryParams.value.sqlText || "";
  const suffix = currentSql.endsWith(" ") || currentSql === "" ? "" : " ";
  queryParams.value.sqlText = currentSql + suffix + text;
}
// 4. 编辑器内容变化
function handleChange(val) {
  queryParams.value.sqlText = val;
}
let DeptTreeRef = ref()
let loadings = ref()


// 5. 查询按钮
async function handleQuery() {
  if (targetPending.value || targetError.value) {
    message.warning(targetPending.value ? "正在校验查询目标，请稍候" : targetError.value);
    return;
  }
  spl.value = editorRef.value?.getEditorSelectedOrAll();
  console.log("🚀 ~ handleQuery ~ spl:", spl)

  const errors = [];
  if (!spl.value) {
    errors.push("请输入 SQL 语句！");
  }

  // 校验数据源 id
  if (!queryParams.value.id) {
    errors.push("请选择数据源！");
  }
  if (errors.length) {
    message.warning(errors.join("，"));
    return;
  }
  if (!TablesByDataSource.value.some((item) => String(item.id) === String(queryParams.value.id))) {
    message.warning("当前数据连接不可查询");
    return;
  }
  // 执行 SQL 查询
  const executionTarget = targetSequence;
  dialogVisible.value = false;
  try {
    loadings.value = true;
    const res = await executeSqlQuery({
      ...queryParams.value,
      sqlText: encrypt(spl.value),
    });
    // sqlText: encrypt(spl),
    if (res.code === 200 && executionTarget === targetSequence && active) {
      dialogVisible.value = true;
    } else if (res.code !== 200 && executionTarget === targetSequence && active) {
      message.warning(res.msg || "查询失败");
    }
  } finally {
    loadings.value = false;
  }
}

// 6. 清空编辑器
function handleClear() {
  queryParams.value.sqlText = "";
}

</script>

<style scoped lang="less">
.app-container {
  height: calc(87vh - 300px);

  .head-container {
    height: 50px;
    background: #fff;
    box-shadow: 0 5px 8px rgba(128, 145, 165, 0.1);
    padding: 0 15px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;

    .head-title {
      font-size: 16px;
      color: var(--el-color-primary);
      display: flex;
      align-items: center;

      &::before {
        content: "";
        display: inline-block;
        background: var(--el-color-primary);
        width: 6px;
        height: 16px;
        border-radius: 2px;
        margin-right: 10px;
      }
    }

    .head-btns {
      display: flex;
      align-items: center;

      .head-select {
        margin-right: 10px;
        padding: 10px 0;
      }

      .ant-btn {
        height: 28px;
        margin-left: 5px;
      }
    }
  }
}

.sql-editor {
  width: 100%;
}
</style>


