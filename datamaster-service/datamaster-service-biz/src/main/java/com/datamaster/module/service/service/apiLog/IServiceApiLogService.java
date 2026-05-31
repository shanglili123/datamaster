

package com.datamaster.module.service.service.apiLog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogPageReqVO;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogRespVO;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogSaveReqVO;
import com.datamaster.module.service.dal.dataobject.apiLog.ServiceApiLogDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * API服务调用日志Service接口
 *
 * @author lhs
 * @date 2025-02-12
 */
public interface IServiceApiLogService extends IService<ServiceApiLogDO> {

    /**
     * 获得API服务调用日志分页列表
     *
     * @param pageReqVO 分页请求
     * @return API服务调用日志分页列表
     */
    PageResult<ServiceApiLogDO> getServiceApiLogPage(ServiceApiLogPageReqVO pageReqVO);

    /**
     * 创建API服务调用日志
     *
     * @param createReqVO API服务调用日志信息
     * @return API服务调用日志编号
     */
    Long createServiceApiLog(ServiceApiLogSaveReqVO createReqVO);

    /**
     * 更新API服务调用日志
     *
     * @param updateReqVO API服务调用日志信息
     */
    int updateServiceApiLog(ServiceApiLogSaveReqVO updateReqVO);

    /**
     * 删除API服务调用日志
     *
     * @param idList API服务调用日志编号
     */
    int removeServiceApiLog(Collection<Long> idList);

    /**
     * 获得API服务调用日志详情
     *
     * @param id API服务调用日志编号
     * @return API服务调用日志
     */
    ServiceApiLogDO getServiceApiLogById(Long id);

    /**
     * 获得全部API服务调用日志列表
     *
     * @return API服务调用日志列表
     */
    List<ServiceApiLogDO> getServiceApiLogList();

    /**
     * 获得全部API服务调用日志 Map
     *
     * @return API服务调用日志 Map
     */
    Map<Long, ServiceApiLogDO> getServiceApiLogMap();


    /**
     * 导入API服务调用日志数据
     *
     * @param importExcelList API服务调用日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importServiceApiLog(List<ServiceApiLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
