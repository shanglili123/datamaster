

package com.datamaster.module.modeling.service.dm;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataDomainDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据域管理Service接口
 *
 * @author FXB
 * @date 2026-03-24
 */
public interface IModelingDataDomainService extends IService<ModelingDataDomainDO> {

    /**
     * 获得数据域管理分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据域管理分页列表
     */
    PageResult<ModelingDataDomainDO> getModelingDataDomainPage(ModelingDataDomainPageReqVO pageReqVO);

    /**
     * 创建数据域管理
     *
     * @param createReqVO 数据域管理信息
     * @return 数据域管理编号
     */
    Long createModelingDataDomain(ModelingDataDomainSaveReqVO createReqVO);

    /**
     * 更新数据域管理
     *
     * @param updateReqVO 数据域管理信息
     */
    int updateModelingDataDomain(ModelingDataDomainSaveReqVO updateReqVO);

    /**
     * 删除数据域管理
     *
     * @param idList 数据域管理编号
     */
    int removeModelingDataDomain(Collection<Long> idList);

    /**
     * 获得数据域管理详情
     *
     * @param id 数据域管理编号
     * @return 数据域管理
     */
    ModelingDataDomainDO getModelingDataDomainById(Long id);

    /**
     * 获得全部数据域管理列表
     *
     * @return 数据域管理列表
     */
    List<ModelingDataDomainDO> getModelingDataDomainList();

    /**
     * 获得全部数据域管理 Map
     *
     * @return 数据域管理 Map
     */
    Map<Long, ModelingDataDomainDO> getModelingDataDomainMap();


    /**
     * 导入数据域管理数据
     *
     * @param importExcelList 数据域管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importModelingDataDomain(List<ModelingDataDomainRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 根据业务分类id查询数据域
     *
     * @param ModelingDataDomain
     * @return
     */
    PageResult<ModelingDataDomainDO> getModelingDataDomainByCategoryId(ModelingDataDomainPageReqVO ModelingDataDomain);
}
