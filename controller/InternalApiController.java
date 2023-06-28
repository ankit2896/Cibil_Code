package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.component.EligibilityComponent;
import com.freecharge.cibil.component.TransunionDataComponent;
import com.freecharge.cibil.config.EligibilityConfig;
import com.freecharge.cibil.constants.ApiUrl;
import com.freecharge.cibil.eligibility.provider.impl.UserEligibilityProvider;
import com.freecharge.cibil.model.enums.UserAction;
import com.freecharge.cibil.model.enums.UserType;
import com.freecharge.cibil.model.pojo.CibilVariable;
import com.freecharge.cibil.model.request.TransUnionDataFetchRequest;
import com.freecharge.cibil.model.response.*;
import com.freecharge.cibil.mysql.accessor.CibilInfoAccessor;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.rest.TNCService;
import com.freecharge.cibil.service.CibilTransformationService;
import com.freecharge.cibil.utility.ImsUserUtils;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.enums.BureauType;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;

import static com.freecharge.cibil.constants.ApiUrl.ELIGIBILITY_API_INTERNAL;
import static com.freecharge.cibil.constants.HeaderConstants.*;

/**
 * Controller for Internal Service Communication
 */
@Log4j2
@RestController
public class InternalApiController {

    private final TransunionDataComponent transunionDataComponent;

    private final UserEligibilityProvider eligibilityProvider;

    private final CibilInfoAccessor cibilInfoAccessor;

    private final CibilTransformationService cibilTransformationService;

    private final CustomerInfoMappingAccessor customerInfoMappingAccessor;

    private final TNCService tncService;

    private final EligibilityComponent eligibilityComponent;

    private final ImsUserUtils imsUserUtils;

    @Setter
    private Integer refreshDays;

    @Setter
    @Value("${tnc.ims.policyId}")
    private int policyId;

    @Autowired
    public InternalApiController(@NonNull final TransunionDataComponent transunionDataComponent,
                                 @NonNull final UserEligibilityProvider eligibilityProvider,
                                 @NonNull final CibilInfoAccessor cibilInfoAccessor,
                                 @NonNull final CibilTransformationService cibilTransformationService,
                                 @NonNull final CustomerInfoMappingAccessor customerInfoMappingAccessor, @NonNull final EligibilityConfig eligibilityConfig,
                                 @NonNull final TNCService tncService,
                                 @NonNull final EligibilityComponent eligibilityComponent ,
                                 @NonNull final ImsUserUtils imsUserUtils) {
        this.transunionDataComponent = transunionDataComponent;
        this.eligibilityProvider = eligibilityProvider;
        this.cibilInfoAccessor = cibilInfoAccessor;
        this.cibilTransformationService = cibilTransformationService;
        this.customerInfoMappingAccessor = customerInfoMappingAccessor;
        this.refreshDays = eligibilityConfig.getRefreshDays();
        this.tncService = tncService;
        this.eligibilityComponent = eligibilityComponent;
        this.imsUserUtils = imsUserUtils;
    }

    /**
     * This method fetches the latest data from Database and returns the extracted required data.
     *
     * @param clientId On-boarded ClientId from Headers.
     * @param purpose  On-boarded Clients Purpose from Headers.
     * @return {@link TransUnionDataFetchResponse}.
     */
    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.FETCH_LATEST_RECORD_FROM_DB_API_INTERNAL)
    public ServiceResponse<TransUnionDataFetchResponse> fetchLatestInternal(
            @RequestHeader(CLIENT_KEY) @Valid final String clientId,
            @RequestHeader(PURPOSE_KEY) @Valid final String purpose,
            @RequestParam("imsId") @Valid @NotBlank(message = IMS_ID_NOT_SUPPLIED) final String imsId) {
        MDC.put("imsId", imsId);
        validateImsId(imsId);

        String mobileNumber = getMobileNumber(imsId);

        final TransUnionDataFetchRequest transUnionDataFetchRequest = TransUnionDataFetchRequest.builder()
                .action(UserAction.NO_ACTION)
                .mobileNumber(mobileNumber)
                .userId(imsId)
                .build();

        log.info("TransUnion Data Fetch Client Request : {}" , transUnionDataFetchRequest);

        log.info("fetching latest data from DB for imsId {}",transUnionDataFetchRequest.getUserId());
        final TransUnionDataFetchResponse response = transunionDataComponent
                .fetchLatestRecord(transUnionDataFetchRequest, UserType.USER.getUserType(),mobileNumber ,"" );

        List<CibilVariable> cibilVariableList = response.getVariables();
        cibilVariableList.remove(1);

        response.setVariables(cibilVariableList);

        log.info("Latest Data fetched from Data Base for imsId {}",transUnionDataFetchRequest.getUserId());
        MDC.remove("imsId");

        return new ServiceResponse<TransUnionDataFetchResponse>(response, true);
    }

    /**
     * This method fetches the Record State of user if exist or not or if it was a no hit.
     *
     * @return {@link RecordStateResponse}.
     */
    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.RECORD_EXISTENCE_CHECK_API)
    public ServiceResponse<RecordStateResponse> fetchRecordState(@RequestParam("imsId") @NotBlank(message = IMS_ID_NOT_SUPPLIED) final String imsId) {
        MDC.put("imsId", imsId);
        validateImsId(imsId);
        log.info("fetching state data from DB for imsId {}", imsId);
        final RecordStateResponse response = transunionDataComponent.fetchRecordState(imsId);
        log.info("Record State fetched from Data Base for imsId {}", imsId);
        MDC.remove("imsId");
        return new ServiceResponse<RecordStateResponse>(response, true);
    }

    /**
     * This method returns the user eligibility for cibil.
     * With Eligibility its also returns the last score and its fetch date.
     *
     * @return {@link ServiceResponse<EligibilityResponse>}.
     */
    @Logged
    @Timed
    @Marked
    @GetMapping(value = ELIGIBILITY_API_INTERNAL)
    public ServiceResponse<EligibilityResponse> eligible(
            @RequestParam(value = "deviceBindingId", required = false) final String deviceBindingId,
            @RequestParam("imsId") @Valid @NotBlank(message = IMS_ID_NOT_SUPPLIED) final String imsId) {
        MDC.put("imsId", imsId);
        validateImsId(imsId);
        final ServiceResponse<EligibilityResponse> response = new ServiceResponse<>();
        String mobileNumber = getMobileNumber(imsId);
        EligibilityResponseV2 eligibilityResponse = eligibilityComponent.getCibilEligibility(imsId, UserType.USER.getUserType(),mobileNumber ,deviceBindingId, BureauType.CIBIL.getBureauType());
        response.setData(parseEligibilityResponse(eligibilityResponse));
        MDC.remove("imsId");
        return response;
    }

    private void validateImsId(final String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new ValidationException(IMS_ID_NOT_SUPPLIED);
        }
    }

    private EligibilityResponse parseEligibilityResponse(EligibilityResponseV2 eligibilityResponse) {
        return JsonUtil.parseObject(eligibilityResponse, EligibilityResponse.class);
    }

    private String getMobileNumber(String imsId){
        String mobileNumber = StringUtils.EMPTY;
        try{
            Optional<CustomerInfo> customerInfoOptional = customerInfoMappingAccessor.getRecordForUserId(imsId);
            if(customerInfoOptional.isPresent() && StringUtils.isNotBlank(customerInfoOptional.get().getMobileNumber())) {
                mobileNumber = customerInfoOptional.get().getMobileNumber();
                log.info("Mobile Number fetched from Db : {}" , mobileNumber);
            }else {
                mobileNumber = imsUserUtils.getUserDetails(imsId).getMobileNumber();
                log.info("Mobile Number fetched from IMS api : {}" , mobileNumber);
            }
        }catch (Exception e){
            log.info("Exception while fetching the mobile Number for imsId {}" , imsId);
            e.printStackTrace();
        }
        return mobileNumber;
    }
}