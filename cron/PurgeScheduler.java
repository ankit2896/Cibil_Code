package com.freecharge.cibil.cron;

import com.freecharge.cibil.component.DataMigrationComponent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class PurgeScheduler {
    @Autowired
    private DataMigrationComponent dataMigrationComponent;


    @SchedulerLock(name = "deleteOldRecords", lockAtMostForString = "PT5M", lockAtLeastForString = "PT5M")
    @Scheduled(cron = "0 30 0 * * ?") // Scheduler run @12:30 AM daily to purge 6 months old cibil data
    public void deleteOldRecords() {
        log.info("Start Cron Job at {}", LocalDate.now());
        dataMigrationComponent.delete6MonthsOldData();
        log.info("End Cron Job successfully at {}", LocalDate.now());
    }
}