package com.freecharge.cibil.component;

import com.freecharge.cibil.constants.FcCibilConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.freecharge.cibil.exceptions.FCBadRequestException;
import com.freecharge.cibil.exceptions.FCInternalClientException;
import com.freecharge.cibil.exceptions.FCInternalServerException;
import com.freecharge.cibil.factory.KycInfoFactory;
import com.freecharge.cibil.model.CustomerCibilInfoModel;
import com.freecharge.cibil.model.enums.*;
import com.freecharge.cibil.model.pojo.CustomerTUInfo;
import com.freecharge.cibil.model.pojo.CustomerTUInfoExpressSearch;
import com.freecharge.cibil.model.pojo.IdentificationInformation;
import com.freecharge.cibil.model.request.*;
import com.freecharge.cibil.model.response.*;
import com.freecharge.cibil.mysql.accessor.CibilInfoAccessor;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.entity.CustomerCibilInfo;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.provider.CustomerIdentifierProvider;
import com.freecharge.cibil.rest.TNCService;
import com.freecharge.cibil.rest.TransunionService;
import com.freecharge.cibil.rest.dto.request.TermsAndConditionAcceptanceRequest;
import com.freecharge.cibil.rest.dto.response.NameInformation;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.enums.BureauType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.freecharge.cibil.model.enums.ErrorCodeAndMessage.*;



@Slf4j
@Component
public class CibilComponent {

    @Autowired
    @Qualifier("transunionService")
    private TransunionService transunionService;

    private final CibilInfoAccessor cibilInfoAccessor;

    private final CustomerInfoMappingAccessor customerInfoMappingAccessor;

    private final TNCService tncService;

    private final TransunionDataComponent transunionDataComponent;

    private final KycInfoFactory kycInfoFactory;


    @Autowired
    public CibilComponent(@NonNull final CibilInfoAccessor cibilInfoAccessor,
                          @NonNull final CustomerInfoMappingAccessor customerInfoMappingAccessor,
                          @NonNull final TNCService tncService,
                          @NonNull final TransunionDataComponent transunionDataComponent, @NonNull final KycInfoFactory kycInfoFactory) {
        this.cibilInfoAccessor = cibilInfoAccessor;
        this.customerInfoMappingAccessor = customerInfoMappingAccessor;
        this.tncService = tncService;
        this.transunionDataComponent = transunionDataComponent;
        this.kycInfoFactory = kycInfoFactory;
    }

    public AuthenticationQuestionResponse getAuthenticationQuestion(@NonNull final String userId, @NonNull final String mobileNumber, @NonNull final CustomerIdentifierProvider customerIdentifierProvider, final String fcChannel, final String fcVersion) {
        final String customerIdentifier = customerIdentifierProvider.getCustomerPccId(userId, mobileNumber);
        final GetAuthenticationQuestionRequest request = GetAuthenticationQuestionRequest.builder().build();
        request.setCustomerIdentifier(customerIdentifier);
        request.setUserId(userId);
        log.info("GetAuthenticationQuestion : Start with request {} for userId {} and mobileNumber {}", JsonUtil.writeValueAsString(request), userId, mobileNumber);
        AuthenticationQuestionResponse response = transunionService.getAuthenticationQuestions(mobileNumber, request);

        if(response.getQueueName().equals(QueueName.OTP_BYPASS_QUEUE)){
            cibilInfoAccessor.updateRecordWhenVerification(request.getUserId(), mobileNumber, FulfillmentStatus.SUCCESS);
        }

        if(Objects.nonNull(fcChannel) && Objects.nonNull(fcVersion)) {
            if (response.getQueueName().equals(QueueName.OTP_ALTERNATE_EMAIL_ENTRY_QUEUE) && fcChannel.equals(ChannelCode.ANDROID_USER.getValue()) && Integer.parseInt(fcVersion) < FcCibilConstants.ALTERNATE_FLOW_ANDROID_VERSION) {
                log.info("GetAuthenticationQuestion | Queue fetched as OTP_ALTERNATE_EMAIL_ENTRY from TU for userId {} and mobileNumber {}", request.getUserId(), mobileNumber);
                throw new FCInternalClientException(ALTERNATE_AUTH_EXCEPTION);
            }
        }
        return response;
    }

    public VerifyAuthenticationAnswersResponse verifyAuthenticationAnswers(@NonNull final String userId, @NonNull final String mobileNumber, @NonNull final CustomerIdentifierProvider customerIdentifierProvider,
                                                                           @NonNull final VerifyAuthenticationAnswersRequest verifyAuthenticationAnswersRequest) {
        final String customerIdentifier = customerIdentifierProvider.getCustomerPccId(userId, mobileNumber);
        verifyAuthenticationAnswersRequest.setCustomerIdentifier(customerIdentifier);
        verifyAuthenticationAnswersRequest.setUserId(userId);
        log.info("VerifyAuthenticationAnswers : Start with request {} for userId {} and mobileNumber {}", JsonUtil.writeValueAsString(verifyAuthenticationAnswersRequest), userId, mobileNumber);
        final VerifyAuthenticationAnswersResponse response = transunionService.verifyAuthenticationAnswers(mobileNumber, verifyAuthenticationAnswersRequest);
        cibilInfoAccessor.updateRecordWhenVerification(verifyAuthenticationAnswersRequest.getUserId(), mobileNumber, getFulfillmentStatus(response.getVerificationStatus()));
        return response;
    }

    public ReportUrlResponse getReportUrl(@NonNull final String userId, @NonNull final String mobileNumber,
                                          @NonNull final CustomerIdentifierProvider customerIdentifierProvider) {

        final String customerIdentifier = customerIdentifierProvider.getCustomerPccId(userId, mobileNumber);
        final GetReportUrlRequest reportUrlRequest = GetReportUrlRequest
                .builder().build();
        reportUrlRequest.setCustomerIdentifier(customerIdentifier);
        reportUrlRequest.setUserId(userId);
        log.info("GetReportUrl API Start with request {} for userId {} and mobileNumber {}", JsonUtil.writeValueAsString(reportUrlRequest), userId, mobileNumber);
        return transunionService.getCustomerReportUrl(mobileNumber, reportUrlRequest);
    }

    public ReportUrlResponseV2 getReportUrlV2(@NonNull final String userId, @NonNull final String mobileNumber,
                                              @NonNull final CustomerIdentifierProvider customerIdentifierProvider) {

        final ReportUrlResponseV2 responseV2 = ReportUrlResponseV2.builder().build();
        final CustomerCibilInfoModel customerCibilInfoModel = transunionDataComponent.getLatestCibilRecordFromDataBase(mobileNumber);

        log.info("GetReportUrlV2 | Latest Record in DB for userId {} and mobileNumber {} fetched {}", userId, mobileNumber, customerCibilInfoModel);
        final CustomerCibilInfoModel updatedModel = transunionDataComponent.createFulfillmentIfForReverification(userId, customerCibilInfoModel, "", "", "");
        log.info("GetReportUrlV2 | Updated info model for userId {} and mobileNumber {} is {}", userId, mobileNumber, updatedModel);
        responseV2.setFulfillOfferStatus(transunionDataComponent.getFulfillOfferStatus(updatedModel.getFulfillmentResponse()));
        log.info("GetReportUrlV2 | Fulfillment Status is {} for userId {} and mobileNumber {} for fetchDataFromTransunion",
                updatedModel.getFulfillmentResponse(), userId, mobileNumber);

        if (updatedModel.getFulfillmentResponse().equals(FulfillmentStatus.SUCCESS)) {
            final String customerIdentifier = customerIdentifierProvider.getCustomerPccId(userId, mobileNumber);
            final GetReportUrlRequest reportUrlRequest = GetReportUrlRequest
                    .builder().build();
            reportUrlRequest.setCustomerIdentifier(customerIdentifier);
            reportUrlRequest.setUserId(userId);
            log.info("GetReportUrl API Start with request {} for userId {} and mobileNumber {}", JsonUtil.writeValueAsString(reportUrlRequest), userId, mobileNumber);
            ReportUrlResponse response = transunionService.getCustomerReportUrl(mobileNumber, reportUrlRequest);
            responseV2.setReportUrl(response.getReportUrl());
        }
        return responseV2;
    }


    private FulfillmentStatus getFulfillmentStatus(@NonNull final VerificationStatus status) {
        switch (status) {
            case SUCCESS:
                return FulfillmentStatus.SUCCESS;
            case PENDING:
                return FulfillmentStatus.PENDING;
            case FAILURE:
                return FulfillmentStatus.FAILURE;
            case ABANDONED:
                return FulfillmentStatus.ABANDONED;
            case IN_PROGRESS:
                return FulfillmentStatus.IN_PROGRESS;
            default:
                throw new FCInternalServerException("Un-Recognised Verification Status");
        }
    }

    public CreateFulfillmentResponse transunionProcessInitiate(
            @NonNull final CreateFulfillOfferRequest createFulfillOfferRequest,
            @NonNull final String clientId, @NonNull final String purpose,
            @NonNull final String mobileNumber, @NonNull final String userId,
            @NonNull final String userType,
            @NonNull final CustomerIdentifierProvider customerIdentifierProvider) {

        final String customerIdentifier = customerIdentifierProvider.getCustomerPccId(userId, userType, mobileNumber,
                createFulfillOfferRequest.getIdentificationInformation().getIdentificationNumber(), createFulfillOfferRequest.getEmail());
        createFulfillOfferRequest.setCustomerIdentifier(customerIdentifier);
        createFulfillOfferRequest.setUserId(userId);
        log.info("TransunionProcessInitiate: userId {} and mobileNumber {} with request {}", createFulfillOfferRequest.getUserId(), mobileNumber,
                JsonUtil.writeValueAsString(createFulfillOfferRequest));
        final CustomerTUInfo customerTUInfo = CustomerTUInfo.builder().build();
        customerTUInfo.setUserConsentForDataSharing(true);
        customerTUInfo.setLegalCopyStatus(true);
        customerTUInfo.setMobileNumber(mobileNumber);
        customerTUInfo.setEmail(createFulfillOfferRequest.getEmail());
        customerTUInfo.setIdentificationInformation(createFulfillOfferRequest.getIdentificationInformation());
        customerTUInfo.setDateOfBirth(createFulfillOfferRequest.getDateOfBirth());


        if (Objects.nonNull(createFulfillOfferRequest.getIdentificationInformation())) {
            final NameInformation nameInformation = getNameDetailsFromKyc(createFulfillOfferRequest
                    .getIdentificationInformation(), userType);

            customerTUInfo.setFirstName(nameInformation.getFirstName());

            customerTUInfo.setLastName(StringUtils.isBlank(nameInformation.getLastName()) ?
                    nameInformation.getFirstName() : nameInformation.getLastName());

            log.info("First Name and Last Name for userId {} and MobileNumber {} is {},{}", userId, mobileNumber, customerTUInfo.getFirstName(), customerTUInfo.getLastName());
        }

        final CreateFulfillmentRequest fulfillmentRequest = CreateFulfillmentRequest.builder().build();
        fulfillmentRequest.setUserId(createFulfillOfferRequest.getUserId());
        fulfillmentRequest.setCustomerIdentifier(createFulfillOfferRequest.getCustomerIdentifier());
        fulfillmentRequest.setCustomerInfo(customerTUInfo);
        acceptTermsAndConditions(createFulfillOfferRequest.getTermsAndConditionsStatus().getValue(),
                createFulfillOfferRequest.getUserId(), userType, BureauType.CIBIL.getBureauType());

        // fetch the latest record from DB
        final CustomerInfo customerInfo = customerInfoMappingAccessor.getRecordForMobileNumber(mobileNumber).get();

        return processFulfillment(fulfillmentRequest, clientId, purpose, userType, customerInfo);
    }

    public void dateValidator(Date dateVal) {
        try {
            String pattern = "yyyy-MM-dd";
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            simpleDateFormat.setLenient(false);
            final String date = simpleDateFormat.format(dateVal);
            final String regex = "([1-9])\\d\\d\\d[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])";
            final Pattern ptrn = Pattern.compile(regex);
            final Matcher matcher = ptrn.matcher(date);
            if (!matcher.matches()) {
                throw new FCBadRequestException(INVALID_DATE);
            }
            simpleDateFormat.parse(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CreateFulfillmentResponse transunionProcessInitiateExpressSearch(@NonNull final CreateFulfillOfferRequestExpressSearch createFulfillOfferRequest,
                                                                            @NonNull final String clientId, @NonNull final String purpose,
                                                                            @NonNull final String mobileNumber, @NonNull final String userId,
                                                                            @NonNull final String userType, @NonNull final CustomerIdentifierProvider customerIdentifierProvider) {
        final String customerIdentifier = customerIdentifierProvider.getCustomerPccId(userId, userType, mobileNumber, createFulfillOfferRequest.getEmail());
        createFulfillOfferRequest.setCustomerIdentifier(customerIdentifier);
        createFulfillOfferRequest.setUserId(userId);
        log.info("transunionProcessInitiateExpressSearch:  userId {} with request {}", createFulfillOfferRequest.getUserId(),
                JsonUtil.writeValueAsString(createFulfillOfferRequest));

        final CustomerTUInfoExpressSearch customerTUInfo = CustomerTUInfoExpressSearch.builder().build();
        customerTUInfo.setUserConsentForDataSharing(true);
        customerTUInfo.setLegalCopyStatus(true);
        customerTUInfo.setMobileNumber(mobileNumber);
        customerTUInfo.setEmail(createFulfillOfferRequest.getEmail());

        customerTUInfo.setFirstName(createFulfillOfferRequest.getFirstName());
        customerTUInfo.setLastName(StringUtils.isBlank(createFulfillOfferRequest.getLastName()) ?
                createFulfillOfferRequest.getFirstName() : createFulfillOfferRequest.getLastName());

        final CreateFulfillmentRequestExpressSearch fulfillmentRequest = CreateFulfillmentRequestExpressSearch.builder().build();
        fulfillmentRequest.setUserId(createFulfillOfferRequest.getUserId());
        fulfillmentRequest.setCustomerIdentifier(createFulfillOfferRequest.getCustomerIdentifier());
        fulfillmentRequest.setCustomerInfo(customerTUInfo);
        acceptTermsAndConditions(createFulfillOfferRequest.getTermsAndConditionsStatus().getValue(),
                createFulfillOfferRequest.getUserId(), userType,BureauType.CIBIL.getBureauType());

        // If user fulfillment request already present in DB with InProgress/Pending response, redirect to the old request
        final CustomerInfo customerInfo = customerInfoMappingAccessor.getRecordForMobileNumber(mobileNumber).get();
        log.info("Fetching customerInfo details for userId {} and mobileNumber {} from DB", userId, mobileNumber);

        if (Objects.nonNull(customerInfo)) {
            CustomerCibilInfo info = customerInfo.getCibilInfo();
            if (Objects.nonNull(info) && (info.getFulfillmentResponse().equals(FulfillmentStatus.PENDING) || info.getFulfillmentResponse().equals(FulfillmentStatus.IN_PROGRESS))) {
                String fulfillmentRequestFromDB = info.getFulfillmentRequest();
                // Is fulfillmentRequest Long Auth.
                if (fulfillmentRequestFromDB.contains("identificationInformation")) {
                    CreateFulfillmentRequest fulfillmentRequestLong = JsonUtil.convertStringIntoObject(info.getFulfillmentRequest(), new TypeReference<CreateFulfillmentRequest>() {
                    });
                    log.info("FulfillmentRequest {} for mobileNumber {} redirect to Long Auth process", mobileNumber,fulfillmentRequestFromDB);
                    return processFulfillment(fulfillmentRequestLong, clientId, purpose, userType, customerInfo);
                }
            }
        }

        return processFulfillmentExpressSearch(fulfillmentRequest, clientId, purpose, userType, customerInfo);
    }

    public NameInformation getNameDetailsFromKyc(@NonNull final IdentificationInformation identificationInformation,
                                                  @NonNull final String userType) {
        return kycInfoFactory.getNameInformation(identificationInformation.getIdentificationNumber(),
                identificationInformation.getType().getValue(), userType);
    }

    public void acceptTermsAndConditions(@NonNull final String tncStatus,
                                          @NonNull final String userId,
                                          @NonNull final String userType,
                                          @NonNull final String bureauType) {
        if (!TermsAndConditionsStatus.ACCEPTED.getValue().equals(tncStatus)) {
            throw new FCBadRequestException(TNC_NOT_ACCEPTED_BY_USER);
        }
        final TermsAndConditionAcceptanceRequest request = TermsAndConditionAcceptanceRequest.builder()
                .acceptanceStatus(tncStatus)
                .userId(userId)
                .userType(userType)
                .bureauType(bureauType)
                .build();
        log.info("Acceptance of Terms and Conditions with request {}", request);
        tncService.recordUserActionToTNC(request);
    }

    public CreateFulfillmentResponse processFulfillment(@NonNull final CreateFulfillmentRequest createFulfillmentRequest,
                                                         @NonNull final String clientId, @NonNull final String purpose,
                                                         @NonNull final String userType,
                                                         final CustomerInfo customerInfo) {
        log.info("ProcessFulFillment : Started for userId {} and mobileNumber {} with request {}", createFulfillmentRequest.getUserId(), createFulfillmentRequest.getCustomerInfo().getMobileNumber(), JsonUtil.writeValueAsString(createFulfillmentRequest));
        final CreateFulfillmentResponse response = transunionService.createFulfillment(createFulfillmentRequest);

        if (Objects.nonNull(customerInfo)) {
            log.info("customerInfo fetched from DB for phone {} and userId {}", createFulfillmentRequest
                    .getCustomerInfo().getMobileNumber(), customerInfo.getUserId());
            if (ObjectUtils.isEmpty(customerInfo.getCibilInfo())) {
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
            }
        }
        return response;
    }

    private CreateFulfillmentResponse processFulfillmentExpressSearch(@NonNull final CreateFulfillmentRequestExpressSearch createFulfillmentRequest,
                                                                      @NonNull final String clientId, @NonNull final String purpose,
                                                                      @NonNull final String userType,
                                                                      final CustomerInfo customerInfo) {
        log.info("processFulfillmentExpressSearch : Started for userId {} and mobileNumber {} with request {}", createFulfillmentRequest.getUserId(), createFulfillmentRequest.getCustomerInfo().getMobileNumber(), JsonUtil.writeValueAsString(createFulfillmentRequest));
        final CreateFulfillmentResponse response = transunionService.createFulfillment(createFulfillmentRequest);

        if (Objects.nonNull(customerInfo)) {
            log.info("customerInfo fetched for Express Search from DB for phone {} and userId {}", createFulfillmentRequest
                    .getCustomerInfo().getMobileNumber(), customerInfo.getUserId());
            if (ObjectUtils.isEmpty(customerInfo.getCibilInfo())) {
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
                CustomerCibilInfo info = customerInfo.getCibilInfo();
                CustomerCibilInfoModel cibilInfoModel = CustomerCibilInfoModel.builder()
                        .customerId(info.getCustomerId())
                        .fulfillmentRequest(JsonUtil.writeValueAsString(createFulfillmentRequest))
                        .fulfillmentResponse(response.getFulfillmentStatus())
                        .clientId(info.getClientId())
                        .purpose(info.getPurpose())
                        .cibilReport(info.getCibilReport())
                        .riskScore(info.getRiskScore())
                        .reportUpdatedAt(info.getReportUpdatedAt())
                        .txnId(info.getTxnId())
                        .transactionStatus(info.getTransactionStatus())
                        .createdAt(info.getCreatedAt())
                        .updatedAt(info.getUpdatedAt()).build();
                cibilInfoAccessor.updateRecordWithRequestAndResponseN(cibilInfoModel);
            }
        }
        return response;
    }
}
