

package com.datamaster.module.taxonomy.service.cat;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTagCatDO;

import java.util.List;
import java.util.Map;

/**
 * 标签类目管理Service接口
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
public interface ITaxonomyTagCatService extends IService<TaxonomyTagCatDO> {

    /**
     * 获得标签类目管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 标签类目管理分页列表
     */
    PageResult<TaxonomyTagCatDO> getAttTagCatPage(TaxonomyTagCatPageReqVO pageReqVO);

    /**
     * 创建标签类目管理
     *
     * @param createReqVO 标签类目管理信息
     * @return 标签类目管理编号
     */
    Long createAttTagCat(TaxonomyTagCatSaveReqVO createReqVO);

    /**
     * 更新标签类目管理
     *
     * @param updateReqVO 标签类目管理信息
     */
    int updateAttTagCat(TaxonomyTagCatSaveReqVO updateReqVO);

    /**
     * 删除标签类目管理
     *
     * @param idList 标签类目管理编号
     */
//    int removeAttTagCat(Collection<Long> idList);

    /**
     * 获得标签类目管理详情
     *
     * @param id 标签类目管理编号
     * @return 标签类目管理
     */
    TaxonomyTagCatDO getAttTagCatById(Long id);

    /**
     * 获得全部标签类目管理列表
     *
     * @return 标签类目管理列表
     */
    List<TaxonomyTagCatDO> getAttTagCatList();

    /**
     * 获得全部标签类目管理 Map
     *
     * @return 标签类目管理 Map
     */
    Map<Long, TaxonomyTagCatDO> getAttTagCatMap();


    /**
     * 导入标签类目管理数据
     *
     * @param importExcelList 标签类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAttTagCat(List<TaxonomyTagCatRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 获得全部数据标签类目管理列表
     *
     * @return 数据标签类目管理列表
     */
    List<TaxonomyTagCatDO> getAttTagCatLIst(TaxonomyTagCatPageReqVO TaxonomyTagCat);

    /**
     * 生成code
     *
     * @param parentId
     * @param parentCode
     * @return
     */
    String createCode(Long parentId, String parentCode);

    /**
     * 更改指定pid下的所有code
     *
     * @param pid
     */
    void changeCodeByPid(Long pid, String parentCode);

    Integer removeAttTagCat(Long id);

}
