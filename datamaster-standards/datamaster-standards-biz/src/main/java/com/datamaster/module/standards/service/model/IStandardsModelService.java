package com.datamaster.module.standards.service.model;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelSaveReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelPageReqVO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelDO;
/**
 * 逻辑模型Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IStandardsModelService extends IService<StandardsModelDO> {

    /**
     * 获得逻辑模型分页列表
     *
     * @param pageReqVO 分页请求
     * @return 逻辑模型分页列表
     */
    PageResult<StandardsModelDO> getDpModelPage(StandardsModelPageReqVO pageReqVO);

    /**
     * 创建逻辑模型
     *
     * @param createReqVO 逻辑模型信息
     * @return 逻辑模型编号
     */
    Long createDpModel(StandardsModelSaveReqVO createReqVO);

    /**
     * 更新逻辑模型
     *
     * @param updateReqVO 逻辑模型信息
     */
    int updateDpModel(StandardsModelSaveReqVO updateReqVO);

    /**
     * 删除逻辑模型
     *
     * @param idList 逻辑模型编号
     */
    int removeDpModel(Collection<Long> idList);

    /**
     * 获得逻辑模型详情
     *
     * @param id 逻辑模型编号
     * @return 逻辑模型
     */
    StandardsModelDO getDpModelById(Long id);

    /**
     * 获得全部逻辑模型列表
     *
     * @return 逻辑模型列表
     */
    List<StandardsModelDO> getDpModelList();

    /**
     * 获得全部逻辑模型 Map
     *
     * @return 逻辑模型 Map
     */
    Map<Long, StandardsModelDO> getDpModelMap();


    /**
     * 导入逻辑模型数据
     *
     * @param importExcelList 逻辑模型数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDpModel(List<StandardsModelRespVO> importExcelList, boolean isUpdateSupport, String operName);

    int removeDpModelAndColumnAll(List<Long> asList);

    Boolean updateStatus(Long id, Long status);
}
