

package com.datamaster.module.taxonomy.service.cat;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTaskCatDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 数据集成任务类目管理Service接口
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
public interface ITaxonomyTaskCatService extends IService<TaxonomyTaskCatDO> {

    /**
     * 获得数据集成任务类目管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据集成任务类目管理分页列表
     */
    PageResult<TaxonomyTaskCatDO> getAttTaskCatPage(TaxonomyTaskCatPageReqVO pageReqVO);

    /**
     * 创建数据集成任务类目管理
     *
     * @param createReqVO 数据集成任务类目管理信息
     * @return 数据集成任务类目管理编号
     */
    Long createAttTaskCat(TaxonomyTaskCatSaveReqVO createReqVO);

    /**
     * 更新数据集成任务类目管理
     *
     * @param updateReqVO 数据集成任务类目管理信息
     */
    int updateAttTaskCat(TaxonomyTaskCatSaveReqVO updateReqVO);

    /**
     * 删除数据集成任务类目管理
     *
     * @param idList 数据集成任务类目管理编号
     */
    int removeAttTaskCat(Collection<Long> idList);

    /**
     * 获得数据集成任务类目管理详情
     *
     * @param id 数据集成任务类目管理编号
     * @return 数据集成任务类目管理
     */
    TaxonomyTaskCatDO getAttTaskCatById(Long id);

    /**
     * 获得全部数据集成任务类目管理列表
     *
     * @return 数据集成任务类目管理列表
     */
    List<TaxonomyTaskCatDO> getAttTaskCatList();

    /**
     * 获得全部数据集成任务类目管理列表
     *
     * @return 数据集成任务类目管理列表
     */
    List<TaxonomyTaskCatDO> getAttTaskCatList(TaxonomyTaskCatPageReqVO reqVO);

    /**
     * 获得全部数据集成任务类目管理 Map
     *
     * @return 数据集成任务类目管理 Map
     */
    Map<Long, TaxonomyTaskCatDO> getAttTaskCatMap();


    /**
     * 导入数据集成任务类目管理数据
     *
     * @param importExcelList 数据集成任务类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAttTaskCat(List<TaxonomyTaskCatRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 生成code
     *
     * @param parentId
     * @param parentCode
     * @return
     */
    String createCode(Long parentId, String parentCode);
}
