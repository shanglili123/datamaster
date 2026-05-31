

package com.datamaster.module.taxonomy.service.client.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientRespVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.client.TaxonomyClientDO;
import com.datamaster.module.taxonomy.dal.mapper.client.TaxonomyClientMapper;
import com.datamaster.module.taxonomy.service.client.ITaxonomyClientService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 应用管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-02-18
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyClientServiceImpl  extends ServiceImpl<TaxonomyClientMapper,TaxonomyClientDO> implements ITaxonomyClientService {
    @Resource
    private TaxonomyClientMapper TaxonomyClientMapper;

    @Override
    public PageResult<TaxonomyClientDO> getAttClientPage(TaxonomyClientPageReqVO pageReqVO) {
        return TaxonomyClientMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttClient(TaxonomyClientSaveReqVO createReqVO) {
        TaxonomyClientDO dictType = BeanUtils.toBean(createReqVO, TaxonomyClientDO.class);
        dictType.setSecret(IdUtil.fastSimpleUUID());
        TaxonomyClientMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttClient(TaxonomyClientSaveReqVO updateReqVO) {
        // 相关校验

        // 更新应用管理
        TaxonomyClientDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyClientDO.class);
        return TaxonomyClientMapper.updateById(updateObj);
    }
    @Override
    public int removeAttClient(Collection<Long> idList) {
        // 批量删除应用管理
        return TaxonomyClientMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyClientDO getAttClientById(Long id) {
        return TaxonomyClientMapper.selectById(id);
    }

    @Override
    public List<TaxonomyClientDO> getAttClientList() {
        return TaxonomyClientMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyClientDO> getAttClientMap() {
        List<TaxonomyClientDO> TaxonomyClientList = TaxonomyClientMapper.selectList();
        return TaxonomyClientList.stream()
                .collect(Collectors.toMap(
                        TaxonomyClientDO::getId,
                        TaxonomyClientDO -> TaxonomyClientDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入应用管理数据
         *
         * @param importExcelList 应用管理数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importAttClient(List<TaxonomyClientRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (TaxonomyClientRespVO respVO : importExcelList) {
                try {
                    TaxonomyClientDO TaxonomyClientDO = BeanUtils.toBean(respVO, TaxonomyClientDO.class);
                    Long TaxonomyClientId = respVO.getId();
                    if (isUpdateSupport) {
                        if (TaxonomyClientId != null) {
                            TaxonomyClientDO existingAttClient = TaxonomyClientMapper.selectById(TaxonomyClientId);
                            if (existingAttClient != null) {
                                TaxonomyClientMapper.updateById(TaxonomyClientDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + TaxonomyClientId + " 的应用管理记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + TaxonomyClientId + " 的应用管理记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<TaxonomyClientDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", TaxonomyClientId);
                        TaxonomyClientDO existingAttClient = TaxonomyClientMapper.selectOne(queryWrapper);
                        if (existingAttClient == null) {
                            TaxonomyClientMapper.insert(TaxonomyClientDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + TaxonomyClientId + " 的应用管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + TaxonomyClientId + " 的应用管理记录已存在。");
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
