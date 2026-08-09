package com.datamaster.common.api.client;

import com.datamaster.common.api.client.dto.ClientRespDTO;

public interface ClientApi {
    ClientRespDTO getClient(Long id);
}
