package com.freecharge.cibil.component;

import com.freecharge.cibil.exceptions.FCInternalClientException;
import com.freecharge.cibil.model.enums.*;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.mysql.mapper.CustomerInfoMappingMapper;
import com.freecharge.cibil.mysql.model.CustomerInfoModel;
import com.freecharge.cibil.mysql.repository.impl.CustomerInfoRepository;
import com.freecharge.cibil.rest.ExperianParsingService;
import com.freecharge.cibil.rest.impl.ExperianServiceImpl;
import com.freecharge.cibil.utils.JsonUtil;
import com.freecharge.experian.enums.BureauType;
import com.freecharge.experian.model.request.EnhancedMatchRequest;
import com.freecharge.experian.model.response.CreateEnhanceMatchRequest;
import com.freecharge.experian.model.response.CreditParseReport;
import com.freecharge.experian.model.response.EnhancedMatchResponse;
import com.freecharge.experian.service.ExperianClientService;
import com.freecharge.experian.utils.ExperianDTOMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class ExperianReportClientComponent {


    @Autowired
    private CustomerInfoRepository customerInfoRepository;

    @Autowired
    private ExperianClientService experianClientService;

    @Autowired
    private ExperianParsingService experianParsingService;

    @Autowired
    private ExperianServiceImpl experianServiceImpl;

    @Autowired
    private ExperianDTOMapper experianDTOMapper;

    public CreditParseReport newUserClientJourney(@NonNull final String userId, @NonNull final String userType, @NonNull final CreateEnhanceMatchRequest createEnhanceMatchRequest, CustomerInfoModel customerInfoModel){
        //Optional<CustomerInfo> customerInfoOptional = customerInfoMappingAccessor.getRecordForMobileNumber(createEnhanceMatchRequest.getMobileNumber());
        CustomerInfo customerInfo = CustomerInfoMappingMapper.convertExperianModelToEntity(customerInfoModel);
        if (Objects.nonNull(customerInfo) && Objects.nonNull(customerInfo.getCustomerId())) {
            if (!BureauType.CIBIL_EXPERIAN.equals(customerInfo.getBureauType())) {
                customerInfo.setBureauType(BureauType.CIBIL_EXPERIAN);
                customerInfoRepository.save(customerInfo);
            }
        } else {
            customerInfo = CustomerInfo.builder()
                    .userId(userId)
                    .userType(UserType.enumOf(userType))
                    .pancardNumber(createEnhanceMatchRequest.getPancardNumber())
                    .bureauType(BureauType.EXPERIAN)
                    .email(createEnhanceMatchRequest.getEmail())
                    .mobileNumber(createEnhanceMatchRequest.getMobileNumber())
                    .build();
        }

        //cibilTnCComponent.acceptTermsAndConditions(TermsAndConditionsStatus.ACCEPTED.getValue(),userId,userType,BureauType.EXPERIAN.getBureauType());

        log.info("experianEnhancedMatchAction call with createEnhanceMatchRequest {}", JsonUtil.writeValueAsString(createEnhanceMatchRequest));
        final EnhancedMatchRequest enhancedMatchRequest = experianDTOMapper.createEnhancedMatchRequest(createEnhanceMatchRequest);
        EnhancedMatchResponse response = null;
        try {
            response = experianClientService.getEnhanceMatch(enhancedMatchRequest);
            log.info("enhanceMatch call for createEnhanceMatchRequest {} with response {}", JsonUtil.writeValueAsString(createEnhanceMatchRequest), JsonUtil.writeValueAsString(response));

            if(StringUtils.isNotBlank(response.getErrorString())){
                throw new FCInternalClientException(response.getErrorString());
            }
        } catch (Exception e) {
            log.error("Exception while Calling experianEnhancedMatchAction {}", e);
            throw new FCInternalClientException(e.getMessage());
        }
        experianServiceImpl.saveUpdateExperianReport(customerInfo, enhancedMatchRequest, response, response.getExperianReport());
        CustomerInfoModel updateCustomerInfoModel = CustomerInfoMappingMapper.convertEntityToModelExperian(customerInfo);
        updateCustomerInfoModel.getCustomerExperianInfoModel().setCustomerId(updateCustomerInfoModel.getCustomerId());
        log.info("CustomerInfoModel is {}", updateCustomerInfoModel);
        return experianParsingService.addExperianDetails(updateCustomerInfoModel);
        // Need to remove or update
        //final CustomerExperianInfoModel customerExperianInfoModel = ExperianInfoMapper.toModel(customerInfo.getExperianInfo());

        //return generateExperianResponse(updateCustomerInfoModel.getCustomerExperianInfoModel());
    }


}
