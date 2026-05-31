

package com.datamaster.module.standards.service.desensitizeRules;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRuleRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRuleSaveReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRulePageReqVO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeRuleDO;
/**
 * 脱敏规则Service接口
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
public interface IStandardsDesensitizeRuleService extends IService<StandardsDesensitizeRuleDO> {

    /**
     * 获得脱敏规则分页列表
     *
     * @param pageReqVO 分页请求
     * @return 脱敏规则分页列表
     */
    PageResult<StandardsDesensitizeRuleDO> getDgDesensitizeRulePage(StandardsDesensitizeRulePageReqVO pageReqVO);

    /**
     * 创建脱敏规则
     *
     * @param createReqVO 脱敏规则信息
     * @return 脱敏规则编号
     */
    Long createDgDesensitizeRule(StandardsDesensitizeRuleSaveReqVO createReqVO);

    /**
     * 更新脱敏规则
     *
     * @param updateReqVO 脱敏规则信息
     */
    int updateDgDesensitizeRule(StandardsDesensitizeRuleSaveReqVO updateReqVO);

    /**
     * 删除脱敏规则
     *
     * @param idList 脱敏规则编号
     */
    int removeDgDesensitizeRule(Collection<Long> idList);

    /**
     * 获得脱敏规则详情
     *
     * @param id 脱敏规则编号
     * @return 脱敏规则
     */
    StandardsDesensitizeRuleDO getDgDesensitizeRuleById(Long id);

    /**
     * 获得全部脱敏规则列表
     *
     * @return 脱敏规则列表
     */
    List<StandardsDesensitizeRuleDO> getDgDesensitizeRuleList();

    /**
     * 获得全部脱敏规则 Map
     *
     * @return 脱敏规则 Map
     */
    Map<Long, StandardsDesensitizeRuleDO> getDgDesensitizeRuleMap();


    /**
     * 导入脱敏规则数据
     *
     * @param importExcelList 脱敏规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDgDesensitizeRule(List<StandardsDesensitizeRuleRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 根据分类id获取规则数量
     *
     * @param idList 分类id数组
     * @return 规则数量
     */
    Long getCountByCategoryIds(Collection<Long> idList);
    StandardsDesensitizeRuleDO getDgDesensitizeRuleByDataCategoryId(Long dataCategoryId);
}
