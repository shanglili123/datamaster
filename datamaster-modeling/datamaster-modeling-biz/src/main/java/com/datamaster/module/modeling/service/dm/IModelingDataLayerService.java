

package com.datamaster.module.modeling.service.dm;

import java.util.List;
import java.util.Map;
import java.util.Collection;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSaveReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerTreeRespVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerDO;

/**
 * 数仓分层管理Service接口
 *
 * @author FXB
 * @date 2026-03-24
 */
public interface IModelingDataLayerService extends IService<ModelingDataLayerDO> {

    /**
     * 获得数仓分层管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数仓分层管理分页列表
     */
    PageResult<ModelingDataLayerDO> getModelingDataLayerPage(ModelingDataLayerPageReqVO pageReqVO);

    /**
     * 创建数仓分层管理
     *
     * @param createReqVO 数仓分层管理信息
     * @return 数仓分层管理编号
     */
    Long createModelingDataLayer(ModelingDataLayerSaveReqVO createReqVO);

    /**
     * 更新数仓分层管理
     *
     * @param updateReqVO 数仓分层管理信息
     */
    int updateModelingDataLayer(ModelingDataLayerSaveReqVO updateReqVO);

    /**
     * 删除数仓分层管理
     *
     * @param idList 数仓分层管理编号
     */
    int removeModelingDataLayer(Collection<Long> idList);

    /**
     * 获得数仓分层管理详情
     *
     * @param id 数仓分层管理编号
     * @return 数仓分层管理
     */
    ModelingDataLayerDO getModelingDataLayerById(Long id);

    /**
     * 获得全部数仓分层管理列表
     *
     * @return 数仓分层管理列表
     */
    List<ModelingDataLayerDO> getModelingDataLayerList();

    /**
     * 获得全部数仓分层管理 Map
     *
     * @return 数仓分层管理 Map
     */
    Map<Long, ModelingDataLayerDO> getModelingDataLayerMap();


    /**
     * 导入数仓分层管理数据
     *
     * @param importExcelList 数仓分层管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importModelingDataLayer(List<ModelingDataLayerRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 获取数仓分层管理树列表
     *
     * @return 数仓分层管理树列表
     */
    List<ModelingDataLayerTreeRespVO> tree();
}
