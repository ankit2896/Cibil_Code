package com.freecharge.cibil.aws.sqs.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.freecharge.cibil.component.TransunionDataComponent;
import com.freecharge.cibil.constants.FcCibilConstants;
import com.freecharge.cibil.exceptions.FCInternalServerException;
import com.freecharge.cibil.model.TNCExpiryUpdateMessage;
import com.freecharge.cibil.model.TNCSqsModel;
import com.freecharge.cibil.model.enums.SQSEventType;
import com.freecharge.cibil.model.enums.UserType;
import com.freecharge.cibil.model.request.TransUnionDataFetchRequest;
import com.freecharge.cibil.model.response.TransUnionDataFetchResponseV2;
import com.freecharge.cibil.mysql.accessor.CibilInfoAccessor;
import com.freecharge.cibil.mysql.accessor.CustomerInfoMappingAccessor;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.provider.CustomerIdentifierProvider;
import com.freecharge.cibil.utils.DateUtils;
import com.freecharge.cibil.utility.ImsUserUtils;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.platform.aws.sqs.handler.AbstractSqsNotificationReceiver;
import com.freecharge.platform.aws.sqs.handler.SqsNotificationHandler;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.freecharge.cibil.model.enums.ErrorCodeAndMessage.INTERNAL_SERVER_ERROR;

@Log4j2
@Component
public class SqsNotificationReceiverImpl extends AbstractSqsNotificationReceiver {

    private final CibilInfoAccessor cibilInfoAccessor;
    private final CustomerInfoMappingAccessor customerInfoMappingAccessor;
    private CustomerIdentifierProvider customerIdentifierProvider;
    private final ImsUserUtils imsUserUtils;
    private TransunionDataComponent transunionDataComponent;

    @Setter
    @Value("${tnc.ims.policyType}")
    private String userPolicyType;

    @Setter
    @Value("${tnc.merchant.policyType}")
    private String merchantPolicyType;

    @Autowired
    public SqsNotificationReceiverImpl(@NonNull final CibilInfoAccessor cibilInfoAccessor,
                                       @NonNull final SqsNotificationHandler sqsNotificationHandler, @NonNull final CustomerInfoMappingAccessor customerInfoMappingAccessor, @NonNull final CustomerIdentifierProvider customerIdentifierProvider, @NonNull final ImsUserUtils imsUserUtils, @NonNull final TransunionDataComponent transunionDataComponent) {
        super(sqsNotificationHandler);
        this.cibilInfoAccessor = cibilInfoAccessor;
        this.customerInfoMappingAccessor = customerInfoMappingAccessor;
        this.customerIdentifierProvider = customerIdentifierProvider;
        this.imsUserUtils = imsUserUtils;
        this.transunionDataComponent = transunionDataComponent;
    }

    public void processNotification(@NonNull final Message notification) {
        MDC.put("requestId", UUID.randomUUID().toString());
        try {
            final TNCSqsModel tncSqsModel = JsonUtil.convertStringIntoSQSObject(notification.body(), new TypeReference<TNCSqsModel>() {
            });
            log.info("TNCSqsModel Event Received from SQS is {}", tncSqsModel);
            final TNCExpiryUpdateMessage tncExpiryUpdateMessage = JsonUtil.convertStringIntoObject(tncSqsModel.getMessage(), new TypeReference<TNCExpiryUpdateMessage>() {
            });
            log.info("TNCExpiryUpdateMessage Event Received from SQS is {}", tncExpiryUpdateMessage);

            if (tncSqsModel.getUserId().equals(tncExpiryUpdateMessage.getEntityId()) &&
                    SQSEventType.TNC_EXPIRY_UPDATE.equals(tncSqsModel.getEventType())) {

                String userType = StringUtils.EMPTY;
                if (userPolicyType.equals(tncExpiryUpdateMessage.getPolicyType()))
                    userType = UserType.USER.getUserType();
                else if (merchantPolicyType.equals(tncExpiryUpdateMessage.getPolicyType()))
                    userType = UserType.MERCHANT.getUserType();

                log.info("refresh cibilReport for event {} and userId {} and timestamp {}", notification, tncSqsModel.getUserId(), DateUtils.epochToDate(Long.valueOf(tncSqsModel.getTimeStamp())));
                refreshCibilReportBeforeTnCExpired(tncSqsModel.getUserId(), userType);
            }
        } catch (Exception ex) {
            log.error("Exception occurred while consuming notification {} and exception is {}", notification, ex);
            throw new FCInternalServerException(INTERNAL_SERVER_ERROR, ex.getMessage());
        } finally {
            MDC.remove("requestId");
            deleteNotification(notification);
        }
    }

    public void refreshCibilReportBeforeTnCExpired(String userId, String userType) {
        String mobileNumber = getMobileNumber(userId);

        if (Objects.nonNull(mobileNumber)) {
            final String customerIdentifier = customerIdentifierProvider.getPccId(mobileNumber);

            TransUnionDataFetchRequest transUnionDataFetchRequest = TransUnionDataFetchRequest.builder()
                    .customerIdentifier(customerIdentifier)
                    .mobileNumber(mobileNumber)
                    .userId(userId)
                    .build();

            log.info("refreshCibilReportBeforeTnCExpired | TransUnionDataFetchRequest request : {}", transUnionDataFetchRequest);
            TransUnionDataFetchResponseV2 responseV2 = transunionDataComponent.getTransUnionDataFetchResponseV2(transUnionDataFetchRequest,
                    mobileNumber, "", "", userType, FcCibilConstants.BEFORE_TNC_EXPIRED_FCCHANNEL_TYPE);

            log.info("refreshCibilReportBeforeTnCExpired | TransUnionDataFetchResponseV2 response : {}", responseV2);
        } else {
            log.info("User entry not Found");
        }
    }

    private String getMobileNumber(String userId) {
        String mobileNumber = StringUtils.EMPTY;
        try {
            Optional<CustomerInfo> customerInfoOptional = customerInfoMappingAccessor.getRecordForUserId(userId);
            if (customerInfoOptional.isPresent() && StringUtils.isNotBlank(customerInfoOptional.get().getMobileNumber())) {
                mobileNumber = customerInfoOptional.get().getMobileNumber();
                log.info("Mobile Number fetched from Db : {}", mobileNumber);
            }
        } catch (Exception e) {
            log.info("Exception while fetching the mobile Number for userId {}", userId);
            e.printStackTrace();
        }
        return mobileNumber;
    }
}
