

package com.datamaster.module.taxonomy.service.client.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.client.TaxonomyClientApiRelDO;
import com.datamaster.module.taxonomy.dal.mapper.client.TaxonomyClientApiRelMapper;
import com.datamaster.module.taxonomy.service.client.ITaxonomyClientApiRelService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 应用API服务关联Service业务层处理
 *
 * @author FXB
 * @date 2025-08-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyClientApiRelServiceImpl  extends ServiceImpl<TaxonomyClientApiRelMapper,TaxonomyClientApiRelDO> implements ITaxonomyClientApiRelService {
    @Resource
    private TaxonomyClientApiRelMapper TaxonomyClientApiRelMapper;

    @Override
    public PageResult<TaxonomyClientApiRelDO> getAttClientApiRelPage(TaxonomyClientApiRelPageReqVO pageReqVO) {
        return TaxonomyClientApiRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttClientApiRel(TaxonomyClientApiRelSaveReqVO createReqVO) {
        TaxonomyClientApiRelDO dictType = BeanUtils.toBean(createReqVO, TaxonomyClientApiRelDO.class);
        TaxonomyClientApiRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttClientApiRel(TaxonomyClientApiRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新应用API服务关联
        TaxonomyClientApiRelDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyClientApiRelDO.class);
        return TaxonomyClientApiRelMapper.updateById(updateObj);
    }
    @Override
    public int removeAttClientApiRel(Collection<Long> idList) {
        // 批量删除应用API服务关联
        return TaxonomyClientApiRelMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyClientApiRelDO getAttClientApiRelById(Long id) {
        return TaxonomyClientApiRelMapper.selectById(id);
    }

    @Override
    public List<TaxonomyClientApiRelDO> getAttClientApiRelList() {
        return TaxonomyClientApiRelMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyClientApiRelDO> getAttClientApiRelMap() {
        List<TaxonomyClientApiRelDO> TaxonomyClientApiRelList = TaxonomyClientApiRelMapper.selectList();
        return TaxonomyClientApiRelList.stream()
                .collect(Collectors.toMap(
                        TaxonomyClientApiRelDO::getId,
                        TaxonomyClientApiRelDO -> TaxonomyClientApiRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入应用API服务关联数据
         *
         * @param importExcelList 应用API服务关联数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importAttClientApiRel(List<TaxonomyClientApiRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (TaxonomyClientApiRelRespVO respVO : importExcelList) {
                try {
                    TaxonomyClientApiRelDO TaxonomyClientApiRelDO = BeanUtils.toBean(respVO, TaxonomyClientApiRelDO.class);
                    Long TaxonomyClientApiRelId = respVO.getId();
                    if (isUpdateSupport) {
                        if (TaxonomyClientApiRelId != null) {
                            TaxonomyClientApiRelDO existingAttClientApiRel = TaxonomyClientApiRelMapper.selectById(TaxonomyClientApiRelId);
                            if (existingAttClientApiRel != null) {
                                TaxonomyClientApiRelMapper.updateById(TaxonomyClientApiRelDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + TaxonomyClientApiRelId + " 的应用API服务关联记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + TaxonomyClientApiRelId + " 的应用API服务关联记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<TaxonomyClientApiRelDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", TaxonomyClientApiRelId);
                        TaxonomyClientApiRelDO existingAttClientApiRel = TaxonomyClientApiRelMapper.selectOne(queryWrapper);
                        if (existingAttClientApiRel == null) {
                            TaxonomyClientApiRelMapper.insert(TaxonomyClientApiRelDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + TaxonomyClientApiRelId + " 的应用API服务关联记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + TaxonomyClientApiRelId + " 的应用API服务关联记录已存在。");
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
