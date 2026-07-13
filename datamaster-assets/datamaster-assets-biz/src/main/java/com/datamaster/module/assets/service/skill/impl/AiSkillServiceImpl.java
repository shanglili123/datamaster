package com.datamaster.module.assets.service.skill.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillPageReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillSaveReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillVersionRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiDatabaseSkillGenerateReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiMultiTableSkillGenerateReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiTableSkillGenerateReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillRefDO;
import com.datamaster.module.assets.dal.dataobject.skill.AiSkillVersionDO;
import com.datamaster.module.assets.dal.mapper.asset.AssetsAssetMapper;
import com.datamaster.module.assets.dal.mapper.assetColumn.AssetsAssetColumnMapper;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceMapper;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillMapper;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillRefMapper;
import com.datamaster.module.assets.dal.mapper.skill.AiSkillVersionMapper;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.assets.service.skill.IAiModelGatewayService;
import com.datamaster.module.assets.service.skill.IAiSkillService;
import com.datamaster.module.catalog.api.column.dto.CatalogColumnRespDTO;
import com.datamaster.module.catalog.api.service.column.CatalogColumnApiService;
import com.datamaster.module.catalog.api.service.table.CatalogTableApiService;
import com.datamaster.module.catalog.api.table.dto.CatalogTableRespDTO;
import com.datamaster.module.collector.api.qa.dto.CollectorQualitySummaryRespDTO;
import com.datamaster.module.collector.api.service.qa.CollectorQualityTaskApiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Skill service implementation.
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AiSkillServiceImpl implements IAiSkillService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_GENERATED = "GENERATED";
    private static final String SOURCE_GENERATED_EDITED = "GENERATED_EDITED";

    private static final String TYPE_PLATFORM_METADATA = "PLATFORM_METADATA";
    private static final String TYPE_PLATFORM_QUALITY = "PLATFORM_QUALITY";
    private static final String TYPE_TABLE = "TABLE";
    private static final String TYPE_DATABASE = "DATABASE";
    private static final String TYPE_MULTI_TABLE = "MULTI_TABLE";

    private static final String BIZ_OBJECT_TABLE = "TABLE";
    private static final String BIZ_OBJECT_DATA_SOURCE = "DATA_SOURCE";
    private static final String BIZ_OBJECT_TABLE_GROUP = "TABLE_GROUP";

    private static final String MANUAL_BEGIN = "<!-- MANUAL_NOTES_BEGIN -->";
    private static final String MANUAL_END = "<!-- MANUAL_NOTES_END -->";
    private static final int MAX_AI_TEXT_LENGTH = 180;
    private static final int MAX_AI_LIST_SIZE = 8;
    private static final Pattern BACKTICK_TOKEN_PATTERN = Pattern.compile("`([^`]+)`");

    @Resource
    private AiSkillMapper aiSkillMapper;
    @Resource
    private AiSkillVersionMapper aiSkillVersionMapper;
    @Resource
    private AiSkillRefMapper aiSkillRefMapper;
    @Resource
    private AssetsDatasourceMapper assetsDatasourceMapper;
    @Resource
    private AssetsAssetMapper assetsAssetMapper;
    @Resource
    private AssetsAssetColumnMapper assetsAssetColumnMapper;
    @Resource
    private IAiModelGatewayService aiModelGatewayService;
    @Resource
    private IAssetsTableGovernanceApiService assetsTableGovernanceApiService;
    @Resource
    private CatalogColumnApiService catalogColumnApiService;
    @Resource
    private CatalogTableApiService catalogTableApiService;
    @Resource
    private CollectorQualityTaskApiService collectorQualityTaskApiService;

    @Override
    public PageResult<AiSkillRespVO> getSkillPage(AiSkillPageReqVO pageReqVO) {
        return BeanUtils.toBean(aiSkillMapper.selectPage(pageReqVO), AiSkillRespVO.class);
    }

    @Override
    public AiSkillRespVO getSkill(Long id) {
        AiSkillDO skill = requireSkill(id);
        return BeanUtils.toBean(skill, AiSkillRespVO.class);
    }

    @Override
    public Long createSkill(AiSkillSaveReqVO saveReqVO) {
        if (aiSkillMapper.selectBySkillCode(saveReqVO.getSkillCode()) != null) {
            throw new ServiceException("Skill编码已存在");
        }
        AiSkillDO skill = BeanUtils.toBean(saveReqVO, AiSkillDO.class);
        skill.setStatus(defaultText(saveReqVO.getStatus(), STATUS_DRAFT));
        skill.setSourceType(defaultText(saveReqVO.getSourceType(), SOURCE_MANUAL));
        skill.setVersion(1);
        skill.setContentHash(hash(saveReqVO.getContent()));
        aiSkillMapper.insert(skill);
        insertVersion(skill, "CREATE", saveReqVO.getChangeRemark());
        return skill.getId();
    }

    @Override
    public Integer updateSkill(AiSkillSaveReqVO saveReqVO) {
        AiSkillDO old = requireSkill(saveReqVO.getId());
        AiSkillDO skill = BeanUtils.toBean(saveReqVO, AiSkillDO.class);
        skill.setVersion(nextVersion(old));
        skill.setContentHash(hash(saveReqVO.getContent()));
        skill.setStatus(defaultText(saveReqVO.getStatus(), old.getStatus()));
        skill.setSourceType(resolveUpdateSourceType(old, saveReqVO));
        int updated = aiSkillMapper.updateById(skill);
        AiSkillDO saved = requireSkill(saveReqVO.getId());
        insertVersion(saved, "EDIT", saveReqVO.getChangeRemark());
        return updated;
    }

    @Override
    public Integer removeSkill(Long id) {
        AiSkillDO skill = requireSkill(id);
        skill.setStatus(STATUS_ARCHIVED);
        return aiSkillMapper.updateById(skill);
    }

    @Override
    public Integer publishSkill(Long id) {
        AiSkillDO skill = requireSkill(id);
        skill.setStatus(STATUS_PUBLISHED);
        int updated = aiSkillMapper.updateById(skill);
        insertVersion(skill, "PUBLISH", "发布Skill");
        return updated;
    }

    @Override
    public Integer rollbackSkill(Long id, Integer version) {
        AiSkillDO skill = requireSkill(id);
        AiSkillVersionDO versionDO = aiSkillVersionMapper.selectBySkillIdAndVersion(id, version);
        if (versionDO == null) {
            throw new ServiceException("Skill版本不存在");
        }
        skill.setContent(versionDO.getContent());
        skill.setVersion(nextVersion(skill));
        skill.setContentHash(hash(versionDO.getContent()));
        int updated = aiSkillMapper.updateById(skill);
        insertVersion(skill, "ROLLBACK", "回滚到版本 " + version);
        return updated;
    }

    @Override
    public List<AiSkillVersionRespVO> getSkillVersions(Long id) {
        requireSkill(id);
        return BeanUtils.toBean(aiSkillVersionMapper.selectListBySkillId(id), AiSkillVersionRespVO.class);
    }

    @Override
    public AiSkillRespVO generateMetadataSkill(Boolean publish) {
        String content = metadataSkillContent();
        AiSkillDO skill = upsertGeneratedSkill("datamaster-metadata", "DataMaster元数据采集能力",
                TYPE_PLATFORM_METADATA, null, null, content, Boolean.TRUE.equals(publish));
        return BeanUtils.toBean(skill, AiSkillRespVO.class);
    }

    @Override
    public AiSkillRespVO generateQualitySkill(Boolean publish) {
        String content = qualitySkillContent();
        AiSkillDO skill = upsertGeneratedSkill("datamaster-quality", "DataMaster质量核检能力",
                TYPE_PLATFORM_QUALITY, null, null, content, Boolean.TRUE.equals(publish));
        return BeanUtils.toBean(skill, AiSkillRespVO.class);
    }

    @Override
    public AiSkillRespVO generateTableSkill(AiTableSkillGenerateReqVO reqVO) {
        if (reqVO == null) {
            throw new ServiceException("生成参数不能为空");
        }
        TableSkillContext tableContext = resolveTableSkillContext(reqVO);
        AssetsAssetDO asset = tableContext.asset;
        List<AssetsAssetColumnDO> columns = tableContext.columns;
        AssetsDatasourceDO datasource = asset.getDatasourceId() == null ? null : assetsDatasourceMapper.selectById(asset.getDatasourceId());
        CollectorQualitySummaryRespDTO qualitySummary = findQualitySummary(asset);
        String skillCode = tableSkillCode(datasource, asset);
        AiSkillDO oldSkill = aiSkillMapper.selectByBizObject(BIZ_OBJECT_TABLE, asset.getId());
        String manualNotes = resolveManualNotes(oldSkill, reqVO);
        SkillEnhancement enhancement = generateTableEnhancement(asset, datasource, columns, manualNotes);
        String content = tableSkillContent(skillCode, asset, datasource, columns, manualNotes, enhancement, qualitySummary);
        boolean publish = Boolean.TRUE.equals(reqVO.getPublish());
        AiSkillDO skill = upsertGeneratedSkill(skillCode, firstNonBlank(asset.getTableComment(), asset.getName(), asset.getTableName()) + "问数Skill",
                TYPE_TABLE, BIZ_OBJECT_TABLE, asset.getId(), content, publish);
        refreshTableRefs(skill, asset, columns, datasource);
        return BeanUtils.toBean(skill, AiSkillRespVO.class);
    }

    @Override
    public AiSkillRespVO generateDatabaseSkill(AiDatabaseSkillGenerateReqVO reqVO) {
        if (reqVO == null || reqVO.getDatasourceId() == null) {
            throw new ServiceException("请选择数据源");
        }
        AssetsDatasourceDO datasource = assetsDatasourceMapper.selectById(reqVO.getDatasourceId());
        if (datasource == null) {
            throw new ServiceException("数据源不存在");
        }
        List<AssetsAssetDO> assets = resolveMetadataTablesForDatasource(reqVO.getDatasourceId());
        if (assets == null || assets.isEmpty()) {
            throw new ServiceException("当前数据源未获取到表元数据");
        }
        Map<Long, List<AssetsAssetColumnDO>> columnMap = collectColumnsForMetadataTables(assets);
        String skillCode = databaseSkillCode(datasource);
        AiSkillDO oldSkill = aiSkillMapper.selectByBizObject(BIZ_OBJECT_DATA_SOURCE, datasource.getId());
        String manualNotes = resolveManualNotes(oldSkill, reqVO.getManualNotes());
        String content = databaseSkillContent(skillCode, datasource, assets, columnMap, manualNotes);
        AiSkillDO skill = upsertGeneratedSkill(skillCode, firstNonBlank(datasource.getDatasourceName(), datasource.getDatasourceType(), String.valueOf(datasource.getId())) + "整库问数Skill",
                TYPE_DATABASE, BIZ_OBJECT_DATA_SOURCE, datasource.getId(), content, Boolean.TRUE.equals(reqVO.getPublish()));
        refreshDatabaseRefs(skill, datasource, assets);
        return BeanUtils.toBean(skill, AiSkillRespVO.class);
    }

    @Override
    public AiSkillRespVO generateMultiTableSkill(AiMultiTableSkillGenerateReqVO reqVO) {
        if (reqVO == null || reqVO.getDatasourceId() == null) {
            throw new ServiceException("请选择数据源");
        }
        if (reqVO.getTableNames() == null || reqVO.getTableNames().size() < 2) {
            throw new ServiceException("多表Skill至少需要选择两张表");
        }
        AssetsDatasourceDO datasource = assetsDatasourceMapper.selectById(reqVO.getDatasourceId());
        if (datasource == null) {
            throw new ServiceException("数据源不存在");
        }
        List<AssetsAssetDO> assets = new ArrayList<>();
        for (String tableName : reqVO.getTableNames()) {
            if (StringUtils.isBlank(tableName)) {
                continue;
            }
            AiTableSkillGenerateReqVO tableReq = new AiTableSkillGenerateReqVO();
            tableReq.setDatasourceId(reqVO.getDatasourceId());
            tableReq.setTableName(tableName);
            tableReq.setForceRefresh(reqVO.getForceRefresh());
            assets.add(resolveTableSkillContext(tableReq).asset);
        }
        if (assets.size() < 2) {
            throw new ServiceException("多表Skill至少需要选择两张有效表");
        }
        Map<Long, List<AssetsAssetColumnDO>> columnMap = collectColumnsForMetadataTables(assets);
        String skillCode = multiTableSkillCode(datasource, assets);
        AiSkillDO oldSkill = aiSkillMapper.selectBySkillCode(skillCode);
        String manualNotes = resolveManualNotes(oldSkill, reqVO.getManualNotes());
        String content = multiTableSkillContent(skillCode, datasource, assets, columnMap, manualNotes);
        AiSkillDO skill = upsertGeneratedSkill(skillCode, multiTableSkillName(assets) + "多表问数Skill",
                TYPE_MULTI_TABLE, BIZ_OBJECT_TABLE_GROUP, null, content, Boolean.TRUE.equals(reqVO.getPublish()));
        refreshMultiTableRefs(skill, datasource, assets, columnMap);
        return BeanUtils.toBean(skill, AiSkillRespVO.class);
    }

    private CatalogTableRespDTO resolveMetadataTableForSkill(AiTableSkillGenerateReqVO reqVO) {
        CatalogTableRespDTO table = null;
        if (reqVO.getAssetId() != null) {
            table = catalogTableApiService.getById(reqVO.getAssetId());
        }
        if (table == null) {
            if (reqVO.getDatasourceId() == null || StringUtils.isBlank(reqVO.getTableName())) {
                throw new ServiceException("请选择数据源和表，或传入元数据表ID");
            }
            table = catalogTableApiService.getByDatasourceIdAndTableName(reqVO.getDatasourceId(), reqVO.getTableName());
        }
        if (table == null) {
            throw new ServiceException("未获取到表元数据，请先完成元数据采集");
        }
        return table;
    }

    private TableSkillContext resolveTableSkillContext(AiTableSkillGenerateReqVO reqVO) {
        AssetsAssetDO asset = resolveAssetTableForSkill(reqVO);
        if (asset != null) {
            List<AssetsAssetColumnDO> columns = assetsAssetColumnMapper.findByAssetId(asset.getId());
            return new TableSkillContext(asset, columns == null ? new ArrayList<>() : columns);
        }
        CatalogTableRespDTO metadataTable = resolveMetadataTableForSkill(reqVO);
        return new TableSkillContext(toSkillTable(metadataTable), metadataColumns(metadataTable));
    }

    private AssetsAssetDO resolveAssetTableForSkill(AiTableSkillGenerateReqVO reqVO) {
        if (reqVO.getDatasourceId() == null || StringUtils.isBlank(reqVO.getTableName())) {
            return null;
        }
        AssetsTableGovernanceReqDTO governanceReq = new AssetsTableGovernanceReqDTO();
        governanceReq.setDatasourceId(reqVO.getDatasourceId());
        governanceReq.setTableName(reqVO.getTableName());
        governanceReq.setEntrance("AI_ASK_DATA_SKILL");
        AssetsTableGovernanceRespDTO governance = assetsTableGovernanceApiService.resolveTable(governanceReq);
        if (governance == null || governance.getAssetId() == null
                || !AssetsTableGovernanceRespDTO.SOURCE_ASSET.equals(governance.getSource())) {
            return null;
        }
        return assetsAssetMapper.selectById(governance.getAssetId());
    }

    private List<AssetsAssetDO> resolveMetadataTablesForDatasource(Long datasourceId) {
        List<CatalogTableRespDTO> metadataTables = catalogTableApiService.listByDatasourceId(datasourceId);
        List<AssetsAssetDO> tables = new ArrayList<>();
        if (metadataTables == null || metadataTables.isEmpty()) {
            List<AssetsAssetDO> assets = assetsAssetMapper.findByDatasourceId(datasourceId);
            if (assets != null) {
                tables.addAll(assets);
            }
            return tables;
        }
        for (CatalogTableRespDTO metadataTable : metadataTables) {
            if (metadataTable != null && StringUtils.isNotBlank(metadataTable.getTableName())) {
                AssetsAssetDO asset = firstAsset(datasourceId, metadataTable.getTableName());
                tables.add(asset == null ? toSkillTable(metadataTable) : asset);
            }
        }
        return tables;
    }

    private AssetsAssetDO toSkillTable(CatalogTableRespDTO table) {
        AssetsAssetDO skillTable = new AssetsAssetDO();
        skillTable.setId(table.getId());
        skillTable.setName(firstNonBlank(table.getTableComment(), table.getTableName()));
        skillTable.setType("CAT_TABLE");
        skillTable.setDatasourceId(table.getDatasourceId());
        skillTable.setTableId(table.getId());
        skillTable.setTableName(table.getTableName());
        skillTable.setTableComment(table.getTableComment());
        skillTable.setDescription(firstNonBlank(table.getDescription(), table.getTableComment()));
        skillTable.setFieldCount(table.getColumnCount());
        return skillTable;
    }

    private List<AssetsAssetColumnDO> metadataColumns(CatalogTableRespDTO table) {
        List<AssetsAssetColumnDO> columns = new ArrayList<>();
        if (table == null || table.getId() == null) {
            return columns;
        }
        List<CatalogColumnRespDTO> catalogColumns = catalogColumnApiService.listByTableId(table.getId());
        if (catalogColumns == null) {
            return columns;
        }
        for (CatalogColumnRespDTO catalogColumn : catalogColumns) {
            if (catalogColumn == null || StringUtils.isBlank(catalogColumn.getColumnName())) {
                continue;
            }
            AssetsAssetColumnDO column = new AssetsAssetColumnDO(catalogColumn);
            column.setId(firstNonNull(catalogColumn.getId(), catalogColumn.getColumnId()));
            column.setAssetId(table.getId());
            columns.add(column);
        }
        return columns;
    }

    private Map<Long, List<AssetsAssetColumnDO>> collectColumnsForMetadataTables(List<AssetsAssetDO> tables) {
        Map<Long, List<AssetsAssetColumnDO>> columnMap = new LinkedHashMap<>();
        if (tables == null) {
            return columnMap;
        }
        for (AssetsAssetDO table : tables) {
            if (table == null || table.getId() == null) {
                continue;
            }
            if (isRealAssetTable(table)) {
                List<AssetsAssetColumnDO> columns = assetsAssetColumnMapper.findByAssetId(table.getId());
                columnMap.put(table.getId(), columns == null ? new ArrayList<>() : columns);
            } else {
                CatalogTableRespDTO metadataTable = catalogTableApiService.getById(table.getId());
                columnMap.put(table.getId(), metadataColumns(metadataTable));
            }
        }
        return columnMap;
    }

    private AssetsAssetDO firstAsset(Long datasourceId, String tableName) {
        List<AssetsAssetDO> assets = assetsAssetMapper.findByDatasourceIdAndTableName(datasourceId, tableName);
        if (assets == null || assets.isEmpty()) {
            return null;
        }
        return assets.get(0);
    }

    private boolean isRealAssetTable(AssetsAssetDO table) {
        return table != null && !"CAT_TABLE".equals(table.getType());
    }

    private CollectorQualitySummaryRespDTO findQualitySummary(AssetsAssetDO table) {
        if (table == null || table.getDatasourceId() == null || StringUtils.isBlank(table.getTableName())) {
            return null;
        }
        try {
            return collectorQualityTaskApiService.getLatestQualitySummary(table.getDatasourceId(), table.getTableName());
        } catch (Exception ignored) {
            return null;
        }
    }

    private AiSkillDO upsertGeneratedSkill(String skillCode, String skillName, String skillType, String bizObjectType,
                                           Long bizObjectId, String content, boolean publish) {
        AiSkillDO skill = null;
        if (StringUtils.isNotBlank(bizObjectType) && bizObjectId != null) {
            skill = aiSkillMapper.selectByBizObject(bizObjectType, bizObjectId);
            if (skill == null) {
                skill = aiSkillMapper.selectBySkillCode(skillCode);
            }
        } else {
            skill = aiSkillMapper.selectBySkillCode(skillCode);
        }
        if (skill == null) {
            skill = new AiSkillDO();
            skill.setSkillCode(skillCode);
            skill.setSkillName(skillName);
            skill.setSkillType(skillType);
            skill.setStatus(publish ? STATUS_PUBLISHED : STATUS_DRAFT);
            skill.setSourceType(SOURCE_GENERATED);
            skill.setBizObjectType(bizObjectType);
            skill.setBizObjectId(bizObjectId);
            skill.setContent(content);
            skill.setContentHash(hash(content));
            skill.setVersion(1);
            aiSkillMapper.insert(skill);
            insertVersion(skill, "GENERATE", "生成Skill");
            return skill;
        }

        skill.setSkillCode(skillCode);
        skill.setSkillName(skillName);
        skill.setSkillType(skillType);
        skill.setStatus(publish ? STATUS_PUBLISHED : defaultText(skill.getStatus(), STATUS_DRAFT));
        skill.setSourceType(SOURCE_GENERATED);
        skill.setBizObjectType(bizObjectType);
        skill.setBizObjectId(bizObjectId);
        skill.setContent(content);
        skill.setContentHash(hash(content));
        skill.setVersion(nextVersion(skill));
        aiSkillMapper.updateById(skill);
        insertVersion(skill, "GENERATE", "刷新生成Skill");
        return skill;
    }

    private void refreshTableRefs(AiSkillDO skill, AssetsAssetDO asset, List<AssetsAssetColumnDO> columns, AssetsDatasourceDO datasource) {
        aiSkillRefMapper.deleteBySkillId(skill.getId());
        insertRef(skill.getId(), "TABLE", asset.getId(), asset.getTableName(), firstNonBlank(asset.getTableComment(), asset.getName()));
        if (datasource != null) {
            insertRef(skill.getId(), "DATA_SOURCE", datasource.getId(), datasource.getDatasourceType(), datasource.getDatasourceName());
        }
        if (columns != null) {
            for (AssetsAssetColumnDO column : columns) {
                insertRef(skill.getId(), "COLUMN", column.getId(), column.getColumnName(), column.getColumnComment());
            }
        }
    }

    private void refreshDatabaseRefs(AiSkillDO skill, AssetsDatasourceDO datasource, List<AssetsAssetDO> assets) {
        aiSkillRefMapper.deleteBySkillId(skill.getId());
        insertRef(skill.getId(), "DATA_SOURCE", datasource.getId(), datasource.getDatasourceType(), datasource.getDatasourceName());
        if (assets != null) {
            for (AssetsAssetDO asset : assets) {
                insertRef(skill.getId(), "TABLE", asset.getId(), asset.getTableName(), firstNonBlank(asset.getTableComment(), asset.getName()));
            }
        }
    }

    private void refreshMultiTableRefs(AiSkillDO skill, AssetsDatasourceDO datasource, List<AssetsAssetDO> assets,
                                       Map<Long, List<AssetsAssetColumnDO>> columnMap) {
        aiSkillRefMapper.deleteBySkillId(skill.getId());
        insertRef(skill.getId(), "DATA_SOURCE", datasource.getId(), datasource.getDatasourceType(), datasource.getDatasourceName());
        if (assets != null) {
            for (AssetsAssetDO asset : assets) {
                insertRef(skill.getId(), "TABLE", asset.getId(), asset.getTableName(), firstNonBlank(asset.getTableComment(), asset.getName()));
                List<AssetsAssetColumnDO> columns = columnMap == null ? null : columnMap.get(asset.getId());
                if (columns != null) {
                    for (AssetsAssetColumnDO column : columns) {
                        insertRef(skill.getId(), "COLUMN", column.getId(), asset.getTableName() + "." + column.getColumnName(), column.getColumnComment());
                    }
                }
            }
        }
    }

    private void insertRef(Long skillId, String refType, Long refId, String refCode, String refName) {
        AiSkillRefDO ref = new AiSkillRefDO();
        ref.setSkillId(skillId);
        ref.setRefType(refType);
        ref.setRefId(refId);
        ref.setRefCode(refCode);
        ref.setRefName(refName);
        aiSkillRefMapper.insert(ref);
    }

    private SkillEnhancement generateTableEnhancement(AssetsAssetDO asset, AssetsDatasourceDO datasource,
                                                      List<AssetsAssetColumnDO> columns, String manualNotes) {
        SkillEnhancement fallback = new SkillEnhancement();
        if (!aiModelGatewayService.available()) {
            return fallback;
        }
        Set<String> allowedFields = allowedColumnNames(columns);
        if (StringUtils.isNotBlank(asset.getTableName())) {
            allowedFields.add(asset.getTableName());
        }
        allowedFields.add("datamaster-metadata");
        allowedFields.add("datamaster-quality");
        String systemPrompt = "你是 DataMaster 的表级问数 Skill 增强器。"
                + "你只能根据输入的真实元数据补充业务摘要、字段用途、示例问题和注意事项。"
                + "禁止改写或新增表名、字段名、数据源、质量结论。"
                + "只输出 JSON，不要输出 Markdown，不要输出解释。";
        String userPrompt = tableEnhancementPrompt(asset, datasource, columns, manualNotes);
        try {
            String generated = aiModelGatewayService.complete(systemPrompt, userPrompt);
            return parseEnhancement(generated, allowedFields);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "模型调用失败" : e.getMessage();
            fallback.warning = limitText(message, 300);
            return fallback;
        }
    }

    private String tableEnhancementPrompt(AssetsAssetDO asset, AssetsDatasourceDO datasource,
                                          List<AssetsAssetColumnDO> columns, String manualNotes) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请按以下 JSON 结构返回：\n");
        prompt.append("{\"businessSummary\":\"\",");
        prompt.append("\"fieldUsages\":[{\"field\":\"字段名\",\"usage\":\"推荐用途\",\"notice\":\"注意事项\"}],");
        prompt.append("\"metricHints\":[\"指标口径建议\"],");
        prompt.append("\"timeHints\":[\"时间口径建议\"],");
        prompt.append("\"exampleQuestions\":[\"示例问题\"],");
        prompt.append("\"sqlRules\":[\"SQL生成规则\"],");
        prompt.append("\"qualityHints\":[\"质量风险提示\"]}\n\n");
        prompt.append("约束：field 必须从字段清单中选择；不要编造字段；不确定就留空数组。\n\n");
        prompt.append("表信息：\n");
        prompt.append("- 数据源：").append(datasource == null ? "未知" : firstNonBlank(datasource.getDatasourceName(), datasource.getDatasourceType())).append("\n");
        prompt.append("- 数据源类型：").append(datasource == null ? "未知" : nullToEmpty(datasource.getDatasourceType())).append("\n");
        prompt.append("- 表名：").append(nullToEmpty(asset.getTableName())).append("\n");
        prompt.append("- 表中文名/注释：").append(firstNonBlank(asset.getTableComment(), asset.getName())).append("\n");
        prompt.append("- 表描述：").append(nullToEmpty(asset.getDescription())).append("\n\n");
        prompt.append("字段清单：\n");
        if (columns != null) {
            for (AssetsAssetColumnDO column : columns) {
                prompt.append("- ")
                        .append(nullToEmpty(column.getColumnName()))
                        .append(" | ").append(nullToEmpty(column.getColumnType()))
                        .append(" | ").append(nullToEmpty(column.getColumnComment()))
                        .append(" | ").append(nullToEmpty(column.getDescription()))
                        .append("\n");
            }
        }
        prompt.append("\n人工维护备注：\n").append(defaultText(manualNotes, "暂无人工维护备注。"));
        return prompt.toString();
    }

    private SkillEnhancement parseEnhancement(String generated, Set<String> allowedFields) {
        SkillEnhancement enhancement = new SkillEnhancement();
        String json = extractJson(generated);
        if (StringUtils.isBlank(json)) {
            enhancement.warning = "模型未返回JSON增强内容";
            return enhancement;
        }
        try {
            JSONObject object = JSON.parseObject(json);
            enhancement.businessSummary = safeText(object.getString("businessSummary"), allowedFields);
            enhancement.metricHints = safeList(object.getJSONArray("metricHints"), allowedFields);
            enhancement.timeHints = safeList(object.getJSONArray("timeHints"), allowedFields);
            enhancement.exampleQuestions = safeList(object.getJSONArray("exampleQuestions"), allowedFields);
            enhancement.sqlRules = safeList(object.getJSONArray("sqlRules"), allowedFields);
            enhancement.qualityHints = safeList(object.getJSONArray("qualityHints"), allowedFields);
            JSONArray fieldUsages = object.getJSONArray("fieldUsages");
            if (fieldUsages != null) {
                for (int i = 0; i < fieldUsages.size() && enhancement.fieldUsages.size() < MAX_AI_LIST_SIZE; i++) {
                    JSONObject item = fieldUsages.getJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    String field = firstNonBlank(item.getString("field"), item.getString("fieldName"), item.getString("columnName"));
                    if (!allowedFields.contains(field)) {
                        continue;
                    }
                    FieldEnhancement fieldEnhancement = new FieldEnhancement();
                    fieldEnhancement.field = field;
                    fieldEnhancement.usage = safeText(item.getString("usage"), allowedFields);
                    fieldEnhancement.notice = safeText(item.getString("notice"), allowedFields);
                    enhancement.fieldUsages.add(fieldEnhancement);
                }
            }
        } catch (Exception e) {
            enhancement.warning = "模型增强JSON解析失败";
        }
        return enhancement;
    }

    private void insertVersion(AiSkillDO skill, String changeType, String changeRemark) {
        AiSkillVersionDO version = new AiSkillVersionDO();
        version.setSkillId(skill.getId());
        version.setVersion(skill.getVersion());
        version.setContent(skill.getContent());
        version.setChangeType(changeType);
        version.setChangeRemark(changeRemark);
        aiSkillVersionMapper.insert(version);
    }

    private AiSkillDO requireSkill(Long id) {
        if (id == null) {
            throw new ServiceException("Skill ID不能为空");
        }
        AiSkillDO skill = aiSkillMapper.selectById(id);
        if (skill == null) {
            throw new ServiceException("Skill不存在");
        }
        return skill;
    }

    private Integer nextVersion(AiSkillDO skill) {
        return skill.getVersion() == null ? 1 : skill.getVersion() + 1;
    }

    private String resolveUpdateSourceType(AiSkillDO old, AiSkillSaveReqVO saveReqVO) {
        if (StringUtils.isNotBlank(saveReqVO.getSourceType())) {
            return saveReqVO.getSourceType();
        }
        if (SOURCE_GENERATED.equals(old.getSourceType()) || SOURCE_GENERATED_EDITED.equals(old.getSourceType())) {
            return SOURCE_GENERATED_EDITED;
        }
        return defaultText(old.getSourceType(), SOURCE_MANUAL);
    }

    private String resolveManualNotes(AiSkillDO oldSkill, AiTableSkillGenerateReqVO reqVO) {
        return resolveManualNotes(oldSkill, reqVO == null ? "" : reqVO.getManualNotes());
    }

    private String resolveManualNotes(AiSkillDO oldSkill, String manualNotes) {
        if (StringUtils.isNotBlank(manualNotes)) {
            return manualNotes;
        }
        if (oldSkill == null || StringUtils.isBlank(oldSkill.getContent())) {
            return "暂无人工维护备注。";
        }
        String content = oldSkill.getContent();
        int begin = content.indexOf(MANUAL_BEGIN);
        int end = content.indexOf(MANUAL_END);
        if (begin >= 0 && end > begin) {
            return content.substring(begin + MANUAL_BEGIN.length(), end).trim();
        }
        return "暂无人工维护备注。";
    }

    private String tableSkillCode(AssetsDatasourceDO datasource, AssetsAssetDO asset) {
        List<String> parts = new ArrayList<>();
        parts.add("table");
        if (datasource != null) {
            parts.add(firstNonBlank(datasource.getDatasourceName(), datasource.getDatasourceType(), String.valueOf(datasource.getId())));
        } else if (asset.getDatasourceId() != null) {
            parts.add(String.valueOf(asset.getDatasourceId()));
        }
        parts.add(firstNonBlank(asset.getTableName(), asset.getName(), String.valueOf(asset.getId())));
        return normalizeCode(join(parts, "-"));
    }

    private String databaseSkillCode(AssetsDatasourceDO datasource) {
        List<String> parts = new ArrayList<>();
        parts.add("database");
        parts.add(firstNonBlank(datasource.getDatasourceName(), datasource.getDatasourceType(), String.valueOf(datasource.getId())));
        return normalizeCode(join(parts, "-"));
    }

    private String multiTableSkillCode(AssetsDatasourceDO datasource, List<AssetsAssetDO> assets) {
        List<String> parts = new ArrayList<>();
        parts.add("multi-table");
        parts.add(firstNonBlank(datasource.getDatasourceName(), datasource.getDatasourceType(), String.valueOf(datasource.getId())));
        if (assets != null) {
            for (AssetsAssetDO asset : assets) {
                parts.add(firstNonBlank(asset.getTableName(), asset.getName(), String.valueOf(asset.getId())));
            }
        }
        return normalizeCode(join(parts, "-"));
    }

    private String multiTableSkillName(List<AssetsAssetDO> assets) {
        List<String> names = new ArrayList<>();
        if (assets != null) {
            for (AssetsAssetDO asset : assets) {
                names.add(firstNonBlank(asset.getTableComment(), asset.getName(), asset.getTableName()));
                if (names.size() >= 3) {
                    break;
                }
            }
        }
        return join(names, "、");
    }

    private String tableSkillContent(String skillCode, AssetsAssetDO asset, AssetsDatasourceDO datasource,
                                     List<AssetsAssetColumnDO> columns, String manualNotes, SkillEnhancement enhancement,
                                     CollectorQualitySummaryRespDTO qualitySummary) {
        String title = firstNonBlank(asset.getTableComment(), asset.getName(), asset.getTableName());
        StringBuilder fieldRows = new StringBuilder();
        StringBuilder timeFields = new StringBuilder();
        StringBuilder metricFields = new StringBuilder();
        enhancement = enhancement == null ? new SkillEnhancement() : enhancement;
        if (columns != null && !columns.isEmpty()) {
            for (AssetsAssetColumnDO column : columns) {
                FieldEnhancement fieldEnhancement = enhancement.findField(column.getColumnName());
                String usage = firstNonBlank(fieldEnhancement == null ? "" : fieldEnhancement.usage, recommendUsage(column));
                String notice = joinUnique(columnNotice(column), fieldEnhancement == null ? "" : fieldEnhancement.notice);
                fieldRows.append("| ")
                        .append(nullToEmpty(column.getColumnName())).append(" | ")
                        .append(nullToEmpty(column.getColumnType())).append(" | ")
                        .append(nullToEmpty(column.getColumnComment())).append(" | ")
                        .append(escapeTableCell(usage)).append(" | ")
                        .append(escapeTableCell(notice)).append(" |\n");
                if (isTimeField(column)) {
                    timeFields.append("- `").append(column.getColumnName()).append("`：").append(nullToEmpty(column.getColumnComment())).append("\n");
                }
                if (isMetricField(column)) {
                    metricFields.append("- `").append(column.getColumnName()).append("`：").append(nullToEmpty(column.getColumnComment())).append("\n");
                }
            }
        } else {
            fieldRows.append("| 暂无字段 |  |  |  | 需要先完成元数据采集 |\n");
        }
        if (timeFields.length() == 0) {
            timeFields.append("- 暂未识别到明显时间字段，问数时需要用户确认时间口径。\n");
        }
        if (metricFields.length() == 0) {
            metricFields.append("- 暂未识别到明显指标字段，涉及统计口径时需要用户确认。\n");
        }
        String description = firstNonBlank(asset.getDescription(), "由元数据管理自动生成，业务说明需要数据负责人补充。");
        String qualitySection = renderQualitySummary(qualitySummary);

        return "---\n"
                + "name: " + skillCode + "\n"
                + "description: " + title + " 问数 Skill。用于理解表结构、字段含义、常用指标、时间口径和质量风险。\n"
                + "---\n\n"
                + "# " + title + "\n\n"
                + "## 表身份\n\n"
                + "- 数据源：" + (datasource == null ? "未知" : firstNonBlank(datasource.getDatasourceName(), datasource.getDatasourceType())) + "\n"
                + "- 数据源类型：" + (datasource == null ? "未知" : nullToEmpty(datasource.getDatasourceType())) + "\n"
                + "- 表名：" + nullToEmpty(asset.getTableName()) + "\n"
                + "- 元数据表ID：" + asset.getId() + "\n"
                + "- 字段数：" + (asset.getFieldCount() == null ? "" : asset.getFieldCount()) + "\n\n"
                + "## 业务说明\n\n"
                + description + "\n\n"
                + (StringUtils.isBlank(enhancement.businessSummary) ? "" : "AI增强摘要：" + enhancement.businessSummary + "\n\n")
                + "## 字段说明\n\n"
                + "| 字段 | 类型 | 说明 | 推荐用途 | 注意事项 |\n"
                + "| --- | --- | --- | --- | --- |\n"
                + fieldRows
                + "\n## 常用指标口径\n\n"
                + metricFields
                + renderList(enhancement.metricHints)
                + "\n## 常用时间字段\n\n"
                + timeFields
                + renderList(enhancement.timeHints)
                + "\n## 关联表\n\n"
                + "- 暂未自动生成关联表。后续可接入血缘接口补充上游、下游和常用关联键。\n\n"
                + "## 质量核检结论\n\n"
                + qualitySection
                + renderList(enhancement.qualityHints)
                + "## SQL 生成规则\n\n"
                + "- 只生成只读查询 SQL。\n"
                + "- 涉及时间范围时优先使用本 Skill 推荐的时间字段。\n"
                + "- 涉及金额、数量、状态枚举时必须参考字段说明和指标口径。\n"
                + "- 未确认口径时先给出待确认问题，不直接给确定性业务结论。\n"
                + "- 如字段缺少注释或表缺少业务说明，回答中要标记为待确认。\n\n"
                + renderList(enhancement.sqlRules)
                + "## 示例问题\n\n"
                + "- 这张表最近一个月有多少条数据？\n"
                + "- 按天统计这张表的记录数趋势。\n"
                + "- 这张表有哪些字段适合做指标统计？\n\n"
                + renderList(enhancement.exampleQuestions)
                + "## 人工维护备注\n\n"
                + MANUAL_BEGIN + "\n"
                + defaultText(manualNotes, "暂无人工维护备注。") + "\n"
                + MANUAL_END + "\n"
                + renderWarning(enhancement.warning);
    }

    private String databaseSkillContent(String skillCode, AssetsDatasourceDO datasource, List<AssetsAssetDO> assets,
                                        Map<Long, List<AssetsAssetColumnDO>> columnMap, String manualNotes) {
        String title = firstNonBlank(datasource.getDatasourceName(), datasource.getDatasourceType(), String.valueOf(datasource.getId()));
        StringBuilder tableRows = new StringBuilder();
        int index = 0;
        for (AssetsAssetDO asset : assets) {
            index++;
            List<AssetsAssetColumnDO> columns = columnMap.get(asset.getId());
            tableRows.append("| ")
                    .append(index).append(" | ")
                    .append(nullToEmpty(asset.getTableName())).append(" | ")
                    .append(nullToEmpty(firstNonBlank(asset.getTableComment(), asset.getName()))).append(" | ")
                    .append(columns == null ? 0 : columns.size()).append(" | ")
                    .append(escapeTableCell(firstNonBlank(asset.getDescription(), ""))).append(" |\n");
        }
        return "---\n"
                + "name: " + skillCode + "\n"
                + "description: " + title + " 整库问数 Skill。用于理解数据库主题、核心表、公共字段、跨表问数边界和质量风险。\n"
                + "---\n\n"
                + "# " + title + " 整库问数 Skill\n\n"
                + "## 数据库身份\n\n"
                + "- 数据源：" + title + "\n"
                + "- 数据源类型：" + nullToEmpty(datasource.getDatasourceType()) + "\n"
                + "- 数据源ID：" + datasource.getId() + "\n"
                + "- 表数量：" + assets.size() + "\n\n"
                + "## 数据库主题说明\n\n"
                + "- 本 Skill 直接根据 DataMaster 元数据管理中的表字段生成。\n"
                + "- 用于普通问数时优先帮助定位候选表；如果候选表不唯一，应先让用户确认业务主题或表范围。\n\n"
                + "## 表清单\n\n"
                + "| 序号 | 表名 | 表说明 | 字段数 | 描述 |\n"
                + "| --- | --- | --- | --- | --- |\n"
                + tableRows
                + "\n## 公共时间字段\n\n"
                + renderCommonFields(columnMap, true, false)
                + "## 公共指标字段\n\n"
                + renderCommonFields(columnMap, false, true)
                + "## 问数规则\n\n"
                + "- 用户问题没有明确表名时，先根据表名、表注释、字段注释和本 Skill 的表清单定位候选表。\n"
                + "- 如果涉及多表关联，优先使用多表 Skill；缺少多表 Skill 时需要说明关联路径待确认。\n"
                + "- 字段缺少注释或表说明不足时，需要在回答中提示口径不确定。\n"
                + "- SQL 只在用户或请求参数明确要求时返回。\n\n"
                + "## 人工维护备注\n\n"
                + MANUAL_BEGIN + "\n"
                + defaultText(manualNotes, "暂无人工维护备注。") + "\n"
                + MANUAL_END + "\n";
    }

    private String multiTableSkillContent(String skillCode, AssetsDatasourceDO datasource, List<AssetsAssetDO> assets,
                                          Map<Long, List<AssetsAssetColumnDO>> columnMap, String manualNotes) {
        String title = multiTableSkillName(assets);
        StringBuilder tableSections = new StringBuilder();
        for (AssetsAssetDO asset : assets) {
            tableSections.append("### ").append(firstNonBlank(asset.getTableComment(), asset.getName(), asset.getTableName())).append("\n\n")
                    .append("- 表名：").append(nullToEmpty(asset.getTableName())).append("\n")
                    .append("- 表说明：").append(firstNonBlank(asset.getDescription(), asset.getTableComment(), asset.getName(), "")).append("\n\n")
                    .append("| 字段 | 类型 | 说明 | 推荐用途 | 注意事项 |\n")
                    .append("| --- | --- | --- | --- | --- |\n")
                    .append(fieldRows(columnMap.get(asset.getId())))
                    .append("\n");
        }
        return "---\n"
                + "name: " + skillCode + "\n"
                + "description: " + title + " 多表问数 Skill。用于理解一组表的业务主题、字段口径、推荐关联和跨表统计注意事项。\n"
                + "---\n\n"
                + "# " + title + " 多表问数 Skill\n\n"
                + "## 数据源\n\n"
                + "- 数据源：" + firstNonBlank(datasource.getDatasourceName(), datasource.getDatasourceType()) + "\n"
                + "- 数据源类型：" + nullToEmpty(datasource.getDatasourceType()) + "\n"
                + "- 涉及表数：" + assets.size() + "\n\n"
                + "## 主题说明\n\n"
                + "- 本 Skill 由用户选择的多张表生成，适合跨表统计、关联查询和报告数据准备。\n"
                + "- 如果实际业务关联键不在自动识别结果中，需要以人工维护备注或用户补充为准。\n\n"
                + "## 涉及表和字段\n\n"
                + tableSections
                + "## 推荐关联线索\n\n"
                + renderRelationHints(assets, columnMap)
                + "## 跨表问数规则\n\n"
                + "- 生成跨表 SQL 前必须确认主表、统计粒度和关联键。\n"
                + "- 同名字段不一定可以直接关联；需要结合字段说明、主键标记和业务语义判断。\n"
                + "- 事实表与维表无法确定时，应在回答中说明假设。\n"
                + "- 多表统计必须避免重复计数，必要时先按主键去重或先聚合再关联。\n\n"
                + "## 人工维护备注\n\n"
                + MANUAL_BEGIN + "\n"
                + defaultText(manualNotes, "暂无人工维护备注。") + "\n"
                + MANUAL_END + "\n";
    }

    private String metadataSkillContent() {
        return "---\n"
                + "name: datamaster-metadata\n"
                + "description: DataMaster 元数据采集能力说明。用于 AI 问数、表结构查找、字段含义理解和数据源定位。\n"
                + "---\n\n"
                + "# DataMaster 元数据能力\n\n"
                + "## 能力边界\n\n"
                + "- 通过元数据管理能力获取数据源、库、表、字段、字段类型、字段注释和质量标记。\n"
                + "- 问数前必须优先定位候选表，再定位候选字段。\n\n"
                + "## 核心接口\n\n"
                + "- `/cat/table/list`：分页检索元数据表。\n"
                + "- `/cat/table/{id}`：获取元数据表详情。\n"
                + "- `/cat/column/list`：按元数据表查询字段。\n"
                + "- `/ast/discoveryTask/list`、`/ast/discoveryTaskLog/list`：查询元数据发现任务和日志。\n\n"
                + "## 问数流程\n\n"
                + "1. 从用户问题抽取业务主题、指标词、时间词和过滤条件。\n"
                + "2. 检索元数据表名、表注释和字段注释，得到候选表。\n"
                + "3. 如果候选表不唯一，先让用户确认。\n"
                + "4. 查询字段列表，识别时间字段、维度字段、指标字段和枚举字段。\n"
                + "5. 结合表级 Skill 生成 SQL。\n\n"
                + "## 注意事项\n\n"
                + "- 不要使用用户无权限访问的资产。\n"
                + "- 字段缺少注释时需要在回答中提示口径不确定。\n";
    }

    private String qualitySkillContent() {
        return "---\n"
                + "name: datamaster-quality\n"
                + "description: DataMaster 数据质量核检能力说明。用于 AI 问数前检查表和字段质量状态、解释质量报告、识别异常数据风险、生成质量可信度提示。\n"
                + "---\n\n"
                + "# DataMaster 质量核检能力\n\n"
                + "## 能力边界\n\n"
                + "- 查询质量任务、质量规则、执行日志、异常数据和核检结果。\n"
                + "- 判断问数所用表和字段最近是否存在质量风险。\n\n"
                + "## 核心接口\n\n"
                + "- `/ast/quality/qualityTask/list`：查询质量任务。\n"
                + "- `/ast/quality/qualityTaskLog/list`：查询质量执行日志。\n"
                + "- `/col/qualityLog/list`：查询采集侧质量日志。\n"
                + "- `/col/evaluateLog/pageErrorData`：查询问题数据。\n\n"
                + "## 风险解释规则\n\n"
                + "- 最近核检失败：回答必须提示该表数据可能不可信。\n"
                + "- 最近无核检记录：回答必须提示未找到近期质量报告。\n"
                + "- 存在空值、唯一性、范围、枚举、时间顺序等规则失败：回答必须说明影响的字段和规则类型。\n\n"
                + "## 问数回答要求\n\n"
                + "- 输出结果时展示最近核检时间、核检状态和主要异常。\n"
                + "- 质量状态不明时，不要给出绝对确定的业务判断。\n";
    }

    private boolean isTimeField(AssetsAssetColumnDO column) {
        String text = lower(column.getColumnName() + " " + column.getColumnComment() + " " + column.getColumnType());
        return text.contains("time") || text.contains("date") || text.contains("day")
                || text.contains("时间") || text.contains("日期");
    }

    private boolean isMetricField(AssetsAssetColumnDO column) {
        String text = lower(column.getColumnName() + " " + column.getColumnComment());
        return text.contains("amount") || text.contains("price") || text.contains("qty") || text.contains("quantity")
                || text.contains("count") || text.contains("num") || text.contains("金额") || text.contains("数量")
                || text.contains("价格") || text.contains("总数");
    }

    private String recommendUsage(AssetsAssetColumnDO column) {
        if (isTimeField(column)) {
            return "时间过滤/趋势分析";
        }
        if (isMetricField(column)) {
            return "指标统计";
        }
        if ("1".equals(column.getPkFlag())) {
            return "主键/去重";
        }
        return "维度过滤/结果展示";
    }

    private String columnNotice(AssetsAssetColumnDO column) {
        List<String> notices = new ArrayList<>();
        if ("1".equals(column.getPkFlag())) {
            notices.add("主键");
        }
        if ("1".equals(column.getNullableFlag())) {
            notices.add("可为空");
        }
        if (StringUtils.isBlank(column.getColumnComment())) {
            notices.add("缺少字段注释");
        }
        return notices.isEmpty() ? "" : join(notices, "；");
    }

    private Set<String> allowedColumnNames(List<AssetsAssetColumnDO> columns) {
        Set<String> names = new HashSet<>();
        if (columns == null) {
            return names;
        }
        for (AssetsAssetColumnDO column : columns) {
            if (column != null && StringUtils.isNotBlank(column.getColumnName())) {
                names.add(column.getColumnName());
            }
        }
        return names;
    }

    private String extractJson(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String value = text.trim();
        int fenceStart = value.indexOf("```");
        if (fenceStart >= 0) {
            int contentStart = value.indexOf('\n', fenceStart);
            int fenceEnd = value.indexOf("```", contentStart + 1);
            if (contentStart > fenceStart && fenceEnd > contentStart) {
                value = value.substring(contentStart + 1, fenceEnd).trim();
            }
        }
        int begin = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (begin >= 0 && end > begin) {
            return value.substring(begin, end + 1);
        }
        return "";
    }

    private List<String> safeList(JSONArray array, Set<String> allowedFields) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.size() && values.size() < MAX_AI_LIST_SIZE; i++) {
            String value = safeText(array.getString(i), allowedFields);
            if (StringUtils.isNotBlank(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private String safeText(String text, Set<String> allowedFields) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String value = text.replace("\r", " ").replace("\n", " ").replace("|", "/").trim();
        value = value.replaceAll("\\s+", " ");
        if (containsUnknownBacktickField(value, allowedFields)) {
            return "";
        }
        return limitText(value, MAX_AI_TEXT_LENGTH);
    }

    private boolean containsUnknownBacktickField(String text, Set<String> allowedFields) {
        Matcher matcher = BACKTICK_TOKEN_PATTERN.matcher(defaultText(text, ""));
        while (matcher.find()) {
            String token = matcher.group(1);
            if (StringUtils.isNotBlank(token) && looksLikeIdentifier(token) && !allowedFields.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeIdentifier(String value) {
        return value.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private String limitText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private String renderList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                builder.append("- ").append(value).append("\n");
            }
        }
        return builder.length() == 0 ? "" : builder.append("\n").toString();
    }

    private String renderQualitySummary(CollectorQualitySummaryRespDTO summary) {
        if (summary == null) {
            return "- 未获取到该表最近质量核检结果，回答时必须提示数据可信度风险。\n\n";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("- 质量任务：").append(firstNonBlank(summary.getTaskName(), summary.getTaskId() == null ? "" : String.valueOf(summary.getTaskId()), "未知")).append("\n");
        if (summary.getLogId() != null) {
            builder.append("- 最近日志：").append(firstNonBlank(summary.getLogName(), String.valueOf(summary.getLogId()))).append("\n");
        }
        builder.append("- 最近状态：").append(firstNonBlank(summary.getSuccessFlag(), "未知")).append("\n");
        if (summary.getStartTime() != null || summary.getEndTime() != null) {
            builder.append("- 执行时间：")
                    .append(summary.getStartTime() == null ? "" : summary.getStartTime())
                    .append(summary.getEndTime() == null ? "" : " ~ " + summary.getEndTime())
                    .append("\n");
        }
        if (summary.getScore() != null) {
            builder.append("- 评分：").append(summary.getScore()).append("\n");
        }
        if (summary.getProblemData() != null) {
            builder.append("- 问题数据量：").append(summary.getProblemData()).append("\n");
        }
        if (summary.getRules() != null && !summary.getRules().isEmpty()) {
            builder.append("\n| 规则 | 字段 | 类型 | 维度 | 等级 | 状态 | 异常说明 | 修复建议 |\n");
            builder.append("| --- | --- | --- | --- | --- | --- | --- | --- |\n");
            for (CollectorQualitySummaryRespDTO.Rule rule : summary.getRules()) {
                builder.append("| ")
                        .append(escapeTableCell(firstNonBlank(rule.getRuleName(), rule.getName()))).append(" | ")
                        .append(escapeTableCell(rule.getEvaColumn())).append(" | ")
                        .append(escapeTableCell(rule.getRuleType())).append(" | ")
                        .append(escapeTableCell(rule.getDimensionType())).append(" | ")
                        .append(escapeTableCell(rule.getWarningLevel())).append(" | ")
                        .append(escapeTableCell(rule.getStatus())).append(" | ")
                        .append(escapeTableCell(rule.getErrDescription())).append(" | ")
                        .append(escapeTableCell(rule.getSuggestion())).append(" |\n");
            }
        }
        builder.append("\n- 如果最近状态失败、评分偏低或问题数据量大于 0，回答必须提示数据可信度风险。\n\n");
        return builder.toString();
    }

    private String fieldRows(List<AssetsAssetColumnDO> columns) {
        if (columns == null || columns.isEmpty()) {
            return "| 暂无字段 |  |  |  | 需要先完成元数据采集 |\n";
        }
        StringBuilder rows = new StringBuilder();
        for (AssetsAssetColumnDO column : columns) {
            rows.append("| ")
                    .append(nullToEmpty(column.getColumnName())).append(" | ")
                    .append(nullToEmpty(column.getColumnType())).append(" | ")
                    .append(nullToEmpty(column.getColumnComment())).append(" | ")
                    .append(escapeTableCell(recommendUsage(column))).append(" | ")
                    .append(escapeTableCell(columnNotice(column))).append(" |\n");
        }
        return rows.toString();
    }

    private String renderCommonFields(Map<Long, List<AssetsAssetColumnDO>> columnMap, boolean timeOnly, boolean metricOnly) {
        StringBuilder builder = new StringBuilder();
        if (columnMap != null) {
            for (List<AssetsAssetColumnDO> columns : columnMap.values()) {
                if (columns == null) {
                    continue;
                }
                for (AssetsAssetColumnDO column : columns) {
                    if (timeOnly && !isTimeField(column)) {
                        continue;
                    }
                    if (metricOnly && !isMetricField(column)) {
                        continue;
                    }
                    builder.append("- `").append(column.getColumnName()).append("`：")
                            .append(firstNonBlank(column.getColumnComment(), column.getColumnType(), "待补充字段说明"))
                            .append("\n");
                    if (builder.length() > 1200) {
                        return builder.append("\n").toString();
                    }
                }
            }
        }
        if (builder.length() == 0) {
            builder.append("- 暂未从字段名或字段注释中识别到明显字段，需要问数时确认口径。\n");
        }
        return builder.append("\n").toString();
    }

    private String renderRelationHints(List<AssetsAssetDO> assets, Map<Long, List<AssetsAssetColumnDO>> columnMap) {
        Map<String, List<String>> fieldOwners = new LinkedHashMap<>();
        if (assets != null) {
            for (AssetsAssetDO asset : assets) {
                List<AssetsAssetColumnDO> columns = columnMap == null ? null : columnMap.get(asset.getId());
                if (columns == null) {
                    continue;
                }
                for (AssetsAssetColumnDO column : columns) {
                    String field = column.getColumnName();
                    if (StringUtils.isBlank(field)) {
                        continue;
                    }
                    String key = field.toLowerCase(Locale.ROOT);
                    if (!fieldOwners.containsKey(key)) {
                        fieldOwners.put(key, new ArrayList<>());
                    }
                    fieldOwners.get(key).add(asset.getTableName() + "." + field);
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : fieldOwners.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            String key = entry.getKey();
            boolean looksLikeKey = key.endsWith("id") || key.endsWith("_id") || key.contains("code")
                    || key.contains("no") || key.contains("编码") || key.contains("编号");
            if (!looksLikeKey) {
                continue;
            }
            builder.append("- 同名/疑似关联字段 `").append(key).append("`：")
                    .append(join(entry.getValue(), "、")).append("\n");
        }
        if (builder.length() == 0) {
            builder.append("- 暂未自动识别到稳定关联键，需要由用户或人工备注确认 join 条件。\n");
        }
        return builder.append("\n").toString();
    }

    private String renderWarning(String warning) {
        if (StringUtils.isBlank(warning)) {
            return "";
        }
        return "\n<!-- AI_SKILL_ENHANCEMENT_WARNING: " + warning.replace("--", "- -") + " -->\n";
    }

    private String joinUnique(String left, String right) {
        if (StringUtils.isBlank(left)) {
            return defaultText(right, "");
        }
        if (StringUtils.isBlank(right) || left.contains(right)) {
            return left;
        }
        return left + "；" + right;
    }

    private String escapeTableCell(String value) {
        return defaultText(value, "").replace("|", "/").replace("\r", " ").replace("\n", " ");
    }

    private String normalizeCode(String text) {
        String value = lower(text).replaceAll("[^a-z0-9]+", "-");
        value = value.replaceAll("^-+", "").replaceAll("-+$", "");
        return StringUtils.isBlank(value) ? "table-skill" : value;
    }

    private String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(defaultText(content, "").getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new ServiceException("计算Skill内容哈希失败");
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private Long firstNonNull(Long... values) {
        if (values == null) {
            return null;
        }
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String lower(String value) {
        return nullToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static class TableSkillContext {
        private AssetsAssetDO asset;
        private List<AssetsAssetColumnDO> columns;

        private TableSkillContext(AssetsAssetDO asset, List<AssetsAssetColumnDO> columns) {
            this.asset = asset;
            this.columns = columns;
        }
    }

    private static class SkillEnhancement {
        private String businessSummary;
        private String warning;
        private List<FieldEnhancement> fieldUsages = new ArrayList<>();
        private List<String> metricHints = new ArrayList<>();
        private List<String> timeHints = new ArrayList<>();
        private List<String> exampleQuestions = new ArrayList<>();
        private List<String> sqlRules = new ArrayList<>();
        private List<String> qualityHints = new ArrayList<>();

        private FieldEnhancement findField(String field) {
            if (StringUtils.isBlank(field)) {
                return null;
            }
            for (FieldEnhancement fieldEnhancement : fieldUsages) {
                if (field.equals(fieldEnhancement.field)) {
                    return fieldEnhancement;
                }
            }
            return null;
        }
    }

    private static class FieldEnhancement {
        private String field;
        private String usage;
        private String notice;
    }
}
