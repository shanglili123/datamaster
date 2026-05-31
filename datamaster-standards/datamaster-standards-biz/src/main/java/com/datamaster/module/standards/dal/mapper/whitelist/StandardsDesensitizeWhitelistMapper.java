

package com.datamaster.module.standards.dal.mapper.whitelist;

import org.apache.commons.lang3.StringUtils;
import com.datamaster.module.standards.dal.dataobject.desensitizeList.StandardsDesensitizeAssetcolumnDO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeRuleDO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeWhitelistDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import com.datamaster.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistPageReqVO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.mybatis.core.query.MPJLambdaWrapperX;

/**
 * 脱敏白名单Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
public interface StandardsDesensitizeWhitelistMapper extends BaseMapperX<StandardsDesensitizeWhitelistDO> {

    default PageResult<StandardsDesensitizeWhitelistDO> selectPage(StandardsDesensitizeWhitelistPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        MPJLambdaWrapperX<StandardsDesensitizeWhitelistDO> lambdaWrapper = new MPJLambdaWrapperX<>();
        lambdaWrapper.selectAll(StandardsDesensitizeWhitelistDO.class)
                .select("t2.NAME AS dataCategoryName")
                .leftJoin("STD_DATA_CATEGORY t2 ON t.DATA_CATEGORY_ID =t2.ID  AND t2.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getName()), StandardsDesensitizeWhitelistDO::getName, reqVO.getName())
                .eq(reqVO.getDataCategoryId() != null, StandardsDesensitizeWhitelistDO::getDataCategoryId, reqVO.getDataCategoryId())
                //根据ValidFlag查询
                .eq(reqVO.getValidFlag() != null, StandardsDesensitizeWhitelistDO::getValidFlag, reqVO.getValidFlag())
                // 按照 createTime 字段降序排序
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        // 构造动态查询条件
        return selectJoinPage(reqVO, StandardsDesensitizeWhitelistDO.class, lambdaWrapper);

    }




}
