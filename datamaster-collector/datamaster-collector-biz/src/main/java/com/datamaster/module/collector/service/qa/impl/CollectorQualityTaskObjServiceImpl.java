

package com.datamaster.module.collector.service.qa.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjPageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskObjSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskObjDO;
import com.datamaster.module.collector.dal.mapper.qa.CollectorQualityTaskObjMapper;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskObjService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 数据质量任务-稽查对象Service业务层处理
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorQualityTaskObjServiceImpl  extends ServiceImpl<CollectorQualityTaskObjMapper,CollectorQualityTaskObjDO> implements ICollectorQualityTaskObjService {
    @Resource
    private CollectorQualityTaskObjMapper CollectorQualityTaskObjMapper;

    @Override
    public PageResult<CollectorQualityTaskObjDO> getCollectorQualityTaskObjPage(CollectorQualityTaskObjPageReqVO pageReqVO) {
        return CollectorQualityTaskObjMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCollectorQualityTaskObj(CollectorQualityTaskObjSaveReqVO createReqVO) {
        CollectorQualityTaskObjDO dictType = BeanUtils.toBean(createReqVO, CollectorQualityTaskObjDO.class);
        CollectorQualityTaskObjMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorQualityTaskObj(CollectorQualityTaskObjSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据质量任务-稽查对象
        CollectorQualityTaskObjDO updateObj = BeanUtils.toBean(updateReqVO, CollectorQualityTaskObjDO.class);
        return CollectorQualityTaskObjMapper.updateById(updateObj);
    }
    @Override
    public int removeCollectorQualityTaskObj(Collection<Long> idList) {
        // 批量删除数据质量任务-稽查对象
        return CollectorQualityTaskObjMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorQualityTaskObjDO getCollectorQualityTaskObjById(Long id) {
        return CollectorQualityTaskObjMapper.selectById(id);
    }

    @Override
    public List<CollectorQualityTaskObjDO> getCollectorQualityTaskObjList() {
        return CollectorQualityTaskObjMapper.selectList();
    }

    @Override
    public List<CollectorQualityTaskObjDO> getCollectorQualityTaskObjList(CollectorQualityTaskObjPageReqVO pageReqVO) {
        LambdaQueryWrapperX<CollectorQualityTaskObjDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eq(pageReqVO.getTaskId() != null,CollectorQualityTaskObjDO::getTaskId, pageReqVO.getTaskId());
        List<CollectorQualityTaskObjDO> CollectorQualityTaskObjDOS = CollectorQualityTaskObjMapper.selectList(queryWrapperX);
        return CollectorQualityTaskObjDOS;
    }


    @Override
    public Map<Long, CollectorQualityTaskObjDO> getCollectorQualityTaskObjMap() {
        List<CollectorQualityTaskObjDO> CollectorQualityTaskObjList = CollectorQualityTaskObjMapper.selectList();
        return CollectorQualityTaskObjList.stream()
                .collect(Collectors.toMap(
                        CollectorQualityTaskObjDO::getId,
                        CollectorQualityTaskObjDO -> CollectorQualityTaskObjDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据质量任务-稽查对象数据
     *
     * @param importExcelList 数据质量任务-稽查对象数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importCollectorQualityTaskObj(List<CollectorQualityTaskObjRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CollectorQualityTaskObjRespVO respVO : importExcelList) {
            try {
                CollectorQualityTaskObjDO CollectorQualityTaskObjDO = BeanUtils.toBean(respVO, CollectorQualityTaskObjDO.class);
                Long CollectorQualityTaskObjId = respVO.getId();
                if (isUpdateSupport) {
                    if (CollectorQualityTaskObjId != null) {
                        CollectorQualityTaskObjDO existingCollectorQualityTaskObj = CollectorQualityTaskObjMapper.selectById(CollectorQualityTaskObjId);
                        if (existingCollectorQualityTaskObj != null) {
                            CollectorQualityTaskObjMapper.updateById(CollectorQualityTaskObjDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CollectorQualityTaskObjId + " 的数据质量任务-稽查对象记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CollectorQualityTaskObjId + " 的数据质量任务-稽查对象记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CollectorQualityTaskObjDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CollectorQualityTaskObjId);
                    CollectorQualityTaskObjDO existingCollectorQualityTaskObj = CollectorQualityTaskObjMapper.selectOne(queryWrapper);
                    if (existingCollectorQualityTaskObj == null) {
                        CollectorQualityTaskObjMapper.insert(CollectorQualityTaskObjDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CollectorQualityTaskObjId + " 的数据质量任务-稽查对象记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CollectorQualityTaskObjId + " 的数据质量任务-稽查对象记录已存在。");
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
