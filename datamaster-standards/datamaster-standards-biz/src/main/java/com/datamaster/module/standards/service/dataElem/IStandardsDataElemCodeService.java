package com.datamaster.module.standards.service.dataElem;


import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodePageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodeRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodeSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemCodeDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据元代码Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IStandardsDataElemCodeService extends IService<StandardsDataElemCodeDO> {

    /**
     * 获得数据元代码分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据元代码分页列表
     */
    PageResult<StandardsDataElemCodeDO> getDpDataElemCodePage(StandardsDataElemCodePageReqVO pageReqVO);

    /**
     * 创建数据元代码
     *
     * @param createReqVO 数据元代码信息
     * @return 数据元代码编号
     */
    Long createDpDataElemCode(StandardsDataElemCodeSaveReqVO createReqVO);

    /**
     * 更新数据元代码
     *
     * @param updateReqVO 数据元代码信息
     */
    int updateDpDataElemCode(StandardsDataElemCodeSaveReqVO updateReqVO);

    /**
     * 删除数据元代码
     *
     * @param idList 数据元代码编号
     */
    int removeDpDataElemCode(Collection<Long> idList);

    /**
     * 获得数据元代码详情
     *
     * @param id 数据元代码编号
     * @return 数据元代码
     */
    StandardsDataElemCodeDO getDpDataElemCodeById(Long id);

    /**
     * 获得全部数据元代码列表
     *
     * @return 数据元代码列表
     */
    List<StandardsDataElemCodeDO> getDpDataElemCodeList();

    /**
     * 获得全部数据元代码 Map
     *
     * @return 数据元代码 Map
     */
    Map<Long, StandardsDataElemCodeDO> getDpDataElemCodeMap();


    /**
     * 导入数据元代码数据
     *
     * @param importExcelList 数据元代码数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importDpDataElemCode(List<StandardsDataElemCodeRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 校验源代码值
     *
     * @param dataElemId
     * @param codeValue
     * @param id
     * @return
     */
    Integer validateCodeValue(String dataElemId, String codeValue, String id);
}
