package com.freecharge.cibil.cron.main;

import com.freecharge.cibil.exceptions.FCCDependencyFailureNonRetriableException;
import com.freecharge.platform.aws.sqs.handler.SqsPoller;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


@Log4j2
@Component
public class PollerScheduler {


    private final SqsPoller sqsPoller;

    @Autowired
    public PollerScheduler(@NonNull final SqsPoller sqsPoller) {
        this.sqsPoller = sqsPoller;
    }

    @PostConstruct
    public void mainSchedule() {
        try{
            log.info("Starting the poller");
            sqsPoller.pollSqs();
            log.info("Poller has been started for sqs {}");
        } catch (Exception ex) {
            log.error("Exception while Starting sqs Poller {} and exception {}", ex.getMessage(), ex);
            throw new FCCDependencyFailureNonRetriableException(ex.getMessage());
        }
    }
}
