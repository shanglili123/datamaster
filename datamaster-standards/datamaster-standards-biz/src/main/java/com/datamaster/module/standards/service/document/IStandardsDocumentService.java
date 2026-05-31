package com.datamaster.module.standards.service.document;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.standards.controller.admin.document.vo.*;
import com.datamaster.module.standards.dal.dataobject.document.StandardsDocumentDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * 标准信息登记Service接口
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
public interface IStandardsDocumentService extends IService<StandardsDocumentDO> {

    /**
     * 获得标准信息登记分页列表
     *
     * @param pageReqVO 分页请求
     * @return 标准信息登记分页列表
     */
    PageResult<StandardsDocumentDO> getDpDocumentPage(StandardsDocumentPageReqVO pageReqVO);

    /**
     * 获得全部标准信息登记列表
     *
     * @return 标准信息登记列表
     */
    List<StandardsDocumentDO> getDpDocumentList(StandardsDocumentPageReqVO pageReqVO);

    /**
     * 创建标准信息登记
     *
     * @param createReqVO 标准信息登记信息
     * @return 标准信息登记编号
     */
    Long createDpDocument(StandardsDocumentSaveReqVO createReqVO);

    /**
     * 更新标准信息登记
     *
     * @param updateReqVO 标准信息登记信息
     */
    int updateDpDocument(StandardsDocumentSaveReqVO updateReqVO);

    /**
     * 删除标准信息登记
     *
     * @param idList 标准信息登记编号
     */
    int removeDpDocument(Collection<Long> idList);

    /**
     * 获得标准信息登记详情
     *
     * @param id 标准信息登记编号
     * @return 标准信息登记
     */
    StandardsDocumentDO getDpDocumentById(Long id);

    /**
     * 获得全部标准信息登记列表
     *
     * @return 标准信息登记列表
     */
    List<StandardsDocumentDO> getDpDocumentList();

    /**
     * 获得全部标准信息登记 Map
     *
     * @return 标准信息登记 Map
     */
    Map<Long, StandardsDocumentDO> getDpDocumentMap();


    /**
     * 导入标准信息登记数据
     *
     * @param importExcelList 标准信息登记数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importDpDocument(List<StandardsDocumentRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 标准检索分页列表
     *
     * @param StandardsDocument
     * @return
     */
    PageResult<StandardsDocumentSearchRespVO> getDpDocumentSearchPage(StandardsDocumentSearchReqVO StandardsDocument);
}
