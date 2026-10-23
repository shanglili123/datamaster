package com.datamaster.module.governance.service.warehouse.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingReqDTO;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingRespDTO;
import com.datamaster.module.governance.dal.dataobject.businessCategory.ModelingBusinessCategoryDO;
import com.datamaster.module.governance.dal.dataobject.businessCategory.ModelingBusinessDomainRelDO;
import com.datamaster.module.governance.dal.dataobject.dm.ModelingDataDomainDO;
import com.datamaster.module.governance.dal.dataobject.dm.ModelingDataLayerDO;
import com.datamaster.module.governance.dal.dataobject.dm.ModelingThemeDomainDO;
import com.datamaster.module.governance.dal.mapper.businessCategory.ModelingBusinessCategoryMapper;
import com.datamaster.module.governance.dal.mapper.businessCategory.ModelingBusinessDomainRelMapper;
import com.datamaster.module.governance.dal.mapper.dm.ModelingDataDomainMapper;
import com.datamaster.module.governance.dal.mapper.dm.ModelingDataLayerMapper;
import com.datamaster.module.governance.dal.mapper.dm.ModelingThemeDomainMapper;
import com.datamaster.module.governance.service.warehouse.IWarehouseTableNamingService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Single naming authority shared by logical models and physical asset creation. */
@Service
public class WarehouseTableNamingServiceImpl implements IWarehouseTableNamingService {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    @Resource
    private ModelingDataLayerMapper dataLayerMapper;
    @Resource
    private ModelingDataDomainMapper dataDomainMapper;
    @Resource
    private ModelingThemeDomainMapper themeDomainMapper;
    @Resource
    private ModelingBusinessCategoryMapper businessCategoryMapper;
    @Resource
    private ModelingBusinessDomainRelMapper businessDomainRelMapper;

    @Override
    public WarehouseTableNamingRespDTO resolveAndValidate(WarehouseTableNamingReqDTO req) {
        if (req == null) {
            throw new ServiceException("数仓命名参数不能为空");
        }
        if (!"1".equals(req.getTableType()) && !"2".equals(req.getTableType())
                && !"3".equals(req.getTableType()) && !"4".equals(req.getTableType())) {
            throw new ServiceException("表类型必须为1、2、3或4");
        }
        ModelingDataLayerDO layer = req.getDataLayerId() == null ? null : dataLayerMapper.selectById(req.getDataLayerId());
        requireValid(layer != null, layer == null ? null : layer.getValidFlag(), "数仓分层");
        String layerPart = identifier(layer.getEngName(), "数仓分层英文缩写");
        String base = identifier(req.getBaseTableName(), "基础表名");
        String tableCase = normalizeCase(req.getTableCase());
        ModelingBusinessCategoryDO category = req.getBusinessCategoryId() == null ? null
                : businessCategoryMapper.selectById(req.getBusinessCategoryId());
        if (req.getBusinessCategoryId() != null) {
            requireValid(category != null, category == null ? null : category.getValidFlag(), "业务分类");
            sameSpace(layer.getSpaceId(), layer.getSpaceCode(), category.getSpaceId(), category.getSpaceCode());
        }
        String domainPart;
        if ("4".equals(req.getTableType())) {
            ModelingThemeDomainDO theme = req.getThemeDomainId() == null ? null : themeDomainMapper.selectById(req.getThemeDomainId());
            requireValid(theme != null, theme == null ? null : theme.getValidFlag(), "应用表主题");
            if (!Objects.equals(theme.getDataLayerId(), layer.getId())) {
                throw new ServiceException("应用表主题不属于所选数仓分层");
            }
            sameSpace(layer.getSpaceId(), layer.getSpaceCode(), theme.getSpaceId(), theme.getSpaceCode());
            domainPart = identifier(theme.getEngName(), "主题英文缩写");
        } else {
            ModelingDataDomainDO domain = req.getDataDomainId() == null ? null : dataDomainMapper.selectById(req.getDataDomainId());
            requireValid(domain != null, domain == null ? null : domain.getValidFlag(), "数据域");
            sameSpace(layer.getSpaceId(), layer.getSpaceCode(), domain.getSpaceId(), domain.getSpaceCode());
            domainPart = identifier(domain.getEngName(), "数据域英文缩写");
            if (category != null) {
                List<ModelingBusinessDomainRelDO> relations = businessDomainRelMapper.selectList(
                        Wrappers.lambdaQuery(ModelingBusinessDomainRelDO.class)
                                .eq(ModelingBusinessDomainRelDO::getBusinessCategoryId, category.getId())
                                .eq(ModelingBusinessDomainRelDO::getDataDomainId, domain.getId()));
                boolean related = relations.stream().anyMatch(rel -> Boolean.TRUE.equals(rel.getValidFlag())
                        && spacesMatch(layer.getSpaceId(), layer.getSpaceCode(), rel.getSpaceId(), rel.getSpaceCode()));
                // Preserve the existing direct domainId association as well as the current relation table.
                if (!related && !Objects.equals(category.getDomainId(), domain.getId())) {
                    throw new ServiceException("业务分类与所选数据域未建立有效关联");
                }
            }
        }
        String prefix = layerPart + "_" + domainPart + "_";
        // A complete existing prefix is idempotent; do not strip arbitrary partial tokens.
        String tableName = base.regionMatches(true, 0, prefix, 0, prefix.length()) ? base : prefix + base;
        tableName = "1".equals(tableCase) ? tableName.toUpperCase(Locale.ROOT) : tableName.toLowerCase(Locale.ROOT);
        int limit = identifierLimit(req.getDatasourceType());
        if (tableName.length() > limit) {
            throw new ServiceException("确定物理表名长度不能超过" + limit + "个字符，请缩短英文缩写或基础表名");
        }
        WarehouseTableNamingRespDTO result = describe(req);
        result.setTableCase(tableCase);
        result.setTableName(tableName);
        return result;
    }

    @Override
    public WarehouseTableNamingRespDTO describe(WarehouseTableNamingReqDTO req) {
        WarehouseTableNamingRespDTO result = new WarehouseTableNamingRespDTO();
        result.setTableType(req.getTableType());
        result.setTableCase(req.getTableCase() == null ? "2" : req.getTableCase());
        result.setDataLayerId(req.getDataLayerId());
        ModelingDataLayerDO layer = req.getDataLayerId() == null ? null : dataLayerMapper.selectById(req.getDataLayerId());
        if (layer != null) {
            result.setDataLayerName(layer.getName());
            result.setDataLayerEngName(layer.getEngName());
        }
        if ("4".equals(req.getTableType())) {
            result.setThemeDomainId(req.getThemeDomainId());
            ModelingThemeDomainDO theme = req.getThemeDomainId() == null ? null : themeDomainMapper.selectById(req.getThemeDomainId());
            if (theme != null) {
                result.setThemeDomainCode(theme.getCode());
                result.setThemeDomainName(theme.getName());
                result.setThemeDomainEngName(theme.getEngName());
            }
        } else {
            result.setDataDomainId(req.getDataDomainId());
            ModelingDataDomainDO domain = req.getDataDomainId() == null ? null : dataDomainMapper.selectById(req.getDataDomainId());
            if (domain != null) {
                result.setDataDomainName(domain.getName());
                result.setDataDomainEngName(domain.getEngName());
            }
        }
        result.setBusinessCategoryId(req.getBusinessCategoryId());
        ModelingBusinessCategoryDO category = req.getBusinessCategoryId() == null ? null : businessCategoryMapper.selectById(req.getBusinessCategoryId());
        if (category != null) {
            result.setBusinessCategoryCode(category.getCode());
            result.setBusinessCategoryName(category.getName());
            result.setBusinessCategoryEngName(category.getEngName());
        }
        // Layer/data-domain entities have no code field; do not fabricate one from a Chinese label.
        return result;
    }

    @Override
    public void validateModelSpace(Long dataLayerId, Long spaceId, String spaceCode) {
        ModelingDataLayerDO layer = dataLayerId == null ? null : dataLayerMapper.selectById(dataLayerId);
        if (layer == null) {
            throw new ServiceException("数仓分层不存在");
        }
        if (spaceId != null && !Objects.equals(spaceId, layer.getSpaceId())) {
            throw new ServiceException("模型与数仓分层不属于同一空间");
        }
        if (spaceCode != null && !Objects.equals(spaceCode, layer.getSpaceCode())) {
            throw new ServiceException("模型与数仓分层不属于同一空间");
        }
    }

    private static void requireValid(boolean exists, Boolean valid, String label) {
        if (!exists || !Boolean.TRUE.equals(valid)) {
            throw new ServiceException(label + "不能为空且必须存在并有效");
        }
    }

    private static String identifier(String value, String label) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new ServiceException(label + "必须为以英文字母开头、仅含英文字母、数字和下划线的安全标识符");
        }
        return value;
    }

    private static String normalizeCase(String tableCase) {
        if (tableCase == null || tableCase.isEmpty()) {
            return "2";
        }
        if (!"1".equals(tableCase) && !"2".equals(tableCase)) {
            throw new ServiceException("表名大小写必须为1（大写）或2（小写）");
        }
        return tableCase;
    }

    private static boolean spacesMatch(Long leftId, String leftCode, Long rightId, String rightCode) {
        if (leftId != null || rightId != null) {
            return Objects.equals(leftId, rightId) && (leftCode == null || rightCode == null || Objects.equals(leftCode, rightCode));
        }
        return Objects.equals(leftCode, rightCode);
    }

    private static void sameSpace(Long leftId, String leftCode, Long rightId, String rightCode) {
        if (!spacesMatch(leftId, leftCode, rightId, rightCode)) {
            throw new ServiceException("数仓治理对象必须属于同一空间");
        }
    }

    private static int identifierLimit(String datasourceType) {
        String type = datasourceType == null ? "" : datasourceType.toUpperCase(Locale.ROOT);
        if (type.contains("POSTGRE") || type.contains("KINGBASE")) {
            return 63;
        }
        if (type.contains("MYSQL") || type.contains("MARIADB") || "2".equals(type)) {
            return 64;
        }
        if (type.contains("ORACLE11")) {
            return 30;
        }
        return 128;
    }
}
