package com.datamaster.module.standards.service.standard;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemCatPageReqVO;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemCatSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.standard.StandardsDataElemCatDO;

import java.util.Collection;
import java.util.List;

/**
 * 数据元类目管理Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface IStandardsDataElemCatService extends IService<StandardsDataElemCatDO> {

    /**
     * 创建数据元类目管理
     *
     * @param createReqVO 数据元类目管理信息
     * @return 数据元类目管理编号
     */
    Long createDgDataElemCat(StandardsDataElemCatSaveReqVO createReqVO);

    /**
     * 更新数据元类目管理
     *
     * @param updateReqVO 数据元类目管理信息
     */
    int updateDgDataElemCat(StandardsDataElemCatSaveReqVO updateReqVO);

    /**
     * 删除数据元类目管理
     *
     * @param idList 数据元类目管理编号
     */
    int removeDgDataElemCat(Collection<Long> idList);

    /**
     * 更改指定pid下的所有code
     *
     * @param pid
     */
    void changeCodeByPid(Long pid, String parentCode);

    /**
     * 获得数据元类目管理详情
     *
     * @param id 数据元类目管理编号
     * @return 数据元类目管理
     */
    StandardsDataElemCatDO getDgDataElemCatById(Long id);

    /**
     * 获得全部数据元类目管理列表
     *
     * @return 数据元类目管理列表
     */
    List<StandardsDataElemCatDO> getDgDataElemCatList(StandardsDataElemCatPageReqVO reqVO);

    /**
     * 生成code
     *
     * @param parentId
     * @param parentCode
     * @return
     */
    String createCode(Long parentId, String parentCode);

    /**
     * 批量删除检查,查询可删除数和不可删除数
     *
     * @param ids
     * @return
     */
    BatchDeleteCheck<Long> batchDeleteCheck(List<Long> ids);
}
