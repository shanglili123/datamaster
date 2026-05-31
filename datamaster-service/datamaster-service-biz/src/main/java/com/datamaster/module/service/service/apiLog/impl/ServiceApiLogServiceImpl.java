

package com.datamaster.module.service.service.apiLog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogPageReqVO;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogRespVO;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogSaveReqVO;
import com.datamaster.module.service.dal.dataobject.apiLog.ServiceApiLogDO;
import com.datamaster.module.service.dal.mapper.apiLog.ServiceApiLogMapper;
import com.datamaster.module.service.service.apiLog.IServiceApiLogService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * API服务调用日志Service业务层处理
 *
 * @author lhs
 * @date 2025-02-12
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ServiceApiLogServiceImpl  extends ServiceImpl<ServiceApiLogMapper,ServiceApiLogDO> implements IServiceApiLogService {
    @Resource
    private ServiceApiLogMapper ServiceApiLogMapper;

    @Override
    public PageResult<ServiceApiLogDO> getServiceApiLogPage(ServiceApiLogPageReqVO pageReqVO) {
        return ServiceApiLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createServiceApiLog(ServiceApiLogSaveReqVO createReqVO) {
        ServiceApiLogDO dictType = BeanUtils.toBean(createReqVO, ServiceApiLogDO.class);
        ServiceApiLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateServiceApiLog(ServiceApiLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新API服务调用日志
        ServiceApiLogDO updateObj = BeanUtils.toBean(updateReqVO, ServiceApiLogDO.class);
        return ServiceApiLogMapper.updateById(updateObj);
    }
    @Override
    public int removeServiceApiLog(Collection<Long> idList) {
        // 批量删除API服务调用日志
        return ServiceApiLogMapper.deleteBatchIds(idList);
    }

    @Override
    public ServiceApiLogDO getServiceApiLogById(Long id) {
        return ServiceApiLogMapper.selectServiceApiLogByID(id);
    }

    @Override
    public List<ServiceApiLogDO> getServiceApiLogList() {
        return ServiceApiLogMapper.selectList();
    }

    @Override
    public Map<Long, ServiceApiLogDO> getServiceApiLogMap() {
        List<ServiceApiLogDO> ServiceApiLogList = ServiceApiLogMapper.selectList();
        return ServiceApiLogList.stream()
                .collect(Collectors.toMap(
                        ServiceApiLogDO::getId,
                        ServiceApiLogDO -> ServiceApiLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入API服务调用日志数据
         *
         * @param importExcelList API服务调用日志数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importServiceApiLog(List<ServiceApiLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (ServiceApiLogRespVO respVO : importExcelList) {
                try {
                    ServiceApiLogDO ServiceApiLogDO = BeanUtils.toBean(respVO, ServiceApiLogDO.class);
                    Long ServiceApiLogId = respVO.getId();
                    if (isUpdateSupport) {
                        if (ServiceApiLogId != null) {
                            ServiceApiLogDO existingServiceApiLog = ServiceApiLogMapper.selectById(ServiceApiLogId);
                            if (existingServiceApiLog != null) {
                                ServiceApiLogMapper.updateById(ServiceApiLogDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + ServiceApiLogId + " 的API服务调用日志记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + ServiceApiLogId + " 的API服务调用日志记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<ServiceApiLogDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", ServiceApiLogId);
                        ServiceApiLogDO existingServiceApiLog = ServiceApiLogMapper.selectOne(queryWrapper);
                        if (existingServiceApiLog == null) {
                            ServiceApiLogMapper.insert(ServiceApiLogDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + ServiceApiLogId + " 的API服务调用日志记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + ServiceApiLogId + " 的API服务调用日志记录已存在。");
                        }
                    }
                } catch (Exception e) {
                    failureNum++;
                    String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                    failureMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }
            StringBuilder resultMsg = new StringBuilder();
            if (failureNum > 0) {
                resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
                resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
                throw new ServiceException(resultMsg.toString());
            } else {
                resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
            }
            return resultMsg.toString();
        }
}
