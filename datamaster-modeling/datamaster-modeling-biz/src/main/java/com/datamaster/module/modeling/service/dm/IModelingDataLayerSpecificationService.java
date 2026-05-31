

package com.datamaster.module.modeling.service.dm;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationSaveReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationPageReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerSpecificationDO;
/**
 * 数仓分层-规范管理Service接口
 *
 * @author FXB
 * @date 2026-03-24
 */
public interface IModelingDataLayerSpecificationService extends IService<ModelingDataLayerSpecificationDO> {

    /**
     * 获得数仓分层-规范管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数仓分层-规范管理分页列表
     */
    PageResult<ModelingDataLayerSpecificationDO> getModelingDataLayerSpecificationPage(ModelingDataLayerSpecificationPageReqVO pageReqVO);

    /**
     * 创建数仓分层-规范管理
     *
     * @param createReqVO 数仓分层-规范管理信息
     * @return 数仓分层-规范管理编号
     */
    Long createModelingDataLayerSpecification(ModelingDataLayerSpecificationSaveReqVO createReqVO);

    /**
     * 更新数仓分层-规范管理
     *
     * @param updateReqVO 数仓分层-规范管理信息
     */
    int updateModelingDataLayerSpecification(ModelingDataLayerSpecificationSaveReqVO updateReqVO);

    /**
     * 删除数仓分层-规范管理
     *
     * @param idList 数仓分层-规范管理编号
     */
    int removeModelingDataLayerSpecification(Collection<Long> idList);

    /**
     * 获得数仓分层-规范管理详情
     *
     * @param id 数仓分层-规范管理编号
     * @return 数仓分层-规范管理
     */
    ModelingDataLayerSpecificationDO getModelingDataLayerSpecificationById(Long id);

    /**
     * 获得全部数仓分层-规范管理列表
     *
     * @return 数仓分层-规范管理列表
     */
    List<ModelingDataLayerSpecificationDO> getModelingDataLayerSpecificationPage();

    /**
     * 获得全部数仓分层-规范管理 Map
     *
     * @return 数仓分层-规范管理 Map
     */
    Map<Long, ModelingDataLayerSpecificationDO> getModelingDataLayerSpecificationMap();


    /**
     * 导入数仓分层-规范管理数据
     *
     * @param importExcelList 数仓分层-规范管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importModelingDataLayerSpecification(List<ModelingDataLayerSpecificationRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
