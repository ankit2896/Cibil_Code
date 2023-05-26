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
public class EnhancedMatchResponse {
    @JsonProperty("stageOneId_")
    private String stageOneId;

    @JsonProperty("stageTwoId_")
    private String stageTwoId;

    @JsonProperty("showHtmlReportForCreditReport")
    private String showHtmlReportForCreditReport;

    @JsonProperty("errorString")
    private String errorString;
    
    private ExperianReport experianReport;
}