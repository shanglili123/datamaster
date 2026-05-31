

package com.datamaster.module.modeling.service.businessCategory;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelPageReqVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelRespVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessDomainRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 业务分类数据域关联关系Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
public interface IModelingBusinessDomainRelService extends IService<ModelingBusinessDomainRelDO> {

    /**
     * 获得业务分类数据域关联关系分页列表
     *
     * @param pageReqVO 分页请求
     * @return 业务分类数据域关联关系分页列表
     */
    PageResult<ModelingBusinessDomainRelDO> getModelingBusinessDomainRelPage(ModelingBusinessDomainRelPageReqVO pageReqVO);

    /**
     * 创建业务分类数据域关联关系
     *
     * @param createReqVO 业务分类数据域关联关系信息
     * @return 业务分类数据域关联关系编号
     */
    Long createModelingBusinessDomainRel(ModelingBusinessDomainRelSaveReqVO createReqVO);

    /**
     * 更新业务分类数据域关联关系
     *
     * @param updateReqVO 业务分类数据域关联关系信息
     */
    int updateModelingBusinessDomainRel(ModelingBusinessDomainRelSaveReqVO updateReqVO);

    /**
     * 删除业务分类数据域关联关系
     *
     * @param idList 业务分类数据域关联关系编号
     */
    int removeModelingBusinessDomainRel(Collection<Long> idList);

    /**
     * 获得业务分类数据域关联关系详情
     *
     * @param id 业务分类数据域关联关系编号
     * @return 业务分类数据域关联关系
     */
    ModelingBusinessDomainRelDO getModelingBusinessDomainRelById(Long id);

    /**
     * 获得全部业务分类数据域关联关系列表
     *
     * @return 业务分类数据域关联关系列表
     */
    List<ModelingBusinessDomainRelDO> getModelingBusinessDomainRelList();

    /**
     * 获得全部业务分类数据域关联关系 Map
     *
     * @return 业务分类数据域关联关系 Map
     */
    Map<Long, ModelingBusinessDomainRelDO> getModelingBusinessDomainRelMap();


    /**
     * 导入业务分类数据域关联关系数据
     *
     * @param importExcelList 业务分类数据域关联关系数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importModelingBusinessDomainRel(List<ModelingBusinessDomainRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

    Integer removeModelingBusinessDomainRelByDomainId(Long domainId, Long businessCategoryId);
}
