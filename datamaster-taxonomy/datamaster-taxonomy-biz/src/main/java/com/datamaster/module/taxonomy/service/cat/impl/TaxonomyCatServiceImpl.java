

package com.datamaster.module.taxonomy.service.cat.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.taxonomy.api.cat.ITaxonomyCatService;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyCatMapper;

import javax.annotation.Resource;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-11 16:53
 **/
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyCatServiceImpl implements ITaxonomyCatService {

    @Resource
    private TaxonomyCatMapper TaxonomyCatMapper;

    @Override
    public Long getCatIdByTableNameAndCatCode(String tableName, String catCode) {
        return TaxonomyCatMapper.getCatIdByTableNameAndCatCode(tableName, catCode);
    }
}
