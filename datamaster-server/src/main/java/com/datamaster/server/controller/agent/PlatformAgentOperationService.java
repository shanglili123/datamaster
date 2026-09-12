package com.datamaster.server.controller.agent;

import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.common.utils.AesEncryptUtil;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSyncReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelSaveReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSaveReqVO;
import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.asset.IAssetsAssetSyncService;
import com.datamaster.module.assets.service.assetColumn.IAssetsAssetColumnService;
import com.datamaster.module.assets.service.assetchild.spaceRel.IAssetsAssetSpaceRelService;
import com.datamaster.module.assets.service.assetColumnSpaceRel.IAssetsAssetColumnSpaceRelService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceSpaceRelService;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceSpaceRelDO;
import com.datamaster.module.assets.service.dbgpt.IDbGptDatasourceSyncService;
import com.datamaster.module.ai.service.dbgpt.IDbGptSkillSyncService;
import com.datamaster.module.ai.controller.admin.skill.vo.AiOntologySkillGenerateReqVO;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import com.datamaster.module.ai.service.skill.IAiSkillService;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskSaveReqVO;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskScopeSaveReqVO;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.metadata.api.service.table.CatalogTableApiService;
import com.datamaster.metadata.service.task.ICatalogTaskService;
import com.datamaster.module.governance.api.cat.dto.CategoryReqDTO;
import com.datamaster.module.governance.api.cat.dto.CategoryRespDTO;
import com.datamaster.module.governance.api.service.cat.ITaxonomyCategoryApiService;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionSaveReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewRespVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateRespVO;
import com.datamaster.module.ontology.service.IActionService;
import com.datamaster.module.ontology.service.IOntologyGenerateService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 平台数据智能体运营流程。该流程不是页面导航，而是对已有模块服务的受控编排。
 */
@Slf4j
@Service
public class PlatformAgentOperationService {

    @Resource private IAssetsDatasourceService datasourceService;
    @Resource private IAssetsDatasourceSpaceRelService datasourceSpaceRelService;
    @Resource private IAssetsDatasourceApiService datasourceApiService;
    @Resource private IAssetsAssetSyncService assetSyncService;
    @Resource private IAssetsAssetService assetService;
    @Resource private IAssetsAssetColumnService assetColumnService;
    @Resource private IAssetsAssetSpaceRelService assetSpaceRelService;
    @Resource private IAssetsAssetColumnSpaceRelService assetColumnSpaceRelService;
    @Resource private ICatalogTaskService catalogTaskService;
    @Resource private CatalogTableApiService catalogTableApiService;
    @Resource private IOntologyGenerateService ontologyGenerateService;
    @Resource private IActionService actionService;
    @Resource private IAiSkillService aiSkillService;
    @Resource private IDbGptSkillSyncService skillSyncService;
    @Resource private IDbGptDatasourceSyncService datasourceSyncService;
    @Resource private ITaxonomyCategoryApiService categoryApiService;
    @Resource private IAiAgentOperationService operationPersistence;

    /** 服务重启后异步线程已经不存在，保留任务记录并明确标记为可恢复的中断状态。 */
    @PostConstruct
    public void markInterruptedOperations() {
        try {
            for (AiAgentOperationDO item : operationPersistence.listRunning()) {
                item.setStatus("INTERRUPTED");
                item.setMessage("服务重新启动，流程已保留，可从当前步骤继续或重试");
                item.setUpdateTime(new Date());
                operationPersistence.update(item);
            }
        } catch (Exception ex) {
            // 首次部署尚未执行迁移时不阻断服务启动，迁移完成后流程接口即可用。
            log.warn("数据智能体流程实例表尚未就绪，跳过中断任务恢复", ex);
        }
    }

    public OperationState start(PlatformAgentOperationReqVO request, Long userId, String userName) {
        request = completeConnectionFromGoal(request);
        DatasourceDO datasource = resolveDatasource(request);
        OperationState existing = findExistingDatabaseOperation(request, datasource);
        if (existing != null) {
            if (isRestartable(existing.getStatus())) {
                // “重新开始”可能只提交了新的目标描述，连接信息从原流程合并，避免旧数据源 ID 失效导致无法重建。
                PlatformAgentOperationReqVO restartRequest = mergeRequest(getRequest(existing.getId()), request);
                DatasourceDO restartDatasource = resolveDatasource(restartRequest);
                if (hasConnection(restartRequest)) {
                    return restartExisting(existing, restartRequest, restartDatasource, userId, userName);
                }
            }
            return existing;
        }
        OperationState state = new OperationState();
        state.setId(UUID.randomUUID().toString());
        state.setGoal(request == null ? null : request.getGoal());
        state.setDatasourceId(datasource == null ? request == null ? null : request.getDatasourceId() : datasource.getId());
        state.setDatasourceName(datasource == null ? request == null ? null : request.getDatasourceName() : datasource.getDatasourceName());
        state.setDatabaseName(resolveInitialDatabaseName(request, datasource));
        state.setStatus("RUNNING");
        state.setSteps(defaultSteps());
        try {
            persistNew(state, request, userId, userName);
        } catch (Exception createError) {
            OperationState concurrent = findExistingDatabaseOperation(request, datasource);
            if (concurrent != null) return concurrent;
            throw createError;
        }

        if (datasource == null && !hasConnection(request)) {
            state.setStatus("NEED_INPUT");
            state.setCurrentStep("确认数据源");
            state.setProgress(0);
            state.setMessage("没有找到目标数据源，请在当前流程中补充连接信息");
            state.setRequiredFields(requiredFields());
            persistState(state);
            return state;
        }
        final DatasourceDO resolved = datasource;
        final PlatformAgentOperationReqVO executionRequest = request;
        CompletableFuture.runAsync(() -> execute(state, executionRequest, resolved, userId, userName));
        return state;
    }

    /** 全局同一数据库只允许保留一个数据智能体构建流程。 */
    private OperationState findExistingDatabaseOperation(PlatformAgentOperationReqVO request,
                                                          DatasourceDO datasource) {
        if (request == null) return null;
        AiAgentOperationDO item = operationPersistence.findActiveByDatabaseName(resolveInitialDatabaseName(request, datasource));
        if (item != null) return toState(item);
        return null;
    }

    private boolean isRestartable(String status) {
        return "FAILED".equals(status) || "INTERRUPTED".equals(status)
                || "CANCELED".equals(status) || "NEED_INPUT".equals(status)
                || "NEED_CONFIRMATION".equals(status);
    }

    /** 复用同一数据库的流程主记录，从失效数据源重新开始，避免创建第二条流程。 */
    private OperationState restartExisting(OperationState state, PlatformAgentOperationReqVO request,
                                            DatasourceDO datasource, Long userId, String userName) {
        state.setGoal(request.getGoal());
        state.setDatasourceId(datasource == null ? null : datasource.getId());
        state.setDatasourceName(datasource == null ? first(request.getDatasourceName(), request.getDatabaseName()) : datasource.getDatasourceName());
        state.setDatabaseName(resolveInitialDatabaseName(request, datasource));
        state.setStatus("RUNNING");
        state.setProgress(0);
        state.setCurrentStep("确认数据源");
        state.setMessage("正在使用最新连接信息重新开始流程");
        state.setRequiredFields(new CopyOnWriteArrayList<>());
        state.setOutput(new CopyOnWriteArrayList<>());
        state.setSteps(defaultSteps());
        state.setResult(null);
        state.setAssetSyncMessage(null);
        state.setCatalogTaskId(null);
        state.setConfirmationType(null);
        state.setConfirmationMessage(null);
        state.setOntologyId(null);
        state.setActionCount(null);
        persistRequest(state.getId(), request);
        persistState(state);
        CompletableFuture.runAsync(() -> execute(state, request, datasource, userId, userName));
        return state;
    }

    private String operationDatabaseName(AiAgentOperationDO item) {
        if (item == null) return null;
        if (item.getDatabaseName() != null && !item.getDatabaseName().trim().isEmpty()) return item.getDatabaseName().trim();
        if (item.getDatasourceName() != null && !item.getDatasourceName().trim().isEmpty()) return item.getDatasourceName().trim();
        if (item.getRequestJson() == null) return null;
        PlatformAgentOperationReqVO request = JSON.parseObject(item.getRequestJson(), PlatformAgentOperationReqVO.class);
        return request == null ? null : first(request.getDatabaseName(), request.getDatasourceName());
    }

    public OperationState resume(String id, PlatformAgentOperationReqVO request, Long userId, String userName) {
        OperationState state = get(id);
        if (state == null) throw new IllegalArgumentException("流程实例不存在");
        if (!"NEED_INPUT".equals(state.getStatus())) throw new IllegalStateException("当前流程不需要补充信息");
        PlatformAgentOperationReqVO savedRequest = getRequest(id);
        final PlatformAgentOperationReqVO resumedRequest = mergeRequest(savedRequest, request);
        if (resumedRequest != null) state.setGoal(resumedRequest.getGoal());
        state.setStatus("RUNNING");
        state.setRequiredFields(Collections.emptyList());
        persistRequest(id, resumedRequest);
        persistState(state);
        DatasourceDO datasource = resolveDatasource(resumedRequest);
        state.setDatasourceId(datasource == null ? resumedRequest.getDatasourceId() : datasource.getId());
        state.setDatasourceName(datasource == null ? resumedRequest.getDatasourceName() : datasource.getDatasourceName());
        state.setDatabaseName(resolveInitialDatabaseName(resumedRequest, datasource));
        if (datasource == null && !hasConnection(resumedRequest)) {
            state.setStatus("NEED_INPUT");
            state.setMessage("连接信息仍不完整");
            state.setRequiredFields(requiredFields());
            persistState(state);
            return state;
        }
        CompletableFuture.runAsync(() -> execute(state, resumedRequest, datasource, userId, userName));
        return state;
    }

    public OperationState get(String id) {
        AiAgentOperationDO persisted = operationPersistence.get(id);
        return persisted == null ? null : toState(persisted);
    }

    public List<OperationState> listLatest(Long spaceId, String spaceCode, int limit) {
        List<OperationState> result = new ArrayList<>();
        // 全屏流程入口可能没有挂载顶部空间选择器；无空间上下文时仍返回流程主记录，避免列表为空白。
        for (AiAgentOperationDO item : operationPersistence.listLatest(spaceId, spaceCode, limit)) {
            result.add(toState(item));
        }
        return result;
    }

    public OperationState cancel(String id) {
        OperationState state = get(id);
        if (state == null) throw new IllegalArgumentException("流程实例不存在");
        if ("SUCCEEDED".equals(state.getStatus()) || "CANCELED".equals(state.getStatus())) return state;
        state.setStatus("CANCELED");
        state.setMessage("流程已取消");
        persistState(state);
        return state;
    }

    /** AI 步骤失败后的受控重试，保留已经完成的数据源、探查、资产步骤。 */
    public OperationState retry(String id, Long userId, String userName) {
        OperationState state = get(id);
        PlatformAgentOperationReqVO request = getRequest(id);
        if (state == null || request == null) throw new IllegalArgumentException("流程实例不存在");
        if (!"FAILED".equals(state.getStatus()) && !"INTERRUPTED".equals(state.getStatus())) {
            throw new IllegalStateException("当前流程不处于失败或中断状态");
        }
        request = completeConnectionFromGoal(request);
        persistRequest(id, request);
        DatasourceDO datasource = state.getDatasourceId() == null ? resolveDatasource(request)
                : datasourceService.getDatasourceDOById(state.getDatasourceId());
        boolean rebuildDatasource = datasource == null && hasConnection(request);
        if (datasource == null && !rebuildDatasource) throw new IllegalStateException("流程使用的数据源不存在，请重新提供连接信息");
        boolean retryFromDiscovery = false;
        if (rebuildDatasource) {
            state.setDatasourceId(null);
            state.setDatasourceName(first(request.getDatasourceName(), request.getDatabaseName()));
            state.setDatabaseName(resolveInitialDatabaseName(request, null));
            state.setProgress(0);
            state.setCurrentStep("确认数据源");
            state.setCatalogTaskId(null);
            state.setOntologyId(null);
            state.setActionCount(null);
            state.setResult(null);
            state.setSteps(defaultSteps());
        } else {
            datasource.setDatasourceType(normalizeDatasourceType(datasource.getDatasourceType()));
            String databaseName = resolveDatabaseName(request, datasource);
            if (!hasCollectedTableMetadata(datasource, request, databaseName)) {
                // 上一次探查任务可能“执行成功但采集 0 张表”。此时不能从本体步骤继续，
                // 必须退回探查步骤重新采集。
                retryFromDiscovery = true;
                state.setProgress(12);
                state.setCurrentStep("探查元数据");
                state.setMessage("未检测到表元数据，正在从探查步骤重新执行");
                state.setOntologyId(null);
                state.setActionCount(null);
                state.setResult(null);
                state.setSteps(defaultSteps());
            }
        }
        state.setStatus("RUNNING");
        state.setMessage(rebuildDatasource ? "原数据源已不存在，正在重新创建连接" : "正在从失败步骤重试：" + state.getCurrentStep());
        persistState(state);
        final PlatformAgentOperationReqVO retryRequest = request;
        final DatasourceDO retryDatasource = datasource;
        final boolean executeDiscovery = rebuildDatasource || retryFromDiscovery || state.getProgress() < 52;
        CompletableFuture.runAsync(() -> {
            try {
                if (executeDiscovery) execute(state, retryRequest, retryDatasource, userId, userName);
                else executeAiStages(state, retryRequest, retryDatasource, userId, userName);
            } catch (Exception ex) {
                state.setStatus("FAILED");
                state.setMessage(ex.getMessage() == null ? "流程执行失败" : ex.getMessage());
                persistState(state);
                log.error("平台数据智能体重试失败，operationId={}", id, ex);
            }
        });
        return state;
    }

    /** 人工确认发布并执行探查任务，然后继续后续 AI 构建步骤。 */
    public OperationState confirmMetadataTask(String id, Long userId, String userName) {
        OperationState state = get(id);
        PlatformAgentOperationReqVO request = getRequest(id);
        if (state == null || request == null) throw new IllegalArgumentException("流程实例不存在");
        if (!"NEED_CONFIRMATION".equals(state.getStatus())
                || !"PUBLISH_METADATA_TASK".equals(state.getConfirmationType())) {
            throw new IllegalStateException("当前流程不需要确认发布探查任务");
        }
        DatasourceDO datasource = state.getDatasourceId() == null ? resolveDatasource(request)
                : datasourceService.getDatasourceDOById(state.getDatasourceId());
        CatalogTaskDO task = state.getCatalogTaskId() == null ? null
                : catalogTaskService.getCatalogTaskById(state.getCatalogTaskId());
        if (datasource == null || task == null) throw new IllegalStateException("数据源或探查任务不存在");
        state.setStatus("RUNNING");
        state.setConfirmationType(null);
        state.setConfirmationMessage(null);
        state.setMessage("已确认，正在发布并执行探查任务");
        persistState(state);
        CompletableFuture.runAsync(() -> {
            try {
                CatalogTaskSaveReqVO publishReq = new CatalogTaskSaveReqVO();
                publishReq.setId(task.getId());
                publishReq.setStatus("1");
                try {
                    catalogTaskService.updateReleaseJobTask(publishReq);
                } catch (Exception publishError) {
                    // 任务已经是发布状态时，调度器定义更新失败不应阻断本地一次性探查。
                    // 只有尚未发布的任务才需要人工确认。
                    if (!"1".equals(task.getStatus())) {
                        state.getOutput().add("发布调度任务失败，已按人工确认执行一次探查："
                                + first(publishError.getMessage(), "调度器不可用"));
                        persistState(state);
                    } else {
                        state.getOutput().add("探查任务已处于发布状态，跳过调度更新，直接执行一次探查");
                        persistState(state);
                    }
                }
                boolean discoverySucceeded = catalogTaskService.runDaDiscoveryTask(task.getId());
                if (!discoverySucceeded) {
                    failWithoutAiStages(state, "探查任务执行失败，请查看探查任务日志后重试");
                    return;
                }
                String targetDatabaseName = resolveDatabaseName(request, datasource);
                if (!hasCollectedTableMetadata(datasource, request, targetDatabaseName)) {
                    failWithoutAiStages(state, "探查任务已执行，但未生成表元数据，请检查探查范围、schema 和数据源连接");
                    return;
                }
                update(state, 35, "同步数据资产", "人工确认后探查任务执行完成，正在同步数据资产");
                AssetsAssetSyncReqVO syncReq = new AssetsAssetSyncReqVO();
                syncReq.setDatasourceId(datasource.getId());
                syncReq.setDatabaseName(targetDatabaseName);
                syncReq.setSchemaName(request.getSchemaName());
                syncReq.setCatCode(resolveAssetCategory(request));
                syncReq.setSpaceId(request.getSpaceId());
                syncReq.setSpaceCode(request.getSpaceCode());
                AjaxResult syncResult = assetSyncService.sync(syncReq);
                authorizeAssets(datasource.getId(), request);
                state.setDatasourceId(datasource.getId());
                state.setAssetSyncMessage(syncResult == null ? null : String.valueOf(syncResult.get("msg")));
                update(state, 52, "配置数据授权", "资产和字段已绑定到当前空间");
                executeAiStages(state, request, datasource, userId, userName);
            } catch (Exception ex) {
                state.setStatus("FAILED");
                state.setMessage(ex.getMessage() == null ? "确认后执行失败" : ex.getMessage());
                persistState(state);
                log.error("平台数据智能体确认发布执行失败，operationId={}", id, ex);
            }
        });
        return state;
    }

    private void execute(OperationState state, PlatformAgentOperationReqVO request, DatasourceDO initial,
                         Long userId, String userName) {
        try {
            DatasourceDO datasource = initial;
            if (datasource == null) {
                update(state, 0, "确认数据源", "正在创建受控数据源连接");
                AssetsDatasourceSaveReqVO save = new AssetsDatasourceSaveReqVO();
                save.setDatasourceName(first(request.getDatasourceName(), request.getDatabaseName()));
                save.setDatasourceType(normalizeDatasourceType(first(request.getDatasourceType(), "PostgreSQL")));
                save.setDatasourceConfig(request.getDatasourceConfig());
                save.setIp(request.getIp());
                save.setPort(request.getPort());
                save.setDescription("由数据智能体流程创建");
                if (request.getSpaceId() != null) {
                    DatasourceSpaceRelDO spaceRel = new DatasourceSpaceRelDO();
                    spaceRel.setSpaceId(request.getSpaceId());
                    spaceRel.setSpaceCode(request.getSpaceCode());
                    spaceRel.setValidFlag(Boolean.TRUE);
                    save.setSpaceList(Collections.singletonList(spaceRel));
                }
                Long id = datasourceService.createDatasource(save);
                datasource = datasourceService.getDatasourceDOById(id);
            } else {
                update(state, 0, "确认数据源", "已匹配数据源：" + datasource.getDatasourceName());
            }
            datasource.setDatasourceType(normalizeDatasourceType(datasource.getDatasourceType()));
            ensureDatasourceSpaceRelation(datasource, request, userId, userName);
            state.setDatasourceId(datasource.getId());
            state.setDatasourceName(datasource.getDatasourceName());
            persistState(state);

            update(state, 12, "探查元数据", "正在准备元数据采集任务");
            CatalogTaskDO task = findTask(datasource.getId(), request.getSpaceId(), request.getSpaceCode());
            String targetDatabaseName = resolveDatabaseName(request, datasource);
            if (task == null) {
                CatalogTaskSaveReqVO taskReq = new CatalogTaskSaveReqVO();
                taskReq.setDatasourceId(datasource.getId());
                taskReq.setSourceSystemName(datasource.getDatasourceName());
                taskReq.setName("数据智能体自动探查-" + datasource.getDatasourceName());
                taskReq.setDbType(normalizeDatasourceType(datasource.getDatasourceType()));
                taskReq.setCollectionMode("1");
                // 平台流程只探查用户指定的数据库，不能使用“全部库”模式。
                taskReq.setCollectionScope("1");
                taskReq.setCollectType("1");
                taskReq.setStatus("0");
                taskReq.setSpaceId(request.getSpaceId());
                taskReq.setSpaceCode(request.getSpaceCode());
                taskReq.setCreatorId(userId);
                taskReq.setCreateBy(userName);
                taskReq.setScopeSaveReqVOS(Collections.singletonList(buildTaskScope(targetDatabaseName, request, datasource)));
                Long taskId = catalogTaskService.createCatalogTask(taskReq);
                task = catalogTaskService.getCatalogTaskById(taskId);
            } else if (!"1".equals(task.getCollectionScope())
                    || !sameSpace(task.getSpaceId(), request.getSpaceId())
                    || !sameText(task.getSpaceCode(), request.getSpaceCode())) {
                // 兼容之前错误创建的“全部库/未绑定空间”平台任务，改为指定库和当前空间后再执行。
                CatalogTaskSaveReqVO taskReq = new CatalogTaskSaveReqVO();
                taskReq.setId(task.getId());
                taskReq.setName(task.getName());
                taskReq.setDatasourceId(datasource.getId());
                taskReq.setDbType(normalizeDatasourceType(datasource.getDatasourceType()));
                taskReq.setCollectionMode("1");
                taskReq.setCollectionScope("1");
                taskReq.setCollectType("1");
                taskReq.setStatus(task.getStatus());
                taskReq.setSpaceId(request.getSpaceId());
                taskReq.setSpaceCode(request.getSpaceCode());
                taskReq.setScopeSaveReqVOS(Collections.singletonList(buildTaskScope(targetDatabaseName, request, datasource)));
                catalogTaskService.updateCatalogTask(taskReq);
                task = catalogTaskService.getCatalogTaskById(task.getId());
            }
            state.setCatalogTaskId(task.getId());
            persistState(state);
            boolean discoverySucceeded;
            try {
                // 探查结果进入目录前先发布任务；无法自动发布时暂停，由人工确认后继续。
                CatalogTaskSaveReqVO publishReq = new CatalogTaskSaveReqVO();
                publishReq.setId(task.getId());
                publishReq.setStatus("1");
                catalogTaskService.updateReleaseJobTask(publishReq);
                update(state, 20, "探查元数据", "探查任务已发布，正在执行一次探查");
                discoverySucceeded = catalogTaskService.runDaDiscoveryTask(task.getId());
            } catch (Exception publishError) {
                if ("1".equals(task.getStatus())) {
                    state.getOutput().add("探查任务已处于发布状态，跳过调度更新，直接执行一次探查");
                    persistState(state);
                    discoverySucceeded = catalogTaskService.runDaDiscoveryTask(task.getId());
                } else {
                state.setStatus("NEED_CONFIRMATION");
                state.setConfirmationType("PUBLISH_METADATA_TASK");
                state.setConfirmationMessage("探查任务已生成，但自动发布或执行失败："
                        + first(publishError.getMessage(), "请确认后手动发布执行"));
                state.setCurrentStep("探查元数据");
                state.setMessage(state.getConfirmationMessage());
                state.getOutput().add(state.getConfirmationMessage());
                persistState(state);
                return;
                }
            }

            if (!discoverySucceeded) {
                failWithoutAiStages(state, "探查任务执行失败，请查看探查任务日志后重试");
                return;
            }

            if (!hasCollectedTableMetadata(datasource, request, targetDatabaseName)) {
                failWithoutAiStages(state, "探查任务已执行，但未生成表元数据，请检查探查范围、schema 和数据源连接");
                return;
            }

            update(state, 35, "同步数据资产", "正在把最新目录元数据同步为资产");
            String catCode = resolveAssetCategory(request);
            AssetsAssetSyncReqVO syncReq = new AssetsAssetSyncReqVO();
            syncReq.setDatasourceId(datasource.getId());
            syncReq.setDatabaseName(resolveDatabaseName(request, datasource));
            syncReq.setSchemaName(request.getSchemaName());
            syncReq.setCatCode(catCode);
            syncReq.setSpaceId(request.getSpaceId());
            syncReq.setSpaceCode(request.getSpaceCode());
            AjaxResult syncResult = assetSyncService.sync(syncReq);
            authorizeAssets(datasource.getId(), request);
            update(state, 52, "配置数据授权", "资产和字段已绑定到当前空间");

            state.setDatasourceId(datasource.getId());
            state.setAssetSyncMessage(syncResult == null ? null : String.valueOf(syncResult.get("msg")));
            persistState(state);
            executeAiStages(state, request, datasource, userId, userName);
        } catch (Exception ex) {
            log.error("平台数据智能体流程失败，operationId={}", state.getId(), ex);
            state.setStatus("FAILED");
            state.setMessage(ex.getMessage() == null ? "流程执行失败" : ex.getMessage());
            persistState(state);
        }
    }

    private void executeAiStages(OperationState state, PlatformAgentOperationReqVO request, DatasourceDO datasource,
                                 Long userId, String userName) {
        Long ontologyId = state.getOntologyId();
        if (ontologyId == null) {
            update(state, 62, "生成本体模型", "正在根据已采集资产生成本体");
            OntologyAiGenerateReqVO ontReq = new OntologyAiGenerateReqVO();
            ontReq.setDatasourceId(datasource.getId());
            ontReq.setSpaceId(request.getSpaceId());
            ontReq.setSpaceCode(request.getSpaceCode());
            OntologyAiGenerateRespVO ontologyResult = ontologyGenerateService.generate(ontReq);
            ontologyId = ontologyResult.getOntologyId();
            if (ontologyId == null) throw new IllegalStateException("本体生成未返回本体编号");
            state.setOntologyId(ontologyId);
            persistState(state);
        }

        int actionCount = state.getActionCount() == null ? 0 : state.getActionCount();
        if (state.getProgress() <= 74 || actionCount == 0) {
            update(state, 74, "生成本体动作", "正在生成可执行动作定义");
            AiActionPreviewReqVO actionReq = new AiActionPreviewReqVO();
            actionReq.setOntologyId(ontologyId);
            actionReq.setPrompt(first(request.getGoal(), "根据当前本体生成常用查询、分析和更新动作"));
            AiActionPreviewRespVO actionPreview = ontologyGenerateService.generateActionsPreview(actionReq);
            actionCount = persistActions(actionPreview, ontologyId);
            state.setActionCount(actionCount);
            persistState(state);
        }

        update(state, 86, "生成决策 Skill", "正在生成本体决策 Skill");
        AiOntologySkillGenerateReqVO skillReq = new AiOntologySkillGenerateReqVO();
        skillReq.setOntologyId(ontologyId);
        skillReq.setPublish(Boolean.TRUE);
        aiSkillService.generateOntologyDecisionSkill(skillReq);

        update(state, 94, "同步智能体能力", "正在同步 Skill 和数据源到决策智能体");
        skillSyncService.syncAllSkills();
        datasourceSyncService.syncById(datasource.getId());
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("datasourceId", datasource.getId());
        result.put("ontologyId", ontologyId);
        result.put("actionCount", actionCount);
        result.put("assetSync", state.getAssetSyncMessage());
        state.setResult(result);
        update(state, 100, "已完成", "数据源、元数据、资产、本体、动作和 Skill 已自动完成");
        state.setStatus("SUCCEEDED");
        persistState(state);
    }

    private int persistActions(AiActionPreviewRespVO preview, Long ontologyId) {
        if (preview == null || preview.getActions() == null) return 0;
        int count = 0;
        for (AiActionPreviewRespVO.GeneratedActionPreview generated : preview.getActions()) {
            if (generated == null || generated.getActionName() == null) continue;
            ActionSaveReqVO save = new ActionSaveReqVO();
            save.setOntologyId(ontologyId);
            save.setName(generated.getActionName());
            save.setDescription(generated.getActionDescription());
            save.setActionType(first(generated.getActionType(), "COMPOSITE"));
            save.setExecutionSteps(generated.getExecutionSteps());
            save.setNeedsApproval(Boolean.TRUE);
            save.setApprovalLevels(1);
            try {
                actionService.createAction(save);
                count++;
            } catch (Exception actionError) {
                // 单个 AI 动作不满足当前动作模型时不阻断整个数据接入流程，状态日志会保留原因。
                log.warn("数据智能体生成动作未落库，name={}", generated.getActionName(), actionError);
            }
        }
        return count;
    }

    private void authorizeAssets(Long datasourceId, PlatformAgentOperationReqVO request) {
        if (request == null || (request.getSpaceId() == null && request.getSpaceCode() == null)) return;
        DatasourceDO sourceDatasource = datasourceService.getDatasourceDOById(datasourceId);
        if (sourceDatasource == null) return;
        String databaseName = resolveDatabaseName(request, sourceDatasource);
        List<com.datamaster.metadata.api.table.dto.CatalogTableRespDTO> scopedTables =
                catalogTableApiService.listByDatasourceAndDatabase(datasourceId, databaseName, request.getSchemaName(),
                        request.getSpaceId(), request.getSpaceCode());
        java.util.Set<String> scopedTableNames = new java.util.HashSet<>();
        if (scopedTables != null) {
            for (com.datamaster.metadata.api.table.dto.CatalogTableRespDTO table : scopedTables) {
                if (table != null && table.getTableName() != null) {
                    scopedTableNames.add(table.getTableName().toLowerCase());
                }
            }
        }
        List<AssetsAssetDO> assets = assetService.list(com.baomidou.mybatisplus.core.toolkit.Wrappers.<AssetsAssetDO>lambdaQuery()
                .eq(AssetsAssetDO::getDatasourceId, datasourceId));
        for (AssetsAssetDO asset : assets) {
            if (scopedTables != null && (asset.getTableName() == null
                    || !scopedTableNames.contains(asset.getTableName().toLowerCase()))) {
                continue;
            }
            AssetsAssetSpaceRelSaveReqVO rel = new AssetsAssetSpaceRelSaveReqVO();
            rel.setAssetId(asset.getId());
            rel.setSpaceId(request.getSpaceId());
            rel.setSpaceCode(request.getSpaceCode());
            assetSpaceRelService.createAssetSpaceRel(rel);
            AssetsAssetColumnPageReqVO columnReq = new AssetsAssetColumnPageReqVO();
            columnReq.setAssetId(asset.getId());
            for (AssetsAssetColumnDO column : assetColumnService.getAssetColumnList(columnReq)) {
                AssetsAssetColumnSpaceRelSaveReqVO columnRel = new AssetsAssetColumnSpaceRelSaveReqVO();
                columnRel.setAssetId(asset.getId());
                columnRel.setColumnId(column.getId());
                columnRel.setSpaceId(request.getSpaceId());
                columnRel.setSpaceCode(request.getSpaceCode());
                assetColumnSpaceRelService.createAssetColumnSpaceRel(columnRel);
            }
        }
    }

    private CatalogTaskDO findTask(Long datasourceId, Long spaceId, String spaceCode) {
        List<CatalogTaskDO> tasks = catalogTaskService.getCatalogTaskList();
        CatalogTaskDO orphan = null;
        for (CatalogTaskDO task : tasks) {
            if (!datasourceId.equals(task.getDatasourceId())
                    || task.getName() == null || !task.getName().startsWith("数据智能体自动探查-")) {
                continue;
            }
            if ((spaceId == null || spaceId.equals(task.getSpaceId()))
                    && (spaceCode == null || spaceCode.equals(task.getSpaceCode()))) {
                return task;
            }
            // 兼容旧版本从流程首页启动时产生的“未绑定空间”任务；下次从空间进入时修复并复用它。
            if (orphan == null && task.getSpaceId() == null
                    && (task.getSpaceCode() == null || task.getSpaceCode().trim().isEmpty())) {
                orphan = task;
            }
        }
        return orphan;
    }

    private void ensureDatasourceSpaceRelation(DatasourceDO datasource, PlatformAgentOperationReqVO request,
                                               Long userId, String userName) {
        if (datasource == null || request == null
                || (request.getSpaceId() == null && (request.getSpaceCode() == null || request.getSpaceCode().trim().isEmpty()))) {
            return;
        }
        AssetsDatasourceSpaceRelDO condition = new AssetsDatasourceSpaceRelDO();
        condition.setDatasourceId(datasource.getId());
        List<AssetsDatasourceSpaceRelDO> relations = datasourceSpaceRelService.getDatasourceSpaceRelList(condition);
        for (AssetsDatasourceSpaceRelDO relation : relations) {
            if (sameSpace(relation.getSpaceId(), request.getSpaceId())
                    && sameText(relation.getSpaceCode(), request.getSpaceCode())) {
                return;
            }
        }
        AssetsDatasourceSpaceRelSaveReqVO save = new AssetsDatasourceSpaceRelSaveReqVO();
        save.setDatasourceId(datasource.getId());
        save.setSpaceId(request.getSpaceId());
        save.setSpaceCode(request.getSpaceCode());
        save.setDescription("由数据智能体流程自动绑定");
        save.setDppAssigned(Boolean.FALSE);
        datasourceSpaceRelService.createDatasourceSpaceRel(save);
    }

    /**
     * 探查任务返回成功并不代表真的落下了表元数据（例如 schema 配置错误时会返回 0 张表）。
     * 本体生成必须以实际目录表为前置条件，避免把后续错误误报成 AI 生成失败。
     */
    private boolean hasCollectedTableMetadata(DatasourceDO datasource, PlatformAgentOperationReqVO request,
                                               String databaseName) {
        if (datasource == null || request == null
                || databaseName == null || databaseName.trim().isEmpty()) {
            return false;
        }
        // 用户未指定 schema 时不增加目录查询条件；采集连接内部会使用数据库类型的默认 schema。
        String schemaName = request.getSchemaName() == null || request.getSchemaName().trim().isEmpty()
                ? null : request.getSchemaName().trim();
        List<com.datamaster.metadata.api.table.dto.CatalogTableRespDTO> tables =
                catalogTableApiService.listByDatasourceAndDatabase(datasource.getId(), databaseName, schemaName,
                        request.getSpaceId(), request.getSpaceCode());
        return tables != null && !tables.isEmpty();
    }

    private void failWithoutAiStages(OperationState state, String message) {
        state.setStatus("FAILED");
        state.setProgress(Math.max(state.getProgress(), 35));
        state.setCurrentStep("探查元数据");
        state.setMessage(message);
        state.getOutput().add(message);
        persistState(state);
    }

    private boolean sameSpace(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }

    private boolean sameText(String first, String second) {
        if (first == null || first.trim().isEmpty()) return second == null || second.trim().isEmpty();
        return second != null && first.trim().equals(second.trim());
    }

    private DatasourceDO resolveDatasource(PlatformAgentOperationReqVO request) {
        if (request == null) return null;
        if (request.getDatasourceId() != null) {
            DatasourceDO byId = datasourceService.getDatasourceDOById(request.getDatasourceId());
            if (byId != null) return byId;
            // 历史流程可能保存了已删除的数据源 ID，继续按名称和连接参数匹配，避免重复创建。
        }
        String name = first(request.getDatasourceName(), request.getDatabaseName());
        String databaseName = first(request.getDatabaseName(), null);
        for (DatasourceDO item : datasourceService.getDatasourceList()) {
            if (name != null && name.equalsIgnoreCase(item.getDatasourceName())) return item;
            if (request.getIp() != null && request.getIp().equalsIgnoreCase(item.getIp())
                    && request.getPort() != null && request.getPort().equals(item.getPort())
                    && databaseName != null && databaseName.equalsIgnoreCase(readDatabaseName(item.getDatasourceConfig()))) {
                return item;
            }
        }
        return null;
    }

    private String readDatabaseName(String configText) {
        if (configText == null || configText.trim().isEmpty()) return null;
        try {
            JSONObject config = JSON.parseObject(configText);
            return config == null ? null : config.getString("dbname");
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readSchemaName(String configText) {
        if (configText == null || configText.trim().isEmpty()) return null;
        try {
            JSONObject config = JSON.parseObject(configText);
            if (config == null) return null;
            return first(config.getString("schema"), config.getString("sid"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private CatalogTaskScopeSaveReqVO buildTaskScope(String databaseName, PlatformAgentOperationReqVO request,
                                                       DatasourceDO datasource) {
        CatalogTaskScopeSaveReqVO scope = new CatalogTaskScopeSaveReqVO();
        scope.setDbName(databaseName);
        scope.setSchemaName(resolveSchemaName(
                first(request.getSchemaName(), readSchemaName(datasource.getDatasourceConfig())),
                datasource.getDatasourceType()));
        scope.setSpaceId(request.getSpaceId());
        scope.setSpaceCode(request.getSpaceCode());
        scope.setDescription("由数据智能体流程指定");
        return scope;
    }

    private String resolveSchemaName(String schemaName, String datasourceType) {
        if (schemaName != null && !schemaName.trim().isEmpty()) {
            return schemaName.trim();
        }
        String normalized = normalizeDatasourceType(datasourceType);
        if ("PostgreSQL".equalsIgnoreCase(normalized)
                || "Kingbase8".equalsIgnoreCase(normalized)
                || "Kingbase".equalsIgnoreCase(normalized)) {
            return "public";
        }
        return null;
    }

    private String resolveAssetCategory(PlatformAgentOperationReqVO request) {
        CategoryReqDTO req = new CategoryReqDTO();
        req.setCatType("ASSET");
        req.setSpaceId(request.getSpaceId());
        req.setSpaceCode(request.getSpaceCode());
        List<CategoryRespDTO> categories = categoryApiService.getCategoryList(req);
        if ((categories == null || categories.isEmpty()) && (request.getSpaceId() != null || request.getSpaceCode() != null)) {
            req.setSpaceId(null);
            req.setSpaceCode(null);
            categories = categoryApiService.getCategoryList(req);
        }
        if (categories != null) {
            for (CategoryRespDTO category : categories) {
                if (category.getCode() != null) return category.getCode();
            }
        }
        throw new IllegalStateException("当前空间没有可用的数据资产目录，请先配置资产目录");
    }

    private String resolveDatabaseName(PlatformAgentOperationReqVO request, DatasourceDO datasource) {
        if (request.getDatabaseName() != null && !request.getDatabaseName().trim().isEmpty()) {
            return request.getDatabaseName().trim();
        }
        try {
            List<com.datamaster.common.database.core.DbName> databases = datasourceApiService.getDatabaseListByDatasourceId(datasource.getId());
            if (databases != null) {
                for (com.datamaster.common.database.core.DbName database : databases) {
                    if (database != null && database.getDbName() != null && !database.getDbName().trim().isEmpty()) {
                        return database.getDbName();
                    }
                }
            }
        } catch (Exception ignored) {
            log.debug("无法读取数据源数据库列表，使用数据源名称作为同步范围", ignored);
        }
        return first(datasource.getDatasourceName(), "");
    }

    private String resolveInitialDatabaseName(PlatformAgentOperationReqVO request, DatasourceDO datasource) {
        if (request != null && request.getDatabaseName() != null && !request.getDatabaseName().trim().isEmpty()) {
            return request.getDatabaseName().trim();
        }
        String configured = datasource == null ? null : readDatabaseName(datasource.getDatasourceConfig());
        return first(configured, datasource == null ? request == null ? null : request.getDatasourceName() : datasource.getDatasourceName());
    }

    /** 兜底解析自然语言中的 owner/password，修复旧流程只保存 dbname 的情况。 */
    private PlatformAgentOperationReqVO completeConnectionFromGoal(PlatformAgentOperationReqVO request) {
        if (request == null || request.getGoal() == null) return request;
        try {
            JSONObject config = request.getDatasourceConfig() == null || request.getDatasourceConfig().trim().isEmpty()
                    ? new JSONObject() : JSON.parseObject(request.getDatasourceConfig());
            if (config == null) config = new JSONObject();
            String username = config == null ? null : config.getString("username");
            String password = config == null ? null : config.getString("password");
            if (username == null || username.trim().isEmpty()) {
                username = findConnectionToken(request.getGoal(), "(?:连接账号|用户名|账号|owner|onwer|user|username)");
            }
            if (password == null || password.trim().isEmpty()) {
                password = findConnectionToken(request.getGoal(), "(?:密码|password|pwd)");
                if (password != null && !password.trim().isEmpty()) password = AesEncryptUtil.encrypt(password.trim());
            }
            if (username != null && !username.trim().isEmpty()) config.put("username", username.trim());
            if (password != null && !password.trim().isEmpty()) config.put("password", password.trim());
            if (request.getDatabaseName() != null && !request.getDatabaseName().trim().isEmpty()
                    && (config.getString("dbname") == null || config.getString("dbname").trim().isEmpty())) {
                config.put("dbname", request.getDatabaseName().trim());
            }
            request.setDatasourceConfig(config.toJSONString());
        } catch (Exception ignored) {
            log.debug("无法从流程描述补齐数据源账号信息");
        }
        return request;
    }

    private String findConnectionToken(String text, String keyPattern) {
        Matcher matcher = Pattern.compile(keyPattern + "\\s*[:：=]\\s*([A-Za-z0-9_.$@-]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean hasConnection(PlatformAgentOperationReqVO request) {
        return request != null && request.getIp() != null && !request.getIp().trim().isEmpty()
                && request.getPort() != null && request.getDatasourceType() != null
                && request.getDatasourceConfig() != null && !request.getDatasourceConfig().trim().isEmpty();
    }

    /**
     * 统一平台智能体输入、资产数据源和 DbType 的类型编码。
     * DbType 使用 PostgreSQL/MySql/Oracle11/SQL_Server 等展示编码，
     * 而部分前端或调度接口会传入 POSTGRESQL/MYSQL/SQLSERVER。
     */
    private String normalizeDatasourceType(String type) {
        if (type == null || type.trim().isEmpty()) return type;
        String value = type.trim();
        if ("POSTGRESQL".equalsIgnoreCase(value) || "POSTGRES".equalsIgnoreCase(value)
                || "POSTGRE_SQL".equalsIgnoreCase(value) || "POSTGRE-SQL".equalsIgnoreCase(value)) {
            return "PostgreSQL";
        }
        if ("MYSQL".equalsIgnoreCase(value)) return "MySql";
        if ("SQLSERVER".equalsIgnoreCase(value) || "SQL_SERVER".equalsIgnoreCase(value)) return "SQL_Server";
        if ("ORACLE".equalsIgnoreCase(value)) return "Oracle11";
        return value;
    }

    private List<String> requiredFields() {
        List<String> fields = new ArrayList<>();
        fields.add("datasourceType"); fields.add("ip"); fields.add("port"); fields.add("datasourceConfig");
        return fields;
    }

    private void update(OperationState state, int progress, String step, String output) {
        state.setProgress(progress);
        state.setCurrentStep(step);
        state.setMessage(output);
        if ("已完成".equals(step)) {
            for (StepState item : state.getSteps()) item.setStatus("DONE");
        }
        for (StepState item : state.getSteps()) {
            if ("已完成".equals(step)) break;
            if (item.getTitle().equals(step)) item.setStatus(progress >= 100 ? "DONE" : "RUNNING");
            else if (state.getSteps().indexOf(item) < state.getSteps().indexOf(findStep(state, step))) item.setStatus("DONE");
        }
        state.getOutput().add(output);
        persistState(state);
    }

    private void persistNew(OperationState state, PlatformAgentOperationReqVO request, Long userId, String userName) {
        AiAgentOperationDO item = new AiAgentOperationDO();
        item.setId(state.getId());
        item.setGoal(state.getGoal());
        item.setRequestJson(request == null ? null : JSON.toJSONString(request));
        item.setCreatorId(userId);
        item.setCreateBy(userName);
        item.setCreateTime(new Date());
        if (request != null) {
            item.setSpaceId(request.getSpaceId());
            item.setSpaceCode(request.getSpaceCode());
        }
        item.setDelFlag(Boolean.FALSE);
        copyState(item, state);
        operationPersistence.create(item);
    }

    private void persistState(OperationState state) {
        AiAgentOperationDO item = operationPersistence.get(state.getId());
        if (item == null) return;
        copyState(item, state);
        item.setUpdateTime(new Date());
        operationPersistence.update(item);
    }

    private void persistRequest(String id, PlatformAgentOperationReqVO request) {
        AiAgentOperationDO item = operationPersistence.get(id);
        if (item == null) return;
        item.setRequestJson(request == null ? item.getRequestJson() : JSON.toJSONString(request));
        if (request != null) {
            item.setSpaceId(request.getSpaceId());
            item.setSpaceCode(request.getSpaceCode());
        }
        item.setUpdateTime(new Date());
        operationPersistence.update(item);
    }

    private void copyState(AiAgentOperationDO item, OperationState state) {
        item.setGoal(state.getGoal());
        item.setDatasourceId(state.getDatasourceId());
        item.setDatasourceName(state.getDatasourceName());
        item.setDatabaseName(state.getDatabaseName());
        item.setStatus(state.getStatus());
        item.setProgress(state.getProgress());
        item.setCurrentStep(state.getCurrentStep());
        item.setMessage(state.getMessage());
        item.setOutputJson(JSON.toJSONString(state.getOutput()));
        item.setStepsJson(JSON.toJSONString(state.getSteps()));
        item.setResultJson(state.getResult() == null ? null : JSON.toJSONString(state.getResult()));
        item.setRequiredFieldsJson(JSON.toJSONString(state.getRequiredFields()));
        item.setAssetSyncMessage(state.getAssetSyncMessage());
        item.setCatalogTaskId(state.getCatalogTaskId());
        item.setConfirmationType(state.getConfirmationType());
        item.setConfirmationMessage(state.getConfirmationMessage());
        item.setOntologyId(state.getOntologyId());
        item.setActionCount(state.getActionCount());
    }

    private OperationState toState(AiAgentOperationDO item) {
        OperationState state = new OperationState();
        state.setId(item.getId());
        state.setGoal(item.getGoal());
        state.setDatasourceId(item.getDatasourceId());
        state.setDatasourceName(item.getDatasourceName());
        state.setDatabaseName(item.getDatabaseName());
        state.setStatus(item.getStatus());
        state.setProgress(item.getProgress() == null ? 0 : item.getProgress());
        state.setCurrentStep(item.getCurrentStep());
        state.setMessage(item.getMessage());
        state.setAssetSyncMessage(item.getAssetSyncMessage());
        state.setOntologyId(item.getOntologyId());
        state.setActionCount(item.getActionCount());
        PlatformAgentOperationReqVO request = item.getRequestJson() == null ? null
                : JSON.parseObject(item.getRequestJson(), PlatformAgentOperationReqVO.class);
        if (request != null) {
            if (state.getDatabaseName() == null || state.getDatabaseName().trim().isEmpty()) state.setDatabaseName(request.getDatabaseName());
            state.setDatasourceType(request.getDatasourceType());
            state.setIp(request.getIp());
            state.setPort(request.getPort());
            state.setSchemaName(request.getSchemaName());
            state.setDatasourceConfig(request.getDatasourceConfig());
        }
        if (item.getOutputJson() != null) state.setOutput(JSON.parseArray(item.getOutputJson(), String.class));
        if (item.getStepsJson() != null) state.setSteps(JSON.parseArray(item.getStepsJson(), StepState.class));
        if (item.getRequiredFieldsJson() != null) state.setRequiredFields(JSON.parseArray(item.getRequiredFieldsJson(), String.class));
        if (item.getResultJson() != null) state.setResult(JSON.parseObject(item.getResultJson(), Map.class));
        if (state.getSteps() == null || state.getSteps().isEmpty()) state.setSteps(defaultSteps());
        if (state.getOutput() == null) state.setOutput(new CopyOnWriteArrayList<>());
        if (state.getRequiredFields() == null) state.setRequiredFields(new CopyOnWriteArrayList<>());
        return state;
    }

    private PlatformAgentOperationReqVO getRequest(String id) {
        AiAgentOperationDO item = operationPersistence.get(id);
        if (item == null || item.getRequestJson() == null) return null;
        return JSON.parseObject(item.getRequestJson(), PlatformAgentOperationReqVO.class);
    }

    private PlatformAgentOperationReqVO mergeRequest(PlatformAgentOperationReqVO base, PlatformAgentOperationReqVO patch) {
        if (base == null) return patch;
        if (patch == null) return base;
        if (patch.getGoal() != null) base.setGoal(patch.getGoal());
        if (patch.getDatasourceId() != null) base.setDatasourceId(patch.getDatasourceId());
        if (patch.getDatasourceName() != null) base.setDatasourceName(patch.getDatasourceName());
        if (patch.getDatabaseName() != null) base.setDatabaseName(patch.getDatabaseName());
        if (patch.getSchemaName() != null) base.setSchemaName(patch.getSchemaName());
        if (patch.getDatasourceType() != null) base.setDatasourceType(patch.getDatasourceType());
        if (patch.getDatasourceConfig() != null) base.setDatasourceConfig(patch.getDatasourceConfig());
        if (patch.getIp() != null) base.setIp(patch.getIp());
        if (patch.getPort() != null) base.setPort(patch.getPort());
        if (patch.getSpaceId() != null) base.setSpaceId(patch.getSpaceId());
        if (patch.getSpaceCode() != null) base.setSpaceCode(patch.getSpaceCode());
        return base;
    }

    private StepState findStep(OperationState state, String title) {
        for (StepState item : state.getSteps()) if (item.getTitle().equals(title)) return item;
        return state.getSteps().get(0);
    }

    private List<StepState> defaultSteps() {
        List<StepState> result = new CopyOnWriteArrayList<>();
        for (String title : new String[]{"确认数据源", "探查元数据", "同步数据资产", "配置数据授权", "生成本体模型", "生成本体动作", "生成决策 Skill", "同步智能体能力"}) {
            StepState step = new StepState(); step.setTitle(title); step.setStatus("PENDING"); result.add(step);
        }
        return result;
    }

    private String first(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OperationState {
        private String id;
        private String goal;
        private String datasourceName;
        private Long datasourceId;
        private String databaseName;
        private String datasourceType;
        private String ip;
        private Long port;
        private String schemaName;
        private String datasourceConfig;
        private String assetSyncMessage;
        private Long catalogTaskId;
        private String confirmationType;
        private String confirmationMessage;
        private Long ontologyId;
        private Integer actionCount;
        private String status;
        private int progress;
        private String currentStep;
        private String message;
        private List<String> requiredFields = new CopyOnWriteArrayList<>();
        private List<String> output = new CopyOnWriteArrayList<>();
        private List<StepState> steps = new CopyOnWriteArrayList<>();
        private Map<String, Object> result;
    }

    @Data
    public static class StepState {
        private String title;
        private String status;
    }
}
