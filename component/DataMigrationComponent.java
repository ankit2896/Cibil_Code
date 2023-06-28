package com.freecharge.cibil.component;


import com.freecharge.cibil.config.MysqlConfig;
import com.freecharge.cibil.mysql.accessor.DataMigrationAccessor;
import com.freecharge.cibil.mysql.entity.*;
import com.freecharge.cibil.mysql.repository.impl.*;
import com.freecharge.cibil.rest.KycInformationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class DataMigrationComponent {

    @Autowired
    private CibilInfoRepository cibilInfoRepository;

    @Autowired
    private CustomerCibilInfoRepository customerCibilInfoRepository;

    @Autowired
    private ImsPanPccMappingRepository imsPanPccMappingRepository;

    @Autowired
    private CustomerPccMappingRepository customerPccMappingRepository;

    @Autowired
    private CustomerInfoRepository customerInfoRepository;

    @Autowired
    private MigrateJpaRepository migrateJpaRepository;


    @Autowired
    private KycInformationService kycInformationService;

    @Autowired
    private MysqlConfig mysqlConfig;

    @Autowired
    private DataMigrationAccessor dataMigrationAccessor;

    @Autowired
    private CustomerInfoMigrateJpaRepository customerInfoMigrateJpaRepository;

    @Autowired
    private CustomerCibilInfoJpaRepository customerCibilInfoJpaRepository;

    @Autowired
    private CustomerDuplicateRepository customerDuplicateRepository;

//    @PersistenceContext(unitName = "entManager")
//    private EntityManager entityManager;

    public boolean migrateCibilInfo(String index) {
        int pageIndex = 0;
        int size = 1000;
        boolean isBatchFailed = false;
        if (Objects.nonNull(index)) {
            log.info("Cibil Info Failure Index : {}", index);
            pageIndex = Integer.parseInt(index);
            isBatchFailed = true;
        }

        long startTime = System.currentTimeMillis();
        log.info("Cibil Info starting time: {}", startTime);

        Map<Integer, List<CustomerInfo>> failureListMap = new HashMap<>();

        try {
            while (true) {
                // Create a PageRequest object that will be passed as Pageable interface to repo
                PageRequest pageRequest = PageRequest.of(pageIndex, size);
                log.info("Cibil Info Page Index : {} ", pageIndex);
                log.info("Cibil Info Page Size : {} ", size);

                List<CustomerInfo> customerInfos = customerInfoMigrateJpaRepository.findAll(pageRequest).getContent();
                log.info("Number of Customer Info records: " + customerInfos.size());

                // Check if data is there
                if (customerInfos == null || customerInfos.isEmpty()) {
                    break;
                }

                try {
                    dataMigrationAccessor.migrateCibilInfo(customerInfos, pageIndex);
                } catch (Exception e) {
//                    log.info("Customer Info Transaction fail with index {}, first data {}, last data {}", pageIndex,
//                            customerInfos.get(0),
//                            customerInfos.get(customerInfos.size() - 1));
                    failureListMap.put(pageIndex, customerInfos);
                }

                // In case of batch failed request , we will only process that batch
                if (isBatchFailed)
                    break;

                // Increment the pageIndex
                pageIndex++;
                log.info("------------Next Page-----------");

            }
            log.info("Successfully migrated Cibil Info table");

            long endTime = System.currentTimeMillis();
            log.info("end time: {}", endTime);

            long timeElapsed = endTime - startTime;

            log.info("time elapsed {} in millisecond", timeElapsed);

            System.out.println("Execution time in milliseconds: " + timeElapsed);

            return true;
        } catch (Exception e) {
            log.error("Exception while migrating Cibil Info : " + e);
            return false;
        } finally {
            //entityManager.close();
        }

    }

    public boolean migrateRemainingImsPanPccMapping() {
        //batch processing
        // pagination
        //auto commit
        int pageIndex = 0;
        int size = 1000;
        long startTime = System.currentTimeMillis();
        log.info("starting time: {}", startTime);

        try {
            while (true) {
                PageRequest pageRequest = PageRequest.of(pageIndex, size);
                log.info("Remaining Ims Pan Page Index : {} ", pageIndex);
                log.info("Remaining Ims Pan Page Size : {} ", size);
                List<ImsPanPccMappingEntity> imsPanPccMappingEntities = migrateJpaRepository.getRemainingData(pageRequest).getContent();
                log.info("Number of Remaining ImsPanPccMappingEntity records: " + imsPanPccMappingEntities.size());

                if (imsPanPccMappingEntities == null || imsPanPccMappingEntities.isEmpty())
                    break;

                try {
                    dataMigrationAccessor.migrateImsPanPccMappingData(imsPanPccMappingEntities, pageIndex);
                } catch (Exception e) {
                    log.info("Transaction fail with index {}, first data {}, last data {}", pageIndex,
                            imsPanPccMappingEntities.get(0),
                            imsPanPccMappingEntities.get(imsPanPccMappingEntities.size() - 1));
                }
                pageIndex++;

                log.info("------------Next Page-----------");
            }

            log.info("Successfully migrated remaining ImsPanPccMapping table");

            long endTime = System.currentTimeMillis();
            log.info("end time: {}", endTime);

            long timeElapsed = endTime - startTime;

            log.info("time elapsed {} in millisecond", timeElapsed);

            System.out.println("Execution time in milliseconds: " + timeElapsed);

            return true;
        } catch (Exception e) {
            log.error("Exception while migrating remaining ImsPanPccMapping: " + e);
            return false;
        }
    }

    public void migrateNewData(Date date) throws Exception {
        List<ImsPanPccMappingEntity> panPccMappingEntities = imsPanPccMappingRepository.
                getRecordsFromTimeStamp(date);

        log.info("No of IMSPanPcc records : {}", panPccMappingEntities.size());

        dataMigrationAccessor.migrateImsPanPccMappingData(panPccMappingEntities, 0);

        log.info("Checking for the same no of records in Customer Info");
        List<CustomerInfo> customerInfoList = customerInfoMigrateJpaRepository.
                getRecordsFromTimeStamp(date);

        log.info("No of Customer Info records : {}", customerInfoList.size());

        dataMigrationAccessor.migrateCibilInfo(customerInfoList, 0);

        log.info("Data successfully migrated");
    }

    public boolean migrateImsPanPccMapping(String index) {
        //batch processing
        // pagination
        //auto commit
        int pageIndex = 0;
        boolean isBatchFailed = false;
        if (Objects.nonNull(index)) {
            log.info("Failure Index : {}", index);
            pageIndex = Integer.parseInt(index);
            isBatchFailed = true;
        }
        int size = 1000;
        //     EntityTransaction entityTransaction = entityManager.getTransaction();
        long startTime = System.currentTimeMillis();
        log.info("starting time: {}", startTime);

        Map<Integer, List<ImsPanPccMappingEntity>> failureListMap = new HashMap<>();

        try {
            while (true) {
                // Create a PageRequest object that will be passed as Pageable interface to repo
                PageRequest pageRequest = PageRequest.of(pageIndex, size);
                log.info("Page Index : {} ", pageIndex);
                log.info("Page Size : {} ", size);


                List<ImsPanPccMappingEntity> imsPanPccMappingEntities = migrateJpaRepository.findAll(pageRequest).getContent();
                log.info("Number of ImsPanPccMappingEntity records: " + imsPanPccMappingEntities.size());

                // Check if data is there
                if (imsPanPccMappingEntities == null || imsPanPccMappingEntities.isEmpty()) {
                    break;
                }

                try {
                    dataMigrationAccessor.migrateImsPanPccMappingData(imsPanPccMappingEntities, pageIndex);
                } catch (Exception e) {
                    log.info("Transaction fail with index {}, first data {}, last data {}", pageIndex,
                            imsPanPccMappingEntities.get(0),
                            imsPanPccMappingEntities.get(imsPanPccMappingEntities.size() - 1));
                    failureListMap.put(pageIndex, imsPanPccMappingEntities);
                }

                // In case of batch failed request , we will only process that batch
                if (isBatchFailed)
                    break;

                // Increment the pageIndex
                pageIndex++;
                log.info("------------Next Page-----------");

            }
            log.info("Successfully migrated ImsPanPccMapping table");

            long endTime = System.currentTimeMillis();
            log.info("end time: {}", endTime);

            long timeElapsed = endTime - startTime;

            log.info("time elapsed {} in millisecond", timeElapsed);

            System.out.println("Execution time in milliseconds: " + timeElapsed);
            return true;
        } catch (Exception e) {
            log.error("Exception while migrating ImsPanPccMapping: " + e);
            //  entityTransaction.rollback();
            return false;
        } finally {
            //entityManager.close();
        }
    }


    public void migrateRemainingCibilInfo() {
        int pageIndex = 0;
        int size = 1000;

        long startTime = System.currentTimeMillis();
        log.info("Remaining Cibil Info starting time: {}", startTime);

        Map<Integer, List<CustomerInfo>> failureListMap = new HashMap<>();

        try {
            while (true) {
                // Create a PageRequest object that will be passed as Pageable interface to repo
                PageRequest pageRequest = PageRequest.of(pageIndex, size);
                log.info("Remaining Cibil Info Page Index : {} ", pageIndex);
                log.info("Remaining Cibil Info Page Size : {} ", size);

                List<CustomerInfo> customerInfos = customerInfoMigrateJpaRepository.getRemainingRecords(pageRequest).getContent();
                log.info("Number of Remaining Customer Info records: " + customerInfos.size());

                // Check if data is there
                if (customerInfos == null || customerInfos.isEmpty()) {
                    break;
                }

                try {
                    dataMigrationAccessor.migrateCibilInfo(customerInfos, pageIndex);
                } catch (Exception e) {
//                    log.info("Customer Info Transaction fail with index {}, first data {}, last data {}", pageIndex,
//                            customerInfos.get(0),
//                            customerInfos.get(customerInfos.size() - 1));
                    failureListMap.put(pageIndex, customerInfos);
                }

                // In case of batch failed request , we will only process that batch

                // Increment the pageIndex
                pageIndex++;
                log.info("------------Next Page-----------");

            }
            log.info("Successfully migrated remaining Cibil Info table");

            long endTime = System.currentTimeMillis();
            log.info("end time: {}", endTime);

            long timeElapsed = endTime - startTime;

            log.info("time elapsed {} in millisecond", timeElapsed);

            System.out.println("Execution time in milliseconds: " + timeElapsed);

        } catch (Exception e) {
            log.error("Exception while migrating Cibil Info : " + e);
        } finally {
            //entityManager.close();
        }

    }

    public void updateMobileNumber() {
        int pageIndex = 0;
        int size = 1000;

        long startTime = System.currentTimeMillis();
        log.info("Update mobile number starting time: {}", startTime);

        try {
            while (true) {
                // Create a PageRequest object that will be passed as Pageable interface to repo
                PageRequest pageRequest = PageRequest.of(pageIndex, size);
                log.info("Update mobile number Page Index : {} ", pageIndex);
                log.info("Update mobile number Page Size : {} ", size);

                List<CustomerInfo> customerInfos = customerInfoMigrateJpaRepository.getRecordsForNullMobileNumber(pageRequest).getContent();
                log.info("Number of Customer Info records : " + customerInfos.size());

                // Check if data is there
                if (customerInfos == null || customerInfos.isEmpty()) {
                    break;
                }

                try {
                    dataMigrationAccessor.updateMobilNumber(customerInfos);
                } catch (Exception e) {
                    log.info("Failure Index : {}", pageIndex);
                }

                // Increment the pageIndex
                pageIndex++;
                log.info("------------Next Page-----------");
            }
            log.info("Successfully updated mobile number for all records");
        } catch (Exception e) {
            log.info("Error while updating mobile number");
        }
    }


    public void parseCibilReport() {
        // Bring data from customer_cibil_info table into batches of 1000s
        // call the VCC_DATA_INSERT api method to parse and save the cibil report into dynamo db

        int pageIndex = 0;
        int size = 1000;
        long startTime = System.currentTimeMillis();
        log.info("parsing cibil report in batches start time :{}", startTime);
        try {
            while (true) {
                PageRequest pageRequest = PageRequest.of(pageIndex, size);
                log.info("Cibil Report Page Index : {} ", pageIndex);
                log.info("Cibil Report Page Size : {} ", size);
                List<CustomerCibilInfo> customerCibilInfos = customerCibilInfoRepository.findAll(pageRequest).getContent();
                log.info("Number of Customer Info records : " + customerCibilInfos.size());

                //check if data is there
                if (customerCibilInfos == null || customerCibilInfos.isEmpty())
                    break;

                try {
                    dataMigrationAccessor.ParsingReportIntoDynamoDb(customerCibilInfos);
                } catch (Exception e) {
                    log.info("Failure Index : {}", pageIndex);
                }
                //Increment the page Index
                pageIndex++;
                log.info("------------Next Page-----------");
            }
            log.info("Successfully report parsed for all records");
        } catch (Exception e) {
            log.info("Error while parsing cibil report");
        }
    }

    public void delete6MonthsOldData() {
        int pageIndex = 0;
        int size = 1000;
        try {
            while (true) {

                PageRequest pageRequest = PageRequest.of(pageIndex, size);
                log.info("Page Request : {}", pageRequest);

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MONTH, -6);
                Date startDate = calendar.getTime();
                log.info("Start Date : {}", startDate);

                List<CustomerCibilInfo> cibilInfos = customerCibilInfoJpaRepository.
                        findCustomerCibilInfosByCibilReportNotNullAndReportUpdatedAtLessThan(startDate, pageRequest).getContent();
                log.info("No of old non null customer cibil info records : {}", cibilInfos.size());

                if (Objects.isNull(cibilInfos) || cibilInfos.isEmpty())
                    break;

                try {
                    dataMigrationAccessor.deleteOldData(cibilInfos);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("Exception while processing page Index : {}", pageIndex);
                }
                pageIndex++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Error while deleting old data");
        }


    }

    public void syncOldData() {
        int pageIndex = 0;
        int size = 1000;
        try {
            while (true) {
                PageRequest pageRequest = PageRequest.of(pageIndex, size);
                log.info("Page Request : {}", pageRequest);

                List<CustomerDuplicate> customerDuplicates = customerDuplicateRepository.findAll(pageRequest).getContent();
                log.info("No of old sync customer records : {}", customerDuplicates.size());

                if (Objects.isNull(customerDuplicates) || customerDuplicates.isEmpty())
                    break;

                try {
                    dataMigrationAccessor.syncOldData(customerDuplicates);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("Exception while processing page Index : {}", pageIndex);
                }
                pageIndex++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Error while deleting old data");
        }
    }
}


