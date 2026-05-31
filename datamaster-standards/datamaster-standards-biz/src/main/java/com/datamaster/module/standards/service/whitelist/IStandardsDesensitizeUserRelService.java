

package com.datamaster.module.standards.service.whitelist;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelRespVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelSaveReqVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelPageReqVO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeUserRelDO;
/**
 * 脱敏白名单与用户关联关系Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
public interface IStandardsDesensitizeUserRelService extends IService<StandardsDesensitizeUserRelDO> {

    /**
     * 获得脱敏白名单与用户关联关系分页列表
     *
     * @param pageReqVO 分页请求
     * @return 脱敏白名单与用户关联关系分页列表
     */
    PageResult<StandardsDesensitizeUserRelDO> getDgDesensitizeUserRelPage(StandardsDesensitizeUserRelPageReqVO pageReqVO);

    /**
     * 创建脱敏白名单与用户关联关系
     *
     * @param createReqVO 脱敏白名单与用户关联关系信息
     * @return 脱敏白名单与用户关联关系编号
     */
    Long createDgDesensitizeUserRel(StandardsDesensitizeUserRelSaveReqVO createReqVO);

    /**
     * 更新脱敏白名单与用户关联关系
     *
     * @param updateReqVO 脱敏白名单与用户关联关系信息
     */
    int updateDgDesensitizeUserRel(StandardsDesensitizeUserRelSaveReqVO updateReqVO);

    /**
     * 删除脱敏白名单与用户关联关系
     *
     * @param idList 脱敏白名单与用户关联关系编号
     */
    int removeDgDesensitizeUserRel(Collection<Long> idList);

    /**
     * 获得脱敏白名单与用户关联关系详情
     *
     * @param id 脱敏白名单与用户关联关系编号
     * @return 脱敏白名单与用户关联关系
     */
    StandardsDesensitizeUserRelDO getDgDesensitizeUserRelById(Long id);

    /**
     * 获得全部脱敏白名单与用户关联关系列表
     *
     * @return 脱敏白名单与用户关联关系列表
     */
    List<StandardsDesensitizeUserRelDO> getDgDesensitizeUserRelList();

    /**
     * 获得全部脱敏白名单与用户关联关系 Map
     *
     * @return 脱敏白名单与用户关联关系 Map
     */
    Map<Long, StandardsDesensitizeUserRelDO> getDgDesensitizeUserRelMap();


    /**
     * 导入脱敏白名单与用户关联关系数据
     *
     * @param importExcelList 脱敏白名单与用户关联关系数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDgDesensitizeUserRel(List<StandardsDesensitizeUserRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
