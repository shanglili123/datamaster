package com.datamaster.module.standards.dal.mapper.model;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelDO;
import java.util.Arrays;
import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

/**
 * 逻辑模型Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface StandardsModelMapper extends BaseMapperX<StandardsModelDO> {

    default PageResult<StandardsModelDO> selectPage(StandardsModelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<StandardsModelDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(StandardsModelDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_MODEL_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getModelName()), StandardsModelDO::getModelName, reqVO.getModelName())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), StandardsModelDO::getCatCode, reqVO.getCatCode())
                .like(StringUtils.isNotBlank(reqVO.getModelComment()), StandardsModelDO::getModelComment, reqVO.getModelComment())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), StandardsModelDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getDocumentId()!= null, StandardsModelDO::getDocumentId, reqVO.getDocumentId())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);

        return selectJoinPage(reqVO, StandardsModelDO.class, lambdaWrapper);
    }
}
