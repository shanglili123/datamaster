package com.datamaster.module.ontology.service.impl;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectLineageRespVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectPermissionVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectVersionVO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.mapper.ActionExecutionMapper;
import com.datamaster.module.ontology.dal.mapper.ActionMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.service.IObjectLineageService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.neo4j.dto.ObjectLineageDTO;
import com.datamaster.neo4j.service.LineageDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对象血缘实现 — 对象血缘四维度聚合读取。
 *
 * <ul>
 *   <li>数据/决策维度：Neo4j（LINEAGE_ENABLED=true 时注入，否则跳过）</li>
 *   <li>版本维度：ONT_ACTION_EXECUTION 快照派生时间线（不依赖 Neo4j）</li>
 *   <li>权限维度：资产统一权限入口实时计算</li>
 * </ul>
 */
@Service
@Validated
public class ObjectLineageServiceImpl implements IObjectLineageService {

    private static final Logger log = LoggerFactory.getLogger(ObjectLineageServiceImpl.class);

    /** 治理 API 入口：对象血缘只读权限摘要（表级语义，不抛异常阻断，仅产出摘要） */
    private static final String ENTRANCE_ONTOLOGY_OBJECT_QUERY = "ONTOLOGY_OBJECT_QUERY";

    /**
     * 对象血缘读取（可插拔）：LINEAGE_ENABLED=true 时存在；未开启/未部署 Neo4j 时注入为 null。
     */
    @Autowired(required = false)
    private LineageDataService lineageDataService;

    @Resource
    private ConceptMapper conceptMapper;
    @Resource
    private ConceptTableMapper conceptTableMapper;
    @Resource
    private ActionMapper actionMapper;
    @Resource
    private ActionExecutionMapper actionExecutionMapper;
    @Resource
    private IAssetsTableGovernanceApiService tableGovernanceApiService;
    @Resource private com.datamaster.module.ontology.service.analysis.OntologyConsumptionContext consumptionContext;
    @Resource private com.datamaster.common.datasource.mgmt.api.IDatasourceApiService datasources;

    @Override
    public ObjectLineageRespVO objectLineage(Long conceptId, Long spaceId, String spaceCode) {
        ObjectLineageRespVO respVO = new ObjectLineageRespVO();
        ConceptDO concept = conceptMapper.selectById(conceptId);
        if (concept == null) {
            respVO.setConceptId(conceptId);
        } else {
            respVO.setConceptId(concept.getId());
        }

        com.datamaster.module.ontology.dal.dataobject.OntologyDO ontology = consumptionContext.resolve(concept, null, spaceId, spaceCode);
        spaceId = ontology.getSpaceId(); spaceCode = ontology.getSpaceCode();
        // 支撑表绑定：用于数据维度 / 版本维度 / 权限维度上下文
        List<ConceptTableDO> bindings = conceptTableMapper.selectByConceptId(conceptId);
        if (bindings == null || bindings.size() != 1) {
            ObjectPermissionVO unknown = new ObjectPermissionVO(); unknown.setAccessible(Boolean.FALSE);
            unknown.setMessage("概念级血缘快照要求唯一来源绑定；当前无绑定或存在多张来源，不能只核验第一张表后返回完整快照");
            respVO.setPermission(unknown); return respVO;
        }
        ConceptTableDO binding = bindings.get(0);
        if (binding == null || !java.util.Objects.equals(conceptId, binding.getConceptId())) {
            ObjectPermissionVO invalid = new ObjectPermissionVO(); invalid.setAccessible(Boolean.FALSE); invalid.setMessage("血缘来源绑定归属无效");
            respVO.setPermission(invalid); return respVO;
        }
        respVO.setTableBindingId(binding.getId());
        respVO.setTableName(binding.getTableName());

        // Check before returning snapshots/lineage data; a denied permission summary is not a data permit.
        ObjectPermissionVO permission = buildPermission(binding, spaceId, spaceCode);
        respVO.setPermission(permission);
        if (!Boolean.TRUE.equals(permission.getAccessible())) return respVO;
        fillDataAndDecision(respVO, conceptId);
        respVO.setVersions(buildVersions(conceptId));
        return respVO;
    }

    // ==================== 1. 数据 + 决策维度 ====================

    private void fillDataAndDecision(ObjectLineageRespVO respVO, Long conceptId) {
        if (lineageDataService == null) {
            return;
        }
        try {
            ObjectLineageDTO dto = lineageDataService.objectLineage(conceptId);
            if (dto != null) {
                respVO.setCurrentObject(dto.getCurrentObject());
                respVO.setTables(dto.getTables());
                respVO.setDecisions(dto.getDecisions());
            }
        } catch (Exception e) {
            log.warn("对象血缘(数据/决策维度)读取失败: {}", e.getMessage());
        }
    }

    // ==================== 2. 版本维度 ====================

    private List<ObjectVersionVO> buildVersions(Long conceptId) {
        // 概念 -> 动作 -> 执行记录，快照派生版本时间线（两级 Lambda 查询，符合 MyBatis-Plus 风格）
        List<ActionDO> actions = actionMapper.selectList(new LambdaQueryWrapperX<ActionDO>()
                .eq(ActionDO::getConceptId, conceptId)
                .orderByAsc(ActionDO::getId));
        if (actions == null || actions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> actionIds = actions.stream().map(ActionDO::getId).collect(Collectors.toList());
        Map<Long, ActionDO> actionMap = actions.stream()
                .collect(Collectors.toMap(ActionDO::getId, a -> a, (a, b) -> a));
        List<ActionExecutionDO> executions = actionExecutionMapper.selectList(
                new LambdaQueryWrapperX<ActionExecutionDO>()
                        .in(ActionExecutionDO::getActionId, actionIds)
                        .orderByDesc(ActionExecutionDO::getExecuteTime));
        if (executions == null || executions.isEmpty()) {
            return Collections.emptyList();
        }

        List<ObjectVersionVO> versions = new java.util.ArrayList<>();
        for (ActionExecutionDO exec : executions) {
            ActionDO action = actionMap.get(exec.getActionId());
            ObjectVersionVO vo = new ObjectVersionVO();
            vo.setExecutionId(exec.getId());
            vo.setActionId(exec.getActionId());
            if (action != null) {
                vo.setActionName(action.getName());
                vo.setActionType(action.getActionType());
            }
            vo.setStatus(exec.getStatus());
            vo.setExecuteTime(exec.getExecuteTime());
            vo.setBeforeData(parseSnapshot(exec.getBeforeData()));
            vo.setAfterData(parseSnapshot(exec.getAfterData()));
            versions.add(vo);
        }
        return versions;
    }

    private Map<String, Object> parseSnapshot(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            Object parsed = JSON.parse(json);
            if (parsed instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) parsed);
            }
            Map<String, Object> wrap = new LinkedHashMap<>();
            wrap.put("value", parsed);
            return wrap;
        } catch (Exception e) {
            log.debug("对象版本快照解析失败，原样包装: {}", e.getMessage());
            Map<String, Object> wrap = new LinkedHashMap<>();
            wrap.put("raw", json);
            return wrap;
        }
    }

    // ==================== 3. 权限维度 ====================

    private ObjectPermissionVO buildPermission(ConceptTableDO binding, Long spaceId, String spaceCode) {
        ObjectPermissionVO vo = new ObjectPermissionVO();
        if (binding == null || binding.getDatasourceId() == null || binding.getDatasourceId() <= 0
                || binding.getTableName() == null || binding.getTableName().isBlank()) {
            vo.setAccessible(Boolean.FALSE);
            vo.setMessage("对象未绑定可解析来源，不能据此确认准入");
            return vo;
        }
        AssetsTableGovernanceReqDTO req = new AssetsTableGovernanceReqDTO();
        req.setDatasourceId(binding.getDatasourceId());
        req.setTableName(binding.getTableName());
        req.setSpaceId(spaceId);
        req.setSpaceCode(spaceCode);
        req.setEntrance(ENTRANCE_ONTOLOGY_OBJECT_QUERY);
        try {
            if (!datasources.hasDatasourceSpace(binding.getDatasourceId(), spaceId, spaceCode)) {
                vo.setAccessible(Boolean.FALSE);
                vo.setMessage("血缘来源数据源未有效关联当前本体空间，不能读取历史快照");
                return vo;
            }
            AssetsTableGovernanceRespDTO resp = tableGovernanceApiService.resolveTable(req);
            if (resp == null) {
                vo.setAccessible(Boolean.FALSE);
                vo.setMessage("权限/流程核验未返回结论，不能视为放行");
                return vo;
            }
            vo.setAccessible(Boolean.TRUE.equals(resp.getAccessAllowed())
                    && (resp.getDeniedColumns() == null || resp.getDeniedColumns().isEmpty())
                    && resp.getAllowedColumns() == null);
            if (!Boolean.TRUE.equals(resp.getAccessAllowed())) {
                vo.setMessage(resp.getMessage() == null || resp.getMessage().isBlank()
                        ? "当前来源权限或空间流程未通过，不能读取完整血缘快照" : resp.getMessage());
            } else {
                vo.setMessage(Boolean.TRUE.equals(vo.getAccessible()) ? resp.getMessage()
                        : "血缘包含完整对象快照，部分字段授权不能用于返回全量快照；请使用受控对象查询");
            }
            vo.setDeniedColumns(resp.getDeniedColumns());
            vo.setAllowedColumns(resp.getAllowedColumns());
        } catch (Exception e) {
            log.warn("对象血缘权限摘要不可用，未取得准入结论");
            vo.setAccessible(Boolean.FALSE);
            vo.setMessage("权限/流程核验不可用或未通过，不能视为放行");
        }
        return vo;
    }

}
