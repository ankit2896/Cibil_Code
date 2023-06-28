package com.freecharge.cibil.aws.sns.accessor;

import com.freecharge.cibil.model.enums.EventType;

public interface SNSAccessor {

    /**
     * This Method publishes the message to sns.
     * @param imsId imsId of user for which message needs to be published.
     * @param eventType {@link EventType}.
     */
    void publishEvent(String imsId, EventType eventType);
}
