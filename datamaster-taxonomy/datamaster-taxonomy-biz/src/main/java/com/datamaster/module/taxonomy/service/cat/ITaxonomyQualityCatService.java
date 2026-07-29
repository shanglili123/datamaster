

package com.datamaster.module.taxonomy.service.cat;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyQualityCatDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 质量探查类目Service接口
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
public interface ITaxonomyQualityCatService extends IService<TaxonomyQualityCatDO> {

    /**
     * 获得质量探查类目分页列表
     *
     * @param pageReqVO 分页请求
     * @return 质量探查类目分页列表
     */
    PageResult<TaxonomyQualityCatDO> getAttQualityCatPage(TaxonomyQualityCatPageReqVO pageReqVO);

    /**
     * 创建质量探查类目
     *
     * @param createReqVO 质量探查类目信息
     * @return 质量探查类目编号
     */
    Long createAttQualityCat(TaxonomyQualityCatSaveReqVO createReqVO);

    /**
     * 更新质量探查类目
     *
     * @param updateReqVO 质量探查类目信息
     */
    int updateAttQualityCat(TaxonomyQualityCatSaveReqVO updateReqVO);

    /**
     * 删除质量探查类目
     *
     * @param idList 质量探查类目编号
     */
    int removeAttQualityCat(Collection<Long> idList);

    /**
     * 获得质量探查类目详情
     *
     * @param id 质量探查类目编号
     * @return 质量探查类目
     */
    TaxonomyQualityCatDO getAttQualityCatById(Long id);

    /**
     * 获得全部质量探查类目列表
     *
     * @return 质量探查类目列表
     */
    List<TaxonomyQualityCatDO> getAttQualityCatList(TaxonomyQualityCatPageReqVO TaxonomyQualityCat);

    /**
     * 获得全部质量探查类目 Map
     *
     * @return 质量探查类目 Map
     */
    Map<Long, TaxonomyQualityCatDO> getAttQualityCatMap();


    /**
     * 导入质量探查类目数据
     *
     * @param importExcelList 质量探查类目数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAttQualityCat(List<TaxonomyQualityCatRespVO> importExcelList, boolean isUpdateSupport, String operName);    /**
     * 生成code
     *
     * @param parentId
     * @param parentCode
     * @return
     */
    String createCode(Long parentId, String parentCode);

}
