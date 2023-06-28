package com.freecharge.cibil.cron;


import com.freecharge.cibil.rest.impl.ExperianServiceImpl;
import com.freecharge.experian.service.ExperianClientService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Date;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class TokenScheduler {

    @Autowired
    ExperianServiceImpl experianServiceImpl;

    @Autowired
    ExperianClientService experianClientService;

    @PostConstruct
    private void onStartup() {
        log.info("Calling Fetch Experian Token on startup");
        experianClientService.generateExperianToken();
        log.info("Token fetched from experian and saved in Dynamodb");
    }

    @SchedulerLock(name = "generateExperianToken", lockAtMostForString = "PT5M", lockAtLeastForString = "PT5M")
    @Scheduled(cron = "* */20 * * * ?") // Scheduler run at every 28th minute
    public void generateExperianToken() {
        log.info("Start Token Cron Job at {}", new Date());
        experianClientService.generateExperianToken();
        log.info("End Token Cron Job successfully at {}", new Date());
    }

}