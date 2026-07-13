package com.datamaster.module.assets.service.governance.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.assets.config.TableGovernanceProperties;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetApply.AssetsAssetApplyDO;
import com.datamaster.module.assets.dal.dataobject.assetchild.projectRel.AssetsAssetProjectRelDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.assetApply.AssetsAssetApplyMapper;
import com.datamaster.module.assets.dal.mapper.assetchild.projectRel.AssetsAssetProjectRelMapper;
import com.datamaster.module.catalog.api.service.table.CatalogTableApiService;
import com.datamaster.module.catalog.api.table.dto.CatalogTableRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;

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
    private AssetsAssetProjectRelMapper assetsAssetProjectRelMapper;
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
        if (reqDTO.getProjectId() == null && StringUtils.isBlank(reqDTO.getProjectCode())) {
            respDTO.setAccessAllowed(true);
            respDTO.setMessage("命中数据资产，但未传入项目上下文，跳过项目权限校验");
            return;
        }
        if (hasProjectRel(asset.getId(), reqDTO) || hasApprovedApply(asset.getId(), reqDTO)) {
            respDTO.setAccessAllowed(true);
            respDTO.setMessage("命中数据资产，项目已授权");
            return;
        }
        respDTO.setAccessAllowed(false);
        respDTO.setMessage("当前项目无权访问数据资产：" + asset.getTableName());
    }

    private boolean hasProjectRel(Long assetId, AssetsTableGovernanceReqDTO reqDTO) {
        Long count = assetsAssetProjectRelMapper.selectCount(Wrappers.<AssetsAssetProjectRelDO>lambdaQuery()
                .eq(AssetsAssetProjectRelDO::getAssetId, assetId)
                .eq(reqDTO.getProjectId() != null, AssetsAssetProjectRelDO::getProjectId, reqDTO.getProjectId())
                .eq(StringUtils.isNotBlank(reqDTO.getProjectCode()), AssetsAssetProjectRelDO::getProjectCode, reqDTO.getProjectCode()));
        return count != null && count > 0;
    }

    private boolean hasApprovedApply(Long assetId, AssetsTableGovernanceReqDTO reqDTO) {
        Long count = assetsAssetApplyMapper.selectCount(Wrappers.<AssetsAssetApplyDO>lambdaQuery()
                .eq(AssetsAssetApplyDO::getAssetId, assetId)
                .eq(reqDTO.getProjectId() != null, AssetsAssetApplyDO::getProjectId, reqDTO.getProjectId())
                .eq(StringUtils.isNotBlank(reqDTO.getProjectCode()), AssetsAssetApplyDO::getProjectCode, reqDTO.getProjectCode())
                .eq(AssetsAssetApplyDO::getStatus, APPLY_APPROVED));
        return count != null && count > 0;
    }
}
