package com.datamaster.module.standards.dal.mapper.document;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.document.vo.StandardsDocumentPageReqVO;
import com.datamaster.module.standards.controller.admin.document.vo.StandardsDocumentSearchReqVO;
import com.datamaster.module.standards.controller.admin.document.vo.StandardsDocumentSearchRespVO;
import com.datamaster.module.standards.dal.dataobject.document.StandardsDocumentDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 标准信息登记Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
public interface StandardsDocumentMapper extends BaseMapperX<StandardsDocumentDO> {


    default PageResult<StandardsDocumentDO> selectPage(StandardsDocumentPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapper<StandardsDocumentDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(StandardsDocumentDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_DOCUMENT_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .like(StringUtils.isNotBlank(reqVO.getName()), StandardsDocumentDO::getName, reqVO.getName())
                .like(StringUtils.isNotBlank(reqVO.getCode()), StandardsDocumentDO::getCode, reqVO.getCode())
                .and(StringUtils.isNotBlank(reqVO.getKeyWordParam()),
                        q -> q.like(StandardsDocumentDO::getCode, reqVO.getKeyWordParam())
                                .or()
                                .like(StandardsDocumentDO::getName, reqVO.getKeyWordParam()))
                .like(StringUtils.isNotBlank(reqVO.getIssuingAgency()), StandardsDocumentDO::getIssuingAgency, reqVO.getIssuingAgency())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), StandardsDocumentDO::getCatCode, reqVO.getCatCode())
                .eq(StringUtils.isNotBlank(reqVO.getType()),StandardsDocumentDO::getType, reqVO.getType())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()),StandardsDocumentDO::getStatus, reqVO.getStatus())
                .eq(StringUtils.isNotBlank(reqVO.getVersion()),StandardsDocumentDO::getVersion, reqVO.getVersion());
        if ("1".equals(reqVO.getExistStandardUrl())) {
            lambdaWrapper.isNotNull(StandardsDocumentDO::getFileUrl)
                    .ne(StandardsDocumentDO::getFileUrl, "");
        }
        lambdaWrapper.orderByStr(
                StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                StringUtils.equals("asc", reqVO.getIsAsc()),
                StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        return selectJoinPage(reqVO, StandardsDocumentDO.class, lambdaWrapper);
    }

    /**
     * 标准检索分页列表
     *
     * @param page
     * @param StandardsDocument
     * @return
     */
    IPage<StandardsDocumentSearchRespVO> getDpDocumentSearchPage(Page page, @Param("params") StandardsDocumentSearchReqVO StandardsDocument);
}
