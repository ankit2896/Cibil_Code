package com.freecharge.experian.transformer.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.freecharge.cibil.exceptions.FCInternalServerException;


import com.freecharge.cibil.model.pojo.CibilScoreRecord;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.model.CustomerExperianInfoModel;
import com.freecharge.experian.model.pojo.ExperianVariableList;
import com.freecharge.experian.model.response.ExperianReport;
import com.freecharge.experian.transformer.builder.ExperianScoreRecordBuilder;
import com.freecharge.experian.transformer.builder.ExperianVariablesListBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

@Slf4j
public class ExperianTransformationServiceImpl extends AbstractExperianTransformationService {

    private final ExecutorService executorService;

    public ExperianTransformationServiceImpl(@NonNull final ExecutorService executorService,
                                             @Nullable final Map<Integer, String> pinCodeMap) {
        this.executorService = executorService;
    }

    protected <R> List<R> getResponse(List<CustomerExperianInfoModel> customerExperianInfoModelList, R r) {

        final CompletableFuture<R>[] futureArray = new CompletableFuture[customerExperianInfoModelList.size()];
        log.info("Cibil Info Mddel List Size : {}", customerExperianInfoModelList.size());
        IntStream.range(0, customerExperianInfoModelList.size()).forEach(index -> {
            ExperianReport experianReport = JsonUtil.convertStringIntoObject(customerExperianInfoModelList.get(index).getExperianReport(), new TypeReference<ExperianReport>() {
            });
            final Date fetchDate = customerExperianInfoModelList.get(index).getReportUpdatedAt();
            final String userId = customerExperianInfoModelList.get(index).getCustomerId();
            futureArray[index] = CompletableFuture.supplyAsync(() ->
                            buildResponse(experianReport, fetchDate, r, userId),
                    executorService);
        });
        CompletableFuture.allOf(futureArray).join();
        final List<CompletableFuture<R>> list = Arrays.asList(futureArray);
        final List<R> result = new ArrayList<>();
        for (CompletableFuture<R> e : list) {
            try {
                result.add(e.get());
            } catch (InterruptedException ex) {
                log.error("Error occurred while getting the sum of future array with exception : {}", ex);
                Thread.currentThread().interrupt();
                throw new FCInternalServerException(ex.getMessage());
            } catch (ExecutionException ey) {
                log.error("Error occurred while getting the sum of future array with exception : {}", ey);
                throw new FCInternalServerException(ey.getMessage());
            }
        }
        log.debug("Result for Object Type {} is {}", r, result);
        return result;
    }

    protected <T, R> T buildResponse(ExperianReport experianReport, Date fetchDate, R r, String userId) {

        if (r instanceof ExperianVariableList) {
            return (T) ExperianVariablesListBuilder.buildExperianVariablesList(experianReport, fetchDate, userId);
        }
        if (r instanceof CibilScoreRecord) {
            return (T) ExperianScoreRecordBuilder.buildExperianScoreRecord(experianReport, fetchDate, userId);
        }
        return null;
    }
}
