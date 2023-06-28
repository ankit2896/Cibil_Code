package com.freecharge.cibil.component;

import com.freecharge.cibil.exceptions.FCCException;
import com.freecharge.cibil.exceptions.FCInternalClientException;
import com.freecharge.cibil.factory.KycInfoFactory;
import com.freecharge.cibil.model.enums.*;
import com.freecharge.cibil.model.response.*;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.mysql.mapper.CustomerInfoMappingMapper;
import com.freecharge.cibil.mysql.model.CustomerInfoModel;
import com.freecharge.cibil.mysql.repository.impl.CustomerCibilInfoRepository;
import com.freecharge.cibil.mysql.repository.impl.CustomerInfoRepository;
import com.freecharge.cibil.provider.CustomerIdentifierProvider;
import com.freecharge.cibil.rest.ExperianParsingService;
import com.freecharge.cibil.rest.ExperianService;
import com.freecharge.cibil.rest.TNCService;
import com.freecharge.cibil.rest.dto.response.NameInformation;
import com.freecharge.cibil.utility.ImsUserUtils;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.enums.BureauType;
import com.freecharge.experian.model.request.EnhancedMatchRequest;
import com.freecharge.experian.model.response.*;
import com.freecharge.experian.utils.ExperianDTOMapper;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class ExperianReportParsingComponent {

    private final CustomerInfoRepository customerInfoRepository;

    private final CustomerCibilInfoRepository customerCibilInfoRepository;

    private final ExperianParsingService experianParsingService;

    private final CibilComponent cibilSyncComponent;

    private final CustomerIdentifierProvider customerIdentifierProvider;

    private final EligibilityComponent eligibilityComponent;

    private final ExperianDataComponent experianDataComponent;

    private final ExperianService experianService;

    private final ExperianReportClientComponent experianReportClientComponent;

    private final TNCService tncService;

    private final ImsUserUtils imsUserUtils;

    private final CustomerInfoMappingAccessor customerInfoMappingAccessor;

    private final KycInfoFactory kycInfoFactory;

    private final CibilTnCComponent cibilTnCComponent;

    @Setter
    @Value("${tnc.ims.policyId}")
    private int policyId;

    @Setter
    @Value("${tnc.merchant.policyId}")
    private int merchantPolicyId;

    public ExperianReportParsingComponent(@NonNull final CustomerInfoRepository customerInfoRepository,
                                          @NonNull final CustomerCibilInfoRepository customerCibilInfoRepository,
                                          @NonNull final ExperianParsingService experianParsingService,
                                          @NonNull final CibilComponent cibilSyncComponent,
                                          @NonNull final CustomerIdentifierProvider customerIdentifierProvider,
                                          @NonNull final EligibilityComponent eligibilityComponent,
                                          @NonNull final ExperianReportClientComponent experianReportClientComponent, @NonNull final CustomerInfoMappingAccessor customerInfoMappingAccessor,
                                          @NonNull final TransunionDataComponent transunionDataComponent,
                                          @NonNull final ExperianDataComponent experianDataComponent,
                                          @NonNull final ExperianService experianService,
                                          @NonNull final TNCService tncService,
                                          @NonNull final ImsUserUtils imsUserUtils,
                                          @NonNull final KycInfoFactory kycInfoFactory, @NonNull final CibilTnCComponent cibilTnCComponent) {
        this.customerInfoRepository = customerInfoRepository;
        this.customerCibilInfoRepository = customerCibilInfoRepository;
        this.experianParsingService = experianParsingService;
        this.cibilSyncComponent = cibilSyncComponent;
        this.customerIdentifierProvider = customerIdentifierProvider;
        this.eligibilityComponent = eligibilityComponent;
        this.experianReportClientComponent = experianReportClientComponent;
        this.experianDataComponent = experianDataComponent;
        this.experianService = experianService;
        this.tncService = tncService;
        this.imsUserUtils = imsUserUtils;
        this.customerInfoMappingAccessor = customerInfoMappingAccessor;
        this.kycInfoFactory = kycInfoFactory;
        this.cibilTnCComponent = cibilTnCComponent;
    }


    public ServiceResponse<CreditParseReport> fetchExperianReport(String userId, String panCardNumber, String mobileNumber, boolean tncConsent, String fcChannel, ReportPullType reportPullType) {
        try {
            log.info("panCardNumber: {} mobileNumber: {} tncConsent: {} fcChannel: {}", panCardNumber, mobileNumber, tncConsent, fcChannel);

            if (!fcChannel.equals(ChannelCode.VCC.getValue())) {
                return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.INCORRECT_VCC_CHANNEL_CODE), false);
            }

            if (!tncConsent) {
                return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.NOT_VALID_TNC_STATUS), false);
            }

            CustomerInfoModel customerInfoModel = customerInfoMappingAccessor.getRecordWithExperianInfo(mobileNumber);

            if (Objects.isNull(customerInfoModel.getCustomerId()) && reportPullType.equals(ReportPullType.GET_ONLY)) {
                return new ServiceResponse<>(new CreditParseReport(), null, true);
            }

            // Old User
            if (Objects.nonNull(customerInfoModel.getCustomerId()) && Objects.nonNull(customerInfoModel.getCustomerExperianInfoModel().getExperianReport())) {
                CreditParseReport creditParseReport = experianParsingService.getDetails(customerInfoModel.getCustomerId(),customerInfoModel.getMobileNumber());

                if (Objects.isNull(creditParseReport)) {
                    // if data present in sqlDB not in dynamoDB
                    creditParseReport = insertReportIntoDynamodb(customerInfoModel);
                }

                if (reportPullType.equals(ReportPullType.GET_ONLY)) {
                    return new ServiceResponse<>(creditParseReport, null, true);
                } else {
                    if (Objects.nonNull(customerInfoModel.getCustomerExperianInfoModel().getExperianReport())) {
                        ServiceResponse<CreditParseReport> response = refetchReportBasedOnPullType(creditParseReport, customerInfoModel, tncConsent, fcChannel, reportPullType);
                        if (!response.isSuccess()) {
                            return new ServiceResponse<>(creditParseReport, true);
                        }
                        return response;
                    }
                }
            } else {
                // Check for user with null cibil report
                if (Objects.nonNull(customerInfoModel.getCustomerId()) && reportPullType.equals(ReportPullType.GET_ONLY))
                    return new ServiceResponse<>(null , null , true);
                // New User Journey
                log.info("new Journey start for mobile Number {} ", mobileNumber);
                ServiceResponse<CreditParseReport> creditParsingServiceResponse = newUserJourneyToExperian(userId, UserType.USER.getUserType(), mobileNumber, panCardNumber, tncConsent, fcChannel, customerInfoModel);
                log.info("new Journey End for mobile Number {} ", mobileNumber);
                return creditParsingServiceResponse;
            }
        } catch (Exception e) {
            log.info("Exception while fetching experian report");
            e.printStackTrace();
        }
        return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.VCC_EXPERIAN_JOURNEY_EXCEPTION), true);
    }


    private ServiceResponse<CreditParseReport> refetchReportBasedOnPullType(CreditParseReport creditParseReport, CustomerInfoModel customerInfoModel, boolean tncConsent, String fcChannel, ReportPullType reportPullType) {
        try {
            log.info("fetchCibilReportBasedOnType | fetch the report for old user");

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -5);
            Date startDate = calendar.getTime();

            Date reportUpdatedDate = customerInfoModel.getCustomerExperianInfoModel().getReportUpdatedAt();
            log.info("startDate: {}, reportUpdateDate {}", startDate, reportUpdatedDate);

            if (startDate.compareTo(reportUpdatedDate) <= 0 && reportPullType.equals(ReportPullType.SOFT_REFRESH)) {
                log.info("Old Cibil report return");
            } else {
                log.info("Report 5 months old. Refresh call for updated report");
                if (!cibilTnCComponent.validateTnCConsentStatus(customerInfoModel.getUserId(), UserType.USER.getUserType(), tncConsent, BureauType.EXPERIAN.getBureauType())) {
                    log.info("User TnC is expired");
                    return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.NOT_VALID_TNC_STATUS), false);
                }
                experianService.experianOnDemandAction(customerInfoModel.getUserId(), UserType.USER.getUserType(), customerInfoModel.getMobileNumber());
                creditParseReport = experianParsingService.getDetails(customerInfoModel.getCustomerId(),customerInfoModel.getMobileNumber());
            }
            return new ServiceResponse<>(creditParseReport, true);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception while fetching 5 months older report");
        }
        return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.FIVE_MONTHS_OLDER_REPORT), false);
    }


    public ServiceResponse<CreditParseReport> newUserJourneyToExperian(String userId, String userType, String mobileNumber, String panCardNumber, Boolean tncConsent, String fcChannel, CustomerInfoModel customerInfoModel) {
        if (!cibilTnCComponent.validateTnCConsentStatus(userId, userType, tncConsent,BureauType.EXPERIAN.getBureauType())) {
            return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.TNC_EXPIRED_EXCEPTION), true);
        }
        NameInformation nameInformation = kycInfoFactory.getNameInformation(panCardNumber, IdentificationType.TAX_ID.getValue(), userType);
        CreateEnhanceMatchRequest createEnhanceMatchRequest = CreateEnhanceMatchRequest.builder().mobileNumber(mobileNumber).pancardNumber(panCardNumber).userId(userId).firstName(nameInformation.getFirstName()).surName(nameInformation.getLastName()).build();
        CreditParseReport creditParseReport = experianReportClientComponent.newUserClientJourney(userId, UserType.USER.getUserType(), createEnhanceMatchRequest,customerInfoModel);
        //CreditParseReport creditParseReport = experianParsingService.getDetails(customerInfoModel.getCustomerId(),customerInfoModel.getMobileNumber());
        ServiceResponse serviceResponse = new ServiceResponse<>(creditParseReport, true);
        if (Objects.isNull(serviceResponse.getData()))
            return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.VCC_EXPERIAN_JOURNEY_EXCEPTION), true);

        return serviceResponse;
    }

    private CreditParseReport insertReportIntoDynamodb(@NonNull final CustomerInfoModel customerInfoModel) {

        // If report is present in cibilDB but not in dynamoDB
        log.info("fetchCibilReport | cibilReport present in cibilDB but not in dyanmoDB");
        if (Objects.nonNull(customerInfoModel.getCustomerExperianInfoModel().getExperianReport())) {
            CreditParseReport creditParseReport = experianParsingService.addExperianDetails(customerInfoModel);
            return creditParseReport;
        } else {
            throw new FCCException(ErrorCodeAndMessage.BAD_DYNAMODB_INSERTION_REQUEST);
        }
    }

}