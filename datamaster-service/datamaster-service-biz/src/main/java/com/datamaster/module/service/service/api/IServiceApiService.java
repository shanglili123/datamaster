

package com.datamaster.module.service.service.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.api.vo.*;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.dataobject.api.SqlParseDto;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * API服务Service接口
 *
 * @author lhs
 * @date 2025-02-12
 */
public interface IServiceApiService extends IService<ServiceApiDO> {

    /**
     * 获得API服务分页列表
     *
     * @param pageReqVO 分页请求
     * @return API服务分页列表
     */
    PageResult<ServiceApiDO> getServiceApiPage(ServiceApiPageReqVO pageReqVO);

    /**
     * 创建API服务
     *
     * @param createReqVO API服务信息
     * @return API服务编号
     */
    Long createServiceApi(ServiceApiSaveReqVO createReqVO);

    /**
     * 更新API服务
     *
     * @param updateReqVO API服务信息
     */
    int updateServiceApi(ServiceApiSaveReqVO updateReqVO);

    /**
     * 删除API服务
     *
     * @param idList API服务编号
     */
    int removeServiceApi(Collection<Long> idList);

    /**
     * 获得API服务详情
     *
     * @param id API服务编号
     * @return API服务
     */
    ServiceApiDO getServiceApiById(Long id);

    /**
     * 获得全部API服务列表
     *
     * @return API服务列表
     */
    List<ServiceApiDO> getServiceApiList();

    /**
     * 获得全部API服务 Map
     *
     * @return API服务 Map
     */
    Map<Long, ServiceApiDO> getServiceApiMap();


    /**
     * 导入API服务数据
     *
     * @param importExcelList API服务数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importServiceApi(List<ServiceApiRespVO> importExcelList, boolean isUpdateSupport, String operName);


    SqlParseVo sqlParse(SqlParseDto sqlParseDto);


    Object serviceTesting(ServiceApiDO dataApi);


    AjaxResult saveDataApi(ServiceApiDO dataApi);


    AjaxResult updateDataApi(ServiceApiDO dataApi);


    void releaseDataApi(String id,Long updateId, String updateBy);

    void cancelDataApi(String id,Long updateId, String updateBy);

    ServiceApiDO repeatFlag(JSONObject jsonObject);

    void queryServiceForwarding(HttpServletResponse response, ServiceApiReqVO ServiceApiReqVO);
}
