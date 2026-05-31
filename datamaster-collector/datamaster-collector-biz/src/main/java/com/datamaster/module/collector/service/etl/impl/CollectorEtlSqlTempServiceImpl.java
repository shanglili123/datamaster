

package com.datamaster.module.collector.service.etl.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSqlTempSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSqlTempDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlSqlTempMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlSqlTempService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据集成SQL模版Service业务层处理
 *
 * @author FXB
 * @date 2025-06-25
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlSqlTempServiceImpl extends ServiceImpl<CollectorEtlSqlTempMapper,CollectorEtlSqlTempDO> implements ICollectorEtlSqlTempService {
    @Resource
    private CollectorEtlSqlTempMapper CollectorEtlSqlTempMapper;

    @Override
    public PageResult<CollectorEtlSqlTempDO> getCollectorEtlSqlTempPage(CollectorEtlSqlTempPageReqVO pageReqVO) {
        return CollectorEtlSqlTempMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCollectorEtlSqlTemp(CollectorEtlSqlTempSaveReqVO createReqVO) {
        CollectorEtlSqlTempDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlSqlTempDO.class);
        CollectorEtlSqlTempMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorEtlSqlTemp(CollectorEtlSqlTempSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成SQL模版
        CollectorEtlSqlTempDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlSqlTempDO.class);
        return CollectorEtlSqlTempMapper.updateById(updateObj);
    }
    @Override
    public int removeCollectorEtlSqlTemp(Collection<Long> idList) {
        // 批量删除数据集成SQL模版
        return CollectorEtlSqlTempMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlSqlTempDO getCollectorEtlSqlTempById(Long id) {
        return CollectorEtlSqlTempMapper.selectById(id);
    }

    @Override
    public List<CollectorEtlSqlTempDO> getCollectorEtlSqlTempList() {
        return CollectorEtlSqlTempMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlSqlTempDO> getCollectorEtlSqlTempMap() {
        List<CollectorEtlSqlTempDO> CollectorEtlSqlTempList = CollectorEtlSqlTempMapper.selectList();
        return CollectorEtlSqlTempList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlSqlTempDO::getId,
                        CollectorEtlSqlTempDO -> CollectorEtlSqlTempDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据集成SQL模版数据
         *
         * @param importExcelList 数据集成SQL模版数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importCollectorEtlSqlTemp(List<CollectorEtlSqlTempRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (CollectorEtlSqlTempRespVO respVO : importExcelList) {
                try {
                    CollectorEtlSqlTempDO CollectorEtlSqlTempDO = BeanUtils.toBean(respVO, CollectorEtlSqlTempDO.class);
                    Long CollectorEtlSqlTempId = respVO.getId();
                    if (isUpdateSupport) {
                        if (CollectorEtlSqlTempId != null) {
                            CollectorEtlSqlTempDO existingCollectorEtlSqlTemp = CollectorEtlSqlTempMapper.selectById(CollectorEtlSqlTempId);
                            if (existingCollectorEtlSqlTemp != null) {
                                CollectorEtlSqlTempMapper.updateById(CollectorEtlSqlTempDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + CollectorEtlSqlTempId + " 的数据集成SQL模版记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + CollectorEtlSqlTempId + " 的数据集成SQL模版记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<CollectorEtlSqlTempDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", CollectorEtlSqlTempId);
                        CollectorEtlSqlTempDO existingCollectorEtlSqlTemp = CollectorEtlSqlTempMapper.selectOne(queryWrapper);
                        if (existingCollectorEtlSqlTemp == null) {
                            CollectorEtlSqlTempMapper.insert(CollectorEtlSqlTempDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + CollectorEtlSqlTempId + " 的数据集成SQL模版记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + CollectorEtlSqlTempId + " 的数据集成SQL模版记录已存在。");
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
