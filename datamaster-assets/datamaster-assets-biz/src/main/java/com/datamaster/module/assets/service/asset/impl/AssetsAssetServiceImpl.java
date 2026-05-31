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
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.file.FileDataReaderUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.service.cat.tagRel.ITaxonomyTagAssetRelApiService;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetReqDTO;
import com.datamaster.module.assets.api.asset.dto.AssetsAssetRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsAssetApiOutService;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetPageReqVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetRespVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnRelRuleVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoSaveReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTableSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.dataobject.assetchild.file.AssetsAssetFileDO;
import com.datamaster.module.assets.dal.dataobject.assetchild.files.AssetsAssetFilesDO;
import com.datamaster.module.assets.dal.dataobject.daAssetApply.AssetsAssetApplyDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskDO;
import com.datamaster.module.assets.dal.dataobject.sensitiveLevel.AssetsSensitiveLevelDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.assetColumn.AssetsAssetColumnMapper;
import com.datamaster.module.assets.dal.mapper.assetchild.file.AssetsAssetFileMapper;
import com.datamaster.module.assets.dal.mapper.daAssetApply.AssetsAssetApplyMapper;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceMapper;
import com.datamaster.module.assets.dal.mapper.sensitiveLevel.AssetsSensitiveLevelMapper;
import com.datamaster.module.assets.service.asset.IAssetsAssetService;
import com.datamaster.module.assets.service.assetColumn.IAssetsAssetColumnService;
import com.datamaster.module.assets.service.assetchild.api.IAssetsAssetApiParamService;
import com.datamaster.module.assets.service.assetchild.api.IAssetsAssetApiService;
import com.datamaster.module.assets.service.assetchild.files.IAssetsAssetFilesService;
import com.datamaster.module.assets.service.assetchild.geo.IAssetsAssetGeoService;
import com.datamaster.module.assets.service.assetchild.gis.IAssetsAssetGisService;
import com.datamaster.module.assets.service.assetchild.projectRel.IAssetsAssetProjectRelService;
import com.datamaster.module.assets.service.assetchild.theme.IAssetsAssetThemeRelService;
import com.datamaster.module.assets.service.assetchild.video.IAssetsAssetVideoService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryTableService;
import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryTaskService;
import com.datamaster.module.standards.dal.dataobject.desensitizeList.StandardsDesensitizeAssetcolumnDO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeRuleDO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeWhitelistDO;
import com.datamaster.module.standards.service.desensitizeList.IStandardsDesensitizeAssetcolumnService;
import com.datamaster.module.standards.service.desensitizeRules.IStandardsDesensitizeRuleService;
import com.datamaster.module.standards.service.whitelist.IStandardsDesensitizeWhitelistService;
import com.datamaster.module.modeling.api.service.businessCategory.IModelingBusinessCategoryApiService;
import com.datamaster.module.modeling.api.service.dataLayer.IModelingDataLayerApiService;
import com.datamaster.module.modeling.api.service.themeDomain.IModelingThemeDomainApiService;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemAssetRelReqDTO;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemAssetRelRespDTO;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemRespDTO;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemRuleRelRespDTO;
import com.datamaster.module.standards.api.model.dto.StandardsModelColumnRespDTO;
import com.datamaster.module.standards.api.model.dto.StandardsModelRespDTO;
import com.datamaster.module.standards.api.service.dataElem.IDataElemRuleRelService;
import com.datamaster.module.standards.api.service.model.IStandardsModelApiService;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskInstanceService;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskService;
import com.datamaster.module.catalog.api.column.dto.CatalogColumnRespDTO;
import com.datamaster.module.catalog.api.service.column.CatalogColumnApiService;
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
    private IAssetsDiscoveryTaskService AssetsDiscoveryTaskService;
    @Resource
    private RedisCache redisCache;
    @Resource
    private AssetsAssetApplyMapper AssetsAssetApplyMapper;
    @Resource
    private IAssetsDiscoveryTableService AssetsDiscoveryTableService;
    @Resource
    private CollectorEtlTaskService dppEtlTaskService;
    @Resource
    private IAssetsAssetThemeRelService AssetsAssetThemeRelService;
    @Resource
    private IAssetsAssetApiService IAssetsAssetApiService;
    @Resource
    private IAssetsAssetApiParamService IAssetsAssetApiParamService;
    @Resource
    private IAssetsAssetProjectRelService IAssetsAssetProjectRelService;
    @Resource
    private IAssetsAssetGeoService IAssetsAssetGeoService;
    @Resource
    private IAssetsAssetGisService IAssetsAssetGisService;
    @Resource
    private IAssetsAssetVideoService IAssetsAssetVideoService;
    @Resource
    private IAssetsAssetFilesService AssetsAssetFilesService;
    @Resource
    private ITaxonomyTagAssetRelApiService attTagAssetRelApiService;
    @Resource
    private LineageDataService lineageDataService;
    @Resource
    private CollectorEtlTaskInstanceService dppEtlTaskInstanceService;
    @Resource
    private IModelingThemeDomainApiService dmThemeDomainApiService;
    @Resource
    private IModelingBusinessCategoryApiService dmBusinessCategoryApiService;
    @Resource
    private IModelingDataLayerApiService dmDataLayerApiService;
    @Resource
    private CatalogColumnApiService mcColumnApiService;

    @Resource
    private IStandardsDesensitizeAssetcolumnService dgDesensitizeAssetcolumnService;
    @Resource
    private IStandardsDesensitizeRuleService dgDesensitizeRuleService;
    @Resource
    private IStandardsDesensitizeWhitelistService whitelistService;
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(".xlsx", ".xls", ".csv");

    /**
     * @param AssetsAssetReqDTO * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetsAssetRespDTO insertDaAsset(AssetsAssetReqDTO AssetsAssetReqDTO) {
        StandardsModelRespDTO dpModelByIdApi = iStandardsModelApiService.getDpModelByIdApi(AssetsAssetReqDTO.getModelId());
        if (dpModelByIdApi == null) {
            throw new ServiceException("");
        }
        AssetsAssetDO AssetsAssetDO = new AssetsAssetDO();
        AssetsAssetDO.setName(dpModelByIdApi.getModelComment());
        AssetsAssetDO.setCatCode(dpModelByIdApi.getCatCode());
        AssetsAssetDO.setDatasourceId(AssetsAssetReqDTO.getDatasourceId());
        AssetsAssetDO.setSource(AssetsAssetReqDTO.getSource());
        AssetsAssetDO.setTableName(dpModelByIdApi.getTableName());
        AssetsAssetDO.setTableComment(dpModelByIdApi.getModelComment());
        AssetsAssetDO.setFieldCount(AssetsAssetReqDTO.getFieldCount());
        AssetsAssetDO.setTableType(dpModelByIdApi.getTableType());
        AssetsAssetDO.setDataLayerId(dpModelByIdApi.getDataLayerId());
        AssetsAssetDO.setBusinessCategoryId(dpModelByIdApi.getBusinessCategoryId());
        AssetsAssetDO.setBusinessCategoryCode(dpModelByIdApi.getBusinessCategoryCode());
        AssetsAssetDO.setDataDomainId(dpModelByIdApi.getDataDomainId());
        AssetsAssetDO.setThemeDomainId(dpModelByIdApi.getThemeDomainId());
        AssetsAssetDO.setThemeDomainCode(dpModelByIdApi.getThemeDomainCode());
        AssetsAssetDO.setTableCase(dpModelByIdApi.getTableCase());
        AssetsAssetPageReqVO AssetsAssetPageReqVO = new AssetsAssetPageReqVO();
        AssetsAssetPageReqVO.setTableName(dpModelByIdApi.getTableName());
        AssetsAssetPageReqVO.setDatasourceId(String.valueOf(AssetsAssetReqDTO.getDatasourceId()));
        AssetsAssetDO assetDO = this.getDaAssetByDaAssetPageReqVO(AssetsAssetPageReqVO);
        if (assetDO != null) {
            AssetsAssetDO.setId(assetDO.getId());
            AssetsAssetMapper.updateById(AssetsAssetDO);
            redisCache.deleteObject(CacheConstants.ASSET_PREVIEW_KEY + AssetsAssetReqDTO.getId() + "_" + dpModelByIdApi.getTableName());
        } else {
            AssetsAssetMapper.insert(AssetsAssetDO);
        }
        List<StandardsModelColumnRespDTO> dpModelColumnListByModelIdApi = iStandardsModelApiService.getDpModelColumnListByModelIdApi(AssetsAssetReqDTO.getModelId());
        List<AssetsAssetColumnDO> AssetsAssetColumnDOList = new ArrayList<>();
        List<AssetsAssetColumnDO> AssetsAssetColumnList = new ArrayList<>();
        if (assetDO != null) {
            AssetsAssetColumnPageReqVO AssetsAssetColumnPageReqVO = new AssetsAssetColumnPageReqVO();
            AssetsAssetColumnPageReqVO.setAssetId(String.valueOf(assetDO.getId()));
            List<AssetsAssetColumnDO> AssetsAssetColumnList1 = IAssetsAssetColumnService.getDaAssetColumnList(AssetsAssetColumnPageReqVO);
            AssetsAssetColumnList = CollectionUtils.isEmpty(AssetsAssetColumnList1) ? AssetsAssetColumnList : AssetsAssetColumnList1;
        }
        if (StringUtils.isNotEmpty(dpModelColumnListByModelIdApi)) {
            for (StandardsModelColumnRespDTO dpModelColumnRespDTO : dpModelColumnListByModelIdApi) {
                AssetsAssetColumnDO AssetsAssetColumnDO = new AssetsAssetColumnDO();
                AssetsAssetColumnDO columnDO = matchColumn(AssetsAssetColumnList, dpModelColumnRespDTO);
                if (columnDO != null) {
                    AssetsAssetColumnDO.setId(columnDO.getId());
                }
                AssetsAssetColumnDO.setAssetId(AssetsAssetDO.getId());
                AssetsAssetColumnDO.setDataElemCodeId(dpModelColumnRespDTO.getDataElemId());
                AssetsAssetColumnDO.setColumnName(dpModelColumnRespDTO.getEngName());
                AssetsAssetColumnDO.setColumnLength(dpModelColumnRespDTO.getColumnLength());
                AssetsAssetColumnDO.setColumnScale(dpModelColumnRespDTO.getColumnScale());
                AssetsAssetColumnDO.setColumnType(dpModelColumnRespDTO.getColumnType());
                AssetsAssetColumnDO.setColumnComment(dpModelColumnRespDTO.getCnName());
                AssetsAssetColumnDO.setDefaultValue(dpModelColumnRespDTO.getDefaultValue());
                AssetsAssetColumnDO.setNullableFlag(dpModelColumnRespDTO.getNullableFlag());
                AssetsAssetColumnDO.setPkFlag(dpModelColumnRespDTO.getPkFlag());
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
            IAssetsAssetColumnService.removeDaAssetColumn(nonExistingIdList);
        }
//Ã¨Â®Â¾Ã§Â½Â®Ã¦ÂÂ°Ã¦ÂÂ®Ã¥ÂÂÃ¦ÂÂ°Ã¦ÂÂ®Ã¨ÂµÂÃ¤ÂºÂ§Ã¥ÂÂ³Ã¨ÂÂÃ¤Â¿Â¡Ã¦ÂÂ¯
        Set<Long> ids = dpModelColumnListByModelIdApi.stream().map(StandardsModelColumnRespDTO::getDataElemId).collect(Collectors.toSet());
//idÃ¦ÂÂ°Ã¦ÂÂ®Ã¤Â¸ÂÃ¤Â¸ÂºÃ§Â©Âº
        if (StringUtils.isNotEmpty(ids)) {
            List<StandardsDataElemRespDTO> dpDataElemListByIdsApi = iStandardsModelApiService.getDpDataElemListByIdsApi(ids);
            List<StandardsDataElemAssetRelReqDTO> dpDataElemAssetRel = new ArrayList<>();
            dpDataElemListByIdsApi.forEach(dpDataElemRespDTO -> {
                StandardsDataElemAssetRelReqDTO dpDataElemAssetRelReqDTO = new StandardsDataElemAssetRelReqDTO();
//Ã¨Â®Â¾Ã§Â½Â®Ã¨ÂµÂÃ¤ÂºÂ§id
                dpDataElemAssetRelReqDTO.setAssetId(AssetsAssetDO.getId());
                dpDataElemAssetRelReqDTO.setDataElemType(dpDataElemRespDTO.getType());
                dpDataElemAssetRelReqDTO.setTableName(dpModelByIdApi.getModelName());
                dpDataElemAssetRelReqDTO.setColumnName(dpDataElemRespDTO.getEngName());
                dpDataElemAssetRelReqDTO.setDataElemId(dpDataElemRespDTO.getId());
                Optional<AssetsAssetColumnDO> first = AssetsAssetColumnDOList.stream().filter(AssetsAssetColumnDO -> AssetsAssetColumnDO.getDataElemCodeId() != null && AssetsAssetColumnDO.getDataElemCodeId().equals(dpDataElemRespDTO.getId())).findFirst();
                first.ifPresent(AssetsAssetColumnDO -> dpDataElemAssetRelReqDTO.setColumnId(AssetsAssetColumnDO.getId()));
                dpDataElemAssetRel.add(dpDataElemAssetRelReqDTO);
            });
            if (StringUtils.isNotEmpty(dpDataElemListByIdsApi)) {
                boolean b = iStandardsModelApiService.insertElementAssetRelation(dpDataElemAssetRel);
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
     * dpModelColumnRespDTO  engName  AssetsAssetColumnList  AssetsAssetColumnDO      *     * @param AssetsAssetColumnList         * @param dpModelColumnRespDTO  DTO engName      * @return  AssetsAssetColumnDO  null
     */
    public static AssetsAssetColumnDO matchColumn(List<AssetsAssetColumnDO> AssetsAssetColumnList, StandardsModelColumnRespDTO dpModelColumnRespDTO) {
        if (AssetsAssetColumnList == null || dpModelColumnRespDTO == null || dpModelColumnRespDTO.getEngName() == null) {
            return null;
        }
        for (AssetsAssetColumnDO AssetsAssetColumnDO : AssetsAssetColumnList) {
// Ã¥Â½ÂÃ¥Â­ÂÃ¦Â®ÂµÃ¥ÂÂÃ§Â§Â°Ã¥ÂÂ¹Ã©ÂÂÃ¦ÂÂ¶Ã¯Â¼ÂÃ¨Â¿ÂÃ¥ÂÂÃ¨Â¯Â¥Ã¥Â¯Â¹Ã¨Â±Â¡
            if (dpModelColumnRespDTO.getEngName().equals(AssetsAssetColumnDO.getColumnName())) {
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
        return BeanUtils.toBean(this.getDaAssetPage(AssetsAssetPageReqVO, "1"), AssetsAssetRespDTO.class);
    }

    @Override
    public List<AssetsAssetDO> getTablesByDataSourceId(AssetsAssetPageReqVO pageReqVO) {
        if (StringUtils.isEmpty(pageReqVO.getDatasourceId())) {
            throw new ServiceException("id");
        }
        return this.lambdaQuery().eq(AssetsAssetDO::getDatasourceId, pageReqVO.getDatasourceId()).eq(AssetsAssetDO::getDelFlag, "0").list();
    }

    @Override
    public AssetsAssetDO getDaAssetByDaAssetPageReqVO(AssetsAssetPageReqVO pageReqVO) {
        MPJLambdaWrapper<AssetsAssetDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.eq(StringUtils.isNotEmpty(pageReqVO.getName()), AssetsAssetDO::getName, pageReqVO.getName()).eq(pageReqVO.getId() != null, AssetsAssetDO::getId, pageReqVO.getId()).eq(StringUtils.isNotEmpty(pageReqVO.getTableName()), AssetsAssetDO::getTableName, pageReqVO.getTableName()).eq(StringUtils.isNotEmpty(pageReqVO.getDatasourceId()), AssetsAssetDO::getDatasourceId, pageReqVO.getDatasourceId()).eq(StringUtils.isNotEmpty(pageReqVO.getTableComment()), AssetsAssetDO::getTableComment, pageReqVO.getTableComment());
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
    public PageResult<AssetsAssetDO> getDaAssetPage(AssetsAssetPageReqVO pageReqVO, String AssetsAssetQueryType) {
        PageResult<AssetsAssetDO> AssetsAssetDOPageResult = AssetsAssetMapper.selectPage(pageReqVO);
        List<AssetsAssetDO> AssetsAssetDOList = (List<AssetsAssetDO>) AssetsAssetDOPageResult.getRows();
        for (AssetsAssetDO AssetsAssetDO : AssetsAssetDOList) {
//Ã¥ÂÂ¤Ã¦ÂÂ­Ã¦ÂÂ¯Ã¥ÂÂ¦Ã¦ÂÂ¯api
            if (StringUtils.equals("2", AssetsAssetDO.getType())) {
                AssetsAssetApiRespVO AssetsAssetApiByAssetId = IAssetsAssetApiService.getDaAssetApiByAssetId(AssetsAssetDO.getId());
                AssetsAssetDO.setAssetsAssetApi(AssetsAssetApiByAssetId);
            }
//
//Ã¥ÂÂ¤Ã¦ÂÂ­Ã¦ÂÂ¯Ã¥ÂÂ¦Ã¦ÂÂ¯Ã¦ÂÂ°Ã¦ÂÂ®Ã¦ÂºÂ
//
            if (StringUtils.equals("1", AssetsAssetDO.getType())) {
//
                AssetsDatasourceDO AssetsDatasourceById = IAssetsDatasourceService.getDaDatasourceById(AssetsAssetDO.getDatasourceId());
//
                AssetsDatasourceById = AssetsDatasourceById == null ? new AssetsDatasourceDO() : AssetsDatasourceById;
//
//
                AssetsAssetDO.setDatasourceName(AssetsDatasourceById.getDatasourceName());
//
                AssetsAssetDO.setDatasourceType(AssetsDatasourceById.getDatasourceType());
//
            }
        }
        AssetsAssetDOPageResult.setRows(AssetsAssetDOList);
        return AssetsAssetDOPageResult;
    }

    @Override
    public List<AssetsAssetDO> getDaAssetList(AssetsAssetPageReqVO reqVO) {
        MPJLambdaWrapper<AssetsAssetDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsAssetDO.class).select("t2.NAME AS catName").select("t3.PROJECT_ID AS projectId,t3.PROJECT_CODE AS projectCode").leftJoin("TAX_ASSET_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'").leftJoin("AST_ASSET_PROJECT_REL t3 on t.id = t3.ASSET_ID AND t3.DEL_FLAG = '0'").likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), AssetsAssetDO::getCatCode, reqVO.getCatCode()).like(StringUtils.isNotBlank(reqVO.getName()), AssetsAssetDO::getName, reqVO.getName()).eq(StringUtils.isNotBlank(reqVO.getDatasourceId()), AssetsAssetDO::getDatasourceId, reqVO.getDatasourceId()).like(StringUtils.isNotBlank(reqVO.getTableName()), AssetsAssetDO::getTableName, reqVO.getTableName()).eq(StringUtils.isNotBlank(reqVO.getTableComment()), AssetsAssetDO::getTableComment, reqVO.getTableComment()).eq(StringUtils.isNotBlank(reqVO.getStatus()), AssetsAssetDO::getStatus, reqVO.getStatus()).eq(StringUtils.isNotBlank(reqVO.getType()), AssetsAssetDO::getType, reqVO.getType()).eq(StringUtils.isNotBlank(reqVO.getDescription()), AssetsAssetDO::getDescription, reqVO.getDescription()).in(reqVO.getThemeAssetIdList() != null && !reqVO.getThemeAssetIdList().isEmpty(), AssetsAssetDO::getId, reqVO.getThemeAssetIdList()).orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return AssetsAssetMapper.selectJoinList(AssetsAssetDO.class, lambdaWrapper);
    }

    @Override
    public AssetsAssetRespVO getDaAssetById(Long id) {
        MPJLambdaWrapper<AssetsAssetDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsAssetDO.class).select("t2.NAME AS catName", "dd.DATASOURCE_NAME as datasourceName", "dd.IP as datasourceIp", "dd.DATASOURCE_TYPE as datasourceType", "t3.NAME AS dataLayerName", "t3.ENG_NAME AS dataLayerEngName", "t4.NAME AS businessCategoryName", "t4.ENG_NAME AS businessCategoryEngName", "t5.NAME AS dataDomainName", "t5.ENG_NAME AS dataDomainEngName", "t6.NAME AS themeDomainName", "t6.ENG_NAME AS themeDomainEngName", "u.PHONENUMBER AS createUserPhoneNumber", "u2.PHONENUMBER AS updateUserPhoneNumber").leftJoin("SYSTEM_USER u on t.CREATOR_ID = u.USER_ID AND u.DEL_FLAG = '0'").leftJoin("SYSTEM_USER u2 on t.UPDATER_ID = u2.USER_ID AND u2.DEL_FLAG = '0'").leftJoin("TAX_ASSET_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'").leftJoin("AST_DATASOURCE dd on t.DATASOURCE_ID = dd.ID").leftJoin("MDL_DATA_LAYER t3 ON t.DATA_LAYER_ID = t3.id AND t3.DEL_FLAG = '0'").leftJoin("MDL_BUSINESS_CATEGORY t4 ON t.BUSINESS_CATEGORY_ID = t4.id AND t4.DEL_FLAG = '0'").leftJoin("MDL_DATA_DOMAIN t5 ON t.DATA_DOMAIN_ID = t5.id AND t5.DEL_FLAG = '0'").leftJoin("MDL_THEME_DOMAIN t6 ON t.THEME_DOMAIN_ID = t6.id AND t6.DEL_FLAG = '0'").eq(AssetsAssetDO::getId, id);
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
        List<AssetsAssetThemeRelRespVO> AssetsAssetThemeRelList = AssetsAssetThemeRelService.getDaAssetThemeRelList(AssetsAssetThemeRelPageReqVO);
        AssetsAssetDO.setAssetsAssetThemeRelList(AssetsAssetThemeRelList);
        AssetsAssetRespVO bean = BeanUtils.toBean(AssetsAssetDO, AssetsAssetRespVO.class);
        queryDaAssetchild(bean);
        if (StringUtils.isNotBlank(bean.getTags())) {
            JSONArray tags = JSONArray.parse(bean.getTags());
            bean.setTagIds(tags.stream().map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("tagId")).collect(Collectors.toList()));
            bean.setTagNames(tags.stream().map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("tagName")).collect(Collectors.toList()));
        }
        return bean;
    }

    @Override
    public AssetsAssetRespVO getDaAssetByIdSimple(Long id) {
        return BeanUtils.toBean(AssetsAssetMapper.selectById(id), AssetsAssetRespVO.class);
    }

    private void queryDaAssetchild(AssetsAssetRespVO AssetsAsset) {
        Long assetId = AssetsAsset.getId();
        String type = AssetsAsset.getType();
        if (StringUtils.equals("1", type)) {
            return;
        } else if (StringUtils.equals("2", type)) {
            AssetsAssetApiRespVO AssetsAssetApiByAssetId = IAssetsAssetApiService.getDaAssetApiByAssetId(assetId);
            AssetsAsset.setAssetsAssetApi(AssetsAssetApiByAssetId);
            if (AssetsAssetApiByAssetId == null) {
                AssetsAsset.setAssetsAssetApiParamList(new ArrayList<>());
                return;
            }
            List<AssetsAssetApiParamRespVO> AssetsAssetApiParamList = IAssetsAssetApiParamService.getDaAssetApiParamList(AssetsAssetApiByAssetId.getId());
            AssetsAsset.setAssetsAssetApiParamList(AssetsAssetApiParamList);
        } else if (StringUtils.equals("3", type)) {
            AssetsAssetGisRespVO AssetsAssetGisByAssetId = IAssetsAssetGisService.getDaAssetGisByAssetId(assetId);
            AssetsAsset.setAssetsAssetGis(AssetsAssetGisByAssetId);
        } else if (StringUtils.equals("4", type)) {
            AssetsAssetGeoRespVO AssetsAssetGeoByAssetId = IAssetsAssetGeoService.getDaAssetGeoByAssetId(assetId);
            AssetsAsset.setAssetsAssetGeo(AssetsAssetGeoByAssetId);
        } else if (StringUtils.equals("5", type)) {
            AssetsAssetVideoRespVO AssetsAssetVideoByAssetId = IAssetsAssetVideoService.getDaAssetVideoByAssetId(assetId);
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
    public Long createDaAsset(AssetsAssetSaveReqVO createReqVO) {
        AssetsAssetDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetDO.class);
        AssetsAssetMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDaAsset(AssetsAssetSaveReqVO updateReqVO) {
// Ã§ÂÂ¸Ã¥ÂÂ³Ã¦Â Â¡Ã©ÂªÂ
// Ã¦ÂÂ´Ã¦ÂÂ°Ã¦ÂÂ°Ã¦ÂÂ®Ã¨ÂµÂÃ¤ÂºÂ§
        AssetsAssetDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetDO.class);
        return AssetsAssetMapper.updateById(updateObj);
    }

    @Override
    public int removeDaAsset(Collection<Long> idList) {
        ArrayList<Long> assetIdList = new ArrayList<>(idList);
        int asset = dppEtlTaskService.checkTaskIdInAsset(assetIdList);
        if (asset > 0) {
            throw new ServiceException(",!");
        }
        List<AssetsAssetDO> AssetsAssetDOList = AssetsAssetMapper.selectList("ID", idList);
        AssetsAssetDO AssetsAssetDO = AssetsAssetDOList != null ? AssetsAssetDOList.get(0) : null;
        if ("1".equals(AssetsAssetDO.getSource())) {
            LambdaQueryWrapperX<AssetsDiscoveryTaskDO> queryWrapperX = new LambdaQueryWrapperX<>();
            queryWrapperX.eqIfPresent(AssetsDiscoveryTaskDO::getDatasourceId, AssetsAssetDO.getDatasourceId());
            List<AssetsDiscoveryTaskDO> taskDOList = AssetsDiscoveryTaskService.list(queryWrapperX);
            List<Long> taskIdList = taskDOList.stream().map(AssetsDiscoveryTaskDO::getId).collect(Collectors.toList());
            AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTableSaveReqVO = new AssetsDiscoveryTableSaveReqVO();
            AssetsDiscoveryTableSaveReqVO.setTaskIdList(taskIdList);
            AssetsDiscoveryTableSaveReqVO.setTableName(AssetsAssetDO.getTableName());
            AssetsDiscoveryTableSaveReqVO.setIgnoreFlag("0");
            AssetsDiscoveryTableSaveReqVO.setStatus("1");
            AssetsDiscoveryTableService.updateByTaskIdListAndTableNameStatus(AssetsDiscoveryTableSaveReqVO);
        }
// Ã¦ÂÂ¹Ã©ÂÂÃ¥ÂÂ Ã©ÂÂ¤Ã¦ÂÂ°Ã¦ÂÂ®Ã¨ÂµÂÃ¤ÂºÂ§
        return AssetsAssetMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeDaAsset(Long id) {
        ArrayList<Long> assetIdList = new ArrayList<>();
        assetIdList.add(id);
        int asset = dppEtlTaskService.checkTaskIdInAsset(assetIdList);
        if (asset > 0) {
            throw new ServiceException(",!");
        }
        AssetsAssetDO AssetsAssetDO = AssetsAssetMapper.selectById(id);
        if (AssetsAssetDO == null) {
            return 1;
        }
        if ("1".equals(AssetsAssetDO.getSource())) {
            LambdaQueryWrapperX<AssetsDiscoveryTaskDO> queryWrapperX = new LambdaQueryWrapperX<>();
            queryWrapperX.eqIfPresent(AssetsDiscoveryTaskDO::getDatasourceId, AssetsAssetDO.getDatasourceId());
            List<AssetsDiscoveryTaskDO> taskDOList = AssetsDiscoveryTaskService.list(queryWrapperX);
            List<Long> taskIdList = taskDOList.stream().map(AssetsDiscoveryTaskDO::getId).collect(Collectors.toList());
            AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTableSaveReqVO = new AssetsDiscoveryTableSaveReqVO();
            AssetsDiscoveryTableSaveReqVO.setTaskIdList(taskIdList);
            AssetsDiscoveryTableSaveReqVO.setTableName(AssetsAssetDO.getTableName());
            AssetsDiscoveryTableSaveReqVO.setIgnoreFlag("0");
            AssetsDiscoveryTableSaveReqVO.setStatus("1");
            AssetsDiscoveryTableService.updateByTaskIdListAndTableNameStatus(AssetsDiscoveryTableSaveReqVO);
        }
//Ã¥ÂÂ Ã©ÂÂ¤Ã©Â¡Â¹Ã§ÂÂ®
        IAssetsAssetProjectRelService.removeProjectRelByAssetId(id);
//Ã¥ÂÂ Ã©ÂÂ¤Ã¤Â¸Â»Ã©Â¢Â
        AssetsAssetThemeRelService.removeThemeRelByAssetId(id);
        AssetsAssetMapper.deleteAssetById(id);
// Ã¦ÂÂ¹Ã©ÂÂÃ¥ÂÂ Ã©ÂÂ¤Ã¦ÂÂ°Ã¦ÂÂ®Ã¨ÂµÂÃ¤ÂºÂ§
// Ã¦ÂÂ´Ã¦ÂÂ°Ã¦Â ÂÃ§Â­Â¾Ã¨ÂµÂÃ¤ÂºÂ§Ã¦ÂÂ°Ã©ÂÂ
        attTagAssetRelApiService.deleteRelByUpdateTag(id);
        return 1;
    }

    @Override
    public List<AssetsAssetDO> getDaAssetList() {
        return AssetsAssetMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetDO> getDaAssetMap() {
        List<AssetsAssetDO> AssetsAssetList = AssetsAssetMapper.selectList();
        return AssetsAssetList.stream().collect(Collectors.toMap(AssetsAssetDO::getId, AssetsAssetDO -> AssetsAssetDO,
// Ã¤Â¿ÂÃ§ÂÂÃ¥Â·Â²Ã¥Â­ÂÃ¥ÂÂ¨Ã§ÂÂÃ¥ÂÂ¼
                (existing, replacement) -> existing));
    }

    /**
     * *     * @param importExcelList      * @param isUpdateSupport      * @param operName             * @return
     */
    @Override
    public String importDaAsset(List<AssetsAssetRespVO> importExcelList, boolean isUpdateSupport, String operName) {
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
                        AssetsAssetDO existingDaAsset = AssetsAssetMapper.selectById(AssetsAssetId);
                        if (existingDaAsset != null) {
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
                    AssetsAssetDO existingDaAsset = AssetsAssetMapper.selectOne(queryWrapper);
                    if (existingDaAsset == null) {
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
        if (StringUtils.isNotEmpty(jsonObject.getStr("taskId")) && StringUtils.isNotEmpty(jsonObject.getStr("tableName"))) {
            AssetsDiscoveryTaskDO discoveryTaskDO = AssetsDiscoveryTaskService.getById(Long.valueOf(jsonObject.getStr("taskId")));
            tableName = jsonObject.getStr("tableName");
            DataSourceId = discoveryTaskDO.getDatasourceId();
        } else {
// Ã¨ÂÂ·Ã¥ÂÂÃ¨ÂµÂÃ¤ÂºÂ§Ã¨Â¯Â¦Ã¦ÂÂ
            AssetsAssetRespVO AssetsAssetDO = this.getDaAssetById(Long.valueOf(jsonObject.getStr("id")));
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
        }
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
        List<DbColumn> AssetsAssetColumns = AssetsAssetColumnMapper.findByAssetId(Long.parseLong(jsonObject.getStr("id"))).stream().map(e -> e.toDbColumn()).collect(Collectors.toList());
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
        List<Map<String, Object>> columnTable = new ArrayList<>();
        for (DbColumn column : AssetsAssetColumns) {
            Map<String, Object> columnMap = new HashMap<>();
            columnMap.put("field", column.getColName());
            columnMap.put("en", column.getColName());
            columnMap.put("cn", column.getColComment());
            columnMap.put("columnNullable", column.getNullable());
            columnMap.put("columnKey", column.getColKey());
            columnTable.add(columnMap);
        }
        List<Map> orderByList = jsonObject.getBeanList("orderBy", Map.class);
        PageUtil pageUtil = new PageUtil(pageNum, pageSize);
        List<Map<String, Object>> queryList;
        queryList = dbQuery.queryDbColumnByList(columns, tableName, dbQueryProperty, jsonObject.getStr("filter"), orderByList, pageUtil.getOffset(), pageSize);
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
        Map<Long, AssetsSensitiveLevelDO> levelMap = AssetsSensitiveLevelMapper.selectList(new QueryWrapper<AssetsSensitiveLevelDO>().eq("online_flag", 1)).stream().collect(Collectors.toMap(AssetsSensitiveLevelDO::getId, x -> x, (a, b) -> a));
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
        AssetsAssetPageReqVO.setDatasourceId(String.valueOf(AssetsAssetDO.getDatasourceId()));
        AssetsAssetDO assetDO = this.getDaAssetByDaAssetPageReqVO(AssetsAssetPageReqVO);
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
            AssetsAssetThemeRelService.createDaAssetThemeRelList(themeIdList, AssetsAssetDO.getId());
        }
        List<AssetsAssetColumnDO> AssetsAssetColumnList = new ArrayList<>();
        if (assetDO != null) {
            AssetsAssetColumnPageReqVO AssetsAssetColumnPageReqVO = new AssetsAssetColumnPageReqVO();
            AssetsAssetColumnPageReqVO.setAssetId(String.valueOf(assetDO.getId()));
            List<AssetsAssetColumnDO> AssetsAssetColumnList1 = IAssetsAssetColumnService.getDaAssetColumnList(AssetsAssetColumnPageReqVO);
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
            IAssetsAssetColumnService.removeDaAssetColumn(nonExistingIdList);
        }
        Long AssetsAssetDOId = AssetsAssetDO.getId();
        for (AssetsAssetColumnSaveReqVO AssetsAssetColumnSaveReqVO : columnSaveReqVOList) {
            AssetsAssetColumnSaveReqVO.setAssetId(String.valueOf(AssetsAssetDOId));
            if (AssetsAssetColumnSaveReqVO.getId() == null) {
                IAssetsAssetColumnService.createDaAssetColumn(AssetsAssetColumnSaveReqVO);
            } else {
                IAssetsAssetColumnService.updateDaAssetColumn(AssetsAssetColumnSaveReqVO);
            }
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
        AssetsAssetPageReqVO.setDatasourceId(String.valueOf(AssetsAssetDO.getDatasourceId()));
        AssetsAssetDO assetDO = this.getDaAssetByDaAssetPageReqVO(AssetsAssetPageReqVO);
        if (assetDO == null) {
            return;
        }
        AssetsAssetMapper.deleteAssetById(assetDO.getId());
        AssetsAssetColumnMapper.deleteAssetColumnByAssetId(assetDO.getId());
        AssetsAssetThemeRelService.removeThemeRelByAssetId(assetDO.getId());
    }

    @Override
    public PageResult<AssetsAssetDO> getDppAssetPage(AssetsAssetPageReqVO AssetsAsset) {
        if (StringUtils.isEmpty(AssetsAsset.getProjectCode()) || AssetsAsset.getProjectId() == null) {
            return new PageResult<AssetsAssetDO>();
        }
        LambdaQueryWrapperX<AssetsAssetApplyDO> queryWrapperX = new LambdaQueryWrapperX();
        String[] sourceTypeArr = AssetsAsset.getParams().get("sourceType") == null ? null : AssetsAsset.getParams().get("sourceType").toString().split(",");
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getStatus, "3");
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getProjectId, AssetsAsset.getProjectId());
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getProjectCode, AssetsAsset.getProjectCode());
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
        PageResult<AssetsAssetDO> AssetsAssetPage = this.getDaAssetPage(AssetsAsset, "2");
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
    public List<AssetsAssetDO> getDppAssetNoPageList(AssetsAssetPageReqVO AssetsAsset) {
        if (StringUtils.isEmpty(AssetsAsset.getProjectCode()) || AssetsAsset.getProjectId() == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapperX<AssetsAssetApplyDO> queryWrapperX = new LambdaQueryWrapperX();
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getStatus, "3");
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getProjectId, AssetsAsset.getProjectId());
        queryWrapperX.eqIfPresent(AssetsAssetApplyDO::getProjectCode, AssetsAsset.getProjectCode());
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
    public Long createDaAssetNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (StringUtils.equals("1", AssetsAsset.getCreateType())) {
            setDaAssetDefaultValues(AssetsAsset);
            Long assetId = createDaAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
            createDaAssetProjectRel(AssetsAsset);
            createDaAssetThemeIdList(AssetsAsset);
            return AssetsAsset.getId();
        }
//1:Ã¦ÂÂ°Ã¦ÂÂ®Ã¥ÂºÂÃ¨Â¡Â¨  2:Ã¥Â¤ÂÃ©ÂÂ¨API 3: Ã¥ÂÂ°Ã§ÂÂÃ§Â©ÂºÃ©ÂÂ´Ã¦ÂÂÃ¥ÂÂ¡ 4:Ã§ÂÂ¢Ã©ÂÂÃ¦ÂÂ°Ã¦ÂÂ® 5:Ã¨Â§ÂÃ©Â¢ÂÃ¦ÂÂ°Ã¦ÂÂ®
        String type = AssetsAsset.getType();
        if (StringUtils.equals("1", type)) {
            createDaAssetColumnNew(AssetsAsset);
        } else if (StringUtils.equals("2", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetApiNew(AssetsAsset);
        } else if (StringUtils.equals("3", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetGisNew(AssetsAsset);
        } else if (StringUtils.equals("4", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetGeoNew(AssetsAsset);
        } else if (StringUtils.equals("5", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetVideoNew(AssetsAsset);
        } else if (StringUtils.equals("6", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetFilesNew(AssetsAsset);
        } else if ("7".equals(type)) {
            AssetsAsset.setTableName("-1");
            AssetsAsset.setDataCount(0L);
            AssetsAsset.setFieldCount(0L);
            createDaAssetFileNew(AssetsAsset);
        } else {
            throw new ServiceException("");
        }
        createDaAssetProjectRel(AssetsAsset);
        createDaAssetThemeIdList(AssetsAsset);
        return AssetsAsset.getId();
    }

    @Override
    public Long createDaAssetBindResources(AssetsAssetSaveReqVO AssetsAsset) {
//1:Ã¦ÂÂ°Ã¦ÂÂ®Ã¥ÂºÂÃ¨Â¡Â¨  2:Ã¥Â¤ÂÃ©ÂÂ¨API 3: Ã¥ÂÂ°Ã§ÂÂÃ§Â©ÂºÃ©ÂÂ´Ã¦ÂÂÃ¥ÂÂ¡ 4:Ã§ÂÂ¢Ã©ÂÂÃ¦ÂÂ°Ã¦ÂÂ® 5:Ã¨Â§ÂÃ©Â¢ÂÃ¦ÂÂ°Ã¦ÂÂ®
        String type = AssetsAsset.getType();
        if (StringUtils.equals("1", type)) {
            createDaAssetColumnNew(AssetsAsset);
        } else if (StringUtils.equals("2", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetApiNew(AssetsAsset);
        } else if (StringUtils.equals("3", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetGisNew(AssetsAsset);
        } else if (StringUtils.equals("4", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetGeoNew(AssetsAsset);
        } else if (StringUtils.equals("5", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetVideoNew(AssetsAsset);
        } else if (StringUtils.equals("6", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            createDaAssetFilesNew(AssetsAsset);
        } else {
            throw new ServiceException("");
        }
//
        createDaAssetProjectRel(AssetsAsset);
        createDaAssetThemeIdList(AssetsAsset);
        AssetsAsset.setCreateType("2");
        updateDaAsset(AssetsAsset);
        return 1L;
    }

    private void createDaAssetFilesNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getId() == null) {
            Long assetId = createDaAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        AssetsAssetFilesSaveReqVO AssetsAssetFiles = AssetsAsset.getAssetsAssetFiles();
        AssetsAssetFiles.setAssetId(AssetsAsset.getId());
        int lastDot = AssetsAssetFiles.getUrl().lastIndexOf('.');
        String type = AssetsAssetFiles.getUrl().substring(lastDot);
        AssetsAssetFiles.setType(type);
        AssetsAssetFilesService.createDaAssetFiles(AssetsAssetFiles);
        if (StringUtils.equalsIgnoreCase(".xls", AssetsAssetFiles.getType()) || StringUtils.equalsIgnoreCase(".xlsx", AssetsAssetFiles.getType())) {
            List<AssetsAssetColumnDO> AssetsAssetColumnDOS = getExcelColumn(AssetsAssetFiles.getUrl(), AssetsAssetFiles.getStartColumn(), AssetsAssetFiles.getStartData(), AssetsAsset.getId());
            IAssetsAssetColumnService.saveBatch(AssetsAssetColumnDOS);
        }
        if (StringUtils.equalsIgnoreCase(".csv", AssetsAssetFiles.getType())) {
            List<AssetsAssetColumnDO> AssetsAssetColumnDOS = getCsvColumn(AssetsAssetFiles.getUrl(), AssetsAsset.getId());
            IAssetsAssetColumnService.saveBatch(AssetsAssetColumnDOS);
        }
    }

    private void createDaAssetFileNew(AssetsAssetSaveReqVO AssetsAsset) {
        Assert.notNull(AssetsAsset.getFileInfo(), () -> new ServiceException(""));
        AssetsDatasourceDO AssetsDatasourceDO = AssetsDatasourceMapper.selectById(AssetsAsset.getDatasourceId());
        if (AssetsAsset.getId() == null) {
            Long assetId = createDaAsset(AssetsAsset);
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

    private void createDaAssetVideoNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getId() == null) {
            Long assetId = createDaAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        AssetsAssetVideoSaveReqVO AssetsAssetVideo = AssetsAsset.getAssetsAssetVideo();
        AssetsAssetVideo.setAssetId(AssetsAsset.getId());
        IAssetsAssetVideoService.createDaAssetVideo(AssetsAssetVideo);
    }

    private void createDaAssetGisNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getId() == null) {
            Long assetId = createDaAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        AssetsAssetGisSaveReqVO AssetsAssetGis = AssetsAsset.getAssetsAssetGis();
        AssetsAssetGis.setAssetId(AssetsAsset.getId());
        IAssetsAssetGisService.createDaAssetGis(AssetsAssetGis);
    }

    private void setDaAssetDefaultValues(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAsset.setDatasourceId("-1");
        AssetsAsset.setTableName("-1");
        AssetsAsset.setDataCount(0L);
        AssetsAsset.setFieldCount(0L);
    }

    private void createDaAssetProjectRel(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getProjectId() == null) {
            return;
        }
        AssetsAssetProjectRelSaveReqVO AssetsAssetProjectRelSaveReqVO = new AssetsAssetProjectRelSaveReqVO();
        AssetsAssetProjectRelSaveReqVO.setProjectCode(AssetsAsset.getProjectCode());
        AssetsAssetProjectRelSaveReqVO.setProjectId(AssetsAsset.getProjectId());
        AssetsAssetProjectRelSaveReqVO.setAssetId(AssetsAsset.getId());
        IAssetsAssetProjectRelService.createDaAssetProjectRel(AssetsAssetProjectRelSaveReqVO);
    }

    /**
     * @param AssetsAsset
     */
    private void createDaAssetGeoNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getId() == null) {
            Long assetId = createDaAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        AssetsAssetGeoSaveReqVO AssetsAssetGeo = AssetsAsset.getAssetsAssetGeo();
        AssetsAssetGeo.setAssetId(AssetsAsset.getId());
        IAssetsAssetGeoService.createDaAssetGeo(AssetsAssetGeo);
    }

    private void createDaAssetApiNew(AssetsAssetSaveReqVO AssetsAsset) {
        if (AssetsAsset.getId() == null) {
            Long assetId = createDaAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        AssetsAssetApiSaveReqVO AssetsAssetApi = AssetsAsset.getAssetsAssetApi();
        AssetsAssetApi.setAssetId(AssetsAsset.getId());
        Long AssetsAssetApiId = IAssetsAssetApiService.createDaAssetApi(AssetsAssetApi);
        List<AssetsAssetApiParamSaveReqVO> AssetsAssetApiParamList = AssetsAsset.getAssetsAssetApiParamList();
        IAssetsAssetApiParamService.createDaAssetApiParamDeep(AssetsAssetApiParamList, AssetsAssetApiId);
    }

    /**
     * *     * @param AssetsAsset
     */
    private void createDaAssetThemeIdList(AssetsAssetSaveReqVO AssetsAsset) {
        List<String> themeIdList = AssetsAsset.getThemeIdList();
        if (CollectionUtils.isEmpty(themeIdList)) {
            return;
        }
        AssetsAssetThemeRelService.createDaAssetThemeRelList(themeIdList, AssetsAsset.getId());
    }

    /**
     * *     * @param AssetsAsset
     */
    private void createDaAssetColumnNew(AssetsAssetSaveReqVO AssetsAsset) {
        List<CatalogColumnRespDTO> CatalogColumnRespDTOList = mcColumnApiService.listByTableId(AssetsAsset.getTableId());
        List<AssetsAssetColumnDO> AssetsAssetColumnDOS = CatalogColumnRespDTOList.stream().map(CatalogColumnRespDTO -> new AssetsAssetColumnDO(CatalogColumnRespDTO)).collect(Collectors.toList());
        AssetsAsset.setFieldCount(Long.valueOf(AssetsAssetColumnDOS.size()));
        if (AssetsAsset.getId() == null) {
            Long assetId = createDaAsset(AssetsAsset);
            AssetsAsset.setId(assetId);
        }
        List<AssetsAssetColumnSaveReqVO> AssetsAssetColumnSaveReqVOList = BeanUtils.toBean(AssetsAssetColumnDOS, AssetsAssetColumnSaveReqVO.class);
        for (AssetsAssetColumnSaveReqVO AssetsAssetColumnSaveReqVO : AssetsAssetColumnSaveReqVOList) {
            AssetsAssetColumnSaveReqVO.setAssetId(String.valueOf(AssetsAsset.getId()));
            IAssetsAssetColumnService.createDaAssetColumn(AssetsAssetColumnSaveReqVO);
        }
    }

    @Override
    public int updateDaAssetNew(AssetsAssetSaveReqVO AssetsAsset) {
//1:Ã¦ÂÂ°Ã¦ÂÂ®Ã¥ÂºÂÃ¨Â¡Â¨  2:Ã¥Â¤ÂÃ©ÂÂ¨API 3: Ã¥ÂÂ°Ã§ÂÂÃ§Â©ÂºÃ©ÂÂ´Ã¦ÂÂÃ¥ÂÂ¡ 4:Ã§ÂÂ¢Ã©ÂÂÃ¦ÂÂ°Ã¦ÂÂ® 5:Ã¨Â§ÂÃ©Â¢ÂÃ¦ÂÂ°Ã¦ÂÂ®
        String type = AssetsAsset.getType();
        if (StringUtils.equals("1", type)) {
            AssetsAssetRespVO AssetsAssetById = getDaAssetById(AssetsAsset.getId());
            if (StringUtils.equals("1", AssetsAssetById.getCreateType()) && StringUtils.equals("2", AssetsAsset.getCreateType())) {
                createDaAssetColumnNew(AssetsAsset);
            }
        } else if (StringUtils.equals("2", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            updateDaAssetApiNew(AssetsAsset);
        } else if (StringUtils.equals("3", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            updateDaAssetGisNew(AssetsAsset);
        } else if (StringUtils.equals("4", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            updateDaAssetGeoNew(AssetsAsset);
        } else if (StringUtils.equals("5", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            updateDaAssetVideoNew(AssetsAsset);
        } else if (StringUtils.equals("6", type)) {
            setDaAssetDefaultValues(AssetsAsset);
            updateDaAssetFilesNew(AssetsAsset);
        }
        createDaAssetProjectRel(AssetsAsset);
        createDaAssetThemeIdList(AssetsAsset);
        updateDaAsset(AssetsAsset);
        return 1;
    }

    private void updateDaAssetVideoNew(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAssetVideoSaveReqVO AssetsAssetVideo = AssetsAsset.getAssetsAssetVideo();
        if (AssetsAssetVideo == null) {
            return;
        }
        AssetsAssetVideo.setAssetId(AssetsAsset.getId());
        IAssetsAssetVideoService.updateDaAssetVideo(AssetsAssetVideo);
    }

    private void updateDaAssetGisNew(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAssetGisSaveReqVO AssetsAssetGis = AssetsAsset.getAssetsAssetGis();
        if (AssetsAssetGis == null) {
            return;
        }
        AssetsAssetGis.setAssetId(AssetsAsset.getId());
        IAssetsAssetGisService.updateDaAssetGis(AssetsAssetGis);
    }

    private void updateDaAssetGeoNew(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAssetGeoSaveReqVO AssetsAssetGeo = AssetsAsset.getAssetsAssetGeo();
        if (AssetsAssetGeo == null) {
            return;
        }
        AssetsAssetGeo.setAssetId(AssetsAsset.getId());
        IAssetsAssetGeoService.updateDaAssetGeo(AssetsAssetGeo);
    }

    private void updateDaAssetApiNew(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAssetApiSaveReqVO AssetsAssetApi = AssetsAsset.getAssetsAssetApi();
        if (AssetsAssetApi == null) {
            return;
        }
        AssetsAssetApi.setAssetId(AssetsAsset.getId());
        IAssetsAssetApiService.updateDaAssetApi(AssetsAssetApi);
        List<AssetsAssetApiParamSaveReqVO> AssetsAssetApiParamList = AssetsAsset.getAssetsAssetApiParamList();
        IAssetsAssetApiParamService.createDaAssetApiParamDeep(AssetsAssetApiParamList, AssetsAssetApi.getId());
    }

    private void updateDaAssetColumnNew(AssetsAssetSaveReqVO AssetsAsset) {
        return;
    }

    private void updateDaAssetFilesNew(AssetsAssetSaveReqVO AssetsAsset) {
        AssetsAssetFilesSaveReqVO AssetsAssetFiles = AssetsAsset.getAssetsAssetFiles();
        int lastDot = AssetsAssetFiles.getUrl().lastIndexOf('.');
        String type = AssetsAssetFiles.getUrl().substring(lastDot);
        AssetsAssetFiles.setType(type);
        AssetsAssetFiles.setAssetId(AssetsAsset.getId());
        AssetsAssetFilesService.updateDaAssetFiles(AssetsAssetFiles);
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
    public void startDaAssetDatasourceTaskNull() {
        this.startDaAssetDatasourceTask(null);
    }

    @Override
    public AjaxResult startDaAssetDatasourceTask(Long id) {
        if (id != null) {
            AssetsAssetRespVO AssetsAssetById = this.getDaAssetById(id);
            if (StringUtils.equals("1", AssetsAssetById.getType())) {
// Ã¥Â¦ÂÃ©ÂÂÃ§ÂÂ¹Ã¦Â®ÂÃ¥Â¤ÂÃ§ÂÂÃ¯Â¼ÂÃ¥Â¡Â«Ã¥ÂÂÃ©ÂÂ»Ã¨Â¾Â
            }
            AssetsDatasourceDO AssetsDatasourceById = IAssetsDatasourceService.getDaDatasourceById(AssetsAssetById.getDatasourceId());
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
            List<AssetsAssetDO> AssetsAssetList = this.getDaAssetList(AssetsAsset);
            Map<Long, List<AssetsAssetDO>> datasourceGroupMap = AssetsAssetList.stream().collect(Collectors.groupingBy(AssetsAssetDO::getDatasourceId));
            for (Map.Entry<Long, List<AssetsAssetDO>> entry : datasourceGroupMap.entrySet()) {
                Long datasourceId = entry.getKey();
                List<AssetsAssetDO> assets = entry.getValue();
                AssetsDatasourceDO datasource = IAssetsDatasourceService.getDaDatasourceById(datasourceId);
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
    public PageResult<AssetsAssetDO> getDaAssetByIds(List<Long> ids) {
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
        Set<Long> dataElemIds = assetRelRespDTOS.stream().map(StandardsDataElemAssetRelRespDTO::getDataElemId).map(Long::valueOf).collect(Collectors.toSet());
        List<StandardsDataElemRuleRelRespDTO> ruleRelRespDTOS = elemRuleRelService.listByDataElemIdList(dataElemIds, type);
        if (ruleRelRespDTOS.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<Long>> map = assetRelRespDTOS.stream().filter(i -> StringUtils.isNotEmpty(i.getColumnId())).collect(Collectors.groupingBy(i -> Long.valueOf(i.getColumnId()), Collectors.mapping(i -> Long.valueOf(i.getDataElemId()), Collectors.toList())));
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
        DbQueryProperty dbProperty = new DbQueryProperty(datasource.getDatasourceType(), datasource.getIp(), datasource.getPort(), datasource.getDatasourceConfig());
        DbDialect dbDialect = DialectFactory.getDialect(DbType.getDbType(dbProperty.getDbType()));
        String tableName = dbDialect.getTableName(dbProperty, AssetsAsset.getTableName());
        LineageDTO lineageDTO = lineageDataService.lineage(dbProperty.trainToHostPort(), tableName);
//Ã¦Â Â¹Ã¦ÂÂ®taskÃ¦ÂÂ¥Ã¨Â¯Â¢Ã¥Â½ÂÃ¥ÂÂÃ¤Â»Â»Ã¥ÂÂ¡Ã¦ÂÂÃ¦ÂÂ°Ã§ÂÂÃ§ÂÂ¶Ã¦ÂÂ
        if (lineageDTO.getTasks() != null && lineageDTO.getTasks().size() > 0) {
            List<Long> ipList = lineageDTO.getTasks().stream().map(TaskNode::getTaskId).collect(Collectors.toList());
            Map<Long, TaskNode> taskNodeMap = lineageDTO.getTasks().stream().collect(Collectors.toMap(k -> k.getTaskId(), v -> v));
            dppEtlTaskInstanceService.getLastTaskInstance(ipList).forEach(taskInstance -> {
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
    public List<AssetsAssetDO> getDaAssetListAll(AssetsAssetPageReqVO AssetsAsset, String number) {
        List<AssetsAssetDO> AssetsAssetDOPageResult = AssetsAssetMapper.selectList();
        return AssetsAssetDOPageResult;
    }

    @Override
    public List<TreeData> getTreeData() {
        List<TreeData> treeData = new ArrayList<>();
        treeData.add(TreeData.builder().name("").type("0").otherData(JSON.parseObject("{\"tooltipStr\":\"\"}")).children(dmBusinessCategoryApiService.getTreeData("1")).build());
        treeData.add(TreeData.builder().name("").type("0").otherData(JSON.parseObject("{\"tooltipStr\":\"\"}")).children(dmThemeDomainApiService.getTreeData("1")).build());
        treeData.add(TreeData.builder().name("").type("0").otherData(JSON.parseObject("{\"tooltipStr\":\"\"}")).children(dmDataLayerApiService.getTreeData("1")).build());
        return treeData;
    }

    @Override
    public List<Long> createDaAssetBatchNew(List<AssetsAssetSaveReqVO> AssetsAssetList) {
        if (AssetsAssetList == null || AssetsAssetList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>(AssetsAssetList.size());
        if (this.count(Wrappers.lambdaQuery(AssetsAssetDO.class).in(AssetsAssetDO::getTableId, AssetsAssetList.stream().map(e -> e.getTableId()).collect(Collectors.toList()))) > 0) {
            throw new ServiceException("");
        }
        for (AssetsAssetSaveReqVO vo : AssetsAssetList) {
            Long id = this.createDaAssetNew(vo);
            ids.add(id);
        }
        return ids;
    }

    @Override
    public List<Map<String, Object>> dataMaskings(Long assetId, List<Map<String, Object>> Data, Long userId, String scene) {
        Map<String, Object> mt = new HashMap<>();
        List<Map<String, Object>> out = new ArrayList<>(Data.size());
        Map<String, Object> mk = new HashMap<>();
        List<AssetsAssetColumnDO> cols = AssetsAssetColumnMapper.findByAssetId(assetId);
        for (AssetsAssetColumnDO col : cols) {
            StandardsDesensitizeAssetcolumnDO assetcolumnDO = dgDesensitizeAssetcolumnService.getDgDesensitizeAssetcolumnByAid(col.getId());
            if (assetcolumnDO == null) {
                mt.put(col.getColumnName(), 1);
            } else {
                StandardsDesensitizeRuleDO rule = dgDesensitizeRuleService.getDgDesensitizeRuleByDataCategoryId(assetcolumnDO.getDataCategoryId());
                StandardsDesensitizeWhitelistDO white = whitelistService.getDgDesensitizeWhitelistByCategoryId(assetcolumnDO.getDataCategoryId());
                if (rule == null) {
                    mt.put(col.getColumnName(), 1);
                } else {
                    if (!rule.getValidFlag() || !rule.getApplicationScene().contains(scene)) {
                        mt.put(col.getColumnName(), 1);
                    } else {
                        mk.put("rp", rule.getReplaceContent());
                        if (rule.getIntervalList().size() > 0) {
                            mk.put("gz", rule.getIntervalList());
                            mt.put(col.getColumnName(), 2);
                        } else {
                            mt.put(col.getColumnName(), 1);
                        }
                    }
                }
                if (white != null) {
                    Date currtime = new Date();
                    boolean b = (!currtime.before(white.getStartTime())) && (!currtime.after(white.getEndTime()));
                    boolean c = white.getUserList().stream().anyMatch(userRelDO -> userRelDO.getUserId() == userId);
                    if (white.getValidFlag() && c && b) {
                        mt.put(col.getColumnName(), 1);
                    }
                }
            }
        }
        for (Map<String, Object> row : Data) {
            Map<String, Object> masked = new LinkedHashMap<>(row.size());
            for (Map.Entry<String, Object> e : row.entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                if (mt.get(key).toString().equals("1")) {
                    masked.put(key, val);
                } else if (mt.get(key).toString().equals("2")) {
                    String s = desensitizeByInterval2((String) val, (String) mk.get("rp"), (List<StandardsDesensitizeIntervalDO>) mk.get("gz"));
                    masked.put(key, s);
                }
            }
            out.add(masked);
        }
        return out;
    }

    @Override
    public List<AssetsAssetDO> getDaAssetByDataSourceId(Long DataSourceId, String tableName) {
        return this.list(Wrappers.lambdaQuery(AssetsAssetDO.class).eq(AssetsAssetDO::getDatasourceId, DataSourceId).eq(AssetsAssetDO::getTableName, tableName));
    }
    public static String desensitizeByInterval(String originalStr, String replaceStr, List<StandardsDesensitizeIntervalDO> intervalList) {
        if (originalStr == null || originalStr.isEmpty()) return originalStr;
        if (replaceStr == null || replaceStr.isEmpty()) return originalStr;
        if (intervalList == null || intervalList.isEmpty()) return originalStr;
        char replaceChar = replaceStr.charAt(0);
        char[] chars = originalStr.toCharArray();
        int len = chars.length;
        intervalList.sort(Comparator.comparing(StandardsDesensitizeIntervalDO::getIntervalNo));
        for (StandardsDesensitizeIntervalDO interval : intervalList) {
            Long startL = interval.getStartNum();
            Long endL = interval.getEndNum();
            if (startL == null || endL == null) continue;
            int start = startL.intValue() - 1;
            int end = endL.intValue() - 1;
            start = Math.max(start, 0);
            end = Math.min(end, len - 1);
            if (start > end) continue;
            for (int i = start; i <= end; i++) {
                chars[i] = replaceChar;
            }
        }
        return new String(chars);
    }

    public static String desensitizeByInterval2(String originalStr, String replaceStr, List<StandardsDesensitizeIntervalDO> intervalList) {
        if (originalStr == null || originalStr.isEmpty()) return originalStr;
        if (replaceStr == null || replaceStr.isEmpty()) return originalStr;
        if (intervalList == null || intervalList.isEmpty()) return originalStr;
        List<StandardsDesensitizeIntervalDO> sortedList = new ArrayList<>(intervalList);
        sortedList.sort(Comparator.comparing(StandardsDesensitizeIntervalDO::getIntervalNo));
        StringBuilder sb = new StringBuilder(originalStr);
        int offset = 0;
        for (StandardsDesensitizeIntervalDO interval : sortedList) {
            Long s = interval.getStartNum();
            Long e = interval.getEndNum();
            if (s == null || e == null) continue;
            int start = s.intValue() - 1;
            int end = e.intValue() - 1;
            int len = end - start + 1;
            if (len <= 0) continue;
            int replaceStart = start - offset;
            if (replaceStart < 0) replaceStart = 0;
            sb.replace(replaceStart, replaceStart + len, replaceStr);
            offset += (len - 1);
        }
        return sb.toString();
    }

    private void updateAssetFieldAndDataCount(DbQuery dbQuery, DbQueryProperty dbQueryProperty, AssetsAssetDO assetDO) {
        List<DbColumn> tableColumns = dbQuery.getTableColumns(dbQueryProperty, assetDO.getTableName());
        int tableColumnsSize = CollectionUtils.isEmpty(tableColumns) ? 0 : tableColumns.size();
        int dataCount = dbQuery.countNew(assetDO.getTableName(), new HashMap<>());
        AssetsAssetSaveReqVO updateObj = BeanUtils.toBean(assetDO, AssetsAssetSaveReqVO.class);
        updateObj.setFieldCount((long) tableColumnsSize);
        updateObj.setDataCount((long) dataCount);
        this.updateDaAsset(updateObj);
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
    public Map<String, Object> getDaAssetOverviewStatistics() {
        Map<String, Object> map = AssetsAssetMapper.getDaAssetOverviewStatistics();
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
    public List<Long> getMcTableInDaAsset(List<Long> mcTableIds) {
        List<AssetsAssetDO> AssetsAssetDOList = this.list(Wrappers.lambdaQuery(AssetsAssetDO.class).select(AssetsAssetDO::getTableId).in(AssetsAssetDO::getTableId, mcTableIds));
        if (AssetsAssetDOList != null || AssetsAssetDOList.size() > 0) {
            return AssetsAssetDOList.stream().map(AssetsAssetDO::getTableId).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public void mcTableColumnUpdateToDaAssetColumn(Map<Long, List<CatalogColumnRespDTO>> columnMap) {
        if (columnMap.isEmpty()) {
            return;
        }
        List<AssetsAssetDO> AssetsAssetDOList = this.list(Wrappers.lambdaQuery(AssetsAssetDO.class).in(AssetsAssetDO::getTableId, columnMap.keySet()));
        for (AssetsAssetDO AssetsAssetDO : AssetsAssetDOList) {
            List<CatalogColumnRespDTO> columnList = columnMap.get(AssetsAssetDO.getTableId());
            if (columnList == null || columnList.size() == 0) {
                continue;
            }
//
            //columnList List<CatalogColumnRespDTO> List<AssetsAssetColumnDO>
            List<AssetsAssetColumnDO> newAssetColumns = convertMcColumnToDaAssetColumn(AssetsAssetDO, columnList);
            List<AssetsAssetColumnDO> oldAssetColumns = IAssetsAssetColumnService.list(Wrappers.lambdaQuery(AssetsAssetColumnDO.class).eq(AssetsAssetColumnDO::getAssetId, AssetsAssetDO.getId()));
            Map<String, List<AssetsAssetColumnDO>> compareResult = compareAssetColumns(newAssetColumns, oldAssetColumns);
            List<AssetsAssetColumnDO> addList = compareResult.get("addList");
            List<AssetsAssetColumnDO> updateList = compareResult.get("updateList");
            List<AssetsAssetColumnDO> deleteList = compareResult.get("deleteList");
            if (addList != null && addList.size() > 0) {
                IAssetsAssetColumnService.saveBatch(addList);
            }
            if (updateList != null && updateList.size() > 0) {

                IAssetsAssetColumnService.updateBatchById(updateList);
            }
            if (deleteList != null && deleteList.size() > 0) {
                IAssetsAssetColumnService.removeByIds(deleteList.stream().map(AssetsAssetColumnDO::getId).collect(Collectors.toList()));
            }
        }
    }

    private List<AssetsAssetColumnDO> convertMcColumnToDaAssetColumn(AssetsAssetDO AssetsAssetDO, List<CatalogColumnRespDTO> mcColumnList) {
        if (mcColumnList == null || mcColumnList.isEmpty()) {
            return new ArrayList<>();

        }
        return mcColumnList.stream().map(mcColumn -> {
            AssetsAssetColumnDO assetColumn = new AssetsAssetColumnDO();
            assetColumn.setAssetId(AssetsAssetDO.getId());
            assetColumn.setColumnName(mcColumn.getColumnName());
            assetColumn.setColumnComment(mcColumn.getColumnComment());
            assetColumn.setColumnType(mcColumn.getColumnType());
            assetColumn.setColumnLength(mcColumn.getColumnLength() != null ? mcColumn.getColumnLength().longValue() : null);
            assetColumn.setColumnScale(mcColumn.getColumnScale() != null ? mcColumn.getColumnScale().longValue() : null);
            assetColumn.setNullableFlag(mcColumn.getNullableFlag());
            assetColumn.setPkFlag(mcColumn.getPkFlag());
            assetColumn.setDefaultValue(mcColumn.getDefaultValue());
            assetColumn.setDataElemCodeFlag("0");
            assetColumn.setRelDataElmeFlag("0");
            assetColumn.setRelCleanFlag("0");
            assetColumn.setRelAuditFlag("0");

            return assetColumn;
        }).collect(Collectors.toList());
    }

    /**
     * *     * @param newAssetColumns      * @param oldAssetColumns      * @return MapdeleteList()updateList()addList()
     */
    public Map<String, List<AssetsAssetColumnDO>> compareAssetColumns(List<AssetsAssetColumnDO> newAssetColumns, List<AssetsAssetColumnDO> oldAssetColumns) {
        Map<String, List<AssetsAssetColumnDO>> result = new HashMap<>();
        List<AssetsAssetColumnDO> deleteList = new ArrayList<>();
        List<AssetsAssetColumnDO> updateList = new ArrayList<>();
        List<AssetsAssetColumnDO> addList = new ArrayList<>();
        Map<String, AssetsAssetColumnDO> oldColumnMap = oldAssetColumns.stream().collect(Collectors.toMap(AssetsAssetColumnDO::getColumnName, column -> column));
        Map<String, AssetsAssetColumnDO> newColumnMap = newAssetColumns.stream().collect(Collectors.toMap(AssetsAssetColumnDO::getColumnName, column -> column));
        for (AssetsAssetColumnDO oldColumn : oldAssetColumns) {
            if (!newColumnMap.containsKey(oldColumn.getColumnName())) {
                deleteList.add(oldColumn);
            }
        }
        for (AssetsAssetColumnDO newColumn : newAssetColumns) {
            if (!oldColumnMap.containsKey(newColumn.getColumnName())) {
                addList.add(newColumn);
            }
        }
        for (AssetsAssetColumnDO newColumn : newAssetColumns) {
            AssetsAssetColumnDO oldColumn = oldColumnMap.get(newColumn.getColumnName());
            if (oldColumn != null) {
                if (isColumnChanged(newColumn, oldColumn)) {
                    updateList.add(newColumn);
                }
            }
        }
        result.put("deleteList", deleteList);
        result.put("updateList", updateList);
        result.put("addList", addList);
        return result;
    }

    /**
     * *     * @param newColumn      * @param oldColumn      * @return
     */
    private boolean isColumnChanged(AssetsAssetColumnDO newColumn, AssetsAssetColumnDO oldColumn) {
        if (!Objects.equals(newColumn.getColumnType(), oldColumn.getColumnType())) {
            return true;
        }
        if (!Objects.equals(newColumn.getColumnLength(), oldColumn.getColumnLength())) {
            return true;
        }
        if (!Objects.equals(newColumn.getColumnScale(), oldColumn.getColumnScale())) {
            return true;
        }
        if (!Objects.equals(newColumn.getPkFlag(), oldColumn.getPkFlag())) {
            return true;
        }
        if (!Objects.equals(newColumn.getNullableFlag(), oldColumn.getNullableFlag())) {
            return true;
        }
        if (!Objects.equals(newColumn.getDefaultValue(), oldColumn.getDefaultValue())) {
            return true;
        }
        if (!Objects.equals(newColumn.getColumnComment(), oldColumn.getColumnComment())) {
            return true;
        }
        return false;
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
