package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotation.Eligibility;
import com.freecharge.cibil.annotation.MerchantValidator;
import com.freecharge.cibil.annotation.impl.MerchantValidatorAspect;
import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.component.TransunionDataComponent;
import com.freecharge.cibil.constants.ApiUrl;
import com.freecharge.cibil.model.enums.UserAction;
import com.freecharge.cibil.model.pojo.MerchantInfoResponse;
import com.freecharge.cibil.model.request.TransUnionDataFetchRequest;
import com.freecharge.cibil.model.response.ServiceResponse;
import com.freecharge.cibil.model.response.TransUnionDataFetchResponse;
import com.freecharge.cibil.model.response.TransUnionDataFetchResponseV2;
import com.freecharge.cibil.provider.CustomerIdentifierProvider;
import com.freecharge.fctoken.annotation.Tokenizer;
import com.freecharge.fctoken.context.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.freecharge.cibil.constants.HeaderConstants.CLIENT_KEY;
import static com.freecharge.cibil.constants.HeaderConstants.PURPOSE_KEY;
import static com.freecharge.cibil.utility.ContextDataExtractorUtility.*;

/**
 * Controller for User Merchant Cibil Info form TU
 */
@Log4j2
@RestController
public class TransunionDataController {

    private final TransunionDataComponent transunionDataComponent;

    private final AuthorizationContext context;

    private final CustomerIdentifierProvider customerIdentifierProvider;

    @Autowired
    public TransunionDataController(@NonNull final TransunionDataComponent transunionDataComponent,
                                    @Qualifier("TokenizerAuthorizationContext") @NonNull final AuthorizationContext context,
                                    @NonNull final CustomerIdentifierProvider customerIdentifierProvider) {
        this.transunionDataComponent = transunionDataComponent;
        this.context = context;
        this.customerIdentifierProvider = customerIdentifierProvider;
    }

    /**
     * This method fetches the latest data of User from Database and returns the extracted required data.
     *
     * @param clientId  On-boarded ClientId from Headers.
     * @param purpose   On-boarded Clients Purpose from Headers.
     * @param action    for the TnC status change request.
     * @param fcChannel value come from the different api channel.
     * @return {@link ServiceResponse<TransUnionDataFetchResponse>}.
     */
    @Logged
    @Timed
    @Marked
    @Tokenizer(validateCSRF = true)
    @GetMapping(value = ApiUrl.FETCH_LATEST_RECORD_FROM_DB_API)
    public ServiceResponse<TransUnionDataFetchResponse> fetchLatestRecord(@RequestHeader(CLIENT_KEY) @Valid final String clientId,
                                                                          @RequestHeader(PURPOSE_KEY) @Valid final String purpose,
                                                                          @RequestParam(value = "action", required = false) final String action,
                                                                          @RequestParam(value = "fcChannel", required = false) final String fcChannel) {
        final TransUnionDataFetchRequest transUnionDataFetchRequest = TransUnionDataFetchRequest.builder()
                .build();
        if (StringUtils.isBlank(action)) {
            transUnionDataFetchRequest.setAction(UserAction.NO_ACTION);
        } else {
            transUnionDataFetchRequest.setAction(UserAction.enumOf(action));
        }
        final String userId = extractIMSFromAuthorizationContext(context);
        final String usertype = extractUserTypeFromAuthorizationContext(context);
        final String mobileNumber = extractPhoneFromAuthorizationContext(context);
        transUnionDataFetchRequest.setUserId(userId);
        log.info("Fetching latest data from DB for userId {} and mobileNumber {}", transUnionDataFetchRequest.getUserId(), mobileNumber);
        final TransUnionDataFetchResponse response = transunionDataComponent
                .fetchLatestRecord(transUnionDataFetchRequest, usertype, mobileNumber, fcChannel);
        log.info("Latest Data fetched from DB for userId {} and mobileNumber {} and response {}", transUnionDataFetchRequest.getUserId(), mobileNumber, response);
        return new ServiceResponse<TransUnionDataFetchResponse>(response, true);
    }

    /**
     * This method fetches the latest data of Merchant from Database and returns the extracted required data.
     *
     * @param clientId  On-boarded ClientId from Headers.
     * @param purpose   On-boarded Clients Purpose from Headers.
     * @param action    for the TnC status change request.
     * @param fcChannel value come from the different api channel.
     * @return {@link ServiceResponse<TransUnionDataFetchResponse>}.
     */
    @Logged
    @Timed
    @Marked
    @MerchantValidator
    @GetMapping(value = ApiUrl.FETCH_LATEST_RECORD_FROM_DB_API_MERCHANT)
    public ServiceResponse<TransUnionDataFetchResponse> fetchLatestRecordMerchant(@RequestHeader(CLIENT_KEY) @Valid final String clientId,
                                                                                  @RequestHeader(PURPOSE_KEY) @Valid final String purpose,
                                                                                  @RequestParam(value = "action", required = false) final String action,
                                                                                  @RequestParam(value = "fcChannel", required = false) final String fcChannel) {
        final TransUnionDataFetchRequest transUnionDataFetchRequest = TransUnionDataFetchRequest.builder()
                .build();
        if (StringUtils.isBlank(action)) {
            transUnionDataFetchRequest.setAction(UserAction.NO_ACTION);
        } else {
            transUnionDataFetchRequest.setAction(UserAction.enumOf(action));
        }
        MerchantInfoResponse merchantInfoResponse = MerchantValidatorAspect.getMerchantDetails();
        transUnionDataFetchRequest.setUserId(merchantInfoResponse.getMerchantId());
        log.info("fetching latest data from DB for userId {}", transUnionDataFetchRequest.getUserId());
        final TransUnionDataFetchResponse response = transunionDataComponent
                .fetchLatestRecord(transUnionDataFetchRequest, merchantInfoResponse.getUserType(), merchantInfoResponse.getMobileNumber(), fcChannel);
        log.info("Latest Data fetched from Data Base for imsId {}", transUnionDataFetchRequest.getUserId());
        return new ServiceResponse<TransUnionDataFetchResponse>(response, true);
    }


    /**
     * This method fetches the data from Transunion Service and returns the extracted required data.
     *
     * @param clientId  On-boarded ClientId from Headers.
     * @param purpose   On-boarded Clients Purpose from Headers.
     * @param fcChannel value come from the different api channel.
     * @return {@link ServiceResponse<TransUnionDataFetchResponse>}.
     */
    @Logged
    @Timed
    @Marked
    @Tokenizer(validateCSRF = true)
    @Eligibility
    @GetMapping(value = ApiUrl.FETCH_DATA_FROM_TU_API)
    public ServiceResponse<TransUnionDataFetchResponse> fetchDataFromTU(@RequestHeader(CLIENT_KEY) @Valid final String clientId,
                                                                        @RequestHeader(PURPOSE_KEY) @Valid final String purpose,
                                                                        @RequestParam(value = "fcChannel", required = false) final String fcChannel) {
        final TransUnionDataFetchRequest transUnionDataFetchRequest = TransUnionDataFetchRequest.builder().build();
        final String phone = extractPhoneFromAuthorizationContext(context);
        final String userId = extractIMSFromAuthorizationContext(context);
        final String userType = extractUserTypeFromAuthorizationContext(context);
        final String customerIdentifier = customerIdentifierProvider.getPccId(phone);
        transUnionDataFetchRequest.setCustomerIdentifier(customerIdentifier);
        transUnionDataFetchRequest.setUserId(userId);
        log.info("Fetching data from transunion Service with request {} for userId {} and mobileNumber{}", transUnionDataFetchRequest, userId, phone);
        final TransUnionDataFetchResponse response = transunionDataComponent
                .fetchCibilData(transUnionDataFetchRequest, phone, clientId, purpose, userType, fcChannel);
        log.info("Data fetched from Transunion Service with request {} for userId {} and mobileNumber {} and response {}", transUnionDataFetchRequest, userId, phone, response);
        return new ServiceResponse<TransUnionDataFetchResponse>(response, true);
    }

    /**
     * This method run the reverification of the User then fetches the latest data from Transunion Service and returns the extracted required data.
     *
     * @param clientId  On-boarded ClientId from Headers.
     * @param purpose   On-boarded Clients Purpose from Headers.
     * @param fcChannel value come from the different api channel.
     * @return {@link ServiceResponse<TransUnionDataFetchResponseV2>}.
     */
    @Logged
    @Timed
    @Marked
    @Tokenizer(validateCSRF = true)
    @Eligibility
    @GetMapping(value = ApiUrl.FETCH_DATA_FROM_TU_API_V2)
    public ServiceResponse<TransUnionDataFetchResponseV2> fetchDataFromTuV2(@RequestHeader(CLIENT_KEY) @Valid final String clientId,
                                                                            @RequestHeader(PURPOSE_KEY) @Valid final String purpose,
                                                                            @RequestParam(value = "fcChannel", required = false) final String fcChannel) {
        final TransUnionDataFetchRequest transUnionDataFetchRequest = TransUnionDataFetchRequest.builder().build();
        final String phone = extractPhoneFromAuthorizationContext(context);
        final String userId = extractIMSFromAuthorizationContext(context);
        final String customerIdentifier = customerIdentifierProvider.getPccId(phone);
        final String userType = extractUserTypeFromAuthorizationContext(context);
        transUnionDataFetchRequest.setCustomerIdentifier(customerIdentifier);
        transUnionDataFetchRequest.setUserId(userId);
        log.info(" FetchDataFromTuV2: Fetching data from transunion Service for request {} with userId {} and mobileNumber {}", transUnionDataFetchRequest);
        final TransUnionDataFetchResponseV2 response = transunionDataComponent
                .getTransUnionDataFetchResponseV2(transUnionDataFetchRequest, phone, clientId, purpose, userType, fcChannel);
        log.info("FetchDataFromTuV2 :Data fetched from Transunion Service for request {} with userId {} and mobileNumber {} and response {}", transUnionDataFetchRequest, userId, phone, response);
        return new ServiceResponse<TransUnionDataFetchResponseV2>(response, true);
    }

    /**
     * This method run the reverification of the Merchant then fetches the latest data from Transunion Service and returns the extracted required data.
     *
     * @param clientId  On-boarded ClientId from Headers.
     * @param purpose   On-boarded Clients Purpose from Headers.
     * @param fcChannel value come from the different api channel.
     * @return {@link ServiceResponse<TransUnionDataFetchResponseV2>}.
     */
    @Logged
    @Timed
    @Marked
    @MerchantValidator
    @GetMapping(value = ApiUrl.FETCH_DATA_FROM_TU_API_V2_MERCHANT)
    public ServiceResponse<TransUnionDataFetchResponseV2> fetchDataFromTuV2Merchant(@RequestHeader(CLIENT_KEY) @Valid final String clientId,
                                                                                    @RequestHeader(PURPOSE_KEY) @Valid final String purpose,
                                                                                    @RequestParam(value = "fcChannel", required = false) final String fcChannel) {
        final TransUnionDataFetchRequest transUnionDataFetchRequest = TransUnionDataFetchRequest.builder().build();
        MerchantInfoResponse merchantInfoResponse = MerchantValidatorAspect.getMerchantDetails();
        final String customerIdentifier = customerIdentifierProvider.getCustomerPccId(merchantInfoResponse.getMerchantId(), merchantInfoResponse.getMobileNumber());
        transUnionDataFetchRequest.setCustomerIdentifier(customerIdentifier);
        transUnionDataFetchRequest.setUserId(merchantInfoResponse.getMerchantId());
        log.info(" fetchDataFromTuV2: fetching data from transunion Service for request {}", transUnionDataFetchRequest);
        final TransUnionDataFetchResponseV2 response = transunionDataComponent
                .getTransUnionDataFetchResponseV2(transUnionDataFetchRequest, merchantInfoResponse.getMobileNumber(), clientId, purpose, merchantInfoResponse.getUserType(), fcChannel);
        log.info("fetchDataFromTuV2 :Data fetched from Transunion Service for response {}", transUnionDataFetchRequest);
        return new ServiceResponse<TransUnionDataFetchResponseV2>(response, true);
    }
}
