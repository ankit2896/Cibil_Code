package com.freecharge.experian.transformer.service;

import com.freecharge.cibil.model.pojo.CibilScoreRecord;
import com.freecharge.experian.model.CustomerExperianInfoModel;
import com.freecharge.experian.model.pojo.ExperianVariableList;

import java.util.List;

public interface ExperianTransformationService {

    List<ExperianVariableList> getExperianVariablesList(List<CustomerExperianInfoModel> customerExperianInfoModelList);

    List<CibilScoreRecord> getExperianScoreRecord(List<CustomerExperianInfoModel> customerExperianInfoModelList);


}
