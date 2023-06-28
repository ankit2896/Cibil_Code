package com.freecharge.cibil.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.freecharge.cibil.aws.sns.accessor.SNSAccessor;
import com.freecharge.cibil.aws.sns.accessor.impl.ActivityTrackerSNSAccessorImpl;
import com.freecharge.cibil.exceptions.*;
import com.freecharge.cibil.mysql.model.CustomerInfoModel;
import com.freecharge.cibil.rest.CibilParsingService;
import com.freecharge.cibil.constants.FcCibilConstants;
import com.freecharge.cibil.enums.UserActionToPolicyEnum;
import com.freecharge.cibil.model.CustomerCibilInfoModel;
import com.freecharge.cibil.model.assetresponse.CreditScore;
import com.freecharge.cibil.model.assetresponse.CustomerAssetSuccess;
import com.freecharge.cibil.model.enums.*;
import com.freecharge.cibil.model.pojo.CibilVariable;
import com.freecharge.cibil.model.pojo.CibilVariableList;
import com.freecharge.cibil.model.request.*;
import com.freecharge.cibil.model.response.*;
import com.freecharge.cibil.mysql.accessor.CibilInfoAccessor;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.accessor.FreshRefreshInfoAccessor;
import com.freecharge.cibil.mysql.entity.CustomerCibilInfo;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.mysql.mapper.CibilInfoMapper;
import com.freecharge.cibil.mysql.repository.impl.CustomerCibilInfoRepository;
import com.freecharge.cibil.mysql.repository.impl.CustomerInfoRepository;
import com.freecharge.cibil.mysql.repository.impl.CustomerPccMappingRepository;
import com.freecharge.cibil.rest.TNCService;
import com.freecharge.cibil.rest.TransunionService;
import com.freecharge.cibil.rest.dto.request.TermsAndConditionAcceptanceRequest;
import com.freecharge.cibil.rest.dto.response.TermsAndConditionStatusResponse;
import com.freecharge.cibil.service.CibilTransformationService;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.enums.BureauType;
import com.freecharge.vault.PropertiesConfig;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.freecharge.cibil.model.enums.ErrorCodeAndMessage.*;
import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_DAY;

@Log4j2
@Component
public class TransunionDataComponent {

    private final CibilTransformationService cibilTransformationService;

    private final TransunionService transUnionServiceBuilder;

    private final CibilInfoAccessor cibilInfoAccessor;

    private final SNSAccessor snsAccessor;

    private final ActivityTrackerSNSAccessorImpl activityTrackerSNSAwsService;

    private Integer refreshDays;

    private final TNCService tncService;

    private final CustomerInfoMappingAccessor customerInfoMappingAccessor;

    private final FreshRefreshInfoAccessor freshRefreshInfoAccessor;

    private final CustomerPccMappingRepository customerPccMappingRepository;

    private final CustomerInfoRepository customerInfoRepository;

    private final CibilParsingService cibilParsingService;

    private final CustomerCibilInfoRepository customerCibilInfoRepository;

    @Setter
    @Value("${tnc.ims.policyId}")
    private int policyId;

    @Setter
    @Value("${tnc.merchant.policyId}")
    private int merchantPolicyId;


    @Autowired
    public TransunionDataComponent(@NonNull final CibilTransformationService cibilTransformationService,
                                   @Qualifier("transunionService") final TransunionService transUnionServiceBuilder,
                                   @NonNull final CibilInfoAccessor cibilInfoAccessor,
                                   @Qualifier("sNSAccessorImpl") final SNSAccessor snsAccessor,
                                   @Qualifier("activityTrackerSNSAccessorImpl") final ActivityTrackerSNSAccessorImpl activityTrackerSNSAwsService,
                                   @Qualifier("applicationProperties") final PropertiesConfig propertiesConfig,
                                   @NonNull final TNCService tncService, @NonNull final CustomerInfoMappingAccessor customerInfoMappingAccessor,
                                   @NonNull final FreshRefreshInfoAccessor freshRefreshInfoAccessor,
                                   @NonNull final CustomerPccMappingRepository customerPccMappingRepository,
                                   @NonNull final CustomerInfoRepository customerInfoRepository,
                                   @NonNull final CibilParsingService cibilParsingService,
                                   @NonNull final CustomerCibilInfoRepository customerCibilInfoRepository) {
        this.cibilTransformationService = cibilTransformationService;
        this.transUnionServiceBuilder = transUnionServiceBuilder;
        this.cibilInfoAccessor = cibilInfoAccessor;
        this.snsAccessor = snsAccessor;
        this.activityTrackerSNSAwsService = activityTrackerSNSAwsService;
        this.refreshDays = (Integer) propertiesConfig.getProperties()
                .get("eligibility.refresh.ttl.days");
        this.tncService = tncService;
        this.customerInfoMappingAccessor = customerInfoMappingAccessor;
        this.freshRefreshInfoAccessor = freshRefreshInfoAccessor;
        this.customerPccMappingRepository = customerPccMappingRepository;
        this.customerInfoRepository = customerInfoRepository;
        this.cibilParsingService = cibilParsingService;
        this.customerCibilInfoRepository = customerCibilInfoRepository;
    }

    public TransUnionDataFetchResponseV2 getTransUnionDataFetchResponseV2(@NonNull final TransUnionDataFetchRequest request,
                                                                          @NonNull final String mobileNumber,
                                                                          @NonNull final String clientId,
                                                                          @NonNull final String purpose,
                                                                          @NonNull final String userType,
                                                                          final String fcChannel) {
        validatedIfTncStillValid(request.getUserId(), userType);
        final TransUnionDataFetchResponseV2 responseV2 = TransUnionDataFetchResponseV2.builder().build();
        final CustomerCibilInfoModel customerCibilInfoModel = getLatestCibilRecordFromDataBase(mobileNumber);

        log.info("GetTransUnionDataFetchResponseV2 | Latest Record in DB for userId {} and mobileNumber {} fetched {}", request.getUserId(), mobileNumber, customerCibilInfoModel);
        final CustomerCibilInfoModel updatedModel = createFulfillmentIfForReverification(request.getUserId(), customerCibilInfoModel, clientId, purpose, userType);
        log.info("GetTransUnionDataFetchResponseV2 | Updated info model for userId {} and mobileNumber {} is {}", request.getUserId(), mobileNumber, updatedModel);
        responseV2.setFulfillOfferStatus(getFulfillOfferStatus(updatedModel.getFulfillmentResponse()));
        log.info("GetTransUnionDataFetchResponseV2 | Fulfillment Status is {} for userId {} and mobileNumber {} for fetchDataFromTransunion",
                updatedModel.getFulfillmentResponse(), request.getUserId(), mobileNumber);

        if (updatedModel.getFulfillmentResponse().equals(FulfillmentStatus.SUCCESS)) {
            // to check the validity of the user to fetch request from TU
            long fetchDays = 0;

            if (Objects.nonNull(FcCibilConstants.defaultReportDate) && FcCibilConstants.defaultReportDate.compareTo(updatedModel.getReportUpdatedAt()) != 0) {
                final long diffInMillies = Math.abs(updatedModel.getReportUpdatedAt().getTime() - new Date().getTime());
                fetchDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            }

            log.info("GetTransUnionDataFetchResponseV2 | Remaining days {} for userId {} and mobileNumber {}", fetchDays - refreshDays, request.getUserId(), mobileNumber);

            if (Objects.isNull(updatedModel.getCibilReport()) || (fetchDays - refreshDays >= 0)) { // if user available to get the latest report from TU
                String isRefresh = getIsRefreshStatus(updatedModel);
                log.info("GetTransUnionDataFetchResponseV2 | Fetch RequestType {} for userId {} and  mobileNumber {}", isRefresh, request.getUserId(), mobileNumber);

                final TUDataResponse tuDataResponse = fetchDataFromTU(request.getUserId(), mobileNumber, request.getCustomerIdentifier());

                if (Objects.nonNull(tuDataResponse)) {
                    log.info("GetTransUnionDataFetchResponseV2 | Data get from TU for userId {} and mobileNumber {} with riskScore {}", request.getUserId(), mobileNumber, tuDataResponse.getRiskScore());

                    updatedModel.setCibilReport(tuDataResponse.getDataFromTU());
                    updatedModel.setRiskScore(tuDataResponse.getRiskScore());
                    saveDataInCustomerCibilInfo(updatedModel, true);

                    // fresh refresh data call
                    freshRefreshInfoAccessor.parseFreshRefreshData(updatedModel.getFulfillmentRequest(),
                            updatedModel.getFulfillmentResponse(), userType, updatedModel.getCustomerId(), isRefresh, fcChannel,BureauType.CIBIL.getBureauType());

                    // Save data in dynamo db for VCC
                    DynamodbDataInsertionRequest dataInsertionRequest = new DynamodbDataInsertionRequest();
                    dataInsertionRequest.setMobileNumber(mobileNumber);
                    CibilParsing cibilParsing = saveDataInDynamoDb(dataInsertionRequest);
                    log.info("Cibil Parsing for dynamodb: {}", cibilParsing);

                    updatedModel.setReportUpdatedAt(new Date());
                    log.info("GetTransUnionDataFetchResponseV2 | CustomerCibilInfo successfully saved in DB with updated report");
                } else {
                    log.info("FetchCibilData | Data fetched as Null from TU for userId {} and mobileNumber {}", request.getUserId(), mobileNumber);
                    throw new FCInternalClientException(SERVICE_CIBIL_EXCEPTION);
                }
            } else { // user is not available to get the latest report from TU
                log.info("GetTransUnionDataFetchResponseV2 | Data fetched from DB for userId {} and mobileNumber {}", request.getUserId(), mobileNumber);
            }

            List<CibilVariableList> variablesList = transformTransunionDataIntoVariables(Arrays.asList(updatedModel));

            if (StringUtils.isNotBlank(fcChannel) && fcChannel.equals(FcCibilConstants.PWA_WEB_FCCHANNEL_TYPE)) {
                variablesList = getPWAWebRequiredInfomation(variablesList);
            }

            publishTUDataFetchEventToSNS(request.getUserId());
            final long daysFromLastFetch = daysFormLastFetch(updatedModel.getReportUpdatedAt());
            log.info("Updated Model for fetch data from TU v2 api : {}", updatedModel);
            TransUnionDataFetchResponseV2 fetchResponseV2 = TransUnionDataFetchResponseV2.builder()
                    .variables(variablesList.get(0).getCibilVariables())
                    .txnId(updatedModel.getTxnId())
                    .daysSinceLastFetch(daysFromLastFetch)
                    .fulfillOfferStatus(getFulfillOfferStatus(updatedModel.getFulfillmentResponse()))
                    .isFetchTtlExhausted(isFetchTtlExhausted(daysFromLastFetch))
                    .build();
            log.info("GetTransUnionDataFetchResponseV2 | TransUnion Data FetchResponse V2 : {} for userId {} and mobileNumber {}", fetchResponseV2, request.getUserId(), mobileNumber);
            return fetchResponseV2;
        }
        log.info("GetTransUnionDataFetchResponseV2 | TransUnion Data Response V2 : {} for userId {} and mobileNumber {}", responseV2, request.getUserId(), mobileNumber);
        return responseV2;
    }

    public CibilParsing getTransUnionDataFetchResponseV2Optimization(@NonNull final TransUnionDataFetchRequest request,
                                                                     @NonNull final CustomerInfoModel customerInfoModel,
                                                                     @NonNull final String userType,
                                                                     CibilParsing cibilParsing,
                                                                     final String fcChannel,
                                                                     boolean isFulfillmentRequired) {
        CustomerCibilInfoModel customerCibilInfoModel = customerInfoModel.getCustomerCibilInfoModel();

        if (isFulfillmentRequired) {
            customerCibilInfoModel = createFulfillmentIfForReverification(request.getUserId(), customerCibilInfoModel, "", "", userType);
        }

        if (customerCibilInfoModel.getFulfillmentResponse().equals(FulfillmentStatus.SUCCESS)) {
            long fetchDays = 0;

            if (Objects.nonNull(FcCibilConstants.defaultReportDate) && FcCibilConstants.defaultReportDate.compareTo(customerCibilInfoModel.getReportUpdatedAt()) != 0) {
                final long diffInMillies = Math.abs(customerCibilInfoModel.getReportUpdatedAt().getTime() - new Date().getTime());
                fetchDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            }

            log.info("GetTransUnionDataFetchResponseV2 | Remaining days {} for userId {} and mobileNumber {}", fetchDays - refreshDays, request.getUserId(), customerInfoModel.getMobileNumber());

            if (Objects.isNull(customerCibilInfoModel.getCibilReport()) || (fetchDays - refreshDays >= 0)) {
                String isRefresh = getIsRefreshStatusOptimization(customerCibilInfoModel);

                final TUDataResponse tuDataResponse = fetchDataFromTU(request.getUserId(), customerInfoModel.getMobileNumber(), request.getCustomerIdentifier());

                if (Objects.nonNull(tuDataResponse)) {
                    customerCibilInfoModel.setCibilReport(tuDataResponse.getDataFromTU());
                    customerCibilInfoModel.setRiskScore(tuDataResponse.getRiskScore());
                    customerCibilInfoModel.setReportUpdatedAt(new Date());
                    saveDataInCustomerCibilInfo(customerCibilInfoModel, true);

                    freshRefreshInfoAccessor.parseFreshRefreshData(customerCibilInfoModel.getFulfillmentRequest(),
                            customerCibilInfoModel.getFulfillmentResponse(), userType, customerCibilInfoModel.getCustomerId(), isRefresh, fcChannel,BureauType.CIBIL.getBureauType());

                    cibilParsing = saveDataInDynamoDb(customerInfoModel);
                } else {
                    throw new FCInternalClientException(SERVICE_CIBIL_EXCEPTION);
                }
            }
            publishTUDataFetchEventToSNS(request.getUserId());
        }
        return cibilParsing;
    }

    private String getIsRefreshStatus(CustomerCibilInfoModel updatedModel) {
        CustomerCibilInfo customerCibilInfo = getInfoFromCustomerId(updatedModel.getCustomerId());
        String isRefresh = Optional.ofNullable(customerCibilInfo)
                .filter(info -> StringUtils.isNotEmpty(info.getCibilReport()))
                .map(customerCibilInfo1 -> FcCibilConstants.REFRESH)
                .orElseGet(() -> FcCibilConstants.FRESH);
        return isRefresh;
    }

    private String getIsRefreshStatusOptimization(CustomerCibilInfoModel customerCibilInfoModel) {
        String isRefresh = Optional.ofNullable(customerCibilInfoModel)
                .filter(info -> StringUtils.isNotEmpty(info.getCibilReport()))
                .map(customerCibilInfo1 -> FcCibilConstants.REFRESH)
                .orElseGet(() -> FcCibilConstants.FRESH);
        return isRefresh;
    }

    /**
     * This Method Fetches the Data form Transunion Service and returns the transformed data.
     * Transunion Data is Transformed into {@link List< CibilVariable >}.
     * This method also createFulfillment if old one is 24 hrs old.
     * This method old publishes an sns event that transunion data has been fetched for the user.
     *
     * @param request  {@link TransUnionDataFetchRequest}.
     * @param clientId Client Id.
     * @param purpose  purpose for the client.
     * @return {@link TransUnionDataFetchResponse}.
     */
    public TransUnionDataFetchResponse fetchCibilData(@NonNull final TransUnionDataFetchRequest request,
                                                      @NonNull final String mobileNumber,
                                                      @NonNull final String clientId,
                                                      @NonNull final String purpose,
                                                      @NonNull final String userType,
                                                      final String fcChannel) {
        validatedIfTncStillValid(request.getUserId(), userType);
        final CustomerCibilInfoModel customerCibilInfoModel = getLatestCibilRecordFromDataBase(mobileNumber);
        log.info("FetchCibilData | Latest Record in DB for userId {} and mobileNumber {} fetched {}", request.getUserId(), mobileNumber, customerCibilInfoModel);
        final CustomerCibilInfoModel updatedModel = createFulfillmentIfNotValid(request.getUserId(), customerCibilInfoModel, clientId, purpose, userType);
        log.info("FetchCibilData | Fulfillment Status is {} for userId {} and mobileNumber {} for fetchDataFromTransunion",
                updatedModel.getFulfillmentResponse(), request.getUserId(), mobileNumber);

        long fetchDays = 0;

        if (Objects.nonNull(FcCibilConstants.defaultReportDate) && FcCibilConstants.defaultReportDate.compareTo(updatedModel.getReportUpdatedAt()) != 0) {
            final long diffInMillies = Math.abs(updatedModel.getReportUpdatedAt().getTime() - new Date().getTime());
            fetchDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        }

        log.info("FetchCibilData | Remaining days {} for userId {} and mobileNumber {}", fetchDays - refreshDays, request.getUserId(), mobileNumber);

        if (Objects.isNull(updatedModel.getCibilReport()) || (fetchDays - refreshDays >= 0)) { // if user available to get the latest report from TU
            String isRefresh = getIsRefreshStatus(updatedModel);
            log.info("FetchCibilData | Fetch RequestType {} for userId {} and mobileNumber {}", isRefresh, request.getUserId(), mobileNumber);

            final TUDataResponse tuDataResponse = fetchDataFromTU(request.getUserId(), mobileNumber, request.getCustomerIdentifier());

            if (Objects.nonNull(tuDataResponse)) {
                log.info("FetchCibilData | Data fetched from TU for userId {} and mobileNumber {} with riskScore {}", request.getUserId(), mobileNumber, tuDataResponse.getRiskScore());
                updatedModel.setCibilReport(tuDataResponse.getDataFromTU());
                updatedModel.setRiskScore(tuDataResponse.getRiskScore());
                saveDataInCustomerCibilInfo(updatedModel, true);
                // fresh refresh data call
                freshRefreshInfoAccessor.parseFreshRefreshData(updatedModel.getFulfillmentRequest(),
                        updatedModel.getFulfillmentResponse(), userType, updatedModel.getCustomerId(), isRefresh, fcChannel,BureauType.CIBIL.getBureauType());

                // Save data in dynamo db for VCC
                DynamodbDataInsertionRequest dataInsertionRequest = new DynamodbDataInsertionRequest();
                dataInsertionRequest.setMobileNumber(mobileNumber);
                CibilParsing cibilParsing = saveDataInDynamoDb(dataInsertionRequest);
                log.info("Cibil Parsing : {}", cibilParsing);

                updatedModel.setReportUpdatedAt(new Date());
                log.info("FetchCibilData | CustomerCibilInfo successfully saved in DB with updated report");
            } else {
                log.info("FetchCibilData | Data fetched as Null from TU for userId {} and mobileNumber {}", request.getUserId(), mobileNumber);
                throw new FCInternalClientException(SERVICE_CIBIL_EXCEPTION);
            }

        } else {
            // user is not available to get the latest report from TU
            log.info("FetchCibilData | Data fetched from DB for userId {} and mobile {}", request.getUserId(), mobileNumber);
        }

        final List<CibilVariableList> variablesList = transformTransunionDataIntoVariables(Arrays.asList(updatedModel));
        publishTUDataFetchEventToSNS(request.getUserId());
        final long daysFromLastFetch = daysFormLastFetch(updatedModel.getReportUpdatedAt());
        log.info("Updated Model for fetch data from TU v1 api : {}", updatedModel);
        return TransUnionDataFetchResponse.builder()
                .variables(variablesList.get(0).getCibilVariables())
                .txnId(updatedModel.getTxnId())
                .daysSinceLastFetch(daysFromLastFetch)
                .isFetchTtlExhausted(isFetchTtlExhausted(daysFromLastFetch))
                .build();
    }

    @Async
    public CibilParsing saveDataInDynamoDb(DynamodbDataInsertionRequest request) {
        try {
            CustomerInfo customerInfo = customerInfoRepository.getRecordForMobileNumber(request.getMobileNumber());
            if (Objects.nonNull(customerInfo)) {
                log.info("Customer Id : {} for user with mobileNumber : {}", customerInfo.getCustomerId(), request.getMobileNumber());
                Optional<CustomerCibilInfo> customerCibilInfo = customerCibilInfoRepository.findById(customerInfo.getCustomerId());
                if (Objects.nonNull(customerCibilInfo) && customerCibilInfo.isPresent()) {
                    String cibilReport = customerCibilInfo.get().getCibilReport();
                    if (StringUtils.isNotBlank(cibilReport)) {
                        CustomerAssetSuccess customerAssetSuccess = JsonUtil.convertStringIntoObject(cibilReport, CustomerAssetSuccess.class);
                        AddCustomerAssetSuccessRequest addRequest = AddCustomerAssetSuccessRequest.builder()
                                .customerId(customerInfo.getCustomerId())
                                .userId(customerInfo.getUserId())
                                .mobileNumber(customerInfo.getMobileNumber())
                                .customerAssetSuccess(customerAssetSuccess)
                                .build();
                        CibilParsing cibilParsing = cibilParsingService.addCibilReportDetails(addRequest);
                        return cibilParsing;
                    }
                }
            }
        } catch (Exception e) {
            log.info("Exception while saving details into Dynamo db ");
            e.printStackTrace();
        }
        return new CibilParsing();
    }


    @Async
    public CibilParsing saveDataInDynamoDb(@NonNull CustomerInfoModel customerInfoModel) {
        if (Objects.nonNull(customerInfoModel.getCustomerCibilInfoModel().getCibilReport())) {
            return cibilParsingService.addDetails(customerInfoModel);
        } else {
            return new CibilParsing();
        }
    }


    private List<CibilVariableList> getPWAWebRequiredInfomation(List<CibilVariableList> variablesList) {
        Set<VariableNameAndType> set = new HashSet<>();
        set.add(VariableNameAndType.CREDIT_SCORE_CATEGORY);
        set.add(VariableNameAndType.CREDIT_SCORE);
        set.add(VariableNameAndType.CREDIT_SCORE_CATEGORY_COLOR_CODE);

        CibilVariableList cibilVariableList = variablesList.get(0);

        List<CibilVariable> cibilVariables = new ArrayList<>();
        for (CibilVariable cibilVariable1 : cibilVariableList.getCibilVariables()) {
            if (set.contains(cibilVariable1.getVariableName())) {
                cibilVariables.add(cibilVariable1);
            }
        }

        List<CibilVariableList> response = new ArrayList<>();
        CibilVariableList newCibilVariableList = new CibilVariableList();

        newCibilVariableList.setCibilVariables(cibilVariables);
        newCibilVariableList.setFetchDate(cibilVariableList.getFetchDate());
        newCibilVariableList.setImsId(cibilVariableList.getImsId());
        newCibilVariableList.setTxnId(cibilVariableList.getTxnId());
        response.add(newCibilVariableList);

        return response;
    }

    public CustomerCibilInfoModel getLatestCibilRecordFromDataBase(@NonNull final String mobileNumber) {
        return customerInfoMappingAccessor.fetchRecordFromMobileNumber(mobileNumber);
    }


    public CustomerCibilInfoModel createFulfillmentIfForReverification(@NonNull final String userId,
                                                                       @Nullable final CustomerCibilInfoModel customerCibilInfoModel,
                                                                       @NonNull final String clientId, @NonNull final String purpose,
                                                                       @NonNull final String userType) {
        if (ObjectUtils.isNotEmpty(customerCibilInfoModel)) {
            if (FulfillmentStatus.SUCCESS.equals(customerCibilInfoModel.getFulfillmentResponse())) {
                if (isFulfillmentNotValid(customerCibilInfoModel.getUpdatedAt())) {
                    final FulfillmentStatus status = createFulfillment(customerCibilInfoModel.getFulfillmentRequest());
                    log.info("createFulfillmentIfForReverification status {} for userId {}", status, userId);
                    customerCibilInfoModel.setFulfillmentResponse(status);
                    saveDataInCustomerCibilInfo(customerCibilInfoModel, false);
                    return customerCibilInfoModel;
                }
            }
            return customerCibilInfoModel;
        }
        log.error("Successful fulfillment could not be found for userId {} and DB object {}", userId, customerCibilInfoModel);
        throw new FCCDependencyFailureNonRetriableException(USER_NOT_ON_BOARDED);
    }


    public FulfillOfferStatus getFulfillOfferStatus(@NonNull final FulfillmentStatus status) {
        return FulfillOfferStatus.enumOf(status.toString());
    }

    private CustomerCibilInfoModel createFulfillmentIfNotValid(@NonNull final String userId,
                                                               @Nullable final CustomerCibilInfoModel customerCibilInfoModel,
                                                               @NonNull final String client,
                                                               @NonNull final String purpose,
                                                               @NonNull final String userType) {
        if (ObjectUtils.isNotEmpty(customerCibilInfoModel)
                && FulfillmentStatus.SUCCESS.equals(customerCibilInfoModel.getFulfillmentResponse())) {
            if (isFulfillmentNotValid(customerCibilInfoModel.getUpdatedAt())) {
                final FulfillmentStatus status = createFulfillment(customerCibilInfoModel.getFulfillmentRequest());
                if (!FulfillmentStatus.SUCCESS.equals(status)) {
                    log.error("Successful fulfillment could not be created for imsId {} and request {}"
                            , customerCibilInfoModel.getCustomerId(), customerCibilInfoModel.getFulfillmentRequest());
                    throw new FCCDependencyFailureNonRetriableException(
                            ErrorCodeAndMessage.SUCCESSFUL_FULFILLMENT_COULD_NOT_BE_CREATED);
                }
                customerCibilInfoModel.setFulfillmentResponse(status);
                saveDataInCustomerCibilInfo(customerCibilInfoModel, false);
                return customerCibilInfoModel;
            }
            return customerCibilInfoModel;
        }
        log.error("Successful fulfillment could not be found for userId {} and DB object {}"
                , userId, customerCibilInfoModel);
        throw new FCCDependencyFailureNonRetriableException(
                ErrorCodeAndMessage.SUCCESSFUL_FULFILLMENT_NOT_FOUND);
    }

    private boolean isFulfillmentNotValid(@NonNull final Date recordUpdateDate) {
        return Math.abs(recordUpdateDate.getTime() - new Date().getTime()) > MILLIS_PER_DAY;
    }

    private FulfillmentStatus createFulfillment(@NonNull final String requestString) {
        final FulfillmentStatus status;

        // for normal search
        if (requestString.contains("identificationInformation")) {
            final CreateFulfillmentRequest fulfillmentRequest = JsonUtil.convertStringIntoObject(requestString,
                    new TypeReference<CreateFulfillmentRequest>() {
                    });

            status = transUnionServiceBuilder.createFulfillment(fulfillmentRequest)
                    .getFulfillmentStatus();
            log.info("fulfillment for normal search {} is {}", fulfillmentRequest.getUserId(), status);

            // for expressed search
        } else {
            final CreateFulfillmentRequestExpressSearch fulfillmentRequest = JsonUtil.convertStringIntoObject(requestString,
                    new TypeReference<CreateFulfillmentRequestExpressSearch>() {
                    });

            status = transUnionServiceBuilder.createFulfillment(fulfillmentRequest)
                    .getFulfillmentStatus();
            log.info("fulfillment for expressed search {} is {}", fulfillmentRequest.getUserId(), status);
        }

        return status;
    }

    private void saveDataInCustomerCibilInfo(@NonNull final CustomerCibilInfoModel customerCibilInfoModel, boolean isReportUpdated) {
        cibilInfoAccessor.saveCustomerCibilInfo(customerCibilInfoModel, isReportUpdated);
    }

    private CustomerCibilInfo getInfoFromCustomerId(String customerId) {
        CustomerCibilInfo customerCibilInfo = cibilInfoAccessor.getCustomerCibilInfoByCustId(customerId);
        log.info("CustomerCibilInfo's get cibil report :: {}", customerCibilInfo.getCibilReport());
        return customerCibilInfo;
    }

    private TUDataResponse fetchDataFromTU(@NonNull final String userId, @NonNull final String mobileNumber, @NonNull final String customerIdentifier) {
        CustomerAssetSuccess response = transUnionServiceBuilder.getCustomerAssets(userId, mobileNumber, customerIdentifier);
        log.info("Transunion Data Fetched for userId {} and mobileNumber {} with pccId {}", userId, mobileNumber, customerIdentifier);
        if (Objects.isNull(response.getGetCustomerAssetsSuccess())) {
            log.info("Transunion Data Fetched for userId {} and mobileNumber {} with pccId {} with NUll Report");
            return null;
        }
        final Optional<List<CreditScore>> creditScoreList = Optional.ofNullable(response.getGetCustomerAssetsSuccess().getAsset().get(0).getTrueLinkCreditReport().getBorrower().get(0).getCreditScore());
        final Optional<CreditScore> creditScore = Optional.ofNullable(creditScoreList.isPresent() ? creditScoreList.get().get(0) : null);
        return TUDataResponse.builder()
                .dataFromTU(JsonUtil.writeValueAsString(response))
                .riskScore(creditScore.get().getRiskScore())
                .build();
    }

    private List<CibilVariableList> transformTransunionDataIntoVariables(@NonNull final List<CustomerCibilInfoModel> customerCibilInfoModels) {
        return cibilTransformationService.getCibilVariablesList(customerCibilInfoModels);
    }

    private void publishTUDataFetchEventToSNS(@NonNull final String imsId) {
        snsAccessor.publishEvent(imsId, EventType.TU_DATA_FETCHED);
        activityTrackerSNSAwsService.publishEvent(imsId, EventType.CIBIL_SCORE);
    }

    /**
     * This Method Fetches the Latest record of TU data for user from DB.
     * Transunion Data is Transformed into {@link List< CibilVariable >}.
     *
     * @param request {@link TransUnionDataFetchRequest}.
     * @return {@link TransUnionDataFetchResponse}.
     */
    public TransUnionDataFetchResponse fetchLatestRecord(@NonNull final TransUnionDataFetchRequest request,
                                                         @NonNull final String userType,
                                                         @NonNull final String mobileNumber,
                                                         final String fcChannel) {
        validatedIfTncStillValid(request.getUserId(), userType);
        reAcceptTNC(request.getUserId(), userType, request.getAction());
        final CustomerCibilInfoModel customerCibilInfoModel = fetchLatestRecordWithReport(request.getUserId(), UserType.enumOf(userType), mobileNumber);
        log.info("Fetching variables for userId {} and mobileNumber {} for latest record.", request.getUserId(), mobileNumber);
        List<CibilVariableList> variablesList = transformTransunionDataIntoVariables(Arrays.asList(customerCibilInfoModel));

        if (StringUtils.isNotBlank(fcChannel) && fcChannel.equals(FcCibilConstants.PWA_WEB_FCCHANNEL_TYPE)) {
            variablesList = getPWAWebRequiredInfomation(variablesList);
        }

        final long daysFromLastFetch = daysFormLastFetch(customerCibilInfoModel.getReportUpdatedAt());
        return TransUnionDataFetchResponse.builder()
                .variables(variablesList.get(0).getCibilVariables())
                .txnId(customerCibilInfoModel.getTxnId())
                .daysSinceLastFetch(daysFromLastFetch)
                .isFetchTtlExhausted(isFetchTtlExhausted(daysFromLastFetch))
                .build();
    }

    /**
     * This Method Fetches the Records State of TU data for user for Ops Pannel.
     *
     * @param userId User Id.
     * @return {@link RecordStateResponse}.
     */
    public RecordStateResponse fetchRecordState(@NonNull final String userId) {
        final Optional<CustomerCibilInfoModel> cibilInfoModelOptional = cibilInfoAccessor.getAllFetchedRecordsForUser(userId);
        //final Optional<CustomerCibilInfoModel> cibilInfoModelOptionalWithoutReport = cibilInfoAccessor.getAllFetchedRecordsForUserWithoutReport(userId);
        if (!cibilInfoModelOptional.isPresent()) {
            log.error("No Record existing for user {}", userId);
            throw new FCBadRequestException(NO_RECORD_FOUND);
        } else if (cibilInfoModelOptional.isPresent()) {
            log.warn("No Report record Found for user {}", userId);
            final CustomerCibilInfoModel customerCibilInfoModel = cibilInfoModelOptional.get();
            if (FulfillmentStatus.NO_HIT.equals(customerCibilInfoModel.getFulfillmentResponse())) {
                return generateRecordStateResponseForNoHit(customerCibilInfoModel);
            }
            return RecordStateResponse.builder()
                    .recordState(RecordState.NO_DATA)
                    .build();
        }
        return generateRecordStateResponseForSuccess(cibilInfoModelOptional.get());
    }


    private RecordStateResponse generateRecordStateResponseForNoHit(@NonNull final CustomerCibilInfoModel customerCibilInfoModel) {
        log.error("Report record Found {} for imsId {}", customerCibilInfoModel, customerCibilInfoModel.getCustomerId());
        return RecordStateResponse.builder()
                .recordState(RecordState.NO_HIT)
                .build();
    }

    private RecordStateResponse generateRecordStateResponseForSuccess(@NonNull final CustomerCibilInfoModel customerCibilInfoModel) {
        log.error("Report record Found {} for imsId {}", customerCibilInfoModel, customerCibilInfoModel.getCustomerId());
        return RecordStateResponse.builder()
                .recordState(RecordState.SUCCESS)
                .dateOfPull(customerCibilInfoModel.getReportUpdatedAt())
                .build();
    }

    private CustomerCibilInfoModel fetchLatestRecordWithReport(@NonNull final String userId, @NonNull final UserType userType, @NonNull final String mobileNumber) {

        Optional<CustomerCibilInfoModel> cibilInfoModelOptional = Optional.empty();
        Optional<CustomerInfo> customerInfoOptional = Optional.empty();

        customerInfoOptional = customerInfoMappingAccessor.getRecordForMobileNumber(mobileNumber);

        if (customerInfoOptional.isPresent() && Objects.nonNull(customerInfoOptional.get().getCibilInfo()) && Objects.nonNull(customerInfoOptional.get().getCibilInfo().getCibilReport()))
            cibilInfoModelOptional = Optional.of(CibilInfoMapper.toModel(customerInfoOptional.get().getCibilInfo()));


        log.info("Record found {} with report for userId {} and mobileNumber {}", cibilInfoModelOptional, userId, mobileNumber);
        final CustomerCibilInfoModel customerCibilInfoModel = cibilInfoModelOptional.orElseThrow(
                () -> new FCBadRequestException(DATA_NOT_FOUND)
        );
        log.info("Latest Record for userId {} and mobileNumber {} with report fetched.", userId, mobileNumber);
        return customerCibilInfoModel;
    }

    private long daysFormLastFetch(@NonNull final Date lastFetchDate) {
        final long diffInMillies = Math.abs(lastFetchDate.getTime() - new Date().getTime());
        return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    private boolean isFetchTtlExhausted(@NonNull final long daysFormLastFetch) {
        return ((daysFormLastFetch - refreshDays) >= 0);
    }

    private void validatedIfTncStillValid(@NonNull final String userId, @NonNull final String userType) {
        final TermsAndConditionStatusResponse response = tncService.getUserPolicyAcceptanceStatus(userId, userType, BureauType.CIBIL.getBureauType());
        if ((!Objects.isNull(response.getPolicyId()) && (policyId == response.getPolicyId() || merchantPolicyId == response.getPolicyId()) && (!Objects.isNull(response.getAction()) && UserActionToPolicyEnum.EXPIRED.equals(response.getAction())))) {
            throw new FCCNonRetriableException(TNC_EXPIRED_EXCEPTION);
        }
    }

    private void reAcceptTNC(@NonNull final String userId, @NonNull final String userType, @NonNull final UserAction action) {
        if (UserAction.RE_ACCEPTED.equals(action)) {
            log.info("Re-Accepting TNC for Action {}", action);
            tncService.reRecordUserActionToTNC(TermsAndConditionAcceptanceRequest.builder()
                    .userId(userId)
                    .userType(userType)
                    .acceptanceStatus(TermsAndConditionsStatus.ACCEPTED.getValue())
                    .build());
        }
    }
}
