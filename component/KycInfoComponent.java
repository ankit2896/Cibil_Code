package com.freecharge.cibil.component;

import com.freecharge.cibil.factory.KycInfoFactory;
import com.freecharge.cibil.model.enums.NameSourceType;
import com.freecharge.cibil.model.response.KycInfoResponse;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.repository.impl.CustomerInfoRepository;
import com.freecharge.cibil.rest.KycInformationService;
import com.freecharge.cibil.rest.dto.response.NameInformation;
import com.freecharge.fctoken.context.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.freecharge.cibil.utility.ContextDataExtractorUtility.*;
import static com.freecharge.cibil.utility.ContextDataExtractorUtility.extractIMSFromAuthorizationContext;

/**
 * Componenet for KycInfo
 */
@Slf4j
@Component
public class KycInfoComponent {

    private final KycInformationService kycInformationService;

    private final CustomerInfoMappingAccessor customerInfoMappingAccessor;

    private final KycInfoFactory kycInfoFactory;

    private final CustomerInfoRepository customerInfoRepository;

    @Autowired
    public KycInfoComponent(@NonNull final KycInformationService kycInformationService,
                            @NonNull final CustomerInfoMappingAccessor customerInfoMappingAccessor, @NonNull final KycInfoFactory kycInfoFactory, @NonNull final CustomerInfoRepository customerInfoRepository) {
        this.kycInformationService = kycInformationService;
        this.customerInfoMappingAccessor = customerInfoMappingAccessor;
        this.kycInfoFactory = kycInfoFactory;
        this.customerInfoRepository = customerInfoRepository;
    }


    /**
     * This Method Retrieves the users kycInformation like pan details and returns them.
     *
     * @param imsId imsId of the user.
     * @return {@link KycInfoResponse}
     */
    public KycInfoResponse getKycInformation(@NonNull final String imsId) {
        log.info("Fetching Kyc Information for imsId {}", imsId);
        final KycInfoResponse response = kycInformationService.getKycInformation(imsId);
        log.info("Kyc Information fetched for imsId {} and Information is {}", imsId, response);
        if (StringUtils.isNotBlank(response.getIdentificationDocumentNumber())) {
            saveImsAndPanInDB(imsId, response.getIdentificationDocumentNumber());
        }
        log.info("imsId and pan saved in pcc mapping db");
        return response;
    }

    private void saveImsAndPanInDB(@NonNull final String imsId, @NonNull final String panNumber) {
        log.info("imsId {} and pan {} that are saved in pcc mapping db", imsId, panNumber);
        customerInfoMappingAccessor.save(imsId, panNumber);
    }

    public KycInfoResponse getKycInfo(AuthorizationContext context) {
        String userType = extractUserTypeFromAuthorizationContext(context);
        String userId = extractIMSFromAuthorizationContext(context);
        String mobile = extractPhoneFromAuthorizationContext(context);

        log.info("kyc user Id : {}, mobile : {}", userId, mobile);
        KycInfoResponse kycInfoResponse = kycInfoFactory.getKycInformation(userId, userType);

        // FirstName & LastName get from Kyc
        if (StringUtils.isNotBlank(kycInfoResponse.getIdentificationDocumentNumber())) {
            try {
                NameInformation nameInformation = kycInfoFactory.getNameInformation(kycInfoResponse.getIdentificationDocumentNumber(), kycInfoResponse.getIdentificationDocumentType(), userType);
                kycInfoResponse.setFirstName(nameInformation.getFirstName());
                if (StringUtils.isNotBlank(nameInformation.getMiddleName())) {
                    kycInfoResponse.setFirstName(kycInfoResponse.getFirstName() + " " + nameInformation.getMiddleName());
                }
                kycInfoResponse.setLastName(nameInformation.getLastName());

                kycInfoResponse.setNameSource(NameSourceType.KYC);
            } catch (Exception e) {

            }
        }

        // FirstName & LastName get from IMS system
        if (Objects.isNull(kycInfoResponse.getNameSource())) {
            kycInfoResponse.setFirstName(extractFirstNameFromAuthorizationContext(context));
            String middleName = extractMiddleNameFromAuthorizationContext(context);
            log.info("IMS middleName: {}",middleName);
            if (StringUtils.isNotBlank(middleName)) {
                kycInfoResponse.setFirstName(kycInfoResponse.getFirstName() + " " + middleName);
            }

            kycInfoResponse.setLastName(extractLastNameFromAuthorizationContext(context));

            kycInfoResponse.setName(kycInfoResponse.getFirstName() + " " + kycInfoResponse.getLastName());

            kycInfoResponse.setNameSource(NameSourceType.IMS);
        }

        log.info("Kyc Information fetched for userId {} and Information is {}", userId, kycInfoResponse);
        kycInfoResponse.setEmail(extractEmailFromAuthorizationContext(context));
        return kycInfoResponse;
    }

}
