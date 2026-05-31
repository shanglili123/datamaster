

package com.datamaster.module.taxonomy.service.project;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectRespVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectSaveReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomySysUserReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 项目Service接口
 *
 * @author shu
 * @date 2025-01-20
 */
public interface ITaxonomyProjectService extends IService<TaxonomyProjectDO> {

    /**
     * 获得项目分页列表
     *
     * @param pageReqVO 分页请求
     * @return 项目分页列表
     */
    PageResult<TaxonomyProjectDO> getAttProjectPage(TaxonomyProjectPageReqVO pageReqVO);

    /**
     * 创建项目
     *
     * @param createReqVO 项目信息
     * @return 项目编号
     */
    Long createAttProject(TaxonomyProjectSaveReqVO createReqVO);

    /**
     * 更新项目
     *
     * @param updateReqVO 项目信息
     */
    int updateAttProject(TaxonomyProjectSaveReqVO updateReqVO);

    /**
     * 删除项目
     *
     * @param idList 项目编号
     */
    int removeAttProject(Collection<Long> idList);

    /**
     * 获得项目详情
     *
     * @param id 项目编号
     * @return 项目
     */
    TaxonomyProjectDO getAttProjectById(Long id);

    /**
     * 获得全部项目列表
     *
     * @return 项目列表
     */
    List<TaxonomyProjectDO> getAttProjectList();

    /**
     * 获得全部项目 Map
     *
     * @return 项目 Map
     */
    Map<Long, TaxonomyProjectDO> getAttProjectMap();


    /**
     * 导入项目数据
     *
     * @param importExcelList 项目数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importAttProject(List<TaxonomyProjectRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 获取当前用户是非具备用户添加和项目管理员
     *
     * @param userId 用户ID
     * @return
     */
    JSONObject addUserAndProjectIsOk(Long userId, Long id);

    /**
     * 查询当前用户所属的项目列表
     *
     * @param userId 用户id
     * @return
     */
    List<TaxonomyProjectDO> getCurrentUserList(Long userId);

    /**
     * 获取用户列表排除当前项目已经存在的用户
     */
    List<SysUser> selectNoProjectUserList(TaxonomySysUserReqVO user);

    Boolean editProjectStatus(Long id,Long status);
}
