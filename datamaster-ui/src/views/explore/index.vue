<template>
  <div class="explore-station">
    <header class="explore-station__header">
      <button class="explore-station__brand" type="button" @click="goFlowHome">
        <span class="explore-station__brand-mark"><NodeIndexOutlined /></span>
        <span>
          <strong>DataMaster Exploration</strong>
          <small>数据探索工作站</small>
        </span>
      </button>

      <nav class="explore-station__primary" aria-label="数据探索流程">
        <button
          v-for="(flow, index) in flows"
          :key="flow.id"
          type="button"
          class="explore-station__primary-item"
          :class="{ 'is-active': activeFlowId === flow.id }"
          @click="selectFlow(flow.id)"
        >
          <span class="explore-station__step">{{ String(index + 1).padStart(2, '0') }}</span>
          <component :is="flow.icon" />
          <span>{{ flow.title }}</span>
        </button>
      </nav>

      <div class="explore-station__header-actions">
        <a-dropdown v-if="spaceList.length" :trigger="['click']">
          <a-button class="explore-station__space-switch" :loading="spaceLoading" :title="`当前空间：${spaceName || '未选择空间'}`">
            <span>空间切换</span>
            <template #icon><DownOutlined /></template>
          </a-button>
          <template #overlay>
            <a-menu :selected-keys="spaceName ? [String(userStore.spaceId)] : []" @click="handleSpaceSwitch">
              <a-menu-item v-for="space in spaceList" :key="String(space.id)">
                {{ space.name || space.spaceName || space.code || space.spaceCode || "未命名空间" }}
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button v-else class="explore-station__space-switch" :loading="spaceLoading" @click="ensureExploreSpace">
          空间切换
          <template #icon><DownOutlined /></template>
        </a-button>
        <a-button v-if="activeFlow" @click="goFlowHome">
          <template #icon><HomeOutlined /></template>
          流程首页
        </a-button>
        <a-button v-else @click="goSystemHome">
          <template #icon><ArrowLeftOutlined /></template>
          返回首页
        </a-button>
      </div>
    </header>

    <template v-if="activeFlow">
      <div class="explore-station__secondary">
        <div class="explore-station__breadcrumb">
          <button type="button" @click="goFlowHome">流程首页</button>
          <RightOutlined />
          <strong>{{ activeFlow.title }}</strong>
          <template v-if="activeSubFlow">
            <RightOutlined />
            <span>{{ activeSubFlow.title }}</span>
          </template>
        </div>

        <nav class="explore-station__secondary-nav" :aria-label="`${activeFlow.title}功能`">
          <button
            v-for="item in activeFlow.children"
            :key="item.id"
            type="button"
            :class="{ 'is-active': activeSubId === item.id }"
            @click="selectSubFlow(item.id)"
          >
            {{ item.title }}
          </button>
        </nav>

        <a-button v-if="nextFlow" type="primary" ghost @click="selectFlow(nextFlow.id)">
          下一流程：{{ nextFlow.title }}
          <template #icon><ArrowRightOutlined /></template>
        </a-button>
        <a-button v-else type="primary" ghost @click="goFlowHome">
          完成探索
          <template #icon><CheckOutlined /></template>
        </a-button>
      </div>

      <main class="explore-station__workspace">
        <div class="explore-station__workspace-head">
          <div>
            <span>{{ activeFlow.eyebrow }}</span>
            <h1>{{ embeddedPage?.title || activeSubFlow?.title }}</h1>
            <p>{{ embeddedPage ? embeddedPageDescription : activeSubFlow?.description }}</p>
          </div>
          <div class="explore-station__progress">
            <span>探索进度</span>
            <strong>{{ activeFlowIndex + 1 }} / {{ flows.length }}</strong>
          </div>
        </div>
        <section class="explore-station__module">
          <component
            :is="embeddedPage?.component || activeSubFlow?.component"
            v-if="embeddedPage?.component || activeSubFlow?.component"
            :key="embeddedPage?.id || activeSubFlow?.id"
          />
        </section>
      </main>
    </template>

    <main v-else class="explore-home">
      <section class="explore-home__agent">
        <div class="explore-home__agent-copy">
          <span class="explore-home__agent-eyebrow">DATA AGENT · WORKFLOW ENTRY</span>
          <h1>用一句话启动数据探索</h1>
          <p>描述你的目标，数据智能体会带你进入数据探查、资产治理、本体建模和决策分析流程。</p>
        </div>
        <div class="explore-home__agent-dialog">
          <a-textarea
            v-model:value="agentIntent"
            :rows="3"
            :maxlength="500"
            placeholder="例如：接入 datamaster_test，探查元数据并同步资产，然后生成本体和决策 Skill。"
            @keydown.enter.exact.prevent="startAgentConversation"
          />
          <div class="explore-home__agent-actions">
            <span>支持自然语言描述目标 <em class="explore-home__agent-counter">{{ agentIntent.length }}/500</em></span>
            <a-button type="primary" :loading="agentPlanning" :disabled="agentPlanning || agentOperationStatus === 'RUNNING'" @click="startAgentConversation">
              {{ agentPlanning ? "正在启动数据智能体" : (agentOperationStatus === 'RUNNING' ? "流程自动执行中" : "自动执行数据流程") }}
              <template #icon><ArrowRightOutlined /></template>
            </a-button>
          </div>
          <div v-if="agentPlanning || agentPlanReady || agentOperationId" class="explore-home__agent-progress">
            <div class="explore-home__agent-progress-head">
              <span>{{ agentOperationStatus === 'NEED_INPUT' ? "需要补充连接信息" : (agentOperationStatus === 'NEED_CONFIRMATION' ? "等待确认发布探查任务" : (agentOperationStatus === 'FAILED' ? "流程执行失败" : (agentOperationStatus === 'INTERRUPTED' ? "流程已中断，可从当前步骤继续" : (agentOperationStatus === 'SUCCEEDED' ? "数据流程已自动完成" : "数据智能体正在执行流程")))) }}</span>
              <strong>{{ agentPlanProgress }}%</strong>
            </div>
            <div class="explore-home__agent-progress-bar">
              <i :style="{ width: `${agentPlanProgress}%` }"></i>
            </div>
            <div ref="agentOutputRef" class="explore-home__agent-output">
              <div v-for="(line, index) in agentPlanOutput" :key="`${line}-${index}`">{{ line }}</div>
            </div>
            <div class="explore-home__agent-steps">
              <div
                v-for="(step, index) in agentPlanSteps"
                :key="step"
                class="explore-home__agent-step"
                :class="agentStepStatus(index)"
              >
                <span class="explore-home__agent-step-dot">
                  <CheckOutlined v-if="agentStepStatus(index) === 'is-done'" />
                  <i v-else-if="agentStepStatus(index) === 'is-active'"></i>
                </span>
                <span>{{ step }}</span>
              </div>
            </div>
            <div v-if="agentPlanReady || agentOperationId" class="explore-home__agent-plan">
              <div class="explore-home__agent-plan-head">
                <div>
                  <span>{{ agentOperationStatus === 'NEED_INPUT' ? '请补充连接信息后继续' : (agentOperationStatus === 'NEED_CONFIRMATION' ? '请确认发布并执行探查任务' : '自动运营流程 · 目标数据源') }}</span>
                  <strong>{{ agentPlanDatabase }}</strong>
                </div>
                <a-tag :color="agentOperationStatus === 'FAILED' ? 'red' : (agentOperationStatus === 'SUCCEEDED' ? 'green' : (agentOperationStatus === 'NEED_CONFIRMATION' ? 'orange' : 'blue'))">{{ agentOperationStatusLabel }}</a-tag>
              </div>
              <div v-if="agentOperationStatus === 'NEED_INPUT'" class="explore-home__agent-connection">
                <a-input v-model:value="agentConnection.databaseName" placeholder="数据库名称" />
                <a-input v-model:value="agentConnection.datasourceType" placeholder="数据库类型，如 PostgreSQL" />
                <a-input v-model:value="agentConnection.ip" placeholder="数据库地址" />
                <a-input-number v-model:value="agentConnection.port" :min="1" placeholder="端口" />
                <a-textarea v-model:value="agentConnection.datasourceConfig" :rows="2" placeholder="连接配置 JSON（用户名、密码等）" />
                <a-button type="primary" :loading="agentPlanning" @click="resumeAgentOperation">补充后继续自动执行</a-button>
              </div>
              <div class="explore-home__agent-plan-list">
                <div
                  v-for="(item, index) in agentPlanItems"
                  :key="item.title"
                  class="explore-home__agent-plan-item"
                  :class="agentPlanItemClass(item)"
                >
                  <span>
                    <CheckOutlined v-if="item.status === '已完成'" />
                    <template v-else>{{ String(index + 1).padStart(2, "0") }}</template>
                  </span>
                  <div>
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.description }}</small>
                  </div>
                  <em>{{ item.status }}</em>
                </div>
              </div>
              <div class="explore-home__agent-plan-actions">
                <a-button @click="resetAgentPlan">重新开始</a-button>
                <a-button v-if="agentOperationStatus === 'NEED_CONFIRMATION'" type="primary" :loading="agentPlanning" @click="confirmMetadataTask">确认发布并执行</a-button>
                <a-button v-if="agentOperationStatus === 'FAILED' || agentOperationStatus === 'INTERRUPTED'" type="primary" :loading="agentPlanning" @click="retryAgentOperation">从当前步骤重试</a-button>
                <a-button v-if="agentOperationStatus === 'SUCCEEDED'" type="primary" @click="selectFlow('ai')">打开决策智能体</a-button>
              </div>
            </div>
          </div>
        </div>
        <div class="explore-home__agent-history">
          <div class="explore-home__agent-history-head">
            <strong>流程任务记录</strong>
            <span>已完成、失败和待处理的流程都会保留</span>
          </div>
          <div v-if="!agentOperationHistory.length" class="explore-home__agent-history-empty">
            当前空间还没有已保存的数据智能体流程记录
          </div>
          <template v-else>
            <button
              v-for="operation in agentOperationHistory"
              :key="operation.id"
              type="button"
              class="explore-home__agent-history-item"
              :class="{ 'is-current': operation.id === agentOperationId }"
              @click="selectAgentOperation(operation)"
            >
              <span>{{ operation.datasourceName || operation.databaseName || '未命名数据源' }}</span>
              <small>{{ operation.currentStep || operation.goal || '数据智能体流程' }}</small>
              <em>{{ operationStatusLabel(operation.status) }}</em>
            </button>
          </template>
        </div>
        <div class="explore-home__agent-prompts">
          <button v-for="example in agentExamples" :key="example" type="button" @click="agentIntent = example">
            {{ example }}
          </button>
        </div>
      </section>
      <section class="explore-home__flow">
        <button
          v-for="(flow, index) in flows"
          :key="flow.id"
          type="button"
          class="explore-flow-card"
          :style="{ '--flow-color': flow.color, '--flow-soft': flow.softColor }"
          @click="selectFlow(flow.id)"
        >
          <div class="explore-flow-card__top">
            <span class="explore-flow-card__number">0{{ index + 1 }}</span>
            <span class="explore-flow-card__icon"><component :is="flow.icon" /></span>
          </div>
          <h2>{{ flow.title }}</h2>
          <p>{{ flow.description }}</p>
          <div class="explore-flow-card__features">
            <span v-for="item in flow.children" :key="item.id">{{ item.title }}</span>
          </div>
          <div class="explore-flow-card__enter">
            进入流程
            <ArrowRightOutlined />
          </div>
        </button>
      </section>
    </main>
  </div>
</template>

<script setup name="DataExploreStation">
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, provide, reactive, ref, watch } from "vue";
import { onBeforeRouteLeave, routeLocationKey, useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import {
  ApartmentOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CheckOutlined,
  DatabaseOutlined,
  DownOutlined,
  HomeOutlined,
  MessageOutlined,
  NodeIndexOutlined,
  RightOutlined,
  ScanOutlined,
} from "@ant-design/icons-vue";
import useUserStore from "@/store/system/user";
import { startPlatformAgent, getPlatformAgentOperation, getPlatformAgentOperationPage, resumePlatformAgent, retryPlatformAgent, confirmPlatformAgentMetadata } from "@/api/ai/platformAgent";
import { encrypt } from "@/utils/aesEncrypt";
import { currentUser } from "@/api/tax/space/space";

const QualityTaskView = defineAsyncComponent(() => import("@/views/ast/quality/qualityTask/index.vue"));
const ProbeInstanceView = defineAsyncComponent(() => import("@/views/ast/quality/probeTaskInstance/index.vue"));
const ProbeReportView = defineAsyncComponent(() => import("@/views/ast/quality/probeTaskInstance/detail/index.vue"));
const MetadataView = defineAsyncComponent(() => import("@/views/meta/catalog/table/index.vue"));
const MetadataDetailView = defineAsyncComponent(() => import("@/views/meta/catalog/table/detail/index.vue"));
const StatisticsView = defineAsyncComponent(() => import("@/views/meta/catalog/statistics/index.vue"));
const DatasourceView = defineAsyncComponent(() => import("@/views/ast/datasource/index.vue"));
const DataSecurityView = defineAsyncComponent(() => import("@/views/ast/security/desensitizeRule/index.vue"));
const AssetView = defineAsyncComponent(() => import("@/views/ast/asset/index.vue"));
const AssetDetailView = defineAsyncComponent(() => import("@/views/col/asset/detail/index.vue"));
const AssetAuditView = defineAsyncComponent(() => import("@/views/ast/assetApply/index.vue"));
const DataQueryView = defineAsyncComponent(() => import("@/views/ast/dataQuery/index.vue"));
const OntologyView = defineAsyncComponent(() => import("@/views/ont/ontology/index.vue"));
const OntologyWorkspaceView = defineAsyncComponent(() => import("@/views/ont/workspace/index.vue"));
const OntFunctionView = defineAsyncComponent(() => import("@/views/ont/function/index.vue"));
const OntWebhookView = defineAsyncComponent(() => import("@/views/ont/webhook/index.vue"));
const AiAskView = defineAsyncComponent(() => import("@/views/ai/chat/index/index.vue"));
const AiSkillView = defineAsyncComponent(() => import("@/views/ai/skill/index.vue"));

const flows = [
  {
    id: "probe",
    title: "数据探查",
    eyebrow: "DISCOVER & PROFILE",
    description: "先理解数据的结构、规模和质量，为后续治理建立可信基础。",
    icon: ScanOutlined,
    color: "#536dfe",
    softColor: "#eef1ff",
    children: [
      { id: "datasource", title: "数据源管理", description: "维护数据源连接和认证配置，为元数据探查提供数据连接。", pagePath: "/ast/datasource", component: DatasourceView },
      { id: "quality-task", title: "探查任务", description: "配置并运行数据质量探查任务。", pagePath: "/ast/quality/qualityTask", component: QualityTaskView },
      { id: "metadata", title: "探查元数据", description: "浏览元数据探查得到的数据库、数据表和字段目录。", pagePath: "/meta/catalog/table", component: MetadataView },
      { id: "probe-log", title: "探查任务日志", description: "查看探查任务的执行状态、运行日志和历史结果。", pagePath: "/ast/quality/probeTaskInstance", component: ProbeInstanceView },
      { id: "statistics", title: "数据统计", description: "查看数据库、数据表及数据行数统计。", pagePath: "/meta/catalog/statistics", component: StatisticsView },
    ],
  },
  {
    id: "asset",
    title: "数据资产",
    eyebrow: "ORGANIZE & GOVERN",
    description: "将已探查的数据整理为可检索、可授权、可复用的数据资产。",
    icon: DatabaseOutlined,
    color: "#008f85",
    softColor: "#e6f8f5",
    children: [
      { id: "asset-list", title: "资产目录", description: "浏览和管理当前空间的数据资产。", pagePath: "/ast/asset", component: AssetView },
      { id: "asset-audit", title: "资产审核", description: "审核数据资产登记、变更和发布状态。", pagePath: "/ast/assetApply", component: AssetAuditView },
      { id: "data-security", title: "数据安全", description: "配置数据脱敏规则和安全控制，保护敏感数据使用过程。", pagePath: "/ast/security/desensitizeRule", component: DataSecurityView },
      { id: "data-query", title: "数据查询", description: "基于已授权资产进行数据检索。", pagePath: "/ast/dataQuery", component: DataQueryView },
    ],
  },
  {
    id: "ontology",
    title: "本体模型",
    eyebrow: "MODEL & CONNECT",
    description: "把数据资产映射为业务概念、关系和动作，形成可执行的语义网络。",
    icon: ApartmentOutlined,
    color: "#8754d8",
    softColor: "#f3edff",
    children: [
      { id: "ontology-list", title: "本体管理", description: "创建本体并进入可视化本体工作台。", pagePath: "/ont/ontology", component: OntologyView },
      { id: "ontology-concept", title: "概念管理", description: "在本体工作台中管理业务概念和主属性。", pagePath: "/ont/ontology", component: OntologyView },
      { id: "ontology-relation", title: "关系管理", description: "在本体工作台中管理概念之间的业务关系。", pagePath: "/ont/ontology", component: OntologyView },
      { id: "ontology-action", title: "动作管理", description: "在本体工作台中配置可执行的业务动作。", pagePath: "/ont/ontology", component: OntologyView },
      { id: "ontology-workflow", title: "动作编排", description: "在本体工作台中编排动作执行流程。", pagePath: "/ont/ontology", component: OntologyView },
      { id: "ontology-function", title: "函数管理", description: "管理本体动作和计算所使用的函数。", pagePath: "/ont/function", component: OntFunctionView },
      { id: "ontology-webhook", title: "Webhook", description: "配置本体动作执行成功后的回调地址和回调日志。", pagePath: "/ont/webhook", component: OntWebhookView },
    ],
  },
  {
    id: "ai",
    title: "数据智能体",
    eyebrow: "ASK & DECIDE",
    description: "让 AI 基于数据资产和本体语义理解问题、分析数据并辅助决策。",
    icon: MessageOutlined,
    color: "#e36f3d",
    softColor: "#fff0e9",
    children: [
      { id: "ai-ask", title: "决策智能体", description: "基于本体的数据查询、分析与决策建议。", component: AiAskView },
      { id: "ai-skill", title: "Skill 管理", description: "配置数据查询、分析和决策建议所需的知识能力。", component: AiSkillView },
    ],
  },
];

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const activeFlowId = ref("");
const activeSubId = ref("");
const embeddedPage = ref(null);
const embeddedPreviousPage = ref(null);
const spaceList = ref([]);
const spaceLoading = ref(false);
const agentIntent = ref("");
const agentPlanning = ref(false);
const agentPlanReady = ref(false);
const agentPlanStep = ref(-1);
const agentPlanOutput = ref([]);
const agentPlanDatabase = ref("待选择数据源");
const agentPlanItems = ref([]);
const agentOperationId = ref("");
const agentOperationStatus = ref("");
const agentOperationHistory = ref([]);
const agentRequiredFields = ref([]);
const agentConnection = ref({ databaseName: "", datasourceType: "PostgreSQL", ip: "", port: 5432, datasourceConfig: "" });
const agentOutputRef = ref();
let agentPlanTimer = null;
const AGENT_OPERATION_STORAGE_KEY = "dataMasterCurrentAgentOperationId";
const AGENT_POLL_INTERVAL = 5000;
const currentAgentSpaceId = () => userStore.spaceId || localStorage.getItem("dataMasterSpaceId") || null;
const agentOperationStorageKey = () => `${AGENT_OPERATION_STORAGE_KEY}:${currentAgentSpaceId() || userStore.spaceCode || "default"}`;
const agentPlanSteps = [
  "确认数据源",
  "探查元数据",
  "同步数据资产",
  "配置数据授权",
  "生成本体模型",
  "生成本体动作",
  "生成决策 Skill",
  "同步智能体能力",
];
const agentExamples = [
  "探查 datamaster_test 的数据结构",
  "同步订单数据资产并配置授权",
  "根据资产生成本体和决策 Skill",
];

function applyExploreSpace(space) {
  if (!space?.id) return false;
  userStore.spaceId = space.id;
  userStore.spaceCode = space.code || space.spaceCode || "";
  userStore.spaceName = space.name || space.spaceName || "";
  localStorage.setItem("dataMasterSpaceId", String(space.id));
  return true;
}

async function ensureExploreSpace() {
  if (spaceLoading.value) return;
  spaceLoading.value = true;
  try {
    const response = await currentUser();
    spaceList.value = response?.data || [];
    if (!spaceList.value.length) return;
    const storedId = userStore.spaceId || localStorage.getItem("dataMasterSpaceId");
    const selected = spaceList.value.find((space) => String(space.id) === String(storedId)) || spaceList.value[0];
    applyExploreSpace(selected);
  } catch (error) {
    // 空间获取失败不阻断流程页，用户仍可点击空间切换重试。
  } finally {
    spaceLoading.value = false;
  }
}

function handleSpaceSwitch({ key }) {
  const selected = spaceList.value.find((space) => String(space.id) === String(key));
  if (!selected || String(selected.id) === String(userStore.spaceId)) return;
  applyExploreSpace(selected);
  resetAgentPlan();
  restoreAgentOperation();
}

// 流程工作站内嵌业务页面时，给子页面提供当前工作站自己的路由上下文。
// 子页面仍可读取 query.id、path 等信息，但不会真的离开 /explore。
const stationRoute = reactive({
  fullPath: route.fullPath || "/explore",
  path: route.path || "/explore",
  query: route.query || {},
  hash: "",
  name: route.name || "DataExploreStation",
  params: route.params || {},
  matched: [],
  meta: { fullScreen: true },
  redirectedFrom: undefined,
});
provide(routeLocationKey, stationRoute);

function updateStationRoute(page) {
  const pagePath = page?.pagePath || page?.path || "/explore";
  const query = page?.query || {};
  const queryString = Object.entries(query)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value == null ? "" : value)}`)
    .join("&");
  stationRoute.path = pagePath;
  stationRoute.fullPath = queryString ? `${pagePath}?${queryString}` : pagePath;
  stationRoute.query = query;
  stationRoute.name = page?.routeName || page?.name || "DataExploreStation";
  stationRoute.params = page?.params || {};
  stationRoute.meta = { ...(page?.meta || {}), fullScreen: true };
}

function openEmbeddedPage({ path, query = {}, component, title, routeName, meta } = {}) {
  if (!path) return false;
  let resolvedComponent = component;
  if (!resolvedComponent && path === "/meta/catalog/table/detail") resolvedComponent = MetadataDetailView;
  if (!resolvedComponent && path === "/col/asset/detail") resolvedComponent = AssetDetailView;
  if (!resolvedComponent && path === "/ast/asset/detail") resolvedComponent = AssetDetailView;
  if (!resolvedComponent && path === "/ast/quality/probeTaskInstance/detail") resolvedComponent = ProbeReportView;
  if (!resolvedComponent && path.startsWith("/ont/workspace/")) resolvedComponent = OntologyWorkspaceView;
  if (!resolvedComponent) return false;
  const resolvedTitle = title || (
    path.includes("asset/detail") ? "资产详情"
      : path.includes("catalog/table/detail") ? "元数据详情"
        : path.includes("probeTaskInstance/detail") ? "探查质量报告"
          : path.startsWith("/ont/workspace/") ? "本体工作台" : "详情"
  );
  // 同一详情页切换 tab 时只更新当前页，不额外压入返回栈；跨页面打开时才保留上一页。
  if (!embeddedPage.value || embeddedPage.value.path !== path) {
    embeddedPreviousPage.value = embeddedPage.value || {
      id: activeSubFlow.value?.id,
      component: activeSubFlow.value?.component,
      pagePath: activeSubFlow.value?.pagePath || activeSubFlow.value?.id,
      query: {},
      title: activeSubFlow.value?.title,
    };
  }
  embeddedPage.value = {
    id: `embedded:${path}:${query.id || ""}:${Date.now()}`,
    component: resolvedComponent,
    pagePath: path,
    path,
    query,
    title: resolvedTitle,
    routeName,
    meta: meta || { title: resolvedTitle },
    params: path.match(/^\/ont\/workspace\/([^/]+)/)?.[1]
      ? { ontologyId: path.match(/^\/ont\/workspace\/([^/]+)/)[1] }
      : {},
  };
  updateStationRoute(embeddedPage.value);
  return true;
}

function closeEmbeddedPage() {
  if (!embeddedPage.value) return false;
  embeddedPage.value = embeddedPreviousPage.value;
  embeddedPreviousPage.value = null;
  updateStationRoute(embeddedPage.value || activeSubFlow.value || { path: "/explore" });
  return true;
}

provide("spaceWorkstationNavigation", {
  openPage: openEmbeddedPage,
  back: closeEmbeddedPage,
});

// 流程页中的菜单切换完全是页内状态，不允许旧模块把页面导航到 /space。
// /space 仍可从首页正常进入，因此只在当前流程页离开时拦截这条错误导航。
onBeforeRouteLeave((to) => {
  if (to.path === "/space") return false;
  return true;
});

const spaceName = computed(() => userStore.spaceName || "");
const activeFlow = computed(() => flows.find((flow) => flow.id === activeFlowId.value));
const activeFlowIndex = computed(() => flows.findIndex((flow) => flow.id === activeFlowId.value));
const activeSubFlow = computed(() => activeFlow.value?.children.find((item) => item.id === activeSubId.value));
const embeddedPageDescription = computed(() => {
  if (embeddedPage.value?.title === "本体工作台") return "管理本体概念、属性、关系、动作编排和对象实例。";
  if (embeddedPage.value?.title === "元数据详情") return "查看数据表基本信息、字段结构和探查历史。";
  if (embeddedPage.value?.title === "资产详情") return "查看资产字段、预览、质量结果和血缘信息。";
  if (embeddedPage.value?.title === "探查质量报告") return "查看本次数据探查的质量评分、问题数据和执行结果。";
  return "查看当前流程步骤的详细信息。";
});
const nextFlow = computed(() => flows[activeFlowIndex.value + 1]);
const agentPlanProgress = computed(() => {
  if (agentOperationId.value) return agentOperationProgress.value;
  if (agentPlanStep.value < 0) return 0;
  return Math.min(100, Math.round(((agentPlanStep.value + 1) / agentPlanSteps.length) * 100));
});
const agentOperationProgress = computed(() => {
  if (!agentOperationId.value) return 0;
  return Math.max(0, Math.min(100, Number(agentPlanStep.value < 0 ? 0 : agentPlanStep.value)));
});
const agentOperationStatusLabel = computed(() => ({
  RUNNING: "自动执行中",
  NEED_INPUT: "等待补充",
  NEED_CONFIRMATION: "等待确认",
  SUCCEEDED: "已完成",
  FAILED: "失败",
  INTERRUPTED: "已中断，可继续",
  CANCELED: "已取消",
}[agentOperationStatus.value] || "准备中"));

function operationStatusLabel(status) {
  return ({
    RUNNING: "执行中", NEED_INPUT: "等待补充", NEED_CONFIRMATION: "等待确认",
    SUCCEEDED: "已完成", FAILED: "失败", INTERRUPTED: "已中断", CANCELED: "已取消",
  }[status] || status || "准备中");
}

function upsertOperationHistory(operation) {
  if (!operation?.id) return;
  const existing = agentOperationHistory.value.filter((item) => item.id !== operation.id);
  agentOperationHistory.value = [operation, ...existing].slice(0, 20);
}

async function selectAgentOperation(operation) {
  applyAgentOperation(operation);
  if (!operation?.id) return;
  try {
    const res = await getPlatformAgentOperation(operation.id);
    applyAgentOperation(res?.data || res);
  } catch (error) {
    // 列表中的历史状态仍可展示，详情刷新失败不阻断页面操作。
  }
}

function selectFlow(flowId) {
  const flow = flows.find((item) => item.id === flowId);
  if (!flow) return;
  embeddedPage.value = null;
  embeddedPreviousPage.value = null;
  activeFlowId.value = flow.id;
  activeSubId.value = flow.children[0]?.id || "";
  updateStationRoute(flow.children[0] || { path: "/explore", name: "DataExploreStation", meta: { fullScreen: true } });
}

function selectSubFlow(subId) {
  embeddedPage.value = null;
  embeddedPreviousPage.value = null;
  activeSubId.value = subId;
  updateStationRoute(activeSubFlow.value || { path: "/explore", name: "DataExploreStation", meta: { fullScreen: true } });
}

function goFlowHome() {
  embeddedPage.value = null;
  embeddedPreviousPage.value = null;
  activeFlowId.value = "";
  activeSubId.value = "";
  updateStationRoute({ path: "/explore", name: "DataExploreStation", meta: { fullScreen: true } });
}

function goSystemHome() {
  router.push("/index");
}

function startAgentConversation() {
  const prompt = agentIntent.value.trim();
  if (!prompt) {
    message.warning("请先描述要完成的数据工作");
    return;
  }
  if (agentPlanTimer) clearTimeout(agentPlanTimer);
  agentPlanning.value = true;
  agentPlanReady.value = true;
  agentPlanStep.value = 0;
  agentPlanOutput.value = [];
  const parsed = parseAgentConnection(prompt);
  agentPlanDatabase.value = parsed.databaseName || extractDatabaseName(prompt);
  agentConnection.value = {
    databaseName: parsed.databaseName === "待选择数据源" ? "" : parsed.databaseName,
    datasourceType: parsed.datasourceType || "PostgreSQL",
    ip: parsed.ip || "",
    port: parsed.port || 5432,
    datasourceConfig: parsed.datasourceConfig || "",
  };
  agentPlanItems.value = buildAgentPlanItems();
  agentOperationStatus.value = "RUNNING";
  const payload = {
    goal: prompt,
    datasourceName: agentPlanDatabase.value === "待选择数据源" ? undefined : agentPlanDatabase.value,
    databaseName: agentPlanDatabase.value === "待选择数据源" ? undefined : agentPlanDatabase.value,
    datasourceType: parsed.datasourceType,
    ip: parsed.ip,
    port: parsed.port,
    schemaName: parsed.schemaName,
    datasourceConfig: parsed.datasourceConfig,
    // 数据智能体从空间工作站启动时必须把空间上下文传给后端，
    // 否则数据源、探查任务和元数据结果会落成“未分配空间”。
    spaceId: currentAgentSpaceId(),
    spaceCode: userStore.spaceCode || '',
  };
  startPlatformAgent(payload).then((res) => {
    applyAgentOperation(res?.data || res);
  }).catch((error) => {
    agentPlanning.value = false;
    agentOperationStatus.value = "FAILED";
    agentPlanOutput.value = [...agentPlanOutput.value, `> 启动失败：${error?.message || "无法连接平台数据智能体"}`];
  });
}

function resumeAgentOperation() {
  if (!agentOperationId.value) return;
  agentPlanning.value = true;
  resumePlatformAgent(agentOperationId.value, {
    goal: agentIntent.value.trim(),
    datasourceName: agentConnection.value.databaseName || agentPlanDatabase.value,
    databaseName: agentConnection.value.databaseName || agentPlanDatabase.value,
    datasourceType: agentConnection.value.datasourceType,
    ip: agentConnection.value.ip,
    port: agentConnection.value.port,
    datasourceConfig: agentConnection.value.datasourceConfig,
  }).then((res) => applyAgentOperation(res?.data || res)).catch((error) => {
    agentPlanning.value = false;
    agentOperationStatus.value = "FAILED";
    agentPlanOutput.value = [...agentPlanOutput.value, `> 继续执行失败：${error?.message || "请求失败"}`];
  });

}

async function retryAgentOperation() {
  if (!agentOperationId.value) return;
  try {
    const latestResponse = await getPlatformAgentOperation(agentOperationId.value);
    const latest = latestResponse?.data || latestResponse;
    applyAgentOperation(latest);
    if (latest?.status !== "FAILED" && latest?.status !== "INTERRUPTED") {
      message.warning(`当前流程状态为“${operationStatusLabel(latest?.status)}”，不能从失败步骤重试`);
      return;
    }
  } catch (error) {
    message.error("无法读取最新流程状态，请刷新后重试");
    return;
  }
  agentPlanning.value = true;
  retryPlatformAgent(agentOperationId.value)
    .then((res) => applyAgentOperation(res?.data || res))
    .catch((error) => {
      agentPlanning.value = false;
      agentOperationStatus.value = "FAILED";
      agentPlanOutput.value = [...agentPlanOutput.value, `> 重试失败：${error?.message || "请求失败"}`];
    });
}

function applyAgentOperation(operation) {
  if (!operation) return;
  upsertOperationHistory(operation);
  agentPlanReady.value = true;
  agentPlanning.value = operation.status === "RUNNING";
  agentOperationId.value = operation.id || agentOperationId.value;
  if (agentOperationId.value) localStorage.setItem(agentOperationStorageKey(), agentOperationId.value);
  agentOperationStatus.value = operation.status || agentOperationStatus.value;
  if (operation.goal && !agentIntent.value) agentIntent.value = operation.goal;
  agentPlanDatabase.value = operation.datasourceName || agentPlanDatabase.value;
  if (operation.databaseName || operation.datasourceType || operation.ip || operation.port || operation.datasourceConfig) {
    agentConnection.value = {
      databaseName: operation.databaseName || agentConnection.value.databaseName || "",
      datasourceType: operation.datasourceType || agentConnection.value.datasourceType || "PostgreSQL",
      ip: operation.ip || agentConnection.value.ip || "",
      port: operation.port || agentConnection.value.port || 5432,
      datasourceConfig: operation.datasourceConfig || agentConnection.value.datasourceConfig || "",
    };
  }
  agentRequiredFields.value = operation.requiredFields || [];
  if (Array.isArray(operation.output) && operation.output.length) agentPlanOutput.value = operation.output;
  if (Array.isArray(operation.steps) && operation.steps.length) {
    agentPlanItems.value = operation.steps.map((item) => ({
      title: item.title,
      description: stepDescription(item.title),
      status: stepStatusLabel(item.status),
    }));
    agentPlanStep.value = Number(operation.progress || 0);
  }
  if (operation.message && (!agentPlanOutput.value.length || agentPlanOutput.value[agentPlanOutput.value.length - 1] !== `> ${operation.message}`)) {
    agentPlanOutput.value = [...agentPlanOutput.value, `> ${operation.message}`];
  }
  if (operation.status === "RUNNING") {
    if (agentPlanTimer) clearTimeout(agentPlanTimer);
    agentPlanTimer = setTimeout(pollAgentOperation, AGENT_POLL_INTERVAL);
  } else {
    agentPlanning.value = false;
  }
}

async function confirmMetadataTask() {
  if (!agentOperationId.value) return;
  try {
    const latestResponse = await getPlatformAgentOperation(agentOperationId.value);
    const latest = latestResponse?.data || latestResponse;
    applyAgentOperation(latest);
    if (latest?.status !== "NEED_CONFIRMATION") {
      message.warning(`当前流程状态为“${operationStatusLabel(latest?.status)}”，无需重复确认`);
      return;
    }
  } catch (error) {
    message.error("无法读取最新流程状态，请刷新后重试");
    return;
  }
  agentPlanning.value = true;
  confirmPlatformAgentMetadata(agentOperationId.value)
    .then((res) => applyAgentOperation(res?.data || res))
    .catch((error) => {
      agentPlanning.value = false;
      agentOperationStatus.value = "FAILED";
      agentPlanOutput.value = [...agentPlanOutput.value, `> 确认发布失败：${error?.message || "请求失败"}`];
    });
}

function pollAgentOperation() {
  if (!agentOperationId.value || agentOperationStatus.value !== "RUNNING") return;
  getPlatformAgentOperation(agentOperationId.value)
    .then((res) => applyAgentOperation(res?.data || res))
    .catch((error) => {
      agentPlanning.value = false;
      agentOperationStatus.value = "FAILED";
      agentPlanOutput.value = [...agentPlanOutput.value, `> 查询流程状态失败：${error?.message || "请求失败"}`];
    });
}

function stepDescription(title) {
  return buildAgentPlanItems().find((item) => item.title === title)?.description || "平台数据智能体正在处理该步骤。";
}

function stepStatusLabel(status) {
  return { DONE: "已完成", RUNNING: "执行中", PENDING: "待执行" }[status] || status || "待执行";
}

function agentPlanItemClass(item) {
  if (item?.status === "已完成") return "is-done";
  if (item?.status === "执行中") return "is-active";
  return "is-pending";
}

function extractDatabaseName(prompt) {
  const named = prompt.match(/数据库\s*名称\s*[:：]?\s*([A-Za-z0-9_.-]+?)(?=schema|连接|密码|开始|\s|$)/i)
    || prompt.match(/(?:数据源|接入|连接)\s*[:：]?\s*([A-Za-z0-9_.-]+)/i);
  if (named?.[1]) return named[1];
  const datamaster = prompt.match(/\b(datamaster_[A-Za-z0-9_-]+)\b/i);
  return datamaster?.[1] || "待选择数据源";
}

function parseAgentConnection(prompt) {
  const text = String(prompt || "");
  const databaseName = extractDatabaseName(text);
  const ip = text.match(/(?:数据库)?(?:地址|host|ip)\s*[:：=]?\s*([A-Za-z0-9_.-]+)/i)?.[1]
    || text.match(/\b((?:\d{1,3}\.){3}\d{1,3})\b/)?.[1] || "";
  const port = Number(text.match(/(?:端口|port)\s*[:：=]?\s*(\d{2,6})/i)?.[1] || 0)
    || Number(text.match(/\b(?:\d{1,3}\.){3}\d{1,3}\s*[，,;；:]\s*(\d{2,6})/)?.[1] || 0)
    || undefined;
  const schemaName = text.match(/schema\s*[:：]?\s*([A-Za-z0-9_.-]+)/i)?.[1] || "";
  const username = text.match(/(?:连接账号|用户名|账号|owner|onwer|user|username)\s*[:：=]?\s*([A-Za-z0-9_.-]+)/i)?.[1] || "";
  const password = text.match(/(?:密码|password|pwd)\s*[:：=]?\s*([^\s，,；;。)）]+)/i)?.[1] || "";
  const typeToken = text.match(/\b(postgresql|postgres|mysql|oracle|sqlserver|sql\s*server)\b/i)?.[1] || "";
  // 数据库类型要使用 DataMaster 的实际编码（展示名 PostgreSQL，而不是 JDBC/DS 的 POSTGRESQL）。
  const normalizedType = /mysql/i.test(typeToken) ? "MySql"
    : /oracle/i.test(typeToken) ? "Oracle11"
      : /sql/i.test(typeToken) ? "SQL_Server" : "PostgreSQL";
  let datasourceConfig;
  if (username || password || databaseName !== "待选择数据源") {
    const config = {
      username: username || undefined,
      password: password ? encrypt(password) : undefined,
      dbname: databaseName !== "待选择数据源" ? databaseName : undefined,
      schema: schemaName || undefined,
    };
    datasourceConfig = JSON.stringify(config);
  }
  return { databaseName, ip, port, schemaName, datasourceType: normalizedType, datasourceConfig };
}

function buildAgentPlanItems() {
  return [
    { title: "确认数据源", description: "校验数据库连接信息和当前空间可见范围。", status: "待执行" },
    { title: "探查元数据", description: "采集数据库、表、字段和基础统计信息。", status: "待执行" },
    { title: "同步数据资产", description: "将可用表和字段登记为当前空间资产。", status: "待执行" },
    { title: "配置数据授权", description: "确认空间、表和字段级访问权限。", status: "待执行" },
    { title: "生成本体模型", description: "根据资产语义生成概念、属性和关系草案。", status: "待执行" },
    { title: "生成本体动作", description: "基于业务目标生成可审核的动作配置。", status: "待执行" },
    { title: "生成决策 Skill", description: "整理本体查询、分析和决策规则。", status: "待执行" },
    { title: "同步智能体能力", description: "同步知识和数据源，供决策智能体使用。", status: "待执行" },
  ];
}

function resetAgentPlan() {
  if (agentPlanTimer) clearTimeout(agentPlanTimer);
  agentPlanning.value = false;
  agentPlanReady.value = false;
  agentPlanStep.value = -1;
  agentPlanOutput.value = [];
  agentPlanItems.value = [];
  agentOperationId.value = "";
  agentOperationStatus.value = "";
  agentRequiredFields.value = [];
  localStorage.removeItem(agentOperationStorageKey());
}

function agentStepStatus(index) {
  if (agentOperationStatus.value === "SUCCEEDED") return "is-done";
  if (agentOperationId.value) {
    const item = agentPlanItems.value[index];
    if (item?.status === "已完成") return "is-done";
    if (item?.status === "执行中") return "is-active";
    return "is-pending";
  }
  if (agentPlanReady.value) return "is-done";
  if (index < agentPlanStep.value) return "is-done";
  if (index === agentPlanStep.value) return "is-active";
  return "is-pending";
}

async function restoreAgentOperation() {
  try {
    const response = await getPlatformAgentOperationPage({
      spaceId: currentAgentSpaceId() || undefined,
      spaceCode: userStore.spaceCode || undefined,
      limit: 20,
    });
    const operations = response?.data || response || [];
    if (Array.isArray(operations) && operations.length) {
      agentOperationHistory.value = operations;
    }
  } catch (error) {
    // 历史记录加载失败不影响正常开始新流程。
  }
}

// 空间信息可能在页面挂载后才由顶部空间选择器恢复，恢复完成后再加载一次该空间的流程记录。
watch(() => [userStore.spaceId, userStore.spaceCode], ([spaceId, spaceCode], oldValue) => {
  if ((spaceId || spaceCode) && (!oldValue || spaceId !== oldValue[0] || spaceCode !== oldValue[1])) {
    restoreAgentOperation();
  }
});

onMounted(() => {
  ensureExploreSpace().finally(() => restoreAgentOperation());
});

onBeforeUnmount(() => {
  if (agentPlanTimer) clearTimeout(agentPlanTimer);
});
</script>

<style lang="scss" scoped>
.explore-station {
  --station-bg: #f4f7fb;
  width: calc(100% + 32px);
  min-height: 100vh;
  margin: -16px;
  color: #15213b;
  background:
    radial-gradient(circle at 78% -8%, rgba(91, 110, 246, 0.12), transparent 28%),
    var(--station-bg);
}

.explore-station__header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: minmax(210px, 0.8fr) minmax(560px, 2.2fr) minmax(210px, 0.8fr);
  align-items: center;
  gap: 20px;
  min-height: 76px;
  padding: 0 28px;
  color: #fff;
  background: rgba(14, 23, 45, 0.98);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 30px rgba(14, 23, 45, 0.18);
}

.explore-station__brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  color: inherit;
  text-align: left;
  background: none;
  border: 0;
  cursor: pointer;

  strong,
  small { display: block; }
  strong { font-size: 14px; letter-spacing: 0.02em; }
  small { margin-top: 2px; color: #91a0bc; font-size: 11px; }
}

.explore-station__brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: #fff;
  font-size: 19px;
  background: linear-gradient(135deg, #5b6df6, #8d5ce5);
  border-radius: 11px;
  box-shadow: 0 8px 22px rgba(91, 109, 246, 0.35);
}

.explore-station__primary {
  display: flex;
  align-items: stretch;
  justify-content: center;
  height: 76px;
}

.explore-station__primary-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 128px;
  padding: 0 18px;
  color: #98a6c0;
  font-size: 13px;
  background: transparent;
  border: 0;
  cursor: pointer;
  transition: 0.2s ease;

  &::after {
    position: absolute;
    right: 18px;
    bottom: 0;
    left: 18px;
    height: 3px;
    background: #7182ff;
    border-radius: 3px 3px 0 0;
    content: "";
    opacity: 0;
    transform: scaleX(0.4);
    transition: 0.2s ease;
  }
  &:hover,
  &.is-active { color: #fff; background: rgba(255, 255, 255, 0.045); }
  &.is-active::after { opacity: 1; transform: scaleX(1); }
}

.explore-station__step { color: #64718b; font-size: 10px; font-weight: 700; }
.explore-station__header-actions { display: flex; align-items: center; justify-content: flex-end; gap: 8px; }
.explore-station__space-switch { display: inline-flex; align-items: center; gap: 6px; }

.explore-station__secondary {
  position: sticky;
  top: 76px;
  z-index: 18;
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(520px, 2fr) minmax(220px, 0.8fr);
  align-items: center;
  gap: 18px;
  min-height: 58px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.97);
  border-bottom: 1px solid #e1e7f0;
  box-shadow: 0 5px 18px rgba(30, 48, 86, 0.06);
}

.explore-station__breadcrumb { display: flex; align-items: center; gap: 7px; color: #8b96a9; font-size: 12px; }
.explore-station__breadcrumb button { padding: 0; color: #5368e5; background: none; border: 0; cursor: pointer; }
.explore-station__breadcrumb strong { color: #273651; }

.explore-station__secondary-nav { display: flex; align-items: center; justify-content: center; gap: 6px; }
.explore-station__secondary-nav button {
  padding: 8px 16px;
  color: #67748b;
  font-size: 12px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 9px;
  cursor: pointer;
}
.explore-station__secondary-nav button:hover { color: #485fd9; background: #f3f5ff; }
.explore-station__secondary-nav button.is-active { color: #4058d4; font-weight: 600; background: #edf0ff; border-color: #dce2ff; }
.explore-station__secondary > .ant-btn { justify-self: end; }

.explore-station__module-layout {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  min-height: calc(100vh - 76px);
}

.explore-station__sidebar {
  position: sticky;
  top: 76px;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 76px);
  padding: 22px 16px 18px;
  overflow-y: auto;
  background: #fff;
  border-right: 1px solid #e1e7f0;
}

.explore-station__sidebar-head {
  padding: 0 8px 18px;
  border-bottom: 1px solid #edf0f5;

  button {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0;
    color: #5066dc;
    font-size: 12px;
    background: none;
    border: 0;
    cursor: pointer;
  }

  span,
  strong { display: block; }
  span { margin-top: 20px; color: #8995a9; font-size: 10px; letter-spacing: 0.08em; }
  strong { margin-top: 5px; color: #202d47; font-size: 18px; }
}

.explore-station__flow-menu,
.explore-station__sidebar .explore-station__secondary-nav {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  gap: 4px;
}

.explore-station__flow-menu { margin-top: 18px; }
.explore-station__flow-menu button {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 9px;
  color: #748097;
  font-size: 13px;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: 0.2s ease;

  &:hover { color: #435bd3; background: #f5f7ff; }
  &.is-active { color: #304bc5; font-weight: 650; background: #edf0ff; border-color: #dfe4ff; }
}

.explore-station__flow-menu-index { width: 21px; color: #a3aec0; font-size: 10px; font-weight: 700; }
.explore-station__flow-menu-icon { display: grid; width: 27px; height: 27px; place-items: center; color: currentColor; background: rgba(79, 101, 221, 0.1); border-radius: 8px; }
.explore-station__flow-menu-check { margin-left: auto; color: #35a77e; font-size: 12px; }
.explore-station__sidebar-divider { height: 1px; margin: 18px 8px 14px; background: #edf0f5; }
.explore-station__sidebar-label { margin: 0 8px 7px; color: #98a2b3; font-size: 10px; font-weight: 700; letter-spacing: 0.12em; }
.explore-station__sidebar .explore-station__secondary-nav button { padding: 10px 12px; color: #68758d; font-size: 12px; text-align: left; background: transparent; border: 1px solid transparent; border-radius: 9px; cursor: pointer; }
.explore-station__sidebar .explore-station__secondary-nav button:hover { color: #425ad2; background: #f5f7ff; }
.explore-station__sidebar .explore-station__secondary-nav button.is-active { color: #4058d4; font-weight: 600; background: #f0f2ff; border-color: #e0e5ff; }
.explore-station__sidebar-footer { margin-top: auto; padding: 20px 4px 0; }

.explore-station__module-layout .explore-station__workspace { min-width: 0; }

.explore-station__workspace { padding: 22px 28px 30px; }
.explore-station__workspace-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 16px; }
.explore-station__workspace-head span { color: #6f7d97; font-size: 10px; font-weight: 700; letter-spacing: 0.14em; }
.explore-station__workspace-head h1 { margin: 4px 0 2px; color: #182540; font-size: 24px; }
.explore-station__workspace-head p { margin: 0; color: #78849a; font-size: 13px; }
.explore-station__progress { min-width: 110px; padding: 10px 14px; text-align: right; background: #fff; border: 1px solid #e2e8f1; border-radius: 12px; }
.explore-station__progress span,
.explore-station__progress strong { display: block; }
.explore-station__progress strong { margin-top: 2px; color: #4058d4; font-size: 18px; }
.explore-station__module { min-height: calc(100vh - 228px); overflow: hidden; background: #fff; border: 1px solid #e0e7f0; border-radius: 16px; box-shadow: 0 12px 35px rgba(28, 45, 81, 0.07); }
.explore-station__module :deep(.app-container) { min-height: calc(100vh - 230px); }
.explore-station__module :deep(.ask-data-page) { height: calc(100vh - 230px); min-height: 560px; border-radius: 16px; }

.explore-home { min-height: calc(100vh - 76px); padding: 32px 32px 40px; }
.explore-home__agent { display: grid; grid-template: auto auto auto / minmax(0, 1fr) 300px; grid-template-areas: "copy ." "dialog history" "prompts ."; column-gap: 24px; width: 100%; margin: 0 0 30px; padding: 30px 34px 24px; border: 1px solid #e4e9f5; border-radius: 24px; background: linear-gradient(135deg, #ffffff 0%, #f7f9ff 62%, #fff8f4 100%); box-shadow: 0 18px 50px rgba(51, 74, 133, 0.1); }
.explore-home__agent-copy { grid-area: copy; max-width: 720px; }
.explore-home__agent-eyebrow { color: #e36f3d; font-size: 12px; font-weight: 700; letter-spacing: 0.16em; }
.explore-home .explore-home__agent-copy h1 { margin: 10px 0 8px; color: #24324d; font-size: 26px; line-height: 1.35; letter-spacing: -0.01em; }
.explore-home__agent-copy p { margin: 0; color: #64708a; font-size: 15px; line-height: 1.7; }
.explore-home__agent-dialog { grid-area: dialog; display: flex; min-height: 220px; flex-direction: column; margin-top: 22px; padding: 12px; border: 1px solid #e7ebf5; border-radius: 16px; background: #fff; }
.explore-home__agent-dialog :deep(.ant-input) { border: 0; box-shadow: none; resize: none; color: #1f2c48; font-size: 15px; }
.explore-home__agent-actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: auto; padding: 10px 4px 0; color: #8a94aa; font-size: 12px; }
.explore-home__agent-counter { margin-left: 4px; color: #a3adbd; font-style: normal; }
.explore-home__agent-progress { margin-top: 18px; padding: 16px; border: 1px solid #e7ebf5; border-radius: 14px; background: #f9fbff; }
.explore-home__agent-progress-head { display: flex; align-items: center; justify-content: space-between; color: #53617a; font-size: 13px; }
.explore-home__agent-progress-head strong { color: #4058d4; font-size: 14px; }
.explore-home__agent-progress-bar { height: 5px; margin-top: 10px; overflow: hidden; border-radius: 99px; background: #e5eaf5; }
.explore-home__agent-progress-bar i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #536dfe, #e36f3d); transition: width 0.45s ease; }
.explore-home__agent-steps { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; margin-top: 16px; }
.explore-home__agent-step { display: flex; align-items: center; gap: 7px; min-width: 0; color: #a0a9bb; font-size: 11px; line-height: 1.4; }
.explore-home__agent-step.is-active { color: #4058d4; font-weight: 600; }
.explore-home__agent-step.is-done { padding: 5px 8px; color: #087c68; font-weight: 700; background: #e8faf4; border-radius: 999px; box-shadow: 0 4px 12px rgba(22, 136, 121, 0.12); }
.explore-home__agent-step-dot { display: grid; width: 20px; height: 20px; flex: 0 0 20px; place-items: center; border: 1px solid #d6ddea; border-radius: 50%; color: #fff; font-size: 11px; }
.explore-home__agent-step.is-active .explore-home__agent-step-dot { border-color: #536dfe; background: #536dfe; }
.explore-home__agent-step.is-done .explore-home__agent-step-dot { border-color: #168879; background: #168879; }
.explore-home__agent-step-dot i { width: 7px; height: 7px; border: 2px solid rgba(255, 255, 255, 0.85); border-top-color: transparent; border-radius: 50%; animation: agent-spin 0.8s linear infinite; }
.explore-home__agent-output { max-height: 92px; margin-top: 14px; padding: 10px 12px; overflow: auto; border-radius: 9px; color: #66738b; background: #fff; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 11px; line-height: 1.8; }
.explore-home__agent-plan { margin-top: 16px; padding-top: 16px; border-top: 1px solid #e6ebf4; }
.explore-home__agent-plan-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.explore-home__agent-plan-head span, .explore-home__agent-plan-head strong { display: block; }
.explore-home__agent-plan-head span { color: #6e7b93; font-size: 11px; }
.explore-home__agent-plan-head strong { margin-top: 3px; color: #22304b; font-size: 16px; }
.explore-home__agent-plan-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 14px; }
.explore-home__agent-plan-item { display: grid; grid-template-columns: 28px minmax(0, 1fr) auto; align-items: center; gap: 8px; padding: 9px 10px; border: 1px solid #edf0f6; border-radius: 10px; background: #fff; }
.explore-home__agent-plan-item { transition: border-color 0.25s ease, background 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease; }
.explore-home__agent-plan-item.is-active { border-color: #8fa0ff; background: linear-gradient(135deg, #f4f6ff, #ffffff); box-shadow: 0 7px 18px rgba(83, 109, 254, 0.14); transform: translateY(-1px); }
.explore-home__agent-plan-item.is-done { border-color: #77d8c2; background: linear-gradient(135deg, #eafbf6, #f8fffc); box-shadow: 0 7px 18px rgba(22, 136, 121, 0.14); }
.explore-home__agent-plan-item > span { color: #9aa5b9; font-size: 10px; font-weight: 700; }
.explore-home__agent-plan-item.is-active > span { color: #536dfe; }
.explore-home__agent-plan-item.is-done > span { display: grid; width: 23px; height: 23px; place-items: center; color: #fff; background: #168879; border-radius: 50%; font-size: 12px; }
.explore-home__agent-plan-item strong, .explore-home__agent-plan-item small { display: block; }
.explore-home__agent-plan-item strong { color: #34415d; font-size: 12px; }
.explore-home__agent-plan-item small { margin-top: 3px; overflow: hidden; color: #8a94a8; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.explore-home__agent-plan-item em { color: #a2adbf; font-size: 10px; font-style: normal; }
.explore-home__agent-plan-item.is-active em { color: #4058d4; font-weight: 700; }
.explore-home__agent-plan-item.is-done strong, .explore-home__agent-plan-item.is-done em { color: #087c68; font-weight: 700; }
.explore-home__agent-plan-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
.explore-home__agent-history { grid-area: history; align-self: stretch; min-height: 0; max-height: none; margin-top: 22px; padding: 14px 16px; overflow: auto; border: 1px solid #e7ebf5; border-radius: 14px; background: #fff; }
.explore-home__agent-history-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 9px; color: #263653; }
.explore-home__agent-history-head span { color: #8b96a9; font-size: 12px; }
.explore-home__agent-history-empty { padding: 14px 10px 4px; color: #9aa5b8; font-size: 13px; }
.explore-home__agent-history-item { display: grid; grid-template-columns: minmax(120px, .8fr) minmax(180px, 1.8fr) auto; align-items: center; width: 100%; gap: 12px; padding: 9px 10px; border: 0; border-top: 1px solid #f0f2f7; color: #33415c; background: transparent; text-align: left; cursor: pointer; }
.explore-home__agent-history-item:first-of-type { border-top: 0; }
.explore-home__agent-history-item small { overflow: hidden; color: #7f8ba1; text-overflow: ellipsis; white-space: nowrap; }
.explore-home__agent-history-item em { color: #536dfe; font-size: 12px; font-style: normal; }
.explore-home__agent-history-item.is-current { border-radius: 8px; background: #f4f6ff; }
.explore-home__agent-connection { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; padding: 12px; margin: 12px 0; background: #f7f9fc; border: 1px solid #e1e7f0; border-radius: 10px; }
.explore-home__agent-connection :deep(.ant-input-number), .explore-home__agent-connection :deep(.ant-input), .explore-home__agent-connection :deep(.ant-input-affix-wrapper) { width: 100%; }
.explore-home__agent-connection :deep(.ant-input-textarea), .explore-home__agent-connection :deep(.ant-btn) { grid-column: 1 / -1; }
.explore-home__agent-prompts { grid-area: prompts; align-self: start; display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
.explore-home__agent-prompts button { padding: 6px 12px; border: 1px solid #e2e7f2; border-radius: 999px; color: #58657e; background: #fff; cursor: pointer; transition: all 0.2s ease; }
.explore-home__agent-prompts button:hover { border-color: #e36f3d; color: #d35d2e; background: #fff8f4; }
@keyframes agent-spin { to { transform: rotate(360deg); } }
.explore-home__hero { position: relative; display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr); align-items: center; min-height: 310px; overflow: hidden; padding: 42px 56px; color: #fff; background: linear-gradient(125deg, #14213d 0%, #202f58 58%, #3f397c 100%); border-radius: 24px; box-shadow: 0 24px 60px rgba(20, 33, 61, 0.2); }
.explore-home__copy { position: relative; z-index: 2; max-width: 720px; }
.explore-home__eyebrow { display: flex; align-items: center; gap: 8px; color: #adb9d4; font-size: 10px; font-weight: 700; letter-spacing: 0.16em; }
.explore-home__eyebrow i { width: 7px; height: 7px; background: #7b8cff; border-radius: 50%; box-shadow: 0 0 14px #7b8cff; }
.explore-home h1 { margin: 14px 0 12px; color: #fff; font-size: clamp(34px, 4vw, 54px); line-height: 1.08; letter-spacing: -0.04em; }
.explore-home__copy p { max-width: 670px; margin: 0 0 25px; color: #c0cae0; font-size: 14px; line-height: 1.8; }
.explore-home__visual { position: relative; min-height: 250px; }
.explore-home__orbit { position: absolute; top: 50%; left: 50%; border: 1px solid rgba(145, 159, 255, 0.32); border-radius: 50%; transform: translate(-50%, -50%); }
.explore-home__orbit--outer { width: 280px; height: 280px; }
.explore-home__orbit--inner { width: 175px; height: 175px; border-style: dashed; }
.explore-home__core { position: absolute; top: 50%; left: 50%; display: grid; width: 94px; height: 94px; place-items: center; color: #fff; font-size: 26px; background: linear-gradient(145deg, #687bff, #925ee5); border: 8px solid rgba(255, 255, 255, 0.08); border-radius: 28px; box-shadow: 0 18px 50px rgba(91, 109, 246, 0.42); transform: translate(-50%, -50%) rotate(45deg); }
.explore-home__core > * { transform: rotate(-45deg); }
.explore-home__core span { margin-top: -24px; font-size: 11px; font-weight: 700; }
.explore-home__node { position: absolute; display: grid; width: 44px; height: 44px; place-items: center; color: #d5dcff; font-size: 18px; background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(255, 255, 255, 0.13); border-radius: 14px; backdrop-filter: blur(8px); }
.explore-home__node--one { top: 6px; left: 50%; }
.explore-home__node--two { right: 7%; bottom: 36px; }
.explore-home__node--three { bottom: 15px; left: 12%; }

.explore-home__flow { position: relative; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-top: 22px; }
.explore-home__flow::before { position: absolute; top: 54px; right: 8%; left: 8%; height: 1px; background: linear-gradient(90deg, transparent, #ccd5e5 12%, #ccd5e5 88%, transparent); content: ""; }
.explore-flow-card { position: relative; z-index: 1; min-height: 290px; padding: 22px; text-align: left; background: #fff; border: 1px solid #e0e7f0; border-radius: 18px; box-shadow: 0 10px 30px rgba(29, 47, 84, 0.06); cursor: pointer; transition: 0.25s ease; }
.explore-flow-card:hover { border-color: var(--flow-color); box-shadow: 0 18px 42px rgba(29, 47, 84, 0.12); transform: translateY(-5px); }
.explore-flow-card__top { display: flex; align-items: center; justify-content: space-between; }
.explore-flow-card__number { color: #a3aec0; font-size: 11px; font-weight: 700; letter-spacing: 0.12em; }
.explore-flow-card__icon { display: grid; width: 48px; height: 48px; place-items: center; color: var(--flow-color); font-size: 22px; background: var(--flow-soft); border-radius: 14px; }
.explore-flow-card h2 { margin: 18px 0 8px; color: #1b2944; font-size: 20px; }
.explore-flow-card p { min-height: 60px; margin: 0; color: #748097; font-size: 12px; line-height: 1.65; }
.explore-flow-card__features { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 16px; }
.explore-flow-card__features span { padding: 4px 8px; color: #69758b; font-size: 10px; background: #f3f6fa; border-radius: 6px; }
.explore-flow-card__enter { position: absolute; right: 22px; bottom: 20px; left: 22px; display: flex; align-items: center; justify-content: space-between; padding-top: 13px; color: var(--flow-color); font-size: 12px; font-weight: 600; border-top: 1px solid #edf0f5; }

@media (max-width: 1180px) {
  .explore-station__header { grid-template-columns: 200px 1fr auto; padding: 0 18px; }
  .explore-station__primary-item { min-width: auto; padding: 0 11px; }
  .explore-station__primary-item .explore-station__step { display: none; }
  .explore-station__secondary { grid-template-columns: auto 1fr auto; padding: 0 18px; }
  .explore-home__flow { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .explore-home__agent { grid-template: auto auto auto auto / 1fr; grid-template-areas: "copy" "dialog" "prompts" "history"; }
  .explore-home__agent-history { max-height: 360px; margin-top: 16px; }
}

@media (max-width: 820px) {
  .explore-station__header { position: relative; display: flex; flex-wrap: wrap; height: auto; padding: 12px 16px; }
  .explore-station__primary { order: 3; width: 100%; height: 48px; overflow-x: auto; justify-content: flex-start; }
  .explore-station__primary-item { flex: 0 0 auto; }
  .explore-station__header-actions { margin-left: auto; }
  .explore-station__secondary { position: relative; top: 0; display: flex; flex-wrap: wrap; padding: 10px 16px; }
  .explore-station__secondary-nav { order: 3; width: 100%; overflow-x: auto; justify-content: flex-start; }
  .explore-station__module-layout { display: block; }
  .explore-station__sidebar { position: relative; top: 0; height: auto; padding: 14px 16px; border-right: 0; border-bottom: 1px solid #e1e7f0; }
  .explore-station__sidebar-head { padding-bottom: 12px; }
  .explore-station__sidebar-head span { margin-top: 12px; }
  .explore-station__flow-menu,
  .explore-station__sidebar .explore-station__secondary-nav { flex-direction: row; overflow-x: auto; margin-top: 12px; }
  .explore-station__flow-menu button,
  .explore-station__sidebar .explore-station__secondary-nav button { flex: 0 0 auto; }
  .explore-station__sidebar-divider { margin: 12px 0; }
  .explore-station__sidebar-footer { margin-top: 12px; padding-top: 0; }
  .explore-station__workspace { padding: 20px 16px 28px; }
  .explore-home { padding: 20px 16px 32px; }
  .explore-home__hero { grid-template-columns: 1fr; padding: 34px 28px; }
  .explore-home__visual { display: none; }
  .explore-home__flow { grid-template-columns: 1fr; }
}
</style>
