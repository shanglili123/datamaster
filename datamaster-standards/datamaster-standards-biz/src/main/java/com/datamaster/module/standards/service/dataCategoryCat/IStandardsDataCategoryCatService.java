

package com.datamaster.module.standards.service.dataCategoryCat;

import java.util.List;
import java.util.Map;
import java.util.Collection;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatRespVO;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatSaveReqVO;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatPageReqVO;
import com.datamaster.module.standards.dal.dataobject.dataCategoryCat.StandardsDataCategoryCatDO;

/**
 * 数据分类-类目Service接口
 *
 * @author FXB
 * @date 2026-04-07
 */
public interface IStandardsDataCategoryCatService extends IService<StandardsDataCategoryCatDO> {

    /**
     * 获得数据分类-类目分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据分类-类目分页列表
     */
    PageResult<StandardsDataCategoryCatDO> getDgDataCategoryCatPage(StandardsDataCategoryCatPageReqVO pageReqVO);

    /**
     * 创建数据分类-类目
     *
     * @param createReqVO 数据分类-类目信息
     * @return 数据分类-类目编号
     */
    Long createDgDataCategoryCat(StandardsDataCategoryCatSaveReqVO createReqVO);

    /**
     * 更新数据分类-类目
     *
     * @param updateReqVO 数据分类-类目信息
     */
    int updateDgDataCategoryCat(StandardsDataCategoryCatSaveReqVO updateReqVO);

    /**
     * 删除数据分类-类目
     *
     * @param idList 数据分类-类目编号
     */
    int removeDgDataCategoryCat(Collection<Long> idList);

    /**
     * 获得数据分类-类目详情
     *
     * @param id 数据分类-类目编号
     * @return 数据分类-类目
     */
    StandardsDataCategoryCatDO getDgDataCategoryCatById(Long id);

    /**
     * 获得全部数据分类-类目列表
     *
     * @return 数据分类-类目列表
     */
    List<StandardsDataCategoryCatDO> getDgDataCategoryCatList();
    /**
     * 获得全部数据分类-类目 Map
     *
     * @return 数据分类-类目 Map
     */
    Map<Long, StandardsDataCategoryCatDO> getDgDataCategoryCatMap();


    /**
     * 导入数据分类-类目数据
     *
     * @param importExcelList 数据分类-类目数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importDgDataCategoryCat(List<StandardsDataCategoryCatRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 生成code
     *
     * @param parentId
     * @param parentCode
     * @return
     */
    String createCode(Long parentId, String parentCode);

    /**
     * 更改指定pid下的所有code
     *
     * @param pid
     */
    void changeCodeByPid(Long pid, String parentCode);

}
