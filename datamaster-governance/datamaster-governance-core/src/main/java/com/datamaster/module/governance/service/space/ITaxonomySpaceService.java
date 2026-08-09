

package com.datamaster.module.governance.service.space;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpacePageReqVO;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpaceRespVO;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpaceSaveReqVO;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySysUserReqVO;
import com.datamaster.module.governance.dal.dataobject.space.TaxonomySpaceDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 空间 Service 接口
 *
 * @author shu
 * @date 2025-01-20
 */
public interface ITaxonomySpaceService extends IService<TaxonomySpaceDO> {

    /**
     * 获得空间分页列表
     *
     * @param pageReqVO 分页请求
     * @return 空间分页列表
     */
    PageResult<TaxonomySpaceDO> getSpacePage(TaxonomySpacePageReqVO pageReqVO);

    /**
     * 创建空间
     *
     * @param createReqVO 空间信息
     * @return 空间编号
     */
    Long createSpace(TaxonomySpaceSaveReqVO createReqVO);

    /**
     * 更新空间
     *
     * @param updateReqVO 空间信息
     */
    int updateSpace(TaxonomySpaceSaveReqVO updateReqVO);

    /**
     * 删除空间
     *
     * @param idList 空间编号
     */
    int removeSpace(Collection<Long> idList);

    /**
     * 获得空间详情
     *
     * @param id 空间编号
     * @return 空间
     */
    TaxonomySpaceDO getSpaceById(Long id);

    /**
     * 获得全部空间列表
     *
     * @return 空间列表
     */
    List<TaxonomySpaceDO> getSpaceList();

    /**
     * 获得全部空间 Map
     *
     * @return 空间 Map
     */
    Map<Long, TaxonomySpaceDO> getSpaceMap();


    /**
     * 导入空间数据
     *
     * @param importExcelList 空间数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importSpace(List<TaxonomySpaceRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 判断当前用户是否可维护空间成员
     *
     * @param userId 用户 ID
     * @return
     */
    JSONObject checkUserSpaceManagePermission(Long userId, Long id);

    /**
     * 查询当前用户所属的空间列表
     *
     * @param userId 用户id
     * @return
     */
    List<TaxonomySpaceDO> getCurrentUserSpaceList(Long userId);

    /**
     * 查询尚未加入当前空间的用户列表
     */
    List<SysUser> selectUsersNotInSpace(TaxonomySysUserReqVO user);

    Boolean updateSpaceStatus(Long id, Long status);
}
