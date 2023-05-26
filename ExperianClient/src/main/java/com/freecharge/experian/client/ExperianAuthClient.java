package com.freecharge.experian.client;

import com.freecharge.experian.config.ExperianConfiguration;
import com.freecharge.experian.model.response.ExperianAuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "experianAuthClient" , url = "${experian.auth.base.url}" , configuration = ExperianConfiguration.class)
public interface ExperianAuthClient {

    @PostMapping(value = "token" , consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE , produces = MediaType.APPLICATION_JSON_VALUE)
    ExperianAuthResponse experianBearerToken(@RequestBody Map<String , ?> authRequest);

}
