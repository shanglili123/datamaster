

package com.datamaster.module.standards.service.dataCategory;

import java.util.List;
import java.util.Map;
import java.util.Collection;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategoryRespVO;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategorySaveReqVO;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategoryPageReqVO;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.StandardsDataCategoryTreeRespVO;
import com.datamaster.module.standards.dal.dataobject.dataCategory.StandardsDataCategoryDO;

/**
 * 数据分类Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
public interface IStandardsDataCategoryService extends IService<StandardsDataCategoryDO> {

    /**
     * 获得数据分类分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据分类分页列表
     */
    PageResult<StandardsDataCategoryDO> getDgDataCategoryPage(StandardsDataCategoryPageReqVO pageReqVO);

    /**
     * 创建数据分类
     *
     * @param createReqVO 数据分类信息
     * @return 数据分类编号
     */
    Long createDgDataCategory(StandardsDataCategorySaveReqVO createReqVO);

    /**
     * 更新数据分类
     *
     * @param updateReqVO 数据分类信息
     */
    int updateDgDataCategory(StandardsDataCategorySaveReqVO updateReqVO);

    /**
     * 删除数据分类
     *
     * @param idList 数据分类编号
     */
    int removeDgDataCategory(Collection<Long> idList);

    /**
     * 获得数据分类详情
     *
     * @param id 数据分类编号
     * @return 数据分类
     */
    StandardsDataCategoryDO getDgDataCategoryById(Long id);

    /**
     * 获得全部数据分类列表
     *
     * @return 数据分类列表
     */
    List<StandardsDataCategoryDO> getDgDataCategoryList();


    /**
     * 获得全部数据分类 Map
     *
     * @return 数据分类 Map
     */
    Map<Long, StandardsDataCategoryDO> getDgDataCategoryMap();


    /**
     * 导入数据分类数据
     *
     * @param importExcelList 数据分类数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importDgDataCategory(List<StandardsDataCategoryRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 获取数据分类树列表
     *
     * @return 树列表
     */
    List<StandardsDataCategoryTreeRespVO> selectTree(String type);

    /**
     * 根据分类编码获取数量
     *
     * @param catCode
     * @return
     */
    Long getCountByCatCode(String catCode);

    /**
     * 将老的 CAT_CODE 批量更新成新的 CAT_CODE
     *
     * @param oldCatCode 旧分类编码
     * @param newCatCode 新分类编码
     * @return 受影响行数
     */
    int updateCatCode(String codeOld, String codeNew);

    List<StandardsDataCategoryDO> getDgDataCategoryList(StandardsDataCategoryPageReqVO StandardsDataCategory);
}
