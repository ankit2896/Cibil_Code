package com.freecharge.cibil.factory;

import com.freecharge.cibil.exceptions.ValidationException;
import com.freecharge.cibil.model.enums.UserType;
import com.freecharge.cibil.model.response.KycInfoResponse;
import com.freecharge.cibil.rest.KycInformationService;
import com.freecharge.cibil.rest.dto.response.NameInformation;
import com.freecharge.cibil.rest.impl.KycInformationServiceImpl;
import com.freecharge.cibil.rest.impl.KycMerchantInfoServiceImpl;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KycInfoFactory {

    private final KycInformationServiceImpl kycInformationService;

    private final KycMerchantInfoServiceImpl kycMerchantInfoServiceImpl;

    @Autowired
    public KycInfoFactory(@NonNull final KycInformationServiceImpl kycInformationService,@NonNull final KycMerchantInfoServiceImpl kycMerchantInfoServiceImpl) {
        this.kycInformationService = kycInformationService;
        this.kycMerchantInfoServiceImpl = kycMerchantInfoServiceImpl;
    }


    public KycInfoResponse getKycInformation(String imsId, String userType) {
        return getInstance(userType).getKycInformation(imsId);
    }

    public NameInformation getNameInformation(@NonNull final String identificationNumber,
                                              @NonNull final String  identificationType,
                                              @NonNull final  String userType) {
        return getInstance(userType).getNameInformation(identificationNumber,
                identificationType);
    }

    private KycInformationService getInstance(@NonNull final String type) {
        switch (UserType.enumOf(type)) {
            case USER:
                return kycInformationService;
            case MERCHANT:
                return kycMerchantInfoServiceImpl;
            default :
                throw new ValidationException("No matching object could be created");
        }
    }
}
