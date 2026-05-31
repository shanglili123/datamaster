

package com.datamaster.module.standards.service.whitelist.impl;

import java.util.*;
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
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistPageReqVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistRespVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeRuleDO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeUserRelDO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeWhitelistDO;
import com.datamaster.module.standards.dal.mapper.dataCategory.StandardsDataCategoryMapper;
import com.datamaster.module.standards.dal.mapper.whitelist.StandardsDesensitizeUserRelMapper;
import com.datamaster.module.standards.dal.mapper.whitelist.StandardsDesensitizeWhitelistMapper;
import com.datamaster.module.standards.service.whitelist.IStandardsDesensitizeWhitelistService;
/**
 * 脱敏白名单Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDesensitizeWhitelistServiceImpl  extends ServiceImpl<StandardsDesensitizeWhitelistMapper,StandardsDesensitizeWhitelistDO> implements IStandardsDesensitizeWhitelistService {
    @Resource
    private StandardsDesensitizeWhitelistMapper StandardsDesensitizeWhitelistMapper;

    @Resource
    private StandardsDesensitizeUserRelMapper StandardsDesensitizeUserRelMapper;

    @Resource
    private StandardsDataCategoryMapper StandardsDataCategoryMapper;

    @Override
    public PageResult<StandardsDesensitizeWhitelistDO> getDgDesensitizeWhitelistPage(StandardsDesensitizeWhitelistPageReqVO pageReqVO) {
        PageResult<StandardsDesensitizeWhitelistDO> pageResult = StandardsDesensitizeWhitelistMapper.selectPage(pageReqVO);
        //根据脱敏白名单ID 查询用户集合存入StandardsDesensitizeWhitelistDO
        pageResult.getRows().forEach(item -> {
            item.setUserList(StandardsDesensitizeUserRelMapper.selectList(new LambdaQueryWrapper<StandardsDesensitizeUserRelDO>().eq(StandardsDesensitizeUserRelDO::getDesensitizeId, item.getId())));
        });
        return pageResult;
    }

    @Override
    public Long createDgDesensitizeWhitelist(StandardsDesensitizeWhitelistSaveReqVO createReqVO) {
        StandardsDesensitizeWhitelistDO dictType = BeanUtils.toBean(createReqVO, StandardsDesensitizeWhitelistDO.class);
        //判断分类是否已在白名单中存在
        if (StandardsDesensitizeWhitelistMapper.selectCount(new LambdaQueryWrapper<StandardsDesensitizeWhitelistDO>()
                .eq(StandardsDesensitizeWhitelistDO::getDataCategoryId, dictType.getDataCategoryId())) > 0) {
            throw new IllegalArgumentException("数据分类已存在");
        }
        StandardsDesensitizeWhitelistMapper.insert(dictType);
        // 插入用户集合
        if (dictType.getUserList() != null && !dictType.getUserList().isEmpty()) {
            dictType.getUserList().forEach(user -> {
                user.setDesensitizeId(dictType.getId());
                user.setDesensitizeName(dictType.getName());
            });
            StandardsDesensitizeUserRelMapper.insertBatch(dictType.getUserList());
        }
        return dictType.getId();
    }
    @Override
    public int updateDgDesensitizeWhitelist(StandardsDesensitizeWhitelistSaveReqVO updateReqVO) {
        // 相关校验
        // 更新脱敏白名单
        StandardsDesensitizeWhitelistDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDesensitizeWhitelistDO.class);
        if(updateObj.getUserList() != null && !updateObj.getUserList().isEmpty()){
            //先删除旧的用户集合
            StandardsDesensitizeUserRelMapper.delete(new LambdaQueryWrapper<StandardsDesensitizeUserRelDO>().eq(StandardsDesensitizeUserRelDO::getDesensitizeId, updateObj.getId()));
            // 更新用户集合
            if (updateObj.getUserList() != null && !updateObj.getUserList().isEmpty()) {
                updateObj.getUserList().forEach(user -> {
                    user.setDesensitizeId(updateObj.getId());
                    user.setDesensitizeName(updateObj.getName());
                });
                StandardsDesensitizeUserRelMapper.insertBatch(updateObj.getUserList());
            }
        }
        return StandardsDesensitizeWhitelistMapper.updateById(updateObj);
    }
    @Override
    public int removeDgDesensitizeWhitelist(Collection<Long> idList) {
        // 先删除旧的用户集合
        StandardsDesensitizeUserRelMapper.delete(new LambdaQueryWrapper<StandardsDesensitizeUserRelDO>().in(StandardsDesensitizeUserRelDO::getDesensitizeId, idList));
        // 批量删除脱敏白名单
        return StandardsDesensitizeWhitelistMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDesensitizeWhitelistDO getDgDesensitizeWhitelistById(Long id) {
        //根据脱敏白名单ID 查询用户集合存入StandardsDesensitizeWhitelistDO
        StandardsDesensitizeWhitelistDO whitelistDO = StandardsDesensitizeWhitelistMapper.selectById(id);

        //将StandardsDesensitizeWhitelistDO中分类ID转换为分类名称
        if (whitelistDO != null && whitelistDO.getDataCategoryId()!=null) {
            whitelistDO.setDataCategoryName(StandardsDataCategoryMapper.selectById(whitelistDO.getDataCategoryId()).getName());
        }

        if (whitelistDO != null) {
            //根据脱敏白名单ID 查询用户集合存入StandardsDesensitizeWhitelistDO
            whitelistDO.setUserList(StandardsDesensitizeUserRelMapper.selectList(new LambdaQueryWrapper<StandardsDesensitizeUserRelDO>().eq(StandardsDesensitizeUserRelDO::getDesensitizeId, id)));
        }
        return whitelistDO;
    }

    @Override
    public StandardsDesensitizeWhitelistDO getDgDesensitizeWhitelistByCategoryId(Long categoryId) {
        //根据脱敏白名单ID 查询用户集合存入StandardsDesensitizeWhitelistDO
        StandardsDesensitizeWhitelistDO whitelistDO = StandardsDesensitizeWhitelistMapper.selectOne(new LambdaQueryWrapper<StandardsDesensitizeWhitelistDO>().eq(StandardsDesensitizeWhitelistDO::getDataCategoryId, categoryId));

        if (whitelistDO != null) {
            //根据脱敏白名单ID 查询用户集合存入StandardsDesensitizeWhitelistDO
            whitelistDO.setUserList(StandardsDesensitizeUserRelMapper.selectList(new LambdaQueryWrapper<StandardsDesensitizeUserRelDO>().eq(StandardsDesensitizeUserRelDO::getDesensitizeId, whitelistDO.getId())));
        }
        return whitelistDO;
    }


    @Override
    public List<StandardsDesensitizeWhitelistDO> getDgDesensitizeWhitelistList() {
        return StandardsDesensitizeWhitelistMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDesensitizeWhitelistDO> getDgDesensitizeWhitelistMap() {
        List<StandardsDesensitizeWhitelistDO> StandardsDesensitizeWhitelistList = StandardsDesensitizeWhitelistMapper.selectList();
        return StandardsDesensitizeWhitelistList.stream()
                .collect(Collectors.toMap(
                        StandardsDesensitizeWhitelistDO::getId,
                        StandardsDesensitizeWhitelistDO -> StandardsDesensitizeWhitelistDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入脱敏白名单数据
         *
         * @param importExcelList 脱敏白名单数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDgDesensitizeWhitelist(List<StandardsDesensitizeWhitelistRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsDesensitizeWhitelistRespVO respVO : importExcelList) {
                try {
                    StandardsDesensitizeWhitelistDO whitelistDO = BeanUtils.toBean(respVO, StandardsDesensitizeWhitelistDO.class);
                    Long StandardsDesensitizeWhitelistId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsDesensitizeWhitelistId != null) {
                            StandardsDesensitizeWhitelistDO existingDgDesensitizeWhitelist = StandardsDesensitizeWhitelistMapper.selectById(StandardsDesensitizeWhitelistId);
                            if (existingDgDesensitizeWhitelist != null) {
                                StandardsDesensitizeWhitelistMapper.updateById(whitelistDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsDesensitizeWhitelistId + " 的脱敏白名单记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsDesensitizeWhitelistId + " 的脱敏白名单记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsDesensitizeWhitelistDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsDesensitizeWhitelistId);
                        StandardsDesensitizeWhitelistDO existingDgDesensitizeWhitelist = StandardsDesensitizeWhitelistMapper.selectOne(queryWrapper);
                        if (existingDgDesensitizeWhitelist == null) {
                            StandardsDesensitizeWhitelistMapper.insert(whitelistDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsDesensitizeWhitelistId + " 的脱敏白名单记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsDesensitizeWhitelistId + " 的脱敏白名单记录已存在。");
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
