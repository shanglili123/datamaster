

package com.datamaster.module.standards.service.desensitizeRules;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalSaveReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalPageReqVO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;
/**
 * 脱敏区间Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
public interface IStandardsDesensitizeIntervalService extends IService<StandardsDesensitizeIntervalDO> {

    /**
     * 获得脱敏区间分页列表
     *
     * @param pageReqVO 分页请求
     * @return 脱敏区间分页列表
     */
    PageResult<StandardsDesensitizeIntervalDO> getDgDesensitizeIntervalPage(StandardsDesensitizeIntervalPageReqVO pageReqVO);

    /**
     * 创建脱敏区间
     *
     * @param createReqVO 脱敏区间信息
     * @return 脱敏区间编号
     */
    Long createDgDesensitizeInterval(StandardsDesensitizeIntervalSaveReqVO createReqVO);

    /**
     * 更新脱敏区间
     *
     * @param updateReqVO 脱敏区间信息
     */
    int updateDgDesensitizeInterval(StandardsDesensitizeIntervalSaveReqVO updateReqVO);

    /**
     * 删除脱敏区间
     *
     * @param idList 脱敏区间编号
     */
    int removeDgDesensitizeInterval(Collection<Long> idList);

    /**
     * 获得脱敏区间详情
     *
     * @param id 脱敏区间编号
     * @return 脱敏区间
     */
    StandardsDesensitizeIntervalDO getDgDesensitizeIntervalById(Long id);

    /**
     * 获得全部脱敏区间列表
     *
     * @return 脱敏区间列表
     */
    List<StandardsDesensitizeIntervalDO> getDgDesensitizeIntervalList();

    /**
     * 获得全部脱敏区间 Map
     *
     * @return 脱敏区间 Map
     */
    Map<Long, StandardsDesensitizeIntervalDO> getDgDesensitizeIntervalMap();


    /**
     * 导入脱敏区间数据
     *
     * @param importExcelList 脱敏区间数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDgDesensitizeInterval(List<StandardsDesensitizeIntervalRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
