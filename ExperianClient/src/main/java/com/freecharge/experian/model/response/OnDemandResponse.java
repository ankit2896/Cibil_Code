package com.freecharge.experian.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder(alphabetic = true)
public class OnDemandResponse {
    @JsonProperty("stgOneHitId")
    private String stgOneHitId;

    @JsonProperty("showHtmlReportForCreditReport")
    private String showHtmlReportForCreditReport;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("errorString")
    private String errorString;

    private ExperianReport experianReport;
}