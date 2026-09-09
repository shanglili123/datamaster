package com.datamaster.module.assets.service.asset.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.config.AniviaConfig;
import com.datamaster.common.constant.CacheConstants;
import com.datamaster.common.constant.Constants;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.TreeData;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.redis.RedisCache;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbDialect;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.DialectFactory;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.FileInfo;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.ExcelToCsvUtil;
import com.datamaster.common.utils.PageUtil;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.file.FileDataReaderUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.governance.api.service.cat.tagRel.ITaxonomyTagAssetRelApiService;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetReqDTO;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetRespDTO;
import com.datamaster.module.assets.api.assetColumn.dto.AssetsAssetColumnReqDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsAssetApiOutService;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetPageReqVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetRespVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnRelRuleVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.dataobject.assetchild.file.AssetsAssetFileDO;
import com.datamaster.module.assets.dal.dataobject.assetchild.files.AssetsAssetFilesDO;
import com.datamaster.module.assets.dal.dataobject.assetApply.AssetsAssetApplyDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.dataobject.sensitiveLevel.AssetsSensitiveLevelDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.assetColumn.AssetsAssetColumnMapper;
import com.datamaster.module.assets.dal.mapper.assetchild.file.AssetsAssetFileMapper;
import com.datamaster.module.assets.dal.mapper.assetApply.AssetsAssetApplyMapper;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceMapper;
import com.datamaster.module.assets.dal.mapper.sensitiveLevel.AssetsSensitiveLevelMapper;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.assetColumn.IAssetsAssetColumnService;
import com.datamaster.module.assets.service.assetColumnSpaceRel.IAssetsAssetColumnSpaceRelService;
import com.datamaster.module.assets.service.assetchild.files.IAssetsAssetFilesService;
import com.datamaster.module.assets.service.assetchild.geo.IAssetsAssetGeoService;
import com.datamaster.module.assets.service.assetchild.spaceRel.IAssetsAssetSpaceRelService;
import com.datamaster.module.assets.service.assetchild.theme.IAssetsAssetThemeRelService;
import com.datamaster.module.assets.service.assetchild.video.IAssetsAssetVideoService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.governance.dal.dataobject.desensitizeList.StandardsDesensitizeAssetcolumnDO;
import com.datamaster.module.governance.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;
import com.datamaster.module.governance.dal.dataobject.desensitizeRules.StandardsDesensitizeRuleDO;
import com.datamaster.module.governance.dal.dataobject.whitelist.StandardsDesensitizeWhitelistDO;
import com.datamaster.module.governance.service.desensitizeList.IStandardsDesensitizeAssetcolumnService;
import com.datamaster.module.governance.service.desensitizeRules.IStandardsDesensitizeRuleService;
import com.datamaster.module.governance.service.whitelist.IStandardsDesensitizeWhitelistService;
import com.datamaster.module.governance.api.service.businessCategory.IModelingBusinessCategoryApiService;
import com.datamaster.module.governance.api.service.dataLayer.IModelingDataLayerApiService;
import com.datamaster.module.governance.api.service.themeDomain.IModelingThemeDomainApiService;
import com.datamaster.module.governance.api.dataElem.dto.StandardsDataElemAssetRelReqDTO;
import com.datamaster.module.governance.api.dataElem.dto.StandardsDataElemAssetRelRespDTO;
import com.datamaster.module.governance.api.dataElem.dto.StandardsDataElemRespDTO;
import com.datamaster.module.governance.api.dataElem.dto.StandardsDataElemRuleRelRespDTO;
import com.datamaster.module.governance.api.model.dto.StandardsModelColumnRespDTO;
import com.datamaster.module.governance.api.model.dto.StandardsModelRespDTO;
import com.datamaster.module.governance.api.service.dataElem.IDataElemRuleRelService;
import com.datamaster.module.governance.api.service.model.IStandardsModelApiService;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskInstanceService;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskService;
import com.datamaster.metadata.api.column.dto.CatalogColumnRespDTO;
import com.datamaster.metadata.api.service.column.CatalogColumnApiService;
import com.datamaster.module.system.domain.vo.ColumnRespVO;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.neo4j.dto.LineageDTO;
import com.datamaster.neo4j.node.TaskNode;
import com.datamaster.neo4j.service.LineageDataService;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service * * @author lhs * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetServiceImpl extends ServiceImpl<AssetsAssetMapper, AssetsAssetDO> implements IAssetsAssetService, IAssetsAssetApiOutService {
    @Value("${ds.resource_url:}")
    private String resourceUrl;
    @Value("${datamaster.profile}")
    private String profile;
    @Resource
    private AssetsAssetMapper AssetsAssetMapper;
    @Resource
    private AssetsDatasourceMapper AssetsDatasourceMapper;
    @Resource
    private IStandardsModelApiService iStandardsModelApiService;
    @Resource
    private IAssetsAssetColumnService IAssetsAssetColumnService;
    @Resource
    private IDataElemRuleRelService elemRuleRelService;
    @Resource
    private IAssetsDatasourceService IAssetsDatasourceService;
    @Resource
    private AssetsAssetColumnMapper AssetsAssetColumnMapper;
    @Resource
    private AssetsSensitiveLevelMapper AssetsSensitiveLevelMapper;
    @Resource
    private AssetsAssetFileMapper assetFileMapper;
    @Autowired
    private DataSourceFactory DataSourceFactory;
    @Resource
    private RedisCache redisCache;
    @Resource
    private AssetsAssetApplyMapper AssetsAssetApplyMapper;
    @Resource
    private CollectorEtlTaskService collectorEtlTaskService;
    @Resource
    private IAssetsAssetThemeRelService AssetsAssetThemeRelService;
    @Resource
    private IAssetsAssetSpaceRelService IAssetsAssetSpaceRelService;
    @Resource
    private IAssetsAssetColumnSpaceRelService assetsAssetColumnSpaceRelService;
    @Resource
    private IAssetsAssetGeoService IAssetsAssetGeoService;
    @Resource
    private IAssetsAssetVideoService IAssetsAssetVideoService;
    @Resource
    private IAssetsAssetFilesService AssetsAssetFilesService;
    @Resource
    private ITaxonomyTagAssetRelApiService taxonomyTagAssetRelApiService;
    @Autowired(required = false)
    private LineageDataService lineageDataService;
    @Resource
    private CollectorEtlTaskInstanceService collectorEtlTaskInstanceService;
    @Resource
    private IModelingThemeDomainApiService modelingThemeDomainApiService;
    @Resource
    private IModelingBusinessCategoryApiService modelingBusinessCategoryApiService;
    @Resource
    private IModelingDataLayerApiService modelingDataLayerApiService;
    @Resource
    private CatalogColumnApiService catalogColumnApiService;

    @Resource
    private IStandardsDesensitizeAssetcolumnService standardsDesensitizeAssetcolumnService;
    @Resource
    private IStandardsDesensitizeRuleService standardsDesensitizeRuleService;
    @Resource
    private IStandardsDesensitizeWhitelistService whitelistService;
    @Resource
    private IAssetsTableGovernanceApiService assetsTableGovernanceApiService;
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(".xlsx", ".xls", ".csv");

    /**
     * @param AssetsAssetReqDTO * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetsAssetRespDTO insertAsset(AssetsAssetReqDTO AssetsAssetReqDTO) {
        StandardsModelRespDTO standardsModelByIdApi = iStandardsModelApiService.getDpModelByIdApi(AssetsAssetReqDTO.getModelId());
        if (standardsModelByIdApi == null) {
            throw new ServiceException("");
        }
        AssetsAssetDO AssetsAssetDO = new AssetsAssetDO();
        AssetsAssetDO.setName(standardsModelByIdApi.getModelComment());
        AssetsAssetDO.setCatCode(standardsModelByIdApi.getCatCode());
        AssetsAssetDO.setDatasourceId(AssetsAssetReqDTO.getDatasourceId());
        AssetsAssetDO.setSource(AssetsAssetReqDTO.getSource());
        AssetsAssetDO.setTableName(standardsModelByIdApi.getTableName());
        AssetsAssetDO.setTableComment(standardsModelByIdApi.getModelComment());
        AssetsAssetDO.setFieldCount(AssetsAssetReqDTO.getFieldCount());
        AssetsAssetDO.setTableType(standardsModelByIdApi.getTableType());
        AssetsAssetDO.setDataLayerId(standardsModelByIdApi.getDataLayerId());
        AssetsAssetDO.setBusinessCategoryId(standardsModelByIdApi.getBusinessCategoryId());
        AssetsAssetDO.setBusinessCategoryCode(standardsModelByIdApi.getBusinessCategoryCode());
        AssetsAssetDO.setDataDomainId(standardsModelByIdApi.getDataDomainId());
        AssetsAssetDO.setThemeDomainId(standardsModelByIdApi.getThemeDomainId());
        AssetsAssetDO.setThemeDomainCode(standardsModelByIdApi.getThemeDomainCode());
        AssetsAssetDO.setTableCase(standardsModelByIdApi.getTableCase());
        AssetsAssetPageReqVO AssetsAssetPageReqVO = new AssetsAssetPageReqVO();
        AssetsAssetPageReqVO.setTableName(standardsModelByIdApi.getTableName());
        AssetsAssetPageReqVO.setDatasourceId(AssetsAssetReqDTO.getDatasourceId());
        AssetsAssetDO assetDO = this.getAssetByAssetPageReqVO(AssetsAssetPageReqVO);
        if (assetDO != null) {
            AssetsAssetDO.setId(assetDO.getId());
            AssetsAssetMapper.updateById(AssetsAssetDO);
            redisCache.deleteObject(CacheConstants.ASSET_PREVIEW_KEY + AssetsAssetReqDTO.getId() + "_" + standardsModelByIdApi.getTableName());
        } else {
            AssetsAssetMapper.insert(AssetsAssetDO);
        }
        List<StandardsModelColumnRespDTO> standardsModelColumnListByModelIdApi = iStandardsModelApiService.getDpModelColumnListByModelIdApi(AssetsAssetReqDTO.getModelId());
        List<AssetsAssetColumnDO> AssetsAssetColumnDOList = new ArrayList<>();
        List<AssetsAssetColumnDO> AssetsAssetColumnList = new ArrayList<>();
        if (assetDO != null) {
            AssetsAssetColumnPageReqVO AssetsAssetColumnPageReqVO = new AssetsAssetColumnPageReqVO();
            AssetsAssetColumnPageReqVO.setAssetId(assetDO.getId());
            List<AssetsAssetColumnDO> AssetsAssetColumnList1 = IAssetsAssetColumnService.getAssetColumnList(AssetsAssetColumnPageReqVO);
            AssetsAssetColumnList = CollectionUtils.isEmpty(AssetsAssetColumnList1) ? AssetsAssetColumnList : AssetsAssetColumnList1;
        }
        if (StringUtils.isNotEmpty(standardsModelColumnListByModelIdApi)) {
            for (StandardsModelColumnRespDTO standardsModelColumnRespDTO : standardsModelColumnListByModelIdApi) {
                AssetsAssetColumnDO AssetsAssetColumnDO = new AssetsAssetColumnDO();
                AssetsAssetColumnDO columnDO = matchColumn(AssetsAssetColumnList, standardsModelColumnRespDTO);
                if (columnDO != null) {
                    AssetsAssetColumnDO.setId(columnDO.getId());
                }
                AssetsAssetColumnDO.setAssetId(AssetsAssetDO.getId());
                AssetsAssetColumnDO.setDataElemCodeId(standardsModelColumnRespDTO.getDataElemId());
                AssetsAssetColumnDO.setColumnName(standardsModelColumnRespDTO.getEngName());
                AssetsAssetColumnDO.setColumnLength(standardsModelColumnRespDTO.getColumnLength());
                AssetsAssetColumnDO.setColumnScale(standardsModelColumnRespDTO.getColumnScale());
                AssetsAssetColumnDO.setColumnType(standardsModelColumnRespDTO.getColumnType());
                AssetsAssetColumnDO.setColumnComment(standardsModelColumnRespDTO.getCnName());
                AssetsAssetColumnDO.setDefaultValue(standardsModelColumnRespDTO.getDefaultValue());
                AssetsAssetColumnDO.setNullableFlag(standardsModelColumnRespDTO.getNullableFlag());
                AssetsAssetColumnDO.setPkFlag(standardsModelColumnRespDTO.getPkFlag());
                AssetsAssetColumnDOList.add(AssetsAssetColumnDO);
            }
        }
//Ã¦ÂÂ¹Ã©ÂÂÃ¤Â¿ÂÃ¥Â­ÂÃ¦ÂÂ°Ã¦ÂÂ®Ã¨ÂµÂÃ¤ÂºÂ§Ã¥Â­ÂÃ¦Â®Âµ
//
        IAssetsAssetColumnService.saveBatch(AssetsAssetColumnDOList);
        for (AssetsAssetColumnDO AssetsAssetColumnDO : AssetsAssetColumnDOList) {
            if (AssetsAssetColumnDO.getId() == null) {
                IAssetsAssetColumnService.save(AssetsAssetColumnDO);
            } else {
                IAssetsAssetColumnService.updateById(AssetsAssetColumnDO);
            }
        }
        Collection<Long> nonExistingIdList = this.findNonExistingIdList(AssetsAssetColumnDOList, AssetsAssetColumnList);
        if (CollectionUtils.isNotEmpty(nonExistingIdList)) {
            IAssetsAssetColumnService.removeAssetColumn(nonExistingIdList);
        }
//Ã¨Â®Â¾Ã§Â½Â®Ã¦ÂÂ°Ã¦ÂÂ®Ã¥ÂÂÃ¦ÂÂ°Ã¦ÂÂ®Ã¨ÂµÂÃ¤ÂºÂ§Ã¥ÂÂ³Ã¨ÂÂÃ¤Â¿Â¡Ã¦ÂÂ¯
        Set<Long> ids = standardsModelColumnListByModelIdApi.stream().map(StandardsModelColumnRespDTO::getDataElemId).collect(Collectors.toSet());
//idÃ¦ÂÂ°Ã¦ÂÂ®Ã¤Â¸ÂÃ¤Â¸ÂºÃ§Â©Âº
        if (StringUtils.isNotEmpty(ids)) {
            List<StandardsDataElemRespDTO> standardsDataElemListByIdsApi = iStandardsModelApiService.getDpDataElemListByIdsApi(ids);
            List<StandardsDataElemAssetRelReqDTO> standardsDataElemAssetRel = new ArrayList<>();
            standardsDataElemListByIdsApi.forEach(standardsDataElemRespDTO -> {
                StandardsDataElemAssetRelReqDTO standardsDataElemAssetRelReqDTO = new StandardsDataElemAssetRelReqDTO();
//Ã¨Â®Â¾Ã§Â½Â®Ã¨ÂµÂÃ¤ÂºÂ§id
                standardsDataElemAssetRelReqDTO.setAssetId(AssetsAssetDO.getId());
                standardsDataElemAssetRelReqDTO.setDataElemType(standardsDataElemRespDTO.getType());
                standardsDataElemAssetRelReqDTO.setTableName(standardsModelByIdApi.getModelName());
                standardsDataElemAssetRelReqDTO.setColumnName(standardsDataElemRespDTO.getEngName());
                standardsDataElemAssetRelReqDTO.setDataElemId(standardsDataElemRespDTO.getId());
                Optional<AssetsAssetColumnDO> first = AssetsAssetColumnDOList.stream().filter(AssetsAssetColumnDO -> AssetsAssetColumnDO.getDataElemCodeId() != null && AssetsAssetColumnDO.getDataElemCodeId().equals(standardsDataElemRespDTO.getId())).findFirst();
                first.ifPresent(AssetsAssetColumnDO -> standardsDataElemAssetRelReqDTO.setColumnId(AssetsAssetColumnDO.getId()));
                standardsDataElemAssetRel.add(standardsDataElemAssetRelReqDTO);
            });
            if (StringUtils.isNotEmpty(standardsDataElemListByIdsApi)) {
                boolean b = iStandardsModelApiService.insertElementAssetRelation(standardsDataElemAssetRel);
                if (!b) {
                    throw new ServiceException("");
                }
            }
        }
        AssetsAssetRespDTO result = new AssetsAssetRespDTO();
        result.setId(AssetsAssetDO.getId());
//Ã¨ÂµÂÃ¤ÂºÂ§id
        return result;
    }

    /**
     * AssetsAssetColumnList  AssetsAssetColumnDOList      *  columnName StringUtils.equals  id      *     * @param AssetsAssetColumnDOList      * @param AssetsAssetColumnList        * @return  id
     */
    public static Collection<Long> findNonExistingIdList(List<AssetsAssetColumnDO> AssetsAssetColumnDOList, List<AssetsAssetColumnDO> AssetsAssetColumnList) {
// Ã¦ÂÂÃ¥ÂÂÃ¥Â·Â²Ã¥Â­ÂÃ¥ÂÂ¨Ã¥ÂÂÃ¨Â¡Â¨Ã¤Â¸Â­Ã¦ÂÂÃ¦ÂÂÃ©ÂÂÃ§Â©ÂºÃ§ÂÂ columnName Ã¥ÂÂ°Ã¤Â¸ÂÃ¤Â¸Âª Set Ã¤Â¸Â­
        Set<String> existingNames = AssetsAssetColumnDOList == null ? null : AssetsAssetColumnDOList.stream().filter(asset -> StringUtils.isNotBlank(asset.getColumnName())).map(AssetsAssetColumnDO::getColumnName).collect(Collectors.toSet());
// Ã¥Â¯Â¹Ã¥Â¾ÂÃ¥ÂÂ¹Ã©ÂÂÃ¥ÂÂÃ¨Â¡Â¨Ã¨Â¿ÂÃ¨Â¡ÂÃ¨Â¿ÂÃ¦Â»Â¤Ã¯Â¼ÂÃ¤Â¿ÂÃ§ÂÂ columnName Ã¤Â¸ÂÃ¥ÂÂ¨ existingNames Ã¤Â¸Â­Ã§ÂÂÃ¨Â®Â°Ã¥Â½ÂÃ¯Â¼ÂÃ¥Â¹Â¶Ã¦ÂÂ¶Ã©ÂÂÃ¥ÂÂ¶ id
        return AssetsAssetColumnList == null ? null : AssetsAssetColumnList.stream().filter(asset -> StringUtils.isNotBlank(asset.getColumnName())).filter(asset -> existingNames == null || existingNames.stream().noneMatch(name -> StringUtils.equals(name, asset.getColumnName()))).map(AssetsAssetColumnDO::getId).collect(Collectors.toList());
    }

    /**
     * standardsModelColumnRespDTO  engName  AssetsAssetColumnList  AssetsAssetColumnDO      *     * @param AssetsAssetColumnList         * @param standardsModelColumnRespDTO  DTO engName      * @return  AssetsAssetColumnDO  null
     */
    public static AssetsAssetColumnDO matchColumn(List<AssetsAssetColumnDO> AssetsAssetColumnList, StandardsModelColumnRespDTO standardsModelColumnRespDTO) {
        if (AssetsAssetColumnList == null || standardsModelColumnRespDTO == null || standardsModelColumnRespDTO.getEngName() == null) {
            return null;
        }
        for (AssetsAssetColumnDO AssetsAssetColumnDO : AssetsAssetColumnList) {
// Ã¥Â½ÂÃ¥Â­ÂÃ¦Â®ÂµÃ¥ÂÂÃ§Â§Â°Ã¥ÂÂ¹Ã©ÂÂÃ¦ÂÂ¶Ã¯Â¼ÂÃ¨Â¿ÂÃ¥ÂÂÃ¨Â¯Â¥Ã¥Â¯Â¹Ã¨Â±Â¡
            if (standardsModelColumnRespDTO.getEngName().equals(AssetsAssetColumnDO.getColumnName())) {
                return AssetsAssetColumnDO;
            }
        }
        return null;
    }

    @Override
    public Long getCountByCatCode(String catCode) {
        return baseMapper.selectCount(Wrappers.lambdaQuery(AssetsAssetDO.class).likeRight(AssetsAssetDO::getCatCode, catCode));
    }

    @Override
    public PageResult<AssetsAssetRespDTO> AssetsAssetListPage(AssetsAssetReqDTO AssetsAssetReqDTO) {
        AssetsAssetPageReqVO AssetsAssetPageReqVO = BeanUtils.toBean(AssetsAssetReqDTO, AssetsAssetPageReqVO.class);
        return BeanUtils.toBean(this.getAssetPage(AssetsAssetPageReqVO, "1"), AssetsAssetRespDTO.class);
    }

    @Override
    public List<AssetsAssetRespDTO> getAssetRespByDataSourceId(Long datasourceId, String tableName) {
        return BeanUtils.toBean(this.getAssetByDataSourceId(datasourceId, tableName), AssetsAssetRespDTO.class);
    }

    @Override
    public void insertAssetByDiscoveryInfo(AssetsAssetReqDTO assetsAssetReqDTO, List<AssetsAssetColumnReqDTO> columnReqDTOList, List<String> themeIdList) {
        AssetsAssetPageReqVO assetsAssetPageReqVO = BeanUtils.toBean(assetsAssetReqDTO, AssetsAssetPageReqVO.class);
        assetsAssetPageReqVO.setThemeIdList(themeIdList);
        List<AssetsAssetColumnSaveReqVO> columnSaveReqVOList = BeanUtils.toBean(columnReqDTOList, AssetsAssetColumnSaveReqVO.class);
        this.insertAssetByDiscoveryInfo(assetsAssetPageReqVO, columnSaveReqVOList);
    }

    @Override
    public void updateAssetByDiscoveryInfo(AssetsAssetReqDTO assetsAssetReqDTO) {
        this.updateAssetByDiscoveryInfo(BeanUtils.toBean(assetsAssetReqDTO, AssetsAssetPageReqVO.class));
    }

    @Override
    public List<AssetsAssetDO> getTablesByDataSourceId(AssetsAssetPageReqVO pageReqVO) {
        if (pageReqVO.getDatasourceId() == null) {
            throw new ServiceException("数据源 ID 不能为空");
        }
        return this.getAssetList(pageReqVO);
    }

    @Override
    public AssetsAssetDO getAssetByAssetPageReqVO(AssetsAssetPageReqVO pageReqVO) {
        MPJLambdaWrapper<AssetsAssetDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.eq(StringUtils.isNotEmpty(pageReqVO.getName()), AssetsAssetDO::getName, pageReqVO.getName())
                .eq(pageReqVO.getId() != null, AssetsAssetDO::getId, pageReqVO.getId())
                .eq(StringUtils.isNotEmpty(pageReqVO.getTableName()), AssetsAssetDO::getTableName, pageReqVO.getTableName())
                .eq(pageReqVO.getDatasourceId() != null, AssetsAssetDO::getDatasourceId, pageReqVO.getDatasourceId())
                .eq(StringUtils.isNotEmpty(pageReqVO.getTableComment()), AssetsAssetDO::getTableComment, pageReqVO.getTableComment());
        return baseMapper.selectOne(lambdaWrapper);
    }

    public List<Long> extractDistinctAssetIds(List<AssetsAssetThemeRelRespVO> vos) {
        if (CollectionUtils.isEmpty(vos)) {
            return new ArrayList<>();
        }
        return vos.stream().map(AssetsAssetThemeRelRespVO::getAssetId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    /**
     * 1-     * 2-
     */
    @Override
    public PageResult<AssetsAssetDO> getAssetPage(AssetsAssetPageReqVO pageReqVO, String AssetsAssetQueryType) {
        PageResult<AssetsAssetDO> AssetsAssetDOPageResult = AssetsAssetMapper.selectPage(pageReqVO);
        List<AssetsAssetDO> AssetsAssetDOList = (List<AssetsAssetDO>) AssetsAssetDOPageResult.getRows();
        for (AssetsAssetDO AssetsAssetDO : AssetsAssetDOList) {
            if (StringUtils.equals("1", AssetsAssetDO.getType())) {
                AssetsDatasourceDO AssetsDatasourceById = IAssetsDatasourceService.getDatasourceDOById(AssetsAssetDO.getDatasourceId());
                AssetsDatasourceById = AssetsDatasourceById == null ? new AssetsDatasourceDO() : AssetsDatasourceById;
                AssetsAssetDO.setDatasourceName(AssetsDatasourceById.getDatasourceName());
                AssetsAssetDO.setDatasourceType(AssetsDatasourceById.getDatasourceType());
            }
        }
        AssetsAssetDOPageResult.setRows(AssetsAssetDOList);
        return AssetsAssetDOPageResult;
    }

    @Override
    public List<AssetsAssetDO> getAssetList(AssetsAssetPageReqVO reqVO) {
        MPJLambdaWrapper<AssetsAssetDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsAssetDO.class)
                .select("t2.NAME AS catName")
                .select("COALESCE(t3.SPACE_ID, t4.SPACE_ID) AS spaceId,COALESCE(t3.SPACE_CODE, t4.SPACE_CODE) AS spaceCode")
                .leftJoin("TAX_CATEGORY t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0' AND t2.CAT_TYPE = 'ASSET'")
                .leftJoin("AST_ASSET_SPACE_REL t3 on t.id = t3.ASSET_ID AND t3.DEL_FLAG = '0'")
                .leftJoin("AST_DATASOURCE_SPACE_REL t4 on t.DATASOURCE_ID = t4.DATASOURCE_ID")
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), AssetsAssetDO::getCatCode, reqVO.getCatCode())
                .like(StringUtils.isNotBlank(reqVO.getName()), AssetsAssetDO::getName, reqVO.getName())
                .eq(reqVO.getDatasourceId() != null, AssetsAssetDO::getDatasourceId, reqVO.getDatasourceId())
                .like(StringUtils.isNotBlank(reqVO.getTableName()), AssetsAssetDO::getTableName, reqVO.getTableName())
                .eq(StringUtils.isNotBlank(reqVO.getTableComment()), AssetsAssetDO::getTableComment, reqVO.getTableComment())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), AssetsAssetDO::getStatus, reqVO.getStatus())
                .eq(StringUtils.isNotBlank(reqVO.getType()), AssetsAssetDO::getType, reqVO.getType())
                .eq(StringUtils.isNotBlank(reqVO.getDescription()), AssetsAssetDO::getDescription, reqVO.getDescription())
                .and(reqVO.getSpaceId() != null, wrapper -> wrapper.eq("t3.SPACE_ID", reqVO.getSpaceId()).or().eq("t4.SPACE_ID", reqVO.getSpaceId()))
                .and(StringUtils.isNotBlank(reqVO.getSpaceCode()), wrapper -> wrapper.eq("t3.SPACE_CODE", reqVO.getSpaceCode()).or().eq("t4.SPACE_CODE", reqVO.getSpaceCode()))
                .in(reqVO.getThemeAssetIdList() != null && !reqVO.getThemeAssetIdList().isEmpty(), AssetsAssetDO::getId, reqVO.getThemeAssetIdList())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return AssetsAssetMapper.selectJoinList(AssetsAssetDO.class, lambdaWrapper);
    }

    @Override
    public AssetsAssetRespVO getAssetById(Long id) {
        MPJLambdaWrapper<AssetsAssetDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsAssetDO.class).select("t2.NAME AS catName", "dd.DATASOURCE_NAME as datasourceName", "dd.IP as datasourceIp", "dd.DATASOURCE_TYPE as datasourceType", "t3.NAME AS dataLayerName", "t3.ENG_NAME AS dataLayerEngName", "t4.NAME AS businessCategoryName", "t4.ENG_NAME AS businessCategoryEngName", "t5.NAME AS dataDomainName", "t5.ENG_NAME AS dataDomainEngName", "t6.NAME AS themeDomainName", "t6.ENG_NAME AS themeDomainEngName", "u.PHONENUMBER AS createUserPhoneNumber", "u2.PHONENUMBER AS updateUserPhoneNumber").leftJoin("SYSTEM_USER u on t.CREATOR_ID = u.USER_ID AND u.DEL_FLAG = '0'").leftJoin("SYSTEM_USER u2 on t.UPDATER_ID = u2.USER_ID AND u2.DEL_FLAG = '0'").leftJoin("TAX_CATEGORY t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0' AND t2.CAT_TYPE = 'ASSET'").leftJoin("AST_DATASOURCE dd on t.DATASOURCE_ID = dd.ID").leftJoin("MDL_DATA_LAYER t3 ON t.DATA_LAYER_ID = t3.id AND t3.DEL_FLAG = '0'").leftJoin("MDL_BUSINESS_CATEGORY t4 ON t.BUSINESS_CATEGORY_ID = t4.id AND t4.DEL_FLAG = '0'").leftJoin("MDL_DATA_DOMAIN t5 ON t.DATA_DOMAIN_ID = t5.id AND t5.DEL_FLAG = '0'").leftJoin("MDL_THEME_DOMAIN t6 ON t.THEME_DOMAIN_ID = t6.id AND t6.DEL_FLAG = '0'").eq(AssetsAssetDO::getId, id);
        String subSelectSql = "SELECT\n" + "'['|| WM_CONCAT(DISTINCT '{\"tagId\":\"' || d.ID || '\",\"tagName\":\"' || d.name || '\"}' ) ||']'\n" + "FROM \n" + "     TAX_TAG d \n" + "JOIN TAX_TAG_ASSET_REL rel ON d.ID = rel.TAG_ID \n" + "WHERE \n" + "    d.DEL_FLAG ='0' \n" + "    AND rel.ASSET_ID = t.ID \n" + "HAVING COUNT(d.ID) > 0";
        if (org.apache.commons.lang3.StringUtils.equals("mysql", MasterDataSourceConfig.getDatabaseType())) {
            subSelectSql = "SELECT \n" + "    CONCAT(\n" + "        '[', \n" + "        GROUP_CONCAT(\n" + "            DISTINCT CONCAT(\n" + "                '{\"tagId\":\"', d.ID, \n" + "                '\",\"tagName\":\"', d.name, \n" + "                '\"}'\n" + "            )\n" + "        ), \n" + "        ']'\n" + "    ) AS json_result\n" + "FROM \n" + "     TAX_TAG d \n" + "JOIN TAX_TAG_ASSET_REL rel ON d.ID = rel.TAG_ID \n" + "WHERE \n" + "    d.DEL_FLAG ='0' \n" + "    AND rel.ASSET_ID = t.ID \n" + "HAVING COUNT(d.ID) > 0";
        } else if (org.apache.commons.lang3.StringUtils.equals("kingbase8", MasterDataSourceConfig.getDatabaseType())) {
            subSelectSql = "SELECT \n" + "    CONCAT_WS('','[' , STRING_AGG(DISTINCT CONCAT_WS('', '{\"tagId\":\"', d.ID, '\",\"tagName\":\"', d.name, '\"}'), ',') , ']')\n" + "FROM \n" + "     TAX_TAG d \n" + "JOIN TAX_TAG_ASSET_REL rel ON d.ID = rel.TAG_ID \n" + "WHERE \n" + "    d.DEL_FLAG ='0' \n" + "    AND rel.ASSET_ID = t.ID \n" + "HAVING COUNT(d.ID) > 0";
        } else {
            subSelectSql = "SELECT \n" + "    '[' || STRING_AGG(DISTINCT '{\"tagId\":\"' || d.ID || '\",\"tagName\":\"' || d.name || '\"}', ',') || ']'\n" + "FROM \n" + "     TAX_TAG d \n" + "JOIN TAX_TAG_ASSET_REL rel ON d.ID = rel.TAG_ID \n" + "WHERE \n" + "    d.DEL_FLAG ='0' \n" + "    AND rel.ASSET_ID = t.ID \n" + "HAVING COUNT(d.ID) > 0";
        }
        lambdaWrapper.select("(" + subSelectSql + ") AS tags");
        AssetsAssetDO AssetsAssetDO = AssetsAssetMapper.selectJoinOne(AssetsAssetDO.class, lambdaWrapper);
        AssetsAssetThemeRelPageReqVO AssetsAssetThemeRelPageReqVO = new AssetsAssetThemeRelPageReqVO();
        AssetsAssetThemeRelPageReqVO.setAssetId(AssetsAssetDO.getId());
        List<AssetsAssetThemeRelRespVO> AssetsAssetThemeRelList = AssetsAssetThemeRelService.getAssetThemeRelList(AssetsAssetThemeRelPageReqVO);
        AssetsAssetDO.setAssetsAssetThemeRelList(AssetsAssetThemeRelList);
        AssetsAssetRespVO bean = BeanUtils.toBean(AssetsAssetDO, AssetsAssetRespVO.class);
        queryAssetchild(bean);
        if (StringUtils.isNotBlank(bean.getTags())) {
            JSONArray tags = JSONArray.parse(bean.getTags());
            bean.setTagIds(tags.stream().map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getLong("tagId")).collect(Collectors.toList()));
            bean.setTagNames(tags.stream().map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("tagName")).collect(Collectors.toList()));
        }
        return bean;
    }

    @Override
    public AssetsAssetRespVO getAssetByIdSimple(Long id) {
        return BeanUtils.toBean(AssetsAssetMapper.selectById(id), AssetsAssetRespVO.class);
    }

    private void queryAssetchild(AssetsAssetRespVO AssetsAsset) {
        Long assetId = AssetsAsset.getId();
        String type = AssetsAsset.getType();
        if (StringUtils.equals("1", type)) {
            return;
        } else if (StringUtils.equals("4", type)) {
            AssetsAssetGeoRespVO AssetsAssetGeoByAssetId = IAssetsAssetGeoService.getAssetGeoByAssetId(assetId);
            AssetsAsset.setAssetsAssetGeo(AssetsAssetGeoByAssetId);
        } else if (StringUtils.equals("5", type)) {
            AssetsAssetVideoRespVO AssetsAssetVideoByAssetId = IAssetsAssetVideoService.getAssetVideoByAssetId(assetId);
            AssetsAsset.setAssetsAssetVideo(AssetsAssetVideoByAssetId);
        } else if (StringUtils.equals("6", type)) {
            AssetsAssetFilesDO serviceById = AssetsAssetFilesService.getOne(new LambdaQueryWrapperX<AssetsAssetFilesDO>().eq(AssetsAssetFilesDO::getAssetId, assetId));
            AssetsAssetFilesSaveReqVO filesSaveReqVO = BeanUtils.toBean(serviceById, AssetsAssetFilesSaveReqVO.class);
            AssetsAsset.setAssetsAssetFiles(filesSaveReqVO);
        } else if (StringUtils.equals("7", type)) {
            AssetsAssetFileDO fileDO = assetFileMapper.selectByAssetId(assetId);
            if (fileDO != null) {
                AssetsAsset.setFileInfo(fileDO.toFileInfo());
            }
        } else {
            return;
        }
    }

    @Override
    public Long createAsset(AssetsAssetSaveReqVO createReqVO) {
        enrichAssetDescriptionFromMetadata(createReqVO);
        AssetsAssetDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetDO.class);
        AssetsAssetDO existingAsset = StringUtils.equals("1", createReqVO.getType()) ? getExistingTableAsset(createReqVO.getDatasourceId(), createReqVO.getTableName()) : null;
        if (existingAsset != null) {
            dictType.setId(existingAsset.getId());
            AssetsAssetMapper.updateById(dictType);
            return existingAsset.getId();
        }
        AssetsAssetMapper.insert(dictType);
        return dictType.getId();
    }

    private void enrichAssetDescriptionFromMetadata(AssetsAssetSaveReqVO asset) {
        if (asset == null || StringUtils.isNotEmpty(asset.getDescription()) || !StringUtils.equals("1", asset.getType())) {
            return;
        }
        StringBuilder description = new StringBuilder();
        description.append("元数据采集表描述：").append(StringUtils.isNotEmpty(asset.getTableComment()) ? asset.getTableComment() : "-").append('\n');
        description.append("元数据采集数据量：").append(asset.getDataCount() != null ? asset.getDataCount() : "-").append('\n');
        description.append("元数据采集字段数：").append(asset.getFieldCount() != null ? asset.getFieldCount() : "-").append('\n');
        description.append("字段注释：");
        if (CollectionUtils.isEmpty(asset.getAssetColumnList())) {
            description.append("-");
        } else {
            for (AssetsAssetColumnSaveReqVO column : asset.getAssetColumnList()) {
                description.append('\n')
                        .append(StringUtils.isNotEmpty(column.getColumnName()) ? column.getColumnName() : "-")
                        .append("：")
                        .append(StringUtils.isNotEmpty(column.getColumnComment()) ? column.getColumnComment() : "-");
                List<String> typeInfo = new ArrayList<>();
                if (StringUtils.isNotEmpty(column.getColumnType())) {
                    typeInfo.add(column.getColumnType());
                }
                if (column.getColumnLength() != null) {
                    typeInfo.add("长度" + column.getColumnLength());
                }
                if (column.getColumnScale() != null) {
                    typeInfo.add("精度" + column.getColumnScale());
                }
                if (!typeInfo.isEmpty()) {
                    description.append("（").append(String.join("/", typeInfo)).append("）");
                }
            }
        }
        asset.setDescription(description.toString());
    }

    private AssetsAssetDO getExistingTableAsset(Long datasourceId, String tableName) {
        if (datasourceId == null || StringUtils.isEmpty(tableName)) {
            return null;
        }
        return AssetsAssetMapper.selectOne(Wrappers.<AssetsAssetDO>lambdaQuery()
                .eq(AssetsAssetDO::getDatasourceId, datasourceId)
                .eq(AssetsAssetDO::getTableName, tableName)
                .eq(AssetsAssetDO::getType, "1")
                .last("LIMIT 1"));
    }

    @Override
    public int updateAsset(AssetsAssetSaveReqVO updateReqVO) {
// Ã§ÂÂ¸Ã¥ÂÂ³Ã¦Â Â¡Ã©ÂªÂ
// Ã¦ÂÂ´Ã¦ÂÂ°Ã¦ÂÂ°Ã¦ÂÂ®Ã¨ÂµÂÃ¤ÂºÂ§
        AssetsAssetDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetDO.class);
        return AssetsAssetMapper.updateById(updateObj);
    }

    @Override
    public int removeAsset(Collection<Long> idList) {
        ArrayList<Long> assetIdList = new ArrayList<>(idList);
        int asset = collectorEtlTaskService.checkTaskIdInAsset(assetIdList);
        if (asset > 0) {
            throw new ServiceException(",!");
        }
// Ã¦ÂÂ¹Ã©ÂÂÃ¥ÂÂ Ã©ÂÂ¤Ã¦ÂÂ°Ã¦ÂÂ®Ã¨ÂµÂÃ¤ÂºÂ§
        return AssetsAssetMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeAsset(Long id) {
        ArrayList<Long> assetIdList = new ArrayList<>();
        assetIdList.add(id);
        int asset = collectorEtlTaskService.checkTaskIdInAsset(assetIdList);
        if (asset > 0) {
            throw new ServiceException(",!");
        }
        AssetsAssetDO AssetsAssetDO = AssetsAssetMapper.selectById(id);
        if (AssetsAssetDO == null) {
            return 1;
        }
//Ã¥ÂÂ Ã©ÂÂ¤Ã©Â¡Â¹Ã§ÂÂ®
        IAssetsAssetSpaceRelService.removeSpaceRelByAssetId(id);
//Ã¥ÂÂ Ã©ÂÂ¤Ã¤Â¸Â»Ã©Â¢Â
        AssetsAssetThemeRelService.removeThemeRelByAssetId(id);
        AssetsAssetMapper.deleteAssetById(id);
// Ã¦ÂÂ¹Ã©ÂÂÃ¥ÂÂ Ã©ÂÂ¤Ã¦ÂÂ°Ã¦ÂÂ®Ã¨ÂµÂÃ¤ÂºÂ§
// Ã¦ÂÂ´Ã¦ÂÂ°Ã¦Â ÂÃ§Â­Â¾Ã¨ÂµÂÃ¤ÂºÂ§Ã¦ÂÂ°Ã©ÂÂ
        taxonomyTagAssetRelApiService.deleteRelByUpdateTag(id);
        return 1;
    }

    @Override
    public List<AssetsAssetDO> getAssetList() {
        return AssetsAssetMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetDO> getAssetMap() {
        List<AssetsAssetDO> AssetsAssetList = AssetsAssetMapper.selectList();
        return AssetsAssetList.stream().collect(Collectors.toMap(AssetsAssetDO::getId, AssetsAssetDO -> AssetsAssetDO,
// Ã¤Â¿ÂÃ§ÂÂÃ¥Â·Â²Ã¥Â­ÂÃ¥ÂÂ¨Ã§ÂÂÃ¥ÂÂ¼
                (existing, replacement) -> existing));
    }

    /**
     * *     * @param importExcelList      * @param isUpdateSupport      * @param operName             * @return
     */
    @Override
    public String importAsset(List<AssetsAssetRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }
        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();
        for (AssetsAssetRespVO respVO : importExcelList) {
            try {
                AssetsAssetDO AssetsAssetDO = BeanUtils.toBean(respVO, AssetsAssetDO.class);
                Long AssetsAssetId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsAssetId != null) {
                        AssetsAssetDO existingAsset = AssetsAssetMapper.selectById(AssetsAssetId);
                        if (existingAsset != null) {
                            AssetsAssetMapper.updateById(AssetsAssetDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsAssetDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsAssetId);
                    AssetsAssetDO existingAsset = AssetsAssetMapper.selectOne(queryWrapper);
                    if (existingAsset == null) {
                        AssetsAssetMapper.insert(AssetsAssetDO);
                        successNum++;
                        successMessages.add("ID " + AssetsAssetId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsAssetId + " ");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append(" ").append(failureNum).append(" ");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append(" ").append(successNum).append(" ");
        }
        return resultMsg.toString();
    }

    /**
     * *     * @param jsonObject id     * @return
     */
    @Override
    public Map<String, Object> getColumnData(JSONObject jsonObject) {
        String tableName = "";
        Long DataSourceId = null;
        if (StringUtils.isEmpty(jsonObject.getStr("pageNum")) || StringUtils.isEmpty(jsonObject.getStr("pageSize"))) {
            throw new DataQueryException("");
        }
// Ã¦ÂÂ¥Ã¨Â¯Â¢Ã¦ÂÂ°Ã¦ÂÂ®
        Integer pageNum = Integer.valueOf(jsonObject.getStr("pageNum"));
        Integer pageSize = Integer.valueOf(jsonObject.getStr("pageSize"));
// Ã¨ÂÂ·Ã¥ÂÂÃ¨ÂµÂÃ¤ÂºÂ§Ã¨Â¯Â¦Ã¦ÂÂ
        AssetsAssetRespVO AssetsAssetDO = this.getAssetById(Long.valueOf(jsonObject.getStr("id")));
        if (StringUtils.equals("6", AssetsAssetDO.getType())) {
            AssetsAssetFilesDO filesServiceOne = AssetsAssetFilesService.getOne(new LambdaQueryWrapperX<AssetsAssetFilesDO>().eq(AssetsAssetFilesDO::getAssetId, AssetsAssetDO.getId()));
            if (SUPPORTED_EXTENSIONS.contains(filesServiceOne.getType())) {
                String fixedResourceUrl = profile.replace("\\", "/").replaceAll("/+$", "").replaceAll("/profile", "");
                String url = filesServiceOne.getUrl().replaceAll("/profile", "");
                Map<String, Object> fileData = FileDataReaderUtil.readFileData(fixedResourceUrl + url, pageNum.longValue(), pageSize.longValue(), filesServiceOne.getStartData(), filesServiceOne.getStartColumn(), jsonObject.getStr("filter"));
                return fileData;
            }
        }
        tableName = AssetsAssetDO.getTableName();
        DataSourceId = AssetsAssetDO.getDatasourceId();
// Ã¨ÂÂ·Ã¥ÂÂÃ¦ÂÂ°Ã¦ÂÂ®Ã¦ÂºÂÃ¨Â¿ÂÃ¦ÂÂ¥Ã¤Â¿Â¡Ã¦ÂÂ¯
        AssetsDatasourceDO AssetsDatasourceDO = AssetsDatasourceMapper.selectById(DataSourceId);
        if (AssetsDatasourceDO == null) {
            return null;
        }
        DbQueryProperty dbQueryProperty = new DbQueryProperty(AssetsDatasourceDO.getDatasourceType(), AssetsDatasourceDO.getIp(), AssetsDatasourceDO.getPort(), AssetsDatasourceDO.getDatasourceConfig());
        DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
        DbDialect dbDialect = DialectFactory.getDialect(DbType.getDbType(dbQueryProperty.getDbType()));
        if (!dbQuery.valid()) {
            dbQuery.close();
            throw new DataQueryException("");
        }
        int existsSQL = dbQuery.generateCheckTableExistsSQL(dbQueryProperty, tableName);
        if (existsSQL == 0) {
            dbQuery.close();
            throw new DataQueryException("!");
        }
// Ã¨ÂÂ·Ã¥ÂÂÃ¥Â­ÂÃ¦Â®ÂµÃ©ÂÂÃ¥ÂÂ
        List<DbColumn> columns = redisCache.getCacheList(CacheConstants.ASSET_PREVIEW_KEY + AssetsDatasourceDO.getId() + "_" + tableName);
// Ã¨ÂÂ·Ã¥ÂÂÃ¨ÂµÂÃ¤ÂºÂ§Ã§ÂÂÃ¥Â­ÂÃ¦Â®Âµ
        boolean columnAuthScoped = StringUtils.isNotEmpty(jsonObject.getStr("spaceId")) || StringUtils.isNotEmpty(jsonObject.getStr("spaceCode"));
        List<AssetsAssetColumnDO> authorizedAssetColumns;
        if (StringUtils.isNotEmpty(jsonObject.getStr("id"))) {
            AssetsAssetColumnPageReqVO columnReqVO = new AssetsAssetColumnPageReqVO();
            columnReqVO.setAssetId(jsonObject.getLong("id"));
            if (StringUtils.isNotEmpty(jsonObject.getStr("spaceId"))) {
                columnReqVO.setSpaceId(Long.valueOf(jsonObject.getStr("spaceId")));
            }
            columnReqVO.setSpaceCode(jsonObject.getStr("spaceCode"));
            authorizedAssetColumns = AssetsAssetColumnMapper.selectListByAuth(columnReqVO);
        } else {
            authorizedAssetColumns = Collections.emptyList();
        }
        List<DbColumn> AssetsAssetColumns = authorizedAssetColumns.stream().map(e -> e.toDbColumn()).collect(Collectors.toList());
        if (columns.isEmpty()) {
//Ã¨ÂÂ·Ã¥ÂÂÃ¨Â¡Â¨Ã§ÂÂÃ¥Â­ÂÃ¦Â®Âµ
            columns = dbQuery.getTableColumns(dbQueryProperty, tableName);
            if (columns.size() == 0) {
                dbQuery.close();
                throw new DataQueryException("");
            }
            redisCache.setCacheList(CacheConstants.ASSET_PREVIEW_KEY + AssetsDatasourceDO.getId() + "_" + tableName, columns);
            redisCache.expire(CacheConstants.ASSET_PREVIEW_KEY + AssetsDatasourceDO.getId() + "_" + tableName, 5, TimeUnit.MINUTES);
        }
// Ã¦ÂÂ¼Ã¦ÂÂ¥Ã¦ÂÂ¥Ã¨Â¯Â¢sqlÃ¨Â¯Â­Ã¥ÂÂ¥
        // 权限字段为空时回退到数据库表字段，避免返回空列定义和空数据
        List<DbColumn> columnsForDisplay = CollectionUtils.isNotEmpty(AssetsAssetColumns) ? AssetsAssetColumns : columns;
        
        List<Map<String, Object>> columnTable = new ArrayList<>();
        for (DbColumn column : columnsForDisplay) {
            Map<String, Object> columnMap = new HashMap<>();
            columnMap.put("field", column.getColName());
            columnMap.put("en", column.getColName());
            columnMap.put("cn", column.getColComment());
            columnMap.put("columnNullable", column.getNullable());
            columnMap.put("columnKey", column.getColKey());
            columnTable.add(columnMap);
        }
        
        if (columnAuthScoped && CollectionUtils.isEmpty(AssetsAssetColumns) && columns.isEmpty()) {
            Map<String, Object> Data = new HashMap<>();
            Data.put("columns", columnTable);
            Data.put("tableData", Collections.emptyList());
            Data.put("total", 0);
            dbQuery.close();
            return Data;
        }
        List<Map> orderByList = jsonObject.getBeanList("orderBy", Map.class);
        PageUtil pageUtil = new PageUtil(pageNum, pageSize);
        List<Map<String, Object>> queryList;
        List<DbColumn> queryColumns = CollectionUtils.isNotEmpty(AssetsAssetColumns) ? AssetsAssetColumns : columns;
        queryList = dbQuery.queryDbColumnByList(queryColumns, tableName, dbQueryProperty, jsonObject.getStr("filter"), orderByList, pageUtil.getOffset(), pageSize);
        int total = dbQuery.countNew(tableName, dbQueryProperty, jsonObject.getStr("filter"));
        Map<String, Object> Data = new HashMap<>();
        Data.put("columns", columnTable);
        Data.put("tableData", queryList);
        Data.put("total", total);
        dbQuery.close();
        return Data;
    }

    @Override
    public List<Map<String, Object>> dataMasking(Long assetId, List<Map<String, Object>> Data) {
// 1) Ã¥Â­ÂÃ¦Â®ÂµÃ¥ÂÂÃ¦ÂÂ°Ã¦ÂÂ®Ã¯Â¼ÂÃ¦ÂÂÃ¥Â­ÂÃ¦Â®ÂµÃ¥ÂÂÃ¥Â¤Â§Ã¥ÂÂÃ¥ÂÂ¹Ã©ÂÂÃ¯Â¼Â
        List<AssetsAssetColumnDO> cols = AssetsAssetColumnMapper.findByAssetId(assetId);
        Map<String, AssetsAssetColumnDO> colMap = cols.stream().collect(Collectors.toMap(c -> c.getColumnName().toUpperCase(), c -> c, (a, b) -> a));
// 2) Ã¦ÂÂÃ¦ÂÂÃ§Â­ÂÃ§ÂºÂ§Ã¯Â¼ÂÃ¤Â»ÂÃ¥ÂÂ¨Ã§ÂºÂ¿Ã¯Â¼Â
        Map<Long, AssetsSensitiveLevelDO> levelMap = AssetsSensitiveLevelMapper.selectList(new QueryWrapper<AssetsSensitiveLevelDO>().eq("online_flag", "1")).stream().collect(Collectors.toMap(AssetsSensitiveLevelDO::getId, x -> x, (a, b) -> a));
        List<Map<String, Object>> out = new ArrayList<>(Data.size());
        for (Map<String, Object> row : Data) {
// Ã§ÂÂ¨ LinkedHashMap Ã¤Â¿ÂÃ¦ÂÂÃ¥Â­ÂÃ¦Â®ÂµÃ©Â¡ÂºÃ¥ÂºÂÃ¯Â¼ÂÃ¤Â¸ÂÃ¤Â¸ÂÃ¤Â¿Â®Ã¦ÂÂ¹Ã¥ÂÂ map
            Map<String, Object> masked = new HashMap<>(row.size());
            for (Map.Entry<String, Object> e : row.entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
// Ã¤Â¿ÂÃ¨Â¯Â _id Ã¥Â§ÂÃ§Â»ÂÃ¦ÂÂ¯Ã¥Â­ÂÃ§Â¬Â¦Ã¤Â¸Â²
                if ("_id".equalsIgnoreCase(key) && val != null && "org.bson.types.ObjectId".equals(val.getClass().getName())) {
                    val = val.toString();
                    masked.put(key, val);
                    continue;
                }
// Ã¢ÂÂÃ¢ÂÂ Ã¦ÂÂªÃ¥ÂÂ¹Ã©ÂÂÃ¥ÂÂ°Ã©ÂÂÃ§Â½Â® Ã¦ÂÂ Ã¦ÂÂ Ã¦ÂÂÃ¦ÂÂÃ§Â­ÂÃ§ÂºÂ§ Ã¢ÂÂ Ã¥ÂÂÃ¦Â Â·Ã¨Â¿ÂÃ¥ÂÂ
                AssetsAssetColumnDO meta = colMap.get(key.toUpperCase());
                if (meta == null || meta.getSensitiveLevelId() == null) {
                    masked.put(key, val);
                    continue;
                }
                AssetsSensitiveLevelDO lvl = levelMap.get(meta.getSensitiveLevelId());
                if (lvl == null) {
                    masked.put(key, val);
                    continue;
                }
// Ã¤Â»ÂÃ¥Â¯Â¹Ã¥Â­ÂÃ§Â¬Â¦Ã¤Â¸Â²Ã¨ÂÂ±Ã¦ÂÂÃ¯Â¼ÂÃ¥ÂÂ¶Ã¤Â»ÂÃ§Â±Â»Ã¥ÂÂÃ¥ÂÂÃ¦Â Â·Ã¨Â¿ÂÃ¥ÂÂ
                if (!(val instanceof CharSequence)) {
                    masked.put(key, val);
                    continue;
                }
                String s = val == null ? null : val.toString();
                if (s == null || s.isEmpty()) {
                    masked.put(key, s);
                    continue;
                }
// Ã¨ÂµÂ·Ã¦Â­Â¢Ã¤Â½ÂÃ§Â½Â®Ã¯Â¼Âstart/end Ã¤Â¸Âº 1 Ã¥ÂÂºÃ¯Â¼Ânull Ã¥ÂÂÃ¥ÂÂ¨Ã¨Â¦ÂÃ§ÂÂ
                int len = s.length();
                int start = lvl.getStartCharLoc() == null ? 1 : lvl.getStartCharLoc().intValue();
                int end = lvl.getEndCharLoc() == null ? len : lvl.getEndCharLoc().intValue();
// Ã¨Â§ÂÃ¨ÂÂÃ¨Â¾Â¹Ã§ÂÂÃ¥Â¹Â¶Ã¤Â¿ÂÃ¨Â¯Â start<=end
                start = Math.max(1, start);
                end = Math.min(len, end);
                if (start > end) {
// Ã¦ÂÂ Ã¦ÂÂÃ¦ÂÂÃ¨Â¦ÂÃ§ÂÂÃ¥ÂÂºÃ©ÂÂ´ Ã¢ÂÂ Ã¥ÂÂÃ¦Â Â·
                    masked.put(key, s);
                    continue;
                }
                String maskUnit = lvl.getMaskCharacter();
                if (maskUnit == null || maskUnit.isEmpty()) maskUnit = "*";
                int coverLen = end - start + 1;
                String midMask = repeat(maskUnit, coverLen);
// Ã¦ÂÂ¯Ã¦ÂÂÃ¥Â¤ÂÃ¥Â­ÂÃ§Â¬Â¦Ã¦ÂÂ©Ã§Â ÂÃ¯Â¼ÂÃ¤Â¸ÂÃ¤Â¼ÂÃ¤Â½ÂÃ§Â§Â»
                String res = s.substring(0, start - 1) + midMask + s.substring(end);
                masked.put(key, res);
            }
            out.add(masked);
        }
        return out;
    }

    /**
     * maskUnit
     */
    private static String repeat(String maskUnit, int targetLen) {
        if (targetLen <= 0) return "";
        if (maskUnit == null || maskUnit.isEmpty()) maskUnit = "*";
        StringBuilder sb = new StringBuilder(targetLen);
        while (sb.length() + maskUnit.length() <= targetLen) sb.append(maskUnit);
        int remain = targetLen - sb.length();
        if (remain > 0) sb.append(maskUnit, 0, remain);
        return sb.toString();
    }

    @Override
    public void insertAssetByDiscoveryInfo(AssetsAssetPageReqVO AssetsAssetReqVO, List<AssetsAssetColumnSaveReqVO> columnSaveReqVOList) {
        AssetsAssetDO AssetsAssetDO = BeanUtils.toBean(AssetsAssetReqVO, AssetsAssetDO.class);
//Ã¥ÂÂ¤Ã¦ÂÂ­Ã¦ÂÂ¯Ã¥ÂÂ¦Ã¥Â­ÂÃ¥ÂÂ¨Ã¨ÂµÂÃ¤ÂºÂ§
        AssetsAssetPageReqVO AssetsAssetPageReqVO = new AssetsAssetPageReqVO();
        AssetsAssetPageReqVO.setTableName(AssetsAssetDO.getTableName());
        AssetsAssetPageReqVO.setDatasourceId(AssetsAssetDO.getDatasourceId());
        AssetsAssetDO assetDO = this.getAssetByAssetPageReqVO(AssetsAssetPageReqVO);
        if (assetDO != null) {
            AssetsAssetDO.setId(assetDO.getId());
            AssetsAssetMapper.updateById(AssetsAssetDO);
//Ã¦Â·Â»Ã¥ÂÂ Ã¨ÂµÂÃ¤ÂºÂ§Ã¦ÂÂ°Ã¦ÂÂ®
        } else {
            AssetsAssetMapper.insert(AssetsAssetDO);
//Ã¦Â·Â»Ã¥ÂÂ Ã¨ÂµÂÃ¤ÂºÂ§Ã¦ÂÂ°Ã¦ÂÂ®
        }
        List<String> themeIdList = AssetsAssetReqVO.getThemeIdList();
        if (CollectionUtils.isNotEmpty(themeIdList)) {
            AssetsAssetThemeRelService.createAssetThemeRelList(themeIdList, AssetsAssetDO.getId());
        }
        List<AssetsAssetColumnDO> AssetsAssetColumnList = new ArrayList<>();
        if (assetDO != null) {
            AssetsAssetColumnPageReqVO AssetsAssetColumnPageReqVO = new AssetsAssetColumnPageReqVO();
            AssetsAssetColumnPageReqVO.setAssetId(assetDO.getId());
            List<AssetsAssetColumnDO> AssetsAssetColumnList1 = IAssetsAssetColumnService.getAssetColumnList(AssetsAssetColumnPageReqVO);
            AssetsAssetColumnList = CollectionUtils.isEmpty(AssetsAssetColumnList1) ? AssetsAssetColumnList : AssetsAssetColumnList1;
        }
        Map<String, Long> columnNameToIdMap = AssetsAssetColumnList.stream().filter(columnDO -> columnDO.getColumnName() != null).collect(Collectors.toMap(AssetsAssetColumnDO::getColumnName, AssetsAssetColumnDO::getId, (id1, id2) -> id1));
        for (AssetsAssetColumnSaveReqVO reqVO : columnSaveReqVOList) {
            if (reqVO.getColumnName() != null) {
                Long id = columnNameToIdMap.get(reqVO.getColumnName());
                if (id != null) {
                    reqVO.setId(id);
                }
            }
        }
        Collection<Long> nonExistingIdList = this.findMissingColumnIds(AssetsAssetColumnList, columnSaveReqVOList);
        if (CollectionUtils.isNotEmpty(nonExistingIdList)) {
            IAssetsAssetColumnService.removeAssetColumn(nonExistingIdList);
        }
        Long AssetsAssetDOId = AssetsAssetDO.getId();
        for (AssetsAssetColumnSaveReqVO AssetsAssetColumnSaveReqVO : columnSaveReqVOList) {
            AssetsAssetColumnSaveReqVO.setAssetId(AssetsAssetDOId);
            Long columnId;
            if (AssetsAssetColumnSaveReqVO.getId() == null) {
                columnId = IAssetsAssetColumnService.createAssetColumn(AssetsAssetColumnSaveReqVO);
            } else {
                IAssetsAssetColumnService.updateAssetColumn(AssetsAssetColumnSaveReqVO);
                columnId = AssetsAssetColumnSaveReqVO.getId();
            }
            AssetsAssetReqVO.setId(AssetsAssetDOId);
            createAssetColumnSpaceRel(BeanUtils.toBean(AssetsAssetReqVO, AssetsAssetSaveReqVO.class), columnId);
        }
    }

    public Collection<Long> findMissingColumnIds(List<AssetsAssetColumnDO> AssetsAssetColumnList, List<AssetsAssetColumnSaveReqVO> columnSaveReqVOList) {
        if (AssetsAssetColumnList == null) {
            return Collections.emptyList();
        }
        Set<String> existingColumnNames = columnSaveReqVOList == null ? Collections.emptySet() : columnSaveReqVOList.stream().filter(vo -> vo.getColumnName() != null).map(AssetsAssetColumnSaveReqVO::getColumnName).collect(Collectors.toSet());
        return AssetsAssetColumnList.stream().filter(doObj -> doObj.getColumnName() != null && !existingColumnNames.contains(doObj.getColumnName())).map(AssetsAssetColumnDO::getId).collect(Collectors.toList());
    }

    @Override
    public void updateAssetByDiscoveryInfo(AssetsAssetPageReqVO AssetsAssetReqVO) {
        AssetsAssetDO AssetsAssetDO = BeanUtils.toBean(AssetsAssetReqVO, AssetsAssetDO.class);
//Ã¥ÂÂ¤Ã¦ÂÂ­Ã¦ÂÂ¯Ã¥ÂÂ¦Ã¥Â­ÂÃ¥ÂÂ¨Ã¨ÂµÂÃ¤ÂºÂ§
        AssetsAssetPageReqVO AssetsAssetPageReqVO = new AssetsAssetPageReqVO();
        AssetsAssetPageReqVO.setTableName(AssetsAssetDO.getTableName());
        AssetsAssetPageReqVO.setDatasourceId(AssetsAssetDO.getDatasourceId());
        AssetsAssetDO assetDO = this.getAssetByAssetPageReqVO(AssetsAssetPageReqVO);
        if (assetDO == null) {
            return;
        }
        AssetsAssetMapper.deleteAssetById(assetDO.getId());
        AssetsAssetColumnMapper.deleteAssetColumnByAssetId(assetDO.getId());
        AssetsAssetThemeRelService.removeThemeRelByAssetId(assetDO.getId());
    }

    @Override
    public PageResult<AssetsAssetDO> getCollectorAssetPage(AssetsAssetPageReqVO AssetsAsset) {
        if (SecurityUtils.hasPermi(Constants.ALL_PERMISSION)) {
            AssetsAsset.setSpaceId(null);
            AssetsAsset.setSpaceCode(null);
            AssetsAsset.setAssetIdList(null);
            return this.getAssetPage(AssetsAsset, "2");
        }
        if (StringUtils.isEmpty(AssetsAsset.getSpaceCode()) || AssetsAsset.getSpaceId() == null) {
            return new PageResult<AssetsAssetDO>();
        }
        LambdaQueryWrapperX<AssetsAssetApplyDO> queryWrapperX = new LambdaQueryWrapperX();
        String[] sourceTypeArr = AssetsAsset.getParams().get("sourceType") == null ? null : AssetsAsset.getParams().get("sourceType").toString().split(",");
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getStatus, "3");
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getSpaceId, AssetsAsset.getSpaceId());
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getSpaceCode, AssetsAsset.getSpaceCode());
        queryWrapperX.inIfPresent(AssetsAssetApplyDO::getSourceType, sourceTypeArr);
        List<AssetsAssetApplyDO> applyDOList = AssetsAssetApplyMapper.selectList(queryWrapperX);
        List<Long> assetIdList;
        Map<Long, AssetsAssetApplyDO> AssetsAssetApplyDOMap;
        if (applyDOList.isEmpty()) {
            assetIdList = new ArrayList<>();
            AssetsAssetApplyDOMap = new HashMap<>();
        } else {
            AssetsAssetApplyDOMap = applyDOList.stream().collect(Collectors.toMap(AssetsAssetApplyDO::getAssetId, AssetsAssetApplyDO -> AssetsAssetApplyDO));
            assetIdList = AssetsAssetApplyDOMap.keySet().stream().collect(Collectors.toList());
        }
        AssetsAsset.setAssetIdList(assetIdList);
        PageResult<AssetsAssetDO> AssetsAssetPage = this.getAssetPage(AssetsAsset, "2");
        if (CollectionUtils.isEmpty(AssetsAssetPage.getRows())) {
            return AssetsAssetPage;
        }
        for (Object assetPageRow : AssetsAssetPage.getRows()) {
            AssetsAssetDO AssetsAssetDO = (AssetsAssetDO) assetPageRow;
            AssetsAssetApplyDO AssetsAssetApplyDO = AssetsAssetApplyDOMap.get(AssetsAssetDO.getId()) == null ? new AssetsAssetApplyDO() : AssetsAssetApplyDOMap.get(AssetsAssetDO.getId());
            if (assetIdList.contains(AssetsAssetDO.getId())) {
                AssetsAssetDO.setSourceType(AssetsAssetApplyDO.getSourceType());
            } else {
                AssetsAssetDO.setSourceType("1");
            }
        }
        return AssetsAssetPage;
    }

    @Override
    public List<AssetsAssetDO> getCollectorAssetNoPageList(AssetsAssetPageReqVO AssetsAsset) {
        if (SecurityUtils.hasPermi(Constants.ALL_PERMISSION)) {
            AssetsAsset.setSpaceId(null);
            AssetsAsset.setSpaceCode(null);
            AssetsAsset.setAssetIdList(null);
            AssetsAsset.setPageSize(PageParam.PAGE_SIZE_NONE);
            return (List<AssetsAssetDO>) this.getAssetPage(AssetsAsset, "2").getRows();
        }
        if (StringUtils.isEmpty(AssetsAsset.getSpaceCode()) || AssetsAsset.getSpaceId() == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapperX<AssetsAssetApplyDO> queryWrapperX = new LambdaQueryWrapperX();
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getStatus, "3");
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getSpaceId, AssetsAsset.getSpaceId());
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getSpaceCode, AssetsAsset.getSpaceCode());
        List<AssetsAssetApplyDO> applyDOList = AssetsAssetApplyMapper.selectList(queryWrapperX);
        if (applyDOList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> assetIdList = applyDOList.stream().collect(Collectors.toMap(AssetsAssetApplyDO::getAssetId, AssetsAssetApplyDO -> AssetsAssetApplyDO)).keySet().stream().collect(Collectors.toList());
        LambdaQueryWrapperX<AssetsAssetDO> AssetsAssetQueryWrapper = new LambdaQueryWrapperX<>();
        AssetsAssetQueryWrapper.inIfPresent(AssetsAssetDO::getId, assetIdList);
        List<AssetsAssetDO> AssetsAssetDOList = AssetsAssetMapper.selectList(AssetsAssetQueryWrapper);
        return AssetsAssetDOList;
    }

    @Override
    public Long createAssetNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (StringUtils.equals("1", AssetsAsset.getCreateType())) {
            setAssetDefaultValues(AssetsAsset);
            Long assetId = createAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
            createAssetSpaceRel(AssetsAsset);
            createAssetThemeIdList(AssetsAsset);
            return AssetsAsset.getId();
        }
//1:Ã¦ÂÂ°Ã¦ÂÂ®Ã¥ÂºÂÃ¨Â¡Â¨  2:Ã¥Â¤ÂÃ©ÂÂ¨API 3: Ã¥ÂÂ°Ã§ÂÂÃ§Â©ÂºÃ©ÂÂ´Ã¦ÂÂÃ¥ÂÂ¡ 4:Ã§ÂÂ¢Ã©ÂÂÃ¦ÂÂ°Ã¦ÂÂ® 5:Ã¨Â§ÂÃ©Â¢ÂÃ¦ÂÂ°Ã¦ÂÂ®
        String type = AssetsAsset.getType();
        if (StringUtils.equals("1", type)) {
            createAssetColumnNew(AssetsAsset);
        } else if (StringUtils.equals("4", type)) {
            setAssetDefaultValues(AssetsAsset);
            createAssetGeoNew(AssetsAsset);
        } else if (StringUtils.equals("5", type)) {
            setAssetDefaultValues(AssetsAsset);
            createAssetVideoNew(AssetsAsset);
        } else if (StringUtils.equals("6", type)) {
            setAssetDefaultValues(AssetsAsset);
            createAssetFilesNew(AssetsAsset);
        } else if ("7".equals(type)) {
            AssetsAsset.setTableName("-1");
            AssetsAsset.setDataCount(0L);
            AssetsAsset.setFieldCount(0L);
            createAssetFileNew(AssetsAsset);
        } else {
            throw new ServiceException("");
        }
        createAssetSpaceRel(AssetsAsset);
        createAssetThemeIdList(AssetsAsset);
        createPendingAssetApply(AssetsAsset);
        return AssetsAsset.getId();
    }

    private void createPendingAssetApply(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset == null || AssetsAsset.getId() == null || AssetsAsset.getSpaceId() == null) {
            return;
        }
        LambdaQueryWrapperX<AssetsAssetApplyDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(AssetsAssetApplyDO::getAssetId, AssetsAsset.getId())
                .eq(AssetsAssetApplyDO::getSpaceId, AssetsAsset.getSpaceId())
                .eqIfPresent(AssetsAssetApplyDO::getSpaceCode, AssetsAsset.getSpaceCode());
        AssetsAssetApplyDO existingApply = AssetsAssetApplyMapper.selectOne(queryWrapper);
        if (existingApply != null) {
            if ("2".equals(existingApply.getStatus())) {
                existingApply.setStatus("1");
                existingApply.setApprovalReason(null);
                existingApply.setUpdateBy(AssetsAsset.getCreateBy());
                existingApply.setUpdatorId(AssetsAsset.getCreatorId());
                existingApply.setUpdateTime(new Date());
                AssetsAssetApplyMapper.updateById(existingApply);
            }
            return;
        }
        AssetsAssetApplyDO apply = new AssetsAssetApplyDO();
        apply.setAssetId(AssetsAsset.getId());
        apply.setSpaceId(AssetsAsset.getSpaceId());
        apply.setSpaceCode(AssetsAsset.getSpaceCode());
        apply.setSourceType(AssetsAsset.getSourceType());
        apply.setApplyReason("新增数据资产");
        apply.setStatus("1");
        apply.setCreatorId(AssetsAsset.getCreatorId());
        apply.setCreateBy(AssetsAsset.getCreateBy());
        apply.setCreateTime(AssetsAsset.getCreateTime() != null ? AssetsAsset.getCreateTime() : new Date());
        AssetsAssetApplyMapper.insert(apply);
    }

    @Override
    public Long createAssetBindResources(AssetsAssetSaveReqVO AssetsAsset) {
//1:Ã¦ÂÂ°Ã¦ÂÂ®Ã¥ÂºÂÃ¨Â¡Â¨  2:Ã¥Â¤ÂÃ©ÂÂ¨API 3: Ã¥ÂÂ°Ã§ÂÂÃ§Â©ÂºÃ©ÂÂ´Ã¦ÂÂÃ¥ÂÂ¡ 4:Ã§ÂÂ¢Ã©ÂÂÃ¦ÂÂ°Ã¦ÂÂ® 5:Ã¨Â§ÂÃ©Â¢ÂÃ¦ÂÂ°Ã¦ÂÂ®
        String type = AssetsAsset.getType();
        if (StringUtils.equals("1", type)) {
            createAssetColumnNew(AssetsAsset);
        } else if (StringUtils.equals("4", type)) {
            setAssetDefaultValues(AssetsAsset);
            createAssetGeoNew(AssetsAsset);
        } else if (StringUtils.equals("5", type)) {
            setAssetDefaultValues(AssetsAsset);
            createAssetVideoNew(AssetsAsset);
        } else if (StringUtils.equals("6", type)) {
            setAssetDefaultValues(AssetsAsset);
            createAssetFilesNew(AssetsAsset);
        } else {
            throw new ServiceException("");
        }
//
        createAssetSpaceRel(AssetsAsset);
        createAssetThemeIdList(AssetsAsset);
        AssetsAsset.setCreateType("2");
        updateAsset(AssetsAsset);
        return 1L;
    }

    private void createAssetFilesNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getId() == null) {
            Long assetId = createAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        AssetsAssetFilesSaveReqVO AssetsAssetFiles = AssetsAsset.getAssetsAssetFiles();
        AssetsAssetFiles.setAssetId(AssetsAsset.getId());
        int lastDot = AssetsAssetFiles.getUrl().lastIndexOf('.');
        String type = AssetsAssetFiles.getUrl().substring(lastDot);
        AssetsAssetFiles.setType(type);
        AssetsAssetFilesService.createAssetFiles(AssetsAssetFiles);
        if (StringUtils.equalsIgnoreCase(".xls", AssetsAssetFiles.getType()) || StringUtils.equalsIgnoreCase(".xlsx", AssetsAssetFiles.getType())) {
            List<AssetsAssetColumnDO> AssetsAssetColumnDOS = getExcelColumn(AssetsAssetFiles.getUrl(), AssetsAssetFiles.getStartColumn(), AssetsAssetFiles.getStartData(), AssetsAsset.getId());
            IAssetsAssetColumnService.saveBatch(AssetsAssetColumnDOS);
        }
        if (StringUtils.equalsIgnoreCase(".csv", AssetsAssetFiles.getType())) {
            List<AssetsAssetColumnDO> AssetsAssetColumnDOS = getCsvColumn(AssetsAssetFiles.getUrl(), AssetsAsset.getId());
            IAssetsAssetColumnService.saveBatch(AssetsAssetColumnDOS);
        }
    }

    private void createAssetFileNew(AssetsAssetSaveReqVO AssetsAsset) {
        Assert.notNull(AssetsAsset.getFileInfo(), () -> new ServiceException(""));
        AssetsDatasourceDO AssetsDatasourceDO = AssetsDatasourceMapper.selectById(AssetsAsset.getDatasourceId());
        if (AssetsAsset.getId() == null) {
            Long assetId = createAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        FileInfo fileInfo = AssetsAsset.getFileInfo();
        AssetsAssetFileDO fileDO = new AssetsAssetFileDO();
        fileDO.setAssetId(AssetsAsset.getId());
        fileDO.setFileCreateTime(null);
        fileDO.setFileSource(AssetsDatasourceDO.getDatasourceType());
        fileDO.setFileName(fileInfo.getName());
        fileDO.setFileUpdateTime(fileInfo.getLastModified());
        fileDO.setFileUrl(fileInfo.getPath());
        fileDO.setFileSize(fileInfo.getSize());
        fileDO.setFileType(fileInfo.getType());
        assetFileMapper.insert(fileDO);
    }

    private List<AssetsAssetColumnDO> getExcelColumn(String excelFile, Integer startColumn, Integer startData, Long assetId) {
        excelFile = AniviaConfig.getProfile() + excelFile.replace(Constants.RESOURCE_PREFIX + "/", "");
        excelFile = excelFile.replace("/", File.separator);
        String csvFile = resourceUrl + "csv" + File.separator + UUID.randomUUID().toString().replace("-", "") + ".csv";
        List<String> columnList = ExcelToCsvUtil.convertExcelToCsv(excelFile, csvFile, startColumn, startData);
        if (columnList.size() > 0) {
            if (!ExcelToCsvUtil.verifyColumn(columnList)) {
                throw new ServiceException("!");
            }
        }
        ColumnRespVO columnRespVO = ColumnRespVO.builder().csvFile(csvFile).columnList(columnList).build();
        List<AssetsAssetColumnDO> AssetsAssetColumnDOS = new ArrayList<>();
        for (String name : columnRespVO.getColumnList()) {
            AssetsAssetColumnDO AssetsAssetColumnDO = new AssetsAssetColumnDO();
            AssetsAssetColumnDO.setColumnName(name);
            AssetsAssetColumnDO.setColumnType("VARCHAR2");
            AssetsAssetColumnDO.setAssetId(assetId);
            AssetsAssetColumnDOS.add(AssetsAssetColumnDO);
        }
        return AssetsAssetColumnDOS;
    }

    private List<AssetsAssetColumnDO> getCsvColumn(String file, Long assetId) {
        file = AniviaConfig.getProfile() + file.replace(Constants.RESOURCE_PREFIX + "/", "");
        file = file.replace("/", File.separator);
        String csvFile = resourceUrl + "csv" + File.separator + UUID.randomUUID().toString().replace("-", "") + ".csv";
        List<String> columnList = ExcelToCsvUtil.parseCsv(file, csvFile);
        if (columnList.size() > 0) {
            if (!ExcelToCsvUtil.verifyColumn(columnList)) {
                throw new ServiceException("!");
            }
        }
        ColumnRespVO columnRespVO = ColumnRespVO.builder().csvFile(csvFile).columnList(columnList).build();
        List<AssetsAssetColumnDO> AssetsAssetColumnDOS = new ArrayList<>();
        for (String name : columnRespVO.getColumnList()) {
            AssetsAssetColumnDO AssetsAssetColumnDO = new AssetsAssetColumnDO();
            AssetsAssetColumnDO.setColumnName(name);
            AssetsAssetColumnDO.setColumnType("VARCHAR2");
            AssetsAssetColumnDO.setAssetId(assetId);
            AssetsAssetColumnDOS.add(AssetsAssetColumnDO);
        }
        return AssetsAssetColumnDOS;
    }

    private void createAssetVideoNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getId() == null) {
            Long assetId = createAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        AssetsAssetVideoSaveReqVO AssetsAssetVideo = AssetsAsset.getAssetsAssetVideo();
        AssetsAssetVideo.setAssetId(AssetsAsset.getId());
        IAssetsAssetVideoService.createAssetVideo(AssetsAssetVideo);
    }

    private void createAssetGeoNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getId() == null) {
            Long assetId = createAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        AssetsAssetGeoSaveReqVO AssetsAssetGeo = AssetsAsset.getAssetsAssetGeo();
        AssetsAssetGeo.setAssetId(AssetsAsset.getId());
        IAssetsAssetGeoService.createAssetGeo(AssetsAssetGeo);
    }

    private void createAssetSpaceRel(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getSpaceId() == null) {
            return;
        }
        AssetsAssetSpaceRelSaveReqVO AssetsAssetSpaceRelSaveReqVO = new AssetsAssetSpaceRelSaveReqVO();
        AssetsAssetSpaceRelSaveReqVO.setSpaceCode(AssetsAsset.getSpaceCode());
        AssetsAssetSpaceRelSaveReqVO.setSpaceId(AssetsAsset.getSpaceId());
        AssetsAssetSpaceRelSaveReqVO.setAssetId(AssetsAsset.getId());
        IAssetsAssetSpaceRelService.createAssetSpaceRel(AssetsAssetSpaceRelSaveReqVO);
    }

    private void setAssetDefaultValues(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAsset.setDatasourceId(-1L);
        AssetsAsset.setTableName("-1");
        AssetsAsset.setDataCount(0L);
        AssetsAsset.setFieldCount(0L);
    }

    /**
     * *     * @param AssetsAsset
     */
    private void createAssetThemeIdList(AssetsAssetSaveReqVO AssetsAsset) {
        List<String> themeIdList = AssetsAsset.getThemeIdList();
        if (CollectionUtils.isEmpty(themeIdList)) {
            return;
        }
        AssetsAssetThemeRelService.createAssetThemeRelList(themeIdList, AssetsAsset.getId());
    }

    /**
     * *     * @param AssetsAsset
     */
    private void createAssetColumnNew(AssetsAssetSaveReqVO AssetsAsset) {
        List<AssetsAssetColumnSaveReqVO> columnSaveReqVOList = AssetsAsset.getAssetColumnList();
        if (CollectionUtils.isEmpty(columnSaveReqVOList)) {
            List<CatalogColumnRespDTO> CatalogColumnRespDTOList = AssetsAsset.getTableId() == null ? Collections.emptyList() : catalogColumnApiService.listByTableId(AssetsAsset.getTableId());
            List<AssetsAssetColumnDO> AssetsAssetColumnDOS = CatalogColumnRespDTOList.stream().map(CatalogColumnRespDTO -> new AssetsAssetColumnDO(CatalogColumnRespDTO)).collect(Collectors.toList());
            columnSaveReqVOList = BeanUtils.toBean(AssetsAssetColumnDOS, AssetsAssetColumnSaveReqVO.class);
        }
        AssetsAsset.setFieldCount(Long.valueOf(columnSaveReqVOList.size()));
        if (AssetsAsset.getId() == null) {
            Long assetId = createAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        String publicSensitiveLevelId = getPublicSensitiveLevelId();
        Map<String, AssetsAssetColumnDO> existingColumnMap = AssetsAssetColumnMapper.findByAssetId(AssetsAsset.getId())
                .stream()
                .filter(column -> StringUtils.isNotEmpty(column.getColumnName()))
                .collect(Collectors.toMap(AssetsAssetColumnDO::getColumnName, column -> column, (existing, replacement) -> existing));
        for (AssetsAssetColumnSaveReqVO columnSaveReqVO : columnSaveReqVOList) {
            columnSaveReqVO.setAssetId(AssetsAsset.getId());
            if (StringUtils.isEmpty(columnSaveReqVO.getSensitiveLevelId())) {
                columnSaveReqVO.setSensitiveLevelId(publicSensitiveLevelId);
            }
            AssetsAssetColumnDO existingColumn = existingColumnMap.get(columnSaveReqVO.getColumnName());
            Long columnId;
            if (existingColumn == null) {
                columnSaveReqVO.setId(null);
                columnId = IAssetsAssetColumnService.createAssetColumn(columnSaveReqVO);
            } else {
                columnSaveReqVO.setId(existingColumn.getId());
                IAssetsAssetColumnService.updateAssetColumn(columnSaveReqVO);
                columnId = existingColumn.getId();
            }
            createAssetColumnSpaceRel(AssetsAsset, columnId);
        }
    }

    private String getPublicSensitiveLevelId() {
        AssetsSensitiveLevelDO publicLevel = AssetsSensitiveLevelMapper.selectOne(
                Wrappers.<AssetsSensitiveLevelDO>lambdaQuery()
                        .like(AssetsSensitiveLevelDO::getSensitiveLevel, "公开")
                        .eq(AssetsSensitiveLevelDO::getOnlineFlag, "1")
                        .last("limit 1")
        );
        return publicLevel == null ? "5" : String.valueOf(publicLevel.getId());
    }

    private void createAssetColumnSpaceRel(AssetsAssetSaveReqVO AssetsAsset, Long columnId) {
        if (AssetsAsset.getSpaceId() == null || columnId == null) {
            return;
        }
        AssetsAssetColumnSpaceRelSaveReqVO rel = new AssetsAssetColumnSpaceRelSaveReqVO();
        rel.setAssetId(AssetsAsset.getId());
        rel.setColumnId(columnId);
        rel.setSpaceId(AssetsAsset.getSpaceId());
        rel.setSpaceCode(AssetsAsset.getSpaceCode());
        assetsAssetColumnSpaceRelService.createAssetColumnSpaceRel(rel);
    }

    @Override
    public int updateAssetNew(AssetsAssetSaveReqVO AssetsAsset) {
//1:Ã¦ÂÂ°Ã¦ÂÂ®Ã¥ÂºÂÃ¨Â¡Â¨  2:Ã¥Â¤ÂÃ©ÂÂ¨API 3: Ã¥ÂÂ°Ã§ÂÂÃ§Â©ÂºÃ©ÂÂ´Ã¦ÂÂÃ¥ÂÂ¡ 4:Ã§ÂÂ¢Ã©ÂÂÃ¦ÂÂ°Ã¦ÂÂ® 5:Ã¨Â§ÂÃ©Â¢ÂÃ¦ÂÂ°Ã¦ÂÂ®
        String type = AssetsAsset.getType();
        if (StringUtils.equals("1", type)) {
            AssetsAssetRespVO AssetsAssetById = getAssetById(AssetsAsset.getId());
            if (StringUtils.equals("1", AssetsAssetById.getCreateType()) && StringUtils.equals("2", AssetsAsset.getCreateType())) {
                createAssetColumnNew(AssetsAsset);
            }
        } else if (StringUtils.equals("4", type)) {
            setAssetDefaultValues(AssetsAsset);
            updateAssetGeoNew(AssetsAsset);
        } else if (StringUtils.equals("5", type)) {
            setAssetDefaultValues(AssetsAsset);
            updateAssetVideoNew(AssetsAsset);
        } else if (StringUtils.equals("6", type)) {
            setAssetDefaultValues(AssetsAsset);
            updateAssetFilesNew(AssetsAsset);
        }
        createAssetSpaceRel(AssetsAsset);
        createAssetThemeIdList(AssetsAsset);
        updateAsset(AssetsAsset);
        return 1;
    }

    private void updateAssetVideoNew(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAssetVideoSaveReqVO AssetsAssetVideo = AssetsAsset.getAssetsAssetVideo();
        if (AssetsAssetVideo == null) {
            return;
        }
        AssetsAssetVideo.setAssetId(AssetsAsset.getId());
        IAssetsAssetVideoService.updateAssetVideo(AssetsAssetVideo);
    }

    private void updateAssetGeoNew(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAssetGeoSaveReqVO AssetsAssetGeo = AssetsAsset.getAssetsAssetGeo();
        if (AssetsAssetGeo == null) {
            return;
        }
        AssetsAssetGeo.setAssetId(AssetsAsset.getId());
        IAssetsAssetGeoService.updateAssetGeo(AssetsAssetGeo);
    }

    private void updateAssetColumnNew(AssetsAssetSaveReqVO AssetsAsset) {
        return;
    }

    private void updateAssetFilesNew(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAssetFilesSaveReqVO AssetsAssetFiles = AssetsAsset.getAssetsAssetFiles();
        int lastDot = AssetsAssetFiles.getUrl().lastIndexOf('.');
        String type = AssetsAssetFiles.getUrl().substring(lastDot);
        AssetsAssetFiles.setType(type);
        AssetsAssetFiles.setAssetId(AssetsAsset.getId());
        AssetsAssetFilesService.updateAssetFiles(AssetsAssetFiles);
        if (StringUtils.equalsIgnoreCase("xls", AssetsAssetFiles.getType()) || StringUtils.equalsIgnoreCase("xlsx", AssetsAssetFiles.getType())) {
            LambdaQueryWrapperX<AssetsAssetColumnDO> queryWrapperX = new LambdaQueryWrapperX<>();
            queryWrapperX.eq(AssetsAssetColumnDO::getAssetId, AssetsAsset.getId());
            IAssetsAssetColumnService.remove(queryWrapperX);
            List<AssetsAssetColumnDO> AssetsAssetColumnDOS = getExcelColumn(AssetsAssetFiles.getUrl(), AssetsAssetFiles.getStartColumn(), AssetsAssetFiles.getStartData(), AssetsAsset.getId());
            IAssetsAssetColumnService.saveBatch(AssetsAssetColumnDOS);
        }
        if (StringUtils.equalsIgnoreCase("csv", AssetsAssetFiles.getType())) {
            LambdaQueryWrapperX<AssetsAssetColumnDO> queryWrapperX = new LambdaQueryWrapperX<>();
            queryWrapperX.eq(AssetsAssetColumnDO::getAssetId, AssetsAsset.getId());
            IAssetsAssetColumnService.remove(queryWrapperX);
            List<AssetsAssetColumnDO> AssetsAssetColumnDOS = getCsvColumn(AssetsAssetFiles.getUrl(), AssetsAsset.getId());
            IAssetsAssetColumnService.saveBatch(AssetsAssetColumnDOS);
        }
    }

    @Override
    public void startAssetDatasourceTaskNull() {
        this.startAssetDatasourceTask(null);
    }

    @Override
    public AjaxResult startAssetDatasourceTask(Long id) {
        if (id != null) {
            AssetsAssetRespVO AssetsAssetById = this.getAssetById(id);
            if (StringUtils.equals("1", AssetsAssetById.getType())) {
// Ã¥Â¦ÂÃ©ÂÂÃ§ÂÂ¹Ã¦Â®ÂÃ¥Â¤ÂÃ§ÂÂÃ¯Â¼ÂÃ¥Â¡Â«Ã¥ÂÂÃ©ÂÂ»Ã¨Â¾Â
            }
            AssetsDatasourceDO AssetsDatasourceById = IAssetsDatasourceService.getDatasourceDOById(AssetsAssetById.getDatasourceId());
            DbQueryProperty dbQueryProperty = new DbQueryProperty(AssetsDatasourceById.getDatasourceType(), AssetsDatasourceById.getIp(), AssetsDatasourceById.getPort(), AssetsDatasourceById.getDatasourceConfig());
            if (!isCountSupported(dbQueryProperty.getDbType())) {
                throw new DataQueryException("");
            }
            DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
            if (!dbQuery.valid()) {
                throw new DataQueryException("");
            }
            updateAssetFieldAndDataCount(dbQuery, dbQueryProperty, AssetsAssetById);
            dbQuery.close();
        } else {
            AssetsAssetPageReqVO AssetsAsset = new AssetsAssetPageReqVO();
            AssetsAsset.setType("1");
            List<AssetsAssetDO> AssetsAssetList = this.getAssetList(AssetsAsset);
            Map<Long, List<AssetsAssetDO>> datasourceGroupMap = AssetsAssetList.stream().collect(Collectors.groupingBy(AssetsAssetDO::getDatasourceId));
            for (Map.Entry<Long, List<AssetsAssetDO>> entry : datasourceGroupMap.entrySet()) {
                Long datasourceId = entry.getKey();
                List<AssetsAssetDO> assets = entry.getValue();
                AssetsDatasourceDO datasource = IAssetsDatasourceService.getDatasourceDOById(datasourceId);
                if (datasource == null) {
                    continue;
                }
                DbQueryProperty dbQueryProperty = new DbQueryProperty(datasource.getDatasourceType(), datasource.getIp(), datasource.getPort(), datasource.getDatasourceConfig());
                if (!isCountSupported(dbQueryProperty.getDbType())) {
                    continue;
                }
                DbQuery dbQuery = DataSourceFactory.createDbQuery(dbQueryProperty);
                try {
                    if (!dbQuery.valid()) {
// Ã¨Â®Â°Ã¥Â½ÂÃ¦ÂÂ¥Ã¥Â¿ÂÃ¥Â¹Â¶Ã¨Â·Â³Ã¨Â¿ÂÃ¨Â¯Â¥Ã¦ÂÂ°Ã¦ÂÂ®Ã¦ÂºÂ
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
                for (AssetsAssetDO asset : assets) {
                    try {
                        updateAssetFieldAndDataCount(dbQuery, dbQueryProperty, asset);
                    } catch (Exception e) {
                        log.error("{} ", asset);
                    }
                }
                dbQuery.close();
            }
        }
        return AjaxResult.success("");
    }

    @Override
    public PageResult<AssetsAssetDO> getAssetByIds(List<Long> ids) {
        AssetsAssetPageReqVO AssetsAssetPageReqVO = new AssetsAssetPageReqVO();
        AssetsAssetPageReqVO.setAssetIdList(ids);
        return AssetsAssetMapper.selectPage(AssetsAssetPageReqVO);
    }

    @Override
    public List<AssetsAssetColumnRelRuleVO> listRelRule(Long id, String type) {
        List<AssetsAssetColumnDO> assetColumns = AssetsAssetColumnMapper.findByAssetId(id);
        if (assetColumns.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> columnIds = assetColumns.stream().map(AssetsAssetColumnDO::getId).collect(Collectors.toSet());
        List<StandardsDataElemAssetRelRespDTO> assetRelRespDTOS = iStandardsModelApiService.getDpDataElemListByColumnIdInApi(columnIds);
        if (assetRelRespDTOS.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> dataElemIds = assetRelRespDTOS.stream().map(StandardsDataElemAssetRelRespDTO::getDataElemId).collect(Collectors.toSet());
        List<StandardsDataElemRuleRelRespDTO> ruleRelRespDTOS = elemRuleRelService.listByDataElemIdList(dataElemIds, type);
        if (ruleRelRespDTOS.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<Long>> map = assetRelRespDTOS.stream().filter(i -> i.getColumnId() != null).collect(Collectors.groupingBy(StandardsDataElemAssetRelRespDTO::getColumnId, Collectors.mapping(StandardsDataElemAssetRelRespDTO::getDataElemId, Collectors.toList())));
        return assetColumns.stream().filter(assetColumn -> CollectionUtils.isNotEmpty(map.get(assetColumn.getId()))).map(assetColumn -> {
            List<Long> temp = map.get(assetColumn.getId());
            return ruleRelRespDTOS.stream().filter(i -> temp.contains(i.getDataElemId())).map(i -> new AssetsAssetColumnRelRuleVO(assetColumn, i)).collect(Collectors.toList());
        }).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public List<AssetsAssetColumnRelRuleVO> listRelRule(Long datasourceId, String tableName, String type) {
        List<AssetsAssetDO> AssetsAssetDOS = AssetsAssetMapper.findByDatasourceIdAndTableName(datasourceId, tableName);
        if (AssetsAssetDOS.isEmpty()) {
            return Collections.emptyList();
        }
        return AssetsAssetDOS.stream().map(i -> listRelRule(i.getId(), type)).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public LineageDTO dataLineage(Long id) {
//Ã¨ÂÂ·Ã¥ÂÂÃ¨ÂµÂÃ¤ÂºÂ§Ã¤Â¿Â¡Ã¦ÂÂ¯
        AssetsAssetDO AssetsAsset = this.getById(id);
        if (!StringUtils.equals("1", AssetsAsset.getType())) {
            throw new ServiceException("");
        }
        Long datasourceId = AssetsAsset.getDatasourceId();
//Ã¨ÂÂ·Ã¥ÂÂÃ¦ÂÂ°Ã¦ÂÂ®Ã¦ÂºÂÃ¨Â¿ÂÃ¦ÂÂ¥Ã¤Â¿Â¡Ã¦ÂÂ¯
        AssetsDatasourceDO datasource = IAssetsDatasourceService.getById(datasourceId);
        if (datasource == null) {
            throw new ServiceException("");
        }
        //血缘开关未开启（LINEAGE_ENABLED=false 或未安装 Neo4j）时不装配 LineageDataService，
        //直接返回空血缘，不影响资产详情展示
        if (lineageDataService == null) {
            return new LineageDTO();
        }
        DbQueryProperty dbProperty = new DbQueryProperty(datasource.getDatasourceType(), datasource.getIp(), datasource.getPort(), datasource.getDatasourceConfig());
        DbDialect dbDialect = DialectFactory.getDialect(DbType.getDbType(dbProperty.getDbType()));
        String tableName = dbDialect.getTableName(dbProperty, AssetsAsset.getTableName());
        LineageDTO lineageDTO = lineageDataService.lineage(dbProperty.trainToHostPort(), tableName);
//Ã¦Â Â¹Ã¦ÂÂ®taskÃ¦ÂÂ¥Ã¨Â¯Â¢Ã¥Â½ÂÃ¥ÂÂÃ¤Â»Â»Ã¥ÂÂ¡Ã¦ÂÂÃ¦ÂÂ°Ã§ÂÂÃ§ÂÂ¶Ã¦ÂÂ
        if (lineageDTO.getTasks() != null && lineageDTO.getTasks().size() > 0) {
            List<Long> ipList = lineageDTO.getTasks().stream().map(TaskNode::getTaskId).collect(Collectors.toList());
            Map<Long, TaskNode> taskNodeMap = lineageDTO.getTasks().stream().collect(Collectors.toMap(k -> k.getTaskId(), v -> v));
            collectorEtlTaskInstanceService.getLastTaskInstance(ipList).forEach(taskInstance -> {
                TaskNode taskNode = taskNodeMap.get(taskInstance.getTaskId());
                if (taskNode != null) {
                    taskNode.setTaskStatus(taskInstance.getStatus());
                    taskNode.setTaskTime(taskInstance.getStartTime());
                }
            });
        }
        return lineageDTO;
    }

    @Override
    public List<AssetsAssetDO> getAssetListAll(AssetsAssetPageReqVO AssetsAsset, String number) {
        List<AssetsAssetDO> AssetsAssetDOPageResult = AssetsAssetMapper.selectList();
        return AssetsAssetDOPageResult;
    }

    @Override
    public List<TreeData> getTreeData() {
        List<TreeData> treeData = new ArrayList<>();
        treeData.add(TreeData.builder().name("").type("0").otherData(JSON.parseObject("{\"tooltipStr\":\"\"}")).children(modelingBusinessCategoryApiService.getTreeData("1")).build());
        treeData.add(TreeData.builder().name("").type("0").otherData(JSON.parseObject("{\"tooltipStr\":\"\"}")).children(modelingThemeDomainApiService.getTreeData("1")).build());
        treeData.add(TreeData.builder().name("").type("0").otherData(JSON.parseObject("{\"tooltipStr\":\"\"}")).children(modelingDataLayerApiService.getTreeData("1")).build());
        return treeData;
    }

    @Override
    public List<Long> createAssetBatchNew(List<AssetsAssetSaveReqVO> AssetsAssetList) {
        if (AssetsAssetList == null || AssetsAssetList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>(AssetsAssetList.size());
        if (this.count(Wrappers.lambdaQuery(AssetsAssetDO.class).in(AssetsAssetDO::getTableId, AssetsAssetList.stream().map(e -> e.getTableId()).collect(Collectors.toList()))) > 0) {
            throw new ServiceException("");
        }
        for (AssetsAssetSaveReqVO vo : AssetsAssetList) {
            Long id = this.createAssetNew(vo);
            ids.add(id);
        }
        return ids;
    }

    @Override
    public List<Map<String, Object>> dataMaskings(Long assetId, List<Map<String, Object>> Data, Long userId, String scene, Long userPermissionLevel) {
        return assetsTableGovernanceApiService.desensitizeResultData(assetId, Data, userId, userPermissionLevel, scene);
    }

    @Override
    public List<AssetsAssetDO> getAssetByDataSourceId(Long DataSourceId, String tableName) {
        return this.list(Wrappers.lambdaQuery(AssetsAssetDO.class).eq(AssetsAssetDO::getDatasourceId, DataSourceId).eq(AssetsAssetDO::getTableName, tableName));
    }


    private void updateAssetFieldAndDataCount(DbQuery dbQuery, DbQueryProperty dbQueryProperty, AssetsAssetDO assetDO) {
        List<DbColumn> tableColumns = dbQuery.getTableColumns(dbQueryProperty, assetDO.getTableName());
        int tableColumnsSize = CollectionUtils.isEmpty(tableColumns) ? 0 : tableColumns.size();
        int dataCount = dbQuery.countNew(assetDO.getTableName(), new HashMap<>());
        AssetsAssetSaveReqVO updateObj = BeanUtils.toBean(assetDO, AssetsAssetSaveReqVO.class);
        updateObj.setFieldCount((long) tableColumnsSize);
        updateObj.setDataCount((long) dataCount);
        this.updateAsset(updateObj);
    }

    private void updateAssetFieldAndDataCount(DbQuery dbQuery, DbQueryProperty dbQueryProperty, AssetsAssetRespVO assetVO) {
        AssetsAssetDO assetDO = BeanUtils.toBean(assetVO, AssetsAssetDO.class);
        updateAssetFieldAndDataCount(dbQuery, dbQueryProperty, assetDO);
    }

    private boolean isCountSupported(String datasourceType) {
        return StringUtils.isNotBlank(datasourceType) && COUNT_SUPPORTED_TYPES.contains(datasourceType);
    }

    private static final Set<String> COUNT_SUPPORTED_TYPES = new HashSet<>(Arrays.asList(DbType.MYSQL.getDb(), DbType.ORACLE.getDb(), DbType.ORACLE_12C.getDb(), DbType.SQL_SERVER.getDb(), DbType.POSTGRE_SQL.getDb(), DbType.DM8.getDb(), DbType.KINGBASE8.getDb(), DbType.DORIS.getDb(), DbType.HIVE.getDb()));

    @Override
    public Map<String, Object> getAssetOverviewStatistics() {
        Map<String, Object> map = AssetsAssetMapper.getAssetOverviewStatistics();
        int diffCount = MapUtils.getIntValue(map, "diffCount");
        int prevCount = MapUtils.getIntValue(map, "prevCount");
        BigDecimal growthRate = BigDecimal.ZERO;
        if (prevCount > 0) {
            growthRate = BigDecimal.valueOf(diffCount).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(prevCount), 2, RoundingMode.HALF_UP);
        } else if (diffCount != 0) {
            growthRate = BigDecimal.valueOf(diffCount);
        }
        map.put("growthRate", growthRate);
        return map;
    }

    @Override
    public int updateCatCode(String oldCatCode, String newCatCode) {
        return AssetsAssetMapper.updateCatCode(oldCatCode, newCatCode);
    }

    @Override
    public List<Long> getCatalogTableInAsset(List<Long> catalogTableIds) {
        List<AssetsAssetDO> AssetsAssetDOList = this.list(Wrappers.lambdaQuery(AssetsAssetDO.class).select(AssetsAssetDO::getTableId).in(AssetsAssetDO::getTableId, catalogTableIds));
        if (AssetsAssetDOList != null || AssetsAssetDOList.size() > 0) {
            return AssetsAssetDOList.stream().map(AssetsAssetDO::getTableId).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public boolean existsByTableId(Long tableId) {
        if (tableId == null) {
            return false;
        }
        Long count = baseMapper.selectCount(Wrappers.lambdaQuery(AssetsAssetDO.class).eq(AssetsAssetDO::getTableId, tableId).eq(AssetsAssetDO::getDelFlag, "0"));
        return count != null && count > 0;
    }
}
