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
import com.freecharge.cibil.model.response.ReportUrlResponse;
import com.freecharge.cibil.model.response.ReportUrlResponseV2;
import com.freecharge.cibil.model.response.ServiceResponse;
import com.freecharge.cibil.provider.CustomerIdentifierProvider;
import com.freecharge.fctoken.annotation.Tokenizer;
import com.freecharge.fctoken.context.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.freecharge.cibil.constants.HeaderConstants.CLIENT_KEY;
import static com.freecharge.cibil.constants.HeaderConstants.PURPOSE_KEY;
import static com.freecharge.cibil.utility.ContextDataExtractorUtility.extractIMSFromAuthorizationContext;
import static com.freecharge.cibil.utility.ContextDataExtractorUtility.extractPhoneFromAuthorizationContext;

/**
 * Controller for CibilReport Url fetch from TU
 */
@Slf4j
@RestController
public class CibilReportUrlController {

    private final CibilComponent cibilSyncComponent;

    private final AuthorizationContext context;

    private final CustomerIdentifierProvider customerIdentifierProvider;

    @Autowired
    public CibilReportUrlController(@NonNull final CibilComponent cibilSyncComponent,
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
    @GetMapping(value = ApiUrl.GET_REPORT_URL)
    @Eligibility
    public ServiceResponse<ReportUrlResponse> getReportUrl(@RequestHeader(CLIENT_KEY) @Valid final String clientId,
                                                           @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {

        final String mobileNumber = extractPhoneFromAuthorizationContext(context);
        final String userId = extractIMSFromAuthorizationContext(context);
        log.info("GetReportUrl : Start for userId {} and mobileNumber {}", userId, mobileNumber);
        final ReportUrlResponse reportUrlResponse = cibilSyncComponent.getReportUrl(userId, mobileNumber, customerIdentifierProvider);
        log.info("GetReportUrl : End with response {} for userId {} and mobileNumber {}", reportUrlResponse, userId, mobileNumber);
        return new ServiceResponse<>(reportUrlResponse, true);
    }

    @Logged
    @Timed
    @Marked
    @MerchantValidator
    @GetMapping(value = ApiUrl.MERCHANT_GET_REPORT_URL)
    public ServiceResponse<ReportUrlResponse> getReportUrlMerchant(@RequestHeader(CLIENT_KEY) @Valid final String clientId,
                                                                   @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {

        MerchantInfoResponse merchantInfoResponse = MerchantValidatorAspect.getMerchantDetails();
        final String userId = merchantInfoResponse.getMerchantId();
        final String mobileNumber = merchantInfoResponse.getMobileNumber();
        log.info("GetReportUrl : Start for userId {} and mobileNumber {}", userId, mobileNumber);
        log.info("GetReportUrl : Start for userId {} and mobileNumber {}", userId, mobileNumber);
        final ReportUrlResponse reportUrlResponse = cibilSyncComponent.getReportUrl(userId, mobileNumber, customerIdentifierProvider);
        log.info("GetReportUrl : End with response {} for userId {} and mobileNumber {}", reportUrlResponse, userId, mobileNumber);
        return new ServiceResponse<>(reportUrlResponse, true);
    }

    @Logged
    @Timed
    @Marked
    @Tokenizer(validateCSRF = true)
    @GetMapping(value = ApiUrl.GET_REPORT_URL_V2)
    @Eligibility
    public ServiceResponse<ReportUrlResponseV2> getReportUrlV2(@RequestHeader(CLIENT_KEY) @Valid final String clientId,
                                                               @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {
        final String mobileNumber = extractPhoneFromAuthorizationContext(context);
        final String userId = extractIMSFromAuthorizationContext(context);
        log.info("GetReportUrlV2 : Start for userId {} and mobileNumber {}", userId, mobileNumber);
        final ReportUrlResponseV2 reportUrlResponseV2 = cibilSyncComponent.getReportUrlV2(userId, mobileNumber, customerIdentifierProvider);
        log.info("GetReportUrlV2 : End with response {} for userId {} and mobileNumber {}", reportUrlResponseV2, userId, mobileNumber);
        return new ServiceResponse<>(reportUrlResponseV2, true);
    }

    @Logged
    @Timed
    @Marked
    @MerchantValidator
    @GetMapping(value = ApiUrl.MERCHANT_GET_REPORT_URL_V2)
    public ServiceResponse<ReportUrlResponseV2> getReportUrlMerchantV2(@RequestHeader(CLIENT_KEY) @Valid final String clientId,
                                                                       @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {

        MerchantInfoResponse merchantInfoResponse = MerchantValidatorAspect.getMerchantDetails();
        final String userId = merchantInfoResponse.getMerchantId();
        final String mobileNumber = merchantInfoResponse.getMobileNumber();
        log.info("GetReportUrlV2 : Start for userId {} and mobileNumber {}", userId, mobileNumber);
        final ReportUrlResponseV2 reportUrlResponseV2 = cibilSyncComponent.getReportUrlV2(userId, mobileNumber, customerIdentifierProvider);
        log.info("GetReportUrlV2 : End with response {} for userId {} and mobileNumber {}", reportUrlResponseV2, userId, mobileNumber);
        return new ServiceResponse<>(reportUrlResponseV2, true);
    }
}