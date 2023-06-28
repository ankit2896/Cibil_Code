package com.freecharge.cibil.component;

import com.freecharge.cibil.exceptions.FCCException;
import com.freecharge.cibil.model.assetresponse.CustomerAssetSuccess;
import com.freecharge.cibil.model.enums.*;
import com.freecharge.cibil.model.request.AddCustomerAssetSuccessRequest;
import com.freecharge.cibil.model.request.DynamodbDataInsertionRequest;
import com.freecharge.cibil.model.response.*;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.model.CustomerInfoModel;
import com.freecharge.cibil.mysql.repository.impl.CustomerInfoRepository;
import com.freecharge.cibil.rest.CibilParsingService;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.enums.BureauType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Component
public class CibilReportParsingComponent {

    private final CustomerInfoRepository customerInfoRepository;
    private final CibilParsingService cibilParsingService;
    private final CibilReportClientComponent cibilReportClientComponent;
    private final CibilTnCComponent cibilTnCComponent;

    @Autowired
    private CustomerInfoMappingAccessor customerInfoMappingAccessor;

    public CibilReportParsingComponent(@NonNull final CustomerInfoRepository customerInfoRepository,
                                       @NonNull final CibilParsingService cibilParsingService,
                                       @NonNull final CibilReportClientComponent cibilReportClientComponent,
                                       @NonNull final CibilTnCComponent cibilTnCComponent) {
        this.customerInfoRepository = customerInfoRepository;
        this.cibilParsingService = cibilParsingService;
        this.cibilReportClientComponent = cibilReportClientComponent;
        this.cibilTnCComponent = cibilTnCComponent;
    }


    public ServiceResponse<CibilParsing> fetchCibilReport(String userId, String panCardNumber, String mobileNumber, boolean tncConsent, String fcChannel, ReportPullType reportPullType) {
        try {
            log.info("panCardNumber: {} mobileNumber: {} tncConsent: {} fcChannel: {}", panCardNumber, mobileNumber, tncConsent, fcChannel);

            if (!fcChannel.equals(ChannelCode.VCC.getValue())) {
                return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.INCORRECT_VCC_CHANNEL_CODE), false);
            }

            if (!tncConsent) {
                return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.NOT_VALID_TNC_STATUS), false);
            }

            CustomerInfoModel customerInfoModel = customerInfoMappingAccessor.getRecordWithCibilInfo(mobileNumber);

            if(Objects.isNull(customerInfoModel.getCustomerId()) && reportPullType.equals(ReportPullType.GET_ONLY)){
                return new ServiceResponse<>(new CibilParsing(), null, true);
            }

            // Old User
            if (Objects.nonNull(customerInfoModel.getCustomerId()) && Objects.nonNull(customerInfoModel.getCustomerCibilInfoModel().getCibilReport())) {
                CibilParsing cibilParsing = cibilParsingService.getDetails(customerInfoModel.getCustomerId());

                if (Objects.isNull(cibilParsing)) {
                    // if data present in sqlDB not in dynamoDB
                    cibilParsing = insertReportIntoDynamodb(customerInfoModel);
                }

                if (reportPullType.equals(ReportPullType.GET_ONLY)) {
                    return new ServiceResponse<>(cibilParsing, null, true);
                } else {
                    if (Objects.nonNull(customerInfoModel.getCustomerCibilInfoModel().getCibilReport())) {
                        ServiceResponse<CibilParsing> response = refetchReportBasedOnPullType(cibilParsing, customerInfoModel, tncConsent, fcChannel, reportPullType);
                        if (!response.isSuccess()) {
                            return new ServiceResponse<>(cibilParsing, true);
                        }
                        return response;
                    }
                }
            }else{
                // Check for user with null cibil report
                if (Objects.nonNull(customerInfoModel.getCustomerId()) && reportPullType.equals(ReportPullType.GET_ONLY))
                    return new ServiceResponse<>(null , null , true);

                // New User Journey
                log.info("new Journey start for mobile Number {} ", mobileNumber);
                ServiceResponse<CibilParsing> cibilParsingServiceResponse = cibilReportClientComponent.newUserJourneyToCibil(userId, UserType.USER.getUserType(), mobileNumber, panCardNumber, tncConsent, fcChannel, customerInfoModel);
                log.info("new Journey End for mobile Number {} ", mobileNumber);
                return cibilParsingServiceResponse;
            }
            //  return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.NO_RECORD_FOUND), false);

        } catch (Exception e) {
            log.info("Exception while fetching cibil report");
            e.printStackTrace();
        }
        return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.VCC_CIBIL_JOURNEY_EXCEPTION), true);
    }


    private ServiceResponse<CibilParsing> refetchReportBasedOnPullType(CibilParsing cibilParsing, CustomerInfoModel customerInfoModel, boolean tncConsent, String fcChannel, ReportPullType reportPullType) {
        try {
            log.info("fetchCibilReportBasedOnType | fetch the report for old user");

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -5);
            Date startDate = calendar.getTime();

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

            Date reportUpdatedDate = customerInfoModel.getCustomerCibilInfoModel().getReportUpdatedAt();
            log.info("startDate: {}, reportUpdateDate {}", startDate, reportUpdatedDate);

            if (startDate.compareTo(reportUpdatedDate) <= 0 && reportPullType.equals(ReportPullType.SOFT_REFRESH)) {
                log.info("Old Cibil report return");
            } else {
                log.info("Report 5 months old. Refresh call for updated report");
                if (!cibilTnCComponent.validateTnCConsentStatus(customerInfoModel.getUserId(), UserType.USER.getUserType(), tncConsent, BureauType.CIBIL.getBureauType())) {
                    log.info("User TnC is expired");
                    return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.NOT_VALID_TNC_STATUS), false);
                }

                cibilParsing = cibilReportClientComponent.fetchDataFromTuV2(customerInfoModel, fcChannel, cibilParsing, true);
            }
            return new ServiceResponse<>(cibilParsing, true);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while fetching 5 months older report");
        }

        return new ServiceResponse<>(null, null, false);
    }


    private CibilParsing insertReportIntoDynamodb(@NonNull final CustomerInfoModel customerInfoModel) {

        // If report is present in cibilDB but not in dynamoDB
        log.info("fetchCibilReport | cibilReport present in cibilDB but not in dyanmoDB");
        if (Objects.nonNull(customerInfoModel.getCustomerCibilInfoModel().getCibilReport())) {
            CibilParsing cibilParsing = cibilParsingService.addDetails(customerInfoModel);
            return cibilParsing;
        } else {
            throw new FCCException(ErrorCodeAndMessage.BAD_DYNAMODB_INSERTION_REQUEST);
        }
    }


    private AddCustomerAssetSuccessRequest createAddCustomerAssetRequest(@NonNull CustomerInfoModel customerInfoModel) {
        if (Objects.nonNull(customerInfoModel.getCustomerCibilInfoModel().getCibilReport())) {
            CustomerAssetSuccess customerAssetSuccess = JsonUtil.convertStringIntoObject(customerInfoModel.getCustomerCibilInfoModel().getCibilReport(), CustomerAssetSuccess.class);
            AddCustomerAssetSuccessRequest request = AddCustomerAssetSuccessRequest.builder()
                    .mobileNumber(customerInfoModel.getMobileNumber())
                    .userId(customerInfoModel.getUserId())
                    .customerId(customerInfoModel.getCustomerId())
                    .customerAssetSuccess(customerAssetSuccess)
                    .build();
            log.info("AddCustomerAssetSuccessRequest : {}", request);
            return request;
        } else {
            log.info("cibilReport is not present in DB");
        }

        return null;
    }

    public CibilParsing cibilReportComponent(DynamodbDataInsertionRequest request) {

        try {
            CustomerInfoModel customerInfoModel = customerInfoMappingAccessor.getRecordWithCibilInfo(request.getMobileNumber());
            if (Objects.nonNull(customerInfoModel.getCustomerId())) {
                log.info("Customer Id : {} for user with mobileNumber : {}", customerInfoModel.getCustomerId(), request.getMobileNumber());
                AddCustomerAssetSuccessRequest addRequest = createAddCustomerAssetRequest(customerInfoModel);
                CibilParsing cibilParsing = cibilParsingService.addCibilReportDetails(addRequest);
                log.info("Data successfully saved in Dynamo db for mobileNumber : {}", customerInfoModel.getMobileNumber());
                return cibilParsing;
            }
        } catch (Exception e) {
            log.info("Exception while saving details into Dynamo db ");
            e.printStackTrace();
        }
        return new CibilParsing();
    }
}