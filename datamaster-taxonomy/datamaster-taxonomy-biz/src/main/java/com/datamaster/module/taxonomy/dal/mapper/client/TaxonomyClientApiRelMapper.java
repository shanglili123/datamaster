

package com.datamaster.module.taxonomy.dal.mapper.client;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.client.TaxonomyClientApiRelDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 应用API服务关联Mapper接口
 *
 * @author FXB
 * @date 2025-08-21
 */
public interface TaxonomyClientApiRelMapper extends BaseMapperX<TaxonomyClientApiRelDO> {

    default PageResult<TaxonomyClientApiRelDO> selectPage(TaxonomyClientApiRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<TaxonomyClientApiRelDO> lambdaWrapper = new MPJLambdaWrapper();

        // CONCAT("/services/",CONCAT(t2.API_VERSION,t2.API_URL))
        lambdaWrapper.selectAll(TaxonomyClientApiRelDO.class)
                .select("t2.NAME AS apiName",
                        "CONCAT('/services/',CONCAT(t2.API_VERSION,t2.API_URL)) AS apiUrl",
                        "t2.REQ_METHOD AS reqMethod")
                .leftJoin("SVC_API t2 ON t.API_ID = t2.ID AND t2.DEL_FLAG = '0'")
                .eq(reqVO.getClientId() != null, TaxonomyClientApiRelDO::getClientId, reqVO.getClientId())
                .eq(reqVO.getApiId() != null, TaxonomyClientApiRelDO::getApiId, reqVO.getApiId())
                .eq(StringUtils.isNotBlank(reqVO.getPvFlag()), TaxonomyClientApiRelDO::getPvFlag, reqVO.getPvFlag())
//                .eq(TaxonomyClientApiRelDO::getStartTime, reqVO.getStartTime())
//                .eq(TaxonomyClientApiRelDO::getEndTime, reqVO.getEndTime())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), TaxonomyClientApiRelDO::getStatus, reqVO.getStatus())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);

        // 构造动态查询条件
        return selectJoinPage(reqVO, TaxonomyClientApiRelDO.class, lambdaWrapper);
    }
}
