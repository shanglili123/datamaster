package com.datamaster.module.assets.dal.mapper.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetPageReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import java.util.*;

/**
 * Mapper
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface AssetsAssetMapper extends BaseMapperX<AssetsAssetDO> {

    default PageResult<AssetsAssetDO> selectPage(AssetsAssetPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapperX<AssetsAssetDO> lambdaWrapper = new MPJLambdaWrapperX();
        lambdaWrapper.selectAll(AssetsAssetDO.class)
                .select(
                        "t2.NAME AS catName",
                        "t3.NAME AS dataLayerName",
                        "t3.ENG_NAME AS dataLayerEngName",
                        "t4.NAME AS businessCategoryName",
                        "t4.ENG_NAME AS businessCategoryEngName",
                        "t5.NAME AS dataDomainName",
                        "t5.ENG_NAME AS dataDomainEngName",
                        "t6.NAME AS themeDomainName",
                        "t6.ENG_NAME AS themeDomainEngName")
                .leftJoin("TAX_ASSET_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")

                .leftJoin("MDL_DATA_LAYER t3 ON t.DATA_LAYER_ID = t3.id AND t3.DEL_FLAG = '0'")
                .leftJoin("MDL_BUSINESS_CATEGORY t4 ON t.BUSINESS_CATEGORY_ID = t4.id AND t4.DEL_FLAG = '0'")
                .leftJoin("MDL_DATA_DOMAIN t5 ON t.DATA_DOMAIN_ID = t5.id AND t5.DEL_FLAG = '0'")
                .leftJoin("MDL_THEME_DOMAIN t6 ON t.THEME_DOMAIN_ID = t6.id AND t6.DEL_FLAG = '0'");

        //增加标签筛选
        if (CollectionUtils.isNotEmpty(reqVO.getTagIdList())) {
            String tagIds = reqVO.getTagIdList().stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));

            lambdaWrapper.exists(
                    "SELECT 1 FROM TAX_TAG_ASSET_REL taRel WHERE t.id = taRel.ASSET_ID AND taRel.DEL_FLAG = '0' AND taRel.TAG_ID IN (" + tagIds + ")"
            );
        }

        //拼接查询标签列表
        String subSelectSql = "SELECT\n" +
                "'['|| WM_CONCAT(DISTINCT '{\"tagId\":\"' || d.ID || '\",\"tagName\":\"' || d.name || '\"}' ) ||']'\n" +
                "FROM \n" +
                "     TAX_TAG d \n" +
                "JOIN TAX_TAG_ASSET_REL rel ON d.ID = rel.TAG_ID \n" +
                "WHERE \n" +
                "    d.DEL_FLAG ='0' AND rel.DEL_FLAG = '0'  \n" +
                "    AND rel.ASSET_ID = t.ID \n" +
                "HAVING COUNT(d.ID) > 0";

        if (StringUtils.equals("mysql", MasterDataSourceConfig.getDatabaseType())) {
            subSelectSql = "SELECT \n" +
                    "    CONCAT(\n" +
                    "        '[', \n" +
                    "        GROUP_CONCAT(\n" +
                    "            DISTINCT CONCAT(\n" +
                    "                '{\"tagId\":\"', d.ID, \n" +
                    "                '\",\"tagName\":\"', d.name, \n" +
                    "                '\"}'\n" +
                    "            )\n" +
                    "        ), \n" +
                    "        ']'\n" +
                    "    ) AS json_result\n" +
                    "FROM \n" +
                    "     TAX_TAG d \n" +
                    "JOIN TAX_TAG_ASSET_REL rel ON d.ID = rel.TAG_ID \n" +
                    "WHERE \n" +
                    "    d.DEL_FLAG ='0' \n" +
                    "    AND rel.ASSET_ID = t.ID \n" +
                    "HAVING COUNT(d.ID) > 0";
        } else if (StringUtils.equals("kingbase8", MasterDataSourceConfig.getDatabaseType())) {
            subSelectSql = "SELECT \n" +
                    "    CONCAT_WS('','[' , STRING_AGG(DISTINCT CONCAT_WS('', '{\"tagId\":\"', d.ID, '\",\"tagName\":\"', d.name, '\"}'), ',') , ']')\n" +
                    "FROM \n" +
                    "     TAX_TAG d \n" +
                    "JOIN TAX_TAG_ASSET_REL rel ON d.ID = rel.TAG_ID \n" +
                    "WHERE \n" +
                    "    d.DEL_FLAG ='0' \n" +
                    "    AND rel.ASSET_ID = t.ID \n" +
                    "HAVING COUNT(d.ID) > 0";
        } else {
            subSelectSql = "SELECT \n" +
                    "    '[' || STRING_AGG(DISTINCT '{\"tagId\":\"' || d.ID || '\",\"tagName\":\"' || d.name || '\"}', ',') || ']'\n" +
                    "FROM \n" +
                    "     TAX_TAG d \n" +
                    "JOIN TAX_TAG_ASSET_REL rel ON d.ID = rel.TAG_ID \n" +
                    "WHERE \n" +
                    "    d.DEL_FLAG ='0' \n" +
                    "    AND rel.ASSET_ID = t.ID \n" +
                    "HAVING COUNT(d.ID) > 0";
        }
        lambdaWrapper.select("(" + subSelectSql + ") AS tags");
        lambdaWrapper
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), AssetsAssetDO::getCatCode, reqVO.getCatCode())
                .like(StringUtils.isNotBlank(reqVO.getName()), AssetsAssetDO::getName, reqVO.getName())
                .eq(StringUtils.isNotBlank(reqVO.getDatasourceId()), AssetsAssetDO::getDatasourceId, reqVO.getDatasourceId())
                .eq(StringUtils.isNotBlank(reqVO.getType()), AssetsAssetDO::getType, reqVO.getType())
                .like(StringUtils.isNotBlank(reqVO.getTableName()), AssetsAssetDO::getTableName, reqVO.getTableName())
                .eq(StringUtils.isNotBlank(reqVO.getTableComment()), AssetsAssetDO::getTableComment, reqVO.getTableComment())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), AssetsAssetDO::getStatus, reqVO.getStatus())
                .eq(StringUtils.isNotBlank(reqVO.getDescription()), AssetsAssetDO::getDescription, reqVO.getDescription())
                .in(reqVO.getAssetIdList() != null && reqVO.getAssetIdList()
                        .size() > 0, AssetsAssetDO::getId, reqVO.getAssetIdList())
                .eq(StringUtils.isNotBlank(reqVO.getTableType()), AssetsAssetDO::getTableType, reqVO.getTableType())
                .eq(reqVO.getDataLayerId() != null, AssetsAssetDO::getDataLayerId, reqVO.getDataLayerId())
                .eq(reqVO.getBusinessCategoryId() != null, AssetsAssetDO::getBusinessCategoryId, reqVO.getBusinessCategoryId())
                .likeRight(StringUtils.isNotBlank(reqVO.getBusinessCategoryCode()), AssetsAssetDO::getBusinessCategoryCode, reqVO.getBusinessCategoryCode())
                .eq(reqVO.getDataDomainId() != null, AssetsAssetDO::getDataDomainId, reqVO.getDataDomainId())
                .eq(reqVO.getThemeDomainId() != null, AssetsAssetDO::getThemeDomainId, reqVO.getThemeDomainId())
                .likeRight(StringUtils.isNotBlank(reqVO.getThemeDomainCode()), AssetsAssetDO::getThemeDomainCode, reqVO.getThemeDomainCode())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn()
                                                                                                                                                                                            .split(",")) : null);
        return selectJoinPage(reqVO, AssetsAssetDO.class, lambdaWrapper);
    }

    default PageResult<AssetsAssetDO> selectPageDpp(AssetsAssetPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<AssetsAssetDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsAssetDO.class)
                .select("t2.NAME AS catName")
                .select("t3.PROJECT_ID AS projectId,t3.PROJECT_CODE AS projectCode")
                .leftJoin("TAX_ASSET_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .leftJoin("AST_ASSET_PROJECT_REL t3 on t.id = t3.ASSET_ID AND t3.DEL_FLAG = '0'")
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), AssetsAssetDO::getCatCode, reqVO.getCatCode())
                .like(StringUtils.isNotBlank(reqVO.getName()), AssetsAssetDO::getName, reqVO.getName())
                .eq(StringUtils.isNotBlank(reqVO.getDatasourceId()), AssetsAssetDO::getDatasourceId, reqVO.getDatasourceId())
                .eq(StringUtils.isNotBlank(reqVO.getType()), AssetsAssetDO::getType, reqVO.getType())
                .like(StringUtils.isNotBlank(reqVO.getTableName()), AssetsAssetDO::getTableName, reqVO.getTableName())
                .eq(StringUtils.isNotBlank(reqVO.getTableComment()), AssetsAssetDO::getTableComment, reqVO.getTableComment())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), AssetsAssetDO::getStatus, reqVO.getStatus())
                .eq(StringUtils.isNotBlank(reqVO.getDescription()), AssetsAssetDO::getDescription, reqVO.getDescription())
                .in(reqVO.getThemeAssetIdList() != null && !reqVO.getThemeAssetIdList()
                        .isEmpty(), AssetsAssetDO::getId, reqVO.getThemeAssetIdList())
                .and(wrapper -> wrapper
                        .in(reqVO.getAssetIdList() != null && !reqVO.getAssetIdList()
                                .isEmpty(), AssetsAssetDO::getId, reqVO.getAssetIdList())
                        .or(inner -> inner
                                .eq(reqVO.getProjectId() != null, "t3.PROJECT_ID", reqVO.getProjectId())
                                .eq(StringUtils.isNotBlank(reqVO.getProjectCode()), "t3.PROJECT_CODE", reqVO.getProjectCode())
                        )
                )
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn()
                                                                                                                                                                                            .split(",")) : null);

        return selectJoinPage(reqVO, AssetsAssetDO.class, lambdaWrapper);
    }

    void deleteAssetById(Long id);

    default List<AssetsAssetDO> findByDatasourceIdAndTableName(Long datasourceId, String tableName) {
        LambdaQueryWrapper<AssetsAssetDO> queryWrapper = Wrappers.<AssetsAssetDO>lambdaQuery()
                .eq(AssetsAssetDO::getDatasourceId, datasourceId)
                .eq(AssetsAssetDO::getTableName, tableName);
        return selectList(queryWrapper);
    }

    Map<String, Object> getAssetOverviewStatistics();

    /**
     *  CAT_CODE  CAT_CODE
     *
     * @param oldCatCode
     * @param newCatCode
     * @return
     */
    default int updateCatCode(String oldCatCode, String newCatCode) {
        return this.update(
                null,
                Wrappers.<AssetsAssetDO>lambdaUpdate()
                        .set(AssetsAssetDO::getCatCode, newCatCode)
                        .eq(AssetsAssetDO::getDelFlag, "0")
                        .eq(AssetsAssetDO::getCatCode, oldCatCode)
        );
    }
}
