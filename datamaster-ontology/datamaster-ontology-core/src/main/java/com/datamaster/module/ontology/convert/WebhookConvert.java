package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookLogRespVO;
import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookRespVO;
import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.WebhookDO;
import com.datamaster.module.ontology.dal.dataobject.WebhookLogDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface WebhookConvert {

    WebhookConvert INSTANCE = Mappers.getMapper(WebhookConvert.class);

    WebhookDO convert(WebhookSaveReqVO bean);

    WebhookRespVO convert(WebhookDO bean);

    List<WebhookRespVO> convertList(List<WebhookDO> list);

    WebhookLogRespVO convert(WebhookLogDO bean);

    List<WebhookLogRespVO> convertLogList(List<WebhookLogDO> list);
}