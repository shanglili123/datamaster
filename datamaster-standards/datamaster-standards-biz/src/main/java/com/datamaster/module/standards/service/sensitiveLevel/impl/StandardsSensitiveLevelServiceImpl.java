package com.datamaster.module.standards.service.sensitiveLevel.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.sensitiveLevel.vo.StandardsSensitiveLevelPageReqVO;
import com.datamaster.module.standards.controller.admin.sensitiveLevel.vo.StandardsSensitiveLevelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.sensitiveLevel.StandardsSensitiveLevelDO;
import com.datamaster.module.standards.dal.mapper.sensitiveLevel.StandardsSensitiveLevelMapper;
import com.datamaster.module.standards.service.sensitiveLevel.IStandardsSensitiveLevelService;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * 敏感等级Service业务层处理
 *
 * @author Chaos
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsSensitiveLevelServiceImpl extends ServiceImpl<StandardsSensitiveLevelMapper, StandardsSensitiveLevelDO> implements IStandardsSensitiveLevelService {
    @Resource
    private StandardsSensitiveLevelMapper mapper;

    @Override
    public PageResult<StandardsSensitiveLevelDO> getDgSensitiveLevelPage(StandardsSensitiveLevelPageReqVO pageReqVO) {
        return mapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDgSensitiveLevel(StandardsSensitiveLevelSaveReqVO createReqVO) {
        StandardsSensitiveLevelDO dictType = BeanUtils.toBean(createReqVO, StandardsSensitiveLevelDO.class);
        mapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDgSensitiveLevel(StandardsSensitiveLevelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新敏感等级
        StandardsSensitiveLevelDO updateObj = BeanUtils.toBean(updateReqVO, StandardsSensitiveLevelDO.class);
        return mapper.updateById(updateObj);
    }

    @Override
    public int removeDgSensitiveLevel(Collection<Long> idList) {
        // 批量删除敏感等级
        return mapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsSensitiveLevelDO getDgSensitiveLevelById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public Boolean updateStatus(Long id, Long status) {
        return this.update(Wrappers.lambdaUpdate(StandardsSensitiveLevelDO.class)
                .eq(StandardsSensitiveLevelDO::getId, id)
                .set(StandardsSensitiveLevelDO::getOnlineFlag, status));
    }
}
