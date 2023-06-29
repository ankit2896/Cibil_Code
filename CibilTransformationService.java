package com.freecharge.cibil.service;

import com.freecharge.cibil.model.CustomerCibilInfoModel;
import com.freecharge.cibil.model.pojo.*;

import java.util.List;

public interface CibilTransformationService {
    List<PersonalInformation> getPersonalInfoResponse(List<CustomerCibilInfoModel> customerCibilInfoModelList);
    List<CibilScoreRecord> getCibilScoreRecord(List<CustomerCibilInfoModel> customerCibilInfoModelList);
    List<ContactInformation> getContactInformation(List<CustomerCibilInfoModel> customerCibilInfoModelList);
    List<AccountInfoList> getAccountInfo(List<CustomerCibilInfoModel> customerCibilInfoModelList);
    List<EnquiryInformationList> getEnquiryInformationList(List<CustomerCibilInfoModel> customerCibilInfoModelList);
    List<CibilVariableList> getCibilVariablesList(List<CustomerCibilInfoModel> customerCibilInfoModelList);

    List<EmployerInformation> getEmployerInformationList(List<CustomerCibilInfoModel> customerCibilInfoModels);
}
