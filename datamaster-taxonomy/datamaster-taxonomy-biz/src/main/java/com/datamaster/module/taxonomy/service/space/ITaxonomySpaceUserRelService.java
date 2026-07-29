

package com.datamaster.module.taxonomy.service.space;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceUserRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceUserRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceUserRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.space.TaxonomySpaceUserRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 空间与用户关联关系 Service 接口
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
public interface ITaxonomySpaceUserRelService extends IService<TaxonomySpaceUserRelDO> {

    /**
     * 获得空间成员关系分页列表
     *
     * @param pageReqVO 分页请求
     * @return 空间成员关系分页列表
     */
    PageResult<TaxonomySpaceUserRelDO> getSpaceUserRelPage(TaxonomySpaceUserRelPageReqVO pageReqVO);

    /**
     * 创建空间成员关系
     *
     * @param createReqVO 空间成员关系信息
     * @return 空间成员关系编号
     */
    Long createSpaceUserRel(TaxonomySpaceUserRelSaveReqVO createReqVO);

    /**
     * 更新空间成员与角色关系
     *
     * @param updateReqVO 空间成员关系信息
     */
    int updateSpaceUserRel(TaxonomySpaceUserRelSaveReqVO updateReqVO);

    /**
     * 更新空间成员与角色关系
     *
     * @param updateReqVO 空间成员与角色信息
     */
    int updateSpaceUsersAndRoles(TaxonomySpaceUserRelSaveReqVO updateReqVO);

    /**
     * 删除空间成员关系
     *
     * @param idList 空间成员关系编号
     */
    int removeSpaceUserRel(Collection<Long> idList);

    /**
     * 获得空间成员关系详情
     *
     * @param id 空间成员关系编号
     * @return 空间成员关系
     */
    TaxonomySpaceUserRelDO getSpaceUserRelById(Long id);

    /**
     * 获得全部空间成员关系列表
     *
     * @return 空间成员关系列表
     */
    List<TaxonomySpaceUserRelDO> getSpaceUserRelList();

    /**
     * 获得全部空间成员关系 Map
     *
     * @return 空间成员关系 Map
     */
    Map<Long, TaxonomySpaceUserRelDO> getSpaceUserRelMap();


    /**
     * 导入空间成员关系数据
     *
     * @param importExcelList 空间成员关系数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importSpaceUserRel(List<TaxonomySpaceUserRelRespVO> importExcelList, boolean isUpdateSupport, String operName);


    /**
     * 批量创建空间成员与角色关系
     *
     * @param saveReqVO 空间成员与角色信息
     * @return 是否创建成功
     */
    Boolean createSpaceUsersAndRoles(TaxonomySpaceUserRelSaveReqVO saveReqVO);

    /**
     * 获取空间成员详情，包括角色信息
     *
     * @param id 空间成员关系 ID
     * @return 空间成员详情
     */
    TaxonomySpaceUserRelRespVO getSpaceUserRoleDetail(Long id);
}
