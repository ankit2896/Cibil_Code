package com.freecharge.cibil.aws.sns.accessor.impl;

import com.freecharge.cibil.aws.service.IAmazonSNSService;
import com.freecharge.cibil.aws.sns.accessor.SNSAccessor;
import com.freecharge.cibil.model.enums.EventType;
import com.freecharge.cibil.model.pojo.ActivityTrackerSNSMessage;
import com.freecharge.generator.MD5HashGenerator;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

@Log4j2
@Component("activityTrackerSNSAccessorImpl")
public class ActivityTrackerSNSAccessorImpl implements SNSAccessor {

    private final IAmazonSNSService amazonSNSService;

    private static final String USER_TYPE = "FC_USER";

    private static final String SOURCE = "CIBIL";

    private static final String META_DATA = "CIBIL FETCH EVENT";

    public static final String GET_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    public ActivityTrackerSNSAccessorImpl(@NonNull @Qualifier("activityTrackerSNSAwsService") final IAmazonSNSService amazonSNSService) {
        this.amazonSNSService = amazonSNSService;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void publishEvent(@NonNull final String imsId, @NonNull final EventType eventType) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(GET_DATE_FORMAT);
        final String date = dateFormat.format(Timestamp.valueOf(LocalDateTime.now()));
        final ActivityTrackerSNSMessage snsMessage = ActivityTrackerSNSMessage
                .builder()
                .createdOn(date)
                .id(MD5HashGenerator.generateHash(imsId + date))
                .eventType(eventType.getValue())
                .userId(imsId)
                .userType(USER_TYPE)
                .metadata(META_DATA)
                .source(SOURCE)
                .build();
        log.info("Sending message {} for imsId {} for eventType {}", snsMessage, imsId, eventType);
        amazonSNSService.publishEvent(snsMessage);
    }
}