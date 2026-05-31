package com.datamaster.module.standards.service.model;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnPageReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelColumnDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 逻辑模型属性信息Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IStandardsModelColumnService extends IService<StandardsModelColumnDO> {

    /**
     * 获得逻辑模型属性信息分页列表
     *
     * @param pageReqVO 分页请求
     * @return 逻辑模型属性信息分页列表
     */
    PageResult<StandardsModelColumnDO> getDpModelColumnPage(StandardsModelColumnPageReqVO pageReqVO);

    /**
     * 创建逻辑模型属性信息
     *
     * @param createReqVO 逻辑模型属性信息信息
     * @return 逻辑模型属性信息编号
     */
    Long createDpModelColumn(StandardsModelColumnSaveReqVO createReqVO);

    /**
     * 更新逻辑模型属性信息
     *
     * @param updateReqVO 逻辑模型属性信息信息
     */
    int updateDpModelColumn(StandardsModelColumnSaveReqVO updateReqVO);

    /**
     * 删除逻辑模型属性信息
     *
     * @param idList 逻辑模型属性信息编号
     */
    int removeDpModelColumn(Collection<Long> idList);

    /**
     * 批量删除逻辑模型属性信息
     *
     * @param modelIdList 逻辑模型编号
     */
    int removeDpModelColumnByModelId(Collection<Long> modelIdList);

    /**
     * 获得逻辑模型属性信息详情
     *
     * @param id 逻辑模型属性信息编号
     * @return 逻辑模型属性信息
     */
    StandardsModelColumnDO getDpModelColumnById(Long id);

    /**
     * 获得全部逻辑模型属性信息列表
     *
     * @return 逻辑模型属性信息列表
     */
    List<StandardsModelColumnDO> getDpModelColumnList();
    List<StandardsModelColumnDO> getDpModelColumnList(StandardsModelColumnSaveReqVO createReqVO);

    long countByDpModelColumn(StandardsModelColumnSaveReqVO createReqVO);

    /**
     * 获得全部逻辑模型属性信息 Map
     *
     * @return 逻辑模型属性信息 Map
     */
    Map<Long, StandardsModelColumnDO> getDpModelColumnMap();


    /**
     * 导入逻辑模型属性信息数据
     *
     * @param importExcelList 逻辑模型属性信息数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDpModelColumn(List<StandardsModelColumnRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 批量插入逻辑模型属性信息数据
     *
     * @param StandardsModelColumnList 逻辑模型属性信息数据列表
     * @return 结果
     */
    Boolean createDpModelColumnList(List<StandardsModelColumnSaveReqVO> StandardsModelColumnList);

    /**
     * 批量修改和插入逻辑模型属性信息数据
     *
     * @param StandardsModelColumnList 逻辑模型属性信息数据列表
     * @return 结果
     */
    Boolean updateDpModelColumnList(List<StandardsModelColumnSaveReqVO> StandardsModelColumnList);
}
