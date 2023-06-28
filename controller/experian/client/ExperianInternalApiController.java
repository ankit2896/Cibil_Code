package com.freecharge.cibil.controller.experian.client;

import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.component.EligibilityComponent;
import com.freecharge.cibil.component.ExperianDataComponent;
import com.freecharge.cibil.constants.ApiUrl;
import com.freecharge.cibil.model.enums.UserAction;
import com.freecharge.cibil.model.enums.UserType;
import com.freecharge.cibil.model.response.EligibilityResponseV2;
import com.freecharge.cibil.model.response.ServiceResponse;
import com.freecharge.cibil.model.response.TransUnionDataFetchResponse;
import com.freecharge.experian.enums.BureauType;
import com.freecharge.experian.model.request.ExperianDataFetchRequest;
import com.freecharge.experian.model.response.ExperianDataFetchResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import static com.freecharge.cibil.constants.ApiUrl.*;
import static com.freecharge.cibil.constants.HeaderConstants.DEVICE_BINDING_ID;

@Slf4j
@RestController
@Validated
public class ExperianInternalApiController {

    private EligibilityComponent eligibilityComponent;

    private final ExperianDataComponent experianDataComponent;

    @Autowired
    public ExperianInternalApiController(@NonNull final EligibilityComponent eligibilityComponent,
                                         @NonNull final ExperianDataComponent experianDataComponent) {
        this.eligibilityComponent = eligibilityComponent;
        this.experianDataComponent = experianDataComponent;
    }

    /**
     * This method returns the user eligibility for experian.
     * With Eligibility its also returns the last score and its fetch date.
     *
     * @return {@link ServiceResponse < EligibilityResponse >}.
     */
    @Logged
    @Timed
    @Marked
    @GetMapping(value = EXPERIAN_ELIGIBILITY_API)
    public ServiceResponse<EligibilityResponseV2> eligible(
            @RequestParam(value = "userId") @Valid @NotBlank final String userId,
            @RequestParam(value = "mobileNumber") @Valid @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$") final String mobileNumber) {
        final ServiceResponse<EligibilityResponseV2> response = new ServiceResponse<>();

        log.info("Eligible Started for userId {}, userType {}, mobileNumber {}", userId, UserType.USER.getUserType(), mobileNumber);
        EligibilityResponseV2 eligibilityResponse = eligibilityComponent.getEligibilityWithTncData(userId, UserType.USER.getUserType(), mobileNumber, null, BureauType.EXPERIAN.getBureauType());
        response.setData(eligibilityResponse);

        log.info("Eligible response for userId and mobileNumber {} and {} is {}", userId, mobileNumber, response);
        return response;
    }

    /**
     * This method fetches the latest data from Database and returns the extracted required data.
     * @param userId
     * @param mobileNumber
     * @return {@link ExperianDataFetchResponse}.
     */
    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.FETCH_EXPERIAN_LATEST_RECORD_FROM_DB_API)
    public ServiceResponse<ExperianDataFetchResponse> fetchLatestRecord(@RequestParam(value = "userId") @Valid @NotBlank final String userId,
                                                                        @RequestParam(value = "mobileNumber") @Valid @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$") final String mobileNumber) {

        final ExperianDataFetchRequest experianDataFetchRequest = ExperianDataFetchRequest.builder()
                .action(UserAction.NO_ACTION)
                .build();

        experianDataFetchRequest.setUserId(userId);
        experianDataFetchRequest.setMobileNumber(mobileNumber);

        log.info("Fetching latest data from DB for userId {} and mobileNumber {}", experianDataFetchRequest.getUserId(), mobileNumber);

        final ExperianDataFetchResponse response = experianDataComponent
                .fetchLatestRecord(experianDataFetchRequest, UserType.USER.getUserType(),"");

        log.info("Latest Data fetched from DB for userId {} and mobileNumber {} and response {}", experianDataFetchRequest.getUserId(), mobileNumber, response);
        return new ServiceResponse<>(response, true);
    }
}
