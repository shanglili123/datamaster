

package com.datamaster.module.modeling.dal.mapper.businessCategory;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategoryPageReqVO;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessCategoryDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 业务分类Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-04-08
 */
public interface ModelingBusinessCategoryMapper extends BaseMapperX<ModelingBusinessCategoryDO> {

    default PageResult<ModelingBusinessCategoryDO> selectPage(ModelingBusinessCategoryPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        MPJLambdaWrapperX<ModelingBusinessCategoryDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper.selectAll(ModelingBusinessCategoryDO.class)
                .select("t2.NICK_NAME AS ownerName")
                .leftJoin("SYSTEM_USER t2 ON t.OWNER_ID=t2.USER_ID  AND t2.DEL_FLAG = '0'")
                .leftJoin("MDL_BUSINESS_DOMAIN_REL t3 ON t3.BUSINESS_CATEGORY_ID=t.ID  AND t3.DEL_FLAG = '0'")
                .leftJoin("MDL_DATA_DOMAIN t4 ON t4.ID=t3.DATA_DOMAIN_ID  AND t4.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getName()), ModelingBusinessCategoryDO::getName, reqVO.getName())
                .eq(reqVO.getValidFlag() != null, ModelingBusinessCategoryDO::getValidFlag, reqVO.getValidFlag())
                .and(reqVO.getParentId() != null, w -> w
                        .eq(ModelingBusinessCategoryDO::getParentId, reqVO.getParentId())
                        .or()
                        .eq(ModelingBusinessCategoryDO::getId, reqVO.getParentId())
                )
                .eq(reqVO.getDomainId() != null, "t4.ID", reqVO.getDomainId());

        // 按照 createTime 字段降序排序
//                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
//                        StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
//                .orderByStr(
//                    StringUtils.isNotBlank(reqVO.getOrderByColumn()), // 条件：排序字段不为空时才排序
//                    StringUtils.equals("asc", reqVO.getIsAsc()),       // 排序方向：true=ASC，false=DESC
//                    reqVO.getOrderByColumn(),                           // 排序字段（数据库下划线格式，如 create_time）
//                    allowedColumns                                      // 允许的字段白名单，防SQL注入
//                );
        lambdaWrapper.orderBy(StringUtils.isNotBlank(reqVO.getOrderByColumn()), !"asc".equals(reqVO.getIsAsc()), ModelingBusinessCategoryDO::getCreateTime);
        // 构造动态查询条件
        return selectJoinPage(reqVO, ModelingBusinessCategoryDO.class, lambdaWrapper);
    /*    // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ModelingBusinessCategoryDO>()
                .likeIfPresent(ModelingBusinessCategoryDO::getName, reqVO.getName())
                .eqIfPresent(ModelingBusinessCategoryDO::getParentId, reqVO.getParentId())
                .eqIfPresent(ModelingBusinessCategoryDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(ModelingBusinessCategoryDO::getDescription, reqVO.getDescription())
                .likeIfPresent(ModelingBusinessCategoryDO::getEngName, reqVO.getEngName())
                .eqIfPresent(ModelingBusinessCategoryDO::getOwnerPhone, reqVO.getOwnerPhone())
                .eqIfPresent(ModelingBusinessCategoryDO::getOwnerId, reqVO.getOwnerId())
                .eqIfPresent(ModelingBusinessCategoryDO::getDomainId, reqVO.getDomainId())
                .eqIfPresent(ModelingBusinessCategoryDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(ModelingBusinessCategoryDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelingBusinessCategoryDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));*/
    }


    default List<ModelingBusinessCategoryDO> selectAllList(ModelingBusinessCategoryPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        MPJLambdaWrapperX<ModelingBusinessCategoryDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper.selectAll(ModelingBusinessCategoryDO.class)
                .select("t2.NICK_NAME AS ownerName")
                .leftJoin("SYSTEM_USER t2 ON t.OWNER_ID=t2.USER_ID  AND t2.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getName()), ModelingBusinessCategoryDO::getName, reqVO.getName())
                .eq(reqVO.getValidFlag() != null, ModelingBusinessCategoryDO::getValidFlag, reqVO.getValidFlag())
                .and(reqVO.getParentId() != null, w -> w
                        .eq(ModelingBusinessCategoryDO::getParentId, reqVO.getParentId())
                        .or()
                        .eq(ModelingBusinessCategoryDO::getId, reqVO.getParentId())
                )
                .eq(reqVO.getOwnerId() != null, ModelingBusinessCategoryDO::getOwnerId, reqVO.getOwnerId());
        lambdaWrapper.orderBy(StringUtils.isNotBlank(reqVO.getOrderByColumn()), !"asc".equals(reqVO.getIsAsc()), ModelingBusinessCategoryDO::getCreateTime);
        // 构造动态查询条件
        return selectList(lambdaWrapper);
    }

    @Update(value = "update MDL_BUSINESS_CATEGORY set VALID_FLAG=CASE WHEN #{validFlag} THEN '1' ELSE '0' END where code like concat(#{prefixCode}, '%')")
    void updateValidFlag(@Param("prefixCode") String prefixCode, @Param("validFlag") Boolean validFlag);
}
