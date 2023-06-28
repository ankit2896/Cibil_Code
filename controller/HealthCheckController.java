package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.constants.ApiUrl;
import com.freecharge.cibil.model.response.HealthStatus;
import com.freecharge.cibil.model.response.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Health Check
 */
@Slf4j
@RestController
public class HealthCheckController {

    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.UP_CHECK)
    public ServiceResponse<HealthStatus> ping() {
        log.debug("Ping Request Start");
        ServiceResponse<HealthStatus> response = new ServiceResponse<>();
        response.setData(HealthStatus.ACTIVE);
        response.setSuccess(true);
        log.debug("Ping Request End");
        return response;
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.HEALTH_CHECK)
    public String shallowPing() {
        log.debug("Ping Request");
        return HttpStatus.OK.toString();
    }

    @Logged
    @Timed
    @Marked
    @GetMapping(value = ApiUrl.DEEP_HEALTH_CHECK)
    public String deepPing() {
        log.debug("Deep Ping Request");
        return HttpStatus.OK.toString();
    }
}
