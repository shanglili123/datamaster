package com.datamaster.module.assets.dal.mapper.assetColumn;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapper
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface AssetsAssetColumnMapper extends BaseMapperX<AssetsAssetColumnDO> {

    default PageResult<AssetsAssetColumnDO> selectPage(AssetsAssetColumnPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        MPJLambdaWrapper<AssetsAssetColumnDO> lambdaQueryWrapper = new MPJLambdaWrapper();
        lambdaQueryWrapper.selectAll(AssetsAssetColumnDO.class)
                .select("t2.SENSITIVE_LEVEl as sensitiveLevelName")
                .select("t3.PROJECT_ID AS projectId,t3.PROJECT_CODE AS projectCode")
                .leftJoin("AST_SENSITIVE_LEVEL t2 on t.SENSITIVE_LEVEL_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .leftJoin("AST_ASSET_COLUMN_PROJECT_REL t3 on t.ID = t3.COLUMN_ID AND t3.DEL_FLAG = '0'")
                .eq(StringUtils.isNotBlank(reqVO.getAssetId()),AssetsAssetColumnDO::getAssetId, reqVO.getAssetId())
                .eq(reqVO.getProjectId() != null, "t3.PROJECT_ID", reqVO.getProjectId())
                .eq(StringUtils.isNotBlank(reqVO.getProjectCode()), "t3.PROJECT_CODE", reqVO.getProjectCode())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        // 构造动态查询条件
        return selectJoinPage(reqVO, AssetsAssetColumnDO.class, lambdaQueryWrapper);
    }

    default List<AssetsAssetColumnDO> selectListByAuth(AssetsAssetColumnPageReqVO reqVO) {
        MPJLambdaWrapper<AssetsAssetColumnDO> lambdaQueryWrapper = new MPJLambdaWrapper();
        lambdaQueryWrapper.selectAll(AssetsAssetColumnDO.class)
                .select("t2.SENSITIVE_LEVEl as sensitiveLevelName")
                .select("t3.PROJECT_ID AS projectId,t3.PROJECT_CODE AS projectCode")
                .leftJoin("AST_SENSITIVE_LEVEL t2 on t.SENSITIVE_LEVEL_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .leftJoin("AST_ASSET_COLUMN_PROJECT_REL t3 on t.ID = t3.COLUMN_ID AND t3.DEL_FLAG = '0'")
                .eq(StringUtils.isNotBlank(reqVO.getAssetId()), AssetsAssetColumnDO::getAssetId, reqVO.getAssetId())
                .eq(StringUtils.isNotBlank(reqVO.getSensitiveLevelId()), AssetsAssetColumnDO::getSensitiveLevelId, reqVO.getSensitiveLevelId())
                .eq(reqVO.getProjectId() != null, "t3.PROJECT_ID", reqVO.getProjectId())
                .eq(StringUtils.isNotBlank(reqVO.getProjectCode()), "t3.PROJECT_CODE", reqVO.getProjectCode())
                .orderByAsc(AssetsAssetColumnDO::getId);
        return selectJoinList(AssetsAssetColumnDO.class, lambdaQueryWrapper);
    }

    int updateAssetColumn(AssetsAssetColumnDO AssetsAssetColumnDO);

    void deleteAssetColumnByAssetId(Long assetId);

    /**
     *
     */
    default List<AssetsAssetColumnDO> findByAssetId(Long assetId) {
        LambdaQueryWrapper<AssetsAssetColumnDO> queryWrapper = Wrappers.<AssetsAssetColumnDO>lambdaQuery()
                .eq(AssetsAssetColumnDO::getAssetId, assetId)
                .orderByAsc(AssetsAssetColumnDO::getId);
        return selectList(queryWrapper);
    }

}
