<template>
  <div class="app-container model-monitor">
    <h2>模型调用监控</h2>
    <a-alert type="info" show-icon message="直连模型与数据分析服务分别统计" description="一次逻辑请求包含客户端内部重试；数据分析服务请求数不代表其内部模型次数。未知 Token 不等于零，统计仅限当前空间，暂不提供费用估算。" />
    <div class="toolbar">
      <a-select v-model:value="days" :options="[{value:1,label:'近1天'},{value:7,label:'近7天'},{value:30,label:'近30天'}]" style="width: 140px" />
      <a-select v-model:value="provider" :options="providerOptions" allow-clear placeholder="全部供应商" style="width: 180px" />
      <a-input v-model:value="modelKeyword" allow-clear placeholder="筛选模型" style="width: 200px" />
      <a-button type="primary" :loading="loading" @click="loadStats">刷新</a-button>
    </div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-table :columns="columns" :data-source="filteredRows" :loading="loading" :pagination="{pageSize:15}" :scroll="{x:1500}" row-key="rowKey" size="small">
      <template #bodyCell="{column,record}">
        <template v-if="column.key==='kind'">{{record.kind==='MODEL'?'直连模型':'数据分析服务'}} </template>
        <template v-else-if="column.key==='status'"><a-tag>{{statusText(record.status)}}</a-tag></template>
        <template v-else-if="column.key==='usage'">已知 {{record.usageknowncount || 0}} / 未知 {{record.usageunknowncount || 0}}</template>
      </template>
    </a-table>
    <a-divider />
    <section v-if="canManage">
      <div class="toolbar"><h3>当前空间模型配置</h3><a-button v-hasPermi="['ai:model-config:add']" type="primary" @click="edit()">新增模型</a-button><a-button @click="loadProfiles">刷新配置</a-button></div>
      <a-alert type="warning" show-icon message="仅支持 OpenAI-compatible API 协议" description="启用DB-GPT接口同步后，保存自动排队同步，用户只需维护一处；密钥会交给受信DB-GPT服务。未开启同步时仍为独立配置。密钥加密保存、不回显，留空保留；保存不发起模型推理，远程就绪以同步状态为准。" />
      <a-table :data-source="profiles" :columns="profileColumns" row-key="id" :pagination="false" :loading="profileLoading" :scroll="{x:1000}">
        <template #bodyCell="{column,record}">
          <template v-if="column.key==='active'"><a-tag>{{record.active?'当前默认':record.enabled?'可启用':'已禁用'}}</a-tag></template>
          <template v-else-if="column.key==='key'">{{record.hasApiKey?'已配置（不回显）':'未配置'}}</template>
          <template v-else-if="column.key==='sync'"><a-tag>{{syncText(record.dbgptSyncStatus)}}</a-tag><div>{{record.dbgptSyncError || ''}}</div><small>{{record.dbgptLastAttemptAt || ''}}</small></template>
          <template v-else-if="column.key==='action'">
            <a-button v-if="record.dbgptSyncEnabled && record.enabled" type="link" v-hasPermi="['ai:model-config:edit']" :disabled="syncingIds.includes(String(record.id)) || record.dbgptSyncStatus==='SYNCING'" @click="syncProfile(record)">同步/重试</a-button>
            <a-button type="link" v-hasPermi="['ai:model-config:edit']" @click="edit(record)">编辑</a-button>
            <a-button type="link" v-if="!record.active && record.enabled" v-hasPermi="['ai:model-config:edit']" @click="activate(record)">设为默认</a-button>
            <a-button type="link" danger v-if="!record.active" v-hasPermi="['ai:model-config:remove']" @click="remove(record)">删除</a-button>
          </template>
        </template>
      </a-table>
      <a-divider />
      <ModelRoutingPanel :space-id="user.spaceId" :space-code="user.spaceCode || ''" :profiles="profiles" :profiles-error="profileError" :editable="checkPermi(['ai:model-config:edit'])" />
    </section>
    <a-modal v-model:open="open" :title="form.id?'编辑模型':'新增模型'" :confirm-loading="saving" @ok="save" ok-text="保存" cancel-text="取消" destroy-on-close>
      <a-form layout="vertical">
        <a-form-item label="名称" required><a-input v-model:value="form.name" :maxlength="100" /></a-form-item>
        <a-form-item label="供应商" required><a-select v-model:value="form.provider" :options="providers" /></a-form-item>
        <a-form-item label="模型标识" required><a-input v-model:value="form.model" :maxlength="100" placeholder="如 qwen-plus，须为供应商实际支持的模型" /></a-form-item>
        <a-form-item label="兼容 API 地址" required><a-input v-model:value="form.apiUrl" :maxlength="500" placeholder="https://…/v1" /></a-form-item>
        <a-form-item label="API Token"><a-input-password v-model:value="form.apiKey" autocomplete="new-password" :maxlength="8192" :placeholder="form.id?'留空保留原密钥':'输入 API Token（保存后不回显）'" /></a-form-item>
        <a-form-item label="启用"><a-switch v-model:checked="form.enabled" :disabled="form.active" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup name="ModelGatewayMonitor">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { checkPermi } from '@/utils/permission'
import useUserStore from '@/store/system/user'
import ModelRoutingPanel from './ModelRoutingPanel.vue'
import { modelStats, listModelProfiles, addModelProfile, updateModelProfile, activateModelProfile, deleteModelProfile, syncModelToDbGpt } from '@/api/ai/modelGateway'
const user = useUserStore()
const days = ref(7), provider = ref(), modelKeyword = ref(''), rows = ref([]), loading = ref(false), error = ref('')
const profiles = ref([]), profileLoading = ref(false), profileError = ref(''), open = ref(false), saving = ref(false), form = ref({})
let sequence = 0
const scope = () => ({spaceId:user.spaceId,spaceCode:user.spaceCode})
const canManage = computed(() => checkPermi(['ai:model-config:list']))
const providers = [{value:'BAILIAN',label:'阿里百炼'},{value:'DEEPSEEK',label:'DeepSeek（兼容API）'},{value:'OPENAI_COMPATIBLE',label:'其他兼容API'},{value:'OTHER',label:'其他供应商（兼容API）'}]
const providerOptions = computed(() => [...new Set(rows.value.map(row=>row.provider).filter(Boolean))].map(value=>({value,label:providers.find(p=>p.value===value)?.label || value})))
const filteredRows = computed(() => rows.value.filter(row=>(!provider.value || row.provider===provider.value) && (!modelKeyword.value || String(row.model || '').toLowerCase().includes(modelKeyword.value.toLowerCase()))))
const statusText = status => ({SUCCESS:'成功',FAILURE:'失败',REJECTED:'容量拒绝'}[status] || status)
const columns = [{title:'来源',key:'kind',width:130},{title:'供应商',dataIndex:'provider',width:140},{title:'模型',dataIndex:'model',width:170},{title:'场景',dataIndex:'scene',width:170},{title:'状态',key:'status',width:90},{title:'调用数',dataIndex:'attempts'},{title:'平均耗时(ms)',dataIndex:'durationmsaverage'},{title:'实测输入Token',dataIndex:'inputtokens'},{title:'实测输出Token',dataIndex:'outputtokens'},{title:'用量覆盖',key:'usage',width:160}]
const syncingIds = ref([])
const syncText = value => ({NOT_ENABLED:'同步未启用',PENDING:'待同步',SYNCING:'同步中',READY:'已同步在线',FAILED:'同步失败',DISABLED:'已停用'}[value] || '状态未知')
async function syncProfile(row) {
  const current=sequence, id=String(row.id)
  if(syncingIds.value.includes(id)) return
  syncingIds.value.push(id)
  try { await syncModelToDbGpt(row.id,scope()); if(current===sequence){message.success('已排队同步当前版本，请刷新状态');loadProfiles()} }
  catch(failure) { if(current===sequence) message.error(failure?.message || '同步排队失败') }
  finally { if(current===sequence) syncingIds.value=syncingIds.value.filter(value=>value!==id) }
}
const profileColumns=[{title:'名称',dataIndex:'name'},{title:'供应商',dataIndex:'provider'},{title:'模型',dataIndex:'model'},{title:'状态',key:'active'},{title:'Token',key:'key'},{title:'DB-GPT同步',key:'sync',width:260},{title:'操作',key:'action',width:320}]
async function loadStats(){const current=sequence;loading.value=true;error.value='';try{const response=await modelStats({...scope(),days:days.value});if(current!==sequence)return;rows.value=(response.data?.groups || []).map((row,index)=>({...row,rowKey:String(index)}))}catch(failure){if(current===sequence){rows.value=[];error.value=failure?.message || '统计读取失败，请检查迁移和权限'}}finally{if(current===sequence)loading.value=false}}
async function loadProfiles(){if(!canManage.value)return;const current=sequence;profileLoading.value=true;profileError.value='正在加载模型配置';try{const response=await listModelProfiles(scope());if(current===sequence){profiles.value=response.data || [];profileError.value=''}}catch(failure){if(current===sequence){profiles.value=[];profileError.value=failure?.message || '模型配置加载失败';message.error(profileError.value)}}finally{if(current===sequence)profileLoading.value=false}}
function edit(row){form.value=row?{id:row.id,name:row.name,provider:row.provider,model:row.model,apiUrl:row.apiUrl,enabled:row.enabled,active:row.active,apiKey:''}:{name:'',provider:'BAILIAN',model:'qwen-plus',apiUrl:'',apiKey:'',enabled:true};open.value=true}
async function save(){if(!form.value.name?.trim() || !form.value.model?.trim() || !form.value.apiUrl?.trim()){message.warning('请填写名称、模型和API地址');return}const current=sequence;saving.value=true;try{const data={...scope(),name:form.value.name,provider:form.value.provider,model:form.value.model,apiUrl:form.value.apiUrl,apiKey:form.value.apiKey,enabled:form.value.enabled};if(form.value.id)await updateModelProfile(form.value.id,data);else await addModelProfile(data);if(current!==sequence)return;form.value.apiKey='';open.value=false;message.success(profiles.value.some(profile=>profile.dbgptSyncEnabled)?'模型配置已保存，DB-GPT同步状态请刷新核对':'模型配置已保存；DB-GPT接口同步未启用');loadProfiles()}finally{if(current===sequence)saving.value=false}}
function activate(row){const current=sequence;Modal.confirm({title:`将「${row.name}」设为当前空间默认模型？`,content:'后续请求使用该默认配置；在途请求不切换。开启DB-GPT接口同步后，远程问数须等待当前版本同步就绪。',async onOk(){await activateModelProfile(row.id,scope());if(current===sequence)loadProfiles()}})}
function remove(row){const current=sequence;Modal.confirm({title:`删除模型配置「${row.name}」？`,async onOk(){await deleteModelProfile(row.id,scope());if(current===sequence)loadProfiles()}})}
watch(()=>[user.spaceId,user.spaceCode],()=>{++sequence;syncingIds.value=[];rows.value=[];profiles.value=[];open.value=false;form.value={};if(user.spaceId){loadStats();loadProfiles()}},{immediate:true})
watch(open,value=>{if(!value && form.value)form.value.apiKey=''})
onBeforeUnmount(()=>{++sequence;form.value={}})
onBeforeUnmount(()=>{ syncingIds.value=[] })
</script>
<style scoped>
.model-monitor{display:flex;flex-direction:column;gap:16px}.toolbar{display:flex;align-items:center;gap:12px;flex-wrap:wrap}h2,h3{margin:0}
</style>
