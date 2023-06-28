package com.freecharge.cibil.controller.experian;

import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.model.enums.UserType;
import com.freecharge.experian.model.response.ExperianParsing;
import com.freecharge.cibil.model.response.ServiceResponse;
import com.freecharge.cibil.mysql.entity.CustomerInfo;
import com.freecharge.cibil.mysql.mapper.CustomerInfoMappingMapper;
import com.freecharge.cibil.mysql.model.CustomerInfoModel;
import com.freecharge.cibil.mysql.repository.impl.CustomerInfoRepository;
import com.freecharge.cibil.rest.ExperianParsingService;
import com.freecharge.cibil.rest.ExperianService;
import com.freecharge.experian.model.request.SingleActionRequest;
import com.freecharge.experian.model.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Objects;

import static com.freecharge.cibil.constants.ApiUrl.*;

@RestController
@Slf4j
@Validated
public class ExperianController {

    @Autowired
    private ExperianService experianService;

    @Autowired
    private ExperianParsingService experianParsingService;

    @Autowired
    private CustomerInfoRepository customerInfoRepository;

    @Logged
    @Timed
    @Marked
    @PostMapping(value = EXPERIAN_ENHANCED_MATCH)
    public ServiceResponse<ExperianDataFetchResponse> enhancedMatch(@Valid @RequestBody CreateEnhanceMatchRequest createEnhanceMatchRequest) {
        ExperianDataFetchResponse response = experianService.experianEnhancedMatchAction(createEnhanceMatchRequest.getUserId(), UserType.USER.getUserType(), createEnhanceMatchRequest);
        return new ServiceResponse<>(response, true);
    }

    @Logged
    @Timed
    @Marked
    @PostMapping(value = EXPERIAN_SINGLE_ACTION)
    public ServiceResponse<SingleActionResponse> singleAction(@Valid @RequestBody SingleActionRequest singleActionRequest) {
        return new ServiceResponse<>(null, true);
    }

    @Logged
    @Timed
    @Marked
    @PostMapping(value = EXPERIAN_ONDEMAND)
    public ServiceResponse<ExperianDataFetchResponse> onDemand(@Valid @RequestBody CreateOnDemandRequest onDemandRequest) {
        ExperianDataFetchResponse response = experianService.experianOnDemandAction(onDemandRequest.getUserId(), UserType.USER.getUserType(), onDemandRequest.getMobileNumber());
        return new ServiceResponse<>(response, true);
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = EXPERIAN_FETCH_TOKEN)
    ServiceResponse<ExperianAuthResponse> experianFetchToken() {
        ExperianAuthResponse response = experianService.fetchBearerToken();
        return new ServiceResponse<>(response, true);
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = EXPERIAN_PARSE_REPORT)
    ServiceResponse<CreditParseReport> experianParseReport(@RequestParam String mobileNumber) {
        CustomerInfo customerInfo = customerInfoRepository.getRecordForMobileNumber(mobileNumber);
        CustomerInfoModel customerInfoModel = CustomerInfoMappingMapper.convertEntityToModelExperian(customerInfo);
        if (Objects.nonNull(customerInfo.getExperianInfo()) && Objects.nonNull(customerInfo.getExperianInfo().getExperianReport())) {
            CreditParseReport response = experianParsingService.addExperianDetails(customerInfoModel);
            return new ServiceResponse<>(response, true);
        }
        return new ServiceResponse<>(null, true);
    }

}