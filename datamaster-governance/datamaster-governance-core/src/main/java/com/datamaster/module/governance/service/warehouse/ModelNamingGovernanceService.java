package com.datamaster.module.governance.service.warehouse;

import cn.hutool.core.bean.BeanUtil;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingReqDTO;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingRespDTO;
import com.datamaster.module.governance.dal.dataobject.dm.ModelingDataLayerDO;
import com.datamaster.module.governance.dal.dataobject.model.StandardsModelDO;
import com.datamaster.module.governance.dal.mapper.dm.ModelingDataLayerMapper;
import com.datamaster.module.governance.dal.mapper.model.StandardsModelMaterializedMapper;
import com.datamaster.module.governance.dal.dataobject.model.StandardsModelMaterializedDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/** Model naming lifecycle: explicit legacy upgrade and an immutable physical target. */
@Service
public class ModelNamingGovernanceService {
    public static final String LEGACY = "LEGACY";
    public static final String GOVERNED_V1 = "GOVERNED_V1";
    @Resource
    private IWarehouseTableNamingService namingService;
    @Resource
    private ModelingDataLayerMapper dataLayerMapper;
    @Resource
    private StandardsModelMaterializedMapper materializedMapper;

    public void prepare(StandardsModelDO model, StandardsModelDO existing, Boolean upgradeNaming) {
        prepare(model, existing, upgradeNaming, true);
    }

    public void prepare(StandardsModelDO model, StandardsModelDO existing, Boolean upgradeNaming,
                        boolean governanceProvided) {
        // Never accept a physical target or version supplied by clients/imported spreadsheets.
        model.setPhysicalTableName(existing == null ? null : existing.getPhysicalTableName());
        model.setNamingVersion(existing == null ? GOVERNED_V1 : version(existing));
        if (existing != null) {
            if (!governanceProvided) {
                mergeGovernance(model, existing);
            }
            if (model.getSpaceId() == null) {
                model.setSpaceId(existing.getSpaceId());
            }
            if (model.getSpaceCode() == null) {
                model.setSpaceCode(existing.getSpaceCode());
            }
            if (!Objects.equals(existing.getSpaceId(), model.getSpaceId())
                    || !Objects.equals(existing.getSpaceCode(), model.getSpaceCode())) {
                throw new ServiceException("模型不能更换所属空间，请新建模型");
            }
        }
        if (existing != null && LEGACY.equals(version(existing)) && !Boolean.TRUE.equals(upgradeNaming)) {
            // Old Excel/API clients may continue ordinary maintenance, but cannot silently opt into governance.
            if (!sameGovernance(model, existing)) {
                throw new ServiceException("历史LEGACY模型升级命名须显式传入upgradeNaming=true");
            }
            if (hasText(existing.getPhysicalTableName()) && !Objects.equals(model.getModelName(), existing.getModelName())) {
                throw new ServiceException("已确定物理目标的模型不能更换基础名，请新建模型");
            }
            return;
        }
        if (existing != null && LEGACY.equals(version(existing))
                && !hasText(existing.getPhysicalTableName())
                && materializedMapper.selectCount(Wrappers.lambdaQuery(StandardsModelMaterializedDO.class)
                .eq(StandardsModelMaterializedDO::getModelId, existing.getId())) > 0) {
            throw new ServiceException("历史模型已有物化记录但物理目标未确证，请先核对资产连接和表名快照，或新建模型");
        }
        clearExclusiveFields(model);
        if (model.getTableCase() == null || model.getTableCase().isEmpty()) {
            model.setTableCase("2");
        }
        boolean stableIdentity = existing != null && sameIdentity(model, existing);
        if (existing != null && GOVERNED_V1.equals(version(existing))
                && hasText(existing.getPhysicalTableName()) && !stableIdentity) {
            throw new ServiceException("已确定物理目标的模型不能更换基础名、分层、表类型、域或大小写，请新建模型");
        }
        if (existing != null && LEGACY.equals(version(existing)) && hasText(existing.getPhysicalTableName())
                && (!Objects.equals(model.getModelName(), existing.getModelName())
                || changedKnown(existing.getTableType(), model.getTableType())
                || changedKnown(existing.getDataLayerId(), model.getDataLayerId())
                || changedKnown(existing.getDataDomainId(), model.getDataDomainId())
                || changedKnown(existing.getThemeDomainId(), model.getThemeDomainId())
                || changedKnown(existing.getTableCase(), model.getTableCase()))) {
            throw new ServiceException("已确定物理目标的历史模型不能更换基础名、分层或域，请新建模型");
        }
        // Normal edits do not depend on mutable dictionary English names or valid flags.
        // Changing classification (metadata only) still verifies its active domain association.
        boolean ordinaryMaintenance = existing != null && GOVERNED_V1.equals(version(existing))
                && hasText(existing.getPhysicalTableName()) && stableIdentity
                && Objects.equals(model.getBusinessCategoryId(), existing.getBusinessCategoryId());
        if (ordinaryMaintenance) {
            describe(model);
            return;
        }
        WarehouseTableNamingRespDTO resolved = namingService.resolveAndValidate(request(model));
        namingService.validateModelSpace(model.getDataLayerId(), model.getSpaceId(), model.getSpaceCode());
        if (existing == null && model.getSpaceId() == null && model.getSpaceCode() == null) {
            ModelingDataLayerDO layer = dataLayerMapper.selectById(model.getDataLayerId());
            model.setSpaceId(layer.getSpaceId());
            model.setSpaceCode(layer.getSpaceCode());
        }
        applyMetadata(model, resolved);
        // Classification or dictionary changes never overwrite an already frozen physical name.
        if (!hasText(model.getPhysicalTableName())) {
            model.setPhysicalTableName(resolved.getTableName());
        }
        model.setNamingVersion(GOVERNED_V1);
    }

    public void describe(StandardsModelDO model) {
        model.setNamingVersion(version(model));
        applyMetadata(model, namingService.describe(request(model)));
    }

    private void applyMetadata(StandardsModelDO model, WarehouseTableNamingRespDTO metadata) {
        // Only resolved hierarchy labels/codes are copied, never the computed target tableName.
        model.setDataLayerCode(metadata.getDataLayerCode());
        model.setDataLayerName(metadata.getDataLayerName());
        model.setDataLayerEngName(metadata.getDataLayerEngName());
        model.setBusinessCategoryCode(metadata.getBusinessCategoryCode());
        model.setBusinessCategoryName(metadata.getBusinessCategoryName());
        model.setBusinessCategoryEngName(metadata.getBusinessCategoryEngName());
        model.setDataDomainCode(metadata.getDataDomainCode());
        model.setDataDomainName(metadata.getDataDomainName());
        model.setDataDomainEngName(metadata.getDataDomainEngName());
        model.setThemeDomainCode(metadata.getThemeDomainCode());
        model.setThemeDomainName(metadata.getThemeDomainName());
        model.setThemeDomainEngName(metadata.getThemeDomainEngName());
    }

    private WarehouseTableNamingReqDTO request(StandardsModelDO model) {
        WarehouseTableNamingReqDTO req = BeanUtil.toBean(model, WarehouseTableNamingReqDTO.class);
        req.setBaseTableName(model.getModelName());
        return req;
    }

    private static void mergeGovernance(StandardsModelDO model, StandardsModelDO existing) {
        if (model.getTableType() == null) model.setTableType(existing.getTableType());
        if (model.getDataLayerId() == null) model.setDataLayerId(existing.getDataLayerId());
        if (model.getBusinessCategoryId() == null) model.setBusinessCategoryId(existing.getBusinessCategoryId());
        if (model.getDataDomainId() == null) model.setDataDomainId(existing.getDataDomainId());
        if (model.getThemeDomainId() == null) model.setThemeDomainId(existing.getThemeDomainId());
        if (model.getTableCase() == null) model.setTableCase(existing.getTableCase());
        model.setBusinessCategoryCode(existing.getBusinessCategoryCode());
        model.setThemeDomainCode(existing.getThemeDomainCode());
    }

    private static void clearExclusiveFields(StandardsModelDO model) {
        if ("4".equals(model.getTableType())) {
            model.setDataDomainId(null);
        } else {
            model.setThemeDomainId(null);
            model.setThemeDomainCode(null);
        }
    }

    private static boolean sameIdentity(StandardsModelDO left, StandardsModelDO right) {
        return Objects.equals(left.getModelName(), right.getModelName())
                && Objects.equals(left.getTableType(), right.getTableType())
                && Objects.equals(left.getDataLayerId(), right.getDataLayerId())
                && Objects.equals(left.getDataDomainId(), right.getDataDomainId())
                && Objects.equals(left.getThemeDomainId(), right.getThemeDomainId())
                && Objects.equals(left.getTableCase(), right.getTableCase());
    }

    private static boolean sameGovernance(StandardsModelDO left, StandardsModelDO right) {
        return Objects.equals(left.getTableType(), right.getTableType())
                && Objects.equals(left.getDataLayerId(), right.getDataLayerId())
                && Objects.equals(left.getBusinessCategoryId(), right.getBusinessCategoryId())
                && Objects.equals(left.getDataDomainId(), right.getDataDomainId())
                && Objects.equals(left.getThemeDomainId(), right.getThemeDomainId())
                && Objects.equals(left.getTableCase(), right.getTableCase());
    }

    private static boolean changedKnown(Object oldValue, Object newValue) {
        return oldValue != null && !Objects.equals(oldValue, newValue);
    }

    private static String version(StandardsModelDO model) {
        return hasText(model.getNamingVersion()) ? model.getNamingVersion() : LEGACY;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
