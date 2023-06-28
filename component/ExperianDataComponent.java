package com.freecharge.cibil.component;

import com.freecharge.cibil.enums.UserActionToPolicyEnum;
import com.freecharge.cibil.exceptions.FCBadRequestException;
import com.freecharge.cibil.exceptions.FCCNonRetriableException;
import com.freecharge.cibil.model.CustomerCibilInfoModel;
import com.freecharge.cibil.model.enums.TermsAndConditionsStatus;
import com.freecharge.cibil.model.enums.UserAction;
import com.freecharge.cibil.model.enums.UserType;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.accessor.FreshRefreshInfoAccessor;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.mysql.mapper.ExperianInfoMapper;
import com.freecharge.experian.enums.BureauType;
import com.freecharge.experian.model.CustomerExperianInfoModel;
import com.freecharge.cibil.rest.TNCService;
import com.freecharge.cibil.rest.dto.request.TermsAndConditionAcceptanceRequest;
import com.freecharge.cibil.rest.dto.response.TermsAndConditionStatusResponse;
import com.freecharge.experian.model.pojo.ExperianVariableList;
import com.freecharge.experian.model.request.ExperianDataFetchRequest;
import com.freecharge.experian.model.response.ExperianDataFetchResponse;
import com.freecharge.experian.transformer.service.ExperianTransformationService;
import com.freecharge.vault.PropertiesConfig;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.freecharge.cibil.model.enums.ErrorCodeAndMessage.DATA_NOT_FOUND;
import static com.freecharge.cibil.model.enums.ErrorCodeAndMessage.TNC_EXPIRED_EXCEPTION;

@Log4j2
@Component
public class ExperianDataComponent {

    private final TNCService tncService;
    private final FreshRefreshInfoAccessor freshRefreshInfoAccessor;

    private final CustomerInfoMappingAccessor customerInfoMappingAccessor;

    private final ExperianTransformationService experianTransformationService;

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
    public ExperianDataComponent(@Qualifier("applicationProperties") final PropertiesConfig propertiesConfig,
                                 @NonNull final TNCService tncService,
                                 @NonNull final CustomerInfoMappingAccessor customerInfoMappingAccessor,
                                 @NonNull final FreshRefreshInfoAccessor freshRefreshInfoAccessor, @NonNull final ExperianTransformationService experianTransformationService) {

        this.refreshDays = (Integer) propertiesConfig.getProperties()
                .get("eligibility.refresh.ttl.days");
        this.tncService = tncService;
        this.freshRefreshInfoAccessor = freshRefreshInfoAccessor;
        this.customerInfoMappingAccessor = customerInfoMappingAccessor;
        this.experianTransformationService = experianTransformationService;
    }

    public ExperianDataFetchResponse fetchLatestRecord(@NonNull final ExperianDataFetchRequest request,
                                                       @NonNull final String userType,
                                                       final String fcChannel) {
        validatedIfTncStillValid(request.getUserId(), userType);
        reAcceptTNC(request.getUserId(), userType, request.getAction(),BureauType.EXPERIAN.getBureauType());

        final CustomerExperianInfoModel customerExperianInfoModel = fetchLatestRecordWithReport(request.getUserId(), UserType.enumOf(userType), request.getMobileNumber());
        log.info("Fetching variables for userId {} and mobileNumber {} for latest record.", request.getUserId(), request.getMobileNumber());

        List<ExperianVariableList> variablesList = transformExperianDataIntoVariables(Arrays.asList(customerExperianInfoModel));

        final long daysFromLastFetch = daysFormLastFetch(customerExperianInfoModel.getReportUpdatedAt());
        return ExperianDataFetchResponse.builder()
                .variables(variablesList.get(0).getExperianVariables())
                .daysSinceLastFetch(daysFromLastFetch)
                .isFetchTtlExhausted(isFetchTtlExhausted(daysFromLastFetch))
                .build();
    }

    private boolean isFetchTtlExhausted(final long daysFormLastFetch) {
        return ((daysFormLastFetch - refreshDays) >= 0);
    }

    public void validatedIfTncStillValid(@NonNull final String userId, @NonNull final String userType) {
        final TermsAndConditionStatusResponse response = tncService.getUserPolicyAcceptanceStatus(userId, userType, BureauType.EXPERIAN.getBureauType());
        if ((!Objects.isNull(response.getPolicyId()) && experianPolicyId == response.getPolicyId() && (!Objects.isNull(response.getAction()) && UserActionToPolicyEnum.EXPIRED.equals(response.getAction())))) {
            throw new FCCNonRetriableException(TNC_EXPIRED_EXCEPTION);
        }
    }

    private void reAcceptTNC(@NonNull final String userId, @NonNull final String userType, @NonNull final UserAction action ,@NonNull String bureauType) {
        if (UserAction.RE_ACCEPTED.equals(action)) {
            log.info("Re-Accepting TNC for Action {}", action);
            tncService.reRecordUserActionToTNC(TermsAndConditionAcceptanceRequest.builder()
                    .userId(userId)
                    .userType(userType)
                    .bureauType(bureauType)
                    .acceptanceStatus(TermsAndConditionsStatus.ACCEPTED.getValue())
                    .build());
        }
    }

    public CustomerExperianInfoModel fetchLatestRecordWithReport(@NonNull final String userId, @NonNull final UserType userType, @NonNull final String mobileNumber) {

        Optional<CustomerExperianInfoModel> experianInfoModelOptional = Optional.empty();
        Optional<CustomerInfo> customerInfoOptional = customerInfoMappingAccessor.getRecordForMobileNumber(mobileNumber);

        if (customerInfoOptional.isPresent() && Objects.nonNull(customerInfoOptional.get().getExperianInfo()) && Objects.nonNull(customerInfoOptional.get().getExperianInfo().getExperianReport()))
            experianInfoModelOptional = Optional.of(ExperianInfoMapper.toModel(customerInfoOptional.get().getExperianInfo()));


        log.info("Record found {} with report for userId {} and mobileNumber {}", experianInfoModelOptional, userId, mobileNumber);
        final CustomerExperianInfoModel customerExperianInfoModel = experianInfoModelOptional.orElseThrow(
                () -> new FCBadRequestException(DATA_NOT_FOUND)
        );
        log.info("Latest Record for userId {} and mobileNumber {} with report fetched.", userId, mobileNumber);
        return customerExperianInfoModel;
    }

    public List<ExperianVariableList> transformExperianDataIntoVariables(@NonNull final List<CustomerExperianInfoModel> customerExperianInfoModels) {
        return experianTransformationService.getExperianVariablesList(customerExperianInfoModels);
    }

    private long daysFormLastFetch(@NonNull final Date lastFetchDate) {
        final long diffInMillies = Math.abs(lastFetchDate.getTime() - new Date().getTime());
        return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

}
