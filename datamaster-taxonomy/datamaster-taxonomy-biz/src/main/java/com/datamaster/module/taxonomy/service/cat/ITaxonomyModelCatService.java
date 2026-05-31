

package com.datamaster.module.taxonomy.service.cat;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyModelCatDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 逻辑模型类目管理Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface ITaxonomyModelCatService extends IService<TaxonomyModelCatDO> {

    /**
     * 获得逻辑模型类目管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 逻辑模型类目管理分页列表
     */
    PageResult<TaxonomyModelCatDO> getAttModelCatPage(TaxonomyModelCatPageReqVO pageReqVO);

    /**
     * 创建逻辑模型类目管理
     *
     * @param createReqVO 逻辑模型类目管理信息
     * @return 逻辑模型类目管理编号
     */
    Long createAttModelCat(TaxonomyModelCatSaveReqVO createReqVO);

    /**
     * 更新逻辑模型类目管理
     *
     * @param updateReqVO 逻辑模型类目管理信息
     */
    int updateAttModelCat(TaxonomyModelCatSaveReqVO updateReqVO);

    /**
     * 删除逻辑模型类目管理
     *
     * @param idList 逻辑模型类目管理编号
     */
    int removeAttModelCat(Collection<Long> idList);
    int removeAttModelCat(Long id);

    /**
     * 获得逻辑模型类目管理详情
     *
     * @param id 逻辑模型类目管理编号
     * @return 逻辑模型类目管理
     */
    TaxonomyModelCatDO getAttModelCatById(Long id);

    /**
     * 获得全部逻辑模型类目管理列表
     *
     * @return 逻辑模型类目管理列表
     */
    List<TaxonomyModelCatDO> getAttModelCatList();

    /**
     * 获得全部逻辑模型类目管理列表
     *
     * @return 逻辑模型类目管理列表
     */
    List<TaxonomyModelCatDO> getAttModelCatList(TaxonomyModelCatPageReqVO reqVO);

    /**
     * 获得全部逻辑模型类目管理 Map
     *
     * @return 逻辑模型类目管理 Map
     */
    Map<Long, TaxonomyModelCatDO> getAttModelCatMap();


    /**
     * 导入逻辑模型类目管理数据
     *
     * @param importExcelList 逻辑模型类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAttModelCat(List<TaxonomyModelCatRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 生成code
     *
     * @param parentId
     * @param parentCode
     * @return
     */
    String createCode(Long parentId, String parentCode);
}
