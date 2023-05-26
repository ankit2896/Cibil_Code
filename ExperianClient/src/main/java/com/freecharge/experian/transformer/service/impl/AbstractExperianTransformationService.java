package com.freecharge.experian.transformer.service.impl;

import com.freecharge.cibil.model.pojo.CibilScoreRecord;
import com.freecharge.experian.model.CustomerExperianInfoModel;
import com.freecharge.experian.model.pojo.ExperianVariableList;
import com.freecharge.experian.model.response.ExperianReport;
import com.freecharge.experian.transformer.service.ExperianTransformationService;

import java.util.Date;
import java.util.List;

public abstract class AbstractExperianTransformationService implements ExperianTransformationService {


    public List<ExperianVariableList> getExperianVariablesList(List<CustomerExperianInfoModel> customerExperianInfoModelList) {
        return getResponse(customerExperianInfoModelList, new ExperianVariableList());
    }

    public List<CibilScoreRecord> getExperianScoreRecord(List<CustomerExperianInfoModel> customerExperianInfoModelList) {
        return getResponse(customerExperianInfoModelList, new CibilScoreRecord());
    }


    protected abstract <T, R> T buildResponse(ExperianReport experianReport, Date fetchDate, R r, String userId);

    protected abstract <R> List<R> getResponse(List<CustomerExperianInfoModel> customerExperianInfoModelList, R r);
}
