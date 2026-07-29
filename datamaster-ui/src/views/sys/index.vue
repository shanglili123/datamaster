<template>
  <div class="dm-page workspace-home">
    <div class="dm-page__header">
      <div>
        <h1 class="dm-page__title">空间工作台</h1>
        <div class="dm-page__desc">
          从空间进入标准建模、数据研发、数据服务和元数据管理工作区。
        </div>
      </div>
      <div class="workspace-home__actions">
        <el-button :icon="Refresh" @click="loadSpaces">刷新</el-button>
        <el-button
          v-if="canCreateSpace"
          type="primary"
          :icon="Plus"
          @click="goCreateSpace"
        >
          新增空间
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="workspace-home__main">
      <el-col :xs="24" :lg="15">
        <div class="dm-card workspace-home__spaces">
          <div class="dm-card__header">
            <div class="workspace-home__card-title">
              <span class="dm-card__title">空间列表</span>
              <el-tag size="small" type="info">{{ spaceList.length }} 个</el-tag>
              <span class="workspace-home__hint">仅展示当前登录人有权限的空间</span>
            </div>
            <el-input
              v-model="keyword"
              clearable
              :prefix-icon="Search"
              placeholder="搜索空间名称或编码"
              class="workspace-home__search"
            />
          </div>
          <div class="dm-card__body">
            <el-skeleton v-if="loading" :rows="6" animated />
            <el-empty
              v-else-if="filteredSpaces.length === 0"
              description="暂无可进入的空间"
            />
            <div v-else class="space-grid">
              <button
                v-for="space in filteredSpaces"
                :key="space.id"
                class="space-card"
                :class="{ 'is-active': activeSpace?.id === space.id }"
                type="button"
                @click="selectSpace(space)"
                @dblclick="enterSpace(space)"
              >
                <div class="space-card__top">
                  <div class="space-card__icon">
                    <el-icon><FolderOpened /></el-icon>
                  </div>
                  <el-tag size="small" effect="plain">空间</el-tag>
                </div>
                <div class="space-card__name" :title="space.name">
                  {{ space.name || "未命名空间" }}
                </div>
                <div class="space-card__meta">
                  {{ space.code || space.spaceCode || "暂无编码" }}
                </div>
                <div class="space-card__footer">
                  <span>点击查看统计，双击进入</span>
                  <el-button
                    link
                    type="primary"
                    @click.stop="enterSpace(space)"
                  >
                    进入
                  </el-button>
                </div>
              </button>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :lg="9">
        <div class="workspace-home__side">
          <template v-if="!activeSpace">
            <div class="dm-card workspace-home__summary workspace-home__summary--empty">
              <div class="dm-card__body">
                <el-empty description="请选择空间查看统计" :image-size="72" />
              </div>
            </div>
          </template>
          <template v-else>
            <el-skeleton v-if="statsLoading" :rows="8" animated />
            <template v-else>
              <div class="dm-card workspace-home__summary">
                <div class="dm-card__header">
                  <span class="dm-card__title">资源概览</span>
                  <el-tag size="small" type="success">{{ activeSpace?.name }}</el-tag>
                </div>
                <div class="dm-card__body">
                  <div class="dm-metric-grid workspace-home__metrics-resource">
                    <div v-for="item in resourceCards" :key="item.label" class="dm-metric-card dm-metric-card--sm">
                      <div class="dm-metric-card__label">
                        <el-icon><component :is="item.icon" /></el-icon>
                        <span>{{ item.label }}</span>
                      </div>
                      <div class="dm-metric-card__value">{{ item.value }}</div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="dm-card workspace-home__summary">
                <div class="dm-card__header">
                  <span class="dm-card__title">任务状态</span>
                </div>
                <div class="dm-card__body">
                  <div class="dm-metric-grid workspace-home__metrics-task">
                    <div v-for="item in taskCards" :key="item.label" class="dm-metric-card">
                      <div class="dm-metric-card__label">
                        <el-icon><component :is="item.icon" /></el-icon>
                        <span>{{ item.label }}</span>
                      </div>
                      <div class="dm-metric-card__value">{{ item.value }}</div>
                      <div class="dm-metric-card__sub">{{ item.sub }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </template>

          <div class="dm-card workspace-home__table-volume">
            <div class="dm-card__header">
              <span class="dm-card__title">数据库表数据量</span>
              <span class="workspace-home__hint">当前空间各表行数</span>
            </div>
            <div class="dm-card__body">
              <el-table
                v-if="tableRows.length > 0"
                :data="tableRows"
                stripe
                height="100%"
              >
                <el-table-column prop="datasourceName" label="数据源" min-width="140" />
                <el-table-column prop="schemaName" label="库/Schema" min-width="120" />
                <el-table-column prop="tableName" label="表名" min-width="150" />
                <el-table-column prop="rowCount" label="数据量" width="100" align="right" />
              </el-table>
              <el-empty
                v-else
                description="暂无表数据量统计"
                :image-size="72"
              />
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  Coin,
  Collection,
  Connection,
  DataAnalysis,
  DataBoard,
  DataLine,
  Document,
  FolderOpened,
  Plus,
  Refresh,
  Search,
  Tickets,
  WarningFilled,
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { currentUser } from "@/api/tax/space/space";
import { getRoutersDpp } from "@/api/system/menu";
import { homeList } from "@/api/system/home";
import usePermissionStore from "@/store/system/permission";
import useUserStore from "@/store/system/user";

const router = useRouter();
const userStore = useUserStore();
const permissionStore = usePermissionStore();

const loading = ref(false);
const statsLoading = ref(false);
const keyword = ref("");
const spaceList = ref([]);
const activeSpace = ref(null);
const tableRows = ref([]);

const stats = reactive({
  integrationTotal: "--",
  integrationFailed: "--",
  developTotal: "--",
  developFailed: "--",
  apiCalls: "--",
  apiErrors: "--",
  datasourceTotal: "--",
  catalogTableTotal: "--",
  apiTotal: "--",
  dataElemTotal: "--",
  modelTotal: "--",
  tagTotal: "--",
  collectTaskTotal: "--",
  documentTotal: "--",
});

const canCreateSpace = computed(() => {
  const roles = userStore.roles || [];
  const permissions = userStore.permissions || [];
  return (
    roles.includes("admin") ||
    roles.includes("system") ||
    permissions.includes("*:*:*")
  );
});

const filteredSpaces = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) return spaceList.value;
  return spaceList.value.filter((space) => {
    const name = String(space.name || "").toLowerCase();
    const code = String(space.code || space.spaceCode || "").toLowerCase();
    return name.includes(value) || code.includes(value);
  });
});

const resourceCards = computed(() => [
  { label: "数据源", value: stats.datasourceTotal, icon: Coin },
  { label: "采集表", value: stats.catalogTableTotal, icon: Collection },
  { label: "API服务", value: stats.apiTotal, icon: Tickets },
  { label: "数据标准", value: stats.dataElemTotal, icon: DataBoard },
  { label: "数据模型", value: stats.modelTotal, icon: DataAnalysis },
  { label: "标签", value: stats.tagTotal, icon: Document },
]);

const taskCards = computed(() => [
  {
    label: "数据集成任务",
    value: stats.integrationTotal,
    sub: `失败 ${stats.integrationFailed}`,
    icon: Connection,
  },
  {
    label: "数据开发任务",
    value: stats.developTotal,
    sub: `失败 ${stats.developFailed}`,
    icon: DataLine,
  },
  {
    label: "API 调用",
    value: stats.apiCalls,
    sub: `异常 ${stats.apiErrors}`,
    icon: Tickets,
  },
  {
    label: "风险任务",
    value: riskTaskCount.value,
    sub: "失败任务汇总",
    icon: WarningFilled,
  },
]);

const riskTaskCount = computed(() => {
  const values = [stats.integrationFailed, stats.developFailed, stats.apiErrors]
    .map((item) => Number(item))
    .filter((item) => !Number.isNaN(item));
  if (values.length === 0) return "--";
  return values.reduce((sum, item) => sum + item, 0);
});

function resetStats() {
  stats.integrationTotal = "--";
  stats.integrationFailed = "--";
  stats.developTotal = "--";
  stats.developFailed = "--";
  stats.apiCalls = "--";
  stats.apiErrors = "--";
  stats.datasourceTotal = "--";
  stats.catalogTableTotal = "--";
  stats.apiTotal = "--";
  stats.dataElemTotal = "--";
  stats.modelTotal = "--";
  stats.tagTotal = "--";
  stats.collectTaskTotal = "--";
  stats.documentTotal = "--";
  tableRows.value = [];
}

async function loadSpaces() {
  loading.value = true;
  try {
    const response = await currentUser();
    spaceList.value = response?.data || [];
    if (spaceList.value.length > 0) {
      const storedSpaceId = localStorage.getItem("dataMasterSpaceId");
      const storedSpace = spaceList.value.find(
        (space) => String(space.id) === String(storedSpaceId)
      );
      selectSpace(storedSpace || spaceList.value[0]);
    } else {
      activeSpace.value = null;
      resetStats();
    }
  } catch {
    spaceList.value = [];
    activeSpace.value = null;
    resetStats();
    ElMessage.error("空间列表加载失败");
  } finally {
    loading.value = false;
  }
}

function selectSpace(space) {
  activeSpace.value = space;
  loadSpaceStats(space);
}

async function loadSpaceStats(space) {
  if (!space?.id) {
    resetStats();
    return;
  }

  resetStats();
  statsLoading.value = true;
  try {
    const response = await homeList({
      spaceId: space.id,
      spaceCode: space.code || space.spaceCode || "",
    });
    const data = response?.data || {};
    stats.integrationTotal = data.integrationTaskTotal ?? "--";
    stats.integrationFailed = data.integrationTaskFailed ?? "--";
    stats.developTotal = data.developTaskTotal ?? "--";
    stats.developFailed = data.developTaskFailed ?? "--";
    stats.apiCalls = data.apiCallTotal ?? "--";
    stats.apiErrors = data.apiCallFailed ?? "--";
    stats.datasourceTotal = data.datasourceTotal ?? "--";
    stats.catalogTableTotal = data.catalogTableTotal ?? "--";
    stats.apiTotal = data.apiTotal ?? "--";
    stats.dataElemTotal = data.dataElemTotal ?? "--";
    stats.modelTotal = data.modelTotal ?? "--";
    stats.tagTotal = data.tagTotal ?? "--";
    stats.collectTaskTotal = data.collectTaskTotal ?? "--";
    stats.documentTotal = data.documentTotal ?? "--";
    tableRows.value = data.tableRows || [];
  } catch {
    resetStats();
  } finally {
    statsLoading.value = false;
  }
}

async function enterSpace(space) {
  if (!space?.id) return;

  userStore.spaceId = space.id;
  userStore.spaceCode = space.code || space.spaceCode || "";
  localStorage.setItem("dataMasterSpaceId", space.id);

  try {
    const response = await getRoutersDpp(space.id);
    const routes = response?.data || [];
    permissionStore.updateTopbarRoutes(routes);
    const targetPath = findFirstRoutePath(permissionStore.addRoutes);
    router.push(targetPath || "/index");
  } catch {
    ElMessage.error("空间菜单加载失败");
  }
}

function findFirstRoutePath(routes, parentPath = "") {
  for (const route of routes || []) {
    if (route.hidden) continue;
    const currentPath = joinRoutePath(parentPath, route.path);
    if (route.children?.length) {
      const childPath = findFirstRoutePath(route.children, currentPath);
      if (childPath) return childPath;
    }
    if (currentPath && currentPath !== "/" && currentPath !== "/index") {
      return currentPath;
    }
  }
  return "";
}

function joinRoutePath(parentPath, path) {
  if (!path) return parentPath;
  if (/^https?:\/\//.test(path)) return path;
  if (path.startsWith("/")) return path;
  const parent = parentPath.endsWith("/")
    ? parentPath.slice(0, -1)
    : parentPath;
  return `${parent}/${path}`.replace(/\/+/g, "/");
}

function goCreateSpace() {
  router.push("/tax/space");
}

onMounted(() => {
  loadSpaces();
});
</script>

<style lang="scss" scoped>
.workspace-home {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 96px);
  min-height: 640px;
  overflow: hidden;

  .workspace-home__actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .workspace-home__search {
    width: 240px;
  }

  .workspace-home__main {
    flex: 1;
    min-height: 0;
    row-gap: 16px;

    :deep(.el-col) {
      display: flex;
      min-height: 0;
    }
  }

  .workspace-home__spaces,
  .workspace-home__side {
    width: 100%;
    min-height: 0;
  }

  .workspace-home__spaces {
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .dm-card__body {
      flex: 1;
      min-height: 0;
      overflow: auto;
    }
  }

  .workspace-home__side {
    display: grid;
    grid-template-rows: auto auto minmax(120px, 1fr);
    gap: 10px;
    height: 100%;
    overflow: hidden;
  }

  .workspace-home__summary {
    overflow: hidden;
  }

  .workspace-home__summary--empty {
    min-height: 160px;
  }

  .workspace-home__table-volume {
    display: flex;
    flex-direction: column;
    min-height: 0;

    .dm-card__body {
      flex: 1;
      min-height: 0;
      padding: 8px 12px 12px;
    }
  }

  .workspace-home__hint {
    color: var(--dm-text-secondary);
    font-size: 12px;
  }

  .workspace-home__metrics-resource {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .workspace-home__metrics-task {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .workspace-home__card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .dm-metric-card--sm {
    .dm-metric-card__value {
      font-size: 20px;
    }
  }

  .dm-metric-card__label {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  :deep(.dm-card__header) {
    min-height: 42px;
    padding: 0 12px;
  }

  :deep(.dm-card__body) {
    padding: 12px;
  }

  :deep(.dm-metric-card) {
    min-height: 74px;
    padding: 10px 12px;
  }

  :deep(.dm-metric-card__value) {
    margin-top: 4px;
    font-size: 22px;
    line-height: 28px;
  }

  :deep(.dm-metric-card__sub) {
    margin-top: 2px;
  }
}

.space-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 10px;
}

.space-card {
  min-width: 0;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--dm-border-light);
  border-radius: var(--dm-radius-base);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;

  &:hover,
  &.is-active {
    border-color: rgba(24, 144, 255, 0.45);
    box-shadow: var(--dm-shadow-card);
    transform: translateY(-1px);
  }

  &.is-active {
    background: linear-gradient(180deg, #ffffff 0%, #f5f9ff 100%);
  }

  .space-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .space-card__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 32px;
    color: var(--dm-color-primary);
    background: var(--dm-bg-soft);
    border-radius: var(--dm-radius-base);
  }

  .space-card__name {
    overflow: hidden;
    color: var(--dm-text-main);
    font-size: 16px;
    font-weight: 600;
    line-height: 24px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .space-card__meta {
    min-height: 20px;
    margin-top: 4px;
    overflow: hidden;
    color: var(--dm-text-secondary);
    font-size: 13px;
    line-height: 20px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .space-card__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
    color: var(--dm-text-secondary);
    font-size: 12px;
  }
}

@media screen and (max-width: 768px) {
  .workspace-home {
    height: auto;
    min-height: 100%;
    overflow: visible;

    .dm-page__header,
    .dm-card__header {
      align-items: flex-start;
      flex-direction: column;
    }

    .workspace-home__main {
      :deep(.el-col) {
        display: block;
      }
    }

    .workspace-home__spaces {
      .dm-card__body {
        overflow: visible;
      }
    }

    .workspace-home__side {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .workspace-home__search {
      width: 100%;
    }

    .workspace-home__metrics-resource {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .workspace-home__metrics-task {
      grid-template-columns: 1fr;
    }
  }
}
</style>
