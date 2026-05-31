

package com.datamaster.module.standards.service.desensitizeList.impl;

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
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnPageReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.desensitizeList.StandardsDesensitizeAssetcolumnDO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeUserRelDO;
import com.datamaster.module.standards.dal.mapper.desensitizeList.StandardsDesensitizeAssetcolumnMapper;
import com.datamaster.module.standards.service.desensitizeList.IStandardsDesensitizeAssetcolumnService;
/**
 * 脱敏清单关联关系Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDesensitizeAssetcolumnServiceImpl  extends ServiceImpl<StandardsDesensitizeAssetcolumnMapper,StandardsDesensitizeAssetcolumnDO> implements IStandardsDesensitizeAssetcolumnService {
    @Resource
    private StandardsDesensitizeAssetcolumnMapper StandardsDesensitizeAssetcolumnMapper;

    @Override
    public PageResult<StandardsDesensitizeAssetcolumnDO> getDgDesensitizeAssetcolumnPage(StandardsDesensitizeAssetcolumnPageReqVO pageReqVO) {
        return StandardsDesensitizeAssetcolumnMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDgDesensitizeAssetcolumn(StandardsDesensitizeAssetcolumnSaveReqVO createReqVO) {
        StandardsDesensitizeAssetcolumnDO dictType = BeanUtils.toBean(createReqVO, StandardsDesensitizeAssetcolumnDO.class);
        StandardsDesensitizeAssetcolumnMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDgDesensitizeAssetcolumn(StandardsDesensitizeAssetcolumnSaveReqVO updateReqVO) {
        // 相关校验

        // 更新脱敏清单关联关系
        StandardsDesensitizeAssetcolumnDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDesensitizeAssetcolumnDO.class);
        return StandardsDesensitizeAssetcolumnMapper.updateById(updateObj);
    }
    @Override
    public int removeDgDesensitizeAssetcolumn(Collection<Long> idList) {
        // 批量删除脱敏清单关联关系
        return StandardsDesensitizeAssetcolumnMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDesensitizeAssetcolumnDO getDgDesensitizeAssetcolumnById(Long id) {
        return StandardsDesensitizeAssetcolumnMapper.selectDesensitizeAssetcolumnById(id);
    }

    @Override
    public StandardsDesensitizeAssetcolumnDO getDgDesensitizeAssetcolumnByAid(Long assetcolumnId) {
        //通过 assetcolumnId 获取
        return StandardsDesensitizeAssetcolumnMapper.selectOne( new LambdaQueryWrapper<StandardsDesensitizeAssetcolumnDO>().eq(StandardsDesensitizeAssetcolumnDO::getAssetcolumnId, assetcolumnId));
    }

    @Override
    public List<StandardsDesensitizeAssetcolumnDO> getDgDesensitizeAssetcolumnList() {
        return StandardsDesensitizeAssetcolumnMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDesensitizeAssetcolumnDO> getDgDesensitizeAssetcolumnMap() {
        List<StandardsDesensitizeAssetcolumnDO> StandardsDesensitizeAssetcolumnList = StandardsDesensitizeAssetcolumnMapper.selectList();
        return StandardsDesensitizeAssetcolumnList.stream()
                .collect(Collectors.toMap(
                        StandardsDesensitizeAssetcolumnDO::getId,
                        StandardsDesensitizeAssetcolumnDO -> StandardsDesensitizeAssetcolumnDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入脱敏清单关联关系数据
         *
         * @param importExcelList 脱敏清单关联关系数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDgDesensitizeAssetcolumn(List<StandardsDesensitizeAssetcolumnRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsDesensitizeAssetcolumnRespVO respVO : importExcelList) {
                try {
                    StandardsDesensitizeAssetcolumnDO StandardsDesensitizeAssetcolumnDO = BeanUtils.toBean(respVO, StandardsDesensitizeAssetcolumnDO.class);
                    Long StandardsDesensitizeAssetcolumnId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsDesensitizeAssetcolumnId != null) {
                            StandardsDesensitizeAssetcolumnDO existingDgDesensitizeAssetcolumn = StandardsDesensitizeAssetcolumnMapper.selectById(StandardsDesensitizeAssetcolumnId);
                            if (existingDgDesensitizeAssetcolumn != null) {
                                StandardsDesensitizeAssetcolumnMapper.updateById(StandardsDesensitizeAssetcolumnDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsDesensitizeAssetcolumnId + " 的脱敏清单关联关系记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsDesensitizeAssetcolumnId + " 的脱敏清单关联关系记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsDesensitizeAssetcolumnDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsDesensitizeAssetcolumnId);
                        StandardsDesensitizeAssetcolumnDO existingDgDesensitizeAssetcolumn = StandardsDesensitizeAssetcolumnMapper.selectOne(queryWrapper);
                        if (existingDgDesensitizeAssetcolumn == null) {
                            StandardsDesensitizeAssetcolumnMapper.insert(StandardsDesensitizeAssetcolumnDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsDesensitizeAssetcolumnId + " 的脱敏清单关联关系记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsDesensitizeAssetcolumnId + " 的脱敏清单关联关系记录已存在。");
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
    public PageResult<StandardsDesensitizeAssetcolumnDO> getDgDesensitizePagebyRuleId(StandardsDesensitizeAssetcolumnPageReqVO StandardsDesensitizeAssetcolumn) {
            return StandardsDesensitizeAssetcolumnMapper.selectPagebyRuleId(StandardsDesensitizeAssetcolumn);
    }

    @Override
    public StandardsDesensitizeAssetcolumnDO getByassetcolumnId(Long assetcolumnId) {
            return StandardsDesensitizeAssetcolumnMapper.selectOne( new LambdaQueryWrapper<StandardsDesensitizeAssetcolumnDO>().eq(StandardsDesensitizeAssetcolumnDO::getAssetcolumnId, assetcolumnId));
    }

    @Override
    public int deleteByassetcolumnId(Long assetcolumnId) {
            return StandardsDesensitizeAssetcolumnMapper.delete(new LambdaQueryWrapper<StandardsDesensitizeAssetcolumnDO>().eq(StandardsDesensitizeAssetcolumnDO::getAssetcolumnId, assetcolumnId));
    }

}
