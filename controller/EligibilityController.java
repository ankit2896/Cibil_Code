package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotation.MerchantValidator;
import com.freecharge.cibil.annotation.impl.MerchantValidatorAspect;
import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.component.EligibilityComponent;
import com.freecharge.cibil.eligibility.provider.impl.UserEligibilityProvider;
import com.freecharge.cibil.model.pojo.MerchantInfoResponse;
import com.freecharge.cibil.model.response.EligibilityResponse;
import com.freecharge.cibil.model.response.EligibilityResponseV2;
import com.freecharge.cibil.model.response.ServiceResponse;
import com.freecharge.cibil.mysql.accessor.CibilInfoAccessor;
import com.freecharge.cibil.service.CibilTransformationService;
import com.freecharge.experian.enums.BureauType;
import com.freecharge.fctoken.annotation.Tokenizer;
import com.freecharge.fctoken.context.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static com.freecharge.cibil.constants.ApiUrl.*;
import static com.freecharge.cibil.constants.HeaderConstants.DEVICE_BINDING_ID;
import static com.freecharge.cibil.utility.ContextDataExtractorUtility.*;

@Slf4j
@RestController
public class EligibilityController {

    private final UserEligibilityProvider eligibilityProvider;

    private final CibilInfoAccessor cibilInfoAccessor;

    private final CibilTransformationService cibilTransformationService;

    private final AuthorizationContext context;

    private EligibilityComponent eligibilityComponent;

    @Autowired
    public EligibilityController(@NonNull final UserEligibilityProvider eligibilityProvider,
                                 @NonNull final CibilInfoAccessor cibilInfoAccessor,
                                 @NonNull final CibilTransformationService transformationService,
                                 @Qualifier("TokenizerAuthorizationContext") @NonNull final AuthorizationContext context,
                                 @NonNull final EligibilityComponent eligibilityComponent) {
        this.eligibilityProvider = eligibilityProvider;
        this.cibilInfoAccessor = cibilInfoAccessor;
        this.cibilTransformationService = transformationService;
        this.context = context;
        this.eligibilityComponent = eligibilityComponent;
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
    @Tokenizer(validateCSRF = true)
    @GetMapping(value = ELIGIBILITY_API)
    public ServiceResponse<EligibilityResponseV2> eligible(
            @RequestHeader(DEVICE_BINDING_ID) final String deviceBindingId) {
        final ServiceResponse<EligibilityResponseV2> response = new ServiceResponse<>();
        final String userType = extractUserTypeFromAuthorizationContext(context);
        String userId = extractIMSFromAuthorizationContext(context);
        String phoneNumber = extractPhoneFromAuthorizationContext(context);
        log.info("Eligible Started for userId {}, userType {}, mobileNumber {}", userId, userType, phoneNumber);
        EligibilityResponseV2 eligibilityResponse = eligibilityComponent.getEligibilityWithTncData(userId, userType, phoneNumber, deviceBindingId, BureauType.CIBIL.getBureauType());
        response.setData(eligibilityResponse);
        log.info("Eligible response for userId and mobileNumber {} and {} is {}", userId, phoneNumber,response);
        return response;
    }

    @Logged
    @Timed
    @Marked
    @MerchantValidator
    @GetMapping(value = MERCHANT_ELIGIBILITY_API)
    public ServiceResponse<EligibilityResponseV2> merchantEligible(
            @RequestHeader(DEVICE_BINDING_ID) final String deviceBindingId) {
        final ServiceResponse<EligibilityResponseV2> response = new ServiceResponse<>();
        MerchantInfoResponse merchantInfoResponse = MerchantValidatorAspect.getMerchantDetails();
        log.info("Eligible Started for userId {}, userType {}, mobileNumber {}", merchantInfoResponse.getMerchantId(), merchantInfoResponse.getUserType(), merchantInfoResponse.getMobileNumber());
        EligibilityResponseV2 eligibilityResponse = eligibilityComponent.getEligibilityWithTncData(merchantInfoResponse.getMerchantId(), merchantInfoResponse.getUserType(), merchantInfoResponse.getMobileNumber(), deviceBindingId,BureauType.CIBIL.getBureauType());
        response.setData(eligibilityResponse);
        log.info("Eligible response for userId {} is {}", merchantInfoResponse.getMerchantId(), response);
        return response;
    }
}
