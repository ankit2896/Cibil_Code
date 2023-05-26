package com.freecharge.experian.client;

import com.freecharge.experian.config.ExperianConfiguration;
import com.freecharge.experian.model.response.EnhancedMatchResponse;
import com.freecharge.experian.model.response.OnDemandResponse;
import com.freecharge.experian.model.response.SingleActionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "experianClient", url = "${experian.base.url}", configuration = ExperianConfiguration.class)
public interface ExperianClient {

    @PostMapping(value = "${enhanced.match.action}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    EnhancedMatchResponse enhancedMatchAction(@RequestBody Map<String, ?> enhancedMatchRequest,
                                              @RequestHeader("Authorization") String token);

    @PostMapping(value = "${single.match.action}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    SingleActionResponse singleMatchAction(@RequestBody Map<String, ?> singleActionRequest,
                                           @RequestHeader("Authorization") String token);

    @PostMapping(value = "${ondemand.refresh.action}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    OnDemandResponse onDemandRefreshAction(@RequestBody Map<String, ?> ondemandRequest,
                                           @RequestHeader("Authorization") String token);

}
