

package com.datamaster.module.governance.service.client.impl;

import org.springframework.stereotype.Service;
import com.datamaster.common.api.client.ClientApi;
import com.datamaster.common.api.client.dto.ClientRespDTO;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.governance.service.client.ITaxonomyClientService;

import javax.annotation.Resource;

/**
 * 应用 Api 实现类
 * @author Ming
 */
@Service
public class ClientApiImpl implements ClientApi {

    @Resource
    private ITaxonomyClientService clientService;

    @Override
    public ClientRespDTO getClient(Long id) {
        return BeanUtils.toBean(clientService.getAttClientById(id), ClientRespDTO.class);
    }
}
