package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotation.Eligibility;
import com.freecharge.cibil.annotation.MerchantValidator;
import com.freecharge.cibil.annotation.impl.MerchantValidatorAspect;
import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.component.CibilComponent;
import com.freecharge.cibil.constants.ApiUrl;
import com.freecharge.cibil.model.pojo.MerchantInfoResponse;
import com.freecharge.cibil.model.request.VerifyAuthenticationAnswersRequest;
import com.freecharge.cibil.model.response.*;
import com.freecharge.cibil.provider.CustomerIdentifierProvider;
import com.freecharge.fctoken.annotation.Tokenizer;
import com.freecharge.fctoken.context.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.freecharge.cibil.constants.HeaderConstants.CLIENT_KEY;
import static com.freecharge.cibil.constants.HeaderConstants.PURPOSE_KEY;
import static com.freecharge.cibil.utility.ContextDataExtractorUtility.*;

/**
 * Controller for User Merchant TU Authentication
 */
@Slf4j
@RestController
public class CibilAuthenticationController {

    private final CibilComponent cibilSyncComponent;

    private final AuthorizationContext context;

    private final CustomerIdentifierProvider customerIdentifierProvider;

    @Autowired
    public CibilAuthenticationController(@NonNull final CibilComponent cibilSyncComponent,
                                         @Qualifier("TokenizerAuthorizationContext") @NonNull final AuthorizationContext context,
                                         @NonNull final CustomerIdentifierProvider customerIdentifierProvider) {
        this.cibilSyncComponent = cibilSyncComponent;
        this.context = context;
        this.customerIdentifierProvider = customerIdentifierProvider;
    }

    @Logged
    @Timed
    @Marked
    @Tokenizer(validateCSRF = true)
    @GetMapping(value = ApiUrl.GET_AUTHENTICATION_QUESTIONS)
    @Eligibility
    public ServiceResponse<AuthenticationQuestionResponse> getAuthenticationQuestion(
            @RequestHeader(CLIENT_KEY) @Valid final String clientId,
            @RequestHeader(PURPOSE_KEY) @Valid final String purpose,
            @RequestParam(value = "fcChannel", required = false) final String fcChannel,
            @RequestParam(value = "fcversion", required = false) final String fcVersion) {
        final String mobileNumber = extractPhoneFromAuthorizationContext(context);
        final String userId = extractIMSFromAuthorizationContext(context);
        log.info("GetAuthenticationQuestion : Start with for userId {} and mobileNumber {}", userId, mobileNumber);
        final AuthenticationQuestionResponse authenticationQuestionResponse = cibilSyncComponent.getAuthenticationQuestion(userId, mobileNumber,customerIdentifierProvider,fcChannel,fcVersion);
        log.info("GetAuthenticationQuestion : END with response {} for userId {} and mobileNumber {}", authenticationQuestionResponse, userId, mobileNumber);
        return new ServiceResponse<>(authenticationQuestionResponse, true);
    }

    @Logged
    @Timed
    @Marked
    @MerchantValidator
    @GetMapping(value = ApiUrl.MERCHANT_GET_AUTHENTICATION_QUESTIONS)
    public ServiceResponse<AuthenticationQuestionResponse> getAuthenticationQuestionMerchant(
            @RequestHeader(CLIENT_KEY) @Valid final String clientId,
            @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {
        MerchantInfoResponse merchantInfoResponse = MerchantValidatorAspect.getMerchantDetails();
        final String userId = merchantInfoResponse.getMerchantId();
        final String mobileNumber = merchantInfoResponse.getMobileNumber();
        log.info("GetAuthenticationQuestion : Start with for userId {} and mobileNumber {}", userId, mobileNumber);
        final AuthenticationQuestionResponse authenticationQuestionResponse = cibilSyncComponent.getAuthenticationQuestion(userId, mobileNumber,customerIdentifierProvider,"","");
        log.info("GetAuthenticationQuestion : END with response {} for userId {} and mobileNumber {}", authenticationQuestionResponse, userId, mobileNumber);
        return new ServiceResponse<>(authenticationQuestionResponse, true);
    }

    @Logged
    @Timed
    @Marked
    @Tokenizer(validateCSRF = true)
    @PostMapping(value = ApiUrl.VERIFY_AUTH_ANSWERS)
    @Eligibility
    public ServiceResponse<VerifyAuthenticationAnswersResponse> verifyAuthenticationAnswers(
            @RequestBody @Valid VerifyAuthenticationAnswersRequest verifyAuthenticationAnswersRequest,
            @RequestHeader(CLIENT_KEY) @Valid final String clientId,
            @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {
        final String mobileNumber = extractPhoneFromAuthorizationContext(context);
        final String userId = extractIMSFromAuthorizationContext(context);
        log.info("VerifyAuthenticationAnswers : Start with request {} for userId {} and mobileNumber {}", verifyAuthenticationAnswersRequest, userId, mobileNumber);
        final VerifyAuthenticationAnswersResponse response = cibilSyncComponent
                .verifyAuthenticationAnswers(userId, mobileNumber, customerIdentifierProvider, verifyAuthenticationAnswersRequest);
        log.info("VerifyAuthenticationAnswers : End with response {} for userId {} and mobileNumber {}", response, userId, mobileNumber);
        return new ServiceResponse<>(response, true);
    }

    @Logged
    @Timed
    @Marked
    @MerchantValidator
    @PostMapping(value = ApiUrl.MERCHANT_VERIFY_AUTH_ANSWERS)
    public ServiceResponse<VerifyAuthenticationAnswersResponse> verifyAuthenticationAnswersMerchant(
            @RequestBody @Valid VerifyAuthenticationAnswersRequest verifyAuthenticationAnswersRequest,
            @RequestHeader(CLIENT_KEY) @Valid final String clientId,
            @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {
        MerchantInfoResponse merchantInfoResponse = MerchantValidatorAspect.getMerchantDetails();
        final String userId = merchantInfoResponse.getMerchantId();
        final String mobileNumber = merchantInfoResponse.getMobileNumber();
        log.info("VerifyAuthenticationAnswers : Start with request {} for userId {} and mobileNumber {}", verifyAuthenticationAnswersRequest, userId, mobileNumber);
        final VerifyAuthenticationAnswersResponse response = cibilSyncComponent
                .verifyAuthenticationAnswers(userId, mobileNumber, customerIdentifierProvider, verifyAuthenticationAnswersRequest);
        log.info("VerifyAuthenticationAnswers : End with response {} for userId {} and mobileNumber {}", response, userId, mobileNumber);
        return new ServiceResponse<>(response, true);
    }
}