

package com.datamaster.module.standards.service.whitelist;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistRespVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistSaveReqVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistPageReqVO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeWhitelistDO;
/**
 * 脱敏白名单Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
public interface IStandardsDesensitizeWhitelistService extends IService<StandardsDesensitizeWhitelistDO> {

    /**
     * 获得脱敏白名单分页列表
     *
     * @param pageReqVO 分页请求
     * @return 脱敏白名单分页列表
     */
    PageResult<StandardsDesensitizeWhitelistDO> getDgDesensitizeWhitelistPage(StandardsDesensitizeWhitelistPageReqVO pageReqVO);

    /**
     * 创建脱敏白名单
     *
     * @param createReqVO 脱敏白名单信息
     * @return 脱敏白名单编号
     */
    Long createDgDesensitizeWhitelist(StandardsDesensitizeWhitelistSaveReqVO createReqVO);

    /**
     * 更新脱敏白名单
     *
     * @param updateReqVO 脱敏白名单信息
     */
    int updateDgDesensitizeWhitelist(StandardsDesensitizeWhitelistSaveReqVO updateReqVO);

    /**
     * 删除脱敏白名单
     *
     * @param idList 脱敏白名单编号
     */
    int removeDgDesensitizeWhitelist(Collection<Long> idList);

    /**
     * 获得脱敏白名单详情
     *
     * @param id 脱敏白名单编号
     * @return 脱敏白名单
     */
    StandardsDesensitizeWhitelistDO getDgDesensitizeWhitelistById(Long id);

    /**
     * 获得全部脱敏白名单列表
     *
     * @return 脱敏白名单列表
     */
    List<StandardsDesensitizeWhitelistDO> getDgDesensitizeWhitelistList();

    /**
     * 获得全部脱敏白名单 Map
     *
     * @return 脱敏白名单 Map
     */
    Map<Long, StandardsDesensitizeWhitelistDO> getDgDesensitizeWhitelistMap();


    /**
     * 导入脱敏白名单数据
     *
     * @param importExcelList 脱敏白名单数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDgDesensitizeWhitelist(List<StandardsDesensitizeWhitelistRespVO> importExcelList, boolean isUpdateSupport, String operName);

    //根据分类ID查询脱敏白名单
    StandardsDesensitizeWhitelistDO getDgDesensitizeWhitelistByCategoryId(Long categoryId);
}
