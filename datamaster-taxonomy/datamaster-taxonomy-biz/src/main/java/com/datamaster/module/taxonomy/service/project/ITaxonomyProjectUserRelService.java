

package com.datamaster.module.taxonomy.service.project;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectUserRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 项目与用户关联关系Service接口
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
public interface ITaxonomyProjectUserRelService extends IService<TaxonomyProjectUserRelDO> {

    /**
     * 获得项目与用户关联关系分页列表
     *
     * @param pageReqVO 分页请求
     * @return 项目与用户关联关系分页列表
     */
    PageResult<TaxonomyProjectUserRelDO> getAttProjectUserRelPage(TaxonomyProjectUserRelPageReqVO pageReqVO);

    /**
     * 创建项目与用户关联关系
     *
     * @param createReqVO 项目与用户关联关系信息
     * @return 项目与用户关联关系编号
     */
    Long createAttProjectUserRel(TaxonomyProjectUserRelSaveReqVO createReqVO);

    /**
     * 更新项目与用户关联关系
     *
     * @param updateReqVO 项目与用户关联关系信息
     */
    int updateAttProjectUserRel(TaxonomyProjectUserRelSaveReqVO updateReqVO);

    /**
     * 更新项目与用户关联关系
     *
     * @param updateReqVO 项目与用户关联关系信息
     */
    int updateUserListAndRoleList(TaxonomyProjectUserRelSaveReqVO updateReqVO);

    /**
     * 删除项目与用户关联关系
     *
     * @param idList 项目与用户关联关系编号
     */
    int removeAttProjectUserRel(Collection<Long> idList);

    /**
     * 获得项目与用户关联关系详情
     *
     * @param id 项目与用户关联关系编号
     * @return 项目与用户关联关系
     */
    TaxonomyProjectUserRelDO getAttProjectUserRelById(Long id);

    /**
     * 获得全部项目与用户关联关系列表
     *
     * @return 项目与用户关联关系列表
     */
    List<TaxonomyProjectUserRelDO> getAttProjectUserRelList();

    /**
     * 获得全部项目与用户关联关系 Map
     *
     * @return 项目与用户关联关系 Map
     */
    Map<Long, TaxonomyProjectUserRelDO> getAttProjectUserRelMap();


    /**
     * 导入项目与用户关联关系数据
     *
     * @param importExcelList 项目与用户关联关系数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importAttProjectUserRel(List<TaxonomyProjectUserRelRespVO> importExcelList, boolean isUpdateSupport, String operName);


    /**
     * 创建项目与用户关联关系前端传用户集合和角色集合
     *
     * @param TaxonomyProject 项目信息
     * @return 项目编号
     */
    Boolean createUserListAndRoleList(TaxonomyProjectUserRelSaveReqVO TaxonomyProject);

    /**
     * 获取项目与用户关联关系详细信息包括角色信息
     *
     * @param id
     * @return
     */
    TaxonomyProjectUserRelRespVO getRoleUser(Long id);
}
