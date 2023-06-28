package com.freecharge.cibil.component;

import com.freecharge.cibil.config.EligibilityConfig;
import com.freecharge.cibil.eligibility.pojo.EligibilityCriteriaRequest;
import com.freecharge.cibil.eligibility.factory.EligibilityFactory;
import com.freecharge.cibil.enums.UserActionToPolicyEnum;
import com.freecharge.cibil.model.CustomerCibilInfoModel;
import com.freecharge.cibil.model.enums.UserType;
import com.freecharge.cibil.model.pojo.CibilScoreRecord;
import com.freecharge.cibil.model.response.EligibilityResponseV2;
import com.freecharge.cibil.mysql.accessor.CibilInfoAccessor;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.entity.CustomerCibilInfo;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.mysql.entity.CustomerPccMapping;
import com.freecharge.cibil.mysql.mapper.CibilInfoMapper;
import com.freecharge.cibil.mysql.mapper.ExperianInfoMapper;
import com.freecharge.experian.model.CustomerExperianInfoModel;
import com.freecharge.cibil.mysql.repository.impl.CustomerInfoRepository;
import com.freecharge.cibil.rest.TNCService;
import com.freecharge.cibil.rest.dto.response.TermsAndConditionStatusResponse;
import com.freecharge.cibil.service.CibilTransformationService;
import com.freecharge.cibil.utils.DateUtils;
import com.freecharge.experian.enums.BureauType;
import com.freecharge.experian.transformer.service.ExperianTransformationService;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class EligibilityComponent {

    private final EligibilityFactory eligibilityFactory;

    private final CibilInfoAccessor cibilInfoAccessor;

    private final CustomerInfoMappingAccessor customerInfoMappingAccessor;

    private final CibilTransformationService cibilTransformationService;

    private final ExperianTransformationService experianTransformationService;

    private final TNCService tncService;

    private final CustomerInfoRepository customerInfoRepository;

    @Setter
    private Integer refreshDays;

    @Setter
    @Value("${tnc.ims.policyId}")
    private int policyId;

    @Setter
    @Value("${tnc.merchant.policyId}")
    private int merchantPolicyId;

    @Setter
    @Value("${tnc.experian.policyId}")
    private int experianPolicyId;

    @Autowired
    public EligibilityComponent(@NonNull final EligibilityFactory eligibilityFactory,
                                @NonNull final CibilInfoAccessor cibilInfoAccessor,
                                @NonNull CustomerInfoMappingAccessor customerInfoMappingAccessor, @NonNull final CibilTransformationService cibilTransformationService,
                                @NonNull final ExperianTransformationService experianTransformationService,
                                @NonNull final TNCService tncService,
                                @NonNull final EligibilityConfig eligibilityConfig,
                                @NonNull final CustomerInfoRepository customerInfoRepository) {
        this.eligibilityFactory = eligibilityFactory;
        this.cibilInfoAccessor = cibilInfoAccessor;
        this.customerInfoMappingAccessor = customerInfoMappingAccessor;
        this.cibilTransformationService = cibilTransformationService;
        this.experianTransformationService = experianTransformationService;
        this.tncService = tncService;
        this.refreshDays = eligibilityConfig.getRefreshDays();
        this.customerInfoRepository = customerInfoRepository;
    }


    public EligibilityResponseV2 getCibilEligibility(String userId, String userType, String mobileNumber, String deviceBindingId, String bureauType) {
        long score = 0;
        long fetchDate = 0;
        log.info("Eligible Started for userId and mobileNumber {} and {}", userId, mobileNumber);
        Optional<CustomerCibilInfoModel> customerCibilInfoModelOptional = Optional.empty();

        Optional<CustomerInfo> customerInfoOptional = customerInfoMappingAccessor.getRecordForMobileNumber(mobileNumber);
        if(customerInfoOptional.isPresent() && !userType.equals(customerInfoOptional.get().getUserType().getUserType()) && bureauType.equals(BureauType.CIBIL.getBureauType())){
            CustomerInfo customerInfo = customerInfoOptional.get();
            customerInfo.setUserType(UserType.USER_MERCHANT);
            customerInfoRepository.save(customerInfo);
        }
        log.info("Eligibility : Record fetched from mobile number : {}", mobileNumber);

            if(!customerInfoOptional.isPresent()) {
                customerInfoOptional = customerInfoMappingAccessor.getRecordForUserId(userId);
                log.info("Eligibility : Record fetched from User Id : {}" , userId);
                if (customerInfoOptional.isPresent()) {
                    CustomerInfo userInfo = customerInfoOptional.get();

                    CustomerPccMapping customerPccMapping = userInfo.getCustomerPccMapping();
                    CustomerPccMapping newCustomerPccMapping = CustomerPccMapping.builder()
                            .pccId(customerPccMapping.getPccId())
                            .build();

                    CustomerCibilInfo customerCibilInfo = userInfo.getCibilInfo();
                    CustomerCibilInfo newCustomerCibilInfo = null;
                    if(Objects.nonNull(customerCibilInfo)) {
                         newCustomerCibilInfo = CustomerCibilInfo.builder()
                                .cibilReport(Objects.nonNull(customerCibilInfo.getCibilReport()) ? customerCibilInfo.getCibilReport() : null)
                                .clientId(customerCibilInfo.getClientId())
                                .fulfillmentRequest(customerCibilInfo.getFulfillmentRequest())
                                .fulfillmentResponse(customerCibilInfo.getFulfillmentResponse())
                                .purpose(customerCibilInfo.getPurpose())
                                .txnId(MDC.get("requestId"))
                                .transactionStatus(customerCibilInfo.getTransactionStatus())
                                .build();
                    }

                    CustomerInfo newCustomerInfo = CustomerInfo.builder()
                                    .userId(userInfo.getUserId())
                                    .mobileNumber(mobileNumber)
                                    .userType(userInfo.getUserType())
                                    .email(Objects.nonNull(userInfo.getEmail()) ? userInfo.getEmail() : null)
                                    .customerPccMapping(newCustomerPccMapping)
                                    .cibilInfo(newCustomerCibilInfo)
                                    .pancardNumber(Objects.nonNull(userInfo.getPancardNumber()) ? userInfo.getPancardNumber() : null)
                                    .build();

                    newCustomerPccMapping.setCustomerInfo(newCustomerInfo);
                    if (Objects.nonNull(newCustomerCibilInfo)) {
                        newCustomerCibilInfo.setCustomerInfo(newCustomerInfo);
                    }
                    customerInfoRepository.save(newCustomerInfo);
                    log.info("Eligibility : New Record Created for  different mobileNumber and Same UserId with customerId {}",newCustomerInfo.getCustomerId());
                }
            }

        if(customerInfoOptional.isPresent() &&
                Objects.nonNull(customerInfoOptional.get().getCibilInfo()) &&
                Objects.nonNull(customerInfoOptional.get().getCibilInfo().getCibilReport())) {
            customerCibilInfoModelOptional = Optional.of(CibilInfoMapper.toModel(customerInfoOptional.get().getCibilInfo()));
        }

        if (customerCibilInfoModelOptional.isPresent()) {
            final CustomerCibilInfoModel customerCibilInfoModel = customerCibilInfoModelOptional.get();
            final CibilScoreRecord record = cibilTransformationService
                    .getCibilScoreRecord(Arrays.asList(customerCibilInfoModel)).get(0);
            score = record.getCibilScore();
            if (score != 0) {
                final long diffInMillies = Math.abs(record.getCibilScoreFetchDate().getTime() - new Date().getTime());
                fetchDate = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            }
        }
        boolean isEligible = eligibilityFactory.checkIfEligible(EligibilityCriteriaRequest.builder()
                .userId(userId)
                .deviceBindingId(deviceBindingId)
                .build(), userType);
        return EligibilityResponseV2.builder()
                .eligible(isEligible)
                .score(score)
                .daysSinceLastFetch(fetchDate)
                .isFetchTtlExhausted(((fetchDate - refreshDays) >= 0))
                .build();
    }

    public EligibilityResponseV2 getEligibilityWithTncData(String userId, String userType, String phoneNum, String deviceBindingId, String bureauType) {
        EligibilityResponseV2 eligibilityResponse = null;
        if(bureauType.equals(BureauType.CIBIL.getBureauType())){
             eligibilityResponse = getCibilEligibility(userId,userType,phoneNum,deviceBindingId,bureauType);
        }else if(bureauType.equals(BureauType.EXPERIAN.getBureauType())){
             eligibilityResponse = getExperianEligibility(userId,userType,phoneNum,deviceBindingId,bureauType);
        }
        return updateEligibilityResponseV2WithTncData(userId,userType,bureauType, eligibilityResponse);
    }


    public EligibilityResponseV2 getExperianEligibility(String userId, String userType, String mobileNumber, String deviceBindingId, String bureauType) {
        long score = 0;
        long fetchDate = 0;
        log.info("Eligible Started for userId and mobileNumber {} and {}", userId, mobileNumber);
        Optional<CustomerExperianInfoModel> customerExperianInfoModelOptional = Optional.empty();
        Optional<CustomerInfo> customerInfoOptional = customerInfoMappingAccessor.getRecordForMobileNumber(mobileNumber);

        log.info("Eligibility : Record fetched from mobile number : {}" , mobileNumber);

        if(customerInfoOptional.isPresent() &&
                Objects.nonNull(customerInfoOptional.get().getExperianInfo()) &&
                Objects.nonNull(customerInfoOptional.get().getExperianInfo().getExperianReport())) {
            customerExperianInfoModelOptional = Optional.of(ExperianInfoMapper.toModel(customerInfoOptional.get().getExperianInfo()));
        }

        if (customerExperianInfoModelOptional.isPresent()) {
            final CustomerExperianInfoModel customerExperianInfoModel = customerExperianInfoModelOptional.get();
            final CibilScoreRecord record = experianTransformationService.getExperianScoreRecord(Arrays.asList(customerExperianInfoModel)).get(0);
            score = record.getCibilScore();
            if (score != 0 ) {
                final long diffInMillies = Math.abs(record.getCibilScoreFetchDate().getTime() - new Date().getTime());
                fetchDate = TimeUnit.DAYS.convert(diffInMillies , TimeUnit.MILLISECONDS);
            }
        }
        return EligibilityResponseV2.builder()
                .eligible(true)
                .score(score)
                .daysSinceLastFetch(fetchDate)
                .isFetchTtlExhausted(((fetchDate - refreshDays) >= 0))
                .build();
    }





    private EligibilityResponseV2 updateEligibilityResponseV2WithTncData(@NonNull final String userId, @NonNull final String userType,@NonNull final String bureauType,
                                                                         @NonNull final EligibilityResponseV2 eligibilityResponse) {
        final TermsAndConditionStatusResponse response =
                tncService.getUserPolicyAcceptanceStatus(userId,userType,bureauType);
        if((!Objects.isNull(response.getPolicyId()) && (policyId == response.getPolicyId() || merchantPolicyId == response.getPolicyId() || experianPolicyId == response.getPolicyId()))) {
            if((!Objects.isNull(response.getAction()) &&
                    UserActionToPolicyEnum.EXPIRED.equals(response.getAction()))) {
                eligibilityResponse.setTncExpired(true);
            } else {
                eligibilityResponse.setTncExpired(false);
            }
            if (Objects.isNull(response.getExpiryTime())) {
                eligibilityResponse.setTncTtl(-1);
            } else {
                eligibilityResponse.setTncTtl(
                        DateUtils.differenceInDaysBetweenDateAndCurrentDate(response.getExpiryTime()));
            }
        }
        return eligibilityResponse;
    }
}
