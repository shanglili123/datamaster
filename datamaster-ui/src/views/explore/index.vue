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
        <a-tag v-if="spaceName" color="blue">{{ spaceName }}</a-tag>
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
            <h1>{{ activeSubFlow?.title }}</h1>
            <p>{{ activeSubFlow?.description }}</p>
          </div>
          <div class="explore-station__progress">
            <span>探索进度</span>
            <strong>{{ activeFlowIndex + 1 }} / {{ flows.length }}</strong>
          </div>
        </div>
        <section class="explore-station__module">
          <component :is="activeSubFlow?.component" v-if="activeSubFlow?.component" />
        </section>
      </main>
    </template>

    <main v-else class="explore-home">
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
import { computed, defineAsyncComponent, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ApartmentOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CheckOutlined,
  DatabaseOutlined,
  HomeOutlined,
  MessageOutlined,
  NodeIndexOutlined,
  RightOutlined,
  ScanOutlined,
} from "@ant-design/icons-vue";
import useUserStore from "@/store/system/user";

const QualityTaskView = defineAsyncComponent(() => import("@/views/ast/quality/qualityTask/index.vue"));
const ProbeInstanceView = defineAsyncComponent(() => import("@/views/ast/quality/probeTaskInstance/index.vue"));
const MetadataView = defineAsyncComponent(() => import("@/views/meta/catalog/table/index.vue"));
const StatisticsView = defineAsyncComponent(() => import("@/views/meta/catalog/statistics/index.vue"));
const DatasourceView = defineAsyncComponent(() => import("@/views/ast/datasource/index.vue"));
const DataSecurityView = defineAsyncComponent(() => import("@/views/ast/security/desensitizeRule/index.vue"));
const AssetView = defineAsyncComponent(() => import("@/views/ast/asset/index.vue"));
const AssetAuditView = defineAsyncComponent(() => import("@/views/ast/assetApply/index.vue"));
const DataQueryView = defineAsyncComponent(() => import("@/views/ast/dataQuery/index.vue"));
const OntologyView = defineAsyncComponent(() => import("@/views/ont/ontology/index.vue"));
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
      { id: "datasource", title: "数据源管理", description: "维护数据源连接和认证配置，为元数据探查提供数据连接。", component: DatasourceView },
      { id: "quality-task", title: "探查任务", description: "配置并运行数据质量探查任务。", component: QualityTaskView },
      { id: "metadata", title: "探查元数据", description: "浏览元数据探查得到的数据库、数据表和字段目录。", component: MetadataView },
      { id: "probe-log", title: "探查任务日志", description: "查看探查任务的执行状态、运行日志和历史结果。", component: ProbeInstanceView },
      { id: "statistics", title: "数据统计", description: "查看数据库、数据表及数据行数统计。", component: StatisticsView },
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
      { id: "asset-list", title: "资产目录", description: "浏览和管理当前空间的数据资产。", component: AssetView },
      { id: "asset-audit", title: "资产审核", description: "审核数据资产登记、变更和发布状态。", component: AssetAuditView },
      { id: "data-security", title: "数据安全", description: "配置数据脱敏规则和安全控制，保护敏感数据使用过程。", component: DataSecurityView },
      { id: "data-query", title: "数据查询", description: "基于已授权资产进行数据检索。", component: DataQueryView },
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
      { id: "ontology-list", title: "本体管理", description: "创建本体并进入可视化本体工作台。", component: OntologyView },
      { id: "ontology-concept", title: "概念管理", description: "在本体工作台中管理业务概念和主属性。", component: OntologyView },
      { id: "ontology-relation", title: "关系管理", description: "在本体工作台中管理概念之间的业务关系。", component: OntologyView },
      { id: "ontology-action", title: "动作管理", description: "在本体工作台中配置可执行的业务动作。", component: OntologyView },
      { id: "ontology-workflow", title: "动作编排", description: "在本体工作台中编排动作执行流程。", component: OntologyView },
      { id: "ontology-function", title: "函数管理", description: "管理本体动作和计算所使用的函数。", component: OntFunctionView },
      { id: "ontology-webhook", title: "Webhook", description: "配置本体动作执行成功后的回调地址和回调日志。", component: OntWebhookView },
    ],
  },
  {
    id: "ai",
    title: "智能问数",
    eyebrow: "ASK & DECIDE",
    description: "让 AI 基于数据资产和本体语义理解问题、分析数据并辅助决策。",
    icon: MessageOutlined,
    color: "#e36f3d",
    softColor: "#fff0e9",
    children: [
      { id: "ai-ask", title: "智能问数", description: "选择数据源和知识库，直接用自然语言分析数据。", component: AiAskView },
      { id: "ai-skill", title: "Skill 管理", description: "配置问数知识、提示词和可复用分析能力。", component: AiSkillView },
    ],
  },
];

const router = useRouter();
const userStore = useUserStore();
const activeFlowId = ref("");
const activeSubId = ref("");

const spaceName = computed(() => userStore.spaceName || "");
const activeFlow = computed(() => flows.find((flow) => flow.id === activeFlowId.value));
const activeFlowIndex = computed(() => flows.findIndex((flow) => flow.id === activeFlowId.value));
const activeSubFlow = computed(() => activeFlow.value?.children.find((item) => item.id === activeSubId.value));
const nextFlow = computed(() => flows[activeFlowIndex.value + 1]);

function selectFlow(flowId) {
  const flow = flows.find((item) => item.id === flowId);
  if (!flow) return;
  activeFlowId.value = flow.id;
  activeSubId.value = flow.children[0]?.id || "";
}

function selectSubFlow(subId) {
  activeSubId.value = subId;
}

function goFlowHome() {
  activeFlowId.value = "";
  activeSubId.value = "";
}

function goSystemHome() {
  router.push("/index");
}
</script>

<style lang="scss" scoped>
.explore-station {
  --station-bg: #f4f7fb;
  min-height: 100%;
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

.explore-home { padding: 38px 4vw 48px; }
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
