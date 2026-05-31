package com.datamaster.module.standards.service.dataElem;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据元Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IStandardsDataElemService extends IService<StandardsDataElemDO> {

    /**
     * 获得数据元分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据元分页列表
     */
    PageResult<StandardsDataElemDO> getDpDataElemPage(StandardsDataElemPageReqVO pageReqVO);

    List<StandardsDataElemDO> getDpDataElemList(StandardsDataElemPageReqVO pageReqVO);

    /**
     * 创建数据元
     *
     * @param createReqVO 数据元信息
     * @return 数据元编号
     */
    Long createDpDataElem(StandardsDataElemSaveReqVO createReqVO);

    /**
     * 更新数据元
     *
     * @param updateReqVO 数据元信息
     */
    int updateDpDataElem(StandardsDataElemSaveReqVO updateReqVO);

    /**
     * 删除数据元
     *
     * @param idList 数据元编号
     */
    int removeDpDataElem(List<Long> idList);

    /**
     * 获得数据元详情
     *
     * @param id 数据元编号
     * @return 数据元
     */
    StandardsDataElemDO getDpDataElemById(Long id);

    /**
     * 获得全部数据元列表
     *
     * @return 数据元列表
     */
    List<StandardsDataElemDO> getDpDataElemList();

    /**
     * 获得全部数据元 Map
     *
     * @return 数据元 Map
     */
    Map<Long, StandardsDataElemDO> getDpDataElemMap();


    /**
     * 导入数据元数据
     *
     * @param importExcelList 数据元数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importDpDataElem(List<StandardsDataElemRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 更改数据元状态
     *
     * @param id
     * @param status
     * @return
     */
    Boolean updateStatus(Long id, Long status);
}
