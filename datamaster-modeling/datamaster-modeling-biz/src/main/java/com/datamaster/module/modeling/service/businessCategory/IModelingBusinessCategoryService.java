

package com.datamaster.module.modeling.service.businessCategory;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategoryPageReqVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategoryRespVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategorySaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessCategoryDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 业务分类Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-08
 */
public interface IModelingBusinessCategoryService extends IService<ModelingBusinessCategoryDO> {

    /**
     * 获得业务分类分页列表
     *
     * @param pageReqVO 分页请求
     * @return 业务分类分页列表
     */
    PageResult<ModelingBusinessCategoryDO> getModelingBusinessCategoryPage(ModelingBusinessCategoryPageReqVO pageReqVO);

    /**
     * 创建业务分类
     *
     * @param createReqVO 业务分类信息
     * @return 业务分类编号
     */
    Long createModelingBusinessCategory(ModelingBusinessCategorySaveReqVO createReqVO);

    /**
     * 更新业务分类
     *
     * @param updateReqVO 业务分类信息
     */
    int updateModelingBusinessCategory(ModelingBusinessCategorySaveReqVO updateReqVO);

    /**
     * 删除业务分类
     *
     * @param idList 业务分类编号
     */
    int removeModelingBusinessCategory(Collection<Long> idList);

    /**
     * 获得业务分类详情
     *
     * @param id 业务分类编号
     * @return 业务分类
     */
    ModelingBusinessCategoryDO getModelingBusinessCategoryById(Long id);

    /**
     * 获得全部业务分类列表
     *
     * @return 业务分类列表
     */
    List<ModelingBusinessCategoryDO> getModelingBusinessCategoryList(ModelingBusinessCategoryPageReqVO ModelingBusinessCategory);

    /**
     * 获得全部业务分类 Map
     *
     * @return 业务分类 Map
     */
    Map<Long, ModelingBusinessCategoryDO> getModelingBusinessCategoryMap();


    /**
     * 导入业务分类数据
     *
     * @param importExcelList 业务分类数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importModelingBusinessCategory(List<ModelingBusinessCategoryRespVO> importExcelList, boolean isUpdateSupport, String operName);

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
}
