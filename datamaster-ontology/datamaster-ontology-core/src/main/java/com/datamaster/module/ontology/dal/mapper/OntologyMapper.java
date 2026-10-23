package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Locale;

/**
 * 本体 Mapper
 *
 * 对应表 ONT_ONTOLOGY
 */
public interface OntologyMapper extends BaseMapperX<OntologyDO> {

    default PageResult<OntologyDO> selectPage(OntologyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OntologyDO>()
                .eq(reqVO.getId() != null, OntologyDO::getId, reqVO.getId())
                // 本体名称/编码是用户自然语言入口，必须忽略英文大小写；普通 LIKE
                // 在 PostgreSQL 上会把 “ai决策模拟库” 和 “AI决策模拟库” 判定为不同值。
                .apply(StringUtils.isNotBlank(reqVO.getName()),
                        "LOWER(name) LIKE LOWER({0})", "%" + lower(reqVO.getName()) + "%")
                .apply(StringUtils.isNotBlank(reqVO.getCode()),
                        "LOWER(code) LIKE LOWER({0})", "%" + lower(reqVO.getCode()) + "%")
                .eq(reqVO.getStatus() != null, OntologyDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getSpaceId() != null, OntologyDO::getSpaceId, reqVO.getSpaceId())
                .eq(StringUtils.isNotBlank(reqVO.getSpaceCode()), OntologyDO::getSpaceCode, reqVO.getSpaceCode())
                .orderByDesc(OntologyDO::getId));
    }

    static String lower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
