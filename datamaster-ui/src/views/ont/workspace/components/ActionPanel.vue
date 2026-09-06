<template>
  <div class="action-panel">
    <!-- 工具条：搜索 + 新增 -->
    <div class="panel-toolbar">
      <a-input
        v-model:value="queryParams.name"
        placeholder="请输入动作名称"
        allow-clear
        style="width: 200px"
        @keyup.enter="handleQuery"
      />
      <a-select
        v-model:value="queryParams.actionType"
        placeholder="动作类型"
        allow-clear
        style="width: 140px"
        :options="actionTypeOptions"
      />
      <a-button type="primary" @click="handleQuery">查询</a-button>
      <a-button @click="resetQuery">重置</a-button>
      <a-button type="primary" class="toolbar-right" @click="handleAdd" v-hasPermi="['ont:action:add']">
        <template #icon><PlusOutlined /></template>
        新增动作
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="actionList"
      :loading="loading"
      row-key="id"
      size="middle"
      :pagination="false"
      :scroll="{ x: 1180 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actionType'">
          <a-tag :color="actionTypeColor(record.actionType)">{{ actionTypeText(record.actionType) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'conceptName'">
          {{ record.actionType === 'FUNCTION' ? (functionMap[record.functionId] || record.functionId || '-') : (conceptMap[record.conceptId] || record.conceptId || '-') }}
        </template>
        <template v-else-if="column.key === 'executionMode'">
          <div class="trigger-cell">
            <a-tag :color="record.needsApproval || record.approvalLevels > 0 ? 'orange' : 'green'">
              {{ record.needsApproval || record.approvalLevels > 0 ? '一次人工确认' : '直接执行' }}
            </a-tag>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openSubmitExec(record)" v-hasPermi="['ont:action:edit']">执行</a-button>
          <a-button v-if="['COMPOSITE', 'FUNCTION'].includes(record.actionType)" type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['ont:action:edit']">修改</a-button>
          <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['ont:action:remove']">删除</a-button>
        </template>
      </template>
      <template #empty>
        <div class="emptyBg">
          <img src="@/assets/system/images/no_data/noData.png" alt="" />
          <p>暂无动作，点击右上角「新增动作」创建</p>
        </div>
      </template>
    </a-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <a-divider class="exec-divider">执行记录</a-divider>

    <a-table
      :columns="execColumns"
      :data-source="execList"
      :loading="execLoading"
      row-key="id"
      size="middle"
      :pagination="false"
      :scroll="{ x: 1450 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actionName'">
          {{ actionNameMap[record.actionId] || record.actionId || '-' }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="execStatusColor(record.status)">{{ execStatusText(record.status) }}</a-tag>
          <a-tag v-if="record.rollbackTime" color="purple">已回退</a-tag>
          <a-tooltip v-if="record.status === 'RECONCILIATION_REQUIRED'" title="执行结果不确定，系统不会自动重放，请人工核对目标系统后处理">
            <span class="reconciliation-mark">!</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'triggerSource'">
          <div class="trigger-cell">
            <a-tag :color="record.triggerType === 'DATA_ARRIVAL' ? 'blue' : 'default'">{{ triggerTypeText(record) }}</a-tag>
            <a-tag :color="record.currentStage > 0 ? 'orange' : 'cyan'">{{ executionModeText(record) }}</a-tag>
            <a-tooltip v-if="record.eventId" :title="record.eventId">
              <code class="event-id">{{ record.eventId }}</code>
            </a-tooltip>
          </div>
        </template>
        <template v-else-if="column.key === 'attempts'">
          {{ record.attemptNo || 0 }} / {{ record.maxAttempts || 1 }}
        </template>
        <template v-else-if="column.dataIndex === 'generatedSql'">
          <span class="sql-cell">{{ record.generatedSql || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'execAction'">
          <a-button v-if="record.status === 'PENDING_APPROVAL' && record.canApprove" type="link" size="small" style="color:#52c41a" @click="openApproval(record, 'approve')" v-hasPermi="['ont:action:edit']">确认执行</a-button>
          <a-button v-if="record.status === 'PENDING_APPROVAL' && record.canApprove" type="link" danger size="small" @click="openApproval(record, 'reject')" v-hasPermi="['ont:action:edit']">不执行</a-button>
          <a-button v-if="record.status === 'APPROVED' && !record.autoExecute" type="link" size="small" style="color:#1677ff" @click="handleRun(record)" v-hasPermi="['ont:action:edit']">执行</a-button>
          <a-button v-if="record.status === 'EXECUTED' && !record.rollbackTime && !['FUNCTION', 'COMPOSITE'].includes(actionTypeMap[record.actionId])" type="link" size="small" style="color:#fa8c16" @click="handleRollback(record)" v-hasPermi="['ont:action:edit']">回退</a-button>
          <a-button v-if="record.objectKey" type="link" size="small" @click="handleViewChain(record)" v-hasPermi="['ont:action:query']">确认记录</a-button>
          <a-button type="link" size="small" @click="handleViewResult(record)" v-hasPermi="['ont:action:query']">查看结果</a-button>
        </template>
      </template>
      <template #empty>
        <div class="emptyBg">
          <img src="@/assets/system/images/no_data/noData.png" alt="" />
          <p v-if="!actionList.length">当前页无动作，请先翻到有动作的分页查看执行记录</p>
          <p v-else>当前页动作暂无执行记录</p>
        </div>
      </template>
    </a-table>

    <pagination
      v-show="execTotal > 0"
      :total="execTotal"
      v-model:page="execQueryParams.pageNum"
      v-model:limit="execQueryParams.pageSize"
      @pagination="loadExecutions"
    />

    <!-- 动作对话框 -->
    <a-modal :title="title" v-model:open="open" width="920px" :get-container="false" destroy-on-close ok-text="确定" cancel-text="取消" @ok="submitForm" @cancel="cancel">
      <a-form ref="actionRef" class="action-form-compact" :model="form" :rules="rules" :label-col="{ style: { width: '105px' } }">
        <div class="action-editor-section">
          <div class="action-editor-section-title">基础信息</div>
          <div class="action-basic-grid">
        <a-form-item label="动作名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入动作名称，如 创建客户" style="width:280px" />
        </a-form-item>
        <a-form-item label="动作类型" name="actionType">
          <a-select v-model:value="form.actionType" placeholder="请选择动作类型" :options="formActionTypeOptions" style="width:200px" />
        </a-form-item>
        <a-form-item v-if="form.actionType !== 'FUNCTION'" label="触发对象类型" name="conceptId">
          <a-select
            v-model:value="form.conceptId"
            placeholder="请选择人工执行时要定位的对象类型"
            show-search
            option-filter-prop="label"
            :options="conceptOptions"
            style="width:280px"
            @change="loadConceptProperties"
          />
        </a-form-item>
        <a-form-item v-else label="绑定函数" name="functionId">
          <a-select
            v-model:value="form.functionId"
            placeholder="请选择共享函数"
            show-search
            option-filter-prop="label"
            :options="functionOptions"
            style="width:280px"
            @change="handleFunctionChange"
          />
        </a-form-item>
        <template v-if="form.actionType === 'FUNCTION'">
          <a-form-item label="数据来源概念">
            <a-select
              v-model:value="form.sourceConceptId"
              placeholder="选填：绑定后执行时将该概念物理表数据注入 input.source.rows"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="conceptOptions"
              style="width:300px"
              @change="handleSourceConceptChange"
            />
          </a-form-item>
          <a-form-item label="输出目标概念">
            <a-select
              v-model:value="form.outputConceptId"
              placeholder="选填：脚本 JSON 输出按主键 UPSERT 到该概念物理表"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="conceptOptions"
              style="width:300px"
            />
          </a-form-item>
          <a-form-item v-if="functionParamMappings.length" label="入参字段映射" class="action-grid-full">
            <div style="width:100%">
              <div style="font-size:12px;color:#999;margin-bottom:6px;">
                固定值入参执行时输入框手填；字段映射入参把脚本参数名映射到数据来源概念的属性，执行时注入该属性 code，脚本按参数名动态处理该字段。
              </div>
              <div v-for="row in functionParamMappings" :key="row.paramName" style="display:flex;align-items:center;gap:8px;margin-bottom:6px;border:1px solid #eee;padding:6px 8px;border-radius:4px;">
                <span style="width:90px;font-weight:500;">{{ row.paramName }}</span>
                <a-radio-group v-model:value="row.kind" size="small">
                  <a-radio-button value="value">固定值</a-radio-button>
                  <a-radio-button value="field">字段映射</a-radio-button>
                </a-radio-group>
                <a-select
                  v-if="row.kind === 'field'"
                  v-model:value="row.sourcePropertyId"
                  style="width:260px"
                  placeholder="选择数据来源概念属性"
                  show-search
                  option-filter-prop="label"
                  :options="sourcePropertyOptions"
                  @change="syncFieldMapping(row)"
                />
                <span v-else style="flex:1;color:#999;font-size:12px;">执行时输入框手填</span>
              </div>
            </div>
          </a-form-item>
          <a-form-item label="读取限制">
            <a-input-number v-model:value="form.readLimit" :min="1" :max="100000" style="width:160px" />
            <div style="font-size:12px;color:#999;margin-top:2px;">数据来源读取行数上限，默认 5000</div>
          </a-form-item>
        </template>
          </div>
        </div>
        <a-alert
          v-if="form.actionType !== 'FUNCTION' && form.actionType !== 'COMPOSITE' && isConditionType"
          type="warning"
          show-icon
          :message="conditionHint"
          style="margin-bottom: 8px;"
        />
        <template v-if="form.actionType !== 'FUNCTION' && form.actionType !== 'COMPOSITE'">
        <a-form-item :wrapper-col="{ span: 24 }">
          <div class="param-config-block">
            <div class="param-block-head">
              <span class="param-block-title">目标属性配置</span>
              <a-button type="dashed" size="small" @click="addParamRow">
                <template #icon><PlusOutlined /></template>
                添加属性
              </a-button>
            </div>
            <a-empty v-if="!paramRows.length" description="尚未添加目标属性，请点击上方按钮添加" :image-style="{ height: '48px' }" />
            <div v-for="(row, idx) in paramRows" :key="idx" class="param-row">
              <div class="param-head">
                <span class="param-idx">属性 {{ idx + 1 }}</span>
                <div class="param-actions">
                  <a-checkbox v-model:checked="row.required">必填</a-checkbox>
                  <a-button type="link" danger size="small" @click="removeParamRow(idx)">删除</a-button>
                </div>
              </div>
              <div class="param-body">
                <a-select
                  v-model:value="row.propertyId"
                  :options="propertyOptions"
                  placeholder="选择目标属性"
                  option-filter-prop="label"
                  show-search
                  class="param-prop-select"
                  style="width:210px"
                  @change="syncParamProperty(row)"
                />
              </div>
              <div class="param-body">
                <a-radio-group v-model:value="row.valueMode" size="small">
                  <a-radio-button value="direct">固定值</a-radio-button>
                  <a-radio-button value="placeholder">引用值</a-radio-button>
                  <a-radio-button v-if="form.actionType === 'UPDATE'" value="relative">当前值运算</a-radio-button>
                  <a-radio-button value="expression">SQL 表达式</a-radio-button>
                </a-radio-group>
                <a-input
                  v-if="row.valueMode === 'direct'"
                  v-model:value="row.valueTemplate"
                  size="small"
                  placeholder="固定目标值，如 已审核"
                  style="width:200px"
                />
                <a-input
                  v-else-if="row.valueMode === 'placeholder'"
                  v-model:value="row.valueTemplate"
                  size="small"
                  placeholder="引用入参名，如 ${newStatus}"
                  style="width:200px"
                />
                <a-input
                  v-else-if="row.valueMode === 'relative'"
                  v-model:value="row.valueTemplate"
                  size="small"
                  placeholder="数字或入参，如 1、${quantity}"
                  style="width:220px"
                >
                  <template #addonBefore>
                    <a-select v-model:value="row.relativeOperator" :options="relativeOperatorOptions" style="width:92px" />
                  </template>
                </a-input>
                <a-textarea
                  v-else
                  v-model:value="row.valueTemplate"
                  size="small"
                  :auto-size="{ minRows: 1, maxRows: 3 }"
                  placeholder="请输入 SQL 表达式，如 CURRENT_TIMESTAMP"
                  style="width:300px"
                />
              </div>
            </div>
          </div>
        </a-form-item>
        <a-form-item v-if="isConditionType" :wrapper-col="{ span: 24 }">
          <div class="condition-config-block">
            <div class="condition-block-head">
              <span class="condition-block-title">条件配置</span>
              <a-button type="dashed" size="small" @click="addConditionRow">
                <template #icon><PlusOutlined /></template>
                添加条件
              </a-button>
            </div>
            <a-empty v-if="!conditionRows.length" description="尚未添加条件，请点击上方按钮添加" :image-style="{ height: '48px' }" />
            <div v-for="(row, idx) in conditionRows" :key="idx" class="condition-row">
              <div class="condition-row-head">
                <span class="condition-idx">条件 {{ idx + 1 }}</span>
                <a-button type="link" danger size="small" @click="removeConditionRow(idx)">删除</a-button>
              </div>
              <div class="param-body">
                <a-select
                  v-model:value="row.propertyId"
                  :options="propertyOptions"
                  placeholder="选择条件属性"
                  option-filter-prop="label"
                  class="condition-prop-select"
                  style="width:180px"
                  @change="syncConditionProperty(row)"
                />
                <a-select
                  v-model:value="row.operator"
                  class="condition-operator-select"
                  :options="conditionOperatorOptions"
                  style="width:105px"
                />
                <a-select
                  v-if="idx > 0"
                  v-model:value="row.link"
                  class="condition-link-select"
                  :options="conditionLinkOptions"
                  style="width:85px"
                />
                <a-checkbox v-model:checked="row.negate">非</a-checkbox>
                <a-tag color="blue">对象值自动带入</a-tag>
              </div>
              <div class="param-body">
                <a-input
                  :value="conditionPlaceholder(row)"
                  size="small"
                  readonly
                  placeholder="选择条件属性后自动生成"
                  style="width:200px"
                />
              </div>
            </div>
          </div>
        </a-form-item>
        </template>
        <template v-if="form.actionType === 'COMPOSITE'">
          <a-form-item :wrapper-col="{ span: 24 }">
            <div class="param-config-block">
              <div class="param-block-head">
                <div>
                  <span class="param-block-title">执行步骤</span>
                  <div class="modal-hint-line">一个动作可按顺序修改多个对象类型；全部目标必须位于同一数据源，任一步失败都会整体撤销。</div>
                </div>
                <a-button type="dashed" size="small" @click="addExecutionStep">
                  <template #icon><PlusOutlined /></template>
                  添加步骤
                </a-button>
              </div>
              <a-empty v-if="!executionSteps.length" description="尚未添加执行步骤" :image-style="{ height: '48px' }" />
              <div class="execution-step-list">
              <div v-for="(step, stepIndex) in executionSteps" :key="step.key" class="execution-step-card">
                <div class="step-card-head">
                  <div>
                    <span class="step-number">步骤 {{ stepIndex + 1 }}</span>
                    <span class="step-summary">{{ step.name || '未命名步骤' }}</span>
                  </div>
                  <a-button type="link" danger size="small" @click="removeExecutionStep(stepIndex)">删除步骤</a-button>
                </div>
                <div class="step-basic-row">
                  <label class="step-field step-name-field">
                    <span>步骤名称</span>
                    <a-input v-model:value="step.name" placeholder="如 扣减库存" style="width:200px" />
                  </label>
                  <label class="step-field step-type-field">
                    <span>操作类型</span>
                    <a-select v-model:value="step.actionType" :options="stepActionTypeOptions" placeholder="请选择" style="width:140px" @change="resetStepConfig(step)" />
                  </label>
                  <label class="step-field step-concept-field">
                    <span>目标对象类型</span>
                    <a-select v-model:value="step.conceptId" :options="conceptOptions" placeholder="请选择" show-search option-filter-prop="label" style="width:200px" @change="loadStepProperties(step)" />
                  </label>
                </div>
                <div v-if="step.actionType !== 'DELETE'" class="step-section-head">
                  <span>目标属性配置</span>
                  <a-button type="dashed" size="small" @click="addStepParamRow(step)">添加属性</a-button>
                </div>
                <a-empty v-if="step.actionType !== 'DELETE' && !step.paramRows.length" description="未添加目标属性" :image-style="{ height: '32px' }" />
                <div v-for="(row, rowIndex) in step.paramRows" :key="'p-' + rowIndex" class="step-param-card">
                  <div class="step-param-card-head">
                    <a-select class="step-property-select" v-model:value="row.propertyId" :options="step.propertyOptions" placeholder="选择目标属性" show-search option-filter-prop="label" style="width:190px" @change="syncStepProperty(step, row)" />
                    <a-radio-group v-model:value="row.valueMode" size="small" button-style="solid" class="step-value-mode-group">
                      <a-radio-button value="direct">固定值</a-radio-button>
                      <a-radio-button value="placeholder">引用值</a-radio-button>
                      <a-radio-button v-if="step.actionType === 'UPDATE'" value="relative">当前值运算</a-radio-button>
                      <a-radio-button value="expression">SQL 表达式</a-radio-button>
                    </a-radio-group>
                    <div class="step-param-actions">
                      <a-checkbox v-model:checked="row.required">必填</a-checkbox>
                      <a-button type="link" danger size="small" @click="step.paramRows.splice(rowIndex, 1)">删除</a-button>
                    </div>
                  </div>
                  <div class="step-param-value-row">
                    <span class="step-value-label">属性值</span>
                    <a-input v-if="row.valueMode === 'direct'" v-model:value="row.valueTemplate" placeholder="输入固定值" style="width:240px" />
                    <a-input v-else-if="row.valueMode === 'placeholder'" v-model:value="row.valueTemplate" placeholder="如 ${prop_dm_status}" style="width:240px" />
                    <a-input v-else-if="row.valueMode === 'relative'" v-model:value="row.valueTemplate" placeholder="数字或引用值" style="width:260px">
                      <template #addonBefore>
                        <a-select v-model:value="row.relativeOperator" :options="relativeOperatorOptions" style="width:92px" />
                      </template>
                    </a-input>
                    <a-textarea v-else v-model:value="row.valueTemplate" :auto-size="{ minRows: 1, maxRows: 3 }" placeholder="输入 SQL 表达式，如 CURRENT_TIMESTAMP" style="width:360px" />
                  </div>
                </div>
                <template v-if="step.actionType === 'UPDATE' || step.actionType === 'DELETE'">
                  <div class="step-section-head">
                    <span>条件配置</span>
                    <a-button type="dashed" size="small" @click="addStepConditionRow(step)">添加条件</a-button>
                  </div>
                  <a-empty v-if="!step.conditionRows.length" description="未添加条件" :image-style="{ height: '32px' }" />
                  <div v-for="(row, rowIndex) in step.conditionRows" :key="'c-' + rowIndex" class="step-condition-card">
                    <div class="step-config-row step-condition-row">
                      <span class="condition-row-number">条件 {{ rowIndex + 1 }}</span>
                      <a-select v-if="rowIndex > 0" class="step-link-select" v-model:value="row.link" :options="conditionLinkOptions" style="width:90px" />
                      <a-select class="step-property-select" v-model:value="row.propertyId" :options="step.propertyOptions" placeholder="选择条件属性" show-search option-filter-prop="label" style="width:190px" @change="syncStepConditionProperty(step, row)" />
                      <a-select class="step-operator-select" v-model:value="row.operator" :options="conditionOperatorOptions" style="width:105px" />
                      <a-checkbox v-model:checked="row.negate">非</a-checkbox>
                      <a-button type="link" danger size="small" @click="step.conditionRows.splice(rowIndex, 1)">删除</a-button>
                    </div>
                    <div class="step-condition-source">
                      <span>值来自触发对象</span>
                      <a-select
                        v-model:value="row.sourcePropertyCode"
                        :options="ownPreconditionPropertyOptions"
                        placeholder="选择触发对象属性"
                        show-search
                        option-filter-prop="label"
                        style="width:200px"
                      />
                      <code>{{ conditionSourcePlaceholder(row) || '尚未选择' }}</code>
                    </div>
                  </div>
                </template>
              </div>
              </div>
            </div>
          </a-form-item>
        </template>
        <div class="action-editor-section action-control-section">
          <div class="action-editor-section-title">执行控制</div>
        <a-form-item label="执行模式">
          <a-radio-group v-model:value="executionMode" button-style="solid" @change="handleExecutionModeChange">
            <a-radio-button value="DIRECT">直接执行</a-radio-button>
            <a-radio-button value="MANUAL_CONFIRM">人工确认后执行</a-radio-button>
          </a-radio-group>
          <div class="modal-hint-line" style="margin-top:4px;">
            直接执行也会创建完整执行记录；人工确认模式最多确认一次，确认意见可选。人工、数据到达、API 和工作流触发都遵守这里的执行模式。
          </div>
        </a-form-item>
        <a-form-item v-if="executionMode === 'MANUAL_CONFIRM'" label="确认人">
          <a-select
            v-model:value="manualApproverId"
            :options="memberOptions"
            placeholder="选择一名确认人（留空=任意登录用户可确认）"
            option-filter-prop="label"
            show-search
            allow-clear
            style="width:280px"
          />
        </a-form-item>
        <a-form-item label="执行前置检查" :wrapper-col="{ span: 24 }">
          <div class="precondition-config-block">
            <div class="precondition-block-head">
              <div>
                <div class="precondition-block-title">满足条件才允许执行</div>
                <div class="modal-hint-line">提交时先检查一次，审批通过后、真正写入数据前会再次检查，避免库存等状态在等待期间发生变化。</div>
              </div>
              <div class="precondition-head-actions">
                <a-select v-model:value="preconditionLogic" size="small" style="width:100px" :options="preconditionLogicOptions" />
                <a-button type="dashed" size="small" @click="addPreconditionRow">
                  <template #icon><PlusOutlined /></template>
                  添加检查
                </a-button>
              </div>
            </div>
            <a-alert
              v-if="preconditionParseError"
              type="warning"
              show-icon
              message="历史前置条件不是当前构建器可识别的格式，本次保存将原样保留；添加新检查后会改为标准条件格式。"
              style="margin:8px 12px;"
            />
            <a-empty v-if="!preconditionRows.length" description="未配置前置检查，动作提交后不会校验业务状态" :image-style="{ height: '44px' }" />
            <div v-for="(row, idx) in preconditionRows" :key="idx" class="precondition-row">
              <div class="precondition-row-head">
                <span class="precondition-idx">{{ idx === 0 ? '当' : (preconditionLogic === 'AND' ? '并且' : '或者') }}</span>
                <a-button type="link" danger size="small" @click="removePreconditionRow(idx)">删除</a-button>
              </div>
              <div class="precondition-row-body">
                <a-select v-model:value="row.source" :options="preconditionSourceOptions" style="width:135px" @change="resetPreconditionField(row)" />
                <a-select
                  v-if="row.source === 'OBJECT'"
                  v-model:value="row.propertyCode"
                  :options="ownPreconditionPropertyOptions"
                  placeholder="当前对象属性"
                  show-search
                  option-filter-prop="label"
                  style="width:170px"
                />
                <template v-else-if="row.source === 'RELATED'">
                  <a-select
                    v-model:value="row.relationCode"
                    :options="actionRelationOptions"
                    placeholder="选择关系"
                    show-search
                    option-filter-prop="label"
                    style="width:155px"
                    @change="row.relatedPropertyCode = undefined"
                  />
                  <a-select
                    v-model:value="row.relatedPropertyCode"
                    :options="relatedPreconditionPropertyOptions(row)"
                    placeholder="关联对象属性"
                    show-search
                    option-filter-prop="label"
                    style="width:170px"
                  />
                </template>
                <a-input v-else-if="row.source === 'PARAM'" v-model:value="row.paramName" placeholder="参数名，如 quantity" style="width:170px" />
                <a-select v-else-if="row.source === 'CONTEXT'" v-model:value="row.contextField" :options="preconditionContextOptions" style="width:170px" />
                <a-input v-else v-model:value="row.customField" placeholder="字段表达式" style="width:190px" />
                <a-select v-model:value="row.op" :options="preconditionOperatorOptions" style="width:105px" />
                <a-input v-model:value="row.value" placeholder="比较值，如 0" style="width:130px" />
              </div>
              <div class="precondition-preview">{{ preconditionRowPreview(row) }}</div>
            </div>
          </div>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="form.description" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="请输入描述" style="width:420px" />
        </a-form-item>
        </div>
      </a-form>
    </a-modal>

    <!-- 提交执行对话框 -->
    <a-modal title="提交执行" v-model:open="execOpen" width="600px" destroy-on-close ok-text="确定" cancel-text="取消" :confirm-loading="execSaving" @ok="submitExec" @cancel="execOpen = false">
      <a-alert
        type="info"
        show-icon
        :message="'动作：' + (execAction.name || '') + '（' + actionTypeText(execAction.actionType) + ' · 触发对象类型：' + (conceptMap[execAction.conceptId] || '-') + '）'"
        style="margin-bottom: 12px;"
      />
      <p class="modal-hint-line">
        提交后先生成执行记录并进行前置检查；直接执行模式由 Worker 自动执行，人工确认模式在确认后自动执行。
      </p>
      <a-form :label-col="{ style: { width: '90px' } }">
        <a-form-item v-if="execAction.actionType !== 'FUNCTION' && execAction.actionType !== 'CREATE'" label="触发对象" required>
          <div class="exec-object-picker">
            <OntFilterBuilder
              v-if="execObjectSet"
              :fields="execObjectFilterFields"
              v-model="execObjectFilterSpec"
              show-query
              @query="loadExecObjects"
            />
            <a-table
              :columns="execObjectColumns"
              :data-source="execObjectRows"
              :loading="execObjectLoading"
              :pagination="execObjectPagination"
              :row-selection="execObjectRowSelection"
              row-key="__objectKey"
              size="small"
              :scroll="{ x: 'max-content', y: 220 }"
              @change="handleExecObjectTableChange"
            />
          </div>
          <div class="modal-hint-line" style="margin-top:4px;">先用属性过滤定位对象，再选择一条记录；条件值由系统从该对象自动读取。</div>
          <div v-if="execSelectedObject && execConditionValueConfigs.length" class="condition-value-preview">
            <div class="condition-value-title">已读取的对象值</div>
            <div v-for="cfg in execConditionValueConfigs" :key="cfg.key" class="condition-value-row">
              <span>{{ cfg.targetName }}</span>
              <a-tag>{{ cfg.operatorText }}</a-tag>
              <span class="condition-source-name">{{ cfg.sourceName }}</span>
              <code>{{ formatCellVal(cfg.value) }}</code>
            </div>
          </div>
        </a-form-item>
        <template v-if="execAction.actionType === 'FUNCTION'">
          <template v-if="execFunctionParams.length">
            <a-form-item
              v-for="paramName in execInputParamsList"
              :key="paramName"
              :label="paramName"
            >
              <a-input v-model:value="execFormValues[paramName]" :placeholder="'引用名 ${' + paramName + '}'" />
            </a-form-item>
            <div v-if="Object.keys(execFieldParamMap).length" class="fixed-param-lines">
              <div class="modal-hint-line">以下入参已映射到数据来源概念属性，无需手填：</div>
              <div v-for="(code, name) in execFieldParamMap" :key="name" class="fixed-param-line">
                <span class="fixed-param-name">{{ name }}</span>
                <span class="fixed-param-tag">字段映射</span>
                <code>{{ code }}</code>
              </div>
            </div>
          </template>
          <a-form-item v-else>
            <div class="modal-hint-line">该函数未声明参数，将直接执行。</div>
          </a-form-item>
        </template>
        <template v-else-if="hasParamConfig">
          <a-form-item
            v-for="cfg in execEditablePlaceholderConfigs"
            :key="cfg.paramName"
            :label="cfg.propName + (cfg.condition ? '（条件）' : '')"
            :required="!!cfg.required"
          >
            <a-input v-model:value="execFormValues[cfg.paramName]" :placeholder="'请输入 ' + cfg.paramName" />
          </a-form-item>
          <div v-if="execFixedConfigs.length" class="fixed-param-lines">
            <div class="modal-hint-line">以下参数已在动作定义中固定：</div>
            <div v-for="cfg in execFixedConfigs" :key="cfg.propertyCode" class="fixed-param-line">
              <span class="fixed-param-name">{{ cfg.propName }}</span>
              <span v-if="cfg.condition" class="fixed-param-tag">条件</span>
              <span class="fixed-param-mode">{{ cfg.modeText }}</span>
              <code>{{ cfg.display }}</code>
            </div>
          </div>
        </template>
        <a-form-item v-else label="输入参数">
          <a-textarea
            v-model:value="inputParams"
            :auto-size="{ minRows: 4, maxRows: 10 }"
            placeholder='JSON 格式，如 {"name":"张三"}'
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 执行结果对话框 -->
    <a-modal title="执行结果" v-model:open="resultOpen" width="720px" :footer="null">
      <a-alert
        v-if="currentResult.status === 'RECONCILIATION_REQUIRED'"
        type="error"
        show-icon
        message="该执行结果不确定，需要人工对账"
        description="为避免重复写入，系统不会自动重放。请先核对目标系统中的实际结果，再决定后续处理。"
        style="margin-bottom:12px;"
      />
      <div class="result-section">
        <div class="result-title">运行信息</div>
        <a-descriptions bordered size="small" :column="2">
          <a-descriptions-item label="触发来源">{{ triggerTypeText(currentResult) }}</a-descriptions-item>
          <a-descriptions-item label="执行模式">{{ executionModeText(currentResult) }}</a-descriptions-item>
          <a-descriptions-item label="Worker 自动领取">{{ currentResult.autoExecute ? '是' : '否' }}</a-descriptions-item>
          <a-descriptions-item label="来源引用">{{ currentResult.triggerRef || '-' }}</a-descriptions-item>
          <a-descriptions-item label="尝试次数">{{ currentResult.attemptNo || 0 }} / {{ currentResult.maxAttempts || 1 }}</a-descriptions-item>
          <a-descriptions-item label="事件 ID" :span="2"><code>{{ currentResult.eventId || '-' }}</code></a-descriptions-item>
          <a-descriptions-item label="幂等键" :span="2"><code>{{ currentResult.idempotencyKey || '-' }}</code></a-descriptions-item>
          <a-descriptions-item label="执行锁">{{ currentResult.lockOwner || '-' }}</a-descriptions-item>
          <a-descriptions-item label="锁定时间">{{ currentResult.lockTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="错误码" :span="2">
            <a-tag v-if="currentResult.errorCode" color="red">{{ currentResult.errorCode }}</a-tag>
            <span v-else>-</span>
          </a-descriptions-item>
        </a-descriptions>
      </div>
      <div v-if="currentResult.resultContext" class="result-section">
        <div class="result-title">统一执行上下文</div>
        <pre class="result-pre">{{ formatJson(currentResult.resultContext) }}</pre>
      </div>
      <div v-if="currentResult.criteriaResult" class="result-section">
        <div class="result-title">前置检查结果</div>
        <pre class="result-pre">{{ formatJson(currentResult.criteriaResult) }}</pre>
      </div>
      <div class="result-section">
        <div class="result-title">{{ resultIsFunction ? '执行代码' : '执行内容' }}</div>
        <pre class="result-pre sql-pre">{{ formatJson(currentResult.generatedSql) || '无' }}</pre>
      </div>
      <div class="result-section">
        <div class="result-title">预览结果</div>
        <pre class="result-pre">{{ currentResult.previewResult || '无' }}</pre>
      </div>
      <div v-if="!isRolledBack && compareRows.length" class="result-section">
        <div class="result-title">执行前后对比{{ compareRowHint }}</div>
        <a-table
          :columns="compareColumns"
          :data-source="compareRows"
          :pagination="false"
          row-key="colKey"
          size="small"
          :scroll="{ x: 420 }"
          :row-class-name="compareRowClassName"
        />
      </div>
      <template v-else-if="!isRolledBack && (beforeRows.length || afterRows.length)">
        <div v-if="beforeRows.length" class="result-section">
          <div class="result-title">执行前数据</div>
          <a-table
            v-if="snapshotColumns.length"
            :columns="snapshotColumns"
            :data-source="beforeRows"
            :pagination="false"
            :row-key="(_, idx) => idx"
            size="small"
            :scroll="{ x: 420 }"
          />
          <pre v-else class="result-pre">{{ currentResult.beforeData }}</pre>
        </div>
        <div v-if="afterRows.length" class="result-section">
          <div class="result-title">执行后数据</div>
          <a-table
            v-if="snapshotColumns.length"
            :columns="snapshotColumns"
            :data-source="afterRows"
            :pagination="false"
            :row-key="(_, idx) => idx"
            size="small"
            :scroll="{ x: 420 }"
          />
          <pre v-else class="result-pre">{{ currentResult.afterData }}</pre>
        </div>
      </template>
      <div v-if="isRolledBack && rollbackCompareRows.length" class="result-section">
        <div class="result-title">回退前后对比{{ rollbackCompareRowHint }}</div>
        <a-table
          :columns="compareColumns"
          :data-source="rollbackCompareRows"
          :pagination="false"
          row-key="colKey"
          size="small"
          :scroll="{ x: 420 }"
          :row-class-name="compareRowClassName"
        />
      </div>
      <template v-else-if="isRolledBack && (rollbackBeforeRows.length || rollbackAfterRows.length)">
        <div v-if="rollbackBeforeRows.length" class="result-section">
          <div class="result-title">回退前数据</div>
          <a-table
            v-if="rollbackSnapshotColumns.length"
            :columns="rollbackSnapshotColumns"
            :data-source="rollbackBeforeRows"
            :pagination="false"
            :row-key="(_, idx) => idx"
            size="small"
            :scroll="{ x: 420 }"
          />
          <pre v-else class="result-pre">{{ currentResult.rollbackBeforeData || currentResult.beforeData }}</pre>
        </div>
        <div v-if="rollbackAfterRows.length" class="result-section">
          <div class="result-title">回退后数据</div>
          <a-table
            v-if="rollbackSnapshotColumns.length"
            :columns="rollbackSnapshotColumns"
            :data-source="rollbackAfterRows"
            :pagination="false"
            :row-key="(_, idx) => idx"
            size="small"
            :scroll="{ x: 420 }"
          />
          <pre v-else class="result-pre">{{ currentResult.rollbackAfterData || currentResult.afterData }}</pre>
        </div>
      </template>
      <div v-if="currentResult.errorMessage" class="result-section">
        <div class="result-title error">错误信息</div>
        <pre class="result-pre error-pre">{{ currentResult.errorMessage }}</pre>
      </div>
    </a-modal>

    <!-- 单次人工确认：意见可选，结论永久审计 -->
    <a-modal title="人工确认" v-model:open="approvalOpen" width="460px" destroy-on-close :ok-text="approvalTitle" cancel-text="取消" :confirm-loading="approvalSaving" @ok="submitApproval" @cancel="approvalOpen = false">
      <p class="modal-hint-line" style="margin:0 0 8px;">请确认对象主键 <code>{{ approvalRecord.objectKey || '-' }}</code> 的动作是否继续执行。业务依据由前置检查给出，此处只记录人工确认结论。</p>
      <a-textarea v-model:value="approvalReason" :auto-size="{ minRows: 3, maxRows: 5 }" placeholder="审批意见（可选）" :disabled="approvalSaving" />
    </a-modal>

    <!-- 单次人工确认审计记录 -->
    <a-modal title="人工确认记录" v-model:open="chainOpen" width="700px" :footer="null" @close="chainOpen = false">
      <div v-if="!chainData" class="chain-empty">该执行记录无需人工确认或暂无确认记录</div>
      <template v-else>
        <div class="chain-header">
          <span>对象主键：<code>{{ chainData.objectKey || '-' }}</code></span>
          <span>状态：<a-tag :color="chainStatusColor(chainData.status)">{{ chainStatusText(chainData.status) }}</a-tag></span>
          <span v-if="chainData.requestTime">提交：{{ chainData.requestTime }}</span>
        </div>
        <a-divider style="margin-top:6px">确认记录</a-divider>
        <div v-for="task in chainData.tasks" :key="task.taskId" class="chain-task">
          <div class="chain-task-head">
            <span class="chain-task-title">人工确认</span>
            <a-tag :color="chainStatusColor(task.status)">{{ chainStatusText(task.status) }}</a-tag>
            <span class="chain-task-count">已同意 {{ task.approvedCount }}/{{ task.requiredApprovals }}</span>
            <span class="chain-task-approver">审批人：{{ task.approverName ? (task.approverName + '（ID:' + task.approverId + '）') : '任意用户' }}</span>
          </div>
          <a-table
            v-if="task.reviewers && task.reviewers.length"
            :columns="chainReviewerColumns"
            :data-source="task.reviewers"
            :pagination="false"
            row-key="reviewerId"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'decision'">
                <a-tag :color="record.decision === 'APPROVE' ? 'green' : 'red'">{{ chainDecisionText(record.decision) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'reason'">
                <span :title="record.reason">{{ record.reason || '-' }}</span>
              </template>
              <template v-else>
                {{ record[column.dataIndex] }}
              </template>
            </template>
          </a-table>
          <div v-else class="chain-empty">本关尚无审阅人决策</div>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="ActionPanel">
import { listAction, getAction, addAction, updateAction, delAction, submitExecution, approveExecution, rejectExecution, runExecution, rollbackExecution, listExecution, getApprovalChain } from '@/api/ont/action'
import { listConcept } from '@/api/ont/concept'
import { listProperty } from '@/api/ont/property'
import { listConceptTable } from '@/api/ont/conceptTable'
import { listPropertyColumn } from '@/api/ont/propertyColumn'
import { listFunction, getFunction } from '@/api/ont/function'
import { listRelation } from '@/api/ont/relation'
import { listObjectSets, queryObjects } from '@/api/ont/objectInstance'
import { listSpaceUserRel } from '@/api/tax/spaceUserRel/spaceUserRel'
import useUserStore from '@/store/system/user'
import OntFilterBuilder from '@/components/OntFilterBuilder/index.vue'
import { PlusOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  ontologyId: {
    type: [Number, String],
    required: true
  }
})
const emit = defineEmits(['changed'])

const { proxy } = getCurrentInstance()
const loading = ref(false)
const actionList = ref([])
const total = ref(0)
const open = ref(false)
const title = ref('')
// 触发来源是每次运行的上下文，不属于动作定义；动作定义只决定是否需要一次人工确认。
const executionMode = ref('DIRECT')

// 执行记录状态
const execLoading = ref(false)
const execList = ref([])
const execTotal = ref(0)
// 独立于动作分页的执行记录分页参数
const execQueryParams = reactive({
  pageNum: 1,
  pageSize: 6,
  ontologyId: props.ontologyId,
  actionIds: []
})
// 上面动作分页「当前页」内动作 id 集合：执行记录分页内容按此动态过滤
// 翻动作分页时由 getList() 同步刷新；翻执行记录分页不影响该集合
const currentActionIds = ref([])

// 概念下拉与名称映射（动作必须绑定到本体内的概念）
const conceptOptions = ref([])
const conceptMap = computed(() => {
  const map = {}
  conceptOptions.value.forEach(c => { map[c.value] = c.label })
  return map
})
// 动作名称映射（执行记录展示用）
const actionNameMap = computed(() => {
  const map = {}
  actionList.value.forEach(a => { map[a.id] = a.name })
  return map
})
// 动作类型映射（id -> 动作类型，回退按钮与结果展示用）
const actionTypeMap = computed(() => {
  const map = {}
  actionList.value.forEach(a => { map[a.id] = a.actionType })
  return map
})

/* ================= 执行前置检查 ================= */

const ontologyRelations = ref([])
const ontologyProperties = ref([])
const preconditionRows = ref([])
const preconditionLogic = ref('AND')
const preconditionParseError = ref(false)

const preconditionLogicOptions = [
  { value: 'AND', label: '全部满足' },
  { value: 'OR', label: '任一满足' }
]

const preconditionSourceOptions = [
  { value: 'OBJECT', label: '当前对象' },
  { value: 'RELATED', label: '关联对象' },
  { value: 'PARAM', label: '动作参数' },
  { value: 'CONTEXT', label: '运行上下文' },
  { value: 'CUSTOM', label: '自定义表达式' }
]

const preconditionContextOptions = [
  { value: 'objectKey', label: '对象主键' },
  { value: '@currentUser', label: '当前用户' },
  { value: '@now', label: '当前时间' }
]

const preconditionOperatorOptions = [
  { value: 'eq', label: '等于 =' },
  { value: 'ne', label: '不等于 ≠' },
  { value: 'gt', label: '大于 >' },
  { value: 'ge', label: '大于等于 ≥' },
  { value: 'lt', label: '小于 <' },
  { value: 'le', label: '小于等于 ≤' },
  { value: 'contains', label: '包含' },
  { value: 'in', label: '属于列表' }
]

const ownPreconditionPropertyOptions = computed(() =>
  propertyOptions.value.map(p => ({ value: p.code, label: p.label }))
)

const actionRelationOptions = computed(() =>
  ontologyRelations.value
    .filter(r => String(r.sourceConceptId) === String(form.value.conceptId))
    .filter(r => r.relationType === 'one_to_one' || r.relationType === 'many_to_one')
    .map(r => ({ value: r.code, label: `${r.name}（${r.code}）`, targetConceptId: r.targetConceptId }))
)

function relatedPreconditionPropertyOptions(row) {
  const relation = ontologyRelations.value.find(r => r.code === row.relationCode)
  if (!relation) return []
  return ontologyProperties.value
    .filter(p => String(p.conceptId) === String(relation.targetConceptId))
    .map(p => ({ value: p.code, label: `${p.name}（${p.code}）` }))
}

async function loadRelations() {
  const res = await listRelation({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 })
  ontologyRelations.value = res.data?.rows || []
}

function newPreconditionRow() {
  return {
    source: 'OBJECT',
    propertyCode: undefined,
    relationCode: undefined,
    relatedPropertyCode: undefined,
    paramName: undefined,
    contextField: 'objectKey',
    customField: undefined,
    op: 'gt',
    value: ''
  }
}

function addPreconditionRow() {
  preconditionParseError.value = false
  preconditionRows.value.push(newPreconditionRow())
}

function removePreconditionRow(index) {
  preconditionRows.value.splice(index, 1)
}

function resetPreconditionField(row) {
  row.propertyCode = undefined
  row.relationCode = undefined
  row.relatedPropertyCode = undefined
  row.paramName = undefined
  row.contextField = 'objectKey'
  row.customField = undefined
}

function preconditionField(row) {
  if (row.source === 'OBJECT') return row.propertyCode ? `object.${row.propertyCode}` : ''
  if (row.source === 'RELATED') {
    return row.relationCode && row.relatedPropertyCode
      ? `object.${row.relationCode}.${row.relatedPropertyCode}` : ''
  }
  if (row.source === 'PARAM') return row.paramName ? `param.${row.paramName}` : ''
  if (row.source === 'CONTEXT') return row.contextField || ''
  return row.customField || ''
}

function preconditionRowPreview(row) {
  const field = preconditionField(row) || '（请选择检查字段）'
  const op = (preconditionOperatorOptions.find(o => o.value === row.op) || {}).label || row.op
  return `${field} ${op || ''} ${row.value === '' || row.value === undefined ? '（比较值）' : row.value}`
}

function parseSubmissionCriteria(json) {
  preconditionRows.value = []
  preconditionLogic.value = 'AND'
  preconditionParseError.value = false
  if (!json) return
  try {
    const criteria = JSON.parse(json)
    preconditionLogic.value = String(criteria.logic || 'AND').toUpperCase() === 'OR' ? 'OR' : 'AND'
    preconditionRows.value = (Array.isArray(criteria.conditions) ? criteria.conditions : []).map(cond => {
      const row = newPreconditionRow()
      const field = String(cond.field || '')
      if (field.startsWith('object.')) {
        const parts = field.split('.')
        if (parts.length === 2) {
          row.source = 'OBJECT'
          row.propertyCode = parts[1]
        } else if (parts.length === 3) {
          row.source = 'RELATED'
          row.relationCode = parts[1]
          row.relatedPropertyCode = parts[2]
        } else {
          row.source = 'CUSTOM'
          row.customField = field
        }
      } else if (field.startsWith('param.')) {
        row.source = 'PARAM'
        row.paramName = field.substring('param.'.length)
      } else if (field === 'objectKey' || field.startsWith('@')) {
        row.source = 'CONTEXT'
        row.contextField = field
      } else {
        row.source = 'CUSTOM'
        row.customField = field
      }
      row.op = cond.op || cond.operator || 'eq'
      row.value = cond.value === null || cond.value === undefined ? '' : String(cond.value)
      return row
    })
  } catch (e) {
    // 不覆盖无法识别的历史配置；保存时仍沿用原始 JSON，直到用户主动添加新的可视化条件。
    preconditionParseError.value = true
  }
}

function buildSubmissionCriteriaJson() {
  if (preconditionParseError.value && !preconditionRows.value.length) {
    return form.value.submissionCriteria
  }
  if (!preconditionRows.value.length) return undefined
  const conditions = preconditionRows.value.map((row, index) => {
    const field = preconditionField(row)
    if (!field) throw new Error(`第 ${index + 1} 条前置检查未选择完整字段`)
    if (row.value === undefined || row.value === null || String(row.value).trim() === '') {
      throw new Error(`第 ${index + 1} 条前置检查未填写比较值`)
    }
    return { field, op: row.op || 'eq', value: row.value }
  })
  return JSON.stringify({ logic: preconditionLogic.value, conditions })
}

// 共享函数下拉与名称映射（函数类动作绑定共享函数，不绑定本体）
const functionOptions = ref([])
const functionMap = computed(() => {
  const map = {}
  functionOptions.value.forEach(f => { map[f.value] = f.label })
  return map
})
// 函数 id -> 参数名数组（执行弹窗渲染输入框用）
const functionParamsMap = ref({})
// 当前执行动作的函数参数声明（可读副本）
const execFunctionParams = computed(() => functionParamsMap.value[execAction.value.id] || [])
// 当前执行动作的字段映射入参（param_name -> sourcePropertyCode），执行时值来自概念属性映射而非手填
const execFieldParamMap = computed(() => {
  const map = {}
  parseParamConfig(execAction.value.paramConfig).forEach(c => {
    if (c.kind === 'field' && c.paramName && c.sourcePropertyCode) map[c.paramName] = c.sourcePropertyCode
  })
  return map
})
// 需要手填的入参：函数参数声明中非字段映射的部分
const execInputParamsList = computed(() =>
  (execFunctionParams.value || []).filter(name => !execFieldParamMap.value[name])
)

function loadFunctions() {
  listFunction({ pageNum: 1, pageSize: 200 }).then(res => {
    const rows = res.data?.rows || []
    functionOptions.value = rows.map(f => ({ value: f.id, label: f.name }))
  })
}

function parseFunctionParams(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr.filter(n => typeof n === 'string') : []
  } catch (e) {
    return []
  }
}

/* ============ 函数动作：入参字段映射绑定 ============ */
// 绑定表单当前选中函数的参数名列表（PARAMS）
const formFunctionParams = ref([])
// 入参映射行：{ paramName, kind: 'value'|'field', sourcePropertyCode }
const functionParamMappings = ref([])
// 数据来源概念的属性下拉（字段映射选目标用）
const sourcePropertyOptions = ref([])

// form.functionId 变化时加载其参数声明，并重置映射行（默认全部为固定值）
function handleFunctionChange() {
  formFunctionParams.value = []
  functionParamMappings.value = []
  const functionId = form.value.functionId
  if (!functionId) return
  getFunction(functionId).then(res => {
    const fn = res.data || {}
    const params = parseFunctionParams(fn.params)
    formFunctionParams.value = params
    functionParamMappings.value = params.map(name => ({ paramName: name, kind: 'value', sourcePropertyCode: undefined }))
  }).catch(() => {
    formFunctionParams.value = []
    functionParamMappings.value = []
  })
}

// form.sourceConceptId 变化时加载该概念属性供字段映射选择
function handleSourceConceptChange() {
  loadSourcePropertiesFor(form.value.sourceConceptId)
}

function loadSourcePropertiesFor(conceptId) {
  sourcePropertyOptions.value = []
  if (!conceptId) return Promise.resolve()
  return listConceptTable({ conceptId }).then(res => {
    const tables = (res.data && res.data.rows) || res.data || []
    if (!tables.length) return null
    return listPropertyColumn(tables[0].id)
  }).then(res => {
    if (!res) return
    const cols = (res.data && res.data.rows) || res.data || []
    sourcePropertyOptions.value = cols.map(c => {
      const p = propertyDict.value[c.propertyId] || {}
      const code = p.code || c.columnName
      return { value: c.propertyId, code, columnName: c.columnName, label: `${p.name || code}（${c.columnName}）` }
    })
  }).catch(() => {
    sourcePropertyOptions.value = []
  })
}

// 字段映射下拉选中后回写 sourcePropertyCode 与 propertyId
function syncFieldMapping(row) {
  const opt = sourcePropertyOptions.value.find(o => o.value === row.sourcePropertyId)
  if (opt) row.sourcePropertyCode = opt.code
}

const actionTypeOptions = [
  { value: 'CREATE', label: '新建' },
  { value: 'UPDATE', label: '更新' },
  { value: 'DELETE', label: '删除' },
  { value: 'COMPOSITE', label: '多目标动作' },
  { value: 'FUNCTION', label: '函数' }
]

// 动作表单可选类型：新建/更新/删除属于对象浏览器行操作，不在此手工创建
const formActionTypeOptions = [
  { value: 'COMPOSITE', label: '多步骤' },
  { value: 'FUNCTION', label: '函数' }
]

function actionTypeText(type) {
  return { CREATE: '新建', UPDATE: '更新', DELETE: '删除', COMPOSITE: '多目标动作', FUNCTION: '函数' }[type] || type
}

function actionTypeColor(type) {
  return { CREATE: 'green', UPDATE: 'blue', DELETE: 'red', COMPOSITE: 'purple', FUNCTION: 'orange' }[type] || 'default'
}

function execStatusText(s) {
  return { DRAFT: '草稿', PENDING_APPROVAL: '待人工确认', APPROVED: '待执行', RUNNING: '执行中', REJECTED: '已决定不执行', EXECUTED: '已执行', FAILED: '失败', ROLLED_BACK: '已回退', RECONCILIATION_REQUIRED: '需对账' }[s] || s
}

function execStatusColor(s) {
  return { DRAFT: 'default', PENDING_APPROVAL: 'orange', APPROVED: 'green', RUNNING: 'processing', REJECTED: 'red', EXECUTED: 'blue', FAILED: 'red', ROLLED_BACK: 'purple', RECONCILIATION_REQUIRED: 'volcano' }[s] || 'default'
}

function triggerTypeText(record = {}) {
  if (record.triggerType === 'DATA_ARRIVAL') return '数据到达'
  if (record.triggerType === 'PREVIEW_ONLY') return '人工预览'
  return '人工提交'
}

function executionModeText(record = {}) {
  if (Number(record.currentStage) > 0) return '人工确认后执行'
  if (record.triggerType === 'PREVIEW_ONLY' && !record.autoExecute) return '预览确认后执行'
  return '直接执行'
}

const columns = [
  { title: '动作名称', dataIndex: 'name', align: 'left', width: 160 },
  { title: '动作类型', key: 'actionType', align: 'center', width: 100 },
  { title: '触发对象类型', key: 'conceptName', align: 'center', width: 140 },
  { title: '执行模式', key: 'executionMode', align: 'left', width: 150 },
  { title: '描述', dataIndex: 'description', align: 'left', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 170 },
  { title: '操作', key: 'action', align: 'center', width: 170, fixed: 'right' }
]

const execColumns = [
  { title: '动作', key: 'actionName', align: 'left', width: 140 },
  { title: '状态', key: 'status', align: 'center', width: 145 },
  { title: '触发来源 / 事件', key: 'triggerSource', align: 'left', width: 240 },
  { title: '尝试', key: 'attempts', align: 'center', width: 80 },
  { title: '执行内容', dataIndex: 'generatedSql', align: 'left', ellipsis: true },
  { title: '错误码', dataIndex: 'errorCode', align: 'center', width: 150, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 170 },
  { title: '审批意见', dataIndex: 'approvalReason', align: 'center', width: 130, ellipsis: true },
  { title: '操作', key: 'execAction', align: 'center', width: 250, fixed: 'right' }
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  ontologyId: props.ontologyId,
  name: undefined,
  actionType: undefined
})

const data = reactive({
  form: {},
  rules: {
    name: [{ required: true, message: '动作名称不能为空', trigger: 'blur' }],
    actionType: [{ required: true, message: '动作类型不能为空', trigger: 'change' }]
  }
})
const { form, rules } = toRefs(data)

// 更新/删除动作必须配置对象定位条件，防止误操作整批数据。
const isConditionType = computed(() => form.value.actionType === 'UPDATE' || form.value.actionType === 'DELETE')
const conditionHint = computed(() => {
  if (form.value.actionType === 'DELETE') {
    return '删除动作必须至少添加 1 个条件，用于准确定位要删除的对象'
  }
  if (form.value.actionType === 'UPDATE') {
    return '更新动作必须至少添加 1 个条件（建议使用主属性）'
  }
  return ''
})
// 条件字段间的逻辑连接符：且(AND) / 或(OR)，配合「非」取反勾选组合出完整 WHERE 条件
const conditionLinkOptions = [
  { value: 'AND', label: '且(AND)' },
  { value: 'OR', label: '或(OR)' }
]

const conditionOperatorOptions = [
  { value: 'eq', label: '等于' },
  { value: 'ne', label: '不等于' },
  { value: 'gt', label: '大于' },
  { value: 'ge', label: '大于等于' },
  { value: 'lt', label: '小于' },
  { value: 'le', label: '小于等于' },
  { value: 'contains', label: '包含' },
  { value: 'in', label: '属于列表' }
]

const relativeOperatorOptions = [
  { value: 'ADD', label: '当前值 +' },
  { value: 'SUBTRACT', label: '当前值 -' }
]

// 动态条件字段行：每行自选属性 + 连接符(首个忽略) + 非取反 + 取值配置
const conditionRows = ref([])

function addConditionRow() {
  conditionRows.value.push({
    propertyId: undefined,
    propertyCode: undefined,
    propertyLabel: undefined,
    operator: 'eq',
    link: 'AND',
    negate: false,
    valueMode: 'placeholder',
    valueTemplate: '',
    required: true
  })
}

function removeConditionRow(idx) {
  conditionRows.value.splice(idx, 1)
}

function syncConditionProperty(row) {
  const opt = propertyOptions.value.find(o => o.value === row.propertyId)
  if (opt) {
    row.propertyCode = opt.code
    row.propertyLabel = opt.label
    if (row.valueMode === 'placeholder') {
      row.valueTemplate = `\${${opt.code}}`
    }
  }
}

function conditionPlaceholder(row) {
  return row.propertyCode ? `\${${row.propertyCode}}` : ''
}

function getList() {
  loading.value = true
  queryParams.ontologyId = props.ontologyId
  listAction(queryParams).then(res => {
    const rows = res.data?.rows || []
    actionList.value = rows
    total.value = res.data?.total || 0
    // 同步当前动作分页页内 id 集合，并重置执行记录页码到 1 后重查，
    // 实现「执行记录分页跟随上面动作分页页内数据动态变化」的联动效果
    currentActionIds.value = rows.map(r => r.id)
    execQueryParams.actionIds = [...currentActionIds.value]
    execQueryParams.pageNum = 1
    loadExecutions()
  }).finally(() => {
    loading.value = false
  })
}

function loadExecutions() {
  clearExecutionRefresh()
  // 联动联动：当前页动作集合为空时不查后端，直接清空执行记录展示。
  // 语义上「当前页没有动作」→ 不应展示任何执行记录；翻到有动作的页后再查。
  if (!currentActionIds.value.length) {
    execList.value = []
    execTotal.value = 0
    return
  }
  execLoading.value = true
  execQueryParams.ontologyId = props.ontologyId
  execQueryParams.actionIds = [...currentActionIds.value]
  listExecution(execQueryParams).then(res => {
    execList.value = res.data?.rows || []
    execTotal.value = res.data?.total || 0
    scheduleExecutionRefresh()
  }).finally(() => {
    execLoading.value = false
  })
}

let executionRefreshTimer

function clearExecutionRefresh() {
  if (executionRefreshTimer) {
    clearTimeout(executionRefreshTimer)
    executionRefreshTimer = undefined
  }
}

function scheduleExecutionRefresh() {
  const hasActiveExecution = execList.value.some(record =>
    record.status === 'RUNNING' || (record.status === 'APPROVED' && record.autoExecute)
  )
  if (!hasActiveExecution) return
  executionRefreshTimer = setTimeout(() => loadExecutions(), 1500)
}

function loadConcepts() {
  listConcept({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 100 }).then(res => {
    const rows = (res.data && res.data.rows) || []
    conceptOptions.value = rows.map(c => ({ value: c.id, label: c.name }))
  })
}

/* ================= 目标属性配置（属性选择 + 目标值） ================= */

// 本体内全部属性字典（id -> 属性）
const propertyDict = ref({})
// 当前绑定概念的可用属性（经概念表列映射解析）
const propertyOptions = ref([])
// 目标属性取值配置行（列表式直填：属性下拉 + 值模式 + 值 + 必填）
const paramRows = ref([])

let propLoadToken = 0

async function loadConceptProperties(conceptId) {
  const token = ++propLoadToken
  propertyOptions.value = []
  paramRows.value = []
  if (!conceptId) return
  const tableRes = await listConceptTable({ conceptId })
  if (token !== propLoadToken) return
  const tables = (tableRes.data && tableRes.data.rows) || tableRes.data || []
  if (!tables.length) return
  const colRes = await listPropertyColumn(tables[0].id)
  if (token !== propLoadToken) return
  const cols = (colRes.data && colRes.data.rows) || colRes.data || []
  propertyOptions.value = cols.map(c => {
    const p = propertyDict.value[c.propertyId] || {}
    const code = p.code || c.columnName
    return {
      value: c.propertyId,
      code,
      columnName: c.columnName,
      isPrimary: !!p.isPrimary,
      label: `${p.name || code}（${c.columnName}）${p.isPrimary ? ' · 主键' : ''}`
    }
  })
}

async function loadProperties() {
  const res = await listProperty({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 1000 })
  const rows = (res.data && res.data.rows) || []
  ontologyProperties.value = rows
  const dict = {}
  rows.forEach(p => { dict[p.id] = p })
  propertyDict.value = dict
}

function stripPlaceholder(template) {
  return String(template || '').trim().replace(/^\$\{/, '').replace(/\}$/, '').trim()
}

function addParamRow() {
  paramRows.value.push({
    propertyId: undefined,
    propertyCode: undefined,
    propertyLabel: undefined,
    valueMode: 'direct',
    valueTemplate: '',
    relativeOperator: 'SUBTRACT',
    required: false
  })
}

function removeParamRow(idx) {
  paramRows.value.splice(idx, 1)
}

function syncParamProperty(row) {
  const opt = propertyOptions.value.find(o => o.value === row.propertyId)
  if (opt) {
    row.propertyCode = opt.code
    row.propertyLabel = opt.label
  }
}

/* ================= 多目标动作执行步骤 ================= */

const executionSteps = ref([])
let executionStepKey = 0
const stepActionTypeOptions = [
  { value: 'CREATE', label: '新建' },
  { value: 'UPDATE', label: '更新' },
  { value: 'DELETE', label: '删除' }
]

function newStepParamRow() {
  return {
    propertyId: undefined,
    propertyCode: undefined,
    valueMode: 'direct',
    valueTemplate: '',
    relativeOperator: 'SUBTRACT',
    required: false
  }
}

function newStepConditionRow() {
  return {
    propertyId: undefined,
    propertyCode: undefined,
    sourcePropertyCode: undefined,
    operator: 'eq',
    link: 'AND',
    negate: false
  }
}

function addExecutionStep() {
  executionSteps.value.push({
    key: ++executionStepKey,
    name: '',
    actionType: 'UPDATE',
    conceptId: undefined,
    propertyOptions: [],
    paramRows: [],
    conditionRows: []
  })
}

function removeExecutionStep(index) {
  executionSteps.value.splice(index, 1)
}

function resetStepConfig(step) {
  step.paramRows = []
  step.conditionRows = []
}

async function loadStepProperties(step, preserveRows = false) {
  step.propertyOptions = []
  if (!preserveRows) resetStepConfig(step)
  if (!step.conceptId) return
  const tableRes = await listConceptTable({ conceptId: step.conceptId })
  const tables = (tableRes.data && tableRes.data.rows) || tableRes.data || []
  if (!tables.length) return
  const colRes = await listPropertyColumn(tables[0].id)
  const cols = (colRes.data && colRes.data.rows) || colRes.data || []
  step.propertyOptions = cols.map(c => {
    const p = propertyDict.value[c.propertyId] || {}
    const code = p.code || c.columnName
    return {
      value: c.propertyId,
      code,
      label: `${p.name || code}（${c.columnName}）${p.isPrimary ? ' · 主属性' : ''}`
    }
  })
}

function addStepParamRow(step) {
  step.paramRows.push(newStepParamRow())
}

function addStepConditionRow(step) {
  step.conditionRows.push(newStepConditionRow())
}

function syncStepProperty(step, row) {
  const option = step.propertyOptions.find(o => o.value === row.propertyId)
  if (option) row.propertyCode = option.code
}

function syncStepConditionProperty(step, row) {
  const option = step.propertyOptions.find(o => o.value === row.propertyId)
  if (option) {
    row.propertyCode = option.code
    if (propertyOptions.value.some(source => source.code === option.code)) {
      row.sourcePropertyCode = option.code
    }
  }
}

function conditionSourcePlaceholder(row) {
  return row.sourcePropertyCode ? `\${${row.sourcePropertyCode}}` : ''
}

function serializeStepConfig(step) {
  const config = step.paramRows.filter(r => r.propertyCode).map(r => ({
    propertyCode: r.propertyCode,
    valueMode: r.valueMode || 'direct',
    valueTemplate: r.valueTemplate || '',
    relativeOperator: r.valueMode === 'relative' ? (r.relativeOperator || 'SUBTRACT') : undefined,
    required: !!r.required
  }))
  step.conditionRows.filter(r => r.propertyCode).forEach(r => config.push({
    propertyCode: r.propertyCode,
    valueMode: 'placeholder',
    valueTemplate: `\${${r.sourcePropertyCode || r.propertyCode}}`,
    required: true,
    condition: true,
    conditionOperator: r.operator || 'eq',
    conditionLink: r.link || 'AND',
    conditionNegate: !!r.negate
  }))
  return config
}

function buildExecutionStepsJson() {
  if (!executionSteps.value.length) throw new Error('请至少添加一个执行步骤')
  const steps = executionSteps.value.map((step, index) => {
    if (!step.conceptId) throw new Error(`步骤 ${index + 1} 未选择目标对象类型`)
    const config = serializeStepConfig(step)
    if (step.actionType === 'CREATE' && !config.some(c => !c.condition)) {
      throw new Error(`步骤 ${index + 1} 至少需要一个目标属性`)
    }
    if ((step.actionType === 'UPDATE' || step.actionType === 'DELETE') && !config.some(c => c.condition)) {
      throw new Error(`步骤 ${index + 1} 至少需要一个条件`)
    }
    const missingConditionSource = step.conditionRows.find(row => row.propertyCode && !row.sourcePropertyCode)
    if (missingConditionSource) {
      throw new Error(`步骤 ${index + 1} 的条件「${missingConditionSource.propertyCode}」未选择触发对象取值属性`)
    }
    if (step.actionType === 'UPDATE' && !config.some(c => !c.condition)) {
      throw new Error(`步骤 ${index + 1} 至少需要一个目标属性`)
    }
    return {
      stepNo: index + 1,
      name: step.name || `步骤 ${index + 1}`,
      actionType: step.actionType,
      conceptId: step.conceptId,
      paramConfig: config
    }
  })
  return JSON.stringify(steps)
}

async function loadExecutionSteps(json) {
  executionSteps.value = []
  const saved = parseJsonArray(json)
  for (const item of (Array.isArray(saved) ? saved : [])) {
    const step = {
      key: ++executionStepKey,
      name: item.name || '',
      actionType: item.actionType || 'UPDATE',
      conceptId: item.conceptId,
      propertyOptions: [],
      paramRows: [],
      conditionRows: []
    }
    executionSteps.value.push(step)
    await loadStepProperties(step, true)
    const config = parseJsonArray(item.paramConfig ?? item.param_config)
    config.forEach(c => {
      const option = step.propertyOptions.find(o => o.code === c.propertyCode)
      if (c.condition) {
        step.conditionRows.push({
          propertyId: option?.value,
          propertyCode: c.propertyCode,
          sourcePropertyCode: stripPlaceholder(c.valueTemplate) || c.propertyCode,
          operator: c.conditionOperator || 'eq',
          link: c.conditionLink === 'OR' ? 'OR' : 'AND',
          negate: !!c.conditionNegate
        })
      } else {
        step.paramRows.push({
          propertyId: option?.value,
          propertyCode: c.propertyCode,
          valueMode: ['direct', 'placeholder', 'relative', 'expression'].includes(c.valueMode) ? c.valueMode : 'direct',
          valueTemplate: c.valueTemplate || '',
          relativeOperator: c.relativeOperator === 'ADD' ? 'ADD' : 'SUBTRACT',
          required: !!c.required
        })
      }
    })
  }
}

function flattenExecutionStepConfigs(json) {
  return parseJsonArray(json).flatMap(step =>
    parseJsonArray(step.paramConfig).map(config => ({
      ...config,
      stepName: step.name,
      targetConceptId: step.conceptId
    }))
  )
}

/* ================= 单次人工确认人绑定（空间成员） ================= */

// 当前空间成员（审批人下拉数据源）；spaceId 取自登录态 store（切换空间后跟随）
const userStore = useUserStore()
const memberOptions = ref([])

function loadMembers() {
  const spaceId = userStore.spaceId
  if (!spaceId) {
    memberOptions.value = []
    return
  }
  listSpaceUserRel({ spaceId, pageNum: 1, pageSize: 200 }).then(res => {
    const rows = (res.data && res.data.rows) || []
    memberOptions.value = rows.map(u => ({ value: u.userId, label: u.nickName || u.userName || u.userId }))
  }).catch(() => {
    memberOptions.value = []
  })
}

// 每个动作最多配置一名人工审批人；留空表示任意登录用户可确认。
const manualApproverId = ref()

function parseManualApprover(json) {
  if (!json) return undefined
  try {
    const arr = JSON.parse(json)
    const reviewer = Array.isArray(arr) ? (arr.find(r => r && Number(r.stage || 1) === 1) || arr[0]) : null
    return reviewer && reviewer.userId != null ? reviewer.userId : undefined
  } catch (e) {
    return undefined
  }
}

function buildSingleApprovalReviewerJson() {
  if (!form.value.needsApproval || manualApproverId.value === undefined || manualApproverId.value === null || manualApproverId.value === '') {
    return undefined
  }
  const option = memberOptions.value.find(o => String(o.value) === String(manualApproverId.value))
  return JSON.stringify([{ stage: 1, userId: manualApproverId.value, userName: option ? option.label : '' }])
}

function handleExecutionModeChange() {
  const needsApproval = executionMode.value === 'MANUAL_CONFIRM'
  form.value.needsApproval = needsApproval
  form.value.approvalLevels = needsApproval ? 1 : 0
  if (!needsApproval) manualApproverId.value = undefined
}

function buildParamConfigJson() {
  const cfg = []
  // 目标属性
  paramRows.value.forEach(r => {
    cfg.push({
      propertyCode: r.propertyCode,
      valueMode: r.valueMode,
      valueTemplate: r.valueTemplate,
      relativeOperator: r.valueMode === 'relative' ? (r.relativeOperator || 'SUBTRACT') : undefined,
      required: !!r.required
    })
  })
  // 对象定位条件
  conditionRows.value.forEach(r => {
    if (!r.propertyCode) return
    cfg.push({
      propertyCode: r.propertyCode,
      valueMode: 'placeholder',
      valueTemplate: `\${${r.propertyCode}}`,
      required: true,
      condition: true,
      conditionOperator: r.operator || 'eq',
      conditionLink: r.link || 'AND',
      conditionNegate: !!r.negate
    })
  })
  return cfg.length ? JSON.stringify(cfg) : undefined
}

function parseJsonArray(value) {
  if (Array.isArray(value)) return value
  if (!value) return []
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    if (Array.isArray(parsed)) return parsed
    if (typeof parsed === 'string' && parsed !== value) return parseJsonArray(parsed)
    return []
  } catch (e) {
    return []
  }
}

function parseParamConfig(json) {
  return parseJsonArray(json)
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.name = undefined
  queryParams.actionType = undefined
  handleQuery()
}

function reset() {
  form.value = { ontologyId: props.ontologyId, name: undefined, actionType: undefined, conceptId: undefined, functionId: undefined, sourceConceptId: undefined, outputConceptId: undefined, readLimit: 5000, needsApproval: false, approvalLevels: 0, approvalReviewers: undefined, submissionCriteria: undefined, triggerRef: undefined, description: undefined, paramConfig: undefined, executionSteps: undefined }
  executionMode.value = 'DIRECT'
  preconditionRows.value = []
  preconditionLogic.value = 'AND'
  preconditionParseError.value = false
  paramRows.value = []
  conditionRows.value = []
  executionSteps.value = []
  manualApproverId.value = undefined
  formFunctionParams.value = []
  functionParamMappings.value = []
  sourcePropertyOptions.value = []
  proxy.resetForm('actionRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增动作'
}

function handleUpdate(row) {
  reset()
  getAction(row.id).then(async res => {
    form.value = res.data
    // 修改回显必须先拿到完整属性字典，否则步骤属性会退化成物理列名，
    // 触发对象取值来源和前置检查下拉也无法正确选中。
    await Promise.all([loadProperties(), loadRelations()])
    form.value.needsApproval = Boolean(form.value.needsApproval) || Number(form.value.approvalLevels) > 0
    executionMode.value = form.value.needsApproval ? 'MANUAL_CONFIRM' : 'DIRECT'
    form.value.approvalLevels = form.value.needsApproval ? 1 : 0
    manualApproverId.value = parseManualApprover(form.value.approvalReviewers)
    parseSubmissionCriteria(form.value.submissionCriteria)
    if (form.value.actionType === 'COMPOSITE') {
      // 多目标步骤中的占位符都相对于触发对象取值，先加载触发对象属性，再回显各目标步骤。
      await loadConceptProperties(form.value.conceptId)
      await loadExecutionSteps(form.value.executionSteps ?? form.value.execution_steps)
    } else if (form.value.actionType !== 'FUNCTION') {
      await loadConceptProperties(form.value.conceptId)
      const cfgList = parseParamConfig(form.value.paramConfig)
      // 目标属性回显（含旧动作无 condition 标记的配置）
      const targetCfgs = cfgList.filter(c => !c.condition)
      const condCfgs = cfgList.filter(c => c.condition)
      if (targetCfgs.length) {
        targetCfgs.forEach(c => {
          const opt = propertyOptions.value.find(o => o.code === c.propertyCode)
          paramRows.value.push({
            propertyId: opt ? opt.value : undefined,
            propertyCode: c.propertyCode,
            propertyLabel: opt ? opt.label : c.propertyCode,
            valueMode: ['direct', 'placeholder', 'relative', 'expression'].includes(c.valueMode) ? c.valueMode : 'direct',
            valueTemplate: c.valueTemplate || '',
            relativeOperator: c.relativeOperator === 'ADD' ? 'ADD' : 'SUBTRACT',
            required: !!c.required
          })
        })
      }
      // 条件字段回显（动态行）
      conditionRows.value = condCfgs.map(c => {
        const opt = propertyOptions.value.find(o => o.code === c.propertyCode)
        return {
          propertyId: opt ? opt.value : undefined,
          propertyCode: c.propertyCode,
          propertyLabel: opt ? opt.label : c.propertyCode,
          operator: c.conditionOperator || 'eq',
          link: ['AND', 'OR'].includes(c.conditionLink) ? c.conditionLink : 'AND',
          negate: !!c.conditionNegate,
          valueMode: 'placeholder',
          valueTemplate: `\${${c.propertyCode}}`,
          required: true
        }
      })
    } else {
      // FUNCTION：加载函数参数声明，并按 param_config 回显字段映射
      formFunctionParams.value = []
      functionParamMappings.value = []
      sourcePropertyOptions.value = []
      const fieldMapByParam = {}
      parseParamConfig(form.value.paramConfig).forEach(c => {
        if (c.kind === 'field' && c.paramName) fieldMapByParam[c.paramName] = c
      })
      if (form.value.functionId) {
        try {
          const fn = (await getFunction(form.value.functionId)).data || {}
          const params = parseFunctionParams(fn.params)
          formFunctionParams.value = params
          functionParamMappings.value = params.map(name => {
            const saved = fieldMapByParam[name]
            return {
              paramName: name,
              kind: (saved && saved.sourcePropertyCode) ? 'field' : 'value',
              sourcePropertyCode: saved ? saved.sourcePropertyCode : undefined,
              sourcePropertyId: saved ? saved.sourcePropertyId : undefined
            }
          })
        } catch {
          formFunctionParams.value = []
          functionParamMappings.value = []
        }
      }
      if (form.value.sourceConceptId) {
        await loadSourcePropertiesFor(form.value.sourceConceptId)
      }
    }
    open.value = true
    title.value = '修改动作'
  })
}

function submitForm() {
  proxy.$refs['actionRef'].validate().then(() => {
    // 触发来源由每次运行传入，不在动作定义中选择。
    form.value.triggerRef = undefined
    try {
      form.value.submissionCriteria = buildSubmissionCriteriaJson()
    } catch (e) {
      proxy.$modal.msgError(e.message)
      return
    }
    const type = form.value.actionType
    if (type === 'FUNCTION') {
      if (!form.value.functionId) {
        proxy.$modal.msgError('请选择共享函数')
        return
      }
      // 入参字段映射：把 kind=field 的入参映射序列化到 param_config（固定值入参不写入，执行时手填）
      const fieldMappings = functionParamMappings.value
        .filter(m => m.kind === 'field' && m.sourcePropertyCode)
        .map(m => ({ paramName: m.paramName, kind: 'field', sourcePropertyCode: m.sourcePropertyCode, sourcePropertyId: m.sourcePropertyId }))
      form.value.paramConfig = fieldMappings.length ? JSON.stringify(fieldMappings) : undefined
      form.value.executionSteps = undefined
    } else if (type === 'COMPOSITE') {
      if (!form.value.conceptId) {
        proxy.$modal.msgError('请选择触发对象类型')
        return
      }
      try {
        form.value.executionSteps = buildExecutionStepsJson()
      } catch (e) {
        proxy.$modal.msgError(e.message)
        return
      }
      form.value.paramConfig = undefined
    } else {
      if (!form.value.conceptId) {
        proxy.$modal.msgError('请选择触发对象类型')
        return
      }
      // 更新/删除必须添加至少一个对象定位条件。
      if (type === 'UPDATE' || type === 'DELETE') {
        const condCount = conditionRows.value.filter(r => r.propertyCode).length
        if (!condCount) {
          proxy.$modal.msgError(type === 'DELETE'
            ? '删除动作必须至少添加 1 个条件'
            : '更新动作必须至少添加 1 个条件（建议使用主属性）')
          return
        }
      }
      form.value.paramConfig = buildParamConfigJson()
      form.value.executionSteps = undefined
    }
    form.value.needsApproval = executionMode.value === 'MANUAL_CONFIRM'
    form.value.approvalLevels = form.value.needsApproval ? 1 : 0
    form.value.approvalReviewers = buildSingleApprovalReviewerJson()
    form.value.ontologyId = props.ontologyId
    const fn = form.value.id ? updateAction : addAction
    fn(form.value).then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
      emit('changed')
    })
  }).catch(() => {})
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除动作"' + row.name + '"？').then(() => {
    return delAction(row.id)
  }).then(() => {
    // getList 内部已联动刷新执行记录（更新 currentActionIds 后重查），无需重复调用
    getList()
    proxy.$modal.msgSuccess('删除成功')
    emit('changed')
  }).catch(() => {})
}

function cancel() {
  open.value = false
  reset()
}

/* ================= 提交执行 ================= */

const execOpen = ref(false)
const execSaving = ref(false)
const execAction = ref({})
const execObjectKey = ref('')
const inputParams = ref('{}')
const execObjectSet = ref(null)
const execObjectRows = ref([])
const execObjectColumns = ref([])
const execObjectLoading = ref(false)
const execObjectSelectedKeys = ref([])
const execSelectedObject = ref(null)
const execObjectFilterFields = ref([])
const execObjectFilterSpec = ref({ groups: [{ connector: 'AND', filters: [] }], orderBy: [], columns: [], keyword: '' })
const execObjectPagination = reactive({ current: 1, pageSize: 8, total: 0, showSizeChanger: false })
// 动作参数配置（PARAM_CONFIG）：配置驱动时按属性渲染表单，否则回退手工JSON
const execParamConfigs = ref([])
const execFormValues = reactive({})

const hasParamConfig = computed(() => execParamConfigs.value.length > 0)

// propertyCode -> 属性友好名称（执行弹窗展示用）
const propertyCodeNameMap = computed(() => {
  const map = {}
  Object.values(propertyDict.value).forEach(p => {
    map[p.code] = p.name || p.code
  })
  return map
})

function recordValue(record, property) {
  if (!record || !property) return undefined
  const candidates = [property.physicalColumnName, property.propertyCode, property.code].filter(Boolean)
  for (const candidate of candidates) {
    if (Object.prototype.hasOwnProperty.call(record, candidate)) return record[candidate]
    const actualKey = Object.keys(record).find(key => String(key).toLowerCase() === String(candidate).toLowerCase())
    if (actualKey !== undefined) return record[actualKey]
  }
  return undefined
}

// 入参引用型：执行时需用户填写
const execPlaceholderConfigs = computed(() =>
  execParamConfigs.value
    .filter(c => c.valueMode === 'placeholder'
      || (c.valueMode === 'relative' && /^\$\{[^}]+\}$/.test(String(c.valueTemplate || '').trim())))
    .map(c => ({
      ...c,
      paramName: stripPlaceholder(c.valueTemplate),
      propName: propertyCodeNameMap.value[c.propertyCode] || c.propertyCode,
      required: c.valueMode === 'relative' || !!c.required
    }))
)

// 条件字段的占位符由所选对象自动提供；这里只让用户填写真正的动作输入（如新状态）。
const execEditablePlaceholderConfigs = computed(() =>
  execPlaceholderConfigs.value.filter(c => !c.condition)
)

const execConditionValueConfigs = computed(() => {
  const properties = execObjectSet.value?.properties || []
  return execParamConfigs.value.filter(c => c.condition).map((cfg, index) => {
    const sourceCode = stripPlaceholder(cfg.valueTemplate) || cfg.propertyCode
    const sourceProperty = properties.find(p => p.propertyCode === sourceCode)
    const operator = conditionOperatorOptions.find(option => option.value === (cfg.conditionOperator || 'eq'))
    return {
      key: `${cfg.targetConceptId || ''}-${cfg.propertyCode}-${index}`,
      targetName: propertyCodeNameMap.value[cfg.propertyCode] || cfg.propertyCode,
      sourceName: sourceProperty?.propertyName || sourceCode,
      operatorText: operator?.label || '等于',
      value: recordValue(execSelectedObject.value, sourceProperty)
    }
  })
})

const execObjectRowSelection = computed(() => ({
  type: 'radio',
  selectedRowKeys: execObjectSelectedKeys.value,
  onChange: (keys, rows) => selectExecObject(keys, rows)
}))

// 固定值 / 表达式型：只读展示
const execFixedConfigs = computed(() =>
  execParamConfigs.value
    .filter(c => !(c.valueMode === 'placeholder'
      || (c.valueMode === 'relative' && /^\$\{[^}]+\}$/.test(String(c.valueTemplate || '').trim()))))
    .map(c => ({
      ...c,
      propName: propertyCodeNameMap.value[c.propertyCode] || c.propertyCode,
      modeText: c.valueMode === 'direct' ? '固定值'
        : c.valueMode === 'relative' ? '当前值运算' : '表达式',
      display: c.valueMode === 'relative'
        ? `当前值 ${c.relativeOperator === 'ADD' ? '+' : '-'} ${c.valueTemplate}` : c.valueTemplate
    }))
)

function objectKeyOf(os, record) {
  const pks = (os?.properties || []).filter(p => p.isPrimary && p.physicalColumnName)
  if (!pks.length) return ''
  if (pks.length === 1) {
    const value = recordValue(record, pks[0])
    return value === undefined || value === null ? '' : String(value)
  }
  const key = {}
  pks.forEach(p => { key[p.propertyCode || p.physicalColumnName] = recordValue(record, p) })
  return JSON.stringify(key)
}

function buildExecObjectMeta(os) {
  execObjectFilterFields.value = (os?.properties || [])
    .filter(p => p.physicalColumnName)
    .map(p => ({ label: p.propertyName || p.physicalColumnName, value: p.physicalColumnName }))
  execObjectColumns.value = (os?.properties || [])
    .filter(p => p.physicalColumnName)
    .map(p => ({
      title: p.propertyName || p.physicalColumnName,
      dataIndex: p.physicalColumnName,
      key: p.physicalColumnName,
      ellipsis: true
    }))
}

async function loadExecObjectSet() {
  execObjectSet.value = null
  execObjectRows.value = []
  if (!execAction.value.conceptId) return
  const res = await listObjectSets(props.ontologyId)
  const sets = res.data || []
  execObjectSet.value = sets.find(os => String(os.conceptId) === String(execAction.value.conceptId)) || null
  if (!execObjectSet.value) return
  buildExecObjectMeta(execObjectSet.value)
  await loadExecObjects()
}

async function loadExecObjects() {
  const os = execObjectSet.value
  if (!os) return
  execObjectLoading.value = true
  try {
    const res = await queryObjects({
      ontologyId: props.ontologyId,
      conceptId: os.conceptId,
      tableBindingId: os.tableBindingId,
      pageNum: execObjectPagination.current,
      pageSize: execObjectPagination.pageSize,
      filters: JSON.stringify(execObjectFilterSpec.value)
    })
    const data = res.data || {}
    execObjectRows.value = (data.rows || []).map((record, index) => ({
      ...record,
      __objectKey: objectKeyOf(os, record) || `unbound-${execObjectPagination.current}-${index}`
    }))
    execObjectPagination.total = Number(data.total) || 0
  } finally {
    execObjectLoading.value = false
  }
}

function selectExecObject(keys, rows) {
  execObjectSelectedKeys.value = keys || []
  execSelectedObject.value = rows && rows.length ? rows[0] : null
  execObjectKey.value = execSelectedObject.value && execObjectSet.value
    ? objectKeyOf(execObjectSet.value, execSelectedObject.value) : ''
  if (!execSelectedObject.value || !execObjectSet.value) return
  const propertyByCode = {}
  ;(execObjectSet.value.properties || []).forEach(p => { propertyByCode[p.propertyCode] = p })
  execPlaceholderConfigs.value.filter(c => c.condition).forEach(cfg => {
    const sourceCode = stripPlaceholder(cfg.valueTemplate) || cfg.propertyCode
    const property = propertyByCode[sourceCode]
    if (property) {
      execFormValues[cfg.paramName] = recordValue(execSelectedObject.value, property)
    }
  })
}

function handleExecObjectTableChange(pagination) {
  execObjectPagination.current = pagination.current
  loadExecObjects()
}

function openSubmitExec(row) {
  execAction.value = row
  execObjectKey.value = ''
  inputParams.value = '{}'
  execParamConfigs.value = row.actionType === 'COMPOSITE'
    ? flattenExecutionStepConfigs(row.executionSteps)
    : parseParamConfig(row.paramConfig)
  execObjectSet.value = null
  execObjectRows.value = []
  execObjectSelectedKeys.value = []
  execSelectedObject.value = null
  execObjectPagination.current = 1
  execObjectFilterSpec.value = { groups: [{ connector: 'AND', filters: [] }], orderBy: [], columns: [], keyword: '' }
  Object.keys(execFormValues).forEach(k => delete execFormValues[k])
  if (row.actionType === 'FUNCTION') {
    const functionId = row.functionId
    functionParamsMap.value[row.id] = []
    if (functionId) {
      getFunction(functionId).then(res => {
        const fn = res.data || {}
        functionParamsMap.value[row.id] = parseFunctionParams(fn.params)
      }).catch(() => {
        functionParamsMap.value[row.id] = []
      })
    }
  }
  execOpen.value = true
  if (row.actionType !== 'FUNCTION' && row.actionType !== 'CREATE') {
    loadExecObjectSet().catch(() => {})
  }
}

function submissionSuccessText(record) {
  if (record && record.status === 'PENDING_APPROVAL') return '已提交，等待人工确认'
  if (record && record.autoExecute) return '已提交，正在自动执行'
  return '已提交'
}

function submitExec() {
  if (execAction.value.actionType === 'FUNCTION') {
    const params = {}
    ;(execInputParamsList.value || []).forEach(name => {
      const v = execFormValues[name]
      if (v !== undefined && v !== null && v !== '') params[name] = v
    })
    const input = JSON.stringify(params)
    execSaving.value = true
    submitExecution({ actionId: execAction.value.id, inputParams: input, objectKey: execObjectKey.value || undefined }).then(res => {
      proxy.$modal.msgSuccess(submissionSuccessText(res.data))
      execOpen.value = false
      loadExecutions()
    }).finally(() => {
      execSaving.value = false
    })
    return
  }
  if (execAction.value.actionType !== 'CREATE' && (!execSelectedObject.value || !execObjectKey.value)) {
    proxy.$modal.msgError('请先选择一个触发对象')
    return
  }
  let params = {}
  if (hasParamConfig.value) {
    // 条件参数来自所选对象；目标值参数由用户填写。
    execPlaceholderConfigs.value.forEach(cfg => {
      const v = execFormValues[cfg.paramName]
      if (v !== undefined && v !== null && v !== '') params[cfg.paramName] = v
    })
    const missing = execEditablePlaceholderConfigs.value.filter(cfg => cfg.required && params[cfg.paramName] === undefined)
    if (missing.length) {
      proxy.$modal.msgError('缺少必填参数: ' + missing.map(m => m.propName).join('、'))
      return
    }
  } else {
    // 未配置目标属性的旧动作：保留 JSON 手工输入
    const raw = (inputParams.value || '').trim()
    if (raw) {
      try {
        params = JSON.parse(raw)
      } catch (e) {
        proxy.$modal.msgError('输入参数必须是合法的 JSON')
        return
      }
    }
  }
  execSaving.value = true
  submitExecution({ actionId: execAction.value.id, inputParams: JSON.stringify(params), objectKey: execObjectKey.value || undefined }).then(res => {
    proxy.$modal.msgSuccess(submissionSuccessText(res.data))
    execOpen.value = false
    loadExecutions()
  }).finally(() => {
    execSaving.value = false
  })
}

/* ================= 审批与执行 ================= */

// 单次人工确认：意见可选，结论永久审计
const approvalOpen = ref(false)
const approvalSaving = ref(false)
const approvalRecord = ref({})
const approvalDecision = ref('approve') // 'approve' | 'reject'
const approvalReason = ref('')
const approvalTitle = computed(() => (approvalDecision.value === 'approve' ? '确认执行' : '确认不执行'))

function openApproval(record, decision) {
  approvalRecord.value = record
  approvalDecision.value = decision
  approvalReason.value = ''
  approvalOpen.value = true
}

function submitApproval() {
  const reason = (approvalReason.value || '').trim()
  approvalSaving.value = true
  const fn = approvalDecision.value === 'approve' ? approveExecution : rejectExecution
  fn({ executionId: approvalRecord.value.id, approvalReason: reason }).then(() => {
    proxy.$modal.msgSuccess(approvalDecision.value === 'approve' ? '已确认，等待自动执行' : '已确认不执行')
    approvalOpen.value = false
    loadExecutions()
  }).catch(() => {}).finally(() => {
    approvalSaving.value = false
  })
}

/* ================= 人工确认审计记录 ================= */

const chainOpen = ref(false)
const chainData = ref(null)

function handleViewChain(record) {
  chainOpen.value = true
  chainData.value = null
  getApprovalChain(record.id).then(res => {
    chainData.value = res.data || null
  }).catch(() => {
    chainData.value = null
  })
}

function chainStatusText(s) {
  return { PENDING: '待审批', APPROVED: '已批准', REJECTED: '已拒绝', CANCELLED: '已取消' }[s] || s
}

function chainStatusColor(s) {
  return { PENDING: 'orange', APPROVED: 'green', REJECTED: 'red', CANCELLED: 'default' }[s] || 'default'
}

function chainDecisionText(d) {
  return { APPROVE: '同意', REJECT: '拒绝' }[d] || d
}

const chainReviewerColumns = [
  { title: '审阅人', dataIndex: 'reviewerName', key: 'reviewerName', align: 'center', width: 120 },
  { title: '决策', dataIndex: 'decision', key: 'decision', align: 'center', width: 90 },
  { title: '理由', dataIndex: 'reason', key: 'reason', align: 'left', ellipsis: true },
  { title: '时间', dataIndex: 'decideTime', key: 'decideTime', align: 'center', width: 160 }
]

function handleRun(record) {
  proxy.$modal.confirm('确认执行该记录？执行后数据将变更。').then(() => {
    return runExecution(record.id)
  }).then(() => {
    proxy.$modal.msgSuccess('执行完成')
    loadExecutions()
  }).catch(() => {})
}

function handleRollback(record) {
  proxy.$modal.confirm('确认回退该记录？将按执行前快照还原数据并留痕。').then(() => {
    return rollbackExecution(record.id)
  }).then(() => {
    proxy.$modal.msgSuccess('回退完成')
    loadExecutions()
  }).catch(() => {})
}

const resultOpen = ref(false)
const currentResult = ref({})
// 当前执行记录对应的「物理列 → 本体属性」映射（对比表左侧按属性展示）
const resultColumnMap = ref({})
// 当前执行结果是否为函数类动作
const resultIsFunction = computed(() => actionTypeMap.value[currentResult.value.actionId] === 'FUNCTION')

function handleViewResult(record) {
  currentResult.value = record
  resultOpen.value = true
  loadResultColumnMap(record.actionId)
}

// 由执行记录 actionId → 动作 → 概念 → 概念表列映射 → 属性名，构建物理列→属性字典
async function loadResultColumnMap(actionId) {
  resultColumnMap.value = {}
  if (!actionId) return
  try {
    const res = await getAction(actionId)
    const action = res.data || {}
    const conceptId = action.conceptId
    if (!conceptId) return
    const tableRes = await listConceptTable({ conceptId })
    const tables = (tableRes.data && tableRes.data.rows) || tableRes.data || []
    if (!tables.length) return
    const colRes = await listPropertyColumn(tables[0].id)
    const cols = (colRes.data && colRes.data.rows) || colRes.data || []
    const map = {}
    cols.forEach(c => {
      const p = propertyDict.value[c.propertyId] || {}
      const code = p.code || c.columnName
      map[c.columnName] = {
        name: p.name || code,
        columnName: c.columnName,
        primary: !!p.isPrimary
      }
    })
    resultColumnMap.value = map
  } catch (e) {
    // 映射失败时对比表回退展示物理列名
  }
}

// 执行前后数据解析 → 属性对比表
function parseResultRows(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

function formatCellVal(v) {
  if (v === null || v === undefined) return '—'
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

function formatJson(value) {
  if (!value) return ''
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return JSON.stringify(parsed, null, 2)
  } catch (e) {
    return String(value)
  }
}

const beforeRows = computed(() => parseResultRows(currentResult.value.beforeData))
const afterRows = computed(() => parseResultRows(currentResult.value.afterData))

// 回退前后数据快照：
// 兼容两种来源——旧版 ROLLED_BACK 记录存于 rollbackBeforeData/rollbackAfterData；
// 新版回退记录(beforeData=回退前/afterData=回退后)未显式写 rollback* 字段，回退取 beforeData/afterData
const rollbackBeforeRows = computed(() => {
  const direct = parseResultRows(currentResult.value.rollbackBeforeData)
  return direct.length ? direct : parseResultRows(currentResult.value.beforeData)
})
const rollbackAfterRows = computed(() => {
  const direct = parseResultRows(currentResult.value.rollbackAfterData)
  return direct.length ? direct : parseResultRows(currentResult.value.afterData)
})

const isRolledBack = computed(() => currentResult.value.status === 'ROLLED_BACK')

// 对比行构建（执行前后 / 回退前后共用）：多行快照逐行展平，左侧列仅展示已映射到本体属性的列
function buildCompareRows(beforeArr, afterArr) {
  const maxRows = Math.max(beforeArr.length, afterArr.length)
  const rows = []
  for (let i = 0; i < maxRows; i++) {
    const b = beforeArr[i] || {}
    const a = afterArr[i] || {}
    const keys = []
    const seen = {}
    ;[...Object.keys(b), ...Object.keys(a)].forEach(k => {
      if (!seen[k]) {
        seen[k] = 1
        keys.push(k)
      }
    })
    keys.forEach(k => {
      // 仅展示已映射到本体属性的列：未映射的物理列不进入对比表
      const mapped = resultColumnMap.value[k]
      if (!mapped || !mapped.name) return
      rows.push({
        colKey: k,
        name: maxRows > 1 ? `第${i + 1}行 · ${mapped.name}` : mapped.name,
        before: formatCellVal(b[k]),
        after: formatCellVal(a[k]),
        changed: JSON.stringify(b[k] ?? null) !== JSON.stringify(a[k] ?? null)
      })
    })
  }
  return rows
}

// 执行前后属性对比表
const compareRows = computed(() => buildCompareRows(beforeRows.value, afterRows.value))

// 回退前后属性对比表
const rollbackCompareRows = computed(() => buildCompareRows(rollbackBeforeRows.value, rollbackAfterRows.value))

const compareRowHint = computed(() => {
  const n = Math.max(beforeRows.value.length, afterRows.value.length)
  return n > 1 ? `（共 ${n} 行）` : ''
})

const rollbackCompareRowHint = computed(() => {
  const n = Math.max(rollbackBeforeRows.value.length, rollbackAfterRows.value.length)
  return n > 1 ? `（共 ${n} 行）` : ''
})

// 对比表列定义（左侧属性名 → 修改前 → 修改后）
const compareColumns = [
  { title: '属性', dataIndex: 'name', width: 160 },
  { title: '修改前', dataIndex: 'before' },
  { title: '修改后', dataIndex: 'after' }
]

// 有值变化的行高亮显示
function compareRowClassName(record) {
  return record.changed ? 'changed-row' : ''
}

// 执行前/执行后数据集表格列：仅展映已映射到本体属性的列（表头=属性名，dataIndex=物理列名）
const snapshotColumns = computed(() => {
  const src = beforeRows.value[0] || afterRows.value[0] || {}
  return Object.keys(src)
    .filter(k => resultColumnMap.value[k] && resultColumnMap.value[k].name)
    .map(k => ({ title: resultColumnMap.value[k].name, dataIndex: k, width: 160 }))
})

// 回退前/回退后数据集表格列（回退快照独立取列，避免执行快照为空时无列可用）
const rollbackSnapshotColumns = computed(() => {
  const src = rollbackBeforeRows.value[0] || rollbackAfterRows.value[0] || {}
  return Object.keys(src)
    .filter(k => resultColumnMap.value[k] && resultColumnMap.value[k].name)
    .map(k => ({ title: resultColumnMap.value[k].name, dataIndex: k, width: 160 }))
})

defineExpose({
  reload() {
    getList()
    loadExecutions()
  }
})

loadConcepts()
loadProperties()
loadFunctions()
loadRelations()
loadMembers()
getList()
onBeforeUnmount(clearExecutionRefresh)
</script>

<style lang="scss" scoped>
.action-panel {
  .panel-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    .toolbar-right {
      margin-left: auto;
    }
  }

  .sql-cell {
    font-family: monospace;
    font-size: 12px;
  }

  .trigger-cell {
    display: flex;
    align-items: center;
    gap: 4px;
    min-width: 0;

    code {
      min-width: 0;
      max-width: 130px;
      overflow: hidden;
      color: #586174;
      font-size: 11px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .event-id {
      max-width: 150px;
    }
  }

  .reconciliation-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    color: #fff;
    font-weight: 700;
    background: #fa541c;
    border-radius: 50%;
    cursor: help;
  }

  .exec-divider {
    margin: 16px 0 12px;
  }

  .modal-hint-line {
    margin: 0 0 8px;
    color: #8a95a6;
    font-size: 12px;
  }

  .action-form-compact {
    max-height: calc(100vh - 210px);
    padding-right: 6px;
    overflow-y: auto;
  }

  .action-editor-section {
    margin-bottom: 14px;
    padding: 14px 14px 2px;
    border: 1px solid #e5eaf2;
    border-radius: 8px;
    background: #fff;

    .action-editor-section-title {
      margin: -14px -14px 14px;
      padding: 10px 14px;
      color: #1f2d3d;
      font-size: 14px;
      font-weight: 600;
      border-bottom: 1px solid #eef1f6;
      background: #f7f9fc;
      border-radius: 8px 8px 0 0;
    }
  }

  .action-basic-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    column-gap: 18px;

    :deep(.ant-form-item) {
      margin-bottom: 12px;
    }

    .action-grid-full {
      grid-column: 1 / -1;
    }
  }

  .action-control-section {
    margin-top: 14px;
  }

  .condition-value-preview {
    width: 100%;
    margin-top: 8px;
    padding: 8px 10px;
    border: 1px solid #d9e2f0;
    border-radius: 6px;
    background: #f7faff;

    .condition-value-title {
      margin-bottom: 6px;
      color: #555;
      font-size: 12px;
      font-weight: 600;
    }

    .condition-value-row {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 3px 0;
      font-size: 12px;

      .condition-source-name {
        color: #8a95a6;
      }

      code {
        color: #1677ff;
        font-weight: 600;
      }
    }
  }

  .trigger-alert {
    margin-top: 8px;
    font-size: 12px;
  }

  .precondition-config-block {
    width: 100%;
    border: 1px solid #d9e2f0;
    border-radius: 6px;
    overflow: hidden;

    .precondition-block-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      padding: 10px 12px;
      background: #f6f9fe;
      border-bottom: 1px solid #e5eaf2;

      .precondition-block-title {
        margin-bottom: 2px;
        color: #1f2d3d;
        font-size: 13px;
        font-weight: 600;
      }

      .modal-hint-line {
        margin-bottom: 0;
      }
    }

    .precondition-head-actions {
      display: flex;
      flex-shrink: 0;
      gap: 8px;
    }

    .precondition-row {
      padding: 10px 12px;
      border-top: 1px solid #eef1f6;

      &:first-of-type {
        border-top: none;
      }
    }

    .precondition-row-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 6px;
    }

    .precondition-idx {
      color: #1677ff;
      font-size: 12px;
      font-weight: 600;
    }

    .precondition-row-body {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
    }

    .precondition-preview {
      margin-top: 5px;
      color: #8a95a6;
      font-family: monospace;
      font-size: 11px;
    }
  }

  .param-config-block {
    width: 100%;
    border: 1px solid #e5eaf2;
    border-radius: 6px;
    overflow: hidden;

    .param-block-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 12px;
      border-bottom: 1px solid #eef1f6;

      .param-block-title {
        font-size: 13px;
        font-weight: 600;
        color: #333;
      }
    }

    .param-idx {
      font-size: 12px;
      font-weight: 600;
      color: #555;
    }

    .param-prop-select {
      width: 200px;
    }

    .param-row {
      padding: 10px 12px;
      border-bottom: 1px solid #eef1f6;

      &:last-child {
        border-bottom: none;
      }

      .param-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;

        .param-name {
          font-size: 13px;
          font-weight: 600;
          color: #333;
        }

        .param-actions {
          display: flex;
          align-items: center;
          gap: 12px;

          :deep(.ant-checkbox-wrapper) {
            font-size: 12px;
          }
        }
      }

      .param-body {
        display: flex;
        align-items: flex-start;
        gap: 8px;

        :deep(.ant-radio-group) {
          flex-shrink: 0;
        }

        :deep(.ant-input),
        textarea.ant-input {
          flex: 1;
        }
      }
    }
  }

  .approver-config-block {
    width: 100%;
    border: 1px solid #e5eaf2;
    border-radius: 6px;
    padding: 10px 12px;

    .approver-row {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 0;
      border-bottom: 1px dashed #eef1f6;

      &:last-child {
        border-bottom: none;
      }

      .approver-stage {
        flex-shrink: 0;
        width: 48px;
        font-size: 12px;
        font-weight: 600;
        color: #555;
      }
    }
  }

  .chain-task-approver {
    margin-left: auto;
    font-size: 12px;
    color: #8a95a6;
  }

  .condition-config-block {
    width: 100%;
    border: 1px solid #e5eaf2;
    border-radius: 6px;
    padding: 10px 12px;

    .condition-block-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;

      .condition-block-title {
        font-size: 13px;
        font-weight: 600;
        color: #333;
      }
    }

    .condition-row {
      padding: 10px 12px;
      border: 1px solid #eef1f6;
      border-radius: 6px;
      margin-bottom: 10px;
      background: #fafbfc;

      &:last-child {
        margin-bottom: 0;
      }

      .condition-row-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;

        .condition-idx {
          font-size: 12px;
          font-weight: 600;
          color: #555;
        }
      }

      .condition-prop-select {
        width: 170px;
      }

      .condition-link-select {
        width: 90px;
      }

      .condition-operator-select {
        width: 110px;
      }
    }
  }

  .execution-step-list {
    padding: 4px 0 12px;
  }

  .execution-step-card {
    width: calc(100% - 24px);
    max-width: 820px;
    margin: 12px auto 0;
    padding: 0 14px 14px;
    border: 1px solid #d9e2f0;
    border-radius: 8px;
    background: #fff;
    box-shadow: 0 2px 8px rgb(31 56 88 / 6%);

    .step-card-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      min-height: 42px;
      margin: 0 -14px;
      padding: 0 12px 0 14px;
      border-bottom: 1px solid #e8edf5;
      border-radius: 8px 8px 0 0;
      background: #f5f8fd;
    }

    .step-number {
      display: inline-flex;
      align-items: center;
      min-width: 48px;
      height: 22px;
      margin-right: 8px;
      padding: 0 8px;
      color: #1677ff;
      font-size: 12px;
      font-weight: 600;
      background: #e6f4ff;
      border-radius: 11px;
    }

    .step-summary {
      color: #344054;
      font-size: 13px;
      font-weight: 600;
    }

    .step-basic-row {
      display: grid;
      grid-template-columns: minmax(180px, 1.25fr) 140px minmax(180px, 1fr);
      gap: 12px;
      margin-top: 12px;
    }

    .step-field {
      display: flex;
      min-width: 0;
      flex-direction: column;
      gap: 5px;
      color: #667085;
      font-size: 12px;

      :deep(.ant-select),
      :deep(.ant-input) {
        width: 100%;
      }
    }

    .step-section-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 14px;
      padding-top: 10px;
      color: #475467;
      font-size: 12px;
      font-weight: 600;
      border-top: 1px dashed #d9e2f0;
    }

    .step-config-row {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      margin-top: 8px;
    }

    .step-param-card {
      margin-top: 8px;
      padding: 10px;
      border: 1px solid #edf0f5;
      border-radius: 6px;
      background: #fafbfc;
    }

    .step-param-card-head {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 10px;
    }

    .step-value-mode-group {
      flex: 1;
      min-width: 350px;
    }

    .step-param-actions {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-left: auto;
    }

    .step-param-value-row {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px dashed #e4e7ec;
    }

    .step-value-label {
      width: 48px;
      padding-top: 5px;
      color: #667085;
      font-size: 12px;
    }

    .step-property-select {
      width: 210px;
    }

    .step-mode-select,
    .step-operator-select {
      width: 115px;
    }

    .step-relative-select,
    .step-link-select {
      width: 105px;
    }

    .step-value-input {
      width: 180px;
      flex: 0 0 180px;
    }

    .step-condition-card {
      margin-top: 8px;
      padding: 8px 10px;
      border: 1px solid #edf0f5;
      border-radius: 6px;
      background: #fafbfc;
    }

    .step-condition-row {
      margin-top: 0;
    }

    .condition-row-number {
      min-width: 42px;
      color: #667085;
      font-size: 12px;
      font-weight: 600;
    }

    .step-condition-source {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 8px;
      padding-top: 8px;
      color: #667085;
      font-size: 12px;
      border-top: 1px dashed #e4e7ec;

      > span {
        width: 96px;
        flex: 0 0 96px;
      }

      :deep(.ant-select) {
        width: 230px;
      }

      code {
        max-width: 190px;
        padding: 2px 7px;
        overflow: hidden;
        color: #1677ff;
        background: #eef6ff;
        border-radius: 4px;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    :deep(.ant-empty-normal) {
      margin-block: 10px 4px;
    }

    @media (max-width: 900px) {
      .step-basic-row {
        grid-template-columns: 1fr 1fr;
      }

      .step-name-field {
        grid-column: 1 / -1;
      }

      .step-value-mode-group {
        min-width: 100%;
      }
    }
  }

  .fixed-param-lines {
    padding: 4px 0 8px;

    .fixed-param-line {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px 0;
      font-size: 12px;

      .fixed-param-name {
        min-width: 90px;
        color: #333;
        font-weight: 600;
      }

      .fixed-param-mode {
        color: #8a95a6;
      }

      .fixed-param-tag {
        flex-shrink: 0;
        padding: 0 6px;
        background: #fff7e6;
        border: 1px solid #ffd591;
        color: #d46b08;
        border-radius: 3px;
        font-size: 12px;
        line-height: 18px;
      }

      code {
        padding: 2px 6px;
        background: #f6f8fa;
        border: 1px solid #e5eaf2;
        border-radius: 4px;
        word-break: break-all;
      }
    }
  }

  .result-section {
    margin-bottom: 12px;

    // a-table 行级变化高亮（row-class-name 返回 changed-row）
    :deep(.changed-row) {
      td {
        background: #fff7e6;
      }

      td:first-child {
        color: #d46b08;
        font-weight: 600;
      }
    }

    .result-title {
      font-size: 12px;
      font-weight: 600;
      color: #555;
      margin-bottom: 4px;

      &.error {
        color: #cf1322;
      }
    }

    .result-pre {
      margin: 0;
      padding: 10px 12px;
      background: #f6f8fa;
      border: 1px solid #e5eaf2;
      border-radius: 6px;
      font-family: monospace;
      font-size: 12px;
      max-height: 220px;
      overflow: auto;
      white-space: pre-wrap;
      word-break: break-all;

      &.error-pre {
        background: #fff1f0;
        border-color: #ffa39e;
      }
    }
  }
}
</style>
