package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.component.DataMigrationComponent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.freecharge.cibil.constants.ApiUrl.*;

/**
 * Controller for Data Manipulation in prod Database
 */
@Slf4j
@RestController
public class DataMigrationController {

    private DataMigrationComponent dataMigrationComponent;

    @Autowired
    public DataMigrationController(@NonNull final DataMigrationComponent dataMigrationComponent) {
        this.dataMigrationComponent = dataMigrationComponent;
    }

    /**
     * Purge the 6-Month-old cibilReport data from the prod Database
     *
     * @return
     */
    @Logged
    @Timed
    @Marked
    @GetMapping(value = DELETE_6_MONTHS_DATA)
    public String deleteOldData() {
        log.info("start cibilReport before 6 month delete");
        dataMigrationComponent.delete6MonthsOldData();
        log.info("end cibilReport before 6 month delete");
        return "Records successfully deleted";
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = IMS_PAN)
    public String migrateDataImsPanPccMapping(@RequestParam(required = false) String index) {
        dataMigrationComponent.migrateImsPanPccMapping(index);
        return "IMS Pan Pcc Migrated";
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = CIBIL_INFO)
    public String migrateCibilInfo(@RequestParam(required = false) String index) {
        dataMigrationComponent.migrateCibilInfo(index);
        return "Cibil Info Successfully migrated!!";
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = NEW_DATA)
    public String migrateNewData(@RequestParam String date) throws Exception {
        log.info("Date from request parameter : {}", date);
        Date newDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
        log.info("New Date : {}", newDate);
        dataMigrationComponent.migrateNewData(newDate);
        return "New Data Successfully migrated!!";
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = REMAINING_IMS_PAN)
    public String migrateRemainingImsPanPccMappingData() {
        dataMigrationComponent.migrateRemainingImsPanPccMapping();
        return "Remaining IMS Pan Pcc Migrated";
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = REMAINING_CIBIL_INFO)
    public String migrateRemainingCibilInfo() {
        dataMigrationComponent.migrateRemainingCibilInfo();
        return "Remaining Cibil Info Migrated Successfully";
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = UPDATE_MOBILE_NUMBER)
    public String updateNullMobileNumber() {
        dataMigrationComponent.updateMobileNumber();
        return "Update mobile number";
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = SYNC_OLD_DATA)
    public String syncCibilReportData() {
        log.info("start sync cibilReport data");
        dataMigrationComponent.syncOldData();
        log.info("end sync cibilReport data");
        return "Records successfully synced";
    }


    @Logged
    @Timed
    @Marked
    @GetMapping(value = CIBIL_REPORT_PARSE)
    public String parseCibilReport() {
        dataMigrationComponent.parseCibilReport();
        return "Cibil report successfully parsed";
    }

}