package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.component.CibilReportParsingComponent;
import com.freecharge.cibil.component.ExperianReportParsingComponent;
import com.freecharge.cibil.dynamo.ExperianDynamoRepository;
import com.freecharge.cibil.model.enums.ReportPullType;
import com.freecharge.cibil.model.request.DynamodbDataInsertionRequest;
import com.freecharge.cibil.model.response.CibilParsing;
import com.freecharge.cibil.dynamo.DynamodbRepository;
import com.freecharge.experian.model.response.CreditParseReport;
import com.freecharge.experian.model.response.ExperianParsing;
import com.freecharge.cibil.rest.CibilParsingService;
import com.freecharge.cibil.constants.ApiUrl;
import com.freecharge.cibil.model.response.ServiceResponse;
import com.freecharge.cibil.mysql.repository.impl.CustomerCibilInfoRepository;
import com.freecharge.cibil.mysql.repository.impl.CustomerInfoRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Slf4j
@RestController
@Validated
public class VCCController {

    private final CibilParsingService cibilParsingService;

    private final CustomerInfoRepository customerInfoRepository;

    private final CustomerCibilInfoRepository customerCibilInfoRepository;

    private final DynamodbRepository dynamodbRepository;

    private final ExperianDynamoRepository experianDynamoRepository;

    private final CibilReportParsingComponent cibilReportParsingComponent;

    private final ExperianReportParsingComponent experianReportParsingComponent;


    public VCCController(@NonNull final CibilParsingService cibilParsingService,
                         @NonNull final CustomerInfoRepository customerInfoRepository,
                         @NonNull final CustomerCibilInfoRepository customerCibilInfoRepository,
                         @NonNull final DynamodbRepository dynamodbRepository,
                         @NonNull final CibilReportParsingComponent cibilReportParsingComponent,
                         @NonNull final  ExperianReportParsingComponent experianReportParsingComponent,
                         @NonNull final ExperianDynamoRepository experianDynamoRepository,
                         @NonNull final  ExperianReportParsingComponent experianReportParsingComponent1) {
        this.cibilParsingService = cibilParsingService;
        this.customerInfoRepository = customerInfoRepository;
        this.customerCibilInfoRepository = customerCibilInfoRepository;
        this.dynamodbRepository = dynamodbRepository;
        this.cibilReportParsingComponent = cibilReportParsingComponent;
        this.experianDynamoRepository = experianDynamoRepository;
        this.experianReportParsingComponent = experianReportParsingComponent1;
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.VCC_CIBIL_FETCH_REPORT)
    public ServiceResponse<CibilParsing> getCibilParsingData(@RequestParam("userId") @Valid @NotBlank String userId,
                                                             @RequestParam("pancardNumber") @Valid @NotBlank String panCardNumber,
                                                             @RequestParam("mobileNumber") @Valid @NotBlank String mobileNumber,
                                                             @RequestParam(name = "fcChannel") @Valid @NotBlank String fcChannel,
                                                             @RequestParam(name = "reportPullType") ReportPullType reportPullType ,
                                                             @RequestParam(name = "tncConsent") boolean tncConsent) {
        ServiceResponse<CibilParsing> response = cibilReportParsingComponent.fetchCibilReport(userId,panCardNumber, mobileNumber, tncConsent, fcChannel,reportPullType);
        return response;
    }

    @Logged
    @Timed
    @Marked
    @PostMapping(value = ApiUrl.VCC_INSERT_DATA)
    public CibilParsing insertDataIntoDynamodb(@RequestBody DynamodbDataInsertionRequest request) {
        log.info("Into VCC Insert data api");
        return cibilReportParsingComponent.cibilReportComponent(request);

    }


    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.VCC_DELETE_REPORT)
    public String deleteReport(@RequestParam String customerId) throws Exception {
        return dynamodbRepository.deleteDataFromDynamoUsingCustomerId(customerId);
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.VCC_EXPERIAN_FETCH_REPORT)
    public ServiceResponse<CreditParseReport> getExperianParsingData(@RequestParam("userId") @Valid @NotBlank String userId,
                                                                @RequestParam("pancardNumber") @Valid @NotBlank String panCardNumber,
                                                                @RequestParam("mobileNumber") @Valid @NotBlank String mobileNumber,
                                                                @RequestParam(name = "fcChannel") @Valid @NotBlank String fcChannel,
                                                                @RequestParam(name = "reportPullType") ReportPullType reportPullType,
                                                                @RequestParam(name = "tncConsent") boolean tncConsent){
        ServiceResponse<CreditParseReport> response = experianReportParsingComponent.fetchExperianReport(userId,panCardNumber, mobileNumber, tncConsent, fcChannel,reportPullType);
        return response;
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.VCC_EXPERIAN_DELETE_REPORT)
    public String deleteExperianReport(@RequestParam String customerId,
                                       @RequestParam String mobileNumber) {
        return experianDynamoRepository.deleteDataFromDynamoUsingCustomerId(customerId,mobileNumber);
    }

}
