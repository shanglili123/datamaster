

package com.datamaster.module.taxonomy.api.client;

import org.springframework.stereotype.Service;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.client.dto.TaxonomyClientRespDTO;
import com.datamaster.module.taxonomy.service.client.ITaxonomyClientService;

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
    public TaxonomyClientRespDTO getClient(Long id) {
        return BeanUtils.toBean(clientService.getAttClientById(id), TaxonomyClientRespDTO.class);
    }
}
