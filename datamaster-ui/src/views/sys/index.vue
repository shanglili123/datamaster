<template>
  <div class="dm-page workspace-home">
    <div class="dm-page__header">
      <div>
        <h1 class="dm-page__title">智能数据操作系统</h1>
      </div>
      <div class="workspace-home__actions">
        <a-dropdown :trigger="['click']">
          <button type="button" class="workspace-home__user-trigger">
            <img :src="userAvatar" class="workspace-home__user-avatar" alt="用户头像" />
            <span>{{ userStore.nickName || userStore.userName || "当前用户" }}</span>
            <DownOutlined />
          </button>
          <template #overlay>
            <a-menu @click="handleHomeUserCommand">
              <a-menu-item key="profile">个人中心</a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button :icon="h(ReloadOutlined)" @click="loadSpaces">刷新</a-button>
        <a-button
          v-if="canCreateSpace"
          type="primary"
          :icon="h(PlusOutlined)"
          @click="goCreateSpace"
        >
          新增空间
        </a-button>
      </div>
    </div>

    <section class="intelligence-hero">
      <div class="intelligence-hero__glow intelligence-hero__glow--one"></div>
      <div class="intelligence-hero__glow intelligence-hero__glow--two"></div>
      <div class="intelligence-hero__content">
        <div class="intelligence-hero__eyebrow">
          <span class="intelligence-hero__pulse"></span>
          DATAMASTER INTELLIGENCE OS
          <span class="intelligence-hero__eyebrow-tag">AI × Ontology</span>
        </div>
        <h2>让数据从被管理，<br /><span>变成可执行的决策。</span></h2>
        <p>
          以本体与行动闭环为核心，从数据发现到业务执行，将分散的数据、业务对象和执行动作组织成一张持续运行的决策网络。
        </p>
        <div class="intelligence-hero__actions">
          <a-button type="primary" size="large" :icon="h(RocketOutlined)" @click="openDataExplore">
            开始数据探索
          </a-button>
        </div>
      </div>
      <div class="intelligence-hero__network" aria-hidden="true">
        <div class="network-ring network-ring--outer"></div>
        <div class="network-ring network-ring--inner"></div>
        <div class="network-line network-line--one"></div>
        <div class="network-line network-line--two"></div>
        <div class="network-line network-line--three"></div>
        <div class="network-core">
          <RobotOutlined />
          <strong>AI</strong>
          <small>决策引擎</small>
        </div>
        <div class="network-node network-node--top">
          <DatabaseOutlined />
          <span>数据资产</span>
        </div>
        <div class="network-node network-node--right">
          <ApartmentOutlined />
          <span>本体语义</span>
        </div>
        <div class="network-node network-node--bottom">
          <ThunderboltOutlined />
          <span>行动闭环</span>
        </div>
        <div class="network-status">
          <span></span>
          LIVE OPERATIONAL GRAPH
        </div>
      </div>
    </section>

    <a-row :gutter="16" class="workspace-home__main">
      <a-col :xs="24" :lg="15">
        <div class="dm-card workspace-home__spaces">
          <div class="dm-card__header">
            <div class="workspace-home__card-title">
              <div>
                <span class="workspace-home__card-kicker">WORKSPACES</span>
                <span class="dm-card__title">空间列表</span>
              </div>
              <a-tag size="small">{{ spaceList.length }} 个</a-tag>
              <span class="workspace-home__hint">仅展示当前登录人有权限的空间</span>
            </div>
            <a-input
              v-model:value="keyword"
              allow-clear
              placeholder="搜索空间名称或编码"
              class="workspace-home__search"
            >
              <template #prefix><SearchOutlined /></template>
            </a-input>
          </div>
          <div class="dm-card__body">
            <div v-if="activeSpace" class="workspace-home__active-context">
              <div class="workspace-home__active-context-icon"><FolderOpenOutlined /></div>
              <div class="workspace-home__active-context-copy">
                <span>ACTIVE WORKSPACE</span>
                <strong>{{ activeSpace.name || "未命名空间" }}</strong>
              </div>
              <a-button type="link" @click="enterSpace(activeSpace)">进入工作区 <ArrowRightOutlined /></a-button>
            </div>
            <a-skeleton v-if="loading" :paragraph="{ rows: 6 }" active />
            <a-empty
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
                    <FolderOpenOutlined />
                  </div>
                  <a-tag size="small">空间</a-tag>
                </div>
                <div class="space-card__name" :title="space.name">
                  {{ space.name || "未命名空间" }}
                </div>
                <div class="space-card__meta">
                  {{ space.code || space.spaceCode || "暂无编码" }}
                </div>
                <div class="space-card__footer">
                  <span>点击查看统计，双击进入</span>
                  <a-button
                    type="link"
                    @click.stop="enterSpace(space)"
                  >
                    进入
                  </a-button>
                </div>
              </button>
            </div>
          </div>
        </div>
      </a-col>

      <a-col :xs="24" :lg="9">
        <div class="workspace-home__side">
          <template v-if="!activeSpace">
            <div class="dm-card workspace-home__summary workspace-home__summary--empty">
              <div class="dm-card__body">
                <a-empty description="请选择空间查看统计" :image-style="{ height: '72px' }" />
              </div>
            </div>
          </template>
          <template v-else>
            <a-skeleton v-if="statsLoading" :paragraph="{ rows: 8 }" active />
            <template v-else>
              <div class="dm-card workspace-home__summary">
                <div class="dm-card__header">
                  <div>
                    <span class="workspace-home__card-kicker">RESOURCE SIGNALS</span>
                    <span class="dm-card__title">资源概览</span>
                  </div>
                  <a-tag size="small" color="success">{{ activeSpace?.name }}</a-tag>
                </div>
                <div class="dm-card__body">
                  <div class="dm-metric-grid workspace-home__metrics-resource">
                    <div v-for="item in resourceCards" :key="item.label" class="dm-metric-card dm-metric-card--sm">
                      <div class="dm-metric-card__label">
                        <component :is="item.icon" />
                        <span>{{ item.label }}</span>
                      </div>
                      <div class="dm-metric-card__value">{{ item.value }}</div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="dm-card workspace-home__summary">
                <div class="dm-card__header">
                  <div>
                    <span class="workspace-home__card-kicker">ACTION MONITOR</span>
                    <span class="dm-card__title">任务状态</span>
                  </div>
                </div>
                <div class="dm-card__body">
                  <div class="dm-metric-grid workspace-home__metrics-task">
                    <div v-for="item in taskCards" :key="item.label" class="dm-metric-card">
                      <div class="dm-metric-card__label">
                        <component :is="item.icon" />
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

        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup name="Index">
import { computed, onMounted, reactive, ref, h } from "vue";
import { useRouter } from "vue-router";
import {
  ApartmentOutlined,
  ApiOutlined,
  ArrowRightOutlined,
  BookOutlined,
  CodeOutlined,
  DatabaseOutlined,
  DollarOutlined,
  DownOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  LineChartOutlined,
  PlusOutlined,
  ProfileOutlined,
  ReloadOutlined,
  RobotOutlined,
  RocketOutlined,
  SearchOutlined,
  ThunderboltOutlined,
  WarningFilled,
} from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { currentUser } from "@/api/tax/space/space";
import { getRoutersDpp } from "@/api/system/menu";
import { homeList } from "@/api/system/home";
import usePermissionStore from "@/store/system/permission";
import useUserStore from "@/store/system/user";
import defaultAvatar from "@/assets/images/defaultAvatar.svg";

const router = useRouter();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const userAvatar = computed(() => userStore.avatar || defaultAvatar);

const loading = ref(false);
const statsLoading = ref(false);
const keyword = ref("");
const spaceList = ref([]);
const activeSpace = ref(null);

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
  { label: "数据源", value: stats.datasourceTotal, icon: DollarOutlined },
  { label: "采集表", value: stats.catalogTableTotal, icon: FolderOutlined },
  { label: "API服务", value: stats.apiTotal, icon: ProfileOutlined },
  { label: "数据标准", value: stats.dataElemTotal, icon: BookOutlined },
  { label: "数据模型", value: stats.modelTotal, icon: LineChartOutlined },
  { label: "标签", value: stats.tagTotal, icon: FileTextOutlined },
]);

const taskCards = computed(() => [
  {
    label: "数据集成任务",
    value: stats.integrationTotal,
    sub: `失败 ${stats.integrationFailed}`,
    icon: ApiOutlined,
  },
  {
    label: "数据开发任务",
    value: stats.developTotal,
    sub: `失败 ${stats.developFailed}`,
    icon: CodeOutlined,
  },
  {
    label: "API 调用",
    value: stats.apiCalls,
    sub: `异常 ${stats.apiErrors}`,
    icon: ProfileOutlined,
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
    message.error("空间列表加载失败");
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
  userStore.spaceName = space.name || space.spaceName || "";
  localStorage.setItem("dataMasterSpaceId", space.id);

  try {
    const response = await getRoutersDpp(space.id);
    const routes = response?.data || [];
    permissionStore.updateTopbarRoutes(routes);
    router.push("/space");
  } catch {
    message.error("空间菜单加载失败");
  }
}

function goCreateSpace() {
  router.push("/tax/space");
}

async function openDataExplore() {
  // 从首页直接进入流程工作站时可能尚未选择空间；默认使用当前用户的第一个空间，
  // 这样流程创建、元数据探查和资产同步都会带上正确的空间上下文。
  if (!userStore.spaceId || !userStore.spaceName) {
    try {
      const response = await currentUser();
      const spaces = response?.data || [];
      if (spaces.length) {
        const storedId = userStore.spaceId || localStorage.getItem("dataMasterSpaceId");
        const selected = spaces.find((space) => String(space.id) === String(storedId)) || spaces[0];
        userStore.spaceId = selected.id;
        userStore.spaceCode = selected.code || selected.spaceCode || "";
        userStore.spaceName = selected.name || selected.spaceName || "";
        localStorage.setItem("dataMasterSpaceId", String(selected.id));
      }
    } catch {
      // 流程工作站自身还会再次尝试加载空间，不阻断页面打开。
    }
  }
  router.push("/explore");
}

function handleHomeUserCommand({ key }) {
  if (key === "profile") {
    router.push("/user/profile");
    return;
  }
  if (key === "logout") {
    userStore.logOut().then(() => {
      window.location.href = "/index";
    });
  }
}

onMounted(() => {
  loadSpaces();
});
</script>

<style lang="scss" scoped>
.intelligence-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  min-height: 330px;
  margin-bottom: 14px;
  overflow: hidden;
  color: #f5f7ff;
  background:
    radial-gradient(circle at 78% 20%, rgba(92, 110, 255, 0.28), transparent 30%),
    linear-gradient(116deg, #111932 0%, #18254c 53%, #10172d 100%);
  border: 1px solid rgba(142, 163, 255, 0.2);
  border-radius: 18px;
  box-shadow: 0 18px 45px rgba(20, 30, 66, 0.2);

  &::before {
    position: absolute;
    inset: 0;
    pointer-events: none;
    content: "";
    opacity: 0.2;
    background-image: linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
    background-size: 34px 34px;
    mask-image: linear-gradient(90deg, #000, transparent 85%);
  }
}

.intelligence-hero__glow {
  position: absolute;
  width: 230px;
  height: 230px;
  border-radius: 50%;
  filter: blur(4px);
  opacity: 0.35;
  pointer-events: none;
}

.intelligence-hero__glow--one {
  top: -120px;
  left: 38%;
  background: #6576ff;
}

.intelligence-hero__glow--two {
  right: -100px;
  bottom: -110px;
  background: #27c6c8;
}

.intelligence-hero__content {
  position: relative;
  z-index: 1;
  padding: 34px 28px 28px 32px;
}

.intelligence-hero__eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #aebcff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.intelligence-hero__pulse,
.network-status span {
  width: 7px;
  height: 7px;
  background: #56e0c0;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(86, 224, 192, 0.13), 0 0 14px rgba(86, 224, 192, 0.8);
}

.intelligence-hero__eyebrow-tag {
  margin-left: 4px;
  padding: 4px 8px;
  color: #c7d0ff;
  font-size: 10px;
  letter-spacing: 0.05em;
  background: rgba(132, 147, 255, 0.16);
  border: 1px solid rgba(168, 181, 255, 0.24);
  border-radius: 999px;
}

.intelligence-hero h2 {
  margin: 18px 0 12px;
  color: #fff;
  font-size: clamp(29px, 3.2vw, 46px);
  font-weight: 650;
  letter-spacing: -0.04em;
  line-height: 1.12;

  span {
    color: #8ee7d7;
  }
}

.intelligence-hero__content > p {
  max-width: 580px;
  margin: 0;
  color: #bac4df;
  font-size: 14px;
  line-height: 1.8;
}

.intelligence-hero__actions {
  display: flex;
  gap: 10px;
  margin-top: 22px;

  :deep(.ant-btn) {
    border-radius: 9px;
  }

  :deep(.ant-btn-primary) {
    color: #10213b;
    background: #8ee7d7;
    border-color: #8ee7d7;
    box-shadow: 0 8px 18px rgba(71, 218, 187, 0.18);
  }

  :deep(.ant-btn-background-ghost) {
    color: #d8e0ff;
    border-color: rgba(205, 216, 255, 0.42);
  }
}

.intelligence-hero__network {
  position: relative;
  min-height: 330px;
}

.network-ring {
  position: absolute;
  top: 50%;
  left: 52%;
  border: 1px solid rgba(131, 160, 255, 0.23);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.network-ring--outer {
  width: 254px;
  height: 254px;
}

.network-ring--inner {
  width: 158px;
  height: 158px;
  border-color: rgba(119, 231, 211, 0.35);
  box-shadow: inset 0 0 30px rgba(75, 221, 202, 0.08), 0 0 36px rgba(92, 126, 255, 0.08);
}

.network-line {
  position: absolute;
  top: 50%;
  left: 52%;
  width: 104px;
  height: 1px;
  transform-origin: left center;
  background: linear-gradient(90deg, rgba(120, 230, 210, 0.8), rgba(120, 160, 255, 0.08));
}

.network-line--one { transform: rotate(-90deg); }
.network-line--two { transform: rotate(28deg); }
.network-line--three { transform: rotate(150deg); }

.network-core {
  position: absolute;
  top: 50%;
  left: 52%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 106px;
  height: 106px;
  color: #eafffb;
  background: radial-gradient(circle at 35% 30%, #3d6f88, #233662 68%);
  border: 1px solid rgba(145, 239, 221, 0.72);
  border-radius: 50%;
  box-shadow: 0 0 0 8px rgba(115, 159, 255, 0.08), 0 0 35px rgba(94, 204, 190, 0.26);
  transform: translate(-50%, -50%);

  :deep(svg) { margin-bottom: 2px; font-size: 21px; }
  strong { font-size: 23px; letter-spacing: 0.08em; }
  small { color: #a7c8d4; font-size: 10px; }
}

.network-node {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 11px;
  color: #d9e2ff;
  font-size: 12px;
  background: rgba(25, 40, 82, 0.76);
  border: 1px solid rgba(148, 169, 255, 0.3);
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(6, 13, 35, 0.18);
  backdrop-filter: blur(8px);
}

.network-node :deep(svg) { color: #8ee7d7; }
.network-node--top { top: 31px; left: calc(52% - 47px); }
.network-node--right { top: 53%; right: 12px; }
.network-node--bottom { bottom: 32px; left: calc(52% - 48px); }

.network-status {
  position: absolute;
  right: 20px;
  bottom: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #7f93c7;
  font-size: 9px;
  letter-spacing: 0.1em;
}

.network-status span { width: 5px; height: 5px; box-shadow: 0 0 9px rgba(86, 224, 192, 0.8); }

.workspace-home {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 32px);
  min-height: 640px;
  overflow: auto;

  .workspace-home__card-kicker {
    display: block;
    color: #8290aa;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.13em;
    line-height: 16px;
  }

  .workspace-home__actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .workspace-home__user-trigger {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 32px;
    max-width: 150px;
    padding: 3px 9px 3px 4px;
    overflow: hidden;
    color: var(--dm-text-regular, #374151);
    font-size: 13px;
    background: #fff;
    border: 1px solid var(--dm-border-light);
    border-radius: 17px;
    cursor: pointer;
    transition: border-color 0.2s ease, background-color 0.2s ease;

    &:hover {
      background: #f8fafc;
      border-color: #cbd5e1;
    }

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .workspace-home__user-avatar {
    width: 24px;
    height: 24px;
    flex: 0 0 24px;
    object-fit: cover;
    background: #f2f3f5;
    border: 1px solid #eef0f3;
    border-radius: 50%;
  }

  .workspace-home__search {
    width: 240px;
  }

  .workspace-home__main {
    flex: 1;
    min-height: 0;
    row-gap: 16px;

    :deep(.ant-col) {
      display: flex;
      min-height: 0;
    }
  }

  .workspace-home__spaces,
  .workspace-home__side {
    width: 100%;
    min-height: 0;
  }

  .workspace-home__spaces,
  .workspace-home__summary {
    border-radius: 15px;
    box-shadow: 0 8px 26px rgba(26, 43, 79, 0.06);
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

    .dm-card__header {
      background: linear-gradient(120deg, #ffffff 0%, #f6f9ff 100%);
    }
  }

  .workspace-home__card-title {
    > div:first-child {
      display: flex;
      flex-direction: column;
    }
  }

  .workspace-home__card-kicker {
    margin-bottom: 1px;
    color: #8b98b0;
    font-size: 9px;
    line-height: 13px;
  }

  .workspace-home__active-context {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 14px;
    padding: 10px 12px;
    background: linear-gradient(105deg, #f0f5ff 0%, #f4fbfa 100%);
    border: 1px solid #dce8f7;
    border-radius: 11px;

    .ant-btn-link {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      margin-left: auto;
      padding-right: 0;
      font-size: 12px;
    }
  }

  .workspace-home__active-context-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    color: #5369d9;
    background: #e4ebff;
    border-radius: 9px;
  }

  .workspace-home__active-context-copy {
    display: flex;
    flex-direction: column;
    min-width: 0;

    span {
      color: #8592ad;
      font-size: 9px;
      font-weight: 700;
      letter-spacing: 0.1em;
    }

    strong {
      max-width: 300px;
      overflow: hidden;
      color: var(--dm-text-main);
      font-size: 13px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .workspace-home__side {
    display: grid;
    grid-template-rows: auto auto;
    gap: 10px;
    height: 100%;
    overflow: hidden;
  }

  .workspace-home__summary {
    overflow: hidden;

    .dm-card__header {
      background: linear-gradient(120deg, #ffffff 0%, #fbfcff 100%);
    }
  }

  .workspace-home__summary--empty {
    min-height: 160px;
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
    background: linear-gradient(145deg, #ffffff 0%, #f8faff 100%);
    border-color: #e7ebf3;
    border-radius: 11px;
    transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

    &:hover {
      border-color: #c8d5fa;
      box-shadow: 0 7px 16px rgba(59, 83, 166, 0.09);
      transform: translateY(-1px);
    }
  }

  :deep(.dm-metric-card__value) {
    margin-top: 4px;
    font-size: 22px;
    line-height: 28px;
  }

  :deep(.dm-metric-card__sub) {
    margin-top: 2px;
  }

  .workspace-home__metrics-task {
    :deep(.dm-metric-card:nth-child(1)) { border-left: 3px solid #6576e8; }
    :deep(.dm-metric-card:nth-child(2)) { border-left: 3px solid #42b8ae; }
    :deep(.dm-metric-card:nth-child(3)) { border-left: 3px solid #e9a44c; }
    :deep(.dm-metric-card:nth-child(4)) { border-left: 3px solid #e56c7a; }
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
  .intelligence-hero {
    display: block;
    min-height: 560px;
  }

  .intelligence-hero__content {
    padding: 24px 20px 0;
  }

  .intelligence-hero h2 {
    font-size: clamp(28px, 8vw, 38px);
  }

  .intelligence-hero__network {
    min-height: 260px;
    margin-top: 4px;
    transform: scale(0.86);
    transform-origin: center top;
  }

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
      :deep(.ant-col) {
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

    .workspace-home__active-context {
      align-items: flex-start;
      flex-wrap: wrap;

      .ant-btn-link {
        width: 100%;
        justify-content: flex-start;
        margin-left: 42px;
        padding-left: 0;
      }
    }

    .workspace-home__metrics-resource {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .workspace-home__metrics-task {
      grid-template-columns: 1fr;
    }
  }
}

@media screen and (min-width: 769px) and (max-width: 1100px) {
  .intelligence-hero {
    grid-template-columns: minmax(0, 1fr) minmax(300px, 0.82fr);
  }

  .intelligence-hero__content {
    padding-left: 24px;
  }

  .intelligence-hero__network {
    transform: scale(0.9);
    transform-origin: center center;
  }

}
</style>
