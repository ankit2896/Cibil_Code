package com.freecharge.cibil.aws.sns.accessor.impl;

import com.freecharge.cibil.aws.service.IAmazonSNSService;
import com.freecharge.cibil.aws.sns.accessor.SNSAccessor;
import com.freecharge.cibil.model.enums.EventType;
import com.freecharge.cibil.model.pojo.SNSMessage;
import com.freecharge.generator.MD5HashGenerator;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;

@Log4j2
@Component("sNSAccessorImpl")
public class SNSAccessorImpl implements SNSAccessor {

    private final IAmazonSNSService amazonSNSService;

    @Autowired
    public SNSAccessorImpl(@NonNull @Qualifier("sNSAwsService") final IAmazonSNSService amazonSNSService) {
        this.amazonSNSService = amazonSNSService;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void publishEvent(@NonNull final String imsId, @NonNull final EventType eventType) {
        final SNSMessage snsMessage = SNSMessage
                .builder()
                .eventType(eventType)
                .userId(imsId)
                .messageId(MD5HashGenerator.generateHash(imsId + String.valueOf(new Date().getTime())))
                .build();
        log.info("Sending message {} for imsId {} for eventType {}", snsMessage, imsId, eventType);
        amazonSNSService.publishEvent(snsMessage);
    }
}
