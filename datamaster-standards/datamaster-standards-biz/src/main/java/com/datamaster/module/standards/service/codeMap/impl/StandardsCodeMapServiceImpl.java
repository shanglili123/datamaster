package com.datamaster.module.standards.service.codeMap.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapPageReqVO;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapRespVO;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.codeMap.StandardsCodeMapDO;
import com.datamaster.module.standards.dal.mapper.codeMap.StandardsCodeMapMapper;
import com.datamaster.module.standards.service.codeMap.IStandardsCodeMapService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 数据元代码映射Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsCodeMapServiceImpl  extends ServiceImpl<StandardsCodeMapMapper,StandardsCodeMapDO> implements IStandardsCodeMapService {
    @Resource
    private StandardsCodeMapMapper StandardsCodeMapMapper;

    @Override
    public PageResult<StandardsCodeMapDO> getDpCodeMapPage(StandardsCodeMapPageReqVO pageReqVO) {
        return StandardsCodeMapMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDpCodeMap(StandardsCodeMapSaveReqVO createReqVO) {
        StandardsCodeMapDO dictType = BeanUtils.toBean(createReqVO, StandardsCodeMapDO.class);
        StandardsCodeMapMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDpCodeMap(StandardsCodeMapSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据元代码映射
        StandardsCodeMapDO updateObj = BeanUtils.toBean(updateReqVO, StandardsCodeMapDO.class);
        return StandardsCodeMapMapper.updateById(updateObj);
    }
    @Override
    public int removeDpCodeMap(Collection<Long> idList) {
        // 批量删除数据元代码映射
        return StandardsCodeMapMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsCodeMapDO getDpCodeMapById(Long id) {
        return StandardsCodeMapMapper.selectById(id);
    }

    @Override
    public List<StandardsCodeMapDO> getDpCodeMapList() {
        return StandardsCodeMapMapper.selectList();
    }

    @Override
    public Map<Long, StandardsCodeMapDO> getDpCodeMapMap() {
        List<StandardsCodeMapDO> StandardsCodeMapList = StandardsCodeMapMapper.selectList();
        return StandardsCodeMapList.stream()
                .collect(Collectors.toMap(
                        StandardsCodeMapDO::getId,
                        StandardsCodeMapDO -> StandardsCodeMapDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据元代码映射数据
         *
         * @param importExcelList 数据元代码映射数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDpCodeMap(List<StandardsCodeMapRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsCodeMapRespVO respVO : importExcelList) {
                try {
                    StandardsCodeMapDO StandardsCodeMapDO = BeanUtils.toBean(respVO, StandardsCodeMapDO.class);
                    Long StandardsCodeMapId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsCodeMapId != null) {
                            StandardsCodeMapDO existingDpCodeMap = StandardsCodeMapMapper.selectById(StandardsCodeMapId);
                            if (existingDpCodeMap != null) {
                                StandardsCodeMapMapper.updateById(StandardsCodeMapDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsCodeMapId + " 的数据元代码映射记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsCodeMapId + " 的数据元代码映射记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsCodeMapDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsCodeMapId);
                        StandardsCodeMapDO existingDpCodeMap = StandardsCodeMapMapper.selectOne(queryWrapper);
                        if (existingDpCodeMap == null) {
                            StandardsCodeMapMapper.insert(StandardsCodeMapDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsCodeMapId + " 的数据元代码映射记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsCodeMapId + " 的数据元代码映射记录已存在。");
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
