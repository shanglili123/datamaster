package com.datamaster.module.standards.dal.mapper.dataElem;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelPageReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemAssetRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;

/**
 * 数据元数据资产关联信息Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface StandardsDataElemAssetRelMapper extends BaseMapperX<StandardsDataElemAssetRelDO> {

    default PageResult<StandardsDataElemAssetRelDO> selectPage(StandardsDataElemAssetRelPageReqVO reqVO) {
        MPJLambdaWrapper<StandardsDataElemAssetRelDO> lambdaWrapper = new MPJLambdaWrapper<>();
        lambdaWrapper.selectAll(StandardsDataElemAssetRelDO.class)
                .select("t2.name AS assetName","t2.table_comment AS tableComment","t2.DESCRIPTION AS description")
                .leftJoin("AST_ASSET t2 on t.asset_id = t2.id AND t2.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getTableName()), StandardsDataElemAssetRelDO::getTableName,
                        reqVO.getTableName())
                .like(StringUtils.isNotBlank(reqVO.getColumnName()), StandardsDataElemAssetRelDO::getColumnName,
                        reqVO.getColumnName())
                .eq(reqVO.getDataElemType() != null, StandardsDataElemAssetRelDO::getDataElemType, reqVO.getDataElemType())
                .eq(reqVO.getDataElemId() != null, StandardsDataElemAssetRelDO::getDataElemId, reqVO.getDataElemId())
                .eq(reqVO.getAssetId() != null, StandardsDataElemAssetRelDO::getAssetId, reqVO.getAssetId())
                .eq(reqVO.getColumnId() != null, StandardsDataElemAssetRelDO::getColumnId, reqVO.getColumnId())
                .eq(reqVO.getCreateTime() != null, StandardsDataElemAssetRelDO::getCreateTime, reqVO.getCreateTime())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()),
                        StringUtils.isNotBlank(reqVO.getOrderByColumn())
                                ? Arrays.asList(reqVO.getOrderByColumn().split(","))
                                : null);

        return selectJoinPage(reqVO, StandardsDataElemAssetRelDO.class, lambdaWrapper);
    }
}
