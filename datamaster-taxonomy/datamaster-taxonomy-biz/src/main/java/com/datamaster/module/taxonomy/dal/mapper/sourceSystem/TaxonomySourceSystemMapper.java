

package com.datamaster.module.taxonomy.dal.mapper.sourceSystem;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.sourceSystem.TaxonomySourceSystemDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 来源系统Mapper接口
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
public interface TaxonomySourceSystemMapper extends BaseMapperX<TaxonomySourceSystemDO> {


    default PageResult<TaxonomySourceSystemDO> selectPage(TaxonomySourceSystemPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        MPJLambdaWrapper<TaxonomySourceSystemDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(TaxonomySourceSystemDO.class)
                .select("t2.NICK_NAME AS responsiblePersonName,t3.NICK_NAME AS contactPersonName")
                .leftJoin("SYSTEM_USER t2 on t.RESPONSIBLE_PERSON = t2.USER_ID AND t2.DEL_FLAG = '0'")
                .leftJoin("SYSTEM_USER t3 on t.CONTACT_PERSON = t3.USER_ID AND t3.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getName()), TaxonomySourceSystemDO::getName, reqVO.getName())
                .eq(StringUtils.isNotBlank(reqVO.getType()), TaxonomySourceSystemDO::getType, reqVO.getType())
                .eq(reqVO.getValidFlag() != null, "valid_flag", Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return selectJoinPage(reqVO, TaxonomySourceSystemDO.class, lambdaWrapper);
    }

    default TaxonomySourceSystemDO selectById(Long id) {
        MPJLambdaWrapper<TaxonomySourceSystemDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(TaxonomySourceSystemDO.class)
                .select("t2.NICK_NAME AS responsiblePersonName,t3.NICK_NAME AS contactPersonName")
                .leftJoin("SYSTEM_USER t2 on t.RESPONSIBLE_PERSON = t2.USER_ID AND t2.DEL_FLAG = '0'")
                .leftJoin("SYSTEM_USER t3 on t.CONTACT_PERSON = t3.USER_ID AND t3.DEL_FLAG = '0'")
                .eq(TaxonomySourceSystemDO::getId, id);
        return selectOne(lambdaWrapper);
    }

  /*  default PageResult<TaxonomySourceSystemDO> selectPage(TaxonomySourceSystemPageReqVO reqVO) {
        MPJLambdaWrapper<TaxonomySourceSystemDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(TaxonomySourceSystemDO.class)
                .like(TaxonomySourceSystemDO::getName, reqVO.getName())
                .eq(TaxonomySourceSystemDO::getType, reqVO.getType())
                .eq(TaxonomySourceSystemDO::getSortOrder, reqVO.getSortOrder())
                .eq(TaxonomySourceSystemDO::getDescription, reqVO.getDescription())
                .eq(TaxonomySourceSystemDO::getValidFlag, reqVO.getValidFlag())
                .eq(TaxonomySourceSystemDO::getResponsiblePerson, reqVO.getResponsiblePerson())
                .eq(TaxonomySourceSystemDO::getContactPerson, reqVO.getContactPerson())
                .eq(TaxonomySourceSystemDO::getCreateTime, reqVO.getCreateTime());
        return selectJoinPage(reqVO, TaxonomySourceSystemDO.class, lambdaWrapper);
    }*/
}
