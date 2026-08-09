package com.datamaster.module.assets.service.governance.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.assets.config.TableGovernanceProperties;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.dataobject.assetColumnSpaceRel.AssetsAssetColumnSpaceRelDO;
import com.datamaster.module.assets.dal.dataobject.assetApply.AssetsAssetApplyDO;
import com.datamaster.module.assets.dal.dataobject.assetchild.spaceRel.AssetsAssetSpaceRelDO;
import com.datamaster.module.assets.dal.mapper.assetColumn.AssetsAssetColumnMapper;
import com.datamaster.module.assets.dal.mapper.assetColumnSpaceRel.AssetsAssetColumnSpaceRelMapper;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.assetApply.AssetsAssetApplyMapper;
import com.datamaster.module.assets.dal.mapper.assetchild.spaceRel.AssetsAssetSpaceRelMapper;
import com.datamaster.metadata.api.service.table.CatalogTableApiService;
import com.datamaster.metadata.api.table.dto.CatalogTableRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssetsTableGovernanceApiServiceImpl implements IAssetsTableGovernanceApiService {

    private static final String MODE_OFF = "off";
    private static final String MODE_WARN = "warn";
    private static final String MODE_STRICT = "strict";
    private static final String APPLY_APPROVED = "2";

    @Resource
    private TableGovernanceProperties properties;
    @Resource
    private AssetsAssetMapper assetsAssetMapper;
    @Resource
    private AssetsAssetColumnMapper assetsAssetColumnMapper;
    @Resource
    private AssetsAssetColumnSpaceRelMapper assetsAssetColumnSpaceRelMapper;
    @Resource
    private AssetsAssetSpaceRelMapper assetsAssetSpaceRelMapper;
    @Resource
    private AssetsAssetApplyMapper assetsAssetApplyMapper;
    @Resource
    private CatalogTableApiService catalogTableApiService;

    @Override
    public AssetsTableGovernanceRespDTO resolveTable(AssetsTableGovernanceReqDTO reqDTO) {
        AssetsTableGovernanceRespDTO respDTO = baseResp(reqDTO);
        if (!Boolean.TRUE.equals(properties.getEnabled()) || MODE_OFF.equals(respDTO.getMode())) {
            respDTO.setSource(AssetsTableGovernanceRespDTO.SOURCE_NONE);
            respDTO.setAccessAllowed(true);
            respDTO.setMessage("表治理未开启");
            return respDTO;
        }
        if (reqDTO == null || reqDTO.getDatasourceId() == null || StringUtils.isBlank(reqDTO.getTableName())) {
            respDTO.setSource(AssetsTableGovernanceRespDTO.SOURCE_NONE);
            respDTO.setAccessAllowed(true);
            respDTO.setMessage("缺少数据源或表名，跳过表治理解析");
            return respDTO;
        }

        AssetsAssetDO asset = firstAsset(reqDTO.getDatasourceId(), reqDTO.getTableName());
        if (asset != null) {
            respDTO.setSource(AssetsTableGovernanceRespDTO.SOURCE_ASSET);
            respDTO.setAssetId(asset.getId());
            respDTO.setDatasourceId(asset.getDatasourceId());
            respDTO.setTableName(asset.getTableName());
            fillAssetAccess(respDTO, reqDTO, asset);
            return respDTO;
        }

        if (Boolean.TRUE.equals(properties.getFallbackToCatalog())) {
            CatalogTableRespDTO table = catalogTableApiService.getByDatasourceIdAndTableName(reqDTO.getDatasourceId(), reqDTO.getTableName());
            if (table != null) {
                respDTO.setSource(AssetsTableGovernanceRespDTO.SOURCE_CATALOG);
                respDTO.setCatalogTableId(table.getId());
                respDTO.setDatasourceId(table.getDatasourceId());
                respDTO.setTableName(table.getTableName());
                respDTO.setAccessAllowed(true);
                respDTO.setMessage("未找到数据资产，已回退到元数据采集");
                return respDTO;
            }
        }

        respDTO.setSource(AssetsTableGovernanceRespDTO.SOURCE_NONE);
        respDTO.setAccessAllowed(true);
        respDTO.setMessage("未找到数据资产或元数据采集记录");
        return respDTO;
    }

    @Override
    public void checkTableAccess(AssetsTableGovernanceReqDTO reqDTO) {
        AssetsTableGovernanceRespDTO respDTO = resolveTable(reqDTO);
        if (Boolean.FALSE.equals(respDTO.getAccessAllowed())) {
            throw new ServiceException(respDTO.getMessage());
        }
    }

    private AssetsTableGovernanceRespDTO baseResp(AssetsTableGovernanceReqDTO reqDTO) {
        AssetsTableGovernanceRespDTO respDTO = new AssetsTableGovernanceRespDTO();
        respDTO.setEnabled(Boolean.TRUE.equals(properties.getEnabled()));
        respDTO.setMode(normalizeMode(properties.getMode()));
        if (reqDTO != null) {
            respDTO.setDatasourceId(reqDTO.getDatasourceId());
            respDTO.setTableName(reqDTO.getTableName());
        }
        respDTO.setAccessAllowed(true);
        return respDTO;
    }

    private String normalizeMode(String mode) {
        if (StringUtils.isBlank(mode)) {
            return MODE_OFF;
        }
        String normalized = mode.toLowerCase(Locale.ROOT);
        if (MODE_WARN.equals(normalized) || MODE_STRICT.equals(normalized)) {
            return normalized;
        }
        return MODE_OFF;
    }

    private AssetsAssetDO firstAsset(Long datasourceId, String tableName) {
        List<AssetsAssetDO> assets = assetsAssetMapper.findByDatasourceIdAndTableName(datasourceId, tableName);
        if (assets == null || assets.isEmpty()) {
            return null;
        }
        return assets.get(0);
    }

    private void fillAssetAccess(AssetsTableGovernanceRespDTO respDTO, AssetsTableGovernanceReqDTO reqDTO, AssetsAssetDO asset) {
        if (!MODE_STRICT.equals(respDTO.getMode())) {
            respDTO.setAccessAllowed(true);
            respDTO.setMessage("命中数据资产，当前模式不强制拦截");
            return;
        }
        if (reqDTO.getSpaceId() == null && StringUtils.isBlank(reqDTO.getSpaceCode())) {
            respDTO.setAccessAllowed(true);
            respDTO.setMessage("命中数据资产，但未传入空间上下文，跳过空间权限校验");
            return;
        }
        if (hasSpaceRel(asset.getId(), reqDTO) || hasApprovedApply(asset.getId(), reqDTO)) {
            fillAllowedColumns(respDTO, reqDTO, asset);
            if (Boolean.FALSE.equals(respDTO.getAccessAllowed())) {
                return;
            }
            respDTO.setAccessAllowed(true);
            respDTO.setMessage("命中数据资产，空间已授权");
            return;
        }
        respDTO.setAccessAllowed(false);
        respDTO.setMessage("当前空间无权访问数据资产：" + asset.getTableName());
    }

    private boolean hasSpaceRel(Long assetId, AssetsTableGovernanceReqDTO reqDTO) {
        Long count = assetsAssetSpaceRelMapper.selectCount(Wrappers.<AssetsAssetSpaceRelDO>lambdaQuery()
                .eq(AssetsAssetSpaceRelDO::getAssetId, assetId)
                .eq(reqDTO.getSpaceId() != null, AssetsAssetSpaceRelDO::getSpaceId, reqDTO.getSpaceId())
                .eq(StringUtils.isNotBlank(reqDTO.getSpaceCode()), AssetsAssetSpaceRelDO::getSpaceCode, reqDTO.getSpaceCode()));
        return count != null && count > 0;
    }

    private boolean hasApprovedApply(Long assetId, AssetsTableGovernanceReqDTO reqDTO) {
        Long count = assetsAssetApplyMapper.selectCount(Wrappers.<AssetsAssetApplyDO>lambdaQuery()
                .eq(AssetsAssetApplyDO::getAssetId, assetId)
                .eq(reqDTO.getSpaceId() != null, AssetsAssetApplyDO::getSpaceId, reqDTO.getSpaceId())
                .eq(StringUtils.isNotBlank(reqDTO.getSpaceCode()), AssetsAssetApplyDO::getSpaceCode, reqDTO.getSpaceCode())
                .eq(AssetsAssetApplyDO::getStatus, APPLY_APPROVED));
        return count != null && count > 0;
    }

    /**
     * 计算正向可查字段清单：
     * 空间未配置字段级授权时返回 null（全部字段放行）；
     * 配置了字段级授权时返回空间已授权字段（请求列与授权列求交）。
     * 仅当请求列全部未授权时拒绝访问，部分未授权时过滤后放行。
     */
    private void fillAllowedColumns(AssetsTableGovernanceRespDTO respDTO, AssetsTableGovernanceReqDTO reqDTO, AssetsAssetDO asset) {
        List<AssetsAssetColumnDO> columns = assetsAssetColumnMapper.findByAssetId(asset.getId());
        if (columns == null || columns.isEmpty()) {
            return;
        }
        List<Long> authorizedColumnIds = assetsAssetColumnSpaceRelMapper.selectList(Wrappers.<AssetsAssetColumnSpaceRelDO>lambdaQuery()
                        .eq(AssetsAssetColumnSpaceRelDO::getAssetId, asset.getId())
                        .eq(reqDTO.getSpaceId() != null, AssetsAssetColumnSpaceRelDO::getSpaceId, reqDTO.getSpaceId())
                        .eq(StringUtils.isNotBlank(reqDTO.getSpaceCode()), AssetsAssetColumnSpaceRelDO::getSpaceCode, reqDTO.getSpaceCode()))
                .stream()
                .map(AssetsAssetColumnSpaceRelDO::getColumnId)
                .collect(Collectors.toList());
        if (authorizedColumnIds.isEmpty()) {
            return;
        }
        Set<String> authorized = columns.stream()
                .filter(column -> authorizedColumnIds.contains(column.getId()))
                .map(AssetsAssetColumnDO::getColumnName)
                .filter(StringUtils::isNotBlank)
                .map(this::normalizeColumn)
                .collect(Collectors.toSet());
        if (reqDTO.getColumnNames() == null || reqDTO.getColumnNames().isEmpty()) {
            respDTO.setAllowedColumns(new ArrayList<>(authorized));
            return;
        }
        Set<String> requested = normalizeColumns(reqDTO.getColumnNames());
        Set<String> allowed = new HashSet<>(requested);
        allowed.retainAll(authorized);
        if (allowed.isEmpty()) {
            respDTO.setAccessAllowed(false);
            respDTO.setDeniedColumns(new ArrayList<>(requested));
            respDTO.setMessage("当前空间无权访问字段：" + String.join(",", requested));
            return;
        }
        respDTO.setAllowedColumns(new ArrayList<>(allowed));
        if (allowed.size() < requested.size()) {
            List<String> denied = new ArrayList<>(requested);
            denied.removeAll(allowed);
            respDTO.setDeniedColumns(denied);
            respDTO.setMessage("部分字段无权访问，已过滤后放行：" + String.join(",", denied));
        }
    }

    private Set<String> normalizeColumns(List<String> columns) {
        Set<String> result = new HashSet<>();
        for (String column : columns) {
            String normalized = normalizeColumn(column);
            if (StringUtils.isNotBlank(normalized) && !normalized.contains("(") && !"*".equals(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalizeColumn(String column) {
        if (StringUtils.isBlank(column)) {
            return "";
        }
        String value = column.trim();
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < value.length() - 1) {
            value = value.substring(dotIndex + 1);
        }
        return value.replace("\"", "").replace("`", "").toLowerCase(Locale.ROOT);
    }
}
