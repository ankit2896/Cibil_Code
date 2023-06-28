package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotation.MerchantValidator;
import com.freecharge.cibil.annotation.impl.MerchantValidatorAspect;
import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.component.KycInfoComponent;
import com.freecharge.cibil.constants.ApiUrl;
import com.freecharge.cibil.model.pojo.MerchantInfoResponse;
import com.freecharge.cibil.model.response.KycInfoResponse;
import com.freecharge.cibil.model.response.ServiceResponse;
import com.freecharge.fctoken.annotation.Tokenizer;
import com.freecharge.fctoken.context.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller for User Merchant Kyc Info
 */
@Slf4j
@RestController
public class KycInfoController {

    private final AuthorizationContext context;

    private final KycInfoComponent kycInfoComponent;

    @Autowired
    public KycInfoController(@NonNull final KycInfoComponent kycInfoComponent,
                             @Qualifier("TokenizerAuthorizationContext") final AuthorizationContext context) {
        this.kycInfoComponent = kycInfoComponent;
        this.context = context;
    }

    /**
     * This Method Retrieves the users kycInformation like name, pan details and email of the user,
     * and returns them.
     *
     * @return {@link ServiceResponse<KycInfoResponse>}
     */
    @Logged
    @Timed
    @Marked
    @Tokenizer(validateCSRF = true)
    @GetMapping(value = ApiUrl.GET_KYC_INFO_API_URI)
    public ServiceResponse<KycInfoResponse> getUserKycInfo() {
        final ServiceResponse<KycInfoResponse> response = new ServiceResponse<>(null, true);
        KycInfoResponse kycInfoResponse = kycInfoComponent.getKycInfo(context);
        response.setData(kycInfoResponse);
        log.info("Get kyc info api response : {}", response);
        return response;
    }

    /**
     * This Method Retrieves the users kycInformation like name, pan details, email etc of the merchant,
     * and returns them.
     *
     * @return {@link ServiceResponse<MerchantInfoResponse>}
     */
    @Logged
    @Timed
    @Marked
    @MerchantValidator
    @GetMapping(value = ApiUrl.GET_MERCHANT_KYC_INFO_API_URI)
    public ServiceResponse<MerchantInfoResponse> getMerchantKycInfo() {
        final ServiceResponse<MerchantInfoResponse> response = new ServiceResponse<>(null, true);
        MerchantInfoResponse merchantInfoResponse = MerchantValidatorAspect.getMerchantDetails();
        response.setData(merchantInfoResponse);
        log.info("Get kyc info merchant api response : {}", response);
        return response;
    }
}