package com.datamaster.module.standards.service.sensitiveLevel;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.sensitiveLevel.vo.StandardsSensitiveLevelPageReqVO;
import com.datamaster.module.standards.controller.admin.sensitiveLevel.vo.StandardsSensitiveLevelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.sensitiveLevel.StandardsSensitiveLevelDO;

import java.util.Collection;

/**
 * 敏感等级Service接口
 *
 * @author Chaos
 * @date 2025-01-21
 */
public interface IStandardsSensitiveLevelService extends IService<StandardsSensitiveLevelDO> {

    /**
     * 获得敏感等级分页列表
     *
     * @param pageReqVO 分页请求
     * @return 敏感等级分页列表
     */
    PageResult<StandardsSensitiveLevelDO> getDgSensitiveLevelPage(StandardsSensitiveLevelPageReqVO pageReqVO);

    /**
     * 创建敏感等级
     *
     * @param createReqVO 敏感等级信息
     * @return 敏感等级编号
     */
    Long createDgSensitiveLevel(StandardsSensitiveLevelSaveReqVO createReqVO);

    /**
     * 更新敏感等级
     *
     * @param updateReqVO 敏感等级信息
     */
    int updateDgSensitiveLevel(StandardsSensitiveLevelSaveReqVO updateReqVO);

    /**
     * 删除敏感等级
     *
     * @param idList 敏感等级编号
     */
    int removeDgSensitiveLevel(Collection<Long> idList);

    /**
     * 获得敏感等级详情
     *
     * @param id 敏感等级编号
     * @return 敏感等级
     */
    StandardsSensitiveLevelDO getDgSensitiveLevelById(Long id);

    /**
     * 修改状态
     *
     * @param id     主键
     * @param status 状态值
     * @return
     */
    Boolean updateStatus(Long id, Long status);
}
