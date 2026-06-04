

package com.datamaster.module.taxonomy.dal.mapper.Tag;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.Tag.TaxonomyTagDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 标签管理Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
public interface TaxonomyTagMapper extends BaseMapperX<TaxonomyTagDO> {

    default PageResult<TaxonomyTagDO> selectPage(TaxonomyTagPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time", "aeest_count"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxonomyTagDO>()
                .likeIfPresent(TaxonomyTagDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyTagDO::getDescription, reqVO.getDescription())
                .eqIfPresent(TaxonomyTagDO::getCatCode, reqVO.getCatCode())
                .eqIfPresent(TaxonomyTagDO::getAeestCount, reqVO.getAeestCount())
                .eqIfPresent(TaxonomyTagDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TaxonomyTagDO::getAlias, reqVO.getAlias())
                .eqIfPresent(TaxonomyTagDO::getNearSynonyms, reqVO.getNearSynonyms())
                .eqIfPresent(TaxonomyTagDO::getSynonyms, reqVO.getSynonyms())
                .eqIfPresent(TaxonomyTagDO::getCreateTime, reqVO.getCreateTime())
                .likeIfPresent(TaxonomyTagDO::getCreateBy, reqVO.getCreateBy())
                .notInIfPresent(TaxonomyTagDO::getId, reqVO.getIds())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(TaxonomyTagDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .eq(reqVO.getProjectId() != null, TaxonomyTagDO::getProjectId, reqVO.getProjectId())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(),allowedColumns));
    }


    /**
     * 将老的 CAT_CODE 批量更新成新的 CAT_CODE
     *
     * @param oldCatCode 旧分类编码
     * @param newCatCode 新分类编码
     * @return 受影响行数
     */
    default int updateCatCode(String oldCatCode, String newCatCode) {
        return this.update(
                null,
                Wrappers.<TaxonomyTagDO>lambdaUpdate()
                        .set(TaxonomyTagDO::getCatCode, newCatCode)
                        .eq(TaxonomyTagDO::getDelFlag, "0")
                        .eq(TaxonomyTagDO::getCatCode, oldCatCode)
        );
    }}
