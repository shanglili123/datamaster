package com.datamaster.module.standards.service.standard;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemPageReqVO;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.standard.StandardsDataMetaDO;

import java.util.List;

/**
 * 数据元Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
public interface IStandardsDataMetaService extends IService<StandardsDataMetaDO> {

    /**
     * 获得数据元分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据元分页列表
     */
    PageResult<StandardsDataMetaDO> getDgDataElemPage(StandardsDataElemPageReqVO pageReqVO);

    List<StandardsDataMetaDO> getDgDataElemList(StandardsDataElemPageReqVO pageReqVO);

    /**
     * 创建数据元
     *
     * @param createReqVO 数据元信息
     * @return 数据元编号
     */
    Long createDgDataElem(StandardsDataElemSaveReqVO createReqVO);

    /**
     * 更新数据元
     *
     * @param updateReqVO 数据元信息
     */
    int updateDgDataElem(StandardsDataElemSaveReqVO updateReqVO);

    /**
     * 删除数据元
     *
     * @param idList 数据元编号
     */
    int removeDgDataElem(List<Long> idList);

    /**
     * 获得数据元详情
     *
     * @param id 数据元编号
     * @return 数据元
     */
    StandardsDataMetaDO getDgDataElemById(Long id);

    /**
     * 更改数据元状态
     *
     * @param id
     * @param status
     * @return
     */
    Boolean updateStatus(Long id, Long status);

}
