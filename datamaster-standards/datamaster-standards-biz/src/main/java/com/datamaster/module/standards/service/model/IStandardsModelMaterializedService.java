package com.datamaster.module.standards.service.model;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsMaterializedMethodReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedPageReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelMaterializedDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 物化模型记录Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IStandardsModelMaterializedService extends IService<StandardsModelMaterializedDO> {

    /**
     * 获得物化模型记录分页列表
     *
     * @param pageReqVO 分页请求
     * @return 物化模型记录分页列表
     */
    PageResult<StandardsModelMaterializedDO> getDpModelMaterializedPage(StandardsModelMaterializedPageReqVO pageReqVO);

    /**
     * 创建物化模型记录
     *
     * @param createReqVO 物化模型记录信息
     * @return 物化模型记录编号
     */
    Long createDpModelMaterialized(StandardsModelMaterializedSaveReqVO createReqVO);

    /**
     * 更新物化模型记录
     *
     * @param updateReqVO 物化模型记录信息
     */
    int updateDpModelMaterialized(StandardsModelMaterializedSaveReqVO updateReqVO);

    /**
     * 删除物化模型记录
     *
     * @param idList 物化模型记录编号
     */
    int removeDpModelMaterialized(Collection<Long> idList);

    /**
     * 获得物化模型记录详情
     *
     * @param id 物化模型记录编号
     * @return 物化模型记录
     */
    StandardsModelMaterializedDO getDpModelMaterializedById(Long id);

    /**
     * 获得全部物化模型记录列表
     *
     * @return 物化模型记录列表
     */
    List<StandardsModelMaterializedDO> getDpModelMaterializedList();

    /**
     * 获得全部物化模型记录 Map
     *
     * @return 物化模型记录 Map
     */
    Map<Long, StandardsModelMaterializedDO> getDpModelMaterializedMap();


    /**
     * 导入物化模型记录数据
     *
     * @param importExcelList 物化模型记录数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDpModelMaterialized(List<StandardsModelMaterializedRespVO> importExcelList, boolean isUpdateSupport, String operName);

    Long createMaterializedTable(StandardsMaterializedMethodReqVO StandardsModelMaterialized);
}
