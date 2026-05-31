

package com.datamaster.module.taxonomy.service.cat;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDocumentCatDO;

import java.util.List;
import java.util.Map;

/**
 * 标准信息分类管理Service接口
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
public interface ITaxonomyDocumentCatService extends IService<TaxonomyDocumentCatDO> {

    /**
     * 获得标准信息分类管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 标准信息分类管理分页列表
     */
    PageResult<TaxonomyDocumentCatDO> getAttDocumentCatPage(TaxonomyDocumentCatPageReqVO pageReqVO);

    /**
     * 获得全部标准信息分类管理列表
     *
     * @return 标准信息分类管理列表
     */
    List<TaxonomyDocumentCatDO> getAttDocumentCatList(TaxonomyDocumentCatPageReqVO pageReqVO);

    /**
     * 创建标准信息分类管理
     *
     * @param createReqVO 标准信息分类管理信息
     * @return 标准信息分类管理编号
     */
    Long createAttDocumentCat(TaxonomyDocumentCatSaveReqVO createReqVO);

    /**
     * 更新标准信息分类管理
     *
     * @param updateReqVO 标准信息分类管理信息
     */
    int updateAttDocumentCat(TaxonomyDocumentCatSaveReqVO updateReqVO);

    /**
     * 删除标准信息分类管理
     *
     * @param id 标准信息分类管理编号
     */
    int removeAttDocumentCat(Long id);

    /**
     * 获得标准信息分类管理详情
     *
     * @param id 标准信息分类管理编号
     * @return 标准信息分类管理
     */
    TaxonomyDocumentCatDO getAttDocumentCatById(Long id);

    /**
     * 获得全部标准信息分类管理列表
     *
     * @return 标准信息分类管理列表
     */
    List<TaxonomyDocumentCatDO> getAttDocumentCatList();

    /**
     * 获得全部标准信息分类管理 Map
     *
     * @return 标准信息分类管理 Map
     */
    Map<Long, TaxonomyDocumentCatDO> getAttDocumentCatMap();

    /**
     * 是否存在标准信息分类管理子节点
     *
     * @param id 标准信息分类管理id
     * @return 结果 true 存在 false 不存在
     */
    boolean hasChildByAttDocumentCatId(Long id);

    /**
     * 生成code
     *
     * @param parentId
     * @param parentCode
     * @return
     */
    String createCode(Long parentId, String parentCode);


}
