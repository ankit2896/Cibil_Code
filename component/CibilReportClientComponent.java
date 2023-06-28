package com.freecharge.cibil.component;

import com.freecharge.cibil.constants.FcCibilConstants;
import com.freecharge.cibil.exceptions.FCCException;
import com.freecharge.cibil.exceptions.FCInternalClientException;
import com.freecharge.cibil.model.CustomerCibilInfoModel;
import com.freecharge.cibil.model.enums.*;
import com.freecharge.cibil.model.pojo.CustomerTUInfo;
import com.freecharge.cibil.model.pojo.IdentificationInformation;
import com.freecharge.cibil.model.request.*;
import com.freecharge.cibil.model.response.*;
import com.freecharge.cibil.mysql.accessor.CibilInfoAccessor;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.entity.CustomerCibilInfo;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.mysql.mapper.CibilInfoMapper;
import com.freecharge.cibil.mysql.mapper.CustomerInfoMappingMapper;
import com.freecharge.cibil.mysql.model.CreateFulfilmentVCCResponse;
import com.freecharge.cibil.mysql.model.CustomerInfoModel;
import com.freecharge.cibil.mysql.repository.impl.CustomerInfoRepository;
import com.freecharge.cibil.provider.CustomerIdentifierProvider;
import com.freecharge.cibil.rest.CibilParsingService;
import com.freecharge.cibil.rest.TransunionService;
import com.freecharge.cibil.rest.dto.response.NameInformation;
import com.freecharge.cibil.utility.ImsUserUtils;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.enums.BureauType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.freecharge.cibil.constants.HeaderConstants.CLIENT_KEY;
import static com.freecharge.cibil.constants.HeaderConstants.PURPOSE_KEY;
import static com.freecharge.cibil.model.enums.ErrorCodeAndMessage.ALTERNATE_AUTH_EXCEPTION;

@Slf4j
@Component
public class CibilReportClientComponent {

    private final CustomerInfoRepository customerInfoRepository;
    private final CibilParsingService cibilParsingService;
    private final CibilComponent cibilSyncComponent;
    private final CustomerIdentifierProvider customerIdentifierProvider;
    private final EligibilityComponent eligibilityComponent;
    private final CustomerInfoMappingAccessor customerInfoMappingAccessor;
    private final TransunionDataComponent transunionDataComponent;
    private final ImsUserUtils imsUserUtils;
    private final CibilTnCComponent cibilTnCComponent;

    @Autowired
    @Qualifier("transunionService")
    private TransunionService transunionService;

    @Autowired
    private CibilInfoAccessor cibilInfoAccessor;

    public CibilReportClientComponent(@NonNull final CustomerInfoRepository customerInfoRepository,
                                      @NonNull final CibilParsingService cibilParsingService,
                                      @NonNull final CibilComponent cibilSyncComponent,
                                      @NonNull final CustomerIdentifierProvider customerIdentifierProvider,
                                      @NonNull final EligibilityComponent eligibilityComponent,
                                      @NonNull final CustomerInfoMappingAccessor customerInfoMappingAccessor,
                                      @NonNull final TransunionDataComponent transunionDataComponent,
                                      @NonNull final ImsUserUtils imsUserUtils,
                                      @NonNull final CibilTnCComponent cibilTnCComponent) {
        this.customerInfoRepository = customerInfoRepository;
        this.cibilParsingService = cibilParsingService;
        this.cibilSyncComponent = cibilSyncComponent;
        this.customerIdentifierProvider = customerIdentifierProvider;
        this.eligibilityComponent = eligibilityComponent;
        this.customerInfoMappingAccessor = customerInfoMappingAccessor;
        this.transunionDataComponent = transunionDataComponent;
        this.imsUserUtils = imsUserUtils;
        this.cibilTnCComponent = cibilTnCComponent;
    }

    public ServiceResponse<CibilParsing> newUserJourneyToCibil(String userId, String userType, String mobileNumber, String panCardNumber, Boolean TncConsent, String fcChannel, CustomerInfoModel customerInfoModel) {
        if (!cibilTnCComponent.validateTnCConsentStatus(userId, userType, TncConsent, BureauType.CIBIL.getBureauType())) {
            return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.TNC_EXPIRED_EXCEPTION), true);
        }
        ServiceResponse<CibilParsing> serviceResponse = userCibilJourney(userId, userType, mobileNumber, panCardNumber, fcChannel, customerInfoModel);

        if (Objects.isNull(serviceResponse.getData()))
            return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.VCC_CIBIL_JOURNEY_EXCEPTION), true);

        return serviceResponse;
    }


    private ServiceResponse<CibilParsing> userCibilJourney(String userId, String userType, String mobileNumber, String panCardNumber, String fcChannel, CustomerInfoModel customerInfoModel) {
        //Fulfilment
        CreateFulfilmentVCCResponse createFulfillmentResponse = initiateFulfillOffer(userId, userType, mobileNumber, panCardNumber, customerInfoModel);
        CustomerInfoModel customerInfoUpdatedModel = createFulfillmentResponse.getCustomerInfoModel();
        String customerId = customerInfoUpdatedModel.getCustomerId();
        customerInfoUpdatedModel.getCustomerCibilInfoModel().setCustomerId(customerId);

        FulfillmentStatus fulfillmentStatus = createFulfillmentResponse.getFulfillmentStatus();
        if (fulfillmentStatus.equals(FulfillmentStatus.IN_PROGRESS) || fulfillmentStatus.equals(FulfillmentStatus.PENDING)) {
            // Authentication
            final AuthenticationQuestionResponse authenticationQuestionResponse = getAuthenticationQuestion(customerInfoUpdatedModel, null, null);

            if (!authenticationQuestionResponse.getQueueName().equals(QueueName.OTP_BYPASS_QUEUE)) {
                return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.OTP_BYPASS_EXCEPTION), true);
            } else {
                fulfillmentStatus = FulfillmentStatus.SUCCESS;
                customerInfoUpdatedModel.getCustomerCibilInfoModel().setFulfillmentResponse(fulfillmentStatus);
            }
        }
        if (fulfillmentStatus.equals(FulfillmentStatus.SUCCESS)) {
            //TU
            CibilParsing cibilParsing = fetchDataFromTuV2(customerInfoUpdatedModel, fcChannel, null, false);
            return new ServiceResponse<>(cibilParsing, true);
        } else {
            log.info("fulfillment status not success");
            return new ServiceResponse<>(null, new FCCException(ErrorCodeAndMessage.SUCCESSFUL_FULFILLMENT_COULD_NOT_BE_CREATED), true);
        }
    }

    public AuthenticationQuestionResponse getAuthenticationQuestion(CustomerInfoModel customerInfoModel, final String fcChannel, final String fcVersion) {
        final GetAuthenticationQuestionRequest request = GetAuthenticationQuestionRequest.builder().build();
        request.setCustomerIdentifier(customerInfoModel.getPccId());
        request.setUserId(customerInfoModel.getUserId());
        AuthenticationQuestionResponse response = transunionService.getAuthenticationQuestions(customerInfoModel.getMobileNumber(), request);

        if (response.getQueueName().equals(QueueName.OTP_BYPASS_QUEUE)) {
            cibilInfoAccessor.updateRecordWhenVerification(customerInfoModel, FulfillmentStatus.SUCCESS);
        }

        if (Objects.nonNull(fcChannel) && Objects.nonNull(fcVersion)) {
            if (response.getQueueName().equals(QueueName.OTP_ALTERNATE_EMAIL_ENTRY_QUEUE) && fcChannel.equals(ChannelCode.ANDROID_USER.getValue()) && Integer.parseInt(fcVersion) < FcCibilConstants.ALTERNATE_FLOW_ANDROID_VERSION) {
                log.info("GetAuthenticationQuestion | Queue fetched as OTP_ALTERNATE_EMAIL_ENTRY from TU for userId {} and mobileNumber {}", request.getUserId(), customerInfoModel.getMobileNumber());
                throw new FCInternalClientException(ALTERNATE_AUTH_EXCEPTION);
            }
        }
        return response;
    }


    public CibilParsing fetchDataFromTuV2(@NonNull CustomerInfoModel customerInfoModel, @NonNull String fcChannel, CibilParsing cibilParsing, @NonNull Boolean isFulfillmentRequired) {
        TransUnionDataFetchRequest transUnionDataFetchRequest = TransUnionDataFetchRequest.builder()
                .customerIdentifier(customerInfoModel.getPccId())
                .mobileNumber(customerInfoModel.getMobileNumber())
                .userId(customerInfoModel.getUserId())
                .build();

        log.info("fetchCibilReport | TransUnionDataFetchRequest request : {}", transUnionDataFetchRequest);
        return transunionDataComponent.getTransUnionDataFetchResponseV2Optimization(transUnionDataFetchRequest, customerInfoModel, customerInfoModel.getUserType(), cibilParsing, fcChannel, isFulfillmentRequired);
    }

    private CreateFulfilmentVCCResponse initiateFulfillOffer(@NonNull String userId, @NonNull String userType, @NonNull String mobileNumber, @NonNull String panCardNumber, CustomerInfoModel customerInfoModel) {
        CreateFulfillOfferRequest createFulfillOfferRequest = createRequestForFulfillment(panCardNumber);
        if (Objects.nonNull(customerInfoModel.getCustomerId())) {
            customerInfoModel = customerIdentifierProvider.createCustomerPccIdOptimize(userId, userType, mobileNumber, panCardNumber, createFulfillOfferRequest.getEmail(), customerInfoModel);
        } else {
            customerInfoModel = CustomerInfoModel.builder()
                    .userId(userId)
                    .userType(UserType.USER.getUserType())
                    .pancardNumber(panCardNumber)
                    .email(createFulfillOfferRequest.getEmail())
                    .mobileNumber(mobileNumber)
                    .build();

            customerInfoModel = customerIdentifierProvider.createCustomerPccIdOptimize(userId, userType, mobileNumber, panCardNumber, createFulfillOfferRequest.getEmail(), customerInfoModel);

        }
        createFulfillOfferRequest.setCustomerIdentifier(customerInfoModel.getPccId());
        createFulfillOfferRequest.setUserId(userId);

        final CustomerTUInfo customerTUInfo = CustomerTUInfo.builder().build();
        customerTUInfo.setUserConsentForDataSharing(true);
        customerTUInfo.setLegalCopyStatus(true);
        customerTUInfo.setMobileNumber(mobileNumber);
        customerTUInfo.setEmail(createFulfillOfferRequest.getEmail());
        customerTUInfo.setIdentificationInformation(createFulfillOfferRequest.getIdentificationInformation());
        customerTUInfo.setDateOfBirth(createFulfillOfferRequest.getDateOfBirth());


        if (Objects.nonNull(createFulfillOfferRequest.getIdentificationInformation())) {
            final NameInformation nameInformation = cibilSyncComponent.getNameDetailsFromKyc(createFulfillOfferRequest
                    .getIdentificationInformation(), userType);

            customerTUInfo.setFirstName(nameInformation.getFirstName());

            customerTUInfo.setLastName(StringUtils.isBlank(nameInformation.getLastName()) ?
                    nameInformation.getFirstName() : nameInformation.getLastName());
        }

        final CreateFulfillmentRequest fulfillmentRequest = CreateFulfillmentRequest.builder().build();
        fulfillmentRequest.setUserId(createFulfillOfferRequest.getUserId());
        fulfillmentRequest.setCustomerIdentifier(createFulfillOfferRequest.getCustomerIdentifier());
        fulfillmentRequest.setCustomerInfo(customerTUInfo);

        CreateFulfilmentVCCResponse createFulfilmentVCCResponse = processFulfillment(fulfillmentRequest, CLIENT_KEY, PURPOSE_KEY, customerInfoModel);
        return createFulfilmentVCCResponse;
    }

    public CreateFulfilmentVCCResponse processFulfillment(@NonNull final CreateFulfillmentRequest createFulfillmentRequest,
                                                          @NonNull final String clientId, @NonNull final String purpose,
                                                          @NonNull final CustomerInfoModel customerInfoModel) {
        log.info("ProcessFulFillment : Started for userId {} and mobileNumber {} with request {}", createFulfillmentRequest.getUserId(), createFulfillmentRequest.getCustomerInfo().getMobileNumber(), JsonUtil.writeValueAsString(createFulfillmentRequest));
        final CreateFulfillmentResponse response = transunionService.createFulfillment(createFulfillmentRequest);
        CustomerInfo customerInfo = CustomerInfoMappingMapper.convertCibilModelToEntity(customerInfoModel);
        String pccId = customerInfoModel.getPccId();
        if (Objects.isNull(customerInfo.getCibilInfo().getCustomerId())) {
            log.info("Created CustomerCibilInfo");
            CustomerCibilInfo customerCibilInfo = CustomerCibilInfo.builder()
                    .clientId(clientId)
                    .customerInfo(customerInfo)
                    .purpose(purpose)
                    .txnId(MDC.get("requestId"))
                    .fulfillmentRequest(JsonUtil.writeValueAsString(createFulfillmentRequest))
                    .fulfillmentResponse(response.getFulfillmentStatus())
                    .transactionStatus(TransactionStatus.PENDING)
                    .build();
            customerInfo.setCibilInfo(customerCibilInfo);
            cibilInfoAccessor.saveFulfillmentInCustomerCibilInfo(customerInfo);
        } else {
            log.info("Updated CustomerCibilInfo");
            CustomerCibilInfo info = customerInfo.getCibilInfo();
            CustomerCibilInfoModel cibilInfoModel = CustomerCibilInfoModel.builder()
                    .customerId(info.getCustomerId())
                    .fulfillmentRequest(JsonUtil.writeValueAsString(createFulfillmentRequest))
                    .fulfillmentResponse(response.getFulfillmentStatus())
                    .clientId(info.getClientId())
                    .purpose(info.getPurpose())
                    .txnId(info.getTxnId())
                    .cibilReport(info.getCibilReport())
                    .riskScore(info.getRiskScore())
                    .reportUpdatedAt(info.getReportUpdatedAt())
                    .transactionStatus(info.getTransactionStatus())
                    .createdAt(info.getCreatedAt())
                    .updatedAt(info.getUpdatedAt()).build();
            cibilInfoAccessor.updateRecordWithRequestAndResponseN(cibilInfoModel);
            customerInfo.setCibilInfo(CibilInfoMapper.convertModelToEntity(cibilInfoModel));
        }
        CustomerInfoModel updatedModel = CustomerInfoMappingMapper.convertEntityToModelCibil(customerInfo);
        updatedModel.setPccId(pccId);
        return CreateFulfilmentVCCResponse.builder().fulfillmentStatus(response.getFulfillmentStatus()).customerInfoModel(updatedModel).build();
    }

    private CreateFulfillOfferRequest createRequestForFulfillment(String panCardNumber) {
        IdentificationInformation identificationInformation = IdentificationInformation.builder()
                .identificationNumber(panCardNumber)
                .type(IdentificationType.TAX_ID)
                .build();

        CreateFulfillOfferRequest request = CreateFulfillOfferRequest.builder()
                .identificationInformation(identificationInformation)
                .termsAndConditionsStatus(TermsAndConditionsStatus.ACCEPTED)
                .build();
        return request;
    }


}
