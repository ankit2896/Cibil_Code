package com.freecharge.cibil.aws.service;

public interface IAmazonSNSService<T> {

    /**
     * This Method publishes the event depicting that user data has been pulled from transunion.
     * This event is for upstream services to consume.
     * @param snsMessage {@link T} Message to be published.
     */
    void publishEvent(T snsMessage);
}
