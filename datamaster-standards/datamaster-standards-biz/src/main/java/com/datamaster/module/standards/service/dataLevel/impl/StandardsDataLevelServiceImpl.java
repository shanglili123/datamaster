

package com.datamaster.module.standards.service.dataLevel.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelRespVO;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataLevel.StandardsDataLevelDO;
import com.datamaster.module.standards.dal.mapper.dataLevel.StandardsDataLevelMapper;
import com.datamaster.module.standards.service.dataLevel.IStandardsDataLevelService;
/**
 * 数据分级Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDataLevelServiceImpl  extends ServiceImpl<StandardsDataLevelMapper,StandardsDataLevelDO> implements IStandardsDataLevelService {
    @Resource
    private StandardsDataLevelMapper StandardsDataLevelMapper;

    @Override
    public PageResult<StandardsDataLevelDO> getDgDataLevelPage(StandardsDataLevelPageReqVO pageReqVO) {
        return StandardsDataLevelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDgDataLevel(StandardsDataLevelSaveReqVO createReqVO) {
        StandardsDataLevelDO dictType = BeanUtils.toBean(createReqVO, StandardsDataLevelDO.class);
        // 敏感等级不能重复
        if (StandardsDataLevelMapper.selectCount(new LambdaQueryWrapper<StandardsDataLevelDO>()
                .eq(StandardsDataLevelDO::getSensitiveLevel, dictType.getSensitiveLevel())) > 0) {
            throw new IllegalArgumentException("敏感等级不能重复");
        }
        StandardsDataLevelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDgDataLevel(StandardsDataLevelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据分级
        StandardsDataLevelDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDataLevelDO.class);
        return StandardsDataLevelMapper.updateById(updateObj);
    }
    @Override
    public int removeDgDataLevel(Collection<Long> idList) {
        // 批量删除数据分级
        return StandardsDataLevelMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDataLevelDO getDgDataLevelById(Long id) {
        return StandardsDataLevelMapper.selectById(id);
    }

    @Override
    public List<StandardsDataLevelDO> getDgDataLevelList() {
        return StandardsDataLevelMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDataLevelDO> getDgDataLevelMap() {
        List<StandardsDataLevelDO> StandardsDataLevelList = StandardsDataLevelMapper.selectList();
        return StandardsDataLevelList.stream()
                .collect(Collectors.toMap(
                        StandardsDataLevelDO::getId,
                        StandardsDataLevelDO -> StandardsDataLevelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据分级数据
         *
         * @param importExcelList 数据分级数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDgDataLevel(List<StandardsDataLevelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsDataLevelRespVO respVO : importExcelList) {
                try {
                    StandardsDataLevelDO StandardsDataLevelDO = BeanUtils.toBean(respVO, StandardsDataLevelDO.class);
                    Long StandardsDataLevelId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsDataLevelId != null) {
                            StandardsDataLevelDO existingDgDataLevel = StandardsDataLevelMapper.selectById(StandardsDataLevelId);
                            if (existingDgDataLevel != null) {
                                StandardsDataLevelMapper.updateById(StandardsDataLevelDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsDataLevelId + " 的数据分级记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsDataLevelId + " 的数据分级记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsDataLevelDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsDataLevelId);
                        StandardsDataLevelDO existingDgDataLevel = StandardsDataLevelMapper.selectOne(queryWrapper);
                        if (existingDgDataLevel == null) {
                            StandardsDataLevelMapper.insert(StandardsDataLevelDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsDataLevelId + " 的数据分级记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsDataLevelId + " 的数据分级记录已存在。");
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
    public List<StandardsDataLevelDO> getDgDataLevelListAll(StandardsDataLevelPageReqVO StandardsDataLevel) {
        return StandardsDataLevelMapper.selectList();
    }
}
