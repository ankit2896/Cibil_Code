package com.freecharge.cibil.component;

import com.freecharge.cibil.enums.UserActionToPolicyEnum;
import com.freecharge.cibil.exceptions.FCBadRequestException;
import com.freecharge.cibil.model.enums.TermsAndConditionsStatus;
import com.freecharge.cibil.model.enums.UserAction;
import com.freecharge.cibil.rest.TNCService;
import com.freecharge.cibil.rest.dto.request.TermsAndConditionAcceptanceRequest;
import com.freecharge.cibil.rest.dto.response.TermsAndConditionStatusResponse;
import com.freecharge.experian.enums.BureauType;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import javax.validation.ValidationException;

import static com.freecharge.cibil.constants.HeaderConstants.IMS_ID_NOT_SUPPLIED;
import static com.freecharge.cibil.model.enums.ErrorCodeAndMessage.TNC_NOT_ACCEPTED_BY_USER;

@Slf4j
@Component
public class CibilTnCComponent {

    private final TNCService tncService;

    @Setter
    @Value("${tnc.ims.policyId}")
    private int policyId;

    @Setter
    @Value("${tnc.merchant.policyId}")
    private int merchantPolicyId;

    public CibilTnCComponent(@NonNull final TNCService tncService) {
        this.tncService = tncService;
    }

    public boolean validateTnCConsentStatus(@NonNull final String userId, @NonNull final String userType, @NonNull final boolean TncConsent, @NonNull final String bureauType) {
        log.info("CibilTnCComponent | validateTnCConsentStatus for userId {}", userId);
        validateImsId(userId);
        final TermsAndConditionStatusResponse response = tncService.getUserPolicyAcceptanceStatus(userId, userType, bureauType);
        if ((!Objects.isNull(response.getPolicyId()) && (policyId == response.getPolicyId() || merchantPolicyId == response.getPolicyId()) &&
                (!Objects.isNull(response.getAction())))) {

            if (UserActionToPolicyEnum.ACCEPTED.equals(response.getAction())) {
                if (TncConsent && response.isReAcceptanceAllowed()) {
                    reAcceptTNC(userId, userType, UserAction.RE_ACCEPTED,bureauType);
                }
                return true;
            } else if (TncConsent) {
                acceptTermsAndConditions(TermsAndConditionsStatus.ACCEPTED.getValue(), userId, userType,bureauType);
                return true;
            }
        }
        return false;
    }


    public boolean isTnCAccepted(@NonNull final String userId, @NonNull final String userType){
        validateImsId(userId);
        final TermsAndConditionStatusResponse response = tncService.getUserPolicyAcceptanceStatus(userId, userType,BureauType.CIBIL.getBureauType());
        if(response.getAction().equals(UserActionToPolicyEnum.ACCEPTED))
            return true;
        return false;
    }

    private void reAcceptTNC(@NonNull final String userId, @NonNull final String userType, @NonNull final UserAction action,@NonNull final String bureauType) {
        validateImsId(userId);
        if (UserAction.RE_ACCEPTED.equals(action)) {
            log.info("Re-Accepting TNC for Action {}", action);
            tncService.reRecordUserActionToTNC(TermsAndConditionAcceptanceRequest.builder()
                    .userId(userId)
                    .userType(userType)
                    .bureauType(bureauType)
                    .acceptanceStatus(TermsAndConditionsStatus.ACCEPTED.getValue())
                    .build());
        }
    }

    public void acceptTermsAndConditions(@NonNull final String tncStatus,
                                          @NonNull final String userId,
                                          @NonNull final String userType,
                                         @NonNull final  String bureauType) {
        validateImsId(userId);
        if (TermsAndConditionsStatus.ACCEPTED.getValue().equals(tncStatus)) {
            final TermsAndConditionAcceptanceRequest request = TermsAndConditionAcceptanceRequest.builder()
                    .acceptanceStatus(tncStatus)
                    .userId(userId)
                    .bureauType(bureauType)
                    .userType(userType)
                    .build();
            log.info("Acceptance of Terms and Conditions with request {}", request);
            tncService.recordUserActionToTNC(request);
        } else {
            throw new FCBadRequestException(TNC_NOT_ACCEPTED_BY_USER);
        }
    }

    private void validateImsId(final String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new ValidationException(IMS_ID_NOT_SUPPLIED);
        }
    }

}
