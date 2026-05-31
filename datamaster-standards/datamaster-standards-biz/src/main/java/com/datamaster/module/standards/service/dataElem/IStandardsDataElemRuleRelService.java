package com.datamaster.module.standards.service.dataElem;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemRuleRelDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据元数据规则关联信息Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IStandardsDataElemRuleRelService extends IService<StandardsDataElemRuleRelDO> {

    /**
     * 获得数据元数据规则关联信息分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据元数据规则关联信息分页列表
     */
    PageResult<StandardsDataElemRuleRelDO> getDpDataElemRuleRelPage(StandardsDataElemRuleRelPageReqVO pageReqVO);

    /**
     * 创建数据元数据规则关联信息
     *
     * @param createReqVO 数据元数据规则关联信息信息
     * @return 数据元数据规则关联信息编号
     */
    Long createDpDataElemRuleRel(StandardsDataElemRuleRelSaveReqVO createReqVO);

    /**
     * 更新数据元数据规则关联信息
     *
     * @param updateReqVO 数据元数据规则关联信息信息
     */
    int updateDpDataElemRuleRel(StandardsDataElemRuleRelSaveReqVO updateReqVO);

    /**
     * 删除数据元数据规则关联信息
     *
     * @param idList 数据元数据规则关联信息编号
     */
    int removeDpDataElemRuleRel(Collection<Long> idList);

    /**
     * 获得数据元数据规则关联信息详情
     *
     * @param id 数据元数据规则关联信息编号
     * @return 数据元数据规则关联信息
     */
    StandardsDataElemRuleRelDO getDpDataElemRuleRelById(Long id);

    /**
     * 获得全部数据元数据规则关联信息列表
     *
     * @return 数据元数据规则关联信息列表
     */
    List<StandardsDataElemRuleRelDO> getDpDataElemRuleRelList();

    /**
     * 获得全部数据元数据规则关联信息 Map
     *
     * @return 数据元数据规则关联信息 Map
     */
    Map<Long, StandardsDataElemRuleRelDO> getDpDataElemRuleRelMap();


    /**
     * 导入数据元数据规则关联信息数据
     *
     * @param importExcelList 数据元数据规则关联信息数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importDpDataElemRuleRel(List<StandardsDataElemRuleRelRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
