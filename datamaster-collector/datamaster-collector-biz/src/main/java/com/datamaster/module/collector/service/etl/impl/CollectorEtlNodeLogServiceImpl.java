

package com.datamaster.module.collector.service.etl.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeLogDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlNodeLogMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeLogService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据集成节点-日志Service业务层处理
 *
 * @author qdata
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlNodeLogServiceImpl extends ServiceImpl<CollectorEtlNodeLogMapper, CollectorEtlNodeLogDO> implements ICollectorEtlNodeLogService {
    @Resource
    private CollectorEtlNodeLogMapper CollectorEtlNodeLogMapper;

    @Override
    public PageResult<CollectorEtlNodeLogDO> getCollectorEtlNodeLogPage(CollectorEtlNodeLogPageReqVO pageReqVO) {
        return CollectorEtlNodeLogMapper.selectPage(pageReqVO);
    }

    @Override
    public CollectorEtlNodeLogDO getCollectorEtlNodeLogRespVOByReqVO(CollectorEtlNodeLogPageReqVO reqVO) {

        MPJLambdaWrapper<CollectorEtlNodeLogDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlNodeLogDO.class)
                .eq(StringUtils.isNotEmpty(reqVO.getCode()), CollectorEtlNodeLogDO::getCode, reqVO.getCode())
                .eq(reqVO.getVersion() != null, CollectorEtlNodeLogDO::getVersion, reqVO.getVersion())
                .eq(reqVO.getDsId() != null, CollectorEtlNodeLogDO::getDsId, reqVO.getDsId());
        CollectorEtlNodeLogDO CollectorEtlNodeLogDO = CollectorEtlNodeLogMapper.selectOne(wrapper);
        return CollectorEtlNodeLogDO;
    }

    @Override
    public Long createCollectorEtlNodeLog(CollectorEtlNodeLogSaveReqVO createReqVO) {
        CollectorEtlNodeLogDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlNodeLogDO.class);
        CollectorEtlNodeLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public CollectorEtlNodeLogDO createCollectorEtlNodeLogNew(CollectorEtlNodeLogSaveReqVO CollectorEtlNodeLogSaveReqVO) {

        CollectorEtlNodeLogDO dictType = BeanUtils.toBean(CollectorEtlNodeLogSaveReqVO, CollectorEtlNodeLogDO.class);
        CollectorEtlNodeLogMapper.insert(dictType);
        return dictType;
    }

    @Override
    public List<CollectorEtlNodeLogDO> createCollectorEtlNodeLogBatch(List<CollectorEtlNodeLogSaveReqVO> CollectorEtlNodeLogSaveReqVOS) {
        List<CollectorEtlNodeLogDO> CollectorEtlNodeDOList = new ArrayList<>();
        for (CollectorEtlNodeLogSaveReqVO CollectorEtlNodeSaveReqVO : CollectorEtlNodeLogSaveReqVOS) {
            CollectorEtlNodeLogDO dictType = BeanUtils.toBean(CollectorEtlNodeSaveReqVO, CollectorEtlNodeLogDO.class);
            dictType.setId(null);
            CollectorEtlNodeLogMapper.insert(dictType);
            CollectorEtlNodeDOList.add(dictType);
        }
        return CollectorEtlNodeDOList;
    }

    @Override
    public int updateCollectorEtlNodeLog(CollectorEtlNodeLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成节点-日志
        CollectorEtlNodeLogDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlNodeLogDO.class);
        return CollectorEtlNodeLogMapper.updateById(updateObj);
    }

    @Override
    public int removeCollectorEtlNodeLog(Collection<Long> idList) {
        // 批量删除数据集成节点-日志
        return CollectorEtlNodeLogMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlNodeLogDO getCollectorEtlNodeLogById(Long id) {
        return CollectorEtlNodeLogMapper.selectById(id);
    }

    @Override
    public List<CollectorEtlNodeLogDO> getCollectorEtlNodeLogList() {
        return CollectorEtlNodeLogMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlNodeLogDO> getCollectorEtlNodeLogMap() {
        List<CollectorEtlNodeLogDO> CollectorEtlNodeLogList = CollectorEtlNodeLogMapper.selectList();
        return CollectorEtlNodeLogList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlNodeLogDO::getId,
                        CollectorEtlNodeLogDO -> CollectorEtlNodeLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据集成节点-日志数据
     *
     * @param importExcelList 数据集成节点-日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCollectorEtlNodeLog(List<CollectorEtlNodeLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CollectorEtlNodeLogRespVO respVO : importExcelList) {
            try {
                CollectorEtlNodeLogDO CollectorEtlNodeLogDO = BeanUtils.toBean(respVO, CollectorEtlNodeLogDO.class);
                Long CollectorEtlNodeLogId = respVO.getId();
                if (isUpdateSupport) {
                    if (CollectorEtlNodeLogId != null) {
                        CollectorEtlNodeLogDO existingCollectorEtlNodeLog = CollectorEtlNodeLogMapper.selectById(CollectorEtlNodeLogId);
                        if (existingCollectorEtlNodeLog != null) {
                            CollectorEtlNodeLogMapper.updateById(CollectorEtlNodeLogDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CollectorEtlNodeLogId + " 的数据集成节点-日志记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CollectorEtlNodeLogId + " 的数据集成节点-日志记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CollectorEtlNodeLogDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CollectorEtlNodeLogId);
                    CollectorEtlNodeLogDO existingCollectorEtlNodeLog = CollectorEtlNodeLogMapper.selectOne(queryWrapper);
                    if (existingCollectorEtlNodeLog == null) {
                        CollectorEtlNodeLogMapper.insert(CollectorEtlNodeLogDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CollectorEtlNodeLogId + " 的数据集成节点-日志记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CollectorEtlNodeLogId + " 的数据集成节点-日志记录已存在。");
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

    @Override
    public CollectorEtlNodeLogDO getByNodeCodeAndVersion(String nodeCode, Integer version) {
        CollectorEtlNodeLogDO collectorEtlNodeLogDO = baseMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlNodeLogDO.class)
                .eq(CollectorEtlNodeLogDO::getCode, nodeCode)
                .eq(CollectorEtlNodeLogDO::getVersion, version));
        return collectorEtlNodeLogDO;
    }

    @Override
    public Integer getMaxVersionByNodeCode(String nodeCode) {
        return baseMapper.getMaxVersionByNodeCode(nodeCode);
    }

    @Override
    public List<CollectorEtlNodeLogDO> listByTaskCode(String taskCode, Integer version) {
        MPJLambdaWrapper<CollectorEtlNodeLogDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlNodeLogDO.class)
                .innerJoin("COL_ETL_TASK_NODE_REL_LOG t2 ON ((t.CODE = t2.PRE_NODE_CODE AND t.VERSION = t2.PRE_NODE_VERSION) OR (t.CODE = t2.POST_NODE_CODE AND t.VERSION = t2.POST_NODE_VERSION)) AND t2.DEL_FLAG = '0' AND t2.TASK_CODE =" + taskCode + " AND  t2.TASK_VERSION  =" + version)
                .distinct();
        List<CollectorEtlNodeLogDO> CollectorEtlTaskNodeRelDOS = CollectorEtlNodeLogMapper.selectList(wrapper);
        return CollectorEtlTaskNodeRelDOS;
    }
}
