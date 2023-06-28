package com.freecharge.cibil.aws.service.impl;

import com.freecharge.cibil.aws.config.AmazonSNSResourceConfig;
import com.freecharge.cibil.aws.service.IAmazonSNSService;
import com.freecharge.cibil.exceptions.FCCDependencyFailureNonRetriableException;
import com.freecharge.cibil.model.pojo.SNSMessage;
import com.freecharge.cibil.utils.JsonUtil;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Log4j2
@Service("sNSAwsService")
public class SNSAwsService implements IAmazonSNSService<SNSMessage> {

    private final AmazonSNSResourceConfig amazonSNSResourceConfig;

    private final SnsClient snsClient;

    private final String snsArn;

    @Autowired
    public SNSAwsService(@NonNull final AmazonSNSResourceConfig amazonSNSResourceConfig,
                        @NonNull @Qualifier("snsClient") final SnsClient snsClient) {
        this.amazonSNSResourceConfig = amazonSNSResourceConfig;
        this.snsArn = amazonSNSResourceConfig.getSnsArn();
        this.snsClient = snsClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publishEvent(@NonNull final SNSMessage snsMessage) {
        try {
            final PublishRequest publishRequest = PublishRequest.builder()
                    .targetArn(snsArn)
                    .message(JsonUtil.writeValueAsString(snsMessage))
                    .build();
            log.info("Publishing Message into SNS {}", snsMessage);
            final PublishResponse publishResponse = snsClient.publish(publishRequest);
            log.info("Message successfully established {} and response was {}", publishRequest, publishResponse);
        } catch (Exception e) {
            log.error("Exception occured while publishing message with exception {}", e);
            throw new FCCDependencyFailureNonRetriableException(e.getMessage());
        }
    }
}
