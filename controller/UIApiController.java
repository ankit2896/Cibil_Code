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
import com.freecharge.cibil.model.request.CreateFulfillOfferRequest;
import com.freecharge.cibil.model.request.CreateFulfillOfferRequestExpressSearch;
import com.freecharge.cibil.model.response.CreateFulfillmentResponse;
import com.freecharge.cibil.model.response.ServiceResponse;
import com.freecharge.cibil.provider.CustomerIdentifierProvider;
import com.freecharge.fctoken.annotation.Tokenizer;
import com.freecharge.fctoken.context.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.freecharge.cibil.constants.HeaderConstants.CLIENT_KEY;
import static com.freecharge.cibil.constants.HeaderConstants.PURPOSE_KEY;
import static com.freecharge.cibil.utility.ContextDataExtractorUtility.*;

/**
 * Controller for CreateFulfillment status of User Merchant
 */
@Log4j2
@RestController
public class UIApiController {

    private final CibilComponent cibilComponent;

    private final AuthorizationContext context;

    private final CustomerIdentifierProvider customerIdentifierProvider;

    @Autowired
    public UIApiController(@NonNull final CibilComponent cibilComponent,
                           @NonNull final AuthorizationContext authorizationContext,
                           @NonNull final CustomerIdentifierProvider customerIdentifierProvider) {
        this.cibilComponent = cibilComponent;
        this.context = authorizationContext;
        this.customerIdentifierProvider = customerIdentifierProvider;
    }

    /**
     * This method return the User fulfillment status of the request comes from
     * the TU for Short Authentication process.
     *
     * @param createFulfillOfferRequest Fulfillment request from client
     * @param clientId On-boarded ClientId from Headers.
     * @param purpose  On-boarded Clients Purpose from Headers.
     *
     * @return {@link ServiceResponse<CreateFulfillmentResponse>}
     */
    @Logged
    @Timed
    @Marked
    @Tokenizer(validateCSRF = false)
    @Eligibility
    @PostMapping(value = ApiUrl.CREATE_FULFILL_OFFER_EXPRESS_SEARCH)
    public ServiceResponse<CreateFulfillmentResponse> initiateTransunionExpressSearchProcess(
            @RequestBody @Valid final CreateFulfillOfferRequestExpressSearch createFulfillOfferRequest,
            @RequestHeader(CLIENT_KEY) @Valid final String clientId,
            @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {
        final String mobileNumber = extractPhoneFromAuthorizationContext(context);
        final String userId = extractIMSFromAuthorizationContext(context);
        final String userType = extractUserTypeFromAuthorizationContext(context);
        log.info("InitiateTransunionExpressSearchProcess started for userId: {}, mobileNumber: {}", userId, mobileNumber);
        CreateFulfillmentResponse response = cibilComponent.transunionProcessInitiateExpressSearch(createFulfillOfferRequest, clientId, purpose, mobileNumber, userId, userType, customerIdentifierProvider);
        log.info("InitiateTransunionExpressSearchProcess completed for userId {} and mobileNumber {} with response {}", userId,mobileNumber,response);
        return new ServiceResponse<CreateFulfillmentResponse>(response, true);
    }

    /**
     * This method return the User fulfillment status of the request comes from
     * the TU for Long Authentication process.
     *
     * @param createFulfillOfferRequest Fulfillment request from client
     * @param clientId On-boarded ClientId from Headers.
     * @param purpose  On-boarded Clients Purpose from Headers.
     *
     * @return {@link ServiceResponse<CreateFulfillmentResponse>}
     */
    @Logged
    @Timed
    @Marked
    @Tokenizer(validateCSRF = false)
    @Eligibility
    @PostMapping(value = ApiUrl.CREATE_FULFILL_OFFER)
    public ServiceResponse<CreateFulfillmentResponse> initiateTransunionProcess(
            @RequestBody @Valid final CreateFulfillOfferRequest createFulfillOfferRequest,
            @RequestHeader(CLIENT_KEY) @Valid final String clientId,
            @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {

        String mobileNumber = extractPhoneFromAuthorizationContext(context);
        final String userId = extractIMSFromAuthorizationContext(context);
        final String userType = extractUserTypeFromAuthorizationContext(context);
        log.info("InitiateTransunionProcess started for userId {} and mobileNumber{}", userId, mobileNumber);
        final CreateFulfillmentResponse response = cibilComponent
                .transunionProcessInitiate(createFulfillOfferRequest, clientId, purpose, mobileNumber, userId, userType, customerIdentifierProvider);
        log.info("InitiateTransunionProcess completed for userId {} and mobileNumber {} with response {}", userId,mobileNumber,response);
        return new ServiceResponse<CreateFulfillmentResponse>(response, true);
    }

    /**
     * This method return the Merchant fulfillment status of the request comes from
     * the TU for Long Authentication process.
     *
     * @param createFulfillOfferRequest Fulfillment request from client
     * @param clientId On-boarded ClientId from Headers.
     * @param purpose  On-boarded Clients Purpose from Headers.
     *
     * @return {@link ServiceResponse<CreateFulfillmentResponse>}
     */
    @Logged
    @Timed
    @Marked
    @MerchantValidator
    @PostMapping(value = ApiUrl.MERCHANT_CREATE_FULFILL_OFFER)
    public ServiceResponse<CreateFulfillmentResponse> merchantInitiateTransunionProcess(
            @RequestBody @Valid final CreateFulfillOfferRequest createFulfillOfferRequest,
            @RequestHeader(CLIENT_KEY) @Valid final String clientId,
            @RequestHeader(PURPOSE_KEY) @Valid final String purpose) {

        MerchantInfoResponse merchantInfoResponse = MerchantValidatorAspect.getMerchantDetails();
        log.info("User Type : {}", merchantInfoResponse.getUserType());
        final CreateFulfillmentResponse response = cibilComponent
                .transunionProcessInitiate(createFulfillOfferRequest, clientId, purpose, merchantInfoResponse.getMobileNumber(), merchantInfoResponse.getMerchantId(), merchantInfoResponse.getUserType(), customerIdentifierProvider);
        log.info("initiateTransunionProcess completed for userId {}", createFulfillOfferRequest.getUserId());
        return new ServiceResponse<CreateFulfillmentResponse>(response, true);
    }
}
